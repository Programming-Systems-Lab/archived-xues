package psl.xues.ep.event;

import java.util.Enumeration;
import java.util.Hashtable;

/**
 * Mail event representation.  Embeds strings representing a mail message.
 * <p>
 * Copyright (c) 2002: The Trustees of Columbia University in the
 * City of New York.  All Rights Reserved.
 *
 * <!--
 * TODO: not sure...
 *  -->
 *
 * @author Julia Cheng <jc424@columbia.edu>
 * @version $Revision$
 */
public class MailEvent extends EPEvent {
	/** Format for this event. */
	private final String format = "MailEvent";
	
	private String from = null;
	private String to = null;
	private String cc = null;
	private String bcc = null;
	private String date = null;
	
	/** Additional header fields that may appear in the message, for example, subject or message ID.  
	 */
	private Hashtable additionalHeaders = null;
	
	/** Associated data: the actual message part of the mail. */
	private String data = null;
	  
	/**
	 * Base CTOR.
	 * 
	 * @param source The generator ("source") of these events.
	 */
	public MailEvent(String source) { this(source, null, null, null, null, null, null, null); }

	public MailEvent(String source, String from, 
						String to, String cc, String bcc, String date,
						Hashtable additionalHeaders, String data) 
	{
		super(source);
		this.from = from;
		this.to = to;
		this.bcc = bcc;
		this.date = date;
		this.additionalHeaders = additionalHeaders;
		this.data = data;
	}

	/**
	 * String representation of MailEvent
	 *
	 * @return The string representation
	 */
	public String toString()
	{
		StringBuffer sb = new StringBuffer();
		if(date != null)
		{
			sb.append("Date: ");
			sb.append(date);
		}
		
		if(from != null)
		{
			sb.append("\nFrom: "); 
			sb.append(from);
		}
		
		if(to != null)
		{
			sb.append("\nTo:");
			sb.append(to);
		}
		
		if(cc != null) 
		{
			sb.append("\nCc:");
			sb.append(cc);
		} 
		
		if(bcc != null)
		{
			sb.append("\nBcc:");
			sb.append(bcc);
		}
		
		if(additionalHeaders != null)
		{
			Enumeration e = additionalHeaders.keys();
			while(e.hasMoreElements())
			{
				String headerName = (String)e.nextElement();
				String headerVal = (String)additionalHeaders.get(headerName);
				sb.append("\n");
				sb.append(headerName);
				sb.append(": ");
				sb.append(headerVal);
			}
		}
		
		if(data != null)
		{
			sb.append("\n");
			sb.append(data);
		}
		return sb.toString();
	}
	
	public String getFormat()
	{
		return format;
	}
	/**
	 * Returns the additionalHeaders.
	 * @return Hashtable
	 */
	public Hashtable getAdditionalHeaders() {
		return additionalHeaders;
	}

	/**
	 * Returns the bcc.
	 * @return String
	 */
	public String getBcc() {
		return bcc;
	}

	/**
	 * Returns the cc.
	 * @return String
	 */
	public String getCc() {
		return cc;
	}

	/**
	 * Returns the data.
	 * @return String
	 */
	public String getData() {
		return data;
	}

	/**
	 * Returns the date.
	 * @return String
	 */
	public String getDate() {
		return date;
	}

	/**
	 * Returns the from.
	 * @return String
	 */
	public String getFrom() {
		return from;
	}

	/**
	 * Returns the to.
	 * @return String
	 */
	public String getTo() {
		return to;
	}

	/**
	 * Sets the additionalHeaders.
	 * @param additionalHeaders The additionalHeaders to set
	 */
	public void setAdditionalHeaders(Hashtable additionalHeaders) {
		this.additionalHeaders = additionalHeaders;
	}

	/**
	 * Sets the bcc.
	 * @param bcc The bcc to set
	 */
	public void setBcc(String bcc) {
		this.bcc = bcc;
	}

	/**
	 * Sets the cc.
	 * @param cc The cc to set
	 */
	public void setCc(String cc) {
		this.cc = cc;
	}

	/**
	 * Sets the data.
	 * @param data The data to set
	 */
	public void setData(String data) {
		this.data = data;
	}

	/**
	 * Sets the date.
	 * @param date The date to set
	 */
	public void setDate(String date) {
		this.date = date;
	}

	/**
	 * Sets the from.
	 * @param from The from to set
	 */
	public void setFrom(String from) {
		this.from = from;
	}

	/**
	 * Sets the to.
	 * @param to The to to set
	 */
	public void setTo(String to) {
		this.to = to;
	}

}
