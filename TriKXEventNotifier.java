package psl.xues;

import psl.groupspace.*;
import java.io.*;
import java.net.*;
import java.util.*;

/**
 * TriKX EventNotifier for Xues.
 *
 * Copyright (c) 2000: The Trustees of Columbia University and the
 * City of New York.  All Rights Reserved.
 *
 * @author Janak J Parekh
 * @version 0.01 (9/7/2000)
 *
 * $Log$
 * Revision 1.3  2000-09-08 20:03:26  jjp32
 *
 * Finished network functionality in TriKXEventNotifier
 *
 * Revision 1.2  2000/09/08 19:08:27  jjp32
 *
 * Minor updates, added socket communications in TriKXEventNotifier
 *
 * Revision 1.1  2000/09/07 23:15:25  jjp32
 *
 * Added EventNotifier code; updated previous event code
 *
 */
public class TriKXEventNotifier extends EventNotifier
  implements GroupspaceService, GroupspaceCallback {

  private String roleName = "TriKXEventNotifier";
  private TriKXSendThread tst = null;
  ServerSocket recvSocket = null;
  Socket sendSocket = null;
  int recvSocketPort = -1;
  int sendSocketPort = -1;

  public TriKXEventNotifier() {
    this(-1, -1);
  }

  public TriKXEventNotifier(int listeningPort, int trikxPort) { 
    super();
    
    this.recvSocketPort = listeningPort;
    this.sendSocketPort = trikxPort;

    try {
      recvSocket = new ServerSocket(listeningPort);
    } catch(Exception e) {
      e.printStackTrace();
    }

    tst = new TriKXSendThread();
  }

  public boolean gsInit(GroupspaceController gc) {
    this.gcRef = gc;
    this.gcRef.registerRole(roleName, this);
    // Subscribe to EventPackager events
    this.gcRef.subscribeEvent(this,"TriKXEventIncoming");
    return true;
  }

  public void gsUnload() {
    // Nothing right now 
  }

  public void run() { 
    while(true) { // Accept listening connections
      try {
	Socket newRecvSocket = recvSocket.accept();
	// Start up a new thread
	new Thread(new TriKXRecvThread(newRecvSocket));
      } catch(Exception e) { e.printStackTrace(); }
    }
  }

  public int callback(GroupspaceEvent ge) {
    // Received event, talk to TriKX
    if(!(ge.getDbo() instanceof TriKXUpdateObject)) {
      System.err.println("Invalid GroupspaceEvent!");
    } else {
      tst.sendUpdate((TriKXUpdateObject)ge.getDbo());
    }

    return GroupspaceCallback.CONTINUE;
  }

  public String roleName() { return this.roleName; }
    
  /**
   * Test method
   */
  public static void main(String[] args) {
    TriKXEventNotifier ten = new TriKXEventNotifier(31338,31337);
    new Thread(ten).run();
    ten.callback(new GroupspaceEvent(new TriKXUpdateObject("drivers",java.awt.Color.blue),"TriKXEventIncoming",null,null,false));
    ten.callback(new GroupspaceEvent(new TriKXUpdateObject("drivers-isdn",java.awt.Color.red),"TriKXEventIncoming",null,null,false));
    
  }

  class TriKXRecvThread implements Runnable {
    private Socket recvSocket = null;
    private BufferedReader in = null;

    public TriKXRecvThread(Socket recvSocket) {
      this.recvSocket = recvSocket;

      try {
	in = new 
	  BufferedReader(new InputStreamReader(recvSocket.getInputStream()));
      } catch(Exception e) { e.printStackTrace(); return; }
    }

    public void run() {
      while(true) {
	try {
	  String input = in.readLine();
	  System.err.println("RECEIVED " + input + "FROM TRIKX");
	} catch(Exception e) {
	  e.printStackTrace(); return;
	}
      }
    }
  }

  class TriKXSendThread implements Runnable {
    private Socket s = null;
    private ObjectOutputStream oos = null;
    private Vector sendQueue = null;
    private Thread tstExecutionContext = null;

    public TriKXSendThread() {
      try {
	s = new Socket("localhost",sendSocketPort);
	oos = new ObjectOutputStream(s.getOutputStream());
      } catch(Exception e) {
	e.printStackTrace(); return;
      }
      sendQueue = new Vector();

    }

    public void run() {
      this.tstExecutionContext = Thread.currentThread();
      TriKXUpdateObject tuo;
      while(true) {
	// Is there stuff in the vector?
	if(sendQueue.size() == 0) {
	  try {
	    Thread.currentThread().sleep(1000);
	  } catch(InterruptedException ie) { ; }
	}
	synchronized(sendQueue) {
	  // Try sending stuff in the vector
	  tuo = (TriKXUpdateObject)sendQueue.remove(0);
	}
	try {
	  oos.writeObject(tuo);
	} catch(Exception e) { 
	  e.printStackTrace();
	}
      }
    }

    public void sendUpdate(TriKXUpdateObject tuo) {
      synchronized(sendQueue) {
	sendQueue.addElement(tuo);
      }
      if(tstExecutionContext != null) tstExecutionContext.interrupt();
    }      
  }
}
