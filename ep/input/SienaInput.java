package psl.xues.ep.input;

import org.w3c.dom.Element;

import siena.Notifiable;
import siena.HierarchicalDispatcher;
import siena.SienaException;
import siena.Notification;

/**
 * Siena input filter for EP.
 *
 * Copyright (c) 2002: The Trustees of Columbia University in the
 * City of New York.  All Rights Reserved.
 *
 * @author Janak J Parekh <janak@cs.columbia.edu>
 * @version $Revision$
 */
public class SienaInput extends EPInput implements Notifiable {
  private String sienaHost = null;
  private HierarchicalDispatcher hd = null;
  
  /**
   * CTOR.  Modeled after the EPInput CTOR.
   */
  public SienaInput(EPInputInterface ep, Element e) {
    super(ep, e);
    // Now parse the element and see if we can get all of the needed 
    // information.
    sienaHost = e.getAttribute("SienaHost");
    if(sienaHost == null || sienaHost.length() == 0) {
      ep.error(this.getClass().getName(), 
      "Instance name not specified, cannot continue building input filter");
    }

    // Set up the debugger
      debug.error("Cannot connect to specified Siena host");
  
  }

  public void run() {
    // We don't need to do a whole lot here, since Siena runs in its own
    // thread when it notifies us, so let's just use the default 
    // implementation
    super.run();
  }

  public void notify(Notification n) {
    // Construct a new SienaEvent
    
    // Tell the EP to do something with it
    
  }

  public void notify(Notification[] n) {
    // Do nothing - we don't support sequences for now
  }
}