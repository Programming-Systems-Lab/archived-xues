package psl.xues;

import siena.*;
import java.net.*;
import java.io.*;
/* additional imports for putting Siena info into databases */
import org.hsql.*;
import org.hsql.util.*;
import psl.kx.*;
//import org.jdom.*;
//import org.jdom.input.*;

import java.sql.*;
import java.util.*;
import java.sql.Types;
/** 
 * EventPackager for Xues.  Now Siena-compliant(TM).
 *
 * Copyright (c) 2000: The Trustees of Columbia University in the
 * City of New York.  All Rights Reserved.
 *
 * TODO: 
 * - Handling multi-line socket input as an event - how to delineate?  
 *   Right now, we just IGNORE all socket event!!!
 * - When socket closes, we wipe out TriKX.  More permanent solution.
 * - Put associated data onto webserver.  Send URL to KXNotification.
 *   Handle streaming in some appropriate way (non-trivial?  custom 
 *   webserver?)
 * - Integrate conf file handling
 * - Intelligent connection to Event Distiller
 * - Timestamp non-stamped incoming events
 *
 * @author Janak J Parekh <jjp32@cs.columbia.edu>
 * @version 0.01 (9/7/2000)
 *
 * $Log$
 * Revision 1.29  2002-01-11 03:30:17  aq41
 * Eventpackager now accepts filters and adds them to the postgresql database.
 * Minor debugging is underway.
 *
 * Revision 1.28  2001/12/27 23:29:54  aq41
 * Converting hsql Db to postgresql.
 *
 *
 * Updating
 *
 * java siena.StartServer -port 1982
 * java psl.xues.EventPackager -s senp://localhost:1982 -d 
 * java psl.xues.EPTest senp://localhost:1982
 */

public class EventPackager{
    /** XXX - This is a hack for now */
    private static String sienaHost = "senp://localhost";
    
    /* new fields for using database */  
    private Connection conn; // database connection
    private Statement statement; // sql statement
    private DatabaseMetaData meta; // database metadata
    private int current; // the largest assigned index 
    private Filter filter; // siena filter
    private String filterName; // filter component name
    private int maxresults = 5; // the max num of results that
                                //can be returned from an EPLookup query
    private static String userID; //userID for database access
    private static String password;//password for database access
    
    private EPHandler handler = null;
    
    int listeningPort = -1;
    ServerSocket listeningSocket = null;
    String spoolFilename;
    ObjectOutputStream spoolFile;
    int srcIDCounter = 0;
    Siena siena = null;
    
    /* Debug flag */
    static boolean DEBUG = false;
    
    /**
     * Basic CTOR.  
     * Assumes no spooling, and no listening sockets.
     */
    public EventPackager() {
	this(-1, null);
    }
    
    /**
     * CTOR.
     *
     * @param listeningPort Port to establish listening on.
     * @param spoolFile File to spool events to.
     */
    
