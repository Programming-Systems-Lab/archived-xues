package psl.xues;

import psl.groupspace.*;
import java.io.*;
import java.net.*;
import java.util.*;
import psl.trikx.impl.TriKXUpdateObject;

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
 * Revision 1.4  2000-09-08 22:40:43  jjp32
 *
 * Numerous server-side bug fixes.
 * Removed TriKXUpdateObject, psl.trikx.impl now owns it to avoid applet permission hassles
 *
 * Revision 1.3  2000/09/08 20:03:26  jjp32
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
    new Thread(tst).start();
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
	System.err.println("Starting up new Receive Thread");
	new Thread(new TriKXRecvThread(newRecvSocket)).start();
      } catch(Exception e) { e.printStackTrace(); }
    }
  }

  public int callback(GroupspaceEvent ge) {
    // Received event, talk to TriKX
    if(!(ge.getDbo() instanceof TriKXUpdateObject)) {
      System.err.println("Invalid GroupspaceEvent!");
    } else {
      System.err.println("Requesting tst to send " + ge.getDbo());
      tst.sendUpdate((TriKXUpdateObject)ge.getDbo());
    }

    return GroupspaceCallback.CONTINUE;
  }

  public String roleName() { return this.roleName; }
    
  /**
   * Test method for TriKX
   */
  public static void main(String[] args) {
    TriKXEventNotifier ten = new TriKXEventNotifier(31337,31338);
    new Thread(ten).start();
    System.err.println("TEN is running");
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
	  if(input == null) {
	    System.err.println("TRT: Connection closed");
	    in.close();
	    recvSocket.close();
	    return;
	  }
	  System.err.println("RECEIVED " + input + " FROM TRIKX");
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
      sendQueue = new Vector();
    }

    public void run() {
      this.tstExecutionContext = Thread.currentThread();
      TriKXUpdateObject tuo;

      while(true) {
	/* Connect repeatedly to TriKX until connection is made */
	try {
	  s = new Socket("localhost",sendSocketPort);
	  oos = new ObjectOutputStream(s.getOutputStream());
	} catch(ConnectException e) {
	  System.err.println("Connection to TriKX refused, will try again");
	  try {
	    Thread.currentThread().sleep(1000);
	  } catch(InterruptedException ie) { ; }
	  continue;
	} catch(Exception e) {
	  e.printStackTrace(); return;
	}
	
	/* Connection made, continue to communicate until the remote side
	 * fails
	 */
	while(true) {
	  // Is there stuff in the vector?
	  while(sendQueue.size() == 0) {
	    try {
	      System.err.println("TSTSendThread sleeping");
	      Thread.currentThread().sleep(1000);
	    } catch(InterruptedException ie) { ; }
	  }
	  System.err.println("TSTSendThread awake, size == " + 
			     sendQueue.size());
	  synchronized(sendQueue) {
	    // Try sending stuff in the vector
	    System.err.println("TSTSendThread remove from queue");
	    tuo = (TriKXUpdateObject)sendQueue.remove(0);
	  }
	  try {
	    System.err.println("Sending " + tuo + " now!!!!");
	    oos.writeObject(tuo);
	  } catch(IOException e) {
	    System.err.println("Connection with remote side failed");
	    break;
	  } catch(Exception e) { 
	    e.printStackTrace();
	  }
	}
      }
    }

    public void sendUpdate(TriKXUpdateObject tuo) {
      synchronized(sendQueue) {
	System.err.println("TSTSendThread adding to queue");
	sendQueue.addElement(tuo);
      }
      if(tstExecutionContext != null) tstExecutionContext.interrupt();
    }      
  }
}
