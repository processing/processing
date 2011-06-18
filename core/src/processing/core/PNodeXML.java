/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2009-11 Ben Fry and Casey Reas

  This library is free software; you can redistribute it and/or
  modify it under the terms of the GNU Lesser General Public
  License as published by the Free Software Foundation, version 2.

  This library is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty 
  of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  
  See the GNU Lesser General Public License for more details.

  You should have received a copy of the GNU Lesser General
  Public License along with this library; if not, write to the
  Free Software Foundation, Inc., 59 Temple Place, Suite 330,
  Boston, MA  02111-1307  USA
*/

package processing.core;

import java.io.*;
import javax.xml.parsers.*;

import org.w3c.dom.*;
import org.xml.sax.*;

import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;

import processing.core.PApplet;


/**
 * This is the base class used for the Processing XML library, 
 * representing a single node of an XML tree.
 */
public class PNodeXML implements PNode,Serializable {

  /** The internal representation, a DOM node. */
  protected Node node;
  
  /** Cached locally because it's used often. */
  protected String name;
  
  /** The parent element. */
  protected PNode parent;

  /** Child elements, once loaded. */
  protected PNode[] children;
  

  protected PNodeXML() { }
  
  
  /**
   * Begin parsing XML data passed in from a PApplet. This code
   * wraps exception handling, for more advanced exception handling,
   * use the constructor that takes a Reader or InputStream.
   */
  public PNodeXML(PApplet parent, String filename) {
    this(parent.createReader(filename));
  }


//  public XML(String xml) {
//    this(new StringReader(xml));
//  }


  public PNodeXML(Reader reader) {
    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
//      factory.setValidating(false);
//      factory.setAttribute("http://xml.org/sax/features/namespaces", true);
//      factory.setAttribute("http://xml.org/sax/features/validation", false);
//      factory.setAttribute("http://xml.org/sax/features/validation", true);
//      factory.setAttribute("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
      // enable this to temporarily get around some parsing quirks (and get a proper error msg)
//      factory.setFeature("http://apache.org/xml/features/dom/defer-node-expansion", false);
      // Prevent 503 errors from www.w3.org
      factory.setAttribute("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
//      factory.setAttribute("http://apache.org/xml/features/dom/create-entity-ref-nodes", false);
      factory.setExpandEntityReferences(false);
//      factory.setExpandEntityReferences(true);
//      factory.setCoalescing(true);
//      builderFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      DocumentBuilder builder = factory.newDocumentBuilder();
//      builder.setEntityResolver()
      
//      SAXParserFactory spf = SAXParserFactory.newInstance();
//      spf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
//      SAXParser p = spf.newSAXParser();
            
      //    builder = DocumentBuilderFactory.newDocumentBuilder();
      //    builder = new SAXBuilder();
      //    builder.setValidation(validating);
      
//      print(dataPath("1broke.html"), System.out);
      
//      Document document = builder.parse(dataPath("1_alt.html"));
      Document document = builder.parse(new InputSource(reader));
      node = document.getDocumentElement();
//      NodeList nodeList = document.getDocumentElement().getChildNodes();
//      for (int i = 0; i < nodeList.getLength(); i++) {
//      }
//      print(createWriter("data/1_alt_reparse.html"), document.getDocumentElement(), 0);
      
    } catch (ParserConfigurationException pce) {
      pce.printStackTrace();
    } catch (IOException e1) {
      e1.printStackTrace();
    } catch (SAXException e2) {
      e2.printStackTrace();
    }    
  }
  
  
  public PNodeXML(PNode parent, Node node) {
    this.parent = parent;
    this.node = node;
    
    if (node.getNodeType() == Node.ELEMENT_NODE) {
      name = node.getNodeName();
    }
  }
  
  
  static public PNode parse(String xml) { 
    return new PNodeXML(new StringReader(xml));
  }


  /**
   * Returns the parent element. This method returns null for the root
   * element.
   */
  public PNode getParent() {
    return this.parent;
  }

    
  /**
   * Returns the full name (i.e. the name including an eventual namespace
   * prefix) of the element.
   * @return the name, or null if the element only contains #PCDATA.
   */
  public String getName() {
    return name;
  }

    
  /**
   * Returns the name of the element (without namespace prefix).
   * @return the name, or null if the element only contains #PCDATA.
   */
  public String getLocalName() {
    return node.getLocalName();
  }


  /**
   * Honey, can you just check on the kids? Thanks.
   */
  protected void checkChildren() {
    if (children == null) {
      NodeList kids = node.getChildNodes();
      int childCount = kids.getLength();
      children = new PNode[childCount];
      for (int i = 0; i < childCount; i++) {
        children[i] = new PNodeXML(this, kids.item(i));
      }
    }
  }