    public EventPackager(int listeningPort, String spoolFilename) { 
	this.listeningPort = listeningPort;
	this.spoolFilename = spoolFilename;
	BufferedReader in = null;
	
	if(this.spoolFilename != null) { 
	    try {
		this.spoolFile = new ObjectOutputStream(new
		    FileOutputStream(this.spoolFilename,true));	
	    } catch(Exception e) { 
		System.err.println("Error creating spool file");
		e.printStackTrace();
	    }
	}
	
	connectDB(userID, password);
	connectFilterActionDB(userID, password);
	
	/* Add a shutdown hook */
	Runtime.getRuntime().addShutdownHook(new Thread() {
		public void run() {      
		    /* Clean up the file streams */
		    if(spoolFile != null) {
			System.err.println("EventPackager: shutting down");
			try {
			    spoolFile.close();
			    spoolFile = null;
			    ((HierarchicalDispatcher)siena).shutdown();
			    disconnectDB(); // **
			} catch(Exception e) { e.printStackTrace(); }
		    }
		}
	    });
	
	// Now create a Siena node
	siena = new HierarchicalDispatcher();
	try {
	    ((HierarchicalDispatcher)siena).
		setReceiver(new TCPPacketReceiver(61977));
	    ((HierarchicalDispatcher)siena).setMaster(sienaHost);
	} catch(Exception e) { e.printStackTrace(); }
	
	/*
	 *setup listening for events by subscribing to filters
	 */
	try{
	    Filter the_filter = null;
	    int filter_id = 0;
	    String attribute_constraint = null;
	    String operator = null;
	    String attribute_value = null;
	    
	    ResultSet answer = statement.executeQuery("SELECT * FROM Attribute_listing WHERE filter_id <= MAX(filter_id)");
	    System.out.println("answer: " + answer);
	    
	    while(!answer.isAfterLast()){
		filter_id = answer.getInt(1);
		attribute_constraint = answer.getString(2);
		operator = answer.getString(3);
		attribute_value = answer.getString(4);
		the_filter = new Filter();
		the_filter.addConstraint(attribute_constraint, Op.op(operator), attribute_value);
		try{
		    siena.subscribe(the_filter, new EPHandler(siena, statement, filter_id, userID, password));
		}
		catch(SienaException e){
		    e.printStackTrace();
		}
	    }//while
	}//try
	catch (SQLException e){
	    System.err.println(e);
	}
	finally{
	    try{
		if(in != null)
		    in.close();
	    }
	    catch (IOException e){
		System.out.println("Error "+ e);
	    }
	}//finally
    

    }//constructor
    
    /**
     * Run routine.
     */
    public void run() {
	/* Set up server socket */
	try {
	    listeningSocket = new ServerSocket(listeningPort);
	} catch(Exception e) {
	    System.err.println("EventPackager: Failed in setting up serverSocket, "+
			       "shutting down");
	    listeningSocket = null;
	    return;
	}
	/* Listen for connection */
	while(true) {
	    try {
		Socket newSock = listeningSocket.accept();
		/* Hand the hot potato off! */
		new Thread(new EPClientThread(srcIDCounter++,newSock)).start();
	    } 
	    catch(Exception e) {
		System.err.println("EventPackager: Failed in accept from "+
				   "serverSocket");
	    }
	}
    }
    
    /**
     * Tester.
     */
    public static void main(String args[]) {
	if(args.length > 0) { // Siena host specified?
	    for(int i=0; i < args.length; i++) {
		if(args[i].equals("-s"))
		    sienaHost = args[++i];

		else if(args[i].equals("-uid"))
		    userID = args[++i];

		else if(args[i].equals("-pwd"))
		    password = args[++i];

		else if(args[i].equals("-?"))
		    usage();
		
		else if(args[i].equals("-d"))
		    DEBUG = true;
		
		else
		    usage();
	    }
	}	   
	
	EventPackager ep = new EventPackager(7777, "EventPackager.spl");
	ep.run();
    } 
    
    /**
     * Print usage.
     */
    public static void usage() {
	System.out.println("usage: java EventPackager [-s sienaHost] [-uid userid] [-pwd password] [-d] [-?]");
	System.exit(-1);
    }
    
    
    class EPClientThread implements Runnable {
	private int srcID;
	private Socket clientSocket;
	private BufferedReader in;
	private String clientAddress="";
	private byte[] ipAddress= new byte[4];
	
