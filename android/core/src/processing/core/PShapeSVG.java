/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2006-11 Ben Fry and Casey Reas
  Copyright (c) 2004-06 Michael Chang

  This library is free software; you can redistribute it and/or
  modify it under the terms of the GNU Lesser General Public
  License version 2.1 as published by the Free Software Foundation.

  This library is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  Lesser General Public License for more details.

  You should have received a copy of the GNU Lesser General
  Public License along with this library; if not, write to the
  Free Software Foundation, Inc., 59 Temple Place, Suite 330,
  Boston, MA  02111-1307  USA
*/

package processing.core;

import processing.data.*;

import java.util.HashMap;

import android.graphics.*;


/**
 * SVG stands for Scalable Vector Graphics, a portable graphics format. It is
 * a vector format so it allows for infinite resolution and relatively small
 * file sizes. Most modern media software can view SVG files, including Adobe
 * products, Firefox, etc. Illustrator and Inkscape can edit SVG files.
 * <p>
 * We have no intention of turning this into a full-featured SVG library.
 * The goal of this project is a basic shape importer that is small enough
 * to be included with applets, meaning that its download size should be
 * in the neighborhood of 25-30k. Starting with release 0149, this library
 * has been incorporated into the core via the loadShape() command, because
 * vector shape data is just as important as the image data from loadImage().
 * <p>
 * For more sophisticated import/export, consider the
 * <A HREF="http://xmlgraphics.apache.org/batik/">Batik</A>
 * library from the Apache Software Foundation. Future improvements to this
 * library may focus on this properly supporting a specific subset of SVG,
 * for instance the simpler SVG profiles known as
 * <A HREF="http://www.w3.org/TR/SVGMobile/">SVG Tiny or Basic</A>,
 * although we still would not support the interactivity options.
 *
 * <p> <hr noshade> <p>
 *
 * A minimal example program using SVG:
 * (assuming a working moo.svg is in your data folder)
 *
 * <PRE>
 * PShape moo;
 *
 * void setup() {
 *   size(400, 400);
 *   moo = loadShape("moo.svg");
 * }
 * void draw() {
 *   background(255);
 *   shape(moo, mouseX, mouseY);
 * }
 * </PRE>
 *
 * This code is based on the Candy library written by Michael Chang, which was
 * later revised and expanded for use as a Processing core library by Ben Fry.
 * Thanks to Ricard Marxer Pinon for help with better Inkscape support in 0154.
 *
 * <p> <hr noshade> <p>
 *
 * Late October 2008 revisions from ricardmp, incorporated by fry (0154)
 * <UL>
 * <LI>Better style attribute handling, enabling better Inkscape support.
 * </UL>
 *
 * October 2008 revisions by fry (Processing 0149, pre-1.0)
 * <UL>
 * <LI> Candy is no longer a separate library, and is instead part of core.
 * <LI> Loading now works through loadShape()
 * <LI> Shapes are now drawn using the new PGraphics shape() method.
 * </UL>
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
public class PShapeSVG extends PShape {
  XML element;

  /// Values between 0 and 1.
  float opacity;
  float strokeOpacity;
  float fillOpacity;


  Gradient strokeGradient;
  Shader strokeGradientPaint;
  String strokeName;  // id of another object, gradients only?

  Gradient fillGradient;
  Shader fillGradientPaint;
  String fillName;  // id of another object


//  /**
//   * Initializes a new SVG Object with the given filename.
//   */
//  public PShapeSVG(PApplet parent, String filename) {
//    // this will grab the root document, starting <svg ...>
//    // the xml version and initial comments are ignored
//    this(parent.loadXML(filename));
//  }


  /**
   * Initializes a new SVG Object from the given PNode.
   */
  public PShapeSVG(XML svg) {
    this(null, svg, true);

    if (!svg.getName().equals("svg")) {
      throw new RuntimeException("root is not <svg>, it's <" + svg.getName() + ">");
    }

    // not proper parsing of the viewBox, but will cover us for cases where
    // the width and height of the object is not specified
    String viewBoxStr = svg.getString("viewBox");
    if (viewBoxStr != null) {
      int[] viewBox = PApplet.parseInt(PApplet.splitTokens(viewBoxStr));
      width = viewBox[2];
      height = viewBox[3];
    }

    // TODO if viewbox is not same as width/height, then use it to scale
    // the original objects. for now, viewbox only used when width/height
    // are empty values (which by the spec means w/h of "100%"
    String unitWidth = svg.getString("width");
    String unitHeight = svg.getString("height");
    if (unitWidth != null) {
      width = parseUnitSize(unitWidth);
      height = parseUnitSize(unitHeight);
    } else {
      if ((width == 0) || (height == 0)) {
        //throw new RuntimeException("width/height not specified");
        PGraphics.showWarning("The width and/or height is not " +
                              "readable in the <svg> tag of this file.");
        // For the spec, the default is 100% and 100%. For purposes
        // here, insert a dummy value because this is prolly just a
        // font or something for which the w/h doesn't matter.
        width = 1;
        height = 1;
      }
    }

    //root = new Group(null, svg);
//    parseChildren(svg);  // ?
  }


  protected PShapeSVG(PShapeSVG parent, XML properties, boolean parseKids) {
    // Need to set this so that findChild() works.
    // Otherwise 'parent' is null until addChild() is called later.
    this.parent = parent;

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

      strokeOpacity = 1;
      fillOpacity = 1;
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
    name = properties.getString("id");
    // @#$(* adobe illustrator mangles names of objects when re-saving
    if (name != null) {
      while (true) {
        String[] m = PApplet.match(name, "_x([A-Za-z0-9]{2})_");
        if (m == null) break;
        char repair = (char) PApplet.unhex(m[1]);
        name = name.replace(m[0], "" + repair);
      }
    }

    String displayStr = properties.getString("display", "inline");
    visible = !displayStr.equals("none");

    String transformStr = properties.getString("transform");
    if (transformStr != null) {
      matrix = parseTransform(transformStr);
    }

    if (parseKids) {
      parseColors(properties);
      parseChildren(properties);
    }
  }


  protected void parseChildren(XML graphics) {
    XML[] elements = graphics.getChildren();
    children = new PShape[elements.length];
    childCount = 0;

    for (XML elem : elements) {
      PShape kid = parseChild(elem);
      if (kid != null) {
//        if (kid.name != null) {
//          System.out.println("adding child " + kid.name);
//        }
        addChild(kid);
      }
    }
    children = (PShape[]) PApplet.subset(children, 0, childCount);
  }


  /**
   * Parse a child XML element.
   * Override this method to add parsing for more SVG elements.
   */
  protected PShape parseChild(XML elem) {
//    System.err.println("parsing child in pshape " + elem.getName());
    String name = elem.getName();
    PShapeSVG shape = null;

    if (name == null) {
      // just some whitespace that can be ignored (hopefully)

    } else if (name.equals("g")) {
      //return new BaseObject(this, elem);
      shape = new PShapeSVG(this, elem, true);

    } else if (name.equals("defs")) {
      // generally this will contain gradient info, so may
      // as well just throw it into a group element for parsing
      //return new BaseObject(this, elem);
      shape = new PShapeSVG(this, elem, true);

    } else if (name.equals("line")) {
      //return new Line(this, elem);
      //return new BaseObject(this, elem, LINE);
      shape = new PShapeSVG(this, elem, true);
      shape.parseLine();

    } else if (name.equals("circle")) {
      //return new BaseObject(this, elem, ELLIPSE);
      shape = new PShapeSVG(this, elem, true);
      shape.parseEllipse(true);

    } else if (name.equals("ellipse")) {
      //return new BaseObject(this, elem, ELLIPSE);
      shape = new PShapeSVG(this, elem, true);
      shape.parseEllipse(false);

    } else if (name.equals("rect")) {
      //return new BaseObject(this, elem, RECT);
      shape = new PShapeSVG(this, elem, true);
      shape.parseRect();

    } else if (name.equals("polygon")) {
      //return new BaseObject(this, elem, POLYGON);
      shape = new PShapeSVG(this, elem, true);
      shape.parsePoly(true);

    } else if (name.equals("polyline")) {
      //return new BaseObject(this, elem, POLYGON);
      shape = new PShapeSVG(this, elem, true);
      shape.parsePoly(false);

    } else if (name.equals("path")) {
      //return new BaseObject(this, elem, PATH);
      shape = new PShapeSVG(this, elem, true);
      shape.parsePath();

    } else if (name.equals("radialGradient")) {
      return new RadialGradient(this, elem);

    } else if (name.equals("linearGradient")) {
      return new LinearGradient(this, elem);

    } else if (name.equals("font")) {
      return new Font(this, elem);

//    } else if (name.equals("font-face")) {
//      return new FontFace(this, elem);

//    } else if (name.equals("glyph") || name.equals("missing-glyph")) {
//      return new FontGlyph(this, elem);

    } else if (name.equals("metadata")) {
      // fontforge just stuffs this in as a comment
      return null;

    } else if (name.equals("text")) {  // || name.equals("font")) {
      PGraphics.showWarning("Text and fonts in SVG files " +
                                  "are not currently supported, " +
                            "convert text to outlines instead.");

    } else if (name.equals("filter")) {
      PGraphics.showWarning("Filters are not supported.");

    } else if (name.equals("mask")) {
      PGraphics.showWarning("Masks are not supported.");

    } else if (name.equals("pattern")) {
      PGraphics.showWarning("Patterns are not supported.");

    } else if (name.equals("stop")) {
      // stop tag is handled by gradient parser, so don't warn about it

    } else if (name.equals("sodipodi:namedview")) {
      // these are always in Inkscape files, the warnings get tedious

    } else {
      PGraphics.showWarning("Ignoring <" + name + "> tag.");
//      new Exception().printStackTrace();
    }
    return shape;
  }


  protected void parseLine() {
    kind = LINE;
    family = PRIMITIVE;
    params = new float[] {
      getFloatWithUnit(element, "x1"),
      getFloatWithUnit(element, "y1"),
      getFloatWithUnit(element, "x2"),
      getFloatWithUnit(element, "y2")
    };
  }


  /**
   * Handles parsing ellipse and circle tags.
   * @param circle true if this is a circle and not an ellipse
   */
  protected void parseEllipse(boolean circle) {
    kind = ELLIPSE;
    family = PRIMITIVE;
    params = new float[4];

    params[0] = getFloatWithUnit(element, "cx");
    params[1] = getFloatWithUnit(element, "cy");

    float rx, ry;
    if (circle) {
      rx = ry = getFloatWithUnit(element, "r");
    } else {
      rx = getFloatWithUnit(element, "rx");
      ry = getFloatWithUnit(element, "ry");
    }
    params[0] -= rx;
    params[1] -= ry;

    params[2] = rx*2;
    params[3] = ry*2;
  }


  protected void parseRect() {
    kind = RECT;
    family = PRIMITIVE;
    params = new float[] {
      getFloatWithUnit(element, "x"),
      getFloatWithUnit(element, "y"),
      getFloatWithUnit(element, "width"),
      getFloatWithUnit(element, "height")
    };
  }


  /**
   * Parse a polyline or polygon from an SVG file.
   * @param close true if shape is closed (polygon), false if not (polyline)
   */
  protected void parsePoly(boolean close) {
    family = PATH;
    this.close = close;

    String pointsAttr = element.getString("points");
    if (pointsAttr != null) {
      String[] pointsBuffer = PApplet.splitTokens(pointsAttr);
      vertexCount = pointsBuffer.length;
      vertices = new float[vertexCount][2];
      for (int i = 0; i < vertexCount; i++) {
        String pb[] = PApplet.split(pointsBuffer[i], ',');
        vertices[i][X] = Float.valueOf(pb[0]).floatValue();
        vertices[i][Y] = Float.valueOf(pb[1]).floatValue();
      }
    }
  }


  protected void parsePath() {
    family = PATH;
    kind = 0;

    String pathData = element.getString("d");
    if (pathData == null || PApplet.trim(pathData).length() == 0) {
      return;
    }
    char[] pathDataChars = pathData.toCharArray();

    StringBuffer pathBuffer = new StringBuffer();
    boolean lastSeparate = false;

    for (int i = 0; i < pathDataChars.length; i++) {
      char c = pathDataChars[i];
      boolean separate = false;

      if (c == 'M' || c == 'm' ||
          c == 'L' || c == 'l' ||
          c == 'H' || c == 'h' ||
          c == 'V' || c == 'v' ||
          c == 'C' || c == 'c' ||  // beziers
          c == 'S' || c == 's' ||
          c == 'Q' || c == 'q' ||  // quadratic beziers
          c == 'T' || c == 't' ||
//          c == 'A' || c == 'a' ||  // elliptical arc
          c == 'Z' || c == 'z' ||  // closepath
          c == ',') {
        separate = true;
        if (i != 0) {
          pathBuffer.append("|");
        }
      }
      if (c == 'Z' || c == 'z') {
        separate = false;
      }
      if (c == '-' && !lastSeparate) {
        // allow for 'e' notation in numbers, e.g. 2.10e-9
        // http://dev.processing.org/bugs/show_bug.cgi?id=1408
        if (i == 0 || pathDataChars[i-1] != 'e') {
          pathBuffer.append("|");
        }
      }
      if (c != ',') {
        pathBuffer.append(c); //"" + pathDataBuffer.charAt(i));
      }
      if (separate && c != ',' && c != '-') {
        pathBuffer.append("|");
      }
      lastSeparate = separate;
    }

    // use whitespace constant to get rid of extra spaces and CR or LF
    String[] pathTokens =
      PApplet.splitTokens(pathBuffer.toString(), "|" + WHITESPACE);
    vertices = new float[pathTokens.length][2];
    vertexCodes = new int[pathTokens.length];

    float cx = 0;
    float cy = 0;
    int i = 0;

    char implicitCommand = '\0';
//    char prevCommand = '\0';
    boolean prevCurve = false;
    float ctrlX, ctrlY;
    // store values for closepath so that relative coords work properly
    float movetoX = 0;
    float movetoY = 0;

    while (i < pathTokens.length) {
      char c = pathTokens[i].charAt(0);
      if (((c >= '0' && c <= '9') || (c == '-')) && implicitCommand != '\0') {
        c = implicitCommand;
        i--;
      } else {
        implicitCommand = c;
      }
      switch (c) {

      case 'M':  // M - move to (absolute)
        cx = PApplet.parseFloat(pathTokens[i + 1]);
        cy = PApplet.parseFloat(pathTokens[i + 2]);
        movetoX = cx;
        movetoY = cy;
        parsePathMoveto(cx, cy);
        implicitCommand = 'L';
        i += 3;
        break;

      case 'm':  // m - move to (relative)
        cx = cx + PApplet.parseFloat(pathTokens[i + 1]);
        cy = cy + PApplet.parseFloat(pathTokens[i + 2]);
        parsePathMoveto(cx, cy);
        implicitCommand = 'l';
        i += 3;
        break;

      case 'L':
        cx = PApplet.parseFloat(pathTokens[i + 1]);
        cy = PApplet.parseFloat(pathTokens[i + 2]);
        parsePathLineto(cx, cy);
        i += 3;
        break;

      case 'l':
        cx = cx + PApplet.parseFloat(pathTokens[i + 1]);
        cy = cy + PApplet.parseFloat(pathTokens[i + 2]);
        parsePathLineto(cx, cy);
        i += 3;
        break;

        // horizontal lineto absolute
      case 'H':
        cx = PApplet.parseFloat(pathTokens[i + 1]);
        parsePathLineto(cx, cy);
        i += 2;
        break;

        // horizontal lineto relative
      case 'h':
        cx = cx + PApplet.parseFloat(pathTokens[i + 1]);
        parsePathLineto(cx, cy);
        i += 2;
        break;

      case 'V':
        cy = PApplet.parseFloat(pathTokens[i + 1]);
        parsePathLineto(cx, cy);
        i += 2;
        break;

      case 'v':
        cy = cy + PApplet.parseFloat(pathTokens[i + 1]);
        parsePathLineto(cx, cy);
        i += 2;
        break;

        // C - curve to (absolute)
      case 'C': {
        float ctrlX1 = PApplet.parseFloat(pathTokens[i + 1]);
        float ctrlY1 = PApplet.parseFloat(pathTokens[i + 2]);
        float ctrlX2 = PApplet.parseFloat(pathTokens[i + 3]);
        float ctrlY2 = PApplet.parseFloat(pathTokens[i + 4]);
        float endX = PApplet.parseFloat(pathTokens[i + 5]);
        float endY = PApplet.parseFloat(pathTokens[i + 6]);
        parsePathCurveto(ctrlX1, ctrlY1, ctrlX2, ctrlY2, endX, endY);
        cx = endX;
        cy = endY;
        i += 7;
        prevCurve = true;
      }
      break;

        // c - curve to (relative)
      case 'c': {
        float ctrlX1 = cx + PApplet.parseFloat(pathTokens[i + 1]);
        float ctrlY1 = cy + PApplet.parseFloat(pathTokens[i + 2]);
        float ctrlX2 = cx + PApplet.parseFloat(pathTokens[i + 3]);
        float ctrlY2 = cy + PApplet.parseFloat(pathTokens[i + 4]);
        float endX = cx + PApplet.parseFloat(pathTokens[i + 5]);
        float endY = cy + PApplet.parseFloat(pathTokens[i + 6]);
        parsePathCurveto(ctrlX1, ctrlY1, ctrlX2, ctrlY2, endX, endY);
        cx = endX;
        cy = endY;
        i += 7;
        prevCurve = true;
      }
      break;

      // S - curve to shorthand (absolute)
      // Draws a cubic Bézier curve from the current point to (x,y). The first
      // control point is assumed to be the reflection of the second control
      // point on the previous command relative to the current point.
      // (x2,y2) is the second control point (i.e., the control point
      // at the end of the curve). S (uppercase) indicates that absolute
      // coordinates will follow; s (lowercase) indicates that relative
      // coordinates will follow. Multiple sets of coordinates may be specified
      // to draw a polybézier. At the end of the command, the new current point
      // becomes the final (x,y) coordinate pair used in the polybézier.
      case 'S': {
        // (If there is no previous command or if the previous command was not
        // an C, c, S or s, assume the first control point is coincident with
        // the current point.)
        if (!prevCurve) {
          ctrlX = cx;
          ctrlY = cy;
        } else {
          float ppx = vertices[vertexCount-2][X];
          float ppy = vertices[vertexCount-2][Y];
          float px = vertices[vertexCount-1][X];
          float py = vertices[vertexCount-1][Y];
          ctrlX = px + (px - ppx);
          ctrlY = py + (py - ppy);
        }
        float ctrlX2 = PApplet.parseFloat(pathTokens[i + 1]);
        float ctrlY2 = PApplet.parseFloat(pathTokens[i + 2]);
        float endX = PApplet.parseFloat(pathTokens[i + 3]);
        float endY = PApplet.parseFloat(pathTokens[i + 4]);
        parsePathCurveto(ctrlX, ctrlY, ctrlX2, ctrlY2, endX, endY);
        cx = endX;
        cy = endY;
        i += 5;
        prevCurve = true;
      }
      break;

        // s - curve to shorthand (relative)
      case 's': {
        if (!prevCurve) {
          ctrlX = cx;
          ctrlY = cy;
        } else {
          float ppx = vertices[vertexCount-2][X];
          float ppy = vertices[vertexCount-2][Y];
          float px = vertices[vertexCount-1][X];
          float py = vertices[vertexCount-1][Y];
          ctrlX = px + (px - ppx);
          ctrlY = py + (py - ppy);
        }
        float ctrlX2 = cx + PApplet.parseFloat(pathTokens[i + 1]);
        float ctrlY2 = cy + PApplet.parseFloat(pathTokens[i + 2]);
        float endX = cx + PApplet.parseFloat(pathTokens[i + 3]);
        float endY = cy + PApplet.parseFloat(pathTokens[i + 4]);
        parsePathCurveto(ctrlX, ctrlY, ctrlX2, ctrlY2, endX, endY);
        cx = endX;
        cy = endY;
        i += 5;
        prevCurve = true;
      }
      break;

      // Q - quadratic curve to (absolute)
      // Draws a quadratic Bézier curve from the current point to (x,y) using
      // (x1,y1) as the control point. Q (uppercase) indicates that absolute
      // coordinates will follow; q (lowercase) indicates that relative
      // coordinates will follow. Multiple sets of coordinates may be specified
      // to draw a polybézier. At the end of the command, the new current point
      // becomes the final (x,y) coordinate pair used in the polybézier.
      case 'Q': {
        ctrlX = PApplet.parseFloat(pathTokens[i + 1]);
        ctrlY = PApplet.parseFloat(pathTokens[i + 2]);
        float endX = PApplet.parseFloat(pathTokens[i + 3]);
        float endY = PApplet.parseFloat(pathTokens[i + 4]);
        //parsePathQuadto(cx, cy, ctrlX, ctrlY, endX, endY);
        parsePathQuadto(ctrlX, ctrlY, endX, endY);
        cx = endX;
        cy = endY;
        i += 5;
        prevCurve = true;
      }
      break;

      // q - quadratic curve to (relative)
      case 'q': {
        ctrlX = cx + PApplet.parseFloat(pathTokens[i + 1]);
        ctrlY = cy + PApplet.parseFloat(pathTokens[i + 2]);
        float endX = cx + PApplet.parseFloat(pathTokens[i + 3]);
        float endY = cy + PApplet.parseFloat(pathTokens[i + 4]);
        //parsePathQuadto(cx, cy, ctrlX, ctrlY, endX, endY);
        parsePathQuadto(ctrlX, ctrlY, endX, endY);
        cx = endX;
        cy = endY;
        i += 5;
        prevCurve = true;
      }
      break;

      // T - quadratic curveto shorthand (absolute)
      // The control point is assumed to be the reflection of the control
      // point on the previous command relative to the current point.
      case 'T': {
        // If there is no previous command or if the previous command was
        // not a Q, q, T or t, assume the control point is coincident
        // with the current point.
        if (!prevCurve) {
          ctrlX = cx;
          ctrlY = cy;
        } else {
          float ppx = vertices[vertexCount-2][X];
          float ppy = vertices[vertexCount-2][Y];
          float px = vertices[vertexCount-1][X];
          float py = vertices[vertexCount-1][Y];
          ctrlX = px + (px - ppx);
          ctrlY = py + (py - ppy);
        }
        float endX = PApplet.parseFloat(pathTokens[i + 1]);
        float endY = PApplet.parseFloat(pathTokens[i + 2]);
        //parsePathQuadto(cx, cy, ctrlX, ctrlY, endX, endY);
        parsePathQuadto(ctrlX, ctrlY, endX, endY);
        cx = endX;
        cy = endY;
        i += 3;
        prevCurve = true;
      }
        break;

        // t - quadratic curveto shorthand (relative)
      case 't': {
        if (!prevCurve) {
          ctrlX = cx;
          ctrlY = cy;
        } else {
          float ppx = vertices[vertexCount-2][X];
          float ppy = vertices[vertexCount-2][Y];
          float px = vertices[vertexCount-1][X];
          float py = vertices[vertexCount-1][Y];
          ctrlX = px + (px - ppx);
          ctrlY = py + (py - ppy);
        }
        float endX = cx + PApplet.parseFloat(pathTokens[i + 1]);
        float endY = cy + PApplet.parseFloat(pathTokens[i + 2]);
        //parsePathQuadto(cx, cy, ctrlX, ctrlY, endX, endY);
        parsePathQuadto(ctrlX, ctrlY, endX, endY);
        cx = endX;
        cy = endY;
        i += 3;
        prevCurve = true;
      }
        break;

      case 'Z':
      case 'z':
        // since closing the path, the 'current' point needs
        // to return back to the last moveto location.
        // http://code.google.com/p/processing/issues/detail?id=1058
        cx = movetoX;
        cy = movetoY;
        close = true;
        i++;
        break;

      default:
        String parsed =
          PApplet.join(PApplet.subset(pathTokens, 0, i), ",");
        String unparsed =
          PApplet.join(PApplet.subset(pathTokens, i), ",");
        System.err.println("parsed: " + parsed);
        System.err.println("unparsed: " + unparsed);
        if (pathTokens[i].equals("a") || pathTokens[i].equals("A")) {
          String msg = "Sorry, elliptical arc support for SVG files " +
            "is not yet implemented (See issue 130 for updates)";
          throw new RuntimeException(msg);
        }
        throw new RuntimeException("shape command not handled: " + pathTokens[i]);
      }
//      prevCommand = c;
    }
  }


