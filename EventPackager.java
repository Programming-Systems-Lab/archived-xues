package psl.xues;

import psl.groupspace.*;
import java.net.*;
import java.io.*;

/** 
 * EventPackager for Xues.
 *
 * Copyright (c) 2000: The Trustees of Columbia University and the
 * City of New York.  All Rights Reserved.
 *
 * TODO: 
 * Listening on local bus.
 * Handling multi-line input as an event - how to delineate?  Right
 * now, we just blindly do it on a line-by-line basis.
 * When socket closes, we wipe out TriKX.  More permanent solution.
 *
 * @author Janak J Parekh <jjp32@cs.columbia.edu>
 * @version 0.01 (9/7/2000)
 *
 * $Log$
 * Revision 1.8  2000-12-26 22:25:13  jjp32
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
public class EventPackager implements GroupspaceService,
				      GroupspaceCallback {
  GroupspaceController gcRef = null;
  int listeningPort = -1;
  ServerSocket listeningSocket = null;
  String spoolFilename;
  ObjectOutputStream spoolFile;
  int srcIDCounter = 0;
  private String roleName = "EventPackager";

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

    /* Add a shutdown hook for JDK 1.3 */
    Runtime.getRuntime().addShutdownHook(new EPShutdownThread());
  }
    
  /**
   * Initialization routine.
   */
  public boolean gsInit(GroupspaceController gc) {
    this.gcRef = gc;
    gcRef.registerRole(roleName, this); // Register to receive events
    gcRef.Log(roleName, "Ready.");
    return true;
  }

  /**
   * Shutdown routine.
   */
  public void gsUnload() { 
    /* Cleanup */
    if(spoolFile != null) {
      try{
	spoolFile.close();
	spoolFile = null;
      } catch(Exception e) { e.printStackTrace(); }
    }
  }

  /**
   * Run routine.
   */
  public void run() {
    /* Set up server socket */
    try {
      listeningSocket = new ServerSocket(listeningPort);
    } catch(Exception e) {
      gcRef.Log(roleName, "Failed in setting up serverSocket, "+
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
	gcRef.Log(roleName, "Failed in accept from serverSocket");
      }
    }
  }

  /**
   * Handle callbacks
   */
  public int callback(GroupspaceEvent ge) {
    System.err.println("EventPackager: received GroupspaceEvent " + ge);
    return GroupspaceCallback.CONTINUE;
  }

  /**
   * Tester.
   */
  public static void main(String args[]) {
    EventPackager ep = new EventPackager(7777, "EventPackager.spl");
    ep.run();
  } 

  /**
   * Small thread for the addShutdownHook method.
   */
  class EPShutdownThread extends Thread {
    public void run() {      
      /* Clean up the file streams */
      if(spoolFile != null) {
	System.err.println("EventPackager: shutdown detected, closing spoolFile");
	try {
	  spoolFile.close();
	  spoolFile = null;
	} catch(Exception e) { e.printStackTrace(); }
      }
    }
  }

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
	gcRef.Log(roleName,"Error establishing client connection:" + e.toString());
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
	  if(gcRef != null) {
	    /* Send the event */
	    gcRef.groupspaceEvent(new 
	      GroupspaceEvent(new EPPayload(srcID,newInput),
			      "EventDistillerIncoming", 
			      null, null, true));
	  }
	}
      } catch(SocketException e) {
	if(gcRef != null) {
	   gcRef.Log(roleName,"Client socket unexpectedly closed");
	   // Clear out TriKX--HACKO
	   gcRef.groupspaceEventNonVetoable(new
	     GroupspaceEvent(new EPPayload(srcID,"allusers null null true"),
			     "EventDistillerIncoming",
			     null,null,true));
	}
	return;
      } catch(Exception e) {
	if(gcRef != null)
	  gcRef.Log(roleName,"Error communicating with client:" + 
		    e.toString());
	e.printStackTrace();
	return;
      }
    }
  }

  public String roleName() { return this.roleName; }
}
