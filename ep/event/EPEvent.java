package psl.xues.ep.event;

import org.apache.log4j.Category;

/**
 * Abstract base class for EP event formats.
 *
 * Copyright (c) 2002: The Trustees of Columbia University in the
 * City of New York.  All Rights Reserved.
 *
 * @author Janak J Parekh <janak@cs.columbia.edu>
 * @version $Revision$
 */
public abstract class EPEvent implements Comparable {
  /** Timestamp of this event, in OUR perception */
  protected long timestamp = -1;
  /** Debugger */
  protected Category debug = null;
  
  /**
   * CTOR.  Assume that construction time is the correct timestamp time.
   */
  public EPEvent() {
    timestamp = System.currentTimeMillis();
    // Build our debugger.  XXX - if another class extends this one?
    debug = Category.getInstance(this.getClass().getName());
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
  public long getTimestamp() {
    return timestamp;
  }

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