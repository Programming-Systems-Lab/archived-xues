package psl.xues.ep;

import java.util.HashSet;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import psl.xues.ep.event.EPEvent;
import psl.xues.ep.input.EPInput;
import psl.xues.ep.output.EPOutput;
import psl.xues.ep.transform.EPTransform;

/**
 * Event packager rule representation.
 * <p>
 * Copyright (c) 2002: The Trustees of Columbia University in the
 * City of New York.  All Rights Reserved.
 *
 * <!--
 * TODO:
 * - Consider getting rid of EventPackager reference
 * - Separate debuggers for each rule (as opposed to one for all rules)
 * - Handle sourceID changes more gracefully (perhaps two different ID's?)
 * -->
 *
 * @author Janak J Parekh <janak@cs.columbia.edu>
 * @version $Revision$
 */
public class EPRule {
  /** Reference to our EventPackager */
  private EventPackager ep = null;
  /** Name of this rule */
  private String ruleName = null;
  /** Set of inputs for this rule */
  private HashSet inputs = null;
  /** Array of transforms (ordered) for this rule */
  private ArrayList transforms = null;
  /** Set of outputs for this rule */
  private HashSet outputs = null;
  /** Our debugger */
  private Logger debug = Logger.getLogger(this.getClass());
  
  /**
   * CTOR.  Use an Element definition.
   *
   * @param el The DOM element defining us.
   */
  public EPRule(Element el, EventPackager ep) throws InstantiationException {
    this.ep = ep;
    
    // First instantiations
    inputs = new HashSet();
    transforms = new ArrayList();
    outputs = new HashSet();
    
    // Get the rule name
    ruleName = el.getAttribute("Name");
    if(ruleName == null || ruleName.length() == 0)
      throw new InstantiationException("Cannot instantiate rule: no rule name");
    
    // Process the inputs first
    NodeList inputsList = el.getElementsByTagName("Inputs");
    if(inputsList.getLength() == 0 ||
    inputsList.item(0).getChildNodes().getLength() == 0)
      throw new InstantiationException("No inputs for this rule");
    
    NodeList inputs = inputsList.item(0).getChildNodes();
    for(int i=0; i < inputs.getLength(); i++) {
      if(inputs.item(i).getNodeType() != Node.ELEMENT_NODE) continue;
      String inputName = ((Element)inputs.item(i)).getAttribute("Name");
      if(inputName == null || inputName.length() == 0) {
        debug.warn("Empty input name, skipping");
        continue;
      }
      // Try and look up the input
      EPInput epi = (EPInput)ep.inputters.get(inputName);
      if(epi == null) {
        debug.warn("Requested input \"" + inputName +"\" not found, skipping");
        continue;
      }
      
      // Add this name to our collection
      this.inputs.add(epi);
      
      // Notify the inputter that they have to deal with this rule.
      // XXX - possible race condition?  Have to make sure elsewhere in 
      // EPRule to wait until the right moment.
      epi.addRule(this);
    }
    
    // ... now the transforms.  Here, unlike inputs, we accept a rule with
    // no transforms.
    NodeList transformsList = el.getElementsByTagName("Transforms");
    if(transformsList.getLength() > 0) {
      NodeList transforms = transformsList.item(0).getChildNodes();
      for(int i=0; i < transforms.getLength(); i++) {
        if(transforms.item(i).getNodeType() != Node.ELEMENT_NODE) continue;
        String transformName = ((Element)transforms.item(i)).
        getAttribute("Name");
        if(transformName == null || transformName.length() == 0) {
          debug.warn("Empty transform name, skipping");
          continue;
        }
        // Try and look up the transform
        EPTransform ept = (EPTransform)ep.transformers.get(transformName);
        if(ept == null) {
          debug.warn("Requested transform \"" + transformName +
          "\" not found, skipping");
          continue;
        }
        
        // Add this transform to our list
        this.transforms.add(ept);
      }
    }
    
    // ... and finally the outputs.  Much more similar to inputs: an
    // output is required for a rule.
    NodeList outputsList = el.getElementsByTagName("Outputs");
    if(outputsList.getLength() == 0 ||
    outputsList.item(0).getChildNodes().getLength() == 0)
      throw new InstantiationException("No outputs for this rule");
    
    NodeList outputs = outputsList.item(0).getChildNodes();
    for(int i=0; i < outputs.getLength(); i++) {
      if(outputs.item(i).getNodeType() != Node.ELEMENT_NODE) continue;
      String outputName = ((Element)outputs.item(i)).getAttribute("Name");
      if(outputName == null || outputName.length() == 0) {
        debug.warn("Empty output name, skipping");
        continue;
      }
      // Try and look up the input
      EPOutput epo = (EPOutput)ep.outputters.get(outputName);
      if(epo == null) {
        debug.warn("Requested output \""+outputName+"\" not found, skipping");
        continue;
      }
      
      // Add this name to our collection
      this.outputs.add(epo);
    }
  }

  /**
   * Get the name of this rule.
   *
   * @return A string with the rule name.
   */
  public String getName() { return ruleName; }
  
  /**
   * Process some incoming data.
   *
   * @return A boolean indicating success (why?)
   */
  public boolean processRule(EPEvent epe) {
    EPEvent oldepe, newepe = epe;
    // First attempt transforms for this rule.  XXX - synchronization issues
    // will appear if we support incremental rule changes.
    for(int i=0; i < transforms.size(); i++) {
      oldepe = newepe;
      newepe = ((EPTransform)transforms.get(i)).transform(oldepe);
      if(newepe == null) {
        debug.warn("Could not apply transform \"" + 
        ((EPTransform)transforms.get(i)).getName() + "\", continuing");
        newepe = oldepe; // Restore
      }
    }

    // Now do the outputs
    EPOutput epo;
    Iterator tempoutputs = outputs.iterator();
    while(tempoutputs.hasNext()) {
      epo = (EPOutput)tempoutputs.next();
      if(epo.handleEvent(newepe) == false) {
        debug.warn("Could not output to \"" + epo.getName() + "\", continuing");
      }
    }

    // Right now, we always succeed.  XXX - might want to change this behavior.
    return true;
  }
}