package psl.xues.ep.input;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;

import org.w3c.dom.NodeList;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import siena.Notification;

import psl.xues.ep.event.SienaEvent;

/**
 * Random Siena-probe input.  We just generate random values and stick them
 * in a SienaEvent.
 * <p>
 * <b>Required attributes:</b> <tt>delay</tt> (<em>int</em>): How long
 * we should sleep between two events, in msec
 * <p>
 * <b>Required embedded elements:</b>
 * <ul>
 * <li>(one or more) <tt>Attribute</tt>s, with attributes
 * <tt>Name</tt> (<em>String</em>) and <tt>Type</tt> (<em>String</em>)
 * </li>
 * </ul>
 * <p>
 * Supported types include:
 * <ul>
 * <li>int</li>
 * <li>float</li>
 * </ul>
 * <p>
 * Copyright (c) 2000-2002: The Trustees of Columbia University and the
 * City of New York.  All Rights Reserved.
 *
 * @author Janak J Parekh (janak@cs.columbia.edu)
 * @version $Revision$
 */
public class RandomSienaProbe extends psl.xues.ep.input.EPInput {
  private HashMap attributes;
  private int delay = 1000; // How long to sleep between notifications?
  
  /**
   * CTOR.
   */
  public RandomSienaProbe(EPInputInterface ep, Element el)
  throws InstantiationException {
    super(ep, el);
    
    // Get delay
    try {
      delay = Integer.parseInt(el.getAttribute("delay"));
    } catch(Exception e) {
      throw new InstantiationException("Incorrect/missing delay supplied");
    }
    
    // What attribute(s) do we want to generate under?
    NodeList children = el.getChildNodes();
    if(children == null || children.getLength() == 0) {
      throw new InstantiationException("No attribute data supplied");
    }
    
    attributes = new HashMap();
    
    for(int i=0; i < children.getLength(); i++) {
      if(children.item(i).getNodeType() == Node.ELEMENT_NODE &&
      children.item(i).getNodeName().equalsIgnoreCase("Attribute")) {
        // Process
        String attrName = ((Element)children.item(i)).getAttribute("Name");
        String attrType = ((Element)children.item(i)).getAttribute("Type");
        if(attrName == null || attrName.length() == 0 || attrType == null ||
        attrType.length() == 0) {
          throw new InstantiationException("Invalid attribute data supplied");
        }
        attributes.put(attrName, attrType);
      }
    }
  }
  
  
  /**
   * Get the "type" of input.
   *
   * @return The type, as String.
   */
  public String getType() {
    return "RandomSienaProbe";
  }
  
  /**
   * Run method - generate random values in Siena Notification form and inject
   * them into EP.
   */
  public void run() {
    while(!shutdown) {
      Notification temp = new Notification();
      Iterator i = attributes.keySet().iterator();
      while(i.hasNext()) {
        String attrName = (String)i.next();
        String attrType = (String)attributes.get(attrName);
        if(attrType.equalsIgnoreCase("int")) {
          temp.putAttribute(attrName, new Random().nextInt());
        } else if(attrType.equalsIgnoreCase("float")) {
          temp.putAttribute(attrName, new Random().nextFloat());
        }
      }
      
      // Now publish temp
      ep.injectEvent(new SienaEvent(getName(),temp));
      
      // And sleep
      try {
        Thread.currentThread().sleep(delay);
      } catch(Exception e) { ; }
    }
  }
  
}
