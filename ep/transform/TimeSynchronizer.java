package psl.xues.ep.transform;

import java.util.HashMap;

import org.w3c.dom.Element;

import psl.xues.ep.event.EPEvent;
import psl.xues.ep.event.SienaEvent;
import psl.xues.ed.EDConst;

import siena.Notification;
import siena.AttributeValue;

/**
 * Time synchronizer transform module for the Event Packager.  Handles only
 * Siena-style events for now, but will attempt to convert another type of
 * EPEvent to Siena form.
 * <p>
 * When instantiating a TimeSynchronizer, you must specify a SourceAttribute
 * in the XML - this is the attribute that will be used for source
 * uniqueness (so that we can calibrate timestamps on a source-by-source
 * basis).  Note that this must exist in the Siena notification before it
 * hits the EP, as it is non-trivial to conclusively identify a given "source".
 * <p>
 * Copyright (c) 2002: The Trustees of Columbia University in the
 * City of New York.  All Rights Reserved.
 *
 * <!--
 * TODO:
 * - Consider handling other types of events (at least, FlatEvents)?? without
 *   requiring conversion (or using FlatEvents as the base event format)
 * - Consider handling non-flatevents which are nonconvertible in the first
 *   place
 * - Modify EPEvent so that a source is appended by EP?  Lots of unknown
 *   issues would have to be resolved, and it's probably not practically doable
 *   with Siena events anyway.
 * - Right now, we implicitly assume ED-style timestamp notification.  Detach
 *   and put into XML instead?
 * -->
 *
 * @author Janak J Parekh <janak@cs.columbia.edu>
 * @version $Revision$
 */
public class TimeSynchronizer extends EPTransform {
  /** Hash of the individual timeskews, by sourceAttribute */
  private HashMap sourceSkew;
  /** The attribute in the Siena notification that we're hashing by */
  private String sourceAttribute;
  
  /**
   * CTOR.
   *
   * @param el The Element with relevant information about our instantiation.
   */
  public TimeSynchronizer(EPTransformInterface ep, Element el) 
  throws InstantiationException {
    super(ep,el);
    
    // Attempt to obtain the sourceAttribute
    sourceAttribute = el.getAttribute("SourceAttribute");
    if(sourceAttribute == null || sourceAttribute.length() == 0) {
      throw new InstantiationException("Can't instantiate TimeSynchronizer, " +
      "no source attribute specified");
    }
    
    sourceSkew = new HashMap();
  }
  
  /**
   * Handle a transform request.
   */
  public EPEvent transform(EPEvent original) {
    // Is this a Siena event?
    EPEvent org = original;
    if(!(original.getFormat().equals("SienaEvent"))) {
      // Convert it
      org = original.convertEvent("SienaEvent");
      if(org == null) {
        debug.warn("Cannot transform event, as it is not convertible into "+
        " a Siena notification; doing nothing");
        return original;
      }
    }
    
    // Extract the notification and the timestamp
    Notification n = ((SienaEvent)org).getSienaEvent();
    // Get the time that *we* received the notification.  We will calibrate
    // the two to have a standardized skew.
    long ourT = org.getTimestamp();
    String src = n.getAttribute(sourceAttribute).stringValue();
    
    if(src == null || src.length() == 0 || src.equalsIgnoreCase("null")) {
      debug.warn("No source declared in notification, doing no calibration");
      return original; // Nothing to do
    }
    
    // Build the skew as an average
    Averager a = (Averager)sourceSkew.get(src);
    AttributeValue tstamp = n.getAttribute(EDConst.TIME_ATT_NAME);
    if (tstamp != null) {
        long t = tstamp.longValue();
        if(a == null) {
            sourceSkew.put(src, new Averager(ourT - t));
            n.putAttribute(EDConst.TIME_ATT_NAME, ourT);
        } else {
            t += a.updateSkew(ourT - t);
            n.putAttribute(EDConst.TIME_ATT_NAME, t);
        }
    }
    
    
    // Now, return the notification; the embedded Siena event has been changed
    // so we don't need to instantiate a new SienaEvent
    return org;
  }
  
  /**
   * Little "averager" helper class.  Allows us to incrementally add numbers and
   * maintain an average over time.  Useful as a skew "damping" effect.
   *
   * TODO:
   * - Consider more drastic damping?
   * - Should this not be an inner class?
   *
   * @author Janak J Parekh <janak@cs.columbia.edu>
   * @version $Revision$
   */
  class Averager {
    private int n = 0;
    private long sum = 0;
    private long avgskew = 0;
    
    public Averager() { ; }
    
    /**
     * CTOR with default initial skew.
     * 
     * @param initialSkew The initial skew.
     */
    public Averager(long initialSkew) {
      n = 1;
      sum = initialSkew;
      avgskew = initialSkew;
    }

    /**
     * Update our skew.  This is accomplished by adding it to the total,
     * and then recomputing the average.
     *
     * @param latestVal The latest data point to be averaged in.
     * @return The new average.
     */
    long updateSkew(long latestVal) {
      sum += latestVal;
      avgskew = sum / ++n;
      return avgskew;
    }
  }

  /**
   * Get the type.
   */
  public String getType() {
    return "TimeSynchronizer";
  }
}
