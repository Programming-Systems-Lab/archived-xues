
package psl.xues;

import psl.kx.KXNotification;
import java.io.*;
import java.util.*;
import siena.*;

// png3
//import oracle.xml.parser.v2.SAXParser;
import org.apache.xerces.parsers.SAXParser;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Event Distiller State Machine Manager
 *
 * This class exists to manage many instances of state machines.  It
 * keeps track of the templates, garbage collects, finalizes them.
 * NOTE: This class no longer creates state machines.  See
 * "EDStateMachineSpecification", below, which acts like a creation
 * template mechanism.
 *
 * Copyright (c) 2000: The Trustees of Columbia University in the
 * City of New York.  All Rights Reserved.
 *
 * TODO:
 * - More efficient way of garbage collecting state machines
 * - Event/reap collision
 *
 * @author Janak J Parekh (jjp32@cs.columbia.edu)
 * @version 1.0
 *
 * Revision 1.13 eb659
 * Now subscribes to internal siena to be notified of
 * changes to be made to the rulebase.
 * added dynamicAddMachine() method
 *
 * $Log$
 * Revision 1.35  2001-09-05 18:57:40  eb659
 *
 * just a couple of bug fixes
 *
 * Revision 1.33  2001/08/27 17:47:42  eb659
 * more work done on the XML generator
 *
 * Revision 1.29  2001/07/03 21:36:23  eb659
 * Improved problems in race conditions. The application now hangs in the subscribe()
 * method in EDBus. Run EDTestConstruct: sometimes it works impeccably, other times
 * it hangs in EDBus.subscribe. James, Janak, do you want to have a look at it?
 *
 * Revision 1.28  2001/07/03 00:29:42  eb659
 * identified and fixed race condition. Others remain
 *
 * Revision 1.27  2001/06/30 21:13:17  eb659
 * *** empty log message ***
 *
 * Revision 1.26  2001/06/29 21:38:43  eb659
 * thoroughly tested counter-feature:
 * - success and failure
 * - event-based and time-based modes
 * - timestamps using currentTime() and hard-coded
 *
 * individuated disfunction due to race/threading condition,
 * I Will try to fix during the weekend
 *
 * Revision 1.25  2001/06/29 00:03:18  eb659
 * timestamp validation for loop doesn't work correctly, darn
 * reaper thread sometimes dies when a new machine is instantiated
 * (this only happens when dealing with an instantiation
 *
 * tested counter feature - works correctly
 * tested flush on shutdown (event-based mode)
 * changed skew to depend entirely on skew of last event,
 * this seems to work better for the moment
 *
 * Revision 1.24  2001/06/28 20:58:42  eb659
 * Tested and debugged timeout, different instantiation policies,
 * improved ED shutdown
 * added functionality to sed an event that fails all events during runtime
 *
 * timestamp validation for loop-rules doesn't work correctly, needs revision
 *
 * Revision 1.23  2001/06/27 22:36:21  eb659
 * *** empty log message ***
 *
 * Revision 1.22  2001/06/27 19:43:39  eb659
 * momentsrily finalized EDErrorManager. Output goes there...
 *
 * Revision 1.21  2001/06/20 20:49:32  eb659
 * two options: time-driven, or event-driven
 * a. options interface
 * b. flush event in shutdown when using event-based time
 * c. documentation (for now summarily in html, more thorough in code)
 *
 * Revision 1.20  2001/06/20 18:54:44  eb659
 * handle self-comparison
 *
 * Revision 1.19  2001/06/18 20:58:36  eb659
 *
 * integrated version of ED. compiles, no testing done
 *
 * Revision 1.18  2001/06/18 17:44:51  jjp32
 *
 * Copied changes from xues-eb659 and xues-jw402 into main trunk.  Main
 * trunk is now development again, and the aforementioned branches are
 * hereby closed.
 *
 * Revision 1.12.4.17  2001/06/06 19:52:09  eb659
 * tested loop feature. Works, but identified other issues, to do with new
 * implementation of timestamp validation:
 * - keeping the time based on last received event has the inconvenience that we
 * do not send failure notifs if we do not get events (i.e. time stops moving)
 * - should there be different scenarios for real-time and non-realtime?
 *
 * Revision 1.12.4.16  2001/06/01 22:31:19  eb659
 *
 * counter feature implemented and tested
 *
 * Revision 1.12.4.15  2001/05/31 22:36:39  eb659
 *
 * revision of timebound check: initial states can now have a specified bound.
 * allow values starting with '*'. Add an initial '*' as escape character...
 * preparing the ground for single-instance rules, and countable states
 *
 * Revision 1.12.4.14  2001/05/30 21:34:52  eb659
 *
 * rule consistency check: the following are implemented and thoroughly
 * tested (You don't need a source file to test: just mess up the spec file):
 * - specification of non-defined actions/fail_actions
 * - specification of non-defined children
 * - specification of non-defined wildcards
 * Also, fixed potential bug in wildcard binding...
 *
 * Revision 1.12.4.13  2001/05/29 21:16:43  eb659
 *
 * Rule ordering implemented, tested, and extended to xsd file
 *
 * Revision 1.12.4.12  2001/05/29 17:30:25  jjp32
 * Fixed condition where no specification file was found -- the reaper
 * would not start.  Still need to test functionality with no
 * specification file.
 *
 * Revision 1.12.4.11  2001/05/28 22:22:14  eb659
 *
 * added EDTestConstruct to test embedded constructor. Can construct ED using
 * a Notifiable owner, and optionally, a spec file and/or a debug flag.
 * There's a bug having to do with the wildcard binding, I'll look at that
 * in more detail tomorrow.
 *
 * Revision 1.12.4.10  2001/05/25 20:13:36  eb659
 *
 * allow ordering of rules;  (XMLspec needs update)
 * stateMachine instances are within the specification
 * StateMachineSpecification implement Comparable interface:
 * (higher priority objects appear smaller)
 * added EDNotifiable interface
 *
 * Revision 1.12.4.9  2001/05/23 21:40:42  eb659
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
 * Revision 1.12.4.8  2001/05/06 05:31:07  eb659
 *
 * Thoroughly re-tested all dynamic rulebase and fixed so that it
 * works with the new arch.
 *
 * Revision 1.12.4.7  2001/05/06 03:54:27  eb659
 *
 * Added support for checking multiple parents, and independent wild hashtables
 * for different paths. ED now has full functionality, and resiliency.
 * Now doing some additional testing for event sequences that actually use
 * the OR representation, and re-testing the dynamic rulebase, to make sure
 * it still works after the changes made.
 *
 * Revision 1.12.4.6  2001/05/02 00:04:27  eb659
 *
 * Tested and fixed a couple of things.
 * New architecture works, and can be tested using EDTest.
 * Reaper thread needs revision...
 * Can we get rid of internal 'loopback' notifications?
 *
 * Revision 1.12.4.5  2001/05/01 04:21:56  eb659
 *
 * Added revised reaper thread and re-enabled validation.
 * New version compiles and has 95% of the expected functionality. Next:
 * 1) testing & debugging
 * 2) if time permits, cool stuff like allowing multiple parents for each state, etc.
 *
 * Revision 1.12.4.4  2001/04/21 06:57:11  eb659
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
 * Revision 1.12.4.3  2001/04/18 04:37:54  eb659
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
 * Revision 1.12.4.2  2001/04/06 00:15:07  eb659
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
 * Revision 1.12.4.1  2001/04/03 01:09:13  eb659
 *
 *
 * OK this is my first upload...
 * Basically, most of the dynamic rulebase stuff has been accomplished.
 * the principal methods are in EDStatemanaged, but most of the files in ED
 * had to be modified, at least in some small way
 * enrico
 *
 * Revision 1.12  2001/02/28 18:02:45  jjp32
 * Removed ^M :)
 *
 * Revision 1.11  2001/02/05 06:43:14  png3
 * Modified to use Apache Xerces instead of Oracle XML parser
 *
 * Revision 1.10  2001/01/30 06:26:18  jjp32
 *
 * Lots and lots of updates.  EventDistiller is now of demo-quality.
 *
 * Revision 1.9  2001/01/30 00:24:50  jjp32
 *
 * Bug fixes, added test class
 *
 * Revision 1.8  2001/01/29 05:22:53  jjp32
 *
 * Reaper written - but it's probably a problem
 *
 * Revision 1.7  2001/01/29 04:58:55  jjp32
 *
 * Each rule can now have multiple attr/value pairs.
 *
 * Revision 1.6  2001/01/29 04:18:42  jjp32
 *
 * Lots of updates.  Doesn't compile yet, hopefully it will by the time I'm home :)
 *
 * Revision 1.5  2001/01/29 02:14:36  jjp32
 *
 * Support for multiple attributes on a output notification added.
 *
 * Added Workgroup Cache test rules
 *
 * Revision 1.4  2001/01/28 22:58:58  jjp32
 *
 * Wildcard support has been added
 *
 * Revision 1.3  2001/01/28 21:34:00  jjp32
 *
 * XML parsing complete; almost ready for demo
 *
 * Revision 1.2  2001/01/28 19:56:18  jjp32
 *
 * Added XML support to EDStateManager.  Supplied test rule file.
 *
 * Revision 1.1  2001/01/22 02:11:54  jjp32
 *
 * First full Siena-aware build of XUES!
 *
 */
