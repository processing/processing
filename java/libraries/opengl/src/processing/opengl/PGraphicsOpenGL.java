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
import javax.media.opengl.GLContext;
import javax.media.opengl.GLDrawable;
import javax.media.opengl.GLDrawableFactory;
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
  /** Interface between Processing and OpenGL */
  protected PGL pgl;
  
  /** Selected GL profile */
  protected GLProfile profile;
  
  /** The capabilities of the OpenGL rendering surface */
  protected GLCapabilities capabilities;  
  
  /** The rendering surface */
  protected GLDrawable drawable;   
  
  /** The rendering context (holds rendering state info) */
  protected GLContext context;
  
  /** The PApplet renderer. For the primary surface, renderer == this. */
  protected PGraphicsOpenGL renderer;

  // ........................................................  
  
  // VBOs for immediate rendering:  
  
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
  
  // GL objects:
  
  static protected HashMap<Integer, Boolean> glVertexArrays      = new HashMap<Integer, Boolean>();
  static protected HashMap<Integer, Boolean> glTextureObjects    = new HashMap<Integer, Boolean>();
  static protected HashMap<Integer, Boolean> glVertexBuffers     = new HashMap<Integer, Boolean>();
  static protected HashMap<Integer, Boolean> glFrameBuffers      = new HashMap<Integer, Boolean>();
  static protected HashMap<Integer, Boolean> glRenderBuffers     = new HashMap<Integer, Boolean>();    
  static protected HashMap<Integer, Boolean> glslPrograms        = new HashMap<Integer, Boolean>();
  static protected HashMap<Integer, Boolean> glslVertexShaders   = new HashMap<Integer, Boolean>();
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
  
  /** Distance between the camera eye and aim point. */
  protected float cameraDepth; 
  
  /** Actual position of the camera. */
  protected float cameraEyeX, cameraEyeY, cameraEyeZ; 
  
  /** Flag to indicate that we are inside beginCamera/endCamera block. */
  protected boolean manipulatingCamera;
  
  // ........................................................

  // Projection, camera, and modelview matrices.
  public PMatrix3D projection;    
  public PMatrix3D camera;
  public PMatrix3D cameraInv;  
  public PMatrix3D modelview;
  
  // Temporary array to copy the PMatrices to OpenGL.
  protected float[] glMatrix;
  
  protected boolean matricesAllocated = false;
  
  /** 
   * Marks when changes to the size have occurred, so that the camera 
   * will be reset in beginDraw().
   */
  protected boolean sizeChanged;  
  
  /** Modelview matrix stack **/
  protected Stack<PMatrix3D> modelviewStack;  
  
  /** Projection matrix stack **/
  protected Stack<PMatrix3D> projectionStack;

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
  protected int[] viewport = {0, 0, 0, 0};
  
  // ........................................................

  // Utility constants:  
  
  /**
   * Set to true if the host system is big endian (PowerPC, MIPS, SPARC), false
   * if little endian (x86 Intel for Mac or PC).
   */
  static public boolean BIG_ENDIAN = ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN;

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

  protected InGeometry inGeo;
  protected TessGeometry tessGeo;
  protected TexCache texCache;

  protected float[] currentVertex = { 0, 0, 0 };
  protected float[] currentColor = { 0, 0, 0, 0 };  
  protected float[] currentNormal = { 0, 0, 1 };
  protected float[] currentTexcoord = { 0, 0 };
  protected float[] currentStroke = { 0, 0, 0, 1, 1 };  

  protected boolean openContour = false;
  protected boolean breakShape = false;  
  
  public static int flushMode = FLUSH_WHEN_FULL;
