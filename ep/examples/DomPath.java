/*
 * @(#)DomEcho05.java	1.9 98/11/10
 *
 * Copyright (c) 1998 Sun Microsystems, Inc. All Rights Reserved.
 *
 * Sun grants you ("Licensee") a non-exclusive, royalty free, license to use,
 * modify and redistribute this software in source and binary code form,
 * provided that i) this copyright notice and license appear on all copies of
 * the software; and ii) Licensee does not utilize the software in a manner
 * which is disparaging to Sun.
 *
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES, INCLUDING ANY
 * IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN AND ITS LICENSORS SHALL NOT BE
 * LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING
 * OR DISTRIBUTING THE SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR ITS
 * LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT,
 * INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER
 * CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF THE USE OF
 * OR INABILITY TO USE SOFTWARE, EVEN IF SUN HAS BEEN ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGES.
 *
 * This software is not designed or intended for use in on-line control of
 * aircraft, air traffic, aircraft navigation or aircraft communications; or in
 * the design, construction, operation or maintenance of any nuclear
 * facility. Licensee represents and warrants that it will not use or
 * redistribute the Software for such purposes.
 */

import javax.xml.parsers.DocumentBuilder; 
import javax.xml.parsers.DocumentBuilderFactory;  
import javax.xml.parsers.FactoryConfigurationError;  
import javax.xml.parsers.ParserConfigurationException;
 
import org.xml.sax.SAXException;  
import org.xml.sax.SAXParseException;  

import java.io.File;
import java.io.IOException;

import org.w3c.dom.Document;
import org.w3c.dom.DOMException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.*;

// Basic GUI components
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;

// GUI components for right-hand side
import javax.swing.JSplitPane;
import javax.swing.JEditorPane;

// GUI support classes
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.WindowEvent;
import java.awt.event.WindowAdapter;

// For creating borders
import javax.swing.border.EmptyBorder;
import javax.swing.border.BevelBorder;
import javax.swing.border.CompoundBorder;

// For creating a TreeModel
import javax.swing.tree.*;
import javax.swing.event.*;
import java.util.*;
import java.util.regex.*;

public class DomPath  extends JPanel
{
    // Global value so it can be ref'd by the tree-adapter
    static Document document; 
    
    boolean compress = false;
    static final int windowHeight = 460;
    static final int leftWidth = 300;
    static final int rightWidth = 340;
    static final int windowWidth = leftWidth + rightWidth;
    static boolean debug = true;
    public DomPath()
    {
	// Make a nice border
	EmptyBorder eb = new EmptyBorder(5,5,5,5);
	BevelBorder bb = new BevelBorder(BevelBorder.LOWERED);
	CompoundBorder cb = new CompoundBorder(eb,bb);
	this.setBorder(new CompoundBorder(cb,eb));
	
       // Set up the tree
       JTree tree = new JTree(new DomToTreeModelAdapter());

       // Iterate over the tree and make nodes visible
       // (Otherwise, the tree shows up fully collapsed)
       //TreePath nodePath = ???;
       //  tree.expandPath(nodePath); 

       // Build left-side view
       JScrollPane treeView = new JScrollPane(tree);
       treeView.setPreferredSize(  
           new Dimension( leftWidth, windowHeight ));

       // Build right-side view
       // (must be final to be referenced in inner class)
       final 
       JEditorPane htmlPane = new JEditorPane("text/html","");
       htmlPane.setEditable(false);
       JScrollPane htmlView = new JScrollPane(htmlPane);
       htmlView.setPreferredSize( 
           new Dimension( rightWidth, windowHeight ));

       // Wire the two views together. Use a selection listener 
       // created with an anonymous inner-class adapter.
       tree.addTreeSelectionListener(
         new TreeSelectionListener() {
           public void valueChanged(TreeSelectionEvent e) {
             TreePath p = e.getNewLeadSelectionPath();
             if (p != null) {
               AdapterNode adpNode = 
                  (AdapterNode) p.getLastPathComponent();
               htmlPane.setText(adpNode.content());
             }
           }
         }
       );

       // Build split-pane view
       JSplitPane splitPane = 
          new JSplitPane( JSplitPane.HORIZONTAL_SPLIT,
                          treeView,
                          htmlView );
       splitPane.setContinuousLayout( true );
       splitPane.setDividerLocation( leftWidth );
       splitPane.setPreferredSize( 
            new Dimension( windowWidth + 10, windowHeight+10 ));

       // Add GUI components
       this.setLayout(new BorderLayout());
       this.add("Center", splitPane );
    } // constructor
    
