package psl.xues.ep.event;

import org.w3c.dom.Element;

import org.apache.log4j.Category;

/**
 * Simple Siena event representation.  Embeds a DOM Element.
 *
 * TODO:
 * - Support conversion to basic formats.  (Where should this be?)
 *
 * @author Janak J Parekh <janak@cs.columbia.edu>
 * @version $Revision$
 */
public class DOMEvent extends EPEvent {
  private final String format = "DOMEvent";
  private Element data = null;
  private Category debug = Category.getInstance(SienaEvent.class.getName());
  
  /**
   * Base CTOR.
   *
   * @param source The generator ("source") of these events.
   */
  public DOMEvent(String source) { this(source, null); }

  /**
   * CTOR given an existing DOM element.
   *
   * @param source The generator ("source") of these events.
   * @param n The DOM element to use (NOTE: it is NOT copied!)
   */
  public DOMEvent(String source, Element data) {
    super(source);
    this.data = data;
  }

  /**
   * CTOR given an existing DOM element and a timestamp.
   *
   * @param source The generator ("source") of these events.
   * @param n The DOM element to use (NOTE: it is NOT copied!)
   * @param t The timestamp.
   */
  public DOMEvent(String source, Element data, long t) {
    super(source, t);
    this.data = data;
  }
  
  /**
   * Get the embedded DOM element.  This is NOT a copy, but rather the original.  
   * Modify with care!
   *
   * @return The embedded DOM "event"
   */
  public Element getDOMEvent() {
    return data;
  }
  
  /** 
   * Get the format (type) of this event.
   *
   * @return A string indicating the type.
   */
  public String getFormat() {
    return "DOMEvent";
  }
}