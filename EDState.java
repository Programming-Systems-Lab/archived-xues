
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
 * Revision 1.9  2001-04-08 22:10:09  jjp32
 *
 * Restored previous revisions on main branch after Enrico's accidental
 * commit (see xues-eb659 branch for those)
 *
 * Revision 1.7  2001/03/21 18:14:02  jjp32
 *
 * Fixed comment
 *
 * Revision 1.6  2001/01/30 06:26:18  jjp32
 *
 * Lots and lots of updates.  EventDistiller is now of demo-quality.
 *
 * Revision 1.5  2001/01/30 02:39:36  jjp32
 *
 * Added loopback functionality so hopefully internal siena gets the msgs
 * back
 *
 * Revision 1.4  2001/01/29 04:58:55  jjp32
 *
 * Each rule can now have multiple attr/value pairs.
 *
 * Revision 1.3  2001/01/29 04:18:42  jjp32
 *
 * Lots of updates.  Doesn't compile yet, hopefully it will by the time I'm home :)
 *
 * Revision 1.2  2001/01/28 22:58:58  jjp32
 *
 * Wildcard support has been added
 *
 * Revision 1.1  2001/01/22 02:11:54  jjp32
 *
 * First full Siena-aware build of XUES!
 * 
 */
public class EDState {
  /* Hash of attribute/value pairs relevant to this state */
  private Hashtable attributes;
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
   * @param tb Elapsed time bound of this state (e.g. how long to wait
   *           from the previous state).  -1 implies no time bound.
   *           Starting states ignore this.
   */
  public EDState(int tb) {
    this.tb = tb;
    this.ts = -1;  // Unvalidated state.
    this.attributes = new Hashtable();
  }

  /**
   * Clone CTOR.
   */
  public EDState(EDState e) {
    /* Ouch, but I have to do this */
    this.attributes = (Hashtable)e.attributes.clone();
    this.tb = e.tb;
    this.ts = e.ts;
  }

