package psl.xues.ep.output;

import java.net.InetAddress;
import org.w3c.dom.Element;

import psl.xues.ep.event.EPEvent;
import psl.xues.ep.util.EPConst;

/**
 * Socket output for EP.
 * <p>
 * Required attributes:<ol>
 * <li><b>Host</b>: specify the host to connect to</li>
 * <li><b>Port</b>: specify the port to connect to</li>
 * <li><b>SocketType</b>: specify socket type (optional)</li>
 * <li><b>DataType</b>: specify structure of events to be sent out</li>
 * </ol>
 * <p>
 * Socket types currently supported:<ol>
 * <li><b>tcpstream</b>: Stream of data over one TCP connection (default)</li>
 * <li><b>tcpwrap</b>: TCP output segmented by an out-of-band character;
 * use the psl.xues.ep.util.WrappedInputStream to read from this
 * connection.</li>
 * <li><b>tcpconn</b>: One message per TCP connection.</li>
 * <li><b>udp</b>: UDP packets, one message per packet.</li>
 * </ol>
 * <p>
 * Data types currently supported:<ol>
 * <li>Opaquely-treated serialized Java objects (via type
 * <b>JavaObject</b>)</li>.  Note that, while this is technically supported
 * under UDP, TCP is strongly recommended to support large objects.</li>
 * <li>String output (via type <b>StringObject</b>).</li>
 * <li>XML plaintext output (via type <b>XMLObject</b>).
 * Note that for TCP, only one XML document is sent per connection.</li>
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
 */
public class SocketOutput extends EPOutput implements EPConst {
  /** Server */
  private String host = null;
  /** Port */
  private int port = null;
  /** Data type */
  private short dataType = -1;
  /** Socket type */
  private short socketType = -1;
  
  /** Client socket for TCP connections */
  private Socket s = null;
  /** Datagram socket for UDP connections */
  private DatagramSocket t = null;
  /** Output "stream" */
  private Object sout = null;
  
  /**
   * CTOR.
   */
  public SocketOutput(EPOutputInterface epo, Element el)
  throws InstantiationException{
    super(epo,el);
    
    // Get important attributes
    host = el.getAttribute("Host");
    if(host == null || host.length() == 0) {
      throw new InstantiationException("No host specified");
      try {
        port = Integer.parseInt(el.getAttribute("Port"));
      } catch(Exception e) {
        throw new InstantiationException("No port or invalid port specified");
      }
    }
    
    // TCP or UDP?
    String socketType = el.getAttribute("SocketType");
    if(socketType == null || socketType.length() == 0) {
      debug.info("Socket type not specified, assuming TCP stream");
      this.socketType = TCPSTREAM;
    }
    else if(socketType.equalsIgnoreCase("tcpstream"))
      this.socketType = TCPSTREAM;
    else if(socketType.equalsIgnoreCase("tcpconn"))
      this.socketType = TCPCONN;
    else if(socketType.equalsIgnoreCase("tcpwrap"))
      this.socketype = TCPWRAP;
    else if(socketType.equalsIgnoreCase("udp"))
      this.socketType = UDP;
    else throw new InstantiationException("Invalid socket type specified");
    
    // Determine data type
    String dataType = el.getAttribute("DataType");
    if(dataType == null || dataType.length() == 0) {
      debug.warn("Type not specified, assuming \"StringObject\"");
      this.dataType = STRING_OBJECT;
    } else { // validate type of object
      if(dataType.equalsIgnoreCase("JavaObject")) {
        this.dataType = JAVA_OBJECT;
      } else if(dataType.equalsIgnoreCase("StringObject")) {
        this.dataType = STRING_OBJECT;
      } else if(dataType.equalsIgnoreCase("XMLObject")) {
        this.dataType = XML_OBJECT;
      } else {
        throw new InstantiationException("Invalid data type specified");
      }
    }
    
    // Now try to set up our socket (for TCP KA)
    if(this.socketType == TCPSTREAM || this.socketType == TCPWRAP) try {
      s = new Socket(this.host, this,port);
      if(this.socketType == TCPSTREAM) {
        sout = new ObjectOutputStream(s.getOutputStream());
      }
    } catch(Exception e) {
      debug.error("Could not establish TCP socket", e);
    }
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
    if(socketType == TCPCONN) try { // Make the connection now
      s = new Socket(host, port);
    } catch(Exception e) {
      debug.error("Could not connect", s);
      return false;
    }
    
    if(dataType == JAVA_OBJECT) {
      if(socketType == TCP
      
      
      
      return false;
    }
  }
}