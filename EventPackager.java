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
 *
 * @author Janak J Parekh <jjp32@cs.columbia.edu>
 * @version 0.01 (9/7/2000)
 *
 * $Log$
 * Revision 1.2  2000-09-07 19:30:49  jjp32
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
    this.spoolFilename = spoolFile;
    if(this.spoolFilename != null) { 
      try {
	this.spoolFile = new ObjectOutputStream(new
	  FileOutputStream(this.spoolFilename));	
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
    gc.registerRole(roleName, this); // Register to receive events
    gc.Log(roleName, "Ready.");
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
    listeningSocket = new ServerSocket(listeningPort);

    /* Listen for connection */
    while(true) {
      Socket newSock = listeningSocket.accept();
      /* Hand the hot potato off! */
      new Thread(new EPClientThread(srcIDCounter++,newSock));
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
	gc.Log("Error establishing client connection:" + e.toString());
	e.printStackTrace();
      }
    }
    
    public void run() {
      /* Wait for stuff - then write it to disk - and to the bus */
      try {
	String newInput = in.readLine();
	if(spoolFile != null) spoolFile.writeObject(newInput);
	if(gcRef != null) {
	  /* Send the event */
	  gcRef.groupspaceEvent(new GroupspaceEvent(newInput,
						    "EventPackagerIncoming", 
						    null, null, true));
	}
      } catch(Exception e) {
	gc.Log("Error communicating with client:" + e.toString());
	e.printStackTrace();
      }
    }
  }

  public String roleName() { return this.roleName; }
}
