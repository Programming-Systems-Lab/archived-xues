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
 * Revision 1.23  2001-06-02 18:22:56  jjp32
 *
 * Fixed bug where wildHash would not get assigned if derivative state never got a notification
 *
 * Revision 1.22  2001/05/29 17:25:25  jjp32
 * Rolled in new constructors for embeddable version
 *
 * Revision 1.21  2001/05/29 17:21:30  jjp32
 * Added embeddable-shutdown functionality (is not a shutdown hook right
 * now-- you have been warned :)
 *
 * Revision 1.20  2001/05/28 00:01:17  jjp32
 * I'll get it right one of these days
 *
 * Revision 1.19  2001/05/27 23:59:52  jjp32
 * Would help compiling if I closed the previous method
 *
 * Revision 1.18  2001/05/27 23:55:13  jjp32
 * Added embeddable support from development branch, but fixed the
 * non-returning-constructor problem
 *
 * Revision 1.17  2001/05/21 00:43:04  jjp32
 * Rolled in Enrico's changes to main Xues trunk
 *
 * Revision 1.16.4.4  2001/05/06 08:01:48  eb659
 *
 * FINAL VERSION FOR THIS RELEASE! Expected functionality fully implemented.
 * Use EDTestOR to test or representation. Call it with smthg like:
 * 'java psl.xues.EDTestOR -s senp://canal:8888 -e abc'
 * using the desired siena host, and the desired event sequence. The command
 * above will send the events a, b, c, in that order. Look at the definition
 * of the 'hexagon rule' in TestRules2.xml to see which sequences are significant
 * to test.
 * Any reasonably short  sequence that contains, as a subset,
 * either 'abcdgh' or 'abefgh' should succeed (that is, propagate a success
 * notification {hex="true"}. Any sequence that does not satisfy this, should
 * fail, and the instantiate machine should time out.
 *
 * Otherwise, use EDTest to see the normal functioning, and AddRuleTest
 * (removing comments as appropriate) to test different dynamic rulebase
 * functions.
 *
 * Revision 1.16.4.3  2001/05/06 05:31:07  eb659
 *
 * Thoroughly re-tested all dynamic rulebase and fixed so that it
 * works with the new arch.
 *
 * Revision 1.16.4.2  2001/05/06 03:54:27  eb659
 *
 * Added support for checking multiple parents, and independent wild hashtables
 * for different paths. ED now has full functionality, and resiliency.
 * Now doing some additional testing for event sequences that actually use
 * the OR representation, and re-testing the dynamic rulebase, to make sure
 * it still works after the changes made.
 *
 * Revision 1.16.4.1  2001/05/02 00:04:27  eb659
 *
 * Tested and fixed a couple of things.
 * New architecture works, and can be tested using EDTest.
 * Reaper thread needs revision...
 * Can we get rid of internal 'loopback' notifications?
 *
 * Revision 1.16  2001/01/30 07:33:10  jjp32
 *
 * I'm done for tonight
 *
 * Revision 1.15  2001/01/30 06:26:18  jjp32
 *
 * Lots and lots of updates.  EventDistiller is now of demo-quality.
 *
 * Revision 1.14  2001/01/30 02:39:36  jjp32
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
public class EventDistiller implements Runnable, Notifiable {
  /**
   * We maintain a stack of events to process - this way incoming
   * callbacks don't get tied up - they just push onto the stack.
   */
  private Vector eventProcessQueue = null;
  
  /** My main execution context */
  private Thread edContext = null;

  /** Reference to state machine manager. */
  EDStateManager edsm;

  /** Private (internal) siena for the state machines */
  private Siena privateSiena = null;

  /** Public (KX) siena to communicate with the outside world */
  private Siena publicSiena = null;

    /** 
     * An object that instantiates an EventDistiller. In this case 
     * we communicate with it derectly, by sending it notifications,
     * otherwise we use Siena. 
     */
    private Notifiable owner = null;

  /** Private loopback Siena */
    //private Siena loopbackSiena = null;

  /** Debug flag. */
  static boolean DEBUG = false;

  /** XXX - hack */
  private static String sienaHost = "senp://localhost";
  
  /** State machine specification file */
  private static String stateSpecFile = null;

  /** Whether the ED is to be shut down by the owning application. */
  boolean inShutdown = false;

  /** 
   * Reap fudge factor.  IMPORTANT to take care of non-realtime event
   * buses (can anyone say Siena?)  XXX - should be a better way to do this.
   */
  public static int reapFudgeMillis = 6000;

    /**
     * Main. */
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

  /** Prints usage. */
  public static void usage() {
    System.out.println("usage: java EventDistiller [-f stateSpecFile] "+
		       "[-s sienaHost] [-d] [-?]");
    System.out.println("Warning!  Omitting stateFile causes EventDistiller "+
		       "to run in demo mode.");
    System.exit(-1);
  }

  /**
   * Constructs a new EventDistiller with an owner.
   * If you use this constructor, pass events to ED directly through 
   * the notify method using EDInputNotification.
   *
   * @param owner the object to which we return notifications
   * @param spec the name of the file containing the specification;
   *             could be null
   * @param debug whether debug statements should be printed
   */
  public EventDistiller(Notifiable owner, String spec, boolean debug) { 
    this.owner = owner;
    stateSpecFile = spec;
    DEBUG = debug;

    new Thread(this).start(); // new thread context to run in
  }

  /** Constructs a new ED with an owner -- Use notifications to add rules. */
  public EventDistiller(Notifiable owner) { this(owner, null, false); }

  /** Constructs a new ED with an owner and spec file. */
  public EventDistiller(Notifiable owner, String spec) { this(owner, spec, false); }

  /** Constructs a new ED with an owner and debug statements. */
  public EventDistiller(Notifiable owner, boolean debug) { this(owner, null, debug); }
  
  /**
   * Standard, non-embedded CTOR.
   */
  public EventDistiller() { 
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

    /* Add a shutdown hook */
    Runtime.getRuntime().addShutdownHook(new Thread() {
	public void run() { finish(); } });

    run(); /* Don't need to create new thread */
  }

  /** Start execution of the new EventDistiller. */
  public void run() {
    eventProcessQueue = new Vector();

    // Set the current execution context, so if the callback is called
    // it can wake up a sleeping distiller
    edContext = Thread.currentThread();

    // Create private siena
    privateSiena = new HierarchicalDispatcher();
    try {
      ((HierarchicalDispatcher)privateSiena).
	setReceiver(new TCPPacketReceiver(61979));
    } catch(Exception e) { e.printStackTrace(); }
    
    // Initialize state machine manager.  Hand it the private siena.
    edsm = new EDStateManager(privateSiena, this, 
					     stateSpecFile);

    // Run process injector.  NOTE: Due to parallelism of Siena, ordering
    // is not guaranteed.  Below is a rather hacky way of ensuring a
    // very high probability of order in the internal Siena.
    while(true) {
      if (inShutdown) { // shutting down -- clean up and terminate
	finish();
	break;
      }
      //       if(EventDistiller.DEBUG)
      // 	System.err.println("EventDistiller: Checking process 
      if(EventDistiller.DEBUG)
       	System.err.print("+");


      // Poll for events to process
      int size;
      synchronized(eventProcessQueue) {
	size = eventProcessQueue.size();
      }

      try {
	Thread.sleep(2000);
      } catch(InterruptedException ie) { ; }

      /* Use the following when Siena is replaced internally with something
	 more sane. */
      /*if(size == 0) {
	if(EventDistiller.DEBUG)
	System.err.println("EventDistiller: Queue empty, sleeping");
	try {
	Thread.sleep(2000);
	} catch(InterruptedException ie) { ; }
	continue;
	} else*/
      if(size != 0) {
	// Pull them off
	synchronized(eventProcessQueue) {
	  Notification n = (Notification)eventProcessQueue.remove(0);
	  if(EventDistiller.DEBUG)
	    System.err.println("EventDistiller: Publishing event internally " + n);
	  // Feed it to our internal Siena
	  try {
	    privateSiena.publish(n);
	  } catch(SienaException e) { e.printStackTrace(); }
	}	      
      }
    }
  }

  /** Shuts down the ED. */
  public void shutdown() {
    inShutdown = true; 
    // the loop will die automatically, after cleaning up
  }

  /** Shutdown hook. */
  private void finish() {
    /* Shut down the hierarchical dispatchers */
    System.err.println("EventDistiller: shutting down");
    try {
      if (owner == null) ((HierarchicalDispatcher)publicSiena).shutdown();
      ((HierarchicalDispatcher)privateSiena).shutdown();
      //((HierarchicalDispatcher)loopbackSiena).shutdown();
    } catch(Exception e) { e.printStackTrace(); }
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
      // Add a loopback value
	//n.putAttribute("loopback",(int)0);
      // Add the event onto the queue and then wake up the engine
      if(DEBUG) System.err.println("EventDistiller: Putting event on queue");
      synchronized(eventProcessQueue) {
	eventProcessQueue.addElement(n);
      }
      //      edContext.interrupt();
    }
  }

  /** Unused Siena construct. */
  public void notify(Notification[] s) { ; }

  /**
   * Propagates a given notification to the world - or
   * internally, if it is an internal notification.
   * @param n the notofication to propagate, 
   *          must be in already wrapped format
   */
  void sendPublic(Notification n) {
    if(EventDistiller.DEBUG)
      System.err.println("YES! EventDistiller: sending event " + n);

    try {
	/* if this is an internal notification
	 * we just send it through to ourselves. */
	if (n.getAttribute("Internal") != null && 
	    n.getAttribute("Internal").booleanValue()) 
	    privateSiena.publish(KXNotification.EDInternalNotification(n)); 
	else { // the notification goes outside
	    // if we have an owner, send him the notification
	    if (owner != null) owner.notify(n);
	    // else send it to the public siena
	    else publicSiena.publish(n); 
	}
    }
    catch(SienaException e) { e.printStackTrace(); }
  }
}
