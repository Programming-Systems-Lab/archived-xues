package psl.xues.ep;

import java.io.*;
import java.net.*;
import java.util.*;
import siena.*;

/**
 * EP test suite.
 *
 * @author Janak J Parekh <janak@cs.columbia.edu>
 * @version $Revision$
 */
public class EPTest {
  
  /** Constructor */
  public static void main(String[] args) {
    
    String xml = null;
    Socket testSocket = null;
    PrintWriter out = null;
    
    
    if(args.length == 0 || args[0].equals("-?")) {
      System.out.println("Usage: java EDTest <senp url>");
      System.exit(0);
    }
    
    HierarchicalDispatcher hd = new HierarchicalDispatcher();
    try {
      hd.setReceiver(new TCPPacketReceiver(8654));
      hd.setMaster(args[0]);
    } catch (InvalidSenderException ise) {
      System.err.println("Invalid Sender:" + ise);
      return;
    } catch (IOException ioe) {
      System.err.println("Unable to set hd receiver:" + ioe);
      return;
    }
    
  /*    Notification n = new Notification();
        n.putAttribute("Source", "EventDistiller");
        n.putAttribute("SourceID", 12345);
        n.putAttribute("Type", "DirectEvent");
        n.putAttribute("foo", "bar");
   */
    
    Notification n1 = new Notification();
    n1.putAttribute("Source", "EventPackager");
    n1.putAttribute("SourceID", 12345);
    n1.putAttribute("Type", "SmartEvent");
    n1.putAttribute("from", "Janak");
    n1.putAttribute("spam", "true");
    n1.putAttribute("SmartEvent", "xxxxxxxxxxxxxxxxxxxx");
    n1.putAttribute("timestamp",System.currentTimeMillis());
    
    // set up filter for EPResultRow here...
    
    
    Notification n2 = new Notification();
    n2.putAttribute("Type", "EPLookup");
    n2.putAttribute("Start", "989882757237");
    n2.putAttribute("End", "989886002306");
    n2.putAttribute("MaxResults", "4");
    n2.putAttribute("LookupType", "smartevent");
    
    Notification n3 = new Notification();
    n3.putAttribute("type", "addFilter");
    n3.putAttribute("keepFilter", true);
    n3.putAttribute("Name", "cd");
    n3.putAttribute("AttrName", "Name");
    n3.putAttribute("AttrOp", "=");
    n3.putAttribute("AttrVal", "Check");
    n3.putAttribute("ActVal", "none");
    
    Notification n4 = new Notification();
    n4.putAttribute("Name", "Check");
    
    Filter f = new Filter();
    f.addConstraint("foo","bar");
    System.err.println(f);
    
    try {
      //now send info through sockets
      testSocket = new Socket("localhost", 7777);
      out = new PrintWriter(testSocket.getOutputStream(), true);
      out.println("asdfwewcewf");
      out.close();
      testSocket.close();
    } catch (Exception e) {
      System.err.println(e);
    }
    
    
    try {
      //publish the test notifications
      hd.publish(n1);
      hd.publish(n2);
      hd.publish(n3);
      hd.publish(n4);
      
    } catch (SienaException se) {
      System.err.println("Siena exception on publish:" + se);
      return;
    }
    hd.shutdown();
    
    
    try {
      //now send info through sockets
      testSocket = new Socket("localhost", 7777);
      out = new PrintWriter(testSocket.getOutputStream(), true);
      out.println("asdfwewcewf");
      out.close();
      testSocket.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
    
  }
}

