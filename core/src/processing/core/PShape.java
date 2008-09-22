/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2006-08 Ben Fry and Casey Reas

  This library is free software; you can redistribute it and/or
  modify it under the terms of the GNU Lesser General Public
  License as published by the Free Software Foundation; either
  version 2.1 of the License, or (at your option) any later version.

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

import java.util.HashMap;

// take a look at the obj loader to see how this fits with things

// PShape.line() PShape.ellipse()?
// PShape s = beginShape()
// line()
// endShape(s)

abstract public class PShape implements PConstants {

  protected String name;

  protected int kind;
  //protected int drawMode;
  protected PMatrix3D matrix;

  // setAxis -> .x and .y to move x and y coords of origin
  protected float x;
  protected float y;
  protected float width;
  protected float height;
  
  // set to false if the object is hidden in the layers palette
  protected boolean visible = true;

  protected boolean stroke;
  protected int strokeColor;
  protected float strokeWeight; // default is 1
  protected int strokeCap;
  protected int strokeJoin;

  protected boolean fill;
  protected int fillColor;
  
  protected boolean styles = true;

  //public boolean hasTransform;
  //protected float[] transformation;

  int[] opcode;
  int opcodeCount;
  // need to reorder vertex fields to make a VERTEX_SHORT_COUNT
  // that puts all the non-rendering fields into later indices
  int dataCount;
  float[][] data;  // second param is the VERTEX_FIELD_COUNT
  // should this be called vertices (consistent with PGraphics internals)
  // or does that hurt flexibility?

  protected PShape parent;
  protected int childCount;
  protected PShape[] children;
  protected HashMap<String,PShape> table;

  // POINTS, LINES, xLINE_STRIP, xLINE_LOOP
  // TRIANGLES, TRIANGLE_STRIP, TRIANGLE_FAN
  // QUADS, QUAD_STRIP
  // xPOLYGON
//  static final int PATH = 1;  // POLYGON, LINE_LOOP, LINE_STRIP
//  static final int GROUP = 2;

  // how to handle rectmode/ellipsemode?
  // are they bitshifted into the constant?
  // CORNER, CORNERS, CENTER, (CENTER_RADIUS?)
//  static final int RECT = 3; // could just be QUAD, but would be x1/y1/x2/y2
//  static final int ELLIPSE = 4;
//
//  static final int VERTEX = 7;
//  static final int CURVE = 5;
//  static final int BEZIER = 6;


  // fill and stroke functions will need a pointer to the parent
  // PGraphics object.. may need some kind of createShape() fxn
  // or maybe the values are stored until draw() is called?

  // attaching images is very tricky.. it's a different type of data

  // material parameters will be thrown out,
  // except those currently supported (kinds of lights)

  // pivot point for transformations
//  public float px;
//  public float py;


  public PShape() {
    this.kind = GROUP;
  }
  
  
  public PShape(int kind) {
    this.kind = kind;
  }


