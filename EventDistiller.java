package psl.xues;

import psl.groupspace.*;
import psl.trikx.impl.TriKXUpdateObject;
import java.io.*;
import java.util.*;

/**
 * The Event Distiller.
 *
 * Copyright (c) 2000: The Trustees of Columbia University and the
 * City of New York.  All Rights Reserved.
 *
 * @author Janak J Parekh
 * @version 0.01 (9/7/2000)
 *
 * $Log$
 * Revision 1.5  2000-09-09 15:13:49  jjp32
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
public class EventDistiller implements GroupspaceService,
				       GroupspaceCallback {
  GroupspaceController gcRef = null;
  private String roleName = "EventDistiller";

  /**
   * We maintain a stack of events to process - this way incoming
   * callbacks don't get tied up - they just push onto the stack.
   */
  Vector eventProcessQueue = null;

  /** My main execution context */
  Thread edContext = null;

  public EventDistiller() { 
    eventProcessQueue = new Vector();
  }

  public boolean gsInit(GroupspaceController gc) {
    this.gcRef = gc;
    this.gcRef.registerRole(roleName, this);
    // Subscribe to EventPackager events
    this.gcRef.subscribeEvent(this,"EventDistillerIncoming");
    this.gcRef.subscribeEvent(this,"MetaparserResult");
    gcRef.Log(roleName,"Ready.");
    return true;
  }

  public void gsUnload() {
    // Nothing right now 
  }

  public void run() { 
    // Set the current execution context, so if the callback is called
    // it can wake up a sleeping distiller
    edContext = Thread.currentThread();

    while(true) {
      // Poll for events to process
      if(eventProcessQueue.size() == 0) {
	try {
	  Thread.sleep(1000);
	} catch(InterruptedException ie) { ; }
	continue;
      } else {
	// Otherwise pull them off
	synchronized(eventProcessQueue) {
	  GroupspaceEvent ge = (GroupspaceEvent)eventProcessQueue.remove(0);
	  System.err.println("EventDistiller: " + ge);

	  try {
	    // Unwrap the stuff into separate strings
	    EPPayload epp = (EPPayload)ge.getDbo();
	    StringTokenizer st = new StringTokenizer((String)epp.getPayload());
	    st.nextToken();
	    String oldRoomName = st.nextToken();
	    // Horrible hack because of current TriKX implementation
	    if(oldRoomName.equals("Linux-2.0.36")) oldRoomName = "root";
	    String newRoomName = st.nextToken();
	    // Horrible hack because of current TriKX implementation
	    if(newRoomName.equals("Linux-2.0.36")) newRoomName = "root";
	    boolean success = Boolean.valueOf(st.nextToken()).booleanValue();
	    
	    // IF successful do the switch
	    if(success == true) {

	      // Clear out old room name
	      gcRef.groupspaceEvent(new 
		GroupspaceEvent(new 
		  TriKXUpdateObject(oldRoomName,null),
				"TriKXEventIncoming",null,null,false));
	      
	      // Set up new room name
	      gcRef.groupspaceEvent(new
		GroupspaceEvent(new
		  TriKXUpdateObject(newRoomName, java.awt.Color.green),
				"TriKXEventIncoming",null,null,false));
	    } else {
	      for(int i=0; i < 5; i++) {
		// Mark the failed room red for a few seconds
		gcRef.groupspaceEvent(new
		  GroupspaceEvent(new
		    TriKXUpdateObject(newRoomName, java.awt.Color.red),
				  "TriKXEventIncoming",null,null,false));
		Thread.currentThread().sleep(500);
		gcRef.groupspaceEvent(new
		  GroupspaceEvent(new
		    TriKXUpdateObject(newRoomName, java.awt.Color.black),
				  "TriKXEventIncoming",null,null,false));
		Thread.currentThread().sleep(500);
	      }
	      gcRef.groupspaceEvent(new
		GroupspaceEvent(new
		  TriKXUpdateObject(newRoomName, null),
				"TriKXEventIncoming",null,null,false));
	      
	    }
	      

	    
	    //	  ten.callback(new GroupspaceEvent(new TriKXUpdateObject("drivers",java.awt.Color.blue),"TriKXEventIncoming",null,null,false));
	  } catch(Exception e) { e.printStackTrace(); }
	}
      }
    }
  }

  public int callback(GroupspaceEvent ge) {
    if(ge.getEventDescription().equals("EventDistillerIncoming")) {
      // Add the event onto the stack and then wake up the distiller
      synchronized(eventProcessQueue) {
	eventProcessQueue.addElement(ge);
      }
      edContext.interrupt();
    } else if(ge.getEventDescription().equals("MetaparserResult")) {
      // Need to deal with this
      System.err.println("METAPARSER RESULT: " + ge);   
    }
   
    // And we are done
    return GroupspaceCallback.CONTINUE;
  }

  public String roleName() { return this.roleName; }
    
}
