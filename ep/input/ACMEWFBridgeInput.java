package psl.xues.ep.input;

import siena.Notification;
import siena.Notifiable;
import siena.HierarchicalDispatcher;
import siena.Filter;
import org.w3c.dom.Element;

/**
 * DASADA DEMO 2002 ACME<->WF bridge implementation.
 *
 * @author Janak J Parekh
 * @version $Revision$
 */
public class ACMEWFBridgeInput extends EPInput implements Notifiable {
  private String sienaHost = null;
  private HierarchicalDispatcher hd = null;
  private int doublePreventer = 0;
  
  public ACMEWFBridgeInput(EPInputInterface ep, Element el) throws
  InstantiationException {
    super(ep, el);
    
    // Connect to siena host
    this.sienaHost = el.getAttribute("SienaHost");
    if(sienaHost == null || sienaHost.length() == 0) {
      throw new InstantiationException("Siena host not specified");
    }
    
    try {
      hd = new HierarchicalDispatcher();
      hd.setMaster(sienaHost);
    } catch(Exception e) {
      debug.error("Siena join error", e);
      throw new InstantiationException("Siena join error");
    }
  }
  
  public String getType() {
    return "ACMEWFBridgeInput";
  }
  
  public void shutdown() {
    shutdown = true;
    hd.shutdown();
  }
  
  public void run() {
    // Create subscriptions
    try {
      Filter f = new Filter();
      f.addConstraint("Type", "TailorDirective");
      hd.subscribe(f, this);
      
      Filter f2 = new Filter();
      f.addConstraint("Type", "TailorQuery");
      hd.subscribe(f2, this);
      
      Filter f3 = new Filter();
      f.addConstraint("Type", "GWJobTable");
      hd.subscribe(f3, this);
      
      Filter f4 = new Filter();
      f.addConstraint("Type", "WFToTailor");
      hd.subscribe(f4, this);
    } catch(Exception e) {
      debug.error("Siena subscribe error", e);
      return;
    }
    
    // Now sit and wait...
  }
  
  public void notify(Notification n) {
    // What have we received?
    
    // 1. Initial Tailor repair request
    if(n.getAttribute("Type") != null &&
    n.getAttribute("Level") != null &&
    n.getAttribute("RepairDirective") != null &&
    n.getAttribute("Type").stringValue().equals("TailorDirective") &&
    n.getAttribute("Level").stringValue().equals("ARCHITECTURE") &&
    n.getAttribute("RepairDirective").stringValue().equals("migrateServiceToNewHost")) {
      debug.debug("Received migrate request");
      // Create the new Notification
      Notification n2 = new Notification();
      n2.putAttribute("Type", "TailorDirective");
      n2.putAttribute("Level", "IMPLEMENTATION");
      n2.putAttribute("DirectiveID", n.getAttribute("DirectiveID"));
      n2.putAttribute("RepairDirective", "migrateServiceToNewHost");
      n2.putAttribute("ServiceType", "XXX");
      n2.putAttribute("Service", "XXX");
      n2.putAttribute("Owner", "XXX");
      n2.putAttribute("oldHost", n.getAttribute("OldHost"));
      n2.putAttribute("newHost", n.getAttribute("NewHost"));
      // Publish it
      try {
        debug.debug("Publishing Tailor message " + n2);
        hd.publish(n2);
      } catch(Exception e) {
        debug.error("Could not publish", e);
      }
    }
    
    // 2. Repair is complete
    else if(n.getAttribute("Type") != null &&
    n.getAttribute("Level") != null &&
    n.getAttribute("Type").stringValue().equals("WFToTailor") &&
    n.getAttribute("Type").stringValue().equals("IMPLEMENTATION")) {
      // Final notification?
      if(n.getAttribute("Success") != null) {
        // First or second copy?
        if(doublePreventer == 1) {
          doublePreventer = 0;
          return; // Nothing to do, actually, we already sent out one
        } else doublePreventer = 1;
        
        // Copy back to Tailor
        Notification n2 = new Notification();
        n2.putAttribute("Type", "WFToTailor");
        n2.putAttribute("Level", "ARCHITECTURE");
        n2.putAttribute("DirectiveID", n.getAttribute("DirectiveID"));
        n2.putAttribute("Success", n.getAttribute("Success"));
        // Check if it was actually successful
        if(n.getAttribute("Success").booleanValue() == false) {
          debug.warn("Repair was unsuccessful");
        }
        n2.putAttribute("NewHost", n.getAttribute("newHost"));

        // Finally, publish
        try {
          debug.debug("Publishing repair message" + n2);
          hd.publish(n2);
        } catch(Exception e) {
          debug.error("Could not publish", e);
        }
      } else {
        // No, intermediate.  We don't do anything useful with this, just
        // print it out for debugging purposes.
        debug.debug("Received WFToTailor INTERMEDIATE event " + n);
      }
    }
    
    // 3. Tailor asks query
    else if(n.getAttribute("Type") != null && 
    n.getAttribute("Query") != null &&
    n.getAttribute("Level") != null &&
    n.getAttribute("Direction") != null &&
    n.getAttribute("Type").stringValue().equals("TailorQuery") &&
    n.getAttribute("Query").stringValue().equals("findBestHostForService") &&
    n.getAttribute("Level").stringValue().equals("ARCHITECTURE") &&
    n.getAttribute("Direction").stringValue().equals("Request")) {
      Notification n2 = new Notification();
      n2.putAttribute("Type", "TailorQuery");
      n2.putAttribute("Level", "IMPLEMENTATION");
      n2.putAttribute("Direction", "Request");
      n2.putAttribute("QueryID", n.getAttribute("QueryID"));
      n2.putAttribute("Query", "findBestHostForService");
      n2.putAttribute("ServiceType", "XXX");
      try {
        debug.debug("Publishing query message " + n2);
        hd.publish(n2);
      } catch(Exception e) {
        debug.error("Could not publish Tailor lookup", e);
      }
    }
    
    // 4. Workflakes replies to query
    else if(n.getAttribute("Type") != null &&
    n.getAttribute("Query") != null &&
    n.getAttribute("Level") != null &&
    n.getAttribute("Direction") != null &&
    n.getAttribute("Type").stringValue().equals("TailorQuery") &&
    n.getAttribute("Query").stringValue().equals("findBestHostForService") &&
    n.getAttribute("Level").stringValue().equals("IMPLEMENTATION") &&
    n.getAttribute("Direction").stringValue().equals("Reply")) {
      Notification n2 = new Notification();
      n2.putAttribute("Type", "TailorQuery");
      n2.putAttribute("Level", "ARCHITECTURE");
      n2.putAttribute("Direction", "Reply");
      n2.putAttribute("QueryID", n.getAttribute("QueryID"));
      n2.putAttribute("Query", "findBestHostForService");
      n2.putAttribute("Result", n.getAttribute("Result"));
      n2.putAttribute("Success", n.getAttribute("Success"));
      // Sanity check
      if(n.getAttribute("Success").booleanValue() == false) {
        debug.warn("Could not queryBestHostForService");
      }
      try {
        debug.debug("Publishing reply message " + n2);
        hd.publish(n2);
      } catch(Exception e) { 
        debug.error("Could not publish Workflakes reply", e);
      }
    }      
  }
  
  public void notify(Notification[] n) { ; }
  
}