    public static void main(String argv[]){
	String input = "";
	String regex = "";
	String path = "";
	String fileName = "";
	int argLength = argv.length;
	if (argLength < 4 ) {
	    usage();
	    System.exit(1);
	}
	else{
	    for(int i = 0; i < argLength; i++){
		if(argv[i].equals("-s")){
		    i++;
		    while(!argv[i].equals("-r") && !argv[i].equals("-f")){
			input= input + "" + argv[i++];
			if(debug) System.out.println("input: " + input);
		    }
		    i--;
		}
		else if(argv[i].equals("-f")){
		    fileName = argv[++i];
		    if(debug) System.out.println(" fileName: " +fileName);
		}
		else if(argv[i].equals("-r")){
		    regex = argv[++i];
		    //if(debug) System.out.println("regex: " + regex);
		}
		else if(argv[i].equals("-p")){
		    path = argv[++i];
		    //if(debug) System.out.println("path: " +path);
		}
	    }
	    
	
	    
	    buildDom( input, regex, path, fileName);
	    makeFrame();
	    return;
	}
	
	DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	//factory.setValidating(true);   
	//factory.setNamespaceAware(true);
	try {
	    DocumentBuilder builder = factory.newDocumentBuilder();
	    document = builder.parse( new File(argv[0]) );
	    makeFrame();
	    
	} catch (SAXException sxe) {
	    // Error generated during parsing)
	    Exception  x = sxe;
	    if (sxe.getException() != null)
		x = sxe.getException();
	    x.printStackTrace();
	    
	} catch (ParserConfigurationException pce) {
	    // Parser with specified options can't be built
	    pce.printStackTrace();
	    
	} catch (IOException ioe) {
	    // I/O error
	    ioe.printStackTrace();
	}
    } // main
    
    public static void makeFrame() {
        // Set up a GUI framework
        JFrame frame = new JFrame("DOM Echo");
        frame.addWindowListener(
          new WindowAdapter() {
            public void windowClosing(WindowEvent e) {System.exit(0);}
          }  
        );

        // Set up the tree, the views, and display it all
        final DomPath echoPanel = 
           new DomPath();
        frame.getContentPane().add("Center", echoPanel );
        frame.pack();
        Dimension screenSize = 
           Toolkit.getDefaultToolkit().getScreenSize();
        int w = windowWidth + 10;
        int h = windowHeight + 10;
        frame.setLocation(screenSize.width/3 - w/2, 
                          screenSize.height/2 - h/2);
        frame.setSize(w, h);
        frame.setVisible(true);
    } // makeFrame
    
