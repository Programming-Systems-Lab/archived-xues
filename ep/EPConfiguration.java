package psl.xues.ep;

import java.io.*;
import java.util.*;
import javax.xml.parsers.*;

import org.apache.log4j.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.xml.sax.SAXException;

import psl.xues.ep.EPRule;
import psl.xues.ep.event.EPEvent;
import psl.xues.ep.input.EPInput;
import psl.xues.ep.input.EPInputInterface;
import psl.xues.ep.output.EPOutput;
import psl.xues.ep.output.EPOutputInterface;
import psl.xues.ep.transform.EPTransform;
import psl.xues.ep.transform.EPTransformInterface;
import psl.xues.ep.store.EPStore;
import psl.xues.ep.store.EPStoreInterface;

/**
 * Event packager configuration parser.  Uses JAXP to handle the XML-formatted
 * configuration file.
 * <p>
 * Copyright (c) 2002: The Trustees of Columbia University in the
 * City of New York.  All Rights Reserved.
 *
 * <!--
 * TODO:
 * - Support propertysets for event formats(?), inputters, outputters,
 * and transformers.
 * - Dynamic (re)configuration
 * -->
 *
 * @author Janak J Parekh <janak@cs.columbia.edu>
 * @version $Revision$
 */
class EPConfiguration {
  /** Log4j debugger */
  static Logger debug =
  Logger.getLogger(EPConfiguration.class.getName());
  
  /** Reference to the event packager */
  private EventPackager ep = null;
  
  /**
   * CTOR.
   *
   * @param configFile The file to read initial configuration from.
   * @param ep Reference to the Event Packager being configured.
   */
  public EPConfiguration(String configFile, EventPackager ep) {
    this.ep = ep;
    
    // Try to parse the configFile into a DOM tree
    Document config = null;
    try {
      config = DocumentBuilderFactory.newInstance().newDocumentBuilder().
      parse(configFile);
    } catch(Exception e) {
      debug.fatal("Failed to read the configuration file", e);
      System.exit(-1); // XXX - should we be quitting here?
    }
    
    // Parse the configuration
    if(parseConfiguration(config) == false) {
      debug.fatal("Could not parse configuration file, exiting");
      System.exit(-1); // XXX - same as above
    }
  }
  
  /**
   * Given a DOM document, parse it and add any configurations to the
   * Event Packager.
   *
   * @param config The new configuration.
   * @return A boolean indicating success.
   */
  private boolean parseConfiguration(Document config) {
    // Get the root
    Element e = config.getDocumentElement();
    
    // Do we have event formats specified?
    NodeList eventFormatsList = e.getElementsByTagName("EventFormats");
    if(eventFormatsList.getLength() == 0 ||
    eventFormatsList.item(0).getChildNodes().getLength() == 0) {
      debug.fatal("No event formats specified, cannot proceed");
      return false;
    }
    // Try loading all specified event formats
    NodeList eventFormats = eventFormatsList.item(0).getChildNodes();
    // (But first) instantiate our container of verified event formats
    ep.eventFormats = new HashSet();
    for(int i=0; i < eventFormats.getLength(); i++) {
      if(eventFormats.item(i).getNodeType() != Node.ELEMENT_NODE) continue;
      String eventFormat = verifyEventFormat((Element)eventFormats.item(i));
      if(eventFormat != null)
        ep.eventFormats.add(eventFormat);
    }
    
    // Stores.  We build these first because other modules might need
    // access to stores.
    NodeList storesList = e.getElementsByTagName("Stores");
    ep.stores = new HashMap();
    if(storesList.getLength() > 0) { // No warnings if otherwise
      NodeList stores = storesList.item(0).getChildNodes();
      for(int i=0; i < stores.getLength(); i++) {
        if(stores.item(i).getNodeType() != Node.ELEMENT_NODE) continue;
        addPlugin(EPPlugin.STORE, (Element)stores.item(i));
      }
    }
    
    // Inputters
    NodeList inputtersList = e.getElementsByTagName("Inputters");
    ep.inputters = new HashMap();
    if(inputtersList.getLength() == 0 ||
    inputtersList.item(0).getChildNodes().getLength() == 0) {
      debug.warn("No inputters specified, EP won't be all that useful");
    } else {
      // Build inputters
      NodeList inputters = inputtersList.item(0).getChildNodes();
      for(int i=0; i < inputters.getLength(); i++) {
        if(inputters.item(i).getNodeType() != Node.ELEMENT_NODE) continue;
        addPlugin(EPPlugin.INPUT, (Element)inputters.item(i));
      }
    }
    
    // Transforms
    NodeList transformsList = e.getElementsByTagName("Transforms");
    ep.transformers = new HashMap();
    if(transformsList.getLength() > 0) { // No warnings if otherwise
      NodeList transforms = transformsList.item(0).getChildNodes();
      for(int i=0; i < transforms.getLength(); i++) {
        if(transforms.item(i).getNodeType() != Node.ELEMENT_NODE) continue;
        addPlugin(EPPlugin.TRANSFORM, (Element)transforms.item(i));
      }
    }
    
    // ...and outputters
    NodeList outputtersList = e.getElementsByTagName("Outputters");
    ep.outputters = new HashMap();
    if(outputtersList.getLength() == 0 ||
    outputtersList.item(0).getChildNodes().getLength() == 0) {
      debug.warn("No outputters specified, EP won't be all that useful");
    } else {
      // Build outputters
      NodeList outputters = outputtersList.item(0).getChildNodes();
      for(int i=0; i < outputters.getLength(); i++) {
        if(outputters.item(i).getNodeType() != Node.ELEMENT_NODE) continue;
        addPlugin(EPPlugin.OUTPUT, (Element)outputters.item(i));
      }
    }
    
    // Finally, load the rules
    NodeList rulesList = e.getElementsByTagName("Rules");
    ep.rules = new HashMap();
    if(rulesList.getLength() == 0 ||
    rulesList.item(0).getChildNodes().getLength() == 0) {
      debug.warn("No rules specified, EP won't be all that useful");
    } else {
      // Build rules
      NodeList rules = rulesList.item(0).getChildNodes();
      for(int i=0; i < rules.getLength(); i++) {
        if(rules.item(i).getNodeType() != Node.ELEMENT_NODE) continue;
        try {
          EPRule epr = new EPRule((Element)rules.item(i), ep);
          ep.rules.put(epr.getName(), epr);
        } catch(Exception ex) {
          debug.warn("Could not instantiate rule", ex);
          continue;
        }
      }
    }
    
    // All done
    return true;
  }
  
