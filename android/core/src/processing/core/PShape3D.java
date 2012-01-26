/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2010 Ben Fry and Casey Reas

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

import processing.core.PGraphicsAndroid3D.InGeometry;
import processing.core.PGraphicsAndroid3D.TessGeometry;
import processing.core.PGraphicsAndroid3D.Tessellator;
import java.nio.FloatBuffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.io.BufferedReader;

/**
 * This class holds a 3D model composed of vertices, normals, colors (per vertex) and 
 * texture coordinates (also per vertex). All this data is stored in Vertex Buffer Objects
 * (VBO) in GPU memory for very fast access.
 * OBJ loading implemented using code from Saito's OBJLoader library (http://code.google.com/p/saitoobjloader/)
 * and OBJReader from Ahmet Kizilay (http://www.openprocessing.org/visuals/?visualID=191). 
 * By Andres Colubri
 * 
 * 
 * Other formats to consider:
 * AMF: http://en.wikipedia.org/wiki/Additive_Manufacturing_File_Format
 * STL: http://en.wikipedia.org/wiki/STL_(file_format)
 * OFF: http://en.wikipedia.org/wiki/STL_(file_format)
 * DXF: http://en.wikipedia.org/wiki/AutoCAD_DXF
 */

// Large VBOs
// http://www.gamedev.net/topic/590890-opengl-es-using-gldrawelements-with-integer-index-data/ 
// http://stackoverflow.com/questions/4331021/java-opengl-gldrawelements-with-32767-vertices

public class PShape3D extends PShape {
  protected PGraphicsAndroid3D renderer;
  protected PGL pgl;

  protected PShape3D root;  
  protected int glMode;
    
  protected InGeometry in;
  protected TessGeometry tess;
  protected Tessellator tessellator;
  
  protected ArrayList<IndexData> fillIndexData;
  protected ArrayList<IndexData> lineIndexData;
  protected ArrayList<IndexData> pointIndexData;
  
  protected HashSet<PImage> textures;
  protected PImage texture;
  
  // ........................................................
  
  // OpenGL buffers  
  
  public int glFillVertexBufferID;
  public int glFillColorBufferID;
  public int glFillNormalBufferID;
  public int glFillTexCoordBufferID;  
  public int glFillIndexBufferID;
  
  public int glLineVertexBufferID;
  public int glLineColorBufferID;
  public int glLineNormalBufferID;
  public int glLineAttribBufferID;
  public int glLineIndexBufferID;  
  
  public int glPointVertexBufferID;
  public int glPointColorBufferID;
  public int glPointNormalBufferID;
  public int glPointAttribBufferID;
  public int glPointIndexBufferID;  

  // ........................................................
  
  // Offsets for geometry aggregation and update.
  
  protected int fillVertCopyOffset;
  protected int fillIndCopyOffset;
  protected int lineVertCopyOffset;
  protected int lineIndCopyOffset;
  protected int pointVertCopyOffset;
  protected int pointIndCopyOffset;

  protected int lastFillVertexOffset;
  protected int lastFillIndexOffset;  
  protected int lastLineVertexOffset;
  protected int lastLineIndexOffset;    
  protected int lastPointVertexOffset;
  protected int lastPointIndexOffset;    
  
  // ........................................................
  
  // Drawing/rendering state
  
  protected boolean tessellated;
  
  boolean modifiedFillVertices;
  boolean modifiedFillColors;
  boolean modifiedFillNormals;
  boolean modifiedFillTexCoords;  
  
  boolean modifiedLineVertices;
  boolean modifiedLineColors;
  boolean modifiedLineNormals;
  boolean modifiedLineAttributes;  

  boolean modifiedPointVertices;
  boolean modifiedPointColors;
  boolean modifiedPointNormals;
  boolean modifiedPointAttributes;  

  protected VertexCache fillVerticesCache;
  protected VertexCache fillColorsCache;
  protected VertexCache fillNormalsCache;
  protected VertexCache fillTexCoordsCache;  
  
  protected VertexCache lineVerticesCache;
  protected VertexCache lineColorsCache;
  protected VertexCache lineNormalsCache;
  protected VertexCache lineAttributesCache;  

  protected VertexCache pointVerticesCache;
  protected VertexCache pointColorsCache;
  protected VertexCache pointNormalsCache;
  protected VertexCache pointAttributesCache;  
    
  protected boolean isSolid;
  protected boolean isClosed;
  
  protected boolean openContour = false;
  protected boolean breakShape = false;
  protected boolean shapeEnded = false;
  
  protected boolean hasFill;
  protected boolean hasLines;
  protected boolean hasPoints;
  
  protected boolean applyMatrix;
  protected boolean childHasMatrix;
  
  // ........................................................
  
  // Input data
  
  protected float normalX, normalY, normalZ;

  // normal calculated per triangle
  static protected final int NORMAL_MODE_AUTO = 0;
  // one normal manually specified per shape
  static protected final int NORMAL_MODE_SHAPE = 1;
  // normals specified for each shape vertex
  static protected final int NORMAL_MODE_VERTEX = 2;

  // Current mode for normals, one of AUTO, SHAPE, or VERTEX
  protected int normalMode;
  
  // ........................................................

  // Fill, stroke and tint colors

  protected boolean fill;
  protected float fillR, fillG, fillB, fillA;
  
  protected boolean stroke;
  protected float strokeR, strokeG, strokeB, strokeA;  
  
  protected boolean tint;
  protected float tintR, tintG, tintB, tintA;
  
  // ........................................................
  
  // Bezier and Catmull-Rom curves  

  protected boolean bezierInited = false;
  public int bezierDetail = 20;
  protected PMatrix3D bezierDrawMatrix;  

  protected boolean curveInited = false;
  protected int curveDetail = 20;
  public float curveTightness = 0;
  
  // catmull-rom basis matrix, perhaps with optional s parameter
  protected PMatrix3D curveBasisMatrix;
  protected PMatrix3D curveDrawMatrix;

  protected PMatrix3D bezierBasisInverse;
  protected PMatrix3D curveToBezierMatrix;

  protected float curveVertices[][];
  protected int curveVertexCount;  
  
  // ........................................................
  
  // Modes inherited from renderer  
  
  protected int textureMode;
  protected int rectMode;
  protected int ellipseMode;
  protected int shapeMode;
  protected int imageMode;
  
  public PShape3D(PApplet parent, int family) {
    renderer = (PGraphicsAndroid3D)parent.g;
    pgl = renderer.pgl;
    
    glMode = PGL.STATIC_DRAW;
    
    glFillVertexBufferID = 0;
    glFillColorBufferID = 0;
    glFillNormalBufferID = 0;
    glFillTexCoordBufferID = 0;
    glFillIndexBufferID = 0;
    
    glLineVertexBufferID = 0;
    glLineColorBufferID = 0;
    glLineNormalBufferID = 0;
    glLineAttribBufferID = 0;
    glLineIndexBufferID = 0;
    
    glPointVertexBufferID = 0;
    glPointColorBufferID = 0;
    glPointNormalBufferID = 0;
    glPointAttribBufferID = 0;
    glPointIndexBufferID = 0;
    
    this.tessellator = renderer.tessellator;
    this.family = family;    
    this.root = this;
    this.parent = null;
    this.tessellated = false;
    
    tess = renderer.newTessGeometry(RETAINED);    
    if (family == GEOMETRY || family == PRIMITIVE || family == PATH) {
      in = renderer.newInGeometry();      
    }
    
    // Modes are retrieved from the current values in the renderer.
    textureMode = renderer.textureMode;    
    rectMode = renderer.rectMode;
    ellipseMode = renderer.ellipseMode;
    shapeMode = renderer.shapeMode;
    imageMode = renderer.imageMode;    
    
    colorMode(renderer.colorMode, renderer.colorModeX, renderer.colorModeY, renderer.colorModeZ, renderer.colorModeA);
    
    // Initial values for fill, stroke and tint colors are also imported from the renderer.
    // This is particular relevant for primitive shapes, since is not possible to set 
    // their color separately when creating them, and their input vertices are actually
    // generated at rendering time, by which the color configuration of the renderer might
    // have changed.
    fill = renderer.fill;
    fillR = ((renderer.fillColor >> 16) & 0xFF) / 255.0f;    
    fillG = ((renderer.fillColor >>  8) & 0xFF) / 255.0f; 
    fillB = ((renderer.fillColor >>  0) & 0xFF) / 255.0f;
    fillA = ((renderer.fillColor >> 24) & 0xFF) / 255.0f;
      
    stroke = renderer.stroke;      
    strokeR = ((renderer.strokeColor >> 16) & 0xFF) / 255.0f;    
    strokeG = ((renderer.strokeColor >>  8) & 0xFF) / 255.0f; 
    strokeB = ((renderer.strokeColor >>  0) & 0xFF) / 255.0f;
    strokeA = ((renderer.strokeColor >> 24) & 0xFF) / 255.0f;

    strokeWeight = renderer.strokeWeight;    
    
    tint = renderer.tint;  
    tintR = ((renderer.tintColor >> 16) & 0xFF) / 255.0f;    
    tintG = ((renderer.tintColor >>  8) & 0xFF) / 255.0f; 
    tintB = ((renderer.tintColor >>  0) & 0xFF) / 255.0f;
    tintA = ((renderer.tintColor >> 24) & 0xFF) / 255.0f;
    
    normalX = normalY = 0; 
    normalZ = 1;
    
    normalMode = NORMAL_MODE_AUTO;
  }
  
  
  public void setKind(int kind) {
    this.kind = kind;
  }

  
  public void setMode(int mode) {
    if (mode == STATIC) {
      glMode = PGL.STATIC_DRAW;
    } else if (mode == DYNAMIC) {
      glMode = PGL.DYNAMIC_DRAW;
    } else if (mode == STREAM) {
      glMode = PGL.STREAM_DRAW;
    }
  }
  
  public void addChild(PShape child) {
    if (child instanceof PShape3D) {
      if (family == GROUP) {
        super.addChild(child);
        child.updateRoot(root);
        root.tessellated = false;
        tessellated = false;
        ((PShape3D)child).tessellated = false;
      } else {
        PGraphics.showWarning("Cannot add child shape to non-group shape.");
      }
    } else {
      PGraphics.showWarning("Shape must be 3D to be added to the group.");
    }
  }  
  
  public void updateRoot(PShape root) {
    this.root = (PShape3D) root;
    if (family == GROUP) {
      for (int i = 0; i < childCount; i++) {
        PShape3D child = (PShape3D)children[i];
        child.updateRoot(root);
      }
    }
  }      
  
  protected void finalize() throws Throwable {
    try {
      finalizeFillBuffers();  
      finalizeLineBuffers();
      finalizePointBuffers();
    } finally {
      super.finalize();
    }
  }
  
  protected void finalizeFillBuffers() {
    if (glFillVertexBufferID != 0) {    
      renderer.finalizeVertexBufferObject(glFillVertexBufferID);   
    }    
    
    if (glFillColorBufferID != 0) {    
      renderer.finalizeVertexBufferObject(glFillColorBufferID);   
    }    

    if (glFillNormalBufferID != 0) {    
      renderer.finalizeVertexBufferObject(glFillNormalBufferID);   
    }     

    if (glFillTexCoordBufferID != 0) {    
      renderer.finalizeVertexBufferObject(glFillTexCoordBufferID);   
    }    
    
    if (glFillIndexBufferID != 0) {    
      renderer.finalizeVertexBufferObject(glFillIndexBufferID);   
    }   
  }
  
  protected void finalizeLineBuffers() {
    if (glLineVertexBufferID != 0) {    
      renderer.finalizeVertexBufferObject(glLineVertexBufferID);   
    }    
    
    if (glLineColorBufferID != 0) {    
      renderer.finalizeVertexBufferObject(glLineColorBufferID);   
    }    

    if (glLineNormalBufferID != 0) {    
      renderer.finalizeVertexBufferObject(glLineNormalBufferID);   
    }     

    if (glLineAttribBufferID != 0) {    
      renderer.finalizeVertexBufferObject(glLineAttribBufferID);   
    }    
    
    if (glLineIndexBufferID != 0) {    
      renderer.finalizeVertexBufferObject(glLineIndexBufferID);   
    }  
  }  
  
  protected void finalizePointBuffers() {
    if (glPointVertexBufferID != 0) {    
      renderer.finalizeVertexBufferObject(glPointVertexBufferID);   
    }    
    
    if (glPointColorBufferID != 0) {    
      renderer.finalizeVertexBufferObject(glPointColorBufferID);   
    }    

    if (glPointNormalBufferID != 0) {    
      renderer.finalizeVertexBufferObject(glPointNormalBufferID);   
    }     

    if (glPointAttribBufferID != 0) {    
      renderer.finalizeVertexBufferObject(glPointAttribBufferID);   
    }    
    
    if (glPointIndexBufferID != 0) {    
      renderer.finalizeVertexBufferObject(glPointIndexBufferID);   
    }  
  }
    
  ///////////////////////////////////////////////////////////  
  
  //
  
