package psl.xues.ep;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.lf5.DefaultLF5Configurator;

import psl.xues.ep.event.EPEvent;
import psl.xues.ep.input.EPInput;
import psl.xues.ep.input.EPInputInterface;
import psl.xues.ep.output.EPOutput;
import psl.xues.ep.output.EPOutputInterface;
import psl.xues.ep.transform.EPTransform;
import psl.xues.ep.transform.EPTransformInterface;
import psl.xues.ep.store.EPStore;
import psl.xues.ep.store.EPStoreInterface;

/**
 * Event Packager for XUES.
 * <p>
 * Copyright (c) 2002: The Trustees of Columbia University in the
 * City of New York.  All Rights Reserved.
 *
 * <!--
 * TODO:
 * - Implement outputter dispatch threads (or should we just continue to trust
 *   that the outputter will be fast?)
 * - Consider executing rules in parallel...
 * -->
 *
 * @author Janak J Parekh <janak@cs.columbia.edu>
 * @version $Revision$
 */
public class EventPackager implements Runnable, EPInputInterface,
EPOutputInterface, EPTransformInterface, EPStoreInterface {
  /** Log4j logger class */
  static Logger debug =
  Logger.getLogger(EventPackager.class.getName());
  
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
  /** Transforms */
  HashMap transformers = null;
  /** Stores */
  HashMap stores = null;
  /** Rules */
  HashMap rules = null;
  
  /** Event dispatch queue */
  List eventQueue = null;
  /** Dequeue thread */
  Thread dequeueThread = null;
  
  /** Has shutdown been initiated? */
  private boolean shutdown = false;
  /** Is the dequeue thread (rule engine) still running? */
  private boolean dequeueing = false;
  /** Is shutdown actually proceeding? */
  private boolean inShutdown = false;

  /** Reference to the EPConfiguration class */
  private EPConfiguration epc = null;
  
  /**
   * Default embedded CTOR.
   *
   * @param configFile The configuration file for EP's initial config.
   */
  public EventPackager(String configFile) { 
    this(configFile, false, null); 
  }
  
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
    epc = new EPConfiguration(configFile, this);
    
    // Initialize the event queue.  Make it synchronized.
    eventQueue = Collections.synchronizedList(new ArrayList());
    
    // Shutdown hook
    Runtime.getRuntime().addShutdownHook(new Thread("Shutdown") {
      public void run() {
        shutdown();
      }
    });
    
    // Start ourselves up.  XXX - do we want to do this permanently?
    new Thread(this).start();
  }
  
  /**
   * Main run context.
   */
  public void run() {
    // "Start" each of the components.  Inputters last, since they will
    // actually start moving data.
    synchronized(outputters) {
      Iterator i = outputters.keySet().iterator();
      while(i.hasNext()) {
        String outputterName = (String)i.next();
        new Thread((EPOutput)outputters.get(outputterName),
        outputterName).start();
      }
    }
    
    synchronized(inputters) {
      Iterator i = inputters.keySet().iterator();
      while(i.hasNext()) {
        String inputterName = (String)i.next();
        new Thread((EPInput)inputters.get(inputterName),
        inputterName).start();
      }
    }
    
    // Store a reference to our current thread
    dequeueThread = Thread.currentThread();
    dequeueThread.setName("EPDequeueThread");
    
    // Start the dequeue loop
    while(!shutdown) {
      dequeueing = true;
      while(eventQueue.size() > 0) {
        // Dequeue the first element
        EPEvent epe = (EPEvent)eventQueue.remove(0);
        debug.debug("Dequeued event " + epe + ", processing");
        // Process it: get the correct inputter and fire it through each of
        // its rules in turn
        EPInput epi = (EPInput)inputters.get(epe.getSource());
        if(epi == null) {
          debug.error("Could not find EPInput \"" + epe.getSource() + "\"," +
          " invalid source?");
          continue;
        }
        epi.addCount(); // Keep count of this firing
        EPRule[] temprules = epi.getCurrentRules();
        for(int i=0; i < temprules.length; i++) {
          // Push it through this rule
          temprules[i].processRule(epe);
        }
      }
      
      // Nothing to do, time to catch a nap.  XXX - handle
      // InterruptedExceptions better?
      try {
        Thread.currentThread().sleep(1000);
      } catch(Exception e) { ; }
    }
    
    // No longer in dequeue loop
    dequeueing = false;
    
    // If we get here, time to actually run the shutdown
    doShutdown();
  }
  
  /**
   * Initiate shutdown.  We need this bizarre wrapped shutdown in case the
   * dequeue thread gets bogged down.
   */
  public void shutdown() {
    // If we're already in shutdown, do nothing
    if(shutdown == true) return;
    
    // Commence shutdown otherwise
    debug.info("Initiating EP shutdown...");
    
    // Stop ourselves from doing anything "new" first
    shutdown = true;
    
    // Now, wait up to 20 seconds (1 second at a time), and if we're not in
    // shutdown by then, forcibly put ourselves in shutdown.  Kind of hacky -
    // a "poll-then-sleep" cycle.
    for(int i=0; i < 20; i++) {
      try {
        Thread.currentThread().sleep(1000);
      } catch(Exception e) { ; }
      if(dequeueing == false || dequeueThread == null) {
        // We are shutting down
        break;
      }
    }
    
    // We've waited... are we shutting down???
    if(dequeueing == true && dequeueThread != null) {
      debug.warn("Shutdown did not start yet, attempting forcible shutdown");
      dequeueThread.interrupt();
      dequeueing = false;
      doShutdown();
    }
  }
  
  /**
   * Actually handle the shutdown.
   */
  private void doShutdown() {
    if(inShutdown == true)
      return;  // Another thread is doing it, ignore
    
    debug.info("Shutting down EP...");
    inShutdown = true;
    
    // Now shut down the inputters
    synchronized(inputters) {
      Iterator i = inputters.values().iterator();
      while(i.hasNext()) {
        ((EPInput)i.next()).shutdown();
      }
    }
    // ... and now the outputters
    synchronized(outputters) {
      Iterator i = outputters.values().iterator();
      while(i.hasNext()) {
        ((EPOutput)i.next()).shutdown();
      }
    }
    
    // (note, transforms are currently not supported for shutdown purposes.
    //  this may change in the future)
    
    // ... finally, the stores
    synchronized(stores) {
      Iterator i = stores.values().iterator();
      while(i.hasNext()) {
        ((EPStore)i.next()).shutdown();
      }
    }
    
    debug.info("EP shutdown process complete (process may not terminate if " +
    "there are hung threads).");
  }
  
  /**
   * Are we in shutdown?
   *
   * @return A boolean indicating true if we're in shutdown.
   */
  public boolean inShutdown() { return shutdown; }
  
  /**
   * EP main method.
   *
   * @param args The command-line arguments to EP.
   */
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
    } else {                                     // No debug-level stuff at all
      BasicConfigurator.configure();
      Logger.getRootLogger().setLevel(Level.INFO);
    }
  }
  
  /**
   * Inject an event into the Event Packager.  Use any supported EPEvent
   * format.
   *
   * @param epe The EPEvent you wish to inject.
   * @return A boolean indicating success.
   */
  public boolean injectEvent(EPEvent epe) {
    // If no source, fail
    if(epe.getSource() == null) {
      debug.error("Received event without source, cannot process");
      return false;
    }
    
    // Verify timestamp and source?
    if(epe.getTimestamp() == -1) {
      debug.warn("Received event without timestamp, putting current time");
      epe.setTimestamp(System.currentTimeMillis());
    }
    
    // Now "inject"
    debug.debug("Injecting event " + epe);
    eventQueue.add(epe);
    return true;
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
   * @param src The source of this error.
   * @param err The error to report.
   */
  public void error(String src, String err) {
    debug.error(src + ": " + err);
  }
  
  /**
   * Get a handle to an EPStore.
   *
   * @param storeName The name of the store you want a handle to.
   * @return The EPStore reference, or null.
   */
  public EPStore getStore(String storeName) {
    return (EPStore)stores.get(storeName);
  }
}