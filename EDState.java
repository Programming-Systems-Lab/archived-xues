
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
 * Revision 1.26  2001-07-24 17:12:30  eb659
 * dddd
 *
 * Revision 1.25  2001/07/03 21:36:23  eb659
 * Improved problems in race conditions. The application now hangs in the subscribe()
 * method in EDBus. Run EDTestConstruct: sometimes it works impeccably, other times
 * it hangs in EDBus.subscribe. James, Janak, do you want to have a look at it?
 *
 * Revision 1.24  2001/07/03 00:29:42  eb659
 * identified and fixed race condition. Others remain
 *
 * Revision 1.23  2001/06/30 21:13:17  eb659
 * *** empty log message ***
 *
 * Revision 1.22  2001/06/29 21:38:43  eb659
 * thoroughly tested counter-feature:
 * - success and failure
 * - event-based and time-based modes
 * - timestamps using currentTime() and hard-coded
 *
 * individuated disfunction due to race/threading condition,
 * I Will try to fix during the weekend
 *
 * Revision 1.21  2001/06/29 00:03:18  eb659
 * timestamp validation for loop doesn't work correctly, darn
 * reaper thread sometimes dies when a new machine is instantiated
 * (this only happens when dealing with an instantiation
 *
 * tested counter feature - works correctly
 * tested flush on shutdown (event-based mode)
 * changed skew to depend entirely on skew of last event,
 * this seems to work better for the moment
 *
 * Revision 1.20  2001/06/28 20:58:42  eb659
 * Tested and debugged timeout, different instantiation policies,
 * improved ED shutdown
 * added functionality to sed an event that fails all events during runtime
 *
 * timestamp validation for loop-rules doesn't work correctly, needs revision
 *
 * Revision 1.19  2001/06/27 22:08:43  eb659
 * color-coded error output for ED
 *
 * Revision 1.18  2001/06/27 17:46:53  eb659
 * added EDErrorManager, so James can have a look. We'll use implementors of
 * this class for the output of ED
 *
 * Revision 1.16  2001/06/20 20:07:21  eb659
 * time-based and event-based timekeeping
 *
 * Revision 1.15  2001/06/20 18:54:44  eb659
 * handle self-comparison
 *
 * Revision 1.14  2001/06/18 20:58:36  eb659
 *
 * integrated version of ED. compiles, no testing done
 *
 * Revision 1.13  2001/06/18 17:44:51  jjp32
 *
 * Copied changes from xues-eb659 and xues-jw402 into main trunk.  Main
 * trunk is now development again, and the aforementioned branches are
 * hereby closed.
 *
 * Revision 1.7.4.15  2001/06/06 19:52:09  eb659
 * tested loop feature. Works, but identified other issues, to do with new
 * implementation of timestamp validation:
 * - keeping the time based on last received event has the inconvenience that we
 * do not send failure notifs if we do not get events (i.e. time stops moving)
 * - should there be different scenarios for real-time and non-realtime?
 *
 * Revision 1.7.4.14  2001/06/05 00:16:30  eb659
 *
 * wildHash handling corrected - (but slightly different than stable version)
 * timestamp handling corrected. Requests that all notifications be timestamped
 * reap based on last event processed internally
 * improved state reaper
 *
 * Revision 1.7.4.13  2001/06/01 22:31:19  eb659
 *
 * counter feature implemented and tested
 *
 * Revision 1.7.4.12  2001/05/31 22:36:38  eb659
 *
 * revision of timebound check: initial states can now have a specified bound.
 * allow values starting with '*'. Add an initial '*' as escape character...
 * preparing the ground for single-instance rules, and countable states
 *
 * Revision 1.7.4.11  2001/05/30 21:34:52  eb659
 *
 * rule consistency check: the following are implemented and thoroughly
 * tested (You don't need a source file to test: just mess up the spec file):
 * - specification of non-defined actions/fail_actions
 * - specification of non-defined children
 * - specification of non-defined wildcards
 * Also, fixed potential bug in wildcard binding...
 *
 * Revision 1.7.4.10  2001/05/29 20:47:07  eb659
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
 * Revision 1.7.4.9  2001/05/28 22:22:14  eb659
 *
 * added EDTestConstruct to test embedded constructor. Can construct ED using
 * a Notifiable owner, and optionally, a spec file and/or a debug flag.
 * There's a bug having to do with the wildcard binding, I'll look at that
 * in more detail tomorrow.
 *
 * Revision 1.7.4.8  2001/05/24 21:01:12  eb659
 *
 * finished rule consistency check (all points psecified in the todo list)
 * current rulebase is written to a file, on shutdown
 * compiles, not tested
 *
 * Revision 1.7.4.7  2001/05/23 21:40:41  eb659
 *
 *
 * DONE Direct (non-Siena) interface: new constructor taking a notifialbe object
 * DONE 2) allow states to absorb events - but waiting for the bus and XML
 *
 * ADDED rule consistency check, and error handling:
 * DONE checkConsistency() method in edsms
 * DONE KXNotification for EDError
 * DONE 1) no duplicate rules - checks against duplicate names
 * DONE 2) What happens if state parameters are missing, e.g., fail_actions?
 * DONE 3) check that all children and actions specified in the states are specified in the rule
 * more checks to be done - wildcards, etc.
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
public class EDState implements EDNotifiable {

