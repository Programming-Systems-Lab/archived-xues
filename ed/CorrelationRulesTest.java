/*
 * CorrelationRulesTest.java
 *
 * Created on January 16, 2002, 4:37 PM
 */
package psl.xues.ed;

import psl.util.siena.*;
import siena.*;

/**
 * The CorrelationRules tester for Peppo.
 *
 * @author Janak J Parekh <janak@cs.columbia.edu>
 * @version 1.0
 */
public class CorrelationRulesTest {
  
  /** CTOR. */
  public CorrelationRulesTest(String sienaHost) {
    // Start up a new AllSubscriber
    AllSubscriber as = new AllSubscriber(sienaHost);
    // Get its hierarchicalDispatcher
    HierarchicalDispatcher hd = as.getSiena();
    try {
      // Now make some publications
      Notification n1 = new Notification();
      n1.putAttribute("classname", "cselt.im.server.presence.PresenceServer");
      n1.putAttribute("methodname",
      "PresenceServer(int,int,String,String,String)");
      n1.putAttribute("callback", "before");
      n1.putAttribute("ipAddr", "128.59.23.7");
      n1.putAttribute("ipPort", "6789");
      n1.putAttribute("Type", "EDInput");
      n1.putAttribute("time", "10000");
      n1.putAttribute("timestamp", 10000);
      //    n1.putAttribute(EDConst.TIME_ATT_NAME, 10000);
      hd.publish(n1);
      Notification n2 = new Notification();
      n2.putAttribute("classname", "cselt.im.dbexec.DBException");
      n2.putAttribute("methodname", "DBException()");
      n2.putAttribute("callback", "before");
      n2.putAttribute("name", "DBException");
      n2.putAttribute("ipAddr", "128.59.23.7");
      n2.putAttribute("ipPort", "6789");
      n2.putAttribute("Type", "EDInput");
      n2.putAttribute("timestamp", 10010);
      //    n2.putAttribute(EDConst.TIME_ATT_NAME, 10010);
      hd.publish(n2);
    } catch(SienaException e) {
      e.printStackTrace();
    }
    
    // Now sleep for 20 seconds
    try {
      Thread.currentThread().sleep(20000);
    } catch(InterruptedException e2) { e2.printStackTrace(); }
    
    // Now shutdown
    System.exit(0);
  }
  
  
  /**
   * Main method.
   *
   * @param args the command line arguments
   */
  public static void main(String args[]) {
    if(args.length != 1) {
      System.err.println("usage: java CorrelationRulesTest <SENP URL>");
      System.err.println("Make sure EventDistiller and Siena are already" +
      " running.");
    } else {
      new CorrelationRulesTest(args[0]);
    }
  }
}