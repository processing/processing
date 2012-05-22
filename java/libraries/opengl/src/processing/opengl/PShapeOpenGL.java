/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2011-12 Ben Fry and Casey Reas

  This library is free software; you can redistribute it and/or
  modify it under the terms of the GNU Lesser General Public
  License as published by the Free Software Foundation, version 2.1.

  This library is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  Lesser General Public License for more details.

  You should have received a copy of the GNU Lesser General
  Public License along with this library; if not, write to the
  Free Software Foundation, Inc., 59 Temple Place, Suite 330,
  Boston, MA  02111-1307  USA
*/

package processing.opengl;

import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PGraphics;
import processing.core.PImage;
import processing.core.PMatrix;
import processing.core.PMatrix2D;
import processing.core.PMatrix3D;
import processing.core.PShape;
import processing.core.PVector;
import processing.opengl.PGraphicsOpenGL.LineShader;
import processing.opengl.PGraphicsOpenGL.PointShader;
import processing.opengl.PGraphicsOpenGL.PolyShader;
import processing.opengl.PGraphicsOpenGL.IndexCache;
import processing.opengl.PGraphicsOpenGL.InGeometry;
import processing.opengl.PGraphicsOpenGL.TessGeometry;
import processing.opengl.PGraphicsOpenGL.Tessellator;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;
import java.util.HashSet;

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
 * OFF: http://people.sc.fsu.edu/~jburkardt/data/off/off.html(file_format)
 * DXF: http://en.wikipedia.org/wiki/AutoCAD_DXF
 */
public class PShapeOpenGL extends PShape {
  static protected final int TRANSLATE = 0;
  static protected final int ROTATE    = 1;
  static protected final int SCALE     = 2;
  static protected final int MATRIX    = 3;
  
  protected PGraphicsOpenGL pg;
  protected PGL pgl;
  protected PGL.Context context;      // The context that created this shape.

  protected PShapeOpenGL root;  

  // ........................................................
  
  // Input, tessellated geometry    
  
  protected InGeometry inGeo;
  protected TessGeometry tessGeo;
  protected Tessellator tessellator;

  // ........................................................
  
  // Texturing  
  
  protected HashSet<PImage> textures;
  protected PImage texture; 
  protected boolean strokedTexture;
  
  // ........................................................
  
  // OpenGL buffers  
  
  public int glPolyVertexBufferID;
  public int glPolyColorBufferID;
  public int glPolyNormalBufferID;
  public int glPolyTexcoordBufferID;
  public int glPolyAmbientBufferID;
  public int glPolySpecularBufferID;
  public int glPolyEmissiveBufferID;
  public int glPolyShininessBufferID;
  public int glPolyIndexBufferID;
  
  public int glLineVertexBufferID;
  public int glLineColorBufferID;
  public int glLineAttribBufferID;
  public int glLineIndexBufferID;  
  
  public int glPointVertexBufferID;
  public int glPointColorBufferID;
  public int glPointAttribBufferID;
  public int glPointIndexBufferID;  

  // ........................................................
  
  // Offsets for geometry aggregation and update.
  
  protected int polyVertCopyOffset;
  protected int polyIndCopyOffset;
  protected int lineVertCopyOffset;
  protected int lineIndCopyOffset;
  protected int pointVertCopyOffset;
  protected int pointIndCopyOffset;

  protected int polyIndexOffset;
  protected int polyVertexOffset;
  protected int polyVertexAbs;
  protected int polyVertexRel;  
  
  protected int lineIndexOffset;
  protected int lineVertexOffset;
  protected int lineVertexAbs;
  protected int lineVertexRel;
  
  protected int pointIndexOffset;
  protected int pointVertexOffset;
  protected int pointVertexAbs;
  protected int pointVertexRel;
    
  protected int firstPolyIndexCache;
  protected int lastPolyIndexCache; 
  protected int firstLineIndexCache;
  protected int lastLineIndexCache;
  protected int firstPointIndexCache;
  protected int lastPointIndexCache;

  protected int firstPolyVertex; 
  protected int lastPolyVertex;
  protected int firstLineVertex; 
  protected int lastLineVertex;
  protected int firstPointVertex; 
  protected int lastPointVertex;  
  
  // ........................................................
  
  // Geometric transformations.
  
  protected PMatrix transform;      
  
  // ........................................................
  
  // State/rendering flags  
  
  protected boolean tessellated;
  protected boolean needBufferInit;
  protected boolean forceTessellation;
  
  protected boolean isSolid;
  protected boolean isClosed;
  
  protected boolean openContour = false;
  protected boolean breakShape = false;
  protected boolean shapeEnded = false;

  // These variables indicate if the shape contains
  // polygon, line and/or point geometry. In the case of
  // 3D shapes, poly geometry is coincident with the fill
  // triangles, as the lines and points are stored separately.
  // However, for 2D shapes the poly geometry contains all of
  // the three since the same rendering shader applies to 
  // fill, line and point geometry. 
  protected boolean hasPolys;
  protected boolean hasLines;
  protected boolean hasPoints;
  
  // ........................................................
  
  // Modes inherited from renderer  
  
  protected int textureMode;
  protected int rectMode;
  protected int ellipseMode;
  protected int shapeMode;
  protected int imageMode;

  // ........................................................
  
  // Bezier and Catmull-Rom curves
  
  protected int bezierDetail = 20;
  protected int curveDetail = 20;
  protected float curveTightness = 0;    
  
  // ........................................................
  
  // Normals
  
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
  
  // Modification variables (used only by the root shape)  
  
  protected boolean modified;
  
  protected boolean modifiedPolyVertices;
  protected boolean modifiedPolyColors;
  protected boolean modifiedPolyNormals;
  protected boolean modifiedPolyTexcoords;  
  protected boolean modifiedPolyAmbient;
  protected boolean modifiedPolySpecular;
  protected boolean modifiedPolyEmissive;
  protected boolean modifiedPolyShininess;
  
  protected boolean modifiedLineVertices;
  protected boolean modifiedLineColors;
  protected boolean modifiedLineAttributes;  

  protected boolean modifiedPointVertices;
  protected boolean modifiedPointColors;
  protected boolean modifiedPointAttributes;     
  
  protected int firstModifiedPolyVertex;
  protected int lastModifiedPolyVertex;
  protected int firstModifiedPolyColor;
  protected int lastModifiedPolyColor;
  protected int firstModifiedPolyNormal;
  protected int lastModifiedPolyNormal;  
  protected int firstModifiedPolyTexcoord;
  protected int lastModifiedPolyTexcoord;  
  protected int firstModifiedPolyAmbient;
  protected int lastModifiedPolyAmbient;  
  protected int firstModifiedPolySpecular;
  protected int lastModifiedPolySpecular;   
  protected int firstModifiedPolyEmissive;
  protected int lastModifiedPolyEmissive; 
  protected int firstModifiedPolyShininess;
  protected int lastModifiedPolyShininess;  
  
  protected int firstModifiedLineVertex;
  protected int lastModifiedLineVertex;  
  protected int firstModifiedLineColor;
  protected int lastModifiedLineColor;  
  protected int firstModifiedLineAttribute;
  protected int lastModifiedLineAttribute;  
  
  protected int firstModifiedPointVertex;
  protected int lastModifiedPointVertex; 
  protected int firstModifiedPointColor;
  protected int lastModifiedPointColor; 
  protected int firstModifiedPointAttribute;
  protected int lastModifiedPointAttribute;  
  
  PShapeOpenGL() {    
  }
  
  public PShapeOpenGL(PApplet parent, int family) {
    pg = (PGraphicsOpenGL)parent.g;
    pgl = pg.pgl;
    context = pgl.createEmptyContext();
    
    glPolyVertexBufferID = 0;
    glPolyColorBufferID = 0;
    glPolyNormalBufferID = 0;
    glPolyTexcoordBufferID = 0;
    glPolyAmbientBufferID = 0;
    glPolySpecularBufferID = 0;
    glPolyEmissiveBufferID = 0;
    glPolyShininessBufferID = 0;     
    glPolyIndexBufferID = 0;
    
    glLineVertexBufferID = 0;
    glLineColorBufferID = 0;
    glLineAttribBufferID = 0;
    glLineIndexBufferID = 0;
    
    glPointVertexBufferID = 0;
    glPointColorBufferID = 0;
    glPointAttribBufferID = 0;
    glPointIndexBufferID = 0;
    
    this.tessellator = PGraphicsOpenGL.tessellator;
    this.family = family;    
    this.root = this;
    this.parent = null;
    this.tessellated = false;
    
    if (family == GEOMETRY || family == PRIMITIVE || family == PATH) {
      inGeo = pg.newInGeometry(PGraphicsOpenGL.RETAINED);      
    }
    
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
    strokeCap = pg.strokeCap;
    strokeJoin = pg.strokeJoin;
    
    tint = pg.tint;  
    tintColor = pg.tintColor;

    ambientColor = pg.ambientColor;  
    specularColor = pg.specularColor;  
    emissiveColor = pg.emissiveColor;
    shininess = pg.shininess;
    
    normalX = normalY = 0; 
    normalZ = 1;
    
    normalMode = NORMAL_MODE_AUTO;
    
    if (family == GROUP) {
      // GROUP shapes are always marked as ended.
      shapeEnded = true;
    }
  }

  
  public void addChild(PShape child) {
    if (child instanceof PShapeOpenGL) {
      if (family == GROUP) {
        PShapeOpenGL c3d = (PShapeOpenGL)child;
        
        super.addChild(c3d);
        c3d.updateRoot(root);
        root.tessellated = false;
        tessellated = false;
        
        if (c3d.texture != null) {
          addTexture(c3d.texture);
        }        
      } else {
        PGraphics.showWarning("Cannot add child shape to non-group shape.");
      }
    } else {
      PGraphics.showWarning("Shape must be 3D to be added to the group.");
    }
  }
  
  
  public void updateRoot(PShape root) {
    this.root = (PShapeOpenGL) root;
    if (family == GROUP) {
      for (int i = 0; i < childCount; i++) {
        PShapeOpenGL child = (PShapeOpenGL)children[i];
        child.updateRoot(root);
      }
    }
  }      
  
  
  protected void finalize() throws Throwable {
    try {
      finalizePolyBuffers();  
      finalizeLineBuffers();
      finalizePointBuffers();
    } finally {
      super.finalize();
    }
  }
  
  
  protected void finalizePolyBuffers() {
    if (glPolyVertexBufferID != 0) {    
      pg.finalizeVertexBufferObject(glPolyVertexBufferID, context.code());   
    }    
    
    if (glPolyColorBufferID != 0) {    
      pg.finalizeVertexBufferObject(glPolyColorBufferID, context.code());   
    }    

    if (glPolyNormalBufferID != 0) {    
      pg.finalizeVertexBufferObject(glPolyNormalBufferID, context.code());   
    }     

    if (glPolyTexcoordBufferID != 0) {    
      pg.finalizeVertexBufferObject(glPolyTexcoordBufferID, context.code());   
    }    

    if (glPolyAmbientBufferID != 0) {    
      pg.finalizeVertexBufferObject(glPolyAmbientBufferID, context.code());   
    }    
    
    if (glPolySpecularBufferID != 0) {    
      pg.finalizeVertexBufferObject(glPolySpecularBufferID, context.code());   
    }    

    if (glPolyEmissiveBufferID != 0) {    
      pg.finalizeVertexBufferObject(glPolyEmissiveBufferID, context.code());   
    }     

    if (glPolyShininessBufferID != 0) {    
      pg.finalizeVertexBufferObject(glPolyShininessBufferID, context.code());   
    }    
    
    if (glPolyIndexBufferID != 0) {    
      pg.finalizeVertexBufferObject(glPolyIndexBufferID, context.code());   
    }   
  }
  
  
  protected void finalizeLineBuffers() {
    if (glLineVertexBufferID != 0) {    
      pg.finalizeVertexBufferObject(glLineVertexBufferID, context.code());   
    }    
    
    if (glLineColorBufferID != 0) {    
      pg.finalizeVertexBufferObject(glLineColorBufferID, context.code());   
    }    

    if (glLineAttribBufferID != 0) {    
      pg.finalizeVertexBufferObject(glLineAttribBufferID, context.code());   
    }    
    
    if (glLineIndexBufferID != 0) {    
      pg.finalizeVertexBufferObject(glLineIndexBufferID, context.code());   
    }  
  }  
  
  
  protected void finalizePointBuffers() {
    if (glPointVertexBufferID != 0) {    
      pg.finalizeVertexBufferObject(glPointVertexBufferID, context.code());   
    }    
    
    if (glPointColorBufferID != 0) {    
      pg.finalizeVertexBufferObject(glPointColorBufferID, context.code());   
    }    

    if (glPointAttribBufferID != 0) {    
      pg.finalizeVertexBufferObject(glPointAttribBufferID, context.code());   
    }    
    
    if (glPointIndexBufferID != 0) {    
      pg.finalizeVertexBufferObject(glPointIndexBufferID, context.code());   
    }  
  }

  
  ///////////////////////////////////////////////////////////  
  
  //
  
  // Query methods  
  
  
  public float getWidth() {
    PVector min = new PVector(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY); 
    PVector max = new PVector(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY);
    if (shapeEnded) {
      getVertexMin(min);
      getVertexMax(max);
    }    
    width = max.x - min.x;
    return width;
  }

  
  public float getHeight() {
    PVector min = new PVector(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY); 
    PVector max = new PVector(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY);
    if (shapeEnded) {
      getVertexMin(min);
      getVertexMax(max);
    }    
    height = max.y - min.y;
    return height;
  }

  
  public float getDepth() {
    PVector min = new PVector(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY); 
    PVector max = new PVector(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY);
    if (shapeEnded) {
      getVertexMin(min);
      getVertexMax(max);
    }    
    depth = max.z - min.z;    
    return depth;
  }  

  
  public PVector getCenter() {
    PVector center = new PVector();
    int count = 0;
    if (shapeEnded) {
      count = getVertexSum(center, count);
      if (0 < count) {
        center.x /= count;
        center.y /= count;
        center.z /= count;
      }    
    }
    return center;
  }  
  
  
  protected void getVertexMin(PVector min) {
    updateTessellation();
    
    if (family == GROUP) {
      for (int i = 0; i < childCount; i++) {
        PShapeOpenGL child = (PShapeOpenGL) children[i];
        child.getVertexMin(min);        
      }
    } else {
      if (hasPolys) tessGeo.getPolyVertexMin(min, firstPolyVertex, lastPolyVertex);
      if (is3D()) {        
        if (hasLines) tessGeo.getLineVertexMin(min, firstLineVertex, lastLineVertex);
        if (hasPoints) tessGeo.getPointVertexMin(min, firstPointVertex, lastPointVertex);
      }
    }
  }

  
  protected void getVertexMax(PVector max) {
    updateTessellation();
    
    if (family == GROUP) {
      for (int i = 0; i < childCount; i++) {
        PShapeOpenGL child = (PShapeOpenGL) children[i];
        child.getVertexMax(max);
      }
    } else {      
      if (hasPolys) tessGeo.getPolyVertexMax(max, firstPolyVertex, lastPolyVertex);
      if (is3D()) {
        if (hasLines) tessGeo.getLineVertexMax(max, firstLineVertex, lastLineVertex);
        if (hasPoints) tessGeo.getPointVertexMax(max, firstPointVertex, lastPointVertex);
      }
    }
  }  
  
  
  protected int getVertexSum(PVector sum, int count) {
    updateTessellation();
    
    if (family == GROUP) {
      for (int i = 0; i < childCount; i++) {
        PShapeOpenGL child = (PShapeOpenGL) children[i];
        count += child.getVertexSum(sum, count);
      }
    } else {      
      if (hasPolys) count += tessGeo.getPolyVertexSum(sum, firstPolyVertex, lastPolyVertex);
      if (is3D()) {
        if (hasLines) count += tessGeo.getLineVertexSum(sum, firstLineVertex, lastLineVertex);
        if (hasPoints) count += tessGeo.getPointVertexSum(sum, firstPointVertex, lastPointVertex);
      }
    }
    return count;
  }

  
  ///////////////////////////////////////////////////////////  
  
  //
  
