package psl.xues;

import siena.*;
import psl.kx.*;
import java.net.*;
import java.io.*;

/** 
 * EventPackager for Xues.  Now Siena-compliant(TM).
 *
 * Copyright (c) 2000: The Trustees of Columbia University in the
 * City of New York.  All Rights Reserved.
 *
 * TODO: 
 * - Handling multi-line input as an event - how to delineate?  Right
 *   now, we just IGNORE all socket event!!!
 * - When socket closes, we wipe out TriKX.  More permanent solution.
 * - Put associated data onto webserver.  Send URL to KXNotification.
 *   Handle streaming in some appropriate way (non-trivial?  custom 
 *   webserver?)
 * - Integrate conf file handling
 * - Intelligent connection to Event Distiller
 * - Timestamp non-stamped incoming events
 *
 * @author Janak J Parekh <jjp32@cs.columbia.edu>
 * @version 0.01 (9/7/2000)
 *
 * $Log$
 * Revision 1.14  2001-01-29 02:14:36  jjp32
 *
 * Support for multiple attributes on a output notification added.
 *
 * Added Workgroup Cache test rules
 *
 * Revision 1.13  2001/01/28 22:58:58  jjp32
 *
 * Wildcard support has been added
 *
 * Revision 1.12  2001/01/26 03:30:54  jjp32
 *
 * Now supports non-localhost siena servers
 *
 * Revision 1.11  2001/01/22 02:11:54  jjp32
 *
 * First full Siena-aware build of XUES!
 *
 * Revision 1.10  2001/01/18 01:41:35  jjp32
 *
 * Moved KXNotification to kx; other modifications for demo
 *
 * Revision 1.9  2001/01/01 00:32:28  jjp32
 *
 * Added rudimentary Siena-publishing capabilities to Event Packager.  Created a (possibly, in the future) base notification class with convenience constructors (right now just for EP but in the future also for other KX components).
 *
 * Revision 1.8  2000/12/26 22:25:13  jjp32
 *
 * Updating to latest preview versions
 *
 * Revision 1.7  2000/09/09 18:17:14  jjp32
 *
 * Lots of bugs and fixes for demo
 *
 * Revision 1.6  2000/09/09 15:13:49  jjp32
 *
 * Numerous updates, bugfixes for demo
 *
 * Revision 1.5  2000/09/08 22:40:43  jjp32
 *
 * Numerous server-side bug fixes.
 * Removed TriKXUpdateObject, psl.trikx.impl now owns it to avoid applet permission hassles
 *
 * Revision 1.4  2000/09/08 19:08:27  jjp32
 *
 * Minor updates, added socket communications in TriKXEventNotifier
 *
 * Revision 1.3  2000/09/07 23:15:25  jjp32
 *
 * Added EventNotifier code; updated previous event code
 *
 * Revision 1.2  2000/09/07 19:30:49  jjp32
 *
 * Updating
 *
 */
public class EventPackager implements Notifiable {
  /** XXX - This is a hack for now */
  private static String sienaHost = "senp://localhost";

  int listeningPort = -1;
  ServerSocket listeningSocket = null;
  String spoolFilename;
  ObjectOutputStream spoolFile;
  int srcIDCounter = 0;
  Siena siena = null;

  /**
   * Basic CTOR.  
   * Assumes no spooling, and no listening sockets.
   */
  public EventPackager() {
    this(-1, null);
  }

  /**
   * CTOR.
   *
   * @param listeningPort Port to establish listening on.
   * @param spoolFile File to spool events to.
   */
  public EventPackager(int listeningPort, String spoolFilename) { 
    this.listeningPort = listeningPort;
    this.spoolFilename = spoolFilename;
    if(this.spoolFilename != null) { 
      try {
	this.spoolFile = new ObjectOutputStream(new
	  FileOutputStream(this.spoolFilename,true));	
      } catch(Exception e) { 
	System.err.println("Error creating spool file");
	e.printStackTrace();
      }
    }

    /* Add a shutdown hook */
    Runtime.getRuntime().addShutdownHook(new Thread() {
	public void run() {      
	  /* Clean up the file streams */
	  if(spoolFile != null) {
	    System.err.println("EventPackager: shutting down");
	    try {
	      spoolFile.close();
	      spoolFile = null;
	      ((HierarchicalDispatcher)siena).shutdown();
	    } catch(Exception e) { e.printStackTrace(); }
	  }
	}
      });
    
    // Now create a Siena node
    siena = new HierarchicalDispatcher();
    try {
      ((HierarchicalDispatcher)siena).
	setReceiver(new TCPPacketReceiver(61977));
      ((HierarchicalDispatcher)siena).setMaster(sienaHost);
    } catch(Exception e) { e.printStackTrace(); }

    // Set up listening.  We listen for SmartEvents (which have a data
    // field with all the data) and DirectEvents (which have the
    // attributes inline).
    Filter f = new Filter();
    f.addConstraint("Type","SmartEvent");
    try {
      siena.subscribe(f, this);
    } catch(SienaException e) { e.printStackTrace(); }

    Filter g = new Filter();
    g.addConstraint("Type","DirectEvent");
    try {
      siena.subscribe(g, this);
    } catch(SienaException e) { e.printStackTrace(); }
  }
    
