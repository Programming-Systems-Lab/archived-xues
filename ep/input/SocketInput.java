package psl.xues.ep.input;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.net.DatagramSocket;
import java.util.Iterator;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.io.ByteArrayInputStream;
import javax.xml.parsers.DocumentBuilder;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import psl.xues.ep.event.DOMEvent;
import psl.xues.ep.event.EPEvent;
import psl.xues.ep.event.SienaEvent;
import psl.xues.ep.event.StringEvent;
import psl.xues.ep.util.EPConst;
import psl.xues.ep.util.WrappedInputStream;
import psl.xues.util.JAXPUtil;

import siena.Notification;


/**
 * Socket event input mechanism.  Allows EP to be a server which receives
 * socket input.  We currently allocate one thread per socket.
 * <p>
 * Usage: <em>([] implies an optional parameter)</em></p>
 * <p><tt>
 * &lt;Inputs&gt;<br>
 * <blockquote>&lt;Inputter Name="<em>instance name</em>" 
 * Type="psl.xues.ep.input.SocketInput" Port="<em>port number</em>"
 * [SocketType="<em>socket type</em>"] DataType="<em>datatype</em>" /&gt;<br>
 * </blockquote>
 * &lt;/Inputs&gt;
 * </tt></p>
 * <p>
 * Required attributes:<ol>
 * <li><b>Port</b>: specify the port to listen on for client connections</li>
 * <li><b>SocketType</b>: specify socket type (optional)</li>
 * <li><b>DataType</b>: specify structure of events to listen for</li>
 * </ol>
 * <p>
 * Socket types currently supported:<ol>
 * <li><b>tcpstream</b>: Stream of data over one TCP connection (default;
 * <i>not supported for XML</i>)</li>
 * <li><b>tcpwrap</b>: TCP output segmented by an out-of-band character;
 * use the psl.xues.ep.util.WrappedOutputStream to write to this
 * connection.</li>
 * <li><b>tcpconn</b>: One message per TCP connection.</li>
 * <li><b>udp</b>: UDP packets, one message per packet.</li>
 * </ol>
 * <p>
 * Data types currently supported:<ol>
 * <li>Serialized Java objects (via type <b>JavaObject</b>)</li>.  Note
 * that, while this is technically supported under UDP, TCP is strongly
 * recommended to support large objects.
 * <p>The following Java object types are currently supported:
 * <ol>
 *  <li>psl.xues.ep.event.EPEvent: the serialized EPEvent is automatically
 *      used.</li>
 *  <li>siena.Notification: a SienaEvent is automatically created.</li>
 *  <li>org.w3c.dom.Document: a DOMEvent is automatically created.</li>
 *  <li>org.w3c.dom.Element: a DOMEvent is automatically created.</li>
 * </ol></li>
 * <li>String input (via type <b>StringObject</b>).  Note that String input
 * is newline-delimited for tcpstream.</li>
 * <li>XML plaintext input (via type <b>XMLObject</b>).
 * Note that for TCP, only tcpwrap and tcpconn modes are supported.  Gets
 * parsed and inserted as a DOMEvent.</li>
 * </ol>
 * <p>
 * Copyright (c) 2002: The Trustees of Columbia University in the
 * City of New York.  All Rights Reserved.
 *
 * <!--
 * TODO:
 * - Rename input types to correspond to event types
 * - Support simple XML Siena representations in addition to serialized Java
 * - Consider using NBIO instead for lots of clients
 * - Support non-serialized for non-Java-specific objects
 * - Make XML parsing more bulletproof
 * -->
 *
 * @author Janak J Parekh
 * @version $Revision$
 */
public class SocketInput extends EPInput implements EPConst {
  /** Server socket port */
  private short port = -1;
  /** Data type */
  private short dataType = -1;
  /** Socket type */
  private short socketType = -1;
  
  /** Server socket for TCP connections */
  private ServerSocket ss = null;
  /** Datagram socket for UDP connections */
  private DatagramSocket ds = null;
  /** Our client sockets.  (For UDP, there's always exactly one entry in
   *  this table.) */
  private HashMap clientSockets = null;
  