  // Drawing methods  
  
  
  public void textureMode(int mode) {
    if (family == GROUP) {
      for (int i = 0; i < childCount; i++) {
        PShape3D child = (PShape3D) children[i];        
        child.textureMode(mode);        
      }         
    } else {    
      textureMode = mode;
    }
  }

  
  public void texture(PImage tex) {
    if (family == GROUP) {
      for (int i = 0; i < childCount; i++) {
        PShape3D child = (PShape3D) children[i];        
        child.texture(tex);        
      }      
    } else {
      PImage tex0 = texture;
      texture = tex;
      if (tex0 != tex && parent != null) {
        ((PShape3D)parent).removeTexture(tex);
      }      
      if (parent != null) {
        ((PShape3D)parent).addTexture(texture);
      }
    }        
  }

  
  public void noTexture() {
    if (family == GROUP) {
      for (int i = 0; i < childCount; i++) {
        PShape3D child = (PShape3D) children[i];        
        child.noTexture();        
      }
    } else {
      PImage tex0 = texture;
      texture = null;
      if (tex0 != null && parent != null) {
        ((PShape3D)parent).removeTexture(tex0);
      }      
    }
  }  

  
  protected void addTexture(PImage tex) {
    if (textures == null) {
      textures = new HashSet<PImage>();      
    }
    textures.add(tex);
    if (parent != null) {
      ((PShape3D)parent).addTexture(tex);
    }   
  }
  
  
  protected void removeTexture(PImage tex) {
    if (textures != null) {
      boolean childHasIt = false;
      
      for (int i = 0; i < childCount; i++) {
        PShape3D child = (PShape3D) children[i];        
        if (child.hasTexture(tex)) {
          childHasIt = true;
          break;
        }
      }
      
      if (!childHasIt) {
        textures.remove(tex);
        if (textures.size() == 0) {
          textures = null;
        }
      }
    }
  }
  
  
  protected boolean hasTexture(PImage tex) {
    if (family == GROUP) {
      return textures != null && textures.contains(tex);  
    } else {
      return texture == tex;
    }
  }
  
  
  public void solid(boolean solid) {
    if (family == GROUP) {
      for (int i = 0; i < childCount; i++) {
        PShape3D child = (PShape3D) children[i];        
        child.solid(solid);
      }
    } else {
      isSolid = solid;  
    }    
  }
  
  
  public void beginContour() {
    if (family == GROUP) {      
      PGraphics.showWarning("Cannot begin contour in GROUP shapes");
      return;
    }
    
    if (openContour) {
      PGraphics.showWarning("P3D: Already called beginContour().");
      return;
    }    
    openContour = true;    
  }
  
  
  public void endContour() {
    if (family == GROUP) {      
      PGraphics.showWarning("Cannot end contour in GROUP shapes");
      return;
    }
    
    if (!openContour) {
      PGraphics.showWarning("P3D: Need to call beginContour() first.");
      return;      
    }
    openContour = false;    
    breakShape = true;  
  }

  
  public void vertex(float x, float y) {
    vertex(x, y, 0, 0, 0);   
  }

  
  public void vertex(float x, float y, float u, float v) {
    vertex(x, y, 0, u, v); 
  }      
  
  
  public void vertex(float x, float y, float z) {
    vertex(x, y, z, 0, 0);      
  }

  
  public void vertex(float x, float y, float z, float u, float v) {
    vertexImpl(x, y, z, u, v, VERTEX);  
  }  
  
  
  protected void vertexImpl(float x, float y, float z, float u, float v, int code) {
    if (in.isFull()) {
      PGraphics.showWarning("P3D: Too many vertices, try creating smaller shapes");
      return;
    }
    
    if (family == GROUP) {      
      PGraphics.showWarning("Cannot add vertices to GROUP shape");
      return;
    }

    boolean textured = texture != null;
    float fR, fG, fB, fA;
    fR = fG = fB = fA = 0;
    if (fill || textured) {
      if (!textured) {
        fR = fillR;
        fG = fillG;
        fB = fillB;
        fA = fillA;
      } else {       
        if (tint) {
          fR = tintR;
          fG = tintG;
          fB = tintB;
          fA = tintA;
        } else {
          fR = 1;
          fG = 1;
          fB = 1;
          fA = 1;
        }
      }
    }    
    
    if (texture != null && textureMode == IMAGE) {
      u /= texture.width;
      v /= texture.height;
      
      PTexture tex = renderer.queryTexture(texture);
      if (tex != null && tex.isFlippedY()) {
        v = 1 - v;
      }          
    }
        
    float sR, sG, sB, sA, sW;
    sR = sG = sB = sA = sW = 0;
    if (stroke) {
      sR = strokeR;
      sG = strokeG;
      sB = strokeB;
      sA = strokeA;
      sW = strokeWeight;
    }     
    
    if (breakShape) {
      code = BREAK;
      breakShape = false;
    }    
    
    in.addVertex(x, y, z, 
                 fR, fG, fB, fA, 
                 normalX, normalY, normalZ,
                 u, v, 
                 sR, sG, sB, sA, sW, 
                 code);    
    
    root.tessellated = false;
    tessellated = false;  
  }
  
  
  public void normal(float nx, float ny, float nz) {
    if (family == GROUP) {      
      PGraphics.showWarning("Cannot set normal in GROUP shape");
      return;
    }
    
    normalX = nx;
    normalY = ny;
    normalZ = nz;
    
    // if drawing a shape and the normal hasn't been set yet,
    // then we need to set the normals for each vertex so far
    if (normalMode == NORMAL_MODE_AUTO) {
      // One normal per begin/end shape
      normalMode = NORMAL_MODE_SHAPE;
    } else if (normalMode == NORMAL_MODE_SHAPE) {
      // a separate normal for each vertex
      normalMode = NORMAL_MODE_VERTEX;
    } 
  }

  
  public void end() {
    end(OPEN);
  }  

  
  public void end(int mode) { 
    if (family == GROUP) {      
      PGraphics.showWarning("Cannot end GROUP shape");
      return;
    }
    
    isClosed = mode == CLOSE;    
    root.tessellated = false;
    tessellated = false;
    shapeEnded = true;
  }  
  
  
  public void setParams(float[] source) {
    if (family != PRIMITIVE) {      
      PGraphics.showWarning("Parameters can only be set to PRIMITIVE shapes");
      return;
    }
    
    super.setParams(source);
    root.tessellated = false;
    tessellated = false;
    shapeEnded = true;
  }

  //////////////////////////////////////////////////////////////

  // STROKE CAP/JOIN/WEIGHT

  
  public void strokeWeight(float weight) {
    if (family == GROUP) {
      for (int i = 0; i < childCount; i++) {
        PShape3D child = (PShape3D) children[i];        
        child.strokeWeight(weight);
      }
    } else {
      strokeWeight = weight;
    }    
  }


  public void strokeJoin(int join) {
    if (family == GROUP) {
      for (int i = 0; i < childCount; i++) {
        PShape3D child = (PShape3D) children[i];        
        child.strokeJoin(join);
      }
    } else {
      strokeJoin = join;
    }        
  }


  public void strokeCap(int cap) {
    if (family == GROUP) {
      for (int i = 0; i < childCount; i++) {
        PShape3D child = (PShape3D) children[i];        
        child.strokeCap(cap);
      }
    } else {
      strokeCap = cap;
    }    
  }
    
  
  //////////////////////////////////////////////////////////////

  // FILL COLOR

  
  public void noFill() {
    if (family == GROUP) {
      for (int i = 0; i < childCount; i++) {
        PShape3D child = (PShape3D) children[i];        
        child.noFill();        
      }      
    } else {
      fill = false;
      fillR = 0;
      fillG = 0;
      fillB = 0;
      fillA = 0;
      fillColor = 0x0;
      updateFillColor();      
    }
  }

  
  public void fill(int rgb) {
    if (family == GROUP) {
      for (int i = 0; i < childCount; i++) {
        PShape3D child = (PShape3D) children[i];        
        child.fill(rgb);        
      }      
    } else {
      colorCalc(rgb);
      fillFromCalc();        
    }
  }

  
  public void fill(int rgb, float alpha) {
    if (family == GROUP) {
      for (int i = 0; i < childCount; i++) {
        PShape3D child = (PShape3D) children[i];        
        child.fill(rgb, alpha);        
      }      
    } else {
      colorCalc(rgb, alpha);
      fillFromCalc();
    }    
  }

  
  public void fill(float gray) {
    if (family == GROUP) {
      for (int i = 0; i < childCount; i++) {
        PShape3D child = (PShape3D) children[i];        
        child.fill(gray);        
      }      
    } else {
      colorCalc(gray);
      fillFromCalc();      
    }
  }

  
  public void fill(float gray, float alpha) {
    if (family == GROUP) {
      for (int i = 0; i < childCount; i++) {
        PShape3D child = (PShape3D) children[i];        
        child.fill(gray, alpha);        
      }      
    } else {
      colorCalc(gray, alpha);
      fillFromCalc();
    }    
  }

  
  public void fill(float x, float y, float z) {
    if (family == GROUP) {
      for (int i = 0; i < childCount; i++) {
        PShape3D child = (PShape3D) children[i];        
        child.fill(x, y, z);        
      }      
    } else {
      colorCalc(x, y, z);
      fillFromCalc();
    }    
  }

  
  public void fill(float x, float y, float z, float a) {
    if (family == GROUP) {
      for (int i = 0; i < childCount; i++) {
        PShape3D child = (PShape3D) children[i];        
        child.fill(x, y, z, a);        
      }      
    } else {
      colorCalc(x, y, z, a);
      fillFromCalc();
    }    
  }
  

  protected void fillFromCalc() {
    fill = true;
    fillR = calcR;
    fillG = calcG;
    fillB = calcB;
    fillA = calcA;
    fillColor = calcColor;
    updateFillColor();  
  }

  
  protected void updateFillColor() {
    if (!shapeEnded || tess.fillVertexCount == 0 || texture != null) {
      return;
    }
      
    updateTesselation();
    
    int size = tess.fillVertexCount;
    float[] colors = tess.fillColors;
    int index;
    for (int i = 0; i < size; i++) {
      index = 4 * i;
      colors[index++] = fillR;
      colors[index++] = fillG;
      colors[index++] = fillB;
      colors[index  ] = fillA;
    }
    modifiedFillColors = true;
    modified();   
  }
  
    
  //////////////////////////////////////////////////////////////

  // STROKE COLOR 
  
  
  public void noStroke() {
    if (family == GROUP) {
      for (int i = 0; i < childCount; i++) {
        PShape3D child = (PShape3D) children[i];        
        child.noStroke();        
      }      
    } else {
      stroke = false;
      strokeR = 0;
      strokeG = 0;
      strokeB = 0;
      strokeA = 0;
      strokeColor = 0x0;
      updateStrokeColor();      
    }  
  }
  
  
  public void stroke(int rgb) {
    if (family == GROUP) {
      for (int i = 0; i < childCount; i++) {
        PShape3D child = (PShape3D) children[i];        
        child.stroke(rgb);        
      }      
    } else {
      colorCalc(rgb);
      strokeFromCalc();
    }    
  }
  
  
  public void stroke(int rgb, float alpha) {
    if (family == GROUP) {
      for (int i = 0; i < childCount; i++) {
        PShape3D child = (PShape3D) children[i];        
        child.stroke(rgb, alpha);        
      }      
    } else {
      colorCalc(rgb, alpha);
      strokeFromCalc();      
    }
  }

  
  public void stroke(float gray) {
    if (family == GROUP) {
      for (int i = 0; i < childCount; i++) {
        PShape3D child = (PShape3D) children[i];        
        child.stroke(gray);        
      }      
    } else {
      colorCalc(gray);
      strokeFromCalc();
    }    
  }

  
  public void stroke(float gray, float alpha) {
    if (family == GROUP) {
      for (int i = 0; i < childCount; i++) {
        PShape3D child = (PShape3D) children[i];        
        child.stroke(gray, alpha);        
      }      
    } else {
      colorCalc(gray, alpha);
      strokeFromCalc();      
    }
  }

  
  public void stroke(float x, float y, float z) {
    if (family == GROUP) {
      for (int i = 0; i < childCount; i++) {
        PShape3D child = (PShape3D) children[i];        
        child.stroke(x, y, z);        
      }      
    } else {
      colorCalc(x, y, z);
      strokeFromCalc();      
    }
  }

  
  public void stroke(float x, float y, float z, float alpha) {
    if (family == GROUP) {
      for (int i = 0; i < childCount; i++) {
        PShape3D child = (PShape3D) children[i];        
        child.stroke(x, y, z, alpha);        
      }      
    } else {
      colorCalc(x, y, z, alpha);
      strokeFromCalc();      
    }
  }
  
  
  protected void strokeFromCalc() {
    stroke = true;
    strokeR = calcR;
    strokeG = calcG;
    strokeB = calcB;
    strokeA = calcA;
    strokeColor = calcColor;
    updateStrokeColor();  
  }

  
  protected void updateStrokeColor() {
    if (shapeEnded) {
      updateTesselation();
      
      if (0 < tess.lineVertexCount) {
        int size = tess.lineVertexCount;
        float[] colors = tess.lineColors;
        int index;
        for (int i = 0; i < size; i++) {
          index = 4 * i;
          colors[index++] = strokeR;
          colors[index++] = strokeG;
          colors[index++] = strokeB;
          colors[index  ] = strokeA;
        }
        modifiedLineColors = true;
        modified();         
      }
      
      if (0 < tess.pointVertexCount) {
        int size = tess.pointVertexCount;
        float[] colors = tess.pointColors;
        int index;
        for (int i = 0; i < size; i++) {
          index = 4 * i;
          colors[index++] = strokeR;
          colors[index++] = strokeG;
          colors[index++] = strokeB;
          colors[index  ] = strokeA;
        }
        modifiedPointColors = true;
        modified();            
      }            
    }    
  }  

 
  //////////////////////////////////////////////////////////////

  // TINT COLOR 
  
  
  public void noTint() {
    if (family == GROUP) {
      for (int i = 0; i < childCount; i++) {
        PShape3D child = (PShape3D) children[i];        
        child.noTint();        
      }      
    } else {
      tint = false;
      tintR = 0;
      tintG = 0;
      tintB = 0;
      tintA = 0;
      tintColor = 0x0;
      updateTintColor();      
    }   
  }  
  
  
  public void tint(int rgb) {
    if (family == GROUP) {
      for (int i = 0; i < childCount; i++) {
        PShape3D child = (PShape3D) children[i];        
        child.tint(rgb);        
      }      
    } else {
      colorCalc(rgb);
      tintFromCalc();      
    }
  }  
  
  
  public void tint(int rgb, float alpha) {
    if (family == GROUP) {
      for (int i = 0; i < childCount; i++) {
        PShape3D child = (PShape3D) children[i];        
        child.tint(rgb, alpha);        
      }      
    } else {
      colorCalc(rgb, alpha);
      tintFromCalc();      
    }
  }
  
  
  public void tint(float gray) {
    if (family == GROUP) {
      for (int i = 0; i < childCount; i++) {
        PShape3D child = (PShape3D) children[i];        
        child.tint(gray);        
      }      
    } else {
      colorCalc(gray);
      tintFromCalc();      
    }    
  }
  
  
  public void tint(float gray, float alpha) {
    if (family == GROUP) {
      for (int i = 0; i < childCount; i++) {
        PShape3D child = (PShape3D) children[i];        
        child.tint(gray, alpha);        
      }      
    } else {
      colorCalc(gray, alpha);
      tintFromCalc();      
    }    
  }
  

  public void tint(float x, float y, float z) {
    if (family == GROUP) {
      for (int i = 0; i < childCount; i++) {
        PShape3D child = (PShape3D) children[i];        
        child.tint(x, y, z);        
      }      
    } else {
      colorCalc(x, y, z);
      tintFromCalc();      
    }    
  }
  
  
  public void tint(float x, float y, float z, float alpha) {
    if (family == GROUP) {
      for (int i = 0; i < childCount; i++) {
        PShape3D child = (PShape3D) children[i];        
        child.tint(x, y, z, alpha);        
      }      
    } else {
      colorCalc(x, y, z, alpha);
      tintFromCalc();      
    }        
  }  
  
  
  protected void tintFromCalc() {
    tint = true;
    tintR = calcR;
    tintG = calcG;
    tintB = calcB;
    tintA = calcA;
    tintColor = calcColor;
    updateTintColor();  
  }  
  
  
  protected void updateTintColor() {    
    if (!shapeEnded || tess.fillVertexCount == 0 || texture == null) {
      return;
    }
      
    updateTesselation();
    
    int size = tess.fillVertexCount;
    float[] colors = tess.fillColors;
    int index;
    for (int i = 0; i < size; i++) {
      index = 4 * i;
      colors[index++] = tintR;
      colors[index++] = tintG;
      colors[index++] = tintB;
      colors[index  ] = tintA;
    }
    modifiedFillColors = true;
    modified();  
  }
  
  ///////////////////////////////////////////////////////////  
  
  //
  
  // Geometric transformations
  
  
  protected int updateCenter(PVector vec, int count) {
    if (family == GROUP) {
      count = updateCenter(vec, count); 
    } else {      
      count += tess.getCenter(vec);      
    }
    return count;
  }
  
  
  public void center(float cx, float cy) {
    if (family == GROUP) {
      PVector center = new PVector();
            
      int count = updateCenter(center, 0);
      center.x /= count;   
      center.y /= count;
      
      float tx = cx - center.x;
      float ty = cy - center.y;      
      
      childHasMatrix();
      applyMatrix = true;
      super.translate(tx, ty);
    } else {
      PVector vec = new PVector();
      int count = tess.getCenter(vec);
      vec.x /= count;
      vec.y /= count;
      
      float tx = cx - vec.x;
      float ty = cy - vec.y;
      
      translate(tx, ty);   
    }
  }

  public void center(float cx, float cy, float cz) {
    if (family == GROUP) {
      PVector center0 = new PVector();
      
      int count = updateCenter(center0, 0);
      center0.x /= count;   
      center0.y /= count;
      center0.z /= count;
      
      float tx = cx - center0.x;
      float ty = cy - center0.y;
      float tz = cz - center0.z;
      
      childHasMatrix();
      applyMatrix = true;
      super.translate(tx, ty, tz);
    } else {
      PVector vec = new PVector();
      int count = tess.getCenter(vec);
      vec.x /= count;
      vec.y /= count;
      vec.z /= count;
      
      float tx = cx - vec.x;
      float ty = cy - vec.y;
      float tz = cz - vec.z;
      
      translate(tx, ty, tz); 
    }
  }  
  
