package psl.xues.ed;

import siena.Notification;

/** 
 * Constants in ED.
 * <p>
 * Copyright (c) 2000-2002: The Trustees of Columbia University and the
 * City of New York.  All Rights Reserved.
 *
 * @author Enrico Buonnano, parts by Janak J Parekh
 * @version $Revision$
 */
public abstract class EDConst {
  // Criteria for instantiating state machines.
  /** only one machine is instantiated. */
  public static final int ONE_ONLY = 0;
  /** a new machine is instantiated as one dyes. */
  public static final int ONE_AT_A_TIME = 1;
  /** a new machine is instantiated as one starts.
   *  this is the default value. */
  public static final int MULTIPLE = 2;
  
  /**
   * Reap fudge factor.  IMPORTANT to take care of non-realtime event
   * buses (can anyone say Siena?)  XXX - should be a better way to do this.
   */
  public static final int REAP_FUDGE = 3000;
  
  /** Frequency for releasing events internally - in millisec. */
  public static int EVENT_PROCESSING = 500;
  
  /** Frequency for releasing events internally - in millisec. */
  public static int REAP_INTERVAL = 1000;
  
  /** Attribute to filter against */
  public static final String INPUT_ATTR = "Type";
  /** Value to filter against with above attribute */
  public static final String INPUT_VAL = "EDInput";
  
  /** Identifier for timestamp in notifications. */
  public static final String TIME_ATT_NAME = "Timestamp";
  
  /** Names for value types. */
  public static final String TYPE_NAMES[] = { "null", "string", "long",
  "int", "double", "bool" };
  
  /** String representation of comparison operators */
  public static final String OPERATOR_STRINGS[] = { "null", "=", "<", ">", 
  ">=", "<=", ">*", "*<", "any", "!=", "*" };
}
