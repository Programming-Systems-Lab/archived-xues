package psl.xues.ed.acme;

import edu.cmu.cs.able.gaugeInfrastructure.*;
import edu.cmu.cs.able.gaugeInfrastructure.util.*;

import java.util.*;

import org.apache.log4j.Logger;

import siena.Filter;
import siena.Siena;
import siena.Notifiable;
import siena.Op;
import siena.Notification;

import psl.xues.ed.EDBus;
import psl.xues.ed.EDNotifiable;

/**
 * Event Distiller ACME Gauge Bus implementation.  This maps to a given
 * return value from the state machines.
 * <p>
 * Copyright (c) 2000-2002: The Trustees of Columbia University and the
 * City of New York.  All Rights Reserved.
 *
 * <!--
 * TODO:
 * - Better setupParams handling?
 * - Support dynamic rule creation upon gauge creation request (currently,
 * we assume gauges are already instantiated... instead, setup parameter with
 * XML embedded?)
 * - Additionally, consider probe deployment upon gauge creation request
 * -->
 *
 * @author Janak J Parekh
 * @version $Revision$
 */
public class EDGaugeImpl extends GaugeImpl implements EDNotifiable {
  /** Debugging logger */
  private Logger debug = Logger.getLogger(EDGaugeImpl.class.getName());
  /** ED output bus */
  private EDBus EDOutputBus = null;
  
  /**
   * CTOR.
   */
  public EDGaugeImpl(GaugeID gid, StringPairVector setupParams,
  StringPairVector mappings, GaugeReportingBus bus, EDBus EDOutputBus) {
    super(gid, setupParams, mappings, bus);
    
    // Store reference to EDOutputBus, and then subscribe to the values
    // that we are monitoring
    this.EDOutputBus = EDOutputBus;
    
    // Insert code to check setup parameters
    debug.debug("Called with mappings " + mappings + " and SetupParams " +
    setupParams);
    
    // Build the subscription filter.
    Filter sienaFilter = new Filter();
    for(int i=0; i < mappings.size(); i++) {
      sienaFilter.addConstraint(mappings.nameAt(i), Op.ANY, "");
    }
    // Setup params handling: take them and make them more constraints
    // on the filter, except these are both LHS and RHS.
    for(int i=0; i < setupParams.size(); i++) {
      sienaFilter.addConstraint(setupParams.nameAt(i), setupParams.valueAt(i));
    }
    
    // Create a subscription for the left-hand-side of the mappings.
    // For now, we assume a conjoined set with ONE subscription.
    try {
      EDOutputBus.subscribe(sienaFilter, this, new Comparable() {
        public int compareTo(Object o) {
          return 0; // Doesn't matter
        }
      });
    } catch(Exception e) {
      debug.warn("Could not create subscription for gauge bridging", e);
    }
  }
  
  /**
   * Unused CTOR.  We override to ensure no other class is calling this one.
   */
  public EDGaugeImpl(GaugeID gid, StringPairVector setupParams,
  StringPairVector mappings, GaugeReportingBus bus) {
    super(gid, setupParams, mappings, bus);
    
    debug.error("Invalid constructor (2) called");
    return;
  }
  
  /**
   * Has the gauge been created "properly", i.e., are setup values
   * correct, and are probes deployed?
   *
   * @return A boolean indicating success.
   */
  public boolean consistentlyCreated() {
    return true; // Since it's not an explicit "creation", we assume success
  }
  
  /**
   * Configure the gauge.  For now, we do nothing, since we don't take any
   * parameters.
   *
   * @param configParams The parameters used to configure the gauge.
   * @return A boolean indicating success.
   */
  public boolean configure(StringPairVector configParams) {
    // At this moment, since no configuration is allowed, just ignore anything
    // we're given.
    return true;
  }
  
  /**
   * Query all the values.
   *
   * @param values The vector into which to place the gauged data.
   * @return A boolean indicating success.
   */
  public boolean queryAllValues(GaugeValueVector values) {
    // We can't supply values we don't have, so report nothing
    debug.warn("QueryAllValues not supported");
    return false;
  }
  
  /**
   * Returns the state of the gauge.
   *
   * @param setupParams The vector into which the setup parameters are copied.
   * @param configParams The configuration parameters for this gauge.
   * @param mappings The property that each gauge value is associated with.
   * @return Whether the state of the gauge could be queried.
   */
  public boolean queryState(StringPairVector setupParams,
  StringPairVector configParams, StringPairVector mappings) {
    // Copy the setup parameters out opaquely
    this.setupParams.copyInto(setupParams);
    
    // Copy out the latest mapping references
    Enumeration enum = this.mappings.keys();
    while (enum.hasMoreElements()) {
      String key = (String)enum.nextElement();
      mappings.addElement(key, (String)this.mappings.get(key));
    }
    
    // Nothing to configure, so leave it empty
    return true;
  }
  
  /**
   * Get a list of all the "value types", i.e., the LHS of the mappings along
   * with a type.
   *
   * @return A StringPairVector of the value types
   */
  public StringPairVector getValueTypes() {
    StringPairVector spv = new StringPairVector();
    Enumeration enum = this.mappings.keys();
    while(enum.hasMoreElements()) {
      String key = (String)enum.nextElement();
      spv.addElement(key, "String"); // XXX - hardcoded String for now
    }
    return spv;
  }
  
  
  /**
   * The method called to query the value of the gauge. This
   * provides a direct query for the gauge, as opposed to the
   * gauge reporting to value on the bus.  (Note: <I>it is assumed
   * that the querier know the property associated with this
   * value</I>).
   *
   * @param valueName The name of the value to query.
   * @return The corresponding value of the gauge.
   */
  public String queryValue(String valueName) {
    // We can't handle queryValue
    debug.warn("QueryValue not supported");
    return null;
  }
  
  /**
   * This method is called periodically by the reporting thread
   * to generate a new value and report it (presumably, on the Siena bus).
   */
  public void reportNewValue() {
    // Insert code to report a new value
    debug.warn("ReportNewValue not supported");
    return;
  }
  
  /**
   * Shut down this gauge.
   */
  public void shutdown() {
    debug.debug("Shutting down...");
    // Unsubscribe from the bus, so we don't report anything
    EDOutputBus.unsubscribe(this);
    return; // Do nothing for now.
  }
  
  /**
   * Siena notify mechanism.  Rewrite the attributes in the attribute-value
   * pairs and send it out to the gauge reporting bus.
   *
   * @param n The notification.
   */
  public boolean notify(Notification n) {
    debug.debug("Notify received");
    Iterator values = n.attributeNamesIterator();
    GaugeValueVector gvv = new GaugeValueVector();
    while(values.hasNext()) {
      String attr = (String)values.next();
      if(mappings.get(attr) != null) {
        // We have an architecturally-interesting data element
        gvv.addElement(attr, (String)mappings.get(attr),
        n.getAttribute(attr).stringValue());
      }
    }
    
    // Now report
    gaugeBus.reportMultipleValues(gaugeID, gvv);
    return false; // Keep on going
  }
}
