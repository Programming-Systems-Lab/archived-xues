
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
 * Revision 1.13  2001-04-03 01:09:14  eb659
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

    /** This hashes vectors with EDStateMachineSpecifications */
    private Vector stateMachineTemplates = null;
    private Vector stateMachines = null;

    /** SAX parser reference */
    private SAXParser sxp = null;

    /** The current statemachine being built. */
    private EDStateMachineSpecification currentEdsms = null;

    /** The current state being built. */
    private EDState currentState = null;

    /** The current action being built.  Unused right now - see code
     * where currentAction is commented out */
    //  private Notification currentAction = null;

    /** What are we currently parsing? */
    private int currentMode = -1;
    
    /** Currently parsing state */
    private static final int PARSINGSTATE = 1;

    /** Currently parsing action */
    private static final int PARSINGACTION = 2;
  
  /**
   * XML main test
   */
  public static void main(String[] args) {
    if(args.length == 0) {
      System.err.println("Must supply XML filename");
      System.exit(-1);
    }
    new EDStateManager(null, null, args[0]);
  }

  /**
   * Test CTOR.  Initialize with test state machine.
   */
  public EDStateManager(Siena siena, EventDistiller ed) {
    this(siena,ed,null);
    stateMachineTemplates.
      addElement(EDStateMachineSpecification.buildDemoSpec(siena,this));
  }
  
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
    if(specFilename == null) return; // No further
    // Initialize SAX parser and run it on the file
    sxp = new SAXParser();
    sxp.setContentHandler(this);
    try {
      // png3
      // sxp.parse(new FileInputStream(specFilename));
      sxp.parse(new InputSource(new FileInputStream(specFilename)));
    } catch(Exception e) {
      System.err.println("FATAL: EDStateManager init failed:");
      e.printStackTrace();
    }

    // Start da reapah.  In a new thread.
    new Thread(this).start();
  }

  /**
   * The reaper thread.
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
	      System.err.println("EDStateManager: Attempting to reap " + 
				 e.myID);
	    
	    if(e.reap()) {
	      if(EventDistiller.DEBUG)
		System.err.println("EDStateManager: Reaped.");
	      stateMachines.removeElementAt(offset);
	    } else offset++;
	  }
	}
      }
    }
    catch(InterruptedException ex) { ; }
  }

  /**
   * Callback by state machine to indicate it has been created, and
   * to add it to the queue for garbage-collection.
   */
  void addMachine(EDStateMachine m) {
    synchronized(stateMachines) {
      stateMachines.addElement(m);
    }
  }

  /**
   * Method call made by EDStateMachines to indicate completion.
   */
  void finish(EDStateMachine m, Notification n) {
    // Propagate the notification up, but wrap it in KX form
    ed.sendPublic(KXNotification.EDOutputKXNotification(12345,n));
    // Garbage-collect the State Machine
    synchronized(stateMachines) {
      stateMachines.removeElement(m);
    }
  }

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
	currentEdsms = new EDStateMachineSpecification(attributes.getValue("", "name"),
						       "" + (idCounter++),
						       siena, this);
      stateMachineTemplates.addElement(currentEdsms);
    }

    if(localName.equals("state")) { // Start of new state
      if(EventDistiller.DEBUG) {
	System.out.println("--> timebound = " +
			   attributes.getValue("","timebound"));
      }

      try {
	currentMode = PARSINGSTATE;
	currentState = new 
	  EDState(Integer.parseInt(attributes.getValue("","timebound")));
	currentEdsms.addState(currentState);
      } catch(Exception e) {
	System.err.println("FATAL: EDStateManager init failed:");
	e.printStackTrace();
	System.exit(-1);
      }	
    }

    
    if(localName.equals("notification")) { // Start of new notification
      // We don't create a new notification, since action can only be
      // one action.  Later we will want to change this.  Right now we
      // can append attr/val pairs to an existing action in edsms.

      //      currentAction = new Notification();
      //      currentEdsms.addAction(currentAction);
      currentMode = PARSINGACTION;
    }

    /////

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
	currentState.add(attr, val);
	break;
      case PARSINGACTION:
	//	currentAction.add(attr, val);
	currentEdsms.addAction(attr,val);
	break;
      default:
	System.err.println("FATAL: EDStateManager init failed in determining mode");
	System.exit(-1);
      }
    }
  } 

  /*
   * Handle the end of a SAX element.
   */
  public void endElement(String namespaceURI, String localName, String qName) 
  throws SAXException {
    if(EventDistiller.DEBUG) System.out.println("parsed " + localName + "," + 
						qName);
    if(localName.equals("rule")) {
      // This state machine is done, subscribe it to Siena so it can
      // start receiving notifications
      currentEdsms.subscribe();
    }
  }

  /**
   * Subscribe, so that we can get notifications asking us to 
   * dynamically modify the state machines.
   * @author enrico buonanno
   */
  public void subscribe() {
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
      }
  }
		
  /** Unused Siena construct. */
  public void notify(Notification[] s) { ; }

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

      // we don't allow duplicate names - or duplicate definitions
      for (int i = 0; i < stateMachines.size(); i++)
	  if (((EDStateMachineSpecification)stateMachineTemplates.get(i)).getName
	      ().equalsIgnoreCase(added.getName())) {
	      stateMachineTemplates.remove(added);
	      if (EventDistiller.DEBUG)  System.out.println("EDStateManager: cannot add rule "
							    + added.getName() + ": name exists");
	  }
      // here there may be a problem: what if the spec has already precreated?
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
      int i = 0;

      while (i < stateMachineTemplates.size()) {
	  if (((EDStateMachineSpecification)stateMachineTemplates.get
	       (i)).getName().equalsIgnoreCase(s)) {
	      // remember
	      spec = (EDStateMachineSpecification)stateMachineTemplates.get(i);
	      // unsubscribe
	      spec.unsubscribe();
	      // remove
	      stateMachineTemplates.remove(i);
	      if (EventDistiller.DEBUG) 
		  System.out.println("EDStateManager: successfully removed rule " + i + " !");
	      break; // we assume there's only one spec for each name
	  }
	  else i++;
      }
	      
	     
      // remove all stateMachines with this specification
      int j = 0;
      while (j < stateMachines.size()) {
	  if (((EDStateMachine)stateMachines.get(j)).getSpecification() == spec) {
	      // unsubscribe
	      ((EDStateMachine)stateMachines.get(j)).unsubscribe();
	      // remove
	      stateMachines.remove(j);
	      if (EventDistiller.DEBUG) 
		  System.out.println("EDStateManager: successfully removed state machine " + 
				     i + ":" + j + " !");
	  }
	  else i++;
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
	EDStateMachineSpecification sm = null;

	// find it
	for (int i = 0; i < stateMachineTemplates.size(); i++) 
	    if (((EDStateMachineSpecification)stateMachineTemplates.get(i)).getName
		().equalsIgnoreCase(s))
		sm = (EDStateMachineSpecification)stateMachineTemplates.get(i);

	// send it
	if (sm != null) {
	    Notification n = new Notification();
	    n.putAttribute("Rule", sm.toXML());
	    /* wrap it in KX form, for now the generic type.
	       eventually, we should distinguish this from the rule fired events;
	       also, we need to look at how to address the sender... */
	    ed.sendPublic(KXNotification.EDOutputKXNotification(12345, n));
	}
    }
}



