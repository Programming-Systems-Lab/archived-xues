
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
 * Revision 1.8  2001-05-21 00:43:04  jjp32
 * Rolled in Enrico's changes to main Xues trunk
 *
 * Revision 1.5.4.7  2001/05/06 05:31:07  eb659
 *
 * Thoroughly re-tested all dynamic rulebase and fixed so that it
 * works with the new arch.
 *
 * Revision 1.5.4.6  2001/05/06 03:54:27  eb659
 *
 * Added support for checking multiple parents, and independent wild hashtables
 * for different paths. ED now has full functionality, and resiliency.
 * Now doing some additional testing for event sequences that actually use
 * the OR representation, and re-testing the dynamic rulebase, to make sure
 * it still works after the changes made.
 *
 * Revision 1.5.4.5  2001/05/02 00:04:27  eb659
 *
 * Tested and fixed a couple of things.
 * New architecture works, and can be tested using EDTest.
 * Reaper thread needs revision...
 * Can we get rid of internal 'loopback' notifications?
 *
 * Revision 1.5.4.4  2001/04/21 06:57:11  eb659
 *
 *
 * A lot of changes (oh boy it's 3am already).
 * Basically, the new architecture is in place, in terms of data
 * structure. Next we will add the functionality.
 *
 * Made the following changes:
 * - states and actions are parsed and stores in hashtables
 * - well with the new non-sequential states you may well have
 * multiple starting states. but then the thing of subscribing
 * the smSpecification and waiting for the first event is not so
 * neat anymore. Instead, for every spec, a new stateMachine is
 * made. Then, as soon as it starts (one of the initial states is met)
 * a new one is placed
 * - neither smSpecification nor stateMachines subscribe.
 * only the states currently waiting for a match.
 * - etc. etc.
 *
 * compiles - but no testing done so far
 *
 * coming next:
 * - new validation method for states (so that it works)
 * - new reaper method (with failure notifs, etc.)
 *
 * Revision 1.5.4.3  2001/04/18 04:37:54  eb659
 *
 *
 * New representation for states!
 *
 * added TestRules2.xml (the spamblocker rule in the new format, and a new hexagonal rule to test OR representation...
 * Updated DistillerRule.xsd to reflect the new representation.
 * modified EDStateManager (the parser) to parse the new information about the states; and, of course, EDState - the constructor, and the toXML() method, and new fields and so on
 *
 * Tested: the new states are parsed correctly.
 * Coming next:
 * 1) read in notifications and failure notifications (all happily sharing the same hashtable!)
 * 2) the new architecture... or, the new representation becomes operative
 *
 * Revision 1.5.4.2  2001/04/06 00:15:07  eb659
 *
 *
 * 1) changed AddTestRule and EDStateManager to conform to the new standard notification types set in KXNotification
 *
 * 2) changed schema to allow multiple notifications
 *
 * 3) changed the following:
 *
 * EDStateMachineSpecification
 * EDStateManager (the parser, and finish())
 * EDStateMachine
 *
 * to allow multiple notifications, and internal notifications as well (this is what we use for loopback and higher order abstractions)
 *
 * Haven't tested it yet, but compiles.
 * enrico
 *
 * Revision 1.5.4.1  2001/04/03 01:09:13  eb659
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
class EDStateMachineSpecification {
    private Siena siena;
    private EDStateManager edsm;
    
    /** the name of the rule represented. */
    private String name;

    /** ID of this specification - an integer */
    //private String myID = null;

    /** states in this machine */
    private Hashtable states = new Hashtable();

    /** actions that are fired when certain states succeed or fail */
    private Hashtable actions = new Hashtable();

    /** Counter for ID tagging of EDStateMachines */
    private int counter = -1;

    /** The (names of the) initial states, to be subscribed
	when a machine is first instantiated. */
    String[] initialStates;

  /** Basic CTOR.  Make sure to add states, and to call findInitialStates */
  public EDStateMachineSpecification(String name, String myID,
				     Siena siena, EDStateManager edsm) {
      this.name = name;
      //this.myID = myID;
      this.siena = siena;
      this.edsm = edsm;
  }

  /** Demo test factory, don't use otherwise 
  public static EDStateMachineSpecification buildDemoSpec(Siena siena,
							  EDStateManager edsm)
  { 
    EDStateMachineSpecification edsms = 
      new EDStateMachineSpecification("demoTest", "foo",siena,edsm);
    // I had to give it some silly parameters, so that it constructs itself - eb659
    EDState e = new EDState("zzz", -1, "", "", "");
    e.add("temperature","60");
    edsms.stateArray.addElement(e);

    //    edsms.subscribe();
    return edsms;
    }*/

