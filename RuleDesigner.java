package psl.xues;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.tree.*;

import org.apache.xerces.parsers.SAXParser;
import org.xml.sax.*;

/**
 * A GUI allowing generation of XML specification for ED rules.
 */
class RuleDesigner extends JFrame implements Rulebase {

    // constants

    final Dimension WINDOW_SIZE = new Dimension(450, 700);
    final Dimension RULES_LIST_SIZE = new Dimension(300, 120);
    final Dimension RULES_BUTTONS_SIZE = new Dimension(100, 120);
    final Dimension TREE_SIZE = new Dimension(405, 300);
    final Dimension TREE_BUTTONS_SIZE = new Dimension(405, 25);

    /** The title for the window. */
    final String TITLE_STRING = "ED rules - XML generator ";
    /** Description for the different instantiation policies. */
    final static String[] INSTANTIATION_POLICY_INDEXES = {"1", "2", "3"};
    /** Description for the different instantiation policies. */
    final static String INSTANTIATION_POLICY_DESCRIPTION =
        "Choose between the following instantiation options:\n" +
        "1) rule will only be instantiated once\n" +
        "2) only one instance at any given time\n" +
        "3) new instance created anytime a previous instance is notified";
    private boolean DEBUG = true;

    // GUI components

    /** The list displaying all the rules in this rulebase. */
    private JList rulesList = null;

    private JTree tree = null;
    private JScrollPane treeScrollPane = new JScrollPane();
    private JButton treeOptionButton = new JButton("No Option");
    private TitledBorder treePanelBorder =
        new TitledBorder(new LineBorder(Color.black), "No Rule Selected");

    private TitledBorder statePanelBorder =
        new TitledBorder(new LineBorder(Color.black), "No Selection");

    // File menu components
    private JMenuBar jMenuBar = new JMenuBar();
    private JMenu jMenu_File = new JMenu("File");
    private JMenuItem jMenu_File_New = new JMenuItem("New...");
    private JMenuItem jMenu_File_Open = new JMenuItem("Open...");
    private JMenuItem jMenu_File_Save = new JMenuItem("Save...");
    private JMenuItem jMenu_File_SaveAs = new JMenuItem("Save As...");
    private JMenuItem jMenu_File_Exit = new JMenuItem("Exit...");

    // variables

    /** The XML parser. */
    SAXParser sxp = null;

    /** Name of file currently processed. */
    File currentFile = null;

    /** Name of default directory. */
    File currentDir = new File("C:/My Documents/Columbia.fl01/psl/xues");

    /** File chooser. */
    JFileChooser fileChooser = new JFileChooser();

    /** The state machine specifications. */
    private Vector specifications = new Vector();

    /** The rule that is currently selected. */
    EDStateMachineSpecification currentSpec = null;

    /** Used for default naming of rules. */
    private int ruleCounter = 0;

    /**
     * Starts the application.
     */
    public static void main(String[] args) {
        new RuleDesigner();
    }

    /**
     * Constructs a new RuleDesigner.
     */
    public RuleDesigner() {
        super();
        this.setSize(WINDOW_SIZE);
        this.setTitle(TITLE_STRING + "(no file specified)");
        this.addComponents();
        this.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
        this.show();
    }

    /** Add window components. */
    void addComponents() {
	setupMenu();

        // add components to viewport
        JPanel vp = new JPanel();
        vp.setLayout(new VerticalLayout());
        vp.add(createRulesPanel());
        vp.add(createTreePanel());
        vp.add(createStatePanel());

        // add to frame
        getContentPane().add(new JScrollPane(vp));
    }

    /** @return the panel with the representation of a state or action. */
    private JPanel createStatePanel() {
        JPanel statePanel = new JPanel();
        treePanel.setLayout(new VerticalLayout());
        treePanel.setBorder(statePanelBorder);

        // buttons
        JPanel treeButtonPanel = new JPanel();
        treeButtonPanel.setLayout(new GridLayout(1, 0));

        treeOptionButton.setEnabled(false);
        treeButtonPanel.add(treeOptionButton);

        treeButtonPanel.setSize(TREE_BUTTONS_SIZE);
        treePanel.add(treeButtonPanel);

        // tree representing a rule
        treeScrollPane.setPreferredSize(TREE_SIZE);
        treePanel.add(treeScrollPane);

        return treePanel;
    }

