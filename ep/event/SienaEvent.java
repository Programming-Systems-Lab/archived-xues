package psl.xues.ep.event;

import siena.Notification;

import org.apache.log4j.Category;

/**
 * Simple Siena event representation.  Embeds a Siena Notification for speed.
 *
 * TODO:
 * - Support conversion to basic formats.  (Where should this be?)
 *
 * @author Janak J Parekh <janak@cs.columbia.edu>
 * @version $Revision$
 */
public class SienaEvent extends EPEvent {
  private final String format = "SienaEvent";
  private Notification n = null;
  private Category debug = Category.getInstance(SienaEvent.class.getName());
  
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
  
}