    /** The name of this state. */
    private String name;

    /**
     * The ID of this state, used for debugging purposes.
     * This is in the form 'machineSpec:machineInstance:stateName'.
     */
    String myID;

    /** Relative timebound from previous state. */
    private long tb = -1;

    /**
     * Timestamp this state has fired in.  Created and used during
     * timebound validation.
     */
    private long ts = -1;

    /** The list of (names of) the  states that may success this one */
    private String[] children;

    /** The list of (names of) notifications we will send if we are matched. */
    private String[] actions;

    /** The list of (names of) notifications we will send if we fail to be matched. */
    private String[] fail_actions;

    /**
     * Hash of constraints on the values in the events that can mathch this state.
     * The keys to the table are the attribute names. The objects are of type siena.AttributeConstraint
     */
    private Hashtable constraints;

    /** Whether this state is currently subscribed and waiting to be matched */
    private boolean alive = false;

    /** Whether this state absorbs the event that it is notified of */
    private boolean absorb;

    /**
     * The list of states that preceded this one. We need this to
     * be able to validate the timebound (and wildcards)
     * against all possible parents that were matched and bore us.
     */
    private Vector parents;

    /**
     * List of subscribers used to subscribe. We need more than one subscriber for states
     * that have more than one parent: if several parents bear us, we need to subscribe with
     * different wildcard values - since different parents may have different values for wildcards.
     * "parents" and "subscribers" are therefore parallel vectors.
     */
    private Vector subscribers;

    /**
     * Wildcard binding hashtable.  If there are wildcards in states that must
     * match later states, we store them in this table. The variable has package
     * access, but if a state needs to inherit its parent's wildHash, use
     * getWildHash(), which checks whether it is necessary to copy the table.
     */
    private Hashtable wildHash;

    /** the internal bus; this is passed to us by our parent, when this state is subscribed */
    private EDBus bus = null;

    /** The state machine that "ownes" us. */
    private EDStateMachine sm = null;

    /**
     * Stores names and values for wildcard values temporarily,
     * while the state checks that all constraints are matched
     */
    //private Vector tempNames, tempValues;

    /** How many times this event will be matched, before it passes. */
    private int count = 1;

    /** Whether this event has started. Only used for events that loop:
     *  i.e. that can occur an indefinite number of times. */
    private boolean hasStarted = false;

    /** Error manager for output. */
    private EDErrorManager errorManager;

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
	this.constraints = new Hashtable();

