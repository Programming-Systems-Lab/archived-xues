
package psl.xues;

import java.io.*;
import java.util.*;
import siena.*;

/**
 * Class to specify a machine template.
 *
 * Copyright (c) 2000: The Trustees of Columbia University in the
 * City of New York.  All Rights Reserved.
 *
 * @author Janak J Parekh
 * @version 0.9
 *
 * $Log$
 * Revision 1.1  2001-01-28 22:58:58  jjp32
 *
 * Wildcard support has been added
 *
 */
class EDStateMachineSpecification implements Notifiable {
  private Vector stateArray;
  private Notification action;
  private Siena siena;
  private EDStateManager edsm;

  /**
   * Basic CTOR.  Make sure to add states.
   */
  public EDStateMachineSpecification(Siena siena, EDStateManager edsm) {
    this.siena = siena;
    this.edsm = edsm;
    stateArray = new Vector();
  }

  /** Demo test factory, don't use otherwise */
  public static EDStateMachineSpecification buildDemoSpec(Siena siena,
							  EDStateManager edsm)
  { 
    EDStateMachineSpecification edsms = 
      new EDStateMachineSpecification(siena,edsm);
    edsms.stateArray.addElement(new EDState("temperature","60",-1));
    edsms.action = new Notification();
    edsms.action.putAttribute("itworked","true");
    edsms.subscribe();
    return edsms;
  }

  /**
   * Add a state.
   *
   * NOTE! For this specification to become active, you must set the action
   * AFTER adding states.
   *
   * @param e The state.
   */
  public void addState(EDState e) {
    stateArray.addElement(e);
  }

  /**
   * Set action.  (Only one action for now)
   *
   * NOTE! IMPORTANT!  You *must* set an action for this state machine
   * to become live.  Additionally, the state machine assumes that
   * setting the action implicitly tells it that the states have been set up.
   * You may add states later, but be forewarned there may already be state
   * machines executing on the current state setup.
   */
  public void setAction(String attr, AttributeValue val) {
    action = new Notification();
    action.putAttribute(attr,val);
    subscribe(); // Do first event subscription
  }

  /**
   * Subscribe based on the first state.  This way, we can create
   * instances when necessary.
   */
  public void subscribe() {
    try {
      siena.subscribe(((EDState)stateArray.elementAt(0)).buildSienaFilter(),
		      this);
    } catch(SienaException e) {
      e.printStackTrace();
    }
  }

  public void notify(Notification n) {
    if(EventDistiller.DEBUG) 
      System.err.println("[EDStateManagerSpecification] Received notification " + n);
    // Create the appropriate state machine(s).  We assume the state
    // machine will register itself with the manager.
    EDStateMachine sm = new EDStateMachine(siena, edsm, stateArray, 1,
					   action);
  }

  /** Unused Siena construct. */
  public void notify(Notification[] s) { ; }
}
