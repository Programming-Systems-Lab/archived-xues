package psl.xues;

import psl.kx.*;
import java.io.*;
import java.util.*;
import siena.*;

import org.apache.log4j.Category;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;

/**
 * The Event Distiller.
 *
 * Copyright (c) 2000-2001: The Trustees of Columbia University and the
 * City of New York.  All Rights Reserved.
 *
 * TODO: Fix timestamp warning if the timestamp attribute is bad
 * TODO: Support more than 1 sec reordering time
 *
 * @author Janak J Parekh, parts by Enrico Buonnano
 * @version $Revision$
 */
public class EventDistiller implements Runnable, Notifiable {
  /** log4j category class */
  static Category debug =
  Category.getInstance(EventDistiller.class.getName());
  
  /**
   * We maintain a stack of events to process - this way incoming
   * callbacks don't get tied up - they just push onto the stack.
   */
  private Vector eventProcessQueue = new Vector();
  
  /** My main execution context */
  //private Thread edContext = null;
  
  /** Reference to state machine manager. */
  EDStateManager manager;
  
  /** Internal event dispatcher. */
  private Siena privateSiena = null;
  private EDBus bus;
  
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
  
  /** Main. */
  public static void main(String args[]) {
    String of = null, sf = null, sh = null;
    boolean e = true, gui = false;
    boolean debugging = false;
    String debugFile = null;                     // Debug properties file
    
    if(args.length > 0) { // Siena host specified?
      for(int i=0; i < args.length; i++) {
        if(args[i].equals("-s"))
          sh = args[++i];
        else if(args[i].equals("-f"))
          sf = args[++i];
        else if (args[i].equals("-o"))
          of = args[++i];
        else if (args[i].equals("-d"))
          debugging = true;
        else if (args[i].equals("-df"))
          debugFile = args[++i];
        else if (args[i].equals("-event"))
          e = true;
        else if (args[i].equals("-gui"))
          gui = true;
        else
          usage();
      }
    }
    
    // Make sure a Siena host has been specified
    if (sh == null) {
      System.err.println("FATAL: " + 
      "Must specify a Siena host for standalone operation");
      usage();
    }

    EventDistiller ed = new EventDistiller(sh, sf, e, of, debugging, 
    debugFile);
  }
  
  /** Prints usage. */
  public static void usage() {
    System.err.println("\nUsage: java EventDistiller <-s sienaHost> " + 
    "[-f ruleSpecFile]\n\t[-d|-df debugScript] [-o outputFileName] " + 
    "[-event] [-?]");
    System.err.println("\t-s:\tSpecify Siena host (required)");
    System.err.println("\t-d:\tEnable basic debugging");
    System.err.println("\t-df:\tEnable complex debugging - must specify \n" + 
    "\t\tlog4j-compliant properties file");
    System.err.println("\t-f:\tRule specification file, see docs");
    System.err.println("\t-o:\tSpecify output rules file to be written on " + 
    "shutdown");
    System.err.println("\t-event:\tSpecify reordering timeout mechanism, see " + 
    "docs");
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
   * @param specFile name of rule spedification file, could be null
   * @param outputFile what the outputFile for the rulebase is
   * @param debugging Whether we want debugging-detail information
   * @param debugFile Alternative to default debugging: see log4j properties
   * format to enable this
   */
  public EventDistiller(String sienaHost, String specFile, boolean eventDriven,
  String outputFile, boolean debugging, String debugFile) {
    this.stateSpecFile = specFile;
    this.eventDriven = eventDriven;

    initDebug(debugging, debugFile);
    
    // Subscribe to the "master" Siena
    publicSiena = new HierarchicalDispatcher();
    try {
      ((HierarchicalDispatcher)publicSiena).
      setReceiver(new TCPPacketReceiver(0));
      ((HierarchicalDispatcher)publicSiena).setMaster(sienaHost);
    } catch(Exception e) { 
      // Failed, print error and quit
      debug.fatal("Can't subscribe to Siena server", e);
      System.exit(-1);                           // XXX - should NOT do this
    }
    
    // Subscribe to event distiller input
    Filter generalFilter = new Filter();
    generalFilter.addConstraint("Type","EDInput");
    try {
      publicSiena.subscribe(generalFilter, this);
    } catch(SienaException e) { 
      debug.warn("Warning: failed in incoming event subscription; " +
      "events may not be received", e);
    }
          
    setOutputFile(new File(outputFile));
    init();
    run(); /* Don't need to create new thread */
  }

  /** Initialize debugging */
  private static void initDebug(boolean debug, String debugFile) {
    // Set up logging
    if(debug == true && debugFile == null) {
      BasicConfigurator.configure();             // Basic (all) debugging)
    } else if(debugFile != null) { // Do it whether or not debugging is true
      PropertyConfigurator.configure(debugFile); // Log4j format file
    } else {                                     // No debugging at all
      BasicConfigurator.configure();
      // Deprecated
      // BasicConfigurator.disableDebug();
      Category.getDefaultHierarchy().disableDebug();
    }
  }      
  
  /** Initializes ED, creating internal bus and manager. */
  private void init() {
    /* Add a shutdown hook */
    Runtime.getRuntime().addShutdownHook(new Thread() {
      public void run() {
        debug.info("Shutting down EP...");
        if (!inShutdown) shutdown();
      }
    });
    
    // Set the current execution context, so if the callback is called
    // it can wake up a sleeping distiller
    //edContext = Thread.currentThread();
    
    // Create internal dispatcher
    bus = new EDBus();
    
    // Initialize state machine manager.
    manager = new EDStateManager(this);
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
          bus.publish(n);
          
          // advance event counter
          processedEvents++;
          
          // update time
          long l = n.getAttribute("timestamp").longValue();
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
    
    /* Shut down the dispatchers */
    if (owner == null) try {
      ((HierarchicalDispatcher)publicSiena).shutdown();
    } catch(Exception e) { 
      debug.warn("Couldn't process Siena shutdown request", e);
    }

    // Shut down EDBus
    bus.shutdown();
    
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
    if (n.getAttribute("timestamp") == null) {
      n.putAttribute("timestamp", System.currentTimeMillis());
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
    
    try {
      // If this is an internal notification, we just send it through to 
      // ourselves. 
      if (n.getAttribute("internal") != null &&
      n.getAttribute("internal").booleanValue())
        bus.publish(KXNotification.EDInternalNotification(n, getTime()));
      else { // the notification goes outside
        // if we have an owner, send him the notification
        if (owner != null) owner.notify(n);
        // else send it to the public siena
        else publicSiena.publish(n);
      }
    }
    catch(SienaException e) { e.printStackTrace(); }
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
  
  /** @return the time to use a s a refernece. */
  long getTime() {
    if (eventDriven) return lastEventTime;
    return System.currentTimeMillis() - timeSkew;
  }
  
  /** 
   * Return the internal EDBus.
   *
   * XXX - why is this needed?
   *
   * @return our EDBus instance.
   */
  EDBus getBus() { return this.bus; }
  
  /** @return the specification file */
  String getSpecFile() { return this.stateSpecFile; }
  
  /** @return whether the ED is in shutdown */
  boolean isInShutdown() { return this.inShutdown; }
}
