
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
 * Revision 1.6  2001-04-03 01:09:14  eb659
 *
 *
 * OK this is my first upload...
 * Basically, most of the dynamic rulebase stuff has been accomplished.
 * the principal methods are in EDStatemanaged, but most of the files in ED
 * had to be modified, at least in some small way
 * enrico
 *
 * Revision 1.5  2001/01/30 06:26:18  jjp32
 *
 * Lots and lots of updates.  EventDistiller is now of demo-quality.
 *
 * Revision 1.4  2001/01/30 02:39:36  jjp32
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
    
    /** the name of the rule represented. */
    private String name;

    /** ID of this specification - an integer */
    private String myID = null;
    /** Counter for ID tagging of EDStateMachines */
    private int counter = 0;

  /**
   * Basic CTOR.  Make sure to add states.
   */
  public EDStateMachineSpecification(String name, String myID,
				     Siena siena, EDStateManager edsm) {
      this.name = name;
      this.myID = myID;
      this.siena = siena;
      this.edsm = edsm;
      stateArray = new Vector();
  }

  /** Demo test factory, don't use otherwise */
  public static EDStateMachineSpecification buildDemoSpec(Siena siena,
							  EDStateManager edsm)
  { 
    EDStateMachineSpecification edsms = 
      new EDStateMachineSpecification("demoTest", "foo",siena,edsm);
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
    EDStateMachine sm = new EDStateMachine(this, myID + ":" + (counter++),
					   siena, edsm, stateArray, n,
					   action);
  }

    /** Unused Siena construct. */
    public void notify(Notification[] s) { ; }

    /**
     * Returns the name of this SMSpecification.
     * @return the name of this SMSpecification
     */
    public String getName(){
	return name;
    }

    /** Unsubscribe from siena. */
    public void unsubscribe(){
	try { siena.unsubscribe(this); } 
	catch(SienaException e) { e.printStackTrace(); }
    }

    /** @return the XML representation of this object */
    public String toXML(){
	String s = "<rule name=\"" + name + "\">\n<states>\n";
	
	// add states
	for (int i = 0; i < stateArray.size(); i++){
	    s = s + ((EDState)stateArray.get(i)).toXML();
	}
	s += "</states>\n <actions>";

	// add action
	s += notificationToXML(action);
	s += "</states>\n <actions>\n";

	return s;
    }

    /** 
     * Returns the XML representation of a Notification.
     * @param the notification to represent
     * @return the XML representation of a Notification
     */
    public static String notificationToXML(Notification n){
	String s = "<notification>\n";
	Iterator iterator = n.attributeNamesIterator();
	while (iterator.hasNext()){
	    String notifName = iterator.next().toString();
	    s = s + "\t<attribute name=\"" + notifName + "\" value=\"" + 
		n.getAttribute(notifName).stringValue() + "\"/>\n";
	      }
	s += "</notification>\n";
	return s;
    }
}
