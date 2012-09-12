/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2012 The Processing Foundation
  Copyright (c) 2009-12 Ben Fry and Casey Reas

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

package processing.data;

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
 *
 * @webref data:composite
 * @see PApplet#loadXML(String)
 */
public class XML implements Serializable {

  /** The internal representation, a DOM node. */
  protected Node node;

  /** Cached locally because it's used often. */
  protected String name;

  /** The parent element. */
  protected XML parent;

  /** Child elements, once loaded. */
  protected XML[] children;


  protected XML() { }


  /**
   * Begin parsing XML data passed in from a PApplet. This code
   * wraps exception handling, for more advanced exception handling,
   * use the constructor that takes a Reader or InputStream.
   *
   * @throws SAXException
   * @throws ParserConfigurationException
   * @throws IOException
   */
  public XML(PApplet parent, String filename) throws IOException, ParserConfigurationException, SAXException {
    this(parent.createReader(filename));
  }


  public XML(File file) throws IOException, ParserConfigurationException, SAXException {
    this(PApplet.createReader(file));
  }


  public XML(Reader reader) throws IOException, ParserConfigurationException, SAXException {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

    // Prevent 503 errors from www.w3.org
    try {
      factory.setAttribute("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
    } catch (IllegalArgumentException e) {
      // ignore this; Android doesn't like it
    }

    // without a validating DTD, this doesn't do anything since it doesn't know what is ignorable
//      factory.setIgnoringElementContentWhitespace(true);

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
    name = node.getNodeName();
//      NodeList nodeList = document.getDocumentElement().getChildNodes();
//      for (int i = 0; i < nodeList.getLength(); i++) {
//      }
//      print(createWriter("data/1_alt_reparse.html"), document.getDocumentElement(), 0);
  }


  // TODO is there a more efficient way of doing this? wow.
  // i.e. can we use one static document object for all PNodeXML objects?
  public XML(String name) {
    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      DocumentBuilder builder = factory.newDocumentBuilder();
      Document document = builder.newDocument();
      node = document.createElement(name);

      this.name = name;
      this.parent = null;

    } catch (ParserConfigurationException e) {
      e.printStackTrace();
    }
  }


  protected XML(XML parent, Node node) {
    this.node = node;
    this.parent = parent;
    this.name = node.getNodeName();
  }


