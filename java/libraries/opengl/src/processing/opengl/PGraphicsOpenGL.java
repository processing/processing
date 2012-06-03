/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2004-12 Ben Fry and Casey Reas

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

import processing.core.PApplet;
import processing.core.PFont;
import processing.core.PGraphics;
import processing.core.PImage;
import processing.core.PMatrix;
import processing.core.PMatrix2D;
import processing.core.PMatrix3D;
import processing.core.PShape;
import processing.core.PVector;

import java.net.URL;
import java.nio.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

/**
 * OpenGL renderer.
 *
 */
public class PGraphicsOpenGL extends PGraphics { 
  // shaders
  static public final int FLAT_SHADER    = 0;
  static public final int LIGHT_SHADER   = 1;
  static public final int TEXTURE_SHADER = 2;
  static public final int FULL_SHADER    = 3;
  static public final int LINE3D_SHADER  = 4;
  static public final int POINT3D_SHADER = 5;
  
  public int textureWrap    = Texture.CLAMP;
  public int textureQuality = Texture.BEST;
  
  /** Interface between Processing and OpenGL */
  public PGL pgl;

  /** The main PApplet renderer. */
  protected static PGraphicsOpenGL pgPrimary = null;
  
  /** The renderer currently in use. */
  protected static PGraphicsOpenGL pgCurrent = null;

  // ........................................................

  // Basic rendering parameters:

  /** Flush modes: continuously (geometry is flushed after each call to
   * endShape) when-full (geometry is accumulated until a maximum size is
   * reached.  */
  static protected final int FLUSH_CONTINUOUSLY = 0;
  static protected final int FLUSH_WHEN_FULL    = 1;     
  
  /** Type of geometry: immediate is that generated with beginShape/vertex/
   * endShape, retained is the result of creating a PShapeOpenGL object with
   * createShape. */
  static protected final int IMMEDIATE = 0;
  static protected final int RETAINED  = 1;

  /** Current flush mode. */
  protected int flushMode = FLUSH_WHEN_FULL;
  
  // ........................................................

  // VBOs for immediate rendering:

  public int glPolyVertexBufferID;
  public int glPolyColorBufferID;
  public int glPolyNormalBufferID;
  public int glPolyTexcoordBufferID;
  public int glPolyAmbientBufferID;
  public int glPolySpecularBufferID;
  public int glPolyEmissiveBufferID;
  public int glPolyShininessBufferID;
  public int glPolyIndexBufferID;
  protected boolean polyBuffersCreated = false;
  protected PGL.Context polyBuffersContext;

  public int glLineVertexBufferID;
  public int glLineColorBufferID;
  public int glLineAttribBufferID;
  public int glLineIndexBufferID;
  protected boolean lineBuffersCreated = false;
  protected PGL.Context lineBuffersContext;

  public int glPointVertexBufferID;
  public int glPointColorBufferID;
  public int glPointAttribBufferID;
  public int glPointIndexBufferID;
  protected boolean pointBuffersCreated = false;
  protected PGL.Context pointBuffersContext;

  protected static final int INIT_VERTEX_BUFFER_SIZE  = 256;
  protected static final int INIT_INDEX_BUFFER_SIZE   = 512;
  
  // ........................................................

  // GL parameters

  static protected boolean glParamsRead = false;

  /** Extensions used by Processing */
  static public boolean npotTexSupported;
  static public boolean autoMipmapGenSupported;
  static public boolean fboMultisampleSupported;
  static public boolean packedDepthStencilSupported;
  static public boolean blendEqSupported;

  /** Some hardware limits */
  static public int maxTextureSize;
  static public int maxSamples;
  static public float maxPointSize;
  static public float maxLineWidth;
  static public int depthBits;
  static public int stencilBits;

  /** OpenGL information strings */
  static public String OPENGL_VENDOR;
  static public String OPENGL_RENDERER;
  static public String OPENGL_VERSION;
  static public String OPENGL_EXTENSIONS;
  static public String GLSL_VERSION;

  // ........................................................

  // GL objects:

  static protected HashMap<GLResource, Boolean> glTextureObjects    = new HashMap<GLResource, Boolean>();
  static protected HashMap<GLResource, Boolean> glVertexBuffers     = new HashMap<GLResource, Boolean>();
  static protected HashMap<GLResource, Boolean> glFrameBuffers      = new HashMap<GLResource, Boolean>();
  static protected HashMap<GLResource, Boolean> glRenderBuffers     = new HashMap<GLResource, Boolean>();
  static protected HashMap<GLResource, Boolean> glslPrograms        = new HashMap<GLResource, Boolean>();
  static protected HashMap<GLResource, Boolean> glslVertexShaders   = new HashMap<GLResource, Boolean>();
  static protected HashMap<GLResource, Boolean> glslFragmentShaders = new HashMap<GLResource, Boolean>();

  // ........................................................

  // Shaders

  static protected URL defPolyFlatShaderVertURL  = PGraphicsOpenGL.class.getResource("PolyFlatShaderVert.glsl");
  static protected URL defPolyTexShaderVertURL   = PGraphicsOpenGL.class.getResource("PolyTexShaderVert.glsl");
  static protected URL defPolyLightShaderVertURL = PGraphicsOpenGL.class.getResource("PolyLightShaderVert.glsl");
  static protected URL defPolyFullShaderVertURL  = PGraphicsOpenGL.class.getResource("PolyFullShaderVert.glsl");
  static protected URL defPolyNoTexShaderFragURL = PGraphicsOpenGL.class.getResource("PolyNoTexShaderFrag.glsl");
  static protected URL defPolyTexShaderFragURL   = PGraphicsOpenGL.class.getResource("PolyTexShaderFrag.glsl");
  static protected URL defLineShaderVertURL      = PGraphicsOpenGL.class.getResource("LineShaderVert.glsl");
  static protected URL defLineShaderFragURL      = PGraphicsOpenGL.class.getResource("LineShaderFrag.glsl");
  static protected URL defPointShaderVertURL     = PGraphicsOpenGL.class.getResource("PointShaderVert.glsl");
  static protected URL defPointShaderFragURL     = PGraphicsOpenGL.class.getResource("PointShaderFrag.glsl");

  static protected PolyFlatShader defPolyFlatShader;
  static protected PolyTexShader defPolyTexShader;
  static protected PolyLightShader defPolyLightShader;
  static protected PolyFullShader defPolyFullShader;
  static protected LineShader defLineShader;
  static protected PointShader defPointShader;

  protected PolyFlatShader polyFlatShader;
  protected PolyTexShader polyTexShader;
  protected PolyLightShader polyLightShader;
  protected PolyFullShader polyFullShader;
  protected LineShader lineShader;
  protected PointShader pointShader;

  // ........................................................

  // Tessellator, geometry

  protected InGeometry inGeo;
  protected TessGeometry tessGeo;
  static protected Tessellator tessellator;
  protected TexCache texCache; 
  
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

  static protected Stack<FrameBuffer> fbStack;
  static protected FrameBuffer screenFramebuffer;
  static protected FrameBuffer currentFramebuffer;

  // .......................................................

  // Offscreen rendering:

  protected FrameBuffer offscreenFramebuffer;
  protected FrameBuffer offscreenFramebufferMultisample;
  protected boolean offscreenMultisample;

  protected boolean offscreenNotCurrent;

  // ........................................................

  // Screen surface:

  /** A handy reference to the PTexture bound to the drawing surface
   * (off or on-screen) */
  protected Texture texture;

  /** IntBuffer wrapping the pixels array. */
  protected IntBuffer pixelBuffer;

  /** Array to store pixels in OpenGL format. */
  protected int[] rgbaPixels;

  /** Flag to indicate if the user is manipulating the
   * pixels array through the set()/get() methods */
  protected boolean setgetPixels;

  // ........................................................

  // Utility variables:

  /** True if we are inside a beginDraw()/endDraw() block. */
  protected boolean drawing = false;

  /** Used to indicate an OpenGL surface recreation */
  protected boolean restoreSurface = false;
  
  /** Type of pixels operation. */
  static protected final int OP_NONE = 0;
  static protected final int OP_READ = 1;
  static protected final int OP_WRITE = 2;
  protected int pixelsOp = OP_NONE;

  /** Used to detect the occurrence of a frame resize event. */
  protected boolean resized = false;

  /** Viewport dimensions. */
  protected int[] viewport = {0, 0, 0, 0};

  /** Used to register calls to glClear. */
  protected boolean clearColorBuffer;
  protected boolean clearColorBuffer0;

  protected boolean openContour = false;
  protected boolean breakShape = false;
  protected boolean defaultEdges = false;
  protected PImage textureImage0;

  static protected final int EDGE_MIDDLE = 0;
  static protected final int EDGE_START  = 1;
  static protected final int EDGE_STOP   = 2;
  static protected final int EDGE_SINGLE = 3;
  
  protected boolean perspectiveCorrectedLines = false;

  /** Used in round point and ellipse tessellation. The
   * number of subdivisions per round point or ellipse is
   * calculated with the following formula: 
   * n = max(N, (TWO_PI * size / F))
   * where size is a measure of the dimensions of the circle
   * when projected on screen coordinates. F just sets the
   * minimum number of subdivisions, while a smaller F
   * would allow to have more detailed circles. 
   * N = MIN_POINT_ACCURACY
   * F = POINT_ACCURACY_FACTOR
   */ 
  final static protected int   MIN_POINT_ACCURACY    = 20;
  final static protected float POINT_ACCURACY_FACTOR = 10.0f;
  
  /** Used in quad point tessellation. */
  final protected float[][] QUAD_POINT_SIGNS = { {-1, +1}, {-1, -1}, {+1, -1}, {+1, +1} };
  

  //////////////////////////////////////////////////////////////

  // INIT/ALLOCATE/FINISH


  public PGraphicsOpenGL() {
    pgl = new PGL(this);
    

    if (tessellator == null) {
      tessellator = new Tessellator();
    }

    inGeo = newInGeometry(IMMEDIATE);
    tessGeo = newTessGeometry(IMMEDIATE);
    texCache = newTexCache();

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


  //public void setParent(PApplet parent)  // PGraphics


  public void setPrimary(boolean primary) {
    super.setPrimary(primary);
    format = ARGB;
  }


  //public void setPath(String path)  // PGraphics


  //public void setAntiAlias(int samples)  // PGraphics


  public void setFrameRate(float framerate) {
    pgl.setFramerate(framerate);
  }


  public void setSize(int iwidth, int iheight) {
    resized = (0 < width && width != iwidth) || (0 < height && height != iwidth);

    width = iwidth;
    height = iheight;
//    width1 = width - 1;
//    height1 = height - 1;

    allocate();
    reapplySettings();

    // init perspective projection based on new dimensions
    cameraFOV = 60 * DEG_TO_RAD; // at least for now
    cameraX = width / 2.0f;
    cameraY = height / 2.0f;
    cameraZ = cameraY / ((float) Math.tan(cameraFOV / 2.0f));
    cameraNear = cameraZ / 10.0f;
    cameraFar = cameraZ * 10.0f;
    cameraAspect = (float) width / (float) height;

    // set this flag so that beginDraw() will do an update to the camera.
    sizeChanged = true;

    // Forces a restart of OpenGL so the canvas has the right size.
    pgl.initialized = false;
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
  }


  public void dispose() { // PGraphics
    super.dispose();
    deleteFinalizedGLResources();
    deletePolyBuffers();
    deleteLineBuffers();
    deletePointBuffers();
  }

  
  protected void setFlushMode(int mode) {
    flushMode = mode;    
  }
  

  //////////////////////////////////////////////////////////////

  // RESOURCE HANDLING

  
  protected class GLResource {
    int id;
    int context;
    GLResource(int id, int context) {
      this.id = id;
      this.context = context;
    }
    
    public boolean equals(Object obj) {
      GLResource other = (GLResource)obj; 
      return other.id == id && other.context == context; 
    }
  }  
  

  // Texture Objects -------------------------------------------

  protected int createTextureObject(int context) {
    deleteFinalizedTextureObjects();

    int[] temp = new int[1];
    pgl.glGenTextures(1, temp, 0);
    int id = temp[0];

    GLResource res = new GLResource(id, context);
    
    if (glTextureObjects.containsKey(res)) {
      showWarning("Adding same texture twice");
    } else {
      glTextureObjects.put(res, false);
    }

    return id;
  }

  protected void deleteTextureObject(int id, int context) {
    GLResource res = new GLResource(id, context); 
    if (glTextureObjects.containsKey(res)) {
      int[] temp = { id };
      pgl.glDeleteTextures(1, temp, 0);
      glTextureObjects.remove(res);
    }
  }

  protected void deleteAllTextureObjects() {
    for (GLResource res : glTextureObjects.keySet()) {
      int[] temp = { res.id };
      pgl.glDeleteTextures(1, temp, 0);
    }
    glTextureObjects.clear();
  }

  // This is synchronized because it is called from the GC thread.
  synchronized protected void finalizeTextureObject(int id, int context) {
    GLResource res = new GLResource(id, context);
    if (glTextureObjects.containsKey(res)) {
      glTextureObjects.put(res, true);
    }
  }

  protected void deleteFinalizedTextureObjects() {
    Set<GLResource> finalized = new HashSet<GLResource>();

    for (GLResource res : glTextureObjects.keySet()) {
      if (glTextureObjects.get(res)) {
        finalized.add(res);
        int[] temp = { res.id };
        pgl.glDeleteTextures(1, temp, 0);
      }
    }

    for (GLResource res : finalized) {
      glTextureObjects.remove(res);
    }
  }
  
  protected void removeTextureObject(int id, int context) {
    GLResource res = new GLResource(id, context);
    if (glTextureObjects.containsKey(res)) {      
      glTextureObjects.remove(res);
    }    
  }  

  // Vertex Buffer Objects ----------------------------------------------

  protected int createVertexBufferObject(int context) {
    deleteFinalizedVertexBufferObjects();

    int[] temp = new int[1];
    pgl.glGenBuffers(1, temp, 0);
    int id = temp[0];
    
    GLResource res = new GLResource(id, context); 
    
    if (glVertexBuffers.containsKey(res)) {
      showWarning("Adding same VBO twice");
    } else {
      glVertexBuffers.put(res, false);
    }

    return id;
  }

  protected void deleteVertexBufferObject(int id, int context) {
    GLResource res = new GLResource(id, context); 
    if (glVertexBuffers.containsKey(res)) {
      int[] temp = { id };
      pgl.glDeleteBuffers(1, temp, 0);
      glVertexBuffers.remove(res);
    }
  }

  protected void deleteAllVertexBufferObjects() {
    for (GLResource res : glVertexBuffers.keySet()) {
      int[] temp = { res.id };
      pgl.glDeleteBuffers(1, temp, 0);
    }
    glVertexBuffers.clear();
  }

  // This is synchronized because it is called from the GC thread.
  synchronized protected void finalizeVertexBufferObject(int id, int context) {
    GLResource res = new GLResource(id, context); 
    if (glVertexBuffers.containsKey(res)) {
      glVertexBuffers.put(res, true);
    }
  }

  protected void deleteFinalizedVertexBufferObjects() {
    Set<GLResource> finalized = new HashSet<GLResource>();

    for (GLResource res : glVertexBuffers.keySet()) {
      if (glVertexBuffers.get(res)) {
        finalized.add(res);
        int[] temp = { res.id };
        pgl.glDeleteBuffers(1, temp, 0);
      }
    }

    for (GLResource res : finalized) {
      glVertexBuffers.remove(res);
    }
  }

  protected void removeVertexBufferObject(int id, int context) {
    GLResource res = new GLResource(id, context); 
    if (glVertexBuffers.containsKey(res)) {      
      glVertexBuffers.remove(res);
    }    
  }    

  // FrameBuffer Objects -----------------------------------------

  protected int createFrameBufferObject(int context) {
    deleteFinalizedFrameBufferObjects();

    int[] temp = new int[1];
    pgl.glGenFramebuffers(1, temp, 0);
    int id = temp[0];
    
    GLResource res = new GLResource(id, context); 
    
    if (glFrameBuffers.containsKey(res)) {
      showWarning("Adding same FBO twice");
    } else {
      glFrameBuffers.put(res, false);
    }

    return id;
  }

  protected void deleteFrameBufferObject(int id, int context) {
    GLResource res = new GLResource(id, context); 
    if (glFrameBuffers.containsKey(res)) {
      int[] temp = { id };
      pgl.glDeleteFramebuffers(1, temp, 0);
      glFrameBuffers.remove(res);
    }
  }

  protected void deleteAllFrameBufferObjects() {
    for (GLResource res : glFrameBuffers.keySet()) {
      int[] temp = { res.id };
      pgl.glDeleteFramebuffers(1, temp, 0);
    }
    glFrameBuffers.clear();
  }

  // This is synchronized because it is called from the GC thread.
  synchronized protected void finalizeFrameBufferObject(int id, int context) {
    GLResource res = new GLResource(id, context); 
    if (glFrameBuffers.containsKey(res)) {
      glFrameBuffers.put(res, true);
    }
  }

  protected void deleteFinalizedFrameBufferObjects() {
    Set<GLResource> finalized = new HashSet<GLResource>();

    for (GLResource res : glFrameBuffers.keySet()) {
      if (glFrameBuffers.get(res)) {
        finalized.add(res);
        int[] temp = { res.id };
        pgl.glDeleteFramebuffers(1, temp, 0);
      }
    }

    for (GLResource res : finalized) {
      glFrameBuffers.remove(res);
    }
  }

  protected void removeFrameBufferObject(int id, int context) {
    GLResource res = new GLResource(id, context); 
    if (glFrameBuffers.containsKey(res)) {      
      glFrameBuffers.remove(res);
    }    
  }   
  
  // RenderBuffer Objects -----------------------------------------------

  protected int createRenderBufferObject(int context) {
    deleteFinalizedRenderBufferObjects();

    int[] temp = new int[1];
    pgl.glGenRenderbuffers(1, temp, 0);
    int id = temp[0];
    
    GLResource res = new GLResource(id, context); 

    if (glRenderBuffers.containsKey(res)) {
      showWarning("Adding same renderbuffer twice");
    } else {
      glRenderBuffers.put(res, false);
    }

    return id;
  }

  protected void deleteRenderBufferObject(int id, int context) {
    GLResource res = new GLResource(id, context); 
    if (glRenderBuffers.containsKey(res)) {
      int[] temp = { id };
      pgl.glDeleteRenderbuffers(1, temp, 0);
      glRenderBuffers.remove(res);
    }
  }

  protected void deleteAllRenderBufferObjects() {
    for (GLResource res : glRenderBuffers.keySet()) {
      int[] temp = { res.id };
      pgl.glDeleteRenderbuffers(1, temp, 0);
    }
    glRenderBuffers.clear();
  }

  // This is synchronized because it is called from the GC thread.
  synchronized protected void finalizeRenderBufferObject(int id, int context) {
    GLResource res = new GLResource(id, context); 
    if (glRenderBuffers.containsKey(res)) {
      glRenderBuffers.put(res, true);
    }
  }

  protected void deleteFinalizedRenderBufferObjects() {
    Set<GLResource> finalized = new HashSet<GLResource>();

    for (GLResource res : glRenderBuffers.keySet()) {
      if (glRenderBuffers.get(res)) {
        finalized.add(res);
        int[] temp = { res.id };
        pgl.glDeleteRenderbuffers(1, temp, 0);
      }
    }

    for (GLResource res : finalized) {
      glRenderBuffers.remove(res);
    }
  }

  protected void removeRenderBufferObject(int id, int context) {
    GLResource res = new GLResource(id, context); 
    if (glRenderBuffers.containsKey(res)) {      
      glRenderBuffers.remove(res);
    }    
  }  
  
  // GLSL Program Objects -----------------------------------------------

  protected int createGLSLProgramObject(int context) {
    deleteFinalizedGLSLProgramObjects();

    int id = pgl.glCreateProgram();

    GLResource res = new GLResource(id, context); 
    
    if (glslPrograms.containsKey(res)) {
      showWarning("Adding same glsl program twice");
    } else {
      glslPrograms.put(res, false);
    }

    return id;
  }

  protected void deleteGLSLProgramObject(int id, int context) {
    GLResource res = new GLResource(id, context); 
    if (glslPrograms.containsKey(res)) {
      pgl.glDeleteProgram(res.id);
      glslPrograms.remove(res);
    }
  }

  protected void deleteAllGLSLProgramObjects() {
    for (GLResource res : glslPrograms.keySet()) {
      pgl.glDeleteProgram(res.id);
    }
    glslPrograms.clear();
  }

  // This is synchronized because it is called from the GC thread.
  synchronized protected void finalizeGLSLProgramObject(int id, int context) {
    GLResource res = new GLResource(id, context); 
    if (glslPrograms.containsKey(res)) {
      glslPrograms.put(res, true);
    }
  }

  protected void deleteFinalizedGLSLProgramObjects() {
    Set<GLResource> finalized = new HashSet<GLResource>();

    for (GLResource res : glslPrograms.keySet()) {
      if (glslPrograms.get(res)) {
        finalized.add(res);
        pgl.glDeleteProgram(res.id);
      }
    }

    for (GLResource res : finalized) {
      glslPrograms.remove(res);
    }
  }

  protected void removeGLSLProgramObject(int id, int context) {
    GLResource res = new GLResource(id, context); 
    if (glslPrograms.containsKey(res)) {      
      glslPrograms.remove(res);
    }    
  }  

  // GLSL Vertex Shader Objects -----------------------------------------------

  protected int createGLSLVertShaderObject(int context) {
    deleteFinalizedGLSLVertShaderObjects();

    int id = pgl.glCreateShader(PGL.GL_VERTEX_SHADER);

    GLResource res = new GLResource(id, context); 
    
    if (glslVertexShaders.containsKey(res)) {
      showWarning("Adding same glsl vertex shader twice");
    } else {
      glslVertexShaders.put(res, false);
    }

    return id;
  }

  protected void deleteGLSLVertShaderObject(int id, int context) {
    GLResource res = new GLResource(id, context); 
    if (glslVertexShaders.containsKey(res)) {
      pgl.glDeleteShader(res.id);
      glslVertexShaders.remove(res);
    }
  }

  protected void deleteAllGLSLVertShaderObjects() {
    for (GLResource res : glslVertexShaders.keySet()) {
      pgl.glDeleteShader(res.id);
    }
    glslVertexShaders.clear();
  }

  // This is synchronized because it is called from the GC thread.
  synchronized protected void finalizeGLSLVertShaderObject(int id, int context) {
    GLResource res = new GLResource(id, context); 
    if (glslVertexShaders.containsKey(res)) {
      glslVertexShaders.put(res, true);
    }
  }

  protected void deleteFinalizedGLSLVertShaderObjects() {
    Set<GLResource> finalized = new HashSet<GLResource>();

    for (GLResource res : glslVertexShaders.keySet()) {
      if (glslVertexShaders.get(res)) {
        finalized.add(res);
        pgl.glDeleteShader(res.id);
      }
    }

    for (GLResource res : finalized) {
      glslVertexShaders.remove(res);
    }
  }

  protected void removeGLSLVertShaderObject(int id, int context) {
    GLResource res = new GLResource(id, context); 
    if (glslVertexShaders.containsKey(res)) {      
      glslVertexShaders.remove(res);
    }    
  }  

  // GLSL Fragment Shader Objects -----------------------------------------------

  protected int createGLSLFragShaderObject(int context) {
    deleteFinalizedGLSLFragShaderObjects();

    int id = pgl.glCreateShader(PGL.GL_FRAGMENT_SHADER);

    GLResource res = new GLResource(id, context); 
    
    if (glslFragmentShaders.containsKey(res)) {
      showWarning("Adding same glsl fragment shader twice");
    } else {
      glslFragmentShaders.put(res, false);
    }

    return id;
  }

  protected void deleteGLSLFragShaderObject(int id, int context) {
    GLResource res = new GLResource(id, context); 
    if (glslFragmentShaders.containsKey(res)) {
      pgl.glDeleteShader(res.id);
      glslFragmentShaders.remove(res);
    }
  }

  protected void deleteAllGLSLFragShaderObjects() {
    for (GLResource res : glslFragmentShaders.keySet()) {
      pgl.glDeleteShader(res.id);
    }
    glslFragmentShaders.clear();
  }

  // This is synchronized because it is called from the GC thread.
  synchronized protected void finalizeGLSLFragShaderObject(int id, int context) {
    GLResource res = new GLResource(id, context); 
    if (glslFragmentShaders.containsKey(res)) {
      glslFragmentShaders.put(res, true);
    }
  }

  protected void deleteFinalizedGLSLFragShaderObjects() {
    Set<GLResource> finalized = new HashSet<GLResource>();

    for (GLResource res : glslFragmentShaders.keySet()) {
      if (glslFragmentShaders.get(res)) {
        finalized.add(res);
        pgl.glDeleteShader(res.id);
      }
    }

    for (GLResource res : finalized) {
      glslFragmentShaders.remove(res);
    }
  }

  protected void removeGLSLFragShaderObject(int id, int context) {
    GLResource res = new GLResource(id, context); 
    if (glslFragmentShaders.containsKey(res)) {      
      glslFragmentShaders.remove(res);
    }    
  }  
  
  // All OpenGL resources -----------------------------------------------
  
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


  public void setFramebuffer(FrameBuffer fbo) {
    currentFramebuffer = fbo;
    currentFramebuffer.bind();
  }


  public void popFramebuffer() {
    try {
      currentFramebuffer.finish();
      currentFramebuffer = fbStack.pop();
      currentFramebuffer.bind();
    } catch (EmptyStackException e) {
      PGraphics.showWarning("P3D: Empty framebuffer stack");
    }
  }


  //////////////////////////////////////////////////////////////

  // FRAME RENDERING


  protected void createPolyBuffers() {
    if (!polyBuffersCreated || polyBuffersContextIsOutdated()) {
      polyBuffersContext = pgl.getCurrentContext();
      
      int sizef = INIT_VERTEX_BUFFER_SIZE * PGL.SIZEOF_FLOAT;
      int sizei = INIT_VERTEX_BUFFER_SIZE * PGL.SIZEOF_INT;
      int sizex = INIT_INDEX_BUFFER_SIZE * PGL.SIZEOF_INDEX;

      glPolyVertexBufferID = createVertexBufferObject(polyBuffersContext.code());
      pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glPolyVertexBufferID);
      pgl.glBufferData(PGL.GL_ARRAY_BUFFER, 3 * sizef, null, PGL.GL_STATIC_DRAW);

      glPolyColorBufferID = createVertexBufferObject(polyBuffersContext.code());
      pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glPolyColorBufferID);
      pgl.glBufferData(PGL.GL_ARRAY_BUFFER, sizei, null, PGL.GL_STATIC_DRAW);

      glPolyNormalBufferID = createVertexBufferObject(polyBuffersContext.code());
      pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glPolyNormalBufferID);
      pgl.glBufferData(PGL.GL_ARRAY_BUFFER, 3 * sizef, null, PGL.GL_STATIC_DRAW);

