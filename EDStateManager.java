
package psl.xues;

import java.io.*;
import java.util.*;
import siena.*;

import oracle.xml.parser.v2.SAXParser;
import org.xml.sax.Attributes;
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
 * TODO:
 * - More efficient way of garbage collecting state machines
 * 
 * @author Janak J Parekh (jjp32@cs.columbia.edu)
 * @version 1.0
 *
 * $Log$
 * Revision 1.3  2001-01-28 21:34:00  jjp32
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
public class EDStateManager extends DefaultHandler implements Runnable {
  private EventDistiller ed = null;
  private Siena siena = null;
  /** Currently unused.  I don't remember what this is for. */
  private int idCounter = -1;
  /** This hashes vectors with EDStateMachineSpecifications */
  private Vector stateMachineTemplates = null;
  private Vector stateMachines = null;
  /** SAX parser reference */
  private SAXParser sxp = null;
  // The current state/statemachine/etc being built.
  private EDStateMachineSpecification currentEdsms = null;
  
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
    // Do we have a spec filename?
    if(specFilename == null) return; // No further
    // Initialize SAX parser and run it on the file
    sxp = new SAXParser();
    sxp.setContentHandler(this);
    try {
      sxp.parse(new FileInputStream(specFilename));
    } catch(Exception e) {
      System.err.println("FATAL: EDStateManager init failed:");
      e.printStackTrace();
    }
  }

  /**
   * The reaper thread.
   */
  public void run() {
    //...
    try { Thread.currentThread().sleep(1000); }
    catch(InterruptedException ex) { ; }
  }

  /**
   * Callback by state machine to indicate it has been created, and
   * to add it to the queue for garbage-collection.
   */
  void addMachine(EDStateMachine m) {
    stateMachines.addElement(m);
  }

  /**
   * Method call made by EDStateMachines to indicate completion.
   */
  void finish(EDStateMachine m, Notification n) {
    // Propagate the notification up
    ed.sendPublic(n);
    // Garbage-collect the State Machine
    stateMachines.removeElement(m);
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
      currentEdsms = new EDStateMachineSpecification(siena,this);
      stateMachineTemplates.addElement(currentEdsms);
    }
    if(localName.equals("state")) { 
      if(EventDistiller.DEBUG) {
	System.out.println("--> attributename = " + 
			   attributes.getValue("","attributename"));
	System.out.println("--> value = " + 
			   attributes.getValue("","value"));
	System.out.println("--> timebound = " +
			   attributes.getValue("","timebound"));
      }

      // Create the state
      try {
	currentEdsms.addState(new EDState(attributes.getValue("","attributename"),
					  attributes.getValue("","value"),
					  Integer.parseInt(attributes.getValue("","timebound"))));
      } catch(Exception e) {
	System.err.println("FATAL: EDStateManager init failed:");
	e.printStackTrace();
      }	
    }
    if(localName.equals("attribute")) {
      if(EventDistiller.DEBUG) {
	System.out.println("--> name = " +
			   attributes.getValue("","name"));
	System.out.println("--> value = " +
			   attributes.getValue("","value"));
      }

      try {
	currentEdsms.
	  setAction(attributes.getValue("","name"),
		    new AttributeValue(attributes.getValue("","value")));
      } catch(Exception e) {
	System.err.println("FATAL: EDStateManager init failed:");
	e.printStackTrace();
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

    

  }
}

/**
 * Class to specify a machine template.
 */
class EDStateMachineSpecification implements Notifiable {
  private Vector stateArray;
  private Notification action;
  private Siena siena;
  private EDStateManager edsm;

  /**
   * Basic CTOR.  Make sure to add states.
   */
  public EDStateMachineSpecification(Siena siena, EDStateManager edsm) {
    this.siena = siena;
    this.edsm = edsm;
    stateArray = new Vector();
  }

  /** Demo test factory, don't use otherwise */
  public static EDStateMachineSpecification buildDemoSpec(Siena siena,
							  EDStateManager edsm)
  { 
    EDStateMachineSpecification edsms = 
      new EDStateMachineSpecification(siena,edsm);
    edsms.stateArray.addElement(new EDState("temperature","60",-1));
    edsms.action = new Notification();
    edsms.action.putAttribute("itworked","true");
    edsms.subscribe();
    return edsms;
  }

  /**
   * Add a state.
   *
   * NOTE! For this specification to become active, you must set the action
   * AFTER adding states.
   *
   * @param e The state.
   */
  public void addState(EDState e) {
    stateArray.addElement(e);
  }

  /**
   * Set action.  (Only one action for now)
   *
   * NOTE! IMPORTANT!  You *must* set an action for this state machine
   * to become live.  Additionally, the state machine assumes that
   * setting the action implicitly tells it that the states have been set up.
   * You may add states later, but be forewarned there may already be state
   * machines executing on the current state setup.
   */
  public void setAction(String attr, AttributeValue val) {
    action = new Notification();
    action.putAttribute(attr,val);
    subscribe(); // Do first event subscription
  }

  /**
   * Subscribe based on the first state.  This way, we can create
   * instances when necessary.
   */
  public void subscribe() {
    try {
      siena.subscribe(((EDState)stateArray.elementAt(0)).buildSienaFilter(),
		      this);
    } catch(SienaException e) {
      e.printStackTrace();
    }
  }

  public void notify(Notification n) {
    if(EventDistiller.DEBUG) 
      System.err.println("[EDStateManagerSpecification] Received notification " + n);
    // Create the appropriate state machine(s).  We assume the state
    // machine will register itself with the manager.
    EDStateMachine sm = new EDStateMachine(siena, edsm, stateArray, 1,
					   action);
  }

  /** Unused Siena construct. */
  public void notify(Notification[] s) { ; }
}