  /**
   * Clone-and-assign-state-machine CTOR.  Useful if this EDState is a template
   * for many state machines.
   */
  public EDState(EDState e, EDStateMachine sm) {
    this.attributes = (Hashtable)e.attributes.clone();
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
   * Add an attribute/value pair.
   *
   * XXX - We should probably check to prevent overwriting, but heck.
   */
  public void add(String attr, AttributeValue val) {
    attributes.put(attr,val);
  }
  
  /**
   * Add an attribute/value pair (strings).  This is accomplished by
   * wrapping an AttributeValue val.
   */
  public void add(String attr, String val) {
    add(attr, new AttributeValue(val));
  }

  /**
   * Validate a state.  If this returns true, then it means the state
   * was successfully matched within the appropriate timebounds.
   *
   * BUG WARNING: If timestamp is mapped to a non-long, we will crash.
   */
  public boolean validate(Notification n, EDState prev) {
    // Step 1. Perform timestamp validation.  If timestamp validation
    // fails, then we don't need to go further.
    AttributeValue timestamp = n.getAttribute("timestamp");
    if(validateTimebound(prev,timestamp/*.longValue()*/) == false)
      return false;

    // Step 2. Now try and compare the attributes in the state's
    // notification.  This notification may have other attributes, but
    // we ignore them.
    Enumeration keys = attributes.keys();
    Enumeration objs = attributes.elements();
    while(keys.hasMoreElements()) {
      String attr = (String)keys.nextElement();
      AttributeValue val = (AttributeValue)objs.nextElement();
      if(validate(attr, val, n.getAttribute(attr)) == false) {
	return false;
      } // else continue
    }

    // They all passed, return true
    return true;
  }

  /**
   * Internal validate function - just check one attribute-value pair
   *
   * GOOD LUCK if you can understand this.  Read through a few times,
   * hopefully it'll make sense then :-)
   */
  private boolean validate(String attr, AttributeValue internalVal,
			   AttributeValue externalVal) {
    // Simple bounds checking
    if(attr == null || internalVal == null || 
       internalVal.getType() == AttributeValue.NULL) {
      System.err.println("FATAL: Internal representation error in "+
			 "EDState");
      return false;
    }

    // Attribute exists externally?
    else if(externalVal == null || 
	    externalVal.getType() != internalVal.getType()) {
      // No match
      return false;
    }

    // Debug
    if(EventDistiller.DEBUG) {
      System.err.println("EDState: comparing attribute \"" + attr +
			 "\", internalVal = " + internalVal + ", " +
			 "externalVal = " + externalVal);
    }

    // Wildcard binding?
    if(internalVal.getType() == AttributeValue.STRING &&
	    internalVal.stringValue().startsWith("*")) {
      // Is this one previously bound?
      String bindName = internalVal.stringValue().substring(1);
      if(bindName.length() == 0) { // Simple wildcard
	if(EventDistiller.DEBUG)
	  System.err.println("EDState: Simple wildcard, match");
	return true;
      } else { // Binding
	if(sm == null || sm.wildHash == null) { // BAD
	  System.err.println("EDState: ERROR - No State Machine hash "+
			     "assigned, wildcard binding requested");
	  return false;
	}
	// Now check the bind
	if(sm.wildHash.get(bindName) != null) {
	  if(attrEqual((AttributeValue)sm.wildHash.get(bindName),externalVal)){
	    // YES!
	    if(EventDistiller.DEBUG)
	      System.err.println("EDState: wildcard already bound to \"" + 
				 sm.wildHash.get(bindName) + "\" and match");
	    return true;
	  }
	  else {
	    if(EventDistiller.DEBUG)
	      System.err.println("EDState: wildcard already bound to \"" + 
				 sm.wildHash.get(bindName) + "\" but nomatch");
	    return false; // Complex wildcard doesn't match
	  }
	} else { // Binding requested, NOT YET BOUND, bind and return true
	  sm.wildHash.put(bindName, externalVal);
	  if(EventDistiller.DEBUG)
	    System.err.println("EDState: wildcard BOUND to " + externalVal);
	  return true;
	}
      }
    }

    // No wildcard binding, SIMPLE match
    if(EventDistiller.DEBUG)
      System.err.println("EDState: performing SIMPLE match on " + externalVal + "," + internalVal);
    
    if(attrEqual(internalVal,externalVal)) {
      if(EventDistiller.DEBUG) {	
	System.err.println("EDState: not wildcard, compare succeeded");
      }
      return true;
    }

    // Not our event
    if(EventDistiller.DEBUG)
      System.err.println("EDState: not wildcard, compare failed");
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
    f.addConstraint("Type", "EDInput");
    // Now enumerate through the actual attr, val pairs
    Enumeration keys = attributes.keys();
    Enumeration objs = attributes.elements();
    while(keys.hasMoreElements()) {
      String attr = (String)keys.nextElement();
      AttributeValue val = (AttributeValue)objs.nextElement();
      if(val.getType() == AttributeValue.STRING &&
	 val.stringValue().startsWith("*")) {
	// XXX - will "" be a problem here?
	f.addConstraint(attr, new AttributeConstraint(Op.ANY,""));
      } else {
	f.addConstraint(attr, new AttributeConstraint(val));
      }
    }
    return f;
  }  

  /**
   * AttributeValue isEqualTo check that WORKS.
   */
  public static boolean attrEqual(AttributeValue e1, AttributeValue e2) {
    if(e1.getType() != e2.getType()) return false;
    
    switch(e1.getType()) {
    case AttributeValue.BOOL:
      if(e1.booleanValue() == e2.booleanValue()) return true;
      else return false;
    case AttributeValue.DOUBLE:
      if(e1.doubleValue() == e2.doubleValue()) return true;
      else return false;
    case AttributeValue.LONG:
      if(e1.longValue() == e2.longValue()) return true;
      else return false;
    case AttributeValue.STRING:
      if(e1.stringValue().equals(e2.stringValue())) return true;
      else return false;
    default:
      System.err.println("EDState: Sorry, can't do attrEqual on this type!!");
      return false;
    }
  }
}
