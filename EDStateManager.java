
package psl.xues;

import java.util.*;
import siena.*;

/**
 * Event Distiller State Machine Manager
 *
 * This class exists to manage many instances of state machines.  It
 * keeps track of the templates, garbage collects, finalizes them.
 * NOTE: This class no longer creates state machines.  See
 * "EDStateMachineSpecification", below, which acts like a creation
 * template mechanism.
 *
 * TODO:
 * - More efficient way of garbage collecting state machines
 * 
 * @author Janak J Parekh (jjp32@cs.columbia.edu)
 * @version 1.0
 *
 * $Log$
 * Revision 1.1  2001-01-22 02:11:54  jjp32
 *
 * First full Siena-aware build of XUES!
 *
 */
public class EDStateManager implements Runnable {
  private EventDistiller ed;
  private Siena siena;
  private int idCounter;
  /** This hashes vectors with EDStateMachineSpecifications */
  private Vector stateMachineTemplates;
  private Vector stateMachines;

  public EDStateManager(Siena siena, EventDistiller ed) {
    this.ed = ed;
    this.siena = siena;
    // Need to initialize stateMachineTemplates.  For now, we just
    // create a demo one.
    stateMachineTemplates.
      addElement(EDStateMachineSpecification.buildDemoSpec(siena,this));
  }

  /**
   * The reaper thread.
   */
  public void run() {
    //...
    try { Thread.currentThread().sleep(1000); }
    catch(InterruptedException ex) { ; }
  }

  /**
   * Callback by state machine to indicate it has been created, and
   * to add it to the queue for garbage-collection.
   */
  void addMachine(EDStateMachine m) {
    stateMachines.addElement(m);
  }

  /**
   * Method call made by EDStateMachines to indicate completion.
   */
  void finish(EDStateMachine m, Notification n) {
    // Propagate the notification up
    ed.sendPublic(n);
    // Garbage-collect the State Machine
    stateMachines.removeElement(m);
  }
}

/**
 * Class to specify a machine template.  We also store references to
 * existing state machines here.
 */
class EDStateMachineSpecification implements Notifiable {
  private Vector stateArray;
  private Notification action;
  private Siena siena;
  private EDStateManager edsm;

  public EDStateMachineSpecification(Siena siena, EDStateManager edsm) {
    this.siena = siena;
    this.edsm = edsm;
    stateArray = new Vector();
  }

  /** Demo test factory, don't use otherwise */
  public static EDStateMachineSpecification buildDemoSpec(Siena siena,
							  EDStateManager edsm)
  { 
    EDStateMachineSpecification edsms = 
      new EDStateMachineSpecification(siena,edsm);
    edsms.stateArray.addElement(new EDState("temperature","60",-1));
    edsms.action = new Notification();
    edsms.action.putAttribute("itworked","true");
    edsms.subscribe();
    return edsms;
  }

  /**
   * Subscribe based on the first state.  This way, we can create
   * instances when necessary.
   */
  public void subscribe() {
    try {
      siena.subscribe(((EDState)stateArray.elementAt(0)).buildSienaFilter(),
		      this);
    } catch(SienaException e) {
      e.printStackTrace();
    }
  }

  public void notify(Notification n) {
    if(EventDistiller.DEBUG) 
      System.err.println("[EDStateManagerSpecification] Received notification " + n);
    // Create the appropriate state machine(s).  We assume the state
    // machine will register itself with the manager.
    EDStateMachine sm = new EDStateMachine(siena, edsm, stateArray, 1,
					   action);
  }

  /** Unused Siena construct. */
  public void notify(Notification[] s) { ; }
}
