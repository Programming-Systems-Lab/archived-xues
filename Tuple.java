/**
 * Copyright (c) 2001: The Trustees of Columbia University 
 * in the City of New York.  All Rights Reserved.
 *
 * Tuple.java
 *
 * @author: Shen Li
 * 
 * The definition of a tuple in the database of the data server.
 * Number of attributes can be modified.
 */

package psl.xues;

import java.util.*;
import java.sql.*;

public class Tuple {
    
    public static final int NUM_FIXED = 4; // number of universal attributes
    //public static final int NUM_OPT = 5; // number of optional attributes 
    public static final int NUM_ATTRIB = 4; // number of total attributes
    
    // the names of universal attributes
    public static final String[] ATTRIB_NAME = {
	"id", "time", "source", "type"
    };
    
    // variable definitions
    private int id;
    private long time;
    private String source;
    private String type;
    
    
    // constructors
    public Tuple (int i, long tm, String s, String ty) {
	
	id = i;
	time = tm;
	source = s;
	type = ty;
	
    }
    
    // parse a result set into a vector of tuples
    public static Vector parseResultSet(ResultSet r) {
	
	Tuple[] tmp = null;
	int id = -1;
	long time = -1;
	String source = null;
	String type = null;

	Vector v = new Vector();

	try {
	    ResultSetMetaData m=r.getMetaData();
	    int col=m.getColumnCount();

	    if ( col != NUM_ATTRIB ) {
		System.err.println("ERROR: RESULT SET HAS INCOMPATIBLE NUMBER OF COLUMNS.");
		System.err.println("We have "+col+ "columns");
		System.exit(1);
	    }

	    while ( r.next() ) {
		//System.err.println("first occurence.");
		
		for(int i=1;i<=col;i++) {
		    if ( m.getColumnLabel(i).equals("ID") ) {
			id = r.getInt(i);
		    } else if ( m.getColumnLabel(i).equals("TIME") ) {
			time = r.getLong(i);
		    } else if ( m.getColumnLabel(i).equals("SOURCE") ) {
			source = r.getString(i);
		    } else if ( m.getColumnLabel(i).equals("TYPE") ) {
			type = r.getString(i);
		    } 
		}
		
		//System.err.println("before adding tuple");
		v.add(new Tuple(id, time, source, type));
	    }

	} catch(SQLException e) {
	    System.err.println("ERROR here1***");
	    System.err.println(e);
	}
	
	//System.err.println("vector returned: " + v.size());
	return v;
    }
    
    public String toString() {
	String tmp = "";
	tmp += "ID: " + id + "  ";
	tmp += "TIME: " + time + "  ";
	tmp += "SOURCE: " + source + "  ";
	tmp += "TYPE: " + type + "\n";
	
	return tmp;
    }
    
    // output an array of tuples to string
    public static String tuplesToString(Vector v) {
	String tmp = "";
	
	if (v==null)
	    return null;
	
	for (int i=0; i<v.size(); i++) {
	    tmp += ((Tuple)v.elementAt(i)).toString();
	    tmp += "\n";
	}
	return tmp;
    }

    // accessors
    public int getId() { return id;}
    public String getTimeStr() {
	String s = Long.toString(time);
	return s;
    }
    public String getSrc() { return source;}
    public String getType() { return type;}
    
}











