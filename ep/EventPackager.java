package psl.xues.ep;

import org.apache.log4j.Category;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;

import java.util.HashSet;
import java.util.HashMap;

import psl.xues.ep.event.EPEvent;
import psl.xues.ep.input.EPInputInterface;

/**
 * Event Packager for XUES.
 *
 * Copyright (c) 2002: The Trustees of Columbia University in the
 * City of New York.  All Rights Reserved.
 *
 * TODO:
 * - Implement outputter dispatch threads (or should we just continue to trust 
 *   that the outputter will be fast?)
 *
 * @author Janak J Parekh <janak@cs.columbia.edu>
 * @version $Revision$
 */
public class EventPackager implements Runnable, EPInputInterface {
  /** log4j category class */
  static Category debug =
  Category.getInstance(EventPackager.class.getName());
  
  /** Base CTOR. */
  public EventPackager() { this(null); }

  /** Default configuration file */
  public static final String defaultConfigFile = "EPConfig.xml";

  /** 
   * "Tested" event formats - meaning they were successfully loaded into
   * the JVM.
   */
  HashSet eventFormats = null;
  
  /** Inputters */
  HashMap inputters = null;
  
  /** Outputters */
  HashMap outputters = null;
  
  /**
   * Default embedded CTOR.
   *
   * @param configFile The configuration file for EP's initial config.
   */
  public EventPackager(String configFile) { this(configFile, false, null); }
  
  /**
   * Full CTOR.
   *
   * @param configFile The configuration file for EP's initial config.
   * @param debugging Enable debugging?
   * @param debugFile Specify log4j-compliant debug specification file, or
   * null.
   */
  public EventPackager(String configFile, boolean debugging, String debugFile) {
    // Initialize the debugging context
    initDebug(debugging, debugFile);
    
    // Parse the configuration file
    EPConfiguration epc = new EPConfiguration(configFile, this);
    
    
  }
  
  public void run() {
    
  }
  
  /** Main. */
  public static void main(String args[]) {
    String configFile = defaultConfigFile, debugFile = null;
    boolean debugging = false;
    
    if(args.length > 0) {
      for(int i=0; i < args.length; i++) {
        if(args[i].equals("-c"))
          configFile = args[++i];
        else if(args[i].equals("-d"))
          debugging = true;
        else if(args[i].equals("-df")) {
          debugging = true;
          debugFile = args[++i];
        } else
          usage();
      }
    }
    
    new EventPackager(configFile, debugging, debugFile);
  }
  
  /** Prints usage. */
  public static void usage() {
    System.err.println("\nUsage: java EventPackager [-c configFile] [-d]");
    System.err.println("\t-c: Specify configuration via XML config file");
    System.err.println("\t-d: Turn on basic (console) debugging");
    System.err.println("\t-df: Specify log4j-compliant logging config file");
    System.exit(-1);
  }
  
  /** 
   * Initialize debugging.
   * 
   * @param debug Enable debugging at all?
   * @param debugFile Custom log4j debugging configuration file 
   */
  private static void initDebug(boolean debug, String debugFile) {
    // Set up logging
    if(debug == true && debugFile == null) {
      BasicConfigurator.configure();             // Basic (all) debugging)
    } else if(debug == true && debugFile != null) {
      PropertyConfigurator.configure(debugFile); // Log4j format file
    } else {                                     // No debugging at all
      BasicConfigurator.configure();
      // Deprecated
      // BasicConfigurator.disableDebug();
      Category.getDefaultHierarchy().disableDebug();
    }
  }

  /**
   * Inject an event into the Event Packager.  Use any supported EPEvent
   * format.
   *
   * @param e The EPEvent you wish to inject.
   * @return A boolean indicating success.
   */
  public boolean injectEvent(EPEvent e) {
    debug.warn("injectEvent not implemented yet");
    return false;
  }

  /**
   * Get a list of supported EPEvent-based event formats.
   *
   * @return A list of Strings with the types of event formats.
   */
  public String[] getSupportedEventFormats() {
    debug.warn("getSupportedEventFormats not implemented yet");
    return null;
  }

  /**
   * Report an error, which will probably get passed to EventPackager's
   * logger verbatim.  ONLY use this if you don't have your own logger
   * (yet) for a good reason.  (You should consider instantiating log4j
   * somewhere...)
   *
   * @param err The error to report.
   */
  public void error(String src, String err) {
    debug.error(src + ": " + err);
  }

}