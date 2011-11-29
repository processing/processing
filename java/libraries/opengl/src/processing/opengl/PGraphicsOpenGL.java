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

  /** Position of the camera. */
  public float cameraX, cameraY, cameraZ;
  /** Distance of the near and far planes. */
  public float cameraNear, cameraFar;
  /** Aspect ratio of camera's view. */
  public float cameraAspect;
  
  /** Distance between the camera eye and and aim point. */
  protected float cameraDepth; 
  
  /** Flag to indicate that we are inside beginCamera/endCamera block. */
  protected boolean manipulatingCamera;
  
  protected boolean scalingDuringCamManip;  
  
  // ........................................................

  // Projection, modelview matrices:
  
  // Array version for use with OpenGL
  protected float[] glmodelview;
  protected float[] glmodelviewInv;
  protected float[] glprojection;

  protected float[] pcamera;
  protected float[] pcameraInv;
  
  protected float[] pprojection;
  
  /** Model transformation (translate/rotate/scale) matrix
   * This relationship hold:
   *  
   * modelviewStack.current = pcamera * transform
   * 
   * or:
   * 
   * transform = pcameraInv * modelviewStack.current
   * 
   */  
  protected float[] transform;
  
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
  static protected GLMatrixStack modelviewStack;  
  
  /** Projection (ortho/perspective) matrix stack **/
  static protected GLMatrixStack projectionStack; 

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

  /** Vertex, color, texture coordinate and normal buffers. */
  public static final int DEFAULT_BUFFER_SIZE = 512;
  protected FloatBuffer vertexBuffer;
  protected FloatBuffer colorBuffer;
  protected FloatBuffer normalBuffer;
  protected FloatBuffer texcoordBuffer;
  protected IntBuffer indexBuffer;

  protected boolean geometryAllocated = false;      
  
  // ........................................................
  
  // Blending:
  
  protected int screenBlendMode;  
  
  // ........................................................

  // Text:
    
  /** Font texture of currently selected font. */
  PFontTexture textTex;
  
  /** Buffers and array to draw text quads. */
  protected FloatBuffer textVertexBuffer = null;
  protected FloatBuffer textTexCoordBuffer = null;
  protected float[] textVertexArray = null;
  protected float[] textTexCoordArray = null;  
  protected int textVertexCount = 0;    

  /** Used in the text block method. */
  protected boolean textBlockMode = false;
  protected int textBlockTex;
  
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
  protected int[] viewport = {0, 0, 0, 0};
  
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

  // The new stuff (shaders, tessellator, etc)    

  protected InGeometry in;
  protected TessGeometry tess;

  protected float[] currentVertex = { 0, 0, 0 };
  protected float[] currentColor = { 0, 0, 0, 0 };  
  protected float[] currentNormal = { 0, 0, 1 };
  protected float[] currentTexcoord = { 0, 0 };
  protected float[] currentStroke = { 0, 0, 0, 1, 1 };  

  protected boolean breakShape;  
  
  public static int flushMode = FLUSH_WHEN_FULL;
