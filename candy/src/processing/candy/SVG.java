/*
  Candy 3 - SVG Importer for Processing - http://processing.org

  Copyright (c) 2006 Michael Chang (Flux)
  http://www.ghost-hack.com/

  Revised and expanded by Ben Fry for inclusion as a core library
  Copyright (c) 2006-08 Ben Fry and Casey Reas

  This library is free software; you can redistribute it and/or
  modify it under the terms of the GNU Lesser General Public
  License version 2.1 as published by the Free Software Foundation.

  This library is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  Lesser General Public License for more details.
*/

package processing.candy;

import processing.core.*;
import processing.xml.*;


/**
 * Candy is a minimal SVG import library for Processing.
 * Candy was written by Michael Chang, and later revised and
 * expanded for use as a Processing core library by Ben Fry.
 * <p>
 * SVG stands for Scalable Vector Graphics, a portable graphics format. It is 
 * a vector format so it allows for infinite resolution and relatively small
 * file sizes. Most modern media software can view SVG files, including Adobe 
 * products, Firefox, etc. Illustrator and Inkscape can edit SVG files.
 * <p>
 * We have no intention of turning this into a full-featured SVG library.
 * The goal of this project is a basic shape importer that is small enough
 * to be included with applets, meaning that its download size should be
 * in the neighborhood of 25-30k. Because of this size, it is not made part
 * of processing.core, as it's not a feature that will be used by the majority
 * of our audience. 
 * 
 * For more sophisticated import/export, consider the
 * <A HREF="http://xmlgraphics.apache.org/batik/">Batik</A> 
 * library from the Apache Software Foundation. Future improvements to this
 * library may focus on this properly supporting a specific subset of SVG, 
 * for instance the simpler SVG profiles known as
 * <A HREF="http://www.w3.org/TR/SVGMobile/">SVG Tiny or Basic</A>,
 * although we still would not support the interactivity options.
 * <p>
 * This library was specifically tested under SVG files created with Adobe
 * Illustrator. We can't guarantee that it will work for any SVGs created with
 * other software. In the future we would like to improve compatibility with
 * Open Source software such as Inkscape, however initial tests show its
 * base implementation produces more complicated files, and this will require
 * more time.
 * <p>
 * An SVG created under Illustrator must be created in one of two ways:
 * <UL>
 * <LI>File &rarr; Save for Web (or control-alt-shift-s on a PC). Under
 * settings, make sure the CSS properties is set to "Presentation Attributes".
 * <LI>With Illustrator CS2, it is also possible to use "Save As" with "SVG"
 * as the file setting, but the CSS properties should also be set similarly.
 * </UL>
 * Saving it any other way will most likely break Candy.
 *
 * <p> <hr noshade> <p>
 *
 * A minimal example program using Candy:
 * (assuming a working moo.svg is in your data folder)
 *
 * <PRE>
 * import processing.candy.*;
 * import processing.xml.*;
 *
 * SVG moo;
 * void setup() {
 *   size(400,400);
 *   moo = new SVG("moo.svg",this);
 * }
 * void draw() {
 *   moo.draw();
 * }
 * </PRE>
 *
 * <EM>Note that processing.xml needs to be imported as well.</EM>
 * This may not be required when running code within the Processing
 * environment, but when exported it may cause a NoClassDefError.
 * This will be fixed in later releases of Processing
 * (<A HREF="http://dev.processing.org/bugs/show_bug.cgi?id=518">Bug 518</A>).
 *
 * <p> <hr noshade> <p>
 *
 * August 2008 revisions by fry (Processing 0149) 
 * <UL>
 * <LI> Major changes to rework around PShape. 
 * <LI> Now implementing more of the "transform" attribute.
 * </UL>
 * 
 * February 2008 revisions by fry (Processing 0136)
 * <UL>
 * <LI> Added support for quadratic curves in paths (Q, q, T, and t operators)
 * <LI> Support for reading SVG font data (though not rendering it yet) 
 * </UL>
 *  
 * Revisions for "Candy 2" November 2006 by fry
 * <UL>
 * <LI> Switch to the new processing.xml library
 * <LI> Several bug fixes for parsing of shape data
 * <LI> Support for linear and radial gradients
 * <LI> Support for additional types of shapes
 * <LI> Added compound shapes (shapes with interior points)
 * <LI> Added methods to get shapes from an internal table
 * </UL>
 *
 * Revision 10/31/06 by flux
 * <UL>
 * <LI> Now properly supports Processing 0118
 * <LI> Fixed a bunch of things for Casey's students and general buggity.
 * <LI> Will now properly draw #FFFFFFFF colors (were being represented as -1)
 * <LI> SVGs without <g> tags are now properly caught and loaded
 * <LI> Added a method customStyle() for overriding SVG colors/styles
 * <LI> Added a method SVGStyle() to go back to using SVG colors/styles
 * </UL>
 *
 * Some SVG objects and features may not yet be supported.
 * Here is a partial list of non-included features
 * <UL>
 * <LI> Rounded rectangles
 * <LI> Drop shadow objects
 * <LI> Typography
 * <LI> <STRIKE>Layers</STRIKE> added for Candy 2
 * <LI> Patterns
 * <LI> Embedded images
 * </UL>
 *
 * For those interested, the SVG specification can be found
 * <A HREF="http://www.w3.org/TR/SVG">here</A>.
 */