  /**
   * Add a state.
   *
   * NOTE! For this specification to become active, you must subscribe() 
   * AFTER adding states.
   *
   * @param e The state.
   */
  public void addState(EDState e) {
    states.put(e.getName(), e);
  }

    /** 
     * Add an Action
     * @param name the name for this action
     * @param action the action
     */
    public void addAction(String name, Notification action) {
	actions.put(name, action);
    }

    /**
     * Finds the initial states of this machine.
     * We do this by seeing which states don't
     * have any incoming edges.
     */
    public void findInitialStates() {
	Enumeration keys = states.keys();
	Vector names = new Vector();
	while (keys.hasMoreElements()) 
	    names.add(keys.nextElement());
	boolean[] hasIncomingEdges = new boolean[names.size()];

	EDState s;
	for (int i = 0; i < names.size(); i++) {
	    s = (EDState)states.get(names.get(i).toString());
	    String[] children = s.getChildren();
	    for (int j = 0; j < children.length; j++) {
		int index = names.indexOf(children[j]);
		hasIncomingEdges[index] = true;
	    }
	}
	// how many initial states?
	int n = 0;
	for (int i = 0; i < hasIncomingEdges.length; i++)
	    if (!hasIncomingEdges[i]) n++;
	initialStates = new String[n];

	// which ones?
	n = -1;
	for (int i = 0; i < names.size(); i++) 
	    if (!hasIncomingEdges[i])
		initialStates[++n] = names.get(i).toString(); 
    }

    /*
     * Add an attribute to an action.
     * We use this when parsing, so we just take the last action 
     * in the list, which is the one being constructed.
     *
    public void addActionAttribute(String attr, AttributeValue val) {
	((Notification)actions.lastElement()).putAttribute(attr,val);
	}*/

  /**
   * (Re)set the action.
   * I don't know where this is used, but we have to add
   * a parameter to specify which action we are resetting
   *
  public void setAction(String attr, String val) {
    action = null;
    addAction(attr,new AttributeValue(val));
    }*/
 
  /* we don't use these two anymore. instead, we make a new machine to begin with,
   * which subscribes itself. and any time a machine starts up (the first state is 
   * matched) it calls the manager saying to make a new instance, based on us (the specification)
   *
   * Subscribe based on the first state.  This way, we can create
   * instances when necessary.
   *
   * change this. now states subscribe themselves when they are born.
   * so just bear all states, as they come in, if they are initial states.
   * Also, we may have many initial states, so we cannot start with any specification...
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
					   actions);
					   }

    /** Unused Siena construct. 
    public void notify(Notification[] s) { ; }

    /** Unsubscribe from siena. 
    public void unsubscribe(){
	try { siena.unsubscribe(this); } 
	catch(SienaException e) { e.printStackTrace(); }
	}*/

    // standars methods

    /** @return the stateMachineManager */
    public EDStateManager getManager(){ return this.edsm; }

    /** @return the name of this SMSpecification */
    public String getName(){ return name; }

    /** @return the actions for this machine */
    public Hashtable getActions(){ return this.actions; }

    /** @return the states for this machine */
    public Hashtable getStates(){ return this.states; }

    /** @return an index - called by new instances of this specification */
    String getNewID(){ return "" + (++counter); }

    /** @return the names pf the states to subscribe initially */
    String[] getInitialStates() { return initialStates; }


    // XML representation

    /** @return the XML representation of this object */
    public String toXML(){
	// start
	String s = "<rule name=\"" + name + "\">\n";
	
	// the states
	s += "<states>\n";
	Enumeration keys = states.keys();
	while(keys.hasMoreElements()){
	    String key = keys.nextElement().toString();
	    s = s + ((EDState)states.get(key)).toXML();
	}
	s += "</states>\n";

	// the actions
	s += "<actions>";
	keys = actions.keys();
	while(keys.hasMoreElements()){
	    String key = keys.nextElement().toString();
	    s = s + actionToXML((Notification)actions.get(key), key);
	}
	s += "</actions>\n";

	// end
	s += "</rule>\n";
	if (EventDistiller.DEBUG) System.out.println(s);
	return s;
    }

    /** 
     * Returns the XML representation of an action.
     * @param n the notification to represent
     * @param name the name for this action
     * @return the XML representation of an action
     */
    public static String actionToXML(Notification n, String name){
	String s = "<notification name=\"" + name + "\">\n";
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
