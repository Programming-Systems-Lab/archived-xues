package psl.xues.util;

import java.lang.reflect.InvocationTargetException;

/**
 * XUES utility class.
 * <p>
 * Copyright (c) 2002: The Trustees of Columbia University in the
 * City of New York.  All Rights Reserved.
 *
 * @author Janak J Parekh <janak@cs.columbia.edu>
 * @version $Revision$
 */
public abstract class XuesUtil {
  /**
   * Parse an exception to see if it's one of several nested exceptions,
   * in which case we return the root cause - that's all we care for.
   * In particular, we handle InvocationTargetExceptions and return
   * the ACTUAL cause, since we don't really care about the highest-level
   * exception.
   *
   * @param e The exception to parse and drill down if necessary
   * @return The appropriate exception (either the original, or the underlying
   * cause exception).
   */
  public static Exception parseException(Exception e) {
    if(e instanceof InvocationTargetException) {
      // Pay dirt
      Throwable newExc = ((InvocationTargetException)e).getCause();
      if(newExc != null && (newExc instanceof Exception)) {
        return (Exception)newExc;
      }
      /* If it's an Error, we don't really want to unwrap it, just send
       * the whole kit and kaboodle back. */
    }
    
    // No, nothing special, just return the original exception.
    return e;
  }
}