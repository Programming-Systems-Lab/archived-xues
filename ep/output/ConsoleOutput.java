package psl.xues.ep.output;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import psl.xues.ep.event.*;

/**
 * Console output for debugging purposes.
 *
 * <!-- TODO:
 * - Handle every other kind of event other than DOMEvent :)
 * -->
 *
 * @author Janak J Parekh <janak@cs.columbia.edu>
 * @version $Revision$
 */
public class ConsoleOutput extends EPOutput {
  /**
   * CTOR.
   */
  public ConsoleOutput(Element el) throws InstantiationException {
    super(el);
  }
  
  /**
   * Run.  Do nothing.
   */
  public void run() {
    // Don't need to
    return;
  }
  
  /**
   * Handle an event.  NullOutput does absolutely zip.
   *
   * @param epe The EPEvent
   * @return Always true.
   */
  public boolean handleEvent(EPEvent epe) {
    // Print out basic EPEvent information
    debug.info("Event received: " + epe);
    // Is it a DOM output?
    if(epe instanceof DOMEvent) {
      if(((DOMEvent)epe).getDOMEvent() != null) {
        debug.info("It's a DOMEvent, now walking");
        // Now walk it
        walkElement(((DOMEvent)epe).getDOMEvent(),0);
      } else {
        debug.info("It's an empty DOMEvent");
      }
    }
    return true;  // Wasn't that hard?
  }
  
  /**
   * Walk an element.
   */
  public void walkElement(Element el, int level) {
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
        walkElement((Element)nl.item(i), level + 1);
      } else { // Just print it out
        debug.info("" + level + 1 + "/Other/" + nl.item(i));
      }
    }
  }
  
  /**
   * Handle shutdown.  (Like there's a lot to do in ConsoleOutput's shutdown :))
   */
  public void shutdown() {
    return;
  }
}