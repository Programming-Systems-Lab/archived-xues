package psl.xues;

import java.util.*;
import siena.*;

/**
 * The Queue used in EDBus.
 * Buffers all the events while optionally reordering
 * <code>Notification</code>s based on the timestamp attribute.
 * 
 *
 * If you don't need reordering, do this:<BR>
 * <pre>
 * new EDQueue(EDQueue.REORDERING_DISABLED);
 * </pre>
 *
 * @author James Wu <jw402@columbia.edu>
 */

public class EDQueue {

    /**
     * The key for timestamp in a <code>Notification</code>
     */
    private static String TS_STR = "timestamp";
    

    // This section contains the options for starting this queue

    /**
     * Disabling reordering by timestamp.
     */
    public static int REORDERING_DISABLED = 0;

    /**
     * Enabling reordering by timestamp - this is the default.
     */
    public static int REORDERING_ENABLED = 1;



    private Vector q;
    private int ordering;

    public EDQueue (){
	this(REORDERING_ENABLED);
    }

    public EDQueue (int ordering){
	q = new Vector();
	this.ordering = ordering;
    }


    /**
     * Adding a <code>Notification</code> to the beginning of the queue.
     *
     * Note: if reordering is enabled, this method will no longer 
     * enforce existance of timestamps.  Instead, it will fall back to
     * a mode that disables event reordering.
     *
     *
     */
    public void enqueue(Notification n){
	if(ordering==REORDERING_DISABLED){
	    q.add(n);
	    return;
	}if(ordering==REORDERING_ENABLED){
	    synchronized(q){
		if(n.getAttribute(TS_STR)==null){
		    q.add(n);
		    ordering = REORDERING_DISABLED;
		    System.err.println("No timestamp - "
				       +"enter fallback mode.");
		    return;
		}else if((q.size()==0)||
			 (n.getAttribute(TS_STR).longValue()>=
			  ((Notification)q.elementAt(q.size()-1)
			   ).getAttribute(TS_STR).longValue())){
		    q.add(n);
		    return;
		}else{
		    int index = q.size();
		    do{
			index--;
		    }while((n.getAttribute(TS_STR).longValue()<
			    ((Notification)q.elementAt(index)
			     ).getAttribute(TS_STR).longValue())&&
			   index>0);	    
		    
		    q.insertElementAt(n, index);
		}
		return;
	    }
	}
	System.err.println("How did you get here? Error in EDQueue.enqueue()");
	System.exit(1);
    }

    
    /**
     * Removing a <code>Notification</code> from the end of the queue.
     *
     * Note: this method doesn't deal with reordering, since everything
     * should be in order already.
     */
    public Notification dequeue(){
	return (Notification)q.remove(0);
    }

    /**
     * Determine the size of the queue.
     *
     * @return size of the queue in int
     */
    public int size(){
	return q.size();
    }

    /**
     * Determine whether or not we can dequeue from the queue.  Given a
     * threshold time, this method figures out if there are any objects
     * older than that, and if such is the case, the method returns true,
     * so we can remove things from the queue.
     *
     *
     * @param maxTime Essentially, the threshold time after which we still
     *                want to keep objects in the queue.
     *                
     * 
     * @return Whether or not there is any object in the queue with timestamp
     *         earlier than the time given in the paramter.
     */
    public boolean canDequeue(long maxTime){
	if(q.size()==0){
	    return false;
	}else{
	    Notification n =
		((Notification)q.elementAt(0));
	    if(n.getAttribute(TS_STR)==null){
		return true;
	    }else{
		return (n.getAttribute(TS_STR).longValue()<=maxTime);
	    }	    
	}
    }

    /** For non-autoflush.. comment me */
    public boolean canDequeueWithLength(long waitTime){
	if(q.size()==0){
	    return false;
	}else{
	    Notification n =
		((Notification)q.elementAt(q.size()-1));
	    if(n.getAttribute(TS_STR)==null){
		return true;
	    }else{
		return canDequeue(n.getAttribute(TS_STR).longValue()-waitTime);
	    }
	}
    }


    public static void main(String[] args){
	simpleQueueTest();
    }

    private static void simpleQueueTest(){
	EDQueue q = new EDQueue();
	

	try{
	Notification n = new Notification();
	n.putAttribute("timestamp",System.currentTimeMillis());
	q.enqueue(n);

	Thread.currentThread().sleep(100);

	n = new Notification();
	n.putAttribute("timestamp",System.currentTimeMillis()-1000);
	q.enqueue(n);

	Thread.currentThread().sleep(100);


	n = new Notification();
	n.putAttribute("timestamp",System.currentTimeMillis());
	q.enqueue(n);

	Thread.currentThread().sleep(100);

	
	n = new Notification();
	n.putAttribute("timestamp",System.currentTimeMillis()-3000);
	q.enqueue(n);
	
	System.out.println("Can we dequeue? "
			   +q.canDequeue(System.currentTimeMillis()-2000));

	System.out.println("Start dequeuing:");

	while(q.q.size()>0){
	    Notification n1 = q.dequeue();
	    System.out.println(n1.toString());
	}
    
    }catch(Exception e){}
    }


}// EDQueue
