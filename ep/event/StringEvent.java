package psl.xues.ep.event;

import siena.Notification;
import org.w3c.dom.Document;
import psl.xues.util.JAXPUtil;
import org.xml.sax.InputSource;
import java.io.StringReader;

/**
 * String event representation.  Embeds a string.
 * <p>
 * Copyright (c) 2002: The Trustees of Columbia University in the
 * City of New York.  All Rights Reserved.
 *
 * <!--
 * TODO:
 * - Support conversion to basic formats.  (Where should this be?)
 * -->
 *
 * @author Janak J Parekh <janak@cs.columbia.edu>
 * @version $Revision$
 */
public class StringEvent extends EPEvent {
  /** Format for this event. */
  private final String format = "StringEvent";
  /** Associated data. */
  private String data = null;
 
  /**
   * Base CTOR.
   *
   * @param source The generator ("source") of these events.
   */
  public StringEvent(String source) { this(source, null); }

  /**
   * CTOR given an existing String.
   *
   * @param source The generator ("source") of these events.
   * @param data The String to use.
   */
  public StringEvent(String source, String data) {
    super(source);
    this.data = data;
  }

  /**
   * CTOR given an existing String and a timestamp.
   *
   * @param source The generator ("source") of these events.
   * @param data The String to use.
   * @param t The timestamp.
   */
  public StringEvent(String source, String data, long t) {
    super(source, t);
    this.data = data;
  }
  
  /**
   * Get the embedded String.  This is NOT a copy, but rather the orignal.  
   * Modify with care!
   *
   * @return The embedded String "event"
   */
  public String getStringEvent() {
    return data;
  }
  
  /** 
   * Get the format (type) of this event.
   *
   * @return A string indicating the type.
   */
  public String getFormat() {
    return "StringEvent";
  }
  
  
  /**
   * Convert this event to one of another form.
   *
   * @param newFormat The requested event format.
   * @return The EPEvent that's actually another event form, or null if it
   * cannot be done.
   */
  public EPEvent convertEvent(String newFormat) {
    if(newFormat.equalsIgnoreCase("DOMEvent")) try {
      // Create a new parser and parse the string into a document
      Document d = JAXPUtil.newDocumentBuilder().
      parse(new InputSource(new StringReader(data)));
      return new DOMEvent(getSource(), d);
    } catch(Exception e) {
      debug.warn("Convert to DOM failed", e);
      return null;
    }
    
    debug.warn("Can't convert StringEvent to type \"" + newFormat + "\"");
    return null;
  }
  
  
}