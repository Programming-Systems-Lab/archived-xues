package psl.xues.ep;

/**
 * Simple class to deliver EventPackager's payload to other classes.
 * Why does this exist at all?  To handle metadata, like srcID.
 *
 * Copyright (c) 2000: The Trustees of Columbia University and the
 * City of New York.  All Rights Reserved.
 *
 * @author Janak J Parekh
 * @version $Revision$
 */
public class EPPayload {
  int srcID;
  Object payload;

  /**
   * CTOR.
   */
  public EPPayload(int srcID, Object payload) {
    this.srcID = srcID;
    this.payload = payload;
  }

  /** Get the srcID of this payload */
  public int getSrcID() { return srcID; }

  /** Get the payload */
  public Object getPayload() { return payload; }
}
