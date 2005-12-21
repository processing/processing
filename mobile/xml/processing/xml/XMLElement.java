package processing.xml;

import processing.core.*;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParser;
import java.io.*;
import java.util.*;

/**
 * Part of the Mobile Processing project - http://mobile.processing.org
 *
 * Copyright (c) 2004-05 Francis Li
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General
 * Public License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA  02111-1307  USA
 *
 * @author  Francis Li
 */
public class XMLElement {
    /** String holding the kind of the Element (the tagname) */
    private String      name;
    /** Holds the values and keys of the elements attributes. */    
    private Hashtable   attributes;    
    /** Vector keeping the children of this Element */
    private Vector      children;
    /** true if this element is empty */
    private boolean     empty = true;
    /** true if this element is a pcdata section */
    private boolean     pcdata;    
    /** Holds the parent of this Element */
    private XMLElement  parent;
    
    /** Initializes a new XMLElement with the given name, attributes and children.
     * @param name String, name of the element
     * @param attributes Hashtable, attributes for the element, with names and values
     * @param children Vector, the children of the element
     * @param pcdata boolean, true if the element is a pcdata section
     */
    private XMLElement (String name, Hashtable attributes, Vector children, boolean pcdata) {
        this.name = name;
        this.attributes = attributes;
        this.children = children;
        this.pcdata = pcdata;
    }
    
    /** Initializes a new XMLElement with the given name.
     * @param name String, name of the element
     * @param pcdata boolean, true if the element is a pcdata section
     */
    public XMLElement(String name, boolean pcdata) {
        this(name, new Hashtable(), new Vector(), pcdata);
    }

    /** Initializes a new XMLElement with the given name, attributes and children.
     * @param name String, name of the element
     * @param attributes Hashtable, attributes for the element, with names and values
     * @param children Vector, the children of the element
     */
    public XMLElement (String name, Hashtable attributes, Vector children) {
        this(name, attributes, children, false);
    }
    
    /** Initializes a XMLElement with the given name, but without children and attributes.
     * @param name String, name of the element
     */
    public XMLElement (String name) {
        this(name, new Hashtable(), new Vector());
    }

    /** Initializes a XMLElement with the given name and children.
     * @param name String, name of the element
     * @param children Vector, children of the element
     */
    public XMLElement (String name, Vector children) {
        this(name, new Hashtable(), children);
    }

    /** Initializes a new XMLElement with the given name and attributes.
     * @param name String, name of the element
     * @param attributes Hashtable, attributes of the element, with names and values
     */
    public XMLElement (String name, Hashtable attributes){
        this(name, attributes, new Vector());
    }
           
    /** Checks if a Vector has Content
     * @param toCheck Vector
     * @return boolean
     */
    private boolean has(Vector toCheck) {
        if (toCheck.isEmpty() || toCheck == null) {
            return false;
        } else {
            return true;
        }
    }
    
    /** Use this method to check if the XMLElement is a PCDATA section. 
     * A PCDATA section is text element.
     * @return boolean, true if the XMLElement is a PCDATA section.
     * @example proxml_isPCDATA
     * @shortdesc Checks if a XMLElement is a PCDATA section
     * @related XMLElement
     * @related getElement ( )
     */
    public boolean isPCDATA() {
        return pcdata;
    }
    
    /** Use this method to get the name of a XMLElement. If the XMLElement is a 
     * PCDATA section getElement() gives you its text.
     * @return String, the name of the element or if it is a PCDATA element the text
     * @example proxml_getElement
     * @shortdesc Use this method to get the name of a XMLElement. 
     * @related XMLElement
     * @related isPCDATA ( )
     */
    public String getElement() {
        return name;
    }