    /** @return the panel with the tree representing a rule. */
    private JPanel createTreePanel() {
        JPanel treePanel = new JPanel();
        treePanel.setLayout(new VerticalLayout());
        treePanel.setBorder(treePanelBorder);
        return treePanel;
    }

    /** @return the panel with the list of rules. */
    private JPanel createRulesPanel() {
        JPanel rulesPanel = new JPanel();
        rulesPanel.setLayout(new HorizontalLayout());
        rulesPanel.setBorder(new TitledBorder(new LineBorder(Color.black), "Rules"));

        // list of rules
        rulesList = new JList(new AbstractListModel() {
            public int getSize() { return RuleDesigner.this.specifications.size(); }
            public Object getElementAt(int index) {
                return ((EDStateMachineSpecification)
                    RuleDesigner.this.specifications.get(index)).getName(); }
        });
        rulesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        rulesList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e)  {
                setCurrentSpec((EDStateMachineSpecification)
                    specifications.get(rulesList.getSelectedIndex()));
            }
        });
        JScrollPane rulesScrollPane = new JScrollPane(rulesList);
        rulesScrollPane.setPreferredSize(RULES_LIST_SIZE);
        rulesPanel.add(rulesScrollPane);

        // buttons to modify rule properties
        JPanel ruleButtonPanel = new JPanel();
        ruleButtonPanel.setLayout(new GridLayout(0, 1));

        // create new a rule
        JButton newRule = new JButton("New");
        newRule.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // add a rule with a default name
                while(!addSpecification(-1, new EDStateMachineSpecification ("Rule " + (++ruleCounter))));
                rulesList.setSelectedIndex(specifications.size() - 1);
                rulesList.updateUI();
            }
        });
        ruleButtonPanel.add(newRule);

        // rename a rule
        JButton renameRule = new JButton("Rename");
        renameRule.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (RuleDesigner.this.currentSpec != null) {
		    String s = JOptionPane.showInputDialog
			(RuleDesigner.this, "Enter a new name for this rule:", "Rename rule",
			 JOptionPane.QUESTION_MESSAGE/*, null, null, currentSpec.getName()*/);
		    if (s != null) {
			if (hasName(s) || s.equals(""))
			    JOptionPane.showMessageDialog
				(RuleDesigner.this, "Could not change name to '" + s + "'\n" +
				 "name may exist or be invalid", "Bad Name", JOptionPane.ERROR_MESSAGE);
			else {
			    currentSpec.setName(s);
			    rulesList.updateUI();
			}
		    }
                }
                else JOptionPane.showMessageDialog(RuleDesigner.this,
                    "No rule selected!", "Error message", JOptionPane.ERROR_MESSAGE);
            }
        });
        ruleButtonPanel.add(renameRule);

        // change instantiation policy for this rule
        JButton changeInstPol = new JButton("Inst. Policy");
        changeInstPol.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (RuleDesigner.this.currentSpec != null) {
                    int selection = JOptionPane.showOptionDialog(RuleDesigner.this,
                        INSTANTIATION_POLICY_DESCRIPTION,
                        "Change Instantiation Policy", JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE, null, INSTANTIATION_POLICY_INDEXES,
                        INSTANTIATION_POLICY_INDEXES[currentSpec.getInstantiationPolicy()]);
                    if (selection != JOptionPane.CLOSED_OPTION)
                        RuleDesigner.this.currentSpec.setInstantiationPolicy(selection);
                }
                else JOptionPane.showMessageDialog(RuleDesigner.this,
                    "No rule selected!", "Error message", JOptionPane.ERROR_MESSAGE);
            }
        });
        ruleButtonPanel.add(changeInstPol);

        // Move the rule up the hierarchy
        JButton moveUpRule = new JButton("Move Up");
        moveUpRule.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (RuleDesigner.this.currentSpec != null) {
                    int currentPos = RuleDesigner.this.specifications.
                        indexOf(RuleDesigner.this.currentSpec);
                    if (currentPos > 0) {
                        RuleDesigner.this.specifications.add(currentPos - 1,
                            RuleDesigner.this.specifications.remove(currentPos));
                        rulesList.setSelectedIndex(currentPos - 1);
                    }
                }
                else JOptionPane.showMessageDialog(RuleDesigner.this,
                    "No rule selected!", "Error message", JOptionPane.ERROR_MESSAGE);
            }
        });
        ruleButtonPanel.add(moveUpRule);

        // Move the rule down the hierarchy
        JButton moveDownRule = new JButton("Move Down");
        moveDownRule.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (RuleDesigner.this.currentSpec != null) {
                    int currentPos = RuleDesigner.this.specifications.
                        indexOf(RuleDesigner.this.currentSpec);
                    if (currentPos < RuleDesigner.this.specifications.size() - 1) {
                        RuleDesigner.this.specifications.add(currentPos + 1,
                            RuleDesigner.this.specifications.remove(currentPos));
                        rulesList.setSelectedIndex(currentPos + 1);
                    }
                }
                else JOptionPane.showMessageDialog(RuleDesigner.this,
                    "No rule selected!", "Error message", JOptionPane.ERROR_MESSAGE);
            }
        });
        ruleButtonPanel.add(moveDownRule);

        ruleButtonPanel.setPreferredSize(RULES_BUTTONS_SIZE);
        rulesPanel.add(ruleButtonPanel);
        return rulesPanel;
    }

    /** Setup menu components and listeners. */
    private void setupMenu() {

        // add menu items to menu
        jMenu_File.add(jMenu_File_New);
        jMenu_File.add(jMenu_File_Open);
        jMenu_File.add(jMenu_File_Save);
        jMenu_File.add(jMenu_File_SaveAs);
        jMenu_File.addSeparator();
        jMenu_File.add(jMenu_File_Exit);

        //add listeners to menuitems
        jMenu_File_New.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                fileChooser.setCurrentDirectory(currentDir);
                if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    File f = addExtension(fileChooser.getSelectedFile());
                    try {
                        if (!f.createNewFile()) {
                            JOptionPane.showMessageDialog(RuleDesigner.this,
                                "Could not create new file: specified name exists");
                        }
                        else {
                            setCurrentFile(f, true);
                        }
                    }
                    catch(Exception ex) { ; }
                }
            }
        });

        jMenu_File_Open.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                fileChooser.setCurrentDirectory(currentDir);
                if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    setCurrentFile(fileChooser.getSelectedFile(), true);
                    if (sxp == null) {
                        sxp = new SAXParser();
                        sxp.setContentHandler(new EDHandler(RuleDesigner.this));
                    }
                    try { sxp.parse(new InputSource(new FileInputStream(currentFile))); }
                    catch(Exception ex) { ex.printStackTrace(); }
                }
            }
        });

        jMenu_File_Save.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                fileChooser.setCurrentDirectory(currentDir);
                if (currentFile == null) {
                    if (fileChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
			// do save as
			File f = addExtension(fileChooser.getSelectedFile());
			EDStateManager.write(f, RuleDesigner.this);
			setCurrentFile(f, false);
		    }
                }
                else EDStateManager.write(currentFile, RuleDesigner.this);
            }
        });

        jMenu_File_SaveAs.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                fileChooser.setCurrentDirectory(currentDir);
                if (fileChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                    File f = addExtension(fileChooser.getSelectedFile());
                    EDStateManager.write(f, RuleDesigner.this);
                    setCurrentFile(f, false);
                }
            }
        });

        jMenu_File_Exit.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                RuleDesigner.this.dispose();
                System.exit(0);
            }
        });

        // add menu to menubar
        jMenuBar.add(jMenu_File);
        this.setJMenuBar(jMenuBar);
    }

    /**
     * Changes the current file.
     * @param currentFile the filename to swich to
     * @param reset whether to reset the values
     */
    private void setCurrentFile(File currentFile, boolean reset) {
        if (DEBUG) System.out.println("changed current file to: " + currentFile);
        this.currentFile = currentFile;
        currentDir = new File(currentFile.getParent());


	// reset variables
        if (reset) {
            specifications = new Vector();
            currentSpec = null;
	    ruleCounter = 0;
        }

	// gui
        this.setTitle(TITLE_STRING + "(" + currentFile.getName() + ")");
        this.repaint();
    }

    /**
     * Updates the component, to reflect the element of the tree that is selected.
     * @param selNode the treeNode currently selected
     */
    private void updateTreeSelection(TreeNode selNode) {
        if (selNode instanceof SpecNode) {
            treeOptionButton.setText("Add Child State");
            treeOptionButton.setEnabled(true);
            treeOptionButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    EDState s = StateDialog.showNewStateDialog();
                    if (s != null) ((SpecNode)selNode).addChildState(s);
                }
            });
        }
        else if (selNode instanceof StateNode) {
            treeOptionButton.setText("Add Child State");
            treeOptionButton.setEnabled(true);
            treeOptionButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {

                }
            });
        }
        else if (selNode instanceof FolderNode) {
            treeOptionButton.setText("Add Action");
            treeOptionButton.setEnabled(true);
            treeOptionButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {

                }
            });
        }
        else {
            treeOptionButton.setText("No Option");
            treeOptionButton.setEnabled(false);
        }
    }

    /**
     * Changes the currently visualized specification.
     * @param currentSpec the specification to change to
     */
    private void setCurrentSpec(EDStateMachineSpecification currentSpec) {
        if (this.currentSpec == currentSpec) return;
        if (DEBUG) System.out.println("current spec is: " + currentSpec.getName());

        this.currentSpec = currentSpec;

        // update panels
        treeOptionButton.setText("No Option");
        treeOptionButton.setEnabled(false);

        treeScrollPane.getViewport().removeAll();
        if (currentSpec != null) {
            treePanelBorder.setText(currentSpec.getName());

            tree = new JTree(new SpecNode(currentSpec));
            treeScrollPane.getViewport().add(tree);
            tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
            tree.addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    int selRow = tree.getRowForLocation(e.getX(), e.getY());
                    TreePath selPath = tree.getPathForLocation(e.getX(), e.getY());
                    if(selRow != -1) {
                        updateTreeSelection((TreeNode)selPath.getLastPathComponent());
                    }
                }
            });
        }
        else treePanelBorder.setText("No Rule Selected");
    }

    /**************************************************
       methods inherited from the Rulebase interface
    **************************************************/

    /** @return the Vector of EDStateSpecification */
    public Vector getSpecifications() {
        return this.specifications;
    }

    /**
     * Returns whether the given name already belongs to one of the rules
     * @param s the candidate name
     * @return whether the given name already belongs to one of the rules
     */
    public boolean hasName(String s) {
        for (int i = 0; i < specifications.size(); i++)
            if (((EDStateMachineSpecification)specifications.get(i)).getName().equals(s))
                return true;
        return false;
    }

    /**
     * Adds a given specification.
     * @param specPosition the index where the specification should be set.
     *        could be -1, if the specification is to be placed at the end
     * @param edsms the specification to add
     * @return whether the rule was added
     */
    public boolean addSpecification(int specPosition, EDStateMachineSpecification edsms) {

	// don't allow duplicate names
	if (hasName(edsms.getName())) {
            publishError("duplicate name: " + edsms.getName());
            return false;
        }

        if (DEBUG) System.out.println("adding rule: " + edsms.getName());
        // where do we add it?
        int targetPosition = specifications.size();
        // at the end, if not specified
        if (specPosition >= 0 && specPosition <= targetPosition) targetPosition = specPosition;
        specifications.add(targetPosition, edsms);
	// gui
	rulesList.updateUI();
	rulesList.setSelectedIndex(targetPosition);
        return true;
    }

    /**
     * Publishes an error in the definition of a specification.
     * @param error a string representation of the error
     */
    public void publishError(String error) {
        JOptionPane.showMessageDialog(this, error);
    }

    /**************************************************
       static methods
    **************************************************/

    /**
     * Checks that the given file has the 'xml' extension,
     * and adds it otherwise.
     * @param f the file to check
     * @return the file, modified if necessary
     */
    public static File addExtension(File f) {
	String s = f.getName();
	if (s.length() < 5 || !s.substring(s.length() - 4).equals(".xml"))
	    f = new File(f.getParent(), s + ".xml");
	return f;
    }
}

