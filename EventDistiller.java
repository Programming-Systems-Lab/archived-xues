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
 * TODO:
 * - Something more complex than a Notification as Action
 * - Multiple actions
 *
 * @author Janak J Parekh
 * @version 0.9
 *
 * $Log$
 * Revision 1.28  2001-06-20 20:07:21  eb659
 * time-based and event-based timekeeping
 *
 * Revision 1.26  2001/06/18 20:58:36  eb659
 *
 * integrated version of ED. compiles, no testing done
 *
 * Revision 1.25  2001/06/18 17:44:51  jjp32
 *
 * Copied changes from xues-eb659 and xues-jw402 into main trunk.  Main
 * trunk is now development again, and the aforementioned branches are
 * hereby closed.
 *
 * Revision 1.16.4.16  2001/06/06 19:52:09  eb659
 * tested loop feature. Works, but identified other issues, to do with new
 * implementation of timestamp validation:
 * - keeping the time based on last received event has the inconvenience that we
 * do not send failure notifs if we do not get events (i.e. time stops moving)
 * - should there be different scenarios for real-time and non-realtime?
 *
 * Revision 1.16.4.15  2001/06/05 00:16:30  eb659
 *
 * wildHash handling corrected - (but slightly different than stable version)
 * timestamp handling corrected. Requests that all notifications be timestamped
 * reap based on last event processed internally
 * improved state reaper
 *
 * Revision 1.16.4.14  2001/06/01 22:31:19  eb659
 *
 * counter feature implemented and tested
 *
 * Revision 1.16.4.13  2001/05/31 22:36:39  eb659
 *
 * revision of timebound check: initial states can now have a specified bound.
 * allow values starting with '*'. Add an initial '*' as escape character...
 * preparing the ground for single-instance rules, and countable states
 *
 * Revision 1.16.4.12  2001/05/29 20:47:07  eb659
 *
 * various fixes. embedded constructor thoroughly tested,
 * in particular the following features:
 * - standard constructor
 * - specification-less constructor
 * - sending events
 * - recieving notifications
 * - shutdown features (when shutdown() is called and when it is not)
 *
 * ED looks for a free address to place private siena.
 *
 * Revision 1.16.4.11  2001/05/29 17:09:36  jjp32
 * shutdown() should be public
 *
 * Revision 1.16.4.10  2001/05/28 22:22:14  eb659
 *
 * added EDTestConstruct to test embedded constructor. Can construct ED using
 * a Notifiable owner, and optionally, a spec file and/or a debug flag.
 * There's a bug having to do with the wildcard binding, I'll look at that
 * in more detail tomorrow.
 *
 * Revision 1.16.4.9  2001/05/28 17:55:17  jjp32
 * Forgot to commit last change
 *
 * Revision 1.16.4.8  2001/05/27 23:55:32  jjp32
 * Fixed non-returning-embedded-constructor problem
 *
 * Revision 1.16.4.7  2001/05/24 21:01:12  eb659
 *
 * finished rule consistency check (all points psecified in the todo list)
 * current rulebase is written to a file, on shutdown
 * compiles, not tested
 *
 * Revision 1.16.4.6  2001/05/23 21:40:42  eb659
 *
 *
 * DONE Direct (non-Siena) interface: new constructor taking a notifialbe object
 * DONE 2) allow states to absorb events - but waiting for the bus and XML
 *
 * ADDED rule consistency check, and error handling:
 * DONE checkConsistency() method in edsms
 * DONE KXNotification for EDError
 * DONE 1) no duplicate rules - checks against duplicate names
 * DONE 2) What happens if state parameters are missing, e.g., fail_actions?
 * DONE 3) check that all children and actions specified in the states are specified in the rule
 * more checks to be done - wildcards, etc.
 *
 * Revision 1.16.4.5  2001/05/21 00:47:25  jjp32
 * Synched up changes with main Xues trunk
 *
 * Revision 1.16.4.4  2001/05/06 08:01:48  eb659
 *
 * FINAL VERSION FOR THIS RELEASE! Expected functionality fully implemented.
 * Use EDTestOR to test or representation. Call it with smthg like:
 * 'java psl.xues.EDTestOR -s senp://canal:8888 -e abc'
 * using the desired siena host, and the desired event sequence. The command
 * above will send the events a, b, c, in that order. Look at the definition
 * of the 'hexagon rule' in TestRules2.xml to see which sequences are significant
 * to test.
 * Any reasonably short  sequence that contains, as a subset,
 * either 'abcdgh' or 'abefgh' should succeed (that is, propagate a success
 * notification {hex="true"}. Any sequence that does not satisfy this, should
 * fail, and the instantiate machine should time out.
 *
 * Otherwise, use EDTest to see the normal functioning, and AddRuleTest
 * (removing comments as appropriate) to test different dynamic rulebase
 * functions.
 *
 * Revision 1.16.4.3  2001/05/06 05:31:07  eb659
 *
 * Thoroughly re-tested all dynamic rulebase and fixed so that it
 * works with the new arch.
 *
 * Revision 1.16.4.2  2001/05/06 03:54:27  eb659
 *
 * Added support for checking multiple parents, and independent wild hashtables
 * for different paths. ED now has full functionality, and resiliency.
 * Now doing some additional testing for event sequences that actually use
 * the OR representation, and re-testing the dynamic rulebase, to make sure
 * it still works after the changes made.
 *
 * Revision 1.16.4.1  2001/05/02 00:04:27  eb659
 *
 * Tested and fixed a couple of things.
 * New architecture works, and can be tested using EDTest.
 * Reaper thread needs revision...
 * Can we get rid of internal 'loopback' notifications?
 *
 * Revision 1.16  2001/01/30 07:33:10  jjp32
 *
 * I'm done for tonight
 *
 * Revision 1.15  2001/01/30 06:26:18  jjp32
 *
 * Lots and lots of updates.  EventDistiller is now of demo-quality.
 *
 * Revision 1.14  2001/01/30 02:39:36  jjp32
 *
 * Added loopback functionality so hopefully internal siena gets the msgs
 * back
 *
 * Revision 1.13  2001/01/29 05:22:53  jjp32
 *
 * Reaper written - but it's probably a problem
 *
 * Revision 1.12  2001/01/28 22:58:58  jjp32
 *
 * Wildcard support has been added
 *
 * Revision 1.11  2001/01/28 21:34:00  jjp32
 *
 * XML parsing complete; almost ready for demo
 *
 * Revision 1.10  2001/01/26 03:30:53  jjp32
 *
 * Now supports non-localhost siena servers
 *
 * Revision 1.9  2001/01/22 02:11:54  jjp32
 *
 * First full Siena-aware build of XUES!
 *
 * Revision 1.8  2001/01/18 01:41:35  jjp32
 *
 * Moved KXNotification to kx; other modifications for demo
 *
 * Revision 1.7  2000/12/26 22:25:13  jjp32
 *
 * Updating to latest preview versions
 *
 * Revision 1.6  2000/09/09 18:17:14  jjp32
 *
 * Lots of bugs and fixes for demo
 *
 * Revision 1.5  2000/09/09 15:13:49  jjp32
 *
 * Numerous updates, bugfixes for demo
 *
 * Revision 1.4  2000/09/08 19:08:27  jjp32
 *
 * Minor updates, added socket communications in TriKXEventNotifier
 *
 * Revision 1.3  2000/09/08 02:09:44  jjp32
 *
 * Some minor updates
 *
 * Revision 1.2  2000/09/07 23:15:25  jjp32
 *
 * Added EventNotifier code; updated previous event code
 *
 * Revision 1.1  2000/09/07 19:30:49  jjp32
 *
 * Updating
 *
 */
public class EventDistiller implements Runnable, Notifiable {
    /**
     * We maintain a stack of events to process - this way incoming
     * callbacks don't get tied up - they just push onto the stack.
     */
    private Vector eventProcessQueue = null;
    
    /** My main execution context */
    private Thread edContext = null;
    
    /** Reference to state machine manager. */
    EDStateManager edsm;
    
    /** Internal event dispatcher. */
    //private Siena privateSiena = null;
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
     * We use this for reaping, so this value is the least recent
     * event we can possibliy receive - assuming we receve events sequentially
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

    /** Main. */
    public static void main(String args[]) {
	String of = null, sf = null, sh = null;
	boolean e = true, d = false;

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
		else
		    usage();
	    }
	}   
	if (sh == null) { 
	    System.err.println("must specify siena host");
	    System.exit(0);
	}
	else {
	    EventDistiller ed = new EventDistiller(sh, sf, e, d);
	    ed.setOutputFile(new File(of));
	}
    }

  /** Prints usage. */
  public static void usage() {
    System.out.println("usage: java EventDistiller [-f ruleSpecFile] "+
		       "[-s sienaHost] [-d] [-o outputFileName] [-event]  [-?]");
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
     */
    public EventDistiller(Notifiable owner, String spec, boolean eventDriven, boolean debug) { 
	this.owner = owner;
	this.stateSpecFile = spec;
	this.eventDriven = eventDriven;
	DEBUG = debug;

	init(); // finish preparing before returning
	new Thread(this).start(); // new thread context to run in
    }

    /** Constructs a new ED with an owner -- Use notifications to add rules. */
    public EventDistiller(Notifiable owner) { this(owner, null, false, false); }

    /** Constructs a new ED with an owner and spec file. */
    public EventDistiller(Notifiable owner, String spec) { this(owner, spec, false, false); }

    /** Constructs a new ED with an owner and debug statements. */
    public EventDistiller(Notifiable owner, boolean debug) { this(owner, null, false, debug); }
  
    /**
     * Standard constructor, called by main.
     * Receives notificaions through Siena.
     * @param sienaHost the siena host through which we receive events
     * @param specFile name of rule spedification file, could be null
     * @param eventDriven the criterion for keeping time
     * @param debug whether debug statements should be printed
     */
    public EventDistiller(String sienaHost, String specFile, boolean eventDriven, boolean debug) { 
	this.stateSpecFile = specFile;
	this.eventDriven = eventDriven;
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
	eventProcessQueue = new Vector();

	/* Add a shutdown hook */
	Runtime.getRuntime().addShutdownHook(new Thread() {
		public void run() { 
		    if (!hasShutdown) finish(); } 
	    });
	
	// Set the current execution context, so if the callback is called
	// it can wake up a sleeping distiller
	edContext = Thread.currentThread();
	
	// Create internal dispatcher
	bus = new EDBus();
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
	edsm = new EDStateManager(this);
    }
	
    /** Start execution of the new EventDistiller. */
    public void run() {
	// Run process injector.  NOTE: Due to parallelism of Siena, ordering
	// is not guaranteed.  Below is a rather hacky way of ensuring a
	// very high probability of order in the internal Siena.
	while (true) {
	    if (inShutdown) { // shutting down -- clean up and terminate
		finish();
		break;
	    }
	    else { // run...
		if(EventDistiller.DEBUG)
		    System.err.print("+");
		
		// every 1/2 sec or so...
		try { Thread.sleep(EDConst.EVENT_PROCESSING); } 
		catch(InterruptedException ie) { ; }
		
		// process one event
		synchronized(eventProcessQueue) {
		    if(eventProcessQueue.size() != 0) {
			Notification n = (Notification)eventProcessQueue.remove(0);
			if(EventDistiller.DEBUG)
			    System.err.println("EventDistiller: Publishing internally " + n);
			bus.publish(n);
 
			// advance event counter
			processedEvents++;

			// update time 
			long l = n.getAttribute("timestamp").longValue();
			if (eventDriven) lastEventTime = l;
			else {
			    // how much does this differ from the previous skew?
			    double skew = (double)(timeSkew - (System.currentTimeMillis() - l));
			    double weight = 1 / processedEvents;
			    // weighted average
			    timeSkew += (long)(skew * weight);
			}
		    }
		}
	    }
	}
    }

    /** Shuts down the ED. */
    public void shutdown() {
	inShutdown = true; 
	// the loop will die automatically, after cleaning up
    }

    /** Shutdown hook. */
    private void finish() {
	hasShutdown = true;
	/* Shut down the hierarchical dispatchers */
	System.err.println("EventDistiller: shutting down");
	try {
	    if (owner == null) ((HierarchicalDispatcher)publicSiena).shutdown();
	    bus.shutdown();
	    //((HierarchicalDispatcher)loopbackSiena).shutdown();
	} catch(Exception e) { e.printStackTrace(); }
	
	// write out the current rulebase to a file, if spedified
	if (outputFile != null) try {
	    PrintWriter outputWriter = new PrintWriter(new BufferedWriter(new FileWriter(outputFile)));
	    // start
	    outputWriter.write("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
			       + "<rulebase xmlns=\"http://www.psl.cs.columbia.edu/2001/01/DistillerRule.xsd\">");
	    // the rules
	    for (int i = 0; i < edsm.stateMachineTemplates.size(); i++) 
		outputWriter.write(((EDStateMachineSpecification)edsm.stateMachineTemplates.get(i)).toXML());
	    // end
	    outputWriter.write("</rulebase>");
	    outputWriter.close();
	} 
	catch(Exception ex) { ex.printStackTrace(); }
    }

    // notifications methods

    /**
     * Siena Callback.  We receive two kinds of callbacks: data from the
     * event packager, and interpreted results from the metaparser.
     */
    public void notify(Notification n) {
	if(DEBUG) System.out.println("EventDistiller: Received notification "+n);

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
		    if(DEBUG) System.err.println("EventDistiller: Sending notification to Metaparser" + mpN);
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
	    if (n.getAttribute("time") != null) // hack for Phil
		n.putAttribute("timestamp", n.getAttribute("time"));
	    else n.putAttribute("timestamp", System.currentTimeMillis());
	}
	
	// Add the event onto the queue 
	if(DEBUG) System.err.println("EventDistiller: Putting event on queue");
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
	if(EventDistiller.DEBUG)
	    System.err.println("YES! EventDistiller: sending event " + n);
	
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
}