//      private void parsePathCheck(int num) {
//        if (vertexCount + num-1 >= vertices.length) {
//          //vertices = (float[][]) PApplet.expand(vertices);
//          float[][] temp = new float[vertexCount << 1][2];
//          System.arraycopy(vertices, 0, temp, 0, vertexCount);
//          vertices = temp;
//        }
//      }

  private void parsePathVertex(float x, float y) {
    if (vertexCount == vertices.length) {
      //vertices = (float[][]) PApplet.expand(vertices);
      float[][] temp = new float[vertexCount << 1][2];
      System.arraycopy(vertices, 0, temp, 0, vertexCount);
      vertices = temp;
    }
    vertices[vertexCount][X] = x;
    vertices[vertexCount][Y] = y;
    vertexCount++;
  }


  private void parsePathCode(int what) {
    if (vertexCodeCount == vertexCodes.length) {
      vertexCodes = PApplet.expand(vertexCodes);
    }
    vertexCodes[vertexCodeCount++] = what;
  }


  private void parsePathMoveto(float px, float py) {
    if (vertexCount > 0) {
      parsePathCode(BREAK);
    }
    parsePathCode(VERTEX);
    parsePathVertex(px, py);
  }


  private void parsePathLineto(float px, float py) {
    parsePathCode(VERTEX);
    parsePathVertex(px, py);
  }


  private void parsePathCurveto(float x1, float y1,
                                float x2, float y2,
                                float x3, float y3) {
    parsePathCode(BEZIER_VERTEX);
    parsePathVertex(x1, y1);
    parsePathVertex(x2, y2);
    parsePathVertex(x3, y3);
  }

