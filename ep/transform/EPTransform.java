package psl.xues.ep.transform;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;
import psl.xues.ep.event.EPEvent;

/**
 * Abstract class for transforms.  Extend this if you want to implement
 * your own.
 * <p>
 * Copyright (c) 2002: The Trustees of Columbia University in the
 * City of New York.  All Rights Reserved.
 *
 * <!--
 * TODO:
 * - Consider making a thread (but first figure out if there is any advantage
 *   to this, since transforms have implied orderings)
 * -->
 *
 * @author Janak J Parekh <janak@cs.columbia.edu>
 * @version $Revision$
 */
public abstract class EPTransform {
  /** Instance ID */
  protected String transformID = null;
  /** Debugger */
  protected Logger debug = null;
  /** EP reference */
  protected EPTransformInterface ep = null;
  
  /**
   * CTOR.  The element is provided for any special customizations on this
   * transform.
   *
   * @param el The element with initialization info for this transform.
   */
  public EPTransform(EPTransformInterface ep, Element el) 
  throws InstantiationException {
    // Attempt to obtain our transformID
    this.transformID = el.getAttribute("Name");
    if(transformID == null || transformID.length() == 0) {
      throw new InstantiationException("No transformID specified for " + 
      "transform");
    }
    
    // Initialize our debugger
    debug = Logger.getLogger(this.getClass().getName() + "." + 
    transformID);

    // Store event packager reference
    this.ep = ep;
  }
  
  /**
   * Get our instance name.
   *
   * @return The string representing the instance name of this transform.
   */
  public String getName() {
    return transformID;
  }

  /**
   * Handle a transform request.  You must implement this to be of any use.
   * NOTE: Unless you have a good reason, copy the existing sourceID...
   * otherwise EP might confused.
   *
   * @param original The EPEvent that needs transformation.
   * @return The transformed EPEvent, or null if you can't handle the 
   * transform.
   */
  public abstract EPEvent transform(EPEvent original);
}
