package psl.xues.ep;

import java.io.*;
import javax.xml.parsers.*;
import java.util.*;

import org.apache.log4j.Category;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import psl.xues.ep.event.EPEvent;
import psl.xues.ep.input.EPInput;
import psl.xues.ep.input.EPInputInterface;
import psl.xues.ep.output.EPOutput;

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
        EPInput epi = buildInputter((Element)inputters.item(i));
        if(epi != null)
          ep.inputters.put(epi.getName(), epi);
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
        EPOutput epo = buildOutputter((Element)outputters.item(i));
        if(epo != null)
          ep.outputters.put(epo.getName(), epo);
      }
    }
    
    // Finally, load the rules
    
    
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
      { inputter.getClass(), EPInputInterface.class }).newInstance(new Object[]
      { inputter, (EPInputInterface)ep });
      ep.inputters.put(inputterName, epi);
    } catch(Exception e) {
      debug.warn("Failed in loading inputter \"" + inputterName +
      "\", ignoring", e);
      return null;
    }
    
    // Success
    debug.info("Inputter \"" + inputterName + "\" loaded.");
    return epi;
  }
  
  private void buildOutputters(NodeList outputters) {
  }
  
  private EPOutput buildOutputter(Element outputter) {
    Element outputter = (Element)outputters.item(i);
    String outputterName = outputter.getAttribute("Name");
    String outputterType = outputter.getAttribute("Type");
    if(outputterName == null || outputterType == null ||
    outputterName.length() == 0 || outputterType.length() == 0) {
      debug.warn("Invalid outputter name or type detected, ignoring");
      continue;
    }
    
    // Try loading this one
    EPOutput epo = null;
    try {
      debug.debug("Loading outputter \"" + outputterName + "\"...");
      epo = (EPOutput)Class.forName(outputterName).getConstructor(new Class[]
      { outputter.getClass() }).newInstance(new Object[] { outputter });
      ep.outputters.put(outputterName, epo);
    } catch(Exception e) {
      debug.warn("Failed in loading outputter \"" + outputterName +
      "\", ignoring", e);
      continue;
    }
  }
  
}