  public void translate(float tx, float ty) {
    if (family == GROUP) {
      childHasMatrix();
      applyMatrix = true;
      super.translate(tx, ty);
    } else {
      if (!shapeEnded) {
        PGraphics.showWarning("Transformations can be applied only after the shape has been ended.");
        return;
      }
      
      checkMatrix(2);
      matrix.reset();
      matrix.translate(tx, ty);
      tess.applyMatrix((PMatrix2D) matrix);
       
      modified();
      if (0 < tess.fillVertexCount) {
        modifiedFillVertices = true;  
        modifiedFillNormals = true; 
      }        
      if (0 < tess.lineVertexCount) {
        modifiedLineVertices = true;
        modifiedLineNormals = true;
        modifiedLineAttributes = true;
      }
      if (0 < tess.pointVertexCount) {
        modifiedPointVertices = true;
        modifiedPointNormals = true;        
      }
      
      // So the transformation is not applied again when drawing
      applyMatrix = false; 
    }    
  }
  
  public void translate(float tx, float ty, float tz) {
    if (family == GROUP) {
      childHasMatrix();
      applyMatrix = true;
      super.translate(tx, ty, tz);
    } else {
      if (!shapeEnded) {
        PGraphics.showWarning("Transformations can be applied only after the shape has been ended.");
        return;
      }

      checkMatrix(3);
      matrix.reset();
      matrix.translate(tx, ty, tz);
      tess.applyMatrix((PMatrix3D) matrix);
      
      modified(); 
      if (0 < tess.fillVertexCount) {
        modifiedFillVertices = true;  
        modifiedFillNormals = true; 
      }        
      if (0 < tess.lineVertexCount) {
        modifiedLineVertices = true;
        modifiedLineNormals = true;
        modifiedLineAttributes = true;
      }
      if (0 < tess.pointVertexCount) {
        modifiedPointVertices = true;
        modifiedPointNormals = true;        
      }
      
      // So the transformation is not applied again when drawing
      applyMatrix = false;
    }    
  }
  
  
  public void rotate(float angle) {
    if (family == GROUP) {
      childHasMatrix();
      applyMatrix = true;
      super.rotate(angle);
    } else {
      if (!shapeEnded) {
        PGraphics.showWarning("Transformations can be applied only after the shape has been ended.");
        return;
      }
      
      checkMatrix(2);
      matrix.reset();
      matrix.rotate(angle);
      tess.applyMatrix((PMatrix2D) matrix);
            
      modified(); 
      if (0 < tess.fillVertexCount) {
        modifiedFillVertices = true;  
        modifiedFillNormals = true; 
      }        
      if (0 < tess.lineVertexCount) {
        modifiedLineVertices = true;
        modifiedLineNormals = true;
        modifiedLineAttributes = true;
      }
      if (0 < tess.pointVertexCount) {
        modifiedPointVertices = true;
        modifiedPointNormals = true;        
      }
      
      // So the transformation is not applied again when drawing
      applyMatrix = false;   
    }

  }
  
  public void rotate(float angle, float v0, float v1, float v2) {
    if (family == GROUP) {
      childHasMatrix();
      applyMatrix = true;
      super.rotate(angle, v0, v1, v2);
    } else {
      if (!shapeEnded) {
        PGraphics.showWarning("Transformations can be applied only after the shape has been ended.");
        return;
      }
      
      checkMatrix(3);
      matrix.reset();
      matrix.rotate(angle, v0, v1, v2);
      tess.applyMatrix((PMatrix3D) matrix);
            
      modified(); 
      if (0 < tess.fillVertexCount) {
        modifiedFillVertices = true;  
        modifiedFillNormals = true; 
      }        
      if (0 < tess.lineVertexCount) {
        modifiedLineVertices = true;
        modifiedLineNormals = true;
        modifiedLineAttributes = true;
      }
      if (0 < tess.pointVertexCount) {
        modifiedPointVertices = true;
        modifiedPointNormals = true;        
      }
      
      // So the transformation is not applied again when drawing
      applyMatrix = false;   
    }
  }
  
  public void scale(float s) {
    if (family == GROUP) {
      childHasMatrix();
      applyMatrix = true;
      super.scale(s);
    } else {
      if (!shapeEnded) {
        PGraphics.showWarning("Transformations can be applied only after the shape has been ended.");
        return;
      }
      
      checkMatrix(2);
      matrix.reset();
      matrix.scale(s);
      tess.applyMatrix((PMatrix2D) matrix);
      
      modified(); 
      if (0 < tess.fillVertexCount) {
        modifiedFillVertices = true;  
        modifiedFillNormals = true; 
      }        
      if (0 < tess.lineVertexCount) {
        modifiedLineVertices = true;
        modifiedLineNormals = true;
        modifiedLineAttributes = true;
      }
      if (0 < tess.pointVertexCount) {
        modifiedPointVertices = true;
        modifiedPointNormals = true;        
      }
      
      // So the transformation is not applied again when drawing
      applyMatrix = false;
    }
  }


  public void scale(float x, float y) {
    if (family == GROUP) {
      childHasMatrix();
      applyMatrix = true;
      super.scale(x, y);
    } else {
      if (!shapeEnded) {
        PGraphics.showWarning("Transformations can be applied only after the shape has been ended.");
        return;
      }
      
      checkMatrix(2);
      matrix.reset();
      matrix.scale(x, y);
      tess.applyMatrix((PMatrix2D) matrix);
      
      modified(); 
      if (0 < tess.fillVertexCount) {
        modifiedFillVertices = true;  
        modifiedFillNormals = true; 
      }        
      if (0 < tess.lineVertexCount) {
        modifiedLineVertices = true;
        modifiedLineNormals = true;
        modifiedLineAttributes = true;
      }
      if (0 < tess.pointVertexCount) {
        modifiedPointVertices = true;
        modifiedPointNormals = true;        
      }
      
      // So the transformation is not applied again when drawing
      applyMatrix = false;
    }
  }


  public void scale(float x, float y, float z) {
    if (family == GROUP) {
      childHasMatrix();
      applyMatrix = true;
      super.scale(x, y, z);
    } else {
      if (!shapeEnded) {
        PGraphics.showWarning("Transformations can be applied only after the shape has been ended.");
        return;
      }
      
      checkMatrix(3);
      matrix.reset();
      matrix.scale(x, y, z);
      tess.applyMatrix((PMatrix3D) matrix);
      
      modified(); 
      if (0 < tess.fillVertexCount) {
        modifiedFillVertices = true;  
        modifiedFillNormals = true; 
      }        
      if (0 < tess.lineVertexCount) {
        modifiedLineVertices = true;
        modifiedLineNormals = true;
        modifiedLineAttributes = true;
      }
      if (0 < tess.pointVertexCount) {
        modifiedPointVertices = true;
        modifiedPointNormals = true;        
      }
      
      // So the transformation is not applied again when drawing
      applyMatrix = false;
    }    
  }  
  

  public void applyMatrix(PMatrix source) {
    super.applyMatrix(source);
  }


  public void applyMatrix(PMatrix2D source) {
    super.applyMatrix(source);
  }


  public void applyMatrix(float n00, float n01, float n02,
                          float n10, float n11, float n12) {
    if (family == GROUP) {
      childHasMatrix();
      applyMatrix = true;
      super.applyMatrix(n00, n01, n02,
                        n10, n11, n12);
    } else {
      if (!shapeEnded) {
        PGraphics.showWarning("Transformations can be applied only after the shape has been ended.");
        return;
      }
      
      checkMatrix(2);
      matrix.reset();
      matrix.apply(n00, n01, n02,
                   n10, n11, n12);   
      tess.applyMatrix((PMatrix2D) matrix);
      
      modified(); 
      if (0 < tess.fillVertexCount) {
        modifiedFillVertices = true;  
        modifiedFillNormals = true; 
      }        
      if (0 < tess.lineVertexCount) {
        modifiedLineVertices = true;
        modifiedLineNormals = true;
        modifiedLineAttributes = true;
      }
      if (0 < tess.pointVertexCount) {
        modifiedPointVertices = true;
        modifiedPointNormals = true;        
      }
      
      // So the transformation is not applied again when drawing
      applyMatrix = false;     
    }
  }


  public void apply(PMatrix3D source) {
    applyMatrix(source.m00, source.m01, source.m02, source.m03,
                source.m10, source.m11, source.m12, source.m13,
                source.m20, source.m21, source.m22, source.m23,
                source.m30, source.m31, source.m32, source.m33);
  }


  public void applyMatrix(float n00, float n01, float n02, float n03,
                          float n10, float n11, float n12, float n13,
                          float n20, float n21, float n22, float n23,
                          float n30, float n31, float n32, float n33) {
    if (family == GROUP) {
      childHasMatrix();
      applyMatrix = true;
      super.applyMatrix(n00, n01, n02, n03,
                        n10, n11, n12, n13,
                        n20, n21, n22, n23,
                        n30, n31, n32, n33);
    } else {
      if (!shapeEnded) {
        PGraphics.showWarning("Transformations can be applied only after the shape has been ended.");
        return;
      }
      
      checkMatrix(3);
      matrix.reset();
      matrix.apply(n00, n01, n02, n03,
                   n10, n11, n12, n13,
                   n20, n21, n22, n23,
                   n30, n31, n32, n33);   
      tess.applyMatrix((PMatrix3D) matrix);
      
      modified(); 
      if (0 < tess.fillVertexCount) {
        modifiedFillVertices = true;  
        modifiedFillNormals = true; 
      }        
      if (0 < tess.lineVertexCount) {
        modifiedLineVertices = true;
        modifiedLineNormals = true;
        modifiedLineAttributes = true;
      }
      if (0 < tess.pointVertexCount) {
        modifiedPointVertices = true;
        modifiedPointNormals = true;        
      }
      
      // So the transformation is not applied again when drawing
      applyMatrix = false;     
    }
  }
  
  
  public void resetMatrix() {
    // TODO
    // What to do in the case of geometry shapes?
    // In order to properly reset the transformation,
    // we need to have the last matrix applied, calculate
    // the inverse and apply on the tess object... 
    //checkMatrix(2);
    //matrix.reset();
  }
  
  
  protected void childHasMatrix() {
    childHasMatrix = true;
    if (parent != null) {
      ((PShape3D)parent).childHasMatrix();
    }
  }
  
  ///////////////////////////////////////////////////////////  
  
  //
  
  // Bezier curves 
  
  public void bezierDetail(int detail) {
    bezierDetail = detail;

    if (bezierDrawMatrix == null) {
      bezierDrawMatrix = new PMatrix3D();
    }

    // setup matrix for forward differencing to speed up drawing
    renderer.splineForward(detail, bezierDrawMatrix);

    // multiply the basis and forward diff matrices together
    // saves much time since this needn't be done for each curve
    bezierDrawMatrix.apply(renderer.bezierBasisMatrix);
  }  
  
  public void bezierVertex(float x2, float y2,
                           float x3, float y3,
                           float x4, float y4) {
    bezierVertex(x2, y2, 0, x3, y3, 0, x4, y4, 0); 
  }
  
  public void bezierVertex(float x2, float y2, float z2,
                           float x3, float y3, float z3,
                           float x4, float y4, float z4) {
    bezierInitCheck();
    bezierVertexCheck();
    PMatrix3D draw = bezierDrawMatrix;

    float x1 = in.getLastVertexX();
    float y1 = in.getLastVertexY();
    float z1 = in.getLastVertexZ();

    float xplot1 = draw.m10*x1 + draw.m11*x2 + draw.m12*x3 + draw.m13*x4;
    float xplot2 = draw.m20*x1 + draw.m21*x2 + draw.m22*x3 + draw.m23*x4;
    float xplot3 = draw.m30*x1 + draw.m31*x2 + draw.m32*x3 + draw.m33*x4;

    float yplot1 = draw.m10*y1 + draw.m11*y2 + draw.m12*y3 + draw.m13*y4;
    float yplot2 = draw.m20*y1 + draw.m21*y2 + draw.m22*y3 + draw.m23*y4;
    float yplot3 = draw.m30*y1 + draw.m31*y2 + draw.m32*y3 + draw.m33*y4;

    float zplot1 = draw.m10*z1 + draw.m11*z2 + draw.m12*z3 + draw.m13*z4;
    float zplot2 = draw.m20*z1 + draw.m21*z2 + draw.m22*z3 + draw.m23*z4;
    float zplot3 = draw.m30*z1 + draw.m31*z2 + draw.m32*z3 + draw.m33*z4;

    for (int j = 0; j < bezierDetail; j++) {
      x1 += xplot1; xplot1 += xplot2; xplot2 += xplot3;
      y1 += yplot1; yplot1 += yplot2; yplot2 += yplot3;
      z1 += zplot1; zplot1 += zplot2; zplot2 += zplot3;
      vertexImpl(x1, y1, z1, 0, 0, BEZIER_VERTEX);
    }    
  }
  
  public void quadraticVertex(float cx, float cy,
                              float x3, float y3) {
    quadraticVertex(cx, cy, 0,
                    x3, y3, 0);
  }  
  
  public void quadraticVertex(float cx, float cy, float cz,
                              float x3, float y3, float z3) {
    float x1 = in.getLastVertexX();
    float y1 = in.getLastVertexY();
    float z1 = in.getLastVertexZ();

    bezierVertex(x1 + ((cx-x1)*2/3.0f), y1 + ((cy-y1)*2/3.0f), z1 + ((cz-z1)*2/3.0f),
                 x3 + ((cx-x3)*2/3.0f), y3 + ((cy-y3)*2/3.0f), z3 + ((cz-z3)*2/3.0f),
                 x3, y3, z3);
  }

  protected void bezierInitCheck() {
    if (!bezierInited) {
      bezierInit();
    }
  }

  protected void bezierInit() {
    // overkill to be broken out, but better parity with the curve stuff below
    bezierDetail(bezierDetail);
    bezierInited = true;
  }  
  
  protected void bezierVertexCheck() {
    if (kind != POLYGON) {
      throw new RuntimeException("createGeometry() or createGeometry(POLYGON) " +
                                 "must be used before bezierVertex() or quadraticVertex()");
    }
    if (in.vertexCount == 0) {
      throw new RuntimeException("vertex() must be used at least once" +
                                 "before bezierVertex() or quadraticVertex()");
    }
  }    
  
  ///////////////////////////////////////////////////////////  
  
  //
  
  // Catmull-Rom curves

  public void curveDetail(int detail) {
    curveDetail = detail;
    curveInit();
  }
  
  public void curveTightness(float tightness) {
    curveTightness = tightness;
    curveInit();
  }  
  
  public void curveVertex(float x, float y) {
    curveVertex(x, y, 0);
  }  

  public void curveVertex(float x, float y, float z) {
    curveVertexCheck();
    float[] vertex = curveVertices[curveVertexCount];
    vertex[X] = x;
    vertex[Y] = y;
    vertex[Z] = z;
    curveVertexCount++;

    // draw a segment if there are enough points
    if (curveVertexCount > 3) {
      curveVertexSegment(curveVertices[curveVertexCount-4][X],
                         curveVertices[curveVertexCount-4][Y],
                         curveVertices[curveVertexCount-4][Z],
                         curveVertices[curveVertexCount-3][X],
                         curveVertices[curveVertexCount-3][Y],
                         curveVertices[curveVertexCount-3][Z],
                         curveVertices[curveVertexCount-2][X],
                         curveVertices[curveVertexCount-2][Y],
                         curveVertices[curveVertexCount-2][Z],
                         curveVertices[curveVertexCount-1][X],
                         curveVertices[curveVertexCount-1][Y],
                         curveVertices[curveVertexCount-1][Z]);
    }
    
  }

  protected void curveVertexCheck() {
    
    if (kind != POLYGON) {
      throw new RuntimeException("You must use createGeometry() or " +
                                 "createGeometry(POLYGON) before curveVertex()");
    }
    
    // to improve code init time, allocate on first use.
    if (curveVertices == null) {
      curveVertices = new float[128][3];
    }

    if (curveVertexCount == curveVertices.length) {
      // Can't use PApplet.expand() cuz it doesn't do the copy properly
      float[][] temp = new float[curveVertexCount << 1][3];
      System.arraycopy(curveVertices, 0, temp, 0, curveVertexCount);
      curveVertices = temp;
    }
    curveInitCheck();
  }
  