    public static void buildDom(String input, String regex, String path, String fileName){
	String pattern = regex;
	String location = path;
	String attribute = "";
	try {
	    if(debug){
		System.out.println("building dom");
		System.out.println("input: " + input + " & regex: " + regex);
	    }
	    //build a new document factory
	    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	    DocumentBuilder builder = factory.newDocumentBuilder();	    
	    
	    if(input.equals("") ||( regex.equals("") && fileName.equals("")))
		usage();
	    
	    else if(!fileName.equals("")){
		//create file object, open it and parse its contents
		Document instructions = builder.parse(new File(fileName));
		document = builder.newDocument();	   
		NodeList patternList = instructions.getElementsByTagName("pattern");
		NodeList locationList = 
		    instructions.getElementsByTagName("location");
		NodeList attrList = instructions.getElementsByTagName("attrname");
		if(debug){
		    System.out.println("patternLength: " + patternList.getLength() +
				       "locationLength: " + 
				       locationList.getLength() +
				       "attrLength: " + attrList.getLength());
		}
		
		if((attrList.getLength()!= patternList.getLength()) && (attrList.getLength()!= locationList.getLength())){
		    System.err.println("Error: Invalid xml file. Tags missing.");
		    System.exit(1);
		    
		}
		else{
		    int ruleNo = locationList.getLength();
		    if(debug) System.out.println("ruleNo: " + ruleNo);
		    for(int i = 0; i< ruleNo ; i++){
			if(debug) System.out.println("i: " +i + "input " +input + " document " + document);
			pattern = (patternList.item(i)).getFirstChild().getNodeValue();
			if(debug) System.out.println("regex: " + pattern);
			location = locationList.item(i).getFirstChild().getNodeValue();
		        attribute = attrList.item(i).getFirstChild().getNodeValue();
			document = buildDom2(input, pattern, location, attribute, document);		
			System.out.println("document for");
		    }
		}
		
	    }//if file is given
	    //otherwise just send off the instructions to another method.:)
	    else{
		buildDom2(input, regex, path, "", document);
	    }
	}catch(ParserConfigurationException pc){
	    pc.printStackTrace();
	}catch(SAXException e){
	    System.err.println("SAXException: " + e);
	    e.printStackTrace();
	}catch(IOException e){
	    System.err.println("IOExcetpion: " + e);
	    e.printStackTrace();
	}
    }
    
    
    public static Document buildDom2(String input, String regex, String path, String attribute, Document document){
	//try{
	//start index of the substring matched by the java regex matcher
	int startIndex;
	//start index of the substring matched by the java regex matcher
	int endIndex;
	//reg expression compilation and matching 
	Pattern p = Pattern.compile(regex);
	Matcher m = p.matcher(input);
	
	Node parent = null;
	NodeList parentList = null;
	System.out.println("in buildDom2");
	/*
	  if(document == null){
	  //build a new document factory
	  DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	  DocumentBuilder builder = factory.newDocumentBuilder();	    
	  document = builder.newDocument();	   
	  }
	*/
	// now extract the data using regexpression
	
	if(m.lookingAt()){
	    startIndex = m.start();
	    endIndex = m.end();
	    String answer = m.group();
	    System.out.println("***answer: " + answer);
	    if(startIndex != endIndex){
		//here need to check path and then stick in  at the right position
		if(!path.equals("")){
		    //append a dummy parent node named root assuming all paths are absolute, ie start at the top 
		    path = "root/"+path;
		    //tokenize with a "/" as the separator
		    StringTokenizer tokenizer = new StringTokenizer(path, "/");
		    
		    while(tokenizer.hasMoreTokens()){
			//go through the path
			String token = tokenizer.nextToken();
			System.out.println("initial token: " + token);
			if(!document.hasChildNodes()){
				//the document is brand new... create the root
			    System.out.println("document is new");
			    Element child = (Element)document.createElement(token);
			    document.appendChild(child);
			    parent = child;//now the parent is the root
			    token = tokenizer.nextToken();
			}
			if(document.hasChildNodes()){
				//document already has some nodes. 
				//therefore need to check if the path given is already mapped out.
			    if(parent == null){
				//make parent=root
				parent = document.getDocumentElement();
				System.out.println("first child = " + parent.getNodeName());
				token = tokenizer.nextToken();
			    }
			    if(parent.hasChildNodes()){ 
				//get the list of those children
				parentList = parent.getChildNodes();
				System.out.println("parent got children");
				
				int parentListLen = parentList.getLength();
				int i = 0;
				boolean done = false;
				
				while(!done){
				    
				    //going thru the parentList to check if path exists.
				    /*xxx
				      NodeList temp = document.getElementsByTagName(token);
				      if(temp != null){
				      parent = temp.item(0);
				      if(parent.hasChildNodes()){
				      parentList = parent.getChildNodes();
				      }
				      else{
				      parentList = null;
				      }					
				      done = true;
				      }
				    */
				    if(i == parentListLen){
					parentList = null;//then list is null
					System.out.println("setting list null");
					done = true;
				    }
				    //otherwise go thru the path
				    else if((parentList.item(i)).getNodeName().equals(token)){
					parent = parentList.item(i);// now the new parent is the named token
					System.out.println("new parent: " + token);
					parentList = parent.getChildNodes();
					done = true;
				    }
				    System.out.println("i: " + i);
				    i++;
				}//while
			    }// if parent has children
			    if(!parent.hasChildNodes() || parentList == null){
				//create node named in path
				System.out.println("creating new node: " + token);
				parent.appendChild((Element)document.createElement(token));
				NodeList temp = document.getElementsByTagName(token);
				parent = temp.item(0);
			    }
			}
			System.out.println("token: " + token);
		    }//while more tokens
		    //having navigated the tree, check if it has the attribute
		    /*
		      NamedNodeMap attrMap = null;
		      Attr attrNode = null;
		      if(parent.hasAttributes()){
		      //now get the attribute, and replace it with a new value
		      attrMap = parent.getAttributes();
		      attrNode = (Attr)attrMap.getNamedItem(attribute);
		      if(attrNode != null){
		      String oldAttr = attrNode.getValue();
		      attrNode.setValue(oldAttr + ":" +answer);
		      }
		      
		      }
		      //the attribute does not exist. therefore create a new one.
		      else{
		      ((Element)parent).setAttribute(attribute, answer);
		      
		      }
		    */
		    parent.appendChild((Element)document.createElement(answer));
		    System.out.println("printing answer: " + answer);
		    while(m.find()){
			startIndex = m.start();
			endIndex = m.end();
			if(startIndex != endIndex){
			    //check if found the attribute node
			    /*if(attrMap != null && attrNode != null){
			      String oldValue = attrNode.getValue();
			      attrNode.setValue(oldValue + ":"+input.substring(startIndex, endIndex));
			      }*/
			    
			    parent.appendChild((Element)document.createElement(input.substring(startIndex, endIndex)));
			}
			//}//while has more tokens
		    }//if path exists
		} //go thru tree and place in right position
		else{ //stick it in at the root since no path given
		    Element root = (Element)document.createElement("root");
		    document.appendChild(root);
		    System.out.println("answer1: " + answer);
		    root.appendChild((Element)document.createElement(answer));
		    while(m.find()){
			startIndex = m.start();
			endIndex = m.end();
			if(startIndex != endIndex)
			    
			    root.appendChild((Element)document.createElement(input.substring(startIndex, endIndex)));
		    }
		    
		    //xxx
		    //root.appendChild((Element)document.createElement(input.substring(startIndex, endIndex)));
		    //parent.appendChild((Element)document.createElement(input.substring(startIndex, endIndex)));
		    
		}
	    }
	}
	/*
	  }catch (ParserConfigurationException pce) {
	  // Parser with specified options can't be built
	  pce.printStackTrace();
	  
	  }
	*/
	return document;
	else{
	    return null;
	}
    } // buildDom
    