  // Drawing methods  
  
  
  public void textureMode(int mode) {
    if (family == GROUP) {
      for (int i = 0; i < childCount; i++) {
        PShapeOpenGL child = (PShapeOpenGL) children[i];        
        child.textureMode(mode);        
      }         
    } else {    
      textureMode = mode;
    }
  }

  
  public void texture(PImage tex) {
    if (family == GROUP) {
      for (int i = 0; i < childCount; i++) {
        PShapeOpenGL child = (PShapeOpenGL) children[i];        
        child.texture(tex);        
      }      
    } else {
      PImage tex0 = texture;
      texture = tex;
      if (tex0 != tex && parent != null) {
        ((PShapeOpenGL)parent).removeTexture(tex);
      }      
      if (parent != null) {
        ((PShapeOpenGL)parent).addTexture(texture);
        if (is2D() && stroke) {
          ((PShapeOpenGL)parent).strokedTexture(true);
        }        
      }
    }        
  }

  
  public void noTexture() {
    if (family == GROUP) {
      for (int i = 0; i < childCount; i++) {
        PShapeOpenGL child = (PShapeOpenGL) children[i];        
        child.noTexture();        
      }
    } else {
      PImage tex0 = texture;
      texture = null;
      if (tex0 != null && parent != null) {
        ((PShapeOpenGL)parent).removeTexture(tex0);
        if (is2D()) {
          ((PShapeOpenGL)parent).strokedTexture(false);
        }        
      }      
    }
  }  

  
  protected void addTexture(PImage tex) {
    if (textures == null) {
      textures = new HashSet<PImage>();      
    }
    textures.add(tex);
    if (parent != null) {
      ((PShapeOpenGL)parent).addTexture(tex);
    }   
  }
  
  
  protected void removeTexture(PImage tex) {
    if (textures == null || !textures.contains(tex)) return; // Nothing to remove.
      
    // First check that none of the child shapes
    // have texture tex...
    boolean childHasTex = false;
    for (int i = 0; i < childCount; i++) {
      PShapeOpenGL child = (PShapeOpenGL) children[i];        
      if (child.hasTexture(tex)) {
        childHasTex = true;
        break;
      }
    }
          
    if (!childHasTex) {
      // ...if not, it is safe to remove from this shape.
      textures.remove(tex);
      if (textures.size() == 0) {
        textures = null;
      }
    }

    // Since this shape and all its child shapes don't contain
    // tex anymore, we now can remove it from the parent.
    if (parent != null) {
      ((PShapeOpenGL)parent).removeTexture(tex);
    }    
  }
  
  
  protected void strokedTexture(boolean newValue) {
    if (strokedTexture == newValue) return; // Nothing to change.
    
    if (newValue) {
      strokedTexture = true;      
    } else {      
      // First check that none of the child shapes
      // have have a stroked texture...
      boolean childHasStrokedTex = false;
      for (int i = 0; i < childCount; i++) {
        PShapeOpenGL child = (PShapeOpenGL) children[i];        
        if (child.hasStrokedTexture()) {
          childHasStrokedTex = true;
          break;
        }
      }

      if (!childHasStrokedTex) {
        // ...if not, it is safe to mark this shape as without
        // stroked texture.
        strokedTexture = false;  
      }
    }
    
    // Now we can update the parent shape.
    if (parent != null) {
      ((PShapeOpenGL)parent).strokedTexture(newValue);
    }    
  }
  
  
  protected boolean hasTexture(PImage tex) {
    if (family == GROUP) {
      return textures != null && textures.contains(tex);  
    } else {
      return texture == tex;
    }
  }
  
  
  protected boolean hasStrokedTexture() {
    if (family == GROUP) {
      return strokedTexture;  
    } else {
      return texture != null && stroke;
    }
  }  
  
  
  public void solid(boolean solid) {
    if (family == GROUP) {
      for (int i = 0; i < childCount; i++) {
        PShapeOpenGL child = (PShapeOpenGL) children[i];        
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
    vertexImpl(x, y, 0, 0, 0);   
  }

  
  public void vertex(float x, float y, float u, float v) {
    vertexImpl(x, y, 0, u, v); 
  }      
  
  
  public void vertex(float x, float y, float z) {
    vertexImpl(x, y, z, 0, 0);      
  }

  
  public void vertex(float x, float y, float z, float u, float v) {
    vertexImpl(x, y, z, u, v);  
  }  
  
  
  protected void vertexImpl(float x, float y, float z, float u, float v) {
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
    
    inGeo.addVertex(x, y, z, 
                 fcolor, 
                 normalX, normalY, normalZ,
                 u, v, 
                 scolor, sweight,
                 ambientColor, specularColor, emissiveColor, shininess,
                 vertexCode());    
    
    root.tessellated = false;
    tessellated = false;  
  }
  
  
  protected int vertexCode() {
    int code = VERTEX;
    if (breakShape) {
      code = BREAK;
      breakShape = false;
    }    
    return code;
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
    inGeo.trim();
    
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
  
  
  public void setPath(int vcount, float[][] verts, int ccount, int[] codes) {
    if (family != PATH) {      
      PGraphics.showWarning("Vertex coordinates and codes can only be set to PATH shapes");
      return;
    }
    
    super.setPath(vcount, verts, ccount, codes);
    root.tessellated = false;
    tessellated = false;
    shapeEnded = true;    
  }

  
  //////////////////////////////////////////////////////////////

  // Stroke cap/join/weight set/update

  
  public void strokeWeight(float weight) {
    if (family == GROUP) {
      for (int i = 0; i < childCount; i++) {
        PShapeOpenGL child = (PShapeOpenGL) children[i];        
        child.strokeWeight(weight);
      }
    } else {
      if (fullyStroked() && strokeWeight != weight) {
        // Changing the stroke weight on a fully stroked
        // shape needs a re-tesellation in order to replace
        // the old stroke geometry.
        root.tessellated = false;           
      }          
      float prevStrokeWeight = strokeWeight; 
      strokeWeight = weight;
      updateStrokeWeight(prevStrokeWeight);
    }    
  }
  
  
  protected void updateStrokeWeight(float prevStrokeWeight) {
    if (shapeEnded && tessellated && (hasLines || hasPoints)) {      
      float resizeFactor = strokeWeight / prevStrokeWeight;
      Arrays.fill(inGeo.strokeWeights, 0, inGeo.vertexCount, strokeWeight);    
            
      if (hasLines) {        
        if (is3D()) {
          for (int i = firstLineVertex; i <= lastLineVertex; i++) {
            tessGeo.lineAttribs[4 * i + 3] *= resizeFactor;
          }        
          root.setModifiedLineAttributes(firstLineVertex, lastLineVertex);
        } else if (is2D()) {
          // TODO: implement...
        }
      }
      
      if (hasPoints) {
        if (is3D()) {
          for (int i = firstPointVertex; i <= lastPointVertex; i++) {
            tessGeo.pointAttribs[2 * i + 0] *= resizeFactor;
            tessGeo.pointAttribs[2 * i + 1] *= resizeFactor;
          }        
          root.setModifiedPointAttributes(firstPointVertex, lastPointVertex);
        } else {
          
        }        
      }            
    }    
  }


  public void strokeJoin(int join) {
    if (family == GROUP) {
      for (int i = 0; i < childCount; i++) {
        PShapeOpenGL child = (PShapeOpenGL) children[i];        
        child.strokeJoin(join);
      }
    } else {
      if (fullyStroked() && strokeJoin != join) {
        // Changing the stroke join on a shape fully stroked
        // shape needs a re-tesellation in order to replace
        // the old join geometry.
        root.tessellated = false;           
      }      
      strokeJoin = join;
    }        
  }


  public void strokeCap(int cap) {
    if (family == GROUP) {
      for (int i = 0; i < childCount; i++) {
        PShapeOpenGL child = (PShapeOpenGL) children[i];        
        child.strokeCap(cap);
      }
    } else {
      if (fullyStroked() && strokeCap != cap) {
        // Changing the stroke cap on a fully stroked
        // shape needs a re-tesellation in order to replace
        // the old cap geometry.
        root.tessellated = false;        
      }
      strokeCap = cap;      
    }    
  }
    
  
  protected boolean fullyStroked() {
    return is2D() && strokeWeight > PGL.MIN_CAPS_JOINS_WEIGHT;
  }
  
  
  //////////////////////////////////////////////////////////////

  // Fill set/update

  
  public void noFill() {
    if (family == GROUP) {
      for (int i = 0; i < childCount; i++) {
        PShapeOpenGL child = (PShapeOpenGL) children[i];        
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
        PShapeOpenGL child = (PShapeOpenGL) children[i];        
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
        PShapeOpenGL child = (PShapeOpenGL) children[i];        
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
        PShapeOpenGL child = (PShapeOpenGL) children[i];        
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
        PShapeOpenGL child = (PShapeOpenGL) children[i];        
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
        PShapeOpenGL child = (PShapeOpenGL) children[i];        
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
        PShapeOpenGL child = (PShapeOpenGL) children[i];        
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
    if (shapeEnded && tessellated && hasPolys && texture == null) {
      Arrays.fill(inGeo.colors, 0, inGeo.vertexCount, PGL.javaToNativeARGB(fillColor));
      if (is3D()) {
        Arrays.fill(tessGeo.polyColors, firstPolyVertex, lastPolyVertex + 1, PGL.javaToNativeARGB(fillColor));      
        root.setModifiedPolyColors(firstPolyVertex, lastPolyVertex);
      } else if (is2D()) {
        
      }
    }
  }
  
    
  //////////////////////////////////////////////////////////////

  // Stroke (color) set/update 
  
  
  public void noStroke() {
    if (family == GROUP) {
      for (int i = 0; i < childCount; i++) {
        PShapeOpenGL child = (PShapeOpenGL) children[i];        
        child.noStroke();        
      }      
    } else {
      if (stroke) {
        // Disabling stroke on a shape previously with
        // stroke needs a re-tesellation in order to remove
        // the additional geometry of lines and/or points.
        root.tessellated = false;        
      }
      stroke = false;
      strokeColor = 0x0;
      updateStrokeColor();
      if (is2D() && parent != null) {
        ((PShapeOpenGL)parent).strokedTexture(false);
      }
    }  
  }
  
  
  public void stroke(int rgb) {
    if (family == GROUP) {
      for (int i = 0; i < childCount; i++) {
        PShapeOpenGL child = (PShapeOpenGL) children[i];        
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
        PShapeOpenGL child = (PShapeOpenGL) children[i];        
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
        PShapeOpenGL child = (PShapeOpenGL) children[i];        
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
        PShapeOpenGL child = (PShapeOpenGL) children[i];        
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
        PShapeOpenGL child = (PShapeOpenGL) children[i];        
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
        PShapeOpenGL child = (PShapeOpenGL) children[i];        
        child.stroke(x, y, z, alpha);        
      }      
    } else {
      colorCalc(x, y, z, alpha);
      strokeFromCalc();      
    }
  }
  
  
  protected void strokeFromCalc() {
    if (!stroke) {
      // Enabling stroke on a shape previously without
      // stroke needs a re-tesellation in order to incorporate
      // the additional geometry of lines and/or points.
      root.tessellated = false;
    }
    stroke = true;
    strokeColor = calcColor;
    updateStrokeColor();
    if (is2D() && texture != null && parent != null) {
      ((PShapeOpenGL)parent).strokedTexture(true);
    }
  }

  
  protected void updateStrokeColor() {
    if (shapeEnded && tessellated && (hasLines || hasPoints)) {
      Arrays.fill(inGeo.strokeColors, 0, inGeo.vertexCount, PGL.javaToNativeARGB(strokeColor));
      
      
      if (hasLines) {        
        if (is3D()) {
          Arrays.fill(tessGeo.lineColors, firstLineVertex, lastLineVertex + 1, PGL.javaToNativeARGB(strokeColor));      
          root.setModifiedLineColors(firstLineVertex, lastLineVertex);
        } else if (is2D()) {
          
        }
      }      
      if (hasPoints) {
        if (is3D()) {
          Arrays.fill(tessGeo.pointColors, firstPointVertex, lastPointVertex + 1, PGL.javaToNativeARGB(strokeColor));
          root.setModifiedPointColors(firstPointVertex, lastPointVertex);
        } else if (is2D()) {
          
        }
      }
      
      
    }    
  }  

 
  //////////////////////////////////////////////////////////////

  // Tint set/update 
  
  
  public void noTint() {
    if (family == GROUP) {
      for (int i = 0; i < childCount; i++) {
        PShapeOpenGL child = (PShapeOpenGL) children[i];        
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
        PShapeOpenGL child = (PShapeOpenGL) children[i];        
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
        PShapeOpenGL child = (PShapeOpenGL) children[i];        
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
        PShapeOpenGL child = (PShapeOpenGL) children[i];        
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
        PShapeOpenGL child = (PShapeOpenGL) children[i];        
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
        PShapeOpenGL child = (PShapeOpenGL) children[i];        
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
        PShapeOpenGL child = (PShapeOpenGL) children[i];        
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
    if (shapeEnded && tessellated && hasPolys && texture != null) {
      Arrays.fill(inGeo.colors, 0, inGeo.vertexCount, PGL.javaToNativeARGB(tintColor));
      if (is3D()) {
        Arrays.fill(tessGeo.polyColors, firstPolyVertex, lastPolyVertex + 1, PGL.javaToNativeARGB(tintColor));      
        root.setModifiedPolyColors(firstPolyVertex, lastPolyVertex);
      } else if (is2D()) {
        
      }
    }
  }
  
  
  //////////////////////////////////////////////////////////////

  // Ambient set/update
  
  
  public void ambient(int rgb) {
    if (family == GROUP) {
      for (int i = 0; i < childCount; i++) {
        PShapeOpenGL child = (PShapeOpenGL) children[i];        
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
        PShapeOpenGL child = (PShapeOpenGL) children[i];        
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
        PShapeOpenGL child = (PShapeOpenGL) children[i];        
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
    if (shapeEnded && tessellated && hasPolys) {
      Arrays.fill(inGeo.ambient, 0, inGeo.vertexCount, PGL.javaToNativeARGB(ambientColor));
      if (is3D()) {
        Arrays.fill(tessGeo.polyAmbient, firstPolyVertex, lastPolyVertex = 1, PGL.javaToNativeARGB(ambientColor));      
        root.setModifiedPolyAmbient(firstPolyVertex, lastPolyVertex);
      } else if (is2D()) {
        
      }
    }      
  }
  
  
  //////////////////////////////////////////////////////////////

  // Specular set/update  
  

  public void specular(int rgb) {
    if (family == GROUP) {
      for (int i = 0; i < childCount; i++) {
        PShapeOpenGL child = (PShapeOpenGL) children[i];        
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
        PShapeOpenGL child = (PShapeOpenGL) children[i];        
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
        PShapeOpenGL child = (PShapeOpenGL) children[i];        
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
    if (shapeEnded && tessellated && hasPolys) {
      Arrays.fill(inGeo.specular, 0, inGeo.vertexCount, PGL.javaToNativeARGB(specularColor));
      if (is3D()) {
        Arrays.fill(tessGeo.polySpecular, firstPolyVertex, lastPolyVertex + 1, PGL.javaToNativeARGB(specularColor));      
        root.setModifiedPolySpecular(firstPolyVertex, lastPolyVertex);
      } else if (is2D()) {
        
      }      
    }
  }
  
  
  //////////////////////////////////////////////////////////////

  // Emissive set/update
  
  
  public void emissive(int rgb) {
    if (family == GROUP) {
      for (int i = 0; i < childCount; i++) {
        PShapeOpenGL child = (PShapeOpenGL) children[i];        
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
        PShapeOpenGL child = (PShapeOpenGL) children[i];        
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
        PShapeOpenGL child = (PShapeOpenGL) children[i];        
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
    if (shapeEnded && tessellated && 0 < tessGeo.polyVertexCount) {
      Arrays.fill(inGeo.emissive, 0, inGeo.vertexCount, PGL.javaToNativeARGB(emissiveColor));      
      if (is3D()) {
        Arrays.fill(tessGeo.polyEmissive, firstPolyVertex, lastPolyVertex + 1, PGL.javaToNativeARGB(emissiveColor));      
        root.setModifiedPolyEmissive(firstPolyVertex, lastPolyVertex);
      } else if (is2D()) {
        
      }
    }    
  }
  
  
  //////////////////////////////////////////////////////////////

  // Shininess set/update  
  
  
  public void shininess(float shine) {
    if (family == GROUP) {
      for (int i = 0; i < childCount; i++) {
        PShapeOpenGL child = (PShapeOpenGL) children[i];        
        child.shininess(shine);        
      }      
    } else {
      shininess = shine;
      updateShininessFactor();    
    }       
  }
  
  
  protected void updateShininessFactor() {
    if (shapeEnded && tessellated && hasPolys) {
      Arrays.fill(inGeo.shininess, 0, inGeo.vertexCount, shininess);
      if (is3D()) {
        Arrays.fill(tessGeo.polyShininess, firstPolyVertex, lastPolyVertex + 1, shininess);      
        root.setModifiedPolyShininess(firstPolyVertex, lastPolyVertex);
      } else if (is2D()) {
        
      }
    }
  }
  
  
  ///////////////////////////////////////////////////////////  
  
  //
  
  // Geometric transformations
  
  
  public void translate(float tx, float ty) {
    transform(TRANSLATE, tx, ty);
  }
  
  
  public void translate(float tx, float ty, float tz) {
    transform(TRANSLATE, tx, ty, tz);
  }
  
  
  public void rotate(float angle) {
    transform(ROTATE, angle);
  }
  
  
  public void rotate(float angle, float v0, float v1, float v2) {
    transform(ROTATE, angle, v0, v1, v2);
  }
  
  
  public void scale(float s) {
    transform(SCALE, s, s);
  }


  public void scale(float x, float y) {
    transform(SCALE, x, y);
  }


  public void scale(float x, float y, float z) {
    transform(SCALE, x, y, z);
  }  


  public void applyMatrix(float n00, float n01, float n02,
                          float n10, float n11, float n12) {
    transform(MATRIX, n00, n01, n02,
                      n10, n11, n12);
  }


  public void applyMatrix(float n00, float n01, float n02, float n03,
                          float n10, float n11, float n12, float n13,
                          float n20, float n21, float n22, float n23,
                          float n30, float n31, float n32, float n33) {
    transform(MATRIX, n00, n01, n02, n03,
                      n10, n11, n12, n13,
                      n20, n21, n22, n23,
                      n30, n31, n32, n33);
  }
  
  
  public void resetMatrix() {
    if (shapeEnded && matrix != null) {
      if (family == GROUP) {
        updateTessellation();
      }
      boolean res = matrix.invert();
      if (res) {
        if (tessellated) {
          applyMatrixImpl(matrix);
        }
        matrix = null;
      } else {
        PGraphics.showWarning("The transformation matrix cannot be inverted");
      }
    }
  }
  
  
  protected void transform(int type, float... args) {
    int dimensions;
    if (type == ROTATE) {
      dimensions = args.length == 1 ? 2 : 3;
    } else if (type == MATRIX) {
      dimensions = args.length == 6 ? 2 : 3;
    } else {
      dimensions = args.length;
    }    
    transformImpl(type, dimensions, args);
  }
  
  
  protected void transformImpl(int type, int ncoords, float... args) {
    if (shapeEnded) {
      if (family == GROUP) {
        updateTessellation();
        // The tessellation is not updated for geometry/primitive shapes
        // because a common situation is shapes not still tessellated
        // but being transformed before adding them to the parent group
        // shape. If each shape is tessellated individually, then the process
        // is significantly slower than tessellating all the geometry in a single 
        // batch when calling tessellate() on the root shape.
      }
      checkMatrix(ncoords);
      calcTransform(type, ncoords, args);      
      if (tessellated) {
        applyMatrixImpl(transform);
      }
    }          
  }
  
  
  protected void calcTransform(int type, int dimensions, float... args) {
    if (transform == null) {
      if (dimensions == 2) {
        transform = new PMatrix2D();
      } else {
        transform = new PMatrix3D();
      }
    } else {
      transform.reset();
    }
      
    switch (type) {
    case TRANSLATE:
      if (dimensions == 3) {
        transform.translate(args[0], args[1], args[2]);
      } else {
        transform.translate(args[0], args[1]);
      }
      break;
    case ROTATE:
      if (dimensions == 3) {
        transform.rotate(args[0], args[1], args[2], args[3]);
      } else {
        transform.rotate(args[0]);
      }
      break;
    case SCALE:
      if (dimensions == 3) {
        transform.scale(args[0], args[1], args[2]);
      } else {
        transform.scale(args[0], args[1]);
      }
      break;
    case MATRIX:
      if (dimensions == 3) {
        transform.set(args[ 0], args[ 1], args[ 2], args[ 3],
                      args[ 4], args[ 5], args[ 6], args[ 7],
                      args[ 8], args[ 9], args[10], args[11],
                      args[12], args[13], args[14], args[15]);
      } else {
        transform.set(args[0], args[1], args[2],
                      args[3], args[4], args[5]);
      }
      break;      
    }   
    matrix.apply(transform);
  }
  
  
  protected void applyMatrixImpl(PMatrix matrix) {
    if (hasPolys) {
      tessGeo.applyMatrixOnPolyGeometry(matrix, firstPolyVertex, lastPolyVertex);      
      root.setModifiedPolyVertices(firstPolyVertex, lastPolyVertex);
      root.setModifiedPolyNormals(firstPolyVertex, lastPolyVertex);
    }
    
    if (is3D()) {
      if (hasLines) {
        tessGeo.applyMatrixOnLineGeometry(matrix, firstLineVertex, lastLineVertex);      
        root.setModifiedLineVertices(firstLineVertex, lastLineVertex);
        root.setModifiedLineAttributes(firstLineVertex, lastLineVertex);
      }    
      
      if (hasPoints) {
        tessGeo.applyMatrixOnPointGeometry(matrix, firstPointVertex, lastPointVertex);      
        root.setModifiedPointVertices(firstPointVertex, lastPointVertex);
      }    
    }
  }
  
  
  ///////////////////////////////////////////////////////////  
  
  //
  
  // Bezier curves 
  
  
  public void bezierDetail(int detail) {
    bezierDetail = detail;
    pg.bezierDetail(detail);
  }  
  
  
  public void bezierVertex(float x2, float y2,
                           float x3, float y3,
                           float x4, float y4) {
    bezierVertex(x2, y2, 0,
                 x3, y3, 0,
                 x4, y4, 0);
  }
  
  
  public void bezierVertex(float x2, float y2, float z2,
                           float x3, float y3, float z3,
                           float x4, float y4, float z4) {
    inGeo.setMaterial(fillColor, strokeColor, strokeWeight,
                      ambientColor, specularColor, emissiveColor, shininess);
    inGeo.setNormal(normalX, normalY, normalZ);
    inGeo.addBezierVertex(x2, y2, z2,
                       x3, y3, z3,
                       x4, y4, z4,
                       fill, stroke, bezierDetail, vertexCode(), kind);     
  }
  
  
  public void quadraticVertex(float cx, float cy,
                              float x3, float y3) {
    quadraticVertex(cx, cy, 0,
                    x3, y3, 0);
  }  
  
  
  public void quadraticVertex(float cx, float cy, float cz,
                              float x3, float y3, float z3) {
    inGeo.setMaterial(fillColor, strokeColor, strokeWeight,
                      ambientColor, specularColor, emissiveColor, shininess);
    inGeo.setNormal(normalX, normalY, normalZ);    
    inGeo.addQuadraticVertex(cx, cy, cz,
                          x3, y3, z3,
                          fill, stroke, bezierDetail, vertexCode(), kind); 
  }
  
  
  ///////////////////////////////////////////////////////////  
  
  //
  
  // Catmull-Rom curves

  
  public void curveDetail(int detail) {
    curveDetail = detail;
    pg.curveDetail(detail);
  }
  
  
  public void curveTightness(float tightness) {
    curveTightness = tightness;
    pg.curveTightness(tightness);
  }  
  
  
  public void curveVertex(float x, float y) {
    curveVertex(x, y, 0);
  }  

  
  public void curveVertex(float x, float y, float z) {
    inGeo.setMaterial(fillColor, strokeColor, strokeWeight,
                      ambientColor, specularColor, emissiveColor, shininess);
    inGeo.setNormal(normalX, normalY, normalZ);
    inGeo.addCurveVertex(x, y, z,
                      fill, stroke, curveDetail, vertexCode(), kind); 
  }
  
  
  ///////////////////////////////////////////////////////////  
  
  //
  
  // Setters/getters of individual vertices
  

  public int getVertexCount() {
    return inGeo.vertexCount;  
  }
  
  
  public PVector getVertex(int index, PVector vec) {
    updateTessellation();
    
    if (vec == null) {
      vec = new PVector();
    }
    vec.x = inGeo.vertices[3 * index + 0];
    vec.y = inGeo.vertices[3 * index + 1];
    vec.z = inGeo.vertices[3 * index + 2];
    return vec;
  }
  
  
  public float getVertexX(int index) {
    updateTessellation();
    
    return inGeo.vertices[3 * index + 0];
  }
  
  
  public float getVertexY(int index) {
    updateTessellation();
    
    return inGeo.vertices[3 * index + 1];
  }
  
  
  public float getVertexZ(int index) {
    updateTessellation();
    
    return inGeo.vertices[3 * index + 2];
  }
  
  
  public void setVertex(int index, float x, float y) {
    setVertex(index, x, y, 0);
  }
  
  
  public void setVertex(int index, float x, float y, float z) {
    updateTessellation();
    
    /*
    int[] indices;
    int[] indices1;
    float[] vertices;
    float[] attribs;    
          
    if (havePoints) {
      indices = inGeo.tessMap.pointIndices[index];
      vertices = tessGeo.pointVertices;      
      for (int i = 0; i < indices.length; i++) {
        int tessIdx = indices[i];        
        vertices[4 * tessIdx + 0] = x;
        vertices[4 * tessIdx + 1] = y;
        vertices[4 * tessIdx + 2] = z;
        root.setModifiedPointVertices(tessIdx, tessIdx);
      } 
    }
    
    if (haveLines) {
      indices = inGeo.tessMap.lineIndices0[index];
      indices1 = inGeo.tessMap.lineIndices1[index];      
      vertices = tessGeo.lineVertices;
      attribs = tessGeo.lineDirWidths;
      for (int i = 0; i < indices.length; i++) {
        int tessIdx = indices[i];        
        vertices[4 * tessIdx + 0] = x;
        vertices[4 * tessIdx + 1] = y;
        vertices[4 * tessIdx + 2] = z;
        root.setModifiedLineVertices(tessIdx, tessIdx);
        
        int tessIdx1 = indices1[i];        
        attribs[4 * tessIdx1 + 0] = x;
        attribs[4 * tessIdx1 + 1] = y;
        attribs[4 * tessIdx1 + 2] = z;
        root.setModifiedLineAttributes(tessIdx1, tessIdx1);
      }     
    }
    
    if (haveFill) {
      vertices = tessGeo.polyVertices;
      if (-1 < inGeo.tessMap.firstFillIndex) {
        // 1-1 mapping, only need to offset the input index
        int tessIdx = inGeo.tessMap.firstFillIndex + index;
        vertices[4 * tessIdx + 0] = x;
        vertices[4 * tessIdx + 1] = y;
        vertices[4 * tessIdx + 2] = z;
        root.setModifiedFillVertices(tessIdx, tessIdx);
      } else {
        // Multi-valued mapping. Going through all the tess
        // vertices affected by inIdx.
        float x0 = inGeo.vertices[4 * index + 0];
        float y0 = inGeo.vertices[4 * index + 1];
        float z0 = inGeo.vertices[4 * index + 2];        
        indices = inGeo.tessMap.fillIndices[index];
        float[] weights = inGeo.tessMap.fillWeights[index];
        for (int i = 0; i < indices.length; i++) {
          // tessIdx is a linear combination of input vertices,
          // including inIdx:
          // tessVert[tessIdx] = SUM(i from I, inVert[i]), inIdx in I
          // For example:
          // xt = w0 * x0 + w1 * x1 + w2 * x2
          // If x2 changes from x2 to x2', then the new value of xt is:
          // xt' = w0 * x0 + w1 * x1 + w2 * x2' =
          //     = w0 * x0 + w1 * x1 + w2 * x2' + w2 * x2 - w2 * x2 
          //     = xt + w2 * (x2' - x2)
          // This explains the calculations below:
          int tessIdx = indices[i];
          if (-1 < tessIdx) {          
            float weight = weights[i];          
            float tx0 = vertices[4 * tessIdx + 0];
            float ty0 = vertices[4 * tessIdx + 1];
            float tz0 = vertices[4 * tessIdx + 2];        
            vertices[4 * tessIdx + 0] = tx0 + weight * (x - x0);
            vertices[4 * tessIdx + 1] = ty0 + weight * (y - y0);
            vertices[4 * tessIdx + 2] = tz0 + weight * (z - z0);       
            root.setModifiedFillVertices(tessIdx, tessIdx);
          } else {
            root.forceTessellation = true;
            break;
          }
        }    
      }  
    }
*/
    inGeo.vertices[3 * index + 0] = x;
    inGeo.vertices[3 * index + 1] = y;
    inGeo.vertices[3 * index + 2] = z;
  }
  
  
  public PVector getNormal(int index, PVector vec) {
    updateTessellation();
    
    if (vec == null) {
      vec = new PVector();
    }
    vec.x = inGeo.normals[3 * index + 0];
    vec.y = inGeo.normals[3 * index + 1];
    vec.z = inGeo.normals[3 * index + 2];
    return vec;
  }
  
  
  public float getNormalX(int index) {
    updateTessellation();
    
    return inGeo.normals[3 * index + 0];
  }

  
  public float getNormalY(int index) {
    updateTessellation();
    
    return inGeo.normals[3 * index + 1];
  }  

  
  public float getNormalZ(int index) {
    updateTessellation();
    
    return inGeo.normals[3 * index + 2];
  }    
  
  
  public void setNormal(int index, float nx, float ny, float nz) {
    updateTessellation();
    /*
    if (haveFill) {
      float[] normals = tessGeo.polyNormals;
      
      if (-1 < inGeo.tessMap.firstFillIndex) {
        int tessIdx = inGeo.tessMap.firstFillIndex + index;
        normals[3 * tessIdx + 0] = nx;
        normals[3 * tessIdx + 1] = ny;
        normals[3 * tessIdx + 2] = nz;
        root.setModifiedFillNormals(tessIdx, tessIdx);
      } else {
        float nx0 = inGeo.normals[3 * index + 0];
        float ny0 = inGeo.normals[3 * index + 1];
        float nz0 = inGeo.normals[3 * index + 2];        
        int[] indices = inGeo.tessMap.fillIndices[index];
        float[] weights = inGeo.tessMap.fillWeights[index];
        for (int i = 0; i < indices.length; i++) {
          int tessIdx = indices[i];
          if (-1 < tessIdx) { 
            float weight = weights[i];
            float tnx0 = normals[3 * tessIdx + 0];
            float tny0 = normals[3 * tessIdx + 1];
            float tnz0 = normals[3 * tessIdx + 2];        
            float tnx = tnx0 + weight * (nx - nx0);
            float tny = tny0 + weight * (ny - ny0);
            float tnz = tnz0 + weight * (nz - nz0);
            
            // Making sure that the new normal vector is indeed
            // normalized.
            float sum = tnx * tnx + tny * tny + tnz * tnz;
            float len = PApplet.sqrt(sum);
            tnx /= len;
            tny /= len;
            tnz /= len;
             
            normals[3 * tessIdx + 0] = tnx;
            normals[3 * tessIdx + 1] = tny;
            normals[3 * tessIdx + 2] = tnz;
            root.setModifiedFillNormals(tessIdx, tessIdx);
          }
        }    
      }          
    }
    */
    inGeo.normals[3 * index + 0] = nx;
    inGeo.normals[3 * index + 1] = ny;
    inGeo.normals[3 * index + 2] = nz;      
  }
  
  
  public float getTextureU(int index) {
    updateTessellation();
    
    return inGeo.texcoords[2 * index + 0];
  }

  
  public float getTextureV(int index) {
    updateTessellation();
    
    return inGeo.texcoords[2 * index + 1];
  }  
  
  
  public void setTextureUV(int index, float u, float v) {
    updateTessellation();
    /*
    if (haveFill) {
      float[] texcoords = tessGeo.polyTexcoords;
      
      if (-1 < inGeo.tessMap.firstFillIndex) {
        int tessIdx = inGeo.tessMap.firstFillIndex + index;
        tessGeo.polyTexcoords[2 * tessIdx + 0] = u;
        tessGeo.polyTexcoords[2 * tessIdx + 1] = v;
        root.setModifiedFillTexcoords(tessIdx, tessIdx);
      } else {       
        float u0 = inGeo.texcoords[2 * index + 0];
        float v0 = inGeo.texcoords[2 * index + 1];
        int[] indices = inGeo.tessMap.fillIndices[index];
        float[] weights = inGeo.tessMap.fillWeights[index];
        for (int i = 0; i < indices.length; i++) {
          int tessIdx = indices[i];
          if (-1 < tessIdx) { 
            float weight = weights[i];
            float tu0 = texcoords[2 * tessIdx + 0];
            float tv0 = texcoords[2 * tessIdx + 1];        
            float tu = tu0 + weight * (u - u0);
            float tv = tv0 + weight * (v - v0);           
            texcoords[2 * tessIdx + 0] = tu;
            texcoords[2 * tessIdx + 1] = tv;
            root.setModifiedFillTexcoords(tessIdx, tessIdx);
          }
        }        
      }       
    }    
    */
    inGeo.texcoords[2 * index + 0] = u;
    inGeo.texcoords[2 * index + 1] = v;
  }
  
  
  public int getFill(int index) {
    updateTessellation();
    
    return PGL.nativeToJavaARGB(inGeo.colors[index]);
  }

  
  public void setFill(int index, int fill) {    
    updateTessellation();    
    
    fill = PGL.javaToNativeARGB(fill);
/*
    if (haveFill) {
      int[] colors = tessGeo.polyColors;
      
      if (-1 < inGeo.tessMap.firstFillIndex) {
        int tessIdx = inGeo.tessMap.firstFillIndex + index;
        colors[tessIdx] = fill;
        root.setModifiedFillColors(tessIdx, tessIdx);
      } else {
        int[] indices = inGeo.tessMap.fillIndices[index];
        float[] weights = inGeo.tessMap.fillWeights[index];
        int fill0 = inGeo.colors[index];
        setColorARGB(colors, fill, fill0, indices, weights);   
        for (int i = 0; i < indices.length; i++) {
          int tessIdx = indices[i];
          if (-1 < tessIdx) {           
            root.setModifiedFillColors(tessIdx, indices[i]);
          }
        }        
      }  
    }
    */
    inGeo.colors[index] = fill;
  }  
  
  
  public int getStroke(int index) {
    updateTessellation();
    
    return PGL.nativeToJavaARGB(inGeo.strokeColors[index]);
  }

  
  public void setStroke(int index, int stroke) {
    updateTessellation();
   
    stroke = PGL.javaToNativeARGB(stroke);
    /*         
    if (havePoints) {
      int[] indices = inGeo.tessMap.pointIndices[index];
      int[] colors = tessGeo.pointColors; 
      for (int i = 0; i < indices.length; i++) {
        int tessIdx = indices[i];
        colors[tessIdx] = stroke;
        root.setModifiedPointColors(tessIdx, tessIdx);
      } 
    }
      
    if (haveLines) {
      int[] colors = tessGeo.lineColors;      
      int[] indices = inGeo.tessMap.lineIndices0[index];       
      for (int i = 0; i < indices.length; i++) {
        int tessIdx = indices[i];
        colors[tessIdx] = stroke;
        root.setModifiedLineColors(tessIdx, tessIdx);
      }
      indices = inGeo.tessMap.lineIndices1[index];       
      for (int i = 0; i < indices.length; i++) {
        int tessIdx = indices[i];
        colors[tessIdx] = stroke;
        root.setModifiedLineColors(tessIdx, tessIdx);
      }  
    }    
    */
    inGeo.strokeColors[index] = stroke;
  }  
  
  
  public float getStrokeWeight(int index) {
    updateTessellation();
    
    return inGeo.strokeWeights[index];
  }
  

  public void setStrokeWeight(int index, float weight) {
    updateTessellation();
        /*
    if (havePoints || haveLines) {
      int[] indices;
      float[] attribs;      
      
      float weight0 = inGeo.sweights[index]; 
      float resizeFactor = weight / weight0;
      
      if (havePoints) {
        indices = inGeo.tessMap.pointIndices[index];
        attribs = tessGeo.pointSizes; 
        for (int i = 0; i < indices.length; i++) {
          int tessIdx = indices[i];
          attribs[2 * tessIdx + 0] *= resizeFactor;
          attribs[2 * tessIdx + 1] *= resizeFactor;
          root.setModifiedPointAttributes(tessIdx, tessIdx);
        }      
      }
      
      if (haveLines) {
        attribs = tessGeo.lineDirWidths;
        indices = inGeo.tessMap.lineIndices0[index];
        for (int i = 0; i < indices.length; i++) {
          int tessIdx = indices[i];
          attribs[4 * tessIdx + 3] *= resizeFactor;
          root.setModifiedLineAttributes(tessIdx, tessIdx);
        }
        indices = inGeo.tessMap.lineIndices1[index];
        for (int i = 0; i < indices.length; i++) {
          int tessIdx = indices[i];
          attribs[4 * tessIdx + 3] *= resizeFactor;
          root.setModifiedLineAttributes(tessIdx, tessIdx);
        }
      }
    }
    */
    inGeo.strokeWeights[index] = weight;
  }   

  
  public int getAmbient(int index) {
    updateTessellation();
    
    return PGL.nativeToJavaARGB(inGeo.ambient[index]);
  }

  
  public void setAmbient(int index, int ambient) {
    updateTessellation();
    
    ambient = PGL.javaToNativeARGB(ambient);
    /*
    if (haveFill) {
      int[] colors = tessGeo.polyAmbient;
      
      if (-1 < inGeo.tessMap.firstFillIndex) {
        int tessIdx = inGeo.tessMap.firstFillIndex + index;
        colors[tessIdx] = ambient;
        root.setModifiedFillAmbient(tessIdx, tessIdx);
      } else {
        int[] indices = inGeo.tessMap.fillIndices[index];
        float[] weights = inGeo.tessMap.fillWeights[index];
        int ambient0 = inGeo.ambient[index];
        setColorRGB(colors, ambient, ambient0, indices, weights);
        for (int i = 0; i < indices.length; i++) {
          int tessIdx = indices[i];
          if (-1 < tessIdx) { 
            root.setModifiedFillAmbient(tessIdx, indices[i]);  
          }                    
        }        
      }  
    }
    */
    inGeo.ambient[index] = ambient;
  }    
  
  public int getSpecular(int index) {
    updateTessellation();
    
    return PGL.nativeToJavaARGB(inGeo.specular[index]);
  }

  
  public void setSpecular(int index, int specular) {
    updateTessellation();
    
    specular = PGL.javaToNativeARGB(specular);
    /*
    if (haveFill) {
      int[] colors = tessGeo.polySpecular;
      
      if (-1 < inGeo.tessMap.firstFillIndex) {
        int tessIdx = inGeo.tessMap.firstFillIndex + index;
        colors[tessIdx] = specular;
        root.setModifiedFillSpecular(tessIdx, tessIdx);
      } else {
        int[] indices = inGeo.tessMap.fillIndices[index];
        float[] weights = inGeo.tessMap.fillWeights[index];
        int specular0 = inGeo.specular[index];
        setColorRGB(colors, specular, specular0, indices, weights);
        for (int i = 0; i < indices.length; i++) {
          int tessIdx = indices[i];
          if (-1 < tessIdx) {           
            root.setModifiedFillSpecular(tessIdx, indices[i]);
          }
        }        
      }  
    }
    */
    inGeo.specular[index] = specular;
  }    
    
  
  public int getEmissive(int index) {
    updateTessellation();
    
    return PGL.nativeToJavaARGB(inGeo.emissive[index]);
  }

  
  public void setEmissive(int index, int emissive) {
    updateTessellation();
    
    emissive = PGL.javaToNativeARGB(emissive);
    /*
    if (haveFill) {
      int[] colors = tessGeo.polyEmissive;
      
      if (-1 < inGeo.tessMap.firstFillIndex) {
        int tessIdx = inGeo.tessMap.firstFillIndex + index;
        colors[tessIdx] = emissive;
        root.setModifiedFillEmissive(tessIdx, tessIdx);
      } else {
        int[] indices = inGeo.tessMap.fillIndices[index];
        float[] weights = inGeo.tessMap.fillWeights[index];
        int emissive0 = inGeo.emissive[index];
        setColorRGB(colors, emissive, emissive0, indices, weights);
        for (int i = 0; i < indices.length; i++) {
          int tessIdx = indices[i];
          if (-1 < tessIdx) {           
            root.setModifiedFillEmissive(tessIdx, indices[i]);
          }
        }        
      }  
    }
    */
    inGeo.emissive[index] = emissive;
  }     
  
  
  public float getShininess(int index) {
    updateTessellation();
    
    return inGeo.shininess[index];
  }

  
  public void setShininess(int index, float shine) {
    updateTessellation();
    /*
    if (haveFill) {
      float[] shininess = tessGeo.polyShininess;
      
      if (-1 < inGeo.tessMap.firstFillIndex) {
        int tessIdx = inGeo.tessMap.firstFillIndex + index;
        shininess[tessIdx] = shine;
        root.setModifiedFillShininess(tessIdx, tessIdx);
      } else {
        float shine0 = inGeo.shininess[index];
        int[] indices = inGeo.tessMap.fillIndices[index];
        float[] weights = inGeo.tessMap.fillWeights[index];
        for (int i = 0; i < indices.length; i++) {
          int tessIdx = indices[i];
          if (-1 < tessIdx) {  
            float weight = weights[i];
            float tshine0 = shininess[tessIdx];
            float tshine = tshine0 + weight * (shine - shine0);          
            shininess[tessIdx] = tshine;
            root.setModifiedFillShininess(tessIdx, tessIdx);
          }
        }    
      }
    }    
    */
    inGeo.shininess[index] = shine;
  }
  
  /*
  protected void setColorARGB(int[] colors, int fill, int fill0, int[] indices, float[] weights) {
    float a = (fill >> 24) & 0xFF;
    float r = (fill >> 16) & 0xFF;
    float g = (fill >>  8) & 0xFF;
    float b = (fill >>  0) & 0xFF;
    
    float a0 = (fill0 >> 24) & 0xFF;
    float r0 = (fill0 >> 16) & 0xFF;
    float g0 = (fill0 >>  8) & 0xFF;
    float b0 = (fill0 >>  0) & 0xFF;

    for (int i = 0; i < indices.length; i++) {
      int tessIdx = indices[i];
      if (-1 < tessIdx) {
        float weight = weights[i];
        int tfill0 = colors[tessIdx];          
        float ta0 = (tfill0 >> 24) & 0xFF;
        float tr0 = (tfill0 >> 16) & 0xFF;
        float tg0 = (tfill0 >>  8) & 0xFF;
        float tb0 = (tfill0 >>  0) & 0xFF;
        
        int ta = (int) (ta0 + weight * (a - a0));
        int tr = (int) (tr0 + weight * (r - r0));
        int tg = (int) (tg0 + weight * (g - g0));
        int tb = (int) (tb0 + weight * (b - b0));
         
        colors[tessIdx] = (ta << 24) | (tr << 16) | (tg << 8) | tb;
      }
    }       
  }
  
  
  protected void setColorRGB(int[] colors, int fill, int fill0, int[] indices, float[] weights) {
    float r = (fill >> 16) & 0xFF;
    float g = (fill >>  8) & 0xFF;
    float b = (fill >>  0) & 0xFF;
    
    float r0 = (fill0 >> 16) & 0xFF;
    float g0 = (fill0 >>  8) & 0xFF;
    float b0 = (fill0 >>  0) & 0xFF;

    for (int i = 0; i < indices.length; i++) {
      int tessIdx = indices[i];
      if (-1 < tessIdx) {
        float weight = weights[i];
        int tfill0 = colors[tessIdx];          
        float tr0 = (tfill0 >> 16) & 0xFF;
        float tg0 = (tfill0 >>  8) & 0xFF;
        float tb0 = (tfill0 >>  0) & 0xFF;
        
        int tr = (int) (tr0 + weight * (r - r0));
        int tg = (int) (tg0 + weight * (g - g0));
        int tb = (int) (tb0 + weight * (b - b0));
         
        colors[tessIdx] = 0xff000000 | (tr << 16) | (tg << 8) | tb;
      }
    }       
  }      
  */
  
  ///////////////////////////////////////////////////////////  
  
  //
  
  // Tessellated geometry getter.
  
  // TODO: in 2D mode, put fill, line and point vertices in separate subshapes.
  public PShape getTessellation() {
    updateTessellation();
    
    float[] vertices = tessGeo.polyVertices;
    float[] normals = tessGeo.polyNormals;
    int[] color = tessGeo.polyColors;
    float[] uv = tessGeo.polyTexcoords;
    short[] indices = tessGeo.polyIndices;
    
    PShape tess = pg.createShape(TRIANGLES);
    tess.noStroke();
    
    IndexCache cache = tessGeo.polyIndexCache;
    for (int n = firstPolyIndexCache; n <= lastPolyIndexCache; n++) {     
      int ioffset = cache.indexOffset[n];
      int icount = cache.indexCount[n];
      int voffset = cache.vertexOffset[n];
      
      for (int tr = ioffset / 3; tr < (ioffset + icount) / 3; tr++) {
        int i0 = voffset + indices[3 * tr + 0];
        int i1 = voffset + indices[3 * tr + 1];
        int i2 = voffset + indices[3 * tr + 2];

        float x0 = vertices[4 * i0 + 0], y0 = vertices[4 * i0 + 1], z0 = vertices[4 * i0 + 2];
        float x1 = vertices[4 * i1 + 0], y1 = vertices[4 * i1 + 1], z1 = vertices[4 * i1 + 2];
        float x2 = vertices[4 * i2 + 0], y2 = vertices[4 * i2 + 1], z2 = vertices[4 * i2 + 2];
        
        float nx0 = normals[3 * i0 + 0], ny0 = normals[3 * i0 + 1], nz0 = normals[3 * i0 + 2];
        float nx1 = normals[3 * i1 + 0], ny1 = normals[3 * i1 + 1], nz1 = normals[3 * i1 + 2];
        float nx2 = normals[3 * i2 + 0], ny2 = normals[3 * i2 + 1], nz2 = normals[3 * i2 + 2];
                
        int argb0 = PGL.nativeToJavaARGB(color[i0]);
        int argb1 = PGL.nativeToJavaARGB(color[i1]);
        int argb2 = PGL.nativeToJavaARGB(color[i2]);        
        
        tess.fill(argb0);
        tess.normal(nx0, ny0, nz0);
        tess.vertex(x0, y0, z0, uv[2 * i0 + 0], uv[2 * i0 + 1]);
        
        tess.fill(argb1);
        tess.normal(nx1, ny1, nz1);
        tess.vertex(x1, y1, z1, uv[2 * i1 + 0], uv[2 * i1 + 1]);
        
        tess.fill(argb2);
        tess.normal(nx2, ny2, nz2);
        tess.vertex(x2, y2, z2, uv[2 * i2 + 0], uv[2 * i2 + 1]);                      
      }
    }
    tess.end();
    
    return tess;
  }
      
    
  ///////////////////////////////////////////////////////////  
  
  //
  
  // Tessellation
  
  
  protected void updateTessellation() {
    if (!root.tessellated || root.contextIsOutdated()) {
      root.tessellate();
      root.aggregate();
    }
  }
  
  
  protected void updateTessellation(boolean force) {
    if (force || !root.tessellated || root.contextIsOutdated()) {
      root.tessellate();
      root.aggregate();
    }
  }
  
  
  protected void tessellate() {
    if (root == this && parent == null) {      
      if (tessGeo == null) {
        tessGeo = pg.newTessGeometry(PGraphicsOpenGL.RETAINED);
      }      
      tessGeo.clear();
      
      tessellateImpl();
      
      // Tessellated arrays are trimmed since they are expanded 
      // by doubling their old size, which might lead to arrays 
      // larger than the vertex counts.        
      tessGeo.trim(); 
      
      needBufferInit = true;
      
      forceTessellation = false;
      
      modified = false;
      
      modifiedPolyVertices = false;
      modifiedPolyColors = false;
      modifiedPolyNormals = false;
      modifiedPolyTexcoords = false;
      modifiedPolyAmbient = false;
      modifiedPolySpecular = false;
      modifiedPolyEmissive = false;
      modifiedPolyShininess = false;
      
      modifiedLineVertices = false;
      modifiedLineColors = false;
      modifiedLineAttributes = false;

      modifiedPointVertices = false;
      modifiedPointColors = false;
      modifiedPointAttributes = false;      
      
      firstModifiedPolyVertex = PConstants.MAX_INT;      
      lastModifiedPolyVertex = PConstants.MIN_INT;
      firstModifiedPolyColor = PConstants.MAX_INT;
      lastModifiedPolyColor = PConstants.MIN_INT;
      firstModifiedPolyNormal = PConstants.MAX_INT;
      lastModifiedPolyNormal = PConstants.MIN_INT;
      firstModifiedPolyTexcoord = PConstants.MAX_INT;
      lastModifiedPolyTexcoord = PConstants.MIN_INT;
      firstModifiedPolyAmbient = PConstants.MAX_INT;
      lastModifiedPolyAmbient = PConstants.MIN_INT;
      firstModifiedPolySpecular = PConstants.MAX_INT;
      lastModifiedPolySpecular = PConstants.MIN_INT;
      firstModifiedPolyEmissive = PConstants.MAX_INT;
      lastModifiedPolyEmissive = PConstants.MIN_INT;
      firstModifiedPolyShininess = PConstants.MAX_INT;
      lastModifiedPolyShininess = PConstants.MIN_INT;
      
      firstModifiedLineVertex = PConstants.MAX_INT;
      lastModifiedLineVertex = PConstants.MIN_INT;
      firstModifiedLineColor = PConstants.MAX_INT;
      lastModifiedLineColor = PConstants.MIN_INT;
      firstModifiedLineAttribute = PConstants.MAX_INT;
      lastModifiedLineAttribute = PConstants.MIN_INT;
      
      firstModifiedPointVertex = PConstants.MAX_INT;
      lastModifiedPointVertex = PConstants.MIN_INT;
      firstModifiedPointColor = PConstants.MAX_INT;
      lastModifiedPointColor = PConstants.MIN_INT;
      firstModifiedPointAttribute = PConstants.MAX_INT;
      lastModifiedPointAttribute = PConstants.MIN_INT;      
    }    
  }

  
  protected void tessellateImpl() {
    tessGeo = root.tessGeo;

    firstPolyIndexCache = -1;
    lastPolyIndexCache = -1; 
    firstLineIndexCache = -1;
    lastLineIndexCache = -1;
    firstPointIndexCache = -1;
    lastPointIndexCache = -1;

    if (family == GROUP) {
      for (int i = 0; i < childCount; i++) {
        PShapeOpenGL child = (PShapeOpenGL) children[i];
        child.tessellateImpl();
      }
    } else {   
      if (shapeEnded) {
        // If the geometry was tessellated previously, then
        // the edges information will still be stored in the
        // input object, so it needs to be removed to avoid
        // duplication.
        inGeo.clearEdges();
        
        tessellator.setInGeometry(inGeo);
        tessellator.setTessGeometry(tessGeo);
        tessellator.setFill(fill || texture != null);
        tessellator.setStroke(stroke);
        tessellator.setStrokeColor(strokeColor);
        tessellator.setStrokeWeight(strokeWeight);
        tessellator.setStrokeCap(strokeCap);
        tessellator.setStrokeJoin(strokeJoin);     
        tessellator.setTexCache(null, null, null);
        
        if (family == GEOMETRY) {
          // The tessellation maps are used to associate input
          // vertices with the corresponding tessellated vertices.
          // This correspondence might not be one-to-one, in the
          // case of lines and polygon shapes for example.
          inGeo.initTessMap(tessGeo);
          
          if (kind == POINTS) {
            tessellator.tessellatePoints();    
          } else if (kind == LINES) {
            tessellator.tessellateLines();
          } else if (kind == LINE_STRIP) {
            tessellator.tessellateLineStrip();
          } else if (kind == LINE_LOOP) {
            tessellator.tessellateLineLoop();
          } else if (kind == TRIANGLE || kind == TRIANGLES) {
            if (stroke) inGeo.addTrianglesEdges();
            if (normalMode == NORMAL_MODE_AUTO) inGeo.calcTrianglesNormals();
            tessellator.tessellateTriangles();
          } else if (kind == TRIANGLE_FAN) {
            if (stroke) inGeo.addTriangleFanEdges();
            if (normalMode == NORMAL_MODE_AUTO) inGeo.calcTriangleFanNormals();
            tessellator.tessellateTriangleFan();
          } else if (kind == TRIANGLE_STRIP) {            
            if (stroke) inGeo.addTriangleStripEdges();
            if (normalMode == NORMAL_MODE_AUTO) inGeo.calcTriangleStripNormals();
            tessellator.tessellateTriangleStrip();
          } else if (kind == QUAD || kind == QUADS) {            
            if (stroke) inGeo.addQuadsEdges();
            if (normalMode == NORMAL_MODE_AUTO) inGeo.calcQuadsNormals();
            tessellator.tessellateQuads();
          } else if (kind == QUAD_STRIP) {
            if (stroke) inGeo.addQuadStripEdges();
            if (normalMode == NORMAL_MODE_AUTO) inGeo.calcQuadStripNormals();
            tessellator.tessellateQuadStrip();
          } else if (kind == POLYGON) {
            if (stroke) inGeo.addPolygonEdges(isClosed);
            tessellator.tessellatePolygon(isSolid, isClosed, normalMode == NORMAL_MODE_AUTO);
          }
        } else if (family == PRIMITIVE) {          
          // The input geometry needs to be cleared because the geometry
          // generation methods in InGeometry add the vertices of the
          // new primitive to what is already stored.
          inGeo.clear();
          
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
          tessellatePath();
        }
                
        if (texture != null && parent != null) {
          ((PShapeOpenGL)parent).addTexture(texture);
        }
        
        inGeo.compactTessMap();

        firstPolyIndexCache = tessellator.firstPolyIndexCache;
        lastPolyIndexCache = tessellator.lastPolyIndexCache;
        firstLineIndexCache = tessellator.firstLineIndexCache;
        lastLineIndexCache = tessellator.lastLineIndexCache;        
        firstPointIndexCache = tessellator.firstPointIndexCache;
        lastPointIndexCache = tessellator.lastPointIndexCache;
      }
    }
        
    firstPolyVertex = lastPolyVertex = -1;
    firstLineVertex = lastLineVertex = -1;
    firstPointVertex = lastPointVertex = -1;
    
    tessellated = true;
  }
  
  
  protected void tessellatePoint() {
    float x = 0, y = 0, z = 0;
    if (params.length == 2) {
      x = params[0];
      y = params[1];
      z = 0;
    } else if (params.length == 3) {
      x = params[0];
      y = params[1];
      z = params[2];
    }
    
    inGeo.setMaterial(fillColor, strokeColor, strokeWeight,
                      ambientColor, specularColor, emissiveColor, shininess);
    inGeo.setNormal(normalX, normalY, normalZ);
    inGeo.addPoint(x, y, z, fill, stroke);    
    inGeo.initTessMap(tessGeo);    
    tessellator.tessellatePoints();   
  }
  
  
  protected void tessellateLine() {
    float x1 = 0, y1 = 0, z1 = 0;
    float x2 = 0, y2 = 0, z2 = 0;
    if (params.length == 4) {
      x1 = params[0];
      y1 = params[1];
      x2 = params[2];
      y2 = params[3];     
    } else if (params.length == 6) {
      x1 = params[0];
      y1 = params[1];
      z1 = params[2];
      x2 = params[3];
      y2 = params[4];
      z2 = params[5];      
    }
    
    inGeo.setMaterial(fillColor, strokeColor, strokeWeight,
                      ambientColor, specularColor, emissiveColor, shininess);
    inGeo.setNormal(normalX, normalY, normalZ);
    inGeo.addLine(x1, y1, z1,
                  x2, y2, z2,
                  fill, stroke);    
    inGeo.initTessMap(tessGeo);    
    tessellator.tessellateLines();  
  }
  
  
  protected void tessellateTriangle() {
    float x1 = 0, y1 = 0;
    float x2 = 0, y2 = 0;
    float x3 = 0, y3 = 0;
    if (params.length == 6) {
      x1 = params[0];
      y1 = params[1];
      x2 = params[2];
      y2 = params[3];
      x3 = params[4];
      y3 = params[5];       
    }

    inGeo.setMaterial(fillColor, strokeColor, strokeWeight,
                      ambientColor, specularColor, emissiveColor, shininess);  
    inGeo.setNormal(normalX, normalY, normalZ);
    inGeo.addTriangle(x1, y1, 0,
                      x2, y2, 0,
                      x3, y3, 0,
                      fill, stroke);    
    inGeo.initTessMap(tessGeo);    
    tessellator.tessellateTriangles();    
  }
  
  
  protected void tessellateQuad() {
    float x1 = 0, y1 = 0;
    float x2 = 0, y2 = 0;
    float x3 = 0, y3 = 0;
    float x4 = 0, y4 = 0;    
    if (params.length == 8) {
      x1 = params[0];
      y1 = params[1];
      x2 = params[2];
      y2 = params[3];
      x3 = params[4];
      y3 = params[5];      
      x4 = params[6];
      y4 = params[7];            
    }

    inGeo.setMaterial(fillColor, strokeColor, strokeWeight,
                      ambientColor, specularColor, emissiveColor, shininess);
    inGeo.setNormal(normalX, normalY, normalZ);
    inGeo.addQuad(x1, y1, 0,
               x2, y2, 0,
               x3, y3, 0,
               x4, y4, 0,
               fill, stroke);    
    inGeo.initTessMap(tessGeo);    
    tessellator.tessellateQuads();     
  }  
  
  
  protected void tessellateRect() {
    float a = 0, b = 0, c = 0, d = 0;
    float tl = 0, tr = 0, br = 0, bl = 0;
    boolean rounded = false;
    if (params.length == 4) {
      rounded = false;
      a = params[0];
      b = params[1];
      c = params[2];
      d = params[3];
    } else if (params.length == 5) {
      a = params[0];
      b = params[1];
      c = params[2];
      d = params[3];
      tl = tr = br = bl = params[4]; 
      rounded = true;
    } else if (params.length == 8) {
      a = params[0];
      b = params[1];
      c = params[2];
      d = params[3];
      tl = params[4];
      tr = params[5];
      br = params[6];
      bl = params[7]; 
      rounded = true;
    }

    rectMode = CORNER;
    inGeo.setMaterial(fillColor, strokeColor, strokeWeight,
                      ambientColor, specularColor, emissiveColor, shininess);
    inGeo.setNormal(normalX, normalY, normalZ);
    if (rounded) {
      inGeo.addRect(a, b, c, d,
                    tl, tr, br, bl,
                    fill, stroke, bezierDetail, rectMode);       
      inGeo.initTessMap(tessGeo);      
      tessellator.tessellatePolygon(false, true, true);      
    } else {
      inGeo.addRect(a, b, c, d,
                   fill, stroke, rectMode);    
      inGeo.initTessMap(tessGeo);
      tessellator.tessellateQuads();      
    }   
  }
  
  
  protected void tessellateEllipse() {
    float a = 0, b = 0, c = 0, d = 0;
    if (params.length == 4) {
      a = params[0];
      b = params[1];
      c = params[2];
      d = params[3];      
    }
    
    ellipseMode = CORNER;
    inGeo.setMaterial(fillColor, strokeColor, strokeWeight,
                      ambientColor, specularColor, emissiveColor, shininess);
    inGeo.setNormal(normalX, normalY, normalZ);    
    inGeo.addEllipse(a, b, c, d, fill, stroke, ellipseMode);
    inGeo.initTessMap(tessGeo);
    tessellator.tessellateTriangleFan(); 
  }
  
  
  protected void tessellateArc() {
    float a = 0, b = 0, c = 0, d = 0;
    float start = 0, stop = 0;
    if (params.length == 6) {
      a = params[0];
      b = params[1];
      c = params[2];
      d = params[3];      
      start = params[4];
      stop = params[5];      
    }    
    
    ellipseMode = CORNER;
    inGeo.setMaterial(fillColor, strokeColor, strokeWeight,
                      ambientColor, specularColor, emissiveColor, shininess);
    inGeo.setNormal(normalX, normalY, normalZ);
    inGeo.addArc(a, b, c, d, start, stop, fill, stroke, ellipseMode);
    inGeo.initTessMap(tessGeo);
    tessellator.tessellateTriangleFan();    
  }
  
  
  protected void tessellateBox() {
    float w = 0, h = 0, d = 0;
    if (params.length == 1) {
      w = h = d = params[0];  
    } else if (params.length == 3) {
      w = params[0];
      h = params[1];
      d = params[2];
    }
        
    inGeo.setMaterial(fillColor, strokeColor, strokeWeight,
                      ambientColor, specularColor, emissiveColor, shininess);    
    inGeo.addBox(w, h, d, fill, stroke);   
    inGeo.initTessMap(tessGeo);    
    tessellator.tessellateQuads();     
  }
  
  
  protected void tessellateSphere() {
    // Getting sphere detail from renderer. Is this correct?
    int nu = pg.sphereDetailU;
    int nv = pg.sphereDetailV;
    float r = 0;
    if (params.length == 1) {
      r = params[0];
    }
    
    inGeo.setMaterial(fillColor, strokeColor, strokeWeight,
                      ambientColor, specularColor, emissiveColor, shininess); 
    int[] indices = inGeo.addSphere(r, nu, nv, fill, stroke);   
    inGeo.initTessMap(tessGeo);
    tessellator.tessellateTriangles(indices);               
  }
  
  
  protected void tessellatePath() {
    if (vertices == null) return;

    inGeo.setMaterial(fillColor, strokeColor, strokeWeight,
                      ambientColor, specularColor, emissiveColor, shininess);
    
    boolean insideContour = false;

    if (vertexCodeCount == 0) {  // each point is a simple vertex
      if (vertices[0].length == 2) {  // tesellating 2D vertices
        for (int i = 0; i < vertexCount; i++) {
          inGeo.addVertex(vertices[i][X], vertices[i][Y], VERTEX);
        }
      } else {  // drawing 3D vertices
        for (int i = 0; i < vertexCount; i++) {
          inGeo.addVertex(vertices[i][X], vertices[i][Y], vertices[i][Z], VERTEX);
        }
      }
    } else {  // coded set of vertices
      int index = 0;
      int code = VERTEX;
      
      if (vertices[0].length == 2) {  // tessellating a 2D path

        for (int j = 0; j < vertexCodeCount; j++) {
          switch (vertexCodes[j]) {

          case VERTEX:
            inGeo.addVertex(vertices[index][X], vertices[index][Y], code);
            code = VERTEX; 
            index++;
            break;

          case QUAD_BEZIER_VERTEX:
            inGeo.addQuadraticVertex(vertices[index+0][X], vertices[index+0][Y], 0, 
                                     vertices[index+1][X], vertices[index+1][Y], 0,
                                     fill, stroke, bezierDetail, code);
            code = VERTEX;
            index += 2;
            break;

          case BEZIER_VERTEX:
            inGeo.addBezierVertex(vertices[index+0][X], vertices[index+0][Y], 0,
                                  vertices[index+1][X], vertices[index+1][Y], 0,
                                  vertices[index+2][X], vertices[index+2][Y], 0,
                                  fill, stroke, bezierDetail, code);
            code = VERTEX;
            index += 3;
            break;

          case CURVE_VERTEX:
            inGeo.addCurveVertex(vertices[index][X], vertices[index][Y], 0,
                                 fill, stroke, curveDetail, code);
            code = VERTEX;
            index++;

          case BREAK:
            if (insideContour) {
              code = BREAK;
            }            
            insideContour = true;
          }
        }
      } else {  // tessellating a 3D path
        for (int j = 0; j < vertexCodeCount; j++) {
          switch (vertexCodes[j]) {

          case VERTEX:
            inGeo.addVertex(vertices[index][X], vertices[index][Y], vertices[index][Z], code);
            code = VERTEX;
            index++;
            break;

          case QUAD_BEZIER_VERTEX:
            inGeo.addQuadraticVertex(vertices[index+0][X], vertices[index+0][Y], vertices[index+0][Z],
                                     vertices[index+1][X], vertices[index+1][Y], vertices[index+0][Z],
                                     fill, stroke, bezierDetail, code);
            code = VERTEX;
            index += 2;
            break;


          case BEZIER_VERTEX:
            inGeo.addBezierVertex(vertices[index+0][X], vertices[index+0][Y], vertices[index+0][Z],
                                  vertices[index+1][X], vertices[index+1][Y], vertices[index+1][Z],
                                  vertices[index+2][X], vertices[index+2][Y], vertices[index+2][Z],
                                  fill, stroke, bezierDetail, code);
            code = VERTEX;
            index += 3;
            break;

          case CURVE_VERTEX:
            inGeo.addCurveVertex(vertices[index][X], vertices[index][Y], vertices[index][Z],
                                 fill, stroke, curveDetail, code);
            code = VERTEX;
            index++;

          case BREAK:
            if (insideContour) {
              code = BREAK;
            }            
            insideContour = true;
          }
        }
      }
    }
    
    if (stroke) inGeo.addPolygonEdges(isClosed);
    inGeo.initTessMap(tessGeo);  
    tessellator.tessellatePolygon(false, isClosed, true);    
  }  
  
  
  ///////////////////////////////////////////////////////////  
  
  //
  
  // Aggregation
  
  
  protected void aggregate() {
    if (root == this && parent == null) {
      // Initializing auxiliary variables in root node
      // needed for aggregation.
      polyIndexOffset = 0;
      polyVertexOffset = 0;
      polyVertexAbs = 0;      
      polyVertexRel = 0;      
      
      lineIndexOffset = 0;
      lineVertexOffset = 0;
      lineVertexAbs = 0;      
      lineVertexRel = 0;  
      
      pointIndexOffset = 0;
      pointVertexOffset = 0;
      pointVertexAbs = 0;      
      pointVertexRel = 0;
      
      // Recursive aggregation.
      aggregateImpl();
    }
  }
  
  
  // This method is very important, as it is responsible of generating the correct 
  // vertex and index offsets for each level of the shape hierarchy.
  // This is the core of the recursive algorithm that calculates the indices
  // for the vertices accumulated in a single VBO.
  // Basically, the algorithm traverses all the shapes in the hierarchy and 
  // updates the index cache for each child shape holding geometry (those being 
  // the leaf nodes in the hierarchy tree), and creates index caches for the 
  // group shapes so that the draw() method can be called from any shape in the
  // hierarchy and the correct piece of geometry will be rendered.  
  //
  // For example, in the following hierarchy:
  //
  //                     ROOT GROUP                       
  //                         |
  //       /-----------------0-----------------\ 
  //       |                                   |
  //  CHILD GROUP 0                       CHILD GROUP 1                     
  //       |                                   |
  //       |                   /---------------0-----------------\              
  //       |                   |               |                 |
  //   GEO SHAPE 0         GEO SHAPE 0     GEO SHAPE 1       GEO SHAPE 2
  //   4 vertices          5 vertices      6 vertices        3 vertices
  //
  // calling draw() from the root group should result in all the 
  // vertices (4 + 5 + 6 + 3 = 18) being rendered, while calling
  // draw() from either child groups 0 or 1 should result in the first
  // 4 vertices or the last 14 vertices being rendered, respectively.
  protected void aggregateImpl() {
    if (family == GROUP) {
      // Recursively aggregating the child shapes.
      hasPolys = false;
      hasLines = false;
      hasPoints = false;      
      for (int i = 0; i < childCount; i++) {        
        PShapeOpenGL child = (PShapeOpenGL) children[i];
        child.aggregateImpl();
        hasPolys |= child.hasPolys;
        hasLines |= child.hasLines; 
        hasPoints |= child.hasPoints; 
      }    
    } else { // LEAF SHAPE (family either GEOMETRY, PATH or PRIMITIVE)
      hasPolys = -1 < firstPolyIndexCache && -1 < lastPolyIndexCache;
      hasLines = -1 < firstLineIndexCache && -1 < lastLineIndexCache; 
      hasPoints = -1 < firstPointIndexCache && -1 < lastPointIndexCache; 
    }
    
    if (hasPolys) updatePolyIndexCache();
    if (is3D()) {
      if (hasLines) updateLineIndexCache();            
      if (hasPoints) updatePointIndexCache();
    }
    
    if (matrix != null) {
      // Some geometric transformations were applied on
      // this shape before tessellation, so they are applied now.
      //applyMatrixImpl(matrix);
      if (hasPolys) tessGeo.applyMatrixOnPolyGeometry(matrix, firstPolyVertex, lastPolyVertex);
      if (is3D()) {
        if (hasLines) tessGeo.applyMatrixOnLineGeometry(matrix, firstLineVertex, lastLineVertex);
        if (hasPoints) tessGeo.applyMatrixOnPointGeometry(matrix, firstPointVertex, lastPointVertex);
      }
    }  
  }
  
  
  // Updates the index cache for the range that corresponds to this shape.
  protected void updatePolyIndexCache() {
    IndexCache cache = tessGeo.polyIndexCache;
    if (family == GROUP) {
      // Updates the index cache to include the elements corresponding to 
      // a group shape, using the cache entries of the child shapes. The 
      // index cache has a pyramidal structure where the base is formed
      // by the entries corresponding to the leaf (geometry) shapes, and 
      // each subsequent level is determined by the higher-level group shapes
      // The index pyramid is flattened into arrays in order to use simple
      // data structures, so each shape needs to store the positions in the
      // cache that corresponds to itself.
      
      // The index ranges of the child shapes that share the vertex offset
      // are unified into a single range in the parent level.      

      firstPolyIndexCache = lastPolyIndexCache = -1;
      int gindex = -1; 
          
      for (int i = 0; i < childCount; i++) {        
        PShapeOpenGL child = (PShapeOpenGL) children[i];
        
        int first = child.firstPolyIndexCache;
        int count = -1 < first ? child.lastPolyIndexCache - first + 1 : -1;        
        for (int n = first; n < first + count; n++) {        
          if (gindex == -1) {
            gindex = cache.addNew(n);
            firstPolyIndexCache = gindex;
          } else {
            if (cache.vertexOffset[gindex] == cache.vertexOffset[n]) {
              // When the vertex offsets are the same, this means that the 
              // current index range in the group shape can be extended to 
              // include the index range in the current child shape.
              // This is a result of how the indices are updated for the
              // leaf shapes.
              cache.incCounts(gindex, cache.indexCount[n], cache.vertexCount[n]);
            } else {
              gindex = cache.addNew(n);
            }
          }
        }
      }
      lastPolyIndexCache = gindex;
      
      if (-1 < firstPolyIndexCache && -1 < lastPolyIndexCache) {
        firstPolyVertex = cache.vertexOffset[firstPolyIndexCache]; 
        lastPolyVertex = cache.vertexOffset[lastPolyIndexCache] + cache.vertexCount[lastPolyIndexCache] - 1;
      }      
    } else {
      // The index cache is updated in order to reflect the fact that all 
      // the vertices will be stored in a single VBO in the root shape.
      // This update works as follows (the methodology is the same for
      // poly, line and point): the VertexAbs variable in the root shape 
      // stores the index of the last vertex up to this shape (plus one)
      // without taking into consideration the MAX_VERTEX_INDEX limit, so
      // it effectively runs over the entire range.
      // VertexRel, on the other hand, is reset every time the limit is
      // exceeded, therefore creating the start of a new index group in the
      // root shape. When this happens, the indices in the child shape need
      // to be restarted as well to reflect the new index offset.
            
      firstPolyVertex = lastPolyVertex = cache.vertexOffset[firstPolyIndexCache];
      for (int n = firstPolyIndexCache; n <= lastPolyIndexCache; n++) {
        int ioffset = cache.indexOffset[n];
        int icount = cache.indexCount[n];
        int vcount = cache.vertexCount[n];

        if (PGL.MAX_VERTEX_INDEX1 <= root.polyVertexRel + vcount || // Too many vertices already signal the start of a new cache... 
            (is2D() && startStrokeCache(n))) { // ... or, in 2D, the beginning of line or points.           
          root.polyVertexRel = 0;
          root.polyVertexOffset = root.polyVertexAbs;
          cache.indexOffset[n] = root.polyIndexOffset;          
          if (is2D()) firstStrokeVertex(n);
        } else tessGeo.incPolyIndices(ioffset, ioffset + icount - 1, root.polyVertexRel);
        cache.vertexOffset[n] = root.polyVertexOffset;
                  
        root.polyIndexOffset += icount;          
        root.polyVertexAbs += vcount;
        root.polyVertexRel += vcount;        
        lastPolyVertex += vcount;
        if (is2D()) incStrokeVertex(vcount); 
      }
      lastPolyVertex--;
      if (is2D())incStrokeVertex(-1);    
    }
  }
  
  
  protected boolean startStrokeCache(int n) {  
    return n == firstLineIndexCache || n == firstPointIndexCache;
  }

  
  protected void firstStrokeVertex(int n) {
    if (n == firstLineIndexCache) {
      firstLineVertex = lastLineVertex = root.polyVertexOffset;
    }
    if (n == firstPointIndexCache) {
      firstPointVertex = lastPointVertex = root.polyVertexOffset;
    }    
  }
  
  
  protected void incStrokeVertex(int vcount) {
    if (-1 < lastLineVertex) {
      lastLineVertex += vcount;
    }          
    if (-1 < lastPointVertex) {
      lastPointVertex += vcount; 
    }
  }  
  
  
  protected void updateLineIndexCache() {
    IndexCache cache = tessGeo.lineIndexCache;
    if (family == GROUP) {
      firstLineIndexCache = lastLineIndexCache = -1;
      int gindex = -1; 
          
      for (int i = 0; i < childCount; i++) {        
        PShapeOpenGL child = (PShapeOpenGL) children[i];
        
        int first = child.firstLineIndexCache;
        int count = -1 < first ? child.lastLineIndexCache - first + 1 : -1;        
        for (int n = first; n < first + count; n++) {        
          if (gindex == -1) {
            gindex = cache.addNew(n);
            firstLineIndexCache = gindex;
          } else {
            if (cache.vertexOffset[gindex] == cache.vertexOffset[n]) {
              cache.incCounts(gindex, cache.indexCount[n], cache.vertexCount[n]);
            } else {
              gindex = cache.addNew(n);
            }
          }
        }
      }
      lastLineIndexCache = gindex;
      
      if (-1 < firstLineIndexCache && -1 < lastLineIndexCache) {
        firstLineVertex = cache.vertexOffset[firstLineIndexCache]; 
        lastLineVertex = cache.vertexOffset[lastLineIndexCache] + cache.vertexCount[lastLineIndexCache] - 1;
      }      
    } else {      
      firstLineVertex = lastLineVertex = cache.vertexOffset[firstLineIndexCache];
      for (int n = firstLineIndexCache; n <= lastLineIndexCache; n++) {
        int ioffset = cache.indexOffset[n];
        int icount = cache.indexCount[n];
        int vcount = cache.vertexCount[n];

        if (PGL.MAX_VERTEX_INDEX1 <= root.lineVertexRel + vcount) {            
          root.lineVertexRel = 0;
          root.lineVertexOffset = root.lineVertexAbs;
          cache.indexOffset[n] = root.lineIndexOffset;
        } else tessGeo.incLineIndices(ioffset, ioffset + icount - 1, root.lineVertexRel);
        cache.vertexOffset[n] = root.lineVertexOffset;
                  
        root.lineIndexOffset += icount;          
        root.lineVertexAbs += vcount;
        root.lineVertexRel += vcount;        
        lastLineVertex += vcount;
      }
      lastLineVertex--;       
    }
  }
  
  
  protected void updatePointIndexCache() {
    IndexCache cache = tessGeo.pointIndexCache;
    if (family == GROUP) {      
      firstPointIndexCache = lastPointIndexCache = -1;
      int gindex = -1; 
          
      for (int i = 0; i < childCount; i++) {        
        PShapeOpenGL child = (PShapeOpenGL) children[i];
        
        int first = child.firstPointIndexCache;
        int count = -1 < first ? child.lastPointIndexCache - first + 1 : -1;        
        for (int n = first; n < first + count; n++) {        
          if (gindex == -1) {
            gindex = cache.addNew(n);
            firstPointIndexCache = gindex;
          } else {
            if (cache.vertexOffset[gindex] == cache.vertexOffset[n]) {
              // When the vertex offsets are the same, this means that the 
              // current index range in the group shape can be extended to 
              // include either the index range in the current child shape.
              // This is a result of how the indices are updated for the
              // leaf shapes in aggregateImpl().
              cache.incCounts(gindex, cache.indexCount[n], cache.vertexCount[n]);
            } else {
              gindex = cache.addNew(n);
            }
          }
        }
      }
      lastPointIndexCache = gindex;
      
      if (-1 < firstPointIndexCache && -1 < lastPointIndexCache) {
        firstPointVertex = cache.vertexOffset[firstPointIndexCache]; 
        lastPointVertex = cache.vertexOffset[lastPointIndexCache] + cache.vertexCount[lastPointIndexCache] - 1;
      }       
    } else {
      firstPointVertex = lastPointVertex = cache.vertexOffset[firstPointIndexCache];
      for (int n = firstPointIndexCache; n <= lastPointIndexCache; n++) {
        int ioffset = cache.indexOffset[n];
        int icount = cache.indexCount[n];
        int vcount = cache.vertexCount[n];

        if (PGL.MAX_VERTEX_INDEX1 <= root.pointVertexRel + vcount) {            
          root.pointVertexRel = 0;
          root.pointVertexOffset = root.pointVertexAbs;
          cache.indexOffset[n] = root.pointIndexOffset;
        } else tessGeo.incPointIndices(ioffset, ioffset + icount - 1, root.pointVertexRel);
        cache.vertexOffset[n] = root.pointVertexOffset;
                  
        root.pointIndexOffset += icount;          
        root.pointVertexAbs += vcount;
        root.pointVertexRel += vcount;        
        lastPointVertex += vcount;
      }
      lastPointVertex--;      
    }
  }
  
  
  ///////////////////////////////////////////////////////////  
  
  //
  
  //  Buffer initialization
  
  
  protected void initBuffers() {
    if (needBufferInit) {
      context = pgl.getCurrentContext();
      
      if (0 < tessGeo.polyVertexCount && 0 < tessGeo.polyIndexCount) {   
        initPolyBuffers();
      }      
      
      if (0 < tessGeo.lineVertexCount && 0 < tessGeo.lineIndexCount) {   
        initLineBuffers();
      }      
      
      if (0 < tessGeo.pointVertexCount && 0 < tessGeo.pointIndexCount) {   
        initPointBuffers();
      }
      
      needBufferInit = false;
    }
  }
  
  
  protected void initPolyBuffers() {    
    int size = tessGeo.polyVertexCount;
    int sizef = size * PGL.SIZEOF_FLOAT;
    int sizei = size * PGL.SIZEOF_INT;
        
    glPolyVertexBufferID = pg.createVertexBufferObject(context.code());  
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glPolyVertexBufferID);
    pgl.glBufferData(PGL.GL_ARRAY_BUFFER, 4 * sizef, FloatBuffer.wrap(tessGeo.polyVertices, 0, 4 * size), PGL.GL_STATIC_DRAW);    
        
    glPolyColorBufferID = pg.createVertexBufferObject(context.code());
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glPolyColorBufferID);
    pgl.glBufferData(PGL.GL_ARRAY_BUFFER, sizei, IntBuffer.wrap(tessGeo.polyColors, 0, size), PGL.GL_STATIC_DRAW);    
    
    glPolyNormalBufferID = pg.createVertexBufferObject(context.code());
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glPolyNormalBufferID);
    pgl.glBufferData(PGL.GL_ARRAY_BUFFER, 3 * sizef, FloatBuffer.wrap(tessGeo.polyNormals, 0, 3 * size), PGL.GL_STATIC_DRAW);     
    
    glPolyTexcoordBufferID = pg.createVertexBufferObject(context.code());
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glPolyTexcoordBufferID);
    pgl.glBufferData(PGL.GL_ARRAY_BUFFER, 2 * sizef, FloatBuffer.wrap(tessGeo.polyTexcoords, 0, 2 * size), PGL.GL_STATIC_DRAW);      
    
    glPolyAmbientBufferID = pg.createVertexBufferObject(context.code());  
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glPolyAmbientBufferID);
    pgl.glBufferData(PGL.GL_ARRAY_BUFFER, sizei, IntBuffer.wrap(tessGeo.polyAmbient, 0, size), PGL.GL_STATIC_DRAW);
    
    glPolySpecularBufferID = pg.createVertexBufferObject(context.code());
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glPolySpecularBufferID);
    pgl.glBufferData(PGL.GL_ARRAY_BUFFER, sizei, IntBuffer.wrap(tessGeo.polySpecular, 0, size), PGL.GL_STATIC_DRAW);    
    
    glPolyEmissiveBufferID = pg.createVertexBufferObject(context.code());
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glPolyEmissiveBufferID);
    pgl.glBufferData(PGL.GL_ARRAY_BUFFER, sizei, IntBuffer.wrap(tessGeo.polyEmissive, 0, size), PGL.GL_STATIC_DRAW);
    
    glPolyShininessBufferID = pg.createVertexBufferObject(context.code());
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glPolyShininessBufferID);
    pgl.glBufferData(PGL.GL_ARRAY_BUFFER, sizef, FloatBuffer.wrap(tessGeo.polyShininess, 0, size), PGL.GL_STATIC_DRAW);
            
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, 0);
        
    glPolyIndexBufferID = pg.createVertexBufferObject(context.code());  
    pgl.glBindBuffer(PGL.GL_ELEMENT_ARRAY_BUFFER, glPolyIndexBufferID);
    pgl.glBufferData(PGL.GL_ELEMENT_ARRAY_BUFFER, tessGeo.polyIndexCount * PGL.SIZEOF_INDEX,
                     ShortBuffer.wrap(tessGeo.polyIndices, 0, tessGeo.polyIndexCount), PGL.GL_STATIC_DRAW);
    
    pgl.glBindBuffer(PGL.GL_ELEMENT_ARRAY_BUFFER, 0);  
  }  
  
  
  protected void initLineBuffers() {
    int size = tessGeo.lineVertexCount;
    int sizef = size * PGL.SIZEOF_FLOAT;
    int sizei = size * PGL.SIZEOF_INT;    
    
    glLineVertexBufferID = pg.createVertexBufferObject(context.code());    
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glLineVertexBufferID);      
    pgl.glBufferData(PGL.GL_ARRAY_BUFFER, 4 * sizef, FloatBuffer.wrap(tessGeo.lineVertices, 0, 4 * size), PGL.GL_STATIC_DRAW);
    
    glLineColorBufferID = pg.createVertexBufferObject(context.code());
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glLineColorBufferID);
    pgl.glBufferData(PGL.GL_ARRAY_BUFFER, sizei, IntBuffer.wrap(tessGeo.lineColors, 0, size), PGL.GL_STATIC_DRAW);

    glLineAttribBufferID = pg.createVertexBufferObject(context.code());
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glLineAttribBufferID);
    pgl.glBufferData(PGL.GL_ARRAY_BUFFER, 4 * sizef, FloatBuffer.wrap(tessGeo.lineAttribs, 0, 4 * size), PGL.GL_STATIC_DRAW);
    
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, 0);    
    
    glLineIndexBufferID = pg.createVertexBufferObject(context.code());    
    pgl.glBindBuffer(PGL.GL_ELEMENT_ARRAY_BUFFER, glLineIndexBufferID);
    pgl.glBufferData(PGL.GL_ELEMENT_ARRAY_BUFFER, tessGeo.lineIndexCount * PGL.SIZEOF_INDEX,
                     ShortBuffer.wrap(tessGeo.lineIndices, 0, tessGeo.lineIndexCount), PGL.GL_STATIC_DRAW);

    pgl.glBindBuffer(PGL.GL_ELEMENT_ARRAY_BUFFER, 0);
  }
  
  
  protected void initPointBuffers() {
    int size = tessGeo.pointVertexCount;
    int sizef = size * PGL.SIZEOF_FLOAT;
    int sizei = size * PGL.SIZEOF_INT;
    
    glPointVertexBufferID = pg.createVertexBufferObject(context.code());
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glPointVertexBufferID);
    pgl.glBufferData(PGL.GL_ARRAY_BUFFER, 4 * sizef, FloatBuffer.wrap(tessGeo.pointVertices, 0, 4 * size), PGL.GL_STATIC_DRAW);

