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
 * Revision 1.24  2001-07-31 19:13:36  aq41
 * Added capability to perform actions on notifications from EPHandler.cfg file
 * instead of hard coding the actions in EventPackager.java
 *
 * Revision 1.23  2001/07/23 02:45:44  aq41
 * Configuration file containing filter properties added instead of being
 * hardcoded within EventPackager.java
 *
 * Revision 1.22  2001/06/29 19:27:45  aq41
 *
 * Put Rose's version in CVS, added Tuple, removed jdom reference
 *
 * Revision 1.17  2001/01/30 02:39:36  jjp32
 *
 * Added loopback functionality so hopefully internal siena gets the msgs
 * back
 *
 * Revision 1.16  2001/01/30 00:24:50  jjp32
 *
 * Bug fixes, added test class
 *
 * Revision 1.15  2001/01/29 05:22:53  jjp32
 *
 * Reaper written - but it's probably a problem
 *
 * Revision 1.14  2001/01/29 02:14:36  jjp32
 *
 * Support for multiple attributes on a output notification added.
 *
 * Added Workgroup Cache test rules
 *
 * Revision 1.13  2001/01/28 22:58:58  jjp32
 *
 * Wildcard support has been added
 *
 * Revision 1.12  2001/01/26 03:30:54  jjp32
 *
 * Now supports non-localhost siena servers
 *
 * Revision 1.11  2001/01/22 02:11:54  jjp32
 *
 * First full Siena-aware build of XUES!
 *
 * Revision 1.10  2001/01/18 01:41:35  jjp32
 *
 * Moved KXNotification to kx; other modifications for demo
 *
 * Revision 1.9  2001/01/01 00:32:28  jjp32
 *
 * Added rudimentary Siena-publishing capabilities to Event Packager.  Created a (possibly, in the future) base notification class with convenience constructors (right now just for EP but in the future also for other KX components).
 *
 * Revision 1.8  2000/12/26 22:25:13  jjp32
 *
 * Updating to latest preview versions
 *
 * Revision 1.7  2000/09/09 18:17:14  jjp32
 *
 * Lots of bugs and fixes for demo
 *
 * Revision 1.6  2000/09/09 15:13:49  jjp32
 *
 * Numerous updates, bugfixes for demo
 *
 * Revision 1.5  2000/09/08 22:40:43  jjp32
 *
 * Numerous server-side bug fixes.
 * Removed TriKXUpdateObject, psl.trikx.impl now owns it to avoid applet permission hassles
 *
 * Revision 1.4  2000/09/08 19:08:27  jjp32
 *
 * Minor updates, added socket communications in TriKXEventNotifier
 *
 * Revision 1.3  2000/09/07 23:15:25  jjp32
 *
 * Added EventNotifier code; updated previous event code
 *
 * Revision 1.2  2000/09/07 19:30:49  jjp32
 *
 * Updating
 *
 * java siena.StartServer -port 1982
 * java psl.xues.EventPackager -s senp://localhost:1982 -d
 * java psl.xues.EPTest senp://localhost:1982
 */

public class EventPackager implements Notifiable {
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
    private EPHandler handler = null;
    private static String[] tableCreationSQL = {		
	"create table EVENTS (id integer,"+
	"time bigint,"+
	"source varchar(200),"+
	"type varchar(200),"+
	"PRIMARY KEY (id) )",
	"create index time on EVENTS(time)"
    };
    
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
	
	/* create a connection to the database */
	connectDB("DB", "sa", "");
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
	
	// Set up listening.  We listen for SmartEvents (which have a data
	// field with all the data), DirectEvents (which have the
	// attributes inline), and EPLookup events 
	
