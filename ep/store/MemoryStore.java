package psl.xues.ep.store;

import java.util.HashMap;
import java.util.TreeMap;
import org.w3c.dom.Element;
import psl.xues.ep.event.EPEvent;

/**
 * Simple implementation of an EP store to store events in memory.
 * Fast, and useful for testing.
 * <p>
 * Copyright (c) 2002: The Trustees of Columbia University in the
 * City of New York.  All Rights Reserved.
 *
 * <!--
 * TODO: 
 * - Support store-to-file (is it possible, actually, to support generalized
 *   serialization?)
 * -->
 *
 * @author Janak J Parekh (janak@cs.columbia.edu)
 * @version $Revision$
 */
public class MemoryStore extends EPStore {
  /**
   * TreeMap of the events sorted by time.
   */
  private TreeMap eventsByTime = new TreeMap();
   
  /**
   * HashMap of the events by source.  Each entry associated with an owner
   * is actually a TreeMap of events mapped by time.
   */
  private HashMap eventsBySource = new HashMap();
  
  /**
   * File to write out data structures to.
   */
  private String storeFile = null;
  
  /**
   * CTOR.
   */
  public MemoryStore(EPStoreInterface ep, Element el) 
  throws InstantiationException {
    super(ep,el);
    
    // Saved file?
    //storeFile = el.getAttribute("StoreFile");
    //if(storeFile != null) {
      // Try to restore it
  }
  
  /** 
   * Request an individual event given its (opaque) reference.
   *
   * @param ref The event reference.
   * @return The event in EPEvent form, or null if it doesn't exist.
   */
  public EPEvent requestEvent(Object ref) {
    if(!(ref instanceof EPEvent)) { // BAD!
      debug.warn("Was handed invalid reference");
      return null;
    }
    return (EPEvent)ref;
  }  
  
  /** 
   * Request event(s).  Return a set of references to this event.
   *
   * @param t1 The lower timebound (inclusive).
   * @param t2 The upper timebound (inclusive).
   * @return An array of (possibly opaque) object references, null if error.
   */
  public Object[] requestEvents(long t1, long t2) {
    // Here's a particularly nifty piece of code
    return eventsByTime.subMap(new Long(t1), new Long(t2)).values().toArray();
  }
  
  /** 
   * Store the supplied event.
   *
   * @param e The event to be stored.
   * @return An object reference indicating success, or null.
   */
  public Object storeEvent(EPEvent e) {
    // Put it in the time map
    eventsByTime.put(new Long(e.getTimestamp()), e);
    // Put it in the owner map
    TreeMap t = (TreeMap)eventsBySource.get(e.getSource());
    if(t == null) { // No such map, create one
      t = new TreeMap();
      eventsBySource.put(e.getSource(), t);
    }
    t.put(new Long(e.getTimestamp()), e); // Actually put it in the tree
    return e;
  }
  
  /** 
   * Request all events from a given source.
   *
   * @param source The source of this event - matches the source in
   * the EPEvent.
   * @return An array of (possibly opaque) object references, empty array if
   * no match, and null if error.
   */
  public Object[] requestEvents(String source) {
    // Grab the TreeMap from the sourceMap
    TreeMap t = (TreeMap)eventsBySource.get(source);
    if(t == null) return null;
    else return t.values().toArray();
  }
  
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
  public Object[] requestEvents(String source, long t1, long t2) {
    // Grab the TreeMap from the sourceMap
    TreeMap t = (TreeMap)eventsBySource.get(source);
    if(t == null) return null;
    else return t.subMap(new Long(t1), new Long(t2)).values().toArray();
  }

  /**
   * Get the type.
   */
  public String getType() {
    return "MemoryStore";
  }

}