    glPointColorBufferID = pg.createVertexBufferObject(context.code());
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glPointColorBufferID);
    pgl.glBufferData(PGL.GL_ARRAY_BUFFER, sizei, IntBuffer.wrap(tessGeo.pointColors, 0, size), PGL.GL_STATIC_DRAW);
    
    glPointAttribBufferID = pg.createVertexBufferObject(context.code());
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glPointAttribBufferID);
    pgl.glBufferData(PGL.GL_ARRAY_BUFFER, 2 * sizef, FloatBuffer.wrap(tessGeo.pointAttribs, 0, 2 * size), PGL.GL_STATIC_DRAW);
      
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, 0);     
        
    glPointIndexBufferID = pg.createVertexBufferObject(context.code());
    pgl.glBindBuffer(PGL.GL_ELEMENT_ARRAY_BUFFER, glPointIndexBufferID);
    pgl.glBufferData(PGL.GL_ELEMENT_ARRAY_BUFFER, tessGeo.pointIndexCount * PGL.SIZEOF_INDEX,
                     ShortBuffer.wrap(tessGeo.pointIndices, 0, tessGeo.pointIndexCount), PGL.GL_STATIC_DRAW);
    
    pgl.glBindBuffer(PGL.GL_ELEMENT_ARRAY_BUFFER, 0);
  }  

  
  protected boolean contextIsOutdated() {
    boolean outdated = !pgl.contextIsCurrent(context);
    if (outdated) {
      // Removing the VBOs from the renderer's list so they
      // doesn't get deleted by OpenGL. The VBOs were already 
      // automatically disposed when the old context was 
      // destroyed.
      pg.removeVertexBufferObject(glPolyVertexBufferID, context.code());
      pg.removeVertexBufferObject(glPolyColorBufferID, context.code());
      pg.removeVertexBufferObject(glPolyNormalBufferID, context.code());
      pg.removeVertexBufferObject(glPolyTexcoordBufferID, context.code());
      pg.removeVertexBufferObject(glPolyAmbientBufferID, context.code());
      pg.removeVertexBufferObject(glPolySpecularBufferID, context.code());
      pg.removeVertexBufferObject(glPolyEmissiveBufferID, context.code());
      pg.removeVertexBufferObject(glPolyShininessBufferID, context.code());     
      pg.removeVertexBufferObject(glPolyIndexBufferID, context.code());
      
      pg.removeVertexBufferObject(glLineVertexBufferID, context.code());
      pg.removeVertexBufferObject(glLineColorBufferID, context.code());
      pg.removeVertexBufferObject(glLineAttribBufferID, context.code());
      pg.removeVertexBufferObject(glLineIndexBufferID, context.code());
      
      pg.removeVertexBufferObject(glPointVertexBufferID, context.code());
      pg.removeVertexBufferObject(glPointColorBufferID, context.code());
      pg.removeVertexBufferObject(glPointAttribBufferID, context.code());
      pg.removeVertexBufferObject(glPointIndexBufferID, context.code());
      
      // The OpenGL resources have been already deleted
      // when the context changed. We only need to zero 
      // them to avoid deleting them again when the GC
      // runs the finalizers of the disposed object.
      glPolyVertexBufferID = 0;
      glPolyColorBufferID = 0;
      glPolyNormalBufferID = 0;
      glPolyTexcoordBufferID = 0;
      glPolyAmbientBufferID = 0;
      glPolySpecularBufferID = 0;
      glPolyEmissiveBufferID = 0;
      glPolyShininessBufferID = 0;     
      glPolyIndexBufferID = 0;
      
      glLineVertexBufferID = 0;
      glLineColorBufferID = 0;
      glLineAttribBufferID = 0;
      glLineIndexBufferID = 0;
      
      glPointVertexBufferID = 0;
      glPointColorBufferID = 0;
      glPointAttribBufferID = 0;
      glPointIndexBufferID = 0;
    }
    return outdated;
  }
  
  
  ///////////////////////////////////////////////////////////  
  
  //
  
  // Deletion methods

  
  protected void release() {
    deletePolyBuffers();
    deleteLineBuffers();
    deletePointBuffers();
  }  
  
  
  protected void deletePolyBuffers() {
    if (glPolyVertexBufferID != 0) {    
      pg.deleteVertexBufferObject(glPolyVertexBufferID, context.code());   
      glPolyVertexBufferID = 0;
    }    
    
    if (glPolyColorBufferID != 0) {    
      pg.deleteVertexBufferObject(glPolyColorBufferID, context.code());   
      glPolyColorBufferID = 0;
    }    

    if (glPolyNormalBufferID != 0) {    
      pg.deleteVertexBufferObject(glPolyNormalBufferID, context.code());   
      glPolyNormalBufferID = 0;
    }     

    if (glPolyTexcoordBufferID != 0) {    
      pg.deleteVertexBufferObject(glPolyTexcoordBufferID, context.code());  
      glPolyTexcoordBufferID = 0;
    }    

    if (glPolyAmbientBufferID != 0) {    
      pg.deleteVertexBufferObject(glPolyAmbientBufferID, context.code());   
      glPolyAmbientBufferID = 0;
    }    
    
    if (glPolySpecularBufferID != 0) {    
      pg.deleteVertexBufferObject(glPolySpecularBufferID, context.code());   
      glPolySpecularBufferID = 0;
    }    

    if (glPolyEmissiveBufferID != 0) {    
      pg.deleteVertexBufferObject(glPolyEmissiveBufferID, context.code());   
      glPolyEmissiveBufferID = 0;
    }     

    if (glPolyShininessBufferID != 0) {    
      pg.deleteVertexBufferObject(glPolyShininessBufferID, context.code());   
      glPolyShininessBufferID = 0;
    }        
    
    if (glPolyIndexBufferID != 0) {    
      pg.deleteVertexBufferObject(glPolyIndexBufferID, context.code());  
      glPolyIndexBufferID = 0;
    }   
  }
  
  
  protected void deleteLineBuffers() {
    if (glLineVertexBufferID != 0) {    
      pg.deleteVertexBufferObject(glLineVertexBufferID, context.code());   
      glLineVertexBufferID = 0;
    }    
    
    if (glLineColorBufferID != 0) {    
      pg.deleteVertexBufferObject(glLineColorBufferID, context.code());   
      glLineColorBufferID = 0;
    }    

    if (glLineAttribBufferID != 0) {    
      pg.deleteVertexBufferObject(glLineAttribBufferID, context.code());  
      glLineAttribBufferID = 0;
    }    
    
    if (glLineIndexBufferID != 0) {    
      pg.deleteVertexBufferObject(glLineIndexBufferID, context.code());   
      glLineIndexBufferID = 0;
    }  
  }  
  
  
  protected void deletePointBuffers() {
    if (glPointVertexBufferID != 0) {    
      pg.deleteVertexBufferObject(glPointVertexBufferID, context.code());   
      glPointVertexBufferID = 0;
    }    
    
    if (glPointColorBufferID != 0) {    
      pg.deleteVertexBufferObject(glPointColorBufferID, context.code());   
      glPointColorBufferID = 0;
    }    

    if (glPointAttribBufferID != 0) {    
      pg.deleteVertexBufferObject(glPointAttribBufferID, context.code());   
      glPointAttribBufferID = 0;
    }    
    
    if (glPointIndexBufferID != 0) {    
      pg.deleteVertexBufferObject(glPointIndexBufferID, context.code());   
      glPointIndexBufferID = 0;
    }  
  }
  
  
  ///////////////////////////////////////////////////////////  
  
  //
  
  //  Geometry update  
  
  
  protected void updateGeometry() {
    root.initBuffers();
    if (root.modified) {      
      root.updateGeometryImpl();      
    }
  }
  
  
  protected void updateGeometryImpl() {
    if (modifiedPolyVertices) {
      int offset = firstModifiedPolyVertex;
      int size = lastModifiedPolyVertex - offset + 1;        
      copyPolyVertices(offset, size);
      modifiedPolyVertices = false;
      firstModifiedPolyVertex = PConstants.MAX_INT;      
      lastModifiedPolyVertex = PConstants.MIN_INT;
    }    
    if (modifiedPolyColors) {
      int offset = firstModifiedPolyColor;
      int size = lastModifiedPolyColor - offset + 1;        
      copyPolyColors(offset, size);       
      modifiedPolyColors = false;
      firstModifiedPolyColor = PConstants.MAX_INT;
      lastModifiedPolyColor = PConstants.MIN_INT;
    }
    if (modifiedPolyNormals) {      
      int offset = firstModifiedPolyNormal;
      int size = lastModifiedPolyNormal - offset + 1;        
      copyPolyNormals(offset, size);      
      modifiedPolyNormals = false;      
      firstModifiedPolyNormal = PConstants.MAX_INT;
      lastModifiedPolyNormal = PConstants.MIN_INT;    
    }
    if (modifiedPolyTexcoords) {      
      int offset = firstModifiedPolyTexcoord;
      int size = lastModifiedPolyTexcoord - offset + 1;        
      copyPolyTexcoords(offset, size);         
      modifiedPolyTexcoords = false;
      firstModifiedPolyTexcoord = PConstants.MAX_INT;
      lastModifiedPolyTexcoord = PConstants.MIN_INT;      
    }
    if (modifiedPolyAmbient) {     
      int offset = firstModifiedPolyAmbient;
      int size = lastModifiedPolyAmbient - offset + 1;        
      copyPolyAmbient(offset, size);      
      modifiedPolyAmbient = false;
      firstModifiedPolyAmbient = PConstants.MAX_INT;
      lastModifiedPolyAmbient = PConstants.MIN_INT;
    }
    if (modifiedPolySpecular) {
      int offset = firstModifiedPolySpecular;
      int size = lastModifiedPolySpecular - offset + 1;        
      copyPolySpecular(offset, size);      
      modifiedPolySpecular = false;
      firstModifiedPolySpecular = PConstants.MAX_INT;
      lastModifiedPolySpecular = PConstants.MIN_INT;
    }
    if (modifiedPolyEmissive) {      
      int offset = firstModifiedPolyEmissive;
      int size = lastModifiedPolyEmissive - offset + 1;        
      copyPolyEmissive(offset, size);            
      modifiedPolyEmissive = false;
      firstModifiedPolyEmissive = PConstants.MAX_INT;
      lastModifiedPolyEmissive = PConstants.MIN_INT;       
    }
    if (modifiedPolyShininess) {
      int offset = firstModifiedPolyShininess;
      int size = lastModifiedPolyShininess - offset + 1;        
      copyPolyShininess(offset, size);      
      modifiedPolyShininess = false;
      firstModifiedPolyShininess = PConstants.MAX_INT;
      lastModifiedPolyShininess = PConstants.MIN_INT;
    }
    
    if (modifiedLineVertices) {
      int offset = firstModifiedLineVertex;
      int size = lastModifiedLineVertex - offset + 1;        
      copyLineVertices(offset, size);      
      modifiedLineVertices = false;
      firstModifiedLineVertex = PConstants.MAX_INT;
      lastModifiedLineVertex = PConstants.MIN_INT;     
    }
    if (modifiedLineColors) {
      int offset = firstModifiedLineColor;
      int size = lastModifiedLineColor - offset + 1;        
      copyLineColors(offset, size);     
      modifiedLineColors = false;
      firstModifiedLineColor = PConstants.MAX_INT;
      lastModifiedLineColor = PConstants.MIN_INT;      
    }
    if (modifiedLineAttributes) {
      int offset = firstModifiedLineAttribute;
      int size = lastModifiedLineAttribute - offset + 1;        
      copyLineAttributes(offset, size);
      modifiedLineAttributes = false;
      firstModifiedLineAttribute = PConstants.MAX_INT;
      lastModifiedLineAttribute = PConstants.MIN_INT;
    }

    if (modifiedPointVertices) {     
      int offset = firstModifiedPointVertex;
      int size = lastModifiedPointVertex - offset + 1;        
      copyPointVertices(offset, size);      
      modifiedPointVertices = false;
      firstModifiedPointVertex = PConstants.MAX_INT;
      lastModifiedPointVertex = PConstants.MIN_INT;     
    }
    if (modifiedPointColors) {     
      int offset = firstModifiedPointColor;
      int size = lastModifiedPointColor - offset + 1;        
      copyPointColors(offset, size);      
      modifiedPointColors = false;
      firstModifiedPointColor = PConstants.MAX_INT;
      lastModifiedPointColor = PConstants.MIN_INT;       
    }
    if (modifiedPointAttributes) {      
      int offset = firstModifiedPointAttribute;
      int size = lastModifiedPointAttribute - offset + 1;        
      copyPointAttributes(offset, size);      
      modifiedPointAttributes = false;
      firstModifiedPointAttribute = PConstants.MAX_INT;
      lastModifiedPointAttribute = PConstants.MIN_INT;        
    }
    
    modified = false;
  }

  
  protected void copyPolyVertices(int offset, int size) {    
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glPolyVertexBufferID);
    pgl.glBufferSubData(PGL.GL_ARRAY_BUFFER, 4 * offset * PGL.SIZEOF_FLOAT, 4 * size * PGL.SIZEOF_FLOAT, 
                        FloatBuffer.wrap(tessGeo.polyVertices, 0, 4 * size));
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, 0);
  }
  
  
  protected void copyPolyColors(int offset, int size) {    
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glPolyColorBufferID);
    pgl.glBufferSubData(PGL.GL_ARRAY_BUFFER, offset * PGL.SIZEOF_INT, size * PGL.SIZEOF_INT, 
                        IntBuffer.wrap(tessGeo.polyColors, 0, size));     
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, 0);
  }  
  
  
  protected void copyPolyNormals(int offset, int size) {
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glPolyNormalBufferID);
    pgl.glBufferSubData(PGL.GL_ARRAY_BUFFER, 3 * offset * PGL.SIZEOF_FLOAT, 3 * size * PGL.SIZEOF_FLOAT, 
                        FloatBuffer.wrap(tessGeo.polyNormals, 0, 3 * size));    
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, 0);
  }  

  
  protected void copyPolyTexcoords(int offset, int size) {
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glPolyTexcoordBufferID);
    pgl.glBufferSubData(PGL.GL_ARRAY_BUFFER, 2 * offset * PGL.SIZEOF_FLOAT, 2 * size * PGL.SIZEOF_FLOAT, 
                        FloatBuffer.wrap(tessGeo.polyTexcoords, 0, 2 * size));
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, 0);
  }   

  
  protected void copyPolyAmbient(int offset, int size) {
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glPolyAmbientBufferID);
    pgl.glBufferSubData(PGL.GL_ARRAY_BUFFER, offset * PGL.SIZEOF_INT, size * PGL.SIZEOF_INT, 
                        IntBuffer.wrap(tessGeo.polyAmbient, 0, size));
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, 0);    
  }
  
  
  protected void copyPolySpecular(int offset, int size) {
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glPolySpecularBufferID);
    pgl.glBufferSubData(PGL.GL_ARRAY_BUFFER, offset * PGL.SIZEOF_INT, size * PGL.SIZEOF_INT, 
                        IntBuffer.wrap(tessGeo.polySpecular, 0, size));     
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, 0);       
  }

  
  protected void copyPolyEmissive(int offset, int size) {
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glPolyEmissiveBufferID);
    pgl.glBufferSubData(PGL.GL_ARRAY_BUFFER, offset * PGL.SIZEOF_INT, size * PGL.SIZEOF_INT, 
                        IntBuffer.wrap(tessGeo.polyEmissive, 0, size));      
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, 0);    
  }  

  
  protected void copyPolyShininess(int offset, int size) {
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glPolyShininessBufferID);
    pgl.glBufferSubData(PGL.GL_ARRAY_BUFFER, offset * PGL.SIZEOF_FLOAT, size * PGL.SIZEOF_FLOAT, 
                        FloatBuffer.wrap(tessGeo.polyShininess, 0, size));
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, 0);     
  }    
  
  
  protected void copyLineVertices(int offset, int size) {    
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glLineVertexBufferID);
    pgl.glBufferSubData(PGL.GL_ARRAY_BUFFER, 4 * offset * PGL.SIZEOF_FLOAT, 4 * size * PGL.SIZEOF_FLOAT, 
                        FloatBuffer.wrap(tessGeo.lineVertices, 0, 4 * size));
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, 0);
  }     
  
  
  protected void copyLineColors(int offset, int size) {
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glLineColorBufferID);
    pgl.glBufferSubData(PGL.GL_ARRAY_BUFFER, offset * PGL.SIZEOF_INT, size * PGL.SIZEOF_INT, 
                        IntBuffer.wrap(tessGeo.lineColors, 0, size));             
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, 0);
  }
  
  
  protected void copyLineAttributes(int offset, int size) {
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glLineAttribBufferID);
    pgl.glBufferSubData(PGL.GL_ARRAY_BUFFER, 4 * offset * PGL.SIZEOF_FLOAT, 4 * size * PGL.SIZEOF_FLOAT, 
                        FloatBuffer.wrap(tessGeo.lineAttribs, 0, 4 * size));
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, 0);
  }
  

  protected void copyPointVertices(int offset, int size) {    
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glPointVertexBufferID);
    pgl.glBufferSubData(PGL.GL_ARRAY_BUFFER, 4 * offset * PGL.SIZEOF_FLOAT, 4 * size * PGL.SIZEOF_FLOAT, 
                        FloatBuffer.wrap(tessGeo.pointVertices, 0, 4 * size));
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, 0);
  }
    
    
  protected void copyPointColors(int offset, int size) {
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glPointColorBufferID);
    pgl.glBufferSubData(PGL.GL_ARRAY_BUFFER, offset * PGL.SIZEOF_INT, size * PGL.SIZEOF_INT, 
                        IntBuffer.wrap(tessGeo.pointColors, 0, size));
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, 0);
  }
    
  
  protected void copyPointAttributes(int offset, int size) {
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glPointAttribBufferID);
    pgl.glBufferSubData(PGL.GL_ARRAY_BUFFER, 2 * offset * PGL.SIZEOF_FLOAT, 2 * size * PGL.SIZEOF_FLOAT, 
                        FloatBuffer.wrap(tessGeo.pointAttribs, 0, 2 * size));
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, 0);
  }
  
  
  protected void setModifiedPolyVertices(int first, int last) {
    if (first < firstModifiedPolyVertex) firstModifiedPolyVertex = first;    
    if (last > lastModifiedPolyVertex) lastModifiedPolyVertex = last;
    modifiedPolyVertices = true;
    modified = true;
  }

  
  protected void setModifiedPolyColors(int first, int last) {
    if (first < firstModifiedPolyColor) firstModifiedPolyColor = first;    
    if (last > lastModifiedPolyColor) lastModifiedPolyColor = last;
    modifiedPolyColors = true;
    modified = true;
  } 

  
  protected void setModifiedPolyNormals(int first, int last) {
    if (first < firstModifiedPolyNormal) firstModifiedPolyNormal = first;    
    if (last > lastModifiedPolyNormal) lastModifiedPolyNormal = last;
    modifiedPolyNormals = true;
    modified = true;
  }
  
  
  protected void setModifiedPolyTexcoords(int first, int last) {
    if (first < firstModifiedPolyTexcoord) firstModifiedPolyTexcoord = first;    
    if (last > lastModifiedPolyTexcoord) lastModifiedPolyTexcoord = last;
    modifiedPolyTexcoords = true;
    modified = true;
  }  
  

  protected void setModifiedPolyAmbient(int first, int last) {
    if (first < firstModifiedPolyAmbient) firstModifiedPolyAmbient = first;    
    if (last > lastModifiedPolyAmbient) lastModifiedPolyAmbient = last;
    modifiedPolyAmbient = true;
    modified = true;
  }
  

  protected void setModifiedPolySpecular(int first, int last) {
    if (first < firstModifiedPolySpecular) firstModifiedPolySpecular = first;    
    if (last > lastModifiedPolySpecular) lastModifiedPolySpecular = last;
    modifiedPolySpecular = true;
    modified = true;
  }  
  
  
  protected void setModifiedPolyEmissive(int first, int last) {
    if (first < firstModifiedPolyEmissive) firstModifiedPolyEmissive = first;    
    if (last > lastModifiedPolyEmissive) lastModifiedPolyEmissive = last;
    modifiedPolyEmissive = true;
    modified = true;
  } 
  
  
  protected void setModifiedPolyShininess(int first, int last) {
    if (first < firstModifiedPolyShininess) firstModifiedPolyShininess = first;    
    if (last > lastModifiedPolyShininess) lastModifiedPolyShininess = last;
    modifiedPolyShininess = true;
    modified = true;
  }   

  
  protected void setModifiedLineVertices(int first, int last) {
    if (first < firstModifiedLineVertex) firstModifiedLineVertex = first;    
    if (last > lastModifiedLineVertex) lastModifiedLineVertex = last;
    modifiedLineVertices = true;
    modified = true;
  }

  
  protected void setModifiedLineColors(int first, int last) {
    if (first < firstModifiedLineColor) firstModifiedLineColor = first;    
    if (last > lastModifiedLineColor) lastModifiedLineColor = last;
    modifiedLineColors = true;
    modified = true;
  }  
  
  
  protected void setModifiedLineAttributes(int first, int last) {
    if (first < firstModifiedLineAttribute) firstModifiedLineAttribute = first;    
    if (last > lastModifiedLineAttribute) lastModifiedLineAttribute = last;
    modifiedLineAttributes = true;
    modified = true;
  }   

  
  protected void setModifiedPointVertices(int first, int last) {
    if (first < firstModifiedPointVertex) firstModifiedPointVertex = first;    
    if (last > lastModifiedPointVertex) lastModifiedPointVertex = last;
    modifiedPointVertices = true;
    modified = true;
  }
  

  protected void setModifiedPointColors(int first, int last) {
    if (first < firstModifiedPointColor) firstModifiedPointColor = first;    
    if (last > lastModifiedPointColor) lastModifiedPointColor = last;
    modifiedPointColors = true;
    modified = true;
  }  
  
  
  protected void setModifiedPointAttributes(int first, int last) {
    if (first < firstModifiedPointAttribute) firstModifiedPointAttribute = first;    
    if (last > lastModifiedPointAttribute) lastModifiedPointAttribute = last;    
    modifiedPointAttributes = true;
    modified = true;
  }   
  
  
  ///////////////////////////////////////////////////////////  
  
  //
  
  // Style handling

  
  // Applies the styles of g.
  protected void styles(PGraphics g) {
    if (stroke) {
      stroke(g.strokeColor);
      strokeWeight(g.strokeWeight);
      
      // These two don't to nothing probably:
      strokeCap(g.strokeCap);
      strokeJoin(g.strokeJoin);
    } else {
      noStroke();
    }

    if (fill) {      
      fill(g.fillColor);
    } else {
      noFill();
    }    
    
    ambient(g.ambientColor);  
    specular(g.specularColor);  
    emissive(g.emissiveColor);
    shininess(g.shininess);   
    
    // What about other style parameters, such as rectMode, etc?
    // These should force a tessellation update, same as stroke
    // cap and weight... right?
  }
  
  
  ///////////////////////////////////////////////////////////  
  
  //
  
  // Rendering methods
  
  
  public void draw() {
    draw(pg);
  }
  
  
  public void draw(PGraphics g) {
    if (visible) {      
      pre(g);
      
      updateTessellation(root.forceTessellation);      
      updateGeometry();
      
      if (family == GROUP) {        
        if (fragmentedGroup(g)) {
          for (int i = 0; i < childCount; i++) {
            ((PShapeOpenGL) children[i]).draw(g);
          }        
        } else {          
          PImage tex = null;
          if (textures != null && textures.size() == 1) {
            tex = (PImage)textures.toArray()[0];
          }
          render((PGraphicsOpenGL)g, tex);
        }
              
      } else {
        render((PGraphicsOpenGL)g, texture);
      }
      
      post(g);
    }
  }

  
  // Returns true if some child shapes below this one either
  // use different texture maps or have stroked textures,   
  // so they cannot rendered in a single call.
  // Or accurate 2D mode is enabled, which forces each 
  // shape to be rendered separately.  
  protected boolean fragmentedGroup(PGraphics g) {
    return g.hintEnabled(ENABLE_ACCURATE_2D) || 
           (textures != null && 1 < textures.size()) || 
           strokedTexture;
  }
  
  
  protected void pre(PGraphics g) {
    if (!style) {
      styles(g);
    }
  }
  
  
  public void post(PGraphics g) {
  }  
  
  
  // Render the geometry stored in the root shape as VBOs, for the vertices 
  // corresponding to this shape. Sometimes we can have root == this.
  protected void render(PGraphicsOpenGL g, PImage texture) {
    if (root == null) {
      // Some error. Root should never be null. At least it should be 'this'.
      throw new RuntimeException("Error rendering PShapeOpenGL, root shape is null");
    }

    if (hasPolys) { 
      renderPolys(g, texture);
      if (g.haveRaw()) {
        rawPolys(g, texture);        
      }
    }    
    
    if (is3D()) {
      // In 3D mode, the lines and points need to be rendered separately
      // as they require their own shaders.
      if (hasLines) {    
        renderLines(g);
        if (g.haveRaw()) {
          rawLines(g);
        }
      }    
      
      if (hasPoints) {
        renderPoints(g);
        if (g.haveRaw()) {
          rawPoints(g);
        }      
      }
    }
  }
    
  
  protected void renderPolys(PGraphicsOpenGL g, PImage textureImage) {
    PTexture tex = null;
    if (textureImage != null) {
      tex = g.getTexture(textureImage);      
      if (tex != null) {
        pgl.enableTexturing(tex.glTarget);          
        pgl.glBindTexture(tex.glTarget, tex.glID);        
      }
    }    
    
    boolean renderingFill = false, renderingStroke = false;
    PolyShader shader = null;
    IndexCache cache = tessGeo.polyIndexCache;
    for (int n = firstPolyIndexCache; n <= lastPolyIndexCache; n++) {
      if (tex != null && (is3D() || n < firstLineIndexCache && n < firstPointIndexCache)) {
        // Rendering fill triangles, which can be lit and textured.
        if (!renderingFill) {
          shader = g.getPolyShader(g.lights, tex != null);
          shader.start();
          renderingFill = true;
        }
      } else {
        // Rendering line or point triangles, which are never lit nor textured.
        if (!renderingStroke) {
          if (tex != null) {
            pgl.glBindTexture(tex.glTarget, 0); 
            pgl.disableTexturing(tex.glTarget);
            tex = null;
          }
          
          if (shader != null && shader.active()) {
            shader.stop();  
          }
          
          // If the renderer is 2D, then g.lights should always be false,
          // so no need to worry about that.          
          shader = g.getPolyShader(g.lights, false);
          shader.start();
          
          renderingFill = false;
          renderingStroke = true;
        }
      }
          
      int ioffset = cache.indexOffset[n];
      int icount = cache.indexCount[n];
      int voffset = cache.vertexOffset[n];
      
      shader.setVertexAttribute(root.glPolyVertexBufferID, 4, PGL.GL_FLOAT, 0, 4 * voffset * PGL.SIZEOF_FLOAT);        
      shader.setColorAttribute(root.glPolyColorBufferID, 4, PGL.GL_UNSIGNED_BYTE, 0, 4 * voffset * PGL.SIZEOF_BYTE);    
      
      if (g.lights) {
        shader.setNormalAttribute(root.glPolyNormalBufferID, 3, PGL.GL_FLOAT, 0, 3 * voffset * PGL.SIZEOF_FLOAT);
        shader.setAmbientAttribute(root.glPolyAmbientBufferID, 4, PGL.GL_UNSIGNED_BYTE, 0, 4 * voffset * PGL.SIZEOF_BYTE);
        shader.setSpecularAttribute(root.glPolySpecularBufferID, 4, PGL.GL_UNSIGNED_BYTE, 0, 4 * voffset * PGL.SIZEOF_BYTE);
        shader.setEmissiveAttribute(root.glPolyEmissiveBufferID, 4, PGL.GL_UNSIGNED_BYTE, 0, 4 * voffset * PGL.SIZEOF_BYTE);
        shader.setShininessAttribute(root.glPolyShininessBufferID, 1, PGL.GL_FLOAT, 0, voffset * PGL.SIZEOF_FLOAT);
      }
      
      if (tex != null) {        
        shader.setTexcoordAttribute(root.glPolyTexcoordBufferID, 2, PGL.GL_FLOAT, 0, 2 * voffset * PGL.SIZEOF_FLOAT);
        shader.setTexture(tex);
      }      
      
      pgl.glBindBuffer(PGL.GL_ELEMENT_ARRAY_BUFFER, root.glPolyIndexBufferID);
      pgl.glDrawElements(PGL.GL_TRIANGLES, icount, PGL.INDEX_TYPE, ioffset * PGL.SIZEOF_INDEX);
      pgl.glBindBuffer(PGL.GL_ELEMENT_ARRAY_BUFFER, 0);
    } 
    
    if (shader != null && shader.active()) {
      shader.stop();  
    }
    
    if (tex != null) {
      pgl.glBindTexture(tex.glTarget, 0); 
      pgl.disableTexturing(tex.glTarget);
    }     
  }  
  
  
  protected void rawPolys(PGraphicsOpenGL g, PImage textureImage) {
    PGraphics raw = g.getRaw();
    
    raw.colorMode(RGB);
    raw.noStroke();
    raw.beginShape(TRIANGLES);
    
    float[] vertices = tessGeo.polyVertices;
    int[] color = tessGeo.polyColors;
    float[] uv = tessGeo.polyTexcoords;
    short[] indices = tessGeo.polyIndices;  
    
    IndexCache cache = tessGeo.polyIndexCache;
    for (int n = firstPolyIndexCache; n <= lastPolyIndexCache; n++) {     
      int ioffset = cache.indexOffset[n];
      int icount = cache.indexCount[n];
      int voffset = cache.vertexOffset[n];
      
      for (int tr = ioffset / 3; tr < (ioffset + icount) / 3; tr++) {
        int i0 = voffset + indices[3 * tr + 0];
        int i1 = voffset + indices[3 * tr + 1];
        int i2 = voffset + indices[3 * tr + 2];

        float[] src0 = {0, 0, 0, 0};
        float[] src1 = {0, 0, 0, 0};
        float[] src2 = {0, 0, 0, 0};        
        float[] pt0 = {0, 0, 0, 0};
        float[] pt1 = {0, 0, 0, 0};
        float[] pt2 = {0, 0, 0, 0};
        int argb0 = PGL.nativeToJavaARGB(color[i0]);
        int argb1 = PGL.nativeToJavaARGB(color[i1]);
        int argb2 = PGL.nativeToJavaARGB(color[i2]);        
        
        PApplet.arrayCopy(vertices, 4 * i0, src0, 0, 4);
        PApplet.arrayCopy(vertices, 4 * i1, src1, 0, 4);
        PApplet.arrayCopy(vertices, 4 * i2, src2, 0, 4);
        // Applying any transformation is currently stored in the
        // modelview matrix of the renderer.
        g.modelview.mult(src0, pt0);
        g.modelview.mult(src1, pt1);
        g.modelview.mult(src2, pt2);        
        
        if (textureImage != null) {
          raw.texture(textureImage);
          if (raw.is3D()) {              
            raw.fill(argb0);
            raw.vertex(pt0[X], pt0[Y], pt0[Z], uv[2 * i0 + 0], uv[2 * i0 + 1]);
            raw.fill(argb1);
            raw.vertex(pt1[X], pt1[Y], pt1[Z], uv[2 * i1 + 0], uv[2 * i1 + 1]);
            raw.fill(argb2);
            raw.vertex(pt2[X], pt2[Y], pt2[Z], uv[2 * i2 + 0], uv[2 * i2 + 1]);              
          } else if (raw.is2D()) {
            float sx0 = g.screenXImpl(pt0[0], pt0[1], pt0[2], pt0[3]), sy0 = g.screenYImpl(pt0[0], pt0[1], pt0[2], pt0[3]);
            float sx1 = g.screenXImpl(pt1[0], pt1[1], pt1[2], pt1[3]), sy1 = g.screenYImpl(pt1[0], pt1[1], pt1[2], pt1[3]);
            float sx2 = g.screenXImpl(pt2[0], pt2[1], pt2[2], pt2[3]), sy2 = g.screenYImpl(pt2[0], pt2[1], pt2[2], pt2[3]);              
            raw.fill(argb0);
            raw.vertex(sx0, sy0, uv[2 * i0 + 0], uv[2 * i0 + 1]);
            raw.fill(argb1);
            raw.vertex(sx1, sy1, uv[2 * i1 + 0], uv[2 * i1 + 1]);
            raw.fill(argb1);
            raw.vertex(sx2, sy2, uv[2 * i2 + 0], uv[2 * i2 + 1]);              
          }
        } else {
          if (raw.is3D()) {
            raw.fill(argb0);
            raw.vertex(pt0[X], pt0[Y], pt0[Z]);
            raw.fill(argb1);
            raw.vertex(pt1[X], pt1[Y], pt1[Z]);
            raw.fill(argb2);
            raw.vertex(pt2[X], pt2[Y], pt2[Z]);              
          } else if (raw.is2D()) {
            float sx0 = g.screenXImpl(pt0[0], pt0[1], pt0[2], pt0[3]), sy0 = g.screenYImpl(pt0[0], pt0[1], pt0[2], pt0[3]);
            float sx1 = g.screenXImpl(pt1[0], pt1[1], pt1[2], pt1[3]), sy1 = g.screenYImpl(pt1[0], pt1[1], pt1[2], pt1[3]);
            float sx2 = g.screenXImpl(pt2[0], pt2[1], pt2[2], pt2[3]), sy2 = g.screenYImpl(pt2[0], pt2[1], pt2[2], pt2[3]);              
            raw.fill(argb0);
            raw.vertex(sx0, sy0);
            raw.fill(argb1);
            raw.vertex(sx1, sy1);
            raw.fill(argb2);
            raw.vertex(sx2, sy2);              
          }
        }
      }
    }
    
    raw.endShape();
  }
  
  
  protected void renderLines(PGraphicsOpenGL g) {
    LineShader shader = g.getLineShader();
    shader.start(); 
    
    IndexCache cache = tessGeo.lineIndexCache;
    for (int n = firstLineIndexCache; n <= lastLineIndexCache; n++) {     
      int ioffset = cache.indexOffset[n];
      int icount = cache.indexCount[n];
      int voffset = cache.vertexOffset[n];
    
      shader.setVertexAttribute(root.glLineVertexBufferID, 4, PGL.GL_FLOAT, 0, 4 * voffset * PGL.SIZEOF_FLOAT);        
      shader.setColorAttribute(root.glLineColorBufferID, 4, PGL.GL_UNSIGNED_BYTE, 0, 4 * voffset * PGL.SIZEOF_BYTE);    
      shader.setLineAttribute(root.glLineAttribBufferID, 4, PGL.GL_FLOAT, 0, 4 * voffset * PGL.SIZEOF_FLOAT);
      
      pgl.glBindBuffer(PGL.GL_ELEMENT_ARRAY_BUFFER, root.glLineIndexBufferID);
      pgl.glDrawElements(PGL.GL_TRIANGLES, icount, PGL.INDEX_TYPE, ioffset * PGL.SIZEOF_INDEX);      
      pgl.glBindBuffer(PGL.GL_ELEMENT_ARRAY_BUFFER, 0);      
    }
    
    shader.stop();
  }  

  
  protected void rawLines(PGraphicsOpenGL g) {
    PGraphics raw = g.getRaw();
    
    raw.colorMode(RGB);
    raw.noFill();
    raw.strokeCap(strokeCap);
    raw.strokeJoin(strokeJoin);
    raw.beginShape(LINES);
    
    float[] vertices = tessGeo.lineVertices;
    int[] color = tessGeo.lineColors;
    float[] attribs = tessGeo.lineAttribs;
    short[] indices = tessGeo.lineIndices;      
    
    IndexCache cache = tessGeo.lineIndexCache;
    for (int n = firstLineIndexCache; n <= lastLineIndexCache; n++) {     
      int ioffset = cache.indexOffset[n];
      int icount = cache.indexCount[n];
      int voffset = cache.vertexOffset[n];
     
      for (int ln = ioffset / 6; ln < (ioffset + icount) / 6; ln++) {
        // Each line segment is defined by six indices since its
        // formed by two triangles. We only need the first and last
        // vertices.
        int i0 = voffset + indices[6 * ln + 0];
        int i1 = voffset + indices[6 * ln + 5];
        
        float[] src0 = {0, 0, 0, 0};
        float[] src1 = {0, 0, 0, 0};         
        float[] pt0 = {0, 0, 0, 0};
        float[] pt1 = {0, 0, 0, 0};        
        int argb0 = PGL.nativeToJavaARGB(color[i0]);
        int argb1 = PGL.nativeToJavaARGB(color[i1]);
        float sw0 = 2 * attribs[4 * i0 + 3];
        float sw1 = 2 * attribs[4 * i1 + 3];        
       
        PApplet.arrayCopy(vertices, 4 * i0, src0, 0, 4);
        PApplet.arrayCopy(vertices, 4 * i1, src1, 0, 4); 
        // Applying any transformation is currently stored in the
        // modelview matrix of the renderer.        
        g.modelview.mult(src0, pt0);
        g.modelview.mult(src1, pt1);        
       
        if (raw.is3D()) {
          raw.strokeWeight(sw0);
          raw.stroke(argb0);
          raw.vertex(pt0[X], pt0[Y], pt0[Z]);
          raw.strokeWeight(sw1);
          raw.stroke(argb1);
          raw.vertex(pt1[X], pt1[Y], pt1[Z]);
        } else if (raw.is2D()) {
          float sx0 = g.screenXImpl(pt0[0], pt0[1], pt0[2], pt0[3]), sy0 = g.screenYImpl(pt0[0], pt0[1], pt0[2], pt0[3]);
          float sx1 = g.screenXImpl(pt1[0], pt1[1], pt1[2], pt1[3]), sy1 = g.screenYImpl(pt1[0], pt1[1], pt1[2], pt1[3]);
          raw.strokeWeight(sw0);
          raw.stroke(argb0);
          raw.vertex(sx0, sy0);
          raw.strokeWeight(sw1);
          raw.stroke(argb1);
          raw.vertex(sx1, sy1);        
        }
      }
    }
    
    raw.endShape();
  }

  
  protected void renderPoints(PGraphicsOpenGL g) {
    PointShader shader = g.getPointShader();
    shader.start(); 
    
    IndexCache cache = tessGeo.pointIndexCache;    
    for (int n = firstPointIndexCache; n <= lastPointIndexCache; n++) {     
      int ioffset = cache.indexOffset[n];
      int icount = cache.indexCount[n];
      int voffset = cache.vertexOffset[n];
      
      shader.setVertexAttribute(root.glPointVertexBufferID, 4, PGL.GL_FLOAT, 0, 4 * voffset * PGL.SIZEOF_FLOAT);        
      shader.setColorAttribute(root.glPointColorBufferID, 4, PGL.GL_UNSIGNED_BYTE, 0, 4 * voffset * PGL.SIZEOF_BYTE);    
      shader.setPointAttribute(root.glPointAttribBufferID, 2, PGL.GL_FLOAT, 0, 2 * voffset * PGL.SIZEOF_FLOAT);      
      
      pgl.glBindBuffer(PGL.GL_ELEMENT_ARRAY_BUFFER, root.glPointIndexBufferID);      
      pgl.glDrawElements(PGL.GL_TRIANGLES, icount, PGL.INDEX_TYPE, ioffset * PGL.SIZEOF_INDEX);    
      pgl.glBindBuffer(PGL.GL_ELEMENT_ARRAY_BUFFER, 0);      
    }
    
    shader.stop();
  }  


  protected void rawPoints(PGraphicsOpenGL g) {    
    PGraphics raw = g.getRaw();
    
    raw.colorMode(RGB);
    raw.noFill();
    raw.strokeCap(strokeCap);
    raw.beginShape(POINTS);
    
    float[] vertices = tessGeo.pointVertices;
    int[] color = tessGeo.pointColors;
    float[] attribs = tessGeo.pointAttribs;
    short[] indices = tessGeo.pointIndices; 
    
    IndexCache cache = tessGeo.pointIndexCache;
    for (int n = 0; n < cache.size; n++) {     
      int ioffset = cache.indexOffset[n];
      int icount = cache.indexCount[n];
      int voffset = cache.vertexOffset[n];
      
      int pt = ioffset;
      while (pt < (ioffset + icount) / 3) {
        float size = attribs[2 * pt + 2];
        float weight;
        int perim;
        if (0 < size) { // round point
          weight = +size / 0.5f;
          perim = PApplet.max(PGraphicsOpenGL.MIN_POINT_ACCURACY, (int) (TWO_PI * weight / 20)) + 1;          
        } else {        // Square point
          weight = -size / 0.5f;
          perim = 5;          
        }
                
        int i0 = voffset + indices[3 * pt];
        int argb0 = PGL.nativeToJavaARGB(color[i0]);
        float[] pt0 = {0, 0, 0, 0};
        
        float[] src0 = {0, 0, 0, 0};          
        PApplet.arrayCopy(vertices, 4 * i0, src0, 0, 4);          
        g.modelview.mult(src0, pt0);
        
        if (raw.is3D()) {
          raw.strokeWeight(weight);
          raw.stroke(argb0);
          raw.vertex(pt0[X], pt0[Y], pt0[Z]);
        } else if (raw.is2D()) {
          float sx0 = g.screenXImpl(pt0[0], pt0[1], pt0[2], pt0[3]), sy0 = g.screenYImpl(pt0[0], pt0[1], pt0[2], pt0[3]);
          raw.strokeWeight(weight);
          raw.stroke(argb0);
          raw.vertex(sx0, sy0);     
        }         
        
        pt += perim;
      }
    }    
    
    raw.endShape();    
  }  
}
