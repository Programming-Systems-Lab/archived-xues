package psl.xues.ep.event;

import java.io.Serializable;

import org.apache.log4j.Logger;

/**
 * Abstract base class for EP event formats.  This is not intended to be
 * transported <em>between</em> EP instances, but rather to be used within
 * <em>one</em> EP.
 * <p>
 * Copyright (c) 2002: The Trustees of Columbia University in the
 * City of New York.  All Rights Reserved.
 *
 * <!--
 * TODO:
 * - Handle multiple/arbitrary sources
 * - Perhaps support wrapping EPEvent in another?
 * -->
 * <p>
 * <em>Copyright (c) 2002: The Trustees of Columbia University in the
 * City of New York.  All Rights Reserved.</em>
 *
 * @author Janak J Parekh <janak@cs.columbia.edu>
 * @version $Revision$
 */
public abstract class EPEvent implements Comparable, Serializable {
  /** Timestamp of this event, in OUR perception */
  protected long timestamp = -1;
  /** Debugger */
  protected Logger debug = null;
  /** 
   * Creator ("source", not "type").  NB: This creator must be the local
   * EP creator -- rules are fired by examining the source.
   */
  protected String source = null;
  
  /**
   * CTOR.  Assume that construction time is the correct timestamp time.
   *
   * @param source The generating source for this EPEvent.  Make sure it
   * uniquely identifies the inputter, else EP will not be able to do anything
   * useful with this event.
   */
  public EPEvent(String source) {
    this(source, System.currentTimeMillis());
  }
  
  /**
   * CTOR.  Use specified timestamp.  See comments from previous CTOR.
   */
  public EPEvent(String source, long timestamp) {
    this.timestamp = timestamp;
    this.source = source;
    // Build our debugger.  Note that if another class extends this one, we
    // will use that class's name as the debugger's instance name.
    debug = Logger.getLogger(this.getClass().getName());
  }
  
  /**
   * Get the format (type) of this event.  You have to implement this.
   *
   * @return A string indicating the type.
   */
  public abstract String getFormat();
  
  /**
   * Get the timestamp of this event.
   *
   * @return Standard UNIX time format (long)
   */
  public long getTimestamp() { return timestamp; }
  
  /**
   * Set the timestamp of this event.
   *
   * <!-- XXX - we really shouldn't provide public access to this! -->
   *
   * @param t Standard UNIX time format
   */
  public void setTimestamp(long t) {
    this.timestamp = t;
  }
  
  /**
   * Get the source of this event.
   *
   * @return The sourceID of this event.
   */
  public String getSource() { return source; }

  /**
   * Convert this event to one of another form.
   *
   * @return The EPEvent that's actually another event form, or null if it
   * cannot be done.
   */
  public EPEvent convertEvent(String newFormat) {
    debug.warn("No implementation of ConvertEvent for event type \"" + 
    getFormat() + "\"");
    return null;
  }
  
  /**
   * Comparison function.  ONLY COMPARES TIMESTAMPS (you have been WARNED!)
   *
   * @param o The other event to compare timestamps against.
   * @return An integer conforming to the Comparable interface.
   */
  public int compareTo(Object o) {
    if(!(o instanceof EPEvent))
      throw new ClassCastException("Can only compare EPEvents");

    if(this.timestamp < ((EPEvent)o).timestamp) return -1;
    else if(this.timestamp == ((EPEvent)o).timestamp) return 0;
    else return 1; // Must be greater
  }
}