package psl.xues.ed.acme;

import edu.cmu.cs.able.gaugeInfrastructure.*;
import edu.cmu.cs.able.gaugeInfrastructure.util.*;

import java.util.*;

import org.apache.log4j.Logger;

import siena.Siena;

/**
 * Event Distiller ACME Gauge Bus implementation.  This maps to a given
 * return value from the state machines.
 * <p>
 * Copyright (c) 2000-2002: The Trustees of Columbia University and the
 * City of New York.  All Rights Reserved.
 *
 * <!--
 * TODO:
 * - Support dynamic rule creation upon gauge creation request
 * - Additionally, consider probe deployment upon gauge creation request
 * -->
 *
 * @author Janak J Parekh
 * @version $Revision$
 */
public class EDGaugeImpl extends GaugeImpl {
  /** Debugging logger */
  private Logger debug = Logger.getLogger(EDGaugeImpl.class.getName());
  /** ED output bus */
  private Siena EDOutputBus = null;
  
  /**
   * CTOR.
   */
  public EDGaugeImpl(GaugeID gid, StringPairVector setupParams, 
  StringPairVector mappings, GaugeReportingBus bus, Siena EDOutputBus) {
    super(gid, setupParams, mappings, bus);

    // Store reference to EDOutputBus, and then subscribe to the values
    // that we are monitoring
    this.EDOutputBus = EDOutputBus;
    
    // Insert code to check setup parameters
    debug.debug("Called with mappings " + mappings);
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
   * Configure the gauge.
   *
   * @param configParams The parameters used to configure the gauge.
   * @return A boolean indicating success.
   */
  public boolean configure(StringPairVector configParams) {
    // At this moment, we do nothing, since we implicitly assume the
    // subscription is all we need.  In the future, this might contain
    // the information we need to create the ED gauge.
    
    //for (int i = 0; i < configParams.size(); i++) {
      // Insert code to process configuration parameters
    //}
    return true;
  }
  
  /** 
   * Query all the values.
   *
   * @param values The vector into which to place the gauged data.
   * @return A boolean indicating success.
   */
  public boolean queryAllValues(GaugeValueVector values) {
    // Insert code to fill values with the current value of all values that 
    // the gauge reports.
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
    this.setupParams.copyInto(setupParams);
    
    // Do a deep copy of the mappings SPV.  This is needed because we are
    // returning the values by reference.
    Enumeration enum = this.mappings.keys();
    while (enum.hasMoreElements()) {
      String key = (String)enum.nextElement();
      mappings.addElement(key, (String)this.mappings.get(key));
    }
    
    return true;
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
    // Insert code to return the current value of the value referred to 
    // by valueName
    return null;
  }
  
  /** 
   * This method is called periodically by the reporting thread
   * to generate a new value and report it (presumably, on the Siena bus).
   */
  public void reportNewValue() {
    // Insert code to report a new value
    return;
  }
  
  /**
   * Shut down this gauge.
   */
  public void shutdown() {
    return; // Do nothing for now.
  }
}
