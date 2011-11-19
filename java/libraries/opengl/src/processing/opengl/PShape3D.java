/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2011 Andres Colubri
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

package processing.opengl;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GL2ES1;
import javax.media.opengl.glu.GLU;
import javax.media.opengl.glu.GLUtessellator;
import javax.media.opengl.glu.GLUtessellatorCallbackAdapter;

import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PImage;
import processing.core.PMatrix3D;
import processing.core.PShape;
import processing.core.PVector;
import processing.opengl.PGraphicsOpenGL.PTessellator;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
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
 */
public class PShape3D extends PShape {

  protected PApplet papplet;      
  protected PGraphicsOpenGL ogl;

  // Element types handled by PShape3D (vertices, normals, color, texture coordinates).
  protected static final int VERTICES = 0;
  protected static final int NORMALS = 1;
  protected static final int COLORS = 2;
  protected static final int TEXCOORDS = 3; 

  // ROOT shape properties:

  // Number of texture buffers currently in use:
  protected int numTexBuffers;  

  // STATIC, DYNAMIC, or STREAM
  // TODO: vertex, color, normal and texcoord data can potentially have 
  // different usage modes.
  public int glUsage;

  // The OpenGL IDs for the different VBOs
  public int glVertexBufferID;
  public int glColorBufferID;
  public int glNormalBufferID;
  public int[] glTexCoordBufferID;

  // The float buffers (directly allocated) used to put data into the VBOs
  protected FloatBuffer vertexBuffer;
  protected FloatBuffer colorBuffer;
  protected FloatBuffer normalBuffer;
  protected FloatBuffer texCoordBuffer; 

  // Public arrays for setting/getting vertices, colors, normals, and 
  // texture coordinates when using loadVertices/updateVertices,
  // loadNormals/updateNormals, etc. This is modeled following the
  // loadPixels/updatePixels API for setting/getting pixel data in 
  // PImage.
  public float[] vertices;
  public float[] colors;
  public float[] normals;
  public float[] texcoords;

  // Indexed mode, testing:
  protected int glIndexBufferID = 0;
  protected IntBuffer indexBuffer = null;
  protected int indexCount = 0;
  protected int[] indices;
  protected boolean useIndices;

  // To put the texture coordinate values adjusted according to texture 
  // flipping mode, max UV range, etc.
  protected float[] convTexcoords;
  // The array of arrays holding the texture coordinates for all texture units.
  protected float[][] allTexcoords;

  // Child PShape3D associated to each vertex in the model.
  protected PShape3D[] vertexChild;

  // Some utility arrays.    
  protected boolean[] texCoordSet;      

  protected boolean autoBounds = true;

  // For OBJ loading. Only used by the root shape.
  boolean readFromOBJ = false;
  ArrayList<PVector> objVertices; 
  ArrayList<PVector> objNormal; 
  ArrayList<PVector> objTexCoords;    
  ArrayList<OBJFace> objFaces;
  ArrayList<OBJMaterial> objMaterials;  

  // GEOMETRY shape properties:

  // Draw mode, point sprites and textures. 
  // Stroke weight is inherited from PShape.
  protected int glMode;
  protected boolean pointSprites;  
  protected PImage[] textures;  
  protected float maxSpriteSize = PGraphicsOpenGL.maxPointSize;
  // Coefficients for point sprite distance attenuation function.  
  // These default values correspond to the constant sprite size.
  protected float spriteDistAtt[] = { 1.0f, 0.0f, 0.0f };
  // Used in the drawGeometry() method.
  protected PTexture[] renderTextures;

  // GROUP and GEOMETRY properties:

  // The root group shape.
  protected PShape3D root;

  // Element type, vertex indices and texture units being currently updated.
  protected int updateElement;
  protected int updateTexunit;
  protected int firstUpdateIdx;
  protected int lastUpdateIdx;  

  // first and last vertex in the shape. For the root group shape, these are 0 and
  // vertexCount - 1.
  protected int firstVertex;
  protected int lastVertex;  

  protected int firstIndex;
  protected int lastIndex;    

  // Bounding box (defined for all shapes, group and geometry):
  public float xmin, xmax;
  public float ymin, ymax;
  public float zmin, zmax;  

  ////////////////////////////////////////////////////////////

  public static final int DEFAULT_VERTICES = 512;
  public static final int DEFAULT_INDICES = 512;

  // Constructors.

  public PShape3D() {
    this.papplet = null;
    ogl = null;

    glVertexBufferID = 0;
    glColorBufferID = 0;
    glNormalBufferID = 0;
    glTexCoordBufferID = null;

    updateElement = -1;
  }

  public PShape3D(PApplet parent) {
    this();
    this.papplet = parent;
    ogl = (PGraphicsOpenGL)parent.g;
    //ogl.registerPGLObject(this);

    this.family = PShape.GROUP;
    this.name = "root";
    this.root = this;
  }

  public PShape3D(PApplet parent, int family) {
    this.papplet = parent;
    ogl = (PGraphicsOpenGL)parent.g;
    //ogl.registerPGLObject(this);    

    this.family = family;

    vertexCount = 0;
    firstVertex = 0;
    lastVertex = 0;

    indexCount = 0;
    firstIndex = 0;
    lastIndex = 0;

    Parameters params = new Parameters();
    params.drawMode = TRIANGLES;
    params.updateMode = DYNAMIC;
    setParameters(params);  

    if (strokeShader == null) {
      strokeShader = new PShader(parent);
      strokeShader.loadVertexShaderSource(strokeShaderVert);
      strokeShader.loadFragmentShaderSource(strokeShaderFrag);
      strokeShader.setup();
    }

    if (pointShader == null) {
      pointShader = new PShader(parent);
      pointShader.loadVertexShaderSource(pointShaderVert);
      pointShader.loadFragmentShaderSource(pointShaderFrag);
      pointShader.setup();
    }    
    
    if (family != GROUP) {
      tobj = GLU.gluNewTess();
      TessCallback tessCallback;
    
      tessCallback = new TessCallback();
      GLU.gluTessCallback(tobj, GLU.GLU_TESS_BEGIN, tessCallback);
      GLU.gluTessCallback(tobj, GLU.GLU_TESS_END, tessCallback);
      GLU.gluTessCallback(tobj, GLU.GLU_TESS_VERTEX, tessCallback);
      GLU.gluTessCallback(tobj, GLU.GLU_TESS_COMBINE, tessCallback);
      GLU.gluTessCallback(tobj, GLU.GLU_TESS_ERROR, tessCallback);
    }
    
    isSolid = false;
    isClosed = true;    
    addBreak = false;
    
    root = this;
    parent = null;
    updateElement = -1;
  }   

  public void setKind(int kind) {
    this.kind = kind;

    if (family == GEOMETRY || family == PRIMITIVE) {
      inVertexTypes = new int[64];
      inVertices = new float[3 * DEFAULT_VERTICES];  
      inTexCoords = new float[2 * DEFAULT_VERTICES];
      inNormals = new float[3 * DEFAULT_VERTICES];
      inColors = new float[4 * DEFAULT_VERTICES];
      inStroke = new float[5 * DEFAULT_VERTICES];

      textures = new PImage[1];
      textures[0] = null;
      
      if (family == PRIMITIVE) {
        if (kind == BOX) {
          params = new float[1];
        } else if (kind == SPHERE) {
          params = new float[3];
          params[1] = 20;
          params[2] = 20;
        }
      }      
    }
  }



  /*
  public PShape3D(PApplet parent, int numVert) {
    this(parent, numVert, new Parameters()); 
  } 
   */ 

  public PShape3D(PApplet parent, String filename, Parameters params) {
    this.papplet = parent;
    ogl = (PGraphicsOpenGL)parent.g;
    //ogl.registerPGLObject(this);

    this.family = PShape.GROUP;
    this.name = "root";
    this.root = this;
    glVertexBufferID = 0;
    glColorBufferID = 0;
    glNormalBufferID = 0;
    glTexCoordBufferID = null;

    updateElement = -1;

    initShapeOBJ(filename, params);    
  }  


  public PShape3D(PApplet parent, int size, Parameters params) {
    this.papplet = parent;
    ogl = (PGraphicsOpenGL)parent.g;
    //ogl.registerPGLObject(this);

    this.family = PShape.GROUP;
    this.name = "root";
    this.root = this;
    glVertexBufferID = 0;
    glColorBufferID = 0;
    glNormalBufferID = 0;
    glTexCoordBufferID = null;

    updateElement = -1; 

    initShape(size, params);
  }


  /*
  public void delete() {
    if (root != this) return; // Can be done only from the root shape.    
    release();
    ogl.unregisterPGLObject(this);
  }


  public void backup() {

  }

  public void restore() {
    if (root != this) return; // Can be done only from the root shape.

    // Loading/updating each piece of data so the arrays on the CPU-side 
    // are copied to the VBOs on the GPU.

    //loadVertices();
    //updateVertices();

    loadColors();
    updateColors();

    loadNormals();
    updateNormals();

    for (int i = 0; i < numTexBuffers; i++) {
      loadTexcoords(i);    
      updateTexcoords();
    }
  }
  */
  
  
  protected void finalize() throws Throwable {
    try {
      release();   
    } finally {
      super.finalize();
    }
  }  
  
  

  ////////////////////////////////////////////////////////////

  // the new stuff  
  static protected final int GEOMETRY_POINT = 0;
  static protected final int LINE_POINT = 1;
  static protected final int CURVE_POINT = 2;
  static protected final int BEZIER_POINT = 3;  
  //static protected final int BEZIER_ANCHOR_POINT = 4;
  static protected final int BREAK_POINT = 4;
  
  // To use later
  static public final int NURBS_CURVE = 4;
  static public final int NURBS_SURFACE = 5;  
  static protected final int NURBS2D_CONTROL_POINT = 4;
  static protected final int NURBS3D_CONTROL_POINT = 5;

  protected float[] currentNormal = { 0, 0, 1 };
  protected float[] currentColor = { 0, 0, 0, 0 };
  protected float[] currentStroke = { 0, 0, 0, 1, 1 };  
  
  protected int[] inVertexTypes;
  protected float[] inVertices;  
  protected float[] inTexCoords;
  protected float[] inNormals;
  protected float[] inColors;  // Fill color
  protected float[] inStroke; // Stroke color+weight  
  protected int inVertexCount;

  protected boolean modified;
  protected int mi0, mi1;    

  // For polygons
  protected boolean addBreak;
  protected boolean isSolid;
  protected boolean isClosed;
  
  protected boolean isStroked;
  
  protected int strokeVertexCount;
  protected int firstStrokeVertex;
  protected int lastStrokeVertex;

  protected int strokeIndexCount;
  protected int firstStrokeIndex;
  protected int lastStrokeIndex;  
  protected int[] strokeIndices;

  public int glStrokeVertexBufferID;
  public int glStrokeColorBufferID;
  public int glStrokeNormalBufferID;
  public int glStrokeAttribBufferID;
  public int glStrokeIndexBufferID;
  
  public float[] strokeVertices;
  public float[] strokeColors;
  public float[] strokeNormals;
  public float[] strokeAttributes;

  protected boolean hasPoints;
  
  protected int pointVertexCount;
  protected int firstPointVertex;
  protected int lastPointVertex;

  protected int pointIndexCount;
  protected int firstPointIndex;
  protected int lastPointIndex;  
  protected int[] pointIndices;

  public int glPointVertexBufferID;
  public int glPointColorBufferID;
  public int glPointNormalBufferID;
  public int glPointAttribBufferID;
  public int glPointIndexBufferID;
  
  public float[] pointVertices;
  public float[] pointColors;
  public float[] pointNormals;
  public float[] pointAttributes;  
  
  
  // ........................................................

  protected boolean bezierInited = false;
  public int bezierDetail = 20;

  // used by both curve and bezier, so just init here
  protected PMatrix3D bezierBasisMatrix =
    new PMatrix3D(-1,  3, -3,  1,
                   3, -6,  3,  0,
                  -3,  3,  0,  0,
                   1,  0,  0,  0);

  //protected PMatrix3D bezierForwardMatrix;
  protected PMatrix3D bezierDrawMatrix;

  // ........................................................

  protected boolean curveInited = false;
  protected int curveDetail = 20;
  public float curveTightness = 0;
  // catmull-rom basis matrix, perhaps with optional s parameter
  protected PMatrix3D curveBasisMatrix;
  protected PMatrix3D curveDrawMatrix;

  protected PMatrix3D bezierBasisInverse;
  protected PMatrix3D curveToBezierMatrix;

  // ........................................................

  // spline vertices

  protected float curveVertices[][];
  protected int curveVertexCount;
  
  
  
  
  static protected String strokeShaderVert = 
    "attribute vec4 attribs;\n" +   
    "uniform vec4 viewport;\n" +
    "vec3 clipToWindow(vec4 clip, vec4 viewport) {\n" +
    "  vec3 post_div = clip.xyz / clip.w;\n" +
    "  vec2 xypos = (post_div.xy + vec2(1.0, 1.0)) * 0.5 * viewport.zw;\n" +
    "  return vec3(xypos, post_div.z * 0.5 + 0.5);\n" +
    "}\n" +
    "void main() {\n" +
    "  vec4 pos_p = gl_Vertex;\n" +
    "  vec4 pos_q = vec4(attribs.xyz, 1);\n" +  
    "  vec4 v_p = gl_ModelViewMatrix * pos_p;\n" +
    "  v_p.xyz = v_p.xyz * 0.99;\n" +   
    "  vec4 clip_p = gl_ProjectionMatrix * v_p;\n" + 
    "  vec4 v_q = gl_ModelViewMatrix * pos_q;\n" +
    "  v_q.xyz = v_q.xyz * 0.99;\n" +   
    "  vec4 clip_q = gl_ProjectionMatrix * v_q;\n" + 
    "  vec3 window_p = clipToWindow(clip_p, viewport);\n" + 
    "  vec3 window_q = clipToWindow(clip_q, viewport);\n" + 
    "  vec3 tangent = window_q - window_p;\n" +
    "  float segment_length = length(tangent.xy);\n" +  
    "  vec2 perp = normalize(vec2(-tangent.y, tangent.x));\n" +
    "  float thickness = attribs.w;\n" +
    "  vec2 window_offset = perp * thickness;\n" +
    "  gl_Position.xy = clip_p.xy + window_offset.xy;\n" +
    "  gl_Position.zw = clip_p.zw;\n" +
    "  gl_FrontColor = gl_Color;\n" +
    "}";
  
  static protected String strokeShaderFrag =
    "void main() {\n" +  
    " gl_FragColor = gl_Color;\n" +
    "}";
  
  static protected String pointShaderVert = 
    "attribute vec2 vertDisp;\n" + 
    "void main() {\n" +
    "  vec4 pos = gl_ModelViewMatrix * gl_Vertex;\n" +
    "  pos.xy += vertDisp.xy;\n" +
    "  gl_Position = gl_ProjectionMatrix * pos;\n" +  
    "  gl_FrontColor = gl_Color;\n" +
    "}";

  static protected String pointShaderFrag =
    "void main() {\n" +  
    " gl_FragColor = gl_Color;\n" +
    "}";
  
  static protected PShader strokeShader;
  static protected PShader pointShader;

  protected GLUtessellator tobj;
  
  protected void inputCheck() {
    if (inVertexCount == inVertexTypes.length) {
      int newSize = inVertexCount << 1; // newSize = 2 * inVertexCount  

      expandInVertexType(newSize);
      expandInVertex(newSize);
      expandInTexCoord(newSize);      
      expandInNormal(newSize);
      expandInColor(newSize);
      expandInStroke(newSize);
    }
  }
  
  protected void tessCheck() {
    if (vertexCount == vertices.length / 3) {
      int newSize = vertexCount << 1;
      
      expandTessVertex(newSize);
      expandTessTexCoord(newSize);      
      expandTessNormal(newSize);
      expandTessColor(newSize);
    }    
  }
  
  protected void indexCheck() {
    if (indexCount == indices.length) {
      int newSize = indexCount << 1;
      
      expandIndex(newSize);
    }
  }
  
  protected void updateTesselation() {
    if (root.isModified()) {
      // Just re-creating everything. 
      // Later we can do something a little more
      // refined.
      root.tessellate();
      root.aggregate();        
      root.setModified(false);
    }    
  }

  protected void expandInVertexType(int n) {
    int temp[] = new int[n];      
    System.arraycopy(inVertexTypes, 0, temp, 0, inVertexCount);
    inVertexTypes = temp;    
  }

  protected void expandInVertex(int n) {
    float temp[] = new float[3 * n];      
    System.arraycopy(inVertices, 0, temp, 0, 3 * inVertexCount);
    inVertices = temp;    
  }

  protected void expandInTexCoord(int n) {
    float temp[] = new float[2 * n];      
    System.arraycopy(inTexCoords, 0, temp, 0, 2 * inVertexCount);
    inTexCoords = temp;    
  }

  protected void expandInNormal(int n) {
    float temp[] = new float[3 * n];      
    System.arraycopy(inNormals, 0, temp, 0, 3 * inVertexCount);
    inNormals = temp;    
  }

  protected void expandInColor(int n){
    float temp[] = new float[4 * n];      
    System.arraycopy(inColors, 0, temp, 0, 4 * inVertexCount);
    inColors = temp;  
  }

  protected void expandInStroke(int n) {
    float temp[] = new float[5 * n];      
    System.arraycopy(inStroke, 0, temp, 0, 5 * inVertexCount);
    inStroke = temp;
  }  

  protected void expandTessVertex(int n) {
    float temp[] = new float[3 * n];      
    System.arraycopy(vertices, 0, temp, 0, 3 * vertexCount);
    vertices = temp;       
  }
  
  protected void expandTessTexCoord(int n) {
    float temp[] = new float[2 * n];      
    System.arraycopy(texcoords, 0, temp, 0, 2 * vertexCount);
    texcoords = temp;
  }
  
  protected void expandTessNormal(int n) {
    float temp[] = new float[3 * n];      
    System.arraycopy(normals, 0, temp, 0, 3 * vertexCount);
    normals = temp;       
  }
  
  protected void expandTessColor(int n) {
    float temp[] = new float[4 * n];      
    System.arraycopy(colors, 0, temp, 0, 4 * vertexCount);
    colors = temp;
  }
  
  protected void expandIndex(int n) {
    int temp[] = new int[n];      
    System.arraycopy(indices, 0, temp, 0, indexCount);
    indices = temp;
  }
  
  // Curve stuff -------------------------------------------
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

    //setup_spline_forward(segments, curveForwardMatrix);
    splineForward(curveDetail, curveDrawMatrix);