	children = listToArray(childrenList);
	actions = listToArray(actionsList);
	fail_actions = listToArray(failActionsList);
    }

    /**
     * Clone-and-assign-state-machine CTOR. This constructor
     * is used when a stateMachine is instantiated.
     * @param e the state in the underlying stateMachineSpecification
     * @param sm the stateMachine to which this state belongs
     */
    public EDState(EDState e, EDStateMachine sm) {
	this.constraints = (Hashtable)e.constraints.clone();
	this.tb = e.tb;
	this.ts = e.ts;
	this.name = e.name;
	this.count = e.count;
	this.absorb = e.absorb;

	this.sm = sm;
	this.myID = sm.myID + ":" + name;
	this.bus = sm.getSpecification().getManager().getEventDistiller().getBus();
	this.errorManager = sm.getSpecification().getManager().getEventDistiller().getErrorManager();

	this.children = e.children;
	this.actions = e.actions;
	this.fail_actions = e.fail_actions;
    }

    /**
     * Adds a constraint.
     * @param s the identifier name of the attribute value
     * @param attributeconstraint the constraint to put on the value
     */
    void addConstraint(String s, AttributeConstraint attributeconstraint){
        constraints.put(s, attributeconstraint);
    }

    /**
     * Gives life to this state. Assigns the owner state machine,
     * the parent state, and state machine, and the siena to subscribe to.
     * @param parent the parent node, against whom we validate our timestamp,
     *               or null, if this is an initial state
     */
    void bear(EDState parent) {
	errorManager.println("EDState: " + myID + " being born", EDErrorManager.STATE);
	if (!alive) {
            // initialize vectors
            parents = new Vector();
            subscribers = new Vector();

	    /* make sure there is a hashtable defined at any time,
	     * for handling wildcards in failure notifications.
             * Also see note in fail() */
            if(parent == null)
                wildHash = new Hashtable();
            else wildHash = parent.getWildHash();

            errorManager.println("EDState: " + myID + " being born for the first time",
                                 EDErrorManager.STATE);
	}

        // timebound for matching this state, given this parent
        long l;
        if(tb == -1) l = Long.MAX_VALUE; // forever
        else l = sm.getSpecification().getManager().getEventDistiller().getTime() + tb;

        //errorManager.print("EDState: " + myID + " checking parent...", EDErrorManager.STATE);

        // have we already been born by this parent? -- this can be the case in counter or loop states
        int i = parents.indexOf(parent);

        //errorManager.println("done", EDErrorManager.STATE);
        if(i != -1) { // parent is already in the list, just reset timebound
            errorManager.println("EDState: " + myID + " extending subscription timebound", EDErrorManager.STATE);
            if(tb != -1) ((EDSubscriber)subscribers.get(i)).resetTimebound(l);

            // put most recent parent at the end of the list -- see reap()
            parents.add(parents.remove(i));
            subscribers.add(subscribers.remove(i));
        }
        else { // new parent
            Hashtable wc; // wildcard values for this subscription
            if(parents.size() == 0) wc = wildHash;
            else wc = parent.getWildHash();

            errorManager.println("EDState: " + myID + " subscribing...", EDErrorManager.STATE);

            EDSubscriber edsubscriber = new EDSubscriber(createFilter(wc, l), this, sm);
            bus.subscribe(edsubscriber);

            errorManager.println("EDState: " + myID + " done subscribing ", EDErrorManager.STATE);

            // put most recent parent at the end of the list
            parents.add(parent);
            subscribers.add(edsubscriber);
        }

        errorManager.println("EDState: " + myID + " born successfully ", EDErrorManager.STATE);
	this.alive = true;
    }

    /** Kills this state: unsubscribe and set alive to false. */
    public void kill() {
	errorManager.println("EDState: " + myID + " being KILLED", EDErrorManager.STATE);
	this.alive = false;
	bus.unsubscribe(this);
    }

    /**
     * Handles callbacks from the dispatcher. The statemachine is synchronized, so that states
     * do not succeed while the mahcine is being reaped. Note that ANY notification
     * we receive satisfies the state, since we build a precise filter in createFilter().
     * All we need to do is register the timestamp and the wildcard values.
     */
    public synchronized boolean notify(Notification n) {
	errorManager.println("EDState " + myID + ": Received: " + n,
			     EDErrorManager.STATE);
        synchronized(sm){

	// hang on while the machine is being reaped
	    /*if (sm.reaping) {
	      try{
	      errorManager.println("EDState " + myID + ": waiting" + n,
	      EDErrorManager.STATE);
	      wait(); }
	      catch(InterruptedException ex) { ; }
	    }*/

            // register timestamp
            ts = n.getAttribute(EDConst.TIME_ATT_NAME).longValue();

            // registrer wildcard values
            if(parents.size() > 1) wildHash = new Hashtable();
            for(Enumeration enumeration = constraints.keys(); enumeration.hasMoreElements();){
                String s = (String)enumeration.nextElement();
                String s1 = ((AttributeConstraint)constraints.get(s)).value.stringValue();

                if(s1.startsWith("*") && !s1.startsWith("**"))
                    wildHash.put(s1.substring(1), n.getAttribute(s));
            }

            // state machine progress
            succeed();

            if(absorb) errorManager.println("EDState:" + myID + ": ABSORBING event", 2);
            return absorb;
        }// synch
    }

    /**
     * Called when the state is matched. The state
     * now lives the climax of its brief existence.
     */
    private void succeed() {
	errorManager.println("EDState: " + myID + ": starting succeed()", EDErrorManager.STATE);

	// the machine has started
	sm.setStarted();
	// this state may be breaking the loop of its parent
        for (int i = 0; i < parents.size(); i++) {
            EDState parent = (EDState)parents.get(i);
            if (parent != null && parent != this && parent.getCount() == -1) parent.kill();
        }

	if (count > 1) { // counter feature
	    if (!hasStarted) {
		/*synchronized(parents)*/ {
		    parents.removeAllElements();
		    parents.add(this);
		}
		hasStarted = true;
	    }
	    count --;

	    errorManager.println
		("EDState: " + myID + ": decreased count to " + count,
		 EDErrorManager.STATE);
	}
	else if (count < 0) { // loop feature
	    errorManager.println("EDState: " + myID + ": LOOP state bearing itself", EDErrorManager.STATE);
	    /* bear children, and myself */
	    bear(this);
	    errorManager.println("EDState: " + myID + ": LOOP state bearing children", EDErrorManager.STATE);
	    for (int i = 0; i < children.length; i++)
		sm.getState(children[i]).bear(this);
	    // the kids will kill me, if they make it
	    hasStarted = true;
	}
	else if (count == 1) { // normal case
	    // 4. bear children
	    for (int i = 0; i < children.length; i++) {
		sm.getState(children[i]).bear(this);
	    }
	    // 5. tell the world we succeeded
	    for (int i = 0; i < actions.length; i++) {
		errorManager.println("EDState: " + myID + ": sending notification: " + actions[i],
				     EDErrorManager.STATE);
		sm.sendAction(actions[i], wildHash);
	    }
	    // 6. commit suicide
	    kill();
	}

	errorManager.println("EDState: " + myID + ": finishing succeed()\n", EDErrorManager.STATE);

	// reap will instantiate new machine, if necessary
	//if (sm.getSpecification().getInstantiationPolicy() == EDConst.ONE_AT_A_TIME) sm.reap();
	// out for now, would make the reaper hangs...
    }

    /**
     * Sends out the failure notifications for this state.
     *
     * NOTE: at the moment we are using the wildhash
     * of the first parent that matched us, to fill in the
     * wildcards of failure notifications.
     * We may need to change
     * this, since different parents may have different
     * wildcard values defined. If you fail, how do you know what
     * wildcards are the correct ones?
     */
    public void fail() {
	for (int i = 0; i < fail_actions.length; i++)
	    sm.sendAction(fail_actions[i], wildHash);
    }

    /**
     * Checks to see if this state has timed out, relative to its
     * parent(s). If so, the state cannot be matched anymore,
     * and we can kill it.
     * @return whether this state has timed out and is now dead
     */
    boolean reap() {
	//errorManager.println("checking state for life: " + myID, EDErrorManager.REAPER);
	if (!alive) return false;

	long currentTime =  sm.getSpecification().getManager().getEventDistiller().getTime();

	/* we're at the end of time, we must fail...
	 * time is set to MAX_VALUE when flushing */
	if (currentTime == Long.MAX_VALUE) {
	    kill();
	    return true;
	}

	errorManager.println("checking live state: " + myID, EDErrorManager.REAPER);

	/* can we still be matched? check the last (most recent) parent.
	 * NOTE: this assumes that events are processed sequentially,
	 * else we would need to check all the parents */
	if(validateTimebound
	   ((EDState)parents.lastElement(), currentTime - EDConst.REAP_FUDGE)) {
	    errorManager.println("state: " + myID + "has not timed out yet, no reap", EDErrorManager.REAPER);
   	    return false;
	}

	// if we're still here, timebound has failed
	errorManager.println("currentTime is " + currentTime + "\nmost recent parent time is " +
			     getTimestampForState((EDState)parents.lastElement()), EDErrorManager.REAPER);
	errorManager.println("EDState: " + myID + " - TIMED OUT!", EDErrorManager.STATE);
	kill();
	return true;
    }

  /**
   * Validate a state.  If this returns true, then it means the state
   * was successfully matched within the appropriate timebounds.
   *
   * BUG WARNING: If timestamp is mapped to a non-long, we will crash.
   *
  public boolean validate(Notification n, EDState prev) {
    // Step 1. Perform timestamp validation.  If timestamp validation
    // fails, then we don't need to go further.
    AttributeValue timestamp = n.getAttribute("timestamp");
    if(!validateTimebound(prev, timestamp)) {
	errorManager.println("EDState: " + myID + ": timestamp validation failed", EDErrorManager.STATE);
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
      if(!validate(attr, val, n.getAttribute(attr))) {
	  // forget any possible registered wildcard values
	  if (tempNames != null) {
	      tempNames = null;
	      tempValues = null;
	  }
	  return false;
      } // else continue
    }

    // They all passed, register any wildcard values, and return true
    if (tempNames != null)
	for (int i = 0; i < tempNames.size(); i++) {
	    wildHash.put(tempNames.get(i), tempValues.get(i));
	    errorManager.println("EDState: " + myID + ": wildcard '*" + tempNames.get(i) +
				 "' BOUND to: " + tempValues.get(i), EDErrorManager.STATE);
	}
    return true;
  }*/

  /**
   * Internal validate function - just check one attribute-value pair
   *
   * GOOD LUCK if you can understand this.  Read through a few times,
   * hopefully it'll make sense then :-)
   *
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
    /*if(EventDistiller.DEBUG) {
      System.err.println("EDState: comparing attribute \"" + attr +
			 "\", internalVal = " + internalVal + ", " +
			 "externalVal = " + externalVal);
			 }*

    // "**" is the escape character for "*"
    if (internalVal.getType() == AttributeValue.STRING &&
	internalVal.stringValue().startsWith("**"))
	return attrEqual(new AttributeValue(internalVal.stringValue().substring(1)), externalVal);

    // Wildcard binding?
    else if(internalVal.getType() == AttributeValue.STRING &&
	    internalVal.stringValue().startsWith("*")) {
      // Is this one previously bound?
      String bindName = internalVal.stringValue().substring(1);
      if(bindName.length() == 0) { // Simple wildcard
	  //if(EventDistiller.DEBUG)
	  //System.err.println("EDState: Simple wildcard, match");
	return true;
      } else { // Binding
	if(sm == null || wildHash == null) { // BAD
	  errorManager.println("EDState: ERROR - No State Machine hash "+
			       "assigned, wildcard binding requested", EDErrorManager.ERROR);
	  return false;
	}
	// Now check the bind
	if(wildHash.get(bindName) != null) {
	  if(attrEqual((AttributeValue)wildHash.get(bindName),externalVal)){
	    // YES!
	    if(EventDistiller.DEBUG)
		//System.err.println("EDState: wildcard already bound to \"" +
		//	 wildHash.get(bindName) + "\" and match");
	    return true;
	  }
	  else {
	    if(EventDistiller.DEBUG)
		//System.err.println("EDState: wildcard already bound to \"" +
		//	 wildHash.get(bindName) + "\" but nomatch");
	    return false; // Complex wildcard doesn't match
	  }
	}
	else { // Binding requested, NOT YET BOUND
	    // remember the name and value
	    if (tempNames == null) {
		tempNames = new Vector();
		tempValues = new Vector();
	    }
	    tempNames.add(bindName);
	    tempValues.add(externalVal);
	  return true;
	}
      }
    }

    // No wildcard binding, SIMPLE match
    /*if(EventDistiller.DEBUG)
      System.err.println("EDState: performing SIMPLE match on " +
      externalVal + "," + internalVal);*
    return attrEqual(internalVal,externalVal);
  }*/

    /**
     * Compare timebound to a (previous) state.  IMPORTANT: to use this,
     * you must validate *every* state.  This is necessary because the
     * timestamp is stored in the state, to allow for future state
     * comparisons.  Otherwise, if this comparison is made against an
     * unvalidated state, we will immediately return false (EXCEPTING
     * non-time-bound states - if it's not time bound, this ALWAYS
     * returns true).
     *
     * @param prev The previous state
     * @param currentTime The current event's timestamp (UNIX time format)
     * @return a boolean indicating if this state can occurred
     *         'in time' from the previous state.
     */
    public boolean validateTimebound(EDState prev, long currentTime) {
	// No time bounds, always good
	if(tb == -1) return true;
	// normal case, compare
	return (currentTime - getTimestampForState(prev) <= this.tb);
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

    /**
     * Convenience method for getting the relevant timestamp.
     * Returns the timestamp of this stateMachine, if the
     * state's unavailable.
     * @return the timestamp of this state, for comparison
     */
    private long getTimestampForState(EDState prev) {
	/* null state or unvalidated state, validate
	 * against the time when the rule was created */
	if (prev == null || prev.ts == -1) return sm.timestamp;
	else return prev.ts;
    }

    /** @return the XML representation of this object */
    public String toXML(){
	String s = "<state name=\"" + name + "\" timebound=\"" + tb + "\" children=\"" +
	    arrayToList(children) + "\" actions=\"" + arrayToList(actions) +
	    "\" fail_actions=\"" + arrayToList(fail_actions) + "\" absorb=\"" +
	    (new Boolean(absorb)).toString() + "\" count=\"" + count + "\">\n";

	for(Enumeration keys = constraints.keys(); keys.hasMoreElements();) {
	    String attName = (String)keys.nextElement();
	    AttributeConstraint ac = (AttributeConstraint)constraints.get(attName);
	    s = s + "\t<attribute name =\"" + attName + "\" value=\"" + ac.value + 
		"\" op=\"" + Op.operators[ac.op] + "\" type=\"" + EDConst.TYPE_NAMES[ac.value.getType()] + "\"/>\n";
	}
	s += "</state>\n";
	return s;
    }

    /**
     * Build a Siena filter to subscribe this state.
     * If expected value begins with an asterisk "*" it will be considered a
     * wildcard and will bind to anything.
     * @param wc the table containing the wildcards values to insert in the subscription
     * @param timebound the timebound within which the notification must be matched
     * @return the siena.Filter object to use to subscribe this state
     */
    private Filter createFilter(Hashtable wc, long timebound){
        Filter f = new Filter();

        // go through the constraints
        for(Enumeration enumeration = constraints.keys(); enumeration.hasMoreElements();){
            String attName = (String)enumeration.nextElement();
            AttributeConstraint ac = (AttributeConstraint)constraints.get(attName);
            AttributeValue attributevalue = ac.value;

            // "**.." is escape char for "*..", so subscribe removing the escape char
            if(attributevalue.stringValue().startsWith("**"))
                f.addConstraint(attName, new AttributeConstraint(ac.op, attributevalue.stringValue().substring(1)));
            // wildcard: do we already have a value for this?
            else if(attributevalue.stringValue().startsWith("*")){
                if(wc.get(attName) == null) // no value registered
                    f.addConstraint(attName, new AttributeConstraint(Op.ANY, ""));
                else
                    f.addConstraint(attName, new AttributeConstraint(ac.op, (String)wc.get(attName)));
            }
            // normal case, add simple constraint
            else f.addConstraint(attName, ac);
        }
        // set timebound
        f.addConstraint(EDConst.TIME_ATT_NAME, new AttributeConstraint(Op.LT, timebound));

	/* old version
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
	}*/

	return f;
    }

    // auxiliary static methods

  /** AttributeValue isEqualTo check that WORKS. *
  public static boolean attrEqual(AttributeValue e1, AttributeValue e2) {
    return Covering.apply_operator((short)1, attributevalue, attributevalue1);
    /*if(e1.getType() != e2.getType()) return false;

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
    }*
  }*/

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
	if (s == null) return new String[0];

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
    Hashtable getWildHash() {
	if (children.length > 1 && count == 1)
	    return (Hashtable)wildHash.clone();
	return wildHash;
    }

    /** @return the name of this EDState */
    String getName(){ return name; }

    /** @return the name of this EDState */
    long getTimebound(){ return tb; }

    /** @return whether this EDState is currently subscribed */
    boolean isAlive(){ return alive; }

    /** @return the (names of the) children of this EDState */
    String[] getChildren() { return children; }

    /** @return the (names of the) children of this EDState */
    String[] getActions() { return actions; }

    /** @return the (names of the) children of this EDState */
    String[] getFailActions() { return fail_actions; }

    /** @param absorb the new value for absorb */
    void setAbsorb(boolean absorb) { this.absorb = absorb; }

    /** @param count how many times this event will need to be matched. */
    void setCount(int count) { this.count = count; }

    /** @return how many times this event will need to be matched. */
    int getCount() { return count; }

    /** @return the constraints */
    Hashtable getConstraints() { return this.constraints; }
}
