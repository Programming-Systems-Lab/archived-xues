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

    /** Error message type. */
    public static final int ERROR_MESSAGE = 0;

    /** Debug/status message type. */
    public static final int DEBUG_MESSAGE = 1;

    /**
     * Prints message to output.
     * @param message the message to print
     * @param source the thread where the message originates
     * @param type type of message
     */
    void print(String message, Object source, int type);
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
     * @param source the threac where the message originates
     * @param type type of message
     */
    public void print(String message, Object source, int type) {
	if (debug || type == EDErrorManager.ERROR_MESSAGE) 
	    System.out.println(message);
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
    }

    /**
     * Prints message to output.
     * @param message the message to print
     * @param source the threac where the message originates
     * @param type type of message
     */
    public void print(String message, Object source, int type) {
	if (!debug && type == EDErrorManager.DEBUG_MESSAGE) return;
	textArea.append(message);
    }
}
