package psl.xues.ep.input;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.Random;

import psl.xues.ep.event.EPEvent;
import psl.xues.ep.event.StringEvent;

/**
 * Sample inputter to keep things sane.
 * <p>
 * Copyright (c) 2002: The Trustees of Columbia University in the
 * City of New York.  All Rights Reserved.
 *
 * <!--
 * TODO:
 * - Better documentation for this class
 * -->
 * 
 * @author Janak J Parekh <janak@cs.columbia.edu>
 * @version $Revision$
 */
public class RandomNumberInput extends EPInput {
  /** How long do we wait before random number generations (in ms)? */
  public int delay = 1000;
  /** The random number generator */
  private Random rand = null;

  /**
   * Constructor.
   */
  public RandomNumberInput(EPInputInterface ep, Element el) 
    throws InstantiationException {
    super(ep,el);

    // Now get the publishing frequency
    String publishDelay = el.getAttribute("Delay");
    if(publishDelay != null && publishDelay.length() > 0) {
      // Catch an exception if a non-integer is specified
      try {
	delay = Integer.parseInt(publishDelay);
      } catch(Exception e) {
	throw new InstantiationException("Invalid delay specified");
      }
    }
    
    // Create the random-number generator
    this.rand = new Random();
  }

  /**
   * Run method
   */
  public void run() {
    while(!shutdown) {
      int randomNumber = rand.nextInt();
      // Publish to EP
      StringEvent se = new StringEvent(getName(), "" + randomNumber);
      ep.injectEvent(se);
      try { Thread.currentThread().sleep(delay); } 
      catch(InterruptedException ie) { ; }
    }
  }

  /**
   * Get type of input.
   */
  public String getType() {
    return "RandomNumberInput";
  }
}
