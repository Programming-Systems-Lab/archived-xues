/**
 * This file contains all the objects that are needed in the XML generator / rule designer
 * for the tree representation of a rule. Each object can represent a specification,
 * a state, an action, or a folder of actions. Each object holds a referece to the state,
 * or edsms, or notif it represents, and a vector representation of the variables that
 * need to be represented in the tree, as well as methods that are called by the GUI to
 * modify the represented object.
 *
 * @author eb659
 */

package psl.xues;

import java.util.*;
import javax.swing.tree.*;
import siena.*;

/** Represents a SMSpec in the tree of the RuleDesigner. */
class SpecNode implements TreeNode {

    /** The specification represented. */
    EDStateMachineSpecification spec;

    /** The children nodes. */
    private Vector children = new Vector();

    /**
     * Constructs a new SpecNode
     * @param spec the represented specification
     */
    public SpecNode(EDStateMachineSpecification spec) {
        this.spec = spec;
        for (int i = 0; i < spec.getInitialStates().size(); i++)
            children.add(new StateNode((EDState)spec.getInitialStates().get(i), this));
    }

    /** @param the string representation of this object */
    public String toString() { return spec.getName(); }

    /**
     * Adds an initial state to the represented specification
     * @param newState the state to add to the spedification
     * @return a new node that represents the added node, or null
     *         if the state could not be added due to a name conflict
     */
    public TreeNode addState(EDState newState) {
        if (!spec.addInitialState(newState)) return null;
        StateNode newNode = new StateNode(newState, this);
        children.add(newNode);
        return (TreeNode)newNode;
    }

    /********************************************************
        methods inherited from the TreeNode interface
    ********************************************************/

    public Enumeration children() { return children.elements(); }
    public boolean getAllowsChildren() { return true; }
    public TreeNode getChildAt(int childIndex) { return (TreeNode)children.get(childIndex); }
    public int getChildCount() { return children.size(); }
    public int getIndex(TreeNode node) { return children.indexOf(node); }
    public TreeNode getParent() { return null; }
    public boolean isLeaf() { return (children.size() == 0); }
}

/**
 * An object that represents a state, in the representation
 * of the tree used in the RuleGenerator.
 */
class StateNode implements TreeNode {

    /** Names of folders that appear as defining this state. */
    public static final String[] FOLDER_NAMES =
        {"actions", "fail_actions"};
    public static int ACTIONS = 0;
    public static int FAIL_ACTIONS = 1;

    /**
     * The parent of this node, in the tree representation.
     * Could be a machine, or another state.
     */
    private TreeNode parent;

    /** The state represented by this node. */
    EDState state;

    /**
     * The children of this node, in the tree representation.
     * make sure to add child states to this vector, as they are added to the state.
     */
    private Vector treeChildren = new Vector();

    /** The constraints on the represented state in vector form. */
    Vector constraints = new Vector();
    /** Parallel vector, holding the names on which the constraints are placed. */
    Vector constraintNames = new Vector();

    /**
     * Constructs a new stateNode.
     * @param e the state represented
     * @param parent the parent node
     */
    public StateNode(EDState state, TreeNode parent) {
        this.state = state;
        this.parent = parent;

        // children nodes
        EDStateMachineSpecification spec = getSpecification();
        for (int i = 0; i < state.getChildren().length; i++)
            treeChildren.add(new StateNode(
                (EDState)spec.getStates().get(state.getChildren()[i]), this));
        for (int i = 0; i < FOLDER_NAMES.length; i++)
            treeChildren.add(new FolderNode(FOLDER_NAMES[i], this));

        // constraints
        Hashtable hc = state.getConstraints();
        for (Enumeration en = hc.keys(); en.hasMoreElements(); ) {
            String s = en.nextElement().toString();
            constraintNames.add(s);
            constraints.add(hc.get(s));
        }
    }

    public EDStateMachineSpecification getSpecification() {
        if (parent instanceof SpecNode)
            return ((SpecNode)parent).spec;
        else return ((StateNode)parent).getSpecification();
    }

    /** @param the string representation of this object */
    public String toString() { return state.getName(); }

    /**
     * Adds a constraint to the filter of the represented state.
     * @param n the name of the attribute
     * @param constr the constraint to put on that name
     * @return false if the constraint could not be added,
     *         due to a naming conflict
     */
    public boolean addConstraint(String n, AttributeConstraint constr) {
        if (state.addConstraint(n, constr) == false) return false;
        constraintNames.add(n);
        constraints.add(constr);
        return true;
    }

