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
public class NoSuchRuleException extends Exception {
  public NoSuchRuleException() {
    super("No such rule exists");
  }
}
