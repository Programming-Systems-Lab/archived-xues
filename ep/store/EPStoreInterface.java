package psl.xues.ep.store;

import psl.xues.ep.event.EPEvent;

/**
 * Interface to Event Packager for stores.
 * <p>
 * Copyright (c) 2002: The Trustees of Columbia University in the
 * City of New York.  All Rights Reserved.
 *  
 * <!--
 * TODO:
 * - Do we really need this method?
 * -->
 *
 * @author Janak J Parekh
 * @version $Revision$
 */
public interface EPStoreInterface {
  /**
   * Get a handle to an EPStore.
   *
   * @param store The name of the store you want a handle to.
   * @return The EPStore reference, or null if not found.
   */
  public EPStore getStore(String storeName);
  
  
  /**
   * Inject an event into the Event Packager.  Use any supported EPEvent
   * format.
   *
   * @param e The EPEvent you wish to inject.
   * @return A boolean indicating success.
   */
  public boolean injectEvent(EPEvent e);
}
