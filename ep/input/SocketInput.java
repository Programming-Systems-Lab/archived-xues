package psl.xues.ep.input;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import psl.xues.ep.event.DOMEvent;
import psl.xues.ep.event.EPEvent;
import psl.xues.ep.event.SienaEvent;
import psl.xues.ep.event.StringEvent;

import siena.Notification;
import java.net.DatagramSocket;

/**
 * Socket event input mechanism.  Allows EP to be a server which receives
 * socket input.  We currently allocate one thread per socket.
 * <p>
 * Required attributes:<ol>
 * <li><b>port</b>: specify the port to listen on for client connections</li>
 * <li><b>socketType</b>: specify socket type, either "tcp" or "udp" (tcp is
 * default)</li>
 * <li><b>dataType</b>: specify structure of events to listen for (currently
 * only <b>JavaObject</b> is supported)</li>
 * </ol>
 *
 * Data types currently supported:<ol>
 * <li>Java objects (via type <b>JavaObject</b>)</li>.  Note
 * that, while this is technically supported under UDP, TCP is strongly
 * recommended to support large objects.
 * <p>Java objects are treated opaquely, with the following exceptions:
 * <ol>
 *  <li>Siena notifications: a SienaEvent is automatically created.</li>
 * </ol>
 * <li>String input (via type <b>StringObject</b>).  Note that String input
 * is currently line-delimited.</li>
 * </ol>
 * <p>
 * Copyright (c) 2002: The Trustees of Columbia University in the
 * City of New York.  All Rights Reserved.
 *
 * <!--
 * TODO:
 * - Support simple XML Siena representations in addition to serialized Java
 * - Consider using NBIO instead for lots of clients
 * - Support non-serialized for non-Java-specific objects
 * -->
 *
 * @author Janak J Parekh
 * @version $Revision$
 */
public class SocketInput extends EPInput {
  // Data types
  /** Specifies a Java object-based data type. */
  public static final short JAVA_OBJECT = 1;
  /** Specifies a String object-based data type. */
  public static final short STRING_OBJECT = 2;
  
  // Socket types
  /** TCP socket */
  public static final short TCP = 1;
  /** UDP socket */
  public static final short UDP = 2;
  /** Max size of UDP packet */
  public static final int MAX_SIZE_UDP = 65536;
  
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
    String port = el.getAttribute("port");
    if(port == null || port.length() == 0) {
      throw new InstantiationException("Port not specified");
    }
    try {
      this.port = Short.parseShort(port);
    } catch(Exception e) {
      throw new InstantiationException("Port value invalid");
    }
    
    // TCP or UDP?
    String socketType = el.getAttribute("socketType");
    if(socketType == null || socketType.length() == 0) {
      debug.info("Socket type not specified, assuming TCP");
      this.socketType = TCP;
    }
    else if(socketType.equalsIgnoreCase("tcp")) this.socketType = TCP;
    else if(socketType.equalsIgnoreCase("udp")) this.socketType = UDP;
    else throw new InstantiationException("Invalid socket type specified");
    
    // Determine data type
    String dataType = el.getAttribute("dataType");
    if(type == null || type.length() == 0) {
      debug.warn("Type not specified, assuming \"StringObject\"");
      this.dataType = STRING_OBJECT;
    } else { // validate type of object
      if(dataType.equalsIgnoreCase("JavaObject")) {
        this.dataType = JAVA_OBJECT;
      } else if(dataType.equalsIgnoreCase("StringObject")) {
        this.dataType = STRING_OBJECT;
      } else {
        throw new InstantiationException("Invalid data type specified");
      }
    }
    