	public EPClientThread(int srcID, Socket clientSocket) {
	    this.srcID = srcID;
	    this.clientSocket = clientSocket;
	    this.ipAddress = ( clientSocket.getInetAddress()).getAddress();
	    
	    for (int i = 0; i < ipAddress.length; i++) {
		
		clientAddress += Integer.toString((new Byte(ipAddress[i])).intValue())
		    + ".";
		// remove "." at the end of the address
		if (i == ipAddress.length -1)
		    clientAddress.substring(0, clientAddress.length()-2);
	    }	  
	    
	    /* Build the streams */
	    try {
		this.in = new BufferedReader(new 
		    InputStreamReader(clientSocket.getInputStream()));
		
	    } catch(Exception e) {
		System.err.println("Error establishing client connection: " +
				   e.toString());
		e.printStackTrace();
	    }
	}
	
	
	public void run() {
	    int bufLen = 100; // num of chars read in at a time
	    String newInput="";
	    
	    /* read in info coming from socket, add a tuple, publish data */
	    try {
		char[] cbuf = new char[bufLen]; 	
		int myCurrent = ++current; //increment tuple id
		boolean receivedData = false;
		
		//open file in which to put data
		File f = new File(Integer.toString(myCurrent));
		FileOutputStream fout= new FileOutputStream(f);
		
		while(true) {
		    int numRead = in.read(cbuf, 0,bufLen);
		    
		    if(numRead == -1 && receivedData) { // Finished - add new tuple now
			addTuple(System.currentTimeMillis(), "SocketConnection",
				 clientAddress, "");
			
			System.err.println("EPCThread: closing connection");
			in.close();
			fout.close();
			clientSocket.close();
			return;
		    }
		    else if(numRead != -1) {	  
			receivedData = true;
			newInput = new String(cbuf);  
			fout.write(newInput.getBytes()); // write data into file
		    }
		    else {
			/* No data received - do nothing for now */
		    }
		    
		    
		    System.err.println("EPCThread: Got " + newInput);
		    if(siena != null) {
			try {
			    siena.publish(new KXNotification("EventPackager event",srcID,null));
			} catch(SienaException e) { e.printStackTrace(); }
		    }
		}
		
	    } catch(SocketException e) {
		System.err.println("EPCThread: Client socket unexpectedly closed");
		return;
	    } catch(Exception e) {
		System.err.println("EPCThread: Error communicating with client:" + 
				   e.toString());
		e.printStackTrace();
		return;
	    }	
	}
    }
    
    //    connect to DB carrying filter and action notification information.
    
    
    private void connectFilterActionDB(String usr, String pwd){
	String classname = "org.postgresql.Driver";
	String host = "liberty.psl.cs.columbia.edu";
	String db = "psl";
	String url = "jdbc:postgresql://" + host + "/" + db;
	String tableName = null;
	String query = null;
	
	// make sure class is available
	try{
	    Class.forName(classname);
	} catch(java.lang.ClassNotFoundException e){
	    System.err.println("ClassNotFoundException: " + e.getMessage());
	}
	
	try{
	    conn = DriverManager.getConnection(url, usr, pwd);
	    statement = conn.createStatement();
	    
	    //attemp to get metadata
	    DatabaseMetaData dmd = conn.getMetaData();
	    
	    //get table name from metadata resultSet
	    ResultSet rs = dmd.getTables(null, null, null, null);
	    if(DEBUG) System.out.println("Resultset is: " + rs + ", size = " + rs.getFetchSize());
	    while (rs.next()){
		tableName = rs.getString(3);
		
		// Now check to see if table is the one we need
		// or enter tableName into a vector so we can search after
	    }
	    if (DEBUG) System.out.println("table name: " + tableName);
	    //XXX how to make sure checking for the right table.
	    
	    if(tableName == null || !tableName.equalsIgnoreCase("Filter_ID")){
		//create the tables for filter and related action management
		query = "CREATE TABLE Filter_ID(id SERIAL NOT NULL, description varchar(200), PRIMARY KEY(id))";
		statement.executeUpdate(query);
		
		if(DEBUG) System.out.println("table created");
	    }
	    
	    if(tableName == null || !tableName.equalsIgnoreCase("Attribute_listing")){
		//create table listing attributes for various filters
		query = "CREATE TABLE Attribute_listing(filter_id INT8 NOT NULL, attribute_value VARCHAR(200) NOT NULL, operator VARCHAR(20), argument VARCHAR(300), PRIMARY KEY(filter_id, attribute_value), CONSTRAINT foreign_id FOREIGN KEY(filter_id) REFERENCES filter_id (id) ON DELETE NO ACTION ON UPDATE NO ACTION NOT DEFERRABLE INITIALLY IMMEDIATE)";
		
		statement.executeUpdate(query);
		if(DEBUG)System.out.println("table created");
	    }
            if(DEBUG)System.out.println("table name " + tableName);
	    if(tableName == null || !tableName.equalsIgnoreCase("Action_listing")){
		//create table listing actios associated with various filters
		query = "CREATE TABLE Action_listing(filter_id BIGINT NOT NULL, action VARCHAR(2000),CONSTRAINT foreign_id FOREIGN KEY(filter_id) REFERENCES filter_id (id) ON DELETE NO ACTION ON UPDATE NO ACTION NOT DEFERRABLE INITIALLY IMMEDIATE)";
		
		statement.executeUpdate(query);
		if(DEBUG)System.out.println("table created");
	    }
	    String desc = "add filter";
	    //Adding initial filters which can then add more filters etc.
	    //int index_of_Filter_ID = NEXTVALUE('filter_id_id_seq');
	    executeSQLQuery("INSERT INTO Filter_ID (id, description)" + "VALUES (NEXTVAL('filter_id_id_seq'),'" + desc + "')");
	    //executeSQLQuery("INSERT INTO Filter_ID (id, description)" + "VALUES ('index_of_Filter_ID','" + desc + "')");
	    if(DEBUG) System.out.println("inserting filter description");

	    //adding filter attributes
	    executeSQLQuery("INSERT INTO Attribute_listing (filter_id, attribute_value, operator, argument) VALUES(1,'type','=','addFilter')");
	    if(DEBUG) System.out.println("inserting attributes");
	    
	    //add reaction to the filtered event
	    executeSQLQuery("INSERT INTO Action_listing(filter_id, action) VALUES(1,'addFilter')");
	    if(DEBUG) System.out.println("inserting actions");
	    
	}catch(SQLException e){
	    System.err.println("SQL EXCEPTION: "+ e.getMessage());
	}
	
    }
    