    if (bezierBasisInverse == null) {
      bezierBasisInverse = bezierBasisMatrix.get();
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
  
  protected void splineForward(int segments, PMatrix3D matrix) {
    float f  = 1.0f / segments;
    float ff = f * f;
    float fff = ff * f;

    matrix.set(0,     0,    0, 1,
               fff,   ff,   f, 0,
               6*fff, 2*ff, 0, 0,
               6*fff, 0,    0, 0);
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

    // vertex() will reset splineVertexCount, so save it
    int savedCount = curveVertexCount;

    float zplot1 = draw.m10*z1 + draw.m11*z2 + draw.m12*z3 + draw.m13*z4;
    float zplot2 = draw.m20*z1 + draw.m21*z2 + draw.m22*z3 + draw.m23*z4;
    float zplot3 = draw.m30*z1 + draw.m31*z2 + draw.m32*z3 + draw.m33*z4;

    addVertexImpl(x0, y0, z0, 0, 0, CURVE_POINT);
    for (int j = 0; j < curveDetail; j++) {
      x0 += xplot1; xplot1 += xplot2; xplot2 += xplot3;
      y0 += yplot1; yplot1 += yplot2; yplot2 += yplot3;
      z0 += zplot1; zplot1 += zplot2; zplot2 += zplot3;
      addVertexImpl(x0, y0, z0, 0, 0, CURVE_POINT);
    }
    curveVertexCount = savedCount;
  }  


  protected void bezierInitCheck() {
    if (!bezierInited) {
      bezierInit();
    }
  }

  protected void bezierInit() {
    // overkill to be broken out, but better parity with the curve stuff below
    setBezierDetail(bezierDetail);
    bezierInited = true;
  }  
  
  protected void bezierVertexCheck() {
    if (kind != POLYGON) {
      throw new RuntimeException("createGeometry() or createGeometry(POLYGON) " +
                                 "must be used before addBezierVertex() or addQuadraticVertex()");
    }
    if (inVertexCount == 0) {
      throw new RuntimeException("addVertex() must be used at least once" +
                                 "before addBezierVertex() or addQuadraticVertex()");
    }
  }  
  
  // -------------------------------------------------------
  
  public void setTexture(PImage tex) {
    if (family != GROUP) {
      textures[0] = tex;  
    }
  }
  
  public void setNoTexture() {
    if (family != GROUP) {
      textures[0] = null;  
    }
  }  
  
  // Explicitly set vertex connectivities
  public void addTriangle(int i0, int i1, int i2) {
    // blah blah blah
  }
  
  public void addVertex(float x, float y) {
    addVertex(x, y, 0, 0, 0);      
  }

  public void addVertex(float x, float y, float z) {
    addVertex(x, y, z, 0, 0);      
  }

  public void addVertex(float x, float y, float z, float u, float v) {
    if (family == NURBS_CURVE) {
      addVertexImpl(x, y, z, u, v, NURBS2D_CONTROL_POINT);  
    } else if (family == NURBS_SURFACE) {
      addVertexImpl(x, y, z, u, v, NURBS3D_CONTROL_POINT);
    } else if (family == GEOMETRY || family == PRIMITIVE){
      addVertexImpl(x, y, z, u, v, GEOMETRY_POINT);
    } else if (family == PATH){
      addVertexImpl(x, y, z, u, v, LINE_POINT);
    }    
  }

  public void setCurveDetail(int detail) {
    curveDetail = detail;
    curveInit();
  }
  
  public void setCurveTightness(float tightness) {
    curveTightness = tightness;
    curveInit();
  }  
  
  public void addCurveVertex(float x, float y) {
    addCurveVertex(x, y, 0);
  }  

  public void addCurveVertex(float x, float y, float z) {
    //addVertexImpl(x, y, 0, 0, 0, CURVE_POINT);
    
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
  
  public void setBezierDetail(int detail) {
    bezierDetail = detail;

    if (bezierDrawMatrix == null) {
      bezierDrawMatrix = new PMatrix3D();
    }

    // setup matrix for forward differencing to speed up drawing
    splineForward(detail, bezierDrawMatrix);

    // multiply the basis and forward diff matrices together
    // saves much time since this needn't be done for each curve
    //mult_spline_matrix(bezierForwardMatrix, bezier_basis, bezierDrawMatrix, 4);
    //bezierDrawMatrix.set(bezierForwardMatrix);
    bezierDrawMatrix.apply(bezierBasisMatrix);
  }  

  public void addBezierVertex(float cx1, float cy1, float cx2, float cy2, float x, float y) {
    addBezierVertex(cx1, cy1, 0, cx2, cy2, 0, x, y, 0);    
  }  

  public void addBezierVertex(float x2, float y2, float z2,
                              float x3, float y3, float z3,
                              float x4, float y4, float z4) {
    bezierInitCheck();
    bezierVertexCheck();
    PMatrix3D draw = bezierDrawMatrix;

    //float[] prev = inVertices[vertexCount-1];
    float x1 = inVertices[3 * (inVertexCount - 1) + 0];
    float y1 = inVertices[3 * (inVertexCount - 1) + 1];
    float z1 = inVertices[3 * (inVertexCount - 1) + 2];

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
      addVertexImpl(x1, y1, z1, 0, 0, BEZIER_POINT);
    }    
    
    /*
    addVertexImpl(cx1, cy1, cz1, 0, 0, BEZIER_CONTROL_POINT);
    addVertexImpl(cx2, cy2, cz2, 0, 0, BEZIER_CONTROL_POINT);
    addVertexImpl(x, y, z, 0, 0, BEZIER_ANCHOR_POINT);
    */
  }
  
  public void addQuadraticVertex(float cx, float cy, float cz,
                              float x3, float y3, float z3) {
    float x1 = inVertices[3 * (inVertexCount - 1) + 0];
    float y1 = inVertices[3 * (inVertexCount - 1) + 1];
    float z1 = inVertices[3 * (inVertexCount - 1) + 2];

    addBezierVertex(x1 + ((cx-x1)*2/3.0f), y1 + ((cy-y1)*2/3.0f), z1 + ((cz-z1)*2/3.0f),
                    x3 + ((cx-x3)*2/3.0f), y3 + ((cy-y3)*2/3.0f), z3 + ((cz-z3)*2/3.0f),
                    x3, y3, z3);
  }
  

  protected void addVertexImpl(float x, float y, float z, float u, float v, int type) {
    inputCheck();

    if (addBreak) {
      inVertexTypes[inVertexCount] = BREAK_POINT;
      addBreak = false;
    } else {
      inVertexTypes[inVertexCount] = type;
    }
    
    inVertices[3 * inVertexCount + 0] = x;
    inVertices[3 * inVertexCount + 1] = y;
    inVertices[3 * inVertexCount + 2] = z;

    inTexCoords[2 * inVertexCount + 0] = u;
    inTexCoords[2 * inVertexCount + 1] = v;

    PApplet.arrayCopy(currentNormal, 0, inNormals, 3 * inVertexCount, 3);
    PApplet.arrayCopy(currentColor, 0, inColors, 4 * inVertexCount, 4);
    PApplet.arrayCopy(currentStroke, 0, inStroke, 5 * inVertexCount, 5);
    
    inVertexCount++;

    modified = true;
  }

  // Will be renamed to setNormal later (now conflicting with old API).
  public void setNormal(float nx, float ny, float nz) {
    currentNormal[0] = nx;
    currentNormal[1] = ny;
    currentNormal[2] = nz;
  }

  public void setNoFill() {
    setFill(0, 0, 0, 0);   
  }
    
  public void setFill(float r, float g, float b, float a) {
    currentColor[0] = r;
    currentColor[1] = g;
    currentColor[2] = b;
    currentColor[3] = a;
  }

  public void setNoStroke() {
    setStroke(0, 0, 0, 0);   
  }  
  
  public void setStroke(float r, float g, float b, float a) {
    currentStroke[0] = r;
    currentStroke[1] = g;
    currentStroke[2] = b;
    currentStroke[3] = a;    
  }

  public void setStrokeWeight(float w) {
    currentStroke[4] = w;
  }  

  public void setStrokeJoin(int join) {
    strokeJoin = join;    
  }
  
  public void setStrokeCap(int cap) {
    strokeCap = cap;        
  }  
  
  public void setSolid(boolean solid) {
    isSolid = solid;
  }
  
  public void setClosed(boolean closed) {
    isClosed = closed;
  }
  
  public void addContour() {
    addBreak = true;
  }
  
  // Some primitives ---------------------------------------------------
  
  public void setBoxSize(float size) {
    if (params != null) {
      params[0] = size;
      modified = true;
    }
  }
  
  public void setSphereRadius(float radius) {
    if (params != null) {
      params[0] = radius;
      modified = true;
    }
  }
  
  public void setSphereDetail(int res) {
    setSphereDetail(res, res);
  }
  
  public void setSphereDetail(int ures, int vres) {
    if (params != null) {
      params[1] = ures;
      params[2] = vres;
    }
  }
  
  // done with primitives -----------------------------------------------
  
  // Will be renamed to getVertex later (now conflicting with old API).
  public float[] getVertex(int i) {
    if (0 <= i && i < inVertexCount) {
      //return new PVector(inVertices[3 * i + 0], inVertices[3 * i + 1], inVertices[3 * i + 2]);
      return new float[] {inVertices[3 * i + 0], inVertices[3 * i + 1], inVertices[3 * i + 2]};
    } else {
      System.err.println("Wrong index");
      return null;
    }
  }

  public float[] getVertexes() {
    return getVertexes(0, inVertexCount - 1);
  }

  public float[] getVertexes(int i0, int i1) {
    return getVertexes(i0, i1, null);
  }

  public float[] getVertexes(int i0, int i1, float[] data) {
    if (0 <= i0 && i0 <= i1 && i1 - i0 < inVertexCount) {
      int n = i1 - i0 + 1;

      if (data == null || data.length != 3 * n) {        
        data = new float[3 * n];
      }

      PApplet.arrayCopy(inVertices, 3 * i0, data, 0, 3 * n);
    } else {
      System.err.println("Wrong indexes");      
    }
    return data;
  }  

  public void setVertex(int i, float x, float y, float z) {
    if (0 <= i && i < inVertexCount) {
      inVertices[3 * i + 0] = x; 
      inVertices[3 * i + 1] = y; 
      inVertices[3 * i + 2] = z;      
      modified = true;
    } else {
      System.err.println("Wrong index");
    }    
  }

  public void setVertexes(float[] data) {
    setVertexes(data, 0, inVertexCount - 1);
  }  

  public void setVertexes(float[] data, int i0, int i1) {
    if (data == null) {
      System.err.println("null data");
      return;
    }

    if (0 <= i0 && i0 <= i1 && i1 - i0 < inVertexCount) {
      int n = i1 - i0 + 1;
      if (data.length == 3 * n) {        
        PApplet.arrayCopy(data, 0, inVertices, 3 * i0, 3 * i1);  
      } else {
        System.err.println("Wrong array length");  
      }

      modified = true;
    } else {
      System.err.println("Wrong indexes");
    }
  }
 

  // Save geometry to DFX/OBJ/BIN (raw 3D coordinates), PDF (lighted (?), transformed, projected)  
  // Flexible enough to other formats can be added easily later.
  public void save(String filename) {
    if (family == GROUP) {
      // Put all child shapes together into a single file.
    } else {
      // ...
    }

    // 
  }  

  protected PTessellator ptess;
  

  // The huber-tessellator is here. Called automatically when
  // rendering the shape.
  protected void tessellate() {

    if (family == GROUP) {
      for (int i = 0; i < childCount; i++) {
        PShape3D child = (PShape3D)children[i];
        child.tessellate();
      }      
    } else {    
      if (modified) {
        if (family == GEOMETRY) {
          if (kind == POINTS) {
            tessellatePoints();
          } else if (kind == LINES) {
            tessellateLines();            
          } else if (kind == TRIANGLES) {
            tessellateTriangles();
          } else if (kind == TRIANGLE_FAN) {
            tessellateTriangleFan();
          } else if (kind == TRIANGLE_STRIP) {
            tessellateTriangleStrip();
          } else if (kind == QUADS) {
            tessellateQuads();
          } else if (kind == QUAD_STRIP) {
            tessellateQuadStrip();
          } else if (kind == POLYGON) {
            tessellatePolygon();
          }
        } else if (family == PRIMITIVE) {
          if (kind == POINT) {
          } else if (kind == LINE) {
          } else if (kind == TRIANGLE) {
          } else if (kind == QUAD) {
          } else if (kind == RECT) {
          } else if (kind == ELLIPSE) {
          } else if (kind == ARC) {
          } else if (kind == BOX) {
            tessellateBox();            
          } else if (kind == SPHERE) {
            tessellateSphere();
          }
        } else if (family == PATH) {
        }      

      }
    }  
    // What about geometric transformations? When they are static, could be applied
    // once to avoid rendering the shape in batches.

  }
  
  static final protected int MIN_ACCURACY = 6; 
  static final protected float sinLUT[];
  static final protected float cosLUT[];
  static final protected float SINCOS_PRECISION = 0.5f;
  static final protected int SINCOS_LENGTH = (int) (360f / SINCOS_PRECISION);
  static {
    sinLUT = new float[SINCOS_LENGTH];
    cosLUT = new float[SINCOS_LENGTH];
    for (int i = 0; i < SINCOS_LENGTH; i++) {
      sinLUT[i] = (float) Math.sin(i * DEG_TO_RAD * SINCOS_PRECISION);
      cosLUT[i] = (float) Math.cos(i * DEG_TO_RAD * SINCOS_PRECISION);
    }
  }  
  protected void tessellatePoints() {
    vertexCount = 0;    
    firstVertex = 0;
    lastVertex = 0;
    
    isStroked = false;
    
    hasPoints = true;
    
    if (strokeCap == ROUND) {
      tessellateRoundPoints();
    } else {
      tessellateSquarePoints();
    }
  }
  
  protected void tessellateRoundPoints() {
    // Each point generates a separate triangle fan. 
    // The number of triangles of each fan depends on the
    // stroke weight of the point.
    int nvertTot = 0;
    int nindTot = 0;
    for (int i = 0; i < inVertexCount; i++) {
      float w = inStroke[5 * i + 4];
      int perim = PApplet.max(MIN_ACCURACY, (int) (TWO_PI * w / 20));
      // Number of points along the perimeter plus the center point.
      int nvert = perim + 1;
      nvertTot += nvert; 
      nindTot += 3 * (nvert - 1);
    }
    
    pointVertexCount = nvertTot; 
    pointVertices = new float[3 * nvertTot];
    pointColors = new float[4 * nvertTot];
    pointNormals = new float[3 * nvertTot];
    pointAttributes = new float[2 * nvertTot];
    
    pointIndexCount = nindTot;    
    pointIndices = new int[nindTot];    
    
    int vertIdx = 0;
    int indIdx = 0;
    int attribIdx = 0;
    int vert0 = 0;
    for (int i = 0; i < inVertexCount; i++) {
      // Creating the triangle fan for each input vertex.
      float w = inStroke[5 * i + 4];
      int perim = PApplet.max(MIN_ACCURACY, (int) (TWO_PI * w / 20));
      int nvert = perim + 1;
      
      // All the tessellated vertices are identical to the center point
      for (int k = 0; k < nvert; k++) {
        PApplet.arrayCopy(inVertices, 3 * i, pointVertices, 3 * vertIdx, 3);
        PApplet.arrayCopy(inStroke, 5 * i, pointColors, 4 * vertIdx, 4);
        PApplet.arrayCopy(inNormals, 3 * i, pointNormals, 3 * vertIdx, 3);      
        vertIdx++; 
      }       
      
      // The attributes for each tessellated vertex are the displacement along
      // the circle perimeter. The point shader will read these attributes and
      // displace the vertices in screen coordinates so the circles are always
      // camera facing (bilboards)
      pointAttributes[2 * attribIdx + 0] = 0;
      pointAttributes[2 * attribIdx + 1] = 0;
      attribIdx++;
      float val = 0;
      float inc = (float) SINCOS_LENGTH / perim;      
      for (int k = 0; k < perim; k++) {
        pointAttributes[2 * attribIdx + 0] = cosLUT[(int) val] * w/2;
        pointAttributes[2 * attribIdx + 1] = sinLUT[(int) val] * w/2;
        val = (val + inc) % SINCOS_LENGTH;                
        attribIdx++;           
      }
      
      // Adding vert0 to take into account the triangles of all
      // the preceding points.
      for (int k = 1; k < nvert - 1; k++) {
        pointIndices[indIdx++] = vert0 + 0;
        pointIndices[indIdx++] = vert0 + k;
        pointIndices[indIdx++] = vert0 + k + 1;
      }
      // Final triangle between the last and first point:
      pointIndices[indIdx++] = vert0 + 0;
      pointIndices[indIdx++] = vert0 + 1;
      pointIndices[indIdx++] = vert0 + nvert - 1;      
      
      vert0 = vertIdx;
    }
    
    firstPointIndex = 0;
    lastPointIndex = pointIndexCount - 1;    
  }
  
  static final protected float[][] QUAD_SIGNS = { {-1, +1}, {-1, -1}, {+1, -1}, {+1, +1}};
  protected void tessellateSquarePoints() {
    // Each point generates a separate quad.
    int quadCount = inVertexCount;
    
    // Each quad is formed by 5 vertices, the center one
    // is the input vertex, and the other 4 define the 
    // corners (so, a triangle fan again).
    int nvertTot = 5 * quadCount;
    // So the quad is formed by 4 triangles, each requires
    // 3 indices.
    int nindTot = 12 * quadCount;
    
    pointVertexCount = nvertTot; 
    pointVertices = new float[3 * nvertTot];
    pointColors = new float[4 * nvertTot];
    pointNormals = new float[3 * nvertTot];
    pointAttributes = new float[2 * nvertTot];
    
    pointIndexCount = nindTot;    
    pointIndices = new int[nindTot];    
    
    int vertIdx = 0;
    int indIdx = 0;
    int attribIdx = 0;
    int vert0 = 0;    
    for (int i = 0; i < inVertexCount; i++) {
      int nvert = 5;
      
      for (int k = 0; k < nvert; k++) {
        PApplet.arrayCopy(inVertices, 3 * i, pointVertices, 3 * vertIdx, 3);
        PApplet.arrayCopy(inStroke, 5 * i, pointColors, 4 * vertIdx, 4);
        PApplet.arrayCopy(inNormals, 3 * i, pointNormals, 3 * vertIdx, 3);      
        vertIdx++; 
      }      
      
      // The attributes for each tessellated vertex are the displacement along
      // the quad corners. The point shader will read these attributes and
      // displace the vertices in screen coordinates so the quads are always
      // camera facing (bilboards)
      pointAttributes[2 * attribIdx + 0] = 0;
      pointAttributes[2 * attribIdx + 1] = 0;
      attribIdx++;
      float w = inStroke[5 * i + 4];
      for (int k = 0; k < 4; k++) {
        pointAttributes[2 * attribIdx + 0] = QUAD_SIGNS[k][0] * w/2;
        pointAttributes[2 * attribIdx + 1] = QUAD_SIGNS[k][1] * w/2;               
        attribIdx++;           
      }
      
      // Adding vert0 to take into account the triangles of all
      // the preceding points.
      for (int k = 1; k < nvert - 1; k++) {
        pointIndices[indIdx++] = vert0 + 0;
        pointIndices[indIdx++] = vert0 + k;
        pointIndices[indIdx++] = vert0 + k + 1;
      }
      // Final triangle between the last and first point:
      pointIndices[indIdx++] = vert0 + 0;
      pointIndices[indIdx++] = vert0 + 1;
      pointIndices[indIdx++] = vert0 + nvert - 1;  
      
      vert0 = vertIdx;      
    }
    
    firstPointIndex = 0;
    lastPointIndex = pointIndexCount - 1;    
  }
  
  protected void tessellateLines() {
    vertexCount = 0;    
    firstVertex = 0;
    lastVertex = 0;
    
    isStroked = true;
    
    int lineCount = inVertexCount / 2;
    
    // Lines are made up of 4 vertices defining the quad. 
    // Each vertex has its own offset representing the stroke weight.
    int nvert = lineCount * 4;
    strokeVertexCount = nvert; 
    strokeVertices = new float[3 * nvert];
    strokeColors = new float[4 * nvert];
    strokeNormals = new float[3 * nvert];
    strokeAttributes = new float[4 * nvert];
    
    // Each stroke line has 4 vertices, defining 2 triangles, which
    // require 3 indices to specify their connectivities.
    int nind = lineCount * 2 * 3;
    strokeIndexCount = nind;
    strokeIndices = new int[nind]; 
    
    int vcount = 0;
    int icount = 0;
    for (int ln = 0; ln < lineCount; ln++) {
      int i0 = 2 * ln + 0;
      int i1 = 2 * ln + 1;
      addStrokeLine(i0, i1, vcount, icount); vcount += 4; icount += 6;
    }    
  }

  protected void tessellateTriangles() {
    copyInDataToTessData();

    int triCount = vertexCount / 3;
    
    useIndices = true;
    indexCount = vertexCount;    
    indices = new int[indexCount];
    for (int i = 0; i < indexCount; i++) {
      indices[i] = i;
    }
    firstIndex = 0;
    lastIndex = indexCount - 1;

    // Count how many triangles in this shape
    // are stroked.
    int strokedCount = 0;
    for (int tr = 0; tr < triCount; tr++) {
      int i0 = 3 * tr + 0;
      int i1 = 3 * tr + 1;
      int i2 = 3 * tr + 2;
      
      if (0 < inStroke[5 * i0 + 4] || 
          0 < inStroke[5 * i1 + 4] ||
          0 < inStroke[5 * i2 + 4]) {
        strokedCount++;
      }      
    }
    
    if (0 < strokedCount) {
      isStroked = true;
      
      // Each stroked triangle has 3 lines, one for each edge. 
      // These lines are made up of 4 vertices defining the quad. 
      // Each vertex has its own offset representing the stroke weight.
      int nvert = strokedCount * 3 * 4;
      strokeVertexCount = nvert; 
      strokeVertices = new float[3 * nvert];
      strokeColors = new float[4 * nvert];
      strokeNormals = new float[3 * nvert];
      strokeAttributes = new float[4 * nvert];
      
      // Each stroke line has 4 vertices, defining 2 triangles, which
      // require 3 indices to specify their connectivities.
      int nind = strokedCount * 3 * 2 * 3;
      strokeIndexCount = nind;
      strokeIndices = new int[nind]; 
      
      int vcount = 0;
      int icount = 0;
      for (int tr = 0; tr < triCount; tr++) {
        int i0 = 3 * tr + 0;
        int i1 = 3 * tr + 1;
        int i2 = 3 * tr + 2;        

        if (0 < inStroke[5 * i0 + 4] || 
            0 < inStroke[5 * i1 + 4] ||
            0 < inStroke[5 * i2 + 4]) {
          addStrokeLine(i0, i1, vcount, icount); vcount += 4; icount += 6;
          addStrokeLine(i1, i2, vcount, icount); vcount += 4; icount += 6;
          addStrokeLine(i2, i0, vcount, icount); vcount += 4; icount += 6;
        }
      }
      
    } else {
      isStroked = false;
    }
  }
    
  protected void tessellateTriangleFan() {
    copyInDataToTessData();
    
    int triCount = vertexCount - 2;
    
    useIndices = true;
    // Each vertex, except the first and last, defines a triangle.
    indexCount = 3 * triCount;    
    indices = new int[indexCount];
    int idx = 0;
    for (int i = 1; i < vertexCount - 1; i++) {
      indices[idx++] = 0;
      indices[idx++] = i;
      indices[idx++] = i + 1;
    }
    firstIndex = 0;
    lastIndex = indexCount - 1;
    
    // Count how many triangles in this shape
    // are stroked.
    int strokedCount = 0;
    for (int i = 1; i < vertexCount - 1; i++) {
      int i0 = 0;
      int i1 = i;
      int i2 = i + 1;
      
      if (0 < inStroke[5 * i0 + 4] || 
          0 < inStroke[5 * i1 + 4] ||
          0 < inStroke[5 * i2 + 4]) {
        strokedCount++;
      }      
    }    
    
    if (0 < strokedCount) {
      isStroked = true;
      
      // Each stroked triangle has 3 lines, one for each edge. 
      // These lines are made up of 4 vertices defining the quad. 
      // Each vertex has its own offset representing the stroke weight.
      int nvert = strokedCount * 3 * 4;
      strokeVertexCount = nvert; 
      strokeVertices = new float[3 * nvert];
      strokeColors = new float[4 * nvert];
      strokeNormals = new float[3 * nvert];
      strokeAttributes = new float[4 * nvert];
      
      // Each stroke line has 4 vertices, defining 2 triangles, which
      // require 3 indices to specify their connectivities.
      int nind = strokedCount * 3 * 2 * 3;
      strokeIndexCount = nind;
      strokeIndices = new int[nind]; 
      
      int vcount = 0;
      int icount = 0;
      for (int i = 1; i < vertexCount - 1; i++) {
        int i0 = 0;
        int i1 = i;
        int i2 = i + 1;     

        if (0 < inStroke[5 * i0 + 4] || 
            0 < inStroke[5 * i1 + 4] ||
            0 < inStroke[5 * i2 + 4]) {
          addStrokeLine(i0, i1, vcount, icount); vcount += 4; icount += 6;
          addStrokeLine(i1, i2, vcount, icount); vcount += 4; icount += 6;
          addStrokeLine(i2, i0, vcount, icount); vcount += 4; icount += 6;
        }
      }
    } else {
      isStroked = false;
    }
  }
  
  protected void tessellateTriangleStrip() {
    copyInDataToTessData();
    
    int triCount = vertexCount - 2;
    
    useIndices = true;
    // Each vertex, except the first and last, defines a triangle.
    indexCount = 3 * triCount;    
    indices = new int[indexCount];
    int idx = 0;
    for (int i = 1; i < vertexCount - 1; i++) {
      indices[idx++] = i;
      if (i % 2 == 0) {
        indices[idx++] = i - 1;  
        indices[idx++] = i + 1;
      } else {
        indices[idx++] = i + 1;  
        indices[idx++] = i - 1;
      }
    }
    firstIndex = 0;
    lastIndex = indexCount - 1;    
    
    // Count how many triangles in this shape
    // are stroked.
    int strokedCount = 0;
    for (int i = 1; i < vertexCount - 1; i++) {
      int i0 = i;
      int i1, i2;
      if (i % 2 == 0) {
        i1 = i - 1;
        i2 = i + 1;        
      } else {
        i1 = i + 1;
        i2 = i - 1;        
      }
      
      if (0 < inStroke[5 * i0 + 4] || 
          0 < inStroke[5 * i1 + 4] ||
          0 < inStroke[5 * i2 + 4]) {
        strokedCount++;
      }      
    } 
    
    if (0 < strokedCount) {
      isStroked = true;
      
      // Each stroked triangle has 3 lines, one for each edge. 
      // These lines are made up of 4 vertices defining the quad. 
      // Each vertex has its own offset representing the stroke weight.
      int nvert = strokedCount * 3 * 4;
      strokeVertexCount = nvert; 
      strokeVertices = new float[3 * nvert];
      strokeColors = new float[4 * nvert];
      strokeNormals = new float[3 * nvert];
      strokeAttributes = new float[4 * nvert];
      
      // Each stroke line has 4 vertices, defining 2 triangles, which
      // require 3 indices to specify their connectivities.
      int nind = strokedCount * 3 * 2 * 3;
      strokeIndexCount = nind;
      strokeIndices = new int[nind]; 
      
      int vcount = 0;
      int icount = 0;
      for (int i = 1; i < vertexCount - 1; i++) {
        int i0 = i;
        int i1, i2;
        if (i % 2 == 0) {
          i1 = i - 1;
          i2 = i + 1;        
        } else {
          i1 = i + 1;
          i2 = i - 1;        
        }  

        if (0 < inStroke[5 * i0 + 4] || 
            0 < inStroke[5 * i1 + 4] ||
            0 < inStroke[5 * i2 + 4]) {
          addStrokeLine(i0, i1, vcount, icount); vcount += 4; icount += 6;
          addStrokeLine(i1, i2, vcount, icount); vcount += 4; icount += 6;
          addStrokeLine(i2, i0, vcount, icount); vcount += 4; icount += 6;
        }
      }
    } else {
      isStroked = false;
    }    
  }

  protected void tessellateQuads() {
    copyInDataToTessData();

    int quadCount = vertexCount / 4;
    
    useIndices = true;
    indexCount = 6 * quadCount;    
    indices = new int[indexCount];
    int idx = 0;
    for (int qd = 0; qd < quadCount; qd++) {      
      int i0 = 4 * qd + 0;
      int i1 = 4 * qd + 1;
      int i2 = 4 * qd + 2;
      int i3 = 4 * qd + 3;
      
      indices[idx++] = i0;
      indices[idx++] = i1;
      indices[idx++] = i3;
      
      indices[idx++] = i1;
      indices[idx++] = i2;
      indices[idx++] = i3;      
    }
    firstIndex = 0;
    lastIndex = indexCount - 1;

    // Count how many quads in this shape
    // are stroked.
    int strokedCount = 0;
    for (int qd = 0; qd < quadCount; qd++) {
      int i0 = 4 * qd + 0;
      int i1 = 4 * qd + 1;
      int i2 = 4 * qd + 2;
      int i3 = 4 * qd + 3;
      
      if (0 < inStroke[5 * i0 + 4] || 
          0 < inStroke[5 * i1 + 4] ||
          0 < inStroke[5 * i2 + 4]||
          0 < inStroke[5 * i3 + 4]) {
        strokedCount++;
      }      
    }
    
    if (0 < strokedCount) {
      isStroked = true;
      
      // Each stroked quad has 4 lines, one for each edge. 
      // These lines are made up of 4 vertices defining the quad. 
      // Each vertex has its own offset representing the stroke weight.
      int nvert = strokedCount * 4 * 4;
      strokeVertexCount = nvert; 
      strokeVertices = new float[3 * nvert];
      strokeColors = new float[4 * nvert];
      strokeNormals = new float[3 * nvert];
      strokeAttributes = new float[4 * nvert];
      
      // Each stroke line has 4 vertices, defining 2 triangles, which
      // require 3 indices to specify their connectivities.
      int nind = strokedCount * 4 * 2 * 3;
      strokeIndexCount = nind;
      strokeIndices = new int[nind]; 
      
      int vcount = 0;
      int icount = 0;
      for (int qd = 0; qd < quadCount; qd++) {
        int i0 = 4 * qd + 0;
        int i1 = 4 * qd + 1;
        int i2 = 4 * qd + 2;
        int i3 = 4 * qd + 3;       

        if (0 < inStroke[5 * i0 + 4] || 
            0 < inStroke[5 * i1 + 4] ||
            0 < inStroke[5 * i2 + 4]||
            0 < inStroke[5 * i3 + 4]) {
          addStrokeLine(i0, i1, vcount, icount); vcount += 4; icount += 6;
          addStrokeLine(i1, i2, vcount, icount); vcount += 4; icount += 6;
          addStrokeLine(i2, i3, vcount, icount); vcount += 4; icount += 6;
          addStrokeLine(i3, i0, vcount, icount); vcount += 4; icount += 6;
        }
      }
      
    } else {
      isStroked = false;
    }
  }
  
  protected void tessellateQuadStrip() {
    copyInDataToTessData();

    int quadCount = vertexCount / 2 - 1;
    
    useIndices = true;
    indexCount = 6 * quadCount;    
    indices = new int[indexCount];
    int idx = 0;
    for (int qd = 1; qd < vertexCount / 2; qd++) {
      int i0 = 2 * (qd - 1);
      int i1 = 2 * (qd - 1) + 1;
      int i2 = 2 * qd + 1;
      int i3 = 2 * qd;      
      
      indices[idx++] = i0;
      indices[idx++] = i1;
      indices[idx++] = i3;
      
      indices[idx++] = i1;
      indices[idx++] = i2;
      indices[idx++] = i3;      
    }
    firstIndex = 0;
    lastIndex = indexCount - 1;

    // Count how many quads in this shape
    // are stroked.
    int strokedCount = 0;
    for (int qd = 1; qd < vertexCount / 2; qd++) {
      int i0 = 2 * (qd - 1);
      int i1 = 2 * (qd - 1) + 1;
      int i2 = 2 * qd + 1;
      int i3 = 2 * qd;
      
      if (0 < inStroke[5 * i0 + 4] || 
          0 < inStroke[5 * i1 + 4] ||
          0 < inStroke[5 * i2 + 4]||
          0 < inStroke[5 * i3 + 4]) {
        strokedCount++;
      }      
    }
    
    if (0 < strokedCount) {
      isStroked = true;
      
      // Each stroked quad has 4 lines, one for each edge. 
      // These lines are made up of 4 vertices defining the quad. 
      // Each vertex has its own offset representing the stroke weight.
      int nvert = strokedCount * 4 * 4;
      strokeVertexCount = nvert; 
      strokeVertices = new float[3 * nvert];
      strokeColors = new float[4 * nvert];
      strokeNormals = new float[3 * nvert];
      strokeAttributes = new float[4 * nvert];
      
      // Each stroke line has 4 vertices, defining 2 triangles, which
      // require 3 indices to specify their connectivities.
      int nind = strokedCount * 4 * 2 * 3;
      strokeIndexCount = nind;
      strokeIndices = new int[nind]; 
      
      int vcount = 0;
      int icount = 0;
      for (int qd = 1; qd < vertexCount / 2; qd++) {
        int i0 = 2 * (qd - 1);
        int i1 = 2 * (qd - 1) + 1;
        int i2 = 2 * qd + 1;
        int i3 = 2 * qd;     

        if (0 < inStroke[5 * i0 + 4] || 
            0 < inStroke[5 * i1 + 4] ||
            0 < inStroke[5 * i2 + 4]||
            0 < inStroke[5 * i3 + 4]) {
          addStrokeLine(i0, i1, vcount, icount); vcount += 4; icount += 6;
          addStrokeLine(i1, i2, vcount, icount); vcount += 4; icount += 6;
          addStrokeLine(i2, i3, vcount, icount); vcount += 4; icount += 6;
          addStrokeLine(i3, i0, vcount, icount); vcount += 4; icount += 6;
        }
      }
      
    } else {
      isStroked = false;
    }
  }  
  
  protected void tessellatePolygon() {
    vertexCount = 0;     
    useIndices = true;
    indexCount = 0; 
    
    vertices = new float[3 * DEFAULT_VERTICES];  
    texcoords = new float[2 * DEFAULT_VERTICES];
    normals = new float[3 * DEFAULT_VERTICES];
    colors = new float[4 * DEFAULT_VERTICES];
    indices = new int[DEFAULT_INDICES];
    
    GLU.gluTessBeginPolygon(tobj, null);
    
    if (isSolid) {
      // Using NONZERO winding rule for solid polygons.
      GLU.gluTessProperty(tobj, GLU.GLU_TESS_WINDING_RULE, GLU.GLU_TESS_WINDING_NONZERO);      
    } else {
      // Using ODD winding rule to generate polygon with holes.
      GLU.gluTessProperty(tobj, GLU.GLU_TESS_WINDING_RULE, GLU.GLU_TESS_WINDING_ODD);      
    }

    GLU.gluTessBeginContour(tobj);    
    
    // Now, iterate over all input data and send to GLU tessellator..
    for (int i = 0; i < inVertexCount; i++) {
      boolean breakPt = inVertexTypes[i] == BREAK_POINT;      
      if (breakPt) {
        GLU.gluTessEndContour(tobj);  
        GLU.gluTessBeginContour(tobj);
      }
      
      // Vertex data includes coordinates, colors, normals and texture coordinates.
      double[] vertex = new double[] { inVertices[3 * i + 0], inVertices[3 * i + 1], inVertices[3 * i + 2],
                                       inColors[4 * i + 0], inColors[4 * i + 1], inColors[4 * i + 2], inColors[4 * i + 3],
                                       inNormals[3 * i + 0], inNormals[3 * i + 1], inNormals[3 * i + 2],
                                       inTexCoords[2 * i + 0], inTexCoords[2 * i + 1] };
      GLU.gluTessVertex(tobj, vertex, 0, vertex);
    }
    
    GLU.gluTessEndContour(tobj);
    
    GLU.gluTessEndPolygon(tobj); 
           
    firstVertex = 0;
    lastVertex = vertexCount - 1;    
    
    firstIndex = 0;
    lastIndex = indexCount - 1;

    // Count many how many line segments in the perimeter
    // of this polygon are stroked.
    int lineCount = 0;
    int lnCount = inVertexCount;
    if (!isClosed) {
      lnCount--;
    }
    int contour0 = 0;
    for (int ln = 0; ln < lnCount; ln++) {
      int i0 = ln;
      int i1 = ln + 1;
      if (inVertexTypes[i0] == BREAK_POINT) {
        contour0 = i0;
      }
      if ((i1 == lnCount || inVertexTypes[i1] == BREAK_POINT) && isClosed) {
        // Make line with the first vertex of the current contour.
        i0 = contour0;
        i1 = ln;
      }
      
      if (inVertexTypes[i1] != BREAK_POINT &&
          (0 < inStroke[5 * i0 + 4] || 
           0 < inStroke[5 * i1 + 4])) {
        lineCount++;
      }      
    }
    
    if (0 < lineCount) {
      isStroked = true;

      // Lines are made up of 4 vertices defining the quad. 
      // Each vertex has its own offset representing the stroke weight.
      int nvert = lineCount * 4;
      strokeVertexCount = nvert; 
      strokeVertices = new float[3 * nvert];
      strokeColors = new float[4 * nvert];
      strokeNormals = new float[3 * nvert];
      strokeAttributes = new float[4 * nvert];
      
      // Each stroke line has 4 vertices, defining 2 triangles, which
      // require 3 indices to specify their connectivities.
      int nind = lineCount * 2 * 3;
      strokeIndexCount = nind;
      strokeIndices = new int[nind]; 
      
      int vcount = 0;
      int icount = 0;
      contour0 = 0;
      for (int ln = 0; ln < lnCount; ln++) {
        int i0 = ln;
        int i1 = ln + 1;
        if (inVertexTypes[i0] == BREAK_POINT) {
          contour0 = i0;
        }
        if ((i1 == lnCount || inVertexTypes[i1] == BREAK_POINT) && isClosed) {
          // Make line with the first vertex of the current contour.
          i0 = contour0;
          i1 = ln;
        }
        
        if (inVertexTypes[i1] != BREAK_POINT &&
            (0 < inStroke[5 * i0 + 4] || 
             0 < inStroke[5 * i1 + 4])) {
          addStrokeLine(i0, i1, vcount, icount); vcount += 4; icount += 6;
        }      
      }    
    }  
  }
  
  protected class TessCallback extends GLUtessellatorCallbackAdapter {
    protected int tessFirst;
    protected int tessCount;
    protected int tessType;
    
    public void begin(int type) {
      tessFirst = vertexCount;
      tessCount = 0;
      
      switch (type) {
      case GL.GL_TRIANGLE_FAN: 
        tessType = TRIANGLE_FAN;
        break;
      case GL.GL_TRIANGLE_STRIP: 
        tessType = TRIANGLE_STRIP;
        break;
      case GL.GL_TRIANGLES: 
        tessType = TRIANGLES;
        break;
      }
    }

    public void end() {
      switch (tessType) {
      case TRIANGLE_FAN: 
        for (int i = 1; i < tessCount - 1; i++) {
          addIndex(0);
          addIndex(i);
          addIndex(i + 1);
        }       
        break;
      case TRIANGLE_STRIP: 
        for (int i = 1; i < tessCount - 1; i++) {
          addIndex(i);
          if (i % 2 == 0) {
            addIndex(i - 1);
            addIndex(i + 1);
          } else {
            addIndex(i + 1);
            addIndex(i - 1);
          }
        }        
        break;
      case TRIANGLES: 
        for (int i = 0; i < tessCount; i++) {
          addIndex(i);          
        }
        break;
      }
    }
    
    protected void addIndex(int tessIdx) {
      indexCheck();
      indices[indexCount] = tessFirst + tessIdx;
      indexCount++;      
    }

    public void vertex(Object data) {
      if (data instanceof double[]) {
        double[] d = (double[]) data;
        if (d.length < 12) {
          throw new RuntimeException("TessCallback vertex() data " +
                                     "isn't length 12");
        }

        tessCheck();
        
        // vertex coordinates
        vertices[3 * vertexCount + 0] = (float) d[0];
        vertices[3 * vertexCount + 1] = (float) d[1];
        vertices[3 * vertexCount + 2] = (float) d[2];
        
        // vertex color
        colors[4 * vertexCount + 0] = (float) d[3];
        colors[4 * vertexCount + 1] = (float) d[4];
        colors[4 * vertexCount + 2] = (float) d[5];
        colors[4 * vertexCount + 3] = (float) d[6];
        
        // vertex normal
        normals[3 * vertexCount + 0] = (float) d[7];
        normals[3 * vertexCount + 1] = (float) d[8];
        normals[3 * vertexCount + 2] = (float) d[9];

        // texture coordinates
        texcoords[2 * vertexCount + 0] = (float) d[10];
        texcoords[2 * vertexCount + 1] = (float) d[11];
  
        tessCount++;
        vertexCount++;
      } else {
        throw new RuntimeException("TessCallback vertex() data not understood");
      }
    }

    public void error(int errnum) {
      String estring = ogl.glu.gluErrorString(errnum);
      PGraphics.showWarning("Tessellation Error: " + estring);
    }
    
    /**
     * Implementation of the GLU_TESS_COMBINE callback.
     * @param coords is the 3-vector of the new vertex
     * @param data is the vertex data to be combined, up to four elements.
     * This is useful when mixing colors together or any other
     * user data that was passed in to gluTessVertex.
     * @param weight is an array of weights, one for each element of "data"
     * that should be linearly combined for new values.
     * @param outData is the set of new values of "data" after being
     * put back together based on the weights. it's passed back as a
     * single element Object[] array because that's the closest
     * that Java gets to a pointer.
     */
    public void combine(double[] coords, Object[] data,
                        float[] weight, Object[] outData) {
      
      double[] vertex = new double[12];
      vertex[0] = coords[0];
      vertex[1] = coords[1];
      vertex[2] = coords[2];

      double[] vert0 = (double[])data[0];
      double[] vert1 = (double[])data[1];
      double[] vert2 = (double[])data[2];
      double[] vert3 = (double[])data[3];
      if (vert0 != null && vert1 != null && 
          vert2 != null && vert3 != null) {
        for (int i = 3; i < 12; i++) {
          vertex[i] = weight[0] * vert0[i] +
                      weight[1] * vert1[i] +
                      weight[2] * vert2[i] +
                      weight[3] * vert3[i];
        }        
      }
      
      // Normalizing normal vector, since the weighted 
      // combination of normal vectors is not necessarily 
      // normal.
      double sum = vertex[7] * vertex[7] + vertex[8] * vertex[8] + vertex[9] * vertex[9];
      double len = Math.sqrt(sum);      
      vertex[7] /= len; 
      vertex[8] /= len;
      vertex[9] /= len;  
      
      outData[0] = vertex;
    }
  }  
  
  protected int numBreaks() {
    int count = 0;
    for (int i = 0; i < inVertexCount; i++) {
      if (inVertexTypes[i] == BREAK_POINT) {
        count++;
      }
    }
    return count;
  }
  
  protected void tessellateBox() {
    float w = params[0];
    float h = params[0];
    float d = params[0];
        
    float x1 = -w/2f; float x2 = w/2f;
    float y1 = -h/2f; float y2 = h/2f;
    float z1 = -d/2f; float z2 = d/2f;

    // front
    setNormal(0, 0, 1);
    addVertex(x1, y1, z1, 0, 0);
    addVertex(x2, y1, z1, 1, 0);
    addVertex(x2, y2, z1, 1, 1);
    addVertex(x1, y2, z1, 0, 1);

    // right
    setNormal(1, 0, 0);
    addVertex(x2, y1, z1, 0, 0);
    addVertex(x2, y1, z2, 1, 0);
    addVertex(x2, y2, z2, 1, 1);
    addVertex(x2, y2, z1, 0, 1);

    // back
    setNormal(0, 0, -1);
    addVertex(x2, y1, z2, 0, 0);
    addVertex(x1, y1, z2, 1, 0);
    addVertex(x1, y2, z2, 1, 1);
    addVertex(x2, y2, z2, 0, 1);

    // left
    setNormal(-1, 0, 0);
    addVertex(x1, y1, z2, 0, 0);
    addVertex(x1, y1, z1, 1, 0);
    addVertex(x1, y2, z1, 1, 1);
    addVertex(x1, y2, z2, 0, 1);

    // top
    setNormal(0, 1, 0);
    addVertex(x1, y1, z2, 0, 0);
    addVertex(x2, y1, z2, 1, 0);
    addVertex(x2, y1, z1, 1, 1);
    addVertex(x1, y1, z1, 0, 1);

    // bottom
    setNormal(0, -1, 0);
    addVertex(x1, y2, z1, 0, 0);
    addVertex(x2, y2, z1, 1, 0);
    addVertex(x2, y2, z2, 1, 1);
    addVertex(x1, y2, z2, 0, 1);

    tessellateQuads();      
  }
  
  protected void tessellateSphere() {
    float r = params[0];
    int nu = (int)params[1];
    int nv = (int)params[2];
    
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
        
        setNormal(x0, y0, z0);     
        addVertex(r * x0, r * y0, r * z0, u1, v1);
   
        setNormal(x1, y1, z1);
        addVertex(r * x1,  r * y1,  r * z1, u1, v2);

        setNormal(x2, y2, z2);
        addVertex(r * x2, r * y2, r * z2, u2, v2);

        setNormal(x0, y0, z0);    
        addVertex(r * x0, r * y0, r * z0, u1, v1);

        setNormal(x2, y2, z2);
        addVertex(r * x2, r * y2, r * z2, u2, v2);
        
        setNormal(x3,  y3,  z3);
        addVertex(r * x3,  r * y3,  r * z3,  u2,  v3);
      }
    }
    
