
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
 * Revision 1.13  2001-06-18 20:58:36  eb659
 *
 * integrated version of ED. compiles, no testing done
 *
 * Revision 1.12  2001/06/18 17:44:51  jjp32
 *
 * Copied changes from xues-eb659 and xues-jw402 into main trunk.  Main
 * trunk is now development again, and the aforementioned branches are
 * hereby closed.
 *
 * Revision 1.7.4.12  2001/06/05 00:16:30  eb659
 *
 * wildHash handling corrected - (but slightly different than stable version)
 * timestamp handling corrected. Requests that all notifications be timestamped
 * reap based on last event processed internally
 * improved state reaper
 *
 * Revision 1.7.4.11  2001/06/01 19:46:07  eb659
 *
 * implemented 'instantiation policy' for state machines.
 * options are documented in xsd.
 * For 'ome at any time' machines, I don't think we need 'terminal' states:
 * we just wait for one to dye, then instantiate the next one. Ok?
 *
 * Revision 1.7.4.10  2001/05/31 22:36:38  eb659
 *
 * revision of timebound check: initial states can now have a specified bound.
 * allow values starting with '*'. Add an initial '*' as escape character...
 * preparing the ground for single-instance rules, and countable states
 *
 * Revision 1.7.4.9  2001/05/29 20:47:07  eb659
 *
 * various fixes. embedded constructor thoroughly tested,
 * in particular the following features:
 * - standard constructor
 * - specification-less constructor
 * - sending events
 * - recieving notifications
 * - shutdown features (when shutdown() is called and when it is not)
 *
 * ED looks for a free address to place private siena.
 *
 * Revision 1.7.4.8  2001/05/25 20:13:35  eb659
 *
 * allow ordering of rules;  (XMLspec needs update)
 * stateMachine instances are within the specification
 * StateMachineSpecification implement Comparable interface:
 * (higher priority objects appear smaller)
 * added EDNotifiable interface
 *
 * Revision 1.7.4.7  2001/05/24 21:01:12  eb659
 *
 * finished rule consistency check (all points psecified in the todo list)
 * current rulebase is written to a file, on shutdown
 * compiles, not tested
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
 * Revision 1.7.4.4  2001/05/01 04:21:56  eb659
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
 * Revision 1.7.4.2  2001/04/06 00:15:07  eb659
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
 * Revision 1.7.4.1  2001/04/03 01:09:13  eb659
 *
 *
 * OK this is my first upload...
 * Basically, most of the dynamic rulebase stuff has been accomplished.
 * the principal methods are in EDStatemanaged, but most of the files in ED
 * had to be modified, at least in some small way
 * enrico
 *
 * Revision 1.7  2001/03/14 20:45:15  png3
 * replaced deprecated call to Notification.iterator()
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
 * Revision 1.4  2001/01/29 05:22:53  jjp32
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
public class EDStateMachine implements Comparable {
    /** the manager */
    private EDStateManager manager = null;

    /** the specification on which this machine is built. */
    private EDStateMachineSpecification specification;

    /** 
     * Whether this machine has started.
     * When the mahcine is first instanciated, 
     * this value is set to false, and the initial states subscribed.
     * When one of these states is matched, a call is made to make a 
     * new instance, and the value set to true. it then remains true.
     */
    private boolean hasStarted = false;

    /**
     * True if the machine is in a state of transition,
     * meaning that it is unsubscribing one state and 
     * subscribing the children. Do not allow reaping 
     * when the machine is in transition.
     */
    private boolean inTransition = false;

    /** an ID number for debugging */
    String myID = null;

    /** the states in the event-graph of this stateMachine */
    private Hashtable states = new Hashtable();

    /** the notifications we send, when the states call them. */
    private Hashtable actions = new Hashtable();

    /** timestamp registered when machine is instantiated. */
    long timestamp;

    /** no! percolated down to the state level, so we can really have multiple paths
     * with independent wildcard values
     * Wildcard binding hashtable.  If there are wildcards in states that must
     * match later states, we store them in this table.  In the future, this
     * might be used for more than just wildcards.
     */ 
    //Hashtable wildHash = new Hashtable();

    /**
     * Constructs a new stateMachine based on a stateMachineSpecification.
     * @param specification the specification on which this machine is constructed
     */
    public EDStateMachine(EDStateMachineSpecification specification) {
	
	this.specification = specification;
	this.manager = specification.getManager();
	this.timestamp = System.currentTimeMillis();
	//this.siena = manager.getSiena();

	/* get an ID - for debugging
	 * this is in the form 'rulename:index' */
	this.myID = specification.getName() + ":" + specification.getNewID();
	if (EventDistiller.DEBUG) 
	    System.out.println("NEW EDStateMachine: " + myID);

	// copy the actions - using the cloning constructor
	Hashtable sourceActions = specification.getActions();
	Enumeration keys = sourceActions.keys();
	while(keys.hasMoreElements()){
	    String key = keys.nextElement().toString();
	    this.actions.put(key, new Notification((Notification)sourceActions.get(key)));
	}

	// copy the states - using the cloning constructor
	Hashtable sourceStates = specification.getStates();
	keys = sourceStates.keys();
	while(keys.hasMoreElements()){
	    String key = keys.nextElement().toString();
	    EDState e = new EDState((EDState)sourceStates.get(key), 
				    this, manager.getBus());
	    this.states.put(key, e);
	}

	// subscribe initial states
	String[] initialStates = specification.getInitialStates();
	for (int i = 0; i < initialStates.length; i++) {
	    EDState state = (EDState)states.get(initialStates[i]);
	    state.bear(null);
	}
  }
    
