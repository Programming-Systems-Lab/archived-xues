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

/**
 * Socket event input mechanism.  Allows EP to be a server which receives
 * socket input.  We currently allocate one thread per socket.
 * <p>
 * Required attributes:<ol>
 * <li><b>port</b>: specify the port to listen on for client connections</li>
 * <li><b>type</b>: specify structure of events to listen for (currently
 * only <b>JavaObject</b> is supported)</li>
 * </ol>
 *
 * Input types currently supported:<ol>
 * <li>Serialized Siena notifications (via type <b>JavaObject</b>)</li>
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
  /** Input type of Java objects */
  public static final int JAVA_OBJECT = 1;
  
  /** Server socket port */
  private short port = -1;
  /** data type */
  private short type = -1;
  /** Our ServerSocket */
  private ServerSocket ss;
  /** Our client sockets */
  private HashMap clientSockets = new HashMap();
  
  /**
   * CTOR.
   */
  public SocketInput(EPInputInterface ep, Element el)
  throws InstantiationException {
    super(ep,el);
    
    // Get the basic ServerSocket parameters
    String port = el.getAttribute("port");
    if(port == null || port.length() == 0) {
      throw new InstantiationException("Port not specified");
    }
    try {
      this.port = Short.parseShort(port);
    } catch(Exception e) {
      throw new InstantiationException("Port value invalid");
    }
    String type = el.getAttribute("type");
    if(type == null || type.length() == 0) {
      debug.warn("Type not specified, assuming \"JavaObject\"");
      this.type = JAVA_OBJECT;
    } else { // validate type of object
      if(type.equalsIgnoreCase("JavaObject")) {
        this.type = JAVA_OBJECT;
      } else {
        throw new InstantiationException("Invalid data type specified");
      }
    }
    
    // Now build our ServerSocket
    try {
      ss = new ServerSocket(this.port);
    } catch(Exception e) {
      ss = null;
      throw new InstantiationException("Could not establish socket: " + e);
    }
    
    // We're ready to run
  }
  
  /**
   * Run.  We listen to the server socket, and hand client connections
   * to a handler.
   */
  public void run() {
    Socket cs = null; // Client socket
    
    while(!shutdown) {
      try {
        cs = ss.accept();
      } catch(Exception e) {
        debug.error("Could not accept connection, shutting down inputter", e);
        shutdown();
        return;
      }
      
      // Store a reference to cs, and then hand it to a client.  XXX -
      // does hashing of sockets like this work?
      ClientThread ct = null;
      try {
        new ClientThread(cs, type);
        clientSockets.put(cs, ct);
        new Thread(ct).start(); // Start it up
      } catch(InstantiationException e) {
        debug.error("Could not instantiate client", e);
        continue;
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
      ret = new DOMEvent(getName(), ((Document)o).getDocumentElement());
    } else if(o instanceof String) {
      ret = new StringEvent(getName(), (String)o);
    } else {
      // No clue what to do with this
      return false;
    }
    
    ep.injectEvent(ret);
    return true;
  }
  
  /**
   * Thread to handle a client connection.
   *
   * @author Janak J Parekh <janak@cs.columbia.edu>
   * @version $Revision$
   */
  class ClientThread implements Runnable {
    /** Reference to client socket */
    private Socket cs = null;
    /** Are we in shutdown? */
    private boolean shutdown = false;
    /** InputStream to handle client input */
    private InputStream cin = null;
    
    public ClientThread(Socket cs, int type) throws InstantiationException {
      this.cs = cs;
      // What kind of entities are we handling?
      switch(type) {
        case JAVA_OBJECT:
          try {
            cin = new ObjectInputStream(cs.getInputStream());
          } catch(Exception e) {
            throw new InstantiationException("Could not establish " +
            "ObjectInputStream: " + e);
          }
        default:
          throw new InstantiationException("Unhandled type in ClientThread");
      }
    }
    
    /**
     * Run.  Receive entities and hand them to our parent.
     */
    public void run() {
      if(type == JAVA_OBJECT) {
        // Read repeatedly from the ObjectInputStream, and hand the resulting
        // object to the SocketInput mechanism.
        Object input = null;
        while(!shutdown) {
          try {
            input = ((ObjectInputStream)cin).readObject();
          } catch(Exception e) {
            debug.error("Could not read object, closing down client thread", e);
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
      
      // We should NOT get here
      debug.error("Unhandled type in client thread run(), " +
      "shutting thread down");
      if(!shutdown) shutdown();
      return;
    }
    
    /**
     * Shutdown.  XXX - not clear if this will work correctly.
     */
    public void shutdown() {
      shutdown = true;
      try { cs.close(); } catch(Exception e) { }
      cs = null;
    }
  }
}
