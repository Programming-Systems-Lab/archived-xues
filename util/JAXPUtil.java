package psl.xues.util;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.apache.log4j.Logger;
import org.omg.CORBA.portable.InputStream;
import org.w3c.dom.Element;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;

/**
 * Various JAXP utils.
 *
 * @author Janak J Parekh
 * @version 1.0
 */
public abstract class JAXPUtil {
  private static DocumentBuilderFactory dbf =
  DocumentBuilderFactory.newInstance();
  private static TransformerFactory tf = TransformerFactory.newInstance();
  private static Logger debug = Logger.getLogger(JAXPUtil.class.getName());
  
  /**
   * A DocumentBuilder for use within JAXPUtil ONLY.  DocumentBuilders are not
   * guaranteed to support multiple requests in separate threads, so we
   * must synchronize all calls to this DocumentBuilder internally.  It's
   * easiest to do this via synchronized methods (or synchronizing on the
   * JAXPUtil "object").
   */
  private static DocumentBuilder mydb = null;
  
  static {
    try {
      mydb = dbf.newDocumentBuilder();
    } catch(Exception e) {
      debug.error("Could not create JAXP DocumentBuilder", e);
      mydb = null;
    }
  }
  
  /**
   * Get a new XML builder.
   *
   * @return A new DocumentBuilder instance
   */
  public static DocumentBuilder newDocumentBuilder() {
    try {
      return dbf.newDocumentBuilder();
    } catch(Exception e) {
      debug.error("Could not create JAXP DocumentBuilder", e);
      return null;
    }
  }
  
  /**
   * Create a blank XML document.
   */
  public synchronized static Document newDocument() {
    if(mydb != null) {
      return mydb.newDocument();
    } else return null;
  }
  
  /**
   * Parse an XML document.
   */
  public synchronized static Document parse(InputStream is) {
    if(mydb != null) try {
      return mydb.parse(is);
    } catch(Exception e) { return null; }
    else return null;
  }
  
  /**
   * Create a XML document with a root element.
   *
   * @param r The first element of this document.
   * @return The document.
   */
  public static Document newDocument(Element r) {
    Document d = newDocument();
    if(d == null) return null;
    
    d.appendChild(r);
    return d;
  }

  /**
   * Get a new Transformer instance.
   *
   * @return A transformer if possible, else null.
   */
  public static Transformer newTransformer() {
    try {
      return tf.newTransformer();
    } catch(Exception e) {
      debug.error("Could not create XSLT Transform");
      return null;
    }
  }
}
