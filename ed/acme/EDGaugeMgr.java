package psl.xues.ed.acme;

import java.util.*;
import java.io.*;

import edu.cmu.cs.able.gaugeInfrastructure.*;
import edu.cmu.cs.able.gaugeInfrastructure.Events.*;
import edu.cmu.cs.able.gaugeInfrastructure.util.*;
import edu.cmu.cs.able.gaugeInfrastructure.Siena.*;

import org.apache.log4j.Logger;

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
  private GaugeReportingBus reportingBus = new SienaGaugeReportingBus();
  /** List of gauge types being tracked (unnecessary?) */
  //private Vector gaugeTypes = new Vector();
  /** Hash of gauges, indexed by gauge ID's */
  private HashMap gauges = new HashMap();
  /** Log4j debugger */
  private Logger debug = Logger.getLogger(EDGaugeMgr.class.getName());
  /** ED output bus */
  private Siena EDOutputBus = null;
  
  /**
   * CTOR.
   *
   * @param gaugeBusURL The Siena URL to connect to for gauge transactions.
   * @param debugging Enable CMU debugging?
   * @param EDOutputBus The ED output bus to watch for gauge values.
   */
  public EDGaugeMgr(String gaugeBusURL, boolean debugging, Siena EDOutputBus) {
    super();
    
    this.EDOutputBus = EDOutputBus;
    
    // Is debugging turned on?
    if(debugging)
      edu.cmu.cs.able.gaugeInfrastructure.util.Global.debugFlag = true;
    
    // Make sure the Reporting Gauge bus classes know of the target bus
    this.gaugeBusURL = gaugeBusURL;
    edu.cmu.cs.able.gaugeInfrastructure.Siena.Initialization.
    initSiena(gaugeBusURL);
    
    // Finally, register ourselves with the reporting bus entity so we get
    // requests for gauge creation, deletion, etc.
    debug.debug("Registering myself");
    ((SienaGaugeReportingBus)reportingBus).sienaBus.registerGaugeMgr(this);
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
    if (gauge.gaugeType.equals("EDGauge")) { // Our gauge to manage
      // "Dummy" handle.  What's the point?
      SienaGaugeMgrGaugeHandle gaugeHandle=new SienaGaugeMgrGaugeHandle(gauge);
      synchronized(gauges) {
        gauges.put(gauge, new SienaEDGauge(gauge, getGaugeMgrID(), setupParams,
        mappings, EDOutputBus));
      }
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
}