//  public static int flushMode = FLUSH_END_SHAPE;
//  public static int flushMode = FLUSH_AFTER_TRANSFORMATION;
  
  public static final int MAX_TESS_VERTICES = 1000000;
 
  public static final int DEFAULT_TESS_VERTICES = 512;
  public static final int DEFAULT_TESS_INDICES = 1024;
  
  public Tessellator tessellator;
  
  static protected PShader lineShader;
  static protected PShader pointShader;
  
  //////////////////////////////////////////////////////////////
  
  
  // INIT/ALLOCATE/FINISH
  
  public PGraphicsOpenGL() {
    glu = new GLU();
    tessellator = new Tessellator();
    in = newInGeometry();
    tess = newTessGeometry(IMMEDIATE);
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
      transform = new float[16];
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

    if (!geometryAllocated) {
      ByteBuffer vbb = ByteBuffer.allocateDirect(DEFAULT_BUFFER_SIZE * 3 * SIZEOF_FLOAT);
      vbb.order(ByteOrder.nativeOrder());
      vertexBuffer = vbb.asFloatBuffer();

      ByteBuffer cbb = ByteBuffer.allocateDirect(DEFAULT_BUFFER_SIZE * 4 * SIZEOF_FLOAT);
      cbb.order(ByteOrder.nativeOrder());
      colorBuffer = cbb.asFloatBuffer();

      ByteBuffer nbb = ByteBuffer.allocateDirect(DEFAULT_BUFFER_SIZE * 3 * SIZEOF_FLOAT);
      nbb.order(ByteOrder.nativeOrder());
      normalBuffer = nbb.asFloatBuffer();

      //texCoordBuffer = new FloatBuffer[MAX_TEXTURES];
      ByteBuffer tbb = ByteBuffer.allocateDirect(DEFAULT_BUFFER_SIZE * 2 * SIZEOF_FLOAT);
      tbb.order(ByteOrder.nativeOrder());
      texcoordBuffer = tbb.asFloatBuffer();
      
      ByteBuffer ibb = ByteBuffer.allocateDirect(DEFAULT_BUFFER_SIZE * SIZEOF_INT);
      ibb.order(ByteOrder.nativeOrder());
      indexBuffer = ibb.asIntBuffer();
      
      geometryAllocated = true;
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

  //////////////////////////////////////////////////////////////

  // RESOURCE HANDLING
  
  
  // Texture Objects -------------------------------------------
  
  protected int createTextureObject() {
    deleteFinalizedTextureObjects();
    
    int[] temp = new int[1];
    gl.glGenTextures(1, temp, 0);
    int idx = temp[0];
    
    if (glTextureObjects.containsKey(idx)) {
      System.err.println("Adding same texture twice");
    } else {    
      glTextureObjects.put(idx, false);
    }
    
    return idx;
  }
  
  // This is synchronized because it is called from the GC thread.
  synchronized protected void finalizeTextureObject(int idx) {
    if (glTextureObjects.containsKey(idx)) {
      glTextureObjects.put(idx, true);
    } else {
      System.err.println("Trying to finalize non-existing texture");
    }
  }
  
  protected void deleteFinalizedTextureObjects() {
    Set<Integer> finalized = new HashSet<Integer>();
    
    for (Integer idx : glTextureObjects.keySet()) {
      if (glTextureObjects.get(idx)) {
        finalized.add(idx);
        int id = idx.intValue();
        int[] temp = { id };
        gl.glDeleteTextures(1, temp, 0);        
      }
    }
    
    for (Integer idx : finalized) {
      glTextureObjects.remove(idx);  
    }
  }
  
  // Vertex Buffer Objects ----------------------------------------------
    
  protected int createVertexBufferObject() {
    deleteFinalizedVertexBufferObjects();
    
    int[] temp = new int[1];
    gl.glGenBuffers(1, temp, 0);
    int idx = temp[0];
    
    if (glVertexBuffers.containsKey(idx)) {
      System.err.println("Adding same VBO twice");
    } else {    
      glVertexBuffers.put(idx, false);
    }
    
    return idx;
  }
  
  // This is synchronized because it is called from the GC thread.
  synchronized protected void finalizeVertexBufferObject(int idx) {
    if (glVertexBuffers.containsKey(idx)) {
      glVertexBuffers.put(idx, true);
    } else {
      System.err.println("Trying to finalize non-existing VBO");
    }
  }
  
  protected void deleteFinalizedVertexBufferObjects() {
    Set<Integer> finalized = new HashSet<Integer>();
    
    for (Integer idx : glVertexBuffers.keySet()) {
      if (glVertexBuffers.get(idx)) {
        finalized.add(idx);
        int id = idx.intValue();
        int[] temp = { id };
        gl.glDeleteBuffers(1, temp, 0);        
      }
    }
    
    for (Integer idx : finalized) {
      glVertexBuffers.remove(idx);  
    }
  }
  
  // FrameBuffer Objects -----------------------------------------

  protected int createFrameBufferObject() {
    deleteFinalizedFrameBufferObjects();
    
    int[] temp = new int[1];
    gl.glGenFramebuffers(1, temp, 0);
    int idx = temp[0];
    
    if (glFrameBuffers.containsKey(idx)) {
      System.err.println("Adding same FBO twice");
    } else {    
      glFrameBuffers.put(idx, false);
    }
    
    return idx;
  }
  
  // This is synchronized because it is called from the GC thread.
  synchronized protected void finalizeFrameBufferObject(int idx) {
    if (glFrameBuffers.containsKey(idx)) {
      glFrameBuffers.put(idx, true);
    } else {
      System.err.println("Trying to finalize non-existing FBO");
    }
  }
  
  protected void deleteFinalizedFrameBufferObjects() {
    Set<Integer> finalized = new HashSet<Integer>();
    
    for (Integer idx : glFrameBuffers.keySet()) {
      if (glFrameBuffers.get(idx)) {
        finalized.add(idx);
        int id = idx.intValue();
        int[] temp = { id };
        gl.glDeleteFramebuffers(1, temp, 0);       
      }
    }
    
    for (Integer idx : finalized) {
      glFrameBuffers.remove(idx);  
    }
  }

  // RenderBuffer Objects -----------------------------------------------
  
  protected int createRenderBufferObject() {
    deleteFinalizedRenderBufferObjects();
    
    int[] temp = new int[1];
    gl.glGenRenderbuffers(1, temp, 0);
    int idx = temp[0];
    
    if (glRenderBuffers.containsKey(idx)) {
      System.err.println("Adding same renderbuffer twice");
    } else {    
      glRenderBuffers.put(idx, false);
    }
    
    return idx;
  }
  
  // This is synchronized because it is called from the GC thread.
  synchronized protected void finalizeRenderBufferObject(int idx) {
    if (glRenderBuffers.containsKey(idx)) {
      glRenderBuffers.put(idx, true);
    } else {
      System.err.println("Trying to finalize non-existing renderbuffer");
    }
  }
  
  protected void deleteFinalizedRenderBufferObjects() {
    Set<Integer> finalized = new HashSet<Integer>();
    
    for (Integer idx : glRenderBuffers.keySet()) {
      if (glRenderBuffers.get(idx)) {
        finalized.add(idx);
        int id = idx.intValue();
        int[] temp = { id };
        gl.glDeleteRenderbuffers(1, temp, 0);       
      }
    }
    
    for (Integer idx : finalized) {
      glRenderBuffers.remove(idx);  
    }
  }
  
  // GLSL Program Objects -----------------------------------------------
  
  protected int createGLSLProgramObject() {
    deleteFinalizedGLSLProgramObjects();
    
    int idx = gl2x.glCreateProgram();
    
    if (glslPrograms.containsKey(idx)) {
      System.err.println("Adding same glsl program twice");
    } else {    
      glslPrograms.put(idx, false);
    }
    
    return idx;
  }
  
  // This is synchronized because it is called from the GC thread.
  synchronized protected void finalizeGLSLProgramObject(int idx) {
    if (glslPrograms.containsKey(idx)) {
      glslPrograms.put(idx, true);
    } else {
      System.err.println("Trying to finalize non-existing glsl program");
    }
  }
  
  protected void deleteFinalizedGLSLProgramObjects() {
    Set<Integer> finalized = new HashSet<Integer>();
    
    for (Integer idx : glslPrograms.keySet()) {
      if (glslPrograms.get(idx)) {
        finalized.add(idx);
        int id = idx.intValue();
        gl2x.glDeleteProgram(id);        
      }
    }
    
    for (Integer idx : finalized) {
      glslPrograms.remove(idx);  
    }
  }

  // GLSL Vertex Shader Objects -----------------------------------------------
  
  protected int createGLSLVertShaderObject() {
    deleteFinalizedGLSLVertShaderObjects();
    
    int idx = gl2x.glCreateShader(GL2.GL_VERTEX_SHADER);
    
    if (glslVertexShaders.containsKey(idx)) {
      System.err.println("Adding same glsl vertex shader twice");
    } else {    
      glslVertexShaders.put(idx, false);
    }
    
    return idx;
  }
  
  // This is synchronized because it is called from the GC thread.
  synchronized protected void finalizeGLSLVertShaderObject(int idx) {
    if (glslVertexShaders.containsKey(idx)) {
      glslVertexShaders.put(idx, true);
    } else {
      System.err.println("Trying to finalize non-existing glsl vertex shader");
    }
  }
  
  protected void deleteFinalizedGLSLVertShaderObjects() {
    Set<Integer> finalized = new HashSet<Integer>();
    
    for (Integer idx : glslVertexShaders.keySet()) {
      if (glslVertexShaders.get(idx)) {
        finalized.add(idx);
        int id = idx.intValue();
        gl2x.glDeleteShader(id);      
      }
    }
    
    for (Integer idx : finalized) {
      glslVertexShaders.remove(idx);  
    }
  }
  
  // GLSL Fragment Shader Objects -----------------------------------------------
    
  
  protected int createGLSLFragShaderObject() {
    deleteFinalizedGLSLFragShaderObjects();
    
    int idx = gl2x.glCreateShader(GL2.GL_FRAGMENT_SHADER);
    
    if (glslFragmentShaders.containsKey(idx)) {
      System.err.println("Adding same glsl fragment shader twice");
    } else {    
      glslFragmentShaders.put(idx, false);
    }
    
    return idx;
  }
  
  // This is synchronized because it is called from the GC thread.
  synchronized protected void finalizeGLSLFragShaderObject(int idx) {
    if (glslFragmentShaders.containsKey(idx)) {
      glslFragmentShaders.put(idx, true);
    } else {
      System.err.println("Trying to finalize non-existing glsl fragment shader");
    }
  }
  
  protected void deleteFinalizedGLSLFragShaderObjects() {
    Set<Integer> finalized = new HashSet<Integer>();
    
    for (Integer idx : glslFragmentShaders.keySet()) {
      if (glslFragmentShaders.get(idx)) {
        finalized.add(idx);
        int id = idx.intValue();
        gl2x.glDeleteShader(id);      
      }
    }
    
    for (Integer idx : finalized) {
      glslFragmentShaders.remove(idx);  
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
    //backupPGLObjects();
      
    releaseResources();
    releaseContext();
    context.destroy();
    context = null;
    allocate();          
    detainContext();
      
    updateGLInterfaces();    
//    allocatePGLObjects();
//    clearPGLFramebuffers();
//    restorePGLObjects();
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
      System.err.println("P3D: Already called beginDraw().");
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
    
    if (lineShader == null) {
      lineShader = new PShader(parent);      
      lineShader.loadVertexShader(PGraphicsOpenGL.class.getResource("LineShaderVert.glsl"));
      lineShader.loadFragmentShader(PGraphicsOpenGL.class.getResource("LineShaderFrag.glsl"));
      lineShader.setup();
    }

    if (pointShader == null) {
      pointShader = new PShader(parent);
      pointShader.loadVertexShader(PGraphicsOpenGL.class.getResource("PointShaderVert.glsl"));
      pointShader.loadFragmentShader(PGraphicsOpenGL.class.getResource("PointShaderFrag.glsl"));
      pointShader.setup();
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
    noTexture();
        
    // Screen blend is needed for alpha (i.e. fonts) to work.
    blendMode(BLEND);
    
    // Default texture blending:
    textureBlend(BLEND);
    
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

    // setup opengl viewport.    
    gl.glGetIntegerv(GL.GL_VIEWPORT, viewport, 0);
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
    
    //shapeFirst = 0;
    
    // The current normal vector is set to zero.
    normalX = normalY = normalZ = 0;
    
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
    
    report("bot beginDraw()");
  }


  public void endDraw() {
    report("top endDraw()");
    
    if (flushMode == FLUSH_WHEN_FULL || flushMode == FLUSH_AFTER_TRANSFORMATION) {
      flush();
      // TODO: Implement depth sorting (http://code.google.com/p/processing/issues/detail?id=51)      
      //if (hints[ENABLE_DEPTH_SORT]) {
      //  flush();
      //}          
    }
    
    if (!drawing) {
      System.err.println("P3D: Cannot call endDraw() before beginDraw().");
      return;
    }
    
    // Restoring previous viewport.
    gl.glViewport(viewport[0], viewport[1], viewport[2], viewport[3]); 
     
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
    
    //deleteFinalizedGLResources();    
    
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
  
  
  /*
  public void allocateGL() {
    allocatePGLObjects();    
  }
  
  
  public void backupGL() {
    backupPGLObjects();
  }  
  
  
  public void restoreGL() {
    clearPGLFramebuffers();
    restorePGLObjects();
  }
  */
  
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
    blendMode(screenBlendMode);
    
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
    scalingDuringCamManip = false;
        
    if (fbStack == null) {
      fbStack = new Stack<PFramebuffer>();

      screenFramebuffer = new PFramebuffer(parent, width, height, true);
      setFramebuffer(screenFramebuffer);
    }    
    
    if (modelviewStack == null) {
      modelviewStack = new GLMatrixStack();
    }
    if (projectionStack == null) {
      projectionStack = new GLMatrixStack();
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
      gl.glDisable(GL.GL_DEPTH_TEST);
      gl.glClearColor(0, 0, 0, 0);
      gl.glClear(GL.GL_DEPTH_BUFFER_BIT);

    } else if (which == ENABLE_DEPTH_TEST) {
      gl.glEnable(GL.GL_DEPTH_TEST);

    } else if (which == DISABLE_DEPTH_MASK) {
      gl.glDepthMask(false);

    } else if (which == ENABLE_DEPTH_MASK) {
      gl.glDepthMask(true);            
    }

  }

  //////////////////////////////////////////////////////////////

  // SHAPE CREATORS
  
  
  public PShape createGroup() {
    return new PShape3D(parent, PShape.GROUP);    
  }
  
  public PShape createGeometry() {
    PShape3D shape = new PShape3D(parent, PShape.GEOMETRY);
    shape.setKind(POLYGON);
    return shape;
  }  
  
  public PShape createGeometry(int kind) {
    PShape3D shape = new PShape3D(parent, PShape.GEOMETRY);
    shape.setKind(kind);
    return shape;
  }

  public PShape createPrimitive(int kind) {
    PShape3D shape = new PShape3D(parent, PShape.PRIMITIVE);
    shape.setKind(kind);
    return shape;
  }  
  
  //////////////////////////////////////////////////////////////

  // VERTEX SHAPES

  
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
    currentVertex[0] = x;
    currentVertex[1] = y;
    currentVertex[2] = z;      
    
    boolean textured = textureImage != null;
    if (fill || textured) {
      if (!textured) {
        currentColor[0] = fillR;
        currentColor[1] = fillG;
        currentColor[2] = fillB;
        currentColor[3] = fillA;
      } else {
        if (tint) {
          currentColor[0] = tintR;
          currentColor[1] = tintG;
          currentColor[2] = tintB;
          currentColor[3] = tintA;
        } else {
          currentColor[0] = 1;
          currentColor[1] = 1;
          currentColor[2] = 1;
          currentColor[3] = 1;
        }
      }
    }
    
    currentNormal[0] = normalX;
    currentNormal[1] = normalY;
    currentNormal[2] = normalZ;    
    
    currentTexcoord[0] = u;
    currentTexcoord[1] = v;    

    if (stroke) {
      currentStroke[0] = strokeR;
      currentStroke[1] = strokeG;
      currentStroke[2] = strokeB;
      currentStroke[3] = strokeA;
      currentStroke[4] = strokeWeight;
    } else {
      currentStroke[0] = 0;
      currentStroke[1] = 0;
      currentStroke[2] = 0;
      currentStroke[3] = 0;
      currentStroke[4] = 0;      
    }
    
    int code;
    if (breakShape) {
      code = BREAK;
      breakShape = false;
    } else {
      code = VERTEX;
    }    
        
    in.addVertex(currentVertex, currentColor, currentNormal, currentTexcoord, currentStroke, code);
  }
  
  
  public void beginShape(int kind) {  
    shape = kind;
 
    in.reset();
  }
    
  
  public void endShape(int mode) {
    tessellator.setInGeometry(in);
    tessellator.setTessGeometry(tess);

    if (flushMode == FLUSH_WHEN_FULL) {
      getTransformMatrix();      
    }
    
    if (shape == POINTS) {
      tessellator.tessellatePoints(strokeCap);    
    } else if (shape == LINES) {
      tessellator.tessellateLines();    
    } else if (shape == TRIANGLES) {
      tessellator.tessellateTriangles();
    } else if (shape == TRIANGLE_FAN) {
      tessellator.tessellateTriangleFan();
    } else if (shape == TRIANGLE_STRIP) {
      tessellator.tessellateTriangleStrip();
    } else if (shape == QUADS) {
      tessellator.tessellateQuads();
    } else if (shape == QUAD_STRIP) {
      tessellator.tessellateQuadStrip();
    } else if (shape == POLYGON) {
      tessellator.tessellatePolygon(false, mode == CLOSE);
    }

    if (flushMode == FLUSH_END_SHAPE || (flushMode == FLUSH_WHEN_FULL && tess.isFull())) {
      flush();
    }    
  }


  public void breakShape() {
    breakShape = true;
  }  
  
  
  //////////////////////////////////////////////////////////////

  // RENDERING

  // protected void render()

  // protected void sort()  
  
  
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
      
      if (hasPoints) {
        renderPoints();
      } 

      if (hasLines) {
        renderLines();    
      }    
            
      if (hasFill) { 
        renderFill(textureImage);
      }
      
      if (flushMode == FLUSH_WHEN_FULL) {
        gl2f.glPopMatrix();
      }
    }
    
    tess.reset();
  }
  

  protected void renderPoints() {
    checkVertexBuffers(tess.pointVertexCount);
    
    pointShader.start();
    
    vertexBuffer.rewind();
    vertexBuffer.put(tess.pointVertices, 0, 3 * tess.pointVertexCount);    
    vertexBuffer.position(0);
    
    colorBuffer.rewind();
    colorBuffer.put(tess.pointColors, 0, 4 * tess.pointVertexCount);
    colorBuffer.position(0);
    
    normalBuffer.rewind();
    normalBuffer.put(tess.pointNormals, 0, 3 * tess.pointVertexCount);
    normalBuffer.position(0);
    
    gl2f.glEnableClientState(GL2.GL_VERTEX_ARRAY);    
    gl2f.glEnableClientState(GL2.GL_COLOR_ARRAY);    
    gl2f.glEnableClientState(GL2.GL_NORMAL_ARRAY);  
        
    gl2f.glVertexPointer(3, GL.GL_FLOAT, 0, vertexBuffer);
    gl2f.glColorPointer(4, GL.GL_FLOAT, 0, colorBuffer);
    gl2f.glNormalPointer(GL.GL_FLOAT, 0, normalBuffer);    
    
    int attribsID = pointShader.getAttribLocation("vertDisp");     
    gl2x.glEnableVertexAttribArray(attribsID);
    gl2x.glVertexAttribPointer(attribsID, 2, GL.GL_FLOAT, false, 0, FloatBuffer.wrap(tess.pointAttributes));
    
    gl2f.glDrawElements(GL.GL_TRIANGLES, tess.pointIndexCount, GL.GL_UNSIGNED_INT, IntBuffer.wrap(tess.pointIndices));
    
    gl2x.glDisableVertexAttribArray(attribsID);
    
    gl2f.glDisableClientState(GL2.GL_VERTEX_ARRAY);    
    gl2f.glDisableClientState(GL2.GL_COLOR_ARRAY);
    gl2f.glDisableClientState(GL2.GL_NORMAL_ARRAY);
    
    pointShader.stop();      
  }  
  

  protected void renderLines() {
    checkVertexBuffers(tess.lineVertexCount);
    
    lineShader.start();
    
    vertexBuffer.rewind();
    vertexBuffer.put(tess.lineVertices, 0, 3 * tess.lineVertexCount);    
    vertexBuffer.position(0);
    
    colorBuffer.rewind();
    colorBuffer.put(tess.lineColors, 0, 4 * tess.lineVertexCount);
    colorBuffer.position(0);
    
    normalBuffer.rewind();
    normalBuffer.put(tess.lineNormals, 0, 3 * tess.lineVertexCount);
    normalBuffer.position(0);

    gl2f.glEnableClientState(GL2.GL_VERTEX_ARRAY);    
    gl2f.glEnableClientState(GL2.GL_COLOR_ARRAY);    
    gl2f.glEnableClientState(GL2.GL_NORMAL_ARRAY);  
    
    gl2f.glVertexPointer(3, GL.GL_FLOAT, 0, vertexBuffer);
    gl2f.glColorPointer(4, GL.GL_FLOAT, 0, colorBuffer);
    gl2f.glNormalPointer(GL.GL_FLOAT, 0, normalBuffer);    
    
    int[] viewport = {0, 0, 0, 0};
    gl2f.glGetIntegerv(GL.GL_VIEWPORT, viewport, 0);
    lineShader.setVecUniform("viewport", viewport[0], viewport[1], viewport[2], viewport[3]);
            
    int attribsID = lineShader.getAttribLocation("attribs");     
    gl2x.glEnableVertexAttribArray(attribsID);
    gl2x.glVertexAttribPointer(attribsID, 4, GL.GL_FLOAT, false, 0, FloatBuffer.wrap(tess.lineAttributes));
    
    gl2f.glDrawElements(GL.GL_TRIANGLES, tess.lineIndexCount, GL.GL_UNSIGNED_INT, IntBuffer.wrap(tess.lineIndices));
    
    gl2x.glDisableVertexAttribArray(attribsID);
    
    gl2f.glDisableClientState(GL2.GL_VERTEX_ARRAY);    
    gl2f.glDisableClientState(GL2.GL_COLOR_ARRAY);
    gl2f.glDisableClientState(GL2.GL_NORMAL_ARRAY);  
    
    lineShader.stop();
  }

  
  protected void renderFill(PImage textureImage) {
    checkVertexBuffers(tess.fillVertexCount);
    
    vertexBuffer.rewind();
    vertexBuffer.put(tess.fillVertices, 0, 3 * tess.fillVertexCount);    
    vertexBuffer.position(0);
    
    colorBuffer.rewind();
    colorBuffer.put(tess.fillColors, 0, 4 * tess.fillVertexCount);
    colorBuffer.position(0);
    
    normalBuffer.rewind();
    normalBuffer.put(tess.fillNormals, 0, 3 * tess.fillVertexCount);
    normalBuffer.position(0);

    gl2f.glEnableClientState(GL2.GL_VERTEX_ARRAY);    
    gl2f.glEnableClientState(GL2.GL_COLOR_ARRAY);    
    gl2f.glEnableClientState(GL2.GL_NORMAL_ARRAY);    
    
    PTexture tex = null;
    if (textureImage != null) {
      tex = ogl.getTexture(textureImage);
      if (tex != null) {
        gl2f.glEnable(tex.glTarget);
        gl2f.glActiveTexture(GL.GL_TEXTURE0);
        gl2f.glBindTexture(tex.glTarget, tex.glID);
      }
      texcoordBuffer.rewind();
      texcoordBuffer.put(tess.fillTexcoords, 0, 2 * tess.fillVertexCount);
      texcoordBuffer.position(0);
    }    
    
    gl2f.glVertexPointer(3, GL.GL_FLOAT, 0, vertexBuffer);
    gl2f.glColorPointer(4, GL.GL_FLOAT, 0, colorBuffer);
    gl2f.glNormalPointer(GL.GL_FLOAT, 0, normalBuffer);
    if (tex != null) {    
      gl2f.glTexCoordPointer(2, GL.GL_FLOAT, 0, texcoordBuffer);
    }
    
    // What is faster?
    
    // 1) wrapping the float array:
    gl2f.glDrawElements(GL.GL_TRIANGLES, tess.fillIndexCount, GL.GL_UNSIGNED_INT, IntBuffer.wrap(tess.fillIndices));

    // or:
    //2) copying the float array to a pre-existing direct buffer:
    //checkIndexBuffers(tess.fillIndexCount);
    //indexBuffer.rewind();
    //indexBuffer.put(tess.fillIndices);
    //indexBuffer.position(0);
    //gl2f.glDrawElements(GL.GL_TRIANGLES, tess.fillIndexCount, GL.GL_UNSIGNED_INT, indexBuffer);
    
    if (tex != null) {
      gl2f.glActiveTexture(GL.GL_TEXTURE0);
      gl2f.glBindTexture(tex.glTarget, 0);
      gl2f.glDisable(tex.glTarget);
      
      gl2f.glDisableClientState(GL2.GL_TEXTURE_COORD_ARRAY);
    }     
    
    gl2f.glDisableClientState(GL2.GL_VERTEX_ARRAY);    
    gl2f.glDisableClientState(GL2.GL_COLOR_ARRAY);
    gl2f.glDisableClientState(GL2.GL_NORMAL_ARRAY);  
  }
  
  
  protected void checkVertexBuffers(int n) {
    if (vertexBuffer.capacity() / 3 < n) {    
      ByteBuffer vbb = ByteBuffer.allocateDirect(n * 3 * SIZEOF_FLOAT);
      vbb.order(ByteOrder.nativeOrder());
      vertexBuffer = vbb.asFloatBuffer();

      ByteBuffer cbb = ByteBuffer.allocateDirect(n * 4 * SIZEOF_FLOAT);
      cbb.order(ByteOrder.nativeOrder());
      colorBuffer = cbb.asFloatBuffer();

      ByteBuffer nbb = ByteBuffer.allocateDirect(n * 3 * SIZEOF_FLOAT);
      nbb.order(ByteOrder.nativeOrder());
      normalBuffer = nbb.asFloatBuffer();
     
      ByteBuffer tbb = ByteBuffer.allocateDirect(n * 2 * SIZEOF_FLOAT);
      tbb.order(ByteOrder.nativeOrder());
      texcoordBuffer = tbb.asFloatBuffer();     
    }
  }  
  
  
  protected void checkIndexBuffers(int n) {
    if (indexBuffer.capacity() < n) {
      ByteBuffer ibb = ByteBuffer.allocateDirect(n * SIZEOF_INT);
      ibb.order(ByteOrder.nativeOrder());
      indexBuffer = ibb.asIntBuffer();
    }    
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

  // TODO it seem there are no evaluators in OpenGL ES

  // protected void bezierVertexCheck();
  // public void bezierVertex(float x2, float y2,
  // float x3, float y3,
  // float x4, float y4)
  // public void bezierVertex(float x2, float y2, float z2,
  // float x3, float y3, float z3,
  // float x4, float y4, float z4)

  //////////////////////////////////////////////////////////////

  // CATMULL-ROM CURVE VERTICES

  // TODO it seem there are no evaluators in OpenGL ES

  // protected void curveVertexCheck();
  // public void curveVertex(float x, float y)
  // public void curveVertex(float x, float y, float z)
  // protected void curveVertexSegment(float x1, float y1,
  // float x2, float y2,
  // float x3, float y3,
  // float x4, float y4)
  // protected void curveVertexSegment(float x1, float y1, float z1,
  // float x2, float y2, float z2,
  // float x3, float y3, float z3,
  // float x4, float y4, float z4)
  

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

  protected void ellipseImpl(float x, float y, float w, float h) {
    float radiusH = w / 2;
    float radiusV = h / 2;

    float centerX = x + radiusH;
    float centerY = y + radiusV;

    float sx1 = screenX(x, y);
    float sy1 = screenY(x, y);
    float sx2 = screenX(x + w, y + h);
    float sy2 = screenY(x + w, y + h);

    if (fill) {
      int accuracy = (int) (TWO_PI * PApplet.dist(sx1, sy1, sx2, sy2) / 20);
      if (accuracy < 6)
        accuracy = 6;

      float inc = (float) SINCOS_LENGTH / accuracy;
      float val = 0;

      boolean strokeSaved = stroke;
      stroke = false;
      boolean smoothSaved = smooth;
      if (smooth && stroke) {
        smooth = false;
      }

      beginShape(TRIANGLE_FAN);
      normal(0, 0, 1);
      vertex(centerX, centerY);
      for (int i = 0; i < accuracy; i++) {
        vertex(centerX + cosLUT[(int) val] * radiusH, centerY
            + sinLUT[(int) val] * radiusV);
        val = (val + inc) % SINCOS_LENGTH;
      }
      // back to the beginning
      vertex(centerX + cosLUT[0] * radiusH, centerY + sinLUT[0] * radiusV);
      endShape();

      stroke = strokeSaved;
      smooth = smoothSaved;
    }

    if (stroke) {
      int accuracy = (int) (TWO_PI * PApplet.dist(sx1, sy1, sx2, sy2) / 8);
      if (accuracy < 6)
        accuracy = 6;

      float inc = (float) SINCOS_LENGTH / accuracy;
      float val = 0;

      boolean savedFill = fill;
      fill = false;

      val = 0;
      beginShape();
      for (int i = 0; i < accuracy; i++) {
        vertex(centerX + cosLUT[(int) val] * radiusH, centerY
            + sinLUT[(int) val] * radiusV);
        val = (val + inc) % SINCOS_LENGTH;
      }
      endShape(CLOSE);

      fill = savedFill;
    }
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
  
  
  public void smooth(int antialias) {
    smooth = true;
    
    if (this.antialias  != antialias) {
      this.antialias = antialias;
      if (primarySurface) {
        restartContext();          
//        throw new PApplet.RendererChangeException();
      } else {
        initOffscreen();
      }
    }
    
    if (antialias < 2) {
      gl2f.glEnable(GL2.GL_MULTISAMPLE);
      gl2f.glEnable(GL2.GL_POINT_SMOOTH);
      gl2f.glEnable(GL2.GL_LINE_SMOOTH);
      gl2f.glEnable(GL2.GL_POLYGON_SMOOTH);      
    }
    
    int[] temp = { 0 };
    gl.glGetIntegerv(GL.GL_SAMPLES, temp, 0);
    antialias = temp[0];
    PApplet.println("Effective multisampling level: " + antialias);    
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

  // protected boolean textModeCheck(int mode)

  // public void textSize(float size)

  // public float textWidth(char c)

  // public float textWidth(String str)

  // protected float textWidthImpl(char buffer[], int start, int stop)

  //////////////////////////////////////////////////////////////

  // TEXT BLOCK

  public void beginText() {
    if (textMode == MODEL) {
      textBlockMode = true;  
      textVertexCount = 0;
    }
  }

  
  public void endText() {
    if (textBlockMode) {
      textBlockMode = false;
      
      if (0 < textVertexCount) {
        // Now we render all the text that has been pushed between 
        // beginText/endText.
        
        if (screenBlendMode != BLEND) {
          gl.glEnable(GL.GL_BLEND);
          if (blendEqSupported) gl.glBlendEquation(GL.GL_FUNC_ADD);
          gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
        }    
        
        textTex.setTexture(textBlockTex);
        renderTextModel();
        
        // Restoring current blend mode.
        blendMode(screenBlendMode);        
        
        gl.glBindTexture(GL.GL_TEXTURE_2D, 0);   
        gl.glDisable(GL.GL_TEXTURE_2D);      
      }
    }
  }  
  
  //////////////////////////////////////////////////////////////

  // TEXT IMPL

  // protected void textLineAlignImpl(char buffer[], int start, int stop,
  // float x, float y)
  
  /**
   * Implementation of actual drawing for a line of text.
   */
  protected void textLineImpl(char buffer[], int start, int stop, float x, float y) {
    // Init opengl state for text rendering...
    gl.glEnable(GL.GL_TEXTURE_2D);
    
    if (screenBlendMode != BLEND) {
      gl.glEnable(GL.GL_BLEND);
      if (blendEqSupported) gl.glBlendEquation(GL.GL_FUNC_ADD);
      gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
    }    
    
    textTex = (PFontTexture)textFont.getCache(ogl);     
    if (textTex == null) {
      textTex = new PFontTexture(parent, textFont, maxTextureSize, maxTextureSize);
      textFont.setCache(this, textTex);
    }    
    textTex.setFirstTexture();

    // Setting the current fill color as the font color.
    setFillColor();
    
    if (textMode == MODEL) {
      if (textVertexBuffer == null) {
        allocateTextModel();
      }
      
      // Setting Z axis as the normal to the text geometry      
      setDefNormals(0, 0, 1);
      
      if (!textBlockMode) {
        // Resetting vertex count when we are not defining a 
        // block of text.
        textVertexCount = 0;
      }
    }
    
    super.textLineImpl(buffer, start, stop, x, y);

    if (textMode == MODEL && 0 < textVertexCount) {
      if (!textBlockMode) {
        // Pushing text geometry to the GPU.
        renderTextModel();
      } else {
        // We don't push any geometry here because we will
        // do it when endText is called. For now we just 
        // save the current texture.
        textBlockTex = textTex.currentTex;
      }
    }
    
    // Restoring current blend mode.
    blendMode(screenBlendMode);
    
    gl.glBindTexture(GL.GL_TEXTURE_2D, 0);   
    gl.glDisable(GL.GL_TEXTURE_2D);    
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

  protected void textCharModelImpl(PFontTexture.TextureInfo info, float x1, float y1,
      float x2, float y2) {
    if ((textTex.currentTex != info.texIndex) || 
        (textBlockMode && textBlockTex != info.texIndex)) {
      if (0 < textVertexCount) {
        // Current texture changes (the font is so large that needs more than one texture).
        // So rendering all we got until now, and reseting vertex counter.
        renderTextModel();        
        textVertexCount = 0;
      }
      textTex.setTexture(info.texIndex);
    }   

    // Division by three needed because each int element in the buffer is used
    // to store three coordinates.
    if (textVertexBuffer.capacity() / 3 < textVertexCount + 6) {
      expandTextBuffers();
    }    
    
    int n = textVertexCount;
    textVertexArray[3 * n + 0] = x1;
    textVertexArray[3 * n + 1] = y1;
    textVertexArray[3 * n + 2] = 0;
    textTexCoordArray[2 * n + 0] = info.u0;
    textTexCoordArray[2 * n + 1] = info.v0;
    n++;    
    
    textVertexArray[3 * n + 0] = x2;
    textVertexArray[3 * n + 1] = y2;
    textVertexArray[3 * n + 2] = 0;        
    textTexCoordArray[2 * n + 0] = info.u1;
    textTexCoordArray[2 * n + 1] = info.v1;
    n++;
    
    textVertexArray[3 * n + 0] = x1;
    textVertexArray[3 * n + 1] = y2;
    textVertexArray[3 * n + 2] = 0;
    textTexCoordArray[2 * n + 0] = info.u0;
    textTexCoordArray[2 * n + 1] = info.v1;
    n++;

    textVertexArray[3 * n + 0] = x1;
    textVertexArray[3 * n + 1] = y1;
    textVertexArray[3 * n + 2] = 0;
    textTexCoordArray[2 * n + 0] = info.u0;
    textTexCoordArray[2 * n + 1] = info.v0;
    n++;    
    
    textVertexArray[3 * n + 0] = x2;
    textVertexArray[3 * n + 1] = y1;
    textVertexArray[3 * n + 2] = 0;
    textTexCoordArray[2 * n + 0] = info.u1;
    textTexCoordArray[2 * n + 1] = info.v0;
    n++;
    
    textVertexArray[3 * n + 0] = x2;
    textVertexArray[3 * n + 1] = y2;
    textVertexArray[3 * n + 2] = 0;
    textTexCoordArray[2 * n + 0] = info.u1;
    textTexCoordArray[2 * n + 1] = info.v1;
    n++;
    
    textVertexCount = n;
  }

  protected void textCharScreenImpl(PFontTexture.TextureInfo info, int xx, int yy,
      int w0, int h0) {
    if (textTex.currentTex != info.texIndex) {
      textTex.setTexture(info.texIndex);
    }
    
    drawTexture(info.width, info.height, info.crop, xx, height - (yy + h0), w0, h0);
  }

  protected void allocateTextModel() {  
    ByteBuffer vbb = ByteBuffer.allocateDirect(DEFAULT_BUFFER_SIZE * 3 * SIZEOF_FLOAT);
    vbb.order(ByteOrder.nativeOrder());
    textVertexBuffer = vbb.asFloatBuffer();

    ByteBuffer tbb = ByteBuffer.allocateDirect(DEFAULT_BUFFER_SIZE * 2 * SIZEOF_FLOAT);
    tbb.order(ByteOrder.nativeOrder());
    textTexCoordBuffer = tbb.asFloatBuffer();      
            
    textVertexArray = new float[DEFAULT_BUFFER_SIZE * 3];
    textTexCoordArray = new float[DEFAULT_BUFFER_SIZE * 2];
  }

  protected void renderTextModel() {  
    textVertexBuffer.position(0);    
    textTexCoordBuffer.position(0);
            
    textVertexBuffer.put(textVertexArray);
    textTexCoordBuffer.put(textTexCoordArray);
    
    gl2f.glEnableClientState(GL2.GL_VERTEX_ARRAY);
    gl2f.glEnableClientState(GL2.GL_TEXTURE_COORD_ARRAY);

    textVertexBuffer.position(0);
    textTexCoordBuffer.position(0);
    
    gl2f.glVertexPointer(3, GL.GL_FLOAT, 0, textVertexBuffer);
    gl2f.glTexCoordPointer(2, GL.GL_FLOAT, 0, textTexCoordBuffer);
    gl2f.glDrawArrays(GL.GL_TRIANGLES, 0, textVertexCount);

    gl2f.glDisableClientState(GL2.GL_TEXTURE_COORD_ARRAY);
    gl2f.glDisableClientState(GL2.GL_VERTEX_ARRAY);    
  }  
  
  protected void expandTextBuffers() {
    int newSize = textVertexBuffer.capacity() / 3 << 1;

    ByteBuffer vbb = ByteBuffer.allocateDirect(newSize * 3 * SIZEOF_FLOAT);
    vbb.order(ByteOrder.nativeOrder());
    textVertexBuffer = vbb.asFloatBuffer();

    ByteBuffer tbb = ByteBuffer.allocateDirect(newSize * 2 * SIZEOF_FLOAT);
    tbb.order(ByteOrder.nativeOrder());
    textTexCoordBuffer = tbb.asFloatBuffer();

    textVertexArray = new float[newSize * 3];
    textTexCoordArray = new float[newSize * 2];
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
    if (flushMode == FLUSH_AFTER_TRANSFORMATION) {
      flush();
    }
    
    gl2f.glTranslatef(tx, ty, tz);
    modelviewStack.translate(tx, ty, tz);
    modelviewUpdated = false;
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
    if (flushMode == FLUSH_AFTER_TRANSFORMATION) {
      flush();
    }
    
    gl2f.glRotatef(PApplet.degrees(angle), v0, v1, v2);
    modelviewStack.rotate(angle, v0, v1, v2);
    modelviewUpdated = false;
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
    if (flushMode == FLUSH_AFTER_TRANSFORMATION) {
      flush();
    }    
    
    if (manipulatingCamera) {
      scalingDuringCamManip = true;
    }
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
    if (flushMode == FLUSH_AFTER_TRANSFORMATION) {
      flush();
    }
    
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
    if (flushMode == FLUSH_AFTER_TRANSFORMATION) {
      flush();
    }
    
    gltemp[ 0] = n00;
    gltemp[ 1] = n10;
    gltemp[ 2] = n20;
    gltemp[ 3] = n30;

    gltemp[ 4] = n01;
    gltemp[ 5] = n11;
    gltemp[ 6] = n21;
    gltemp[ 7] = n31;

    gltemp[ 8] = n02;
    gltemp[ 9] = n12;
    gltemp[10] = n22;
    gltemp[11] = n32;

    gltemp[12] = n03;
    gltemp[13] = n13;
    gltemp[14] = n23;
    gltemp[15] = n33;

    gl2f.glMultMatrixf(gltemp, 0);

    modelviewStack.mult(gltemp);
    modelviewUpdated = false;   
  }

  public void updateModelview() {
    updateModelview(true);
  }
  
  public void updateModelview(boolean calcInv) {
    if (flushMode == FLUSH_AFTER_TRANSFORMATION) {
      flush();
    }
    
    copyPMatrixToGLArray(modelview, glmodelview);
    if (calcInv) {
      calculateModelviewInverse();
    } else {
      copyPMatrixToGLArray(modelviewInv, glmodelviewInv);
    }
    gl2f.glLoadMatrixf(glmodelview, 0);
    modelviewStack.set(glmodelview);
    modelviewUpdated = true;
  }

  
  // This method is needed to copy a PMatrix3D into a  opengl array.
  // The PMatrix3D.get(float[]) is not useful, because PMatrix3D assumes
  // row-major ordering of the elements of the float array, and opengl
  // uses column-major ordering.
  protected void copyPMatrixToGLArray(PMatrix3D src, float[] dest) {
    dest[ 0] = src.m00;
    dest[ 1] = src.m10;
    dest[ 2] = src.m20;
    dest[ 3] = src.m30;

    dest[ 4] = src.m01;
    dest[ 5] = src.m11;
    dest[ 6] = src.m21;
    dest[ 7] = src.m31;
    
    dest[ 8] = src.m02;
    dest[ 9] = src.m12;
    dest[10] = src.m22;
    dest[11] = src.m32;

    dest[12] = src.m03;
    dest[13] = src.m13;
    dest[14] = src.m23;
    dest[15] = src.m33;
  }

  // This method is needed to copy an opengl array into a  PMatrix3D.
  // The PMatrix3D.set(float[]) is not useful, because PMatrix3D assumes
  // row-major ordering of the elements of the float array, and opengl
  // uses column-major ordering.  
  protected void copyGLArrayToPMatrix(float[] src, PMatrix3D dest) {
    dest.m00 = src[0];
    dest.m10 = src[1];
    dest.m20 = src[2];
    dest.m30 = src[3];

    dest.m01 = src[4];
    dest.m11 = src[5];
    dest.m21 = src[6];
    dest.m31 = src[7];
    
    dest.m02 = src[8];
    dest.m12 = src[9];
    dest.m22 = src[10];
    dest.m32 = src[11];

    dest.m03 = src[12];
    dest.m13 = src[13];
    dest.m23 = src[14];
    dest.m33 = src[15];
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
  /*
   * private boolean drawing2D() { if (modelview.m00 != 1.0f || modelview.m11 !=
   * 1.0f || modelview.m22 != 1.0f || // check scale modelview.m01 != 0.0f ||
   * modelview.m02 != 0.0f || // check rotational pieces modelview.m10 != 0.0f
   * || modelview.m12 != 0.0f || modelview.m20 != 0.0f || modelview.m21 != 0.0f
   * || !((camera.m23-modelview.m23) <= EPSILON && (camera.m23-modelview.m23) >=
   * -EPSILON)) { // check for z-translation // Something about the modelview
   * matrix indicates 3d drawing // (or rotated 2d, in which case 2d subpixel
   * fixes probably aren't needed) return false; } else { //The matrix is
   * mapping z=0 vertices to the screen plane, // which means it's likely that
   * 2D drawing is happening. return true; } }
   */
  
  //////////////////////////////////////////////////////////////

  // PROJECTION
  
  protected void getProjectionMatrix() {
    projectionStack.get(glprojection);
    copyGLArrayToPMatrix(glprojection, projection);
    projectionUpdated = true;
  }
  
  public void pushProjection() {
    gl2f.glMatrixMode(GL2.GL_PROJECTION);
    gl2f.glPushMatrix();
    projectionStack.push();
    gl2f.glMatrixMode(GL2.GL_MODELVIEW);
  }
  
  public void popProjection() {
    gl2f.glMatrixMode(GL2.GL_PROJECTION);
    gl2f.glPopMatrix();
    projectionStack.pop();
    gl2f.glMatrixMode(GL2.GL_MODELVIEW);    
  }
    
  public void updateProjection() {
    copyPMatrixToGLArray(projection, glprojection);
    gl2f.glMatrixMode(GL2.GL_PROJECTION);
    gl2f.glLoadMatrixf(glprojection, 0);
    gl2f.glMatrixMode(GL2.GL_MODELVIEW);
    projectionStack.set(glprojection);
    projectionUpdated = true;
  }
  
  public void restoreProjection() {
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
      scalingDuringCamManip = false;
    }
  }

  public void updateCamera() {
    if (!manipulatingCamera) {
      throw new RuntimeException("Cannot call updateCamera() "
          + "without first calling beginCamera()");
    }    
    copyPMatrixToGLArray(camera, glmodelview);
    gl2f.glLoadMatrixf(glmodelview, 0);
    modelviewStack.set(glmodelview);    
    
    scalingDuringCamManip = true; // Assuming general transformation.
    modelviewUpdated = false;
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

    if (scalingDuringCamManip) {
      // General inversion Rotation+Translation+Scaling
      calculateModelviewInverse();
    } else {
      // Inverse calculation for Rotation+Translation matrix only.
      calculateModelviewInvNoScaling();
    }

    // Copying modelview matrix after camera transformations to the camera
    // matrices.
    PApplet.arrayCopy(glmodelview, pcamera);
    PApplet.arrayCopy(glmodelviewInv, pcameraInv);
    copyGLArrayToPMatrix(pcamera, camera);
    copyGLArrayToPMatrix(pcameraInv, cameraInv);

    // all done
    manipulatingCamera = false;
    scalingDuringCamManip = false;
  }

  protected void getModelviewMatrix() {
    modelviewStack.get(glmodelview);
    copyGLArrayToPMatrix(glmodelview, modelview);
    modelviewUpdated = true;
  }

  protected void getTransformMatrix() {
    // transform = pcameraInv * modelviewStack.current
    float[] mv = modelviewStack.current;
    float[] ic = pcameraInv;
    float[] tr = transform;
    
    tr[ 0] = ic[0] * mv[ 0] + ic[4] * mv[ 1] + ic[ 8] * mv[ 2] + ic[12] * mv[ 3];
    tr[ 4] = ic[0] * mv[ 4] + ic[4] * mv[ 5] + ic[ 8] * mv[ 6] + ic[12] * mv[ 7];
    tr[ 8] = ic[0] * mv[ 8] + ic[4] * mv[ 9] + ic[ 8] * mv[10] + ic[12] * mv[11];
    tr[12] = ic[0] * mv[12] + ic[4] * mv[13] + ic[ 8] * mv[14] + ic[12] * mv[15];

    tr[ 1] = ic[1] * mv[ 0] + ic[5] * mv[ 1] + ic[ 9] * mv[ 2] + ic[13] * mv[ 3];
    tr[ 5] = ic[1] * mv[ 4] + ic[5] * mv[ 5] + ic[ 9] * mv[ 6] + ic[13] * mv[ 7];
    tr[ 9] = ic[1] * mv[ 8] + ic[5] * mv[ 9] + ic[ 9] * mv[10] + ic[13] * mv[11];
    tr[13] = ic[1] * mv[12] + ic[5] * mv[13] + ic[ 9] * mv[14] + ic[13] * mv[15];

    tr[ 2] = ic[2] * mv[ 0] + ic[6] * mv[ 1] + ic[10] * mv[ 2] + ic[14] * mv[ 3];
    tr[ 6] = ic[2] * mv[ 4] + ic[6] * mv[ 5] + ic[10] * mv[ 6] + ic[14] * mv[ 7];
    tr[10] = ic[2] * mv[ 8] + ic[6] * mv[ 9] + ic[10] * mv[10] + ic[14] * mv[11];
    tr[14] = ic[2] * mv[12] + ic[6] * mv[13] + ic[10] * mv[14] + ic[14] * mv[15];

    tr[ 3] = ic[3] * mv[ 0] + ic[7] * mv[ 1] + ic[11] * mv[ 2] + ic[15] * mv[ 3];
    tr[ 7] = ic[3] * mv[ 4] + ic[7] * mv[ 5] + ic[11] * mv[ 6] + ic[15] * mv[ 7];
    tr[11] = ic[3] * mv[ 8] + ic[7] * mv[ 9] + ic[11] * mv[10] + ic[15] * mv[11];
    tr[15] = ic[3] * mv[12] + ic[7] * mv[13] + ic[11] * mv[14] + ic[15] * mv[15];
  }  
  
  // Calculates the inverse of the modelview matrix.
  // From Matrix4<Real> Matrix4<Real>::Inverse in 
  // http://www.geometrictools.com/LibMathematics/Algebra/Wm5Matrix4.inl
  protected void calculateModelviewInverse() {
    float[] m = glmodelview;
    float[] inv = glmodelviewInv; 
    
    float a0 = m[ 0] * m[ 5] - m[ 1] * m[ 4];
    float a1 = m[ 0] * m[ 6] - m[ 2] * m[ 4];
    float a2 = m[ 0] * m[ 7] - m[ 3] * m[ 4];
    float a3 = m[ 1] * m[ 6] - m[ 2] * m[ 5];
    float a4 = m[ 1] * m[ 7] - m[ 3] * m[ 5];
    float a5 = m[ 2] * m[ 7] - m[ 3] * m[ 6];
    float b0 = m[ 8] * m[13] - m[ 9] * m[12];
    float b1 = m[ 8] * m[14] - m[10] * m[12];
    float b2 = m[ 8] * m[15] - m[11] * m[12];
    float b3 = m[ 9] * m[14] - m[10] * m[13];
    float b4 = m[ 9] * m[15] - m[11] * m[13];
    float b5 = m[10] * m[15] - m[11] * m[14];

    float det = a0 * b5 - a1 * b4 + a2 * b3 + a3 * b2 - a4 * b1 + a5 * b0;
    
    if (PApplet.abs(det) > 0)  {
      inv[ 0] = + m[ 5] * b5 - m[ 6] * b4 + m[ 7] * b3;
      inv[ 4] = - m[ 4] * b5 + m[ 6] * b2 - m[ 7] * b1;
      inv[ 8] = + m[ 4] * b4 - m[ 5] * b2 + m[ 7] * b0;
      inv[12] = - m[ 4] * b3 + m[ 5] * b1 - m[ 6] * b0;
      inv[ 1] = - m[ 1] * b5 + m[ 2] * b4 - m[ 3] * b3;
      inv[ 5] = + m[ 0] * b5 - m[ 2] * b2 + m[ 3] * b1;
      inv[ 9] = - m[ 0] * b4 + m[ 1] * b2 - m[ 3] * b0;
      inv[13] = + m[ 0] * b3 - m[ 1] * b1 + m[ 2] * b0;
      inv[ 2] = + m[13] * a5 - m[14] * a4 + m[15] * a3;
      inv[ 6] = - m[12] * a5 + m[14] * a2 - m[15] * a1;
      inv[10] = + m[12] * a4 - m[13] * a2 + m[15] * a0;
      inv[14] = - m[12] * a3 + m[13] * a1 - m[14] * a0;
      inv[ 3] = - m[ 9] * a5 + m[10] * a4 - m[11] * a3;
      inv[ 7] = + m[ 8] * a5 - m[10] * a2 + m[11] * a1;
      inv[11] = - m[ 8] * a4 + m[ 9] * a2 - m[11] * a0;
      inv[15] = + m[ 8] * a3 - m[ 9] * a1 + m[10] * a0;

      float invDet = 1.0f / det;
      inv[0] *= invDet;
      inv[1] *= invDet;
      inv[2] *= invDet;
      inv[3] *= invDet;
      inv[4] *= invDet;
      inv[5] *= invDet;
      inv[6] *= invDet;
      inv[7] *= invDet;
      inv[8] *= invDet;
      inv[9] *= invDet;
      inv[10] *= invDet;
      inv[11] *= invDet;
      inv[12] *= invDet;
      inv[13] *= invDet;
      inv[14] *= invDet;
      inv[15] *= invDet;
      
      copyGLArrayToPMatrix(inv, modelviewInv);      
    }
  }

  // Calculates the inverse of the modelview matrix, assuming that no scaling
  // transformation was applied, only translations and rotations.
  // Here is the derivation of the formula:
  // http://www-graphics.stanford.edu/courses/cs248-98-fall/Final/q4.html
  protected void calculateModelviewInvNoScaling() {
    float[] m = glmodelview;
    float[] inv = glmodelviewInv; 
    
    float ux = m[0];
    float uy = m[1];
    float uz = m[2];

    float vx = m[4];
    float vy = m[5];
    float vz = m[6];

    float wx = m[8];
    float wy = m[9];
    float wz = m[10];

    float tx = m[12];
    float ty = m[13];
    float tz = m[14];

    inv[0] = ux;
    inv[1] = vx;
    inv[2] = wx;
    inv[3] = 0.0f;

    inv[4] = uy;
    inv[5] = vy;
    inv[6] = wy;
    inv[7] = 0.0f;

    inv[8] = uz;
    inv[9] = vz;
    inv[10] = wz;
    inv[11] = 0;

    inv[12] = -(ux * tx + uy * ty + uz * tz);
    inv[13] = -(vx * tx + vy * ty + vz * tz);
    inv[14] = -(wx * tx + wy * ty + wz * tz);
    inv[15] = 1.0f;
    
    copyGLArrayToPMatrix(inv, modelviewInv);
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
    modelviewStack.set(glmodelview);
    copyGLArrayToPMatrix(glmodelview, modelview);
    modelviewUpdated = true;

    calculateModelviewInvNoScaling();
    
    PApplet.arrayCopy(glmodelview, pcamera);
    PApplet.arrayCopy(glmodelviewInv, pcameraInv);
    copyGLArrayToPMatrix(pcamera, camera);
    copyGLArrayToPMatrix(pcameraInv, cameraInv);        
  }

  public void restoreCamera() {
    PApplet.arrayCopy(pcamera, glmodelview);
    PApplet.arrayCopy(pcameraInv, glmodelviewInv);
    copyGLArrayToPMatrix(pcamera, camera);
    copyGLArrayToPMatrix(pcameraInv, cameraInv);
    
    gl2f.glMatrixMode(GL2.GL_MODELVIEW);
    gl2f.glLoadMatrixf(glmodelview, 0);
    modelviewStack.set(glmodelview);
    copyGLArrayToPMatrix(glmodelview, modelview);
    modelviewUpdated = true;    
  }
  
  /**
   * Print the current camera matrix.
   */
  public void printCamera() {
    PMatrix3D temp = new PMatrix3D();
    copyGLArrayToPMatrix(pcamera, temp);
    temp.print();
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
    y = -1 * y; // To take into account Processsing's inverted Y axis with
                // respect to OpenGL.

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
    y = -1 * y; // To take into account Processsing's inverted Y axis with
                // respect to OpenGL.
    
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
    y = -1 * y; // To take into account Processsing's inverted Y axis with
                // respect to OpenGL.
    
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
    if (join != DEFAULT_STROKE_JOIN) {
      showMethodWarning("strokeJoin");
    }
  }

  public void strokeCap(int cap) {
    if (cap != DEFAULT_STROKE_CAP) {
      showMethodWarning("strokeCap");
    }
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
    if (!lights) {
      enableLighting();
    }
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
    if (!lights) {
      enableLighting();
    }    
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
    if (!lights) {
      enableLighting();
    }    
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
    if (!lights) {
      enableLighting();
    }    
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
    lights = true;
    gl2f.glEnable(GL2.GL_LIGHTING);
  }

  protected void disableLighting() {
    lights = false;
    gl2f.glDisable(GL2.GL_LIGHTING);
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
      texture.setImage(this);
      
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
    screenBlendMode = mode;
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
    } else if (img.isModified()) {
      if (img.width != tex.width || img.height != tex.height) {
        tex.init(img.width, img.height);
      }
      updateTexture(img, tex);
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
    tex.set(img.pixels);
    img.setCache(ogl, tex);
    tex.setImage(img); // The parent image so the texture can regenerate itself upon re-allocation.
    return tex;
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
  
  
  /** Utility function to render texture. */
  protected void drawTexture(int target, int id, int tw, int th, int[] crop, int x, int y, int w, int h) {
    gl.glEnable(target);
    gl.glBindTexture(target, id);    
    gl.glDisable(GL.GL_BLEND);    
    
    // The texels of the texture replace the color of wherever is on the screen.
    gl2f.glTexEnvi(GL2.GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_MODE, GL2.GL_REPLACE);       
    
    drawTexture(tw, th, crop, x, y, w, h);
    
    // Returning to the default texture environment mode, GL_MODULATE. This allows tinting a texture
    // with the current fragment color.
    gl2f.glTexEnvi(GL2.GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_MODE, GL2.GL_MODULATE);        
    
    gl.glBindTexture(target, 0);
    gl.glDisable(target);
    
    blendMode(screenBlendMode);
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
    
    if (hints[DISABLE_DEPTH_MASK]) {
      gl.glDepthMask(false);  
    } else {
      gl.glDepthMask(true);
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
    
    //profile = GLProfile.getDefault();
    profile = GLProfile.get(GLProfile.GL2ES1);
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
      PApplet.println("Requested multisample level: " + capabilities.getNumSamples());
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
  
  
  /**
   *  This class encapsulates a static matrix stack that can be used
   *  to mirror the changes in OpenGL matrices.
   */
  protected class GLMatrixStack {
    protected Stack<float[]> matrixStack;
    protected float[] current;
    
    public GLMatrixStack() {
      matrixStack = new Stack<float[]>();
      current = new float[16];
      setIdentity();
    }
    
    public void setIdentity() {
      set(1, 0, 0, 0,
          0, 1, 0, 0,
          0, 0, 1, 0,
          0, 0, 0, 1);
    }
    
    public void push() {
      float[] mat = new float[16];
      PApplet.arrayCopy(current, mat);
      matrixStack.push(mat);
    }
    
    public void pop() {
      try {
        float[] mat = matrixStack.pop();
        PApplet.arrayCopy(mat, current);
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
    
    public void mult(float n0, float n4, float n8, float n12,
                     float n1, float n5, float n9, float n13,
                     float n2, float n6, float n10, float n14,
                     float n3, float n7, float n11, float n15) {
      float r0  = current[0] *  n0 + current[4] *  n1 + current[ 8] *  n2 + current[12] *  n3;
      float r4  = current[0] *  n4 + current[4] *  n5 + current[ 8] *  n6 + current[12] *  n7;
      float r8  = current[0] *  n8 + current[4] *  n9 + current[ 8] * n10 + current[12] * n11;
      float r12 = current[0] * n12 + current[4] * n13 + current[ 8] * n14 + current[12] * n15;

      float r1  = current[1] *  n0 + current[5] *  n1 + current[ 9] *  n2 + current[13] *  n3;
      float r5  = current[1] *  n4 + current[5] *  n5 + current[ 9] *  n6 + current[13] *  n7;
      float r9  = current[1] *  n8 + current[5] *  n9 + current[ 9] * n10 + current[13] * n11;
      float r13 = current[1] * n12 + current[5] * n13 + current[ 9] * n14 + current[13] * n15;

      float r2  = current[2] *  n0 + current[6] *  n1 + current[10] *  n2 + current[14] *  n3;
      float r6  = current[2] *  n4 + current[6] *  n5 + current[10] *  n6 + current[14] *  n7;
      float r10 = current[2] *  n8 + current[6] *  n9 + current[10] * n10 + current[14] * n11;
      float r14 = current[2] * n12 + current[6] * n13 + current[10] * n14 + current[14] * n15;

      float r3  = current[3] *  n0 + current[7] *  n1 + current[11] *  n2 + current[15] *  n3;
      float r7  = current[3] *  n4 + current[7] *  n5 + current[11] *  n6 + current[15] *  n7;
      float r11 = current[3] *  n8 + current[7] *  n9 + current[11] * n10 + current[15] * n11;
      float r15 = current[3] * n12 + current[7] * n13 + current[11] * n14 + current[15] * n15;

      current[0] = r0; current[4] = r4; current[ 8] =  r8; current[12] = r12;
      current[1] = r1; current[5] = r5; current[ 9] =  r9; current[13] = r13;
      current[2] = r2; current[6] = r6; current[10] = r10; current[14] = r14;
      current[3] = r3; current[7] = r7; current[11] = r11; current[15] = r15;      
    }
    
    public void get(float[] mat) {
      PApplet.arrayCopy(current, mat);  
    }

    public void set(float[] mat) {
      PApplet.arrayCopy(mat, current);
    }
    
    public void set(float n0, float n4, float n8, float n12,
                    float n1, float n5, float n9, float n13,
                    float n2, float n6, float n10, float n14,
                    float n3, float n7, float n11, float n15) {
      current[0] = n0; current[4] = n4; current[ 8] = n8;  current[12] = n12;
      current[1] = n1; current[5] = n5; current[ 9] = n9;  current[13] = n13;
      current[2] = n2; current[6] = n6; current[10] = n10; current[14] = n14;
      current[3] = n3; current[7] = n7; current[11] = n11; current[15] = n15;      
    }
    
    public void translate(float tx, float ty, float tz) {
      current[12] += tx * current[0] + ty * current[4] + tz * current[ 8];
      current[13] += tx * current[1] + ty * current[5] + tz * current[ 9];
      current[14] += tx * current[2] + ty * current[6] + tz * current[10];
      current[15] += tx * current[3] + ty * current[7] + tz * current[11];      
    }

    public void rotate(float angle, float rx, float ry, float rz) {
      float c = PApplet.cos(angle);
      float s = PApplet.sin(angle);
      float t = 1.0f - c;
      
      mult((t*rx*rx) + c     , (t*rx*ry) - (s*rz), (t*rx*rz) + (s*ry), 0,
           (t*rx*ry) + (s*rz),      (t*ry*ry) + c, (t*ry*rz) - (s*rx), 0,
           (t*rx*rz) - (s*ry), (t*ry*rz) + (s*rx),      (t*rz*rz) + c, 0,
                            0,                  0,                  0, 1);
    }
    
    public void scale(float sx, float sy, float sz) {
      current[0] *= sx;  current[4] *= sy;  current[ 8] *= sz;
      current[1] *= sx;  current[5] *= sy;  current[ 9] *= sz;
      current[2] *= sx;  current[6] *= sy;  current[10] *= sz;
      current[3] *= sx;  current[7] *= sy;  current[11] *= sz;      
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
    
    // Range of vertices that will be processed by the 
    // tessellator.
    public int firstVertex;
    public int lastVertex;    
    
    public int[] codes;
    public float[] vertices;  
    public float[] colors;
    public float[] normals;
    public float[] texcoords;
    public float[] strokes;

    public InGeometry() {
      allocate();
    }    
    
    public void reset() {
      vertexCount = firstVertex = lastVertex = 0;
    }
    
    public void allocate() {      
      codes = new int[DEFAULT_VERTICES];
      vertices = new float[3 * DEFAULT_VERTICES];
      colors = new float[4 * DEFAULT_VERTICES];      
      normals = new float[3 * DEFAULT_VERTICES];
      texcoords = new float[2 * DEFAULT_VERTICES];
      strokes = new float[5 * DEFAULT_VERTICES];
      reset();
    }
    
    public void dispose() {
      codes = null;
      vertices = null;
      colors = null;      
      normals = null;
      texcoords = null;
      strokes = null;      
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
    
    public void addVertex(float[] vertex, float[] color, float[] normal, float[] texcoord, float[] stroke, int code) {
      vertexCheck();

      codes[vertexCount] = code;      
      PApplet.arrayCopy(vertex, 0, vertices, 3 * vertexCount, 3);
      PApplet.arrayCopy(color, 0, colors, 4 * vertexCount, 4);
      PApplet.arrayCopy(normal, 0, normals, 3 * vertexCount, 3);
      PApplet.arrayCopy(texcoord, 0, texcoords, 2 * vertexCount, 2);      
      PApplet.arrayCopy(stroke, 0, strokes, 5 * vertexCount, 5);
            
      lastVertex = vertexCount; 
      vertexCount++;      
    }
    
    public void vertexCheck() {
      if (vertexCount == vertices.length / 3) {
        int newSize = vertexCount << 1; // newSize = 2 * vertexCount  

        expandCodes(newSize);
        expandVertices(newSize);
        expandColors(newSize);
        expandNormals(newSize);
        expandTexcoords(newSize);      
        expandStrokes(newSize);
      }
    }  
    
    protected void expandCodes(int n) {
      int temp[] = new int[n];      
      System.arraycopy(codes, 0, temp, 0, vertexCount);
      codes = temp;    
    }

    protected void expandVertices(int n) {
      float temp[] = new float[3 * n];      
      System.arraycopy(vertices, 0, temp, 0, 3 * vertexCount);
      vertices = temp;    
    }

    protected void expandColors(int n){
      float temp[] = new float[4 * n];      
      System.arraycopy(colors, 0, temp, 0, 4 * vertexCount);
      colors = temp;  
    }

    protected void expandNormals(int n) {
      float temp[] = new float[3 * n];      
      System.arraycopy(normals, 0, temp, 0, 3 * vertexCount);
      normals = temp;    
    }    
    
    protected void expandTexcoords(int n) {
      float temp[] = new float[2 * n];      
      System.arraycopy(texcoords, 0, temp, 0, 2 * vertexCount);
      texcoords = temp;    
    }
        
    protected void expandStrokes(int n) {
      float temp[] = new float[5 * n];      
      System.arraycopy(strokes, 0, temp, 0, 5 * vertexCount);
      strokes = temp;
    }      
  }
  
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
    }
      
    public void allocate() {     
      fillVertices = new float[0];
      fillColors = new float[0];
      fillNormals = new float[0];
      fillTexcoords = new float[0];
      fillIndices = new int[0];  
      
      lineVertices = new float[0];
      lineColors = new float[0];
      lineNormals = new float[0];
      lineAttributes = new float[0];
      lineIndices = new int[0];       
      
      pointVertices = new float[0];
      pointColors = new float[0];
      pointNormals = new float[0];
      pointAttributes = new float[0];
      pointIndices = new int[0];
      
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
      return MAX_TESS_VERTICES <= fillVertexCount + lineVertexCount + pointVertexCount;
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
        int newSize = fillIndexCount == 0 ? DEFAULT_TESS_INDICES : fillIndexCount << 1;
        expandFillIndices(newSize);
      }
    }    
    
    public void expandFillIndices(int n) {
      int temp[] = new int[n];      
      System.arraycopy(fillIndices, 0, temp, 0, fillIndexCount);
      fillIndices = temp;      
    }
    
    public void addFillIndex(int idx) {
      fillIndexCheck();
      fillIndices[fillIndexCount] = idx;
      fillIndexCount++;
    }
    
    public void fillVertexCheck() {
      if (fillVertexCount == fillVertices.length / 3) {
        int newSize = fillVertexCount == 0 ? DEFAULT_TESS_VERTICES : fillVertexCount << 1; 
      
        expandFillVertices(newSize);
        expandFillColors(newSize);              
        expandFillNormals(newSize);
        expandFillTexcoords(newSize);                
      }
    }
    
    public void addFillVertices(int count) {
      if (fillVertexCount + count >= fillVertices.length / 3) {
        int newSize = fillVertexCount + count;
        
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
      if (fillIndexCount + count >= fillIndices.length) {
        int newSize = fillIndexCount + count;
        
        expandFillIndices(newSize);
      }
     
      firstFillIndex = fillIndexCount;
      fillIndexCount += count;            
      lastFillIndex = fillIndexCount - 1;   
    }     
    
    protected void expandFillVertices(int n) {
      float temp[] = new float[3 * n];      
      System.arraycopy(fillVertices, 0, temp, 0, 3 * fillVertexCount);
      fillVertices = temp;       
    }

    protected void expandFillColors(int n) {
      float temp[] = new float[4 * n];      
      System.arraycopy(fillColors, 0, temp, 0, 4 * fillVertexCount);
      fillColors = temp;
    }
    
    protected void expandFillNormals(int n) {
      float temp[] = new float[3 * n];      
      System.arraycopy(fillNormals, 0, temp, 0, 3 * fillVertexCount);
      fillNormals = temp;       
    }
    
    protected void expandFillTexcoords(int n) {
      float temp[] = new float[2 * n];      
      System.arraycopy(fillTexcoords, 0, temp, 0, 2 * fillVertexCount);
      fillTexcoords = temp;
    }
    
    public void addLineVertices(int count) {
      if (lineVertexCount + count >= lineVertices.length / 3) {
        int newSize = lineVertexCount + count;
        
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
      System.arraycopy(lineVertices, 0, temp, 0, 3 * lineVertexCount);
      lineVertices = temp;  
    }
    
    public void expandLineColors(int n) {
      float temp[] = new float[4 * n];      
      System.arraycopy(lineColors, 0, temp, 0, 4 * lineVertexCount);
      lineColors = temp;      
    }
    
    public void expandLineNormals(int n) {
      float temp[] = new float[3 * n];      
      System.arraycopy(lineNormals, 0, temp, 0, 3 * lineVertexCount);
      lineNormals = temp;      
    }
    
    public void expandLineAttributes(int n) {
      float temp[] = new float[4 * n];      
      System.arraycopy(lineAttributes, 0, temp, 0, 4 * lineVertexCount);
      lineAttributes = temp;      
    }      
    
    public void addLineIndices(int count) {
      if (lineIndexCount + count >= lineIndices.length) {
        int newSize = lineIndexCount + count;
        
        expandLineIndices(newSize);
      }
     
      firstLineIndex = lineIndexCount;
      lineIndexCount += count;      
      lastLineIndex = lineIndexCount - 1;   
    }   
    
    public void expandLineIndices(int n) {
      int temp[] = new int[n];      
      System.arraycopy(lineIndices, 0, temp, 0, lineIndexCount);
      lineIndices = temp;        
    }
    
    public void addPointVertices(int count) {
      if (pointVertexCount + count >= pointVertices.length / 3) {
        int newSize = pointVertexCount + count;
        
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
      System.arraycopy(pointVertices, 0, temp, 0, 3 * pointVertexCount);
      pointVertices = temp;  
    }
    
    public void expandPointColors(int n) {
      float temp[] = new float[4 * n];      
      System.arraycopy(pointColors, 0, temp, 0, 4 * pointVertexCount);
      pointColors = temp;      
    }
    
    public void expandPointNormals(int n) {
      float temp[] = new float[3 * n];      
      System.arraycopy(pointNormals, 0, temp, 0, 3 * pointVertexCount);
      pointNormals = temp;      
    }
    
    public void expandPointAttributes(int n) {
      float temp[] = new float[2 * n];      
      System.arraycopy(pointAttributes, 0, temp, 0, 2 * pointVertexCount);
      pointAttributes = temp;      
    }
    
    public void addPointIndices(int count) {
      if (pointIndexCount + count >= pointIndices.length) {
        int newSize = pointIndexCount + count;
        
        expandPointIndices(newSize);
      }
     
      firstPointIndex = pointIndexCount;
      pointIndexCount += count;      
      lastPointIndex = pointIndexCount - 1;   
    }   
    
    public void expandPointIndices(int n) {
      int temp[] = new int[n];      
      System.arraycopy(pointIndices, 0, temp, 0, pointIndexCount);
      pointIndices = temp;        
    }
    
    public void addFillVertex(float x, float y, float z, float r, 
                              float g, float b, float a,
                              float nx, float ny, float nz, 
                              float u, float v) {
      fillVertexCheck();
      
      if (renderMode == IMMEDIATE && flushMode == FLUSH_WHEN_FULL) {
        float[] mm = transform;
        
        fillVertices[3 * fillVertexCount + 0] = x * mm[0] + y * mm[4] + z * mm[ 8] + mm[12];
        fillVertices[3 * fillVertexCount + 1] = x * mm[1] + y * mm[5] + z * mm[ 9] + mm[13];
        fillVertices[3 * fillVertexCount + 2] = x * mm[2] + y * mm[6] + z * mm[10] + mm[14];
        
        fillNormals[3 * fillVertexCount + 0] = nx * mm[0] + ny * mm[4] + nz * mm[ 8] + mm[12];
        fillNormals[3 * fillVertexCount + 1] = nx * mm[1] + ny * mm[5] + nz * mm[ 9] + mm[13];
        fillNormals[3 * fillVertexCount + 2] = nx * mm[2] + ny * mm[6] + nz * mm[10] + mm[14];
      } else {
        fillVertices[3 * fillVertexCount + 0] = x;
        fillVertices[3 * fillVertexCount + 1] = y;
        fillVertices[3 * fillVertexCount + 2] = z;

        fillNormals[3 * fillVertexCount + 0] = nx;
        fillNormals[3 * fillVertexCount + 1] = ny;
        fillNormals[3 * fillVertexCount + 2] = nz;        
      }
      
      fillColors[4 * fillVertexCount + 0] = r;
      fillColors[4 * fillVertexCount + 1] = g;
      fillColors[4 * fillVertexCount + 2] = b;
      fillColors[4 * fillVertexCount + 3] = a;
      
      fillTexcoords[2 * fillVertexCount + 0] = u;
      fillTexcoords[2 * fillVertexCount + 1] = v;      
    }    

    public void addFillVertices(InGeometry in) {
      int i0 = in.firstVertex;
      int i1 = in.lastVertex;
      int nvert = i1 - i0 + 1;
      
      addFillVertices(nvert);
      
      if (renderMode == IMMEDIATE && flushMode == FLUSH_WHEN_FULL) {
        float[] mm = transform;
        for (int i = 0; i < nvert; i++) {
          int inIdx = i0 + i;
          int tessIdx = firstFillVertex + i;
          
          float x = in.vertices[3 * inIdx + 0];
          float y = in.vertices[3 * inIdx + 1];
          float z = in.vertices[3 * inIdx + 2];
          
          float nx = in.normals[3 * inIdx + 0];
          float ny = in.normals[3 * inIdx + 1];
          float nz = in.normals[3 * inIdx + 2];
          
          fillVertices[3 * tessIdx + 0] = x * mm[0] + y * mm[4] + z * mm[ 8] + mm[12];
          fillVertices[3 * tessIdx + 1] = x * mm[1] + y * mm[5] + z * mm[ 9] + mm[13];
          fillVertices[3 * tessIdx + 2] = x * mm[2] + y * mm[6] + z * mm[10] + mm[14];
          
          fillNormals[3 * tessIdx + 0] = nx * mm[0] + ny * mm[4] + nz * mm[ 8] + mm[12];
          fillNormals[3 * tessIdx + 1] = nx * mm[1] + ny * mm[5] + nz * mm[ 9] + mm[13];
          fillNormals[3 * tessIdx + 2] = nx * mm[2] + ny * mm[6] + nz * mm[10] + mm[14];          
        }        
      } else {
        PApplet.arrayCopy(in.vertices, 3 * i0, fillVertices, 3 * firstFillVertex, 3 * nvert);
        PApplet.arrayCopy(in.normals, 3 * i0, fillNormals, 3 * firstFillVertex, 3 * nvert);        
      }
              
      PApplet.arrayCopy(in.colors, 4 * i0, fillColors, 4 * firstFillVertex, 4 * nvert);      
      PApplet.arrayCopy(in.texcoords, 2 * i0, fillTexcoords, 2 * firstFillVertex, 2 * nvert);
    }     
    
    public void putLineVertex(InGeometry in, int inIdx0, int inIdx1, int tessIdx) {
      if (renderMode == IMMEDIATE && flushMode == FLUSH_WHEN_FULL) {
        float[] mm = transform;
        
        float x0 = in.vertices[3 * inIdx0 + 0];
        float y0 = in.vertices[3 * inIdx0 + 1];
        float z0 = in.vertices[3 * inIdx0 + 2];
        
        float nx = in.normals[3 * inIdx0 + 0];
        float ny = in.normals[3 * inIdx0 + 1];
        float nz = in.normals[3 * inIdx0 + 2];
        
        lineVertices[3 * tessIdx + 0] = x0 * mm[0] + y0 * mm[4] + z0 * mm[ 8] + mm[12];
        lineVertices[3 * tessIdx + 1] = x0 * mm[1] + y0 * mm[5] + z0 * mm[ 9] + mm[13];
        lineVertices[3 * tessIdx + 2] = x0 * mm[2] + y0 * mm[6] + z0 * mm[10] + mm[14];
        
        lineNormals[3 * tessIdx + 0] = nx * mm[0] + ny * mm[4] + nz * mm[ 8] + mm[12];
        lineNormals[3 * tessIdx + 1] = nx * mm[1] + ny * mm[5] + nz * mm[ 9] + mm[13];
        lineNormals[3 * tessIdx + 2] = nx * mm[2] + ny * mm[6] + nz * mm[10] + mm[14];
        
        float x1 = in.vertices[3 * inIdx1 + 0];
        float y1 = in.vertices[3 * inIdx1 + 1];
        float z1 = in.vertices[3 * inIdx1 + 2];

        lineAttributes[4 * tessIdx + 0] = x1 * mm[0] + y1 * mm[4] + z1 * mm[ 8] + mm[12];
        lineAttributes[4 * tessIdx + 1] = x1 * mm[1] + y1 * mm[5] + z1 * mm[ 9] + mm[13];
        lineAttributes[4 * tessIdx + 2] = x1 * mm[2] + y1 * mm[6] + z1 * mm[10] + mm[14];        
      } else {
        System.arraycopy(in.vertices, 3 * inIdx0, lineVertices, 3 * tessIdx, 3);
        System.arraycopy(in.normals, 3 * inIdx0, lineNormals, 3 * tessIdx, 3); 
        System.arraycopy(in.vertices, 3 * inIdx1, lineAttributes, 4 * tessIdx, 3);
      }      
      
      System.arraycopy(in.strokes, 5 * inIdx0, lineColors, 4 * tessIdx, 4);    
    }
        
    public void putPointVertex(InGeometry in, int inIdx, int tessIdx) {
      if (renderMode == IMMEDIATE && flushMode == FLUSH_WHEN_FULL) {
        float[] mm = transform;
        
        float x = in.vertices[3 * inIdx + 0];
        float y = in.vertices[3 * inIdx + 1];
        float z = in.vertices[3 * inIdx + 2];
        
        float nx = in.normals[3 * inIdx + 0];
        float ny = in.normals[3 * inIdx + 1];
        float nz = in.normals[3 * inIdx + 2];
        
        pointVertices[3 * tessIdx + 0] = x * mm[0] + y * mm[4] + z * mm[ 8] + mm[12];
        pointVertices[3 * tessIdx + 1] = x * mm[1] + y * mm[5] + z * mm[ 9] + mm[13];
        pointVertices[3 * tessIdx + 2] = x * mm[2] + y * mm[6] + z * mm[10] + mm[14];
        
        pointNormals[3 * tessIdx + 0] = nx * mm[0] + ny * mm[4] + nz * mm[ 8] + mm[12];
        pointNormals[3 * tessIdx + 1] = nx * mm[1] + ny * mm[5] + nz * mm[ 9] + mm[13];
        pointNormals[3 * tessIdx + 2] = nx * mm[2] + ny * mm[6] + nz * mm[10] + mm[14];           
      } else {
        System.arraycopy(in.vertices, 3 * inIdx, pointVertices, 3 * tessIdx, 3);
        System.arraycopy(in.normals, 3 * inIdx, pointNormals, 3 * tessIdx, 3);        
      }      
      
      System.arraycopy(in.strokes, 5 * inIdx, pointColors, 4 * tessIdx, 4);
    }    
  }
  
  public class Tessellator {
    final protected int MIN_ACCURACY = 6; 
    final protected float sinLUT[];
    final protected float cosLUT[];
    final protected float SINCOS_PRECISION = 0.5f;
    final protected int SINCOS_LENGTH = (int) (360f / SINCOS_PRECISION);
//    static {
//      sinLUT = new float[SINCOS_LENGTH];
//      cosLUT = new float[SINCOS_LENGTH];
//      for (int i = 0; i < SINCOS_LENGTH; i++) {
//        sinLUT[i] = (float) Math.sin(i * DEG_TO_RAD * SINCOS_PRECISION);
//        cosLUT[i] = (float) Math.cos(i * DEG_TO_RAD * SINCOS_PRECISION);
//      }      
//    }  
    final protected float[][] QUAD_SIGNS = { {-1, +1}, {-1, -1}, {+1, -1}, {+1, +1} };
    
    public GLUtessellator gluTess;
    InGeometry inGeo; 
    TessGeometry tessGeo;
    GLU glu;
    
    public Tessellator() {
      sinLUT = new float[SINCOS_LENGTH];
      cosLUT = new float[SINCOS_LENGTH];
      for (int i = 0; i < SINCOS_LENGTH; i++) {
        sinLUT[i] = (float) Math.sin(i * DEG_TO_RAD * SINCOS_PRECISION);
        cosLUT[i] = (float) Math.cos(i * DEG_TO_RAD * SINCOS_PRECISION);
      }
      
      glu = new GLU();
      
      gluTess = GLU.gluNewTess();
      GLUTessCallback tessCallback;
    
      tessCallback = new GLUTessCallback();
      GLU.gluTessCallback(gluTess, GLU.GLU_TESS_BEGIN, tessCallback);
      GLU.gluTessCallback(gluTess, GLU.GLU_TESS_END, tessCallback);
      GLU.gluTessCallback(gluTess, GLU.GLU_TESS_VERTEX, tessCallback);
      GLU.gluTessCallback(gluTess, GLU.GLU_TESS_COMBINE, tessCallback);
      GLU.gluTessCallback(gluTess, GLU.GLU_TESS_ERROR, tessCallback);        
    }

    public void setInGeometry(InGeometry in) {
      this.inGeo = in;
    }

    public void setTessGeometry(TessGeometry tess) {
      this.tessGeo = tess;
    }
        
    public void tessellatePoints(int cap) {
      if (cap == ROUND) {
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
      for (int i = inGeo.firstVertex; i <= inGeo.lastVertex; i++) {
        float w = inGeo.strokes[5 * i + 4];
        int perim = PApplet.max(MIN_ACCURACY, (int) (TWO_PI * w / 20));
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
        float w = inGeo.strokes[5 * i + 4];
        int perim = PApplet.max(MIN_ACCURACY, (int) (TWO_PI * w / 20));
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
          tessGeo.pointAttributes[2 * attribIdx + 0] = cosLUT[(int) val] * w/2;
          tessGeo.pointAttributes[2 * attribIdx + 1] = sinLUT[(int) val] * w/2;
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
    }
    
    protected void tessellateSquarePoints() {
      // Each point generates a separate quad.
      int quadCount = inGeo.lastVertex - inGeo.firstVertex + 1;
      
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
        float w = inGeo.strokes[5 * i + 4];
        for (int k = 0; k < 4; k++) {
          tessGeo.pointAttributes[2 * attribIdx + 0] = QUAD_SIGNS[k][0] * w/2;
          tessGeo.pointAttributes[2 * attribIdx + 1] = QUAD_SIGNS[k][1] * w/2;               
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
    }
    
    public void tessellateLines() {      
      int lineCount = (inGeo.lastVertex - inGeo.firstVertex + 1) / 2;
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
      int first = inGeo.firstVertex;
      for (int ln = 0; ln < lineCount; ln++) {
        int i0 = first + 2 * ln + 0;
        int i1 = first + 2 * ln + 1;
        addLine(i0, i1, vcount, icount); vcount += 4; icount += 6;
      }    
    }

    public void tessellateTriangles() {
      tessGeo.addFillVertices(inGeo);

      int nvertFill = inGeo.lastVertex - inGeo.firstVertex + 1;
      int triCount = nvertFill / 3;
      
      tessGeo.addFillIndices(nvertFill);
      int idx0 = tessGeo.firstFillIndex;
      int offset = tessGeo.firstFillVertex;
      for (int i = inGeo.firstVertex; i <= inGeo.lastVertex; i++) {
        tessGeo.fillIndices[idx0 + i] = offset + i;
      }

      // Count how many triangles in this shape
      // are stroked.
      int strokedCount = 0;      
      int first = inGeo.firstVertex;
      for (int tr = 0; tr < triCount; tr++) {
        int i0 = first + 3 * tr + 0;
        int i1 = first + 3 * tr + 1;
        int i2 = first + 3 * tr + 2;
        
        if (0 < inGeo.strokes[5 * i0 + 4] || 
            0 < inGeo.strokes[5 * i1 + 4] ||
            0 < inGeo.strokes[5 * i2 + 4]) {
          strokedCount++;
        }      
      }
      
      if (0 < strokedCount) {        
        // Each stroked triangle has 3 lines, one for each edge. 
        // These lines are made up of 4 vertices defining the quad. 
        // Each vertex has its own offset representing the stroke weight.
        int nvertLine = strokedCount * 3 * 4;
        tessGeo.addLineVertices(nvertLine);
        
        // Each stroke line has 4 vertices, defining 2 triangles, which
        // require 3 indices to specify their connectivities.
        int nind = strokedCount * 3 * 2 * 3;
        tessGeo.addLineIndices(nind);
        
        int vcount = tessGeo.firstLineVertex;
        int icount = tessGeo.firstLineIndex;
        for (int tr = 0; tr < triCount; tr++) {
          int i0 = first + 3 * tr + 0;
          int i1 = first + 3 * tr + 1;
          int i2 = first + 3 * tr + 2;        

          if (0 < inGeo.strokes[5 * i0 + 4] || 
              0 < inGeo.strokes[5 * i1 + 4] ||
              0 < inGeo.strokes[5 * i2 + 4]) {
            addLine(i0, i1, vcount, icount); vcount += 4; icount += 6;
            addLine(i1, i2, vcount, icount); vcount += 4; icount += 6;
            addLine(i2, i0, vcount, icount); vcount += 4; icount += 6;
          }
        }
        
      }
    }

    public void tessellateTriangleFan() {
      tessGeo.addFillVertices(inGeo);
      
      int nvertFill = inGeo.lastVertex - inGeo.firstVertex + 1;
      int triCount = nvertFill - 2;
      
      tessGeo.addFillIndices(3 * triCount);
      int idx = tessGeo.firstFillIndex;
      int offset = tessGeo.firstFillVertex; 
      for (int i = inGeo.firstVertex + 1; i < inGeo.lastVertex; i++) {
        tessGeo.fillIndices[idx++] = offset + inGeo.firstVertex;
        tessGeo.fillIndices[idx++] = offset + i;
        tessGeo.fillIndices[idx++] = offset + i + 1;
      }
      
      // Count how many triangles in this shape
      // are stroked.
      int strokedCount = 0;
      for (int i = inGeo.firstVertex + 1; i < inGeo.lastVertex; i++) {
        int i0 = inGeo.firstVertex;
        int i1 = i;
        int i2 = i + 1;
        
        if (0 < inGeo.strokes[5 * i0 + 4] || 
            0 < inGeo.strokes[5 * i1 + 4] ||
            0 < inGeo.strokes[5 * i2 + 4]) {
          strokedCount++;
        }      
      }    
      
      if (0 < strokedCount) {        
        // Each stroked triangle has 3 lines, one for each edge. 
        // These lines are made up of 4 vertices defining the quad. 
        // Each vertex has its own offset representing the stroke weight.
        int nvertLine = strokedCount * 3 * 4;
        tessGeo.addLineVertices(nvertLine);
        
        // Each stroke line has 4 vertices, defining 2 triangles, which
        // require 3 indices to specify their connectivities.
        int nind = strokedCount * 3 * 2 * 3;
        tessGeo.addLineIndices(nind); 
        
        int vcount = tessGeo.firstLineVertex;
        int icount = tessGeo.firstLineIndex;
        for (int i = inGeo.firstVertex + 1; i < inGeo.lastVertex; i++) {
          int i0 = inGeo.firstVertex;
          int i1 = i;
          int i2 = i + 1;     

          if (0 < inGeo.strokes[5 * i0 + 4] || 
              0 < inGeo.strokes[5 * i1 + 4] ||
              0 < inGeo.strokes[5 * i2 + 4]) {
            addLine(i0, i1, vcount, icount); vcount += 4; icount += 6;
            addLine(i1, i2, vcount, icount); vcount += 4; icount += 6;
            addLine(i2, i0, vcount, icount); vcount += 4; icount += 6;
          }
        }
      }
    }
    
    
    public void tessellateTriangleStrip() {
      tessGeo.addFillVertices(inGeo);
      
      int nvertFill = inGeo.lastVertex - inGeo.firstVertex + 1;
      int triCount = nvertFill - 2;
      
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
      
      // Count how many triangles in this shape
      // are stroked.
      int strokedCount = 0;
      for (int i = inGeo.firstVertex + 1; i < inGeo.lastVertex; i++) {
        int i0 = i;
        int i1, i2;
        if (i % 2 == 0) {
          i1 = i - 1;
          i2 = i + 1;        
        } else {
          i1 = i + 1;
          i2 = i - 1;        
        }
        
        if (0 < inGeo.strokes[5 * i0 + 4] || 
            0 < inGeo.strokes[5 * i1 + 4] ||
            0 < inGeo.strokes[5 * i2 + 4]) {
          strokedCount++;
        }      
      } 
      
      if (0 < strokedCount) {
        // Each stroked triangle has 3 lines, one for each edge. 
        // These lines are made up of 4 vertices defining the quad. 
        // Each vertex has its own offset representing the stroke weight.
        int nvertLine = strokedCount * 3 * 4;
        tessGeo.addLineVertices(nvertLine);
        
        // Each stroke line has 4 vertices, defining 2 triangles, which
        // require 3 indices to specify their connectivities.
        int nind = strokedCount * 3 * 2 * 3;
        tessGeo.addLineIndices(nind); 
        
        int vcount = tessGeo.firstLineVertex;
        int icount = tessGeo.firstLineIndex;
        for (int i = inGeo.firstVertex + 1; i < inGeo.lastVertex; i++) {
          int i0 = i;
          int i1, i2;
          if (i % 2 == 0) {
            i1 = i - 1;
            i2 = i + 1;        
          } else {
            i1 = i + 1;
            i2 = i - 1;        
          }  

          if (0 < inGeo.strokes[5 * i0 + 4] || 
              0 < inGeo.strokes[5 * i1 + 4] ||
              0 < inGeo.strokes[5 * i2 + 4]) {
            addLine(i0, i1, vcount, icount); vcount += 4; icount += 6;
            addLine(i1, i2, vcount, icount); vcount += 4; icount += 6;
            addLine(i2, i0, vcount, icount); vcount += 4; icount += 6;
          }
        }
      }
    }

    public void tessellateQuads() {
      tessGeo.addFillVertices(inGeo);

      int nvertFill = inGeo.lastVertex - inGeo.firstVertex + 1;
      int quadCount = nvertFill / 4;
      
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
      
      // Count how many quads in this shape
      // are stroked.
      int strokedCount = 0;
      for (int qd = 0; qd < quadCount; qd++) {
        int i0 = 4 * qd + 0;
        int i1 = 4 * qd + 1;
        int i2 = 4 * qd + 2;
        int i3 = 4 * qd + 3;
        
        if (0 < inGeo.strokes[5 * i0 + 4] || 
            0 < inGeo.strokes[5 * i1 + 4] ||
            0 < inGeo.strokes[5 * i2 + 4]||
            0 < inGeo.strokes[5 * i3 + 4]) {
          strokedCount++;
        }      
      }
      
      if (0 < strokedCount) {
        // Each stroked quad has 4 lines, one for each edge. 
        // These lines are made up of 4 vertices defining the quad. 
        // Each vertex has its own offset representing the stroke weight.
        int nvertLine = strokedCount * 4 * 4;
        tessGeo.addLineVertices(nvertLine);
        
        // Each stroke line has 4 vertices, defining 2 triangles, which
        // require 3 indices to specify their connectivities.
        int nind = strokedCount * 4 * 2 * 3;
        tessGeo.addLineIndices(nind); 
        
        int vcount = tessGeo.firstLineVertex;
        int icount = tessGeo.firstLineIndex;
        for (int qd = 0; qd < quadCount; qd++) {
          int i0 = 4 * qd + 0;
          int i1 = 4 * qd + 1;
          int i2 = 4 * qd + 2;
          int i3 = 4 * qd + 3;    

          if (0 < inGeo.strokes[5 * i0 + 4] || 
              0 < inGeo.strokes[5 * i1 + 4] ||
              0 < inGeo.strokes[5 * i2 + 4]||
              0 < inGeo.strokes[5 * i3 + 4]) {
            addLine(i0, i1, vcount, icount); vcount += 4; icount += 6;
            addLine(i1, i2, vcount, icount); vcount += 4; icount += 6;
            addLine(i2, i3, vcount, icount); vcount += 4; icount += 6;
            addLine(i3, i0, vcount, icount); vcount += 4; icount += 6;
          }
        }
        
      }
    }
    
    
    public void tessellateQuadStrip() {
      tessGeo.addFillVertices(inGeo);

      int nvertFill = inGeo.lastVertex - inGeo.firstVertex + 1;
      int quadCount = nvertFill / 2 - 1;
      
      tessGeo.addFillIndices(6 * quadCount);
      int idx = tessGeo.firstFillIndex;
      int offset = tessGeo.firstFillVertex; 
      for (int qd = 1; qd < nvertFill / 2; qd++) {        
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
 
      // Count how many quads in this shape
      // are stroked.
      int strokedCount = 0;
      int first = inGeo.firstVertex;
      for (int qd = 1; qd < nvertFill / 2; qd++) {
        int i0 = first + 2 * (qd - 1);
        int i1 = first + 2 * (qd - 1) + 1;
        int i2 = first + 2 * qd + 1;
        int i3 = first + 2 * qd;
        
        if (0 < inGeo.strokes[5 * i0 + 4] || 
            0 < inGeo.strokes[5 * i1 + 4] ||
            0 < inGeo.strokes[5 * i2 + 4]||
            0 < inGeo.strokes[5 * i3 + 4]) {
          strokedCount++;
        }      
      }
      
      if (0 < strokedCount) {
        // Each stroked quad has 4 lines, one for each edge. 
        // These lines are made up of 4 vertices defining the quad. 
        // Each vertex has its own offset representing the stroke weight.
        int nvertLine = strokedCount * 4 * 4;
        tessGeo.addLineVertices(nvertLine);
        
        // Each stroke line has 4 vertices, defining 2 triangles, which
        // require 3 indices to specify their connectivities.
        int nind = strokedCount * 4 * 2 * 3;
        tessGeo.addLineIndices(nind); 
        
        int vcount = tessGeo.firstLineVertex;
        int icount = tessGeo.firstLineIndex;
        for (int qd = 1; qd < nvertFill / 2; qd++) {
          int i0 = first + 2 * (qd - 1);
          int i1 = first + 2 * (qd - 1) + 1;
          int i2 = first + 2 * qd + 1;
          int i3 = first + 2 * qd;     

          if (0 < inGeo.strokes[5 * i0 + 4] || 
              0 < inGeo.strokes[5 * i1 + 4] ||
              0 < inGeo.strokes[5 * i2 + 4]||
              0 < inGeo.strokes[5 * i3 + 4]) {
            addLine(i0, i1, vcount, icount); vcount += 4; icount += 6;
            addLine(i1, i2, vcount, icount); vcount += 4; icount += 6;
            addLine(i2, i3, vcount, icount); vcount += 4; icount += 6;
            addLine(i3, i0, vcount, icount); vcount += 4; icount += 6;
          }
        }
        
      }
    }  
    
    public void tessellatePolygon(boolean solid, boolean closed) {
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

      // Count many how many line segments in the perimeter
      // of this polygon are stroked.      
      int lineCount = 0;
      int lnCount = inGeo.lastVertex - inGeo.firstVertex + 1;
      //int offset = tessGeo.firstFillVertex;
      int first = inGeo.firstVertex;
      if (!closed) {
        lnCount--;
      }
      int contour0 = first;
      for (int ln = 0; ln < lnCount; ln++) {
        int i0 = first + ln;
        int i1 = first + ln + 1;
        if (inGeo.codes[i0] == PShape.BREAK) {
          contour0 = i0;
        }
        if ((i1 == lnCount || inGeo.codes[i1] == PShape.BREAK) && closed) {
          // Draw line with the first vertex of the current contour.
          i0 = contour0;
          i1 = first + ln;
        }
        
        if (inGeo.codes[i1] != PShape.BREAK &&
            (0 < inGeo.strokes[5 * i0 + 4] || 
             0 < inGeo.strokes[5 * i1 + 4])) {
          lineCount++;
        }      
      }
      
      if (0 < lineCount) {
        // Lines are made up of 4 vertices defining the quad. 
        // Each vertex has its own offset representing the stroke weight.
        int nvertLine = lineCount * 4;
        tessGeo.addLineVertices(nvertLine);
        
        // Each stroke line has 4 vertices, defining 2 triangles, which
        // require 3 indices to specify their connectivities.
        int nind = lineCount * 2 * 3;
        tessGeo.addLineIndices(nind);  
        
        int vcount = tessGeo.firstLineVertex;
        int icount = tessGeo.firstLineIndex;
        contour0 = first;
        for (int ln = 0; ln < lnCount; ln++) {
          int i0 = first + ln;
          int i1 = first + ln + 1;
          if (inGeo.codes[i0] == PShape.BREAK) {
            contour0 = i0;
          }
          if ((i1 == lnCount || inGeo.codes[i1] == PShape.BREAK) && closed) {
            // Draw line with the first vertex of the current contour.
            i0 = contour0;
            i1 = first + ln;
          }
          
          if (inGeo.codes[i1] != PShape.BREAK &&
              (0 < inGeo.strokes[5 * i0 + 4] || 
               0 < inGeo.strokes[5 * i1 + 4])) {
            addLine(i0, i1, vcount, icount); vcount += 4; icount += 6;
          }      
        }    
      }  
    }
    
    // Adding the data that defines a quad starting at vertex i0 and
    // ending at i1.
    protected void addLine(int i0, int i1, int vcount, int icount) {
      tessGeo.putLineVertex(inGeo, i0, i1, vcount);
      tessGeo.lineAttributes[4 * vcount + 3] = +inGeo.strokes[5 * i0 + 4];    
      tessGeo.lineIndices[icount++] = vcount;
      
      vcount++;
      tessGeo.putLineVertex(inGeo, i0, i1, vcount);
      tessGeo.lineAttributes[4 * vcount + 3] = -inGeo.strokes[5 * i0 + 4];
      tessGeo.lineIndices[icount++] = vcount;
      
      vcount++;
      tessGeo.putLineVertex(inGeo, i1, i0, vcount);
      tessGeo.lineAttributes[4 * vcount + 3] = -inGeo.strokes[5 * i1 + 4];
      tessGeo.lineIndices[icount++] = vcount;
      
      // Starting a new triangle re-using prev vertices.
      tessGeo.lineIndices[icount++] = vcount;
      tessGeo.lineIndices[icount++] = vcount - 1;
      
      vcount++;
      tessGeo.putLineVertex(inGeo, i1, i0, vcount);      
      tessGeo.lineAttributes[4 * vcount + 3] = +inGeo.strokes[5 * i1 + 4];
      tessGeo.lineIndices[icount++] = vcount;
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
          tessGeo.fillVertexCount++;
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
