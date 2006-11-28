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


/**
 * An XMLParseException is thrown when an error occures while parsing an XML
 * string.
 *
 * @see processing.xml.XMLElement
 *
 * @author Marc De Scheemaecker
 */
public class XMLParseException
    extends RuntimeException
{

    /**
     * Indicates that no line number has been associated with this exception.
     */
    public static final int NO_LINE = -1;


    /**
     * The line number in the source code where the error occurred, or
     * <code>NO_LINE</code> if the line number is unknown.
     *
     * <dl><dt><b>Invariants:</b></dt><dd>
     * <ul><li><code>lineNumber &gt 0 || lineNumber == NO_LINE</code>
     * </ul></dd></dl>
     */
    private int lineNumber;


    /**
     * Creates an exception.
     *
     * @param name    The name of the element where the error is located.
     * @param message A message describing what went wrong.
     *
     * </dl><dl><dt><b>Preconditions:</b></dt><dd>
     * <ul><li><code>message != null</code>
     * </ul></dd></dl>
     *
     * <dl><dt><b>Postconditions:</b></dt><dd>
     * <ul><li>getLineNumber() => NO_LINE
     * </ul></dd></dl><dl>
     */
    public XMLParseException(String name,
                             String message)
    {
        super("XML Parse Exception during parsing of "
              + ((name == null) ? "the XML definition"
                                : ("a " + name + " element"))
              + ": " + message);
        this.lineNumber = XMLParseException.NO_LINE;
    }


    /**
     * Creates an exception.
     *
     * @param name    The name of the element where the error is located.
     * @param lineNumber  The number of the line in the input.
     * @param message A message describing what went wrong.
     *
     * </dl><dl><dt><b>Preconditions:</b></dt><dd>
     * <ul><li><code>message != null</code>
     *     <li><code>lineNumber &gt; 0</code>
     * </ul></dd></dl>
     *
     * <dl><dt><b>Postconditions:</b></dt><dd>
     * <ul><li>getLineNumber() => lineNumber
     * </ul></dd></dl><dl>
     */
    public XMLParseException(String name,
                             int    lineNumber,
                             String message)
    {
        super("XML Parse Exception during parsing of "
              + ((name == null) ? "the XML definition"
                                : ("a " + name + " element"))
              + " at line " + lineNumber + ": " + message);
        this.lineNumber = lineNumber;
    }


    /**
     * Where the error occurred, or <code>NO_LINE</code> if the line number is
     * unknown.
     *
     * @see processing.xml.XMLParseException#NO_LINE
     */
    public int getLineNumber()
    {
        return this.lineNumber;
    }

}
