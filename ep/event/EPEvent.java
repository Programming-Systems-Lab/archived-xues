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
abstract class EPEvent {
  private final String format = "EPEvent";
  
  /**
   * Get the format (type) of this event.
   *
   * @return A string indicating the type.
   */
  public String getFormat() {
    return format;
  }
}