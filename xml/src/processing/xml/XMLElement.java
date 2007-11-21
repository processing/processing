/*
  This code is from NanoXML 2.2.3 Lite
  Copyright (C) 2000-2002 Marc De Scheemaecker, All Rights Reserved.

  Additional modifications Copyright (c) 2006 Ben Fry and Casey Reas
  to make the code better-suited for use with Processing.

  Original license notice from Marc De Scheemaecker:

  This software is provided 'as-is', without any express or implied warranty.
  In no event will the authors be held liable for any damages arising from the
  use of this software.

  Permission is granted to anyone to use this software for any purpose,
  including commercial applications, and to alter it and redistribute it
  freely, subject to the following restrictions:

  1. The origin of this software must not be misrepresented; you must not
     claim that you wrote the original software. If you use this software in
     a product, an acknowledgment in the product documentation would be
     appreciated but is not required.

  2. Altered source versions must be plainly marked as such, and must not be
     misrepresented as being the original software.

  3. This notice may not be removed or altered from any source distribution.
*/

package processing.xml;


import java.io.ByteArrayOutputStream;
import java.io.CharArrayReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import processing.core.PApplet;


/**
 * XMLElement is a representation of an XML object. The object is able to parse
 * XML code.
 * <P>
 * The code is from NanoXML 2.2.3 Lite from Marc De Scheemaecker. His project is
 * online at <A HREF="http://nanoxml.cyberelf.be/">http://nanoxml.cyberelf.be/</A>.
 * Some interfaces to the code have been changed, and the package naming has been
 * altered, however it should be clear that this code is almost entirely his
 * work, with no connection to the Processing project.
 * <P>
 * Alterations/additions for the Processing library: <UL>
 * <LI>a slightly different model for getChild() and getChildren()
 * <LI>addition of getChild(name or path) and getChildren(name or path)
 * <LI>slight changes to support the SVG library
 * </UL>
 * The intent of this library (with regard to Processing) is to provide an
 * extremely simple (and compact) means of reading and writing XML data from a
 * sketch. As such, this is not a full-featured library for handling XML data.
 * For those who need it, more sophisticated libraries are available, and there
 * are no plans to add significant new features to this library.
 * <P>
 * The encoding parameter inside XML files is ignored, all files are
 * parsed with UTF-8 encoding (as of release 0134).
 * <CODE>
 * import processing.xml.*;
 *
 * XMLElement xml = new XMLElement(this, "filename.xml");
 * int childCount = xml.getChildCount();
 * for (int i = 0; i < childCount; i++) {
 *   XMLElement kid = xml.getChild(i);
 *   float attr = kid.getFloatAttribute("some-attribute");
 *   println("some-attribute is " + attr);
 * }
 * </CODE>
 * @author Marc De Scheemaecker
 * @author Ben Fry
 */
public class XMLElement
{
    static final boolean DEBUG = false;


    /**
     * The attributes given to the element.
     *
     * <dl><dt><b>Invariants:</b></dt><dd>
     * <ul><li>The field can be empty.
     *     <li>The field is never <code>null</code>.
     *     <li>The keys and the values are strings.
     * </ul></dd></dl>
     */
    private Hashtable attributes;


    /**
     * Child elements of the element.
     *
     * <dl><dt><b>Invariants:</b></dt><dd>
     * <ul><li>The field can be empty.
     *     <li>The field is never <code>null</code>.
     *     <li>The elements are instances of <code>XMLElement</code>
     *         or a subclass of <code>XMLElement</code>.
     * </ul></dd></dl>
     */
    private Vector children;


    /**
     * The name of the element.
     *
     * <dl><dt><b>Invariants:</b></dt><dd>
     * <ul><li>The field is <code>null</code> iff the element is not
     *         initialized by either parse or setName.
     *     <li>If the field is not <code>null</code>, it's not empty.
     *     <li>If the field is not <code>null</code>, it contains a valid
     *         XML identifier.
     * </ul></dd></dl>
     */
    private String name;


    /**
     * The #PCDATA content of the object.
     *
     * <dl><dt><b>Invariants:</b></dt><dd>
     * <ul><li>The field is <code>null</code> iff the element is not a
     *         #PCDATA element.
     *     <li>The field can be any string, including the empty string.
     * </ul></dd></dl>
     */
    private String content;


    /**
     * Conversion table for &amp;...; entities. The keys are the entity names
     * without the &amp; and ; delimiters.
     *
     * <dl><dt><b>Invariants:</b></dt><dd>
     * <ul><li>The field is never <code>null</code>.
     *     <li>The field always contains the following associations:
     *         "lt"&nbsp;=&gt;&nbsp;"&lt;", "gt"&nbsp;=&gt;&nbsp;"&gt;",
     *         "quot"&nbsp;=&gt;&nbsp;"\"", "apos"&nbsp;=&gt;&nbsp;"'",
     *         "amp"&nbsp;=&gt;&nbsp;"&amp;"
     *     <li>The keys are strings
     *     <li>The values are char arrays
     * </ul></dd></dl>
     */
    private Hashtable entities;


    /**
     * Use this to leave unknown entities unchanged. This is a necessity because
     * <!ENTITY ...> lines are not currently parsed, and cause dumb problems
     * with Illustrator SVG files. [fry]
     */
    private boolean ignoreUnknownEntities = true;


    /**
     * For attributes such as NOWRAP that aren't set equal to anything,
     * parse without giving an error, and set their contents to an empty String.
     */
    private boolean ignoreMissingAttributes = true;


    /**
     * The line number where the element starts.
     *
     * <dl><dt><b>Invariants:</b></dt><dd>
     * <ul><li><code>lineNumber &gt= 0</code>
     * </ul></dd></dl>
     */
    private int lineNumber;


    /**
     * <code>true</code> if the case of the element and attribute names
     * are case insensitive.
     */
    private boolean ignoreCase;


    /**
     * <code>true</code> if the leading and trailing whitespace of #PCDATA
     * sections have to be ignored.
     */
    private boolean ignoreWhitespace;


    /**
     * Character read too much.
     * This character provides push-back functionality to the input reader
     * without having to use a PushbackReader.
     * If there is no such character, this field is '\0'.
     */
    private char charReadTooMuch;


    /**
     * The reader provided by the caller of the parse method.
     *
     * <dl><dt><b>Invariants:</b></dt><dd>
     * <ul><li>The field is not <code>null</code> while the parse method
     *         is running.
     * </ul></dd></dl>
     */
    private Reader reader;


    /**
     * The current line number in the source content.
     *
     * <dl><dt><b>Invariants:</b></dt><dd>
     * <ul><li>parserLineNumber &gt; 0 while the parse method is running.
     * </ul></dd></dl>
     */
    private int parserLineNumber;


    /**
     * Creates and initializes a new XML element.
     * Calling the construction is equivalent to:
     * <ul><code>new XMLElement(new Hashtable(), false, true)
     * </code></ul>
     *
     * <dl><dt><b>Postconditions:</b></dt><dd>
     * <ul><li>countChildren() => 0
     *     <li>enumerateChildren() => empty enumeration
     *     <li>enumeratePropertyNames() => empty enumeration
     *     <li>getChildren() => empty vector
     *     <li>getContent() => ""
     *     <li>getLineNumber() => 0
     *     <li>getName() => null
     * </ul></dd></dl>
     *
     * @see processing.xml.XMLElement#XMLElement(java.util.Hashtable)
     *         XMLElement(Hashtable)
     * @see processing.xml.XMLElement#XMLElement(boolean)
     * @see processing.xml.XMLElement#XMLElement(java.util.Hashtable,boolean)
     *         XMLElement(Hashtable, boolean)
     */
    public XMLElement()
    {
        this(new Hashtable(), false, true, true);
    }


    /**
     * Creates and initializes a new XML element.
     * Calling the construction is equivalent to:
     * <ul><code>new XMLElement(entities, false, true)
     * </code></ul>
     *
     * @param entities
     *     The entity conversion table.
     *
     * </dl><dl><dt><b>Preconditions:</b></dt><dd>
     * <ul><li><code>entities != null</code>
     * </ul></dd></dl>
     *
     * <dl><dt><b>Postconditions:</b></dt><dd>
     * <ul><li>countChildren() => 0
     *     <li>enumerateChildren() => empty enumeration
     *     <li>enumeratePropertyNames() => empty enumeration
     *     <li>getChildren() => empty vector
     *     <li>getContent() => ""
     *     <li>getLineNumber() => 0
     *     <li>getName() => null
     * </ul></dd></dl><dl>
     *
     * @see processing.xml.XMLElement#XMLElement()
     * @see processing.xml.XMLElement#XMLElement(boolean)
     * @see processing.xml.XMLElement#XMLElement(java.util.Hashtable,boolean)
     *         XMLElement(Hashtable, boolean)
     */
    public XMLElement(Hashtable entities)
    {
        this(entities, false, true, true);
    }


    /**
     * Creates and initializes a new XML element.
     * Calling the construction is equivalent to:
     * <ul><code>new XMLElement(new Hashtable(), skipLeadingWhitespace, true)
     * </code></ul>
     *
     * @param skipLeadingWhitespace
     *     <code>true</code> if leading and trailing whitespace in PCDATA
     *     content has to be removed.
     *
     * </dl><dl><dt><b>Postconditions:</b></dt><dd>
     * <ul><li>countChildren() => 0
     *     <li>enumerateChildren() => empty enumeration
     *     <li>enumeratePropertyNames() => empty enumeration
     *     <li>getChildren() => empty vector
     *     <li>getContent() => ""
     *     <li>getLineNumber() => 0
     *     <li>getName() => null
     * </ul></dd></dl><dl>
     *
     * @see processing.xml.XMLElement#XMLElement()
     * @see processing.xml.XMLElement#XMLElement(java.util.Hashtable)
     *         XMLElement(Hashtable)
     * @see processing.xml.XMLElement#XMLElement(java.util.Hashtable,boolean)
     *         XMLElement(Hashtable, boolean)
     */
    public XMLElement(boolean skipLeadingWhitespace)
    {
        this(new Hashtable(), skipLeadingWhitespace, true, true);
    }


    /**
     * Creates and initializes a new XML element.
     * Calling the construction is equivalent to:
     * <ul><code>new XMLElement(entities, skipLeadingWhitespace, true)
     * </code></ul>
     *
     * @param entities
     *     The entity conversion table.
     * @param skipLeadingWhitespace
     *     <code>true</code> if leading and trailing whitespace in PCDATA
     *     content has to be removed.
     *
     * </dl><dl><dt><b>Preconditions:</b></dt><dd>
     * <ul><li><code>entities != null</code>
     * </ul></dd></dl>
     *
     * <dl><dt><b>Postconditions:</b></dt><dd>
     * <ul><li>countChildren() => 0
     *     <li>enumerateChildren() => empty enumeration
     *     <li>enumeratePropertyNames() => empty enumeration
     *     <li>getChildren() => empty vector
     *     <li>getContent() => ""
     *     <li>getLineNumber() => 0
     *     <li>getName() => null
     * </ul></dd></dl><dl>
     *
     * @see processing.xml.XMLElement#XMLElement()
     * @see processing.xml.XMLElement#XMLElement(boolean)
     * @see processing.xml.XMLElement#XMLElement(java.util.Hashtable)
     *         XMLElement(Hashtable)
     */
    public XMLElement(Hashtable entities,
                      boolean   skipLeadingWhitespace)
    {
        this(entities, skipLeadingWhitespace, true, true);
    }


