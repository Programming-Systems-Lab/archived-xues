package psl.xues.ed;

import psl.kx.*;

import java.io.*;
import java.util.*;

import psl.xues.ed.acme.EDGaugeMgr;
import psl.xues.util.SienaUtils;

import siena.*;

import org.apache.log4j.Logger;
import org.apache.log4j.Level;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;

/**
 * The Event Distiller.
 * <p>
 * Copyright (c) 2000-2002: The Trustees of Columbia University and the
 * City of New York.  All Rights Reserved.
 *
 * <!--
 * TODO:
 * - Fix timestamp warning if the timestamp attribute is bad
 * - Support more than 1 sec reordering time
 * - Make getTime static
 * -->
 *
 * @author Janak J Parekh, parts by Enrico Buonnano
 * @version $Revision$
 */
public class EventDistiller implements Runnable, Notifiable {
  /** Log4j logging class */
  static Logger debug = Logger.getLogger(EventDistiller.class.getName());
  /** Is debugging enabled? */
  private static boolean debugEnabled = false;
  
  /**
   * We maintain a stack of events to process - this way incoming
   * callbacks don't get tied up - they just push onto the stack.
   */
  private Vector eventProcessQueue = new Vector();
  
  /** My main execution context */
  //private Thread edContext = null;
  
  /** Reference to state machine manager. */
  EDStateManager manager;
  
  // BUSES ///////////////////////////////////////////////////////////////////
  
  /** Internal event dispatcher. */
  private EDBus internalBus = null;
  
  /**
   * Internal event output bus.  We pass all stuff to be outputted through
   * HERE first to support ED plugins that take ED results synchronously.
   */
  private EDBus outputBus = null;
  
  /** Public (KX) siena to communicate with the outside world */
  private Siena publicSiena = null;
  
  /**
   * An object that instantiates an EventDistiller. In this case
   * we communicate with it derectly, by sending it notifications,
   * otherwise we use Siena.
   */
  private Notifiable owner = null;
  
  /**
   * The timestamp of the last event processed internally.
   * We use this for time-keeping in the event-driven version,
   * (when eventDriven is set to true) This value is the least recent
   * event we can possibliy receive - assuming we receve events sequentially.
   */
  private long lastEventTime = 0;
  
  /**
   * The model for keeping time. This can be time-driven (current time is
   * computed by consulting system time, and adjusting it using a skew factor
   * computed on the basis on the actual time when events are receive, compared
   * to their timestamp), or event-driven (current time is taken to be equal to
   * the timestamp of the last event that has been processed).
   */
  private boolean eventDriven = false;
  
  /** The skew factor used to keep time. */
  private long timeSkew = 0;
  
  /** The number of events processed so far. */
  private long processedEvents = 0;
  
  /** State machine specification file */
  private String stateSpecFile = null;
  
  /**
   * Output file name. ED writes the current rulebase to this file, on shutdown.
   */
  private File outputFile;
  
  /** Whether the ED is to be shut down by the owning application. */
  boolean inShutdown = false;
  
  /** Whether the ED has been shut down. */
  private boolean hasShutdown = false;
  
  /** ACME support? */
  private String acmeBus = null;
  
  /** ACME gauge manager */
  private EDGaugeMgr acmeGM = null;
  
  /** Main. */
  public static void main(String args[]) {
    String of = null, sf = null, sh = null, sp = null;
    boolean e = false, gui = false;
    boolean debugging = false;
    String debugFile = null;                     // Debug properties file
    String acmeBus = null;                       // ACME Siena bus
    
    for(int i=0; i < args.length; i++) {
      if(args[i].equals("-s"))                   // Siena host
        sh = args[++i];
      else if(args[i].equals("-p"))              // Siena port
        sp = args[++i];
      else if(args[i].equals("-f"))              // State description file
        sf = args[++i];
      else if (args[i].equals("-o"))             // State description output
        of = args[++i];
      else if (args[i].equals("-d"))             // Verbose console debugging
        debugging = true;
      else if (args[i].equals("-df"))            // Debugging specified by file
        debugFile = args[++i];
      else if (args[i].equals("-event"))         // Event-driven processing
        e = true;
      else if (args[i].equals("-gui"))           // GUI feedback
        gui = true;
      else if (args[i].equals("-acme"))          // ACME integration
        acmeBus = args[++i];
      else
        usage();
    }
    
    EventDistiller ed = new EventDistiller(sh, sp, sf, e, of, debugging,
    debugFile, acmeBus);
  }
  
