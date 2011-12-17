/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2011 Andres Colubri
  Copyright (c) 2004-10 Ben Fry and Casey Reas

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

package processing.opengl;

import processing.core.*;

import java.awt.BasicStroke;
import java.awt.Shape;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.nio.*;
import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

import javax.media.nativewindow.GraphicsConfigurationFactory;
import javax.media.nativewindow.NativeWindow;
import javax.media.nativewindow.NativeWindowFactory;
import javax.media.nativewindow.awt.AWTGraphicsConfiguration;
import javax.media.nativewindow.awt.AWTGraphicsDevice;
import javax.media.nativewindow.awt.AWTGraphicsScreen;
import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GL2ES1;
import javax.media.opengl.GL2ES2;
import javax.media.opengl.GL2GL3;
import javax.media.opengl.GL3;
import javax.media.opengl.GL4;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLDrawable;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.GLException;
import javax.media.opengl.GLProfile;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.glu.GLU;
import javax.media.opengl.glu.GLUtessellator;
import javax.media.opengl.glu.GLUtessellatorCallbackAdapter;

import processing.core.PApplet;

/**
 * New OpenGL renderer for Processing, entirely based on OpenGL 2.x 
 * (fixed-function pipeline) and OpenGL 3.x (programmable pipeline).
 * By Andres Colubri
 * 
 */
public class PGraphicsOpenGL extends PGraphics {
  // JOGL2 objects:
  
  /** 
   * How the P3D renderer handles the different OpenGL profiles? Basically,
   * P3D has two pipeline modes: fixed or programmable. In the fixed mode,
   * only the gl and gl2f objects are available. The gl2f object contains the 
   * intersection between OpenGL 2.x desktop and OpenGL 1.1 embedded, and in this
   * way it ensures the functionality parity between the P3D render (PC/MAC)
   * and A3D (Android) in the fixed pipeline mode.
   * In the programmable mode, there further options: GL2, GL3 and GL4. 
   * GL2 corresponds to the basic programmable profile that results from the common 
   * functionality between OpenGL 3.0 desktop and OpenGL 2.0 embedded. As said just 
   * before, since P3D and A3D aim at feature parity as much as possible, this is
   * the only programmable-pipeline GL object that the P3D renderer uses.
   * The gl3 and gl4 objects will be available when the pipeline mode is PROG_GL3 or
   * PROG_GL4, respectively. Although P3D doens't make any use of these objects,
   * they are part of the API nonetheless for users (or libraries) requiring advanced 
   * functionality introduced with OpenGL 3 or OpenGL 4.
   * By default, P3D tries to auto-select the pipeline mode by with the following 
   * priority order: PROG_GL4, PROG_GL3, PROG_GL2, FIXED. In all the programmable modes, 
   * the gl2p object is always available. This auto-selection can be optionally
   * overridden when creating the renderer object, so that a specific mode is set. 
   * Note that the programmable mode uses the non-backward compatible GL objects
   * (GL3, GL4, and not GL3bc, GL4bc) so no fixed mode calls are possible under this mode. 
   */
  
  /** Pipeline mode: FIXED, PROG_GL2, PROG_GL3 or PROG_GL4 */
  public int pipeline;
  
  /** Basic GL functionality, common to all profiles */
  public GL gl;
  
  public GL2 gl2;
  
  /** Advanced GL functionality (usually, things available as extensions in JOGL1).
   * This profile is the intersection between GL 2.0 and GL 3.0 */
  public GL2GL3 gl2x;
  
  /** Fixed GL pipeline, with the functionality common to the GL2 desktop and GLES 1.1 profiles */
  public GL2ES1 gl2f;
  
  /** Basic programmable GL pipeline: intersection of desktop GL3, GL2 and embedded ES2 profile */
  public GL2ES2 gl2p;
  
  /** GL3 programmable pipeline, not backwards compatible with GL2 fixed */
  public GL3 gl3p;
  
  /** GL4 programmable pipeline, not backwards compatible with GL2 fixed */
  public GL4 gl4p;
  
  public GLU glu;
  
  /** Selected GL profile */
  protected GLProfile profile;
  
  /** The capabilities of the OpenGL rendering surface */
  protected GLCapabilities capabilities;  
  
  /** The rendering surface */
  protected GLDrawable drawable;   
  
  /** The rendering context (holds rendering state info) */
  protected GLContext context;
  
  /** The PApplet renderer. For the primary surface, pgl == this. */
  protected PGraphicsOpenGL ogl;

  // ........................................................  
  
  // OpenGL id's of all the VBOs used in immediate rendering  
  
  public int glFillVertexBufferID;
  public int glFillColorBufferID;
  public int glFillNormalBufferID;
  public int glFillTexCoordBufferID;  
  public int glFillIndexBufferID;
  protected boolean fillVBOsCreated = false;
  
  public int glLineVertexBufferID;
  public int glLineColorBufferID;
  public int glLineNormalBufferID;
  public int glLineAttribBufferID;
  public int glLineIndexBufferID;  
  protected boolean lineVBOsCreated = false;
  
  public int glPointVertexBufferID;
  public int glPointColorBufferID;
  public int glPointNormalBufferID;
  public int glPointAttribBufferID;
  public int glPointIndexBufferID;   
  protected boolean pointVBOsCreated = false;
  
  // ........................................................  
  
  // GL parameters
  
  static protected boolean glParamsRead = false;
  
  /** Extensions used by Processing */
  static public boolean npotTexSupported;
  static public boolean mipmapGeneration;
  static public boolean matrixGetSupported;
  static public boolean vboSupported;
  static public boolean fboSupported;
  static public boolean blendEqSupported;
  static public boolean texenvCrossbarSupported;
  static public boolean fboMultisampleSupported;
  
  /** Some hardware limits */  
  static public int maxTextureSize;
  static public float maxPointSize;
  static public float maxLineWidth;
  static public int maxTextureUnits;
  
  /** OpenGL information strings */
  static public String OPENGL_VENDOR;
  static public String OPENGL_RENDERER;
  static public String OPENGL_VERSION;  
  static public String OPENGL_EXTENSIONS;
  
  // ........................................................  
  
  static protected HashMap<Integer, Boolean> glVertexArrays = new HashMap<Integer, Boolean>();
  static protected HashMap<Integer, Boolean> glTextureObjects = new HashMap<Integer, Boolean>();
  static protected HashMap<Integer, Boolean> glVertexBuffers = new HashMap<Integer, Boolean>();
  static protected HashMap<Integer, Boolean> glFrameBuffers = new HashMap<Integer, Boolean>();
  static protected HashMap<Integer, Boolean> glRenderBuffers = new HashMap<Integer, Boolean>();    
  static protected HashMap<Integer, Boolean> glslPrograms = new HashMap<Integer, Boolean>();
  static protected HashMap<Integer, Boolean> glslVertexShaders = new HashMap<Integer, Boolean>();
  static protected HashMap<Integer, Boolean> glslFragmentShaders = new HashMap<Integer, Boolean>();
  
  // ........................................................  

  // Camera:
  
  /** Camera field of view. */
  public float cameraFOV;

  /** Default position of the camera. */
  public float cameraX, cameraY, cameraZ;
  /** Distance of the near and far planes. */
  public float cameraNear, cameraFar;
  /** Aspect ratio of camera's view. */
  public float cameraAspect;
  
  /** Distance between the camera eye and and aim point. */
  protected float cameraDepth; 
  
  /** Actual position of the camera. */
  protected float cameraEyeX, cameraEyeY, cameraEyeZ; 
  
  /** Flag to indicate that we are inside beginCamera/endCamera block. */
  protected boolean manipulatingCamera;
  
  // ........................................................

  // Projection, modelview matrices:
  
  // Array version for use with OpenGL
  protected float[] glmodelview;
  protected float[] pcamera;  
  protected float[] glprojection;  
  protected float[] glmodelviewInv;  
  protected float[] pcameraInv;
  protected float[] pprojection;  
  protected float[] gltemp;
  
  
  // PMatrix3D version for use in Processing.
  public PMatrix3D modelview;
  public PMatrix3D modelviewInv;
  public PMatrix3D projection;

  public PMatrix3D camera;
  public PMatrix3D cameraInv;
  
  /** 
   * Marks when changes to the size have occurred, so that the camera 
   * will be reset in beginDraw().
   */
  protected boolean sizeChanged;  
  
  protected boolean modelviewUpdated;
  protected boolean projectionUpdated;

  protected boolean matricesAllocated = false;
  
  /** Camera and model transformation (translate/rotate/scale) matrix stack **/
  protected ModelviewStack modelviewStack;  
  
  /** Projection (ortho/perspective) matrix stack **/
  protected ProjectionStack projectionStack;

  // ........................................................

  // Lights:  
  
  /**
   * Maximum lights by default is 8, the minimum defined by OpenGL.
   */
  public static final int MAX_LIGHTS = 8;

  public boolean lights;
  public int lightCount = 0;

  /** Light types */
  public int[] lightType;

  /** Light positions */
  public float[][] lightPosition;

  /** Light direction (normalized vector) */
  public float[][] lightNormal;

  /** Light falloff */
  public float[] lightFalloffConstant;
  public float[] lightFalloffLinear;
  public float[] lightFalloffQuadratic;

  /** Light spot angle */
  public float[] lightSpotAngle;

  /** Cosine of light spot angle */
  public float[] lightSpotAngleCos;

  /** Light spot concentration */
  public float[] lightSpotConcentration;

  /**
   * Diffuse colors for lights. For an ambient light, this will hold the ambient
   * color. Internally these are stored as numbers between 0 and 1.
   */
  public float[][] lightDiffuse;

  /**
   * Specular colors for lights. Internally these are stored as numbers between
   * 0 and 1.
   */
  public float[][] lightSpecular;

  /** Current specular color for lighting */
  public float[] currentLightSpecular;

  /** Current light falloff */
  public float currentLightFalloffConstant;
  public float currentLightFalloffLinear;
  public float currentLightFalloffQuadratic;

  /** 
   * Used to store empty values to be passed when a light has no
   * ambient, diffuse or specular component 
   */
  public float[] zeroLight = { 0.0f, 0.0f, 0.0f, 1.0f };
  /** Default ambient light for the entire scene **/
  public float[] baseLight = { 0.05f, 0.05f, 0.05f, 1.0f };
 
  protected boolean lightsAllocated = false;   
  
  // ........................................................
  
  // Blending:
  
  protected int blendMode;  
  
  // ........................................................

  // Text:
    
  /** Font texture of currently selected font. */
  PFontTexture textTex;

  // .......................................................
  
  // Utility textured quad:
  
  static protected FloatBuffer quadVertexBuffer = null;
  static protected FloatBuffer quadTexCoordBuffer = null;  
  
  // .......................................................
  
  // Framebuffer stack:
  
  static protected Stack<PFramebuffer> fbStack;
  static protected PFramebuffer screenFramebuffer;
  static protected PFramebuffer currentFramebuffer;  
  
  // .......................................................
  
  // Offscreen rendering:
  
  protected PFramebuffer offscreenFramebuffer;
  protected PFramebuffer offscreenFramebufferMultisample;
  protected boolean offscreenMultisample;
  
  /** These are public so they can be changed by advanced users. */
  public int offscreenDepthBits = 24;
  public int offscreenStencilBits = 8;
  
  // ........................................................
  
  // Drawing surface:

  /** A handy reference to the PTexture bound to the drawing surface (off or on-screen) */
  protected PTexture texture;
  
  /** The crop rectangle for texture. It should always be {0, 0, width, height}. */
  protected int[] texCrop;
  
  /** IntBuffer to go with the pixels[] array. */
  protected IntBuffer pixelBuffer;
  
  /** 1-pixel get/set buffer. */
  protected IntBuffer getsetBuffer;
  
  /** 1-pixel get/set texture. */
  protected PTexture getsetTexture;

  // ........................................................  
  
  // Utility variables:
  
  /** True if we are inside a beginDraw()/endDraw() block. */
  protected boolean drawing = false;  
  
  /** Used to make backups of current drawing state. */
  protected DrawingState drawState;
  
  /** Used to hold color values to be sent to OpenGL. */
  protected float[] colorFloats; 
  
  /** Used to detect the occurrence of a frame resize event. */
  protected boolean resized = false;
  
  /** Stores previous viewport dimensions. */
  protected int[] savedViewport = {0, 0, 0, 0};
  
  // ........................................................

  // Utility constants:  
  
  /**
   * Set to true if the host system is big endian (PowerPC, MIPS, SPARC), false
   * if little endian (x86 Intel for Mac or PC).
   */
  static public boolean BIG_ENDIAN = ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN;

  /** Size of an int (in bytes). */
  protected static final int SIZEOF_INT = Integer.SIZE / 8;
   
  /** Size of a float (in bytes). */
  protected static final int SIZEOF_FLOAT = Float.SIZE / 8;

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

  // used by both curve and bezier, so just init here
  protected PMatrix3D bezierBasisMatrix =
    new PMatrix3D(-1,  3, -3,  1,
                   3, -6,  3,  0,
                  -3,  3,  0,  0,
                   1,  0,  0,  0);   
  
  // ........................................................

  // The new stuff (shaders, tessellator, etc)    

  protected InGeometry in;
  protected TessGeometry tess;

  protected float[] currentVertex = { 0, 0, 0 };
  protected float[] currentColor = { 0, 0, 0, 0 };  
  protected float[] currentNormal = { 0, 0, 1 };
  protected float[] currentTexcoord = { 0, 0 };
  protected float[] currentStroke = { 0, 0, 0, 1, 1 };  

  protected boolean openContour = false;
  protected boolean breakShape = false;  
  
  public static int flushMode = FLUSH_WHEN_FULL;
//  public static int flushMode = FLUSH_AFTER_SHAPE;
 
  public static final int MIN_ARRAYCOPY_SIZE = 3;
  
  public static final int MAX_TESS_VERTICES = 1000000;
  public static final int MAX_TESS_INDICES  = 3000000; 
  
  public static final int DEFAULT_IN_VERTICES = 512;
  public static final int DEFAULT_IN_EDGES = 1024;
  public static final int DEFAULT_TESS_VERTICES = 64;
  public static final int DEFAULT_TESS_INDICES = 1024;
  
  protected Tessellator tessellator;
  
  static protected PShader lineShader;
  static protected PShader pointShader;
  static protected int lineAttribsID;
  static protected int pointAttribsID;
  
  protected boolean drawing2D;
  protected PImage textureImage0;
  
  protected boolean clip = false;
  
  protected boolean defaultEdges = false;
  
  protected int vboMode = GL.GL_STATIC_DRAW;
    
  static public float FLOAT_EPS = Float.MIN_VALUE;
  // Calculation of the Machine Epsilon for float precision. From:
  // http://en.wikipedia.org/wiki/Machine_epsilon#Approximation_using_Java
  static {
    float eps = 1.0f;

    do {
      eps /= 2.0f;
    } while ((float)(1.0 + (eps / 2.0)) != 1.0);
   
    FLOAT_EPS = eps;
  }  
  
  //////////////////////////////////////////////////////////////
  
  
  // INIT/ALLOCATE/FINISH
  
  public PGraphicsOpenGL() {
    glu = new GLU();
    
    tessellator = new Tessellator();
    
    in = newInGeometry();
    tess = newTessGeometry(IMMEDIATE);
    
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
  }  

  //public void setParent(PApplet parent)  // PGraphics


  public void setPrimary(boolean primary) {
    super.setPrimary(primary);
    format = ARGB;    
  } 


  //public void setPath(String path)  // PGraphics
    
  public void setSize(int iwidth, int iheight) {
    resized = (0 < width && width != iwidth) || (0 < height && height != iwidth);
    
    width = iwidth;
    height = iheight;
    width1 = width - 1;
    height1 = height - 1;
    
    allocate();
    reapplySettings();
    
    vertexCheck();

    // init perspective projection based on new dimensions
    cameraFOV = 60 * DEG_TO_RAD; // at least for now
    cameraX = width / 2.0f;
    cameraY = height / 2.0f;
    cameraZ = cameraY / ((float) Math.tan(cameraFOV / 2.0f));
    cameraNear = cameraZ / 10.0f;
    cameraFar = cameraZ * 10.0f;
    cameraAspect = (float) width / (float) height;
    cameraDepth = cameraZ; // eye is at (cameraX, cameraY, cameraZ), aiming at (cameraX, cameraY, 0)
    
    // set this flag so that beginDraw() will do an update to the camera.
    sizeChanged = true;    
  }


  /**
   * Called by resize(), this handles creating the actual GLCanvas the
   * first time around, or simply resizing it on subsequent calls.
   * There is no pixel array to allocate for an OpenGL canvas
   * because OpenGL's pixel buffer is all handled internally.
   */
  protected void allocate() {
    super.allocate();
    
    if (!matricesAllocated) {
      glprojection = new float[16];
      glmodelview = new float[16];
      glmodelviewInv = new float[16];
      pcamera = new float[16];
      pcameraInv = new float[16];
      pprojection = new float[16];      
      gltemp = new float[16];
      
      projection = new PMatrix3D();
      modelview = new PMatrix3D();
      modelviewInv = new PMatrix3D();
      camera = new PMatrix3D();
      cameraInv = new PMatrix3D();
      
      matricesAllocated = true;
    }

    if (!lightsAllocated) {
      lightType = new int[MAX_LIGHTS];
      lightPosition = new float[MAX_LIGHTS][4];
      lightNormal = new float[MAX_LIGHTS][4];
      lightDiffuse = new float[MAX_LIGHTS][4];
      lightSpecular = new float[MAX_LIGHTS][4];
      lightFalloffConstant = new float[MAX_LIGHTS];
      lightFalloffLinear = new float[MAX_LIGHTS];
      lightFalloffQuadratic = new float[MAX_LIGHTS];
      lightSpotAngle = new float[MAX_LIGHTS];
      lightSpotAngleCos = new float[MAX_LIGHTS];
      lightSpotConcentration = new float[MAX_LIGHTS];
      currentLightSpecular = new float[4];
      lightsAllocated = true;
    }
    
    if (primarySurface) {
      // Allocation of the main renderer, which mainly involves initializing OpenGL.
      if (context == null) {
        initPrimary();
        // If there are registered GL objects (i.e.: PTexture, PShape3D, etc), it means
        // that the context has been re-created, so we need to re-allocate them in
        // order to be able to keep using them. This step doesn't refresh their data, this 
        // is, they are empty after re-allocation.
        //getGLObjects();
        //allocateGLObjects();        
      } else {
        reapplySettings();
      }      
    } else {      
      // Allocation of an offscreen renderer.
      if (context == null) {
        initOffscreen();
      } else {
        // Updating OpenGL context associated to this offscreen
        // surface, to take into account a context recreation situation.
        updateOffscreenContext();
        reapplySettings();
      }
    }    
  }

  
  public void dispose() { // PGraphics    
    super.dispose();
    detainContext();
    deleteFinalizedGLResources();
    releaseContext();
    GLProfile.shutdown();
  }
  

  // Only for debuggin purposes.
  public void setFlushMode(int mode) {
    PGraphicsOpenGL.flushMode = mode;    
  }
  
  //////////////////////////////////////////////////////////////

  // RESOURCE HANDLING
  
  // Vertex Array Objects --------------------------------------
  
  protected int createVertexArrayObject() {
    deleteFinalizedVertexArrayObjects();
    
    int[] temp = new int[1];
    gl2x.glGenVertexArrays(1, temp, 0);
    int id = temp[0];
    
    if (glVertexArrays.containsKey(id)) {
      showWarning("Adding same VAO twice");
    } else {    
      glVertexArrays.put(id, false);
    }
    
    return id;
  }
  
  protected void deleteVertexArrayObject(int id) {
    if (glVertexArrays.containsKey(id)) {
      int[] temp = { id };
      gl2x.glDeleteVertexArrays(1, temp, 0);      
      glVertexArrays.remove(id); 
    }
  }
  
  protected void deleteAllVertexArrayObjects() {
    for (Integer id : glVertexArrays.keySet()) {
      int[] temp = { id.intValue() };
      gl2x.glDeleteVertexArrays(1, temp, 0);
    }
    glVertexArrays.clear();
  }  
  
  // This is synchronized because it is called from the GC thread.
  synchronized protected void finalizeVertexArrayObject(int id) {
    if (glVertexArrays.containsKey(id)) {
      glVertexArrays.put(id, true);
    } else {
      showWarning("Trying to finalize non-existing VAO");
    }
  }
  
  protected void deleteFinalizedVertexArrayObjects() {
    Set<Integer> finalized = new HashSet<Integer>();
    
    for (Integer id : glVertexArrays.keySet()) {
      if (glVertexArrays.get(id)) {
        finalized.add(id);
        int[] temp = { id.intValue() };
        gl2x.glDeleteVertexArrays(1, temp, 0);        
      }
    }
    
    for (Integer id : finalized) {
      glVertexArrays.remove(id);  
    }
  }

  
  // Texture Objects -------------------------------------------
  
  protected int createTextureObject() {
    deleteFinalizedTextureObjects();
    
    int[] temp = new int[1];
    gl.glGenTextures(1, temp, 0);
    int id = temp[0];
    PApplet.println("Created texture object: " + id);
    
    if (glTextureObjects.containsKey(id)) {
      showWarning("Adding same texture twice");
    } else {    
      glTextureObjects.put(id, false);
    }
    
    return id;
  }
  
  protected void deleteTextureObject(int id) {
    if (glTextureObjects.containsKey(id)) {
      int[] temp = { id };
      gl.glDeleteTextures(1, temp, 0);      
      glTextureObjects.remove(id); 
    }
  }  
  
  protected void deleteAllTextureObjects() {
    for (Integer id : glTextureObjects.keySet()) {
      int[] temp = { id.intValue() };
      gl.glDeleteTextures(1, temp, 0);
    }
    glTextureObjects.clear();
  }
  
  // This is synchronized because it is called from the GC thread.
  synchronized protected void finalizeTextureObject(int id) {
    if (glTextureObjects.containsKey(id)) {
      glTextureObjects.put(id, true);
    } else {
      showWarning("Trying to finalize non-existing texture");
    }
  }
  
  protected void deleteFinalizedTextureObjects() {
    Set<Integer> finalized = new HashSet<Integer>();
    
    for (Integer id : glTextureObjects.keySet()) {
      if (glTextureObjects.get(id)) {
        finalized.add(id);
        int[] temp = { id.intValue() };
        gl.glDeleteTextures(1, temp, 0);        
      }
    }
    
    for (Integer id : finalized) {
      glTextureObjects.remove(id);  
    }
  }
  
  // Vertex Buffer Objects ----------------------------------------------
    
  protected int createVertexBufferObject() {
    deleteFinalizedVertexBufferObjects();
    
    int[] temp = new int[1];
    gl.glGenBuffers(1, temp, 0);
    int id = temp[0];
    
    if (glVertexBuffers.containsKey(id)) {
      showWarning("Adding same VBO twice");
    } else {    
      glVertexBuffers.put(id, false);
    }
    
    return id;
  }
  
  protected void deleteVertexBufferObject(int id) {
    if (glVertexBuffers.containsKey(id)) {
      int[] temp = { id };
      gl.glDeleteBuffers(1, temp, 0);      
      glVertexBuffers.remove(id); 
    }
  }
  
  protected void deleteAllVertexBufferObjects() {
    for (Integer id : glVertexBuffers.keySet()) {
      int[] temp = { id.intValue() };
      gl.glDeleteBuffers(1, temp, 0);
    }
    glVertexBuffers.clear();
  }  
  
  // This is synchronized because it is called from the GC thread.
  synchronized protected void finalizeVertexBufferObject(int id) {
    if (glVertexBuffers.containsKey(id)) {
      glVertexBuffers.put(id, true);
    } else {
      showWarning("Trying to finalize non-existing VBO");
    }
  }
  
  protected void deleteFinalizedVertexBufferObjects() {
    Set<Integer> finalized = new HashSet<Integer>();
    
    for (Integer id : glVertexBuffers.keySet()) {
      if (glVertexBuffers.get(id)) {
        finalized.add(id);
        int[] temp = { id.intValue() };
        gl.glDeleteBuffers(1, temp, 0);        
      }
    }
    
    for (Integer id : finalized) {
      glVertexBuffers.remove(id);  
    }
  }
  
  // FrameBuffer Objects -----------------------------------------

  protected int createFrameBufferObject() {
    deleteFinalizedFrameBufferObjects();
    
    int[] temp = new int[1];
    gl.glGenFramebuffers(1, temp, 0);
    int id = temp[0];
    
    if (glFrameBuffers.containsKey(id)) {
      showWarning("Adding same FBO twice");
    } else {    
      glFrameBuffers.put(id, false);
    }
    
    return id;
  }
  
  protected void deleteFrameBufferObject(int id) {
    if (glFrameBuffers.containsKey(id)) {
      int[] temp = { id };
      gl.glDeleteFramebuffers(1, temp, 0);      
      glFrameBuffers.remove(id); 
    }
  }  
  
  protected void deleteAllFrameBufferObjects() {
    for (Integer id : glFrameBuffers.keySet()) {
      int[] temp = { id.intValue() };
      gl.glDeleteFramebuffers(1, temp, 0);
    }
    glFrameBuffers.clear();
  }   
  
  // This is synchronized because it is called from the GC thread.
  synchronized protected void finalizeFrameBufferObject(int id) {
    if (glFrameBuffers.containsKey(id)) {
      glFrameBuffers.put(id, true);
    } else {
      showWarning("Trying to finalize non-existing FBO");
    }
  }
  
  protected void deleteFinalizedFrameBufferObjects() {
    Set<Integer> finalized = new HashSet<Integer>();
    
    for (Integer id : glFrameBuffers.keySet()) {
      if (glFrameBuffers.get(id)) {
        finalized.add(id);
        int[] temp = { id.intValue() };
        gl.glDeleteFramebuffers(1, temp, 0);       
      }
    }
    
    for (Integer id : finalized) {
      glFrameBuffers.remove(id);  
    }
  }

  // RenderBuffer Objects -----------------------------------------------
  
  protected int createRenderBufferObject() {
    deleteFinalizedRenderBufferObjects();
    
    int[] temp = new int[1];
    gl.glGenRenderbuffers(1, temp, 0);
    int id = temp[0];
    
    if (glRenderBuffers.containsKey(id)) {
      showWarning("Adding same renderbuffer twice");
    } else {    
      glRenderBuffers.put(id, false);
    }
    
    return id;
  }
  
  protected void deleteRenderBufferObject(int id) {
    if (glRenderBuffers.containsKey(id)) {
      int[] temp = { id };
      gl.glDeleteRenderbuffers(1, temp, 0);      
      glRenderBuffers.remove(id); 
    }
  }   
  
  protected void deleteAllRenderBufferObjects() {
    for (Integer id : glRenderBuffers.keySet()) {
      int[] temp = { id.intValue() };
      gl.glDeleteRenderbuffers(1, temp, 0);
    }
    glRenderBuffers.clear();
  }     
  
  // This is synchronized because it is called from the GC thread.
  synchronized protected void finalizeRenderBufferObject(int id) {
    if (glRenderBuffers.containsKey(id)) {
      glRenderBuffers.put(id, true);
    } else {
      showWarning("Trying to finalize non-existing renderbuffer");
    }
  }
  
  protected void deleteFinalizedRenderBufferObjects() {
    Set<Integer> finalized = new HashSet<Integer>();
    
    for (Integer id : glRenderBuffers.keySet()) {
      if (glRenderBuffers.get(id)) {
        finalized.add(id);
        int[] temp = { id.intValue() };
        gl.glDeleteRenderbuffers(1, temp, 0);       
      }
    }
    
    for (Integer id : finalized) {
      glRenderBuffers.remove(id);  
    }
  }
  
  // GLSL Program Objects -----------------------------------------------
  
  protected int createGLSLProgramObject() {
    
    ogl.report("before delete");
    deleteFinalizedGLSLProgramObjects();
        
    int id = gl2x.glCreateProgram();    
    
    if (glslPrograms.containsKey(id)) {
      showWarning("Adding same glsl program twice");
    } else {    
      glslPrograms.put(id, false);
    }
    
    return id;
  }
  
  protected void deleteGLSLProgramObject(int id) {
    if (glslPrograms.containsKey(id)) {
      gl2x.glDeleteProgram(id);      
      glslPrograms.remove(id); 
    }
  }     
  
  protected void deleteAllGLSLProgramObjects() {
    for (Integer id : glslPrograms.keySet()) {
      gl2x.glDeleteProgram(id); 
    }
    glslPrograms.clear();
  }    
  
  // This is synchronized because it is called from the GC thread.
  synchronized protected void finalizeGLSLProgramObject(int id) {
    if (glslPrograms.containsKey(id)) {
      glslPrograms.put(id, true);
    } else {
      showWarning("Trying to finalize non-existing glsl program");
    }
  }
  
  protected void deleteFinalizedGLSLProgramObjects() {
    Set<Integer> finalized = new HashSet<Integer>();
    
    for (Integer id : glslPrograms.keySet()) {
      if (glslPrograms.get(id)) {
        finalized.add(id);
        gl2x.glDeleteProgram(id.intValue());       
      }
    }
    
    for (Integer id : finalized) {
      glslPrograms.remove(id);  
    }
  }

  // GLSL Vertex Shader Objects -----------------------------------------------
  
  protected int createGLSLVertShaderObject() {
    deleteFinalizedGLSLVertShaderObjects();
    
    int id = gl2x.glCreateShader(GL2.GL_VERTEX_SHADER);
    
    if (glslVertexShaders.containsKey(id)) {
      showWarning("Adding same glsl vertex shader twice");
    } else {    
      glslVertexShaders.put(id, false);
    }
    
    return id;
  }
  
  protected void deleteGLSLVertShaderObject(int id) {
    if (glslVertexShaders.containsKey(id)) {
      gl2x.glDeleteShader(id);      
      glslVertexShaders.remove(id); 
    }
  }    
  
  protected void deleteAllGLSLVertShaderObjects() {
    for (Integer id : glslVertexShaders.keySet()) {
      gl2x.glDeleteShader(id); 
    }
    glslVertexShaders.clear();
  }     
  
  // This is synchronized because it is called from the GC thread.
  synchronized protected void finalizeGLSLVertShaderObject(int id) {
    if (glslVertexShaders.containsKey(id)) {
      glslVertexShaders.put(id, true);
    } else {
      showWarning("Trying to finalize non-existing glsl vertex shader");
    }
  }
  
  protected void deleteFinalizedGLSLVertShaderObjects() {
    Set<Integer> finalized = new HashSet<Integer>();
    
    for (Integer id : glslVertexShaders.keySet()) {
      if (glslVertexShaders.get(id)) {
        finalized.add(id);
        gl2x.glDeleteShader(id.intValue());      
      }
    }
    
    for (Integer id : finalized) {
      glslVertexShaders.remove(id);  
    }
  }
  
  // GLSL Fragment Shader Objects -----------------------------------------------
    
  
  protected int createGLSLFragShaderObject() {
    deleteFinalizedGLSLFragShaderObjects();
    
    int id = gl2x.glCreateShader(GL2.GL_FRAGMENT_SHADER);
    
    if (glslFragmentShaders.containsKey(id)) {
      showWarning("Adding same glsl fragment shader twice");
    } else {    
      glslFragmentShaders.put(id, false);
    }
    
    return id;
  }
  
  protected void deleteGLSLFragShaderObject(int id) {
    if (glslFragmentShaders.containsKey(id)) {
      gl2x.glDeleteShader(id);      
      glslFragmentShaders.remove(id); 
    }
  }     
  
  protected void deleteAllGLSLFragShaderObjects() {
    for (Integer id : glslFragmentShaders.keySet()) {
      gl2x.glDeleteShader(id); 
    }
    glslFragmentShaders.clear();
  }    
  
  // This is synchronized because it is called from the GC thread.
  synchronized protected void finalizeGLSLFragShaderObject(int id) {
    if (glslFragmentShaders.containsKey(id)) {
      glslFragmentShaders.put(id, true);
    } else {
      showWarning("Trying to finalize non-existing glsl fragment shader");
    }
  }
  
  protected void deleteFinalizedGLSLFragShaderObjects() {
    Set<Integer> finalized = new HashSet<Integer>();
    
    for (Integer id : glslFragmentShaders.keySet()) {
      if (glslFragmentShaders.get(id)) {
        finalized.add(id);
        gl2x.glDeleteShader(id.intValue());      
      }
    }
    
    for (Integer id : finalized) {
      glslFragmentShaders.remove(id);  
    }
  }  
  
  
  protected void deleteFinalizedGLResources() {
    deleteFinalizedTextureObjects();
    deleteFinalizedVertexBufferObjects();
    deleteFinalizedFrameBufferObjects();
    deleteFinalizedRenderBufferObjects();
    deleteFinalizedGLSLProgramObjects();
    deleteFinalizedGLSLVertShaderObjects();
    deleteFinalizedGLSLFragShaderObjects();
  }
    
  protected void deleteAllGLResources() {
    deleteAllTextureObjects();
    deleteAllVertexBufferObjects();
    deleteAllFrameBufferObjects();
    deleteAllRenderBufferObjects();
    deleteAllGLSLProgramObjects();
    deleteAllGLSLVertShaderObjects();
    deleteAllGLSLFragShaderObjects();    
  }
  
  //////////////////////////////////////////////////////////////

  // FRAMEBUFFERS
  
  public void pushFramebuffer() {
    fbStack.push(currentFramebuffer);
  }

  
  public void setFramebuffer(PFramebuffer fbo) {
    currentFramebuffer = fbo;
    currentFramebuffer.bind();
  }

  
  public void popFramebuffer() {
    try {
      currentFramebuffer.finish();
      currentFramebuffer = fbStack.pop();
      currentFramebuffer.bind();
    } catch (EmptyStackException e) {
      PGraphics.showWarning(": Empty framebuffer stack");
    }
  }
  
  //////////////////////////////////////////////////////////////

  // FRAME RENDERING
  
  /**
   * Get the current context, for use by libraries that need to talk to it.
   */
  public GLContext getContext() {
    return context;
  }


  /**
   * Get the current capabilities.
   */
  public GLCapabilities getCapabilities() {
    return capabilities;
  }  

  
  /**
   * Get the current profile.
   */  
  public GLProfile getProfile() {
    return profile;
  }
  
  
  /**
   * Get the current drawable.
   */  
  public GLDrawable getDrawable() {
    return drawable;
  }
  
  
  /**
   * Make the OpenGL rendering context current for this thread.
   */
  protected void detainContext() {
    try {
      while (context.makeCurrent() == GLContext.CONTEXT_NOT_CURRENT) {
        Thread.sleep(10);
      }
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  protected void releaseResources() {
    // First, releasing the resources used by
    // renderer itself.
    if (texture != null) {
      texture.release();
      texture = null;
    }
    if (lineShader != null) {
      lineShader.release();
      lineShader = null;
    }
    if (pointShader != null) {
      pointShader.release();
      pointShader = null;
    }
    
    if (fillVBOsCreated) {
      releaseFillBuffers();
      fillVBOsCreated = false;
    }
    
    if (lineVBOsCreated) {
      releaseLineBuffers();
      lineVBOsCreated = false;
    }
    
    if (pointVBOsCreated) {
      releasePointBuffers();
      pointVBOsCreated = false;
    }
    
    // Now, releasing the remaining resources 
    // (from user's objects).
    deleteAllGLResources();    
  }
  

  /**
   * Release the context, otherwise the AWT lock on X11 will not be released
   */
  protected void releaseContext() {
    context.release();
  }

  
  /**
   * Destroys current OpenGL context and creates a new one, making sure that all
   * the current OpenGL objects remain valid afterward.
   */  
  public void restartContext() {
    releaseResources();    
    //deleteFinalizedGLResources();
    
    releaseContext();
    context.destroy();
    context = null;
    allocate();          
    detainContext();
      
    updateGLInterfaces();    
  }  

  protected void createFillBuffers() {
    glFillVertexBufferID = createVertexBufferObject();    
    gl2f.glBindBuffer(GL.GL_ARRAY_BUFFER, glFillVertexBufferID);    
    gl2f.glBufferData(GL.GL_ARRAY_BUFFER, 3 * MAX_TESS_VERTICES * PGraphicsOpenGL.SIZEOF_FLOAT, null, vboMode);    
            
    glFillColorBufferID = createVertexBufferObject();
    gl2f.glBindBuffer(GL.GL_ARRAY_BUFFER, glFillColorBufferID);
    gl2f.glBufferData(GL.GL_ARRAY_BUFFER, 4 * MAX_TESS_VERTICES * PGraphicsOpenGL.SIZEOF_FLOAT, null, vboMode);    
        
    glFillNormalBufferID = createVertexBufferObject();
    gl2f.glBindBuffer(GL.GL_ARRAY_BUFFER, glFillNormalBufferID);
    gl2f.glBufferData(GL.GL_ARRAY_BUFFER, 3 * MAX_TESS_VERTICES * PGraphicsOpenGL.SIZEOF_FLOAT, null, vboMode);
    
    glFillTexCoordBufferID = createVertexBufferObject();
    gl2f.glBindBuffer(GL.GL_ARRAY_BUFFER, glFillTexCoordBufferID);
    gl2f.glBufferData(GL.GL_ARRAY_BUFFER, 2 * MAX_TESS_VERTICES * PGraphicsOpenGL.SIZEOF_FLOAT, null, vboMode);
    
    glFillIndexBufferID = createVertexBufferObject();    
    gl2f.glBindBuffer(GL.GL_ARRAY_BUFFER, glFillIndexBufferID);    
    gl2f.glBufferData(GL.GL_ARRAY_BUFFER, MAX_TESS_INDICES * PGraphicsOpenGL.SIZEOF_INT, null, vboMode);
    
    gl2f.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);        
  }
  
  protected void releaseFillBuffers() {
    deleteVertexBufferObject(glFillVertexBufferID);
    glFillVertexBufferID = 0;
    
    deleteVertexBufferObject(glFillColorBufferID);
    glFillColorBufferID = 0;

    deleteVertexBufferObject(glFillNormalBufferID);
    glFillNormalBufferID = 0;    
    
    deleteVertexBufferObject(glFillTexCoordBufferID);
    glFillTexCoordBufferID = 0;
    
    deleteVertexBufferObject(glFillIndexBufferID);
    glFillIndexBufferID = 0;    
  }

  protected void createLineBuffers() {
    glLineVertexBufferID = createVertexBufferObject();    
    gl2f.glBindBuffer(GL.GL_ARRAY_BUFFER, glLineVertexBufferID);    
    gl2f.glBufferData(GL.GL_ARRAY_BUFFER, 3 * MAX_TESS_VERTICES * PGraphicsOpenGL.SIZEOF_FLOAT, null, vboMode);    
    
    glLineColorBufferID = createVertexBufferObject();
    gl2f.glBindBuffer(GL.GL_ARRAY_BUFFER, glLineColorBufferID);
    gl2f.glBufferData(GL.GL_ARRAY_BUFFER, 4 * MAX_TESS_VERTICES * PGraphicsOpenGL.SIZEOF_FLOAT, null, vboMode);

    glLineNormalBufferID = createVertexBufferObject();    
    gl2f.glBindBuffer(GL.GL_ARRAY_BUFFER, glLineNormalBufferID);    
    gl2f.glBufferData(GL.GL_ARRAY_BUFFER, 3 * MAX_TESS_VERTICES * PGraphicsOpenGL.SIZEOF_FLOAT, null, vboMode);
    
    glLineAttribBufferID = createVertexBufferObject();
    gl2f.glBindBuffer(GL.GL_ARRAY_BUFFER, glLineAttribBufferID);   
    gl2f.glBufferData(GL.GL_ARRAY_BUFFER, 4 * MAX_TESS_VERTICES * PGraphicsOpenGL.SIZEOF_FLOAT, null, vboMode);
    
    glLineIndexBufferID = createVertexBufferObject();    
    gl2f.glBindBuffer(GL.GL_ARRAY_BUFFER, glLineIndexBufferID);    
    gl2f.glBufferData(GL.GL_ARRAY_BUFFER, MAX_TESS_INDICES * PGraphicsOpenGL.SIZEOF_INT, null, vboMode);
    
    gl2f.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);      
  }  
  