  /**
   * CTOR.
   */
  public SocketInput(EPInputInterface ep, Element el)
  throws InstantiationException {
    super(ep,el);
    
    // Get the basic listening socket parameters
    try {
      this.port = Short.parseShort(el.getAttribute("Port"));
    } catch(Exception e) {
      throw new InstantiationException("Port value invalid or not specified");
    }
    
    // TCP or UDP?
    String socketType = el.getAttribute("SocketType");
    if(socketType == null || socketType.length() == 0) {
      debug.info("Socket type not specified, assuming TCP stream");
      this.socketType = TCPSTREAM;
    }
    else if(socketType.equalsIgnoreCase("tcpstream"))
      this.socketType = TCPSTREAM;
    else if(socketType.equalsIgnoreCase("tcpwrap"))
      this.socketType = TCPWRAP;
    else if(socketType.equalsIgnoreCase("tcpconn"))
      this.socketType = TCPCONN;
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
    
    // Sanity checks for socket/data combinations
    if(this.socketType == TCPSTREAM && this.dataType == XML_OBJECT)
      throw new InstantiationException("Can't stream multiple XML messages "+
      "without wrapping");
    
    // Now build our socket
    if((this.socketType & TCP) != 0) {
      try {
        ss = new ServerSocket(this.port);
      } catch(Exception e) {
        ss = null;
        throw new InstantiationException("Could not establish TCP socket: "+e);
      }
    } else { // UDP
      try {
        ds = new DatagramSocket(this.port);
      } catch(Exception e) {
        ds = null;
        throw new InstantiationException("Could not establish UDP socket: "+e);
      }
    }
    
    // We're ready to run
    clientSockets = new HashMap();
  }
  
  /**
   * Run.  For TCP, we listen to the server socket, and hand client connections
   * to a handler.  For UDP, we just hand the datagram socket to the handler.
   */
  public void run() {
    if((socketType & TCP) != 0) {
      Socket cs = null; // Client socket
      
      while(!shutdown) {
        try {
          cs = ss.accept();
        } catch(Exception e) {
          if(!shutdown) {
            debug.error("Could not accept connection, shutting down inputter", e);
            shutdown();
          }
          return;
        }
        
        // Store a reference to cs, and then hand it to a client.  XXX -
        // does hashing of sockets like this work?
        try {
          ClientThread ct = new ClientThread(cs, socketType, dataType);
          clientSockets.put(cs, ct);
          new Thread(ct).start(); // Start it up
        } catch(InstantiationException e) {
          debug.error("Could not instantiate client", e);
          continue;
        }
      }
    } else { // UDP
      try {
        ClientThread ct = new ClientThread(ds, dataType);
        clientSockets.put(ds, ct);
        ct.run();  /* We don't need a separate thread for the client handler,
         * since there's only one socket connection */
      } catch(InstantiationException e) {
        debug.error("Could not instantiate client", e);
        return;
      }
    }
  }
  
  /**
   * Return our type.
   */
  public String getType() { return "SocketInput"; }
  
  /**
   * Handle an object that comes from a client thread.
   *
   * @param o The object that the client thread is handing to us.
   * @return A boolean indicating success.
   */
  boolean handleObject(Object o) {
    EPEvent ret = null;
    // Explicitly handle the different object types here.  If a recognized
    // object type, wrap it up in an EPEvent and give it to our InputHandler.
    // If it's an EPEvent, just take it and hand it over without any wrapping.
    if(o instanceof EPEvent) {
      ret = (EPEvent)o;
    } else if(o instanceof Notification) {
      ret = new SienaEvent(getName(), (Notification)o);
    } else if(o instanceof Element) {
      ret = new DOMEvent(getName(), (Element)o);
    } else if(o instanceof Document) {
      ret = new DOMEvent(getName(), (Document)o);
    } else if(o instanceof String) {
      ret = new StringEvent(getName(), (String)o);
    } else {
      // No clue what to do with this
      debug.warn("Received invalid object from socket connection, ignoring");
      return false;
    }
    
    ep.injectEvent(ret);
    return true;
  }
  
  /**
   * Handle a shutdown request.
   */
  public void shutdown() {
    super.shutdown();
    // Close all client socket threads
    synchronized(clientSockets) {
      Iterator i = clientSockets.values().iterator();
      while(i.hasNext()) {
        ((ClientThread)i.next()).shutdown();
      }
    }
    /* Now shut down the "server sockets". XXX - through the current setup, the
     * datagram socket will close twice.  Obviously, it'll fail the second time,
     * but we should try and eliminate that redundancy */
    if(ss != null) try { ss.close(); } catch(Exception e) { ; }
    if(ds != null) try { ds.close(); } catch(Exception e) { ; }
    debug.debug("SocketInput shutdown complete");
  }
  
