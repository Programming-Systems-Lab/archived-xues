package psl.xues.ed;

import java.util.*;
import siena.*;

import org.apache.log4j.Logger;
import psl.xues.util.EDConst;

/**
 * Individual Event Distiller state machine state.  A state is matched
 * against a String attribute-String value pair.  A upper bound
 * timestamp may be applied.
 * <p>
 * Copyright (c) 2000-2002: The Trustees of Columbia University and the
 * City of New York.  All Rights Reserved.
 *
 * <!--
 * TODO:
 * - the bear method:
 *   set parent, subscribe, wait...
 * - the kill/reap method
 *   alive to false
 * - the notify method
 *   if succeed, send notifs, and bear children
 *   etc...
 * -->
 *
 * @author Janak J Parekh, parts by Enrico Buonnano
 * @version $Revision$
 */
public class EDState implements EDNotifiable {
  /** Logger.  Set when we have our own ID */
  private Logger debug = null;
  
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
  
  /**
   * Time limit. The latest time where we can be matched.
   * This is temporary. Normally we should be able to put the limit
   * in the dispatiching mechanism, somehow (see createFilter method)
   */
  private long tl = Long.MAX_VALUE;
  
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
   * List of subscribers used to subscribe. We need more than one subscriber
   * for states that have more than one parent: if several parents bear us,
   * we need to subscribe with different wildcard values - since different
   * parents may have different values for wildcards. "parents" and
   * "subscribers" are therefore parallel vectors.
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
  
  /** How many times this event will be matched, before it passes. */
  private int count = 1;
  
  /** Whether this event has started. Only used for events that loop:
   *  i.e. that can occur an indefinite number of times. */
  private boolean hasStarted = false;
  
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
  public EDState(String name, int tb, String childrenList, String actionsList,
  String failActionsList) {
    this.name = name;
    this.tb = tb;
    this.ts = -1;                                // Unvalidated state
    this.constraints = new Hashtable();
    
    children = listToArray(childrenList);
    actions = listToArray(actionsList);
    fail_actions = listToArray(failActionsList);
    
    // Build a generic debugger
    debug = Logger.getLogger(EDState.class.getName());
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
    this.myID = sm.myID + "." + name;
    this.bus = sm.getSpecification().getManager().getEventDistiller().getBus();
    
    this.children = e.children;
    this.actions = e.actions;
    this.fail_actions = e.fail_actions;
    
    // Build the debugger
    debug = Logger.getLogger(EDState.class.getName() + "." + myID);
  }
  
  /**
   * Adds a constraint.
   *
   * @param s the identifier name of the attribute value
   * @param attributeconstraint the constraint to put on the value
   * @return false if the constraint could not be added due to a
   *         naming conflict
   */
  boolean addConstraint(String s, AttributeConstraint attributeconstraint) {
    if (constraints.get(s) != null) return false;
    constraints.put(s, attributeconstraint);
    return true;
  }
  
  /**
   * Adds a child name.
   * @param childName the name to add
   */
  void addChildName(String childName){
    String[] nc = new String[children.length + 1];
    int i;
    for (i = 0; i < children.length; i++)
      nc[i] = children[i];
    nc[nc.length - 1] = childName;
    children = nc;
  }
  
  /**
   * Adds an action name.
   * @param actionName the name to add
   */
  void addActionName(String actionName){
    String[] nc = new String[actions.length + 1];
    int i;
    for (i = 0; i < actions.length; i++)
      nc[i] = actions[i];
    nc[nc.length - 1] = actionName;
    actions = nc;
  }
  
  /**
   * Adds a fail_action name.
   * @param actionName the name to add
   */
  void addFailActionName(String actionName){
    String[] nc = new String[fail_actions.length + 1];
    int i;
    for (i = 0; i < fail_actions.length; i++)
      nc[i] = fail_actions[i];
    nc[nc.length - 1] = actionName;
    fail_actions = nc;
  }
  
  /**
   * Gives life to this state. Assigns the owner state machine,
   * the parent state, and state machine, and the siena to subscribe to.
   * @param parent the parent node, against whom we validate our timestamp,
   *               or null, if this is an initial state
   */
  void bear(EDState parent) {
    debug.debug("Being born");
    if (!alive) {
      // initialize vectors
      parents = new Vector();
      subscribers = new Vector();
      
      /* make sure there is a hashtable defined at any time, for handling
       * wildcards in failure notifications. Also see note in fail()
       */
      if(parent == null) wildHash = new Hashtable();
      else wildHash = parent.getWildHash();
      
      debug.debug("Being born for the first time");
    }
    
    // timebound for matching this state, given this parent
    long l;
    if(tb == -1) l = Long.MAX_VALUE; // forever
    else l = sm.getSpecification().getManager().getEventDistiller().getTime()
    + tb;
    
    //errorManager.print("EDState: " + myID + " checking parent...",
    //EDErrorManager.STATE);
    
    // Have we already been born by this parent? -- this can be the case in
    // counter or loop states
    int i = parents.indexOf(parent);
    
    //errorManager.println("done", EDErrorManager.STATE);
    if(i != -1) { // parent is already in the list, just reset timebound
      debug.debug("Extending subscription timebound");
      if(tb != -1) {
        // this is how it should be
        //((EDSubscriber)subscribers.get(i)).resetTimebound(l);
        // XXX - hack -- extend timelimit here
        tl = l;
      }
      // put most recent parent at the end of the list -- see reap()
      parents.add(parents.remove(i));
      subscribers.add(subscribers.remove(i));
    }
    else { // new parent
      Hashtable wc; // wildcard values for this subscription
      if(parents.size() == 0) wc = wildHash;
      else wc = parent.getWildHash();
      
      debug.debug("Subscribing...");
      
      EDSubscriber edsubscriber = new EDSubscriber(createFilter(wc, l), this,
      sm);
      bus.subscribe(edsubscriber);
      
      debug.debug("Subscription complete");
      
      // put most recent parent at the end of the list
      parents.add(parent);
      subscribers.add(edsubscriber);
    }
    
    debug.debug("Successfully born");
    this.alive = true;
  }
  