  /** Prints usage. */
  public static void usage() {
    System.err.println("\nUsage: java EventDistiller <-s sienaHost> " +
    "[-f ruleSpecFile]\n\t[-d|-df debugScript] [-o outputFileName] " +
    "[-event] [-acme sienaHost] [-?]");
    System.err.println("\t-s:\tSpecify Siena host");
    System.err.println("\t-p:\tSpecify Siena packet receiver port");
    System.err.println("\t-d:\tEnable basic debugging");
    System.err.println("\t-df:\tEnable complex debugging - must specify \n" +
    "\t\tlog4j-compliant properties file");
    System.err.println("\t-f:\tRule specification file, see docs");
    System.err.println("\t-o:\tSpecify output rules file to be written on " +
    "shutdown");
    System.err.println("\t-event:\tSpecify reordering timeout mechanism, see " +
    "docs");
    System.err.println("\t-acme:\tReport values on ACME gauge bus (requires " +
    "ACME gauge infrastructure)");
    System.err.println("\t-?:\tSee this description");
    System.exit(-1);
  }
  
  /**
   * Simple CTOR for embedded operation.  Assumes an EventDistiller with no
   * rules.  To add rules, use the dynamic rulebase notification updates.
   *
   * @param owner An owner to receive ED's publications, e.g., when a
   * rule matches
   */
  public EventDistiller(Notifiable owner) {
    this(owner, null, false, null, false, null);
  }
  
  /**
   * Constructs a new EventDistiller with an owner.
   * If you use this constructor, pass events to ED directly through
   * the notify method using EDInputNotification.
   *
   * @param owner the object to which we return notifications
   * @param spec the name of the file containing the specification;
   *             could be null
   * @param eventDriven the criterion for keeping time
   * @param debugging Whether we want debugging-detail information
   * @param debugFile Alternative to default debugging: see log4j properties
   * format to enable this
   */
  public EventDistiller(Notifiable owner, String spec, boolean eventDriven,
  String outputFile, boolean debugging, String debugFile) {
    this.owner = owner;
    this.stateSpecFile = spec;
    this.eventDriven = eventDriven;
    
    initDebug(debugging, debugFile);
    
    init();                            // finish preparing before returning
    new Thread(this).start();          // new thread context to run in
  }
  
  /**
   * Standard constructor, called by main.
   * Receives notificaions through Siena.
   *
   * @param sienaHost the siena host through which we receive events
   * @param sienaPort The port for the siena packet receiver, can be null
   * @param specFile name of rule spedification file, can be null
   * @param outputFile what the outputFile for the rulebase is
   * @param debugging Whether we want debugging-detail information
   * @param debugFile Alternative to default debugging: see log4j properties
   * format to enable this
   * @param acmeBus ACME bus (optional)
   */
  public EventDistiller(String sienaHost, String sienaPort, String specFile,
  boolean eventDriven, String outputFile, boolean debugging, String debugFile,
  String acmeBus) {
    this.stateSpecFile = specFile;
    this.eventDriven = eventDriven;
    this.acmeBus = acmeBus;
    
    initDebug(debugging, debugFile);
    
    // Subscribe to the "master" Siena
    publicSiena = new HierarchicalDispatcher();
    
    if(sienaHost == null && sienaPort == null) {
      debug.warn("Siena host and receive port not specified, operating in standalone mode");
    }
    
    // Set receiver and master properties
    try {
      if(sienaPort != null) {
        ((HierarchicalDispatcher)publicSiena).setReceiver(
        SienaUtils.newTCPPacketReceiver(Integer.parseInt(sienaPort)));
      }
      if(sienaHost != null)
        ((HierarchicalDispatcher)publicSiena).setMaster(sienaHost);
    } catch(Exception e) {
      // Failed, print error and quit
      debug.fatal("Can't set Siena server parameters", e);
      System.exit(-1);                           // XXX - should NOT do this?
    }
    
    // Subscribe to event distiller input
    Filter generalFilter = new Filter();
    generalFilter.addConstraint(EDConst.INPUT_ATTR,EDConst.INPUT_VAL);
    try {
      publicSiena.subscribe(generalFilter, this);
    } catch(SienaException e) {
      debug.warn("Warning: failed in incoming event subscription; " +
      "events may not be received", e);
    }
    
    if(outputFile != null) {
      setOutputFile(new File(outputFile));
    }
    
    init();
    run(); /* Don't need to create new thread */
  }
  
