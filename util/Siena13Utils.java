package psl.xues.util;

import siena.HierarchicalDispatcher;
import siena.TCPPacketReceiver;
import org.apache.log4j.Logger;

/**
 * Siena 1.3-specific utilities.  WARNING: DO NOT USE THIS PACKAGE DIRECTLY.
 * ALLOW THE SIENAUTILS CLASS TO ACCESS IT THROUGH REFLECTION.
 * <p>
 * Copyright (c) 2000-2002: The Trustees of Columbia University and the
 * City of New York.  All Rights Reserved.
 *
 * @author Janak J Parekh
 * @version $Revision$
 */
public class Siena13Utils {
  static Logger debug = Logger.getLogger(Siena13Utils.class.getName());
  
  public static boolean setTCPPacketReceiver(HierarchicalDispatcher hd, 
  int port) {
    try {
      hd.setReceiver(new TCPPacketReceiver(port));
    } catch(Exception e) {
      debug.warn("Could not establish packet receiver", e);
      return false;
    }
    return true;
  }
}