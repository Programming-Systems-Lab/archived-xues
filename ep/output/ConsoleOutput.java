package psl.xues.ep.output;

import java.util.Iterator;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import psl.xues.ep.event.*;

import siena.Notification;

/**
 * Console output for debugging purposes.
 * <p>
 * Copyright (c) 2002: The Trustees of Columbia University in the
 * City of New York.  All Rights Reserved.
 *
 * <!-- 
 * TODO:
 * - Handle every other kind of event other than DOMEvent better:)
 * -->
 *
 * @author Janak J Parekh <janak@cs.columbia.edu>
 * @version $Revision$
 */
public class ConsoleOutput extends EPOutput {
  /**
   * CTOR.
   */
  public ConsoleOutput(EPOutputInterface ep, Element el) 
  throws InstantiationException {
    super(ep,el);
  }
  
  /**
   * Run.  Do nothing.
   */
  public void run() {
    // Don't need to
    return;
  }
  
  /**
   * Handle an event.
   *
   * @param epe The EPEvent
   * @return Always true.
   */
  public boolean handleEvent(EPEvent epe) {
    // Is it a DOM output?
    if(epe instanceof DOMEvent) {
      if(((DOMEvent)epe).getDOMRoot() != null) {
        debug.info("Event (DOMEvent) received:");
        // Now walk it
        printDOMElement(((DOMEvent)epe).getDOMRoot(),0);
      } else {
        debug.info("It's an empty DOMEvent");
      }
    } else if(epe instanceof SienaEvent) {
      debug.info("Event (Siena) received:");
      printSienaEvent(((SienaEvent)epe).getSienaEvent());
    } else { // Just try to print it out
      debug.info("Event (" + epe.getFormat() + ") received");
      debug.info("Event contents:\n---" + epe + "---");
    }      
      
    return true;  // Wasn't that hard?
  }
  
  /**
   * Walk a DOM element.
   */
  private void printDOMElement(Element el, int level) {
    // print myself
    debug.info("" + level + "/Element/" + el.getNodeName());
    
    // Get embedded attributes and print them out first
    if(el.getAttributes() != null) {
      NamedNodeMap attrs = el.getAttributes();
      for(int i=0; i < attrs.getLength(); i++) {
        if(attrs.item(i).getNodeType() == Node.ATTRIBUTE_NODE) {
          Attr at = (Attr)attrs.item(i);
          debug.info("" + level + "/Element/" + el.getNodeName() +
          "/Attribute/" + at.getNodeName() + "/Value/" + at.getNodeValue());
        } else { // Not an attribute, just print it out
          debug.info("" + level + "/Element/" + el.getNodeName() +
          "/Other/" + attrs.item(i));
        }
      }
    }
    
    // Now get the children and walk them
    NodeList nl = el.getChildNodes();
    for(int i=0; i < nl.getLength(); i++) {
      if(nl.item(i).getNodeType() == Node.ELEMENT_NODE) {
        // Recurse
        printDOMElement((Element)nl.item(i), level + 1);
      } else { // Just print it out
        debug.info("" + level + 1 + "/Other/" + nl.item(i));
      }
    }
  }

  /**
   * Print Siena notification
   */
  private void printSienaEvent(Notification n) {
    Iterator i = n.attributeNamesIterator();
    while(i.hasNext()) {
      String attrName = (String)i.next();
      System.out.println("- " + attrName + " : " + n.getAttribute(attrName));
    }
  }
  
  
  /**
   * Handle shutdown.  (Like there's a lot to do in ConsoleOutput's shutdown :))
   */
  public void shutdown() {
    return;
  }
  
  /**
   * Get the type.
   */
  public String getType() {
    return "ConsoleOutput";
  }
}