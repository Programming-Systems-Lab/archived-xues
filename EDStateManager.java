
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
 * Revision 1.16  2001-05-29 17:31:08  jjp32
 * Reflected change from branch on main version (re spec file)
 *
 * Revision 1.15  2001/05/21 00:43:04  jjp32
 * Rolled in Enrico's changes to main Xues trunk
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
public class EDStateManager extends DefaultHandler implements Runnable, Notifiable {
  
    private EventDistiller ed = null;
    private Siena siena = null;
  
    /** Counts EDStateMachines for ID tagging */
    private int idCounter = 0;

    /** EDStateMachineSpecifications */
    private Vector stateMachineTemplates = null;
    /** EDStateMachines */
    private Vector stateMachines = null;

    // variables used for parsing only

    /** SAX parser reference */
    private SAXParser sxp = null;
    /** The current statemachine being built. */
    private EDStateMachineSpecification currentEdsms = null;
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
  
    /** XML main test 
    public static void main(String[] args) {
	if(args.length == 0) {
	    System.err.println("Must supply XML filename");
	    System.exit(-1);
	}
	new EDStateManager(null, null, args[0]);
	}*/

    /** Test CTOR.  Initialize with test state machine. 
    public EDStateManager(Siena siena, EventDistiller ed) {
	this(siena,ed,null);
	stateMachineTemplates.
	    addElement(EDStateMachineSpecification.buildDemoSpec(siena,this));
	    }*/
  
  /**
   * Regular CTOR.  Utilize XML specification file to build state machines.
   */
  public EDStateManager(Siena siena, EventDistiller ed, 
			String specFilename) {
    this.ed = ed;
    this.siena = siena;
    this.stateMachineTemplates = new Vector();
    this.stateMachines = new Vector();
    
    // subscribe to internal siena
    subscribe();

    // Do we have a spec filename?
    if(specFilename != null) {
      // Initialize SAX parser and run it on the file
      sxp = new SAXParser();
      sxp.setContentHandler(this);
      try {
	sxp.parse(new InputSource(new FileInputStream(specFilename)));
      } catch(Exception e) {
	System.err.println("FATAL: EDStateManager init failed:");
	e.printStackTrace();
      }
    }

    // Start da reapah.  In a new thread.
    new Thread(this).start();
  }

    /** 
     * Called when a new machine of the given specification needs to be created.
     * @param spec the type of machine that needs to be constructed
     */
    public void addStateMachine(EDStateMachineSpecification spec) {
	synchronized(stateMachines) {
	    stateMachines.addElement(new EDStateMachine(spec));
	}
    }

  /**
   * The reaper thread. Disposes state machines that 
   * have timed out.
   */
  public void run() {
    //...
      try { 
      while(true) {
	Thread.currentThread().sleep(1000); 
	// Reap!
	if(EventDistiller.DEBUG) System.err.println("EDStateManager: Reaping...");

	synchronized(stateMachines) {
	  // Only reap if there *are* state machines
	  int offset = 0;
	  
	  while(offset < stateMachines.size()) {
	    EDStateMachine e = (EDStateMachine)stateMachines.elementAt(offset);
	    if(EventDistiller.DEBUG)
	      System.err.println("EDStateManager: Attempting to reap " + e.myID);
	    
	    if(e.reap()) {
	      if(EventDistiller.DEBUG)
		System.err.println("EDStateManager: Reaped." + e.myID);
	      stateMachines.removeElementAt(offset);
	    } else offset++;
	  }
	}
      }
    }
    catch(InterruptedException ex) { ; }
  }

  /* We don't hav ethis any more... now states handle their own notifications
   * and when all states are dead, the machine is reaped
   *
   * Method call made by EDStateMachines to indicate completion.
   * @param m the state machine that makes the call
   * @param a the Vector containing the notifications to be sent
   *
  void finish(EDStateMachine m, Vector a) {
      // send all the notifications/actions
      for (int i = 0; i < a.size(); i++) {
	  Notification n = (Notification)a.get(i);

	  if (n.getAttribute("Internal") != null && n.getAttribute("Internal").booleanValue()) {
	      /* if this is an internal notification
	       * we just send it through to ourselves. 
	      try { siena.publish(KXNotification.EDInternalNotification(n)); }
	      catch (SienaException se) { se.printStackTrace(); }
	  }
	  else { // Propagate the notification up, but wrap it in KX form
	      ed.sendPublic(KXNotification.EDOutputKXNotification(12345,n));
	  }
      }

      // Garbage-collect the State Machine
      synchronized(stateMachines) {
	  stateMachines.removeElement(m);
      }
  }*/

