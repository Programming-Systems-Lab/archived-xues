package psl.xues.ep.input;

import psl.xues.ep.event.*;
import psl.xues.ep.store.EPStore;

/**
 * EP input interface.  Inputters can use this to give EP callbacks when there
 * is new data.
 * <p>
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

  /**
   * Report an error, which will probably get passed to EventPackager's
   * logger verbatim.  ONLY use this if you don't have your own logger
   * (yet) for a good reason.  (You should consider instantiating log4j
   * somewhere...)
   *
   * @param err The error to report.
   */
  public void error(String src, String err);

  // Control methods
  
  /**
   * Request that the Event Packager be shutdown.
   */
  public void shutdown();
  
  /**
   * Ask the EP if we're in shutdown.
   *
   * @return A boolean indicating if we're in shutdonw
   */
  public boolean inShutdown();

  /**
   * Get a handle to an EPStore.
   *
   * @param store The name of the store you want a handle to.
   * @return The EPStore reference, or null if not found.
   */
  public EPStore getStore(String storeName);
}
