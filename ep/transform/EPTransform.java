package psl.xues.ep.transform;

import java.util.HashMap;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;

import psl.xues.ep.EPPlugin;
import psl.xues.ep.EPRule;
import psl.xues.ep.event.EPEvent;

/**
 * Abstract class for transforms.  Extend this if you want to implement
 * your own.
 * <p>
 * Copyright (c) 2002: The Trustees of Columbia University in the
 * City of New York.  All Rights Reserved.
 *
 * <!--
 * TODO:
 * - Consider making a thread (but first figure out if there is any advantage
 *   to this, since transforms have implied orderings)
 * - If so (or otherwise), shutdown?
 * -->
 *
 * @author Janak J Parekh <janak@cs.columbia.edu>
 * @version $Revision$
 */
public abstract class EPTransform implements EPPlugin {
  /** Instance ID */
  protected String transformID = null;
  /** Debugger */
  protected Logger debug = null;
  /** EP reference */
  protected EPTransformInterface ep = null;
  /** Number of times this has "fired" */
  private long count = 0;
  /** List of associated rules -- not used except in deletion */
  private HashMap runtimeRules = new HashMap();

  /**
   * CTOR.  The element is provided for any special customizations on this
   * transform.
   *
   * @param el The element with initialization info for this transform.
   */
  public EPTransform(EPTransformInterface ep, Element el) 
  throws InstantiationException {
    // Attempt to obtain our transformID
    this.transformID = el.getAttribute("Name");
    if(transformID == null || transformID.length() == 0) {
      throw new InstantiationException("No transformID specified for " + 
      "transform");
    }
    
    // Initialize our debugger
    debug = Logger.getLogger(this.getClass().getName() + "." + 
    transformID);

    // Store event packager reference
    this.ep = ep;
  }
  
  /**
   * Get our instance name.
   *
   * @return The string representing the instance name of this transform.
   */
  public String getName() {
    return transformID;
  }
  
  /**
   * Get the type.  You must implement this.
   *
   * @return A String indicating type.
   */
  public abstract String getType();
    

  /**
   * Handle a transform request.  You must implement this to be of any use.
   * NOTE: Unless you have a good reason, copy the existing sourceID...
   * otherwise EP might confused.
   *
   * @param original The EPEvent that needs transformation.
   * @return The transformed EPEvent, or null if you can't handle the 
   * transform.
   */
  public abstract EPEvent transform(EPEvent original);

  /**
   * Mark this input as having been "fired".  Return the new count.
   *
   * @return The new count, as int.
   */
  public final long addCount() {
    return ++count;
  }
  
  /**
   * Get the number of times this has been "fired".
   * 
   * @return The new count, as int.
   */
  public final long getCount() {
    return count;
  }
  
  /**
   * Shutdown.  Currently, EP does nothing with EPTransform shutdowns, so
   * this can't be overriden.  This behavior may change in the future if
   * needed.
   */
  public final void shutdown() { return; }


  /**
   * Add a rule reference to us.
   */
  public void addRule(EPRule r) {
    synchronized(runtimeRules) {
      runtimeRules.put(r.getName(), r);
    }
  }
  
  /**
   * Get the rules as a snapshot.  Inefficient, but it doesn't leave the
   * structure locked.  XXX - more efficient way to do this?
   */
  public EPRule[] getCurrentRules() {
    EPRule[] ret = null;
    synchronized(runtimeRules) {
      ret = (EPRule[])runtimeRules.values().toArray(new EPRule[0]);
    }
    return ret;
  }

  /**
   * Remove a rule reference.
   *
   * @param name The name of the rule
   * @return A boolean indicating success.
   */
  public boolean removeRule(String ruleName) {
    boolean success = false;
    synchronized(runtimeRules) {
      success = (runtimeRules.remove(ruleName) == null ? false : true);
    }
    return success;
  }  
}
