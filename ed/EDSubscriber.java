package psl.xues.ed;

import java.util.*;
import siena.*;

import org.apache.log4j.Category;

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
   * Resets the timebound for this subscriber. Used by states that have counter 
   * or loop features, where the timebound may be extended, as the subscriber 
   * is matched.
   *
   * @param l the new timebound within which this subscriber may be matched
   */
  void resetTimebound(long l){
    synchronized(f){
      f.removeConstraints(EDConst.TIME_ATT_NAME);
      f.addConstraint(EDConst.TIME_ATT_NAME, new AttributeConstraint(Op.LT, 
      new AttributeValue(l)));
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
