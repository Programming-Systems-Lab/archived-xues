package psl.xues;

import psl.groupspace.*;
import java.io.*;

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
 * Revision 1.1  2000-09-07 19:30:49  jjp32
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
  Stack eventProcessStack = null;

  /**  My execution context */
  Thread edContext = null;

  public EventDistiller() { 
    // Nothing right now
  }

  public boolean gsInit(GroupspaceController gc) {
    this.gcRef = gc;
    this.gcRef.registerRole(roleName, this);
    // Subscribe to EventPackager events
    this.gcRef.subscribeEvent(this,"EventPackagerIncoming");
    return true;
  }

  public void gsUnload() {
    // Nothing right now 
  }

  public void run() { 
    // Set the current execution context, so if the callback is called
    // it can wake up a sleeping distiller
    edContext = Thread.getCurrentThread();

    while(true) {
      // Poll for events to process
      if(eventProcessStack.empty()) try {
	Thread.sleep(1000);
      } catch(InterruptedException ie) { ; }

    }
  }

  public int callback(GroupspaceEvent ge) {
    if(ge.getDescription().equals("EventPackagerIncoming") {
      // Add the event onto the stack and then wake up the distiller
      eventProcessStack.add(ge);
      edContext.interrupt();
    } else if(ge.getDescription().equals("MetaparserResult") {
      // Need to deal with this
      System.err.println("METAPARSER RESULT: " + ge);   
    }
   
    // And we are done
    return GroupspaceCallback.CONTINUE;
  }

  public String roleName() { return this.roleName; }
    
}