    /**
     * Creates and initializes a new XML element.
     *
     * @param entities
     *     The entity conversion table.
     * @param skipLeadingWhitespace
     *     <code>true</code> if leading and trailing whitespace in PCDATA
     *     content has to be removed.
     * @param ignoreCase
     *     <code>true</code> if the case of element and attribute names have
     *     to be ignored.
     *
     * </dl><dl><dt><b>Preconditions:</b></dt><dd>
     * <ul><li><code>entities != null</code>
     * </ul></dd></dl>
     *
     * <dl><dt><b>Postconditions:</b></dt><dd>
     * <ul><li>countChildren() => 0
     *     <li>enumerateChildren() => empty enumeration
     *     <li>enumeratePropertyNames() => empty enumeration
     *     <li>getChildren() => empty vector
     *     <li>getContent() => ""
     *     <li>getLineNumber() => 0
     *     <li>getName() => null
     * </ul></dd></dl><dl>
     *
     * @see processing.xml.XMLElement#XMLElement()
     * @see processing.xml.XMLElement#XMLElement(boolean)
     * @see processing.xml.XMLElement#XMLElement(java.util.Hashtable)
     *         XMLElement(Hashtable)
     * @see processing.xml.XMLElement#XMLElement(java.util.Hashtable,boolean)
     *         XMLElement(Hashtable, boolean)
     */
    public XMLElement(Hashtable entities,
                      boolean   skipLeadingWhitespace,
                      boolean   ignoreCase)
    {
        this(entities, skipLeadingWhitespace, true, ignoreCase);
    }


    /**
     * Creates and initializes a new XML element.
     * <P>
     * This constructor should <I>only</I> be called from
     * {@link #createAnotherElement() createAnotherElement}
     * to create child elements.
     *
     * @param entities
     *     The entity conversion table.
     * @param skipLeadingWhitespace
     *     <code>true</code> if leading and trailing whitespace in PCDATA
     *     content has to be removed.
     * @param fillBasicConversionTable
     *     <code>true</code> if the basic entities need to be added to
     *     the entity list.
     * @param ignoreCase
     *     <code>true</code> if the case of element and attribute names have
     *     to be ignored.
     *
     * </dl><dl><dt><b>Preconditions:</b></dt><dd>
     * <ul><li><code>entities != null</code>
     *     <li>if <code>fillBasicConversionTable == false</code>
     *         then <code>entities</code> contains at least the following
     *         entries: <code>amp</code>, <code>lt</code>, <code>gt</code>,
     *         <code>apos</code> and <code>quot</code>
     * </ul></dd></dl>
     *
     * <dl><dt><b>Postconditions:</b></dt><dd>
     * <ul><li>countChildren() => 0
     *     <li>enumerateChildren() => empty enumeration
     *     <li>enumeratePropertyNames() => empty enumeration
     *     <li>getChildren() => empty vector
     *     <li>getContent() => ""
     *     <li>getLineNumber() => 0
     *     <li>getName() => null
     * </ul></dd></dl><dl>
     *
     * @see processing.xml.XMLElement#createAnotherElement()
     */
    protected XMLElement(Hashtable entities,
                         boolean   skipLeadingWhitespace,
                         boolean   fillBasicConversionTable,
                         boolean   ignoreCase)
    {
        this.ignoreWhitespace = skipLeadingWhitespace;
        this.ignoreCase = ignoreCase;
        this.name = null;
        this.content = "";
        this.attributes = new Hashtable();
        this.children = new Vector();
        this.entities = entities;
        this.lineNumber = 0;
        Enumeration en = this.entities.keys();
        while (en.hasMoreElements()) {
            Object key = en.nextElement();
            Object value = this.entities.get(key);
            if (value instanceof String) {
                value = ((String) value).toCharArray();
                this.entities.put(key, value);
            }
        }
        if (fillBasicConversionTable) {
            this.entities.put("amp", new char[] { '&' });
            this.entities.put("quot", new char[] { '"' });
            this.entities.put("apos", new char[] { '\'' });
            this.entities.put("lt", new char[] { '<' });
            this.entities.put("gt", new char[] { '>' });
            //this.entities.put("nbsp", new char[] { 0xA0 });
        }
    }


    /**
     * Begin parsing XML data passed in from a PApplet. This code
     * wraps exception handling, for more advanced exception handling,
     * use the constructor that takes a Reader or InputStream.
     * @author processing.org
     * @param filename
     * @param parent
     */
    public XMLElement(PApplet parent, String filename) {
        this();
        try {
            Reader r = parent.createReader(filename);
            //if (r == null) return;
            parseFromReader(r);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public XMLElement(Reader r) throws IOException {
        this();
        parseFromReader(r);
    }


    public XMLElement(String s) {
        try {
                parseString(s);
        } catch (XMLParseException e) {
                e.printStackTrace();
        }
    }


    public XMLElement(InputStream input) throws IOException {
        this(new InputStreamReader(input, "UTF-8"));
    }


    /**
     * Adds a child element.
     *
     * @param child
     *     The child element to add.
     *
     * </dl><dl><dt><b>Preconditions:</b></dt><dd>
     * <ul><li><code>child != null</code>
     *     <li><code>child.getName() != null</code>
     *     <li><code>child</code> does not have a parent element
     * </ul></dd></dl>
     *
     * <dl><dt><b>Postconditions:</b></dt><dd>
     * <ul><li>countChildren() => old.countChildren() + 1
     *     <li>enumerateChildren() => old.enumerateChildren() + child
     *     <li>getChildren() => old.enumerateChildren() + child
     * </ul></dd></dl><dl>
     *
     * @see processing.xml.XMLElement#countChildren()
     * @see processing.xml.XMLElement#enumerateChildren()
     * @see processing.xml.XMLElement#getChildren()
     * @see processing.xml.XMLElement#removeChild(processing.xml.XMLElement)
     *         removeChild(XMLElement)
     */
    public void addChild(XMLElement child)
    {
        this.children.addElement(child);
    }


    /**
     * Adds or modifies an attribute.
     *
     * @param name
     *     The name of the attribute.
     * @param value
     *     The value of the attribute.
     *
     * </dl><dl><dt><b>Preconditions:</b></dt><dd>
     * <ul><li><code>name != null</code>
     *     <li><code>name</code> is a valid XML identifier
     *     <li><code>value != null</code>
     * </ul></dd></dl>
     *
     * <dl><dt><b>Postconditions:</b></dt><dd>
     * <ul><li>enumerateAttributeNames()
     *         => old.enumerateAttributeNames() + name
     *     <li>getAttribute(name) => value
     * </ul></dd></dl><dl>
     *
     * @see processing.xml.XMLElement#setDoubleAttribute(java.lang.String, double)
     *         setDoubleAttribute(String, double)
     * @see processing.xml.XMLElement#setIntAttribute(java.lang.String, int)
     *         setIntAttribute(String, int)
     * @see processing.xml.XMLElement#enumerateAttributeNames()
     * @see processing.xml.XMLElement#getAttribute(java.lang.String)
     *         getAttribute(String)
     * @see processing.xml.XMLElement#getAttribute(java.lang.String, java.lang.Object)
     *         getAttribute(String, Object)
     * @see processing.xml.XMLElement#getAttribute(java.lang.String,
     *                                      java.util.Hashtable,
     *                                      java.lang.String, boolean)
     *         getAttribute(String, Hashtable, String, boolean)
     * @see processing.xml.XMLElement#getStringAttribute(java.lang.String)
     *         getStringAttribute(String)
     * @see processing.xml.XMLElement#getStringAttribute(java.lang.String,
     *                                            java.lang.String)
     *         getStringAttribute(String, String)
     * @see processing.xml.XMLElement#getStringAttribute(java.lang.String,
     *                                            java.util.Hashtable,
     *                                            java.lang.String, boolean)
     *         getStringAttribute(String, Hashtable, String, boolean)
     */
    public void setAttribute(String name,
                             Object value)
    {
        if (this.ignoreCase) {
            name = name.toUpperCase();
        }
        this.attributes.put(name, value.toString());
    }


    /**
     * Adds or modifies an attribute.
     *
     * @param name
     *     The name of the attribute.
     * @param value
     *     The value of the attribute.
     *
     * </dl><dl><dt><b>Preconditions:</b></dt><dd>
     * <ul><li><code>name != null</code>
     *     <li><code>name</code> is a valid XML identifier
     * </ul></dd></dl>
     *
     * <dl><dt><b>Postconditions:</b></dt><dd>
     * <ul><li>enumerateAttributeNames()
     *         => old.enumerateAttributeNames() + name
     *     <li>getIntAttribute(name) => value
     * </ul></dd></dl><dl>
     *
     * @see processing.xml.XMLElement#setDoubleAttribute(java.lang.String, double)
     *         setDoubleAttribute(String, double)
     * @see processing.xml.XMLElement#setAttribute(java.lang.String, java.lang.Object)
     *         setAttribute(String, Object)
     * @see processing.xml.XMLElement#removeAttribute(java.lang.String)
     *         removeAttribute(String)
     * @see processing.xml.XMLElement#enumerateAttributeNames()
     * @see processing.xml.XMLElement#getIntAttribute(java.lang.String)
     *         getIntAttribute(String)
     * @see processing.xml.XMLElement#getIntAttribute(java.lang.String, int)
     *         getIntAttribute(String, int)
     * @see processing.xml.XMLElement#getIntAttribute(java.lang.String,
     *                                         java.util.Hashtable,
     *                                         java.lang.String, boolean)
     *         getIntAttribute(String, Hashtable, String, boolean)
     */
    public void setIntAttribute(String name,
                                int    value)
    {
        if (this.ignoreCase) {
            name = name.toUpperCase();
        }
        this.attributes.put(name, Integer.toString(value));
    }


    /**
     * Adds or modifies an attribute.
     *
     * @param name
     *     The name of the attribute.
     * @param value
     *     The value of the attribute.
     *
     * </dl><dl><dt><b>Preconditions:</b></dt><dd>
     * <ul><li><code>name != null</code>
     *     <li><code>name</code> is a valid XML identifier
     * </ul></dd></dl>
     *
     * <dl><dt><b>Postconditions:</b></dt><dd>
     * <ul><li>enumerateAttributeNames()
     *         => old.enumerateAttributeNames() + name
     *     <li>getDoubleAttribute(name) => value
     * </ul></dd></dl><dl>
     *
     * @see processing.xml.XMLElement#setIntAttribute(java.lang.String, int)
     *         setIntAttribute(String, int)
     * @see processing.xml.XMLElement#setAttribute(java.lang.String, java.lang.Object)
     *         setAttribute(String, Object)
     * @see processing.xml.XMLElement#removeAttribute(java.lang.String)
     *         removeAttribute(String)
     * @see processing.xml.XMLElement#enumerateAttributeNames()
     * @see processing.xml.XMLElement#getDoubleAttribute(java.lang.String)
     *         getDoubleAttribute(String)
     * @see processing.xml.XMLElement#getDoubleAttribute(java.lang.String, double)
     *         getDoubleAttribute(String, double)
     * @see processing.xml.XMLElement#getDoubleAttribute(java.lang.String,
     *                                            java.util.Hashtable,
     *                                            java.lang.String, boolean)
     *         getDoubleAttribute(String, Hashtable, String, boolean)
     */
    public void setDoubleAttribute(String name,
                                   double value)
    {
        if (this.ignoreCase) {
            name = name.toUpperCase();
        }
        this.attributes.put(name, Double.toString(value));
    }


    /**
     * Get the number of children for this element.
     * @return number of children
     */
    public int getChildCount()
    {
        return this.children.size();
    }


    /**
     * Enumerates the attribute names.
     *
     * <dl><dt><b>Postconditions:</b></dt><dd>
     * <ul><li><code>result != null</code>
     * </ul></dd></dl>
     *
     * @see processing.xml.XMLElement#setDoubleAttribute(java.lang.String, double)
     *         setDoubleAttribute(String, double)
     * @see processing.xml.XMLElement#setIntAttribute(java.lang.String, int)
     *         setIntAttribute(String, int)
     * @see processing.xml.XMLElement#setAttribute(java.lang.String, java.lang.Object)
     *         setAttribute(String, Object)
     * @see processing.xml.XMLElement#removeAttribute(java.lang.String)
     *         removeAttribute(String)
     * @see processing.xml.XMLElement#getAttribute(java.lang.String)
     *         getAttribute(String)
     * @see processing.xml.XMLElement#getAttribute(java.lang.String, java.lang.Object)
     *         getAttribute(String, String)
     * @see processing.xml.XMLElement#getAttribute(java.lang.String,
     *                                      java.util.Hashtable,
     *                                      java.lang.String, boolean)
     *         getAttribute(String, Hashtable, String, boolean)
     * @see processing.xml.XMLElement#getStringAttribute(java.lang.String)
     *         getStringAttribute(String)
     * @see processing.xml.XMLElement#getStringAttribute(java.lang.String,
     *                                            java.lang.String)
     *         getStringAttribute(String, String)
     * @see processing.xml.XMLElement#getStringAttribute(java.lang.String,
     *                                            java.util.Hashtable,
     *                                            java.lang.String, boolean)
     *         getStringAttribute(String, Hashtable, String, boolean)
     * @see processing.xml.XMLElement#getIntAttribute(java.lang.String)
     *         getIntAttribute(String)
     * @see processing.xml.XMLElement#getIntAttribute(java.lang.String, int)
     *         getIntAttribute(String, int)
     * @see processing.xml.XMLElement#getIntAttribute(java.lang.String,
     *                                         java.util.Hashtable,
     *                                         java.lang.String, boolean)
     *         getIntAttribute(String, Hashtable, String, boolean)
     * @see processing.xml.XMLElement#getDoubleAttribute(java.lang.String)
     *         getDoubleAttribute(String)
     * @see processing.xml.XMLElement#getDoubleAttribute(java.lang.String, double)
     *         getDoubleAttribute(String, double)
     * @see processing.xml.XMLElement#getDoubleAttribute(java.lang.String,
     *                                            java.util.Hashtable,
     *                                            java.lang.String, boolean)
     *         getDoubleAttribute(String, Hashtable, String, boolean)
     * @see processing.xml.XMLElement#getBooleanAttribute(java.lang.String,
     *                                             java.lang.String,
     *                                             java.lang.String, boolean)
     *         getBooleanAttribute(String, String, String, boolean)
     */
    public Enumeration enumerateAttributeNames()
    {
        return this.attributes.keys();
    }


    /**
     * Enumerates the child elements.
     *
     * <dl><dt><b>Postconditions:</b></dt><dd>
     * <ul><li><code>result != null</code>
     * </ul></dd></dl>
     *
     * @see processing.xml.XMLElement#addChild(processing.xml.XMLElement)
     *         addChild(XMLElement)
     * @see processing.xml.XMLElement#countChildren()
     * @see processing.xml.XMLElement#getChildren()
     * @see processing.xml.XMLElement#removeChild(processing.xml.XMLElement)
     *         removeChild(XMLElement)
     */
    public Enumeration enumerateChildren()
    {
        return this.children.elements();
    }


    /**
     * Returns the child elements as an array. The code currently uses
     * a Vector internally, but that will likely change in a future release
     * because arrays are more efficient.
     *
     * <dl><dt><b>Postconditions:</b></dt><dd>
     * <ul><li><code>result != null</code>
     * </ul></dd></dl>
     *
     * @see processing.xml.XMLElement#addChild(processing.xml.XMLElement)
     *         addChild(XMLElement)
     * @see processing.xml.XMLElement#countChildren()
     * @see processing.xml.XMLElement#enumerateChildren()
     * @see processing.xml.XMLElement#removeChild(processing.xml.XMLElement)
     *         removeChild(XMLElement)
     */
    public XMLElement[] getChildren() {
        int childCount = getChildCount();
        XMLElement[] kids = new XMLElement[childCount];
        children.copyInto(kids);
        return kids;
    }


    /**
     * Quick accessor for an element at a particular index.
     * @author processing.org
     */
    public XMLElement getChild(int which) {
        return (XMLElement) children.elementAt(which);
    }


    /**
     * Get a child by its name or path.
     * @param name element name or path/to/element
     * @return
     * @author processing.org
     */
    public XMLElement getChild(String name) {
        if (name.indexOf('/') != -1) {
                return getChild(PApplet.split(name, '/'), 0);
        }
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
                XMLElement kid = getChild(i);
                if (kid.getName().equals(name)) {
                        return kid;
                }
        }
        return null;
    }


    protected XMLElement getChild(String[] items, int offset) {
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
                XMLElement kid = getChild(i);
                if (kid.getName().equals(items[offset])) {
                        if (offset == items.length-1) {
                                return kid;
                        } else {
                                return kid.getChild(items, offset+1);
                        }
                }
        }
        return null;
    }


