
package psl.xues.ed;

import psl.kx.KXNotification;
import psl.xues.util.EDConst;
import siena.*;
import java.util.*;

import org.apache.log4j.Logger;

/**
 * Event Distiller State Machine.
 * <p>
 * Copyright (c) 2000-2002: The Trustees of Columbia University and the
 * City of New York.  All Rights Reserved.
 *
 * <!--
 * TODO:
 * - Not interested in notification of state zero (CTOR)
 * - Multiple actions in case of notification
 * -->
 *
 * @author Janak J Parekh
 * @version $Revision$
 */
public class EDStateMachine implements Comparable {
  /** Logger.  Set when we have our own ID */
  private Logger debug = null;
  
  /** the manager */
  private EDStateManager manager = null;
  
  /** the specification on which this machine is built. */
  private EDStateMachineSpecification specification;
  
  /**
   * Whether this machine has started.
   * When the machine is first instantiated,
   * this value is set to false, and the initial states subscribed.
   * When one of these states is matched, a call is made to make a
   * new instance, and the value set to true. It then remains true.
   */
  private boolean hasStarted = false;
  
  /** an ID number for debugging */
  String myID = null;
  
  /** the states in the event-graph of this stateMachine */
  private Hashtable states = new Hashtable();
  
  /** the notifications we send, when the states call them. */
  private Hashtable actions = new Hashtable();
  
  /** timestamp registered when machine is instantiated. */
  long timestamp;
  
  /** Whether this state machine is currently being reaped. */
  boolean reaping = false;

  /** Whether this state machine can currently be reaped. */
  boolean dontreap = false;
  
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
    
    /* Get an ID for debugging.  This is in the form 'rulename:index' */
    this.myID = specification.getName() + ":" + specification.getNewID();

    // Instantiate debugger.  XXX - can we append myID like this?
    debug = Logger.getLogger(EDStateMachine.class.getName() + "." + myID);

    debug.debug("StateMachine instantiated");
    
    // Copy the actions using the cloning constructor.
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
    Vector initialStates = specification.getInitialStates();
    for (int i = 0; i < initialStates.size(); i++) {
      EDState state = (EDState)states.get(((EDState)initialStates.get(i)).getName());
      state.bear(null);
    }
  }

  /**
   * Disable reaping - needed if something "essential" happens.  Synchronized
   * so that it won't coincide with a reaping.
   */
  synchronized void disableReap() {
    dontreap = true;
  }
  
  /**
   * Reenable reaping
   */
  synchronized void enableReap() {
    dontreap = false;
  }
  
  /**
   * Reap ourselves if necessary. The Grim Reaper (in EventDistiller) will
   * eventually get to us by calling this method.
   *
   * @return whether this machine is 'dead' and can be removed
   */
  synchronized boolean reap() {
    /* Should I not reap? */
    if(dontreap == true) {
      debug.debug("Skipping reaping, I'm doing something");
      return false;
    }
    
    reaping = true;
    debug.debug("Attempting to reap myself");
    
    /* if machine has not started yet, no need to check it
     * may reconsider this, once we will have rules that are made on the fly,
     * with limited validity */
    if(!hasStarted) return false;

    debug.debug("Beginning enumeration of individual states to reap");
    
    /* go through individual states,
     * which will kill themselves if they timed out */
    Vector failedStateNames = new Vector();
    Enumeration elements = states.elements();
    while(elements.hasMoreElements()) {
      EDState e = (EDState)elements.nextElement();
      // remember which states timed out
      if (e.reap()) failedStateNames.add(e.getName());
    }
    
    debug.debug("Enumeration done");
    
    if(containsLiveStates()) return endReap(false);
    
    /* if all states are dead by now: */
    debug.debug("No more states left, about reap myself");
    
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
          debug.error(error);
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
        if (manager.getSpecifications().indexOf(this.specification) <
        manager.getSpecifications().indexOf(other.getSpecification())) return -1;
        else return 1;
      }
    }
  }
}