  /* do we need this?
   * Add a state.  WARNING! This machine *WILL* adjust the state.  If you 
   * need a deep copy, make one!
   *
  public void addState(EDState s) {
    // Assign ourselves as the "owner" state machine
    s.assignOwner(this);
    states.addElement(s);
    try {
      Filter f = s.buildSienaFilter();
      if(EventDistiller.DEBUG)
	System.err.println("EDStateMachine/"+myID+"/"+": Subscribing " + f);
      siena.subscribe(f,this);
    } catch(SienaException e) { e.printStackTrace(); }
  }*/

  /*
   * Add an action.  If the notification does not exist it will be
   * created the first time.  Use setAction if you want to *replace*
   * the notification with a new one.
   *
  public void addAction(String attr, String val) {
    if(action == null) {
      action = new Notification();
    }
    action.putAttribute(attr, val);
    } */

  /*
   * (Re)set the action.
   *
  public void setAction(String attr, String val) {
    action = null;
    addAction(attr,val);
    } */

    /*
      move this to the EDState...
  public void notify(Notification n) {
    long millis = System.currentTimeMillis();
    if(EventDistiller.DEBUG) 
      System.err.println("EDStateMachine/"+myID+"/"+
			 ": Received notification " + millis + 
			 " which is " + n);
    // Check it against the current state - but to do this, we need
    // the prev state
    EDState prevState = (currentState == 0 ? null : 
			 (EDState)states.elementAt(currentState-1));
    if(((EDState)states.elementAt(currentState)).validate(n,prevState)) {
      if(EventDistiller.DEBUG)
	System.err.println("EDStateMachine/"+myID+"/"+
			   ": Notification " + millis + " matched!");
      // Yes!
      currentState++;
      // Did we pass the last state?
      if(states.size() == currentState) {
	// Yes - send action
	finish();
      }
    } else {
      if(EventDistiller.DEBUG)
	System.err.println("EDStateMachine/"+myID+"/"+
			   ": Notification " + millis + " rejected");
    }
  }

  /** Unused Siena construct. 
      public void notify(Notification[] s) { ; }*/

  /** moved down to the state level 
   * needs revision - or, do we need this at all?
   * Finish up the state machine.  Yes, this could be inlined, but
   * why?  Also, another alternative is to have EDManager do the
   * unsubscription.  Since we subscribe in the first place it seems
   * to make better sense to handle our own unsubscriptions (but this
   * behavior may change someday...)
   *
  private void finish() {
    if(EventDistiller.DEBUG)
      System.err.println("EDStateMachine/" + myID + "/: finishing");
    // Remove all notifications
    try {
      siena.unsubscribe(this);
    } catch(SienaException e) { e.printStackTrace(); }

    }

    // Call our manager and tell them we're finished, and hand them
    // the (modified) notifications to send
    el.finish(this, actions);
  } */

    /** 
     * Reap ourselves if necessary. 
     * The Grim Reaper (in EventDistiller) will eventually get to us by
     * calling this method.
     * @return whether this machine is 'dead' and can be removed 
     */
    public boolean reap() {
	/* if machine has not started yet, keep it
	   if it is in a state of transition, don't check it */
	if(!hasStarted || inTransition) return false;

	/* go through individual states,
	 * which will kill themselves if they timed out */
	Vector failedStateNames = new Vector();
	Enumeration keys = states.keys();
	while(keys.hasMoreElements()){
	    String key = keys.nextElement().toString();
	    EDState e = (EDState)states.get(key);
	    // remember which states timed out
	    if (e.isAlive() && e.reap()) failedStateNames.add(e.getName());
	}

	if(containsLiveStates()) return false;

	/* if all states are dead by now: 
	 * 1) send failure notifications for SOME state that failed */
	if (failedStateNames.size() > 0) 
	    ((EDState)states.get(failedStateNames.get(0).toString())).fail();
	/* 2) instantiate new machine, if necessary */
	if (specification.getInstantiationPolicy() == EDConst.ONE_AT_A_TIME)
	    specification.instantiate();
	/* 3) throw the state amchine away */
	if (EventDistiller.DEBUG) 
	    System.out.println("EDStateMachine: " + myID + " about to get reaped");
	return true; 


	/*
	  this test has been moved down to the state level

    boolean reap = false;

    // Should never happen, but easy boundary cases 
    if(currentState == 0) reap = false;
    else if(currentState == states.size()) reap = true;
    
    /* Now try calling validateTimebound, assume the current state
     * occurs --NOW--, and if that fails, then we MUST reap.
     *
    else if(((EDState)states.elementAt(currentState)).
	    validateTimebound((EDState)states.elementAt(currentState-1),
			      System.currentTimeMillis() - 
			      EventDistiller.reapFudgeMillis) == false) {
      reap = true;
    }
    
	// Now, shall we reap?  
    if(reap) unsubscribe();

    return reap;    */
    }