    // methods for parsing

  /**
   * Handle the beginning of a SAX element.  We only use this in
   * certain cases, e.g. in the case of a new rule where we have to
   * instantiate a new currentStateMachine.
   */
  public void startElement(String uri, String localName, String qName,
			   Attributes attributes) throws SAXException {
    if(EventDistiller.DEBUG) System.out.println("parsing " + localName + "," + 
						qName);
    if(localName.equals("rule")) { // Start of new EDSMS
	currentEdsms = new  EDStateMachineSpecification
	    (attributes.getValue("", "name"), "" + (idCounter++), siena, this);
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
	currentEdsms.addState(currentState);
      } catch(Exception e) {
	System.err.println("FATAL: EDStateManager init failed:");
	e.printStackTrace();
	System.exit(-1);
      }	
    }

    if(localName.equals("notification")) { 
	// Start of new notification
	currentAction = new Notification();
	currentEdsms.addAction(attributes.getValue("", "name"), currentAction);
	currentMode = PARSINGACTION;
    }

    if(localName.equals("attribute")) { // Start of new attribute
      if(EventDistiller.DEBUG) {
	System.out.println("--> name = " + 
			   attributes.getValue("","name"));
	System.out.println("--> value = " + 
			   attributes.getValue("","value"));
      }
      // Create the attribute
      String attr = attributes.getValue("","name");
      AttributeValue val = new AttributeValue(attributes.getValue("","value"));

      // Add it (somewhere)
      switch(currentMode) {
      case PARSINGSTATE:
	currentState.putAttribute(attr, val);
	break;
      case PARSINGACTION:
	currentAction.putAttribute(attr,val);
	break;
      default:
	System.err.println("FATAL: EDStateManager init failed in determining mode");
	System.exit(-1);
      }
    }
  } 

    /**
     * Handle the end of a SAX element.
     */
    public void endElement(String namespaceURI, String localName, String qName) 
      throws SAXException {
	if(EventDistiller.DEBUG) System.out.println("parsed " + localName + "," + 
						    qName);
	if(localName.equals("rule")) {
	    // don't allow duplicate names
	    for (int i = 0; i < stateMachines.size(); i++)
		if (((EDStateMachineSpecification)stateMachineTemplates.get(i)).getName
		    ().equalsIgnoreCase(currentEdsms.getName())) {
		    if (EventDistiller.DEBUG)  
			System.err.println("ERROR: EDStateManager: cannot add rule "
					   + currentEdsms.getName() + ": name exists");
		    return; // don't add it
	    }
	    // ok, now we can add it
	    stateMachineTemplates.addElement(currentEdsms);
	    currentEdsms.findInitialStates(); 
	    /* instantiate the first child constructed on the model of 
	     * the specification, so that it starts receiving notifications */
	    stateMachines.add(new EDStateMachine(currentEdsms));
	}
    }

    // siena methods

  /**
   * Subscribe, so that we can get notifications asking us to 
   * dynamically modify the state machines.
   * @author enrico buonanno
   */
  private void subscribe() {
    try {
	//specify what notifications we are interested in
	Filter f =  new Filter();
	f.addConstraint("Type", "EDInput");
	f.addConstraint("EDInput", "ManagerInstruction");
	siena.subscribe(f, this);
	if(EventDistiller.DEBUG)
	    System.err.println("EDStateManager: Subscribing with filter " + f);
    } catch(SienaException e) {
	e.printStackTrace();
    }
  }

  /**
   * Siena Callback.  We receive callbacks to dynamically
   * change or query the state of the rules.
   * @param n the notification received
   */
  public void notify(Notification n) {
      if(EventDistiller.DEBUG) System.out.println("EDStateManager: Received notification "+n);
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
      }
  }
		
  /** Unused Siena construct. */
  public void notify(Notification[] s) { ; }

    // methods to handle the dynamic rulebase

    /**
     * Dynamically add a rule.
     * @param s the XML representation of the new rule
     * @author enrico buonanno
     */
    public void dynamicAddRule(String s){
	if (EventDistiller.DEBUG) System.out.println("EDStateManager: adding rule");
	if(s == null) return; 
	
	// Initialize SAX parser if we don-t have one
	if (sxp == null) {
	    sxp = new SAXParser();
	    sxp.setContentHandler(this);
	}
	// parse the string - this effectively adds the rule
	try {
	    sxp.parse(new InputSource(new StringReader(s)));
	} catch(Exception e) {
	    System.err.println("EDStateManager: could not parse rule specification:");
	    e.printStackTrace();
	    return;
	}
	EDStateMachineSpecification added = 
	    (EDStateMachineSpecification)stateMachineTemplates.lastElement();
	if (EventDistiller.DEBUG) 
	    System.out.println("EDStateManager: adding rule\n" + added.getName());
	
    }

  /**
   * Dynamically remove a rule.
   * NOTE: we remove the 1st SMspec with the given name - we assume only one spec per name
   *       also, matching the given name is NOT case-sensitive
   * @param s the name of the rule
   */
  public void dynamicRemoveRule(String s) {
      if (EventDistiller.DEBUG) System.out.println("EDStateManager: removing rule " + s);
      if(s == null) return; 

      EDStateMachineSpecification spec = null;     

      // remove the specification      
      for (int i = 0; i < stateMachineTemplates.size(); i++) {
	  if (((EDStateMachineSpecification)stateMachineTemplates.get
	       (i)).getName().equalsIgnoreCase(s)) {
	      // remember
	      spec = (EDStateMachineSpecification)stateMachineTemplates.get(i);
	      // remove
	      stateMachineTemplates.remove(i);
	      if (EventDistiller.DEBUG) 
		  System.out.println("EDStateManager: successfully removed rule " + i + " !");
	      break; // we assume there's only one spec for each name
	  }
      }

      // if not found forget it
      if(spec == null){
	  if(EventDistiller.DEBUG) 
	      System.out.println("EDStateManager: could not remove rule: none matched");
	  return;
      }
	     
      // kill all stateMachines with this specification
      synchronized(stateMachines) {
	  for(int j = 0;j < stateMachines.size(); j++) {
	      if (((EDStateMachine)stateMachines.get(j)).getSpecification() == spec) {
		  // unsubscribe
		  ((EDStateMachine)stateMachines.get(j)).killAllStates();
		  // remove
		  stateMachines.remove(j);
		  j--;
	      }
	  }
      }
  }

    /**
     * Dynamically query a rule.
     * Sends out a notification containing the XML representation of 
     * the specified rule.
     * @param s the name of the rule to query
     */
    private void dynamicQueryRule(String s) {
	if (EventDistiller.DEBUG) System.out.println("EDStateManager: queried rule " + s);

	// find it
	for (int i = 0; i < stateMachineTemplates.size(); i++) 
	    if (((EDStateMachineSpecification)stateMachineTemplates.get(i)).getName
		().equalsIgnoreCase(s)) {
		ed.sendPublic(KXNotification.EDOutputRule
			      (((EDStateMachineSpecification)stateMachineTemplates.get
				(i)).toXML()));
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
	if (stateMachineTemplates.size() > 0) {
	    for (int i = 0; i < stateMachineTemplates.size(); i++) 
		s = s + ((EDStateMachineSpecification)
			 stateMachineTemplates.get(i)).getName() + ",";
	    s = s.substring(0, s.length() - 1);
	}
	ed.sendPublic(KXNotification.EDOutputRules(s));
    }

    // standard methods

    /** @return the eventDistiller */ 
    public EventDistiller getEventDistiller(){ return ed; }

    /** @return the siena bus */
    public Siena getSiena(){ return this.siena; }
}

















