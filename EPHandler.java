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

public class EPHandler{
    private Hashtable list_of_actions;//hashtable containing list of actions to be performed
    //on a certain type of notification
    private boolean DEBUG = true;//debug flag
    private int current;// the largest assigned index
    private int maxresults = 5; //themax num of results that can be returned from an EPLookup query
    private Statement statement;//sql statement
    private Siena siena = null;

    /*constructor*/
    public EPHandler(Hashtable actions, Statement st, Siena s ){
	list_of_actions = actions;
	statement = st;
	siena =s;
    }
    
    public void performing_action(Notification n){
	StringTokenizer st = null;//string tokenizer to read hashtable string
	String property = n.getAttribute("Type").stringValue();
	if(DEBUG)System.out.println("property: "+ property);
	String toDo = (String)list_of_actions.get(property);
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
			if(DEBUG)System.out.println("does enter the else if statement");
			extractData_addTuple(n);
		    }	   
		    else if(a.equals("extractAttributes_runQuery")) extractAttributes_runQuery(n);
		}
	    }
	}
    }
    
    private void extractData_addTuple(Notification n){
	int myCurrent = ++current; //increment tuple id
	String data =n.getAttribute(n.getAttribute("Type").stringValue()).stringValue();
	
	createNewDataFile(myCurrent, data); // put data in file with name of myCurrent
	addTuple(System.currentTimeMillis(), n.getAttribute("Type").stringValue(), "Siena", myCurrent, data);
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
    
}






