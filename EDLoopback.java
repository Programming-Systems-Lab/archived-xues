
package psl.xues;

import siena.*;

public class EDLoopback implements Notifiable {
  Siena mySiena;

  public EDLoopback(Siena s) {
    this.mySiena = s;
    // Now subscribe
    Filter f = new Filter();
    f.addConstraint("loopback",new AttributeConstraint(Op.NE,(int)1));
    try {
      s.subscribe(f,this);
    } catch(SienaException e) { e.printStackTrace(); }
  }

  public void notify(Notification n) {
    if(EventDistiller.DEBUG)
      System.err.println("EDLoopback: Received " + n);
    // Add loopback field and send it out
    Notification o = new Notification(n);
    o.putAttribute("loopback",(int)1);
    try {
      mySiena.publish(o);
    } catch(Exception e) { e.printStackTrace(); }
  }

  public void notify(Notification[] n) { ; }
}
