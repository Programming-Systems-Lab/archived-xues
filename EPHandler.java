package psl.xues;

import java.util.*;
import siena.*;

public class EPHandler{
    private Hashtable list_of_actions;
    private boolean DEBUG = true;
    public EPHandler(Hashtable actions){
	list_of_actions = actions;
    }
    public void performing_action(Notification n){
	if(DEBUG) System.out.println("enters EPHandler class");
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
	if(DEBUG)System.out.println("leaving performing_action method");
    }
    
    private void print(Notification n){
	System.out.println("printing notification n: " + n);
    }
    
    private void printCapital(Notification n){
	System.out.println("printing notificatio n in capitals: " + n);
    }
}