  static public XML parse(String xml) {
    try {
      return new XML(new StringReader(xml));
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }


  public boolean save(OutputStream output) {
    return save(PApplet.createWriter(output));
  }


  public boolean save(File file) {
    return save(PApplet.createWriter(file));
  }


  public boolean save(PrintWriter output) {
    output.print(format(2));
    output.flush();
    return true;
  }


  /**
   * Returns the parent element. This method returns null for the root
   * element.
   *
   * @webref xml:method
   * @brief Gets a copy of the element's parent
   */
  public XML getParent() {
    return this.parent;
  }

  /**
   * Internal function; not included in reference.
   */
  protected Node getNode() {
    return node;
  }


  /**
   * Returns the full name (i.e. the name including an eventual namespace
   * prefix) of the element.
   *
   * @webref xml:method
   * @brief Gets the element's full name
   * @return the name, or null if the element only contains #PCDATA.
   */
  public String getName() {
    return name;
  }

  /**
   * @webref xml:method
   * @brief Sets the element's name
   */
  public void setName(String newName) {
    Document document = node.getOwnerDocument();
    node = document.renameNode(node, null, newName);
    name = node.getNodeName();
  }


  /**
   * Returns the name of the element (without namespace prefix).
   *
   * @webref xml:method
   * @brief Gets the element's name
   * @return the name, or null if the element only contains #PCDATA.
   */
  public String getLocalName() {
    return node.getLocalName();
  }


  /**
   * Honey, can you just check on the kids? Thanks.
   *
   * Internal function; not included in reference.
   */
  protected void checkChildren() {
    if (children == null) {
      NodeList kids = node.getChildNodes();
      int childCount = kids.getLength();
      children = new XML[childCount];
      for (int i = 0; i < childCount; i++) {
        children[i] = new XML(this, kids.item(i));
      }
    }
  }


  /**
   * Returns the number of children.
   *
   * @webref xml:method
   * @brief Returns the element's number of children
   * @return the count.
   */
  public int getChildCount() {
    checkChildren();
    return children.length;
  }


  /**
   * Returns a boolean of whether or not there are children.
   *
   * @webref xml:method
   * @brief Checks whether or not an element has any children
   */
  public boolean hasChildren() {
    checkChildren();
    return children.length > 0;
  }


  /**
   * Put the names of all children into an array. Same as looping through
   * each child and calling getName() on each XMLElement.
   *
   * @webref xml:method
   * @brief Returns the names of all children as an array
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
   *
   * @webref xml:method
   * @brief Returns an array containing all child elements
   */
  public XML[] getChildren() {
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
   *
   * @webref xml:method
   * @brief Returns the child element with the specified index value or path
   */
  public XML getChild(int index) {
    checkChildren();
    return children[index];
  }


  /**
   * Get a child by its name or path.
   *
   * @param name element name or path/to/element
   * @return the first matching element
   */
  public XML getChild(String name) {
    if (name.indexOf('/') != -1) {
      return getChildRecursive(PApplet.split(name, '/'), 0);
    }
    int childCount = getChildCount();
    for (int i = 0; i < childCount; i++) {
      XML kid = getChild(i);
      String kidName = kid.getName();
      if (kidName != null && kidName.equals(name)) {
        return kid;
      }
    }
    return null;
  }


  /**
   * Internal helper function for getChild(String).
   *
   * @param items result of splitting the query on slashes
   * @param offset where in the items[] array we're currently looking
   * @return matching element or null if no match
   * @author processing.org
   */
  protected XML getChildRecursive(String[] items, int offset) {
    // if it's a number, do an index instead
    if (Character.isDigit(items[offset].charAt(0))) {
      XML kid = getChild(Integer.parseInt(items[offset]));
      if (offset == items.length-1) {
        return kid;
      } else {
        return kid.getChildRecursive(items, offset+1);
      }
    }
    int childCount = getChildCount();
    for (int i = 0; i < childCount; i++) {
      XML kid = getChild(i);
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
   *
   * @param name element name or path/to/element
   * @return array of child elements that match
   * @author processing.org
   */
  public XML[] getChildren(String name) {
    if (name.indexOf('/') != -1) {
      return getChildrenRecursive(PApplet.split(name, '/'), 0);
    }
    // if it's a number, do an index instead
    // (returns a single element array, since this will be a single match
    if (Character.isDigit(name.charAt(0))) {
      return new XML[] { getChild(Integer.parseInt(name)) };
    }
    int childCount = getChildCount();
    XML[] matches = new XML[childCount];
    int matchCount = 0;
    for (int i = 0; i < childCount; i++) {
      XML kid = getChild(i);
      String kidName = kid.getName();
      if (kidName != null && kidName.equals(name)) {
        matches[matchCount++] = kid;
      }
    }
    return (XML[]) PApplet.subset(matches, 0, matchCount);
  }


  protected XML[] getChildrenRecursive(String[] items, int offset) {
    if (offset == items.length-1) {
      return getChildren(items[offset]);
    }
    XML[] matches = getChildren(items[offset]);
    XML[] outgoing = new XML[0];
    for (int i = 0; i < matches.length; i++) {
      XML[] kidMatches = matches[i].getChildrenRecursive(items, offset+1);
      outgoing = (XML[]) PApplet.concat(outgoing, kidMatches);
    }
    return outgoing;
  }


  /**
   * @webref xml:method
   * @brief Appends a new child to the element
   */
  public XML addChild(String tag) {
    Document document = node.getOwnerDocument();
    Node newChild = document.createElement(tag);
    return appendChild(newChild);
  }


  public XML addChild(XML child) {
    Document document = node.getOwnerDocument();
    Node newChild = document.importNode(child.getNode(), true);
    return appendChild(newChild);
  }


  /** Internal handler to add the node structure. */
  protected XML appendChild(Node newNode) {
    node.appendChild(newNode);
    XML newbie = new XML(this, newNode);
    if (children != null) {
      children = (XML[]) PApplet.concat(children, new XML[] { newbie });
    }
    return newbie;
  }


  /**
   * @webref xml:method
   * @brief Removes the specified child
   */
  public void removeChild(XML kid) {
    node.removeChild(kid.node);
    children = null;  // TODO not efficient
  }


  /** Remove whitespace nodes. */
  public void trim() {
////    public static boolean isWhitespace(XML xml) {
////      if (xml.node.getNodeType() != Node.TEXT_NODE)
////        return false;
////      Matcher m = whitespace.matcher(xml.node.getNodeValue());
////      return m.matches();
////    }
//    trim(this);
//  }
//
//
//  protected void trim() {
    checkChildren();
    int index = 0;
    for (int i = 0; i < children.length; i++) {
      if (i != index) {
        children[index] = children[i];
      }
      Node childNode = children[i].getNode();
      if (childNode.getNodeType() != Node.TEXT_NODE ||
          children[i].getContent().trim().length() > 0) {
        children[i].trim();
        index++;
      }
    }
    if (index != children.length) {
      children = (XML[]) PApplet.subset(children, 0, index);
    }
  }


  /**
   * Returns the number of attributes.
   *
   * @webref xml:method
   * @brief Counts the specified element's number of attributes
   */
  public int getAttributeCount() {
    return node.getAttributes().getLength();
  }


  /**
   * Get a list of the names for all of the attributes for this node.
   *
   * @webref xml:method
   * @brief Returns a list of names of all attributes as an array
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
   *
   * @webref xml:method
   * @brief Checks whether or not an element has the specified attribute
   */
  public boolean hasAttribute(String name) {
    return (node.getAttributes().getNamedItem(name) != null);
  }


  /**
   * Returns the value of an attribute.
   *
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
   * @return the value, or defaultValue if the attribute does not exist.
   */
//  public String getAttribute(String name, String defaultValue) {
//    Node attr = node.getAttributes().getNamedItem(name);
//    return (attr == null) ? defaultValue : attr.getNodeValue();
//  }


  /**
   * @webref xml:method
   * @brief Gets the content of an element as a String
   */
  public String getString(String name) {
    return getString(name, null);
  }


  public String getString(String name, String defaultValue) {
    Node attr = node.getAttributes().getNamedItem(name);
    return (attr == null) ? defaultValue : attr.getNodeValue();
  }


  /**
   * @webref xml:method
   * @brief Sets the content of an element as a String
   */
  public void setString(String name, String value) {
    ((Element) node).setAttribute(name, value);
  }


  /**
   * @webref xml:method
   * @brief Gets the content of an element as an int
   */
  public int getInt(String name) {
    return getInt(name, 0);
  }


  /**
   * @webref xml:method
   * @brief Sets the content of an element as an int
   */
  public void setInt(String name, int value) {
    setString(name, String.valueOf(value));
  }


  /**
   * Returns the value of an attribute.
   *
   * @param name the non-null full name of the attribute.
   * @param defaultValue the default value of the attribute.
   * @return the value, or defaultValue if the attribute does not exist.
   */
  public int getInt(String name, int defaultValue) {
    String value = getString(name);
    return (value == null) ? defaultValue : Integer.parseInt(value);
  }


  /**
   * Returns the value of an attribute, or zero if not present.
   *
   * @webref xml:method
   * @brief Gets the content of an element as a float
   */
  public float getFloat(String name) {
    return getFloat(name, 0);
  }


  /**
   * Returns the value of an attribute.
   *
   * @param name the non-null full name of the attribute.
   * @param defaultValue the default value of the attribute.
   * @return the value, or defaultValue if the attribute does not exist.
   */
  public float getFloat(String name, float defaultValue) {
    String value = getString(name);
    return (value == null) ? defaultValue : Float.parseFloat(value);
  }


  /**
   * @webref xml:method
   * @brief Sets the content of an element as a float
   */
  public void setFloat(String name, float value) {
    setString(name, String.valueOf(value));
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


  public void setDouble(String name, double value) {
    setString(name, String.valueOf(value));
  }


  /**
   * Return the #PCDATA content of the element. If the element has a
   * combination of #PCDATA content and child elements, the #PCDATA
   * sections can be retrieved as unnamed child objects. In this case,
   * this method returns null.
   *
   * @webref xml:method
   * @brief Gets the content of an element
   * @return the content.
   */
  public String getContent() {
    return node.getTextContent();
  }


  /**
   * @webref xml:method
   * @brief Sets the content of an element
   */
  public void setContent(String text) {
    node.setTextContent(text);
  }


  public String format(int indent) {
    try {
      DOMSource dumSource = new DOMSource(node);
      // entities = doctype.getEntities()
      TransformerFactory tf = TransformerFactory.newInstance();
      Transformer transformer = tf.newTransformer();
      // if this is the root, output the decl, if not, hide it
      if (indent == -1 || parent != null) {
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
      } else {
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
      }
//      transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, "sample.dtd");
      transformer.setOutputProperty(OutputKeys.METHOD, "xml");
//      transformer.setOutputProperty(OutputKeys.CDATA_SECTION_ELEMENTS, "yes");  // huh?

//      transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC,
//          "-//W3C//DTD XHTML 1.0 Transitional//EN");
//      transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM,
//          "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd");

//      transformer.setOutputProperty(OutputKeys.ENCODING,"ISO-8859-1");
//      transformer.setOutputProperty(OutputKeys.ENCODING,"UTF8");
      transformer.setOutputProperty(OutputKeys.ENCODING,"UTF-8");
//      transformer.setOutputProperty(OutputKeys.CDATA_SECTION_ELEMENTS
      // indent by default, but sometimes this needs to be turned off
      if (indent != 0) {
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", String.valueOf(indent));
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
      }
//      Properties p = transformer.getOutputProperties();
//      for (Object key : p.keySet()) {
//        System.out.println(key + " -> " + p.get(key));
//      }

      StringWriter sw = new StringWriter();
      StreamResult sr = new StreamResult(sw);
      transformer.transform(dumSource, sr);
      return sw.toString();

    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }


  @Override
  /** Return the XML data as a single line, with no DOCTYPE declaration. */
  public String toString() {
    return format(-1);
  }
}