    // Now build our socket
    if(this.socketType == TCP) {
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
    if(socketType == TCP) {
      Socket cs = null; // Client socket
      
      while(!shutdown) {
        try {
          cs = ss.accept();
        } catch(Exception e) {
          debug.error("Could not accept connection, shutting down inputter" e);
          shutdown();
          return;
        }
        
        // Store a reference to cs, and then hand it to a client.  XXX -
        // does hashing of sockets like this work?
        try {
          ClientThread ct = new ClientThread(cs, dataType);
          clientSockets.put(cs, ct);
          new Thread(ct).start(); // Start it up
        } catch(InstantiationException e) {
          debug.error("Could not instantiate client", e);
          continue;
        }
      }
    } else { // UDP
      ct = new ClientThread(ds, dataType);
      clientSockets.put(ds, ct);
      ct.run();  /* We don't need a separate thread for the client handler,
       * since there's only one socket connection */
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
      ret = new DOMEvent(getName(), ((Document)o).getDocumentElement());
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
   * Shutdown.  XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
   * XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
   * XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
   */
  public abstract void shutdown() {
    
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
    /** InputStream to handle client input */
    private InputStream cin = null;
    /** The socket type */
    private short socketType = -1;
    /** Data type */
    private short dataType = -1;
    /** UDP packet buffer */
    private byte[] udpbuf = null;
    /** UDP packet valid data length */
    private int udpbuflen = -1;
    
    /**
     * CTOR for TCP connection.
     *
     * @param cs The client socket connection.
     * @param type The datatype.
     */
    public ClientThread(Socket cs, short dataType)
    throws InstantiationException {
      this.cs = cs;
      this.socketType = TCP;
      this.dataType = dataType;
      
      // Set up the streams
      switch(dataType) {
        case JAVA_OBJECT:
          try {
            cin = new ObjectInputStream(cs.getInputStream());
          } catch(Exception e) {
            throw new InstantiationException("Could not establish " +
            "ObjectInputStream: " + e);
          }
        case STRING_OBJECT:
          try {
            cin = new BufferedReader(new InputStreamReader(cs.getInputStream()));
          } catch(Exception e) {
            throw new InstantiationException("Could not establish " +
            "BufferedReader: " + e);
          }
        default:
          throw new InstantiationException("Unhandled type in ClientThread");
      }
    }
    
    /**
     * CTOR for UDP connection.
     *
     * @param ds The datagram socket connection
     * @param type The datatype.
     */
    public ClientThread(DatagramSocket ds, int type) throws
    InstantiationException {
      this.ds = ds;
      this.socketType = UDP;
      this.dataType = dataType;
      
      // Set up the UDP packet buffer
      udpbuf = new byte[MAX_SIZE_UDP];
    }
    
    /**
     * Run.  Receive entities and hand them to our parent.  Yes, this might
     * eventually need some optimization.
     */
    public void run() {
      Object input = null;
      while(!shutdown) {
        // UDP handling: first read the datagram
        if(socketType == UDP) {
          DatagramPacket dp = new DatagramPacket(udpbuf, MAX_SIZE_UDP);
          try {
            ds.receive(dp);
            udpbuflen = dp.getLength();
          } catch(Exception e) {
            debug.error("Could not read datagram, closing down client thread",e);
            if(!shutdown) shutdown();
            return;
          }
        }
        
        // Now differentiate based on datatype
        if(dataType == JAVA_OBJECT) { //////// JAVA_OBJECT ////////
          if(socketType == UDP) {
            // Construct a stream around our udpbuf
            try {
              cin = new ObjectInputStream(new ByteArrayInputStream(udpbuf, 0, udpbuflen));
            } catch(Exception e) {
              debug.error("Could not create ObjectInputStream for UDP buffer, "
              + "closing down client thread", e);
              if(!shutdown) shutdown;
              return;
            }
          }
          
          // Read using a objectInputStream, and hand the resulting
          // object to the SocketInput mechanism.
          try {
            input = ((ObjectInputStream)cin).readObject();
          } catch(Exception e) {
            debug.error("Could not read object, closing down client thread", e);
            if(!shutdown) shutdown();
            return;
          }
        } else if(dataType == STRING_OBJECT) { //////// STRING_OBJECT ////////
          if(socketType == UDP) {
            // Grab the string
            input = new String(udpbuf, 0, udpbuflen);
          } else {
            try {
              input = ((BufferedReader)cin).readLine();
            } catch(Exception e) {
              debug.error("Could not read string, closing down client thread", e);
              if(!shutdown) shutdown();
              return;
            }
          }
        } else {
          // We should NOT get here
          debug.error("Unhandled type in client thread run(), " +
          "shutting thread down");
          if(!shutdown) shutdown();
          return;
        }
        
        // Now hand the object to our parent
        if(!handleObject(input)) {
          // Problem
          debug.error("Invalid object received, closing down client thread");
          if(!shutdown) shutdown();
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
      shutdown = true;
      try {
        if(cs != null) cs.close();
        if(ds != null) ds.close();
      } catch(Exception e) { }
      cs = null; ds = null;
    }
  }
}
