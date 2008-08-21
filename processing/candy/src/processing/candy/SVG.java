/*
  Candy 2 - SVG Importer for Processing - http://processing.org

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

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.Hashtable;

import processing.core.*;
import processing.xml.*;


/**
 * Candy is a minimal SVG import library for Processing.
 * Candy was written by Michael Chang, and later revised and
 * expanded for use as a Processing core library by Ben Fry.
 * <p>
 * SVG stands for Scalable Vector Graphics, a portable graphics
 * format. It is a vector format so it allows for infinite resolution
 * and relatively minute file sizes. Most modern media software
 * can view SVG files, including Firefox, Adobe products, etc.
 * You can use something like Illustrator to edit SVG files.
 * <p>
 * We have no intention of turning this into a full-featured SVG library.
 * The goal of this project is a basic shape importer that is small enough
 * to be included with applets, meaning that its download size should be
 * in the neighborhood of 25-30k. Because of this size, it is not made part
 * of processing.core, because it would increase the download size of any
 * applet by 20%, and it's not a feature that will be used by the majority
 * of our audience. For more sophisticated import/export, consider the
 * <A HREF="http://xmlgraphics.apache.org/batik/">Batik</A> library
 * from the Apache Software Foundation. Future improvements to this
 * library may focus on this properly supporting a specific subset of
 * SVG, for instance the simpler SVG profiles known as
 * <A HREF="http://www.w3.org/TR/SVGMobile/">SVG Tiny or Basic</A>,
 * although we still would not support the interactivity options.
 * <p>
 * This library was specifically tested under SVG files created with Adobe
 * Illustrator. We can't guarantee that it will work for any SVGs created with
 * other software. In the future we would like to improve compatibility with
 * Open Source software such as InkScape, however initial tests show its
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
public class SVG {

    protected PApplet parent;

    public float width;
    public float height;

    protected Hashtable table = new Hashtable();
    protected XMLElement svg;
    protected BaseObject root;

    protected boolean ignoreStyles = false;

    int drawMode = PConstants.CORNER;


    /**
     * Initializes a new SVG Object with the given filename.
     */
    public SVG(PApplet parent, String filename) {
        // this will grab the root document, starting <svg ...>
        // the xml version and initial comments are ignored
    	this(parent, new XMLElement(parent, filename));
    }
    
    
    /**
     * Initializes a new SVG Object with the given filename.
     */
    public SVG(PApplet parent, XMLElement svg) {
        this.parent = parent;
        this.svg = svg;

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

        /*
        PApplet.println("document has " + document.getChildCount() + " children");
        //Get the xml child node we need
        XMLElement doc = document.getChild(1);
        PApplet.println(doc);
        if (true) return;
        */

        /*
        //XMLElement entSVG = doc.getChild(0);
        //XMLElement svg = entSVG.getChild(1);
        //While we're doing that, save the width and height too
        //svgWidth = svg.getIntAttribute("width");
        //svgHeight = svg.getIntAttribute("height");

        //Catch exception when SVG doesn't have a <g> tag
        XMLElement graphics;
        String nameOfFirstChild = svg.getChild(1).toString();
        if(nameOfFirstChild.equals("<g>"))
            graphics = svg.getChild(1);
        else
            graphics = svg;

        this.svgData = svg;
        */

        //parseChildren(document);
        root = new Group(null, svg);

        /*
        XMLElement graphics = null;

        //Print SVG on construction
        //Use this for debugging
        //svg.printElementTree(" .");
        */
    }


    /**
     * Internal method used to clone an object and return the subtree.
     */
    protected SVG(PApplet parent, float width, float height, Hashtable table,
                  BaseObject obj, boolean styleOverride) {
        this.parent = parent;
        this.width = width;
        this.height = height;
        this.table = table;
        this.root = obj;
        this.svg = obj.element;
        this.ignoreStyles = styleOverride;
    }


    /**
     * Parse a size that may have a suffix for its units.
     * Ignoring cases where this could also be a percentage.
     * The <A HREF="http://www.w3.org/TR/SVG/coords.html#Units">units</A> spec:
     * <UL>
     * <LI>"1pt" equals "1.25px" (and therefore 1.25 user units)
     * <LI>"1pc" equals "15px" (and therefore 15 user units)
     * <LI>"1mm" would be "3.543307px" (3.543307 user units)
     * <LI>"1cm" equals "35.43307px" (and therefore 35.43307 user units)
     * <LI>"1in" equals "90px" (and therefore 90 user units)
     * </UL>
     */
    public float parseUnitSize(String text) {
        int len = text.length() - 2;

        if (text.endsWith("pt")) {
            return PApplet.parseFloat(text.substring(0, len)) * 1.25f;
        } else if (text.endsWith("pc")) {
            return PApplet.parseFloat(text.substring(0, len)) * 15;
        } else if (text.endsWith("mm")) {
            return PApplet.parseFloat(text.substring(0, len)) * 3.543307f;
        } else if (text.endsWith("cm")) {
            return PApplet.parseFloat(text.substring(0, len)) * 35.43307f;
        } else if (text.endsWith("in")) {
            return PApplet.parseFloat(text.substring(0, len)) * 90;
        } else if (text.endsWith("px")) {
            return PApplet.parseFloat(text.substring(0, len));
        } else {
            return PApplet.parseFloat(text);
        }
    }


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
    public SVG get(String name) {
        BaseObject obj = (BaseObject) table.get(name);
        if (obj == null) {
            // try with underscores instead of spaces
            obj = (BaseObject) table.get(name.replace(' ', '_'));
        }
        if (obj != null) {
            return new SVG(parent, width, height, table, obj, ignoreStyles);
        }
        return null;
    }


    // grab the (fill) gradient from a particular object by name
    // and apply it to either the stroke or fill
    // based on


    protected Paint getGradient(String name, float cx, float cy, float r) {
        BaseObject obj = (BaseObject) table.get(name);
        if (obj == null) {
            // try with underscores instead of spaces
            obj = (BaseObject) table.get(name.replace(' ', '_'));
        }

        if (obj != null) {
            if (obj.fillGradient != null) {
                return obj.calcGradientPaint(obj.fillGradient, cx, cy, r);
            }
        }
        throw new RuntimeException("No gradient found for shape " + name);
    }


    protected Paint getGradient(String name, float x1, float y1, float x2, float y2) {
        BaseObject obj = (BaseObject) table.get(name);
        if (obj == null) {
            // try with underscores instead of spaces
            obj = (BaseObject) table.get(name.replace(' ', '_'));
        }

        if (obj != null) {
            if (obj.fillGradient != null) {
                return obj.calcGradientPaint(obj.fillGradient, x1, y1, x2, y2);
            }
        }
        throw new RuntimeException("No gradient found for shape " + name);
    }


    public void strokeGradient(String name, float x, float y, float r) {
        Paint paint = getGradient(name, x, y, r);

        if (parent.g instanceof PGraphicsJava2D) {
            PGraphicsJava2D p2d = ((PGraphicsJava2D) parent.g);

            p2d.strokeGradient = true;
            p2d.strokeGradientObject = paint;
        }
    }

    public void strokeGradient(String name, float x1, float y1, float x2, float y2) {
        Paint paint = getGradient(name, x1, y1, x2, y2);

        if (parent.g instanceof PGraphicsJava2D) {
            PGraphicsJava2D p2d = ((PGraphicsJava2D) parent.g);

            p2d.strokeGradient = true;
            p2d.strokeGradientObject = paint;
        }
    }


    public void fillGradient(String name, float x, float y, float r) {
        Paint paint = getGradient(name, x, y, r);

        if (parent.g instanceof PGraphicsJava2D) {
            PGraphicsJava2D p2d = ((PGraphicsJava2D) parent.g);

            p2d.fillGradient = true;
            p2d.fillGradientObject = paint;
        }
    }


    public void fillGradient(String name, float x1, float y1, float x2, float y2) {
        Paint paint = getGradient(name, x1, y1, x2, y2);

        if (parent.g instanceof PGraphicsJava2D) {
            PGraphicsJava2D p2d = ((PGraphicsJava2D) parent.g);

            p2d.fillGradient = true;
            p2d.fillGradientObject = paint;
        }
    }


    /**
     * Temporary hack for gradient handling. This is not supported
     * and will be removed from future releases.
     */
    /*
    public void drawStyles() {
        root.drawStyles();
        //PApplet.println(root);

        if (root instanceof VectorObject) {
            ((VectorObject)root).drawStyles();
        } else {
            PApplet.println("Only use drawStyles() on an object, not a group.");
        }
    }
    */


    public void draw() {
        if (drawMode == PConstants.CENTER) {
            parent.pushMatrix();
            parent.translate(-width/2, -height/2);
            drawImpl();
            parent.popMatrix();

        } else if ((drawMode == PConstants.CORNER) ||
                   (drawMode == PConstants.CORNERS)) {
            drawImpl();
        }
    }


    /**
     * Convenience method to draw at a particular location.
     */
    public void draw(float x, float y) {
        parent.pushMatrix();

        if (drawMode == PConstants.CENTER) {
            parent.translate(x - width/2, y - height/2);

        } else if ((drawMode == PConstants.CORNER) ||
                   (drawMode == PConstants.CORNERS)) {
            parent.translate(x, y);
        }
        drawImpl();

        parent.popMatrix();
    }


    public void draw(float x, float y, float c, float d) {
        parent.pushMatrix();

        if (drawMode == PConstants.CENTER) {
            // x and y are center, c and d refer to a diameter
            parent.translate(x - c/2f, y - d/2f);
            parent.scale(c / width, d / height);

        } else if (drawMode == PConstants.CORNER) {
            parent.translate(x, y);
            parent.scale(c / width, d / height);

        } else if (drawMode == PConstants.CORNERS) {
            // c and d are x2/y2, make them into width/height
            c -= x;
            d -= y;
            // then same as above
            parent.translate(x, y);
            parent.scale(c / width, d / height);
        }
        drawImpl();

        parent.popMatrix();
    }


    /**
     * Draws the SVG document.
     */
    public void drawImpl() {
        boolean stroke = parent.g.stroke;
        int strokeColor = parent.g.strokeColor;
        float strokeWeight = parent.g.strokeWeight;
        int strokeCap = parent.g.strokeCap;
        int strokeJoin= parent.g.strokeJoin;

        boolean fill = parent.g.fill;
        int fillColor = parent.g.fillColor;

        int ellipseMode = parent.g.ellipseMode;

        root.draw();

        parent.g.stroke = stroke;
        parent.g.strokeColor = strokeColor;
        parent.g.strokeWeight = strokeWeight;
        parent.g.strokeCap = strokeCap;
        parent.g.strokeJoin = strokeJoin;

        parent.g.fill = fill;
        parent.g.fillColor = fillColor;

        parent.g.ellipseMode = ellipseMode;
    }


    /**
     * Set the orientation for drawn objects, similar to PImage.imageMode().
     * @param which Either CORNER, CORNERS, or CENTER.
     */
    public void drawMode(int which) {
        drawMode = which;
    }


    /**
     * Overrides SVG-set styles and uses PGraphics styles and colors.
     * Identical to ignoreStyles(true).
     */
    public void ignoreStyles() {
        ignoreStyles(true);
    }


    /**
     * Enables or disables style information (fill and stroke) set in the file.
     * @param state true to use user-specified stroke/fill, false for svg version
     */
    public void ignoreStyles(boolean state) {
        ignoreStyles = state;
    }


    /**
     * Prints out the SVG document useful for parsing
     */
    public void print() {
        PApplet.println(svg.toString());
    }


    // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


    protected abstract class BaseObject {
        String id;
        XMLElement element;

        // set to false if the object is hidden in the layers palette
        boolean display;

        boolean stroke;
        int strokeColor;
        float strokeWeight; // default is 1
        int strokeCap;
        int strokeJoin;
        Gradient strokeGradient;
        Paint strokeGradientPaint;
        String strokeName;  // id of another object, gradients only?

        boolean fill;
        int fillColor;
        Gradient fillGradient;
        Paint fillGradientPaint;
        String fillName;  // id of another object

        boolean hasTransform;
        float[] transformation;

        float opacity;


        public BaseObject(BaseObject parent, XMLElement properties) {

            if (parent == null) {
                // set values to their defaults according to the SVG spec
                stroke = false;
                strokeColor = 0xff000000;
                strokeWeight = 1;
                strokeCap = PConstants.SQUARE;  // equivalent to BUTT in svg spec
                strokeJoin = PConstants.MITER;
                strokeGradient = null;
                strokeGradientPaint = null;
                strokeName = null;

                fill = true;
                fillColor = 0xff000000;
                fillGradient = null;
                fillGradientPaint = null;
                fillName = null;

                //hasTransform = false;
                //transformation = null; //new float[] { 1, 0, 0, 1, 0, 0 };

                opacity = 1;

            } else {
                stroke = parent.stroke;
                strokeColor = parent.strokeColor;
                strokeWeight = parent.strokeWeight;
                strokeCap = parent.strokeCap;
                strokeJoin = parent.strokeJoin;
                strokeGradient = parent.strokeGradient;
                strokeGradientPaint = parent.strokeGradientPaint;
                strokeName = parent.strokeName;

                fill = parent.fill;
                fillColor = parent.fillColor;
                fillGradient = parent.fillGradient;
                fillGradientPaint = parent.fillGradientPaint;
                fillName = parent.fillName;

                //hasTransform = parent.hasTransform;
                //transformation = parent.transformation;

                opacity = parent.opacity;
            }

            element = properties;

            id = properties.getStringAttribute("id");
            if (id != null) {
                table.put(id, this);
                //System.out.println("now parsing " + id);
            }

            String displayStr = properties.getStringAttribute("display", "inline");
            display = !displayStr.equals("none");

            getColors(properties);
            getTransformation(properties);
        }


        private void getTransformation(XMLElement properties) {
            String transform = properties.getStringAttribute("transform");
            if (transform != null) {
                this.hasTransform = true;
                transform = transform.substring(7, transform.length() - 2);
                String tf[] = PApplet.splitTokens(transform);
                this.transformation = PApplet.parseFloat(tf);
                /*
                this.transformation = new float[tf.length];
                for (int i = 0; i < transformation.length; i++) {
                    this.transformation[i] = Float.valueOf(tf[i]).floatValue();
                }

                // Hacky code to get rotation working
                // Done through the powers of trial and error [mchang]
                float t[] = this.transformation;
                if (t[0] < 0 && t[1] < 0 && t[2] > 0 && t[3] < 0)
                    this.rotation = -PApplet.acos(this.transformation[3]);
                if (t[0] > 0 && t[1] < 0 && t[2] > 0 && t[3] > 0)
                    this.rotation = PApplet.asin(this.transformation[1]);
                if (t[0] < 0 && t[1] > 0 && t[2] < 0 && t[3] < 0)
                    this.rotation = PApplet.acos(this.transformation[0]);
                if (t[0] > 0 && t[1] > 0 && t[2] < 0 && t[3] > 0)
                    this.rotation = PApplet.acos(this.transformation[0]);
                this.translateX = this.transformation[4];
                this.translateY = this.transformation[5];
                */
            }
        }


        /*
        private int colorFromString(String color, String opacity) {
            if (!color.equals("none")) {
                color = color.substring(1, 7);
                color = opacity + color;
                return PApplet.unhex(color);
            } else {
                return transValue;
            }
        }
        */


        protected void getColors(XMLElement properties) {

            if (properties.hasAttribute("opacity")) {
                opacity = properties.getFloatAttribute("opacity");
            }
            int opacityMask = ((int) (opacity * 255)) << 24;

            if (properties.hasAttribute("stroke")) {
                String strokeText = properties.getStringAttribute("stroke");
                if (strokeText.equals("none")) {
                    stroke = false;
                } else if (strokeText.startsWith("#")) {
                    stroke = true;
                    strokeColor = opacityMask |
                    (Integer.parseInt(strokeText.substring(1), 16)) & 0xFFFFFF;
                } else if (strokeText.startsWith("rgb")) {
                    stroke = true;
                    strokeColor = opacityMask | parseRGB(strokeText);
                } else if (strokeText.startsWith("url(#")) {
                    strokeName = strokeText.substring(5, strokeText.length() - 1);
                    Object strokeObject = table.get(strokeName);
                    if (strokeObject instanceof Gradient) {
                        strokeGradient = (Gradient) strokeObject;
                        strokeGradientPaint = calcGradientPaint(strokeGradient); //, opacity);
                    } else {
                        System.err.println("url " + strokeName + " refers to unexpected data");
                    }
                }
            }

            if (properties.hasAttribute("stroke-width")) {
                // if NaN (i.e. if it's 'inherit') then default back to the inherit setting
                strokeWeight = properties.getFloatAttribute("stroke-width", strokeWeight);
            }

            if (properties.hasAttribute("stroke-linejoin")) {
                String linejoin = properties.getStringAttribute("stroke-linejoin");
                if (linejoin.equals("inherit")) {
                    // do nothing, will inherit automatically

                } else if (linejoin.equals("miter")) {
                    strokeJoin = PConstants.MITER;

                } else if (linejoin.equals("round")) {
                    strokeJoin = PConstants.ROUND;

                } else if (linejoin.equals("bevel")) {
                    strokeJoin = PConstants.BEVEL;
                }
            }

            if (properties.hasAttribute("stroke-linecap")) {
                String linecap = properties.getStringAttribute("stroke-linecap");
                if (linecap.equals("inherit")) {
                    // do nothing, will inherit automatically

                } else if (linecap.equals("butt")) {
                    strokeCap = PConstants.SQUARE;

                } else if (linecap.equals("round")) {
                    strokeCap = PConstants.ROUND;

                } else if (linecap.equals("square")) {
                    strokeCap = PConstants.PROJECT;
                }
            }


            // fill defaults to black (though stroke defaults to "none")
            // http://www.w3.org/TR/SVG/painting.html#FillProperties
            if (properties.hasAttribute("fill")) {
                String fillText = properties.getStringAttribute("fill");
                if (fillText.equals("none")) {
                    fill = false;
                } else if (fillText.startsWith("#")) {
                    fill = true;
                    fillColor = opacityMask |
                    (Integer.parseInt(fillText.substring(1), 16)) & 0xFFFFFF;
                    //System.out.println("hex for fill is " + PApplet.hex(fillColor));
                } else if (fillText.startsWith("rgb")) {
                    fill = true;
                    fillColor = opacityMask | parseRGB(fillText);
                } else if (fillText.startsWith("url(#")) {
                    fillName = fillText.substring(5, fillText.length() - 1);
                    //PApplet.println("looking for " + fillName);
                    Object fillObject = table.get(fillName);
                    //PApplet.println("found " + fillObject);
                    if (fillObject instanceof Gradient) {
                        fill = true;
                        fillGradient = (Gradient) fillObject;
                        fillGradientPaint = calcGradientPaint(fillGradient); //, opacity);
                        //PApplet.println("got filla " + fillObject);
                    } else {
                        System.err.println("url " + fillName + " refers to unexpected data");
                    }
                }
            }
        }


        int parseRGB(String what) {
            int leftParen = what.indexOf('(') + 1;
            int rightParen = what.indexOf(')');
            String sub = what.substring(leftParen, rightParen);
            int[] values = PApplet.parseInt(PApplet.splitTokens(sub, ", "));
            return (values[0] << 16) | (values[1] << 8) | (values[2]);
        }


        protected Paint calcGradientPaint(Gradient gradient) {
            if (gradient instanceof LinearGradient) {
                LinearGradient grad = (LinearGradient) gradient;
                return new LinearGradientPaint(grad.x1, grad.y1, grad.x2, grad.y2,
                                               grad.offset, grad.color, grad.count,
                                               opacity);

            } else if (gradient instanceof RadialGradient) {
                RadialGradient grad = (RadialGradient) gradient;
                return new RadialGradientPaint(grad.cx, grad.cy, grad.r,
                                               grad.offset, grad.color, grad.count,
                                               opacity);
            }
            return null;
        }


        protected Paint calcGradientPaint(Gradient gradient,
                                          float x1, float y1, float x2, float y2) {
            if (gradient instanceof LinearGradient) {
                LinearGradient grad = (LinearGradient) gradient;
                return new LinearGradientPaint(x1, y1, x2, y2,
                                               grad.offset, grad.color, grad.count,
                                               opacity);
            }
            throw new RuntimeException("Not a linear gradient.");
        }


        protected Paint calcGradientPaint(Gradient gradient,
                                          float cx, float cy, float r) {
            if (gradient instanceof RadialGradient) {
                RadialGradient grad = (RadialGradient) gradient;
                return new RadialGradientPaint(cx, cy, r,
                                               grad.offset, grad.color, grad.count,
                                               opacity);
            }
            throw new RuntimeException("Not a radial gradient.");
        }


        protected abstract void drawShape();


        protected void draw() {
            if (!display) return;  // don't display if set invisible

            if (!ignoreStyles) {
                drawStyles();
            }

            if (hasTransform) {
                parent.pushMatrix();
                parent.applyMatrix(transformation[0], transformation[1], transformation[2],
                                   transformation[3], transformation[4], transformation[5]);
                //parent.translate(translateX, translateY);
                //parent.rotate(rotation);
            }

            drawShape();

            if (hasTransform) {
                parent.popMatrix();
            }

            /*
            if (parent.g instanceof PGraphicsJava2D) {
                PGraphicsJava2D p2d = ((PGraphicsJava2D) parent.g);

                if (strokeGradient != null) {
                    p2d.strokeGradient = false;
                }
                if (fillGradient != null) {
                    p2d.fillGradient = false;
                }
            }
            */
        }


        protected void drawStyles() {
            parent.colorMode(PConstants.RGB, 255);

            if (stroke) {
                parent.stroke(strokeColor);
                parent.strokeWeight(strokeWeight);
                parent.strokeCap(strokeCap);
                parent.strokeJoin(strokeJoin);
            } else {
                parent.noStroke();
            }

            if (fill) {
                //System.out.println("filling " + PApplet.hex(fillColor));
                parent.fill(fillColor);
            } else {
                parent.noFill();
            }

            if (parent.g instanceof PGraphicsJava2D) {
                PGraphicsJava2D p2d = ((PGraphicsJava2D) parent.g);

                if (strokeGradient != null) {
                    p2d.strokeGradient = true;
                    p2d.strokeGradientObject = strokeGradientPaint;
                } else {
                    // need to shut off, in case parent object has a gradient applied
                    //p2d.strokeGradient = false;
                }
                if (fillGradient != null) {
                    p2d.fillGradient = true;
                    p2d.fillGradientObject = fillGradientPaint;
                } else {
                    // need to shut off, in case parent object has a gradient applied
                    //p2d.fillGradient = false;
                }
            }
        }
    }


    // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


    private class Group extends BaseObject {
        BaseObject[] objects;
        int objectCount;


        public Group(BaseObject parent, XMLElement graphics) {
            super(parent, graphics);

            XMLElement elements[] = graphics.getChildren();
            objects = new BaseObject[elements.length];

            for (int i = 0; i < elements.length; i++) {
                String name = elements[i].getName(); //getElement();
                XMLElement elem = elements[i];

                if (name.equals("g")) {
                    objects[objectCount++] = new Group(this, elem);

                } else if (name.equals("defs")) {
                    // generally this will contain gradient info, so may
                    // as well just throw it into a group element for parsing
                    objects[objectCount++] = new Group(this, elem);

                } else if (name.equals("line")) {
                    objects[objectCount++] = new Line(this, elem);

                } else if (name.equals("circle")) {
                    objects[objectCount++] = new Circle(this, elem);

                } else if (name.equals("ellipse")) {
                    objects[objectCount++] = new Ellipse(this, elem);

/*
                } else if (name.equals("font")) {
                	objects[objectCount++] = new Font(this, elem); 
*/
                		
                } else if (name.equals("rect")) {
                    objects[objectCount++] = new Rect(this, elem);

                } else if (name.equals("polygon")) {
                    objects[objectCount++] = new Poly(this, elem, true);

                } else if (name.equals("polyline")) {
                    objects[objectCount++] = new Poly(this, elem, false);

                } else if (name.equals("path")) {
                    objects[objectCount++] = new Path(this, elem);

                } else if (name.equals("radialGradient")) {
                    objects[objectCount++] = new RadialGradient(this, elem);

                } else if (name.equals("linearGradient")) {
                    objects[objectCount++] = new LinearGradient(this, elem);

                } else if (name.equals("text")) {
                    PApplet.println("Text is not currently handled, " +
                                    "convert text to outlines instead.");

                } else if (name.equals("filter")) {
                    PApplet.println("Filters are not supported.");

                } else if (name.equals("mask")) {
                    PApplet.println("Masks are not supported.");

                } else {
                    System.err.println("Ignoring  <" + name + "> tag.");
                }
            }
        }


        public void drawShape() {
            if (display) {
                for (int i = 0; i < objectCount; i++) {
                    objects[i].draw();
                }
            }
        }
    }


    // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


    abstract private class Gradient extends BaseObject {
        AffineTransform transform;

        float[] offset;
        int[] color;
        int count;

        public Gradient(BaseObject parent, XMLElement properties) {
            super(parent, properties);

            XMLElement elements[] = properties.getChildren();
            offset = new float[elements.length];
            color = new int[elements.length];

            // <stop  offset="0" style="stop-color:#967348"/>
            for (int i = 0; i < elements.length; i++) {
                XMLElement elem = elements[i];
                String name = elem.getName();
                if (name.equals("stop")) {
                    offset[count] = elem.getFloatAttribute("offset");
                    String style = elem.getStringAttribute("style");
                    Hashtable styles = parseStyleAttributes(style);

                    String colorStr = (String) styles.get("stop-color");
                    if (colorStr == null) colorStr = "#000000";
                    String opacityStr = (String) styles.get("stop-opacity");
                    if (opacityStr == null) opacityStr = "1";
                    int tupacity = (int) (PApplet.parseFloat(opacityStr) * 255);
                    color[count] = (tupacity << 24) |
                        Integer.parseInt(colorStr.substring(1), 16);
                    count++;
                    //System.out.println("this color is " + PApplet.hex(color[count]));
                    /*
                    int idx = farbe.indexOf("#");
                    if (idx != -1) {
                        color[count] = Integer.parseInt(farbe.substring(idx+1), 16);
                        count++;
                    } else {
                        System.err.println("problem with gradient stop " + properties);
                    }
                    */
                }
            }
        }

        abstract protected void drawShape();
    }


    static protected Hashtable parseStyleAttributes(String style) {
        Hashtable table = new Hashtable();
        String[] pieces = style.split(";");
        for (int i = 0; i < pieces.length; i++) {
            String[] parts = pieces[i].split(":");
            table.put(parts[0], parts[1]);
        }
        return table;
    }


    private class LinearGradient extends Gradient {
        float x1, y1, x2, y2;

        public LinearGradient(BaseObject parent, XMLElement properties) {
            super(parent, properties);

            this.x1 = properties.getFloatAttribute("x1");
            this.y1 = properties.getFloatAttribute("y1");
            this.x2 = properties.getFloatAttribute("x2");
            this.y2 = properties.getFloatAttribute("y2");

            String transformStr =
                properties.getStringAttribute("gradientTransform");
            if (transformStr != null) {
                this.transform = parseTransform(transformStr);

                Point2D t1 = transform.transform(new Point2D.Float(x1, y1), null);
                Point2D t2 = transform.transform(new Point2D.Float(x2, y2), null);
                this.x1 = (float) t1.getX();
                this.y1 = (float) t1.getY();
                this.x2 = (float) t2.getX();
                this.y2 = (float) t2.getY();

            }
        }

        protected void drawShape() {
        }
    }


    // complete version is here
    // http://www.w3.org/TR/SVG/coords.html#TransformAttribute
    AffineTransform parseTransform(String what) {
        if (what != null) {
            if (what.startsWith("matrix(") && what.endsWith(")")) {
                // columns go first with AT constructor
                what = what.substring(7, what.length() - 1);
                return new AffineTransform(PApplet.parseFloat(PApplet.split(what, ' ')));
            }
        }
        return null;
    }


    private class RadialGradient extends Gradient {
        float cx, cy, r;

        public RadialGradient(BaseObject parent, XMLElement properties) {
            super(parent, properties);

            this.cx = properties.getFloatAttribute("cx");
            this.cy = properties.getFloatAttribute("cy");
            this.r = properties.getFloatAttribute("r");

            String transformStr =
                properties.getStringAttribute("gradientTransform");
            if (transformStr != null) {
                this.transform = parseTransform(transformStr);

                Point2D t1 = transform.transform(new Point2D.Float(cx, cy), null);
                Point2D t2 = transform.transform(new Point2D.Float(cx + r, cy), null);
                this.cx = (float) t1.getX();
                this.cy = (float) t1.getY();
                this.r = (float) (t2.getX() - t1.getX());
            }
        }

        protected void drawShape() {
        }
    }


    // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


    private class Line extends BaseObject {

        float x1, y1, x2, y2;

        public Line(BaseObject parent, XMLElement properties) {
            super(parent, properties);
            this.x1 = properties.getFloatAttribute("x1");
            this.y1 = properties.getFloatAttribute("y1");
            this.x2 = properties.getFloatAttribute("x2");
            this.y2 = properties.getFloatAttribute("y2");
        }

        protected void drawShape() {
            parent.line(x1, y1, x2, y2);
        }
    }


    // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


    private class Circle extends BaseObject {

        float x, y, radius;

        public Circle(BaseObject parent, XMLElement properties) {
            super(parent, properties);
            this.x = properties.getFloatAttribute("cx");
            this.y = properties.getFloatAttribute("cy");
            this.radius = properties.getFloatAttribute("r") * 2;
        }

        protected void drawShape() {
            parent.ellipseMode(PConstants.CENTER);
            parent.ellipse(x, y, radius, radius);
        }
    }


    // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


    private class Ellipse extends BaseObject{

        float x, y, rx, ry;


        public Ellipse(BaseObject parent, XMLElement properties) {
            super(parent, properties);
            this.x = properties.getFloatAttribute("cx");
            this.y = properties.getFloatAttribute("cy");
            this.rx = properties.getFloatAttribute("rx") * 2;
            this.ry = properties.getFloatAttribute("ry") * 2;
        }

        protected void drawShape() {
            parent.ellipseMode(PConstants.CENTER);
            parent.ellipse(x, y, rx, ry);
        }
    }


    // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


    private class Rect extends BaseObject{

        float x, y, w, h;

        public Rect(BaseObject parent, XMLElement properties) {
            super(parent, properties);
            this.x = properties.getFloatAttribute("x");
            this.y = properties.getFloatAttribute("y");
            this.w = properties.getFloatAttribute("width");
            this.h = properties.getFloatAttribute("height");
        }

        protected void drawShape() {
            parent.rectMode(PConstants.CORNER);
            parent.rect(x, y, w, h);
        }
    }


    // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


    private class Poly extends BaseObject {

        float points[][] = null;
        /** true if polygon, false if polyline */
        boolean closed;

        public Poly(BaseObject parent, XMLElement properties, boolean closed) {
            super(parent, properties);
            String pointsBuffer[] = null;
            this.closed = closed;

            if (properties.hasAttribute("points")) {
                pointsBuffer = PApplet.splitTokens(properties.getStringAttribute("points"));
            }

            points = new float[pointsBuffer.length][2];
            for (int i = 0; i < points.length; i++) {
                String pb[] = PApplet.split(pointsBuffer[i], ',');
                points[i][0] = Float.valueOf(pb[0]).floatValue();
                points[i][1] = Float.valueOf(pb[1]).floatValue();
            }
        }

        protected void drawShape() {
            if (points != null)
                if (points.length > 0) {
                    parent.beginShape();
                    for (int i = 0; i < points.length; i++) {
                        parent.vertex(points[i][0], points[i][1]);
                    }
                    parent.endShape(closed ? PConstants.CLOSE : PConstants.OPEN);
                }
        }
    }


    // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


    public class Path extends BaseObject {

        public int count = 0;
        public float[] x = new float[4];
        public float[] y = new float[4];

        static public final int MOVETO = 0;
        static public final int LINETO = 1;
        static public final int CURVETO = 2;
        static public final int QCURVETO = 3;
        public int[] kind = new int[4];

        public boolean closed = false;


        public Path(BaseObject parent, XMLElement properties) {
            super(parent, properties);
            String pathDataBuffer = "";

            if (!properties.hasAttribute("d"))
                return;

            pathDataBuffer = properties.getStringAttribute("d");
            StringBuffer pathChars = new StringBuffer();

            boolean lastSeparate = false;

            for (int i = 0; i < pathDataBuffer.length(); i++) {
                char c = pathDataBuffer.charAt(i);
                boolean separate = false;

                if (c == 'M' || c == 'm' ||
                    c == 'L' || c == 'l' ||
                    c == 'H' || c == 'h' ||
                    c == 'V' || c == 'v' ||
                    c == 'C' || c == 'c' ||  // beziers
                    c == 'S' || c == 's' ||
                    c == 'Q' || c == 'q' ||  // quadratic beziers
                    c == 'T' || c == 't' ||
                    c == 'Z' || c == 'z' ||  // closepath 
                    c == ',') {
                    separate = true;
                    if (i != 0) {
                        pathChars.append("|");
                    }
                }
                if (c == 'Z' || c == 'z') {
                    separate = false;
                }
                if (c == '-' && !lastSeparate) {
                    pathChars.append("|");
                }
                if (c != ',') {
                    pathChars.append("" + pathDataBuffer.charAt(i));
                }
                if (separate && c != ',' && c != '-') {
                    pathChars.append("|");
                }
                lastSeparate = separate;
            }

            pathDataBuffer = pathChars.toString();

            //String pathDataKeys[] = PApplet.split(pathDataBuffer, '|');
            // use whitespace constant to get rid of extra spaces and CR or LF
            String pathDataKeys[] =
                PApplet.splitTokens(pathDataBuffer, "|" + PConstants.WHITESPACE);
            //for (int j = 0; j < pathDataKeys.length; j++) {
            //    PApplet.println(j + "\t" + pathDataKeys[j]);
            //}
            //PApplet.println(pathDataKeys);
            //PApplet.println();

            //float cp[] = {0, 0};
            float cx = 0;
            float cy = 0;

            int i = 0;
            //for (int i = 0; i < pathDataKeys.length; i++) {
            while (i < pathDataKeys.length) {
                char c = pathDataKeys[i].charAt(0);
                switch (c) {

                //M - move to (absolute)
                case 'M':
                    /*
                    cp[0] = PApplet.parseFloat(pathDataKeys[i + 1]);
                    cp[1] = PApplet.parseFloat(pathDataKeys[i + 2]);
                    float s[] = {cp[0], cp[1]};
                    i += 2;
                    points.add(s);
                    */
                    cx = PApplet.parseFloat(pathDataKeys[i + 1]);
                    cy = PApplet.parseFloat(pathDataKeys[i + 2]);
                    moveto(cx, cy);
                    i += 3;
                    break;


                //m - move to (relative)
                case 'm':
                    /*
                    cp[0] = cp[0] + PApplet.parseFloat(pathDataKeys[i + 1]);
                    cp[1] = cp[1] + PApplet.parseFloat(pathDataKeys[i + 2]);
                    float s[] = {cp[0], cp[1]};
                    i += 2;
                    points.add(s);
                    */
                    cx = cx + PApplet.parseFloat(pathDataKeys[i + 1]);
                    cy = cy + PApplet.parseFloat(pathDataKeys[i + 2]);
                    moveto(cx, cy);
                    i += 3;
                    break;


                case 'L':
                    cx = PApplet.parseFloat(pathDataKeys[i + 1]);
                    cy = PApplet.parseFloat(pathDataKeys[i + 2]);
                    lineto(cx, cy);
                    i += 3;
                    break;


                case 'l':
                    cx = cx + PApplet.parseFloat(pathDataKeys[i + 1]);
                    cy = cy + PApplet.parseFloat(pathDataKeys[i + 2]);
                    lineto(cx, cy);
                    i += 3;
                    break;


                // horizontal lineto absolute
                case 'H':
                    cx = PApplet.parseFloat(pathDataKeys[i + 1]);
                    lineto(cx, cy);
                    i += 2;
                    break;


                // horizontal lineto relative
                case 'h':
                    cx = cx + PApplet.parseFloat(pathDataKeys[i + 1]);
                    lineto(cx, cy);
                    i += 2;
                    break;


                case 'V':
                    cy = PApplet.parseFloat(pathDataKeys[i + 1]);
                    lineto(cx, cy);
                    i += 2;
                    break;


                case 'v':
                    cy = cy + PApplet.parseFloat(pathDataKeys[i + 1]);
                    lineto(cx, cy);
                    i += 2;
                    break;


                // C - curve to (absolute)
                case 'C': {
                    float ctrlX1 = PApplet.parseFloat(pathDataKeys[i + 1]);
                    float ctrlY1 = PApplet.parseFloat(pathDataKeys[i + 2]);
                    float ctrlX2 = PApplet.parseFloat(pathDataKeys[i + 3]);
                    float ctrlY2 = PApplet.parseFloat(pathDataKeys[i + 4]);
                    float endX = PApplet.parseFloat(pathDataKeys[i + 5]);
                    float endY = PApplet.parseFloat(pathDataKeys[i + 6]);
                    curveto(ctrlX1, ctrlY1, ctrlX2, ctrlY2, endX, endY);
                    cx = endX;
                    cy = endY;
                    i += 7;
                }
                break;

                // c - curve to (relative)
                case 'c': {
                    float ctrlX1 = cx + PApplet.parseFloat(pathDataKeys[i + 1]);
                    float ctrlY1 = cy + PApplet.parseFloat(pathDataKeys[i + 2]);
                    float ctrlX2 = cx + PApplet.parseFloat(pathDataKeys[i + 3]);
                    float ctrlY2 = cy + PApplet.parseFloat(pathDataKeys[i + 4]);
                    float endX = cx + PApplet.parseFloat(pathDataKeys[i + 5]);
                    float endY = cy + PApplet.parseFloat(pathDataKeys[i + 6]);
                    curveto(ctrlX1, ctrlY1, ctrlX2, ctrlY2, endX, endY);
                    cx = endX;
                    cy = endY;
                    i += 7;
                }
                break;

                // S - curve to shorthand (absolute)
                case 'S': {
                    float ppx = x[count-2];
                    float ppy = y[count-2];
                    float px = x[count-1];
                    float py = y[count-1];
                    float ctrlX1 = px + (px - ppx);
                    float ctrlY1 = py + (py - ppy);
                    float ctrlX2 = PApplet.parseFloat(pathDataKeys[i + 1]);
                    float ctrlY2 = PApplet.parseFloat(pathDataKeys[i + 2]);
                    float endX = PApplet.parseFloat(pathDataKeys[i + 3]);
                    float endY = PApplet.parseFloat(pathDataKeys[i + 4]);
                    curveto(ctrlX1, ctrlY1, ctrlX2, ctrlY2, endX, endY);
                    cx = endX;
                    cy = endY;
                    i += 5;
                }
                break;

                // s - curve to shorthand (relative)
                case 's': {
                    float ppx = x[count-2];
                    float ppy = y[count-2];
                    float px = x[count-1];
                    float py = y[count-1];
                    float ctrlX1 = px + (px - ppx);
                    float ctrlY1 = py + (py - ppy);
                    float ctrlX2 = cx + PApplet.parseFloat(pathDataKeys[i + 1]);
                    float ctrlY2 = cy + PApplet.parseFloat(pathDataKeys[i + 2]);
                    float endX = cx + PApplet.parseFloat(pathDataKeys[i + 3]);
                    float endY = cy + PApplet.parseFloat(pathDataKeys[i + 4]);
                    curveto(ctrlX1, ctrlY1, ctrlX2, ctrlY2, endX, endY);
                    cx = endX;
                    cy = endY;
                    i += 5;
                }
                break;

                // Q - quadratic curve to (absolute)
                case 'Q': {
                    float ctrlX = PApplet.parseFloat(pathDataKeys[i + 1]);
                    float ctrlY = PApplet.parseFloat(pathDataKeys[i + 2]);
                    float endX = PApplet.parseFloat(pathDataKeys[i + 3]);
                    float endY = PApplet.parseFloat(pathDataKeys[i + 4]);
                    curveto(ctrlX, ctrlY, endX, endY);
                    cx = endX;
                    cy = endY;
                    i += 5;
                }
                break;

                // q - quadratic curve to (relative)
                case 'q': {
                    float ctrlX = cx + PApplet.parseFloat(pathDataKeys[i + 1]);
                    float ctrlY = cy + PApplet.parseFloat(pathDataKeys[i + 2]);
                    float endX = cx + PApplet.parseFloat(pathDataKeys[i + 3]);
                    float endY = cy + PApplet.parseFloat(pathDataKeys[i + 4]);
                    curveto(ctrlX, ctrlY, endX, endY);
                    cx = endX;
                    cy = endY;
                    i += 5;
                }
                break;

                // T - quadratic curve to shorthand (absolute)
                // The control point is assumed to be the reflection of the 
                // control point on the previous command relative to the 
                // current point. (If there is no previous command or if the 
                // previous command was not a Q, q, T or t, assume the control 
                // point is coincident with the current point.) 
                case 'T': {
                    float ppx = x[count-2];
                    float ppy = y[count-2];
                    float px = x[count-1];
                    float py = y[count-1];
                    float ctrlX = px + (px - ppx);
                    float ctrlY = py + (py - ppy);
                    float endX = PApplet.parseFloat(pathDataKeys[i + 1]);
                    float endY = PApplet.parseFloat(pathDataKeys[i + 2]);
                    curveto(ctrlX, ctrlY, endX, endY);
                    cx = endX;
                    cy = endY;
                    i += 3;
                }
                break;

                // t - quadratic curve to shorthand (relative)
                case 't': {
                    float ppx = x[count-2];
                    float ppy = y[count-2];
                    float px = x[count-1];
                    float py = y[count-1];
                    float ctrlX = px + (px - ppx);
                    float ctrlY = py + (py - ppy);
                    float endX = cx + PApplet.parseFloat(pathDataKeys[i + 1]);
                    float endY = cy + PApplet.parseFloat(pathDataKeys[i + 2]);
                    curveto(ctrlX, ctrlY, endX, endY);
                    cx = endX;
                    cy = endY;
                    i += 3;
                }
                break;

                case 'Z':
                case 'z':
                    closed = true;
                    i++;
                    break;

                default:
                	String parsed = 
                		PApplet.join(PApplet.subset(pathDataKeys, 0, i), ",");
                	String unparsed = 
                		PApplet.join(PApplet.subset(pathDataKeys, i), ",");
                	System.err.println("parsed: " + parsed);
                	System.err.println("unparsed: " + unparsed);
                    throw new RuntimeException("shape command not handled: " + pathDataKeys[i]);
                }
            }
        }


        protected void moveto(float px, float py) {
            if (count == x.length) {
                x = PApplet.expand(x);
                y = PApplet.expand(y);
                kind = PApplet.expand(kind);
            }
            kind[count] = MOVETO;
            x[count] = px;
            y[count] = py;
            count++;
        }


        protected void lineto(float px, float py) {
            if (count == x.length) {
                x = PApplet.expand(x);
                y = PApplet.expand(y);
                kind = PApplet.expand(kind);
            }
            kind[count] = LINETO;
            x[count] = px;
            y[count] = py;
            count++;
        }


        /** Quadratic curveto command. */
        protected void curveto(float x1, float y1, float x2, float y2) {
            if (count + 2 >= x.length) {
                x = PApplet.expand(x);
                y = PApplet.expand(y);
                kind = PApplet.expand(kind);
            }
            kind[count] = QCURVETO;
            x[count] = x1;
            y[count] = y1;
            count++;
            x[count] = x2;
            y[count] = y2;
            count++;
        }


        /** Cubic curveto command. */
        protected void curveto(float x1, float y1, float x2, float y2, float x3, float y3) {
            if (count + 2 >= x.length) {
                x = PApplet.expand(x);
                y = PApplet.expand(y);
                kind = PApplet.expand(kind);
            }
            kind[count] = CURVETO;
            x[count] = x1;
            y[count] = y1;
            count++;
            x[count] = x2;
            y[count] = y2;
            count++;
            x[count] = x3;
            y[count] = y3;
            count++;
        }


        protected void drawShape() {
            parent.beginShape();

            parent.vertex(x[0], y[0]);
            int i = 1;  // moveto has the first point
            while (i < count) {
                switch (kind[i]) {
                case MOVETO:
                    parent.breakShape();
                    parent.vertex(x[i], y[i]);
                    i++;
                    break;

                case LINETO:
                    parent.vertex(x[i], y[i]);
                    i++;
                    break;

                case QCURVETO:  // doubles the control point
                    parent.bezierVertex(x[i], y[i], x[i+1], y[i+1], x[i+1], y[i+1]);
                    i += 2;
                    break;

                case CURVETO:
                    parent.bezierVertex(x[i], y[i], x[i+1], y[i+1], x[i+2], y[i+2]);
                    i += 3;
                    break;
                }
            }
            parent.endShape(closed ? PConstants.CLOSE : PConstants.OPEN);
        }
    }
}
