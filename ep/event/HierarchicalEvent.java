package psl.xues.ep.event;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * Default EP event representation: hierarchical format.  Supports many
 * possible inputs, including both flat and XML-like hierarchies.
 * <p>
 * <b>Warning: this format is not yet implemented.</b>
 * <p>
 * Copyright (c) 2002: The Trustees of Columbia University in the
 * City of New York.  All Rights Reserved.
 *
 * <!--
 * TODO:
 * - Finish abstract class implementation
 * - Create necessary implementations of this abstract class
 * -->
 *
 * @author Janak J Parekh <janak@cs.columbia.edu>
 * @revision $Revision$
 */
public abstract class HierarchicalEvent extends EPEvent {
  /** Format of this event */
  private final String format = "HierarchicalEvent";
  /** Associated data */
  private Document eventData = null;
  
  /**
   * Create an empty HierarchicalEvent.
   *
   * @param source The generator of this event.
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
