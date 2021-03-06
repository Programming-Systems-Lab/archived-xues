package psl.xues.ep.transform;

import psl.xues.ep.store.EPStore;

/**
 * Interface to Event Packager for transforms.  Provides access to persistent
 * stores registered with the Event Packager.
 * <p>
 * Copyright (c) 2002: The Trustees of Columbia University in the
 * City of New York.  All Rights Reserved.
 * 
 * @author Janak J Parekh
 * @version $Revision$
 */
public interface EPTransformInterface {
  /**
   * Get a handle to an EPStore.
   *
   * @param store The name of the store you want a handle to.
   * @return The EPStore reference, or null if not found.
   */
  public EPStore getStore(String storeName);
}
