package psl.xues;

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
 * @version 0.9
 *
 * $Log$
 * Revision 1.1  2002-01-23 02:17:15  jjp32
 *
 * Another massive update - repackaging xues so I can release
 * EventDistiller separately (jar time coming up tomorrow :-))
 *
 * Revision 1.4  2001/01/30 07:18:42  jjp32
 *
 * Sienaified
 *
 * Revision 1.3  2000/12/26 22:25:13  jjp32
 *
 * Updating to latest preview versions
 *
 * Revision 1.2  2000/09/08 20:03:26  jjp32
 *
 * Finished network functionality in TriKXEventNotifier
 *
 * Revision 1.1  2000/09/07 23:15:25  jjp32
 *
 * Added EventNotifier code; updated previous event code
 *
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
