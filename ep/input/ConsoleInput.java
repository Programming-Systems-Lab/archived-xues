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
        "  {\"a1\"=(valuetype)\"data\", ...} as the parameter; supported\n" +
        "  valuetypes include String, boolean, double, or long.  If\n" +
        "  valuetype is not specified, String is assumed\n" +
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
        Notification n = new Notification();
        String attribute = null, rawValueType = null, valueType = null,
        data = null;
        while(tok.hasMoreTokens()) {
          try {
            attribute = tok.nextToken();
            rawValueType = tok.nextToken();
            // Parse rawValueType.  Handle situations where there are NO
            // tokens left for the rawValueType, i.e., there's no valueType.
            StringTokenizer tok2 = new StringTokenizer(rawValueType, "()");
            if(tok2.hasMoreTokens()) tok2.nextToken(); // Skip =
            valueType = (tok2.hasMoreTokens()) ? tok2.nextToken() : null;
            // Parse data
            data = tok.nextToken();
          } catch(Exception e) {
            out.println("Parse error in InjectSiena: " + e);
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
          
          // Now publish the notification
          ep.injectEvent(new SienaEvent(getName(), n));
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