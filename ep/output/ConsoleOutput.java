package psl.xues.ep.output;

import java.util.Iterator;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import psl.xues.ep.event.*;
import psl.xues.ep.util.EPConst;

import siena.Notification;

/**
 * Console output for debugging purposes.
 * <p>
 * Copyright (c) 2002: The Trustees of Columbia University in the
 * City of New York.  All Rights Reserved.
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
    debug.info(epe.getInfo());
    
    return true;  // Wasn't that hard?
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