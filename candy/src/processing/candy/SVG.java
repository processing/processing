/*
  Candy 2 - SVG Importer for Processing - http://processing.org

  Copyright (c) 2006 Michael Chang (Flux)
  http://www.ghost-hack.com/

  Revised and expanded by Ben Fry for inclusion as a core library
  Copyright (c) 2006- Ben Fry and Casey Reas

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
import java.awt.geom.*;
import java.awt.image.*;
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
 * Illustrator. We can't guarantee that it'll work for any SVGs created with 
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
 * void setup(){
 *   size(400,400);
 *   moo = new SVG("moo.svg",this);
 * }
 * void draw(){
 *   moo.draw();
 * }
 * </PRE>
 *
 * Note that processing.xml is imported as well. This is not needed
 * when running the app directly from Processing, as Candy will know
 * where it is. However when you export as an applet you will
 * also need to export processing.xml along with it to have working Candy.
 *
 * <p> <hr noshade> <p>
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
 * <LI> Now properly supports Processing-0118
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
 * If you experience any other wierdness or bugs, please file them to
 * flux.blackcat at gmail.com with subject: Delicious Candy
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
        this.parent = parent;
        //this.filename = filename;

        // this will grab the root document, starting <svg ...>
        // the xml version and initial comments are ignored
        svg = new XMLElement(parent, filename);

        if (!svg.getName().equals("svg")) {
            throw new RuntimeException("root isn't svg, it's <" + svg.getName() + ">");
        }

        width = parseUnitSize(svg.getStringAttribute("width"));
        height = parseUnitSize(svg.getStringAttribute("height"));

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
        root = new Group(svg);

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


    /**
     * Temporary hack for gradient handling. This is not supported 
     * and will be removed from future releases.
     */
    public void drawStyles() {
        //PApplet.println(root);
        
        if (root instanceof VectorObject) {
            ((VectorObject)root).drawStyles();
        } else {
            PApplet.println("Only use drawStyles() on an object, not a group.");
        }
    }
    

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

        boolean fill = parent.g.fill;
        int fillColor = parent.g.fillColor;

        int ellipseMode = parent.g.ellipseMode;

        root.draw();

        parent.g.stroke = stroke;
        parent.g.strokeColor = strokeColor;
        parent.g.strokeWeight = strokeWeight;

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
        
        public BaseObject(XMLElement properties) {
            element = properties;
            
            id = properties.getStringAttribute("id");
            if (id != null) {
                table.put(id, this);
                //System.out.println("now parsing " + id);
            }
            
            String displayStr = properties.getStringAttribute("display", "inline");
            display = !displayStr.equals("none");
        }

        protected abstract void drawShape();

        protected void draw() {
            drawShape();
        }
    }


    //Default vector graphics class from which all others will polymorph
    protected abstract class VectorObject extends BaseObject {

        boolean stroke;
        int strokeColor; // = transValue;
        float strokeWeight; // default is 1
        Gradient strokeGradient;
        Paint strokeGradientPaint;
        String strokeName;  // id of another object, gradients only?

        boolean fill;
        int fillColor; // = transValue;
        Gradient fillGradient;
        Paint fillGradientPaint;
        String fillName;  // id of another object

        boolean hasTransform; //= false;
        float[] transformation; //= null;

        float opacity;

        //Should we keep these here even when we don't have transforms?
        float rotation = 0;
        float translateX = 0;
        float translateY = 0;


        public VectorObject(XMLElement properties) {
            super(properties);
            
            getColors(properties);
            getTransformation(properties);
        }


        private void getTransformation(XMLElement properties){
            String transform = "";
            if (properties.hasAttribute("transform")){
                this.hasTransform = true;
                transform = properties.getStringAttribute("transform");
                transform = transform.substring(7, transform.length() - 2);
                String tf[] = PApplet.split(transform);

                this.transformation = new float[tf.length];
                for (int i = 0; i < transformation.length; i++)
                    this.transformation[i] = Float.valueOf(tf[i]).floatValue();
            }
            //Hacky code to get rotation working
            //Done through the powers of trial and error
            if (this.hasTransform){
                float t[] = this.transformation;
                if (t[0] < 0 && t[1] < 0 && t[2] > 0 && t[3] < 0)
                    this.rotation = -parent.acos(this.transformation[3]);
                if (t[0] > 0 && t[1] < 0 && t[2] > 0 && t[3] > 0)
                    this.rotation = parent.asin(this.transformation[1]);
                if (t[0] < 0 && t[1] > 0 && t[2] < 0 && t[3] < 0)
                    this.rotation = parent.acos(this.transformation[0]);
                if (t[0] > 0 && t[1] > 0 && t[2] < 0 && t[3] > 0)
                    this.rotation = parent.acos(this.transformation[0]);
                this.translateX = this.transformation[4];
                this.translateY = this.transformation[5];
            }
        }


        /*
        private int colorFromString(String color, String opacity){
            if (!color.equals("none")){
                color = color.substring(1, 7);
                color = opacity + color;
                return PApplet.unhex(color);
            }else{
                return transValue;
            }
        }
        */


        //We'll need color information like stroke, fill, opacity, stroke-weight
        protected void getColors(XMLElement properties){

            // opacity for postscript-derived things like svg affects both stroke and fill
            //if (properties.hasAttribute("opacity")) {
            opacity = properties.getFloatAttribute("opacity", 1);
            //}
            int opacityMask = ((int) (opacity * 255)) << 24;

            String strokeText = properties.getStringAttribute("stroke", "none");
            if (strokeText.equals("none")) {

            } else if (strokeText.startsWith("#")) {
                stroke = true;
                strokeColor = opacityMask |
                    (Integer.parseInt(strokeText.substring(1), 16)) & 0xFFFFFF;
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
            // strokeWeight defaults to 1, assuming that a stroke is present
            strokeWeight = properties.getFloatAttribute("stroke-width", 1);

            // fill defaults to black (though stroke defaults to "none")
            // http://www.w3.org/TR/SVG/painting.html#FillProperties
            String fillText = properties.getStringAttribute("fill", "#000000");
            if (fillText.equals("none")) {

            } else if (fillText.startsWith("#")) {
                fill = true;
                fillColor = opacityMask |
                    (Integer.parseInt(fillText.substring(1), 16)) & 0xFFFFFF;
                //System.out.println("hex for fill is " + PApplet.hex(fillColor));
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


        protected Paint calcGradientPaint(Gradient gradient) { //, float opacity) {
            if (gradient instanceof LinearGradient) {
                LinearGradient grad = (LinearGradient) gradient;

                /*
                Color c1 = new Color(0xFF000000 | grad.color[0]);
                Color c2 = new Color(0xFF000000 | grad.color[grad.count-1]);
                return new GradientPaint(grad.x1, grad.y1, c1,
                                         grad.x2, grad.y2, c2);
                                         */
                return new LinearGradientPaint(grad.x1, grad.y1, grad.x2, grad.y2,
                                               grad.offset, grad.color, grad.count,
                                               opacity);


            } else if (gradient instanceof RadialGradient) {
                RadialGradient grad = (RadialGradient) gradient;

                //Color c1 = new Color(0xFF000000 | grad.color[0]);
                //Color c2 = new Color(0xFF000000 | grad.color[grad.count-1]);
                return new RadialGradientPaint(grad.cx, grad.cy, grad.r,
                                               grad.offset, grad.color, grad.count,
                                               opacity);
            }
            return null;
        }


        protected abstract void drawShape();


        protected void draw(){
            if (!display) return;  // don't display if set invisible
            
            if (!ignoreStyles) {
                drawStyles();
            }

            if (hasTransform){
                parent.pushMatrix();
                parent.translate(translateX, translateY);
                parent.rotate(rotation);
            }

            drawShape();

            if (hasTransform){
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


    public class RadialGradientPaint implements Paint {
        float cx, cy, radius;
        float[] offset;
        int[] color;
        int count;
        float opacity;

        public RadialGradientPaint(float cx, float cy, float radius,
                                   float[] offset, int[] color, int count,
                                   float opacity) {
            this.cx = cx;
            this.cy = cy;
            this.radius = radius;
            this.offset = offset;
            this.color = color;
            this.count = count;
            this.opacity = opacity;
        }

        public PaintContext createContext(ColorModel cm,
                                          Rectangle deviceBounds, Rectangle2D userBounds,
                                          AffineTransform xform, RenderingHints hints) {
            Point2D transformedPoint =
                xform.transform(new Point2D.Float(cx, cy), null);
            // this causes problems
            //Point2D transformedRadius =
            //    xform.deltaTransform(new Point2D.Float(radius, radius), null);
            return new RadialGradientContext((float) transformedPoint.getX(),
                                             (float) transformedPoint.getY(),
                                             radius, //(float) transformedRadius.distance(0, 0),
                                             offset, color, count, opacity);
        }

        public int getTransparency() {
            /*
            int a1 = mPointColor.getAlpha();
            int a2 = mBackgroundColor.getAlpha();
            return (((a1 & a2) == 0xff) ? OPAQUE : TRANSLUCENT);
            */
            //return (opacity == 1) ? OPAQUE : TRANSLUCENT;
            return TRANSLUCENT;  // why not.. rather than checking each color
        }
    }


    public class RadialGradientContext implements PaintContext {
        float cx, cy, radius;
        float[] offset;
        int[] color;
        int count;
        float opacity;

        public RadialGradientContext(float cx, float cy, float radius,
                                     float[] offset, int[] color, int count,
                                     float opacity) {
            this.cx = cx;
            this.cy = cy;
            this.radius = radius;
            this.offset = offset;
            this.color = color;
            this.count = count;
            this.opacity = opacity;
        }

        public void dispose() {}

        public ColorModel getColorModel() { return ColorModel.getRGBdefault(); }

        int ACCURACY = 5;

        public Raster getRaster(int x, int y, int w, int h) {
            WritableRaster raster =
                getColorModel().createCompatibleWritableRaster(w, h);

            //System.out.println("radius here is " + radius);
            //System.out.println("count is " + count);
            int span = (int) radius * ACCURACY;
            int[][] interp = new int[span][4];
            int prev = 0;
            for (int i = 1; i < count; i++) {
                int c0 = color[i-1];
                int c1 = color[i];
                int last = (int) (offset[i] * (span - 1));
                for (int j = prev; j <= last; j++) {
                    float btwn = PApplet.norm(j, prev, last);
                    interp[j][0] = (int) PApplet.lerp((c0 >> 16) & 0xff, (c1 >> 16) & 0xff, btwn);
                    interp[j][1] = (int) PApplet.lerp((c0 >> 8) & 0xff, (c1 >> 8) & 0xff, btwn);
                    interp[j][2] = (int) PApplet.lerp(c0 & 0xff, c1 & 0xff, btwn);
                    interp[j][3] = (int) (PApplet.lerp((c0 >> 24) & 0xff, (c1 >> 24) & 0xff, btwn) * opacity);
                    //System.out.println(interp[j][3]);
                }
                prev = last;
            }

            int[] data = new int[w * h * 4];
            int index = 0;
            for (int j = 0; j < h; j++) {
                for (int i = 0; i < w; i++) {
                    float distance = PApplet.dist(cx, cy, x + i, y + j);
                    int which = PApplet.min((int) (distance * ACCURACY), interp.length-1);

                    data[index++] = interp[which][0];
                    data[index++] = interp[which][1];
                    data[index++] = interp[which][2];
                    data[index++] = interp[which][3];
                }
            }
            raster.setPixels(0, 0, w, h, data);

            return raster;
        }
    }


    public class LinearGradientPaint implements Paint {
        float x1, y1, x2, y2;
        float[] offset;
        int[] color;
        int count;
        float opacity;

        public LinearGradientPaint(float x1, float y1, float x2, float y2,
                                   float[] offset, int[] color, int count,
                                   float opacity) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
            this.offset = offset;
            this.color = color;
            this.count = count;
            this.opacity = opacity;
        }

        public PaintContext createContext(ColorModel cm,
                                          Rectangle deviceBounds, Rectangle2D userBounds,
                                          AffineTransform xform, RenderingHints hints) {
            Point2D t1 = xform.transform(new Point2D.Float(x1, y1), null);
            Point2D t2 = xform.transform(new Point2D.Float(x2, y2), null);
            return new LinearGradientContext((float) t1.getX(), (float) t1.getY(),
                                             (float) t2.getX(), (float) t2.getY(),
                                             offset, color, count, opacity);
        }

        public int getTransparency() {
            /*
            int a1 = mPointColor.getAlpha();
            int a2 = mBackgroundColor.getAlpha();
            return (((a1 & a2) == 0xff) ? OPAQUE : TRANSLUCENT);
            */
            //return OPAQUE;
            return TRANSLUCENT;  // why not.. rather than checking each color
        }
    }


    public class LinearGradientContext implements PaintContext {
        float x1, y1, x2, y2;
        float[] offset;
        int[] color;
        int count;
        float opacity;

        public LinearGradientContext(float x1, float y1, float x2, float y2,
                                     float[] offset, int[] color, int count,
                                     float opacity) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
            this.offset = offset;
            this.color = color;
            this.count = count;
            this.opacity = opacity;
        }

        public void dispose() { }

        public ColorModel getColorModel() { return ColorModel.getRGBdefault(); }

        int ACCURACY = 2;

        public Raster getRaster(int x, int y, int w, int h) {
            WritableRaster raster =
                getColorModel().createCompatibleWritableRaster(w, h);

            int[] data = new int[w * h * 4];
            
            // make normalized version of base vector
            float nx = x2 - x1;
            float ny = y2 - y1;
            float len = (float) Math.sqrt(nx*nx + ny*ny);
            if (len != 0) {
                nx /= len;
                ny /= len;
            }

            int span = (int) PApplet.dist(x1, y1, x2, y2) * ACCURACY;
            if (span <= 0) {
                // annoying edge case where the gradient isn't legit
                int index = 0;
                for (int j = 0; j < h; j++) {
                    for (int i = 0; i < w; i++) {
                        data[index++] = 0;
                        data[index++] = 0;
                        data[index++] = 0;
                        data[index++] = 255;
                    }
                }
                
            } else {
                //System.out.println("span is " + span + " " + x1 + " " + y1 + " " + x2 + " " + y2);
                int[][] interp = new int[span][4];
                int prev = 0;
                for (int i = 1; i < count; i++) {
                    int c0 = color[i-1];
                    int c1 = color[i];
                    int last = (int) (offset[i] * (span-1));
                    //System.out.println("last is " + last);
                    for (int j = prev; j <= last; j++) {
                        float btwn = PApplet.norm(j, prev, last);
                        interp[j][0] = (int) PApplet.lerp((c0 >> 16) & 0xff, (c1 >> 16) & 0xff, btwn);
                        interp[j][1] = (int) PApplet.lerp((c0 >> 8) & 0xff, (c1 >> 8) & 0xff, btwn);
                        interp[j][2] = (int) PApplet.lerp(c0 & 0xff, c1 & 0xff, btwn);
                        interp[j][3] = (int) (PApplet.lerp((c0 >> 24) & 0xff, (c1 >> 24) & 0xff, btwn) * opacity);
                        //System.out.println(j + " " + interp[j][0] + " " + interp[j][1] + " " + interp[j][2]);
                    }
                    prev = last;
                }

                int index = 0;
                for (int j = 0; j < h; j++) {
                    for (int i = 0; i < w; i++) {
                        //float distance = 0; //PApplet.dist(cx, cy, x + i, y + j);
                        //int which = PApplet.min((int) (distance * ACCURACY), interp.length-1);
                        float px = (x + i) - x1;
                        float py = (y + j) - y1;
                        // distance up the line is the dot product of the normalized
                        // vector of the gradient start/stop by the point being tested
                        int which = (int) ((px*nx + py*ny) * ACCURACY);
                        if (which < 0) which = 0;
                        if (which > interp.length-1) which = interp.length-1;
                        //if (which > 138) System.out.println("grabbing " + which);

                        data[index++] = interp[which][0];
                        data[index++] = interp[which][1];
                        data[index++] = interp[which][2];
                        data[index++] = interp[which][3];
                    }
                }
            }
            raster.setPixels(0, 0, w, h, data);

            return raster;
        }
    }


    // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


    private class Group extends BaseObject {
        BaseObject[] objects;
        int objectCount;


        public Group(XMLElement graphics) {
            super(graphics);

            XMLElement elements[] = graphics.getChildren();
            objects = new BaseObject[elements.length];

            for (int i = 0; i < elements.length; i++){
                String name = elements[i].getName(); //getElement();
                XMLElement elem = elements[i];

                if (name.equals("g")) {
                    objects[objectCount++] = new Group(elem);

                } else if (name.equals("defs")) {
                    // generally this will contain gradient info, so may
                    // as well just throw it into a group element for parsing
                    objects[objectCount++] = new Group(elem);

                } else if (name.equals("line")) {
                    objects[objectCount++] = new Line(elem);

                } else if (name.equals("circle")) {
                    objects[objectCount++] = new Circle(elem);

                } else if (name.equals("ellipse")) {
                    objects[objectCount++] = new Ellipse(elem);

                } else if (name.equals("rect")) {
                    objects[objectCount++] = new Rect(elem);

                } else if (name.equals("polygon")) {
                    objects[objectCount++] = new Poly(elem, true);

                } else if (name.equals("polyline")) {
                    objects[objectCount++] = new Poly(elem, false);

                } else if (name.equals("path")) {
                    objects[objectCount++] = new Path(elem);

                } else if (name.equals("radialGradient")) {
                    objects[objectCount++] = new RadialGradient(elem);

                } else if (name.equals("linearGradient")) {
                    objects[objectCount++] = new LinearGradient(elem);

                } else if (name.equals("text")) {
                    PApplet.println("Text is not currently handled, " + 
                                    "convert text to outlines instead.");
                    
                } else if (name.equals("filter")) {
                    PApplet.println("Filters are not supported.");
                    
                } else if (name.equals("mask")) {
                    PApplet.println("Masks are not supported.");
                    
                } else {
                    PApplet.println("not handled " + name);
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
        float[] offset;
        int[] color;
        int count;

        public Gradient(XMLElement properties) {
            super(properties);

            XMLElement elements[] = properties.getChildren();
            offset = new float[elements.length];
            color = new int[elements.length];

            // <stop  offset="0" style="stop-color:#967348"/>
            for (int i = 0; i < elements.length; i++){
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

        public LinearGradient(XMLElement properties) {
            super(properties);

            this.x1 = properties.getFloatAttribute("x1");
            this.y1 = properties.getFloatAttribute("y1");
            this.x2 = properties.getFloatAttribute("x2");
            this.y2 = properties.getFloatAttribute("y2");
        }

        protected void drawShape(){
        }
    }


    private class RadialGradient extends Gradient {
        float cx, cy, r;


        public RadialGradient(XMLElement properties) {
            super(properties);

            this.cx = properties.getFloatAttribute("cx");
            this.cy = properties.getFloatAttribute("cy");
            this.r = properties.getFloatAttribute("r");
        }

        protected void drawShape() {
        }
    }


    // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


    private class Line extends VectorObject{

        float x1, y1, x2, y2;

        public Line(XMLElement properties){
            super(properties);
            this.x1 = properties.getFloatAttribute("x1");
            this.y1 = properties.getFloatAttribute("y1");
            this.x2 = properties.getFloatAttribute("x2");
            this.y2 = properties.getFloatAttribute("y2");
        }

        protected void drawShape(){
            parent.line(x1, y1, x2, y2);
        }
    }


    // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


    private class Circle extends VectorObject{

        float x, y, radius;

        public Circle(XMLElement properties){
            super(properties);
            this.x = properties.getFloatAttribute("cx");
            this.y = properties.getFloatAttribute("cy");
            this.radius = properties.getFloatAttribute("r") * 2;
        }

        protected void drawShape(){
            parent.ellipseMode(PConstants.CENTER);
            parent.ellipse(x, y, radius, radius);
        }
    }


    // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


    private class Ellipse extends VectorObject{

        float x, y, rx, ry;


        public Ellipse(XMLElement properties){
            super(properties);
            this.x = properties.getFloatAttribute("cx");
            this.y = properties.getFloatAttribute("cy");
            this.rx = properties.getFloatAttribute("rx") * 2;
            this.ry = properties.getFloatAttribute("ry") * 2;
        }

        protected void drawShape(){
            parent.ellipseMode(PConstants.CENTER);
            parent.ellipse(x, y, rx, ry);
        }
    }


    // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


    private class Rect extends VectorObject{

        float x, y, w, h;

        public Rect(XMLElement properties){
            super(properties);
            this.x = properties.getFloatAttribute("x");
            this.y = properties.getFloatAttribute("y");
            this.w = properties.getFloatAttribute("width");
            this.h = properties.getFloatAttribute("height");
        }

        protected void drawShape(){
            parent.rectMode(PConstants.CORNER);
            parent.rect(x, y, w, h);
        }
    }


    // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


    private class Poly extends VectorObject {

        float points[][] = null;
        /** true if polygon, false if polyline */
        boolean closed;

        public Poly(XMLElement properties, boolean closed){
            super(properties);
            String pointsBuffer[] = null;
            this.closed = closed;

            if (properties.hasAttribute("points")) {
                //pointsBuffer = PApplet.split(properties.getStringAttribute("points"), ' ');
                pointsBuffer = PApplet.split(properties.getStringAttribute("points"));
            }

            points = new float[pointsBuffer.length][2];
            for (int i = 0; i < points.length; i++){
                String pb[] = PApplet.split(pointsBuffer[i], ',');
                points[i][0] = Float.valueOf(pb[0]).floatValue();
                points[i][1] = Float.valueOf(pb[1]).floatValue();
            }
        }

        protected void drawShape(){
            if (points != null)
                if (points.length > 0){
                    parent.beginShape();
                    for (int i = 0; i < points.length; i++){
                        parent.vertex(points[i][0], points[i][1]);
                    }
                    parent.endShape(closed ? PConstants.CLOSE : PConstants.OPEN);
                }
        }
    }


    // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


    private class Path extends VectorObject {

        //Vector points = new Vector();
        boolean closed = false;

        int count = 0;
        float[] x = new float[4];
        float[] y = new float[4];
        //boolean[] bezier = new boolean[4];

        static final int MOVETO = 0;
        static final int LINETO = 1;
        static final int CURVETO = 2;
        int[] kind = new int[4];


        //Hang on! This is going to be meaty.
        //Big and nasty constructor coming up....
        public Path(XMLElement properties){
            super(properties);
            String pathDataBuffer = "";

            if (!properties.hasAttribute("d"))
                return;

            pathDataBuffer = properties.getStringAttribute("d");
            StringBuffer pathChars = new StringBuffer();

            boolean lastSeparate = false;

            for (int i = 0; i < pathDataBuffer.length(); i++){
                char c = pathDataBuffer.charAt(i);
                boolean separate = false;

                if (c == 'M' || c == 'm' ||
                    c == 'L' || c == 'l' ||
                    c == 'H' || c == 'h' ||
                    c == 'V' || c == 'v' ||
                    c == 'C' || c == 'c' ||
                    c == 'S' || c == 's' ||
                    c == 'Z' || c == 'z' ||
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
                PApplet.split(pathDataBuffer, "|" + PConstants.WHITESPACE);
            //for (int j = 0; j < pathDataKeys.length; j++) {
            //    PApplet.println(j + "\t" + pathDataKeys[j]);
            //}
            //PApplet.println(pathDataKeys);
            //PApplet.println();

            //float cp[] = {0, 0};
            float cx = 0;
            float cy = 0;

            int i = 0;
            //for (int i = 0; i < pathDataKeys.length; i++){
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


                //C - curve to (absolute)
                case 'C': {
                    /*
                    float curvePA[] = {PApplet.parseFloat(pathDataKeys[i + 1]), PApplet.parseFloat(pathDataKeys[i + 2])};
                    float curvePB[] = {PApplet.parseFloat(pathDataKeys[i + 3]), PApplet.parseFloat(pathDataKeys[i + 4])};
                    float endP[] = {PApplet.parseFloat(pathDataKeys[i + 5]), PApplet.parseFloat(pathDataKeys[i + 6])};
                    cp[0] = endP[0];
                    cp[1] = endP[1];
                    i += 6;
                    points.add(curvePA);
                    points.add(curvePB);
                    points.add(endP);
                    */
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

                //c - curve to (relative)
                case 'c': {
                    /*
                    float curvePA[] = {cp[0] + PApplet.parseFloat(pathDataKeys[i + 1]), cp[1] + PApplet.parseFloat(pathDataKeys[i + 2])};
                    float curvePB[] = {cp[0] + PApplet.parseFloat(pathDataKeys[i + 3]), cp[1] + PApplet.parseFloat(pathDataKeys[i + 4])};
                    float endP[] = {cp[0] + PApplet.parseFloat(pathDataKeys[i + 5]), cp[1] + PApplet.parseFloat(pathDataKeys[i + 6])};
                    cp[0] = endP[0];
                    cp[1] = endP[1];
                    i += 6;
                    points.add(curvePA);
                    points.add(curvePB);
                    points.add(endP);
                    */
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

                //S - curve to shorthand (absolute)
                case 'S': {
                    /*
                    float lastPoint[] = (float[]) points.get(points.size() - 1);
                    float lastLastPoint[] = (float[]) points.get(points.size() - 2);
                    float curvePA[] = {cp[0] + (lastPoint[0] - lastLastPoint[0]),
                                       cp[1] + (lastPoint[1] - lastLastPoint[1])};
                    float curvePB[] = {PApplet.parseFloat(pathDataKeys[i + 1]),
                                       PApplet.parseFloat(pathDataKeys[i + 2])};
                    float e[] = {PApplet.parseFloat(pathDataKeys[i + 3]), PApplet.parseFloat(pathDataKeys[i + 4])};
                    cp[0] = e[0];
                    cp[1] = e[1];
                    points.add(curvePA);
                    points.add(curvePB);
                    points.add(e);
                    i += 4;
                    */
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

                //s - curve to shorthand (relative)
                case 's': {
                    /*
                    float lastPoint[] = (float[]) points.get(points.size() - 1);
                    float lastLastPoint[] = (float[]) points.get(points.size() - 2);
                    float curvePA[] = {cp[0] + (lastPoint[0] - lastLastPoint[0]), cp[1] + (lastPoint[1] - lastLastPoint[1])};
                    float curvePB[] = {cp[0] + PApplet.parseFloat(pathDataKeys[i + 1]), cp[1] + PApplet.parseFloat(pathDataKeys[i + 2])};
                    float e[] = {cp[0] + PApplet.parseFloat(pathDataKeys[i + 3]), cp[1] + PApplet.parseFloat(pathDataKeys[i + 4])};
                    cp[0] = e[0];
                    cp[1] = e[1];
                    points.add(curvePA);
                    points.add(curvePB);
                    points.add(e);
                    i += 4;
                    */
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

                case 'Z':
                case 'z':
                    closed = true;
                    i++;
                    break;

                default:
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


        protected void drawShape(){
            parent.beginShape();
            /*
            float start[] = (float[]) points.get(0);
            parent.vertex(start[0], start[1]);
            for (int i = 1; i < points.size(); i += 3){
                float a[] = (float[]) points.get(i);
                float b[] = (float[]) points.get(i + 1);
                float e[] = (float[]) points.get(i + 2);
                parent.bezierVertex(a[0], a[1], b[0], b[1], e[0], e[1]);
            }
            */

            /*
            for (int i = 0; i < count; i++) {
                PApplet.println(i + "\t" + x[i] + "\t" + y[i] + "\t" + bezier[i]);
            }
            PApplet.println();
            */

            parent.vertex(x[0], y[0]);
            int i = 1;  // moveto has the first point
            while (i < count) {
                switch (kind[i]) {
                case MOVETO:
                    /*
                    //if (i != 0) {
                    parent.endShape(closed ? PConstants.CLOSE : PConstants.OPEN);
                    closed = false;
                    parent.beginShape();
                    //}
                     */
                    parent.breakShape();
                    parent.vertex(x[i], y[i]);
                    i++;
                    break;

                case LINETO:
                    parent.vertex(x[i], y[i]);
                    i++;
                    break;

                case CURVETO:
                    parent.bezierVertex(x[i], y[i], x[i+1], y[i+1], x[i+2], y[i+2]);
                    i += 3;
                    break;
                }
            }
            parent.endShape(closed ? PConstants.CLOSE : PConstants.OPEN);
            /*
            if (closed)
                parent.endShape(PConstants.CLOSE);
            else
                parent.beginShape();
            //      p.endShape();
             */
        }
    }
}