  protected void curveInitCheck() {
    if (!curveInited) {
      curveInit();
    }
  }
  
  protected void curveInit() {
    // allocate only if/when used to save startup time
    if (curveDrawMatrix == null) {
      curveBasisMatrix = new PMatrix3D();
      curveDrawMatrix = new PMatrix3D();
      curveInited = true;
    }

    float s = curveTightness;
    curveBasisMatrix.set((s-1)/2f, (s+3)/2f,  (-3-s)/2f, (1-s)/2f,
                         (1-s),    (-5-s)/2f, (s+2),     (s-1)/2f,
                         (s-1)/2f, 0,         (1-s)/2f,  0,
                         0,        1,         0,         0);

    renderer.splineForward(curveDetail, curveDrawMatrix);

    if (bezierBasisInverse == null) {
      bezierBasisInverse = renderer.bezierBasisMatrix.get();
      bezierBasisInverse.invert();
      curveToBezierMatrix = new PMatrix3D();
    }

    // TODO only needed for PGraphicsJava2D? if so, move it there
    // actually, it's generally useful for other renderers, so keep it
    // or hide the implementation elsewhere.
    curveToBezierMatrix.set(curveBasisMatrix);
    curveToBezierMatrix.preApply(bezierBasisInverse);

    // multiply the basis and forward diff matrices together
    // saves much time since this needn't be done for each curve
    curveDrawMatrix.apply(curveBasisMatrix);
  }  
  
  /**
   * Handle emitting a specific segment of Catmull-Rom curve. This can be
   * overridden by subclasses that need more efficient rendering options.
   */
  protected void curveVertexSegment(float x1, float y1, float z1,
                                    float x2, float y2, float z2,
                                    float x3, float y3, float z3,
                                    float x4, float y4, float z4) {
    float x0 = x2;
    float y0 = y2;
    float z0 = z2;

    PMatrix3D draw = curveDrawMatrix;

    float xplot1 = draw.m10*x1 + draw.m11*x2 + draw.m12*x3 + draw.m13*x4;
    float xplot2 = draw.m20*x1 + draw.m21*x2 + draw.m22*x3 + draw.m23*x4;
    float xplot3 = draw.m30*x1 + draw.m31*x2 + draw.m32*x3 + draw.m33*x4;

    float yplot1 = draw.m10*y1 + draw.m11*y2 + draw.m12*y3 + draw.m13*y4;
    float yplot2 = draw.m20*y1 + draw.m21*y2 + draw.m22*y3 + draw.m23*y4;
    float yplot3 = draw.m30*y1 + draw.m31*y2 + draw.m32*y3 + draw.m33*y4;

    float zplot1 = draw.m10*z1 + draw.m11*z2 + draw.m12*z3 + draw.m13*z4;
    float zplot2 = draw.m20*z1 + draw.m21*z2 + draw.m22*z3 + draw.m23*z4;
    float zplot3 = draw.m30*z1 + draw.m31*z2 + draw.m32*z3 + draw.m33*z4;

    vertexImpl(x0, y0, z0, 0, 0, CURVE_VERTEX);
    for (int j = 0; j < curveDetail; j++) {
      x0 += xplot1; xplot1 += xplot2; xplot2 += xplot3;
      y0 += yplot1; yplot1 += yplot2; yplot2 += yplot3;
      z0 += zplot1; zplot1 += zplot2; zplot2 += zplot3;
      vertexImpl(x0, y0, z0, 0, 0, CURVE_VERTEX);
    }
  }  
  
  
  ///////////////////////////////////////////////////////////  
  
  //
  
  // Methods to access tessellated data.
  
  
  public int firstFillVertex() {
    updateTesselation();
    return tess.firstFillVertex;  
  }
  
  public int lastFillVertex() {
    updateTesselation();
    return tess.lastFillVertex;
  }

  public int fillVertexCount() {
    updateTesselation();
    return tess.fillVertexCount;
  }
  
  public int firstFillIndex() {
    updateTesselation();
    return tess.firstFillIndex;  
  }
  
  public int lastFillIndex() {
    updateTesselation();
    return tess.lastFillIndex;
  }  
    
  public int fillIndexCount() {
    updateTesselation();
    return tess.fillIndexCount;
  }
  
  public float[] fillVertices() {
    updateTesselation();
    return tess.fillVertices;
  }
  
  public float[] fillColors() {
    updateTesselation();
    return tess.fillColors;
  }  
  
  public float[] fillNormals() {
    updateTesselation();
    return tess.fillNormals;
  }  
  
  public float[] fillTexCoords() {
    updateTesselation();
    return tess.fillTexcoords;
  }  
  
  public short[] fillIndices() {
    updateTesselation();
    return tess.fillIndices;
  }
    
  public int firstLineVertex() {
    updateTesselation();
    return tess.firstLineVertex;  
  }
  
  public int lastLineVertex() {
    updateTesselation();
    return tess.lastLineVertex;
  }
  
  public int lineVertexCount() {
    updateTesselation();
    return tess.lineVertexCount;
  }  
  
  public int firstLineIndex() {
    updateTesselation();
    return tess.firstLineIndex;  
  }
  
  public int lastLineIndex() {
    updateTesselation();
    return tess.lastLineIndex;
  }

  public int lineIndexCount() {
    updateTesselation();
    return tess.lineIndexCount;
  }
    
  public float[] lineVertices() {
    updateTesselation();
    return tess.lineVertices;
  }
  
  public float[] lineColors() {
    updateTesselation();
    return tess.lineColors;
  }  
  
  public float[] lineNormals() {
    updateTesselation();
    return tess.lineNormals;
  }  
  
  public float[] lineAttributes() {
    updateTesselation();
    return tess.lineAttributes;
  }  
  
  public short[] lineIndices() {
    updateTesselation();
    return tess.lineIndices;
  }  
  
  public int firstPointVertex() {
    updateTesselation();
    return tess.firstPointVertex;  
  }
  
  public int lastPointVertex() {
    updateTesselation();
    return tess.lastPointVertex;
  }
  
  public int pointVertexCount() {
    updateTesselation();
    return tess.pointVertexCount;
  }    
  
  public int firstPointIndex() {
    updateTesselation();
    return tess.firstPointIndex;  
  }
  
  public int lastPointIndex() {
    updateTesselation();
    return tess.lastPointIndex;
  }  
  
  public int pointIndexCount() {
    updateTesselation();
    return tess.pointIndexCount;
  }  
  
  public float[] pointVertices() {
    updateTesselation();
    return tess.pointVertices;
  }
  
  public float[] pointColors() {
    updateTesselation();
    return tess.pointColors;
  }  
  
  public float[] pointNormals() {
    updateTesselation();
    return tess.pointNormals;
  }  
  
  public float[] pointAttributes() {
    updateTesselation();
    return tess.pointAttributes;
  }  
  
  public short[] pointIndices() {
    updateTesselation();
    return tess.pointIndices;
  }   
  
  public FloatBuffer mapFillVertices() {        
    return mapVertexImpl(root.glFillVertexBufferID, 3 * tess.firstFillVertex, 3 * tess.fillVertexCount).asFloatBuffer();
  }
  
  public void unmapFillVertices() {
    unmapVertexImpl();
  }
  
  public FloatBuffer mapFillColors() {        
    return mapVertexImpl(root.glFillColorBufferID, 4 * tess.firstFillVertex, 4 * tess.fillVertexCount).asFloatBuffer();
  }
  
  public void unmapFillColors() {
    unmapVertexImpl();
  }
  
  public FloatBuffer mapFillNormals() {        
    return mapVertexImpl(root.glFillNormalBufferID, 3 * tess.firstFillVertex, 3 * tess.fillVertexCount).asFloatBuffer();
  }
  
  public void unmapFillNormals() {
    unmapVertexImpl();
  }
  
  public FloatBuffer mapFillTexCoords() {        
    return mapVertexImpl(root.glFillTexCoordBufferID, 2 * tess.firstFillVertex, 2 * tess.fillVertexCount).asFloatBuffer();
  }
  
  public void unmapFillTexCoords() {
    unmapVertexImpl();
  }
  
  public IntBuffer mapFillIndices() {        
    return mapIndexImpl(root.glFillIndexBufferID, tess.firstFillIndex, tess.fillIndexCount).asIntBuffer();
  }
  
  public void unmapFillIndices() {
    unmapIndexImpl();
  }
  
  public FloatBuffer mapLineVertices() {        
    return mapVertexImpl(root.glLineVertexBufferID, 3 * tess.firstLineVertex, 3 * tess.lineVertexCount).asFloatBuffer();
  }
  
  public void unmapLineVertices() {
    unmapVertexImpl();
  }
  
  public FloatBuffer mapLineColors() {        
    return mapVertexImpl(root.glLineColorBufferID, 4 * tess.firstLineVertex, 4 * tess.lineVertexCount).asFloatBuffer();
  }
  
  public void unmapLineColors() {
    unmapVertexImpl();
  }
  
  public FloatBuffer mapLineNormals() {        
    return mapVertexImpl(root.glLineNormalBufferID, 3 * tess.firstLineVertex, 3 * tess.lineVertexCount).asFloatBuffer();
  }
  
  public void unmapLineNormals() {
    unmapVertexImpl();
  }
  
  public FloatBuffer mapLineAttributes() {        
    return mapVertexImpl(root.glLineAttribBufferID, 2 * tess.firstLineVertex, 2 * tess.lineVertexCount).asFloatBuffer();
  }
  
  public void unmapLineAttributes() {
    unmapVertexImpl();
  }
  
  public IntBuffer mapLineIndices() {        
    return mapIndexImpl(root.glLineIndexBufferID, tess.firstLineIndex, tess.lineIndexCount).asIntBuffer();
  }
  
  public void unmapLineIndices() {
    unmapIndexImpl();
  }
  
  public FloatBuffer mapPointVertices() {        
    return mapVertexImpl(root.glPointVertexBufferID, 3 * tess.firstPointVertex, 3 * tess.pointVertexCount).asFloatBuffer();
  }
  
  public void unmapPointVertices() {
    unmapVertexImpl();
  }
  
  public FloatBuffer mapPointColors() {        
    return mapVertexImpl(root.glPointColorBufferID, 4 * tess.firstPointVertex, 4 * tess.pointVertexCount).asFloatBuffer();
  }
  
  public void unmapPointColors() {
    unmapVertexImpl();
  }
  
  public FloatBuffer mapPointNormals() {        
    return mapVertexImpl(root.glPointNormalBufferID, 3 * tess.firstPointVertex, 3 * tess.pointVertexCount).asFloatBuffer();
  }
  
  public void unmapPointNormals() {
    unmapVertexImpl();
  }
  
  public FloatBuffer mapPointAttributes() {        
    return mapVertexImpl(root.glPointAttribBufferID, 2 * tess.firstPointVertex, 2 * tess.pointVertexCount).asFloatBuffer();
  }
  
  public void unmapPointAttributes() {
    unmapVertexImpl();
  }
  
  public IntBuffer mapPointIndices() {        
    return mapIndexImpl(root.glPointIndexBufferID, tess.firstPointIndex, tess.pointIndexCount).asIntBuffer();
  }
  
  public void unmapPointIndices() {
    unmapIndexImpl();
  }
  
  protected ByteBuffer mapVertexImpl(int id, int offset, int count) {
    updateTesselation();
    pgl.bindVertexBuffer(id);
    ByteBuffer bb;
    if (root == this) {            
      bb = pgl.mapVertexBuffer();  
    } else {
      bb = pgl.mapVertexBufferRange(offset, count); 
    }
    return bb;
  }
  
  protected void unmapVertexImpl() {
    pgl.unmapVertexBuffer();
    pgl.unbindVertexBuffer();    
  }
  
  protected ByteBuffer mapIndexImpl(int id, int offset, int count) {
    updateTesselation();
    pgl.bindIndexBuffer(id);
    ByteBuffer bb;
    if (root == this) {            
      bb = pgl.mapIndexBuffer();  
    } else {
      bb = pgl.mapIndexBufferRange(offset, count); 
    }
    return bb;
  }
  
  protected void unmapIndexImpl() {
    pgl.unmapIndexBuffer();
    pgl.unbindIndexBuffer();    
  }

  
  ///////////////////////////////////////////////////////////  
  
  //
  
