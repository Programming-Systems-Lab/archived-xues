package psl.xues.ep.gui;

import psl.xues.ep.EventPackager;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.JTree;
import javax.swing.JFrame;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.AbstractTableModel;
import javax.swing.JTable;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * EP first-cut GUI.  Yes, it's a hack.
 *
 * @author Janak J Parekh
 * @version $Revision$
 */
public class EPgui extends JFrame {
  private EventPackager ep = null;
  public EPgui(EventPackager epkg) {
    super("Event Packager Administration");
    
    this.ep = epkg;
    
    // Set up the frame
    getContentPane().setLayout(new BorderLayout());
    
    EPTableModel eptm = new EPTableModel();
    JTable inputTable = new JTable(eptm);
    
    getContentPane().add(new JScrollPane(inputTable));
    addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
        if(ep != null) ep.shutdown();
        else System.exit(-1);
      }
    });
    pack();
    show();
  }
  
  public static void main(String[] args) {
    JFrame jf = new EPgui(null);
  }
}

class EPTableModel extends AbstractTableModel {
  public int getRowCount() {
    return 1;
  }
  
  public int getColumnCount() {
    return 4;
  }
  
  public String getColumnName(int column) {
    switch(column) {
      case 0: return "Type";
      case 1: return "Name";
      case 2: return "# fired";
      case 3: return "Last fired";
    }
    return null;
  }
  
  public Object getValueAt(int row, int column) {
    return "foo";
  }
}