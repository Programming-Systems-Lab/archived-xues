package psl.xues.util;

import siena.TCPPacketReceiver;
import java.io.IOException;
import siena.HierarchicalDispatcher;

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
   * Sets up a TCPPacketReceiver for a HierarchicalDispatcher.  Does <i>not</i>
   * return the handle to the packet receiver itself; this way, the parent
   * doesn't need to know about the Siena.comm package, and can therefore
   * support both Siena < 1.4 and >= 1.4.
   *
   * @param hd The HierarchicalDispatcher for which you want a Siena.
   * @param port The port for the TCPPacketReceiver.
   */
  public static boolean setTCPPacketReceiver(HierarchicalDispatcher hd, 
  int port) {
    boolean success = false;
    
    // First try the 1.4 version
    try {
      // Good luck trying to understand this
      success = ((java.lang.Boolean)Class.
      forName("psl.xues.util.Siena14Utils").
      getMethod("setTCPPacketReceiver",new Class[]{HierarchicalDispatcher.class}).
      invoke(null, new Object[] { hd })).booleanValue();
    } catch(Exception e) {
      // Try the 1.3 version
      try {
        success = ((java.lang.Boolean)Class.
        forName("psl.xues.util.Siena13Utils").
        getMethod("setTCPPacketReceiver",new Class[]{HierarchicalDispatcher.class}).
        invoke(null, new Object[] { hd })).booleanValue();
      } catch(Exception e2) {
        return false;
      }
      return success;
    }
    return success;
  }
}