    /**
     * Removes a constraint from the filter of the represented state.
     * @param n the name of the attribute
     */
    public void removeConstraint(String n) {
        removeConstraint(constraintNames.indexOf(n));
    }

    /**
     * Removes a constraint from the filter of the represented state.
     * @param i the index of the attribute
     */
    public void removeConstraint(int i) {
        String n = constraintNames.remove(i).toString();
        constraints.remove(i);
        state.getConstraints().remove(n);
    }

    /**
     * Removes a constraint from the filter of the represented state.
     * @param i the index of the attribute
     * @param newName the new name for the constraint
     * @return false if the name could not be changed due to a naming conflict
     */
    public boolean renameConstraint(int i, String newName) {
        if (newName.equals("") || !state.addConstraint
            (newName, (AttributeConstraint)constraints.get(i)))
            return false;
        String oldName = constraintNames.remove(i).toString();
        constraintNames.add(i, newName);
        state.getConstraints().remove(oldName);
        return true;
    }

    /**
     * Adds an child state to the represented state
     * @param newState the child state to add
     * @return a new node that represents the added node, or null
     *         if the state could not be added due to a name conflict
     */
    public TreeNode addState(EDState newState) {
        EDStateMachineSpecification spec = getSpecification();
        if (!spec.addState(newState)) return null;
        state.addChildName(newState.getName());
        StateNode newNode = new StateNode(newState, this);
        treeChildren.add(treeChildren.size() - FOLDER_NAMES.length, newNode);
        return (TreeNode)newNode;
    }

    /********************************************************
        methods inherited from the TreeNode interface
    ********************************************************/

    public Enumeration children() { return treeChildren.elements(); }
    public boolean getAllowsChildren() { return true; }
    public TreeNode getChildAt(int childIndex) { return (TreeNode)treeChildren.get(childIndex); }
    public int getChildCount() { return treeChildren.size(); }
    public int getIndex(TreeNode node) { return treeChildren.indexOf(node); }
    public TreeNode getParent() { return parent; }
    public boolean isLeaf() { return false; }
}

/**
 * An object that represents a state, in the representation
 * of the tree used in the RuleGenerator.
 */
class FolderNode implements TreeNode {

    /** The name for this folder. */
    private String name = null;

    /** The parent of this node, in the tree representation. */
    private StateNode parent;

    /** The state that owns the actions in this folder. */
    private EDState state;

    /** The children of this node, in the tree representation. */
    private Vector children = new Vector();

    /**
     * Constructs a new stateNode.
     * @param name the name for this folder
     * @param e the state represented
     * @param parent the parent node
     */
    public FolderNode(String name, StateNode parent) {
        this.name = name;
        this.parent = parent;
	this.state = parent.state;

        // children nodes
        String[] actionNames;
        if (name.equals(StateNode.FOLDER_NAMES[StateNode.ACTIONS]))
            actionNames = state.getActions();
        else actionNames = state.getFailActions();

        EDStateMachineSpecification spec = parent.getSpecification();
        for (int i = 0; i < actionNames.length; i++)
            children.add(new ActionNode(actionNames[i],
                (Notification)spec.getActions().get(actionNames[i]), this));
    }

    /** @param the string representation of this object */
    public String toString() { return name; }

    /**
     * Adds an action to this state
     * @param newState the state to add to the spedification
     * @return a new node that represents the added node, or null
     *         if the state could not be added due to a name conflict
     */
    public TreeNode addAction(String actionName, Notification action) {
        EDStateMachineSpecification spec = ((StateNode)parent).getSpecification();
        if (!spec.addAction(actionName, action)) return null;

        if (name.equals(StateNode.FOLDER_NAMES[0])) {
            state.addActionName(actionName);
            ActionNode newNode = new ActionNode(actionName, action, this);
            children.add(newNode);
            return (TreeNode)newNode;
        }
        else {
            state.addFailActionName(actionName);
            ActionNode newNode = new ActionNode(actionName, action, this);
            children.add(newNode);
            return (TreeNode)newNode;
        }
    }

    /********************************************************
        methods inherited from the TreeNode interface
    ********************************************************/