/** Dialog to create or edit a state. */
class StateDialog extends JDialog {

    /** The source to which the input is added. */
    private TreeNode source;

    /** Value the user has input. */
    private EDState state;

    /**
     * Displays a dialog to create a new state.
     * @param parentComponent the parent frame
     * @param source the source where the input is added
     * @param state the state to edit, or null if a new state is being defined
     * @return the state defined by the user, or null
     *         if the user chose to cancel the operation
     */
    public StateDialog(JFrame parentComponent, TreeNode source, EDState state) {
        super(parentComponent, true);
        this.state = state;
        this.source = source;
        addComponents();

        if (state == null) setTitle("New State");
        else setTitle("Edit State");

        show();
    }

    private void addComponents() {

    }
}

class VerticalLayout extends GridLayout
{   private int xInset = 5;
    private int yInset = 2;
    private int yGap = 0;

    /** Constructor. */
    public VerticalLayout() { this(5); }

    /** Constructor.
     *  @param the gap between the components. */
    public VerticalLayout(int gap)
    {   super(0, 1, 0, 0);
        yGap = gap;
    }

    /**
     * Manages the layout of the components in the container to which the layout applies.
     * @param c - the container to which the layout applies.
     */
    public void layoutContainer(Container c)
    {   Insets insets = c.getInsets();
        int height = yInset + insets.top;

        Component[] children = c.getComponents();
        Dimension compSize = null;
        for (int i = 0; i < children.length; i++)
        {   compSize = children[i].getPreferredSize();
            children[i].setSize(compSize.width, compSize.height);
            children[i].setLocation( xInset + insets.left, height);
            height += compSize.height + yGap;
        }
    }