  /** Initialize debugging */
  private static void initDebug(boolean debug, String debugFile) {
    // Set up logging
    if(debug == true && debugFile == null) {
      debugEnabled = true;
      BasicConfigurator.configure();             // Basic (all) debugging)
    } else if(debugFile != null) { // Do it whether or not debugging is true
      debugEnabled = true;
      PropertyConfigurator.configure(debugFile); // Log4j format file
    } else {                                     // No debugging at all
      debugEnabled = false;
      BasicConfigurator.configure();
      Logger.getRootLogger().setLevel(Level.INFO);
    }
  }
  
  /** Initializes ED, creating internal bus and manager. */
  private void init() {
    /* Add a shutdown hook */
    Runtime.getRuntime().addShutdownHook(new Thread() {
      public void run() {
        debug.info("Shutting down ED...");
        if (!inShutdown) shutdown();
      }
    });
    
    // Set the current execution context, so if the callback is called
    // it can wake up a sleeping distiller
    //edContext = Thread.currentThread();
    
    // Create internal dispatcher
    internalBus = new EDBus();
    
    // Create output bus
    outputBus = new EDBus();
    
    // Add one subscriber for ALL events to outputBus - this proxies the
    // results to the outside world.
    outputBus.subscribe(new Filter(),
    new EDNotifiable() {
      public boolean notify(Notification n) {
        try {
          // if we have an owner, send him the notification
          if (owner != null) owner.notify(n);
          // else send it to the public siena
          else publicSiena.publish(n);
          debug.debug("Notification " + n + " sent externally!");
          return false; // Allow it to go through
        } catch(Exception e) {
          debug.error("Could not publish to outside world", e);
          return false; // Still allow it to go through
        }
      }
    },
    new Comparable() {
      public int compareTo(Object o) {
        return 0; // Don't care about order
      }
    });
    
    // Initialize state machine manager.
    manager = new EDStateManager(this);
    
    // Initialize gauge manager for ACME, if necessary
    if(acmeBus != null) {
      debug.debug("Starting gauge manger");
      this.acmeBus = acmeBus;
      // Create the ACME gauge manager
      acmeGM = new EDGaugeMgr(acmeBus, debugEnabled, outputBus);
    }
  }
  
  /** Start execution of the new EventDistiller. */
  public void run() {
    // Run process injector.  NOTE: Due to parallelism of Siena, ordering
    // is not guaranteed.  Below is a rather hacky way of ensuring a
    // very high probability of order in the internal Siena.
    debug.info("Running.");
    while (!inShutdown) { // run...
      //      errorManager.print("+", EDErrorManager.DISPATCHER);
      
      // every 1/2 sec or so...
      try { Thread.sleep(EDConst.EVENT_PROCESSING); }
      catch(InterruptedException ie) { ; }
      
      // process one event
      synchronized(eventProcessQueue) {
        if(eventProcessQueue.size() != 0) {
          Notification n = (Notification)eventProcessQueue.remove(0);
          debug.debug("Publishing " + n + " internally");
          internalBus.publish(n);
          
          // advance event counter
          processedEvents++;
          
          // update time
          long l = n.getAttribute(EDConst.TIME_ATT_NAME).longValue();
          if (eventDriven) lastEventTime = l;
          else {
            // for now, assume the skew of the last event is consistent
            timeSkew = System.currentTimeMillis() - l;
            /* // how much does this differ from the previous skew?
               double skew = (double)(timeSkew - (System.currentTimeMillis() - l));
               double weight = 1 / processedEvents;
               // weighted average
               timeSkew += (long)(skew * weight); */
          }
        }
      }
    }
    debug.info("In shutdown, no longer processing events");
  }
  
