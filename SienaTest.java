package psl.xues;

import java.io.*;
import java.net.*;
import java.util.*;
import siena.*;

/** Generates ED-like XML notification.
  * One argument: xml message to include.
  *
  * $Log$
  * Revision 1.1  2001-01-30 00:24:50  jjp32
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
public class SienaTest {
  
  /** Constructor */
  public static void main(String[] args) {
    
    String xml = null;
    
    
    HierarchicalDispatcher hd = new HierarchicalDispatcher();
    try {
      hd.setReceiver(new TCPPacketReceiver(61980));
      hd.setMaster("senp://canal.psl.cs.columbia.edu:4321");
    } catch (InvalidSenderException ise) {
      System.err.println("Invalid Sender:" + ise);
      return;
    } catch (IOException ioe) {
      System.err.println("Unable to set hd receiver:" + ioe);
      return;
    }

    Notification n = new Notification();
    n.putAttribute("Source", "EventDistiller");
    n.putAttribute("SourceID", 12345);
    n.putAttribute("Type", "DirectEvent");
    n.putAttribute("foo", "bar");

    try {
      hd.publish(n);
    } catch (SienaException se) {
      System.err.println("Siena exception on publish:" + se);
      return;
    }
    hd.shutdown();
  }
}
