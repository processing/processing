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

import processing.core.PGraphicsAndroid3D.FillShader;
import processing.core.PGraphicsAndroid3D.InGeometry;
import processing.core.PGraphicsAndroid3D.LineShader;
import processing.core.PGraphicsAndroid3D.PointShader;
import processing.core.PGraphicsAndroid3D.TessGeometry;
import processing.core.PGraphicsAndroid3D.Tessellator;
import java.nio.FloatBuffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Arrays;
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

public class PShape3D extends PShape {
  protected PGraphicsAndroid3D pg;
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
  public int glFillAmbientBufferID;
  public int glFillSpecularBufferID;
  public int glFillEmissiveBufferID;
  public int glFillShininessBufferID;
  public int glFillIndexBufferID;
  
  public int glLineVertexBufferID;
  public int glLineColorBufferID;
  public int glLineDirWidthBufferID;
  public int glLineIndexBufferID;  
  
  public int glPointVertexBufferID;
  public int glPointColorBufferID;
  public int glPointSizeBufferID;
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
  protected int firstFillVertexRel;
  protected int firstFillVertexAbs;
  
  protected int lastLineVertexOffset;
  protected int lastLineIndexOffset; 
  protected int firstLineVertexRel;
  protected int firstLineVertexAbs;
  
  protected int lastPointVertexOffset;
  protected int lastPointIndexOffset;    
  protected int firstPointVertexRel;
  protected int firstPointVertexAbs;
  
  // ........................................................
  
  // Drawing/rendering state
  
  protected boolean tessellated;
  
  boolean modifiedFillVertices;
  boolean modifiedFillColors;
  boolean modifiedFillNormals;
  boolean modifiedFillTexCoords;  
  boolean modifiedFillAmbient;
  boolean modifiedFillSpecular;
  boolean modifiedFillEmissive;
  boolean modifiedFillShininess;
  
  boolean modifiedLineVertices;
  boolean modifiedLineColors;
  boolean modifiedLineAttributes;  

  boolean modifiedPointVertices;
  boolean modifiedPointColors;
  boolean modifiedPointNormals;
  boolean modifiedPointAttributes;  

  protected VertexCache fillVerticesCache;
  protected VertexCache fillColorsCache;
  protected VertexCache fillNormalsCache;
  protected VertexCache fillTexCoordsCache;
  protected VertexCache fillAmbientCache;
  protected VertexCache fillSpecularCache;
  protected VertexCache fillEmissiveCache;
  protected VertexCache fillShininessCache;
  
  protected VertexCache lineVerticesCache;
  protected VertexCache lineColorsCache;
  protected VertexCache lineAttributesCache;  

  protected VertexCache pointVerticesCache;
  protected VertexCache pointColorsCache;
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
    pg = (PGraphicsAndroid3D)parent.g;
    pgl = pg.pgl;
    
    glMode = PGL.GL_STATIC_DRAW;
    
    glFillVertexBufferID = 0;
    glFillColorBufferID = 0;
    glFillNormalBufferID = 0;
    glFillTexCoordBufferID = 0;
    glFillAmbientBufferID = 0;
    glFillSpecularBufferID = 0;
    glFillEmissiveBufferID = 0;
    glFillShininessBufferID = 0;     
    glFillIndexBufferID = 0;
    
    glLineVertexBufferID = 0;
    glLineColorBufferID = 0;
    glLineDirWidthBufferID = 0;
    glLineIndexBufferID = 0;
    
    glPointVertexBufferID = 0;
    glPointColorBufferID = 0;
    glPointSizeBufferID = 0;
    glPointIndexBufferID = 0;
    
    this.tessellator = pg.tessellator;
    this.family = family;    
    this.root = this;
    this.parent = null;
    this.tessellated = false;
    
    if (family == GEOMETRY || family == PRIMITIVE || family == PATH) {
      in = pg.newInGeometry(RETAINED);      
    }    
    tess = pg.newTessGeometry(RETAINED);
    fillIndexData = new ArrayList<IndexData>();
    lineIndexData = new ArrayList<IndexData>();
    pointIndexData = new ArrayList<IndexData>();
    
    // Modes are retrieved from the current values in the renderer.
    textureMode = pg.textureMode;    
    rectMode = pg.rectMode;
    ellipseMode = pg.ellipseMode;
    shapeMode = pg.shapeMode;
    imageMode = pg.imageMode;    
    
    colorMode(pg.colorMode, pg.colorModeX, pg.colorModeY, pg.colorModeZ, pg.colorModeA);
    
    // Initial values for fill, stroke and tint colors are also imported from the renderer.
    // This is particular relevant for primitive shapes, since is not possible to set 
    // their color separately when creating them, and their input vertices are actually
    // generated at rendering time, by which the color configuration of the renderer might
    // have changed.
    fill = pg.fill;
    fillColor = pg.fillColor;
    
    stroke = pg.stroke;      
    strokeColor = pg.strokeColor;     
    strokeWeight = pg.strokeWeight;    
    
    tint = pg.tint;  
    tintColor = pg.tintColor;

    ambientColor = pg.ambientColor;  
    specularColor = pg.specularColor;  
    emissiveColor = pg.emissiveColor;
    shininess = pg.shininess;
    
    normalX = normalY = 0; 
    normalZ = 1;
    
