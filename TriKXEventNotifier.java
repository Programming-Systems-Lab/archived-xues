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
 * Revision 1.2  2000-09-08 19:08:27  jjp32
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

  }

  public String roleName() { return this.roleName; }
    
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
	String input = in.readLine();
	System.err.println("RECEIVED " + input + "FROM TRIKX");
      } catch(Exception e) {
	e.printStackTrace(); return;
      }
    }
  }
}
