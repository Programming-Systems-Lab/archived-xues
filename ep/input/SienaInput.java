package psl.xues.ep.input;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.apache.log4j.Category;

import siena.AttributeConstraint;
import siena.Filter;
import siena.Notifiable;
import siena.HierarchicalDispatcher;
import siena.SienaException;
import siena.Notification;
import siena.TCPPacketReceiver;

import psl.xues.ep.event.EPEvent;
import psl.xues.ep.event.SienaEvent;

/**
 * Siena input filter for EP.
 *
 * Copyright (c) 2002: The Trustees of Columbia University in the
 * City of New York.  All Rights Reserved.
 *
 * TODO:
 * - Support bytearray filters (why does Siena have this?)
 *
 * @author Janak J Parekh <janak@cs.columbia.edu>
 * @version $Revision$
 */
public class SienaInput extends EPInput implements Notifiable {
  private String sienaHost = null;
  private HierarchicalDispatcher hd = null;
  private Category debug = null;
  
  /**
   * CTOR.  Modeled after the EPInput CTOR.
   */
  public SienaInput(EPInputInterface ep, Element el) 
  throws InstantiationException {
    super(ep, el);
    // Now parse the element and see if we can get all of the needed
    // information.
    sienaHost = el.getAttribute("SienaHost");
    if(sienaHost == null || sienaHost.length() == 0) {
      debug.error("Siena host not specified, " +
      "cannot continue building Siena input filter");
      sienaHost = null;
      throw new InstantiationException("No Siena host specified");
    }
    
    // Now actually try and connect
    hd = new HierarchicalDispatcher();
    try {
      hd.setReceiver(new TCPPacketReceiver(0));
      hd.setMaster(sienaHost);
    } catch(Exception ex) {
      debug.error("Cannot connect to specified Siena host", ex);
      hd = null;
      throw new InstantiationException("Cannot connect to Siena host");
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
        Element constraintTemplate = (Element)constraints.item(i);
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
        if(op == null || op.length() == 0) {
          op = "EQ"; // Default operation in Siena as well
        }
        if(valueType == null || valueType.length() == 0) {
          valueType = "String"; // Default format in Siena
        }
        
        // Add the constraint now, based on filterType
        try {
          if(valueType.equalsIgnoreCase("String"))
            f.addConstraint(attrName, value);
          else if(valueType.equalsIgnoreCase("Boolean"))
            f.addConstraint(attrName, Boolean.valueOf(value).booleanValue());
          else if(valueType.equalsIgnoreCase("ByteArray"))
            debug.warn("Bytearrays not yet supported in Siena constraint");
          else if(valueType.equalsIgnoreCase("Double"))
            f.addConstraint(attrName, Double.parseDouble(value));
          else if(valueType.equalsIgnoreCase("Integer") ||
          valueType.equalsIgnoreCase("Int"))
            f.addConstraint(attrName, Integer.parseInt(value));
          else if(valueType.equalsIgnoreCase("Long"))
            f.addConstraint(attrName, Long.parseLong(value));
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
    SienaEvent se = new SienaEvent(n);
    
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
    hd.shutdown(); // Unsubscribe from everything
    hd = null;
  }
}