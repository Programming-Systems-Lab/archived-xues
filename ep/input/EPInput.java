package psl.xues.ep.input;

/**
 * Extend this class to have your very own input mechanism.
 *
 * Copyright (c) 2002: The Trustees of Columbia University in the
 * City of New York.  All Rights Reserved.
 *
 * @author Janak J Parekh <janak@cs.columbia.edu>
 * @version $Revision$
 */
public abstract class EPInput {
  protected EPInputInterface ep = null;
 
  /**
   * CTOR.  You are instantiated by the Event Packager and given an interface
   * with which you can inject events (and perform other miscellaneous tasks)
   * into the Event Packager.
   * 
   * Use a similar signature for your constructor.
   *
   * @param ep The EPInputInterface.
   */
  public EPInput(EPInputInterface ep) {
    this.ep = ep;
  }
}