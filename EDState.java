
package psl.xues;

import java.util.*;
import siena.*;

/**
 * things to do:
 * -the bear method:
 * set parent, subscribe, wait...
 * -the kill/reap method
 * alive to false
 * -the notify method
 * if succeed, send notifs, and bear children
 * etc...
 * 
 * Individual Event Distiller state machine state.  A state is matched
 * against a String attribute-String value pair.  A upper bound
 * timestamp may be applied.
 *
 * Copyright (c) 2001: The Trustees of Columbia University and the
 * City of New York.  All Rights Reserved.
 *
 * @author Janak J Parekh (jjp32@cs.columbia.edu)
 * @version 1.0
 *
 * $Log$
 * Revision 1.12  2001-06-03 01:11:13  jjp32
 *
 * Updates, tweaks, hacks for demo.  Also now makes sanity check on command line params
 *
 * Revision 1.11  2001/06/02 18:22:56  jjp32
 *
 * Fixed bug where wildHash would not get assigned if derivative state never got a notification
 *
 * Revision 1.10  2001/05/21 00:43:04  jjp32
 * Rolled in Enrico's changes to main Xues trunk
 *
 * Revision 1.7.4.6  2001/05/06 03:54:27  eb659
 *
 * Added support for checking multiple parents, and independent wild hashtables
 * for different paths. ED now has full functionality, and resiliency.
 * Now doing some additional testing for event sequences that actually use
 * the OR representation, and re-testing the dynamic rulebase, to make sure
 * it still works after the changes made.
 *
 * Revision 1.7.4.5  2001/05/02 00:04:27  eb659
 *
 * Tested and fixed a couple of things.
 * New architecture works, and can be tested using EDTest.
 * Reaper thread needs revision...
 * Can we get rid of internal 'loopback' notifications?
 *
 * Revision 1.7.4.4  2001/05/01 04:21:55  eb659
 *
 * Added revised reaper thread and re-enabled validation.
 * New version compiles and has 95% of the expected functionality. Next:
 * 1) testing & debugging
 * 2) if time permits, cool stuff like allowing multiple parents for each state, etc.
 *
 * Revision 1.7.4.3  2001/04/21 06:57:11  eb659
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
 * Revision 1.7.4.2  2001/04/18 04:37:54  eb659
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
 * Revision 1.7.4.1  2001/04/03 01:09:13  eb659
 *
 *
 * OK this is my first upload...
 * Basically, most of the dynamic rulebase stuff has been accomplished.
 * the principal methods are in EDStatemanaged, but most of the files in ED
 * had to be modified, at least in some small way
 * enrico
 *
 * Revision 1.7  2001/03/21 18:14:02  jjp32
 *
 * Fixed comment
 *
 * Revision 1.6  2001/01/30 06:26:18  jjp32
 *
 * Lots and lots of updates.  EventDistiller is now of demo-quality.
 *
 * Revision 1.5  2001/01/30 02:39:36  jjp32
 *
 * Added loopback functionality so hopefully internal siena gets the msgs
 * back
 *
 * Revision 1.4  2001/01/29 04:58:55  jjp32
 *
 * Each rule can now have multiple attr/value pairs.
 *
 * Revision 1.3  2001/01/29 04:18:42  jjp32
 *
 * Lots of updates.  Doesn't compile yet, hopefully it will by the time I'm home :)
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
public class EDState implements Notifiable{

    /** The name of this state. */
    private String name;

    /**
     * The ID of this state, used for debugging purposes.
     * This is in the form 'machineSpec:machineInstance:stateName'.
     */
    private String myID;
  
    /** Relative timebound from previous state. */
    private long tb;

    /** The list of (names of) the  states that may success this one */
    private String[] children;

    /** The list of (names of) notifications we will send if we are matched. */
    private String[] actions;

    /** The list of (names of) notifications we will send if we fail to be matched. */
    private String[] fail_actions;

    /* Hash of attribute/value pairs relevant to this state */
    private Hashtable attributes;

    /** Whether this state is currently subscribed and waiting to be matched */
    private boolean alive = false;

    /** 
     * Timestamp this state has fired in.  Created and used during
     * timebound validation.
     */
    private long ts;

    /** 
     * The list of states that preceded this one. We need this to 
     * be able to validate the timebound (and wildcards) 
     * against all possible parents that were matched and bore us.   
     */
    private Vector parents;

    /**
     * Wildcard binding hashtable.  If there are wildcards in states that must
     * match later states, we store them in this table. The variable has package
     * access, but if a state needs to inherit its parent's wildHash, use
     * getWildHash(), which checks whether it is necessary to copy the table.
     */ 
    Hashtable wildHash;

    /** the Siena bus; this is passed to us by our parent, when this state is subscribed */
    private Siena siena = null;
    
    /** The state machine that "ownes" us. */
    private EDStateMachine sm = null;

    /**
     * Constructs a new EDState. This constructor is used in the definition
     * of the stateMachineSpecification, when parsing the rule, so the states that
     * are actually subscribed are always created through:
     * public EDState(EDState e, EDStateMachine sm, Siena siena)
     * wnere 'e' is the state template built with the present constructor.
     *
     * @param name the name for this state
     * @param tb Elapsed time bound of this state (e.g. how long to wait
     *           from the previous state).  -1 implies no time bound.
     * @param childrenList the list of children for this node
     * @param actionsList the list of actions to fire when this node is matche
     * @param failActionList the list of actions to fire if this node is not matched
     */
    public EDState(String name, int tb, 
		   String childrenList, 
		   String actionsList, 
		   String failActionsList) {
	this.name = name;
	this.tb = tb;
	this.ts = -1;  // Unvalidated state.
	this.attributes = new Hashtable();
	
	children = listToArray(childrenList);
	actions = listToArray(actionsList);
	fail_actions = listToArray(failActionsList);

	if (EventDistiller.DEBUG) 
	    System.out.println("/nconstructed state '" 
			       + name + ":'\n" + toXML());
    }

    /** are we sing this anywere?
     * Clone CTOR.
     *
    public EDState(EDState e) {
	/* Ouch, but I have to do this 
	this.attributes = (Hashtable)e.attributes.clone();
	this.tb = e.tb;
	this.ts = e.ts;
	this.name = e.name;

	this.children = e.children;
	this.actions = e.actions;
	this.fail_actions = e.fail_actions;
    }*/

    /**
     * Clone-and-assign-state-machine CTOR. This constructor 
     * is used when a stateMachine is instantiated.
     * @param e the state in the underlying stateMachineSpecification
     * @param sm the stateMachine to which this state belongs
     * @param siena the siena bus through which we are to subscribe
     */
    public EDState(EDState e, EDStateMachine sm, Siena siena) {
	this.attributes = (Hashtable)e.attributes.clone();
	this.tb = e.tb;
	this.ts = e.ts;
	this.name = e.name;
	this.sm = sm;
	this.myID = sm.myID + ":" + name;
	this.siena = siena;

	this.children = e.children;
	this.actions = e.actions;
	this.fail_actions = e.fail_actions;
	}

    /**
     * Gives life to this state. Assigns the owner state machine, 
     * the parent state, and state machine, and the siena to subscribe to.
     * @param parent the parent node, against whom we validate our timestamp
     */
    public void bear(EDState parent) {
	if (!alive) {
	    parents = new Vector();
	    
	    //subscribe
	    Filter f = buildSienaFilter();
	    try { siena.subscribe(f, this); } 
	    catch(SienaException e) { e.printStackTrace(); }

	    if(EventDistiller.DEBUG) 
		System.out.println("EDState: subscribing state: " + myID + " with " + f);
	}

	/* add it at the beginning, so when we search
	 * we find the more recent events first */
	parents.add(0, parent);

	/* [janak] Build the wildHash NOW in case we never get a
	 * notification.  I presume Enrico goes through the list to
	 * find the last guy with the wildHash, although we don't need
	 * that anymore, so it's commented out.
	 */

	/* inherit the wildHash from the candidate parent,
	 * so we can compare wildcard values while validating  */
	//	EDState parent;
	// 	for (int i = 0; i < parents.size(); i++) {
	// 	  parent = (EDState)parents.get(i);
	if (parent == null) {
	  if(EventDistiller.DEBUG)
	    System.out.println("EDState " + myID + " creating NEW wildHash");
	  wildHash = new Hashtable();
	} else {
	  if(EventDistiller.DEBUG)
	    System.out.println("EDState " + myID + " inheriting wildHash");
	  wildHash = parent.getWildHash();
	}

	this.alive = true; // don't move up...
    }

    /** Kills this state: unsubscribe and set alive to false */
    public void kill(){
	this.alive = false;
	try{ siena.unsubscribe(this); }
	catch(SienaException se){ se.printStackTrace(); };
    }

    /** 
     * Called when the state is matched. The state 
     * now lives the climax of its brief existence.
     */
    private void succeed() {
	if (EventDistiller.DEBUG) 
	    System.out.println("EDState: " + myID + " succeded");
	
	// 1. machine is in transition
	sm.setInTransition(true);
	// 2. the machine has started
	sm.setStarted(); 
	// 3. state is indelebly stamped
	ts = System.currentTimeMillis();
	// 4. bear children
	for (int i = 0; i < children.length; i++) {
	    sm.getState(children[i]).bear(this); 
	}
	// 5. tell the world we succeeded
	for (int i = 0; i < actions.length; i++) {
	    sm.sendAction(actions[i], wildHash); 
	}
	// 6. commit suicide
	kill(); 
	// 7. end of transition
	sm.setInTransition(false);
    }

    /** Sends out the failure notifications for this state. */
    public void fail() {
	for (int i = 0; i < fail_actions.length; i++)
	    sm.sendAction(fail_actions[i], wildHash);
    }

    /** Handles siena callbacks */
    public void notify(Notification n) {
      //	long millis = System.currentTimeMillis();
      // Log by received timestamp instead
      long millis = n.getAttribute("timestamp").longValue();

	if(EventDistiller.DEBUG) 
	    System.err.println("EDState " + myID +
			 ": Received notification " + n + 
			 "/nat time " + millis);

	EDState parent;
	for (int i = 0; i < parents.size(); i++) {
	parent = (EDState)parents.get(i);

	    // does the notification match us? 
	    if(validate(n, parent)) {
		if(EventDistiller.DEBUG)
		    System.err.println("EDState " + myID + " matched at time: " + millis);
		
		// yes!
		succeed();
	    }
	    }
	if (alive && EventDistiller.DEBUG) 
	    System.err.println("EDState/" + myID +
			       ": rejected Notification " + millis);
	
    }

    /** Unused Siena construct. */
    public void notify(Notification[] s) { ; }

  /**
   * Add an attribute/value pair.
   *
   * XXX - We should probably check to prevent overwriting, but heck.
   */
  public void putAttribute(String attr, AttributeValue val) {
    attributes.put(attr,val);
  }
  
  /** do we need this?
   * Add an attribute/value pair (strings).  This is accomplished by
   * wrapping an AttributeValue val.
   *
  public void add(String attr, String val) {
    add(attr, new AttributeValue(val));
    }*/

    /**
     * Checks to see if this state has timed out, relative to its
     * parent(s). If so, the state cannot be matched anymore,
     * and we can kill it.
     * @return whether this state has timed out and is now dead
     */
    public boolean reap() {
	if (EventDistiller.DEBUG)
	    System.out.println("checking state: " + myID);
	EDState parent;
	for (int i = 0; i < parents.size(); i++) {
	parent = (EDState)parents.get(i);
	    if (parent != null && EventDistiller.DEBUG)
		System.out.println("EDState:" + myID + ":checking parent: " +
				   parent.getName());
	    // can we still be matched ?
	    if(validateTimebound 
	       (parent, System.currentTimeMillis() - EventDistiller.reapFudgeMillis)) {
		return false;
	    }
	    else { // we can get rid of it...
		parents.remove(i); 
		i--;
	    }
    }	

	// if we're still here, all the parents have failed
	if (EventDistiller.DEBUG)
	    System.out.println("EDState: " + myID + " - timed out!");
	kill();
	return true;
    }

  /**
   * Validate a state.  If this returns true, then it means the state
   * was successfully matched within the appropriate timebounds.
   *
   * BUG WARNING: If timestamp is mapped to a non-long, we will crash.
   */
  public boolean validate(Notification n, EDState prev) {
    // Step 1. Perform timestamp validation.  If timestamp validation
    // fails, then we don't need to go further.
    AttributeValue timestamp = n.getAttribute("timestamp");
    if(validateTimebound(prev,timestamp/*.longValue()*/) == false) {
      if(EventDistiller.DEBUG) System.out.println("EDState " + myID + ": validate timebound FAILED");
      return false;
    }
    // Step 2. Now try and compare the attributes in the state's
    // notification.  This notification may have other attributes, but
    // we ignore them.
    Enumeration keys = attributes.keys();
    Enumeration objs = attributes.elements();
    while(keys.hasMoreElements()) {
      String attr = (String)keys.nextElement();
      AttributeValue val = (AttributeValue)objs.nextElement();
      if(validate(attr, val, n.getAttribute(attr)) == false) {
	return false;
      } // else continue
    }

    // They all passed, return true
    return true;
  }

  /**
   * Internal validate function - just check one attribute-value pair
   *
   * GOOD LUCK if you can understand this.  Read through a few times,
   * hopefully it'll make sense then :-)
   */
  private boolean validate(String attr, AttributeValue internalVal,
			   AttributeValue externalVal) {
    // Simple bounds checking
    if(attr == null || internalVal == null || 
       internalVal.getType() == AttributeValue.NULL) {
      System.err.println("FATAL: Internal representation error in "+
			 "EDState");
      return false;
    }

    // Attribute exists externally?
    else if(externalVal == null || 
	    externalVal.getType() != internalVal.getType()) {
      // No match
      return false;
    }

    // Debug
    if(EventDistiller.DEBUG) {
      System.err.println("EDState: comparing attribute \"" + attr +
			 "\", internalVal = " + internalVal + ", " +
			 "externalVal = " + externalVal);
    }

    // Wildcard binding?
    if(internalVal.getType() == AttributeValue.STRING &&
	    internalVal.stringValue().startsWith("*")) {
      // Is this one previously bound?
      String bindName = internalVal.stringValue().substring(1);
      if(bindName.length() == 0) { // Simple wildcard
	if(EventDistiller.DEBUG)
	  System.err.println("EDState: Simple wildcard, match");
	return true;
      } else { // Binding
	if(sm == null || wildHash == null) { // BAD
	  System.err.println("EDState: ERROR - No State Machine hash "+
			     "assigned, wildcard binding requested");
	  return false;
	}
	// Now check the bind
	if(wildHash.get(bindName) != null) {
	  if(attrEqual((AttributeValue)wildHash.get(bindName),externalVal)){
	    // YES!
	    if(EventDistiller.DEBUG)
	      System.err.println("EDState: wildcard already bound to \"" + 
				 wildHash.get(bindName) + "\" and match");
	    return true;
	  }
	  else {
	    if(EventDistiller.DEBUG)
	      System.err.println("EDState: wildcard already bound to \"" + 
				 wildHash.get(bindName) + "\" but nomatch");
	    return false; // Complex wildcard doesn't match
	  }
	} else { // Binding requested, NOT YET BOUND, bind and return true
	  wildHash.put(bindName, externalVal);
	  if(EventDistiller.DEBUG)
	    System.err.println("EDState: wildcard BOUND to " + externalVal);
	  return true;
	}
      }
    }

    // No wildcard binding, SIMPLE match
    if(EventDistiller.DEBUG)
      System.err.println("EDState: performing SIMPLE match on " + 
			 externalVal + "," + internalVal);
    
    if(attrEqual(internalVal,externalVal)) {
      if(EventDistiller.DEBUG) {	
	System.err.println("EDState: not wildcard, compare succeeded");
      }
      return true;
    }

    // Not our event
    if(EventDistiller.DEBUG)
      System.err.println("EDState: not wildcard, compare failed");
    return false;
  }

  /**
   * Compare timebound to a (previous) state.  IMPORTANT: to use this,
   * you must validate *every* state.  This is necessary because the
   * timestamp is stored in the state, to allow for future state 
   * comparisons.  Otherwise, if this comparison is made against an
   * unvalidated state, we will immediately return false (EXCEPTING
   * non-time-bound states - if it's not time bound, this ALWAYS
   * returns true).
   *
   * @param s The previous state
   * @param t The current event's timestamp (UNIX time format)
   * @return a boolean indicating if this state has occurred  
   *         'in time' from the previous state.
   */
  public boolean validateTimebound(EDState prev, long t) { 
    if(tb == -1) { // No time bounds
      return true;
    }
    if(prev.ts == -1) { // Prev state never validated
      return false;
    }
    if(t - prev.ts <= this.tb) return true;
    else return false;
  }

  /**
   * Convenience accessor method to validateTimebound.
   */
  public boolean validateTimebound(EDState s, AttributeValue t) {
    if(t == null || t.getType() != AttributeValue.LONG) { 
      // No match, do WE have a timebound
      if(tb == -1) { // OK, no timebound specified but we didn't expect one
	return true;
      }
      return false; // No timebound specified, we wanted one.
    }

    // There was a match to timebound, let's compare    
    return validateTimebound(s, t.longValue());
    }


    /** @return the XML representation of this object */
    public String toXML(){
	String s = "<state name=\"" + name + "\" timebound=\"" + tb + "\" children=\"" +
	    arrayToList(children) + "\" actions=\"" + arrayToList(actions) + 
	    "\" fail_actions=\"" + arrayToList(fail_actions) + "\">\n";
	
	Enumeration keys = attributes.keys();
	Enumeration objs = attributes.elements();
	while(keys.hasMoreElements()) {
	    String attr = (String)keys.nextElement();
	    AttributeValue val = (AttributeValue)objs.nextElement();
	    s = s + "\t<attribute attribute =\"" + attr + 
		"\" value=\"" + val + "\"/>\n";
	}
	s += "</state>\n";
	return s;
    }

    /**
     * Build a Siena filter to subscribe this state.
     * If expected value begins with an asterisk "*" it will be considered a
     * wildcard and will bind to anything. 
     * @return the Siena filter to use to subscribe this state.  
     */
    private Filter buildSienaFilter() {
	Filter f = new Filter();
	// We only want events from metaparser that have the state that
	// maches us
	f.addConstraint("Type", "EDInput");
	// Now enumerate through the actual attr, val pairs
	Enumeration keys = attributes.keys();
	Enumeration objs = attributes.elements();
	while(keys.hasMoreElements()) {
	    String attr = (String)keys.nextElement();
	    AttributeValue val = (AttributeValue)objs.nextElement();
	    if(val.getType() == AttributeValue.STRING &&
	       val.stringValue().startsWith("*")) {
		// XXX - will "" be a problem here?
		f.addConstraint(attr, new AttributeConstraint(Op.ANY,""));
	    } else {
		f.addConstraint(attr, new AttributeConstraint(val));
	    }
	}
	return f;
    }  

    // auxiliary static methods 

  /** AttributeValue isEqualTo check that WORKS. */
  public static boolean attrEqual(AttributeValue e1, AttributeValue e2) {
    if(e1.getType() != e2.getType()) return false;
    
    switch(e1.getType()) {
    case AttributeValue.BOOL:
      if(e1.booleanValue() == e2.booleanValue()) return true;
      else return false;
    case AttributeValue.DOUBLE:
      if(e1.doubleValue() == e2.doubleValue()) return true;
      else return false;
    case AttributeValue.LONG:
      if(e1.longValue() == e2.longValue()) return true;
      else return false;
    case AttributeValue.STRING:
      if(e1.stringValue().equals(e2.stringValue())) return true;
      else return false;
    default:
      System.err.println("EDState: Sorry, can't do attrEqual on this type!!");
      return false;
    }
  }

    /**
     * Converts an array of strings into a comma-delimited list.
     * @param a the array of strings
     * @return the comma delimited list of the elements in the array
     */
    public static String arrayToList(String[] a){
	String s = "";
	if (a.length == 0) return s;
	for(int i = 0; i < a.length; i++) s = s + a[i] + ",";
	return s.substring(0, s.length() - 1);
    }

    /**
     * Converts a comma-delimited list into an array containing the tokens.
     * @param s the comma-delimited list
     * @return an array containing the tokens of the list
     */
    public static String[] listToArray(String s){
	StringTokenizer st = new StringTokenizer(s, ",");
	String[] a = new String[st.countTokens()];
	int i = 0;
	while(st.hasMoreTokens()){
	    a[i] = st.nextToken();
	    i++;
	}
	return a;
    }

    // standard methods
	
    /** 
     * Returns the wildcard hashtable for this node.
     * If this state has multiple successors, that may
     * modify the table, we need to clone it.
     * @return the name of this EDState 
     */
    public Hashtable getWildHash() { 
	if (children.length > 1)
	    return (Hashtable)wildHash.clone();
	return wildHash; 
    }
	
    /** @return the name of this EDState */
    public String getName(){ return name; }
	
    /** @return the name of this EDState */
    public long getTimebound(){ return tb; }

    /** @return whether this EDState is currently subscribed */
    public boolean isAlive(){ return alive; }
	
    /** @return the (names of the) children of this EDState */
    String[] getChildren() { return children; }
}