    /**
     * Get any children that match this name or path. Similar to getChild(),
     * but will grab multiple matches rather than only the first.
     * @param name element name or path/to/element
     * @return
     * @author processing.org
     */
    public XMLElement[] getChildren(String name) {
        if (name.indexOf('/') != -1) {
                return getChildren(PApplet.split(name, '/'), 0);
        }
        int childCount = getChildCount();
        XMLElement[] matches = new XMLElement[childCount];
        int matchCount = 0;
        for (int i = 0; i < childCount; i++) {
                XMLElement kid = getChild(i);
                if (kid.getName().equals(name)) {
                        matches[matchCount++] = kid;
                }
        }
        return (XMLElement[]) PApplet.subset(matches, 0, matchCount);
    }


    protected XMLElement[] getChildren(String[] items, int offset) {
        if (offset == items.length-1) {
                return getChildren(items[offset]);
        }
        XMLElement[] matches = getChildren(items[offset]);
        XMLElement[] outgoing = new XMLElement[0];
        for (int i = 0; i < matches.length; i++) {
                XMLElement[] kidMatches = matches[i].getChildren(items, offset+1);
                outgoing = (XMLElement[]) PApplet.concat(outgoing, kidMatches);
        }
        return outgoing;
    }


    /**
     * Returns the PCDATA content of the object. If there is no such content,
     * <CODE>null</CODE> is returned.
     *
     * @see processing.xml.XMLElement#setContent(java.lang.String)
     *         setContent(String)
     */
    public String getContent()
    {
        return this.content;
    }


    /**
     * Returns the line Number in the source data on which the element is found.
     * This method returns <code>0</code> there is no associated source data.
     *
     * <dl><dt><b>Postconditions:</b></dt><dd>
     * <ul><li><code>result >= 0</code>
     * </ul></dd></dl>
     */
    public int getLineNumber()
    {
        return this.lineNumber;
    }


    /**
     * Convenience function to check whether an attribute is available.
     * @author fry
     * @param name name of the attribute
     * @return true if the attribute is non-null
     */
    public boolean hasAttribute(String aname) {
        return (getAttribute(aname) != null);
    }


    /**
     * Returns an attribute of the element.
     * If the attribute doesn't exist, <code>null</code> is returned.
     *
     * @param name The name of the attribute.
     *
     * </dl><dl><dt><b>Preconditions:</b></dt><dd>
     * <ul><li><code>name != null</code>
     *     <li><code>name</code> is a valid XML identifier
     * </ul></dd></dl><dl>
     *
     * @see processing.xml.XMLElement#setAttribute(java.lang.String, java.lang.Object)
     *         setAttribute(String, Object)
     * @see processing.xml.XMLElement#removeAttribute(java.lang.String)
     *         removeAttribute(String)
     * @see processing.xml.XMLElement#enumerateAttributeNames()
     * @see processing.xml.XMLElement#getAttribute(java.lang.String, java.lang.Object)
     *         getAttribute(String, Object)
     * @see processing.xml.XMLElement#getAttribute(java.lang.String,
     *                                      java.util.Hashtable,
     *                                      java.lang.String, boolean)
     *         getAttribute(String, Hashtable, String, boolean)
     */
    public Object getAttribute(String aname)
    {
        return this.getAttribute(aname, null);
    }


    /**
     * Returns an attribute of the element.
     * If the attribute doesn't exist, <code>defaultValue</code> is returned.
     *
     * @param aname         The name of the attribute.
     * @param defaultValue Key to use if the attribute is missing.
     *
     * </dl><dl><dt><b>Preconditions:</b></dt><dd>
     * <ul><li><code>name != null</code>
     *     <li><code>name</code> is a valid XML identifier
     * </ul></dd></dl><dl>
     *
     * @see processing.xml.XMLElement#setAttribute(java.lang.String, java.lang.Object)
     *         setAttribute(String, Object)
     * @see processing.xml.XMLElement#removeAttribute(java.lang.String)
     *         removeAttribute(String)
     * @see processing.xml.XMLElement#enumerateAttributeNames()
     * @see processing.xml.XMLElement#getAttribute(java.lang.String)
     *         getAttribute(String)
     * @see processing.xml.XMLElement#getAttribute(java.lang.String,
     *                                      java.util.Hashtable,
     *                                      java.lang.String, boolean)
     *         getAttribute(String, Hashtable, String, boolean)
     */
    public Object getAttribute(String aname,
                               Object defaultValue)
    {
        if (this.ignoreCase) {
            aname = aname.toUpperCase();
        }
        Object value = this.attributes.get(aname);
        if (value == null) {
            value = defaultValue;
        }
        return value;
    }


    /**
     * Returns an attribute by looking up a key in a hashtable.
     * If the attribute doesn't exist, the value corresponding to defaultKey
     * is returned.
     * <P>
     * As an example, if valueSet contains the mapping <code>"one" =>
     * "1"</code>
     * and the element contains the attribute <code>attr="one"</code>, then
     * <code>getAttribute("attr", mapping, defaultKey, false)</code> returns
     * <code>"1"</code>.
     *
     * @param name
     *     The name of the attribute.
     * @param valueSet
     *     Hashtable mapping keys to values.
     * @param defaultKey
     *     Key to use if the attribute is missing.
     * @param allowLiterals
     *     <code>true</code> if literals are valid.
     *
     * </dl><dl><dt><b>Preconditions:</b></dt><dd>
     * <ul><li><code>name != null</code>
     *     <li><code>name</code> is a valid XML identifier
     *     <li><code>valueSet</code> != null
     *     <li>the keys of <code>valueSet</code> are strings
     * </ul></dd></dl><dl>
     *
     * @see processing.xml.XMLElement#setAttribute(java.lang.String, java.lang.Object)
     *         setAttribute(String, Object)
     * @see processing.xml.XMLElement#removeAttribute(java.lang.String)
     *         removeAttribute(String)
     * @see processing.xml.XMLElement#enumerateAttributeNames()
     * @see processing.xml.XMLElement#getAttribute(java.lang.String)
     *         getAttribute(String)
     * @see processing.xml.XMLElement#getAttribute(java.lang.String, java.lang.Object)
     *         getAttribute(String, Object)
     */
    public Object getAttribute(String    name,
                               Hashtable valueSet,
                               String    defaultKey,
                               boolean   allowLiterals)
    {
        if (this.ignoreCase) {
            name = name.toUpperCase();
        }
        Object key = this.attributes.get(name);
        Object result;
        if (key == null) {
            key = defaultKey;
        }
        result = valueSet.get(key);
        if (result == null) {
            if (allowLiterals) {
                result = key;
            } else {
                throw this.invalidValue(name, (String) key);
            }
        }
        return result;
    }


