package psl.xues.ep.event;

import siena.Notification;

/**
 * Simple Siena event representation.  Embeds a Siena Notification for speed.
 * <p>
 * Copyright (c) 2002: The Trustees of Columbia University in the
 * City of New York.  All Rights Reserved.
 *
 * <!--
 * TODO:
 * - Support conversion to XML.
 * -->
 *
 * @author Janak J Parekh <janak@cs.columbia.edu>
 * @version $Revision$
 */
public class SienaEvent extends EPEvent {
  /** Format of this event */
  private final String format = "SienaEvent";
  /** Associated data */
  private Notification n = null;
  
  /**
   * Base CTOR.
   *
   * @param source The generator ("source") of these events.
   */
  public SienaEvent(String source) { this(source, null); }
  
  /**
   * CTOR given an existing Siena notification.
   *
   * @param source The generator ("source") of these events.
   * @param n The Siena notification to use.
   */
  public SienaEvent(String source, Notification n) {
    super(source);
    this.n = n;
  }
  
  /**
   * CTOR given an existing Siena notification and a timestamp.
   *
   * @param source The generator ("source") of these events.
   * @param n The Siena notification to use.
   * @param t The timestamp.
   */
  public SienaEvent(String source, Notification n, long t) {
    super(source, t);
    this.n = n;
  }
  
  /**
   * Get the embedded Siena notification.  This is NOT a copy, but rather
   * the orignal.  Modify with care!
   *
   * @return The embedded Siena notification
   */
  public Notification getSienaEvent() {
    return n;
  }
  
  /**
   * Convert this Siena event to a (new) FlatEvent.  NOTE that this may be
   * slow, as each attribute must be enumerated over and converted manually.
   *
   * @return The new FlatEvent
   */
  public FlatEvent toFlatEvent() {
    // Not implemented right now
    debug.warn("toFlatEvent not yet implemented");
    return null;
  }
  
  /**
   * Get the format (type) of this event.
   *
   * @return A string indicating the type.
   */
  public String getFormat() {
    return "SienaEvent";
  }
  
  /**
   * String representation of SienaEvent
   *
   * @return The string representation
   */
  public String toString() {
    return "psl.xues.ep.event.SienaEvent - source \"" + source +
    "\", timestamp \"" + timestamp + "\", notification " + n;
  }
  
  /**
   * Convert this DOMEvent to an event of a different format.
   *
   * @param newFormat The new requested format.
   * @return The new event, or null.
   */
  public EPEvent convertEvent(String newFormat) {
    // To string format: just use Notification.toString().
    if(newFormat.equalsIgnoreCase("StringEvent")) {
      return new StringEvent(getSource(), n.toString());
    } else { // (Currently) unsupported
      debug.warn("DOMEvent can't be converted to \"" + newFormat + "\"");
      return null;
    }
  }
}