package psl.xues.notifiers;

import siena.*;
import psl.xues.*;
import psl.sendmail.*;
import psl.worklets2.wvm.*;
import psl.worklets2.worklets.*;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * Worklet Event Notifier
 *
 * Sorry for sparse comments, this version is being written at 2:09 AM.
 *
 * TODO: Integrate with EventNotifier class.  Too late to do this now.
 * 
 * Copyright (c) 2000: The Trustees of Columbia University and the
 * City of New York.  All Rights Reserved.
 *
 * @author Janak J Parekh and Gaurav S. Kc
 * @version 0.9
 *
 * $Log$
 * Revision 1.4  2001-02-08 23:11:35  gskc
 * Not much
 *
 * VS: ----------------------------------------------------------------------
 *
 * Revision 1.3  2001/02/07 01:47:43  gskc
 * Something that was hardcoded _even_ for Monterey.
 *
 * Revision 1.2  2001/02/07 01:45:25  gskc
 * Changes made in Monterey.
 *
 * Revision 1.1  2001/01/30 07:18:32  jjp32
 *
 * Last commit?
 *
 *
 */
public class WorkletEventNotifier implements Notifiable {
  protected static String sienaHost = null;
  protected Siena sienaRef = null;
  private WVM wvmRef = null;

  static boolean DEBUG = false;

  public static void main(String[] args) {
    if(args.length < 1) { // Siena host specified?
      usage();
    }

    for(int i=0; i < args.length; i++) {
      if(args[i].equals("-s"))
	sienaHost = args[++i];
      else if(args[i].equals("-d"))
	DEBUG = true;
      else
	usage();
    }

    new WorkletEventNotifier();
  }

  /**
   * Print usage.
   */
  public static void usage() {
    System.out.println("usage: java WorkletEventNotifier [-s sienaHost] [-d] [-?]");
    System.exit(-1);
  }


  public WorkletEventNotifier() { 
    // Create a siena
    sienaRef = new HierarchicalDispatcher();
    try {
      ((HierarchicalDispatcher)sienaRef).
	setReceiver(new TCPPacketReceiver(5557));
      ((HierarchicalDispatcher)sienaRef).setMaster(sienaHost);
    } catch(Exception e) { e.printStackTrace(); }

    // Subscribe to events


    // Create WVM
    // wvmRef = new WVM(this, InetAddress.getLocalHost().getHostName(), "foo");
    wvmRef = new WVM(this, "127.0.0.1", "WorkletEventNotifier");

  }

  public void notify(Notification n) {

    String emailAdd_to_reject = "";

    Worklet wkl = new Worklet(null);
    System.out.println("Creating CERWJ");
    String confFileIn = "config.txt";
    // write out the confFileIn
    try {
      Writer out = new FileWriter(confFileIn);
      out.write("# END_OF /etc/mail/access\t" +
                "# END_OF /etc/mail/access\t" +
                "# END_OF /etc/mail/access\t" +
                "# END_OF /etc/mail/access\t" + 
                "# THE FOLLOWING HAS BEEN INSERTED BY A WORKLET ... worklet wuz 'ere\n" +
                emailAdd_to_reject + "        REJECT\n\n" + 
                "# END_OF /etc/mail/access\n");
      out.close();
    } catch (IOException e) {
      System.out.println("Error trying to write to config file: " + confFileIn);
      e.printStackTrace();
    }
    
    // This hardcoded bit has to be read in from a config file
    CERWJ wj = new CERWJ(confFileIn, "/etc/mail/access", "10.0.1.2", "sendmailMonitor");
    System.out.println("Adding CERWJ to WKL");
    wkl.addJunction(wj);
    System.out.println("Deploying WKL w/ CERWJ");
    wkl.deployWorklet(wvmRef);
  }

  public void notify(Notification[] n) {
    ;
  }
    
}
