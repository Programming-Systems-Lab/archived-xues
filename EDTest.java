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
 * Revision 1.1  2001-03-08 01:29:15  jjp32
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
public class EDTest {
  
  /** Constructor */
  public static void main(String[] args) {
    
    String xml = null;

    if(args.length == 0 || args[0].equals("-?")) {
      System.out.println("Usage: java EDTest <senp url>");
      System.exit(0);
    }
    
    HierarchicalDispatcher hd = new HierarchicalDispatcher();
    try {
      hd.setReceiver(new TCPPacketReceiver(61981));
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
    n1.putAttribute("Source", "EventDistiller");
    n1.putAttribute("SourceID", 12345);
    n1.putAttribute("Type", "DirectEvent");
    n1.putAttribute("from", "Janak");
    n1.putAttribute("spam", "true");
    n1.putAttribute("timestamp",System.currentTimeMillis());

    Filter f = new Filter();
    f.addConstraint("foo","bar");
    System.err.println(f);

    try {
      //      hd.publish(n);
      hd.publish(n1);
      hd.publish(n1);
    } catch (SienaException se) {
      System.err.println("Siena exception on publish:" + se);
      return;
    }
    hd.shutdown();
  }
}
