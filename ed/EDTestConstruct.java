package psl.xues.ed;

import java.io.*;
import java.util.*;
import siena.*;

/**
 * Tester for the embedded constructor.
 * @author enrico buonanno
 * @version $Revision$
 */
public class EDTestConstruct implements Notifiable {
  
  /** the EventDistiller. */
  EventDistiller ed;
  
  /** the rule to test. */
  static String rule = "spamblocker";
  
  /** whether we test the failure of the rule. */
  static boolean fail = false;
  
  /** whether the rules are written to output on shutdown. */
  static boolean output = false;
  
  /**
   * Starts the test. Arguments are:
   * -r [rulename] the name of the rule to test
   * -f if you want to test the failure for that rule
   * -o if you want the rules to be written to output file
   */
  public static void main(String[] args) {
    for(int i=0; i < args.length; i++) {
      if(args[i].equals("-f"))
        fail = true;
      else if(args[i].equals("-r"))
        rule = args[++i];
      else if(args[i].equals("-o"))
        output = true;
    }
    new EDTestConstruct();
  }
  
  /** Constructs a new EDTestConstruct. */
  public EDTestConstruct() {
    
    // instantiate new ED
    ed = new EventDistiller(this, "psl/xues/ed/SampleRules.xml", true, null, 
    true, null);
    
    // and an output -- optional
    if (output) ed.setOutputFile(new File("psl/xues/ed/currentRulebase.xml"));
    
    // test spamblocker rule
    if(rule.equals("spamblocker")) {
      sendSpamEvent(1000000);
      if(fail) {
        // wait beyond timebound
        sendSpamEvent(1000200);
      }
      else {
        sendSpamEvent(1000005);
      }
    }
    
    // test counter feature
    else if(rule.equals("counter")) {
      for(int i = 1; i < 10; i++) {
        sendEvent("counter", i*10);
      }
      if (!fail) {
        sendEvent("counter", 100);
      }
    }
    
    // test loop feature
    else if(rule.equals("loop")) {
      for(int i = 1; i < 10; i++) {
        sendEvent("loop", i*10);
      }
      if (!fail) {
        sendEvent("end", 100);
      }
    }
    
    //for (int i = 1; i <= 10; i++) sendSpamEvent(10 * i);
    // use delay to simulate time-out
        /*try { Thread.currentThread().sleep(600); }
          catch (Exception ex) { ; }*/
    //sendSpamEvent();
    
    
    // test the loop rule
    //double d = (new Random()).nextDouble();
    //int n = 10; //(int)(d * 10);
    //for (int i = 1; i < n; i++) sendLoopEvent(10 * i);
    //sendEndEvent(n * 10);
    
    // test inequality comparison in "temperature" rule
    else if(rule.equals("temperature")) {
      if(fail) {
        sendTemperatureEvent(90);
      }
      else {
        sendTemperatureEvent(110);
      }
    }
    
    // rule not recognized
    else {
      System.out.println("tester for rule '" + rule + "' not found");
      System.exit(0);
    }
    
    // let ED run for a bit...
    try { Thread.currentThread().sleep(15000); }
    catch (Exception ex) { ; }
    
    // then kill it
    ed.shutdown();
  }
  
  /** Sends a "spam" event, using present time for timestamp. */
  private void sendSpamEvent() { sendSpamEvent(System.currentTimeMillis()); }
  
  /** Sends a spamblock event, with a given timestamp. */
  private void sendSpamEvent(long l) {
    // make event
    Notification n1 = new Notification();
    //n1.putAttribute("Source", "EventDistiller");
    //n1.putAttribute("SourceID", 12345);
    n1.putAttribute("Type", "EDInput");
    n1.putAttribute("from", "Janak");
    n1.putAttribute("spam", "true");
    n1.putAttribute(EDConst.TIME_ATT_NAME, l);
    
    // send it
    ed.notify(n1);
  }
  
  /** Sends a "temperature" event, using present time for timestamp. */
  private void sendTemperatureEvent(int temp){
    // make event
    Notification notification = new Notification();
    notification.putAttribute("Type", "EDInput");
    notification.putAttribute("temperature", temp);
    //notification.putAttribute("spam", "true");
    notification.putAttribute(EDConst.TIME_ATT_NAME, (long)System.currentTimeMillis());
    // send it
    ed.notify(notification);
  }
  
  /** Sends a "loop" event, using present time for timestamp. */
  private void sendEvent(String eventName) { sendEvent(eventName, System.currentTimeMillis()); }
  
  /** Sends a 'loop' event.  */
  private void sendEvent(String eventName, long l) {
    // make event
    Notification n1 = new Notification();
    n1.putAttribute("Type", "EDInput");
    n1.putAttribute("event", eventName);
    n1.putAttribute("timestamp", l);
    
    // send it
    ed.notify(n1);
  }
  
  /** Sends a "loop" event, using present time for timestamp. */
  private void sendEndEvent() { sendEndEvent(System.currentTimeMillis()); }
  
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
