package psl.xues;

import psl.kx.*;
import java.io.*;
import java.util.*;
import siena.*;

/**
 * The Event Distiller.
 *
 * Copyright (c) 2000-2001: The Trustees of Columbia University and the
 * City of New York.  All Rights Reserved.
 *
 * TODO: Fix timestamp warning if the timestamp attribute is bad
 *
 * @author Janak J Parekh, parts by Enrico Buonnano
 * @version $Revision$
 */
public class EventDistiller implements Runnable, Notifiable {
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
  
  /** Debug flag. */
  static boolean DEBUG = false;
  
  /** State machine specification file */
  private String stateSpecFile = null;
  
  /** Output file name. ED writes the current rulebase to this file, on shutdown. */
  private File outputFile;
  
  /** Whether the ED is to be shut down by the owning application. */
  boolean inShutdown = false;
  
  /** Whether the ED has been shut down. */
  private boolean hasShutdown = false;
  
  /** Whether the output goes to a graphical window. */
  private boolean outputToGUI = false;
  
  /** The error manager where we send debug statements. */
  EDErrorManager errorManager;
  
  /** Main. */
  public static void main(String args[]) {
    String of = null, sf = null, sh = null;
    boolean e = true, d = false, gui = false;
    
    if(args.length > 0) { // Siena host specified?
      for(int i=0; i < args.length; i++) {
        if(args[i].equals("-s"))
          sh = args[++i];
        else if(args[i].equals("-f"))
          sf = args[++i];
        else if(args[i].equals("-d"))
          d = true;
        else if (args[i].equals("-o"))
          of = args[++i];
        else if (args[i].equals("-event"))
          e = true;
        else if (args[i].equals("-gui"))
          gui = true;
        else
          usage();
      }
    }
    // At least, a Siena host must be specified
    if (sh == null) {
      System.err.println("Error: Must specify a Siena host for standalone " +
      "operation");
      usage();
    }
    else {
      EventDistiller ed = new EventDistiller(sh, sf, e, d, gui);
      ed.setOutputFile(new File(of));
    }
  }
  
