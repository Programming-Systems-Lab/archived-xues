package psl.xues.ep.transform;

import org.w3c.dom.Element;
import psl.xues.ep.event.EPEvent;
import psl.xues.ep.util.EPConst;

/**
 * Transform that forces an event conversion.  By default, Event Packager does
 * lazy conversion; this forces it explicitly.
 * <p>
 * Copyright (c) 2002: The Trustees of Columbia University in the
 * City of New York.  All Rights Reserved.
 *
 * @author Janak J Parekh <janak@cs.columbia.edu>
 * @version $Revision$
 */
public class EventConverter extends EPTransform implements EPConst {
  private short outputFormat = -1;  // -1 construes whatever comes our way
  
  /**
   * CTOR.
   *
   * @param el The element with initialization info for this transform.
   */
  public EventConverter(EPTransformInterface ep, Element el) 
  throws InstantiationException {
    super(ep,el);
    
    
    // Do we force output format?
    String outputFormat = el.getAttribute("OutputFormat");
    if(outputFormat != null && outputFormat.length() > 0) {
      if(outputFormat.equalsIgnoreCase("DOMEvent") ||
      outputFormat.equalsIgnoreCase("DOM_OBJECT")) {
        this.outputFormat = DOM_OBJECT;
      } else if(outputFormat.equalsIgnoreCase("SienaEvent") ||
      outputFormat.equalsIgnoreCase("SIENA_OBJECT")) {
        this.outputFormat = SIENA_OBJECT;
      } else if(outputFormat.equalsIgnoreCase("XMLEvent") ||
      outputFormat.equalsIgnoreCase("XML_OBJECT")) {
        this.outputFormat = XML_OBJECT;
      } else if(outputFormat.equalsIgnoreCase("StringEvent") ||
      outputFormat.equalsIgnoreCase("STRING_OBJECT")) {
        this.outputFormat = STRING_OBJECT;
      } else {
        debug.error("Invalid OutputFormat specified, ignoring");
      }
    }
  }
  
  /** 
   * Handle a transform request.
   *
   * @param original The EPEvent that needs transformation.
   * @return The transformed EPEvent, or null if you can't handle the
   * transform.
   */
  public EPEvent transform(EPEvent original) {
    EPEvent newepe = null;
    if(outputFormat != -1) {
      if(outputFormat == STRING_OBJECT) {
        newepe = original.convertEvent("StringEvent");
      } else if(outputFormat == XML_OBJECT || outputFormat == DOM_OBJECT) {
        newepe = original.convertEvent("DOMEvent");
      } else if(outputFormat == SIENA_OBJECT) {
        newepe = original.convertEvent("SienaEvent");
      } else {
        debug.error("Unhandled OutputFormat");
      }
    }
    if(newepe == null) {
      debug.warn("Couldn't convert event, returning original event");
      return original;
    }
      
    return newepe;
  }

  /**
   * Get the type.
   */
  public String getType() {
    return "EventConverter";
  }
}