package psl.xues.ed;

import psl.kx.KXNotification;
import java.io.*;
import java.net.*;
import java.util.*;
import siena.*;

/**
 * Tester for the functionality in the OR representation.
 * @author enrico buonanno
 * @version 0.1
 */
public class EDTestOR {

    /** the Siena dispatcher. */
    HierarchicalDispatcher hd;
  
    /**
     * Starts the test. 
     * @param args the command line arguments. These include: 
     *        the siena server to subscribe to, preceded by '-s'
     *        the sequence of events to send (e.g. 'abc'), preceded by '-e'
     */
    public static void main(String[] args) {
	String sienaHost = "", 
	    eventSequence = "";
	if(args.length > 0) { // Siena host specified?
	    for(int i=0; i < args.length; i++) {
		if(args[i].equals("-s"))
		    sienaHost = args[++i];
		else if(args[i].equals("-e"))
		    eventSequence = args[++i];
		else
		    usage();
	    }
	}   
	else usage();
	new EDTestOR(sienaHost, eventSequence);
    }

    /** Prints the usage instructions. */
    static void usage() {
	System.out.println("usage: java EDTersOR [-s sienaHost] [-e eventSequence]");
	System.exit(0);
    }

    /**
     * Constructs a new EDTestOR.
     * @param sienaHost the host to subscribe to
     * @param eventSequence the sequence of events to send.
     */
    public EDTestOR(String sienaHost, String eventSequence) {
	// subscribe 
	hd = new HierarchicalDispatcher();
	try {
	    hd.setReceiver(new TCPPacketReceiver(0));
	    hd.setMaster(sienaHost);
	} catch (InvalidSenderException ise) {
	    System.err.println("Invalid Sender:" + ise);
	    return;
	} catch (IOException ioe) {
	    System.err.println("Unable to set hd receiver:" + ioe);
	    return;
	}

	// send events - leaving enough time in between
	for (int i = 0; i < eventSequence.length(); i++) {
	    sendEvent(eventSequence.substring(i, i + 1));
	    try { Thread.currentThread().sleep(500); }
	    catch(InterruptedException e) { System.err.println(e); }
	}

    }

    /**
     * Sends an event with the given name.
     * @param name the name of the event to send
     */
    private void sendEvent(String name) {
	System.out.println("sending event: " + name);

	// make event
	Notification n1 = new Notification();
	n1.putAttribute("Type","EDInput");
	n1.putAttribute("event", name);
	n1.putAttribute(EDConst.TIME_ATT_NAME, System.currentTimeMillis());

	// send it out
	try { hd.publish(n1); }  
	catch (SienaException se) { System.err.println("Siena exception on publish:" + se); }
    }
}
