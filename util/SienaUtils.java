package psl.xues.util;

import siena.TCPPacketReceiver;
import java.io.IOException;

/**
 * Various Siena utilities.
 *
 * @author Janak J Parekh
 * @version $Revision$
 */
public class SienaUtils {
  /**
   * Make class non-instantiable
   */
  protected SienaUtils() { ; }
  
  /**
   * Get a packet receiver.  We do this here so that we don't need 1.4
   * dependencies scattered throughout the code.
   */
  public static TCPPacketReceiver newTCPPacketReceiver(int port) 
  throws IOException {
    return new TCPPacketReceiver(port);
  }
}