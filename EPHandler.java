/*
  Copyright (c) 2001: The Trustees of Columbia University
  in the City of New York. All Rights Reserved.

  EPHandler.java
  
  @author: Amna Qaiser
  Handler class to perform various actions on different Notifications.
  Actions to be performed can be modified in EPHandler.cfg file.
  
*/
package psl.xues;

import java.util.*;
import siena.*;
import java.sql.*;
import java.io.*;
import org.hsql.*;
import org.hsql.util.*;
import psl.kx.*;
import java.net.*;
import java.sql.*;

public class EPHandler implements Notifiable{
    
    private boolean DEBUG = true;//debug flag
    private int current;// the largest assigned index
    private int maxresults = 5; //themax num of results that can be returned from an EPLookup query
    private Siena siena = null;
    private int filterIndex = 0;
    private String uid = null;
    private String pwd = null;
    private Statement statement = null;
    /*constructor*/
    
    public EPHandler(Siena s, Statement state, int filter, String uid, String pwd ){
	siena =s;
	filterIndex = filter;
	uid=uid;
	pwd=pwd;
	statement = state;
    }
    /*
     * get the actions associated with the particular Notification since we already know the 
     * filter used for the Notification and execute the actions by calling method 
     * executeAction(n)
     */
    
    public void notify(Notification n){
	try{
	String action = null;
	ResultSet rs = statement.executeQuery("SELECT * FROM Action_listing WHERE filter_id = filter_index");
	while(!rs.isAfterLast()){
	    action = rs.getString(2);
	    executeAction(action, n);
	}
	}
	catch(SQLException e){
	    e.printStackTrace();
	}
        
	/*
	  StringTokenizer st = null;//string tokenizer to read hashtable string
	  //String property = n.getAttribute("Type").stringValue();
	  
	  String toDo = (String)list_of_actions.get(filterIndex);
	if(toDo!=null){
	    if(DEBUG)System.out.println("toDo---stuff to do to property " + toDo);
	    st = new StringTokenizer(toDo);
	    if (DEBUG) System.out.println("things to do: " + toDo);
	    
	    if(toDo != null && toDo.length() >0){
		while(st.hasMoreTokens()){
		    String a = st.nextToken();
		    if(DEBUG)System.out.println("a: " + a);
		    if(a.equals("print"))print(n);
		    else if(a.equals("printCapital")) printCapital(n);
		    else if(a.equals("extractData_addTuple")){
			extractData_addTuple(n);
		    }	   
		    else if(a.equals("extractAttributes_runQuery")) extractAttributes_runQuery(n);
		    else if(a.equals("addFilter")) addFilter(n);
		}
	    }
	}
	*/    
    }
    
    /**Unused Siena construct.*/
    
    public void notify(Notification[] s){;}
    
    private void executeAction(String action, Notification n){
	if(action.equals("addFilter")) addFilter(n);
    }

