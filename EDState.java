
package psl.xues;

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
 * Revision 1.1  2001-01-22 02:11:54  jjp32
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
  public long ts;
   

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
   * Validate a state.  If this returns true, then it means the state
   * was successfully matched within the appropriate timebounds.
   *
   * BUG WARNING: If timestamp is mapped to a non-long, we will crash.
   */
  public boolean validate(Notification n, EDState prev) {
    AttributeValue a = n.getAttribute(attr);
    AttributeValue timestamp = n.getAttribute("timestamp");

    if(a != null && a.stringValue() != null &&
       a.stringValue().equals(val)) {
      // Pull out the timestamp
      if(timestamp != null) { // Use validate
	return validateTimebound(prev, timestamp.longValue());
      }
      
      // No timestamp.  If timebound is -1, we're OK, otherwise fail
      if(tb == -1) return true;
      else return false;
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
   * Build a Siena filter for the state machine.  Primarily for
   * EDStateMachine.
   *
   * @return A Siena filter.
   */
  Filter buildSienaFilter() {
    Filter f = new Filter();
    // We only want events from metaparser that have the state that
    // maches us
    f.addConstraint("type", "ParsedEvent");
    f.addConstraint(attr, val);
    return f;
  }  
}
