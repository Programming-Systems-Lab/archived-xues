package psl.xues.ep.event;

/**
 * Flat event format.  Compatible with Siena and others.
 * <p>
 * <b>Warning: this format is not yet implemented.</b>
 * <p>
 * Copyright (c) 2002: The Trustees of Columbia University in the
 * City of New York.  All Rights Reserved.
 *
 * <!--
 * TODO:
 * - Implement this!
 * -->
 *
 * @author Janak J Parekh <janak@cs.columbia.edu>
 * @version $Revision$
 */
public class FlatEvent extends EPEvent {
  /**
   * Create an empty FlatEvent.
   *
   * @param source The data source for this flat event.
   */
  public FlatEvent(String source) { super(source); }
  
  /** 
   * Get the format (type) of this event.
   *
   * @return A string indicating the type.
   */
  public String getFormat() {
    return "FlatEvent";
  }
}
