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

    private boolean DEBUG = false;

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
	subsHash = new Hashtable();
	subscribers = new Vector();
	eventQueue = new EDQueue();

	new Thread(){
		public void run(){
		    dispatcher();
		}
	    }.start();
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
    public void subscribe(Filter f, EDNotifiable n, Comparable c){
	Subscriber s = new Subscriber(f,n,c);
	synchronized(subscribers){
	    subsHash.put(n,s);
	    subscribers.add(s);
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
	    System.err.println("Cannot publish when EDBus has been shutdown.");
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
	    Subscriber s = (Subscriber)subsHash.get(n);
	    subsHash.remove(n);
	    return subscribers.remove(s);
	}
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
		    
		    if(DEBUG)System.out.println("Dispatcher dequeuing");
		    dispatch(eventQueue.dequeue());
		}else{
		    // this is really stupid and wasteful.
		    // there should be one more case for an empty queue
		    // for which we should just wait until someone
		    // publishes.
		    
		    // Will implement more efficient threading interaction
		    // when I feel like it.

		    if(DEBUG)System.out.println("Dispatcher sleeping");
		    Thread.currentThread().sleep(100);
		}
	    }catch(InterruptedException exception){
		//OK, I don't know why this happened. Report anyway
		System.err.println("Dispatcher interuppted");
	    }
	}
	synchronized(this){
	    this.notifyAll();
	}
    }

    /**
     * Sends an event to all qualifying <code>Subscriber</code>s, unless
     * the message is absorbed halfway.
     *
     * Sends a <code>Notification</code> to any
     * <code>Subscriber</code> which specifies a <code>Filter</code>
     * that matches this event.
     *
     * @param e  The <code>Notification</code> to be sent
     */
    private void dispatch(Notification e){

	
	/*
	Iterator iterator = subscribers.iterator();
	*/
	Enumeration iterator = subscribers.elements();
	boolean absorbed = false;
	
	//while(iterator.hasNext() && !absorbed){
	while(iterator.hasMoreElements() && ! absorbed){
	    Subscriber thisSubscriber = 
		//(Subscriber) iterator.next();
		(Subscriber) iterator.nextElement();
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
class Subscriber implements Comparable{
    private boolean DEBUG = false;

    /**
     * The <code>Filter</code> specifying the subscription of this 
     * <code>Subscriber</code>
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
     *                   particular <code>Subscriber</code>.
     *
     * @param notifiable The actual <code>EDNotifiable</code> object with the
     *                   callback function(s) during a message dispatch.
     *
     * @param comp       The <code>Comparable</code> used for deterministic
     *                   dispatch.
     */

    public Subscriber(Filter filter, EDNotifiable notifiable, Comparable comp){
	f = filter;
	n = notifiable;
	c = comp;
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
	return this.c.compareTo(((Subscriber)o).c);
    }

    /** Determines whether or not a <code>Notification</code>
     *  should be accepted by the <code>Subscriber</code>.
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
    public boolean acceptsNotification(Notification e){
	if(f.isEmpty()){// empty filter doesn't match any notification, right?
	    if(DEBUG)System.out.println("Filter empty");
	    return false;
	}
	Iterator names = f.constraintNamesIterator();
	while(names.hasNext()){
	    String constraintName = (String)names.next();
	    Iterator constraints = f.constraintsIterator(constraintName);

	    if(constraintName == null){
		return true;
	    }

	    AttributeValue value = e.getAttribute(constraintName);

	    if(value==null){
		if(DEBUG){
		    System.out.println("Missing AttributeValue "+
				       "in Notification");
		}
		return false;
	    }

	    while(constraints.hasNext()){
		AttributeConstraint constraint =
		    (AttributeConstraint)constraints.next();
		if(!match(constraint,value)){
		    return false;
		}
	    }
	}
	return true;
    }

    
    /** A helper method for acceptsNotification.<BR><BR>
     *
     *  It compares the value in the <code>AttributeConstraint</code>
     *  with <code>AttributeValue</code> using the operator specified
     *  in the <code>AttributeConstraint</code>
     *  
     *  @return whether or not the value matches the constraint
     */ 
    private boolean match(AttributeConstraint ac, AttributeValue av){
	if(ac.op==Op.ANY){
	    // if you get this far, the attribute should at least exist
	    // thus, always accept in this case
	    if(DEBUG)System.out.println("Matching the *ANY* rule");
	    return true;
	}else if(ac.op==Op.EQ){
	    /*
	      // hmm, I don't know what you think, but this isEqualTo method
	      // seems fishy to me.  I'll write my own match function
	      System.out.println("Equality test of "+ac.value+
	      " and "+av);
	      System.out.println("Types are "+ac.value.getType()+
	      " and "+av.getType());
	      return ac.value.isEqualTo(av);
	    */
	    return ac.value.toString().equals(av.toString());
	}else{
	    return true;// for fancy operators I don't understand, I let it slide
	}
    }
}// Subscriber
