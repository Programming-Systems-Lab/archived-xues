package psl.xues.ed;

import psl.kx.KXNotification;
import java.io.*;
import java.util.*;
import siena.*;

import org.apache.xerces.parsers.SAXParser;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import org.apache.log4j.Category;

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
 * @version $Revision$
 */
public class EDStateManager implements Rulebase, Runnable, EDNotifiable, 
Comparable {
  /** Debugging context */
  static Category debug =
  Category.getInstance(EDStateManager.class.getName());
    
  /** The ed that owns us. */
  private EventDistiller ed = null;
  
  /** The internal dispatcher through which we receive instructions. */
  private EDBus bus = null;
  
  /** EDStateMachineSpecifications */
  Vector specifications = new Vector();
  
  /** SAX parser reference */
  private SAXParser sxp = null;
  
  /**
   * Constructs a new manager.
   * @param ed the EventDistiller that ownes us.
   */
  public EDStateManager(EventDistiller ed) {
    this.ed = ed;
    this.bus = ed.getBus();
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
        debug.fatal("EDStateManager init failed", e);
        System.exit(-1);                         // XXX - should we do this?
      }
    }
    debug.info("Initialization complete");
    
    // Start the reaper in a new thread.
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
    debug.info("Reaper started");
    while(!ed.inShutdown) {
      try { Thread.currentThread().sleep(EDConst.REAP_INTERVAL);  }
      catch(InterruptedException ex) { ; }
      //reap();
    }
    debug.info("In shutdown: no longer reaping");
  }
  
  void reap() {
    //    errorManager.print("%", EDErrorManager.REAPER);
    
    synchronized(specifications) {
      for (int i = 0; i < specifications.size(); i++) {
        Vector stateMachines =
        ((EDStateMachineSpecification)specifications.get(i)).stateMachines;
        
        synchronized(stateMachines) {
          for (int j = 0; j < stateMachines.size(); j++) {
            EDStateMachine reapand = (EDStateMachine)stateMachines.get(j);
            debug.debug("Attempting to reap " + reapand.myID);
            if(reapand.reap()) {
              debug.debug("Reap of " + reapand.myID + " successful!");
              stateMachines.remove(j);
              j--;
            }
            else {
              debug.debug("Did not reap " + reapand.myID);
            }
          }
        }
      }
    }
  }
  
  /**
   * Callback.  We receive callbacks to dynamically
   * change or query the state of the rules.
   * @param n the notification received
   */
  public boolean notify(Notification n) {
    debug.debug("Received notification " + n);
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
    debug.info("Adding rule " + s.substring(0, 30));
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
      debug.error("Could not parse rule specification", e);
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
    debug.info("Removing rule " + s);
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
          debug.debug("Successfully removed rule " + i);
          break; // we assume there's only one spec for each name
        }
      }
    }
    
    // if not found
    if(spec == null){
      debug.error("Could not remove rule '" + s + "': not found");
      ed.sendPublic(KXNotification.EDError(KXNotification.EDERROR_RULEBASE,
      "Could not remove rule '" + s + "': not found"));
    }
  }
  
  /**
   * Dynamically query a rule.
   * Sends out a notification containing the XML representation of
   * the specified rule.
   * @param s the name of the rule to query
   */
  private void dynamicQueryRule(String s) {
    debug.info("Querying rule " + s);
    
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
   * methods inherited from the Rulebase interface
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
    debug.debug("Parsed rule '" + edsms.getName());
    
    // don't allow duplicate names
    if (hasName(edsms.getName())) {
      publishError("Could not add rule '" + edsms.getName() + 
      "': already exists");
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
    debug.error("While parsing specification:\n" + error);
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
        EDStateManager.debug.fatal("EDStateManager init failed", e);
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
