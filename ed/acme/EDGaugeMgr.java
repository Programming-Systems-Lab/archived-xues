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
  /** List of gauge types being tracked (unnecessary?) */
  //private Vector gaugeTypes = new Vector();
  /** Hash of gauges, indexed by gauge ID's */
  private HashMap gauges = new HashMap();
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
    debug.debug("createGauge called for gauge " + gauge);
    if (managesType(gauge.gaugeType)) { // Our gauge to manage
      // "Dummy" handle.  What's the point?
      debug.debug("Our gauge to handle, mappings are " + mappings);
      debug.debug("Creating a gauge handle");
      SienaGaugeMgrGaugeHandle gaugeHandle=new SienaGaugeMgrGaugeHandle(gauge);
      debug.debug("Handle created: " + gaugeHandle);
      debug.debug("About to start creating gauge");
      SienaEDGauge sed = new SienaEDGauge(gauge, getGaugeMgrID(), setupParams,
        mappings, EDOutputBus);
      synchronized(gauges) {
        debug.debug("Inserting gauge into List");
        gauges.put(gauge, sed);
      }
      debug.debug("Gauge creation complete");
      return gaugeHandle;
    } else return null; // We aren't handling it
  }
  
  /**
   * Deletes the gauge associated with the gauge ID.
   *
   * @param gauge The gauge to be deleted.
   * @return A boolean indicating success.
   */
  public boolean deleteGauge(GaugeControl gauge) {
    debug.debug("deleteGauge called");
    SienaEDGauge seg = null;
    synchronized(gauges) {
      seg = ((SienaEDGauge)gauges.get(gauge.getGaugeID()));
    }
    if(seg == null) return false; // Can't shut down if we don't have a handle
    
    seg.shutdown();       // Notify the gauge it's about to be shut down
    synchronized(gauges) {
      gauges.remove(gauge); // Remove it from the hash
    }
    
    // Signal to the gauge infrastucture that the gauge was successfully
    // deleted.
    DeletedEvent event = new DeletedEvent(gauge.getGaugeID());
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
    if (gaugeType.equals("EDGauge")) { // Yes, we handle it
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
    if(gaugeType.equals("EDGauge")) return true;
    else return false;
  }
}
