package psl.xues.ep.input;

import java.util.HashMap;
import org.apache.log4j.Category;
import org.w3c.dom.Element;
import psl.xues.ep.EventPackager;
import psl.xues.ep.EPRule;

/**
 * Extend this class to have your very own input mechanism.
 *
 * Copyright (c) 2002: The Trustees of Columbia University in the
 * City of New York.  All Rights Reserved.
 *
 * @author Janak J Parekh <janak@cs.columbia.edu>
 * @version $Revision$
 */
public abstract class EPInput implements Runnable {
  /** Reference to the EP inputting interface (primarily for callbacks */
  protected EPInputInterface ep = null;
  /** The name of this instance (the "source") */
  protected String sourceID = null;
  /** Our logger */
  protected Category debug = null;
  /** Are we in shutdown? */
  protected boolean shutdown = false;
  /** The thread context of this object */
  protected Thread myThread = null;
  /** The element responsible for this input format */
  protected Element defnElem = null;
  /** Associated runtime rules */
  protected HashMap runtimeRules = new HashMap();
  
  /**
   * CTOR.  You are instantiated by the Event Packager and given an interface
   * with which you can inject events (and perform other miscellaneous tasks)
   * into the Event Packager.
   *
   * Use a similar signature for your constructor.
   *
   * @param ep The EPInputInterface.
   * @param e The DOM element associated with this input, may contain
   * useful information.
   * @param sourceID The unique sourceID assigned to you by the configuration
   * file.
   * @exception InstantiationException may get thrown by a child.  We don't
   * but it's here anyway just to be explicit.
   */
  public EPInput(EPInputInterface ep, Element el) 
  throws InstantiationException {
    // Attempt to identify our instance name, which we call sourceID
    this.sourceID = el.getAttribute("Name");
    if(sourceID == null || sourceID.length() == 0) {
      throw new InstantiationException("No sourceID specified for inputter");
    }
    
    this.ep = ep;
    this.defnElem = el;
    
    // Set up the debugging.  We need the type for this as well.
    debug = Category.getInstance(this.getClass().getName() + "." + sourceID);
  }
  
  /**
   * Get this input's name.
   *
   * @return The string representing the instance name of this input.
   */
  public String getName() {
    return sourceID;
  }
  
  /**
   * Default run implementation - basically, do nothing.  Change/override this 
   * only if you need to do some form of polling or listening for your input -
   * and if you do, consider adding shutdown functionality.
   */
  public void run() {
    // Store a reference to my current thread so it can be interrupted from
    // another thread context
    myThread = Thread.currentThread();
    
    try {
      while(!shutdown) {
        Thread.currentThread().sleep(1000);
      }
    } catch(InterruptedException ie) { ; }
  }
  
  /**
   * Called when you need to shut down.
   */
  public void shutdown() {
    shutdown = true;
    if(myThread != null) {
      myThread.interrupt();
    } else {
      debug.warn("Can't interrupt thread, may not shutdown");
    }
  }
 
  /**
   * Add a rule reference to us.
   */
  public void addRule(EPRule r) {
    runtimeRules.put(r.getName(), r);
  }

  /**
   * Get the "type" of input.  You have to implement this.
   *
   * @return The type, as String.  Usually, it should be the class name without
   * the package identification.
   */
  public abstract String getType();
}
