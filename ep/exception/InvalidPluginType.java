package psl.xues.ep.exception;

/**
 * Invalid plugin type exception.
 * <p>
 * Copyright (c) 2000-2002: The Trustees of Columbia University and the
 * City of New York.  All Rights Reserved.
 *
 * @author Janak J Parekh
 * @version $Revision$
 */
public class InvalidPluginType extends RuntimeException {
  public InvalidPluginType() {
    super("Invalid plugin type supplied");
  }
}