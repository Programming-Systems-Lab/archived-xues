
package psl.xues;

import java.io.*;
import java.util.*;
import javax.swing.tree.*;
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
 * Revision 1.17  2001-09-05 18:57:40  eb659
 *
 * just a couple of bug fixes
 *
 * Revision 1.15  2001/08/27 17:47:42  eb659
 * more work done on the XML generator
 *
 * Revision 1.11  2001/06/28 20:58:42  eb659
 * Tested and debugged timeout, different instantiation policies,
 * improved ED shutdown
 * added functionality to sed an event that fails all events during runtime
 *
 * timestamp validation for loop-rules doesn't work correctly, needs revision
 *
 * Revision 1.10  2001/06/18 20:58:36  eb659
 *
 * integrated version of ED. compiles, no testing done
 *
 * Revision 1.9  2001/06/18 17:44:51  jjp32
 *
 * Copied changes from xues-eb659 and xues-jw402 into main trunk.  Main
 * trunk is now development again, and the aforementioned branches are
 * hereby closed.
 *
 * Revision 1.5.4.16  2001/06/05 00:16:30  eb659
 *
 * wildHash handling corrected - (but slightly different than stable version)
 * timestamp handling corrected. Requests that all notifications be timestamped
 * reap based on last event processed internally
 * improved state reaper
 *
 * Revision 1.5.4.15  2001/06/01 22:31:19  eb659
 *
 * counter feature implemented and tested
 *
 * Revision 1.5.4.14  2001/06/01 19:46:07  eb659
 *
 * implemented 'instantiation policy' for state machines.
 * options are documented in xsd.
 * For 'ome at any time' machines, I don't think we need 'terminal' states:
 * we just wait for one to dye, then instantiate the next one. Ok?
 *
 * Revision 1.5.4.13  2001/05/31 22:36:39  eb659
 *
 * revision of timebound check: initial states can now have a specified bound.
 * allow values starting with '*'. Add an initial '*' as escape character...
 * preparing the ground for single-instance rules, and countable states
 *
 * Revision 1.5.4.12  2001/05/30 21:34:52  eb659
 *
 * rule consistency check: the following are implemented and thoroughly
 * tested (You don't need a source file to test: just mess up the spec file):
 * - specification of non-defined actions/fail_actions
 * - specification of non-defined children
 * - specification of non-defined wildcards
 * Also, fixed potential bug in wildcard binding...
 *
 * Revision 1.5.4.11  2001/05/28 22:22:14  eb659
 *
 * added EDTestConstruct to test embedded constructor. Can construct ED using
 * a Notifiable owner, and optionally, a spec file and/or a debug flag.
 * There's a bug having to do with the wildcard binding, I'll look at that
 * in more detail tomorrow.
 *
 * Revision 1.5.4.10  2001/05/25 20:13:36  eb659
 *
 * allow ordering of rules;  (XMLspec needs update)
 * stateMachine instances are within the specification
 * StateMachineSpecification implement Comparable interface:
 * (higher priority objects appear smaller)
 * added EDNotifiable interface
 *
 * Revision 1.5.4.9  2001/05/24 21:01:12  eb659
 *
 * finished rule consistency check (all points psecified in the todo list)
 * current rulebase is written to a file, on shutdown
 * compiles, not tested
 *
 * Revision 1.5.4.8  2001/05/23 21:40:42  eb659
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
public class EDStateMachineSpecification {

    /** our manager. */
    private EDStateManager manager;

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

    /**
     * Reference to all initial states.
     * This is used for 2 reasons:
     * 1) to subscribed all initial states, when
     *    a machine is first instantiated;
     * 2) to display initial states in the XML
     *    generator tree.
     */
    Vector initialStates = null;

    /** State machines with this specification. */
    Vector stateMachines = new Vector();

    /** The instantiation policy for this rule.
     *  See EDConst for legal values. */
    int instantiationPolicy = EDConst.MULTIPLE;

  /** Basic CTOR.  Make sure to add states, and to call findInitialStates */
  public EDStateMachineSpecification(String name) {
      this.name = name;
  }

  /** Demo test factory, don't use otherwise
  public static EDStateMachineSpecification buildDemoSpec(Siena siena,
							  EDStateManager manager)
  {
    EDStateMachineSpecification managers =
      new EDStateMachineSpecification("demoTest", "foo",siena,manager);
    // I had to give it some silly parameters, so that it constructs itself - eb659
    EDState e = new EDState("zzz", -1, "", "", "");
    e.add("temperature","60");
    managers.stateArray.addElement(e);

    //    managers.subscribe();
    return managers;
    }*/

    /**
     * Add a state.
     * @param e the state to add
     * @return false if the state could not be added due to a
     *         naming conflict (another state with this name)
     */
    public boolean addState(EDState e) {
        if (states.get(e.getName()) != null)
            return false;
	states.put(e.getName(), e);
        return true;
    }

    /**
     * Add a state in intitial position.
     * @param e the state to add
     * @return false if the state could not be added due to a
     *         naming conflict (another state with this name)
     */
    public boolean addInitialState(EDState e) {
        if (states.get(e.getName()) != null)
            return false;
	states.put(e.getName(), e);
        initialStates.add(e);
        return true;
    }

    /**
     * Add an Action
     * @param name the name for this action
     * @param action the action
     * @return false if the action could not be added
     *         due to a naming conflict
     */
    public boolean addAction(String name, Notification action) {
        if (actions.get(name) != null) return false;
	actions.put(name, action);
        return true;
    }

    /**
     * Performs various checks for consistency on this specification.
     * @return a string representing what error prevents this rule from proper
     *         functioning, or null if the rule is legal
     */
    String checkConsistency() {
	String error = "";

	// check state representation
	Vector wildcards = new Vector();
	Enumeration keys = states.keys();
	while (keys.hasMoreElements()) {
	    EDState checkState = ((EDState)states.get(keys.nextElement()));
	    String checkName = checkState.getName();

	    // do all specified children exist?
	    String[] children = checkState.getChildren();
	    for (int i = 0; i < children.length; i++)
		if (states.get(children[i]) == null)
		    error = error + "state '" + children[i] + "' - definded as child of '"
			+ checkName + "' - is not defined in specification\n";

	    // do all specified actions exist?
	    String[] checkAct = checkState.getActions();
	    for (int i = 0; i < checkAct.length; i++)
		if (actions.get(checkAct[i]) == null)
		    error = error + "action '" + checkAct[i] + "' sent by state '"
			+ checkName + "' - is not defined in specification\n";

	    // do all specified fail_actions exist?
	    String[] checkFail = checkState.getFailActions();
	    for (int i = 0; i < checkFail.length; i++)
		if (actions.get(checkFail[i]) == null)
		    error = error + "fail_action '" + checkFail[i] + "' - sent by state '"
			+  checkName + "' - is not defined in specification\n";

	    // sample all defined wildcards
	    Vector wc = getWildcards(checkState);
	    for (int i = 0; i < wc.size(); i++)
		if (!wildcards.contains(wc.get(i)))
		    wildcards.add(wc.get(i));
	    //if (EventDistiller.DEBUG)
	    //System.out.println("state "+ checkState.getName() + " contains wildcards " + wc);
	}

	// check actions representation
	// are wildcards in all actions defined somewhere? -- this doesn't work right now!
	keys = actions.keys();
	while (keys.hasMoreElements()) {
	    String key = keys.nextElement().toString();
	    Notification act = (Notification)actions.get(key);
	    Vector wc = getWildcards(act);

	    //if (EventDistiller.DEBUG)
	    //System.out.println("action "+ key + " requests wildcards " + wc);

	    for (int i = 0; i < wc.size(); i++) {
		if (wc.get(i).toString().equals("*"))
		    error = error + "cannot specify value '*' in action '" + key;
		else if (!wildcards.contains(wc.get(i)))
		    error = error + "wildcard '*" + wc.get(i) + "' in action '"
			+ key + "' is not defined in any state\n";
	    }
	}

	if (error.equals("")) return null;
	return error;
    }

    /**
     * Initializes this specification, by finding the initial states,
     * and subscribing the first instance of this specification.
     */
    void init(EDStateManager manager) {
        this.manager = manager;
	findInitialStates();
	stateMachines.add(new EDStateMachine(this));
    }

    /**
     * Finds the initial states of this machine.
     * We do this by seeing which states don't
     * have any incoming edges.
     */
    private void findInitialStates() {

        // assume all states are initial
	Enumeration e = states.elements();
	initialStates = new Vector();
	while (e.hasMoreElements())
	    initialStates.add(e.nextElement());

        // flag states with incoming edges
        // which cannot be initial
	boolean[] hasIncomingEdges = new boolean[initialStates.size()];
	for (int i = 0; i < initialStates.size(); i++) {
	    String[] children = ((EDState)initialStates.get(i)).getChildren();
	    for (int j = 0; j < children.length; j++) {
		int index = initialStates.indexOf(states.get(children[j]));
		hasIncomingEdges[index] = true;
	    }
	}

	// remove states that are not initial
        int j, i = 0;
	for (j = 0; j < hasIncomingEdges.length; j++)
	    if (hasIncomingEdges[j])
		initialStates.remove(i);
            else i++;
    }


    // XML representation

    /**
     * @param position the index of priority of the rule
     * @return the XML representation of this object
     */
    public String toXML(int position){
	// start
	String s = "<rule name=\"" + name + "\" instantiation=\"" +
	    instantiationPolicy + "\" position=\"" + position + "\">\n";

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

    /**
     * Returns all the wildcards defined in a given state.
     * @param checkState the state whose wildcards
     * @return the wildcards (without the initial *)
     */
    public static Vector getWildcards(EDState checkState) {
	Vector v = new Vector();
	Hashtable ht = checkState.getConstraints();
	Enumeration elements = ht.elements();
	while (elements.hasMoreElements()) {
	    String s = ((AttributeConstraint)elements.nextElement()).value.stringValue();
	    if (s.startsWith("*") && s.length() > 1 && !s.startsWith("**")) v.add(s.substring(1));
	}
	return v;
    }

    /**
     * Returns all the wildcards defined in a given notification.
     * @param checkNotif the notification whose wildcards
     * @return the wildcards (without the initial *)
     */
    public static Vector getWildcards(Notification checkNotif) {
	Vector v = new Vector();
	Iterator iter = checkNotif.attributeNamesIterator();
	while (iter.hasNext()) {
	    String s = checkNotif.getAttribute(iter.next().toString()).stringValue();
	    if (s.startsWith("*") && s.length() > 1) v.add(s.substring(1));
	}
	return v;
    }

    /** @param instantiationPolicy the instantiation policy for this rule */
    public void setInstantiationPolicy(int i) throws IllegalArgumentException {
	if (i < 0 || 2 < i) throw new IllegalArgumentException
	    ("encountered invalid value for instantiationPolicy");
	else this.instantiationPolicy = i;
    }

    /** @return the instantiation policy for this rule */
    public int getInstantiationPolicy() { return instantiationPolicy; }

    /**
     * Creates and subscribes a new machine with this specification.
     * Fails to do so if ED is is shutdown, in which case it is
     * irrelevant to have new machines - at least for now, until
     * retro-active rules are not envisaged.
     */
    void instantiate() {
	if (!manager.getEventDistiller().isInShutdown()) {
	    synchronized (stateMachines) {
		stateMachines.add(new EDStateMachine(this));
	    }
	}
    }

    /**********************************
             standars methods
    **********************************/

    /** @return the stateMachineManager */
    public EDStateManager getManager(){ return this.manager; }

    /** @return the name of this SMSpecification */
    public String getName(){ return name; }

    /** @param name the new name for this SMSpecification */
    public void setName(String name){ this.name = name; }

    /** @return the actions for this machine */
    public Hashtable getActions(){ return this.actions; }

    /** @return the states for this machine */
    public Hashtable getStates(){ return this.states; }

    /** @return an index - called by new instances of this specification */
    String getNewID(){ return "" + (++counter); }

    /** @return the vector of states to subscribe initially */
    Vector getInitialStates() {
        if (initialStates == null) findInitialStates();
        return initialStates;
    }
}

