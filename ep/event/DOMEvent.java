package psl.xues.ep.event;

import org.w3c.dom.Element;
import org.w3c.dom.Document;
import psl.xues.util.JAXPUtil;
import java.io.StringWriter;
import javax.xml.transform.Transformer;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/**
 * Simple DOM-based event representation.  Embeds a DOM Element.
 * <p>
 * Copyright (c) 2002: The Trustees of Columbia University in the
 * City of New York.  All Rights Reserved.
 *
 * <!--
 * TODO:
 * - Support conversion to Siena format.
 * -->
 *
 * @author Janak J Parekh <janak@cs.columbia.edu>
 * @version $Revision$
 */
public class DOMEvent extends EPEvent {
  /** Format of this event */
  private final String format = "DOMEvent";
  /** Associated DOM tree */
  private Document data = null;
  
  /**
   * Base CTOR.
   *
   * @param source The generator ("source") of these events.
   */
  public DOMEvent(String source) { this(source, (Document)null); }
  
  /**
   * CTOR given an existing DOM document.
   *
   * @param source The generator ("source") of these events.
   * @param data The DOM document to use (NOTE: it is NOT copied!)
   */
  public DOMEvent(String source, Document data) {
    super(source);
    this.data = data;
  }
  
  /**
   * CTOR given an existing DOM element.  We wrap a new document around it.
   *
   * @param source The generator ("source") of these events.
   * @param data The DOM element to use (NOTE: it is NOT copied!)
   */
  public DOMEvent(String source, Element data) {
    super(source);
    this.data = JAXPUtil.newDocument(data);
  }
  
  /**
   * CTOR given an existing DOM element and a timestamp.
   *
   * @param source The generator ("source") of these events.
   * @param data The DOM element to use (NOTE: it is NOT copied!)
   * @param t The timestamp.
   */
  public DOMEvent(String source, Element data, long t) {
    super(source, t);
    this.data = JAXPUtil.newDocument(data);
  }
  
  /**
   * Get the embedded DOM document.  This is NOT a copy, but rather the
   * original.  Handle with care!
   *
   * @return The embedded DOM document.
   */
  public Document getDOMDocument() {
    return data;
  }
  
  /**
   * Get the embedded DOM element.  This is NOT a copy, but rather the original.
   * Modify with care!
   *
   * @return The embedded DOM "event"
   */
  public Element getDOMRoot() {
    if(data != null)
      return data.getDocumentElement();
    return null;
  }
  
  /**
   * Get the format (type) of this event.
   *
   * @return A string indicating the type.
   */
  public String getFormat() {
    return "DOMEvent";
  }
  
  /**
   * Convert this DOMEvent to an event of a different format.
   */
  public EPEvent convertEvent(String newFormat) {
    // TO STRING FORMAT ///////////////////////////////////////////////
    if(newFormat.equalsIgnoreCase("StringEvent")) {
      // DOM output to String
      StringWriter sw = new StringWriter();
      // Use transformer
      Transformer t = JAXPUtil.newTransformer();
      // Set up the source and target
      DOMSource source = new DOMSource(data);
      StreamResult result = new StreamResult(sw);
      try {
        t.transform(source, result);
      } catch(Exception e) {
        debug.error("Could not convert event to String", e);
        return null;
      }
      // Finally, construct the StringEvent and return it
      return new StringEvent(getSource(), sw.toString());
    }
    // TO OTHER FORMAT... /////////////////////////////////////////////
    
    
    // Not supported
    return super.convertEvent(newFormat);
  
  }

}