package psl.xues.ep;

import java.io.*;
import javax.xml.parsers.*;

import org.apache.log4j.Category;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;

import org.w3c.dom.*;
import org.xml.sax.SAXException;

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
    ep.eventFormats = new Hashtable();
    
    for(int i=0; i < eventFormats.getLength(); i++) {
      // Load via reflection and store in event format hash
      Element eventFormat = eventFormats.item(i);
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
      Element inputter = inputters.item(i);
      String inputterName = inputter.getAttribute("Name");
      if(inputterName = null) {
        debug.warn("Invalid inputter detected, ignoring");
        continue;
      }
      
      // Try loading this one
      EPInput epi = null;
      try {
        debug.debug("Loading inputter \"" + inputterName + "\"...");
        epi = (EPInput)Class.forName(inputterName).newInstance();
      } catch(Exception e) {
        debug.warn("Failed in loading inputter \"" + inputterName +
        "\", ignoring", e);
        continue;
      }
    }
  }
}