  /** Kills this state: unsubscribe and set alive to false. */
  public void kill() {
    debug.debug("State being killed, unsubscribing");
    this.alive = false;
    bus.unsubscribe(this);
  }
  
  /**
   * Handles callbacks from the dispatcher. We turn off reaping while this 
   * happens, in case any "succeeds" occur during our notification.
   * Note that ANY notification we receive satisfies the state, since we build 
   * a precise filter in createFilter(). All we need to do is register the 
   * timestamp and the wildcard values.
   */
  public synchronized boolean notify(Notification n) {
    debug.debug("Received " + n);
    sm.disableReap();

    // check time - this is a hack, see createFilter()
    if (n.getAttribute(EDConst.TIME_ATT_NAME).longValue() > tl){
      // notif is too late
      debug.warn("Rejecting late notification");
      kill();
      return false;
    }
    
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
    
    if(absorb)
      debug.debug("Absorbing event");
    
    sm.enableReap();
    return absorb;
  }
  
  /**
   * Called when the state is matched. The state
   * now lives the climax of its brief existence.
   */
  private void succeed() {
    debug.debug("Starting succeed()");
    
    // the machine has started
    sm.setStarted();
    // this state may be breaking the loop of its parent
    for (int i = 0; i < parents.size(); i++) {
      EDState parent = (EDState)parents.get(i);
      if (parent != null && parent != this && parent.getCount() == -1)
        parent.kill();
    }
    
    if (count > 1) { // counter feature
      if (!hasStarted) {
        /*synchronized(parents)*/ {
          parents.removeAllElements();
          parents.add(this);
        }
        hasStarted = true;
      }
      count--;
      
      debug.debug("Decreased count to " + count);
    }
    else if (count < 0) { // loop feature
      debug.debug("Loop state bearing itself");
      /* bear children, and myself */
      bear(this);
      debug.debug("Loop state bearing children");
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
        debug.debug("Sending notification " + actions[i]);
        sm.sendAction(actions[i], wildHash);
      }
      // 6. commit suicide
      kill();
    }
    
    debug.debug("Succeed finished");
    
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
    
    debug.debug("Reap - checking live state " + myID);
    
    /* can we still be matched? check the last (most recent) parent.
     * NOTE: this assumes that events are processed sequentially,
     * else we would need to check all the parents */
    if(validateTimebound
    ((EDState)parents.lastElement(), currentTime - EDConst.REAP_FUDGE)) {
      debug.debug("Not timed out, not reaping");
      return false;
    }
    
    // if we're still here, timebound has failed
    debug.debug("currentTime is " + currentTime +
    "\nmost recent parent time is " +
    getTimestampForState((EDState)parents.lastElement()));
    debug.debug("Timed out");
    kill();
    return true;
  }
  
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
      s = s + "\t<attribute name =\"" + attName + "\" value=\"" + ac.value.stringValue() +
      "\" op=\"" + Op.operators[ac.op] + "\" type=\"" + EDConst.TYPE_NAMES[ac.value.getType()] + "\"/>\n";
    }
    s += "</state>\n";
    return s;
  }
  
  /**
   * Build a Siena filter to subscribe this state.
   * If expected value begins with an asterisk "*" it will be considered a
   * wildcard and will bind to anything.
   * @param wc the table containing the wildcards values to insert in the
   * subscription
   * @param timeLimit the time within which the notification must be matched
   * @return the siena.Filter object to use to subscribe this state
   */
  private Filter createFilter(Hashtable wc, long timeLimit){
    Filter f = new Filter();
    
    // go through the constraints
    for(Enumeration enumeration = constraints.keys();
    enumeration.hasMoreElements();) {
      String attName = (String)enumeration.nextElement();
      AttributeConstraint ac = (AttributeConstraint)constraints.get(attName);
      AttributeValue attributevalue = ac.value;
      
      // "**.." is escape char for "*..", so subscribe removing the escape char
      if(attributevalue.stringValue().startsWith("**"))
        f.addConstraint(attName, new AttributeConstraint(ac.op,
        attributevalue.stringValue().substring(1)));
      // wildcard: do we already have a value for this?
      else if(attributevalue.stringValue().startsWith("*")){
        if(wc.get(attName) == null) // no value registered
          f.addConstraint(attName, new AttributeConstraint(Op.ANY, ""));
        else
          f.addConstraint(attName, new AttributeConstraint(ac.op,
          (AttributeValue)wc.get(attName)));
      }
      // normal case, add simple constraint
      else f.addConstraint(attName, ac);
    }
    // set timebound
        /* this does not work, for some reason that I don't really understand... any ideas?
           for now, we validate timebound in the notify method */
    //f.addConstraint(EDConst.TIME_ATT_NAME, new AttributeConstraint(Op.LT, timeLimit));
    // instead, temporarily, we do this
    tl = timeLimit;
    return f;
  }
  
  /************************************
   * auxiliary static methods
   *************************************/
  
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
  
  /**********************************
   * standars methods
   **********************************/
  
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
  
  /** @return the timebound of this EDState */
  long getTimebound(){ return tb; }
  
  /** @param tb the timebound for this EDState */
  void setTimebound(int tb){ this.tb = tb; }
  
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
