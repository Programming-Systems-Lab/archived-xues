package psl.xues.ed;

import psl.kx.KXNotification;
import java.io.*;
import java.net.*;
import java.util.*;
import siena.*;

/**
 * We use this to test various functionalities of the ED,
 * in particular those that need to work dynamically.
 *
 * @author enrico buonanno
 * @version 0.1
 *
 */
public class EDTestDynamic {

    /** the Siena dispatcher. */
    static HierarchicalDispatcher hd;
  
    /** Constructor */
    public static void main(String[] args) {
    
    if(args.length == 0 || args[0].equals("-?")) {
      System.out.println("Usage: java EDTest <senp url>");
      System.exit(0);
    }
    
    hd = new HierarchicalDispatcher();
    try {
      hd.setReceiver(new TCPPacketReceiver(0));
      hd.setMaster(args[0]);
    } catch (InvalidSenderException ise) {
      System.err.println("Invalid Sender:" + ise);
      return;
    } catch (IOException ioe) {
      System.err.println("Unable to set hd receiver:" + ioe);
      return;
    }

    /* test any of the dynamic rulebase action.
     * just comment out what you don't need, in each test. */


    // test removing the spamblocker rule
    /*try { hd.publish(KXNotification.EDManagerRemoveRule("spamblocker")); } 
    catch (SienaException se) { System.err.println("Siena exception on publish:" + se);  } 
    // wait for the rule to be deleted - just to test
    try { Thread.currentThread().sleep(500); }
    catch(InterruptedException e) { System.err.println(e); }
    */
    /* test adding the rule -- we add the spamblocker rule.
     * if you want to do this, run the EventDistiller with a spec. file
     * that does NOT already have the spamblocker rule. */
    /*addRule();
    // make sure the rule gets added
    try { Thread.currentThread().sleep(1000); }
    catch(InterruptedException e) { System.err.println(e); }
    */
    // send the first event specified by the spamblocker rule 
    /*sendEvent();
    // make sure the first notif gets there first
    try { Thread.currentThread().sleep(500); }
    catch(InterruptedException e) { System.err.println(e); }
    */
    // send the second event of the rule
    //sendEvent();
    /*
    // query the rule
    try { hd.publish(KXNotification.EDManagerQueryRule("spamblocker"));	} 
    catch (SienaException se) { System.err.println("Siena exception on publish:" + se);  } 
    */
    // get the list of rules
    try { hd.publish(KXNotification.EDManagerQueryRules());	} 
    catch (SienaException se) { System.err.println("Siena exception on publish:" + se);  } 
    
    // finish
    hd.shutdown();
  }

    /** sends out an event to test a rule. */
    private static void sendEvent() {
	
	// make sample event
	Notification n1 = new Notification();
	n1.putAttribute("Source", "EventDistiller");
	n1.putAttribute("SourceID", 12345);
	n1.putAttribute("Type", "DirectEvent");
	n1.putAttribute("from", "Janak");
	n1.putAttribute("spam", "true");
	n1.putAttribute("timestamp",System.currentTimeMillis());

	// send it out
	try { hd.publish(n1); }  
	catch (SienaException se) { System.err.println("Siena exception on publish:" + se); }
    }

    /** Queries a rule. */
    private static void queryRule() {

	// make the notification containing the query
        Notification n1 = new Notification();
	n1.putAttribute("Type", "EDInput");
	n1.putAttribute("EDInput", "ManagerInstruction");
	n1.putAttribute("Action", "QueryRule");
	n1.putAttribute("Rule", "testRule");

	// send the notification out
	try { hd.publish(n1);	} 
	catch (SienaException se) { System.err.println("Siena exception on publish:" + se);  }
    }

    /**
     * asks the StateMachineManager to remove a rule to its list,
     * so we can test removing rules.
     * @author enrico buonanno
     */
    public static void removeRule(){
	// make the notification containing the rule to remove
        Notification n1 = new Notification();
	n1.putAttribute("Type", "EDInput");
	n1.putAttribute("EDInput", "ManagerInstruction");
	n1.putAttribute("Action", "RemoveRule");
	n1.putAttribute("Rule", "testRule");

	// send the notification out
	try { hd.publish(n1);	} 
	catch (SienaException se) { System.err.println("Siena exception on publish:" + se);  }
    }

    /**
     * asks the StateMachineManager to add a rule to its list,
     * so then we can test this rule.
     * @author enrico buonanno
     */
    public static void addRule(){
	// add the spamblocker rule
	try {
	    hd.publish(KXNotification.EDManagerAddRule("<rule name=\"spamblocker\"><states><state name=\"a\" timebound=\"-1\" children=\"b\" actions=\"\" fail_actions=\"\"><attribute name=\"from\" value=\"*1\"/><attribute name=\"spam\" value=\"true\"/></state><state name=\"b\" timebound=\"5\" children=\"\" actions=\"A,B\" fail_actions=\"F\"><attribute name=\"from\" value=\"*1\"/><attribute name=\"spam\" value=\"true\"/></state></states><actions><notification name=\"A\"><attribute name=\"spamblock\" value=\"*1\"/></notification><notification name=\"B\"><attribute name=\"spamblock\" value=\"*1\"/><attribute name=\"internal\" value=\"true\"/></notification><notification name=\"F\"><attribute name=\"spamblocker_failure\" value=\"true\"/></notification></actions></rule>"));
	} catch (SienaException se) {
	    System.err.println("Siena exception on publish:" + se);
	    return;
	}
    }
  
}





