
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
 * Revision 1.24  2001-08-06 16:43:29  eb659
 * Tested essentially all features of ED, particularly counter and loop states.
 * Run EDTestConstruct to test different rules, specifying which rule ot test, and whether the rule should fail. For instance, run 'java psl.xues.EDTestConstruct -r spamblocker -f' to test failure of the spamblocker rule; omit '-f' to test its success.
 *
 * Removed a couple of bugs, and added a small hack, unfortunately. See comments in EDState.createFilter(). Unfortunately, the internal dispatching mechanism does not work very well with inequalities of big numbers, possibly due to automatic type conversion or something. This, btw, has nothing to do with James's work.
 *
 * A couple of hours of work.
 *
 * Revision 1.22  2001/07/03 21:36:23  eb659
 * Improved problems in race conditions. The application now hangs in the subscribe()
 * method in EDBus. Run EDTestConstruct: sometimes it works impeccably, other times
 * it hangs in EDBus.subscribe. James, Janak, do you want to have a look at it?
 *
 * Revision 1.21  2001/07/03 00:29:42  eb659
 * identified and fixed race condition. Others remain
 *
 * Revision 1.20  2001/06/30 21:13:17  eb659
 * *** empty log message ***
 *
 * Revision 1.19  2001/06/29 00:03:18  eb659
 * timestamp validation for loop doesn't work correctly, darn
 * reaper thread sometimes dies when a new machine is instantiated
 * (this only happens when dealing with an instantiation
 *
 * tested counter feature - works correctly
 * tested flush on shutdown (event-based mode)
 * changed skew to depend entirely on skew of last event,
 * this seems to work better for the moment
 *
 * Revision 1.18  2001/06/28 20:58:42  eb659
 * Tested and debugged timeout, different instantiation policies,
 * improved ED shutdown
 * added functionality to sed an event that fails all events during runtime
 *
 * timestamp validation for loop-rules doesn't work correctly, needs revision
 *
 * Revision 1.17  2001/06/27 22:08:43  eb659
 * color-coded error output for ED
 *
 * Revision 1.16  2001/06/27 17:46:53  eb659
 * added EDErrorManager, so James can have a look. We'll use implementors of
 * this class for the output of ED
 *
 * Revision 1.14  2001/06/20 18:54:44  eb659
 * handle self-comparison
 *
 * Revision 1.13  2001/06/18 20:58:36  eb659
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
    //private boolean inTransition = false;

    /** an ID number for debugging */
    String myID = null;

    /** the states in the event-graph of this stateMachine */
    private Hashtable states = new Hashtable();

    /** the notifications we send, when the states call them. */
    private Hashtable actions = new Hashtable();

    /** timestamp registered when machine is instantiated. */
    long timestamp;

    /** error manager for output. */
    EDErrorManager errorManager;

    /** Whether this state machine is currently being reaped. */
    boolean reaping = false;

    /**
     * How many states are succeeding at this point.
     * Only allow reaping if the number of states succeeding at this point is 0.
     */
    private int succeedingStates = 0;

    /**
     * Constructs a new stateMachine based on a stateMachineSpecification.
     * @param specification the specification on which this machine is constructed
     */
    public EDStateMachine(EDStateMachineSpecification specification) {

	this.specification = specification;
	this.manager = specification.getManager();
	this.timestamp = System.currentTimeMillis();
	this.errorManager = manager.getEventDistiller().getErrorManager();

	/* get an ID - for debugging
	 * this is in the form 'rulename:index' */
	this.myID = specification.getName() + ":" + specification.getNewID();
	errorManager.println("NEW EDStateMachine: " + myID, EDErrorManager.MANAGER);

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
	    EDState e = new EDState((EDState)sourceStates.get(key), this);
	    this.states.put(key, e);
	}

	// subscribe initial states
	String[] initialStates = specification.getInitialStates();
	for (int i = 0; i < initialStates.length; i++) {
	    EDState state = (EDState)states.get(initialStates[i]);
	    state.bear(null);
	}
  }

    /**
     * Reap ourselves if necessary.
     * The Grim Reaper (in EventDistiller) will eventually get to us by
     * calling this method.
     * @return whether this machine is 'dead' and can be removed
     */
    synchronized boolean reap() {
	/*if (succeedingStates > 0) try { wait(); }
	  catch(InterruptedException ex) { ; }
	*/
	reaping = true;
	errorManager.println("EDStateMachine: " + myID + " attempting to reap itself", EDErrorManager.REAPER);

	/* if machine has not started yet, no need to check it
	 * may reconsider this, once we will have rules that are made on the fly,
	 * with limited validity */
	if(!hasStarted) return false;

	errorManager.println("EDStateMachine: " + myID + " has started, enumeration beginning", EDErrorManager.REAPER);

	/* go through individual states,
	 * which will kill themselves if they timed out */
	Vector failedStateNames = new Vector();
	Enumeration elements = states.elements();
	while(elements.hasMoreElements()) {
	    EDState e = (EDState)elements.nextElement();
	    // remember which states timed out
	    if (e.reap()) failedStateNames.add(e.getName());
	}

	errorManager.println("EDStateMachine: " + myID + " enumeration done, live state check", EDErrorManager.REAPER);

	if(containsLiveStates()) return endReap(false);

	/* if all states are dead by now: */
	errorManager.println("EDStateMachine: " + myID + " about to get reaped", EDErrorManager.REAPER);

	/* 1) send failure notifications for SOME state that failed */
	if (failedStateNames.size() > 0)
	    ((EDState)states.get(failedStateNames.get(0).toString())).fail();

	/* 2) instantiate new machine, if necessary. */
	if (specification.getInstantiationPolicy() == EDConst.ONE_AT_A_TIME)
	    specification.instantiate();

	/* 3) throw the state machine away */
	return endReap(true);
    }

    /**
     * Just wakes up sleeping states, and returns.
     */
    private boolean endReap(boolean b) {
	/*if (!b) {
	  Enumeration elements = states.elements();
	  while(elements.hasMoreElements()) {
	  Object e = elements.nextElement();
	  synchronized (e) { e.notifyAll(); }
	  }
	    }*/
	reaping = false;
	return b;
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
    //void setInTransition(boolean inTransition) { this.inTransition = inTransition; }

    /** @return the specification for this machine */
    public EDStateMachineSpecification getSpecification(){ return specification;  }

    /** @return whether this machine has started receiving notifications */
    public boolean hasStarted(){ return this.hasStarted; }

    /** Called by a state of tis machine, when it receives a notification. */
    void addSucceedingState() { succeedingStates++; }

    /** Called by a state of this machine, when it ends receiving a notification. */
    synchronized void removeSucceedingState() {
	succeedingStates--;
	if (succeedingStates == 0) notify();
    }

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

	// comparing to self
	else if (this == o) return 0;

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

