package psl.xues.ep.input;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import siena.AttributeConstraint;
import siena.Filter;
import siena.Notifiable;
import siena.HierarchicalDispatcher;
import siena.SienaException;
import siena.Notification;
import siena.Op;

import psl.xues.ep.event.EPEvent;
import psl.xues.ep.event.SienaEvent;
import psl.xues.util.SienaUtils;

/**
 * Siena input filter for EP.
 * <p>
 * Copyright (c) 2002: The Trustees of Columbia University in the
 * City of New York.  All Rights Reserved.
 *
 * <!--
 * TODO:
 * - Support bytearray filters (why does Siena have this, anyway?)
 * -->
 *
 * @author Janak J Parekh <janak@cs.columbia.edu>
 * @version $Revision$
 */
public class SienaInput extends EPInput implements Notifiable {
  private String sienaHost = null;
  /** Port for setting the HD's PacketReceiver to, if the user specifies
   *  a custom one. */
  private String sienaPort = null;
  private HierarchicalDispatcher hd = null;
  /** Are EP control commands supported on this Siena channel?  If so,
   * we make the necessary subscriptions and listen for relevant commands. */
  private boolean control = false;
  
  /**
   * CTOR.  Modeled after the EPInput CTOR.
   */
  public SienaInput(EPInputInterface ep, Element el)
  throws InstantiationException {
    super(ep, el);
    // Now parse the element and see if we can get all of the needed
    // information.
    sienaHost = el.getAttribute("SienaHost");
    sienaPort = el.getAttribute("SienaReceivePort");
    if(sienaHost == null || sienaHost.length() == 0) {
      sienaHost = null; // In case of the length-0 scenario
    }
    if(sienaPort == null || sienaPort.length() == 0) {
      sienaPort = null; // In case of the length-0 scenario
    }
    
    if(sienaHost == null && sienaPort == null) {
      debug.warn("Siena host not specified, assuming local operation");
    } else if(sienaHost == null) {
      debug.info("Siena host not specified, will run as Siena master");
    }
    
    // Now actually try and connect
    hd = new HierarchicalDispatcher();
    try {
      if(sienaPort != null) {
        // Custom port
        SienaUtils.setTCPPacketReceiver(hd, Integer.parseInt(sienaPort));
      }
      if(sienaHost != null) hd.setMaster(sienaHost);
    } catch(Exception ex) {
      debug.error("Cannot establish Siena node", ex);
      hd = null;
      throw new InstantiationException("Cannot establish Siena node");
    }
    
    // Control channel?
    String control = el.getAttribute("Control");
    if(control != null && control.length() > 0) {
      if(Boolean.valueOf(control) != null) {
        this.control = Boolean.valueOf(control).booleanValue();
      } else {
        debug.warn("Invalid value for control attribute, ignoring");
      }
    }
  }
  
  public void run() {
    // Make the subscriptions happen
    debug.debug("Building filters...");
    NodeList children = defnElem.getElementsByTagName("SienaFilter");
    for(int i=0; i < children.getLength(); i++) {
      Element filterTemplate = (Element)children.item(i);
      String filterName = filterTemplate.getAttribute("Name");
      if(filterName == null || filterName.length() == 0) {
        debug.warn("Filter name is null, assigning default filter name");
        filterName = "filter";
      }
      // Start building a new filter
      Filter f = new Filter();
      NodeList constraints = filterTemplate.
      getElementsByTagName("SienaConstraint");
      for(int j=0; j < constraints.getLength(); j++) {
        // Grab each constraint and map them to a Siena AttributeConstraint.
        // We also need to type each of the elements
        Element constraintTemplate = (Element)constraints.item(j);
        String attrName = constraintTemplate.getAttribute("AttributeName");
        String op = constraintTemplate.getAttribute("Op");
        String valueType = constraintTemplate.getAttribute("ValueType");
        String value = constraintTemplate.getAttribute("Value");
        
        // Handling for incomplete filters
        if(attrName == null || attrName.length() == 0 ||
        value == null || value.length() == 0) { // Bad
          debug.error("Can't process attribute in filter \"" +
          filterName + "\", " + "skipping filter entirely");
          f = null;
          break;
        }
        if(valueType == null || valueType.length() == 0) {
          valueType = "String"; // Default format in Siena
        }
        
        // Operator handling
        short opcode = 0; // Actual operation we will use for the subscription
        if(op == null || op.length() == 0) {
          op = "EQ"; // Default operation in Siena as well
        }
        debug.debug("Processing op \"" + op + "\"");
        opcode = Op.op(op);
        if(opcode == 0) { // Siena couldn't parse the op
          debug.error("Can't process op in filter \"" +
          filterName + "\", " + "skipping filter entirely");
          f = null;
          break;
        }
        
        // Add the constraint now, based on filterType and opcode
        try {
          if(valueType.equalsIgnoreCase("String"))
            f.addConstraint(attrName, opcode, value);
          else if(valueType.equalsIgnoreCase("Boolean"))
            f.addConstraint(attrName, opcode,
            Boolean.valueOf(value).booleanValue());
          else if(valueType.equalsIgnoreCase("ByteArray"))
            debug.warn("Bytearrays not yet supported in Siena constraint, "+
            "ignoring constraint");
          else if(valueType.equalsIgnoreCase("Double"))
            f.addConstraint(attrName, opcode, Double.parseDouble(value));
          else if(valueType.equalsIgnoreCase("Integer") ||
          valueType.equalsIgnoreCase("Int"))
            f.addConstraint(attrName, opcode, Integer.parseInt(value));
          else if(valueType.equalsIgnoreCase("Long"))
            f.addConstraint(attrName, opcode, Long.parseLong(value));
          else {
            debug.error("Failed in parsing constraint for filter \""+filterName+
            "\": invalid format for attribute \"" + attrName +
            "\"; dropping filter");
            f = null;
            break;
          }
        } catch(NumberFormatException nfe) {
          debug.error("Failed in parsing constraint for filter \"" +filterName +
          "\": invalid format for attribute \"" + attrName +
          "\"; dropping filter", nfe);
          f = null;
          break;
        } catch(Exception e) {
          debug.error("General exception in parsing constraint for filter \"" +
          filterName + "\", dropping filter", e);
          f = null;
          break;
        }
      } // end constraint loop
      
      if(f != null) { // success, actually apply it
        try {
          debug.info("Subscribing filter \"" + filterName + "\"...");
          hd.subscribe(f, this);
        } catch(SienaException se) {
          debug.error("Cannot subscribe filter \"" + filterName + "\"", se);
          continue; // Do we need to make this explicit?
        }
      }
      
      if(control) {
        // Subscription for EP control channel
        
      }
      
      
      // And we continue with the next filter...
    } // end subscription loop
    
    // Now we don't need to do a whole lot here, since Siena runs in its own
    // thread when it notifies us, so let's just use the default
    // implementation
    super.run();
  }
  
  public void notify(Notification n) {
    debug.debug("Received notification " + n);
    
    // Construct a new SienaEvent and wrap the notification in there
    SienaEvent se = new SienaEvent(getName(), n);
    
    // Inject it into the EP
    ep.injectEvent(se);
  }
  
  public void notify(Notification[] n) {
    // Do nothing - we don't support sequences for now
  }
  
  /**
   * Handle shutdown - first let super do its cleanup, then kill the hd
   */
  public void shutdown() {
    super.shutdown();
    if(hd != null) {
      hd.shutdown(); // Unsubscribe from everything
      hd = null;
    }
  }
  
  /**
   * Get the "type" of input.
   */
  public String getType() {
    return "SienaInput";
  }
}