    /**
     * Returns an attribute of the element.
     * If the attribute doesn't exist, <code>null</code> is returned.
     *
     * @param name The name of the attribute.
     *
     * </dl><dl><dt><b>Preconditions:</b></dt><dd>
     * <ul><li><code>name != null</code>
     *     <li><code>name</code> is a valid XML identifier
     * </ul></dd></dl><dl>
     *
     * @see processing.xml.XMLElement#setAttribute(java.lang.String, java.lang.Object)
     *         setAttribute(String, Object)
     * @see processing.xml.XMLElement#removeAttribute(java.lang.String)
     *         removeAttribute(String)
     * @see processing.xml.XMLElement#enumerateAttributeNames()
     * @see processing.xml.XMLElement#getStringAttribute(java.lang.String,
     *                                            java.lang.String)
     *         getStringAttribute(String, String)
     * @see processing.xml.XMLElement#getStringAttribute(java.lang.String,
     *                                            java.util.Hashtable,
     *                                            java.lang.String, boolean)
     *         getStringAttribute(String, Hashtable, String, boolean)
     */
    public String getStringAttribute(String name)
    {
        return this.getStringAttribute(name, null);
    }


    /**
     * Returns an attribute of the element.
     * If the attribute doesn't exist, <code>defaultValue</code> is returned.
     *
     * @param name         The name of the attribute.
     * @param defaultValue Key to use if the attribute is missing.
     *
     * </dl><dl><dt><b>Preconditions:</b></dt><dd>
     * <ul><li><code>name != null</code>
     *     <li><code>name</code> is a valid XML identifier
     * </ul></dd></dl><dl>
     *
     * @see processing.xml.XMLElement#setAttribute(java.lang.String, java.lang.Object)
     *         setAttribute(String, Object)
     * @see processing.xml.XMLElement#removeAttribute(java.lang.String)
     *         removeAttribute(String)
     * @see processing.xml.XMLElement#enumerateAttributeNames()
     * @see processing.xml.XMLElement#getStringAttribute(java.lang.String)
     *         getStringAttribute(String)
     * @see processing.xml.XMLElement#getStringAttribute(java.lang.String,
     *                                            java.util.Hashtable,
     *                                            java.lang.String, boolean)
     *         getStringAttribute(String, Hashtable, String, boolean)
     */
    public String getStringAttribute(String name,
                                     String defaultValue)
    {
        return (String) this.getAttribute(name, defaultValue);
    }


    /**
     * Returns an attribute by looking up a key in a hashtable.
     * If the attribute doesn't exist, the value corresponding to defaultKey
     * is returned.
     * <P>
     * As an example, if valueSet contains the mapping <code>"one" =>
     * "1"</code>
     * and the element contains the attribute <code>attr="one"</code>, then
     * <code>getAttribute("attr", mapping, defaultKey, false)</code> returns
     * <code>"1"</code>.
     *
     * @param name
     *     The name of the attribute.
     * @param valueSet
     *     Hashtable mapping keys to values.
     * @param defaultKey
     *     Key to use if the attribute is missing.
     * @param allowLiterals
     *     <code>true</code> if literals are valid.
     *
     * </dl><dl><dt><b>Preconditions:</b></dt><dd>
     * <ul><li><code>name != null</code>
     *     <li><code>name</code> is a valid XML identifier
     *     <li><code>valueSet</code> != null
     *     <li>the keys of <code>valueSet</code> are strings
     *     <li>the values of <code>valueSet</code> are strings
     * </ul></dd></dl><dl>
     *
     * @see processing.xml.XMLElement#setAttribute(java.lang.String, java.lang.Object)
     *         setAttribute(String, Object)
     * @see processing.xml.XMLElement#removeAttribute(java.lang.String)
     *         removeAttribute(String)
     * @see processing.xml.XMLElement#enumerateAttributeNames()
     * @see processing.xml.XMLElement#getStringAttribute(java.lang.String)
     *         getStringAttribute(String)
     * @see processing.xml.XMLElement#getStringAttribute(java.lang.String,
     *                                            java.lang.String)
     *         getStringAttribute(String, String)
     */
    public String getStringAttribute(String    name,
                                     Hashtable valueSet,
                                     String    defaultKey,
                                     boolean   allowLiterals)
    {
        return (String) this.getAttribute(name, valueSet, defaultKey,
                                          allowLiterals);
    }


    /**
     * Returns an attribute of the element.
     * If the attribute doesn't exist, <code>0</code> is returned.
     *
     * @param name The name of the attribute.
     *
     * </dl><dl><dt><b>Preconditions:</b></dt><dd>
     * <ul><li><code>name != null</code>
     *     <li><code>name</code> is a valid XML identifier
     * </ul></dd></dl><dl>
     *
     * @see processing.xml.XMLElement#setIntAttribute(java.lang.String, int)
     *         setIntAttribute(String, int)
     * @see processing.xml.XMLElement#enumerateAttributeNames()
     * @see processing.xml.XMLElement#getIntAttribute(java.lang.String, int)
     *         getIntAttribute(String, int)
     * @see processing.xml.XMLElement#getIntAttribute(java.lang.String,
     *                                         java.util.Hashtable,
     *                                         java.lang.String, boolean)
     *         getIntAttribute(String, Hashtable, String, boolean)
     */
    public int getIntAttribute(String name)
    {
        return this.getIntAttribute(name, 0);
    }