    /** Returns a String Array with all attribute names of an Element. Use 
     * getAttribute() to get the value for an attribute.
     * @return String[], Array with the Attributes of an Element
     * @example proxml_getAttributes
     * @shortdesc Returns a String Array with all attribute names of an Element.
     * @related XMLElement
     * @related getAttribute ( )
     * @related getIntAttribute ( )
     * @related getFloatAttribute ( )
     * @related hasAttributes ( )
     * @related hasAttribute ( )
     * @related countAttributes ( )
     * @related addAttribute ( )
     */
    public String[] getAttributes() {
        int length = attributes.size();
        String[] result = new String[length];
        Enumeration e = attributes.keys();
        for (int i = 0; i < length; i++) {
            result[i] = (String) e.nextElement();
        }
        return result;
    }

    /** Use getAttribute() to get the value of an attribute as a string. If your are
     * sure, the value is an int or a float value you can also use getIntAttribute() or 
     * getFloatAttribute() to get the numeric value without a cast.
     * @param key String, the name of the attribute you want the value of
     * @return String, the value to the given attribute
     * @example proxml_getAttributes
     * @shortdesc Returns the value of a given attribute.
     * @related XMLElement
     * @related getAttributes ( )
     * @related getIntAttribute ( )
     * @related getFloatAttribute ( )
     * @related hasAttributes ( )
     * @related hasAttribute ( )
     * @related countAttributes ( )
     * @related addAttribute ( )
     */
    public String getAttribute(String key) {
        String result = (String)attributes.get(key);
        if (result == null) throw new RuntimeException("XMLElement " + this.name + " has no attribute " + key);
        return result;
    }
    
    /** Use getIntAttribute() to get the value of an attribute as int value. You 
     * can only use this method on attributes that are numeric, otherwise you get 
     * a InvalidAttributeException. 
     * @param key String, the name of the attribute you want the value of
     * @return int, the value of the attribute
     * @example proxml
     * @shortdesc Use getIntAttribute() to get the value of an attribute as int value.
     * @related XMLElement
     * @related getAttributes ( )
     * @related getAttribute ( )
     * @related getFloatAttribute ( )
     * @related hasAttributes ( )
     * @related hasAttribute ( )
     * @related countAttributes ( )
     * @related addAttribute ( )
     */
    public int getIntAttribute(String key){
        String attributeValue = (String)attributes.get(key);
        if (attributeValue==null) throw new RuntimeException("XMLElement " + this.name + " has no attribute " + key);
        try{
            return Integer.parseInt((String)attributes.get(key));
        }catch (NumberFormatException e){
            throw new RuntimeException("XMLElement " + this.name + " has no attribute " + key);
        }
    }
    
    /** Use this method to check if the XMLElement has attributes.
     * @return boolean, true if the XMLElement has attributes
     * @example proxml_hasAttributes
     * @related XMLElement
     * @related getAttributes ( )
     * @related getAttribute ( )
     * @related getIntAttribute ( )
     * @related getFloatAttribute ( )
     * @related hasAttribute ( )
     * @related countAttributes ( )
     * @related addAttribute ( )
     */
    public boolean hasAttributes() {
        return !attributes.isEmpty();
    }

    /** This method checks if the XMLElement has the given Attribute.
     * @param key String, attribute you want to check
     * @return boolean, true if the XMLElement has the given attribute
     * @example proxml_hasAttribute
     * @related getAttributes ( )
     * @related getAttribute ( )
     * @related getIntAttribute ( )
     * @related getFloatAttribute ( )
     * @related hasAttributes ( )
     * @related countAttributes ( )
     * @related addAttribute ( )
     */
    public boolean hasAttribute(String key) {
        return attributes.containsKey(key);
    }
    
    /** Use this method to count the attributes of a XMLElement.
     * @return int, the number of attributes
     * @example proxml_countAttributes
     * @related XMLElement
     * @related getAttributes ( )
     * @related getAttribute ( )
     * @related getIntAttribute ( )
     * @related getFloatAttribute ( )
     * @related hasAttributes ( )
     * @related hasAttribute ( )
     * @related addAttribute ( )
     */
    public int countAttributes() {
        return attributes.size();
    }

