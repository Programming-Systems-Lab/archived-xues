
package psl.xues;

import psl.kx.KXNotification;
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
 * Copyright (c) 2000: The Trustees of Columbia University in the
 * City of New York.  All Rights Reserved.
 *
 * TODO:
 * - More efficient way of garbage collecting state machines
 * 
 * @author Janak J Parekh (jjp32@cs.columbia.edu)
 * @version 1.0
 *
 * $Log$
 * Revision 1.7  2001-01-29 04:58:55  jjp32
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
    // Propagate the notification up, but wrap it in KX form
    ed.sendPublic(KXNotification.EDOutputKXNotification(12345,n));
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

    /////

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

    /////
    
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

  /**
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
}
