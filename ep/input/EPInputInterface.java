package psl.xues.ep.input;

import psl.xues.ep.event.*;

/**
 * EP input interface.  Inputters can use this to give EP callbacks when there
 * is new data.
 *
 * Copyright (c) 2002: The Trustees of Columbia University in the
 * City of New York.  All Rights Reserved.
 *
 * @author Janak J Parekh <janak@cs.columbia.edu>
 * @version $Revision$
 */
public interface EPInputInterface {
  /**
   * Inject an event into the Event Packager.  Use any supported EPEvent
   * format.
   *
   * @param e The EPEvent you wish to inject.
   * @return A boolean indicating success.
   */
  public boolean injectEvent(EPEvent e);

  /**
   * Get a list of supported EPEvent-based event formats.
   *
   * @return A list of Strings with the types of event formats.
   */
  public String[] getSupportedEventFormats();
}
