package psl.xues.notifiers;

import siena.*;
import psl.xues.*;
import psl.worklets2.wvm.*;
import psl.worklets2.worklets.*;

import java.io.*;
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
 * @author Janak J Parekh and Gaurav S Kc
 * @version 0.9
 *
 * $Log$
 * Revision 1.1  2001-01-30 07:18:32  jjp32
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
	setReceiver(new TCPPacketReceiver(61990));
      ((HierarchicalDispatcher)sienaRef).setMaster(sienaHost);
    } catch(Exception e) { e.printStackTrace(); }

    // Subscribe to events


    // Create WVM
    wvmRef = new WVM(this, "localhost", "foo");

  }

  public void notify(Notification n) {
    Worklet wkl = new Worklet(null);
    /*
      CERWJ wj = new CERWJ(confFile, rHost, rName);
    */
  }

  public void notify(Notification[] n) {
    ;
  }
    
}
