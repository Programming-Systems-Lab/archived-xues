package psl.xues.ep.transform;

import org.apache.log4j.Category;
import org.w3c.dom.Element;
import psl.xues.event.EPEvent;

/**
 * Transform that does nothing.  Use as a model to write your own.
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
  public NullTransform(Element el) {
    super(el);
    
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
}