  // Construction methods  
  
  
  protected void updateTesselation() {
    if (!root.tessellated) {
      root.tessellate();
      root.aggregate();        
    }
  }
 
  
  protected void tessellate() {
    if (family == GROUP) {
      for (int i = 0; i < childCount; i++) {
        PShape3D child = (PShape3D) children[i];
        child.tessellate();
      }      
    } else {   
      if (!tessellated && shapeEnded) {
        tessellator.setInGeometry(in);
        tessellator.setTessGeometry(tess);
        tessellator.setFill(fill || texture != null);
        tessellator.setStroke(stroke);
        tessellator.setStrokeWeight(strokeWeight);
        tessellator.setStrokeCap(strokeCap);
        tessellator.setStrokeJoin(strokeJoin);
        tessellator.setStrokeColor(strokeR, strokeG, strokeB, strokeA);        
        
        if (family == GEOMETRY) {
          if (kind == POINTS) {
            if (normalMode == NORMAL_MODE_AUTO) in.calcPointsNormals();
            tessellator.tessellatePoints();    
          } else if (kind == LINES) {
            if (normalMode == NORMAL_MODE_AUTO) in.calcLinesNormals();
            tessellator.tessellateLines();    
          } else if (kind == TRIANGLE || kind == TRIANGLES) {
            if (stroke) in.addTrianglesEdges();
            if (normalMode == NORMAL_MODE_AUTO) in.calcTrianglesNormals();
            tessellator.tessellateTriangles();
          } else if (kind == TRIANGLE_FAN) {
            if (stroke) in.addTriangleFanEdges();
            if (normalMode == NORMAL_MODE_AUTO) in.calcTriangleFanNormals();
            tessellator.tessellateTriangleFan();
          } else if (kind == TRIANGLE_STRIP) {            
            if (stroke) in.addTriangleStripEdges();
            if (normalMode == NORMAL_MODE_AUTO) in.calcTriangleStripNormals();
            tessellator.tessellateTriangleStrip();
          } else if (kind == QUAD || kind == QUADS) {            
            if (stroke) in.addQuadsEdges();
            if (normalMode == NORMAL_MODE_AUTO) in.calcQuadsNormals();
            tessellator.tessellateQuads();
          } else if (kind == QUAD_STRIP) {
            if (stroke) in.addQuadStripEdges();
            if (normalMode == NORMAL_MODE_AUTO) in.calcQuadStripNormals();
            tessellator.tessellateQuadStrip();
          } else if (kind == POLYGON) {
            if (stroke) in.addPolygonEdges(isClosed);
            tessellator.tessellatePolygon(isSolid, isClosed, normalMode == NORMAL_MODE_AUTO);
          }
        } else if (family == PRIMITIVE) {
          if (kind == POINT) {
            tessellatePoint();
          } else if (kind == LINE) {
            tessellateLine(); 
          } else if (kind == TRIANGLE) {
            tessellateTriangle();            
          } else if (kind == QUAD) {
            tessellateQuad();            
          } else if (kind == RECT) {
            tessellateRect();
          } else if (kind == ELLIPSE) {
            tessellateEllipse();
          } else if (kind == ARC) {
            tessellateArc();
          } else if (kind == BOX) {
            tessellateBox();            
          } else if (kind == SPHERE) {
            tessellateSphere();
          }
        } else if (family == PATH) {
          // TODO: Determine if this is necessary, since it is 
          // equivalent to use POLYGON with fill disabled.
        }
        
        if (texture != null && parent != null) {
          ((PShape3D)parent).addTexture(texture);
        }        
      }
    }
    
    tessellated = true;
    modified = false;
  }

  
  protected void tessellatePoint() {
    
  }
  
  
  protected void tessellateLine() {
    
  }
  
  
  protected void tessellateTriangle() {
    
  }
  
  
  protected void tessellateQuad() {
    
  }  
  
  
  protected void tessellateRect() {
    
  }
  
  
  protected void tessellateEllipse() {
    float a = params[0];
    float b = params[1];
    float c = params[2];
    float d = params[3];    

    in.generateEllipse(ellipseMode, a, b, c, d,
                       fill, fillR, fillG, fillB, fillA, 
                       stroke, strokeR, strokeG, strokeB, strokeA,
                       strokeWeight);
    
    tessellator.tessellateTriangleFan(); 
  }
  
  
  protected void tessellateArc() {
    
  }
  
  
  protected void tessellateBox() {
    // TODO: move to InGeometry
    float w = params[0];
    float h = params[1];
    float d = params[2];
        
    float x1 = -w/2f; float x2 = w/2f;
    float y1 = -h/2f; float y2 = h/2f;
    float z1 = -d/2f; float z2 = d/2f;

    // front
    normal(0, 0, 1);
    vertex(x1, y1, z1, 0, 0);
    vertex(x2, y1, z1, 1, 0);
    vertex(x2, y2, z1, 1, 1);
    vertex(x1, y2, z1, 0, 1);

    // right
    normal(1, 0, 0);
    vertex(x2, y1, z1, 0, 0);
    vertex(x2, y1, z2, 1, 0);
    vertex(x2, y2, z2, 1, 1);
    vertex(x2, y2, z1, 0, 1);

    // back
    normal(0, 0, -1);
    vertex(x2, y1, z2, 0, 0);
    vertex(x1, y1, z2, 1, 0);
    vertex(x1, y2, z2, 1, 1);
    vertex(x2, y2, z2, 0, 1);

    // left
    normal(-1, 0, 0);
    vertex(x1, y1, z2, 0, 0);
    vertex(x1, y1, z1, 1, 0);
    vertex(x1, y2, z1, 1, 1);
    vertex(x1, y2, z2, 0, 1);

    // top
    normal(0, 1, 0);
    vertex(x1, y1, z2, 0, 0);
    vertex(x2, y1, z2, 1, 0);
    vertex(x2, y1, z1, 1, 1);
    vertex(x1, y1, z1, 0, 1);

    // bottom
    normal(0, -1, 0);
    vertex(x1, y2, z1, 0, 0);
    vertex(x2, y2, z1, 1, 0);
    vertex(x2, y2, z2, 1, 1);
    vertex(x1, y2, z2, 0, 1);
    
    if (stroke) in.addQuadsEdges(); 
    tessellator.tessellateQuads();      
  }
  
  
  protected void tessellateSphere() {
    // TODO: move to InGeometry
    float r = params[0];
    int nu = renderer.sphereDetailU;
    int nv = renderer.sphereDetailV;
    
    if ((nu < 3) || (nv < 2)) {
      nu = nv = 30;
    }
 
    
    float startLat = -90;
    float startLon = 0.0f;

    float latInc = 180.0f / nu;
    float lonInc = 360.0f / nv;

    float phi1,  phi2;
    float theta1,  theta2;
    float x0, y0, z0;
    float x1, y1, z1;
    float x2, y2, z2;
    float x3, y3, z3;
    float u1, v1, u2, v2, v3;

    for (int col = 0; col < nu; col++) {
      phi1 = (startLon + col * lonInc) * DEG_TO_RAD;
      phi2 = (startLon + (col + 1) * lonInc) * DEG_TO_RAD;
      for (int row = 0; row < nv; row++) {
        theta1 = (startLat + row * latInc) * DEG_TO_RAD;
        theta2 = (startLat + (row + 1) * latInc) * DEG_TO_RAD;

        x0 = PApplet.cos(phi1) * PApplet.cos(theta1);
        x1 = PApplet.cos(phi1) * PApplet.cos(theta2);
        x2 = PApplet.cos(phi2) * PApplet.cos(theta2);
        
        y0 = PApplet.sin(theta1);
        y1 = PApplet.sin(theta2);
        y2 = PApplet.sin(theta2);
        
        z0 = PApplet.sin(phi1) * PApplet.cos(theta1);
        z1 = PApplet.sin(phi1) * PApplet.cos(theta2);
        z2 = PApplet.sin(phi2) * PApplet.cos(theta2);

        x3 = PApplet.cos(phi2) * PApplet.cos(theta1);
        y3 = PApplet.sin(theta1);            
        z3 = PApplet.sin(phi2) * PApplet.cos(theta1);
        
        u1 = PApplet.map(phi1, TWO_PI, 0, 0, 1); 
        u2 = PApplet.map(phi2, TWO_PI, 0, 0, 1);
        v1 = PApplet.map(theta1, -HALF_PI, HALF_PI, 0, 1);
        v2 = PApplet.map(theta2, -HALF_PI, HALF_PI, 0, 1);
        v3 = PApplet.map(theta1, -HALF_PI, HALF_PI, 0, 1);
        
        normal(x0, y0, z0);     
        vertex(r * x0, r * y0, r * z0, u1, v1);
   
        normal(x1, y1, z1);
        vertex(r * x1,  r * y1,  r * z1, u1, v2);

        normal(x2, y2, z2);
        vertex(r * x2, r * y2, r * z2, u2, v2);

        normal(x0, y0, z0);    
        vertex(r * x0, r * y0, r * z0, u1, v1);

        normal(x2, y2, z2);
        vertex(r * x2, r * y2, r * z2, u2, v2);
        
        normal(x3,  y3,  z3);
        vertex(r * x3,  r * y3,  r * z3,  u2,  v3);
      }
    }
    
    if (stroke) in.addTrianglesEdges();
    tessellator.tessellateTriangles();
  }
  
  
  protected void updateGeometry() {
    if (root == this && parent == null && modified) {
      // Initializing offsets
      fillVertCopyOffset = 0;
      lineVertCopyOffset = 0;
      pointVertCopyOffset = 0;
      
      updateRootGeometry();
      
      // Copying any data remaining in the caches
      if (root.fillVerticesCache != null && root.fillVerticesCache.hasData()) {
        root.copyFillVertices(root.fillVerticesCache.offset, root.fillVerticesCache.size, root.fillVerticesCache.data);
        root.fillVerticesCache.reset();
      }
      
      if (root.fillColorsCache != null && root.fillColorsCache.hasData()) {
        root.copyFillColors(root.fillColorsCache.offset, root.fillColorsCache.size, root.fillColorsCache.data);
        root.fillColorsCache.reset();
      }
      
      if (root.fillNormalsCache != null && root.fillNormalsCache.hasData()) {
        root.copyFillNormals(root.fillNormalsCache.offset, root.fillNormalsCache.size, root.fillNormalsCache.data);
        root.fillNormalsCache.reset();
      }
      
      if (root.fillTexCoordsCache != null && root.fillTexCoordsCache.hasData()) {
        root.copyFillTexCoords(root.fillTexCoordsCache.offset, root.fillTexCoordsCache.size, root.fillTexCoordsCache.data);
        root.fillTexCoordsCache.reset();
      }
      
      if (root.lineVerticesCache != null && root.lineVerticesCache.hasData()) {
        root.copyLineVertices(root.lineVerticesCache.offset, root.lineVerticesCache.size, root.lineVerticesCache.data);
        root.lineVerticesCache.reset();
      }
      
      if (root.lineColorsCache != null && root.lineColorsCache.hasData()) {
        root.copyLineColors(root.lineColorsCache.offset, root.lineColorsCache.size, root.lineColorsCache.data);
        root.lineColorsCache.reset();
      }
      
      if (root.lineNormalsCache != null && root.lineNormalsCache.hasData()) {
        root.copyLineNormals(root.lineNormalsCache.offset, root.lineNormalsCache.size, root.lineNormalsCache.data);
        root.lineNormalsCache.reset();
      }
      
      if (root.lineAttributesCache != null && root.lineAttributesCache.hasData()) {
        root.copyLineAttributes(root.lineAttributesCache.offset, root.lineAttributesCache.size, root.lineAttributesCache.data);
        root.lineAttributesCache.reset();
      }      
    
     if (root.pointVerticesCache != null && root.pointVerticesCache.hasData()) {
        root.copyPointVertices(root.pointVerticesCache.offset, root.pointVerticesCache.size, root.pointVerticesCache.data);
        root.pointVerticesCache.reset();
      }
      
      if (root.pointColorsCache != null && root.pointColorsCache.hasData()) {
        root.copyPointColors(root.pointColorsCache.offset, root.pointColorsCache.size, root.pointColorsCache.data);
        root.pointColorsCache.reset();
      }
      
      if (root.pointNormalsCache != null && root.pointNormalsCache.hasData()) {
        root.copyPointNormals(root.pointNormalsCache.offset, root.pointNormalsCache.size, root.pointNormalsCache.data);
        root.pointNormalsCache.reset();
      }
      
      if (root.pointAttributesCache != null && root.pointAttributesCache.hasData()) {
        root.copyPointAttributes(root.pointAttributesCache.offset, root.pointAttributesCache.size, root.pointAttributesCache.data);
        root.pointAttributesCache.reset();
      }        
    }
  }
  
  
  protected void aggregate() {
    if (root == this && parent == null) {
      // We recursively calculate the total number of vertices and indices.
      lastFillVertexOffset = 0;
      lastFillIndexOffset = 0;
      
      lastLineVertexOffset = 0;
      lastLineIndexOffset = 0;

      lastPointVertexOffset = 0;
      lastPointIndexOffset = 0;      
      
      aggregateImpl();
      
      // Now that we know, we can initialize the buffers with the correct size.
      if (0 < tess.fillVertexCount && 0 < tess.fillIndexCount) {   
        initFillBuffers(tess.fillVertexCount, tess.fillIndexCount);          
        fillVertCopyOffset = 0;
        fillIndCopyOffset = 0;
        copyFillGeometryToRoot();
      }
      
      if (0 < tess.lineVertexCount && 0 < tess.lineIndexCount) {   
        initLineBuffers(tess.lineVertexCount, tess.lineIndexCount);
        lineVertCopyOffset = 0;
        lineIndCopyOffset = 0;
        copyLineGeometryToRoot();
      }
      
      if (0 < tess.pointVertexCount && 0 < tess.pointIndexCount) {   
        initPointBuffers(tess.pointVertexCount, tess.pointIndexCount);
        pointVertCopyOffset = 0;
        pointIndCopyOffset = 0;
        copyPointGeometryToRoot();
      }      
    }
  }
  
  
  // This method is very important, as it is responsible of
  // generating the correct vertex and index values for each
  // level of the shape hierarchy.
  protected void aggregateImpl() {
    if (family == GROUP) {
      tess.reset();
      
      boolean firstGeom = true;
      boolean firstStroke = true;
      boolean firstPoint = true;
      for (int i = 0; i < childCount; i++) {
        PShape3D child = (PShape3D) children[i];
        child.aggregateImpl();

        tess.addCounts(child.tess);
        
        if (0 < child.tess.fillVertexCount) {
          if (firstGeom) {
            tess.setFirstFill(child.tess);
            firstGeom = false;
          }
          tess.setLastFill(child.tess);
        }  

        if (0 < child.tess.lineVertexCount) {
          if (firstStroke) {
            tess.setFirstLine(child.tess);
            firstStroke = false;
          }          
          tess.setLastLine(child.tess);
        }
        
        if (0 < child.tess.pointVertexCount) {
          if (firstPoint) {
            tess.setFirstPoint(child.tess);
            firstPoint = false;
          }
          tess.setLastPoint(child.tess);
        }           
      }
    } else {
      if (0 < tess.fillVertexCount) {
        root.lastFillVertexOffset = tess.setFillVertex(root.lastFillVertexOffset);
      }
      if (0 < tess.fillIndexCount) {
        root.lastFillIndexOffset = tess.setFillIndex(root.lastFillIndexOffset);
      }
            
      if (0 < tess.lineVertexCount) {
        root.lastLineVertexOffset = tess.setLineVertex(root.lastLineVertexOffset);
      }      
      if (0 < tess.lineIndexCount) {
        root.lastLineIndexOffset = tess.setLineIndex(root.lastLineIndexOffset);
      }
            
      if (0 < tess.pointVertexCount) {
        root.lastPointVertexOffset = tess.setPointVertex(root.lastPointVertexOffset);
      }
      if (0 < tess.pointIndexCount) {
        root.lastPointIndexOffset = tess.setPointIndex(root.lastPointIndexOffset);
      }      
      
      // recursive add up to the root level
      addFillIndexData(tess.firstFillVertex, tess.firstFillIndex, tess.lastFillIndex - tess.firstFillIndex + 1);
    }
    
    hasFill = 0 < tess.fillVertexCount && 0 < tess.fillIndexCount;
    hasLines = 0 < tess.lineVertexCount && 0 < tess.lineIndexCount; 
    hasPoints = 0 < tess.pointVertexCount && 0 < tess.pointIndexCount;    
  }

  protected void addFillIndexData(int first, int offset, int size) {
    if (fillIndexData == null) {
      fillIndexData = new ArrayList<IndexData>();
    }
    IndexData data = new IndexData(first, offset, size);
    fillIndexData.add(data);
    if (parent != null) {
      ((PShape3D)parent).addFillIndexData(first, offset, size);
    }
  }    
  
