package psl.xues.ed.acme;

import java.util.*;

import edu.cmu.cs.able.gaugeInfrastructure.util.*;
import edu.cmu.cs.able.gaugeInfrastructure.*;

import org.apache.log4j.Logger;
import siena.Siena;

/**
 * Siena transport layer for ED architectural gauges.
 *
 * @author Janak J Parekh
 * @version $Revision$
 */
public class SienaEDGauge 
extends edu.cmu.cs.able.gaugeInfrastructure.Siena.SienaGauge {
  /** Debugger */
  private Logger debug = Logger.getLogger(SienaEDGauge.class.getName());
  // Why do we need this?
  protected String gaugeType = "EDGauge";
  /** Reference to actual gauge implementation */
  private EDGaugeImpl impl = null;
  
  /** 
   * Creates a new gauge.
   *
   * @param gaugeID The ID of the new gauge.
   * @param creatorID The ID of the gauge manager that created the gauge.
   * @param setupParams The parameters that the gauge uses to set itself up.
   * @param mappings The property that the <pre>date</pre> value is associated 
   * with.
   * @param EDOutputBus The ED's output bus that we watch and bridge events
   * to the CMU gauge infrastructure.
   */
  public SienaEDGauge(GaugeID gaugeID, String creatorID, 
  StringPairVector setupParams, StringPairVector mappings, 
  Siena EDOutputBus) {
    super(gaugeID, creatorID, setupParams, mappings);
    debug.debug("Creating implementation");
    impl = new EDGaugeImpl(gaugeID, setupParams, mappings, gaugeBus,
    EDOutputBus);
    debug.debug("Finalizing creation");
    finalizeCreation(creatorID, impl.consistentlyCreated());
  }
  
  /** 
   * The method called when configuring a gauge.
   *
   * @param configParams The configuration parameters for the gauge as 
   * (name, value) pairs.
   *
   * @return <B>true</B> if the gauge could be configured successfully; 
   * <B>false</B> otherwise
   *
   */
  public boolean configure(StringPairVector configParams) {
    return impl.configure(configParams);
  }
  
  /** 
   * Returns all the gauge values that the gauge reports.
   *
   * @param values This is filled with all the values as a (valueName, 
   * propertyName, value) tuple
   *
   * @return <B>true</B> if the values were successfully determined; 
   * <B>false</B> otherwise.
   */
  public boolean queryAllValues(GaugeValueVector values) {
    return impl.queryAllValues(values);
  }
  
  /** The method called to query the state of the gauge
   *
   * @param setupParams Will be filled with the parameters with which the gauge 
   * was setup, as (name, value) pairs.
   * @param configParams Will be filled with the current configuration of the 
   * gauge, as (name, value) pairs
   * @param mappings Will be filled with the mappings for the gauge, as (name, 
   * propertyName) pairs.
   * @return <B>true</B> if the gauge successfully returns its state; 
   * <B>false</B> otherwise
   */
  public boolean queryState(StringPairVector setupParams, 
  StringPairVector configParams, StringPairVector mappings) {
    return impl.queryState(setupParams, configParams, mappings);
  }
  
  /** 
   * Called when a gauge is queried for a value. This provides an
   * alternative to the gauge reporting events.
   *
   * @param valueName The name of the value to be queried.
   * @return The value corresponding to the name passed in; <B>null</B> 
   * indicates that the gauge value could not be determined.
   */
  public String queryValue(String valueName) {
    return impl.queryValue(valueName);
  }
  
  /**
   * Shut down this gauge.  We notify the actual gauge of our intention
   * so it can do cleanup.
   */
  public void shutdown() {
    impl.shutdown();
  }
}
