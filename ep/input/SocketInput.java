package psl.xues.ep.input;

/**
 * Socket event input mechanism.  Allows EP to be a server which receives
 * socket input.  We currently allocate one thread per socket.
 * <p>
 * Input types supported:<ol>
 * <li>Serialized Siena notifications (type <b>Siena</b>)
 * </ol>
 *
 * <!--
 * TODO:
 * - Support simple XML Siena representations instead of serialized Java
 * -->
 *
 * @author Janak J Parekh
 * @version $Revision$
 */
public class SocketInput extends EPInput {
  /** Server socket port */
  private short port = -1;
  
  /**
   * CTOR.
   */
  public SocketInput(EPInputInterface ep, Element el) {
    super(ep,el);
    // Get the basic ServerSocket parameters
    this.port = 