    tessellateTriangles();
  }
  
  protected void copyInDataToTessData() {
    vertexCount = inVertexCount;    
    firstVertex = 0;
    lastVertex = vertexCount - 1;

    vertices = new float[3 * inVertexCount];
    PApplet.arrayCopy(inVertices, vertices, 3 * inVertexCount);

    texcoords = new float[2 * inVertexCount];
    PApplet.arrayCopy(inTexCoords, texcoords, 2 * inVertexCount);

    colors = new float[4 * inVertexCount];
    PApplet.arrayCopy(inColors, colors, 4 * inVertexCount);

    normals = new float[3 * inVertexCount];
    PApplet.arrayCopy(inNormals, normals, 3 * inVertexCount);    
  }
  
  // Adding the data that defines a quad starting at vertex i0 and
  // ending at i1.
  protected void addStrokeLine(int i0, int i1, int vcount, int icount) {
    PApplet.arrayCopy(inVertices, 3 * i0, strokeVertices, 3 * vcount, 3);
    PApplet.arrayCopy(inNormals, 3 * i0, strokeNormals, 3 * vcount, 3);
    PApplet.arrayCopy(inStroke, 5 * i0, strokeColors, 4 * vcount, 4);    
    PApplet.arrayCopy(inVertices, 3 * i1, strokeAttributes, 4 * vcount, 3);

    strokeColors[4 * vcount + 0] = inStroke[5 * i0 + 0];
    strokeColors[4 * vcount + 1] = inStroke[5 * i0 + 1];
    strokeColors[4 * vcount + 2] =  inStroke[5 * i0 + 2];
    strokeColors[4 * vcount + 3] =  inStroke[5 * i0 + 3];
    
    strokeAttributes[4 * vcount + 3] = inStroke[5 * i0 + 4];    
    strokeIndices[icount++] = vcount;
    
    vcount++;    
    PApplet.arrayCopy(inVertices, 3 * i0, strokeVertices, 3 * vcount, 3);
    PApplet.arrayCopy(inNormals, 3 * i0, strokeNormals, 3 * vcount, 3);
    PApplet.arrayCopy(inStroke, 5 * i0, strokeColors, 4 * vcount, 4);
    PApplet.arrayCopy(inVertices, 3 * i1, strokeAttributes, 4 * vcount, 3);
    strokeAttributes[4 * vcount + 3] = -inStroke[5 * i0 + 4];
    strokeIndices[icount++] = vcount;
    
    vcount++;  
    PApplet.arrayCopy(inVertices, 3 * i1, strokeVertices, 3 * vcount, 3);
    PApplet.arrayCopy(inNormals, 3 * i1, strokeNormals, 3 * vcount, 3);
    PApplet.arrayCopy(inStroke, 5 * i1, strokeColors, 4 * vcount, 4);
    PApplet.arrayCopy(inVertices, 3 * i0, strokeAttributes, 4 * vcount, 3); 
    strokeAttributes[4 * vcount + 3] = -inStroke[5 * i1 + 4];
    strokeIndices[icount++] = vcount;
    
    // Starting a new triangle re-using prev vertices.
    strokeIndices[icount++] = vcount;
    strokeIndices[icount++] = vcount - 1;
    
    vcount++;
    PApplet.arrayCopy(inVertices, 3 * i1, strokeVertices, 3 * vcount, 3);
    PApplet.arrayCopy(inNormals, 3 * i1, strokeNormals, 3 * vcount, 3);
    PApplet.arrayCopy(inStroke, 5 * i1, strokeColors, 4 * vcount, 4);
    PApplet.arrayCopy(inVertices, 3 * i0, strokeAttributes, 4 * vcount, 3);
    strokeAttributes[4 * vcount + 3] = +inStroke[5 * i1 + 4];
    strokeIndices[icount++] = vcount;
  }
  

  
  public void addShape(PShape3D child) {
    if (family == GROUP) {
      super.addChild(child);
      child.updateRoot(root);
      modified = true;
    } else {
      System.err.println("Cannot add child shape to non-group shape.");
    }
  }
  
  private void updateRoot(PShape3D root) {
    this.root = root;
    if (family == GROUP) {
      for (int i = 0; i < childCount; i++) {
        PShape3D child = (PShape3D)children[i];
        child.updateRoot(root);
      }
    }
  }
  
  protected int copyVertOffset;
  protected int copyIndOffset;
  protected int strokeVertCopyOffset;
  protected int strokeIndCopyOffset;
  protected int pointVertCopyOffset;
  protected int pointIndCopyOffset;
  
  protected int lastVertexOffset;
  protected int lastIndexOffset;  
  protected int lastStrokeVertexOffset;
  protected int lastStrokeIndexOffset;    
  protected int lastPointVertexOffset;
  protected int lastPointIndexOffset;  

  protected void aggregate() {
    if (root == this && parent == null) {
      // We recursively calculate the total number of vertices and indices.
      lastVertexOffset = 0;
      lastIndexOffset = 0;
      
      lastStrokeVertexOffset = 0;
      lastStrokeIndexOffset = 0;

      lastPointVertexOffset = 0;
      lastPointIndexOffset = 0;      
      
      aggregateImpl();
      
      // Now that we know, we can initialize the buffers with the correct size.
      if (0 < vertexCount && 0 < indexCount) {   
        initBuffers(vertexCount, indexCount);      
        copyVertOffset = 0;
        copyIndOffset = 0;
        copyGeometryToRoot();
      }
      
      if (0 < strokeVertexCount && 0 < strokeIndexCount) {   
        initStrokeBuffers(strokeVertexCount, strokeIndexCount);
        strokeVertCopyOffset = 0;
        strokeIndCopyOffset = 0;
        copyStrokeGeometryToRoot();
      }
      
      if (0 < pointVertexCount && 0 < pointIndexCount) {   
        initPointBuffers(pointVertexCount, pointIndexCount);
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
      firstVertex = lastVertex = vertexCount = 0;
      firstIndex = lastIndex = indexCount = 0;
      
      firstStrokeVertex = lastStrokeVertex = strokeVertexCount = 0;
      firstStrokeIndex = lastStrokeIndex = strokeIndexCount = 0;     
      
      firstPointVertex = lastPointVertex = pointVertexCount = 0;
      firstPointIndex = lastPointIndex = pointIndexCount = 0;       
      
      boolean firstGeom = true;
      boolean firstStroke = true;
      boolean firstPoint = true;
      for (int i = 0; i < childCount; i++) {
        PShape3D child = (PShape3D)children[i];
        child.aggregateImpl();

        vertexCount += child.vertexCount;
        indexCount += child.indexCount;
        
        strokeVertexCount += child.strokeVertexCount;
        strokeIndexCount += child.strokeIndexCount;        

        pointVertexCount += child.pointVertexCount;
        pointIndexCount += child.pointIndexCount;          
        
        if (0 < child.vertexCount) {
          if (firstGeom) {
            firstVertex = child.firstVertex;
            firstIndex = child.firstIndex;
            firstGeom = false;
          }

          lastVertex = child.lastVertex;
          lastIndex = child.lastIndex;          
        }  

        if (0 < child.strokeVertexCount) {
          if (firstStroke) {
            firstStrokeVertex = child.firstStrokeVertex;
            firstStrokeIndex = child.firstStrokeIndex;
            firstStroke = false;
          }
          
          lastStrokeVertex = child.lastStrokeVertex;
          lastStrokeIndex = child.lastStrokeIndex;
        }
        
        if (0 < child.pointVertexCount) {
          if (firstPoint) {
            firstPointVertex = child.firstPointVertex;
            firstPointIndex = child.firstPointIndex;
            firstPoint = false;
          }
          
          lastPointVertex = child.lastPointVertex;
          lastPointIndex = child.lastPointIndex;
        }           
      }

      isStroked = 0 < strokeVertexCount && 0 < strokeIndexCount; 
      hasPoints = 0 < pointVertexCount && 0 < pointIndexCount;
    } else {
      // Shape holding some filled geometry ----------------------------------------      
      if (0 < vertexCount) {
        firstVertex = 0;
        if (0 < root.lastVertexOffset) {
          firstVertex = root.lastVertexOffset + 1; 
        }
        lastVertex = firstVertex + vertexCount - 1;        
        root.lastVertexOffset = lastVertex; 
      }      
      
      if (0 < indexCount) {
        firstIndex = 0;
        if (0 < root.lastIndexOffset) {
          firstIndex = root.lastIndexOffset + 1; 
        }
        
        // The indices are update to take into account all the previous 
        // shapes in the hierarchy, as the entire geometry will be stored
        // contiguously in a single VBO in the root node.
        for (int i = 0; i < indexCount; i++) {
          indices[i] += firstVertex;
        }
        lastIndex = firstIndex + indexCount - 1;        
        root.lastIndexOffset = lastIndex; 
      }
      
      // Shape holding some stroke geometry ----------------------------------------      
      if (0 < strokeVertexCount) {
        firstStrokeVertex = 0;
        if (0 < root.lastStrokeVertexOffset) {
          firstStrokeVertex = root.lastStrokeVertexOffset + 1; 
        }        
        lastStrokeVertex = firstStrokeVertex + strokeVertexCount - 1;
        root.lastStrokeVertexOffset = lastStrokeVertex;
      }
      
      if (0 < strokeIndexCount) {
        firstStrokeIndex = 0;
        if (0 < root.lastStrokeIndexOffset) {
          firstStrokeIndex = root.lastStrokeIndexOffset + 1; 
        }        
        
        // The indices are update to take into account all the previous 
        // shapes in the hierarchy, as the entire geometry will be stored
        // contiguously in a single VBO in the root node.
        for (int i = 0; i < strokeIndexCount; i++) {
          strokeIndices[i] += firstStrokeVertex;
        }
        lastStrokeIndex = firstStrokeIndex + strokeIndexCount - 1;
        root.lastStrokeIndexOffset = lastStrokeIndex;
      }
      
      // Shape holding some point geometry ----------------------------------------      
      if (0 < pointVertexCount) {
        firstPointVertex = 0;
        if (0 < root.lastPointVertexOffset) {
          firstPointVertex = root.lastPointVertexOffset + 1; 
        }        
        lastPointVertex = firstPointVertex + pointVertexCount - 1;
        root.lastPointVertexOffset = lastPointVertex;
      }
      
      if (0 < pointIndexCount) {
        firstPointIndex = 0;
        if (0 < root.lastPointIndexOffset) {
          firstPointIndex = root.lastPointIndexOffset + 1; 
        }        
        
        // The indices are update to take into account all the previous 
        // shapes in the hierarchy, as the entire geometry will be stored
        // contiguously in a single VBO in the root node.
        for (int i = 0; i < pointIndexCount; i++) {
          pointIndices[i] += firstPointVertex;
        }
        lastPointIndex = firstPointIndex + pointIndexCount - 1;
        root.lastPointIndexOffset = lastPointIndex;
      }      
    }  
  }
  
  protected void initBuffers(int nvert, int nind) {
    glVertexBufferID = ogl.createVertexBufferObject();    
    getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, glVertexBufferID);    
    getGl().glBufferData(GL.GL_ARRAY_BUFFER, nvert * 3 * PGraphicsOpenGL.SIZEOF_FLOAT, null, glUsage);    
    getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, 0);    
    
    glColorBufferID = ogl.createVertexBufferObject();
    getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, glColorBufferID);
    getGl().glBufferData(GL.GL_ARRAY_BUFFER, nvert * 4 * PGraphicsOpenGL.SIZEOF_FLOAT, null, glUsage);    
    getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, 0);    

    glNormalBufferID = ogl.createVertexBufferObject();
    getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, glNormalBufferID);
    getGl().glBufferData(GL.GL_ARRAY_BUFFER, nvert * 3 * PGraphicsOpenGL.SIZEOF_FLOAT, null, glUsage);
    getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, 0);    
    
    
    if (glTexCoordBufferID == null) {
      glTexCoordBufferID = new int[PGraphicsOpenGL.MAX_TEXTURES];
      java.util.Arrays.fill(glTexCoordBufferID, 0);      
    }
        
    numTexBuffers = 1;
    for (int i = 0; i < numTexBuffers; i++) {
      glTexCoordBufferID[i] = ogl.createVertexBufferObject();
      getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, glTexCoordBufferID[i]);
      getGl().glBufferData(GL.GL_ARRAY_BUFFER, nvert * 2 * PGraphicsOpenGL.SIZEOF_FLOAT, null, glUsage);
      getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
    }    
    
    glIndexBufferID = ogl.createVertexBufferObject();    
    getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, glIndexBufferID);    
    getGl().glBufferData(GL.GL_ARRAY_BUFFER, nind * PGraphicsOpenGL.SIZEOF_INT, null, glUsage);    
    getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, 0);         
  }
  
  protected void initStrokeBuffers(int nvert, int nind) {
    glStrokeVertexBufferID = ogl.createVertexBufferObject();    
    getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, glStrokeVertexBufferID);    
    getGl().glBufferData(GL.GL_ARRAY_BUFFER, nvert * 3 * PGraphicsOpenGL.SIZEOF_FLOAT, null, glUsage);    
    getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, 0);    
    
    glStrokeNormalBufferID = ogl.createVertexBufferObject();    
    getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, glStrokeNormalBufferID);    
    getGl().glBufferData(GL.GL_ARRAY_BUFFER, nvert * 3 * PGraphicsOpenGL.SIZEOF_FLOAT, null, glUsage);    
    getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
    
    glStrokeColorBufferID = ogl.createVertexBufferObject();
    getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, glStrokeColorBufferID);
    getGl().glBufferData(GL.GL_ARRAY_BUFFER, nvert * 4 * PGraphicsOpenGL.SIZEOF_FLOAT, null, glUsage);    
    getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, 0);

    glStrokeAttribBufferID = ogl.createVertexBufferObject();
    getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, glStrokeAttribBufferID);   
    getGl().glBufferData(GL.GL_ARRAY_BUFFER, nvert * 4 * PGraphicsOpenGL.SIZEOF_FLOAT, null, glUsage);
    getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
    
    glStrokeIndexBufferID = ogl.createVertexBufferObject();    
    getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, glStrokeIndexBufferID);    
    getGl().glBufferData(GL.GL_ARRAY_BUFFER, nind * PGraphicsOpenGL.SIZEOF_INT, null, glUsage);    
    getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, 0);         
  }
  
  protected void initPointBuffers(int nvert, int nind) {
    glPointVertexBufferID = ogl.createVertexBufferObject();    
    getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, glPointVertexBufferID);    
    getGl().glBufferData(GL.GL_ARRAY_BUFFER, nvert * 3 * PGraphicsOpenGL.SIZEOF_FLOAT, null, glUsage);    
    getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, 0);    
    
    glPointNormalBufferID = ogl.createVertexBufferObject();    
    getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, glPointNormalBufferID);    
    getGl().glBufferData(GL.GL_ARRAY_BUFFER, nvert * 3 * PGraphicsOpenGL.SIZEOF_FLOAT, null, glUsage);    
    getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
    
    glPointColorBufferID = ogl.createVertexBufferObject();
    getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, glPointColorBufferID);
    getGl().glBufferData(GL.GL_ARRAY_BUFFER, nvert * 4 * PGraphicsOpenGL.SIZEOF_FLOAT, null, glUsage);    
    getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, 0);

    glPointAttribBufferID = ogl.createVertexBufferObject();
    getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, glPointAttribBufferID);   
    getGl().glBufferData(GL.GL_ARRAY_BUFFER, nvert * 4 * PGraphicsOpenGL.SIZEOF_FLOAT, null, glUsage);
    getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
    
    glPointIndexBufferID = ogl.createVertexBufferObject();    
    getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, glPointIndexBufferID);    
    getGl().glBufferData(GL.GL_ARRAY_BUFFER, nind * PGraphicsOpenGL.SIZEOF_INT, null, glUsage);    
    getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, 0);         
  }  
  
  protected void copyGeometryToRoot() {
    if (family == GROUP) {
      for (int i = 0; i < childCount; i++) {
        PShape3D child = (PShape3D)children[i];
        child.copyGeometryToRoot();
      }    
    } else {
      if (0 < vertexCount && 0 < indexCount) {
        root.copyGeometry(root.copyVertOffset, vertexCount, vertices, texcoords, colors, normals, indices);
        root.copyVertOffset += vertexCount;
      
        root.copyIndices(root.copyIndOffset, indexCount, indices);
        root.copyIndOffset += indexCount;
      }
    }
  }
  
  protected void copyGeometry(int offset, int size, float[] vertices, float[] texcoords, float[] colors, 
                                                   float[] normals, int[] indices) {
    getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, glVertexBufferID);
    getGl().glBufferSubData(GL.GL_ARRAY_BUFFER, 3 * offset * PGraphicsOpenGL.SIZEOF_FLOAT, 
                            3 * size * PGraphicsOpenGL.SIZEOF_FLOAT, FloatBuffer.wrap(vertices));
    getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
    
    getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, glTexCoordBufferID[0]);
    getGl().glBufferSubData(GL.GL_ARRAY_BUFFER, 2 * offset * PGraphicsOpenGL.SIZEOF_FLOAT, 
                            2 * size * PGraphicsOpenGL.SIZEOF_FLOAT, FloatBuffer.wrap(texcoords));
    getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, 0);    

    getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, glColorBufferID);
    getGl().glBufferSubData(GL.GL_ARRAY_BUFFER, 4 * offset * PGraphicsOpenGL.SIZEOF_FLOAT, 
                            4 * size * PGraphicsOpenGL.SIZEOF_FLOAT, FloatBuffer.wrap(colors));
    getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, 0);    
    
    getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, glNormalBufferID);
    getGl().glBufferSubData(GL.GL_ARRAY_BUFFER, 3 * offset * PGraphicsOpenGL.SIZEOF_FLOAT, 
                            3 * size * PGraphicsOpenGL.SIZEOF_FLOAT, FloatBuffer.wrap(normals));
    getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, 0);      
  }
  
  protected void copyIndices(int offset, int size, int[] indices) {
    getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, glIndexBufferID);
    getGl().glBufferSubData(GL.GL_ARRAY_BUFFER, offset * PGraphicsOpenGL.SIZEOF_INT, 
                            size * PGraphicsOpenGL.SIZEOF_INT, IntBuffer.wrap(indices));
    getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, 0);   
  }  
  
  protected void copyStrokeGeometryToRoot() {
    if (family == GROUP) {
      for (int i = 0; i < childCount; i++) {
        PShape3D child = (PShape3D)children[i];
        child.copyStrokeGeometryToRoot();
      }    
    } else {
      if (isStroked) {
        root.copyStrokeGeometry(root.strokeVertCopyOffset, strokeVertexCount, strokeVertices, strokeColors, strokeNormals, strokeAttributes);        
        root.strokeVertCopyOffset += strokeVertexCount;
        
        root.copyStrokeIndices(root.strokeIndCopyOffset, strokeIndexCount, strokeIndices);
        root.strokeIndCopyOffset += strokeIndexCount;        
      }
    }
  }
  
  protected void copyStrokeGeometry(int offset, int size, float[] vertices, float[] colors, float[] normals, float[] attribs) {
    getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, glStrokeVertexBufferID);
    getGl().glBufferSubData(GL.GL_ARRAY_BUFFER, 3 * offset * PGraphicsOpenGL.SIZEOF_FLOAT, 
                            3 * size * PGraphicsOpenGL.SIZEOF_FLOAT, FloatBuffer.wrap(vertices));
    getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, 0);

    getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, glStrokeNormalBufferID);
    getGl().glBufferSubData(GL.GL_ARRAY_BUFFER, 3 * offset * PGraphicsOpenGL.SIZEOF_FLOAT, 
                            3 * size * PGraphicsOpenGL.SIZEOF_FLOAT, FloatBuffer.wrap(normals));
    getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, 0);    
    
    getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, glStrokeColorBufferID);
    getGl().glBufferSubData(GL.GL_ARRAY_BUFFER, 4 * offset * PGraphicsOpenGL.SIZEOF_FLOAT, 
                            4 * size * PGraphicsOpenGL.SIZEOF_FLOAT, FloatBuffer.wrap(colors));
    getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, 0);    
    
    getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, glStrokeAttribBufferID);
    getGl().glBufferSubData(GL.GL_ARRAY_BUFFER, 4 * offset * PGraphicsOpenGL.SIZEOF_FLOAT, 
                            4 * size * PGraphicsOpenGL.SIZEOF_FLOAT, FloatBuffer.wrap(attribs));
    getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
  }  

  protected void copyStrokeIndices(int offset, int size, int[] indices) {
    getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, glStrokeIndexBufferID);
    getGl().glBufferSubData(GL.GL_ARRAY_BUFFER, offset * PGraphicsOpenGL.SIZEOF_INT, 
                            size * PGraphicsOpenGL.SIZEOF_INT, IntBuffer.wrap(indices));
    getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, 0);   
  }
  
  protected void copyPointGeometryToRoot() {
    if (family == GROUP) {
      for (int i = 0; i < childCount; i++) {
        PShape3D child = (PShape3D)children[i];
        child.copyPointGeometryToRoot();
      }    
    } else {
      if (hasPoints) {
        root.copyPointGeometry(root.pointVertCopyOffset, pointVertexCount, pointVertices, pointColors, pointNormals, pointAttributes);        
        root.pointVertCopyOffset += pointVertexCount;
        
        root.copyPointIndices(root.pointIndCopyOffset, pointIndexCount, pointIndices);
        root.pointIndCopyOffset += pointIndexCount;        
      }
    }
  }
  
  protected void copyPointGeometry(int offset, int size, float[] vertices, float[] colors, float[] normals, float[] attribs) {
    getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, glPointVertexBufferID);
    getGl().glBufferSubData(GL.GL_ARRAY_BUFFER, 3 * offset * PGraphicsOpenGL.SIZEOF_FLOAT, 
                            3 * size * PGraphicsOpenGL.SIZEOF_FLOAT, FloatBuffer.wrap(vertices));
    getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, 0);

    getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, glPointNormalBufferID);
    getGl().glBufferSubData(GL.GL_ARRAY_BUFFER, 3 * offset * PGraphicsOpenGL.SIZEOF_FLOAT, 
                            3 * size * PGraphicsOpenGL.SIZEOF_FLOAT, FloatBuffer.wrap(normals));
    getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, 0);    
    
    getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, glPointColorBufferID);
    getGl().glBufferSubData(GL.GL_ARRAY_BUFFER, 4 * offset * PGraphicsOpenGL.SIZEOF_FLOAT, 
                            4 * size * PGraphicsOpenGL.SIZEOF_FLOAT, FloatBuffer.wrap(colors));
    getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, 0);    
    
    getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, glPointAttribBufferID);
    getGl().glBufferSubData(GL.GL_ARRAY_BUFFER, 4 * offset * PGraphicsOpenGL.SIZEOF_FLOAT, 
                            4 * size * PGraphicsOpenGL.SIZEOF_FLOAT, FloatBuffer.wrap(attribs));
    getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
  }  

  protected void copyPointIndices(int offset, int size, int[] indices) {
    getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, glPointIndexBufferID);
    getGl().glBufferSubData(GL.GL_ARRAY_BUFFER, offset * PGraphicsOpenGL.SIZEOF_INT, 
                            size * PGraphicsOpenGL.SIZEOF_INT, IntBuffer.wrap(indices));
    getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, 0);   
  }  
  
  ////////////////////////////////////////////////////////////

  // SHAPE RECORDING HACK

  /*
  protected float normalX, normalY, normalZ;    
  protected float textureU, textureV;  
  protected float strokeR, strokeG, strokeB, strokeA;
  protected float fillR, fillG, fillB, fillA;  
  protected boolean tint;
  protected int tintColor;
  protected float tintR, tintG, tintB, tintA;
  
  public void beginRecord() {
    ogl.beginRecord(this);
  }
  
  public void endRecord() {
    ogl.endRecord();
  }
  
  public void beginShape(int kind) {
    ogl.beginShape(kind);
  }
  
  public void endShape() {
    ogl.endShape();
  }
  
  public void endShape(int mode) {
    ogl.endShape(mode);
  }
  
  public void shapeName(String name) {
    ogl.shapeName(name);
  }
  
  public void mergeShapes(boolean val) {
    ogl.mergeShapes(val);
  }  
  
  public void vertex(float x, float y, float u, float v) {
    ogl.vertex(x, y, u, v);
  }

  public void vertex(float x, float y, float z, float u, float v) {
    ogl.vertex(x, y, z, u, v);
  }
  
  public void vertex(float... values) {
    ogl.vertex(values);
  }
  
  public void normal(float nx, float ny, float nz) {
    ogl.normal(nx, ny, nz);
  }
  
  public void texture(PImage image) {
    ogl.texture(image);
  }
  
  public void texture(PImage... images) {
    ogl.texture(images);
  }

  public void sphereDetail(int res) {
    ogl.sphereDetail(res);
  }
  
  public void sphereDetail(int ures, int vres) {
    ogl.sphereDetail(ures, vres);
  }
  
  public void sphere(float r) {
    ogl.sphere(r);
  }
  
  public void box(float size) {
    ogl.box(size);
  }
  
  public void box(float w, float h, float d) {
    ogl.box(w, h, d);
  }
  
  public void noFill() {
    ogl.saveDrawingState();
    ogl.noFill();
    ogl.restoreDrawingState();
  }

  public void fill(int rgb) {
    ogl.saveDrawingState();
    ogl.fill(rgb);
    ogl.restoreDrawingState();
  }

  public void fill(int rgb, float alpha) {
    ogl.saveDrawingState();
    ogl.fill(rgb, alpha);
    ogl.restoreDrawingState();
  }

  public void fill(float gray) {
    ogl.saveDrawingState();
    ogl.fill(gray);
    ogl.restoreDrawingState();
  }

  public void fill(float gray, float alpha) {
    ogl.saveDrawingState();
    ogl.fill(gray, alpha);
    ogl.restoreDrawingState();
  }

  public void fill(float x, float y, float z) {
    ogl.saveDrawingState();
    ogl.fill(x, y, z);
    ogl.restoreDrawingState();
  }

  public void fill(float x, float y, float z, float a) {
    ogl.saveDrawingState();
    ogl.fill(x, y, z, a);    
    ogl.restoreDrawingState();
  }
    
  public void noStroke() {
    ogl.saveDrawingState();
    ogl.noStroke();
    ogl.restoreDrawingState();
  }
  
  public void stroke(int rgb) {
    ogl.saveDrawingState();
    ogl.stroke(rgb);
    ogl.restoreDrawingState();
  }

  public void stroke(int rgb, float alpha) {
    ogl.saveDrawingState();
    ogl.stroke(rgb, alpha);
    ogl.restoreDrawingState();
  }

  public void stroke(float gray) {
    ogl.saveDrawingState();
    ogl.stroke(gray);
    ogl.restoreDrawingState();
  }

  public void stroke(float gray, float alpha) {
    ogl.saveDrawingState();
    ogl.stroke(gray, alpha);
    ogl.restoreDrawingState();
  }

  public void stroke(float x, float y, float z) {
    ogl.saveDrawingState();
    ogl.stroke(x, y, z);    
    ogl.restoreDrawingState();
  }

  public void stroke(float x, float y, float z, float a) {
    ogl.saveDrawingState();
    ogl.stroke(x, y, z, a);
    ogl.restoreDrawingState();
  }  
  */
  
  /*  
  // Reference to the renderer of the main PApplet
  PGraphics g;
  
  // Fill methods
  
  public void nofill() {
    fill = false;
  }
     
  public void fill(int rgb) {
    g.colorCalc(rgb);
    fillFromCalc();
  }

  public void fill(int rgb, float alpha) {
    g.colorCalc(rgb, alpha);
    fillFromCalc();
  }

  public void fill(float gray) {
    g.colorCalc(gray);
    fillFromCalc();    
  }

  public void fill(float gray, float alpha) {
    g.colorCalc(gray, alpha);
    fillFromCalc();    
  }

  public void fill(float x, float y, float z) {
    g.colorCalc(x, y, z);
    fillFromCalc();    
  }

  public void fill(float x, float y, float z, float a) {
    g.colorCalc(x, y, z, a);
    fillFromCalc();
  }
  
  protected void fillFromCalc() {
    fill = true;
    fillColor = g.calcColor;
    fillR = g.calcR;
    fillG = g.calcG;
    fillB = g.calcB;
    fillA = g.calcA;    
  }
  
  // Stroke methods
  
  public void noStroke() {
    stroke = false; 
  }  
  
  public void stroke(int rgb) {
    g.colorCalc(rgb);
    strokeFromCalc();
  }

  public void stroke(int rgb, float alpha) {
    g.colorCalc(rgb, alpha);
    strokeFromCalc();    
  }

  public void stroke(float gray) {
    g.colorCalc(gray);
    strokeFromCalc();    
  }

  public void stroke(float gray, float alpha) {
    g.colorCalc(gray, alpha);
    strokeFromCalc();    
  }

  public void stroke(float x, float y, float z) {
    g.colorCalc(x, y, z);
    strokeFromCalc();    
  }

  public void stroke(float x, float y, float z, float a) {
    g.colorCalc(x, y, z, a);
    strokeFromCalc();
  }

  protected void strokeFromCalc() {
    stroke = true;
    strokeColor = g.calcColor;
    strokeR = g.calcR;
    strokeG = g.calcG;
    strokeB = g.calcB;
    strokeA = g.calcA;    
  }
  
  public void strokeWeight(float weight) {
    strokeWeight = weight;
  }
  
  public void strokeCap(int cap) {
    strokeCap = cap;
  }  
  
  public void strokeJoin(int join) {
    strokeJoin = join;
  }    
  
  // Tint methods

  public void noTint() {
    tint = false;
  }
  
  public void tint(int rgb) {    
    g.colorCalc(rgb);
    tintFromCalc();
  }

  public void tint(int rgb, float alpha) {
    g.colorCalc(rgb, alpha);
    tintFromCalc();
  }

  public void tint(float gray) {
    g.colorCalc(gray);
    tintFromCalc();
  }

  public void tint(float gray, float alpha) {
    g.colorCalc(gray, alpha);
    tintFromCalc(); 
  }

  public void tint(float x, float y, float z) {
    g.colorCalc(x, y, z);
    tintFromCalc(); 
  }

  public void tint(float x, float y, float z, float a) {
    g.colorCalc(x, y, z, a);
    tintFromCalc();     
  }

  protected void tintFromCalc() {
    tint = true;
    tintColor = g.calcColor;
    tintR = g.calcR;
    tintG = g.calcG;
    tintB = g.calcB;
    tintA = g.calcA;
  }  
  
  public void texture(PImage image) {
    this.image = image;
  }  
  
  public void normal(float nx, float ny, float nz) {
    normalX = nx;
    normalY = ny;
    normalZ = nz;
  }
    
  public void vertex(float x, float y) {
    vertexCheck();
    float[] vertex = vertices[vertexCount];
    
    vertex[X] = x;
    vertex[Y] = y;
    vertex[Z] = 0;
    
    if (family != PATH) {    
      boolean textured = image != null;
      if (fill || textured) {
        if (!textured) {
          vertex[R] = fillR;
          vertex[G] = fillG;
          vertex[B] = fillB;
          vertex[A] = fillA;
        } else {
          if (tint) {
            vertex[R] = tintR;
            vertex[G] = tintG;
            vertex[B] = tintB;
            vertex[A] = tintA;
          } else {
            vertex[R] = 1;
            vertex[G] = 1;
            vertex[B] = 1;
            vertex[A] = 1;
          }
        }
      }

      if (stroke) {
        vertex[SR] = strokeR;
        vertex[SG] = strokeG;
        vertex[SB] = strokeB;
        vertex[SA] = strokeA;
        vertex[SW] = strokeWeight;
      }

      if (textured) {
        vertex[U] = textureU;
        vertex[V] = textureV;
      }
    }
    
    vertexCount++;    
  }

  public void vertex(float x, float y, float z) {
  }
  
  public void vertex(float x, float y, float u, float v) {
  }
  
  public void vertex(float x, float y, float z, float u, float v) {
  }

  public void curveVertex(float x, float y) {
  }
    
  public void curveVertex(float x, float y, float z) {
  }
  
  public void bezierVertex(float x2, float y2,
                           float x3, float y3,
                           float x4, float y4) {
  }
    
  public void bezierVertex(float x2, float y2, float z2,
                           float x3, float y3, float z3,
                           float x4, float y4, float z4) {   
  }
  
  protected void vertexCheck() {
    if (vertexCount == vertices.length) {
      float temp[][] = new float[vertexCount << 1][family == PATH ? 2 : VERTEX_FIELD_COUNT];
      System.arraycopy(vertices, 0, temp, 0, vertexCount);
      vertices = temp;
    }        
  }
  
  // Call it only after setting the shape family
  protected void vertexInit() {
    vertices = new float[PGraphics.DEFAULT_VERTICES][family == PATH ? 2 : VERTEX_FIELD_COUNT];    
  }
  
  protected void vertexTexture(float u, float v) {
    if (image == null) {
      throw new RuntimeException("You must first call texture() before " +
                                 "using u and v coordinates with vertex()");
    }
    if (g.textureMode == IMAGE) {
      u /= image.width;
      v /= image.height;
    }

    textureU = u;
    textureV = v;

    if (textureU < 0) textureU = 0;
    else if (textureU > 1) textureU = 1;

    if (textureV < 0) textureV = 0;
    else if (textureV > 1) textureV = 1;
  }  

  protected int vertCount;  
  protected float[] vertices2;
  protected float[] colors;  
  protected float[] normals;
  protected float[] texcoords;
  boolean vertModified;
    
  protected float[] tvertices2;  
  protected float[] tnormals;
  boolean tvertModified;
    
  protected int indexCount;
  protected int[] indices;
  boolean indexModified;
  
  protected int paramCount;
  protected float[] params2;
  boolean paramModified;
  
  protected PMatrix3D tmatrix;
  protected boolean tmatModified;


  public void draw2(PGraphics g) {
    if (visible) {
      pre(g);
      drawImpl2(g);
      post(g);
    }
  }
  
  public void drawImpl2(PGraphics g) {
  
  
  }
  
  
  protected void vertexCheck() {
    int n = vertices2.length / 3;
    if (vertCount == n) {
      float[] vtemp = new float[3 * (vertCount << 1)];
      System.arraycopy(vertices2, 0, vtemp, 0, vertCount);      
      vertices2 = vtemp;
      
      float[] ctemp = new float[4 * (vertCount << 1)];
      System.arraycopy(colors, 0, ctemp, 0, vertCount);      
      colors = ctemp;

      float[] ntemp = new float[3 * (vertCount << 1)];
      System.arraycopy(normals, 0, ntemp, 0, vertCount);      
      normals = ntemp;
      
      float[] tctemp = new float[2 * (vertCount << 1)];
      System.arraycopy(texcoords, 0, tctemp, 0, vertCount);      
      texcoords = ntemp;
    }
  }
  
  protected void indexCheck() {
    if (indexCount == indices.length) {
      int[] temp = new int[indexCount << 1];
      System.arraycopy(indices, 0, temp, 0, indexCount);
      indices = temp;
    }  
  }

  
  void addChild(String name) {
  }
  
  
  
  void addVertex(float x, float y, float z, int rgba) {
    addVertex(x, y, z, rgba, 0, 0, 0);
  }
  
  void addVertex(float x, float y, float z, int rgba, float nx, float ny, float nz) {
    addVertex(x, y, z, rgba, nx, ny, nz, 0, 0);    
  }
  

  // Add vertex method (single texture version)
  void addVertex(float x, float y, float z, int rgba, float nx, float ny, float nz, float u, float v) {    
    // Add data to flat arrays in root node
    vertexCheck();
    
    int idx = vertCount;
    vertices2[3 * idx + 0] = x;
    vertices2[3 * idx + 1] = y;
    vertices2[3 * idx + 2] = z;

    int a = (rgba >> 24) & 0xFF;
    int r = (rgba >> 16) & 0xFF;
    int g = (rgba >> 8) & 0xFF;
    int b = (rgba  >> 0) & 0xFF;
    
    colors[4 * idx + 0] = r / 255.0f;
    colors[4 * idx + 1] = g / 255.0f;
    colors[4 * idx + 2] = b / 255.0f;
    colors[4 * idx + 3] = a / 255.0f;

    normals[3 * idx + 0] = nx;
    normals[3 * idx + 1] = ny;
    normals[3 * idx + 2] = nz;

    texcoords[2 * idx + 0] = u;
    texcoords[2 * idx + 1] = v;
           
    vertCount++;
  }
  
  protected int update(int index0) {    
    if (family == GROUP) {
      index0 = updateGroup(index0);
    } else if (family == PRIMITIVE) {
      index0 = updateImpl(index0);
    } else if (family == GEOMETRY) {
      index0 = updateImpl(index0);
    } else if (family == PATH) {
      index0 = updateImpl(index0);
    }
    return index0;
  }
  
  protected int updateGroup(int index0) {    
    for (int i = 0; i < childCount; i++) {
      index0 += children[i].update(index0);
    }
    return index0;
  }

  
  
  // This method is supposed to be called by the root shape when it is drawn, 
  // which will provide the number of indices up to this shape.
  protected int updateImpl(int index0) {
    if ((family == PATH || family == PRIMITIVE) && paramModified) {
      vertCount = 0;
      // Evaluate parameters and add vertices
      // ...
      vertModified = true;
    }
    
    index0 = updateIndex(index0);
    updateVert();
    
    return index0;
  }
  
  protected int updateIndex(int index0) {
    if (indexCount == 0) {
      // Calculate vertex indices depending on the geometry type and the root
      
      indexModified = true;
    }
    
    return index0 + vertCount;
  }
  
  protected void updateVert() {
    if (vertModified || tmatModified) {
      if (tvertices2 == null) {      
        if (tmatrix == null) {
          // When there is no transformation matrix,
          // the array of transformed vertices is set
          // as the original array, in order to save
          // memory.
          tvertices2 = vertices2;
          tnormals = normals;
        } else {
          tvertices2 = new float[vertices2.length];
          tnormals = new float[normals.length];
        }
      }
              
      if (tmatrix != null) {
        // Apply the transformation matrix on all the vertices2
        // and normals in order to obtain the transformed vertex
        // coordinates and normals.
        float x, y, z, nx, ny, nz;
        PMatrix3D tm = tmatrix;
        for (int i = 0; i < vertCount; i++) {
          x = vertices2[3 * i + 0];
          y = vertices2[3 * i + 1];
          z = vertices2[3 * i + 2];
          
          tvertices2[3 * i + 0] = x * tm.m00 + y * tm.m01 + z * tm.m02 + tm.m03;
          tvertices2[3 * i + 1] = x * tm.m10 + y * tm.m11 + z * tm.m12 + tm.m13;
          tvertices2[3 * i + 2] = x * tm.m20 + y * tm.m21 + z * tm.m22 + tm.m23;

          nx = normals[3 * i + 0];
          ny = normals[3 * i + 1];
          nz = normals[3 * i + 2];

          tnormals[3 * i + 0] = nx * tm.m00 + ny * tm.m01 + nz * tm.m02 + tm.m03;
          tnormals[3 * i + 1] = nx * tm.m10 + ny * tm.m11 + nz * tm.m12 + tm.m13;
          tnormals[3 * i + 2] = nx * tm.m20 + ny * tm.m21 + nz * tm.m22 + tm.m23;
        }
      }
      
      tvertModified = true;      
    }        
  }
  
  
  // When indices should be created.
  //indexCheck();
  //indices[indexCount] = idx;    
  //indexCount++;
  
  void setX(int i, float x) {
    
  
  }
  
  void setColor(int i, int rgba) {
    
    
  }
  
  float getX(int i) {
    return 0;  
  }
  
  int getColor(int i) {
    return 0;
  }
  */

  
  ////////////////////////////////////////////////////////////

  // load/update/set/get methods
 
  public void loadVertices() {
    updateTesselation();
        
    getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, root.glVertexBufferID);    
    FloatBuffer mbuf = getGl().glMapBuffer(GL.GL_ARRAY_BUFFER, GL.GL_WRITE_ONLY).asFloatBuffer();    

    int offset = 3 * firstVertex;
    int size = 3 * vertexCount;
    if (vertices == null || vertices.length != size) {
      vertices = new float[size];      
    }
    mbuf.position(offset);   
    mbuf.get(vertices, 0, size);
    
    getGl().glUnmapBuffer(GL.GL_ARRAY_BUFFER);
    getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, 0); 
  }  
  
  public void updateVertices() {
    int offset = 3 * firstVertex;
    int size = 3 * vertexCount;    
    
    if (vertices != null && vertices.length == size) {
      getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, root.glVertexBufferID);    
      FloatBuffer mbuf = getGl().glMapBuffer(GL.GL_ARRAY_BUFFER, GL.GL_WRITE_ONLY).asFloatBuffer();    

      mbuf.position(offset);
      mbuf.put(vertices, 0, size);
      
      getGl().glUnmapBuffer(GL.GL_ARRAY_BUFFER);
      getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, 0);      
    
    } else {
      PGraphics.showWarning("Load vertices first using loadVertices()");
    }
  }

  
  
  public void loadStrokeVertices() {
    updateTesselation();

    if (!isStroked) return;
    
    getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, root.glStrokeVertexBufferID);    
    FloatBuffer mbuf = getGl().glMapBuffer(GL.GL_ARRAY_BUFFER, GL.GL_WRITE_ONLY).asFloatBuffer();    

    int offset = 3 * firstStrokeVertex;
    int size = 3 * strokeVertexCount;
    if (strokeVertices == null || strokeVertices.length != size) {
      strokeVertices = new float[size];      
    }
    mbuf.position(offset);   
    mbuf.get(strokeVertices, 0, size);
    
    getGl().glUnmapBuffer(GL.GL_ARRAY_BUFFER);
    getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, 0); 
  }  
  
  public void updateStrokeVertices() {
    if (!isStroked) return;
    
    int offset = 3 * firstStrokeVertex;
    int size = 3 * strokeVertexCount;
    
    if (strokeVertices != null && strokeVertices.length == size) {
      getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, root.glStrokeVertexBufferID);    
      FloatBuffer mbuf = getGl().glMapBuffer(GL.GL_ARRAY_BUFFER, GL.GL_WRITE_ONLY).asFloatBuffer();    

      mbuf.position(offset);
      mbuf.put(strokeVertices, 0, size);
      
      getGl().glUnmapBuffer(GL.GL_ARRAY_BUFFER);
      getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, 0);    
    } else {
      PGraphics.showWarning("Load vertices first using loadVertices()");
    }
  }
  
  
  

  
  
  
  
  
  
  
  
  public void loadColors() {
    loadColors(firstVertex, lastVertex);
  }

  
  public void loadColors(int first, int last) {
    if (last < first || first < firstVertex || lastVertex < last) {
      PGraphics.showWarning("PShape3D: wrong vertex index");
      updateElement = -1;
      return;  
    }
    
    if (updateElement != -1) {
      PGraphics.showWarning("PShape3D: can load only one type of data at the time");
      return;        
    }
    
    updateElement = COLORS;
    firstUpdateIdx = first;
    lastUpdateIdx = last;
    
    getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, glColorBufferID);
    colorBuffer = getGl().glMapBuffer(GL.GL_ARRAY_BUFFER, GL.GL_WRITE_ONLY).asFloatBuffer();    
  }
  
  
  public void updateColors() {
    if (updateElement == COLORS) {    
      int offset = firstUpdateIdx * 4;
      int size = (lastUpdateIdx - firstUpdateIdx + 1) * 4;
          
      colorBuffer.position(0);      
      colorBuffer.put(colors, offset, size);
      colorBuffer.flip();
  
      getGl().glUnmapBuffer(GL.GL_ARRAY_BUFFER);
      getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
      
      updateElement = -1;      
    } else {
      PGraphics.showWarning("PShape3D: need to call loadColors() first");
    }    
  }

  public void loadNormals() {
    loadNormals(firstVertex, lastVertex);
  }  
  
  
  public void loadNormals(int first, int last) {
    if (last < first || first < firstVertex || lastVertex < last) {
      PGraphics.showWarning("PShape3D: wrong vertex index");
      updateElement = -1;
      return;  
    }
    
    if (updateElement != -1) {
      PGraphics.showWarning("PShape3D: can load only one type of data at the time");
      return;        
    }    
    
    updateElement = NORMALS;
    firstUpdateIdx = first;
    lastUpdateIdx = last;
    
    getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, glNormalBufferID);
    normalBuffer = getGl().glMapBuffer(GL.GL_ARRAY_BUFFER, GL.GL_WRITE_ONLY).asFloatBuffer();      
  }
  
  
  public void updateNormals() {
    if (updateElement == NORMALS) {    
      int offset = firstUpdateIdx * 3;
      int size = (lastUpdateIdx - firstUpdateIdx + 1) * 3;
      
      normalBuffer.position(0);      
      normalBuffer.put(normals, offset, size);
      normalBuffer.flip();
    
      getGl().glUnmapBuffer(GL.GL_ARRAY_BUFFER);
      getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
      
      updateElement = -1;      
    } else {
      PGraphics.showWarning("PShape3D: need to call loadNormals() first");
    }      
  }  
  
  
  public void loadTexcoords() {
    loadTexcoords(0);
  }
  
  
  public void loadTexcoords(int unit) {
    loadTexcoords(unit, firstVertex, lastVertex);
  }  
  
  
  protected void loadTexcoords(int unit, int first, int last) {
    if (last < first || first < firstVertex || lastVertex < last) {
      PGraphics.showWarning("PShape3D: wrong vertex index");
      updateElement = -1;
      return;  
    }
    
    if (updateElement != -1) {
      PGraphics.showWarning("PShape3D: can load only one type of data at the time");
      return;        
    }    
    
    if (PGraphicsOpenGL.maxTextureUnits <= unit) {
      PGraphics.showWarning("PShape3D: wrong texture unit");
      return;
    }
    
    updateElement = TEXCOORDS;
    firstUpdateIdx = first;
    lastUpdateIdx = last;
    updateTexunit = unit;
    
    if (numTexBuffers <= unit) {
      addTexBuffers(unit - numTexBuffers + 1);
    }
    
    getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, glTexCoordBufferID[unit]);
    texCoordBuffer = getGl().glMapBuffer(GL.GL_ARRAY_BUFFER, GL.GL_WRITE_ONLY).asFloatBuffer(); 
    
    texcoords = allTexcoords[unit];
  }  
  
  
  public void updateTexcoords() {
    if (updateElement == TEXCOORDS) {    
      int offset = firstUpdateIdx * 2;
      int size = (lastUpdateIdx - firstUpdateIdx + 1) * 2;
      
      convertTexcoords();
      
      texCoordBuffer.position(0);      
      texCoordBuffer.put(convTexcoords, offset, size);
      texCoordBuffer.flip();
      
      getGl().glUnmapBuffer(GL.GL_ARRAY_BUFFER);
      getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
      
      texCoordSet[updateTexunit] = true;
      
      updateElement = -1;      
    } else {
      PGraphics.showWarning("PShape3D: need to call loadTexcoords() first");
    }       
  }  
  
  
  public float[] get(int idx) {
    float[] v = null;
    if (updateElement == VERTICES) {
      v = new float[3];
      PApplet.arrayCopy(vertices, 3 * idx, v, 0, 3);
    } else if (updateElement == COLORS) {
      v = new float[4];
      PApplet.arrayCopy(colors, 4 * idx, v, 0, 4);    
    } else if (updateElement == NORMALS) {
      v = new float[3];
      PApplet.arrayCopy(normals, 3 * idx, v, 0, 3);    
    } else if (updateElement == TEXCOORDS) {
      v = new float[2];
      PApplet.arrayCopy(texcoords, 2 * idx, v, 0, 2);        
    }
    return v;
  }
  
  
  public void set(int idx, float[] v) {
    if (updateElement == VERTICES) {
      PApplet.arrayCopy(v, 0, vertices, 3 * idx, 3);
    } else if (updateElement == COLORS) {
      PApplet.arrayCopy(v, 0, colors, 4 * idx, 4);    
    } else if (updateElement == NORMALS) {
      PApplet.arrayCopy(v, 0, normals, 3 * idx, 3);    
    } else if (updateElement == TEXCOORDS) {
      PApplet.arrayCopy(v, 0, texcoords, 2 * idx, 2);        
    }    
  }

  
  public void set(int idx, int c) {
    set(idx, rgba(c)); 
  }
  
  
  public void set(int idx, float x, float y) {
    if (updateElement == VERTICES) {
      set(idx, new float[] {x, y, 0});
    } else if (updateElement == TEXCOORDS) {
      set(idx, new float[] {x, y});    
    }
  }
  
  
  public void set(int idx, float x, float y, float z) {
    if (updateElement == VERTICES) {
      set(idx, new float[] {x, y, z});
    } else if (updateElement == NORMALS) {
      set(idx, new float[] {x, y, z});    
    } else if (updateElement == COLORS) {
      set(idx, new float[] {x, y, z, 1});
    }
  }

  
  public void set(int idx, float x, float y, float z, float w) {
    if (updateElement == COLORS) {
      set(idx, new float[] {x, y, z, w});
    }
  }

  
  static public int color(float[] rgba) {
    int r = (int)(rgba[0] * 255);
    int g = (int)(rgba[1] * 255);
    int b = (int)(rgba[2] * 255);
    int a = (int)(rgba[3] * 255);
    
    return a << 24 | r << 16 | g << 8 | b;
  }
  
  
  static public float[] rgba(int c) {
    int a = (c >> 24) & 0xFF;
    int r = (c >> 16) & 0xFF;
    int g = (c >> 8) & 0xFF;
    int b = (c  >> 0) & 0xFF;

    float[] res = new float[4];
    res[0] = r / 255.0f;
    res[1] = g / 255.0f;
    res[2] = b / 255.0f;
    res[3] = a / 255.0f;
    
    return res;
  }
  
  public void autoBounds(boolean value) {
    root.autoBounds = value;
  }
  
  public void updateBounds() {
    updateBounds(firstVertex, lastVertex);
  }
  
  protected void updateBounds(int first, int last) {
    if (first <= firstVertex && lastVertex <= last) {
      resetBounds();
    }
    
    if (family == GROUP) {      
      if (root == this && childCount == 0) {
        // This might happen: the vertices has been set
        // first but children still not initialized. So we
        // need to calculate the bounding box directly:
        for (int i = firstVertex; i <= lastVertex; i++) {
          updateBounds(vertices[3 * i + 0], vertices[3 * i + 1], vertices[3 * i + 2]);      
        }        
      } else {      
        for (int i = 0; i < childCount; i++) {
          PShape3D child = (PShape3D)children[i];
          child.updateBounds(first, last);
          xmin = PApplet.min(xmin, child.xmin);
          xmax = PApplet.max(xmax, child.xmax);
        
          ymin = PApplet.min(ymin, child.ymin);
          ymax = PApplet.max(ymax, child.ymax);

          zmin = PApplet.min(zmin, child.zmin);
          zmax = PApplet.max(zmax, child.zmax);
        
          width = xmax - xmin;
          height = ymax - ymin;
          depth = zmax - zmin;        
        }
      }
    } else {
      // TODO: extract minimum and maximum values using some sorting algorithm (maybe).
      int n0 = PApplet.max(first, firstVertex);
      int n1 = PApplet.min(last, lastVertex);
      
      for (int i = n0; i <= n1; i++) {
        updateBounds(vertices[3 * i + 0], vertices[3 * i + 1], vertices[3 * i + 2]);      
      }
    }   
  }

  protected void resetBounds() {
    if (family == GROUP) {            
      for (int i = 0; i < childCount; i++) {
        ((PShape3D)children[i]).resetBounds();
      }      
    }
    width = height = depth = 0;
    xmin = ymin = zmin = 10000;
    xmax = ymax = zmax = -10000;    
  }
  
  protected void updateBounds(float x, float y, float z) {
    xmin = PApplet.min(xmin, x);
    xmax = PApplet.max(xmax, x);
    
    ymin = PApplet.min(ymin, y);
    ymax = PApplet.max(ymax, y);

    zmin = PApplet.min(zmin, z);
    zmax = PApplet.max(zmax, z);

    width = xmax - xmin;
    height = ymax - ymin;
    depth = zmax - zmin;
  }  
  
  
  // Convert texture coordinates given by the user to values required by
  // the GPU (non-necessarily normalized because of texture size being smaller
  // than image size, for instance). 
  protected void convertTexcoords() {
    PTexture tex;
    float u, v;
    
    PTexture tex0 = null;
    float uscale = 1.0f;
    float vscale = 1.0f;
    float cx = 0.0f;
    float sx = +1.0f;
    float cy = 0.0f;
    float sy = +1.0f;
    for (int i = firstUpdateIdx; i <= lastUpdateIdx; i++) {
      if (vertexChild[i] != null && vertexChild[i].textures[updateTexunit] != null) {
        PImage img = vertexChild[i].textures[updateTexunit];
        tex = ogl.getTexture(img);
        
        if (tex != tex0) {
          uscale = 1.0f;
          vscale = 1.0f;
          cx = 0.0f;
          sx = +1.0f;
          cy = 0.0f;
          sy = +1.0f;
          
          if (tex != null) {
            if (tex.isFlippedX()) {
              cx = 1.0f;      
              sx = -1.0f;
            }
          
            if (tex.isFlippedY()) {
              cy = 1.0f;      
              sy = -1.0f;
            }
          
            uscale *= tex.getMaxTexCoordU();
            vscale *= tex.getMaxTexCoordV();
          }
          tex0 = tex;
        }

        u = texcoords[2 * i + 0];
        v = texcoords[2 * i + 1];
        
        u = (cx +  sx * u) * uscale;
        v = (cy +  sy * v) * vscale;
        
        convTexcoords[2 * i + 0] = u;
        convTexcoords[2 * i + 1] = v;        
      }  
    }    
  }
  
  ////////////////////////////////////////////////////////////  
  
  // Child shapes   
  
  
  static public PShape createChild(int n0, int n1) {
    return createChild(null, n0, n1, POINTS, 0, null);
  }   
  
  
  static public PShape createChild(String name, int n0, int n1) {
    return createChild(name, n0, n1, POINTS, 0, null);
  }  
  
  
  static public PShape createChild(String name, int n0, int n1, int mode) {
    return createChild(name, n0, n1, mode, 0, null);
  }
  
  
  static public PShape createChild(String name, int n0, int n1, int mode, float weight) {
    return createChild(name, n0, n1, mode, weight, null);
  }
  
  
  static public PShape createChild(String name, int n0, int n1, int i0, int i1, int mode, float weight, PImage[] tex) {
    PShape3D child = (PShape3D)createChild(name, n0, n1, mode, weight, tex);    
    child.firstIndex = i0;
    child.lastIndex = i1;
    return child;
  }
  
  static public PShape createChild(String name, int n0, int n1, int mode, float weight, PImage[] tex) {
    PShape3D child = new PShape3D();
    child.family = PShape.GEOMETRY;    
    child.name = name;
    child.firstVertex = n0;
    child.lastVertex = n1;
    child.setDrawModeImpl(mode);    
    child.strokeWeight = weight;
    child.textures = new PImage[PGraphicsOpenGL.MAX_TEXTURES];
    child.renderTextures = new PTexture[PGraphicsOpenGL.MAX_TEXTURES];
    
    java.util.Arrays.fill(child.textures, null);    
    if (tex != null) {
      int n = PApplet.min(tex.length, child.textures.length);
      PApplet.arrayCopy(tex, 0, child.textures, 0, n);
    }
    return child;
  }

  
  public void addChild(String name, int n0, int n1) {
    PShape child = createChild(name, n0, n1, getDrawModeImpl());
    addChild(child);
  }
  
  
  public void addChild(String name, int n0, int n1, int mode) {
    PShape child = createChild(name, n0, n1, mode);
    addChild(child);
  }
  
  
  public void addChild(String name, int n0, int n1, int mode, float weight) {
    PShape child = createChild(name, n0, n1, mode, weight);
    addChild(child);
  }
  
  
  public void addChild(String name, int n0, int n1, int mode, float weight, PImage[] tex) {
    PShape child = createChild(name, n0, n1, mode, weight, tex);
    addChild(child);
  }
  
  
  public void addChild(PShape who) {
    addChildImpl(who, true);
  }
  
  
  protected void addChildImpl(PShape who, boolean newShape) {
    if (family == GROUP) {
      super.addChild(who);
      
      if (newShape) {
        PShape3D who3d = (PShape3D)who;
        who3d.papplet = papplet;
        who3d.ogl = ogl;     
        who3d.root = root;        
        
        // So we can use the load/update methods in the child geometries.        
        who3d.numTexBuffers = root.numTexBuffers;
        
        who3d.glVertexBufferID = root.glVertexBufferID;
        who3d.glColorBufferID = root.glColorBufferID;
        who3d.glNormalBufferID = root.glNormalBufferID;
        who3d.glTexCoordBufferID = root.glTexCoordBufferID;
        who3d.glIndexBufferID = root.glIndexBufferID;
        
        who3d.vertexBuffer = root.vertexBuffer;
        who3d.colorBuffer = root.colorBuffer;
        who3d.normalBuffer = root.normalBuffer;
        who3d.texCoordBuffer = root.texCoordBuffer;
        
        who3d.vertices = root.vertices;
        who3d.colors = root.colors;
        who3d.normals = root.normals;
        who3d.texcoords = root.texcoords;
        
        who3d.convTexcoords = root.convTexcoords;
        who3d.allTexcoords = root.allTexcoords;        
        who3d.vertexChild = root.vertexChild;
        who3d.texCoordSet = root.texCoordSet;
        
        // In case the style was disabled from the root.
        who3d.style = root.style;

        // Updating vertex information.
        for (int n = who3d.firstVertex; n <= who3d.lastVertex; n++) {
          who3d.vertexChild[n] = who3d;
        }

        // If this new child shape has textures, then we should update the texture coordinates
        // for the vertices involved. All we need to do is to call loadTexcoords and updateTexcoords
        // so the current texture coordinates are converted into values that take into account
        // the texture flipping, max UV ranges, etc.
        for (int i = 0; i < who3d.textures.length; i++) {
          if (who3d.textures[i] != null && who3d.texCoordSet[i]) {
            who3d.loadTexcoords(i);  
            who3d.updateTexcoords();
          }
        }
      }      
    } else {
      PGraphics.showWarning("PShape3D: Child shapes can only be added to a group shape.");
    }  
  }

  /**
   * Add a shape to the name lookup table.
   */
  public void addName(String nom, PShape shape) {    
    if (nameTable == null) {
      nameTable = new HashMap<String,PShape>();
    }
    nameTable.put(nom, shape);
  }  

  protected void addDefaultChild() {
    PShape child = createChild("geometry", 0, vertexCount - 1, getDrawModeImpl(), 0, null);
    addChild(child);
  }
  

  public PShape groupChildren(int cidx0, int cidx1, String gname) {
    if (family != GROUP) return null; // Can be done only in a GROUP shape.
    return groupChildren(new int[] {cidx0, cidx1}, gname);
  }

  
  public PShape groupChildren(int cidx0, int cidx1, int cidx2, String gname) {
    if (family != GROUP) return null; // Can be done only in a GROUP shape.
    return groupChildren(new int[] {cidx0, cidx1, cidx2}, gname);
  }  
  
  
  public PShape groupChildren(int[] cidxs, String gname) {
    if (family != GROUP) return null; // Can be done only in a GROUP shape.
    PShape[] temp = new PShape[cidxs.length];
    
    int nsel = 0;    
    for (int i = 0; i < cidxs.length; i++) {
      PShape child = getChild(cidxs[i]);
      if (child != null) { 
        temp[nsel] = child;
        nsel++;
      }
    }
    
    PShape[] schildren = new PShape[nsel];
    PApplet.arrayCopy(temp, schildren, nsel);    
    
    return groupChildren(schildren, gname);
  }
  
  
  public PShape groupChildren(String cname0, String cname1, String gname) {
    if (family != GROUP) return null; // Can be done only in a GROUP shape.
    return groupChildren(new String[] {cname0, cname1}, gname);
  }
  
  
  public PShape groupChildren(String cname0, String cname1, String cname2, String gname) {   
    if (family != GROUP) return null; // Can be done only in a GROUP shape.
    return groupChildren(new String[] {cname0, cname1, cname2}, gname);
  }  
  
  
  public PShape groupChildren(String[] cnames, String gname) {
    if (family != GROUP) return null; // Can be done only in a GROUP shape.
    
    PShape[] temp = new PShape[cnames.length];
    
    int nsel = 0;    
    for (int i = 0; i < cnames.length; i++) {
      PShape child = getChild(cnames[i]);
      if (child != null) { 
        temp[nsel] = child;
        nsel++;
      }
    }
    
    PShape[] schildren = new PShape[nsel];
    PApplet.arrayCopy(temp, schildren, nsel);    
    
    return groupChildren(schildren, gname);
  }  
  
  
  public PShape groupChildren(PShape[] gchildren, String gname) {
    if (family != GROUP) return null; // Can be done only in a GROUP shape.
    
    // Creating the new group containing the children.
    PShape3D group = new PShape3D();
    group.family = PShape.GROUP;
    group.name = gname;
    group.papplet = papplet;
    group.ogl = ogl; 
    group.root = root;
    
    PShape child, p;
    int idx;
    
    // Inserting the new group at the position of the first
    // child shape in the group (in its original parent).
    child = gchildren[0];    
    p = child.getParent();
    if (p != null) {
      idx = p.getChildIndex(child);
      if (idx < 0) idx = 0; 
    } else {
      p = this;
      idx = 0;
    }
    p.addChild(group, idx);
    
    // Removing the children in the new group from their
    // respective parents.
    for (int i = 0; i < gchildren.length; i++) {
      child = gchildren[i];
      p = child.getParent();
      if (p != null) {
        idx = p.getChildIndex(child);
        if (-1 < idx) {
          p.removeChild(idx);
        }
      }
    }
    
    group.firstVertex = root.vertexCount;
    group.lastVertex = 0;
    for (int i = 0; i < gchildren.length; i++) {
      group.firstVertex = PApplet.min(group.firstVertex, ((PShape3D)gchildren[i]).firstVertex);
      group.lastVertex = PApplet.max(group.lastVertex, ((PShape3D)gchildren[i]).lastVertex);
    }
    
    // Adding the children shapes to the new group.
    for (int i = 0; i < gchildren.length; i++) {
      group.addChildImpl(gchildren[i], false);
    }    
    
    return group; 
  }
  
  
  public int getFirstVertex() {     
    return firstVertex;
  }

  
  public int getFirstVertex(int idx) {
    if (0 <= idx && idx < childCount) {
      return ((PShape3D)children[idx]).getFirstVertex();
    }      
    return -1;
  }  
  
  
  public void setFirstVertex(int n0) {
    firstVertex = n0;   
  }  
  
  
  public void setFirstVertex(int idx, int n0) {
    if (0 <= idx && idx < childCount) {
      ((PShape3D)children[idx]).setFirstVertex(n0);
    }     
  }
  
  
  public int getLastVertex() {
    return lastVertex;
  }
   
  
  public int getLastVertex(int idx) {
    if (0 <= idx && idx < childCount) {
      return ((PShape3D)children[idx]).getLastVertex();    
    }
    return -1;    
  }
  
  
  public void setLastVertex(int n1) {
    lastVertex = n1;
  }  
  
  
  public void setLastVertex(int idx, int n1) {
    if (0 <= idx && idx < childCount) {
      ((PShape3D)children[idx]).setLastVertex(n1);
    }     
  }
  
  
  public void setDrawMode(int mode) {
    if (family == GROUP) {
      init();
      setDrawModeImpl(mode);
      for (int n = 0; n < childCount; n++) {
        setDrawMode(n, mode);
      }
    } else { 
      setDrawModeImpl(mode);
    }     
  }
  
  
  public void setDrawMode(int idx, int mode) {
    if (0 <= idx && idx < childCount) {
      ((PShape3D)children[idx]).setDrawMode(mode);
    }
  }
  
  
  protected void setDrawModeImpl(int mode) {
    pointSprites = false;
    if (mode == POINTS) glMode = GL.GL_POINTS;
    else if (mode == POINT_SPRITES) {
      glMode = GL.GL_POINTS;
      pointSprites = true;
    }
    else if (mode == LINES) glMode = GL.GL_LINES;
    else if (mode == LINE_STRIP) glMode = GL.GL_LINE_STRIP;
    else if (mode == LINE_LOOP) glMode = GL.GL_LINE_LOOP;
    else if (mode == TRIANGLES) glMode = GL.GL_TRIANGLES; 
    else if (mode == TRIANGLE_FAN) glMode = GL.GL_TRIANGLE_FAN;
    else if (mode == TRIANGLE_STRIP) glMode = GL.GL_TRIANGLE_STRIP;
    else {
      throw new RuntimeException("PShape3D: Unknown draw mode");
    }    
  }
  
  
  protected boolean isTexturable() {    
    return glMode == GL.GL_TRIANGLES || 
           glMode == GL.GL_TRIANGLE_FAN ||
           glMode == GL.GL_TRIANGLE_STRIP ||
           pointSprites;       
  }
  
  
  public int getDrawMode() {
    if (family == GROUP) {
      init();
      return getDrawMode(0);
    } else { 
      return getDrawModeImpl();
    }     
  }
  
  
  public int getDrawMode(int idx) {
    if (0 <= idx && idx < childCount) {
      return ((PShape3D)children[idx]).getDrawMode();
    }
    return -1;
  }
  
  
  protected int getDrawModeImpl() {
    if (glMode == GL.GL_POINTS) {
      if (pointSprites) return POINT_SPRITES;
      else return POINTS;
    } else if (glMode == GL.GL_LINES) return LINES;
    else if (glMode == GL.GL_LINE_STRIP) return LINE_STRIP;
    else if (glMode == GL.GL_LINE_LOOP) return LINE_LOOP;
    else if (glMode == GL.GL_TRIANGLES) return TRIANGLES; 
    else if (glMode == GL.GL_TRIANGLE_FAN) return TRIANGLE_FAN;
    else if (glMode == GL.GL_TRIANGLE_STRIP) return TRIANGLE_STRIP;
    else return -1;
  }  
  
  /*
  public void setTexture(PImage tex) {
    if (family == GROUP) {
      init();
      for (int i = 0; i < childCount; i++) {
        setTexture(i, tex);
      }
    } else {
      setTextureImpl(tex, 0);
    }      
  }
  
  
  public void setTexture(PImage tex0, PImage tex1) {
    if (family == GROUP) {
      init();
      for (int i = 0; i < childCount; i++) {
        setTexture(i, tex0, tex1);
      }
    } else {
      setTextureImpl(tex0, 0);
      setTextureImpl(tex1, 1);
    }     
  }  
  
  
  public void setTexture(PImage tex0, PImage tex1, PImage tex2) {
    if (family == GROUP) {
      init();
      for (int i = 0; i < childCount; i++) {
        setTexture(i, tex0, tex1, tex2);
      }
    } else {
      setTextureImpl(tex0, 0);
      setTextureImpl(tex1, 1);
      setTextureImpl(tex2, 2);
    }    
  }  
  
  
  public void setTexture(PImage tex0, PImage tex1, PImage tex2, PImage tex3) {
    if (family == GROUP) {
      init();
      for (int i = 0; i < childCount; i++) {
        setTexture(i, tex0, tex1, tex2, tex3);
      }
    } else {
      setTextureImpl(tex0, 0);
      setTextureImpl(tex1, 1);
      setTextureImpl(tex2, 2);
      setTextureImpl(tex3, 3);      
    }
  }  
  
  
  public void setTexture(PImage[] tex) {
    if (family == GROUP) {
      init();
      for (int i = 0; i < childCount; i++) {
        setTexture(i, tex);
      }
    } else {
      for (int i = 0; i < tex.length; i++) {
        setTextureImpl(tex[i], i);
      }
    }
  }   
  
  
  public void setTexture(int idx, PImage tex) {
    if (0 <= idx && idx < childCount) {
      ((PShape3D)children[idx]).setTexture(tex);
    }    
  }
  
  
  public void setTexture(int idx, PImage tex0, PImage tex1) {
    if (0 <= idx && idx < childCount) {
      ((PShape3D)children[idx]).setTexture(tex0, tex1);
    }        
  }  
  
  
  public void setTexture(int idx, PImage tex0, PImage tex1, PImage tex2) {
    if (0 <= idx && idx < childCount) {
      ((PShape3D)children[idx]).setTexture(tex0, tex1, tex2);      
    }        
  }  
  
  
  public void setTexture(int idx, PImage tex0, PImage tex1, PImage tex2, PImage tex3) {
    if (0 <= idx && idx < childCount) {
      ((PShape3D)children[idx]).setTexture(tex0, tex1, tex2, tex3);      
    }            
  }  
  
  
  public void setTexture(int idx, PImage[] tex) {
    if (0 <= idx && idx < childCount) {
      ((PShape3D)children[idx]).setTexture(tex);    
    }
  }
 */
  
  protected void setTextureImpl(PImage tex, int unit) {
    if (unit < 0 || PGraphicsOpenGL.maxTextureUnits <= unit) {
      System.err.println("PShape3D: Wrong texture unit.");
      return;
    }
    
    if (numTexBuffers <= unit) {
      root.addTexBuffers(unit - numTexBuffers + 1);
    }
    
    if (tex == null) {
      throw new RuntimeException("PShape3D: trying to set null texture.");
    } 
        
    if  (texCoordSet[unit] && isTexturable()) {
      // Ok, setting a new texture, when texture coordinates have already been set. 
      // What is the problem? the new texture might have different max UV coords, 
      // flippedX/Y values, so the texture coordinates need to be updated accordingly...
      
      // The way to do it is just load the texcoords array (in the parent)...
      loadTexcoords(unit);
      // ... then replacing the old texture with the new and...
      textures[unit] = tex;
      // ...,finally, updating the texture coordinates, step in which the texcoords
      // array is converted, this time using the new texture.
      updateTexcoords();
    } else {
      textures[unit] = tex;  
    }    
  }
   
  /*
  public PImage[] getTexture() {
    if (family == GROUP) {
      init();
      return getTexture(0);
    } else {
      return textures;
    }          
  }
  
  
  public PImage[] getTexture(int idx) {
    if (0 <= idx && idx < childCount) {
      return ((PShape3D)children[idx]).getTexture();
    }
    return null;
  }
  */
  
  public float getStrokeWeight() {
    if (family == GROUP) {
      init();
      return getStrokeWeight(0);
    } else { 
      return strokeWeight;
    }
  }
  
  
  public float getStrokeWeight(int idx) {
    if (0 <= idx && idx < childCount) {
      return ((PShape3D)children[idx]).getStrokeWeight();
    }
    return 0;
  }
  
  /*
  public void setStrokeWeight(float sw) {
    if (family == GROUP) {
      init();
      for (int i = 0; i < childCount; i++) {
        setStrokeWeight(i, sw);
      }            
    } else { 
      strokeWeight = sw;
    }
  }
  
  
  public void setStrokeWeight(int idx, float sw) {
    if (0 <= idx && idx < childCount) {
      ((PShape3D)children[idx]).setStrokeWeight(sw);
    }
  }  
  */
  
  public float getMaxSpriteSize() {
    if (family == GROUP) {
      init();
      return getMaxSpriteSize(0);
    } else { 
      return maxSpriteSize;
    }
  }

  
  public float getMaxSpriteSize(int idx) {
    if (0 <= idx && idx < childCount) {
      return ((PShape3D)children[idx]).getMaxSpriteSize();
    }
    return 0;
  }

  
  public void setMaxSpriteSize(float s) {
    if (family == GROUP) {
      init();
      for (int i = 0; i < childCount; i++) {
        setMaxSpriteSize(i, s);
      }      
    } else {
      setMaxSpriteSizeImpl(s);
    }
  }
  

  public void setMaxSpriteSize(int idx, float s) {
    if (0 <= idx && idx < childCount) {
      ((PShape3D)children[idx]).setMaxSpriteSize(s);
    }
  }

  
  protected void setMaxSpriteSizeImpl(float s) {
    maxSpriteSize = PApplet.min(s, PGraphicsOpenGL.maxPointSize);    
  }

   
  public void setSpriteSize(float s) {
    if (family == GROUP) {
      init();
      for (int i = 0; i < childCount; i++) {
        setSpriteSize(i, s);
      }            
    } else {
      setSpriteSizeImpl(s);      
    }
  }
  
  
  public void setSpriteSize(float s, float d, int mode) {
    if (family == GROUP) {
      init();
      for (int i = 0; i < childCount; i++) {
        setSpriteSize(i, s, d, mode);
      }      
    } else {
      setSpriteSizeImpl(s, d, mode);      
    }
  }
  

  public void setSpriteSize(int idx, float s) {
    if (0 <= idx && idx < childCount) {
      ((PShape3D)children[idx]).setSpriteSize(s);
    }
  }  
  
  
  public void setSpriteSize(int idx, float s, float d, int mode) {
    if (0 <= idx && idx < childCount) {
      ((PShape3D)children[idx]).setSpriteSize(s, d, mode);
    }
  }  
  
  
  // Sets the coefficient of the distance attenuation function so that the 
  // size of the sprite is exactly s when its distance from the camera is d.
  protected void setSpriteSizeImpl(float s, float d, int mode) {
    float s0 = maxSpriteSize;
    if (mode == LINEAR) {
      spriteDistAtt[1] = (s0 - s) / (d * s);
      spriteDistAtt[2] = 0;
    } else if (mode == QUADRATIC) {
      spriteDistAtt[1] = 0; 
      spriteDistAtt[2] = (s0 - s) / (d * d * s);
    } else {
      PGraphics.showWarning("Invalid point sprite mode");
    }
  }
  

  // Sets constant sprite size equal to s.
  protected void setSpriteSizeImpl(float s) {
    setMaxSpriteSizeImpl(s);
    spriteDistAtt[1] = 0;
    spriteDistAtt[2] = 0;
  }
  
  /*
  public void setColor(int c) {
    setColor(rgba(c));
  }    
  
  
  public void setColor(float r, float g, float b, float a) {
    setColor(new float[] {r, g, b, a});
  }  
  
  
  public void setColor(float[] c) {
    if (family == GROUP) {
      init();
      for (int i = 0; i < childCount; i++) {
        setColor(i, c);
      }      
    } else {
      setColorImpl(c);
    }
  }

  
  public void setColor(int idx, int c) {
    setColor(idx, rgba(c));
  }    
  
  
  public void setColor(int idx, float r, float g, float b, float a) {
    setColor(idx, new float[] {r, g, b, a}); 
  }  
  
  
  public void setColor(int idx, float[] c) {
    if (0 <= idx && idx < childCount) {
      ((PShape3D)children[idx]).setColor(c);
    }
  } 
  
  
  protected void setColorImpl(float[] c) {
    PShape3D p = root;
    p.loadColors();
    for (int i = firstVertex; i <= lastVertex; i++) {
      p.set(i, c);
    }
    p.updateColors();
  }
  
  
  public void setNormal(float nx, float ny, float nz) {
    setNormal(new float[] {nx, ny, nz});  
  }  
  
  
  public void setNormal(float[] n) {
    if (family == GROUP) {
      init();      
      for (int i = 0; i < childCount; i++) {
        setNormal(i, n);
      }      
    } else {
      setNormalImpl(n);
    }
  }

  
  public void setNormal(int idx, float nx, float ny, float nz) {
    setNormal(idx, new float[] {nx, ny, nz});  
  } 
  

  public void setNormal(int idx, float[] n) {
    if (0 <= idx && idx < childCount) {
      ((PShape3D)children[idx]).setNormal(n);
    }    
  } 
    
  
  protected void setNormalImpl(float[] n) {
    PShape3D p = root;
    p.loadNormals();
    for (int i = firstVertex; i <= lastVertex; i++) {
      p.set(i, n);
    }
    p.updateNormals();
  }  
  */
  
  // Optimizes the array list containing children shapes so that shapes with identical
  // parameters are removed. Also, making sure that the names are unique.
  protected void optimizeChildren(ArrayList<PShape3D> childList) {    
    PShape3D child0, child1;

    // Expanding identical, contiguous shapes. Names are taken into account (two
    // shapes with different names are considered to be different, even though the
    // rest of their parameters are identical).
    child0 = childList.get(0);
    for (int i = 1; i < childList.size(); i++) {
      child1 = childList.get(i);
      if (child0.equalTo(child1, false)) {
        child0.lastVertex = child1.lastVertex;       // Extending child0.
        child0.lastIndex = child1.lastIndex;
        // Updating the vertex data:
        for (int n = child0.firstVertex; n <= child0.lastVertex; n++) {
          vertexChild[n] = child0;
        }    
        child1.firstVertex = child1.lastVertex = -1; // Marking for deletion.
      } else {
        child0 = child1;
      }
    }
      
    // Deleting superfluous shapes.
    for (int i = childList.size() - 1; i >= 0; i--) {
      if (childList.get(i).lastVertex == -1) {
        childList.remove(i);
      }
    }
    
    // Making sure the names are unique.
    for (int i = 1; i < childList.size(); i++) {
      child1 = childList.get(i);
      for (int j = i - 1; j >= 0; j--) {
        child0 = childList.get(j);
        if (child1.name.equals(child0.name)) {
          int pos = child0.name.indexOf(':');
          if (-1 < pos) {
            // The name of the preceding child contains the ':'
            // character so trying to use the following substring
            // as a number.
            String nstr = child0.name.substring(pos + 1);
            int n = 1;
            try {
              n = Integer.parseInt(nstr);              
            } catch (NumberFormatException e) {
              child0.name += ":1";   
            } 
            child1.name += ":" + (n + 1);
          } else {
            child0.name += ":1";
            child1.name += ":2";
          }
        }         
      }
    }
  }

 
  protected boolean equalTo(PShape3D child, boolean ignoreNames) {
    boolean res = family == child.family && 
            glMode == child.glMode && 
            strokeWeight == child.strokeWeight && 
            (ignoreNames || name.equals(child.name));
    if (!res) return false;
    for (int i = 0; i < textures.length; i++) {
      if (textures[i] != child.textures[i]) {
        res = false;
      }
    }
    return res; 
  }  
  
  ////////////////////////////////////////////////////////////