public class SVG extends BaseObject {

    /**
     * Initializes a new SVG Object with the given filename.
     */
    public SVG(PApplet parent, String filename) {
        // this will grab the root document, starting <svg ...>
        // the xml version and initial comments are ignored
    	this(new XMLElement(parent, filename));
    }
    
    
    /**
     * Initializes a new SVG Object with the given filename.
     */
    public SVG(XMLElement svg) {
    	super(null, svg);
        
        if (!svg.getName().equals("svg")) {
            throw new RuntimeException("root is not <svg>, it's <" + svg.getName() + ">");
        }

        // not proper parsing of the viewBox, but will cover us for cases where
        // the width and height of the object is not specified
        String viewBoxStr = svg.getStringAttribute("viewBox");
        if (viewBoxStr != null) {
            int[] viewBox = PApplet.parseInt(PApplet.splitTokens(viewBoxStr));
            width = viewBox[2];
            height = viewBox[3];
        }

        // TODO if viewbox is not same as width/height, then use it to scale
        // the original objects. for now, viewbox only used when width/height
        // are empty values (which by the spec means w/h of "100%"
        String unitWidth = svg.getStringAttribute("width");
        String unitHeight = svg.getStringAttribute("height");
        if (unitWidth != null) {
            width = parseUnitSize(unitWidth);
            height = parseUnitSize(unitHeight);
        } else {
            if ((width == 0) || (height == 0)) {
                //throw new RuntimeException("width/height not specified");
            	System.err.println("The width and/or height is not " +
            			           "readable in the <svg> tag of this file.");
            	// For the spec, the default is 100% and 100%. For purposes 
            	// here, insert a dummy value because this is prolly just a 
            	// font or something for which the w/h doesn't matter.
            	width = 1;
            	height = 1;
            }
        }

        //root = new Group(null, svg);
    }


    /**
     * Internal method used to clone an object and return the subtree.
     */
//    protected SVG(PApplet parent, float width, float height, 
//                  HashMap<String, BaseObject> table,
//                  BaseObject obj, boolean styleOverride) {
//    	super(kind);
//    	
//        this.parent = parent;
//        this.width = width;
//        this.height = height;
//        this.table = table;
//        this.root = obj;
//        this.svg = obj.element;
//        this.ignoreStyles = styleOverride;
//    }


    /**
     * Get a particular element based on its SVG ID. When editing SVG by hand,
     * this is the id="" tag on any SVG element. When editing from Illustrator,
     * these IDs can be edited by expanding the layers palette. The names used
     * in the layers palette, both for the layers or the shapes and groups
     * beneath them can be used here.
     * <PRE>
     * // This code grabs "Layer 3" and the shapes beneath it.
     * SVG layer3 = svg.get("Layer 3");
     * </PRE>
     */
//    public SVG get(String name) {
//        BaseObject obj = table.get(name);
//        if (obj == null) {
//            // try with underscores instead of spaces
//            obj = table.get(name.replace(' ', '_'));
//        }
//        if (obj != null) {
//            return new SVG(parent, width, height, table, obj, ignoreStyles);
//        }
//        return null;
//    }
}
