package psl.xues.ep.event;

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
  /** Intrinsic format.  Override this. */
  protected final String format = "EPEvent";
  /** Timestamp of this event, in OUR perception */
  protected long timestamp = -1;
  
  /**
   * CTOR.  Assume that construction time is the correct timestamp time.
   */
  public EPEvent() {
    timestamp = System.currentTimeMillis();
  }
  
  /**
   * Get the format (type) of this event.
   *
   * @return A string indicating the type.
   */
  public String getFormat() {
    return format;
  }
  
  /**
   * Get the timestamp of this event.
   *
   * @return Standard UNIX time format (long)
   */
  public long getTimestamp() {
    return timestamp;
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