public class EDStateManager implements Rulebase, Runnable, EDNotifiable, Comparable {

    /** The ed that owns us. */
    private EventDistiller ed = null;

    /** The internal dispatcher through which we receive instructions. */
    private EDBus bus = null;

    /** EDStateMachineSpecifications */
    Vector specifications = new Vector();

    /** ErrorManager to print errors to. */
    private EDErrorManager errorManager;

    /** SAX parser reference */
    private SAXParser sxp = null;

    /**
     * Constructs a new manager.
     * @param ed the EventDistiller that ownes us.
     */
    public EDStateManager(EventDistiller ed) {
	this.ed = ed;
	this.bus = ed.getBus();
	this.errorManager = ed.getErrorManager();
	String specFileName = ed.getSpecFile();

	// subscribe to internal dispatcher
	subscribe();

	// Do we have a spec filename?
	if(specFileName != null) {
	    // Initialize SAX parser and run it on the file
	    sxp = new SAXParser();
	    sxp.setContentHandler(new EDHandler(this));
	    try {
		sxp.parse(new InputSource(new FileInputStream(specFileName)));
	    }
	    catch(Exception e) {
		errorManager.println("FATAL: EDStateManager init failed:",
				      EDErrorManager.ERROR);
		e.printStackTrace();
	    }
	}
	errorManager.println("\n//*********** DONE INIT ***********//\n", EDErrorManager.MANAGER);

	// Start da reapah.  In a new thread.
	new Thread(this).start();
    }

