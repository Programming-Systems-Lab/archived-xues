package psl.xues;

import psl.groupspace.*;
import java.net.*;

/** 
 * EventPackager for Xues.
 *
 * TODO: Listening on local bus.
 *
 * @author Janak J Parekh <jjp32@cs.columbia.edu>
 * @version 0.01
 */
public class EventPackager implements GroupspaceService,
				      GroupspaceEventListener {
  GroupspaceController gcRef = null;
  int listeningPort = -1;
  ServerSocket listeningSocket = null;
  String spoolFilename;
  ObjectOutputStream spoolFile;
  int srcIDCounter = 0;

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
  public void groupspaceEvent(GroupspaceEvent ge) throws
  PropertyVetoException {
    
  }
 
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
    public EPClientThread(int srcID, Socket clientSocket) {

    }
    
    public void run() {


    }
  }
}