    public static void usage(){
	System.out.println("Usage: java DomPath -s [string] -r [regex] -p [xpath]");
    }
    
    // An array of names for DOM node-types
    // (Array indexes = nodeType() values.)
    static final String[] typeName = {
        "none",
        "Element",
        "Attr",
        "Text",
        "CDATA",
        "EntityRef",
        "Entity",
        "ProcInstr",
        "Comment",
        "Document",
        "DocType",
        "DocFragment",
        "Notation",
    };
    static final int ELEMENT_TYPE =   1;
    static final int ATTR_TYPE =      2;
    static final int TEXT_TYPE =      3;
    static final int CDATA_TYPE =     4;
    static final int ENTITYREF_TYPE = 5;
    static final int ENTITY_TYPE =    6;
    static final int PROCINSTR_TYPE = 7;
    static final int COMMENT_TYPE =   8;
    static final int DOCUMENT_TYPE =  9;
    static final int DOCTYPE_TYPE =  10;
    static final int DOCFRAG_TYPE =  11;
    static final int NOTATION_TYPE = 12;
    // The list of elements to display in the tree
    // Could set this with a command-line argument, but
    // not much point -- the list of tree elements still
    // has to be defined internally.
    // Extra credit: Read the list from a file
    // Super-extra credit: Process a DTD and build the list.
   static String[] treeElementNames = {
        "slideshow",
        "slide",
        "title",         // For slideshow #1
        "slide-title",   // For slideshow #10
        "item",
    };
    boolean treeElement(String elementName) {
      for (int i=0; i<treeElementNames.length; i++) {
        if ( elementName.equals(treeElementNames[i]) ) return true;
      }
      return false;
    }

    // This class wraps a DOM node and returns the text we want to
    // display in the tree. It also returns children, index values,
    // and child counts.
    public class AdapterNode 
    { 
	org.w3c.dom.Node domNode;

      // Construct an Adapter node from a DOM node
      public AdapterNode(org.w3c.dom.Node node) {
        domNode = node;
      }

