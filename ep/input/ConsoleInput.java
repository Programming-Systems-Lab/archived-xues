package psl.xues.ep.input;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.StringTokenizer;

import org.w3c.dom.Element;

import psl.xues.ep.event.StringEvent;
import psl.xues.ep.event.DOMEvent;
import psl.xues.ep.event.SienaEvent;
import psl.xues.ep.store.EPStore;

import siena.Notification;

/**
 * Console interface to the Event Packager.  Primarily for administrative
 * tasks, but it can also be a source of human input.
 * <p>
 * <b>Notes:</b>
 * <ul>
 * <li>Only one of these can exist in a JVM.</li>
 * <li>For usage, type help at the console interface.
 * </ul>
 * <b>Configuration:</b> There currently isn't any.
 * <p>
 * Copyright (c) 2002: The Trustees of Columbia University in the
 * City of New York.  All Rights Reserved.
 *
 * <!--
 * TODO;
 * - Support replay, database maintenance
 * - Consider combining with configuration to support GUI frontend
 * -->
 *
 * @author Janak J Parekh <janak@cs.columbia.edu>
 * @version $Revision$
 */
public class ConsoleInput extends EPInput {
  private static boolean instantiated = false;
  private static final InputStream input = System.in;
  private static final OutputStream output = System.out;
  private BufferedReader in = null;
  private PrintWriter out = null;
  
  public ConsoleInput(EPInputInterface ep, Element el)
  throws InstantiationException {
    super(ep,el);
    if(instantiated == false) {
      instantiated = true;
    } else throw new InstantiationException("A console input already exists");
  }
  