  /**
   * Flush all started machines, so that any failure notifications are sent.
   * This implies that all events that are currently subscribed will fail.
   * This is called by shutdown in the event-driven model (when this shuts down,
   * we assume there are no more possible); or it may be called by an event
   * (see KXNotification.EDFailAll).
   */
  void failAll() {
    // remember values, so we can resume
    boolean previousEventDriven = eventDriven;
    long previousLastEventTime = lastEventTime;
    
    // fail all states currently subscribed
    lastEventTime = Long.MAX_VALUE;
    manager.reap();
    
    // revert to previous values
    lastEventTime = previousLastEventTime;
    eventDriven = previousEventDriven;
  }
  
  /** Shuts down the ED. */
  public void shutdown() {
    inShutdown = true;
    debug.info("Shutting down");
    
    // Shut down the ACME stuff, if it's around
    if(acmeGM != null) acmeGM.shutdown();
    
    // Shut down the dispatchers
    if (owner == null) try {
      ((HierarchicalDispatcher)publicSiena).shutdown();
    } catch(Exception e) {
      debug.warn("Couldn't process Siena shutdown request", e);
    }
    
    // Shut down EDBuses
    internalBus.shutdown();
    outputBus.shutdown();
    
    // fail all states, if in the event-driven mode
    if (eventDriven) failAll();
    
    // write out the current rulebase to a file, if specified
    if (outputFile != null) manager.write(outputFile);
  }
  
  // notifications methods
  
  /**
   * Siena Callback.  We receive two kinds of callbacks: data from the
   * event packager, and interpreted results from the metaparser.
   */
  public void notify(Notification n) {
    debug.debug("Received notification " + n);
    
    // Publish internally, we no longer care about discriminating, let
    // Siena do all the work.  The dispatcher will then process the event
    queue(n);
  }
  
  /**
   * Auxiliary method: places an event on the processing queue,
   * checking that it has a timestamp.
   * @param n the event to queue
   */
  private void queue(Notification n) {
    // Make sure a timestamp has been set.
    if (n.getAttribute(EDConst.TIME_ATT_NAME) == null) {
      n.putAttribute(EDConst.TIME_ATT_NAME, System.currentTimeMillis());
    }
    
    // Add the event onto the queue
    synchronized(eventProcessQueue) {
      eventProcessQueue.addElement(n);
    }
  }
  
  /** Unused Siena construct. */
  public void notify(Notification[] s) { ; }
  
  /**
   * Propagates a given notification to the world - or
   * internally, if it is an internal notification.
   * @param n the notofication to propagate,
   *          must be in already wrapped format
   */
  void sendPublic(Notification n) {
    debug.debug("Sending out " + n);
    
    // If this is an internal notification, we just send it through to
    // ourselves.
    if (n.getAttribute("internal") != null &&
    n.getAttribute("internal").booleanValue()) {
      internalBus.publish(KXNotification.EDInternalNotification(n, getTime()));
    } else { // the notification goes outside
      outputBus.publish(n);
    }
  }
  
  // standard methods
  
  /**
   * Set the output file of the Event Distiller's rulebase.
   * Useful if you're running an embedded EventDistiller and want it to save
   * the changes you feed it.
   *
   * @param outputFile the file to write the rulebase on shutdown
   */
  public void setOutputFile(File outputFile) { this.outputFile = outputFile; }
  
  /**
   * Get the "current system" time.  This isn't as simple as it seems, since
   * if we are event-driven we consider our time to be the time of the last-
   * received event.
   *
   * For the other case, we return current time minus the timeSkew.  The skew
   * "delays" failures, etc.
   *
   * @return Time in standard UNIX time format
   */
  /*static*/ long getTime() {
    if (eventDriven) return lastEventTime;
    return System.currentTimeMillis() - timeSkew;
  }
  
  /**
   * Return the internal EDBus.  Needed by various ED subcomponents to publish
   * & subscribe on.  XXX - should remove this and pass everything around
   * via references.
   *
   * @return our EDBus instance.
   */
  EDBus getBus() { return this.internalBus; }
  
  /** @return the specification file */
  String getSpecFile() { return this.stateSpecFile; }
  
  /** @return whether the ED is in shutdown */
  boolean isInShutdown() { return this.inShutdown; }
}
