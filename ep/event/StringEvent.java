package psl.xues.ep.event;

import siena.Notification;

/**
 * Simple Siena event representation.  Embeds a string.
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
  private final String format = "StringEvent";
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
   * @param n The String to use.
   */
  public StringEvent(String source, String data) {
    super(source);
    this.data = data;
  }

  /**
   * CTOR given an existing String and a timestamp.
   *
   * @param source The generator ("source") of these events.
   * @param n The String to use.
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
}