  /** Prints usage. */
  public static void usage() {
    System.err.println("usage: java EventDistiller [-f ruleSpecFile] "+
    "[-s sienaHost] [-d] [-o outputFileName] [-event] [-?]");
    System.err.println("\t-d implies debugging mode");
    System.exit(-1);
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
   * @param debug whether debug statements should be printed
   * @param outputToGUI whether error statements appear in a graphic component
   */
  public EventDistiller(Notifiable owner, String spec, boolean eventDriven,
  boolean debug, boolean outputToGUI) {
    this.owner = owner;
    this.stateSpecFile = spec;
    this.eventDriven = eventDriven;
    this.outputToGUI = outputToGUI;
    DEBUG = debug;
    
    init();                            // finish preparing before returning
    new Thread(this).start();          // new thread context to run in
  }
  
  /** Constructs a new ED with an owner -- Use notifications to add rules. */
  public EventDistiller(Notifiable owner) {
    this(owner, null, false, false, false);
  }
  
  /**
   * Standard constructor, called by main.
   * Receives notificaions through Siena.
   * @param sienaHost the siena host through which we receive events
   * @param specFile name of rule spedification file, could be null
   * @param eventDriven the criterion for keeping time
   * @param debug whether debug statements should be printed
   * @param outputToGUI whether error statements appear in a graphic component
   */
  public EventDistiller(String sienaHost, String specFile, boolean eventDriven,
  boolean debug, boolean outputToGUI) {
    this.stateSpecFile = specFile;
    this.eventDriven = eventDriven;
    this.outputToGUI = outputToGUI;
    DEBUG = debug;
    
    // Subscribe to the "master" Siena
    publicSiena = new HierarchicalDispatcher();
    try {
      ((HierarchicalDispatcher)publicSiena).
      setReceiver(new TCPPacketReceiver(61978));
      ((HierarchicalDispatcher)publicSiena).setMaster(sienaHost);
    } catch(Exception e) { e.printStackTrace(); }
    
    // Subscribe to packager input, metaparser results, and general input
    Filter packagerFilter = new Filter();
    packagerFilter.addConstraint("Type","PackagedEvent");
    Filter metaparserFilter = new Filter();
    metaparserFilter.addConstraint("Type","ParsedEvent");
    Filter generalFilter = new Filter();
    generalFilter.addConstraint("Type","EDInput");
    try {
      publicSiena.subscribe(packagerFilter, this);
      publicSiena.subscribe(metaparserFilter, this);
      publicSiena.subscribe(generalFilter, this);
    } catch(SienaException e) { e.printStackTrace(); }
    
    init();
    run(); /* Don't need to create new thread */
  }
  
  /** Initializes ED, creating internal bus and manager. */
  private void init() {
    // Initialize error manager
    if (outputToGUI) errorManager = new EDErrorGUI(DEBUG);
    else errorManager = new EDErrorConsole(DEBUG);
    
        /* Add a shutdown hook */
    Runtime.getRuntime().addShutdownHook(new Thread() {
      public void run() {
        if (!inShutdown) shutdown();
      }
    });
    
    // Set the current execution context, so if the callback is called
    // it can wake up a sleeping distiller
    //edContext = Thread.currentThread();
    
    // Create internal dispatcher
    bus = new EDBus();
    bus.setErrorManager(errorManager);
        /*privateSiena = new HierarchicalDispatcher();
        boolean done = false;
        int port = 61979;
        while (!done) try {
            done = true;
            if (DEBUG) System.out.println("creating internal siena on port: " + port);
            ((HierarchicalDispatcher)privateSiena).
                setReceiver(new TCPPacketReceiver(port));
        } catch (Exception e) {
            port++;
            done = false;
            }*/
    
    // Initialize state machine manager.
    manager = new EDStateManager(this);
  }
  
  /** Start execution of the new EventDistiller. */
  public void run() {
    // Run process injector.  NOTE: Due to parallelism of Siena, ordering
    // is not guaranteed.  Below is a rather hacky way of ensuring a
    // very high probability of order in the internal Siena.
    while (!inShutdown) { // run...
      errorManager.print("+", EDErrorManager.DISPATCHER);
      
      // every 1/2 sec or so...
      try { Thread.sleep(EDConst.EVENT_PROCESSING); }
      catch(InterruptedException ie) { ; }
      
      // process one event
      synchronized(eventProcessQueue) {
        if(eventProcessQueue.size() != 0) {
          Notification n = (Notification)eventProcessQueue.remove(0);
          errorManager.println("EventDistiller: Publishing internally " + n,
          EDErrorManager.DISPATCHER);
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
    errorManager.println("EventDistiller: ceased processing events due to shutdown",
    EDErrorManager.DISPATCHER);
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
    errorManager.println("EventDistiller: shutting down", EDErrorManager.ERROR);
    
    /* Shut down the dispatchers */
    if (owner == null) try {
      ((HierarchicalDispatcher)publicSiena).shutdown();
    } catch(Exception e) { e.printStackTrace(); }
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
    errorManager.println("EventDistiller: Received notification ", EDErrorManager.DISPATCHER);
    
    // if this is called directly, just take it
    if (owner != null) queue(n);
    
    // the notif comes from siena
    else { // What kind of notification is it?
      
      if(n.getAttribute("Type").stringValue().equals("PackagedEvent")) {
        // Just wrap it up and send it out to the metaparser, for now.
        try {
          Notification mpN = KXNotification.MetaparserInput
          ("EventDistiller", 12345, (int)n.getAttribute("DataSourceID"). longValue(),
          (String)null, n.getAttribute("SmartEvent").stringValue());
          publicSiena.publish(mpN);
        } catch(SienaException e) { e.printStackTrace(); }
      }
      else // process this event
        if(n.getAttribute("Type").stringValue().equals("ParsedEvent") ||
        n.getAttribute("Type").stringValue().equals("EDInput")) {
          queue(n);
        }
    }
  }
  
  /**
   * Auxiliary method: places an event on the processing queue,
   * checking that it has a timestamp.
   * @param n the event to queue
   */
  private void queue(Notification n) {
    // make sure timestamp is available
    if (n.getAttribute("timestamp") == null) {
      /*if (n.getAttribute("time") != null) // hack for Phil
      //  n.putAttribute("timestamp", n.getAttribute("time"));
      else*/
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
    errorManager.println("SENDING " + n, EDErrorManager.DISPATCHER);
    
    try {
            /* if this is an internal notification
             * we just send it through to ourselves. */
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
  
  /** @param otputFile the file to write the rulebase on shutdown */
  public void setOutputFile(File outputFile) { this.outputFile = outputFile; }
  
  /** @return the time to use a s a refernece. */
  long getTime() {
    if (eventDriven) return lastEventTime;
    return System.currentTimeMillis() - timeSkew;
  }
  
  /** @return the internal event dispatcher */
  EDBus getBus() { return this.bus; }
  
  /** @return the specification file */
  String getSpecFile() { return this.stateSpecFile; }
  
  /** @return the error manager, call to print out */
  EDErrorManager getErrorManager() { return this.errorManager; }
  
  /** @return whether the ED is in shutdown */
  boolean isInShutdown() { return this.inShutdown; }
}