    /**
     * Subscribe, so that we can get notifications asking us to
     * dynamically modify the state machines.
     */
    private void subscribe() {
	//specify what notifications we are interested in
	Filter f =  new Filter();
	f.addConstraint("Type", "EDInput");
	f.addConstraint("EDInput", "ManagerInstruction");
	bus.subscribe(f, this, this);
    }

    /** The reaper thread. Disposes state machines that have timed out. */
    public void run() {
	while(!ed.inShutdown) {
	    try { Thread.currentThread().sleep(EDConst.REAP_INTERVAL);  }
	    catch(InterruptedException ex) { ; }
	    //reap();
	}
	errorManager.println
	    ("reaper ceased due to shutdown", EDErrorManager.REAPER);
    }

    void reap() {
	errorManager.print("%", EDErrorManager.REAPER);

	synchronized(specifications) {
	    for (int i = 0; i < specifications.size(); i++) {
		Vector stateMachines =
		    ((EDStateMachineSpecification)specifications.get(i)).stateMachines;

		synchronized(stateMachines) {
		    for (int j = 0; j < stateMachines.size(); j++) {
			EDStateMachine reapand = (EDStateMachine)stateMachines.get(j);
			errorManager.println("EDStateManager: Attempting to reap " + reapand.myID,
						     EDErrorManager.REAPER);
			if(reapand.reap()) {
			    errorManager.println("REAPED " + reapand.myID, EDErrorManager.MANAGER);
			    stateMachines.remove(j);
			    j--;
			}
			else {
			    errorManager.println("EDStateManager: Did not reap " + reapand.myID,
						 EDErrorManager.REAPER);
			}
		    }
		}
	    }
	}
    }

