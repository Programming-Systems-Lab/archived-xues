
package psl.xues;

import psl.kx.KXNotification;
import siena.*;
import java.util.*;

/**
 * Event Distiller State Machine.
 *
 * Copyright (c) 2001: The Trustees of Columbia University and the
 * City of New York.  All Rights Reserved.
 *
 * TODO:
 * - Not interested in notification of state zero (CTOR)
 * - Multiple actions in case of notification
 *
 * @author Janak J Parekh (jjp32@cs.columbia.edu)
 * @version 0.5
 *
 * $Log$
 * Revision 1.4  2001-01-29 05:22:53  jjp32
 *
 * Reaper written - but it's probably a problem
 *
 * Revision 1.3  2001/01/29 02:14:36  jjp32
 *
 * Support for multiple attributes on a output notification added.
 *
 * Added Workgroup Cache test rules
 *
 * Revision 1.2  2001/01/28 22:58:58  jjp32
 *
 * Wildcard support has been added
 *
 * Revision 1.1  2001/01/22 02:11:54  jjp32
 *
 * First full Siena-aware build of XUES!
 *
 */
public class EDStateMachine implements Notifiable {
  private int currentState;
  private Vector states = null;
  private Siena siena = null;
  private Notification action = null;
  private EDStateManager el = null;
  /**
   * Wildcard binding hashtable.  If there are wildcards in states that must
   * match later states, we store them in this table.  In the future, this
   * might be used for more than just wildcards.
   */ 
  Hashtable wildHash = null;

  /**
   * CTOR.  EDStateMachines *must* be launched through a StateManager.
   */
  EDStateMachine(Siena siena, 
		 EDStateManager el, 
		 Vector stateArray,
		 int startState, 
		 Notification action) {
    this.siena = siena;
    this.el = el;
    this.action = new Notification(action);  // Make a copy
    // Are we already done?
    if(stateArray.size() == startState) { // All states done, we work, go home
      finish();
      return;
    }
    this.wildHash = new Hashtable();
    // Add the states, set up notifications.  Why copy the states
    // here?  Since we have to create notifications anyway...
    states = new Vector();
    for(int i=0; i < stateArray.size(); i++) {
      // Clone 'em
      addState(new EDState((EDState)stateArray.elementAt(i),this));
    }
    this.currentState = startState;
    // Now register ourselves with the StateManager.  Doing so enables
    // us to be garbage-collected intelligently (in the future).
    el.addMachine(this);
  }
    
  /**
   * Add a state.  WARNING! This machine *WILL* adjust the state.  If you 
   * need a deep copy, make one!
   */
  public void addState(EDState s) {
    // Assign ourselves as the "owner" state machine
    s.assignOwner(this);
    states.addElement(s);
    try {
      siena.subscribe(s.buildSienaFilter(),this);
    } catch(SienaException e) { e.printStackTrace(); }
  }

  /** 
   * Add an action.  If the notification does not exist it will be
   * created the first time.  Use setAction if you want to *replace*
   * the notification with a new one.
   */
  public void addAction(String attr, String val) {
    if(action == null) {
      action = new Notification();
    }
    action.putAttribute(attr, val);
  }

  /**
   * (Re)set the action.
   */
  public void setAction(String attr, String val) {
    action = null;
    addAction(attr,val);
  }
    
  public void notify(Notification n) {
    if(EventDistiller.DEBUG) 
      System.err.println("[EDStateMachine] Received notification " + n);
    // Check it against the current state - but to do this, we need
    // the prev state
    EDState prevState = (currentState == 0 ? null : 
			 (EDState)states.elementAt(currentState-1));
    if(((EDState)states.elementAt(currentState)).validate(n,prevState)) {
      // Yes!
      currentState++;
      // Did we pass the last state?
      if(states.size() == currentState) {
	// Yes - send action
	finish();
      }
    }
  }

  /** Unused Siena construct. */
  public void notify(Notification[] s) { ; }

  /**
   * Finish up the state machine.  Yes, this could be inlined, but
   * why?  Also, another alternative is to have EDManager do the
   * unsubscription.  Since we subscribe in the first place it seems
   * to make better sense to handle our own unsubscriptions (but this
   * behavior may change someday...)
   */
  private void finish() {
    // Remove all notifications
    try {
      siena.unsubscribe(this);
    } catch(SienaException e) { e.printStackTrace(); }

    // Do we need to amend the Notification?  Iterate through all
    // attribute values and fill in any wildcard hashes in.
    Iterator i = action.iterator();

    while(i.hasNext()) {
      String attr = (String)i.next();
      AttributeValue val = action.getAttribute(attr);
      if(val.getType() == AttributeValue.STRING &&
	 val.stringValue().startsWith("*")) {
	String key = val.stringValue().substring(1);
	String bindVal = (String)wildHash.get(key);
	if(bindVal != null) {
	  // Replace this attributeValue
	  action.putAttribute(attr,bindVal);
	}
      }
    }

    // Call our manager and tell them we're finished, and hand them
    // the (modified) notification to send
    el.finish(this, action);
  }

  /**
   * Reap ourselves if necessary.  This involves deregistering us from
   * EDStateManager stateMachines array, and unsubscribing from
   * Siena.  The former is done by our StateManager oh-so-nicely.
   *
   * The Grim Reaper (in EventDistiller) will eventually get to us by
   * calling this method.
   */
  public boolean reap() {

    boolean reap = false;

    /* Should never happen, but easy boundary cases */
    if(currentState == 0) reap = false;
    else if(currentState == states.size()) reap = true;
    
    /* Now try calling validateTimebound, assume the current state
     * occurs --NOW--, and if that fails, then we MUST reap.
     */
    else if(((EDState)states.elementAt(currentState)).
	    validateTimebound((EDState)states.elementAt(currentState-1),
			      System.currentTimeMillis() - 
			      EventDistiller.reapFudgeMillis) == false) {
      reap = true;
    }
    
    /* Now, shall we reap?  :) */
    if(reap) {
      // Let's go
      try {
	siena.unsubscribe(this);
      } catch(SienaException e) { e.printStackTrace(); }
    }

    return reap;    
  }
}