  protected void releaseLineBuffers() {
    deleteVertexBufferObject(glLineVertexBufferID);
    glLineVertexBufferID = 0;
    
    deleteVertexBufferObject(glLineColorBufferID);
    glLineColorBufferID = 0;

    deleteVertexBufferObject(glLineNormalBufferID);
    glLineNormalBufferID = 0;    
    
    deleteVertexBufferObject(glLineAttribBufferID);
    glLineAttribBufferID = 0;
    
    deleteVertexBufferObject(glLineIndexBufferID);
    glLineIndexBufferID = 0;    
  }

  protected void createPointBuffers() {
    glPointVertexBufferID = createVertexBufferObject();    
    gl2f.glBindBuffer(GL.GL_ARRAY_BUFFER, glPointVertexBufferID);    
    gl2f.glBufferData(GL.GL_ARRAY_BUFFER, 3 * MAX_TESS_VERTICES * PGraphicsOpenGL.SIZEOF_FLOAT, null, vboMode);    

    glPointColorBufferID = createVertexBufferObject();
    gl2f.glBindBuffer(GL.GL_ARRAY_BUFFER, glPointColorBufferID);
    gl2f.glBufferData(GL.GL_ARRAY_BUFFER, 4 * MAX_TESS_VERTICES * PGraphicsOpenGL.SIZEOF_FLOAT, null, vboMode);    
    
    glPointNormalBufferID = createVertexBufferObject();    
    gl2f.glBindBuffer(GL.GL_ARRAY_BUFFER, glPointNormalBufferID);    
    gl2f.glBufferData(GL.GL_ARRAY_BUFFER, 3 * MAX_TESS_VERTICES * PGraphicsOpenGL.SIZEOF_FLOAT, null, vboMode);    

    glPointAttribBufferID = createVertexBufferObject();
    gl2f.glBindBuffer(GL.GL_ARRAY_BUFFER, glPointAttribBufferID);   
    gl2f.glBufferData(GL.GL_ARRAY_BUFFER, 2 * MAX_TESS_VERTICES * PGraphicsOpenGL.SIZEOF_FLOAT, null, vboMode);
    
    glPointIndexBufferID = createVertexBufferObject();    
    gl2f.glBindBuffer(GL.GL_ARRAY_BUFFER, glPointIndexBufferID);    
    gl2f.glBufferData(GL.GL_ARRAY_BUFFER, MAX_TESS_INDICES * PGraphicsOpenGL.SIZEOF_INT, null, vboMode);
    
    gl2f.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);    
  }  
  
  protected void releasePointBuffers() {
    deleteVertexBufferObject(glPointVertexBufferID);
    glPointVertexBufferID = 0;
      
    deleteVertexBufferObject(glPointColorBufferID);
    glPointColorBufferID = 0; 
      
    deleteVertexBufferObject(glPointNormalBufferID);
    glPointNormalBufferID = 0;
    
    deleteVertexBufferObject(glPointAttribBufferID);
    glPointAttribBufferID = 0; 
      
    deleteVertexBufferObject(glPointIndexBufferID);  
    glPointIndexBufferID = 0;
  }
  
  
  
  /**
   * OpenGL cannot draw until a proper native peer is available, so this
   * returns the value of PApplet.isDisplayable() (inherited from Component).
   */
  public boolean canDraw() {
    return parent.isDisplayable();
  }

  
  public void beginDraw() {
    if (drawing) {
      showWarning("P3D: Already called beginDraw().");
      return;
    }    
    
    if (primarySurface && drawable != null) {
      // Call setRealized() after addNotify() has been called
      drawable.setRealized(parent.isDisplayable());
      if (parent.isDisplayable()) {
        drawable.setRealized(true);
      } else {
        return;  // Should have called canDraw() anyway
      }
      detainContext();
    }
      
    updateGLInterfaces();
    
    if (!glParamsRead) {
      getGLParameters();  
    }
    
    if (!settingsInited) {
      defaultSettings();
    }    
    
    // We are ready to go!
    
    report("top beginDraw()");    
    
    if (!primarySurface) {
      ogl.saveGLState();
            
      // Disabling all lights, so the offscreen renderer can set completely
      // new light configuration (otherwise some light configuration from the 
      // primary renderer might stay).
      ogl.disableLights();
    }     
    
    in.reset();
    tess.reset();
    
    // Each frame starts with textures disabled. 
    super.noTexture();
        
    // Screen blend is needed for alpha (i.e. fonts) to work.
    // Using setDefaultBlend() instead of blendMode() because
    // the latter will set the blend mode only if it is different
    // from current.
    setDefaultBlend();
       
    // this is necessary for 3D drawing
    if (hints[DISABLE_DEPTH_TEST]) {
      gl.glDisable(GL.GL_DEPTH_TEST);           
    } else {
      gl.glEnable(GL.GL_DEPTH_TEST); 
    }
    // use <= since that's what processing.core does
    gl.glDepthFunc(GL.GL_LEQUAL);
    
    if (hints[DISABLE_DEPTH_MASK]) {
      gl.glDepthMask(false);             
    } else {
      gl.glDepthMask(true); 
    }

    if (hints[ENABLE_ACCURATE_2D]) {
      flushMode = FLUSH_CONTINUOUSLY;
    } else {
      flushMode = FLUSH_WHEN_FULL;
    }
    
    // setup opengl viewport.    
    gl.glGetIntegerv(GL.GL_VIEWPORT, savedViewport, 0);
    gl.glViewport(0, 0, width, height);  
    if (resized) {
      // To avoid having garbage in the screen after a resize,
      // in the case background is not called in draw().
      background(0);
      if (texture != null) {
        // The screen texture should be deleted because it 
        // corresponds to the old window size.
        this.removeCache(ogl);
        this.removeParams(ogl);
        texture = null;
        loadTexture();
      }      
      resized = false;            
    }
    
    if (sizeChanged) {    
      // set up the default camera and initializes modelview matrix.
      camera();

      // defaults to perspective, if the user has setup up their
      // own projection, they'll need to fix it after resize anyway.
      // this helps the people who haven't set up their own projection.
      perspective();
      // clear the flag
      sizeChanged = false;
    } else {
      // The pcamera and pprojection arrays, saved when calling camera() and frustrum()
      // are set as the current modelview and projection matrices. This is done to
      // remove any additional modelview transformation (and less likely, projection
      // transformations) applied by the user after setting the camera and/or projection      
      restoreCamera();
      restoreProjection();
    }
      
    noLights();
    lightFalloff(1, 0, 0);
    lightSpecular(0, 0, 0);

    // because y is flipped
    gl.glFrontFace(GL.GL_CW);
    
    setSurfaceParams();
    
    // The current normal vector is set to be parallel to the Z axis.
    normalX = normalY = 0; 
    normalZ = 0;
    
    if (primarySurface) {
      // This instance of PGraphicsOpenGL is the primary (onscreen) drawing surface.    
      // Nothing else needs setup here.      
    } else {
      pushFramebuffer();
      if (offscreenMultisample) {
        setFramebuffer(offscreenFramebufferMultisample);        
        gl2x.glDrawBuffer(GL.GL_COLOR_ATTACHMENT0);
      } else {
        setFramebuffer(offscreenFramebuffer);
      }
    }
    
    // Clear depth and stencil buffers.
    gl.glClearColor(0, 0, 0, 0);
    gl.glClear(GL.GL_DEPTH_BUFFER_BIT | GL.GL_STENCIL_BUFFER_BIT);    
    
    drawing = true;    
    drawing2D = true;   
    
    report("bot beginDraw()");
  }


  public void endDraw() {
    report("top endDraw()");
    
    if (flushMode == FLUSH_WHEN_FULL) {
      // Flushing any remaining geometry.
      flush();
      // TODO: Implement depth sorting (http://code.google.com/p/processing/issues/detail?id=51)      
      //if (hints[ENABLE_DEPTH_SORT]) {
      //  flush();
      //}          
    }
    
    if (!drawing) {
      showWarning("P3D: Cannot call endDraw() before beginDraw().");
      return;
    }
    
    // Restoring previous viewport.
    gl.glViewport(savedViewport[0], savedViewport[1], savedViewport[2], savedViewport[3]); 
    
    if (primarySurface) {
      // glFlush should be called only once, since it is an expensive
      // operation. Thus, only the main renderer (the primary surface)
      // should call it at the end of draw, and none of the offscreen 
      // renderers...
      gl.glFlush();
      
      if (drawable != null) {
        drawable.swapBuffers();
        releaseContext();
      }
    } else {
      if (offscreenMultisample) {
        offscreenFramebufferMultisample.copy(offscreenFramebuffer);       
      }
      popFramebuffer();
      
      ogl.restoreGLState();
    }    

    drawing = false;    
    
    report("bot endDraw()");    
  }

  
  public GL beginGL() {
    saveGLState();
    // OpenGL is using Processing defaults at this point,
    // such as the inverted Y axis, GL_COLOR_MATERIAL mode, etc.
    // The user is free to change anything she wants, endGL will
    // try to do its best to restore things to the Processing
    // settings valid at the time of calling beginGL().
    return gl; // which one to return?
  }

  
  public void endGL() {
    restoreGLState();
  }
  
  
  public void updateGLInterfaces() {
    gl = context.getGL();        
    
    if (pipeline == PROG_GL4) {
      gl4p = gl.getGL4();
      gl3p = gl4p;
      gl2p = gl4p;        
      gl2f = null;
    } else if (pipeline == PROG_GL3) {     
      gl4p = null;
      gl3p = gl.getGL3();
      gl2p = gl3p;
      gl2f = null;        
    } else if (pipeline == PROG_GL2) { 
      gl4p = null;
      gl3p = null;
      gl2p = gl.getGL2ES2();
      gl2f = null;        
    } else if (pipeline == FIXED) {
      gl4p = null;
      gl3p = null;
      gl2p = null;
      gl2f = gl.getGL2ES1();
      gl2 = gl.getGL2();
    }
    
    try {
      gl2x = gl.getGL2GL3();
    } catch (GLException e) {}
  }
  
  
  protected void saveGLState() {
    saveGLMatrices();
  }
  
  
  protected void restoreGLState() {    
    // Restoring viewport.
    gl.glViewport(0, 0, width, height);

    restoreGLMatrices();
    
    // Restoring hints.
    if (hints[DISABLE_DEPTH_TEST]) {
      gl.glDisable(GL.GL_DEPTH_TEST);
      gl.glClearColor(0, 0, 0, 0);
      gl.glClear(GL.GL_DEPTH_BUFFER_BIT);      
    } else {
      gl.glEnable(GL.GL_DEPTH_TEST);      
    }
    
    if (hints[DISABLE_DEPTH_MASK]) {
      gl.glDepthMask(false);             
    } else {
      gl.glDepthMask(true);  
    }
        
    // Restoring blending.
    blendMode(blendMode);
    
    // Restoring fill
    if (fill) {
      calcR = fillR;
      calcG = fillG;
      calcB = fillB;
      calcA = fillA;
      fillFromCalc();  
    }    

    // Restoring material properties.
    calcR = ambientR;
    calcG = ambientG;
    calcB = ambientB;    
    ambientFromCalc();

    calcR = specularR;
    calcG = specularG;
    calcB = specularB;
    specularFromCalc();
    
    shininess(shininess);
    
    calcR = emissiveR;
    calcG = emissiveG;
    calcB = emissiveB;    
    emissiveFromCalc();
    
    // Restoring lights.
    if (lights) {
      lights();
      for (int i = 0; i < lightCount; i++) {
        lightEnable(i);
        if (lightType[i] == AMBIENT) {
          lightEnable(i);
          lightAmbient(i);
          lightPosition(i);
          lightFalloff(i);
          lightNoSpot(i);
          lightNoDiffuse(i);
          lightNoSpecular(i);
        } else if (lightType[i] == DIRECTIONAL) {
          lightEnable(i);
          lightNoAmbient(i);
          lightDirection(i);
          lightDiffuse(i);
          lightSpecular(i);
          lightFalloff(i);
          lightNoSpot(i);
        } else if (lightType[i] == POINT) {
          lightEnable(i);
          lightNoAmbient(i);
          lightPosition(i);
          lightDiffuse(i);
          lightSpecular(i);
          lightFalloff(i);
          lightNoSpot(i);
        } else if (lightType[i] == SPOT) {
          lightEnable(i);
          lightNoAmbient(i);
          lightPosition(i);
          lightDirection(i);
          lightDiffuse(i);
          lightSpecular(i);
          lightFalloff(i);
          lightSpotAngle(i);
          lightSpotConcentration(i);
        }
      }
    } else {
      noLights();
    } 
    
    // Some things the user might have changed from OpenGL, 
    // but we want to make sure they return to the Processing
    // defaults (plus these cannot be changed through the API
    // so they should remain constant anyways):
    gl.glFrontFace(GL.GL_CW);    
    gl.glDepthFunc(GL.GL_LEQUAL);

    setSurfaceParams();
  }  
  
  
  protected void saveDrawingState() {
    if (drawState == null) {
      drawState = new DrawingState();
    }
    drawState.save();
  }

  
  protected void restoreDrawingState() {
    drawState.restore();
  }
  
  // Utility function to get ready OpenGL for a specific
  // operation, such as grabbing the contents of the color
  // buffer.
  protected void beginGLOp() {
    detainContext();
    updateGLInterfaces();
  }

  
  // Pairs-up with beginGLOp().
  protected void endGLOp() {
    releaseContext();
  }
  
  
  //////////////////////////////////////////////////////////////  
  
  // SETTINGS

  // protected void checkSettings()

  
  protected void defaultSettings() {
    super.defaultSettings();

    manipulatingCamera = false;
    //scalingDuringCamManip = false;
        
    if (fbStack == null) {
      fbStack = new Stack<PFramebuffer>();

      screenFramebuffer = new PFramebuffer(parent, width, height, true);
      setFramebuffer(screenFramebuffer);
    }    
    
    if (modelviewStack == null) {
      modelviewStack = new ModelviewStack();
    }
    if (projectionStack == null) {
      projectionStack = new ProjectionStack();
    }
    
    // easiest for beginners
    textureMode(IMAGE);
  }
  
  // reapplySettings

  //////////////////////////////////////////////////////////////

  // HINTS

  public void hint(int which) {
    super.hint(which);    

    if (which == DISABLE_DEPTH_TEST) {
      flush();
      gl.glDisable(GL.GL_DEPTH_TEST);
      gl.glClearColor(0, 0, 0, 0);
      gl.glClear(GL.GL_DEPTH_BUFFER_BIT);

    } else if (which == ENABLE_DEPTH_TEST) {
      flush();
      gl.glEnable(GL.GL_DEPTH_TEST);

    } else if (which == DISABLE_DEPTH_MASK) {
      flush();
      gl.glDepthMask(false);

    } else if (which == ENABLE_DEPTH_MASK) {
      flush();
      gl.glDepthMask(true);
      
    } else if (which == DISABLE_ACCURATE_2D) {
      flush();
      setFlushMode(FLUSH_WHEN_FULL);
      
    } else if (which == ENABLE_ACCURATE_2D) {
      flush();
      setFlushMode(FLUSH_CONTINUOUSLY);
      
    }

  }

  //////////////////////////////////////////////////////////////

  // SHAPE CREATORS
  
  
  public PShape createShape() {
    return createShape(POLYGON);
  }
  
  public PShape createShape(int type) {
    PShape3D shape = null;
    if (type == PShape.GROUP) {
      shape = new PShape3D(parent, PShape.GROUP);
    } else if (type == POINTS) {
      shape = new PShape3D(parent, PShape.GEOMETRY);
      shape.setKind(POINTS);
    } else if (type == LINES) {
      shape = new PShape3D(parent, PShape.GEOMETRY);
      shape.setKind(LINES);
    } else if (type == TRIANGLES) {
      shape = new PShape3D(parent, PShape.GEOMETRY);
      shape.setKind(TRIANGLES);
    } else if (type == TRIANGLE_FAN) {
      shape = new PShape3D(parent, PShape.GEOMETRY);
      shape.setKind(TRIANGLE_FAN);
    } else if (type == TRIANGLE_STRIP) {      
      shape = new PShape3D(parent, PShape.GEOMETRY);
      shape.setKind(TRIANGLE_STRIP);
    } else if (type == QUADS) {
      shape = new PShape3D(parent, PShape.GEOMETRY);
      shape.setKind(QUADS);
    } else if (type == QUAD_STRIP) {
      shape = new PShape3D(parent, PShape.GEOMETRY);
      shape.setKind(QUAD_STRIP);
    } else if (type == POLYGON) {
      shape = new PShape3D(parent, PShape.GEOMETRY);
      shape.setKind(POLYGON);      
    }
    return shape;
  }
  
  public PShape createShape(int kind, float... p) {
    PShape3D shape = null;
    int len = p.length;
    
    if (kind == POINT) {
      if (len != 2) {
        showWarning("Wrong number of parameters");
        return null;
      }
      shape = new PShape3D(parent, PShape.PRIMITIVE);
      shape.setKind(POINT);      
    } else if (kind == LINE) {
      if (len != 4) {
        showWarning("Wrong number of parameters");
        return null;
      }
      shape = new PShape3D(parent, PShape.PRIMITIVE);
      shape.setKind(LINE);
    } else if (kind == TRIANGLE) {
      if (len != 6) {
        showWarning("Wrong number of parameters");
        return null;
      }
      shape = new PShape3D(parent, PShape.PRIMITIVE);
      shape.setKind(TRIANGLE);
    } else if (kind == QUAD) {
      if (len != 8) {
        showWarning("Wrong number of parameters");
        return null;
      }
      shape = new PShape3D(parent, PShape.PRIMITIVE);
      shape.setKind(QUAD);
    } else if (kind == RECT) {
      if (len != 4 && len != 5 && len != 8) {
        showWarning("Wrong number of parameters");
        return null;
      }      
      shape = new PShape3D(parent, PShape.PRIMITIVE);
      shape.setKind(RECT);  
    } else if (kind == ELLIPSE) {
      if (len != 4) {
        showWarning("Wrong number of parameters");
        return null;
      }      
      shape = new PShape3D(parent, PShape.PRIMITIVE);
      shape.setKind(ELLIPSE);          
    } else if (kind == ARC) {
      if (len != 6) {
        showWarning("Wrong number of parameters");
        return null;
      }      
      shape = new PShape3D(parent, PShape.PRIMITIVE);
      shape.setKind(ARC);
    } else if (kind == BOX) {
      if (len != 1 && len != 3) {
        showWarning("Wrong number of parameters");
        return null;
      }           
      shape = new PShape3D(parent, PShape.PRIMITIVE);
      shape.setKind(BOX);          
    } else if (kind == SPHERE) {
      if (len != 1) {
        showWarning("Wrong number of parameters");
        return null;
      }
      shape = new PShape3D(parent, PShape.PRIMITIVE);
      shape.setKind(SPHERE);          
    }
    
    if (shape != null) {
      shape.setParams(p);
    }
    
    return shape;
  }
  
  //////////////////////////////////////////////////////////////

  // VERTEX SHAPES
  
  public void beginShape(int kind) {
    shape = kind;
    
    in.reset();
        
    breakShape = false;    
    defaultEdges = true;
    
    textureImage0 = textureImage;
    // The superclass method is called to avoid an early flush.
    super.noTexture();
  }

  
  public void endShape(int mode) {
    if (textureImage0 != null && textureImage == null && flushMode == FLUSH_WHEN_FULL) {
      // The previous shape had a texture and this one doesn't. So we need to flush
      // the textured geometry.
      textureImage = textureImage0;
      flush();
      textureImage = null;      
    }

    tessellate(mode);
    
    if (flushMode == FLUSH_CONTINUOUSLY || 
        (flushMode == FLUSH_WHEN_FULL && tess.isFull()) ||
        (flushMode == FLUSH_WHEN_FULL && drawing2D && textureImage != null && stroke)) {
      // Flushing this current shape either because we are in the flush-after-shape,
      // or the tess buffer is full or... we are in 2D mode and the shape is textured
      // and stroked, so we need to rendering right away to avoid depth-sorting issues.
      flush();
    }    
  }

  
  public void texture(PImage image) {
    if (image != textureImage0 && flushMode == FLUSH_WHEN_FULL) {
      // Changing the texture image, so we need to flush the
      // tessellated geometry accumulated until now, so that
      // textures are not mixed.
      textureImage = textureImage0;      
      flush();     
    }    
    super.texture(image);
  }
  
  
  public void noTexture() {
    if (null != textureImage0 && flushMode == FLUSH_WHEN_FULL) {
      // Changing the texture image, so we need to flush the
      // tessellated geometry accumulated until now, so that
      // textures are not mixed.      
      textureImage = textureImage0;
      flush();     
    }        
    super.noTexture();
  }
  
  
  public void beginContour() {
    if (openContour) {
      showWarning("P3D: Already called beginContour().");
      return;
    }    
    openContour = true;
  }
  
  
  public void endContour() {
    if (!openContour) {
      showWarning("P3D: Need to call beginContour() first.");
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
    boolean textured = textureImage != null;
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
            
    if (textured && textureMode == IMAGE) {
      u /= textureImage.width;
      v /= textureImage.height;
      
      PTexture tex = getTexture(textureImage);
      if (tex.isFlippedY()) {
        v = 1 - v;
      }      
    }

    drawing2D &= PApplet.abs(z) < FLOAT_EPS;  
    
    in.addVertex(x, y, z, 
                 fR, fG, fB, fA, 
                 normalX, normalY, normalZ,
                 u, v, 
                 sR, sG, sB, sA, sW, 
                 code);     
  }
   
  
  public void clip(float a, float b, float c, float d) {        
    if (imageMode == CORNER) {
      if (c < 0) {  // reset a negative width
        a += c; c = -c;
      }
      if (d < 0) {  // reset a negative height
        b += d; d = -d;
      }

      clipImpl(a, b, a + c, b + d);

    } else if (imageMode == CORNERS) {
      if (c < a) {  // reverse because x2 < x1
        float temp = a; a = c; c = temp;
      }
      if (d < b) {  // reverse because y2 < y1
        float temp = b; b = d; d = temp;
      }

      clipImpl(a, b, c, d);
      
    } else if (imageMode == CENTER) {
      // c and d are width/height
      if (c < 0) c = -c;
      if (d < 0) d = -d;
      float x1 = a - c/2;
      float y1 = b - d/2;

      clipImpl(x1, y1, x1 + c, y1 + d);
    }  
  }

  
  protected void clipImpl(float x1, float y1, float x2, float y2) {
    flush();
    gl.glEnable(GL.GL_SCISSOR_TEST);
    
    float h = y2 - y1;
    gl.glScissor((int)x1, (int)(height - y1 - h), (int)(x2 - x1), (int)h);
    
    clip = true;
  }

  
  public void noClip() {
    if (clip) {
      flush();
      gl.glDisable(GL.GL_SCISSOR_TEST);
      
      clip = false;
    }
  }  
  
  
  //////////////////////////////////////////////////////////////

  // RENDERING

  // protected void render()

  // protected void sort()  
  
  protected void tessellate(int mode) {
    tessellator.setInGeometry(in);
    tessellator.setTessGeometry(tess);
    tessellator.setFill(fill || textureImage != null);
    tessellator.setStroke(stroke);
    tessellator.setStrokeWeight(strokeWeight);
    tessellator.setStrokeCap(strokeCap);
    tessellator.setStrokeJoin(strokeJoin);
    tessellator.setStrokeColor(strokeR, strokeG, strokeB, strokeA);
    
    if (drawing2D) {
      tessellator.set2D();
    } else {
      tessellator.set3D();
    }
    
    if (shape == POINTS) {
      tessellator.tessellatePoints();    
    } else if (shape == LINES) {
      tessellator.tessellateLines();    
    } else if (shape == TRIANGLE || shape == TRIANGLES) {
      if (stroke && defaultEdges) in.addTrianglesEdges();
      tessellator.tessellateTriangles();
    } else if (shape == TRIANGLE_FAN) {
      if (stroke && defaultEdges) in.addTriangleFanEdges();
      tessellator.tessellateTriangleFan();
    } else if (shape == TRIANGLE_STRIP) {
      if (stroke && defaultEdges) in.addTriangleStripEdges();
      tessellator.tessellateTriangleStrip();
    } else if (shape == QUAD || shape == QUADS) {
      if (stroke && defaultEdges) in.addQuadsEdges();
      tessellator.tessellateQuads();
    } else if (shape == QUAD_STRIP) {
      if (stroke && defaultEdges) in.addQuadStripEdges();
      tessellator.tessellateQuadStrip();
    } else if (shape == POLYGON) {
      if (stroke && defaultEdges) in.addPolygonEdges(mode == CLOSE);
      tessellator.tessellatePolygon(false, mode == CLOSE);
    }    
  }
  
  public void flush() {    
    boolean hasPoints = 0 < tess.pointVertexCount && 0 < tess.pointIndexCount;
    boolean hasLines = 0 < tess.lineVertexCount && 0 < tess.lineIndexCount;
    boolean hasFill = 0 < tess.fillVertexCount && 0 < tess.fillIndexCount;
    
    if (hasPoints || hasLines || hasFill) {
      
      if (flushMode == FLUSH_WHEN_FULL) {
        // The geometric transformations have been applied already to the 
        // tessellated geometry, so we reset the modelview matrix to the
        // camera stage to avoid applying the model transformations twice.        
        gl2f.glPushMatrix();
        gl2f.glLoadMatrixf(pcamera, 0);
      }
      
      if (hasFill) {
        if (drawing2D && textureImage != null && tess.isStroked) {
          // If the shape is stroked and texture in 2D mode, we need to 
          // first render the textured fill geometry, and then the non-textured
          // stroke geometry. This is done in the next call.
          renderStrokedFill(textureImage);  
        } else {        
          // Normal fill rendering. Also valid in the case of 2D drawing,
          // since the fill and stroke geometry are all stored in the fill
          // buffers.
          renderFill(textureImage);
        }
      }
      
      if (hasPoints) {
        renderPoints();
      } 

      if (hasLines) {
        renderLines();
      }          
      
      if (flushMode == FLUSH_WHEN_FULL) {
        gl2f.glPopMatrix();
      }
    }
    
    tess.reset();     
  }
  

  protected void renderPoints() {
    if (!pointVBOsCreated) {
      createPointBuffers();
      pointVBOsCreated = true;
    }
    
    startPointShader();
    
    int size = tess.pointVertexCount;
    gl2f.glEnableClientState(GL2.GL_NORMAL_ARRAY);
    gl2f.glBindBuffer(GL.GL_ARRAY_BUFFER, glPointNormalBufferID);
    gl2f.glBufferData(GL.GL_ARRAY_BUFFER, 3 * size * PGraphicsOpenGL.SIZEOF_FLOAT, FloatBuffer.wrap(tess.pointNormals, 0, 3 * size), vboMode);    
    gl2f.glNormalPointer(GL.GL_FLOAT, 0, 0);
          
    gl2f.glEnableClientState(GL2.GL_COLOR_ARRAY);
    gl2f.glBindBuffer(GL.GL_ARRAY_BUFFER, glPointColorBufferID);
    gl2f.glBufferData(GL.GL_ARRAY_BUFFER, 4 * size * PGraphicsOpenGL.SIZEOF_FLOAT, FloatBuffer.wrap(tess.pointColors, 0, 4 * size), vboMode);    
    gl2f.glColorPointer(4, GL.GL_FLOAT, 0, 0);
    
    gl2f.glEnableClientState(GL2.GL_VERTEX_ARRAY);            
    gl2f.glBindBuffer(GL.GL_ARRAY_BUFFER, glPointVertexBufferID);
    gl2f.glBufferData(GL.GL_ARRAY_BUFFER, 3 * size * PGraphicsOpenGL.SIZEOF_FLOAT, FloatBuffer.wrap(tess.pointVertices, 0, 3 * size), vboMode);     
    gl2f.glVertexPointer(3, GL.GL_FLOAT, 0, 0);
    
    setupPointShader(glPointAttribBufferID, tess.pointAttributes, size);
    
    size = tess.pointIndexCount;
    gl2f.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, glPointIndexBufferID);
    gl2f.glBufferData(GL.GL_ELEMENT_ARRAY_BUFFER, size * PGraphicsOpenGL.SIZEOF_INT, IntBuffer.wrap(tess.pointIndices, 0, size), vboMode);     
    gl2f.glDrawElements(GL.GL_TRIANGLES, size, GL.GL_UNSIGNED_INT, 0);
    
    gl2f.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, 0);
    gl2f.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);    
    
    gl2f.glDisableClientState(GL2.GL_VERTEX_ARRAY);
    gl2f.glDisableClientState(GL2.GL_COLOR_ARRAY);
    gl2f.glDisableClientState(GL2.GL_NORMAL_ARRAY);
    
    stopPointShader();
  }  
    
  protected void renderLines() {
    if (!lineVBOsCreated) {
      createLineBuffers();
      lineVBOsCreated = true;
    }
    
    startLineShader();
    
    int size = tess.lineVertexCount;
    gl2f.glEnableClientState(GL2.GL_NORMAL_ARRAY);
    gl2f.glBindBuffer(GL.GL_ARRAY_BUFFER, glLineNormalBufferID);
    gl2f.glBufferData(GL.GL_ARRAY_BUFFER, 3 * size * PGraphicsOpenGL.SIZEOF_FLOAT, FloatBuffer.wrap(tess.lineNormals, 0, 3 * size), vboMode);
    gl2f.glNormalPointer(GL.GL_FLOAT, 0, 0);
          
    gl2f.glEnableClientState(GL2.GL_COLOR_ARRAY);
    gl2f.glBindBuffer(GL.GL_ARRAY_BUFFER, glLineColorBufferID);
    gl2f.glBufferData(GL.GL_ARRAY_BUFFER, 4 * size * PGraphicsOpenGL.SIZEOF_FLOAT, FloatBuffer.wrap(tess.lineColors, 0, 4 * size), vboMode);
    gl2f.glColorPointer(4, GL.GL_FLOAT, 0, 0);
    
    gl2f.glEnableClientState(GL2.GL_VERTEX_ARRAY);            
    gl2f.glBindBuffer(GL.GL_ARRAY_BUFFER, glLineVertexBufferID);
    gl2f.glBufferData(GL.GL_ARRAY_BUFFER, 3 * size * PGraphicsOpenGL.SIZEOF_FLOAT, FloatBuffer.wrap(tess.lineVertices, 0, 3 * size), vboMode);
    gl2f.glVertexPointer(3, GL.GL_FLOAT, 0, 0);
    
    setupLineShader(glLineAttribBufferID, tess.lineAttributes, size);
    
    size = tess.lineIndexCount;
    gl2f.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, glLineIndexBufferID);
    gl2f.glBufferData(GL.GL_ELEMENT_ARRAY_BUFFER, size * PGraphicsOpenGL.SIZEOF_INT, IntBuffer.wrap(tess.lineIndices, 0, size), vboMode);
    gl2f.glDrawElements(GL.GL_TRIANGLES, size, GL.GL_UNSIGNED_INT, 0);
    
    gl2f.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, 0);
    gl2f.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);    
    
    gl2f.glDisableClientState(GL2.GL_VERTEX_ARRAY);
    gl2f.glDisableClientState(GL2.GL_COLOR_ARRAY);
    gl2f.glDisableClientState(GL2.GL_NORMAL_ARRAY);
    
    stopLineShader();
  }  
  
  protected void renderFill0(PImage textureImage) {
    float[] colors = tess.fillColors;
    float[] normals = tess.fillNormals;
    float[] texcoords = tess.fillTexcoords;
    float[] vertices = tess.fillVertices;
    int[] indices = tess.fillIndices;
    
    
    //gl2f.glBindVertexArray
    
    //gl2x.glVertexAttribPointer(arg0)
    //gl2.glVertexAttribPointer(arg0)
    
    PTexture tex = null;
    if (textureImage != null) {
      tex = ogl.getTexture(textureImage);
      if (tex != null) {
        gl2f.glEnable(tex.glTarget);
        gl2f.glActiveTexture(GL.GL_TEXTURE0);
        gl2f.glBindTexture(tex.glTarget, tex.glID);
      } 
    }         
    
    int size = tess.fillIndexCount;
    gl2.glBegin(GL.GL_TRIANGLES);
    for (int tr = 0; tr < size / 3; tr++) {
            
      for (int pt = 0; pt < 3; pt++) {
        int i = indices[3 * tr + pt];
        gl2.glColor4f(colors[4 * i + 0], colors[4 * i + 1], colors[4 * i + 2], colors[4 * i + 3]);
        gl2.glNormal3f(normals[3 * i + 0], normals[3 * i + 1], normals[3 * i + 2]);
        if (tex != null) {
          gl2.glTexCoord2f(texcoords[2 * i + 0], texcoords[2 * i + 1]);          
        }
        gl2.glVertex3f(vertices[3 * i + 0], vertices[3 * i + 1], vertices[3 * i + 2]);        
      }
      
      
    }    
    gl2.glEnd();
    
    if (tex != null) {
      gl2f.glActiveTexture(GL.GL_TEXTURE0);
      gl2f.glBindTexture(tex.glTarget, 0);
      gl2f.glDisable(tex.glTarget);
    }       
  }
  
  protected void renderFill(PImage textureImage) {
    if (!fillVBOsCreated) {
      createFillBuffers();
      fillVBOsCreated = true;
    }    

    int size = tess.fillVertexCount;
    gl2f.glEnableClientState(GL2.GL_NORMAL_ARRAY);
    gl2f.glBindBuffer(GL.GL_ARRAY_BUFFER, glFillNormalBufferID);
    gl2f.glBufferData(GL.GL_ARRAY_BUFFER, 3 * size * PGraphicsOpenGL.SIZEOF_FLOAT, FloatBuffer.wrap(tess.fillNormals, 0, 3 * size), vboMode);
    gl2f.glNormalPointer(GL.GL_FLOAT, 0, 0);

    gl2f.glEnableClientState(GL2.GL_COLOR_ARRAY);
    gl2f.glBindBuffer(GL.GL_ARRAY_BUFFER, glFillColorBufferID);
    gl2f.glBufferData(GL.GL_ARRAY_BUFFER, 4 * size * PGraphicsOpenGL.SIZEOF_FLOAT, FloatBuffer.wrap(tess.fillColors, 0, 4 * size), vboMode);
    gl2f.glColorPointer(4, GL.GL_FLOAT, 0, 0);    
    
    gl2f.glEnableClientState(GL2.GL_VERTEX_ARRAY);
    gl2f.glBindBuffer(GL.GL_ARRAY_BUFFER, glFillVertexBufferID);   
    gl2f.glBufferData(GL.GL_ARRAY_BUFFER, 3 * size * PGraphicsOpenGL.SIZEOF_FLOAT, FloatBuffer.wrap(tess.fillVertices, 0, 3 * size), vboMode);
    gl2f.glVertexPointer(3, GL.GL_FLOAT, 0, 0);

    PTexture tex = null;
    if (textureImage != null) {
      tex = ogl.getTexture(textureImage);
      if (tex != null) {
        gl2f.glEnable(tex.glTarget);
        gl2f.glActiveTexture(GL.GL_TEXTURE0);
        gl2f.glBindTexture(tex.glTarget, tex.glID);
      }
      
      gl2f.glEnableClientState(GL2.GL_TEXTURE_COORD_ARRAY);
      gl2f.glBindBuffer(GL.GL_ARRAY_BUFFER, glFillTexCoordBufferID);
      gl2f.glBufferData(GL.GL_ARRAY_BUFFER, 2 * size * PGraphicsOpenGL.SIZEOF_FLOAT, FloatBuffer.wrap(tess.fillTexcoords, 0, 2 * size), vboMode);
      gl2f.glTexCoordPointer(2, GL.GL_FLOAT, 0, 0); 
    }        
    
    size = tess.fillIndexCount;
    gl2f.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, glFillIndexBufferID);
    gl2f.glBufferData(GL.GL_ELEMENT_ARRAY_BUFFER, size * PGraphicsOpenGL.SIZEOF_INT, IntBuffer.wrap(tess.fillIndices, 0, size), vboMode);
    gl2f.glDrawElements(GL.GL_TRIANGLES, size, GL.GL_UNSIGNED_INT, 0);        
    
    if (tex != null) {
      gl2f.glActiveTexture(GL.GL_TEXTURE0);
      gl2f.glBindTexture(tex.glTarget, 0);
      gl2f.glDisable(tex.glTarget);
      gl2f.glDisableClientState(GL2.GL_TEXTURE_COORD_ARRAY);
    }     
    
    gl2f.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, 0);
    gl2f.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
    
    gl2f.glDisableClientState(GL2.GL_VERTEX_ARRAY);    
    gl2f.glDisableClientState(GL2.GL_COLOR_ARRAY);
    gl2f.glDisableClientState(GL2.GL_NORMAL_ARRAY);    
  }
  
  
  protected void renderStrokedFill(PImage textureImage) {
    if (!fillVBOsCreated) {
      createFillBuffers();
      fillVBOsCreated = true;
    }    

    int size = tess.fillVertexCount;
    gl2f.glEnableClientState(GL2.GL_NORMAL_ARRAY);
    gl2f.glBindBuffer(GL.GL_ARRAY_BUFFER, glFillNormalBufferID);
    gl2f.glBufferData(GL.GL_ARRAY_BUFFER, 3 * size * PGraphicsOpenGL.SIZEOF_FLOAT, FloatBuffer.wrap(tess.fillNormals, 0, 3 * size), vboMode);
    gl2f.glNormalPointer(GL.GL_FLOAT, 0, 0);

    gl2f.glEnableClientState(GL2.GL_COLOR_ARRAY);
    gl2f.glBindBuffer(GL.GL_ARRAY_BUFFER, glFillColorBufferID);    
    gl2f.glBufferData(GL.GL_ARRAY_BUFFER, 4 * size * PGraphicsOpenGL.SIZEOF_FLOAT, FloatBuffer.wrap(tess.fillColors, 0, 4 * size), vboMode);
    gl2f.glColorPointer(4, GL.GL_FLOAT, 0, 0);    
    
    gl2f.glEnableClientState(GL2.GL_VERTEX_ARRAY);
    gl2f.glBindBuffer(GL.GL_ARRAY_BUFFER, glFillVertexBufferID);
    gl2f.glBufferData(GL.GL_ARRAY_BUFFER, 3 * size * PGraphicsOpenGL.SIZEOF_FLOAT, FloatBuffer.wrap(tess.fillVertices, 0, 3 * size), vboMode);
    gl2f.glVertexPointer(3, GL.GL_FLOAT, 0, 0);

    PTexture tex = null;
    if (textureImage != null) {
      tex = ogl.getTexture(textureImage);
      if (tex != null) {
        gl2f.glEnable(tex.glTarget);
        gl2f.glActiveTexture(GL.GL_TEXTURE0);
        gl2f.glBindTexture(tex.glTarget, tex.glID);
      }
      
      gl2f.glEnableClientState(GL2.GL_TEXTURE_COORD_ARRAY);
      gl2f.glBindBuffer(GL.GL_ARRAY_BUFFER, glFillTexCoordBufferID);
      gl2f.glBufferData(GL.GL_ARRAY_BUFFER, 2 * size * PGraphicsOpenGL.SIZEOF_FLOAT, FloatBuffer.wrap(tess.fillTexcoords, 0, 2 * size), vboMode); 
      gl2f.glTexCoordPointer(2, GL.GL_FLOAT, 0, 0); 
    }        
    
    // Draw the textured fill geometry (reaches up to the tess.firstLineIndex - 1 vertex).
    size = tess.firstLineIndex;
    gl2f.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, glFillIndexBufferID);
    gl2f.glBufferData(GL.GL_ELEMENT_ARRAY_BUFFER, size * PGraphicsOpenGL.SIZEOF_INT, IntBuffer.wrap(tess.fillIndices, 0, size), vboMode);
    gl2f.glDrawElements(GL.GL_TRIANGLES, tess.firstLineIndex, GL.GL_UNSIGNED_INT, 0);        
    
    if (tex != null) {
      gl2f.glActiveTexture(GL.GL_TEXTURE0);
      gl2f.glBindTexture(tex.glTarget, 0);
      gl2f.glDisable(tex.glTarget);
      gl2f.glDisableClientState(GL2.GL_TEXTURE_COORD_ARRAY);
    }     

    // Drawing the stroked lines without texture.
    int offset = tess.firstLineIndex;
    size = tess.lastLineIndex - tess.firstLineIndex + 1;
    gl2f.glBufferData(GL.GL_ELEMENT_ARRAY_BUFFER, size * PGraphicsOpenGL.SIZEOF_INT, IntBuffer.wrap(tess.fillIndices, offset, size), vboMode);
    gl2f.glDrawElements(GL.GL_TRIANGLES, size, GL.GL_UNSIGNED_INT, 0);        

    gl2f.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, 0);
    gl2f.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
    
    gl2f.glDisableClientState(GL2.GL_VERTEX_ARRAY);    
    gl2f.glDisableClientState(GL2.GL_COLOR_ARRAY);
    gl2f.glDisableClientState(GL2.GL_NORMAL_ARRAY);    
  }

  
  protected void startLineShader() {
    if (lineShader == null) {
      lineShader = new PShader(parent);      
      lineShader.loadVertexShader(PGraphicsOpenGL.class.getResource("LineShaderVert.glsl"));
      lineShader.loadFragmentShader(PGraphicsOpenGL.class.getResource("LineShaderFrag.glsl"));
      lineShader.setup();
    }
    
    lineShader.start();
  }

  
  protected void setupLineShader(int attrBufID, float[] attribs, int nvert) {
    int[] viewport = {0, 0, 0, 0};
    gl2f.glGetIntegerv(GL.GL_VIEWPORT, viewport, 0);    
    lineShader.setVecUniform("viewport", viewport[0], viewport[1], viewport[2], viewport[3]);
    
    lineShader.setIntUniform("lights", lightCount);           
        
    lineShader.setVecUniform("eye", cameraEyeX, cameraEyeY, cameraEyeZ, 0);
    
    lineAttribsID = lineShader.getAttribLocation("attribs");     
    gl2x.glEnableVertexAttribArray(lineAttribsID);
    gl2f.glBindBuffer(GL.GL_ARRAY_BUFFER, attrBufID);      
    gl2f.glBufferSubData(GL.GL_ARRAY_BUFFER, 0, 4 * nvert * PGraphicsOpenGL.SIZEOF_FLOAT, 
                         FloatBuffer.wrap(attribs));    
    gl2x.glVertexAttribPointer(lineAttribsID, 4, GL.GL_FLOAT, false, 0, 0);        
  }
  
  
  protected void setupLineShader(int attrBufID) {
    int[] viewport = {0, 0, 0, 0};
    gl2f.glGetIntegerv(GL.GL_VIEWPORT, viewport, 0);    
    lineShader.setVecUniform("viewport", viewport[0], viewport[1], viewport[2], viewport[3]);
    
    lineShader.setIntUniform("lights", lightCount);           
        
    lineShader.setVecUniform("eye", cameraEyeX, cameraEyeY, cameraEyeZ, 0);
    
    lineAttribsID = lineShader.getAttribLocation("attribs");     
    gl2x.glEnableVertexAttribArray(lineAttribsID);
    gl2f.glBindBuffer(GL.GL_ARRAY_BUFFER, attrBufID);      
    gl2x.glVertexAttribPointer(lineAttribsID, 4, GL.GL_FLOAT, false, 0, 0);          
  }
  
  
  protected void stopLineShader() {
    gl2x.glDisableVertexAttribArray(lineAttribsID);
    lineShader.stop();
  }  
  
  
  protected void startPointShader() {
    if (pointShader == null) {
      pointShader = new PShader(parent);
      pointShader.loadVertexShader(PGraphicsOpenGL.class.getResource("PointShaderVert.glsl"));
      pointShader.loadFragmentShader(PGraphicsOpenGL.class.getResource("PointShaderFrag.glsl"));
      pointShader.setup();
    }    
    
    pointShader.start();    
  }
  
  
  protected void setupPointShader(int attrBufID, float[] attribs, int nvert) {
    pointShader.setIntUniform("lights", lightCount);           
    
    pointShader.setVecUniform("eye", cameraEyeX, cameraEyeY, cameraEyeZ, 0);
    
    pointAttribsID = PGraphicsOpenGL.pointShader.getAttribLocation("vertDisp");     
    gl2x.glEnableVertexAttribArray(pointAttribsID);
    gl2f.glBindBuffer(GL.GL_ARRAY_BUFFER, attrBufID);
    gl2f.glBufferSubData(GL.GL_ARRAY_BUFFER, 0, 2 * nvert * PGraphicsOpenGL.SIZEOF_FLOAT, 
                         FloatBuffer.wrap(attribs));    
    ogl.gl2x.glVertexAttribPointer(pointAttribsID, 2, GL.GL_FLOAT, false, 0, 0);      
  }
  
  
  protected void setupPointShader(int attrBufID) {
    pointShader.setIntUniform("lights", lightCount);           
    
    pointShader.setVecUniform("eye", cameraEyeX, cameraEyeY, cameraEyeZ, 0);
    
    pointAttribsID = PGraphicsOpenGL.pointShader.getAttribLocation("vertDisp");     
    gl2x.glEnableVertexAttribArray(pointAttribsID);
    gl2f.glBindBuffer(GL.GL_ARRAY_BUFFER, attrBufID);      
    ogl.gl2x.glVertexAttribPointer(pointAttribsID, 2, GL.GL_FLOAT, false, 0, 0);      
  }
  
  
  protected void stopPointShader() {
    gl2x.glDisableVertexAttribArray(pointAttribsID);
    pointShader.stop();  
  }

  //////////////////////////////////////////////////////////////

  // PSHAPE RENDERING IN 3D

  public void shape(PShape shape, float x, float y, float z) {
    if (shape.isVisible()) { // don't do expensive matrix ops if invisible
      pushMatrix();

      if (shapeMode == CENTER) {
        translate(x - shape.getWidth() / 2, y - shape.getHeight() / 2, z
            - shape.getDepth() / 2);

      } else if ((shapeMode == CORNER) || (shapeMode == CORNERS)) {
        translate(x, y, z);
      }
      shape.draw(this);

      popMatrix();
    }
  }

  public void shape(PShape shape, float x, float y, float z, float c, float d,
      float e) {
    if (shape.isVisible()) { // don't do expensive matrix ops if invisible
      pushMatrix();

      if (shapeMode == CENTER) {
        // x, y and z are center, c, d and e refer to a diameter
        translate(x - c / 2f, y - d / 2f, z - e / 2f);
        scale(c / shape.getWidth(), d / shape.getHeight(), e / shape.getDepth());

      } else if (shapeMode == CORNER) {
        translate(x, y, z);
        scale(c / shape.getWidth(), d / shape.getHeight(), e / shape.getDepth());

      } else if (shapeMode == CORNERS) {
        // c, d, e are x2/y2/z2, make them into width/height/depth
        c -= x;
        d -= y;
        e -= z;
        // then same as above
        translate(x, y, z);
        scale(c / shape.getWidth(), d / shape.getHeight(), e / shape.getDepth());
      }
      shape.draw(this);

      popMatrix();
    }
  }

  //////////////////////////////////////////////////////////////

  // BEZIER CURVE VERTICES

  public void bezierDetail(int detail) {
    bezierDetail = detail;

    if (bezierDrawMatrix == null) {
      bezierDrawMatrix = new PMatrix3D();
    }

    // setup matrix for forward differencing to speed up drawing
    ogl.splineForward(detail, bezierDrawMatrix);

    // multiply the basis and forward diff matrices together
    // saves much time since this needn't be done for each curve
    bezierDrawMatrix.apply(ogl.bezierBasisMatrix);
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
    bezierInitCheck();
    bezierVertexCheck();
    PMatrix3D draw = bezierDrawMatrix;

    float x1 = in.getlastVertexX();
    float y1 = in.getlastVertexY();
    float z1 = in.getlastVertexZ();

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
    float x1 = in.getlastVertexX();
    float y1 = in.getlastVertexY();
    float z1 = in.getlastVertexZ();

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
    if (shape != POLYGON) {
      throw new RuntimeException("beginShape() or beginShape(POLYGON) " +
                                 "must be used before bezierVertex() or quadraticVertex()");
    }
    if (in.vertexCount == 0) {
      throw new RuntimeException("vertex() must be used at least once" +
                                 "before bezierVertex() or quadraticVertex()");
    }
  }    

  //////////////////////////////////////////////////////////////

  // CATMULL-ROM CURVE VERTICES    

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
    if (shape != POLYGON) {
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

    ogl.splineForward(curveDetail, curveDrawMatrix);

    if (bezierBasisInverse == null) {
      bezierBasisInverse = ogl.bezierBasisMatrix.get();
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

  //////////////////////////////////////////////////////////////

  // SPLINE UTILITY FUNCTIONS (used by both Bezier and Catmull-Rom)

  /**
   * Setup forward-differencing matrix to be used for speedy
   * curve rendering. It's based on using a specific number
   * of curve segments and just doing incremental adds for each
   * vertex of the segment, rather than running the mathematically
   * expensive cubic equation.
   * @param segments number of curve segments to use when drawing
   * @param matrix target object for the new matrix
   */
  protected void splineForward(int segments, PMatrix3D matrix) {
    float f  = 1.0f / segments;
    float ff = f * f;
    float fff = ff * f;

    matrix.set(0,     0,    0, 1,
               fff,   ff,   f, 0,
               6*fff, 2*ff, 0, 0,
               6*fff, 0,    0, 0);
  }  
  

  //////////////////////////////////////////////////////////////

  // POINT, LINE, TRIANGLE, QUAD

  // Because vertex(x, y) is mapped to vertex(x, y, 0), none of these commands
  // need to be overridden from their default implementation in PGraphics.

  // public void point(float x, float y)

  // public void point(float x, float y, float z)

  // public void line(float x1, float y1, float x2, float y2)

  // public void line(float x1, float y1, float z1,
  // float x2, float y2, float z2)

  // public void triangle(float x1, float y1, float x2, float y2,
  // float x3, float y3)

  // public void quad(float x1, float y1, float x2, float y2,
  // float x3, float y3, float x4, float y4)

  //////////////////////////////////////////////////////////////

  // RECT

  // public void rectMode(int mode)

  // public void rect(float a, float b, float c, float d)

  // protected void rectImpl(float x1, float y1, float x2, float y2)

  //////////////////////////////////////////////////////////////

  // ELLIPSE

  // public void ellipseMode(int mode)

  
  public void ellipse(float a, float b, float c, float d) {
     beginShape(TRIANGLE_FAN); 
     defaultEdges = false;
     in.generateEllipse(ellipseMode, a, b, c, d, 
                        fill, fillR, fillG, fillB, fillA, 
                        stroke, strokeR, strokeG, strokeB, strokeA,
                        strokeWeight);
     endShape();
  }
  
  
  // public void ellipse(float a, float b, float c, float d)

  // public void arc(float a, float b, float c, float d,
  // float start, float stop)

  //////////////////////////////////////////////////////////////

  // ARC
    
  protected void arcImpl(float x, float y, float w, float h,
                         float start, float stop) {
    float hr = w / 2f;
    float vr = h / 2f;

    float centerX = x + hr;
    float centerY = y + vr;

    if (fill) {
      // shut off stroke for a minute
      boolean savedStroke = stroke;
      stroke = false;

      int startLUT = (int) (0.5f + (start / TWO_PI) * SINCOS_LENGTH);
      int stopLUT = (int) (0.5f + (stop / TWO_PI) * SINCOS_LENGTH);

      beginShape(TRIANGLE_FAN);
      vertex(centerX, centerY);
      int increment = 1; // what's a good algorithm? stopLUT - startLUT;
      for (int i = startLUT; i < stopLUT; i += increment) {
        int ii = i % SINCOS_LENGTH;
        // modulo won't make the value positive
        if (ii < 0) ii += SINCOS_LENGTH;
        vertex(centerX + cosLUT[ii] * hr,
               centerY + sinLUT[ii] * vr);
      }
      // draw last point explicitly for accuracy
      vertex(centerX + cosLUT[stopLUT % SINCOS_LENGTH] * hr,
             centerY + sinLUT[stopLUT % SINCOS_LENGTH] * vr);
      endShape();

      stroke = savedStroke;
    }

    if (stroke) {
      // Almost identical to above, but this uses a LINE_STRIP
      // and doesn't include the first (center) vertex.

      boolean savedFill = fill;
      fill = false;

      int startLUT = (int) (0.5f + (start / TWO_PI) * SINCOS_LENGTH);
      int stopLUT = (int) (0.5f + (stop / TWO_PI) * SINCOS_LENGTH);

      beginShape(); //LINE_STRIP);
      int increment = 1; // what's a good algorithm? stopLUT - startLUT;
      for (int i = startLUT; i < stopLUT; i += increment) {
        int ii = i % SINCOS_LENGTH;
        if (ii < 0) ii += SINCOS_LENGTH;
        vertex(centerX + cosLUT[ii] * hr,
               centerY + sinLUT[ii] * vr);
      }
      // draw last point explicitly for accuracy
      vertex(centerX + cosLUT[stopLUT % SINCOS_LENGTH] * hr,
             centerY + sinLUT[stopLUT % SINCOS_LENGTH] * vr);
      endShape();

      fill = savedFill;
    }
  }

  //////////////////////////////////////////////////////////////

  // BOX

  // TODO GL and GLUT in GL ES doesn't offer functions to create
  // cubes.

  // public void box(float size)

  // public void box(float w, float h, float d) // P3D

  //////////////////////////////////////////////////////////////

  // SPHERE

  // TODO GL and GLUT in GL ES doesn't offer functions to create
  // spheres.

  // public void sphereDetail(int res)

  // public void sphereDetail(int ures, int vres)

  // public void sphere(float r)

  //////////////////////////////////////////////////////////////

  // BEZIER

  // public float bezierPoint(float a, float b, float c, float d, float t)

  // public float bezierTangent(float a, float b, float c, float d, float t)

  // public void bezierDetail(int detail)

  // public void bezier(float x1, float y1,
  // float x2, float y2,
  // float x3, float y3,
  // float x4, float y4)

  // public void bezier(float x1, float y1, float z1,
  // float x2, float y2, float z2,
  // float x3, float y3, float z3,
  // float x4, float y4, float z4)

  //////////////////////////////////////////////////////////////

  // CATMULL-ROM CURVES

  // public float curvePoint(float a, float b, float c, float d, float t)

  // public float curveTangent(float a, float b, float c, float d, float t)

  // public void curveDetail(int detail)

  // public void curveTightness(float tightness)

  // public void curve(float x1, float y1,
  // float x2, float y2,
  // float x3, float y3,
  // float x4, float y4)

  // public void curve(float x1, float y1, float z1,
  // float x2, float y2, float z2,
  // float x3, float y3, float z3,
  // float x4, float y4, float z4)

  //////////////////////////////////////////////////////////////

  // IMAGES

  // public void imageMode(int mode)

  // public void image(PImage image, float x, float y)

  // public void image(PImage image, float x, float y, float c, float d)

  // public void image(PImage image,
  // float a, float b, float c, float d,
  // int u1, int v1, int u2, int v2)

  // protected void imageImpl(PImage image,
  // float x1, float y1, float x2, float y2,
  // int u1, int v1, int u2, int v2)

  //////////////////////////////////////////////////////////////

  // SMOOTH
  public void smooth() {
    smooth(2);
  }
  
  
  public void smooth(int level) {
    smooth = true;
    
    if (antialias != level) {
      antialias = level;
      if (primarySurface) {
        restartContext();          
//        throw new PApplet.RendererChangeException();
      } else {
        initOffscreen();
      }
    }
    
    int[] temp = { 0 };
    gl.glGetIntegerv(GL.GL_SAMPLES, temp, 0);
    if (antialias != temp[0]) {
      antialias = temp[0];
      PApplet.println("Effective multisampling level: " + antialias);
    }
    
    if (antialias < 2) {
      gl2f.glEnable(GL2.GL_MULTISAMPLE);
      gl2f.glEnable(GL2.GL_POINT_SMOOTH);
      gl2f.glEnable(GL2.GL_LINE_SMOOTH);
      gl2f.glEnable(GL2.GL_POLYGON_SMOOTH);      
    }    
  }

  
  public void noSmooth() {
    smooth = false;
    
    if (1 < antialias) {
      antialias = 0;
      if (primarySurface) {
        restartContext();          
        //throw new PApplet.RendererChangeException();
      } else {
        initOffscreen();
      }      
    }
    
    gl2f.glDisable(GL2.GL_MULTISAMPLE);
    gl2f.glDisable(GL2.GL_POINT_SMOOTH);
    gl2f.glDisable(GL.GL_LINE_SMOOTH);
    gl2f.glDisable(GL2.GL_POLYGON_SMOOTH);
  }   
  
  //////////////////////////////////////////////////////////////

  // SHAPE

  // public void shapeMode(int mode)

  public void shape(PShape3D shape) {
    shape.draw(this);
  }

  public void shape(PShape3D shape, float x, float y) {
    shape(shape, x, y, 0);
  }

  public void shape(PShape3D shape, float x, float y, float z) {
    pushMatrix();
    translate(x, y, z);
    shape.draw(this);
    popMatrix();
  }

  // public void shape(PShape shape, float x, float y, float c, float d)

  //////////////////////////////////////////////////////////////

  // TEXT SETTINGS

  // public void textAlign(int align)

  // public void textAlign(int alignX, int alignY)

  // public float textAscent()

  // public float textDescent()

  // public void textFont(PFont which)

  // public void textFont(PFont which, float size)

  // public void textLeading(float leading)

  // public void textMode(int mode)

  protected boolean textModeCheck(int mode) {
    return mode == MODEL;
  }

  // public void textSize(float size)

  // public float textWidth(char c)

  // public float textWidth(String str)

  // protected float textWidthImpl(char buffer[], int start, int stop)

  
  //////////////////////////////////////////////////////////////

  // TEXT IMPL

  // protected void textLineAlignImpl(char buffer[], int start, int stop,
  // float x, float y)
  
  /**
   * Implementation of actual drawing for a line of text.
   */
  protected void textLineImpl(char buffer[], int start, int stop, float x, float y) {
    textTex = (PFontTexture)textFont.getCache(ogl);     
    if (textTex == null) {
      textTex = new PFontTexture(parent, textFont, maxTextureSize, maxTextureSize);
      textFont.setCache(this, textTex);
    } else {
      if (context.hashCode() != textTex.context.hashCode()) {
        for (int i = 0; i < textTex.textures.length; i++) {
          textTex.textures[i].glID = 0; // To avoid finalization (texture objects were already deleted when context changed).
          textTex.textures[i] = null;
        }
        textTex = new PFontTexture(parent, textFont, maxTextureSize, maxTextureSize);
        textFont.setCache(this, textTex);
      }
    }
    textTex.setFirstTexture();
    
    // Saving style parameters modified by text rendering.
    int savedTextureMode = textureMode;
    boolean savedStroke = stroke;
    float savedNormalX = normalX;
    float savedNormalY = normalY;
    float savedNormalZ = normalZ;
    boolean savedTint = tint;
    float savedTintR = tintR;
    float savedTintG = tintG;
    float savedTintB = tintB;
    float savedTintA = tintA;
    int savedBlendMode = blendMode;
    
    // Setting style used in text rendering.
    textureMode = NORMAL;    
    stroke = false;    
    normalX = 0;
    normalY = 0;
    normalZ = 1;    
    tint = true;
    tintR = fillR;
    tintG = fillG;
    tintB = fillB;
    tintA = fillA;        
    
    blendMode(BLEND);
    
    super.textLineImpl(buffer, start, stop, x, y);
       
    // Restoring original style.
    textureMode  = savedTextureMode;
    stroke = savedStroke;
    normalX = savedNormalX;
    normalY = savedNormalY;
    normalZ = savedNormalZ;
    tint = savedTint;
    tintR = savedTintR;
    tintG = savedTintG;
    tintB = savedTintB;
    tintA = savedTintA;
    
    // Note that if the user is using a blending mode different from
    // BLEND, and has a bunch of continuous text rendering, the performance
    // won't be optimal because at the end of each text() call the geometry
    // will be flushed when restoring the user's blend.
    blendMode(savedBlendMode);
  }

  protected void textCharImpl(char ch, float x, float y) {
    PFont.Glyph glyph = textFont.getGlyph(ch);
    
    if (glyph != null) {      
      PFontTexture.TextureInfo tinfo = textTex.getTexInfo(glyph);
      
      if (tinfo == null) {
        // Adding new glyph to the font texture.
        tinfo = textTex.addToTexture(glyph);
      }
      
      if (textMode == MODEL) {       
        float high = glyph.height / (float) textFont.getSize();
        float bwidth = glyph.width / (float) textFont.getSize();
        float lextent = glyph.leftExtent / (float) textFont.getSize();
        float textent = glyph.topExtent / (float) textFont.getSize();

        float x1 = x + lextent * textSize;
        float y1 = y - textent * textSize;
        float x2 = x1 + bwidth * textSize;
        float y2 = y1 + high * textSize;

        textCharModelImpl(tinfo, x1, y1, x2, y2);
      } 
    }
  }


  protected void textCharModelImpl(PFontTexture.TextureInfo info, float x0, float y0,
      float x1, float y1) {
    if (textTex.currentTex != info.texIndex) {
      textTex.setTexture(info.texIndex);
    }    
    PImage tex = textTex.getCurrentTexture();
    
    beginShape(QUADS);
    texture(tex);    
    vertex(x0, y0, info.u0, info.v0);
    vertex(x1, y0, info.u1, info.v0);
    vertex(x1, y1, info.u1, info.v1);
    vertex(x0, y1, info.u0, info.v1);
    endShape();
  }
  
  
  //////////////////////////////////////////////////////////////

  // MATRIX STACK

  
  public void pushMatrix() {
    gl2f.glPushMatrix();  
    modelviewStack.push();
  }

  
  public void popMatrix() {
    gl2f.glPopMatrix();
    modelviewStack.pop();
  }
  
  
  //////////////////////////////////////////////////////////////

  // MATRIX TRANSFORMATIONS

  public void translate(float tx, float ty) {
    translate(tx, ty, 0);
  }

  public void translate(float tx, float ty, float tz) {
    gl2f.glTranslatef(tx, ty, tz);
    modelviewStack.translate(tx, ty, tz);
    modelviewUpdated = false;
    
    drawing2D &= PApplet.abs(tz) < FLOAT_EPS;
  }

  /**
   * Two dimensional rotation. Same as rotateZ (this is identical to a 3D
   * rotation along the z-axis) but included for clarity -- it'd be weird for
   * people drawing 2D graphics to be using rotateZ. And they might kick our a--
   * for the confusion.
   */
  public void rotate(float angle) {
    rotateZ(angle);
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

  /**
   * Rotate around an arbitrary vector, similar to glRotate(), except that it
   * takes radians (instead of degrees).
   */
  public void rotate(float angle, float v0, float v1, float v2) {
    gl2f.glRotatef(PApplet.degrees(angle), v0, v1, v2);
    modelviewStack.rotate(angle, v0, v1, v2);
    modelviewUpdated = false;
    
    drawing2D &= PApplet.abs(v0) < FLOAT_EPS && PApplet.abs(v1) < FLOAT_EPS;
  }

  /**
   * Same as scale(s, s, s).
   */
  public void scale(float s) {
    scale(s, s, s);
  }

  /**
   * Same as scale(sx, sy, 1).
   */
  public void scale(float sx, float sy) {
    scale(sx, sy, 1);
  }

  /**
   * Scale in three dimensions.
   */
  public void scale(float sx, float sy, float sz) {
    gl2f.glScalef(sx, sy, sz);
    modelviewStack.scale(sx, sy, sz);
    modelviewUpdated = false;
  }

  public void shearX(float angle) {
    float t = (float) Math.tan(angle);
    applyMatrix(1, t, 0, 0, 
                0, 1, 0, 0, 
                0, 0, 1, 0, 
                0, 0, 0, 1);
  }

  public void shearY(float angle) {
    float t = (float) Math.tan(angle);
    applyMatrix(1, 0, 0, 0, 
                t, 1, 0, 0, 
                0, 0, 1, 0, 
                0, 0, 0, 1);
  }

  //////////////////////////////////////////////////////////////

  // MATRIX MORE!

    
  public void resetMatrix() {
    gl2f.glLoadIdentity();    
    modelviewStack.setIdentity();
    modelviewUpdated = false;
  }

  public void applyMatrix(PMatrix2D source) {
    applyMatrix(source.m00, source.m01, source.m02, 
                source.m10, source.m11, source.m12);
  }

  public void applyMatrix(float n00, float n01, float n02, 
                          float n10, float n11, float n12) {
    applyMatrix(n00, n01, n02, 0, 
                n10, n11, n12, 0, 
                0,   0,   1,   0, 
                0,   0,   0,   1);
  }

  public void applyMatrix(PMatrix3D source) {
    applyMatrix(source.m00, source.m01, source.m02, source.m03, 
                source.m10, source.m11, source.m12, source.m13, 
                source.m20, source.m21, source.m22, source.m23, 
                source.m30, source.m31, source.m32, source.m33);
  }

  /**
   * Apply a 4x4 transformation matrix to the modelview stack using
   * glMultMatrix(). This call will be slow because it will try to calculate the
   * inverse of the transform. So avoid it whenever possible.
   */
  public void applyMatrix(float n00, float n01, float n02, float n03,
                          float n10, float n11, float n12, float n13, 
                          float n20, float n21, float n22, float n23, 
                          float n30, float n31, float n32, float n33) {
    gltemp[ 0] = n00; gltemp[ 4] = n01; gltemp[ 8] = n02; gltemp[12] = n03;
    gltemp[ 1] = n10; gltemp[ 5] = n11; gltemp[ 9] = n12; gltemp[13] = n13;
    gltemp[ 2] = n20; gltemp[ 6] = n21; gltemp[10] = n22; gltemp[14] = n23;
    gltemp[ 3] = n30; gltemp[ 7] = n31; gltemp[11] = n32; gltemp[15] = n33;

    gl2f.glMultMatrixf(gltemp, 0);

    modelviewStack.mult(n00, n01, n02, n03,
                        n10, n11, n12, n13,
                        n20, n21, n22, n23,
                        n30, n31, n32, n33);
    modelviewUpdated = false;
    
    drawing2D = false;
    // drawing2D &= drawing2D(); // could use this?
  }

  public void updateModelview() {
    updateModelview(true);
  }
  
  public void updateModelview(boolean calcInv) {
    if (calcInv) {
      modelviewInv.set(modelview);
      modelviewInv.invert();
    }
    
    copyPMatrixToGLArray(modelview, glmodelview);
    copyPMatrixToGLArray(modelviewInv, glmodelviewInv);
    gl2f.glLoadMatrixf(glmodelview, 0);
    
    modelviewStack.set(modelview);
    modelviewUpdated = true;
  }

  
  // This method is needed to copy a PMatrix3D into a  opengl array.
  // The PMatrix3D.get(float[]) is not useful, because PMatrix3D assumes
  // row-major ordering of the elements of the float array, and opengl
  // uses column-major ordering.
  protected void copyPMatrixToGLArray(PMatrix3D src, float[] dest) {
    dest[ 0] = src.m00; dest[ 4] = src.m01; dest[ 8] = src.m02; dest[12] = src.m03;
    dest[ 1] = src.m10; dest[ 5] = src.m11; dest[ 9] = src.m12; dest[13] = src.m13;
    dest[ 2] = src.m20; dest[ 6] = src.m21; dest[10] = src.m22; dest[14] = src.m23;
    dest[ 3] = src.m30; dest[ 7] = src.m31; dest[11] = src.m32; dest[15] = src.m33;
  }

  
  // This method is needed to copy an opengl array into a  PMatrix3D.
  // The PMatrix3D.set(float[]) is not useful, because PMatrix3D assumes
  // row-major ordering of the elements of the float array, and opengl
  // uses column-major ordering.  
  protected void copyGLArrayToPMatrix(float[] src, PMatrix3D dest) {
    dest.m00 = src[ 0]; dest.m01 = src[ 4]; dest.m02 = src[ 8]; dest.m03 = src[12]; 
    dest.m10 = src[ 1]; dest.m11 = src[ 5]; dest.m12 = src[ 9]; dest.m13 = src[13];
    dest.m20 = src[ 2]; dest.m21 = src[ 6]; dest.m22 = src[10]; dest.m23 = src[14];
    dest.m30 = src[ 3]; dest.m31 = src[ 7]; dest.m32 = src[11]; dest.m33 = src[15];
  }  
  
  
  //////////////////////////////////////////////////////////////

  // MATRIX GET/SET/PRINT

  public PMatrix getMatrix() {
    if (!modelviewUpdated) {
      getModelviewMatrix();    
    }
    return modelview.get();      
  }

  // public PMatrix2D getMatrix(PMatrix2D target)

  public PMatrix3D getMatrix(PMatrix3D target) {
    if (target == null) {
      target = new PMatrix3D();
    }
    if (!modelviewUpdated) {
      getModelviewMatrix();    
    }
    target.set(modelview);
    return target;
  }

  // public void setMatrix(PMatrix source)

  public void setMatrix(PMatrix2D source) {
    // not efficient, but at least handles the inverse stuff.
    resetMatrix();
    applyMatrix(source);
  }

  /**
   * Set the current transformation to the contents of the specified source.
   */
  public void setMatrix(PMatrix3D source) {
    // not efficient, but at least handles the inverse stuff.
    resetMatrix();
    applyMatrix(source);
  }

  /**
   * Print the current model (or "transformation") matrix.
   */
  public void printMatrix() {
    if (!modelviewUpdated) {
      getModelviewMatrix();    
    }      
    modelview.print();      
  }

  /*
   * This function checks if the modelview matrix is set up to likely be drawing
   * in 2D. It merely checks if the non-translational piece of the matrix is
   * unity. If this is to be used, it should be coupled with a check that the
   * raw vertex coordinates lie in the z=0 plane. Mainly useful for applying
   * sub-pixel shifts to avoid 2d artifacts in the screen plane. Added by
   * ewjordan 6/13/07
   * 
   * TODO need to invert the logic here so that we can simply return the value,
   * rather than calculating true/false and returning it.
   */
   protected boolean drawing2D() { 
     if (modelview.m00 != 1.0f || modelview.m11 != 1.0f || modelview.m22 != 1.0f || // check scale 
         modelview.m01 != 0.0f || modelview.m02 != 0.0f || // check rotational pieces 
         modelview.m10 != 0.0f || modelview.m12 != 0.0f || 
         modelview.m20 != 0.0f || modelview.m21 != 0.0f || !((camera.m23 - modelview.m23) <= EPSILON && 
         (camera.m23-modelview.m23) >= -EPSILON)) { // check for z-translation 
       // Something about the modelview matrix indicates 3d drawing 
       // (or rotated 2d, in which case 2d subpixel fixes probably aren't needed) 
       return false; 
     } else { 
       //The matrix is mapping z=0 vertices to the screen plane, 
       // which means it's likely that 2D drawing is happening. 
       return true; 
     } 
   }
  
  //////////////////////////////////////////////////////////////

  // PROJECTION
  
  public void pushProjection() {
    gl2f.glMatrixMode(GL2.GL_PROJECTION);
    gl2f.glPushMatrix();
    projectionStack.push();
    gl2f.glMatrixMode(GL2.GL_MODELVIEW);
  }
  
  public void multProjection(PMatrix3D mat) {
    projection.apply(mat);
  }

  public void setProjection(PMatrix3D mat) {
    projection.set(mat);
  }
    
  public void popProjection() {
    gl2f.glMatrixMode(GL2.GL_PROJECTION);
    gl2f.glPopMatrix();
    projectionStack.pop();
    gl2f.glMatrixMode(GL2.GL_MODELVIEW);    
  }

  protected void getProjectionMatrix() {
    projectionStack.get(projection);
    projectionUpdated = true;
  }  
  
  protected void updateProjection() {
    copyPMatrixToGLArray(projection, glprojection);
    gl2f.glMatrixMode(GL2.GL_PROJECTION);
    gl2f.glLoadMatrixf(glprojection, 0);
    gl2f.glMatrixMode(GL2.GL_MODELVIEW);
    projectionStack.set(glprojection);
    projectionUpdated = true;
  }
  
  protected void restoreProjection() {
    PApplet.arrayCopy(pprojection, glprojection);
    copyGLArrayToPMatrix(pprojection, projection);
    
    gl2f.glMatrixMode(GL2.GL_PROJECTION);
    gl2f.glLoadMatrixf(glprojection, 0);
    gl2f.glMatrixMode(GL2.GL_MODELVIEW);
    projectionStack.set(glprojection);
    projectionUpdated = true;    
  }
  
  //////////////////////////////////////////////////////////////

  // CAMERA

  /**
   * Set matrix mode to the camera matrix (instead of the current transformation
   * matrix). This means applyMatrix, resetMatrix, etc. will affect the camera.
   * <P>
   * Note that the camera matrix is *not* the perspective matrix, it contains
   * the values of the modelview matrix immediatly after the latter was
   * initialized with ortho() or camera(), or the modelview matrix as result of
   * the operations applied between beginCamera()/endCamera().
   * <P>
   * beginCamera() specifies that all coordinate transforms until endCamera()
   * should be pre-applied in inverse to the camera transform matrix. Note that
   * this is only challenging when a user specifies an arbitrary matrix with
   * applyMatrix(). Then that matrix will need to be inverted, which may not be
   * possible. But take heart, if a user is applying a non-invertible matrix to
   * the camera transform, then he is clearly up to no good, and we can wash our
   * hands of those bad intentions.
   * <P>
   * begin/endCamera clauses do not automatically reset the camera transform
   * matrix. That's because we set up a nice default camera transform int
   * setup(), and we expect it to hold through draw(). So we don't reset the
   * camera transform matrix at the top of draw(). That means that an
   * innocuous-looking clause like
   * 
   * <PRE>
   * beginCamera();
   * translate(0, 0, 10);
   * endCamera();
   * </PRE>
   * 
   * at the top of draw(), will result in a runaway camera that shoots
   * infinitely out of the screen over time. In order to prevent this, it is
   * necessary to call some function that does a hard reset of the camera
   * transform matrix inside of begin/endCamera. Two options are
   * 
   * <PRE>
   * camera(); // sets up the nice default camera transform
   * resetMatrix(); // sets up the identity camera transform
   * </PRE>
   * 
   * So to rotate a camera a constant amount, you might try
   * 
   * <PRE>
   * beginCamera();
   * camera();
   * rotateY(PI / 8);
   * endCamera();
   * </PRE>
   */
  public void beginCamera() {
    if (manipulatingCamera) {
      throw new RuntimeException("beginCamera() cannot be called again "
          + "before endCamera()");
    } else {
      manipulatingCamera = true;
      //scalingDuringCamManip = false;
      modelviewStack.resetTransform();
    }    
  }

  public void updateCamera() {
    if (!manipulatingCamera) {
      throw new RuntimeException("Cannot call updateCamera() "
          + "without first calling beginCamera()");
    }    
    modelviewStack.setCamera(camera);    
    
//    scalingDuringCamManip = true; // Assuming general transformation.
    modelviewUpdated = false;
    
    flush(); // TODO: need to revise the projection/modelview matrix stack.
    copyPMatrixToGLArray(camera, glmodelview);
    gl2f.glLoadMatrixf(glmodelview, 0);    
  }  
  
  /**
   * Record the current settings into the camera matrix, and set the matrix mode
   * back to the current transformation matrix.
   * <P>
   * Note that this will destroy any settings to scale(), translate(), or
   * whatever, because the final camera matrix will be copied (not multiplied)
   * into the modelview.
   */
  public void endCamera() {
    if (!manipulatingCamera) {
      throw new RuntimeException("Cannot call endCamera() "
          + "without first calling beginCamera()");
    }
    
    getModelviewMatrix();

    modelviewInv.set(modelview);
    modelviewInv.invert();    

    camera.set(modelview);
    
    // Copying modelview matrix after camera transformations to the camera
    // matrices.
    // DO WE NEED ALL THESE GUYS?
    PApplet.arrayCopy(glmodelview, pcamera);

    
    PApplet.arrayCopy(glmodelviewInv, pcameraInv);
    copyGLArrayToPMatrix(pcamera, camera);
    copyGLArrayToPMatrix(pcameraInv, cameraInv);

    // all done
    manipulatingCamera = false;
    //scalingDuringCamManip = false;
  }

  protected void getModelviewMatrix() {
    modelviewStack.get(modelview);
    modelviewUpdated = true;
  }


  /**
   * Set camera to the default settings.
   * <P>
   * Processing camera behavior:
   * <P>
   * Camera behavior can be split into two separate components, camera
   * transformation, and projection. The transformation corresponds to the
   * physical location, orientation, and scale of the camera. In a physical
   * camera metaphor, this is what can manipulated by handling the camera body
   * (with the exception of scale, which doesn't really have a physcial analog).
   * The projection corresponds to what can be changed by manipulating the lens.
   * <P>
   * We maintain separate matrices to represent the camera transform and
   * projection. An important distinction between the two is that the camera
   * transform should be invertible, where the projection matrix should not,
   * since it serves to map three dimensions to two. It is possible to bake the
   * two matrices into a single one just by multiplying them together, but it
   * isn't a good idea, since lighting, z-ordering, and z-buffering all demand a
   * true camera z coordinate after modelview and camera transforms have been
   * applied but before projection. If the camera transform and projection are
   * combined there is no way to recover a good camera-space z-coordinate from a
   * model coordinate.
   * <P>
   * Fortunately, there are no functions that manipulate both camera
   * transformation and projection.
   * <P>
   * camera() sets the camera position, orientation, and center of the scene. It
   * replaces the camera transform with a new one.
   * <P>
   * The transformation functions are the same ones used to manipulate the
   * modelview matrix (scale, translate, rotate, etc.). But they are bracketed
   * with beginCamera(), endCamera() to indicate that they should apply (in
   * inverse), to the camera transformation matrix.
   */
  public void camera() {
    camera(cameraX, cameraY, cameraZ, cameraX, cameraY, 0, 0, 1, 0);
  }

  /**
   * More flexible method for dealing with camera().
   * <P>
   * The actual call is like gluLookat. Here's the real skinny on what does
   * what:
   * 
   * <PRE>
   * camera(); or
   * camera(ex, ey, ez, cx, cy, cz, ux, uy, uz);
   * </PRE>
   * 
   * do not need to be called from with beginCamera();/endCamera(); That's
   * because they always apply to the camera transformation, and they always
   * totally replace it. That means that any coordinate transforms done before
   * camera(); in draw() will be wiped out. It also means that camera() always
   * operates in untransformed world coordinates. Therefore it is always
   * redundant to call resetMatrix(); before camera(); This isn't technically
   * true of gluLookat, but it's pretty much how it's used.
   * <P>
   * Now, beginCamera(); and endCamera(); are useful if you want to move the
   * camera around using transforms like translate(), etc. They will wipe out
   * any coordinate system transforms that occur before them in draw(), but they
   * will not automatically wipe out the camera transform. This means that they
   * should be at the top of draw(). It also means that the following:
   * 
   * <PRE>
   * beginCamera();
   * rotateY(PI / 8);
   * endCamera();
   * </PRE>
   * 
   * will result in a camera that spins without stopping. If you want to just
   * rotate a small constant amount, try this:
   * 
   * <PRE>
   * beginCamera();
   * camera(); // sets up the default view
   * rotateY(PI / 8);
   * endCamera();
   * </PRE>
   * 
   * That will rotate a little off of the default view. Note that this is
   * entirely equivalent to
   * 
   * <PRE>
   * camera(); // sets up the default view
   * beginCamera();
   * rotateY(PI / 8);
   * endCamera();
   * </PRE>
   * 
   * because camera() doesn't care whether or not it's inside a begin/end
   * clause. Basically it's safe to use camera() or camera(ex, ey, ez, cx, cy,
   * cz, ux, uy, uz) as naked calls because they do all the matrix resetting
   * automatically.
   */
  public void camera(float eyeX, float eyeY, float eyeZ, 
                     float centerX, float centerY, float centerZ, 
                     float upX, float upY, float upZ) {
    // Flushing geometry with a different camera configuration.
    flush();
    
    // Calculating Z vector
    float z0 = eyeX - centerX;
    float z1 = eyeY - centerY;
    float z2 = eyeZ - centerZ;
    float mag = PApplet.sqrt(z0 * z0 + z1 * z1 + z2 * z2);
    if (mag != 0) {
      z0 /= mag;
      z1 /= mag;
      z2 /= mag;
    }
    cameraEyeX = eyeX;
    cameraEyeY = eyeY;
    cameraEyeZ = eyeZ;
    cameraDepth = mag;
    
    // Calculating Y vector
    float y0 = upX;
    float y1 = upY;
    float y2 = upZ;

    // Computing X vector as Y cross Z
    float x0 = y1 * z2 - y2 * z1;
    float x1 = -y0 * z2 + y2 * z0;
    float x2 = y0 * z1 - y1 * z0;

    // Recompute Y = Z cross X
    y0 = z1 * x2 - z2 * x1;
    y1 = -z0 * x2 + z2 * x0;
    y2 = z0 * x1 - z1 * x0;

    // Cross product gives area of parallelogram, which is < 1.0 for
    // non-perpendicular unit-length vectors; so normalize x, y here:
    mag = PApplet.sqrt(x0 * x0 + x1 * x1 + x2 * x2);
    if (mag != 0) {
      x0 /= mag;
      x1 /= mag;
      x2 /= mag;
    }

    mag = PApplet.sqrt(y0 * y0 + y1 * y1 + y2 * y2);
    if (mag != 0) {
      y0 /= mag;
      y1 /= mag;
      y2 /= mag;
    }

    float[] m = glmodelview;
    m[0] = x0;
    m[1] = y0;
    m[2] = z0;
    m[3] = 0.0f;

    m[4] = x1;
    m[5] = y1;
    m[6] = z1;
    m[7] = 0.0f;

    m[8] = x2;
    m[9] = y2;
    m[10] = z2;
    m[11] = 0;
    
    m[12] = 0.0f;
    m[13] = 0.0f;
    m[14] = 0.0f;
    m[15] = 1.0f;

    // Translating to the eye position, followed by a translation of height units along the Y axis.
    // The last one is needed to properly invert coordinate axis of OpenGL so it matches Processing's.
    float tx = -eyeX;
    float ty = -eyeY;
    float tz = -eyeZ;
    m[12] += tx * m[0] + ty * m[4] + tz * m[8];
    m[13] += tx * m[1] + ty * m[5] + tz * m[9];
    m[14] += tx * m[2] + ty * m[6] + tz * m[10];
    m[15] += tx * m[3] + ty * m[7] + tz * m[11];        
  
    gl2f.glMatrixMode(GL2.GL_MODELVIEW);
    gl2f.glLoadMatrixf(glmodelview, 0);
    
    copyGLArrayToPMatrix(glmodelview, modelview);    
    modelviewStack.setCamera(modelview);    
    modelviewUpdated = true;

    modelviewInv.set(modelview);
    modelviewInv.invert();    
    
    PApplet.arrayCopy(glmodelview, pcamera);
    PApplet.arrayCopy(glmodelviewInv, pcameraInv);
    copyGLArrayToPMatrix(pcamera, camera);
    copyGLArrayToPMatrix(pcameraInv, cameraInv);        
  }

  
  /**
   * Print the current camera matrix.
   */
  public void printCamera() {
    PMatrix3D temp = new PMatrix3D();
    copyGLArrayToPMatrix(pcamera, temp);
    temp.print();
  }

  
  protected void restoreCamera() {
    PApplet.arrayCopy(pcamera, glmodelview);
    PApplet.arrayCopy(pcameraInv, glmodelviewInv);
    copyGLArrayToPMatrix(pcamera, camera);
    copyGLArrayToPMatrix(pcameraInv, cameraInv);
    
    gl2f.glMatrixMode(GL2.GL_MODELVIEW);
    gl2f.glLoadMatrixf(glmodelview, 0);
    
    copyGLArrayToPMatrix(glmodelview, modelview);
    modelviewStack.setCamera(modelview);
    modelviewUpdated = true;    
  }
  
  
  //////////////////////////////////////////////////////////////

  // PROJECTION
  
  /**
   * Calls ortho() with the proper parameters for Processing's standard
   * orthographic projection.
   */
  public void ortho() {
    ortho(0, width, 0, height, -500, 500);
  }

  /**
   * Calls ortho() with the specified size of the viewing volume along
   * the X and Z directions.
   */  
  public void ortho(float left, float right, 
                    float bottom, float top) {
    ortho(left, right, bottom, top, -500, 500);
  }  
  
  /**
   * Sets orthographic projection. The left, right, bottom and top
   * values refer to the top left corner of the screen, not to the
   * center or eye of the camera. This is like this because making
   * it relative to the camera is not very intuitive if we think
   * of the perspective function, which is also independent of the
   * camera position.
   * 
   */
  public void ortho(float left, float right, 
                    float bottom, float top,
                    float near, float far) {
    // Flushing geometry with a different perspective configuration.
    flush();
    
    left -= width/2;
    right -= width/2;
    
    bottom -= height/2;
    top -= height/2;
    
    near += cameraDepth;
    far += cameraDepth;
    
    float x = 2.0f / (right - left);
    float y = 2.0f / (top - bottom);
    float z = -2.0f / (far - near);

    float tx = -(right + left) / (right - left);
    float ty = -(top + bottom) / (top - bottom);
    float tz = -(far + near) / (far - near);

    glprojection[0] = x;
    glprojection[1] = 0.0f;
    glprojection[2] = 0.0f;
    glprojection[3] = 0.0f;

    glprojection[4] = 0.0f;
    glprojection[5] = -y; // The minus here inverts the Y axis in order to use Processing's convention
    glprojection[6] = 0.0f;
    glprojection[7] = 0.0f;

    glprojection[8] = 0;
    glprojection[9] = 0;
    glprojection[10] = z;
    glprojection[11] = 0.0f;

    glprojection[12] = tx;
    glprojection[13] = ty;
    glprojection[14] = tz;
    glprojection[15] = 1.0f;

    gl2f.glMatrixMode(GL2.GL_PROJECTION);
    gl2f.glLoadMatrixf(glprojection, 0);
    copyGLArrayToPMatrix(glprojection, projection);
    projectionUpdated = true;
    
    // The matrix mode is always MODELVIEW, because the user will be doing
    // geometrical transformations all the time, projection transformations 
    // only a few times.
    gl2f.glMatrixMode(GL2.GL_MODELVIEW);
  }

  /**
   * Calls perspective() with Processing's standard coordinate projection.
   * <P>
   * Projection functions:
   * <UL>
   * <LI>frustrum()
   * <LI>ortho()
   * <LI>perspective()
   * </UL>
   * Each of these three functions completely replaces the projection matrix
   * with a new one. They can be called inside setup(), and their effects will
   * be felt inside draw(). At the top of draw(), the projection matrix is not
   * reset. Therefore the last projection function to be called always
   * dominates. On resize, the default projection is always established, which
   * has perspective.
   * <P>
   * This behavior is pretty much familiar from OpenGL, except where functions
   * replace matrices, rather than multiplying against the previous.
   * <P>
   */
  public void perspective() {
    perspective(cameraFOV, cameraAspect, cameraNear, cameraFar);
  }

  /**
   * Similar to gluPerspective(). Implementation based on Mesa's glu.c
   */
  public void perspective(float fov, float aspect, float zNear, float zFar) {
    float ymax = zNear * (float) Math.tan(fov / 2);
    float ymin = -ymax;
    float xmin = ymin * aspect;
    float xmax = ymax * aspect;
    frustum(xmin, xmax, ymin, ymax, zNear, zFar);
  }

  /**
   * Same as glFrustum(), except that it wipes out (rather than multiplies
   * against) the current perspective matrix.
   * <P>
   * Implementation based on the explanation in the OpenGL blue book.
   */
  public void frustum(float left, float right, float bottom, float top,
      float znear, float zfar) {
    // Flushing geometry with a different perspective configuration.
    flush();
    
    float temp, temp2, temp3, temp4;
    temp = 2.0f * znear;
    temp2 = right - left;
    temp3 = top - bottom;
    temp4 = zfar - znear;
    
    glprojection[0] = temp / temp2;
    glprojection[1] = 0.0f;
    glprojection[2] = 0.0f;
    glprojection[3] = 0.0f;
    glprojection[4] = 0.0f;
    
    // The minus here inverts the Y axis in order to use Processing's convention:
    glprojection[5] = -temp / temp3; 
    
    glprojection[6] = 0.0f;
    glprojection[7] = 0.0f;
    glprojection[8] = (right + left) / temp2;
    glprojection[9] = (top + bottom) / temp3;
    glprojection[10] = (-zfar - znear) / temp4;
    glprojection[11] = -1.0f;
    glprojection[12] = 0.0f;
    glprojection[13] = 0.0f;
    glprojection[14] = (-temp * zfar) / temp4;
    glprojection[15] = 0.0f;
    
    gl2f.glMatrixMode(GL2.GL_PROJECTION);
    gl2f.glLoadMatrixf(glprojection, 0);
    copyGLArrayToPMatrix(glprojection, projection);
    projectionUpdated = true;
    
    projectionStack.set(projection);
    
    PApplet.arrayCopy(glprojection, pprojection);
    
    // The matrix mode is always MODELVIEW, because the user will be doing
    // geometrical transformations all the time, projection transformations 
    // only a few times.
    gl2f.glMatrixMode(GL2.GL_MODELVIEW);
  }

  /**
   * Print the current projection matrix.
   */
  public void printProjection() {
    if (!projectionUpdated) {
      getProjectionMatrix();      
    }    
    projection.print();
  }
  
  //////////////////////////////////////////////////////////////

  // SCREEN AND MODEL COORDS

  public float screenX(float x, float y) {
    return screenX(x, y, 0);
  }

  public float screenY(float x, float y) {
    return screenY(x, y, 0);
  }

  public float screenX(float x, float y, float z) {
    if (!modelviewUpdated) {
      getModelviewMatrix();      
    }

    if (!projectionUpdated) {
      getProjectionMatrix();      
    }
        
    float ax = glmodelview[0] * x + glmodelview[4] * y + glmodelview[8] * z + glmodelview[12];
    float ay = glmodelview[1] * x + glmodelview[5] * y + glmodelview[9] * z  + glmodelview[13];
    float az = glmodelview[2] * x + glmodelview[6] * y + glmodelview[10] * z + glmodelview[14];
    float aw = glmodelview[3] * x + glmodelview[7] * y + glmodelview[11] * z + glmodelview[15];

    float ox = glprojection[0] * ax + glprojection[4] * ay + glprojection[8] * az + glprojection[12] * aw;
    float ow = glprojection[3] * ax + glprojection[7] * ay + glprojection[11] * az + glprojection[15] * aw;

    if (ow != 0) {
      ox /= ow;
    }
    return width * (1 + ox) / 2.0f;
  }

  public float screenY(float x, float y, float z) {
    if (!modelviewUpdated) {
      getModelviewMatrix();
    }

    if (!projectionUpdated) {
      getProjectionMatrix();      
    }
        
    float ax = glmodelview[0] * x + glmodelview[4] * y + glmodelview[8] * z + glmodelview[12];
    float ay = glmodelview[1] * x + glmodelview[5] * y + glmodelview[9] * z  + glmodelview[13];
    float az = glmodelview[2] * x + glmodelview[6] * y + glmodelview[10] * z + glmodelview[14];
    float aw = glmodelview[3] * x + glmodelview[7] * y + glmodelview[11] * z + glmodelview[15];

    float oy = glprojection[1] * ax + glprojection[5] * ay + glprojection[9] * az + glprojection[13] * aw;
    float ow = glprojection[3] * ax + glprojection[7] * ay + glprojection[11] * az + glprojection[15] * aw;

    if (ow != 0) {
      oy /= ow;
    }
    return height * (1 + oy) / 2.0f;
  }

  public float screenZ(float x, float y, float z) {
    if (!modelviewUpdated) {
      getModelviewMatrix();      
    }

    if (!projectionUpdated) {
      getProjectionMatrix();      
    }
    
    float ax = glmodelview[0] * x + glmodelview[4] * y + glmodelview[8] * z + glmodelview[12];
    float ay = glmodelview[1] * x + glmodelview[5] * y + glmodelview[9] * z  + glmodelview[13];
    float az = glmodelview[2] * x + glmodelview[6] * y + glmodelview[10] * z + glmodelview[14];
    float aw = glmodelview[3] * x + glmodelview[7] * y + glmodelview[11] * z + glmodelview[15];

    float oz = glprojection[2] * ax + glprojection[6] * ay + glprojection[10] * az + glprojection[14] * aw;
    float ow = glprojection[3] * ax + glprojection[7] * ay + glprojection[11] * az + glprojection[15] * aw;

    if (ow != 0) {
      oz /= ow;
    }
    return (oz + 1) / 2.0f;
  }

  public float modelX(float x, float y, float z) {
    if (!modelviewUpdated) {
      getModelviewMatrix();
    }

    float ax = glmodelview[0] * x + glmodelview[4] * y + glmodelview[8] * z + glmodelview[12];
    float ay = glmodelview[1] * x + glmodelview[5] * y + glmodelview[9] * z  + glmodelview[13];
    float az = glmodelview[2] * x + glmodelview[6] * y + glmodelview[10] * z + glmodelview[14];
    float aw = glmodelview[3] * x + glmodelview[7] * y + glmodelview[11] * z + glmodelview[15];

    float ox = pcameraInv[0] * ax + pcameraInv[4] * ay + pcameraInv[8] * az + pcameraInv[12] * aw;
    float ow = pcameraInv[3] * ax + pcameraInv[7] * ay + pcameraInv[11] * az + pcameraInv[15] * aw;

    return (ow != 0) ? ox / ow : ox;
  }

  public float modelY(float x, float y, float z) {
    if (!modelviewUpdated) {
      getModelviewMatrix();
    }

    float ax = glmodelview[0] * x + glmodelview[4] * y + glmodelview[8] * z + glmodelview[12];
    float ay = glmodelview[1] * x + glmodelview[5] * y + glmodelview[9] * z  + glmodelview[13];
    float az = glmodelview[2] * x + glmodelview[6] * y + glmodelview[10] * z + glmodelview[14];
    float aw = glmodelview[3] * x + glmodelview[7] * y + glmodelview[11] * z + glmodelview[15];

    float oy = pcameraInv[1] * ax + pcameraInv[5] * ay + pcameraInv[9] * az + pcameraInv[13] * aw;
    float ow = pcameraInv[3] * ax + pcameraInv[7] * ay + pcameraInv[11] * az + pcameraInv[15] * aw;

    return (ow != 0) ? oy / ow : oy;
  }

  public float modelZ(float x, float y, float z) {
    if (!modelviewUpdated) {
      getModelviewMatrix();
    }

    float ax = glmodelview[0] * x + glmodelview[4] * y + glmodelview[8] * z + glmodelview[12];
    float ay = glmodelview[1] * x + glmodelview[5] * y + glmodelview[9] * z  + glmodelview[13];
    float az = glmodelview[2] * x + glmodelview[6] * y + glmodelview[10] * z + glmodelview[14];
    float aw = glmodelview[3] * x + glmodelview[7] * y + glmodelview[11] * z + glmodelview[15];

    float oz = pcameraInv[2] * ax + pcameraInv[6] * ay + pcameraInv[10] * az + pcameraInv[14] * aw;
    float ow = pcameraInv[3] * ax + pcameraInv[7] * ay + pcameraInv[11] * az + pcameraInv[15] * aw;

    return (ow != 0) ? oz / ow : oz;
  }

  // STYLES

  // public void pushStyle()
  // public void popStyle()
  // public void style(PStyle)
  // public PStyle getStyle()
  // public void getStyle(PStyle)
  
  //////////////////////////////////////////////////////////////

  // COLOR MODE

  // public void colorMode(int mode)
  // public void colorMode(int mode, float max)
  // public void colorMode(int mode, float mx, float my, float mz);
  // public void colorMode(int mode, float mx, float my, float mz, float ma);

  //////////////////////////////////////////////////////////////

  // COLOR CALC

  // protected void colorCalc(int rgb)
  // protected void colorCalc(int rgb, float alpha)
  // protected void colorCalc(float gray)
  // protected void colorCalc(float gray, float alpha)
  // protected void colorCalc(float x, float y, float z)
  // protected void colorCalc(float x, float y, float z, float a)
  // protected void colorCalcARGB(int argb, float alpha)

  //////////////////////////////////////////////////////////////

  // STROKE CAP/JOIN/WEIGHT

  public void strokeWeight(float weight) {
    this.strokeWeight = weight;
  }

  public void strokeJoin(int join) {
    this.strokeJoin = join;
  }

  public void strokeCap(int cap) {
    this.strokeCap = cap;
  }

  //////////////////////////////////////////////////////////////

  // STROKE, TINT, FILL

  // public void noStroke()
  // public void stroke(int rgb)
  // public void stroke(int rgb, float alpha)
  // public void stroke(float gray)
  // public void stroke(float gray, float alpha)
  // public void stroke(float x, float y, float z)
  // public void stroke(float x, float y, float z, float a)
  // protected void strokeFromCalc()

  // public void noTint()
  // public void tint(int rgb)
  // public void tint(int rgb, float alpha)
  // public void tint(float gray)
  // public void tint(float gray, float alpha)
  // public void tint(float x, float y, float z)
  // public void tint(float x, float y, float z, float a)
  // protected void tintFromCalc()

  // public void noFill()
  // public void fill(int rgb)
  // public void fill(int rgb, float alpha)
  // public void fill(float gray)
  // public void fill(float gray, float alpha)
  // public void fill(float x, float y, float z)
  // public void fill(float x, float y, float z, float a)

  protected void fillFromCalc() {
    super.fillFromCalc();
    calcColorBuffer();
    
    // P3D uses GL_COLOR_MATERIAL mode, so the ambient and diffuse components
    // for all vertices are taken from the glColor/color buffer settings and we don't
    // need this:
    //gl.glMaterialfv(GL10.GL_FRONT_AND_BACK, GL10.GL_AMBIENT_AND_DIFFUSE, colorFloats, 0);
  }

  protected void setFillColor() {
    gl2f.glColor4f(fillR, fillG, fillB, fillA);
  } 
  
  protected void setTintColor() {
    gl2f.glColor4f(tintR, tintG, tintB, tintA);
  }
  
  //////////////////////////////////////////////////////////////

  // MATERIAL PROPERTIES

  // public void ambient(int rgb) {
  // super.ambient(rgb);
  // calcColorBuffer();
  // gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_AMBIENT, colorBuffer, 0);
  // }

  // public void ambient(float gray) {
  // super.ambient(gray);
  // calcColorBuffer();
  // gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_AMBIENT, colorBuffer, 0);
  // }

  // public void ambient(float x, float y, float z) {
  // super.ambient(x, y, z);
  // calcColorBuffer();
  // gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_AMBIENT, colorBuffer, 0);
  // }

  protected void ambientFromCalc() {
    super.ambientFromCalc();
    calcColorBuffer();
    
    // OPENGL uses GL_COLOR_MATERIAL mode, so the ambient and diffuse components
    // for all vertices are taken from the glColor/color buffer settings.    
    gl2f.glMaterialfv(GL.GL_FRONT_AND_BACK, GL2.GL_AMBIENT, colorFloats, 0);
  }

  // public void specular(int rgb) {
  // super.specular(rgb);
  // calcColorBuffer();
  // gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_SPECULAR, colorBuffer, 0);
  // }

  // public void specular(float gray) {
  // super.specular(gray);
  // calcColorBuffer();
  // gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_SPECULAR, colorBuffer, 0);
  // }

  // public void specular(float x, float y, float z) {
  // super.specular(x, y, z);
  // calcColorBuffer();
  // gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_SPECULAR, colorBuffer, 0);
  // }

  protected void specularFromCalc() {
    super.specularFromCalc();
    calcColorBuffer();
    gl2f.glMaterialfv(GL.GL_FRONT_AND_BACK, GL2.GL_SPECULAR, colorFloats, 0);
  }

  public void shininess(float shine) {
    super.shininess(shine);
    gl2f.glMaterialf(GL.GL_FRONT_AND_BACK, GL2.GL_SHININESS, shine);
  }

  // public void emissive(int rgb) {
  // super.emissive(rgb);
  // calcColorBuffer();
  // gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_EMISSION, colorBuffer, 0);
  // }

  // public void emissive(float gray) {
  // super.emissive(gray);
  // calcColorBuffer();
  // gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_EMISSION, colorBuffer, 0);
  // }

  // public void emissive(float x, float y, float z) {
  // super.emissive(x, y, z);
  // calcColorBuffer();
  // gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_EMISSION, colorBuffer, 0);
  // }

  protected void emissiveFromCalc() {
    super.emissiveFromCalc();
    calcColorBuffer();
    gl2f.glMaterialfv(GL2.GL_FRONT_AND_BACK, GL2.GL_EMISSION, colorFloats, 0);
  } 
  
  //////////////////////////////////////////////////////////////

  // LIGHTING

  /**
   * Sets up an ambient and directional light using OpenGL. API taken from
   * PGraphics3D.
   * 
   * <PRE>
   * The Lighting Skinny:
   * The way lighting works is complicated enough that it's worth
   * producing a document to describe it. Lighting calculations proceed
   * pretty much exactly as described in the OpenGL red book.
   * Light-affecting material properties:
   *   AMBIENT COLOR
   *   - multiplies by light's ambient component
   *   - for believability this should match diffuse color
   *   DIFFUSE COLOR
   *   - multiplies by light's diffuse component
   *   SPECULAR COLOR
   *   - multiplies by light's specular component
   *   - usually less colored than diffuse/ambient
   *   SHININESS
   *   - the concentration of specular effect
   *   - this should be set pretty high (20-50) to see really
   *     noticeable specularity
   *   EMISSIVE COLOR
   *   - constant additive color effect
   * Light types:
   *   AMBIENT
   *   - one color
   *   - no specular color
   *   - no direction
   *   - may have falloff (constant, linear, and quadratic)
   *   - may have position (which matters in non-constant falloff case)
   *   - multiplies by a material's ambient reflection
   *   DIRECTIONAL
   *   - has diffuse color
   *   - has specular color
   *   - has direction
   *   - no position
   *   - no falloff
   *   - multiplies by a material's diffuse and specular reflections
   *   POINT
   *   - has diffuse color
   *   - has specular color
   *   - has position
   *   - no direction
   *   - may have falloff (constant, linear, and quadratic)
   *   - multiplies by a material's diffuse and specular reflections
   *   SPOT
   *   - has diffuse color
   *   - has specular color
   *   - has position
   *   - has direction
   *   - has cone angle (set to half the total cone angle)
   *   - has concentration value
   *   - may have falloff (constant, linear, and quadratic)
   *   - multiplies by a material's diffuse and specular reflections
   * Normal modes:
   * All of the primitives (rect, box, sphere, etc.) have their normals
   * set nicely. During beginShape/endShape normals can be set by the user.
   *   AUTO-NORMAL
   *   - if no normal is set during the shape, we are in auto-normal mode
   *   - auto-normal calculates one normal per triangle (face-normal mode)
   *   SHAPE-NORMAL
   *   - if one normal is set during the shape, it will be used for
   *     all vertices
   *   VERTEX-NORMAL
   *   - if multiple normals are set, each normal applies to
   *     subsequent vertices
   *   - (except for the first one, which applies to previous
   *     and subsequent vertices)
   * Efficiency consequences:
   *   There is a major efficiency consequence of position-dependent
   *   lighting calculations per vertex. (See below for determining
   *   whether lighting is vertex position-dependent.) If there is no
   *   position dependency then the only factors that affect the lighting
   *   contribution per vertex are its colors and its normal.
   *   There is a major efficiency win if
   *   1) lighting is not position dependent
   *   2) we are in AUTO-NORMAL or SHAPE-NORMAL mode
   *   because then we can calculate one lighting contribution per shape
   *   (SHAPE-NORMAL) or per triangle (AUTO-NORMAL) and simply multiply it
   *   into the vertex colors. The converse is our worst-case performance when
   *   1) lighting is position dependent
   *   2) we are in AUTO-NORMAL mode
   *   because then we must calculate lighting per-face * per-vertex.
   *   Each vertex has a different lighting contribution per face in
   *   which it appears. Yuck.
   * Determining vertex position dependency:
   *   If any of the following factors are TRUE then lighting is
   *   vertex position dependent:
   *   1) Any lights uses non-constant falloff
   *   2) There are any point or spot lights
   *   3) There is a light with specular color AND there is a
   *      material with specular color
   * So worth noting is that default lighting (a no-falloff ambient
   * and a directional without specularity) is not position-dependent.
   * We should capitalize.
   * Simon Greenwold, April 2005
   * </PRE>
   */
  public void lights() {
    enableLighting();

    // need to make sure colorMode is RGB 255 here
    int colorModeSaved = colorMode;
    colorMode = RGB;

    lightFalloff(1, 0, 0);
    lightSpecular(0, 0, 0);

    ambientLight(colorModeX * 0.5f, colorModeY * 0.5f, colorModeZ * 0.5f);
    directionalLight(colorModeX * 0.5f, colorModeY * 0.5f, colorModeZ * 0.5f, 0, 0, -1);
    
    colorMode = colorModeSaved;
  }

  /**
   * Disables lighting.
   */
  public void noLights() {
    disableLighting();
    lightCount = 0;
  }

  /**
   * Add an ambient light based on the current color mode.
   */
  public void ambientLight(float r, float g, float b) {
    ambientLight(r, g, b, 0, 0, 0);
  }

  /**
   * Add an ambient light based on the current color mode. This version includes
   * an (x, y, z) position for situations where the falloff distance is used.
   */
  public void ambientLight(float r, float g, float b, float x, float y, float z) {
    enableLighting();
    if (lightCount == MAX_LIGHTS) {
      throw new RuntimeException("can only create " + MAX_LIGHTS + " lights");
    }
    colorCalc(r, g, b);
    lightDiffuse[lightCount][0] = calcR;
    lightDiffuse[lightCount][1] = calcG;
    lightDiffuse[lightCount][2] = calcB;
    lightDiffuse[lightCount][3] = 1.0f;

    lightType[lightCount] = AMBIENT;
    lightFalloffConstant[lightCount] = currentLightFalloffConstant;
    lightFalloffLinear[lightCount] = currentLightFalloffLinear;
    lightFalloffQuadratic[lightCount] = currentLightFalloffQuadratic;
    lightPosition[lightCount][0] = x;
    lightPosition[lightCount][1] = y;
    lightPosition[lightCount][2] = z;
    lightPosition[lightCount][3] = 1.0f;
    
    lightEnable(lightCount);
    lightAmbient(lightCount);
    lightPosition(lightCount);
    lightFalloff(lightCount);
    lightNoSpot(lightCount);
    lightNoDiffuse(lightCount);
    lightNoSpecular(lightCount);
    
    lightCount++;
  }

  public void directionalLight(float r, float g, float b, float nx, float ny,
      float nz) {
    enableLighting();
    if (lightCount == MAX_LIGHTS) {
      throw new RuntimeException("can only create " + MAX_LIGHTS + " lights");
    }
    colorCalc(r, g, b);
    lightDiffuse[lightCount][0] = calcR;
    lightDiffuse[lightCount][1] = calcG;
    lightDiffuse[lightCount][2] = calcB;
    lightDiffuse[lightCount][3] = 1.0f;

    lightType[lightCount] = DIRECTIONAL;
    lightFalloffConstant[lightCount] = currentLightFalloffConstant;
    lightFalloffLinear[lightCount] = currentLightFalloffLinear;
    lightFalloffQuadratic[lightCount] = currentLightFalloffQuadratic;
    lightSpecular[lightCount][0] = currentLightSpecular[0];
    lightSpecular[lightCount][1] = currentLightSpecular[1];
    lightSpecular[lightCount][2] = currentLightSpecular[2];
    lightSpecular[lightCount][3] = currentLightSpecular[3];
    
    // In this case, the normal is used to indicate the direction
    // of the light, with the w component equals to zero. See
    // the comments in the lightDirection() method.
    lightNormal[lightCount][0] = -nx;
    lightNormal[lightCount][1] = -ny;
    lightNormal[lightCount][2] = -nz;
    lightNormal[lightCount][3] = 0.0f;

    lightEnable(lightCount);
    lightNoAmbient(lightCount);
    lightDirection(lightCount);
    lightDiffuse(lightCount);
    lightSpecular(lightCount);
    lightFalloff(lightCount);
    lightNoSpot(lightCount);

    lightCount++;
  }

  public void pointLight(float r, float g, float b, float x, float y, float z) {
    enableLighting();   
    if (lightCount == MAX_LIGHTS) {
      throw new RuntimeException("can only create " + MAX_LIGHTS + " lights");
    }
    colorCalc(r, g, b);
    lightDiffuse[lightCount][0] = calcR;
    lightDiffuse[lightCount][1] = calcG;
    lightDiffuse[lightCount][2] = calcB;
    lightDiffuse[lightCount][3] = 1.0f;

    lightType[lightCount] = POINT;
    lightFalloffConstant[lightCount] = currentLightFalloffConstant;
    lightFalloffLinear[lightCount] = currentLightFalloffLinear;
    lightFalloffQuadratic[lightCount] = currentLightFalloffQuadratic;
    lightSpecular[lightCount][0] = currentLightSpecular[0];
    lightSpecular[lightCount][1] = currentLightSpecular[1];
    lightSpecular[lightCount][2] = currentLightSpecular[2];

    lightPosition[lightCount][0] = x;
    lightPosition[lightCount][1] = y;
    lightPosition[lightCount][2] = z;
    lightPosition[lightCount][3] = 1.0f;

    lightEnable(lightCount);
    lightNoAmbient(lightCount);
    lightPosition(lightCount);
    lightDiffuse(lightCount);
    lightSpecular(lightCount);
    lightFalloff(lightCount);
    lightNoSpot(lightCount);

    lightCount++;
  }

  public void spotLight(float r, float g, float b, float x, float y, float z,
      float nx, float ny, float nz, float angle, float concentration) {
    enableLighting();  
    if (lightCount == MAX_LIGHTS) {
      throw new RuntimeException("can only create " + MAX_LIGHTS + " lights");
    }
    colorCalc(r, g, b);
    lightDiffuse[lightCount][0] = calcR;
    lightDiffuse[lightCount][1] = calcG;
    lightDiffuse[lightCount][2] = calcB;
    lightDiffuse[lightCount][3] = 1.0f;

    lightType[lightCount] = SPOT;
    lightFalloffConstant[lightCount] = currentLightFalloffConstant;
    lightFalloffLinear[lightCount] = currentLightFalloffLinear;
    lightFalloffQuadratic[lightCount] = currentLightFalloffQuadratic;
    lightSpecular[lightCount][0] = currentLightSpecular[0];
    lightSpecular[lightCount][1] = currentLightSpecular[1];
    lightSpecular[lightCount][2] = currentLightSpecular[2];

    lightPosition[lightCount][0] = x;
    lightPosition[lightCount][1] = y;
    lightPosition[lightCount][2] = z;
    lightPosition[lightCount][3] = 1.0f;

    float invn = 1.0f / PApplet.dist(0, 0, 0, nx, ny, nz);
    lightNormal[lightCount][0] = invn * nx;
    lightNormal[lightCount][1] = invn * ny;
    lightNormal[lightCount][2] = invn * nz;
    lightNormal[lightCount][3] = 0.0f;

    lightSpotAngle[lightCount] = PApplet.degrees(angle);
    lightSpotAngleCos[lightCount] = Math.max(0, (float) Math.cos(angle));
    lightSpotConcentration[lightCount] = concentration;

    lightEnable(lightCount);
    lightNoAmbient(lightCount);
    lightPosition(lightCount);
    lightDirection(lightCount);
    lightDiffuse(lightCount);
    lightSpecular(lightCount);
    lightFalloff(lightCount);
    lightSpotAngle(lightCount);
    lightSpotConcentration(lightCount);

    lightCount++;
  }

  /**
   * Set the light falloff rates for the last light that was created. Default is
   * lightFalloff(1, 0, 0).
   */
  public void lightFalloff(float constant, float linear, float quadratic) {
    currentLightFalloffConstant = constant;
    currentLightFalloffLinear = linear;
    currentLightFalloffQuadratic = quadratic;
  }

  /**
   * Set the specular color of the last light created.
   */
  public void lightSpecular(float x, float y, float z) {
    colorCalc(x, y, z);
    currentLightSpecular[0] = calcR;
    currentLightSpecular[1] = calcG;
    currentLightSpecular[2] = calcB;
    currentLightSpecular[3] = 1.0f;
  }

  protected void enableLights() {
    for (int i = 0; i < lightCount; i++) {
      lightEnable(i);
    }
  }

  protected void disableLights() {
    for (int i = 0; i < lightCount; i++) {
      lightDisable(i);
    }
  }  
  
  protected void enableLighting() {
    if (!lights) {
      // Flushing non-lit geometry.
      flush();
      
      lights = true;
      gl2f.glEnable(GL2.GL_LIGHTING);
    }
  }

  protected void disableLighting() {
    if (lights) {
      // Flushing lit geometry.
      flush();
      
      lights = false;
      gl2f.glDisable(GL2.GL_LIGHTING);
    }
  }
    
  protected void lightAmbient(int num) {    
    gl2f.glLightfv(GL2.GL_LIGHT0 + num, GL2.GL_AMBIENT, lightDiffuse[num], 0);
  }

  protected void lightNoAmbient(int num) {
    gl2f.glLightfv(GL2.GL_LIGHT0 + num, GL2.GL_AMBIENT, zeroLight, 0);
  }
  
  protected void lightNoSpot(int num) {
    gl2f.glLightf(GL2.GL_LIGHT0 + num, GL2.GL_SPOT_CUTOFF, 180);
    gl2f.glLightf(GL2.GL_LIGHT0 + num, GL2.GL_SPOT_EXPONENT, 0);
  }

  protected void lightDiffuse(int num) {
    gl2f.glLightfv(GL2.GL_LIGHT0 + num, GL2.GL_DIFFUSE, lightDiffuse[num], 0);
  }

  protected void lightNoDiffuse(int num) {
    gl2f.glLightfv(GL2.GL_LIGHT0 + num, GL2.GL_DIFFUSE, zeroLight, 0);
  }
    
  protected void lightDirection(int num) {
    if (lightType[num] == DIRECTIONAL) {      
      // The w component of lightNormal[num] is zero, so the light is considered as
      // a directional source because the position effectively becomes a direction
      // in homogeneous coordinates:
      // http://glprogramming.com/red/appendixf.html
      gl2f.glLightfv(GL2.GL_LIGHT0 + num, GL2.GL_POSITION, lightNormal[num], 0);
    } else { // spotlight
      gl2f.glLightfv(GL2.GL_LIGHT0 + num, GL2.GL_SPOT_DIRECTION, lightNormal[num], 0);
    }
  }

  protected void lightEnable(int num) {
    gl2f.glEnable(GL2.GL_LIGHT0 + num);
  }

  protected void lightDisable(int num) {
    gl2f.glDisable(GL2.GL_LIGHT0 + num);
  }

  protected void lightFalloff(int num) {
    gl2f.glLightf(GL2.GL_LIGHT0 + num, GL2.GL_CONSTANT_ATTENUATION,
        lightFalloffConstant[num]);
    gl2f.glLightf(GL2.GL_LIGHT0 + num, GL2.GL_LINEAR_ATTENUATION,
        lightFalloffLinear[num]);
    gl2f.glLightf(GL2.GL_LIGHT0 + num, GL2.GL_QUADRATIC_ATTENUATION,
        lightFalloffQuadratic[num]);
  }

  protected void lightPosition(int num) {
    gl2f.glLightfv(GL2.GL_LIGHT0 + num, GL2.GL_POSITION, lightPosition[num], 0);
  }

  protected void lightSpecular(int num) {
    gl2f.glLightfv(GL2.GL_LIGHT0 + num, GL2.GL_SPECULAR, lightSpecular[num], 0);
  }

  protected void lightNoSpecular(int num) {
    gl2f.glLightfv(GL2.GL_LIGHT0 + num, GL2.GL_SPECULAR, zeroLight, 0);
  }
  
  protected void lightSpotAngle(int num) {
    gl2f.glLightf(GL2.GL_LIGHT0 + num, GL2.GL_SPOT_CUTOFF, lightSpotAngle[num]);
  }

  protected void lightSpotConcentration(int num) {
    gl2f.glLightf(GL2.GL_LIGHT0 + num, GL2.GL_SPOT_EXPONENT,
        lightSpotConcentration[num]);
  }  
  
  //////////////////////////////////////////////////////////////

  // BACKGROUND

  protected void backgroundImpl(PImage image) {
    backgroundImpl();
    set(0, 0, image);
  }

  protected void backgroundImpl() {
    gl.glClearColor(0, 0, 0, 0);
    gl.glClear(GL.GL_DEPTH_BUFFER_BIT);

    gl.glClearColor(backgroundR, backgroundG, backgroundB, 1);
    gl.glClear(GL.GL_COLOR_BUFFER_BIT);
  }  
  
  //////////////////////////////////////////////////////////////

  // COLOR MODE

  // colorMode() is inherited from PGraphics.

  //////////////////////////////////////////////////////////////

  // COLOR CALC

  // This is the OpenGL complement to the colorCalc() methods.

  /**
   * Load the calculated color into a pre-allocated array so that it can be
   * quickly passed over to OpenGL.
   */
  protected final void calcColorBuffer() {
    if (colorFloats == null) {
      colorFloats = new float[4];
    }
    colorFloats[0] = calcR;
    colorFloats[1] = calcG;
    colorFloats[2] = calcB;
    colorFloats[3] = calcA;
  }

  //////////////////////////////////////////////////////////////

  // COLOR METHODS

  // public final int color(int gray)
  // public final int color(int gray, int alpha)
  // public final int color(int rgb, float alpha)
  // public final int color(int x, int y, int z)

  // public final float alpha(int what)
  // public final float red(int what)
  // public final float green(int what)
  // public final float blue(int what)
  // public final float hue(int what)
  // public final float saturation(int what)
  // public final float brightness(int what)

  // public int lerpColor(int c1, int c2, float amt)
  // static public int lerpColor(int c1, int c2, float amt, int mode)

  //////////////////////////////////////////////////////////////

  // BEGINRAW/ENDRAW

  // beginRaw, endRaw() both inherited.

  //////////////////////////////////////////////////////////////

  // WARNINGS and EXCEPTIONS

  // showWarning() and showException() available from PGraphics.
  
  /**
   * Report on anything from glError().
   * Don't use this inside glBegin/glEnd otherwise it'll
   * throw an GL_INVALID_OPERATION error.
   */
  public void report(String where) {
    if (!hints[DISABLE_OPENGL_ERROR_REPORT]) {
      int err = gl.glGetError();
      if (err != 0) {
        String errString = glu.gluErrorString(err);
        String msg = "OpenGL error " + err + " at " + where + ": " + errString;
        PGraphics.showWarning(msg);
      }
    }
  }
  
  //////////////////////////////////////////////////////////////

  // RENDERER SUPPORT QUERIES

  // public boolean displayable()

  // public boolean dimensional() // from P3D
  
  
  /**
   * Return true if this renderer supports 2D drawing. Defaults to true.
   */
  public boolean is2D() {
    return true;
  }
  

  /**
   * Return true if this renderer supports 2D drawing. Defaults to false.
   */
  public boolean is3D() {
    return true;
  }  
  

  //////////////////////////////////////////////////////////////

  // PIMAGE METHODS

  // getImage
  // setCache, getCache, removeCache
  // isModified, setModified

  //////////////////////////////////////////////////////////////

  // LOAD/UPDATE PIXELS

  public void loadPixels() {
    if ((pixels == null) || (pixels.length != width * height)) {
      pixels = new int[width * height];      
      pixelBuffer = IntBuffer.allocate(pixels.length);      
    }

    boolean outsideDraw = primarySurface && !drawing;
    if (outsideDraw) {
      beginGLOp();      
    }
    
    boolean notCurrent = !primarySurface && offscreenFramebuffer != currentFramebuffer;
    
    if (notCurrent) {
      pushFramebuffer();
      setFramebuffer(offscreenFramebuffer);
    }
    
    pixelBuffer.rewind();
    gl.glReadPixels(0, 0, width, height, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, pixelBuffer);
    
    if (notCurrent) {
      popFramebuffer();
    } 
        
    pixelBuffer.get(pixels);

    // flip vertically (opengl stores images upside down),
    // and swap RGBA components to ARGB (big endian)
    int index = 0;
    int yindex = (height - 1) * width;
    for (int y = 0; y < height / 2; y++) {
      if (BIG_ENDIAN) {
        for (int x = 0; x < width; x++) {
          int temp = pixels[index];
          // ignores alpha component, just sets it opaque
          pixels[index] = 0xff000000 | ((pixels[yindex] >> 8) & 0x00ffffff);
          pixels[yindex] = 0xff000000 | ((temp >> 8) & 0x00ffffff);

          index++;
          yindex++;
        }
      } else { // LITTLE_ENDIAN, convert ABGR to ARGB
        for (int x = 0; x < width; x++) {
          int temp = pixels[index];

          // identical to endPixels because only two
          // components are being swapped
          pixels[index] = 0xff000000 | ((pixels[yindex] << 16) & 0xff0000)
              | (pixels[yindex] & 0xff00) | ((pixels[yindex] >> 16) & 0xff);

          pixels[yindex] = 0xff000000 | ((temp << 16) & 0xff0000)
              | (temp & 0xff00) | ((temp >> 16) & 0xff);

          index++;
          yindex++;
        }
      }
      yindex -= width * 2;
    }

    // When height is an odd number, the middle line needs to be
    // endian swapped, but not y-swapped.
    // http://dev.processing.org/bugs/show_bug.cgi?id=944
    if ((height % 2) == 1) {
      index = (height / 2) * width;
      if (BIG_ENDIAN) {
        for (int x = 0; x < width; x++) {
          // ignores alpha component, just sets it opaque
          pixels[index] = 0xff000000 | ((pixels[index] >> 8) & 0x00ffffff);
        }
      } else {
        for (int x = 0; x < width; x++) {
          pixels[index] = 0xff000000 | ((pixels[index] << 16) & 0xff0000)
              | (pixels[index] & 0xff00) | ((pixels[index] >> 16) & 0xff);
        }
      }
    }
    
    if (primarySurface) {
      // Load texture.
      loadTextureImpl(POINT);           
      pixelsToTexture();
    }
    
    if (outsideDraw) {
      endGLOp();      
    }    
  }

  /**
   * Convert native OpenGL format into palatable ARGB format. This function
   * leaves alone (ignores) the alpha component. Also flips the image
   * vertically, since images are upside-down in GL.
   */
  static public void nativeToJavaRGB(PImage image) {
    int index = 0;
    int yindex = (image.height - 1) * image.width;
    for (int y = 0; y < image.height / 2; y++) {
      if (BIG_ENDIAN) {
        for (int x = 0; x < image.width; x++) {
          int temp = image.pixels[index];
          // ignores alpha component, just sets it opaque
          image.pixels[index] = 0xff000000 | ((image.pixels[yindex] >> 8) & 0x00ffffff);
          image.pixels[yindex] = 0xff000000 | ((temp >> 8) & 0x00ffffff);
          index++;
          yindex++;
        }
      } else { // LITTLE_ENDIAN, convert ABGR to ARGB
        for (int x = 0; x < image.width; x++) {
          int temp = image.pixels[index];

          // identical to endPixels because only two
          // components are being swapped
          image.pixels[index] = 0xff000000
              | ((image.pixels[yindex] << 16) & 0xff0000)
              | (image.pixels[yindex] & 0xff00)
              | ((image.pixels[yindex] >> 16) & 0xff);

          image.pixels[yindex] = 0xff000000 | ((temp << 16) & 0xff0000)
              | (temp & 0xff00) | ((temp >> 16) & 0xff);

          index++;
          yindex++;
        }
      }
      yindex -= image.width * 2;
    }
  }

  /**
   * Convert native OpenGL format into palatable ARGB format. This function
   * leaves alone (ignores) the alpha component. Also flips the image
   * vertically, since images are upside-down in GL.
   */
  static public void nativeToJavaARGB(PImage image) {
    int index = 0;
    int yindex = (image.height - 1) * image.width;
    for (int y = 0; y < image.height / 2; y++) {
      if (BIG_ENDIAN) {
        for (int x = 0; x < image.width; x++) {
          int temp = image.pixels[index];
          // ignores alpha component, just sets it opaque
          image.pixels[index] = (image.pixels[yindex] & 0xff000000)
              | ((image.pixels[yindex] >> 8) & 0x00ffffff);
          image.pixels[yindex] = (temp & 0xff000000)
              | ((temp >> 8) & 0x00ffffff);
          index++;
          yindex++;
        }
      } else { // LITTLE_ENDIAN, convert ABGR to ARGB
        for (int x = 0; x < image.width; x++) {
          int temp = image.pixels[index];

          // identical to endPixels because only two
          // components are being swapped
          image.pixels[index] = (image.pixels[yindex] & 0xff000000)
              | ((image.pixels[yindex] << 16) & 0xff0000)
              | (image.pixels[yindex] & 0xff00)
              | ((image.pixels[yindex] >> 16) & 0xff);

          image.pixels[yindex] = (temp & 0xff000000)
              | ((temp << 16) & 0xff0000) | (temp & 0xff00)
              | ((temp >> 16) & 0xff);

          index++;
          yindex++;
        }
      }
      yindex -= image.width * 2;
    }
  }

  /**
   * Convert ARGB (Java/Processing) data to native OpenGL format. This function
   * leaves alone (ignores) the alpha component. Also flips the image
   * vertically, since images are upside-down in GL.
   */
  static public void javaToNativeRGB(PImage image) {
    int width = image.width;
    int height = image.height;
    int pixels[] = image.pixels;

    int index = 0;
    int yindex = (height - 1) * width;
    for (int y = 0; y < height / 2; y++) {
      if (BIG_ENDIAN) {
        // and convert ARGB back to opengl RGBA components (big endian)
        for (int x = 0; x < image.width; x++) {
          int temp = pixels[index];
          /*
           * pixels[index] = ((pixels[yindex] >> 24) & 0xff) | ((pixels[yindex]
           * << 8) & 0xffffff00); pixels[yindex] = ((temp >> 24) & 0xff) |
           * ((temp << 8) & 0xffffff00);
           */
          pixels[index] = ((pixels[yindex] << 8) & 0xffffff00) | 0xff;
          pixels[yindex] = ((temp << 8) & 0xffffff00) | 0xff;

          index++;
          yindex++;
        }

      } else {
        // convert ARGB back to native little endian ABGR
        for (int x = 0; x < width; x++) {
          int temp = pixels[index];

          pixels[index] = 0xff000000 | ((pixels[yindex] << 16) & 0xff0000)
              | (pixels[yindex] & 0xff00) | ((pixels[yindex] >> 16) & 0xff);

          pixels[yindex] = 0xff000000 | ((temp << 16) & 0xff0000)
              | (temp & 0xff00) | ((temp >> 16) & 0xff);

          index++;
          yindex++;
        }
      }
      yindex -= width * 2;
    }
  }

  /**
   * Convert Java ARGB to native OpenGL format. Also flips the image vertically,
   * since images are upside-down in GL.
   */
  static public void javaToNativeARGB(PImage image) {
    int width = image.width;
    int height = image.height;
    int pixels[] = image.pixels;

    int index = 0;
    int yindex = (height - 1) * width;
    for (int y = 0; y < height / 2; y++) {
      if (BIG_ENDIAN) {
        // and convert ARGB back to opengl RGBA components (big endian)
        for (int x = 0; x < image.width; x++) {
          int temp = pixels[index];
          pixels[index] = ((pixels[yindex] >> 24) & 0xff)
              | ((pixels[yindex] << 8) & 0xffffff00);
          pixels[yindex] = ((temp >> 24) & 0xff) | ((temp << 8) & 0xffffff00);

          index++;
          yindex++;
        }

      } else {
        // convert ARGB back to native little endian ABGR
        for (int x = 0; x < width; x++) {
          int temp = pixels[index];

          pixels[index] = (pixels[yindex] & 0xff000000)
              | ((pixels[yindex] << 16) & 0xff0000) | (pixels[yindex] & 0xff00)
              | ((pixels[yindex] >> 16) & 0xff);

          pixels[yindex] = (pixels[yindex] & 0xff000000)
              | ((temp << 16) & 0xff0000) | (temp & 0xff00)
              | ((temp >> 16) & 0xff);

          index++;
          yindex++;
        }
      }
      yindex -= width * 2;
    }
  }

  public void updatePixels() {
    // flip vertically (opengl stores images upside down),
    
    int index = 0;
    int yindex = (height - 1) * width;
    for (int y = 0; y < height / 2; y++) {
      if (BIG_ENDIAN) {
        // and convert ARGB back to opengl RGBA components (big endian)
        for (int x = 0; x < width; x++) {
          int temp = pixels[index];

          pixels[index] = ((pixels[yindex] << 8) & 0xffffff00) | 0xff;
          pixels[yindex] = ((temp << 8) & 0xffffff00) | 0xff;

          index++;
          yindex++;
        }

      } else {
        // convert ARGB back to native little endian ABGR
        for (int x = 0; x < width; x++) {
          int temp = pixels[index];

          pixels[index] = 0xff000000 | ((pixels[yindex] << 16) & 0xff0000)
              | (pixels[yindex] & 0xff00) | ((pixels[yindex] >> 16) & 0xff);

          pixels[yindex] = 0xff000000 | ((temp << 16) & 0xff0000)
              | (temp & 0xff00) | ((temp >> 16) & 0xff);

          index++;
          yindex++;
        }
      }
      yindex -= width * 2;
    }
    
    pixelBuffer.rewind();    
    pixelBuffer.put(pixels);
    
    boolean outsideDraw = primarySurface && !drawing;
    if (outsideDraw) {
      beginGLOp();      
    }    
    
    // Copying pixel buffer to screen texture...
    copyToTexture(pixelBuffer);
    
    // ...and drawing the texture to screen.
    
    boolean notCurrent = !primarySurface && offscreenFramebuffer != currentFramebuffer;
    if (notCurrent) {
      // If this is an offscreen surface that is not current, then the offscreen framebuffer
      // is bound so the texture is drawn to it instead to the main surface. Even if the
      // surface in antialiased we don't need to bind the multisample framebuffer, we
      // just draw to the regular offscreen framebuffer.
      pushFramebuffer();
      setFramebuffer(offscreenFramebuffer);
    }
    
    drawTexture();
    
    if (notCurrent) {
      popFramebuffer();
    }    
    
    if (outsideDraw) {
      endGLOp();      
    }    
  }
    
  //////////////////////////////////////////////////////////////

  // LOAD/UPDATE TEXTURE
  
  public void loadTexture() {
    if (primarySurface) {
      if (!drawing) {
        beginGLOp();      
      }   
      
      loadTextureImpl(POINT);      
      loadPixels();      
      pixelsToTexture();
      
      if (!drawing) {
        endGLOp();      
      }            
    }
  }
  
  protected void loadTextureImpl(int sampling) {
    if (width == 0 || height == 0) return;
    if (texture == null) {
      PTexture.Parameters params = PTexture.newParameters(ARGB, sampling);
      texture = new PTexture(parent, width, height, params);      
      texture.setFlippedY(true);
      this.setCache(ogl, texture);
      this.setParams(ogl, params);
      
      texCrop = new int[4];
      texCrop[0] = 0;
      texCrop[1] = 0;
      texCrop[2] = width;
      texCrop[3] = height;     
    }
  }   
  
  // Draws wherever it is in the screen texture right now to the screen.
  public void updateTexture() {
    boolean outsideDraw = primarySurface && !drawing;
    if (outsideDraw) {
      beginGLOp();      
    }    
    
    boolean notCurrent = !primarySurface && offscreenFramebuffer != currentFramebuffer;
    if (notCurrent) {
      pushFramebuffer();
      setFramebuffer(offscreenFramebuffer);
    }  
    
    drawTexture();
    
    if (notCurrent) {
      popFramebuffer();
    }
    
    if (outsideDraw) {
      endGLOp();      
    }    
  }
  
  protected void drawTexture() {
    drawTexture(texture, texCrop, 0, 0, width, height);
  }
  
  protected void copyToTexture(IntBuffer buffer) {
    copyToTexture(texture, buffer, 0, 0, width, height);
  }
  
  protected void copyFrameToTexture() {
    // Make sure that the execution off all the openGL commands is 
    // finished before loading the texture. 
    gl.glFinish(); 
    loadTexture();    
  }
  
  protected void pixelsToTexture() {    
    texture.set(pixels);
  }
  
  protected void textureToPixels() {
    texture.get(pixels);
  }

  //////////////////////////////////////////////////////////////

  // RESIZE

  public void resize(int wide, int high) {
    PGraphics.showMethodWarning("resize");
  }

  //////////////////////////////////////////////////////////////

  // GET/SET
  
  public int get(int x, int y) {
    if (getsetBuffer == null) {
      getsetBuffer = IntBuffer.allocate(1);      
    }
    
    boolean outsideDraw = primarySurface && !drawing;
    if (outsideDraw) {
      beginGLOp();      
    }       
    
    boolean notCurrent = !primarySurface && offscreenFramebuffer != currentFramebuffer;
    if (notCurrent) {
      // If the surface is not primary and multisampling is on, then the framebuffer
      // will be switched momentarily from offscreenFramebufferMultisample to offscreenFramebuffer.
      // This is in fact correct, because the glReadPixels() function doesn't work with 
      // multisample framebuffer attached as the read buffer.
      pushFramebuffer();
      setFramebuffer(offscreenFramebuffer);
    }
     
    getsetBuffer.rewind();
    gl.glReadPixels(x, height - y - 1, 1, 1, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, getsetBuffer);

    if (notCurrent) {
      popFramebuffer();
    }

    int getset = getsetBuffer.get(0);
    
    if (outsideDraw) {
      endGLOp();      
    }    
    
    if (BIG_ENDIAN) {
      return 0xff000000 | ((getset >> 8) & 0x00ffffff);

    } else {
      return 0xff000000 | ((getset << 16) & 0xff0000) | (getset & 0xff00)
          | ((getset >> 16) & 0xff);
    }   
  }

  // public PImage get(int x, int y, int w, int h)

  protected PImage getImpl(int x, int y, int w, int h) {
    PImage newbie = parent.createImage(w, h, ARGB);
    PTexture newbieTex = addTexture(newbie);

    IntBuffer newbieBuffer = IntBuffer.allocate(w * h);    
    
    boolean notCurrent = !primarySurface && offscreenFramebuffer != currentFramebuffer;
    if (notCurrent) {
      pushFramebuffer();
      setFramebuffer(offscreenFramebuffer);
    }    

    newbieBuffer.rewind();
    gl.glReadPixels(x, height - y - h, w, h, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, newbieBuffer);

    if (notCurrent) {
      popFramebuffer();
    }    
    
    copyToTexture(newbieTex, newbieBuffer, 0, 0, w, h);
    newbie.loadPixels();
    newbieTex.setFlippedY(true);
    newbieTex.get(newbie.pixels);
    
    return newbie;
  }

  public PImage get() {
    return get(0, 0, width, height);
  }

  public void set(int x, int y, int argb) {
    int getset = 0;

    if (BIG_ENDIAN) {
      // convert ARGB to RGBA
      getset = (argb << 8) | 0xff;
    } else {
      // convert ARGB to ABGR
      getset = (argb & 0xff00ff00) | ((argb << 16) & 0xff0000)
          | ((argb >> 16) & 0xff);
    }
    
    if (getsetBuffer == null) {
      getsetBuffer = IntBuffer.allocate(1);
      getsetBuffer.rewind();
    }
    
    getsetBuffer.put(0, getset);
    getsetBuffer.rewind();    
    
    if (getsetTexture == null) {
      getsetTexture = new PTexture(parent, 1, 1, new PTexture.Parameters(ARGB, POINT));
    }
    
    boolean outsideDraw = primarySurface && !drawing;
    if (outsideDraw) {
      beginGLOp();      
    }    
    
    copyToTexture(getsetTexture, getsetBuffer, 0, 0, 1, 1);
    
    boolean notCurrent = !primarySurface && offscreenFramebuffer != currentFramebuffer;
    if (notCurrent) {
      pushFramebuffer();
      setFramebuffer(offscreenFramebuffer);
    } 
    
    drawTexture(getsetTexture, 0, 0, 1, 1, x, height - y, 1, 1);
    
    if (notCurrent) {
      popFramebuffer();
    }
    
    if (outsideDraw) {
      endGLOp();      
    }    
  }

  /**
   * Set an image directly to the screen.
   * 
   */
  public void set(int x, int y, PImage source) {
    PTexture tex = getTexture(source);
    if (tex != null) {
      int w = source.width; 
      int h = source.height;
      
      boolean outsideDraw = primarySurface && !drawing;
      if (outsideDraw) {
        beginGLOp();      
      }        
      
      boolean notCurrent = !primarySurface && offscreenFramebuffer != currentFramebuffer;
      if (notCurrent) {
        pushFramebuffer();
        setFramebuffer(offscreenFramebuffer);
      }
      
      // The crop region and draw rectangle are given like this to take into account
      // inverted y-axis in Processin with respect to OpenGL.
      drawTexture(tex, 0, 0, w, h, x, height - y, w, -h);
      
      if (notCurrent) {
        popFramebuffer();
      }     
      
      if (outsideDraw) {
        endGLOp();      
      }      
    }
  }  
      
  //////////////////////////////////////////////////////////////

  // MASK

  public void mask(int alpha[]) {
    PGraphics.showMethodWarning("mask");
  }
  

  public void mask(PImage alpha) {
    PGraphics.showMethodWarning("mask");
  }

  //////////////////////////////////////////////////////////////

  // FILTER

  /**
   * This is really inefficient and not a good idea in OpenGL. Use get() and
   * set() with a smaller image area, or call the filter on an image instead,
   * and then draw that.
   */
  public void filter(int kind) {
    PImage temp = get();
    temp.filter(kind);
    set(0, 0, temp);
  }

  /**
   * This is really inefficient and not a good idea in OpenGL. Use get() and
   * set() with a smaller image area, or call the filter on an image instead,
   * and then draw that.
   */
  public void filter(int kind, float param) {
    PImage temp = get();
    temp.filter(kind, param);
    set(0, 0, temp);
  }

  //////////////////////////////////////////////////////////////

  /**
   * Extremely slow and not optimized, should use GL methods instead. Currently
   * calls a beginPixels() on the whole canvas, then does the copy, then it
   * calls endPixels().
   */
  // public void copy(int sx1, int sy1, int sx2, int sy2,
  // int dx1, int dy1, int dx2, int dy2)

  /**
   * TODO - extremely slow and not optimized. Currently calls a beginPixels() on
   * the whole canvas, then does the copy, then it calls endPixels().
   */
  // public void copy(PImage src,
  // int sx1, int sy1, int sx2, int sy2,
  // int dx1, int dy1, int dx2, int dy2)

  //////////////////////////////////////////////////////////////

  // BLEND

  // static public int blendColor(int c1, int c2, int mode)

  // public void blend(PImage src,
  // int sx, int sy, int dx, int dy, int mode) {
  // set(dx, dy, PImage.blendColor(src.get(sx, sy), get(dx, dy), mode));
  // }

  /**
   * Extremely slow and not optimized, should use GL methods instead. Currently
   * calls a beginPixels() on the whole canvas, then does the copy, then it
   * calls endPixels(). Please help fix: <A
   * HREF="http://dev.processing.org/bugs/show_bug.cgi?id=941">Bug 941</A>, <A
   * HREF="http://dev.processing.org/bugs/show_bug.cgi?id=942">Bug 942</A>.
   */
  // public void blend(int sx1, int sy1, int sx2, int sy2,
  // int dx1, int dy1, int dx2, int dy2, int mode) {
  // loadPixels();
  // super.blend(sx1, sy1, sx2, sy2, dx1, dy1, dx2, dy2, mode);
  // updatePixels();
  // }

  // public void blend(PImage src,
  // int sx1, int sy1, int sx2, int sy2,
  // int dx1, int dy1, int dx2, int dy2, int mode) {
  // loadPixels();
  // super.blend(src, sx1, sy1, sx2, sy2, dx1, dy1, dx2, dy2, mode);
  // updatePixels();
  // }

  /**
   * Allows to set custom blend modes for the entire scene, using openGL.
   * Reference article about blending modes:
   * http://www.pegtop.net/delphi/articles/blendmodes/
   */
  public void blendMode(int mode) {
    if (blendMode != mode) {
      // Flushing any remaining geometry that uses a different blending
      // mode.
      flush();
    
      blendMode = mode;
      gl.glEnable(GL.GL_BLEND);
      
      if (mode == REPLACE) {
        // This is equivalent to disable blending.
        if (blendEqSupported) gl.glBlendEquation(GL.GL_FUNC_ADD);
        gl.glBlendFunc(GL.GL_ONE, GL.GL_ZERO);
      } else if (mode == BLEND) {
        if (blendEqSupported) gl.glBlendEquation(GL.GL_FUNC_ADD);
        gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
      } else if (mode == ADD) {
        if (blendEqSupported) gl.glBlendEquation(GL.GL_FUNC_ADD);
        gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE);
      } else if (mode == SUBTRACT) {
        if (blendEqSupported) gl.glBlendEquation(GL.GL_FUNC_ADD);
        gl.glBlendFunc(GL.GL_ONE_MINUS_DST_COLOR, GL.GL_ZERO); 
      } else if (mode == LIGHTEST) {
        if (blendEqSupported) { 
          gl.glBlendEquation(GL2.GL_MAX);
          gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_DST_ALPHA);
        } else {
          PGraphics.showWarning("P3D: This blend mode is currently unsupported.");
        }
      } else if (mode == DARKEST) {
        if (blendEqSupported) { 
          gl.glBlendEquation(GL2.GL_MIN);      
          gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_DST_ALPHA);
        } else {
          PGraphics.showWarning("P3D: This blend mode is currently unsupported.");  
        }
      } else if (mode == DIFFERENCE) {
        if (blendEqSupported) {
          gl.glBlendEquation(GL.GL_FUNC_REVERSE_SUBTRACT);
          gl.glBlendFunc(GL.GL_ONE, GL.GL_ONE);
        } else {
          PGraphics.showWarning("P3D: This blend mode is currently unsupported.");
        }       
      } else if (mode == EXCLUSION) {
        if (blendEqSupported) gl.glBlendEquation(GL.GL_FUNC_ADD);
        gl.glBlendFunc(GL.GL_ONE_MINUS_DST_COLOR, GL.GL_ONE_MINUS_SRC_COLOR);
      } else if (mode == MULTIPLY) {
        if (blendEqSupported) gl.glBlendEquation(GL.GL_FUNC_ADD);      
        gl.glBlendFunc(GL.GL_DST_COLOR, GL.GL_SRC_COLOR);
      } else if (mode == SCREEN) {
        if (blendEqSupported) gl.glBlendEquation(GL.GL_FUNC_ADD);
        gl.glBlendFunc(GL.GL_ONE_MINUS_DST_COLOR, GL.GL_ONE);
      }  
      // HARD_LIGHT, SOFT_LIGHT, OVERLAY, DODGE, BURN modes cannot be implemented
      // in fixed-function pipeline because they require conditional blending and
      // non-linear blending equations.
    }
  }

  protected void setDefaultBlend() {
    blendMode = BLEND;
    gl.glEnable(GL.GL_BLEND);
    if (blendEqSupported) gl.glBlendEquation(GL.GL_FUNC_ADD);
    gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);    
  }
  
  
  //////////////////////////////////////////////////////////////

  // SAVE

  // public void save(String filename) // PImage calls loadPixels()

  //////////////////////////////////////////////////////////////
  
  // SHAPE I/O  
  
  protected String[] getSupportedShapeFormats() {
    return new String[] { "obj" };
  }
    
  
  protected PShape loadShape(String filename, Object params) {
    return null;
    //return new PShape3D(parent, filename, (PShape3D.Parameters)params);
  }
  
  
  protected PShape createShape(int size, Object params) {
    return null;
    //return new PShape3D(parent, size, (PShape3D.Parameters)params);
  }
  
  
  //////////////////////////////////////////////////////////////
  
  // TEXTURE UTILS  

  /**
   * This utility method returns the texture associated to the renderer's.
   * drawing surface, making sure is updated to reflect the current contents
   * off the screen (or offscreen drawing surface).
   */    
  public PTexture getTexture() {    
    loadTexture();
    return texture;
  }
  
  /**
   * This utility method returns the texture associated to the image.
   * creating and/or updating it if needed.
   * @param img the image to have a texture metadata associated to it
   */  
  public PTexture getTexture(PImage img) {
    PTexture tex = (PTexture)img.getCache(ogl);
    if (tex == null) {
      tex = addTexture(img);
    } else {       
      if (context.hashCode() != tex.context.hashCode()) {
        // The texture was created with a different context. We need
        // to recreate it. First, we make sure that the old GL id
        // is not used to delete the texture object (it was already
        // deleted when the context changed).
        tex.glID = 0;
        tex = addTexture(img);
        // TODO: apply this mechanism to all the Processing objects using
        // GL resources (PShape, PShader, PFramebuffer). They will probably 
        // need the cache thingy as well.
      }
      
      if (img.isModified()) {
        if (img.width != tex.width || img.height != tex.height) {
          tex.init(img.width, img.height);
        }
        updateTexture(img, tex);        
      }
      
    }
    return tex;
  }
  
  /**
   * This utility method creates a texture for the provided image, and adds it
   * to the metadata cache of the image.
   * @param img the image to have a texture metadata associated to it
   */
  protected PTexture addTexture(PImage img) {
    PTexture.Parameters params = (PTexture.Parameters)img.getParams(ogl);
    if (params == null) {
      params = PTexture.newParameters();
      img.setParams(ogl, params);
    }
    PTexture tex = new PTexture(img.parent, img.width, img.height, params);
    img.loadPixels();    
    if (img.pixels != null) tex.set(img.pixels);
    img.setCache(ogl, tex);
    return tex;
  }
  
  protected PImage wrapTexture(PTexture tex) {
    // We don't use the PImage(int width, int height, int mode) constructor to
    // avoid initializing the pixels array.
    PImage img = new PImage();
    img.parent = parent;
    img.width = tex.width; 
    img.height = tex.height;
    img.format = ARGB;    
    img.setCache(ogl, tex);
    return img;
  }
    
  
  
  protected void updateTexture(PImage img, PTexture tex) {    
    if (tex != null) {
      int x = img.getModifiedX1();
      int y = img.getModifiedY1();
      int w = img.getModifiedX2() - x;
      int h = img.getModifiedY2() - y;      
      tex.set(img.pixels, x, y, w, h, img.format);
    }
    img.setModified(false);
  }
  
  
  /** Utility function to render texture. */
  protected void drawTexture(PTexture tex, int x1, int y1, int w1, int h1, int x2, int y2, int w2, int h2) {
    int[] crop = {x1, y1, w1, h1};
    drawTexture(tex, crop, x2, y2, w2, h2);    
  }
  
  
  /** Utility function to render texture. */
  protected void drawTexture(PTexture tex, int[] crop, int x, int y, int w, int h) {
    drawTexture(tex.glTarget, tex.glID, tex.glWidth, tex.glHeight, crop, x, y, w, h);
  }  
  
  
  /** Utility function to render texture. */
  protected void drawTexture(int target, int id, int w, int h, int x1, int y1, int w1, int h1, int x2, int y2, int w2, int h2) {
    int[] crop = {x1, y1, w1, h1};
    drawTexture(target, id, w, h, crop, x2, y2, w2, h2);    
  }  
  
  
  /** Utility function to render texture wihtout blend. */
  protected void drawTexture(int target, int id, int tw, int th, int[] crop, int x, int y, int w, int h) {
    gl.glEnable(target);
    gl.glBindTexture(target, id);
    
    int savedBlendMode = blendMode;
    blendMode(REPLACE); 
    
    // The texels of the texture replace the color of wherever is on the screen.
    gl2f.glTexEnvi(GL2.GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_MODE, GL2.GL_REPLACE);       
    
    drawTexture(tw, th, crop, x, y, w, h);
    
    // Returning to the default texture environment mode, GL_MODULATE. This allows tinting a texture
    // with the current fragment color.
    gl2f.glTexEnvi(GL2.GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_MODE, GL2.GL_MODULATE);        
    
    gl.glBindTexture(target, 0);
    gl.glDisable(target);
    
    blendMode(savedBlendMode);
  }  
  
  protected void drawTexture(int w, int h, int x1, int y1, int w1, int h1, int x2, int y2, int w2, int h2) {
    int[] crop = {x1, y1, w1, h1};
    drawTexture(w, h, crop, x2, y2, w2, h2);
  }
  
  /** 
   * Utility function to render currently bound texture using current blend mode. 
   * Equivalent to:
   * glTexParameteriv(GL10.GL_TEXTURE_2D, GL11Ext.GL_TEXTURE_CROP_RECT_OES, crop, 0);
   * glDrawTexiOES(x, y, 0, w, h);
   * in OpenGL ES. 
   */
  protected void drawTexture(int tw, int th, int[] crop, int x, int y, int w, int h) {
    gl.glDepthMask(false);

    gl2f.glMatrixMode(GL2.GL_PROJECTION);
    gl2f.glPushMatrix();
    gl2f.glLoadIdentity();
    
    gl2f.glOrthof(0, width, 0, height, -1, 1);
    
    gl2f.glMatrixMode(GL2.GL_MODELVIEW);
    gl2f.glPushMatrix();
    gl2f.glLoadIdentity();      

    gl2f.glTranslatef(x, y, 0);
    gl2f.glScalef(w, h, 1);
    // Rendering the quad with the appropriate texture coordinates needed for the
    // specified crop region
    float s0 = (float)crop[0] / tw;
    float s1 = (float)(crop[0] + crop[2]) / tw;    
    float t0 = (float)crop[1] / th;
    float t1 = (float)(crop[1] + crop[3]) / th;
    drawTexQuad(s0, t0, s1, t1);

    // Restoring matrices.
    gl2f.glMatrixMode(GL2.GL_PROJECTION);
    gl2f.glPopMatrix();
    gl2f.glMatrixMode(GL2.GL_MODELVIEW);
    gl2f.glPopMatrix();
    
    if (hintEnabled(ENABLE_DEPTH_MASK)) {
      gl.glDepthMask(true);        
    } else {
      gl.glDepthMask(false);
    }    
  }
  
  protected void allocateTexQuad() {  
    ByteBuffer vbb = ByteBuffer.allocateDirect(4 * 3 * SIZEOF_FLOAT);
    vbb.order(ByteOrder.nativeOrder());
    quadVertexBuffer = vbb.asFloatBuffer();

    ByteBuffer tbb = ByteBuffer.allocateDirect(4 * 2 * SIZEOF_FLOAT);
    tbb.order(ByteOrder.nativeOrder());
    quadTexCoordBuffer = tbb.asFloatBuffer();
    
    quadVertexBuffer.position(0);       
    quadVertexBuffer.put(new float[] {0, 0, 0, 
                                      0, 1, 0, 
                                      1, 0, 0,                        
                                      1, 1, 0});
  }
  
  protected void drawTexQuad() { 
    drawTexQuad(0, 0, 1, 1);
  }
  
  /** 
   * Pushes a normalized (1x1) textured quad to the GPU.
   */
  protected void drawTexQuad(float u0, float v0, float u1, float v1) {  
    if (quadVertexBuffer == null) {
      allocateTexQuad();
    }
    
    quadTexCoordBuffer.position(0);
    quadTexCoordBuffer.put(new float[] {u0, v0, 
                                        u0, v1, 
                                        u1, v0, 
                                        u1, v1});
    
    gl2f.glEnableClientState(GL2.GL_VERTEX_ARRAY);
    gl2f.glEnableClientState(GL2.GL_TEXTURE_COORD_ARRAY);

    quadVertexBuffer.position(0);
    quadTexCoordBuffer.position(0);
    
    gl2f.glVertexPointer(3, GL.GL_FLOAT, 0, quadVertexBuffer);
    gl2f.glTexCoordPointer(2, GL.GL_FLOAT, 0, quadTexCoordBuffer);
    gl2f.glDrawArrays(GL.GL_TRIANGLE_STRIP, 0, 4);

    gl2f.glDisableClientState(GL2.GL_TEXTURE_COORD_ARRAY);
    gl2f.glDisableClientState(GL2.GL_VERTEX_ARRAY);    
  }  
  
  
  /** 
   * Utility function to copy buffer to texture.
   */
  protected void copyToTexture(PTexture tex, IntBuffer buffer, int x, int y, int w, int h) {    
    buffer.rewind();
    gl.glEnable(tex.glTarget);
    gl.glBindTexture(tex.glTarget, tex.glID);    
    gl.glTexSubImage2D(tex.glTarget, 0, x, y, w, h, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, buffer);
    gl.glBindTexture(tex.glTarget, 0);
    gl.glDisable(tex.glTarget);
  }   

  //////////////////////////////////////////////////////////////
  
  // OPENGL ROUTINES    
  
  protected void setSurfaceParams() {
    // The default shade model is GL_SMOOTH, but we set
    // here just in case...
    gl2f.glShadeModel(GL2.GL_SMOOTH);    
    
    // The ambient and diffuse components for each vertex are taken
    // from the glColor/color buffer setting:
    gl2f.glEnable(GL2.GL_COLOR_MATERIAL);
    // For a quick overview of how the lighting model works in OpenGL
    // see this page:
    // http://www.sjbaker.org/steve/omniv/opengl_lighting.html
    
    // Some normal related settings:
    gl2f.glEnable(GL2.GL_NORMALIZE);
    gl2f.glEnable(GL2.GL_RESCALE_NORMAL);
    
    // Light model defaults:
    // The default opengl ambient light is (0.2, 0.2, 0.2), so
    // here we set our own default value.
    gl2f.glLightModelfv(GL2.GL_LIGHT_MODEL_AMBIENT, baseLight, 0);
    gl2f.glLightModelf(GL2.GL_LIGHT_MODEL_TWO_SIDE, 0);    
  }  
  
  protected void saveGLMatrices() {
    gl2f.glMatrixMode(GL2.GL_PROJECTION);
    gl2f.glPushMatrix();
    gl2f.glMatrixMode(GL2.GL_MODELVIEW);
    gl2f.glPushMatrix();      
  }

  protected void restoreGLMatrices() {
    // Restoring matrices.
    gl2f.glMatrixMode(GL2.GL_PROJECTION);
    gl2f.glPopMatrix();
    gl2f.glMatrixMode(GL2.GL_MODELVIEW);
    gl2f.glPopMatrix();    
  }  
  
    
  protected void setDefNormals(float nx, float ny, float nz) { 
    gl2f.glNormal3f(nx, ny, nz);
  }
  
  //////////////////////////////////////////////////////////////
  
  // INITIALIZATION ROUTINES    
  
  static public void init() {
    init(true);
  }

  /**
   * This static method can be called by applications that use
   * Processing+P3D inside their own GUI, so they can initialize
   * JOGL2 before anything else.
   * According to the JOGL2 documentation, applications shall call 
   * GLProfile.initSingleton() ASAP, before any other UI invocation.
   * In case applications are able to initialize JOGL before any other 
   * UI action, hey shall invoke this method with beforeUI=true and 
   * benefit from fast native multithreading support on all platforms 
   * if possible. 
   *
   */
  static public void init(boolean beforeUI) {
    try {
      GLProfile.initSingleton(beforeUI);
    } catch (Exception e) {
      e.printStackTrace();
    }    
  }
    
  protected void initPrimary() {
    if (parent.online) {
      // RCP Application (Applet's, Webstart, Netbeans, ..) using JOGL may not 
      // be able to initialize JOGL before the first UI action, so initSingleton()
      // is called with its argument set to false.
      GLProfile.initSingleton(false);
    } else {
      if (PApplet.platform == LINUX) {
        // Special case for Linux, since the multithreading issues described for
        // example here:
        // http://forum.jogamp.org/QtJambi-JOGL-Ubuntu-Lucid-td909554.html
        // have not been solved yet (at least for stable release b32 of JOGL2).
        GLProfile.initSingleton(false);
      } else { 
        GLProfile.initSingleton(true);
      }
    }
    
    ogl = this;
    
    profile = null;      
    
    profile = GLProfile.getDefault();
    //profile = GLProfile.get(GLProfile.GL2ES1);    
    //profile = GLProfile.get(GLProfile.GL4bc);
    //profile = GLProfile.getMaxProgrammable();    
    pipeline = FIXED;

    /*
    // Profile auto-selection disabled for the time being.
    // TODO: Implement programmable pipeline :-)
    try {
      profile = GLProfile.get(GLProfile.GL4);
      pipeline = PROG_GL4;
    } catch (GLException e) {}   
    
    if (profile == null) {
      try {
        profile = GLProfile.get(GLProfile.GL3);
        pipeline = PROG_GL3;
      } catch (GLException e) {}           
    }
    
    if (profile == null) {
      try {
        profile = GLProfile.get(GLProfile.GL2ES2);
        pipeline = PROG_GL2;
      } catch (GLException e) {}           
    }

    if (profile == null) {
      try {
        profile = GLProfile.get(GLProfile.GL2ES1);
        pipeline = FIXED;
      } catch (GLException e) {}
    }
    */      
          
    if (profile == null) {
      parent.die("Cannot get a valid OpenGL profile");
    }

    capabilities = new GLCapabilities(profile);
    if (1 < antialias) {
      capabilities.setSampleBuffers(true);
      capabilities.setNumSamples(antialias);
    } else {
      capabilities.setSampleBuffers(false);
    }

    // Getting the native window:
    // http://www.java-gaming.org/index.php/topic,21559.0.html
    AWTGraphicsScreen screen = (AWTGraphicsScreen)AWTGraphicsScreen.createDefault();
    AWTGraphicsConfiguration config = (AWTGraphicsConfiguration)GraphicsConfigurationFactory
        .getFactory(AWTGraphicsDevice.class).chooseGraphicsConfiguration(capabilities, capabilities, null, screen);
    NativeWindow win = NativeWindowFactory.getNativeWindow(parent, config);    
    
    // With the native window we get the drawable and context:
    GLDrawableFactory factory = GLDrawableFactory.getFactory(profile);
    drawable = factory.createGLDrawable(win);
    context = drawable.createContext(null);    
  }
  
  protected void initOffscreen() {
    // Getting the context and capabilities from the main renderer.
    ogl = (PGraphicsOpenGL)parent.g;
    
    context = ogl.getContext();
    capabilities = ogl.getCapabilities();
    drawable = null;
    
    //registerPGLObject(this);
    
    updateGLInterfaces();
    loadTextureImpl(BILINEAR);
    
    // In case of reinitialization (for example, when the smooth level
    // is changed), we make sure that all the OpenGL resources associated
    // to the surface are released by calling delete().
    if (offscreenFramebuffer != null) {
      //offscreenFramebuffer.delete();
      offscreenFramebuffer = null;
    }
    if (offscreenFramebufferMultisample != null) {
      //offscreenFramebufferMultisample.delete();
      offscreenFramebufferMultisample = null;
    }
    
    // We need the GL2GL3 profile to access the glRenderbufferStorageMultisample
    // function used in multisampled (antialiased) offscreen rendering.        
    if (PGraphicsOpenGL.fboMultisampleSupported && gl2x != null && 1 < antialias) {
      int nsamples = antialias;
      offscreenFramebufferMultisample = new PFramebuffer(parent, texture.glWidth, texture.glHeight, nsamples, 0, 
                                                         offscreenDepthBits, offscreenStencilBits, 
                                                         offscreenDepthBits == 24 && offscreenStencilBits == 8, false);
      offscreenFramebufferMultisample.clear();
      offscreenMultisample = true;
      
      // The offscreen framebuffer where the multisampled image is finally drawn to doesn't
      // need depth and stencil buffers since they are part of the multisampled framebuffer.
      offscreenFramebuffer = new PFramebuffer(parent, texture.glWidth, texture.glHeight, 1, 1, 
                                              0, 0,
                                              false, false); 
    } else {
      offscreenFramebuffer = new PFramebuffer(parent, texture.glWidth, texture.glHeight, 1, 1, 
                                              offscreenDepthBits, offscreenStencilBits,
                                              offscreenDepthBits == 24 && offscreenStencilBits == 8, false);
      offscreenMultisample = false;
    }
        
    offscreenFramebuffer.setColorBuffer(texture);
    offscreenFramebuffer.clear();
  }
  
  protected void updateOffscreenContext() {
    context = ogl.getContext();
    capabilities = ogl.getCapabilities();
    drawable = null;    
    
    updateGLInterfaces();
  }  
  
  protected void getGLParameters() {
    OPENGL_VENDOR = gl.glGetString(GL.GL_VENDOR);
    OPENGL_RENDERER = gl.glGetString(GL.GL_RENDERER);
    OPENGL_VERSION = gl.glGetString(GL.GL_VERSION);

    // Better way to check for extensions and related functions (taken from jMonkeyEngine):
    // renderbufferStorageMultisample = gl.isExtensionAvailable("GL_EXT_framebuffer_multisample") && 
    //                                  gl.isFunctionAvailable("glRenderbufferStorageMultisample");    
    // For more details on GL properties initialiation in jMonkey using JOGL2, take a look at:
    // http://code.google.com/p/jmonkeyengine/source/browse/branches/jme3/src/jogl2/com/jme3/renderer/jogl/JoglRenderer.java
    
    npotTexSupported = false;
    mipmapGeneration = false;
    matrixGetSupported = false;
    texenvCrossbarSupported = false;
    vboSupported = false;
    fboSupported = false;
    fboMultisampleSupported = false;
    OPENGL_EXTENSIONS = gl.glGetString(GL.GL_EXTENSIONS);      
    if (-1 < OPENGL_EXTENSIONS.indexOf("texture_non_power_of_two")) {
      npotTexSupported = true;
    }
    if (-1 < OPENGL_EXTENSIONS.indexOf("generate_mipmap")) {
      mipmapGeneration = true;
    }
    if (-1 < OPENGL_EXTENSIONS.indexOf("matrix_get")) {
      matrixGetSupported = true;
    }
    if (-1 < OPENGL_EXTENSIONS.indexOf("texture_env_crossbar")) {
      texenvCrossbarSupported = true;
    }
    if (-1 < OPENGL_EXTENSIONS.indexOf("vertex_buffer_object")) {
      vboSupported = true;
    }
    if (-1 < OPENGL_EXTENSIONS.indexOf("framebuffer_object")) {
      fboSupported = true;
    }
    if (-1 < OPENGL_EXTENSIONS.indexOf("framebuffer_multisample")) {
      fboMultisampleSupported = true;
    }    

    blendEqSupported = true;   
    
    int temp[] = new int[2];    
          
    gl.glGetIntegerv(GL.GL_MAX_TEXTURE_SIZE, temp, 0);
    maxTextureSize = temp[0];

    gl.glGetIntegerv(GL.GL_ALIASED_LINE_WIDTH_RANGE, temp, 0);
    maxLineWidth  = temp[1];        
    
    gl.glGetIntegerv(GL.GL_ALIASED_POINT_SIZE_RANGE, temp, 0);
    maxPointSize = temp[1];        
    
    // The maximum number of texture units only makes sense in the
    // fixed pipeline.
    gl.glGetIntegerv(GL2.GL_MAX_TEXTURE_UNITS, temp, 0);
    maxTextureUnits = temp[0];
    
    glParamsRead = true;
  }
    
  
  //////////////////////////////////////////////////////////////
  
  // UTILITY INNER CLASSES    
    
  /**
   *  This class encapsulates the drawing state in Processing.
   */  
  protected class DrawingState {
    int tMode0;    
    boolean auto0;
    boolean stroke0;
    int cMode0;
    boolean merge0;
    float specularR0, specularG0, specularB0;    
    float ambientR0, ambientG0, ambientB0; 
    boolean fill0;
    float fillR0, fillG0, fillB0, fillA0;
    int fillRi0, fillGi0, fillBi0, fillAi0;
    int fillColor0;
    boolean fillAlpha0;
    boolean tint0;
    float tintR0, tintG0, tintB0, tintA0;
    int tintRi0, tintGi0, tintBi0, tintAi0;
    int tintColor0;
    boolean tintAlpha0;
    float shininess0;        
    
    DrawingState() {}
    
    void save() {      
      tMode0 = textureMode;
      auto0 = autoNormal;
      stroke0 = stroke;      
      cMode0 = colorMode; 
      //merge0 = mergeRecShapes;
      
      // Saving current colors.
      specularR0 = specularR;
      specularG0 = specularG;
      specularB0 = specularB;
       
      ambientR0 = ambientR;
      ambientG0 = ambientG;
      ambientB0 = ambientB;    
      
      fill0 = fill;
      fillR0 = fillR;
      fillG0 = fillG;
      fillB0 = fillB;
      fillA0 = fillA;
      fillRi0 = fillRi;
      fillGi0 = fillGi;
      fillBi0 = fillBi;
      fillAi0 = fillAi;
      fillColor0 = fillColor;
      fillAlpha0 = fillAlpha;
      
      tint0 = tint;
      tintR0 = tintR;
      tintG0 = tintG;
      tintB0 = tintB;
      tintA0 = tintA;
      tintRi0 = tintRi;
      tintGi0 = tintGi;
      tintBi0 = tintBi;
      tintAi0 = tintAi;
      tintColor0 = tintColor;
      tintAlpha0 = tintAlpha;    
      
      shininess0 = shininess;    
    }

    void restore() {
      textureMode = tMode0;
      colorMode = cMode0;
      autoNormal = auto0;
      stroke = stroke0;
      //mergeRecShapes = merge0;
      
      // Restore colors
      calcR = specularR0;
      calcG = specularG0;
      calcB = specularB0;
      specularFromCalc();
      
      calcR = ambientR0;
      calcG = ambientG0;
      calcB = ambientB0;    
      ambientFromCalc();
      
      if (!fill0) {
        noFill();
      } else {
        calcR = fillR0;
        calcG = fillG0;
        calcB = fillB0;
        calcA = fillA0;
        calcRi = fillRi0;
        calcGi = fillGi0;
        calcBi = fillBi0;
        calcAi = fillAi0;
        calcColor = fillColor0;
        calcAlpha = fillAlpha0;
        fillFromCalc();
      }

      if (!tint0) {
        noTint();
      } else {
        calcR = tintR0;
        calcG = tintG0;
        calcB = tintB0;
        calcA = tintA0;
        calcRi = tintRi0;
        calcGi = tintGi0;
        calcBi = tintBi0;
        calcAi = tintAi0;
        calcColor = tintColor0;
        calcAlpha = tintAlpha0;
        tintFromCalc();
      }    
      
      shininess(shininess0);  
    }    
  }
  
  protected class ModelviewStack {
    protected Stack<PMatrix3D> stack;
    protected PMatrix3D modelview;
    protected PMatrix3D camera;
    protected PMatrix3D cameraInv;
    protected PMatrix3D transform;
    
    public ModelviewStack() {
      stack = new Stack<PMatrix3D>();
      camera = new PMatrix3D();
      cameraInv = new PMatrix3D();
      transform = new PMatrix3D();
      modelview = new PMatrix3D();      
      setIdentity();
    }
    
    public void setCamera(float[] mat) {      
      camera.set(mat[0], mat[4], mat[ 8], mat[12],
                 mat[1], mat[5], mat[ 9], mat[13],
                 mat[2], mat[6], mat[10], mat[14],
                 mat[3], mat[7], mat[11], mat[15]); 
      cameraInv.set(camera);
      cameraInv.invert();
      transform.reset();
      modelview.set(camera);      
    }    
    
    public void setCamera(float m00, float m01, float m02, float m03,
                          float m10, float m11, float m12, float m13,
                          float m20, float m21, float m22, float m23,
                          float m30, float m31, float m32, float m33) {
      camera.set(m00, m01, m02, m03,
                 m10, m11, m12, m13,
                 m20, m21, m22, m23,
                 m30, m31, m32, m33);
      cameraInv.set(camera);
      cameraInv.invert();
      transform.reset();
      modelview.set(camera);   
    }
    
    
    public void setCamera(PMatrix3D cam) {
      camera.set(cam);
      cameraInv.set(cam);
      cameraInv.invert();
      transform.reset();
      modelview.set(cam);      
    }
    
    public void setIdentity() {
      camera.reset();
      cameraInv.reset();
      transform.reset();
      modelview.reset();
    }
    
    public void push() {
      PMatrix3D mat = new PMatrix3D(transform);
      stack.push(mat);
    }
    
    public void pop() {
      try {
        PMatrix3D mat = stack.pop();
        transform.set(mat);
      } catch (EmptyStackException e) {
        PGraphics.showWarning("P3D: Empty modelview stack");
      }
    }

    public void mult(float[] mat) {
      mult(mat[0], mat[4], mat[ 8], mat[12],
           mat[1], mat[5], mat[ 9], mat[13],
           mat[2], mat[6], mat[10], mat[14],
           mat[3], mat[7], mat[11], mat[15]);
    }
    
    public void mult(float n00, float n01, float n02, float n03,
                     float n10, float n11, float n12, float n13,
                     float n20, float n21, float n22, float n23,
                     float n30, float n31, float n32, float n33) {
      transform.apply(n00, n01, n02, n03,
                      n10, n11, n12, n13,
                      n20, n21, n22, n23,
                      n30, n31, n32, n33);
    }
    
    public void mult(PMatrix3D mat) {
      transform.apply(mat);
    }    
    
    public void get(float[] mat) {
      modelview.apply(transform);
      
      mat[0] = modelview.m00; mat[4] = modelview.m01; mat[ 8] = modelview.m02; mat[12] = modelview.m03;
      mat[1] = modelview.m10; mat[5] = modelview.m11; mat[ 9] = modelview.m12; mat[13] = modelview.m13;
      mat[2] = modelview.m20; mat[6] = modelview.m21; mat[10] = modelview.m22; mat[14] = modelview.m23;
      mat[3] = modelview.m30; mat[7] = modelview.m31; mat[11] = modelview.m32; mat[15] = modelview.m33;
    }

    public void get(PMatrix3D mat) {
      modelview.apply(transform);
      mat.set(modelview);      
    }
    
    public void set(float[] mat) {      
      modelview.set(mat[0], mat[4], mat[ 8], mat[12],
                    mat[1], mat[5], mat[ 9], mat[13],
                    mat[2], mat[6], mat[10], mat[14],
                    mat[3], mat[7], mat[11], mat[15]); 
      updateTransform();
    }
    
    public void set(float m00, float m01, float m02, float m03,
                    float m10, float m11, float m12, float m13,
                    float m20, float m21, float m22, float m23,
                    float m30, float m31, float m32, float m33) {
      modelview.set(m00, m01, m02, m03,
                  m10, m11, m12, m13,
                  m20, m21, m22, m23,
                  m30, m31, m32, m33);
      updateTransform();
    }
    
    public void set(PMatrix3D mat) {
      modelview.set(mat.m00, mat.m01, mat.m02, mat.m03,
                    mat.m10, mat.m11, mat.m12, mat.m13,
                    mat.m20, mat.m21, mat.m22, mat.m23,
                    mat.m30, mat.m31, mat.m32, mat.m33);
      updateTransform();
    }    
    
    public void translate(float tx, float ty, float tz) {
      transform.translate(tx, ty, tz);
    }

    public void rotate(float angle, float rx, float ry, float rz) {
      transform.rotate(angle, rx, ry, rz);
    }
    
    public void scale(float sx, float sy, float sz) {
      transform.scale(sx, sy, sz);
    }

    public PMatrix3D getTransform() {
      return transform;
    }
    
    public PMatrix3D getCamera() {
      return camera;
    }
    
    public PMatrix3D getCameraInv() {
      return cameraInv;
    }    

    public void resetTransform() {
      transform.reset();
    }    
    
    // It updates the transform matrix given the 
    // current camera and modelview. Since they are related
    // by:
    // modelview = camera * transform
    // then:
    // transform = cameraInv * modelview
    protected void updateTransform() {
      transform.set(cameraInv);
      transform.apply(modelview);      
    }
  }  

  protected class ProjectionStack {
    protected Stack<PMatrix3D> stack;
    protected PMatrix3D projection;
    
    public ProjectionStack() {
      stack = new Stack<PMatrix3D>();
      projection = new PMatrix3D();
      setIdentity();
    }
    
    public void setIdentity() {
      set(1, 0, 0, 0,
          0, 1, 0, 0,
          0, 0, 1, 0,
          0, 0, 0, 1);
    }
    
    public void push() {
      PMatrix3D mat = new PMatrix3D(projection);
      stack.push(mat);
    }
    
    public void pop() {
      try {
        PMatrix3D mat = stack.pop();
        projection.set(mat);
      } catch (EmptyStackException e) {
        PGraphics.showWarning("P3D: Empty projection stack");
      }
    }

    public void mult(float[] mat) {
      mult(mat[0], mat[4], mat[ 8], mat[12],
           mat[1], mat[5], mat[ 9], mat[13],
           mat[2], mat[6], mat[10], mat[14],
           mat[3], mat[7], mat[11], mat[15]);
    }
    
    public void mult(float n00, float n01, float n02, float n03,
                     float n10, float n11, float n12, float n13,
                     float n20, float n21, float n22, float n23,
                     float n30, float n31, float n32, float n33) {
      projection.apply(n00, n01, n02, n03,
                    n10, n11, n12, n13,
                    n20, n21, n22, n23,
                    n30, n31, n32, n33);
    }
    
    public void mult(PMatrix3D mat) {
      projection.apply(mat);
    }    
    
    public void get(float[] mat) {
      mat[0] = projection.m00; mat[4] = projection.m01; mat[ 8] = projection.m02; mat[12] = projection.m03;
      mat[1] = projection.m10; mat[5] = projection.m11; mat[ 9] = projection.m12; mat[13] = projection.m13;
      mat[2] = projection.m20; mat[6] = projection.m21; mat[10] = projection.m22; mat[14] = projection.m23;
      mat[3] = projection.m30; mat[7] = projection.m31; mat[11] = projection.m32; mat[15] = projection.m33;
    }

    public void get(PMatrix3D mat) {
      mat.set(projection);      
    }
    
    public void set(float[] mat) {
      projection.set(mat[0], mat[4], mat[ 8], mat[12],
                  mat[1], mat[5], mat[ 9], mat[13],
                  mat[2], mat[6], mat[10], mat[14],
                  mat[3], mat[7], mat[11], mat[15]); 
    }
    
    public void set(float m00, float m01, float m02, float m03,
                    float m10, float m11, float m12, float m13,
                    float m20, float m21, float m22, float m23,
                    float m30, float m31, float m32, float m33) {
      projection.set(m00, m01, m02, m03,
                  m10, m11, m12, m13,
                  m20, m21, m22, m23,
                  m30, m31, m32, m33);      
    }
    
    public void set(PMatrix3D mat) {
      projection.set(mat.m00, mat.m01, mat.m02, mat.m03,
                  mat.m10, mat.m11, mat.m12, mat.m13,
                  mat.m20, mat.m21, mat.m22, mat.m23,
                  mat.m30, mat.m31, mat.m32, mat.m33);     
    }
  }  
  
  public InGeometry newInGeometry() {
    return new InGeometry(); 
  }
  
  protected TessGeometry newTessGeometry(int mode) {
    return new TessGeometry(mode);
  }
  
  public class InGeometry {
    public int vertexCount;
    public int edgeCount;
    
    // Range of vertices that will be processed by the 
    // tessellator. They can be used in combination with the
    // edges array to have the tessellator using only a specific
    // range of vertices to generate fill geometry, while the
    // line geometry will be read from the edge vertices, which
    // could be completely different.    
    public int firstVertex;
    public int lastVertex;    

    public int firstEdge;
    public int lastEdge;    
    
    public int[] codes;
    public float[] vertices;  
    public float[] colors;
    public float[] normals;
    public float[] texcoords;
    public float[] strokes;
    
    public int[][] edges;
        
    // For later, to be used by libraries...
    //public float[][] mtexcoords;
    //public float[][] attributes;

    public InGeometry() {
      allocate();
    }    
    
    public void reset() {
      vertexCount = firstVertex = lastVertex = 0; 
      edgeCount = firstEdge = lastEdge = 0;
    }
    
    public void allocate() {      
      codes = new int[DEFAULT_IN_VERTICES];
      vertices = new float[3 * DEFAULT_IN_VERTICES];
      colors = new float[4 * DEFAULT_IN_VERTICES];      
      normals = new float[3 * DEFAULT_IN_VERTICES];
      texcoords = new float[2 * DEFAULT_IN_VERTICES];
      strokes = new float[5 * DEFAULT_IN_VERTICES];
      edges = new int[DEFAULT_IN_EDGES][3];
      reset();
    }
    
    public void dispose() {
      codes = null;
      vertices = null;
      colors = null;      
      normals = null;
      texcoords = null;
      strokes = null;  
      edges = null;
    }

    public float getVertexX(int idx) {
      return vertices[3 * idx + 0];  
    }
    
    public float getVertexY(int idx) {
      return vertices[3 * idx + 1];
    }    
    
    public float getVertexZ(int idx) {
      return vertices[3 * idx + 2];
    }    
    
    public float getlastVertexX() {
      return vertices[3 * (vertexCount - 1) + 0];  
    }
    
    public float getlastVertexY() {
      return vertices[3 * (vertexCount - 1) + 1];
    }    
    
    public float getlastVertexZ() {
      return vertices[3 * (vertexCount - 1) + 2];
    }
    
    public int addVertex(float[] vertex, float[] color, float[] normal, float[] texcoord, float[] stroke, int code) {
      vertexCheck();

      codes[vertexCount] = code;  
      
      PApplet.arrayCopy(vertex, 0, vertices, 3 * vertexCount, 3);
      PApplet.arrayCopy(color, 0, colors, 4 * vertexCount, 4);
      PApplet.arrayCopy(normal, 0, normals, 3 * vertexCount, 3);
      PApplet.arrayCopy(texcoord, 0, texcoords, 2 * vertexCount, 2);      
      PApplet.arrayCopy(stroke, 0, strokes, 5 * vertexCount, 5);
            
      lastVertex = vertexCount; 
      vertexCount++; 
      
      return lastVertex;
    }
    
    public int addVertex(float x, float y, 
                         float r, float g, float b, float a,
                         float u, float v,
                         float sr, float sg, float sb, float sa, float sw, 
                         int code) {
      return addVertex(x, y, 0, 
                       r, g, b, a, 
                       0, 0, 1, 
                       u, v, 
                       sr, sg, sb, sa, sw, 
                       code);      
    }

    public int addVertex(float x, float y, 
                         float r, float g, float b, float a,
                         float sr, float sg, float sb, float sa, float sw, 
                         int code) {
      return addVertex(x, y, 0, 
                       r, g, b, a, 
                       0, 0, 1, 
                       0, 0, 
                       sr, sg, sb, sa, sw, 
                       code);   
    }    
    
    public int addVertex(float x, float y, float z, 
                         float r, float g, float b, float a,
                         float nx, float ny, float nz,
                         float u, float v,
                         float sr, float sg, float sb, float sa, float sw, 
                         int code) {
      vertexCheck();
      int index;

      codes[vertexCount] = code;      
      
      index = 3 * vertexCount;
      vertices[index++] = x;
      vertices[index++] = y;
      vertices[index  ] = z;

      index = 4 * vertexCount;
      colors[index++] = r;
      colors[index++] = g;
      colors[index++] = b;
      colors[index  ] = a;
      
      index = 3 * vertexCount;
      normals[index++] = nx;
      normals[index++] = ny;
      normals[index  ] = ny;      
      
      index = 2 * vertexCount;
      texcoords[index++] = u;
      texcoords[index  ] = v;

      index = 5 * vertexCount;
      strokes[index++] = sr;
      strokes[index++] = sg;
      strokes[index++] = sb;
      strokes[index++] = sa;
      strokes[index  ] = sw;
      
      lastVertex = vertexCount; 
      vertexCount++;
      
      return lastVertex; 
    }
        
    public void vertexCheck() {
      if (vertexCount == vertices.length / 3) {
        int newSize = vertexCount << 1;  

        expandCodes(newSize);
        expandVertices(newSize);
        expandColors(newSize);
        expandNormals(newSize);
        expandTexcoords(newSize);      
        expandStrokes(newSize);
      }
    }  
    
    public int addEdge(int i, int j, boolean start, boolean end) {
      edgeCheck();
      
      int[] edge = edges[edgeCount];
      edge[0] = i;
      edge[1] = j;
      
      // Possible values for state:
      // 0 = middle edge (not start, not end)
      // 1 = start edge (start, not end)
      // 2 = end edge (not start, end)
      // 3 = isolated edge (start, end)      
      edge[2] = (start ? 1 : 0) + 2 * (end ? 1 : 0);
      
      lastEdge = edgeCount; 
      edgeCount++;
      
      return lastEdge;
    }
    
    public void edgeCheck() {
      if (edgeCount == edges.length) {
        int temp[][] = new int[edgeCount << 1][3];
        PApplet.arrayCopy(edges, 0, temp, 0, edgeCount);
        edges = temp;        
      }
    }
    
    protected void expandCodes(int n) {
      int temp[] = new int[n];      
      PApplet.arrayCopy(codes, 0, temp, 0, vertexCount);
      codes = temp;    
    }

    protected void expandVertices(int n) {
      float temp[] = new float[3 * n];      
      PApplet.arrayCopy(vertices, 0, temp, 0, 3 * vertexCount);
      vertices = temp;    
    }

    protected void expandColors(int n){
      float temp[] = new float[4 * n];      
      PApplet.arrayCopy(colors, 0, temp, 0, 4 * vertexCount);
      colors = temp;  
    }

    protected void expandNormals(int n) {
      float temp[] = new float[3 * n];      
      PApplet.arrayCopy(normals, 0, temp, 0, 3 * vertexCount);
      normals = temp;    
    }    
    
    protected void expandTexcoords(int n) {
      float temp[] = new float[2 * n];      
      PApplet.arrayCopy(texcoords, 0, temp, 0, 2 * vertexCount);
      texcoords = temp;    
    }
        
    protected void expandStrokes(int n) {
      float temp[] = new float[5 * n];      
      PApplet.arrayCopy(strokes, 0, temp, 0, 5 * vertexCount);
      strokes = temp;
    }
    
    public int getNumLineVertices() {
      return 4 *(lastEdge - firstEdge + 1);      
    }

    public int getNumLineIndices() {      
      return 6 *(lastEdge - firstEdge + 1);
    }    
    
    public void addTrianglesEdges() {
      for (int i = 0; i < (lastVertex - firstVertex + 1) / 3; i++) {
        int i0 = 3 * i + 0;
        int i1 = 3 * i + 1;
        int i2 = 3 * i + 2;
        
        addEdge(i0, i1,  true, false);
        addEdge(i1, i2, false, false);
        addEdge(i2, i0, false,  true);
      }
    }

    public void addTriangleFanEdges() {      
      for (int i = firstVertex + 1; i < lastVertex; i++) {
        int i0 = firstVertex;
        int i1 = i;
        int i2 = i + 1;
        
        addEdge(i0, i1,  true, false);
        addEdge(i1, i2, false, false);
        addEdge(i2, i0, false,  true);        
      }
    }
    
    public void addTriangleStripEdges() {
      for (int i = firstVertex + 1; i < lastVertex; i++) {
        int i0 = i;
        int i1, i2;
        if (i % 2 == 0) {
          i1 = i - 1;
          i2 = i + 1;        
        } else {
          i1 = i + 1;
          i2 = i - 1;        
        }
        
        addEdge(i0, i1,  true, false);
        addEdge(i1, i2, false, false);
        addEdge(i2, i0, false,  true);        
      }
    }
    
    public void addQuadsEdges() {
      for (int i = 0; i < (lastVertex - firstVertex + 1) / 4; i++) {
        int i0 = 4 * i + 0;
        int i1 = 4 * i + 1;
        int i2 = 4 * i + 2;
        int i3 = 4 * i + 3;
        
        addEdge(i0, i1,  true, false);
        addEdge(i1, i2, false, false);
        addEdge(i2, i3, false,  false);
        addEdge(i3, i0, false,  true);
      }        
    }      
      
    public void addQuadStripEdges() {
      for (int qd = 1; qd < (lastVertex - firstVertex + 1) / 2; qd++) {
        int i0 = firstVertex + 2 * (qd - 1);
        int i1 = firstVertex + 2 * (qd - 1) + 1;
        int i2 = firstVertex + 2 * qd + 1;
        int i3 = firstVertex + 2 * qd;     

        addEdge(i0, i1,  true, false);
        addEdge(i1, i2, false, false);
        addEdge(i2, i3, false,  false);
        addEdge(i3, i0, false,  true);
      }
    }
    
    public void addPolygonEdges(boolean closed) {
      // Count number of edge segments in the perimeter.      
      int edgeCount = 0;
      int lnMax = lastVertex - firstVertex + 1;
      int first = firstVertex;      
      int contour0 = first;
      if (!closed) lnMax--;
      for (int ln = 0; ln < lnMax; ln++) {
        int i = first + ln + 1;
        if ((i == lnMax || codes[i] == PShape.BREAK) && closed) {
          i = first + ln;
        }            
        if (codes[i] != PShape.BREAK) {
          edgeCount++;
        }      
      }
      
      if (0 < edgeCount) {
        boolean begin = true;
        contour0 = first;
        for (int ln = 0; ln < lnMax; ln++) {
          int i0 = first + ln;
          int i1 = first + ln + 1;
          if (codes[i0] == PShape.BREAK) contour0 = i0;
          if (i1 == lnMax || codes[i1] == PShape.BREAK) {
            // We are at the end of a contour. 
            if (closed) {
              // Draw line to the first vertex of the current contour,
              // if the polygon is closed.
              i0 = first + ln;
              i1 = contour0;            
              addEdge(i0, i1, begin, true);
            } else if (codes[i1] != PShape.BREAK) {
              addEdge(i0, i1, begin, false);
            }
            // We might start a new contour in the next iteration.
            begin = true;            
          } else if (codes[i1] != PShape.BREAK) {
            addEdge(i0, i1, begin, false);
          }
        }    
      }
    }    
    
    // Primitive generation
    
    public void generateEllipse(int ellipseMode, float a, float b, float c, float d, 
                                boolean fill, float fillR, float fillG, float fillB, float fillA, 
                                boolean stroke, float strokeR, float strokeG, float strokeB, float strokeA,
                                float strokeWeight) {      
      float x = a;
      float y = b;
      float w = c;
      float h = d;

      if (ellipseMode == CORNERS) {
        w = c - a;
        h = d - b;

      } else if (ellipseMode == RADIUS) {
        x = a - c;
        y = b - d;
        w = c * 2;
        h = d * 2;

      } else if (ellipseMode == DIAMETER) {
        x = a - c/2f;
        y = b - d/2f;
      }

      if (w < 0) {  // undo negative width
        x += w;
        w = -w;
      }

      if (h < 0) {  // undo negative height
        y += h;
        h = -h;
      }
      
      float radiusH = w / 2;
      float radiusV = h / 2;

      float centerX = x + radiusH;
      float centerY = y + radiusV;

      float sx1 = screenX(x, y);
      float sy1 = screenY(x, y);
      float sx2 = screenX(x + w, y + h);
      float sy2 = screenY(x + w, y + h);

      int accuracy = (int) (TWO_PI * PApplet.dist(sx1, sy1, sx2, sy2) / 20);
      if (accuracy < 6) {
        accuracy = 6;
      }      
      float inc = (float) PGraphicsOpenGL.SINCOS_LENGTH / accuracy;
      
      if (fill) {
        addVertex(centerX, centerY, 
                  fillR, fillG, fillB, fillA, strokeR, strokeG, strokeB, strokeA, strokeWeight, VERTEX);
      }
      int idx0, pidx, idx;
      idx0 = pidx = idx = 0;
      float val = 0;
      for (int i = 0; i < accuracy; i++) {
        idx = addVertex(centerX + PGraphicsOpenGL.cosLUT[(int) val] * radiusH, 
                        centerY + PGraphicsOpenGL.sinLUT[(int) val] * radiusV, 
                        fillR, fillG, fillB, fillA, strokeR, strokeG, strokeB, strokeA, strokeWeight, VERTEX);
        val = (val + inc) % PGraphicsOpenGL.SINCOS_LENGTH;
        
        if (0 < i) {
          if (stroke) addEdge(pidx, idx, i == 1, false);
        } else {
          idx0 = idx;  
        }
        
        pidx = idx;
      }
      // Back to the beginning
      addVertex(centerX + PGraphicsOpenGL.cosLUT[0] * radiusH, 
                centerY + PGraphicsOpenGL.sinLUT[0] * radiusV, 
                fillR, fillG, fillB, fillA, strokeR, strokeG, strokeB, strokeA, strokeWeight, VERTEX);
      if (stroke) addEdge(idx, idx0, false, true);
    }
    
  }
  
  // The big issue with TessGeometry is that for immediate mode, it is better to have the data stored in a singke
  // interleaved array:
  // float[] data = { x0, y0, z0, r0, g0, b0, a0, nx0, ny0, nz0, u0, v0, x1, y1, z1, r1, g1, b1, a1, nx1, ny1, nz1, u1, v1... 
  // because coords, colors, normals, etc. are always set at the same time, whereas for retained mode, and specially if the
  // user creates a PShape which later wants to manipulate by only changing coordinates or color, it is better to have
  // the data separated as it is currently done:
  // float[] vertices = { x0, y0, z0, x1, y1, z1...
  // float[] colors   = { r0, g0, b0, a0, r1, g1, b1, a1...
  // float[] normals  = { nx0, ny0, nz0, nx1, ny1, nz1...
  // float[] texcoords= { u0, v0, u1, v1...
  public class TessGeometry {
    int renderMode;
    
    // Tessellated fill data
    public int fillVertexCount;
    
    // Range of vertices that were generated during last
    // call to the tessellator.
    public int firstFillVertex;
    public int lastFillVertex;
    
    public float[] fillVertices;
    public float[] fillColors;
    public float[] fillNormals;
    public float[] fillTexcoords;

    
    // The structure of fillData is as follows:
    // 1) 3 floats (4 * 3 = 12 bytes) for the xyz coordinates of the vertex
    // 2) 4 floats  (4 * 4 = 16 bytes) for the rgba coordinates of the vertex
    // 3) 3 floats (4 * 3 = 12 bytes) for the xyz coordinates of the normal
    // 4) 2 floats (4 * 2 = 8 bytes) for the uv texture coordinates
    // Since up to here we have 12 + 16 + 12 + 8 = 48 bytes, and it is better make it 
    // a multiple of 32, then we allocate 16 bytes more for two more texture coordinates
    // to make for a total of 64 bytes per vertex.
    // TessGeometry should allow to set how many bytes per vertex, being the number 
    // at last 64 and a multiple of 32. This would be useful for libraries than need
    // to add additional custom attributes for each vertex.     
    public float[] fillData;
    
    
    public int fillIndexCount;
    public int firstFillIndex;
    public int lastFillIndex;    
    public int[] fillIndices;
    
    // Tessellated line data    
    public int lineVertexCount;
    public int firstLineVertex;
    public int lastLineVertex;    
    public float[] lineVertices;
    public float[] lineColors;
    public float[] lineNormals;
    public float[] lineAttributes;    
    
    
    // Likewise:
    // 3 floats (xyz) 12 byes
    // 4 floats (rgba) 16 bytes
    // 3 floats (nxyz) 12 bytes
    // 4 floats (xyzw) 16 bytes
    // 12 + 16 + 12 + 16 = 24 + 32 = 56 bytes.
    // We add padding of 8 bytes to reach 64, which can be
    // used for textured lines (an additional uv float pair per vertex).
    public float[] lineData;
    
    
    public int lineIndexCount;
    public int firstLineIndex;
    public int lastLineIndex;  
    public int[] lineIndices;  
    
    // Tessellated point data
    public int pointVertexCount;
    public int firstPointVertex;
    public int lastPointVertex;    
    public float[] pointVertices;
    public float[] pointColors;
    public float[] pointNormals;
    public float[] pointAttributes;  

    public int pointIndexCount;
    public int firstPointIndex;
    public int lastPointIndex;  
    public int[] pointIndices;
    
    public boolean isStroked;

    public TessGeometry(int mode) {
      renderMode = mode;
      allocate();      
    }    
    
    public void reset() {
      firstFillVertex = lastFillVertex = fillVertexCount = 0;
      firstFillIndex = lastFillIndex = fillIndexCount = 0;
      
      firstLineVertex = lastLineVertex = lineVertexCount = 0;
      firstLineIndex = lastLineIndex = lineIndexCount = 0;     
      
      firstPointVertex = lastPointVertex = pointVertexCount = 0;
      firstPointIndex = lastPointIndex = pointIndexCount = 0;  
      
      isStroked = false;
    }
      
    public void allocate() {     
      fillVertices = new float[3 * DEFAULT_TESS_VERTICES];
      fillColors = new float[4 * DEFAULT_TESS_VERTICES];
      fillNormals = new float[3 * DEFAULT_TESS_VERTICES];
      fillTexcoords = new float[2 * DEFAULT_TESS_VERTICES];
      fillIndices = new int[DEFAULT_TESS_VERTICES];  
      
      lineVertices = new float[3 * DEFAULT_TESS_VERTICES];
      lineColors = new float[4 * DEFAULT_TESS_VERTICES];
      lineNormals = new float[3 * DEFAULT_TESS_VERTICES];
      lineAttributes = new float[4 * DEFAULT_TESS_VERTICES];
      lineIndices = new int[DEFAULT_TESS_VERTICES];       
      
      pointVertices = new float[3 * DEFAULT_TESS_VERTICES];
      pointColors = new float[4 * DEFAULT_TESS_VERTICES];
      pointNormals = new float[3 * DEFAULT_TESS_VERTICES];
      pointAttributes = new float[2 * DEFAULT_TESS_VERTICES];
      pointIndices = new int[DEFAULT_TESS_VERTICES];
      
      reset();
    }
    
    public void dipose() {
      fillVertices = null;
      fillColors = null;
      fillNormals = null;
      fillTexcoords = null;
      fillIndices = null;  
      
      lineVertices = null;
      lineColors = null;
      lineNormals = null;
      lineAttributes = null;
      lineIndices = null;       
      
      pointVertices = null;
      pointColors = null;
      pointNormals = null;
      pointAttributes = null;
      pointIndices = null;
    }
    
    public boolean isFull() {
      return MAX_TESS_VERTICES <= fillVertexCount || 
             MAX_TESS_VERTICES <= lineVertexCount ||
             MAX_TESS_VERTICES <= pointVertexCount ||
             MAX_TESS_INDICES <= fillIndexCount ||
             MAX_TESS_INDICES <= fillIndexCount ||
             MAX_TESS_INDICES <= fillIndexCount;
    }
    
    public void addCounts(TessGeometry other) {
      fillVertexCount += other.fillVertexCount;
      fillIndexCount += other.fillIndexCount;
      
      lineVertexCount += other.lineVertexCount;
      lineIndexCount += other.lineIndexCount;        

      pointVertexCount += other.pointVertexCount;
      pointIndexCount += other.pointIndexCount;          
    }
    
    public void setFirstFill(TessGeometry other) {
      firstFillVertex = other.firstFillVertex;
      firstFillIndex = other.firstFillIndex;
    }
    
    public void setLastFill(TessGeometry other) {
      lastFillVertex = other.lastFillVertex;
      lastFillIndex = other.lastFillIndex;      
    }

    public void setFirstLine(TessGeometry other) {
      firstLineVertex = other.firstLineVertex;
      firstLineIndex = other.firstLineIndex;
    }
    
    public void setLastLine(TessGeometry other) {
      lastLineVertex = other.lastLineVertex;
      lastLineIndex = other.lastLineIndex;      
    }  

    public void setFirstPoint(TessGeometry other) {
      firstPointVertex = other.firstPointVertex;
      firstPointIndex = other.firstPointIndex;
    }
    
    public void setLastPoint(TessGeometry other) {
      lastPointVertex = other.lastPointVertex;
      lastPointIndex = other.lastPointIndex;      
    }    
    
    public int setFillVertex(int offset) {
      firstFillVertex = 0;        
      if (0 < offset) {
        firstFillVertex = offset + 1; 
      }      
      lastFillVertex = firstFillVertex + fillVertexCount - 1;      
      return lastFillVertex; 
    }
    
    public int setFillIndex(int offset) {
      firstFillIndex = 0;
      if (0 < offset) {
        firstFillIndex = offset + 1; 
      }
      
      // The indices are update to take into account all the previous 
      // shapes in the hierarchy, as the entire geometry will be stored
      // contiguously in a single VBO in the root node.
      for (int i = 0; i < fillIndexCount; i++) {
        fillIndices[i] += firstFillVertex;
      }
      lastFillIndex = firstFillIndex + fillIndexCount - 1;        
      return lastFillIndex; 
    }
    
    public int setLineVertex(int offset) {
      firstLineVertex = 0;
      if (0 < offset) {
        firstLineVertex = offset + 1; 
      }        
      lastLineVertex = firstLineVertex + lineVertexCount - 1;
      return lastLineVertex;      
    }
    
    public int setLineIndex(int offset) {      
      firstLineIndex = 0;
      if (0 < offset) {
        firstLineIndex = offset + 1; 
      }        
      
      // The indices are update to take into account all the previous 
      // shapes in the hierarchy, as the entire geometry will be stored
      // contiguously in a single VBO in the root node.
      for (int i = 0; i < lineIndexCount; i++) {
        lineIndices[i] += firstLineVertex;
      }
      lastLineIndex = firstLineIndex + lineIndexCount - 1;
      return lastLineIndex;      
    }
    
    public int setPointVertex(int offset) {
      firstPointVertex = 0;
      if (0 < offset) {
        firstPointVertex = offset + 1; 
      }        
      lastPointVertex = firstPointVertex + pointVertexCount - 1;
      return lastPointVertex;      
    }
    
    public int setPointIndex(int offset) { 
      firstPointIndex = 0;
      if (0 < offset) {
        firstPointIndex = offset + 1; 
      }        
      
      // The indices are update to take into account all the previous 
      // shapes in the hierarchy, as the entire geometry will be stored
      // contiguously in a single VBO in the root node.
      for (int i = 0; i < pointIndexCount; i++) {
        pointIndices[i] += firstPointVertex;
      }
      lastPointIndex = firstPointIndex + pointIndexCount - 1;
      return lastPointIndex;
    }
    
    public void fillIndexCheck() {
      if (fillIndexCount == fillIndices.length) {
        int newSize = fillIndexCount << 1;
        expandFillIndices(newSize);
      }
    }    
    
    public void expandFillIndices(int n) {
      int temp[] = new int[n];      
      PApplet.arrayCopy(fillIndices, 0, temp, 0, fillIndexCount);
      fillIndices = temp;      
    }
    
    public void addFillIndex(int idx) {
      fillIndexCheck();
      fillIndices[fillIndexCount] = idx;
      fillIndexCount++;
    }
    
    public void fillVertexCheck() {
      if (fillVertexCount == fillVertices.length / 3) {
        int newSize = fillVertexCount << 1; 
      
        expandFillVertices(newSize);
        expandFillColors(newSize);              
        expandFillNormals(newSize);
        expandFillTexcoords(newSize);                
      }
    }
    
    public void addFillVertices(int count) {
      int oldSize = fillVertices.length / 3;
      if (fillVertexCount + count >= oldSize) {
        int newSize = expandSize(oldSize, fillVertexCount + count); 
                
        expandFillVertices(newSize);
        expandFillColors(newSize);
        expandFillNormals(newSize);
        expandFillTexcoords(newSize);
      }
                  
      firstFillVertex = fillVertexCount;
      fillVertexCount += count;
      lastFillVertex = fillVertexCount - 1;
    }
    
    public void addFillIndices(int count) {
      int oldSize = fillIndices.length;
      if (fillIndexCount + count >= oldSize) {
        int newSize = expandSize(oldSize, fillIndexCount + count);    
        
        expandFillIndices(newSize);
      }
     
      firstFillIndex = fillIndexCount;
      fillIndexCount += count;            
      lastFillIndex = fillIndexCount - 1;   
    }     
    
    protected void expandFillVertices(int n) {
      float temp[] = new float[3 * n];      
      PApplet.arrayCopy(fillVertices, 0, temp, 0, 3 * fillVertexCount);
      fillVertices = temp;       
    }

    protected void expandFillColors(int n) {
      float temp[] = new float[4 * n];      
      PApplet.arrayCopy(fillColors, 0, temp, 0, 4 * fillVertexCount);
      fillColors = temp;
    }
    
    protected void expandFillNormals(int n) {
      float temp[] = new float[3 * n];      
      PApplet.arrayCopy(fillNormals, 0, temp, 0, 3 * fillVertexCount);
      fillNormals = temp;       
    }
    
    protected void expandFillTexcoords(int n) {
      float temp[] = new float[2 * n];      
      PApplet.arrayCopy(fillTexcoords, 0, temp, 0, 2 * fillVertexCount);
      fillTexcoords = temp;
    }
    
    public void addLineVertices(int count) {
      int oldSize = lineVertices.length / 3;
      if (lineVertexCount + count >= oldSize) {
        int newSize = expandSize(oldSize, lineVertexCount + count);
        
        expandLineVertices(newSize);
        expandLineColors(newSize);
        expandLineNormals(newSize);
        expandLineAttributes(newSize);
      }
      
      firstLineVertex = lineVertexCount;
      lineVertexCount += count;            
      lastLineVertex = lineVertexCount - 1;
    }

    public void expandLineVertices(int n) {
      float temp[] = new float[3 * n];      
      PApplet.arrayCopy(lineVertices, 0, temp, 0, 3 * lineVertexCount);
      lineVertices = temp;  
    }
    
    public void expandLineColors(int n) {
      float temp[] = new float[4 * n];      
      PApplet.arrayCopy(lineColors, 0, temp, 0, 4 * lineVertexCount);
      lineColors = temp;      
    }
    
    public void expandLineNormals(int n) {
      float temp[] = new float[3 * n];      
      PApplet.arrayCopy(lineNormals, 0, temp, 0, 3 * lineVertexCount);
      lineNormals = temp;      
    }
    
    public void expandLineAttributes(int n) {
      float temp[] = new float[4 * n];      
      PApplet.arrayCopy(lineAttributes, 0, temp, 0, 4 * lineVertexCount);
      lineAttributes = temp;      
    }      
    
    public void addLineIndices(int count) {
      int oldSize = lineIndices.length;
      if (lineIndexCount + count >= oldSize) {
        int newSize = expandSize(oldSize, lineIndexCount + count);
        
        expandLineIndices(newSize);
      }
     
      firstLineIndex = lineIndexCount;
      lineIndexCount += count;      
      lastLineIndex = lineIndexCount - 1;   
    }   
    
    public void expandLineIndices(int n) {
      int temp[] = new int[n];      
      PApplet.arrayCopy(lineIndices, 0, temp, 0, lineIndexCount);
      lineIndices = temp;        
    }
    
    public void addPointVertices(int count) {
      int oldSize = pointVertices.length / 3;
      if (pointVertexCount + count >= oldSize) {
        int newSize = expandSize(oldSize, pointVertexCount + count);
        
        expandPointVertices(newSize);
        expandPointColors(newSize);
        expandPointNormals(newSize);
        expandPointAttributes(newSize);
      }
      
      firstPointVertex = pointVertexCount;
      pointVertexCount += count;      
      lastPointVertex = pointVertexCount - 1;
    }

    public void expandPointVertices(int n) {
      float temp[] = new float[3 * n];      
      PApplet.arrayCopy(pointVertices, 0, temp, 0, 3 * pointVertexCount);
      pointVertices = temp;  
    }
    
    public void expandPointColors(int n) {
      float temp[] = new float[4 * n];      
      PApplet.arrayCopy(pointColors, 0, temp, 0, 4 * pointVertexCount);
      pointColors = temp;      
    }
    
    public void expandPointNormals(int n) {
      float temp[] = new float[3 * n];      
      PApplet.arrayCopy(pointNormals, 0, temp, 0, 3 * pointVertexCount);
      pointNormals = temp;      
    }
    
    public void expandPointAttributes(int n) {
      float temp[] = new float[2 * n];      
      PApplet.arrayCopy(pointAttributes, 0, temp, 0, 2 * pointVertexCount);
      pointAttributes = temp;      
    }
    
    public void addPointIndices(int count) {
      int oldSize = pointIndices.length;
      if (pointIndexCount + count >= oldSize) {
        int newSize = expandSize(oldSize, pointIndexCount + count);
        
        expandPointIndices(newSize);
      }
     
      firstPointIndex = pointIndexCount;
      pointIndexCount += count;      
      lastPointIndex = pointIndexCount - 1;   
    }   
    
    public void expandPointIndices(int n) {
      int temp[] = new int[n];      
      PApplet.arrayCopy(pointIndices, 0, temp, 0, pointIndexCount);
      pointIndices = temp;        
    }
    
    public void addFillVertex(float x, float y, float z, float r, 
                              float g, float b, float a,
                              float nx, float ny, float nz, 
                              float u, float v) {
      fillVertexCheck();
      int index;
      
      if (renderMode == IMMEDIATE && flushMode == FLUSH_WHEN_FULL) {
        PMatrix3D tr = modelviewStack.getTransform();
        
        index = 3 * fillVertexCount;
        fillVertices[index++] = x * tr.m00 + y * tr.m01 + z * tr.m02 + tr.m03;
        fillVertices[index++] = x * tr.m10 + y * tr.m11 + z * tr.m12 + tr.m13;
        fillVertices[index  ] = x * tr.m20 + y * tr.m21 + z * tr.m22 + tr.m23;
        
        index = 3 * fillVertexCount;
        fillNormals[index++] = nx * tr.m00 + ny * tr.m01 + nz * tr.m02;
        fillNormals[index++] = nx * tr.m10 + ny * tr.m11 + nz * tr.m12;
        fillNormals[index  ] = nx * tr.m20 + ny * tr.m21 + nz * tr.m22;
      } else {
        index = 3 * fillVertexCount;
        fillVertices[index++] = x;
        fillVertices[index++] = y;
        fillVertices[index  ] = z;

        index = 3 * fillVertexCount;
        fillNormals[index++] = nx;
        fillNormals[index++] = ny;
        fillNormals[index  ] = nz;        
      }
      
      index = 4 * fillVertexCount;
      fillColors[index++] = r;
      fillColors[index++] = g;
      fillColors[index++] = b;
      fillColors[index  ] = a;
      
      index = 2 * fillVertexCount;
      fillTexcoords[index++] = u;
      fillTexcoords[index  ] = v;      
      
      fillVertexCount++;
    }    

    public void addFillVertices(InGeometry in) {
      int index;
      int i0 = in.firstVertex;
      int i1 = in.lastVertex;
      int nvert = i1 - i0 + 1;
      
      addFillVertices(nvert);
      
      if (renderMode == IMMEDIATE && flushMode == FLUSH_WHEN_FULL) {
        PMatrix3D tr = modelviewStack.getTransform();
        
        for (int i = 0; i < nvert; i++) {
          int inIdx = i0 + i;
          int tessIdx = firstFillVertex + i;
          
          index = 3 * inIdx;
          float x = in.vertices[index++];
          float y = in.vertices[index++];
          float z = in.vertices[index  ];
          
          index = 3 * inIdx;
          float nx = in.normals[index++];
          float ny = in.normals[index++];
          float nz = in.normals[index  ];
          
          index = 3 * tessIdx;
          fillVertices[index++] = x * tr.m00 + y * tr.m01 + z * tr.m02 + tr.m03;
          fillVertices[index++] = x * tr.m10 + y * tr.m11 + z * tr.m12 + tr.m13;
          fillVertices[index  ] = x * tr.m20 + y * tr.m21 + z * tr.m22 + tr.m23;
          
          index = 3 * tessIdx;
          fillNormals[index++] = nx * tr.m00 + ny * tr.m01 + nz * tr.m02;
          fillNormals[index++] = nx * tr.m10 + ny * tr.m11 + nz * tr.m12;
          fillNormals[index  ] = nx * tr.m20 + ny * tr.m21 + nz * tr.m22;
        }        
      } else {
        if (nvert < MIN_ARRAYCOPY_SIZE) {
          // Copying elements one by one instead of using arrayCopy is more efficient for
          // few vertices...
          for (int i = 0; i < nvert; i++) {
            int inIdx = i0 + i;
            int tessIdx = firstFillVertex + i;

            index = 3 * inIdx;
            float x = in.vertices[index++];
            float y = in.vertices[index++];
            float z = in.vertices[index  ];
            
            index = 3 * inIdx;
            float nx = in.normals[index++];
            float ny = in.normals[index++];
            float nz = in.normals[index  ];
            
            index = 3 * tessIdx;
            fillVertices[index++] = x;
            fillVertices[index++] = y;
            fillVertices[index  ] = z;
            
            index = 3 * tessIdx;
            fillNormals[index++] = nx;
            fillNormals[index++] = ny;
            fillNormals[index  ] = nz;            
          }     
        } else {          
          PApplet.arrayCopy(in.vertices, 3 * i0, fillVertices, 3 * firstFillVertex, 3 * nvert);
          PApplet.arrayCopy(in.normals, 3 * i0, fillNormals, 3 * firstFillVertex, 3 * nvert);                  
        }
      }
        
      if (nvert < MIN_ARRAYCOPY_SIZE) {
        for (int i = 0; i < nvert; i++) {
          int inIdx = i0 + i;
          int tessIdx = firstFillVertex + i;

          index = 4 * inIdx;
          float r = in.colors[index++];
          float g = in.colors[index++];
          float b = in.colors[index++];
          float a = in.colors[index  ];
          
          index = 2 * inIdx;
          float u = in.texcoords[index++];
          float v = in.texcoords[index  ];
          
          index = 4 * tessIdx;
          fillColors[index++] = r;
          fillColors[index++] = g;
          fillColors[index++] = b;
          fillColors[index  ] = a;
          
          index = 2 * tessIdx;
          fillNormals[index++] = u;
          fillNormals[index  ] = v;            
        }
      } else {
        PApplet.arrayCopy(in.colors, 4 * i0, fillColors, 4 * firstFillVertex, 4 * nvert);      
        PApplet.arrayCopy(in.texcoords, 2 * i0, fillTexcoords, 2 * firstFillVertex, 2 * nvert);
      }
    }     
    
    public void putLineVertex(InGeometry in, int inIdx0, int inIdx1, int tessIdx, 
                              float sr, float sg, float sb, float sa) {
      int index;

      index = 3 * inIdx0;
      float x0 = in.vertices[index++];
      float y0 = in.vertices[index++];
      float z0 = in.vertices[index  ];
      
      index = 3 * inIdx0;
      float nx = in.normals[index++];
      float ny = in.normals[index++];
      float nz = in.normals[index  ];      

      index = 3 * inIdx1;
      float x1 = in.vertices[index++];
      float y1 = in.vertices[index++];
      float z1 = in.vertices[index  ];        
      
      if (renderMode == IMMEDIATE && flushMode == FLUSH_WHEN_FULL) {
        PMatrix3D tr = modelviewStack.getTransform();
        
        index = 3 * tessIdx;
        lineVertices[index++] = x0 * tr.m00 + y0 * tr.m01 + z0 * tr.m02 + tr.m03;
        lineVertices[index++] = x0 * tr.m10 + y0 * tr.m11 + z0 * tr.m12 + tr.m13;
        lineVertices[index  ] = x0 * tr.m20 + y0 * tr.m21 + z0 * tr.m22 + tr.m23;
        
        index = 3 * tessIdx;
        lineNormals[index++] = nx * tr.m00 + ny * tr.m01 + nz * tr.m02;
        lineNormals[index++] = nx * tr.m10 + ny * tr.m11 + nz * tr.m12;
        lineNormals[index  ] = nx * tr.m20 + ny * tr.m21 + nz * tr.m22;

        index = 4 * tessIdx;
        lineAttributes[index++] = x1 * tr.m00 + y1 * tr.m01 + z1 * tr.m02 + tr.m03;
        lineAttributes[index++] = x1 * tr.m10 + y1 * tr.m11 + z1 * tr.m12 + tr.m13;
        lineAttributes[index  ] = x1 * tr.m20 + y1 * tr.m21 + z1 * tr.m22 + tr.m23;        
      } else {
        index = 3 * tessIdx;
        lineVertices[index++] = x0;
        lineVertices[index++] = y0;
        lineVertices[index  ] = z0;
        
        index = 3 * tessIdx;
        lineNormals[index++] = nx;
        lineNormals[index++] = ny;
        lineNormals[index  ] = nz;

        index = 4 * tessIdx;
        lineAttributes[index++] = x1;
        lineAttributes[index++] = y1;
        lineAttributes[index  ] = z1;
      }      
      
      index = 4 * tessIdx;
      lineColors[index++] = sr;
      lineColors[index++] = sg;
      lineColors[index++] = sb;
      lineColors[index  ] = sa;
    }

    public void putLineVertex(InGeometry in, int inIdx0, int inIdx1, int tessIdx) {      
      int index = 5 * inIdx0;
      float r = in.strokes[index++];
      float g = in.strokes[index++];
      float b = in.strokes[index++];
      float a = in.strokes[index  ];
      putLineVertex(in, inIdx0, inIdx1, tessIdx, r, g, b, a);
    }    
    
    
    public void putLineVertexIntoFill(InGeometry in, int inIdx, float dispX, float dispY, int tessIdx, 
                                      float sr, float sg, float sb, float sa) {
      int index;

      index = 3 * inIdx;
      float x = in.vertices[index++] + dispX;
      float y = in.vertices[index++] + dispY;
      float z = in.vertices[index  ];
      
      index = 3 * inIdx;
      float nx = in.normals[index++];
      float ny = in.normals[index++];
      float nz = in.normals[index  ];      
      
      if (renderMode == IMMEDIATE && flushMode == FLUSH_WHEN_FULL) {
        PMatrix3D tr = modelviewStack.getTransform();
        
        index = 3 * tessIdx;
        fillVertices[index++] = x * tr.m00 + y * tr.m01 + z * tr.m02 + tr.m03;
        fillVertices[index++] = x * tr.m10 + y * tr.m11 + z * tr.m12 + tr.m13;
        fillVertices[index  ] = x * tr.m20 + y * tr.m21 + z * tr.m22 + tr.m23;
        
        index = 3 * tessIdx;
        fillNormals[index++] = nx * tr.m00 + ny * tr.m01 + nz * tr.m02;
        fillNormals[index++] = nx * tr.m10 + ny * tr.m11 + nz * tr.m12;
        fillNormals[index  ] = nx * tr.m20 + ny * tr.m21 + nz * tr.m22;
      } else {
        index = 3 * tessIdx;
        fillVertices[index++] = x;
        fillVertices[index++] = y;
        fillVertices[index  ] = z;
        
        index = 3 * tessIdx;
        fillNormals[index++] = nx;
        fillNormals[index++] = ny;
        fillNormals[index  ] = nz;
      }      
      
      index = 4 * tessIdx;
      fillColors[index++] = sr;
      fillColors[index++] = sg;
      fillColors[index++] = sb;
      fillColors[index  ] = sa;
    }    
    
    
    public void putLineVertexIntoFill(InGeometry in, int inIdx, float dx, float dy, int tessIdx) {      
      int index = 5 * inIdx;
      float r = in.strokes[index++];
      float g = in.strokes[index++];
      float b = in.strokes[index++];
      float a = in.strokes[index  ];
      putLineVertexIntoFill(in, inIdx, dx, dy, tessIdx, r, g, b, a);
    }
    
    public void putPointVertex(InGeometry in, int inIdx, int tessIdx) {
      int index;

      index = 3 * inIdx;
      float x = in.vertices[index++];
      float y = in.vertices[index++];
      float z = in.vertices[index ];
      
      index = 3 * inIdx;
      float nx = in.normals[index++];
      float ny = in.normals[index++];
      float nz = in.normals[index  ];      
      
      if (renderMode == IMMEDIATE && flushMode == FLUSH_WHEN_FULL) {
        PMatrix3D tr = modelviewStack.getTransform();

        index = 3 * tessIdx;
        pointVertices[index++] = x * tr.m00 + y * tr.m01 + z * tr.m02 + tr.m03;
        pointVertices[index++] = x * tr.m10 + y * tr.m11 + z * tr.m12 + tr.m13;
        pointVertices[index  ] = x * tr.m20 + y * tr.m21 + z * tr.m22 + tr.m23;
        
        index = 3 * tessIdx;
        pointNormals[index++] = nx * tr.m00 + ny * tr.m01 + nz * tr.m02;
        pointNormals[index++] = nx * tr.m10 + ny * tr.m11 + nz * tr.m12;
        pointNormals[index  ] = nx * tr.m20 + ny * tr.m21 + nz * tr.m22;
      } else {
        index = 3 * tessIdx;
        pointVertices[index++] = x;
        pointVertices[index++] = y;
        pointVertices[index  ] = z;
        
        index = 3 * tessIdx;
        pointNormals[index++] = nx;
        pointNormals[index++] = ny;
        pointNormals[index  ] = nz;        
      }      
      
      index = 5 * inIdx;
      float r = in.strokes[index++];
      float g = in.strokes[index++];
      float b = in.strokes[index++];
      float a = in.strokes[index  ];
      
      index = 4 * tessIdx;
      pointColors[index++] = r;
      pointColors[index++] = g;
      pointColors[index++] = b;
      pointColors[index  ] = a;      
    }
    
    public int expandSize(int currSize, int newMinSize) {
      int newSize = currSize; 
      while (newSize < newMinSize) {
        newSize = newSize << 1;
      }
      return newSize;
    }
    
    public void applyMatrix(PMatrix2D tr) {
      
    }
    
    public void applyMatrix(PMatrix3D tr) {
      if (0 < fillVertexCount) {
        int index;
          
        for (int i = 0; i < fillVertexCount; i++) {
          index = 3 * i;
          float x = fillVertices[index++];
          float y = fillVertices[index++];
          float z = fillVertices[index  ];
        
          index = 3 * i;
          float nx = fillNormals[index++];
          float ny = fillNormals[index++];
          float nz = fillNormals[index  ];

          index = 3 * i;
          fillVertices[index++] = x * tr.m00 + y * tr.m01 + z * tr.m02 + tr.m03;
          fillVertices[index++] = x * tr.m10 + y * tr.m11 + z * tr.m12 + tr.m13;
          fillVertices[index  ] = x * tr.m20 + y * tr.m21 + z * tr.m22 + tr.m23;
        
          index = 3 * i;
          fillNormals[index++] = nx * tr.m00 + ny * tr.m01 + nz * tr.m02;
          fillNormals[index++] = nx * tr.m10 + ny * tr.m11 + nz * tr.m12;
          fillNormals[index  ] = nx * tr.m20 + ny * tr.m21 + nz * tr.m22;          
        }
      }

      if (0 < lineVertexCount) {
        int index;
        
        for (int i = 0; i < lineVertexCount; i++) {
          index = 3 * i;
          float x = lineVertices[index++];
          float y = lineVertices[index++];
          float z = lineVertices[index  ];
        
          index = 3 * i;
          float nx = lineNormals[index++];
          float ny = lineNormals[index++];
          float nz = lineNormals[index  ];

          index = 4 * i;
          float xa = lineAttributes[index++];
          float ya = lineAttributes[index++];
          float za = lineAttributes[index  ];
                    
          index = 3 * i;
          lineVertices[index++] = x * tr.m00 + y * tr.m01 + z * tr.m02 + tr.m03;
          lineVertices[index++] = x * tr.m10 + y * tr.m11 + z * tr.m12 + tr.m13;
          lineVertices[index  ] = x * tr.m20 + y * tr.m21 + z * tr.m22 + tr.m23;
        
          index = 3 * i;
          lineNormals[index++] = nx * tr.m00 + ny * tr.m01 + nz * tr.m02;
          lineNormals[index++] = nx * tr.m10 + ny * tr.m11 + nz * tr.m12;
          lineNormals[index  ] = nx * tr.m20 + ny * tr.m21 + nz * tr.m22;
          
          index = 4 * i;
          lineAttributes[index++] = xa * tr.m00 + ya * tr.m01 + za * tr.m02 + tr.m03;
          lineAttributes[index++] = xa * tr.m10 + ya * tr.m11 + za * tr.m12 + tr.m13;
          lineAttributes[index  ] = xa * tr.m20 + ya * tr.m21 + za * tr.m22 + tr.m23;              
        }   
      }      
      
      if (0 < pointVertexCount) {
        int index;
       
        for (int i = 0; i < pointVertexCount; i++) {
          index = 3 * i;
          float x = pointVertices[index++];
          float y = pointVertices[index++];
          float z = pointVertices[index  ];
        
          index = 3 * i;
          float nx = pointNormals[index++];
          float ny = pointNormals[index++];
          float nz = pointNormals[index  ];
                    
          index = 3 * i;
          pointVertices[index++] = x * tr.m00 + y * tr.m01 + z * tr.m02 + tr.m03;
          pointVertices[index++] = x * tr.m10 + y * tr.m11 + z * tr.m12 + tr.m13;
          pointVertices[index  ] = x * tr.m20 + y * tr.m21 + z * tr.m22 + tr.m23;
          
          index = 3 * i;
          pointNormals[index++] = nx * tr.m00 + ny * tr.m01 + nz * tr.m02;
          pointNormals[index++] = nx * tr.m10 + ny * tr.m11 + nz * tr.m12;
          pointNormals[index  ] = nx * tr.m20 + ny * tr.m21 + nz * tr.m22;
        } 
      }
      
    }    
  }

  final static protected int MIN_ACCURACY = 6; 
  final static protected float sinLUT[];
  final static protected float cosLUT[];
  final static protected float SINCOS_PRECISION = 0.5f;
  final static protected int SINCOS_LENGTH = (int) (360f / SINCOS_PRECISION);
  static {
    sinLUT = new float[SINCOS_LENGTH];
    cosLUT = new float[SINCOS_LENGTH];
    for (int i = 0; i < SINCOS_LENGTH; i++) {
      sinLUT[i] = (float) Math.sin(i * DEG_TO_RAD * SINCOS_PRECISION);
      cosLUT[i] = (float) Math.cos(i * DEG_TO_RAD * SINCOS_PRECISION);
    }      
  }  
  final protected float[][] QUAD_SIGNS = { {-1, +1}, {-1, -1}, {+1, -1}, {+1, +1} };
  
  public class Tessellator {
    public GLUtessellator gluTess;
    InGeometry inGeo; 
    TessGeometry tessGeo;
    GLU glu;
    
    boolean fill;
    boolean stroke;
    float strokeWeight;
    int strokeJoin;
    int strokeCap;
    float strokeRed, strokeGreen, strokeBlue, strokeAlpha;
    int bezierDetil = 20;
    
    boolean is3D;
    
    public Tessellator() {
      glu = new GLU();
      
      gluTess = GLU.gluNewTess();
      GLUTessCallback tessCallback;
    
      tessCallback = new GLUTessCallback();
      GLU.gluTessCallback(gluTess, GLU.GLU_TESS_BEGIN, tessCallback);
      GLU.gluTessCallback(gluTess, GLU.GLU_TESS_END, tessCallback);
      GLU.gluTessCallback(gluTess, GLU.GLU_TESS_VERTEX, tessCallback);
      GLU.gluTessCallback(gluTess, GLU.GLU_TESS_COMBINE, tessCallback);
      GLU.gluTessCallback(gluTess, GLU.GLU_TESS_ERROR, tessCallback);    
      
      bezierDetil = 20;
      is3D = true;
    }

    public void setInGeometry(InGeometry in) {
      this.inGeo = in;
    }

    public void setTessGeometry(TessGeometry tess) {
      this.tessGeo = tess;
    }
    
    public void set2D() {
      is3D = false;  
    }    
    
    public void set3D() {
      is3D = true;  
    }
    
    public void setFill(boolean fill) {
      this.fill = fill;
    }    
    
    public void setStroke(boolean stroke) {
      this.stroke = stroke;
    }
    
    public void setStrokeWeight(float weight) {
      this.strokeWeight = weight;
    }
    
    public void setStrokeJoin(int strokeJoin) { 
      this.strokeJoin = strokeJoin;
    }
    
    public void setStrokeCap(int strokeCap) { 
      this.strokeCap = strokeCap;
    }
    
    public void setStrokeColor(float r, float g, float b, float a) {
      this.strokeRed = r;
      this.strokeGreen = g; 
      this.strokeBlue = b;
      this.strokeAlpha = a;  
    }
        
    public void tessellatePoints() {
      if (strokeCap == ROUND) {
        tessellateRoundPoints();
      } else {
        tessellateSquarePoints();
      }
    }    

    protected void tessellateRoundPoints() {
      int nInVert = inGeo.lastVertex - inGeo.firstVertex + 1;
      
      if (stroke && 1 <= nInVert) {
        tessGeo.isStroked = true;
        
        if (is3D) {
          // Each point generates a separate triangle fan. 
          // The number of triangles of each fan depends on the
          // stroke weight of the point.
          int nvertTot = 0;
          int nindTot = 0;
          for (int i = inGeo.firstVertex; i <= inGeo.lastVertex; i++) {
            int perim = PApplet.max(MIN_ACCURACY, (int) (TWO_PI * strokeWeight / 20));
            // Number of points along the perimeter plus the center point.
            int nvert = perim + 1;
            nvertTot += nvert; 
            nindTot += 3 * (nvert - 1);
          }
          
          tessGeo.addPointVertices(nvertTot);
          tessGeo.addPointIndices(nindTot);
          int vertIdx = 3 * tessGeo.firstPointVertex;
          int attribIdx = 2 * tessGeo.firstPointVertex;
          int indIdx = tessGeo.firstPointIndex;      
          int firstVert = tessGeo.firstPointVertex;      
          for (int i = inGeo.firstVertex; i <= inGeo.lastVertex; i++) {
            // Creating the triangle fan for each input vertex.
            int perim = PApplet.max(MIN_ACCURACY, (int) (TWO_PI * strokeWeight / 20));
            int nvert = perim + 1;
            
            // All the tessellated vertices are identical to the center point
            for (int k = 0; k < nvert; k++) {
              tessGeo.putPointVertex(inGeo, i, vertIdx);
              vertIdx++; 
            }       
            
            // The attributes for each tessellated vertex are the displacement along
            // the circle perimeter. The point shader will read these attributes and
            // displace the vertices in screen coordinates so the circles are always
            // camera facing (bilboards)
            tessGeo.pointAttributes[2 * attribIdx + 0] = 0;
            tessGeo.pointAttributes[2 * attribIdx + 1] = 0;
            attribIdx++;
            float val = 0;
            float inc = (float) SINCOS_LENGTH / perim;      
            for (int k = 0; k < perim; k++) {
              tessGeo.pointAttributes[2 * attribIdx + 0] = 0.5f * cosLUT[(int) val] * strokeWeight;
              tessGeo.pointAttributes[2 * attribIdx + 1] = 0.5f * sinLUT[(int) val] * strokeWeight;
              val = (val + inc) % SINCOS_LENGTH;                
              attribIdx++;           
            }
            
            // Adding vert0 to take into account the triangles of all
            // the preceding points.
            for (int k = 1; k < nvert - 1; k++) {
              tessGeo.pointIndices[indIdx++] = firstVert + 0;
              tessGeo.pointIndices[indIdx++] = firstVert + k;
              tessGeo.pointIndices[indIdx++] = firstVert + k + 1;
            }
            // Final triangle between the last and first point:
            tessGeo.pointIndices[indIdx++] = firstVert + 0;
            tessGeo.pointIndices[indIdx++] = firstVert + 1;
            tessGeo.pointIndices[indIdx++] = firstVert + nvert - 1;      
            
            firstVert = vertIdx;
          }          
        } else {
          // Same as in 3D, but the geometry is stored in the fill arrays
          
          // Each point generates a separate triangle fan. 
          // The number of triangles of each fan depends on the
          // stroke weight of the point.
          int nvertTot = 0;
          int nindTot = 0;
          for (int i = inGeo.firstVertex; i <= inGeo.lastVertex; i++) {
            int perim = PApplet.max(MIN_ACCURACY, (int) (TWO_PI * strokeWeight / 20));
            // Number of points along the perimeter plus the center point.
            int nvert = perim + 1;
            nvertTot += nvert; 
            nindTot += 3 * (nvert - 1);
          }
          
          PMatrix3D tr = modelviewStack.getTransform();
          
          tessGeo.addFillVertices(nvertTot);
          tessGeo.addFillIndices(nindTot);
          int vertIdx = 3 * tessGeo.firstFillVertex;
          int indIdx = tessGeo.firstFillIndex;      
          int firstVert = tessGeo.firstFillVertex;      
          for (int i = inGeo.firstVertex; i <= inGeo.lastVertex; i++) {
            // Creating the triangle fan for each input vertex.
            int perim = PApplet.max(MIN_ACCURACY, (int) (TWO_PI * strokeWeight / 20));
            int nvert = perim + 1;
            
            // Center point (assuming z = 0).
            float x0 = inGeo.vertices[3 * i + 0];
            float y0 = inGeo.vertices[3 * i + 1];            
            
            tessGeo.fillVertices[3 * vertIdx + 0] = x0 * tr.m00 + y0 * tr.m01 + tr.m03;
            tessGeo.fillVertices[3 * vertIdx + 1] = x0 * tr.m10 + y0 * tr.m11 + tr.m13;
            tessGeo.fillVertices[3 * vertIdx + 2] = 0;            
            tessGeo.fillColors[4 * vertIdx + 0] = strokeRed; 
            tessGeo.fillColors[4 * vertIdx + 1] = strokeGreen;
            tessGeo.fillColors[4 * vertIdx + 2] = strokeBlue;
            tessGeo.fillColors[4 * vertIdx + 3] = strokeAlpha;            
            tessGeo.fillNormals[3 * vertIdx + 0] = 0;            
            tessGeo.fillNormals[3 * vertIdx + 1] = 0;
            tessGeo.fillNormals[3 * vertIdx + 2] = 1;
            tessGeo.fillTexcoords[2 * vertIdx + 0] = 0;            
            tessGeo.fillTexcoords[2 * vertIdx + 1] = 0;            
            vertIdx++;
                        
            float val = 0;
            float inc = (float) SINCOS_LENGTH / perim;      
            for (int k = 0; k < perim; k++) {
              float x1 = x0 + 0.5f * cosLUT[(int) val] * strokeWeight;
              float y1 = y0 + 0.5f * sinLUT[(int) val] * strokeWeight;
              
              tessGeo.fillVertices[3 * vertIdx + 0] = x1 * tr.m00 + y1 * tr.m01 + tr.m03;
              tessGeo.fillVertices[3 * vertIdx + 1] = x1 * tr.m10 + y1 * tr.m11 + tr.m13;
              tessGeo.fillVertices[3 * vertIdx + 2] = 0;
              val = (val + inc) % SINCOS_LENGTH;
              
              tessGeo.fillColors[4 * vertIdx + 0] = strokeRed; 
              tessGeo.fillColors[4 * vertIdx + 1] = strokeGreen;
              tessGeo.fillColors[4 * vertIdx + 2] = strokeBlue;
              tessGeo.fillColors[4 * vertIdx + 3] = strokeAlpha;            
              tessGeo.fillNormals[3 * vertIdx + 0] = 0;            
              tessGeo.fillNormals[3 * vertIdx + 1] = 0;
              tessGeo.fillNormals[3 * vertIdx + 2] = 1;
              tessGeo.fillTexcoords[2 * vertIdx + 0] = 0;            
              tessGeo.fillTexcoords[2 * vertIdx + 1] = 0;
              
              vertIdx++;           
            }
            
            // Adding vert0 to take into account the triangles of all
            // the preceding points.
            for (int k = 1; k < nvert - 1; k++) {
              tessGeo.fillIndices[indIdx++] = firstVert + 0;
              tessGeo.fillIndices[indIdx++] = firstVert + k;
              tessGeo.fillIndices[indIdx++] = firstVert + k + 1;
            }
            // Final triangle between the last and first point:
            tessGeo.fillIndices[indIdx++] = firstVert + 0;
            tessGeo.fillIndices[indIdx++] = firstVert + 1;
            tessGeo.fillIndices[indIdx++] = firstVert + nvert - 1;      
            
            firstVert = vertIdx;
          }          
        }
      }
    }
    
    protected void tessellateSquarePoints() {
      int nInVert = inGeo.lastVertex - inGeo.firstVertex + 1;
      
      if (stroke && 1 <= nInVert) {
        tessGeo.isStroked = true;
        
        if (is3D) {
          // Each point generates a separate quad.
          int quadCount = nInVert;
          
          // Each quad is formed by 5 vertices, the center one
          // is the input vertex, and the other 4 define the 
          // corners (so, a triangle fan again).
          int nvertTot = 5 * quadCount;
          // So the quad is formed by 4 triangles, each requires
          // 3 indices.
          int nindTot = 12 * quadCount;
          
          tessGeo.addPointVertices(nvertTot);
          tessGeo.addPointIndices(nindTot);
          int vertIdx = 3 * tessGeo.firstPointVertex;
          int attribIdx = 2 * tessGeo.firstPointVertex;
          int indIdx = tessGeo.firstPointIndex;      
          int firstVert = tessGeo.firstPointVertex;      
          for (int i = inGeo.firstVertex; i <= inGeo.lastVertex; i++) {
            int nvert = 5;
            
            for (int k = 0; k < nvert; k++) {
              tessGeo.putPointVertex(inGeo, i, vertIdx);
              vertIdx++; 
            }       
            
            // The attributes for each tessellated vertex are the displacement along
            // the quad corners. The point shader will read these attributes and
            // displace the vertices in screen coordinates so the quads are always
            // camera facing (bilboards)
            tessGeo.pointAttributes[2 * attribIdx + 0] = 0;
            tessGeo.pointAttributes[2 * attribIdx + 1] = 0;
            attribIdx++;            
            for (int k = 0; k < 4; k++) {
              tessGeo.pointAttributes[2 * attribIdx + 0] = 0.5f * QUAD_SIGNS[k][0] * strokeWeight;
              tessGeo.pointAttributes[2 * attribIdx + 1] = 0.5f * QUAD_SIGNS[k][1] * strokeWeight;               
              attribIdx++;           
            }
            
            // Adding firstVert to take into account the triangles of all
            // the preceding points.
            for (int k = 1; k < nvert - 1; k++) {
              tessGeo.pointIndices[indIdx++] = firstVert + 0;
              tessGeo.pointIndices[indIdx++] = firstVert + k;
              tessGeo.pointIndices[indIdx++] = firstVert + k + 1;
            }
            // Final triangle between the last and first point:
            tessGeo.pointIndices[indIdx++] = firstVert + 0;
            tessGeo.pointIndices[indIdx++] = firstVert + 1;
            tessGeo.pointIndices[indIdx++] = firstVert + nvert - 1;  
            
            firstVert = vertIdx;      
          }    
        } else {
          // Same as in 3D, but the geometry is stored in the fill arrays
                    
          // Each point generates a separate quad.
          int quadCount = nInVert;
          
          // Each quad is formed by 5 vertices, the center one
          // is the input vertex, and the other 4 define the 
          // corners (so, a triangle fan again).
          int nvertTot = 5 * quadCount;
          // So the quad is formed by 4 triangles, each requires
          // 3 indices.
          int nindTot = 12 * quadCount;
          
          PMatrix3D tr = modelviewStack.getTransform();
          
          tessGeo.addFillVertices(nvertTot);
          tessGeo.addFillIndices(nindTot);
          int vertIdx = 3 * tessGeo.firstFillVertex;
          int indIdx = tessGeo.firstFillIndex;      
          int firstVert = tessGeo.firstFillVertex;      
          for (int i = inGeo.firstVertex; i <= inGeo.lastVertex; i++) {
            int nvert = 5;
            
            // Center point (assuming z = 0).
            float x0 = inGeo.vertices[3 * i + 0];
            float y0 = inGeo.vertices[3 * i + 1];       
            
            tessGeo.fillVertices[3 * vertIdx + 0] = x0 * tr.m00 + y0 * tr.m01 + tr.m03;
            tessGeo.fillVertices[3 * vertIdx + 1] = x0 * tr.m10 + y0 * tr.m11 + tr.m13;
            tessGeo.fillVertices[3 * vertIdx + 2] = 0;            
            tessGeo.fillColors[4 * vertIdx + 0] = strokeRed; 
            tessGeo.fillColors[4 * vertIdx + 1] = strokeGreen;
            tessGeo.fillColors[4 * vertIdx + 2] = strokeBlue;
            tessGeo.fillColors[4 * vertIdx + 3] = strokeAlpha;            
            tessGeo.fillNormals[3 * vertIdx + 0] = 0;            
            tessGeo.fillNormals[3 * vertIdx + 1] = 0;
            tessGeo.fillNormals[3 * vertIdx + 2] = 1;
            tessGeo.fillTexcoords[2 * vertIdx + 0] = 0;            
            tessGeo.fillTexcoords[2 * vertIdx + 1] = 0;            
            vertIdx++;
           
            for (int k = 0; k < 4; k++) {
              float x1 = x0 + 0.5f * QUAD_SIGNS[k][0] * strokeWeight;
              float y1 = y0 + 0.5f * QUAD_SIGNS[k][1] * strokeWeight;               
              
              tessGeo.fillVertices[3 * vertIdx + 0] = x1 * tr.m00 + y1 * tr.m01 + tr.m03;
              tessGeo.fillVertices[3 * vertIdx + 1] = x1 * tr.m10 + y1 * tr.m11 + tr.m13;
              tessGeo.fillVertices[3 * vertIdx + 2] = 0;            
              tessGeo.fillColors[4 * vertIdx + 0] = strokeRed; 
              tessGeo.fillColors[4 * vertIdx + 1] = strokeGreen;
              tessGeo.fillColors[4 * vertIdx + 2] = strokeBlue;
              tessGeo.fillColors[4 * vertIdx + 3] = strokeAlpha;            
              tessGeo.fillNormals[3 * vertIdx + 0] = 0;            
              tessGeo.fillNormals[3 * vertIdx + 1] = 0;
              tessGeo.fillNormals[3 * vertIdx + 2] = 1;
              tessGeo.fillTexcoords[2 * vertIdx + 0] = 0;            
              tessGeo.fillTexcoords[2 * vertIdx + 1] = 0;  
              
              vertIdx++;           
            }
            
            // Adding firstVert to take into account the triangles of all
            // the preceding points.
            for (int k = 1; k < nvert - 1; k++) {
              tessGeo.fillIndices[indIdx++] = firstVert + 0;
              tessGeo.fillIndices[indIdx++] = firstVert + k;
              tessGeo.fillIndices[indIdx++] = firstVert + k + 1;
            }
            // Final triangle between the last and first point:
            tessGeo.fillIndices[indIdx++] = firstVert + 0;
            tessGeo.fillIndices[indIdx++] = firstVert + 1;
            tessGeo.fillIndices[indIdx++] = firstVert + nvert - 1;  
            
            firstVert = vertIdx;      
          }          
        }
      }
    }
    
    public void tessellateLines() {
      int nInVert = inGeo.lastVertex - inGeo.firstVertex + 1;
      
      if (stroke && 2 <= nInVert) {
        tessGeo.isStroked = true;
        
        int lineCount = nInVert / 2;
        int first = inGeo.firstVertex;
        if (is3D) {
          // Lines are made up of 4 vertices defining the quad. 
          // Each vertex has its own offset representing the stroke weight.
          int nvert = lineCount * 4;
          // Each stroke line has 4 vertices, defining 2 triangles, which
          // require 3 indices to specify their connectivities.
          int nind = lineCount * 2 * 3;

          tessGeo.addLineVertices(nvert);
          tessGeo.addLineIndices(nind);      
          int vcount = tessGeo.firstLineVertex;
          int icount = tessGeo.firstLineIndex;
          for (int ln = 0; ln < lineCount; ln++) {
            int i0 = first + 2 * ln + 0;
            int i1 = first + 2 * ln + 1;
            addLine(i0, i1, vcount, icount); vcount += 4; icount += 6;
          }          
        } else {          
          tessGeo.firstLineIndex = tessGeo.fillIndexCount;
          tessGeo.addFillVertices(inGeo.getNumLineVertices());
          tessGeo.addFillIndices(inGeo.getNumLineIndices());
          tessGeo.lastLineIndex = tessGeo.fillIndexCount - 1; 
          int vcount = tessGeo.firstFillVertex;
          int icount = tessGeo.firstFillIndex;           
          for (int ln = 0; ln < lineCount; ln++) {
            int i0 = first + 2 * ln + 0;
            int i1 = first + 2 * ln + 1;            
            addLineToFill(i0, i1, vcount, icount); vcount += 4; icount += 6;
          }    
//          GeneralPath path = new GeneralPath(GeneralPath.WIND_NON_ZERO);
//          for (int ln = 0; ln < lineCount; ln++) {
//            int i0 = first + 2 * ln + 0;
//            int i1 = first + 2 * ln + 1;
//            path.moveTo(inGeo.vertices[3 * i0 + 0], inGeo.vertices[3 * i0 + 1]);
//            path.lineTo(inGeo.vertices[3 * i1 + 0], inGeo.vertices[3 * i1 + 1]);
//            path.closePath();
//          }
//          tessGeo.firstLineIndex = tessGeo.fillIndexCount;        
//          tessellatePath(path);
//          tessGeo.lastLineIndex = tessGeo.fillIndexCount - 1;          
        }
      }  
    }

    public void tessellateTriangles() {
      int nInVert = inGeo.lastVertex - inGeo.firstVertex + 1;
      
      if (fill && 3 <= nInVert) {
        tessGeo.addFillVertices(inGeo);

        tessGeo.addFillIndices(nInVert);
        int idx0 = tessGeo.firstFillIndex;
        int offset = tessGeo.firstFillVertex;
        for (int i = inGeo.firstVertex; i <= inGeo.lastVertex; i++) {
          tessGeo.fillIndices[idx0 + i] = offset + i;
        }        
      }

      if (stroke) {
        tessGeo.isStroked = true;
        tessellateEdges();        
      }      
    }
    
    public void tessellateTriangleFan() {
      int nInVert = inGeo.lastVertex - inGeo.firstVertex + 1;
      
      if (fill && 3 <= nInVert) {
        tessGeo.addFillVertices(inGeo);

        tessGeo.addFillIndices(3 * (nInVert - 2));
        int idx = tessGeo.firstFillIndex;
        int offset = tessGeo.firstFillVertex; 
        for (int i = inGeo.firstVertex + 1; i < inGeo.lastVertex; i++) {
          tessGeo.fillIndices[idx++] = offset + inGeo.firstVertex;
          tessGeo.fillIndices[idx++] = offset + i;
          tessGeo.fillIndices[idx++] = offset + i + 1;
        }
      }
      
      if (stroke) {
        tessGeo.isStroked = true;
        tessellateEdges();
      }
    }
    
    
    public void tessellateTriangleStrip() {
      int nInVert = inGeo.lastVertex - inGeo.firstVertex + 1;
      
      if (fill && 3 <= nInVert) {
        tessGeo.addFillVertices(inGeo);
                
        int triCount = nInVert - 2;
        
        // Each vertex, except the first and last, defines a triangle.
        tessGeo.addFillIndices(3 * triCount);
        int idx = tessGeo.firstFillIndex;
        int offset = tessGeo.firstFillVertex;
        for (int i = inGeo.firstVertex + 1; i < inGeo.lastVertex; i++) {
          tessGeo.fillIndices[idx++] = offset + i;
          if (i % 2 == 0) {
            tessGeo.fillIndices[idx++] = offset + i - 1;  
            tessGeo.fillIndices[idx++] = offset + i + 1;
          } else {
            tessGeo.fillIndices[idx++] = offset + i + 1;  
            tessGeo.fillIndices[idx++] = offset + i - 1;
          }
        }              
      }      
      
      if (stroke) {
        tessGeo.isStroked = true;
        tessellateEdges();
      }
    }

    public void tessellateQuads() {
      int nInVert = inGeo.lastVertex - inGeo.firstVertex + 1;
      
      if (fill && 4 <= nInVert) {
        tessGeo.addFillVertices(inGeo);
      
        int quadCount = nInVert / 4;
        
        tessGeo.addFillIndices(6 * quadCount);
        int idx = tessGeo.firstFillIndex;
        int offset = tessGeo.firstFillVertex; 
        for (int qd = 0; qd < quadCount; qd++) {        
          int i0 = offset + 4 * qd + 0;
          int i1 = offset + 4 * qd + 1;
          int i2 = offset + 4 * qd + 2;
          int i3 = offset + 4 * qd + 3;
          
          tessGeo.fillIndices[idx++] = i0;
          tessGeo.fillIndices[idx++] = i1;
          tessGeo.fillIndices[idx++] = i3;
          
          tessGeo.fillIndices[idx++] = i1;
          tessGeo.fillIndices[idx++] = i2;
          tessGeo.fillIndices[idx++] = i3;
        }              
      }
      
      if (stroke) {
        tessGeo.isStroked = true;
        tessellateEdges();
      }
    }
    
    
    public void tessellateQuadStrip() {
      int nInVert = inGeo.lastVertex - inGeo.firstVertex + 1;
      
      if (fill && 4 <= nInVert) {
        tessGeo.addFillVertices(inGeo);
        
        int quadCount = nInVert / 2 - 1;
        
        tessGeo.addFillIndices(6 * quadCount);
        int idx = tessGeo.firstFillIndex;
        int offset = tessGeo.firstFillVertex; 
        for (int qd = 1; qd < nInVert / 2; qd++) {        
          int i0 = offset + 2 * (qd - 1);
          int i1 = offset + 2 * (qd - 1) + 1;
          int i2 = offset + 2 * qd + 1;
          int i3 = offset + 2 * qd;      
          
          tessGeo.fillIndices[idx++] = i0;
          tessGeo.fillIndices[idx++] = i1;
          tessGeo.fillIndices[idx++] = i3;
          
          tessGeo.fillIndices[idx++] = i1;
          tessGeo.fillIndices[idx++] = i2;
          tessGeo.fillIndices[idx++] = i3;
        }              
      }
 
      if (stroke) {
        tessGeo.isStroked = true;
        tessellateEdges();
      }
    }  
    
    public void tessellatePolygon(boolean solid, boolean closed) {
      int nInVert = inGeo.lastVertex - inGeo.firstVertex + 1;
      
      if (fill && 3 <= nInVert) {
        GLU.gluTessBeginPolygon(gluTess, null);
        
        if (solid) {
          // Using NONZERO winding rule for solid polygons.
          GLU.gluTessProperty(gluTess, GLU.GLU_TESS_WINDING_RULE, GLU.GLU_TESS_WINDING_NONZERO);      
        } else {
          // Using ODD winding rule to generate polygon with holes.
          GLU.gluTessProperty(gluTess, GLU.GLU_TESS_WINDING_RULE, GLU.GLU_TESS_WINDING_ODD);      
        }

        GLU.gluTessBeginContour(gluTess);    
        
        // Now, iterate over all input data and send to GLU tessellator..
        for (int i = inGeo.firstVertex; i <= inGeo.lastVertex; i++) {
          boolean breakPt = inGeo.codes[i] == PShape.BREAK;      
          if (breakPt) {
            GLU.gluTessEndContour(gluTess);  
            GLU.gluTessBeginContour(gluTess);
          }
          
          // Vertex data includes coordinates, colors, normals and texture coordinates.
          double[] vertex = new double[] { inGeo.vertices[3 * i + 0], inGeo.vertices[3 * i + 1], inGeo.vertices[3 * i + 2],
                                           inGeo.colors[4 * i + 0], inGeo.colors[4 * i + 1], inGeo.colors[4 * i + 2], inGeo.colors[4 * i + 3],
                                           inGeo.normals[3 * i + 0], inGeo.normals[3 * i + 1], inGeo.normals[3 * i + 2],
                                           inGeo.texcoords[2 * i + 0], inGeo.texcoords[2 * i + 1] };
          GLU.gluTessVertex(gluTess, vertex, 0, vertex);
        }
        
        GLU.gluTessEndContour(gluTess);
        
        GLU.gluTessEndPolygon(gluTess);
      }

      if (stroke) {
        tessGeo.isStroked = true;
        tessellateEdges();
      }  
    }
     
    protected void addLineToFill(int i0, int i1, int vcount, int icount) {
      int index; 
          
      index = 3 * i0;
      float x0 = in.vertices[index++];
      float y0 = in.vertices[index++];
      
      index = 3 * i1;
      float x1 = in.vertices[index++];
      float y1 = in.vertices[index++];      
      
      float dx = x1 - x0;
      float dy = y1 - y0;
      float len = PApplet.sqrt(dx * dx + dy * dy);

      float linePerpX = -dy * (0.5f * strokeWeight / len);
      float linePerpY =  dx * (0.5f * strokeWeight / len);
      
      tessGeo.putLineVertexIntoFill(inGeo, i0, +linePerpX, +linePerpY, vcount);
      tessGeo.fillIndices[icount++] = vcount;
      
      vcount++;
      tessGeo.putLineVertexIntoFill(inGeo, i0, -linePerpX, -linePerpY, vcount);
      tessGeo.fillIndices[icount++] = vcount;
      
      vcount++;
      tessGeo.putLineVertexIntoFill(inGeo, i1, +linePerpX, +linePerpY, vcount);
      tessGeo.fillIndices[icount++] = vcount;
      
      // Starting a new triangle re-using prev vertices.
      tessGeo.fillIndices[icount++] = vcount;
      tessGeo.fillIndices[icount++] = vcount - 1;
      
      vcount++;
      tessGeo.putLineVertexIntoFill(inGeo, i1, -linePerpX, -linePerpY, vcount);      
      tessGeo.fillIndices[icount++] = vcount;  
    }
    
    // Adding the data that defines a quad starting at vertex i0 and
    // ending at i1.
    protected void addLine(int i0, int i1, int vcount, int icount) {
      tessGeo.putLineVertex(inGeo, i0, i1, vcount);
   
      tessGeo.lineAttributes[4 * vcount + 3] = +strokeWeight;
      tessGeo.lineIndices[icount++] = vcount;
      
      vcount++;
      tessGeo.putLineVertex(inGeo, i0, i1, vcount);
      tessGeo.lineAttributes[4 * vcount + 3] = -strokeWeight;
      tessGeo.lineIndices[icount++] = vcount;
      
      vcount++;
      tessGeo.putLineVertex(inGeo, i1, i0, vcount);
      tessGeo.lineAttributes[4 * vcount + 3] = -strokeWeight;
      tessGeo.lineIndices[icount++] = vcount;
      
      // Starting a new triangle re-using prev vertices.
      tessGeo.lineIndices[icount++] = vcount;
      tessGeo.lineIndices[icount++] = vcount - 1;
      
      vcount++;
      tessGeo.putLineVertex(inGeo, i1, i0, vcount);      
      tessGeo.lineAttributes[4 * vcount + 3] = +strokeWeight;
      tessGeo.lineIndices[icount++] = vcount;
    }
    
    public void tessellateEdges() {
      // Tessellation of stroked line is done differently in 3D or 2D mode.
      // When we are in 3D, the z-buffer is enough to determine visibility of
      // shapes, so we can render fill, line, and point geometry in separate
      // buffers and the visual result would be correct.
      // But in 2D, because all the shapes are contained in the Z=0 plane
      // we need to render the geometry in exactly the same order it has
      // been drawn by the user. So fill and line geometry are both stored
      // in the fill arrays. The line geometry is tessellated using the path
      // shape generated with the AWT utilities.      
      if (is3D) {
        tessGeo.addLineVertices(inGeo.getNumLineVertices());
        tessGeo.addLineIndices(inGeo.getNumLineIndices());
        int vcount = tessGeo.firstLineVertex;
        int icount = tessGeo.firstLineIndex;          
        for (int i = inGeo.firstEdge; i <= inGeo.lastEdge; i++) {
          int[] edge = inGeo.edges[i];
          addLine(edge[0], edge[1], vcount, icount); vcount += 4; icount += 6;
        }
      } else {
        tessGeo.firstLineIndex = tessGeo.fillIndexCount;
        tessGeo.addFillVertices(inGeo.getNumLineVertices());
        tessGeo.addFillIndices(inGeo.getNumLineIndices());
        tessGeo.lastLineIndex = tessGeo.fillIndexCount - 1; 
        int vcount = tessGeo.firstFillVertex;
        int icount = tessGeo.firstFillIndex;          
        for (int i = inGeo.firstEdge; i <= inGeo.lastEdge; i++) {
          int[] edge = inGeo.edges[i];
          addLineToFill(edge[0], edge[1], vcount, icount); vcount += 4; icount += 6;
        }     

        // Not using the fancy path tessellation in 2D because it slows down things
        // significantly (it also calls the GLU tessellator).
        // It generates the right caps and joins, though.
        
//        GeneralPath path = new GeneralPath(GeneralPath.WIND_NON_ZERO);          
//        for (int i = inGeo.firstEdge; i <= inGeo.lastEdge; i++) {
//          int[] edge = inGeo.edges[i];
//          if (startEdge(edge[2])) path.moveTo(inGeo.getVertexX(edge[0]), inGeo.getVertexY(edge[0])); 
//          path.lineTo(inGeo.getVertexX(edge[1]), inGeo.getVertexY(edge[1]));
//          if (endEdge(edge[2])) path.closePath();
//        }        
//        tessGeo.firstLineIndex = tessGeo.fillIndexCount;        
//        tessellatePath(path);
//        tessGeo.lastLineIndex = tessGeo.fillIndexCount - 1;          
      }      
    }
    
    public boolean startEdge(int edge) {
      return edge % 2 != 0;
    }
    
    public boolean endEdge(int edge) {
      return 1 < edge;
    }       
    
    // Tessellates the path given as parameter. This will work only in 2D mode.
    // By Tom Carden, and Karl D.D. Willis:
    // http://wiki.processing.org/w/Stroke_attributes_in_OpenGL
    public void tessellatePath(GeneralPath path) {
      // AWT implementation for Android?
      // http://hi-android.info/src/java/awt/Shape.java.html
      // http://hi-android.info/src/java/awt/geom/GeneralPath.java.html
      // http://hi-android.info/src/java/awt/geom/PathIterator.java.html
      // and:
      // http://stackoverflow.com/questions/3897775/using-awt-with-android
        
      BasicStroke bs;
      int bstrokeCap = strokeCap == ROUND ? BasicStroke.CAP_ROUND :
                       strokeCap == PROJECT ? BasicStroke.CAP_SQUARE :
                       BasicStroke.CAP_BUTT;
      int bstrokeJoin = strokeJoin == ROUND ? BasicStroke.JOIN_ROUND :
                        strokeJoin == BEVEL ? BasicStroke.JOIN_BEVEL :
                        BasicStroke.JOIN_MITER;              
      bs = new BasicStroke(strokeWeight, bstrokeCap, bstrokeJoin);      
            
      // Make the outline of the stroke from the path
      Shape sh = bs.createStrokedShape(path);
      
      GLU.gluTessBeginPolygon(gluTess, null);
      
      float lastX = 0;
      float lastY = 0;
      double[] vertex;
      float[] coords = new float[6];
      
      PathIterator iter = sh.getPathIterator(null); // ,5) add a number on here to simplify verts
      int rule = iter.getWindingRule();
      switch(rule) {
      case PathIterator.WIND_EVEN_ODD:
        GLU.gluTessProperty(gluTess, GLU.GLU_TESS_WINDING_RULE, GLU.GLU_TESS_WINDING_ODD);
        break;
      case PathIterator.WIND_NON_ZERO:
        GLU.gluTessProperty(gluTess, GLU.GLU_TESS_WINDING_RULE, GLU.GLU_TESS_WINDING_NONZERO);
        break;
      }
      
      while (!iter.isDone()) {
        
        switch (iter.currentSegment(coords)) {
   
        case PathIterator.SEG_MOVETO:   // 1 point (2 vars) in coords
          GLU.gluTessBeginContour(gluTess);
   
        case PathIterator.SEG_LINETO:   // 1 point
          vertex = new double[] { coords[0], coords[1], 0,
            strokeRed, strokeGreen, strokeBlue, strokeAlpha,
            0, 0, 1,
            0, 0 };          
          
          GLU.gluTessVertex(gluTess, vertex, 0, vertex);
          lastX = coords[0];
          lastY = coords[1];
          break;
   
        case PathIterator.SEG_QUADTO:   // 2 points
          for (int i = 1; i < bezierDetail; i++) {
            float t = (float)i / (float)bezierDetail;
            vertex = new double[] { 
              bezierPoint(lastX, coords[0], coords[2], coords[2], t),
              bezierPoint(lastY, coords[1], coords[3], coords[3], t), 
              0, 
              strokeRed, strokeGreen, strokeBlue, strokeAlpha,
              0, 0, 1,
              0, 0 };
            GLU.gluTessVertex(gluTess, vertex, 0, vertex);
          }
          lastX = coords[2];
          lastY = coords[3];
          break;
   
        case PathIterator.SEG_CUBICTO:  // 3 points
          for (int i = 1; i < bezierDetail; i++) {
            float t = (float)i / (float)bezierDetail;
            vertex = new double[] { 
              bezierPoint(lastX, coords[0], coords[2], coords[4], t),
              bezierPoint(lastY, coords[1], coords[3], coords[5], t), 
              0, 
              strokeRed, strokeGreen, strokeBlue, strokeAlpha,
              0, 0, 1,
              0, 0 };
            GLU.gluTessVertex(gluTess, vertex, 0, vertex);
          }
          lastX = coords[4];
          lastY = coords[5];
          break;
   
        case PathIterator.SEG_CLOSE:
          GLU.gluTessEndContour(gluTess);
          break;
        }
        iter.next();
      }
      GLU.gluTessEndPolygon(gluTess);      
    }
    
    public class GLUTessCallback extends GLUtessellatorCallbackAdapter {
      protected int tessFirst;
      protected int tessCount;
      protected int tessType;
      
      public void begin(int type) {
        tessFirst = tessGeo.fillVertexCount;
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
        tessGeo.addFillIndex(tessFirst + tessIdx);
      }

      public void vertex(Object data) {
        if (data instanceof double[]) {
          double[] d = (double[]) data;
          if (d.length < 12) {
            throw new RuntimeException("TessCallback vertex() data " +
                                       "isn't length 12");
          }
          
          tessGeo.addFillVertex((float) d[0], (float) d[1], (float) d[2],
                                (float) d[3], (float) d[4], (float) d[5], (float) d[6],
                                (float) d[7], (float) d[8], (float) d[9],
                                (float) d[10], (float) d[11]);

          tessCount++;
        } else {
          throw new RuntimeException("TessCallback vertex() data not understood");
        }
      }

      public void error(int errnum) {
        String estring = glu.gluErrorString(errnum);
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

        // Calculating the rest of the vertex parameters (color,
        // normal, texcoords) as the linear combination of the 
        // combined vertices.
        for (int i = 3; i < 12; i++) {
          vertex[i] = 0;
          for (int j = 0; j < 4; j++) {
            double[] vertData = (double[])data[j];
            if (vertData != null) {
              vertex[i] += weight[j] * vertData[i];
            }
          }
        }
        
        // Normalizing normal vector, since the weighted 
        // combination of normal vectors is not necessarily 
        // normal.
        double sum = vertex[7] * vertex[7] + 
                     vertex[8] * vertex[8] + 
                     vertex[9] * vertex[9];
        double len = Math.sqrt(sum);      
        vertex[7] /= len; 
        vertex[8] /= len;
        vertex[9] /= len;  
        
        outData[0] = vertex;
      }
    }
  }
}