    // EDNotifiable interface

    /**
     * Callback.  We receive callbacks to dynamically
     * change or query the state of the rules.
     * @param n the notification received
     */
    public boolean notify(Notification n) {
	errorManager.println("EDStateManager: Received notification "+ n, EDErrorManager.MANAGER);
	// What kind of notification is it?
	String a = n.getAttribute("Action").stringValue();

	if(a.equals("AddRule")) {
	    dynamicAddRule(n.getAttribute("Rule").stringValue());
	} else if(a.equals("RemoveRule")) {
	    dynamicRemoveRule(n.getAttribute("Rule").stringValue());
	} else if(a.equals("QueryRule")) {
	    dynamicQueryRule(n.getAttribute("Rule").stringValue());
	} else if(a.equals("QueryRules")) {
	    dynamicQueryRules();
	} else if(a.equals("FailAll")) {
	    ed.failAll();
	}

	/* no reason to absorb these events.
	 * also, they could be monitored by rules. */
	return false;
    }


    // comparable interface

    /**
     * We need to implement comparable to subscribe to EDBus.
     * Manager instructions always have
     * priority over any other notification.
     * @param o the object to compare to
     */
    public int compareTo(Object o) {
	if (this == o) return 0;
	return -1;
    }


    // methods to handle the dynamic rulebase

    /**
     * Dynamically add a rule.
     * @param s the XML representation of the new rule
     * @author enrico buonanno
     */
    public void dynamicAddRule(String s){
	errorManager.println("EDStateManager: adding rule", EDErrorManager.MANAGER);
	if(s == null) return;

	// Initialize SAX parser if we don-t have one
	if (sxp == null) {
	    sxp = new SAXParser();
	    sxp.setContentHandler(new EDHandler(this));
	}
	// parse the string - this effectively adds the rule
	try {
	    sxp.parse(new InputSource(new StringReader(s)));
	} catch(Exception e) {
	    errorManager.println("EDStateManager: could not parse rule specification:",
					 EDErrorManager.ERROR);
	    e.printStackTrace();
	    return;
	}
	EDStateMachineSpecification added =
	    (EDStateMachineSpecification)specifications.lastElement();
    }

  /**
   * Dynamically remove a rule.
   * NOTE: we remove the 1st SMspec with the given name - we assume only one spec per name
   *       also, matching the given name is NOT case-sensitive
   * @param s the name of the rule
   */
  public void dynamicRemoveRule(String s) {
      errorManager.println("EDStateManager: removing rule " + s, EDErrorManager.MANAGER);
      if(s == null) return;

      EDStateMachineSpecification spec = null;

      // remove the specification
      synchronized(specifications) {
	  for (int i = 0; i < specifications.size(); i++) {
	      if (((EDStateMachineSpecification)specifications.get
		   (i)).getName().equalsIgnoreCase(s)) {
		  // remember
		  spec = (EDStateMachineSpecification)specifications.get(i);

		  // kill all stateMachines instances
		  Vector stateMachines = spec.stateMachines;
		  synchronized(stateMachines) {
		      for(int j = 0;j < stateMachines.size(); j++)
			  ((EDStateMachine)stateMachines.get(j)).killAllStates();
		  }

		  // remove the spec
		  specifications.remove(i);
		  if (EventDistiller.DEBUG)
		  errorManager.println("EDStateManager: successfully removed rule " + i + " !",
					       EDErrorManager.MANAGER);
		  break; // we assume there's only one spec for each name
	      }
	  }
      }

      // if not found
      if(spec == null){
	  errorManager.println("ERROR: could not remove rule: name '" + s + "' not found",
				       EDErrorManager.MANAGER);
	  ed.sendPublic(KXNotification.EDError
			(KXNotification.EDERROR_RULEBASE,
			 "could not remove rule: name '" + s + "' not found"));
      }

  }

