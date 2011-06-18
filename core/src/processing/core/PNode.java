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


/**
 * This is the base class used for the Processing XML library, 
 * representing a single node of an XML tree.
 */
public interface PNode extends Serializable {

  /**
   * Returns the parent element. This method returns null for the root
   * element.
   */
  public PNode getParent();

    
  /**
   * Returns the full name (i.e. the name including an eventual namespace
   * prefix) of the element.
   * @return the name, or null if the element only contains #PCDATA.
   */
  public String getName();

    
  /**
   * Returns the name of the element (without namespace prefix).
   * @return the name, or null if the element only contains #PCDATA.
   */
  public String getLocalName();


  /**
   * Returns the number of children.
   * @return the count.
   */
  public int getChildCount();

    
  /**
   * Put the names of all children into an array. Same as looping through 
   * each child and calling getName() on each XMLElement.
   */
  public String[] listChildren();
    
    
  /**
   * Returns an array containing all the child elements.
   */
  public PNode[] getChildren();


  /**
   * Quick accessor for an element at a particular index.
   * @author processing.org
   */
  public PNode getChild(int index);


  /**
   * Get a child by its name or path.
   * @param name element name or path/to/element
   * @return the first matching element
   */
  public PNode getChild(String name);


  /**
   * Get any children that match this name or path. Similar to getChild(),
   * but will grab multiple matches rather than only the first.
   * @param name element name or path/to/element
   * @return array of child elements that match
   * @author processing.org
   */
  public PNode[] getChildren(String name);


  /**
   * Returns the number of attributes.
   */
  public int getAttributeCount();

  
  /**
   * Get a list of the names for all of the attributes for this node.
   */
  public String[] listAttributes();


  /**
   * Returns whether an attribute exists.
   */
  public boolean hasAttribute(String name);

  
  public String getString(String name);


  public String getString(String name, String defaultValue);


  public int getInt(String name);


  /**
   * Returns the value of an attribute.
   *
   * @param name the non-null full name of the attribute.
   * @param defaultValue the default value of the attribute.
   *
   * @return the value, or defaultValue if the attribute does not exist.
   */
  public int getInt(String name, int defaultValue);


  /**
   * Returns the value of an attribute, or zero if not present.
   */
  public float getFloat(String name);
  
  
  /**
   * Returns the value of an attribute.
   *
   * @param name the non-null full name of the attribute.
   * @param defaultValue the default value of the attribute.
   *
   * @return the value, or defaultValue if the attribute does not exist.
   */
  public float getFloat(String name, float defaultValue);


  public double getDouble(String name);

  
  /**
   * Returns the value of an attribute.
   *
   * @param name the non-null full name of the attribute.
   * @param defaultValue the default value of the attribute.
   *
   * @return the value, or defaultValue if the attribute does not exist.
   */
  public double getDouble(String name, double defaultValue);


  /**
   * Return the #PCDATA content of the element. If the element has a
   * combination of #PCDATA content and child elements, the #PCDATA
   * sections can be retrieved as unnamed child objects. In this case,
   * this method returns null.
   *
   * @return the content.
   */
  public String getContent();

  
  public String toString();
  
  
  public String toString(boolean indent);
}