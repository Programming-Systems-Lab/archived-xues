package psl.xues;

import psl.kx.*;
import java.io.*;
import java.util.*;
import siena.*;

/**
 * The Event Distiller.
 *
 * Copyright (c) 2000-2001: The Trustees of Columbia University and the
 * City of New York.  All Rights Reserved.
 *
 * TODO:
 * - Something more complex than a Notification as Action
 * - Multiple actions
 *
 * @author Janak J Parekh
 * @version 0.9
 *
 * $Log$
 * Revision 1.14  2001-01-30 02:39:36  jjp32
 *
 * Added loopback functionality so hopefully internal siena gets the msgs
 * back
 *
 * Revision 1.13  2001/01/29 05:22:53  jjp32
 *
 * Reaper written - but it's probably a problem
 *
 * Revision 1.12  2001/01/28 22:58:58  jjp32
 *
 * Wildcard support has been added
 *
 * Revision 1.11  2001/01/28 21:34:00  jjp32
 *
 * XML parsing complete; almost ready for demo
 *
 * Revision 1.10  2001/01/26 03:30:53  jjp32
 *
 * Now supports non-localhost siena servers
 *
 * Revision 1.9  2001/01/22 02:11:54  jjp32
 *
 * First full Siena-aware build of XUES!
 *
 * Revision 1.8  2001/01/18 01:41:35  jjp32
 *
 * Moved KXNotification to kx; other modifications for demo
 *
 * Revision 1.7  2000/12/26 22:25:13  jjp32
 *
 * Updating to latest preview versions
 *
 * Revision 1.6  2000/09/09 18:17:14  jjp32
 *
 * Lots of bugs and fixes for demo
 *
 * Revision 1.5  2000/09/09 15:13:49  jjp32
 *
 * Numerous updates, bugfixes for demo
 *
 * Revision 1.4  2000/09/08 19:08:27  jjp32
 *
 * Minor updates, added socket communications in TriKXEventNotifier
 *
 * Revision 1.3  2000/09/08 02:09:44  jjp32
 *
 * Some minor updates
 *
 * Revision 1.2  2000/09/07 23:15:25  jjp32
 *
 * Added EventNotifier code; updated previous event code
 *
 * Revision 1.1  2000/09/07 19:30:49  jjp32
 *
 * Updating
 *
 */
public class EventDistiller implements Notifiable {
  /**
   * We maintain a stack of events to process - this way incoming
   * callbacks don't get tied up - they just push onto the stack.
   */
  private Vector eventProcessQueue = null;
  
  /** My main execution context */
  private Thread edContext = null;

  /** Private (internal) siena for the state machines */
  private Siena privateSiena = null;

  /** Public (KX) siena to communicate with the outside world */
  private Siena publicSiena = null;

  /** Private loopback Siena */
  private Siena loopbackSiena = null;

  /** Debug flag. */
  static boolean DEBUG = false;

  /** XXX - hack */
  private static String sienaHost = "senp://localhost";
  
  /** State machine specification file */
  private static String stateSpecFile = null;

  /** 
   * Reap fudge factor.  IMPORTANT to take care of non-realtime event
   * buses (can anyone say Siena?)  XXX - should be a better way to do this.
   */
  public static int reapFudgeMillis = 10000;

  /**
   * Main.  */
  public static void main(String args[]) {
    if(args.length > 0) { // Siena host specified?
      for(int i=0; i < args.length; i++) {
	if(args[i].equals("-s"))
	  sienaHost = args[++i];
	else if(args[i].equals("-f"))
	  stateSpecFile = args[++i];
	else if(args[i].equals("-d"))
	  DEBUG = true;
	else
	  usage();
      }
    }   
    new EventDistiller();
  }

  /**
   * Print usage.
   */
  public static void usage() {
    System.out.println("usage: java EventDistiller [-f stateSpecFile] "+
		       "[-s sienaHost] [-d] [-?]");
    System.out.println("Warning!  Omitting stateFile causes EventDistiller "+
		       "to run in demo mode.");
    System.exit(-1);
  }