	try{
	    FileReader reader = new FileReader("EventPackager.cfg");
	    if(DEBUG)System.out.println("file reader");
	    in = new BufferedReader(reader);
	    String inputLine = in.readLine();
	    int numberOfFilters =0;
	    Filter filterName = null;	    
	    StringTokenizer st = null;
	    
	    while(inputLine != null && inputLine.length() > 0){
		numberOfFilters++;
		st = new StringTokenizer (inputLine);
		if(DEBUG)System.out.println("inputLine: "+ inputLine);
		String word = null;
		String first = null;
		String second = null;
		String third = null;
		int wordCount = 0;
		while(st.hasMoreTokens()){
		    word = st.nextToken();
		    wordCount++;
		    if(DEBUG)System.out.println("tokens read: " +word );
		    //String filterName= "filter"+numberOfFilters;
		    if (!word.endsWith(",") && wordCount  == 1){
			filterName  = new Filter();
			wordCount--;
			if(DEBUG)System.out.println("filter initialized");
		    }
		    else if(word.endsWith(";") && wordCount % 3 == 0){
			third = word;
			third = third.substring(0, third.length() -1);
			if(DEBUG)System.out.println("first: " + first + "third: " + third);
			filterName.addConstraint(first, third);
			System.out.println("filter constrain: " + filterName);
			try{
			    siena.subscribe(filterName, this);
			}
			catch(SienaException e){
			    e.printStackTrace();
			}
		    }
		    
		    else if (word.endsWith(",") && wordCount % 3 == 1){
			    first = word;
			    first = first.substring(0, first.length()-1);
		    }
		    else if(word.endsWith(",") && wordCount % 3 == 2){
			second = word;
			second = second.substring(0, second.length()-1);
		    }
				
		    
		}//while more tokens
		if(DEBUG)System.out.println("next line read");
		inputLine = in.readLine();
		if(DEBUG)System.out.println("line read in: " + inputLine);
	    }//while 
	    System.out.println("filtername: " + filterName);
	}//try
	catch (IOException e){
	    System.out.println("Error: " + e);
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
	
	// making a hashtable containing action Strings to use on a certain
	// type of notification. EPHandler.cfg contains the list of actions that
	// can be performed.
	
	Hashtable actions = new Hashtable();
	try{
	    FileReader reader = new FileReader("EPHandler.cfg");
	    BufferedReader in2 = new BufferedReader(reader);
	    String inputLine = in2.readLine();
	    StringTokenizer st = null;
	    while(inputLine != null && inputLine.length() > 0){
		inputLine.trim();
		st = new StringTokenizer(inputLine);
		String notificationType = st.nextToken();
		actions.put(notificationType, inputLine.substring(notificationType.length()+1));
		inputLine = in2.readLine();
	    }
	}
	catch(IOException e){
	    System.out.println("Error: " + e);
	}
	System.out.println("hashtable: " + actions);
        handler = new EPHandler(actions);
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
	System.out.println("usage: java EventPackager [-s sienaHost] [-d] [-?]");
	System.exit(-1);
    }
    
    /**
     * Handle incoming siena notifications.
     */
    public void notify(Notification n) {
	if(DEBUG) System.err.println("EventPackager: received notification " + n);
	handler.performing_action(n);
	
	/*notify(n);
	  Notification q = null;
	  
	  /* if we get a SMARTEVENT - extract data and add a tuple*/
	/*if(n.getAttribute("Type").stringValue().equals("SmartEvent")) {
	  int myCurrent = ++current; //increment tuple id
	  String data = n.getAttribute("SmartEvent").stringValue();
	  
	  createNewDataFile(myCurrent, data); //put data in file with name of myCurrent
	  addTuple(System.currentTimeMillis(), "SmartEvent", "Siena", myCurrent, data);      
	  q = KXNotification.EventPackagerKXNotification(11111,22222,(String)null, data);
	  
	  /* if we get an EPLOOKUP - extract needed attributes, run query */ 
	/* } else if (n.getAttribute("Type").stringValue().equals("EPLookup")) {
	    
	   long starttime = Long.parseLong(n.getAttribute("Start").stringValue());
	   long endtime = Long.parseLong(n.getAttribute("End").stringValue());
	   String lookuptype = n.getAttribute("LookupType").stringValue();
	   int max = Integer.parseInt(n.getAttribute("MaxResults").stringValue());
	   
	   if (max < 0 )
	   max = 0;
	   else if (max > maxresults)
	   max = maxresults;
	   
	   queryTimes(starttime, endtime, lookuptype, max); 
	   
	   } else {
	   // Direct Siena thing, just send it out
	   q = KXNotification.EventPackagerKXNotification(11111,22222,n);
	   }
	
	   try {
	   if(DEBUG) System.err.println("EventPackager: sending notification " + q);
	   
	   if ( !n.getAttribute("Type").stringValue().equals("EPLookup") )
	   siena.publish(q);
	   
	   } catch(SienaException e) { e.printStackTrace(); }
	*/
    }
    
