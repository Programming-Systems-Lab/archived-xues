package psl.xues.ep.transform;

import siena.*;

/**
 * Time synchronizer module
 * @author  jjp32
 */
public class TimeSynchronizer implements Notifiable {
  public static String sourceAttribute = "SourceIP";
  
  private String sienaMaster;
  private HierarchicalDispatcher siena;
  private Hashtable sourceSkew;
  
  /**
   * CTOR.
   */
  public TimeSynchronizer() {
    siena = new HierarchicalDispatcher();
    siena.setReceiver(new TCPPacketReceiver(0));
    siena.setMaster(sienaMaster);
    
    Runtime.getRuntime().addShutdownHook(new Thread() {
      public void run() {
        siena.shutdown();
      }
    });
    
    sourceSkew = new Hashtable();
    Filter f = new Filter();
    f.addConstraint("Type", "EPInput");
    // XXX - need to test this
    f.addConstraint(sourceAttribute, new AttributeConstraint(Op.ANY, ""));
    f.addConstraint(EDConst.TIME_ATT_NAME, new AttributeConstraint(Op.ANY, ""));
    siena.subscribe(f, this);
  }
  
  /**
   * Notify method.
   */
  public void notify(Notification n) {
    long currentTime = System.currentTimeMillis();
    String src = n.getAttribute(sourceAttribute);
    
    if(src == null || src.length() == 0 || src.equalsIgnoreCase("null")) 
      return; // Nothing to do
    
    // Build the skew as an average
    Averager a = sourceSkew.get(src);
    long t = (long)n.getAttribute(EDConst.TIME_ATT_NAME);
    if(a == null) {
      sourceSkew.put(src, new Averager(currentTime - t));
      n.putAttribute(EDConst.TIME_ATT_NAME, currentTime);
    } else {
      t += a.updateSkew(currentTime - t);
      n.putAttribute(EDConst.TIME_ATT_NAME, t);
    }

    // Publish n
    try {
      siena.publish(n);
    } catch(SienaException se) { /* XXX */ }
  }
  
  /**
   * Main method.
   */
  public static void main(String[] args) {
    new TimeSynchronizer();
    
  }
  
  public void notify(siena.Notification[] notification)
  throws siena.SienaException { ; }
}

class Averager {
  private int n = 0;
  private long sum = 0;
  private long avgskew = 0;
  
  private int updateSkew(int latestVal) {
    sum += latestVal;
    avgskew = sum / ++n;
    return avgskew;
  }
}