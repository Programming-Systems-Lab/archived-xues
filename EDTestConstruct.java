package psl.xues;

import java.io.*;
import java.util.*;
import siena.*;

/**
 * Tester for the embedded constructor.
 * @author enrico buonanno
 * @version 0.1
 */
public class EDTestConstruct implements Notifiable {

    /** the EventDistiller. */
    EventDistiller ed;
  
    /** Starts the test. */
    public static void main(String[] args) {
	new EDTestConstruct();
    }

    /** Constructs a new EDTestConstruct. */
    public EDTestConstruct() {
	// instantiate new ED
	ed = new EventDistiller(this, "psl/xues/SampleRules.xml", false, true, true);
	// give it an input...
	// and an output -- optional
	//ed.setOutputFile(new File("psl/xues/currentRulebase.xml"));

	/* send it a couple of events, 
	   to test spamblocker rule */
	//for (int i = 1; i <= 10; i++) sendEvent(10 * i);
	// use delay to simulate time-out
	/*try { Thread.currentThread().sleep(600); }
	  catch (Exception ex) { ; }*/
	//sendEvent();

	
	// test the loop rule
	//double d = (new Random()).nextDouble();
	int n = 10; //(int)(d * 10);
	for (int i = 1; i < n; i++) sendLoopEvent(10 * i);
	//sendEndEvent(n * 10);
	

	// let ED run for a bit...
	try { Thread.currentThread().sleep(15000); }
	catch (Exception ex) { ; }
	
	// then kill it
	ed.shutdown();
    }

    /** Sends a "spam" event, using present time for timestamp. */
    private void sendEvent() { sendEvent(System.currentTimeMillis()); }

    /** Sends an event, with a given timestamp. */
    private void sendEvent(long l) {
	// make event
	Notification n1 = new Notification();
	n1.putAttribute("Source", "EventDistiller");
	n1.putAttribute("SourceID", 12345);
	n1.putAttribute("Type", "EDInput");
	n1.putAttribute("from", "Janak");
	n1.putAttribute("spam", "true");
	n1.putAttribute("timestamp", l);

	// send it
	ed.notify(n1);   
    }

    /** Sends a "loop" event, using present time for timestamp. */
    private void sendLoopEvent() { sendEvent(System.currentTimeMillis()); }

    /** Sends a 'loop' event.  */
    private void sendLoopEvent(long l) {
	// make event
	Notification n1 = new Notification();
	n1.putAttribute("Source", "EventDistiller");
	n1.putAttribute("SourceID", 12345);
	n1.putAttribute("Type", "EDInput");
	n1.putAttribute("event", "loop");
	n1.putAttribute("timestamp", l);

	// send it
	ed.notify(n1);   
    }

    /** Sends a "loop" event, using present time for timestamp. */
    private void sendEndEvent() { sendEvent(System.currentTimeMillis()); }

    /** Sends an event.  */
    private void sendEndEvent(long l) {
	// make event
	Notification n1 = new Notification();
	n1.putAttribute("Source", "EventDistiller");
	n1.putAttribute("SourceID", 12345);
	n1.putAttribute("Type", "EDInput");
	n1.putAttribute("event", "end");
	n1.putAttribute("timestamp", l);

	// send it
	ed.notify(n1);   
    }

    /** Siena Callback. */
    public void notify(Notification n) {
	System.out.println("OWNER: received notification:" + n);
    }

    /** Unused Siena construct. */
    public void notify(Notification[] s) { ; }
}
