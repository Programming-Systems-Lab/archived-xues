package psl.xues.ep.store;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;

import psl.xues.ep.event.EPEvent;

/**
 * Store interface.  Support these methods to allow EP to use this mechanism
 * as an event store.  You will be run as a thread.  Make sure to handle
 * sources and timestamps as first-class search primitives.
 * <p>
 * Copyright (c) 2002: The Trustees of Columbia University in the
 * City of New York.  All Rights Reserved.
 * 
 * @author Janak J Parekh <janak@cs.columbia.edu>
 * @version $Revision$
 */
public abstract class EPStore implements Runnable {
  /** The name of this instance */
  protected String storeID = null;
  /** Our logger */
  protected Logger debug = null;
  /** Are we in shutdown? */
  protected boolean shutdown = false;
  /** EP reference */
  protected EPStoreInterface ep = null;

  /**
   * CTOR.
   */
  public EPStore(EPStoreInterface ep, Element el) 
  throws InstantiationException {
    this.storeID = el.getAttribute("Name");
    if(storeID == null || storeID.length() == 0) {
      throw new InstantiationException("No ID specified for store");
    }
    
    // Set up the debugging.
    debug = Logger.getLogger(this.getClass().getName() + "." + storeID);
    
    // Store reference to ep
    this.ep = ep;
  }

  /**
   * Get our instance name.
   *
   * @return The string representing the instance name of this store.
   */
  public String getName() {
    return storeID;
  }
  
  /**
   * Store the supplied event.
   *
   * @param e The event to be stored.
   * @return An object reference indicating success, or null.
   */
  public abstract Object storeEvent(EPEvent e);
  
  /**
   * Request an individual event given its (opaque) reference.
   *
   * @param ref The event reference.
   * @return The event in EPEvent form, or null if it doesn't exist.
   */
  public abstract EPEvent requestEvent(Object ref);
  
  /**
   * Request event(s).  Return a set of references to this event.  These
   * references are treated opaquely; they may be handed back via the
   * individual event request mechanism.  Return an empty Object[] if there
   * are no matches (do NOT return null unless there is an error).
   *
   * @param t1 The lower timebound (inclusive).
   * @param t2 The upper timebound (inclusive).
   * @return An array of (possibly opaque) object references, null if error.
   */
  public abstract Object[] requestEvents(long t1, long t2);
  
  /**
   * Request all events from a given source.
   *
   * @param source The source of this event - matches the source in
   * the EPEvent.
   * @return An array of (possibly opaque) object references, empty array if
   * no match, and null if error.
   */
  public abstract Object[] requestEvents(String source);
  
  /**
   * Request events from a given source between the two timestamps.
   *
   * @param source The source of this event - matches the source in
   * the EPEvent.
   * @param t1 The lower timebound (inclusive).
   * @param t2 The upper timebound (inclusive).
   * @return An array of (possibly opaque) object references, empty array if
   * no match, and null if error.
   */
  public abstract Object[] requestEvents(String source, long t1, long t2);
  
  /**
   * Run.
   */
  public void run() {
    // Do nothing by default
  }
  
  /**
   * Handle shutdown.
   *
   * @return True usually, false if you can't shutdown for some reason.
   */
  public boolean shutdown() {
    shutdown = true;
    // Do nothing by default
    return true;
  }
}