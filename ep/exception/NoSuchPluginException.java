package psl.xues.ep.exception;

/**
 * No-such-plugin exception.
 * <p>
 * Copyright (c) 2000-2002: The Trustees of Columbia University and the
 * City of New York.  All Rights Reserved.
 *
 * @author Janak J Parekh
 * @version $Revision$
 */
public class NoSuchPluginException extends Exception {
  public NoSuchPluginException() {
    super("No such plugin exists");
  }
}