      glPolyTexcoordBufferID = createVertexBufferObject(polyBuffersContext.code());
      pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glPolyTexcoordBufferID);
      pgl.glBufferData(PGL.GL_ARRAY_BUFFER, 2 * sizef, null, PGL.GL_STATIC_DRAW);

      glPolyAmbientBufferID = pgPrimary.createVertexBufferObject(polyBuffersContext.code());
      pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glPolyAmbientBufferID);
      pgl.glBufferData(PGL.GL_ARRAY_BUFFER, sizei, null, PGL.GL_STATIC_DRAW);

      glPolySpecularBufferID = pgPrimary.createVertexBufferObject(polyBuffersContext.code());
      pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glPolySpecularBufferID);
      pgl.glBufferData(PGL.GL_ARRAY_BUFFER, sizei, null, PGL.GL_STATIC_DRAW);

      glPolyEmissiveBufferID = pgPrimary.createVertexBufferObject(polyBuffersContext.code());
      pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glPolyEmissiveBufferID);
      pgl.glBufferData(PGL.GL_ARRAY_BUFFER, sizei, null, PGL.GL_STATIC_DRAW);

      glPolyShininessBufferID = pgPrimary.createVertexBufferObject(polyBuffersContext.code());
      pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glPolyShininessBufferID);
      pgl.glBufferData(PGL.GL_ARRAY_BUFFER, sizef, null, PGL.GL_STATIC_DRAW);

      pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, 0);

      glPolyIndexBufferID = createVertexBufferObject(polyBuffersContext.code());
      pgl.glBindBuffer(PGL.GL_ELEMENT_ARRAY_BUFFER, glPolyIndexBufferID);
      pgl.glBufferData(PGL.GL_ELEMENT_ARRAY_BUFFER, sizex, null, PGL.GL_STATIC_DRAW);

      pgl.glBindBuffer(PGL.GL_ELEMENT_ARRAY_BUFFER, 0);
      
      polyBuffersCreated = true;
    }
  }


  protected void updatePolyBuffers(boolean lit, boolean tex) {
    createPolyBuffers();
    
    int size = tessGeo.polyVertexCount;
    int sizef = size * PGL.SIZEOF_FLOAT;
    int sizei = size * PGL.SIZEOF_INT;

    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glPolyVertexBufferID);
    pgl.glBufferData(PGL.GL_ARRAY_BUFFER, 4 * sizef, FloatBuffer.wrap(tessGeo.polyVertices, 0, 4 * size), PGL.GL_STATIC_DRAW);

    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glPolyColorBufferID);
    pgl.glBufferData(PGL.GL_ARRAY_BUFFER, sizei, IntBuffer.wrap(tessGeo.polyColors, 0, size), PGL.GL_STATIC_DRAW);

    if (lit) {
      pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glPolyNormalBufferID);
      pgl.glBufferData(PGL.GL_ARRAY_BUFFER, 3 * sizef, FloatBuffer.wrap(tessGeo.polyNormals, 0, 3 * size), PGL.GL_STATIC_DRAW);

      pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glPolyAmbientBufferID);
      pgl.glBufferData(PGL.GL_ARRAY_BUFFER, sizei, IntBuffer.wrap(tessGeo.polyAmbient, 0, size), PGL.GL_STATIC_DRAW);

      pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glPolySpecularBufferID);
      pgl.glBufferData(PGL.GL_ARRAY_BUFFER, sizei, IntBuffer.wrap(tessGeo.polySpecular, 0, size), PGL.GL_STATIC_DRAW);

      pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glPolyEmissiveBufferID);
      pgl.glBufferData(PGL.GL_ARRAY_BUFFER, sizei, IntBuffer.wrap(tessGeo.polyEmissive, 0, size), PGL.GL_STATIC_DRAW);


      pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glPolyShininessBufferID);
      pgl.glBufferData(PGL.GL_ARRAY_BUFFER, sizef, FloatBuffer.wrap(tessGeo.polyShininess, 0, size), PGL.GL_STATIC_DRAW);
    }

    if (tex) {
      pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glPolyTexcoordBufferID);
      pgl.glBufferData(PGL.GL_ARRAY_BUFFER, 2 * sizef, FloatBuffer.wrap(tessGeo.polyTexcoords, 0, 2 * size), PGL.GL_STATIC_DRAW);
    }
    
    pgl.glBindBuffer(PGL.GL_ELEMENT_ARRAY_BUFFER, glPolyIndexBufferID);
    pgl.glBufferData(PGL.GL_ELEMENT_ARRAY_BUFFER, tessGeo.polyIndexCount * PGL.SIZEOF_INDEX,
                     ShortBuffer.wrap(tessGeo.polyIndices, 0, tessGeo.polyIndexCount), PGL.GL_STATIC_DRAW);     
  }


  protected void unbindPolyBuffers() {
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, 0);
    pgl.glBindBuffer(PGL.GL_ELEMENT_ARRAY_BUFFER, 0);
  }


  protected boolean polyBuffersContextIsOutdated() {
    return !pgl.contextIsCurrent(polyBuffersContext);    
  }

  
  protected void deletePolyBuffers() {
    if (polyBuffersCreated) {
      deleteVertexBufferObject(glPolyVertexBufferID, polyBuffersContext.code());
      glPolyVertexBufferID = 0;

      deleteVertexBufferObject(glPolyColorBufferID, polyBuffersContext.code());
      glPolyColorBufferID = 0;

      deleteVertexBufferObject(glPolyNormalBufferID, polyBuffersContext.code());
      glPolyNormalBufferID = 0;

      deleteVertexBufferObject(glPolyTexcoordBufferID, polyBuffersContext.code());
      glPolyTexcoordBufferID = 0;

      deleteVertexBufferObject(glPolyAmbientBufferID, polyBuffersContext.code());
      glPolyAmbientBufferID = 0;

      deleteVertexBufferObject(glPolySpecularBufferID, polyBuffersContext.code());
      glPolySpecularBufferID = 0;

      deleteVertexBufferObject(glPolyEmissiveBufferID, polyBuffersContext.code());
      glPolyEmissiveBufferID = 0;

      deleteVertexBufferObject(glPolyShininessBufferID, polyBuffersContext.code());
      glPolyShininessBufferID = 0;

      deleteVertexBufferObject(glPolyIndexBufferID, polyBuffersContext.code());
      glPolyIndexBufferID = 0;    
      
      polyBuffersCreated = false;
    }
  }


  protected void createLineBuffers() {
    if (!lineBuffersCreated || lineBufferContextIsOutdated()) {
      lineBuffersContext = pgl.getCurrentContext();
      
      int sizef = INIT_VERTEX_BUFFER_SIZE * PGL.SIZEOF_FLOAT;
      int sizei = INIT_VERTEX_BUFFER_SIZE * PGL.SIZEOF_INT;
      int sizex = INIT_INDEX_BUFFER_SIZE * PGL.SIZEOF_INDEX;

      glLineVertexBufferID = createVertexBufferObject(lineBuffersContext.code());

      pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glLineVertexBufferID);
      pgl.glBufferData(PGL.GL_ARRAY_BUFFER, 3 * sizef, null, PGL.GL_STATIC_DRAW);

      glLineColorBufferID = createVertexBufferObject(lineBuffersContext.code());
      pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glLineColorBufferID);
      pgl.glBufferData(PGL.GL_ARRAY_BUFFER, sizei, null, PGL.GL_STATIC_DRAW);

      glLineAttribBufferID = createVertexBufferObject(lineBuffersContext.code());
      pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glLineAttribBufferID);
      pgl.glBufferData(PGL.GL_ARRAY_BUFFER, 4 * sizef, null, PGL.GL_STATIC_DRAW);

      pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, 0);

      glLineIndexBufferID = createVertexBufferObject(lineBuffersContext.code());
      pgl.glBindBuffer(PGL.GL_ELEMENT_ARRAY_BUFFER, glLineIndexBufferID);
      pgl.glBufferData(PGL.GL_ELEMENT_ARRAY_BUFFER, sizex, null, PGL.GL_STATIC_DRAW);

      pgl.glBindBuffer(PGL.GL_ELEMENT_ARRAY_BUFFER, 0);
      
      lineBuffersCreated = true;      
    }
  }


  protected void updateLineBuffers() {
    createLineBuffers();
    
    int size = tessGeo.lineVertexCount;
    int sizef = size * PGL.SIZEOF_FLOAT;
    int sizei = size * PGL.SIZEOF_INT;

    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glLineVertexBufferID);
    pgl.glBufferData(PGL.GL_ARRAY_BUFFER, 4 * sizef, FloatBuffer.wrap(tessGeo.lineVertices, 0, 4 * size), PGL.GL_STATIC_DRAW);

    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glLineColorBufferID);
    pgl.glBufferData(PGL.GL_ARRAY_BUFFER, sizei, IntBuffer.wrap(tessGeo.lineColors, 0, size), PGL.GL_STATIC_DRAW);

    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glLineAttribBufferID);
    pgl.glBufferData(PGL.GL_ARRAY_BUFFER, 4 * sizef, FloatBuffer.wrap(tessGeo.lineAttribs, 0, 4 * size), PGL.GL_STATIC_DRAW);

    pgl.glBindBuffer(PGL.GL_ELEMENT_ARRAY_BUFFER, glLineIndexBufferID);
    pgl.glBufferData(PGL.GL_ELEMENT_ARRAY_BUFFER, tessGeo.lineIndexCount * PGL.SIZEOF_INDEX,
                     ShortBuffer.wrap(tessGeo.lineIndices, 0, tessGeo.lineIndexCount), PGL.GL_STATIC_DRAW);
  }


  protected void unbindLineBuffers() {
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, 0);
    pgl.glBindBuffer(PGL.GL_ELEMENT_ARRAY_BUFFER, 0);
  }

  
  protected boolean lineBufferContextIsOutdated() {
    return !pgl.contextIsCurrent(lineBuffersContext);    
  }  

  
  protected void deleteLineBuffers() {
    if (lineBuffersCreated) {
      deleteVertexBufferObject(glLineVertexBufferID, lineBuffersContext.code());
      glLineVertexBufferID = 0;

      deleteVertexBufferObject(glLineColorBufferID, lineBuffersContext.code());
      glLineColorBufferID = 0;

      deleteVertexBufferObject(glLineAttribBufferID, lineBuffersContext.code());
      glLineAttribBufferID = 0;

      deleteVertexBufferObject(glLineIndexBufferID, lineBuffersContext.code());
      glLineIndexBufferID = 0;
      
      lineBuffersCreated = false;
    }
  }


  protected void createPointBuffers() {
    if (!pointBuffersCreated || pointBuffersContextIsOutdated()) {
      pointBuffersContext = pgl.getCurrentContext();
      
      int sizef = INIT_VERTEX_BUFFER_SIZE * PGL.SIZEOF_FLOAT;
      int sizei = INIT_VERTEX_BUFFER_SIZE * PGL.SIZEOF_INT;
      int sizex = INIT_INDEX_BUFFER_SIZE * PGL.SIZEOF_INDEX;      

      glPointVertexBufferID = createVertexBufferObject(pointBuffersContext.code());
      pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glPointVertexBufferID);
      pgl.glBufferData(PGL.GL_ARRAY_BUFFER, 3 * sizef, null, PGL.GL_STATIC_DRAW);

      glPointColorBufferID = createVertexBufferObject(pointBuffersContext.code());
      pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glPointColorBufferID);
      pgl.glBufferData(PGL.GL_ARRAY_BUFFER, sizei, null, PGL.GL_STATIC_DRAW);

      glPointAttribBufferID = createVertexBufferObject(pointBuffersContext.code());
      pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glPointAttribBufferID);
      pgl.glBufferData(PGL.GL_ARRAY_BUFFER, 2 * sizef, null, PGL.GL_STATIC_DRAW);

      pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, 0);

      glPointIndexBufferID = createVertexBufferObject(pointBuffersContext.code());
      pgl.glBindBuffer(PGL.GL_ELEMENT_ARRAY_BUFFER, glPointIndexBufferID);
      pgl.glBufferData(PGL.GL_ELEMENT_ARRAY_BUFFER, sizex, null, PGL.GL_STATIC_DRAW);

      pgl.glBindBuffer(PGL.GL_ELEMENT_ARRAY_BUFFER, 0);
            
      pointBuffersCreated = true;
    }    
  }


  protected void updatePointBuffers() {
    createPointBuffers();
    
    int size = tessGeo.pointVertexCount;
    int sizef = size * PGL.SIZEOF_FLOAT;
    int sizei = size * PGL.SIZEOF_INT;

    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glPointVertexBufferID);
    pgl.glBufferData(PGL.GL_ARRAY_BUFFER, 4 * sizef, FloatBuffer.wrap(tessGeo.pointVertices, 0, 4 * size), PGL.GL_STATIC_DRAW);

    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glPointColorBufferID);
    pgl.glBufferData(PGL.GL_ARRAY_BUFFER, sizei, IntBuffer.wrap(tessGeo.pointColors, 0, size), PGL.GL_STATIC_DRAW);

    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glPointAttribBufferID);
    pgl.glBufferData(PGL.GL_ARRAY_BUFFER, 2 * sizef, FloatBuffer.wrap(tessGeo.pointAttribs, 0, 2 * size), PGL.GL_STATIC_DRAW);

    pgl.glBindBuffer(PGL.GL_ELEMENT_ARRAY_BUFFER, glPointIndexBufferID);
    pgl.glBufferData(PGL.GL_ELEMENT_ARRAY_BUFFER, tessGeo.pointIndexCount * PGL.SIZEOF_INDEX,
                     ShortBuffer.wrap(tessGeo.pointIndices, 0, tessGeo.pointIndexCount), PGL.GL_STATIC_DRAW);
  }


  protected void unbindPointBuffers() {
    pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, 0);
    pgl.glBindBuffer(PGL.GL_ELEMENT_ARRAY_BUFFER, 0);
  }


  protected boolean pointBuffersContextIsOutdated() {
    return !pgl.contextIsCurrent(pointBuffersContext);    
  }    
  
  
  protected void deletePointBuffers() {
    if (pointBuffersCreated) {
      deleteVertexBufferObject(glPointVertexBufferID, pointBuffersContext.code());
      glPointVertexBufferID = 0;

      deleteVertexBufferObject(glPointColorBufferID, pointBuffersContext.code());
      glPointColorBufferID = 0;

      deleteVertexBufferObject(glPointAttribBufferID, pointBuffersContext.code());
      glPointAttribBufferID = 0;

      deleteVertexBufferObject(glPointIndexBufferID, pointBuffersContext.code());
      glPointIndexBufferID = 0;
      
      pointBuffersCreated = false;
    }
  }


  /**
   * OpenGL cannot draw until a proper native peer is available, so this
   * returns the value of PApplet.isDisplayable() (inherited from Component).
   */
  public boolean canDraw() {
    return pgl.canDraw();
  }


  public void requestDraw() {
    if (primarySurface) {
      if (pgl.initialized) {
        pgl.requestDraw();
      } else {
        initPrimary();
      }
    }
  }


  public void beginDraw() {
    if (drawing) {
      showWarning("P3D: Already called beginDraw().");
      return;
    }

    if (pgCurrent != pgPrimary && this != pgPrimary) {
      // It seems that the user is trying to start
      // another beginDraw()/endDraw() block for an
      // offscreen surface, still drawing on another
      // offscreen surface. This situation is not
      // catched by the drawing check above.
      showWarning("P3D: Already called beginDraw() for another P3D object.");
      return;      
    }    
    
    if (!glParamsRead) {
      getGLParameters();
    }

    if (!settingsInited) {
      defaultSettings();
    }

    if (primarySurface) {
      pgl.updatePrimary();
    } else {
      if (!pgl.initialized) {
        initOffscreen();
      } else {
        boolean outdated = offscreenFramebuffer != null && offscreenFramebuffer.contextIsOutdated();
        boolean outdatedMulti = offscreenFramebufferMultisample != null && offscreenFramebufferMultisample.contextIsOutdated();
        if (outdated || outdatedMulti) {
          pgl.initialized = false;
          initOffscreen();
        }
      }

      pushFramebuffer();
      if (offscreenMultisample) {
        setFramebuffer(offscreenFramebufferMultisample);
      } else {
        setFramebuffer(offscreenFramebuffer);
      }
      pgl.updateOffscreen(pgPrimary.pgl);
      pgl.glDrawBuffer(PGL.GL_COLOR_ATTACHMENT0);      
    }

    // We are ready to go!
    report("top beginDraw()");
    
    drawing = true;    
    pgCurrent = this;

    inGeo.clear();
    tessGeo.clear();
    texCache.clear();

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
    pgl.glDepthFunc(PGL.GL_LEQUAL);

    if (hints[ENABLE_ACCURATE_2D]) {
      flushMode = FLUSH_CONTINUOUSLY;
    } else {
      flushMode = FLUSH_WHEN_FULL;
    }

    if (primarySurface) {
      int[] temp = new int[1];
      pgl.glGetIntegerv(PGL.GL_SAMPLES, temp, 0);
      if (quality != temp[0] && 1 < temp[0] && 1 < quality) {
        quality = temp[0];
      }
    }
    if (quality < 2) {
      pgl.glDisable(PGL.GL_MULTISAMPLE);
    } else {
      pgl.glEnable(PGL.GL_MULTISAMPLE);
    }
    pgl.glDisable(PGL.GL_POINT_SMOOTH);
    pgl.glDisable(PGL.GL_LINE_SMOOTH);
    pgl.glDisable(PGL.GL_POLYGON_SMOOTH);    

    // setup opengl viewport.
    viewport[0] = 0; viewport[1] = 0; viewport[2] = width; viewport[3] = height;
    pgl.glViewport(viewport[0], viewport[1], viewport[2], viewport[3]);
    if (resized) {
      // To avoid having garbage in the screen after a resize,
      // in the case background is not called in draw().
      background(0);
      if (texture != null) {
        // The screen texture should be deleted because it
        // corresponds to the old window size.
        this.removeCache(pgPrimary);
        this.removeParams(pgPrimary);
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
      modelviewInv.set(cameraInv);
      calcProjmodelview();
    }

    if (is3D()) {
      noLights();
      lightFalloff(1, 0, 0);
      lightSpecular(0, 0, 0);
    }

    // Because y is flipped, the vertices that should be specified by 
    // the user in CCW order to define a front-facing facet, end up being
    // CW.
    pgl.glFrontFace(PGL.GL_CW);
    pgl.glDisable(PGL.GL_CULL_FACE);

    // Processing uses only one texture unit.
    pgl.glActiveTexture(PGL.GL_TEXTURE0);

    // The current normal vector is set to be parallel to the Z axis.
    normalX = normalY = normalZ = 0;

    perspectiveCorrectedLines = hints[ENABLE_PERSPECTIVE_CORRECTED_LINES];

    // Clear depth and stencil buffers.
    pgl.glDepthMask(true);
    pgl.glClearColor(0, 0, 0, 0);
    pgl.glClear(PGL.GL_DEPTH_BUFFER_BIT | PGL.GL_STENCIL_BUFFER_BIT);

    if (primarySurface) {
      pgl.beginOnscreenDraw(clearColorBuffer);
    } else {
      pgl.beginOffscreenDraw(pgPrimary.clearColorBuffer);

      // Just in case the texture was recreated (in a resize event for example)
      offscreenFramebuffer.setColorBuffer(texture);
    }

    if (restoreSurface) {
      restoreSurfaceFromPixels();
      restoreSurface = false;        
    }    
    
    if (hints[DISABLE_DEPTH_MASK]) {
      pgl.glDepthMask(false);
    } else {
      pgl.glDepthMask(true);
    }
    
    pixelsOp = OP_NONE;

    modified = false;
    setgetPixels = false;

    clearColorBuffer0 = clearColorBuffer;
    clearColorBuffer = false;

    report("bot beginDraw()");
  }

  
  public void endDraw() {
    report("top endDraw()");

    // Flushing any remaining geometry.
    flush();

    if (!drawing) {
      showWarning("P3D: Cannot call endDraw() before beginDraw().");
      return;
    }
    
    if (primarySurface) {
      pgl.endOnscreenDraw(clearColorBuffer0);

      if (!pgl.initialized || parent.frameCount == 0) {
        // Smooth was called at some point during drawing. We save
        // the current contents of the back buffer (because the 
        // buffers haven't been swapped yet) to the pixels array.
        // The frameCount == 0 condition is to handle the situation when
        // no smooth is called in setup in the PDE, but the OpenGL appears to
        // be recreated due to the size() nastiness.        
        saveSurfaceToPixels();
        restoreSurface = true;
      }         
      
      pgl.glFlush();
    } else {
      if (offscreenMultisample) {
        offscreenFramebufferMultisample.copy(offscreenFramebuffer);
      }
      
      if (!pgl.initialized || !pgPrimary.pgl.initialized || parent.frameCount == 0) {
        // If the primary surface is re-initialized, this offscreen 
        // surface needs to save its contents into the pixels array
        // so they can be restored after the FBOs are recreated.
        // Note that a consequence of how this is code works, is that
        // if the user changes th smooth level of the primary surface
        // in the middle of draw, but after drawing the offscreen surfaces
        // then these won't be restored in the next frame since their 
        // endDraw() calls didn't pick up any change in the initialization
        // state of the primary surface.        
        saveSurfaceToPixels();
        restoreSurface = true;
      }      
      
      popFramebuffer();

      pgl.endOffscreenDraw(pgPrimary.clearColorBuffer0);

      pgPrimary.restoreGL();
    }

    // Done!
    drawing = false;    
    if (pgCurrent == pgPrimary) {
      // Done with the main surface
      pgCurrent = null;
    } else {
      // Done with an offscreen surface,
      // going back to onscreen drawing.
      pgCurrent = pgPrimary;
    }
    
    report("bot endDraw()");
  }


  public PGL beginPGL() {
    return pgl;
  }


  public void endPGL() {
    restoreGL();
  }


  public void restartPGL() {
    pgl.initialized = false;
  }


  protected void restoreGL() {
    blendMode(blendMode);

    if (hints[DISABLE_DEPTH_TEST]) {
      pgl.glDisable(PGL.GL_DEPTH_TEST);
    } else {
      pgl.glEnable(PGL.GL_DEPTH_TEST);
    }
    pgl.glDepthFunc(PGL.GL_LEQUAL);

    if (quality < 2) {
      pgl.glDisable(PGL.GL_MULTISAMPLE);
//      pgl.glEnable(PGL.GL_POINT_SMOOTH);
//      pgl.glEnable(PGL.GL_LINE_SMOOTH);
//      pgl.glEnable(PGL.GL_POLYGON_SMOOTH);
    } else {
      pgl.glEnable(PGL.GL_MULTISAMPLE);
      pgl.glDisable(PGL.GL_POINT_SMOOTH);
      pgl.glDisable(PGL.GL_LINE_SMOOTH);
      pgl.glDisable(PGL.GL_POLYGON_SMOOTH);
    }

    pgl.glViewport(viewport[0], viewport[1], viewport[2], viewport[3]);

    pgl.glFrontFace(PGL.GL_CW);
    pgl.glDisable(PGL.GL_CULL_FACE);

    pgl.glActiveTexture(PGL.GL_TEXTURE0);

    if (hints[DISABLE_DEPTH_MASK]) {
      pgl.glDepthMask(false);
    } else {
      pgl.glDepthMask(true);
    }
  }


  protected void beginPixelsOp(int op) {
    if (primarySurface) {
      if (pgl.primaryIsDoubleBuffered()) {
        // We read or write from the back buffer, where all the 
        // drawing in the current frame is taking place.
        if (op == OP_READ) {
          pgl.glReadBuffer(PGL.GL_BACK);
        } else {
          pgl.glDrawBuffer(PGL.GL_BACK);
        }
      }
      offscreenNotCurrent = false;
    } else {
      // Making sure that the offscreen FBO is current. This allows to do calls
      // like loadPixels(), set() or get() without enclosing them between
      // beginDraw()/endDraw() when working with a PGraphics object. We don't
      // need the rest of the surface initialization/finalization, since only
      // the pixels are affected.
      if (op == OP_READ) {
        // We always read the screen pixels from the color FBO.
        offscreenNotCurrent = offscreenFramebuffer != currentFramebuffer;
        if (offscreenNotCurrent) {
          pushFramebuffer();
          setFramebuffer(offscreenFramebuffer);
          pgl.updateOffscreen(pgPrimary.pgl);
        }
        pgl.glReadBuffer(PGL.GL_COLOR_ATTACHMENT0);
      } else {
        // We can write directly to the color FBO, or to the multisample FBO
        // if multisampling is enabled.
        if (offscreenMultisample) {
          offscreenNotCurrent = offscreenFramebufferMultisample != currentFramebuffer;
        } else {
          offscreenNotCurrent = offscreenFramebuffer != currentFramebuffer;
        }
        if (offscreenNotCurrent) {
          pushFramebuffer();
          if (offscreenMultisample) {
            setFramebuffer(offscreenFramebufferMultisample);
          } else {
            setFramebuffer(offscreenFramebuffer);
          }
          pgl.updateOffscreen(pgPrimary.pgl);
        }
        pgl.glDrawBuffer(PGL.GL_COLOR_ATTACHMENT0);
      }
    }
    pixelsOp = op;
  }


  protected void endPixelsOp() {
    if (offscreenNotCurrent) {
      if (pixelsOp == OP_WRITE && offscreenMultisample) {
        // We were writing to the multisample FBO, so we need
        // to blit its contents to the color FBO.
        offscreenFramebufferMultisample.copy(offscreenFramebuffer);
      }
      popFramebuffer();
    }
    pixelsOp = OP_NONE;
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
    // modelview (remember that gl matrices are column-major,
    // meaning that elements 0, 1, 2 are the first column,
    // 3, 4, 5 the second, etc.:
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

    clearColorBuffer = false;

    if (fbStack == null) {
      fbStack = new Stack<FrameBuffer>();

      screenFramebuffer = new FrameBuffer(parent, width, height, true);
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

    // Default material properties
    ambient(80);
    specular(125);
    emissive(0);
    shininess(1);
    
    // To indicate that the user hasn't set ambient
    setAmbient = false;
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
        // We flush the geometry using the previous line setting.
        flush();
      }
      perspectiveCorrectedLines = false;
    } else if (which == ENABLE_PERSPECTIVE_CORRECTED_LINES) {
      if (0 < tessGeo.lineVertexCount && 0 < tessGeo.lineIndexCount) {
        // We flush the geometry using the previous line setting.
        flush();
      }
      perspectiveCorrectedLines = true;
    }
  }


  //////////////////////////////////////////////////////////////

  // VERTEX SHAPES


  public void beginShape(int kind) {
    shape = kind;

    inGeo.clear();

    breakShape = true;
    defaultEdges = true;

    textureImage0 = textureImage;
    // The superclass method is called to avoid an early flush.
    super.noTexture();

    normalMode = NORMAL_MODE_AUTO;
  }


  public void endShape(int mode) {
    if (flushMode == FLUSH_WHEN_FULL && hints[DISABLE_TEXTURE_CACHE] &&
        textureImage0 != null && textureImage == null) {
      // The previous shape had a texture and this one doesn't. So we need to flush
      // the textured geometry.
      textureImage = textureImage0;
      flush();
      textureImage = null;
    }

    tessellate(mode);

    if ((flushMode == FLUSH_CONTINUOUSLY) ||
        (flushMode == FLUSH_WHEN_FULL && tessGeo.isFull())) {
      flush();
    }
  }

  protected void endShape(int[] indices) {
    endShape(indices, null);
  }

  protected void endShape(int[] indices, int[] edges) {
    if (shape != TRIANGLE && shape != TRIANGLES) {
      throw new RuntimeException("Indices and edges can only be set for TRIANGLE shapes");
    }
    
    if (flushMode == FLUSH_WHEN_FULL && hints[DISABLE_TEXTURE_CACHE] &&
        textureImage0 != null && textureImage == null) {
      // The previous shape had a texture and this one doesn't. So we need to flush
      // the textured geometry.
      textureImage = textureImage0;
      flush();
      textureImage = null;
    }   

    tessellate(indices, edges);    
    
    if (flushMode == FLUSH_CONTINUOUSLY ||
        (flushMode == FLUSH_WHEN_FULL && tessGeo.isFull())) {
      flush();
    }    
  }

  
  public void textureWrap(int wrap) {
    this.textureWrap = wrap;
  }
  
  
  public void textureQuality(int quality) {
    this.textureQuality = quality;
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
    breakShape = true;
  }


  public void endContour() {
    if (!openContour) {
      showWarning("P3D: Need to call beginContour() first.");
      return;
    }
    openContour = false;    
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

    if (textured && textureMode == IMAGE) {
      u = PApplet.min(1, u / textureImage.width);
      v = PApplet.min(1, v / textureImage.height);
    }

    inGeo.addVertex(x, y, z,
                    fcolor,
                    normalX, normalY, normalZ,
                    u, v,
                    scolor, sweight,
                    ambientColor, specularColor, emissiveColor, shininess,
                    vertexCode());
  }


  protected int vertexCode() {
    int code = VERTEX;
    if (breakShape) {
      code = BREAK;
      breakShape = false;
    }    
    return code;
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
    tessellator.setStrokeColor(strokeColor);
    tessellator.setStrokeWeight(strokeWeight);
    tessellator.setStrokeCap(strokeCap);
    tessellator.setStrokeJoin(strokeJoin);
    tessellator.setTexCache(texCache, textureImage0, textureImage);        
    tessellator.setTransform(modelview);
    tessellator.set3D(is3D());
    
    if (shape == POINTS) {
      tessellator.tessellatePoints();
    } else if (shape == LINES) {
      tessellator.tessellateLines();
    } else if (shape == LINE_STRIP) {
      tessellator.tessellateLineStrip();
    } else if (shape == LINE_LOOP) {
      tessellator.tessellateLineLoop();
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
  }

  protected void tessellate(int[] indices, int[] edges) {
    if (edges != null) {
      int nedges = edges.length / 2;
      for (int n = 0; n < nedges; n++) {
        int i0 = edges[2 * n + 0];
        int i1 = edges[2 * n + 1];
        inGeo.addEdge(i0, i1, n == 0, n == nedges - 1);            
      }
    }
        
    tessellator.setInGeometry(inGeo);
    tessellator.setTessGeometry(tessGeo);
    tessellator.setFill(fill || textureImage != null);
    tessellator.setStroke(stroke);
    tessellator.setStrokeColor(strokeColor);
    tessellator.setStrokeWeight(strokeWeight);
    tessellator.setStrokeCap(strokeCap);
    tessellator.setStrokeJoin(strokeJoin);
    tessellator.setTexCache(texCache, textureImage0, textureImage);
    tessellator.setTransform(modelview);
    tessellator.set3D(is3D());    

    if (stroke && defaultEdges && edges == null) inGeo.addTrianglesEdges();
    if (normalMode == NORMAL_MODE_AUTO) inGeo.calcTrianglesNormals();
    tessellator.tessellateTriangles(indices);    
  }


  public void flush() {    
    boolean hasPolys = 0 < tessGeo.polyVertexCount && 0 < tessGeo.polyIndexCount;
    boolean hasLines = 0 < tessGeo.lineVertexCount && 0 < tessGeo.lineIndexCount;    
    boolean hasPoints = 0 < tessGeo.pointVertexCount && 0 < tessGeo.pointIndexCount;    
    
    boolean hasPixels = modified && pixels != null;
    
    if (hasPixels) {
      // If the user has been manipulating individual pixels,
      // the changes need to be copied to the screen before
      // drawing any new geometry.
      flushPixels();
      setgetPixels = false;
    }

    if (hasPoints || hasLines || hasPolys) {
      if (flushMode == FLUSH_WHEN_FULL && !hints[DISABLE_TRANSFORM_CACHE]) {
        // The modelview transformation has been applied already to the
        // tessellated vertices, so we set the OpenGL modelview matrix as
        // the identity to avoid applying the model transformations twice.
        pushMatrix();
        resetMatrix();
      }

      if (hasPolys) {
        flushPolys();
        if (raw != null) {
          rawPolys();
        }
      }

      if (is3D()) {
        if (hasLines) {
          flushLines();
          if (raw != null) {
            rawLines();
          }                
        }
        
        if (hasPoints) {
          flushPoints();
          if (raw != null) {
            rawPoints();
          }        
        }
      }

      if (flushMode == FLUSH_WHEN_FULL && !hints[DISABLE_TRANSFORM_CACHE]) {
        popMatrix();
      }
    }

    tessGeo.clear();
    texCache.clear();
  }


  protected void flushPixels() {
    drawPixels(mx1, my1, mx2 - mx1 + 1, my2 - my1 + 1);
    modified = false;
  }
  

  protected void flushPolys() {
    updatePolyBuffers(lights, texCache.hasTexture);

    texCache.beginRender();
    for (int i = 0; i < texCache.size; i++) {
      Texture tex = texCache.getTexture(i);

      // If the renderer is 2D, then lights should always be false,
      // so no need to worry about that.
      PolyShader shader = getPolyShader(lights, tex != null);
      shader.start();
       
      int first = texCache.firstCache[i];
      int last = texCache.lastCache[i];
      IndexCache cache = tessGeo.polyIndexCache;
      
      for (int n = first; n <= last; n++) {
        int ioffset = n == first ? texCache.firstIndex[i] : cache.indexOffset[n];
        int icount = n == last ? texCache.lastIndex[i] - ioffset + 1 : 
                                 cache.indexOffset[n] + cache.indexCount[n] - ioffset;        
        int voffset = cache.vertexOffset[n];

        shader.setVertexAttribute(glPolyVertexBufferID, 4, PGL.GL_FLOAT, 0, 4 * voffset * PGL.SIZEOF_FLOAT);
        shader.setColorAttribute(glPolyColorBufferID, 4, PGL.GL_UNSIGNED_BYTE, 0, 4 * voffset * PGL.SIZEOF_BYTE);
        
        if (lights) {
          shader.setNormalAttribute(glPolyNormalBufferID, 3, PGL.GL_FLOAT, 0, 3 * voffset * PGL.SIZEOF_FLOAT);
          shader.setAmbientAttribute(glPolyAmbientBufferID, 4, PGL.GL_UNSIGNED_BYTE, 0, 4 * voffset * PGL.SIZEOF_BYTE);
          shader.setSpecularAttribute(glPolySpecularBufferID, 4, PGL.GL_UNSIGNED_BYTE, 0, 4 * voffset * PGL.SIZEOF_BYTE);
          shader.setEmissiveAttribute(glPolyEmissiveBufferID, 4, PGL.GL_UNSIGNED_BYTE, 0, 4 * voffset * PGL.SIZEOF_BYTE);
          shader.setShininessAttribute(glPolyShininessBufferID, 1, PGL.GL_FLOAT, 0, voffset * PGL.SIZEOF_FLOAT);
        }
        
        if (tex != null) {
          shader.setTexcoordAttribute(glPolyTexcoordBufferID, 2, PGL.GL_FLOAT, 0, 2 * voffset * PGL.SIZEOF_FLOAT);
          shader.setTexture(tex);
        }
        
        pgl.glBindBuffer(PGL.GL_ELEMENT_ARRAY_BUFFER, glPolyIndexBufferID);
        pgl.glDrawElements(PGL.GL_TRIANGLES, icount, PGL.INDEX_TYPE, ioffset * PGL.SIZEOF_INDEX);
        pgl.glBindBuffer(PGL.GL_ELEMENT_ARRAY_BUFFER, 0);
      }

      shader.stop();
    }
    texCache.endRender();
    unbindPolyBuffers();
  }


  void rawPolys() {
    raw.colorMode(RGB);
    raw.noStroke();
    raw.beginShape(TRIANGLES);
    
    float[] vertices = tessGeo.polyVertices;
    int[] color = tessGeo.polyColors;
    float[] uv = tessGeo.polyTexcoords;
    short[] indices = tessGeo.polyIndices;
    
    for (int i = 0; i < texCache.size; i++) {
      PImage textureImage = texCache.getTextureImage(i);
      
      int first = texCache.firstCache[i];
      int last = texCache.lastCache[i];
      IndexCache cache = tessGeo.polyIndexCache;
      for (int n = first; n <= last; n++) {
        int ioffset = n == first ? texCache.firstIndex[i] : cache.indexOffset[n];
        int icount = n == last ? texCache.lastIndex[i] - ioffset + 1 : 
                                 cache.indexOffset[n] + cache.indexCount[n] - ioffset;        
        int voffset = cache.vertexOffset[n];
        
        for (int tr = ioffset / 3; tr < (ioffset + icount) / 3; tr++) {
          int i0 = voffset + indices[3 * tr + 0];
          int i1 = voffset + indices[3 * tr + 1];
          int i2 = voffset + indices[3 * tr + 2];
          
          float[] pt0 = {0, 0, 0, 0};
          float[] pt1 = {0, 0, 0, 0};
          float[] pt2 = {0, 0, 0, 0};
          int argb0 = PGL.nativeToJavaARGB(color[i0]);
          int argb1 = PGL.nativeToJavaARGB(color[i1]);
          int argb2 = PGL.nativeToJavaARGB(color[i2]);
                  
          if (flushMode == FLUSH_CONTINUOUSLY || hints[DISABLE_TRANSFORM_CACHE]) {
            float[] src0 = {0, 0, 0, 0};
            float[] src1 = {0, 0, 0, 0};
            float[] src2 = {0, 0, 0, 0};
            PApplet.arrayCopy(vertices, 4 * i0, src0, 0, 4);
            PApplet.arrayCopy(vertices, 4 * i1, src1, 0, 4);
            PApplet.arrayCopy(vertices, 4 * i2, src2, 0, 4);
            modelview.mult(src0, pt0);
            modelview.mult(src1, pt1);
            modelview.mult(src2, pt2);
          } else {
            PApplet.arrayCopy(vertices, 4 * i0, pt0, 0, 4);
            PApplet.arrayCopy(vertices, 4 * i1, pt1, 0, 4);
            PApplet.arrayCopy(vertices, 4 * i2, pt2, 0, 4);
          }
          
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
              float sx0 = screenXImpl(pt0[0], pt0[1], pt0[2], pt0[3]), sy0 = screenYImpl(pt0[0], pt0[1], pt0[2], pt0[3]);
              float sx1 = screenXImpl(pt1[0], pt1[1], pt1[2], pt1[3]), sy1 = screenYImpl(pt1[0], pt1[1], pt1[2], pt1[3]);
              float sx2 = screenXImpl(pt2[0], pt2[1], pt2[2], pt2[3]), sy2 = screenYImpl(pt2[0], pt2[1], pt2[2], pt2[3]);              
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
              float sx0 = screenXImpl(pt0[0], pt0[1], pt0[2], pt0[3]), sy0 = screenYImpl(pt0[0], pt0[1], pt0[2], pt0[3]);
              float sx1 = screenXImpl(pt1[0], pt1[1], pt1[2], pt1[3]), sy1 = screenYImpl(pt1[0], pt1[1], pt1[2], pt1[3]);
              float sx2 = screenXImpl(pt2[0], pt2[1], pt2[2], pt2[3]), sy2 = screenYImpl(pt2[0], pt2[1], pt2[2], pt2[3]);              
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
    }
        
    raw.endShape();
  }

  
  protected void flushLines() {
    updateLineBuffers();

    LineShader shader = getLineShader();
    shader.start();

    IndexCache cache = tessGeo.lineIndexCache;
    for (int n = 0; n < cache.size; n++) {     
      int ioffset = cache.indexOffset[n];
      int icount = cache.indexCount[n];
      int voffset = cache.vertexOffset[n];
      
      shader.setVertexAttribute(glLineVertexBufferID, 4, PGL.GL_FLOAT, 0, 4 * voffset * PGL.SIZEOF_FLOAT);
      shader.setColorAttribute(glLineColorBufferID, 4, PGL.GL_UNSIGNED_BYTE, 0, 4 * voffset * PGL.SIZEOF_BYTE);
      shader.setLineAttribute(glLineAttribBufferID, 4, PGL.GL_FLOAT, 0, 4 * voffset * PGL.SIZEOF_FLOAT);

      pgl.glBindBuffer(PGL.GL_ELEMENT_ARRAY_BUFFER, glLineIndexBufferID);
      pgl.glDrawElements(PGL.GL_TRIANGLES, icount, PGL.INDEX_TYPE, ioffset * PGL.SIZEOF_INDEX);
      pgl.glBindBuffer(PGL.GL_ELEMENT_ARRAY_BUFFER, 0);
    }
    
    shader.stop();
    unbindLineBuffers();
  }

  
  void rawLines() {
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
    for (int n = 0; n < cache.size; n++) {     
      int ioffset = cache.indexOffset[n];
      int icount = cache.indexCount[n];
      int voffset = cache.vertexOffset[n];    
          
      for (int ln = ioffset / 6; ln < (ioffset + icount) / 6; ln++) {
        // Each line segment is defined by six indices since its
        // formed by two triangles. We only need the first and last
        // vertices. 
        // This bunch of vertices could also be the bevel triangles,
        // with we detect this situation by looking at the line weight.
        int i0 = voffset + indices[6 * ln + 0];
        int i1 = voffset + indices[6 * ln + 5];
        float sw0 = 2 * attribs[4 * i0 + 3];
        float sw1 = 2 * attribs[4 * i1 + 3];
        
        if (zero(sw0)) continue; // Bevel triangles, skip. 
                
        float[] pt0 = {0, 0, 0, 0};
        float[] pt1 = {0, 0, 0, 0};        
        int argb0 = PGL.nativeToJavaARGB(color[i0]);
        int argb1 = PGL.nativeToJavaARGB(color[i1]);
          
        if (flushMode == FLUSH_CONTINUOUSLY || hints[DISABLE_TRANSFORM_CACHE]) {
          float[] src0 = {0, 0, 0, 0};
          float[] src1 = {0, 0, 0, 0};          
          PApplet.arrayCopy(vertices, 4 * i0, src0, 0, 4);
          PApplet.arrayCopy(vertices, 4 * i1, src1, 0, 4);          
          modelview.mult(src0, pt0);
          modelview.mult(src1, pt1);
        } else {
          PApplet.arrayCopy(vertices, 4 * i0, pt0, 0, 4);
          PApplet.arrayCopy(vertices, 4 * i1, pt1, 0, 4);
        }

        if (raw.is3D()) {
          raw.strokeWeight(sw0);
          raw.stroke(argb0);
          raw.vertex(pt0[X], pt0[Y], pt0[Z]);
          raw.strokeWeight(sw1);
          raw.stroke(argb1);
          raw.vertex(pt1[X], pt1[Y], pt1[Z]);
        } else if (raw.is2D()) {
          float sx0 = screenXImpl(pt0[0], pt0[1], pt0[2], pt0[3]), sy0 = screenYImpl(pt0[0], pt0[1], pt0[2], pt0[3]);
          float sx1 = screenXImpl(pt1[0], pt1[1], pt1[2], pt1[3]), sy1 = screenYImpl(pt1[0], pt1[1], pt1[2], pt1[3]);
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
  
  
  protected void flushPoints() {
    updatePointBuffers();

    PointShader shader = getPointShader();
    shader.start();

    IndexCache cache = tessGeo.pointIndexCache;
    for (int n = 0; n < cache.size; n++) {     
      int ioffset = cache.indexOffset[n];
      int icount = cache.indexCount[n];
      int voffset = cache.vertexOffset[n];

      shader.setVertexAttribute(glPointVertexBufferID, 4, PGL.GL_FLOAT, 0, 4 * voffset * PGL.SIZEOF_FLOAT);
      shader.setColorAttribute(glPointColorBufferID, 4, PGL.GL_UNSIGNED_BYTE, 0, 4 * voffset * PGL.SIZEOF_BYTE);
      shader.setPointAttribute(glPointAttribBufferID, 2, PGL.GL_FLOAT, 0, 2 * voffset * PGL.SIZEOF_FLOAT);

      pgl.glBindBuffer(PGL.GL_ELEMENT_ARRAY_BUFFER, glPointIndexBufferID);
      pgl.glDrawElements(PGL.GL_TRIANGLES, icount, PGL.INDEX_TYPE, ioffset * PGL.SIZEOF_INDEX);
      pgl.glBindBuffer(PGL.GL_ELEMENT_ARRAY_BUFFER, 0);
    }

    shader.stop();
    unbindPointBuffers();
  }


  void rawPoints() {
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
          perim = PApplet.max(MIN_POINT_ACCURACY, 
                              (int) (TWO_PI * weight / POINT_ACCURACY_FACTOR)) + 1;          
        } else {        // Square point
          weight = -size / 0.5f;
          perim = 5;          
        }
                
        int i0 = voffset + indices[3 * pt];
        int argb0 = PGL.nativeToJavaARGB(color[i0]);
        float[] pt0 = {0, 0, 0, 0};
        
        if (flushMode == FLUSH_CONTINUOUSLY || hints[DISABLE_TRANSFORM_CACHE]) {
          float[] src0 = {0, 0, 0, 0};          
          PApplet.arrayCopy(vertices, 4 * i0, src0, 0, 4);          
          modelview.mult(src0, pt0);
        } else {
          PApplet.arrayCopy(vertices, 4 * i0, pt0, 0, 4);
        }        
        
        if (raw.is3D()) {
          raw.strokeWeight(weight);
          raw.stroke(argb0);
          raw.vertex(pt0[X], pt0[Y], pt0[Z]);
        } else if (raw.is2D()) {
          float sx0 = screenXImpl(pt0[0], pt0[1], pt0[2], pt0[3]), sy0 = screenYImpl(pt0[0], pt0[1], pt0[2], pt0[3]);
          raw.strokeWeight(weight);
          raw.stroke(argb0);
          raw.vertex(sx0, sy0);     
        }         
        
        pt += perim;
      }
    }    
    
    raw.endShape();
  }
  
  
  //////////////////////////////////////////////////////////////

  // BEZIER CURVE VERTICES

  
  public void bezierVertex(float x2, float y2,
                           float x3, float y3,
                           float x4, float y4) {
    bezierVertexImpl(x2, y2, 0,
                     x3, y3, 0,
                     x4, y4, 0);
  }


  public void bezierVertex(float x2, float y2, float z2,
                           float x3, float y3, float z3,
                           float x4, float y4, float z4) {
    bezierVertexImpl(x2, y2, z2, 
                     x3, y3, z3, 
                     x4, y4, z4); 
  }


  protected void bezierVertexImpl(float x2, float y2, float z2,
                                  float x3, float y3, float z3,
                                  float x4, float y4, float z4) {
    inGeo.setMaterial(fillColor, strokeColor, strokeWeight,
                      ambientColor, specularColor, emissiveColor, shininess);
    inGeo.setNormal(normalX, normalY, normalZ);
    inGeo.addBezierVertex(x2, y2, z2,
                          x3, y3, z3,
                          x4, y4, z4,
                          fill, stroke, bezierDetail, vertexCode(), shape); 
                          
  }  
  

  public void quadraticVertex(float cx, float cy,
                              float x3, float y3) {
    quadraticVertexImpl(cx, cy, 0,
                        x3, y3, 0);
  }

  
  public void quadraticVertex(float cx, float cy, float cz,
                              float x3, float y3, float z3) {
    quadraticVertexImpl(cx, cy, cz,
                        x3, y3, z3);
  }

  
  protected void quadraticVertexImpl(float cx, float cy, float cz,
                                     float x3, float y3, float z3) {
    inGeo.setMaterial(fillColor, strokeColor, strokeWeight,
                      ambientColor, specularColor, emissiveColor, shininess);
    inGeo.setNormal(normalX, normalY, normalZ);    
    inGeo.addQuadraticVertex(cx, cy, cz,
                             x3, y3, z3,
                             fill, stroke, bezierDetail, vertexCode(), shape);    
  }

  
  //////////////////////////////////////////////////////////////

  // CATMULL-ROM CURVE VERTICES


  public void curveVertex(float x, float y) {
    curveVertexImpl(x, y, 0);
  }

  
  public void curveVertex(float x, float y, float z) {
    curveVertexImpl(x, y, z);
  }

  
  protected void curveVertexImpl(float x, float y, float z) {
    inGeo.setMaterial(fillColor, strokeColor, strokeWeight,
                      ambientColor, specularColor, emissiveColor, shininess);
    inGeo.setNormal(normalX, normalY, normalZ);
    inGeo.addCurveVertex(x, y, z,
                         fill, stroke, curveDetail, vertexCode(), shape); 
  }

  
  //////////////////////////////////////////////////////////////

  // POINT, LINE, TRIANGLE, QUAD

  
  public void point(float x, float y) {
    pointImpl(x, y, 0);  
  }

  
  public void point(float x, float y, float z) {
    pointImpl(x, y, z);
  }
  
  
  protected void pointImpl(float x, float y, float z) {
    beginShape(POINTS);
    defaultEdges = false;
    normalMode = NORMAL_MODE_SHAPE;
    inGeo.setMaterial(fillColor, strokeColor, strokeWeight,
                      ambientColor, specularColor, emissiveColor, shininess);
    inGeo.setNormal(normalX, normalY, normalZ);     
    inGeo.addPoint(x, y, z, fill, stroke);
    endShape();
  }

  
  public void line(float x1, float y1, float x2, float y2) {
    lineImpl(x1, y1, 0, x2, y2, 0);
  }

  
  public void line(float x1, float y1, float z1,
                   float x2, float y2, float z2) {
    lineImpl(x1, y1, z1, x2, y2, z2);
  }
  
  
  protected void lineImpl(float x1, float y1, float z1,
                          float x2, float y2, float z2) {
    beginShape(LINES);
    defaultEdges = false;
    normalMode = NORMAL_MODE_SHAPE;
    inGeo.setMaterial(fillColor, strokeColor, strokeWeight,
                      ambientColor, specularColor, emissiveColor, shininess);
    inGeo.setNormal(normalX, normalY, normalZ);
    inGeo.addLine(x1, y1, z1,
                  x2, y2, z2,
                  fill, stroke);
    endShape();
  }
  

  public void triangle(float x1, float y1, float x2, float y2,
                       float x3, float y3) {
    beginShape(TRIANGLES);
    defaultEdges = false;
    normalMode = NORMAL_MODE_SHAPE;
    inGeo.setMaterial(fillColor, strokeColor, strokeWeight,
                      ambientColor, specularColor, emissiveColor, shininess);
    inGeo.setNormal(normalX, normalY, normalZ);
    inGeo.addTriangle(x1, y1, 0,
                      x2, y2, 0,
                      x3, y3, 0,
                      fill, stroke);    
    endShape();
  }

  
  public void quad(float x1, float y1, float x2, float y2,
                   float x3, float y3, float x4, float y4) {
    beginShape(QUADS);
    defaultEdges = false;
    normalMode = NORMAL_MODE_SHAPE;
    inGeo.setMaterial(fillColor, strokeColor, strokeWeight,
                      ambientColor, specularColor, emissiveColor, shininess);
    inGeo.setNormal(normalX, normalY, normalZ);
    inGeo.addQuad(x1, y1, 0,
                  x2, y2, 0,
                  x3, y3, 0,
                  x4, y4, 0,
                  fill, stroke);
    endShape();
  }

  //////////////////////////////////////////////////////////////

  // RECT

  // public void rectMode(int mode)

  public void rect(float a, float b, float c, float d) {
    beginShape(QUADS);
    defaultEdges = false;
    normalMode = NORMAL_MODE_SHAPE;
    inGeo.setMaterial(fillColor, strokeColor, strokeWeight,
                      ambientColor, specularColor, emissiveColor, shininess);
    inGeo.setNormal(normalX, normalY, normalZ);
    inGeo.addRect(a, b, c, d,
                  fill, stroke, rectMode);    
    endShape();
  }

  
  public void rect(float a, float b, float c, float d,
                   float tl, float tr, float br, float bl) {
    beginShape(POLYGON);
    defaultEdges = false;
    normalMode = NORMAL_MODE_SHAPE;
    inGeo.setMaterial(fillColor, strokeColor, strokeWeight,
                      ambientColor, specularColor, emissiveColor, shininess);
    inGeo.setNormal(normalX, normalY, normalZ);
    inGeo.addRect(a, b, c, d,
                  tl, tr, br, bl,
                  fill, stroke, bezierDetail, rectMode);
    endShape(CLOSE);
  }
  
  // protected void rectImpl(float x1, float y1, float x2, float y2)

  //////////////////////////////////////////////////////////////

  // ELLIPSE

  // public void ellipseMode(int mode)


  public void ellipse(float a, float b, float c, float d) {
     beginShape(TRIANGLE_FAN);
     defaultEdges = false;
     normalMode = NORMAL_MODE_SHAPE;
     inGeo.setMaterial(fillColor, strokeColor, strokeWeight,
                       ambientColor, specularColor, emissiveColor, shininess);
     inGeo.setNormal(normalX, normalY, normalZ);     
     inGeo.addEllipse(a, b, c, d, fill, stroke, ellipseMode);
     endShape();
  }


  // public void ellipse(float a, float b, float c, float d)

  public void arc(float a, float b, float c, float d,
                  float start, float stop) {
    beginShape(TRIANGLE_FAN);
    defaultEdges = false;
    normalMode = NORMAL_MODE_SHAPE;
    inGeo.setMaterial(fillColor, strokeColor, strokeWeight,
                      ambientColor, specularColor, emissiveColor, shininess);
    inGeo.setNormal(normalX, normalY, normalZ);     
    inGeo.addArc(a, b, c, d, start, stop, fill, stroke, ellipseMode);
    endShape();    
  }


  //////////////////////////////////////////////////////////////

  // BOX

  // public void box(float size)
  
  public void box(float w, float h, float d) {
    beginShape(QUADS);
    defaultEdges = false;
    normalMode = NORMAL_MODE_VERTEX;
    inGeo.setMaterial(fillColor, strokeColor, strokeWeight,
                      ambientColor, specularColor, emissiveColor, shininess);    
    inGeo.addBox(w, h, d, fill, stroke);
    endShape();
  }

  //////////////////////////////////////////////////////////////

  // SPHERE

  // public void sphereDetail(int res)

  // public void sphereDetail(int ures, int vres)
  
  public void sphere(float r) {
    beginShape(TRIANGLES);
    defaultEdges = false;
    normalMode = NORMAL_MODE_VERTEX;
    inGeo.setMaterial(fillColor, strokeColor, strokeWeight,
                      ambientColor, specularColor, emissiveColor, shininess);    
    int[] indices = inGeo.addSphere(r, sphereDetailU, sphereDetailV, fill, stroke);    
    endShape(indices);
  }

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

    if (maxSamples < level) {
      PGraphics.showWarning("Smooth level " + level + 
                            " is not supported by the hardware. Using " + 
                            maxSamples + " instead.");
      level = maxSamples;
    }

    if (quality != level) {
      quality = level;
      if (quality == 1) {
        quality = 0;
      }
      // This will trigger a surface restart next time
      // requestDraw() is called.
      pgl.initialized = false;
    }
  }


  public void noSmooth() {
    smooth = false;

    if (1 < quality) {
      quality = 0;
      // This will trigger a surface restart next time
      // requestDraw() is called.
      pgl.initialized = false;
    }
  }


  //////////////////////////////////////////////////////////////

  // SHAPE

  // public void shapeMode(int mode)


  public void shape(PShape shape, float x, float y, float z) {
    if (shape.isVisible()) { // don't do expensive matrix ops if invisible
      flush();

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


  public void shape(PShape shape, float x, float y, float z, float c, float d, float e) {
    if (shape.isVisible()) { // don't do expensive matrix ops if invisible
      flush();

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

  // SHAPE I/O

  
  public PShape loadShape(String filename) {
    String ext = PApplet.getExtension(filename);    
    if (PGraphics2D.isSupportedExtension(ext)) {
      return PGraphics2D.loadShapeImpl(this, filename, ext);
    } if (PGraphics3D.isSupportedExtension(ext)) {
      return PGraphics3D.loadShapeImpl(this, filename, ext);
    } else {
      PGraphics.showWarning("Unsupported format");
      return null;
    }
  }


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
    textTex = (PFontTexture)textFont.getCache(pgPrimary);
    if (textTex == null) {
      textTex = new PFontTexture(parent, textFont, maxTextureSize, maxTextureSize, is3D());
      textFont.setCache(this, textTex);
    } else {
      if (textTex.contextIsOutdated()) {
        textTex = new PFontTexture(parent, textFont, PApplet.min(PGL.MAX_FONT_TEX_SIZE, maxTextureSize),
                                                     PApplet.min(PGL.MAX_FONT_TEX_SIZE, maxTextureSize), is3D());
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
    translateImpl(tx, ty, 0);
  }


  public void translate(float tx, float ty, float tz) {
    translateImpl(tx, ty, tz);
  }

  
  protected void translateImpl(float tx, float ty, float tz) {
    if (hints[DISABLE_TRANSFORM_CACHE]) {
      flush();
    }

    modelview.translate(tx, ty, tz);
    invTranslate(modelviewInv, tx, ty, tz);
    projmodelview.translate(tx, ty, tz);
  }


  static protected void invTranslate(PMatrix3D matrix, float tx, float ty, float tz) {
    matrix.preApply(1, 0, 0, -tx,
                    0, 1, 0, -ty,
                    0, 0, 1, -tz,
                    0, 0, 0, 1);
  }


  /**
   * Two dimensional rotation. Same as rotateZ (this is identical to a 3D
   * rotation along the z-axis) but included for clarity -- it'd be weird for
   * people drawing 2D graphics to be using rotateZ. And they might kick our a--
   * for the confusion.
   */
  public void rotate(float angle) {
    rotateImpl(angle, 0, 0, 1);
  }


  public void rotateX(float angle) {
    rotateImpl(angle, 1, 0, 0);
  }


  public void rotateY(float angle) {
    rotateImpl(angle, 0, 1, 0);
  }


  public void rotateZ(float angle) {
    rotateImpl(angle, 0, 0, 1);
  }


  /**
   * Rotate around an arbitrary vector, similar to glRotate(), except that it
   * takes radians (instead of degrees).
   */
  public void rotate(float angle, float v0, float v1, float v2) {
    rotateImpl(angle, v0, v1, v2);
  }


  protected void rotateImpl(float angle, float v0, float v1, float v2) {
    if (hints[DISABLE_TRANSFORM_CACHE]) {
      flush();
    }

    float norm2 = v0 * v0 + v1 * v1 + v2 * v2;
    if (zero(norm2)) {
      // The vector is zero, cannot apply rotation.
      return;
    }    
    
    if (diff(norm2, 1)) {
      // The rotation vector is not normalized.
      float norm = PApplet.sqrt(norm2);
      v0 /= norm;
      v1 /= norm;
      v2 /= norm;
    }    
    
    modelview.rotate(angle, v0, v1, v2);
    invRotate(modelviewInv, angle, v0, v1, v2);
    calcProjmodelview(); // Possibly cheaper than doing projmodelview.rotate()    
  }
  
  
  static private void invRotate(PMatrix3D matrix, float angle, float v0, float v1, float v2) {
    float c = PApplet.cos(-angle);
    float s = PApplet.sin(-angle);
    float t = 1.0f - c;

    matrix.preApply((t*v0*v0) + c, (t*v0*v1) - (s*v2), (t*v0*v2) + (s*v1), 0,
                    (t*v0*v1) + (s*v2), (t*v1*v1) + c, (t*v1*v2) - (s*v0), 0,
                    (t*v0*v2) - (s*v1), (t*v1*v2) + (s*v0), (t*v2*v2) + c, 0,
                    0, 0, 0, 1);
  }


  /**
   * Same as scale(s, s, s).
   */
  public void scale(float s) {
    scaleImpl(s, s, s);
  }


  /**
   * Same as scale(sx, sy, 1).
   */
  public void scale(float sx, float sy) {
    scaleImpl(sx, sy, 1);
  }

  
  /**
   * Scale in three dimensions.
   */
  public void scale(float sx, float sy, float sz) {
    scaleImpl(sx, sy, sz);
  }
  
  /**
   * Scale in three dimensions.
   */
  protected void scaleImpl(float sx, float sy, float sz) {
    if (hints[DISABLE_TRANSFORM_CACHE]) {
      flush();
    }

    modelview.scale(sx, sy, sz);
    invScale(modelviewInv, sx, sy, sz);
    projmodelview.scale(sx, sy, sz);
  }


  static protected void invScale(PMatrix3D matrix, float x, float y, float z) {
    matrix.preApply(1/x, 0, 0, 0,  0, 1/y, 0, 0,  0, 0, 1/z, 0,  0, 0, 0, 1);
  }


  public void shearX(float angle) {
    float t = (float) Math.tan(angle);
    applyMatrixImpl(1, t, 0, 0,
                    0, 1, 0, 0,
                    0, 0, 1, 0,
                    0, 0, 0, 1);
  }


  public void shearY(float angle) {
    float t = (float) Math.tan(angle);
    applyMatrixImpl(1, 0, 0, 0,
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
    applyMatrixImpl(source.m00, source.m01, 0, source.m02,
                    source.m10, source.m11, 0, source.m12,
                             0,          0, 1, 0,
                             0,          0, 0, 1);
  }


  public void applyMatrix(float n00, float n01, float n02,
                          float n10, float n11, float n12) {
    applyMatrixImpl(n00, n01, 0, n02,
                    n10, n11, 0, n12,
                      0,   0, 1,   0,
                      0,   0, 0,   1);
  }


  public void applyMatrix(PMatrix3D source) {
    applyMatrixImpl(source.m00, source.m01, source.m02, source.m03,
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
    applyMatrixImpl(n00, n01, n02, n03,
                    n10, n11, n12, n13,
                    n20, n21, n22, n23,
                    n30, n31, n32, n33);    
  }
  
  
  protected void applyMatrixImpl(float n00, float n01, float n02, float n03,
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

  // Some float math utilities

  
  protected static boolean same(float a, float b) {
    return Math.abs(a - b) < PGL.FLOAT_EPS;
  }
  
  
  protected static boolean diff(float a, float b) {
    return PGL.FLOAT_EPS <= Math.abs(a - b);
  }

  
  protected static boolean zero(float a) {
    return Math.abs(a) < PGL.FLOAT_EPS;
  }  
  
  
  protected static boolean nonZero(float a) {
    return PGL.FLOAT_EPS <= Math.abs(a);
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
    if (nonZero(mag)) {
      z0 /= mag;
      z1 /= mag;
      z2 /= mag;
    }
    cameraEyeX = eyeX;
    cameraEyeY = eyeY;
    cameraEyeZ = eyeZ;

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
    if (nonZero(mag)) {
      x0 /= mag;
      x1 /= mag;
      x2 /= mag;
    }

    mag = PApplet.sqrt(y0 * y0 + y1 * y1 + y2 * y2);
    if (nonZero(mag)) {
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
    ortho(0, width, 0, height, cameraNear, cameraFar);
  }


  /**
   * Calls ortho() with the specified size of the viewing volume along
   * the X and Z directions.
   */
  public void ortho(float left, float right,
                    float bottom, float top) {
    ortho(left, right, bottom, top, cameraNear, cameraFar);
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
    return screenXImpl(x, y, 0);
  }


  public float screenY(float x, float y) {
    return screenYImpl(x, y, 0);
  }


  public float screenX(float x, float y, float z) {
    return screenXImpl(x, y, z);
  }
  
  
  public float screenY(float x, float y, float z) {
    return screenYImpl(x, y, z);
  }
  
  
  public float screenZ(float x, float y, float z) {
    return screenZImpl(x, y, z);   
  }
  
  
  protected float screenXImpl(float x, float y, float z) {
    float ax = modelview.m00 * x + modelview.m01 * y + modelview.m02 * z + modelview.m03;
    float ay = modelview.m10 * x + modelview.m11 * y + modelview.m12 * z + modelview.m13;
    float az = modelview.m20 * x + modelview.m21 * y + modelview.m22 * z + modelview.m23;
    float aw = modelview.m30 * x + modelview.m31 * y + modelview.m32 * z + modelview.m33;
    return screenXImpl(ax, ay, az, aw);
  }

  
  protected float screenXImpl(float x, float y, float z, float w) {
    float ox = projection.m00 * x + projection.m01 * y + projection.m02 * z + projection.m03 * w;
    float ow = projection.m30 * x + projection.m31 * y + projection.m32 * z + projection.m33 * w;

    if (nonZero(ow)) {
      ox /= ow;
    }
    float sx = width * (1 + ox) / 2.0f;
    return sx;
  }
  
  
  protected float screenYImpl(float x, float y, float z) {
    float ax = modelview.m00 * x + modelview.m01 * y + modelview.m02 * z + modelview.m03;
    float ay = modelview.m10 * x + modelview.m11 * y + modelview.m12 * z + modelview.m13;
    float az = modelview.m20 * x + modelview.m21 * y + modelview.m22 * z + modelview.m23;
    float aw = modelview.m30 * x + modelview.m31 * y + modelview.m32 * z + modelview.m33;
    return screenYImpl(ax, ay, az, aw);
  }


  protected float screenYImpl(float x, float y, float z, float w) {
    float oy = projection.m10 * x + projection.m11 * y + projection.m12 * z + projection.m13 * w;
    float ow = projection.m30 * x + projection.m31 * y + projection.m32 * z + projection.m33 * w;

    if (nonZero(ow)) {
      oy /= ow;
    }
    float sy = height * (1 + oy) / 2.0f;
    // Turning value upside down because of Processing's inverted Y axis.
    sy = height - sy;
    return sy;    
  }
  
    
  protected float screenZImpl(float x, float y, float z) {
    float ax = modelview.m00 * x + modelview.m01 * y + modelview.m02 * z + modelview.m03;
    float ay = modelview.m10 * x + modelview.m11 * y + modelview.m12 * z + modelview.m13;
    float az = modelview.m20 * x + modelview.m21 * y + modelview.m22 * z + modelview.m23;
    float aw = modelview.m30 * x + modelview.m31 * y + modelview.m32 * z + modelview.m33;
    return screenZImpl(ax, ay, az, aw);
  }

  
  protected float screenZImpl(float x, float y, float z, float w) {
    float oz = projection.m20 * x + projection.m21 * y + projection.m22 * z + projection.m23 * w;
    float ow = projection.m30 * x + projection.m31 * y + projection.m32 * z + projection.m33 * w;

    if (nonZero(ow)) {
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

    return nonZero(ow) ? ox / ow : ox;
  }


  public float modelY(float x, float y, float z) {
    float ax = modelview.m00 * x + modelview.m01 * y + modelview.m02 * z + modelview.m03;
    float ay = modelview.m10 * x + modelview.m11 * y + modelview.m12 * z + modelview.m13;
    float az = modelview.m20 * x + modelview.m21 * y + modelview.m22 * z + modelview.m23;
    float aw = modelview.m30 * x + modelview.m31 * y + modelview.m32 * z + modelview.m33;

    float oy = cameraInv.m10 * ax + cameraInv.m11 * ay + cameraInv.m12 * az + cameraInv.m13 * aw;
    float ow = cameraInv.m30 * ax + cameraInv.m31 * ay + cameraInv.m32 * az + cameraInv.m33 * aw;

    return nonZero(ow) ? oy / ow : oy;
  }


  public float modelZ(float x, float y, float z) {
    float ax = modelview.m00 * x + modelview.m01 * y + modelview.m02 * z + modelview.m03;
    float ay = modelview.m10 * x + modelview.m11 * y + modelview.m12 * z + modelview.m13;
    float az = modelview.m20 * x + modelview.m21 * y + modelview.m22 * z + modelview.m23;
    float aw = modelview.m30 * x + modelview.m31 * y + modelview.m32 * z + modelview.m33;

    float oz = cameraInv.m20 * ax + cameraInv.m21 * ay + cameraInv.m22 * az + cameraInv.m23 * aw;
    float ow = cameraInv.m30 * ax + cameraInv.m31 * ay + cameraInv.m32 * az + cameraInv.m33 * aw;

    return nonZero(ow) ? oz / ow : oz;
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

  // FILL COLOR  
  
  
  protected void fillFromCalc() {
    super.fillFromCalc();
    
    if (!setAmbient) {
      // Setting the ambient color from the current fill
      // is what the old P3D did and allows to have an 
      // default ambient color when the user doesn't specify
      // it explicitly.
      ambientFromCalc();
      setAmbient = false;
    }
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
    if (0 < parent.frameCount) {
      clearColorBuffer = true;
    }
  }


  protected void backgroundImpl() {
    flush();

    pgl.glDepthMask(true);
    pgl.glClearColor(0, 0, 0, 0);
    pgl.glClear(PGL.GL_DEPTH_BUFFER_BIT);
    if (hints[DISABLE_DEPTH_MASK]) {
      pgl.glDepthMask(false);
    } else {
      pgl.glDepthMask(true);
    }

    pgl.glClearColor(backgroundR, backgroundG, backgroundB, backgroundA);
    pgl.glClear(PGL.GL_COLOR_BUFFER_BIT);
    if (0 < parent.frameCount) {
      clearColorBuffer = true;
    }
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
      int err = pgl.glGetError();
      if (err != 0) {
        String errString = pgl.glErrorString(err);
        String msg = "OpenGL error " + err + " at " + where + ": " + errString;
        PGraphics.showWarning(msg);
      }
    }
  }


  //////////////////////////////////////////////////////////////

  // RENDERER SUPPORT QUERIES

  // public boolean displayable()


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


  // Initializes the pixels array, copying the current contents of the
  // color buffer into it.
  public void loadPixels() {
    boolean needEndDraw = false;
    if (!drawing) {
      beginDraw();
      needEndDraw = true;
    }
    
    if (!setgetPixels) {
      // Draws any remaining geometry in case the user is still not
      // setting/getting new pixels.
      flush();
    }

    allocatePixels();

    if (!setgetPixels) {
      readPixels();

      if (primarySurface) {
        loadTextureImpl(POINT, false);
        pixelsToTexture();
      }
    }
    
    if (needEndDraw) {
      endDraw();
    }
  }

  
  protected void saveSurfaceToPixels() {
    allocatePixels();      
    readPixels();      
  }
  
  
  protected void restoreSurfaceFromPixels() {
    drawPixels(0, 0, width, height);
  }

  
  protected void allocatePixels() {
    if ((pixels == null) || (pixels.length != width * height)) {
      pixels = new int[width * height];
      pixelBuffer = IntBuffer.wrap(pixels);
    }    
  }  
  
  
  protected void readPixels() {
    beginPixelsOp(OP_READ);
    pixelBuffer.rewind();
    pgl.glReadPixels(0, 0, width, height, PGL.GL_RGBA, PGL.GL_UNSIGNED_BYTE, pixelBuffer);
    endPixelsOp();
    
    PGL.nativeToJavaARGB(pixels, width, height);    
  }
  
  
  protected void drawPixels(int x, int y, int w, int h) {
    int i0 = y * width + x;
    int len = w * h;
      
    if (rgbaPixels == null || rgbaPixels.length < len) {
      rgbaPixels = new int[len];
    }

    PApplet.arrayCopy(pixels, i0, rgbaPixels, 0, len);
    PGL.javaToNativeARGB(rgbaPixels, w, h);
    
    // Copying pixel buffer to screen texture...
    if (primarySurface) {
      loadTextureImpl(POINT, false);  // (first making sure that the screen texture is valid).
    }
    pgl.copyToTexture(texture.glTarget, texture.glFormat, texture.glID,
                      x, y, w, h, IntBuffer.wrap(rgbaPixels));

    if (primarySurface || offscreenMultisample) {
      // ...and drawing the texture to screen... but only
      // if we are on the primary surface or we have
      // multisampled FBO. Why? Because in the case of non-
      // multisampled FBO, texture is actually the color buffer
      // used by the color FBO, so with the copy operation we
      // should be done updating the (off)screen buffer.
      beginPixelsOp(OP_WRITE);
      drawTexture(x, y, w, h);
      endPixelsOp();
    }
  }
  
  
  //////////////////////////////////////////////////////////////

  // GET/SET PIXELS


  public int get(int x, int y) {
    loadPixels();
    setgetPixels = true;
    return super.get(x, y);
  }


  protected PImage getImpl(int x, int y, int w, int h) {
    loadPixels();
    setgetPixels = true;
    return super.getImpl(x, y, w, h);
  }


  public void set(int x, int y, int argb) {
    loadPixels();
    setgetPixels = true;
    super.set(x, y, argb);
  }


  protected void setImpl(int dx, int dy, int sx, int sy, int sw, int sh,
                         PImage src) {
    loadPixels();
    setgetPixels = true;
    super.setImpl(dx, dy, sx, sy, sw, sh, src);
  }


  //////////////////////////////////////////////////////////////

  // LOAD/UPDATE TEXTURE


  // Copies the contents of the color buffer into the pixels
  // array, and then the pixels array into the screen texture.
  public void loadTexture() {
    if (primarySurface) {
      loadTextureImpl(POINT, false);
      loadPixels();
      pixelsToTexture();
    }
  }


  // Draws wherever it is in the screen texture right now to the screen.
  public void updateTexture() {
    flush();
    beginPixelsOp(OP_WRITE);
    drawTexture();
    endPixelsOp();
  }


  protected void loadTextureImpl(int sampling, boolean mipmap) {
    if (width == 0 || height == 0) return;
    if (texture == null || texture.contextIsOutdated()) {
      Texture.Parameters params = new Texture.Parameters(ARGB, sampling, mipmap);
      texture = new Texture(parent, width, height, params);
      texture.setFlippedY(true);
      this.setCache(pgPrimary, texture);
      this.setParams(pgPrimary, params);
    }
  }


  protected void drawTexture() {
    pgl.drawTexture(texture.glTarget, texture.glID,
                    texture.glWidth, texture.glHeight,
                    0, 0, width, height);
  }


  protected void drawTexture(int x, int y, int w, int h) {
    pgl.drawTexture(texture.glTarget, texture.glID,
                    texture.glWidth, texture.glHeight,
                    x, y, x + w, y + h);
  }


  protected void pixelsToTexture() {
    texture.set(pixels);
  }


  protected void textureToPixels() {
    texture.get(pixels);
  }
  
  
  //////////////////////////////////////////////////////////////

  // IMAGE CONVERSION


  static public void nativeToJavaRGB(PImage image) {
    if (image.pixels != null) {
      PGL.nativeToJavaRGB(image.pixels, image.width, image.height);
    }
  }


  static public void nativeToJavaARGB(PImage image) {
    if (image.pixels != null) {
      PGL.nativeToJavaARGB(image.pixels, image.width, image.height);
    }
  }


  static public void javaToNativeRGB(PImage image) {
    if (image.pixels != null) {
      PGL.javaToNativeRGB(image.pixels, image.width, image.height);
    }
  }


  static public void javaToNativeARGB(PImage image) {
    if (image.pixels != null) {
      PGL.javaToNativeARGB(image.pixels, image.width, image.height);
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

  // TEXTURE UTILS


  /**
   * This utility method returns the texture associated to the renderer's.
   * drawing surface, making sure is updated to reflect the current contents
   * off the screen (or offscreen drawing surface).
   */
  public Texture getTexture() {
    loadTexture();
    return texture;
  }


  /**
   * This utility method returns the texture associated to the image.
   * creating and/or updating it if needed.
   *
   * @param img the image to have a texture metadata associated to it
   */
  public Texture getTexture(PImage img) {
    Texture tex = (Texture)img.getCache(pgPrimary);
    if (tex == null) {
      tex = addTexture(img);
    } else {
      if (tex.contextIsOutdated()) {
        tex = addTexture(img);
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
   * This utility method creates a texture for the provided image, and adds it
   * to the metadata cache of the image.
   * @param img the image to have a texture metadata associated to it
   */
  protected Texture addTexture(PImage img) {
    Texture.Parameters params = (Texture.Parameters)img.getParams(pgPrimary);
    if (params == null) {
      params = new Texture.Parameters();   
      if (hints[DISABLE_TEXTURE_MIPMAPS]) {
        params.mipmaps = false;
      } else {
        params.mipmaps = true;
      }      
      if (textureQuality == Texture.LOW) {
        params.sampling = POINT;  
      } else if (textureQuality == Texture.MEDIUM) {
        params.sampling = Texture.LINEAR;  
      } else if (textureQuality == Texture.HIGH) {
        params.sampling = Texture.BILINEAR;
      } else if (textureQuality == Texture.BEST) {
        if (params.mipmaps) {
          params.sampling = Texture.TRILINEAR;
        } else {
          params.sampling = Texture.BILINEAR;
          PGraphics.showWarning("BEST texture quality requires mipmaps, will switch to HIGH.");
        }        
      }         
      params.wrapU = textureWrap;
      params.wrapV = textureWrap;
      img.setParams(pgPrimary, params);
    }
    if (img.parent == null) {
      img.parent = parent;
    }
    Texture tex = new Texture(img.parent, img.width, img.height, params);
    img.loadPixels();
    if (img.pixels != null) tex.set(img.pixels);
    img.setCache(pgPrimary, tex);
    return tex;
  }


  protected PImage wrapTexture(Texture tex) {
    // We don't use the PImage(int width, int height, int mode) constructor to
    // avoid initializing the pixels array.
    PImage img = new PImage();
    img.parent = parent;
    img.width = tex.width;
    img.height = tex.height;
    img.format = ARGB;
    img.setCache(pgPrimary, tex);
    return img;
  }


  protected void updateTexture(PImage img, Texture tex) {
    if (tex != null) {
      int x = img.getModifiedX1();
      int y = img.getModifiedY1();
      int w = img.getModifiedX2() - x + 1;
      int h = img.getModifiedY2() - y + 1;
      tex.set(img.pixels, x, y, w, h, img.format);
    }
    img.setModified(false);
  }


  //////////////////////////////////////////////////////////////

  // RESIZE


  public void resize(int wide, int high) {
    PGraphics.showMethodWarning("resize");
  }


  //////////////////////////////////////////////////////////////

  // INITIALIZATION ROUTINES


  protected void initPrimary() {
    pgl.initPrimarySurface(quality);
    if (pgPrimary == null) {
      pgPrimary = this;
    }
  }


  protected void initOffscreen() {
    // Getting the context and capabilities from the main renderer.
    pgPrimary = (PGraphicsOpenGL)parent.g;
    pgl.initOffscreenSurface(pgPrimary.pgl);
    pgl.updateOffscreen(pgPrimary.pgl);
    
    loadTextureImpl(Texture.BILINEAR, false);
    
    // In case of reinitialization (for example, when the smooth level
    // is changed), we make sure that all the OpenGL resources associated
    // to the surface are released by calling delete().
    if (offscreenFramebuffer != null) {
      offscreenFramebuffer.release();
    }
    if (offscreenFramebufferMultisample != null) {
      offscreenFramebufferMultisample.release();
    }

    if (PGraphicsOpenGL.fboMultisampleSupported && 1 < quality) {
      offscreenFramebufferMultisample = new FrameBuffer(parent, texture.glWidth, texture.glHeight, quality, 0,
                                                         depthBits, stencilBits,
                                                         depthBits == 24 && stencilBits == 8 && packedDepthStencilSupported, false);

      offscreenFramebufferMultisample.clear();
      offscreenMultisample = true;

      // The offscreen framebuffer where the multisampled image is finally drawn to doesn't
      // need depth and stencil buffers since they are part of the multisampled framebuffer.
      offscreenFramebuffer = new FrameBuffer(parent, texture.glWidth, texture.glHeight, 1, 1,
                                              0, 0,
                                              false, false);

    } else {
      quality = 0;
      offscreenFramebuffer = new FrameBuffer(parent, texture.glWidth, texture.glHeight, 1, 1,
                                              depthBits, stencilBits,
                                              depthBits == 24 && stencilBits == 8 && packedDepthStencilSupported, false);
      offscreenMultisample = false;
    }

    offscreenFramebuffer.setColorBuffer(texture);
    offscreenFramebuffer.clear();
  }


  protected void getGLParameters() {
    OPENGL_VENDOR     = pgl.glGetString(PGL.GL_VENDOR);
    OPENGL_RENDERER   = pgl.glGetString(PGL.GL_RENDERER);
    OPENGL_VERSION    = pgl.glGetString(PGL.GL_VERSION);
    OPENGL_EXTENSIONS = pgl.glGetString(PGL.GL_EXTENSIONS);
    GLSL_VERSION      = pgl.glGetString(PGL.GL_SHADING_LANGUAGE_VERSION);

    npotTexSupported            = -1 < OPENGL_EXTENSIONS.indexOf("texture_non_power_of_two");
    autoMipmapGenSupported      = -1 < OPENGL_EXTENSIONS.indexOf("generate_mipmap");
    fboMultisampleSupported     = -1 < OPENGL_EXTENSIONS.indexOf("framebuffer_multisample");
    packedDepthStencilSupported = -1 < OPENGL_EXTENSIONS.indexOf("packed_depth_stencil");

    try {
      pgl.glBlendEquation(PGL.GL_FUNC_ADD);
      blendEqSupported = true;
    } catch (UnsupportedOperationException e) {
      blendEqSupported = false;
    }

    int temp[] = new int[2];

    pgl.glGetIntegerv(PGL.GL_MAX_TEXTURE_SIZE, temp, 0);
    maxTextureSize = temp[0];

    pgl.glGetIntegerv(PGL.GL_MAX_SAMPLES, temp, 0);
    maxSamples = temp[0];

    pgl.glGetIntegerv(PGL.GL_ALIASED_LINE_WIDTH_RANGE, temp, 0);
    maxLineWidth = temp[1];

    pgl.glGetIntegerv(PGL.GL_ALIASED_POINT_SIZE_RANGE, temp, 0);
    maxPointSize = temp[1];

    pgl.glGetIntegerv(PGL.GL_DEPTH_BITS, temp, 0);
    depthBits = temp[0];

    pgl.glGetIntegerv(PGL.GL_STENCIL_BITS, temp, 0);
    stencilBits = temp[0];

    glParamsRead = true;
  }


  //////////////////////////////////////////////////////////////

  // SHADER HANDLING


  public PShader loadShader(String vertFilename, String fragFilename, int kind) {
    if (kind == FLAT_SHADER) {
      return new PolyFlatShader(parent, vertFilename, fragFilename);
    } else if (kind == LIGHT_SHADER) {
      return new PolyLightShader(parent, vertFilename, fragFilename);
    } else if (kind == TEXTURE_SHADER) {
      return new PolyTexShader(parent, vertFilename, fragFilename);
    } else if (kind == FULL_SHADER) {
      return new PolyFullShader(parent, vertFilename, fragFilename);
    } else if (kind == LINE3D_SHADER) {
      return new LineShader(parent, vertFilename, fragFilename);
    } else if (kind == POINT3D_SHADER) {
      return new PointShader(parent, vertFilename, fragFilename);
    } else {
      PGraphics.showWarning("Wrong shader type");
      return null;
    }
  }


  public PShader loadShader(String fragFilename, int kind) {
    PShader shader;
    if (kind == FLAT_SHADER) {
      shader = new PolyFlatShader(parent);
      shader.setVertexShader(defPolyFlatShaderVertURL);
    } else if (kind == LIGHT_SHADER) {
      shader = new PolyLightShader(parent);
      shader.setVertexShader(defPolyLightShaderVertURL);
    } else if (kind == TEXTURE_SHADER) {
      shader = new PolyTexShader(parent);
      shader.setVertexShader(defPolyTexShaderVertURL);
    } else if (kind == FULL_SHADER) {
      shader = new PolyFullShader(parent);
      shader.setVertexShader(defPolyFullShaderVertURL);
    } else if (kind == LINE3D_SHADER) {
      shader = new LineShader(parent);
      shader.setVertexShader(defLineShaderVertURL);
    } else if (kind == POINT3D_SHADER) {
      shader = new PointShader(parent);
      shader.setVertexShader(defPointShaderVertURL);
    } else {
      PGraphics.showWarning("Wrong shader type");
      return null;
    }
    shader.setFragmentShader(fragFilename);
    return shader;
  }


  public void setShader(PShader shader, int kind) {
    flush(); // Flushing geometry with a different shader.
    if (kind == FLAT_SHADER) {
      polyFlatShader = (PolyFlatShader) shader;
    } else if (kind == LIGHT_SHADER) {
      polyLightShader = (PolyLightShader) shader;
    } else if (kind == TEXTURE_SHADER) {
      polyTexShader = (PolyTexShader) shader;
    } else if (kind == FULL_SHADER) {
      polyFullShader = (PolyFullShader) shader;
    } else if (kind == LINE3D_SHADER) {
      lineShader = (LineShader) shader;
    } else if (kind == POINT3D_SHADER) {
      pointShader = (PointShader) shader;
    } else {
      PGraphics.showWarning("Wrong shader type");
    }
  }


  public void defaultShader(int kind) {
    flush(); // Flushing geometry with a different shader.
    if (kind == FLAT_SHADER) {
      if (defPolyFlatShader == null || defPolyFlatShader.contextIsOutdated()) {
        defPolyFlatShader = new PolyFlatShader(parent, defPolyFlatShaderVertURL, defPolyNoTexShaderFragURL);
      }
      polyFlatShader = defPolyFlatShader;
    } else if (kind == LIGHT_SHADER) {
      if (defPolyLightShader == null || defPolyLightShader.contextIsOutdated()) {
        defPolyLightShader = new PolyLightShader(parent, defPolyLightShaderVertURL, defPolyNoTexShaderFragURL);
      }
      polyLightShader = defPolyLightShader;
    } else if (kind == TEXTURE_SHADER) {
      if (defPolyTexShader == null || defPolyTexShader.contextIsOutdated()) {
        defPolyTexShader = new PolyTexShader(parent, defPolyTexShaderVertURL, defPolyTexShaderFragURL);
      }
      polyTexShader = defPolyTexShader;
    } else if (kind == FULL_SHADER) {
      if (defPolyFullShader == null || defPolyFullShader.contextIsOutdated()) {
        defPolyFullShader = new PolyFullShader(parent, defPolyFullShaderVertURL, defPolyTexShaderFragURL);
      }
      polyFullShader = defPolyFullShader;
    } else if (kind == LINE3D_SHADER) {
      if (defLineShader == null || defLineShader.contextIsOutdated()) {
        defLineShader = new LineShader(parent, defLineShaderVertURL, defLineShaderFragURL);
      }
      lineShader = defLineShader;
    } else if (kind == POINT3D_SHADER) {
      if (defPointShader == null || defPointShader.contextIsOutdated()) {
        defPointShader = new PointShader(parent, defPointShaderVertURL, defPointShaderFragURL);
      }
      pointShader = defPointShader;
    } else {
      PGraphics.showWarning("Wrong shader type");
    }
  }


  protected PolyShader getPolyShader(boolean lit, boolean tex) {
    PolyShader shader;
    if (lit) {
      if (tex) {
        if (defPolyFullShader == null || defPolyFullShader.contextIsOutdated()) {
          defPolyFullShader = new PolyFullShader(parent, defPolyFullShaderVertURL, defPolyTexShaderFragURL);
        }
        if (polyFullShader == null || polyFullShader.contextIsOutdated()) {
          polyFullShader = defPolyFullShader;
        }
        shader = polyFullShader;
      } else {
        if (defPolyLightShader == null || defPolyLightShader.contextIsOutdated()) {
          defPolyLightShader = new PolyLightShader(parent, defPolyLightShaderVertURL, defPolyNoTexShaderFragURL);
        }
        if (polyLightShader == null || polyLightShader.contextIsOutdated()) {
          polyLightShader = defPolyLightShader;
        }
        shader = polyLightShader;
      }
    } else {
      if (tex) {
        if (defPolyTexShader == null || defPolyTexShader.contextIsOutdated()) {
          defPolyTexShader = new PolyTexShader(parent, defPolyTexShaderVertURL, defPolyTexShaderFragURL);
        }
        if (polyTexShader == null || polyTexShader.contextIsOutdated()) {
          polyTexShader = defPolyTexShader;
        }
        shader = polyTexShader;
      } else {
        if (defPolyFlatShader == null || defPolyFlatShader.contextIsOutdated()) {
          defPolyFlatShader = new PolyFlatShader(parent, defPolyFlatShaderVertURL, defPolyNoTexShaderFragURL);
        }
        if (polyFlatShader == null || polyFlatShader.contextIsOutdated()) {
          polyFlatShader = defPolyFlatShader;
        }
        shader = polyFlatShader;
      }
    }
    shader.setRenderer(this);
    shader.loadAttributes();
    shader.loadUniforms();
    return shader;
  }


  protected LineShader getLineShader() {
    if (defLineShader == null || defLineShader.contextIsOutdated()) {
      defLineShader = new LineShader(parent, defLineShaderVertURL, defLineShaderFragURL);
    }
    if (lineShader == null || lineShader.contextIsOutdated()) {
      lineShader = defLineShader;
    }
    lineShader.setRenderer(this);
    lineShader.loadAttributes();
    lineShader.loadUniforms();
    return lineShader;
  }


  protected PointShader getPointShader() {
    if (defPointShader == null || defPointShader.contextIsOutdated()) {
      defPointShader = new PointShader(parent, defPointShaderVertURL, defPointShaderFragURL);
    }
    if (pointShader == null || pointShader.contextIsOutdated()) {
      pointShader = defPointShader;
    }
    pointShader.setRenderer(this);
    pointShader.loadAttributes();
    pointShader.loadUniforms();
    return pointShader;
  }


  protected class PolyShader extends PShader {
    // We need a reference to the renderer since a shader might
    // be called by different renderers within a single application
    // (the one corresponding to the main surface, or other offscreen
    // renderers).
    protected PGraphicsOpenGL renderer;

    public PolyShader(PApplet parent) {
      super(parent);
    }

    public PolyShader(PApplet parent, String vertFilename, String fragFilename) {
      super(parent, vertFilename, fragFilename);
    }

    public PolyShader(PApplet parent, URL vertURL, URL fragURL) {
      super(parent, vertURL, fragURL);
    }

    public void setRenderer(PGraphicsOpenGL pg) {
      this.renderer = pg;
    }

    public void loadAttributes() { }
    public void loadUniforms() { }

    public void setAttribute(int loc, int vboId, int size, int type, boolean normalized, int stride, int offset) {
      if (-1 < loc) {
        pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, vboId);
        pgl.glVertexAttribPointer(loc, size, type, normalized, stride, offset);
      }
    }

    public void setVertexAttribute(int vboId, int size, int type, int stride, int offset) { }
    public void setColorAttribute(int vboId, int size, int type, int stride, int offset) { }
    public void setNormalAttribute(int vboId, int size, int type, int stride, int offset) { }
    public void setAmbientAttribute(int vboId, int size, int type, int stride, int offset) { }
    public void setSpecularAttribute(int vboId, int size, int type, int stride, int offset) { }
    public void setEmissiveAttribute(int vboId, int size, int type, int stride, int offset) { }
    public void setShininessAttribute(int vboId, int size, int type, int stride, int offset) { }
    public void setTexcoordAttribute(int vboId, int size, int type, int stride, int offset) { }
    public void setTexture(Texture tex) { }
  }


  protected class PolyFlatShader extends PolyShader {
    protected int projmodelviewMatrixLoc;

    protected int inVertexLoc;
    protected int inColorLoc;

    public PolyFlatShader(PApplet parent) {
      super(parent);
    }

    public PolyFlatShader(PApplet parent, String vertFilename, String fragFilename) {
      super(parent, vertFilename, fragFilename);
    }

    public PolyFlatShader(PApplet parent, URL vertURL, URL fragURL) {
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

      if (-1 < inVertexLoc) pgl.glEnableVertexAttribArray(inVertexLoc);
      if (-1 < inColorLoc)  pgl.glEnableVertexAttribArray(inColorLoc);

      if (renderer != null) {
        renderer.updateGLProjmodelview();
        set4x4MatUniform(projmodelviewMatrixLoc, renderer.glProjmodelview);
      }
    }

    public void stop() {
      if (-1 < inVertexLoc) pgl.glDisableVertexAttribArray(inVertexLoc);
      if (-1 < inColorLoc)  pgl.glDisableVertexAttribArray(inColorLoc);

      pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, 0);

      super.stop();
    }
  }


  protected class PolyLightShader extends PolyShader {
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

    public PolyLightShader(PApplet parent) {
      super(parent);
    }

    public PolyLightShader(PApplet parent, String vertFilename, String fragFilename) {
      super(parent, vertFilename, fragFilename);
    }

    public PolyLightShader(PApplet parent, URL vertURL, URL fragURL) {
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

      if (-1 < inVertexLoc) pgl.glEnableVertexAttribArray(inVertexLoc);
      if (-1 < inColorLoc)  pgl.glEnableVertexAttribArray(inColorLoc);
      if (-1 < inNormalLoc) pgl.glEnableVertexAttribArray(inNormalLoc);

      if (-1 < inAmbientLoc)  pgl.glEnableVertexAttribArray(inAmbientLoc);
      if (-1 < inSpecularLoc) pgl.glEnableVertexAttribArray(inSpecularLoc);
      if (-1 < inEmissiveLoc) pgl.glEnableVertexAttribArray(inEmissiveLoc);
      if (-1 < inShineLoc)    pgl.glEnableVertexAttribArray(inShineLoc);

      if (renderer != null) {
        renderer.updateGLProjmodelview();
        set4x4MatUniform(projmodelviewMatrixLoc, renderer.glProjmodelview);

        renderer.updateGLModelview();
        set4x4MatUniform(modelviewMatrixLoc, renderer.glModelview);

        renderer.updateGLNormal();
        set3x3MatUniform(normalMatrixLoc, renderer.glNormal);

        setIntUniform(lightCountLoc, renderer.lightCount);
        set4FloatVecUniform(lightPositionLoc, renderer.lightPosition);
        set3FloatVecUniform(lightNormalLoc, renderer.lightNormal);
        set3FloatVecUniform(lightAmbientLoc, renderer.lightAmbient);
        set3FloatVecUniform(lightDiffuseLoc, renderer.lightDiffuse);
        set3FloatVecUniform(lightSpecularLoc, renderer.lightSpecular);
        set3FloatVecUniform(lightFalloffCoefficientsLoc, renderer.lightFalloffCoefficients);
        set2FloatVecUniform(lightSpotParametersLoc, renderer.lightSpotParameters);
      }
    }

    public void stop() {
      if (-1 < inVertexLoc) pgl.glDisableVertexAttribArray(inVertexLoc);
      if (-1 < inColorLoc)  pgl.glDisableVertexAttribArray(inColorLoc);
      if (-1 < inNormalLoc) pgl.glDisableVertexAttribArray(inNormalLoc);

      if (-1 < inAmbientLoc)  pgl.glDisableVertexAttribArray(inAmbientLoc);
      if (-1 < inSpecularLoc) pgl.glDisableVertexAttribArray(inSpecularLoc);
      if (-1 < inEmissiveLoc) pgl.glDisableVertexAttribArray(inEmissiveLoc);
      if (-1 < inShineLoc)    pgl.glDisableVertexAttribArray(inShineLoc);

      pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, 0);

      super.stop();
    }
  }


  protected class PolyTexShader extends PolyFlatShader {
    protected int inTexcoordLoc;

    protected int texcoordMatrixLoc;
    protected int texcoordOffsetLoc;

    protected float[] tcmat;

    public PolyTexShader(PApplet parent) {
      super(parent);
    }

    public PolyTexShader(PApplet parent, String vertFilename, String fragFilename) {
      super(parent, vertFilename, fragFilename);
    }

    public PolyTexShader(PApplet parent, URL vertURL, URL fragURL) {
      super(parent, vertURL, fragURL);
    }

    public void loadUniforms() {
      super.loadUniforms();

      texcoordMatrixLoc = getUniformLocation("texcoordMatrix");
      texcoordOffsetLoc = getUniformLocation("texcoordOffset");
    }

    public void loadAttributes() {
      super.loadAttributes();

      inTexcoordLoc = getAttribLocation("inTexcoord");
    }

    public void setTexcoordAttribute(int vboId, int size, int type, int stride, int offset) {
      setAttribute(inTexcoordLoc, vboId, size, type, false, stride, offset);
    }

    public void setTexture(Texture tex) {
      float scaleu = 1;
      float scalev = 1;
      float dispu  = 0;
      float dispv  = 0;

      if (tex.isFlippedX()) {
        scaleu = -1;
        dispu  = 1;
      }

      if (tex.isFlippedY()) {
        scalev = -1;
        dispv  = 1;
      }

      scaleu *= tex.maxTexcoordU;
      dispu  *= tex.maxTexcoordU;
      scalev *= tex.maxTexcoordV;
      dispv  *= tex.maxTexcoordV;

      if (tcmat == null) {
        tcmat = new float[16];
      }

      tcmat[0] = scaleu; tcmat[4] = 0;      tcmat[ 8] = 0; tcmat[12] = dispu;
      tcmat[1] = 0;      tcmat[5] = scalev; tcmat[ 9] = 0; tcmat[13] = dispv;
      tcmat[2] = 0;      tcmat[6] = 0;      tcmat[10] = 0; tcmat[14] = 0;
      tcmat[3] = 0;      tcmat[7] = 0;      tcmat[11] = 0; tcmat[15] = 0;
      set4x4MatUniform(texcoordMatrixLoc, tcmat);

      set2FloatUniform(texcoordOffsetLoc, 1.0f / tex.width, 1.0f / tex.height);
    }

    public void start() {
      super.start();

      if (-1 < inTexcoordLoc) pgl.glEnableVertexAttribArray(inTexcoordLoc);
    }

    public void stop() {
      if (-1 < inTexcoordLoc) pgl.glDisableVertexAttribArray(inTexcoordLoc);

      super.stop();
    }
  }


  protected class PolyFullShader extends PolyLightShader {
    protected int inTexcoordLoc;

    protected int texcoordMatrixLoc;
    protected int texcoordOffsetLoc;

    protected float[] tcmat;

    public PolyFullShader(PApplet parent) {
      super(parent);
    }

    public PolyFullShader(PApplet parent, String vertFilename, String fragFilename) {
      super(parent, vertFilename, fragFilename);
    }

    public PolyFullShader(PApplet parent, URL vertURL, URL fragURL) {
      super(parent, vertURL, fragURL);
    }

    public void loadUniforms() {
      super.loadUniforms();

      texcoordMatrixLoc = getUniformLocation("texcoordMatrix");
      texcoordOffsetLoc = getUniformLocation("texcoordOffset");
    }

    public void loadAttributes() {
      super.loadAttributes();

      inTexcoordLoc = getAttribLocation("inTexcoord");
    }

    public void setTexcoordAttribute(int vboId, int size, int type, int stride, int offset) {
      setAttribute(inTexcoordLoc, vboId, size, type, false, stride, offset);
    }

    public void setTexture(Texture tex) {
      float scaleu = 1;
      float scalev = 1;
      float dispu  = 0;
      float dispv  = 0;

      if (tex.isFlippedX()) {
        scaleu = -1;
        dispu  = 1;
      }

      if (tex.isFlippedY()) {
        scalev = -1;
        dispv  = 1;
      }

      scaleu *= tex.maxTexcoordU;
      dispu  *= tex.maxTexcoordU;
      scalev *= tex.maxTexcoordV;
      dispv  *= tex.maxTexcoordV;

      if (tcmat == null) {
        tcmat = new float[16];
      }

      tcmat[0] = scaleu; tcmat[4] = 0;      tcmat[ 8] = 0; tcmat[12] = dispu;
      tcmat[1] = 0;      tcmat[5] = scalev; tcmat[ 9] = 0; tcmat[13] = dispv;
      tcmat[2] = 0;      tcmat[6] = 0;      tcmat[10] = 0; tcmat[14] = 0;
      tcmat[3] = 0;      tcmat[7] = 0;      tcmat[11] = 0; tcmat[15] = 0;
      set4x4MatUniform(texcoordMatrixLoc, tcmat);

      set2FloatUniform(texcoordOffsetLoc, 1.0f / tex.width, 1.0f / tex.height);
    }

    public void start() {
      super.start();

      if (-1 < inTexcoordLoc) pgl.glEnableVertexAttribArray(inTexcoordLoc);
    }

    public void stop() {
      if (-1 < inTexcoordLoc) pgl.glDisableVertexAttribArray(inTexcoordLoc);

      super.stop();
    }
  }


  protected class LineShader extends PShader {
    protected PGraphicsOpenGL renderer;

    protected int projectionMatrixLoc;
    protected int modelviewMatrixLoc;

    protected int viewportLoc;
    protected int perspectiveLoc;

    protected int inVertexLoc;
    protected int inColorLoc;
    protected int inAttribLoc;

    public LineShader(PApplet parent) {
      super(parent);
    }

    public LineShader(PApplet parent, String vertFilename, String fragFilename) {
      super(parent, vertFilename, fragFilename);
    }

    public LineShader(PApplet parent, URL vertURL, URL fragURL) {
      super(parent, vertURL, fragURL);
    }

    public void setRenderer(PGraphicsOpenGL pg) {
      this.renderer = pg;
    }

    public void loadAttributes() {
      inVertexLoc = getAttribLocation("inVertex");
      inColorLoc = getAttribLocation("inColor");
      inAttribLoc = getAttribLocation("inLine");
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

    public void setLineAttribute(int vboId, int size, int type, int stride, int offset) {
      setAttribute(inAttribLoc, vboId, size, type, false, stride, offset);
    }

    public void start() {
      super.start();

      if (-1 < inVertexLoc) pgl.glEnableVertexAttribArray(inVertexLoc);
      if (-1 < inColorLoc)  pgl.glEnableVertexAttribArray(inColorLoc);
      if (-1 < inAttribLoc) pgl.glEnableVertexAttribArray(inAttribLoc);

      if (renderer != null) {
        renderer.updateGLProjection();
        set4x4MatUniform(projectionMatrixLoc, renderer.glProjection);

        renderer.updateGLModelview();
        set4x4MatUniform(modelviewMatrixLoc, renderer.glModelview);

        set4FloatUniform(viewportLoc, renderer.viewport[0], renderer.viewport[1], renderer.viewport[2], renderer.viewport[3]);
      }

      setIntUniform(perspectiveLoc, perspectiveCorrectedLines ? 1 : 0);
    }

    public void stop() {
      if (-1 < inVertexLoc) pgl.glDisableVertexAttribArray(inVertexLoc);
      if (-1 < inColorLoc)  pgl.glDisableVertexAttribArray(inColorLoc);
      if (-1 < inAttribLoc) pgl.glDisableVertexAttribArray(inAttribLoc);

      pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, 0);

      super.stop();
    }
  }


  protected class PointShader extends PShader {
    protected PGraphicsOpenGL renderer;

    protected int projectionMatrixLoc;
    protected int modelviewMatrixLoc;

    protected int inVertexLoc;
    protected int inColorLoc;
    protected int inPointLoc;

    public PointShader(PApplet parent) {
      super(parent);
    }

    public PointShader(PApplet parent, String vertFilename, String fragFilename) {
      super(parent, vertFilename, fragFilename);
    }

    public PointShader(PApplet parent, URL vertURL, URL fragURL) {
      super(parent, vertURL, fragURL);
    }

    public void setRenderer(PGraphicsOpenGL pg) {
      this.renderer = pg;
    }

    public void loadAttributes() {
      inVertexLoc = getAttribLocation("inVertex");
      inColorLoc = getAttribLocation("inColor");
      inPointLoc = getAttribLocation("inPoint");
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

    public void setPointAttribute(int vboId, int size, int type, int stride, int offset) {
      setAttribute(inPointLoc, vboId, size, type, false, stride, offset);
    }

    public void start() {
      super.start();

      if (-1 < inVertexLoc) pgl.glEnableVertexAttribArray(inVertexLoc);
      if (-1 < inColorLoc)  pgl.glEnableVertexAttribArray(inColorLoc);
      if (-1 < inPointLoc)  pgl.glEnableVertexAttribArray(inPointLoc);

      if (renderer != null) {
        renderer.updateGLProjection();
        set4x4MatUniform(projectionMatrixLoc, renderer.glProjection);

        renderer.updateGLModelview();
        set4x4MatUniform(modelviewMatrixLoc, renderer.glModelview);
      }
    }

    public void stop() {
      if (-1 < inVertexLoc) pgl.glDisableVertexAttribArray(inVertexLoc);
      if (-1 < inColorLoc)  pgl.glDisableVertexAttribArray(inColorLoc);
      if (-1 < inPointLoc)  pgl.glDisableVertexAttribArray(inPointLoc);

      pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, 0);

      super.stop();
    }
  }

  //////////////////////////////////////////////////////////////

  // Utils
  
  static protected int expandArraySize(int currSize, int newMinSize) {
    int newSize = currSize;
    while (newSize < newMinSize) {
      newSize <<= 1;
    }
    return newSize;
  }

  //////////////////////////////////////////////////////////////

  // Input (raw) and Tessellated geometry, tessellator.


  protected InGeometry newInGeometry(int mode) {
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
  protected class TexCache {
    int size;
    PImage[] textures;
    int[] firstIndex;
    int[] lastIndex;
    int[] firstCache;
    int[] lastCache;
    boolean hasTexture;
    Texture tex0;

    TexCache() {
      allocate();
    }

    void allocate() {
      textures = new PImage[PGL.DEFAULT_IN_TEXTURES];
      firstIndex = new int[PGL.DEFAULT_IN_TEXTURES];
      lastIndex = new int[PGL.DEFAULT_IN_TEXTURES];      
      firstCache = new int[PGL.DEFAULT_IN_TEXTURES];
      lastCache = new int[PGL.DEFAULT_IN_TEXTURES];
      size = 0;
      hasTexture = false;
    }

    void clear() {
      java.util.Arrays.fill(textures, 0, size, null);
      size = 0;
      hasTexture = false;
    }

    void dispose() {
      textures = null;
      firstIndex = null;
      lastIndex = null;
      firstCache = null;
      lastCache = null;
    }

    void beginRender() {
      tex0 = null;
    }

    PImage getTextureImage(int i) {
      return textures[i];
    }
    
    Texture getTexture(int i) {
      PImage img = textures[i];
      Texture tex = null;

      if (img != null) {
        tex = pgPrimary.getTexture(img);
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

    void endRender() {
      if (hasTexture) {
        // Unbinding all the textures in the cache.
        for (int i = 0; i < size; i++) {
          PImage img = textures[i];
          if (img != null) {
            Texture tex = pgPrimary.getTexture(img);
            if (tex != null) {
              tex.unbind();
            }
          }
        }        
        // Disabling texturing for each of the targets used
        // by textures in the cache.
        for (int i = 0; i < size; i++) {
          PImage img = textures[i];
          if (img != null) {
            Texture tex = pgPrimary.getTexture(img);
            if (tex != null) {
              pgl.disableTexturing(tex.glTarget);
            }
          }
        }
      }
    }

    void addTexture(PImage img, int firsti, int firstb, int lasti, int lastb) {
      arrayCheck();

      textures[size] = img;
      firstIndex[size] = firsti;
      lastIndex[size] = lasti;
      firstCache[size] = firstb;
      lastCache[size] = lastb;
      
      // At least one non-null texture since last reset.
      hasTexture |= img != null;

      size++;
    }

    void setLastIndex(int lasti, int lastb) {
      lastIndex[size - 1] = lasti;
      lastCache[size - 1] = lastb;
    }

    void arrayCheck() {
      if (size == textures.length) {
        int newSize = size << 1;

        expandTextures(newSize);
        expandFirstIndex(newSize);
        expandLastIndex(newSize);
        expandFirstCache(newSize);
        expandLastCache(newSize);        
      }
    }

    void expandTextures(int n) {
      PImage[] temp = new PImage[n];
      PApplet.arrayCopy(textures, 0, temp, 0, size);
      textures = temp;
    }

    void expandFirstIndex(int n) {
      int[] temp = new int[n];
      PApplet.arrayCopy(firstIndex, 0, temp, 0, size);
      firstIndex = temp;
    }

    void expandLastIndex(int n) {
      int[] temp = new int[n];
      PApplet.arrayCopy(lastIndex, 0, temp, 0, size);
      lastIndex = temp;
    }
    
    void expandFirstCache(int n) {
      int[] temp = new int[n];
      PApplet.arrayCopy(firstCache, 0, temp, 0, size);
      firstCache = temp;
    }
    
    void expandLastCache(int n) {
      int[] temp = new int[n];
      PApplet.arrayCopy(lastCache, 0, temp, 0, size);
      lastCache = temp;
    }    
  }


  // Stores the offsets and counts of indices and vertices
  // to render a piece of geometry that doesn't fit in a single
  // glDrawElements() call.
  protected class IndexCache {
    int size;
    int[] indexCount;
    int[] indexOffset;
    int[] vertexCount;
    int[] vertexOffset;
    
    IndexCache() {
      allocate();
    }
    
    void allocate() {
      indexCount = new int[2];
      indexOffset = new int[2];      
      vertexCount = new int[2];
      vertexOffset = new int[2];
      size = 0;
    }

    void clear() {
      size = 0;
    }    
    
    int addNew() {
      arrayCheck();
      init(size);
      size++;      
      return size - 1;
    }
    
    int addNew(int index) {
      arrayCheck();
      indexCount[size] = indexCount[index];
      indexOffset[size] = indexOffset[index]; 
      vertexCount[size] = vertexCount[index];  
      vertexOffset[size] = vertexOffset[index];        
      size++;      
      return size - 1;      
    }    
    
    int getLast() {
      if (size == 0) {
        arrayCheck();
        init(0);
        size = 1;
      }
      return size - 1;
    }
    
    void incCounts(int index, int icount, int vcount) {
      indexCount[index] += icount;
      vertexCount[index] += vcount;            
    }
    
    void init(int n) {
      if (0 < n) {        
        indexOffset[n] = indexOffset[n - 1] + indexCount[n - 1];        
        vertexOffset[n] = vertexOffset[n - 1] + vertexCount[n - 1];
      } else {
        indexOffset[n] = 0;
        vertexOffset[n] = 0;        
      }
      indexCount[n] = 0;
      vertexCount[n] = 0;
    }
    
    void arrayCheck() {
      if (size == indexCount.length) {
        int newSize = size << 1;

        expandIndexCount(newSize);
        expandIndexOffset(newSize);
        expandVertexCount(newSize);
        expandVertexOffset(newSize);       
      }
    }
    
    void expandIndexCount(int n) {
      int[] temp = new int[n];
      PApplet.arrayCopy(indexCount, 0, temp, 0, size);
      indexCount = temp;
    }
    
    void expandIndexOffset(int n) {
      int[] temp = new int[n];
      PApplet.arrayCopy(indexOffset, 0, temp, 0, size);
      indexOffset = temp;      
    }
    
    void expandVertexCount(int n) {
      int[] temp = new int[n];
      PApplet.arrayCopy(vertexCount, 0, temp, 0, size);
      vertexCount = temp;
    }
    
    void expandVertexOffset(int n) {
      int[] temp = new int[n];
      PApplet.arrayCopy(vertexOffset, 0, temp, 0, size);
      vertexOffset = temp;
    }    
  }

  
  // Holds the input vertices: xyz coordinates, fill/tint color,
  // normal, texture coordinates and stroke color and weight.
  protected class InGeometry {
    int renderMode;
    int vertexCount;
    int edgeCount;

    // Range of vertices that will be processed by the
    // tessellator. They can be used in combination with the
    // edges array to have the tessellator using only a specific
    // range of vertices to generate fill geometry, while the
    // line geometry will be read from the edge vertices, which
    // could be completely different.
    int firstVertex;
    int lastVertex;

    int firstEdge;
    int lastEdge;
    
    float[] vertices;
    int[] colors;
    float[] normals;
    float[] texcoords;
    int[] strokeColors;
    float[] strokeWeights;
    
    // lines
    boolean[] breaks;
    int[][] edges;

    // Material properties
    int[] ambient;
    int[] specular;
    int[] emissive;
    float[] shininess;
    
    // Internally used by the addVertex() methods.
    int fillColor;
    int strokeColor; 
    float strokeWeight;
    int ambientColor;
    int specularColor;
    int emissiveColor;
    float shininessFactor; 
    float normalX, normalY, normalZ;
    
    InGeometry(int mode) {
      renderMode = mode;
      allocate();
    }

    // -----------------------------------------------------------------
    //
    // Allocate/dispose    
    
    void clear() {
      vertexCount = firstVertex = lastVertex = 0;
      edgeCount = firstEdge = lastEdge = 0;
    }

    void clearEdges() {
      edgeCount = firstEdge = lastEdge = 0;
    }

    void allocate() {      
      vertices = new float[3 * PGL.DEFAULT_IN_VERTICES];
      colors = new int[PGL.DEFAULT_IN_VERTICES];
      normals = new float[3 * PGL.DEFAULT_IN_VERTICES];
      texcoords = new float[2 * PGL.DEFAULT_IN_VERTICES];
      strokeColors = new int[PGL.DEFAULT_IN_VERTICES];
      strokeWeights = new float[PGL.DEFAULT_IN_VERTICES];
      ambient = new int[PGL.DEFAULT_IN_VERTICES];
      specular = new int[PGL.DEFAULT_IN_VERTICES];
      emissive = new int[PGL.DEFAULT_IN_VERTICES];
      shininess = new float[PGL.DEFAULT_IN_VERTICES];
      breaks = new boolean[PGL.DEFAULT_IN_VERTICES];
      edges = new int[PGL.DEFAULT_IN_EDGES][3];
      
      clear();
    }

    void dispose() {
      breaks = null;
      vertices = null;
      colors = null;
      normals = null;
      texcoords = null;
      strokeColors = null;
      strokeWeights = null;
      ambient = null;
      specular = null;
      emissive = null;
      shininess = null;
      edges = null;
    }

    void vertexCheck() {
      if (vertexCount == vertices.length / 3) {
        int newSize = vertexCount << 1;
        
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
        expandBreaks(newSize);
      }
    }    

    void edgeCheck() {
      if (edgeCount == edges.length) {
        int newLen = edgeCount << 1;

        expandEdges(newLen);
      }
    }
        
    // -----------------------------------------------------------------
    //
    // Query    
    
    float getVertexX(int idx) {
      return vertices[3 * idx + 0];
    }

    float getVertexY(int idx) {
      return vertices[3 * idx + 1];
    }

    float getVertexZ(int idx) {
      return vertices[3 * idx + 2];
    }

    float getLastVertexX() {
      return vertices[3 * (vertexCount - 1) + 0];
    }

    float getLastVertexY() {
      return vertices[3 * (vertexCount - 1) + 1];
    }

    float getLastVertexZ() {
      return vertices[3 * (vertexCount - 1) + 2];
    }
    
    int getNumEdgeVertices(boolean bevel) {
      int segVert = 4 * (lastEdge - firstEdge + 1);
      int bevVert = 0;
      if (bevel) {
        for (int i = firstEdge; i <= lastEdge; i++) {
          int[] edge = edges[i];
          if (edge[2] == EDGE_MIDDLE || edge[2] == EDGE_START) {
            bevVert++;  
          }
        }
      }
      return segVert + bevVert;
    }

    int getNumEdgeIndices(boolean bevel) {
      int segInd = 6 * (lastEdge - firstEdge + 1);
      int bevInd = 0;
      if (bevel) {
        for (int i = firstEdge; i <= lastEdge; i++) {
          int[] edge = edges[i];
          if (edge[2] == EDGE_MIDDLE || edge[2] == EDGE_START) {
            bevInd += 6;  
          }
        }          
      } 
      return segInd + bevInd;
    }    
    
    void getVertexMin(PVector v) {
      int index;
      for (int i = 0; i < vertexCount; i++) {
        index = 4 * i;
        v.x = PApplet.min(v.x, vertices[index++]);
        v.y = PApplet.min(v.y, vertices[index++]);
        v.z = PApplet.min(v.z, vertices[index  ]);
      }      
    }    

    void getVertexMax(PVector v) {
      int index;
      for (int i = 0; i < vertexCount; i++) {
        index = 4 * i;
        v.x = PApplet.max(v.x, vertices[index++]);
        v.y = PApplet.max(v.y, vertices[index++]);
        v.z = PApplet.max(v.z, vertices[index  ]);
      }      
    }        
    
    int getVertexSum(PVector v) {
      int index;
      for (int i = 0; i < vertexCount; i++) {
        index = 4 * i;
        v.x += vertices[index++];
        v.y += vertices[index++];
        v.z += vertices[index  ];
      }
      return vertexCount;
    }       

    // -----------------------------------------------------------------
    //
    // Expand arrays     

    void expandVertices(int n) {
      float temp[] = new float[3 * n];
      PApplet.arrayCopy(vertices, 0, temp, 0, 3 * vertexCount);
      vertices = temp;
    }

    void expandColors(int n) {
      int temp[] = new int[n];
      PApplet.arrayCopy(colors, 0, temp, 0, vertexCount);
      colors = temp;
    }

    void expandNormals(int n) {
      float temp[] = new float[3 * n];
      PApplet.arrayCopy(normals, 0, temp, 0, 3 * vertexCount);
      normals = temp;
    }

    void expandTexcoords(int n) {
      float temp[] = new float[2 * n];
      PApplet.arrayCopy(texcoords, 0, temp, 0, 2 * vertexCount);
      texcoords = temp;
    }

    void expandStrokeColors(int n) {
      int temp[] = new int[n];
      PApplet.arrayCopy(strokeColors, 0, temp, 0, vertexCount);
      strokeColors = temp;
    }

    void expandStrokeWeights(int n) {
      float temp[] = new float[n];
      PApplet.arrayCopy(strokeWeights, 0, temp, 0, vertexCount);
      strokeWeights = temp;
    }

    void expandAmbient(int n) {
      int temp[] = new int[n];
      PApplet.arrayCopy(ambient, 0, temp, 0, vertexCount);
      ambient = temp;
    }

    void expandSpecular(int n) {
      int temp[] = new int[n];
      PApplet.arrayCopy(specular, 0, temp, 0, vertexCount);
      specular = temp;
    }

    void expandEmissive(int n) {
      int temp[] = new int[n];
      PApplet.arrayCopy(emissive, 0, temp, 0, vertexCount);
      emissive = temp;
    }

    void expandShininess(int n) {
      float temp[] = new float[n];
      PApplet.arrayCopy(shininess, 0, temp, 0, vertexCount);
      shininess = temp;
    }

    void expandBreaks(int n) {
      boolean temp[] = new boolean[n];
      PApplet.arrayCopy(breaks, 0, temp, 0, vertexCount);
      breaks = temp;
    }
  
    void expandEdges(int n) {
      int temp[][] = new int[n][3];
      PApplet.arrayCopy(edges, 0, temp, 0, edgeCount);
      edges = temp;      
    }     
    
    // -----------------------------------------------------------------
    //
    // Trim arrays     
    
    void trim() {
      if (0 < vertexCount && vertexCount < vertices.length / 3) {
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
        trimBreaks();
      }

      if (0 < edgeCount && edgeCount < edges.length) {
        trimEdges();
      }
    }     
    
    void trimVertices() {
      float temp[] = new float[3 * vertexCount];
      PApplet.arrayCopy(vertices, 0, temp, 0, 3 * vertexCount);
      vertices = temp;
    }

    void trimColors() {
      int temp[] = new int[vertexCount];
      PApplet.arrayCopy(colors, 0, temp, 0, vertexCount);
      colors = temp;
    }

    void trimNormals() {
      float temp[] = new float[3 * vertexCount];
      PApplet.arrayCopy(normals, 0, temp, 0, 3 * vertexCount);
      normals = temp;
    }

    void trimTexcoords() {
      float temp[] = new float[2 * vertexCount];
      PApplet.arrayCopy(texcoords, 0, temp, 0, 2 * vertexCount);
      texcoords = temp;
    }

    void trimStrokeColors() {
      int temp[] = new int[vertexCount];
      PApplet.arrayCopy(strokeColors, 0, temp, 0, vertexCount);
      strokeColors = temp;
    }

    void trimStrokeWeights() {
      float temp[] = new float[vertexCount];
      PApplet.arrayCopy(strokeWeights, 0, temp, 0, vertexCount);
      strokeWeights = temp;
    }

    void trimAmbient() {
      int temp[] = new int[vertexCount];
      PApplet.arrayCopy(ambient, 0, temp, 0, vertexCount);
      ambient = temp;
    }

    void trimSpecular() {
      int temp[] = new int[vertexCount];
      PApplet.arrayCopy(specular, 0, temp, 0, vertexCount);
      specular = temp;
    }

    void trimEmissive() {
      int temp[] = new int[vertexCount];
      PApplet.arrayCopy(emissive, 0, temp, 0, vertexCount);
      emissive = temp;
    }

    void trimShininess() {
      float temp[] = new float[vertexCount];
      PApplet.arrayCopy(shininess, 0, temp, 0, vertexCount);
      shininess = temp;
    }
    
    void trimBreaks() {
      boolean temp[] = new boolean[vertexCount];
      PApplet.arrayCopy(breaks, 0, temp, 0, vertexCount);
      breaks = temp;    
    }

    void trimEdges() {
      int temp[][] = new int[edgeCount][3];
      PApplet.arrayCopy(edges, 0, temp, 0, edgeCount);
      edges = temp;
    }
        
    // -----------------------------------------------------------------
    //
    // Vertices    
    
    int addVertex(float x, float y,
                  int code) {
      return addVertex(x, y, 0,
                       fillColor,
                       normalX, normalY, normalZ,
                       0, 0,
                       strokeColor, strokeWeight,
                       ambientColor, specularColor, emissiveColor, shininessFactor,
                       code);
    }

    int addVertex(float x, float y,
                  float u, float v,
                  int code) { 
      return addVertex(x, y, 0,
                       fillColor,
                       normalX, normalY, normalZ,
                       u, v,
                       strokeColor, strokeWeight,
                       ambientColor, specularColor, emissiveColor, shininessFactor,
                       code);
    }    

    int addVertex(float x, float y, float z,
                  int code) { 
      return addVertex(x, y, z,
                       fillColor,
                       normalX, normalY, normalZ,
                       0, 0,
                       strokeColor, strokeWeight,
                       ambientColor, specularColor, emissiveColor, shininessFactor,
                       code);
    }      
    
    int addVertex(float x, float y, float z,
                  float u, float v,
                  int code) {
      return addVertex(x, y, z,
                       fillColor,
                       normalX, normalY, normalZ,
                       u, v, 
                       strokeColor, strokeWeight,
                       ambientColor, specularColor, emissiveColor, shininessFactor,
                       code);           
    }    
    
    int addVertex(float x, float y, float z,
                  int fcolor,
                  float nx, float ny, float nz,
                  float u, float v,
                  int scolor, float sweight,
                  int am, int sp, int em, float shine,
                  int code) {
      vertexCheck();
      int index;

      curveVertexCount = 0;

      index = 3 * vertexCount;
      vertices[index++] = x;
      vertices[index++] = y;
      vertices[index  ] = z;

      colors[vertexCount] = PGL.javaToNativeARGB(fcolor);

      index = 3 * vertexCount;
      normals[index++] = nx;
      normals[index++] = ny;
      normals[index  ] = nz;

      index = 2 * vertexCount;
      texcoords[index++] = u;
      texcoords[index  ] = v;

      strokeColors[vertexCount] = PGL.javaToNativeARGB(scolor);
      strokeWeights[vertexCount] = sweight;

      ambient[vertexCount] = PGL.javaToNativeARGB(am);
      specular[vertexCount] = PGL.javaToNativeARGB(sp);
      emissive[vertexCount] = PGL.javaToNativeARGB(em);
      shininess[vertexCount] = shine;

      breaks[vertexCount] = code == BREAK;      
      
      lastVertex = vertexCount;
      vertexCount++;
      
      return lastVertex;
    }

    void addBezierVertex(float x2, float y2, float z2,
                         float x3, float y3, float z3,
                         float x4, float y4, float z4,                         
                         boolean fill, boolean stroke, int detail, int code) {
      addBezierVertex(x2, y2, z2,
                      x3, y3, z3,
                      x4, y4, z4,                         
                      fill, stroke, detail, code, POLYGON);      
    }
    
    void addBezierVertex(float x2, float y2, float z2,
                         float x3, float y3, float z3,
                         float x4, float y4, float z4,                         
                         boolean fill, boolean stroke, int detail, int code, int shape) {
      bezierInitCheck();
      bezierVertexCheck(shape, vertexCount);  
      
      PMatrix3D draw = bezierDrawMatrix;

      float x1 = getLastVertexX();
      float y1 = getLastVertexY();
      float z1 = getLastVertexZ();

      float xplot1 = draw.m10*x1 + draw.m11*x2 + draw.m12*x3 + draw.m13*x4;
      float xplot2 = draw.m20*x1 + draw.m21*x2 + draw.m22*x3 + draw.m23*x4;
      float xplot3 = draw.m30*x1 + draw.m31*x2 + draw.m32*x3 + draw.m33*x4;

      float yplot1 = draw.m10*y1 + draw.m11*y2 + draw.m12*y3 + draw.m13*y4;
      float yplot2 = draw.m20*y1 + draw.m21*y2 + draw.m22*y3 + draw.m23*y4;
      float yplot3 = draw.m30*y1 + draw.m31*y2 + draw.m32*y3 + draw.m33*y4;

      float zplot1 = draw.m10*z1 + draw.m11*z2 + draw.m12*z3 + draw.m13*z4;
      float zplot2 = draw.m20*z1 + draw.m21*z2 + draw.m22*z3 + draw.m23*z4;
      float zplot3 = draw.m30*z1 + draw.m31*z2 + draw.m32*z3 + draw.m33*z4;

      for (int j = 0; j < detail; j++) {
        x1 += xplot1; xplot1 += xplot2; xplot2 += xplot3;
        y1 += yplot1; yplot1 += yplot2; yplot2 += yplot3;
        z1 += zplot1; zplot1 += zplot2; zplot2 += zplot3;
        addVertex(x1, y1, z1, j == 0 && code == BREAK ? BREAK : VERTEX);
      }
    }  

    public void addQuadraticVertex(float cx, float cy, float cz,
                                   float x3, float y3, float z3,
                                   boolean fill, boolean stroke, int detail, int code) {
      addQuadraticVertex(cx, cy, cz,
                         x3, y3, z3,
                         fill, stroke, detail, code, POLYGON);      
    }
    
    public void addQuadraticVertex(float cx, float cy, float cz,
                                   float x3, float y3, float z3,
                                   boolean fill, boolean stroke, int detail, int code, int shape) {
      float x1 = getLastVertexX();
      float y1 = getLastVertexY();
      float z1 = getLastVertexZ();
      addBezierVertex(x1 + ((cx-x1)*2/3.0f), y1 + ((cy-y1)*2/3.0f), z1 + ((cz-z1)*2/3.0f),
                      x3 + ((cx-x3)*2/3.0f), y3 + ((cy-y3)*2/3.0f), z3 + ((cz-z3)*2/3.0f),
                      x3, y3, z3,
                      fill, stroke, detail, code, shape);
    }

    void addCurveVertex(float x, float y, float z,
                        boolean fill, boolean stroke, int detail, int code) {
      addCurveVertex(x, y, z,
                     fill, stroke, detail, code, POLYGON);      
    }
    
    void addCurveVertex(float x, float y, float z,
                        boolean fill, boolean stroke, int detail, int code, int shape) {
      curveVertexCheck(shape);
      float[] vertex = curveVertices[curveVertexCount];
      vertex[X] = x;
      vertex[Y] = y;
      vertex[Z] = z;
      curveVertexCount++;

      // draw a segment if there are enough points
      if (curveVertexCount > 3) {
        float[] v1 = curveVertices[curveVertexCount-4];      
        float[] v2 = curveVertices[curveVertexCount-3];
        float[] v3 = curveVertices[curveVertexCount-2];
        float[] v4 = curveVertices[curveVertexCount-1];
        addCurveVertexSegment(v1[X], v1[Y], v1[Z],
                              v2[X], v2[Y], v2[Z],
                              v3[X], v3[Y], v3[Z],
                              v4[X], v4[Y], v4[Z],
                              detail, code);
      }      
    }
    
    void addCurveVertexSegment(float x1, float y1, float z1,
                               float x2, float y2, float z2,
                               float x3, float y3, float z3,
                               float x4, float y4, float z4,
                               int detail, int code) {
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

      // addVertex() will reset curveVertexCount, so save it
      int savedCount = curveVertexCount;
      
      addVertex(x0, y0, z0, code == BREAK ? BREAK : VERTEX);
      for (int j = 0; j < detail; j++) {
        x0 += xplot1; xplot1 += xplot2; xplot2 += xplot3;
        y0 += yplot1; yplot1 += yplot2; yplot2 += yplot3;
        z0 += zplot1; zplot1 += zplot2; zplot2 += zplot3;
        addVertex(x0, y0, z0, VERTEX);
      }
      
      curveVertexCount = savedCount;
    }

    // -----------------------------------------------------------------
    //
    // Edges    
    
    int addEdge(int i, int j, boolean start, boolean end) {
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

    void addTrianglesEdges() {
      for (int i = 0; i < (lastVertex - firstVertex + 1) / 3; i++) {
        int i0 = 3 * i + 0;
        int i1 = 3 * i + 1;
        int i2 = 3 * i + 2;

        addEdge(i0, i1,  true, false);
        addEdge(i1, i2, false, false);
        addEdge(i2, i0, false,  true);
      }
    }

    void addTriangleFanEdges() {
      for (int i = firstVertex + 1; i < lastVertex; i++) {
        int i0 = firstVertex;
        int i1 = i;
        int i2 = i + 1;

        addEdge(i0, i1,  true, false);
        addEdge(i1, i2, false, false);
        addEdge(i2, i0, false,  true);
      }
    }

    void addTriangleStripEdges() {
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

    void addQuadsEdges() {
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
    
    void addQuadStripEdges() {
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

    void addPolygonEdges(boolean closed) {
      // Count number of edge segments in the perimeter.
      int edgeCount = 0;
      int lnMax = lastVertex - firstVertex + 1;
      int first = firstVertex;
      int contour0 = first;
      if (!closed) lnMax--;
      for (int ln = 0; ln < lnMax; ln++) {
        int i = first + ln + 1;
        if ((i == lnMax || breaks[i]) && closed) {
          i = first + ln;
        }
        if (!breaks[i]) {
          edgeCount++;
        }
      }

      if (0 < edgeCount) {
        boolean begin = true;
        contour0 = first;
        for (int ln = 0; ln < lnMax; ln++) {
          int i0 = first + ln;
          int i1 = first + ln + 1;
          if (breaks[i0]) contour0 = i0;
          if (i1 == lnMax || breaks[i1]) {
            // We are either at the end of a contour or at the end of the
            // edge path.
            if (closed) {
              // Draw line to the first vertex of the current contour,
              // if the polygon is closed.
              i0 = first + ln;
              i1 = contour0;
              addEdge(i0, i1, begin, true);
            } else if (!breaks[i1]) {
              addEdge(i0, i1, begin, false);
            }
            // We might start a new contour in the next iteration.
            begin = true;
          } else if (!breaks[i1]) {
            boolean end = i1 + 1 < lnMax && breaks[i1 + 1];
            addEdge(i0, i1, begin, end);
            begin = false;
          }
        }
      }
    }        
    
    // -----------------------------------------------------------------
    //
    // Normal calculation       
    
    void calcTriangleNormal(int i0, int i1, int i2) {
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

      // The automatic normal calculation in Processing assumes
      // that vertices as given in CCW order so:
      // n = v12 x v10
      // so that the normal outwards.
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

    void calcTrianglesNormals() {
      for (int i = 0; i < (lastVertex - firstVertex + 1) / 3; i++) {
        int i0 = 3 * i + 0;
        int i1 = 3 * i + 1;
        int i2 = 3 * i + 2;

        calcTriangleNormal(i0, i1, i2);
      }
    }
    
    void calcTriangleFanNormals() {
      for (int i = firstVertex + 1; i < lastVertex; i++) {
        int i0 = firstVertex;
        int i1 = i;
        int i2 = i + 1;

        calcTriangleNormal(i0, i1, i2);
      }
    }
    
    void calcTriangleStripNormals() {
      for (int i = firstVertex + 1; i < lastVertex; i++) {
        int i1 = i;
        int i0, i2;
        if (i % 2 == 0) {
          // The even triangles (0, 2, 4...) should be CW
          i0 = i + 1;
          i2 = i - 1;
        } else {
          // The even triangles (1, 3, 5...) should be CCW
          i0 = i - 1;
          i2 = i + 1;
        }
        calcTriangleNormal(i0, i1, i2);
      }
    }
    
    void calcQuadsNormals() {
      for (int i = 0; i < (lastVertex - firstVertex + 1) / 4; i++) {
        int i0 = 4 * i + 0;
        int i1 = 4 * i + 1;
        int i2 = 4 * i + 2;
        int i3 = 4 * i + 3;

        calcTriangleNormal(i0, i1, i2);
        calcTriangleNormal(i2, i3, i0);
      }
    }

    void calcQuadStripNormals() {
      for (int qd = 1; qd < (lastVertex - firstVertex + 1) / 2; qd++) {
        int i0 = firstVertex + 2 * (qd - 1);
        int i1 = firstVertex + 2 * (qd - 1) + 1;
        int i2 = firstVertex + 2 * qd;
        int i3 = firstVertex + 2 * qd + 1;

        calcTriangleNormal(i0, i3, i1);
        calcTriangleNormal(i0, i2, i3);
      }
    }
    
    // -----------------------------------------------------------------
    //
    // Primitives
    
    void setMaterial(int fillColor, int strokeColor, float strokeWeight,
                     int ambientColor, int specularColor, int emissiveColor, float shininessFactor) {
      this.fillColor = fillColor;
      this.strokeColor = strokeColor; 
      this.strokeWeight = strokeWeight;
      this.ambientColor = ambientColor;
      this.specularColor = specularColor;
      this.emissiveColor = emissiveColor;
      this.shininessFactor = shininessFactor;      
    }
    
    void setNormal(float normalX, float normalY, float normalZ) {
      this.normalX = normalX;
      this.normalY = normalY; 
      this.normalZ = normalZ;
    }
    
    void addPoint(float x, float y, float z, boolean fill, boolean stroke) {       
      addVertex(x, y, z, VERTEX);      
    }

    void addLine(float x1, float y1, float z1,
                 float x2, float y2, float z2,
                 boolean fill, boolean stroke) {
      int idx1 = addVertex(x1, y1, z1, VERTEX);
      int idx2 = addVertex(x2, y2, z2, VERTEX);
      if (stroke) addEdge(idx1, idx2, true, true);      
    } 

    void addTriangle(float x1, float y1, float z1, 
                     float x2, float y2, float z2,
                     float x3, float y3, float z3,
                     boolean fill, boolean stroke) {
      int idx1 = addVertex(x1, y1, z1, VERTEX);
      int idx2 = addVertex(x2, y2, z2, VERTEX);      
      int idx3 = addVertex(x3, y3, z3, VERTEX);
      if (stroke) { 
        addEdge(idx1, idx2, true, false);
        addEdge(idx2, idx3, false, false);
        addEdge(idx3, idx1, false, true);
      }
    }    

    void addQuad(float x1, float y1, float z1,
                 float x2, float y2, float z2,
                 float x3, float y3, float z3,
                 float x4, float y4, float z4,
                 boolean fill, boolean stroke) {
      int idx1 = addVertex(x1, y1, z1, 0, 0, VERTEX);
      int idx2 = addVertex(x2, y2, z2, 1, 0, VERTEX);      
      int idx3 = addVertex(x3, y3, z3, 1, 1, VERTEX);
      int idx4 = addVertex(x4, y4, z4, 0, 1, VERTEX);
      if (stroke) { 
        addEdge(idx1, idx2, true, false);
        addEdge(idx2, idx3, false, false);
        addEdge(idx3, idx4, false, false);
        addEdge(idx4, idx1, false, true);
      }
    } 
    
    void addRect(float a, float b, float c, float d, 
                 boolean fill, boolean stroke, int rectMode) {
      float hradius, vradius;
      switch (rectMode) {
      case CORNERS:
        break;
      case CORNER:
        c += a; d += b;
        break;
      case RADIUS:
        hradius = c;
        vradius = d;
        c = a + hradius;
        d = b + vradius;
        a -= hradius;
        b -= vradius;
        break;
      case CENTER:
        hradius = c / 2.0f;
        vradius = d / 2.0f;
        c = a + hradius;
        d = b + vradius;
        a -= hradius;
        b -= vradius;
      }

      if (a > c) {
        float temp = a; a = c; c = temp;
      }

      if (b > d) {
        float temp = b; b = d; d = temp;
      }   
        
      addQuad(a, b, 0,
              c, b, 0, 
              c, d, 0,
              a, d, 0,
              fill, stroke);
    }
    
    void addRect(float a, float b, float c, float d,
                 float tl, float tr, float br, float bl,
                 boolean fill, boolean stroke, int detail, int rectMode) {
      float hradius, vradius;
      switch (rectMode) {
      case CORNERS:
        break;
      case CORNER:
        c += a; d += b;
        break;
      case RADIUS:
        hradius = c;
        vradius = d;
        c = a + hradius;
        d = b + vradius;
        a -= hradius;
        b -= vradius;
        break;
      case CENTER:
        hradius = c / 2.0f;
        vradius = d / 2.0f;
        c = a + hradius;
        d = b + vradius;
        a -= hradius;
        b -= vradius;
      }

      if (a > c) {
        float temp = a; a = c; c = temp;
      }

      if (b > d) {
        float temp = b; b = d; d = temp;
      }

      float maxRounding = PApplet.min((c - a) / 2, (d - b) / 2);
      if (tl > maxRounding) tl = maxRounding;
      if (tr > maxRounding) tr = maxRounding;
      if (br > maxRounding) br = maxRounding;
      if (bl > maxRounding) bl = maxRounding;      
            
      if (nonZero(tr)) {
        addVertex(c-tr, b, VERTEX);
        addQuadraticVertex(c, b, 0, c, b+tr, 0,
                           fill, stroke, detail, VERTEX);
      } else {
        addVertex(c, b, VERTEX);
      }
      if (nonZero(br)) {
        addVertex(c, d-br, VERTEX);
        addQuadraticVertex(c, d, 0, c-br, d, 0,
                           fill, stroke, detail, VERTEX);
      } else {
        addVertex(c, d, VERTEX);
      }
      if (nonZero(bl)) {
        addVertex(a+bl, d, VERTEX);
        addQuadraticVertex(a, d, 0, a, d-bl, 0,
                           fill, stroke, detail, VERTEX);
      } else {
        addVertex(a, d, VERTEX);
      }
      if (nonZero(tl)) {
        addVertex(a, b+tl, VERTEX);
        addQuadraticVertex(a, b, 0, a+tl, b, 0,
                           fill, stroke, detail, VERTEX);
      } else {
        addVertex(a, b, VERTEX);
      }
      
      if (stroke) addPolygonEdges(true);
    }
    
    void addEllipse(float a, float b, float c, float d, 
                    boolean fill, boolean stroke, int ellipseMode) {
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

      // should call screenX/Y using current renderer.
      float sx1 = pgCurrent.screenX(x, y);
      float sy1 = pgCurrent.screenY(x, y);
      float sx2 = pgCurrent.screenX(x + w, y + h);
      float sy2 = pgCurrent.screenY(x + w, y + h);
      
      int accuracy = PApplet.max(MIN_POINT_ACCURACY, 
                                 (int) (TWO_PI * PApplet.dist(sx1, sy1, sx2, sy2) / POINT_ACCURACY_FACTOR));
      float inc = (float) PGraphicsOpenGL.SINCOS_LENGTH / accuracy;

      if (fill) {
        addVertex(centerX, centerY, VERTEX);
      }
      int idx0, pidx, idx;
      idx0 = pidx = idx = 0;
      float val = 0;
      for (int i = 0; i < accuracy; i++) {
        idx = addVertex(centerX + PGraphicsOpenGL.cosLUT[(int) val] * radiusH,
                        centerY + PGraphicsOpenGL.sinLUT[(int) val] * radiusV,
                        VERTEX);
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
                VERTEX);
      if (stroke) addEdge(idx, idx0, false, true);
    }
    
    void addArc(float a, float b, float c, float d,
                float start, float stop,
                boolean fill, boolean stroke, int ellipseMode) {
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

      } else if (ellipseMode == CENTER) {
        x = a - c/2f;
        y = b - d/2f;
      }

      // make sure this loop will exit before starting while
      if (Float.isInfinite(start) || Float.isInfinite(stop)) return;
      if (stop < start) return;  // why bother

      // make sure that we're starting at a useful point
      while (start < 0) {
        start += TWO_PI;
        stop += TWO_PI;
      }

      if (stop - start > TWO_PI) {
        start = 0;
        stop = TWO_PI;
      }
      
      float hr = w / 2f;
      float vr = h / 2f;

      float centerX = x + hr;
      float centerY = y + vr;

      
      int startLUT = (int) (0.5f + (start / TWO_PI) * SINCOS_LENGTH);
      int stopLUT = (int) (0.5f + (stop / TWO_PI) * SINCOS_LENGTH);

      if (fill) {
        vertex(centerX, centerY, VERTEX);
      }
        
      int increment = 1; // what's a good algorithm? stopLUT - startLUT;
      int pidx, idx;
      pidx = idx = 0;
      for (int i = startLUT; i < stopLUT; i += increment) {
        int ii = i % SINCOS_LENGTH;
        // modulo won't make the value positive
        if (ii < 0) ii += SINCOS_LENGTH;
        idx = addVertex(centerX + cosLUT[ii] * hr,
                        centerY + sinLUT[ii] * vr,
                        VERTEX);
        
        if (0 < i) {
          if (stroke) addEdge(pidx, idx, i == 1, false);
        }

        pidx = idx;        
      }
      // draw last point explicitly for accuracy
      idx = addVertex(centerX + cosLUT[stopLUT % SINCOS_LENGTH] * hr,
                      centerY + sinLUT[stopLUT % SINCOS_LENGTH] * vr,
                      VERTEX);
      if (stroke) addEdge(pidx, idx, false, true);
    }    

    void addBox(float w, float h, float d,
                boolean fill, boolean stroke) {
      float x1 = -w/2f; float x2 = w/2f;
      float y1 = -h/2f; float y2 = h/2f;
      float z1 = -d/2f; float z2 = d/2f;

      if (fill || stroke) {
        // front face
        setNormal(0, 0, 1);
        addVertex(x1, y1, z1, 0, 0, VERTEX);
        addVertex(x2, y1, z1, 1, 0, VERTEX);
        addVertex(x2, y2, z1, 1, 1, VERTEX);
        addVertex(x1, y2, z1, 0, 1, VERTEX);

        // right face
        setNormal(1, 0, 0);
        addVertex(x2, y1, z1, 0, 0, VERTEX);
        addVertex(x2, y1, z2, 1, 0, VERTEX);
        addVertex(x2, y2, z2, 1, 1, VERTEX);
        addVertex(x2, y2, z1, 0, 1, VERTEX);

        // back face
        setNormal(0, 0, -1);
        addVertex(x2, y1, z2, 0, 0, VERTEX);
        addVertex(x1, y1, z2, 1, 0, VERTEX);
        addVertex(x1, y2, z2, 1, 1, VERTEX);
        addVertex(x2, y2, z2, 0, 1, VERTEX);

        // left face
        setNormal(-1, 0, 0);
        addVertex(x1, y1, z2, 0, 0, VERTEX);
        addVertex(x1, y1, z1, 1, 0, VERTEX);
        addVertex(x1, y2, z1, 1, 1, VERTEX);
        addVertex(x1, y2, z2, 0, 1, VERTEX);;

        // top face
        setNormal(0, 1, 0);
        addVertex(x1, y1, z2, 0, 0, VERTEX);
        addVertex(x2, y1, z2, 1, 0, VERTEX);
        addVertex(x2, y1, z1, 1, 1, VERTEX);
        addVertex(x1, y1, z1, 0, 1, VERTEX);

        // bottom face
        setNormal(0, -1, 0);
        addVertex(x1, y2, z1, 0, 0, VERTEX);
        addVertex(x2, y2, z1, 1, 0, VERTEX);
        addVertex(x2, y2, z2, 1, 1, VERTEX);
        addVertex(x1, y2, z2, 0, 1, VERTEX);        
      }
      
      if (stroke) {
        addEdge(0, 1, true, true);
        addEdge(1, 2, true, true);
        addEdge(2, 3, true, true);
        addEdge(3, 0, true, true);
                
        addEdge(0,  9, true, true);
        addEdge(1,  8, true, true);
        addEdge(2, 11, true, true);
        addEdge(3, 10, true, true);
        
        addEdge( 8,  9, true, true);
        addEdge( 9, 10, true, true);
        addEdge(10, 11, true, true);
        addEdge(11,  8, true, true);        
      }
    }
    
    // Adds the vertices that define an sphere, without duplicating
    // any vertex or edge.
    int[] addSphere(float r, int detailU, int detailV, 
                    boolean fill, boolean stroke) {
      if ((detailU < 3) || (detailV < 2)) {
        sphereDetail(30);
        detailU = detailV = 30;
      } else {
        sphereDetail(detailU, detailV);
      }
      
      int nind = 3 * detailU + (6 * detailU + 3) * (detailV - 2) + 3 * detailU;
      int[] indices = new int[nind];      
      
      int vertCount = 0;
      int indCount = 0;
      int vert0, vert1;      
      
      float u, v;
      float du = 1.0f / (detailU);
      float dv = 1.0f / (detailV);

      // Southern cap -------------------------------------------------------
      
      // Adding multiple copies of the south pole vertex, each one with a 
      // different u coordinate, so the texture mapping is correct when 
      // making the first strip of triangles.
      u = 1; v = 1;
      for (int i = 0; i < detailU; i++) {
        setNormal(0, 1, 0);
        addVertex(0, r, 0, u , v, VERTEX);
        u -= du;
      }      
      vertCount = detailU;
      vert0 = vertCount;
      u = 1; v -= dv;
      for (int i = 0; i < detailU; i++) {
        setNormal(sphereX[i], sphereY[i], sphereZ[i]);
        addVertex(r * sphereX[i], r *sphereY[i], r * sphereZ[i], u , v, VERTEX);
        u -= du;
      }      
      vertCount += detailU;
      vert1 = vertCount;
      setNormal(sphereX[0], sphereY[0], sphereZ[0]);
      addVertex(r * sphereX[0], r * sphereY[0], r * sphereZ[0], u, v, VERTEX);
      vertCount++;
      
      for (int i = 0; i < detailU; i++) {
        int i1 = vert0 + i;
        int i0 = vert0 + i - detailU;
        
        indices[3 * i + 0] = i1;
        indices[3 * i + 1] = i0;
        indices[3 * i + 2] = i1 + 1;
        
        addEdge(i0, i1, true, true);
        addEdge(i1, i1 + 1, true, true);        
      }
      indCount += 3 * detailU;
      
      // Middle rings -------------------------------------------------------
            
      int offset = 0;
      for (int j = 2; j < detailV; j++) {      
        offset += detailU;
        vert0 = vertCount;
        u = 1; v -= dv;       
        for (int i = 0; i < detailU; i++) {
          int ioff = offset + i;
          setNormal(sphereX[ioff], sphereY[ioff], sphereZ[ioff]);
          addVertex(r * sphereX[ioff], r *sphereY[ioff], r * sphereZ[ioff], u , v, VERTEX);
          u -= du;
        }
        vertCount += detailU;
        vert1 = vertCount;
        setNormal(sphereX[offset], sphereY[offset], sphereZ[offset]);
        addVertex(r * sphereX[offset], r * sphereY[offset], r * sphereZ[offset], u, v, VERTEX);
        vertCount++;
        
        for (int i = 0; i < detailU; i++) {
          int i1 = vert0 + i;
          int i0 = vert0 + i - detailU - 1;
          
          indices[indCount + 6 * i + 0] = i1;
          indices[indCount + 6 * i + 1] = i0;
          indices[indCount + 6 * i + 2] = i0 + 1;
          
          indices[indCount + 6 * i + 3] = i1;
          indices[indCount + 6 * i + 4] = i0 + 1;
          indices[indCount + 6 * i + 5] = i1 + 1;
          
          addEdge(i0, i1, true, true);
          addEdge(i1, i1 + 1, true, true);
          addEdge(i0 + 1, i1, true, true);
        }
        indCount += 6 * detailU;
        indices[indCount + 0] = vert1;
        indices[indCount + 1] = vert1 - detailU;
        indices[indCount + 2] = vert1 - 1;
        indCount += 3;
        
        addEdge(vert1 - detailU, vert1 - 1, true, true);
        addEdge(vert1 - 1, vert1, true, true);        
      }
            
      // Northern cap -------------------------------------------------------
      
      // Adding multiple copies of the north pole vertex, each one with a 
      // different u coordinate, so the texture mapping is correct when 
      // making the last strip of triangles.      
      u = 1; v = 0;
      for (int i = 0; i < detailU; i++) {
        setNormal(0, -1, 0);
        addVertex(0, -r, 0, u , v, VERTEX);
        u -= du;
      }         
      vertCount += detailU;      
      
      for (int i = 0; i < detailU; i++) {
        int i0 = vert0 + i;
        int i1 = vert0 + i + detailU + 1;
        
        indices[indCount + 3 * i + 0] = i0;
        indices[indCount + 3 * i + 1] = i1;
        indices[indCount + 3 * i + 2] = i0 + 1;
        
        addEdge(i0, i0 + 1, true, true);
        addEdge(i0, i1, true, true);        
      }
      indCount += 3 * detailU;      
      
      return indices;
    } 
  }

  
  // Holds tessellated data for polygon, line and point geometry.
  protected class TessGeometry {
    int renderMode;

    // Tessellated polygon data
    int polyVertexCount;
    int firstPolyVertex;
    int lastPolyVertex;
    float[] polyVertices;
    int[] polyColors;
    float[] polyNormals;
    float[] polyTexcoords;    

    // Polygon material properties (polyColors is used
    // as the diffuse color when lighting is enabled)
    int[] polyAmbient;
    int[] polySpecular;
    int[] polyEmissive;
    float[] polyShininess;
    
    int polyIndexCount;
    int firstPolyIndex;
    int lastPolyIndex;
    short[] polyIndices;    
    IndexCache polyIndexCache = new IndexCache();

    // Tessellated line data
    int lineVertexCount;
    int firstLineVertex;
    int lastLineVertex;
    float[] lineVertices;
    int[] lineColors;
    float[] lineAttribs;

    int lineIndexCount;
    int firstLineIndex;
    int lastLineIndex;
    short[] lineIndices;
    IndexCache lineIndexCache = new IndexCache();

    // Tessellated point data
    int pointVertexCount;
    int firstPointVertex;
    int lastPointVertex;
    float[] pointVertices;
    int[] pointColors;
    float[] pointAttribs;

    int pointIndexCount;
    int firstPointIndex;
    int lastPointIndex;
    short[] pointIndices;
    IndexCache pointIndexCache = new IndexCache();

    TessGeometry(int mode) {
      renderMode = mode;      
      allocate();
    }

    // -----------------------------------------------------------------
    //
    // Allocate/dispose    

    void allocate() {
      polyVertices = new float[4 * PGL.DEFAULT_TESS_VERTICES];
      polyColors = new int[PGL.DEFAULT_TESS_VERTICES];
      polyNormals = new float[3 * PGL.DEFAULT_TESS_VERTICES];
      polyTexcoords = new float[2 * PGL.DEFAULT_TESS_VERTICES];
      polyAmbient = new int[PGL.DEFAULT_TESS_VERTICES];
      polySpecular = new int[PGL.DEFAULT_TESS_VERTICES];
      polyEmissive = new int[PGL.DEFAULT_TESS_VERTICES];
      polyShininess = new float[PGL.DEFAULT_TESS_VERTICES];
      polyIndices = new short[PGL.DEFAULT_TESS_VERTICES];

      lineVertices = new float[4 * PGL.DEFAULT_TESS_VERTICES];
      lineColors = new int[PGL.DEFAULT_TESS_VERTICES];
      lineAttribs = new float[4 * PGL.DEFAULT_TESS_VERTICES];
      lineIndices = new short[PGL.DEFAULT_TESS_VERTICES];

      pointVertices = new float[4 * PGL.DEFAULT_TESS_VERTICES];
      pointColors = new int[PGL.DEFAULT_TESS_VERTICES];
      pointAttribs = new float[2 * PGL.DEFAULT_TESS_VERTICES];
      pointIndices = new short[PGL.DEFAULT_TESS_VERTICES];

      clear();
    }
        
    void clear() {
      firstPolyVertex = lastPolyVertex = polyVertexCount = 0;
      firstPolyIndex = lastPolyIndex = polyIndexCount = 0;

      firstLineVertex = lastLineVertex = lineVertexCount = 0;
      firstLineIndex = lastLineIndex = lineIndexCount = 0;

      firstPointVertex = lastPointVertex = pointVertexCount = 0;
      firstPointIndex = lastPointIndex = pointIndexCount = 0;

      polyIndexCache.clear();
      lineIndexCache.clear();
      pointIndexCache.clear();
    }
    
    void dipose() {
      polyVertices = null;
      polyColors = null;
      polyNormals = null;
      polyTexcoords = null;
      polyAmbient = null;
      polySpecular = null;
      polyEmissive = null;
      polyShininess = null;
      polyIndices = null;

      lineVertices = null;
      lineColors = null;
      lineAttribs = null;
      lineIndices = null;

      pointVertices = null;
      pointColors = null;
      pointAttribs = null;
      pointIndices = null;
    }
    
    void polyVertexCheck() {
      if (polyVertexCount == polyVertices.length / 4) {
        int newSize = polyVertexCount << 1;

        expandPolyVertices(newSize);
        expandPolyColors(newSize);
        expandPolyNormals(newSize);
        expandPolyTexcoords(newSize);
        expandPolyAmbient(newSize);
        expandPolySpecular(newSize);
        expandPolyEmissive(newSize);
        expandPolyShininess(newSize);
      }
      
      firstPolyVertex = polyVertexCount;
      polyVertexCount++;
      lastPolyVertex = polyVertexCount - 1;       
    }

    void polyVertexCheck(int count) {
      int oldSize = polyVertices.length / 4;
      if (polyVertexCount + count > oldSize) {
        int newSize = expandArraySize(oldSize, polyVertexCount + count);

        expandPolyVertices(newSize);
        expandPolyColors(newSize);
        expandPolyNormals(newSize);
        expandPolyTexcoords(newSize);
        expandPolyAmbient(newSize);
        expandPolySpecular(newSize);
        expandPolyEmissive(newSize);
        expandPolyShininess(newSize);
      }
      
      firstPolyVertex = polyVertexCount;
      polyVertexCount += count;
      lastPolyVertex = polyVertexCount - 1;      
    }
        
    void polyIndexCheck(int count) {
      int oldSize = polyIndices.length;
      if (polyIndexCount + count > oldSize) {
        int newSize = expandArraySize(oldSize, polyIndexCount + count);

        expandPolyIndices(newSize);
      }

      firstPolyIndex = polyIndexCount;
      polyIndexCount += count;
      lastPolyIndex = polyIndexCount - 1;
    }    
    
    void polyIndexCheck() {
      if (polyIndexCount == polyIndices.length) {
        int newSize = polyIndexCount << 1;
        expandPolyIndices(newSize);
      }
      
      firstPolyIndex = polyIndexCount;
      polyIndexCount++;
      lastPolyIndex = polyIndexCount - 1;      
    }    
    
    void lineVertexCheck(int count) {
      int oldSize = lineVertices.length / 4;
      if (lineVertexCount + count > oldSize) {
        int newSize = expandArraySize(oldSize, lineVertexCount + count);

        expandLineVertices(newSize);
        expandLineColors(newSize);
        expandLineAttribs(newSize);
      }

      firstLineVertex = lineVertexCount;
      lineVertexCount += count;
      lastLineVertex = lineVertexCount - 1;
    }

    void lineIndexCheck(int count) {
      int oldSize = lineIndices.length;
      if (lineIndexCount + count > oldSize) {
        int newSize = expandArraySize(oldSize, lineIndexCount + count);

        expandLineIndices(newSize);
      }

      firstLineIndex = lineIndexCount;
      lineIndexCount += count;
      lastLineIndex = lineIndexCount - 1;
    }
    
    void pointVertexCheck(int count) {
      int oldSize = pointVertices.length / 4;
      if (pointVertexCount + count > oldSize) {
        int newSize = expandArraySize(oldSize, pointVertexCount + count);

        expandPointVertices(newSize);
        expandPointColors(newSize);
        expandPointAttribs(newSize);
      }

      firstPointVertex = pointVertexCount;
      pointVertexCount += count;
      lastPointVertex = pointVertexCount - 1;
    }
    
    void pointIndexCheck(int count) {
      int oldSize = pointIndices.length;
      if (pointIndexCount + count > oldSize) {
        int newSize = expandArraySize(oldSize, pointIndexCount + count);

        expandPointIndices(newSize);
      }

      firstPointIndex = pointIndexCount;
      pointIndexCount += count;
      lastPointIndex = pointIndexCount - 1;
    }    
    
    // -----------------------------------------------------------------
    //
    // Query
    
    boolean isFull() {
      return PGL.FLUSH_VERTEX_COUNT <= polyVertexCount ||
             PGL.FLUSH_VERTEX_COUNT <= lineVertexCount ||
             PGL.FLUSH_VERTEX_COUNT <= pointVertexCount;
    }
    
    void getPolyVertexMin(PVector v, int first, int last) {
      for (int i = first; i <= last; i++) {
        int index = 4 * i;
        v.x = PApplet.min(v.x, polyVertices[index++]);
        v.y = PApplet.min(v.y, polyVertices[index++]);
        v.z = PApplet.min(v.z, polyVertices[index  ]);
      }     
    }
    
    void getLineVertexMin(PVector v, int first, int last) {
      for (int i = first; i <= last; i++) {
        int index = 4 * i;
        v.x = PApplet.min(v.x, lineVertices[index++]);
        v.y = PApplet.min(v.y, lineVertices[index++]);
        v.z = PApplet.min(v.z, lineVertices[index  ]);
      }      
    }
    
    void getPointVertexMin(PVector v, int first, int last) {
      for (int i = first; i <= last; i++) {
        int index = 4 * i;
        v.x = PApplet.min(v.x, pointVertices[index++]);
        v.y = PApplet.min(v.y, pointVertices[index++]);
        v.z = PApplet.min(v.z, pointVertices[index  ]);
      }      
    }    
        
    void getPolyVertexMax(PVector v, int first, int last) {
      for (int i = first; i <= last; i++) {
        int index = 4 * i;
        v.x = PApplet.max(v.x, polyVertices[index++]);
        v.y = PApplet.max(v.y, polyVertices[index++]);
        v.z = PApplet.max(v.z, polyVertices[index  ]);
      }
    }       
    
    void getLineVertexMax(PVector v, int first, int last) {
      for (int i = first; i <= last; i++) {
        int index = 4 * i;
        v.x = PApplet.max(v.x, lineVertices[index++]);
        v.y = PApplet.max(v.y, lineVertices[index++]);
        v.z = PApplet.max(v.z, lineVertices[index  ]);
      }     
    }
    
    void getPointVertexMax(PVector v, int first, int last) {
      for (int i = first; i <= last; i++) {
        int index = 4 * i;
        v.x = PApplet.max(v.x, pointVertices[index++]);
        v.y = PApplet.max(v.y, pointVertices[index++]);
        v.z = PApplet.max(v.z, pointVertices[index  ]);
      }         
    }
    
    int getPolyVertexSum(PVector v, int first, int last) {
      for (int i = first; i <= last; i++) {
        int index = 4 * i;
        v.x += polyVertices[index++];
        v.y += polyVertices[index++];
        v.z += polyVertices[index  ];
      }
      return last - first + 1;
    }
    
    int getLineVertexSum(PVector v, int first, int last) {
      for (int i = first; i <= last; i++) {
        int index = 4 * i;
        v.x += lineVertices[index++];
        v.y += lineVertices[index++];
        v.z += lineVertices[index  ];
      }
      return last - first + 1;
    }
    
    int getPointVertexSum(PVector v, int first, int last) {
      for (int i = first; i <= last; i++) {
        int index = 4 * i;
        v.x += pointVertices[index++];
        v.y += pointVertices[index++];
        v.z += pointVertices[index  ];
      }      
      return last - first + 1;
    }    
        
    // -----------------------------------------------------------------
    //
    // Expand arrays     

    void expandPolyVertices(int n) {
      float temp[] = new float[4 * n];
      PApplet.arrayCopy(polyVertices, 0, temp, 0, 4 * polyVertexCount);
      polyVertices = temp;
    }

    void expandPolyColors(int n) {
      int temp[] = new int[n];
      PApplet.arrayCopy(polyColors, 0, temp, 0, polyVertexCount);
      polyColors = temp;
    }

    void expandPolyNormals(int n) {
      float temp[] = new float[3 * n];
      PApplet.arrayCopy(polyNormals, 0, temp, 0, 3 * polyVertexCount);
      polyNormals = temp;
    }

    void expandPolyTexcoords(int n) {
      float temp[] = new float[2 * n];
      PApplet.arrayCopy(polyTexcoords, 0, temp, 0, 2 * polyVertexCount);
      polyTexcoords = temp;
    }

    void expandPolyAmbient(int n) {
      int temp[] = new int[n];
      PApplet.arrayCopy(polyAmbient, 0, temp, 0, polyVertexCount);
      polyAmbient = temp;
    }

    void expandPolySpecular(int n) {
      int temp[] = new int[n];
      PApplet.arrayCopy(polySpecular, 0, temp, 0, polyVertexCount);
      polySpecular = temp;
    }

    void expandPolyEmissive(int n) {
      int temp[] = new int[n];
      PApplet.arrayCopy(polyEmissive, 0, temp, 0, polyVertexCount);
      polyEmissive = temp;
    }

    void expandPolyShininess(int n) {
      float temp[] = new float[n];
      PApplet.arrayCopy(polyShininess, 0, temp, 0, polyVertexCount);
      polyShininess = temp;
    }
    
    void expandPolyIndices(int n) {
      short temp[] = new short[n];
      PApplet.arrayCopy(polyIndices, 0, temp, 0, polyIndexCount);
      polyIndices = temp;
    }
    
    void expandLineVertices(int n) {
      float temp[] = new float[4 * n];
      PApplet.arrayCopy(lineVertices, 0, temp, 0, 4 * lineVertexCount);
      lineVertices = temp;
    }

    void expandLineColors(int n) {
      int temp[] = new int[n];
      PApplet.arrayCopy(lineColors, 0, temp, 0, lineVertexCount);
      lineColors = temp;
    }

    void expandLineAttribs(int n) {
      float temp[] = new float[4 * n];
      PApplet.arrayCopy(lineAttribs, 0, temp, 0, 4 * lineVertexCount);
      lineAttribs = temp;
    }
        
    void expandLineIndices(int n) {
      short temp[] = new short[n];
      PApplet.arrayCopy(lineIndices, 0, temp, 0, lineIndexCount);
      lineIndices = temp;
    }
    
    void expandPointVertices(int n) {
      float temp[] = new float[4 * n];
      PApplet.arrayCopy(pointVertices, 0, temp, 0, 4 * pointVertexCount);
      pointVertices = temp;
    }

    void expandPointColors(int n) {
      int temp[] = new int[n];
      PApplet.arrayCopy(pointColors, 0, temp, 0, pointVertexCount);
      pointColors = temp;
    }

    void expandPointAttribs(int n) {
      float temp[] = new float[2 * n];
      PApplet.arrayCopy(pointAttribs, 0, temp, 0, 2 * pointVertexCount);
      pointAttribs = temp;
    }
    
    void expandPointIndices(int n) {
      short temp[] = new short[n];
      PApplet.arrayCopy(pointIndices, 0, temp, 0, pointIndexCount);
      pointIndices = temp;
    }    
        
    // -----------------------------------------------------------------
    //
    // Trim arrays     
    
    void trim() {
      if (0 < polyVertexCount && polyVertexCount < polyVertices.length / 4) {
        trimPolyVertices();
        trimPolyColors();
        trimPolyNormals();
        trimPolyTexcoords();
        trimPolyAmbient();
        trimPolySpecular();
        trimPolyEmissive();
        trimPolyShininess();
      }

      if (0 < polyIndexCount && polyIndexCount < polyIndices.length) {
        trimPolyIndices();
      }

      if (0 < lineVertexCount && lineVertexCount < lineVertices.length / 4) {
        trimLineVertices();
        trimLineColors();
        trimLineAttribs();
      }

      if (0 < lineIndexCount && lineIndexCount < lineIndices.length) {
        trimLineIndices();
      }

      if (0 < pointVertexCount && pointVertexCount < pointVertices.length / 4) {
        trimPointVertices();
        trimPointColors();
        trimPointAttribs();
      }

      if (0 < pointIndexCount && pointIndexCount < pointIndices.length) {
        trimPointIndices();
      }
    }

    void trimPolyVertices() {
      float temp[] = new float[4 * polyVertexCount];
      PApplet.arrayCopy(polyVertices, 0, temp, 0, 4 * polyVertexCount);
      polyVertices = temp;
    }

    void trimPolyColors() {
      int temp[] = new int[polyVertexCount];
      PApplet.arrayCopy(polyColors, 0, temp, 0, polyVertexCount);
      polyColors = temp;
    }

    void trimPolyNormals() {
      float temp[] = new float[3 * polyVertexCount];
      PApplet.arrayCopy(polyNormals, 0, temp, 0, 3 * polyVertexCount);
      polyNormals = temp;
    }

    void trimPolyTexcoords() {
      float temp[] = new float[2 * polyVertexCount];
      PApplet.arrayCopy(polyTexcoords, 0, temp, 0, 2 * polyVertexCount);
      polyTexcoords = temp;
    }

    void trimPolyAmbient() {
      int temp[] = new int[polyVertexCount];
      PApplet.arrayCopy(polyAmbient, 0, temp, 0, polyVertexCount);
      polyAmbient = temp;
    }

    void trimPolySpecular() {
      int temp[] = new int[polyVertexCount];
      PApplet.arrayCopy(polySpecular, 0, temp, 0, polyVertexCount);
      polySpecular = temp;
    }

    void trimPolyEmissive() {
      int temp[] = new int[polyVertexCount];
      PApplet.arrayCopy(polyEmissive, 0, temp, 0, polyVertexCount);
      polyEmissive = temp;
    }

    void trimPolyShininess() {
      float temp[] = new float[polyVertexCount];
      PApplet.arrayCopy(polyShininess, 0, temp, 0, polyVertexCount);
      polyShininess = temp;
    }
    
    void trimPolyIndices() {
      short temp[] = new short[polyIndexCount];
      PApplet.arrayCopy(polyIndices, 0, temp, 0, polyIndexCount);
      polyIndices = temp;
    }

    void trimLineVertices() {
      float temp[] = new float[4 * lineVertexCount];
      PApplet.arrayCopy(lineVertices, 0, temp, 0, 4 * lineVertexCount);
      lineVertices = temp;
    }

    void trimLineColors() {
      int temp[] = new int[lineVertexCount];
      PApplet.arrayCopy(lineColors, 0, temp, 0, lineVertexCount);
      lineColors = temp;
    }

    void trimLineAttribs() {
      float temp[] = new float[4 * lineVertexCount];
      PApplet.arrayCopy(lineAttribs, 0, temp, 0, 4 * lineVertexCount);
      lineAttribs = temp;
    }

    void trimLineIndices() {
      short temp[] = new short[lineIndexCount];
      PApplet.arrayCopy(lineIndices, 0, temp, 0, lineIndexCount);
      lineIndices = temp;
    }

    void trimPointVertices() {
      float temp[] = new float[4 * pointVertexCount];
      PApplet.arrayCopy(pointVertices, 0, temp, 0, 4 * pointVertexCount);
      pointVertices = temp;
    }

    void trimPointColors() {
      int temp[] = new int[pointVertexCount];
      PApplet.arrayCopy(pointColors, 0, temp, 0, pointVertexCount);
      pointColors = temp;
    }

    void trimPointAttribs() {
      float temp[] = new float[2 * pointVertexCount];
      PApplet.arrayCopy(pointAttribs, 0, temp, 0, 2 * pointVertexCount);
      pointAttribs = temp;
    }

    void trimPointIndices() {
      short temp[] = new short[pointIndexCount];
      PApplet.arrayCopy(pointIndices, 0, temp, 0, pointIndexCount);
      pointIndices = temp;
    }
    
    // -----------------------------------------------------------------
    //
    // Aggregation methods  
    
    void incPolyIndices(int first, int last, int inc) {
      for (int i = first; i <= last; i++) {
        polyIndices[i] += inc;
      }
    }
    
    void incLineIndices(int first, int last, int inc) {
      for (int i = first; i <= last; i++) {
        lineIndices[i] += inc;
      }
    }
    
    void incPointIndices(int first, int last, int inc) {
      for (int i = first; i <= last; i++) {
        pointIndices[i] += inc;
      }
    }
    
    // -----------------------------------------------------------------
    //
    // Normal calculation    
    
    void calcPolyNormal(int i0, int i1, int i2) {
      int index;

      index = 4 * i0;
      float x0 = polyVertices[index++];
      float y0 = polyVertices[index++];
      float z0 = polyVertices[index  ];

      index = 4 * i1;
      float x1 = polyVertices[index++];
      float y1 = polyVertices[index++];
      float z1 = polyVertices[index  ];

      index = 4 * i2;
      float x2 = polyVertices[index++];
      float y2 = polyVertices[index++];
      float z2 = polyVertices[index  ];

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
      polyNormals[index++] = nx;
      polyNormals[index++] = ny;
      polyNormals[index  ] = nz;

      index = 3 * i1;
      polyNormals[index++] = nx;
      polyNormals[index++] = ny;
      polyNormals[index  ] = nz;

      index = 3 * i2;
      polyNormals[index++] = nx;
      polyNormals[index++] = ny;
      polyNormals[index  ] = nz;
    }    
    
    // -----------------------------------------------------------------
    //
    // Add point geometry
    
    // Sets point vertex with index tessIdx using the data from input vertex inIdx.
    void setPointVertex(int tessIdx, InGeometry in, int inIdx) {
      int index;

      index = 3 * inIdx;
      float x = in.vertices[index++];
      float y = in.vertices[index++];
      float z = in.vertices[index  ];

      if (renderMode == IMMEDIATE && flushMode == FLUSH_WHEN_FULL && !hints[DISABLE_TRANSFORM_CACHE]) {
        PMatrix3D mm = modelview;

        index = 4 * tessIdx;
        pointVertices[index++] = x * mm.m00 + y * mm.m01 + z * mm.m02 + mm.m03;
        pointVertices[index++] = x * mm.m10 + y * mm.m11 + z * mm.m12 + mm.m13;
        pointVertices[index++] = x * mm.m20 + y * mm.m21 + z * mm.m22 + mm.m23;
        pointVertices[index  ] = x * mm.m30 + y * mm.m31 + z * mm.m32 + mm.m33;
      } else {
        index = 4 * tessIdx;
        pointVertices[index++] = x;
        pointVertices[index++] = y;
        pointVertices[index++] = z;
        pointVertices[index  ] = 1;
      }

      pointColors[tessIdx] = in.strokeColors[inIdx];    
    }        
    
    // -----------------------------------------------------------------
    //
    // Add line geometry
    
    void setLineVertex(int tessIdx, InGeometry in, int inIdx0, int rgba) {
      int index;

      index = 3 * inIdx0;
      float x0 = in.vertices[index++];
      float y0 = in.vertices[index++];
      float z0 = in.vertices[index  ];
      
      if (renderMode == IMMEDIATE && flushMode == FLUSH_WHEN_FULL && !hints[DISABLE_TRANSFORM_CACHE]) {
        PMatrix3D mm = modelview;

        index = 4 * tessIdx;
        lineVertices[index++] = x0 * mm.m00 + y0 * mm.m01 + z0 * mm.m02 + mm.m03;
        lineVertices[index++] = x0 * mm.m10 + y0 * mm.m11 + z0 * mm.m12 + mm.m13;
        lineVertices[index++] = x0 * mm.m20 + y0 * mm.m21 + z0 * mm.m22 + mm.m23;
        lineVertices[index  ] = x0 * mm.m30 + y0 * mm.m31 + z0 * mm.m32 + mm.m33;
      } else {
        index = 4 * tessIdx;
        lineVertices[index++] = x0;
        lineVertices[index++] = y0;
        lineVertices[index++] = z0;
        lineVertices[index  ] = 1;
      }
      
      lineColors[tessIdx] = rgba;    
      index = 4 * tessIdx;
      lineAttribs[index++] = 0;
      lineAttribs[index++] = 0;
      lineAttribs[index++] = 0;      
      lineAttribs[index  ] = 0;      
    }
    
    // Sets line vertex with index tessIdx using the data from input vertices inIdx0 and inIdx1.
    void setLineVertex(int tessIdx, InGeometry in, int inIdx0, int inIdx1, int rgba, float weight) {
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

        index = 4 * tessIdx;
        lineVertices[index++] = x0 * mm.m00 + y0 * mm.m01 + z0 * mm.m02 + mm.m03;
        lineVertices[index++] = x0 * mm.m10 + y0 * mm.m11 + z0 * mm.m12 + mm.m13;
        lineVertices[index++] = x0 * mm.m20 + y0 * mm.m21 + z0 * mm.m22 + mm.m23;
        lineVertices[index  ] = x0 * mm.m30 + y0 * mm.m31 + z0 * mm.m32 + mm.m33;

        index = 4 * tessIdx;
        lineAttribs[index++] = x1 * mm.m00 + y1 * mm.m01 + z1 * mm.m02 + mm.m03;
        lineAttribs[index++] = x1 * mm.m10 + y1 * mm.m11 + z1 * mm.m12 + mm.m13;
        lineAttribs[index  ] = x1 * mm.m20 + y1 * mm.m21 + z1 * mm.m22 + mm.m23;
      } else {
        index = 4 * tessIdx;
        lineVertices[index++] = x0;
        lineVertices[index++] = y0;
        lineVertices[index++] = z0;
        lineVertices[index  ] = 1;

        index = 4 * tessIdx;
        lineAttribs[index++] = x1;
        lineAttribs[index++] = y1;
        lineAttribs[index  ] = z1;         
      }

      lineColors[tessIdx] = rgba;      
      lineAttribs[4 * tessIdx + 3] = weight;
    }
    
    // -----------------------------------------------------------------
    //
    // Add poly geometry
    
    void setPolyVertex(int tessIdx, float x, float y, float z, int rgba) {
      setPolyVertex(tessIdx, x, y, z, 
                    rgba,
                    0, 0, 1, 
                    0, 0, 
                    0, 0, 0, 0);
    }
    
    void setPolyVertex(int tessIdx, float x, float y, float z,
                       int rgba,
                       float nx, float ny, float nz,
                       float u, float v,
                       int am, int sp, int em, float shine) {
      int index;      

      if (renderMode == IMMEDIATE && flushMode == FLUSH_WHEN_FULL && !hints[DISABLE_TRANSFORM_CACHE]) {
        PMatrix3D mm = modelview;
        PMatrix3D nm = modelviewInv;

        index = 4 * tessIdx;
        polyVertices[index++] = x * mm.m00 + y * mm.m01 + z * mm.m02 + mm.m03;
        polyVertices[index++] = x * mm.m10 + y * mm.m11 + z * mm.m12 + mm.m13;
        polyVertices[index++] = x * mm.m20 + y * mm.m21 + z * mm.m22 + mm.m23;
        polyVertices[index  ] = x * mm.m30 + y * mm.m31 + z * mm.m32 + mm.m33;

        index = 3 * tessIdx;
        polyNormals[index++] = nx * nm.m00 + ny * nm.m10 + nz * nm.m20;
        polyNormals[index++] = nx * nm.m01 + ny * nm.m11 + nz * nm.m21;
        polyNormals[index  ] = nx * nm.m02 + ny * nm.m12 + nz * nm.m22;
      } else {
        index = 4 * tessIdx;
        polyVertices[index++] = x;
        polyVertices[index++] = y;
        polyVertices[index++] = z;
        polyVertices[index  ] = 1;

        index = 3 * tessIdx;
        polyNormals[index++] = nx;
        polyNormals[index++] = ny;
        polyNormals[index  ] = nz;
      }

      polyColors[tessIdx] = rgba;

      index = 2 * tessIdx;
      polyTexcoords[index++] = u;
      polyTexcoords[index  ] = v;

      polyAmbient[tessIdx] = am;
      polySpecular[tessIdx] = sp;
      polyEmissive[tessIdx] = em;
      polyShininess[tessIdx] = shine;     
    }
    
    void addPolyVertex(float x, float y, float z,
                       int rgba,
                       float nx, float ny, float nz,
                       float u, float v,
                       int am, int sp, int em, float shine) {
      polyVertexCheck();
      int index;
      int count = polyVertexCount - 1;

      if (renderMode == IMMEDIATE && flushMode == FLUSH_WHEN_FULL && !hints[DISABLE_TRANSFORM_CACHE]) {
        PMatrix3D mm = modelview;
        PMatrix3D nm = modelviewInv;

        index = 4 * count;
        polyVertices[index++] = x * mm.m00 + y * mm.m01 + z * mm.m02 + mm.m03;
        polyVertices[index++] = x * mm.m10 + y * mm.m11 + z * mm.m12 + mm.m13;
        polyVertices[index++] = x * mm.m20 + y * mm.m21 + z * mm.m22 + mm.m23;
        polyVertices[index  ] = x * mm.m30 + y * mm.m31 + z * mm.m32 + mm.m33;

        index = 3 * count;
        polyNormals[index++] = nx * nm.m00 + ny * nm.m10 + nz * nm.m20;
        polyNormals[index++] = nx * nm.m01 + ny * nm.m11 + nz * nm.m21;
        polyNormals[index  ] = nx * nm.m02 + ny * nm.m12 + nz * nm.m22;
      } else {
        index = 4 * count;
        polyVertices[index++] = x;
        polyVertices[index++] = y;
        polyVertices[index++] = z;
        polyVertices[index  ] = 1;

        index = 3 * count;
        polyNormals[index++] = nx;
        polyNormals[index++] = ny;
        polyNormals[index  ] = nz;
      }
      
      polyColors[count] = rgba;

      index = 2 * count;
      polyTexcoords[index++] = u;
      polyTexcoords[index  ] = v;

      polyAmbient[count] = am;
      polySpecular[count] = sp;
      polyEmissive[count] = em;
      polyShininess[count] = shine; 
    }
    
    void addPolyVertices(InGeometry in) {
      addPolyVertices(in, in.firstVertex, in.lastVertex);
    }    

    void addPolyVertex(InGeometry in, int i) {
      addPolyVertices(in, i, i);
    }
    
    void addPolyVertices(InGeometry in, int i0, int i1) {
      int index;
      int nvert = i1 - i0 + 1;

      polyVertexCheck(nvert);
      
      if (renderMode == IMMEDIATE && flushMode == FLUSH_WHEN_FULL && !hints[DISABLE_TRANSFORM_CACHE]) {
        PMatrix3D mm = modelview;
        PMatrix3D nm = modelviewInv;

        for (int i = 0; i < nvert; i++) {
          int inIdx = i0 + i;
          int tessIdx = firstPolyVertex + i;

          index = 3 * inIdx;
          float x = in.vertices[index++];
          float y = in.vertices[index++];
          float z = in.vertices[index  ];

          index = 3 * inIdx;
          float nx = in.normals[index++];
          float ny = in.normals[index++];
          float nz = in.normals[index  ];

          index = 4 * tessIdx;
          polyVertices[index++] = x * mm.m00 + y * mm.m01 + z * mm.m02 + mm.m03;
          polyVertices[index++] = x * mm.m10 + y * mm.m11 + z * mm.m12 + mm.m13;
          polyVertices[index++] = x * mm.m20 + y * mm.m21 + z * mm.m22 + mm.m23;
          polyVertices[index  ] = x * mm.m30 + y * mm.m31 + z * mm.m32 + mm.m33;

          index = 3 * tessIdx;
          polyNormals[index++] = nx * nm.m00 + ny * nm.m10 + nz * nm.m20;
          polyNormals[index++] = nx * nm.m01 + ny * nm.m11 + nz * nm.m21;
          polyNormals[index  ] = nx * nm.m02 + ny * nm.m12 + nz * nm.m22;
        }
      } else {
        if (nvert <= PGL.MIN_ARRAYCOPY_SIZE) {
          // Copying elements one by one instead of using arrayCopy is more efficient for
          // few vertices...
          for (int i = 0; i < nvert; i++) {
            int inIdx = i0 + i;
            int tessIdx = firstPolyVertex + i;

            index = 3 * inIdx;
            float x = in.vertices[index++];
            float y = in.vertices[index++];
            float z = in.vertices[index  ];

            index = 3 * inIdx;
            float nx = in.normals[index++];
            float ny = in.normals[index++];
            float nz = in.normals[index  ];

            index = 4 * tessIdx;
            polyVertices[index++] = x;
            polyVertices[index++] = y;
            polyVertices[index++] = z;
            polyVertices[index  ] = 1;

            index = 3 * tessIdx;
            polyNormals[index++] = nx;
            polyNormals[index++] = ny;
            polyNormals[index  ] = nz;
          }
        } else {
          for (int i = 0; i < nvert; i++) {
            int inIdx = i0 + i;
            int tessIdx = firstPolyVertex + i;
            PApplet.arrayCopy(in.vertices, 3 * inIdx, polyVertices, 4 * tessIdx, 3);
            polyVertices[4 * tessIdx + 3] = 1;            
          }
          PApplet.arrayCopy(in.normals, 3 * i0, polyNormals, 3 * firstPolyVertex, 3 * nvert);
        }
      }

      if (nvert <= PGL.MIN_ARRAYCOPY_SIZE) {
        for (int i = 0; i < nvert; i++) {
          int inIdx = i0 + i;
          int tessIdx = firstPolyVertex + i;

          index = 2 * inIdx;
          float u = in.texcoords[index++];
          float v = in.texcoords[index  ];
          
          polyColors[tessIdx] = in.colors[inIdx];

          index = 2 * tessIdx;
          polyTexcoords[index++] = u;
          polyTexcoords[index  ] = v;

          polyAmbient[tessIdx] = in.ambient[inIdx];
          polySpecular[tessIdx] = in.specular[inIdx];
          polyEmissive[tessIdx] = in.emissive[inIdx];
          polyShininess[tessIdx] = in.shininess[inIdx];
        }
      } else {
        PApplet.arrayCopy(in.colors, i0, polyColors, firstPolyVertex, nvert);
        PApplet.arrayCopy(in.texcoords, 2 * i0, polyTexcoords, 2 * firstPolyVertex, 2 * nvert);
        PApplet.arrayCopy(in.ambient, i0, polyAmbient, firstPolyVertex, nvert);
        PApplet.arrayCopy(in.specular, i0, polySpecular, firstPolyVertex, nvert);
        PApplet.arrayCopy(in.emissive, i0, polyEmissive, firstPolyVertex, nvert);
        PApplet.arrayCopy(in.shininess, i0, polyShininess, firstPolyVertex, nvert);
      }    
    }

    // -----------------------------------------------------------------
    //
    // Matrix transformations

    void applyMatrixOnPolyGeometry(PMatrix tr, int first, int last) {
      if (tr instanceof PMatrix2D) {
        applyMatrixOnPolyGeometry((PMatrix2D) tr, first, last);
      } else if (tr instanceof PMatrix3D) {
        applyMatrixOnPolyGeometry((PMatrix3D) tr, first, last);
      }   
    }

    void applyMatrixOnLineGeometry(PMatrix tr, int first, int last) {
      if (tr instanceof PMatrix2D) {
        applyMatrixOnLineGeometry((PMatrix2D) tr, first, last);
      } else if (tr instanceof PMatrix3D) {
        applyMatrixOnLineGeometry((PMatrix3D) tr, first, last);
      }        
    }    

    void applyMatrixOnPointGeometry(PMatrix tr, int first, int last) {
      if (tr instanceof PMatrix2D) {
        applyMatrixOnPointGeometry((PMatrix2D) tr, first, last);
      } else if (tr instanceof PMatrix3D) {
        applyMatrixOnPointGeometry((PMatrix3D) tr, first, last);
      }      
    }

    void applyMatrixOnPolyGeometry(PMatrix2D tr, int first, int last) {
      if (first < last) {
        int index;

        for (int i = first; i <= last; i++) {
          index = 4 * i;
          float x = polyVertices[index++];
          float y = polyVertices[index  ];

          index = 3 * i;
          float nx = polyNormals[index++];
          float ny = polyNormals[index  ];

          index = 4 * i;
          polyVertices[index++] = x * tr.m00 + y * tr.m01 + tr.m02;
          polyVertices[index  ] = x * tr.m10 + y * tr.m11 + tr.m12;          

          index = 3 * i;
          polyNormals[index++] = nx * tr.m00 + ny * tr.m01;
          polyNormals[index  ] = nx * tr.m10 + ny * tr.m11;
        }
      }
    }
        
    void applyMatrixOnLineGeometry(PMatrix2D tr, int first, int last) {
      if (first < last) {
        int index;

        for (int i = first; i <= last; i++) {
          index = 4 * i;
          float x = lineVertices[index++];
          float y = lineVertices[index  ];

          index = 4 * i;
          float xa = lineAttribs[index++];
          float ya = lineAttribs[index  ];

          index = 4 * i;
          lineVertices[index++] = x * tr.m00 + y * tr.m01 + tr.m02;
          lineVertices[index  ] = x * tr.m10 + y * tr.m11 + tr.m12;

          index = 4 * i;
          lineAttribs[index++] = xa * tr.m00 + ya * tr.m01 + tr.m02;
          lineAttribs[index  ] = xa * tr.m10 + ya * tr.m11 + tr.m12;
        }
      }      
    } 
    
    void applyMatrixOnPointGeometry(PMatrix2D tr, int first, int last) {
      if (first < last) {
        int index;

        for (int i = first; i <= last; i++) {
          index = 4 * i;
          float x = pointVertices[index++];
          float y = pointVertices[index  ];

          index = 4 * i;
          pointVertices[index++] = x * tr.m00 + y * tr.m01 + tr.m02;
          pointVertices[index  ] = x * tr.m10 + y * tr.m11 + tr.m12;
        }
      }      
    }    
    
    void applyMatrixOnPolyGeometry(PMatrix3D tr, int first, int last) {
      if (first < last) {
        int index;

        for (int i = first; i <= last; i++) {
          index = 4 * i;
          float x = polyVertices[index++];
          float y = polyVertices[index++];
          float z = polyVertices[index++];
          float w = polyVertices[index  ];

          index = 3 * i;
          float nx = polyNormals[index++];
          float ny = polyNormals[index++];
          float nz = polyNormals[index  ];

          index = 4 * i;
          polyVertices[index++] = x * tr.m00 + y * tr.m01 + z * tr.m02 + w * tr.m03;
          polyVertices[index++] = x * tr.m10 + y * tr.m11 + z * tr.m12 + w * tr.m13;
          polyVertices[index++] = x * tr.m20 + y * tr.m21 + z * tr.m22 + w * tr.m23;
          polyVertices[index  ] = x * tr.m30 + y * tr.m31 + z * tr.m32 + w * tr.m33;

          index = 3 * i;
          polyNormals[index++] = nx * tr.m00 + ny * tr.m01 + nz * tr.m02;
          polyNormals[index++] = nx * tr.m10 + ny * tr.m11 + nz * tr.m12;
          polyNormals[index  ] = nx * tr.m20 + ny * tr.m21 + nz * tr.m22;
        }
      }
    }
    
    void applyMatrixOnLineGeometry(PMatrix3D tr, int first, int last) {
      if (first < last) {
        int index;

        for (int i = first; i <= last; i++) {
          index = 4 * i;
          float x = lineVertices[index++];
          float y = lineVertices[index++];
          float z = lineVertices[index++];
          float w = lineVertices[index  ];

          index = 4 * i;
          float xa = lineAttribs[index++];
          float ya = lineAttribs[index++];
          float za = lineAttribs[index  ];

          index = 4 * i;
          lineVertices[index++] = x * tr.m00 + y * tr.m01 + z * tr.m02 + w * tr.m03;
          lineVertices[index++] = x * tr.m10 + y * tr.m11 + z * tr.m12 + w * tr.m13;
          lineVertices[index++] = x * tr.m20 + y * tr.m21 + z * tr.m22 + w * tr.m23;
          lineVertices[index  ] = x * tr.m30 + y * tr.m31 + z * tr.m32 + w * tr.m33;

          index = 4 * i;
          lineAttribs[index++] = xa * tr.m00 + ya * tr.m01 + za * tr.m02 + tr.m03;
          lineAttribs[index++] = xa * tr.m10 + ya * tr.m11 + za * tr.m12 + tr.m13;
          lineAttribs[index  ] = xa * tr.m20 + ya * tr.m21 + za * tr.m22 + tr.m23;
        }
      }      
    }
    
    void applyMatrixOnPointGeometry(PMatrix3D tr, int first, int last) {
      if (first < last) {
        int index;

        for (int i = first; i <= last; i++) {
          index = 4 * i;
          float x = pointVertices[index++];
          float y = pointVertices[index++];
          float z = pointVertices[index++];
          float w = pointVertices[index  ];

          index = 4 * i;
          pointVertices[index++] = x * tr.m00 + y * tr.m01 + z * tr.m02 + w * tr.m03;
          pointVertices[index++] = x * tr.m10 + y * tr.m11 + z * tr.m12 + w * tr.m13;
          pointVertices[index++] = x * tr.m20 + y * tr.m21 + z * tr.m22 + w * tr.m23;
          pointVertices[index  ] = x * tr.m30 + y * tr.m31 + z * tr.m32 + w * tr.m33;
        }
      }
    }
  }

  // Generates tessellated geometry given a batch of input vertices.
  protected class Tessellator {
    InGeometry in;
    TessGeometry tess;
    TexCache texCache; 
    PImage prevTexImage; 
    PImage newTexImage;    
    int firstTexIndex;
    int firstTexCache;    
    
    PGL.Tessellator gluTess;
    TessellatorCallback callback;
    
    boolean fill;
    boolean stroke;
    int strokeColor;
    float strokeWeight;
    int strokeJoin;
    int strokeCap;
    boolean accurate2DStrokes;

    PMatrix transform;
    boolean is2D, is3D;    
    
    int[] rawIndices;
    int rawSize;

    int firstPolyIndexCache;
    int lastPolyIndexCache;
    int firstLineIndexCache;
    int lastLineIndexCache;    
    int firstPointIndexCache;
    int lastPointIndexCache;
    
    Tessellator() {
      callback = new TessellatorCallback();
      gluTess = pgl.createTessellator(callback);
      rawIndices = new int[512];
      accurate2DStrokes = true;
      transform = null;
      is2D = false;
      is3D = true;      
    }
    
    void setInGeometry(InGeometry in) {
      this.in = in;

      firstPolyIndexCache = -1;
      lastPolyIndexCache = -1;
      firstLineIndexCache = -1;
      lastLineIndexCache = -1;      
      firstPointIndexCache = -1;
      lastPointIndexCache = -1;
    }

    void setTessGeometry(TessGeometry tess) {
      this.tess = tess;
    }

    void setFill(boolean fill) {
      this.fill = fill;
    }

    void setStroke(boolean stroke) {
      this.stroke = stroke;
    }
    
    void setStrokeColor(int color) {
      this.strokeColor = PGL.javaToNativeARGB(color);
    }

    void setStrokeWeight(float weight) {
      this.strokeWeight = weight;
    }

    void setStrokeJoin(int strokeJoin) {
      this.strokeJoin = strokeJoin;
    }

    void setStrokeCap(int strokeCap) {
      this.strokeCap = strokeCap;
    }
    
    void setAccurate2DStrokes(boolean accurate) {
      this.accurate2DStrokes = accurate;
    }    
    
    void setTexCache(TexCache texCache, PImage prevTexImage, PImage newTexImage) {
      this.texCache = texCache;
      this.prevTexImage = prevTexImage;
      this.newTexImage = newTexImage;
    }

    void set3D(boolean value) {
      if (value) {
        this.is2D = false;
        this.is3D = true;
      } else {
        this.is2D = true;
        this.is3D = false;        
      }
    }
    
    void setTransform(PMatrix transform) {
      this.transform = transform;
    }
    
    // -----------------------------------------------------------------
    //
    // Point tessellation    
    
    void tessellatePoints() {
      if (strokeCap == ROUND) {
        tessellateRoundPoints();
      } else {
        tessellateSquarePoints();
      }
    }

    void tessellateRoundPoints() {
      int nInVert = in.lastVertex - in.firstVertex + 1;
      if (stroke && 1 <= nInVert) {
        // Each point generates a separate triangle fan.
        // The number of triangles of each fan depends on the
        // stroke weight of the point.        
        int nPtVert = PApplet.max(MIN_POINT_ACCURACY, 
                                  (int) (TWO_PI * strokeWeight / POINT_ACCURACY_FACTOR)) + 1;
        if (PGL.MAX_VERTEX_INDEX1 <= nPtVert) {
          throw new RuntimeException("P3D: error in point tessellation.");
        }        
        updateTex();
        int nvertTot = nPtVert * nInVert;
        int nindTot = 3 * (nPtVert - 1) * nInVert;
        if (is3D) {          
          tessellateRoundPoints3D(nvertTot, nindTot, nPtVert);
        } else if (is2D) {
          beginNoTex();
          tessellateRoundPoints2D(nvertTot, nindTot, nPtVert);
          endNoTex();      
        }
      }
    }
    
    void tessellateRoundPoints3D(int nvertTot, int nindTot, int nPtVert) {
      int perim = nPtVert - 1;
      tess.pointVertexCheck(nvertTot);
      tess.pointIndexCheck(nindTot);
      int vertIdx = tess.firstPointVertex;
      int attribIdx = tess.firstPointVertex;
      int indIdx = tess.firstPointIndex;
      IndexCache cache = tess.pointIndexCache;
      int index = in.renderMode == RETAINED ? cache.addNew() : cache.getLast();
      firstPointIndexCache = index;        
      for (int i = in.firstVertex; i <= in.lastVertex; i++) {
        // Creating the triangle fan for each input vertex.          
        
        int count = cache.vertexCount[index];
        if (PGL.MAX_VERTEX_INDEX1 <= count + nPtVert) {
          // We need to start a new index block for this point.
          index = cache.addNew();
          count = 0;
        }           

        // All the tessellated vertices are identical to the center point
        for (int k = 0; k < nPtVert; k++) {
          tess.setPointVertex(vertIdx, in, i);            
          vertIdx++;
        }

        // The attributes for each tessellated vertex are the displacement along
        // the circle perimeter. The point shader will read these attributes and
        // displace the vertices in screen coordinates so the circles are always
        // camera facing (bilboards)
        tess.pointAttribs[2 * attribIdx + 0] = 0;
        tess.pointAttribs[2 * attribIdx + 1] = 0;
        attribIdx++;
        float val = 0;
        float inc = (float) SINCOS_LENGTH / perim;
        for (int k = 0; k < perim; k++) {
          tess.pointAttribs[2 * attribIdx + 0] = 0.5f * cosLUT[(int) val] * strokeWeight;
          tess.pointAttribs[2 * attribIdx + 1] = 0.5f * sinLUT[(int) val] * strokeWeight;
          val = (val + inc) % SINCOS_LENGTH;
          attribIdx++;
        }

        // Adding vert0 to take into account the triangles of all
        // the preceding points.
        for (int k = 1; k < nPtVert - 1; k++) {
          tess.pointIndices[indIdx++] = (short) (count + 0);
          tess.pointIndices[indIdx++] = (short) (count + k);
          tess.pointIndices[indIdx++] = (short) (count + k + 1);
        }
        // Final triangle between the last and first point:
        tess.pointIndices[indIdx++] = (short) (count + 0);
        tess.pointIndices[indIdx++] = (short) (count + 1);
        tess.pointIndices[indIdx++] = (short) (count + nPtVert - 1);

        cache.incCounts(index, 3 * (nPtVert - 1), nPtVert);
      }
      lastPointIndexCache = index;             
    }

    void tessellateRoundPoints2D(int nvertTot, int nindTot, int nPtVert) {
      int perim = nPtVert - 1;
      tess.polyVertexCheck(nvertTot);
      tess.polyIndexCheck(nindTot);
      int vertIdx = tess.firstPolyVertex;
      int indIdx = tess.firstPolyIndex;
      IndexCache cache = tess.polyIndexCache;
      int index = in.renderMode == RETAINED ? cache.addNew() : cache.getLast();
      firstPointIndexCache = index;        
      for (int i = in.firstVertex; i <= in.lastVertex; i++) {
        int count = cache.vertexCount[index];
        if (PGL.MAX_VERTEX_INDEX1 <= count + nPtVert) {
          // We need to start a new index block for this point.
          index = cache.addNew();
          count = 0;
        }   
        
        float x0 = in.vertices[3 * i + 0];
        float y0 = in.vertices[3 * i + 1];
        int rgba = in.strokeColors[i];
        
        float val = 0;
        float inc = (float) SINCOS_LENGTH / perim;            
        tess.setPolyVertex(vertIdx, x0, y0, 0, rgba);              
        vertIdx++;            
        for (int k = 0; k < perim; k++) {
          tess.setPolyVertex(vertIdx, x0 + 0.5f * cosLUT[(int) val] * strokeWeight, 
                                      y0 + 0.5f * sinLUT[(int) val] * strokeWeight, 0, rgba);
          vertIdx++;
          val = (val + inc) % SINCOS_LENGTH;
        }
        
        // Adding vert0 to take into account the triangles of all
        // the preceding points.
        for (int k = 1; k < nPtVert - 1; k++) {
          tess.polyIndices[indIdx++] = (short) (count + 0);
          tess.polyIndices[indIdx++] = (short) (count + k);
          tess.polyIndices[indIdx++] = (short) (count + k + 1);
        }
        // Final triangle between the last and first point:
        tess.polyIndices[indIdx++] = (short) (count + 0);
        tess.polyIndices[indIdx++] = (short) (count + 1);
        tess.polyIndices[indIdx++] = (short) (count + nPtVert - 1);

        cache.incCounts(index, 3 * (nPtVert - 1), nPtVert);            
      }
      lastPointIndexCache = lastPolyIndexCache = index; 
    }    
    
    void tessellateSquarePoints() {
      int nInVert = in.lastVertex - in.firstVertex + 1;
      if (stroke && 1 <= nInVert) {
        updateTex();
        int quadCount = nInVert; // Each point generates a separate quad.
        // Each quad is formed by 5 vertices, the center one
        // is the input vertex, and the other 4 define the
        // corners (so, a triangle fan again).
        int nvertTot = 5 * quadCount;
        // So the quad is formed by 4 triangles, each requires
        // 3 indices.
        int nindTot = 12 * quadCount;        
        if (is3D) {           
          tessellateSquarePoints3D(nvertTot, nindTot);
        } else if (is2D) {
          beginNoTex();
          tessellateSquarePoints2D(nvertTot, nindTot);
          endNoTex();
        }
      }
    }
    
    void tessellateSquarePoints3D(int nvertTot, int nindTot) {
      tess.pointVertexCheck(nvertTot);
      tess.pointIndexCheck(nindTot);
      int vertIdx = tess.firstPointVertex;
      int attribIdx = tess.firstPointVertex;
      int indIdx = tess.firstPointIndex;
      IndexCache cache = tess.pointIndexCache;
      int index = in.renderMode == RETAINED ? cache.addNew() : cache.getLast();        
      firstPointIndexCache = index;
      for (int i = in.firstVertex; i <= in.lastVertex; i++) {
        int nvert = 5;
        int count = cache.vertexCount[index];
        if (PGL.MAX_VERTEX_INDEX1 <= count + nvert) {
          // We need to start a new index block for this point.
          index = cache.addNew();
          count = 0;
        }        
        
        for (int k = 0; k < nvert; k++) {
          tess.setPointVertex(vertIdx, in, i);
          vertIdx++;
        }

        // The attributes for each tessellated vertex are the displacement along
        // the quad corners. The point shader will read these attributes and
        // displace the vertices in screen coordinates so the quads are always
        // camera facing (bilboards)
        tess.pointAttribs[2 * attribIdx + 0] = 0;
        tess.pointAttribs[2 * attribIdx + 1] = 0;
        attribIdx++;
        for (int k = 0; k < 4; k++) {
          tess.pointAttribs[2 * attribIdx + 0] = 0.5f * QUAD_POINT_SIGNS[k][0] * strokeWeight;
          tess.pointAttribs[2 * attribIdx + 1] = 0.5f * QUAD_POINT_SIGNS[k][1] * strokeWeight;
          attribIdx++;
        }

        // Adding firstVert to take into account the triangles of all
        // the preceding points.
        for (int k = 1; k < nvert - 1; k++) {
          tess.pointIndices[indIdx++] = (short) (count + 0);
          tess.pointIndices[indIdx++] = (short) (count + k);
          tess.pointIndices[indIdx++] = (short) (count + k + 1);
        }
        // Final triangle between the last and first point:
        tess.pointIndices[indIdx++] = (short) (count + 0);
        tess.pointIndices[indIdx++] = (short) (count + 1);
        tess.pointIndices[indIdx++] = (short) (count + nvert - 1);

        cache.incCounts(index, 12, 5);
      }
      lastPointIndexCache = index;
    }

    void tessellateSquarePoints2D(int nvertTot, int nindTot) {
      tess.polyVertexCheck(nvertTot);
      tess.polyIndexCheck(nindTot);     
      int vertIdx = tess.firstPolyVertex;
      int indIdx = tess.firstPolyIndex;
      IndexCache cache = tess.polyIndexCache;
      int index = in.renderMode == RETAINED ? cache.addNew() : cache.getLast();        
      firstPointIndexCache = index;
      for (int i = in.firstVertex; i <= in.lastVertex; i++) {
        int nvert = 5;
        int count = cache.vertexCount[index];
        if (PGL.MAX_VERTEX_INDEX1 <= count + nvert) {
          // We need to start a new index block for this point.
          index = cache.addNew();
          count = 0;
        }        
        
        float x0 = in.vertices[3 * i + 0];
        float y0 = in.vertices[3 * i + 1];
        int rgba = in.strokeColors[i];

        tess.setPolyVertex(vertIdx, x0, y0, 0, rgba);              
        vertIdx++;            
        for (int k = 0; k < nvert - 1; k++) {
          tess.setPolyVertex(vertIdx, x0 + 0.5f * QUAD_POINT_SIGNS[k][0] * strokeWeight, 
                                      y0 + 0.5f * QUAD_POINT_SIGNS[k][1] * strokeWeight, 0, rgba);
          vertIdx++;
        }            
        
        for (int k = 1; k < nvert - 1; k++) {
          tess.polyIndices[indIdx++] = (short) (count + 0);
          tess.polyIndices[indIdx++] = (short) (count + k);
          tess.polyIndices[indIdx++] = (short) (count + k + 1);
        }
        // Final triangle between the last and first point:
        tess.polyIndices[indIdx++] = (short) (count + 0);
        tess.polyIndices[indIdx++] = (short) (count + 1);
        tess.polyIndices[indIdx++] = (short) (count + nvert - 1);

        cache.incCounts(index, 12, 5);            
      }          
      lastPointIndexCache = lastPolyIndexCache = index;   
    }    
    
    // -----------------------------------------------------------------
    //
    // Line tessellation    
    
    void tessellateLines() {
      int nInVert = in.lastVertex - in.firstVertex + 1;      
      if (stroke && 2 <= nInVert) {
        updateTex();
        int lineCount = nInVert / 2; // Each individual line is formed by two consecutive input vertices.                
        if (is3D) {               
          tessellateLines3D(lineCount);
        } else if (is2D) {
          beginNoTex(); // Line geometry in 2D are stored in the poly array next to the fill triangles, but w/out textures.
          tessellateLines2D(lineCount);
          endNoTex();
        }
      }
    }
    
    void tessellateLines3D(int lineCount) {
      // Lines are made up of 4 vertices defining the quad. 
      int nvert = lineCount * 4;
      // Each stroke line has 4 vertices, defining 2 triangles, which
      // require 3 indices to specify their connectivities.        
      int nind = lineCount * 2 * 3;
      
      int first = in.firstVertex;
      tess.lineVertexCheck(nvert);
      tess.lineIndexCheck(nind);
      int index = in.renderMode == RETAINED ? tess.lineIndexCache.addNew() : tess.lineIndexCache.getLast();
      firstLineIndexCache = index;
      for (int ln = 0; ln < lineCount; ln++) {
        int i0 = first + 2 * ln + 0;
        int i1 = first + 2 * ln + 1;
        index = addLine3D(i0, i1, index, null, false);
      }
      lastLineIndexCache = index;                  
    }

    void tessellateLines2D(int lineCount) { 
      int nvert = lineCount * 4;        
      int nind = lineCount * 2 * 3;
      
      int first = in.firstVertex;
      if (noCapsJoins(nvert)) {
        tess.polyVertexCheck(nvert);
        tess.polyIndexCheck(nind);
        int index = in.renderMode == RETAINED ? tess.polyIndexCache.addNew() : tess.polyIndexCache.getLast();
        firstLineIndexCache = index;
        if (firstPolyIndexCache == -1) firstPolyIndexCache = index; // If the geometry has no fill, needs the first poly index.
        for (int ln = 0; ln < lineCount; ln++) {
          int i0 = first + 2 * ln + 0;
          int i1 = first + 2 * ln + 1;
          index = addLine2D(i0, i1, index, false);
        }
        lastLineIndexCache = lastPolyIndexCache = index;
      } else { // full stroking algorithm            
        LinePath path = new LinePath(LinePath.WIND_NON_ZERO);
        for (int ln = 0; ln < lineCount; ln++) {
          int i0 = first + 2 * ln + 0;
          int i1 = first + 2 * ln + 1;
          path.moveTo(in.vertices[3 * i0 + 0], in.vertices[3 * i0 + 1]);
          path.lineTo(in.vertices[3 * i1 + 0], in.vertices[3 * i1 + 1]);              
        }
        tessellateLinePath(path);
      }      
    }    
    
    void tessellateLineStrip() {
      int nInVert = in.lastVertex - in.firstVertex + 1;      
      if (stroke && 2 <= nInVert) {
        updateTex();
        int lineCount = nInVert - 1;  
        if (is3D) {          
          tessellateLineStrip3D(lineCount);
        } else if (is2D) {
          beginNoTex();
          tessellateLineStrip2D(lineCount);
          endNoTex();
        }  
      }
    }
    
    void tessellateLineStrip3D(int lineCount) {      
      int nvert = lineCount * 4 + (lineCount - 1); // (lineCount - 1) for the bevel triangles
      int nind = lineCount * 2 * 3 + (lineCount - 1) * 2 * 3; // same thing               
      
      tess.lineVertexCheck(nvert);
      tess.lineIndexCheck(nind);
      int index = in.renderMode == RETAINED ? tess.lineIndexCache.addNew() : tess.lineIndexCache.getLast();
      firstLineIndexCache = index;
      int i0 = in.firstVertex;
      short[] lastInd = {-1, -1};
      for (int ln = 0; ln < lineCount; ln++) {
        int i1 = in.firstVertex + ln + 1;
        index = addLine3D(i0, i1, index, lastInd, false);        
        i0 = i1;
      }         
      lastLineIndexCache = index;          
    }

    void tessellateLineStrip2D(int lineCount) {
      int nvert = lineCount * 4;
      int nind = lineCount * 2 * 3;                

      if (noCapsJoins(nvert)) {
        tess.polyVertexCheck(nvert);
        tess.polyIndexCheck(nind);
        int index = in.renderMode == RETAINED ? tess.polyIndexCache.addNew() : tess.polyIndexCache.getLast();
        firstLineIndexCache = index;
        if (firstPolyIndexCache == -1) firstPolyIndexCache = index; // If the geometry has no fill, needs the first poly index.
        int i0 = in.firstVertex;
        for (int ln = 0; ln < lineCount; ln++) {
          int i1 = in.firstVertex + ln + 1;
          index = addLine2D(i0, i1, index, false);
          i0 = i1;
        }         
        lastLineIndexCache = lastPolyIndexCache = index;
      } else {  // full stroking algorithm
        int first = in.firstVertex;          
        LinePath path = new LinePath(LinePath.WIND_NON_ZERO);
        path.moveTo(in.vertices[3 * first + 0], in.vertices[3 * first + 1]);
        for (int ln = 0; ln < lineCount; ln++) {
          int i1 = first + ln + 1;          
          path.lineTo(in.vertices[3 * i1 + 0], in.vertices[3 * i1 + 1]);
        }    
        tessellateLinePath(path);            
      }
    }    
    
    void tessellateLineLoop() {
      int nInVert = in.lastVertex - in.firstVertex + 1;
      if (stroke && 2 <= nInVert) {
        updateTex();
        int lineCount = nInVert;
        if (is3D) {          
          tessellateLineLoop3D(lineCount);
        } else if (is2D) {
          beginNoTex();  
          tessellateLineLoop2D(lineCount);
          endNoTex();
        }          
      }      
    }    
    
    void tessellateLineLoop3D(int lineCount) {
      // This calculation doesn't add the bevel join between
      // the first and last vertex, need to fix.
      int nvert = lineCount * 4 + (lineCount - 1);
      int nind = lineCount * 2 * 3 + (lineCount - 1) * 2 * 3;
      
      tess.lineVertexCheck(nvert);
      tess.lineIndexCheck(nind);
      int index = in.renderMode == RETAINED ? tess.lineIndexCache.addNew() : tess.lineIndexCache.getLast();
      firstLineIndexCache = index;
      int i0 = in.firstVertex;
      short[] lastInd = {-1, -1};
      for (int ln = 0; ln < lineCount - 1; ln++) {
        int i1 = in.firstVertex + ln + 1;
        index = addLine3D(i0, i1, index, lastInd, false);
        i0 = i1;
      }
      index = addLine3D(in.lastVertex, in.firstVertex, index, lastInd, false);
      lastLineIndexCache = index;          
    }

    void tessellateLineLoop2D(int lineCount) {
      int nvert = lineCount * 4;
      int nind = lineCount * 2 * 3;
      
      if (noCapsJoins(nvert)) {
        tess.polyVertexCheck(nvert);
        tess.polyIndexCheck(nind);
        int index = in.renderMode == RETAINED ? tess.polyIndexCache.addNew() : tess.polyIndexCache.getLast();
        firstLineIndexCache = index;
        if (firstPolyIndexCache == -1) firstPolyIndexCache = index; // If the geometry has no fill, needs the first poly index.
        int i0 = in.firstVertex;
        for (int ln = 0; ln < lineCount - 1; ln++) {
          int i1 = in.firstVertex + ln + 1;
          index = addLine2D(i0, i1, index, false);
          i0 = i1;
        }
        index = addLine2D(in.lastVertex, in.firstVertex, index, false);
        lastLineIndexCache = lastPolyIndexCache = index;
      } else { // full stroking algorithm           
        int first = in.firstVertex;          
        LinePath path = new LinePath(LinePath.WIND_NON_ZERO);
        path.moveTo(in.vertices[3 * first + 0], in.vertices[3 * first + 1]);
        for (int ln = 0; ln < lineCount - 1; ln++) {
          int i1 = first + ln + 1;          
          path.lineTo(in.vertices[3 * i1 + 0], in.vertices[3 * i1 + 1]);
        }    
        path.closePath();
        tessellateLinePath(path);              
      }
    }    
    
    void tessellateEdges() {
      if (stroke) {        
        if (is3D) {
          tessellateEdges3D();
        } else if (is2D) {
          beginNoTex();
          tessellateEdges2D();
          endNoTex();
        }
      }
    }
    
    void tessellateEdges3D() {
      // This calculation doesn't add the bevel join between
      // the first and last vertex, need to fix.
      int nInVert = in.getNumEdgeVertices(true);
      int nInInd = in.getNumEdgeIndices(true);
      
      tess.lineVertexCheck(nInVert);
      tess.lineIndexCheck(nInInd);
      int index = in.renderMode == RETAINED ? tess.lineIndexCache.addNew() : tess.lineIndexCache.getLast();
      firstLineIndexCache = index;
      short[] lastInd = {-1, -1};
      for (int i = in.firstEdge; i <= in.lastEdge; i++) {
        int[] edge = in.edges[i];
        int i0 = edge[0];
        int i1 = edge[1];        
        index = addLine3D(i0, i1, index, lastInd, true);        
        if (edge[2] == EDGE_STOP || edge[2] == EDGE_SINGLE) {
          // No join with next line segment.
          lastInd[0] = lastInd[1] = -1; 
        }
      }
      lastLineIndexCache = index;          
    }
    
    void tessellateEdges2D() {
      int nInVert = in.getNumEdgeVertices(false);            
      if (noCapsJoins(nInVert)) {
        int nInInd = in.getNumEdgeIndices(false);
        
        tess.polyVertexCheck(nInVert);
        tess.polyIndexCheck(nInInd);
        int index = in.renderMode == RETAINED ? tess.polyIndexCache.addNew() : tess.polyIndexCache.getLast();
        firstLineIndexCache = index;            
        if (firstPolyIndexCache == -1) firstPolyIndexCache = index; // If the geometry has no fill, needs the first poly index.
        for (int i = in.firstEdge; i <= in.lastEdge; i++) {
          int[] edge = in.edges[i];
          int i0 = edge[0];
          int i1 = edge[1];
          index = addLine2D(i0, i1, index, true);
        }
        lastLineIndexCache = lastPolyIndexCache = index;          
      } else { // full stroking algorithm       
        LinePath path = new LinePath(LinePath.WIND_NON_ZERO);
        for (int i = in.firstEdge; i <= in.lastEdge; i++) {
          int[] edge = in.edges[i];
          int i0 = edge[0];
          int i1 = edge[1];
          switch (edge[2]) {
          case EDGE_MIDDLE:
            path.lineTo(in.vertices[3 * i1 + 0], in.vertices[3 * i1 + 1]);
            break;
          case EDGE_START:
            path.moveTo(in.vertices[3 * i0 + 0], in.vertices[3 * i0 + 1]);
            path.lineTo(in.vertices[3 * i1 + 0], in.vertices[3 * i1 + 1]);
            break;
          case EDGE_STOP:
            path.lineTo(in.vertices[3 * i1 + 0], in.vertices[3 * i1 + 1]);
            path.closePath();
            break;
          case EDGE_SINGLE:
            path.moveTo(in.vertices[3 * i0 + 0], in.vertices[3 * i0 + 1]);
            path.lineTo(in.vertices[3 * i1 + 0], in.vertices[3 * i1 + 1]);
            path.closePath();
            break;              
          }
        }
        tessellateLinePath(path);             
      }      
    }
    
    // Adding the data that defines a quad starting at vertex i0 and
    // ending at i1.
    int addLine3D(int i0, int i1, int index, short[] lastInd, boolean constStroke) {
      IndexCache cache = tess.lineIndexCache;
      int count = cache.vertexCount[index];
      boolean addBevel = lastInd != null && -1 < lastInd[0] && -1 < lastInd[1];      
      if (PGL.MAX_VERTEX_INDEX1 <= count + 4 + (addBevel ? 1 : 0)) {
        // We need to start a new index block for this line.
        index = cache.addNew();
        count = 0;
      }
      int iidx = cache.indexOffset[index] + cache.indexCount[index];
      int vidx = cache.vertexOffset[index] + cache.vertexCount[index];
      int color, color0;
      float weight;
      
      color0 = color = constStroke ? strokeColor : in.strokeColors[i0];
      weight = constStroke ? strokeWeight : in.strokeWeights[i0];
      
      tess.setLineVertex(vidx++, in, i0, i1, color, +weight/2);
      tess.lineIndices[iidx++] = (short) (count + 0);      
            
      tess.setLineVertex(vidx++, in, i0, i1, color, -weight/2);
      tess.lineIndices[iidx++] = (short) (count + 1);
      
      color = constStroke ? strokeColor : in.strokeColors[i1];
      weight = constStroke ? strokeWeight : in.strokeWeights[i1];
      
      tess.setLineVertex(vidx++, in, i1, i0, color, -weight/2);
      tess.lineIndices[iidx++] = (short) (count + 2);
      
      // Starting a new triangle re-using prev vertices.
      tess.lineIndices[iidx++] = (short) (count + 2);      
      tess.lineIndices[iidx++] = (short) (count + 1);

      tess.setLineVertex(vidx++, in, i1, i0, color, +weight/2);
      tess.lineIndices[iidx++] = (short) (count + 3);
      
      cache.incCounts(index, 6, 4);
      
      if (lastInd != null) {
        if (-1 < lastInd[0] && -1 < lastInd[1]) {
          // Adding bevel triangles
          tess.setLineVertex(vidx, in, i0, color0);
          
          tess.lineIndices[iidx++] = (short) (count + 4);
          tess.lineIndices[iidx++] = lastInd[0];
          tess.lineIndices[iidx++] = (short) (count + 0);          
          
          tess.lineIndices[iidx++] = (short) (count + 4);
          tess.lineIndices[iidx++] = lastInd[1];
          tess.lineIndices[iidx  ] = (short) (count + 1);           
          
          cache.incCounts(index, 6, 1);
        }
        
        // Vertices for next bevel
        lastInd[0] = (short) (count + 2);
        lastInd[1] = (short) (count + 3);
      }      
      return index;
    } 
    
    // Adding the data that defines a quad starting at vertex i0 and
    // ending at i1, in the case of pure 2D renderers (line geometry
    // is added to the poly arrays).
    int addLine2D(int i0, int i1, int index, boolean constStroke) {
      IndexCache cache = tess.polyIndexCache;
      int count = cache.vertexCount[index];
      if (PGL.MAX_VERTEX_INDEX1 <= count + 4) {
        // We need to start a new index block for this line.
        index = cache.addNew();
        count = 0;
      }
      int iidx = cache.indexOffset[index] + cache.indexCount[index];
      int vidx = cache.vertexOffset[index] + cache.vertexCount[index];
      
      int color = constStroke ? strokeColor : in.strokeColors[i0];
      float weight = constStroke ? strokeWeight : in.strokeWeights[i0];
      
      float x0 = in.vertices[3 * i0 + 0];
      float y0 = in.vertices[3 * i0 + 1];
      
      float x1 = in.vertices[3 * i1 + 0];
      float y1 = in.vertices[3 * i1 + 1];

      // Calculating direction and normal of the line.
      float dirx = x1 - x0;
      float diry = y1 - y0;
      float llen = PApplet.sqrt(dirx * dirx + diry * diry);
      float normx = 0, normy = 0; 
      if (nonZero(llen)) {
        normx = -diry / llen;
        normy = +dirx / llen;        
      }
      
      tess.setPolyVertex(vidx++, x0 + normx * weight/2, y0 + normy * weight/2, 0, color); 
      tess.polyIndices[iidx++] = (short) (count + 0);      
      
      tess.setPolyVertex(vidx++, x0 - normx * weight/2, y0 - normy * weight/2, 0, color);
      tess.polyIndices[iidx++] = (short) (count + 1);
      
      if (!constStroke) {
        color =  in.strokeColors[i1];
        weight = in.strokeWeights[i1];
      }
 
      tess.setPolyVertex(vidx++, x1 - normx * weight/2, y1 - normy * weight/2, 0, color);
      tess.polyIndices[iidx++] = (short) (count + 2);
      
      // Starting a new triangle re-using prev vertices.
      tess.polyIndices[iidx++] = (short) (count + 2);      
      tess.polyIndices[iidx++] = (short) (count + 0);

      tess.setPolyVertex(vidx++, x1 + normx * weight/2, y1 + normy * weight/2, 0, color);
      tess.polyIndices[iidx++] = (short) (count + 3);   
      
      cache.incCounts(index, 6, 4);
      return index;
    }
    
    boolean noCapsJoins(int nInVert) {
      if (!accurate2DStrokes) {
        return true;
      } else if (PGL.MAX_CAPS_JOINS_LENGTH <= nInVert) {
        // The line path is too long, so it could make the GLU tess
        // to run out of memory, so full caps and joins are disabled.
        return true;        
      } else {
        // We first calculate the (volumetric) scaling factor that is associated 
        // to the current transformation matrix, which is given by the absolute 
        // value of its determinant:        
        float scaleFactor = 1;
        
        if (transform != null) {
          if (transform instanceof PMatrix2D) {
            PMatrix2D tr = (PMatrix2D)transform;
            float areaScaleFactor = Math.abs(tr.m00 * tr.m11 - tr.m01 * tr.m10);          
            scaleFactor = (float) Math.sqrt(areaScaleFactor);
          } else if (transform instanceof PMatrix3D) {
            PMatrix3D tr = (PMatrix3D)transform;
            float volumeScaleFactor = Math.abs(tr.m00 * (tr.m11 * tr.m22 - tr.m12 * tr.m21) +
                                               tr.m01 * (tr.m12 * tr.m20 - tr.m10 * tr.m22) +
                                               tr.m02 * (tr.m10 * tr.m21 - tr.m11 * tr.m20));
            scaleFactor = (float) Math.pow(volumeScaleFactor, 1.0f / 3.0f);          
          }          
        }
        
        // The stroke weight is scaled so it correspons to the current 
        // "zoom level" being applied on the geometry due to scaling:
        return scaleFactor * strokeWeight < PGL.MIN_CAPS_JOINS_WEIGHT;
      }
    }    
    
    // -----------------------------------------------------------------
    //
    // Polygon primitives tessellation      
    
    void tessellateTriangles() {
      beginTex();
      int nInVert = in.lastVertex - in.firstVertex + 1;
      if (fill && 3 <= nInVert) {
        int nInInd = nInVert;
        setRawSize(nInInd);
        int idx = 0;
        for (int i = in.firstVertex; i <= in.lastVertex; i++) {
          rawIndices[idx++] = i;
        }
        partitionRawIndices();
      }
      endTex();
      tessellateEdges();
    }

    void tessellateTriangles(int[] indices) {
      beginTex();
      int nInVert = in.lastVertex - in.firstVertex + 1;
      if (fill && 3 <= nInVert) {
        int nInInd = indices.length;
        setRawSize(nInInd);
        PApplet.arrayCopy(indices, rawIndices, nInInd);
        partitionRawIndices();        
      }
      endTex();
      tessellateEdges();
    }
    
    void tessellateTriangleFan() {
      beginTex();
      int nInVert = in.lastVertex - in.firstVertex + 1;
      if (fill && 3 <= nInVert) {
        int nInInd = 3 * (nInVert - 2);
        setRawSize(nInInd);
        int idx = 0;
        for (int i = in.firstVertex + 1; i < in.lastVertex; i++) {
          rawIndices[idx++] = in.firstVertex;
          rawIndices[idx++] = i;
          rawIndices[idx++] = i + 1;
        }
        partitionRawIndices();
      }
      endTex();
      tessellateEdges();
    }

    void tessellateTriangleStrip() {
      beginTex();
      int nInVert = in.lastVertex - in.firstVertex + 1;
      if (fill && 3 <= nInVert) {
        int nInInd = 3 * (nInVert - 2);
        setRawSize(nInInd);
        int idx = 0;
        for (int i = in.firstVertex + 1; i < in.lastVertex; i++) {
          rawIndices[idx++] = i;
          if (i % 2 == 0) {
            rawIndices[idx++] = i - 1;
            rawIndices[idx++] = i + 1;
          } else {
            rawIndices[idx++] = i + 1;
            rawIndices[idx++] = i - 1;
          }
        }
        partitionRawIndices();
      }
      endTex();
      tessellateEdges();
    }

    void tessellateQuads() {
      beginTex();
      int nInVert = in.lastVertex - in.firstVertex + 1;
      if (fill && 4 <= nInVert) {
        int quadCount = nInVert / 4;
        int nInInd = 6 * quadCount;
        setRawSize(nInInd);        
        int idx = 0;
        for (int qd = 0; qd < quadCount; qd++) {
          int i0 = in.firstVertex + 4 * qd + 0;
          int i1 = in.firstVertex + 4 * qd + 1;
          int i2 = in.firstVertex + 4 * qd + 2;
          int i3 = in.firstVertex + 4 * qd + 3;

          rawIndices[idx++] = i0;
          rawIndices[idx++] = i1;
          rawIndices[idx++] = i3;

          rawIndices[idx++] = i1;
          rawIndices[idx++] = i2;
          rawIndices[idx++] = i3;
        }
        partitionRawIndices();
      }
      endTex();
      tessellateEdges();
    }

    void tessellateQuadStrip() {
      beginTex();
      int nInVert = in.lastVertex - in.firstVertex + 1;
      if (fill && 4 <= nInVert) {
        int quadCount = nInVert / 2 - 1;
        int nInInd = 6 * quadCount;
        setRawSize(nInInd); 
        int idx = 0;
        for (int qd = 1; qd < nInVert / 2; qd++) {
          int i0 = in.firstVertex + 2 * (qd - 1);
          int i1 = in.firstVertex + 2 * (qd - 1) + 1;
          int i2 = in.firstVertex + 2 * qd + 1;
          int i3 = in.firstVertex + 2 * qd;

          rawIndices[idx++] = i0;
          rawIndices[idx++] = i1;
          rawIndices[idx++] = i3;

          rawIndices[idx++] = i1;
          rawIndices[idx++] = i2;
          rawIndices[idx++] = i3;
        }
        partitionRawIndices();
      }
      endTex();
      tessellateEdges();
    }

    // Uses the raw indices to partition the geometry into contiguous 
    // index groups when the vertex indices become too large. The basic
    // idea of this algorithm is to scan through the array of raw indices
    // in groups of three vertices at the time (since we are always dealing
    // with triangles) and create a new offset in the index cache once the
    // index values go above the MAX_VERTEX_INDEX constant. The tricky part
    // is that triangles in the new group might refer to vertices in a 
    // previous group. Since the index groups are by definition disjoint,
    // these vertices need to be duplicated at the end of the corresponding
    // region in the vertex array.
    void partitionRawIndices() {
      tess.polyIndexCheck(rawSize);
      int offset = tess.firstPolyIndex; 
      
      int inInd0 = 0, inInd1 = 0;
      int inMaxVert0 = in.firstVertex, inMaxVert1 = in.firstVertex;
      int inMaxRel = 0;
      
      Set<Integer> inDupSet = null;
      IndexCache cache = tess.polyIndexCache;
      // In retained mode, each shape has with its own cache item, since
      // they should always be available to be rendererd individually, even
      // if contained in a larger hierarchy.
      int index = in.renderMode == RETAINED ? cache.addNew() : cache.getLast();
      firstPolyIndexCache = index;
      
      int trCount = rawSize / 3;        
      for (int tr = 0; tr < trCount; tr++) {
        if (index == -1) index = cache.addNew();
        
        int i0 = rawIndices[3 * tr + 0];
        int i1 = rawIndices[3 * tr + 1];
        int i2 = rawIndices[3 * tr + 2];

        // Vertex indices relative to the last copy position.
        int ii0 = i0 - inMaxVert0;
        int ii1 = i1 - inMaxVert0;
        int ii2 = i2 - inMaxVert0;
        
        // Vertex indices relative to the current group.
        int count = cache.vertexCount[index];
        int ri0, ri1, ri2;
        if (ii0 < 0) {
          if (inDupSet == null) inDupSet = new HashSet<Integer>();
          inDupSet.add(ii0);
          ri0 = ii0;
        } else ri0 = count + ii0;
        if (ii1 < 0) {
          if (inDupSet == null) inDupSet = new HashSet<Integer>();
          inDupSet.add(ii1);
          ri1 = ii1;
        } else ri1 = count + ii1;
        if (ii2 < 0) {
          if (inDupSet == null) inDupSet = new HashSet<Integer>();          
          inDupSet.add(ii2);
          ri2 = ii2;
        } else ri2 = count + ii2;
        
        tess.polyIndices[offset + 3 * tr + 0] = (short) ri0;
        tess.polyIndices[offset + 3 * tr + 1] = (short) ri1;
        tess.polyIndices[offset + 3 * tr + 2] = (short) ri2;   
        
        inInd1 = 3 * tr + 2;
        inMaxVert1 = PApplet.max(i0, i1, i2);          
        
        inMaxRel = PApplet.max(inMaxRel, PApplet.max(ri0, ri1, ri2));
        int dup = inDupSet == null ? 0 : inDupSet.size();
        
        if ((PGL.MAX_VERTEX_INDEX1 - 3 <= inMaxRel + dup && inMaxRel + dup < PGL.MAX_VERTEX_INDEX1) ||
            (tr == trCount - 1)) {
          // The vertex indices of the current group are about to 
          // surpass the MAX_VERTEX_INDEX limit, or we are at the last triangle
          // so we need to wrap-up things anyways.
          
          // So, copy vertices in current region first
          tess.addPolyVertices(in, inMaxVert0, inMaxVert1);
          
          if (0 < dup) {
            // Adjusting the negative indices so they correspond to vertices added 
            // at the end of the block.
            ArrayList<Integer> inDupList = new ArrayList<Integer>(inDupSet);            
            Collections.sort(inDupList);
            for (int i = inInd0; i <= inInd1; i++) {
              int ri = tess.polyIndices[i];
              if (ri < 0) {
                tess.polyIndices[i] = (short) (inMaxRel + 1 + inDupList.indexOf(ri));
              }
            }
            
            // Copy duplicated vertices from previous regions last            
            for (int i = 0; i < inDupList.size(); i++) {
              int ri = inDupList.get(i);
              tess.addPolyVertex(in, ri + inMaxVert0);  
            }
          }
          
          // Increment counts:
          cache.incCounts(index, inInd1 - inInd0 + 1, inMaxVert1 - inMaxVert0 + 1 + dup);
          lastPolyIndexCache = index;
          index = -1;
          
          inMaxRel = 0;
          inMaxVert0 = inMaxVert1 + 1;
          inInd0 = inInd1 + 1;
          if (inDupSet != null) inDupSet.clear(); 
        }
      }
    }
        
    void setRawSize(int size) {      
      int size0 = rawIndices.length;
      if (size0 < size) {
        int size1 = expandArraySize(size0, size);
        expandRawIndices(size1);
      }
      rawSize = size;
    }

    void expandRawIndices(int n) {
      int temp[] = new int[n];
      PApplet.arrayCopy(rawIndices, 0, temp, 0, rawSize);
      rawIndices = temp;
    }
    
    void beginTex() {
      setFirstTexIndex(tess.polyIndexCount, tess.polyIndexCache.size - 1);
    }
    
    void endTex() {
      setLastTexIndex(tess.lastPolyIndex, tess.polyIndexCache.size - 1);      
    }
    
    void beginNoTex() {
      prevTexImage = newTexImage; 
      newTexImage = null;
      setFirstTexIndex(tess.polyIndexCount, tess.polyIndexCache.size - 1);      
    }
    
    void endNoTex() {
      setLastTexIndex(tess.lastPolyIndex, tess.polyIndexCache.size - 1);
    }    
    
    void updateTex() {
      beginTex();
      endTex();
    }    
    
    void setFirstTexIndex(int firstIndex, int firstCache) {
      if (texCache != null) {
        firstTexIndex = firstIndex;
        firstTexCache = PApplet.max(0, firstCache);
      }
    }

    void setLastTexIndex(int lastIndex, int lastCache) {
      if (texCache != null) {
        if (prevTexImage != newTexImage || texCache.size == 0) {
          texCache.addTexture(newTexImage, firstTexIndex, firstTexCache, lastIndex, lastCache);
        } else {
          texCache.setLastIndex(lastIndex, lastCache);
        }
      }
    }     
    
    // -----------------------------------------------------------------
    //
    // Polygon tessellation    
    
    void tessellatePolygon(boolean solid, boolean closed, boolean calcNormals) {
      beginTex();
      
      int nInVert = in.lastVertex - in.firstVertex + 1;

      if (fill && 3 <= nInVert) {
        firstPolyIndexCache = -1;
        
        callback.init(in.renderMode == RETAINED, false, calcNormals);
        
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
          boolean breakPt = in.breaks[i];
          if (breakPt) {
            gluTess.endContour();
            gluTess.beginContour();
          }

          // Separting colors into individual rgba components for interpolation.
          int fa = (in.colors[i] >> 24) & 0xFF;
          int fr = (in.colors[i] >> 16) & 0xFF;
          int fg = (in.colors[i] >>  8) & 0xFF;
          int fb = (in.colors[i] >>  0) & 0xFF;

          int aa = (in.ambient[i] >> 24) & 0xFF;
          int ar = (in.ambient[i] >> 16) & 0xFF;
          int ag = (in.ambient[i] >>  8) & 0xFF;
          int ab = (in.ambient[i] >>  0) & 0xFF;

          int sa = (in.specular[i] >> 24) & 0xFF;
          int sr = (in.specular[i] >> 16) & 0xFF;
          int sg = (in.specular[i] >>  8) & 0xFF;
          int sb = (in.specular[i] >>  0) & 0xFF;

          int ea = (in.emissive[i] >> 24) & 0xFF;
          int er = (in.emissive[i] >> 16) & 0xFF;
          int eg = (in.emissive[i] >>  8) & 0xFF;
          int eb = (in.emissive[i] >>  0) & 0xFF;

          // Vertex data includes coordinates, colors, normals, texture coordinates, and material properties.
          double[] vertex = new double[] { in.vertices [3 * i + 0], in.vertices [3 * i + 1], in.vertices[3 * i + 2],
                                           fa, fr, fg, fb,
                                           in.normals  [3 * i + 0], in.normals  [3 * i + 1], in.normals [3 * i + 2],
                                           in.texcoords[2 * i + 0], in.texcoords[2 * i + 1],
                                           aa, ar, ag, ab, sa, sr, sg, sb, ea, er, eg, eb, in.shininess[i]};

          gluTess.addVertex(vertex);
        }
        gluTess.endContour();

        gluTess.endPolygon();
      }
      endTex();
      
      tessellateEdges();
    }    
    
    // Tessellates the path given as parameter. This will work only in 2D.
    // Based on the opengl stroke hack described here: 
    // http://wiki.processing.org/w/Stroke_attributes_in_OpenGL
    public void tessellateLinePath(LinePath path) {  
      callback.init(in.renderMode == RETAINED, true, false);
      
      int cap = strokeCap == ROUND ? LinePath.CAP_ROUND :
                strokeCap == PROJECT ? LinePath.CAP_SQUARE :
                LinePath.CAP_BUTT;
      int join = strokeJoin == ROUND ? LinePath.JOIN_ROUND :
                 strokeJoin == BEVEL ? LinePath.JOIN_BEVEL :
                 LinePath.JOIN_MITER;        
      
      // Make the outline of the stroke from the path
      LinePath strokedPath = LinePath.createStrokedPath(path, strokeWeight, cap, join);
      
      gluTess.beginPolygon();
      
      double[] vertex;
      float[] coords = new float[6];
      
      LinePath.PathIterator iter = strokedPath.getPathIterator();
      int rule = iter.getWindingRule();
      switch(rule) {
      case LinePath.WIND_EVEN_ODD:
        gluTess.setWindingRule(PGL.GLU_TESS_WINDING_ODD);
        break;
      case LinePath.WIND_NON_ZERO:
        gluTess.setWindingRule(PGL.GLU_TESS_WINDING_NONZERO);
        break;
      }
      
      while (!iter.isDone()) {        
        float sr = 0; 
        float sg = 0; 
        float sb = 0;
        float sa = 0;
        
        switch (iter.currentSegment(coords)) {
   
        case LinePath.SEG_MOVETO:
          gluTess.beginContour();
   
        case LinePath.SEG_LINETO:
          sa = (strokeColor >> 24) & 0xFF;
          sr = (strokeColor >> 16) & 0xFF;
          sg = (strokeColor >>  8) & 0xFF;
          sb = (strokeColor >>  0) & 0xFF;
          
          // Vertex data includes coordinates, colors, normals, texture coordinates, and material properties.
          vertex = new double[] { coords[0], coords[1], 0,
                                  sa, sr, sg, sb,
                                  0, 0, 1,
                                  0, 0,
                                  0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};          
          
          gluTess.addVertex(vertex);

          break;
        case LinePath.SEG_CLOSE:
          gluTess.endContour();
          break;
        }
        iter.next();
      }
      gluTess.endPolygon();     
    }
    
    /////////////////////////////////////////
    
    // Interenting notes about using the GLU tessellator to render thick polylines:
    // http://stackoverflow.com/questions/687173/how-do-i-render-thick-2d-lines-as-polygons
    //
    // "...Since I disliked the tesselator API I lifted the tesselation code from the free 
    //  SGI OpenGL reference implementation, rewrote the entire front-end and added memory 
    //  pools to get the number of allocations down. It took two days to do this, but it was 
    //  well worth it (like factor five performance improvement)..."
    //
    // This C implementation of GLU could be useful:    
    // http://code.google.com/p/glues/
    // to eventually come up with an optimized GLU tessellator in native code.
    protected class TessellatorCallback implements PGL.TessellatorCallback {
      boolean calcNormals;
      boolean strokeTess;
      IndexCache cache;
      int cacheIndex;
      int vertFirst;
      int vertCount;
      int primitive;

      public void init(boolean addCache, boolean strokeTess, boolean calcNorm) {
        this.strokeTess = strokeTess;
        this.calcNormals = calcNorm;
        
        cache = tess.polyIndexCache;
        if (addCache) {
          cache.addNew();
        }
      }
      
      public void begin(int type) {
        cacheIndex = cache.getLast();
        if (firstPolyIndexCache == -1) {
          firstPolyIndexCache = cacheIndex;
        }
        if (strokeTess && firstLineIndexCache == -1) {
          firstLineIndexCache = cacheIndex;
        }
        
        vertFirst = cache.vertexCount[cacheIndex];        
        vertCount = 0;

        switch (type) {
        case PGL.GL_TRIANGLE_FAN:
          primitive = TRIANGLE_FAN;
          break;
        case PGL.GL_TRIANGLE_STRIP:
          primitive = TRIANGLE_STRIP;
          break;
        case PGL.GL_TRIANGLES:
          primitive = TRIANGLES;
          break;
        }
      }

      public void end() {
        if (PGL.MAX_VERTEX_INDEX1 <= vertFirst + vertCount) {
          // We need a new index block for the new batch of
          // vertices resulting from this primitive. tessCount can
          // be safely assumed here to be less or equal than
          // MAX_VERTEX_INDEX1 because the condition was checked
          // every time a new vertex was emitted (see vertex() below).
          //tessBlock = tess.addFillIndexBlock(tessBlock);          
          cacheIndex = cache.addNew();          
          vertFirst = 0;
        }
                        
        int indCount = 0;         
        switch (primitive) {
        case TRIANGLE_FAN:
          indCount = 3 * (vertCount - 2);
          for (int i = 1; i < vertCount - 1; i++) {
            addIndex(0);
            addIndex(i);
            addIndex(i + 1);
            if (calcNormals) calcTriNormal(0, i, i + 1);
          }
          break;
        case TRIANGLE_STRIP:
          indCount = 3 * (vertCount - 2);          
          for (int i = 1; i < vertCount - 1; i++) {            
            if (i % 2 == 0) {
              addIndex(i + 1);
              addIndex(i);
              addIndex(i - 1);              
              if (calcNormals) calcTriNormal(i + 1, i, i - 1);
            } else {
              addIndex(i - 1);
              addIndex(i);
              addIndex(i + 1);              
              if (calcNormals) calcTriNormal(i - 1, i, i + 1);
            }
          }
          break;
        case TRIANGLES:
          indCount = vertCount;
          for (int i = 0; i < vertCount; i++) {
            addIndex(i);
          }
          if (calcNormals) {
            for (int tr = 0; tr < vertCount / 3; tr++) {
              int i0 = 3 * tr + 0;
              int i1 = 3 * tr + 1;
              int i2 = 3 * tr + 2;
              calcTriNormal(i0, i1, i2);
            }
          }
          break;
        }
        
        cache.incCounts(cacheIndex, indCount, vertCount);        
        lastPolyIndexCache = cacheIndex;
        if (strokeTess) {
          lastLineIndexCache = cacheIndex;
        }
      }

      protected void addIndex(int tessIdx) {
        tess.polyIndexCheck();
        tess.polyIndices[tess.polyIndexCount - 1] = (short) (vertFirst + tessIdx);
      }

      protected void calcTriNormal(int tessIdx0, int tessIdx1, int tessIdx2) {
        tess.calcPolyNormal(vertFirst + tessIdx0, vertFirst + tessIdx1, vertFirst + tessIdx2);
      }

      public void vertex(Object data) {
        if (data instanceof double[]) {
          double[] d = (double[]) data;
          int l = d.length;
          if (l < 25) {
            throw new RuntimeException("TessCallback vertex() data is not of length 25");
          }

          if (vertCount < PGL.MAX_VERTEX_INDEX1) {
            // Combining individual rgba components back into int color values
            int fcolor = ((int) d[ 3] << 24) | ((int) d[ 4] << 16) | ((int) d[ 5] << 8) | (int) d[ 6];
            int acolor = ((int) d[12] << 24) | ((int) d[13] << 16) | ((int) d[14] << 8) | (int) d[15];
            int scolor = ((int) d[16] << 24) | ((int) d[17] << 16) | ((int) d[18] << 8) | (int) d[19];
            int ecolor = ((int) d[20] << 24) | ((int) d[21] << 16) | ((int) d[22] << 8) | (int) d[23];
            
            tess.addPolyVertex((float) d[ 0],  (float) d[ 1], (float) d[ 2],
                               fcolor,
                               (float) d[ 7],  (float) d[ 8], (float) d[ 9],
                               (float) d[10], (float) d[11],
                               acolor, scolor, ecolor,
                               (float) d[24]);
            
            vertCount++;
          } else {
            throw new RuntimeException("P3D: the tessellator is generating too many vertices, reduce complexity of shape.");
          }

        } else {
          throw new RuntimeException("TessCallback vertex() data not understood");
        }
      }

      public void error(int errnum) {
        String estring = pgl.gluErrorString(errnum);
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
        double[] vertex = new double[25 + 8];
        vertex[0] = coords[0];
        vertex[1] = coords[1];
        vertex[2] = coords[2];

        // Calculating the rest of the vertex parameters (color,
        // normal, texcoords) as the linear combination of the
        // combined vertices.
        for (int i = 3; i < 25; i++) {
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