    /**
     * Dynamically query a rule.
     * Sends out a notification containing the XML representation of
     * the specified rule.
     * @param s the name of the rule to query
     */
    private void dynamicQueryRule(String s) {
	errorManager.println("EDStateManager: queried rule " + s, EDErrorManager.MANAGER);

	// find it
	for (int i = 0; i < specifications.size(); i++)
	    if (((EDStateMachineSpecification)specifications.get(i)).getName
		().equalsIgnoreCase(s)) {
		ed.sendPublic(KXNotification.EDOutputRule
			      (((EDStateMachineSpecification)specifications.get
				(i)).toXML(i)));
		break;
	    }
    }

    /**
     * Dynamically query a list of rules
     * Sends out a comma-delimited list of all rules in the current rulebase,
     * identified by name.
     */
    private void dynamicQueryRules() {
	String s = "";
	if (specifications.size() > 0) {
	    for (int i = 0; i < specifications.size(); i++)
		s = s + ((EDStateMachineSpecification)
			 specifications.get(i)).getName() + ",";
	    s = s.substring(0, s.length() - 1);
	}
	ed.sendPublic(KXNotification.EDOutputRules(s));
    }

    // XML rendering

    void write(File outputFile) {
        write(outputFile, this);
    }

    /**
     * Writes the rules of the given Rulebase to a file.
     * @param outputFile the file to write to
     * @param rulebase the source of the rules
     */
    static void write(File outputFile, Rulebase rulebase) {
        try {
            PrintWriter outputWriter = new PrintWriter(new BufferedWriter(new FileWriter(outputFile)));
            // start
            outputWriter.write("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
                               + "<rulebase xmlns=\"http://www.psl.cs.columbia.edu/2001/01/DistillerRule.xsd\">");
            // the rules
            for (int i = 0; i < rulebase.getSpecifications().size(); i++)
                outputWriter.write(((EDStateMachineSpecification)rulebase.getSpecifications().get(i)).toXML(i));
            // end
            outputWriter.write("</rulebase>");
            outputWriter.close();
        }
        catch(Exception ex) { ex.printStackTrace(); }
    }

    /**************************************************
       methods inherited from the Rulebase interface
    **************************************************/

    /** @return the Vector of EDStateSpecification */
    public Vector getSpecifications() { return specifications; }

    /**
     * Returns whether the given name already belongs to one of the rules
     * @param s the candidate name
     * @return whether the given name already belongs to one of the rules
     */
    public boolean hasName(String s) {
        for (int i = 0; i < specifications.size(); i++)
            if (((EDStateMachineSpecification)specifications.get(i)).getName().equals(s))
                return true;
        return false;
    }

    /**
     * Adds a given specification.
     * @param specPosition the index where the specification should be set.
     *        could be -1, if the specification is to be placed at the end
     * @param edsms the specification to add
     * @return whether the rule was added
     */
    public boolean addSpecification(int specPosition, EDStateMachineSpecification edsms) {
        errorManager.println("parsed rule: " + edsms.getName(),
                                     EDErrorManager.MANAGER);

	// don't allow duplicate names
	if (hasName(edsms.getName())) {
            publishError("duplicate name: " + edsms.getName());
            return false;
        }

        synchronized(specifications) {
            // where do we add it?
            int targetPosition = specifications.size();
            // at the end, if not specified
            if (specPosition >= 0 && specPosition <= targetPosition) targetPosition = specPosition;
            specifications.add(targetPosition, edsms);
            // initialize the specification
            edsms.init(this);
        }
        return true;
    }

