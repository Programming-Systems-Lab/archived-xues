package psl.xues.ep.util;

import java.io.*;
import java.sql.*;

/**
 * Database tester, to test what data types are supported, etc.
 *
 * @author Janak J Parekh
 * @version $Revision$
 */
public class DBTest {
  private static Connection conn = null;
  
  public static void main(String[] args) {
    if(args.length != 4 && args.length != 5) {
      System.err.println("usage: java psl.xues.ep.util.DBTest "+
      "<dbdriver> <dbtype> <dbname> <username> [password]");
      System.exit(0);
    }
    
    try {
      Class.forName(args[0]);
      conn = DriverManager.getConnection("jdbc:" + args[1] + ":" +
      args[2],args[3],(args.length == 5 ? args[4] : ""));
    } catch(Exception e) {
      e.printStackTrace();
      System.exit(-1);
    }
    
    System.out.println("Connected to database.  Choose an option: ");
    System.out.println("1- List supported types");
    System.out.println("9- Exit");
    
    BufferedReader in = null;
    try {
      in = new BufferedReader(new InputStreamReader(System.in));
    } catch(Exception e) {
      e.printStackTrace();
      System.exit(-1);
    }
    
    boolean running = true;
    
    while(running) {
      System.out.print("> ");
      int command = -1;
      try {
        String input = in.readLine();
        command = Integer.parseInt(input);
      } catch(Exception e) {
        command = -1;
      }
      switch(command) {
        case 1:
          printTypes();
          break;
        case 9:
          running = false;
          break;
        default:
          System.out.println("Invalid choice, try again");
      }
    }
    
    System.out.println("Shutting down...");
    try {
      // Clean up
      conn.close();
    } catch(Exception e) {
      e.printStackTrace();
      System.exit(-1);
    }
  }
  
  private static void printTypes() {
    System.out.println("SUPPORTED TYPES:");
    
    try {
      ResultSet rs = conn.getMetaData().getTypeInfo();
      while(rs.next()) {
        System.out.println(rs.getString(1));
      }
      
    } catch(Exception e) {
      e.printStackTrace();
      System.exit(-1);
    }
  }
}