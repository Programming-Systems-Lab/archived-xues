package psl.xues.util;

import siena.HierarchicalDispatcher;
import java.io.IOException;
import org.apache.log4j.Logger;

/**
 * Various Siena utilities.
 *
 * @author Janak J Parekh
 * @version $Revision$
 */
public class SienaUtils {
  /** Logger. */
  static Logger debug = Logger.getLogger(SienaUtils.class.getName());
  
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
    
    // First off: good luck trying to understand this...
    
    // First try the 1.4 version
    try {
      // Must first run a simple test... nested classloading exceptions cause
      // the JVM to die without resumed control flow. (Is that buggy behavior?)
      Class test = Class.forName("siena.comm.PacketReceiver");
      
      // If we get here, we're using a Siena 1.4-style packaging.
      debug.debug("Using Siena 1.4...");
      success = ((java.lang.Boolean)Class.
      forName("psl.xues.util.Siena14Utils").
      getMethod("setTCPPacketReceiver",new Class[]{HierarchicalDispatcher.class,
      Integer.class}).invoke(null, new Object[] { hd, new Integer(port) })).
      booleanValue();
    } catch(Exception e) {
      // We're using the older-style Siena packaging
      debug.debug("Using Siena 1.3...");
      try {
        success = ((java.lang.Boolean)Class.
        forName("psl.xues.util.Siena13Utils").
        getMethod("setTCPPacketReceiver",new Class[]{HierarchicalDispatcher.class,
        Integer.class}).invoke(null, new Object[] { hd, new Integer(port) })).
        booleanValue();
      } catch(Exception e2) {
        return false;
      }
      return success;
    }
    return success;
  }
  
  /**
   * Tester method.
   */
  public static void main(String[] args) {
    HierarchicalDispatcher hd = new HierarchicalDispatcher();
    setTCPPacketReceiver(hd, 0);
    System.err.println("done");
  }
}