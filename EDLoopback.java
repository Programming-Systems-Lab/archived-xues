
package psl.xues;

import siena.*;

/**
 * Loopback notifiable class to send events back to the main siena.
 */
public class EDLoopback implements Notifiable {
  Siena mySiena;

  public EDLoopback(Siena s) {
    this.mySiena = s;
    // Now subscribe
    Filter f = new Filter();
    f.addConstraint("loopback",new AttributeConstraint(Op.EQ,(int)0));
    try {
      s.subscribe(f,this);
    } catch(SienaException e) { e.printStackTrace(); }
  }

  public void notify(Notification n) {
    if(EventDistiller.DEBUG)
      System.err.println("EDLoopback: Received " + n);
    // Add loopback field and send it out
    n.putAttribute("loopback",(int)1);
    try {
      if(EventDistiller.DEBUG)
	System.err.println("EDLoopback: Publishing " + n);
      mySiena.publish(n);

      /* TESTING CRAP
	 Notification o = new Notification();
	 o.putAttribute("foobarbar","foovalval");
	 mySiena.publish(o);*/
    } catch(Exception e) { e.printStackTrace(); }
  }

  public void notify(Notification[] n) { ; }
}