    /*XXX to be documented: any filter that to be added must have the attributes of 1) desc,
     * 2)AttrName, 3)AttrOp, 4)AttrVal 5)actionVal
     */ 
    private void addFilter(Notification n){
	String description = n.getAttribute("Type").stringValue();//picked up new filter desc.
	String attrName = n.getAttribute("AttrName").stringValue();//picked up attribute
	String opName = n.getAttribute("AttrOp").stringValue();
	String attrValue = n.getAttribute("AttrVal").stringValue();
	String actionValue = n.getAttribute("ActVal").stringValue();
	int temp = 0;
	try{
	    if(attrName != null && opName != null && attrValue != null){
		executeSQLQuery("INSERT INTO Filter_ID (id, description)" + "VALUES (NEXTVAL('filter_id_id_seq'),'" + description + "')");
		ResultSet r = statement.executeQuery("SELECT MAX(id) FROM Filter_ID");
		if (r.next())
		    temp = r.getInt(1);
		
		executeSQLQuery("INSERT INTO Attribute_listing(filter_id, attribute_value, operator, argument)" + " VALUES('"+temp+"', '" + attrName+"', '" + opName+"','"+ attrValue +"')");
		executeSQLQuery("INSERT INTO Action_listing(filter_id, action)" + 
				" VALUES('"+temp+"','" + actionValue + "')");
	    }
	    /*XXX right now siena subscribes to the new filter immediately. one may consider implementation to come in effect in a new session*/ 
	    try{
		siena.subscribe(new Filter(), new EPHandler(siena, statement, temp, uid, pwd));
	    }
	    catch(SienaException e){
		e.printStackTrace();
	    }
	}
	catch(SQLException e){
	    e.printStackTrace();
	}
    }

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
    /*
      private void addFilter(Notification n){
      
      //subscribe the filter to siena
      if(DEBUG) System.out.println("addFilter method");
      boolean keepFilter= false;
      Boolean a;
      String filterIndex = n.getAttribute("Name").stringValue();//picked up the filter name
      String attrName = n.getAttribute("AttrName").stringValue();//picked up the attribute
      String opName = n.getAttribute("AttrOp").stringValue();
      String attrValue = n.getAttribute("AttrVal").stringValue();
	
      String filterValue = n.getAttribute("keepFilter").stringValue();
      if(filterValue != null){
      a = new Boolean(n.getAttribute("keepFilter").stringValue());
      keepFilter = a.booleanValue();
	}
	try{
	if(filterIndex != null && attrName != null && opName != null && attrValue != null){
	Filter theFilter = new Filter();
	theFilter.addConstraint(attrName, Op.op(opName), attrValue);
	siena.subscribe(theFilter, new EPHandler(siena, filterIndex));
	    }//if
	    if(filterValue != null){
	    if(keepFilter){
	    //add to EventPackager.cfg file for permanent addition of the filter
	    if(DEBUG)System.out.println("filter added permanently");
		    FileWriter writer = new FileWriter("EventPackager.cfg",true);
		    PrintWriter out = new PrintWriter(writer);
		    
		    //XXX bug: if the same filter may be added without checking if its present in the EventPackager.cfg or not.
		    out.println(filterIndex + " " + attrName + ", " + opName + ", " + attrValue + ";");
		    //		out.flush();
		    out.close();
		    writer.close();
		}//if
		}//if
		}//try
		
	catch(IOException er){
	System.out.println(er);
	}
	catch(SienaException e){
	e.printStackTrace();
	}
	
	}//addFilter
	
	private void extractData_addTuple(Notification n){
	int myCurrent = ++current; //increment tuple id
	String data =n.getAttribute(n.getAttribute("Type").stringValue()).stringValue();
	
	createNewDataFile(myCurrent, data); // put data in file with name of myCurrent
	addTuple(System.currentTimeMillis(), n.getAttribute("Type").stringValue(), "Siena",data);
	if(DEBUG)System.out.println("tuple added!!!!");
    }
    
    private void extractAttributes_runQuery(Notification n){
    long starttime = Long.parseLong(n.getAttribute("Start").stringValue());
    long endtime = Long.parseLong(n.getAttribute("End").stringValue());
	String lookuptype = n.getAttribute("LookupType").stringValue();
	int max = Integer.parseInt(n.getAttribute("MaxResult").stringValue());
	if(max<0)
	max = 0;
	else if (max > maxresults)
	    max = maxresults;
	queryTimes (starttime, endtime, lookuptype, max);
	if(DEBUG)System.out.println("extractAttributes_runQuery ran!!!!");
    }
    
    private void print(Notification n){
	System.out.println("printing notification n: " + n);
    }
    
    private void printCapital(Notification n){
	System.out.println("printing notificatio n in capitals: " + n);
    }
    */
     /* creates a file in which to put the data in Notification */
    /*
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

    */

    /* adds a tuple from SmartEvent to the database */
    /*
      public boolean addTuple(long time, String type, String source, String data) {
	
	String query = null;
	source= source.toLowerCase();
	type= type.toLowerCase();
	data= data.toLowerCase();
    */
	/*add tuple to database */
    /*
      try {
	    
	    if (DEBUG) {
		System.out.println("INSERTING: " + time + " " + source + " " + type);
	    }
	    
	    query = "insert into EVENTS (id, time, source, type) VALUES (NEXTVAL('events_id_seq'), 'time', 'source', 'type')";
	    int result = statement.executeUpdate(query);
	    
	    if(DEBUG)System.out.println("inserted row, result = " + result);

	} catch (SQLException e) {
            System.out.println("ERROR in ADD TUPLE: ");
	    System.err.println(e);
	    return false;
	}
	
	if (DEBUG) { printTable(); }
	
	return true;
	
    }//end::addTuple
    
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
    */    
    
}






