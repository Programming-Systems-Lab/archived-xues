package psl.xues;

import java.util.*;
import siena.*;


/**
 *
 * The Event Distiller Bus that's a simplified event bus with single threaded
 * dispatcher.
 *
 * <code>EDBus</code> was written to replace <code>Siena</code> for use as
 * the internal bus of <code>EventDistiller</code>.  The issues
 * <code>EDBus</code> addresses are the following:
 *
 * <ul>
 * <li><B>Spawning delays</B>: Since dispatching is done with a single thread,
 *                             it will wait for individual state to spawn
 *                             before sending the next event down the tube,
 *                             thus eliminating the need to introduce timing
 *                             delays to ensure each event is received by
 *                             each state.
 *
 * <li><B>Reordering</B>: Sometimes events do not arrive
 *                        in strict chronological order, which is essential
 *                        for matching rules correctly. <code>EDBus</code> can
 *                        be instantiated to handle out-of-order events, and
 *                        dispatch them to subscribers in the correct order,
 *                        provided that the events are not delayed more than
 *                        an user-specified timebound(in the realtime mode),
 *                        or that the they do not come after an later event
 *                        with a timestamp offset exceeding the timebound.
 *
 * <li><B>Dispatch Ordering</B>: Dispatch messages in a predetermined order,
 *                               mainly to support <I>Rule Absorption</I>.
 *
 * <li><B>Rule Absorption</B>: Enables Event Distiller to suppress further
 *                             notification to other subscribers once a rule
 *                             is deemed to be <I>absorbed</I>
 * </ul>
 * <BR>
 * Note: Timestamps are consider essential for all notifications coming into
 *       the bus, since we wish to reorder them if they come in out-of-order.
 *
 * @author James Wu <jw402@columbia.edu>
 */
public class EDBus {


    // Constants

    /**
     * The key for timestamp in a <code>Notification</code>
     */
    private static final String TS_STR = "timestamp";

    /**
     * Disabling autoflush.
     */
    public static final int AUTOFLUSH_DISABLED = 0;


    /**
     * Enabling autoflush(DEFAULT).
     */
    public static final int AUTOFLUSH_ENABLED = 1;




    /**
     * All the subscribers.
     */
    private Vector subscribers;

    /**
     * The hashtable linking a notifiable to the Subscriber wrapper class.
     * It's used to speed up unsubscribe
     */
    private Hashtable subsHash;

    /**
     * The <code>EDQueue</code> responsible for storing Notifications.
     */
    private EDQueue eventQueue;

    /**
     * When this value is set to false the dispatching thread will stop
     */
    private boolean dispatching = true;

    /**
     * Whether or not we use autoflush.
     */
    private int autoflushMode;

    /**
     * How long should the pipeline be, i.e. how long do we wait to reorder.
     */
    private long waitTime;


    private boolean VERBOSE = false;

    private EDErrorManager em;


    /**
     *
     * Constructor.
     */
    public EDBus(){
	this(AUTOFLUSH_ENABLED, 1000);
    }

    /**
     * Constructor with more options.
     *
     * @param autoflushMode whether or not to use autoflush
     * @param waitTime  how long to wait for reorder(default 1000ms)
     */
    public EDBus (int autoflushMode, long waitTime){
	this.autoflushMode = autoflushMode;
	this.waitTime = waitTime;
	subscribers = new Vector();
	subsHash = new Hashtable();
	eventQueue = new EDQueue();
	em = null;

	new Thread(){
		public void run(){
		    dispatcher();
		}
	    }.start();
    }

    /** For debugging purposes */
    private void dumpSubscribers(){
	verbosePrintln("EDBus:******Start dumping subscribers*****");
	for(int i = 0;i<subscribers.size();i++){
	    EDNotifiable n = ((EDSubscriber)subscribers.elementAt(i)).n;
	    if(n instanceof EDStateManager){
		verbosePrintln("Manager");
	    }else if(n instanceof EDState){
		verbosePrintln("EDState: "+((EDState)n).myID);
	    }else{
		verbosePrintln(n);
	    }
	}
	verbosePrintln("***End dumping subscribers.\n");
    }

