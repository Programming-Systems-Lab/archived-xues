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

public class EPHandler{
    private Hashtable list_of_actions;//hashtable containing list of actions to be performed
    //on a certain type of notification
    private boolean DEBUG = true;//debug flag

    /*constructor*/
    public EPHandler(Hashtable actions){
	list_of_actions = actions;
    }
    
    public void performing_action(Notification n){
	StringTokenizer st = null;
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
		    if(a.equals("print"))print(n);
		    else if(a.equals("printCapital")) printCapital(n);
		}
	    }
	}
    }
    
    private void print(Notification n){
	System.out.println("printing notification n: " + n);
    }
    
    private void printCapital(Notification n){
	System.out.println("printing notificatio n in capitals: " + n);
    }
}






