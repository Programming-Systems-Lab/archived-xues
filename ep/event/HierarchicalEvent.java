package psl.xues.ep.event;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * Default EP event representation: hierarchical format.  Supports many
 * possible inputs, including both flat and XML-like hierarchies.
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
 * @revision $Revision$
 */
public class HierarchicalEvent extends EPEvent {
  private final String format = "HierarchicalEvent";
  private Document eventData = null;
  
  /**
   * Create an empty HierarchicalEvent.
   */
  public HierarchicalEvent(String source) { super(source); }
  
  /** 
   * Get the format (type) of this event.
   *
   * @return A string indicating the type.
   */
  public String getFormat() {
    return "HierarchicalEvent";
  }
  
}