    private void verbosePrintln(Object s){
	if(VERBOSE){
	    if(em!=null){
		em.println("EDBus: "+s,EDErrorManager.DISPATCHER);
	    }else{
		System.out.println("EDBus: "+s);
	    }
	}
    }

    private void errorPrintln(Object s){
	if(em!=null){
	    em.println("EDBus: "+s,EDErrorManager.ERROR);
	}else{
	    System.out.println("EDBus: "+s);
	}
    }

    private void println(Object s){
	if(em!=null){
	    em.println("EDBus: "+s,EDErrorManager.DISPATCHER);
	}
    }



    /**
     * Set the error manager.
     */

    public void setErrorManager(EDErrorManager em){
	this.em = em;
    }


    /**
     * Subscribes to this bus.
     *
     * Specifies a <code>EDNotifiable</code> object for the event dispatch
     * callback with a filter.
     *
     * @param f <code>Filter</code> used to specify subscription
     * @param n <code>EDNotifiable</code> object containing callback function
     *          for dispatch
     * @param c <code>Comparable</code> for dispatch ordering
     */
    public void subscribe(Filter filter, EDNotifiable ednotifiable, Comparable comparable){
        subscribe(new EDSubscriber(filter, ednotifiable, comparable));
    }

    public void subscribe(EDSubscriber edsubscriber){
        println("new subscrption with " + edsubscriber.f);
        synchronized(subscribers){
            subsHash.put(edsubscriber.n, edsubscriber);
            subscribers.add(edsubscriber);
            Collections.sort(subscribers);
            dumpSubscribers();
        }
        println("finished subscription");
    }

    /**
     *  Unsubscribe from this bus.
     *
     *  Further <code>Notification</code>s won't be sent to that
     *  <code>EDNotifiable</code> object afterwards.
     *
     *  <BR><B>NOTE: Unsubscribe implicitly flushes the queue.</B>
     *
     *
     *  @param n  the <code>EDNotifiable</code> to unsubscribe from the bus
     *
     *  @return  <code>true</code> if the <code>EDNotifiable</code> was present
     *           and is successfully removed
     */
    public boolean unsubscribe(EDNotifiable n){
	synchronized(subscribers){
	    EDSubscriber s = (EDSubscriber)subsHash.get(n);

	    subsHash.remove(n);
	    boolean returnVal = subscribers.remove(s);
	    dumpSubscribers();
	    return returnVal;
	}
    }


    /**
     *  Puts a <code>Notification</code> on the queue for dispatch.
     *
     *  @param e  the event to send out
     */
    public void publish(Notification e){
	if(dispatching){
	    eventQueue.enqueue(e);
	}else{
	    errorPrintln("Cannot publish when EDBus has been shutdown.");
	}
    }


    /**
     *  Shuts down the bus.
     *
     *  Before shutting down the bus, all events in the queue are flushed.
     *  <BR>NOTE: This is more than what Siena does with the method with the
     *  same name.
     */
    public synchronized void shutdown(){
	flush();
	dispatching = false;
    }

    /**
     *
     * Flush the queue.
     *
     * Dispatches all the unsent <code>Notification</code> in the queue.
     */
    public synchronized void flush(){
	while(eventQueue.size()!=0){
	    dispatch(eventQueue.dequeue());
	}
    }



    /**
     *  A loop that dispatches events on the queue.
     *
     *  Should be run from a <code>Thread</code> after
     *  <code>EDBus</code> is started.  It monitors the queue, and
     *  <code>dispatch</code>es <code>Notification</code>s when
     *  necessary
     */
    private void dispatcher(){
	while(dispatching || (eventQueue.size()!=0)){
	    try{
		if(((autoflushMode==AUTOFLUSH_ENABLED)&&
		    eventQueue.canDequeue(System.currentTimeMillis()
					  -waitTime))
		   ||
		   ((autoflushMode==AUTOFLUSH_DISABLED)&&
		    eventQueue.canDequeueWithLength(waitTime))){

		    verbosePrintln("Dispatcher dequeuing");
		    dispatch(eventQueue.dequeue());
		}else{
		    // this is really stupid and wasteful.
		    // there should be one more case for an empty queue
		    // for which we should just wait until someone
		    // publishes.

		    // Will implement more efficient threading interaction
		    // when I feel like it.

		    Thread.currentThread().sleep(100);
		}
	    }catch(InterruptedException exception){
		//OK, I don't know why this happened. Report anyway
		verbosePrintln("Dispatcher interuppted");
	    }
	}
	// For debugging purposes
	//  	synchronized(this){
	//  	    this.notifyAll();
	//  	}
    }

