package psl.xues.ep.output;

import org.w3c.dom.Element;
import org.apache.log4j.Category;

import psl.xues.ep.EventPackager;

/**
 * Output mechanism for EP.  Extend this class if you want to output stuff
 * in a new format.
 *
 * @author Janak J Parekh <janak@cs.columbia.edu>
 * @version $Revision$
 */
public abstract class EPOutput {
  /** Instance name */
  protected String outputID = null;
  /** Logger */
  protected Category debug = null;
  
  /**
   * CTOR.  You are instantiated by the Event Packager and given the XML DOM
   * Element that corresponds to your configuration.
   *
   * @param e The element containing useful configuration information.
   */
  public EPOutput(Element e) {
    // Attempt to identify our instance name, which we call sourceID
    this.outputID = e.getAttribute("Name");
    if(outputID == null || outputID.length() == 0) {
      // Shouldn't happen, just for paranoia's sake
      outputID = this.getClass().getName();
    }
    
    // Set up the debugging.  We need the type for this as well.
    debug = Category.getInstance(this.getClass() + "." + outputID);
    System.err.println("DEBUGOut: " + this.getClass()); // XXX
  }
}