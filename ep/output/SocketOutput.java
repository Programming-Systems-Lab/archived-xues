package psl.xues.ep.output;

import java.net.InetAddress;
import org.w3c.dom.Element;

import psl.xues.ep.event.EPEvent;
import psl.xues.ep.event.StringEvent;
import psl.xues.ep.event.DOMEvent;
import psl.xues.ep.util.EPConst;
import psl.xues.ep.util.WrappedOutputStream;
import psl.xues.util.JAXPUtil;

import java.lang.InstantiationException;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Socket;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.Transformer;
import javax.xml.transform.dom.DOMSource;

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
 * - Support serialized forms other than EPEvent
 * - Consider actually sending the events in an asynchronous thread
 * -->
 *
 * @author Janak J Parekh
 * @version $Revision$
 */
public class SocketOutput extends EPOutput implements EPConst {
  /** Server */
  private String host = null;
  /** Port */
  private int port = -1;
  /** Data type */
  private short dataType = -1;
  /** Socket type */
  private short socketType = -1;
  
  /** Client socket for TCP connections */
  private Socket s = null;
  /** Datagram socket for UDP connections */
  private DatagramSocket t = null;
  /** Output stream for TCP connections */
  private OutputStream sout = null;
  /** Transformer for XML output */
  private Transformer tr = null;
  
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
    }
    try {
      port = Integer.parseInt(el.getAttribute("Port"));
    } catch(Exception e) {
      throw new InstantiationException("No port or invalid port specified");
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
      this.socketType = TCPWRAP;
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
    
    // Now try to set up our socket
    if(this.socketType == TCPSTREAM || this.socketType == TCPWRAP) try {
      s = new Socket(this.host, this.port);
      if(this.socketType == TCPSTREAM) {
        sout = s.getOutputStream();
      }
    } catch(Exception e) {
      throw new InstantiationException("Could not establish TCP socket" + e);
    } else if(this.socketType == UDP) try {
      t = new DatagramSocket();
    } catch(Exception e) {
      throw new InstantiationException("Could not establish UDP socket" + e);
    }
    
    // Transformer?
    if(this.dataType == XML_OBJECT) {
      tr = JAXPUtil.newTransformer();
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
    // Handling common to all data types
    if(socketType == TCPCONN) try { // Make the connection now
      s = new Socket(host, port);
      sout = s.getOutputStream();
    } catch(Exception e) {
      return failHandle("Could not connect", e);
    }
    else if(socketType == TCPWRAP) try { // Wrap the stream now
      sout = new WrappedOutputStream(s.getOutputStream());
    } catch(Exception e) {
      return failHandle("Could not build WrappedOutputStream", e);
    }
    else if(socketType == UDP) {
      // XXX - ByteArrayOutputStream can dynamically grow.  Should we
      // do that instead of allocating a fixed size?  There's no max then,
      // so no upper bound, but maybe less memory-intensive.  Is there any
      // BAOS that can take an existing byte array?
      sout = new ByteArrayOutputStream(MAX_SIZE_UDP);
    }
    
    if(dataType == JAVA_OBJECT) try { ///////////// JAVA_OBJECT //////////////
      // Wrap an ObjectOutputStream around sout.
      // XXX - possible optimization - in TCPSTREAM, don't keep on wrapping
      // new ObjectOutputStreams every time.  For now, I'm not doing this to
      // keep things orthogonal, but it will likely hinder performance.
      ObjectOutputStream oos = new ObjectOutputStream(sout);
      oos.writeObject(epe);
      oos.close();
    } catch(Exception e) {
      return failHandle("Could not write object to output", e);
    }
    
    else if(dataType == STRING_OBJECT) try { /////// STRING_OBJECT //////
      EPEvent f = epe;
      if(!epe.getFormat().equals("StringEvent")) {
        f = epe.convertEvent("StringEvent");
        if(f == null) {
          return failHandle("Could not convert event to String format for output");
        }
      }
      PrintWriter pw = new PrintWriter(sout);
      pw.print(((StringEvent)f).getStringEvent());
      pw.close();
    } catch(Exception e) {
      return failHandle("Could not write string to output", e);
    }
    
    else if(dataType == XML_OBJECT) { ////// XML_OBJECT ///////
      EPEvent f = epe;
      if(!epe.getFormat().equals("DOMEvent")) {
        f = epe.convertEvent("DOMEvent");
        if(f == null) {
          return failHandle("Could not convert event to DOM format for output");
        }
      }
      // Now transform it into our OutputStream.  We could use DOMEvent's
      // ConvertEvent, but this is more efficient, as it gets the results
      // directly out
      DOMSource ds = new DOMSource(((DOMEvent)f).getDOMDocument());
      StreamResult sr = new StreamResult(sout);
      try {
        tr.transform(ds, sr);
      } catch(Exception e) {
        return failHandle("Could not output XML", e);
      }
    }
    
    // If UDP, get and send out the ByteArray
    if(socketType == UDP) try {
      byte[] data = ((ByteArrayOutputStream)sout).toByteArray();
      t.send(new DatagramPacket(data, data.length));
    } catch(Exception e) {
      return failHandle("Could not send UDP datagram packet", e);
    }
    
    // Wrap up the communication
    if(socketType == TCPCONN || socketType == TCPWRAP) try {
      sout.flush();
      sout.close();
      sout = null;
      if(socketType == TCPCONN) {
        s.close();
        s = null;
      }
    } catch(Exception e) {
      return failHandle("Could not clean up after send", e);
    }
    
    // We're actually done
    return true;
  }
  
  /**
   * Cleanup for failure cases.  See full version for parameter detail.
   */
  private boolean failHandle(String err) {
    return failHandle(err, null);
  }
  
  /**
   * Cleanup for failure cases.  Don't play with this unless you know what
   * you're doing.
   *
   * @param sout The stream to be (maybe) cleaned up.
   * @param err The error to be sent to the logger.
   * @param e The exception to be sent to the logger (can be null).
   * @return Always false, so it can be inlined in the caller's return
   * statement.
   */
  private boolean failHandle(String err, Exception e) {
    if(socketType == TCPCONN) try {
      if(sout != null) {
        sout.close();
        sout = null;
      }
      if(s != null) {
        s.close();
        s = null;
      }
    } catch(Exception ex) { ; }
    else if(socketType == TCPWRAP || socketType == UDP) try {
      if(sout != null) {
        sout.close();
        sout = null;
      }
    } catch(Exception ex) { ; }
    
    if(e != null) debug.error(err, e);
    else debug.error(err);
    
    return false;
  }
  
  /**
   * Handle shutdown.
   */
  public void shutdown() {
    shutdown = true;
    try {
      if(sout != null) sout.close();
      if(s != null) s.close();
      if(t != null) t.close();
      sout = null;
      s = null;
      t = null;
    } catch(Exception e) { ; }
  }
}