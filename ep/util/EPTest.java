package psl.xues.ep.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import psl.xues.ep.EventPackager;
import psl.xues.ep.util.Base64;
import siena.HierarchicalDispatcher;
import siena.Filter;
import siena.Notifiable;
import siena.Notification;

/**
 * Event packager testing framework.
 * <p>
 * Copyright (c) 2002: The Trustees of Columbia University in the
 * City of New York.  All Rights Reserved.
 *
 * @author Janak J Parekh
 * @version $Revision
 */
public class EPTest {
  public HierarchicalDispatcher siena1;
  public HierarchicalDispatcher siena2;
  
  public EPTest(String host1, String host2) {
    siena1 = new HierarchicalDispatcher();
    siena2 = new HierarchicalDispatcher();
    try {
      siena1.setMaster(host1);
      siena2.setMaster(host2);
    } catch(Exception e) {
      e.printStackTrace();
    }
    
    // Subscribe to all events on siena2
    Filter f = new Filter();
    try {
      siena2.subscribe(f, new Notifiable() {
        public void notify(Notification n) {
          System.out.println("--> RECEIVED: " + n);
        }
        public void notify(Notification[] n) { ; }
      });
    } catch(Exception e) { e.printStackTrace(); }
    
    // Now publish some events on siena1
    Notification n = new Notification();
    n.putAttribute("Name", "Janak");
    Notification n2 = new Notification();
    n2.putAttribute("Name", "Yuan");
    
    try {
      siena1.publish(n);
      siena1.publish(n2);
    } catch(Exception e) { e.printStackTrace(); }
    
    System.out.println("Done!");
  }
  
  public static void main(String[] args) {
    Notification n = new Notification();
    byte[] ba = null;
    n.putAttribute("foo", "bar");
    try {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ObjectOutputStream oos = new ObjectOutputStream(baos);
      oos.writeObject(n);
      oos.close();
      ba = baos.toByteArray();
      baos.close();
      System.out.println("Length is " + ba.length + ", now reading back in");
      ByteArrayInputStream bais = new ByteArrayInputStream(ba);
      ObjectInputStream ois = new ObjectInputStream(bais);
      Notification n2 = (Notification)ois.readObject();
      System.out.println("Notification read, it's \"" + n2 + "\"");
    } catch(Exception e) { e.printStackTrace(); }
      //    System.out.println(Base64.encodeObject(n));
      //    System.out.println(Base64.encodeObject(n).length());
      //new EPTest(args[0], args[1]);
  }
}