/*
  // Some overloading of translate, rotate, scale
  
  public void resetMatrix() {
    checkMatrix(3);
    matrix.reset();
  }
  
  public void translate(float tx, float ty) {
    checkMatrix(3);
    matrix.translate(tx, ty, 0);
  }
  
  public void rotate(float angle) {
    checkMatrix(3);
    matrix.rotate(angle);
  }  
  
  public void scale(float s) {
    checkMatrix(3);
    matrix.scale(s);
  }

  public void scale(float x, float y) {
    checkMatrix(3);
    matrix.scale(x, y);
  }  
  
  public void centerAt(float cx, float cy, float cz) {
    float dx = cx - 0.5f * (xmin + xmax);
    float dy = cy - 0.5f * (ymin + ymax);
    float dz = cz - 0.5f * (zmin + zmax);
    // Centering
    loadVertices();    
    for (int i = 0; i < vertexCount; i++) {
      vertices[3 * i + 0] += dx;
      vertices[3 * i + 1] += dy;
      vertices[3 * i + 2] += dz;  
    }    
    updateVertices();
  }
  */
  
  ////////////////////////////////////////////////////////////  
  
  /*
  // Bulk vertex operations.
  
  public void setVertices(ArrayList<PVector> vertexList) {
    setVertices(vertexList, 0); 
  }
  
  public void setVertices(ArrayList<PVector> vertexList, int offset) {
    loadVertices();
    for (int i = firstVertex; i <= lastVertex; i++) {
      PVector v = vertexList.get(i - firstVertex + offset);
      set(i, v.x, v.y, v.z);
    }
    updateVertices();
  }
  
  public void setColors(ArrayList<float[]> colorList) {
    setColors(colorList, 0);
  }
  
  public void setColors(ArrayList<float[]> colorList, int offset) {
    loadColors();
    for (int i = firstVertex; i <= lastVertex; i++) {
      float[] c = colorList.get(i - firstVertex + offset);
      set(i, c);
    }
    updateColors();    
  }  
  
  
  public void setNormals(ArrayList<PVector> normalList) {
    setNormals(normalList, 0); 
  }

  public void setNormals(ArrayList<PVector> normalList, int offset) {
    loadNormals();
    for (int i = firstVertex; i <= lastVertex; i++) {
      PVector n = normalList.get(i - firstVertex + offset);
      set(i, n.x, n.y, n.z);
    }
    updateNormals();    
  }  

  
  public void setTexcoords(ArrayList<PVector> tcoordList) {
    setTexcoords(tcoordList, 0);
  }
  
  public void setTexcoords(ArrayList<PVector> tcoordList, int offset) {
    setTexcoords(0, tcoordList, offset);
  }  
  
  public void setTexcoords(int unit, ArrayList<PVector> tcoordList) {
    setTexcoords(unit, tcoordList, 0);
  }
  
  public void setTexcoords(int unit, ArrayList<PVector> tcoordList, int offset) {
    loadTexcoords(unit);
    for (int i = firstVertex; i <= lastVertex; i++) {
      PVector tc = tcoordList.get(i - firstVertex + offset);
      set(i, tc.x, tc.y);
    }
    updateTexcoords();     
  }  
    
  
  public void setChildren(ArrayList<PShape3D> who) {
    if (family != GROUP) return; // Can be done only from a group shape.
    
    childCount = 0;
    for (int i = 0; i < who.size(); i++) {
      PShape child = who.get(i);
      addChild(child);   
    }
  }  
  
  public void mergeChildren() {
    if (family != GROUP) return; // Can be done only from a group shape.
    
    if (children == null) {
      if (root == this) {
        addDefaultChild();
      } else {
        return;
      }
    } else {
      PShape3D child0, child1;

      // Expanding identical, contiguous shapes (names are ignored).     
      int ndiff = 1;    
      // Looking for the first geometry child.
      int i0 = 0;
      child0 = null;
      for (int i = 0; i < childCount; i++) {
        child0 = (PShape3D)children[i];
        if (child0.family == GROUP) {
          child0.mergeChildren();
        } else {
          i0 = i + 1;
          break;
        }
      }
      if (i0 == 0) return; // No geometry child found.
      for (int i = i0; i < childCount; i++) {
        child1 = (PShape3D)children[i];
        if (child1.family == GROUP) {
          child1.mergeChildren();
          continue;
        }        
        if (child0.equalTo(child1, true)) {
          child0.lastVertex = child1.lastVertex;       // Extending child0.
          // Updating the vertex data:
          for (int n = child0.firstVertex; n <= child0.lastVertex; n++) {
            vertexChild[n] = child0;
          }    
          child1.firstVertex = child1.lastVertex = -1; // Marking for deletion.
        } else {
          child0 = child1;
          ndiff++;
        }
      }
      
      // Deleting superfluous shapes.
      PShape[] temp = new PShape[ndiff]; 
      int n = 0;
      for (int i = 0; i < childCount; i++) {
        child1 = (PShape3D)children[i];      
        if (child1.family == GEOMETRY && child1.lastVertex == -1 && 
            child1.getName() != null && nameTable != null) {
          nameTable.remove(child1.getName());
        } else {
          temp[n++] = child1;
        }
      }      
      children = temp;
      childCount = ndiff;
    }
  }  
  
  public void translateVertices(float tx, float ty) {
    translateVertices(tx, ty, 0);
  }


  public void translateVertices(float tx, float ty, float tz) {
    init();
    loadVertices();
    for (int i = firstVertex; i <= lastVertex; i++) {
      vertices[3 * i + 0] += tx;
      vertices[3 * i + 1] += -ty;
      vertices[3 * i + 2] += tz;  
    }
    updateVertices();
  }  
  
  
  public void rotateVerticesX(float angle) {
    rotateVertices(angle, 1, 0, 0);
  }


  public void rotateVerticesY(float angle) {
    rotateVertices(angle, 0, 1, 0);
  }


  public void rotateVerticesZ(float angle) {
    rotateVertices(angle, 0, 0, 1);
  }


  public void rotateVertices(float angle) {
    rotateVertices(angle, 0, 0, 1);
  }


  public void rotateVertices(float angle, float v0, float v1, float v2) {
    init();

    float norm2 = v0 * v0 + v1 * v1 + v2 * v2;
    if (Math.abs(norm2 - 1) > EPSILON) {
      // Normalizing rotation axis vector.
      float norm = PApplet.sqrt(norm2);
      v0 /= norm;
      v1 /= norm;
      v2 /= norm;
    }
    
    // Rotating around the center of the shape.
    float cx = 0.5f * (xmin + xmax);
    float cy = 0.5f * (ymin + ymax);
    float cz = 0.5f * (zmin + zmax);
    
    // Rotation matrix
    float c = PApplet.cos(angle);
    float s = PApplet.sin(angle);
    float t = 1.0f - c;
    float[] m = new float[9];
    m[0] = (t*v0*v0) + c;          // 0, 0
    m[1] = (t*v0*v1) - (s*v2);     // 0, 1
    m[2] = (t*v0*v2) + (s*v1);     // 0, 2 
    m[3] = (t*v0*v1) + (s*v2);     // 1, 0
    m[4] = (t*v1*v1) + c;          // 1, 1
    m[5] = (t*v1*v2) - (s*v0);     // 1, 2
    m[6] = (t*v0*v2) - (s*v1);     // 2, 0
    m[7] = (t*v1*v2) + (s*v0);     // 2, 1 
    m[8] = (t*v2*v2) + c;          // 2, 2

    float x, y, z;
    
    loadVertices();    
    for (int i = firstVertex; i <= lastVertex; i++) {
      x = vertices[3 * i + 0] - cx; 
      y = vertices[3 * i + 1] - cy;
      z = vertices[3 * i + 2] - cz;      
      
      vertices[3 * i + 0] = m[0] * x + m[1] * y + m[2] * z + cx; 
      vertices[3 * i + 1] = m[3] * x + m[4] * y + m[5] * z + cy;
      vertices[3 * i + 2] = m[6] * x + m[7] * y + m[8] * z + cz;
    }
    updateVertices();
    
    // Re-centering, because of loss of precision when applying the
    // rotation matrix, the center of rotation moves slightly so 
    // after many consecutive rotations the object might translate
    // significantly.
    centerAt(cx, cy, cz);
    
    // The normals also need to be rotated to reflect the new orientation 
    //of the faces.
    loadNormals();
    for (int i = firstVertex; i <= lastVertex; i++) {
      x = normals[3 * i + 0]; 
      y = normals[3 * i + 1];
      z = normals[3 * i + 2];      
      
      normals[3 * i + 0] = m[0] * x + m[1] * y + m[2] * z + cx; 
      normals[3 * i + 1] = m[3] * x + m[4] * y + m[5] * z + cy;
      normals[3 * i + 2] = m[6] * x + m[7] * y + m[8] * z + cz;
    }            
    updateNormals();    
  }

  
  public void scaleVertices(float s) {
    scaleVertices(s, s, s);
  }


  public void scaleVertices(float sx, float sy) {
    scaleVertices(sx, sy, 1);
  }


  public void scaleVertices(float x, float y, float z) {
    init();    
    loadVertices();
    for (int i = firstVertex; i <= lastVertex; i++) {
      vertices[3 * i + 0] *= x;
      vertices[3 * i + 1] *= y;
      vertices[3 * i + 2] *= z;  
    }        
    updateVertices();       
  }
  */
  
  
  
  ////////////////////////////////////////////////////////////  
  
  // Parameters  
  
  public Parameters getParameters() {
    if (root != this) return null; // Can be done only from the root shape.
    
    Parameters res = new Parameters();
    
    res.drawMode = getDrawModeImpl();
    
    if (glUsage == GL.GL_STATIC_DRAW) res.updateMode = STATIC;
    else if (glUsage == GL.GL_DYNAMIC_DRAW) res.updateMode = DYNAMIC;
    else if (glUsage == GL2.GL_STREAM_COPY) res.updateMode = STREAM;
    
    return res;
  }
  
  
  protected void setParameters(Parameters params) {
    //if (root != this) return; // Can be done only from the root shape.
    
    setDrawModeImpl(params.drawMode);

    if (params.updateMode == STATIC) glUsage = GL.GL_STATIC_DRAW;
    else if (params.updateMode == DYNAMIC) glUsage = GL.GL_DYNAMIC_DRAW;
    else if (params.updateMode == STREAM) glUsage = GL2.GL_STREAM_COPY;
    else {
      throw new RuntimeException("PShape3D: Unknown update mode");
    }
  }
  
  
  ////////////////////////////////////////////////////////////
  
  // INDEXED MODE: TESTING
  
  public void initIndices(int n) {
    indexCount = n;
    
    //glIndexBufferID = ogl.createGLResource(PGraphicsOpenGL.GL_VERTEX_BUFFER);    
    glIndexBufferID = ogl.createVertexBufferObject();
    getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, glIndexBufferID);    
    final int bufferSize = indexCount * PGraphicsOpenGL.SIZEOF_INT;
    getGl().glBufferData(GL.GL_ARRAY_BUFFER, bufferSize, null, GL.GL_STATIC_DRAW);    
    getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, 0); 
    
    indices = new int[indexCount];
    useIndices = true;
  }
  
  public void setIndices(ArrayList<Integer> recordedIndices) {
    getGl().glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, glIndexBufferID);
    indexBuffer = getGl().glMapBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, GL.GL_WRITE_ONLY).asIntBuffer();
    
    for (int i = 0; i < indexCount; i++) {
      indices[i] = recordedIndices.get(i);
    }    
    indexBuffer.put(indices);    
    
    getGl().glUnmapBuffer(GL.GL_ELEMENT_ARRAY_BUFFER);
    getGl().glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, 0);    
  }
  
  public void setIndices(int src[]) {
    getGl().glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, glIndexBufferID);
    indexBuffer = getGl().glMapBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, GL.GL_WRITE_ONLY).asIntBuffer();
    
    PApplet.arrayCopy(src, indices);
    indexBuffer.put(indices);    
    
    getGl().glUnmapBuffer(GL.GL_ELEMENT_ARRAY_BUFFER);
    getGl().glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, 0);    
  }    
    
  public void useIndices(boolean val) {
    if (family == GROUP) {
      init();
      for (int i = 0; i < childCount; i++) {
        useIndices(i, val);
      }            
    } else { 
      useIndices = val;
    }
  }
  
  public void useIndices(int idx, boolean val) {
    if (0 <= idx && idx < childCount) {
      ((PShape3D)children[idx]).useIndices = val;
      
      // Debugging. This mess needs to be fixed soon, which means 
      // using the indexed mode everywhere and sorting out the 
      // issues with children data.      
      ((PShape3D)children[idx]).firstIndex = 0;
      ((PShape3D)children[idx]).lastIndex = indexCount - 1;
    }
  }  
  
  ////////////////////////////////////////////////////////////  
  
  // Init methods.

  public void init() {
    if (root != this) return; // Can be done only from the root shape.
    
    if (readFromOBJ) {
      recordOBJ();
      //centerAt(0, 0, 0);      
    }    
    if (children == null) {
      addDefaultChild();
    }    
  }
  
  
  protected void initShape(int numVert) {
    initShape(numVert, new Parameters());
  }
  
  
  protected void initShape(int numVert, Parameters params) {
    // Checking we have what we need:
    if (ogl.gl2f == null) {
      throw new RuntimeException("PShape3D: OpenGL ES 1.1 required");
    }
    if (!PGraphicsOpenGL.vboSupported) {
       throw new RuntimeException("PShape3D: Vertex Buffer Objects are not available");
    }
    
    setParameters(params);
    setSize(numVert);
    allocate();
    initChildrenData();
    updateElement = -1;
    
    resetBounds();
  }
  
  
  protected void initShapeOBJ(String filename, Parameters params) {
    // Checking we have all we need:
    if (ogl.gl2f == null) {
      throw new RuntimeException("PShape3D: OpenGL ES 1.1 required");
    }
    if (!PGraphicsOpenGL.vboSupported) {
       throw new RuntimeException("PShape3D: Vertex Buffer Objects are not available");
    }

    readFromOBJ = true;
    objVertices = new ArrayList<PVector>(); 
    objNormal = new ArrayList<PVector>(); 
    objTexCoords = new ArrayList<PVector>();    
    objFaces = new ArrayList<OBJFace>();
    objMaterials = new ArrayList<OBJMaterial>();
    BufferedReader reader = getBufferedReader(filename);
    if (reader == null) {
      throw new RuntimeException("PShape3D: Cannot read source file");
    }
    
    // Setting parameters.
    if (params == null) {
      params = PShape3D.newParameters(TRIANGLES, STATIC);
    } else {
      params.drawMode = TRIANGLES;
    }
    setParameters(params);
    
    parseOBJ(reader, objVertices, objNormal, objTexCoords, objFaces, objMaterials);
    
    // Putting the number of vertices retrieved from the OBJ file in the vertex count
    // field, although the actual geometry hasn't been sent to the VBOs yet.
    vertexCount = objVertices.size();
  }
   
  ///////////////////////////////////////////////////////////  

  // Allocate/release shape. 
  
  protected void setSize(int numVert) {
    vertexCount = numVert;
    numTexBuffers = 1;
    
    firstVertex = 0;
    lastVertex = numVert - 1;

    initVertexData();
    initColorData();
    initNormalData();
    initTexCoordData();    
  }
  
  protected void allocate() {
    release(); // Just in the case this object is being re-allocated.
    
    createVertexBuffer();    
    createColorBuffer();    
    createNormalBuffer();    
    createTexCoordBuffer();
  }
  
  protected void release() {
    deleteVertexBuffer();
    deleteColorBuffer();
    deleteTexCoordBuffer();
    deleteNormalBuffer();    
    deleteIndexBuffer();    
  }
    
  
  ////////////////////////////////////////////////////////////  
  
  // Data creation, deletion.
  
  
  protected void initVertexData() {    
    vertices = new float[vertexCount * 3];
  }
  
  
  protected void createVertexBuffer() {    
    //glVertexBufferID = ogl.createGLResource(PGraphicsOpenGL.GL_VERTEX_BUFFER);    
    glVertexBufferID = ogl.createVertexBufferObject();
    getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, glVertexBufferID);    
    final int bufferSize = vertexCount * 3 * PGraphicsOpenGL.SIZEOF_FLOAT;
    getGl().glBufferData(GL.GL_ARRAY_BUFFER, bufferSize, null, glUsage);    
    getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
  }
  

  protected void initColorData() {
    colors = new float[vertexCount * 4];
    // Set the initial color of all vertices to white, so they are initially visible
    // even if the user doesn't set any vertex color.
    Arrays.fill(colors, 1.0f);
  }  
  
  
  protected void createColorBuffer() {
    //glColorBufferID = ogl.createGLResource(PGraphicsOpenGL.GL_VERTEX_BUFFER);
    glColorBufferID = ogl.createVertexBufferObject();
    getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, glColorBufferID);
    final int bufferSize = vertexCount * 4 * PGraphicsOpenGL.SIZEOF_FLOAT;    
    getGl().glBufferData(GL.GL_ARRAY_BUFFER, bufferSize, FloatBuffer.wrap(colors), glUsage);    
    getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
  }
  

  protected void initNormalData() {
    normals = new float[vertexCount * 3];
  }  

  
  protected void createNormalBuffer() {
    //glNormalBufferID = ogl.createGLResource(PGraphicsOpenGL.GL_VERTEX_BUFFER);
    glNormalBufferID = ogl.createVertexBufferObject();
    getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, glNormalBufferID);
    final int bufferSize = vertexCount * 3 * PGraphicsOpenGL.SIZEOF_FLOAT;    
    getGl().glBufferData(GL.GL_ARRAY_BUFFER, bufferSize, null, glUsage);
    getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
  }
  

  protected void initTexCoordData() {
    allTexcoords = new float[1][vertexCount * 2];
    texcoords = allTexcoords[0];
    convTexcoords = new float[vertexCount * 2];    
    texCoordSet = new boolean[PGraphicsOpenGL.MAX_TEXTURES];
  }  
  
  
  protected void createTexCoordBuffer() {
    if (glTexCoordBufferID == null) {
      glTexCoordBufferID = new int[PGraphicsOpenGL.MAX_TEXTURES];
      java.util.Arrays.fill(glTexCoordBufferID, 0);      
    }
    
    for (int i = 0; i < numTexBuffers; i++) {
      //glTexCoordBufferID[i] = ogl.createGLResource(PGraphicsOpenGL.GL_VERTEX_BUFFER);
      glTexCoordBufferID[i] = ogl.createVertexBufferObject();
      getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, glTexCoordBufferID[i]);
      final int bufferSize = vertexCount * 2 * PGraphicsOpenGL.SIZEOF_FLOAT;
      getGl().glBufferData(GL.GL_ARRAY_BUFFER, bufferSize, null, glUsage);
      getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
    }
  }
  
  
  protected void addTexBuffers(int more) {
    for (int i = 0; i < more; i++) {
      int t = numTexBuffers + i;
      deleteTexCoordBuffer(t);
      
      //glTexCoordBufferID[t] = ogl.createGLResource(PGraphicsOpenGL.GL_VERTEX_BUFFER);
      glTexCoordBufferID[t] = ogl.createVertexBufferObject();      
      getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, glTexCoordBufferID[t]);    
      final int bufferSize = vertexCount * 2 * PGraphicsOpenGL.SIZEOF_FLOAT;
      getGl().glBufferData(GL.GL_ARRAY_BUFFER, bufferSize, null, glUsage);
      getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, 0);       
    }
    
    // We need more arrays for texture coordinates, and to save the contents of the already
    // existing ones.
    float temp[][] = new float[numTexBuffers + more][vertexCount * 2];
    for (int i = 0; i < numTexBuffers; i++) {
      PApplet.arrayCopy(allTexcoords[i], temp[i]);
    }
    allTexcoords = temp;
    texcoords = allTexcoords[0];
    
    numTexBuffers += more;

    // Updating the allTexcoords and numTexBuffers in all
    // child geometries.
    updateTexBuffers();
  }
  
  
  protected void updateTexBuffers() {
    if (family == GROUP) {
      for (int i = 0; i < childCount; i++) {
        ((PShape3D)children[i]).updateTexBuffers();
      }
    } else {
      numTexBuffers = root.numTexBuffers;
      allTexcoords = root.allTexcoords;
      texcoords = allTexcoords[0];
    }
  }
  
  
  protected void deleteVertexBuffer() {
    if (glVertexBufferID != 0) {    
      ogl.finalizeVertexBufferObject(glVertexBufferID);   
      glVertexBufferID = 0;
    }
  }  
  

  protected void deleteColorBuffer() {
    if (glColorBufferID != 0) {
      ogl.finalizeVertexBufferObject(glColorBufferID);
      //ogl.deleteGLResource(glColorBufferID, PGraphicsOpenGL.GL_VERTEX_BUFFER);
      glColorBufferID = 0;
    }
  }
  
  
  protected void deleteNormalBuffer() {
    if (glNormalBufferID != 0) {
      //ogl.deleteGLResource(glNormalBufferID, PGraphicsOpenGL.GL_VERTEX_BUFFER);
      ogl.finalizeVertexBufferObject(glNormalBufferID);
      glNormalBufferID = 0;
    }
  }  

  
  protected void deleteIndexBuffer() {
    if (glIndexBufferID != 0) {
      ogl.finalizeVertexBufferObject(glIndexBufferID);
      //ogl.deleteGLResource(glIndexBufferID, PGraphicsOpenGL.GL_VERTEX_BUFFER);
      glIndexBufferID = 0;    
    }
  }
  
  
  protected void deleteTexCoordBuffer() {
    if (glTexCoordBufferID != null) {      
      for (int i = 0; i < glTexCoordBufferID.length; i++) { 
        deleteTexCoordBuffer(i);    
      }
    }
  }
  
  
  protected void deleteTexCoordBuffer(int idx) {  
    if (glTexCoordBufferID[idx] != 0) {
      ogl.finalizeVertexBufferObject(glTexCoordBufferID[idx]);
      //ogl.deleteGLResource(glTexCoordBufferID[idx], PGraphicsOpenGL.GL_VERTEX_BUFFER);
      glTexCoordBufferID[idx] = 0;
    }
  }

  
  protected void initChildrenData() {
    children = null;
    vertexChild = new PShape3D[vertexCount];    
  }

  
  ///////////////////////////////////////////////////////////  

  // These methods are not available in PShape3D.  
  
  /*
  public float[] getVertex(int index) {
    PGraphics.showMethodWarning("getVertex");
    return null;
  }
  
  
  public float getVertexX(int index) {
    PGraphics.showMethodWarning("getVertexX");
    return 0;    
  }

  
  public float getVertexY(int index) {
    PGraphics.showMethodWarning("getVertexY");
    return 0;    
  }

  
  public float getVertexZ(int index) {
    PGraphics.showMethodWarning("getVertexZ");
    return 0;      
  }


  public int[] getVertexCodes() {
    PGraphics.showMethodWarning("getVertexCodes");
    return null;
  }


  public int getVertexCodeCount() {
    PGraphics.showMethodWarning("getVertexCodeCount");
    return 0;         
  }
  

  public int getVertexCode(int index) {
    PGraphics.showMethodWarning("getVertexCode");
    return 0;    
  }
  */
  ///////////////////////////////////////////////////////////    
  
  // Style handling
  
  public void disableStyle() {
    style = false;
    if (family == GROUP) {
      init();
      for (int i = 0; i < childCount; i++) {
        children[i].disableStyle();
      }     
    }   
  }

  
  public void enableStyle() {
    style = true;
    if (family == GROUP) {
      init();
      for (int i = 0; i < childCount; i++) {
        children[i].enableStyle();
      }     
    }   
  }  
  
  
  protected void styles(PGraphics g) {
    // Nothing to do here. The styles are set in the drawGeometry() method.  
  }
  
  
  public boolean is3D() {
    return true;
  }  
  
  ///////////////////////////////////////////////////////////  
  
  //
  
  // Drawing methods
  
  
  public void draw() {
    draw(ogl);
  }
  
  
  public void draw(PGraphics g) {
    if (visible) {
      
      updateTesselation();
      
      if (matrix != null) {
        g.pushMatrix();
        g.applyMatrix(matrix);
      }
    
      if (family == GROUP) {
        
        boolean matrixBelow = false;
        for (int i = 0; i < childCount; i++) {
          if (((PShape3D)children[i]).hasMatrix()) {
            matrixBelow = true;
            break;
          }
        }

        HashSet<PImage> textures = getTextures();
        boolean diffTexBelow = 1 < textures.size();

        for (int i = 0; i < childCount; i++) {
          if (((PShape3D)children[i]).hasMatrix()) {
            matrixBelow = true;
            break;
          }
        }        
        
        if (matrixBelow || diffTexBelow) {
          // Some child shape below this group has a non-null matrix
          // transformation assigned to it, so the group cannot
          // be drawn in a single render call.
          // Or, some child shapes below this group use different
          // texture maps, so they cannot rendered in a single call
          // either.
          
          //init();
          for (int i = 0; i < childCount; i++) {
            ((PShape3D)children[i]).draw(g);
          }        
        } else {
          // None of the child shapes below this group has a matrix
          // transformation applied to them, so we can render everything
          // in a single block.
          // And all have the same texture applied to them.
          PImage tex = null;
          if (textures.size() == 1) {
            tex = (PImage)textures.toArray()[0];
          }
          render(g, tex);
        }
              
      } else {
        render(g, textures[0]);
      }
    
      if (matrix != null) {
        g.popMatrix();
      } 
    }
  }

  // Recursively checks if the there is a transformation
  // matrix associated to this shape or any of its child 
  // shapes.
  protected boolean hasMatrix() {
    if (matrix != null) {
      return true;
    }
    if (family == GROUP) {
      for (int i = 0; i < childCount; i++) {
        PShape3D child = (PShape3D)children[i];
        if (child.hasMatrix()) {
          return true;
        }
      }
    }
    return false;
  }
  
  protected HashSet<PImage> getTextures() {
    HashSet<PImage> texSet = new HashSet<PImage>();
    
    if (family == GROUP) {
      
      HashSet<PImage> childSet = null;
      for (int i = 0; i < childCount; i++) {
        PShape3D child = (PShape3D)children[i];
        childSet = child.getTextures();
        texSet.addAll(childSet);
      }
      
    } else {
      texSet.add(textures[0]);
    }
    
    return texSet;    
  }  
  
  protected boolean isModified() {
    if (modified) {
      return true;
    }
    if (family == GROUP) {
      for (int i = 0; i < childCount; i++) {
        PShape3D child = (PShape3D)children[i];
        if (child.isModified()) {
          return true;
        }
      }
    }
    return false;
  }
  
  protected void setModified(boolean value) {
    modified = value;
    if (family == GROUP) {
      for (int i = 0; i < childCount; i++) {
        PShape3D child = (PShape3D)children[i];
        child.setModified(value);
      }
    }    
  }
  
  /*
  protected void pre(PGraphics g) {
    if (matrix != null) {
      g.pushMatrix();
      g.applyMatrix(matrix);
    }
    // No need to push and set styles.
  }

  
  public void post(PGraphics g) {
    if (matrix != null) {
      g.popMatrix();
    }
  }
  */
  
  /*
  public void drawImpl(PGraphics g) {
    if (family == GROUP) {
      drawGroup(g);
    } else {
      drawGeometry(g);
    }
  }
  

  protected void drawGroup(PGraphics g) {
    init();
    for (int i = 0; i < childCount; i++) {
      children[i].draw(g);
    }
  }
*/  
  
  // Render the geometry stored in the root shape as VBOs, for the vertices 
  // corresponding to this shape. Sometimes we can have root == this.
  protected void render(PGraphics g, PImage texture) {
    if (root == null) {
      // Some error. Root should never be null. At least it should be this.
      return; 
    }
    
    if (0 < vertexCount && 0 < indexCount) { 
      renderFill(texture);
    }
    
    if (isStroked) {
      renderStroke();    
    }    
    
    if (hasPoints) {
      renderPoints();
    }    
  }
  
  protected void renderFill(PImage textureImage) {
    getGl().glEnableClientState(GL2.GL_NORMAL_ARRAY);
    getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, root.glNormalBufferID);
    getGl().glNormalPointer(GL.GL_FLOAT, 0, 0);    

    getGl().glEnableClientState(GL2.GL_COLOR_ARRAY);
    getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, root.glColorBufferID);
    getGl().glColorPointer(4, GL.GL_FLOAT, 0, 0);
    
    getGl().glEnableClientState(GL2.GL_VERTEX_ARRAY);            
    getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, root.glVertexBufferID);
    getGl().glVertexPointer(3, GL.GL_FLOAT, 0, 0);    

    getGl().glEnableClientState(GL2.GL_TEXTURE_COORD_ARRAY);
    getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, root.glTexCoordBufferID[0]);
    getGl().glTexCoordPointer(2, GL.GL_FLOAT, 0, 0);    
    
    PTexture tex = null;
    if (textureImage != null) {
      tex = ogl.getTexture(textureImage);
      if (tex != null) {
        getGl().glEnable(tex.glTarget);
        getGl().glActiveTexture(GL.GL_TEXTURE0);
        getGl().glBindTexture(tex.glTarget, tex.glID);
      }
    }
    
    getGl().glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, root.glIndexBufferID);    
    getGl().glDrawElements(GL.GL_TRIANGLES, lastIndex - firstIndex + 1, GL.GL_UNSIGNED_INT, firstIndex * PGraphicsOpenGL.SIZEOF_INT);      
    getGl().glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, 0);    
    
    if (tex != null) {
      getGl().glActiveTexture(GL.GL_TEXTURE0);
      getGl().glBindTexture(tex.glTarget, 0);
      getGl().glDisable(tex.glTarget);
    } 
    
    getGl().glDisableClientState(GL2.GL_VERTEX_ARRAY);
    getGl().glDisableClientState(GL2.GL_TEXTURE_COORD_ARRAY);
    getGl().glDisableClientState(GL2.GL_COLOR_ARRAY);
    getGl().glDisableClientState(GL2.GL_NORMAL_ARRAY);     
  }

  protected void renderStroke() {
    strokeShader.start();
    
    getGl().glEnableClientState(GL2.GL_NORMAL_ARRAY);
    getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, root.glStrokeNormalBufferID);
    getGl().glNormalPointer(GL.GL_FLOAT, 0, 0);
          
    getGl().glEnableClientState(GL2.GL_COLOR_ARRAY);
    getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, root.glStrokeColorBufferID);
    getGl().glColorPointer(4, GL.GL_FLOAT, 0, 0);
    
    getGl().glEnableClientState(GL2.GL_VERTEX_ARRAY);            
    getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, root.glStrokeVertexBufferID);
    getGl().glVertexPointer(3, GL.GL_FLOAT, 0, 0);
    
    int[] viewport = {0, 0, 0, 0};
    getGl().glGetIntegerv(GL.GL_VIEWPORT, viewport, 0);
    strokeShader.setVecUniform("viewport", viewport[0], viewport[1], viewport[2], viewport[3]);
            
    int attribsID = strokeShader.getAttribLocation("attribs");     
    ogl.gl2x.glEnableVertexAttribArray(attribsID);
    getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, root.glStrokeAttribBufferID);      
    ogl.gl2x.glVertexAttribPointer(attribsID, 4, GL.GL_FLOAT, false, 0, 0);      
    
    getGl().glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, root.glStrokeIndexBufferID);    
    getGl().glDrawElements(GL.GL_TRIANGLES, lastStrokeIndex - firstStrokeIndex + 1, GL.GL_UNSIGNED_INT, firstStrokeIndex * PGraphicsOpenGL.SIZEOF_INT);
    getGl().glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, 0);
    
    ogl.gl2x.glDisableVertexAttribArray(attribsID);
    
    getGl().glDisableClientState(GL2.GL_VERTEX_ARRAY);
    getGl().glDisableClientState(GL2.GL_COLOR_ARRAY);
    getGl().glDisableClientState(GL2.GL_NORMAL_ARRAY);
    
    strokeShader.stop();    
  }  
    
  protected void renderPoints() {
    pointShader.start();
    
    getGl().glEnableClientState(GL2.GL_NORMAL_ARRAY);
    getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, root.glPointNormalBufferID);
    getGl().glNormalPointer(GL.GL_FLOAT, 0, 0);
          
    getGl().glEnableClientState(GL2.GL_COLOR_ARRAY);
    getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, root.glPointColorBufferID);
    getGl().glColorPointer(4, GL.GL_FLOAT, 0, 0);
    
    getGl().glEnableClientState(GL2.GL_VERTEX_ARRAY);            
    getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, root.glPointVertexBufferID);
    getGl().glVertexPointer(3, GL.GL_FLOAT, 0, 0);
            
    int attribsID = pointShader.getAttribLocation("vertDisp");     
    ogl.gl2x.glEnableVertexAttribArray(attribsID);
    getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, root.glPointAttribBufferID);      
    ogl.gl2x.glVertexAttribPointer(attribsID, 2, GL.GL_FLOAT, false, 0, 0);      
    
    getGl().glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, root.glPointIndexBufferID);    
    getGl().glDrawElements(GL.GL_TRIANGLES, lastPointIndex - firstPointIndex + 1, GL.GL_UNSIGNED_INT, firstPointIndex * PGraphicsOpenGL.SIZEOF_INT);
    getGl().glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, 0);
    
    ogl.gl2x.glDisableVertexAttribArray(attribsID);
    
    getGl().glDisableClientState(GL2.GL_VERTEX_ARRAY);
    getGl().glDisableClientState(GL2.GL_COLOR_ARRAY);
    getGl().glDisableClientState(GL2.GL_NORMAL_ARRAY);
    
    pointShader.stop();    
  }  

  
  protected void drawGeometry(PGraphics g) {
    int numTextures;
    float pointSize;

    // Setting line width and point size from stroke value, using 
    // either the group's weight or the renderer's weight. 
    if (0 < strokeWeight && style) {
      getGl().glLineWidth(strokeWeight);
      pointSize = PApplet.min(strokeWeight, PGraphicsOpenGL.maxPointSize);
    } else {
      getGl().glLineWidth(g.strokeWeight);
      pointSize = PApplet.min(g.strokeWeight, PGraphicsOpenGL.maxPointSize);
    }
    if (!pointSprites) {
      // Point sprites use their own size variable (set below).
      getGl().glPointSize(pointSize); 
    }
    
    getGl().glEnableClientState(GL2.GL_NORMAL_ARRAY);
    getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, glNormalBufferID);
    getGl().glNormalPointer(GL.GL_FLOAT, 0, 0);    

    if (style) {
      getGl().glEnableClientState(GL2.GL_COLOR_ARRAY);
      getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, glColorBufferID);
      getGl().glColorPointer(4, GL.GL_FLOAT, 0, 0);
    }        

    getGl().glEnableClientState(GL2.GL_VERTEX_ARRAY);            
    getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, glVertexBufferID);
    getGl().glVertexPointer(3, GL.GL_FLOAT, 0, 0);

    numTextures = 0;
    if (style) {
      for (int t = 0; t < textures.length; t++) {
        if (textures[t] != null) {
          PTexture tex = (PTexture)textures[t].getCache(ogl);
          tex = ogl.getTexture(textures[t]);
          if (tex == null) {
            break;
          }

          getGl().glEnable(tex.glTarget);
          getGl().glActiveTexture(GL.GL_TEXTURE0 + t);
          getGl().glBindTexture(tex.glTarget, tex.glID);
          renderTextures[numTextures] = tex;
          numTextures++;
        } else {
          break;
        }
      }
    }

    if (0 < numTextures)  {        
      if (pointSprites) {
        // Texturing with point sprites.
        
        // The alpha of a point is calculated to allow the fading of points 
        // instead of shrinking them past a defined threshold size. The threshold 
        // is defined by GL_POINT_FADE_THRESHOLD_SIZE and is not clamped to the 
        // minimum and maximum point sizes.
        getGl().glPointParameterf(GL2.GL_POINT_FADE_THRESHOLD_SIZE, 0.6f * maxSpriteSize);
        getGl().glPointParameterf(GL2.GL_POINT_SIZE_MIN, 1.0f);
        getGl().glPointParameterf(GL2.GL_POINT_SIZE_MAX, maxSpriteSize);
        getGl().glPointSize(maxSpriteSize);
        
        // This is how will our point sprite's size will be modified by 
        // distance from the viewer:
        // actualSize = pointSize / sqrt(p[0] + p[1] * d + p[2] * d * d)
        // where pointSize is the value set with glPointSize(), clamped to the extreme values
        // in glPointParameterf(GL.GL_POINT_SIZE_MIN/GL.GL_POINT_SIZE_MAX. 
        // d is the distance from the point sprite to the camera and p is the array parameter 
        // passed in the following call: 
        getGl().glPointParameterfv(GL2.GL_POINT_DISTANCE_ATTENUATION, spriteDistAtt, 0);

        // Specify point sprite texture coordinate replacement mode for each 
        // texture unit
        getGl().glTexEnvf(GL2.GL_POINT_SPRITE, GL2.GL_COORD_REPLACE, GL.GL_TRUE);

        getGl().glEnable(GL2.GL_POINT_SPRITE);           
      } else {
        // Regular texturing.         
        for (int t = 0; t < numTextures; t++) {
          getGl().glClientActiveTexture(GL.GL_TEXTURE0 + t);
          getGl().glEnableClientState(GL2.GL_TEXTURE_COORD_ARRAY);
          getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, glTexCoordBufferID[t]);
          getGl().glTexCoordPointer(2, GL.GL_FLOAT, 0, 0);
        } 
        if (1 < numTextures) {
          ogl.setupTextureBlend(renderTextures, numTextures);
        }
      }
    }

    if (!style) {
      // Using fill or tint color when the style is disabled.
      if (0 < numTextures) {
        if (g.tint) {
          ogl.setTintColor();  
        } else {
          getGl().glColor4f(1, 1, 1, 1);  
        }
      } else {
        ogl.setFillColor();                  
      }              
    }
    
    if (glIndexBufferID != 0 && useIndices) {
      getGl().glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, glIndexBufferID);
      // Here the vertex indices are understood as the range of indices.
      int last = lastIndex;
      int first = firstIndex;
      getGl().glDrawElements(glMode, last - first + 1, GL.GL_UNSIGNED_INT, first * PGraphicsOpenGL.SIZEOF_INT);      
      getGl().glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, 0);
    } else {
      getGl().glDrawArrays(glMode, firstVertex, lastVertex - firstVertex + 1);  
    }

    if (0 < numTextures) {
      for (int t = 0; t < numTextures; t++) {
        PTexture tex = renderTextures[t];
        getGl().glActiveTexture(GL.GL_TEXTURE0 + t);
        getGl().glBindTexture(tex.glTarget, 0); 
      }      
      for (int t = 0; t < numTextures; t++) {
        PTexture tex = renderTextures[t];
        getGl().glDisable(tex.glTarget);
      }            
      if (pointSprites)   {
        getGl().glDisable(GL2.GL_POINT_SPRITE);
      } else {
        for (int t = 0; t < numTextures; t++) {
          getGl().glClientActiveTexture(GL.GL_TEXTURE0 + t);        
          getGl().glDisableClientState(GL2.GL_TEXTURE_COORD_ARRAY);
        }
        if (1 < numTextures) {
          ogl.cleanupTextureBlend(numTextures);
        }         
      }
    }

    getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
    getGl().glDisableClientState(GL2.GL_VERTEX_ARRAY);
    getGl().glDisableClientState(GL2.GL_COLOR_ARRAY);
    getGl().glDisableClientState(GL2.GL_NORMAL_ARRAY);
  }
  
  
  /////////////////////////////////////////////////////////////////////////// 

  // Utilities 
  
  
  protected GL2ES1 getGl() {
    return ogl.gl2f;
  }
  
  
  ///////////////////////////////////////////////////////////////////////////   
  
  // Parameters
  
  static public Parameters newParameters() {
    return new Parameters();
  }

  
  static public Parameters newParameters(int drawMode) {
    return new Parameters(drawMode);
  }  

  
  static public Parameters newParameters(int drawMode, int updateMode) {
    return new Parameters(drawMode, updateMode);
  }  
  
  
  static public class Parameters {
    public int drawMode;    
    public int updateMode;  
    
    public Parameters() {
      drawMode= POINTS;
      updateMode = STATIC;    
    }

    public Parameters(int drawMode) {
      this.drawMode= drawMode;      
      updateMode = STATIC;    
    }

    public Parameters(int drawMode, int updateMode) {
      this.drawMode= drawMode;
      this.updateMode = updateMode;      
    }
    
    public Parameters(Parameters src) {
      drawMode= src.drawMode;      
      updateMode = src.updateMode;
    }
  }  
  
	
  ///////////////////////////////////////////////////////////////////////////   
  
  // OBJ loading
	
	
  protected BufferedReader getBufferedReader(String filename) {
    BufferedReader retval = papplet.createReader(filename);
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
            currentMtl.kdMap = papplet.loadImage(texname);
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
    recordOBJ(objVertices, objNormal, objTexCoords, objFaces, objMaterials);
    objVertices = null; 
    objNormal = null; 
    objTexCoords = null;    
    objFaces = null;
    objMaterials = null;    
    
    readFromOBJ = false;
  }
  
  protected void recordOBJ(ArrayList<PVector> vertices, ArrayList<PVector> normals, ArrayList<PVector> textures, ArrayList<OBJFace> faces, ArrayList<OBJMaterial> materials) {
    int mtlIdxCur = -1;
    OBJMaterial mtl = null;
    
    ogl.saveDrawingState();
    
    // The recorded shapes are not merged, they are grouped
    // according to the group names found in the OBJ file.    
    ogl.mergeRecShapes = false;
    
    // Using RGB mode for coloring.
    ogl.colorMode = RGB;
    
    // Strokes are not used to draw the model.
    ogl.stroke = false;    
    
    // Normals are automatically computed if not specified in the OBJ file.
    ogl.autoNormal(true);
    
    // Using normal mode for texture coordinates (i.e.: normalized between 0 and 1).
    ogl.textureMode = NORMAL;    
    
    ogl.beginShapeRecorderImpl();    
    for (int i = 0; i < faces.size(); i++) {
      OBJFace face = faces.get(i);
      
      // Getting current material.
      if (mtlIdxCur != face.matIdx) {
        mtlIdxCur = PApplet.max(0, face.matIdx); // To make sure that at least we get the default material.
        
        mtl = materials.get(mtlIdxCur);

        // Setting colors.
        ogl.specular(mtl.ks.x * 255.0f, mtl.ks.y * 255.0f, mtl.ks.z * 255.0f);
        ogl.ambient(mtl.ka.x * 255.0f, mtl.ka.y * 255.0f, mtl.ka.z * 255.0f);
        if (ogl.fill) {
          ogl.fill(mtl.kd.x * 255.0f, mtl.kd.y * 255.0f, mtl.kd.z * 255.0f, mtl.d * 255.0f);  
        }        
        ogl.shininess(mtl.ns);
        
        if (ogl.tint && mtl.kdMap != null) {
          // If current material is textured, then tinting the texture using the diffuse color.
          ogl.tint(mtl.kd.x * 255.0f, mtl.kd.y * 255.0f, mtl.kd.z * 255.0f, mtl.d * 255.0f);
        }
      }

      // Recording current face.
      if (face.vertIdx.size() == 3) {
        ogl.beginShape(TRIANGLES); // Face is a triangle, so using appropriate shape kind.
      } else if (face.vertIdx.size() == 4) {
        ogl.beginShape(QUADS);        // Face is a quad, so using appropriate shape kind.
      } else {
        ogl.beginShape();  
      }      
      
      ogl.shapeName(face.name);
      
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
          
          PTexture texMtl = (PTexture)mtl.kdMap.getCache(ogl);
          if (texMtl != null) {     
            // Texture orientation in Processing is inverted.
            texMtl.setFlippedY(true);          
          }
          ogl.texture(mtl.kdMap);
          if (norms != null) {
            ogl.normal(norms.x, norms.y, norms.z);
          }
          if (tex != null) {
            ogl.vertex(vert.x, vert.y, vert.z, tex.x, tex.y);  
          } else {
            ogl.vertex(vert.x, vert.y, vert.z);
          }
        } else {
          // This face is not textured.
          if (norms != null) {
            ogl.normal(norms.x, norms.y, norms.z);
          }
          ogl.vertex(vert.x, vert.y, vert.z);          
        }
      } 
      ogl.endShape(CLOSE);
    }
    
    // Allocate space for the geometry that the triangulator has generated from the OBJ model.
    setSize(ogl.recordedVertices.size());
    allocate();
    initChildrenData();
    updateElement = -1;
    
    width = height = depth = 0;
    xmin = ymin = zmin = 10000;
    xmax = ymax = zmax = -10000;
    
    ogl.endShapeRecorderImpl(this);
    
    ogl.restoreDrawingState();    
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
