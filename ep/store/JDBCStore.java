package psl.xues.ep.store;

import psl.xues.ep.event.EPEvent;

/**
 * JDBC store mechanism.
 *
 * @author Janak J Parekh
 * @version $Revision$
 */
public class JDBCStore implements EPStore {
  
  /** 
   * Request an individual event given its (opaque) reference.
   *
   * @param ref The event reference.
   * @return The event in EPEvent form, or null if it doesn't exist.
   */
  public EPEvent requestEvent(Object ref) {
    
    return null;
    
  }  
  
  /** 
   * Request event(s).  Return a set of references to this event.  These
   * references are opaque.
   *
   * @param t1 The lower timebound (inclusive).
   * @param t2 The upper timebound (inclusive).
   * @return An array of (possibly opaque) object references, null if error.
   */
  public Object[] requestEvents(long t1, long t2) {
    return null;
  }
  
  /** 
   * Store the supplied event.
   *
   * @param e The event to be stored.
   * @return An object reference indicating success, or null.
   */
  public Object storeEvent(EPEvent e) {
    return null;
  }
}