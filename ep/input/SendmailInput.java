package psl.xues.ep.input;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Hashtable;

import org.w3c.dom.Element;

import psl.xues.ep.event.MailEvent;

/**
 * SMTP input filter. 
 * <p>Usage: <em>([] implies an optional parameter)</em></p>
 * (in the inputters section of the configuration file:)<br>
 * <code> &ltInputter Name="SendmailInput1" Type="psl.xues.ep.input.
 * SendmailInput" [ListenPort="<i>port number</i>"] [EndOfMailMarker="
 * <i>marker</i>"] [MailFieldSeparator="<i>delimiter</i>"]/&gt
 * </code>
 * <ul>
 * <li>ListenPort: a number indicating the port number where the Inputter will
 * listen
 * <li>EndOfMailMarker: The string (one or more characters) that will indicate
 * the end of data in the socket.  The entire value of this field is one
 * delimiter.
 * <li>MailFieldSeparator: The string (one or more characters) that will
 * indicate break between fields of data.  The entire value of this field is
 * one delimiter.
 * </ul>
 * <p>
 * (in the rules section of the configuration file:) <br>
 * <code> &ltRule Name=" SendmailRule"&gt<br> &nbsp;&nbsp;&nbsp;&nbsp;
 * &ltInputs&gt<br> &nbsp;&nbsp; &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&ltInput
 * Name=" SendmailInput1" /&gt <br> &nbsp;&nbsp;&nbsp;&nbsp; &lt/Inputs&gt <br>
 * &nbsp;&nbsp;&nbsp;&nbsp;  &ltOutputs&gt <br> &nbsp; &nbsp;&nbsp;&nbsp;&nbsp;
 * &nbsp;&nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp; &ltOutput Name=" <i>outputter
 * name</i>" /&gt <br> &nbsp;&nbsp;&nbsp;&nbsp; &lt/Outputs&gt<br> &lt/Rule&gt
 * </code>
 * <p>
 * Copyright (c) 2002: The Trustees of Columbia University in the
 * City of New York.  All Rights Reserved.
 *
 * <!--
 * TODO:
 - Maybe separate out the string handling functions into a separate helper
 class?  -- >
 * 
 * @author Julia Cheng <jc424@columbia.edu>
 * @version $Revision$
 */
public class SendmailInput extends EPInput {
	/** The socket that will receive data
	 */
	ServerSocket serverSocket = null;
	/** Indicates location of the socket. The default value is 4444.
	 */
	int serverPort = 4444;
	/** String (one or more character) to indicate end of data in the socket. The 
	 * default value is ".".   
	 */
  	String inputEndMarker = ".";
  	/** The default value is ";;".
  	 */
	String inputFieldSeparator = ";;";

  /**
   * Constructor.
   */
  public SendmailInput(EPInputInterface ep, Element el) 
    throws InstantiationException {
    super(ep,el);
    
    String inputMarker = el.getAttribute("EndOfMailMarker");
    if(isStringValid(inputMarker))
    {
    	inputEndMarker = inputMarker;
    }
    
    String fieldSeparator = el.getAttribute("MailFieldsSeparator");
    if(isStringValid(fieldSeparator)) 
    {
    	inputFieldSeparator = fieldSeparator;
    }
    
	String listenPort = el.getAttribute("ListenPort");
    if(isStringValid(listenPort)) 
    {
		try {
			serverPort = Integer.parseInt(listenPort);
		} catch (Exception e) {
			throw new InstantiationException("Invalid listen port specified");
			
		}
    }
    
	try {
		 serverSocket = new ServerSocket(serverPort);
	} catch (Exception e) {
		throw new InstantiationException("Could not socket on port " + serverPort);
	}
  }

/**
 * A simple function to test is a String object contains valid and useful data.
 * @param str - the string to be tested
 * @return true if the string has valid data, false if the string is empty,
 * null, or contains only white space.
 */
	private boolean isStringValid(String str)
	{
		return ((str != null) && (str.trim().length() > 0));
	}
	
