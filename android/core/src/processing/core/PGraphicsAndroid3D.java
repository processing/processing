
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

import java.net.URL;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Stack;


// drawPixels is missing...calls to glDrawPixels are commented out
//   setRasterPos() is also commented out

/**
 * OpenGL renderer.
 * 
 */
public class PGraphicsAndroid3D extends PGraphics {
  /** Interface between Processing and OpenGL */
  protected PGL pgl;
  
  /** The PApplet renderer. For the primary surface, pg == this. */
  protected PGraphicsAndroid3D pg;

  // ........................................................  
  
  // VBOs for immediate rendering:  
  
  public int glFillVertexBufferID;
  public int glFillColorBufferID;
  public int glFillNormalBufferID;
  public int glFillTexCoordBufferID;
  public int glFillAmbientBufferID;
  public int glFillSpecularBufferID;
  public int glFillEmissiveBufferID;
  public int glFillShininessBufferID;  
  public int glFillIndexBufferID;  
  protected boolean fillVBOsCreated = false;
  
  public int glLineVertexBufferID;
  public int glLineColorBufferID;
  public int glLineDirWidthBufferID;
  public int glLineIndexBufferID;  
  protected boolean lineVBOsCreated = false;
  
  public int glPointVertexBufferID;
  public int glPointColorBufferID;
  public int glPointSizeBufferID;
  public int glPointIndexBufferID;   
  protected boolean pointVBOsCreated = false;
  
  // ........................................................  
  
  // GL parameters
  
  static protected boolean glParamsRead = false;
  
  /** Extensions used by Processing */
  static public boolean npotTexSupported;
  static public boolean mipmapGeneration;
  static public boolean vboSupported;
  static public boolean fboSupported;
  static public boolean fboMultisampleSupported;
  static public boolean blendEqSupported;
  
  /** Some hardware limits */  
  static public int maxTextureSize;
  static public float maxPointSize;
  static public float maxLineWidth;
  
  /** OpenGL information strings */
  static public String OPENGL_VENDOR;
  static public String OPENGL_RENDERER;
  static public String OPENGL_VERSION;  
  static public String OPENGL_EXTENSIONS;
  
  // ........................................................  
  
  // GL objects:
  
  static protected HashMap<Integer, Boolean> glTextureObjects    = new HashMap<Integer, Boolean>();
  static protected HashMap<Integer, Boolean> glVertexBuffers     = new HashMap<Integer, Boolean>();
  static protected HashMap<Integer, Boolean> glFrameBuffers      = new HashMap<Integer, Boolean>();
  static protected HashMap<Integer, Boolean> glRenderBuffers     = new HashMap<Integer, Boolean>();    
  static protected HashMap<Integer, Boolean> glslPrograms        = new HashMap<Integer, Boolean>();
  static protected HashMap<Integer, Boolean> glslVertexShaders   = new HashMap<Integer, Boolean>();
  static protected HashMap<Integer, Boolean> glslFragmentShaders = new HashMap<Integer, Boolean>();
  
  // ........................................................
  
  // Shaders    
  
  protected static URL defFillShaderVertSimpleURL = PGraphicsAndroid3D.class.getResource("FillShaderVertSimple.glsl");
  protected static URL defFillShaderVertTexURL    = PGraphicsAndroid3D.class.getResource("FillShaderVertTex.glsl");
  protected static URL defFillShaderVertLitURL    = PGraphicsAndroid3D.class.getResource("FillShaderVertLit.glsl");
  protected static URL defFillShaderVertFullURL   = PGraphicsAndroid3D.class.getResource("FillShaderVertFull.glsl");  
  protected static URL defFillShaderFragNoTexURL  = PGraphicsAndroid3D.class.getResource("FillShaderFragNoTex.glsl");
  protected static URL defFillShaderFragTexURL    = PGraphicsAndroid3D.class.getResource("FillShaderFragTex.glsl");  
  protected static URL defLineShaderVertURL       = PGraphicsAndroid3D.class.getResource("LineShaderVert.glsl");
  protected static URL defLineShaderFragURL       = PGraphicsAndroid3D.class.getResource("LineShaderFrag.glsl");
  protected static URL defPointShaderVertURL      = PGraphicsAndroid3D.class.getResource("PointShaderVert.glsl");
  protected static URL defPointShaderFragURL      = PGraphicsAndroid3D.class.getResource("PointShaderFrag.glsl");
  
  protected static FillShaderSimple defFillShaderSimple;
  protected static FillShaderTex defFillShaderTex;
  protected static FillShaderLit defFillShaderLit;
  protected static FillShaderFull defFillShaderFull;
  protected static LineShader defLineShader;
  protected static PointShader defPointShader;
  
  protected FillShaderSimple fillShaderSimple;
  protected FillShaderTex fillShaderTex;
  protected FillShaderLit fillShaderLit;
  protected FillShaderFull fillShaderFull;
  protected LineShader lineShader;
  protected PointShader pointShader;

  // ........................................................
  
  // Tessellator, geometry  
    
  protected InGeometry inGeo;
  protected TessGeometry tessGeo;
  protected int firstTexIndex;
  protected TexCache texCache;
  protected Tessellator tessellator; 
  
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

  // All the matrices required for camera and geometry transformations.
  public PMatrix3D projection;    
  public PMatrix3D camera;
  public PMatrix3D cameraInv;  
  public PMatrix3D modelview;
  public PMatrix3D modelviewInv;
  public PMatrix3D projmodelview;  
  
  // To pass to shaders
  protected float[] glProjection;
  protected float[] glModelview;
  protected float[] glProjmodelview;
  protected float[] glNormal;
    
  protected boolean matricesAllocated = false;
  
  /** 
   * Marks when changes to the size have occurred, so that the camera 
   * will be reset in beginDraw().
   */
  protected boolean sizeChanged;  
  
  /** Modelview matrix stack **/
  protected Stack<PMatrix3D> modelviewStack;  

  /** Inverse modelview matrix stack **/
  protected Stack<PMatrix3D> modelviewInvStack;  
  
  /** Projection matrix stack **/
  protected Stack<PMatrix3D> projectionStack;

  // ........................................................

  // Lights:  
  
  public boolean lights;
  public int lightCount = 0;

  /** Light types */
  public int[] lightType;

  /** Light positions */
  public float[] lightPosition;

  /** Light direction (normalized vector) */
  public float[] lightNormal;

  /**
   * Ambient colors for lights.
   */
  public float[] lightAmbient;  
  
  /**
   * Diffuse colors for lights.
   */
  public float[] lightDiffuse;  
  
  /**
   * Specular colors for lights. Internally these are stored as numbers between
   * 0 and 1.
   */
  public float[] lightSpecular;
    
  /** Light falloff */
  public float[] lightFalloffCoefficients;

  /** Light spot parameters: Cosine of light spot angle 
   * and concentration */
  public float[] lightSpotParameters;

  /** Current specular color for lighting */
  public float[] currentLightSpecular;

  /** Current light falloff */
  public float currentLightFalloffConstant;
  public float currentLightFalloffLinear;
  public float currentLightFalloffQuadratic;

  protected boolean lightsAllocated = false;   
  
  // ........................................................
  
  // Blending:
  
  protected int blendMode;  
  
  // ........................................................
  
  // Clipping  
  
  protected boolean clip = false;  
  
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
  
  // Utility variables:
  
  /** True if we are inside a beginDraw()/endDraw() block. */
  protected boolean drawing = false;  
  
  /** Used to detect the occurrence of a frame resize event. */
  protected boolean resized = false;
  
  /** Stores previous viewport dimensions. */
  protected int[] savedViewport = {0, 0, 0, 0};
  protected int[] viewport = {0, 0, 0, 0};
  
  protected boolean openContour = false;
  protected boolean breakShape = false;
  protected boolean defaultEdges = false;
  protected PImage textureImage0;    
  
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
  
  // Constants    
  
  protected static int flushMode = FLUSH_WHEN_FULL;  
  protected static final int MIN_ARRAYCOPY_SIZE = 2;  
  protected int vboMode = PGL.GL_STATIC_DRAW;
    
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
  
  /**
   * Set to true if the host system is big endian (PowerPC, MIPS, SPARC), false
   * if little endian (x86 Intel for Mac or PC).
   */
  static public boolean BIG_ENDIAN = ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN;  
  
  //////////////////////////////////////////////////////////////
  
  
  // INIT/ALLOCATE/FINISH
  
  public PGraphicsAndroid3D() {
    pgl = new PGL(this);
    
    tessellator = new Tessellator();
    
    inGeo = newInGeometry(IMMEDIATE);
    tessGeo = newTessGeometry(IMMEDIATE);
    texCache = newTexCache();
    
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
      projection = new PMatrix3D();
      camera = new PMatrix3D();
      cameraInv = new PMatrix3D();
      modelview = new PMatrix3D();
      modelviewInv = new PMatrix3D();
      projmodelview = new PMatrix3D();
      matricesAllocated = true;
    }

    if (!lightsAllocated) {
      lightType = new int[PGL.MAX_LIGHTS];
      lightPosition = new float[4 * PGL.MAX_LIGHTS];
      lightNormal = new float[3 * PGL.MAX_LIGHTS];
      lightAmbient = new float[3 * PGL.MAX_LIGHTS];
      lightDiffuse = new float[3 * PGL.MAX_LIGHTS];
      lightSpecular = new float[3 * PGL.MAX_LIGHTS];
      lightFalloffCoefficients = new float[3 * PGL.MAX_LIGHTS];
      lightSpotParameters = new float[2 * PGL.MAX_LIGHTS];
      currentLightSpecular = new float[3];
      lightsAllocated = true;
    }
    