    public Enumeration children() { return children.elements(); }
    public boolean getAllowsChildren() { return true; }
    public TreeNode getChildAt(int childIndex) { return (TreeNode)children.get(childIndex); }
    public int getChildCount() { return children.size(); }
    public int getIndex(TreeNode node) { return children.indexOf(node); }
    public TreeNode getParent() { return parent; }
    public boolean isLeaf() { return (children.size() == 0); }
}

/**
 * An object that represents an action, in the representation
 * of the tree used in the RuleGenerator.
 */
class ActionNode implements TreeNode {

    /** The name for this action. */
    String name = null;

    /** The parent of this node, in the tree representation. */
    private FolderNode parent;

    /** The action represented by this node. */
    private Notification action = null;

    /** The attributes of this action. */
    Vector attributes = new Vector();
    /** Parallel vector with the names for the attributes. */
    Vector attributeNames = new Vector();

    /**
     * Constructs a new ActionNode.
     * @param name the name for this action
     * @param action the action represented
     * @param parent the parent node
     */
    public ActionNode(String name, Notification action, FolderNode parent) {
        this.name = name;
        this.action = action;
        this.parent = parent;

        // children nodes
        for (Iterator iter = action.attributeNamesIterator(); iter.hasNext();) {
            String s = iter.next().toString();
            attributeNames.add(s);
            attributes.add(action.getAttribute(s));
        }
    }

    /**
     * Renames an attribute of the represented action.
     * @param i the index of the attribute
     * @param newName the new name for the constraint
     * @return false if the name could not be changed due to a naming conflict
     */
    public boolean renameAttribute(int i, String newName) {
        if (newName.equals("") || action.getAttribute(newName) != null)
            return false;
        String oldName = attributeNames.remove(i).toString();
        attributeNames.add(i, newName);
        action.putAttribute(newName, (AttributeValue)attributes.get(i));
        removeAttribute(oldName);
        return true;
    }

    /**
     * Changes the value of a given attribute.
     * @param i the index of the attribute
     * @param newVal the new value
     */
    public void changeAttributeVal(int i, AttributeValue newVal) {
        String s = attributeNames.get(i).toString();
        attributes.remove(i);
        attributes.add(i, newVal);
        removeAttribute(s);
        action.putAttribute(s, newVal);
    }

    /**
     * Removes the attribute on the given name
     * from the represented notification.
     * @param target the attr name to remove
     * @return false if the name could not be found
     */
    private boolean removeAttribute(String target) {
        boolean b = false;
        Notification newAction = new Notification();
        for (Iterator iter = action.attributeNamesIterator(); iter.hasNext(); ) {
            String s = iter.next().toString();
            if (s.equals(target)) b = true;
            else newAction.putAttribute(s, action.getAttribute(s));
        }
        action = newAction;
        return b;
    }

    /**
     * Removes the attribute at the given index, from both
     * this representation, and the notification.
     * @param index the index of the attribute
     * @return false if the name could not be found
     */
    public boolean removeAttribute(int index) {
        String attName = attributeNames.remove(index).toString();
        attributes.remove(index);
        return removeAttribute(attName);
    }

    /**
     * Adds an attribute to the represented notification.
     * @param n the name
     * @param val the value
     * @return false if the name could not be added due to a naming conflict
     */
    public boolean addAttribute(String n, AttributeValue val) {
        if (action.getAttribute(n) != null) return false;
        attributes.add(val);
        attributeNames.add(n);
        action.putAttribute(n, val);
        return true;
    }

    /**
     * Rename this action. Incomplete...
     * @param newName the new name for this action
     * @return false if the name was not valid
     */
    public boolean setName(String newName) {
        // boolean b = parent.getSpecification().changeActionName(name, newName);
        // should do the following:
        // 1) check that no other action in the specification has this name
        // 2) change the hash name for the action in the state machine spec
        // 3) change reference name for all the states that call this action

        // change name locally
        //if (b) name = newName;
        return //b;
        false;
    }

    /** @param the string representation of this object */
    public String toString() { return name; }

    /********************************************************
        methods inherited from the TreeNode interface
    ********************************************************/

    public Enumeration children() { return null; }
    public boolean getAllowsChildren() { return false; }
    public TreeNode getChildAt(int childIndex) { return null; }
    public int getChildCount() { return 0; }
    public int getIndex(TreeNode node) { return -1; }
    public TreeNode getParent() { return parent; }
    public boolean isLeaf() { return true; }
}