    /* establish connection to local database */
    private void connectDB(String usr, String pwd) {
	
	String classname ="org.postgresql.Driver";
	String host = "liberty.psl.cs.columbia.edu";
	String db = "psl";
	String url = "jdbc:postgresql://" + host + "/" + db;
	String tableName = null;
	String query = null;
	
	// make sure class is available
	try {
	    Class.forName(classname);
	} catch(java.lang.ClassNotFoundException e) {
	    System.err.println("ClassNotFoundException: " + e.getMessage());
	}
	
	try {
	    
	    conn = DriverManager.getConnection(url, usr, pwd);
	    statement = conn.createStatement();
	    
	    //attempt to get metadata
	    DatabaseMetaData dmd = conn.getMetaData();
	    
	    //get table name from metadata resultSet
	    ResultSet rs = dmd.getTables(null, null, null, null);
	    if(DEBUG) System.out.println("Resultset is: " + rs + ", size = " + rs.getFetchSize());
	    while (rs.next()){
		//the table name is the third column, according to janak's research :).
		tableName = rs.getString(3);
	    }
	    if(DEBUG)System.out.println("table name: " + tableName);
	    
	    if(tableName == null || !tableName.equalsIgnoreCase("Events")){
		// create the table EVENTS, if not already existing
		query = "CREATE TABLE Events(id SERIAL, time BIGINT, source VARCHAR, type VARCHAR, PRIMARY KEY(id))";
		
		statement.executeUpdate(query);
		if(DEBUG)System.out.println("table created");
		
		query = "CREATE INDEX time ON Events(time)";
		if(DEBUG)System.out.println("query redefined");
		
		statement.executeUpdate(query);
		System.out.println("index on events created");

		if (DEBUG) System.out.println("table created");
	    }
	    
	}catch(SQLException e){
	    System.err.println("SQL EXCEPTION: " + e.getMessage());
	}
	current = getMaxIndex();
	
	
	System.out.println("CURRENT MAX ID: " + current);
    }//end::connectDB
    
    
    // close database connection
    private void disconnectDB() {
	try {
	    statement.close();
	    conn.close();
	} catch (SQLException e) {
	    System.err.println("SQLException: " + e.getMessage());
	}
	
	if(DEBUG) System.out.println("CONNECTION TO DATABASE CLOSED.");
    }//end::disconnectDB
    
    
    // retrieve the maximum id in the database - used in connectDB
    private int getMaxIndex() {
	int tmp = -1;
	String query = null;
	try {
	    query = "SELECT MAX(id) FROM EVENTS";
	    ResultSet r = statement.executeQuery(query);
       	    if ( r.next() )
		tmp = r.getInt(1);
	} catch(SQLException e) {} 
	
	return tmp;
    }//end::getMaxIndex
    
    
    //query run with info received from EPLookup events
    public void queryTimes(long starttime, long endtime, String typ, int max) {
	
	
	try {
	    String query = null;
	    query = "SELECT * FROM Events WHERE Time > 'starttime' AND Time < 'endtime' AND Type = 'typ' ORDER BY Time";
	    
	    Vector v = Tuple.parseResultSet(statement.executeQuery(query));
	    
	    if (v.size() > max) {
		for (int k= v.size()-1; k >= max; k--)
		    v.removeElementAt(k);
	    }
	    
	    if (DEBUG) {
		System.out.println("The result set of query:");
		System.out.println(Tuple.tuplesToString(v)); 
	    }
	    
	    // send out a notification for each result row
	    for (int k=0; k < v.size(); k++) {
		
		Notification n = new Notification();
		n.putAttribute("Type", "EPResultRow");
		n.putAttribute("TimeStamp", ((Tuple)v.elementAt(k)).getTimeStr());
		n.putAttribute("DataSource", ((Tuple)v.elementAt(k)).getSrc());
		n.putAttribute("DataPath", ((Tuple)v.elementAt(k)).getId());
		
		siena.publish(n);
	    } 	
	} 
	catch (Exception e) {
	    System.err.println(e);
	}
	
    }	
    