    if (primarySurface) {
      // Allocation of the main renderer, which mainly involves initializing OpenGL.
//      if (context == null) {
//        initPrimary();      
//      } else {
//        reapplySettings();
//      }
      if (pgl.initialized) {
        reapplySettings();
      }      
    } else {      
      // Allocation of an offscreen renderer.
//      if (context == null) {
//        initOffscreen();
//      } else {
//        // Updating OpenGL context associated to this offscreen
//        // surface, to take into account a context recreation situation.
//        updateOffscreenContext();
//        reapplySettings();
//      }
      if (pgl.initialized) {
        updateOffscreenContext();
        reapplySettings();        
      }
    }    
  }

  
  public void dispose() { // PGraphics    
    super.dispose();
    pgl.detainContext();
    deleteFinalizedGLResources();
    pgl.releaseContext();
    PGL.shutdown();    
  }
  

  // Only for debugging purposes.
  public void setFlushMode(int mode) {
    PGraphicsAndroid3D.flushMode = mode;    
  }
  
  
  //////////////////////////////////////////////////////////////

  // RESOURCE HANDLING
  
  // Texture Objects -------------------------------------------
  
  protected int createTextureObject() {
    deleteFinalizedTextureObjects();
    
    int[] temp = new int[1];
    pgl.glGenTextures(1, temp, 0);
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
      pgl.glDeleteTextures(1, temp, 0);
      glTextureObjects.remove(id); 
    }
  }  
  
  protected void deleteAllTextureObjects() {
    for (Integer id : glTextureObjects.keySet()) {
      int[] temp = { id.intValue() };
      pgl.glDeleteTextures(1, temp, 0);
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
        pgl.glDeleteTextures(1, temp, 0);
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
    pgl.glGenBuffers(1, temp, 0);
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
      pgl.glDeleteBuffers(1, temp, 0);
      glVertexBuffers.remove(id); 
    }
  }
  
  protected void deleteAllVertexBufferObjects() {
    for (Integer id : glVertexBuffers.keySet()) {
      int[] temp = { id.intValue() };
      pgl.glDeleteBuffers(1, temp, 0);
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
        pgl.glDeleteBuffers(1, temp, 0);
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
    pgl.glGenFramebuffers(1, temp, 0);
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
      pgl.glDeleteFramebuffers(1, temp, 0);
      glFrameBuffers.remove(id); 
    }
  }  
  
  protected void deleteAllFrameBufferObjects() {
    for (Integer id : glFrameBuffers.keySet()) {
      int[] temp = { id.intValue() };
      pgl.glDeleteFramebuffers(1, temp, 0);
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
        pgl.glDeleteFramebuffers(1, temp, 0);
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
    pgl.glDeleteRenderbuffers(1, temp, 0);
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
      pgl.glGenRenderbuffers(1, temp, 0);
      glRenderBuffers.remove(id); 
    }
  }   
  
  protected void deleteAllRenderBufferObjects() {
    for (Integer id : glRenderBuffers.keySet()) {
      int[] temp = { id.intValue() };
      pgl.glDeleteRenderbuffers(1, temp, 0);
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
        pgl.glDeleteRenderbuffers(1, temp, 0);
      }
    }
    
    for (Integer id : finalized) {
      glRenderBuffers.remove(id);  
    }
  }
  
  // GLSL Program Objects -----------------------------------------------
  
  protected int createGLSLProgramObject() {
    
    pg.report("before delete");
    deleteFinalizedGLSLProgramObjects();
        
    int id = pgl.glCreateProgram();
    
    if (glslPrograms.containsKey(id)) {
      showWarning("Adding same glsl program twice");
    } else {    
      glslPrograms.put(id, false);
    }
    
    return id;
  }
  
  protected void deleteGLSLProgramObject(int id) {
    if (glslPrograms.containsKey(id)) {
      pgl.glDeleteProgram(id);
      glslPrograms.remove(id); 
    }
  }     
  
  protected void deleteAllGLSLProgramObjects() {
    for (Integer id : glslPrograms.keySet()) {
      pgl.glDeleteProgram(id);
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
        pgl.glDeleteProgram(id);
      }
    }
    
    for (Integer id : finalized) {
      glslPrograms.remove(id);  
    }
  }

  // GLSL Vertex Shader Objects -----------------------------------------------
  
  protected int createGLSLVertShaderObject() {
    deleteFinalizedGLSLVertShaderObjects();
    
    int id = pgl.glCreateShader(PGL.GL_VERTEX_SHADER);
    
    if (glslVertexShaders.containsKey(id)) {
      showWarning("Adding same glsl vertex shader twice");
    } else {    
      glslVertexShaders.put(id, false);
    }
    
    return id;
  }
  
  protected void deleteGLSLVertShaderObject(int id) {
    if (glslVertexShaders.containsKey(id)) {
      pgl.glDeleteShader(id);
      glslVertexShaders.remove(id); 
    }
  }    
  
  protected void deleteAllGLSLVertShaderObjects() {
    for (Integer id : glslVertexShaders.keySet()) {
      pgl.glDeleteShader(id);
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
        pgl.glDeleteShader(id);
      }
    }
    
    for (Integer id : finalized) {
      glslVertexShaders.remove(id);  
    }
  }
  
  // GLSL Fragment Shader Objects -----------------------------------------------
    
  
  protected int createGLSLFragShaderObject() {
    deleteFinalizedGLSLFragShaderObjects();
    
    int id = pgl.glCreateShader(PGL.GL_FRAGMENT_SHADER);        
    
    if (glslFragmentShaders.containsKey(id)) {
      showWarning("Adding same glsl fragment shader twice");
    } else {    
      glslFragmentShaders.put(id, false);
    }
    
    return id;
  }
  
  protected void deleteGLSLFragShaderObject(int id) {
    if (glslFragmentShaders.containsKey(id)) {
      pgl.glDeleteShader(id);
      glslFragmentShaders.remove(id); 
    }
  }     
  
  protected void deleteAllGLSLFragShaderObjects() {
    for (Integer id : glslFragmentShaders.keySet()) {
      pgl.glDeleteShader(id);
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
        pgl.glDeleteShader(id);
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
  
//  public GLContext getContext() {
//    return context;
//  }
//  public GLCapabilities getCapabilities() {
//    return capabilities;
//  }  
//  public GLProfile getProfile() {
//    return profile;
//  }
//  public GLDrawable getDrawable() {
//    return drawable;
//  }
  
  

  protected void releaseResources() {
    // First, releasing the resources used by
    // the renderer itself.
    if (texture != null) {
      texture.release();
      texture = null;
    }
        
    if (defFillShaderSimple != null) {
      defFillShaderSimple.release();
      defFillShaderSimple = null;
    }

    if (defFillShaderLit != null) {
      defFillShaderLit.release();
      defFillShaderLit = null;
    }    

    if (defFillShaderTex != null) {
      defFillShaderTex.release();
      defFillShaderTex = null;
    }        

    if (defFillShaderFull != null) {
      defFillShaderFull.release();
      defFillShaderFull = null;
    }            
    
    if (defLineShader != null) {
      defLineShader.release();
      defLineShader = null;
    }
    
    if (defPointShader != null) {
      defPointShader.release();
      defPointShader = null;
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
   * Destroys current OpenGL context and creates a new one, making sure that all
   * the current OpenGL objects remain valid afterward.
   */  
  public void restartContext() {
    releaseResources();    
    
    pgl.releaseContext();
    pgl.destroyContext();
    restartSurface();    
    pgl.detainContext();      
    updatePGL();    
  }  

  protected void createFillBuffers() {
    int sizef = PGL.MAX_TESS_VERTICES * PGL.SIZEOF_FLOAT;
    int sizei = PGL.MAX_TESS_VERTICES * PGL.SIZEOF_INT;
    int sizex = PGL.MAX_TESS_INDICES * PGL.SIZEOF_INDEX;
    
    glFillVertexBufferID = createVertexBufferObject();    
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glFillVertexBufferID);
    pgl.glBufferData(PGL.GL_ARRAY_BUFFER, 3 * sizef, null, vboMode);
                
    glFillColorBufferID = createVertexBufferObject();
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glFillColorBufferID);
    pgl.glBufferData(PGL.GL_ARRAY_BUFFER, sizei, null, vboMode);
    
    glFillNormalBufferID = createVertexBufferObject();
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glFillNormalBufferID);
    pgl.glBufferData(PGL.GL_ARRAY_BUFFER, 3 * sizef, null, vboMode);    
    
    glFillTexCoordBufferID = createVertexBufferObject();
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glFillTexCoordBufferID);
    pgl.glBufferData(PGL.GL_ARRAY_BUFFER, 2 * sizef, null, vboMode);        
    
    glFillAmbientBufferID = pg.createVertexBufferObject();  
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glFillAmbientBufferID);
    pgl.glBufferData(PGL.GL_ARRAY_BUFFER, sizei, null, vboMode);
    
    glFillSpecularBufferID = pg.createVertexBufferObject();
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glFillSpecularBufferID);
    pgl.glBufferData(PGL.GL_ARRAY_BUFFER, sizei, null, vboMode);    
    
    glFillEmissiveBufferID = pg.createVertexBufferObject();
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glFillEmissiveBufferID);
    pgl.glBufferData(PGL.GL_ARRAY_BUFFER, sizei, null, vboMode);
    
    glFillShininessBufferID = pg.createVertexBufferObject();
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glFillShininessBufferID);
    pgl.glBufferData(PGL.GL_ARRAY_BUFFER, sizef, null, vboMode);
    
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, 0);
    
    glFillIndexBufferID = createVertexBufferObject();    
    pgl.glBindBuffer(PGL.GL_ELEMENT_ARRAY_BUFFER, glFillIndexBufferID);
    pgl.glBufferData(PGL.GL_ELEMENT_ARRAY_BUFFER, sizex, null, vboMode);

    pgl.glBindBuffer(PGL.GL_ELEMENT_ARRAY_BUFFER, 0);
  }
  
  protected void updateFillBuffers(boolean lit, boolean tex) {
    int size = tessGeo.fillVertexCount;
    int sizef = size * PGL.SIZEOF_FLOAT;
    int sizei = size * PGL.SIZEOF_INT;
    
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glFillVertexBufferID);
    pgl.glBufferData(PGL.GL_ARRAY_BUFFER, 3 * sizef, FloatBuffer.wrap(tessGeo.fillVertices, 0, 3 * size), vboMode);
    
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glFillColorBufferID);
    pgl.glBufferData(PGL.GL_ARRAY_BUFFER, sizei, IntBuffer.wrap(tessGeo.fillColors, 0, size), vboMode);    
    
    if (lit) {
      pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glFillNormalBufferID);
      pgl.glBufferData(PGL.GL_ARRAY_BUFFER, 3 * sizef, FloatBuffer.wrap(tessGeo.fillNormals, 0, 3 * size), vboMode);     
      
      pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glFillAmbientBufferID);
      pgl.glBufferData(PGL.GL_ARRAY_BUFFER, sizei, IntBuffer.wrap(tessGeo.fillAmbient, 0, size), vboMode);

      pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glFillSpecularBufferID);
      pgl.glBufferData(PGL.GL_ARRAY_BUFFER, sizei, IntBuffer.wrap(tessGeo.fillSpecular, 0, size), vboMode);

      pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glFillEmissiveBufferID);
      pgl.glBufferData(PGL.GL_ARRAY_BUFFER, sizei, IntBuffer.wrap(tessGeo.fillEmissive, 0, size), vboMode);
     
      
      pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glFillShininessBufferID);
      pgl.glBufferData(PGL.GL_ARRAY_BUFFER, sizef, FloatBuffer.wrap(tessGeo.fillShininess, 0, size), vboMode);
    }
    
    if (tex) {
      pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glFillTexCoordBufferID);
      pgl.glBufferData(PGL.GL_ARRAY_BUFFER, 2 * sizef, FloatBuffer.wrap(tessGeo.fillTexcoords, 0, 2 * size), vboMode);      
    }
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
    
    deleteVertexBufferObject(glFillAmbientBufferID);
    glFillAmbientBufferID = 0;
    
    deleteVertexBufferObject(glFillSpecularBufferID);
    glFillSpecularBufferID = 0;

    deleteVertexBufferObject(glFillEmissiveBufferID);
    glFillEmissiveBufferID = 0;    
    
    deleteVertexBufferObject(glFillShininessBufferID);
    glFillShininessBufferID = 0;    
    
    deleteVertexBufferObject(glFillIndexBufferID);
    glFillIndexBufferID = 0;    
  }

  protected void createLineBuffers() {
    int sizef = PGL.MAX_TESS_VERTICES * PGL.SIZEOF_FLOAT;
    int sizex = PGL.MAX_TESS_INDICES * PGL.SIZEOF_INDEX;
    int sizei = PGL.MAX_TESS_INDICES * PGL.SIZEOF_INT;    
    
    glLineVertexBufferID = createVertexBufferObject();
    
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glLineVertexBufferID);
    pgl.glBufferData(PGL.GL_ARRAY_BUFFER, 3 * sizef, null, vboMode);
    
    glLineColorBufferID = createVertexBufferObject();
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glLineColorBufferID);
    pgl.glBufferData(PGL.GL_ARRAY_BUFFER, sizei, null, vboMode);
    
    glLineDirWidthBufferID = createVertexBufferObject();
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glLineDirWidthBufferID);
    pgl.glBufferData(PGL.GL_ARRAY_BUFFER, 4 * sizef, null, vboMode);
    
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, 0);
    
    glLineIndexBufferID = createVertexBufferObject();    
    pgl.glBindBuffer(PGL.GL_ELEMENT_ARRAY_BUFFER, glLineIndexBufferID);
    pgl.glBufferData(PGL.GL_ELEMENT_ARRAY_BUFFER, sizex, null, vboMode);
    
    pgl.glBindBuffer(PGL.GL_ELEMENT_ARRAY_BUFFER, 0);
  }  
  
  protected void updateLineBuffers() {
    int size = tessGeo.lineVertexCount;
    int sizef = size * PGL.SIZEOF_FLOAT;
    int sizei = size * PGL.SIZEOF_INT;

    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glLineVertexBufferID);
    pgl.glBufferData(PGL.GL_ARRAY_BUFFER, 3 * sizef, FloatBuffer.wrap(tessGeo.lineVertices, 0, 3 * size), vboMode);

    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glLineColorBufferID);
    pgl.glBufferData(PGL.GL_ARRAY_BUFFER, sizei, IntBuffer.wrap(tessGeo.lineColors, 0, size), vboMode);
    
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glLineDirWidthBufferID);
    pgl.glBufferData(PGL.GL_ARRAY_BUFFER, 4 * sizef, FloatBuffer.wrap(tessGeo.lineDirWidths, 0, 4 * size), vboMode);    
  }
  
  protected void releaseLineBuffers() {
    deleteVertexBufferObject(glLineVertexBufferID);
    glLineVertexBufferID = 0;
    
    deleteVertexBufferObject(glLineColorBufferID);
    glLineColorBufferID = 0;

    deleteVertexBufferObject(glLineDirWidthBufferID);
    glLineDirWidthBufferID = 0;
    
    deleteVertexBufferObject(glLineIndexBufferID);
    glLineIndexBufferID = 0;    
  }

  protected void createPointBuffers() {
    int sizef = PGL.MAX_TESS_VERTICES * PGL.SIZEOF_FLOAT;
    int sizex = PGL.MAX_TESS_INDICES * PGL.SIZEOF_INDEX;
    int sizei = PGL.MAX_TESS_INDICES * PGL.SIZEOF_INT;
    
    glPointVertexBufferID = createVertexBufferObject();    
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glPointVertexBufferID);
    pgl.glBufferData(PGL.GL_ARRAY_BUFFER, 3 * sizef, null, vboMode);
    
    glPointColorBufferID = createVertexBufferObject();
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glPointColorBufferID);
    pgl.glBufferData(PGL.GL_ARRAY_BUFFER, sizei, null, vboMode);    
    
    glPointSizeBufferID = createVertexBufferObject();
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glPointSizeBufferID);
    pgl.glBufferData(PGL.GL_ARRAY_BUFFER, 2 * sizef, null, vboMode);    
    
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, 0);
    
    glPointIndexBufferID = createVertexBufferObject();    
    pgl.glBindBuffer(PGL.GL_ELEMENT_ARRAY_BUFFER, glPointIndexBufferID);
    pgl.glBufferData(PGL.GL_ELEMENT_ARRAY_BUFFER, sizex, null, vboMode);
    
    pgl.glBindBuffer(PGL.GL_ELEMENT_ARRAY_BUFFER, 0);
  }  
  
  protected void updatePointBuffers() {
    int size = tessGeo.pointVertexCount;
    int sizef = size * PGL.SIZEOF_FLOAT;
    int sizei = size * PGL.SIZEOF_INT;

    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glPointVertexBufferID);
    pgl.glBufferData(PGL.GL_ARRAY_BUFFER, 3 * sizef, FloatBuffer.wrap(tessGeo.pointVertices, 0, 3 * size), vboMode);
    
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glPointColorBufferID);
    pgl.glBufferData(PGL.GL_ARRAY_BUFFER, sizei, IntBuffer.wrap(tessGeo.pointColors, 0, size), vboMode);

    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glPointSizeBufferID);    
    pgl.glBufferData(PGL.GL_ARRAY_BUFFER, 2 * sizef, FloatBuffer.wrap(tessGeo.pointSizes, 0, 2 * size), vboMode);   
  }
  
  protected void releasePointBuffers() {
    deleteVertexBufferObject(glPointVertexBufferID);
    glPointVertexBufferID = 0;
      
    deleteVertexBufferObject(glPointColorBufferID);
    glPointColorBufferID = 0; 
      
    deleteVertexBufferObject(glPointSizeBufferID);
    glPointSizeBufferID = 0; 
      
    deleteVertexBufferObject(glPointIndexBufferID);  
    glPointIndexBufferID = 0;
  }
  
    
  /**
   * OpenGL cannot draw until a proper native peer is available, so this
   * returns the value of PApplet.isDisplayable() (inherited from Component).
   */
  public boolean canDraw() {
    return pgl.canDraw();
  }

  
  public void requestDraw() {    
    pgl.requestDraw();
  }  
  
  
  public void beginDraw() {
    if (drawing) {
      showWarning("P3D: Already called beginDraw().");
      return;
    }    
    
    if (primarySurface) {
      if (!pgl.initialized) {
        initPrimary();
      } 
      
      if (!pgl.initOnscreenDraw()) {
        return;
      }
      pgl.detainContext();      
    } else {
      if (!pgl.initialized) {
        initOffscreen();
      }     
      
      pushFramebuffer();
      if (offscreenMultisample) {
        setFramebuffer(offscreenFramebufferMultisample);   
        pgl.setDrawBuffer(0);
      } else {
        setFramebuffer(offscreenFramebuffer);
      } 
    }
      
    updatePGL();
    
    if (!glParamsRead) {
      getGLParameters();  
    }
    
    if (!settingsInited) {
      defaultSettings();
    }    
    
    // We are ready to go!
    
    report("top beginDraw()");    
    
    if (!primarySurface) {
      pg.saveGLState();
            
      // Disabling all lights, so the offscreen renderer can set completely
      // new light configuration (otherwise some light configuration from the 
      // primary renderer might stay).
      //pg.disableLights();
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
      pgl.glDisable(PGL.GL_DEPTH_TEST);
    } else {
      pgl.glEnable(PGL.GL_DEPTH_TEST);
    }
    // use <= since that's what processing.core does
    pgl.setDepthFunc(PGL.GL_LEQUAL);
    
    if (hints[DISABLE_DEPTH_MASK]) {
      pgl.glDepthMask(false);
    } else {
      pgl.glDepthMask(true);
    }

    if (hints[ENABLE_ACCURATE_2D]) {
      flushMode = FLUSH_CONTINUOUSLY;      
    } else {
      flushMode = FLUSH_WHEN_FULL;
    }
    
    // setup opengl viewport.    
    pgl.glGetIntegerv(PGL.GL_VIEWPORT, savedViewport, 0);    
    viewport[0] = 0; viewport[1] = 0; viewport[2] = width; viewport[3] = height;
    pgl.glViewport(viewport[0], viewport[1], viewport[2], viewport[3]);
    if (resized) {
      // To avoid having garbage in the screen after a resize,
      // in the case background is not called in draw().
      background(0);
      if (texture != null) {
        // The screen texture should be deleted because it 
        // corresponds to the old window size.
        this.removeCache(pg);
        this.removeParams(pg);
        texture = null;
        loadTexture();
      }      
      resized = false;            
    }
    
    if (sizeChanged) {
      // defaults to perspective, if the user has setup up their
      // own projection, they'll need to fix it after resize anyway.
      // this helps the people who haven't set up their own projection.
      perspective();
      
      // set up the default camera and initializes modelview matrix.
      camera();
      
      // clear the flag
      sizeChanged = false;
    } else {
      // The camera and projection matrices, saved when calling camera() and frustrum()
      // are set as the current modelview and projection matrices. This is done to
      // remove any additional modelview transformation (and less likely, projection
      // transformations) applied by the user after setting the camera and/or projection      
      modelview.set(camera);
      calcProjmodelview();
    }
      
    noLights();
    lightFalloff(1, 0, 0);
    lightSpecular(0, 0, 0);

    // because y is flipped
    pgl.setFrontFace(PGL.GL_CW);
    
    // Processing uses only one texture unit.
    pgl.glActiveTexture(PGL.GL_TEXTURE0);
    
    // The current normal vector is set to be parallel to the Z axis.
    normalX = normalY = normalZ = 0;
    
    // Clear depth and stencil buffers.
    pgl.glClearColor(0, 0, 0, 0);
    pgl.glClear(PGL.GL_DEPTH_BUFFER_BIT | PGL.GL_STENCIL_BUFFER_BIT);
    
    if (primarySurface) {
      pgl.beginOnscreenDraw();  
    } else {
      pgl.beginOffscreenDraw();  
    }
    
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
    pgl.glViewport(savedViewport[0], savedViewport[1], savedViewport[2], savedViewport[3]);
    
    if (primarySurface) {
      // glFlush should be called only once, since it is an expensive
      // operation. Thus, only the main renderer (the primary surface)
      // should call it at the end of draw, and none of the offscreen 
      // renderers...
      pgl.endOnscreenDraw();
      pgl.glFlush();                  
      pgl.releaseContext();
    } else {
      if (offscreenMultisample) {
        offscreenFramebufferMultisample.copy(offscreenFramebuffer);       
      }
      popFramebuffer();
      
      pgl.endOffscreenDraw();
      
      pg.restoreGLState();
    }    

    drawing = false;    
    
    report("bot endDraw()");    
  }

  
  public PGL beginPGL() {
    saveGLState();
    return pgl;
  }

  
  public void endGL() {
    restoreGLState();
  }
  
  
  public void updatePGL() {
    if (primarySurface) {
      pgl.updatePrimary();  
    } else {
      pgl.updateOffscreen(pg.pgl);
    }
  }
  
  
  protected void saveGLState() {
  }
  
  
  protected void restoreGLState() {        
    // Restoring viewport.
    pgl.glViewport(viewport[0], viewport[1], viewport[2], viewport[3]);
    
    // Restoring hints.
    if (hints[DISABLE_DEPTH_TEST]) {
      pgl.glDisable(PGL.GL_DEPTH_TEST);
      pgl.glClearColor(0, 0, 0, 0);
      pgl.glClear(PGL.GL_DEPTH_BUFFER_BIT);
    } else {
      pgl.glEnable(PGL.GL_DEPTH_TEST);
    }
    
    if (hints[DISABLE_DEPTH_MASK]) {
      pgl.glDepthMask(false);
    } else {
      pgl.glDepthMask(true);
    }
        
    // Restoring blending.
    blendMode(blendMode);
    
    // Some things the user might have changed from OpenGL, 
    // but we want to make sure they return to the Processing
    // defaults (plus these cannot be changed through the API
    // so they should remain constant anyways):   
    pgl.setFrontFace(PGL.GL_CW);
    pgl.setDepthFunc(PGL.GL_LEQUAL);
  }  

  
  // Utility function to get ready OpenGL for a specific
  // operation, such as grabbing the contents of the color
  // buffer.
  protected void beginGLOp() {
    pgl.detainContext();
    updatePGL();
  }

  
  // Pairs-up with beginGLOp().
  protected void endGLOp() {
    pgl.releaseContext();
  }
    
  protected void updateGLProjection() {
    if (glProjection == null) {
      glProjection = new float[16];
    }
    
    glProjection[0] = projection.m00;
    glProjection[1] = projection.m10;
    glProjection[2] = projection.m20;
    glProjection[3] = projection.m30;

    glProjection[4] = projection.m01;
    glProjection[5] = projection.m11;
    glProjection[6] = projection.m21;
    glProjection[7] = projection.m31;

    glProjection[8] = projection.m02;
    glProjection[9] = projection.m12;
    glProjection[10] = projection.m22;
    glProjection[11] = projection.m32;

    glProjection[12] = projection.m03;
    glProjection[13] = projection.m13;
    glProjection[14] = projection.m23;
    glProjection[15] = projection.m33;
  }

  protected void updateGLModelview() {
    if (glModelview == null) {
      glModelview = new float[16];
    }
    
    glModelview[0] = modelview.m00;
    glModelview[1] = modelview.m10;
    glModelview[2] = modelview.m20;
    glModelview[3] = modelview.m30;

    glModelview[4] = modelview.m01;
    glModelview[5] = modelview.m11;
    glModelview[6] = modelview.m21;
    glModelview[7] = modelview.m31;

    glModelview[8] = modelview.m02;
    glModelview[9] = modelview.m12;
    glModelview[10] = modelview.m22;
    glModelview[11] = modelview.m32;

    glModelview[12] = modelview.m03;
    glModelview[13] = modelview.m13;
    glModelview[14] = modelview.m23;
    glModelview[15] = modelview.m33;    
  }  
  
  protected void calcProjmodelview() {
    projmodelview.set(projection);
    projmodelview.apply(modelview);    
  }
    
  protected void updateGLProjmodelview() {
    if (glProjmodelview == null) {
      glProjmodelview = new float[16];
    }
    
    glProjmodelview[0] = projmodelview.m00;
    glProjmodelview[1] = projmodelview.m10;
    glProjmodelview[2] = projmodelview.m20;
    glProjmodelview[3] = projmodelview.m30;

    glProjmodelview[4] = projmodelview.m01;
    glProjmodelview[5] = projmodelview.m11;
    glProjmodelview[6] = projmodelview.m21;
    glProjmodelview[7] = projmodelview.m31;

    glProjmodelview[8] = projmodelview.m02;
    glProjmodelview[9] = projmodelview.m12;
    glProjmodelview[10] = projmodelview.m22;
    glProjmodelview[11] = projmodelview.m32;

    glProjmodelview[12] = projmodelview.m03;
    glProjmodelview[13] = projmodelview.m13;
    glProjmodelview[14] = projmodelview.m23;
    glProjmodelview[15] = projmodelview.m33;
  }
  
  protected void updateGLNormal() {
    if (glNormal == null) {
      glNormal = new float[9];
    }
    
    // The normal matrix is the transpose of the inverse of the
    // modelview:
    glNormal[0] = modelviewInv.m00; 
    glNormal[1] = modelviewInv.m01; 
    glNormal[2] = modelviewInv.m02;
    
    glNormal[3] = modelviewInv.m10; 
    glNormal[4] = modelviewInv.m11; 
    glNormal[5] = modelviewInv.m12;
    
    glNormal[6] = modelviewInv.m20; 
    glNormal[7] = modelviewInv.m21; 
    glNormal[8] = modelviewInv.m22;     
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
    if (modelviewInvStack == null) {
      modelviewInvStack = new Stack<PMatrix3D>();
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
    boolean oldValue = hints[PApplet.abs(which)];
    super.hint(which);
    boolean newValue = hints[PApplet.abs(which)];

    if (oldValue == newValue) {
      return;
    }
    
    if (which == DISABLE_DEPTH_TEST) {
      flush();
      pgl.glDisable(PGL.GL_DEPTH_TEST);
      pgl.glClearColor(0, 0, 0, 0);
      pgl.glClear(PGL.GL_DEPTH_BUFFER_BIT);      

    } else if (which == ENABLE_DEPTH_TEST) {
      flush();
      pgl.glEnable(PGL.GL_DEPTH_TEST);

    } else if (which == DISABLE_DEPTH_MASK) {
      flush();
      pgl.glDepthMask(false);

    } else if (which == ENABLE_DEPTH_MASK) {
      flush();
      pgl.glDepthMask(true);
      
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
      
    } else if (which == ENABLE_PERSPECTIVE_CORRECTED_LINES) {
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
      
      if (flushMode == FLUSH_WHEN_FULL && tessGeo.isOverflow()) {
        PGraphics.showWarning("P3D: tessellated arrays are overflowing");
      }
      
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
    if (inGeo.isFull()) {
      PGraphics.showWarning("P3D: Too many vertices, try creating smaller shapes");
      return;
    }
    
    boolean textured = textureImage != null;
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
            
    if (textured && textureMode == IMAGE) {
      u /= textureImage.width;
      v /= textureImage.height;

      PTexture tex = queryTexture(textureImage);
      if (tex != null && tex.isFlippedY()) {
        v = 1 - v;
      }
    }
    
    inGeo.addVertex(x, y, z, 
                 fcolor, 
                 normalX, normalY, normalZ,
                 u, v, 
                 scolor, sweight,
                 ambientColor, specularColor, emissiveColor, shininess,
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
    pgl.glEnable(PGL.GL_SCISSOR_TEST);
    
    float h = y2 - y1;
    pgl.glScissor((int)x1, (int)(height - y1 - h), (int)(x2 - x1), (int)h);
    
    clip = true;
  }

  
  public void noClip() {
    if (clip) {
      flush();
      pgl.glDisable(PGL.GL_SCISSOR_TEST);
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
    
    setFirstTexIndex(tessGeo.fillIndexCount);
    
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
    
    setLastTexIndex(tessGeo.lastFillIndex);
  }
  
  protected void setFirstTexIndex(int first) {
    firstTexIndex = first;
  }
  
  protected void setLastTexIndex(int last) {            
    if (textureImage0 != textureImage || texCache.count == 0) {
      texCache.addTexture(textureImage, firstTexIndex, last);
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
        pushMatrix();
        resetMatrix();
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
        popMatrix();
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
    updatePointBuffers();
    
    PointShader shader = getPointShader();
    shader.start();    
    shader.setVertexAttribute(glPointVertexBufferID, 3, PGL.GL_FLOAT, 0, 0);        
    shader.setColorAttribute(glPointColorBufferID, 4, PGL.GL_UNSIGNED_BYTE, 0, 0);    
    shader.setSizeAttribute(glPointSizeBufferID, 2, PGL.GL_FLOAT, 0, 0);
    
    int size = tessGeo.pointIndexCount;
    int sizex = size * PGL.SIZEOF_INDEX;
    pgl.glBindBuffer(PGL.GL_ELEMENT_ARRAY_BUFFER, glPointIndexBufferID);
    pgl.glBufferData(PGL.GL_ELEMENT_ARRAY_BUFFER, sizex, ShortBuffer.wrap(tessGeo.pointIndices, 0, size), vboMode);
    pgl.glDrawElements(PGL.GL_TRIANGLES, size, PGL.INDEX_TYPE, 0);        
    pgl.glBindBuffer(PGL.GL_ELEMENT_ARRAY_BUFFER, 0);
    
    shader.stop();
  }  
    
  protected void renderLines() {
    if (!lineVBOsCreated) {
      createLineBuffers();
      lineVBOsCreated = true;
    }
    updateLineBuffers();
    
    LineShader shader = getLineShader();
    shader.start();    
    shader.setVertexAttribute(glLineVertexBufferID, 3, PGL.GL_FLOAT, 0, 0);        
    shader.setColorAttribute(glLineColorBufferID, 4, PGL.GL_UNSIGNED_BYTE, 0, 0);    
    shader.setDirWidthAttribute(glLineDirWidthBufferID, 4, PGL.GL_FLOAT, 0, 0);
    
    int size = tessGeo.lineIndexCount;
    int sizex = size * PGL.SIZEOF_INDEX;
    pgl.glBindBuffer(PGL.GL_ELEMENT_ARRAY_BUFFER, glLineIndexBufferID);
    pgl.glBufferData(PGL.GL_ELEMENT_ARRAY_BUFFER, sizex, ShortBuffer.wrap(tessGeo.lineIndices, 0, size), vboMode);
    pgl.glDrawElements(PGL.GL_TRIANGLES, size, PGL.INDEX_TYPE, 0);
    pgl.glBindBuffer(PGL.GL_ELEMENT_ARRAY_BUFFER, 0);
    
    shader.stop();
  }  
  
  
  protected void renderFill() {
    if (!fillVBOsCreated) {
      createFillBuffers();
      fillVBOsCreated = true;
    }        
    updateFillBuffers(lights, texCache.hasTexture);

    pgl.glBindBuffer(PGL.GL_ELEMENT_ARRAY_BUFFER, glFillIndexBufferID);
    pgl.glBufferData(PGL.GL_ELEMENT_ARRAY_BUFFER, tessGeo.fillIndexCount * PGL.SIZEOF_INDEX, 
                     ShortBuffer.wrap(tessGeo.fillIndices, 0, tessGeo.fillIndexCount), vboMode);
    
    texCache.beginRender();    
    for (int i = 0; i < texCache.count; i++) {
      PTexture tex = texCache.getTexture(i);      
          
      FillShader shader = getFillShader(lights, tex != null);      
      shader.start();
      
      shader.setVertexAttribute(glFillVertexBufferID, 3, PGL.GL_FLOAT, 0, 0);        
      shader.setColorAttribute(glFillColorBufferID, 4, PGL.GL_UNSIGNED_BYTE, 0, 0);    
      
      if (lights) {
        shader.setNormalAttribute(glFillNormalBufferID, 3, PGL.GL_FLOAT, 0, 0);
        shader.setAmbientAttribute(glFillAmbientBufferID, 4, PGL.GL_UNSIGNED_BYTE, 0, 0);
        shader.setSpecularAttribute(glFillSpecularBufferID, 4, PGL.GL_UNSIGNED_BYTE, 0, 0);
        shader.setEmissiveAttribute(glFillEmissiveBufferID, 4, PGL.GL_UNSIGNED_BYTE, 0, 0);      
        shader.setShininessAttribute(glFillShininessBufferID, 1, PGL.GL_FLOAT, 0, 0);
      }
      
      if (tex != null) {
        shader.setTexCoordAttribute(glFillTexCoordBufferID, 2, PGL.GL_FLOAT, 0, 0);     
      }
      
      int offset = texCache.firstIndex[i];
      int size = texCache.lastIndex[i] - texCache.firstIndex[i] + 1;
      pgl.glDrawElements(PGL.GL_TRIANGLES, size, PGL.INDEX_TYPE, offset * PGL.SIZEOF_INDEX);
      
      shader.stop();
    }  
    texCache.endRender();    
    pgl.glBindBuffer(PGL.GL_ELEMENT_ARRAY_BUFFER, 0);
  }

 
  // Utility function to render current tessellated geometry, under the assumption that
  // the texture is already bound.
  protected void renderTexFill() {
    if (!fillVBOsCreated) {
      createFillBuffers();
      fillVBOsCreated = true;
    }    
    updateFillBuffers(lights, true);
    
    FillShader shader = getFillShader(lights, true);      
    shader.start();
    
    shader.setVertexAttribute(glFillVertexBufferID, 3, PGL.GL_FLOAT, 0, 0);        
    shader.setColorAttribute(glFillColorBufferID, 4, PGL.GL_UNSIGNED_BYTE, 0, 0);
    shader.setTexCoordAttribute(glFillTexCoordBufferID, 2, PGL.GL_FLOAT, 0, 0);
    
    if (lights) {
      shader.setNormalAttribute(glFillNormalBufferID, 3, PGL.GL_FLOAT, 0, 0);
      shader.setAmbientAttribute(glFillAmbientBufferID, 4, PGL.GL_UNSIGNED_BYTE, 0, 0);
      shader.setSpecularAttribute(glFillSpecularBufferID, 4, PGL.GL_UNSIGNED_BYTE, 0, 0);
      shader.setEmissiveAttribute(glFillEmissiveBufferID, 4, PGL.GL_UNSIGNED_BYTE, 0, 0);      
      shader.setShininessAttribute(glFillShininessBufferID, 1, PGL.GL_FLOAT, 0, 0);
    }

    int size = tessGeo.fillIndexCount;
    int sizex = size * PGL.SIZEOF_INDEX;
    pgl.glBindBuffer(PGL.GL_ELEMENT_ARRAY_BUFFER, glFillIndexBufferID);
    pgl.glBufferData(PGL.GL_ELEMENT_ARRAY_BUFFER, sizex, ShortBuffer.wrap(tessGeo.fillIndices, 0, size), vboMode);
    pgl.glDrawElements(PGL.GL_TRIANGLES, size, PGL.INDEX_TYPE, 0);       
    pgl.glBindBuffer(PGL.GL_ELEMENT_ARRAY_BUFFER, 0);
    
    shader.stop();
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
    pg.splineForward(detail, bezierDrawMatrix);

    // multiply the basis and forward diff matrices together
    // saves much time since this needn't be done for each curve
    bezierDrawMatrix.apply(pg.bezierBasisMatrix);
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

    float x1 = inGeo.getLastVertexX();
    float y1 = inGeo.getLastVertexY();
    float z1 = inGeo.getLastVertexZ();

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
    float x1 = inGeo.getLastVertexX();
    float y1 = inGeo.getLastVertexY();
    float z1 = inGeo.getLastVertexZ();

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
                           fill, fillColor, 
                           stroke, strokeColor, strokeWeight,
                           ambientColor, specularColor, emissiveColor, shininess);
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
    pgl.glGetIntegerv(PGL.GL_SAMPLES, temp, 0);    
    if (antialias != temp[0]) {
      antialias = temp[0];
      PApplet.println("Effective multisampling level: " + antialias);
    }
    
    if (antialias < 2) {
      pgl.glEnable(PGL.GL_MULTISAMPLE);
      pgl.glEnable(PGL.GL_POINT_SMOOTH);
      pgl.glEnable(PGL.GL_LINE_SMOOTH);
      pgl.glEnable(PGL.GL_POLYGON_SMOOTH);    
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
    
    pgl.glDisable(PGL.GL_MULTISAMPLE);
    pgl.glDisable(PGL.GL_POINT_SMOOTH);
    pgl.glDisable(PGL.GL_LINE_SMOOTH);
    pgl.glDisable(PGL.GL_POLYGON_SMOOTH);    
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
    textTex = (PFontTexture)textFont.getCache(pg);        
    if (textTex == null) {
      textTex = new PFontTexture(parent, textFont, maxTextureSize, maxTextureSize);
      textFont.setCache(this, textTex);      
    } else {
      if (!pgl.contextIsCurrent(textTex.context)) {
        for (int i = 0; i < textTex.textures.length; i++) {
          textTex.textures[i].glID = 0; // To avoid finalization (texture objects were already deleted when context changed).
          textTex.textures[i] = null;
        }
        textTex = new PFontTexture(parent, textFont, PApplet.min(PGL.MAX_FONT_TEX_SIZE, maxTextureSize), 
                                                     PApplet.min(PGL.MAX_FONT_TEX_SIZE, maxTextureSize));
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
    int savedTintColor = tintColor;
    int savedBlendMode = blendMode;
    
    // Setting style used in text rendering.
    textureMode = NORMAL;    
    stroke = false;    
    normalX = 0;
    normalY = 0;
    normalZ = 1;    
    tint = true;
    tintColor = fillColor;
    
    blendMode(BLEND);
    
    super.textLineImpl(buffer, start, stop, x, y);
    
    // Restoring original style.
    textureMode  = savedTextureMode;
    stroke = savedStroke;
    normalX = savedNormalX;
    normalY = savedNormalY;
    normalZ = savedNormalZ;
    tint = savedTint;
    tintColor = savedTintColor;
    
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
    modelviewStack.push(new PMatrix3D(modelview));
    modelviewInvStack.push(new PMatrix3D(modelviewInv));
  }

  
  public void popMatrix() {
    if (hints[DISABLE_TRANSFORM_CACHE]) {
      flush();  
    }
    PMatrix3D mat;
    
    mat = modelviewStack.pop();
    modelview.set(mat);
        
    mat = modelviewInvStack.pop();
    modelviewInv.set(mat);
    
    calcProjmodelview();
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
    
    modelview.translate(tx, ty, tz);    
    modelviewInv.invTranslate(tx, ty, tz);    
    projmodelview.translate(tx, ty, tz);  
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

    modelview.rotate(angle, v0, v1, v2);
    modelviewInv.invRotate(angle, v0, v1, v2);
    calcProjmodelview(); // Possibly cheaper than doing projmodelview.rotate()
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

    modelview.scale(sx, sy, sz);
    modelviewInv.invScale(sx, sy, sz);    
    projmodelview.scale(sx, sy, sz);    
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
    modelview.reset();
    modelviewInv.reset();
    projmodelview.set(projection);
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
   * Apply a 4x4 transformation matrix to the modelview stack.
   */
  public void applyMatrix(float n00, float n01, float n02, float n03,
                          float n10, float n11, float n12, float n13, 
                          float n20, float n21, float n22, float n23, 
                          float n30, float n31, float n32, float n33) {
    if (hints[DISABLE_TRANSFORM_CACHE]) {
      flush();  
    }    
    modelview.apply(n00, n01, n02, n03,
                    n10, n11, n12, n13,
                    n20, n21, n22, n23,
                    n30, n31, n32, n33);
    projmodelview.apply(n00, n01, n02, n03,
                        n10, n11, n12, n13,
                        n20, n21, n22, n23,
                        n30, n31, n32, n33);
  }

  /*
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
    modelview.set(pMatrix);    
  }  
  */
  
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
    projectionStack.push(new PMatrix3D(projection));
  }
  
  
  public void popProjection() {
    PMatrix3D mat = projectionStack.pop();
    projection.set(mat);        
  }

  
  public void applyProjection(PMatrix3D mat) {
    projection.apply(mat);
  }

  
  public void setProjection(PMatrix3D mat) {
    projection.set(mat);
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
    cameraInv.set(modelviewInv);
    
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

    modelviewInv.set(modelview);
    modelviewInv.invert();    
    
    camera.set(modelview);
    cameraInv.set(modelviewInv);
    
    calcProjmodelview();
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
    
    calcProjmodelview();      
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
    lightSpecular(255, 255, 255);

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
    if (lightCount == PGL.MAX_LIGHTS) {
      throw new RuntimeException("can only create " + PGL.MAX_LIGHTS + " lights");
    }
    
    lightType[lightCount] = AMBIENT;
    
    lightPosition(lightCount, x, y, z, false);
    lightNormal(lightCount, 0, 0, 0);
        
    lightAmbient(lightCount, r, g, b);
    noLightDiffuse(lightCount);
    noLightSpecular(lightCount);
    noLightSpot(lightCount);
    lightFalloff(lightCount, currentLightFalloffConstant, 
                             currentLightFalloffLinear, 
                             currentLightFalloffQuadratic);
    
    lightCount++;
  }

  public void directionalLight(float r, float g, float b, 
                               float dx, float dy, float dz) {
    enableLighting();
    if (lightCount == PGL.MAX_LIGHTS) {
      throw new RuntimeException("can only create " + PGL.MAX_LIGHTS + " lights");
    }
    
    lightType[lightCount] = DIRECTIONAL;

    lightPosition(lightCount, 0, 0, 0, true);
    lightNormal(lightCount, dx, dy, dz);
        
    noLightAmbient(lightCount);
    lightDiffuse(lightCount, r, g, b);
    lightSpecular(lightCount, currentLightSpecular[0], 
                              currentLightSpecular[1], 
                              currentLightSpecular[2]);        
    noLightSpot(lightCount);
    noLightFalloff(lightCount);

    lightCount++;
  }

  public void pointLight(float r, float g, float b, 
                         float x, float y, float z) {
    enableLighting();   
    if (lightCount == PGL.MAX_LIGHTS) {
      throw new RuntimeException("can only create " + PGL.MAX_LIGHTS + " lights");
    }
    
    lightType[lightCount] = POINT;

    lightPosition(lightCount, x, y, z, false);
    lightNormal(lightCount, 0, 0, 0);
    
    noLightAmbient(lightCount);
    lightDiffuse(lightCount, r, g, b);
    lightSpecular(lightCount, currentLightSpecular[0], 
                              currentLightSpecular[1], 
                              currentLightSpecular[2]);
    noLightSpot(lightCount);
    lightFalloff(lightCount, currentLightFalloffConstant, 
                             currentLightFalloffLinear, 
                             currentLightFalloffQuadratic);
    
    lightCount++;
  }

  public void spotLight(float r, float g, float b, 
                        float x, float y, float z,
                        float dx, float dy, float dz, 
                        float angle, float concentration) {
    enableLighting();  
    if (lightCount == PGL.MAX_LIGHTS) {
      throw new RuntimeException("can only create " + PGL.MAX_LIGHTS + " lights");
    }
    
    lightType[lightCount] = SPOT;

    lightPosition(lightCount, x, y, z, false);
    lightNormal(lightCount, dx, dy, dz);
        
    noLightAmbient(lightCount);
    lightDiffuse(lightCount, r, g, b);
    lightSpecular(lightCount, currentLightSpecular[0], 
                              currentLightSpecular[1], 
                              currentLightSpecular[2]);
    lightSpot(lightCount, angle, concentration);    
    lightFalloff(lightCount, currentLightFalloffConstant, 
                             currentLightFalloffLinear, 
                             currentLightFalloffQuadratic);    
    
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
  }

  protected void enableLighting() {
    if (!lights) {      
      flush(); // Flushing non-lit geometry.      
      lights = true;
    }
  }

  protected void disableLighting() {
    if (lights) {      
      flush(); // Flushing lit geometry.      
      lights = false;
    }
  }  
  
  protected void lightPosition(int num, float x, float y, float z, boolean dir) {
    lightPosition[4 * num + 0] = x * modelview.m00 + y * modelview.m01 + z * modelview.m02 + modelview.m03;
    lightPosition[4 * num + 1] = x * modelview.m10 + y * modelview.m11 + z * modelview.m12 + modelview.m13;
    lightPosition[4 * num + 2] = x * modelview.m20 + y * modelview.m21 + z * modelview.m22 + modelview.m23;
    
    // Used to inicate if the light is directional or not.
    lightPosition[4 * num + 3] = dir ? 1: 0;
  }  

  protected void lightNormal(int num, float dx, float dy, float dz) {
    // Applying normal matrix to the light direction vector, which is the transpose of the inverse of the
    // modelview.
    float nx = dx * modelviewInv.m00 + dy * modelviewInv.m10 + dz * modelviewInv.m20;
    float ny = dx * modelviewInv.m01 + dy * modelviewInv.m11 + dz * modelviewInv.m21;
    float nz = dx * modelviewInv.m02 + dy * modelviewInv.m12 + dz * modelviewInv.m22;    
    
    float invn = 1.0f / PApplet.dist(0, 0, 0, nx, ny, nz);
    lightNormal[3 * num + 0] = invn * nx;
    lightNormal[3 * num + 1] = invn * ny;
    lightNormal[3 * num + 2] = invn * nz;
  }
  
  protected void lightAmbient(int num, float r, float g, float b) {       
    colorCalc(r, g, b);
    lightAmbient[3 * num + 0] = calcR;
    lightAmbient[3 * num + 1] = calcG;
    lightAmbient[3 * num + 2] = calcB;
  }

  protected void noLightAmbient(int num) {
    lightAmbient[3 * num + 0] = 0;
    lightAmbient[3 * num + 1] = 0;
    lightAmbient[3 * num + 2] = 0;
  }

  protected void lightDiffuse(int num, float r, float g, float b) {
    colorCalc(r, g, b);
    lightDiffuse[3 * num + 0] = calcR;
    lightDiffuse[3 * num + 1] = calcG;
    lightDiffuse[3 * num + 2] = calcB;
  }

  protected void noLightDiffuse(int num) {
    lightDiffuse[3 * num + 0] = 0;
    lightDiffuse[3 * num + 1] = 0;
    lightDiffuse[3 * num + 2] = 0;
  }

  protected void lightSpecular(int num, float r, float g, float b) {
    lightSpecular[3 * num + 0] = r;
    lightSpecular[3 * num + 1] = g;
    lightSpecular[3 * num + 2] = b;
  }

  protected void noLightSpecular(int num) {
    lightSpecular[3 * num + 0] = 0;
    lightSpecular[3 * num + 1] = 0;
    lightSpecular[3 * num + 2] = 0;
  }  
  
  protected void lightFalloff(int num, float c0, float c1, float c2) {
    lightFalloffCoefficients[3 * num + 0] = c0;
    lightFalloffCoefficients[3 * num + 1] = c1;
    lightFalloffCoefficients[3 * num + 2] = c2;
  }

  protected void noLightFalloff(int num) {
    lightFalloffCoefficients[3 * num + 0] = 1;
    lightFalloffCoefficients[3 * num + 1] = 0;
    lightFalloffCoefficients[3 * num + 2] = 0;
  }
  
  protected void lightSpot(int num, float angle, float exponent) {
    lightSpotParameters[2 * num + 0] = Math.max(0, PApplet.cos(angle));
    lightSpotParameters[2 * num + 1] = exponent;
  }
  
  protected void noLightSpot(int num) {
    lightSpotParameters[2 * num + 0] = 0;
    lightSpotParameters[2 * num + 1] = 0;
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
    
    pgl.glClearColor(0, 0, 0, 0);
    pgl.glClear(PGL.GL_DEPTH_BUFFER_BIT);

    pgl.glClearColor(backgroundR, backgroundG, backgroundB, 1);
    pgl.glClear(PGL.GL_COLOR_BUFFER_BIT);    
  }  
  
  //////////////////////////////////////////////////////////////

  // COLOR MODE

  // colorMode() is inherited from PGraphics.


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

  
  public boolean isGL() {
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
      pgl.setReadBuffer(PGL.GL_FRONT);
    }
    pgl.glReadPixels(0, 0, width, height, PGL.GL_RGBA, PGL.GL_UNSIGNED_BYTE, pixelBuffer);
    
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
      this.setCache(pg, texture);
      this.setParams(pg, params);
      
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
    pgl.glFinish();
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
      pgl.setReadBuffer(PGL.GL_FRONT);
    }
    pgl.glReadPixels(x, height - y - 1, 1, 1, PGL.GL_RGBA, PGL.GL_UNSIGNED_BYTE, getsetBuffer);

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
      pgl.setReadBuffer(PGL.GL_FRONT);
    }

    pgl.glReadPixels(x, height - y - h, w, h, PGL.GL_RGBA, PGL.GL_UNSIGNED_BYTE, newbieBuffer);
    
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
      pgl.glEnable(PGL.GL_BLEND);
      
      if (mode == REPLACE) {
        if (blendEqSupported) { 
          pgl.glBlendEquation(PGL.GL_FUNC_ADD);          
        }
        pgl.glBlendFunc(PGL.GL_ONE, PGL.GL_ZERO);
      } else if (mode == BLEND) {
        if (blendEqSupported) {
          pgl.glBlendEquation(PGL.GL_FUNC_ADD);
        }
        pgl.glBlendFunc(PGL.GL_SRC_ALPHA, PGL.GL_ONE_MINUS_SRC_ALPHA);
      } else if (mode == ADD) {
        if (blendEqSupported) {
          pgl.glBlendEquation(PGL.GL_FUNC_ADD);
        }
        pgl.glBlendFunc(PGL.GL_SRC_ALPHA, PGL.GL_ONE);
      } else if (mode == SUBTRACT) {
        if (blendEqSupported) {
          pgl.glBlendEquation(PGL.GL_FUNC_ADD);
        }
        pgl.glBlendFunc(PGL.GL_ONE_MINUS_DST_COLOR, PGL.GL_ZERO);
      } else if (mode == LIGHTEST) {
        if (blendEqSupported) { 
          pgl.glBlendEquation(PGL.GL_FUNC_MAX);
        } else {
          PGraphics.showWarning("This blend mode is not supported");
          return;
        }
        pgl.glBlendFunc(PGL.GL_SRC_ALPHA, PGL.GL_DST_ALPHA);
      } else if (mode == DARKEST) {
        if (blendEqSupported) {
          pgl.glBlendEquation(PGL.GL_FUNC_MIN);
        } else {
          PGraphics.showWarning("This blend mode is not supported");
          return;
        }        
        pgl.glBlendFunc(PGL.GL_SRC_ALPHA, PGL.GL_DST_ALPHA);
      } else if (mode == DIFFERENCE) {
        if (blendEqSupported) {
          pgl.glBlendEquation(PGL.GL_FUNC_REVERSE_SUBTRACT);
        } else {
          PGraphics.showWarning("This blend mode is not supported");
          return;
        }        
        pgl.glBlendFunc(PGL.GL_ONE, PGL.GL_ONE);
      } else if (mode == EXCLUSION) {
        if (blendEqSupported) {
          pgl.glBlendEquation(PGL.GL_FUNC_ADD);
        }
        pgl.glBlendFunc(PGL.GL_ONE_MINUS_DST_COLOR, PGL.GL_ONE_MINUS_SRC_COLOR);
      } else if (mode == MULTIPLY) {
        if (blendEqSupported) {
          pgl.glBlendEquation(PGL.GL_FUNC_ADD);
        }
        pgl.glBlendFunc(PGL.GL_DST_COLOR, PGL.GL_SRC_COLOR);
      } else if (mode == SCREEN) {
        if (blendEqSupported) {
          pgl.glBlendEquation(PGL.GL_FUNC_ADD);
        }
        pgl.glBlendFunc(PGL.GL_ONE_MINUS_DST_COLOR, PGL.GL_ONE);
      }  
      // HARD_LIGHT, SOFT_LIGHT, OVERLAY, DODGE, BURN modes cannot be implemented
      // in fixed-function pipeline because they require conditional blending and
      // non-linear blending equations.
    }
  }

  protected void setDefaultBlend() {
    blendMode = BLEND;
    pgl.glEnable(PGL.GL_BLEND);
    if (blendEqSupported) {
      pgl.glBlendEquation(PGL.GL_FUNC_ADD);
    }
    pgl.glBlendFunc(PGL.GL_SRC_ALPHA, PGL.GL_ONE_MINUS_SRC_ALPHA);
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
    PTexture tex = (PTexture)img.getCache(pg);
    if (tex == null) {
      tex = addTexture(img);      
    } else {       
      if (!pgl.contextIsCurrent(tex.context)) {
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
            
      if (tex.hasBuffers()) {
        tex.bufferUpdate();
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
    PTexture tex = (PTexture)img.getCache(pg);
    return tex;
  }
  
  
  /**
   * This utility method creates a texture for the provided image, and adds it
   * to the metadata cache of the image.
   * @param img the image to have a texture metadata associated to it
   */
  protected PTexture addTexture(PImage img) {
    PTexture.Parameters params = (PTexture.Parameters)img.getParams(pg);
    if (params == null) {
      params = PTexture.newParameters();
      img.setParams(pg, params);
    }
    PTexture tex = new PTexture(img.parent, img.width, img.height, params);    
    img.loadPixels();    
    if (img.pixels != null) tex.set(img.pixels);
    img.setCache(pg, tex);
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
    img.setCache(pg, tex);
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
    pgl.glBindTexture(target, id);
    
    int savedBlendMode = blendMode;
    blendMode(REPLACE); 
    
    // The texels of the texture replace the color of wherever is on the screen.      
//    texEnvMode = REPLACE;
    
    drawTexture(tw, th, crop, x, y, w, h);
    
    // Returning to the default texture environment mode, MODULATE. 
    // This allows tinting a texture with the current fragment color.       
//    texEnvMode = MODULATE;
    
    pgl.glBindTexture(target, 0);
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
    
    pgl.glDepthMask(false);

    // TODO: finish this!
    /*
    
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
    
    */
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
    pgl.glBindTexture(tex.glTarget, tex.glID);    
    pgl.glTexSubImage2D(tex.glTarget, 0, x, y, w, h, PGL.GL_RGBA, PGL.GL_UNSIGNED_BYTE, buffer);
    pgl.glBindTexture(tex.glTarget, 0);
    pgl.disableTexturing(tex.glTarget);
  }   

  
  //////////////////////////////////////////////////////////////
  
  // INITIALIZATION ROUTINES    
  
  static public void init() {
    PGL.startup(true);
  }


  static public void init(boolean beforeUI) {
    PGL.startup(beforeUI);
  }
    
  protected void restartSurface() {
    if (primarySurface) {
      initPrimary();
    } else {
      initOffscreen();
    }    
  }
  
  protected void initPrimary() {
    pgl.initPrimarySurface(antialias);
    pg = this;
  }
  
  protected void initOffscreen() {
    // Getting the context and capabilities from the main renderer.
    pg = (PGraphicsAndroid3D)parent.g;
    pgl.initOffscreenSurface(pg.pgl);
    
    updatePGL();
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
    if (PGraphicsAndroid3D.fboMultisampleSupported && 1 < antialias) {
      int nsamples = antialias;
      offscreenFramebufferMultisample = new PFramebuffer(parent, texture.glWidth, texture.glHeight, nsamples, 0, 
                                                         offscreenDepthBits, offscreenStencilBits, 
                                                         offscreenDepthBits == 24 && offscreenStencilBits == 8, false);
      
      offscreenFramebufferMultisample.clear();
      offscreenMultisample = true;
      
      pg.report("after cleaning fbm");
      
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
    pgl.updateOffscreenSurface(pg.pgl);
    updatePGL();
  }  
  
  protected void getGLParameters() {
    OPENGL_VENDOR     = pgl.glGetString(PGL.GL_VENDOR);  
    OPENGL_RENDERER   = pgl.glGetString(PGL.GL_RENDERER);
    OPENGL_VERSION    = pgl.glGetString(PGL.GL_VERSION);    
    OPENGL_EXTENSIONS = pgl.glGetString(PGL.GL_EXTENSIONS);
    
    npotTexSupported        = -1 < OPENGL_EXTENSIONS.indexOf("texture_non_power_of_two");
    mipmapGeneration        = -1 < OPENGL_EXTENSIONS.indexOf("generate_mipmap");
    vboSupported            = -1 < OPENGL_EXTENSIONS.indexOf("vertex_buffer_object");
    fboSupported            = -1 < OPENGL_EXTENSIONS.indexOf("framebuffer_object");
    fboMultisampleSupported = -1 < OPENGL_EXTENSIONS.indexOf("framebuffer_multisample");
       
    try {      
      pgl.glBlendEquation(PGL.GL_FUNC_ADD);
      blendEqSupported = true;
    } catch (UnsupportedOperationException e) {
      blendEqSupported = false;
    }
    
    int temp[] = new int[2];
    
    pgl.glGetIntegerv(PGL.GL_MAX_TEXTURE_SIZE, temp, 0);    
    maxTextureSize = temp[0];  
    
    pgl.glGetIntegerv(PGL.GL_ALIASED_LINE_WIDTH_RANGE, temp, 0);    
    maxLineWidth = temp[1];
    
    pgl.glGetIntegerv(PGL.GL_ALIASED_POINT_SIZE_RANGE, temp, 0);
    maxPointSize = temp[1];        
    
    glParamsRead = true;
  }
   
  //////////////////////////////////////////////////////////////
  
  // SHADER HANDLING

  public PShader loadShader(String vertFilename, String fragFilename, int kind) {
    if (kind == FILL_SHADER_SIMPLE) {
      return new FillShaderSimple(parent, vertFilename, fragFilename);
    } else if (kind == FILL_SHADER_LIT) {
      return new FillShaderLit(parent, vertFilename, fragFilename);
    } else if (kind == FILL_SHADER_TEX) {
      return new FillShaderTex(parent, vertFilename, fragFilename);
    } else if (kind == FILL_SHADER_FULL) {
      return new FillShaderFull(parent, vertFilename, fragFilename);
    } else if (kind == LINE_SHADER) {
      return new LineShader(parent, vertFilename, fragFilename);
    } else if (kind == POINT_SHADER) {      
      return new PointShader(parent, vertFilename, fragFilename);
    } else {
      PGraphics.showWarning("Wrong shader type");
      return null;
    }
  }

  public void setShader(PShader shader, int kind) {
    if (kind == FILL_SHADER_SIMPLE) {
      fillShaderSimple = (FillShaderSimple) shader;
    } else if (kind == FILL_SHADER_LIT) {
      fillShaderLit = (FillShaderLit) shader;
    } else if (kind == FILL_SHADER_TEX) {
      fillShaderTex = (FillShaderTex) shader;
    } else if (kind == FILL_SHADER_FULL) {
      fillShaderFull = (FillShaderFull) shader;
    } else if (kind == LINE_SHADER) {
      lineShader = (LineShader) shader;
    } else if (kind == POINT_SHADER) {      
      pointShader = (PointShader) shader;
    } else {
      PGraphics.showWarning("Wrong shader type");
    }    
  }

  public void resetShader(int kind) {
    if (kind == FILL_SHADER_SIMPLE) {
      if (defFillShaderSimple == null) {
        defFillShaderSimple = new FillShaderSimple(parent, defFillShaderVertSimpleURL, defFillShaderFragNoTexURL);       
      }      
      fillShaderSimple = defFillShaderSimple;
    } else if (kind == FILL_SHADER_LIT) {
      if (defFillShaderLit == null) {
        defFillShaderLit = new FillShaderLit(parent, defFillShaderVertLitURL, defFillShaderFragNoTexURL);
      }      
      fillShaderLit = defFillShaderLit;
    } else if (kind == FILL_SHADER_TEX) {
      if (defFillShaderTex == null) {
        defFillShaderTex = new FillShaderTex(parent, defFillShaderVertTexURL, defFillShaderFragTexURL);
      }      
      fillShaderTex = defFillShaderTex;
    } else if (kind == FILL_SHADER_FULL) {
      if (defFillShaderFull == null) {
        defFillShaderFull = new FillShaderFull(parent, defFillShaderVertFullURL, defFillShaderFragTexURL);
      }      
      fillShaderFull = defFillShaderFull;
    } else if (kind == LINE_SHADER) {
      if (defLineShader == null) {
        defLineShader = new LineShader(parent, defLineShaderVertURL, defLineShaderFragURL);        
      }      
      lineShader = defLineShader;
    } else if (kind == POINT_SHADER) {
      if (defPointShader == null) {
        defPointShader = new PointShader(parent, defPointShaderVertURL, defPointShaderFragURL);        
      }      
      pointShader = defPointShader;
    } else {
      PGraphics.showWarning("Wrong shader type");
    }     
  }
  
  protected FillShader getFillShader(boolean lit, boolean tex) {
    FillShader shader;
    if (lit) {
      if (tex) {
        if (fillShaderFull == null) {
          if (defFillShaderFull == null) {
            defFillShaderFull = new FillShaderFull(parent, defFillShaderVertFullURL, defFillShaderFragTexURL);
          }
          fillShaderFull = defFillShaderFull;
        }
        shader = fillShaderFull;  
      } else {
        if (defFillShaderLit == null) {
          if (defFillShaderLit == null) {
            defFillShaderLit = new FillShaderLit(parent, defFillShaderVertLitURL, defFillShaderFragNoTexURL);
          }
          fillShaderLit = defFillShaderLit;
        }
        shader = fillShaderLit;
      }
    } else {
      if (tex) {
        if (fillShaderTex == null) {
          if (defFillShaderTex == null) {
            defFillShaderTex = new FillShaderTex(parent, defFillShaderVertTexURL, defFillShaderFragTexURL);
          }
          fillShaderTex = defFillShaderTex;
        }
        shader = fillShaderTex;
      } else {
        if (fillShaderSimple == null) {
          if (defFillShaderSimple == null) {
            defFillShaderSimple = new FillShaderSimple(parent, defFillShaderVertSimpleURL, defFillShaderFragNoTexURL);            
          }
          fillShaderSimple = defFillShaderSimple;
        }        
        shader = fillShaderSimple;
      }      
    }    
    shader.loadAttributes();
    shader.loadUniforms();
    return shader;
  }
  
  protected LineShader getLineShader() {
    if (lineShader == null) {
      if (defLineShader == null) {
        defLineShader = new LineShader(parent, defLineShaderVertURL, defLineShaderFragURL);
      }
      lineShader = defLineShader;  
    }
    lineShader.loadAttributes();
    lineShader.loadUniforms();
    return lineShader;
  }

  protected PointShader getPointShader() {
    if (pointShader == null) {
      if (defPointShader == null) {
        defPointShader = new PointShader(parent, defPointShaderVertURL, defPointShaderFragURL);
      }
      pointShader = defPointShader;  
    }
    pointShader.loadAttributes();
    pointShader.loadUniforms();
    return pointShader;    
  }  
  
  protected class FillShader extends PShader {
    public FillShader(PApplet parent, String vertFilename, String fragFilename) {
      super(parent, vertFilename, fragFilename);
    }
    
    public FillShader(PApplet parent, URL vertURL, URL fragURL) {
      super(parent, vertURL, fragURL);
    }
    
    public void loadAttributes() { }    
    public void loadUniforms() { }
    
    public void setAttribute(int loc, int vboId, int size, int type, boolean normalized, int stride, int offset) {     
      pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, vboId);
      pgl.glVertexAttribPointer(loc, size, type, normalized, stride, offset);       
    }
    
    public void setVertexAttribute(int vboId, int size, int type, int stride, int offset) { }    
    public void setColorAttribute(int vboId, int size, int type, int stride, int offset) { }
    public void setNormalAttribute(int vboId, int size, int type, int stride, int offset) { }
    public void setAmbientAttribute(int vboId, int size, int type, int stride, int offset) { }
    public void setSpecularAttribute(int vboId, int size, int type, int stride, int offset) { }
    public void setEmissiveAttribute(int vboId, int size, int type, int stride, int offset) { }
    public void setShininessAttribute(int vboId, int size, int type, int stride, int offset) { }
    public void setTexCoordAttribute(int vboId, int size, int type, int stride, int offset) { }
  }
  
  protected class FillShaderSimple extends FillShader {
    protected int projmodelviewMatrixLoc;
    
    protected int inVertexLoc;
    protected int inColorLoc;

    public FillShaderSimple(PApplet parent, String vertFilename, String fragFilename) {
      super(parent, vertFilename, fragFilename);
    }
    
    public FillShaderSimple(PApplet parent, URL vertURL, URL fragURL) {
      super(parent, vertURL, fragURL);
    }    
    
    public void loadAttributes() {
      inVertexLoc = getAttribLocation("inVertex");
      inColorLoc = getAttribLocation("inColor");
    }
    
    public void loadUniforms() { 
      projmodelviewMatrixLoc = getUniformLocation("projmodelviewMatrix");
    }
    
    public void setVertexAttribute(int vboId, int size, int type, int stride, int offset) { 
      setAttribute(inVertexLoc, vboId, size, type, false, stride, offset);
    } 
    
    public void setColorAttribute(int vboId, int size, int type, int stride, int offset) { 
      setAttribute(inColorLoc, vboId, size, type, true, stride, offset); 
    }  
    
    public void start() {
      super.start();

      pgl.glEnableVertexAttribArray(inVertexLoc);
      pgl.glEnableVertexAttribArray(inColorLoc);
      
      updateGLProjmodelview();
      set4x4MatUniform(projmodelviewMatrixLoc, glProjmodelview);      
    }

    public void stop() {      
      pgl.glDisableVertexAttribArray(inVertexLoc);
      pgl.glDisableVertexAttribArray(inColorLoc);
      
      pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, 0);
      
      super.stop();
    }
  }

  protected class FillShaderLit extends FillShader {
    protected int projmodelviewMatrixLoc;
    protected int modelviewMatrixLoc;    
    protected int normalMatrixLoc;
    
    protected int lightCountLoc;  
    protected int lightPositionLoc;
    protected int lightNormalLoc;
    protected int lightAmbientLoc;
    protected int lightDiffuseLoc;
    protected int lightSpecularLoc;
    protected int lightFalloffCoefficientsLoc;
    protected int lightSpotParametersLoc;      
    
    protected int inVertexLoc;
    protected int inColorLoc;
    protected int inNormalLoc;

    protected int inAmbientLoc;
    protected int inSpecularLoc;
    protected int inEmissiveLoc;
    protected int inShineLoc;    
    
    public FillShaderLit(PApplet parent, String vertFilename, String fragFilename) {
      super(parent, vertFilename, fragFilename);
    }
    
    public FillShaderLit(PApplet parent, URL vertURL, URL fragURL) {
      super(parent, vertURL, fragURL);
    }    
    
    public void loadAttributes() {
      inVertexLoc = getAttribLocation("inVertex");
      inColorLoc = getAttribLocation("inColor");
      inNormalLoc = getAttribLocation("inNormal");
      
      inAmbientLoc = getAttribLocation("inAmbient");
      inSpecularLoc = getAttribLocation("inSpecular");
      inEmissiveLoc = getAttribLocation("inEmissive");
      inShineLoc = getAttribLocation("inShine");
    }
    
    public void loadUniforms() { 
      projmodelviewMatrixLoc = getUniformLocation("projmodelviewMatrix");      
      modelviewMatrixLoc = getUniformLocation("modelviewMatrix");
      normalMatrixLoc = getUniformLocation("normalMatrix");
      
      lightCountLoc = getUniformLocation("lightCount");
      lightPositionLoc = getUniformLocation("lightPosition");
      lightNormalLoc = getUniformLocation("lightNormal");
      lightAmbientLoc = getUniformLocation("lightAmbient");
      lightDiffuseLoc = getUniformLocation("lightDiffuse");
      lightSpecularLoc = getUniformLocation("lightSpecular");
      lightFalloffCoefficientsLoc = getUniformLocation("lightFalloffCoefficients");
      lightSpotParametersLoc = getUniformLocation("lightSpotParameters");      
    }    
    
    public void setVertexAttribute(int vboId, int size, int type, int stride, int offset) { 
      setAttribute(inVertexLoc, vboId, size, type, false, stride, offset);
    } 
    
    public void setColorAttribute(int vboId, int size, int type, int stride, int offset) { 
      setAttribute(inColorLoc, vboId, size, type, true, stride, offset); 
    } 

    public void setNormalAttribute(int vboId, int size, int type, int stride, int offset) { 
      setAttribute(inNormalLoc, vboId, size, type, false, stride, offset);
    }
    
    public void setAmbientAttribute(int vboId, int size, int type, int stride, int offset) { 
      setAttribute(inAmbientLoc, vboId, size, type, true, stride, offset);
    }
    
    public void setSpecularAttribute(int vboId, int size, int type, int stride, int offset) { 
      setAttribute(inSpecularLoc, vboId, size, type, true, stride, offset);
    }
    
    public void setEmissiveAttribute(int vboId, int size, int type, int stride, int offset) { 
      setAttribute(inEmissiveLoc, vboId, size, type, true, stride, offset);
    }
    
    public void setShininessAttribute(int vboId, int size, int type, int stride, int offset) { 
      setAttribute(inShineLoc, vboId, size, type, false, stride, offset);
    }       
    
    public void start() {
      super.start();
      
      pgl.glEnableVertexAttribArray(inVertexLoc);
      pgl.glEnableVertexAttribArray(inColorLoc);      
      pgl.glEnableVertexAttribArray(inNormalLoc);
      
      pgl.glEnableVertexAttribArray(inAmbientLoc);
      pgl.glEnableVertexAttribArray(inSpecularLoc);
      pgl.glEnableVertexAttribArray(inEmissiveLoc);
      pgl.glEnableVertexAttribArray(inShineLoc);         
      
      updateGLProjmodelview();
      set4x4MatUniform(projmodelviewMatrixLoc, glProjmodelview);
      
      updateGLModelview();
      set4x4MatUniform(modelviewMatrixLoc, glModelview);
      
      updateGLNormal();
      set3x3MatUniform(normalMatrixLoc, glNormal);
      
      setIntUniform(lightCountLoc, lightCount);      
      set4FloatVecUniform(lightPositionLoc, lightPosition);
      set3FloatVecUniform(lightNormalLoc, lightNormal);
      set3FloatVecUniform(lightAmbientLoc, lightAmbient);
      set3FloatVecUniform(lightDiffuseLoc, lightDiffuse);
      set3FloatVecUniform(lightSpecularLoc, lightSpecular);
      set3FloatVecUniform(lightFalloffCoefficientsLoc, lightFalloffCoefficients);
      set2FloatVecUniform(lightSpotParametersLoc, lightSpotParameters);
    }

    public void stop() {      
      pgl.glDisableVertexAttribArray(inVertexLoc);
      pgl.glDisableVertexAttribArray(inColorLoc);      
      pgl.glDisableVertexAttribArray(inNormalLoc);
      
      pgl.glDisableVertexAttribArray(inAmbientLoc);
      pgl.glDisableVertexAttribArray(inSpecularLoc);
      pgl.glDisableVertexAttribArray(inEmissiveLoc);
      pgl.glDisableVertexAttribArray(inShineLoc);     
      
      pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, 0);
      
      super.stop();
    }    
  }
  
  protected class FillShaderTex extends FillShaderSimple {
    protected int inTexcoordLoc;
    
    public FillShaderTex(PApplet parent, String vertFilename, String fragFilename) {
      super(parent, vertFilename, fragFilename);
    }
    
    public FillShaderTex(PApplet parent, URL vertURL, URL fragURL) {
      super(parent, vertURL, fragURL);
    }        
    
    public void loadAttributes() {
      super.loadAttributes();
      
      inTexcoordLoc = getAttribLocation("inTexcoord");
    }    
    
    public void setTexCoordAttribute(int vboId, int size, int type, int stride, int offset) { 
      setAttribute(inTexcoordLoc, vboId, size, type, false, stride, offset);
    }     
    
    public void start() {
      super.start();
      
      pgl.glEnableVertexAttribArray(inTexcoordLoc);
    }    
    
    public void stop() {      
      pgl.glDisableVertexAttribArray(inTexcoordLoc);
      
      super.stop();
    }    
  }  
  
  protected class FillShaderFull extends FillShaderLit {
    protected int inTexcoordLoc;
    
    public FillShaderFull(PApplet parent, String vertFilename, String fragFilename) {
      super(parent, vertFilename, fragFilename);
    }
    
    public FillShaderFull(PApplet parent, URL vertURL, URL fragURL) {
      super(parent, vertURL, fragURL);
    }     
    
    public void loadAttributes() {
      super.loadAttributes();
      
      inTexcoordLoc = getAttribLocation("inTexcoord");
    }    
    
    public void setTexCoordAttribute(int vboId, int size, int type, int stride, int offset) { 
      setAttribute(inTexcoordLoc, vboId, size, type, false, stride, offset);
    }     
    
    public void start() {
      super.start();
      
      pgl.glEnableVertexAttribArray(inTexcoordLoc);
    }
    
    public void stop() {      
      pgl.glDisableVertexAttribArray(inTexcoordLoc);
      
      super.stop();
    }    
  } 
  
  protected class LineShader extends PShader {
    protected int projectionMatrixLoc;
    protected int modelviewMatrixLoc;

    protected int viewportLoc;
    protected int perspectiveLoc;

    protected int inVertexLoc;
    protected int inColorLoc;
    protected int inDirWidthLoc;
    
    public LineShader(PApplet parent, String vertFilename, String fragFilename) {
      super(parent, vertFilename, fragFilename);
    }
    
    public LineShader(PApplet parent, URL vertURL, URL fragURL) {
      super(parent, vertURL, fragURL);
    }       
    
    public void loadAttributes() {
      inVertexLoc = getAttribLocation("inVertex");
      inColorLoc = getAttribLocation("inColor");
      inDirWidthLoc = getAttribLocation("inDirWidth");      
    } 
    
    public void loadUniforms() { 
      projectionMatrixLoc = getUniformLocation("projectionMatrix");      
      modelviewMatrixLoc = getUniformLocation("modelviewMatrix");
      
      viewportLoc = getUniformLocation("viewport");
      perspectiveLoc = getUniformLocation("perspective");
    }
    
    public void setAttribute(int loc, int vboId, int size, int type, boolean normalized, int stride, int offset) {
      pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, vboId);
      pgl.glVertexAttribPointer(loc, size, type, normalized, stride, offset);       
    }
    
    public void setVertexAttribute(int vboId, int size, int type, int stride, int offset) { 
      setAttribute(inVertexLoc, vboId, size, type, false, stride, offset);
    } 
    
    public void setColorAttribute(int vboId, int size, int type, int stride, int offset) { 
      setAttribute(inColorLoc, vboId, size, type, true, stride, offset); 
    }  

    public void setDirWidthAttribute(int vboId, int size, int type, int stride, int offset) { 
      setAttribute(inDirWidthLoc, vboId, size, type, false, stride, offset); 
    }
    
    public void start() {
      super.start();
      
      pgl.glEnableVertexAttribArray(inVertexLoc);
      pgl.glEnableVertexAttribArray(inColorLoc);
      pgl.glEnableVertexAttribArray(inDirWidthLoc);      
      
      updateGLProjection();
      set4x4MatUniform(projectionMatrixLoc, glProjection);

      updateGLModelview();
      set4x4MatUniform(modelviewMatrixLoc, glModelview);      
      
      set4FloatUniform(viewportLoc, viewport[0], viewport[1], viewport[2], viewport[3]);
      
      if (hints[ENABLE_PERSPECTIVE_CORRECTED_LINES]) {
        setIntUniform(perspectiveLoc, 1);
      } else {
        setIntUniform(perspectiveLoc, 0);
      }
    }

    public void stop() {      
      pgl.glDisableVertexAttribArray(inVertexLoc);
      pgl.glDisableVertexAttribArray(inColorLoc);
      pgl.glDisableVertexAttribArray(inDirWidthLoc);
      
      pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, 0);
      
      super.stop();
    }
  }
  
  protected class PointShader extends PShader {
    protected int projectionMatrixLoc;
    protected int modelviewMatrixLoc;
     
    protected int inVertexLoc;
    protected int inColorLoc;
    protected int inSizeLoc;

    public PointShader(PApplet parent, String vertFilename, String fragFilename) {
      super(parent, vertFilename, fragFilename);
    }
    
    public PointShader(PApplet parent, URL vertURL, URL fragURL) {
      super(parent, vertURL, fragURL);
    }        
    
    public void loadAttributes() {
      inVertexLoc = getAttribLocation("inVertex");
      inColorLoc = getAttribLocation("inColor");
      inSizeLoc = getAttribLocation("inSize");      
    } 
    
    public void loadUniforms() { 
      projectionMatrixLoc = getUniformLocation("projectionMatrix");      
      modelviewMatrixLoc = getUniformLocation("modelviewMatrix");
    }
    
    public void setAttribute(int loc, int vboId, int size, int type, boolean normalized, int stride, int offset) {
      pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, vboId);
      pgl.glVertexAttribPointer(loc, size, type, normalized, stride, offset);       
    }
    
    public void setVertexAttribute(int vboId, int size, int type, int stride, int offset) { 
      setAttribute(inVertexLoc, vboId, size, type, false, stride, offset);
    } 
    
    public void setColorAttribute(int vboId, int size, int type, int stride, int offset) { 
      setAttribute(inColorLoc, vboId, size, type, true, stride, offset); 
    }  

    public void setSizeAttribute(int vboId, int size, int type, int stride, int offset) { 
      setAttribute(inSizeLoc, vboId, size, type, false, stride, offset); 
    }
    
    public void start() {
      super.start();
      
      pgl.glEnableVertexAttribArray(inVertexLoc);
      pgl.glEnableVertexAttribArray(inColorLoc);
      pgl.glEnableVertexAttribArray(inSizeLoc);      
      
      updateGLProjection();
      set4x4MatUniform(projectionMatrixLoc, glProjection);

      updateGLModelview();
      set4x4MatUniform(modelviewMatrixLoc, glModelview);      
    }

    public void stop() {      
      pgl.glDisableVertexAttribArray(inVertexLoc);
      pgl.glDisableVertexAttribArray(inColorLoc);
      pgl.glDisableVertexAttribArray(inSizeLoc);
      
      pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, 0);
      
      super.stop();
    }    
  }
    
  //////////////////////////////////////////////////////////////
  
  // Input (raw) and Tessellated geometry, tessellator.        
    
  public InGeometry newInGeometry(int mode) {
    return new InGeometry(mode); 
  }
  
  protected TessGeometry newTessGeometry(int mode) {
    return new TessGeometry(mode);
  }
  
  protected TexCache newTexCache() {
    return new TexCache();
  }  
  
  // Holds an array of textures and the range of vertex
  // indices each texture applies to.
  public class TexCache {
    protected int count;
    protected PImage[] textures;
    protected int[] firstIndex;
    protected int[] lastIndex;
    protected boolean hasTexture;
    protected PTexture tex0;
    
    public TexCache() {
      allocate();
    }
    
    public void allocate() {
      textures = new PImage[PGL.DEFAULT_IN_TEXTURES];
      firstIndex = new int[PGL.DEFAULT_IN_TEXTURES];
      lastIndex = new int[PGL.DEFAULT_IN_TEXTURES];
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
    
    public void beginRender() {
      tex0 = null;
    }

    public PTexture getTexture(int i) {
      PImage img = textures[i];
      PTexture tex = null;
      
      if (img != null) {
        tex = pg.getTexture(img);
        if (tex != null) {                   
          tex.bind();          
          tex0 = tex;
        }
      }
      if (tex == null && tex0 != null) {
        tex0.unbind();
        pgl.disableTexturing(tex0.glTarget);        
      }
      
      return tex;
    }
    
    public void endRender() {
      if (hasTexture) {
        // Unbinding all the textures in the cache.      
        for (int i = 0; i < count; i++) {
          PImage img = textures[i];
          if (img != null) {
            PTexture tex = pg.getTexture(img);
            if (tex != null) {
              tex.unbind();  
            }
          }
        }
        // Disabling texturing for each of the targets used
        // by textures in the cache.
        for (int i = 0; i < count; i++) {
          PImage img = textures[i];
          if (img != null) {
            PTexture tex = pg.getTexture(img);
            if (tex != null) {
              pgl.disableTexturing(tex.glTarget);
            }          
          }
        }
      }      
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
  
  // Holds the input vertices: xyz coordinates, fill/tint color, 
  // normal, texture coordinates and stroke color and weight.
  public class InGeometry {
    int renderMode;
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
    public int[] colors;
    public float[] normals;
    public float[] texcoords;
    public int[] scolors;
    public float[] sweights;
    
    // Material properties
    public int[] ambient;
    public int[] specular;
    public int[] emissive;
    public float[] shininess;
    
    public int[][] edges;
    
    // For later, to be used by libraries...
    //public float[][] mtexcoords;
    //public float[][] attributes;

    public InGeometry(int mode) {
      renderMode = mode;
      allocate();
    }    
    
    public void reset() {
      vertexCount = firstVertex = lastVertex = 0; 
      edgeCount = firstEdge = lastEdge = 0;
    }
    
    public void allocate() {      
      codes = new int[PGL.DEFAULT_IN_VERTICES];
      vertices = new float[3 * PGL.DEFAULT_IN_VERTICES];
      colors = new int[PGL.DEFAULT_IN_VERTICES];      
      normals = new float[3 * PGL.DEFAULT_IN_VERTICES];
      texcoords = new float[2 * PGL.DEFAULT_IN_VERTICES];
      scolors = new int[PGL.DEFAULT_IN_VERTICES];
      sweights = new float[PGL.DEFAULT_IN_VERTICES];      
      ambient = new int[PGL.DEFAULT_IN_VERTICES];
      specular = new int[PGL.DEFAULT_IN_VERTICES];
      emissive = new int[PGL.DEFAULT_IN_VERTICES];
      shininess = new float[PGL.DEFAULT_IN_VERTICES];
      edges = new int[PGL.DEFAULT_IN_EDGES][3];
      reset();
    }
    
    public void trim() {
      if (vertexCount < vertices.length / 3) {
        trimVertices();
        trimColors();
        trimNormals();
        trimTexcoords();
        trimStrokeColors();
        trimStrokeWeights();        
        trimAmbient();
        trimSpecular();
        trimEmissive();
        trimShininess();
        trimEdges();
      }      
    }
    
    public void dispose() {
      codes = null;
      vertices = null;
      colors = null;      
      normals = null;
      texcoords = null;
      scolors = null;
      scolors = null;
      ambient = null;
      specular = null;
      emissive = null;
      shininess = null;      
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
    
    public float getLastVertexX() {
      return vertices[3 * (vertexCount - 1) + 0];  
    }
    
    public float getLastVertexY() {
      return vertices[3 * (vertexCount - 1) + 1];
    }    
    
    public float getLastVertexZ() {
      return vertices[3 * (vertexCount - 1) + 2];
    }
    
    public boolean isFull() {
      return PGL.MAX_TESS_VERTICES <= vertexCount;
    }

    
    public int addVertex(float x, float y, 
                         int fcolor,
                         float u, float v,
                         int scolor, float sweight,
                         int am, int sp, int em, float shine,
                         int code) {
      return addVertex(x, y, 0, 
                       fcolor,
                       0, 0, 1, 
                       u, v, 
                       scolor, sweight, 
                       am, sp, em, shine,
                       code);      
    }

    public int addVertex(float x, float y, 
                         int fcolor,
                         int scolor, float sweight,
                         int am, int sp, int em, float shine,
                         int code) {
      return addVertex(x, y, 0, 
                       fcolor,
                       0, 0, 1, 
                       0, 0, 
                       scolor, sweight,
                       am, sp, em, shine,
                       code);   
    }    
    
    public int addVertex(float x, float y, float z, 
                         int fcolor,
                         float nx, float ny, float nz,
                         float u, float v,
                         int scolor, float sweight, 
                         int am, int sp, int em, float shine,
                         int code) {
      vertexCheck();
      int index;

      codes[vertexCount] = code;      
      
      index = 3 * vertexCount;
      vertices[index++] = x;
      vertices[index++] = y;
      vertices[index  ] = z;

      colors[vertexCount] = javaToNativeARGB(fcolor);

      index = 3 * vertexCount;
      normals[index++] = nx;
      normals[index++] = ny;
      normals[index  ] = ny;      
      
      index = 2 * vertexCount;
      texcoords[index++] = u;
      texcoords[index  ] = v;

      scolors[vertexCount] = javaToNativeARGB(scolor);
      sweights[vertexCount] = sweight;

      ambient[vertexCount] = javaToNativeARGB(am);
      specular[vertexCount] = javaToNativeARGB(sp);
      emissive[vertexCount] = javaToNativeARGB(em);
      shininess[vertexCount] = shine;
      
      lastVertex = vertexCount; 
      vertexCount++;
      
      return lastVertex; 
    }
        
    public int javaToNativeARGB(int color) {
      if (BIG_ENDIAN) {
        return ((color >> 24) & 0xff) | ((color << 8) & 0xffffff00);
      } else {
        return (color & 0xff000000)
               | ((color << 16) & 0xff0000) | (color & 0xff00)
               | ((color >> 16) & 0xff);
      }     
    }
    
    public void vertexCheck() {
      if (vertexCount == vertices.length / 3) {
        int newSize = vertexCount << 1;

        expandCodes(newSize);
        expandVertices(newSize);
        expandColors(newSize);
        expandNormals(newSize);
        expandTexcoords(newSize);      
        expandStrokeColors(newSize);
        expandStrokeWeights(newSize);
        expandAmbient(newSize);
        expandSpecular(newSize);
        expandEmissive(newSize);
        expandShininess(newSize);
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
        int newLen = edgeCount << 1;
        
        int temp[][] = new int[newLen][3];
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

    protected void expandColors(int n) {
      int temp[] = new int[n];      
      PApplet.arrayCopy(colors, 0, temp, 0, vertexCount);
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
        
    protected void expandStrokeColors(int n) {
      int temp[] = new int[n];      
      PApplet.arrayCopy(scolors, 0, temp, 0, vertexCount);
      scolors = temp;
    }

    protected void expandStrokeWeights(int n) {
      float temp[] = new float[n];      
      PApplet.arrayCopy(sweights, 0, temp, 0, vertexCount);
      sweights = temp;
    }    
    
    protected void expandAmbient(int n) {
      int temp[] = new int[n];      
      PApplet.arrayCopy(ambient, 0, temp, 0, vertexCount);
      ambient = temp;          
    }
    
    protected void expandSpecular(int n) {
      int temp[] = new int[n];      
      PApplet.arrayCopy(specular, 0, temp, 0, vertexCount);
      specular = temp;       
    }
    
    protected void expandEmissive(int n) {
      int temp[] = new int[n];      
      PApplet.arrayCopy(emissive, 0, temp, 0, vertexCount);
      emissive = temp;      
    }
    
    protected void expandShininess(int n) {
      float temp[] = new float[n];      
      PApplet.arrayCopy(shininess, 0, temp, 0, vertexCount);
      shininess = temp;       
    }
    
    protected void trimVertices() {
      float temp[] = new float[3 * vertexCount];      
      PApplet.arrayCopy(vertices, 0, temp, 0, 3 * vertexCount);
      vertices = temp;      
    }
    
    protected void trimColors() {
      int temp[] = new int[vertexCount];      
      PApplet.arrayCopy(colors, 0, temp, 0, vertexCount);
      colors = temp;        
    }

    protected void trimNormals() {
      float temp[] = new float[3 * vertexCount];      
      PApplet.arrayCopy(normals, 0, temp, 0, 3 * vertexCount);
      normals = temp;          
    }
    
    protected void trimTexcoords() {
      float temp[] = new float[2 * vertexCount];      
      PApplet.arrayCopy(texcoords, 0, temp, 0, 2 * vertexCount);
      texcoords = temp;    
    }
        
    protected void trimStrokeColors() {
      int temp[] = new int[vertexCount];      
      PApplet.arrayCopy(scolors, 0, temp, 0, vertexCount);
      scolors = temp;
    }    

    protected void trimStrokeWeights() {
      float temp[] = new float[vertexCount];      
      PApplet.arrayCopy(sweights, 0, temp, 0, vertexCount);
      sweights = temp;
    }        
    
    protected void trimAmbient() {
      int temp[] = new int[vertexCount];
      PApplet.arrayCopy(ambient, 0, temp, 0, vertexCount);
      ambient = temp;      
    }
    
    protected void trimSpecular() {
      int temp[] = new int[vertexCount];
      PApplet.arrayCopy(specular, 0, temp, 0, vertexCount);
      specular = temp;      
    }
        
    protected void trimEmissive() {
      int temp[] = new int[vertexCount];
      PApplet.arrayCopy(emissive, 0, temp, 0, vertexCount);
      emissive = temp; 
    }
    
    protected void trimShininess() {
      float temp[] = new float[vertexCount];
      PApplet.arrayCopy(shininess, 0, temp, 0, vertexCount);
      shininess = temp;      
    }

    protected void trimEdges() {
      int temp[][] = new int[edgeCount][3];
      PApplet.arrayCopy(edges, 0, temp, 0, edgeCount);
      edges = temp;        
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
                                boolean fill, int fillColor, 
                                boolean stroke, int strokeColor, float strokeWeight,
                                int ambient, int specular, int emissive, float shininess) {      
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
      float inc = (float) PGraphicsAndroid3D.SINCOS_LENGTH / accuracy;
      
      if (fill) {
        addVertex(centerX, centerY, 
                  fillColor, strokeColor, strokeWeight,
                  ambient, specular, emissive, shininess,
                  VERTEX);
      }
      int idx0, pidx, idx;
      idx0 = pidx = idx = 0;
      float val = 0;
      for (int i = 0; i < accuracy; i++) {
        idx = addVertex(centerX + PGraphicsAndroid3D.cosLUT[(int) val] * radiusH, 
                        centerY + PGraphicsAndroid3D.sinLUT[(int) val] * radiusV, 
                        fillColor, strokeColor, strokeWeight,
                        ambient, specular, emissive, shininess,
                        VERTEX);
        val = (val + inc) % PGraphicsAndroid3D.SINCOS_LENGTH;
        
        if (0 < i) {
          if (stroke) addEdge(pidx, idx, i == 1, false);
        } else {
          idx0 = idx;  
        }
        
        pidx = idx;
      }
      // Back to the beginning
      addVertex(centerX + PGraphicsAndroid3D.cosLUT[0] * radiusH, 
                centerY + PGraphicsAndroid3D.sinLUT[0] * radiusV, 
                fillColor, strokeColor, strokeWeight,
                ambient, specular, emissive, shininess,
                VERTEX);
      if (stroke) addEdge(idx, idx0, false, true);
    }
    
  }
  
  // Holds tessellated data for fill, line and point geometry.
  public class TessGeometry {
    int renderMode;
    
    // Tessellated fill data
    public int fillVertexCount;
    public int firstFillVertex;
    public int lastFillVertex;    
    public float[] fillVertices;
    public int[] fillColors;
    public float[] fillNormals;
    public float[] fillTexcoords;
    
    // Fill material properties (fillColor is used
    // as the diffuse color when lighting is enabled)
    public int[] fillAmbient;
    public int[] fillSpecular;
    public int[] fillEmissive;
    public float[] fillShininess;
        
    public int fillIndexCount;
    public int firstFillIndex;
    public int lastFillIndex;    
    public short[] fillIndices;
    
    // Tessellated line data    
    public int lineVertexCount;
    public int firstLineVertex;
    public int lastLineVertex;    
    public float[] lineVertices;
    public int[] lineColors;
    public float[] lineDirWidths;    
    
    public int lineIndexCount;
    public int firstLineIndex;
    public int lastLineIndex;  
    public short[] lineIndices;  
    
    // Tessellated point data
    public int pointVertexCount;
    public int firstPointVertex;
    public int lastPointVertex;    
    public float[] pointVertices;
    public int[] pointColors;
    public float[] pointSizes;  

    public int pointIndexCount;
    public int firstPointIndex;
    public int lastPointIndex;  
    public short[] pointIndices;
    
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
      fillVertices = new float[3 * PGL.DEFAULT_TESS_VERTICES];
      fillColors = new int[PGL.DEFAULT_TESS_VERTICES];
      fillNormals = new float[3 * PGL.DEFAULT_TESS_VERTICES];
      fillTexcoords = new float[2 * PGL.DEFAULT_TESS_VERTICES];
      fillAmbient = new int[PGL.DEFAULT_TESS_VERTICES];
      fillSpecular = new int[PGL.DEFAULT_TESS_VERTICES];
      fillEmissive = new int[PGL.DEFAULT_TESS_VERTICES];
      fillShininess = new float[PGL.DEFAULT_TESS_VERTICES];      
      fillIndices = new short[PGL.DEFAULT_TESS_VERTICES];        
      
      lineVertices = new float[3 * PGL.DEFAULT_TESS_VERTICES];
      lineColors = new int[PGL.DEFAULT_TESS_VERTICES];
      lineDirWidths = new float[4 * PGL.DEFAULT_TESS_VERTICES];
      lineIndices = new short[PGL.DEFAULT_TESS_VERTICES];       
      
      pointVertices = new float[3 * PGL.DEFAULT_TESS_VERTICES];
      pointColors = new int[PGL.DEFAULT_TESS_VERTICES];
      pointSizes = new float[2 * PGL.DEFAULT_TESS_VERTICES];
      pointIndices = new short[PGL.DEFAULT_TESS_VERTICES];
      
      reset();
    }
    
    public void trim() {
      if (fillVertexCount < fillVertices.length / 3) {
        trimFillVertices();
        trimFillColors();
        trimFillNormals();
        trimFillTexcoords();
        trimFillAmbient();
        trimFillSpecular();
        trimFillEmissive();
        trimFillShininess();
      }
      
      if (fillIndexCount < fillIndices.length) {
        trimFillIndices();  
      }
            
      if (lineVertexCount < lineVertices.length / 3) {
        trimLineVertices();
        trimLineColors();
        trimLineAttributes();
      }
      
      if (lineIndexCount < lineIndices.length) {
        trimLineIndices();  
      }
      
      if (pointVertexCount < pointVertices.length / 3) {
        trimPointVertices();
        trimPointColors();
        trimPointAttributes();
      }
      
      if (pointIndexCount < pointIndices.length) {
        trimPointIndices();  
      }       
    }    
    
    protected void trimFillVertices() {
      float temp[] = new float[3 * fillVertexCount];      
      PApplet.arrayCopy(fillVertices, 0, temp, 0, 3 * fillVertexCount);
      fillVertices = temp;       
    }

    protected void trimFillColors() {
      int temp[] = new int[fillVertexCount];      
      PApplet.arrayCopy(fillColors, 0, temp, 0, fillVertexCount);
      fillColors = temp;
    }
    
    protected void trimFillNormals() {
      float temp[] = new float[3 * fillVertexCount];      
      PApplet.arrayCopy(fillNormals, 0, temp, 0, 3 * fillVertexCount);
      fillNormals = temp;       
    }
    
    protected void trimFillTexcoords() {
      float temp[] = new float[2 * fillVertexCount];      
      PApplet.arrayCopy(fillTexcoords, 0, temp, 0, 2 * fillVertexCount);
      fillTexcoords = temp;
    }
    
    protected void trimFillAmbient() {
      int temp[] = new int[fillVertexCount];
      PApplet.arrayCopy(fillAmbient, 0, temp, 0, fillVertexCount);
      fillAmbient = temp;      
    }
    
    protected void trimFillSpecular() {
      int temp[] = new int[fillVertexCount];
      PApplet.arrayCopy(fillSpecular, 0, temp, 0, fillVertexCount);
      fillSpecular = temp;      
    }
        
    protected void trimFillEmissive() {
      int temp[] = new int[fillVertexCount];
      PApplet.arrayCopy(fillEmissive, 0, temp, 0, fillVertexCount);
      fillEmissive = temp; 
    }
    
    protected void trimFillShininess() {
      float temp[] = new float[fillVertexCount];
      PApplet.arrayCopy(fillShininess, 0, temp, 0, fillVertexCount);
      fillShininess = temp;      
    }
    
    public void trimFillIndices() {
      short temp[] = new short[fillIndexCount];      
      PApplet.arrayCopy(fillIndices, 0, temp, 0, fillIndexCount);
      fillIndices = temp;      
    }    
    
    protected void trimLineVertices() {
      float temp[] = new float[3 * lineVertexCount];      
      PApplet.arrayCopy(lineVertices, 0, temp, 0, 3 * lineVertexCount);
      lineVertices = temp;  
    }
    
    protected void trimLineColors() {
      int temp[] = new int[lineVertexCount];      
      PApplet.arrayCopy(lineColors, 0, temp, 0, lineVertexCount);
      lineColors = temp;      
    }
    
    protected void trimLineAttributes() {
      float temp[] = new float[4 * lineVertexCount];      
      PApplet.arrayCopy(lineDirWidths, 0, temp, 0, 4 * lineVertexCount);
      lineDirWidths = temp;      
    }      
    
    protected void trimLineIndices() {
      short temp[] = new short[lineVertexCount];      
      PApplet.arrayCopy(lineIndices, 0, temp, 0, lineIndexCount);
      lineIndices = temp;        
    }    
    
    protected void trimPointVertices() {
      float temp[] = new float[3 * pointVertexCount];      
      PApplet.arrayCopy(pointVertices, 0, temp, 0, 3 * pointVertexCount);
      pointVertices = temp;  
    }
    
    protected void trimPointColors() {
      int temp[] = new int[pointVertexCount];      
      PApplet.arrayCopy(pointColors, 0, temp, 0, pointVertexCount);
      pointColors = temp;      
    }
    
    protected void trimPointAttributes() {
      float temp[] = new float[2 * pointVertexCount];      
      PApplet.arrayCopy(pointSizes, 0, temp, 0, 2 * pointVertexCount);
      pointSizes = temp;      
    }
    
    protected void trimPointIndices() {
      short temp[] = new short[pointIndexCount];      
      PApplet.arrayCopy(pointIndices, 0, temp, 0, pointIndexCount);
      pointIndices = temp;        
    }    
    
    public void dipose() {
      fillVertices = null;
      fillColors = null;
      fillNormals = null;
      fillTexcoords = null;
      fillAmbient = null;
      fillSpecular = null;
      fillEmissive = null;
      fillShininess = null;      
      fillIndices = null;        
      
      lineVertices = null;
      lineColors = null;
      lineDirWidths = null;
      lineIndices = null;       
      
      pointVertices = null;
      pointColors = null;
      pointSizes = null;
      pointIndices = null;
    }
    
    public boolean isFull() {
      return PGL.MAX_TESS_VERTICES <= fillVertexCount || 
             PGL.MAX_TESS_VERTICES <= lineVertexCount ||
             PGL.MAX_TESS_VERTICES <= pointVertexCount ||
             PGL.MAX_TESS_INDICES  <= fillIndexCount ||
             PGL.MAX_TESS_INDICES  <= fillIndexCount ||
             PGL.MAX_TESS_INDICES  <= fillIndexCount;
    }

    public boolean isOverflow() {
      return PGL.MAX_TESS_VERTICES < fillVertexCount || 
             PGL.MAX_TESS_VERTICES < lineVertexCount ||
             PGL.MAX_TESS_VERTICES < pointVertexCount ||
             PGL.MAX_TESS_INDICES  < fillIndexCount ||
             PGL.MAX_TESS_INDICES  < fillIndexCount ||
             PGL.MAX_TESS_INDICES  < fillIndexCount;
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
    
    public int setFillIndex(int voffset, int ioffset) {
      firstFillIndex = 0;
      if (0 < ioffset) {
        firstFillIndex = ioffset + 1; 
      }
      
      if (0 < voffset) {
        // The indices are update to take into account all the previous 
        // shapes in the hierarchy, as the entire geometry will be stored
        // contiguously in a single VBO in the root node.
        for (int i = 0; i < fillIndexCount; i++) {
          fillIndices[i] += voffset;
        }
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
    
    public int setLineIndex(int voffset, int ioffset) {      
      firstLineIndex = 0;
      if (0 < ioffset) {
        firstLineIndex = ioffset + 1; 
      }        
      
      if (0 < voffset) {
        // The indices are update to take into account all the previous 
        // shapes in the hierarchy, as the entire geometry will be stored
        // contiguously in a single VBO in the root node.
        for (int i = 0; i < lineIndexCount; i++) {
          lineIndices[i] += firstLineVertex;
        }
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
    
    public int setPointIndex(int voffset, int ioffset) { 
      firstPointIndex = 0;
      if (0 < ioffset) {
        firstPointIndex = ioffset + 1; 
      }        
      
      if (0 < voffset) {
        // The indices are update to take into account all the previous 
        // shapes in the hierarchy, as the entire geometry will be stored
        // contiguously in a single VBO in the root node.
        for (int i = 0; i < pointIndexCount; i++) {
          pointIndices[i] += firstPointVertex;
        }        
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
      short temp[] = new short[n];      
      PApplet.arrayCopy(fillIndices, 0, temp, 0, fillIndexCount);
      fillIndices = temp;      
    }
    
    public void addFillIndex(int idx) {
      fillIndexCheck();
      fillIndices[fillIndexCount] = PGL.makeIndex(idx);
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
        expandFillAmbient(newSize);
        expandFillSpecular(newSize);
        expandFillEmissive(newSize);
        expandFillShininess(newSize);
      }
    }
    
    public void addFillVertices(int count) {
      int oldSize = fillVertices.length / 3;
      if (fillVertexCount + count > oldSize) {
        int newSize = expandVertSize(oldSize, fillVertexCount + count); 
                
        expandFillVertices(newSize);
        expandFillColors(newSize);
        expandFillNormals(newSize);
        expandFillTexcoords(newSize);
        expandFillAmbient(newSize);
        expandFillSpecular(newSize);
        expandFillEmissive(newSize);
        expandFillShininess(newSize);
      }
                  
      firstFillVertex = fillVertexCount;
      fillVertexCount += count;
      lastFillVertex = fillVertexCount - 1;
    }
    
    public void addFillIndices(int count) {
      int oldSize = fillIndices.length;
      if (fillIndexCount + count > oldSize) {
        int newSize = expandIndSize(oldSize, fillIndexCount + count);    
        
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
      int temp[] = new int[n];      
      PApplet.arrayCopy(fillColors, 0, temp, 0, fillVertexCount);
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
    
    protected void expandFillAmbient(int n) {
      int temp[] = new int[n];      
      PApplet.arrayCopy(fillAmbient, 0, temp, 0, fillVertexCount);
      fillAmbient = temp;          
    }
    
    protected void expandFillSpecular(int n) {
      int temp[] = new int[n];      
      PApplet.arrayCopy(fillSpecular, 0, temp, 0, fillVertexCount);
      fillSpecular = temp;       
    }
    
    protected void expandFillEmissive(int n) {
      int temp[] = new int[n];      
      PApplet.arrayCopy(fillEmissive, 0, temp, 0, fillVertexCount);
      fillEmissive = temp;      
    }
    
    protected void expandFillShininess(int n) {
      float temp[] = new float[n];      
      PApplet.arrayCopy(fillShininess, 0, temp, 0, fillVertexCount);
      fillShininess = temp;       
    }    
    
    public void addLineVertices(int count) {
      int oldSize = lineVertices.length / 3;
      if (lineVertexCount + count > oldSize) {
        int newSize = expandVertSize(oldSize, lineVertexCount + count);
        
        expandLineVertices(newSize);
        expandLineColors(newSize);
        expandLineAttributes(newSize);
      }
      
      firstLineVertex = lineVertexCount;
      lineVertexCount += count;            
      lastLineVertex = lineVertexCount - 1;
    }

    protected void expandLineVertices(int n) {
      float temp[] = new float[3 * n];      
      PApplet.arrayCopy(lineVertices, 0, temp, 0, 3 * lineVertexCount);
      lineVertices = temp;  
    }
    
    protected void expandLineColors(int n) {
      int temp[] = new int[n];      
      PApplet.arrayCopy(lineColors, 0, temp, 0, lineVertexCount);
      lineColors = temp;      
    }
    
    protected void expandLineAttributes(int n) {
      float temp[] = new float[4 * n];      
      PApplet.arrayCopy(lineDirWidths, 0, temp, 0, 4 * lineVertexCount);
      lineDirWidths = temp;      
    }      
    
    public void addLineIndices(int count) {
      int oldSize = lineIndices.length;
      if (lineIndexCount + count > oldSize) {
        int newSize = expandIndSize(oldSize, lineIndexCount + count);
        
        expandLineIndices(newSize);
      }
     
      firstLineIndex = lineIndexCount;
      lineIndexCount += count;      
      lastLineIndex = lineIndexCount - 1;   
    }   
    
    protected void expandLineIndices(int n) {
      short temp[] = new short[n];      
      PApplet.arrayCopy(lineIndices, 0, temp, 0, lineIndexCount);
      lineIndices = temp;        
    }
    
    public void addPointVertices(int count) {
      int oldSize = pointVertices.length / 3;
      if (pointVertexCount + count > oldSize) {
        int newSize = expandVertSize(oldSize, pointVertexCount + count);
        
        expandPointVertices(newSize);
        expandPointColors(newSize);
        expandPointAttributes(newSize);
      }
      
      firstPointVertex = pointVertexCount;
      pointVertexCount += count;      
      lastPointVertex = pointVertexCount - 1;
    }

    protected void expandPointVertices(int n) {
      float temp[] = new float[3 * n];      
      PApplet.arrayCopy(pointVertices, 0, temp, 0, 3 * pointVertexCount);
      pointVertices = temp;  
    }
    
    protected void expandPointColors(int n) {
      int temp[] = new int[n];      
      PApplet.arrayCopy(pointColors, 0, temp, 0, pointVertexCount);
      pointColors = temp;      
    }
    
    protected void expandPointAttributes(int n) {
      float temp[] = new float[2 * n];      
      PApplet.arrayCopy(pointSizes, 0, temp, 0, 2 * pointVertexCount);
      pointSizes = temp;      
    }
    
    public void addPointIndices(int count) {
      int oldSize = pointIndices.length;
      if (pointIndexCount + count > oldSize) {
        int newSize = expandIndSize(oldSize, pointIndexCount + count);
        
        expandPointIndices(newSize);
      }
     
      firstPointIndex = pointIndexCount;
      pointIndexCount += count;      
      lastPointIndex = pointIndexCount - 1;   
    }   
    
    protected void expandPointIndices(int n) {
      short temp[] = new short[n];      
      PApplet.arrayCopy(pointIndices, 0, temp, 0, pointIndexCount);
      pointIndices = temp;        
    }
    
    public void addFillVertex(float x, float y, float z, 
                              int rgba,
                              float nx, float ny, float nz, 
                              float u, float v, 
                              int am, int sp, int em, float shine) {
      fillVertexCheck();
      int index;
      
      if (renderMode == IMMEDIATE && flushMode == FLUSH_WHEN_FULL && !hints[DISABLE_TRANSFORM_CACHE]) {
        PMatrix3D mm = modelview;
        PMatrix3D nm = modelviewInv;
        
        index = 3 * fillVertexCount;
        fillVertices[index++] = x * mm.m00 + y * mm.m01 + z * mm.m02 + mm.m03;
        fillVertices[index++] = x * mm.m10 + y * mm.m11 + z * mm.m12 + mm.m13;
        fillVertices[index  ] = x * mm.m20 + y * mm.m21 + z * mm.m22 + mm.m23;
        
        index = 3 * fillVertexCount;
        fillNormals[index++] = nx * nm.m00 + ny * nm.m10 + nz * nm.m02;
        fillNormals[index++] = nx * nm.m01 + ny * nm.m11 + nz * nm.m12;
        fillNormals[index  ] = nx * nm.m02 + ny * nm.m21 + nz * nm.m22;
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
      
      fillColors[fillVertexCount] = rgba;
      
      index = 2 * fillVertexCount;
      fillTexcoords[index++] = u;
      fillTexcoords[index  ] = v;      
      
      fillAmbient[fillVertexCount] = am;
      fillSpecular[fillVertexCount] = sp;
      fillEmissive[fillVertexCount] = em;
      fillShininess[fillVertexCount] = shine;
      
      fillVertexCount++;
    }    

    public void addFillVertices(InGeometry in) {
      int index;
      int i0 = in.firstVertex;
      int i1 = in.lastVertex;
      int nvert = i1 - i0 + 1;
      
      addFillVertices(nvert);
      
      if (renderMode == IMMEDIATE && flushMode == FLUSH_WHEN_FULL && !hints[DISABLE_TRANSFORM_CACHE]) {
        PMatrix3D mm = modelview;
        PMatrix3D nm = modelviewInv;
        
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
          fillVertices[index++] = x * mm.m00 + y * mm.m01 + z * mm.m02 + mm.m03;
          fillVertices[index++] = x * mm.m10 + y * mm.m11 + z * mm.m12 + mm.m13;
          fillVertices[index  ] = x * mm.m20 + y * mm.m21 + z * mm.m22 + mm.m23;
          
          index = 3 * tessIdx;
          fillNormals[index++] = nx * nm.m00 + ny * nm.m10 + nz * nm.m20;
          fillNormals[index++] = nx * nm.m01 + ny * nm.m11 + nz * nm.m21;
          fillNormals[index  ] = nx * nm.m02 + ny * nm.m12 + nz * nm.m22;
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

          index = 2 * inIdx;
          float u = in.texcoords[index++];
          float v = in.texcoords[index  ];
          
          fillColors[tessIdx] = in.colors[inIdx];
          
          index = 2 * tessIdx;
          fillTexcoords[index++] = u;
          fillTexcoords[index  ] = v;
          
          fillAmbient[tessIdx] = in.ambient[inIdx];
          fillSpecular[tessIdx] = in.specular[inIdx];
          fillEmissive[tessIdx] = in.emissive[inIdx];
          fillShininess[tessIdx] = in.shininess[inIdx];          
        }
      } else {
        PApplet.arrayCopy(in.colors, i0, fillColors, firstFillVertex, nvert);      
        PApplet.arrayCopy(in.texcoords, 2 * i0, fillTexcoords, 2 * firstFillVertex, 2 * nvert);        
        PApplet.arrayCopy(in.ambient, i0, fillAmbient, firstFillVertex, nvert);
        PApplet.arrayCopy(in.specular, i0, fillSpecular, firstFillVertex, nvert);
        PApplet.arrayCopy(in.emissive, i0, fillEmissive, firstFillVertex, nvert);
        PApplet.arrayCopy(in.shininess, i0, fillShininess, firstFillVertex, nvert);        
      }
    }     
    
    public void putLineVertex(InGeometry in, int inIdx0, int inIdx1, int tessIdx, int rgba) {
      int index;

      index = 3 * inIdx0;
      float x0 = in.vertices[index++];
      float y0 = in.vertices[index++];
      float z0 = in.vertices[index  ];
      
      index = 3 * inIdx1;
      float x1 = in.vertices[index++];
      float y1 = in.vertices[index++];
      float z1 = in.vertices[index  ];        
      
      if (renderMode == IMMEDIATE && flushMode == FLUSH_WHEN_FULL && !hints[DISABLE_TRANSFORM_CACHE]) {
        PMatrix3D mm = modelview;
        
        index = 3 * tessIdx;
        lineVertices[index++] = x0 * mm.m00 + y0 * mm.m01 + z0 * mm.m02 + mm.m03;
        lineVertices[index++] = x0 * mm.m10 + y0 * mm.m11 + z0 * mm.m12 + mm.m13;
        lineVertices[index  ] = x0 * mm.m20 + y0 * mm.m21 + z0 * mm.m22 + mm.m23;
        
        index = 4 * tessIdx;
        lineDirWidths[index++] = x1 * mm.m00 + y1 * mm.m01 + z1 * mm.m02 + mm.m03;
        lineDirWidths[index++] = x1 * mm.m10 + y1 * mm.m11 + z1 * mm.m12 + mm.m13;
        lineDirWidths[index  ] = x1 * mm.m20 + y1 * mm.m21 + z1 * mm.m22 + mm.m23;        
      } else {
        index = 3 * tessIdx;
        lineVertices[index++] = x0;
        lineVertices[index++] = y0;
        lineVertices[index  ] = z0;
        
        index = 4 * tessIdx;
        lineDirWidths[index++] = x1;
        lineDirWidths[index++] = y1;
        lineDirWidths[index  ] = z1;
      }      
      
      lineColors[tessIdx] = rgba;
    }

    public void putLineVertex(InGeometry in, int inIdx0, int inIdx1, int tessIdx) {      
      putLineVertex(in, inIdx0, inIdx1, tessIdx, in.scolors[inIdx0]);
    }        
    
    
    public void putPointVertex(InGeometry in, int inIdx, int tessIdx) {
      int index;

      index = 3 * inIdx;
      float x = in.vertices[index++];
      float y = in.vertices[index++];
      float z = in.vertices[index ];
      
      if (renderMode == IMMEDIATE && flushMode == FLUSH_WHEN_FULL && !hints[DISABLE_TRANSFORM_CACHE]) {
        PMatrix3D mm = modelview;
        
        index = 3 * tessIdx;
        pointVertices[index++] = x * mm.m00 + y * mm.m01 + z * mm.m02 + mm.m03;
        pointVertices[index++] = x * mm.m10 + y * mm.m11 + z * mm.m12 + mm.m13;
        pointVertices[index  ] = x * mm.m20 + y * mm.m21 + z * mm.m22 + mm.m23;        
      } else {
        index = 3 * tessIdx;
        pointVertices[index++] = x;
        pointVertices[index++] = y;
        pointVertices[index  ] = z;
      }      
      
      pointColors[tessIdx] = in.scolors[inIdx];
    }
    
    public int expandVertSize(int currSize, int newMinSize) {
      int newSize = currSize; 
      while (newSize < newMinSize) {
        newSize <<= 1;        
      }
      return newSize;
    }

    public int expandIndSize(int currSize, int newMinSize) {
      int newSize = currSize; 
      while (newSize < newMinSize) {
        newSize <<= 1;
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
          lineDirWidths[index++] += tx;
          lineDirWidths[index  ] += ty;           
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
          lineDirWidths[index++] += tx;
          lineDirWidths[index++] += ty;
          lineDirWidths[index  ] += tz;           
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
        
          index = 4 * i;
          float xa = lineDirWidths[index++];
          float ya = lineDirWidths[index  ];
                    
          index = 3 * i;
          lineVertices[index++] = x * tr.m00 + y * tr.m01 + tr.m02;
          lineVertices[index  ] = x * tr.m10 + y * tr.m11 + tr.m12;
        
          index = 4 * i;
          lineDirWidths[index++] = xa * tr.m00 + ya * tr.m01 + tr.m02;
          lineDirWidths[index  ] = xa * tr.m10 + ya * tr.m11 + tr.m12;              
        }   
      }      
      
      if (0 < pointVertexCount) {
        int index;
       
        for (int i = 0; i < pointVertexCount; i++) {
          index = 3 * i;
          float x = pointVertices[index++];
          float y = pointVertices[index  ];
        
          index = 3 * i;
          pointVertices[index++] = x * tr.m00 + y * tr.m01 + tr.m02;
          pointVertices[index  ] = x * tr.m10 + y * tr.m11 + tr.m12;
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
        
          index = 4 * i;
          float xa = lineDirWidths[index++];
          float ya = lineDirWidths[index++];
          float za = lineDirWidths[index  ];
                    
          index = 3 * i;
          lineVertices[index++] = x * tr.m00 + y * tr.m01 + z * tr.m02 + tr.m03;
          lineVertices[index++] = x * tr.m10 + y * tr.m11 + z * tr.m12 + tr.m13;
          lineVertices[index  ] = x * tr.m20 + y * tr.m21 + z * tr.m22 + tr.m23;
        
          index = 4 * i;
          lineDirWidths[index++] = xa * tr.m00 + ya * tr.m01 + za * tr.m02 + tr.m03;
          lineDirWidths[index++] = xa * tr.m10 + ya * tr.m11 + za * tr.m12 + tr.m13;
          lineDirWidths[index  ] = xa * tr.m20 + ya * tr.m21 + za * tr.m22 + tr.m23;              
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
          pointVertices[index++] = x * tr.m00 + y * tr.m01 + z * tr.m02 + tr.m03;
          pointVertices[index++] = x * tr.m10 + y * tr.m11 + z * tr.m12 + tr.m13;
          pointVertices[index  ] = x * tr.m20 + y * tr.m21 + z * tr.m22 + tr.m23;
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
  
  // Generates tessellated geometry given a batch of input vertices.  
  public class Tessellator {    
    InGeometry in; 
    TessGeometry tess;
    PGL.Tessellator gluTess;
    TessellatorCallback callback;
    
    boolean fill;
    boolean stroke;
    float strokeWeight;
    int strokeJoin;
    int strokeCap;
    int bezierDetil = 20;
    
    public Tessellator() {
      callback = new TessellatorCallback();
      gluTess = pgl.createTessellator(callback);
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
     
        checkForFlush(tess.lineVertexCount + nvertTot, tess.lineIndexCount + nindTot);
        
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
          tess.pointSizes[2 * attribIdx + 0] = 0;
          tess.pointSizes[2 * attribIdx + 1] = 0;
          attribIdx++;
          float val = 0;
          float inc = (float) SINCOS_LENGTH / perim;      
          for (int k = 0; k < perim; k++) {
            tess.pointSizes[2 * attribIdx + 0] = 0.5f * cosLUT[(int) val] * strokeWeight;
            tess.pointSizes[2 * attribIdx + 1] = 0.5f * sinLUT[(int) val] * strokeWeight;
            val = (val + inc) % SINCOS_LENGTH;                
            attribIdx++;           
          }
          
          // Adding vert0 to take into account the triangles of all
          // the preceding points.
          for (int k = 1; k < nvert - 1; k++) {
            tess.pointIndices[indIdx++] = PGL.makeIndex(firstVert + 0);
            tess.pointIndices[indIdx++] = PGL.makeIndex(firstVert + k);
            tess.pointIndices[indIdx++] = PGL.makeIndex(firstVert + k + 1);
          }
          // Final triangle between the last and first point:
          tess.pointIndices[indIdx++] = PGL.makeIndex(firstVert + 0);
          tess.pointIndices[indIdx++] = PGL.makeIndex(firstVert + 1);
          tess.pointIndices[indIdx++] = PGL.makeIndex(firstVert + nvert - 1);      
          
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
        
        checkForFlush(tess.lineVertexCount + nvertTot, tess.lineIndexCount + nindTot);     
        
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
          tess.pointSizes[2 * attribIdx + 0] = 0;
          tess.pointSizes[2 * attribIdx + 1] = 0;
          attribIdx++;            
          for (int k = 0; k < 4; k++) {
            tess.pointSizes[2 * attribIdx + 0] = 0.5f * QUAD_SIGNS[k][0] * strokeWeight;
            tess.pointSizes[2 * attribIdx + 1] = 0.5f * QUAD_SIGNS[k][1] * strokeWeight;               
            attribIdx++;           
          }
          
          // Adding firstVert to take into account the triangles of all
          // the preceding points.
          for (int k = 1; k < nvert - 1; k++) {
            tess.pointIndices[indIdx++] = PGL.makeIndex(firstVert + 0);
            tess.pointIndices[indIdx++] = PGL.makeIndex(firstVert + k);
            tess.pointIndices[indIdx++] = PGL.makeIndex(firstVert + k + 1);
          }
          // Final triangle between the last and first point:
          tess.pointIndices[indIdx++] = PGL.makeIndex(firstVert + 0);
          tess.pointIndices[indIdx++] = PGL.makeIndex(firstVert + 1);
          tess.pointIndices[indIdx++] = PGL.makeIndex(firstVert + nvert - 1);  
          
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

        checkForFlush(tess.lineVertexCount + nvert, tess.lineIndexCount + nvert);        
        
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
        int nInInd = nInVert;
        checkForFlush(tess.fillVertexCount + nInVert, tess.fillIndexCount + nInInd);
        
        tess.addFillVertices(in);
        
        tess.addFillIndices(nInVert);
        int idx0 = tess.firstFillIndex;
        int offset = tess.firstFillVertex;
        for (int i = in.firstVertex; i <= in.lastVertex; i++) {
          tess.fillIndices[idx0 + i] = PGL.makeIndex(offset + i);
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
        int nInInd = 3 * (nInVert - 2); 
        checkForFlush(tess.fillVertexCount + nInVert, tess.fillIndexCount + nInInd);
        
        tess.addFillVertices(in);

        tess.addFillIndices(nInInd);
        int idx = tess.firstFillIndex;
        int offset = tess.firstFillVertex; 
        for (int i = in.firstVertex + 1; i < in.lastVertex; i++) {
          tess.fillIndices[idx++] = PGL.makeIndex(offset + in.firstVertex);
          tess.fillIndices[idx++] = PGL.makeIndex(offset + i);
          tess.fillIndices[idx++] = PGL.makeIndex(offset + i + 1);
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
        int triCount = nInVert - 2;
        int nInInd = 3 * triCount;
        
        checkForFlush(tess.fillVertexCount + nInVert, tess.fillIndexCount + nInInd);
        
        tess.addFillVertices(in);
        
        // Each vertex, except the first and last, defines a triangle.
        tess.addFillIndices(nInInd);
        int idx = tess.firstFillIndex;
        int offset = tess.firstFillVertex;
        for (int i = in.firstVertex + 1; i < in.lastVertex; i++) {
          tess.fillIndices[idx++] = PGL.makeIndex(offset + i);
          if (i % 2 == 0) {
            tess.fillIndices[idx++] = PGL.makeIndex(offset + i - 1);  
            tess.fillIndices[idx++] = PGL.makeIndex(offset + i + 1);
          } else {
            tess.fillIndices[idx++] = PGL.makeIndex(offset + i + 1);  
            tess.fillIndices[idx++] = PGL.makeIndex(offset + i - 1);
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
        int quadCount = nInVert / 4;
        int nInInd = 6 * quadCount;
        
        checkForFlush(tess.fillVertexCount + nInVert, tess.fillIndexCount + nInInd);
        tess.addFillVertices(in);
        
        tess.addFillIndices(nInInd);
        int idx = tess.firstFillIndex;
        int offset = tess.firstFillVertex; 
        for (int qd = 0; qd < quadCount; qd++) {        
          int i0 = offset + 4 * qd + 0;
          int i1 = offset + 4 * qd + 1;
          int i2 = offset + 4 * qd + 2;
          int i3 = offset + 4 * qd + 3;
          
          tess.fillIndices[idx++] = PGL.makeIndex(i0);
          tess.fillIndices[idx++] = PGL.makeIndex(i1);
          tess.fillIndices[idx++] = PGL.makeIndex(i3);
          
          tess.fillIndices[idx++] = PGL.makeIndex(i1);
          tess.fillIndices[idx++] = PGL.makeIndex(i2);
          tess.fillIndices[idx++] = PGL.makeIndex(i3);
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
        int quadCount = nInVert / 2 - 1;
        int nInInd = 6 * quadCount;
        
        checkForFlush(tess.fillVertexCount + nInVert, tess.fillIndexCount + nInInd);
        tess.addFillVertices(in);        
        
        tess.addFillIndices(nInInd);
        int idx = tess.firstFillIndex;
        int offset = tess.firstFillVertex; 
        for (int qd = 1; qd < nInVert / 2; qd++) {        
          int i0 = offset + 2 * (qd - 1);
          int i1 = offset + 2 * (qd - 1) + 1;
          int i2 = offset + 2 * qd + 1;
          int i3 = offset + 2 * qd;      
          
          tess.fillIndices[idx++] = PGL.makeIndex(i0);
          tess.fillIndices[idx++] = PGL.makeIndex(i1);
          tess.fillIndices[idx++] = PGL.makeIndex(i3);
          
          tess.fillIndices[idx++] = PGL.makeIndex(i1);
          tess.fillIndices[idx++] = PGL.makeIndex(i2);
          tess.fillIndices[idx++] = PGL.makeIndex(i3);
        }              
      }
 
      if (stroke) {
        tess.isStroked = true;
        tessellateEdges();
      }
    }  
    
    public void tessellatePolygon(boolean solid, boolean closed, boolean calcNormals) {
      int nInVert = in.lastVertex - in.firstVertex + 1;
      
      callback.calcNormals = calcNormals;
      
      if (fill && 3 <= nInVert) {
        checkForFlush(nInVert);
        
        gluTess.beginPolygon();
        
        if (solid) {
          // Using NONZERO winding rule for solid polygons.
          gluTess.setWindingRule(PGL.GLU_TESS_WINDING_NONZERO);          
        } else {
          // Using ODD winding rule to generate polygon with holes.
          gluTess.setWindingRule(PGL.GLU_TESS_WINDING_ODD);
        }

        gluTess.beginContour();    
        
        // Now, iterate over all input data and send to GLU tessellator..
        for (int i = in.firstVertex; i <= in.lastVertex; i++) {
          boolean breakPt = in.codes[i] == PShape.BREAK;      
          if (breakPt) {
            gluTess.endContour();
            gluTess.beginContour();
          }
          
          // Vertex data includes coordinates, colors, normals, texture coordinates, and material properties.
          double[] vertex = new double[] { in.vertices [3 * i + 0], in.vertices [3 * i + 1], in.vertices[3 * i + 2],
                                           in.colors   [4 * i + 0], in.colors   [4 * i + 1], in.colors  [4 * i + 2], in.colors[4 * i + 3],
                                           in.normals  [3 * i + 0], in.normals  [3 * i + 1], in.normals [3 * i + 2],
                                           in.texcoords[2 * i + 0], in.texcoords[2 * i + 1],
                                           in.ambient[i], in.specular[i], in.emissive[i], in.shininess[i] };
          gluTess.addVertex(vertex);
        }        
        gluTess.endContour();
        
        gluTess.endPolygon();
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
   
      tess.lineDirWidths[4 * vcount + 3] = +strokeWeight;
      tess.lineIndices[icount++] = PGL.makeIndex(vcount);
      
      vcount++;
      tess.putLineVertex(in, i0, i1, vcount);
      tess.lineDirWidths[4 * vcount + 3] = -strokeWeight;
      tess.lineIndices[icount++] = PGL.makeIndex(vcount);
      
      vcount++;
      tess.putLineVertex(in, i1, i0, vcount);
      tess.lineDirWidths[4 * vcount + 3] = -strokeWeight;
      tess.lineIndices[icount++] = PGL.makeIndex(vcount);
      
      // Starting a new triangle re-using prev vertices.
      tess.lineIndices[icount++] = PGL.makeIndex(vcount);
      tess.lineIndices[icount++] = PGL.makeIndex(vcount - 1);
      
      vcount++;
      tess.putLineVertex(in, i1, i0, vcount);      
      tess.lineDirWidths[4 * vcount + 3] = +strokeWeight;
      tess.lineIndices[icount++] = PGL.makeIndex(vcount);
    }
    
    public void tessellateEdges() {
      int nInVert = in.getNumLineVertices();
      int nInInd = in.getNumLineIndices();     
      
      checkForFlush(tess.lineVertexCount + nInVert, tess.lineIndexCount + nInInd);
      
      tess.addLineVertices(nInVert);
      tess.addLineIndices(nInInd);
      int vcount = tess.firstLineVertex;
      int icount = tess.firstLineIndex;          
      for (int i = in.firstEdge; i <= in.lastEdge; i++) {
        int[] edge = in.edges[i];
        addLine(edge[0], edge[1], vcount, icount); vcount += 4; icount += 6;
      }    
    }
    
    protected void checkForFlush(int vertCount) {
      if (tess.renderMode == IMMEDIATE && PGL.MAX_TESS_VERTICES < vertCount) {
        setLastTexIndex(tess.lastFillIndex);
        flush();
        setFirstTexIndex(0);
      }      
    }     
    
    protected void checkForFlush(int vertCount, int indCount) {
      if (tess.renderMode == IMMEDIATE && (PGL.MAX_TESS_VERTICES < vertCount ||  
                                           PGL.MAX_TESS_INDICES  < indCount)) {
        setLastTexIndex(tess.lastFillIndex);
        flush();
        setFirstTexIndex(0);
      }      
    }    
        
    protected boolean startEdge(int edge) {
      return edge % 2 != 0;
    }
    
    protected boolean endEdge(int edge) {
      return 1 < edge;
    }    
    
    protected class TessellatorCallback implements PGL.TessellatorCallback {
      public boolean calcNormals;
      protected int tessFirst;
      protected int tessCount;
      protected int tessType;
      
      public void begin(int type) {
        tessFirst = tess.fillVertexCount;
        tessCount = 0;
        
        switch (type) {
        case PGL.GL_TRIANGLE_FAN: 
          tessType = TRIANGLE_FAN;
          break;
        case PGL.GL_TRIANGLE_STRIP: 
          tessType = TRIANGLE_STRIP;
          break;
        case PGL.GL_TRIANGLES: 
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
        if (tess.fillVertexCount < PGL.MAX_TESS_INDICES) {
          tess.addFillIndex(tessFirst + tessIdx);
        } else {
          throw new RuntimeException("P3D: the tessellator is generating too many indices, reduce complexity of shape.");
        }
      }
      
      protected void calcTriNormal(int tessIdx0, int tessIdx1, int tessIdx2) {
        tess.calcFillNormal(tessFirst + tessIdx0, tessFirst + tessIdx1, tessFirst + tessIdx2);
      }
      
      public void vertex(Object data) {
        if (data instanceof double[]) {
          double[] d = (double[]) data;
          if (d.length < 13) {
            throw new RuntimeException("TessCallback vertex() data is not of length 13");
          }
          
          // We need to use separate rgba components for correct interpolation...
          
          if (tess.fillVertexCount < PGL.MAX_TESS_VERTICES) {
            tess.addFillVertex((float) d[0], (float) d[ 1], (float) d[ 2],
                               (int)   d[3],
                               (float) d[4], (float) d[ 5], (float) d[ 6],
                               (float) d[7], (float) d[ 8],
                               (int)   d[9], (int)   d[10], (int)   d[11], (float) d[12]);
            tessCount++;
          } else {
            throw new RuntimeException("P3D: the tessellator is generating too many vertices, reduce complexity of shape.");
          }          
          
        } else {
          throw new RuntimeException("TessCallback vertex() data not understood");
        }
      }

      public void error(int errnum) {
        String estring = pgl.getErrorString(errnum);
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
        double[] vertex = new double[13];
        vertex[0] = coords[0];
        vertex[1] = coords[1];
        vertex[2] = coords[2];

        // Here w e need to use separate rgba components for correct interpolation...
        
        // Calculating the rest of the vertex parameters (color,
        // normal, texcoords) as the linear combination of the 
        // combined vertices.
        for (int i = 3; i < 13; i++) {
          vertex[i] = 0;
          for (int j = 0; j < 4; j++) {
            double[] vertData = (double[])data[j];
            if (vertData != null) {
              if (i == 3 || 8 < i) {
                // Color data, needs to be split into rgba components
                // for interpolation.
                int colorj = (int) vertData[i];
                int xj = (colorj >> 24) & 0xFF;
                int yj = (colorj >> 16) & 0xFF;
                int zj = (colorj >>  8) & 0xFF; 
                int wj = (colorj >>  0) & 0xFF;

                int colori = (int) vertex[i];
                int xi = (colori >> 24) & 0xFF;
                int yi = (colori >> 16) & 0xFF;
                int zi = (colori >>  8) & 0xFF; 
                int wi = (colori >>  0) & 0xFF;
                
                xi += weight[j] * xj;
                yi += weight[j] * yj;
                zi += weight[j] * zj;
                wi += weight[j] * wj;                
                
                vertex[i] = (xi << 24) | (yi << 16) | (zi << 8) | wi;                
              } else {
                vertex[i] += weight[j] * vertData[i];
              }
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