  /**
   * Run method
   * <ol> 
   *  <li>Receives a string message from an external source via socket 
   *  <li>Parse  the string message into fields (This is bulk of the processing
   * in this function)
   *  <li>Create a mailEvent based on the string data
   *  <li>Inject the mailEvent into EventPackager
   * </ol>
   */
  public void run() {
	Socket clientSocket = null;
	StringBuffer sbInput = new StringBuffer();
	String nameAndValueSeparator = "::";
	String headerListSeparator = "##";
    while(!shutdown) {
		try {
			clientSocket = serverSocket.accept();
		} catch (Exception e) {
			if(!shutdown) {
				debug.error("Could not accept connection, shutting down inputter", e);
				shutdown();
			}
			return;
	  	}
    
    	// Read data from the socket until the EndOfMailMarker is reached. 
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			String sInput;
			sbInput = new StringBuffer();
			while((sInput = in.readLine()) != null)
			{
				if(sInput.trim().equals(inputEndMarker)) {
					break;
				}
				sbInput.append(sInput);
				sbInput.append("\n");
			}
		} catch (Exception e) {
			debug.error("Could not read from connection", e);
		}
			
		try {
			clientSocket.close();
		} catch (IOException e) {
			debug.error("Could not close client socket", e);
		}
			
		// Parse out the fields of the message
		String mailInput = sbInput.toString();
			
		/* Can't just use a stringTokenizer to parse, because it will fail with a delimiter longer than 1 char
		 * Instead, two numbers to keep track of the positions of delimitors,
		 * and capture the substring between delimiters. 
		*/ 
		int index1 = 0;
		int index2 = mailInput.indexOf(inputFieldSeparator);
		Hashtable headersList = new Hashtable();
		MailEvent me = new MailEvent(getName());
		while((index1 > -1) || (index2 > -1)) 
		{
			String field = null;
			if(index2 > -1)
			{
				field = mailInput.substring(index1, index2);
			} else {
				field = mailInput.substring(index1);
			}
				
			String fieldName = null;
			String fieldValue = null;
			int index = field.indexOf(nameAndValueSeparator);
			if(index > -1)
			{
				fieldName = field.substring(0, index).trim().toUpperCase();
				fieldValue = field.substring(index+2).trim();
			}
				
			if(fieldName.equals("FROM")) {
				me.setFrom(fieldValue);
			} else if(fieldName.equals("TO")) {
				me.setTo(fieldValue);
			} else if(fieldName.equals("CC")) {
				me.setCc(fieldValue);
			} else if(fieldName.equals("BCC")) {
				me.setBcc(fieldValue);
			} else if(fieldName.equals("DATE")) {
				me.setDate(fieldValue);
			} else if(fieldName.equals("SUBJECT")) {
				headersList.put("Subject", fieldValue);
				me.setAdditionalHeaders(headersList);
			} else if(fieldName.equals("HEADERS")) {
				// Parse out the additional headers into a hashtable
				// Again, cannot use StringTokenizer in case the separator is more than 1 char long
				int headersIndex1 = 0;
				int headersIndex2 = fieldValue.indexOf(headerListSeparator);
				while((headersIndex1 > -1) || (headersIndex2 > -1))
				{
					String header = null;
					if(headersIndex2 > -1)
					{
						header = fieldValue.substring(headersIndex1, headersIndex2);
					} else {
						header = fieldValue.substring(headersIndex1);
					}
					
					int separator_index = header.indexOf(":");
					if(separator_index > -1)
					{
						headersList.put(header.substring(0, header.indexOf(":")),
										header.substring(header.indexOf(":") + 1));
					}
					
					if(headersIndex2 != -1)
					{
						headersIndex1 = headersIndex2 + headerListSeparator.length();
						headersIndex2 = fieldValue.indexOf(headerListSeparator, headersIndex1);
					} else {
						headersIndex1 = -1;
					}
				}
				me.setAdditionalHeaders(headersList);
			} else if(fieldName.equals("DATA")){
				me.setData(fieldValue);
			}
			// Get ready for the next loop iteration
			if(index2 == -1) 
			{
				index1 = -1;
			} else {
				index1 = index2 + inputFieldSeparator.length();
				index2 = mailInput.indexOf(inputFieldSeparator, index1);
			}
		}
	    // Publish to EP
	    ep.injectEvent(me);
    }
  }
  
  public void shutdown() {
  		super.shutdown();
  		// Close the server socket
  		try {
  			if(serverSocket != null)
  			{
				serverSocket.close();
  			}
		} catch (IOException e) {
			debug.error("Could not close server socket", e);
		}
  }

  /**
   * Get type of input.
   */
  public String getType() {
    return "SendmailInput";
  }
}