    /**
     * Publishes an error in the definition of a specification.
     * @param error a string representation of the error
     */
    public void publishError(String error) {
        // print to output
        errorManager.println("ERROR: EDStateManager: cannot add rule:\n" + error,
                             EDErrorManager.ERROR);
        // send an error notification
        ed.sendPublic(KXNotification.EDError(KXNotification.EDERROR_RULEBASE, error));
    }

    // standard methods

    /** @return the eventDistiller */
    public EventDistiller getEventDistiller(){ return ed; }

    /** @return the siena bus */
    public EDBus getBus(){ return this.bus; }
}

/**
 * An interface to be implemented by those classes
 * that have a vector of EDStateSpecification
 */
interface Rulebase{

    /** @return the Vector of EDStateSpecification */
    Vector getSpecifications();

    /**
     * Returns whether the given name already belongs to one of the rules
     * @param s the candidate name
     * @return whether the given name already belongs to one of the rules
     */
    public boolean hasName(String s);

    /**
     * Adds a given specification.
     * @param position the index where the specification should be set.
     *        could be -1, if the specification is to be placed at the end
     * @param edsms the specification to add
     * @return whether the rule was added
     */
    boolean addSpecification(int position, EDStateMachineSpecification edsms);

    /**
     * Publishes an error in the definition of a specification.
     * @param error a string representation of the error
     */
    void publishError(String error);
}

/**
 * Class that handles the XML parsing.
 */
class EDHandler extends DefaultHandler {

    /** The class where the Specifications are to be stored. */
    Rulebase r = null;

    // variables used for parsing only

    /** The current statemachine being built. */
    private EDStateMachineSpecification currentEdsms = null;
    /** The positoin where we will place the current sped. */
    private int currentPosition = -1;
    /** The current state being built. */
    private EDState currentState = null;
    /** The current action being built. */
    private Notification currentAction = null;
    /** What are we currently parsing? */
    private int currentMode = -1;
    /** Currently parsing state */
    private static final int PARSINGSTATE = 1;
    /** Currently parsing action */
    private static final int PARSINGACTION = 2;

    /**
     * Constructs a new EDHandler
     */
    public EDHandler(Rulebase r) {
        this.r = r;
    }

    // methods for parsing

