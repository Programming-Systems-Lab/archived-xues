package psl.xues.ep.util;

import bsh.Interpreter;
import psl.xues.ep.EventPackager;
import siena.HierarchicalDispatcher;

/**
 * Event packager testing framework.
 *
 * @author Janak J Parekh
 * @version $Revision
 */
public class EPTest {
  public HierarchicalDispatcher siena1;
  public HierarchicalDispatcher siena2;
  
  public EPTest() {
    
    
    
  }
  
  public void buildSiena() {
    siena1 = new HierarchicalDispatcher();
    siena2 = new HierarchicalDispatcher();
  }
}