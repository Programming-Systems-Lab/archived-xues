package psl.xues.ep.store;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import org.w3c.dom.Element;
import psl.xues.ep.event.EPEvent;

/**
 * JDBC store mechanism.
 *
 * Copyright (c) 2002: The Trustees of Columbia University in the
 * City of New York.  All Rights Reserved.
 *
 * <!--
 * TODO:
 * - Support anonymous and passwordless database connections
 * -->
 *
 * @author Janak J Parekh
 * @version $Revision$
 */
public class JDBCStore extends EPStore {
  /** Type of database we're using */
  private String dbType = null;
  /** Database driver for this type */
  private String dbDriver = null;
  /** Name of database */
  private String dbName = null;
  /** Name of table */
  private String tableName = null;
  /** Username */
  private String username = null;
  /** Password */
  private String password = null;
  /** JDBC connection */
  private Connection conn = null;
  /** JDBC statement */
  private Statement st = null;
  
  /**
   * CTOR.
   */
  public JDBCStore(Element el) throws InstantiationException {
    super(el);
    
    // Now attempt to determine necessary JDBC parameters
    dbType = el.getAttribute("DBType");
    dbDriver = el.getAttribute("DBDriver");
    dbName = el.getAttribute("DBName");
    tableName = el.getAttribute("DBTable");
    username = el.getAttribute("Username");
    password = el.getAttribute("Password");
    
    if(dbType == null || dbDriver == null || dbName == null || 
    tableName == null || username == null || password == null || 
    dbType.length() == 0 || dbDriver.length() == 0 ||
    dbName.length() == 0 || tableName.length() == 0 || 
    username.length() == 0) {
      debug.error("Can't initialize store: missing parameters");
      throw new InstantiationException();
    }
    
    // Try to load the driver
    try {
      Class.forName(dbDriver);
    } catch(Exception e) {
      debug.error("Can't initialize store", e);
      throw new InstantiationException();
    }
    
    // Attempt connection
    debug.debug("Connecting to jdbc:" + dbType + ":" + dbName + "...");
    try {
      conn = DriverManager.getConnection("jdbc:" + dbType + ":" + 
      dbName, username, password);
    } catch(Exception e) {
      debug.error("Can't connect to database", e);
    }
    
    // Connection successful
    debug.debug("Initialization complete");

    // Do we have our necessary table?
    try {
      ResultSet tableList = conn.getMetaData().getTables(null, null, tableName, 
      null);
      if(tableList.first() == false) {
        // Create the table
        debug.debug("Table \"" + tableName + "\" doesn't exist, creating");
        Statement s = conn.createStatement();
        s.executeUpdate("CREATE TABLE " + tableName + 
        " (SOURCE VARCHAR(100), TIMESTAMP " +
        "  
    
  }
 
  /**
   * Request an individual event given its (opaque) reference.
   *
   * @param ref The event reference.
   * @return The event in EPEvent form, or null if it doesn't exist.
   */
  public EPEvent requestEvent(Object ref) {
    
    
    
    return null;
    
  }
  
  /**
   * Request event(s).  Return a set of references to this event.  These
   * references are opaque.
   *
   * @param t1 The lower timebound (inclusive).
   * @param t2 The upper timebound (inclusive).
   * @return An array of (possibly opaque) object references, null if error.
   */
  public Object[] requestEvents(long t1, long t2) {
    return null;
  }
  
  /**
   * Store the supplied event.
   *
   * @param e The event to be stored.
   * @return An object reference indicating success, or null.
   */
  public Object storeEvent(EPEvent e) {
    return null;
  }
  
  /**
   * Handle shutdown.
   *
   * @return True usually, false if you can't shutdown for some reason.
   */
  public boolean shutdown() {
    
    return true;
  }
  
  /** Request all events from a given source.
   *
   * @param source The source of this event - matches the source in
   * the EPEvent.
   * @return An array of (possibly opaque) object references, empty array if
   * no match, and null if error.
   */
  public Object[] requestEvents(String source) {
    return null;
  }
  
  /** Request events from a given source between the two timestamps.
   *
   * @param source The source of this event - matches the source in
   * the EPEvent.
   * @param t1 The lower timebound (inclusive).
   * @param t2 The upper timebound (inclusive).
   * @return An array of (possibly opaque) object references, empty array if
   * no match, and null if error.
   */
  public Object[] requestEvents(String source, long t1, long t2) {
    return null;
  }
  
}