package psl.xues.ed.tests;

import java.io.*;
import java.net.*;
import java.util.*;
import siena.*;
import psl.xues.ed.*;
import psl.xues.util.EDConst;

/**
 * Generates ED-like XML notification.
 * <p>
 * Copyright (c) 2000-2002: The Trustees of Columbia University and the
 * City of New York.  All Rights Reserved.
 *
 * @author Janak J Parekh
 * @version $Revision$
 */
public class EDTest implements Notifiable {
  
  /** Constructor. */
  public static void main(String[] args) {
    // Make an instance of us so that we can receive notifications
    new EDTest(args);
  }

  public EDTest(String[] args) {
    String xml = null;

    if(args.length == 0 || args[0].equals("-?")) {
      System.out.println("Usage: java EDTest <senp url>");
      System.exit(0);
    }
    
    HierarchicalDispatcher hd = new HierarchicalDispatcher();
    try {
      hd.setMaster(args[0]);
    } catch(Exception e) {
      e.printStackTrace();
      return;
    }

    Notification n1 = new Notification();
    n1.putAttribute("Source", "EventDistiller");
    n1.putAttribute("SourceID", 12345);
    n1.putAttribute("Type", "EDInput");
    n1.putAttribute("from", "Janak");
    n1.putAttribute("spam", "true");
    n1.putAttribute(EDConst.TIME_ATT_NAME,System.currentTimeMillis());

//     Filter f = new Filter();
//     f.addConstraint("foo","bar");
//     System.err.println(f);

    /* Subscribe to all events */
    /*    Filter f = new Filter();
	  f.addConstraint("foo", new AttributeConstraint(Op.ANY, ""));*/

    try {
      /*      hd.subscribe(f, this);*/
      hd.publish(n1);
      hd.publish(n1);
      while(true) {
	try {
	  Thread.currentThread().sleep(1000);
	} catch(InterruptedException ie) { ; }
      } 
    } catch (SienaException se) {
      System.err.println("Siena exception on publish:" + se);
      return;
    }
    //    hd.shutdown();
  }

  public void notify(Notification n) {
    System.err.println("[EDTest] Received notification " + n);
  }

  public void notify(Notification[] s) { ; }

}



