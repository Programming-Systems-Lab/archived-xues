package psl.xues;

/**
 * Simple example of an TriKX Update Object.
 *
 * @author Janak J Parekh
 * @version 0.01 (9/8/00)
 *
 * $Log$
 * Revision 1.1  2000-09-08 19:08:50  jjp32
 *
 * Added TriKXUpdateObject for Kanan, as a target for serialized objects.
 *
 */
public class TriKXUpdateObject implements java.io.Serializable {
  String nodeName;
  java.awt.Color whichColor;

  public TriKXUpdateObject(String nodeName, java.awt.Color whichColor) {
    this.nodeName = nodeName;
    this.whichColor = whichColor;
  }

  public String getNodename() { return this.nodeName; }
  public java.awt.Color getColor() { return whichColor; }
}
