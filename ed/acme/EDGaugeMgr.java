package psl.xues.ed.acme;

import java.util.*;
import java.io.*;

import edu.cmu.cs.able.gaugeInfrastructure.*;
import edu.cmu.cs.able.gaugeInfrastructure.Events.*;
import edu.cmu.cs.able.gaugeInfrastructure.util.*;
import edu.cmu.cs.able.gaugeInfrastructure.Siena.*;

import org.apache.log4j.Logger;

import psl.xues.ed.EDBus;

import siena.Siena;
import java.util.HashSet;

/**
 * ACME Gauge Manager for ED.  Bridges results from ED to the ACME gauge
 * bus.
 * <p>
 * Copyright (c) 2000-2002: The Trustees of Columbia University and the
 * City of New York.  All Rights Reserved.
 *
 * <!--
 * TODO:
 * - Fix queryMetaInfo
 * -->
 *
 * @author Janak J Parekh
 * @version $Revision$
 */
public class EDGaugeMgr
extends edu.cmu.cs.able.gaugeInfrastructure.Siena.SienaGaugeMgr {
  /** ACME gauge bus URL */
  public String gaugeBusURL = null;
  /** ACME gauge bus */
  private SienaGaugeReportingBus reportingBus = null;
  /**
   * List of gauge types being tracked.  We override the SienaGaugeMgr
   * implementation of this.  Each entry in gaugeTypes is
   * (String, HashSet of Strings of Gauge Names).
   */
  private HashMap gaugeTypes = new HashMap();
  /** Log4j debugger */
  private Logger debug = Logger.getLogger(EDGaugeMgr.class.getName());
  /** ED output bus */
  private EDBus EDOutputBus = null;
  
  /**
   * CTOR.
   *
   * @param gaugeBusURL The Siena URL to connect to for gauge transactions.
   * @param debugging Enable CMU debugging?
   * @param EDOutputBus The ED output bus to watch for gauge values.
   */
  public EDGaugeMgr(String gaugeBusURL, boolean debugging, EDBus EDOutputBus) {
    super();
    
    this.EDOutputBus = EDOutputBus;
    
    // Is debugging turned on?
    if(debugging)
      edu.cmu.cs.able.gaugeInfrastructure.util.Global.debugFlag = true;
    
    // Let the acme infrastructure ini tialize the Siena node
    this.gaugeBusURL = gaugeBusURL;
    edu.cmu.cs.able.gaugeInfrastructure.Siena.Initialization.
    initSiena(gaugeBusURL);
    
    // Now that the Siena node has been initialized, create our
    // reporting gauge bus
    reportingBus = new SienaGaugeReportingBus();
    
    // Finally, register ourselves with the reporting bus entity so we get
    // requests for gauge creation, deletion, etc.
    debug.debug("Registering myself");
    reportingBus.sienaBus.registerGaugeMgr(this);
    debug.debug("Registration complete");
  }
  
  /**
   * Creates a new gauge.  For the ED, this just adds a output result for us
   * to look for and proxy to the gauge bus if there's a match.
   *
   * @param gauge The ID of the gauge to create.
   * @param setupParams The setup parameters to pass to the new gauge.
   * @param mappings The mappings to pass to the new gauge.
   * @return A "dummy" gauge control that has the new ID.
   */
  public GaugeControl createGauge(GaugeID gauge, StringPairVector setupParams,
  StringPairVector mappings) {
    if (managesType(gauge.gaugeType)) { // Our gauge to manage
      debug.debug("createGauge called for gauge " + gauge);
      
      // "Dummy" handle.  What's the point?
      SienaGaugeMgrGaugeHandle gaugeHandle=new SienaGaugeMgrGaugeHandle(gauge);
      
      // Create the gauge and put it in the list
      debug.debug("About to start creating gauge");
      SienaEDGauge sed = new SienaEDGauge(gauge, getGaugeMgrID(), setupParams,
      mappings, EDOutputBus);
      gauges.put(gauge, sed);
      debug.debug("Gauge creation complete");
      
      // Now maintain the gaugeTypes we are currently managing
      HashSet gt = (HashSet)gaugeTypes.get(gauge.gaugeType);
      if(gt == null) { // Create it now, we can deal with the ref afterwards
        gt = new HashSet();
        gaugeTypes.put(gauge.gaugeType, gt);
      }
      gt.add(sed);
      
      return gaugeHandle;
    } else return null; // We aren't handling it
  }
  
  /**
   * Deletes the gauge associated with the gauge control.
   *
   * @param gauge The gauge control whose gauge ID is to be deleted.
   * @return A boolean indicating success.
   */
  public boolean deleteGauge(GaugeControl gauge) {
    return deleteGauge(gauge.getGaugeID());
  }
  
  /**
   * Deletes the gauge associated with the gauge ID.
   *
   * @param gauge The gauge ID representing the gauge to be deleted.
   * @return A boolean indicating success.
   */
  public boolean deleteGauge(GaugeID gaugeID) {
    debug.debug("deleteGauge called");
    SienaEDGauge seg = null;
    seg = (SienaEDGauge)gauges.get(gaugeID);
    if(seg == null) return false; // Can't shut down if we don't have a handle
    
    seg.shutdown();       // Notify the gauge it's about to be shut down
    gauges.remove(gaugeID); // Remove it from the hash
    
    // Remove it from the managing set
    HashSet gt = (HashSet)gaugeTypes.get(gaugeID.gaugeType);
    if(gt == null) { // Allow us to continue, but warn
      debug.warn("Inconsistency detected in gaugeTypes data structure");
    } else {
      if(gt.remove(seg) == false) {
        debug.warn("Inconsistency detected in a gaugeType gauge set");
      }
    }
    
    // Signal to the gauge infrastucture that the gauge was successfully
    // deleted.
    DeletedEvent event = new DeletedEvent(gaugeID);
    event.gaugeMgrID = getGaugeMgrID();
    event.status = true;
    reportingBus.reportDeleted(event);
    
    return true;
  }
  
  /**
   * Handle a shutdown request.  Delete all gauges to do this.
   */
  public void shutdown() {
    debug.debug("shutdown called");
    synchronized(gauges) {
      Iterator keys = gauges.keySet().iterator();
      while(keys.hasNext()) {
        GaugeID g = (GaugeID)keys.next();
        SienaEDGauge seg = (SienaEDGauge)gauges.remove(g);
        seg.shutdown();
        // Now tell the gauge infrastructure this gauge is gone
        DeletedEvent event = new DeletedEvent(g);
        event.gaugeMgrID = getGaugeMgrID();
        event.status = true;
        reportingBus.reportDeleted(event);
      }
    }
    // Finally shutdown our siena node
    try {
      reportingBus.sienaBus.siena.shutdown();
    } catch(Exception e) {
      debug.warn("Error in shutting down", e);
    }
  }
  
  /**
   * Returns the parameters that can be used to configure the gauge,
   * as well as the values reported by the gauge, for a particular
   * gauge type.
   *
   * @param gaugeType The type of the gauge about which to get information.
   * @param configParamsMeta
   * @param valuesMeta
   * @return
   */
  public boolean queryMetaInfo(String gaugeType,
  StringPairVector configParamsMeta, StringPairVector valuesMeta) {
    debug.debug("queryMetaInfo called");
    if(managesType(gaugeType)) { // Yes, we handle it
      // Get the gauge and examine its mappings to determine the values.
      // XXX - for now, just use the first gauge in the set of this type.
      HashSet hs = (HashSet)gaugeTypes.get(gaugeType);
      if(hs == null) {
        debug.error("Could not queryMetaInfo for type \"" + gaugeType + "\"");
        return false;
      }
      
      SienaEDGauge seg = null;
      if(hs.iterator().hasNext()) {
        seg = (SienaEDGauge)hs.iterator().next();
      } else {
        debug.error("Could not queryMetaInfo as type \"" + gaugeType + "\"" +
        " currently has no gauges");
      }
      
      // Return the values... XXX - all as string for now
      seg.getValueTypes().copyInto(valuesMeta);
      debug.debug("Returning value types " + valuesMeta);
      
      // No config params for now
      
      //int index = gaugeTypes.indexOf(gaugeType);
      //switch (index) {
      //  case 0:
      //    configParamsMeta.addElement("ReportingFrequency", "Int");
      //    valuesMeta.addElement("Load", "Float");
      //    break;
      //}
      return true;
    }
    
    return false;
  }
  
  /**
   * Do we manage this type of gauge?  This method gets called by other
   * parties to verify if it should send us gauge management requests for
   * these type(s).
   *
   * @return True if we do.
   */
  public boolean managesType(String gaugeType) {
    debug.debug("managesType called");
    if(gaugeType.startsWith("EDGauge")) return true;
    else return false;
  }
}
