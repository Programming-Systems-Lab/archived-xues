package psl.xues.ep.output;

import org.w3c.dom.Element;
import org.apache.log4j.Category;

import psl.xues.ep.EventPackager;
import psl.xues.ep.event.EPEvent;

/**
 * Output mechanism for EP.  Extend this class if you want to output stuff
 * in a new format.
 *
 * TODO:
 * - Instead of returning success for handleEvent, support absorption?
 *
 * Copyright (c) 2002: The Trustees of Columbia University in the
 * City of New York.  All Rights Reserved.
 *
 * @author Janak J Parekh <janak@cs.columbia.edu>
 * @version $Revision$
 */
public abstract class EPOutput implements Runnable {
  /** Instance name */
  protected String outputID = null;
  /** Logger */
  protected Category debug = null;
  /** The thread context of this object */
  protected Thread myThread = null;
  /** Are we in shutdown? */
  protected boolean shutdown = false;
  
  /**
   * CTOR.  You are instantiated by the Event Packager and given the XML DOM
   * Element that corresponds to your configuration.
   *
   * @param e The element containing useful configuration information.
   * @exception InstantiationException
   */
  public EPOutput(Element el) throws InstantiationException {
    // Attempt to identify our instance name, which we call sourceID
    this.outputID = el.getAttribute("Name");
    if(outputID == null || outputID.length() == 0) {
      throw new InstantiationException("No sourceID specified for outputter");
    }
    
    // Set up the debugging.  We need the type for this as well.
    debug = Category.getInstance(this.getClass() + "." + outputID);
  }
  
  /**
   * Get this output's name.
   *
   * @return The string representing the instance name of this output.
   */
  public String getName() {
    return outputID;
  }
  
  /**
   * Handle an event handed to you.  All EPOutputters must implement this.
   * If you need to do something over a long period of time, consider 
   * doing it in your own thread context instead.
   *
   * @param epe The EPEvent
   * @return A boolean indicating success (if you don't know, return true).
   */
  public abstract boolean handleEvent(EPEvent epe);
  
  /**
   * Default run implementation - basically, do nothing.  Change/override this
   * if you need to do some synchronous output - it's better not to hang up 
   * the Event Packager, but rather queue the events handed to you.
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
}