  /**
   * Thread to handle a client connection.
   *
   * @author Janak J Parekh <janak@cs.columbia.edu>
   * @version $Revision$
   */
  class ClientThread implements Runnable {
    /** Reference to TCP client socket */
    private Socket cs = null;
    /** Reference to UDP datagram socket */
    private DatagramSocket ds = null;
    /** Are we in shutdown? */
    private boolean shutdown = false;
    /** Structure (either stream or reader) to handle client input */
    private Object cin = null;
    /** The socket type */
    private short socketType = -1;
    /** Data type */
    private short dataType = -1;
    /** UDP packet buffer */
    private byte[] udpbuf = null;
    /** UDP packet valid data length */
    private int udpbuflen = -1;
    /** XML parser, if we need it */
    private DocumentBuilder db = null;
    
    /**
     * CTOR for TCP connection.
     *
     * @param cs The client socket connection.
     * @param type The datatype.
     */
    public ClientThread(Socket cs, short socketType, short dataType)
    throws InstantiationException {
      this.cs = cs;
      this.socketType = socketType;
      this.dataType = dataType;
      
      // Set up the streams
      if(dataType == JAVA_OBJECT) {
        if(socketType == TCPSTREAM || socketType == TCPCONN) try {
          cin = new ObjectInputStream(cs.getInputStream());
        } catch(Exception e) {
          throw new InstantiationException("Could not establish " +
          "ObjectInputStream: " + e);
        }
      }
      
      else if(dataType == STRING_OBJECT) {
        if(socketType == TCPSTREAM || socketType == TCPCONN) try {
          cin = new BufferedReader(new InputStreamReader(cs.getInputStream()));
        } catch(Exception e) {
          throw new InstantiationException("Could not establish " +
          "BufferedReader: " + e);
        }
      }
      
      else if(dataType == XML_OBJECT) {
        if(socketType == TCPSTREAM || socketType == TCPCONN) try {
          cin = cs.getInputStream();
        } catch(Exception e) {
          throw new InstantiationException("Could not get inputStream: " +e);
        }
        
        // Now build the parser
        db = JAXPUtil.newDocumentBuilder();
        if(db == null) {
          throw new InstantiationException("Could not set up XML parser");
        }
      }
      
      else {
        throw new InstantiationException("Unhandled type in ClientThread");
      }
    }
    
    /**
     * CTOR for UDP connection.
     *
     * @param ds The datagram socket connection
     * @param type The datatype.
     */
    public ClientThread(DatagramSocket ds, short dataType) throws
    InstantiationException {
      this.ds = ds;
      this.socketType = UDP;
      this.dataType = dataType;
      
      if(dataType == XML_OBJECT) {
        // Build the parser
        db = JAXPUtil.newDocumentBuilder();
        if(db == null) {
          throw new InstantiationException("Could not set up XML parser");
        }
      }
      
      // Set up the UDP packet buffer
      udpbuf = new byte[MAX_SIZE_UDP];
    }
    
