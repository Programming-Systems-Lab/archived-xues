package psl.xues.ep.output;

/**
 * Socket output for EP.
 *
 * @author Janak J Parekh
 * @version $Revision$
 */
public class SocketOutput extends EPOutput {
  /**
   * CTOR.
   */
  public SocketOutput(EPOutputInterface epo, Element el) {
    super(epo,el);
    
  }
  
  /**
   * Get the plugin "type" as String.
   */
  public String getType() {
    return "SocketOutput";
  }
  
  /**
   * Output an event via the socket connection.
   *
   * @param epe The EPEvent
   * @return A boolean indicating success (if you don't know, return true).
   */
  public boolean handleEvent(EPEvent epe) {
    return false;
  }
  
  
}