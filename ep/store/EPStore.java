package psl.xues.ep.store;

import psl.xues.ep.event.EPEvent;

/**
 * Store interface.  Support these methods to allow EP to use this mechanism
 * as an event store.
 *
 * @author Janak J Parekh <janak@cs.columbia.edu>
 * @version $Revision$
 */
public interface EPStore {
  /**
   * Store the supplied event.
   *
   * @param e The event to be stored.
   * @return An object reference indicating success, or null.
   */
  public Object storeEvent(EPEvent e);

  /**
   * Request an individual event given its (opaque) reference.
   *
   * @param ref The event reference.
   * @return The event in EPEvent form, or null if it doesn't exist.
   */
  public EPEvent requestEvent(Object ref);
  
  /**
   * Request event(s).  Return a set of references to this event.  These
   * references are treated opaquely; they may be handed back via the 
   * individual event request mechanism.  Return an empty Object[] if there
   * are no matches (do NOT return null unless there is an error).
   *
   * @param t1 The lower timebound (inclusive).
   * @param t2 The upper timebound (inclusive).
   * @return An array of (possibly opaque) object references, null if error.
   */
  public Object[] requestEvents(long t1, long t2);
}