  /**
   * Run code.  This is where we actually prompt the user.
   */
  public void run() {
    in = null;
    out = null;
    
    try {
      in = new BufferedReader(new InputStreamReader(input));
      out = new PrintWriter(output, true);
    } catch(Exception e) {
      debug.error("Can't run console input", e);
      return;
    }
    
    // Wait 2 seconds to allow things to finish.  XXX - hack - better way?
    try {
      Thread.sleep(1000);
    } catch(Exception e) { ; }
    
    // Print out the welcome message
    out.println("\n\nEvent Packager v2.0");
    out.println("Copyright (c) 2002: The Trustees of Columbia University " +
    "in the\nCity of New York.  All Rights Reserved.");
    out.println("Type HELP for console help.");
    
    // Actually listen for requests now
    String command = null;
    while(true) {
      out.print("> ");
      out.flush();
      try {
        command = in.readLine();
      } catch(Exception e) {
        debug.error("Could not read console input", e);
        break;
      }
      
      // Now try to determine if the command is important
      if(command == null) { // Bad
        out.println("Warning: null encountered, shutting down input");
        break;
      } else if(command.trim().length() == 0) {
        // ignore - it's whitespace
      }
      
      // Help
      else if(isCommand(command, "help")) {
        out.println("-------------------\n" +
        "Available commands:\n"+
        "- HELP: produces this output\n"+
        "- INJECTSTRING: injects a \"quoted string\" into EP for testing\n" +
        "- INJECTXML: injects \"quoted XML\" into EP for testing\n" +
        "- INJECTSIENA: injects a Siena notification; use\n" +
        "  {\"a1\"=(valuetype)\"data\", ...} as the parameter; supported valuetypes\n" +
        "  include String, boolean, double, or long.  If valuetype is not specified,\n" +
        "  String is assumed\n" +
        "- REPLAY <StoreName> [-s SourceName] [-t StartTime EndTime] [-o]: replay a set\n" +
        "  of events to the associated output.  If you use -t, specify BOTH start and end\n" +
        "  times, in long integer form; -o implies \"Original Time Framing\", which plays\n" +
        "  back events with the time intervals in which they were originally received.\n" +
        "- SHUTDOWN: shuts down the Event Packager cleanly\n" +
        "-------------------");
      }
      
      // Shutdown
      else if(isCommand(command, "shutdown")) {
        ep.shutdown();
        break;
      }
      
      // Inject(String|XML)
      else if(isCommand(command, "injectstring") ||
      isCommand(command, "injectxml")) {
        // Grab the actual string from the input
        StringTokenizer tok = new StringTokenizer(command, "\"");
        String data = null;
        try {
          tok.nextToken(); // Ignore the stuff outside
          data = tok.nextToken();
        } catch(Exception e) {
          out.println("Invalid format for injecting a string");
          continue;
        }
        // Encapsulate it into a StringEvent...
        StringEvent se = new StringEvent(getName(), data);
        // ...and then either ship it or convert it to DOMEvent
        if(isCommand(command, "injectstring")) {
          debug.debug("Injecting " + se);
          ep.injectEvent(se);
        } else { // XML
          DOMEvent de = (DOMEvent)se.convertEvent("DOMEvent");
          if(de == null) {
            out.println("Failed in parsing XML");
            continue;
          }
          debug.debug("Injecting " + de);
          ep.injectEvent(de);
        }
      }
      
      // InjectSiena
      else if(isCommand(command, "injectsiena")) {
        StringTokenizer tok = new StringTokenizer(command, "\"");
        // Now build the notification
        Notification n = new Notification();
        String attribute = null, rawValueType = null, valueType = null,
        data = null;
        while(tok.hasMoreTokens()) {
          try {
            // Strip out the initial command declaration, or the comma, or
            // the closing curly
            tok.nextToken();
            if(!tok.hasMoreTokens()) break; // End of statement
            // We have another Siena AttributeValue, parse
            attribute = tok.nextToken();
            rawValueType = tok.nextToken();
            // Parse rawValueType.  Handle situations where there are NO
            // tokens left for the rawValueType, i.e., there's no valueType.
            StringTokenizer tok2 = new StringTokenizer(rawValueType, "()");
            // Yes, I know this is an evil test, just trying to compact
            // the code slightly.  The inline nextToken strips out the equals
            // sign.
            if(tok2.hasMoreTokens() && tok2.nextToken() != null &&
            tok2.hasMoreTokens()) {
              valueType = tok2.nextToken();
            } else { // No valueType at all
              valueType = null;
            }
            // Finally, slurp the data
            data = tok.nextToken();
          } catch(Exception e) {
            out.println("Parse error in InjectSiena");
            e.printStackTrace();
            break;
          }
          
          // Now add to the notification
          if(valueType == null || valueType.equalsIgnoreCase("String")) {
            n.putAttribute(attribute, data);
          } else if(valueType.equalsIgnoreCase("boolean")) {
            n.putAttribute(attribute, Boolean.valueOf(data).booleanValue());
          } else if(valueType.equalsIgnoreCase("double")) {
            n.putAttribute(attribute, Double.parseDouble(data));
          } else if(valueType.equalsIgnoreCase("long")) {
            n.putAttribute(attribute, Integer.parseInt(data));
          } else {
            out.println("Invalid value type \"" + valueType + "\", try again");
            break;
          }
          
        }
        // Now publish the notification, if there's anything
        if(n.size() > 0)
          ep.injectEvent(new SienaEvent(getName(), n));
      }
      
      else if(isCommand(command, "replay")) {
        // Arguments parsing, much like main()
        StringTokenizer tok = new StringTokenizer(command);
        String storeName = null;
        String sourceName = null;
        long beginTime = -1;
        long endTime = -1;
        boolean originalTime = false;
        try {
          // Skip over command declaration; this should never fail
          tok.nextToken();
          // Now parse
          while(tok.hasMoreTokens()) {
            String param = tok.nextToken();
            if(!param.startsWith("-")) { // Must be store name
              storeName = param;
            } else if(param.equals("-s")) { // Source name
              sourceName = tok.nextToken();
            } else if(param.equals("-t")) { // Timestamp range
              beginTime = Long.parseLong(tok.nextToken());
              endTime = Long.parseLong(tok.nextToken());
            } else if(param.equals("-o")) { // Original timestamp
              originalTime = true;
            }
          }
        } catch(Exception e) {
          out.println("Error parsing replay request, try again");
          continue;
        }
        
        if(storeName == null) {
          out.println("Store name must be specified for replay");
          continue;
        } else if(sourceName == null && (beginTime == -1 || endTime == -1)) {
          out.println("Either source or timestamp range must be specified");
          continue;
        }
        
        // Now replay!
        EPStore eps = ep.getStore(storeName);
        if(eps.playbackEvents(getName(), sourceName, beginTime, endTime, 
        originalTime) == false) {
          out.println("No events to replay!");
        }
      }
      // Invalid
      else {
        // If we're in ep-shutdown, let bygones be bygones
        if(ep.inShutdown()) break;
        else out.println("Invalid command; please try again, or try HELP.");
      }
    }
  }
  
  /**
   * Shutdown method.  Overridden from EPInput to eliminate warnings about
   * thread shutdown.
   */
  public void shutdown() {
    shutdown = true;
    // Kill console input now
    try {
      in.close();
    } catch(Exception e) {
      debug.warn("Couldn't shut down console input", e);
    }
  }
  
  /**
   * Get the "type" of input.
   *
   * @return The type, as String.  Usually, it should be the class name without
   * the package identification.
   */
  public String getType() {
    return "ConsoleInput";
  }
  
  /**
   * Useful string mechanism for disambiguating commands.
   */
  public boolean isCommand(String input, String command) {
    // See if the command is the beginning of the input
    if(input == null || command == null || input.length() < command.length()) {
      return false; // Can't be
    }
    if(input.substring(0,command.length()).equalsIgnoreCase(command))
      return true;
    else return false;
  }
}