  /**
   * Not strictly necessary, but verify the presence of the event format
   * and place it in EP's "tested" list.
   *
   * @param eventFormat the EventFormat to be tested in DOM Element form.
   * @return The name of the EventFormat as a string, or null if non-verifiable
   */
  private String verifyEventFormat(Element eventFormat) {
    // Load via reflection and store in event format hash
    String eventFormatName = eventFormat.getAttribute("Name");
    if(eventFormatName == null) {
      debug.warn("Invalid event format detected, ignoring");
      return null;
    }
    
    // Attempt to load it
    try {
      Class.forName(eventFormatName);
    } catch(Exception e) {
      debug.warn("Failed in loading event format \"" + eventFormatName +
      "\", ignoring", e);
      return null;
    }
    
    // Successful load, store it
    debug.info("Successfully loaded event format \"" + eventFormatName + "\"");
    return eventFormatName;
  }
  
  /**
   * Add a plugin based on an XML DOM description of it.
   *
   * @param type The type of plugin this is.  Use EPPlugin constants.
   * @param data The XML data needed to construct this plugin.
   * @return An instance of EPPlugin if successful, else null.
   */
  private EPPlugin addPlugin(short type, Element data) {
    // Read the configuration
    String pluginName = data.getAttribute("Name");
    String pluginClass = data.getAttribute("Type");
    if(pluginName == null || pluginClass == null ||
    pluginName.length() == 0 || pluginClass.length() == 0) {
      debug.warn("Invalid plugin name \"" + pluginName + "\"" +
      "or type \"" + pluginClass + "\" detected, ignoring");
      return null;
    }
    
    // Obtain the parameters needed for the construction of the plugin
    Class epInterface = null;
    String pluginType = null;
    HashMap pluginList = null;
    switch(type) {
      case EPPlugin.INPUT:
        pluginType = "Inputter";
        epInterface = EPInputInterface.class;
        pluginList = ep.inputters;
        break;
      case EPPlugin.OUTPUT:
        pluginType = "Outputter";
        epInterface = EPOutputInterface.class;
        pluginList = ep.outputters;
        break;
      case EPPlugin.TRANSFORM:
        pluginType = "Transform";
        epInterface = EPTransformInterface.class;
        pluginList = ep.transformers;
        break;
      case EPPlugin.STORE:
        pluginType = "Store";
        epInterface = EPStoreInterface.class;
        pluginList = ep.stores;
        break;
      default:
        debug.warn("Invalid plugin type \"" + type + "\" specified, skipping");
        return null;
    }
    
    // Try constructing it
    EPPlugin epp = null;
    try {
      debug.debug(pluginType + " \"" + pluginName + "\" being loaded...");
      // XXX - Should we be making a deep copy of the element, since we're 
      // handing it to a potentially unknown constructor?
      epp = (EPPlugin)Class.forName(pluginClass).getConstructor(new Class[]
      { epInterface, Element.class }).newInstance(new Object[]
      { ep, data });
    } catch(Exception e) {
      debug.warn(pluginType + " \"" + pluginName +
      "\" failed to load, ignoring", e);
      return null;
    }
    
    // Success
    debug.info(pluginType + " loaded successfully.");
    pluginList.put(epp.getName(), epp);
    return epp;
  }
  
  /**
   * Build a new store given the XML DOM definition of it.
   *
   * @param store The description in DOM-tree form.
   * @return An instance of EPStore if successful, else null.
   */
  private EPStore addStore(Element store) {
    String storeName = store.getAttribute("Name");
    String storeType = store.getAttribute("Type");
    if(storeName == null || storeType == null ||
    storeName.length() == 0 || storeType.length() == 0) {
      debug.warn("Invalid store name or type detected, ignoring");
      return null;
    }
    
    // Load and instantiate this store
    EPStore eps = null;
    try {
      debug.debug("Loading store \"" + storeName + "\"...");
      // XXX - Should we be making a deep copy of the store element,
      // since we're handing it to a potentially unknown constructor?
      eps = (EPStore)Class.forName(storeType).getConstructor(new Class[]
      { EPStoreInterface.class, Element.class }).newInstance(new Object[]
      { (EPStoreInterface)ep, store });
    } catch(Exception e) {
      debug.warn("Failed in loading store \"" + storeName +
      "\", ignoring", e);
      return null;
    }
    
    // Success!
    return eps;
  }
}