
package psl.xues;

import java.util.*;
import siena.*;

/**
 * Individual Event Distiller state machine state.  A state is matched
 * against a String attribute-String value pair.  A upper bound
 * timestamp may be applied.
 *
 * Copyright (c) 2001: The Trustees of Columbia University and the
 * City of New York.  All Rights Reserved.
 *
 * @author Janak J Parekh (jjp32@cs.columbia.edu)
 * @version 1.0
 *
 * $Log$
 * Revision 1.2  2001-01-28 22:58:58  jjp32
 *
 * Wildcard support has been added
 *
 * Revision 1.1  2001/01/22 02:11:54  jjp32
 *
 * First full Siena-aware build of XUES!
 * 
 */
public class EDState {
  private String attr;
  private String val;
  /**
   * Relative timebound from previous state.
   */
  private long tb;
  /** 
   * Timestamp this state has fired in.  Created and used during
   * timebound validation.
   */
  private long ts;
  /**
   * The state machine that "ownes" us.
   */
  private EDStateMachine sm = null;

  /**
   * CTOR.
   *
   * @param attr AttributeValue of this state.
   * @param val Value of this state.
   * @param tb Elapsed time bound of this state (e.g. how long to wait
   *           from the previous state).  -1 implies no time bound.
   *           Starting states ignore this.
   */
  public EDState(String attr, String val, int tb) {
    this.attr = attr;
    this.val = val;
    this.tb = tb;
    this.ts = -1;  // Unvalidated state.
  }

  /**
   * Clone CTOR.
   */
  public EDState(EDState e) {
    this.attr = e.attr;
    this.val = e.val;
    this.tb = e.tb;
    this.ts = e.ts;
  }

  /**
   * Clone-and-assign-state-machine CTOR.  Useful if this EDState is a template
   * for many state machines.
   */
  public EDState(EDState e, EDStateMachine sm) {
    this.attr = e.attr;
    this.val = e.val;
    this.tb = e.tb;
    this.ts = e.ts;
    this.sm = sm;
  }

  /**
   * Set our state machine.
   */
  public void assignOwner(EDStateMachine sm) {
    this.sm = sm;
  }

  /**
   * Validate a state.  If this returns true, then it means the state
   * was successfully matched within the appropriate timebounds.
   *
   * BUG WARNING: If timestamp is mapped to a non-long, we will crash.
   *
   * GOOD LUCK if you can understand this.  Read through a few times,
   * hopefully it'll make sense then :-)
   */
  public boolean validate(Notification n, EDState prev) {
    AttributeValue a = n.getAttribute(attr);
    AttributeValue timestamp = n.getAttribute("timestamp");

    // Check to make sure the attribute exists
    if(a == null || a.stringValue() == null) {
      // Not our event
      return false;
    }

    // Wildcard binding?
    if(val.startsWith("*")) {
      // Is this one previously bound?
      String bindName = val.substring(1);
      if(bindName.length() == 0) { // Simple wildcard
	return validateTimebound(prev, timestamp);
      } else { // Binding
	if(sm == null || sm.wildHash == null) { // BAD
	  System.err.println("EDState: ERROR - No State Machine hash "+
			     "assigned, wildcard binding requested");
	  return false;
	}
	// Now check the bind
	if(sm.wildHash.get(bindName) != null) {
	  if(((String)sm.wildHash.get(bindName)).equals(a.stringValue())) {
	    // YES!
	    return validateTimebound(prev, timestamp);
	  }
	  return false; // Complex wildcard doesn't match
	} else { 
	  // Binding requested, NOT YET BOUND, check timestamp and bind
	  if(validateTimebound(prev, timestamp)) {
	    // We have a match, bind and return true
	    sm.wildHash.put(bindName, a.stringValue());
	  } else { 
	    // Not our event (timestamp failed)
	    return false;
	  }
	}
      }
    }

    // No wildcard binding, SIMPLE match
    if(a.stringValue().equals(val)) {
      // Pull out the timestamp
      return validateTimebound(prev, timestamp);
    }

    // Not our event
    return false;
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
   * @param s The other state.
   * @param t The current event's timestamp (UNIX time format).
   * @return A boolean indicating if this state has occurred  
   * 'in time' from the previous state.
   */
  public boolean validateTimebound(EDState s, long t) { 
    this.ts = t;
    if(tb == -1) { // No time bounds
      return true;
    }
    if(s.ts == -1) { // Prev state never validated
      return false;
    }
    if(this.ts - s.ts <= this.tb) return true;
    else return false;
  }

  /**
   * Convenience accessor method to validateTimebound.
   */
  public boolean validateTimebound(EDState s, AttributeValue t) {
    if(t == null) { // No match, do WE have a timebound
      if(tb == -1) { // OK, no timebound specified but we didn't expect one
	return true;
      }
      return false; // No timebound specified, we wanted one.
    }

    // There was a match to timebound, let's compare    
    return validateTimebound(s, t.longValue());
  }

  /**
   * Build a Siena filter for the state machine.  Primarily for
   * EDStateMachine.
   * 
   * Note, if val begins with an asterisk "*" it will be considered a
   * wildcard and will bind to anything.  If there is a suffix after *
   * it will match to that after the first binding (at least, in this
   * state machine).
   * 
   * @return A Siena filter.  */
  Filter buildSienaFilter() {
    Filter f = new Filter();
    // We only want events from metaparser that have the state that
    // maches us
    f.addConstraint("type", "EDInput");
    if(val.startsWith("*")) {
      // XXX - will "" be a problem here?
      f.addConstraint(attr, new AttributeConstraint(Op.ANY,""));
    } else {
      f.addConstraint(attr, val);
    }
    return f;
  }  
}