    normalMode = NORMAL_MODE_AUTO;
  }
  
  
  public void setKind(int kind) {
    this.kind = kind;
  }

  
  public void setMode(int mode) {
    if (mode == STATIC) {
      glMode = PGL.GL_STATIC_DRAW;
    } else if (mode == DYNAMIC) {
      glMode = PGL.GL_DYNAMIC_DRAW;
    } else if (mode == STREAM) {
      glMode = PGL.GL_STREAM_DRAW;
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
      pg.finalizeVertexBufferObject(glFillVertexBufferID);   
    }    
    
    if (glFillColorBufferID != 0) {    
      pg.finalizeVertexBufferObject(glFillColorBufferID);   
    }    

    if (glFillNormalBufferID != 0) {    
      pg.finalizeVertexBufferObject(glFillNormalBufferID);   
    }     

    if (glFillTexCoordBufferID != 0) {    
      pg.finalizeVertexBufferObject(glFillTexCoordBufferID);   
    }    

    if (glFillAmbientBufferID != 0) {    
      pg.finalizeVertexBufferObject(glFillAmbientBufferID);   
    }    
    
    if (glFillSpecularBufferID != 0) {    
      pg.finalizeVertexBufferObject(glFillSpecularBufferID);   
    }    

    if (glFillEmissiveBufferID != 0) {    
      pg.finalizeVertexBufferObject(glFillEmissiveBufferID);   
    }     

    if (glFillShininessBufferID != 0) {    
      pg.finalizeVertexBufferObject(glFillShininessBufferID);   
    }    
    
    if (glFillIndexBufferID != 0) {    
      pg.finalizeVertexBufferObject(glFillIndexBufferID);   
    }   
  }
  
  protected void finalizeLineBuffers() {
    if (glLineVertexBufferID != 0) {    
      pg.finalizeVertexBufferObject(glLineVertexBufferID);   
    }    
    
    if (glLineColorBufferID != 0) {    
      pg.finalizeVertexBufferObject(glLineColorBufferID);   
    }    

    if (glLineDirWidthBufferID != 0) {    
      pg.finalizeVertexBufferObject(glLineDirWidthBufferID);   
    }    
    
    if (glLineIndexBufferID != 0) {    
      pg.finalizeVertexBufferObject(glLineIndexBufferID);   
    }  
  }  
  
  protected void finalizePointBuffers() {
    if (glPointVertexBufferID != 0) {    
      pg.finalizeVertexBufferObject(glPointVertexBufferID);   
    }    
    
    if (glPointColorBufferID != 0) {    
      pg.finalizeVertexBufferObject(glPointColorBufferID);   
    }    

    if (glPointSizeBufferID != 0) {    
      pg.finalizeVertexBufferObject(glPointSizeBufferID);   
    }    
    
    if (glPointIndexBufferID != 0) {    
      pg.finalizeVertexBufferObject(glPointIndexBufferID);   
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
    int fcolor = 0x00;
    if (fill || textured) {
      if (!textured) {
        fcolor = fillColor;
      } else {       
        if (tint) {
          fcolor = tintColor;
        } else {
          fcolor = 0xffFFFFFF;
        }
      }
    }    
    
    if (texture != null && textureMode == IMAGE) {
      u = PApplet.min(1, u / texture.width);
      v = PApplet.min(1, v / texture.height);
    }
        
    int scolor = 0x00;
    float sweight = 0;
    if (stroke) {
      scolor = strokeColor;
      sweight = strokeWeight;
    }    
    
    if (breakShape) {
      code = BREAK;
      breakShape = false;
    }    
    
    in.addVertex(x, y, z, 
                 fcolor, 
                 normalX, normalY, normalZ,
                 u, v, 
                 scolor, sweight,
                 ambientColor, specularColor, emissiveColor, shininess,
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
    
    // Input arrays are trimmed since they are expanded by doubling their old size,
    // which might lead to arrays larger than the vertex counts.
    in.trim();
    
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
    fillColor = calcColor;
    updateFillColor();  
  }

  
  protected void updateFillColor() {
    if (!shapeEnded || tess.fillVertexCount == 0 || texture != null) {
      return;
    }
      
    updateTesselation();
    
    Arrays.fill(tess.fillColors, 0, tess.fillVertexCount, fillColor);

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
    strokeColor = calcColor;
    updateStrokeColor();  
  }

  
  protected void updateStrokeColor() {
    if (shapeEnded) {
      updateTesselation();
      
      if (0 < tess.lineVertexCount) {
        Arrays.fill(tess.lineColors, 0, tess.lineVertexCount, strokeColor);
        modifiedLineColors = true;
        modified();         
      }
      
      if (0 < tess.pointVertexCount) {
        Arrays.fill(tess.pointColors, 0, tess.pointVertexCount, strokeColor);
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
    tintColor = calcColor;
    updateTintColor();  
  }  
  
  
  protected void updateTintColor() {    
    if (!shapeEnded || tess.fillVertexCount == 0 || texture == null) {
      return;
    }
      
    updateTesselation();
    
    Arrays.fill(tess.fillColors, 0, tess.pointVertexCount, tintColor);

    modifiedFillColors = true;
    modified();  
  }
  
  //////////////////////////////////////////////////////////////

  // AMBIENT COLOR
  
  public void ambient(int rgb) {
    if (family == GROUP) {
      for (int i = 0; i < childCount; i++) {
        PShape3D child = (PShape3D) children[i];        
        child.ambient(rgb);        
      }      
    } else {
      colorCalc(rgb);
      ambientFromCalc();      
    }    
  }


  public void ambient(float gray) {
    if (family == GROUP) {
      for (int i = 0; i < childCount; i++) {
        PShape3D child = (PShape3D) children[i];        
        child.ambient(gray);        
      }      
    } else {
      colorCalc(gray);
      ambientFromCalc();      
    }     
  }


  public void ambient(float x, float y, float z) {
    if (family == GROUP) {
      for (int i = 0; i < childCount; i++) {
        PShape3D child = (PShape3D) children[i];        
        child.ambient(x, y, z);        
      }      
    } else {
      colorCalc(x, y, z);
      ambientFromCalc();      
    }      
  }
  
  protected void ambientFromCalc() {
    ambientColor = calcColor;
    updateAmbientColor();      
  }

  protected void updateAmbientColor() {    
    if (!shapeEnded || tess.fillVertexCount == 0 || texture == null) {
      return;
    }
      
    updateTesselation();
    
    Arrays.fill(tess.fillAmbient, 0, tess.fillVertexCount, ambientColor);
    
    modifiedFillAmbient = true;
    modified();      
  }
  
  //////////////////////////////////////////////////////////////

  // SPECULAR COLOR  
  

  public void specular(int rgb) {
    if (family == GROUP) {
      for (int i = 0; i < childCount; i++) {
        PShape3D child = (PShape3D) children[i];        
        child.specular(rgb);        
      }      
    } else {
      colorCalc(rgb);
      specularFromCalc();      
    }      
  }


  public void specular(float gray) {
    if (family == GROUP) {
      for (int i = 0; i < childCount; i++) {
        PShape3D child = (PShape3D) children[i];        
        child.specular(gray);        
      }      
    } else {
      colorCalc(gray);
      specularFromCalc();      
    }     
  }


  public void specular(float x, float y, float z) {
    if (family == GROUP) {
      for (int i = 0; i < childCount; i++) {
        PShape3D child = (PShape3D) children[i];        
        child.specular(x, y, z);        
      }      
    } else {
      colorCalc(x, y, z);
      specularFromCalc();      
    }      
  }
  
  protected void specularFromCalc() {
    specularColor = calcColor;
    updateSpecularColor();    
  }

  protected void updateSpecularColor() {
    if (!shapeEnded || tess.fillVertexCount == 0 || texture == null) {
      return;
    }
      
    updateTesselation();
    
    Arrays.fill(tess.fillSpecular, 0, tess.fillVertexCount, specularColor);
    
    modifiedFillSpecular = true;
    modified();     
  }
  
  //////////////////////////////////////////////////////////////

  // EMISSIVE COLOR
  
  
  public void emissive(int rgb) {
    if (family == GROUP) {
      for (int i = 0; i < childCount; i++) {
        PShape3D child = (PShape3D) children[i];        
        child.emissive(rgb);        
      }      
    } else {
      colorCalc(rgb);
      emissiveFromCalc();      
    }      
  }


  public void emissive(float gray) {
    if (family == GROUP) {
      for (int i = 0; i < childCount; i++) {
        PShape3D child = (PShape3D) children[i];        
        child.emissive(gray);        
      }      
    } else {
      colorCalc(gray);
      emissiveFromCalc();      
    }     
  }


  public void emissive(float x, float y, float z) {
    if (family == GROUP) {
      for (int i = 0; i < childCount; i++) {
        PShape3D child = (PShape3D) children[i];        
        child.emissive(x, y, z);        
      }      
    } else {
      colorCalc(x, y, z);
      emissiveFromCalc();      
    }      
  }
  
  protected void emissiveFromCalc() {
    emissiveColor = calcColor;
    updateEmissiveColor();     
  }

  protected void updateEmissiveColor() {   
    if (!shapeEnded || tess.fillVertexCount == 0 || texture == null) {
      return;
    }
      
    updateTesselation();
    
    Arrays.fill(tess.fillEmissive, 0, tess.fillVertexCount, emissiveColor);
    
    modifiedFillEmissive = true;
    modified();    
  }
  
  //////////////////////////////////////////////////////////////

  // SHININESS  
  
  
  public void shininess(float shine) {
    if (family == GROUP) {
      for (int i = 0; i < childCount; i++) {
        PShape3D child = (PShape3D) children[i];        
        child.shininess(shine);        
      }      
    } else {
      shininess = shine;
      updateShininessFactor();    
    }       
  }
  
  
  protected void updateShininessFactor() {
    if (!shapeEnded || tess.fillVertexCount == 0 || texture == null) {
      return;
    }
      
    updateTesselation();
    
    Arrays.fill(tess.fillShininess, 0, tess.fillVertexCount, shininess);
    
    modifiedFillShininess = true;
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
    pg.splineForward(detail, bezierDrawMatrix);

    // multiply the basis and forward diff matrices together
    // saves much time since this needn't be done for each curve
    bezierDrawMatrix.apply(pg.bezierBasisMatrix);
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

    pg.splineForward(curveDetail, curveDrawMatrix);

    if (bezierBasisInverse == null) {
      bezierBasisInverse = pg.bezierBasisMatrix.get();
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
  
  public int[] fillColors() {
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

  public int[] fillAmbient() {
    updateTesselation();
    return tess.fillAmbient;
  }  

  public int[] fillSpecular() {
    updateTesselation();
    return tess.fillSpecular;
  }

  public int[] fillEmissive() {
    updateTesselation();
    return tess.fillEmissive;
  }

  public float[] fillShininess() {
    updateTesselation();
    return tess.fillShininess;
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
  
  public int[] lineColors() {
    updateTesselation();
    return tess.lineColors;
  }  
  
  public float[] lineAttributes() {
    updateTesselation();
    return tess.lineDirWidths;
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
  
  public int[] pointColors() {
    updateTesselation();
    return tess.pointColors;
  }  
  
  public float[] pointAttributes() {
    updateTesselation();
    return tess.pointSizes;
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
  
  public IntBuffer mapFillAmbient() {        
    return mapVertexImpl(root.glFillAmbientBufferID, tess.firstFillVertex, tess.fillVertexCount).asIntBuffer();
  }
  
  public void unmapFillAmbient() {
    unmapVertexImpl();
  }

  public IntBuffer mapFillSpecular() {        
    return mapVertexImpl(root.glFillSpecularBufferID, tess.firstFillVertex, tess.fillVertexCount).asIntBuffer();
  }
  
  public void unmapFillSpecular() {
    unmapVertexImpl();
  }
  
  public IntBuffer mapFillEmissive() {        
    return mapVertexImpl(root.glFillEmissiveBufferID, tess.firstFillVertex, tess.fillVertexCount).asIntBuffer();
  }
  
  public void unmapFillEmissive() {
    unmapVertexImpl();
  }

  public FloatBuffer mapFillShininess() {        
    return mapVertexImpl(root.glFillShininessBufferID, tess.firstFillVertex, tess.fillVertexCount).asFloatBuffer();
  }
  
  public void unmapFillShininess() {
    unmapVertexImpl();
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
  
  public void unmapLineNormals() {
    unmapVertexImpl();
  }
  
  public FloatBuffer mapLineAttributes() {        
    return mapVertexImpl(root.glLineDirWidthBufferID, 2 * tess.firstLineVertex, 2 * tess.lineVertexCount).asFloatBuffer();
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
  
  public void unmapPointNormals() {
    unmapVertexImpl();
  }
  
  public FloatBuffer mapPointAttributes() {        
    return mapVertexImpl(root.glPointSizeBufferID, 2 * tess.firstPointVertex, 2 * tess.pointVertexCount).asFloatBuffer();
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
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, id);
    ByteBuffer bb;
    if (root == this) {            
      bb = pgl.glMapBuffer(PGL.GL_ARRAY_BUFFER, PGL.GL_READ_WRITE);  
    } else {
      bb = pgl.glMapBufferRange(PGL.GL_ARRAY_BUFFER, offset, count, PGL.GL_READ_WRITE); 
    }
    return bb;
  }
  
  protected void unmapVertexImpl() {
    pgl.glUnmapBuffer(PGL.GL_ARRAY_BUFFER);
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, 0);    
  }
  
  protected ByteBuffer mapIndexImpl(int id, int offset, int count) {
    updateTesselation();
    pgl.glBindBuffer(PGL.GL_ELEMENT_ARRAY_BUFFER, id);
    ByteBuffer bb;
    if (root == this) {            
      bb = pgl.glMapBuffer(PGL.GL_ELEMENT_ARRAY_BUFFER, PGL.GL_READ_WRITE);  
    } else {
      bb = pgl.glMapBufferRange(PGL.GL_ELEMENT_ARRAY_BUFFER, offset, count, PGL.GL_READ_WRITE);
    }
    return bb;
  }
  
  protected void unmapIndexImpl() {
    pgl.glUnmapBuffer(PGL.GL_ELEMENT_ARRAY_BUFFER);
    pgl.glBindBuffer(PGL.GL_ELEMENT_ARRAY_BUFFER, 0);    
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
        
        if (family == GEOMETRY) {
          if (kind == POINTS) {
            tessellator.tessellatePoints();    
          } else if (kind == LINES) {
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
        
        // Tessellated arrays are trimmed since they are expanded by doubling their old size,
        // which might lead to arrays larger than the vertex counts.        
        tess.trim();
        
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
                       fill, fillColor, 
                       stroke, strokeColor, strokeWeight,
                       ambientColor, specularColor, emissiveColor, shininess);
    
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
    int nu = pg.sphereDetailU;
    int nv = pg.sphereDetailV;
    
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
        root.copyFillVertices(root.fillVerticesCache.offset, root.fillVerticesCache.size, root.fillVerticesCache.floatData);
        root.fillVerticesCache.reset();
      }
      
      if (root.fillColorsCache != null && root.fillColorsCache.hasData()) {
        root.copyFillColors(root.fillColorsCache.offset, root.fillColorsCache.size, root.fillColorsCache.intData);
        root.fillColorsCache.reset();
      }
      
      if (root.fillNormalsCache != null && root.fillNormalsCache.hasData()) {
        root.copyFillNormals(root.fillNormalsCache.offset, root.fillNormalsCache.size, root.fillNormalsCache.floatData);
        root.fillNormalsCache.reset();
      }
      
      if (root.fillTexCoordsCache != null && root.fillTexCoordsCache.hasData()) {
        root.copyFillTexCoords(root.fillTexCoordsCache.offset, root.fillTexCoordsCache.size, root.fillTexCoordsCache.floatData);
        root.fillTexCoordsCache.reset();
      }
      
      if (root.lineVerticesCache != null && root.lineVerticesCache.hasData()) {
        root.copyLineVertices(root.lineVerticesCache.offset, root.lineVerticesCache.size, root.lineVerticesCache.floatData);
        root.lineVerticesCache.reset();
      }
      
      if (root.lineColorsCache != null && root.lineColorsCache.hasData()) {
        root.copyLineColors(root.lineColorsCache.offset, root.lineColorsCache.size, root.lineColorsCache.intData);
        root.lineColorsCache.reset();
      }
      
      if (root.lineAttributesCache != null && root.lineAttributesCache.hasData()) {
        root.copyLineAttributes(root.lineAttributesCache.offset, root.lineAttributesCache.size, root.lineAttributesCache.floatData);
        root.lineAttributesCache.reset();
      }      
    
     if (root.pointVerticesCache != null && root.pointVerticesCache.hasData()) {
        root.copyPointVertices(root.pointVerticesCache.offset, root.pointVerticesCache.size, root.pointVerticesCache.floatData);
        root.pointVerticesCache.reset();
      }
      
      if (root.pointColorsCache != null && root.pointColorsCache.hasData()) {
        root.copyPointColors(root.pointColorsCache.offset, root.pointColorsCache.size, root.pointColorsCache.intData);
        root.pointColorsCache.reset();
      }
      
      if (root.pointAttributesCache != null && root.pointAttributesCache.hasData()) {
        root.copyPointAttributes(root.pointAttributesCache.offset, root.pointAttributesCache.size, root.pointAttributesCache.floatData);
        root.pointAttributesCache.reset();
      }        
    }
  }
  
  
  protected void aggregate() {
    if (root == this && parent == null) {
      // We recursively calculate the total number of vertices and indices.
      lastFillVertexOffset = 0;
      lastFillIndexOffset = 0;      
      firstFillVertexRel = 0;
      firstFillVertexAbs = 0;  
      
      lastLineVertexOffset = 0;
      lastLineIndexOffset = 0;
      firstLineVertexRel = 0;
      firstLineVertexAbs = 0;
      
      lastPointVertexOffset = 0;
      lastPointIndexOffset = 0;      
      firstPointVertexRel = 0;
      firstPointVertexAbs = 0;
      
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
      
      addFillIndexData();
      addLineIndexData();
      addPointIndexData();
    } else {
      if (0 < tess.fillVertexCount && 0 < tess.fillIndexCount) {
        if (PGL.MAX_TESS_VERTICES < root.firstFillVertexRel + tess.fillVertexCount) {
          root.firstFillVertexRel = 0;
          root.firstFillVertexAbs = root.lastFillVertexOffset + 1;          
        } 
        root.lastFillVertexOffset = tess.setFillVertex(root.lastFillVertexOffset);              
        root.lastFillIndexOffset = tess.setFillIndex(root.firstFillVertexRel, root.lastFillIndexOffset);        
        root.firstFillVertexRel += tess.fillVertexCount;
        addFillIndexData(root.firstFillVertexAbs, tess.firstFillIndex, tess.lastFillIndex - tess.firstFillIndex + 1);
      }
            
      if (0 < tess.lineVertexCount && 0 < tess.lineIndexCount) {
        if (PGL.MAX_TESS_VERTICES < root.firstLineVertexRel + tess.lineVertexCount) {
          root.firstLineVertexRel = 0;
          root.firstLineVertexAbs = root.lastLineVertexOffset + 1;          
        }        
        root.lastLineVertexOffset = tess.setLineVertex(root.lastLineVertexOffset);
        root.lastLineIndexOffset = tess.setLineIndex(root.firstLineVertexRel, root.lastLineIndexOffset);
        addLineIndexData(root.firstLineVertexAbs, tess.firstLineIndex, tess.lastLineIndex - tess.firstLineIndex + 1);
      }
            
      if (0 < tess.pointVertexCount && 0 < tess.pointIndexCount) {
        if (PGL.MAX_TESS_VERTICES < root.firstPointVertexRel + tess.pointVertexCount) {
          root.firstPointVertexRel = 0;
          root.firstPointVertexAbs = root.lastPointVertexOffset + 1;          
        }                
        root.lastPointVertexOffset = tess.setPointVertex(root.lastPointVertexOffset);
        root.lastPointIndexOffset = tess.setPointIndex(root.firstPointVertexRel, root.lastPointIndexOffset);
        addPointIndexData(root.firstPointVertexAbs, tess.firstPointIndex, tess.lastPointIndex - tess.firstPointIndex + 1);
      }      
    }
    
    hasFill = 0 < tess.fillVertexCount && 0 < tess.fillIndexCount;
    hasLines = 0 < tess.lineVertexCount && 0 < tess.lineIndexCount; 
    hasPoints = 0 < tess.pointVertexCount && 0 < tess.pointIndexCount;    
  }

  // Creates fill index data for a geometry shape.
  protected void addFillIndexData(int first, int offset, int size) {
    fillIndexData.clear(); 
    IndexData data = new IndexData(first, offset, size);
    fillIndexData.add(data);
  }    
  
  // Creates fill index data for a group shape.
  protected void addFillIndexData() {
    fillIndexData.clear(); 
    IndexData gdata = null;
    
    for (int i = 0; i < childCount; i++) {        
      PShape3D child = (PShape3D) children[i];
      
      for (int j = 0; j < child.fillIndexData.size(); j++) {
        IndexData cdata = child.fillIndexData.get(j);
          
        if (gdata == null) {
          gdata = new IndexData(cdata.first, cdata.offset, cdata.size);
          fillIndexData.add(gdata);
        } else {
          if (gdata.first == cdata.first) {
            gdata.size += cdata.size;  
          } else {
            gdata = new IndexData(cdata.first, cdata.offset, cdata.size);
            fillIndexData.add(gdata);
          }
        }
      }
      
    }    
  }
  
  // Creates line index data for a geometry shape.
  protected void addLineIndexData(int first, int offset, int size) {
    lineIndexData.clear(); 
    IndexData data = new IndexData(first, offset, size);
    lineIndexData.add(data);
  }    
  
  // Creates line index data for a group shape.
  protected void addLineIndexData() {
    lineIndexData.clear(); 
    IndexData gdata = null;
    
    for (int i = 0; i < childCount; i++) {        
      PShape3D child = (PShape3D) children[i];
      
      for (int j = 0; j < child.lineIndexData.size(); j++) {
        IndexData cdata = child.lineIndexData.get(j);
          
        if (gdata == null) {
          gdata = new IndexData(cdata.first, cdata.offset, cdata.size);
          lineIndexData.add(gdata);
        } else {
          if (gdata.first == cdata.first) {
            gdata.size += cdata.size;  
          } else {
            gdata = new IndexData(cdata.first, cdata.offset, cdata.size);
            lineIndexData.add(gdata);
          }
        }
      }
      
    }    
  }  

  // Creates point index data for a geometry shape.
  protected void addPointIndexData(int first, int offset, int size) {
    pointIndexData.clear(); 
    IndexData data = new IndexData(first, offset, size);
    pointIndexData.add(data);
  }    
  
  // Creates point index data for a group shape.
  protected void addPointIndexData() {
    pointIndexData.clear(); 
    IndexData gdata = null;
    
    for (int i = 0; i < childCount; i++) {        
      PShape3D child = (PShape3D) children[i];
      
      for (int j = 0; j < child.pointIndexData.size(); j++) {
        IndexData cdata = child.pointIndexData.get(j);
          
        if (gdata == null) {
          gdata = new IndexData(cdata.first, cdata.offset, cdata.size);
          pointIndexData.add(gdata);
        } else {
          if (gdata.first == cdata.first) {
            gdata.size += cdata.size;  
          } else {
            gdata = new IndexData(cdata.first, cdata.offset, cdata.size);
            pointIndexData.add(gdata);
          }
        }
      }
      
    }    
  }
  
  protected void initFillBuffers(int nvert, int nind) {
    int sizef = nvert * PGL.SIZEOF_FLOAT;
    int sizei = nvert * PGL.SIZEOF_INT;
    int sizex = nind * PGL.SIZEOF_INDEX;
    
    glFillVertexBufferID = pg.createVertexBufferObject();  
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glFillVertexBufferID);
    pgl.glBufferData(PGL.GL_ARRAY_BUFFER, 3 * sizef, null, glMode);
    
    glFillColorBufferID = pg.createVertexBufferObject();
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glFillColorBufferID);
    pgl.glBufferData(PGL.GL_ARRAY_BUFFER, sizei, null, glMode);    
    
    glFillNormalBufferID = pg.createVertexBufferObject();
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glFillNormalBufferID);
    pgl.glBufferData(PGL.GL_ARRAY_BUFFER, 3 * sizef, null, glMode);     
    
    glFillTexCoordBufferID = pg.createVertexBufferObject();
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glFillTexCoordBufferID);
    pgl.glBufferData(PGL.GL_ARRAY_BUFFER, 2 * sizef, null, glMode);  
    
    glFillAmbientBufferID = pg.createVertexBufferObject();  
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glFillAmbientBufferID);
    pgl.glBufferData(PGL.GL_ARRAY_BUFFER, sizei, null, glMode);
    
    glFillSpecularBufferID = pg.createVertexBufferObject();
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glFillSpecularBufferID);
    pgl.glBufferData(PGL.GL_ARRAY_BUFFER, sizei, null, glMode);    
    
    glFillEmissiveBufferID = pg.createVertexBufferObject();
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glFillEmissiveBufferID);
    pgl.glBufferData(PGL.GL_ARRAY_BUFFER, sizei, null, glMode);
    
    glFillShininessBufferID = pg.createVertexBufferObject();
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glFillShininessBufferID);
    pgl.glBufferData(PGL.GL_ARRAY_BUFFER, sizef, null, glMode);
        
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, 0);
        
    glFillIndexBufferID = pg.createVertexBufferObject();  
    pgl.glBindBuffer(PGL.GL_ELEMENT_ARRAY_BUFFER, glFillIndexBufferID);
    pgl.glBufferData(PGL.GL_ELEMENT_ARRAY_BUFFER, sizex, null, glMode);
    
    pgl.glBindBuffer(PGL.GL_ELEMENT_ARRAY_BUFFER, 0);  
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
                              tess.fillVertices, tess.fillColors, tess.fillNormals, tess.fillTexcoords,
                              tess.fillAmbient, tess.fillSpecular, tess.fillEmissive, tess.fillShininess);
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
            root.fillVerticesCache = new VertexCache(3, true);
          }            
          
          root.fillVerticesCache.add(root.fillVertCopyOffset, tess.fillVertexCount, tess.fillVertices);
          modifiedFillVertices = false;
        } else if (root.fillVerticesCache != null && root.fillVerticesCache.hasData()) {
          root.copyFillVertices(root.fillVerticesCache.offset, root.fillVerticesCache.size, root.fillVerticesCache.floatData);
          root.fillVerticesCache.reset();
        }
        
        if (modifiedFillColors) {
          if (root.fillColorsCache == null) { 
            root.fillColorsCache = new VertexCache(1, false);
          }            
          root.fillColorsCache.add(root.fillVertCopyOffset, tess.fillVertexCount, tess.fillColors);
          modifiedFillColors = false;            
        } else if (root.fillColorsCache != null && root.fillColorsCache.hasData()) {
          root.copyFillColors(root.fillColorsCache.offset, root.fillColorsCache.size, root.fillColorsCache.intData);
          root.fillColorsCache.reset();
        }
        
        if (modifiedFillNormals) {
          if (root.fillNormalsCache == null) { 
            root.fillNormalsCache = new VertexCache(3, true);
          }            
          root.fillNormalsCache.add(root.fillVertCopyOffset, tess.fillVertexCount, tess.fillNormals);            
          modifiedFillNormals = false;            
        } else if (root.fillNormalsCache != null && root.fillNormalsCache.hasData()) {
          root.copyFillNormals(root.fillNormalsCache.offset, root.fillNormalsCache.size, root.fillNormalsCache.floatData);
          root.fillNormalsCache.reset();
        }
        
        if (modifiedFillTexCoords) {
          if (root.fillTexCoordsCache == null) { 
            root.fillTexCoordsCache = new VertexCache(2, true);
          }            
          root.fillTexCoordsCache.add(root.fillVertCopyOffset, tess.fillVertexCount, tess.fillTexcoords);            
          modifiedFillTexCoords = false;
        } else if (root.fillTexCoordsCache != null && root.fillTexCoordsCache.hasData()) {
          root.copyFillTexCoords(root.fillTexCoordsCache.offset, root.fillTexCoordsCache.size, root.fillTexCoordsCache.floatData);
          root.fillTexCoordsCache.reset();
        }
        
        if (modifiedFillAmbient) {
          if (root.fillAmbientCache == null) { 
            root.fillAmbientCache = new VertexCache(1, false);
          }            
          root.fillAmbientCache.add(root.fillVertCopyOffset, tess.fillVertexCount, tess.fillAmbient);            
          modifiedFillAmbient = false;
        } else if (root.fillAmbientCache != null && root.fillAmbientCache.hasData()) {
          root.copyfillAmbient(root.fillAmbientCache.offset, root.fillAmbientCache.size, root.fillAmbientCache.intData);
          root.fillAmbientCache.reset();
        }

        if (modifiedFillSpecular) {
          if (root.fillSpecularCache == null) { 
            root.fillSpecularCache = new VertexCache(1, false);
          }            
          root.fillSpecularCache.add(root.fillVertCopyOffset, tess.fillVertexCount, tess.fillSpecular);            
          modifiedFillSpecular = false;
        } else if (root.fillSpecularCache != null && root.fillSpecularCache.hasData()) {
          root.copyfillSpecular(root.fillSpecularCache.offset, root.fillSpecularCache.size, root.fillSpecularCache.intData);
          root.fillSpecularCache.reset();
        }        
        
        if (modifiedFillEmissive) {
          if (root.fillEmissiveCache == null) { 
            root.fillEmissiveCache = new VertexCache(1, false);
          }            
          root.fillEmissiveCache.add(root.fillVertCopyOffset, tess.fillVertexCount, tess.fillEmissive);            
          modifiedFillEmissive = false;
        } else if (root.fillEmissiveCache != null && root.fillEmissiveCache.hasData()) {
          root.copyfillEmissive(root.fillEmissiveCache.offset, root.fillEmissiveCache.size, root.fillEmissiveCache.intData);
          root.fillEmissiveCache.reset();
        }          
        
        if (modifiedFillShininess) {
          if (root.fillShininessCache == null) { 
            root.fillShininessCache = new VertexCache(1, true);
          }            
          root.fillShininessCache.add(root.fillVertCopyOffset, tess.fillVertexCount, tess.fillShininess);            
          modifiedFillShininess = false;
        } else if (root.fillShininessCache != null && root.fillShininessCache.hasData()) {
          root.copyfillShininess(root.fillShininessCache.offset, root.fillShininessCache.size, root.fillShininessCache.floatData);
          root.fillShininessCache.reset();
        }          
      } 
      
      if (0 < tess.lineVertexCount) {
        if (modifiedLineVertices) {
          if (root.lineVerticesCache == null) { 
            root.lineVerticesCache = new VertexCache(3, true);
          }            
          root.lineVerticesCache.add(root.lineVertCopyOffset, tess.lineVertexCount, tess.lineVertices);
          modifiedLineVertices = false;
        } else if (root.lineVerticesCache != null && root.lineVerticesCache.hasData()) {
          root.copyLineVertices(root.lineVerticesCache.offset, root.lineVerticesCache.size, root.lineVerticesCache.floatData);
          root.lineVerticesCache.reset();
        }
        
        if (modifiedLineColors) {
          if (root.lineColorsCache == null) { 
            root.lineColorsCache = new VertexCache(1, false);
          }            
          root.lineColorsCache.add(root.lineVertCopyOffset, tess.lineVertexCount, tess.lineColors);
          modifiedLineColors = false;            
        } else if (root.lineColorsCache != null && root.lineColorsCache.hasData()) {
          root.copyLineColors(root.lineColorsCache.offset, root.lineColorsCache.size, root.lineColorsCache.intData);
          root.lineColorsCache.reset();
        }
        
        if (modifiedLineAttributes) {
          if (root.lineAttributesCache == null) { 
            root.lineAttributesCache = new VertexCache(4, true);
          }            
          root.lineAttributesCache.add(root.lineVertCopyOffset, tess.lineVertexCount, tess.lineDirWidths);            
          modifiedLineAttributes = false;
        } else if (root.lineAttributesCache != null && root.lineAttributesCache.hasData()) {
          root.copyLineAttributes(root.lineAttributesCache.offset, root.lineAttributesCache.size, root.lineAttributesCache.floatData);
          root.lineAttributesCache.reset();
        }      
      }

      if (0 < tess.pointVertexCount) {
        if (modifiedPointVertices) {
          if (root.pointVerticesCache == null) { 
            root.pointVerticesCache = new VertexCache(3, true);
          }            
          root.pointVerticesCache.add(root.pointVertCopyOffset, tess.pointVertexCount, tess.pointVertices);
          modifiedPointVertices = false;
        } else if (root.pointVerticesCache != null && root.pointVerticesCache.hasData()) {
          root.copyPointVertices(root.pointVerticesCache.offset, root.pointVerticesCache.size, root.pointVerticesCache.floatData);
          root.pointVerticesCache.reset();
        }
        
        if (modifiedPointColors) {
          if (root.pointColorsCache == null) { 
            root.pointColorsCache = new VertexCache(1, false);
          }            
          root.pointColorsCache.add(root.pointVertCopyOffset, tess.pointVertexCount, tess.pointColors);
          modifiedPointColors = false;            
        } else if (root.pointColorsCache != null && root.pointColorsCache.hasData()) {
          root.copyPointColors(root.pointColorsCache.offset, root.pointColorsCache.size, root.pointColorsCache.intData);
          root.pointColorsCache.reset();
        }
        
        if (modifiedPointAttributes) {
          if (root.pointAttributesCache == null) { 
            root.pointAttributesCache = new VertexCache(2, true);
          }            
          root.pointAttributesCache.add(root.pointVertCopyOffset, tess.pointVertexCount, tess.pointSizes);            
          modifiedPointAttributes = false;
        } else if (root.pointAttributesCache != null && root.pointAttributesCache.hasData()) {
          root.copyPointAttributes(root.pointAttributesCache.offset, root.pointAttributesCache.size, root.pointAttributesCache.floatData);
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
                                  float[] vertices, int[] colors, 
                                  float[] normals, float[] texcoords,
                                  int[] ambient, int[] specular, int[] emissive, float[] shininess) {
    int offsetf = offset * PGL.SIZEOF_FLOAT;
    int offseti = offset * PGL.SIZEOF_INT;
    int sizef = size * PGL.SIZEOF_FLOAT;
    int sizei = size * PGL.SIZEOF_INT;
    
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glFillVertexBufferID);
    pgl.glBufferSubData(PGL.GL_ARRAY_BUFFER, 3 * offsetf, 3 * sizef, FloatBuffer.wrap(vertices, 0, 3 * size));
    
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glFillColorBufferID);
    pgl.glBufferSubData(PGL.GL_ARRAY_BUFFER, offseti, sizei, IntBuffer.wrap(colors, 0, size));
    
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glFillNormalBufferID);
    pgl.glBufferSubData(PGL.GL_ARRAY_BUFFER, 3 * offsetf, 3 * sizef, FloatBuffer.wrap(normals, 0, 3 * size));
    
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glFillTexCoordBufferID);
    pgl.glBufferSubData(PGL.GL_ARRAY_BUFFER, 2 * offsetf, 2 * sizef, FloatBuffer.wrap(texcoords, 0, 2 * size));
    
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glFillAmbientBufferID);
    pgl.glBufferSubData(PGL.GL_ARRAY_BUFFER, offseti, sizei, IntBuffer.wrap(ambient, 0, size));
    
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glFillSpecularBufferID);
    pgl.glBufferSubData(PGL.GL_ARRAY_BUFFER, offseti, sizei, IntBuffer.wrap(specular, 0, size));    
    
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glFillEmissiveBufferID);
    pgl.glBufferSubData(PGL.GL_ARRAY_BUFFER, offseti, sizei, IntBuffer.wrap(emissive, 0, size));   
    
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glFillShininessBufferID);
    pgl.glBufferSubData(PGL.GL_ARRAY_BUFFER, offsetf, sizef, FloatBuffer.wrap(shininess, 0, size));
        
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, 0);    
  }

  
  protected void copyFillVertices(int offset, int size, float[] vertices) {    
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glFillVertexBufferID);
    pgl.glBufferSubData(PGL.GL_ARRAY_BUFFER, 3 * offset * PGL.SIZEOF_FLOAT, 3 * size * PGL.SIZEOF_FLOAT, FloatBuffer.wrap(vertices, 0, 3 * size));
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, 0);
  }
  
  
  protected void copyFillColors(int offset, int size, int[] colors) {    
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glFillColorBufferID);
    pgl.glBufferSubData(PGL.GL_ARRAY_BUFFER, offset * PGL.SIZEOF_INT, size * PGL.SIZEOF_INT, IntBuffer.wrap(colors, 0, size));     
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, 0);
  }  
  
  
  protected void copyFillNormals(int offset, int size, float[] normals) {
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glFillNormalBufferID);
    pgl.glBufferSubData(PGL.GL_ARRAY_BUFFER, 3 * offset * PGL.SIZEOF_FLOAT, 3 * size * PGL.SIZEOF_FLOAT, FloatBuffer.wrap(normals, 0, 3 * size));    
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, 0);
  }  

  
  protected void copyFillTexCoords(int offset, int size, float[] texcoords) {
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glFillTexCoordBufferID);
    pgl.glBufferSubData(PGL.GL_ARRAY_BUFFER, 2 * offset * PGL.SIZEOF_FLOAT, 2 * size * PGL.SIZEOF_FLOAT, FloatBuffer.wrap(texcoords, 0, 2 * size));
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, 0);
  }   

  
  protected void copyfillAmbient(int offset, int size, int[] ambient) {
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glFillAmbientBufferID);
    pgl.glBufferSubData(PGL.GL_ARRAY_BUFFER, offset * PGL.SIZEOF_INT, size * PGL.SIZEOF_INT, IntBuffer.wrap(ambient, 0, size));
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, 0);    
  }
  
  
  protected void copyfillSpecular(int offset, int size, int[] specular) {
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glFillSpecularBufferID);
    pgl.glBufferSubData(PGL.GL_ARRAY_BUFFER, offset * PGL.SIZEOF_INT, size * PGL.SIZEOF_INT, IntBuffer.wrap(specular, 0, size));     
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, 0);       
  }

  
  protected void copyfillEmissive(int offset, int size, int[] emissive) {
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glFillEmissiveBufferID);
    pgl.glBufferSubData(PGL.GL_ARRAY_BUFFER, offset * PGL.SIZEOF_INT, size * PGL.SIZEOF_INT, IntBuffer.wrap(emissive, 0, size));      
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, 0);    
  }  

  
  protected void copyfillShininess(int offset, int size, float[] shininess) {
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glFillShininessBufferID);
    pgl.glBufferSubData(PGL.GL_ARRAY_BUFFER, offset * PGL.SIZEOF_FLOAT, size * PGL.SIZEOF_FLOAT, FloatBuffer.wrap(shininess, 0, size));
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, 0);     
  }    
  
  
  protected void copyFillIndices(int offset, int size, short[] indices) {
    pgl.glBindBuffer(PGL.GL_ELEMENT_ARRAY_BUFFER, glFillIndexBufferID);
    pgl.glBufferSubData(PGL.GL_ELEMENT_ARRAY_BUFFER, offset * PGL.SIZEOF_INDEX, size * PGL.SIZEOF_INDEX, ShortBuffer.wrap(indices, 0, size));
    pgl.glBindBuffer(PGL.GL_ELEMENT_ARRAY_BUFFER, 0);
  }
  
  
  protected void initLineBuffers(int nvert, int nind) {
    int sizef = nvert * PGL.SIZEOF_FLOAT;
    int sizei = nvert * PGL.SIZEOF_INT;
    int sizex = nind * PGL.SIZEOF_INDEX;
    
    glLineVertexBufferID = pg.createVertexBufferObject();    
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glLineVertexBufferID);      
    pgl.glBufferData(PGL.GL_ARRAY_BUFFER, 3 * sizef, null, glMode);
    
    glLineColorBufferID = pg.createVertexBufferObject();
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glLineColorBufferID);
    pgl.glBufferData(PGL.GL_ARRAY_BUFFER, sizei, null, glMode);       

    glLineDirWidthBufferID = pg.createVertexBufferObject();
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glLineDirWidthBufferID);
    pgl.glBufferData(PGL.GL_ARRAY_BUFFER, 4 * sizef, null, glMode);    
    
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, 0);    
    
    glLineIndexBufferID = pg.createVertexBufferObject();    
    pgl.glBindBuffer(PGL.GL_ELEMENT_ARRAY_BUFFER, glLineIndexBufferID);
    pgl.glBufferData(PGL.GL_ELEMENT_ARRAY_BUFFER, sizex, null, glMode);

    pgl.glBindBuffer(PGL.GL_ELEMENT_ARRAY_BUFFER, 0);
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
                              tess.lineVertices, tess.lineColors, tess.lineDirWidths);        
        root.lineVertCopyOffset += tess.lineVertexCount;
        
        root.copyLineIndices(root.lineIndCopyOffset, tess.lineIndexCount, tess.lineIndices);
        root.lineIndCopyOffset += tess.lineIndexCount;        
      }
    }    
  }

  
  protected void copyLineGeometry(int offset, int size, 
                                  float[] vertices, int[] colors, float[] attribs) {
    int offsetf = offset * PGL.SIZEOF_FLOAT;
    int sizef = size * PGL.SIZEOF_FLOAT;
    int offseti = offset * PGL.SIZEOF_INT;
    int sizei = size * PGL.SIZEOF_INT;
    
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glLineVertexBufferID);
    pgl.glBufferSubData(PGL.GL_ARRAY_BUFFER, 3 * offsetf, 3 * sizef, FloatBuffer.wrap(vertices, 0, 3 * size));

    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glLineColorBufferID);
    pgl.glBufferSubData(PGL.GL_ARRAY_BUFFER, offseti, sizei, IntBuffer.wrap(colors, 0, size));
    
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glLineDirWidthBufferID);
    pgl.glBufferSubData(PGL.GL_ARRAY_BUFFER, 4 * offsetf, 4 * sizef, FloatBuffer.wrap(attribs, 0, 4 * size));
    
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, 0);
  }    
  
  
  protected void copyLineVertices(int offset, int size, float[] vertices) {    
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glLineVertexBufferID);
    pgl.glBufferSubData(PGL.GL_ARRAY_BUFFER, 3 * offset * PGL.SIZEOF_FLOAT, 3 * size * PGL.SIZEOF_FLOAT, FloatBuffer.wrap(vertices, 0, 3 * size));
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, 0);
  }     
  
  
  protected void copyLineColors(int offset, int size, int[] colors) {
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glLineColorBufferID);
    pgl.glBufferSubData(PGL.GL_ARRAY_BUFFER, offset * PGL.SIZEOF_INT, size * PGL.SIZEOF_INT, IntBuffer.wrap(colors, 0, size));             
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, 0);
  }
  
  
  protected void copyLineAttributes(int offset, int size, float[] attribs) {
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glLineDirWidthBufferID);
    pgl.glBufferSubData(PGL.GL_ARRAY_BUFFER, 4 * offset * PGL.SIZEOF_FLOAT, 4 * size * PGL.SIZEOF_FLOAT, FloatBuffer.wrap(attribs, 0, 4 * size));
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, 0);
  }
  
  
  protected void copyLineIndices(int offset, int size, short[] indices) {
    pgl.glBindBuffer(PGL.GL_ELEMENT_ARRAY_BUFFER, glLineIndexBufferID);
    pgl.glBufferSubData(PGL.GL_ELEMENT_ARRAY_BUFFER, offset * PGL.SIZEOF_INDEX, size * PGL.SIZEOF_INDEX, ShortBuffer.wrap(indices, 0, size));
    pgl.glBindBuffer(PGL.GL_ELEMENT_ARRAY_BUFFER, 0);
  }  
  

  protected void initPointBuffers(int nvert, int nind) {
    int sizef = nvert * PGL.SIZEOF_FLOAT;
    int sizei = nvert * PGL.SIZEOF_INT;
    int sizex = nind * PGL.SIZEOF_INDEX;
    
    glPointVertexBufferID = pg.createVertexBufferObject();
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glPointVertexBufferID);
    pgl.glBufferData(PGL.GL_ARRAY_BUFFER, 3 * sizef, null, glMode);   

    glPointColorBufferID = pg.createVertexBufferObject();
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glPointColorBufferID);
    pgl.glBufferData(PGL.GL_ARRAY_BUFFER, sizei, null, glMode);     
    
    glPointSizeBufferID = pg.createVertexBufferObject();
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glPointSizeBufferID);
    pgl.glBufferData(PGL.GL_ARRAY_BUFFER, 2 * sizef, null, glMode);
      
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, 0);     
        
    glPointIndexBufferID = pg.createVertexBufferObject();
    pgl.glBindBuffer(PGL.GL_ELEMENT_ARRAY_BUFFER, glPointIndexBufferID);
    pgl.glBufferData(PGL.GL_ELEMENT_ARRAY_BUFFER, sizex, null, glMode);
    
    pgl.glBindBuffer(PGL.GL_ELEMENT_ARRAY_BUFFER, 0);
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
                               tess.pointVertices, tess.pointColors, tess.pointSizes);        
        root.pointVertCopyOffset += tess.pointVertexCount;
        
        root.copyPointIndices(root.pointIndCopyOffset, tess.pointIndexCount, tess.pointIndices);
        root.pointIndCopyOffset += tess.pointIndexCount;        
      }
    }
  }
  
  
  protected void copyPointGeometry(int offset, int size, 
                                   float[] vertices, int[] colors, float[] attribs) {
    int offsetf = offset * PGL.SIZEOF_FLOAT;
    int sizef = size * PGL.SIZEOF_FLOAT;
    int offseti = offset * PGL.SIZEOF_INT;
    int sizei = size * PGL.SIZEOF_INT;    

    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glPointVertexBufferID);
    pgl.glBufferSubData(PGL.GL_ARRAY_BUFFER, 3 * offsetf, 3 * sizef, FloatBuffer.wrap(vertices, 0, 3 * size));

    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glPointColorBufferID);
    pgl.glBufferSubData(PGL.GL_ARRAY_BUFFER, offseti, sizei, IntBuffer.wrap(colors, 0, size));
    
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glPointSizeBufferID);
    pgl.glBufferSubData(PGL.GL_ARRAY_BUFFER, 2 * offsetf, 2 * sizef, FloatBuffer.wrap(attribs, 0, 2 * size));
    
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, 0);    
  }  


  protected void copyPointVertices(int offset, int size, float[] vertices) {    
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glPointVertexBufferID);
    pgl.glBufferSubData(PGL.GL_ARRAY_BUFFER, 3 * offset * PGL.SIZEOF_FLOAT, 3 * size * PGL.SIZEOF_FLOAT, FloatBuffer.wrap(vertices, 0, 3 * size));
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, 0);
  }
    
    
  protected void copyPointColors(int offset, int size, int[] colors) {
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glPointColorBufferID);
    pgl.glBufferSubData(PGL.GL_ARRAY_BUFFER, offset * PGL.SIZEOF_INT, size * PGL.SIZEOF_INT, IntBuffer.wrap(colors, 0, size));
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, 0);
  }
    
  
  protected void copyPointAttributes(int offset, int size, float[] attribs) {
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glPointSizeBufferID);
    pgl.glBufferSubData(PGL.GL_ARRAY_BUFFER, 2 * offset * PGL.SIZEOF_FLOAT, 2 * size * PGL.SIZEOF_FLOAT, FloatBuffer.wrap(attribs, 0, 2 * size));
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, 0);
  }
  
  
  protected void copyPointIndices(int offset, int size, short[] indices) {
    pgl.glBindBuffer(PGL.GL_ELEMENT_ARRAY_BUFFER, glPointIndexBufferID);
    pgl.glBufferSubData(PGL.GL_ELEMENT_ARRAY_BUFFER, offset * PGL.SIZEOF_INDEX, size * PGL.SIZEOF_INDEX, ShortBuffer.wrap(indices, 0, size));
    pgl.glBindBuffer(PGL.GL_ELEMENT_ARRAY_BUFFER, 0);    
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
      pg.deleteVertexBufferObject(glFillVertexBufferID);   
      glFillVertexBufferID = 0;
    }    
    
    if (glFillColorBufferID != 0) {    
      pg.deleteVertexBufferObject(glFillColorBufferID);   
      glFillColorBufferID = 0;
    }    

    if (glFillNormalBufferID != 0) {    
      pg.deleteVertexBufferObject(glFillNormalBufferID);   
      glFillNormalBufferID = 0;
    }     

    if (glFillTexCoordBufferID != 0) {    
      pg.deleteVertexBufferObject(glFillTexCoordBufferID);   
      glFillTexCoordBufferID = 0;
    }    

    if (glFillAmbientBufferID != 0) {    
      pg.deleteVertexBufferObject(glFillAmbientBufferID);   
      glFillAmbientBufferID = 0;
    }    
    
    if (glFillSpecularBufferID != 0) {    
      pg.deleteVertexBufferObject(glFillSpecularBufferID);   
      glFillSpecularBufferID = 0;
    }    

    if (glFillEmissiveBufferID != 0) {    
      pg.deleteVertexBufferObject(glFillEmissiveBufferID);   
      glFillEmissiveBufferID = 0;
    }     

    if (glFillShininessBufferID != 0) {    
      pg.deleteVertexBufferObject(glFillShininessBufferID);   
      glFillShininessBufferID = 0;
    }        
    
    if (glFillIndexBufferID != 0) {    
      pg.deleteVertexBufferObject(glFillIndexBufferID);   
      glFillIndexBufferID = 0;
    }   
  }
  
  
  protected void deleteLineBuffers() {
    if (glLineVertexBufferID != 0) {    
      pg.deleteVertexBufferObject(glLineVertexBufferID);   
      glLineVertexBufferID = 0;
    }    
    
    if (glLineColorBufferID != 0) {    
      pg.deleteVertexBufferObject(glLineColorBufferID);   
      glLineColorBufferID = 0;
    }    

    if (glLineDirWidthBufferID != 0) {    
      pg.deleteVertexBufferObject(glLineDirWidthBufferID);   
      glLineDirWidthBufferID = 0;
    }    
    
    if (glLineIndexBufferID != 0) {    
      pg.deleteVertexBufferObject(glLineIndexBufferID);   
      glLineIndexBufferID = 0;
    }  
  }  
  
  
  protected void deletePointBuffers() {
    if (glPointVertexBufferID != 0) {    
      pg.deleteVertexBufferObject(glPointVertexBufferID);   
      glPointVertexBufferID = 0;
    }    
    
    if (glPointColorBufferID != 0) {    
      pg.deleteVertexBufferObject(glPointColorBufferID);   
      glPointColorBufferID = 0;
    }    

    if (glPointSizeBufferID != 0) {    
      pg.deleteVertexBufferObject(glPointSizeBufferID);   
      glPointSizeBufferID = 0;
    }    
    
    if (glPointIndexBufferID != 0) {    
      pg.deleteVertexBufferObject(glPointIndexBufferID);   
      glPointIndexBufferID = 0;
    }  
  }
  
  ///////////////////////////////////////////////////////////  
  
  //
  
  // Rendering methods
  
  
  public void draw() {
    draw(pg);
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
    PointShader shader = pg.getPointShader();
    shader.start(); 
    
    for (int i = 0; i < pointIndexData.size(); i++) {
      IndexData index = (IndexData)pointIndexData.get(i);      
      int first = index.first;
      int offset = index.offset;
      int size =  index.size;
      
      shader.setVertexAttribute(root.glPointVertexBufferID, 3, PGL.GL_FLOAT, 0, 3 * first * PGL.SIZEOF_FLOAT);        
      shader.setColorAttribute(root.glPointColorBufferID, 4, PGL.GL_UNSIGNED_BYTE, 0, 4 * first);    
      shader.setSizeAttribute(root.glPointSizeBufferID, 2, PGL.GL_FLOAT, 0, 2 * first * PGL.SIZEOF_FLOAT);      
      
      pgl.glBindBuffer(PGL.GL_ELEMENT_ARRAY_BUFFER, root.glPointIndexBufferID);      
      pgl.glDrawElements(PGL.GL_TRIANGLES, size, PGL.INDEX_TYPE, offset * PGL.SIZEOF_INDEX);       
      pgl.glBindBuffer(PGL.GL_ELEMENT_ARRAY_BUFFER, 0);      
    }
    
    shader.stop();
  }  


  protected void renderLines() {
    LineShader shader = pg.getLineShader();
    shader.start(); 
    
    for (int i = 0; i < lineIndexData.size(); i++) {
      IndexData index = (IndexData)lineIndexData.get(i);      
      int first = index.first;
      int offset = index.offset;
      int size =  index.size;
    
      shader.setVertexAttribute(root.glLineVertexBufferID, 3, PGL.GL_FLOAT, 0, 3 * first * PGL.SIZEOF_FLOAT);        
      shader.setColorAttribute(root.glLineColorBufferID, 4, PGL.GL_UNSIGNED_BYTE, 0, 4 * first);    
      shader.setDirWidthAttribute(root.glLineDirWidthBufferID, 4, PGL.GL_FLOAT, 0, 4 * first * PGL.SIZEOF_FLOAT);
      
      pgl.glBindBuffer(PGL.GL_ELEMENT_ARRAY_BUFFER, root.glLineIndexBufferID);
      pgl.glDrawElements(PGL.GL_TRIANGLES, size, PGL.INDEX_TYPE, offset * PGL.SIZEOF_INDEX);      
      pgl.glBindBuffer(PGL.GL_ELEMENT_ARRAY_BUFFER, 0);      
    }
    
    shader.stop();
  }  

  
  protected void renderFill(PImage textureImage) {
    PTexture tex = null;
    if (textureImage != null) {
      tex = pg.getTexture(textureImage);
      if (tex != null) {
        pgl.enableTexturing(tex.glTarget);          
        pgl.glBindTexture(tex.glTarget, tex.glID);        
      }
    }    
    
    FillShader shader = pg.getFillShader(pg.lights, tex != null);
    shader.start();
    
    for (int i = 0; i < fillIndexData.size(); i++) {
      IndexData index = (IndexData)fillIndexData.get(i);      
      int first = index.first;
      int offset = index.offset;
      int size =  index.size;
      
      shader.setVertexAttribute(root.glFillVertexBufferID, 3, PGL.GL_FLOAT, 0, 3 * first * PGL.SIZEOF_FLOAT);        
      shader.setColorAttribute(root.glFillColorBufferID, 4, PGL.GL_UNSIGNED_BYTE, 0, 4 * first);    
      
      if (pg.lights) {
        shader.setNormalAttribute(root.glFillNormalBufferID, 3, PGL.GL_FLOAT, 0, 3 * first * PGL.SIZEOF_FLOAT);
        shader.setAmbientAttribute(root.glFillAmbientBufferID, 4, PGL.GL_UNSIGNED_BYTE, 0, 4 * first);
        shader.setSpecularAttribute(root.glFillSpecularBufferID, 4, PGL.GL_UNSIGNED_BYTE, 0, 4 * first);
        shader.setEmissiveAttribute(root.glFillEmissiveBufferID, 4, PGL.GL_UNSIGNED_BYTE, 0, 4 * first);      
        shader.setShininessAttribute(root.glFillShininessBufferID, 1, PGL.GL_FLOAT, 0, first * PGL.SIZEOF_FLOAT);
      }
      
      if (tex != null) {        
        shader.setTexCoordAttribute(root.glFillTexCoordBufferID, 2, PGL.GL_FLOAT, 0, 2 * first * PGL.SIZEOF_FLOAT);
        shader.setTexture(tex);
      }      
      
      pgl.glBindBuffer(PGL.GL_ELEMENT_ARRAY_BUFFER, root.glFillIndexBufferID);
      pgl.glDrawElements(PGL.GL_TRIANGLES, size, PGL.INDEX_TYPE, offset * PGL.SIZEOF_INDEX);
      pgl.glBindBuffer(PGL.GL_ELEMENT_ARRAY_BUFFER, 0);
    }
    
    shader.stop();
    
    if (tex != null) {
      pgl.glBindTexture(tex.glTarget, 0); 
      pgl.disableTexturing(tex.glTarget);
    }    
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
    boolean isFloat;
    int ncoords;
    int offset;
    int size;    
    float[] floatData;
    int[] intData;
    
    VertexCache(int ncoords, boolean isFloat) {
      this.ncoords = ncoords;
      this.isFloat = isFloat;
      if (isFloat) {
        this.floatData = new float[ncoords * PGL.DEFAULT_VERTEX_CACHE_SIZE];        
      } else {
        this.intData = new int[ncoords * PGL.DEFAULT_VERTEX_CACHE_SIZE];
      }
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
      
      int oldSize = floatData.length / ncoords;
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
            floatData[destIndex++] = newData[srcIndex++];
            floatData[destIndex  ] = newData[srcIndex  ];
          } else if (ncoords == 3) {
            floatData[destIndex++] = newData[srcIndex++];
            floatData[destIndex++] = newData[srcIndex++];
            floatData[destIndex  ] = newData[srcIndex  ];
          } else if (ncoords == 4) {
            floatData[destIndex++] = newData[srcIndex++];
            floatData[destIndex++] = newData[srcIndex++];
            floatData[destIndex++] = newData[srcIndex++];
            floatData[destIndex  ] = newData[srcIndex  ];            
          } else {
            for (int j = 0; j < ncoords; j++) {
              floatData[destIndex++] = newData[srcIndex++];
            }            
          }
        }
      } else {
        PApplet.arrayCopy(newData, 0, floatData, ncoords * size, ncoords * dataSize);
      }
      
      size += dataSize;
    } 
    
    void add(int dataOffset, int dataSize, int[] newData) {
      if (size == 0) {
        offset = dataOffset;
      }
      
      int oldSize = intData.length / ncoords;
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
            intData[destIndex++] = newData[srcIndex++];
            intData[destIndex  ] = newData[srcIndex  ];
          } else if (ncoords == 3) {
            intData[destIndex++] = newData[srcIndex++];
            intData[destIndex++] = newData[srcIndex++];
            intData[destIndex  ] = newData[srcIndex  ];
          } else if (ncoords == 4) {
            intData[destIndex++] = newData[srcIndex++];
            intData[destIndex++] = newData[srcIndex++];
            intData[destIndex++] = newData[srcIndex++];
            intData[destIndex  ] = newData[srcIndex  ];            
          } else {
            for (int j = 0; j < ncoords; j++) {
              intData[destIndex++] = newData[srcIndex++];
            }            
          }
        }
      } else {
        PApplet.arrayCopy(newData, 0, intData, ncoords * size, ncoords * dataSize);
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
      
      int oldSize = floatData.length / ncoords;
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
          floatData[destIndex++] = x * tr.m00 + y * tr.m01 + tr.m02;
          floatData[destIndex  ] = x * tr.m10 + y * tr.m11 + tr.m12;
        }        
      }
      
      size += dataSize;
    }
    
    void add(int dataOffset, int dataSize, float[] newData, PMatrix3D tr) {
      if (size == 0) {
        offset = dataOffset;
      }
      
      int oldSize = floatData.length / ncoords;
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
          floatData[destIndex++] = x * tr.m00 + y * tr.m01 + z * tr.m02 + tr.m03;
          floatData[destIndex++] = x * tr.m10 + y * tr.m11 + z * tr.m12 + tr.m13;
          floatData[destIndex  ] = x * tr.m20 + y * tr.m21 + z * tr.m22 + tr.m23;
        }          
      }      
      
      size += dataSize;
    }
    
    void expand(int n) {
      if (isFloat) {
        expandFloat(n);
      } else {
        expandInt(n);
      }
    }

    void expandFloat(int n) {
      float temp[] = new float[ncoords * n];      
      PApplet.arrayCopy(floatData, 0, temp, 0, ncoords * size);
      floatData = temp;      
    }
    
    void expandInt(int n) {
      int temp[] = new int[ncoords * n];      
      PApplet.arrayCopy(intData, 0, temp, 0, ncoords * size);
      intData = temp;      
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
    
    //pg.saveDrawingState();
    
    // The recorded shapes are not merged, they are grouped
    // according to the group names found in the OBJ file.    
    //ogl.mergeRecShapes = false;
    
    // Using RGB mode for coloring.
    pg.colorMode = RGB;
    
    // Strokes are not used to draw the model.
    pg.stroke = false;    
    
    // Normals are automatically computed if not specified in the OBJ file.
    //renderer.autoNormal(true);
    
    // Using normal mode for texture coordinates (i.e.: normalized between 0 and 1).
    pg.textureMode = NORMAL;    
    
    //ogl.beginShapeRecorderImpl();    
    for (int i = 0; i < faces.size(); i++) {
      OBJFace face = faces.get(i);
      
      // Getting current material.
      if (mtlIdxCur != face.matIdx) {
        mtlIdxCur = PApplet.max(0, face.matIdx); // To make sure that at least we get the default material.
        
        mtl = materials.get(mtlIdxCur);

        // Setting colors.
        pg.specular(mtl.ks.x * 255.0f, mtl.ks.y * 255.0f, mtl.ks.z * 255.0f);
        pg.ambient(mtl.ka.x * 255.0f, mtl.ka.y * 255.0f, mtl.ka.z * 255.0f);
        if (pg.fill) {
          pg.fill(mtl.kd.x * 255.0f, mtl.kd.y * 255.0f, mtl.kd.z * 255.0f, mtl.d * 255.0f);  
        }        
        pg.shininess(mtl.ns);
        
        if (pg.tint && mtl.kdMap != null) {
          // If current material is textured, then tinting the texture using the diffuse color.
          pg.tint(mtl.kd.x * 255.0f, mtl.kd.y * 255.0f, mtl.kd.z * 255.0f, mtl.d * 255.0f);
        }
      }

      // Recording current face.
      if (face.vertIdx.size() == 3) {
        pg.beginShape(TRIANGLES); // Face is a triangle, so using appropriate shape kind.
      } else if (face.vertIdx.size() == 4) {
        pg.beginShape(QUADS);        // Face is a quad, so using appropriate shape kind.
      } else {
        pg.beginShape();  
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
          
          PTexture texMtl = (PTexture)mtl.kdMap.getCache(pg);
          if (texMtl != null) {     
            // Texture orientation in Processing is inverted.
            texMtl.setFlippedY(true);          
          }
          pg.texture(mtl.kdMap);
          if (norms != null) {
            pg.normal(norms.x, norms.y, norms.z);
          }
          if (tex != null) {
            pg.vertex(vert.x, vert.y, vert.z, tex.x, tex.y);  
          } else {
            pg.vertex(vert.x, vert.y, vert.z);
          }
        } else {
          // This face is not textured.
          if (norms != null) {
            pg.normal(norms.x, norms.y, norms.z);
          }
          pg.vertex(vert.x, vert.y, vert.z);          
        }
      } 
      pg.endShape(CLOSE);
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
    
    //pg.restoreDrawingState();    
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
