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
 * Revision 1.2  2001-05-21 00:43:04  jjp32
 * Rolled in Enrico's changes to main Xues trunk
 *
 * Revision 1.1.4.1  2001/05/02 00:04:27  eb659
 *
 * Tested and fixed a couple of things.
 * New architecture works, and can be tested using EDTest.
 * Reaper thread needs revision...
 * Can we get rid of internal 'loopback' notifications?
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
      hd.setReceiver(new TCPPacketReceiver(61981));
      hd.setMaster(args[0]);
    } catch (InvalidSenderException ise) {
      System.err.println("Invalid Sender:" + ise);
      return;
    } catch (IOException ioe) {
      System.err.println("Unable to set hd receiver:" + ioe);
      return;
    }

    Notification n1 = new Notification();
    n1.putAttribute("Source", "EventDistiller");
    n1.putAttribute("SourceID", 12345);
    n1.putAttribute("Type", "EDInput");
    n1.putAttribute("from", "Janak");
    n1.putAttribute("spam", "true");
    n1.putAttribute("timestamp",System.currentTimeMillis());

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



