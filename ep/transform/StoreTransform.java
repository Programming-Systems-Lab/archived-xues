package psl.xues.ep.transform;

import psl.xues.ep.event.EPEvent;
import psl.xues.ep.store.EPStore;

import org.w3c.dom.Element;

/**
 * Transform that snapshots the current event and writes it to the
 * configured store.
 * <p>
 * Attributes/parameters: <ol>
 * <li><b>StoreName</b>: Specifies the store instance to capture events to.
 * </ol>
 * <p>
 * Copyright (c) 2002: The Trustees of Columbia University in the
 * City of New York.  All Rights Reserved.
 * 
 * <!--
 * TODO:
 * - Consider additional filtration mechanisms.
 * -->
 *
 * @author Janak J Parekh
 * @version $Revision$
 */
public class StoreTransform extends EPTransform {
  private String storeName = null;
  private EPStore storeRef = null;
  
  public StoreTransform(EPTransformInterface ep, Element el) 
  throws InstantiationException {
    super(ep,el);
    
    // What store should we be using?
    storeName = el.getAttribute("StoreName");
    if(storeName == null || storeName.length() == 0) {
      throw new InstantiationException("No store instance specified");
    }
    
    // Now try and get the store reference
    storeRef = ep.getStore(storeName);
    if(storeRef == null) {
      throw new InstantiationException("Invalid store instance specified");
    }
  }
  
  /** 
   * Handle the transform request.  We just store the event and pass it
   * along unchanged.
   *
   * @param original The EPEvent that needs transformation.
   * @return The transformed EPEvent, or null if you can't handle the
   * transform.
   */
  public EPEvent transform(EPEvent original) {
    // Just throw the event to the store.  If it can store it, great; if not,
    // we don't worry about it.
    storeRef.storeEvent(original);
    return original; // No changes
  }
  
  /**
   * Get the type.
   */
  public String getType() {
    return "StoreTransform";
  }
}

