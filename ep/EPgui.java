package psl.xues.ep;

import psl.xues.ep.EventPackager;
import psl.xues.ep.input.EPInput;
import psl.xues.ep.output.EPOutput;
import psl.xues.ep.transform.EPTransform;
import psl.xues.ep.store.EPStore;

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
import java.util.ArrayList;
import java.util.Iterator;
import javax.swing.JPanel;
import java.awt.FlowLayout;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Color;

import org.apache.log4j.Logger;

/**
 * EP first-cut GUI.  Yes, it's a hack.
 *
 * @author Janak J Parekh
 * @version $Revision$
 */
public class EPgui extends JFrame implements Runnable {
  static Logger debug = Logger.getLogger(EPgui.class.getName());
  private EventPackager ep = null;
  private EPTableModel eptm = null;
  private JTable inputTable = null;
  
  public EPgui(EventPackager epkg) {
    super("Event Packager Administration");
    
    this.ep = epkg;
  }
  
  public void run() {
    // Set up the frame
    getContentPane().setLayout(new BorderLayout());
    
    eptm = new EPTableModel(ep);
    inputTable = new JTable(eptm);
    
    getContentPane().add(new JScrollPane(inputTable), "Center");
    
    JPanel buttonPanel = new JPanel();
    buttonPanel.setLayout(new FlowLayout());

    // Refresh button
    JButton refreshButton = new JButton("Rebuild");
    refreshButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        eptm.rebuildData();
      }
    });
    buttonPanel.add(refreshButton);
    
    // Hide button
    JButton hideButton = new JButton("Hide window");
    hideButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        hide();
      }
    });
    buttonPanel.add(hideButton);
    
    // Shutdown button
    JButton shutdownButton = new JButton("Shutdown EP");
    shutdownButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        shutdown();
      }
    });
    buttonPanel.add(shutdownButton);
    
    getContentPane().add(buttonPanel, "South");
    addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
        hide();
      }
    });

    setSize(600, 400);
    show();
    
    // Update loop
    while(!ep.inShutdown()) {
      repaint();
      try {
        Thread.currentThread().sleep(1000);
      } catch(Exception e) { ; }
    }
  }
  
  /**
   * Shutdown the EPgui.
   */
  public void shutdown() {
    dispose();
    if(ep != null) {
      ep.epgui = null;
      ep.shutdown();
    }
  }
  
  /**
   * Flash row
   */
  void flashRow(int row) {
    for(int i=0; i < inputTable.getColumnCount(); i++) {
      if(((DefaultTableCellRenderer)inputTable.getCellRenderer(row, i)).getBackground() == Color.WHITE) {
        ((DefaultTableCellRenderer)inputTable.getCellRenderer(row, i)).setBackground(Color.YELLOW);
      } else {
        ((DefaultTableCellRenderer)inputTable.getCellRenderer(row, i)).setBackground(Color.WHITE);
      }
    }
  }

  class EPTableModel extends AbstractTableModel {
    /**
     * Hashmap of all types (inputs, outputs, transforms, stores
     */
    private ArrayList data;
    private EventPackager ep;
    private long[] lastRowCount;
    
    public EPTableModel(EventPackager ep) {
      this.ep = ep;
      rebuildData();
    }
    
    public void rebuildData() {
      debug.debug("Rebuilding data");
      
      // This, to put it bluntly, sucks.
      data = new ArrayList();
      synchronized(ep.inputters) {
        Iterator i = ep.inputters.values().iterator();
        while(i.hasNext()) {
          data.add(i.next());
        }
      }
      
      synchronized(ep.outputters) {
        Iterator i = ep.outputters.values().iterator();
        while(i.hasNext()) {
          data.add(i.next());
        }
      }
      
      synchronized(ep.transformers) {
        Iterator i = ep.transformers.values().iterator();
        while(i.hasNext()) {
          data.add(i.next());
        }
      }
      
      synchronized(ep.stores) {
        Iterator i = ep.stores.values().iterator();
        while(i.hasNext()) {
          data.add(i.next());
        }
      }

      lastRowCount = new long[data.size()];
      debug.debug("Data rebuilt");
    }
    
    public int getRowCount() {
      return data.size();
    }
    
    public int getColumnCount() {
      return 4;
    }
    
    public String getColumnName(int column) {
      switch(column) {
        case 0: return "Class";
        case 1: return "Type";
        case 2: return "Name";
        case 3: return "# fired";
      }
      return null;
    }
    
    public Object getValueAt(int row, int column) {
      Object o;
      switch(column) {
        case 0: // Type
          o = data.get(row);
          if(o instanceof EPInput) {
            return "EPInput";
          } else if(o instanceof EPOutput) {
            return "EPOutput";
          } else if(o instanceof EPTransform) {
            return "EPTransform";
          } else if(o instanceof EPStore) {
            return "EPStore";
          } else {
            return "";
          }
        case 1: // EP type
          o = data.get(row);
          if(o instanceof EPInput) {
            return ((EPInput)o).getType();
          } else if(o instanceof EPOutput) {
            return ((EPOutput)o).getType();
          } else if(o instanceof EPTransform) {
            return ((EPTransform)o).getType();
          } else if(o instanceof EPStore) {
            return ((EPStore)o).getType();
          } else {
            return "";
          }
        case 2: // EP name
          o = data.get(row);
          if(o instanceof EPInput) {
            return ((EPInput)o).getName();
          } else if(o instanceof EPOutput) {
            return ((EPOutput)o).getName();
          } else if(o instanceof EPTransform) {
            return ((EPTransform)o).getName();
          } else if(o instanceof EPStore) {
            return ((EPStore)o).getName();
          } else {
            return "";
          }
        case 3:
          o = data.get(row);
          long count = 0;
          if(o instanceof EPInput) {
            count = ((EPInput)o).getCount();
          } else if(o instanceof EPOutput) {
            count = ((EPOutput)o).getCount();
          } else if(o instanceof EPTransform) {
            count = ((EPTransform)o).getCount();
          } else if(o instanceof EPStore) {
            count = ((EPStore)o).getCount();
          }
          // Any changes?
//          if(lastRowCount[row] != count) {
//            lastRowCount[row] = count;
//            flashRow(row);
//          }
          return "" + count;
        default: return "";
      }
    }
  }
}