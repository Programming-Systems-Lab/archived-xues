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
      System.exit(-1);                           // XXX - shouldn't exit here?
    }
    
    // Parse the configuration
    parseConfiguration(config);
  }
  
  private void parseConfiguration(Document config) {
    // Get the root
    Element e = config.getDocumentElement();
    
    // Get the event formats
    NodeList eventFormatsList = e.getElementsByTagName("EventFormats");
    if(eventFormatsList.getLength() == 0) {
      debug.fatal("No event formats specified, cannot proceed");
      System.exit(-1);                           // XXX - shouldn't exit here?
    }
    NodeList eventFormats = eventFormatsList.item(0).getChildNodes();
    if(eventFormats.getLength() == 0) {          // Double-check
      debug.fatal("No event formats specified, cannot proceed");
      System.exit(-1);                           // XXX
    }
    
    // Build event format representations for EP
    buildEventFormats(eventFormats);
    
    // Now load the inputters and outputters
    NodeList inputtersList = e.getElementsByTagName("InputFormats");
    if(inputtersList.getLength() == 0) {
      debug.warn("No inputters specified, EP won't be all that useful");
    } else {
      // Build inputters
      buildInputters(inputtersList.item(0).getChildNodes());
    }
  }
  
  private void buildEventFormats(NodeList eventFormats) {
    ep.eventFormats = new HashMap();
    
    for(int i=0; i < eventFormats.getLength(); i++) {
      // Load via reflection and store in event format hash
      Element eventFormat = (Element)eventFormats.item(i);
      String eventFormatName = eventFormat.getAttribute("Name");
      if(eventFormatName == null) {
        debug.warn("Invalid event format detected, ignoring");
        continue;
      }
      
      // Try loading this one
      EPEvent epe = null;
      try {
        debug.debug("Loading event format \"" + eventFormatName + "\"...");
        epe = (EPEvent)Class.forName(eventFormatName).newInstance();
      } catch(Exception e) {
        debug.warn("Failed in loading event format \"" + eventFormatName +
        "\", ignoring", e);
        continue;
      }
      
      // ... and store
      ep.eventFormats.put(eventFormatName, epe);
    }
  }
  
  private void buildInputters(NodeList inputters) {
    if(inputters.getLength() == 0) {
      debug.warn("No inputters found, EP won't be all that useful");
      return;
    }
    
    for(int i=0; i < inputters.getLength(); i++) {
      Element inputter = (Element)inputters.item(i);
      String inputterName = inputter.getAttribute("Name");
      String inputterType = inputter.getAttribute("Type");
      if(inputterName == null || inputterType == null ||
      inputterName.length() == 0 || inputterType.length() == 0) {
        debug.warn("Invalid inputter name or type detected, ignoring");
        continue;
      }
      
      // Try loading this one
      EPInput epi = null;
      try {
        debug.debug("Loading inputter \"" + inputterName + "\"...");
        // XXX - Should we be making a deep copy of the inputter element,
        // since we'return handing it to a potentially unknown constructor.
        epi = (EPInput)Class.forName(inputterType).getConstructor(new Class[]
        { inputter.getClass(), EPInputInterface.class }).newInstance(new Object[] 
        { inputter, (EPInputInterface)ep });
        ep.inputters.put(inputterName, epi);
      } catch(Exception e) {
        debug.warn("Failed in loading inputter \"" + inputterName +
        "\", ignoring", e);
        continue;
      }
    }
  }
  
  private void buildOutputters(NodeList outputters) {
    if(outputters.getLength() == 0) {
      debug.warn("No outputters found");
      return;
    }
    
    for(int i=0; i < outputters.getLength(); i++) {
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
  
}