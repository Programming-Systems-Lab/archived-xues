package psl.xues;

import psl.groupspace.*;
import java.io.*;
import java.util.*;

/**
 * EventNotifier for Xues.
 *
 * Copyright (c) 2000: The Trustees of Columbia University and the
 * City of New York.  All Rights Reserved.
 *
 * @author Janak J Parekh
 * @version 0.01 (9/7/2000)
 *
 * $Log$
 * Revision 1.1  2000-09-07 23:15:25  jjp32
 *
 * Added EventNotifier code; updated previous event code
 *
 */
public class TriKXEventNotifier extends EventNotifier
  implements GroupspaceService, GroupspaceCallback {

  private String roleName = "TriKXEventNotifier";

  public TriKXEventNotifier() { 
    super();
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
  }

  public int callback(GroupspaceEvent ge) {
    // Received event, talk to TriKX

  }

  public String roleName() { return this.roleName; }
    
}