    /** Handle the beginning of a SAX element. */
    public void startElement(String uri, String localName, String qName,
			     Attributes attributes) throws SAXException {

	if(localName.equals("rule")) { // Start of new EDSMS
	    currentEdsms = new EDStateMachineSpecification
		(attributes.getValue("", "name"));

            // position
	    String s = attributes.getValue("", "position");
	    if (s != null) currentPosition = Integer.parseInt(s);

            // instantiation policy
	    s = attributes.getValue("", "instantiation");
	    if (s != null)
                try { currentEdsms.setInstantiationPolicy(Integer.parseInt(s)); }
	        catch(IllegalArgumentException ex) { System.err.println(ex); }
	}

	if(localName.equals("state")) { // Start of new state
	    try {
		currentMode = PARSINGSTATE;
		currentState = new
		    EDState(attributes.getValue("", "name"),
			    Integer.parseInt(attributes.getValue("","timebound")),
			    attributes.getValue("", "children"),
			    attributes.getValue("", "actions"),
			    attributes.getValue("", "fail_actions"));

		// absorb
		String s = attributes.getValue("", "absorb");
		if (s != null && s.equals("true")) currentState.setAbsorb(true);

		// count
		s = attributes.getValue("", "count");
		if (s != null) currentState.setCount(Integer.parseInt(s));

		currentEdsms.addState(currentState);
	    }
            catch(Exception e) {
		System.out.println("FATAL: EDStateManager init failed");
		e.printStackTrace();
		System.exit(-1);
	    }
	}

	else if(localName.equals("notification")) {
	    // Start of new notification
	    currentAction = new Notification();
	    currentEdsms.addAction(attributes.getValue("", "name"), currentAction);
	    currentMode = PARSINGACTION;
	}

        else if(localName.equals("attribute")) { // Start of new attribute
            AttributeValue attributevalue =
                makeAttribute(attributes.getValue("", "value"),
                              attributes.getValue("", "type"));
	    // Create the attribute
            /*String s5 = attributes.getValue("", "name");
            AttributeValue attributevalue = null;
            String s6 = attributes.getValue("", "type");

            // wrap the correct type in an attribute
            if(s6 == null || s6.equals(EDConst.TYPE_NAMES[1]))
                attributevalue = new AttributeValue(attributes.getValue("", "value"));
            else if(attributes.getValue("", "type").equals(EDConst.TYPE_NAMES[2]))
                attributevalue = new AttributeValue(Integer.parseInt(attributes.getValue("", "value")));
            else if(attributes.getValue("", "type").equals(EDConst.TYPE_NAMES[2]))
                attributevalue = new AttributeValue(Long.parseLong(attributes.getValue("", "value")));
            else if(attributes.getValue("", "type").equals(EDConst.TYPE_NAMES[3]))
                attributevalue = new AttributeValue(Double.parseDouble(attributes.getValue("", "value")));
            else if(attributes.getValue("", "type").equals(EDConst.TYPE_NAMES[4]))
                attributevalue = new AttributeValue(Boolean.getBoolean(attributes.getValue("", "value")));
            */
            short word0 = 1;
            String s7 = attributes.getValue("", "op");
            if(s7 != null)
                word0 = Op.op(s7);
            s7 = attributevalue.stringValue();
            if(attributevalue.getType() == 1 && s7.startsWith("*") && !s7.startsWith("**"))
                word0 = 8;

            // add the attribute
            switch(currentMode){
            case PARSINGSTATE:
                AttributeConstraint attributeconstraint = new AttributeConstraint(word0, attributevalue);
                currentState.addConstraint(attributes.getValue("", "name"), attributeconstraint);
                break;

            case PARSINGACTION:
                currentAction.putAttribute(attributes.getValue("", "name"), attributevalue);
                break;

            default:
                System.out.println("FATAL: EDStateManager init failed in determining mode");
                System.exit(-1);
                break;
            }
        }
    }

    /** Handle the end of a SAX element. */
    public void endElement(String namespaceURI, String localName, String qName)
        throws SAXException {
	if(localName.equals("rule")) {

	    // is the specified rule legal?
	    String error = currentEdsms.checkConsistency();

	    if (error == null) {
                r.addSpecification(currentPosition, currentEdsms);
                currentPosition = -1;
	    }
	    else { // specification is ill-defined
                r.publishError(error);
	    }
	}
    }

    /**
     * Returns an attribute value, with value and type specified as strings
     * @param val the string representation of the value
     * @param type the type of teh value
     * @return the desired AttributeValue
     */
    public static AttributeValue makeAttribute(String value, String type) {
        AttributeValue attributevalue = null;
        // wrap the correct type in an attribute
        if(type == null || type.equals(EDConst.TYPE_NAMES[1]))
            attributevalue = new AttributeValue(value);
        else if(type.equals(EDConst.TYPE_NAMES[2]))
            attributevalue = new AttributeValue(Integer.parseInt(value));
        else if(type.equals(EDConst.TYPE_NAMES[2]))
            attributevalue = new AttributeValue(Long.parseLong(value));
        else if(type.equals(EDConst.TYPE_NAMES[3]))
            attributevalue = new AttributeValue(Double.parseDouble(value));
        else if(type.equals(EDConst.TYPE_NAMES[4]))
            attributevalue = new AttributeValue(Boolean.getBoolean(value));
        return attributevalue;
    }
}