    // Print the entire source table
    public void printTable() {
	
	try {
	    Vector v = Tuple.parseResultSet(statement.executeQuery("select * from EVENTS") );
	    
	    System.out.println("\nThe current data table is:");
	    //System.out.println(Tuple.tuplesToString(v));
	} catch (SQLException e) {
	    System.err.println(e);
	}
    }//end::printtable	
    
    
    /* adds a tuple from SmartEvent to the database */
    public boolean addTuple(long time1, String type1, String source1, String data1) {
	
	String query = null;
	
	if (DEBUG)System.out.println("time being added for tuple is: " + time1);
	String source= source1.toLowerCase();
	String type= type1.toLowerCase();
	String data= data1.toLowerCase();
	
	/*add tuple to database */
	int result = 
	  executeSQLQuery("INSERT INTO Events (id, time, type, source)" +
			  "VALUES (NEXTVAL('events_id_seq') ,"+ time1 + 
			  ", '"  + type + "', '" + source +"' )");
	
	if(DEBUG)System.out.println("inserted row, result = " + result);
	
	if(result == -1) return false;
	
	if (DEBUG) { printTable(); }
	
	return true;
	
    }//end::addTuple

  private int executeSQLQuery(String query) {
    if(DEBUG)
      System.out.println("Executing " + query);
    
    try {
      return statement.executeUpdate(query);
    } catch(SQLException e) {
      e.printStackTrace();
      return -1;
    }
  }
      
    /* creates a file in which to put the data in Notification */
    public void createNewDataFile(int fname, String data) {
	
        try{
            File f = new File(Integer.toString(fname));
            PrintWriter out = new PrintWriter(new FileWriter(f));
            out.print(data);
            out.close();
        } catch (IOException e) {
            System.err.println(e);
        }
	
    }//end::createNewDataFile
}




