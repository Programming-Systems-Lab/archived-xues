package psl.xues.en;

import siena.*;
import java.io.*;
import java.util.*;

/**
 * EventNotifier for Xues.
 *
 * Sorry for sparse comments, this version is being written at 2:09 AM.
 *
 * Copyright (c) 2000: The Trustees of Columbia University and the
 * City of New York.  All Rights Reserved.
 *
 * @author Janak J Parekh
 * @version $Revision$
 */
public class EventNotifier implements Notifiable {
  protected static String sienaHost = null;
  protected Siena sienaRef = null;

  public static boolean DEBUG = false;

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

    new EventNotifier();
  }

  /**
   * Print usage.
   */
  public static void usage() {
    System.out.println("usage: java EventNotifier [-s sienaHost] [-d] [-?]");
    System.exit(-1);
  }


  public EventNotifier() { 
    // Nothing right now
  }

  public void notify(Notification n) {
    ;
  }

  public void notify(Notification[] n) {
    ;
  }
    
}
