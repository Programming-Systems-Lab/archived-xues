package psl.xues.ep.output;

import org.w3c.dom.Element;

import psl.xues.ep.event.EPEvent;
import psl.xues.ep.event.SienaEvent;
import psl.xues.util.SienaUtils;

import siena.HierarchicalDispatcher;
import siena.Notification;
import siena.SienaException;

/**
 * Siena outputter for EP.
 * <p>
 * Copyright (c) 2002: The Trustees of Columbia University in the
 * City of New York.  All Rights Reserved.
 *
 * <!--
 * TODO:
 * - Implement a separate publisher thread (is this really necessary?)
 * -->
 *
 * @author Janak J Parekh <janak@cs.columbia.edu>
 * @version $Revision$
 */
public class SienaOutput extends EPOutput {
  private String sienaHost = null;
  /** Port for setting the HD's PacketReceiver to, if the user specifies
   *  a custom one. */
  private String sienaPort = null;
  private HierarchicalDispatcher hd = null;
  
  /**
   * CTOR.
   *
   * @exception InstantiationException is thrown if we cannot finish
   * initialization -- this is likely due to networking or parameter problems.
   */
  public SienaOutput(EPOutputInterface ep, Element el) 
  throws InstantiationException {
    super(ep,el); // Set up debugging, etc.
    
    // Now parse the element and see if we can get all of the needed
    // information.
    sienaHost = el.getAttribute("SienaHost");
    sienaPort = el.getAttribute("SienaReceivePort");
    if(sienaHost == null || sienaHost.length() == 0) {
      sienaHost = null; // In case of the length-0 scenario
      if(sienaPort == null || sienaPort.length() == 0) {
        sienaPort = null; // In case of the length-0 scenario
        debug.warn("Siena host not specified, assuming local operation");
      } else {
        debug.info("Siena host not specified, will run as Siena master");
      }
      
    }
    
    // Now actually try and connect
    hd = new HierarchicalDispatcher();
    try {
      if(sienaPort != null && sienaPort.length() > 0) {
        // Specify a custom receiver
        hd.setReceiver(SienaUtils.newTCPPacketReceiver(Integer.parseInt(sienaPort)));
      }
      if(sienaHost != null && sienaHost.length() > 0) hd.setMaster(sienaHost);
    } catch(Exception ex) {
      debug.error("Cannot establish Siena node", ex);
      hd = null;
      throw new InstantiationException("Cannot establish Siena node");
    }
  }
  
  /**
   * Handle an event handed to you.  All EPOutputters must implement this.
   *
   * @param epe The EPEvent
   * @return A boolean indicating success (if you don't know, return true).
   */
  public boolean handleEvent(EPEvent epe) {
    debug.debug("Received event, about to publish");
    if(epe.getFormat().equals("SienaEvent")) {
      // Just get the embedded Siena event and publish it
      return publishEvent(((SienaEvent)epe).getSienaEvent());
    } else {
      // Convert to Siena form, THEN publish
      EPEvent newepe = epe.convertEvent("SienaFormat");
      if(newepe == null) { // No can do
        debug.warn("Could not publish event: no conversion from \"" + 
        epe.getFormat() + "\" to SienaFormat");
        return false;
      }
      // Successful conversion, publish it
      return publishEvent(((SienaEvent)newepe).getSienaEvent());
    }
  }
 
  /**
   * Actual publication operation.
   *
   * @param n The notification to publish to our Siena.
   */
  private boolean publishEvent(Notification n) {
    try {
      hd.publish(n);
    } catch(SienaException e) {
      debug.warn("Could not publish event", e);
      return false;
    }
    // Success
    return true;
  }
    
  /**
   * Handle shutdown - first let super do its cleanup, then kill the hd
   */
  public void shutdown() {
    super.shutdown();
    hd.shutdown(); // Unsubscribe from everything
    hd = null;
  }
}