    /**
     * Sends an event to all qualifying <code>EDSubscriber</code>s, unless
     * the message is absorbed halfway.
     *
     * Sends a <code>Notification</code> to any
     * <code>EDSubscriber</code> which specifies a <code>Filter</code>
     * that matches this event.
     *
     * @param e  The <code>Notification</code> to be sent
     */
    private void dispatch(Notification e){
	Vector copyOfSubscribers = new Vector(subscribers);
	Enumeration elements = copyOfSubscribers.elements();
	boolean absorbed = false;
	while(elements.hasMoreElements() && !absorbed){
	    EDSubscriber thisSubscriber =
		(EDSubscriber) elements.nextElement();
	    if(thisSubscriber.acceptsNotification(e)){
		absorbed = thisSubscriber.n.notify(e);
	    }
	}
    }

    public static void main(String[] args){
	EDBusTester.main(null);
    }
}// EDBus

/**
 *  Helper class to keep track of an individual subscriber of
 *  <code>EDBus</code>.
 */
class EDSubscriber implements Comparable{
    private boolean DEBUG = false;

    /**
     * The <code>Filter</code> specifying the subscription of this
     * <code>EDSubscriber</code>
     */
    Filter f;

    /**
     * The object containing the callback methods for dispatch; the object
     * that receives events through the subscription.
     */
    EDNotifiable n;

    /**
     * The object used to determine the order of comparisons.
     */
    Comparable c;


    /**
     * Constructor.
     *
     * @param filter     The <code>Filter</code> that will be used by this
     *                   particular <code>EDSubscriber</code>.
     *
     * @param notifiable The actual <code>EDNotifiable</code> object with the
     *                   callback function(s) during a message dispatch.
     *
     * @param comp       The <code>Comparable</code> used for deterministic
     *                   dispatch.
     */

    public EDSubscriber(Filter filter, EDNotifiable notifiable, Comparable comp){
	f = filter;
	n = notifiable;
	c = comp;
    }

    /**
     * Resets the timebound for this subscriber. Used by states that have counter or
     * loop features, where the timebound may be extended, as the subscriber is matched.
     * @param l the new timebound within which this subscriber may be matched
     */
    void resetTimebound(long l){
        synchronized(f){
            f.removeConstraints(EDConst.TIME_ATT_NAME);
            f.addConstraint(EDConst.TIME_ATT_NAME, new AttributeConstraint(Op.LT, new AttributeValue(l)));
        }
    }


    /**
     * Implements the <code>Comparable</code> interface.
     *
     * @param o the <code>Object</code> to be compared
     *
     * @return a negative integer, zero, or a positive integer as this
     *         object is less than, equal to, or greater than the specified
     *         object.
     */
    public int compareTo(Object o){
	return this.c.compareTo(((EDSubscriber)o).c);
    }

    /** Determines whether or not a <code>Notification</code>
     *  should be accepted by the <code>EDSubscriber</code>.
     *
     *  The following creates a wildcard:<BR><PRE>
     *	Filter f = new Filter();
     *	f.addConstraint(null,
     *			new AttributeConstraint(Op.ANY,(AttributeValue)null));
     *  </PRE>
     *
     *  @param  e The <code>Notification</code> in question
     *
     *  @return whether or not the <code>Notification</code> will be accepted
     */
    public boolean acceptsNotification(Notification notification){
        return Covering.apply(f, notification); // just use siena
    }
}// EDSubscriber






