  protected void initFillBuffers(int nvert, int nind) {
    glFillVertexBufferID = renderer.createVertexBufferObject();  
    pgl.bindVertexBuffer(glFillVertexBufferID);
    pgl.initVertexBuffer(3 * nvert, glMode);    
    
    glFillColorBufferID = renderer.createVertexBufferObject();
    pgl.bindVertexBuffer(glFillColorBufferID);
    pgl.initVertexBuffer(4 * nvert, glMode);    
    
    glFillNormalBufferID = renderer.createVertexBufferObject();
    pgl.bindVertexBuffer(glFillNormalBufferID);
    pgl.initVertexBuffer(3 * nvert, glMode);     
    
    glFillTexCoordBufferID = renderer.createVertexBufferObject();
    pgl.bindVertexBuffer(glFillTexCoordBufferID);
    pgl.initVertexBuffer(2 * nvert, glMode);  
    
    pgl.unbindVertexBuffer();
        
    glFillIndexBufferID = renderer.createVertexBufferObject();  
    pgl.bindIndexBuffer(glFillIndexBufferID);
    pgl.initIndexBuffer(nind, glMode);    
    
    pgl.unbindIndexBuffer();  
  }  
  
  
  protected void copyFillGeometryToRoot() {
    if (family == GROUP) {
      for (int i = 0; i < childCount; i++) {
        PShape3D child = (PShape3D) children[i];        
        child.copyFillGeometryToRoot();
      }    
    } else {
      if (0 < tess.fillVertexCount && 0 < tess.fillIndexCount) {        
        root.copyFillGeometry(root.fillVertCopyOffset, tess.fillVertexCount, 
                              tess.fillVertices, tess.fillColors, tess.fillNormals, tess.fillTexcoords);
        root.fillVertCopyOffset += tess.fillVertexCount;
      
        root.copyFillIndices(root.fillIndCopyOffset, tess.fillIndexCount, tess.fillIndices);
        root.fillIndCopyOffset += tess.fillIndexCount;
      }
    }
  }
  
  
  protected void updateRootGeometry() {
    if (family == GROUP) {
      for (int i = 0; i < childCount; i++) {
        PShape3D child = (PShape3D) children[i];        
        child.updateRootGeometry();        
      } 
    } else {
 
      if (0 < tess.fillVertexCount) {    
        if (modifiedFillVertices) {
          if (root.fillVerticesCache == null) { 
            root.fillVerticesCache = new VertexCache(3);
          }            
          
          root.fillVerticesCache.add(root.fillVertCopyOffset, tess.fillVertexCount, tess.fillVertices);
          modifiedFillVertices = false;
        } else if (root.fillVerticesCache != null && root.fillVerticesCache.hasData()) {
          root.copyFillVertices(root.fillVerticesCache.offset, root.fillVerticesCache.size, root.fillVerticesCache.data);
          root.fillVerticesCache.reset();
        }
        
        if (modifiedFillColors) {
          if (root.fillColorsCache == null) { 
            root.fillColorsCache = new VertexCache(4);
          }            
          root.fillColorsCache.add(root.fillVertCopyOffset, tess.fillVertexCount, tess.fillColors);
          modifiedFillColors = false;            
        } else if (root.fillColorsCache != null && root.fillColorsCache.hasData()) {
          root.copyFillColors(root.fillColorsCache.offset, root.fillColorsCache.size, root.fillColorsCache.data);
          root.fillColorsCache.reset();
        }
        
        if (modifiedFillNormals) {
          if (root.fillNormalsCache == null) { 
            root.fillNormalsCache = new VertexCache(3);
          }            
          root.fillNormalsCache.add(root.fillVertCopyOffset, tess.fillVertexCount, tess.fillNormals);            
          modifiedFillNormals = false;            
        } else if (root.fillNormalsCache != null && root.fillNormalsCache.hasData()) {
          root.copyFillNormals(root.fillNormalsCache.offset, root.fillNormalsCache.size, root.fillNormalsCache.data);
          root.fillNormalsCache.reset();
        }
        
        if (modifiedFillTexCoords) {
          if (root.fillTexCoordsCache == null) { 
            root.fillTexCoordsCache = new VertexCache(2);
          }            
          root.fillTexCoordsCache.add(root.fillVertCopyOffset, tess.fillVertexCount, tess.fillTexcoords);            
          modifiedFillTexCoords = false;
        } else if (root.fillTexCoordsCache != null && root.fillTexCoordsCache.hasData()) {
          root.copyFillTexCoords(root.fillTexCoordsCache.offset, root.fillTexCoordsCache.size, root.fillTexCoordsCache.data);
          root.fillTexCoordsCache.reset();
        } 
      } 
      
      if (0 < tess.lineVertexCount) {
        if (modifiedLineVertices) {
          if (root.lineVerticesCache == null) { 
            root.lineVerticesCache = new VertexCache(3);
          }            
          root.lineVerticesCache.add(root.lineVertCopyOffset, tess.lineVertexCount, tess.lineVertices);
          modifiedLineVertices = false;
        } else if (root.lineVerticesCache != null && root.lineVerticesCache.hasData()) {
          root.copyLineVertices(root.lineVerticesCache.offset, root.lineVerticesCache.size, root.lineVerticesCache.data);
          root.lineVerticesCache.reset();
        }
        
        if (modifiedLineColors) {
          if (root.lineColorsCache == null) { 
            root.lineColorsCache = new VertexCache(4);
          }            
          root.lineColorsCache.add(root.lineVertCopyOffset, tess.lineVertexCount, tess.lineColors);
          modifiedLineColors = false;            
        } else if (root.lineColorsCache != null && root.lineColorsCache.hasData()) {
          root.copyLineColors(root.lineColorsCache.offset, root.lineColorsCache.size, root.lineColorsCache.data);
          root.lineColorsCache.reset();
        }
        
        if (modifiedLineNormals) {
          if (root.lineNormalsCache == null) { 
            root.lineNormalsCache = new VertexCache(3);
          }            
          root.lineNormalsCache.add(root.lineVertCopyOffset, tess.lineVertexCount, tess.lineNormals);            
          modifiedLineNormals = false;
        } else if (root.lineNormalsCache != null && root.lineNormalsCache.hasData()) {
          root.copyLineNormals(root.lineNormalsCache.offset, root.lineNormalsCache.size, root.lineNormalsCache.data);
          root.lineNormalsCache.reset();
        }
        
        if (modifiedLineAttributes) {
          if (root.lineAttributesCache == null) { 
            root.lineAttributesCache = new VertexCache(4);
          }            
          root.lineAttributesCache.add(root.lineVertCopyOffset, tess.lineVertexCount, tess.lineAttributes);            
          modifiedLineAttributes = false;
        } else if (root.lineAttributesCache != null && root.lineAttributesCache.hasData()) {
          root.copyLineAttributes(root.lineAttributesCache.offset, root.lineAttributesCache.size, root.lineAttributesCache.data);
          root.lineAttributesCache.reset();
        }      
      }

      if (0 < tess.pointVertexCount) {
        if (modifiedPointVertices) {
          if (root.pointVerticesCache == null) { 
            root.pointVerticesCache = new VertexCache(3);
          }            
          root.pointVerticesCache.add(root.pointVertCopyOffset, tess.pointVertexCount, tess.pointVertices);
          modifiedPointVertices = false;
        } else if (root.pointVerticesCache != null && root.pointVerticesCache.hasData()) {
          root.copyPointVertices(root.pointVerticesCache.offset, root.pointVerticesCache.size, root.pointVerticesCache.data);
          root.pointVerticesCache.reset();
        }
        
        if (modifiedPointColors) {
          if (root.pointColorsCache == null) { 
            root.pointColorsCache = new VertexCache(4);
          }            
          root.pointColorsCache.add(root.pointVertCopyOffset, tess.pointVertexCount, tess.pointColors);
          modifiedPointColors = false;            
        } else if (root.pointColorsCache != null && root.pointColorsCache.hasData()) {
          root.copyPointColors(root.pointColorsCache.offset, root.pointColorsCache.size, root.pointColorsCache.data);
          root.pointColorsCache.reset();
        }
        
        if (modifiedPointNormals) {
          if (root.pointNormalsCache == null) { 
            root.pointNormalsCache = new VertexCache(3);
          }            
          root.pointNormalsCache.add(root.pointVertCopyOffset, tess.pointVertexCount, tess.pointNormals);            
          modifiedPointNormals = false;
        } else if (root.pointNormalsCache != null && root.pointNormalsCache.hasData()) {
          root.copyPointNormals(root.pointNormalsCache.offset, root.pointNormalsCache.size, root.pointNormalsCache.data);
          root.pointNormalsCache.reset();
        }
        
        if (modifiedPointAttributes) {
          if (root.pointAttributesCache == null) { 
            root.pointAttributesCache = new VertexCache(3);
          }            
          root.pointAttributesCache.add(root.pointVertCopyOffset, tess.pointVertexCount, tess.pointAttributes);            
          modifiedPointAttributes = false;
        } else if (root.pointAttributesCache != null && root.pointAttributesCache.hasData()) {
          root.copyPointAttributes(root.pointAttributesCache.offset, root.pointAttributesCache.size, root.pointAttributesCache.data);
          root.pointAttributesCache.reset();
        }        
      }
      
      root.fillVertCopyOffset += tess.fillVertexCount;
      root.lineVertCopyOffset += tess.lineVertexCount;
      root.pointVertCopyOffset += tess.pointVertexCount;
    }
    
    modified = false;
  }
    
  
  protected void copyFillGeometry(int offset, int size, 
                                  float[] vertices, float[] colors, 
                                  float[] normals, float[] texcoords) {
    pgl.bindVertexBuffer(glFillVertexBufferID);
    pgl.copyVertexBufferSubData(vertices, 3 * offset, 3 * size, glMode);    
    
    pgl.bindVertexBuffer(glFillColorBufferID);
    pgl.copyVertexBufferSubData(colors, 4 * offset, 4 * size, glMode);    
    
    pgl.bindVertexBuffer(glFillNormalBufferID);
    pgl.copyVertexBufferSubData(normals, 3 * offset, 3 * size, glMode);    
    
    pgl.bindVertexBuffer(glFillTexCoordBufferID);
    pgl.copyVertexBufferSubData(texcoords, 2 * offset, 2 * size, glMode);     
    
    pgl.unbindVertexBuffer();    
  }

  
  protected void copyFillVertices(int offset, int size, float[] vertices) {
    pgl.bindVertexBuffer(glFillVertexBufferID);
    pgl.copyVertexBufferSubData(vertices, 3 * offset, 3 * size, glMode);      
    pgl.unbindVertexBuffer();
  }
  
  
  protected void copyFillColors(int offset, int size, float[] colors) {    
    pgl.bindVertexBuffer(glFillColorBufferID);
    pgl.copyVertexBufferSubData(colors, 4 * offset, 4 * size, glMode);     
    pgl.unbindVertexBuffer();
  }  
  
  
  protected void copyFillNormals(int offset, int size, float[] normals) {
    pgl.bindVertexBuffer(glFillNormalBufferID);
    pgl.copyVertexBufferSubData(normals, 3 * offset, 3 * size, glMode);    
    pgl.unbindVertexBuffer();
  }  

  
  protected void copyFillTexCoords(int offset, int size, float[] texcoords) {
    pgl.bindVertexBuffer(glFillTexCoordBufferID);
    pgl.copyVertexBufferSubData(texcoords, 2 * offset, 2 * size, glMode);      
    pgl.unbindVertexBuffer();
  }   
  
  
  protected void copyFillIndices(int offset, int size, short[] indices) {
    pgl.bindIndexBuffer(glFillIndexBufferID);
    pgl.copyIndexBufferSubData(indices, offset, size, glMode); 
    pgl.unbindIndexBuffer();
  }
  
  
  protected void initLineBuffers(int nvert, int nind) {
    glLineVertexBufferID = renderer.createVertexBufferObject();    
    pgl.bindVertexBuffer(glLineVertexBufferID);
    pgl.initVertexBuffer(3 * nvert, glMode);   
    
    glLineColorBufferID = renderer.createVertexBufferObject();
    pgl.bindVertexBuffer(glLineColorBufferID);
    pgl.initVertexBuffer(4 * nvert, glMode);       

    glLineNormalBufferID = renderer.createVertexBufferObject();    
    pgl.bindVertexBuffer(glLineNormalBufferID);
    pgl.initVertexBuffer(3 * nvert, glMode);    
    
    glLineAttribBufferID = renderer.createVertexBufferObject();
    pgl.bindVertexBuffer(glLineAttribBufferID);
    pgl.initVertexBuffer(4 * nvert, glMode);    
    
    pgl.unbindVertexBuffer();    
    
    glLineIndexBufferID = renderer.createVertexBufferObject();    
    pgl.bindIndexBuffer(glLineIndexBufferID);
    pgl.initIndexBuffer(nind, glMode);    

    pgl.unbindIndexBuffer();
  }
  
  
  protected void copyLineGeometryToRoot() {
    if (family == GROUP) {
      for (int i = 0; i < childCount; i++) {
        PShape3D child = (PShape3D) children[i];
        child.copyLineGeometryToRoot();
      }    
    } else {
      if (hasLines) {
        root.copyLineGeometry(root.lineVertCopyOffset, tess.lineVertexCount, 
                              tess.lineVertices, tess.lineColors, tess.lineNormals, tess.lineAttributes);        
        root.lineVertCopyOffset += tess.lineVertexCount;
        
        root.copyLineIndices(root.lineIndCopyOffset, tess.lineIndexCount, tess.lineIndices);
        root.lineIndCopyOffset += tess.lineIndexCount;        
      }
    }    
  }

  
  protected void copyLineGeometry(int offset, int size, 
                                  float[] vertices, float[] colors, float[] normals, float[] attribs) {
    pgl.bindVertexBuffer(glLineVertexBufferID);
    pgl.copyVertexBufferSubData(vertices, 3 * offset, 3 * size, glMode);     

    pgl.bindVertexBuffer(glLineColorBufferID);
    pgl.copyVertexBufferSubData(colors, 4 * offset, 4 * size, glMode);    
    
    pgl.bindVertexBuffer(glLineNormalBufferID);
    pgl.copyVertexBufferSubData(normals, 3 * offset, 3 * size, glMode);
    
    pgl.bindVertexBuffer(glLineAttribBufferID);
    pgl.copyVertexBufferSubData(attribs, 4 * offset, 4 * size, glMode);    
    
    pgl.unbindVertexBuffer();
  }    
  
  
  protected void copyLineVertices(int offset, int size, float[] vertices) {    
    pgl.bindVertexBuffer(glLineVertexBufferID);
    pgl.copyVertexBufferSubData(vertices, 3 * offset, 3 * size, glMode);    
    pgl.unbindVertexBuffer();
  }     
  
  
  protected void copyLineColors(int offset, int size, float[] colors) {
    pgl.bindVertexBuffer(glLineColorBufferID);
    pgl.copyVertexBufferSubData(colors, 4 * offset, 4 * size, glMode);     
    pgl.unbindVertexBuffer();
  }
  
  
  protected void copyLineNormals(int offset, int size, float[] normals) {
    pgl.bindVertexBuffer(glLineNormalBufferID);
    pgl.copyVertexBufferSubData(normals, 3 * offset, 3 * size, glMode);    
    pgl.unbindVertexBuffer();
  }

  
  protected void copyLineAttributes(int offset, int size, float[] attribs) {
    pgl.bindVertexBuffer(glLineAttribBufferID);
    pgl.copyVertexBufferSubData(attribs, 4 * offset, 4 * size, glMode);    
    pgl.unbindVertexBuffer();
  }
  
  
  protected void copyLineIndices(int offset, int size, short[] indices) {
    pgl.bindIndexBuffer(glLineIndexBufferID);
    pgl.copyIndexBufferSubData(indices, offset, size, glMode);    
    pgl.unbindIndexBuffer();
  }  
  

  protected void initPointBuffers(int nvert, int nind) {
    glPointVertexBufferID = renderer.createVertexBufferObject();
    pgl.bindVertexBuffer(glPointVertexBufferID);
    pgl.initVertexBuffer(3 * nvert, glMode);   

    glPointColorBufferID = renderer.createVertexBufferObject();
    pgl.bindVertexBuffer(glPointColorBufferID);
    pgl.initVertexBuffer(4 * nvert, glMode);     
    
    glPointNormalBufferID = renderer.createVertexBufferObject();    
    pgl.bindVertexBuffer(glPointNormalBufferID);
    pgl.initVertexBuffer(3 * nvert, glMode);    

    glPointAttribBufferID = renderer.createVertexBufferObject();
    pgl.bindVertexBuffer(glPointAttribBufferID);
    pgl.initVertexBuffer(2 * nvert, glMode);    
      
    pgl.unbindVertexBuffer();     
        
    glPointIndexBufferID = renderer.createVertexBufferObject();
    pgl.bindIndexBuffer(glPointIndexBufferID);
    pgl.initIndexBuffer(nind, glMode);    
    
    pgl.unbindIndexBuffer();
  }  
  
  
  protected void copyPointGeometryToRoot() {
    if (family == GROUP) {
      for (int i = 0; i < childCount; i++) {
        PShape3D child = (PShape3D) children[i];
        child.copyPointGeometryToRoot();
      }    
    } else {
      if (hasPoints) {
        root.copyPointGeometry(root.pointVertCopyOffset, tess.pointVertexCount, 
                               tess.pointVertices, tess.pointColors, tess.pointNormals, tess.pointAttributes);        
        root.pointVertCopyOffset += tess.pointVertexCount;
        
        root.copyPointIndices(root.pointIndCopyOffset, tess.pointIndexCount, tess.pointIndices);
        root.pointIndCopyOffset += tess.pointIndexCount;        
      }
    }
  }
  
  
  protected void copyPointGeometry(int offset, int size, 
                                   float[] vertices, float[] colors, float[] normals, float[] attribs) {
    pgl.bindVertexBuffer(glPointVertexBufferID);
    pgl.copyVertexBufferSubData(vertices, 3 * offset, 3 * size, glMode);

    pgl.bindVertexBuffer(glPointColorBufferID);
    pgl.copyVertexBufferSubData(colors, 4 * offset, 4 * size, glMode);    
    
    pgl.bindVertexBuffer(glPointNormalBufferID);
    pgl.copyVertexBufferSubData(normals, 3 * offset, 3 * size, glMode);    
    
    pgl.bindVertexBuffer(glPointAttribBufferID);
    pgl.copyVertexBufferSubData(attribs, 2 * offset, 2 * size, glMode);     
    
    pgl.unbindVertexBuffer();    
  }  


