
package psl.xues;

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
 *
 * @author Janak J Parekh (jjp32@cs.columbia.edu)
 * @version 0.5
 *
 * $Log$
 * Revision 1.1  2001-01-22 02:11:54  jjp32
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
   * CTOR.  EDStateMachines *must* be launched through a StateManager.
   */
  EDStateMachine(Siena siena, 
		 EDStateManager el, 
		 Vector stateArray,
		 int startState, 
		 Notification action) {
    this.siena = siena;
    this.el = el;
    this.action = action;
    // Are we already done?
    if(stateArray.size() == startState) { // All states done, we work, go home
      finish();
      return;
    }
    // Add the states, set up notifications.  Why copy the states
    // here?  Since we have to create notifications anyway...
    states = new Vector();
    for(int i=0; i < stateArray.size(); i++) {
      // Clone 'em
      addState(new EDState((EDState)stateArray.elementAt(i)));
    }
    this.currentState = startState;
    // Now register ourselves with the StateManager.  Doing so enables
    // us to be garbage-collected intelligently (in the future).
    el.addMachine(this);
  }
    
  public void addState(EDState s) {
    states.addElement(s);
    try {
      siena.subscribe(s.buildSienaFilter(),this);
    } catch(SienaException e) { e.printStackTrace(); }
  }

  public void setAction(String attr, String val) {
    action = new Notification();
    action.putAttribute(attr, val);
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
    // Call our manager and tell them we're finished
    el.finish(this, action);
  }
}