    /**
     * Run.  Receive entities and hand them to our parent.  Yes, this might
     * eventually need some optimization.  Yes, this code is ugly.
     */
    public void run() {
      Object input = null;
      while(!shutdown) {
        // Common UDP handling: first read the datagram
        if(socketType == UDP) {
          DatagramPacket dp = new DatagramPacket(udpbuf, MAX_SIZE_UDP);
          try {
            ds.receive(dp);
            udpbuflen = dp.getLength();
          } catch(Exception e) {
            debug.error("Could not read datagram, closing down client thread",e);
            shutdown();
            return;
          }
        }
        // End common UDP handling
        
        // Now differentiate based on datatype
        if(dataType == JAVA_OBJECT) { //////// JAVA_OBJECT ////////
          // UDP: create a stream around our udpbuf
          if(socketType == UDP) {
            try {
              cin = new ObjectInputStream(new ByteArrayInputStream(udpbuf, 0, udpbuflen));
            } catch(Exception e) {
              debug.error("Could not create ObjectInputStream for UDP buffer, "
              + "closing down client thread", e);
              shutdown();
              return;
            }
          }
          // TCPWRAP: create a new WrappedInputStream
          else if(socketType == TCPWRAP) try {
            cin = new ObjectInputStream(new WrappedInputStream(cs.getInputStream()));
          } catch(Exception e) {
            debug.error("Could not create WrappedInputStream, closing down " +
            "client thread", e);
            shutdown();
            return;
          }
          // TCPSTREAM and TCPCONN need no special handling
          
          // Read using a objectInputStream, and hand the resulting
          // object to the SocketInput mechanism.
          try {
            input = ((ObjectInputStream)cin).readObject();
          } catch(Exception e) {
            debug.error("Could not read object, closing down client thread", e);
            shutdown();
            return;
          }
        }
        
        
        else if(dataType == STRING_OBJECT) try { //////// STRING_OBJECT ////////
          // UDP: grab the packet as one string
          if(socketType == UDP) {
            input = new String(udpbuf, 0, udpbuflen);
          }
          // TCPWRAP/TCPCONN: grab the entire InputStream as one string
          else if(socketType == TCPWRAP || socketType == TCPCONN) {
            // Set up the stream based on WRAP or CONN first
            if(socketType == TCPWRAP) {
              try {
                cin = new BufferedReader(new InputStreamReader(new
                WrappedInputStream(cs.getInputStream())));
              } catch(Exception e) {
                debug.warn("Could not create WrappedInputStream, closing client socket");
                shutdown();
                return;
              }
            }
            else //(socketType == TCPCONN)
              cin = new BufferedReader(new
              InputStreamReader(cs.getInputStream()));
            // Processing common to both TCP types
            String temp = null;
            StringBuffer tempBuffer = new StringBuffer();
            while(((BufferedReader)cin).ready()) {
              temp = ((BufferedReader)cin).readLine();
              if(temp == null) break;
              // Keep on reading and appending to our buffer
              else tempBuffer.append(temp).append('\n');
            }
            input = tempBuffer.toString();
          }
          // TCPSTREAM: grab exactly one line
          else {
            input = ((BufferedReader)cin).readLine();
          }
        } catch(Exception e) {
          debug.error("Could not read string, closing down client thread", e);
          shutdown();
          return;
        }
        
        
        else if(dataType == XML_OBJECT) { //////// XML_OBJECT ////////
          // UDP: just create a ByteArrayInputStream
          if(socketType == UDP) {
            try {
              cin = new ByteArrayInputStream(udpbuf, 0, udpbuflen);
            } catch(Exception e) {
              debug.error("Could not create ObjectInputStream for UDP buffer, "
              + "closing down client thread", e);
              shutdown();
              return;
            }
          }
          // TCPWRAP: use WrappedInputStream
          else if(socketType == TCPWRAP) {
            try {
              cin = new WrappedInputStream(cs.getInputStream());
            } catch(Exception e) {
              debug.warn("Could not create WrappedInputStream, " +
              "closing down client thread", e);
              shutdown();
              return;
            }
          }
          // TCPSTREAM: not supported; TCPCONN: no presetup needed
          
          // Now hand cin to the parser
          try {
            input = db.parse((InputStream)cin);
          } catch(Exception e) {
            debug.error("Could not parse incoming document", e);
            shutdown();
            return;
          }
          // If TCPCONN, we're done with this socket connection... TCPSTREAM
          // here for paranoia's sake
          if(socketType == TCPCONN || socketType == TCPSTREAM) shutdown();
          
          
        } else { /////////////////// UNHANDLED_TYPE ////////////////////////
          // We should NOT get here
          debug.error("Unhandled type in client thread run(), " +
          "shutting thread down");
          shutdown();
          return;
        }
        
        // Now hand the object to our parent
        if(!handleObject(input)) {
          // Problem
          debug.error("Invalid object received, closing down client thread");
          shutdown();
          return;
        }
      }
      // If we've gotten here, we're in shutdown
      return;
    }
    
    /**
     * Shutdown.  XXX - not clear if this will work correctly.
     */
    public void shutdown() {
      if(shutdown) return;  // Don't bother
      
      shutdown = true;
      try {
        if(cs != null) {
          cs.close();
          cs = null;
        }
        if(ds != null) {
          ds.close();
          ds = null;
        }
      } catch(Exception e) { }
      cs = null; ds = null;
    }
  }
}
