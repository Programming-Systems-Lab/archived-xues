package psl.xues.ed.acme;

import edu.cmu.cs.able.gaugeInfrastructure.util.*;
import edu.cmu.cs.able.gaugeInfrastructure.*;
import java.util.*;

/**
 * Siena communication layer for EDGauges.
 *
 * @author ABLE (http://www-2.cs.cmu.edu/afs/cs.cmu.edu/project/able/www/)
 * @version $Revision$
 */
public class SienaEDGauge 
extends edu.cmu.cs.able.gaugeInfrastructure.Siena.SienaGauge {
  protected String gaugeType = "EDGauge";
  
  /** 
   * Creates a new gauge.
   *
   * @param gaugeID The ID of the new gauge.
   * @param creatorID The ID of the gauge manager that created the gauge.
   * @param setupParams The parameters that the gauge uses to set itself up.
   * @param mappings The property that the <pre>date</pre> value is associated with.
   */
  public SienaNounLoadT(GaugeID gaugeID, String creatorID, 
  StringPairVector setupParams, StringPairVector mappings) {
    super(gaugeID, creatorID, setupParams, mappings);
    Global.debug("Creating implementation");
    impl = new NounLoadTImpl(gaugeID, setupParams, mappings, gaugeBus);
    
    Global.debug("Finalizing creation");
    finalizeCreation(creatorID, impl.consistentlyCreated());
  }
  
  /** 
   * A command-line interface to start the gauge.
   *
   * @param args -gt <i>gauge type</i><br>
   * -gn <i>gauge name</i><br>
   * -mt <i>model type</i><br>
   * -mn <i>model name</i><br>
   * [-setup <i>name=value</i>]<br>
   * [-mappings <i>name=property</i>]<br>
   * -creator <i>creator ID</i><br>
   * -senp <i>siena port</i><br>
   * [-debug]
   */
  public static void main(String [] args) {
    String creatorID = null;
    String gaugeName = null;
    String gaugeType = null;
    String modelName = null;
    String modelType = null;
    StringPairVector mappings = new StringPairVector();
    StringPairVector setupParams = new StringPairVector();
    boolean gotMappings = false;
    boolean gotSetup = false;
    // Process args
    boolean ok = true;
    for (int i = 0; i < args.length && ok; i++) {
      String tag = args [i];
      if (args [i].equals("-creator")) {
        creatorID = args [++i];
      }
      else if (args [i].equals("-gn")) {
        gaugeName = args [++i];
      }
      else if (args [i].equals("-gt")) {
        gaugeType = args [++i];
      }
      else if (args [i].equals("-mn")) {
        modelName = args [++i];
      }
      else if (args [i].equals("-mt")) {
        modelType = args [++i];
      }
      else if (args [i].equals("-setup")) {
        gotSetup = true;
        i++;
        while (i < args.length && args [i].charAt(0) != '-') {
          String setup = args [i];
          String name = setup.substring(0, setup.indexOf('='));
          String value = setup.substring(setup.indexOf('=') + 1);
          setupParams.addElement(name, value);
          i++;
        }
        i--;
      }
      else if (args [i].equals("-mappings")) {
        gotMappings = true;
        i++;
        while (i < args.length && args [i].charAt(0) != '-') {
          String mapping = args [i];
          String value = mapping.substring(0, mapping.indexOf('='));
          String property = mapping.substring(mapping.indexOf('=') + 1);
          mappings.addElement(value, property);
          i++;
        }
        i--;
      }
      else if (args [i].equals("-senp")) {
        edu.cmu.cs.able.gaugeInfrastructure.Siena.Initialization.initSiena(args [++i]);
      }
      else if (args [i].equals("-debug")) {
        edu.cmu.cs.able.gaugeInfrastructure.util.Global.debugFlag = true;
      }
      else {
        ok = false;
      }
    }
    
    if (!ok) {
      printUsageMessage();
      System.exit(0);
    }
    
    GaugeID gid = new GaugeID();
    gid.modelName = modelName;
    gid.modelType = modelType;
    gid.gaugeName = gaugeName;
    gid.gaugeType = gaugeType;
    
    SienaNounLoadT gauge = new SienaNounLoadT(gid, creatorID, setupParams, mappings);
  }
  
  private static void printUsageMessage() {
    System.out.println("Usage: SienaNounLoadT -creator <creatorID> -mt <modelType> -mn <modelName> -gt <gaugeType> -gn <gaugeName> -setup <name=value> -mappings <value=property> -senp <sienaport>");
  }
}