    /** @return the minimum layout size to display all components */
    public Dimension minimumLayoutSize(Container c)
    {   Insets insets = c.getInsets();
        int height = yInset + insets.top;
        int width = 0 + insets.left + insets.right;

        Component[] children = c.getComponents();
        Dimension compSize = null;
        for (int i = 0; i < children.length; i++)
        {
            compSize = children[i].getPreferredSize();
            height += compSize.height + yGap;
            width = Math.max(width, compSize.width + insets.left + insets.right + xInset*2);
        }
        height += insets.bottom;
        return new Dimension( width, height);
    }

    /** @return the preferred layout size to display all components */
    public Dimension preferredLayoutSize(Container c)
    {   return minimumLayoutSize(c);
    }
}

class HorizontalLayout extends GridLayout
{   private int xInset = 5;
    private int yInset = 5;
    private int xGap;

    /**
     * Constructor.     */
    public HorizontalLayout()
    {   this(5);
    }

    /**
     * Constructor.
     * @param the gap between the components.
     */
    public HorizontalLayout(int gap)
    {   super(1, 0, 0, 0);
        xGap = gap;
    }

    /**
     * Manages the layout of the components in the container to which the layout applies.
     * @param c - the container to which the layout applies.
     */
    public void layoutContainer(Container c)
    {   Insets insets = c.getInsets();
        int width = xInset + insets.left;
        Component[] children = c.getComponents();
        Dimension compSize = null;
        for (int i = 0; i < children.length; i++)
        {   compSize = children[i].getPreferredSize();
            children[i].setSize(compSize.width, compSize.height);
            children[i].setLocation(width, yInset + insets.top);
            width += compSize.width + xGap;
        }
    }

    /**
     * @return the minimum layout size to display all components in the container
     * to which the layout applies
     */
    public Dimension minimumLayoutSize(Container c)
    {   Insets insets = c.getInsets();
        int width = xInset + insets.left;
        int height = 0 + insets.top + insets.bottom;

        Component[] children = c.getComponents();
        Dimension compSize = null;
        for (int i = 0; i < children.length; i++)
        {   compSize = children[i].getPreferredSize();
            width += compSize.width + xGap;
            height = Math.max(height, compSize.height + insets.top + insets.bottom + yInset*2);
        }
        width += insets.right;
        return new Dimension( width, height);
    }

    /**
     * @return the preferred layout size to display all components in the container
     * to which the layout applies
     */
    public Dimension preferredLayoutSize(Container c)
    {   return minimumLayoutSize(c);
    }
}
