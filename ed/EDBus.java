package psl.xues.ed;

import java.util.*;
import siena.*;

import org.apache.log4j.Category;

/**
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
 * @version $Revision$
 */
public class EDBus {
  /** log4j category class */
  static Category debug =
  Category.getInstance(EDBus.class.getName());
  
  // Constants
  
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
  public EDBus(int autoflushMode, long waitTime){
    this.autoflushMode = autoflushMode;
    this.waitTime = waitTime;
    subscribers = new Vector();
    subsHash = new Hashtable();
    eventQueue = new EDQueue();
    
    new Thread(){
      public void run(){
        dispatcher();
      }
    }.start();
  }
  
  /** For debugging purposes */
  private void dumpSubscribers(){
    debug.debug("Start dumping subscribers:");
    for(int i = 0;i<subscribers.size();i++){
      EDNotifiable n = ((EDSubscriber)subscribers.elementAt(i)).n;
      if(n instanceof EDStateManager){
        debug.debug("Encountered manager in dump");
      }else if(n instanceof EDState){
        debug.debug("Encountered EDState in dump");
      }else{
        debug.debug("Encountered " + n + " in dump");
      }
    }
    debug.debug("Finished dumping subscribers.");
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
  public void subscribe(Filter filter, EDNotifiable ednotifiable, 
  Comparable comparable){
    subscribe(new EDSubscriber(filter, ednotifiable, comparable));
  }
  
  /**
   * Actual subscription mechanism.
   */
  public void subscribe(EDSubscriber edsubscriber){
    debug.debug("New subscription with " + edsubscriber.f);
    synchronized(subscribers){
      // XXX - should this be in the synchronized block?
      subsHash.put(edsubscriber.n, edsubscriber);
      subscribers.add(edsubscriber);
      Collections.sort(subscribers);
      dumpSubscribers();
    }
    debug.debug("Finished subscription");
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
    debug.debug("Starting unsubscription");
    boolean returnVal;
    synchronized(subscribers){
      // XXX - should this be in the synchronized block?
      EDSubscriber s = (EDSubscriber)subsHash.get(n);
      subsHash.remove(n);
      returnVal = subscribers.remove(s);
      dumpSubscribers();
    }
    debug.debug("Unsubscription finished");
    return returnVal;
  }
  
  /**
   *  Puts a <code>Notification</code> on the queue for dispatch.
   *
   *  @param e  the event to send out
   */
  public void publish(Notification e){
    if(dispatching){
      eventQueue.enqueue(e);
    } else{
      debug.error("Cannot publish since EDBus has been shut down.");
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
          debug.debug("Dispatcher beginning dequeue");
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
        debug.warn("Dispatcher interrupted!");
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
    // Make a copy first.  Important because we want the dispatch-list to be
    // consistent from the very start of the dispatching.
    debug.debug("Starting dispatch of notification");
    Vector copyOfSubscribers = null;
    synchronized(subscribers) {
      copyOfSubscribers = new Vector(subscribers);
    }
    Enumeration elements = copyOfSubscribers.elements();
    boolean absorbed = false;
    while(elements.hasMoreElements() && !absorbed){
      EDSubscriber thisSubscriber =
      (EDSubscriber) elements.nextElement();
      if(thisSubscriber.acceptsNotification(e)){
        absorbed = thisSubscriber.n.notify(e);
      }
    }
    debug.debug("Dispatch complete");
  }
  
  public static void main(String[] args){
    EDBusTester.main(null);
  }
}// EDBus

