package psl.xues.ep.util;

import javax.xml.parsers.DocumentBuilderFactory;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.InputStream;
import org.w3c.dom.Document;
import java.io.PrintWriter;

/**
 * Some XML test code.
 *
 * @author Janak J Parekh
 * @version $Revision$
 */
public class XMLTest {
  public static final int PORT = 9999;
  
  public static void main(String[] args) {
    new XMLTest();
  }
  
  public XMLTest() {
    new Thread(new Inputter()).start();
    try {
      Socket s = new Socket("localhost", PORT);
      PrintWriter pw = new PrintWriter(s.getOutputStream(), true);
      pw.println("<testmsg><a></a></testmsg>");
      pw.println("<testmsg><a></a></testmsg>");
      Thread.currentThread().sleep(5000);
      s.close();
    } catch(Exception e) { e.printStackTrace(); }
  }
  
  class Inputter implements Runnable {
    public void run() {
      ServerSocket ss = null;
      try {
        ss = new ServerSocket(PORT);
      } catch(Exception e) { e.printStackTrace(); }
      
      while(true) {
        Socket cs = null;
        InputStream is = null;
        try {
          cs = ss.accept();
          is = cs.getInputStream();
        } catch(Exception e) { e.printStackTrace(); }
        while(true) try {
          System.err.println("Beginning parse");
          Document doc = DocumentBuilderFactory.newInstance().
          newDocumentBuilder().parse(is);
          
          System.err.println("Got document \"" + doc + "\"");
        } catch(Exception e) {
          continue;
        }
      }
    }
  }
}
