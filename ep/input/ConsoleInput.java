package psl.xues.ep.input;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import org.w3c.dom.Element;

/**
 * Console interface to the Event Packager.  Primarily for administrative
 * tasks.  Only one of these can exist in a JVM.
 *
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
    BufferedReader in = null;
    PrintWriter out = null;
    
    try {
      in = new BufferedReader(new InputStreamReader(input));
      out = new PrintWriter(output, true);
    } catch(Exception e) {
      debug.error("Can't run console input", e);
      return;
    }
    
    // Print out the welcome message
    out.println("Event Packager v2.0");
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
      } else if(isCommand(command, "help")) {
        out.println("-------------------\n" +
        "Available commands:\n"+
        "- HELP: produces this output\n"+
        "- SHUTDOWN: shuts down the Event Packager cleanly\n" +
        "-------------------");
      } else if(isCommand(command, "shutdown")) {
        ep.shutdown();
        break;
      } else {
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
    
    // XXX - now we hope the user had actually used the console to initiate
    // a shutdown, in which case we have to do nothing.  Otherwise, any way
    // of terminating a readLine call?
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