
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
 * Revision 1.4  2001-01-30 02:39:36  jjp32
 *
 * Added loopback functionality so hopefully internal siena gets the msgs
 * back
 *
 * Revision 1.3  2001/01/29 04:58:55  jjp32
 *
 * Each rule can now have multiple attr/value pairs.
 *
 * Revision 1.2  2001/01/29 02:14:36  jjp32
 *
 * Support for multiple attributes on a output notification added.
 *
 * Added Workgroup Cache test rules
 *
 * Revision 1.1  2001/01/28 22:58:58  jjp32
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
    EDState e = new EDState(-1);
    e.add("temperature","60");
    edsms.stateArray.addElement(e);
    edsms.action = new Notification();
    edsms.action.putAttribute("itworked","true");
    edsms.subscribe();
    return edsms;
  }

  /**
   * Add a state.
   *
   * NOTE! For this specification to become active, you must subscribe() 
   * AFTER adding states.
   *
   * @param e The state.
   */
  public void addState(EDState e) {
    stateArray.addElement(e);
  }

  /**
   * Add an action.  If the notification does not exist it will be
   * created the first time.  Use setAction if you want to *replace*
   * the notification with a new one.
   */
  public void addAction(String attr, AttributeValue val) {
    if(action == null) 
      action = new Notification();
    action.putAttribute(attr,val);
  }

  /**
   * (Re)set the action.
   */
  public void setAction(String attr, String val) {
    action = null;
    addAction(attr,new AttributeValue(val));
  }
 
  /**
   * Subscribe based on the first state.  This way, we can create
   * instances when necessary.
   */
  public void subscribe() {
    try {
      Filter f = ((EDState)stateArray.elementAt(0)).buildSienaFilter();
      if(EventDistiller.DEBUG)
	System.err.println("EDStateMachSpec: Subscribing with filter " + f);
      siena.subscribe(f,this);
    } catch(SienaException e) {
      e.printStackTrace();
    }
  }

  public void notify(Notification n) {
    if(EventDistiller.DEBUG) 
      System.err.println("EDStateManagerSpecification: Received notification " + n);
    // Create the appropriate state machine(s).  We assume the state
    // machine will register itself with the manager.
    EDStateMachine sm = new EDStateMachine(siena, edsm, stateArray, 1,
					   action);
  }

  /** Unused Siena construct. */
  public void notify(Notification[] s) { ; }
}
