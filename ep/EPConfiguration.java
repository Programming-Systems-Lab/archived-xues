package psl.xues.ep;

import java.io.*;
import java.util.*;
import javax.xml.parsers.*;
import org.apache.log4j.Category;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;
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
import psl.xues.ep.transform.EPTransform;

/**
 * Event packager configuration parser.  Uses JAXP to handle the XML-formatted
 * configuration file.
 *
 * TODO: Support propertysets for event formats(?), inputters, outputters,
 * and transformers.
 *
 * @author Janak J Parekh <janak@cs.columbia.edu>
 * @version $Revision$
 */
class EPConfiguration {
  /** log4j category class */
  static Category debug =
  Category.getInstance(EPConfiguration.class.getName());
  
  private EventPackager ep = null;
  
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
    
    // Do we have any inputters specified?  If so, load them
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
        EPInput epi = buildInputter((Element)inputters.item(i));
        if(epi != null)
          ep.inputters.put(epi.getName(), epi);
      }
    }
    
    // Transforms
    NodeList transformsList = e.getElementsByTagName("Transforms");
    ep.transformers = new HashMap();
    if(transformsList.getLength() > 0) { // No warnings if otherwise
      NodeList transforms = transformsList.item(0).getChildNodes();
      for(int i=0; i < transforms.getLength(); i++) {
        if(transforms.item(i).getNodeType() != Node.ELEMENT_NODE) continue;
        EPTransform ept = buildTransform((Element)transforms.item(i));
        if(ept != null)
          ep.transformers.put(ept.getName(), ept);
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
        EPOutput epo = buildOutputter((Element)outputters.item(i));
        if(epo != null)
          ep.outputters.put(epo.getName(), epo);
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
   * Build a new inputter based on a XML DOM description of it.
   *
   * @param inputter The description in DOM-tree form.
   * @return An instance of EPInput if successful, else null.
   */
  private EPInput buildInputter(Element inputter) {
    String inputterName = inputter.getAttribute("Name");
    String inputterType = inputter.getAttribute("Type");
    if(inputterName == null || inputterType == null ||
    inputterName.length() == 0 || inputterType.length() == 0) {
      debug.warn("Invalid inputter name or type detected, ignoring");
      return null;
    }
    
    // Try loading this one
    EPInput epi = null;
    try {
      debug.debug("Loading inputter \"" + inputterName + "\"...");
      // XXX - Should we be making a deep copy of the inputter element,
      // since we're handing it to a potentially unknown constructor?
      epi = (EPInput)Class.forName(inputterType).getConstructor(new Class[]
      { EPInputInterface.class, Element.class }).newInstance(new Object[]
      { (EPInputInterface)ep, inputter });
    } catch(Exception e) {
      debug.warn("Failed in loading inputter \"" + inputterName +
      "\", ignoring", e);
      return null;
    }
    
    // Success
    debug.info("Inputter \"" + inputterName + "\" loaded.");
    return epi;
  }
  
  /**
   * Build a new outputter given the XML DOM definition of it.
   *
   * @param outputter The description in DOM-tree form.
   * @return An instance of EPOutput if successful, else null.
   */
  private EPOutput buildOutputter(Element outputter) {
    String outputterName = outputter.getAttribute("Name");
    String outputterType = outputter.getAttribute("Type");
    if(outputterName == null || outputterType == null ||
    outputterName.length() == 0 || outputterType.length() == 0) {
      debug.warn("Invalid outputter name or type detected, ignoring");
      return null;
    }
    
    // Load and instantiate this outputter
    EPOutput epo = null;
    try {
      debug.debug("Loading outputter \"" + outputterName + "\"...");
      // XXX - Should we be making a deep copy of the outputter element,
      // since we're handing it to a potentially unknown constructor?
      epo = (EPOutput)Class.forName(outputterType).getConstructor(new Class[]
      { Element.class }).newInstance(new Object[] { outputter });
    } catch(Exception e) {
      debug.warn("Failed in loading outputter \"" + outputterName +
      "\", ignoring", e);
      return null;
    }

    // Success!
    return epo;
  }

  /**
   * Build a new transform given the XML DOM definition of it.
   *
   * @param outputter The description in DOM-tree form.
   * @return An instance of EPTransform if successful, else null.
   */
  private EPTransform buildTransform(Element transform) {
    String transformName = transform.getAttribute("Name");
    String transformType = transform.getAttribute("Type");
    if(transformName == null || transformType == null ||
    transformName.length() == 0 || transformType.length() == 0) {
      debug.warn("Invalid transform name or type detected, ignoring");
      return null;
    }
    
    // Load and instantiate this transform
    EPTransform ept = null;
    try {
      debug.debug("Loading transform \"" + transformName + "\"...");
      // XXX - Should we be making a deep copy of the transform element,
      // since we're handing it to a potentially unknown constructor?
      ept = (EPTransform)Class.forName(transformName).getConstructor(new Class[]
      { transform.getClass() }).newInstance(new Object[] { transform });
    } catch(Exception e) {
      debug.warn("Failed in loading transform \"" + transformName +
      "\", ignoring", e);
      return null;
    }

    // Success!
    return ept;
  }
}