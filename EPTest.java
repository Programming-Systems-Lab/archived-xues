package psl.xues;

import java.io.*;
import java.net.*;
import java.util.*;
import siena.*;

/**
 * Generates ED-like XML notification.
 * @author Janak J Parekh
 * @version 0.1
 *
 * $Log$
 * Revision 1.2  2001-07-23 17:05:32  jjp32
 * Minor change to exception handling in EPTest
 *
 * Revision 1.1  2001/06/27 18:09:42  jjp32
 *
 * Added EP test code from Rose's distro
 *
 * Revision 1.1  2001/03/08 01:29:15  jjp32
 * Oops, forgot to commit these
 *
 * Revision 1.3  2001/01/30 06:26:18  jjp32
 *
 * Lots and lots of updates.  EventDistiller is now of demo-quality.
 *
 * Revision 1.2  2001/01/30 02:39:36  jjp32
 *
 * Added loopback functionality so hopefully internal siena gets the msgs
 * back
 *
 * Revision 1.1  2001/01/30 00:24:50  jjp32
 *
 * Bug fixes, added test class
 *
 * Revision 2.2  2001/01/29 04:04:48  png3
 * Added package psl.metaparser statements.  Can you say "Oops?"
 *
 * Revision 2.1  2001/01/28 17:52:17  png3
 * New version of Metaparser: fully multithreaded.  PrintWriter logs.
 *
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
      e.printStackTrace();
    }
    

    try {
      //      hd.publish(n);
      hd.publish(n1);
      hd.publish(n2);
    } catch (SienaException se) {
      System.err.println("Siena exception on publish:" + se);
      return;
    }
    hd.shutdown();

    
  }
    
}