    /** Unused Siena construct. */
    public void notify(Notification[] s) { ; }
    
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
				 clientAddress, myCurrent, "");
			
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
			//try {
			//      siena.publish(new KXNotification(srcID,null));
			//    } catch(SienaException e) { e.printStackTrace(); }
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
    
    
    /* establish connection to local database - from Xescii.java*/
    private void connectDB(String file, String usr, String pwd) {
	try {
	    Class.forName("org.hsql.jdbcDriver").newInstance();
	    conn = DriverManager.getConnection("jdbc:HypersonicSQL:"+
					       file,usr,pwd);
	    statement = conn.createStatement();
	} catch (Exception e) {
	    System.err.println("ERROR: FAILS TO ESTABLISH CONNECTION TO " +
			       file + ".");
	    System.exit(1);
	}
	
	// create the table EVENTS, if not already exists
	for(int i=0;i<tableCreationSQL.length;i++) {
	    try {
		statement.executeQuery(tableCreationSQL[i]);
	    }
	    catch(SQLException e) {
		if (e.getErrorCode() != 0) {
		    System.err.println("ERROR: FAILS TO CREATE TABLE");
		    System.exit(1);
		} 
		
		System.err.println(e);
	    }
	}
	
	current = getMaxIndex();
	
	System.out.println("CONNECTION TO " + file + " ESTABLISHED.");
	System.out.println("CURRENT MAX ID: " + current);
    }//end::connectDB
    
    
    // close database connection
    private void disconnectDB() {
	try {
	    conn.close();
	} catch (Exception e) {
	    System.err.println("ERROR: FAILS TO CLOSE DATABASE CONNECTION.");
            return;
	}
	
	System.out.println("CONNECTION TO DATABASE CLOSED.");
    }//end::disconnectDB
    
    
    // retrieve the maximum id in the database - used in connectDB
    private int getMaxIndex() {
	int tmp = -1;
	try {
	    ResultSet r = statement.executeQuery("select max(id) from EVENTS");
	    if ( r.next() )
		tmp = r.getInt(1);
	} catch(SQLException e) {} 
	
	return tmp;
    }//end::getMaxIndex
    
    
    //query run with info received from EPLookup events
    public void queryTimes(long starttime, long endtime, String typ, int max) {
	
	
	try {
	    Vector v = Tuple.parseResultSet(statement.executeQuery("select * from EVENTS E where E.time >"+ starttime + "AND E.time < " + endtime + " AND E.type = '" + typ + "' order by E.time") );
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
    public boolean addTuple(long time, String type, String source,
			    int curId, String data) {
	
	
	source= source.toLowerCase();
	type= type.toLowerCase();
	data= data.toLowerCase();
	
	/*add tuple to database */
	try {
	    
	    if (DEBUG) {
		System.out.println("INSERTING: " + curId + " " +
				   time + " " + source + " " + type);
	    }
	    
	    statement.executeQuery("insert into EVENTS values (" +
				   curId + "," + time +
				   " ,'" + source + "','" + type + "')");            
	    
	} catch (SQLException e) {
            System.out.println("ERROR in ADD TUPLE: ");
	    System.err.println(e);
	    return false;
	}
	
	if (DEBUG) { printTable(); }
	
	return true;
	
    }//end::addTuple
    
    
    /* creates a file in which to put the data in Notification */
    public void createNewDataFile(int fname, String data) {
	
        try {
            File f = new File(Integer.toString(fname));
            PrintWriter out = new PrintWriter(new FileWriter(f));
            out.print(data);
            out.close();
        } catch (IOException e) {
            System.err.println(e);
        }
	
    }//end::createNewDataFile
}















