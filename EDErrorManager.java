/*
 * File contains the EDErrorManager interface, and its implementors.
 * @author enrico buonanno (eb659)
 */

package psl.xues;

import java.awt.*;
import javax.swing.*;
import javax.swing.text.*;

/**
 * Classes that handle output of the ED must implement this interface.
 */
interface EDErrorManager {

    /* types of messages, by source
     *
     * NOTE: only ERROR type messages will be printed
     * if debug is set to false 
     */
    public static final int ERROR = 0;
    public static final int MANAGER = 1;
    public static final int STATE = 2;
    public static final int DISPATCHER = 3;
    public static final int REAPER = 4;

    /**
     * Prints message to output.
     * @param message the message to print
     * @param type type of message
     */
    void print(String message, int type);

    /**
     * Prints message to output.
     * @param message the message to print
     * @param type type of message
     */
    void println(String message, int type);
}


/**
 * Text-based console to handle ED error messages.
 */
class EDErrorConsole implements EDErrorManager {

    /** Whether debug messages are printed. */
    private boolean debug = false;
    
    /** 
     * Constructs a new EDErrorConsole.
     * @param whether debug messages should be printed
     */
    public EDErrorConsole(boolean debug) { this.debug = debug; }

    /**
     * Prints message to output.
     * @param message the message to print
     * @param type type of message
     */
    public void println(String message, int type) {
	print(message + "\n", type); }

    /**
     * Prints message to output.
     * @param message the message to print
     * @param type type of message
     */
    public void print(String message, int type) {
	if (!debug && type > 0) return;
	System.out.print(message);
    }
}


/**
 * GUI to handle ED error messages.
 */
class EDErrorGUI extends JFrame implements EDErrorManager {

    /** Whether debug messages are printed. */
    private boolean debug = false;
    
    /** The text pane to which we print. */
    private JTextPane textPane = new JTextPane();

    /** The scroll pane for the text area. */
    private JScrollPane scrollPane;

    /** Names for the styles we use for different outputs. */
    static final String[] STYLE_NAMES = {"error", "manager", "state", "dispatcher", "reaper"};

    /** Colors for the styles. */
    static final Color[] COLORS = {Color.red, Color.magenta, Color.blue, Color.darkGray, Color.lightGray };

    /** 
     * Constructs a new EDErrorGUI.
     * @param whether debug messages should be printed
     */
    public EDErrorGUI(boolean debug) { 
	this.debug = debug; 

	setTitle("Event Distiller output");
	setIconImage(null);
	setSize(550, 1000);
	textPane.setEditable(false);
	scrollPane = new JScrollPane(textPane);
	getContentPane().add(scrollPane, BorderLayout.CENTER);
	addStyles();
	show();
    }

    /** Adds the styles we need for the output. */
    private void addStyles() {
	Style def = StyleContext.getDefaultStyleContext().getStyle(StyleContext.DEFAULT_STYLE); 
        Style s = null;
	for (int i = 0; i < STYLE_NAMES.length; i++) {
	    s = textPane.addStyle(STYLE_NAMES[i], def);
	    StyleConstants.setForeground(s, COLORS[i]);
	}
    }

    /**
     * Prints message to output.
     * @param message the message to print
     * @param type type of message
     */
    public void println(String message, int type) {
	print(message + "\n", type); }

    /**
     * Prints message to output.
     * @param message the message to print
     * @param type type of message
     */
    public void print(String message, int type) {
	if (!debug && type > 0) return;
	Document document = textPane.getDocument();

	try { document.insertString(document.getLength(), message, textPane.getStyle(STYLE_NAMES[type])); }
	catch (Exception ex) { ; }
	//	textArea.append(message);
	
	//scroll down
	JScrollBar vert = scrollPane.getVerticalScrollBar();
	vert.setValue(vert.getMaximum());
    }
}
