package psl.xues.ep.output;

import org.w3c.dom.Element;
import psl.xues.ep.event.EPEvent;

/**
 * Does exactly what its name implies: outputs results to /dev/null.
 * Useful as a stub output for null channels.
 * <p>
 * Copyright (c) 2002: The Trustees of Columbia University in the
 * City of New York.  All Rights Reserved.
 *
 * @author Janak J Parekh <janak@cs.columbia.edu>
 * @version $Revision$
 */
public class NullOutput extends EPOutput {
  /**
   * CTOR.
   */
  public NullOutput(EPOutputInterface ep, Element el) 
  throws InstantiationException {
    super(ep,el); // Although we don't really need it...
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
    return true;  // Wasn't that hard?
  }
  
  /**
   * Handle shutdown.  (Like there's a lot to do in NullOutput's shutdown :))
   */
  public void shutdown() {
    return;
  }
}