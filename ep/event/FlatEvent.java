package psl.xues.ep.event;

/**
 * Flat event format.  Compatible with Siena and others.
 *
 * TODO:
 * - Implement this!
 *
 * @author Janak J Parekh <janak@cs.columbia.edu>
 * @version $Revision$
 */
public class FlatEvent extends EPEvent {
  /**
   * Create an empty FlatEvent.
   */
  public FlatEvent(String source) { super(source); }
  
  /** 
   * Get the format (type) of this event.
   *
   * @return A string indicating the type.
   */
  public String getFormat() {
    return "FlatEvent";
  }
}