    /** With addAttribute() you can add attributes to a XMLElement. The value
     * of attribute can be a String a float or an int. 
     * @param key String, name of the attribute
     * @param value String, int or float: value of the attribute
     * @example proxml
     * @shortdesc With addAttribute() you can add attributes to a XMLElement.
     * @related XMLElement
     * @related getAttributes ( )
     * @related getAttribute ( )
     * @related getIntAttribute ( )
     * @related getFloatAttribute ( )
     * @related hasAttributes ( )
     * @related hasAttribute ( )
     * @related countAttributes ( )
     */
    public void addAttribute(String key, String value) {
        if (isPCDATA()) throw new RuntimeException("You can't add the attribute " + key + " to a PCDATA section");
        attributes.put(key, value);
    }
    
    public void addAttribute(String key, int value) {
        addAttribute(key, Integer.toString(value));
    }

    /** With getParent() you can get the parent of a XMLElement. If the 
     * XMLElement is the root element it returns null.
     * @return XMLElement, the parent of the XMLElement or null 
     * if the XMLElement is the root element
     * @example proxml_getParent
     * @shortdesc With getParent() you can get the parent of a XMLElement.
     * @related XMLElement
     * @related addChild ( )
     * @related countChildren ( )
     * @related getChild ( )
     * @related getChildren ( )
     * @related hasChildren ( )
     */
    public XMLElement getParent() {
        return parent;
    }

    /** Use getChildren() to get an array with all children of an element. 
     * @return XMLElement[], an Array of child elements
     * @example proxml_getChildren
     * @related XMLElement
     * @related addChild ( )
     * @related countChildren ( )
     * @related getChild ( )
     * @related getParent ( )
     * @related hasChildren ( )
     */
    public XMLElement[] getChildren() {
        XMLElement[] result = new XMLElement[children.size()];
        children.copyInto(result);
        return result;
    }
    
    /** Use getChild() to get a certain child element of a XMLElement. 
     * With countAllChildren() you get the number of all children.
     * @param i int, number of the child
     * @return XMLElement, the child
     * @example proxml
     * @shortdesc Use getChild() to get a certain child element of a XMLElement.
     * @related XMLElement
     * @related addChild ( )
     * @related countChildren ( )
     * @related getChildren ( )
     * @related getParent ( )
     * @related hasChildren ( )
     */
    public XMLElement getChild(int i){
        return ((XMLElement)children.elementAt(i));
    }

    /** Looks if the XMLElement has Children.
     * @return boolean, true if the XMLElement has children
     * @example proxml_hasChildren
     * @related XMLElement
     * @related addChild ( )
     * @related countChildren ( )
     * @related getChild ( )
     * @related getChildren ( )
     * @related getParent ( )
     */
    public boolean hasChildren() {
        return has(children);
    }
    
    /** With countChildren() you get the number of children of a XMLElement.
     * @return int, the number of children
     * @example proxml
     * @related XMLElement
     * @related addChild ( )
     * @related getChild ( )
     * @related getChildren ( )
     * @related getParent ( )
     * @related hasChildren ( )
     */
    public int countChildren(){
        return children.size();
    }

    /** Use addChild() to add a Child to a XMLElement.
     * @param element XMLElement, element you want to add as child
     * @example proxml
     * @related XMLElement
     * @related addChild ( )
     * @related countChildren ( )
     * @related getChildren ( )
     * @related getParent ( )
     * @related hasChildren ( )
     */
    public void addChild(XMLElement element){
        empty = false;
        element.parent = this;
        children.addElement(element);
    }

    /** 
     * @param element XMLElement, element you want to add as child
     * @param position int, position where you want to insert the element
     */
    public void addChild(XMLElement element, int position){
        empty = false;
        element.parent = this;
        children.insertElementAt(element, position);
    }

    /** Use getDepth to get the maximum depth of an Element to one of its leaves.
     * @return int, the maximum depth of an Element to one of its leaves
     * @example proxml_getDepth
     * @related XMLElement
     * @related countAllChildren ( )
     * @related countAttributes ( )
     * @related countChildren ( )
     */
    public int getDepth() {
        int result = 0;
        XMLElement[] children = getChildren();
        for (int i = 0; i < children.length; i++) {
            result = Math.max(result, children[i].getDepth());
        }
        return 1 + result;
    }

