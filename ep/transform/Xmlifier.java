

/*
 * Xmlifier.java
 *
 * Created on May 1, 2002, 3:04 PM
 */

package psl.xues.ep.transform;

import psl.xues.ep.event.EPEvent;
import psl.xues.ep.event.DOMEvent;
import psl.xues.ep.event.StringEvent;

import org.w3c.dom.Document;
import org.w3c.dom.DOMException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;

import java.util.*;
import java.util.regex.*;
/**
 *
 * @author  Amna Qaiser
 */
public class Xmlifier extends EPTransform{
    private Element root = null;
    /** Creates a new instance of Xmlifier 
     *@param String input on which regular expression is applied
     *@param String regex the regular expression to be applied
     *@param String path in the xml document at the end of which the 
     *extracted strings will be appended
     */
    public Xmlifier(Element el) throws InstantiationException {
	super(el);  
	root = el;
	if(root == null){
	    debug.warn( "No rules for xmlification given");
	}
    }
    
    /**
     * Handle a transform request.
     */
    public EPEvent transform(EPEvent original){
	//check to make sure its a string event
	EPEvent org = original;
	if(!(original.getFormat().equals("StringEvent"))){
	    debug.warn("Cannot transform event as it is not of type String");
	    return original;
	}
	
	//Extract the string
	String data = ((StringEvent)original).getStringEvent();
	if(data.length() ==0){
	    debug.warn("Empty string in StringEvent. Doing no XMLification. ");
	    return original; //nothing to do
	}
	//extract information and create a Dom tree
	Document doc = extractDomInfo(data);
	//get root of the created Dom
	Element e = doc.getDocumentElement();
	DOMEvent Devent = new DOMEvent("dont-know", e);
	return Devent;
    }
    
    /** extract information from the xml file given to create the DOM tree 
     *@param String input on which the regular expression is to be applied
     */
    public Document extractDomInfo(String input){
	try{
	    
	    //build a new document factory
	    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	    DocumentBuilder builder = factory.newDocumentBuilder();	    
	    Document document = builder.newDocument();
	    
	    
	    Element instructions = root;
	    
	    if(input.equals("")){
		//invalid input, throw a tantrum
		debug.warn("No data provided to xmlify");
	    }
	    
	    else{
		
		//proceed to extract information from the xml file
		/*xxx not needed any more?
		  //step 1: parse the xml file.
		  Document instructions = builder.parse(new File(fileName));
		*/
		document = builder.newDocument();
		//step 2a: get a list of regular expressions to use on string
		//the xml file must contain regex within the tag "pattern"
		NodeList patternList = instructions.getElementsByTagName("pattern");
		
		//step 2b: get a list of locations where to store the extracted data
		//the xml file must contain location within the tag "location"
		NodeList locationList = instructions.getElementsByTagName("location");
		
		//step 2c: get a list of attribute names where to store
		//the extracted data
		//the xml file must contain location within the tag "attrname"
		NodeList attrList = instructions.getElementsByTagName("attrname");
		
		//in case the number of tags in the xml file dont match up, shutdown.
		if((attrList.getLength()!= patternList.getLength()) && (attrList.getLength()!= locationList.getLength())){
		    debug.fatal("Error: Invalid xml file. Tags missing.");
		    System.exit(1);
		}
		
		else{
		    //get the total number of rules to process
		    int ruleNo = locationList.getLength();
		    
		    //now extract the pattern, location and attribute 
		    //and build the dom for each set
		    for(int i = 0; i < ruleNo; i++){
			String pattern = (patternList.item(i)).getFirstChild().getNodeValue();
			String location = locationList.item(i).getFirstChild().getNodeValue();
			String attribute = attrList.item(i).getFirstChild().getNodeValue();
			document = buildDom(input, pattern, location, attribute, document);		
		    }
		}
	    }
	    return document;
	}
	//do some error catching
	catch(ParserConfigurationException pc){
	    pc.printStackTrace();
	}
	return null;
    }
    /* this method builds the actual tree
     * @param String input on which regular expressions will be applied
     * @param String regex, the regular expression applied on input
     * @param String path, the path within the document at which the
     * extracted string will be stored. If the path doesn't exist in
     * the document, it is create. If the path tokens specified are 
     * not immediate children of but are present under the root, the path is taken
     * to be relative to them.
     * @param String attribute in which the extract expression will be stored
     * @return Document a dom document
     */
    private Document buildDom(String input, String regex, String path, String attribute, Document document){
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
				parent = document.getFirstChild();
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
				    i++;
				    //going thru the parentList to check if path exists.
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
		    //having navigated the tree, stick in the new node
		    parent.appendChild((Element)document.createElement(answer));
		    while(m.find()){
			startIndex = m.start();
			endIndex = m.end();
			if(startIndex != endIndex)
			    parent.appendChild((Element)document.createElement(input.substring(startIndex, endIndex)));
		    }
		    //}//while has more tokens
		}//if path exists
		//go thru tree and place in right position
		else{ //stick it in at the root since no path given
		    Element root = (Element)document.createElement("root");
		    document.appendChild(root);
		    System.out.println("answer1: " + answer);
		    root.appendChild((Element)document.createElement(answer));
		    while(m.find()){
			startIndex = m.start();
			endIndex = m.end();
			if(startIndex != endIndex)
			    ;
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
    } // buildDom
    
    public static void usage(){
	System.out.println("Usage: java DomPath -s [string] -r [regex] -p [xpath]");
    }
}