  /**
   * Run routine.
   */
  public void run() {
    /* Set up server socket */
    try {
      listeningSocket = new ServerSocket(listeningPort);
    } catch(Exception e) {
      System.err.println("EventPackager: Failed in setting up serverSocket, "+
			 "shutting down");
      listeningSocket = null;
      return;
    }
    /* Listen for connection */
    while(true) {
      try {
	Socket newSock = listeningSocket.accept();
	/* Hand the hot potato off! */
	new Thread(new EPClientThread(srcIDCounter++,newSock)).start();
      } catch(Exception e) {
	System.err.println("EventPackager: Failed in accept from "+
			   "serverSocket");
      }
    }
  }

  /**
   * Tester.
   */
  public static void main(String args[]) {
    if(args.length > 0) { // Siena host specified?
      for(int i=0; i < args.length; i++) {
	if(args[i].equals("-s"))
	  sienaHost = args[++i];
	else if(args[i].equals("-?"))
	  usage();
	else
	  usage();
      }
    }	   

    EventPackager ep = new EventPackager(7777, "EventPackager.spl");
    ep.run();
  } 

  /**
   * Print usage.
   */
  public static void usage() {
    System.out.println("usage: java EventPackager [-s sienaHost] [-?]");
    System.exit(-1);
  }

  /**
   * Handle incoming siena notifications.
   */
  public void notify(Notification n) {
    // Do a turnaround and send it out.
    String data = n.getAttribute("SmartEvent").stringValue();
    Notification q = 
      KXNotification.EventPackagerKXNotification(11111,22222,(String)null,
						 data);
  
    try {
      siena.publish(q);
    } catch(SienaException e) { e.printStackTrace(); }
  }

  /** Unused Siena construct. */
  public void notify(Notification[] s) { ; }

  class EPClientThread implements Runnable {
    private int srcID;
    private Socket clientSocket;
    private BufferedReader in;
    //    private PrintWriter out;

    public EPClientThread(int srcID, Socket clientSocket) {
      this.srcID = srcID;
      this.clientSocket = clientSocket;
      /* Build the streams */
      try {
	this.in = new BufferedReader(new 
	  InputStreamReader(clientSocket.getInputStream()));
	//	this.out = new PrintWriter(clientSocket.getOutputStream(), 
	//				   true); //autoflush
      } catch(Exception e) {
	System.err.println("Error establishing client connection: " +
			   e.toString());
	e.printStackTrace();
      }
    }
    
    public void run() {
      /* Wait for stuff - then write it to disk - and to the bus */
      try {
	while(true) {
	  String newInput = in.readLine();
	  if(newInput == null) { // Finished
	    System.err.println("EPCThread: closing connection");
	    in.close();
	    clientSocket.close();
	    return;
	  }
	  System.err.println("EPCThread: Got " + newInput);
	  if(spoolFile != null) spoolFile.writeObject(newInput);
	  // Now send out the event.
	  if(siena != null) {
	    //	    try {
	    //	      siena.publish(new KXNotification(srcID,null));
	    //	    } catch(SienaException e) { e.printStackTrace(); }
	  }
	}
      } catch(SocketException e) {
	System.err.println("EPCThread: Client socket unexpectedly closed");
	return;
      } catch(Exception e) {
	System.err.println("EPCThread: Error communicating with client:" + 
			   e.toString());
	e.printStackTrace();
	return;
      }
    }
  }
}