//  private void parsePathQuadto(float x1, float y1,
//                               float cx, float cy,
//                               float x2, float y2) {
//    //System.out.println("quadto: " + x1 + "," + y1 + " " + cx + "," + cy + " " + x2 + "," + y2);
////    parsePathCode(BEZIER_VERTEX);
//    parsePathCode(QUAD_BEZIER_VERTEX);
//    // x1/y1 already covered by last moveto, lineto, or curveto
//
//    parsePathVertex(x1 + ((cx-x1)*2/3.0f), y1 + ((cy-y1)*2/3.0f));
//    parsePathVertex(x2 + ((cx-x2)*2/3.0f), y2 + ((cy-y2)*2/3.0f));
//    parsePathVertex(x2, y2);
//  }

  private void parsePathQuadto(float cx, float cy,
                               float x2, float y2) {
    //System.out.println("quadto: " + x1 + "," + y1 + " " + cx + "," + cy + " " + x2 + "," + y2);
//    parsePathCode(BEZIER_VERTEX);
    parsePathCode(QUAD_BEZIER_VERTEX);
    // x1/y1 already covered by last moveto, lineto, or curveto
    parsePathVertex(cx, cy);
    parsePathVertex(x2, y2);
  }


  /**
   * Parse the specified SVG matrix into a PMatrix2D. Note that PMatrix2D
   * is rotated relative to the SVG definition, so parameters are rearranged
   * here. More about the transformation matrices in
   * <a href="http://www.w3.org/TR/SVG/coords.html#TransformAttribute">this section</a>
   * of the SVG documentation.
   * @param matrixStr text of the matrix param.
   * @return a good old-fashioned PMatrix2D
   */
  static protected PMatrix2D parseTransform(String matrixStr) {
    matrixStr = matrixStr.trim();
    PMatrix2D outgoing = null;
    int start = 0;
    int stop = -1;
    while ((stop = matrixStr.indexOf(')', start)) != -1) {
      PMatrix2D m = parseSingleTransform(matrixStr.substring(start, stop+1));
      if (outgoing == null) {
        outgoing = m;
      } else {
        outgoing.apply(m);
      }
      start = stop + 1;
    }
    return outgoing;
  }


  static protected PMatrix2D parseSingleTransform(String matrixStr) {
    //String[] pieces = PApplet.match(matrixStr, "^\\s*(\\w+)\\((.*)\\)\\s*$");
    String[] pieces = PApplet.match(matrixStr, "[,\\s]*(\\w+)\\((.*)\\)");
    if (pieces == null) {
      System.err.println("Could not parse transform " + matrixStr);
      return null;
    }
    float[] m = PApplet.parseFloat(PApplet.splitTokens(pieces[2], ", "));
    if (pieces[1].equals("matrix")) {
      return new PMatrix2D(m[0], m[2], m[4], m[1], m[3], m[5]);

    } else if (pieces[1].equals("translate")) {
      float tx = m[0];
      float ty = (m.length == 2) ? m[1] : m[0];
      //return new float[] { 1, 0, tx,  0, 1, ty };
      return new PMatrix2D(1, 0, tx, 0, 1, ty);

    } else if (pieces[1].equals("scale")) {
      float sx = m[0];
      float sy = (m.length == 2) ? m[1] : m[0];
      //return new float[] { sx, 0, 0, 0, sy, 0 };
      return new PMatrix2D(sx, 0, 0,  0, sy, 0);

    } else if (pieces[1].equals("rotate")) {
      float angle = m[0];

      if (m.length == 1) {
        float c = PApplet.cos(angle);
        float s = PApplet.sin(angle);
        // SVG version is cos(a) sin(a) -sin(a) cos(a) 0 0
        return new PMatrix2D(c, -s, 0, s, c, 0);

      } else if (m.length == 3) {
        PMatrix2D mat = new PMatrix2D(0, 1, m[1],  1, 0, m[2]);
        mat.rotate(m[0]);
        mat.translate(-m[1], -m[2]);
        return mat; //.get(null);
      }

    } else if (pieces[1].equals("skewX")) {
      return new PMatrix2D(1, 0, 1,  PApplet.tan(m[0]), 0, 0);

    } else if (pieces[1].equals("skewY")) {
      return new PMatrix2D(1, 0, 1,  0, PApplet.tan(m[0]), 0);
    }
    return null;
  }


  protected void parseColors(XML properties) {
    if (properties.hasAttribute("opacity")) {
      String opacityText = properties.getString("opacity");
      setOpacity(opacityText);
    }

    if (properties.hasAttribute("stroke")) {
      String strokeText = properties.getString("stroke");
      setColor(strokeText, false);
    }

    if (properties.hasAttribute("stroke-opacity")) {
      String strokeOpacityText = properties.getString("stroke-opacity");
      setStrokeOpacity(strokeOpacityText);
    }

    if (properties.hasAttribute("stroke-width")) {
      // if NaN (i.e. if it's 'inherit') then default back to the inherit setting
      String lineweight = properties.getString("stroke-width");
      setStrokeWeight(lineweight);
    }

    if (properties.hasAttribute("stroke-linejoin")) {
      String linejoin = properties.getString("stroke-linejoin");
      setStrokeJoin(linejoin);
    }

    if (properties.hasAttribute("stroke-linecap")) {
      String linecap = properties.getString("stroke-linecap");
      setStrokeCap(linecap);
    }

    // fill defaults to black (though stroke defaults to "none")
    // http://www.w3.org/TR/SVG/painting.html#FillProperties
    if (properties.hasAttribute("fill")) {
      String fillText = properties.getString("fill");
      setColor(fillText, true);
    }

    if (properties.hasAttribute("fill-opacity")) {
      String fillOpacityText = properties.getString("fill-opacity");
      setFillOpacity(fillOpacityText);
    }

    if (properties.hasAttribute("style")) {
      String styleText = properties.getString("style");
      String[] styleTokens = PApplet.splitTokens(styleText, ";");

      //PApplet.println(styleTokens);
      for (int i = 0; i < styleTokens.length; i++) {
        String[] tokens = PApplet.splitTokens(styleTokens[i], ":");
        //PApplet.println(tokens);

        tokens[0] = PApplet.trim(tokens[0]);

        if (tokens[0].equals("fill")) {
          setColor(tokens[1], true);

        } else if(tokens[0].equals("fill-opacity")) {
          setFillOpacity(tokens[1]);

        } else if(tokens[0].equals("stroke")) {
          setColor(tokens[1], false);

        } else if(tokens[0].equals("stroke-width")) {
          setStrokeWeight(tokens[1]);

        } else if(tokens[0].equals("stroke-linecap")) {
          setStrokeCap(tokens[1]);

        } else if(tokens[0].equals("stroke-linejoin")) {
          setStrokeJoin(tokens[1]);

        } else if(tokens[0].equals("stroke-opacity")) {
          setStrokeOpacity(tokens[1]);

        } else if(tokens[0].equals("opacity")) {
          setOpacity(tokens[1]);

        } else {
          // Other attributes are not yet implemented
        }
      }
    }
  }


  void setOpacity(String opacityText) {
    opacity = PApplet.parseFloat(opacityText);
    strokeColor = ((int) (opacity * 255)) << 24 | strokeColor & 0xFFFFFF;
    fillColor = ((int) (opacity * 255)) << 24 | fillColor & 0xFFFFFF;
  }


  void setStrokeWeight(String lineweight) {
    strokeWeight = parseUnitSize(lineweight);
  }


  void setStrokeOpacity(String opacityText) {
    strokeOpacity = PApplet.parseFloat(opacityText);
    strokeColor = ((int) (strokeOpacity * 255)) << 24 | strokeColor & 0xFFFFFF;
  }


  void setStrokeJoin(String linejoin) {
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


  void setStrokeCap(String linecap) {
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


  void setFillOpacity(String opacityText) {
    fillOpacity = PApplet.parseFloat(opacityText);
    fillColor = ((int) (fillOpacity * 255)) << 24 | fillColor & 0xFFFFFF;
  }


  void setColor(String colorText, boolean isFill) {
    int opacityMask = fillColor & 0xFF000000;
    boolean visible = true;
    int color = 0;
    String name = "";
    Gradient gradient = null;
    Shader paint = null;
    if (colorText.equals("none")) {
      visible = false;
    } else if (colorText.equals("black")) {
      color = opacityMask;
    } else if (colorText.equals("white")) {
      color = opacityMask | 0xFFFFFF;
    } else if (colorText.startsWith("#")) {
      if (colorText.length() == 4) {
        // Short form: #ABC, transform to long form #AABBCC
        colorText = colorText.replaceAll("^#(.)(.)(.)$", "#$1$1$2$2$3$3");
      }
      color = opacityMask |
          (Integer.parseInt(colorText.substring(1), 16)) & 0xFFFFFF;
      //System.out.println("hex for fill is " + PApplet.hex(fillColor));
    } else if (colorText.startsWith("rgb")) {
      color = opacityMask | parseRGB(colorText);
    } else if (colorText.startsWith("url(#")) {
      name = colorText.substring(5, colorText.length() - 1);
//      PApplet.println("looking for " + name);
      Object object = findChild(name);
      //PApplet.println("found " + fillObject);
      if (object instanceof Gradient) {
        gradient = (Gradient) object;
        paint = calcGradientPaint(gradient); //, opacity);
        //PApplet.println("got filla " + fillObject);
      } else {
//        visible = false;
        System.err.println("url " + name + " refers to unexpected data: " + object);
      }
    }
    if (isFill) {
      fill = visible;
      fillColor = color;
      fillName = name;
      fillGradient = gradient;
      fillGradientPaint = paint;
    } else {
      stroke = visible;
      strokeColor = color;
      strokeName = name;
      strokeGradient = gradient;
      strokeGradientPaint = paint;
    }
  }


  static protected int parseRGB(String what) {
    int leftParen = what.indexOf('(') + 1;
    int rightParen = what.indexOf(')');
    String sub = what.substring(leftParen, rightParen);
    int[] values = PApplet.parseInt(PApplet.splitTokens(sub, ", "));
    return (values[0] << 16) | (values[1] << 8) | (values[2]);
  }


  static protected HashMap<String, String> parseStyleAttributes(String style) {
    HashMap<String, String> table = new HashMap<String, String>();
    String[] pieces = style.split(";");
    for (int i = 0; i < pieces.length; i++) {
      String[] parts = pieces[i].split(":");
      table.put(parts[0], parts[1]);
    }
    return table;
  }


  /**
   * Used in place of element.getFloatAttribute(a) because we can
   * have a unit suffix (length or coordinate).
   * @param element what to parse
   * @param attribute name of the attribute to get
   * @return unit-parsed version of the data
   */
  static protected float getFloatWithUnit(XML element, String attribute) {
    String val = element.getString(attribute);
    return (val == null) ? 0 : parseUnitSize(val);
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
  static protected float parseUnitSize(String text) {
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


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  static class Gradient extends PShapeSVG {
    Matrix transform;

    float[] offset;
    int[] color;
    int count;

    public Gradient(PShapeSVG parent, XML properties) {
      super(parent, properties, true);

      XML elements[] = properties.getChildren();
      offset = new float[elements.length];
      color = new int[elements.length];

      // <stop  offset="0" style="stop-color:#967348"/>
      for (int i = 0; i < elements.length; i++) {
        XML elem = elements[i];
        String name = elem.getName();
        if (name.equals("stop")) {
          String offsetAttr = elem.getString("offset");
          float div = 1.0f;
          if (offsetAttr.endsWith("%")) {
            div = 100.0f;
            offsetAttr = offsetAttr.substring(0, offsetAttr.length() - 1);
          }
          offset[count] = PApplet.parseFloat(offsetAttr) / div;
          String style = elem.getString("style");
          HashMap<String, String> styles = parseStyleAttributes(style);

          String colorStr = styles.get("stop-color");
          if (colorStr == null) colorStr = "#000000";
          String opacityStr = styles.get("stop-opacity");
          if (opacityStr == null) opacityStr = "1";
          int tupacity = (int) (PApplet.parseFloat(opacityStr) * 255);
          color[count] = (tupacity << 24) |
            Integer.parseInt(colorStr.substring(1), 16);
          count++;
        }
      }
      offset = PApplet.subset(offset, 0, count);
      color = PApplet.subset(color, 0, count);
    }
  }


  class LinearGradient extends Gradient {
    float x1, y1, x2, y2;

    public LinearGradient(PShapeSVG parent, XML properties) {
      super(parent, properties);

      this.x1 = getFloatWithUnit(properties, "x1");
      this.y1 = getFloatWithUnit(properties, "y1");
      this.x2 = getFloatWithUnit(properties, "x2");
      this.y2 = getFloatWithUnit(properties, "y2");

      String transformStr =
        properties.getString("gradientTransform");

      if (transformStr != null) {
        float t[] = parseTransform(transformStr).get(null);
        //this.transform = new AffineTransform(t[0], t[3], t[1], t[4], t[2], t[5]);
        transform = new Matrix();
        transform.setValues(new float[] {  // TODO don't create temp floats
          t[0], t[1], t[2],
          t[3], t[4], t[5],
          0, 0, 1
        });

//        Point2D t1 = transform.transform(new Point2D.Float(x1, y1), null);
//        Point2D t2 = transform.transform(new Point2D.Float(x2, y2), null);
        float[] t1 = new float[] { x1, y1 };
        float[] t2 = new float[] { x2, y2 };
        transform.mapPoints(t1);
        transform.mapPoints(t2);

//        this.x1 = (float) t1.getX();
//        this.y1 = (float) t1.getY();
//        this.x2 = (float) t2.getX();
//        this.y2 = (float) t2.getY();
        x1 = t1[0];
        y1 = t1[1];
        x2 = t2[0];
        y2 = t2[1];
      }
    }
  }


  class RadialGradient extends Gradient {
    float cx, cy, r;

    public RadialGradient(PShapeSVG parent, XML properties) {
      super(parent, properties);

      this.cx = getFloatWithUnit(properties, "cx");
      this.cy = getFloatWithUnit(properties, "cy");
      this.r  = getFloatWithUnit(properties, "r");

      String transformStr =
        properties.getString("gradientTransform");

      if (transformStr != null) {
        float t[] = parseTransform(transformStr).get(null);
//        this.transform = new AffineTransform(t[0], t[3], t[1], t[4], t[2], t[5]);
        transform = new Matrix();
        transform.setValues(new float[] {  // TODO don't create temp floats
          t[0], t[1], t[2],
          t[3], t[4], t[5],
          0, 0, 1
        });

//        Point2D t1 = transform.transform(new Point2D.Float(cx, cy), null);
//        Point2D t2 = transform.transform(new Point2D.Float(cx + r, cy), null);
        float[] t1 = new float[] { cx, cy };
        float[] t2 = new float[] { cx + r, cy };
        transform.mapPoints(t1);
        transform.mapPoints(t2);

//        this.cx = (float) t1.getX();
//        this.cy = (float) t1.getY();
//        this.r = (float) (t2.getX() - t1.getX());
        cx = t1[0];
        cy = t1[1];
        r = t2[0] - t1[0];
      }
    }
  }


/*
  class LinearGradientPaint implements Paint {
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
                                       (float) t2.getX(), (float) t2.getY());
    }

    public int getTransparency() {
      return TRANSLUCENT;  // why not.. rather than checking each color
    }

    public class LinearGradientContext implements PaintContext {
      int ACCURACY = 2;
      float tx1, ty1, tx2, ty2;

      public LinearGradientContext(float tx1, float ty1, float tx2, float ty2) {
        this.tx1 = tx1;
        this.ty1 = ty1;
        this.tx2 = tx2;
        this.ty2 = ty2;
      }

      public void dispose() { }

      public ColorModel getColorModel() { return ColorModel.getRGBdefault(); }

      public Raster getRaster(int x, int y, int w, int h) {
        WritableRaster raster =
          getColorModel().createCompatibleWritableRaster(w, h);

        int[] data = new int[w * h * 4];

        // make normalized version of base vector
        float nx = tx2 - tx1;
        float ny = ty2 - ty1;
        float len = (float) Math.sqrt(nx*nx + ny*ny);
        if (len != 0) {
          nx /= len;
          ny /= len;
        }

        int span = (int) PApplet.dist(tx1, ty1, tx2, ty2) * ACCURACY;
        if (span <= 0) {
          //System.err.println("span is too small");
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
              float px = (x + i) - tx1;
              float py = (y + j) - ty1;
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
  }
  */


  /*
  class RadialGradientPaint implements Paint {
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
      return new RadialGradientContext();
    }

    public int getTransparency() {
      return TRANSLUCENT;
    }

    public class RadialGradientContext implements PaintContext {
      int ACCURACY = 5;

      public void dispose() {}

      public ColorModel getColorModel() { return ColorModel.getRGBdefault(); }

      public Raster getRaster(int x, int y, int w, int h) {
        WritableRaster raster =
          getColorModel().createCompatibleWritableRaster(w, h);

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
  }
  */


  protected Shader calcGradientPaint(Gradient gradient) {
    // TODO just do this with the other parsing
    int[] colors = new int[gradient.count];
    int opacityMask = ((int) (opacity * 255)) << 24;
    for (int i = 0; i < gradient.count; i++) {
      colors[i] = opacityMask | (gradient.color[i] & 0xFFFFFF);
    }

    if (gradient instanceof LinearGradient) {
      LinearGradient grad = (LinearGradient) gradient;
//      return new LinearGradientPaint(grad.x1, grad.y1, grad.x2, grad.y2,
//                                     grad.offset, grad.color, grad.count,
//                                     opacity);
      return new android.graphics.LinearGradient(grad.x1, grad.y1,
                                                 grad.x2, grad.y2,
                                                 colors, grad.offset,
                                                 Shader.TileMode.CLAMP );

    } else if (gradient instanceof RadialGradient) {
      RadialGradient grad = (RadialGradient) gradient;
//      return new RadialGradientPaint(grad.cx, grad.cy, grad.r,
//                                     grad.offset, grad.color, grad.count,
//                                     opacity);
      return new android.graphics.RadialGradient(grad.cx, grad.cy, grad.r,
                                                 colors, grad.offset,
                                                 Shader.TileMode.CLAMP);
    }
    return null;
  }


//  protected Shader calcGradientPaint(Gradient gradient,
//                                    float x1, float y1, float x2, float y2) {
//    if (gradient instanceof LinearGradient) {
//      LinearGradient grad = (LinearGradient) gradient;
//      return new LinearGradientPaint(x1, y1, x2, y2,
//                                     grad.offset, grad.color, grad.count,
//                                     opacity);
//    }
//    throw new RuntimeException("Not a linear gradient.");
//  }


//  protected Shader calcGradientPaint(Gradient gradient,
//                                    float cx, float cy, float r) {
//    if (gradient instanceof RadialGradient) {
//      RadialGradient grad = (RadialGradient) gradient;
//      return new RadialGradientPaint(cx, cy, r,
//                                     grad.offset, grad.color, grad.count,
//                                     opacity);
//    }
//    throw new RuntimeException("Not a radial gradient.");
//  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  @Override
  protected void styles(PGraphics g) {
    super.styles(g);

    if (g instanceof PGraphicsAndroid2D) {
      PGraphicsAndroid2D gg = (PGraphicsAndroid2D) g;

      if (strokeGradient != null) {
//        gg.strokeGradient = true;
//        gg.strokeGradientObject = strokeGradientPaint;
        gg.strokePaint.setShader(strokeGradientPaint);
      }
      if (fillGradient != null) {
//        p2d.fillGradient = true;
//        p2d.fillGradientObject = fillGradientPaint;
        gg.fillPaint.setShader(fillGradientPaint);
      }
    }
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  //public void drawImpl(PGraphics g) {
  // do nothing
  //}


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  public class Font extends PShapeSVG {
    public FontFace face;

    public HashMap<String,FontGlyph> namedGlyphs;
    public HashMap<Character,FontGlyph> unicodeGlyphs;

    public int glyphCount;
    public FontGlyph[] glyphs;
    public FontGlyph missingGlyph;

    int horizAdvX;


    public Font(PShapeSVG parent, XML properties) {
      super(parent, properties, false);
//      handle(parent, properties);

      XML[] elements = properties.getChildren();

      horizAdvX = properties.getInt("horiz-adv-x", 0);

      namedGlyphs = new HashMap<String,FontGlyph>();
      unicodeGlyphs = new HashMap<Character,FontGlyph>();
      glyphCount = 0;
      glyphs = new FontGlyph[elements.length];

      for (int i = 0; i < elements.length; i++) {
        String name = elements[i].getName();
        XML elem = elements[i];
        if (name == null) {
          // skip it
        } else if (name.equals("glyph")) {
          FontGlyph fg = new FontGlyph(this, elem, this);
          if (fg.isLegit()) {
            if (fg.name != null) {
              namedGlyphs.put(fg.name, fg);
            }
            if (fg.unicode != 0) {
              unicodeGlyphs.put(new Character(fg.unicode), fg);
            }
          }
          glyphs[glyphCount++] = fg;

        } else if (name.equals("missing-glyph")) {
//          System.out.println("got missing glyph inside <font>");
          missingGlyph = new FontGlyph(this, elem, this);
        } else if (name.equals("font-face")) {
          face = new FontFace(this, elem);
        } else {
          System.err.println("Ignoring " + name + " inside <font>");
        }
      }
    }


    protected void drawShape() {
      // does nothing for fonts
    }


    public void drawString(PGraphics g, String str, float x, float y, float size) {
      // 1) scale by the 1.0/unitsPerEm
      // 2) scale up by a font size
      g.pushMatrix();
      float s =  size / (float) face.unitsPerEm;
      //System.out.println("scale is " + s);
      // swap y coord at the same time, since fonts have y=0 at baseline
      g.translate(x, y);
      g.scale(s, -s);
      char[] c = str.toCharArray();
      for (int i = 0; i < c.length; i++) {
        // call draw on each char (pulling it w/ the unicode table)
        FontGlyph fg = (FontGlyph) unicodeGlyphs.get(new Character(c[i]));
        if (fg != null) {
          fg.draw(g);
          // add horizAdvX/unitsPerEm to the x coordinate along the way
          g.translate(fg.horizAdvX, 0);
        } else {
          System.err.println("'" + c[i] + "' not available.");
        }
      }
      g.popMatrix();
    }


    public void drawChar(PGraphics g, char c, float x, float y, float size) {
      g.pushMatrix();
      float s =  size / (float) face.unitsPerEm;
      g.translate(x, y);
      g.scale(s, -s);
      FontGlyph fg = (FontGlyph) unicodeGlyphs.get(new Character(c));
      if (fg != null) g.shape(fg);
      g.popMatrix();
    }


    public float textWidth(String str, float size) {
      float w = 0;
      char[] c = str.toCharArray();
      for (int i = 0; i < c.length; i++) {
        // call draw on each char (pulling it w/ the unicode table)
        FontGlyph fg = (FontGlyph) unicodeGlyphs.get(new Character(c[i]));
        if (fg != null) {
          w += (float) fg.horizAdvX / face.unitsPerEm;
        }
      }
      return w * size;
    }
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  class FontFace extends PShapeSVG {
    int horizOriginX;  // dflt 0
    int horizOriginY;  // dflt 0
//    int horizAdvX;     // no dflt?
    int vertOriginX;   // dflt horizAdvX/2
    int vertOriginY;   // dflt ascent
    int vertAdvY;      // dflt 1em (unitsPerEm value)

    String fontFamily;
    int fontWeight;    // can also be normal or bold (also comma separated)
    String fontStretch;
    int unitsPerEm;    // dflt 1000
    int[] panose1;     // dflt "0 0 0 0 0 0 0 0 0 0"
    int ascent;
    int descent;
    int[] bbox;        // spec says comma separated, tho not w/ forge
    int underlineThickness;
    int underlinePosition;
    //String unicodeRange; // gonna ignore for now


    public FontFace(PShapeSVG parent, XML properties) {
      super(parent, properties, true);

      unitsPerEm = properties.getInt("units-per-em", 1000);
    }


    protected void drawShape() {
      // nothing to draw in the font face attribute
    }
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  public class FontGlyph extends PShapeSVG {  // extends Path
    public String name;
    char unicode;
    int horizAdvX;

    public FontGlyph(PShapeSVG parent, XML properties, Font font) {
      super(parent, properties, true);
      super.parsePath();  // ??

      name = properties.getString("glyph-name");
      String u = properties.getString("unicode");
      unicode = 0;
      if (u != null) {
        if (u.length() == 1) {
          unicode = u.charAt(0);
          //System.out.println("unicode for " + name + " is " + u);
        } else {
          System.err.println("unicode for " + name +
                             " is more than one char: " + u);
        }
      }
      if (properties.hasAttribute("horiz-adv-x")) {
        horizAdvX = properties.getInt("horiz-adv-x");
      } else {
        horizAdvX = font.horizAdvX;
      }
    }


    protected boolean isLegit() {  // TODO need a better way to handle this...
      return vertexCount != 0;
    }
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  /**
   * Get a particular element based on its SVG ID. When editing SVG by hand,
   * this is the id="" tag on any SVG element. When editing from Illustrator,
   * these IDs can be edited by expanding the layers palette. The names used
   * in the layers palette, both for the layers or the shapes and groups
   * beneath them can be used here.
   * <PRE>
   * // This code grabs "Layer 3" and the shapes beneath it.
   * PShape layer3 = svg.getChild("Layer 3");
   * </PRE>
   */
  @Override
  public PShape getChild(String name) {
    PShape found = super.getChild(name);
    if (found == null) {
      // Otherwise try with underscores instead of spaces
      // (this is how Illustrator handles spaces in the layer names).
      found = super.getChild(name.replace(' ', '_'));
    }
    // Set bounding box based on the parent bounding box
    if (found != null) {
//      found.x = this.x;
//      found.y = this.y;
      found.width = this.width;
      found.height = this.height;
    }
    return found;
  }


  /**
   * Prints out the SVG document. Useful for parsing.
   */
  public void print() {
    PApplet.println(element.toString());
  }
}