    /** This method returns the number of all decendents of a XMLElement.
     * @return int, the number of all decendents of an Element
     * @example proxml_countAllChildren
     * @related XMLElement
     * @related getParent ( )
     * @related getDepth ( )
     * @related countAttributes ( )
     * @related countChildren ( )
     */
    public int countAllChildren() {
        int result = 0;
        XMLElement[] children = getChildren();
        for (int i = 0; i < children.length; i++) {
            result += children[i].countAllChildren();
        }
        return 1 + result;
    }

    /** Gives back a vector with elements of the given kind being decendents of this Element
     * @param element String
     * @return Vector
     * @invisible
     */
    public Vector getSpecificElements(String element) {
        Vector result = new Vector();
        XMLElement[] children = getChildren();
        for (int i = 0; i < children.length; i++) {
            if (!children[i].isPCDATA()) {
                Enumeration e = children[i].getSpecificElements(element).elements();
                while (e.hasMoreElements()) {
                    result.addElement(e.nextElement());
                }
            }
            if (children[i].name.equals(element)) {
                result.addElement(children[i]);
            }
        }
        return result;
    }
    
    /** Use toString to get the String representation of a XMLElement. The 
     * Methode gives you the starttag with the name and its attributes, or its text if 
     * it is a PCDATA section.
     * @return String, String representation of the XMLElement
     * @example proxml_toString
     * @shortdesc Use toString to get the String representation of a XMLElement.
     * @related XMLElement
     * @related printElementTree ( )
     * @related getElement ( )
     * @related isPCDATA ( )
     */
    public String toString() {
        final StringBuffer result = new StringBuffer();
        if(isPCDATA()){
            char c;
            for (int i = 0, length = name.length(); i < length; i++) {
                c = name.charAt(i);
                switch (c) {
                    case '&':
                        result.append("&amp;");
                        break;
                    case '<':
                        result.append("&lt;");
                        break;
                    case '>':
                        result.append("&gt;");
                        break;
                    case '"':
                        result.append("&quot;");
                        break;
                    case '\'':
                        result.append("&apos;");
                        break;
                    default:
                        result.append(c);
                }
            }
        } else {
            result.append("<");
            result.append(name);
            for (Enumeration e = attributes.keys(); e.hasMoreElements(); ) {
                String key = (String) e.nextElement();
                result.append(" ");
                result.append(key);
                result.append("=\"");
                result.append(attributes.get(key));
                result.append("\"");
            }
            if (empty) {
                result.append("/>");
            } else {
                result.append(">");
            }
        }
        return result.toString();
    }
    
    /** Use this method for a simple trace of the XML structure, beginning from a certain 
     * XMLElement.
     * @param dist String, for formating
     * @example proxml_printElementTree
     * @related XMLElement
     * @related toString ( )
     */
    public void printElementTree(String dist) {
        printElementTree(System.out, "", dist);
    }
    
    /**
     * Prints the tree of this Element with the given distance and start string.
     * @param start String
     * @param dist String
     * @related XMLElement
     */
    public void printElementTree(String start, String dist) {
        printElementTree(System.out, start, dist);
    }

    /**
     * Prints the tree of this Element with the given distance
     * @param dist String
     * @param output PrintWriter
      * @related XMLElement
     */
    void printElementTree(PrintStream output, String dist) {
        printElementTree(output, "", dist);
    }

    /**
     * Prints the tree of this Element with the given distance and start string.
     * @param start String
     * @param dist String
     * @param output PrintWriter
     */
    void printElementTree(PrintStream output, String start, String dist) {
        output.println(start + this);
        for (int i = 0; i < children.size(); i++) {
            ((XMLElement)children.elementAt(i)).printElementTree(output,start + dist, dist);
        }
        if(!empty){
            output.println(start + "</" + name + ">");
        }
    }
}
