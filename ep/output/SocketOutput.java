package psl.xues.ep.output;

import org.w3c.dom.Element;

import psl.xues.ep.event.EPEvent;

/**
 * Socket output for EP.
 * <p>
 * Required attributes:<ol>
 * <li><b>Host</b>: specify the host to connect to</li>
 * <li><b>Port</b>: specify the port to connect to</li>
 * <li><b>SocketType</b>: specify socket type, either "tcp" or "udp" (tcp is
 * default)</li>
 * <li><b>DataType</b>: specify structure of events to be sent out</li>
 * </ol>
 *
 * Data types currently supported:<ol>
 * <li>Opaquely-treated serialized Java objects (via type <b>JavaObject</b>)</li>.  Note
 * that, while this is technically supported under UDP, TCP is strongly
 * recommended to support large objects.</li>
 * <li>String output (via type <b>StringObject</b>).  Note that String input
 * is currently line-delimited.</li>
 * <li>XML plaintext input (via type <b>XMLObject</b>).
 * Note that for TCP, only one XML document per connection is supported!  Gets
 * parsed and inserted as a DOMEvent.</li>
 * </ol>
 * <p>
 * Copyright (c) 2002: The Trustees of Columbia University in the
 * City of New York.  All Rights Reserved.
 *
 * <!--
 * TODO:
 * - Support simple XML Siena representations in addition to serialized Java
 * - Support non-serialized for non-Java-specific objects
 * -->
 *
 * @author Janak J Parekh
 * @version $Revision$
 *
 * @author Janak J Parekh
 * @version $Revision$
 */
public class SocketOutput extends EPOutput {
  /**
   * CTOR.
   */
  public SocketOutput(EPOutputInterface epo, Element el) 
  throws InstantiationException{
    super(epo,el);
    
  }
  
  /**
   * Get the plugin "type" as String.
   */
  public String getType() {
    return "SocketOutput";
  }
  
  /**
   * Output an event via the socket connection.
   *
   * @param epe The EPEvent
   * @return A boolean indicating success (if you don't know, return true).
   */
  public boolean handleEvent(EPEvent epe) {
    return false;
  }
}