    /**
     * Returns an attribute of the element.
     * If the attribute doesn't exist, <code>defaultValue</code> is returned.
     *
     * @param name         The name of the attribute.
     * @param defaultValue Key to use if the attribute is missing.
     *
     * </dl><dl><dt><b>Preconditions:</b></dt><dd>
     * <ul><li><code>name != null</code>
     *     <li><code>name</code> is a valid XML identifier
     * </ul></dd></dl><dl>
     *
     * @see processing.xml.XMLElement#setIntAttribute(java.lang.String, int)
     *         setIntAttribute(String, int)
     * @see processing.xml.XMLElement#enumerateAttributeNames()
     * @see processing.xml.XMLElement#getIntAttribute(java.lang.String)
     *         getIntAttribute(String)
     * @see processing.xml.XMLElement#getIntAttribute(java.lang.String,
     *                                         java.util.Hashtable,
     *                                         java.lang.String, boolean)
     *         getIntAttribute(String, Hashtable, String, boolean)
     */
    public int getIntAttribute(String name,
                               int    defaultValue)
    {
        if (this.ignoreCase) {
            name = name.toUpperCase();
        }
        String value = (String) this.attributes.get(name);
        if (value == null) {
            return defaultValue;
        } else {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                throw this.invalidValue(name, value);
            }
        }
    }


    /**
     * Returns an attribute by looking up a key in a hashtable.
     * If the attribute doesn't exist, the value corresponding to defaultKey
     * is returned.
     * <P>
     * As an example, if valueSet contains the mapping <code>"one" => 1</code>
     * and the element contains the attribute <code>attr="one"</code>, then
     * <code>getIntAttribute("attr", mapping, defaultKey, false)</code> returns
     * <code>1</code>.
     *
     * @param name
     *     The name of the attribute.
     * @param valueSet
     *     Hashtable mapping keys to values.
     * @param defaultKey
     *     Key to use if the attribute is missing.
     * @param allowLiteralNumbers
     *     <code>true</code> if literal numbers are valid.
     *
     * </dl><dl><dt><b>Preconditions:</b></dt><dd>
     * <ul><li><code>name != null</code>
     *     <li><code>name</code> is a valid XML identifier
     *     <li><code>valueSet</code> != null
     *     <li>the keys of <code>valueSet</code> are strings
     *     <li>the values of <code>valueSet</code> are Integer objects
     *     <li><code>defaultKey</code> is either <code>null</code>, a
     *         key in <code>valueSet</code> or an integer.
     * </ul></dd></dl><dl>
     *
     * @see processing.xml.XMLElement#setIntAttribute(java.lang.String, int)
     *         setIntAttribute(String, int)
     * @see processing.xml.XMLElement#enumerateAttributeNames()
     * @see processing.xml.XMLElement#getIntAttribute(java.lang.String)
     *         getIntAttribute(String)
     * @see processing.xml.XMLElement#getIntAttribute(java.lang.String, int)
     *         getIntAttribute(String, int)
     */
    public int getIntAttribute(String    name,
                               Hashtable valueSet,
                               String    defaultKey,
                               boolean   allowLiteralNumbers)
    {
        if (this.ignoreCase) {
            name = name.toUpperCase();
        }
        Object key = this.attributes.get(name);
        Integer result;
        if (key == null) {
            key = defaultKey;
        }
        try {
            result = (Integer) valueSet.get(key);
        } catch (ClassCastException e) {
            throw this.invalidValueSet(name);
        }
        if (result == null) {
            if (! allowLiteralNumbers) {
                throw this.invalidValue(name, (String) key);
            }
            try {
                result = Integer.valueOf((String) key);
            } catch (NumberFormatException e) {
                throw this.invalidValue(name, (String) key);
            }
        }
        return result.intValue();
    }


    /**
     * Method to get a float attribute, identical to getDoubleAttribute(),
     * @see processing.xml.XMLElement#getDoubleAttribute() but added to
     * handle float data as used by Processing. This code is adapted from
     * the identical code in the getDoubleAttribute methods.
     * @author processing.org
     */
    public float getFloatAttribute(String name)
    {
        return getFloatAttribute(name, 0);
    }


    /**
     * Method to get a float attribute, identical to getDoubleAttribute(),
     * @see processing.xml.XMLElement#getDoubleAttribute() but added to
     * handle float data as used by Processing. This code is adapted from
     * the identical code in the getDoubleAttribute methods.
     * @author processing.org
     */
    public float getFloatAttribute(String name,
                                   float defaultValue)
    {
        if (this.ignoreCase) {
            name = name.toUpperCase();
        }
        String value = (String) this.attributes.get(name);
        if (value == null) {
            return defaultValue;
        } else {
            try {
                return Float.valueOf(value).floatValue();
            } catch (NumberFormatException e) {
                throw this.invalidValue(name, value);
            }
        }
    }


    /**
     * Method to get a float attribute, identical to getDoubleAttribute(),
     * @see processing.xml.XMLElement#getDoubleAttribute() but added to
     * handle float data as used by Processing. This code is adapted from
     * the identical code in the getDoubleAttribute methods.
     * @author processing.org
     */
    public float getFloatAttribute(String    name,
                                   Hashtable valueSet,
                                   String    defaultKey,
                                   boolean   allowLiteralNumbers)
    {
        if (this.ignoreCase) {
            name = name.toUpperCase();
        }
        Object key = this.attributes.get(name);
        Float result;
        if (key == null) {
            key = defaultKey;
        }
        try {
            result = (Float) valueSet.get(key);
        } catch (ClassCastException e) {
            throw this.invalidValueSet(name);
        }
        if (result == null) {
            if (! allowLiteralNumbers) {
                throw this.invalidValue(name, (String) key);
            }
            try {
                result = Float.valueOf((String) key);
            } catch (NumberFormatException e) {
                throw this.invalidValue(name, (String) key);
            }
        }
        return result.floatValue();
    }


    /**
     * Returns an attribute of the element.
     * If the attribute doesn't exist, <code>0.0</code> is returned.
     *
     * @param name The name of the attribute.
     *
     * </dl><dl><dt><b>Preconditions:</b></dt><dd>
     * <ul><li><code>name != null</code>
     *     <li><code>name</code> is a valid XML identifier
     * </ul></dd></dl><dl>
     *
     * @see processing.xml.XMLElement#setDoubleAttribute(java.lang.String, double)
     *         setDoubleAttribute(String, double)
     * @see processing.xml.XMLElement#enumerateAttributeNames()
     * @see processing.xml.XMLElement#getDoubleAttribute(java.lang.String, double)
     *         getDoubleAttribute(String, double)
     * @see processing.xml.XMLElement#getDoubleAttribute(java.lang.String,
     *                                            java.util.Hashtable,
     *                                            java.lang.String, boolean)
     *         getDoubleAttribute(String, Hashtable, String, boolean)
     */
    public double getDoubleAttribute(String name)
    {
        return this.getDoubleAttribute(name, 0.);
    }


    /**
     * Returns an attribute of the element.
     * If the attribute doesn't exist, <code>defaultValue</code> is returned.
     *
     * @param name         The name of the attribute.
     * @param defaultValue Key to use if the attribute is missing.
     *
     * </dl><dl><dt><b>Preconditions:</b></dt><dd>
     * <ul><li><code>name != null</code>
     *     <li><code>name</code> is a valid XML identifier
     * </ul></dd></dl><dl>
     *
     * @see processing.xml.XMLElement#setDoubleAttribute(java.lang.String, double)
     *         setDoubleAttribute(String, double)
     * @see processing.xml.XMLElement#enumerateAttributeNames()
     * @see processing.xml.XMLElement#getDoubleAttribute(java.lang.String)
     *         getDoubleAttribute(String)
     * @see processing.xml.XMLElement#getDoubleAttribute(java.lang.String,
     *                                            java.util.Hashtable,
     *                                            java.lang.String, boolean)
     *         getDoubleAttribute(String, Hashtable, String, boolean)
     */
    public double getDoubleAttribute(String name,
                                     double defaultValue)
    {
        if (this.ignoreCase) {
            name = name.toUpperCase();
        }
        String value = (String) this.attributes.get(name);
        if (value == null) {
            return defaultValue;
        } else {
            try {
                return Double.valueOf(value).doubleValue();
            } catch (NumberFormatException e) {
                throw this.invalidValue(name, value);
            }
        }
    }


    /**
     * Returns an attribute by looking up a key in a hashtable.
     * If the attribute doesn't exist, the value corresponding to defaultKey
     * is returned.
     * <P>
     * As an example, if valueSet contains the mapping <code>"one" =&gt;
     * 1.0</code>
     * and the element contains the attribute <code>attr="one"</code>, then
     * <code>getDoubleAttribute("attr", mapping, defaultKey, false)</code>
     * returns <code>1.0</code>.
     *
     * @param name
     *     The name of the attribute.
     * @param valueSet
     *     Hashtable mapping keys to values.
     * @param defaultKey
     *     Key to use if the attribute is missing.
     * @param allowLiteralNumbers
     *     <code>true</code> if literal numbers are valid.
     *
     * </dl><dl><dt><b>Preconditions:</b></dt><dd>
     * <ul><li><code>name != null</code>
     *     <li><code>name</code> is a valid XML identifier
     *     <li><code>valueSet != null</code>
     *     <li>the keys of <code>valueSet</code> are strings
     *     <li>the values of <code>valueSet</code> are Double objects
     *     <li><code>defaultKey</code> is either <code>null</code>, a
     *         key in <code>valueSet</code> or a double.
     * </ul></dd></dl><dl>
     *
     * @see processing.xml.XMLElement#setDoubleAttribute(java.lang.String, double)
     *         setDoubleAttribute(String, double)
     * @see processing.xml.XMLElement#enumerateAttributeNames()
     * @see processing.xml.XMLElement#getDoubleAttribute(java.lang.String)
     *         getDoubleAttribute(String)
     * @see processing.xml.XMLElement#getDoubleAttribute(java.lang.String, double)
     *         getDoubleAttribute(String, double)
     */
    public double getDoubleAttribute(String    name,
                                     Hashtable valueSet,
                                     String    defaultKey,
                                     boolean   allowLiteralNumbers)
    {
        if (this.ignoreCase) {
            name = name.toUpperCase();
        }
        Object key = this.attributes.get(name);
        Double result;
        if (key == null) {
            key = defaultKey;
        }
        try {
            result = (Double) valueSet.get(key);
        } catch (ClassCastException e) {
            throw this.invalidValueSet(name);
        }
        if (result == null) {
            if (! allowLiteralNumbers) {
                throw this.invalidValue(name, (String) key);
            }
            try {
                result = Double.valueOf((String) key);
            } catch (NumberFormatException e) {
                throw this.invalidValue(name, (String) key);
            }
        }
        return result.doubleValue();
    }


    /**
     * Returns an attribute of the element.
     * If the attribute doesn't exist, <code>defaultValue</code> is returned.
     * If the value of the attribute is equal to <code>trueValue</code>,
     * <code>true</code> is returned.
     * If the value of the attribute is equal to <code>falseValue</code>,
     * <code>false</code> is returned.
     * If the value doesn't match <code>trueValue</code> or
     * <code>falseValue</code>, an exception is thrown.
     *
     * @param name         The name of the attribute.
     * @param trueValue    The value associated with <code>true</code>.
     * @param falseValue   The value associated with <code>true</code>.
     * @param defaultValue Value to use if the attribute is missing.
     *
     * </dl><dl><dt><b>Preconditions:</b></dt><dd>
     * <ul><li><code>name != null</code>
     *     <li><code>name</code> is a valid XML identifier
     *     <li><code>trueValue</code> and <code>falseValue</code>
     *         are different strings.
     * </ul></dd></dl><dl>
     *
     * @see processing.xml.XMLElement#setAttribute(java.lang.String, java.lang.Object)
     *         setAttribute(String, Object)
     * @see processing.xml.XMLElement#removeAttribute(java.lang.String)
     *         removeAttribute(String)
     * @see processing.xml.XMLElement#enumerateAttributeNames()
     */
    public boolean getBooleanAttribute(String  name,
                                       String  trueValue,
                                       String  falseValue,
                                       boolean defaultValue)
    {
        if (this.ignoreCase) {
            name = name.toUpperCase();
        }
        Object value = this.attributes.get(name);
        if (value == null) {
            return defaultValue;
        } else if (value.equals(trueValue)) {
            return true;
        } else if (value.equals(falseValue)) {
            return false;
        } else {
            throw this.invalidValue(name, (String) value);
        }
    }


    /**
     * Returns the name of the element.
     *
     * @see processing.xml.XMLElement#setName(java.lang.String) setName(String)
     */
    public String getName()
    {
        return this.name;
    }


    /**
     * Reads one XML element from a java.io.Reader and parses it.
     *
     * @param reader
     *     The reader from which to retrieve the XML data.
     *
     * </dl><dl><dt><b>Preconditions:</b></dt><dd>
     * <ul><li><code>reader != null</code>
     *     <li><code>reader</code> is not closed
     * </ul></dd></dl>
     *
     * <dl><dt><b>Postconditions:</b></dt><dd>
     * <ul><li>the state of the receiver is updated to reflect the XML element
     *         parsed from the reader
     *     <li>the reader points to the first character following the last
     *         '&gt;' character of the XML element
     * </ul></dd></dl><dl>
     *
     * @throws java.io.IOException
     *     If an error occured while reading the input.
     * @throws processing.xml.XMLParseException
     *     If an error occured while parsing the read data.
     */
    public void parseFromReader(Reader r)
    throws IOException, XMLParseException
    {
        this.parseFromReader(r, /*startingLineNumber*/ 1);
    }


    /**
     * Reads one XML element from a java.io.Reader and parses it.
     *
     * @param reader
     *     The reader from which to retrieve the XML data.
     * @param startingLineNumber
     *     The line number of the first line in the data.
     *
     * </dl><dl><dt><b>Preconditions:</b></dt><dd>
     * <ul><li><code>reader != null</code>
     *     <li><code>reader</code> is not closed
     * </ul></dd></dl>
     *
     * <dl><dt><b>Postconditions:</b></dt><dd>
     * <ul><li>the state of the receiver is updated to reflect the XML element
     *         parsed from the reader
     *     <li>the reader points to the first character following the last
     *         '&gt;' character of the XML element
     * </ul></dd></dl><dl>
     *
     * @throws java.io.IOException
     *     If an error occured while reading the input.
     * @throws processing.xml.XMLParseException
     *     If an error occured while parsing the read data.
     */
    public void parseFromReader(Reader r,
                                int    startingLineNumber)
        throws IOException, XMLParseException
    {
        this.name = null;
        this.content = "";
        this.attributes = new Hashtable();
        this.children = new Vector();
        this.charReadTooMuch = '\0';
        this.reader = r;
        this.parserLineNumber = startingLineNumber;

        for (;;) {
            char ch = this.scanWhitespace();

            if (ch != '<') {
                throw this.expectedInput("<");
            }

            ch = this.readChar();

            if ((ch == '!') || (ch == '?')) {
                this.skipSpecialTag(0);
            } else {
                this.unreadChar(ch);
                this.scanElement(this);
                return;
            }
        }
    }


    /**
     * Reads one XML element from a String and parses it.
     *
     * @param reader
     *     The reader from which to retrieve the XML data.
     *
     * </dl><dl><dt><b>Preconditions:</b></dt><dd>
     * <ul><li><code>string != null</code>
     *     <li><code>string.length() &gt; 0</code>
     * </ul></dd></dl>
     *
     * <dl><dt><b>Postconditions:</b></dt><dd>
     * <ul><li>the state of the receiver is updated to reflect the XML element
     *         parsed from the reader
     * </ul></dd></dl><dl>
     *
     * @throws processing.xml.XMLParseException
     *     If an error occured while parsing the string.
     */
    public void parseString(String string)
        throws XMLParseException
    {
        try {
            this.parseFromReader(new StringReader(string),
                                 /*startingLineNumber*/ 1);
        } catch (IOException e) {
            // Java exception handling suxx
        }
    }


    /**
     * Reads one XML element from a String and parses it.
     *
     * @param reader
     *     The reader from which to retrieve the XML data.
     * @param offset
     *     The first character in <code>string</code> to scan.
     *
     * </dl><dl><dt><b>Preconditions:</b></dt><dd>
     * <ul><li><code>string != null</code>
     *     <li><code>offset &lt; string.length()</code>
     *     <li><code>offset &gt;= 0</code>
     * </ul></dd></dl>
     *
     * <dl><dt><b>Postconditions:</b></dt><dd>
     * <ul><li>the state of the receiver is updated to reflect the XML element
     *         parsed from the reader
     * </ul></dd></dl><dl>
     *
     * @throws processing.xml.XMLParseException
     *     If an error occured while parsing the string.
     */
    public void parseString(String string,
                            int    offset)
        throws XMLParseException
    {
        this.parseString(string.substring(offset));
    }


    /**
     * Reads one XML element from a String and parses it.
     *
     * @param reader
     *     The reader from which to retrieve the XML data.
     * @param offset
     *     The first character in <code>string</code> to scan.
     * @param end
     *     The character where to stop scanning.
     *     This character is not scanned.
     *
     * </dl><dl><dt><b>Preconditions:</b></dt><dd>
     * <ul><li><code>string != null</code>
     *     <li><code>end &lt;= string.length()</code>
     *     <li><code>offset &lt; end</code>
     *     <li><code>offset &gt;= 0</code>
     * </ul></dd></dl>
     *
     * <dl><dt><b>Postconditions:</b></dt><dd>
     * <ul><li>the state of the receiver is updated to reflect the XML element
     *         parsed from the reader
     * </ul></dd></dl><dl>
     *
     * @throws processing.xml.XMLParseException
     *     If an error occured while parsing the string.
     */
    public void parseString(String string,
                            int    offset,
                            int    end)
        throws XMLParseException
    {
        this.parseString(string.substring(offset, end));
    }


    /**
     * Reads one XML element from a String and parses it.
     *
     * @param reader
     *     The reader from which to retrieve the XML data.
     * @param offset
     *     The first character in <code>string</code> to scan.
     * @param end
     *     The character where to stop scanning.
     *     This character is not scanned.
     * @param startingLineNumber
     *     The line number of the first line in the data.
     *
     * </dl><dl><dt><b>Preconditions:</b></dt><dd>
     * <ul><li><code>string != null</code>
     *     <li><code>end &lt;= string.length()</code>
     *     <li><code>offset &lt; end</code>
     *     <li><code>offset &gt;= 0</code>
     * </ul></dd></dl>
     *
     * <dl><dt><b>Postconditions:</b></dt><dd>
     * <ul><li>the state of the receiver is updated to reflect the XML element
     *         parsed from the reader
     * </ul></dd></dl><dl>
     *
     * @throws processing.xml.XMLParseException
     *     If an error occured while parsing the string.
     */
    public void parseString(String string,
                            int    offset,
                            int    end,
                            int    startingLineNumber)
        throws XMLParseException
    {
        string = string.substring(offset, end);
        try {
            this.parseFromReader(new StringReader(string), startingLineNumber);
        } catch (IOException e) {
            // Java exception handling suxx
        }
    }


    /**
     * Reads one XML element from a char array and parses it.
     *
     * @param reader
     *     The reader from which to retrieve the XML data.
     * @param offset
     *     The first character in <code>string</code> to scan.
     * @param end
     *     The character where to stop scanning.
     *     This character is not scanned.
     *
     * </dl><dl><dt><b>Preconditions:</b></dt><dd>
     * <ul><li><code>input != null</code>
     *     <li><code>end &lt;= input.length</code>
     *     <li><code>offset &lt; end</code>
     *     <li><code>offset &gt;= 0</code>
     * </ul></dd></dl>
     *
     * <dl><dt><b>Postconditions:</b></dt><dd>
     * <ul><li>the state of the receiver is updated to reflect the XML element
     *         parsed from the reader
     * </ul></dd></dl><dl>
     *
     * @throws processing.xml.XMLParseException
     *     If an error occured while parsing the string.
     */
    public void parseCharArray(char[] input,
                               int    offset,
                               int    end)
        throws XMLParseException
    {
        this.parseCharArray(input, offset, end, /*startingLineNumber*/ 1);
    }


    /**
     * Reads one XML element from a char array and parses it.
     *
     * @param reader
     *     The reader from which to retrieve the XML data.
     * @param offset
     *     The first character in <code>string</code> to scan.
     * @param end
     *     The character where to stop scanning.
     *     This character is not scanned.
     * @param startingLineNumber
     *     The line number of the first line in the data.
     *
     * </dl><dl><dt><b>Preconditions:</b></dt><dd>
     * <ul><li><code>input != null</code>
     *     <li><code>end &lt;= input.length</code>
     *     <li><code>offset &lt; end</code>
     *     <li><code>offset &gt;= 0</code>
     * </ul></dd></dl>
     *
     * <dl><dt><b>Postconditions:</b></dt><dd>
     * <ul><li>the state of the receiver is updated to reflect the XML element
     *         parsed from the reader
     * </ul></dd></dl><dl>
     *
     * @throws processing.xml.XMLParseException
     *     If an error occured while parsing the string.
     */
    public void parseCharArray(char[] input,
                               int    offset,
                               int    end,
                               int    startingLineNumber)
        throws XMLParseException
    {
        try {
            Reader r = new CharArrayReader(input, offset, end);
            this.parseFromReader(r, startingLineNumber);
        } catch (IOException e) {
            // This exception will never happen.
        }
    }


    /**
     * Removes a child element.
     *
     * @param child
     *     The child element to remove.
     *
     * </dl><dl><dt><b>Preconditions:</b></dt><dd>
     * <ul><li><code>child != null</code>
     *     <li><code>child</code> is a child element of the receiver
     * </ul></dd></dl>
     *
     * <dl><dt><b>Postconditions:</b></dt><dd>
     * <ul><li>countChildren() => old.countChildren() - 1
     *     <li>enumerateChildren() => old.enumerateChildren() - child
     *     <li>getChildren() => old.enumerateChildren() - child
     * </ul></dd></dl><dl>
     *
     * @see processing.xml.XMLElement#addChild(processing.xml.XMLElement)
     *         addChild(XMLElement)
     * @see processing.xml.XMLElement#countChildren()
     * @see processing.xml.XMLElement#enumerateChildren()
     * @see processing.xml.XMLElement#getChildren()
     */
    public void removeChild(XMLElement child)
    {
        this.children.removeElement(child);
    }


    /**
     * Removes an attribute.
     *
     * @param name
     *     The name of the attribute.
     *
     * </dl><dl><dt><b>Preconditions:</b></dt><dd>
     * <ul><li><code>name != null</code>
     *     <li><code>name</code> is a valid XML identifier
     * </ul></dd></dl>
     *
     * <dl><dt><b>Postconditions:</b></dt><dd>
     * <ul><li>enumerateAttributeNames()
     *         => old.enumerateAttributeNames() - name
     *     <li>getAttribute(name) => <code>null</code>
     * </ul></dd></dl><dl>
     *
     * @see processing.xml.XMLElement#enumerateAttributeNames()
     * @see processing.xml.XMLElement#setDoubleAttribute(java.lang.String, double)
     *         setDoubleAttribute(String, double)
     * @see processing.xml.XMLElement#setIntAttribute(java.lang.String, int)
     *         setIntAttribute(String, int)
     * @see processing.xml.XMLElement#setAttribute(java.lang.String, java.lang.Object)
     *         setAttribute(String, Object)
     * @see processing.xml.XMLElement#getAttribute(java.lang.String)
     *         getAttribute(String)
     * @see processing.xml.XMLElement#getAttribute(java.lang.String, java.lang.Object)
     *         getAttribute(String, Object)
     * @see processing.xml.XMLElement#getAttribute(java.lang.String,
     *                                      java.util.Hashtable,
     *                                      java.lang.String, boolean)
     *         getAttribute(String, Hashtable, String, boolean)
     * @see processing.xml.XMLElement#getStringAttribute(java.lang.String)
     *         getStringAttribute(String)
     * @see processing.xml.XMLElement#getStringAttribute(java.lang.String,
     *                                            java.lang.String)
     *         getStringAttribute(String, String)
     * @see processing.xml.XMLElement#getStringAttribute(java.lang.String,
     *                                            java.util.Hashtable,
     *                                            java.lang.String, boolean)
     *         getStringAttribute(String, Hashtable, String, boolean)
     * @see processing.xml.XMLElement#getIntAttribute(java.lang.String)
     *         getIntAttribute(String)
     * @see processing.xml.XMLElement#getIntAttribute(java.lang.String, int)
     *         getIntAttribute(String, int)
     * @see processing.xml.XMLElement#getIntAttribute(java.lang.String,
     *                                         java.util.Hashtable,
     *                                         java.lang.String, boolean)
     *         getIntAttribute(String, Hashtable, String, boolean)
     * @see processing.xml.XMLElement#getDoubleAttribute(java.lang.String)
     *         getDoubleAttribute(String)
     * @see processing.xml.XMLElement#getDoubleAttribute(java.lang.String, double)
     *         getDoubleAttribute(String, double)
     * @see processing.xml.XMLElement#getDoubleAttribute(java.lang.String,
     *                                            java.util.Hashtable,
     *                                            java.lang.String, boolean)
     *         getDoubleAttribute(String, Hashtable, String, boolean)
     * @see processing.xml.XMLElement#getBooleanAttribute(java.lang.String,
     *                                             java.lang.String,
     *                                             java.lang.String, boolean)
     *         getBooleanAttribute(String, String, String, boolean)
     */
    public void removeAttribute(String name)
    {
        if (this.ignoreCase) {
            name = name.toUpperCase();
        }
        this.attributes.remove(name);
    }


    /**
     * Creates a new similar XML element.
     * <P>
     * You should override this method when subclassing XMLElement.
     */
    protected XMLElement createAnotherElement()
    {
        return new XMLElement(this.entities,
                              this.ignoreWhitespace,
                              false,
                              this.ignoreCase);
    }


    /**
     * Changes the content string.
     *
     * @param content
     *     The new content string.
     */
    public void setContent(String content)
    {
        this.content = content;
    }


    /**
     * Changes the name of the element.
     *
     * @param name
     *     The new name.
     *
     * </dl><dl><dt><b>Preconditions:</b></dt><dd>
     * <ul><li><code>name != null</code>
     *     <li><code>name</code> is a valid XML identifier
     * </ul></dd></dl>
     *
     * @see processing.xml.XMLElement#getName()
     */
    public void setName(String name)
    {
        this.name = name;
    }


    /**
     * Writes the XML element to a string.
     *
     * @see processing.xml.XMLElement#write(java.io.Writer) write(Writer)
     */
    public String toString()
    {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            OutputStreamWriter writer = new OutputStreamWriter(out);
            this.write(writer);
            writer.flush();
            return new String(out.toByteArray());
        } catch (IOException e) {
            // Java exception handling suxx
            return super.toString();
        }
    }


    /**
     * Writes the XML element to a writer.
     *
     * @param writer
     *     The writer to write the XML data to.
     *
     * </dl><dl><dt><b>Preconditions:</b></dt><dd>
     * <ul><li><code>writer != null</code>
     *     <li><code>writer</code> is not closed
     * </ul></dd></dl>
     *
     * @throws java.io.IOException
     *      If the data could not be written to the writer.
     *
     * @see processing.xml.XMLElement#toString()
     */
    public void write(Writer writer)
        throws IOException
    {
        if (this.name == null) {
            this.writeEncoded(writer, this.content);
            return;
        }
        writer.write('<');
        writer.write(this.name);
        if (! this.attributes.isEmpty()) {
            Enumeration en = this.attributes.keys();
            while (en.hasMoreElements()) {
                writer.write(' ');
                String key = (String) en.nextElement();
                String value = (String) this.attributes.get(key);
                writer.write(key);
                writer.write('='); writer.write('"');
                this.writeEncoded(writer, value);
                writer.write('"');
            }
        }
        if ((this.content != null) && (this.content.length() > 0)) {
            writer.write('>');
            this.writeEncoded(writer, this.content);
            writer.write('<'); writer.write('/');
            writer.write(this.name);
            writer.write('>');
        } else if (this.children.isEmpty()) {
            writer.write('/'); writer.write('>');
        } else {
            writer.write('>');
            Enumeration en = this.enumerateChildren();
            while (en.hasMoreElements()) {
                XMLElement child = (XMLElement) en.nextElement();
                child.write(writer);
            }
            writer.write('<'); writer.write('/');
            writer.write(this.name);
            writer.write('>');
        }
    }


    /**
     * Writes a string encoded to a writer.
     *
     * @param writer
     *     The writer to write the XML data to.
     * @param str
     *     The string to write encoded.
     *
     * </dl><dl><dt><b>Preconditions:</b></dt><dd>
     * <ul><li><code>writer != null</code>
     *     <li><code>writer</code> is not closed
     *     <li><code>str != null</code>
     * </ul></dd></dl>
     */
    protected void writeEncoded(Writer writer,
                                String str)
        throws IOException
    {
        for (int i = 0; i < str.length(); i += 1) {
            char ch = str.charAt(i);
            switch (ch) {
                case '<':
                    writer.write('&'); writer.write('l'); writer.write('t');
                    writer.write(';');
                    break;
                case '>':
                    writer.write('&'); writer.write('g'); writer.write('t');
                    writer.write(';');
                    break;
                case '&':
                    writer.write('&'); writer.write('a'); writer.write('m');
                    writer.write('p'); writer.write(';');
                    break;
                case '"':
                    writer.write('&'); writer.write('q'); writer.write('u');
                    writer.write('o'); writer.write('t'); writer.write(';');
                    break;
                case '\'':
                    writer.write('&'); writer.write('a'); writer.write('p');
                    writer.write('o'); writer.write('s'); writer.write(';');
                    break;
                default:
                    int unicode = (int) ch;
                    if ((unicode < 32) || (unicode > 126)) {
                        writer.write('&'); writer.write('#');
                        writer.write('x');
                        writer.write(Integer.toString(unicode, 16));
                        writer.write(';');
                    } else {
                        writer.write(ch);
                    }
            }
        }
    }


    /**
     * Scans an identifier from the current reader.
     * The scanned identifier is appended to <code>result</code>.
     *
     * @param result
     *     The buffer in which the scanned identifier will be put.
     *
     * </dl><dl><dt><b>Preconditions:</b></dt><dd>
     * <ul><li><code>result != null</code>
     *     <li>The next character read from the reader is a valid first
     *         character of an XML identifier.
     * </ul></dd></dl>
     *
     * <dl><dt><b>Postconditions:</b></dt><dd>
     * <ul><li>The next character read from the reader won't be an identifier
     *         character.
     * </ul></dd></dl><dl>
     */
    protected void scanIdentifier(StringBuffer result)
        throws IOException
    {
        for (;;) {
            char ch = this.readChar();
            if (((ch < 'A') || (ch > 'Z')) && ((ch < 'a') || (ch > 'z'))
                && ((ch < '0') || (ch > '9')) && (ch != '_') && (ch != '.')
                && (ch != ':') && (ch != '-') && (ch <= '\u007E')) {
                this.unreadChar(ch);
                return;
            }
            result.append(ch);
        }
    }


    /**
     * This method scans an identifier from the current reader.
     *
     * @return the next character following the whitespace.
     */
    protected char scanWhitespace()
        throws IOException
    {
        for (;;) {
            char ch = this.readChar();
            switch (ch) {
                case ' ':
                case '\t':
                case '\n':
                case '\r':
                    break;
                default:
                    return ch;
            }
        }
    }


    /**
     * This method scans an identifier from the current reader.
     * The scanned whitespace is appended to <code>result</code>.
     *
     * @return the next character following the whitespace.
     *
     * </dl><dl><dt><b>Preconditions:</b></dt><dd>
     * <ul><li><code>result != null</code>
     * </ul></dd></dl>
     */
    protected char scanWhitespace(StringBuffer result)
        throws IOException
    {
        for (;;) {
            char ch = this.readChar();
            switch (ch) {
                case ' ':
                case '\t':
                case '\n':
                    result.append(ch);
                    break;
                case '\r':
                    break;
                default:
                    return ch;
            }
        }
    }


    /**
     * This method scans a delimited string from the current reader.
     * The scanned string without delimiters is appended to
     * <code>string</code>.
     *
     * </dl><dl><dt><b>Preconditions:</b></dt><dd>
     * <ul><li><code>string != null</code>
     *     <li>the next char read is the string delimiter
     * </ul></dd></dl>
     */
    protected void scanString(StringBuffer string)
        throws IOException
    {
        /*
        char delimiter = this.readChar();
        if ((delimiter != '\'') && (delimiter != '"')) {
            throw this.expectedInput("' or \"");
        }
        for (;;) {
            char ch = this.readChar();
            if (ch == delimiter) {
                return;
            } else if (ch == '&') {
                //this.resolveEntity(string);
                System.out.println("resolving ent " + delimiter);
                this.resolveEntity(string, delimiter);
            } else {
                string.append(ch);
            }
        }
        */
        // be slightly less strict, and don't require delimiters
        char delimiter = this.readChar();
        if ((delimiter != '\'') && (delimiter != '"')) {
            string.append(delimiter);
            //throw this.expectedInput("' or \"");
            // read until whitespace, a slash, or a closing bracket >
            // don't allow delimiters in this case
            for (;;) {
                char ch = this.readChar();
                if ((ch == ' ') ||
                    (ch == '\t') || (ch == '\n') || (ch == '\r') ||
                    (ch == '/') || (ch == '>')) {
                    unreadChar(ch);
                    return;
                } else {
                    string.append(ch);
                }
            }

        } else {
            for (;;) {
                char ch = this.readChar();
                if (ch == delimiter) {
                    return;
                } else if (ch == '&') {
                    //this.resolveEntity(string);
                    //System.out.println("resolving ent " + delimiter);
                    this.resolveEntity(string, delimiter);
                } else {
                    string.append(ch);
                }
            }
        }
    }


    /**
     * Scans a #PCDATA element. CDATA sections and entities are resolved.
     * The next &lt; char is skipped.
     * The scanned data is appended to <code>data</code>.
     *
     * </dl><dl><dt><b>Preconditions:</b></dt><dd>
     * <ul><li><code>data != null</code>
     * </ul></dd></dl>
     */
    protected void scanPCData(StringBuffer data)
        throws IOException
    {
        for (;;) {
            char ch = this.readChar();
            if (ch == '<') {
                ch = this.readChar();
                if (ch == '!') {
                    this.checkCDATA(data);
                } else {
                    this.unreadChar(ch);
                    return;
                }
            } else if (ch == '&') {
                this.resolveEntity(data);
            } else {
                data.append(ch);
            }
        }
    }


    /**
     * Scans a special tag and if the tag is a CDATA section, append its
     * content to <code>buf</code>.
     *
     * </dl><dl><dt><b>Preconditions:</b></dt><dd>
     * <ul><li><code>buf != null</code>
     *     <li>The first &lt; has already been read.
     * </ul></dd></dl>
     */
    protected boolean checkCDATA(StringBuffer buf)
        throws IOException
    {
        char ch = this.readChar();
        if (ch != '[') {
            this.unreadChar(ch);
            this.skipSpecialTag(0);
            return false;
        } else if (! this.checkLiteral("CDATA[")) {
            this.skipSpecialTag(1); // one [ has already been read
            return false;
        } else {
            int delimiterCharsSkipped = 0;
            while (delimiterCharsSkipped < 3) {
                ch = this.readChar();
                switch (ch) {
                    case ']':
                        if (delimiterCharsSkipped < 2) {
                            delimiterCharsSkipped += 1;
                        } else {
                            buf.append(']');
                            buf.append(']');
                            delimiterCharsSkipped = 0;
                        }
                        break;
                    case '>':
                        if (delimiterCharsSkipped < 2) {
                            for (int i = 0; i < delimiterCharsSkipped; i++) {
                                buf.append(']');
                            }
                            delimiterCharsSkipped = 0;
                            buf.append('>');
                        } else {
                            delimiterCharsSkipped = 3;
                        }
                        break;
                    default:
                        for (int i = 0; i < delimiterCharsSkipped; i += 1) {
                            buf.append(']');
                        }
                        buf.append(ch);
                        delimiterCharsSkipped = 0;
                }
            }
            return true;
        }
    }


    /**
     * Skips a comment.
     *
     * </dl><dl><dt><b>Preconditions:</b></dt><dd>
     * <ul><li>The first &lt;!-- has already been read.
     * </ul></dd></dl>
     */
    protected void skipComment()
        throws IOException
    {
        int dashesToRead = 2;
        while (dashesToRead > 0) {
            char ch = this.readChar();
            if (ch == '-') {
                dashesToRead -= 1;
            } else {
                dashesToRead = 2;
            }
        }
        if (this.readChar() != '>') {
            throw this.expectedInput(">");
        }
    }


    /**
     * Skips a special tag or comment.
     *
     * @param bracketLevel The number of open square brackets ([) that have
     *                     already been read.
     *
     * </dl><dl><dt><b>Preconditions:</b></dt><dd>
     * <ul><li>The first &lt;! has already been read.
     *     <li><code>bracketLevel >= 0</code>
     * </ul></dd></dl>
     */
    protected void skipSpecialTag(int bracketLevel)
        throws IOException
    {
        int tagLevel = 1; // <
        char stringDelimiter = '\0';
        if (bracketLevel == 0) {
            char ch = this.readChar();
            if (ch == '[') {
                bracketLevel += 1;
            } else if (ch == '-') {
                ch = this.readChar();
                if (ch == '[') {
                    bracketLevel += 1;
                } else if (ch == ']') {
                    bracketLevel -= 1;
                } else if (ch == '-') {
                    this.skipComment();
                    return;
                }
            }
        }
        while (tagLevel > 0) {
            char ch = this.readChar();
            if (stringDelimiter == '\0') {
                if ((ch == '"') || (ch == '\'')) {
                    stringDelimiter = ch;
                } else if (bracketLevel <= 0) {
                    if (ch == '<') {
                        tagLevel += 1;
                    } else if (ch == '>') {
                        tagLevel -= 1;
                    }
                }
                if (ch == '[') {
                    bracketLevel += 1;
                } else if (ch == ']') {
                    bracketLevel -= 1;
                }
            } else {
                if (ch == stringDelimiter) {
                    stringDelimiter = '\0';
                }
            }
        }
    }


    /**
     * Scans the data for literal text.
     * Scanning stops when a character does not match or after the complete
     * text has been checked, whichever comes first.
     *
     * @param literal the literal to check.
     *
     * </dl><dl><dt><b>Preconditions:</b></dt><dd>
     * <ul><li><code>literal != null</code>
     * </ul></dd></dl>
     */
    protected boolean checkLiteral(String literal)
        throws IOException
    {
        int length = literal.length();
        for (int i = 0; i < length; i += 1) {
            if (this.readChar() != literal.charAt(i)) {
                return false;
            }
        }
        return true;
    }


    /**
     * Reads a character from a reader.
     */
    protected char readChar()
        throws IOException
    {
        if (this.charReadTooMuch != '\0') {
            char ch = this.charReadTooMuch;
            this.charReadTooMuch = '\0';
            return ch;
        } else {
            int i = this.reader.read();
            if (i < 0) {
                throw this.unexpectedEndOfData();
            } else if (i == 10) {
                this.parserLineNumber += 1;
                return '\n';
            } else {
                if (DEBUG) System.out.println((char) i);
                return (char) i;
            }
        }
    }


    /**
     * Scans an XML element.
     *
     * @param elt The element that will contain the result.
     *
     * </dl><dl><dt><b>Preconditions:</b></dt><dd>
     * <ul><li>The first &lt; has already been read.
     *     <li><code>elt != null</code>
     * </ul></dd></dl>
     */
    protected void scanElement(XMLElement elt)
        throws IOException
    {
        StringBuffer buf = new StringBuffer();
        this.scanIdentifier(buf);
        String bname = buf.toString();
        elt.setName(bname);
        if (DEBUG) System.out.println("got bname '" + bname + "'" + "  " + parserLineNumber);
        char ch = this.scanWhitespace();
        while ((ch != '>') && (ch != '/')) {
            if (DEBUG) System.out.println("up here then");
            buf.setLength(0);
            this.unreadChar(ch);
            this.scanIdentifier(buf);
            String key = buf.toString();
            if (DEBUG) System.out.println("  attr is " + key);
            ch = this.scanWhitespace();
            /*
            if (ch != '=') {
                throw this.expectedInput("=");
            }
            */
            if (ch != '=') {
                if (ignoreMissingAttributes) {
                    if (DEBUG) System.out.println("expected = got " + ch);
                    this.unreadChar(ch);
                    //buf.setLength(0);
                    elt.setAttribute(key, "");
                    if (DEBUG) System.out.println("    value is empty");
                } else {
                    throw this.expectedInput("=");
                }
            } else {
                this.unreadChar(this.scanWhitespace());
                buf.setLength(0);
                this.scanString(buf);
                elt.setAttribute(key, buf);
                if (DEBUG) System.out.println("    value is " + buf);
            }
            ch = this.scanWhitespace();
            if (DEBUG) System.out.println("scan of white produced " + ch);
        }
        if (DEBUG) System.out.println("out here now?");
        if (ch == '/') {
            ch = this.readChar();
            if (ch != '>') {
                throw this.expectedInput(">");
            }
            if (DEBUG) System.out.println("leaving");
            return;
        }
        buf.setLength(0);
        ch = this.scanWhitespace(buf);
        if (DEBUG) System.out.println("not yet leaving " + ch);
        if (ch != '<') {
            this.unreadChar(ch);
            this.scanPCData(buf);
        } else {
            for (;;) {
                ch = this.readChar();
                if (ch == '!') {
                    if (this.checkCDATA(buf)) {
                        this.scanPCData(buf);
                        break;
                    } else {
                        ch = this.scanWhitespace(buf);
                        if (ch != '<') {
                            this.unreadChar(ch);
                            this.scanPCData(buf);
                            break;
                        }
                    }
                } else {
                    if ((ch != '/') || this.ignoreWhitespace) {
                        buf.setLength(0);
                    }
                    if (ch == '/') {
                        this.unreadChar(ch);
                    }
                    break;
                }
            }
        }
        if (buf.length() == 0) {
            while (ch != '/') {
                if (ch == '!') {
                    ch = this.readChar();
                    if (ch != '-') {
                        throw this.expectedInput("Comment or Element");
                    }
                    ch = this.readChar();
                    if (ch != '-') {
                        throw this.expectedInput("Comment or Element");
                    }
                    this.skipComment();
                } else {
                    this.unreadChar(ch);
                    XMLElement child = this.createAnotherElement();
                    this.scanElement(child);
                    elt.addChild(child);
                }
                ch = this.scanWhitespace();
                if (ch != '<') {
                    throw this.expectedInput("<");
                }
                ch = this.readChar();
            }
            this.unreadChar(ch);
        } else {
            if (this.ignoreWhitespace) {
                elt.setContent(buf.toString().trim());
            } else {
                elt.setContent(buf.toString());
            }
        }
        ch = this.readChar();
        if (ch != '/') {
            throw this.expectedInput("/");
        }
        this.unreadChar(this.scanWhitespace());
        if (! this.checkLiteral(bname)) {
            throw this.expectedInput(bname);
        }
        if (this.scanWhitespace() != '>') {
            throw this.expectedInput(">");
        }
    }


    /**
     * Resolves an entity. The name of the entity is read from the reader.
     * The value of the entity is appended to <code>buf</code>.
     *
     * @param buf Where to put the entity value.
     *
     * </dl><dl><dt><b>Preconditions:</b></dt><dd>
     * <ul><li>The first &amp; has already been read.
     *     <li><code>buf != null</code>
     * </ul></dd></dl>
     */
    protected void resolveEntity(StringBuffer buf)
        throws IOException
    {
        char ch = '\0';
        StringBuffer keyBuf = new StringBuffer();
        for (;;) {
            ch = this.readChar();
            if (ch == ';') {
                break;
            }
            keyBuf.append(ch);
        }
        //System.out.println("found ent " + keyBuf);
        String key = keyBuf.toString();
        if (key.charAt(0) == '#') {
            try {
                if (key.charAt(1) == 'x') {
                    ch = (char) Integer.parseInt(key.substring(2), 16);
                } else {
                    ch = (char) Integer.parseInt(key.substring(1), 10);
                }
            } catch (NumberFormatException e) {
                throw this.unknownEntity(key);
            }
            buf.append(ch);
        } else {
            char[] value = (char[]) this.entities.get(key);
            // [fry] modified this to provide the option of ignoring missing entities
            if (value == null) {
                if (!ignoreUnknownEntities) {
                    throw this.unknownEntity(key);
                } else {
                    // push the unknown entity back onto the stream
                    buf.append("&" + key + ";");
                }
            } else {
                if (DEBUG) System.out.println("appending entity " + new String(value));
                buf.append(value);
            }
        }
    }


    /**
     * Version of resolveEntity that gives up when it hits its ending delimiter.
     * Handles parsing <meta http-equiv> stuff where the URL contains an ampersand.
     */
    protected void resolveEntity(StringBuffer buf, char delimiter) throws IOException
    {
        char ch = '\0';
        StringBuffer keyBuf = new StringBuffer();
        for (;;) {
            ch = this.readChar();
            if (ch == ';') {
                break;
            }
            // this wasn't an entity, and we've reached the end of the string
            if (ch == delimiter) {
                if (DEBUG) System.out.println("found end of delim " + keyBuf);
                buf.append(keyBuf);
                unreadChar(ch);
                return;
            }
            keyBuf.append(ch);
        }
        String key = keyBuf.toString();
        if (key.charAt(0) == '#') {
            try {
                if (key.charAt(1) == 'x') {
                    ch = (char) Integer.parseInt(key.substring(2), 16);
                } else {
                    ch = (char) Integer.parseInt(key.substring(1), 10);
                }
            } catch (NumberFormatException e) {
                throw this.unknownEntity(key);
            }
            buf.append(ch);
        } else {
            char[] value = (char[]) this.entities.get(key);
            if (value == null) {
                if (!ignoreUnknownEntities) {
                    throw this.unknownEntity(key);
                } else {
                    // could also just push back the text instead of a blank?
                }
            } else {
                if (DEBUG) System.out.println("appending entity2 " + new String(value));
                buf.append(value);
            }
        }
    }

    /**
     * Pushes a character back to the read-back buffer.
     *
     * @param ch The character to push back.
     *
     * </dl><dl><dt><b>Preconditions:</b></dt><dd>
     * <ul><li>The read-back buffer is empty.
     *     <li><code>ch != '\0'</code>
     * </ul></dd></dl>
     */
    protected void unreadChar(char ch)
    {
        this.charReadTooMuch = ch;
    }


    /**
     * Creates a parse exception for when an invalid valueset is given to
     * a method.
     *
     * @param name The name of the entity.
     *
     * </dl><dl><dt><b>Preconditions:</b></dt><dd>
     * <ul><li><code>name != null</code>
     * </ul></dd></dl>
     */
    protected XMLParseException invalidValueSet(String name)
    {
        String msg = "Invalid value set (entity name = \"" + name + "\")";
        return new XMLParseException(this.getName(), this.parserLineNumber, msg);
    }


    /**
     * Creates a parse exception for when an invalid value is given to a
     * method.
     *
     * @param name  The name of the entity.
     * @param value The value of the entity.
     *
     * </dl><dl><dt><b>Preconditions:</b></dt><dd>
     * <ul><li><code>name != null</code>
     *     <li><code>value != null</code>
     * </ul></dd></dl>
     */
    protected XMLParseException invalidValue(String name,
                                             String value)
    {
        String msg = "Attribute \"" + name + "\" does not contain a valid "
                   + "value (\"" + value + "\")";
        return new XMLParseException(this.getName(), this.parserLineNumber, msg);
    }


    /**
     * Creates a parse exception for when the end of the data input has been
     * reached.
     */
    protected XMLParseException unexpectedEndOfData()
    {
        String msg = "Unexpected end of data reached";
        return new XMLParseException(this.getName(), this.parserLineNumber, msg);
    }


    /**
     * Creates a parse exception for when a syntax error occured.
     *
     * @param context The context in which the error occured.
     *
     * </dl><dl><dt><b>Preconditions:</b></dt><dd>
     * <ul><li><code>context != null</code>
     *     <li><code>context.length() &gt; 0</code>
     * </ul></dd></dl>
     */
    protected XMLParseException syntaxError(String context)
    {
        String msg = "Syntax error while parsing " + context;
        return new XMLParseException(this.getName(), this.parserLineNumber, msg);
    }


    /**
     * Creates a parse exception for when the next character read is not
     * the character that was expected.
     *
     * @param charSet The set of characters (in human readable form) that was
     *                expected.
     *
     * </dl><dl><dt><b>Preconditions:</b></dt><dd>
     * <ul><li><code>charSet != null</code>
     *     <li><code>charSet.length() &gt; 0</code>
     * </ul></dd></dl>
     */
    protected XMLParseException expectedInput(String charSet)
    {
        String msg = "Expected: " + charSet;
        return new XMLParseException(this.getName(), this.parserLineNumber, msg);
    }


    /**
     * Creates a parse exception for when an entity could not be resolved.
     *
     * @param name The name of the entity.
     *
     * </dl><dl><dt><b>Preconditions:</b></dt><dd>
     * <ul><li><code>name != null</code>
     *     <li><code>name.length() &gt; 0</code>
     * </ul></dd></dl>
     */
    protected XMLParseException unknownEntity(String name)
    {
        String msg = "Unknown or invalid entity: &" + name + ";";
        return new XMLParseException(this.getName(), this.parserLineNumber, msg);
    }
}
