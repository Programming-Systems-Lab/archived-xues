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
import psl.xues.ep.store.EPStore;
import psl.xues.util.SienaUtils;

/**
 * Siena input filter for EP.  Supports all versions up to and including
 * Siena 1.4.2.  (Note that Siena 1.4.0-1 is extremely buggy and will probably
 * not work well.)
 * <p>
 * Usage: <em>([] implies an optional parameter)</em></p>
 * <p><tt>
 * &lt;Inputs&gt;<br>
 * <blockquote>&lt;Inputter Name="<em>instance name</em>" 
 * Type="psl.xues.ep.input.SienaInput" [SienaHost="<em>senp url</em>"]
 * [SienaReceivePort="<em>port number</em>"] [Control="true"]&gt;
 * <blockquote>&lt;SienaFilter&gt;
 * <blockquote>[&lt;SienaConstraint AttributeName="<em>attribute name</em>"
 * Op="<em>matching operator</em>" ValueType="<em>value type</em>"
 * Value="<em>attribute value</em>"&gt;]
 * <br>...</blockquote>
 * &lt;/SienaFilter&gt;
 * <br>...</blockquote>
 * &lt;/Inputter&gt;
 * </blockquote>
 * &lt;/Inputs&gt;
 * </tt></p>
 * <p>
 * Attributes/parameters: <ol>
 * <li><b>SienaHost</b>: Specifies the Siena master that this node will
 * connect to (in Siena URL format {which is version-dependent}).  If this field
 * is left blank, this node will assume that it itself is a Siena master (in
 * which case, specify a port if you want it to be remotely-accessible; else it
 * will be completely local).</li>
 * <li><b>SienaReceivePort</b>: Specifies the port on which this node will
 * establish a TCPPacketReceiver.  Particularly useful if this node is a
 * master.</li>
 * <li><b>Control</b>: Specify "true" if you want this to be a EP control
 * channel.  See the section below for more details on how to use this.</li>
 * </ol><p>
 * Filters:<p>
 * If no filters are specified, this inputter will not receive any Siena
 * events.  To create a filter, embed a <b>SienaFilter</b> element within
 * the declaration of this inputter.  SienaFilter takes one or more
 * <b>SienaConstraint</b>s, which in turn support the following attributes:<ul>
 * <li>AttributeName;</li>
 * <li>Op;</li>
 * <li>ValueType (which can be String, boolean, double, or long);</li>
 * <li>Value.</li>
 * </ul> 
 * You can also create a "wildcard filter", which has no SienaConstraints,
 * and which therefore subscribes to all events on the specified event bus.
 * <p>
 * Siena control: <p>
 * If this node is enabled for control (see "Attributes" above), this Siena
 * node will, in addition to custom filters, listen for events that match
 * the AttributeValue { "Type" = "EPInput" } and dispatch appropriate control
 * signals to EP, specified as follows: <ol>
 * <li><b>{ "Request" = "Replay" }</b>: Request a replay.  Other parameters
 * in the Siena event are:<ul>
 *   <li>Store (required): Name of the store to replay events from;</li>
 *   <li>Source (optional): Name of the source to replay events for;</li>
 *   <li>StartTime (optional): Timestamp, using ms since 1970 in an 8-byte
 *       Java long;</li>
 *   <li>EndTime (optional): Timestamp, format same as StartTime; if one of the
 *       two are specified, the other is required;</li>
 *   <li>OriginalTime: Play back events in the original time intervals ("true"),
 *       or stream them contiguously ("false")?</li></ul>
 * </li>
 * </ol><p>
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
          else if(valueType.equalsIgnoreCase("boolean"))
            f.addConstraint(attrName, opcode,
            Boolean.valueOf(value).booleanValue());
          else if(valueType.equalsIgnoreCase("ByteArray"))
            debug.warn("Bytearrays not yet supported in Siena constraint, "+
            "ignoring constraint");
          else if(valueType.equalsIgnoreCase("double"))
            f.addConstraint(attrName, opcode, Double.parseDouble(value));
          else if(valueType.equalsIgnoreCase("long"))
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
          debug.warn("Cannot subscribe filter \"" + filterName + "\"", se);
          continue; // Do we need to make this explicit?
        }
      }
      // And we continue with the next filter...
    } // end subscription loop
    
    
    if(control) {
      // Subscription for EP control channel
      Filter f = new Filter();
      f.addConstraint("Type", "EPControl");
      try {
        debug.info("Subscribing control-channel filter...");
        hd.subscribe(f, this);
      } catch(SienaException se) {
        debug.warn("Cannot subscribe control-channel filter", se);
        // XXX - do we need to do anything here?
      }
    }
    
    // Now we don't need to do a whole lot here, since Siena runs in its own
    // thread when it notifies us, so let's just use the default
    // implementation
    super.run();
  }
  
  public void notify(Notification n) {
    debug.debug("Received notification " + n);
    
    // If it's not a control event, inject it into the EP
    if(!control || n.getAttribute("Type") == null ||
    (!n.getAttribute("Type").stringValue().equals("EPControl"))) {
      // Construct a new SienaEvent and wrap the notification in there
      SienaEvent se = new SienaEvent(getName(), n);
      ep.injectEvent(se);
    } else {
      // Sanity check
      if(n.getAttribute("Request") == null) {
        debug.error("Invalid request \"" + n + "\" to EP, ignoring");
        return;
      }
      
      // Try to parse the control message
      if(n.getAttribute("Request").stringValue().equals("Replay")) {
        // Get replay parameters
        String store = n.getAttribute("Store") == null ? null : n.getAttribute("Store").stringValue();
        String source = n.getAttribute("Source") == null ? null : n.getAttribute("Source").stringValue();
        long t1 = n.getAttribute("StartTime") == null ? -1 : n.getAttribute("StartTime").longValue();
        long t2 = n.getAttribute("EndTime") == null ? -1 : n.getAttribute("EndTime").longValue();
        boolean orgTime = n.getAttribute("OriginalTime") == null ? false : n.getAttribute("OriginalTime").booleanValue();
        // Sanity checks
        if(store == null || ep.getStore(store) == null) {
          debug.error("Cannot replay without valid store reference, ignoring");
          return;
        }
        if(source == null && (t1 == -1 || t2 == -1)) {
          debug.error("Cannot replay without source or timestamp, ignoring");
        }
        // Now playback based on what we have
        EPStore eps = ep.getStore(store);
        if(eps.playbackEvents(getName(), source, t1, t2, orgTime) == false) {
          debug.debug("No events found to actually playback");
          // XXX - send Siena notification back?
        }
      }
    }
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