  public void setName(String name) {
    this.name = name;
  }
  
  
  public String getName() {
    return name;
  }
  
  
  public boolean isVisible() {
    return visible;
  }
  
  
  public void setVisible(boolean visible) {
    this.visible = visible;
  }

  
  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  protected void checkBounds() {
    if (width == 0 || height == 0) {
      // calculate bounds here (also take kids into account)
      width = 1;
      height = 1;
    }
  }
  
  
  public float getWidth() {
    checkBounds();
    return width;
  }
  
  
  public float getHeight() {
    checkBounds();
    return height;
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

  
  boolean strokeSaved;
  int strokeColorSaved;
  float strokeWeightSaved;
  int strokeCapSaved;
  int strokeJoinSaved;
  
  boolean fillSaved;
  int fillColorSaved;

  int ellipseModeSaved;
  

  protected void pre(PGraphics g) {
    if (matrix != null) {
      boolean flat = g instanceof PGraphics2D;

      g.pushMatrix();
      if (flat) {
        g.applyMatrix(matrix.m00, matrix.m01, matrix.m03,  // PMatrix3D
                      matrix.m10, matrix.m11, matrix.m13);
      } else {
        g.applyMatrix(matrix.m00, matrix.m01, matrix.m02, matrix.m03,
                      matrix.m10, matrix.m11, matrix.m12, matrix.m13,
                      matrix.m20, matrix.m21, matrix.m22, matrix.m23,
                      matrix.m30, matrix.m31, matrix.m32, matrix.m33);
      }
    }

    strokeSaved = g.stroke;
    strokeColorSaved = g.strokeColor;
    strokeWeightSaved = g.strokeWeight;
    strokeCapSaved = g.strokeCap;
    strokeJoinSaved = g.strokeJoin;

    fillSaved = g.fill;
    fillColorSaved = g.fillColor;

    ellipseModeSaved = g.ellipseMode;

    if (styles) {
      styles(g);
    }
  }
  
  
  protected void styles(PGraphics g) {
    // should not be necessary because using only the int version of color
    //parent.colorMode(PConstants.RGB, 255);

    if (stroke) {
      g.stroke(strokeColor);
      g.strokeWeight(strokeWeight);
      g.strokeCap(strokeCap);
      g.strokeJoin(strokeJoin);
    } else {
      g.noStroke();
    }

    if (fill) {
      //System.out.println("filling " + PApplet.hex(fillColor));
      g.fill(fillColor);
    } else {
      g.noFill();
    }
  }


  public void post(PGraphics g) {
    for (int i = 0; i < childCount; i++) {
      children[i].draw(g);
    }
    
    // TODO this is not sufficient, since not saving fillR et al.
    g.stroke = strokeSaved;
    g.strokeColor = strokeColorSaved;
    g.strokeWeight = strokeWeightSaved;
    g.strokeCap = strokeCapSaved;
    g.strokeJoin = strokeJoinSaved;

    g.fill = fillSaved;
    g.fillColor = fillColorSaved;

    g.ellipseMode = ellipseModeSaved;

    if (matrix != null) {
      g.popMatrix();
    }
  }
  

  /**
   * Called by the following (the shape() command adds the g)
   * PShape s = loadShapes("blah.svg");
   * shape(s);
   */
  public void draw(PGraphics g) {
    //System.out.println("drawing " + getClass().getName());
    if (visible) {
      pre(g);
      drawImpl(g);
      post(g);
    }
  }
  

  /**
   * Draws the SVG document.
   */
  abstract public void drawImpl(PGraphics g);
  

  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

  
  public int getChildCount() {
    return childCount;
  }
  
  
  public PShape getChild(int index) {
    return children[index];
  }
  
  
  public PShape getChild(String who) {
    if (name != null && name.equals(who)) {
      return this;
    }
    if (table != null) {
      for (String n : table.keySet()) {
        if (n.equals(name)) {
          return table.get(name);
        }
      }
    }
    for (int i = 0; i < childCount; i++) {
      PShape found = children[i].getChild(name);
      if (found != null) return found;
    }
    return null;
  }
  
  
  /**
   * Same as getChild(name), except that it first walks all the way up the 
   * hierarchy to the farthest parent, so that children can be found anywhere. 
   */
  public PShape findChild(String name) {
    if (parent == null) {
      return getChild(name);
      
    } else {
      return parent.findChild(name);
    }
  }

  
  // can't be 'add' because that suggests additive geometry
  public void addChild(PShape who) {
    if (children == null) {
      children = new PShape[2];
    }
    if (childCount == children.length) {
      children = (PShape[]) PApplet.expand(children);
    }
    children[childCount++] = who;
    who.parent = this;

    /*
    String childName = who.getName();
    if (childName != null) {
      if (table == null) {
        table = new HashMap<String,PShape>();
      }
      table.put(childName, who);
    }
    */
  }


//  public PShape createGroup() {
//    PShape group = new PShape();
//    group.kind = GROUP;
//    addChild(group);
//    return group;
//  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  // translate, rotate, scale, apply (no push/pop)
  //   these each call matrix.translate, etc
  // if matrix is null when one is called,
  //   it is created and set to identity


  public void translate(float tx, float ty) {
    translate(tx, ty, 0);
  }

  public void translate(float tx, float ty, float tz) {
    checkMatrix();
    matrix.translate(tx, ty, 0);
  }


  public void rotateX(float angle) {
    rotate(angle, 1, 0, 0);
  }

  public void rotateY(float angle) {
    rotate(angle, 0, 1, 0);
  }

  public void rotateZ(float angle) {
    rotate(angle, 0, 0, 1);
  }

  public void rotate(float angle) {
    rotateZ(angle);
  }

  public void rotate(float angle, float v0, float v1, float v2) {
    checkMatrix();
    matrix.rotate(angle, v0, v1, v2);
  }


  //


  public void scale(float s) {
    scale(s, s, s);
  }

  
  public void scale(float sx, float sy) {
    scale(sx, sy, 1);
  }

  
  public void scale(float x, float y, float z) {
    checkMatrix();
    matrix.scale(x, y, z);
  }


  //


  public void resetMatrix() {
  }
  
  
  public void applyMatrix(float n00, float n01, float n02,
                          float n10, float n11, float n12) {
    checkMatrix();
    matrix.apply(n00, n01, n02, 0,
                 n10, n11, n12, 0,
                 0,   0,   1,   0,
                 0,   0,   0,   1);
  }

  
  public void applyMatrix(float n00, float n01, float n02, float n03,
                          float n10, float n11, float n12, float n13,
                          float n20, float n21, float n22, float n23,
                          float n30, float n31, float n32, float n33) {
    checkMatrix();
    matrix.apply(n00, n01, n02, n03,
                 n10, n11, n12, n13,
                 n20, n21, n22, n23,
                 n30, n31, n32, n33);
  }


  //


  protected void checkMatrix() {
    if (matrix == null) {
      matrix = new PMatrix3D();
    }
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  /**
   * Center the shape based on its bounding box. Can't assume
   * that the bounding box is 0, 0, width, height. Common case will be
   * opening a letter size document in Illustrator, and drawing something
   * in the middle, then reading it in as an svg file.
   * This will also need to flip the y axis (scale(1, -1)) in cases
   * like Adobe Illustrator where the coordinates start at the bottom.
   */
//  public void center() {
//  }


  /**
   * Set the pivot point for all transformations.
   */
//  public void pivot(float x, float y) {
//    px = x;
//    py = y;
//  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

}