  protected void copyPointVertices(int offset, int size, float[] vertices) {    
    pgl.bindVertexBuffer(glPointVertexBufferID);
    pgl.copyVertexBufferSubData(vertices, 3 * offset, 3 * size, glMode);    
    pgl.unbindVertexBuffer();
  }
    
    
  protected void copyPointColors(int offset, int size, float[] colors) {
    pgl.bindVertexBuffer(glPointColorBufferID);
    pgl.copyVertexBufferSubData(colors, 4 * offset, 4 * size, glMode);     
    pgl.unbindVertexBuffer();
  }
    
  
  protected void copyPointNormals(int offset, int size, float[] normals) {
    pgl.bindVertexBuffer(glPointNormalBufferID);
    pgl.copyVertexBufferSubData(normals, 3 * offset, 3 * size, glMode);      
    pgl.unbindVertexBuffer();
  }

    
  protected void copyPointAttributes(int offset, int size, float[] attribs) {
    pgl.bindVertexBuffer(glPointAttribBufferID);
    pgl.copyVertexBufferSubData(attribs, 2 * offset, 2 * size, glMode);  
    pgl.unbindVertexBuffer();
  }
  
  
  protected void copyPointIndices(int offset, int size, short[] indices) {
    pgl.bindIndexBuffer(glPointIndexBufferID);
    pgl.copyIndexBufferSubData(indices, offset, size, glMode);    
    pgl.unbindIndexBuffer();    
  }    

  
  ///////////////////////////////////////////////////////////  
  
  //
  
  // Deletion methods

  
  protected void release() {
    deleteFillBuffers();
    deleteLineBuffers();
    deletePointBuffers();
  }  
  
  
  protected void deleteFillBuffers() {
    if (glFillVertexBufferID != 0) {    
      renderer.deleteVertexBufferObject(glFillVertexBufferID);   
      glFillVertexBufferID = 0;
    }    
    
    if (glFillColorBufferID != 0) {    
      renderer.deleteVertexBufferObject(glFillColorBufferID);   
      glFillColorBufferID = 0;
    }    

    if (glFillNormalBufferID != 0) {    
      renderer.deleteVertexBufferObject(glFillNormalBufferID);   
      glFillNormalBufferID = 0;
    }     

    if (glFillTexCoordBufferID != 0) {    
      renderer.deleteVertexBufferObject(glFillTexCoordBufferID);   
      glFillTexCoordBufferID = 0;
    }    
    
    if (glFillIndexBufferID != 0) {    
      renderer.deleteVertexBufferObject(glFillIndexBufferID);   
      glFillIndexBufferID = 0;
    }   
  }
  
  
  protected void deleteLineBuffers() {
    if (glLineVertexBufferID != 0) {    
      renderer.deleteVertexBufferObject(glLineVertexBufferID);   
      glLineVertexBufferID = 0;
    }    
    
    if (glLineColorBufferID != 0) {    
      renderer.deleteVertexBufferObject(glLineColorBufferID);   
      glLineColorBufferID = 0;
    }    

    if (glLineNormalBufferID != 0) {    
      renderer.deleteVertexBufferObject(glLineNormalBufferID);   
      glLineNormalBufferID = 0;
    }     

    if (glLineAttribBufferID != 0) {    
      renderer.deleteVertexBufferObject(glLineAttribBufferID);   
      glLineAttribBufferID = 0;
    }    
    
    if (glLineIndexBufferID != 0) {    
      renderer.deleteVertexBufferObject(glLineIndexBufferID);   
      glLineIndexBufferID = 0;
    }  
  }  
  
  
  protected void deletePointBuffers() {
    if (glPointVertexBufferID != 0) {    
      renderer.deleteVertexBufferObject(glPointVertexBufferID);   
      glPointVertexBufferID = 0;
    }    
    
    if (glPointColorBufferID != 0) {    
      renderer.deleteVertexBufferObject(glPointColorBufferID);   
      glPointColorBufferID = 0;
    }    

    if (glPointNormalBufferID != 0) {    
      renderer.deleteVertexBufferObject(glPointNormalBufferID);   
      glPointNormalBufferID = 0;
    }     

    if (glPointAttribBufferID != 0) {    
      renderer.deleteVertexBufferObject(glPointAttribBufferID);   
      glPointAttribBufferID = 0;
    }    
    
    if (glPointIndexBufferID != 0) {    
      renderer.deleteVertexBufferObject(glPointIndexBufferID);   
      glPointIndexBufferID = 0;
    }  
  }
  
  ///////////////////////////////////////////////////////////  
  
  //
  
  // Rendering methods
  
  
  public void draw() {
    draw(renderer);
  }
  
  
  public void draw(PGraphics g) {
    if (visible) {
      
      updateTesselation();      
      updateGeometry();
      
      if (matrix != null && applyMatrix) {
        g.pushMatrix();
        g.applyMatrix(matrix);
      }
    
      if (family == GROUP) {
        
        boolean matrixBelow = childHasMatrix;
        boolean diffTexBelow = textures != null && 1 < textures.size();
        
        if (matrixBelow || diffTexBelow) {
          // Some child shape below this group has a non-null matrix
          // transformation assigned to it, so the group cannot
          // be drawn in a single render call.
          // Or, some child shapes below this group use different
          // texture maps, so they cannot rendered in a single call
          // either.
          
          for (int i = 0; i < childCount; i++) {
            ((PShape3D) children[i]).draw(g);
          }        
        } else {
          // None of the child shapes below this group has a matrix
          // transformation applied to them, so we can render everything
          // in a single block.
          // And all have the same texture applied to them.          
          PImage tex = null;
          if (textures != null && textures.size() == 1) {
            tex = (PImage)textures.toArray()[0];
          }
          render(tex);
        }
              
      } else {
        render(texture);
      }
    
      if (matrix != null) {
        g.popMatrix();
      } 
    }
  }

  
  // Render the geometry stored in the root shape as VBOs, for the vertices 
  // corresponding to this shape. Sometimes we can have root == this.
  protected void render(PImage texture) {
    if (root == null) {
      // Some error. Root should never be null. At least it should be this.
      return; 
    }

    if (hasPoints) {
      renderPoints();
    }
    
    if (hasLines) {    
      renderLines();    
    }    
    
    if (hasFill) { 
      renderFill(texture);
    }
  }


  protected void renderPoints() {
    renderer.startPointShader();
    
    pgl.enableVertexArrays();
    pgl.enableColorArrays();
    pgl.enableNormalArrays();
    
    pgl.bindVertexBuffer(root.glPointVertexBufferID);
    pgl.setVertexFormat(3, 0, 0); 
                  
    pgl.bindVertexBuffer(root.glPointColorBufferID);    
    pgl.setColorFormat(4, 0, 0);    
    
    pgl.bindVertexBuffer(root.glPointNormalBufferID);    
    pgl.setNormalFormat(3, 0, 0);    
    
    renderer.setupPointShader(root.glPointAttribBufferID);
    
    int offset = tess.firstPointIndex;
    int size =  tess.lastPointIndex - tess.firstPointIndex + 1;
    pgl.bindIndexBuffer(root.glPointIndexBufferID);
    pgl.renderIndexBuffer(offset, size);
    
    pgl.unbindIndexBuffer();
    pgl.unbindVertexBuffer();    
    
    pgl.disableVertexArrays();
    pgl.disableColorArrays();
    pgl.disableNormalArrays();  
        
    renderer.stopPointShader();
  }  


  protected void renderLines() {
    renderer.startLineShader();
    
    pgl.enableVertexArrays();
    pgl.enableColorArrays();
    pgl.enableNormalArrays();     
    
    pgl.bindVertexBuffer(root.glLineVertexBufferID);
    pgl.setVertexFormat(3, 0, 0);  
    
    pgl.bindVertexBuffer(root.glLineColorBufferID);    
    pgl.setColorFormat(4, 0, 0);      
    
    pgl.bindVertexBuffer(root.glLineNormalBufferID);    
    pgl.setNormalFormat(3, 0, 0);      
        
    renderer.setupLineShader(root.glLineAttribBufferID);    
    
    int offset = tess.firstLineIndex;
    int size =  tess.lastLineIndex - tess.firstLineIndex + 1;
    pgl.bindIndexBuffer(root.glLineIndexBufferID);
    pgl.renderIndexBuffer(offset, size);
    
    pgl.unbindIndexBuffer();
    pgl.unbindVertexBuffer();    
    
    pgl.disableVertexArrays();
    pgl.disableColorArrays();
    pgl.disableNormalArrays();    
    
    renderer.stopLineShader();    
  }  
  
  
  protected void renderFill0(PImage textureImage) {    
    pgl.enableVertexArrays();
    pgl.enableColorArrays();
    pgl.enableNormalArrays(); 
    pgl.enableTexCoordArrays();
        
    pgl.bindVertexBuffer(root.glFillVertexBufferID);
    //pgl.setVertexFormat(3, 0, 0);      

    pgl.bindVertexBuffer(root.glFillColorBufferID);    
    //pgl.setColorFormat(4, 0, 0);           
    
    pgl.bindVertexBuffer(root.glFillNormalBufferID);    
    //pgl.setNormalFormat(3, 0, 0);     
    
    pgl.bindVertexBuffer(root.glFillTexCoordBufferID);    
    //pgl.setTexCoordFormat(2, 0, 0);      
    
    PTexture tex = null;
    if (textureImage != null) {
      tex = renderer.getTexture(textureImage);
      if (tex != null) {
        pgl.enableTexturing(tex.glTarget);
        pgl.setActiveTexUnit(0);
        pgl.bindTexture(tex.glTarget, tex.glID);        
      }
    }
    
    pgl.bindIndexBuffer(root.glFillIndexBufferID);    
    for (int i = 0; i < fillIndexData.size(); i++) {
      IndexData index = (IndexData)fillIndexData.get(i);
      
      int first = index.first;
      int offset = index.offset;
      int size =  index.size;
      //PApplet.println(first + " " + offset + " " + size);
//      pgl.setVertexFormat(3, 0, 3 * first);
//      pgl.setColorFormat(4, 0, 4 * first);
//      pgl.setNormalFormat(3, 0, 3 * first);
//      pgl.setTexCoordFormat(2, 0, 2 * first);
      pgl.renderIndexBuffer(offset, size);      
    }
    
    
//    int offset = tess.firstFillIndex;
//    int size =  tess.lastFillIndex - tess.firstFillIndex + 1;
//    pgl.renderIndexBuffer(offset, size);
    
    if (tex != null) {
      pgl.unbindTexture(tex.glTarget); 
      pgl.disableTexturing(tex.glTarget);
    } 
    
    pgl.unbindIndexBuffer();
    pgl.unbindVertexBuffer();    
    
    pgl.disableVertexArrays();
    pgl.disableColorArrays();
    pgl.disableNormalArrays(); 
    pgl.disableTexCoordArrays();    
  }

  
  protected void renderFill(PImage textureImage) {    
    pgl.enableVertexArrays();
    pgl.enableColorArrays();
    pgl.enableNormalArrays(); 
    pgl.enableTexCoordArrays();
        
    
        
    for (int i = 0; i < fillIndexData.size(); i++) {
      IndexData index = (IndexData)fillIndexData.get(i);
      
      int first = index.first;
      int offset = index.offset;
      int size =  index.size;    
    
    
    pgl.bindVertexBuffer(root.glFillVertexBufferID);
    pgl.setVertexFormat(3, 0, 3 * first);     

    pgl.bindVertexBuffer(root.glFillColorBufferID);    
    pgl.setColorFormat(4, 0, 4 * first);  
    
    pgl.bindVertexBuffer(root.glFillNormalBufferID);    
    pgl.setNormalFormat(3, 0, 3 * first);     
    
    pgl.bindVertexBuffer(root.glFillTexCoordBufferID);    
    pgl.setTexCoordFormat(2, 0, 2 * first);    
    
    PTexture tex = null;
    if (textureImage != null) {
      tex = renderer.getTexture(textureImage);
      if (tex != null) {
        pgl.enableTexturing(tex.glTarget);
        pgl.setActiveTexUnit(0);
        pgl.bindTexture(tex.glTarget, tex.glID);        
      }
    }
    
    pgl.bindIndexBuffer(root.glFillIndexBufferID);
      //PApplet.println(first + " " + offset + " " + size);
//      pgl.setVertexFormat(3, 0, 3 * first);
//      pgl.setColorFormat(4, 0, 4 * first);
//      pgl.setNormalFormat(3, 0, 3 * first);
//      pgl.setTexCoordFormat(2, 0, 2 * first);
      pgl.renderIndexBuffer(offset, size);      
    
    
    
//    int offset = tess.firstFillIndex;
//    int size =  tess.lastFillIndex - tess.firstFillIndex + 1;
//    pgl.renderIndexBuffer(offset, size);
    
    if (tex != null) {
      pgl.unbindTexture(tex.glTarget); 
      pgl.disableTexturing(tex.glTarget);
    } 
    
    pgl.unbindIndexBuffer();
    pgl.unbindVertexBuffer();    
    
    }
    
    pgl.disableVertexArrays();
    pgl.disableColorArrays();
    pgl.disableNormalArrays(); 
    pgl.disableTexCoordArrays();
  }
  
  
  
  ///////////////////////////////////////////////////////////  

  // 

  protected class IndexData {
    IndexData(int first, int offset, int size) {
      this.first = first;
      this.offset = offset;
      this.size = size;
    }  
    int first;
    int offset;
    int size;
  }
  
  ///////////////////////////////////////////////////////////  
  
  // 
  
  // Internal class to store a cache of vertex data used to copy data
  // to the VBOs with fewer calls.
  protected class VertexCache {
    int ncoords;
    int offset;
    int size;    
    float[] data;
    
    VertexCache(int ncoords) {
      this.ncoords = ncoords;
      this.data = new float[ncoords * PGL.DEFAULT_VERTEX_CACHE_SIZE];
      this.offset = 0;
      this.size = 0;      
    }
    
    void reset() {
      offset = 0;
      size = 0;
    }    
    
    void add(int dataOffset, int dataSize, float[] newData) {
      if (size == 0) {
        offset = dataOffset;
      }
      
      int oldSize = data.length / ncoords;
      if (size + dataSize >= oldSize) {
        int newSize = expandSize(oldSize, size + dataSize);        
        expand(newSize);
      }
      
      if (dataSize <= PGraphicsAndroid3D.MIN_ARRAYCOPY_SIZE) {
        // Copying elements one by one instead of using arrayCopy is more efficient for
        // few vertices...
        for (int i = 0; i < dataSize; i++) {
          int srcIndex = ncoords * i;
          int destIndex = ncoords * (size + i);
          
          if (ncoords == 2) {
            data[destIndex++] = newData[srcIndex++];
            data[destIndex  ] = newData[srcIndex  ];
          } else if (ncoords == 3) {
            data[destIndex++] = newData[srcIndex++];
            data[destIndex++] = newData[srcIndex++];
            data[destIndex  ] = newData[srcIndex  ];
          } else if (ncoords == 4) {
            data[destIndex++] = newData[srcIndex++];
            data[destIndex++] = newData[srcIndex++];
            data[destIndex++] = newData[srcIndex++];
            data[destIndex  ] = newData[srcIndex  ];            
          } else {
            for (int j = 0; j < ncoords; j++) {
              data[destIndex++] = newData[srcIndex++];
            }            
          }
        }
      } else {
        PApplet.arrayCopy(newData, 0, data, ncoords * size, ncoords * dataSize);
      }
      
      size += dataSize;
    } 
    