  /**
   * Returns the number of children.
   * @return the count.
   */
  public int getChildCount() {
    checkChildren();
    return children.length;
  }

    
  /**
   * Put the names of all children into an array. Same as looping through 
   * each child and calling getName() on each XMLElement.
   */
  public String[] listChildren() {
//    NodeList children = node.getChildNodes();
//    int childCount = children.getLength();
//    String[] outgoing = new String[childCount];
//    for (int i = 0; i < childCount; i++) {
//      Node kid = children.item(i);
//      if (kid.getNodeType() == Node.ELEMENT_NODE) {
//        outgoing[i] = kid.getNodeName();
//      } // otherwise just leave him null
//    }
    checkChildren();
    String[] outgoing = new String[children.length];
    for (int i = 0; i < children.length; i++) {
      outgoing[i] = children[i].getName();
    }
    return outgoing;
  }
    
    
  /**
   * Returns an array containing all the child elements.
   */
  public PNode[] getChildren() {
//    NodeList children = node.getChildNodes();
//    int childCount = children.getLength();
//    XMLElement[] kids = new XMLElement[childCount];
//    for (int i = 0; i < childCount; i++) {
//      Node kid = children.item(i);
//      kids[i] = new XMLElement(this, kid);
//    }
//    return kids;
    checkChildren();
    return children;
  }


  /**
   * Quick accessor for an element at a particular index.
   * @author processing.org
   */
  public PNode getChild(int index) {
    checkChildren();
    return children[index];
  }


  /**
   * Get a child by its name or path.
   * @param name element name or path/to/element
   * @return the first matching element
   */
  public PNode getChild(String name) {
    if (name.indexOf('/') != -1) {
      return getChildRecursive(PApplet.split(name, '/'), 0);
    }
    int childCount = getChildCount();
    for (int i = 0; i < childCount; i++) {
      PNode kid = getChild(i);
      String kidName = kid.getName();
      if (kidName != null && kidName.equals(name)) {
        return kid;
      }
    }
    return null;
  }


  /**
   * Internal helper function for getChild(String).
   * @param items result of splitting the query on slashes
   * @param offset where in the items[] array we're currently looking
   * @return matching element or null if no match
   * @author processing.org
   */
  protected PNode getChildRecursive(String[] items, int offset) {
    // if it's a number, do an index instead
    if (Character.isDigit(items[offset].charAt(0))) {
      PNodeXML kid = (PNodeXML) getChild(Integer.parseInt(items[offset]));
      if (offset == items.length-1) {
        return kid;
      } else {
        return kid.getChildRecursive(items, offset+1);
      }
    }
    int childCount = getChildCount();
    for (int i = 0; i < childCount; i++) {
      PNodeXML kid = (PNodeXML) getChild(i);
      String kidName = kid.getName();
      if (kidName != null && kidName.equals(items[offset])) {
        if (offset == items.length-1) {
          return kid;
        } else {
          return kid.getChildRecursive(items, offset+1);
        }
      }
    }
    return null;
  }


  /**
   * Get any children that match this name or path. Similar to getChild(),
   * but will grab multiple matches rather than only the first.
   * @param name element name or path/to/element
   * @return array of child elements that match
   * @author processing.org
   */
  public PNode[] getChildren(String name) {
    if (name.indexOf('/') != -1) {
      return getChildrenRecursive(PApplet.split(name, '/'), 0);
    }
    // if it's a number, do an index instead
    // (returns a single element array, since this will be a single match
    if (Character.isDigit(name.charAt(0))) {
      return new PNode[] { getChild(Integer.parseInt(name)) };
    }
    int childCount = getChildCount();
    PNodeXML[] matches = new PNodeXML[childCount];
    int matchCount = 0;
    for (int i = 0; i < childCount; i++) {
      PNodeXML kid = (PNodeXML) getChild(i);
      String kidName = kid.getName();
      if (kidName != null && kidName.equals(name)) {
        matches[matchCount++] = kid;
      }
    }
    return (PNodeXML[]) PApplet.subset(matches, 0, matchCount);
  }


  protected PNode[] getChildrenRecursive(String[] items, int offset) {
    if (offset == items.length-1) {
      return getChildren(items[offset]);
    }
    PNodeXML[] matches = (PNodeXML[]) getChildren(items[offset]);
    PNodeXML[] outgoing = new PNodeXML[0];
    for (int i = 0; i < matches.length; i++) {
      PNode[] kidMatches = matches[i].getChildrenRecursive(items, offset+1);
      outgoing = (PNodeXML[]) PApplet.concat(outgoing, kidMatches);
    }
    return outgoing;
  }


  /**
   * Returns the number of attributes.
   */
  public int getAttributeCount() {
    return node.getAttributes().getLength();
  }

  
  /**
   * Get a list of the names for all of the attributes for this node.
   */
  public String[] listAttributes() {
    NamedNodeMap nnm = node.getAttributes();
    String[] outgoing = new String[nnm.getLength()];
    for (int i = 0; i < outgoing.length; i++) {
      outgoing[i] = nnm.item(i).getNodeName();
    }
    return outgoing;
  }

  /**
   * Returns whether an attribute exists.
   */
  public boolean hasAttribute(String name) {
    return (node.getAttributes().getNamedItem(name) != null);
  }


  /**
   * Returns the value of an attribute.
   * @param name the non-null name of the attribute.
   * @return the value, or null if the attribute does not exist.
   */
//  public String getAttribute(String name) {
//    return this.getAttribute(name, null);
//  }