      // Return a string that identifies this node in the tree
      // *** Refer to table at top of org.w3c.dom.Node ***
      public String toString() {
        String s = typeName[domNode.getNodeType()];
        String nodeName = domNode.getNodeName();
        if (! nodeName.startsWith("#")) {
           s += ": " + nodeName;
        }
        if (compress) {
           String t = content().trim();
           int x = t.indexOf("\n");
           if (x >= 0) t = t.substring(0, x);
           s += " " + t;
           return s;
        }
        if (domNode.getNodeValue() != null) {
           if (s.startsWith("ProcInstr")) 
              s += ", "; 
           else 
              s += ": ";
           // Trim the value to get rid of NL's at the front
           String t = domNode.getNodeValue().trim();
           int x = t.indexOf("\n");
           if (x >= 0) t = t.substring(0, x);
           s += t;
        }
        return s;
      }

      public String content() {
        String s = "";
        org.w3c.dom.NodeList nodeList = domNode.getChildNodes();
        for (int i=0; i<nodeList.getLength(); i++) {
          org.w3c.dom.Node node = nodeList.item(i);
          int type = node.getNodeType();
          AdapterNode adpNode = new AdapterNode(node); //inefficient, but works
          if (type == ELEMENT_TYPE) {
            // Skip subelements that are displayed in the tree.   
            if ( treeElement(node.getNodeName()) ) continue;

            // EXTRA-CREDIT HOMEWORK:
            //   Special case the SLIDE element to use the TITLE text
            //   and ignore TITLE element when constructing the tree.

            // EXTRA-CREDIT
            //   Convert ITEM elements to html lists using
            //   <ul>, <li>, </ul> tags

            s += "<" + node.getNodeName() + ">";
            s += adpNode.content();
            s += "</" + node.getNodeName() + ">";
          } else if (type == TEXT_TYPE) {
            s += node.getNodeValue();
          } else if (type == ENTITYREF_TYPE) {
            // The content is in the TEXT node under it
            s += adpNode.content();
          } else if (type == CDATA_TYPE) {
            // The "value" has the text, same as a text node.
            //   while EntityRef has it in a text node underneath.
            //   (because EntityRef can contain multiple subelements)
            // Convert angle brackets and ampersands for display
            StringBuffer sb = new StringBuffer( node.getNodeValue() );
            for (int j=0; j<sb.length(); j++) {
              if (sb.charAt(j) == '<') {
                sb.setCharAt(j, '&');
                sb.insert(j+1, "lt;");
                j += 3;
              } else if (sb.charAt(j) == '&') {
                sb.setCharAt(j, '&');
                sb.insert(j+1, "amp;");
                j += 4;
              }
            }
            s += "<pre>" + sb + "\n</pre>";
          }
           // Ignoring these:
           //   ATTR_TYPE      -- not in the DOM tree
           //   ENTITY_TYPE    -- does not appear in the DOM
           //   PROCINSTR_TYPE -- not "data"
           //   COMMENT_TYPE   -- not "data"
           //   DOCUMENT_TYPE  -- Root node only. No data to display.
           //   DOCTYPE_TYPE   -- Appears under the root only
           //   DOCFRAG_TYPE   -- equiv. to "document" for fragments
           //   NOTATION_TYPE  -- nothing but binary data in here
        }
        return s;
      }

      /*
       * Return children, index, and count values
       */
      public int index(AdapterNode child) {
        //System.err.println("Looking for index of " + child);
        int count = childCount();
        for (int i=0; i<count; i++) {
          AdapterNode n = this.child(i);
          if (child.domNode == n.domNode) return i;
        }
        return -1; // Should never get here.
      }

      public AdapterNode child(int searchIndex) {
        //Note: JTree index is zero-based. 
        org.w3c.dom.Node node = 
             domNode.getChildNodes().item(searchIndex);
        if (compress) {
          // Return Nth displayable node
          int elementNodeIndex = 0;
          for (int i=0; i<domNode.getChildNodes().getLength(); i++) {
            node = domNode.getChildNodes().item(i);
            if (node.getNodeType() == ELEMENT_TYPE 
            && treeElement( node.getNodeName() )
            && elementNodeIndex++ == searchIndex) {
               break; 
            }
          }
        }
        return new AdapterNode(node); 
      }

