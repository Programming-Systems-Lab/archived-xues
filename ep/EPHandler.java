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
	System.out.println("EPHandler created");
    }
    /*
     * get the actions associated with the particular Notification since we already know the 
     * filter used for the Notification and execute the actions by calling method 
     * executeAction(n)
     */
    
    public void notify(Notification n){
	System.out.println("notification received");	
	try{
	    String action = null;
	    ResultSet rs = statement.executeQuery("SELECT * FROM Action_listing WHERE filter_id='"+ filterIndex+"'");
	    System.out.println("notification received " + rs);
	    while(rs.next()){
		action = rs.getString(2);
		executeAction(action, n);
	    }
	}
	catch(SQLException e){
	    e.printStackTrace();
	}
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
	AttributeValue description = n.getAttribute("Type");
	System.out.println("description: " + description);
	String description2 = "n.a";
	if(description != null) description2=description.stringValue();
	String attrName = n.getAttribute("AttrName").stringValue();//picked up attribute
	String opName = n.getAttribute("AttrOp").stringValue();
	String attrValue = n.getAttribute("AttrVal").stringValue();
	String actionValue = n.getAttribute("ActVal").stringValue();
	int temp = 0;
	try{
	    if(attrName != null && opName != null && attrValue != null){
		executeSQLQuery("INSERT INTO Filter_ID (id, description)" + "VALUES (NEXTVAL('filter_id_id_seq'),'" + description2 + "')");
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
}