  /**
   * Returns the value of an attribute.
   *
   * @param name the non-null full name of the attribute.
   * @param defaultValue the default value of the attribute.
   *
   * @return the value, or defaultValue if the attribute does not exist.
   */
//  public String getAttribute(String name, String defaultValue) {
//    Node attr = node.getAttributes().getNamedItem(name);
//    return (attr == null) ? defaultValue : attr.getNodeValue();
//  }

  
  /** @deprecated */
  public String getStringAttribute(String name) {
    return getString(name, null);
  }

  
  public String getString(String name) {
    return getString(name, null);
  }


  public String getString(String name, String defaultValue) {
    Node attr = node.getAttributes().getNamedItem(name);
    return (attr == null) ? defaultValue : attr.getNodeValue();
  }


  /** @deprecated */
  public int getIntAttribute(String name) {
    return getInt(name, 0);
  }


  public int getInt(String name) {
    return getInt(name, 0);
  }


  /**
   * Returns the value of an attribute.
   *
   * @param name the non-null full name of the attribute.
   * @param defaultValue the default value of the attribute.
   *
   * @return the value, or defaultValue if the attribute does not exist.
   */
  public int getInt(String name, int defaultValue) {
    String value = getString(name);
    return (value == null) ? defaultValue : Integer.parseInt(value);
  }


  /** @deprecated */
  public float getFloatAttribute(String name) {
    return getFloat(name);
  }


  /**
   * Returns the value of an attribute, or zero if not present.
   */
  public float getFloat(String name) {
    return getFloat(name, 0);
  }
  
  
  /**
   * Returns the value of an attribute.
   *
   * @param name the non-null full name of the attribute.
   * @param defaultValue the default value of the attribute.
   *
   * @return the value, or defaultValue if the attribute does not exist.
   */
  public float getFloat(String name, float defaultValue) {
    String value = getString(name);
    return (value == null) ? defaultValue : Float.parseFloat(value);
  }


  /** @deprecated */
  public double getDoubleAttribute(String name) {
    return getDouble(name, 0);
  }


  public double getDouble(String name) {
    return getDouble(name, 0);
  }

  
  /**
   * Returns the value of an attribute.
   *
   * @param name the non-null full name of the attribute.
   * @param defaultValue the default value of the attribute.
   *
   * @return the value, or defaultValue if the attribute does not exist.
   */
  public double getDouble(String name, double defaultValue) {
    String value = getString(name);
    return (value == null) ? defaultValue : Double.parseDouble(value);
  }


  /**
   * Return the #PCDATA content of the element. If the element has a
   * combination of #PCDATA content and child elements, the #PCDATA
   * sections can be retrieved as unnamed child objects. In this case,
   * this method returns null.
   *
   * @return the content.
   */
  public String getContent() {
    return node.getTextContent();
  }

  
  public String toString() {
    return toString(true);
  }
  
  
  public String toString(boolean indent) {
    try {
      DOMSource dumSource = new DOMSource(node);
      TransformerFactory tf = TransformerFactory.newInstance();
      Transformer transformer = tf.newTransformer();
      // if this is the root, output the decl, if not, hide it
      if (parent != null) {
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
      }
      transformer.setOutputProperty(OutputKeys.METHOD, "xml");
//      transformer.setOutputProperty(OutputKeys.ENCODING,"ISO-8859-1");
      transformer.setOutputProperty(OutputKeys.ENCODING,"UTF8");
      // indent by default, but sometimes this needs to be turned off
      if (indent) {
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
      }
      java.io.StringWriter sw = new java.io.StringWriter();
      StreamResult sr = new StreamResult(sw);
      transformer.transform(dumSource, sr);
      return sw.toString();
      
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
    
//    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
//    try {
//      DocumentBuilder builder = factory.newDocumentBuilder();
//      //builder.get
////      Document document = builder.
//      
//    } catch (ParserConfigurationException e) {
//      e.printStackTrace();
//    }

    
    
    //    Document doc = new DocumentImpl();
//    return node.toString();
    
//    TransformerFactory transfac = TransformerFactory.newInstance();
//    Transformer trans = transfac.newTransformer();
//    trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
//    trans.setOutputProperty(OutputKeys.INDENT, "yes");
//
//    //create string from xml tree
//    StringWriter sw = new StringWriter();
//    StreamResult result = new StreamResult(sw);
////    Document doc = 
//    DOMSource source = new DOMSource(doc);
//    trans.transform(source, result);
//    String xmlString = sw.toString();

  }
  
  
//    return toString(true);
//  }
//    
//
//  // TODO finish the writer here!
//  public String toString(boolean pretty) {
//    ByteArrayOutputStream baos = new ByteArrayOutputStream();
//    OutputStreamWriter osw = new OutputStreamWriter(baos);
//        XMLWriter writer = new XMLWriter(osw);
//        try {
//          if (pretty) {
//            writer.write(this, true, 2, true);
//          } else {
//            writer.write(this, false, 0, true);
//          }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    return baos.toString();
//  }
}
