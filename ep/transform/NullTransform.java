package psl.xues.ep.transform;

import org.w3c.dom.Element;
import psl.xues.ep.event.EPEvent;

/**
 * Transform that does nothing.  Use as a model to write your own.
 * <p>
 * Copyright (c) 2002: The Trustees of Columbia University in the
 * City of New York.  All Rights Reserved.
 *
 * @author Janak J Parekh <janak@cs.columbia.edu>
 * @version $Revision$
 */
public class NullTransform extends EPTransform {
  /**
   * CTOR.  This should closely match EPTransform's constructor in terms of
   * parameters, etc.
   *
   * @param el The element with initialization info for this transform.
   */
  public NullTransform(EPTransformInterface ep, Element el) 
  throws InstantiationException {
    super(ep,el);
    
    // Insert custom constructor code here, including parsing the DOM element
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
  public EPEvent transform(EPEvent original) {
    return original;
  }

  /**
   * Get the type.
   */
  public String getType() {
    return "NullTransform";
  }
}