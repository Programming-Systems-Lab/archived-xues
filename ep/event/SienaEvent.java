package psl.xues.ep.event;

import siena.Notification;

/**
 * Simple Siena event representation.  Embeds a Siena Notification for speed.
 *
 * @author Janak J Parekh <janak@cs.columbia.edu>
 * @version $Revision$
 */
public class SienaEvent extends EPEvent {
  private final String format = "SienaEvent";
  private Notification n = null;
  
  /**
   * Empty CTOR.
   */
  public SienaEvent() {
   
  }

  /**
   * CTOR given an existing Siena notification.
   *
   * @param n The Siena notification to use.
   */
  public SienaEvent(Notification n) {
    this.n = n;
  }

  /**
   * Get the embedded Siena notification.
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
    
  }
}