    /** @return whether there are live states in this machine */
    private boolean containsLiveStates(){
	Enumeration elements = states.elements();
	while(elements.hasMoreElements()){
	    if(((EDState)elements.nextElement()).isAlive()) return true;
	}
	return false;
    }

    /** 
     * Kill all the states currently subscribed. 
     * this has the effect that the machine will be
     * reaped the next time the manager takes a look at us.
     */
    public void killAllStates(){
	Enumeration keys = states.keys();
	while(keys.hasMoreElements()){
	    Object key = keys.nextElement();
	    EDState e = (EDState)states.get(key);
	    if(e.isAlive()) e.kill();
	}
    }

    /** Called when a state has been matched. So now we know we've started */
    public void setStarted(){
	if(hasStarted) return;
	hasStarted = true;
	// put a new 'clear' machine on the list, if allowed
	if (specification.getInstantiationPolicy() == EDConst.MULTIPLE)
	    specification.instantiate();
    }

    /** 
     * Sends the given notification out, after filling
     * in the required wildcards. The wildcard hashtable
     * is passed from the specific state that publishes the action,
     * since different states in the graph may have different
     * values for their wildcard hashtables.
     * @param actionName the name of the action to send
     * @param wildHash the hashtable containing any wildcards
     *        that may be needed to fill in the actions
     */
    public void sendAction(String actionName, Hashtable wildHash) {
	Notification action = (Notification)actions.get(actionName);
	// Do we need to amend the Notification?  Iterate through all
	// attribute values and fill in any wildcard hashes in.
	Iterator i = action.attributeNamesIterator();	
	while(i.hasNext()) {
	    String attr = (String)i.next();
	    AttributeValue val = action.getAttribute(attr);
	    if(val.getType() == AttributeValue.STRING &&
	       val.stringValue().startsWith("*")) {
		String key = val.stringValue().substring(1);
		AttributeValue bindVal = (AttributeValue)wildHash.get(key);
		if(bindVal != null) { // Replace this attributeValue
		    action.putAttribute(attr,bindVal);
		} 
		else { // the value is not defined - send error message
		    String error = "wildcard '" + key + "' has not been defined; "
			+ "incomplete action is being sent";
		    System.err.println("ERROR: " + error);
		    manager.getEventDistiller().sendPublic
			(KXNotification.EDError(KXNotification.EDERROR_WILDCARD, error));
		}
	    }
	}
	if (EventDistiller.DEBUG)
	    System.out.println("EDStateMachine: "+ myID + " sending notification: " + actionName);
	manager.getEventDistiller().sendPublic(KXNotification.EDOutput(action));
    }

    /** 
     * Returns the action requested.
     * @param actionName the name of the action
     * @return the action with the given name
     */
    public Notification getAction(String actionName) { 
	return (Notification)actions.get(actionName); 
    }

    /** 
     * Returns the state requested.
     * @param stateName the name of the action
     * @return the state with the given name
     */
    public EDState getState(String stateName) { return (EDState)states.get(stateName); }

    /** 
     * Sets the value for inTransition. Called by a state
     * when it is bearing children.
     * @param inTransition the new value for inTransition
     */
    void setInTransition(boolean inTransition) { this.inTransition = inTransition; }

    /** @return the specification for this machine */
    public EDStateMachineSpecification getSpecification(){ return specification;  }

    /** @return whether this machine has started receiving notifications */
    public boolean hasStarted(){ return this.hasStarted; }

    // comparable interface

    /** 
     * We compare state machines to determine priorities in receiving notifs.
     * For now call indexOf() -- inefficient, but eventually store indexes
     * with the sm and sms, updating when necessary.
     * @param o the object to compare to.
     */
    public int compareTo(Object o) {
	// yield to manager
	if (!(o instanceof EDStateMachine)) return 1;

	else { // proper compare to another state machine
	    EDStateMachine other = (EDStateMachine)o;

	    if (this.specification == other.getSpecification()) {
		// different instances of the same specification
		if (specification.stateMachines.indexOf(this) <
		    specification.stateMachines.indexOf(other)) return -1;
		else return 1;
	    }
	    
	    else { // different specifications
		if (manager.stateMachineTemplates.indexOf(this.specification) < 
		    manager.stateMachineTemplates.indexOf(other.getSpecification())) return -1;
		else return 1;
	    }
	}
    }
}