//  public static int flushMode = FLUSH_AFTER_SHAPE;
 
  public static final int MIN_ARRAYCOPY_SIZE = 2;
  
  public static final int MAX_TESS_VERTICES = 1000000;
  public static final int MAX_TESS_INDICES  = 3000000; 
  
  public static final int DEFAULT_IN_VERTICES = 64;
  public static final int DEFAULT_IN_EDGES = 128;
  public static final int DEFAULT_IN_TEXTURES = 64;
  public static final int DEFAULT_TESS_VERTICES = 64;
  public static final int DEFAULT_TESS_INDICES = 128;
    
  protected Tessellator tessellator;
  
  static protected PShader lineShader;
  static protected PShader pointShader;
  static protected int lineAttribsLoc;
  static protected int pointAttribsLoc;
  
  protected PImage textureImage0;
  
  protected boolean clip = false;
  
  protected boolean defaultEdges = false;
  
  protected int vboMode = PGL.STATIC_DRAW;
    
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
    pgl = new PGL();
    
    tessellator = new Tessellator();
    
    inGeo = newInGeometry();
    tessGeo = newTessGeometry(IMMEDIATE);
    texCache = newTexCache();
    
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
      glMatrix = new float[16];
      projection = new PMatrix3D();
      camera = new PMatrix3D();
      cameraInv = new PMatrix3D();
      modelview = new PMatrix3D();      
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
  

  // Only for debugging purposes.
  public void setFlushMode(int mode) {
    PGraphicsOpenGL.flushMode = mode;    
  }
  
  
  //////////////////////////////////////////////////////////////

  // RESOURCE HANDLING
  
  // Vertex Array Objects --------------------------------------
  
  protected int createVertexArrayObject() {
    deleteFinalizedVertexArrayObjects();
    
    int[] temp = new int[1];
    pgl.genVertexArray(temp);
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
      pgl.delVertexArray(temp);  
      glVertexArrays.remove(id); 
    }
  }
  
  protected void deleteAllVertexArrayObjects() {
    for (Integer id : glVertexArrays.keySet()) {
      int[] temp = { id.intValue() };
      pgl.delVertexArray(temp);
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
        pgl.delVertexArray(temp);
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
    pgl.genTexture(temp);
    int id = temp[0];
    
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
      pgl.delTexture(temp);
      glTextureObjects.remove(id); 
    }
  }  
  
  protected void deleteAllTextureObjects() {
    for (Integer id : glTextureObjects.keySet()) {
      int[] temp = { id.intValue() };
      pgl.delTexture(temp);
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
        pgl.delTexture(temp);
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
    pgl.genBuffer(temp);
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
      pgl.delBuffer(temp);
      glVertexBuffers.remove(id); 
    }
  }
  
  protected void deleteAllVertexBufferObjects() {
    for (Integer id : glVertexBuffers.keySet()) {
      int[] temp = { id.intValue() };
      pgl.delBuffer(temp);
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
        pgl.delBuffer(temp);
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
    pgl.genFramebuffer(temp);
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
      pgl.delFramebuffer(temp);
      glFrameBuffers.remove(id); 
    }
  }  
  
  protected void deleteAllFrameBufferObjects() {
    for (Integer id : glFrameBuffers.keySet()) {
      int[] temp = { id.intValue() };
      pgl.delFramebuffer(temp);
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
        pgl.delFramebuffer(temp);
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
    pgl.genRenderbuffer(temp);
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
      pgl.delRenderbuffer(temp);
      glRenderBuffers.remove(id); 
    }
  }   
  
  protected void deleteAllRenderBufferObjects() {
    for (Integer id : glRenderBuffers.keySet()) {
      int[] temp = { id.intValue() };
      pgl.delRenderbuffer(temp);
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
        pgl.delRenderbuffer(temp);
      }
    }
    
    for (Integer id : finalized) {
      glRenderBuffers.remove(id);  
    }
  }
  
  // GLSL Program Objects -----------------------------------------------
  
  protected int createGLSLProgramObject() {
    
    renderer.report("before delete");
    deleteFinalizedGLSLProgramObjects();
        
    int[] temp = new int[1];
    pgl.genProgram(temp);
    int id = temp[0];
    
    if (glslPrograms.containsKey(id)) {
      showWarning("Adding same glsl program twice");
    } else {    
      glslPrograms.put(id, false);
    }
    
    return id;
  }
  
  protected void deleteGLSLProgramObject(int id) {
    if (glslPrograms.containsKey(id)) {
      int[] temp = { id };
      pgl.delProgram(temp);
      glslPrograms.remove(id); 
    }
  }     
  
  protected void deleteAllGLSLProgramObjects() {
    for (Integer id : glslPrograms.keySet()) {
      int[] temp = { id.intValue() };
      pgl.delProgram(temp);
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
        int[] temp = { id.intValue() };
        pgl.delProgram(temp);
      }
    }
    
    for (Integer id : finalized) {
      glslPrograms.remove(id);  
    }
  }

  // GLSL Vertex Shader Objects -----------------------------------------------
  
  protected int createGLSLVertShaderObject() {
    deleteFinalizedGLSLVertShaderObjects();
    
    int[] temp = new int[1];
    pgl.genVertexShader(temp);
    int id = temp[0];    
    
    if (glslVertexShaders.containsKey(id)) {
      showWarning("Adding same glsl vertex shader twice");
    } else {    
      glslVertexShaders.put(id, false);
    }
    
    return id;
  }
  
  protected void deleteGLSLVertShaderObject(int id) {
    if (glslVertexShaders.containsKey(id)) {
      int[] temp = { id };
      pgl.delVertexShader(temp);
      glslVertexShaders.remove(id); 
    }
  }    
  
  protected void deleteAllGLSLVertShaderObjects() {
    for (Integer id : glslVertexShaders.keySet()) {
      int[] temp = { id.intValue() };
      pgl.delVertexShader(temp);
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
        int[] temp = { id.intValue() };
        pgl.delVertexShader(temp);
      }
    }
    
    for (Integer id : finalized) {
      glslVertexShaders.remove(id);  
    }
  }
  
  // GLSL Fragment Shader Objects -----------------------------------------------
    
  
  protected int createGLSLFragShaderObject() {
    deleteFinalizedGLSLFragShaderObjects();
    
    int[] temp = new int[1];
    pgl.genFragmentShader(temp);
    int id = temp[0];        
    
    if (glslFragmentShaders.containsKey(id)) {
      showWarning("Adding same glsl fragment shader twice");
    } else {    
      glslFragmentShaders.put(id, false);
    }
    
    return id;
  }
  
  protected void deleteGLSLFragShaderObject(int id) {
    if (glslFragmentShaders.containsKey(id)) {
      int[] temp = { id };
      pgl.delFragmentShader(temp);
      glslFragmentShaders.remove(id); 
    }
  }     
  
  protected void deleteAllGLSLFragShaderObjects() {
    for (Integer id : glslFragmentShaders.keySet()) {
      int[] temp = { id.intValue() };
      pgl.delFragmentShader(temp);
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
        int[] temp = { id.intValue() };
        pgl.delFragmentShader(temp);
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
    // the renderer itself.
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
    
    releaseContext();
    context.destroy();
    context = null;
    
    allocate();          
    detainContext();      
    updateGLInterfaces();    
  }  

  protected void createFillBuffers() {
    glFillVertexBufferID = createVertexBufferObject();    
    pgl.bindVertexBuffer(glFillVertexBufferID);
    pgl.initVertexBuffer(3 * MAX_TESS_VERTICES, vboMode);
                
    glFillColorBufferID = createVertexBufferObject();
    pgl.bindVertexBuffer(glFillColorBufferID);
    pgl.initVertexBuffer(4 * MAX_TESS_VERTICES, vboMode);
    
    glFillNormalBufferID = createVertexBufferObject();
    pgl.bindVertexBuffer(glFillNormalBufferID);
    pgl.initVertexBuffer(3 * MAX_TESS_VERTICES, vboMode);    
    
    glFillTexCoordBufferID = createVertexBufferObject();
    pgl.bindVertexBuffer(glFillTexCoordBufferID);
    pgl.initVertexBuffer(2 * MAX_TESS_VERTICES, vboMode);    
    
    pgl.unbindVertexBuffer();
    
    glFillIndexBufferID = createVertexBufferObject();    
    pgl.bindIndexBuffer(glFillIndexBufferID);
    pgl.initIndexBuffer(MAX_TESS_INDICES, vboMode);

    pgl.unbindIndexBuffer();
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
    pgl.bindVertexBuffer(glLineVertexBufferID);
    pgl.initVertexBuffer(3 * MAX_TESS_VERTICES, vboMode);
    
    glLineColorBufferID = createVertexBufferObject();
    pgl.bindVertexBuffer(glLineColorBufferID);
    pgl.initVertexBuffer(4 * MAX_TESS_VERTICES, vboMode);
    
    glLineNormalBufferID = createVertexBufferObject();    
    pgl.bindVertexBuffer(glLineNormalBufferID);
    pgl.initVertexBuffer(3 * MAX_TESS_VERTICES, vboMode);
        
    glLineAttribBufferID = createVertexBufferObject();
    pgl.bindVertexBuffer(glLineAttribBufferID);
    pgl.initVertexBuffer(4 * MAX_TESS_VERTICES, vboMode);
    
    pgl.unbindVertexBuffer();
    
    glLineIndexBufferID = createVertexBufferObject();    
    pgl.bindIndexBuffer(glLineIndexBufferID);
    pgl.initIndexBuffer(MAX_TESS_INDICES, vboMode);    
    
    pgl.unbindIndexBuffer();
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
    pgl.bindVertexBuffer(glPointVertexBufferID);
    pgl.initVertexBuffer(3 * MAX_TESS_VERTICES, vboMode);
    
    glPointColorBufferID = createVertexBufferObject();
    pgl.bindVertexBuffer(glPointColorBufferID);
    pgl.initVertexBuffer(4 * MAX_TESS_VERTICES, vboMode);    
    
    glPointNormalBufferID = createVertexBufferObject();    
    pgl.bindVertexBuffer(glPointNormalBufferID);
    pgl.initVertexBuffer(3 * MAX_TESS_VERTICES, vboMode);
    
    glPointAttribBufferID = createVertexBufferObject();
    pgl.bindVertexBuffer(glPointAttribBufferID);
    pgl.initVertexBuffer(2 * MAX_TESS_VERTICES, vboMode);    
    
    pgl.unbindVertexBuffer();
    
    glPointIndexBufferID = createVertexBufferObject();    
    pgl.bindIndexBuffer(glPointIndexBufferID);
    pgl.initIndexBuffer(MAX_TESS_INDICES, vboMode);       
    
    pgl.unbindIndexBuffer();
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
      renderer.saveGLState();
            
      // Disabling all lights, so the offscreen renderer can set completely
      // new light configuration (otherwise some light configuration from the 
      // primary renderer might stay).
      renderer.disableLights();
    }     
    
    inGeo.reset();
    tessGeo.reset();
    texCache.reset();
    
    // Each frame starts with textures disabled. 
    super.noTexture();
        
    // Screen blend is needed for alpha (i.e. fonts) to work.
    // Using setDefaultBlend() instead of blendMode() because
    // the latter will set the blend mode only if it is different
    // from current.
    setDefaultBlend();
       
    // this is necessary for 3D drawing
    if (hints[DISABLE_DEPTH_TEST]) {
      pgl.disableDepthTest();
    } else {
      pgl.enableDepthTest();
    }
    // use <= since that's what processing.core does
    pgl.setDepthFunc(PGL.LESS_OR_EQUAL);
    
    if (hints[DISABLE_DEPTH_MASK]) {
      pgl.disableDepthMask();
    } else {
      pgl.enableDepthMask();
    }

    if (hints[ENABLE_ACCURATE_2D]) {
      flushMode = FLUSH_CONTINUOUSLY;      
    } else {
      flushMode = FLUSH_WHEN_FULL;
    }
    
    // setup opengl viewport.    
    pgl.getViweport(savedViewport);
    viewport[0] = 0; viewport[1] = 0; viewport[2] = width; viewport[3] = height;
    pgl.setViewport(viewport);
    if (resized) {
      // To avoid having garbage in the screen after a resize,
      // in the case background is not called in draw().
      background(0);
      if (texture != null) {
        // The screen texture should be deleted because it 
        // corresponds to the old window size.
        this.removeCache(renderer);
        this.removeParams(renderer);
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
      // The camera and projection matrices, saved when calling camera() and frustrum()
      // are set as the current modelview and projection matrices. This is done to
      // remove any additional modelview transformation (and less likely, projection
      // transformations) applied by the user after setting the camera and/or projection      
      loadCamera();
      modelview.set(camera);      
      loadProjection();
    }
      
    noLights();
    lightFalloff(1, 0, 0);
    lightSpecular(0, 0, 0);

    // because y is flipped
    pgl.setFrontFace(PGL.CLOCKWISE);
    
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
        pgl.setDrawBuffer(0);
      } else {
        setFramebuffer(offscreenFramebuffer);
      }
    }
    
    // Clear depth and stencil buffers.
    pgl.setClearColor(0, 0, 0, 0);
    pgl.clearDepthAndStencilBuffers();
    
    drawing = true;
    
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
    pgl.setViewport(savedViewport);
    
    if (primarySurface) {
      // glFlush should be called only once, since it is an expensive
      // operation. Thus, only the main renderer (the primary surface)
      // should call it at the end of draw, and none of the offscreen 
      // renderers...
      pgl.flush();
      
      if (drawable != null) {
        drawable.swapBuffers();
        releaseContext();
      }
    } else {
      if (offscreenMultisample) {
        offscreenFramebufferMultisample.copy(offscreenFramebuffer);       
      }
      popFramebuffer();
      
      renderer.restoreGLState();
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
    return pgl.gl;
  }

  
  public void endGL() {
    restoreGLState();
  }
  
  
  public void updateGLInterfaces() {
    pgl.update(context);
  }
  
  
  protected void saveGLState() {
    saveGLMatrices();
  }
  
  
  protected void restoreGLState() {        
    // Restoring viewport.
    pgl.setViewport(viewport);

    restoreGLMatrices();
    
    // Restoring hints.
    if (hints[DISABLE_DEPTH_TEST]) {
      pgl.disableDepthTest();
      pgl.setClearColor(0, 0, 0, 0);
      pgl.clearDepthBuffer();
    } else {
      pgl.enableDepthTest();
    }
    
    if (hints[DISABLE_DEPTH_MASK]) {
      pgl.disableDepthMask();
    } else {
      pgl.enableDepthMask();
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
    pgl.setFrontFace(PGL.CLOCKWISE);
    pgl.setDepthFunc(PGL.LESS_OR_EQUAL);
    
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
        
    if (fbStack == null) {
      fbStack = new Stack<PFramebuffer>();

      screenFramebuffer = new PFramebuffer(parent, width, height, true);
      setFramebuffer(screenFramebuffer);
    }    
    
    if (modelviewStack == null) {
      modelviewStack = new Stack<PMatrix3D>();
    }
    if (projectionStack == null) {
      projectionStack = new Stack<PMatrix3D>();
    }
    
    // easiest for beginners
    textureMode(IMAGE);
  }
  
  // reapplySettings

  //////////////////////////////////////////////////////////////

  // HINTS

  public void hint(int which) {
    boolean oldValue = hints[which];
    super.hint(which);
    boolean newValue = hints[which];

    if (oldValue == newValue) {
      return;
    }
    
    if (which == DISABLE_DEPTH_TEST) {
      flush();
      pgl.disableDepthTest();
      pgl.setClearColor(0, 0, 0, 0);
      pgl.clearDepthBuffer();      

    } else if (which == ENABLE_DEPTH_TEST) {
      flush();
      pgl.enableDepthTest();

    } else if (which == DISABLE_DEPTH_MASK) {
      flush();
      pgl.disableDepthMask();

    } else if (which == ENABLE_DEPTH_MASK) {
      flush();
      pgl.enableDepthMask();
      
    } else if (which == DISABLE_ACCURATE_2D) {
      flush();
      setFlushMode(FLUSH_WHEN_FULL);
      
    } else if (which == ENABLE_ACCURATE_2D) {
      flush();
      setFlushMode(FLUSH_CONTINUOUSLY);
      
    } else if (which == DISABLE_TEXTURE_CACHE) {
      flush();
      
    } else if (which == DISABLE_PERSPECTIVE_CORRECTED_LINES) {
      if (0 < tessGeo.lineVertexCount && 0 < tessGeo.lineIndexCount) {
        flush();
      }
      
    } else if (which == ENABLE_PERSPECTIVE_CORRECTED_LINES &&
               0 < tessGeo.lineVertexCount && 0 < tessGeo.lineIndexCount) {
      if (0 < tessGeo.lineVertexCount && 0 < tessGeo.lineIndexCount) {
        flush();
      }      
      
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
    
    inGeo.reset();
        
    breakShape = false;    
    defaultEdges = true;
    
    textureImage0 = textureImage;
    // The superclass method is called to avoid an early flush.
    super.noTexture();
    
    normalMode = NORMAL_MODE_AUTO;
  }

  
  public void endShape(int mode) {
    // Disabled for now. This should be controlled by an additional hint...
    if (flushMode == FLUSH_WHEN_FULL && hints[DISABLE_TEXTURE_CACHE] && 
        textureImage0 != null && textureImage == null) {
      // The previous shape had a texture and this one doesn't. So we need to flush
      // the textured geometry.
      textureImage = textureImage0;
      flush();
      textureImage = null;      
    }
    
    tessellate(mode);
    
    if (flushMode == FLUSH_CONTINUOUSLY || 
        (flushMode == FLUSH_WHEN_FULL && tessGeo.isFull())) {
      // Flushing this current shape either because we are in the flush-after-shape,
      // or the tess buffer is full or... we are in 2D mode and the shape is textured
      // and stroked, so we need to rendering right away to avoid depth-sorting issues.
      flush();
    }    
  }

  
  public void texture(PImage image) {
    if (flushMode == FLUSH_WHEN_FULL && hints[DISABLE_TEXTURE_CACHE] && 
        image != textureImage0) {
      // Changing the texture image, so we need to flush the
      // tessellated geometry accumulated until now, so that
      // textures are not mixed.
      textureImage = textureImage0;      
      flush();     
    }
    super.texture(image);
  }
  
  
  public void noTexture() {
    if (flushMode == FLUSH_WHEN_FULL && hints[DISABLE_TEXTURE_CACHE] &&
        null != textureImage0) {
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

      PTexture tex = queryTexture(textureImage);
      if (tex != null && tex.isFlippedY()) {
        v = 1 - v;
      }
    }
    
    inGeo.addVertex(x, y, z, 
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
    pgl.enableClipping();
    
    float h = y2 - y1;
    pgl.setClipRect((int)x1, (int)(height - y1 - h), (int)(x2 - x1), (int)h);
    
    clip = true;
  }

  
  public void noClip() {
    if (clip) {
      flush();
      pgl.disableClipping();      
      clip = false;
    }
  }  
  
  
  //////////////////////////////////////////////////////////////

  // RENDERING

  // protected void render()

  // protected void sort()  
  
  protected void tessellate(int mode) {
    tessellator.setInGeometry(inGeo);
    tessellator.setTessGeometry(tessGeo);
    tessellator.setFill(fill || textureImage != null);
    tessellator.setStroke(stroke);
    tessellator.setStrokeWeight(strokeWeight);
    tessellator.setStrokeCap(strokeCap);
    tessellator.setStrokeJoin(strokeJoin);
    tessellator.setStrokeColor(strokeR, strokeG, strokeB, strokeA);
    
    int first = tessGeo.fillIndexCount;
    
    if (shape == POINTS) {
      if (normalMode == NORMAL_MODE_AUTO) inGeo.calcPointsNormals();
      tessellator.tessellatePoints();    
    } else if (shape == LINES) {
      if (normalMode == NORMAL_MODE_AUTO) inGeo.calcLinesNormals();
      tessellator.tessellateLines();    
    } else if (shape == TRIANGLE || shape == TRIANGLES) {
      if (stroke && defaultEdges) inGeo.addTrianglesEdges();
      if (normalMode == NORMAL_MODE_AUTO) inGeo.calcTrianglesNormals();
      tessellator.tessellateTriangles();
    } else if (shape == TRIANGLE_FAN) {
      if (stroke && defaultEdges) inGeo.addTriangleFanEdges();
      if (normalMode == NORMAL_MODE_AUTO) inGeo.calcTriangleFanNormals();
      tessellator.tessellateTriangleFan();
    } else if (shape == TRIANGLE_STRIP) {
      if (stroke && defaultEdges) inGeo.addTriangleStripEdges();      
      if (normalMode == NORMAL_MODE_AUTO) inGeo.calcTriangleStripNormals();      
      tessellator.tessellateTriangleStrip();
    } else if (shape == QUAD || shape == QUADS) {
      if (stroke && defaultEdges) inGeo.addQuadsEdges();
      if (normalMode == NORMAL_MODE_AUTO) inGeo.calcQuadsNormals();
      tessellator.tessellateQuads();
    } else if (shape == QUAD_STRIP) {
      if (stroke && defaultEdges) inGeo.addQuadStripEdges();
      if (normalMode == NORMAL_MODE_AUTO) inGeo.calcQuadStripNormals();
      tessellator.tessellateQuadStrip();
    } else if (shape == POLYGON) {
      if (stroke && defaultEdges) inGeo.addPolygonEdges(mode == CLOSE);
      tessellator.tessellatePolygon(false, mode == CLOSE, normalMode == NORMAL_MODE_AUTO);      
    }
    
    int last = tessGeo.lastFillIndex;        
    if (textureImage0 != textureImage || texCache.count == 0) {
      texCache.addTexture(textureImage, first, last);
    } else {
      texCache.setLastIndex(last);
    }
  }
  
  public void flush() {    
    boolean hasPoints = 0 < tessGeo.pointVertexCount && 0 < tessGeo.pointIndexCount;
    boolean hasLines = 0 < tessGeo.lineVertexCount && 0 < tessGeo.lineIndexCount;
    boolean hasFill = 0 < tessGeo.fillVertexCount && 0 < tessGeo.fillIndexCount;
    
    if (hasPoints || hasLines || hasFill) {
      
      if (flushMode == FLUSH_WHEN_FULL && !hints[DISABLE_TRANSFORM_CACHE]) {
        // The modelview transformation has been applied already to the 
        // tessellated vertices, so we set the OpenGL modelview matrix as
        // the identity to avoid applying the model transformations twice.        
        pgl.pushMatrix();
        pgl.loadIdentity();
      }
      
      if (hasFill) {
        renderFill();
      }
      
      if (hasPoints) {
        renderPoints();
      } 

      if (hasLines) {
        renderLines();
      }          
      
      if (flushMode == FLUSH_WHEN_FULL && !hints[DISABLE_TRANSFORM_CACHE]) {
        pgl.popMatrix();
      }
    }
    
    tessGeo.reset();  
    texCache.reset();
  }
  

  protected void renderPoints() {
    if (!pointVBOsCreated) {
      createPointBuffers();
      pointVBOsCreated = true;
    }
    
    startPointShader();
    
    pgl.enableVertexArrays();
    pgl.enableColorArrays();
    pgl.enableNormalArrays();    
    
    int size = tessGeo.pointVertexCount;
   
    pgl.bindVertexBuffer(glPointVertexBufferID);
    pgl.copyVertexBufferData(tessGeo.pointVertices, 3 * size, vboMode);
    pgl.setVertexFormat(3, 0, 0); 
    
    pgl.bindVertexBuffer(glPointColorBufferID);
    pgl.copyVertexBufferData(tessGeo.pointColors, 4 * size, vboMode);    
    pgl.setColorFormat(4, 0, 0);
    
    pgl.bindVertexBuffer(glPointNormalBufferID);
    pgl.copyVertexBufferData(tessGeo.pointNormals, 3 * size, vboMode);    
    pgl.setNormalFormat(3, 0, 0);
    
    setupPointShader(glPointAttribBufferID, tessGeo.pointAttributes, size);
    
    size = tessGeo.pointIndexCount;
    pgl.bindIndexBuffer(glPointIndexBufferID);
    pgl.copyIndexBufferData(tessGeo.pointIndices, size, vboMode);
    pgl.renderIndexBuffer(size);
        
    pgl.unbindIndexBuffer();
    pgl.unbindVertexBuffer();
    
    pgl.disableVertexArrays();
    pgl.disableColorArrays();
    pgl.disableNormalArrays();    
    
    stopPointShader();
  }  
    
  protected void renderLines() {
    if (!lineVBOsCreated) {
      createLineBuffers();
      lineVBOsCreated = true;
    }
    
    startLineShader();
    
    pgl.enableVertexArrays();
    pgl.enableColorArrays();
    pgl.enableNormalArrays(); 
    
    int size = tessGeo.lineVertexCount;
    
    pgl.bindVertexBuffer(glLineVertexBufferID);
    pgl.copyVertexBufferData(tessGeo.lineVertices, 3 * size, vboMode);
    pgl.setVertexFormat(3, 0, 0);     
    
    pgl.bindVertexBuffer(glLineColorBufferID);
    pgl.copyVertexBufferData(tessGeo.lineColors, 4 * size, vboMode);    
    pgl.setColorFormat(4, 0, 0);    
    
    pgl.bindVertexBuffer(glLineNormalBufferID);
    pgl.copyVertexBufferData(tessGeo.lineNormals, 3 * size, vboMode);    
    pgl.setNormalFormat(3, 0, 0);    
    
    setupLineShader(glLineAttribBufferID, tessGeo.lineAttributes, size);
    
    size = tessGeo.lineIndexCount;
    pgl.bindIndexBuffer(glLineIndexBufferID);
    pgl.copyIndexBufferData(tessGeo.lineIndices, size, vboMode);
    pgl.renderIndexBuffer(size);    

    pgl.unbindIndexBuffer();
    pgl.unbindVertexBuffer();
    
    pgl.disableVertexArrays();
    pgl.disableColorArrays();
    pgl.disableNormalArrays();
    
    stopLineShader();
  }  
  
  
  protected void renderFill() {
    if (!fillVBOsCreated) {
      createFillBuffers();
      fillVBOsCreated = true;
    }    

    pgl.enableVertexArrays();
    pgl.enableColorArrays();
    pgl.enableNormalArrays();    
    
    int size = tessGeo.fillVertexCount;
    pgl.bindVertexBuffer(glFillVertexBufferID);
    pgl.copyVertexBufferData(tessGeo.fillVertices, 3 * size, vboMode);
    pgl.setVertexFormat(3, 0, 0);

    pgl.bindVertexBuffer(glFillColorBufferID);
    pgl.copyVertexBufferData(tessGeo.fillColors, 4 * size, vboMode);    
    pgl.setColorFormat(4, 0, 0);    
    
    pgl.bindVertexBuffer(glFillNormalBufferID);
    pgl.copyVertexBufferData(tessGeo.fillNormals, 3 * size, vboMode);    
    pgl.setNormalFormat(3, 0, 0);
    
    if (texCache.hasTexture) {
      pgl.enableTexCoordArrays();
      pgl.bindVertexBuffer(glFillTexCoordBufferID);
      pgl.copyVertexBufferData(tessGeo.fillTexcoords, 2 * size, vboMode);    
      pgl.setTexCoordFormat(2, 0, 0);            
    }  
         
    pgl.bindIndexBuffer(glFillIndexBufferID);  
    
    pgl.setActiveTexUnit(0);
    
    PTexture tex0 = null;
    for (int i = 0; i < texCache.count; i++) {
      PImage img = texCache.textures[i];
      PTexture tex = null;
      
      if (img != null) {
        tex = renderer.getTexture(img);
        if (tex != null) {                   
          tex.bind();          
          tex0 = tex;
        }        
      }
      if (tex == null && tex0 != null) {
        tex0.unbind();
        pgl.disableTexturing(tex0.glTarget);
      }
              
      int offset = texCache.firstIndex[i];
      size = texCache.lastIndex[i] - texCache.firstIndex[i] + 1;
      pgl.copyIndexBufferData(tessGeo.fillIndices, offset, size, vboMode);
      pgl.renderIndexBuffer(size);      
    }  
    
    if (texCache.hasTexture) {
      // Unbinding all the textures in the cache.      
      for (int i = 0; i < texCache.count; i++) {
        PImage img = texCache.textures[i];
        if (img != null) {
          PTexture tex = renderer.getTexture(img);
          if (tex != null) {
            tex.unbind();  
          }
        }
      }
      // Disabling texturing for each of the targets used
      // by textures in the cache.
      for (int i = 0; i < texCache.count; i++) {
        PImage img = texCache.textures[i];
        if (img != null) {
          PTexture tex = renderer.getTexture(img);
          if (tex != null) {
            pgl.disableTexturing(tex.glTarget);
          }          
        }
      }
      
      pgl.disableTexCoordArrays();
    }
    
    pgl.unbindIndexBuffer();
    pgl.unbindVertexBuffer();    
    
    pgl.disableVertexArrays();
    pgl.disableColorArrays();
    pgl.disableNormalArrays();    
  }
 
  // Utility function to render current tessellated geometry, under the assumption that
  // the texture is already bound.
  protected void renderTexFill() {
    if (!fillVBOsCreated) {
      createFillBuffers();
      fillVBOsCreated = true;
    }    

    pgl.enableVertexArrays();
    pgl.enableColorArrays();
    pgl.enableNormalArrays();    
    pgl.enableTexCoordArrays();
    
    int size = tessGeo.fillVertexCount;   
    pgl.bindVertexBuffer(glFillVertexBufferID);
    pgl.copyVertexBufferData(tessGeo.fillVertices, 3 * size, vboMode);
    pgl.setVertexFormat(3, 0, 0);
       
    pgl.bindVertexBuffer(glFillColorBufferID);
    pgl.copyVertexBufferData(tessGeo.fillColors, 4 * size, vboMode);    
    pgl.setColorFormat(4, 0, 0);    
    
    pgl.bindVertexBuffer(glFillNormalBufferID);
    pgl.copyVertexBufferData(tessGeo.fillNormals, 3 * size, vboMode);    
    pgl.setNormalFormat(3, 0, 0);
      
    pgl.bindVertexBuffer(glFillTexCoordBufferID);
    pgl.copyVertexBufferData(tessGeo.fillTexcoords, 2 * size, vboMode);    
    pgl.setTexCoordFormat(2, 0, 0);            

    size = tessGeo.fillIndexCount;
    pgl.bindIndexBuffer(glFillIndexBufferID);
    pgl.copyIndexBufferData(tessGeo.fillIndices, size, vboMode);
    pgl.renderIndexBuffer(size);      
  
    pgl.unbindIndexBuffer();
    pgl.unbindVertexBuffer();    
    
    pgl.disableTexCoordArrays();
    pgl.disableVertexArrays();
    pgl.disableColorArrays();
    pgl.disableNormalArrays();  
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
    lineShader.setVecUniform("viewport", viewport[0], viewport[1], viewport[2], viewport[3]);
    
    if (hints[ENABLE_PERSPECTIVE_CORRECTED_LINES]) {
      lineShader.setIntUniform("perspective", 1);
    } else {
      lineShader.setIntUniform("perspective", 0);
    }
    
    lineShader.setIntUniform("lights", lightCount);           
        
    lineShader.setVecUniform("eye", cameraEyeX, cameraEyeY, cameraEyeZ, 0);
    
    lineAttribsLoc = lineShader.getAttribLocation("attribs");
        
    pgl.enableAttribsArray(lineAttribsLoc);
    pgl.bindVertexBuffer(attrBufID);
    pgl.copyVertexBufferData(attribs, 4 * nvert, vboMode);
    pgl.setAttribsFormat(lineAttribsLoc, 4, 0, 0);
  }
  
  
  protected void setupLineShader(int attrBufID) {
    lineShader.setVecUniform("viewport", viewport[0], viewport[1], viewport[2], viewport[3]);

    if (hints[ENABLE_PERSPECTIVE_CORRECTED_LINES]) {
      lineShader.setIntUniform("perspective", 1);
    } else {
      lineShader.setIntUniform("perspective", 0);
    }    
    
    lineShader.setIntUniform("lights", lightCount);           
        
    lineShader.setVecUniform("eye", cameraEyeX, cameraEyeY, cameraEyeZ, 0);
    
    lineAttribsLoc = lineShader.getAttribLocation("attribs");     

    pgl.enableAttribsArray(lineAttribsLoc);
    pgl.bindVertexBuffer(attrBufID);
    pgl.setAttribsFormat(lineAttribsLoc, 4, 0, 0);    
  }
  
  
  protected void stopLineShader() {
    pgl.disableAttribsArray(lineAttribsLoc);
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
    
    pointAttribsLoc = PGraphicsOpenGL.pointShader.getAttribLocation("vertDisp");     

    pgl.enableAttribsArray(pointAttribsLoc);
    pgl.bindVertexBuffer(attrBufID);
    pgl.copyVertexBufferData(attribs, 2 * nvert, vboMode);
    pgl.setAttribsFormat(pointAttribsLoc, 2, 0, 0);    
  }
  
  
  protected void setupPointShader(int attrBufID) {
    pointShader.setIntUniform("lights", lightCount);           
    
    pointShader.setVecUniform("eye", cameraEyeX, cameraEyeY, cameraEyeZ, 0);
    
    pointAttribsLoc = PGraphicsOpenGL.pointShader.getAttribLocation("vertDisp");     

    pgl.enableAttribsArray(pointAttribsLoc);
    pgl.bindVertexBuffer(attrBufID);
    pgl.setAttribsFormat(pointAttribsLoc, 2, 0, 0);      
  }
  
  
  protected void stopPointShader() {
    pgl.disableAttribsArray(pointAttribsLoc);
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
    renderer.splineForward(detail, bezierDrawMatrix);

    // multiply the basis and forward diff matrices together
    // saves much time since this needn't be done for each curve
    bezierDrawMatrix.apply(renderer.bezierBasisMatrix);
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

    float x1 = inGeo.getlastVertexX();
    float y1 = inGeo.getlastVertexY();
    float z1 = inGeo.getlastVertexZ();

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
    float x1 = inGeo.getlastVertexX();
    float y1 = inGeo.getlastVertexY();
    float z1 = inGeo.getlastVertexZ();

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
    if (inGeo.vertexCount == 0) {
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
     inGeo.generateEllipse(ellipseMode, a, b, c, d, 
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
    pgl.getNumSamples(temp);    
    if (antialias != temp[0]) {
      antialias = temp[0];
      PApplet.println("Effective multisampling level: " + antialias);
    }
    
    if (antialias < 2) {
      pgl.enableMultisample();
      pgl.enablePointSmooth();
      pgl.enableLineSmooth();
      pgl.enablePolygonSmooth();
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
    
    pgl.disableMultisample();
    pgl.disablePointSmooth();
    pgl.disableLineSmooth();
    pgl.disablePolygonSmooth();    
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
    textTex = (PFontTexture)textFont.getCache(renderer);     
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
    pgl.pushMatrix();
    modelviewStack.push(new PMatrix3D(modelview));    
  }

  
  public void popMatrix() {
    if (hints[DISABLE_TRANSFORM_CACHE]) {
      flush();  
    }    
    pgl.popMatrix();
    PMatrix3D mat = modelviewStack.pop();
    modelview.set(mat);
  }
  
  
  //////////////////////////////////////////////////////////////

  // MATRIX TRANSFORMATIONS

  
  public void translate(float tx, float ty) {
    translate(tx, ty, 0);
  }

  
  public void translate(float tx, float ty, float tz) {
    if (hints[DISABLE_TRANSFORM_CACHE]) {
      flush();  
    }
    
    pgl.translate(tx, ty, tz);
    modelview.translate(tx, ty, tz);    
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
    if (hints[DISABLE_TRANSFORM_CACHE]) {
      flush();  
    }
           
    pgl.rotate(angle, v0, v1, v2);
    modelview.rotate(angle, v0, v1, v2);
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
    if (hints[DISABLE_TRANSFORM_CACHE]) {
      flush();  
    }
    
    pgl.scale(sx, sy, sz);
    modelview.scale(sx, sy, sz);
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
    pgl.loadIdentity();
    modelview.reset();
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
   * glMultMatrix().
   */
  public void applyMatrix(float n00, float n01, float n02, float n03,
                          float n10, float n11, float n12, float n13, 
                          float n20, float n21, float n22, float n23, 
                          float n30, float n31, float n32, float n33) {
    if (hints[DISABLE_TRANSFORM_CACHE]) {
      flush();  
    }    
    
    glMatrix[ 0] = n00; glMatrix[ 4] = n01; glMatrix[ 8] = n02; glMatrix[12] = n03;
    glMatrix[ 1] = n10; glMatrix[ 5] = n11; glMatrix[ 9] = n12; glMatrix[13] = n13;
    glMatrix[ 2] = n20; glMatrix[ 6] = n21; glMatrix[10] = n22; glMatrix[14] = n23;
    glMatrix[ 3] = n30; glMatrix[ 7] = n31; glMatrix[11] = n32; glMatrix[15] = n33;

    pgl.multMatrix(glMatrix);

    modelview.apply(n00, n01, n02, n03,
                    n10, n11, n12, n13,
                    n20, n21, n22, n23,
                    n30, n31, n32, n33);
  }

  
  protected void loadProjection() {
    pgl.setProjectionMode();    
    loadMatrix(projection);
    pgl.setModelviewMode();
  }
  
  
  protected void loadCamera() {
    pgl.setModelviewMode();
    loadMatrix(camera);
  }
  
  
  protected void loadModelview() {
    pgl.setModelviewMode();
    loadMatrix(modelview);  
  }
  
  
  protected void loadMatrix(PMatrix3D pMatrix) {
    glMatrix[ 0] = pMatrix.m00; glMatrix[ 4] = pMatrix.m01; glMatrix[ 8] = pMatrix.m02; glMatrix[12] = pMatrix.m03;
    glMatrix[ 1] = pMatrix.m10; glMatrix[ 5] = pMatrix.m11; glMatrix[ 9] = pMatrix.m12; glMatrix[13] = pMatrix.m13;
    glMatrix[ 2] = pMatrix.m20; glMatrix[ 6] = pMatrix.m21; glMatrix[10] = pMatrix.m22; glMatrix[14] = pMatrix.m23;
    glMatrix[ 3] = pMatrix.m30; glMatrix[ 7] = pMatrix.m31; glMatrix[11] = pMatrix.m32; glMatrix[15] = pMatrix.m33;
    
    pgl.loadMatrix(glMatrix);
  }  
  
  
  //////////////////////////////////////////////////////////////

  // MATRIX GET/SET/PRINT

  public PMatrix getMatrix() {
    return modelview.get();      
  }

  // public PMatrix2D getMatrix(PMatrix2D target)

  public PMatrix3D getMatrix(PMatrix3D target) {
    if (target == null) {
      target = new PMatrix3D();
    }
    target.set(modelview);
    return target;
  }

  // public void setMatrix(PMatrix source)

  public void setMatrix(PMatrix2D source) {
    resetMatrix();
    applyMatrix(source);
  }

  /**
   * Set the current transformation to the contents of the specified source.
   */
  public void setMatrix(PMatrix3D source) {
    resetMatrix();
    applyMatrix(source);
  }

  /**
   * Print the current model (or "transformation") matrix.
   */
  public void printMatrix() {      
    modelview.print();      
  }
  
  //////////////////////////////////////////////////////////////

  // PROJECTION
  
  
  public void pushProjection() {
    pgl.setProjectionMode();
    pgl.pushMatrix();
    projectionStack.push(new PMatrix3D(projection));
    pgl.setModelviewMode();
  }
  
  
  public void popProjection() {
    pgl.setProjectionMode();
    pgl.popMatrix();
    PMatrix3D mat = projectionStack.pop();
    projection.set(mat);        
    pgl.setModelviewMode();
  }

  
  public void applyProjection(PMatrix3D mat) {
    projection.apply(mat);
    loadProjection();
  }

  
  public void setProjection(PMatrix3D mat) {
    projection.set(mat);
    loadProjection();
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
   * matrix. That's because we set up a nice default camera transform in
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
    }    
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
    
    camera.set(modelview);
    cameraInv.set(camera);
    cameraInv.invert();
    
    // all done
    manipulatingCamera = false;
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
    if (hints[DISABLE_TRANSFORM_CACHE]) {
      flush();  
    }
    
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

    modelview.set(x0, x1, x2, 0,
                  y0, y1, y2, 0,
                  z0, z1, z2, 0,
                   0,  0,  0, 1);
    
    float tx = -eyeX;
    float ty = -eyeY;
    float tz = -eyeZ;
    modelview.translate(tx, ty, tz);
  
    loadModelview();
    
    camera.set(modelview);
    cameraInv.set(camera);
    cameraInv.invert();
  }
    
  
  /**
   * Print the current camera matrix.
   */
  public void printCamera() {
    camera.print();
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
    
    // The minus sign is needed to invert the Y axis.
    projection.set(x,  0, 0, tx,
                   0, -y, 0, ty,
                   0,  0, z, tz,
                   0,  0, 0,  1);
    
    loadProjection();
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

    // The minus sign in the temp / temp3 term is to invert the Y axis.
    projection.set(temp / temp2,              0,  (right + left) / temp2,                      0,
                              0,  -temp / temp3,  (top + bottom) / temp3,                      0,
                              0,              0, (-zfar - znear) / temp4, (-temp * zfar) / temp4,
                              0,              0,                      -1,                      1);
    
    loadProjection();
  }

  
  /**
   * Print the current projection matrix.
   */
  public void printProjection() {
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
    float ax = modelview.m00 * x + modelview.m01 * y + modelview.m02 * z + modelview.m03;
    float ay = modelview.m10 * x + modelview.m11 * y + modelview.m12 * z + modelview.m13;
    float az = modelview.m20 * x + modelview.m21 * y + modelview.m22 * z + modelview.m23;
    float aw = modelview.m30 * x + modelview.m31 * y + modelview.m32 * z + modelview.m33;

    float ox = projection.m00 * ax + projection.m01 * ay + projection.m02 * az + projection.m03 * aw;
    float ow = projection.m30 * ax + projection.m31 * ay + projection.m32 * az + projection.m33 * aw;
        
    if (ow != 0) {
      ox /= ow;
    }
    float sx = width * (1 + ox) / 2.0f;
    return sx;
  }

  
  public float screenY(float x, float y, float z) {        
    float ax = modelview.m00 * x + modelview.m01 * y + modelview.m02 * z + modelview.m03;
    float ay = modelview.m10 * x + modelview.m11 * y + modelview.m12 * z + modelview.m13;
    float az = modelview.m20 * x + modelview.m21 * y + modelview.m22 * z + modelview.m23;
    float aw = modelview.m30 * x + modelview.m31 * y + modelview.m32 * z + modelview.m33;
    
    float oy = projection.m10 * ax + projection.m11 * ay + projection.m12 * az + projection.m13 * aw;
    float ow = projection.m30 * ax + projection.m31 * ay + projection.m32 * az + projection.m33 * aw;
        
    if (ow != 0) {
      oy /= ow;
    }    
    float sy = height * (1 + oy) / 2.0f;
    // Turning value upside down because of Processing's inverted Y axis.
    sy = height - sy;
    return sy;
  }

  
  public float screenZ(float x, float y, float z) {   
    float ax = modelview.m00 * x + modelview.m01 * y + modelview.m02 * z + modelview.m03;
    float ay = modelview.m10 * x + modelview.m11 * y + modelview.m12 * z + modelview.m13;
    float az = modelview.m20 * x + modelview.m21 * y + modelview.m22 * z + modelview.m23;
    float aw = modelview.m30 * x + modelview.m31 * y + modelview.m32 * z + modelview.m33;
    
    float oz = projection.m20 * ax + projection.m21 * ay + projection.m22 * az + projection.m23 * aw;
    float ow = projection.m30 * ax + projection.m31 * ay + projection.m32 * az + projection.m33 * aw;    

    if (ow != 0) {
      oz /= ow;
    }
    float sz = (oz + 1) / 2.0f;
    return sz;
  }

  
  public float modelX(float x, float y, float z) {
    float ax = modelview.m00 * x + modelview.m01 * y + modelview.m02 * z + modelview.m03;
    float ay = modelview.m10 * x + modelview.m11 * y + modelview.m12 * z + modelview.m13;
    float az = modelview.m20 * x + modelview.m21 * y + modelview.m22 * z + modelview.m23;
    float aw = modelview.m30 * x + modelview.m31 * y + modelview.m32 * z + modelview.m33;
    
    float ox = cameraInv.m00 * ax + cameraInv.m01 * ay + cameraInv.m02 * az + cameraInv.m03 * aw;
    float ow = cameraInv.m30 * ax + cameraInv.m31 * ay + cameraInv.m32 * az + cameraInv.m33 * aw;    
    
    return (ow != 0) ? ox / ow : ox;
  }

  
  public float modelY(float x, float y, float z) {
    float ax = modelview.m00 * x + modelview.m01 * y + modelview.m02 * z + modelview.m03;
    float ay = modelview.m10 * x + modelview.m11 * y + modelview.m12 * z + modelview.m13;
    float az = modelview.m20 * x + modelview.m21 * y + modelview.m22 * z + modelview.m23;
    float aw = modelview.m30 * x + modelview.m31 * y + modelview.m32 * z + modelview.m33;
    
    float oy = cameraInv.m10 * ax + cameraInv.m11 * ay + cameraInv.m12 * az + cameraInv.m13 * aw;
    float ow = cameraInv.m30 * ax + cameraInv.m31 * ay + cameraInv.m32 * az + cameraInv.m33 * aw;

    return (ow != 0) ? oy / ow : oy;
  }

  
  public float modelZ(float x, float y, float z) {
    float ax = modelview.m00 * x + modelview.m01 * y + modelview.m02 * z + modelview.m03;
    float ay = modelview.m10 * x + modelview.m11 * y + modelview.m12 * z + modelview.m13;
    float az = modelview.m20 * x + modelview.m21 * y + modelview.m22 * z + modelview.m23;
    float aw = modelview.m30 * x + modelview.m31 * y + modelview.m32 * z + modelview.m33;
    
    float oz = cameraInv.m20 * ax + cameraInv.m21 * ay + cameraInv.m22 * az + cameraInv.m23 * aw;
    float ow = cameraInv.m30 * ax + cameraInv.m31 * ay + cameraInv.m32 * az + cameraInv.m33 * aw;

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
    pgl.setColor(fillR, fillG, fillB, fillA);
  } 
  
  protected void setTintColor() {
    pgl.setColor(tintR, tintG, tintB, tintA);
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
    pgl.setMaterialAmbient(colorFloats);
    
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
    pgl.setMaterialSpecular(colorFloats);
  }

  public void shininess(float shine) {
    super.shininess(shine);  
    pgl.setMaterialShininess(shine);
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
    pgl.setMaterialEmission(colorFloats);
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

  public void directionalLight(float r, float g, float b, float nx, float ny, float nz) {
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
    lightNormal[lightCount][0] = nx;
    lightNormal[lightCount][1] = ny;
    lightNormal[lightCount][2] = nz;
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
      pgl.enableLighting();
    }
  }

  protected void disableLighting() {
    if (lights) {
      // Flushing lit geometry.
      flush();
      
      lights = false;
      pgl.disableLighting();
    }
  }
    
  protected void lightAmbient(int num) {       
    pgl.setAmbientLight(num, lightDiffuse[num]);
    
  }

  protected void lightNoAmbient(int num) {
    pgl.setAmbientLight(num, zeroLight);
  }
  
  protected void lightNoSpot(int num) {
    pgl.setSpotLightCutoff(num, 180);
    pgl.setSpotLightExponent(num, 0);    
  }

  protected void lightDiffuse(int num) {
    pgl.setDiffuseLight(num, lightDiffuse[num]);
  }

  protected void lightNoDiffuse(int num) {
    pgl.setDiffuseLight(num, zeroLight);
  }
    
  protected void lightDirection(int num) {
    if (lightType[num] == DIRECTIONAL) {      
      pgl.setLightDirection(num, lightNormal[num]);      
    } else { // spotlight
      pgl.setSpotLightDirection(num, lightNormal[num]);      
    }
  }

  protected void lightEnable(int num) {
    pgl.enableLight(num);
  }

  protected void lightDisable(int num) {
    pgl.disableLight(num);
  }

  protected void lightFalloff(int num) {
    pgl.setLightConstantAttenuation(num, lightFalloffConstant[num]);
    pgl.setLightLinearAttenuation(num, lightFalloffLinear[num]);
    pgl.setLightQuadraticAttenuation(num, lightFalloffQuadratic[num]);
  }

  protected void lightPosition(int num) {
    pgl.setLightPosition(num, lightPosition[num]);
  }

  protected void lightSpecular(int num) {
    pgl.setSpecularLight(num, lightSpecular[num]);
  }

  protected void lightNoSpecular(int num) {
    pgl.setSpecularLight(num, zeroLight);
  }
  
  protected void lightSpotAngle(int num) {
    pgl.setSpotLightCutoff(num, lightSpotAngle[num]);
  }

  protected void lightSpotConcentration(int num) {
    pgl.setSpotLightExponent(num, lightSpotConcentration[num]);    
  }  
  
  //////////////////////////////////////////////////////////////

  // BACKGROUND

  protected void backgroundImpl(PImage image) {
    backgroundImpl();
    set(0, 0, image);
  }

  protected void backgroundImpl() {
    tessGeo.reset();  
    texCache.reset();
    
    pgl.setClearColor(0, 0, 0, 0);
    pgl.clearDepthBuffer();

    pgl.setClearColor(backgroundR, backgroundG, backgroundB, 1);
    pgl.clearColorBuffer();    
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
      int err = pgl.getError();
      if (err != 0) {
        //String errString = glu.gluErrorString(err);
        String errString = pgl.getErrorString(err);
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
    flush();
    
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
    if (primarySurface) {
      pgl.setReadBuffer(PGL.FRONT);
    }
    pgl.readPixels(pixelBuffer, 0, 0, width, height);
    
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
      this.setCache(renderer, texture);
      this.setParams(renderer, params);
      
      texCrop = new int[4];
      texCrop[0] = 0;
      texCrop[1] = 0;
      texCrop[2] = width;
      texCrop[3] = height;     
    }
  }   
  
  // Draws wherever it is in the screen texture right now to the screen.
  public void updateTexture() {
    flush();
    
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
    pgl.finish();
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
    flush();
    
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
    if (primarySurface) {
      pgl.setReadBuffer(PGL.FRONT);
    }
    pgl.readPixels(getsetBuffer, x, height - y - 1, 1, 1);

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
    flush();
    
    PImage newbie = parent.createImage(w, h, ARGB);

    IntBuffer newbieBuffer = IntBuffer.allocate(w * h);    
    
    boolean notCurrent = !primarySurface && offscreenFramebuffer != currentFramebuffer;
    if (notCurrent) {
      pushFramebuffer();
      setFramebuffer(offscreenFramebuffer);
    }    

    newbieBuffer.rewind();
    if (primarySurface) {
      pgl.setReadBuffer(PGL.FRONT);
    }
    pgl.readPixels(newbieBuffer, x, height - y - h, w, h);

    if (notCurrent) {
      popFramebuffer();
    }    
    
    newbie.loadPixels();
    newbieBuffer.get(newbie.pixels);
    nativeToJavaARGB(newbie);
    return newbie;
  }
  
  public PImage get() {
    return get(0, 0, width, height);
  }

  // TODO: doesn't appear to work
  public void set(int x, int y, int argb) {
    flush();
    
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
    flush();
    
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
      pgl.enableBlend();
      
      if (mode == REPLACE) {
        // This is equivalent to disable blending.
        if (blendEqSupported) {
          pgl.setBlendEquation(PGL.BLEND_EQ_ADD);
        }
        pgl.setReplaceBlend();        
      } else if (mode == BLEND) {
        if (blendEqSupported) {
          pgl.setBlendEquation(PGL.BLEND_EQ_ADD);
        }
        pgl.setDefaultBlend();
      } else if (mode == ADD) {
        if (blendEqSupported) {
          pgl.setBlendEquation(PGL.BLEND_EQ_ADD);
        }
        pgl.setAdditiveBlend();
      } else if (mode == SUBTRACT) {
        if (blendEqSupported) {
          pgl.setBlendEquation(PGL.BLEND_EQ_ADD);
        }
        pgl.setSubstractiveBlend();
      } else if (mode == LIGHTEST) {
        if (blendEqSupported) { 
          pgl.setBlendEquation(PGL.BLEND_EQ_MAX);
          pgl.setLightestBlend();
        } else {
          PGraphics.showWarning("P3D: This blend mode is currently unsupported.");
        }
      } else if (mode == DARKEST) {
        if (blendEqSupported) { 
          pgl.setBlendEquation(PGL.BLEND_EQ_MIN);
          pgl.setDarkestBlend();
        } else {
          PGraphics.showWarning("P3D: This blend mode is currently unsupported.");  
        }
      } else if (mode == DIFFERENCE) {
        if (blendEqSupported) {
          pgl.setBlendEquation(PGL.BLEND_EQ_REVERSE_SUBTRACT);
          pgl.setDifferenceBlend();
          
        } else {
          PGraphics.showWarning("P3D: This blend mode is currently unsupported.");
        }       
      } else if (mode == EXCLUSION) {
        if (blendEqSupported) {
          pgl.setBlendEquation(PGL.BLEND_EQ_ADD);
        }
        pgl.setExclussionBlend();
      } else if (mode == MULTIPLY) {
        if (blendEqSupported) {
          pgl.setBlendEquation(PGL.BLEND_EQ_ADD);
        }
        pgl.setMultiplyBlend();
      } else if (mode == SCREEN) {
        if (blendEqSupported) {
          pgl.setBlendEquation(PGL.BLEND_EQ_ADD);
        }
        pgl.setScreenBlend();
      }  
      // HARD_LIGHT, SOFT_LIGHT, OVERLAY, DODGE, BURN modes cannot be implemented
      // in fixed-function pipeline because they require conditional blending and
      // non-linear blending equations.
    }
  }

  protected void setDefaultBlend() {
    blendMode = BLEND;
    pgl.enableBlend();
    if (blendEqSupported) {
      pgl.setBlendEquation(PGL.BLEND_EQ_ADD);
    }
    pgl.setDefaultBlend();
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
    PTexture tex = (PTexture)img.getCache(renderer);
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
   * This utility method returns the texture associated to the image,
   * but it doesn't create a new texture if the image has no texture.
   * @param img the image to have a texture metadata associated to it
   */      
  protected PTexture queryTexture(PImage img) {
    PTexture tex = (PTexture)img.getCache(renderer);
    return tex;
  }
  
  
  /**
   * This utility method creates a texture for the provided image, and adds it
   * to the metadata cache of the image.
   * @param img the image to have a texture metadata associated to it
   */
  protected PTexture addTexture(PImage img) {
    PTexture.Parameters params = (PTexture.Parameters)img.getParams(renderer);
    if (params == null) {
      params = PTexture.newParameters();
      img.setParams(renderer, params);
    }
    PTexture tex = new PTexture(img.parent, img.width, img.height, params);    
    img.loadPixels();    
    if (img.pixels != null) tex.set(img.pixels);
    img.setCache(renderer, tex);
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
    img.setCache(renderer, tex);
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
  
  
  /** Utility function to render texture without blend. */
  protected void drawTexture(int target, int id, int tw, int th, int[] crop, int x, int y, int w, int h) {
    pgl.enableTexturing(target);
    pgl.bindTexture(target, id);
    
    int savedBlendMode = blendMode;
    blendMode(REPLACE); 
    
    // The texels of the texture replace the color of wherever is on the screen.      
    pgl.setTexEnvironmentMode(PGL.REPLACE);
    
    drawTexture(tw, th, crop, x, y, w, h);
    
    // Returning to the default texture environment mode, GL_MODULATE. This allows tinting a texture
    // with the current fragment color.       
    pgl.setTexEnvironmentMode(PGL.MODULATE);
    
    pgl.unbindTexture(target);
    pgl.disableTexturing(target);
    
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
    flush();
    
    pgl.disableDepthMask();

    pgl.setProjectionMode();
    pgl.pushMatrix();
    pgl.loadIdentity();
    pgl.setOrthographicProjection(0, width, 0, height, -1, 1);
    
    pgl.setModelviewMode();
    pgl.pushMatrix();
    pgl.loadIdentity();

    pgl.translate(x, y, 0);
    pgl.scale(w, h, 1);
    // Rendering the quad with the appropriate texture coordinates needed for the
    // specified crop region
    float s0 = (float)crop[0] / tw;
    float s1 = (float)(crop[0] + crop[2]) / tw;    
    float t0 = (float)crop[1] / th;
    float t1 = (float)(crop[1] + crop[3]) / th;
    drawTexQuad(s0, t0, s1, t1);

    // Restoring matrices.
    pgl.setProjectionMode();
    pgl.popMatrix();
    pgl.setModelviewMode();
    pgl.popMatrix();
    
    if (hintEnabled(ENABLE_DEPTH_MASK)) {
      pgl.enableDepthMask();
    }       
  }
  
  
  protected void drawTexQuad() {
    drawTexQuad(0, 0, 1, 1);
  }
  
  /** 
   * Pushes a normalized (1x1) textured quad to the GPU.
   */
  protected void drawTexQuad(float u0, float v0, float u1, float v1) {
    // TODO: need to test...
    stroke = false;
    beginShape(QUAD);
    vertex(0, 0, u0, v0);
    vertex(1, 0, u1, v0);
    vertex(1, 1, u1, v1);
    vertex(0, 1, u0, v1);
    endShape();
    tessellate(OPEN);    
    renderTexFill();   
  }  
  
  
  /** 
   * Utility function to copy buffer to texture.
   */
  protected void copyToTexture(PTexture tex, IntBuffer buffer, int x, int y, int w, int h) {    
    buffer.rewind();    
    pgl.enableTexturing(tex.glTarget);
    pgl.bindTexture(tex.glTarget, tex.glID);    
    pgl.copyTexSubImage(buffer, tex.glTarget, x, y, w, h);
    pgl.bindTexture(tex.glTarget, 0);
    pgl.disableTexturing(tex.glTarget);
  }   

  //////////////////////////////////////////////////////////////
  
  // OPENGL ROUTINES    
  
  protected void setSurfaceParams() {
    // The default shade model is GL_SMOOTH, but we set
    // here just in case...
    pgl.setShadeModel(PGL.SMOOTH);
    
    
    // The ambient and diffuse components for each vertex are taken
    // from the glColor/color buffer setting:
    pgl.enableColorMaterial();
    
    // For a quick overview of how the lighting model works in OpenGL
    // see this page:
    // http://www.sjbaker.org/steve/omniv/opengl_lighting.html
    
    // Some normal related settings:
    pgl.enableNormalization();
    pgl.enableRescaleNormals();    
    
    // Light model defaults:
    // The default opengl ambient light is (0.2, 0.2, 0.2), so
    // here we set our own default value.
    pgl.setTwoSidedLightModel();
    pgl.setDefaultAmbientLight(baseLight);    
  }  
  
  protected void saveGLMatrices() {
    pgl.setProjectionMode();
    pgl.pushMatrix();
    pgl.setModelviewMode();
    pgl.pushMatrix();    
  }

  protected void restoreGLMatrices() {
    pgl.setProjectionMode();
    pgl.popMatrix();
    pgl.setModelviewMode();
    pgl.popMatrix();
  }  
  
    
  protected void setDefNormals(float nx, float ny, float nz) { 
    pgl.setNormal(nx, ny, nz);  
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
    
    renderer = this;
    
    profile = null;      
    
    profile = GLProfile.getDefault();
    
    //profile = GLProfile.get(GLProfile.GL2ES1);    
    //profile = GLProfile.get(GLProfile.GL4bc);
    //profile = GLProfile.getMaxProgrammable();    
    pgl.pipeline = PGL.FIXED; 

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
    renderer = (PGraphicsOpenGL)parent.g;
    
    context = renderer.getContext();
    capabilities = renderer.getCapabilities();
    drawable = null;
    
    updateGLInterfaces();
    loadTextureImpl(BILINEAR);
    
    // In case of reinitialization (for example, when the smooth level
    // is changed), we make sure that all the OpenGL resources associated
    // to the surface are released by calling delete().
    if (offscreenFramebuffer != null) {
      offscreenFramebuffer = null;
    }
    if (offscreenFramebufferMultisample != null) {
      offscreenFramebufferMultisample = null;
    }
    
    // We need the GL2GL3 profile to access the glRenderbufferStorageMultisample
    // function used in multisampled (antialiased) offscreen rendering.        
    if (PGraphicsOpenGL.fboMultisampleSupported && pgl.gl2x != null && 1 < antialias) {
      int nsamples = antialias;
      offscreenFramebufferMultisample = new PFramebuffer(parent, texture.glWidth, texture.glHeight, nsamples, 0, 
                                                         offscreenDepthBits, offscreenStencilBits, 
                                                         offscreenDepthBits == 24 && offscreenStencilBits == 8, false);
      
      
      
      offscreenFramebufferMultisample.clear();
      offscreenMultisample = true;
      
      renderer.report("after cleaning fbm");
      
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
    context = renderer.getContext();
    capabilities = renderer.getCapabilities();
    drawable = null;    
    
    updateGLInterfaces();
  }  
  
  protected void getGLParameters() {
    OPENGL_VENDOR = pgl.getVendorString();  
    OPENGL_RENDERER = pgl.getRendererString();
    OPENGL_VERSION = pgl.getVersionString();    
    OPENGL_EXTENSIONS = pgl.getExtensionsString();
  
    npotTexSupported = pgl.isNpotTexSupported();
    mipmapGeneration = pgl.hasMipmapGeneration();
    matrixGetSupported = pgl.isMatrixGetSupported();
    texenvCrossbarSupported = pgl.isTexenvCrossbarSupported();
    vboSupported = pgl.isVboSupported();
    fboSupported = pgl.isFboSupported();
    fboMultisampleSupported = pgl.isFboMultisampleSupported();
    blendEqSupported = pgl.isBlendEqSupported(); 
    
    maxTextureSize = pgl.getMaxTexureSize();  
    maxLineWidth = pgl.getMaxAliasedLineWidth();        
    maxPointSize = pgl.getMaxAliasedPointSize();        
    maxTextureUnits = pgl.getMaxTextureUnits();
    
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
    
  public InGeometry newInGeometry() {
    return new InGeometry(); 
  }
  
  protected TessGeometry newTessGeometry(int mode) {
    return new TessGeometry(mode);
  }
  
  protected TexCache newTexCache() {
    return new TexCache();
  }
  
  public class TexCache {
    int count;
    PImage[] textures;
    int[] firstIndex;
    int[] lastIndex;
    boolean hasTexture;
    
    public TexCache() {
      allocate();
    }
    
    public void allocate() {
      textures = new PImage[DEFAULT_IN_TEXTURES];
      firstIndex = new int[DEFAULT_IN_TEXTURES];
      lastIndex = new int[DEFAULT_IN_TEXTURES];
      count = 0;
      hasTexture = false;
    }
    
    public void reset() {
      java.util.Arrays.fill(textures, 0, count, null);
      count = 0;
      hasTexture = false;
    }
    
    public void dispose() {
      textures = null;
      firstIndex = null;
      lastIndex = null;      
    }
    
    public void addTexture(PImage img, int first, int last) {
      textureCheck();
      
      textures[count] = img;
      firstIndex[count] = first;
      lastIndex[count] = last;
      
      // At least one non-null texture since last reset.
      hasTexture |= img != null;
      
      count++;
    }
    
    public void setLastIndex(int last) {
      lastIndex[count - 1] = last;
    }
    
    public void textureCheck() {
      if (count == textures.length) {
        int newSize = count << 1;  

        expandTextures(newSize);
        expandFirstIndex(newSize);
        expandLastIndex(newSize);
      }
    }
    
    public void expandTextures(int n) {
      PImage[] temp = new PImage[n];      
      PApplet.arrayCopy(textures, 0, temp, 0, count);
      textures = temp;          
    }
        
    public void expandFirstIndex(int n) {
      int[] temp = new int[n];      
      PApplet.arrayCopy(firstIndex, 0, temp, 0, count);
      firstIndex = temp;      
    }
    
    public void expandLastIndex(int n) {
      int[] temp = new int[n];      
      PApplet.arrayCopy(lastIndex, 0, temp, 0, count);
      lastIndex = temp;      
    }    
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
    
    public void calcTriangleNormal(int i0, int i1, int i2) {
      int index;
      
      index = 3 * i0;
      float x0 = vertices[index++];
      float y0 = vertices[index++];
      float z0 = vertices[index  ];

      index = 3 * i1;
      float x1 = vertices[index++];
      float y1 = vertices[index++];
      float z1 = vertices[index  ];

      index = 3 * i2;
      float x2 = vertices[index++];
      float y2 = vertices[index++];
      float z2 = vertices[index  ];
      
      float v12x = x2 - x1;
      float v12y = y2 - y1;
      float v12z = z2 - z1;
      
      float v10x = x0 - x1;
      float v10y = y0 - y1;
      float v10z = z0 - z1;
      
      float nx = v12y * v10z - v10y * v12z;
      float ny = v12z * v10x - v10z * v12x;
      float nz = v12x * v10y - v10x * v12y;
      float d = PApplet.sqrt(nx * nx + ny * ny + nz * nz);
      nx /= d;
      ny /= d;
      nz /= d;
      
      index = 3 * i0;
      normals[index++] = nx;
      normals[index++] = ny;
      normals[index  ] = nz;

      index = 3 * i1;
      normals[index++] = nx;
      normals[index++] = ny;
      normals[index  ] = nz;

      index = 3 * i2;
      normals[index++] = nx;
      normals[index++] = ny;
      normals[index  ] = nz;      
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

    public void calcPointsNormals() {
      // TODO 
    }    
    
    public void calcLinesNormals() {
      // TODO
    }
        
    public void calcTrianglesNormals() {
      for (int i = 0; i < (lastVertex - firstVertex + 1) / 3; i++) {
        int i0 = 3 * i + 0;
        int i1 = 3 * i + 1;
        int i2 = 3 * i + 2;
        
        calcTriangleNormal(i0, i1, i2);
      }      
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

    public void calcTriangleFanNormals() {
      for (int i = firstVertex + 1; i < lastVertex; i++) {
        int i0 = firstVertex;
        int i1 = i;
        int i2 = i + 1;
        
        calcTriangleNormal(i0, i1, i2);
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
    
    public void calcTriangleStripNormals() {
      for (int i = firstVertex + 1; i < lastVertex; i++) {
        int i1 = i;
        int i0, i2;
        if (i % 2 == 0) {
          i0 = i + 1;
          i2 = i - 1;                  
        } else {
          i0 = i - 1;
          i2 = i + 1;             
        }
        calcTriangleNormal(i0, i1, i2);
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
    
    public void calcQuadsNormals() {
      for (int i = 0; i < (lastVertex - firstVertex + 1) / 4; i++) {
        int i0 = 4 * i + 0;
        int i1 = 4 * i + 1;
        int i2 = 4 * i + 2;
        int i3 = 4 * i + 3;
        
        calcTriangleNormal(i0, i1, i2);
        calcTriangleNormal(i2, i3, i0);
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
    
    public void calcQuadStripNormals() {
      for (int qd = 1; qd < (lastVertex - firstVertex + 1) / 2; qd++) {
        int i0 = firstVertex + 2 * (qd - 1);
        int i1 = firstVertex + 2 * (qd - 1) + 1;
        int i2 = firstVertex + 2 * qd + 1;
        int i3 = firstVertex + 2 * qd;     

        calcTriangleNormal(i0, i1, i3);
        calcTriangleNormal(i3, i2, i0);
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
  
  public class TessGeometry {
    int renderMode;
    
    // Tessellated fill data
    public int fillVertexCount;
    public int firstFillVertex;
    public int lastFillVertex;    
    public float[] fillVertices;
    public float[] fillColors;
    public float[] fillNormals;
    public float[] fillTexcoords;
    
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
      lastFillIndex = fillIndexCount - 1;
    }

    public void calcFillNormal(int i0, int i1, int i2) {
      int index;
      
      index = 3 * i0;
      float x0 = fillVertices[index++];
      float y0 = fillVertices[index++];
      float z0 = fillVertices[index  ];

      index = 3 * i1;
      float x1 = fillVertices[index++];
      float y1 = fillVertices[index++];
      float z1 = fillVertices[index  ];

      index = 3 * i2;
      float x2 = fillVertices[index++];
      float y2 = fillVertices[index++];
      float z2 = fillVertices[index  ];
      
      float v12x = x2 - x1;
      float v12y = y2 - y1;
      float v12z = z2 - z1;
      
      float v10x = x0 - x1;
      float v10y = y0 - y1;
      float v10z = z0 - z1;
      
      float nx = v12y * v10z - v10y * v12z;
      float ny = v12z * v10x - v10z * v12x;
      float nz = v12x * v10y - v10x * v12y;
      float d = PApplet.sqrt(nx * nx + ny * ny + nz * nz);
      nx /= d;
      ny /= d;
      nz /= d;
      
      index = 3 * i0;
      fillNormals[index++] = nx;
      fillNormals[index++] = ny;
      fillNormals[index  ] = nz;

      index = 3 * i1;
      fillNormals[index++] = nx;
      fillNormals[index++] = ny;
      fillNormals[index  ] = nz;

      index = 3 * i2;
      fillNormals[index++] = nx;
      fillNormals[index++] = ny;
      fillNormals[index  ] = nz;      
      
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
      
      if (renderMode == IMMEDIATE && flushMode == FLUSH_WHEN_FULL && !hints[DISABLE_TRANSFORM_CACHE]) {
        PMatrix3D tr = modelview;
        
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
      
      if (renderMode == IMMEDIATE && flushMode == FLUSH_WHEN_FULL && !hints[DISABLE_TRANSFORM_CACHE]) {
        PMatrix3D tr = modelview;
        
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
        if (nvert <= MIN_ARRAYCOPY_SIZE) {
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
        
      if (nvert <= MIN_ARRAYCOPY_SIZE) {
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
          fillTexcoords[index++] = u;
          fillTexcoords[index  ] = v;            
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
      
      if (renderMode == IMMEDIATE && flushMode == FLUSH_WHEN_FULL && !hints[DISABLE_TRANSFORM_CACHE]) {
        PMatrix3D tr = modelview;
        
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
      
      if (renderMode == IMMEDIATE && flushMode == FLUSH_WHEN_FULL && !hints[DISABLE_TRANSFORM_CACHE]) {
        PMatrix3D tr = modelview;
        
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
    
    public void center(float cx, float cy) {
      int index;
      
      // Computing current center
      float cx0 = 0;
      float cy0 = 0;
      for (int i = 0; i < fillVertexCount; i++) {
        index = 3 * i;
        cx0 += fillVertices[index++];
        cy0 += fillVertices[index  ];
      }
      for (int i = 0; i < lineVertexCount; i++) {
        index = 3 * i;
        cx0 += lineVertices[index++];
        cy0 += lineVertices[index  ];        
      }
      for (int i = 0; i < pointVertexCount; i++) {
        index = 3 * i;
        cx0 += pointVertices[index++];
        cy0 += pointVertices[index  ];          
      }      
      int nt = fillVertexCount + lineVertexCount + pointVertexCount;
      if (0 < nt) { 
        cx0 /= nt;
        cy0 /= nt;
      }

      float tx = cx - cx0;
      float ty = cy - cy0;
      
      if (0 < fillVertexCount) {
        for (int i = 0; i < fillVertexCount; i++) {
          index = 3 * i;
          fillVertices[index++] += tx;
          fillVertices[index  ] += ty;
        }        
      }
      
      if (0 < lineVertexCount) {
        for (int i = 0; i < lineVertexCount; i++) {
          index = 3 * i;
          lineVertices[index++] += tx;
          lineVertices[index  ] += ty;
          
          index = 4 * i;
          lineAttributes[index++] += tx;
          lineAttributes[index  ] += ty;           
        }
      }
      
      if (0 < pointVertexCount) {
        for (int i = 0; i < pointVertexCount; i++) {
          index = 3 * i;
          pointVertices[index++] += tx;
          pointVertices[index  ] += ty;
        }        
      }      
    }
    
    public void center(float cx, float cy, float cz) {
      int index;
      
      // Computing current center
      float cx0 = 0;
      float cy0 = 0;
      float cz0 = 0;      
      for (int i = 0; i < fillVertexCount; i++) {
        index = 3 * i;
        cx0 += fillVertices[index++];
        cy0 += fillVertices[index++];
        cz0 += fillVertices[index  ];
      }
      for (int i = 0; i < lineVertexCount; i++) {
        index = 3 * i;
        cx0 += lineVertices[index++];
        cy0 += lineVertices[index++];
        cz0 += lineVertices[index  ];        
      }
      for (int i = 0; i < pointVertexCount; i++) {
        index = 3 * i;
        cx0 += pointVertices[index++];
        cy0 += pointVertices[index++];
        cz0 += pointVertices[index  ];          
      }      
      int nt = fillVertexCount + lineVertexCount + pointVertexCount;
      if (0 < nt) { 
        cx0 /= nt;
        cy0 /= nt;
        cz0 /= nt;
      }

      float tx = cx - cx0;
      float ty = cy - cy0;
      float tz = cz - cz0;      
      
      if (0 < fillVertexCount) {
        for (int i = 0; i < fillVertexCount; i++) {
          index = 3 * i;
          fillVertices[index++] += tx;
          fillVertices[index++] += ty;
          fillVertices[index  ] += tz;
        }        
      }
      
      if (0 < lineVertexCount) {
        for (int i = 0; i < lineVertexCount; i++) {
          index = 3 * i;
          lineVertices[index++] += tx;
          lineVertices[index++] += ty;
          lineVertices[index  ] += tz;
          
          index = 4 * i;
          lineAttributes[index++] += tx;
          lineAttributes[index++] += ty;
          lineAttributes[index  ] += tz;           
        }
      }
      
      if (0 < pointVertexCount) {
        for (int i = 0; i < pointVertexCount; i++) {
          index = 3 * i;
          pointVertices[index++] += tx;
          pointVertices[index++] += ty;
          pointVertices[index  ] += tz;
        }        
      }
    }
    
    public int getCenter(PVector v) {
      int index;
      for (int i = 0; i < fillVertexCount; i++) {
        index = 3 * i;
        v.x += fillVertices[index++];
        v.y += fillVertices[index++];
        v.z += fillVertices[index  ];
      }
      for (int i = 0; i < lineVertexCount; i++) {
        index = 3 * i;
        v.x += lineVertices[index++];
        v.y += lineVertices[index++];
        v.z += lineVertices[index  ];        
      }
      for (int i = 0; i < pointVertexCount; i++) {
        index = 3 * i;
        v.x += pointVertices[index++];
        v.y += pointVertices[index++];
        v.z += pointVertices[index  ];          
      }      
      return fillVertexCount + lineVertexCount + pointVertexCount;
    }
    
    public void applyMatrix(PMatrix2D tr) {
      if (0 < fillVertexCount) {
        int index;
          
        for (int i = 0; i < fillVertexCount; i++) {
          index = 3 * i;
          float x = fillVertices[index++];
          float y = fillVertices[index  ];
        
          index = 3 * i;
          float nx = fillNormals[index++];
          float ny = fillNormals[index  ];

          index = 3 * i;
          fillVertices[index++] = x * tr.m00 + y * tr.m01 + tr.m02;
          fillVertices[index  ] = x * tr.m10 + y * tr.m11 + tr.m12;
        
          index = 3 * i;
          fillNormals[index++] = nx * tr.m00 + ny * tr.m01;
          fillNormals[index  ] = nx * tr.m10 + ny * tr.m11;          
        }
      }

      if (0 < lineVertexCount) {
        int index;
        
        for (int i = 0; i < lineVertexCount; i++) {
          index = 3 * i;
          float x = lineVertices[index++];
          float y = lineVertices[index  ];
        
          index = 3 * i;
          float nx = lineNormals[index++];
          float ny = lineNormals[index  ];

          index = 4 * i;
          float xa = lineAttributes[index++];
          float ya = lineAttributes[index  ];
                    
          index = 3 * i;
          lineVertices[index++] = x * tr.m00 + y * tr.m01 + tr.m02;
          lineVertices[index  ] = x * tr.m10 + y * tr.m11 + tr.m12;
        
          index = 3 * i;
          lineNormals[index++] = nx * tr.m00 + ny * tr.m01;
          lineNormals[index  ] = nx * tr.m10 + ny * tr.m11;
          
          index = 4 * i;
          lineAttributes[index++] = xa * tr.m00 + ya * tr.m01 + tr.m02;
          lineAttributes[index  ] = xa * tr.m10 + ya * tr.m11 + tr.m12;              
        }   
      }      
      
      if (0 < pointVertexCount) {
        int index;
       
        for (int i = 0; i < pointVertexCount; i++) {
          index = 3 * i;
          float x = pointVertices[index++];
          float y = pointVertices[index  ];
        
          index = 3 * i;
          float nx = pointNormals[index++];
          float ny = pointNormals[index  ];
                    
          index = 3 * i;
          pointVertices[index++] = x * tr.m00 + y * tr.m01 + tr.m02;
          pointVertices[index  ] = x * tr.m10 + y * tr.m11 + tr.m12;
          
          index = 3 * i;
          pointNormals[index++] = nx * tr.m00 + ny * tr.m01;
          pointNormals[index  ] = nx * tr.m10 + ny * tr.m11;
        } 
      }       
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
    InGeometry in; 
    TessGeometry tess;
    GLU glu;
    GLUtessellator gluTess;
    GLUTessCallback tessCallback;
    
    boolean fill;
    boolean stroke;
    float strokeWeight;
    int strokeJoin;
    int strokeCap;
    float strokeRed, strokeGreen, strokeBlue, strokeAlpha;
    int bezierDetil = 20;
    
    public Tessellator() {
      glu = new GLU();
      
      gluTess = GLU.gluNewTess();
    
      tessCallback = new GLUTessCallback();
      GLU.gluTessCallback(gluTess, GLU.GLU_TESS_BEGIN, tessCallback);
      GLU.gluTessCallback(gluTess, GLU.GLU_TESS_END, tessCallback);
      GLU.gluTessCallback(gluTess, GLU.GLU_TESS_VERTEX, tessCallback);
      GLU.gluTessCallback(gluTess, GLU.GLU_TESS_COMBINE, tessCallback);
      GLU.gluTessCallback(gluTess, GLU.GLU_TESS_ERROR, tessCallback);    
      
      bezierDetil = 20;
    }

    public void setInGeometry(InGeometry in) {
      this.in = in;
    }

    public void setTessGeometry(TessGeometry tess) {
      this.tess = tess;
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
      int nInVert = in.lastVertex - in.firstVertex + 1;
      
      if (stroke && 1 <= nInVert) {
        tess.isStroked = true;
        
        // Each point generates a separate triangle fan. 
        // The number of triangles of each fan depends on the
        // stroke weight of the point.
        int nvertTot = 0;
        int nindTot = 0;
        for (int i = in.firstVertex; i <= in.lastVertex; i++) {
          int perim = PApplet.max(MIN_ACCURACY, (int) (TWO_PI * strokeWeight / 20));
          // Number of points along the perimeter plus the center point.
          int nvert = perim + 1;
          nvertTot += nvert; 
          nindTot += 3 * (nvert - 1);
        }
        
        tess.addPointVertices(nvertTot);
        tess.addPointIndices(nindTot);
        int vertIdx = tess.firstPointVertex;
        int attribIdx = tess.firstPointVertex;
        int indIdx = tess.firstPointIndex;      
        int firstVert = tess.firstPointVertex;      
        for (int i = in.firstVertex; i <= in.lastVertex; i++) {
          // Creating the triangle fan for each input vertex.
          int perim = PApplet.max(MIN_ACCURACY, (int) (TWO_PI * strokeWeight / 20));
          int nvert = perim + 1;
          
          // All the tessellated vertices are identical to the center point
          for (int k = 0; k < nvert; k++) {
            tess.putPointVertex(in, i, vertIdx);
            vertIdx++; 
          }       
          
          // The attributes for each tessellated vertex are the displacement along
          // the circle perimeter. The point shader will read these attributes and
          // displace the vertices in screen coordinates so the circles are always
          // camera facing (bilboards)
          tess.pointAttributes[2 * attribIdx + 0] = 0;
          tess.pointAttributes[2 * attribIdx + 1] = 0;
          attribIdx++;
          float val = 0;
          float inc = (float) SINCOS_LENGTH / perim;      
          for (int k = 0; k < perim; k++) {
            tess.pointAttributes[2 * attribIdx + 0] = 0.5f * cosLUT[(int) val] * strokeWeight;
            tess.pointAttributes[2 * attribIdx + 1] = 0.5f * sinLUT[(int) val] * strokeWeight;
            val = (val + inc) % SINCOS_LENGTH;                
            attribIdx++;           
          }
          
          // Adding vert0 to take into account the triangles of all
          // the preceding points.
          for (int k = 1; k < nvert - 1; k++) {
            tess.pointIndices[indIdx++] = firstVert + 0;
            tess.pointIndices[indIdx++] = firstVert + k;
            tess.pointIndices[indIdx++] = firstVert + k + 1;
          }
          // Final triangle between the last and first point:
          tess.pointIndices[indIdx++] = firstVert + 0;
          tess.pointIndices[indIdx++] = firstVert + 1;
          tess.pointIndices[indIdx++] = firstVert + nvert - 1;      
          
          firstVert = vertIdx;
        } 
      }
    }
    
    protected void tessellateSquarePoints() {
      int nInVert = in.lastVertex - in.firstVertex + 1;
      
      if (stroke && 1 <= nInVert) {
        tess.isStroked = true;
        
        // Each point generates a separate quad.
        int quadCount = nInVert;
        
        // Each quad is formed by 5 vertices, the center one
        // is the input vertex, and the other 4 define the 
        // corners (so, a triangle fan again).
        int nvertTot = 5 * quadCount;
        // So the quad is formed by 4 triangles, each requires
        // 3 indices.
        int nindTot = 12 * quadCount;
        
        tess.addPointVertices(nvertTot);
        tess.addPointIndices(nindTot);
        int vertIdx = tess.firstPointVertex;
        int attribIdx = tess.firstPointVertex;
        int indIdx = tess.firstPointIndex;      
        int firstVert = tess.firstPointVertex;      
        for (int i = in.firstVertex; i <= in.lastVertex; i++) {
          int nvert = 5;
          
          for (int k = 0; k < nvert; k++) {
            tess.putPointVertex(in, i, vertIdx);
            vertIdx++; 
          }       
          
          // The attributes for each tessellated vertex are the displacement along
          // the quad corners. The point shader will read these attributes and
          // displace the vertices in screen coordinates so the quads are always
          // camera facing (bilboards)
          tess.pointAttributes[2 * attribIdx + 0] = 0;
          tess.pointAttributes[2 * attribIdx + 1] = 0;
          attribIdx++;            
          for (int k = 0; k < 4; k++) {
            tess.pointAttributes[2 * attribIdx + 0] = 0.5f * QUAD_SIGNS[k][0] * strokeWeight;
            tess.pointAttributes[2 * attribIdx + 1] = 0.5f * QUAD_SIGNS[k][1] * strokeWeight;               
            attribIdx++;           
          }
          
          // Adding firstVert to take into account the triangles of all
          // the preceding points.
          for (int k = 1; k < nvert - 1; k++) {
            tess.pointIndices[indIdx++] = firstVert + 0;
            tess.pointIndices[indIdx++] = firstVert + k;
            tess.pointIndices[indIdx++] = firstVert + k + 1;
          }
          // Final triangle between the last and first point:
          tess.pointIndices[indIdx++] = firstVert + 0;
          tess.pointIndices[indIdx++] = firstVert + 1;
          tess.pointIndices[indIdx++] = firstVert + nvert - 1;  
          
          firstVert = vertIdx;
        }
      }
    }
    
    public void tessellateLines() {
      int nInVert = in.lastVertex - in.firstVertex + 1;
      
      if (stroke && 2 <= nInVert) {
        tess.isStroked = true;
        
        int lineCount = nInVert / 2;
        int first = in.firstVertex;

        // Lines are made up of 4 vertices defining the quad. 
        // Each vertex has its own offset representing the stroke weight.
        int nvert = lineCount * 4;
        // Each stroke line has 4 vertices, defining 2 triangles, which
        // require 3 indices to specify their connectivities.
        int nind = lineCount * 2 * 3;

        tess.addLineVertices(nvert);
        tess.addLineIndices(nind);      
        int vcount = tess.firstLineVertex;
        int icount = tess.firstLineIndex;
        for (int ln = 0; ln < lineCount; ln++) {
          int i0 = first + 2 * ln + 0;
          int i1 = first + 2 * ln + 1;
          addLine(i0, i1, vcount, icount); vcount += 4; icount += 6;
        }
      }  
    }

    public void tessellateTriangles() {
      int nInVert = in.lastVertex - in.firstVertex + 1;
      
      if (fill && 3 <= nInVert) {
        tess.addFillVertices(in);

        tess.addFillIndices(nInVert);
        int idx0 = tess.firstFillIndex;
        int offset = tess.firstFillVertex;
        for (int i = in.firstVertex; i <= in.lastVertex; i++) {
          tess.fillIndices[idx0 + i] = offset + i;
        }        
      }

      if (stroke) {
        tess.isStroked = true;
        tessellateEdges();        
      }      
    }
    
    public void tessellateTriangleFan() {
      int nInVert = in.lastVertex - in.firstVertex + 1;
      
      if (fill && 3 <= nInVert) {
        tess.addFillVertices(in);

        tess.addFillIndices(3 * (nInVert - 2));
        int idx = tess.firstFillIndex;
        int offset = tess.firstFillVertex; 
        for (int i = in.firstVertex + 1; i < in.lastVertex; i++) {
          tess.fillIndices[idx++] = offset + in.firstVertex;
          tess.fillIndices[idx++] = offset + i;
          tess.fillIndices[idx++] = offset + i + 1;
        }
      }
      
      if (stroke) {
        tess.isStroked = true;
        tessellateEdges();
      }
    }
    
    
    public void tessellateTriangleStrip() {
      int nInVert = in.lastVertex - in.firstVertex + 1;
      
      if (fill && 3 <= nInVert) {
        tess.addFillVertices(in);
                
        int triCount = nInVert - 2;
        
        // Each vertex, except the first and last, defines a triangle.
        tess.addFillIndices(3 * triCount);
        int idx = tess.firstFillIndex;
        int offset = tess.firstFillVertex;
        for (int i = in.firstVertex + 1; i < in.lastVertex; i++) {
          tess.fillIndices[idx++] = offset + i;
          if (i % 2 == 0) {
            tess.fillIndices[idx++] = offset + i - 1;  
            tess.fillIndices[idx++] = offset + i + 1;
          } else {
            tess.fillIndices[idx++] = offset + i + 1;  
            tess.fillIndices[idx++] = offset + i - 1;
          }
        }              
      }      
      
      if (stroke) {
        tess.isStroked = true;
        tessellateEdges();
      }
    }

    public void tessellateQuads() {
      int nInVert = in.lastVertex - in.firstVertex + 1;
      
      if (fill && 4 <= nInVert) {
        tess.addFillVertices(in);
      
        int quadCount = nInVert / 4;
        
        tess.addFillIndices(6 * quadCount);
        int idx = tess.firstFillIndex;
        int offset = tess.firstFillVertex; 
        for (int qd = 0; qd < quadCount; qd++) {        
          int i0 = offset + 4 * qd + 0;
          int i1 = offset + 4 * qd + 1;
          int i2 = offset + 4 * qd + 2;
          int i3 = offset + 4 * qd + 3;
          
          tess.fillIndices[idx++] = i0;
          tess.fillIndices[idx++] = i1;
          tess.fillIndices[idx++] = i3;
          
          tess.fillIndices[idx++] = i1;
          tess.fillIndices[idx++] = i2;
          tess.fillIndices[idx++] = i3;
        }              
      }
      
      if (stroke) {
        tess.isStroked = true;
        tessellateEdges();
      }
    }
    
    
    public void tessellateQuadStrip() {
      int nInVert = in.lastVertex - in.firstVertex + 1;
      
      if (fill && 4 <= nInVert) {
        tess.addFillVertices(in);
        
        int quadCount = nInVert / 2 - 1;
        
        tess.addFillIndices(6 * quadCount);
        int idx = tess.firstFillIndex;
        int offset = tess.firstFillVertex; 
        for (int qd = 1; qd < nInVert / 2; qd++) {        
          int i0 = offset + 2 * (qd - 1);
          int i1 = offset + 2 * (qd - 1) + 1;
          int i2 = offset + 2 * qd + 1;
          int i3 = offset + 2 * qd;      
          
          tess.fillIndices[idx++] = i0;
          tess.fillIndices[idx++] = i1;
          tess.fillIndices[idx++] = i3;
          
          tess.fillIndices[idx++] = i1;
          tess.fillIndices[idx++] = i2;
          tess.fillIndices[idx++] = i3;
        }              
      }
 
      if (stroke) {
        tess.isStroked = true;
        tessellateEdges();
      }
    }  
    
    public void tessellatePolygon(boolean solid, boolean closed, boolean calcNormals) {
      int nInVert = in.lastVertex - in.firstVertex + 1;
      
      tessCallback.calcNormals = calcNormals;
      
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
        for (int i = in.firstVertex; i <= in.lastVertex; i++) {
          boolean breakPt = in.codes[i] == PShape.BREAK;      
          if (breakPt) {
            GLU.gluTessEndContour(gluTess);  
            GLU.gluTessBeginContour(gluTess);
          }
          
          // Vertex data includes coordinates, colors, normals and texture coordinates.
          double[] vertex = new double[] { in.vertices[3 * i + 0], in.vertices[3 * i + 1], in.vertices[3 * i + 2],
                                           in.colors[4 * i + 0], in.colors[4 * i + 1], in.colors[4 * i + 2], in.colors[4 * i + 3],
                                           in.normals[3 * i + 0], in.normals[3 * i + 1], in.normals[3 * i + 2],
                                           in.texcoords[2 * i + 0], in.texcoords[2 * i + 1] };
          GLU.gluTessVertex(gluTess, vertex, 0, vertex);
        }
        
        GLU.gluTessEndContour(gluTess);
        
        GLU.gluTessEndPolygon(gluTess);
      }

      if (stroke) {
        tess.isStroked = true;
        tessellateEdges();
      }  
    }

    
    // Adding the data that defines a quad starting at vertex i0 and
    // ending at i1.
    protected void addLine(int i0, int i1, int vcount, int icount) {
      tess.putLineVertex(in, i0, i1, vcount);
   
      tess.lineAttributes[4 * vcount + 3] = +strokeWeight;
      tess.lineIndices[icount++] = vcount;
      
      vcount++;
      tess.putLineVertex(in, i0, i1, vcount);
      tess.lineAttributes[4 * vcount + 3] = -strokeWeight;
      tess.lineIndices[icount++] = vcount;
      
      vcount++;
      tess.putLineVertex(in, i1, i0, vcount);
      tess.lineAttributes[4 * vcount + 3] = -strokeWeight;
      tess.lineIndices[icount++] = vcount;
      
      // Starting a new triangle re-using prev vertices.
      tess.lineIndices[icount++] = vcount;
      tess.lineIndices[icount++] = vcount - 1;
      
      vcount++;
      tess.putLineVertex(in, i1, i0, vcount);      
      tess.lineAttributes[4 * vcount + 3] = +strokeWeight;
      tess.lineIndices[icount++] = vcount;
    }
    
    public void tessellateEdges() {
      tess.addLineVertices(in.getNumLineVertices());
      tess.addLineIndices(in.getNumLineIndices());
      int vcount = tess.firstLineVertex;
      int icount = tess.firstLineIndex;          
      for (int i = in.firstEdge; i <= in.lastEdge; i++) {
        int[] edge = in.edges[i];
        addLine(edge[0], edge[1], vcount, icount); vcount += 4; icount += 6;
      }    
    }
    
    public boolean startEdge(int edge) {
      return edge % 2 != 0;
    }
    
    public boolean endEdge(int edge) {
      return 1 < edge;
    }    
    
    public class GLUTessCallback extends GLUtessellatorCallbackAdapter {
      public boolean calcNormals;
      protected int tessFirst;
      protected int tessCount;
      protected int tessType;
      
      public void begin(int type) {
        tessFirst = tess.fillVertexCount;
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
            if (calcNormals) calcTriNormal(0, i, i + 1);
          }       
          break;
        case TRIANGLE_STRIP: 
          for (int i = 1; i < tessCount - 1; i++) {
            addIndex(i);
            if (i % 2 == 0) {
              addIndex(i - 1);
              addIndex(i + 1);
              if (calcNormals) calcTriNormal(i + 1, i, i - 1);
            } else {
              addIndex(i + 1);
              addIndex(i - 1);
              if (calcNormals) calcTriNormal(i - 1, i, i + 1);
            }            
          }        
          break;
        case TRIANGLES: 
          for (int i = 0; i < tessCount; i++) {
            addIndex(i);          
          }
          if (calcNormals) {
            for (int tr = 0; tr < tessCount / 3; tr++) {
              int i0 = 3 * tr + 0;
              int i1 = 3 * tr + 1;
              int i2 = 3 * tr + 2;
              calcTriNormal(i0, i1, i2);
            }
          }            
          break;
        }
      }
      
      protected void addIndex(int tessIdx) {
        tess.addFillIndex(tessFirst + tessIdx);
      }
      
      protected void calcTriNormal(int tessIdx0, int tessIdx1, int tessIdx2) {
        tess.calcFillNormal(tessFirst + tessIdx0, tessFirst + tessIdx1, tessFirst + tessIdx2);
      }
      
      public void vertex(Object data) {
        if (data instanceof double[]) {
          double[] d = (double[]) data;
          if (d.length < 12) {
            throw new RuntimeException("TessCallback vertex() data is not of length 12");
          }
          
          tess.addFillVertex((float) d[ 0], (float) d[ 1], (float) d[2],
                             (float) d[ 3], (float) d[ 4], (float) d[5], (float) d[6],
                             (float) d[ 7], (float) d[ 8], (float) d[9],
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
