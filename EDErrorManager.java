/*
 * File contains the EDErrorManager interface, and its implementors.
 * @author enrico buonanno (eb659)
 */

package psl.xues;

import java.awt.*;
import javax.swing.*;

/**
 * Classes that handle output of the ED must implement this interface.
 */
interface EDErrorManager {

    /* types of messages, by source
     *
     * NOTE: only ERROR type messages will be printed
     * if debug is set to false 
     */
    public static final int ERROR = -1;
    public static final int REAPER = 0;
    public static final int MANAGER = 1;
    public static final int STATE = 2;
    public static final int DISPATCHER = 3;

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
	if (!debug && type >= 0) return;
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
    private JTextArea textArea = new JTextArea();

    /** 
     * Constructs a new EDErrorGUI.
     * @param whether debug messages should be printed
     */
    public EDErrorGUI(boolean debug) { 
	this.debug = debug; 

	setTitle("Event Distiller output");
	setSize(500, 500);
	JScrollPane scrollPane = new JScrollPane(textArea);
	getContentPane().add(scrollPane, BorderLayout.CENTER);
	show();
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
	if (!debug && type >= 0) return;
	textArea.append(message);
    }
}
