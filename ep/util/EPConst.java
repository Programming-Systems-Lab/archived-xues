package psl.xues.ep.util;

/**
 * Useful constants for Event Packager classes.
 *
 * @author Janak J Parekh
 * @version $Revision$
 */
public interface EPConst {
  /** TCP stream */
  public static final short TCPSTREAM = 1;
  /** TCP wrapped stream */
  public static final short TCPWRAP = 2;
  /** TCP message-per-connection */
  public static final short TCPCONN = 3;
  /** TCP bitmask */
  public static final short TCP = 3;
  /** UDP socket */
  public static final short UDP = 4;
  /** Max size of UDP packet */
  public static final int MAX_SIZE_UDP = 65507;
  // Data types for socket (and other?) output
  /** Specifies a Java object-based data type. */
  public static final short JAVA_OBJECT = 1;
  /** Specifies a String object-based data type. */
  public static final short STRING_OBJECT = 2;
  /** Specifies a XML object-based data type */
  public static final short XML_OBJECT = 3;
}
