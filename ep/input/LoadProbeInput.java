package psl.xues.ep.input;

import psl.xues.ep.event.EPEvent;
import psl.xues.ep.event.SienaEvent;
import psl.xues.ep.input.EPInput;

import siena.Notification;

import org.w3c.dom.Element;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.StringTokenizer;
import java.io.PrintWriter;

/**
 * Input mechanism to (launch and) handle Phil's load probe.
 * <p>
 * Copyright (c) 2000-2002: The Trustees of Columbia University and the
 * City of New York.  All Rights Reserved.
 *
 * <!--
 * TODO:
 * - Generalize the subprocess-style probe into another class
 * -->
 *
 * @author Janak J Parekh
 * @version $Revision$
 */
public class LoadProbeInput extends EPInput {
  private String programName = null;
  private int delay = 1000;
  
  public LoadProbeInput(EPInputInterface ep, Element el) 
  throws InstantiationException{
    super(ep, el);
    
    // Get the binary program name
    programName = el.getAttribute("ProgramName");
    if(programName == null || programName.length() == 0) {
      throw new InstantiationException("Could not identify program " +
      "location/name");
    }
    
    // Delay attribute?
    String delay = el.getAttribute("Delay");
    if(delay != null && delay.length() != 0) {
      try {
        this.delay = Integer.parseInt(delay);
      } catch(Exception e) {
        throw new InstantiationException("Invalid delay provided");
      }
    }
  }
  
  /**
   * Get the "type" of input.
   *
   * @return The type, as String.
   */
  public String getType() {
    return "LoadProbeInput";
  }
  
  /**
   * Run method.  Start up our subprocess and then have fun.
   */
  public void run() {
    Process probeProcess = null;
    try {
      probeProcess = Runtime.getRuntime().exec(programName + " " + delay);
    } catch(Exception e) {
      debug.error("Could not start process", e);
      return;
    }
    
    // Grab the input streams
    BufferedReader in = null;
    BufferedReader err = null;
    PrintWriter out = null;
    
    try {
      in = new BufferedReader(new
      InputStreamReader(probeProcess.getInputStream()));
      err = new BufferedReader(new
      InputStreamReader(probeProcess.getErrorStream()));
      out = new PrintWriter(probeProcess.getOutputStream(), true);
    } catch(Exception e) {
      debug.error("Could not set up streams for subprocess", e);
      probeProcess.destroy();
      return;
    }
    
    // Start the err tracker
    new Thread(new ErrTracker(err)).start();
    
    // Poll for input and inject it when appropriate
    while (!shutdown) {
      String input = null;
      try {
        input = in.readLine();
      } catch(Exception e) {
        debug.error("Failed in readLine", e);
        break;  // Initiate shutdown
      }
      
      // Now parse the input
      StringTokenizer tok = new StringTokenizer(input);
      
      float cpu;
      int ramFree, systemRunLength, diskQueueRunLength;
      
      try {
        cpu = Float.parseFloat(tok.nextToken());
        ramFree = Integer.parseInt(tok.nextToken());
        systemRunLength = Integer.parseInt(tok.nextToken());
        diskQueueRunLength = Integer.parseInt(tok.nextToken());
      } catch(Exception e) {
        debug.warn("Could not parse input \"" + input + "\"");
        continue; // Go back and try again
      }
      
      // Now build the event and send it out
      Notification n = new Notification();
      n.putAttribute("CPU", cpu);
      n.putAttribute("RAMFree", ramFree);
      n.putAttribute("SystemRunLength", systemRunLength);
      n.putAttribute("DiskQueueRunLength", diskQueueRunLength);
      SienaEvent se = new SienaEvent(getName(), n);
      ep.injectEvent(se);
    }
    
    // We're in shutdown, close up
    debug.debug("Shutting down process...");
    if(out != null) out.println("exit");
    try {
      if(in != null) in.close();
      if(err != null) err.close();
      if(out != null) out.close();
      if(probeProcess != null) {
        probeProcess.destroy();
        // Wait for it to actually shut down
        probeProcess.waitFor();
      }      
    } catch(Exception e) {
      debug.warn("Error in shutdown, may not be completely successful", e);
    }
  }
 
  /**
   * Shutdown.  XXX - force process closed, etc.?
   */
  public void shutdown() {
    shutdown = true;
  }
  
  /**
   * Error tracker.  Perhaps consider making this based on a stream, not
   * reader, in the future.
   */
  class ErrTracker implements Runnable {
    private BufferedReader err;
    
    public ErrTracker(BufferedReader errReader) {
      this.err = errReader;
    }
    
    public void run() {
      try {
        err.readLine();
      } catch(Exception e) {
        debug.warn("Could not read err stream, shutting down err reader");
        return;
      }
    }
  }
}
  