    void add(int dataOffset, int dataSize, float[] newData, PMatrix tr) {
      
      if (tr instanceof PMatrix2D) {
        add(dataOffset, dataSize, newData, (PMatrix2D)tr);  
      } else if (tr instanceof PMatrix3D) {
        add(dataOffset, dataSize, newData, (PMatrix3D)tr);
      }
    }
    
    void add(int dataOffset, int dataSize, float[] newData, PMatrix2D tr) {
      if (size == 0) {
        offset = dataOffset;
      }
      
      int oldSize = data.length / ncoords;
      if (size + dataSize >= oldSize) {
        int newSize = expandSize(oldSize, size + dataSize);        
        expand(newSize);
      }
      
      if (2 <= ncoords) {
        for (int i = 0; i < dataSize; i++) {
          int srcIndex = ncoords * i;
          float x = newData[srcIndex++];
          float y = newData[srcIndex  ];

          int destIndex = ncoords * (size + i); 
          data[destIndex++] = x * tr.m00 + y * tr.m01 + tr.m02;
          data[destIndex  ] = x * tr.m10 + y * tr.m11 + tr.m12;
        }        
      }
      
      size += dataSize;
    }
    
    void add(int dataOffset, int dataSize, float[] newData, PMatrix3D tr) {
      if (size == 0) {
        offset = dataOffset;
      }
      
      int oldSize = data.length / ncoords;
      if (size + dataSize >= oldSize) {
        int newSize = expandSize(oldSize, size + dataSize);        
        expand(newSize);
      }
      
      if (3 <= ncoords) {
        for (int i = 0; i < dataSize; i++) {
          int srcIndex = ncoords * i;
          float x = newData[srcIndex++];
          float y = newData[srcIndex++];
          float z = newData[srcIndex++];

          int destIndex = ncoords * (size + i); 
          data[destIndex++] = x * tr.m00 + y * tr.m01 + z * tr.m02 + tr.m03;
          data[destIndex++] = x * tr.m10 + y * tr.m11 + z * tr.m12 + tr.m13;
          data[destIndex  ] = x * tr.m20 + y * tr.m21 + z * tr.m22 + tr.m23;
        }          
      }      
      
      size += dataSize;
    }
    
    void expand(int n) {
      float temp[] = new float[ncoords * n];      
      PApplet.arrayCopy(data, 0, temp, 0, ncoords * size);
      data = temp;      
    }
    
    int expandSize(int currSize, int newMinSize) {
      int newSize = currSize; 
      while (newSize < newMinSize) {
        newSize = newSize << 1;
      }
      return newSize;
    }    
    
    boolean hasData() {
      return 0 < size;
    }
    
  }  
  
  
  ///////////////////////////////////////////////////////////////////////////   
  
  // OBJ loading
  
  
  protected BufferedReader getBufferedReader(String filename) {
    //BufferedReader retval = papplet.createReader(filename);
    BufferedReader retval = null;
    if (retval != null) {
      return retval;
    } else {
      PApplet.println("Could not find this file " + filename);
      return null;
    }
  }
  
  
  protected void parseOBJ(BufferedReader reader, ArrayList<PVector> vertices, ArrayList<PVector> normals, ArrayList<PVector> textures, ArrayList<OBJFace> faces, ArrayList<OBJMaterial> materials) {
    Hashtable<String, Integer> mtlTable  = new Hashtable<String, Integer>();
    int mtlIdxCur = -1;
    boolean readv, readvn, readvt;
    try {
      // Parse the line.
      
      readv = readvn = readvt = false;
      String line;
      String gname = "object";
      while ((line = reader.readLine()) != null) {
        
        // The below patch/hack comes from Carlos Tomas Marti and is a
        // fix for single backslashes in Rhino obj files
        
        // BEGINNING OF RHINO OBJ FILES HACK
        // Statements can be broken in multiple lines using '\' at the
        // end of a line.
        // In regular expressions, the backslash is also an escape
        // character.
        // The regular expression \\ matches a single backslash. This
        // regular expression as a Java string, becomes "\\\\".
        // That's right: 4 backslashes to match a single one.
        while (line.contains("\\")) {
          line = line.split("\\\\")[0];
          final String s = reader.readLine();
          if (s != null)
            line += s;
        }
        // END OF RHINO OBJ FILES HACK
        
        String[] elements = line.split("\\s+");        
        // if not a blank line, process the line.
        if (elements.length > 0) {
          if (elements[0].equals("v")) {
            // vertex
            PVector tempv = new PVector(Float.valueOf(elements[1]).floatValue(), Float.valueOf(elements[2]).floatValue(), Float.valueOf(elements[3]).floatValue());
            vertices.add(tempv);
            readv = true;
          } else if (elements[0].equals("vn")) {
            // normal
            PVector tempn = new PVector(Float.valueOf(elements[1]).floatValue(), Float.valueOf(elements[2]).floatValue(), Float.valueOf(elements[3]).floatValue());
            normals.add(tempn);
            readvn = true;
          } else if (elements[0].equals("vt")) {
            // uv
            PVector tempv = new PVector(Float.valueOf(elements[1]).floatValue(), Float.valueOf(elements[2]).floatValue());
            textures.add(tempv);
            readvt = true;
          } else if (elements[0].equals("o")) {
            // Object name is ignored, for now.
          } else if (elements[0].equals("mtllib")) {
            if (elements[1] != null) {
              parseMTL(getBufferedReader(elements[1]), materials, mtlTable); 
            }
          } else if (elements[0].equals("g")) {            
            gname = elements[1];
          } else if (elements[0].equals("usemtl")) {
            // Getting index of current active material (will be applied on all subsequent faces)..
            if (elements[1] != null) {
              String mtlname = elements[1];
              if (mtlTable.containsKey(mtlname)) {
                Integer tempInt = mtlTable.get(mtlname);
                mtlIdxCur = tempInt.intValue();
              } else {
                mtlIdxCur = -1;                
              }
            }
          } else if (elements[0].equals("f")) {
            // Face setting
            OBJFace face = new OBJFace();
            face.matIdx = mtlIdxCur; 
            face.name = gname;
            
            for (int i = 1; i < elements.length; i++) {
              String seg = elements[i];

              if (seg.indexOf("/") > 0) {
                String[] forder = seg.split("/");

                if (forder.length > 2) {
                  // Getting vertex and texture and normal indexes.
                  if (forder[0].length() > 0 && readv) {
                    face.vertIdx.add(Integer.valueOf(forder[0]));
                  }

                  if (forder[1].length() > 0 && readvt) {
                    face.texIdx.add(Integer.valueOf(forder[1]));
                  }

                  if (forder[2].length() > 0 && readvn) {
                    face.normIdx.add(Integer.valueOf(forder[2]));
                  }
                } else if (forder.length > 1) {
                  // Getting vertex and texture/normal indexes.
                  if (forder[0].length() > 0 && readv) {
                    face.vertIdx.add(Integer.valueOf(forder[0]));
                  }
 
                  if (forder[1].length() > 0) {
                    if (readvt) {
                      face.texIdx.add(Integer.valueOf(forder[1]));  
                    } else  if (readvn) {
                      face.normIdx.add(Integer.valueOf(forder[1]));
                    }
                    
                  }
                  
                } else if (forder.length > 0) {
                  // Getting vertex index only.
                  if (forder[0].length() > 0 && readv) {
                    face.vertIdx.add(Integer.valueOf(forder[0]));
                  }
                }
              } else {
                // Getting vertex index only.
                if (seg.length() > 0 && readv) {
                  face.vertIdx.add(Integer.valueOf(seg));
                }
              }
            }
           
            faces.add(face);
            
          }
        }
      }

      if (materials.size() == 0) {
        // No materials definition so far. Adding one default material.
        OBJMaterial defMtl = new OBJMaterial(); 
        materials.add(defMtl);
      }      
      
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  
  protected void parseMTL(BufferedReader reader, ArrayList<OBJMaterial> materials, Hashtable<String, Integer> materialsHash) {
    try {
      String line;
      OBJMaterial currentMtl = null;
      while ((line = reader.readLine()) != null) {
        // Parse the line
        line = line.trim();

        String elements[] = line.split("\\s+");

        if (elements.length > 0) {
          // Extract the material data.

          if (elements[0].equals("newmtl")) {
            // Starting new material.
            String mtlname = elements[1];
            currentMtl = new OBJMaterial(mtlname);
            materialsHash.put(mtlname, new Integer(materials.size()));
            materials.add(currentMtl);
          } else if (elements[0].equals("map_Kd") && elements.length > 1) {
            // Loading texture map.
            String texname = elements[1];
            //currentMtl.kdMap = papplet.loadImage(texname);
            currentMtl.kdMap = null;
          } else if (elements[0].equals("Ka") && elements.length > 3) {
            // The ambient color of the material
            currentMtl.ka.x = Float.valueOf(elements[1]).floatValue();
            currentMtl.ka.y = Float.valueOf(elements[2]).floatValue();
            currentMtl.ka.z = Float.valueOf(elements[3]).floatValue();
          } else if (elements[0].equals("Kd") && elements.length > 3) {
            // The diffuse color of the material
            currentMtl.kd.x = Float.valueOf(elements[1]).floatValue();
            currentMtl.kd.y = Float.valueOf(elements[2]).floatValue();
            currentMtl.kd.z = Float.valueOf(elements[3]).floatValue();
          } else if (elements[0].equals("Ks") && elements.length > 3) {
            // The specular color weighted by the specular coefficient
            currentMtl.ks.x = Float.valueOf(elements[1]).floatValue();
            currentMtl.ks.y = Float.valueOf(elements[2]).floatValue();
            currentMtl.ks.z = Float.valueOf(elements[3]).floatValue();
          } else if ((elements[0].equals("d") || elements[0].equals("Tr")) && elements.length > 1) {
            // Reading the alpha transparency.
            currentMtl.d = Float.valueOf(elements[1]).floatValue();
          } else if (elements[0].equals("Ns") && elements.length > 1) {
            // The specular component of the Phong shading model
            currentMtl.ns = Float.valueOf(elements[1]).floatValue();
          } 
          
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }    
  }
  
  protected void recordOBJ() {
//    recordOBJ(objVertices, objNormal, objTexCoords, objFaces, objMaterials);
//    objVertices = null; 
//    objNormal = null; 
//    objTexCoords = null;    
//    objFaces = null;
//    objMaterials = null;    
//    
//    readFromOBJ = false;
  }
  
  protected void recordOBJ(ArrayList<PVector> vertices, ArrayList<PVector> normals, ArrayList<PVector> textures, ArrayList<OBJFace> faces, ArrayList<OBJMaterial> materials) {
    int mtlIdxCur = -1;
    OBJMaterial mtl = null;
    
    renderer.saveDrawingState();
    
    // The recorded shapes are not merged, they are grouped
    // according to the group names found in the OBJ file.    
    //ogl.mergeRecShapes = false;
    
    // Using RGB mode for coloring.
    renderer.colorMode = RGB;
    
    // Strokes are not used to draw the model.
    renderer.stroke = false;    
    
    // Normals are automatically computed if not specified in the OBJ file.
    //renderer.autoNormal(true);
    
    // Using normal mode for texture coordinates (i.e.: normalized between 0 and 1).
    renderer.textureMode = NORMAL;    
    
    //ogl.beginShapeRecorderImpl();    
    for (int i = 0; i < faces.size(); i++) {
      OBJFace face = faces.get(i);
      
      // Getting current material.
      if (mtlIdxCur != face.matIdx) {
        mtlIdxCur = PApplet.max(0, face.matIdx); // To make sure that at least we get the default material.
        
        mtl = materials.get(mtlIdxCur);

        // Setting colors.
        renderer.specular(mtl.ks.x * 255.0f, mtl.ks.y * 255.0f, mtl.ks.z * 255.0f);
        renderer.ambient(mtl.ka.x * 255.0f, mtl.ka.y * 255.0f, mtl.ka.z * 255.0f);
        if (renderer.fill) {
          renderer.fill(mtl.kd.x * 255.0f, mtl.kd.y * 255.0f, mtl.kd.z * 255.0f, mtl.d * 255.0f);  
        }        
        renderer.shininess(mtl.ns);
        
        if (renderer.tint && mtl.kdMap != null) {
          // If current material is textured, then tinting the texture using the diffuse color.
          renderer.tint(mtl.kd.x * 255.0f, mtl.kd.y * 255.0f, mtl.kd.z * 255.0f, mtl.d * 255.0f);
        }
      }

      // Recording current face.
      if (face.vertIdx.size() == 3) {
        renderer.beginShape(TRIANGLES); // Face is a triangle, so using appropriate shape kind.
      } else if (face.vertIdx.size() == 4) {
        renderer.beginShape(QUADS);        // Face is a quad, so using appropriate shape kind.
      } else {
        renderer.beginShape();  
      }      
      
      //renderer.shapeName(face.name);
      
      for (int j = 0; j < face.vertIdx.size(); j++){
        int vertIdx, normIdx;
        PVector vert, norms;

        vert = norms = null;
        
        vertIdx = face.vertIdx.get(j).intValue() - 1;
        vert = vertices.get(vertIdx);
        
        if (j < face.normIdx.size()) {
          normIdx = face.normIdx.get(j).intValue() - 1;
          if (-1 < normIdx) {
            norms = normals.get(normIdx);  
          }
        }
        
        if (mtl != null && mtl.kdMap != null) {
          // This face is textured.
          int texIdx;
          PVector tex = null; 
          
          if (j < face.texIdx.size()) {
            texIdx = face.texIdx.get(j).intValue() - 1;
            if (-1 < texIdx) {
              tex = textures.get(texIdx);  
            }
          }
          
          PTexture texMtl = (PTexture)mtl.kdMap.getCache(renderer);
          if (texMtl != null) {     
            // Texture orientation in Processing is inverted.
            texMtl.setFlippedY(true);          
          }
          renderer.texture(mtl.kdMap);
          if (norms != null) {
            renderer.normal(norms.x, norms.y, norms.z);
          }
          if (tex != null) {
            renderer.vertex(vert.x, vert.y, vert.z, tex.x, tex.y);  
          } else {
            renderer.vertex(vert.x, vert.y, vert.z);
          }
        } else {
          // This face is not textured.
          if (norms != null) {
            renderer.normal(norms.x, norms.y, norms.z);
          }
          renderer.vertex(vert.x, vert.y, vert.z);          
        }
      } 
      renderer.endShape(CLOSE);
    }
    
    // Allocate space for the geometry that the triangulator has generated from the OBJ model.
    //setSize(ogl.recordedVertices.size());
//    allocate();
//    initChildrenData();
//    updateElement = -1;
    
    width = height = depth = 0;
//    xmin = ymin = zmin = 10000;
//    xmax = ymax = zmax = -10000;
    
    //ogl.endShapeRecorderImpl(this);
    //ogl.endShapeRecorderImpl(null);
    
    renderer.restoreDrawingState();    
  }
  

  protected class OBJFace {
    ArrayList<Integer> vertIdx;
    ArrayList<Integer> texIdx;
    ArrayList<Integer> normIdx;
    int matIdx;
    String name;
    
    OBJFace() {
      vertIdx = new ArrayList<Integer>();
      texIdx = new ArrayList<Integer>();
      normIdx = new ArrayList<Integer>();
      matIdx = -1;
      name = "";
    }
  }

  protected class OBJMaterial {
    String name;
    PVector ka;
    PVector kd;
    PVector ks;
    float d;
    float ns;
    PImage kdMap;
    
    OBJMaterial() {
      this("default");
    }
    
    OBJMaterial(String name) {
      this.name = name;
      ka = new PVector(0.5f, 0.5f, 0.5f);
      kd = new PVector(0.5f, 0.5f, 0.5f);
      ks = new PVector(0.5f, 0.5f, 0.5f);
      d = 1.0f;
      ns = 0.0f;
      kdMap = null;
    }    
  }

  
  
}
