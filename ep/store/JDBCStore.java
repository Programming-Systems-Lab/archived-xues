package psl.xues.ep.store;

import java.sql.Array;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import org.w3c.dom.Element;

import psl.xues.ep.event.EPEvent;
import psl.xues.ep.util.Base64;

/**
 * JDBC store mechanism.
 * <p>
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
  /** Maximum base64-encoded size of events */
  public static final int eventSize = 32768;
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
   * Autoincrement counter - needed because HSQL and possibly other DB's don't
   * have increment functionality
   */
  private long lastID = -1;
  
  /**
   * CTOR.
   */
  public JDBCStore(EPStoreInterface ep, Element el) 
  throws InstantiationException {
    super(ep,el);
    
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
      if(tableList.next() == false) {
        // Create the table
        debug.debug("Table \"" + tableName + "\" doesn't exist, creating");
        Statement s = conn.createStatement();
        s.executeUpdate("CREATE TABLE " + tableName +
        " (ID BIGINT PRIMARY KEY, SOURCE VARCHAR(100), TIMESTAMP TIMESTAMP, " +
        "FORMAT VARCHAR(100), EVENT VARCHAR(" + eventSize + "))");
        s.executeUpdate("CREATE INDEX source_index ON " + tableName +
        " (SOURCE)");
        s.executeUpdate("CREATE INDEX timestamp_index ON " + tableName +
        " (TIMESTAMP)");
        s.executeUpdate("CREATE TABLE " + tableName + "metadata" +
        " (LASTID BIGINT)");
        s.executeUpdate("INSERT INTO " + tableName + "metadata" +
        " (LASTID) VALUES (-1)");
        s.close();
      } else {
        // Get the lastID from metadata
        Statement s = conn.createStatement();
        ResultSet lastIDSet = s.executeQuery("SELECT LASTID FROM " + tableName
        + "metadata");
        if(lastIDSet.next() == false) {
          throw new InstantiationException("Can't determine last ID");
        }
        lastID = lastIDSet.getLong(1);
        debug.debug("Table \"" + tableName + "\" exists, last ID is " + lastID);
        s.close();
      }
    } catch(Exception e) {
      debug.error("Could not check/create table", e);
      throw new InstantiationException("Could not check/create table");
    }
    
    // All done
  }
  
  /**
   * Request an individual event given its (opaque) reference.
   *
   * @param ref The event reference.
   * @return The event in EPEvent form, or null if it doesn't exist.
   */
  public EPEvent requestEvent(Object ref) {
    EPEvent ret = null;
    
    // Is the reference legitimate?
    if(!(ref instanceof Long)) {
      debug.error("requestEvent called with invalid reference");
      return null;
    }
    long reqID = ((Long)ref).longValue();
    if(reqID > lastID) {
      debug.error("requestEvent called with reference out of bounds");
      return null;
    }
    
    try {
      Statement reqE = conn.createStatement();
      ResultSet requestedEvent = reqE.executeQuery("SELECT EVENT FROM " +
      tableName + " WHERE ID = " + reqID);
      if(requestedEvent.next() == false) {
        debug.warn("requestEvent found no match for reference");
        return null;
      }
      // Extract the data
      ret = (EPEvent)Base64.decodeToObject(requestedEvent.getString(1));
      reqE.close();
    } catch(Exception e) {
      debug.error("Could not requestEvent", e);
      return null;
    }
    
    // Return the event
    return ret;
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
    return getIDs("SELECT ID FROM " +
    tableName + " WHERE TIMESTAMP BETWEEN " + t1 + " AND " + t2);
  }
  
  /**
   * Store the supplied event.
   *
   * @param e The event to be stored.
   * @return An object reference indicating success, or null.
   */
  public Object storeEvent(EPEvent e) {
    // First grab the event and try to base64 it.
    String encoding = Base64.encodeObject(e);
    if(encoding == null) {
      debug.warn("Could not serialize event, skipping");
      return null;
    } else if(encoding.length() > eventSize) {
      debug.warn("Cannot fit encoded event in database, skipping");
      return null;
    }
    
    // Store it
    try {
      Statement addS = conn.createStatement();
      addS.executeUpdate("INSERT INTO " + tableName +
      " (ID, SOURCE, TIMESTAMP, FORMAT, EVENT) VALUES (" + (++lastID) +
      ", '" + e.getSource() + "', " + e.getTimestamp() +
      ", '" + e.getFormat() + "', '" + encoding + "')");
      addS.close();
    } catch(SQLException ex) {
      debug.error("Could not add event", ex);
      return null;
    }
    
    // Done
    return new Long(lastID);
  }
  
  /**
   * Request all events from a given source.
   *
   * @param source The source of this event - matches the source in
   * the EPEvent.
   * @return An array of (possibly opaque) object references, empty array if
   * no match, and null if error.
   */
  public Object[] requestEvents(String source) {
    return getIDs("SELECT ID FROM " + tableName + " WHERE SOURCE = '" + 
    source + "'");
  }
  
  /**
   * Request events from a given source between the two timestamps.
   *
   * @param source The source of this event - matches the source in
   * the EPEvent.
   * @param t1 The lower timebound (inclusive).
   * @param t2 The upper timebound (inclusive).
   * @return An array of (possibly opaque) object references, empty array if
   * no match, and null if error.
   */
  public Object[] requestEvents(String source, long t1, long t2) {
    return getIDs("SELECT ID FROM " + tableName + " WHERE (SOURCE = '" + 
    source + "') AND (TIMESTAMP BETWEEN " + t1 + " AND " + t2 + ")");
  }
  
  /**
   * Handle shutdown.
   *
   * @return True usually, false if you can't shutdown for some reason.
   */
  public boolean shutdown() {
    try {
      // Write out last ID
      debug.debug("Shutting down...");
      Statement s = conn.createStatement();
      s.executeUpdate("UPDATE " + tableName + "metadata SET LASTID = "
      + lastID);
      s.close();
      conn.close();
      debug.debug("Shutdown complete");
    } catch(Exception e) {
      debug.error("Could not shutdown, ignoring", e);
    }
    return true;
  }
  
  /**
   * Perform the specified query and extract the IDs associated with the query.
   *
   * @param rs The SQL query.
   * @return An array of the Objects corresponding to the identifiers.
   */
  private Object[] getIDs(String query) {
    ArrayList ret = new ArrayList();

    try {
      Statement reqE = conn.createStatement();
      ResultSet events = reqE.executeQuery(query);
      while(events.next()) { // Build our result array
        ret.add(new Long(events.getLong(1)));
      }
      reqE.close();
    } catch(Exception e) {
      debug.error("Could not execute query", e);
      return null;
    }
    
    // Success
    return ret.toArray(); // Convert to standard Java array
  }
}