  /**
   * CTOR.
   */
  public EventDistiller() { 
    eventProcessQueue = new Vector();

    // Set the current execution context, so if the callback is called
    // it can wake up a sleeping distiller
    edContext = Thread.currentThread();

    // Siena handling
    // Subscribe to the "master" Siena
    publicSiena = new HierarchicalDispatcher();
    try {
      ((HierarchicalDispatcher)publicSiena).
	setReceiver(new TCPPacketReceiver(61978));
      ((HierarchicalDispatcher)publicSiena).setMaster(sienaHost);
    } catch(Exception e) { e.printStackTrace(); }
   
    // Subscribe to packager input, metaparser results, and general input
    Filter packagerFilter = new Filter();
    packagerFilter.addConstraint("Type","PackagedEvent");
    Filter metaparserFilter = new Filter();
    metaparserFilter.addConstraint("Type","ParsedEvent");
    Filter generalFilter = new Filter();
    generalFilter.addConstraint("Type","EDInput");
    try {
      publicSiena.subscribe(packagerFilter, this);
      publicSiena.subscribe(metaparserFilter, this);
      publicSiena.subscribe(generalFilter, this);
    } catch(SienaException e) { e.printStackTrace(); }

    // Create private siena
    privateSiena = new HierarchicalDispatcher();
    try {
      ((HierarchicalDispatcher)privateSiena).
	setReceiver(new TCPPacketReceiver(61979));
    } catch(Exception e) { e.printStackTrace(); }
    
    // Create private loopback siena
    loopbackSiena = new HierarchicalDispatcher();
    try {
      ((HierarchicalDispatcher)loopbackSiena).
	setReceiver(new TCPPacketReceiver(61980));
      // Now start a loopback class
      EDLoopback edlb = new EDLoopback(loopbackSiena);
    } catch(Exception e) { e.printStackTrace(); }

    // Initialize state machine manager.  Hand it the private siena.
    EDStateManager edsm = new EDStateManager(privateSiena, this, 
					     stateSpecFile);

    /* Add a shutdown hook */
    Runtime.getRuntime().addShutdownHook(new Thread() {
	public void run() {      
	  /* Shut down the hierarchical dispatchers */
	  System.err.println("EventDistiller: shutting down");
	  try {
	    ((HierarchicalDispatcher)publicSiena).shutdown();
	    ((HierarchicalDispatcher)privateSiena).shutdown();
	  } catch(Exception e) { e.printStackTrace(); }
	}
      });


    // Run
    while(true) {
      if(EventDistiller.DEBUG)
	System.err.println("EventDistiller: Checking process queue");

      // Poll for events to process
      int size;
      synchronized(eventProcessQueue) {
	size = eventProcessQueue.size();
      }

      if(size == 0) {
	if(EventDistiller.DEBUG)
	  System.err.println("EventDistiller: Queue empty, sleeping");
	try {
	  Thread.sleep(10000);
	} catch(InterruptedException ie) { ; }
	continue;
      } else {
	// Otherwise pull them off
	synchronized(eventProcessQueue) {
	  Notification n = (Notification)eventProcessQueue.remove(0);
	  if(EventDistiller.DEBUG)
	    System.err.println("EventDistiller: Processing " + n);
	  // Feed it to our internal Siena
	  try {
	    privateSiena.publish(n);
	  } catch(SienaException e) { e.printStackTrace(); }
	}	      
      }
    }
  }

  /**
   * Siena Callback.  We receive two kinds of callbacks: data from the
   * event packager, and interpreted results from the metaparser.
   */
  public void notify(Notification n) {
    if(DEBUG) System.out.println("EventDistiller: Received notification "+n);
    // What kind of notification is it?
    if(n.getAttribute("Type").stringValue().equals("PackagedEvent")) {
      // Just wrap it up and send it out to the metaparser, for now.
      try {
	Notification mpN = KXNotification.
	  EventDistillerKXNotification(12345,
				       (int)n.
				       getAttribute("DataSourceID").
				       longValue(),
				       (String)null, //dataSourceURL
				       n.getAttribute("SmartEvent").
				       stringValue());
	if(DEBUG) System.err.println("EventDistiller: Sending notification to Metaparser" + mpN);
	publicSiena.publish(mpN);
      } catch(SienaException e) { e.printStackTrace(); }
    } else if(n.getAttribute("Type").stringValue().equals("ParsedEvent") ||
	      n.getAttribute("Type").stringValue().equals("EDInput")) {
      // Add the event onto the queue and then wake up the engine
      if(DEBUG) System.err.println("EventDistiller: Putting event on queue");
      synchronized(eventProcessQueue) {
	eventProcessQueue.addElement(n);
      }
      edContext.interrupt();
    }
  }

  /** Unused Siena construct. */
  public void notify(Notification[] s) { ; }

  /**
   * Send out to the world.  Used for the state machine.
   */
  void sendPublic(Notification n) {
    if(EventDistiller.DEBUG)
      System.err.println("EventDistiller: sending event " + n);
    try {
      publicSiena.publish(n);
    } catch(SienaException e) { e.printStackTrace(); }
  }
}