      public int childCount() {
        if (!compress) {
          // Indent this
          return domNode.getChildNodes().getLength();  
        } 
        int count = 0;
        for (int i=0; i<domNode.getChildNodes().getLength(); i++) {
           org.w3c.dom.Node node = domNode.getChildNodes().item(i); 
           if (node.getNodeType() == ELEMENT_TYPE
           && treeElement( node.getNodeName() )) 
           {
             // Note: 
             //   Have to check for proper type. 
             //   The DOCTYPE element also has the right name
             ++count;
           }
        }
        return count;
      }
    }

    // This adapter converts the current Document (a DOM) into 
    // a JTree model. 
    public class DomToTreeModelAdapter 
      implements javax.swing.tree.TreeModel 
    {
      // Basic TreeModel operations
      public Object  getRoot() {
        //System.err.println("Returning root: " +document);
        return new AdapterNode(document);
      }
      public boolean isLeaf(Object aNode) {
        // Determines whether the icon shows up to the left.
        // Return true for any node with no children
        AdapterNode node = (AdapterNode) aNode;
        if (node.childCount() > 0) return false;
        return true;
      }
      public int     getChildCount(Object parent) {
        AdapterNode node = (AdapterNode) parent;
        return node.childCount();
      }
      public Object getChild(Object parent, int index) {
        AdapterNode node = (AdapterNode) parent;
        return node.child(index);
      }
      public int getIndexOfChild(Object parent, Object child) {
        AdapterNode node = (AdapterNode) parent;
        return node.index((AdapterNode) child);
      }
      public void valueForPathChanged(TreePath path, Object newValue) {
        // Null. We won't be making changes in the GUI
        // If we did, we would ensure the new value was really new,
        // adjust the model, and then fire a TreeNodesChanged event.
      }

      /*
       * Use these methods to add and remove event listeners.
       * (Needed to satisfy TreeModel interface, but not used.)
       */
      private Vector listenerList = new Vector();
      public void addTreeModelListener(TreeModelListener listener) {
        if ( listener != null 
        && ! listenerList.contains( listener ) ) {
           listenerList.addElement( listener );
        }
      }
      public void removeTreeModelListener(TreeModelListener listener) {
        if ( listener != null ) {
           listenerList.removeElement( listener );
        }
      }
	
	// Note: Since XML works with 1.1, this example uses Vector.
	// If coding for 1.2 or later, though, I'd use this instead:
	//   private List listenerList = new LinkedList();
	// The operations on the List are then add(), remove() and
	// iteration, via:
	//  Iterator it = listenerList.iterator();
	//  while ( it.hasNext() ) {
	//    TreeModelListener listener = (TreeModelListener) it.next();
	//    ...
	//  }
	
	/*
	 * Invoke these methods to inform listeners of changes.
	 * (Not needed for this example.)
	 * Methods taken from TreeModelSupport class described at 
	 *   http://java.sun.com/products/jfc/tsc/articles/jtree/index.html
	 * That architecture (produced by Tom Santos and Steve Wilson)
	 * is more elegant. I just hacked 'em in here so they are
	 * immediately at hand.
	 */
	public void fireTreeNodesChanged( TreeModelEvent e ) {
	    Enumeration listeners = listenerList.elements();
	    while ( listeners.hasMoreElements() ) {
		TreeModelListener listener = 
		    (TreeModelListener) listeners.nextElement();
		listener.treeNodesChanged( e );
	    }
	} 
      public void fireTreeNodesInserted( TreeModelEvent e ) {
	  Enumeration listeners = listenerList.elements();
	  while ( listeners.hasMoreElements() ) {
	      TreeModelListener listener =
		  (TreeModelListener) listeners.nextElement();
	      listener.treeNodesInserted( e );
	  }
      }   
	public void fireTreeNodesRemoved( TreeModelEvent e ) {
	    Enumeration listeners = listenerList.elements();
	    while ( listeners.hasMoreElements() ) {
		TreeModelListener listener = 
		    (TreeModelListener) listeners.nextElement();
		listener.treeNodesRemoved( e );
	    }
	}   
	public void fireTreeStructureChanged( TreeModelEvent e ) {
	    Enumeration listeners = listenerList.elements();
	    while ( listeners.hasMoreElements() ) {
		TreeModelListener listener =
		    (TreeModelListener) listeners.nextElement();
		listener.treeStructureChanged( e );
	    }
	}
    }
}





