
package psl.xues.ed;

import java.io.*;
import java.util.*;
import javax.swing.tree.*;
import siena.*;

/**
 * Class to specify a machine template.
 * <p>
 * Copyright (c) 2000-2002: The Trustees of Columbia University and the
 * City of New York.  All Rights Reserved.
 *
 * @author Janak J Parekh
 * @version $Revision$
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
   * public static EDStateMachineSpecification buildDemoSpec(Siena siena,
   * EDStateManager manager)
   * {
   * EDStateMachineSpecification managers =
   * new EDStateMachineSpecification("demoTest", "foo",siena,manager);
   * // I had to give it some silly parameters, so that it constructs itself - eb659
   * EDState e = new EDState("zzz", -1, "", "", "");
   * e.add("temperature","60");
   * managers.stateArray.addElement(e);
   *
   * //    managers.subscribe();
   * return managers;
   * }*/
  
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
        // XXX - should this be outside?  If we do so, it might be very, very 
        // bad - maybe the new EDStateMachine will
        // receive something that needs a reference to the stateMachines index
        // of itself... but I don't like holding onto the lock for so long!
        EDStateMachine edsm = new EDStateMachine(this);
        stateMachines.add(edsm);
      }
    }
  }
  
  /**********************************
   * standars methods
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

