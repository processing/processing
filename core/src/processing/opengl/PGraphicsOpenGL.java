/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2004-13 Ben Fry and Casey Reas

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

import processing.core.*;

import java.io.IOException;
import java.net.URL;
import java.nio.*;
import java.util.*;

/**
 * OpenGL renderer.
 *
 */
public class PGraphicsOpenGL extends PGraphics {
  /** Interface between Processing and OpenGL */
  public static PGL pgl;

  /** The main PApplet renderer. */
  protected static PGraphicsOpenGL pgPrimary = null;

  /** The renderer currently in use. */
  protected static PGraphicsOpenGL pgCurrent = null;

  /** Font cache for texture objects. */
  protected WeakHashMap<PFont, FontTexture> fontMap =
    new WeakHashMap<PFont, FontTexture>();

  // ........................................................

  static final String OPENGL_THREAD_ERROR =
    "Cannot run the OpenGL renderer outside the main thread, change your code" +
    "\nso the drawing calls are all inside the main thread, " +
    "\nor use the default renderer instead.";
  static final String BLEND_DRIVER_ERROR =
    "blendMode(%1$s) is not supported by this hardware (or driver)";
  static final String BLEND_RENDERER_ERROR =
    "blendMode(%1$s) is not supported by this renderer";
  static final String ALREADY_DRAWING_ERROR =
    "Already called beginDraw()";
  static final String NO_BEGIN_DRAW_ERROR =
  "Cannot call endDraw() before beginDraw()";
  static final String NESTED_DRAW_ERROR =
    "Already called drawing on another PGraphicsOpenGL object";
  static final String ALREADY_BEGAN_CONTOUR_ERROR =
    "Already called beginContour()";
  static final String NO_BEGIN_CONTOUR_ERROR =
    "Need to call beginContour() first";
  static final String UNSUPPORTED_SMOOTH_LEVEL_ERROR =
    "Smooth level %1$s is not available. Using %2$s instead";
  static final String UNSUPPORTED_SMOOTH_ERROR =
    "Smooth is not supported by this hardware (or driver)";
  static final String TOO_MANY_SMOOTH_CALLS_ERROR =
    "The smooth/noSmooth functions are being called too often.\n" +
    "This results in screen flickering, so they will be disabled\n" +
    "for the rest of the sketch's execution";
  static final String UNSUPPORTED_SHAPE_FORMAT_ERROR =
    "Unsupported shape format";
  static final String INVALID_FILTER_SHADER_ERROR =
    "Your shader needs to be of TEXTURE type to be used as a filter";
  static final String INCONSISTENT_SHADER_TYPES =
    "The vertex and fragment shaders have different types";
  static final String WRONG_SHADER_TYPE_ERROR =
    "shader() called with a wrong shader";
  static final String SHADER_NEED_LIGHT_ATTRIBS =
    "The provided shader needs light attributes (ambient, diffuse, etc.), but " +
    "the current scene is unlit, so the default shader will be used instead";
  static final String MISSING_FRAGMENT_SHADER =
    "The fragment shader is missing, cannot create shader object";
  static final String MISSING_VERTEX_SHADER =
    "The vertex shader is missing, cannot create shader object";
  static final String UNKNOWN_SHADER_KIND_ERROR =
    "Unknown shader kind";
  static final String NO_TEXLIGHT_SHADER_ERROR =
    "Your shader needs to be of TEXLIGHT type " +
    "to render this geometry properly, using default shader instead.";
  static final String NO_LIGHT_SHADER_ERROR =
    "Your shader needs to be of LIGHT type " +
    "to render this geometry properly, using default shader instead.";
  static final String NO_TEXTURE_SHADER_ERROR =
    "Your shader needs to be of TEXTURE type " +
    "to render this geometry properly, using default shader instead.";
  static final String NO_COLOR_SHADER_ERROR =
    "Your shader needs to be of COLOR type " +
    "to render this geometry properly, using default shader instead.";
  static final String TOO_LONG_STROKE_PATH_ERROR =
    "Stroke path is too long, some bevel triangles won't be added";
  static final String TESSELLATION_ERROR =
    "Tessellation Error: %1$s";

  // ........................................................

  // Basic rendering parameters:

  /** Whether the PGraphics object is ready to render or not. */
  public boolean initialized;

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

  public int glPolyVertex;
  public int glPolyColor;
  public int glPolyNormal;
  public int glPolyTexcoord;
  public int glPolyAmbient;
  public int glPolySpecular;
  public int glPolyEmissive;
  public int glPolyShininess;
  public int glPolyIndex;
  protected boolean polyBuffersCreated = false;
  protected int polyBuffersContext;

  public int glLineVertex;
  public int glLineColor;
  public int glLineAttrib;
  public int glLineIndex;
  protected boolean lineBuffersCreated = false;
  protected int lineBuffersContext;

  public int glPointVertex;
  public int glPointColor;
  public int glPointAttrib;
  public int glPointIndex;
  protected boolean pointBuffersCreated = false;
  protected int pointBuffersContext;

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
  static public boolean anisoSamplingSupported;
  static public boolean blendEqSupported;

  /** Some hardware limits */
  static public int maxTextureSize;
  static public int maxSamples;
  static public float maxPointSize;
  static public float maxLineWidth;
  static public float maxAnisoAmount;
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

  static protected HashMap<GLResource, Boolean> glTextureObjects =
    new HashMap<GLResource, Boolean>();
  static protected HashMap<GLResource, Boolean> glVertexBuffers =
    new HashMap<GLResource, Boolean>();
  static protected HashMap<GLResource, Boolean> glFrameBuffers =
    new HashMap<GLResource, Boolean>();
  static protected HashMap<GLResource, Boolean> glRenderBuffers =
    new HashMap<GLResource, Boolean>();
  static protected HashMap<GLResource, Boolean> glslPrograms =
    new HashMap<GLResource, Boolean>();
  static protected HashMap<GLResource, Boolean> glslVertexShaders =
    new HashMap<GLResource, Boolean>();
  static protected HashMap<GLResource, Boolean> glslFragmentShaders =
    new HashMap<GLResource, Boolean>();

  // ........................................................

  // Shaders

  static protected String pointShaderAttrRegexp =
    "attribute *vec2 *offset";
  static protected String lineShaderAttrRegexp =
    "attribute *vec4 *direction";
  static protected String pointShaderDefRegexp =
    "#define *PROCESSING_POINT_SHADER";
  static protected String lineShaderDefRegexp =
    "#define *PROCESSING_LINE_SHADER";
  static protected String colorShaderDefRegexp =
    "#define *PROCESSING_COLOR_SHADER";
  static protected String lightShaderDefRegexp =
    "#define *PROCESSING_LIGHT_SHADER";
  static protected String texShaderDefRegexp =
    "#define *PROCESSING_TEXTURE_SHADER";
  static protected String texlightShaderDefRegexp =
    "#define *PROCESSING_TEXLIGHT_SHADER";
  static protected String polyShaderDefRegexp =
    "#define *PROCESSING_POLYGON_SHADER";
  static protected String triShaderAttrRegexp =
    "#define *PROCESSING_TRIANGLES_SHADER";
  static protected String quadShaderAttrRegexp =
    "#define *PROCESSING_QUADS_SHADER";

  static protected URL defColorShaderVertURL =
    PGraphicsOpenGL.class.getResource("ColorVert.glsl");
  static protected URL defTextureShaderVertURL =
    PGraphicsOpenGL.class.getResource("TextureVert.glsl");
  static protected URL defLightShaderVertURL =
    PGraphicsOpenGL.class.getResource("LightVert.glsl");
  static protected URL defTexlightShaderVertURL =
    PGraphicsOpenGL.class.getResource("TexlightVert.glsl");
  static protected URL defColorShaderFragURL =
    PGraphicsOpenGL.class.getResource("ColorFrag.glsl");
  static protected URL defTextureShaderFragURL =
    PGraphicsOpenGL.class.getResource("TextureFrag.glsl");
  static protected URL defLineShaderVertURL =
    PGraphicsOpenGL.class.getResource("LineVert.glsl");
  static protected URL defLineShaderFragURL =
    PGraphicsOpenGL.class.getResource("LineFrag.glsl");
  static protected URL defPointShaderVertURL =
    PGraphicsOpenGL.class.getResource("PointVert.glsl");
  static protected URL defPointShaderFragURL =
    PGraphicsOpenGL.class.getResource("PointFrag.glsl");

  static protected PShader defColorShader;
  static protected PShader defTextureShader;
  static protected PShader defLightShader;
  static protected PShader defTexlightShader;
  static protected PShader defLineShader;
  static protected PShader defPointShader;

  static protected URL maskShaderFragURL =
    PGraphicsOpenGL.class.getResource("MaskFrag.glsl");
  static protected PShader maskShader;

  protected PShader polyShader;
  protected PShader lineShader;
  protected PShader pointShader;

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

  // Useful to have around.
  static protected PMatrix3D identity = new PMatrix3D();

  protected boolean matricesAllocated = false;

  /**
   * Marks when changes to the size have occurred, so that the camera
   * will be reset in beginDraw().
   */
  protected boolean sized;

  static protected final int MATRIX_STACK_DEPTH = 32;

  protected int modelviewStackDepth;
  protected int projectionStackDepth;

  /** Modelview matrix stack **/
  protected float[][] modelviewStack = new float[MATRIX_STACK_DEPTH][16];

  /** Inverse modelview matrix stack **/
  protected float[][] modelviewInvStack = new float[MATRIX_STACK_DEPTH][16];

  /** Camera matrix stack **/
  protected float[][] cameraStack = new float[MATRIX_STACK_DEPTH][16];

  /** Inverse camera matrix stack **/
  protected float[][] cameraInvStack = new float[MATRIX_STACK_DEPTH][16];

  /** Projection matrix stack **/
  protected float[][] projectionStack = new float[MATRIX_STACK_DEPTH][16];

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

  // Texturing:

  protected int textureWrap     = CLAMP;
  protected int textureSampling = Texture.TRILINEAR;

  // ........................................................

  // Clipping

  protected boolean clip = false;

  /** Clipping rectangle. */
  protected int[] clipRect = {0, 0, 0, 0};


  // ........................................................

  // Text:

  /** Font texture of currently selected font. */
  FontTexture textTex;

  // .......................................................

  // Framebuffer stack:

  static protected final int FB_STACK_DEPTH = 16;

  static protected int fbStackDepth;
  static protected FrameBuffer[] fbStack = new FrameBuffer[FB_STACK_DEPTH];
  static protected FrameBuffer drawFramebuffer;
  static protected FrameBuffer readFramebuffer;
  static protected FrameBuffer currentFramebuffer;

  // .......................................................

  // Offscreen rendering:

  protected FrameBuffer offscreenFramebuffer;
  protected FrameBuffer multisampleFramebuffer;
  protected boolean offscreenMultisample;

  protected boolean pixOpChangedFB;

  // ........................................................

  // Screen surface:

  /** Texture containing the current frame */
  protected Texture texture;

  /** Texture containing the previous frame */
  protected Texture ptexture;

  /** IntBuffer wrapping the pixels array. */
  protected IntBuffer pixelBuffer;

  /** Array to store pixels in OpenGL format. */
  protected int[] nativePixels;

  /** IntBuffer wrapping the native pixels array. */
  protected IntBuffer nativePixelBuffer;

  /** texture used to apply a filter on the screen image. */
  protected Texture filterTexture;

  /** PImage that wraps filterTexture. */
  protected PImage filterImage;

  /** Flag to indicate if the user is manipulating the
   * pixels array through the set()/get() methods */
  protected boolean setgetPixels;

  // ........................................................

  // Utility variables:

  /** True if we are inside a beginDraw()/endDraw() block. */
  protected boolean drawing = false;

  /** Used to indicate an OpenGL surface recreation */
  protected boolean restoreSurface = false;

  /** Used to detect continuous use of the smooth/noSmooth functions */
  protected boolean smoothDisabled = false;
  protected int smoothCallCount = 0;
  protected int lastSmoothCall = -10;

  /** Used to avoid flushing the geometry when blendMode() is called with the
   * same blend mode as the last */
  protected int lastBlendMode = -1;

  /** Type of pixels operation. */
  static protected final int OP_NONE  = 0;
  static protected final int OP_READ  = 1;
  static protected final int OP_WRITE = 2;
  protected int pixelsOp = OP_NONE;

  /** Viewport dimensions. */
  protected IntBuffer viewport;

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
  static protected final int EDGE_CLOSE  = -1;

  /** Used in round point and ellipse tessellation. The
   * number of subdivisions per round point or ellipse is
   * calculated with the following formula:
   * n = min(M, max(N, (TWO_PI * size / F)))
   * where size is a measure of the dimensions of the circle
   * when projected on screen coordinates. F just sets the
   * minimum number of subdivisions, while a smaller F
   * would allow to have more detailed circles.
   * N = MIN_POINT_ACCURACY
   * M = MAX_POINT_ACCURACY
   * F = POINT_ACCURACY_FACTOR
   */
  final static protected int   MIN_POINT_ACCURACY    = 20;
  final static protected int   MAX_POINT_ACCURACY    = 200;
  final static protected float POINT_ACCURACY_FACTOR = 10.0f;

  /** Used in quad point tessellation. */
  final static protected float[][] QUAD_POINT_SIGNS =
    { {-1, +1}, {-1, -1}, {+1, -1}, {+1, +1} };

  /** To get data from OpenGL. */
  static protected IntBuffer intBuffer;
  static protected FloatBuffer floatBuffer;

  //////////////////////////////////////////////////////////////

  // INIT/ALLOCATE/FINISH


  public PGraphicsOpenGL() {
    if (pgl == null) {
      pgl = createPGL(this);
    }

    if (tessellator == null) {
      tessellator = new Tessellator();
    }

    if (intBuffer == null) {
      intBuffer = PGL.allocateIntBuffer(2);
      floatBuffer = PGL.allocateFloatBuffer(2);
    }

    viewport = PGL.allocateIntBuffer(4);

    inGeo = newInGeometry(this, IMMEDIATE);
    tessGeo = newTessGeometry(this, IMMEDIATE);
    texCache = newTexCache();

    initialized = false;
  }


  @Override
  public void setPrimary(boolean primary) {
    super.setPrimary(primary);
    format = ARGB;
  }


  //public void setPath(String path)  // PGraphics


  //public void setAntiAlias(int samples)  // PGraphics


  @Override
  public void setFrameRate(float frameRate) {
    pgl.setFps(frameRate);
  }


  @Override
  public void setSize(int iwidth, int iheight) {
    width = iwidth;
    height = iheight;

    sized = true;
  }


  /**
   * Called by resize(), this handles creating the actual GLCanvas the
   * first time around, or simply resizing it on subsequent calls.
   * There is no pixel array to allocate for an OpenGL canvas
   * because OpenGL's pixel buffer is all handled internally.
   */
  @Override
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


  @Override
  public void dispose() { // PGraphics
    super.dispose();

    // Swap buffers the end to make sure that no
    // garbage is shown on the screen, this particularly
    // affects non-interactive sketches on windows that
    // render only 1 frame, so no enough rendering
    // iterations have been conducted so far to properly
    // initialize all the buffers.
    pgl.swapBuffers();

    deletePolyBuffers();
    deleteLineBuffers();
    deletePointBuffers();

    deleteSurfaceTextures();
    if (primarySurface) {
      deleteDefaultShaders();
    } else {
      if (offscreenFramebuffer != null) {
        offscreenFramebuffer.dispose();
      }
      if (multisampleFramebuffer != null) {
        multisampleFramebuffer.dispose();
      }
    }

    deleteFinalizedGLResources();

    if (primarySurface) pgl.deleteSurface();
  }

//  @Override
  @Override
  protected void finalize() throws Throwable {
    try {
      deletePolyBuffers();
      deleteLineBuffers();
      deletePointBuffers();

      deleteSurfaceTextures();
      if (!primarySurface) {
        if (offscreenFramebuffer != null) {
          offscreenFramebuffer.dispose();
          offscreenFramebuffer = null;
        }
        if (multisampleFramebuffer != null) {
          multisampleFramebuffer.dispose();
          multisampleFramebuffer = null;
        }
      }
    } finally {
      super.finalize();
    }
  }

  protected void setFlushMode(int mode) {
    flushMode = mode;
  }


  //////////////////////////////////////////////////////////////


  protected void setFontTexture(PFont font, FontTexture fontTexture) {
    fontMap.put(font, fontTexture);
  }


  protected FontTexture getFontTexture(PFont font) {
    return fontMap.get(font);
  }


  protected void removeFontTexture(PFont font) {
    fontMap.remove(font);
  }


  //////////////////////////////////////////////////////////////

  // RESOURCE HANDLING


  protected static class GLResource {
    int id;
    int context;

    GLResource(int id, int context) {
      this.id = id;
      this.context = context;
    }

    @Override
    public boolean equals(Object obj) {
      GLResource other = (GLResource)obj;
      return other.id == id && other.context == context;
    }

    @Override
    public int hashCode() {
      int result = 17;
      result = 31 * result + id;
      result = 31 * result + context;
      return result;
    }
  }


  // Texture Objects -----------------------------------------------------------

  protected static int createTextureObject(int context) {
    deleteFinalizedTextureObjects();

    pgl.genTextures(1, intBuffer);
    int id = intBuffer.get(0);

    GLResource res = new GLResource(id, context);
    if (!glTextureObjects.containsKey(res)) {
      glTextureObjects.put(res, false);
    }

    return id;
  }

  protected static void deleteTextureObject(int id, int context) {
    GLResource res = new GLResource(id, context);
    if (glTextureObjects.containsKey(res)) {
      intBuffer.put(0, id);
      if (pgl.threadIsCurrent()) pgl.deleteTextures(1, intBuffer);
      glTextureObjects.remove(res);
    }
  }

  protected static void deleteAllTextureObjects() {
    for (GLResource res : glTextureObjects.keySet()) {
      intBuffer.put(0, res.id);
      if (pgl.threadIsCurrent()) pgl.deleteTextures(1, intBuffer);
    }
    glTextureObjects.clear();
  }

  // This is synchronized because it is called from the GC thread.
  synchronized protected static void finalizeTextureObject(int id, int context) {
    GLResource res = new GLResource(id, context);
    if (glTextureObjects.containsKey(res)) {
      glTextureObjects.put(res, true);
    }
  }

  protected static void deleteFinalizedTextureObjects() {
    Set<GLResource> finalized = new HashSet<GLResource>();

    for (GLResource res : glTextureObjects.keySet()) {
      if (glTextureObjects.get(res)) {
        finalized.add(res);
        intBuffer.put(0, res.id);
        if (pgl.threadIsCurrent()) pgl.deleteTextures(1, intBuffer);
      }
    }

    for (GLResource res : finalized) {
      glTextureObjects.remove(res);
    }
  }

  protected static void removeTextureObject(int id, int context) {
    GLResource res = new GLResource(id, context);
    if (glTextureObjects.containsKey(res)) {
      glTextureObjects.remove(res);
    }
  }

  // Vertex Buffer Objects -----------------------------------------------------

  protected static int createVertexBufferObject(int context) {
    deleteFinalizedVertexBufferObjects();

    pgl.genBuffers(1, intBuffer);
    int id = intBuffer.get(0);

    GLResource res = new GLResource(id, context);
    if (!glVertexBuffers.containsKey(res)) {
      glVertexBuffers.put(res, false);
    }

    return id;
  }

  protected static void deleteVertexBufferObject(int id, int context) {
    GLResource res = new GLResource(id, context);
    if (glVertexBuffers.containsKey(res)) {
      intBuffer.put(0, id);
      if (pgl.threadIsCurrent()) pgl.deleteBuffers(1, intBuffer);
      glVertexBuffers.remove(res);
    }
  }

  protected static void deleteAllVertexBufferObjects() {
    for (GLResource res : glVertexBuffers.keySet()) {
      intBuffer.put(0, res.id);
      if (pgl.threadIsCurrent()) pgl.deleteBuffers(1, intBuffer);
    }
    glVertexBuffers.clear();
  }

  // This is synchronized because it is called from the GC thread.
  synchronized static protected void finalizeVertexBufferObject(int id, int context) {
    GLResource res = new GLResource(id, context);
    if (glVertexBuffers.containsKey(res)) {
      glVertexBuffers.put(res, true);
    }
  }

  protected static void deleteFinalizedVertexBufferObjects() {
    Set<GLResource> finalized = new HashSet<GLResource>();

    for (GLResource res : glVertexBuffers.keySet()) {
      if (glVertexBuffers.get(res)) {
        finalized.add(res);
        intBuffer.put(0, res.id);
        if (pgl.threadIsCurrent()) pgl.deleteBuffers(1, intBuffer);
      }
    }

    for (GLResource res : finalized) {
      glVertexBuffers.remove(res);
    }
  }

  protected static void removeVertexBufferObject(int id, int context) {
    GLResource res = new GLResource(id, context);
    if (glVertexBuffers.containsKey(res)) {
      glVertexBuffers.remove(res);
    }
  }

  // FrameBuffer Objects -------------------------------------------------------

  protected static int createFrameBufferObject(int context) {
    deleteFinalizedFrameBufferObjects();

    pgl.genFramebuffers(1, intBuffer);
    int id = intBuffer.get(0);

    GLResource res = new GLResource(id, context);
    if (!glFrameBuffers.containsKey(res)) {
      glFrameBuffers.put(res, false);
    }

    return id;
  }

  protected static void deleteFrameBufferObject(int id, int context) {
    GLResource res = new GLResource(id, context);
    if (glFrameBuffers.containsKey(res)) {
      intBuffer.put(0, id);
      if (pgl.threadIsCurrent()) pgl.deleteFramebuffers(1, intBuffer);
      glFrameBuffers.remove(res);
    }
  }

  protected static void deleteAllFrameBufferObjects() {
    for (GLResource res : glFrameBuffers.keySet()) {
      intBuffer.put(0, res.id);
      if (pgl.threadIsCurrent()) pgl.deleteFramebuffers(1, intBuffer);
    }
    glFrameBuffers.clear();
  }

  // This is synchronized because it is called from the GC thread.
  synchronized static protected void finalizeFrameBufferObject(int id, int context) {
    GLResource res = new GLResource(id, context);
    if (glFrameBuffers.containsKey(res)) {
      glFrameBuffers.put(res, true);
    }
  }

  protected static void deleteFinalizedFrameBufferObjects() {
    Set<GLResource> finalized = new HashSet<GLResource>();

    for (GLResource res : glFrameBuffers.keySet()) {
      if (glFrameBuffers.get(res)) {
        finalized.add(res);
        intBuffer.put(0, res.id);
        if (pgl.threadIsCurrent()) {
          pgl.deleteFramebuffers(1, intBuffer);
        }
      }
    }

    for (GLResource res : finalized) {
      glFrameBuffers.remove(res);
    }
  }

  protected static void removeFrameBufferObject(int id, int context) {
    GLResource res = new GLResource(id, context);
    if (glFrameBuffers.containsKey(res)) {
      glFrameBuffers.remove(res);
    }
  }

  // RenderBuffer Objects ------------------------------------------------------

  protected static int createRenderBufferObject(int context) {
    deleteFinalizedRenderBufferObjects();

    pgl.genRenderbuffers(1, intBuffer);
    int id = intBuffer.get(0);

    GLResource res = new GLResource(id, context);
    if (!glRenderBuffers.containsKey(res)) {
      glRenderBuffers.put(res, false);
    }

    return id;
  }

  protected static void deleteRenderBufferObject(int id, int context) {
    GLResource res = new GLResource(id, context);
    if (glRenderBuffers.containsKey(res)) {
      intBuffer.put(0, id);
      if (pgl.threadIsCurrent()) pgl.deleteRenderbuffers(1, intBuffer);
      glRenderBuffers.remove(res);
    }
  }

  protected static void deleteAllRenderBufferObjects() {
    for (GLResource res : glRenderBuffers.keySet()) {
      intBuffer.put(0, res.id);
      if (pgl.threadIsCurrent()) pgl.deleteRenderbuffers(1, intBuffer);
    }
    glRenderBuffers.clear();
  }

  // This is synchronized because it is called from the GC thread.
  synchronized static protected void finalizeRenderBufferObject(int id, int context) {
    GLResource res = new GLResource(id, context);
    if (glRenderBuffers.containsKey(res)) {
      glRenderBuffers.put(res, true);
    }
  }

  protected static void deleteFinalizedRenderBufferObjects() {
    Set<GLResource> finalized = new HashSet<GLResource>();

    for (GLResource res : glRenderBuffers.keySet()) {
      if (glRenderBuffers.get(res)) {
        finalized.add(res);
        intBuffer.put(0, res.id);
        if (pgl.threadIsCurrent()) pgl.deleteRenderbuffers(1, intBuffer);
      }
    }

    for (GLResource res : finalized) {
      glRenderBuffers.remove(res);
    }
  }

  protected static void removeRenderBufferObject(int id, int context) {
    GLResource res = new GLResource(id, context);
    if (glRenderBuffers.containsKey(res)) {
      glRenderBuffers.remove(res);
    }
  }

  // GLSL Program Objects ------------------------------------------------------

  protected static int createGLSLProgramObject(int context) {
    deleteFinalizedGLSLProgramObjects();

    int id = pgl.createProgram();

    GLResource res = new GLResource(id, context);
    if (!glslPrograms.containsKey(res)) {
      glslPrograms.put(res, false);
    }

    return id;
  }

  protected static void deleteGLSLProgramObject(int id, int context) {
    GLResource res = new GLResource(id, context);
    if (glslPrograms.containsKey(res)) {
      if (pgl.threadIsCurrent()) pgl.deleteProgram(res.id);
      glslPrograms.remove(res);
    }
  }

  protected static void deleteAllGLSLProgramObjects() {
    for (GLResource res : glslPrograms.keySet()) {
      if (pgl.threadIsCurrent()) pgl.deleteProgram(res.id);
    }
    glslPrograms.clear();
  }

  // This is synchronized because it is called from the GC thread.
  synchronized static protected void finalizeGLSLProgramObject(int id, int context) {
    GLResource res = new GLResource(id, context);
    if (glslPrograms.containsKey(res)) {
      glslPrograms.put(res, true);
    }
  }

  protected static void deleteFinalizedGLSLProgramObjects() {
    Set<GLResource> finalized = new HashSet<GLResource>();

    for (GLResource res : glslPrograms.keySet()) {
      if (glslPrograms.get(res)) {
        finalized.add(res);
        if (pgl.threadIsCurrent()) pgl.deleteProgram(res.id);
      }
    }

    for (GLResource res : finalized) {
      glslPrograms.remove(res);
    }
  }

  protected static void removeGLSLProgramObject(int id, int context) {
    GLResource res = new GLResource(id, context);
    if (glslPrograms.containsKey(res)) {
      glslPrograms.remove(res);
    }
  }

  // GLSL Vertex Shader Objects ------------------------------------------------

  protected static int createGLSLVertShaderObject(int context) {
    deleteFinalizedGLSLVertShaderObjects();

    int id = pgl.createShader(PGL.VERTEX_SHADER);

    GLResource res = new GLResource(id, context);
    if (!glslVertexShaders.containsKey(res)) {
      glslVertexShaders.put(res, false);
    }

    return id;
  }

  protected static void deleteGLSLVertShaderObject(int id, int context) {
    GLResource res = new GLResource(id, context);
    if (glslVertexShaders.containsKey(res)) {
      if (pgl.threadIsCurrent()) pgl.deleteShader(res.id);
      glslVertexShaders.remove(res);
    }
  }

  protected static void deleteAllGLSLVertShaderObjects() {
    for (GLResource res : glslVertexShaders.keySet()) {
      if (pgl.threadIsCurrent()) pgl.deleteShader(res.id);
    }
    glslVertexShaders.clear();
  }

  // This is synchronized because it is called from the GC thread.
  synchronized static protected void finalizeGLSLVertShaderObject(int id,
                                                           int context) {
    GLResource res = new GLResource(id, context);
    if (glslVertexShaders.containsKey(res)) {
      glslVertexShaders.put(res, true);
    }
  }

  protected static void deleteFinalizedGLSLVertShaderObjects() {
    Set<GLResource> finalized = new HashSet<GLResource>();

    for (GLResource res : glslVertexShaders.keySet()) {
      if (glslVertexShaders.get(res)) {
        finalized.add(res);
        if (pgl.threadIsCurrent()) pgl.deleteShader(res.id);
      }
    }

    for (GLResource res : finalized) {
      glslVertexShaders.remove(res);
    }
  }

  protected static void removeGLSLVertShaderObject(int id, int context) {
    GLResource res = new GLResource(id, context);
    if (glslVertexShaders.containsKey(res)) {
      glslVertexShaders.remove(res);
    }
  }

  // GLSL Fragment Shader Objects ----------------------------------------------

  protected static int createGLSLFragShaderObject(int context) {
    deleteFinalizedGLSLFragShaderObjects();

    int id = pgl.createShader(PGL.FRAGMENT_SHADER);

    GLResource res = new GLResource(id, context);
    if (!glslFragmentShaders.containsKey(res)) {
      glslFragmentShaders.put(res, false);
    }

    return id;
  }

  protected static void deleteGLSLFragShaderObject(int id, int context) {
    GLResource res = new GLResource(id, context);
    if (glslFragmentShaders.containsKey(res)) {
      if (pgl.threadIsCurrent()) pgl.deleteShader(res.id);
      glslFragmentShaders.remove(res);
    }
  }

  protected static void deleteAllGLSLFragShaderObjects() {
    for (GLResource res : glslFragmentShaders.keySet()) {
      if (pgl.threadIsCurrent()) pgl.deleteShader(res.id);
    }
    glslFragmentShaders.clear();
  }

  // This is synchronized because it is called from the GC thread.
  synchronized static protected void finalizeGLSLFragShaderObject(int id,
                                                           int context) {
    GLResource res = new GLResource(id, context);
    if (glslFragmentShaders.containsKey(res)) {
      glslFragmentShaders.put(res, true);
    }
  }

  protected static void deleteFinalizedGLSLFragShaderObjects() {
    Set<GLResource> finalized = new HashSet<GLResource>();

    for (GLResource res : glslFragmentShaders.keySet()) {
      if (glslFragmentShaders.get(res)) {
        finalized.add(res);
        if (pgl.threadIsCurrent()) pgl.deleteShader(res.id);
      }
    }

    for (GLResource res : finalized) {
      glslFragmentShaders.remove(res);
    }
  }

  protected static void removeGLSLFragShaderObject(int id, int context) {
    GLResource res = new GLResource(id, context);
    if (glslFragmentShaders.containsKey(res)) {
      glslFragmentShaders.remove(res);
    }
  }

  // All OpenGL resources ------------------------------------------------------

  protected static void deleteFinalizedGLResources() {
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


  protected static void pushFramebuffer() {
    if (fbStackDepth == FB_STACK_DEPTH) {
      throw new RuntimeException("Too many pushFramebuffer calls");
    }
    fbStack[fbStackDepth] = currentFramebuffer;
    fbStackDepth++;
  }


  protected static void setFramebuffer(FrameBuffer fbo) {
    if (currentFramebuffer != fbo) {
      currentFramebuffer = fbo;
      currentFramebuffer.bind();
    }
  }


  protected static void popFramebuffer() {
    if (fbStackDepth == 0) {
      throw new RuntimeException("popFramebuffer call is unbalanced.");
    }
    fbStackDepth--;
    FrameBuffer fbo = fbStack[fbStackDepth];
    if (currentFramebuffer != fbo) {
      currentFramebuffer.finish(pgPrimary);
      currentFramebuffer = fbo;
      currentFramebuffer.bind();
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

      glPolyVertex = createVertexBufferObject(polyBuffersContext);
      pgl.bindBuffer(PGL.ARRAY_BUFFER, glPolyVertex);
      pgl.bufferData(PGL.ARRAY_BUFFER, 3 * sizef, null, PGL.STATIC_DRAW);

      glPolyColor = createVertexBufferObject(polyBuffersContext);
      pgl.bindBuffer(PGL.ARRAY_BUFFER, glPolyColor);
      pgl.bufferData(PGL.ARRAY_BUFFER, sizei, null, PGL.STATIC_DRAW);

      glPolyNormal = createVertexBufferObject(polyBuffersContext);
      pgl.bindBuffer(PGL.ARRAY_BUFFER, glPolyNormal);
      pgl.bufferData(PGL.ARRAY_BUFFER, 3 * sizef, null, PGL.STATIC_DRAW);

      glPolyTexcoord = createVertexBufferObject(polyBuffersContext);
      pgl.bindBuffer(PGL.ARRAY_BUFFER, glPolyTexcoord);
      pgl.bufferData(PGL.ARRAY_BUFFER, 2 * sizef, null, PGL.STATIC_DRAW);

      glPolyAmbient = createVertexBufferObject(polyBuffersContext);
      pgl.bindBuffer(PGL.ARRAY_BUFFER, glPolyAmbient);
      pgl.bufferData(PGL.ARRAY_BUFFER, sizei, null, PGL.STATIC_DRAW);

      glPolySpecular = createVertexBufferObject(polyBuffersContext);
      pgl.bindBuffer(PGL.ARRAY_BUFFER, glPolySpecular);
      pgl.bufferData(PGL.ARRAY_BUFFER, sizei, null, PGL.STATIC_DRAW);

      glPolyEmissive = createVertexBufferObject(polyBuffersContext);
      pgl.bindBuffer(PGL.ARRAY_BUFFER, glPolyEmissive);
      pgl.bufferData(PGL.ARRAY_BUFFER, sizei, null, PGL.STATIC_DRAW);

      glPolyShininess = createVertexBufferObject(polyBuffersContext);
      pgl.bindBuffer(PGL.ARRAY_BUFFER, glPolyShininess);
      pgl.bufferData(PGL.ARRAY_BUFFER, sizef, null, PGL.STATIC_DRAW);

      pgl.bindBuffer(PGL.ARRAY_BUFFER, 0);

      glPolyIndex = createVertexBufferObject(polyBuffersContext);
      pgl.bindBuffer(PGL.ELEMENT_ARRAY_BUFFER, glPolyIndex);
      pgl.bufferData(PGL.ELEMENT_ARRAY_BUFFER, sizex, null, PGL.STATIC_DRAW);

      pgl.bindBuffer(PGL.ELEMENT_ARRAY_BUFFER, 0);

      polyBuffersCreated = true;
    }
  }


  protected void updatePolyBuffers(boolean lit, boolean tex,
                                   boolean needNormals, boolean needTexCoords) {
    createPolyBuffers();

    int size = tessGeo.polyVertexCount;
    int sizef = size * PGL.SIZEOF_FLOAT;
    int sizei = size * PGL.SIZEOF_INT;

    tessGeo.updatePolyVerticesBuffer();
    pgl.bindBuffer(PGL.ARRAY_BUFFER, glPolyVertex);
    pgl.bufferData(PGL.ARRAY_BUFFER, 4 * sizef,
                   tessGeo.polyVerticesBuffer, PGL.STATIC_DRAW);

    tessGeo.updatePolyColorsBuffer();
    pgl.bindBuffer(PGL.ARRAY_BUFFER, glPolyColor);
    pgl.bufferData(PGL.ARRAY_BUFFER, sizei,
                   tessGeo.polyColorsBuffer, PGL.STATIC_DRAW);

    if (lit) {
      tessGeo.updatePolyAmbientBuffer();
      pgl.bindBuffer(PGL.ARRAY_BUFFER, glPolyAmbient);
      pgl.bufferData(PGL.ARRAY_BUFFER, sizei,
                     tessGeo.polyAmbientBuffer, PGL.STATIC_DRAW);

      tessGeo.updatePolySpecularBuffer();
      pgl.bindBuffer(PGL.ARRAY_BUFFER, glPolySpecular);
      pgl.bufferData(PGL.ARRAY_BUFFER, sizei,
                     tessGeo.polySpecularBuffer, PGL.STATIC_DRAW);

      tessGeo.updatePolyEmissiveBuffer();
      pgl.bindBuffer(PGL.ARRAY_BUFFER, glPolyEmissive);
      pgl.bufferData(PGL.ARRAY_BUFFER, sizei,
                     tessGeo.polyEmissiveBuffer, PGL.STATIC_DRAW);

      tessGeo.updatePolyShininessBuffer();
      pgl.bindBuffer(PGL.ARRAY_BUFFER, glPolyShininess);
      pgl.bufferData(PGL.ARRAY_BUFFER, sizef,
                     tessGeo.polyShininessBuffer, PGL.STATIC_DRAW);
    }
    if (lit || needNormals) {
      tessGeo.updatePolyNormalsBuffer();
      pgl.bindBuffer(PGL.ARRAY_BUFFER, glPolyNormal);
      pgl.bufferData(PGL.ARRAY_BUFFER, 3 * sizef,
                     tessGeo.polyNormalsBuffer, PGL.STATIC_DRAW);
    }

    if (tex || needTexCoords) {
      tessGeo.updatePolyTexCoordsBuffer();
      pgl.bindBuffer(PGL.ARRAY_BUFFER, glPolyTexcoord);
      pgl.bufferData(PGL.ARRAY_BUFFER, 2 * sizef,
                     tessGeo.polyTexCoordsBuffer, PGL.STATIC_DRAW);
    }

    tessGeo.updatePolyIndicesBuffer();
    pgl.bindBuffer(PGL.ELEMENT_ARRAY_BUFFER, glPolyIndex);
    pgl.bufferData(PGL.ELEMENT_ARRAY_BUFFER,
      tessGeo.polyIndexCount * PGL.SIZEOF_INDEX, tessGeo.polyIndicesBuffer,
      PGL.STATIC_DRAW);
  }


  protected void unbindPolyBuffers() {
    pgl.bindBuffer(PGL.ARRAY_BUFFER, 0);
    pgl.bindBuffer(PGL.ELEMENT_ARRAY_BUFFER, 0);
  }


  protected boolean polyBuffersContextIsOutdated() {
    return !pgl.contextIsCurrent(polyBuffersContext);
  }


  protected void deletePolyBuffers() {
    if (polyBuffersCreated) {
      deleteVertexBufferObject(glPolyVertex, polyBuffersContext);
      glPolyVertex = 0;

      deleteVertexBufferObject(glPolyColor, polyBuffersContext);
      glPolyColor = 0;

      deleteVertexBufferObject(glPolyNormal, polyBuffersContext);
      glPolyNormal = 0;

      deleteVertexBufferObject(glPolyTexcoord, polyBuffersContext);
      glPolyTexcoord = 0;

      deleteVertexBufferObject(glPolyAmbient, polyBuffersContext);
      glPolyAmbient = 0;

      deleteVertexBufferObject(glPolySpecular, polyBuffersContext);
      glPolySpecular = 0;

      deleteVertexBufferObject(glPolyEmissive, polyBuffersContext);
      glPolyEmissive = 0;

      deleteVertexBufferObject(glPolyShininess, polyBuffersContext);
      glPolyShininess = 0;

      deleteVertexBufferObject(glPolyIndex, polyBuffersContext);
      glPolyIndex = 0;

      polyBuffersCreated = false;
    }
  }


  protected void createLineBuffers() {
    if (!lineBuffersCreated || lineBufferContextIsOutdated()) {
      lineBuffersContext = pgl.getCurrentContext();

      int sizef = INIT_VERTEX_BUFFER_SIZE * PGL.SIZEOF_FLOAT;
      int sizei = INIT_VERTEX_BUFFER_SIZE * PGL.SIZEOF_INT;
      int sizex = INIT_INDEX_BUFFER_SIZE * PGL.SIZEOF_INDEX;

      glLineVertex = createVertexBufferObject(lineBuffersContext);

      pgl.bindBuffer(PGL.ARRAY_BUFFER, glLineVertex);
      pgl.bufferData(PGL.ARRAY_BUFFER, 3 * sizef, null, PGL.STATIC_DRAW);

      glLineColor = createVertexBufferObject(lineBuffersContext);
      pgl.bindBuffer(PGL.ARRAY_BUFFER, glLineColor);
      pgl.bufferData(PGL.ARRAY_BUFFER, sizei, null, PGL.STATIC_DRAW);

      glLineAttrib = createVertexBufferObject(lineBuffersContext);
      pgl.bindBuffer(PGL.ARRAY_BUFFER, glLineAttrib);
      pgl.bufferData(PGL.ARRAY_BUFFER, 4 * sizef, null, PGL.STATIC_DRAW);

      pgl.bindBuffer(PGL.ARRAY_BUFFER, 0);

      glLineIndex = createVertexBufferObject(lineBuffersContext);
      pgl.bindBuffer(PGL.ELEMENT_ARRAY_BUFFER, glLineIndex);
      pgl.bufferData(PGL.ELEMENT_ARRAY_BUFFER, sizex, null, PGL.STATIC_DRAW);

      pgl.bindBuffer(PGL.ELEMENT_ARRAY_BUFFER, 0);

      lineBuffersCreated = true;
    }
  }


  protected void updateLineBuffers() {
    createLineBuffers();

    int size = tessGeo.lineVertexCount;
    int sizef = size * PGL.SIZEOF_FLOAT;
    int sizei = size * PGL.SIZEOF_INT;

    tessGeo.updateLineVerticesBuffer();
    pgl.bindBuffer(PGL.ARRAY_BUFFER, glLineVertex);
    pgl.bufferData(PGL.ARRAY_BUFFER, 4 * sizef, tessGeo.lineVerticesBuffer,
                   PGL.STATIC_DRAW);

    tessGeo.updateLineColorsBuffer();
    pgl.bindBuffer(PGL.ARRAY_BUFFER, glLineColor);
    pgl.bufferData(PGL.ARRAY_BUFFER, sizei,
                   tessGeo.lineColorsBuffer, PGL.STATIC_DRAW);

    tessGeo.updateLineDirectionsBuffer();
    pgl.bindBuffer(PGL.ARRAY_BUFFER, glLineAttrib);
    pgl.bufferData(PGL.ARRAY_BUFFER, 4 * sizef,
                   tessGeo.lineDirectionsBuffer, PGL.STATIC_DRAW);

    tessGeo.updateLineIndicesBuffer();
    pgl.bindBuffer(PGL.ELEMENT_ARRAY_BUFFER, glLineIndex);
    pgl.bufferData(PGL.ELEMENT_ARRAY_BUFFER,
                   tessGeo.lineIndexCount * PGL.SIZEOF_INDEX,
                   tessGeo.lineIndicesBuffer, PGL.STATIC_DRAW);
  }


  protected void unbindLineBuffers() {
    pgl.bindBuffer(PGL.ARRAY_BUFFER, 0);
    pgl.bindBuffer(PGL.ELEMENT_ARRAY_BUFFER, 0);
  }


  protected boolean lineBufferContextIsOutdated() {
    return !pgl.contextIsCurrent(lineBuffersContext);
  }


  protected void deleteLineBuffers() {
    if (lineBuffersCreated) {
      deleteVertexBufferObject(glLineVertex, lineBuffersContext);
      glLineVertex = 0;

      deleteVertexBufferObject(glLineColor, lineBuffersContext);
      glLineColor = 0;

      deleteVertexBufferObject(glLineAttrib, lineBuffersContext);
      glLineAttrib = 0;

      deleteVertexBufferObject(glLineIndex, lineBuffersContext);
      glLineIndex = 0;

      lineBuffersCreated = false;
    }
  }


  protected void createPointBuffers() {
    if (!pointBuffersCreated || pointBuffersContextIsOutdated()) {
      pointBuffersContext = pgl.getCurrentContext();

      int sizef = INIT_VERTEX_BUFFER_SIZE * PGL.SIZEOF_FLOAT;
      int sizei = INIT_VERTEX_BUFFER_SIZE * PGL.SIZEOF_INT;
      int sizex = INIT_INDEX_BUFFER_SIZE * PGL.SIZEOF_INDEX;

      glPointVertex = createVertexBufferObject(pointBuffersContext);
      pgl.bindBuffer(PGL.ARRAY_BUFFER, glPointVertex);
      pgl.bufferData(PGL.ARRAY_BUFFER, 3 * sizef, null, PGL.STATIC_DRAW);

      glPointColor = createVertexBufferObject(pointBuffersContext);
      pgl.bindBuffer(PGL.ARRAY_BUFFER, glPointColor);
      pgl.bufferData(PGL.ARRAY_BUFFER, sizei, null, PGL.STATIC_DRAW);

      glPointAttrib = createVertexBufferObject(pointBuffersContext);
      pgl.bindBuffer(PGL.ARRAY_BUFFER, glPointAttrib);
      pgl.bufferData(PGL.ARRAY_BUFFER, 2 * sizef, null, PGL.STATIC_DRAW);

      pgl.bindBuffer(PGL.ARRAY_BUFFER, 0);

      glPointIndex = createVertexBufferObject(pointBuffersContext);
      pgl.bindBuffer(PGL.ELEMENT_ARRAY_BUFFER, glPointIndex);
      pgl.bufferData(PGL.ELEMENT_ARRAY_BUFFER, sizex, null, PGL.STATIC_DRAW);

      pgl.bindBuffer(PGL.ELEMENT_ARRAY_BUFFER, 0);

      pointBuffersCreated = true;
    }
  }


  protected void updatePointBuffers() {
    createPointBuffers();

    int size = tessGeo.pointVertexCount;
    int sizef = size * PGL.SIZEOF_FLOAT;
    int sizei = size * PGL.SIZEOF_INT;

    tessGeo.updatePointVerticesBuffer();
    pgl.bindBuffer(PGL.ARRAY_BUFFER, glPointVertex);
    pgl.bufferData(PGL.ARRAY_BUFFER, 4 * sizef,
                   tessGeo.pointVerticesBuffer, PGL.STATIC_DRAW);

    tessGeo.updatePointColorsBuffer();
    pgl.bindBuffer(PGL.ARRAY_BUFFER, glPointColor);
    pgl.bufferData(PGL.ARRAY_BUFFER, sizei,
                   tessGeo.pointColorsBuffer, PGL.STATIC_DRAW);

    tessGeo.updatePointOffsetsBuffer();
    pgl.bindBuffer(PGL.ARRAY_BUFFER, glPointAttrib);
    pgl.bufferData(PGL.ARRAY_BUFFER, 2 * sizef,
                   tessGeo.pointOffsetsBuffer, PGL.STATIC_DRAW);

    tessGeo.updatePointIndicesBuffer();
    pgl.bindBuffer(PGL.ELEMENT_ARRAY_BUFFER, glPointIndex);
    pgl.bufferData(PGL.ELEMENT_ARRAY_BUFFER,
      tessGeo.pointIndexCount * PGL.SIZEOF_INDEX,
      tessGeo.pointIndicesBuffer, PGL.STATIC_DRAW);
  }


  protected void unbindPointBuffers() {
    pgl.bindBuffer(PGL.ARRAY_BUFFER, 0);
    pgl.bindBuffer(PGL.ELEMENT_ARRAY_BUFFER, 0);
  }


  protected boolean pointBuffersContextIsOutdated() {
    return !pgl.contextIsCurrent(pointBuffersContext);
  }


  protected void deletePointBuffers() {
    if (pointBuffersCreated) {
      deleteVertexBufferObject(glPointVertex, pointBuffersContext);
      glPointVertex = 0;

      deleteVertexBufferObject(glPointColor, pointBuffersContext);
      glPointColor = 0;

      deleteVertexBufferObject(glPointAttrib, pointBuffersContext);
      glPointAttrib = 0;

      deleteVertexBufferObject(glPointIndex, pointBuffersContext);
      glPointIndex = 0;

      pointBuffersCreated = false;
    }
  }


  @Override
  public void requestFocus() {  // ignore
    pgl.requestFocus();
  }


  /**
   * OpenGL cannot draw until a proper native peer is available, so this
   * returns the value of PApplet.isDisplayable() (inherited from Component).
   */
  @Override
  public boolean canDraw() {
    return pgl.canDraw();
  }


  @Override
  public void requestDraw() {
    if (primarySurface) {
      if (initialized) {
        if (sized) reinitPrimary();

        pgl.requestDraw();
      } else {
        initPrimary();
      }
    }
  }


  @Override
  public void beginDraw() {
    report("top beginDraw()");

    if (!checkGLThread()) {
      return;
    }

    if (drawing) {
      PGraphics.showWarning(ALREADY_DRAWING_ERROR);
      return;
    }

    if (pgCurrent != null && !pgCurrent.primarySurface &&
                             !this.primarySurface) {
      // It seems that the user is trying to start another beginDraw()/endDraw()
      // block for an offscreen surface, still drawing on another one.
      PGraphics.showWarning(NESTED_DRAW_ERROR);
      return;
    }

    if (!primarySurface && pgPrimary.texCache.containsTexture(this)) {
      // This offscreen surface is being used as a texture earlier in draw,
      // so we should update the rendering up to this point since it will
      // modified.
      pgPrimary.flush();
    }

    if (!glParamsRead) {
      getGLParameters();
    }

    setViewport();
    if (primarySurface) {
      beginOnscreenDraw();
    } else {
      beginOffscreenDraw();
    }
    setDrawDefaults(); // TODO: look at using checkSettings() instead...


    pgCurrent = this;
    drawing = true;

    report("bot beginDraw()");
  }


  @Override
  public void endDraw() {
    report("top endDraw()");

    if (!drawing) {
      PGraphics.showWarning(NO_BEGIN_DRAW_ERROR);
      return;
    }

    // Flushing any remaining geometry.
    flush();

    if (PGL.SAVE_SURFACE_TO_PIXELS_HACK &&
        (!pgPrimary.initialized || parent.frameCount == 0)) {
      // Smooth was disabled/enabled at some point during drawing. We save
      // the current contents of the back buffer (because the  buffers haven't
      // been swapped yet) to the pixels array. The frameCount == 0 condition
      // is to handle the situation when no smooth is called in setup in the
      // PDE, but the OpenGL appears to be recreated due to the size() nastiness.
      saveSurfaceToPixels();
      restoreSurface = true;
    }

    if (primarySurface) {
      endOnscreenDraw();
    } else {
      endOffscreenDraw();
    }

    if (pgCurrent == pgPrimary) {
      // Done with the main surface
      pgCurrent = null;
    } else {
      // Done with an offscreen surface, going back to onscreen drawing.
      pgCurrent = pgPrimary;
    }
    drawing = false;

    report("bot endDraw()");
  }


  // Factory method
  protected PGL createPGL(PGraphicsOpenGL pg) {
    return new PJOGL(pg);
  }


  @Override
  public PGL beginPGL() {
    flush();
    pgl.beginGL();
    return pgl;
  }


  @Override
  public void endPGL() {
    pgl.endGL();
    restoreGL();
  }


  public void updateProjmodelview() {
    projmodelview.set(projection);
    projmodelview.apply(modelview);
  }


  protected void restartPGL() {
    initialized = false;
  }


  protected void restoreGL() {
    blendMode(blendMode);  // this should be set by reapplySettings...

    if (hints[DISABLE_DEPTH_TEST]) {
      pgl.disable(PGL.DEPTH_TEST);
    } else {
      pgl.enable(PGL.DEPTH_TEST);
    }
    pgl.depthFunc(PGL.LEQUAL);

    if (quality < 2) {
      pgl.disable(PGL.MULTISAMPLE);
    } else {
      pgl.enable(PGL.MULTISAMPLE);
      pgl.disable(PGL.POINT_SMOOTH);
      pgl.disable(PGL.LINE_SMOOTH);
      pgl.disable(PGL.POLYGON_SMOOTH);
    }

    pgl.viewport(viewport.get(0), viewport.get(1),
                 viewport.get(2), viewport.get(3));
    if (clip) {
      pgl.enable(PGL.SCISSOR_TEST);
      pgl.scissor(clipRect[0], clipRect[1], clipRect[2], clipRect[3]);
    } else {
      pgl.disable(PGL.SCISSOR_TEST);
    }

    pgl.frontFace(PGL.CW);
    pgl.disable(PGL.CULL_FACE);

    pgl.activeTexture(PGL.TEXTURE0);

    if (hints[DISABLE_DEPTH_MASK]) {
      pgl.depthMask(false);
    } else {
      pgl.depthMask(true);
    }

    currentFramebuffer.bind();
    pgl.drawBuffer(currentFramebuffer.getDefaultDrawBuffer());
  }

  public void beginReadPixels() {
    pgCurrent.beginPixelsOp(OP_READ);
  }

  public void endReadPixels() {
    pgCurrent.endPixelsOp();
  }

  protected void beginPixelsOp(int op) {
    FrameBuffer pixfb = null;
    if (primarySurface) {
      if (op == OP_READ) {
        if (pgl.isFBOBacked() && pgl.isMultisampled()) {
          // Making sure the back texture is up-to-date...
          pgl.syncBackTexture();
          // ...because the read framebuffer uses it as the color buffer (the
          // draw framebuffer is MSAA so it cannot be read from it).
          pixfb = readFramebuffer;
        } else {
          pixfb = drawFramebuffer;
        }
      } else if (op == OP_WRITE) {
        // We can write to the draw framebuffer irrespective of whether is
        // FBO-baked or multisampled.
        pixfb = drawFramebuffer;
      }
    } else {
      if (op == OP_READ) {
        if (offscreenMultisample) {
          // Making sure the offscreen FBO is up-to-date
          multisampleFramebuffer.copy(offscreenFramebuffer, currentFramebuffer);
        }
        // We always read the screen pixels from the color FBO.
        pixfb = offscreenFramebuffer;
      } else if (op == OP_WRITE) {
        // We can write directly to the color FBO, or to the multisample FBO
        // if multisampling is enabled.
        pixfb = offscreenMultisample ? multisampleFramebuffer :
                                       offscreenFramebuffer;
      }
    }

    // Set the framebuffer where the pixel operation shall be carried out.
    if (pixfb != currentFramebuffer) {
      pushFramebuffer();
      setFramebuffer(pixfb);
      pixOpChangedFB = true;
    }

    // We read from/write to the draw buffer.
    if (op == OP_READ) {
      pgl.readBuffer(currentFramebuffer.getDefaultDrawBuffer());
    } else if (op == OP_WRITE) {
      pgl.drawBuffer(currentFramebuffer.getDefaultDrawBuffer());
    }

    pixelsOp = op;
  }


  protected void endPixelsOp() {
    // Restoring current framebuffer prior to the pixel operation
    if (pixOpChangedFB) {
      popFramebuffer();
      pixOpChangedFB = false;
    }

    // Restoring default read/draw buffer configuration.
    pgl.readBuffer(currentFramebuffer.getDefaultReadBuffer());
    pgl.drawBuffer(currentFramebuffer.getDefaultDrawBuffer());

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
    // 3, 4, 5 the second, etc.):
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


  @Override
  protected void defaultSettings() {
    super.defaultSettings();

    manipulatingCamera = false;

    clearColorBuffer = false;

    // easiest for beginners
    textureMode(IMAGE);

    // Default material properties
    ambient(255);
    specular(125);
    emissive(0);
    shininess(1);

    // To indicate that the user hasn't set ambient
    setAmbient = false;
  }


  // reapplySettings

  //////////////////////////////////////////////////////////////

  // HINTS


  @Override
  public void hint(int which) {
    boolean oldValue = hints[PApplet.abs(which)];
    super.hint(which);
    boolean newValue = hints[PApplet.abs(which)];

    if (oldValue == newValue) {
      return;
    }

    if (which == DISABLE_DEPTH_TEST) {
      flush();
      pgl.disable(PGL.DEPTH_TEST);
    } else if (which == ENABLE_DEPTH_TEST) {
      flush();
      pgl.enable(PGL.DEPTH_TEST);
    } else if (which == DISABLE_DEPTH_MASK) {
      flush();
      pgl.depthMask(false);
    } else if (which == ENABLE_DEPTH_MASK) {
      flush();
      pgl.depthMask(true);
    } else if (which == ENABLE_OPTIMIZED_STROKE) {
      flush();
      setFlushMode(FLUSH_WHEN_FULL);
    } else if (which == DISABLE_OPTIMIZED_STROKE) {
      if (is2D()) {
        PGraphics.showWarning("Optimized strokes can only be disabled in 3D");
      } else {
        flush();
        setFlushMode(FLUSH_CONTINUOUSLY);
      }
    } else if (which == DISABLE_STROKE_PERSPECTIVE) {
      if (0 < tessGeo.lineVertexCount && 0 < tessGeo.lineIndexCount) {
        // We flush the geometry using the previous line setting.
        flush();
      }
    } else if (which == ENABLE_STROKE_PERSPECTIVE) {
      if (0 < tessGeo.lineVertexCount && 0 < tessGeo.lineIndexCount) {
        // We flush the geometry using the previous line setting.
        flush();
      }
    }
  }


  protected boolean getHint(int which) {
    if (which > 0) {
      return hints[which];
    } else {
      return !hints[-which];
    }
  }


  //////////////////////////////////////////////////////////////

  // VERTEX SHAPES


  @Override
  public void beginShape(int kind) {
    shape = kind;
    inGeo.clear();

    curveVertexCount = 0;
    breakShape = false;
    defaultEdges = true;

    textureImage0 = textureImage;
    // The superclass method is called to avoid an early flush.
    super.noTexture();

    normalMode = NORMAL_MODE_AUTO;
  }


  @Override
  public void endShape(int mode) {
    tessellate(mode);

    if ((flushMode == FLUSH_CONTINUOUSLY) ||
        (flushMode == FLUSH_WHEN_FULL && tessGeo.isFull())) {
      flush();
    }
  }


  protected void endShape(int[] indices) {
    if (shape != TRIANGLE && shape != TRIANGLES) {
      throw new RuntimeException("Indices and edges can only be set for " +
                                 "TRIANGLE shapes");
    }

    tessellate(indices);

    if (flushMode == FLUSH_CONTINUOUSLY ||
        (flushMode == FLUSH_WHEN_FULL && tessGeo.isFull())) {
      flush();
    }
  }


  @Override
  public void textureWrap(int wrap) {
    this.textureWrap = wrap;
  }


  public void textureSampling(int sampling) {
    this.textureSampling = sampling;
  }


  @Override
  public void beginContour() {
    if (openContour) {
      PGraphics.showWarning(ALREADY_BEGAN_CONTOUR_ERROR);
      return;
    }
    openContour = true;
    breakShape = true;
  }


  @Override
  public void endContour() {
    if (!openContour) {
      PGraphics.showWarning(NO_BEGIN_CONTOUR_ERROR);
      return;
    }
    openContour = false;
  }


  @Override
  public void vertex(float x, float y) {
    vertexImpl(x, y, 0, 0, 0);
  }


  @Override
  public void vertex(float x, float y, float u, float v) {
    vertexImpl(x, y, 0, u, v);
  }


  @Override
  public void vertex(float x, float y, float z) {
    vertexImpl(x, y, z, 0, 0);
  }


  @Override
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
      u /= textureImage.width;
      v /= textureImage.height;
    }

    inGeo.addVertex(x, y, z,
                    fcolor,
                    normalX, normalY, normalZ,
                    u, v,
                    scolor, sweight,
                    ambientColor, specularColor, emissiveColor, shininess,
                    VERTEX, vertexBreak());
  }


  protected boolean vertexBreak() {
    if (breakShape) {
      breakShape = false;
      return true;
    }
    return false;
  }


  @Override
  protected void clipImpl(float x1, float y1, float x2, float y2) {
    flush();
    pgl.enable(PGL.SCISSOR_TEST);

    float h = y2 - y1;
    clipRect[0] = (int)x1;
    clipRect[1] = (int)(height - y1 - h);
    clipRect[2] = (int)(x2 - x1);
    clipRect[3] = (int)h;
    pgl.scissor(clipRect[0], clipRect[1], clipRect[2], clipRect[3]);

    clip = true;
  }


  @Override
  public void noClip() {
    if (clip) {
      flush();
      pgl.disable(PGL.SCISSOR_TEST);
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
    tessellator.setTexCache(texCache, textureImage0, textureImage);
    tessellator.setStroke(stroke);
    tessellator.setStrokeColor(strokeColor);
    tessellator.setStrokeWeight(strokeWeight);
    tessellator.setStrokeCap(strokeCap);
    tessellator.setStrokeJoin(strokeJoin);
    tessellator.setRenderer(pgCurrent);
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
      tessellator.tessellatePolygon(false, mode == CLOSE,
                                    normalMode == NORMAL_MODE_AUTO);
    }
  }


  protected void tessellate(int[] indices) {
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

    if (stroke && defaultEdges) inGeo.addTrianglesEdges();
    if (normalMode == NORMAL_MODE_AUTO) inGeo.calcTrianglesNormals();
    tessellator.tessellateTriangles(indices);
  }


  @Override
  public void flush() {
    boolean hasPolys = 0 < tessGeo.polyVertexCount &&
                       0 < tessGeo.polyIndexCount;
    boolean hasLines = 0 < tessGeo.lineVertexCount &&
                       0 < tessGeo.lineIndexCount;
    boolean hasPoints = 0 < tessGeo.pointVertexCount &&
                        0 < tessGeo.pointIndexCount;

    boolean hasPixels = modified && pixels != null;

    if (hasPixels) {
      // If the user has been manipulating individual pixels,
      // the changes need to be copied to the screen before
      // drawing any new geometry.
      flushPixels();
    }

    if (hasPoints || hasLines || hasPolys) {
      PMatrix3D modelview0 = null;
      PMatrix3D modelviewInv0 = null;
      if (flushMode == FLUSH_WHEN_FULL) {
        // The modelview transformation has been applied already to the
        // tessellated vertices, so we set the OpenGL modelview matrix as
        // the identity to avoid applying the model transformations twice.
        // We save the modelview objects and temporarily use the identity
        // static matrix to avoid calling pushMatrix(), resetMatrix(),
        // popMatrix().
        modelview0 = modelview;
        modelviewInv0 = modelviewInv;
        modelview = modelviewInv = identity;
        projmodelview.set(projection);
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

      if (flushMode == FLUSH_WHEN_FULL) {
        modelview = modelview0;
        modelviewInv = modelviewInv0;
        updateProjmodelview();
      }
    }

    tessGeo.clear();
    texCache.clear();
    setgetPixels = false;
  }


  protected void flushPixels() {
    drawPixels(mx1, my1, mx2 - mx1, my2 - my1);
    modified = false;
  }


  protected void flushPolys() {
    boolean customShader = polyShader != null;
    boolean needNormals = customShader ? polyShader.accessNormals() : false;
    boolean needTexCoords = customShader ? polyShader.accessTexCoords() : false;

    updatePolyBuffers(lights, texCache.hasTextures, needNormals, needTexCoords);

    for (int i = 0; i < texCache.size; i++) {
      Texture tex = texCache.getTexture(i);

      // If the renderer is 2D, then lights should always be false,
      // so no need to worry about that.
      PShader shader = getPolyShader(lights, tex != null);
      shader.bind();

      int first = texCache.firstCache[i];
      int last = texCache.lastCache[i];
      IndexCache cache = tessGeo.polyIndexCache;

      for (int n = first; n <= last; n++) {
        int ioffset = n == first ? texCache.firstIndex[i] : cache.indexOffset[n];
        int icount = n == last ? texCache.lastIndex[i] - ioffset + 1 :
                                 cache.indexOffset[n] + cache.indexCount[n] - ioffset;
        int voffset = cache.vertexOffset[n];

        shader.setVertexAttribute(glPolyVertex, 4, PGL.FLOAT, 0,
                                  4 * voffset * PGL.SIZEOF_FLOAT);
        shader.setColorAttribute(glPolyColor, 4, PGL.UNSIGNED_BYTE, 0,
                                 4 * voffset * PGL.SIZEOF_BYTE);

        if (lights) {
          shader.setNormalAttribute(glPolyNormal, 3, PGL.FLOAT, 0,
                                    3 * voffset * PGL.SIZEOF_FLOAT);
          shader.setAmbientAttribute(glPolyAmbient, 4, PGL.UNSIGNED_BYTE, 0,
                                     4 * voffset * PGL.SIZEOF_BYTE);
          shader.setSpecularAttribute(glPolySpecular, 4, PGL.UNSIGNED_BYTE, 0,
                                      4 * voffset * PGL.SIZEOF_BYTE);
          shader.setEmissiveAttribute(glPolyEmissive, 4, PGL.UNSIGNED_BYTE, 0,
                                      4 * voffset * PGL.SIZEOF_BYTE);
          shader.setShininessAttribute(glPolyShininess, 1, PGL.FLOAT, 0,
                                       voffset * PGL.SIZEOF_FLOAT);
        }

        if (lights || needNormals) {
          shader.setNormalAttribute(glPolyNormal, 3, PGL.FLOAT, 0,
                                    3 * voffset * PGL.SIZEOF_FLOAT);
        }

        if (tex != null || needTexCoords) {
          shader.setTexcoordAttribute(glPolyTexcoord, 2, PGL.FLOAT, 0,
                                      2 * voffset * PGL.SIZEOF_FLOAT);
          shader.setTexture(tex);
        }

        pgl.bindBuffer(PGL.ELEMENT_ARRAY_BUFFER, glPolyIndex);
        pgl.drawElements(PGL.TRIANGLES, icount, PGL.INDEX_TYPE,
                         ioffset * PGL.SIZEOF_INDEX);
        pgl.bindBuffer(PGL.ELEMENT_ARRAY_BUFFER, 0);
      }

      shader.unbind();
    }
    unbindPolyBuffers();
  }


  void rawPolys() {
    raw.colorMode(RGB);
    raw.noStroke();
    raw.beginShape(TRIANGLES);

    float[] vertices = tessGeo.polyVertices;
    int[] color = tessGeo.polyColors;
    float[] uv = tessGeo.polyTexCoords;
    short[] indices = tessGeo.polyIndices;

    for (int i = 0; i < texCache.size; i++) {
      PImage textureImage = texCache.getTextureImage(i);

      int first = texCache.firstCache[i];
      int last = texCache.lastCache[i];
      IndexCache cache = tessGeo.polyIndexCache;
      for (int n = first; n <= last; n++) {
        int ioffset = n == first ? texCache.firstIndex[i] :
                                   cache.indexOffset[n];
        int icount = n == last ? texCache.lastIndex[i] - ioffset + 1 :
                                 cache.indexOffset[n] + cache.indexCount[n] -
                                 ioffset;
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

          if (flushMode == FLUSH_CONTINUOUSLY) {
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
              float sx0 = screenXImpl(pt0[0], pt0[1], pt0[2], pt0[3]);
              float sy0 = screenYImpl(pt0[0], pt0[1], pt0[2], pt0[3]);
              float sx1 = screenXImpl(pt1[0], pt1[1], pt1[2], pt1[3]);
              float sy1 = screenYImpl(pt1[0], pt1[1], pt1[2], pt1[3]);
              float sx2 = screenXImpl(pt2[0], pt2[1], pt2[2], pt2[3]);
              float sy2 = screenYImpl(pt2[0], pt2[1], pt2[2], pt2[3]);
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
              float sx0 = screenXImpl(pt0[0], pt0[1], pt0[2], pt0[3]);
              float sy0 = screenYImpl(pt0[0], pt0[1], pt0[2], pt0[3]);
              float sx1 = screenXImpl(pt1[0], pt1[1], pt1[2], pt1[3]);
              float sy1 = screenYImpl(pt1[0], pt1[1], pt1[2], pt1[3]);
              float sx2 = screenXImpl(pt2[0], pt2[1], pt2[2], pt2[3]);
              float sy2 = screenYImpl(pt2[0], pt2[1], pt2[2], pt2[3]);
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

    PShader shader = getLineShader();
    shader.bind();

    IndexCache cache = tessGeo.lineIndexCache;
    for (int n = 0; n < cache.size; n++) {
      int ioffset = cache.indexOffset[n];
      int icount = cache.indexCount[n];
      int voffset = cache.vertexOffset[n];

      shader.setVertexAttribute(glLineVertex, 4, PGL.FLOAT, 0,
                                4 * voffset * PGL.SIZEOF_FLOAT);
      shader.setColorAttribute(glLineColor, 4, PGL.UNSIGNED_BYTE, 0,
                               4 * voffset * PGL.SIZEOF_BYTE);
      shader.setLineAttribute(glLineAttrib, 4, PGL.FLOAT, 0,
                              4 * voffset * PGL.SIZEOF_FLOAT);

      pgl.bindBuffer(PGL.ELEMENT_ARRAY_BUFFER, glLineIndex);
      pgl.drawElements(PGL.TRIANGLES, icount, PGL.INDEX_TYPE,
                       ioffset * PGL.SIZEOF_INDEX);
      pgl.bindBuffer(PGL.ELEMENT_ARRAY_BUFFER, 0);
    }

    shader.unbind();
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
    float[] attribs = tessGeo.lineDirections;
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

        if (flushMode == FLUSH_CONTINUOUSLY) {
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
          float sx0 = screenXImpl(pt0[0], pt0[1], pt0[2], pt0[3]);
          float sy0 = screenYImpl(pt0[0], pt0[1], pt0[2], pt0[3]);
          float sx1 = screenXImpl(pt1[0], pt1[1], pt1[2], pt1[3]);
          float sy1 = screenYImpl(pt1[0], pt1[1], pt1[2], pt1[3]);
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

    PShader shader = getPointShader();
    shader.bind();

    IndexCache cache = tessGeo.pointIndexCache;
    for (int n = 0; n < cache.size; n++) {
      int ioffset = cache.indexOffset[n];
      int icount = cache.indexCount[n];
      int voffset = cache.vertexOffset[n];

      shader.setVertexAttribute(glPointVertex, 4, PGL.FLOAT, 0,
                                4 * voffset * PGL.SIZEOF_FLOAT);
      shader.setColorAttribute(glPointColor, 4, PGL.UNSIGNED_BYTE, 0,
                               4 * voffset * PGL.SIZEOF_BYTE);
      shader.setPointAttribute(glPointAttrib, 2, PGL.FLOAT, 0,
                               2 * voffset * PGL.SIZEOF_FLOAT);

      pgl.bindBuffer(PGL.ELEMENT_ARRAY_BUFFER, glPointIndex);
      pgl.drawElements(PGL.TRIANGLES, icount, PGL.INDEX_TYPE,
                       ioffset * PGL.SIZEOF_INDEX);
      pgl.bindBuffer(PGL.ELEMENT_ARRAY_BUFFER, 0);
    }

    shader.unbind();
    unbindPointBuffers();
  }


  void rawPoints() {
    raw.colorMode(RGB);
    raw.noFill();
    raw.strokeCap(strokeCap);
    raw.beginShape(POINTS);

    float[] vertices = tessGeo.pointVertices;
    int[] color = tessGeo.pointColors;
    float[] attribs = tessGeo.pointOffsets;
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
          perim = PApplet.min(MAX_POINT_ACCURACY, PApplet.max(MIN_POINT_ACCURACY,
                              (int) (TWO_PI * weight / POINT_ACCURACY_FACTOR))) + 1;
        } else {        // Square point
          weight = -size / 0.5f;
          perim = 5;
        }

        int i0 = voffset + indices[3 * pt];
        int argb0 = PGL.nativeToJavaARGB(color[i0]);
        float[] pt0 = {0, 0, 0, 0};

        if (flushMode == FLUSH_CONTINUOUSLY) {
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
          float sx0 = screenXImpl(pt0[0], pt0[1], pt0[2], pt0[3]);
          float sy0 = screenYImpl(pt0[0], pt0[1], pt0[2], pt0[3]);
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


  @Override
  public void bezierVertex(float x2, float y2,
                           float x3, float y3,
                           float x4, float y4) {
    bezierVertexImpl(x2, y2, 0,
                     x3, y3, 0,
                     x4, y4, 0);
  }


  @Override
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
                          x4, y4, z4, vertexBreak());
  }


  @Override
  public void quadraticVertex(float cx, float cy,
                              float x3, float y3) {
    quadraticVertexImpl(cx, cy, 0,
                        x3, y3, 0);
  }


  @Override
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
                             x3, y3, z3, vertexBreak());
  }


  //////////////////////////////////////////////////////////////

  // CATMULL-ROM CURVE VERTICES


  @Override
  public void curveVertex(float x, float y) {
    curveVertexImpl(x, y, 0);
  }


  @Override
  public void curveVertex(float x, float y, float z) {
    curveVertexImpl(x, y, z);
  }


  protected void curveVertexImpl(float x, float y, float z) {
    inGeo.setMaterial(fillColor, strokeColor, strokeWeight,
                      ambientColor, specularColor, emissiveColor, shininess);
    inGeo.setNormal(normalX, normalY, normalZ);
    inGeo.addCurveVertex(x, y, z, vertexBreak());
  }


  //////////////////////////////////////////////////////////////

  // POINT, LINE, TRIANGLE, QUAD


  @Override
  public void point(float x, float y) {
    pointImpl(x, y, 0);
  }


  @Override
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


  @Override
  public void line(float x1, float y1, float x2, float y2) {
    lineImpl(x1, y1, 0, x2, y2, 0);
  }


  @Override
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


  @Override
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


  @Override
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
                  stroke);
    endShape();
  }


  @Override
  protected void rectImpl(float x1, float y1, float x2, float y2,
                          float tl, float tr, float br, float bl) {
    beginShape(POLYGON);
    defaultEdges = false;
    normalMode = NORMAL_MODE_SHAPE;
    inGeo.setMaterial(fillColor, strokeColor, strokeWeight,
                      ambientColor, specularColor, emissiveColor, shininess);
    inGeo.setNormal(normalX, normalY, normalZ);
    inGeo.addRect(x1, y1, x2, y2, tl, tr, br, bl, stroke);
    endShape();
  }


  //////////////////////////////////////////////////////////////

  // ELLIPSE


  @Override
  public void ellipseImpl(float a, float b, float c, float d) {
     beginShape(TRIANGLE_FAN);
     defaultEdges = false;
     normalMode = NORMAL_MODE_SHAPE;
     inGeo.setMaterial(fillColor, strokeColor, strokeWeight,
                       ambientColor, specularColor, emissiveColor, shininess);
     inGeo.setNormal(normalX, normalY, normalZ);
     inGeo.addEllipse(a, b, c, d, fill, stroke);
     endShape();
  }


  @Override
  protected void arcImpl(float x, float y, float w, float h,
                         float start, float stop, int mode) {
    beginShape(TRIANGLE_FAN);
    defaultEdges = false;
    normalMode = NORMAL_MODE_SHAPE;
    inGeo.setMaterial(fillColor, strokeColor, strokeWeight,
                      ambientColor, specularColor, emissiveColor, shininess);
    inGeo.setNormal(normalX, normalY, normalZ);
    inGeo.addArc(x, y, w, h, start, stop, fill, stroke, mode);
    endShape();
  }


  //////////////////////////////////////////////////////////////

  // BOX

  // public void box(float size)

  @Override
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

  @Override
  public void sphere(float r) {
    if ((sphereDetailU < 3) || (sphereDetailV < 2)) {
      sphereDetail(30);
    }

    beginShape(TRIANGLES);
    defaultEdges = false;
    normalMode = NORMAL_MODE_VERTEX;
    inGeo.setMaterial(fillColor, strokeColor, strokeWeight,
                      ambientColor, specularColor, emissiveColor, shininess);
    int[] indices = inGeo.addSphere(r, sphereDetailU, sphereDetailV,
                                    fill, stroke);
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


  @Override
  public void smooth() {
    smooth(2);
  }


  @Override
  public void smooth(int level) {
    if (smoothDisabled) return;

    smooth = true;

    if (maxSamples < level) {
      if (0 < maxSamples) {
        PGraphics.showWarning(UNSUPPORTED_SMOOTH_LEVEL_ERROR, level, maxSamples);
      } else{
        PGraphics.showWarning(UNSUPPORTED_SMOOTH_ERROR);
      }
      level = maxSamples;
    }

    if (quality != level) {
      smoothCallCount++;
      if (parent.frameCount - lastSmoothCall < 30 && 5 < smoothCallCount) {
        smoothDisabled = true;
        PGraphics.showWarning(TOO_MANY_SMOOTH_CALLS_ERROR);
      }
      lastSmoothCall = parent.frameCount;

      quality = level;
      if (quality == 1) {
        quality = 0;
      }

      // This will trigger a surface restart next time
      // requestDraw() is called.
      restartPGL();
    }
  }


  @Override
  public void noSmooth() {
    if (smoothDisabled) return;

    smooth = false;

    if (1 < quality) {
      smoothCallCount++;
      if (parent.frameCount - lastSmoothCall < 30 && 5 < smoothCallCount) {
        smoothDisabled = true;
        PGraphics.showWarning(TOO_MANY_SMOOTH_CALLS_ERROR);
      }
      lastSmoothCall = parent.frameCount;

      quality = 0;

      // This will trigger a surface restart next time
      // requestDraw() is called.
      restartPGL();
    }
  }


  //////////////////////////////////////////////////////////////

  // SHAPE

  // public void shapeMode(int mode)


  // TODO unapproved
  @Override
  protected void shape(PShape shape, float x, float y, float z) {
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


  // TODO unapproved
  @Override
  protected void shape(PShape shape, float x, float y, float z,
                       float c, float d, float e) {
    if (shape.isVisible()) { // don't do expensive matrix ops if invisible
      flush();

      pushMatrix();

      if (shapeMode == CENTER) {
        // x, y and z are center, c, d and e refer to a diameter
        translate(x - c / 2f, y - d / 2f, z - e / 2f);
        scale(c / shape.getWidth(),
              d / shape.getHeight(),
              e / shape.getDepth());

      } else if (shapeMode == CORNER) {
        translate(x, y, z);
        scale(c / shape.getWidth(),
              d / shape.getHeight(),
              e / shape.getDepth());

      } else if (shapeMode == CORNERS) {
        // c, d, e are x2/y2/z2, make them into width/height/depth
        c -= x;
        d -= y;
        e -= z;
        // then same as above
        translate(x, y, z);
        scale(c / shape.getWidth(),
              d / shape.getHeight(),
              e / shape.getDepth());
      }
      shape.draw(this);

      popMatrix();
    }
  }


  //////////////////////////////////////////////////////////////

  // SHAPE I/O


  @Override
  public PShape loadShape(String filename) {
    String ext = PApplet.getExtension(filename);
    if (PGraphics2D.isSupportedExtension(ext)) {
      return PGraphics2D.loadShapeImpl(this, filename, ext);
    } if (PGraphics3D.isSupportedExtension(ext)) {
      return PGraphics3D.loadShapeImpl(this, filename, ext);
    } else {
      PGraphics.showWarning(UNSUPPORTED_SHAPE_FORMAT_ERROR);
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

  @Override
  protected boolean textModeCheck(int mode) {
    return mode == MODEL || (mode == SHAPE && PGL.SHAPE_TEXT_SUPPORTED);
  }

  // public void textSize(float size)

  // public float textWidth(char c)

  // public float textWidth(String str)

  // protected float textWidthImpl(char buffer[], int start, int stop)


  //////////////////////////////////////////////////////////////

  // TEXT IMPL


  @Override
  public float textAscent() {
    if (textFont == null) defaultFontOrDeath("textAscent");
    Object font = textFont.getNative();
    float ascent = 0;
    if (font != null) ascent = pgl.getFontAscent(font);
    if (ascent == 0) ascent = super.textAscent();
    return ascent;
  }


  @Override
  public float textDescent() {
    if (textFont == null) defaultFontOrDeath("textAscent");
    Object font = textFont.getNative();
    float descent = 0;
    if (font != null) descent = pgl.getFontDescent(font);
    if (descent == 0) descent = super.textDescent();
    return descent;
  }


  @Override
  protected float textWidthImpl(char buffer[], int start, int stop) {
    Object font = textFont.getNative();
    float twidth = 0;
    if (font != null) twidth = pgl.getTextWidth(font, buffer, start, stop);
    if (twidth == 0) twidth = super.textWidthImpl(buffer, start, stop);
    return twidth;
  }


  @Override
  public void textSize(float size) {
    if (textFont == null) defaultFontOrDeath("textSize", size);
    Object font = textFont.getNative();
    if (font != null) {
      Object dfont = pgl.getDerivedFont(font, size);
      textFont.setNative(dfont);
    }
    super.textSize(size);
  }


  /**
   * Implementation of actual drawing for a line of text.
   */
  @Override
  protected void textLineImpl(char buffer[], int start, int stop,
                              float x, float y) {
    if (textMode == MODEL) {
      textTex = pgPrimary.getFontTexture(textFont);

      if (textTex == null || textTex.contextIsOutdated()) {
        textTex = new FontTexture(pgPrimary, textFont, is3D());
        pgPrimary.setFontTexture(textFont, textTex);
      }

      textTex.begin();

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

      textTex.end();
    } else if (textMode == SHAPE) {
      super.textLineImpl(buffer, start, stop, x, y);
    }
  }


  @Override
  protected void textCharImpl(char ch, float x, float y) {
    PFont.Glyph glyph = textFont.getGlyph(ch);
    if (glyph != null) {
      if (textMode == MODEL) {
        FontTexture.TextureInfo tinfo = textTex.getTexInfo(glyph);

        if (tinfo == null) {
          // Adding new glyph to the font texture.
          tinfo = textTex.addToTexture(pgPrimary, glyph);
        }

        float high    = glyph.height     / (float) textFont.getSize();
        float bwidth  = glyph.width      / (float) textFont.getSize();
        float lextent = glyph.leftExtent / (float) textFont.getSize();
        float textent = glyph.topExtent  / (float) textFont.getSize();

        float x1 = x + lextent * textSize;
        float y1 = y - textent * textSize;
        float x2 = x1 + bwidth * textSize;
        float y2 = y1 + high * textSize;

        textCharModelImpl(tinfo, x1, y1, x2, y2);
      } else if (textMode == SHAPE) {
        textCharShapeImpl(ch, x, y);
      }
    }
  }


  protected void textCharModelImpl(FontTexture.TextureInfo info,
                                   float x0, float y0,
                                   float x1, float y1) {
    if (textTex.currentTex != info.texIndex) {
      textTex.setTexture(info.texIndex);
    }
    beginShape(QUADS);
    texture(textTex.getCurrentTexture());
    vertex(x0, y0, info.u0, info.v0);
    vertex(x1, y0, info.u1, info.v0);
    vertex(x1, y1, info.u1, info.v1);
    vertex(x0, y1, info.u0, info.v1);
    endShape();
  }


  /**
   * Ported from the implementation of textCharShapeImpl() in 1.5.1
   *
   * <EM>No attempt has been made to optimize this code</EM>
   * <p/>
   * TODO: Implement a FontShape class where each glyph is tessellated and
   * stored inside a larger PShapeOpenGL object (which needs to be expanded as
   * new glyphs are added and exceed the initial capacity in a similar way as
   * the textures in FontTexture work). When a string of text is to be rendered
   * in shape mode, then the correct sequences of vertex indices are computed
   * (akin to the texcoords in the texture case) and used to draw only those
   * parts of the PShape object that are required for the text.
   * <p/>
   *
   * Some issues of the original implementation probably remain, so they are
   * reproduced below:
   * <p/>
   * Also a problem where some fonts seem to be a bit slight, as if the
   * control points aren't being mapped quite correctly. Probably doing
   * something dumb that the control points don't map to P5's control
   * points. Perhaps it's returning b-spline data from the TrueType font?
   * Though it seems like that would make a lot of garbage rather than
   * just a little flattening.
   * <p/>
   * There also seems to be a bug that is causing a line (but not a filled
   * triangle) back to the origin on some letters (i.e. a capital L when
   * tested with Akzidenz Grotesk Light). But this won't be visible
   * with the stroke shut off, so tabling that bug for now.
   */
  protected void textCharShapeImpl(char ch, float x, float y) {
    // save the current stroke because it needs to be disabled
    // while the text is being drawn
    boolean strokeSaved = stroke;
    stroke = false;

    PGL.FontOutline outline = pgl.createFontOutline(ch, textFont.getNative());

    // six element array received from the Java2D path iterator
    float textPoints[] = new float[6];
    float lastX = 0;
    float lastY = 0;

    beginShape();
    while (!outline.isDone()) {
      int type = outline.currentSegment(textPoints);
      if (type == PGL.SEG_MOVETO) {         // 1 point (2 vars) in textPoints
      } else if (type == PGL.SEG_LINETO) {  // 1 point
        if (type == PGL.SEG_MOVETO) beginContour();
        vertex(x + textPoints[0], y + textPoints[1]);
        lastX = textPoints[0];
        lastY = textPoints[1];
      } else if (type == PGL.SEG_QUADTO) {   // 2 points
        for (int i = 1; i < bezierDetail; i++) {
          float t = (float)i / (float)bezierDetail;
          vertex(x + bezierPoint(lastX,
                            lastX + (float) ((textPoints[0] - lastX) * 2/3.0),
                            textPoints[2] + (float) ((textPoints[0] - textPoints[2]) * 2/3.0),
                            textPoints[2], t),
                 y + bezierPoint(lastY,
                            lastY + (float) ((textPoints[1] - lastY) * 2/3.0),
                            textPoints[3] + (float) ((textPoints[1] - textPoints[3]) * 2/3.0),
                            textPoints[3], t));
        }
        lastX = textPoints[2];
        lastY = textPoints[3];
      } else if (type == PGL.SEG_CUBICTO) {  // 3 points
        for (int i = 1; i < bezierDetail; i++) {
          float t = (float)i / (float)bezierDetail;
          vertex(x + bezierPoint(lastX, textPoints[0],
                                 textPoints[2], textPoints[4], t),
                 y + bezierPoint(lastY, textPoints[1],
                                 textPoints[3], textPoints[5], t));
        }
        lastX = textPoints[4];
        lastY = textPoints[5];
      } else if (type == PGL.SEG_CLOSE) {
        endContour();
      }
      outline.next();
    }
    endShape();

    // re-enable stroke if it was in use before
    stroke = strokeSaved;
  }


  //////////////////////////////////////////////////////////////

  // MATRIX STACK


  @Override
  public void pushMatrix() {
    if (modelviewStackDepth == MATRIX_STACK_DEPTH) {
      throw new RuntimeException(ERROR_PUSHMATRIX_OVERFLOW);
    }
    modelview.get(modelviewStack[modelviewStackDepth]);
    modelviewInv.get(modelviewInvStack[modelviewStackDepth]);
    camera.get(cameraStack[modelviewStackDepth]);
    cameraInv.get(cameraInvStack[modelviewStackDepth]);
    modelviewStackDepth++;
  }


  @Override
  public void popMatrix() {
    if (modelviewStackDepth == 0) {
      throw new RuntimeException(ERROR_PUSHMATRIX_UNDERFLOW);
    }
    modelviewStackDepth--;
    modelview.set(modelviewStack[modelviewStackDepth]);
    modelviewInv.set(modelviewInvStack[modelviewStackDepth]);
    camera.set(cameraStack[modelviewStackDepth]);
    cameraInv.set(cameraInvStack[modelviewStackDepth]);
    updateProjmodelview();
  }


  //////////////////////////////////////////////////////////////

  // MATRIX TRANSFORMATIONS


  @Override
  public void translate(float tx, float ty) {
    translateImpl(tx, ty, 0);
  }


  @Override
  public void translate(float tx, float ty, float tz) {
    translateImpl(tx, ty, tz);
  }


  protected void translateImpl(float tx, float ty, float tz) {
    modelview.translate(tx, ty, tz);
    invTranslate(modelviewInv, tx, ty, tz);
    projmodelview.translate(tx, ty, tz);
  }


  static protected void invTranslate(PMatrix3D matrix,
                                     float tx, float ty, float tz) {
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
  @Override
  public void rotate(float angle) {
    rotateImpl(angle, 0, 0, 1);
  }


  @Override
  public void rotateX(float angle) {
    rotateImpl(angle, 1, 0, 0);
  }


  @Override
  public void rotateY(float angle) {
    rotateImpl(angle, 0, 1, 0);
  }


  @Override
  public void rotateZ(float angle) {
    rotateImpl(angle, 0, 0, 1);
  }


  /**
   * Rotate around an arbitrary vector, similar to glRotate(), except that it
   * takes radians (instead of degrees).
   */
  @Override
  public void rotate(float angle, float v0, float v1, float v2) {
    rotateImpl(angle, v0, v1, v2);
  }


  protected void rotateImpl(float angle, float v0, float v1, float v2) {
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
    updateProjmodelview(); // Possibly cheaper than doing projmodelview.rotate()
  }


  static private void invRotate(PMatrix3D matrix, float angle,
                                float v0, float v1, float v2) {
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
  @Override
  public void scale(float s) {
    scaleImpl(s, s, s);
  }


  /**
   * Same as scale(sx, sy, 1).
   */
  @Override
  public void scale(float sx, float sy) {
    scaleImpl(sx, sy, 1);
  }


  /**
   * Scale in three dimensions.
   */
  @Override
  public void scale(float sx, float sy, float sz) {
    scaleImpl(sx, sy, sz);
  }

  /**
   * Scale in three dimensions.
   */
  protected void scaleImpl(float sx, float sy, float sz) {
    modelview.scale(sx, sy, sz);
    invScale(modelviewInv, sx, sy, sz);
    projmodelview.scale(sx, sy, sz);
  }


  static protected void invScale(PMatrix3D matrix, float x, float y, float z) {
    matrix.preApply(1/x, 0, 0, 0,  0, 1/y, 0, 0,  0, 0, 1/z, 0,  0, 0, 0, 1);
  }


  @Override
  public void shearX(float angle) {
    float t = (float) Math.tan(angle);
    applyMatrixImpl(1, t, 0, 0,
                    0, 1, 0, 0,
                    0, 0, 1, 0,
                    0, 0, 0, 1);
  }


  @Override
  public void shearY(float angle) {
    float t = (float) Math.tan(angle);
    applyMatrixImpl(1, 0, 0, 0,
                    t, 1, 0, 0,
                    0, 0, 1, 0,
                    0, 0, 0, 1);
  }


  //////////////////////////////////////////////////////////////

  // MATRIX MORE!


  @Override
  public void resetMatrix() {
    modelview.reset();
    modelviewInv.reset();
    projmodelview.set(projection);

    // For consistency, since modelview = camera * [all other transformations]
    // the camera matrix should be set to the identity as well:
    camera.reset();
    cameraInv.reset();
  }


  @Override
  public void applyMatrix(PMatrix2D source) {
    applyMatrixImpl(source.m00, source.m01, 0, source.m02,
                    source.m10, source.m11, 0, source.m12,
                             0,          0, 1, 0,
                             0,          0, 0, 1);
  }


  @Override
  public void applyMatrix(float n00, float n01, float n02,
                          float n10, float n11, float n12) {
    applyMatrixImpl(n00, n01, 0, n02,
                    n10, n11, 0, n12,
                      0,   0, 1,   0,
                      0,   0, 0,   1);
  }


  @Override
  public void applyMatrix(PMatrix3D source) {
    applyMatrixImpl(source.m00, source.m01, source.m02, source.m03,
                    source.m10, source.m11, source.m12, source.m13,
                    source.m20, source.m21, source.m22, source.m23,
                    source.m30, source.m31, source.m32, source.m33);
  }


  /**
   * Apply a 4x4 transformation matrix to the modelview stack.
   */
  @Override
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
    modelview.apply(n00, n01, n02, n03,
                    n10, n11, n12, n13,
                    n20, n21, n22, n23,
                    n30, n31, n32, n33);
    modelviewInv.set(modelview);
    modelviewInv.invert();

    projmodelview.apply(n00, n01, n02, n03,
                        n10, n11, n12, n13,
                        n20, n21, n22, n23,
                        n30, n31, n32, n33);
  }


  protected void begin2D() {
  }


  protected void end2D() {
  }


  //////////////////////////////////////////////////////////////

  // MATRIX GET/SET/PRINT


  @Override
  public PMatrix getMatrix() {
    return modelview.get();
  }


  // public PMatrix2D getMatrix(PMatrix2D target)


  @Override
  public PMatrix3D getMatrix(PMatrix3D target) {
    if (target == null) {
      target = new PMatrix3D();
    }
    target.set(modelview);
    return target;
  }


  // public void setMatrix(PMatrix source)


  @Override
  public void setMatrix(PMatrix2D source) {
    resetMatrix();
    applyMatrix(source);
  }


  /**
   * Set the current transformation to the contents of the specified source.
   */
  @Override
  public void setMatrix(PMatrix3D source) {
    resetMatrix();
    applyMatrix(source);
  }


  /**
   * Print the current model (or "transformation") matrix.
   */
  @Override
  public void printMatrix() {
    modelview.print();
  }


  //////////////////////////////////////////////////////////////

  // PROJECTION


  public void pushProjection() {
    if (projectionStackDepth == MATRIX_STACK_DEPTH) {
      throw new RuntimeException(ERROR_PUSHMATRIX_OVERFLOW);
    }
    projection.get(projectionStack[projectionStackDepth]);
    projectionStackDepth++;
  }


  public void popProjection() {
    flush(); // The geometry with the old projection matrix needs to be drawn now

    if (projectionStackDepth == 0) {
      throw new RuntimeException(ERROR_PUSHMATRIX_UNDERFLOW);
    }
    projectionStackDepth--;
    projection.set(projectionStack[projectionStackDepth]);
    updateProjmodelview();
  }


  public void resetProjection() {
    flush();
    projection.reset();
    updateProjmodelview();
  }


  public void applyProjection(PMatrix3D mat) {
    flush();
    projection.apply(mat);
    updateProjmodelview();
  }


  public void applyProjection(float n00, float n01, float n02, float n03,
                              float n10, float n11, float n12, float n13,
                              float n20, float n21, float n22, float n23,
                              float n30, float n31, float n32, float n33) {
    flush();
    projection.apply(n00, n01, n02, n03,
                     n10, n11, n12, n13,
                     n20, n21, n22, n23,
                     n30, n31, n32, n33);
    updateProjmodelview();
  }


  public void setProjection(PMatrix3D mat) {
    flush();
    projection.set(mat);
    updateProjmodelview();
  }


  // Returns true if the matrix is of the form:
  // x, 0, 0, a,
  // 0, y, 0, b,
  // 0, 0, z, c,
  // 0, 0, 0, 1
  protected boolean orthoProjection() {
    return zero(projection.m01) && zero(projection.m02) &&
           zero(projection.m10) && zero(projection.m12) &&
           zero(projection.m20) && zero(projection.m21) &&
           zero(projection.m30) && zero(projection.m31) &&
           zero(projection.m32) && same(projection.m33, 1);
  }


  protected boolean nonOrthoProjection() {
    return nonZero(projection.m01) || nonZero(projection.m02) ||
           nonZero(projection.m10) || nonZero(projection.m12) ||
           nonZero(projection.m20) || nonZero(projection.m21) ||
           nonZero(projection.m30) || nonZero(projection.m31) ||
           nonZero(projection.m32) || diff(projection.m33, 1);
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
  @Override
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
  @Override
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
  @Override
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
  @Override
  public void camera(float eyeX, float eyeY, float eyeZ,
                     float centerX, float centerY, float centerZ,
                     float upX, float upY, float upZ) {
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
    float x0 =  y1 * z2 - y2 * z1;
    float x1 = -y0 * z2 + y2 * z0;
    float x2 =  y0 * z1 - y1 * z0;

    // Recompute Y = Z cross X
    y0 =  z1 * x2 - z2 * x1;
    y1 = -z0 * x2 + z2 * x0;
    y2 =  z0 * x1 - z1 * x0;

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

    updateProjmodelview();
  }


  // Sets a camera for 2D rendering, which only involves centering
  public void camera(float centerX, float centerY) {
    modelview.reset();
    modelview.translate(-centerX, -centerY);

    modelviewInv.set(modelview);
    modelviewInv.invert();

    camera.set(modelview);
    cameraInv.set(modelviewInv);

    updateProjmodelview();
  }


  /**
   * Print the current camera matrix.
   */
  @Override
  public void printCamera() {
    camera.print();
  }


  protected void defaultCamera() {
    camera();
  }


  //////////////////////////////////////////////////////////////

  // PROJECTION


  /**
   * Calls ortho() with the proper parameters for Processing's standard
   * orthographic projection.
   */
  @Override
  public void ortho() {
    ortho(0, width, 0, height, cameraNear, cameraFar);
  }


  /**
   * Calls ortho() with the specified size of the viewing volume along
   * the X and Z directions.
   */
  @Override
  public void ortho(float left, float right,
                    float bottom, float top) {
    ortho(left, right, bottom, top, cameraNear, cameraFar);
  }


  /**
   * Sets an orthographic projection.
   *
   */
  @Override
  public void ortho(float left, float right,
                    float bottom, float top,
                    float near, float far) {
    left   -= width/2f;
    right  -= width/2f;
    bottom -= height/2f;
    top    -= height/2f;

    // Flushing geometry with a different perspective configuration.
    flush();

    float x = +2.0f / (right - left);
    float y = +2.0f / (top - bottom);
    float z = -2.0f / (far - near);

    float tx = -(right + left) / (right - left);
    float ty = -(top + bottom) / (top - bottom);
    float tz = -(far + near)   / (far - near);

    // The minus sign is needed to invert the Y axis.
    projection.set(x,  0, 0, tx,
                   0, -y, 0, ty,
                   0,  0, z, tz,
                   0,  0, 0,  1);

    updateProjmodelview();
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
  @Override
  public void perspective() {
    perspective(cameraFOV, cameraAspect, cameraNear, cameraFar);
  }


  /**
   * Similar to gluPerspective(). Implementation based on Mesa's glu.c
   */
  @Override
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
  @Override
  public void frustum(float left, float right, float bottom, float top,
                      float znear, float zfar) {
    // Flushing geometry with a different perspective configuration.
    flush();

    float n2 = 2 * znear;
    float w = right - left;
    float h = top - bottom;
    float d = zfar - znear;

    projection.set(n2 / w,       0,  (right + left) / w,                0,
                        0, -n2 / h,  (top + bottom) / h,                0,
                        0,       0, -(zfar + znear) / d, -(n2 * zfar) / d,
                        0,       0,                  -1,                0);

    updateProjmodelview();
  }


  /**
   * Print the current projection matrix.
   */
  @Override
  public void printProjection() {
    projection.print();
  }


  protected void defaultPerspective() {
    perspective();
  }


  //////////////////////////////////////////////////////////////

  // SCREEN AND MODEL COORDS


  @Override
  public float screenX(float x, float y) {
    return screenXImpl(x, y, 0);
  }


  @Override
  public float screenY(float x, float y) {
    return screenYImpl(x, y, 0);
  }


  @Override
  public float screenX(float x, float y, float z) {
    return screenXImpl(x, y, z);
  }


  @Override
  public float screenY(float x, float y, float z) {
    return screenYImpl(x, y, z);
  }


  @Override
  public float screenZ(float x, float y, float z) {
    return screenZImpl(x, y, z);
  }


  protected float screenXImpl(float x, float y, float z) {
    float ax =
      modelview.m00*x + modelview.m01*y + modelview.m02*z + modelview.m03;
    float ay =
      modelview.m10*x + modelview.m11*y + modelview.m12*z + modelview.m13;
    float az =
      modelview.m20*x + modelview.m21*y + modelview.m22*z + modelview.m23;
    float aw =
      modelview.m30*x + modelview.m31*y + modelview.m32*z + modelview.m33;
    return screenXImpl(ax, ay, az, aw);
  }


  protected float screenXImpl(float x, float y, float z, float w) {
    float ox =
      projection.m00*x + projection.m01*y + projection.m02*z + projection.m03*w;
    float ow =
      projection.m30*x + projection.m31*y + projection.m32*z + projection.m33*w;

    if (nonZero(ow)) {
      ox /= ow;
    }
    float sx = width * (1 + ox) / 2.0f;
    return sx;
  }


  protected float screenYImpl(float x, float y, float z) {
    float ax =
      modelview.m00*x + modelview.m01*y + modelview.m02*z + modelview.m03;
    float ay =
      modelview.m10*x + modelview.m11*y + modelview.m12*z + modelview.m13;
    float az =
      modelview.m20*x + modelview.m21*y + modelview.m22*z + modelview.m23;
    float aw =
      modelview.m30*x + modelview.m31*y + modelview.m32*z + modelview.m33;
    return screenYImpl(ax, ay, az, aw);
  }


  protected float screenYImpl(float x, float y, float z, float w) {
    float oy =
      projection.m10*x + projection.m11*y + projection.m12*z + projection.m13*w;
    float ow =
      projection.m30*x + projection.m31*y + projection.m32*z + projection.m33*w;

    if (nonZero(ow)) {
      oy /= ow;
    }
    float sy = height * (1 + oy) / 2.0f;
    // Turning value upside down because of Processing's inverted Y axis.
    sy = height - sy;
    return sy;
  }


  protected float screenZImpl(float x, float y, float z) {
    float ax =
      modelview.m00*x + modelview.m01*y + modelview.m02*z + modelview.m03;
    float ay =
      modelview.m10*x + modelview.m11*y + modelview.m12*z + modelview.m13;
    float az =
      modelview.m20*x + modelview.m21*y + modelview.m22*z + modelview.m23;
    float aw =
      modelview.m30*x + modelview.m31*y + modelview.m32*z + modelview.m33;
    return screenZImpl(ax, ay, az, aw);
  }


  protected float screenZImpl(float x, float y, float z, float w) {
    float oz =
      projection.m20*x + projection.m21*y + projection.m22*z + projection.m23*w;
    float ow =
      projection.m30*x + projection.m31*y + projection.m32*z + projection.m33*w;

    if (nonZero(ow)) {
      oz /= ow;
    }
    float sz = (oz + 1) / 2.0f;
    return sz;
  }


  @Override
  public float modelX(float x, float y, float z) {
    float ax =
      modelview.m00*x + modelview.m01*y + modelview.m02*z + modelview.m03;
    float ay =
      modelview.m10*x + modelview.m11*y + modelview.m12*z + modelview.m13;
    float az =
      modelview.m20*x + modelview.m21*y + modelview.m22*z + modelview.m23;
    float aw =
      modelview.m30*x + modelview.m31*y + modelview.m32*z + modelview.m33;

    float ox =
      cameraInv.m00*ax + cameraInv.m01*ay + cameraInv.m02*az + cameraInv.m03*aw;
    float ow =
      cameraInv.m30*ax + cameraInv.m31*ay + cameraInv.m32*az + cameraInv.m33*aw;

    return nonZero(ow) ? ox / ow : ox;
  }


  @Override
  public float modelY(float x, float y, float z) {
    float ax =
      modelview.m00*x + modelview.m01*y + modelview.m02*z + modelview.m03;
    float ay =
      modelview.m10*x + modelview.m11*y + modelview.m12*z + modelview.m13;
    float az =
      modelview.m20*x + modelview.m21*y + modelview.m22*z + modelview.m23;
    float aw =
      modelview.m30*x + modelview.m31*y + modelview.m32*z + modelview.m33;

    float oy =
      cameraInv.m10*ax + cameraInv.m11*ay + cameraInv.m12*az + cameraInv.m13*aw;
    float ow =
      cameraInv.m30*ax + cameraInv.m31*ay + cameraInv.m32*az + cameraInv.m33*aw;

    return nonZero(ow) ? oy / ow : oy;
  }


  @Override
  public float modelZ(float x, float y, float z) {
    float ax =
      modelview.m00*x + modelview.m01*y + modelview.m02*z + modelview.m03;
    float ay =
      modelview.m10*x + modelview.m11*y + modelview.m12*z + modelview.m13;
    float az =
      modelview.m20*x + modelview.m21*y + modelview.m22*z + modelview.m23;
    float aw =
      modelview.m30*x + modelview.m31*y + modelview.m32*z + modelview.m33;

    float oz =
      cameraInv.m20*ax + cameraInv.m21*ay + cameraInv.m22*az + cameraInv.m23*aw;
    float ow =
      cameraInv.m30*ax + cameraInv.m31*ay + cameraInv.m32*az + cameraInv.m33*aw;

    return nonZero(ow) ? oz / ow : oz;
  }

  //////////////////////////////////////////////////////////////

  // STYLES

  @Override
  public void popStyle() {
    // popStyle() sets ambient to true (because it calls ambient() in style())
    // and so setting the setAmbient flag to true, even if the user didn't call
    // ambient, so need to revert to false.
    boolean savedSetAmbient = setAmbient;
    super.popStyle();
    if (!savedSetAmbient) setAmbient = false;
  }

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


  @Override
  public void strokeWeight(float weight) {
    this.strokeWeight = weight;
  }


  @Override
  public void strokeJoin(int join) {
    this.strokeJoin = join;
  }


  @Override
  public void strokeCap(int cap) {
    this.strokeCap = cap;
  }


  //////////////////////////////////////////////////////////////

  // FILL COLOR


  @Override
  protected void fillFromCalc() {
    super.fillFromCalc();

    if (!setAmbient) {
      // Setting the ambient color from the current fill
      // is what the old P3D did and allows to have an
      // default ambient color when the user doesn't specify
      // it explicitly.
      ambientFromCalc();
      // ambientFromCalc sets setAmbient to true, but it hasn't been
      // set by the user so put back to false.
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
  @Override
  public void lights() {
    enableLighting();

    // reset number of lights
    lightCount = 0;

    // need to make sure colorMode is RGB 255 here
    int colorModeSaved = colorMode;
    colorMode = RGB;

    lightFalloff(1, 0, 0);
    lightSpecular(0, 0, 0);

    ambientLight(colorModeX * 0.5f, colorModeY * 0.5f, colorModeZ * 0.5f);
    directionalLight(colorModeX * 0.5f, colorModeY * 0.5f, colorModeZ * 0.5f,
                     0, 0, -1);

    colorMode = colorModeSaved;
  }


  /**
   * Disables lighting.
   */
  @Override
  public void noLights() {
    disableLighting();
    lightCount = 0;
  }


  /**
   * Add an ambient light based on the current color mode.
   */
  @Override
  public void ambientLight(float r, float g, float b) {
    ambientLight(r, g, b, 0, 0, 0);
  }


  /**
   * Add an ambient light based on the current color mode. This version includes
   * an (x, y, z) position for situations where the falloff distance is used.
   */
  @Override
  public void ambientLight(float r, float g, float b,
                           float x, float y, float z) {
    enableLighting();
    if (lightCount == PGL.MAX_LIGHTS) {
      throw new RuntimeException("can only create " + PGL.MAX_LIGHTS +
                                 " lights");
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


  @Override
  public void directionalLight(float r, float g, float b,
                               float dx, float dy, float dz) {
    enableLighting();
    if (lightCount == PGL.MAX_LIGHTS) {
      throw new RuntimeException("can only create " + PGL.MAX_LIGHTS +
                                 " lights");
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


  @Override
  public void pointLight(float r, float g, float b,
                         float x, float y, float z) {
    enableLighting();
    if (lightCount == PGL.MAX_LIGHTS) {
      throw new RuntimeException("can only create " + PGL.MAX_LIGHTS +
                                 " lights");
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


  @Override
  public void spotLight(float r, float g, float b,
                        float x, float y, float z,
                        float dx, float dy, float dz,
                        float angle, float concentration) {
    enableLighting();
    if (lightCount == PGL.MAX_LIGHTS) {
      throw new RuntimeException("can only create " + PGL.MAX_LIGHTS +
                                 " lights");
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
  @Override
  public void lightFalloff(float constant, float linear, float quadratic) {
    currentLightFalloffConstant = constant;
    currentLightFalloffLinear = linear;
    currentLightFalloffQuadratic = quadratic;
  }


  /**
   * Set the specular color of the last light created.
   */
  @Override
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


  protected void lightPosition(int num, float x, float y, float z,
                               boolean dir) {
    lightPosition[4 * num + 0] =
      x*modelview.m00 + y*modelview.m01 + z*modelview.m02 + modelview.m03;
    lightPosition[4 * num + 1] =
      x*modelview.m10 + y*modelview.m11 + z*modelview.m12 + modelview.m13;
    lightPosition[4 * num + 2] =
      x*modelview.m20 + y*modelview.m21 + z*modelview.m22 + modelview.m23;

    // Used to inicate if the light is directional or not.
    lightPosition[4 * num + 3] = dir ? 1: 0;
  }


  protected void lightNormal(int num, float dx, float dy, float dz) {
    // Applying normal matrix to the light direction vector, which is the
    // transpose of the inverse of the modelview.
    float nx =
      dx*modelviewInv.m00 + dy*modelviewInv.m10 + dz*modelviewInv.m20;
    float ny =
      dx*modelviewInv.m01 + dy*modelviewInv.m11 + dz*modelviewInv.m21;
    float nz =
      dx*modelviewInv.m02 + dy*modelviewInv.m12 + dz*modelviewInv.m22;

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


  @Override
  protected void backgroundImpl(PImage image) {
    backgroundImpl();
    set(0, 0, image);
    if (0 < parent.frameCount) {
      clearColorBuffer = true;
    }
  }


  @Override
  protected void backgroundImpl() {
    flush();

    pgl.depthMask(true);
    pgl.clearDepth(1);
    pgl.clear(PGL.DEPTH_BUFFER_BIT);
    if (hints[DISABLE_DEPTH_MASK]) {
      pgl.depthMask(false);
    } else {
      pgl.depthMask(true);
    }

    pgl.clearColor(backgroundR, backgroundG, backgroundB, backgroundA);
    pgl.clear(PGL.COLOR_BUFFER_BIT);
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
    if (!hints[DISABLE_OPENGL_ERRORS]) {
      int err = pgl.getError();
      if (err != 0) {
        String errString = pgl.errorString(err);
        String msg = "OpenGL error " + err + " at " + where + ": " + errString;
        PGraphics.showWarning(msg);
      }
    }
  }



  //////////////////////////////////////////////////////////////

  // RENDERER SUPPORT QUERIES

  // public boolean displayable()


  @Override
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
  @Override
  public void loadPixels() {
    if (primarySurface && sized) {
      // Something wrong going on with threading, sized can never be true if
      // all the steps in a resize happen inside the Animation thread.
      return;
    }

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
    }

    if (needEndDraw) {
      endDraw();
    }
  }


  protected void allocatePixels() {
    if ((pixels == null) || (pixels.length != width * height)) {
      pixels = new int[width * height];
      pixelBuffer = PGL.allocateIntBuffer(pixels);
    }
  }


  protected void saveSurfaceToPixels() {
    allocatePixels();
    readPixels();
  }


  protected void restoreSurfaceFromPixels() {
    drawPixels(0, 0, width, height);
  }


  protected void readPixels() {
    beginPixelsOp(OP_READ);
    try {
      // The readPixelsImpl() call in inside a try/catch block because it appears
      // that (only sometimes) JOGL will run beginDraw/endDraw on the EDT
      // thread instead of the Animation thread right after a resize. Because
      // of this the width and height might have a different size than the
      // one of the pixels arrays.
      pgl.readPixelsImpl(0, 0, width, height, PGL.RGBA, PGL.UNSIGNED_BYTE,
                         pixelBuffer);
    } catch (IndexOutOfBoundsException e) {
      // Silently catch the exception.
    }
    endPixelsOp();
    try {
      // Idem...
      PGL.getIntArray(pixelBuffer, pixels);
      PGL.nativeToJavaARGB(pixels, width, height);
    } catch (ArrayIndexOutOfBoundsException e) {
    }
  }


  protected void drawPixels(int x, int y, int w, int h) {
    int len = w * h;
    if (nativePixels == null || nativePixels.length < len) {
      nativePixels = new int[len];
      nativePixelBuffer = PGL.allocateIntBuffer(nativePixels);
    }

    try {
      if (0 < x || 0 < y || w < width || h < height) {
        // The pixels to copy to the texture need to be consecutive, and they
        // are not in the pixels array, so putting each row one after another
        // in nativePixels.
        int offset0 = y * width + x;
        int offset1 = 0;

        for (int yc = y; yc < y + h; yc++) {
          System.arraycopy(pixels, offset0, nativePixels, offset1, w);
          offset0 += width;
          offset1 += w;
        }
      } else {
        PApplet.arrayCopy(pixels, 0, nativePixels, 0, len);
      }
      PGL.javaToNativeARGB(nativePixels, w, h);
    } catch (ArrayIndexOutOfBoundsException e) {
    }
    PGL.putIntArray(nativePixelBuffer, nativePixels);
    // Copying pixel buffer to screen texture...
    if (primarySurface && !pgl.isFBOBacked()) {
      // First making sure that the screen texture is valid. Only in the case
      // of non-FBO-backed primary surface we might need to create the texture.
      loadTextureImpl(POINT, false);
    }

    boolean needToDrawTex = primarySurface && (!pgl.isFBOBacked() ||
                            (pgl.isFBOBacked() && pgl.isMultisampled())) ||
                            offscreenMultisample;
    if (needToDrawTex) {
      // The texture to screen needs to be drawn only if we are on the primary
      // surface w/out FBO-layer, or with FBO-layer and multisampling. Or, we
      // are doing multisampled offscreen. Why? Because in the case of
      // non-multisampled FBO, texture is actually the color buffer used by the
      // color FBO, so with the copy operation we should be done updating the
      // (off)screen buffer.

      // First, copy the pixels to the texture. We don't need to invert the
      // pixel copy because the texture will be drawn inverted.

      pgl.copyToTexture(texture.glTarget, texture.glFormat, texture.glName,
                        x, y, w, h, nativePixelBuffer);
      beginPixelsOp(OP_WRITE);
      drawTexture(x, y, w, h);
      endPixelsOp();
    } else {
      // We only need to copy the pixels to the back texture where we are
      // currently drawing to. Because the texture is invertex along Y, we
      // need to reflect that in the vertical arguments.
      pgl.copyToTexture(texture.glTarget, texture.glFormat, texture.glName,
                        x, height - (y + h), w, h, nativePixelBuffer);
    }
  }


  //////////////////////////////////////////////////////////////

  // GET/SET PIXELS


  @Override
  public int get(int x, int y) {
    loadPixels();
    setgetPixels = true;
    return super.get(x, y);
  }


  @Override
  protected void getImpl(int sourceX, int sourceY,
                         int sourceWidth, int sourceHeight,
                         PImage target, int targetX, int targetY) {
    loadPixels();
    setgetPixels = true;
    super.getImpl(sourceX, sourceY, sourceWidth, sourceHeight,
                  target, targetX, targetY);
  }


  @Override
  public void set(int x, int y, int argb) {
    loadPixels();
    setgetPixels = true;
    super.set(x, y, argb);
  }


  @Override
  protected void setImpl(PImage sourceImage,
                         int sourceX, int sourceY,
                         int sourceWidth, int sourceHeight,
                         int targetX, int targetY) {
    loadPixels();
    setgetPixels = true;
    super.setImpl(sourceImage, sourceX, sourceY, sourceWidth, sourceHeight,
                  targetX, targetY);
  }


  //////////////////////////////////////////////////////////////

  // LOAD/UPDATE TEXTURE


  // Loads the current contents of the renderer's drawing surface into the
  // its texture.
  public void loadTexture() {
    boolean needEndDraw = false;
    if (!drawing) {
      beginDraw();
      needEndDraw = true;
    }

    flush(); // To make sure the color buffer is updated.

    if (primarySurface) {
      if (pgl.isFBOBacked()) {
        // In the case of MSAA, this is needed so the back buffer is in sync
        // with the rendering.
        pgl.syncBackTexture();
      } else {
        loadTextureImpl(Texture.POINT, false);

        // Here we go the slow route: we first copy the contents of the color
        // buffer into a pixels array (but we keep it in native format) and
        // then copy this array into the texture.
        if (nativePixels == null || nativePixels.length < width * height) {
          nativePixels = new int[width * height];
          nativePixelBuffer = PGL.allocateIntBuffer(nativePixels);
        }

        beginPixelsOp(OP_READ);
        try {
          // See comments in readPixels() for the reason for this try/catch.
          pgl.readPixelsImpl(0, 0, width, height, PGL.RGBA, PGL.UNSIGNED_BYTE,
                             nativePixelBuffer);
        } catch (IndexOutOfBoundsException e) {
        }
        endPixelsOp();

        texture.setNative(nativePixelBuffer, 0, 0, width, height);
      }
    } else if (offscreenMultisample) {
       // We need to copy the contents of the multisampled buffer to the color
       // buffer, so the later is up-to-date with the last drawing.
       multisampleFramebuffer.copy(offscreenFramebuffer, currentFramebuffer);
    }

    if (needEndDraw) {
      endDraw();
    }
  }


  // Just marks the whole texture as updated
  public void updateTexture() {
    texture.updateTexels();
  }


  // Marks the specified rectanglular subregion in the texture as
  // updated.
  public void updateTexture(int x, int y, int w, int h) {
    texture.updateTexels(x, y, w, h);
  }


  // Draws wherever it is in the screen texture right now to the display.
  public void updateDisplay() {
    flush();
    beginPixelsOp(OP_WRITE);
    drawTexture();
    endPixelsOp();
  }


  protected void loadTextureImpl(int sampling, boolean mipmap) {
    if (width == 0 || height == 0) return;
    if (texture == null || texture.contextIsOutdated()) {
      Texture.Parameters params = new Texture.Parameters(ARGB,
                                                         sampling, mipmap);
      texture = new Texture(width, height, params);
      texture.invertedY(true);
      texture.colorBuffer(true);
      pgPrimary.setCache(this, texture);
    }
  }


  protected void createPTexture() {
    ptexture = new Texture(width, height, texture.getParameters());
    ptexture.invertedY(true);
    ptexture.colorBuffer(true);
  }


  protected void swapOffscreenTextures() {
    if (ptexture != null) {
      int temp = texture.glName;
      texture.glName = ptexture.glName;
      ptexture.glName = temp;
      offscreenFramebuffer.setColorBuffer(texture);
    }
  }


  protected void drawTexture() {
    // No blend so the texure replaces wherever is on the screen,
    // irrespective of the alpha
    pgl.disable(PGL.BLEND);
    pgl.drawTexture(texture.glTarget, texture.glName,
                    texture.glWidth, texture.glHeight,
                    0, 0, width, height);
    pgl.enable(PGL.BLEND);
  }


  protected void drawTexture(int x, int y, int w, int h) {
    // Processing Y axis is inverted with respect to OpenGL, so we need to
    // invert the y coordinates of the screen rectangle.
    pgl.disable(PGL.BLEND);
    pgl.drawTexture(texture.glTarget, texture.glName,
                    texture.glWidth, texture.glHeight, width, height,
                    x, y, x + w, y + h,
                    x, height - (y + h), x + w, height - y);
    pgl.enable(PGL.BLEND);
  }


  protected void drawPTexture() {
    if (ptexture != null) {
      // No blend so the texure replaces wherever is on the screen,
      // irrespective of the alpha
      pgl.disable(PGL.BLEND);
      pgl.drawTexture(ptexture.glTarget, ptexture.glName,
                      ptexture.glWidth, ptexture.glHeight,
                      0, 0, width, height);
      pgl.enable(PGL.BLEND);
    }
  }


  //////////////////////////////////////////////////////////////

  // MASK


//  @Override
//  public void mask(int alpha[]) {
//    PImage temp = get();
//    temp.mask(alpha);
//    set(0, 0, temp);
//  }


  @Override
  public void mask(PImage alpha) {
    if (alpha.width != width || alpha.height != height) {
      throw new RuntimeException("The PImage used with mask() must be " +
      "the same size as the applet.");
    }

    if (maskShader == null) {
      maskShader = new PShader(parent, defTextureShaderVertURL,
                                       maskShaderFragURL);
    }
    maskShader.set("mask", alpha);
    filter(maskShader);
  }



  //////////////////////////////////////////////////////////////

  // FILTER


  /**
   * This is really inefficient and not a good idea in OpenGL. Use get() and
   * set() with a smaller image area, or call the filter on an image instead,
   * and then draw that.
   */
  @Override
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
  @Override
  public void filter(int kind, float param) {
    PImage temp = get();
    temp.filter(kind, param);
    set(0, 0, temp);
  }


  @Override
  public void filter(PShader shader) {
    if (!shader.isPolyShader() || !shader.supportsTexturing()) {
      PGraphics.showWarning(INVALID_FILTER_SHADER_ERROR);
      return;
    }

    boolean needEndDraw = false;
    if (primarySurface) pgl.requestFBOLayer();
    else if (!drawing) {
      beginDraw();
      needEndDraw = true;
    }
    loadTexture();

    if (filterTexture == null || filterTexture.contextIsOutdated()) {
      filterTexture = new Texture(texture.width, texture.height,
                                  texture.getParameters());
      filterTexture.invertedY(true);
      filterImage = wrapTexture(filterTexture);
    }
    filterTexture.set(texture);

    // Disable writing to the depth buffer, so that after applying the filter we
    // can still use the depth information to keep adding geometry to the scene.
    pgl.depthMask(false);
    // Also disabling depth testing so the texture is drawn on top of everything
    // that has been drawn before.
    pgl.disable(PGL.DEPTH_TEST);

    // Drawing a textured quad in 2D, covering the entire screen,
    // with the filter shader applied to it:
    begin2D();

    // Changing light configuration and shader after begin2D()
    // because it calls flush().
    boolean prevLights = lights;
    lights = false;
    int prevTextureMode = textureMode;
    textureMode = NORMAL;
    boolean prevStroke = stroke;
    stroke = false;
    int prevBlendMode = blendMode;
    blendMode(REPLACE);
    PShader prevShader = polyShader;
    polyShader = shader;

    beginShape(QUADS);
    texture(filterImage);
    vertex(0, 0, 0, 0);
    vertex(width, 0, 1, 0);
    vertex(width, height, 1, 1);
    vertex(0, height, 0, 1);
    endShape();
    end2D();

    // Restoring previous configuration.
    polyShader = prevShader;
    stroke = prevStroke;
    lights = prevLights;
    textureMode = prevTextureMode;
    blendMode(prevBlendMode);

    if (!hints[DISABLE_DEPTH_TEST]) {
      pgl.enable(PGL.DEPTH_TEST);
    }
    if (!hints[DISABLE_DEPTH_MASK]) {
      pgl.depthMask(true);
    }

    if (needEndDraw) {
      endDraw();
    }
  }


  //////////////////////////////////////////////////////////////

  // COPY


  @Override
  public void copy(int sx, int sy, int sw, int sh,
                   int dx, int dy, int dw, int dh) {
    if (primarySurface) pgl.requestFBOLayer();
    loadTexture();
    if (filterTexture == null || filterTexture.contextIsOutdated()) {
      filterTexture = new Texture(texture.width, texture.height,
                                  texture.getParameters());
      filterTexture.invertedY(true);
      filterImage = wrapTexture(filterTexture);
    }
    filterTexture.put(texture, sx, height - (sy + sh), sw, height - sy);
    copy(filterImage, sx, sy, sw, sh, dx, dy, dw, dh);
  }


  @Override
  public void copy(PImage src,
                   int sx, int sy, int sw, int sh,
                   int dx, int dy, int dw, int dh) {
    boolean needEndDraw = false;
    if (!drawing) {
      beginDraw();
      needEndDraw = true;
    }

    flush(); // make sure that the screen contents are up to date.

    Texture tex = getTexture(src);
    pgl.drawTexture(tex.glTarget, tex.glName,
                    tex.glWidth, tex.glHeight, width, height,
                    sx, tex.height - (sy + sh),
                    sx + sw, tex.height - sy,
                    dx, height - (dy + dh),
                    dx + dw, height - dy);

    if (needEndDraw) {
      endDraw();
    }
  }


  //////////////////////////////////////////////////////////////

  // BLEND


  /**
   * Allows to set custom blend modes for the entire scene, using openGL.
   * Reference article about blending modes:
   * http://www.pegtop.net/delphi/articles/blendmodes/
   * DIFFERENCE, HARD_LIGHT, SOFT_LIGHT, OVERLAY, DODGE, BURN modes cannot be
   * implemented in fixed-function pipeline because they require
   * conditional blending and non-linear blending equations.
   */
  @Override
  protected void blendModeImpl() {
    if (blendMode != lastBlendMode) {
      // Flush any geometry that uses a different blending mode.
      flush();
    }

    pgl.enable(PGL.BLEND);

    if (blendMode == REPLACE) {
      if (blendEqSupported) {
        pgl.blendEquation(PGL.FUNC_ADD);
      }
      pgl.blendFunc(PGL.ONE, PGL.ZERO);

    } else if (blendMode == BLEND) {
      if (blendEqSupported) {
        pgl.blendEquation(PGL.FUNC_ADD);
      }
      pgl.blendFunc(PGL.SRC_ALPHA, PGL.ONE_MINUS_SRC_ALPHA);

    } else if (blendMode == ADD) {
      if (blendEqSupported) {
        pgl.blendEquation(PGL.FUNC_ADD);
      }
      pgl.blendFunc(PGL.SRC_ALPHA, PGL.ONE);

    } else if (blendMode == SUBTRACT) {
      if (blendEqSupported) {
        pgl.blendEquation(PGL.FUNC_REVERSE_SUBTRACT);
        pgl.blendFunc(PGL.ONE, PGL.SRC_ALPHA);
      } else {
        PGraphics.showWarning(BLEND_DRIVER_ERROR, "SUBTRACT");
      }

    } else if (blendMode == LIGHTEST) {
      if (blendEqSupported) {
        pgl.blendEquation(PGL.FUNC_MAX);
        pgl.blendFunc(PGL.SRC_ALPHA, PGL.DST_ALPHA);
      } else {
        PGraphics.showWarning(BLEND_DRIVER_ERROR, "LIGHTEST");
      }

    } else if (blendMode == DARKEST) {
      if (blendEqSupported) {
        pgl.blendEquation(PGL.FUNC_MIN);
        pgl.blendFunc(PGL.SRC_ALPHA, PGL.DST_ALPHA);
      } else {
        PGraphics.showWarning(BLEND_DRIVER_ERROR, "DARKEST");
      }

    } else if (blendMode == EXCLUSION) {
      if (blendEqSupported) {
        pgl.blendEquation(PGL.FUNC_ADD);
      }
      pgl.blendFunc(PGL.ONE_MINUS_DST_COLOR, PGL.ONE_MINUS_SRC_COLOR);

    } else if (blendMode == MULTIPLY) {
      if (blendEqSupported) {
        pgl.blendEquation(PGL.FUNC_ADD);
      }
      pgl.blendFunc(PGL.DST_COLOR, PGL.SRC_COLOR);

    } else if (blendMode == SCREEN) {
      if (blendEqSupported) {
        pgl.blendEquation(PGL.FUNC_ADD);
      }
      pgl.blendFunc(PGL.ONE_MINUS_DST_COLOR, PGL.ONE);

    } else if (blendMode == DIFFERENCE) {
      PGraphics.showWarning(BLEND_RENDERER_ERROR, "DIFFERENCE");

    } else if (blendMode == OVERLAY) {
      PGraphics.showWarning(BLEND_RENDERER_ERROR, "OVERLAY");

    } else if (blendMode == HARD_LIGHT) {
      PGraphics.showWarning(BLEND_RENDERER_ERROR, "HARD_LIGHT");

    } else if (blendMode == SOFT_LIGHT) {
      PGraphics.showWarning(BLEND_RENDERER_ERROR, "SOFT_LIGHT");

    } else if (blendMode == DODGE) {
      PGraphics.showWarning(BLEND_RENDERER_ERROR, "DODGE");

    } else if (blendMode == BURN) {
      PGraphics.showWarning(BLEND_RENDERER_ERROR, "BURN");
    }
    lastBlendMode = blendMode;
  }


  //////////////////////////////////////////////////////////////

  // SAVE

  // public void save(String filename) // PImage calls loadPixels()


  //////////////////////////////////////////////////////////////

  // TEXTURE UTILS


  /**
   * Not an approved function, this will change or be removed in the future.
   * This utility method returns the texture associated to the renderer's.
   * drawing surface, making sure is updated to reflect the current contents
   * off the screen (or offscreen drawing surface).
   */
  public Texture getTexture() {
    loadTexture();
    return texture;
  }


  /**
   * Not an approved function, this will change or be removed in the future.
   * This utility method returns the texture associated to the image.
   * creating and/or updating it if needed.
   *
   * @param img the image to have a texture metadata associated to it
   */
  public Texture getTexture(PImage img) {
    Texture tex = (Texture)initCache(img);
    if (tex == null) return null;

    if (img.isModified() || img.isLoaded()) {
      if (img.width != tex.width || img.height != tex.height) {
        tex.init(img.width, img.height);
      }
      updateTexture(img, tex);
    }

    if (tex.hasBuffers()) {
      tex.bufferUpdate();
    }

    checkTexture(tex);

    return tex;
  }


  protected Object initCache(PImage img) {
    if (!checkGLThread()) {
      return null;
    }

    Texture tex = (Texture)pgPrimary.getCache(img);
    if (tex == null || tex.contextIsOutdated()) {
      tex = addTexture(img);
      if (tex != null) {
        img.loadPixels();
        tex.set(img.pixels, img.format);
        img.setLoaded(false);
      }
    }
    return tex;
  }


  protected void bindFrontTexture() {
    if (primarySurface) {
      pgl.bindFrontTexture();
    } else {
      if (ptexture == null) createPTexture();
      ptexture.bind();
    }
  }


  protected void unbindFrontTexture() {
    if (primarySurface) {
      pgl.unbindFrontTexture();
    } else {
      ptexture.unbind();
    }
  }


  /**
   * This utility method creates a texture for the provided image, and adds it
   * to the metadata cache of the image.
   * @param img the image to have a texture metadata associated to it
   */
  protected Texture addTexture(PImage img) {
    Texture.Parameters params =
      new Texture.Parameters(ARGB, textureSampling,
                             getHint(ENABLE_TEXTURE_MIPMAPS), textureWrap);
    return addTexture(img, params);
  }


  protected Texture addTexture(PImage img, Texture.Parameters params) {
    if (img.width == 0 || img.height == 0) {
      // Cannot add textures of size 0
      return null;
    }
    if (img.parent == null) {
      img.parent = parent;
    }
    Texture tex = new Texture(img.width, img.height, params);
    pgPrimary.setCache(img, tex);
    return tex;
  }


  protected void checkTexture(Texture tex) {
    if (!tex.colorBuffer() &&
        tex.usingMipmaps == hints[DISABLE_TEXTURE_MIPMAPS]) {
      if (hints[DISABLE_TEXTURE_MIPMAPS]) {
        tex.usingMipmaps(false, textureSampling);
      } else {
        tex.usingMipmaps(true, textureSampling);
      }
    }

    if ((tex.usingRepeat && textureWrap == CLAMP) ||
        (!tex.usingRepeat && textureWrap == REPEAT)) {
      if (textureWrap == CLAMP) {
        tex.usingRepeat(false);
      } else {
        tex.usingRepeat(true);
      }
    }
  }


  protected PImage wrapTexture(Texture tex) {
    // We don't use the PImage(int width, int height, int mode) constructor to
    // avoid initializing the pixels array.
    PImage img = new PImage();
    img.parent = parent;
    img.width = tex.width;
    img.height = tex.height;
    img.format = ARGB;
    pgPrimary.setCache(img, tex);
    return img;
  }


  protected void updateTexture(PImage img, Texture tex) {
    if (tex != null) {
      int x = img.getModifiedX1();
      int y = img.getModifiedY1();
      int w = img.getModifiedX2() - x;
      int h = img.getModifiedY2() - y;
      tex.set(img.pixels, x, y, w, h, img.format);
    }
    img.setModified(false);
    img.setLoaded(false);
  }


  protected void deleteSurfaceTextures() {
    if (texture != null) {
      texture.dispose();
    }

    if (ptexture != null) {
      ptexture.dispose();
    }

    if (filterTexture != null) {
      filterTexture.dispose();
    }
  }


  protected boolean checkGLThread() {
    if (pgl.threadIsCurrent()) {
      return true;
    } else {
      PGraphics.showWarning(OPENGL_THREAD_ERROR);
      return false;
    }
  }


  //////////////////////////////////////////////////////////////

  // RESIZE


  @Override
  public void resize(int wide, int high) {
    PGraphics.showMethodWarning("resize");
  }


  //////////////////////////////////////////////////////////////

  // INITIALIZATION ROUTINES


  protected void initPrimary() {
    pgl.initSurface(quality);
    if (texture != null) {
      pgPrimary.removeCache(this);
      texture = ptexture = null;
    }
    pgPrimary = this;
    initialized = true;
  }


  protected void reinitPrimary() {
    allocate();

    // init perspective projection based on new dimensions
    cameraFOV = 60 * DEG_TO_RAD; // at least for now
    cameraX = width / 2.0f;
    cameraY = height / 2.0f;
    cameraZ = cameraY / ((float) Math.tan(cameraFOV / 2.0f));
    cameraNear = cameraZ / 10.0f;
    cameraFar = cameraZ * 10.0f;
    cameraAspect = (float) width / (float) height;

    // Forces a restart of OpenGL so the canvas has the right size.
    // restartPGL();

    // System.out.println("hasbeenSized");

    pgl.reinitSurface();
  }


  protected void beginOnscreenDraw() {
    pgl.beginDraw(clearColorBuffer);

    if (drawFramebuffer == null) {
      drawFramebuffer = new FrameBuffer(width, height, true);
    }
    drawFramebuffer.setFBO(pgl.getDrawFramebuffer());
    if (readFramebuffer == null) {
      readFramebuffer = new FrameBuffer(width, height, true);
    }
    readFramebuffer.setFBO(pgl.getReadFramebuffer());
    if (currentFramebuffer == null) {
      setFramebuffer(drawFramebuffer);
    }

    if (pgl.isFBOBacked()) {
      texture = pgl.wrapBackTexture(texture);
      ptexture = pgl.wrapFrontTexture(ptexture);
    }
  }


  protected void endOnscreenDraw() {
    pgl.endDraw(clearColorBuffer0);
  }


  protected void initOffscreen() {
    // Getting the context and capabilities from the main renderer.
    loadTextureImpl(Texture.BILINEAR, false);

    // In case of reinitialization (for example, when the smooth level
    // is changed), we make sure that all the OpenGL resources associated
    // to the surface are released by calling delete().
    if (offscreenFramebuffer != null) {
      offscreenFramebuffer.dispose();
    }
    if (multisampleFramebuffer != null) {
      multisampleFramebuffer.dispose();
    }

    boolean packed = depthBits == 24 && stencilBits == 8 &&
                     packedDepthStencilSupported;
    if (PGraphicsOpenGL.fboMultisampleSupported && 1 < quality) {
      multisampleFramebuffer =
        new FrameBuffer(texture.glWidth, texture.glHeight, quality, 0,
                        depthBits, stencilBits, packed, false);

      multisampleFramebuffer.clear();
      offscreenMultisample = true;

      // The offscreen framebuffer where the multisampled image is finally drawn
      // to doesn't need depth and stencil buffers since they are part of the
      // multisampled framebuffer.
      offscreenFramebuffer =
        new FrameBuffer(texture.glWidth, texture.glHeight, 1, 1, 0, 0,
                        false, false);

    } else {
      quality = 0;
      offscreenFramebuffer =
        new FrameBuffer(texture.glWidth, texture.glHeight, 1, 1,
                        depthBits, stencilBits, packed, false);
      offscreenMultisample = false;
    }

    offscreenFramebuffer.setColorBuffer(texture);
    offscreenFramebuffer.clear();

    initialized = true;
  }


  protected void beginOffscreenDraw() {
    if (!initialized) {
      initOffscreen();
    } else {
      boolean outdated = offscreenFramebuffer != null &&
                         offscreenFramebuffer.contextIsOutdated();
      boolean outdatedMulti = multisampleFramebuffer != null &&
        multisampleFramebuffer.contextIsOutdated();
      if (outdated || outdatedMulti) {
        restartPGL();
        initOffscreen();
      } else {
        // The back texture of the past frame becomes the front,
        // and the front texture becomes the new back texture where the
        // new frame is drawn to.
        swapOffscreenTextures();
      }
    }

    pushFramebuffer();
    if (offscreenMultisample) {
      setFramebuffer(multisampleFramebuffer);
    } else {
      setFramebuffer(offscreenFramebuffer);
    }

    // Render previous back texture (now is the front) as background
    drawPTexture();

    // Restoring the clipping configuration of the offscreen surface.
    if (clip) {
      pgl.enable(PGL.SCISSOR_TEST);
      pgl.scissor(clipRect[0], clipRect[1], clipRect[2], clipRect[3]);
    } else {
      pgl.disable(PGL.SCISSOR_TEST);
    }
  }


  protected void endOffscreenDraw() {
    if (offscreenMultisample) {
      multisampleFramebuffer.copy(offscreenFramebuffer, currentFramebuffer);
    }

    popFramebuffer();
    texture.updateTexels(); // Mark all texels in screen texture as modified.

    pgPrimary.restoreGL();
  }


  protected void setViewport() {
    viewport.put(0, 0); viewport.put(1, 0);
    viewport.put(2, width); viewport.put(3, height);
    pgl.viewport(viewport.get(0), viewport.get(1),
                 viewport.get(2), viewport.get(3));
  }


  protected void setDrawDefaults() {
    inGeo.clear();
    tessGeo.clear();
    texCache.clear();

    // Each frame starts with textures disabled.
    super.noTexture();

    // Making sure that OpenGL is using the last blend mode set by the user.
    blendModeImpl();

    // this is necessary for 3D drawing
    if (hints[DISABLE_DEPTH_TEST]) {
      pgl.disable(PGL.DEPTH_TEST);
    } else {
      pgl.enable(PGL.DEPTH_TEST);
    }
    // use <= since that's what processing.core does
    pgl.depthFunc(PGL.LEQUAL);

    if (hints[DISABLE_OPTIMIZED_STROKE]) {
      flushMode = FLUSH_CONTINUOUSLY;
    } else {
      flushMode = FLUSH_WHEN_FULL;
    }

    if (primarySurface) {
      pgl.getIntegerv(PGL.SAMPLES, intBuffer);
      int temp = intBuffer.get(0);
      if (quality != temp && 1 < temp && 1 < quality) {
        quality = temp;
      }
    }
    if (quality < 2) {
      pgl.disable(PGL.MULTISAMPLE);
    } else {
      pgl.enable(PGL.MULTISAMPLE);
    }
    pgl.disable(PGL.POINT_SMOOTH);
    pgl.disable(PGL.LINE_SMOOTH);
    pgl.disable(PGL.POLYGON_SMOOTH);

    if (sized) {
      //reapplySettings();

      // To avoid having garbage in the screen after a resize,
      // in the case background is not called in draw().
      background(backgroundColor);

      // Sets the default projection and camera (initializes modelview).
      // If the user has setup up their own projection, they'll need
      // to fix it after resize anyway. This helps the people who haven't
      // set up their own projection.
      defaultPerspective();
      defaultCamera();

      // clear the flag
      sized = false;
    } else {
      // Eliminating any user's transformations by going back to the
      // original camera setup.
      modelview.set(camera);
      modelviewInv.set(cameraInv);
      updateProjmodelview();
    }

    if (is3D()) {
      noLights();
      lightFalloff(1, 0, 0);
      lightSpecular(0, 0, 0);
    }

    // Because y is flipped, the vertices that should be specified by
    // the user in CCW order to define a front-facing facet, end up being CW.
    pgl.frontFace(PGL.CW);
    pgl.disable(PGL.CULL_FACE);

    // Processing uses only one texture unit.
    pgl.activeTexture(PGL.TEXTURE0);

    // The current normal vector is set to be parallel to the Z axis.
    normalX = normalY = normalZ = 0;

    // Clear depth and stencil buffers.
    pgl.depthMask(true);
    pgl.clearDepth(1);
    pgl.clearStencil(0);
    pgl.clear(PGL.DEPTH_BUFFER_BIT | PGL.STENCIL_BUFFER_BIT);

    if (!settingsInited) {
      defaultSettings();
    }

    if (restoreSurface) {
      restoreSurfaceFromPixels();
      //if (1 < parent.frameCount) {
      restoreSurface = false;
      //}
    }

    if (hints[DISABLE_DEPTH_MASK]) {
      pgl.depthMask(false);
    } else {
      pgl.depthMask(true);
    }

    pixelsOp = OP_NONE;

    clearColorBuffer0 = clearColorBuffer;
    clearColorBuffer = false;

    modified = false;
    setgetPixels = false;
  }


  protected void getGLParameters() {
    OPENGL_VENDOR     = pgl.getString(PGL.VENDOR);
    OPENGL_RENDERER   = pgl.getString(PGL.RENDERER);
    OPENGL_VERSION    = pgl.getString(PGL.VERSION);
    OPENGL_EXTENSIONS = pgl.getString(PGL.EXTENSIONS);
    GLSL_VERSION      = pgl.getString(PGL.SHADING_LANGUAGE_VERSION);

    npotTexSupported =
      -1 < OPENGL_EXTENSIONS.indexOf("_texture_non_power_of_two");
    autoMipmapGenSupported =
      -1 < OPENGL_EXTENSIONS.indexOf("_generate_mipmap");
    fboMultisampleSupported =
      -1 < OPENGL_EXTENSIONS.indexOf("_framebuffer_multisample");
    packedDepthStencilSupported =
      -1 < OPENGL_EXTENSIONS.indexOf("_packed_depth_stencil");
    anisoSamplingSupported =
      -1 < OPENGL_EXTENSIONS.indexOf("_texture_filter_anisotropic");

    try {
      pgl.blendEquation(PGL.FUNC_ADD);
      blendEqSupported = true;
    } catch (Exception e) {
      blendEqSupported = false;
    }

    depthBits = pgl.getDepthBits();
    stencilBits = pgl.getStencilBits();

    pgl.getIntegerv(PGL.MAX_TEXTURE_SIZE, intBuffer);
    maxTextureSize = intBuffer.get(0);

    pgl.getIntegerv(PGL.MAX_SAMPLES, intBuffer);
    maxSamples = intBuffer.get(0);

    pgl.getIntegerv(PGL.ALIASED_LINE_WIDTH_RANGE, intBuffer);
    maxLineWidth = intBuffer.get(0);

    pgl.getIntegerv(PGL.ALIASED_POINT_SIZE_RANGE, intBuffer);
    maxPointSize = intBuffer.get(0);

    if (anisoSamplingSupported) {
      pgl.getFloatv(PGL.MAX_TEXTURE_MAX_ANISOTROPY, floatBuffer);
      maxAnisoAmount = floatBuffer.get(0);
    }

    glParamsRead = true;
  }


  //////////////////////////////////////////////////////////////

  // SHADER HANDLING


  @Override
  public PShader loadShader(String fragFilename) {
    if (fragFilename == null || fragFilename.equals("")) {
      PGraphics.showWarning(MISSING_FRAGMENT_SHADER);
      return null;
    }

    int type = getShaderType(fragFilename, PShader.POLY);
    PShader shader = new PShader(parent);
    shader.setType(type);
    shader.setFragmentShader(fragFilename);
    if (type == PShader.POINT) {
      shader.setVertexShader(defPointShaderVertURL);
    } else if (type == PShader.LINE) {
      shader.setVertexShader(defLineShaderVertURL);
    } else if (type == PShader.TEXLIGHT) {
      shader.setVertexShader(defTexlightShaderVertURL);
    } else if (type == PShader.LIGHT) {
      shader.setVertexShader(defLightShaderVertURL);
    } else if (type == PShader.TEXTURE) {
      shader.setVertexShader(defTextureShaderVertURL);
    } else if (type == PShader.COLOR) {
      shader.setVertexShader(defColorShaderVertURL);
    } else {
      shader.setVertexShader(defTextureShaderVertURL);
    }
    return shader;
  }


  @Override
  public PShader loadShader(String fragFilename, String vertFilename) {
    if (fragFilename == null || fragFilename.equals("")) {
      PGraphics.showWarning(MISSING_FRAGMENT_SHADER);
      return null;
    } else if (fragFilename == null || fragFilename.equals("")) {
      PGraphics.showWarning(MISSING_VERTEX_SHADER);
      return null;
    } else {
      return new PShader(parent, vertFilename, fragFilename);
    }
  }


  @Override
  public void shader(PShader shader) {
    flush(); // Flushing geometry drawn with a different shader.

    if (shader.isPolyShader()) polyShader = shader;
    else if (shader.isLineShader()) lineShader = shader;
    else if (shader.isPointShader()) pointShader = shader;
    else PGraphics.showWarning(UNKNOWN_SHADER_KIND_ERROR);
  }


  @Override
  // TODO: deprecate this method, the kind arguments is not used anymore
  public void shader(PShader shader, int kind) {
    shader(shader);
  }


  @Override
  public void resetShader() {
    resetShader(TRIANGLES);
  }


  @Override
  public void resetShader(int kind) {
    flush(); // Flushing geometry drawn with a different shader.

    if (kind == TRIANGLES || kind == QUADS || kind == POLYGON) {
      polyShader = null;
    } else if (kind == LINES) {
      lineShader = null;
    } else if (kind == POINTS) {
      pointShader = null;
    } else {
      PGraphics.showWarning(UNKNOWN_SHADER_KIND_ERROR);
    }
  }


  protected int getShaderType(String filename, int defaulType) {
    String[] source = parent.loadStrings(filename);
    return getShaderTypeImpl(source, defaulType);
  }


  protected int getShaderType(URL url, int defaultType) throws IOException {
    String[] source = PApplet.loadStrings(url.openStream());
    return getShaderTypeImpl(source, defaultType);
  }


  protected int getShaderTypeImpl(String[] source, int defaultType) {
    for (int i = 0; i < source.length; i++) {
      String line = source[i].trim();
      if (PApplet.match(line, pointShaderAttrRegexp) != null)
        return PShader.POINT;
      else if (PApplet.match(line, lineShaderAttrRegexp) != null)
        return PShader.LINE;
      else if (PApplet.match(line, pointShaderDefRegexp) != null)
        return PShader.POINT;
      else if (PApplet.match(line, lineShaderDefRegexp) != null)
        return PShader.LINE;
      else if (PApplet.match(line, colorShaderDefRegexp) != null)
        return PShader.COLOR;
      else if (PApplet.match(line, lightShaderDefRegexp) != null)
        return PShader.LIGHT;
      else if (PApplet.match(line, texShaderDefRegexp) != null)
        return PShader.TEXTURE;
      else if (PApplet.match(line, texlightShaderDefRegexp) != null)
        return PShader.TEXLIGHT;
      else if (PApplet.match(line, polyShaderDefRegexp) != null)
        return PShader.POLY;
      else if (PApplet.match(line, triShaderAttrRegexp) != null)
        return PShader.POLY;
      else if (PApplet.match(line, quadShaderAttrRegexp) != null)
        return PShader.POLY;
    }
    return defaultType;
  }


  protected void deleteDefaultShaders() {
    // The default shaders contains references to the PGraphics object that
    // creates them, so when restarting the renderer, those references should
    // dissapear.
    defColorShader = null;
    defTextureShader = null;
    defLightShader = null;
    defTexlightShader = null;
    defLineShader = null;
    defPointShader = null;
    maskShader = null;
  }


  protected PShader getPolyShader(boolean lit, boolean tex) {
    PShader shader;
    boolean useDefault = polyShader == null;
    if (polyShader != null) {
      polyShader.setRenderer(this);
      polyShader.loadAttributes();
      polyShader.loadUniforms();
    }
    if (lit) {
      if (tex) {
        if (useDefault || !polyShader.checkPolyType(PShader.TEXLIGHT)) {
          if (defTexlightShader == null) {
            defTexlightShader = new PShader(parent,
                                            defTexlightShaderVertURL,
                                            defTextureShaderFragURL);
          }
          shader = defTexlightShader;
        } else {
          shader = polyShader;
        }
      } else {
        if (useDefault || !polyShader.checkPolyType(PShader.LIGHT)) {
          if (defLightShader == null) {
            defLightShader = new PShader(parent,
                                         defLightShaderVertURL,
                                         defColorShaderFragURL);
          }
          shader = defLightShader;
        } else {
          shader = polyShader;
        }
      }
    } else {
      if (polyShader != null && polyShader.accessLightAttribs()) {
        PGraphics.showWarning(SHADER_NEED_LIGHT_ATTRIBS);
        useDefault = true;
      }

      if (tex) {
        if (useDefault || !polyShader.checkPolyType(PShader.TEXTURE)) {
          if (defTextureShader == null) {
            defTextureShader = new PShader(parent,
                                           defTextureShaderVertURL,
                                           defTextureShaderFragURL);
          }
          shader = defTextureShader;
        } else {
          shader = polyShader;
        }
      } else {
        if (useDefault || !polyShader.checkPolyType(PShader.COLOR)) {
          if (defColorShader == null) {
            defColorShader = new PShader(parent,
                                         defColorShaderVertURL,
                                         defColorShaderFragURL);
          }
          shader = defColorShader;
        } else {
          shader = polyShader;
        }
      }
    }
    if (shader != polyShader) {
      shader.setRenderer(this);
      shader.loadAttributes();
      shader.loadUniforms();
    }
    return shader;
  }


  protected PShader getLineShader() {
    PShader shader;
    if (lineShader == null) {
      if (defLineShader == null) {
        defLineShader = new PShader(parent, defLineShaderVertURL,
                                            defLineShaderFragURL);
      }
      shader = defLineShader;
    } else {
      shader = lineShader;
    }
    shader.setRenderer(this);
    shader.loadAttributes();
    shader.loadUniforms();
    return shader;
  }


  protected PShader getPointShader() {
    PShader shader;
    if (pointShader == null) {
      if (defPointShader == null) {
        defPointShader = new PShader(parent, defPointShaderVertURL,
                                             defPointShaderFragURL);
      }
      shader = defPointShader;
    } else {
      shader = pointShader;
    }
    shader.setRenderer(this);
    shader.loadAttributes();
    shader.loadUniforms();
    return shader;
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


  static protected InGeometry newInGeometry(PGraphicsOpenGL pg, int mode) {
    return new InGeometry(pg, mode);
  }


  static protected TessGeometry newTessGeometry(PGraphicsOpenGL pg, int mode) {
    return new TessGeometry(pg, mode);
  }


  static protected TexCache newTexCache() {
    return new TexCache();
  }


  // Holds an array of textures and the range of vertex
  // indices each texture applies to.
  static protected class TexCache {
    int size;
    PImage[] textures;
    int[] firstIndex;
    int[] lastIndex;
    int[] firstCache;
    int[] lastCache;
    boolean hasTextures;

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
      hasTextures = false;
    }

    void clear() {
      java.util.Arrays.fill(textures, 0, size, null);
      size = 0;
      hasTextures = false;
    }

    boolean containsTexture(PImage img) {
      for (int i = 0; i < size; i++) {
        if (textures[i] == img) return true;
      }
      return false;
    }

    PImage getTextureImage(int i) {
      return textures[i];
    }

    Texture getTexture(int i) {
      PImage img = textures[i];
      Texture tex = null;

      if (img != null) {
        tex = pgPrimary.getTexture(img);
      }

      return tex;
    }

    void addTexture(PImage img, int firsti, int firstb, int lasti, int lastb) {
      arrayCheck();

      textures[size] = img;
      firstIndex[size] = firsti;
      lastIndex[size] = lasti;
      firstCache[size] = firstb;
      lastCache[size] = lastb;

      // At least one non-null texture since last reset.
      hasTextures |= img != null;

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
  static protected class IndexCache {
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
  static protected class InGeometry {
    PGraphicsOpenGL pg;
    int renderMode;

    int vertexCount;
    int codeCount;
    int edgeCount;

    float[] vertices;
    int[] colors;
    float[] normals;
    float[] texcoords;
    int[] strokeColors;
    float[] strokeWeights;

    // vertex codes
    int[] codes;

    // Stroke edges
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

    InGeometry(PGraphicsOpenGL pg, int mode) {
      this.pg = pg;
      renderMode = mode;
      allocate();
    }

    // -----------------------------------------------------------------
    //
    // Allocate/dispose

    void clear() {
      vertexCount = 0;
      codeCount = 0;
      edgeCount = 0;
    }

    void clearEdges() {
      edgeCount = 0;
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
      edges = new int[PGL.DEFAULT_IN_EDGES][3];

      clear();
    }

    void vertexCheck() {
      if (vertexCount == vertices.length / 3) {
        int newSize = vertexCount << 1;

        expandVertices(newSize);
        expandColors(newSize);
        expandNormals(newSize);
        expandTexCoords(newSize);
        expandStrokeColors(newSize);
        expandStrokeWeights(newSize);
        expandAmbient(newSize);
        expandSpecular(newSize);
        expandEmissive(newSize);
        expandShininess(newSize);
      }
    }

    void codeCheck() {
      if (codeCount == codes.length) {
        int newLen = codeCount << 1;

        expandCodes(newLen);
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

    int getNumEdgeClosures() {
      int count = 0;
      for (int i = 0; i < edgeCount; i++) {
        if (edges[i][2] == EDGE_CLOSE) count++;
      }
      return count;
    }

    int getNumEdgeVertices(boolean bevel) {
      int segVert = edgeCount;
      int bevVert = 0;
      if (bevel) {
        for (int i = 0; i < edgeCount; i++) {
          int[] edge = edges[i];
          if (edge[2] == EDGE_MIDDLE || edge[2] == EDGE_START) bevVert++;
          if (edge[2] == EDGE_CLOSE) segVert--;
        }
      } else {
        segVert -= getNumEdgeClosures();
      }
      return 4 * segVert + bevVert;
    }

    int getNumEdgeIndices(boolean bevel) {
      int segInd = edgeCount;
      int bevInd = 0;
      if (bevel) {
        for (int i = 0; i < edgeCount; i++) {
          int[] edge = edges[i];
          if (edge[2] == EDGE_MIDDLE || edge[2] == EDGE_START) bevInd++;
          if (edge[2] == EDGE_CLOSE) segInd--;
        }
      } else {
        segInd -= getNumEdgeClosures();
      }
      return 6 * (segInd + bevInd);
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

    void expandTexCoords(int n) {
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

    void expandCodes(int n) {
      int temp[] = new int[n];
      PApplet.arrayCopy(codes, 0, temp, 0, codeCount);
      codes = temp;
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
        trimTexCoords();
        trimStrokeColors();
        trimStrokeWeights();
        trimAmbient();
        trimSpecular();
        trimEmissive();
        trimShininess();
      }

      if (0 < codeCount && codeCount < codes.length) {
        trimCodes();
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

    void trimTexCoords() {
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

    void trimCodes() {
      int temp[] = new int[codeCount];
      PApplet.arrayCopy(codes, 0, temp, 0, codeCount);
      codes = temp;
    }

    void trimEdges() {
      int temp[][] = new int[edgeCount][3];
      PApplet.arrayCopy(edges, 0, temp, 0, edgeCount);
      edges = temp;
    }

    // -----------------------------------------------------------------
    //
    // Vertices

    int addVertex(float x, float y, boolean brk) {
      return addVertex(x, y, VERTEX, brk);
    }

    int addVertex(float x, float y,
                  int code, boolean brk) {
      return addVertex(x, y, 0,
                       fillColor,
                       normalX, normalY, normalZ,
                       0, 0,
                       strokeColor, strokeWeight,
                       ambientColor, specularColor, emissiveColor,
                       shininessFactor,
                       code, brk);
    }

    int addVertex(float x, float y,
                  float u, float v,
                  boolean brk) {
      return addVertex(x, y, u, v, VERTEX, brk);
    }

    int addVertex(float x, float y,
                  float u, float v,
                  int code, boolean brk) {
      return addVertex(x, y, 0,
                       fillColor,
                       normalX, normalY, normalZ,
                       u, v,
                       strokeColor, strokeWeight,
                       ambientColor, specularColor, emissiveColor,
                       shininessFactor,
                       code, brk);
    }

    int addVertex(float x, float y, float z, boolean brk) {
      return addVertex(x, y, z, VERTEX, brk);
    }

    int addVertex(float x, float y, float z,
                  int code, boolean brk) {
      return addVertex(x, y, z,
                       fillColor,
                       normalX, normalY, normalZ,
                       0, 0,
                       strokeColor, strokeWeight,
                       ambientColor, specularColor, emissiveColor,
                       shininessFactor,
                       code, brk);
    }

    int addVertex(float x, float y, float z,
                  float u, float v,
                  boolean brk) {
      return addVertex(x, y, z, u, v, VERTEX, brk);
    }

    int addVertex(float x, float y, float z,
                  float u, float v,
                  int code, boolean brk) {
      return addVertex(x, y, z,
                       fillColor,
                       normalX, normalY, normalZ,
                       u, v,
                       strokeColor, strokeWeight,
                       ambientColor, specularColor, emissiveColor,
                       shininessFactor,
                       code, brk);
    }

    int addVertex(float x, float y, float z,
                  int fcolor,
                  float nx, float ny, float nz,
                  float u, float v,
                  int scolor, float sweight,
                  int am, int sp, int em, float shine,
                  int code, boolean brk) {
      vertexCheck();
      int index;

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

      if (brk || (code == VERTEX && codes != null) ||
          code == BEZIER_VERTEX ||
          code == QUADRATIC_VERTEX ||
          code == CURVE_VERTEX) {
        if (codes == null) {
          codes = new int[PApplet.max(PGL.DEFAULT_IN_VERTICES, vertexCount)];
          Arrays.fill(codes, 0, vertexCount, VERTEX);
          codeCount = vertexCount;
        }

        if (brk) {
          codeCheck();
          codes[codeCount] = BREAK;
          codeCount++;
        }

        if (code != -1) {
          codeCheck();
          codes[codeCount] = code;
          codeCount++;
        }
      }

      vertexCount++;

      return vertexCount - 1;
    }

    public void addBezierVertex(float x2, float y2, float z2,
                                float x3, float y3, float z3,
                                float x4, float y4, float z4, boolean brk) {
      addVertex(x2, y2, z2, BEZIER_VERTEX, brk);
      addVertex(x3, y3, z3, -1, false);
      addVertex(x4, y4, z4, -1, false);
    }

    public void addQuadraticVertex(float cx, float cy, float cz,
                                   float x3, float y3, float z3, boolean brk) {
      addVertex(cx, cy, cz, QUADRATIC_VERTEX, brk);
      addVertex(x3, y3, z3, -1, false);
    }

    public void addCurveVertex(float x, float y, float z, boolean brk) {
      addVertex(x, y, z, CURVE_VERTEX, brk);
    }

    // Returns the vertex data in the PGraphics double array format.
    float[][] getVertexData() {
      float[][] data = new float[vertexCount][VERTEX_FIELD_COUNT];
      for (int i = 0; i < vertexCount; i++) {
        float[] vert = data[i];

        vert[X] = vertices[3 * i + 0];
        vert[Y] = vertices[3 * i + 1];
        vert[Z] = vertices[3 * i + 2];

        vert[R] = ((colors[i] >> 16) & 0xFF) / 255.0f;
        vert[G] = ((colors[i] >>  8) & 0xFF) / 255.0f;
        vert[B] = ((colors[i] >>  0) & 0xFF) / 255.0f;
        vert[A] = ((colors[i] >> 24) & 0xFF) / 255.0f;

        vert[U] = texcoords[2 * i + 0];
        vert[V] = texcoords[2 * i + 1];

        vert[NX] = normals[3 * i + 0];
        vert[NY] = normals[3 * i + 1];
        vert[NZ] = normals[3 * i + 2];

        vert[SR] = ((strokeColors[i] >> 16) & 0xFF) / 255.0f;
        vert[SG] = ((strokeColors[i] >>  8) & 0xFF) / 255.0f;
        vert[SB] = ((strokeColors[i] >>  0) & 0xFF) / 255.0f;
        vert[SA] = ((strokeColors[i] >> 24) & 0xFF) / 255.0f;

        vert[SW] = strokeWeights[i];
      }

      return data;
    }

    boolean hasBezierVertex() {
      for (int i = 0; i < codeCount; i++) {
        if (codes[i] == BEZIER_VERTEX) return true;
      }
      return false;
    }

    boolean hasQuadraticVertex() {
      for (int i = 0; i < codeCount; i++) {
        if (codes[i] == QUADRATIC_VERTEX) return true;
      }
      return false;
    }

    boolean hasCurveVertex() {
      for (int i = 0; i < codeCount; i++) {
        if (codes[i] == CURVE_VERTEX) return true;
      }
      return false;
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

      edgeCount++;

      return edgeCount - 1;
    }

    int closeEdge(int i, int j) {
      edgeCheck();

      int[] edge = edges[edgeCount];
      edge[0] = i;
      edge[1] = j;
      edge[2] = EDGE_CLOSE;

      edgeCount++;

      return edgeCount - 1;
    }

    void addTrianglesEdges() {
      for (int i = 0; i < vertexCount / 3; i++) {
        int i0 = 3 * i + 0;
        int i1 = 3 * i + 1;
        int i2 = 3 * i + 2;

        addEdge(i0, i1,  true, false);
        addEdge(i1, i2, false, false);
        addEdge(i2, i0, false,  false);
        closeEdge(i2, i0);
      }
    }

    void addTriangleFanEdges() {
      for (int i = 1; i < vertexCount - 1; i++) {
        int i0 = 0;
        int i1 = i;
        int i2 = i + 1;

        addEdge(i0, i1,  true, false);
        addEdge(i1, i2, false, false);
        addEdge(i2, i0, false,  false);
        closeEdge(i2, i0);
      }
    }

    void addTriangleStripEdges() {
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

        addEdge(i0, i1,  true, false);
        addEdge(i1, i2, false, false);
        addEdge(i2, i0, false,  false);
        closeEdge(i2, i0);
      }
    }

    void addQuadsEdges() {
      for (int i = 0; i < vertexCount / 4; i++) {
        int i0 = 4 * i + 0;
        int i1 = 4 * i + 1;
        int i2 = 4 * i + 2;
        int i3 = 4 * i + 3;

        addEdge(i0, i1,  true, false);
        addEdge(i1, i2, false, false);
        addEdge(i2, i3, false,  false);
        addEdge(i3, i0, false,  false);
        closeEdge(i3, i0);
      }
    }

    void addQuadStripEdges() {
      for (int qd = 1; qd < vertexCount / 2; qd++) {
        int i0 = 2 * (qd - 1);
        int i1 = 2 * (qd - 1) + 1;
        int i2 = 2 * qd + 1;
        int i3 = 2 * qd;

        addEdge(i0, i1,  true, false);
        addEdge(i1, i2, false, false);
        addEdge(i2, i3, false,  false);
        addEdge(i3, i0, false,  true);
        closeEdge(i3, i0);
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
      for (int i = 0; i < vertexCount / 3; i++) {
        int i0 = 3 * i + 0;
        int i1 = 3 * i + 1;
        int i2 = 3 * i + 2;

        calcTriangleNormal(i0, i1, i2);
      }
    }

    void calcTriangleFanNormals() {
      for (int i = 1; i < vertexCount - 1; i++) {
        int i0 = 0;
        int i1 = i;
        int i2 = i + 1;

        calcTriangleNormal(i0, i1, i2);
      }
    }

    void calcTriangleStripNormals() {
      for (int i = 1; i < vertexCount - 1; i++) {
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
      for (int i = 0; i < vertexCount / 4; i++) {
        int i0 = 4 * i + 0;
        int i1 = 4 * i + 1;
        int i2 = 4 * i + 2;
        int i3 = 4 * i + 3;

        calcTriangleNormal(i0, i1, i2);
        calcTriangleNormal(i2, i3, i0);
      }
    }

    void calcQuadStripNormals() {
      for (int qd = 1; qd < vertexCount / 2; qd++) {
        int i0 = 2 * (qd - 1);
        int i1 = 2 * (qd - 1) + 1;
        int i2 = 2 * qd;
        int i3 = 2 * qd + 1;

        calcTriangleNormal(i0, i3, i1);
        calcTriangleNormal(i0, i2, i3);
      }
    }

    // -----------------------------------------------------------------
    //
    // Primitives

    void setMaterial(int fillColor, int strokeColor, float strokeWeight,
                     int ambientColor, int specularColor, int emissiveColor,
                     float shininessFactor) {
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
      addVertex(x, y, z, VERTEX, true);
    }

    void addLine(float x1, float y1, float z1,
                 float x2, float y2, float z2,
                 boolean fill, boolean stroke) {
      int idx1 = addVertex(x1, y1, z1, VERTEX, true);
      int idx2 = addVertex(x2, y2, z2, VERTEX, false);
      if (stroke) addEdge(idx1, idx2, true, true);
    }

    void addTriangle(float x1, float y1, float z1,
                     float x2, float y2, float z2,
                     float x3, float y3, float z3,
                     boolean fill, boolean stroke) {
      int idx1 = addVertex(x1, y1, z1, VERTEX, true);
      int idx2 = addVertex(x2, y2, z2, VERTEX, false);
      int idx3 = addVertex(x3, y3, z3, VERTEX, false);
      if (stroke) {
        addEdge(idx1, idx2, true, false);
        addEdge(idx2, idx3, false, false);
        addEdge(idx3, idx1, false, false);
        closeEdge(idx3, idx1);
      }
    }

    void addQuad(float x1, float y1, float z1,
                 float x2, float y2, float z2,
                 float x3, float y3, float z3,
                 float x4, float y4, float z4,
                 boolean stroke) {
      int idx1 = addVertex(x1, y1, z1, 0, 0, VERTEX, true);
      int idx2 = addVertex(x2, y2, z2, 1, 0, VERTEX, false);
      int idx3 = addVertex(x3, y3, z3, 1, 1, VERTEX, false);
      int idx4 = addVertex(x4, y4, z4, 0, 1, VERTEX, false);
      if (stroke) {
        addEdge(idx1, idx2, true, false);
        addEdge(idx2, idx3, false, false);
        addEdge(idx3, idx4, false, false);
        addEdge(idx4, idx1, false, false);
        closeEdge(idx4, idx1);
      }
    }

    void addRect(float a, float b, float c, float d,
                 boolean stroke) {
      addQuad(a, b, 0,
              c, b, 0,
              c, d, 0,
              a, d, 0,
              stroke);
    }

    void addRect(float a, float b, float c, float d,
                 float tl, float tr, float br, float bl,
                 boolean stroke) {
      if (nonZero(tr)) {
        addVertex(c-tr, b, VERTEX, true);
        addQuadraticVertex(c, b, 0, c, b+tr, 0, false);
      } else {
        addVertex(c, b, VERTEX, true);
      }
      if (nonZero(br)) {
        addVertex(c, d-br, VERTEX, false);
        addQuadraticVertex(c, d, 0, c-br, d, 0, false);
      } else {
        addVertex(c, d, VERTEX, false);
      }
      if (nonZero(bl)) {
        addVertex(a+bl, d, VERTEX, false);
        addQuadraticVertex(a, d, 0, a, d-bl, 0, false);
      } else {
        addVertex(a, d, VERTEX, false);
      }
      if (nonZero(tl)) {
        addVertex(a, b+tl, VERTEX, false);
        addQuadraticVertex(a, b, 0, a+tl, b, 0, false);
      } else {
        addVertex(a, b, VERTEX, false);
      }
    }

    void addEllipse(float x, float y, float w, float h,
                    boolean fill, boolean stroke) {
      float radiusH = w / 2;
      float radiusV = h / 2;

      float centerX = x + radiusH;
      float centerY = y + radiusV;

      // should call screenX/Y using current renderer.
      float sx1 = pg.screenX(x, y);
      float sy1 = pg.screenY(x, y);
      float sx2 = pg.screenX(x + w, y + h);
      float sy2 = pg.screenY(x + w, y + h);

      int accuracy =
        PApplet.min(MAX_POINT_ACCURACY, PApplet.max(MIN_POINT_ACCURACY,
                    (int) (TWO_PI * PApplet.dist(sx1, sy1, sx2, sy2) /
                    POINT_ACCURACY_FACTOR)));
      float inc = (float) SINCOS_LENGTH / accuracy;

      if (fill) {
        addVertex(centerX, centerY, VERTEX, true);
      }
      int idx0, pidx, idx;
      idx0 = pidx = idx = 0;
      float val = 0;
      for (int i = 0; i < accuracy; i++) {
        idx = addVertex(centerX + cosLUT[(int) val] * radiusH,
                        centerY + sinLUT[(int) val] * radiusV,
                        VERTEX, i == 0 && !fill);
        val = (val + inc) % SINCOS_LENGTH;

        if (0 < i) {
          if (stroke) addEdge(pidx, idx, i == 1, false);
        } else {
          idx0 = idx;
        }

        pidx = idx;
      }
      // Back to the beginning
      addVertex(centerX + cosLUT[0] * radiusH,
                centerY + sinLUT[0] * radiusV,
                VERTEX, false);
      if (stroke) {
        addEdge(idx, idx0, false, false);
        closeEdge(idx, idx0);
      }
    }

    // arcMode can be 0, OPEN, CHORD, or PIE
    void addArc(float x, float y, float w, float h,
                float start, float stop,
                boolean fill, boolean stroke, int arcMode) {
      float hr = w / 2f;
      float vr = h / 2f;

      float centerX = x + hr;
      float centerY = y + vr;

      int startLUT = (int) (0.5f + (start / TWO_PI) * SINCOS_LENGTH);
      int stopLUT = (int) (0.5f + (stop / TWO_PI) * SINCOS_LENGTH);

      if (fill) {
        addVertex(centerX, centerY, VERTEX, true);
      }

      int increment = 1; // what's a good algorithm? stopLUT - startLUT;
      int idx0, pidx, idx;
      idx0 = pidx = idx = 0;
      for (int i = startLUT; i < stopLUT; i += increment) {
        int ii = i % SINCOS_LENGTH;
        // modulo won't make the value positive
        if (ii < 0) ii += SINCOS_LENGTH;
        idx = addVertex(centerX + cosLUT[ii] * hr,
                        centerY + sinLUT[ii] * vr,
                        VERTEX, i == startLUT && !fill);

        if (stroke) {
          if (arcMode == PIE) {
            addEdge(pidx, idx, i == startLUT, false);
          } else if (startLUT < i) {
            addEdge(pidx, idx, i == startLUT + 1, arcMode == 0 &&
                                                  i == stopLUT - 1);
          }
        }

        if (startLUT == i) idx0 = idx;
        pidx = idx;
      }
      // draw last point explicitly for accuracy
      idx = addVertex(centerX + cosLUT[stopLUT % SINCOS_LENGTH] * hr,
                      centerY + sinLUT[stopLUT % SINCOS_LENGTH] * vr,
                      VERTEX, false);
      if (stroke) {
        if (arcMode == PIE) {
          addEdge(idx, idx0, false, false);
          closeEdge(idx, idx0);
        }
      }
      if (arcMode == CHORD || arcMode == OPEN) {
        // Add a last vertex coincident with the first along the perimeter
        pidx = idx;
        int i = startLUT;
        int ii = i % SINCOS_LENGTH;
        if (ii < 0) ii += SINCOS_LENGTH;
        idx = addVertex(centerX + cosLUT[ii] * hr,
                        centerY + sinLUT[ii] * vr,
                        VERTEX, false);
        if (stroke && arcMode == CHORD) {
          addEdge(pidx, idx, false, true);
        }
      }
    }

    void addBox(float w, float h, float d,
                boolean fill, boolean stroke) {
      float x1 = -w/2f; float x2 = w/2f;
      float y1 = -h/2f; float y2 = h/2f;
      float z1 = -d/2f; float z2 = d/2f;

      int idx1 = 0, idx2 = 0, idx3 = 0, idx4 = 0;
      if (fill || stroke) {
        // back face
        setNormal(0, 0, -1);
        idx1 = addVertex(x1, y1, z1, 0, 0, VERTEX, true);
        idx2 = addVertex(x2, y1, z1, 1, 0, VERTEX, false);
        idx3 = addVertex(x2, y2, z1, 1, 1, VERTEX, false);
        idx4 = addVertex(x1, y2, z1, 0, 1, VERTEX, false);
        if (stroke) {
          addEdge(idx1, idx2, true, false);
          addEdge(idx2, idx3, false, false);
          addEdge(idx3, idx4, false, false);
          addEdge(idx4, idx1, false, false);
          closeEdge(idx4, idx1);
        }

        // front face
        setNormal(0, 0, 1);
        idx1 = addVertex(x2, y1, z2, 0, 0, VERTEX, false);
        idx2 = addVertex(x1, y1, z2, 1, 0, VERTEX, false);
        idx3 = addVertex(x1, y2, z2, 1, 1, VERTEX, false);
        idx4 = addVertex(x2, y2, z2, 0, 1, VERTEX, false);
        if (stroke) {
          addEdge(idx1, idx2, true, false);
          addEdge(idx2, idx3, false, false);
          addEdge(idx3, idx4, false, false);
          addEdge(idx4, idx1, false, false);
          closeEdge(idx4, idx1);
        }

        // right face
        setNormal(1, 0, 0);
        idx1 = addVertex(x2, y1, z1, 0, 0, VERTEX, false);
        idx2 = addVertex(x2, y1, z2, 1, 0, VERTEX, false);
        idx3 = addVertex(x2, y2, z2, 1, 1, VERTEX, false);
        idx4 = addVertex(x2, y2, z1, 0, 1, VERTEX, false);
        if (stroke) {
          addEdge(idx1, idx2, true, false);
          addEdge(idx2, idx3, false, false);
          addEdge(idx3, idx4, false, false);
          addEdge(idx4, idx1, false, false);
          closeEdge(idx4, idx1);
        }

        // left face
        setNormal(-1, 0, 0);
        idx1 = addVertex(x1, y1, z2, 0, 0, VERTEX, false);
        idx2 = addVertex(x1, y1, z1, 1, 0, VERTEX, false);
        idx3 = addVertex(x1, y2, z1, 1, 1, VERTEX, false);
        idx4 = addVertex(x1, y2, z2, 0, 1, VERTEX, false);
        if (stroke) {
          addEdge(idx1, idx2, true, false);
          addEdge(idx2, idx3, false, false);
          addEdge(idx3, idx4, false, false);
          addEdge(idx4, idx1, false, false);
          closeEdge(idx4, idx1);
        }

        // bottom face
        setNormal(0, -1, 0);
        idx1 = addVertex(x1, y1, z2, 0, 0, VERTEX, false);
        idx2 = addVertex(x2, y1, z2, 1, 0, VERTEX, false);
        idx3 = addVertex(x2, y1, z1, 1, 1, VERTEX, false);
        idx4 = addVertex(x1, y1, z1, 0, 1, VERTEX, false);
        if (stroke) {
          addEdge(idx1, idx2, true, false);
          addEdge(idx2, idx3, false, false);
          addEdge(idx3, idx4, false, false);
          addEdge(idx4, idx1, false, false);
          closeEdge(idx4, idx1);
        }

        // top face
        setNormal(0, 1, 0);
        idx1 = addVertex(x1, y2, z1, 0, 0, VERTEX, false);
        idx2 = addVertex(x2, y2, z1, 1, 0, VERTEX, false);
        idx3 = addVertex(x2, y2, z2, 1, 1, VERTEX, false);
        idx4 = addVertex(x1, y2, z2, 0, 1, VERTEX, false);
        if (stroke) {
          addEdge(idx1, idx2, true, false);
          addEdge(idx2, idx3, false, false);
          addEdge(idx3, idx4, false, false);
          addEdge(idx4, idx1, false, false);
          closeEdge(idx4, idx1);
        }
      }
    }

    // Adds the vertices that define an sphere, without duplicating
    // any vertex or edge.
    int[] addSphere(float r, int detailU, int detailV,
                    boolean fill, boolean stroke) {
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
        addVertex(0, r, 0, u , v, VERTEX, true);
        u -= du;
      }
      vertCount = detailU;
      vert0 = vertCount;
      u = 1; v -= dv;
      for (int i = 0; i < detailU; i++) {
        setNormal(pg.sphereX[i], pg.sphereY[i], pg.sphereZ[i]);
        addVertex(r*pg.sphereX[i], r*pg.sphereY[i], r*pg.sphereZ[i], u , v,
                  VERTEX, false);
        u -= du;
      }
      vertCount += detailU;
      vert1 = vertCount;
      setNormal(pg.sphereX[0], pg.sphereY[0], pg.sphereZ[0]);
      addVertex(r*pg.sphereX[0], r*pg.sphereY[0], r*pg.sphereZ[0], u, v,
                VERTEX, false);
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
          setNormal(pg.sphereX[ioff], pg.sphereY[ioff], pg.sphereZ[ioff]);
          addVertex(r*pg.sphereX[ioff], r*pg.sphereY[ioff], r*pg.sphereZ[ioff],
                    u , v, VERTEX, false);
          u -= du;
        }
        vertCount += detailU;
        vert1 = vertCount;
        setNormal(pg.sphereX[offset], pg.sphereY[offset], pg.sphereZ[offset]);
        addVertex(r*pg.sphereX[offset], r*pg.sphereY[offset], r*pg.sphereZ[offset],
                  u, v, VERTEX, false);
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
        addVertex(0, -r, 0, u , v, VERTEX, false);
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
  static protected class TessGeometry {
    int renderMode;
    PGraphicsOpenGL pg;

    // Tessellated polygon data
    int polyVertexCount;
    int firstPolyVertex;
    int lastPolyVertex;
    FloatBuffer polyVerticesBuffer;
    IntBuffer polyColorsBuffer;
    FloatBuffer polyNormalsBuffer;
    FloatBuffer polyTexCoordsBuffer;

    // Polygon material properties (polyColors is used
    // as the diffuse color when lighting is enabled)
    IntBuffer polyAmbientBuffer;
    IntBuffer polySpecularBuffer;
    IntBuffer polyEmissiveBuffer;
    FloatBuffer polyShininessBuffer;

    int polyIndexCount;
    int firstPolyIndex;
    int lastPolyIndex;
    ShortBuffer polyIndicesBuffer;
    IndexCache polyIndexCache = new IndexCache();

    // Tessellated line data
    int lineVertexCount;
    int firstLineVertex;
    int lastLineVertex;
    FloatBuffer lineVerticesBuffer;
    IntBuffer lineColorsBuffer;
    FloatBuffer lineDirectionsBuffer;

    int lineIndexCount;
    int firstLineIndex;
    int lastLineIndex;
    ShortBuffer lineIndicesBuffer;
    IndexCache lineIndexCache = new IndexCache();

    // Tessellated point data
    int pointVertexCount;
    int firstPointVertex;
    int lastPointVertex;
    FloatBuffer pointVerticesBuffer;
    IntBuffer pointColorsBuffer;
    FloatBuffer pointOffsetsBuffer;

    int pointIndexCount;
    int firstPointIndex;
    int lastPointIndex;
    ShortBuffer pointIndicesBuffer;
    IndexCache pointIndexCache = new IndexCache();

    // Backing arrays
    float[] polyVertices;
    int[] polyColors;
    float[] polyNormals;
    float[] polyTexCoords;
    int[] polyAmbient;
    int[] polySpecular;
    int[] polyEmissive;
    float[] polyShininess;
    short[] polyIndices;
    float[] lineVertices;
    int[] lineColors;
    float[] lineDirections;
    short[] lineIndices;
    float[] pointVertices;
    int[] pointColors;
    float[] pointOffsets;
    short[] pointIndices;

    TessGeometry(PGraphicsOpenGL pg, int mode) {
      this.pg = pg;
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
      polyTexCoords = new float[2 * PGL.DEFAULT_TESS_VERTICES];
      polyAmbient = new int[PGL.DEFAULT_TESS_VERTICES];
      polySpecular = new int[PGL.DEFAULT_TESS_VERTICES];
      polyEmissive = new int[PGL.DEFAULT_TESS_VERTICES];
      polyShininess = new float[PGL.DEFAULT_TESS_VERTICES];
      polyIndices = new short[PGL.DEFAULT_TESS_VERTICES];

      lineVertices = new float[4 * PGL.DEFAULT_TESS_VERTICES];
      lineColors = new int[PGL.DEFAULT_TESS_VERTICES];
      lineDirections = new float[4 * PGL.DEFAULT_TESS_VERTICES];
      lineIndices = new short[PGL.DEFAULT_TESS_VERTICES];

      pointVertices = new float[4 * PGL.DEFAULT_TESS_VERTICES];
      pointColors = new int[PGL.DEFAULT_TESS_VERTICES];
      pointOffsets = new float[2 * PGL.DEFAULT_TESS_VERTICES];
      pointIndices = new short[PGL.DEFAULT_TESS_VERTICES];

      polyVerticesBuffer = PGL.allocateFloatBuffer(polyVertices);
      polyColorsBuffer = PGL.allocateIntBuffer(polyColors);
      polyNormalsBuffer = PGL.allocateFloatBuffer(polyNormals);
      polyTexCoordsBuffer = PGL.allocateFloatBuffer(polyTexCoords);
      polyAmbientBuffer = PGL.allocateIntBuffer(polyAmbient);
      polySpecularBuffer = PGL.allocateIntBuffer(polySpecular);
      polyEmissiveBuffer = PGL.allocateIntBuffer(polyEmissive);
      polyShininessBuffer = PGL.allocateFloatBuffer(polyShininess);
      polyIndicesBuffer = PGL.allocateShortBuffer(polyIndices);

      lineVerticesBuffer = PGL.allocateFloatBuffer(lineVertices);
      lineColorsBuffer = PGL.allocateIntBuffer(lineColors);
      lineDirectionsBuffer = PGL.allocateFloatBuffer(lineDirections);
      lineIndicesBuffer = PGL.allocateShortBuffer(lineIndices);

      pointVerticesBuffer = PGL.allocateFloatBuffer(pointVertices);
      pointColorsBuffer = PGL.allocateIntBuffer(pointColors);
      pointOffsetsBuffer = PGL.allocateFloatBuffer(pointOffsets);
      pointIndicesBuffer = PGL.allocateShortBuffer(pointIndices);

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

    void polyVertexCheck() {
      if (polyVertexCount == polyVertices.length / 4) {
        int newSize = polyVertexCount << 1;

        expandPolyVertices(newSize);
        expandPolyColors(newSize);
        expandPolyNormals(newSize);
        expandPolyTexCoords(newSize);
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
        expandPolyTexCoords(newSize);
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
        expandLineDirections(newSize);
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
        expandPointOffsets(newSize);
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
    // Methods to prepare buffers for relative read/write operations

    protected void updatePolyVerticesBuffer() {
      updatePolyVerticesBuffer(0, polyVertexCount);
    }

    protected void updatePolyVerticesBuffer(int offset, int size) {
      PGL.updateFloatBuffer(polyVerticesBuffer, polyVertices,
                            4 * offset, 4 * size);
    }

    protected void updatePolyColorsBuffer() {
      updatePolyColorsBuffer(0, polyVertexCount);
    }

    protected void updatePolyColorsBuffer(int offset, int size) {
      PGL.updateIntBuffer(polyColorsBuffer, polyColors, offset, size);
    }

    protected void updatePolyNormalsBuffer() {
      updatePolyNormalsBuffer(0, polyVertexCount);
    }

    protected void updatePolyNormalsBuffer(int offset, int size) {
      PGL.updateFloatBuffer(polyNormalsBuffer, polyNormals,
                            3 * offset, 3 * size);
    }

    protected void updatePolyTexCoordsBuffer() {
      updatePolyTexCoordsBuffer(0, polyVertexCount);
    }

    protected void updatePolyTexCoordsBuffer(int offset, int size) {
      PGL.updateFloatBuffer(polyTexCoordsBuffer, polyTexCoords,
                            2 * offset, 2 * size);
    }

    protected void updatePolyAmbientBuffer() {
      updatePolyAmbientBuffer(0, polyVertexCount);
    }

    protected void updatePolyAmbientBuffer(int offset, int size) {
      PGL.updateIntBuffer(polyAmbientBuffer, polyAmbient, offset, size);
    }

    protected void updatePolySpecularBuffer() {
      updatePolySpecularBuffer(0, polyVertexCount);
    }

    protected void updatePolySpecularBuffer(int offset, int size) {
      PGL.updateIntBuffer(polySpecularBuffer, polySpecular, offset, size);
    }

    protected void updatePolyEmissiveBuffer() {
      updatePolyEmissiveBuffer(0, polyVertexCount);
    }

    protected void updatePolyEmissiveBuffer(int offset, int size) {
      PGL.updateIntBuffer(polyEmissiveBuffer, polyEmissive, offset, size);
    }

    protected void updatePolyShininessBuffer() {
      updatePolyShininessBuffer(0, polyVertexCount);
    }

    protected void updatePolyShininessBuffer(int offset, int size) {
      PGL.updateFloatBuffer(polyShininessBuffer, polyShininess, offset, size);
    }

    protected void updatePolyIndicesBuffer() {
      updatePolyIndicesBuffer(0, polyIndexCount);
    }

    protected void updatePolyIndicesBuffer(int offset, int size) {
      PGL.updateShortBuffer(polyIndicesBuffer, polyIndices, offset, size);
    }

    protected void updateLineVerticesBuffer() {
      updateLineVerticesBuffer(0, lineVertexCount);
    }

    protected void updateLineVerticesBuffer(int offset, int size) {
      PGL.updateFloatBuffer(lineVerticesBuffer, lineVertices,
                            4 * offset, 4 * size);
    }

    protected void updateLineColorsBuffer() {
      updateLineColorsBuffer(0, lineVertexCount);
    }

    protected void updateLineColorsBuffer(int offset, int size) {
      PGL.updateIntBuffer(lineColorsBuffer, lineColors, offset, size);
    }

    protected void updateLineDirectionsBuffer() {
      updateLineDirectionsBuffer(0, lineVertexCount);
    }

    protected void updateLineDirectionsBuffer(int offset, int size) {
      PGL.updateFloatBuffer(lineDirectionsBuffer, lineDirections,
                            4 * offset, 4 * size);
    }

    protected void updateLineIndicesBuffer() {
      updateLineIndicesBuffer(0, lineIndexCount);
    }

    protected void updateLineIndicesBuffer(int offset, int size) {
      PGL.updateShortBuffer(lineIndicesBuffer, lineIndices, offset, size);
    }

    protected void updatePointVerticesBuffer() {
      updatePointVerticesBuffer(0, pointVertexCount);
    }

    protected void updatePointVerticesBuffer(int offset, int size) {
      PGL.updateFloatBuffer(pointVerticesBuffer, pointVertices,
                            4 * offset, 4 * size);
    }

    protected void updatePointColorsBuffer() {
      updatePointColorsBuffer(0, pointVertexCount);
    }

    protected void updatePointColorsBuffer(int offset, int size) {
      PGL.updateIntBuffer(pointColorsBuffer, pointColors, offset, size);
    }

    protected void updatePointOffsetsBuffer() {
      updatePointOffsetsBuffer(0, pointVertexCount);
    }

    protected void updatePointOffsetsBuffer(int offset, int size) {
      PGL.updateFloatBuffer(pointOffsetsBuffer, pointOffsets,
                            2 * offset, 2 * size);
    }

    protected void updatePointIndicesBuffer() {
      updatePointIndicesBuffer(0, pointIndexCount);
    }

    protected void updatePointIndicesBuffer(int offset, int size) {
      PGL.updateShortBuffer(pointIndicesBuffer, pointIndices, offset, size);
    }

    // -----------------------------------------------------------------
    //
    // Expand arrays

    void expandPolyVertices(int n) {
      float temp[] = new float[4 * n];
      PApplet.arrayCopy(polyVertices, 0, temp, 0, 4 * polyVertexCount);
      polyVertices = temp;
      polyVerticesBuffer = PGL.allocateFloatBuffer(polyVertices);
    }

    void expandPolyColors(int n) {
      int temp[] = new int[n];
      PApplet.arrayCopy(polyColors, 0, temp, 0, polyVertexCount);
      polyColors = temp;
      polyColorsBuffer = PGL.allocateIntBuffer(polyColors);
    }

    void expandPolyNormals(int n) {
      float temp[] = new float[3 * n];
      PApplet.arrayCopy(polyNormals, 0, temp, 0, 3 * polyVertexCount);
      polyNormals = temp;
      polyNormalsBuffer = PGL.allocateFloatBuffer(polyNormals);
    }

    void expandPolyTexCoords(int n) {
      float temp[] = new float[2 * n];
      PApplet.arrayCopy(polyTexCoords, 0, temp, 0, 2 * polyVertexCount);
      polyTexCoords = temp;
      polyTexCoordsBuffer = PGL.allocateFloatBuffer(polyTexCoords);
    }

    void expandPolyAmbient(int n) {
      int temp[] = new int[n];
      PApplet.arrayCopy(polyAmbient, 0, temp, 0, polyVertexCount);
      polyAmbient = temp;
      polyAmbientBuffer = PGL.allocateIntBuffer(polyAmbient);
    }

    void expandPolySpecular(int n) {
      int temp[] = new int[n];
      PApplet.arrayCopy(polySpecular, 0, temp, 0, polyVertexCount);
      polySpecular = temp;
      polySpecularBuffer = PGL.allocateIntBuffer(polySpecular);
    }

    void expandPolyEmissive(int n) {
      int temp[] = new int[n];
      PApplet.arrayCopy(polyEmissive, 0, temp, 0, polyVertexCount);
      polyEmissive = temp;
      polyEmissiveBuffer = PGL.allocateIntBuffer(polyEmissive);
    }

    void expandPolyShininess(int n) {
      float temp[] = new float[n];
      PApplet.arrayCopy(polyShininess, 0, temp, 0, polyVertexCount);
      polyShininess = temp;
      polyShininessBuffer = PGL.allocateFloatBuffer(polyShininess);
    }

    void expandPolyIndices(int n) {
      short temp[] = new short[n];
      PApplet.arrayCopy(polyIndices, 0, temp, 0, polyIndexCount);
      polyIndices = temp;
      polyIndicesBuffer = PGL.allocateShortBuffer(polyIndices);
    }

    void expandLineVertices(int n) {
      float temp[] = new float[4 * n];
      PApplet.arrayCopy(lineVertices, 0, temp, 0, 4 * lineVertexCount);
      lineVertices = temp;
      lineVerticesBuffer = PGL.allocateFloatBuffer(lineVertices);
    }

    void expandLineColors(int n) {
      int temp[] = new int[n];
      PApplet.arrayCopy(lineColors, 0, temp, 0, lineVertexCount);
      lineColors = temp;
      lineColorsBuffer = PGL.allocateIntBuffer(lineColors);
    }

    void expandLineDirections(int n) {
      float temp[] = new float[4 * n];
      PApplet.arrayCopy(lineDirections, 0, temp, 0, 4 * lineVertexCount);
      lineDirections = temp;
      lineDirectionsBuffer = PGL.allocateFloatBuffer(lineDirections);
    }

    void expandLineIndices(int n) {
      short temp[] = new short[n];
      PApplet.arrayCopy(lineIndices, 0, temp, 0, lineIndexCount);
      lineIndices = temp;
      lineIndicesBuffer = PGL.allocateShortBuffer(lineIndices);
    }

    void expandPointVertices(int n) {
      float temp[] = new float[4 * n];
      PApplet.arrayCopy(pointVertices, 0, temp, 0, 4 * pointVertexCount);
      pointVertices = temp;
      pointVerticesBuffer = PGL.allocateFloatBuffer(pointVertices);
    }

    void expandPointColors(int n) {
      int temp[] = new int[n];
      PApplet.arrayCopy(pointColors, 0, temp, 0, pointVertexCount);
      pointColors = temp;
      pointColorsBuffer = PGL.allocateIntBuffer(pointColors);
    }

    void expandPointOffsets(int n) {
      float temp[] = new float[2 * n];
      PApplet.arrayCopy(pointOffsets, 0, temp, 0, 2 * pointVertexCount);
      pointOffsets = temp;
      pointOffsetsBuffer = PGL.allocateFloatBuffer(pointOffsets);
    }

    void expandPointIndices(int n) {
      short temp[] = new short[n];
      PApplet.arrayCopy(pointIndices, 0, temp, 0, pointIndexCount);
      pointIndices = temp;
      pointIndicesBuffer = PGL.allocateShortBuffer(pointIndices);
    }

    // -----------------------------------------------------------------
    //
    // Trim arrays

    void trim() {
      if (0 < polyVertexCount && polyVertexCount < polyVertices.length / 4) {
        trimPolyVertices();
        trimPolyColors();
        trimPolyNormals();
        trimPolyTexCoords();
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
        trimLineDirections();
      }

      if (0 < lineIndexCount && lineIndexCount < lineIndices.length) {
        trimLineIndices();
      }

      if (0 < pointVertexCount && pointVertexCount < pointVertices.length / 4) {
        trimPointVertices();
        trimPointColors();
        trimPointOffsets();
      }

      if (0 < pointIndexCount && pointIndexCount < pointIndices.length) {
        trimPointIndices();
      }
    }

    void trimPolyVertices() {
      float temp[] = new float[4 * polyVertexCount];
      PApplet.arrayCopy(polyVertices, 0, temp, 0, 4 * polyVertexCount);
      polyVertices = temp;
      polyVerticesBuffer = PGL.allocateFloatBuffer(polyVertices);
    }

    void trimPolyColors() {
      int temp[] = new int[polyVertexCount];
      PApplet.arrayCopy(polyColors, 0, temp, 0, polyVertexCount);
      polyColors = temp;
      polyColorsBuffer = PGL.allocateIntBuffer(polyColors);
    }

    void trimPolyNormals() {
      float temp[] = new float[3 * polyVertexCount];
      PApplet.arrayCopy(polyNormals, 0, temp, 0, 3 * polyVertexCount);
      polyNormals = temp;
      polyNormalsBuffer = PGL.allocateFloatBuffer(polyNormals);
    }

    void trimPolyTexCoords() {
      float temp[] = new float[2 * polyVertexCount];
      PApplet.arrayCopy(polyTexCoords, 0, temp, 0, 2 * polyVertexCount);
      polyTexCoords = temp;
      polyTexCoordsBuffer = PGL.allocateFloatBuffer(polyTexCoords);
    }

    void trimPolyAmbient() {
      int temp[] = new int[polyVertexCount];
      PApplet.arrayCopy(polyAmbient, 0, temp, 0, polyVertexCount);
      polyAmbient = temp;
      polyAmbientBuffer = PGL.allocateIntBuffer(polyAmbient);
    }

    void trimPolySpecular() {
      int temp[] = new int[polyVertexCount];
      PApplet.arrayCopy(polySpecular, 0, temp, 0, polyVertexCount);
      polySpecular = temp;
      polySpecularBuffer = PGL.allocateIntBuffer(polySpecular);
    }

    void trimPolyEmissive() {
      int temp[] = new int[polyVertexCount];
      PApplet.arrayCopy(polyEmissive, 0, temp, 0, polyVertexCount);
      polyEmissive = temp;
      polyEmissiveBuffer = PGL.allocateIntBuffer(polyEmissive);
    }

    void trimPolyShininess() {
      float temp[] = new float[polyVertexCount];
      PApplet.arrayCopy(polyShininess, 0, temp, 0, polyVertexCount);
      polyShininess = temp;
      polyShininessBuffer = PGL.allocateFloatBuffer(polyShininess);
    }

    void trimPolyIndices() {
      short temp[] = new short[polyIndexCount];
      PApplet.arrayCopy(polyIndices, 0, temp, 0, polyIndexCount);
      polyIndices = temp;
      polyIndicesBuffer = PGL.allocateShortBuffer(polyIndices);
    }

    void trimLineVertices() {
      float temp[] = new float[4 * lineVertexCount];
      PApplet.arrayCopy(lineVertices, 0, temp, 0, 4 * lineVertexCount);
      lineVertices = temp;
      lineVerticesBuffer = PGL.allocateFloatBuffer(lineVertices);
    }

    void trimLineColors() {
      int temp[] = new int[lineVertexCount];
      PApplet.arrayCopy(lineColors, 0, temp, 0, lineVertexCount);
      lineColors = temp;
      lineColorsBuffer = PGL.allocateIntBuffer(lineColors);
    }

    void trimLineDirections() {
      float temp[] = new float[4 * lineVertexCount];
      PApplet.arrayCopy(lineDirections, 0, temp, 0, 4 * lineVertexCount);
      lineDirections = temp;
      lineDirectionsBuffer = PGL.allocateFloatBuffer(lineDirections);
    }

    void trimLineIndices() {
      short temp[] = new short[lineIndexCount];
      PApplet.arrayCopy(lineIndices, 0, temp, 0, lineIndexCount);
      lineIndices = temp;
      lineIndicesBuffer = PGL.allocateShortBuffer(lineIndices);
    }

    void trimPointVertices() {
      float temp[] = new float[4 * pointVertexCount];
      PApplet.arrayCopy(pointVertices, 0, temp, 0, 4 * pointVertexCount);
      pointVertices = temp;
      pointVerticesBuffer = PGL.allocateFloatBuffer(pointVertices);
    }

    void trimPointColors() {
      int temp[] = new int[pointVertexCount];
      PApplet.arrayCopy(pointColors, 0, temp, 0, pointVertexCount);
      pointColors = temp;
      pointColorsBuffer = PGL.allocateIntBuffer(pointColors);
    }

    void trimPointOffsets() {
      float temp[] = new float[2 * pointVertexCount];
      PApplet.arrayCopy(pointOffsets, 0, temp, 0, 2 * pointVertexCount);
      pointOffsets = temp;
      pointOffsetsBuffer = PGL.allocateFloatBuffer(pointOffsets);
    }

    void trimPointIndices() {
      short temp[] = new short[pointIndexCount];
      PApplet.arrayCopy(pointIndices, 0, temp, 0, pointIndexCount);
      pointIndices = temp;
      pointIndicesBuffer = PGL.allocateShortBuffer(pointIndices);
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

    // Sets point vertex with index tessIdx using the data from input vertex
    // inIdx.
    void setPointVertex(int tessIdx, InGeometry in, int inIdx) {
      int index;

      index = 3 * inIdx;
      float x = in.vertices[index++];
      float y = in.vertices[index++];
      float z = in.vertices[index  ];

      if (renderMode == IMMEDIATE && pg.flushMode == FLUSH_WHEN_FULL) {
        PMatrix3D mm = pg.modelview;

        index = 4 * tessIdx;
        pointVertices[index++] = x*mm.m00 + y*mm.m01 + z*mm.m02 + mm.m03;
        pointVertices[index++] = x*mm.m10 + y*mm.m11 + z*mm.m12 + mm.m13;
        pointVertices[index++] = x*mm.m20 + y*mm.m21 + z*mm.m22 + mm.m23;
        pointVertices[index  ] = x*mm.m30 + y*mm.m31 + z*mm.m32 + mm.m33;
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

    void setLineVertex(int tessIdx, float[] vertices, int inIdx0, int rgba) {
      int index;

      index = 3 * inIdx0;
      float x0 = vertices[index++];
      float y0 = vertices[index++];
      float z0 = vertices[index  ];

      if (renderMode == IMMEDIATE && pg.flushMode == FLUSH_WHEN_FULL) {
        PMatrix3D mm = pg.modelview;

        index = 4 * tessIdx;
        lineVertices[index++] = x0*mm.m00 + y0*mm.m01 + z0*mm.m02 + mm.m03;
        lineVertices[index++] = x0*mm.m10 + y0*mm.m11 + z0*mm.m12 + mm.m13;
        lineVertices[index++] = x0*mm.m20 + y0*mm.m21 + z0*mm.m22 + mm.m23;
        lineVertices[index  ] = x0*mm.m30 + y0*mm.m31 + z0*mm.m32 + mm.m33;
      } else {
        index = 4 * tessIdx;
        lineVertices[index++] = x0;
        lineVertices[index++] = y0;
        lineVertices[index++] = z0;
        lineVertices[index  ] = 1;
      }

      lineColors[tessIdx] = rgba;
      index = 4 * tessIdx;
      lineDirections[index++] = 0;
      lineDirections[index++] = 0;
      lineDirections[index++] = 0;
      lineDirections[index  ] = 0;
    }

    // Sets line vertex with index tessIdx using the data from input vertices
    // inIdx0 and inIdx1.
    void setLineVertex(int tessIdx, float[] vertices, int inIdx0, int inIdx1,
                       int rgba, float weight) {
      int index;

      index = 3 * inIdx0;
      float x0 = vertices[index++];
      float y0 = vertices[index++];
      float z0 = vertices[index  ];

      index = 3 * inIdx1;
      float x1 = vertices[index++];
      float y1 = vertices[index++];
      float z1 = vertices[index  ];

      float dx = x1 - x0;
      float dy = y1 - y0;
      float dz = z1 - z0;

      if (renderMode == IMMEDIATE && pg.flushMode == FLUSH_WHEN_FULL) {
        PMatrix3D mm = pg.modelview;

        index = 4 * tessIdx;
        lineVertices[index++] = x0*mm.m00 + y0*mm.m01 + z0*mm.m02 + mm.m03;
        lineVertices[index++] = x0*mm.m10 + y0*mm.m11 + z0*mm.m12 + mm.m13;
        lineVertices[index++] = x0*mm.m20 + y0*mm.m21 + z0*mm.m22 + mm.m23;
        lineVertices[index  ] = x0*mm.m30 + y0*mm.m31 + z0*mm.m32 + mm.m33;

        index = 4 * tessIdx;
        lineDirections[index++] = dx*mm.m00 + dy*mm.m01 + dz*mm.m02;
        lineDirections[index++] = dx*mm.m10 + dy*mm.m11 + dz*mm.m12;
        lineDirections[index  ] = dx*mm.m20 + dy*mm.m21 + dz*mm.m22;
      } else {
        index = 4 * tessIdx;
        lineVertices[index++] = x0;
        lineVertices[index++] = y0;
        lineVertices[index++] = z0;
        lineVertices[index  ] = 1;

        index = 4 * tessIdx;
        lineDirections[index++] = dx;
        lineDirections[index++] = dy;
        lineDirections[index  ] = dz;
      }

      lineColors[tessIdx] = rgba;
      lineDirections[4 * tessIdx + 3] = weight;
    }

    // -----------------------------------------------------------------
    //
    // Add poly geometry

    void addPolyVertex(float x, float y, float z,
                       int rgba,
                       float nx, float ny, float nz,
                       float u, float v,
                       int am, int sp, int em, float shine,
                       boolean clampXY) {
      polyVertexCheck();
      int tessIdx = polyVertexCount - 1;
      setPolyVertex(tessIdx, x, y, z,
                    rgba,
                    nx, ny, nz,
                    u, v,
                    am, sp, em, shine, clampXY);
    }

    void setPolyVertex(int tessIdx, float x, float y, float z, int rgba,
                       boolean clampXY) {
      setPolyVertex(tessIdx, x, y, z,
                    rgba,
                    0, 0, 1,
                    0, 0,
                    0, 0, 0, 0, clampXY);
    }

    void setPolyVertex(int tessIdx, float x, float y, float z,
                       int rgba,
                       float nx, float ny, float nz,
                       float u, float v,
                       int am, int sp, int em, float shine,
                       boolean clampXY) {
      int index;

      if (renderMode == IMMEDIATE && pg.flushMode == FLUSH_WHEN_FULL) {
        PMatrix3D mm = pg.modelview;
        PMatrix3D nm = pg.modelviewInv;

        index = 4 * tessIdx;
        if (clampXY) {
          // ceil emulates the behavior of JAVA2D
          polyVertices[index++] =
            PApplet.ceil(x*mm.m00 + y*mm.m01 + z*mm.m02 + mm.m03);
          polyVertices[index++] =
            PApplet.ceil(x*mm.m10 + y*mm.m11 + z*mm.m12 + mm.m13);
        } else {
          polyVertices[index++] = x*mm.m00 + y*mm.m01 + z*mm.m02 + mm.m03;
          polyVertices[index++] = x*mm.m10 + y*mm.m11 + z*mm.m12 + mm.m13;
        }
        polyVertices[index++] = x*mm.m20 + y*mm.m21 + z*mm.m22 + mm.m23;
        polyVertices[index  ] = x*mm.m30 + y*mm.m31 + z*mm.m32 + mm.m33;

        index = 3 * tessIdx;
        polyNormals[index++] = nx*nm.m00 + ny*nm.m10 + nz*nm.m20;
        polyNormals[index++] = nx*nm.m01 + ny*nm.m11 + nz*nm.m21;
        polyNormals[index  ] = nx*nm.m02 + ny*nm.m12 + nz*nm.m22;
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
      polyTexCoords[index++] = u;
      polyTexCoords[index  ] = v;

      polyAmbient[tessIdx] = am;
      polySpecular[tessIdx] = sp;
      polyEmissive[tessIdx] = em;
      polyShininess[tessIdx] = shine;
    }

    void addPolyVertices(InGeometry in, boolean clampXY) {
      addPolyVertices(in, 0, in.vertexCount - 1, clampXY);
    }

    void addPolyVertex(InGeometry in, int i, boolean clampXY) {
      addPolyVertices(in, i, i, clampXY);
    }

    void addPolyVertices(InGeometry in, int i0, int i1, boolean clampXY) {
      int index;
      int nvert = i1 - i0 + 1;

      polyVertexCheck(nvert);

      if (renderMode == IMMEDIATE && pg.flushMode == FLUSH_WHEN_FULL) {
        PMatrix3D mm = pg.modelview;
        PMatrix3D nm = pg.modelviewInv;

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
          if (clampXY) {
            // ceil emulates the behavior of JAVA2D
            polyVertices[index++] =
              PApplet.ceil(x*mm.m00 + y*mm.m01 + z*mm.m02 + mm.m03);
            polyVertices[index++] =
              PApplet.ceil(x*mm.m10 + y*mm.m11 + z*mm.m12 + mm.m13);
          } else {
            polyVertices[index++] = x*mm.m00 + y*mm.m01 + z*mm.m02 + mm.m03;
            polyVertices[index++] = x*mm.m10 + y*mm.m11 + z*mm.m12 + mm.m13;
          }
          polyVertices[index++] = x*mm.m20 + y*mm.m21 + z*mm.m22 + mm.m23;
          polyVertices[index  ] = x*mm.m30 + y*mm.m31 + z*mm.m32 + mm.m33;

          index = 3 * tessIdx;
          polyNormals[index++] = nx*nm.m00 + ny*nm.m10 + nz*nm.m20;
          polyNormals[index++] = nx*nm.m01 + ny*nm.m11 + nz*nm.m21;
          polyNormals[index  ] = nx*nm.m02 + ny*nm.m12 + nz*nm.m22;
        }
      } else {
        if (nvert <= PGL.MIN_ARRAYCOPY_SIZE) {
          // Copying elements one by one instead of using arrayCopy is more
          // efficient for few vertices...
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
            PApplet.arrayCopy(in.vertices, 3 * inIdx,
                              polyVertices, 4 * tessIdx, 3);
            polyVertices[4 * tessIdx + 3] = 1;
          }
          PApplet.arrayCopy(in.normals, 3 * i0,
                            polyNormals, 3 * firstPolyVertex, 3 * nvert);
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
          polyTexCoords[index++] = u;
          polyTexCoords[index  ] = v;

          polyAmbient[tessIdx] = in.ambient[inIdx];
          polySpecular[tessIdx] = in.specular[inIdx];
          polyEmissive[tessIdx] = in.emissive[inIdx];
          polyShininess[tessIdx] = in.shininess[inIdx];
        }
      } else {
        PApplet.arrayCopy(in.colors, i0,
                          polyColors, firstPolyVertex, nvert);
        PApplet.arrayCopy(in.texcoords, 2 * i0,
                          polyTexCoords, 2 * firstPolyVertex, 2 * nvert);
        PApplet.arrayCopy(in.ambient, i0,
                          polyAmbient, firstPolyVertex, nvert);
        PApplet.arrayCopy(in.specular, i0,
                          polySpecular, firstPolyVertex, nvert);
        PApplet.arrayCopy(in.emissive, i0,
                          polyEmissive, firstPolyVertex, nvert);
        PApplet.arrayCopy(in.shininess, i0,
                          polyShininess, firstPolyVertex, nvert);
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
          polyVertices[index++] = x*tr.m00 + y*tr.m01 + tr.m02;
          polyVertices[index  ] = x*tr.m10 + y*tr.m11 + tr.m12;

          index = 3 * i;
          polyNormals[index++] = nx*tr.m00 + ny*tr.m01;
          polyNormals[index  ] = nx*tr.m10 + ny*tr.m11;
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
          float xa = lineDirections[index++];
          float ya = lineDirections[index  ];

          float dx = xa - x;
          float dy = ya - y;

          index = 4 * i;
          lineVertices[index++] = x*tr.m00 + y*tr.m01 + tr.m02;
          lineVertices[index  ] = x*tr.m10 + y*tr.m11 + tr.m12;

          index = 4 * i;
          lineDirections[index++] = dx*tr.m00 + dy*tr.m01;
          lineDirections[index  ] = dx*tr.m10 + dy*tr.m11;
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
          pointVertices[index++] = x*tr.m00 + y*tr.m01 + tr.m02;
          pointVertices[index  ] = x*tr.m10 + y*tr.m11 + tr.m12;
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
          polyVertices[index++] = x*tr.m00 + y*tr.m01 + z*tr.m02 + w*tr.m03;
          polyVertices[index++] = x*tr.m10 + y*tr.m11 + z*tr.m12 + w*tr.m13;
          polyVertices[index++] = x*tr.m20 + y*tr.m21 + z*tr.m22 + w*tr.m23;
          polyVertices[index  ] = x*tr.m30 + y*tr.m31 + z*tr.m32 + w*tr.m33;

          index = 3 * i;
          polyNormals[index++] = nx*tr.m00 + ny*tr.m01 + nz*tr.m02;
          polyNormals[index++] = nx*tr.m10 + ny*tr.m11 + nz*tr.m12;
          polyNormals[index  ] = nx*tr.m20 + ny*tr.m21 + nz*tr.m22;
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
          float xa = lineDirections[index++];
          float ya = lineDirections[index++];
          float za = lineDirections[index  ];

          float dx = xa - x;
          float dy = ya - y;
          float dz = za - z;

          index = 4 * i;
          lineVertices[index++] = x*tr.m00 + y*tr.m01 + z*tr.m02 + w*tr.m03;
          lineVertices[index++] = x*tr.m10 + y*tr.m11 + z*tr.m12 + w*tr.m13;
          lineVertices[index++] = x*tr.m20 + y*tr.m21 + z*tr.m22 + w*tr.m23;
          lineVertices[index  ] = x*tr.m30 + y*tr.m31 + z*tr.m32 + w*tr.m33;

          index = 4 * i;
          lineDirections[index++] = dx*tr.m00 + dy*tr.m01 + dz*tr.m02;
          lineDirections[index++] = dx*tr.m10 + dy*tr.m11 + dz*tr.m12;
          lineDirections[index  ] = dx*tr.m20 + dy*tr.m21 + dz*tr.m22;
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
          pointVertices[index++] = x*tr.m00 + y*tr.m01 + z*tr.m02 + w*tr.m03;
          pointVertices[index++] = x*tr.m10 + y*tr.m11 + z*tr.m12 + w*tr.m13;
          pointVertices[index++] = x*tr.m20 + y*tr.m21 + z*tr.m22 + w*tr.m23;
          pointVertices[index  ] = x*tr.m30 + y*tr.m31 + z*tr.m32 + w*tr.m33;
        }
      }
    }
  }

  // Generates tessellated geometry given a batch of input vertices.
  static protected class Tessellator {
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
    float transformScale;
    boolean is2D, is3D;
    protected PGraphicsOpenGL pg;

    int[] rawIndices;
    int rawSize;
    int[] dupIndices;
    int dupCount;

    int firstPolyIndexCache;
    int lastPolyIndexCache;
    int firstLineIndexCache;
    int lastLineIndexCache;
    int firstPointIndexCache;
    int lastPointIndexCache;

    // Accessor arrays to get the geometry data needed to tessellate the
    // strokes, it can point to either the input geometry, or the internal
    // path vertices generated in the polygon discretization.
    float[] strokeVertices;
    int[] strokeColors;
    float[] strokeWeights;

    // Path vertex data that results from discretizing a polygon (i.e.: turning
    // bezier, quadratic, and curve vertices into "regular" vertices).
    int pathVertexCount;
    float[] pathVertices;
    int[] pathColors;
    float[] pathWeights;
    int beginPath;

    public Tessellator() {
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

    void setTexCache(TexCache texCache, PImage prevTexImage,
                     PImage newTexImage) {
      this.texCache = texCache;
      this.prevTexImage = prevTexImage;
      this.newTexImage = newTexImage;
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

    void setStrokeCap(int strokeCap) {
      this.strokeCap = strokeCap;
    }

    void setStrokeJoin(int strokeJoin) {
      this.strokeJoin = strokeJoin;
    }

    void setAccurate2DStrokes(boolean accurate) {
      this.accurate2DStrokes = accurate;
    }

    protected void setRenderer(PGraphicsOpenGL pg) {
      this.pg = pg;
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
      transformScale = -1;
    }

    void resetCurveVertexCount() {
      pg.curveVertexCount = 0;
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
      int nInVert = in.vertexCount;
      if (stroke && 1 <= nInVert) {
        // Each point generates a separate triangle fan.
        // The number of triangles of each fan depends on the
        // stroke weight of the point.
        int nPtVert =
          PApplet.min(MAX_POINT_ACCURACY, PApplet.max(MIN_POINT_ACCURACY,
                      (int) (TWO_PI * strokeWeight /
                      POINT_ACCURACY_FACTOR))) + 1;
        if (PGL.MAX_VERTEX_INDEX1 <= nPtVert) {
          throw new RuntimeException("Error in point tessellation.");
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
      for (int i = 0; i < in.vertexCount; i++) {
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
        tess.pointOffsets[2 * attribIdx + 0] = 0;
        tess.pointOffsets[2 * attribIdx + 1] = 0;
        attribIdx++;
        float val = 0;
        float inc = (float) SINCOS_LENGTH / perim;
        for (int k = 0; k < perim; k++) {
          tess.pointOffsets[2 * attribIdx + 0] =
            0.5f * cosLUT[(int) val] * strokeWeight;
          tess.pointOffsets[2 * attribIdx + 1] =
            0.5f * sinLUT[(int) val] * strokeWeight;
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
      for (int i = 0; i < in.vertexCount; i++) {
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
        tess.setPolyVertex(vertIdx, x0, y0, 0, rgba, false);
        vertIdx++;
        for (int k = 0; k < perim; k++) {
          tess.setPolyVertex(vertIdx,
                             x0 + 0.5f * cosLUT[(int) val] * strokeWeight,
                             y0 + 0.5f * sinLUT[(int) val] * strokeWeight,
                             0, rgba, false);
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
      int nInVert = in.vertexCount;
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
      for (int i = 0; i < in.vertexCount; i++) {
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
        tess.pointOffsets[2 * attribIdx + 0] = 0;
        tess.pointOffsets[2 * attribIdx + 1] = 0;
        attribIdx++;
        for (int k = 0; k < 4; k++) {
          tess.pointOffsets[2 * attribIdx + 0] =
            0.5f * QUAD_POINT_SIGNS[k][0] * strokeWeight;
          tess.pointOffsets[2 * attribIdx + 1] =
            0.5f * QUAD_POINT_SIGNS[k][1] * strokeWeight;
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
      boolean clamp = clampSquarePoints2D();
      int vertIdx = tess.firstPolyVertex;
      int indIdx = tess.firstPolyIndex;
      IndexCache cache = tess.polyIndexCache;
      int index = in.renderMode == RETAINED ? cache.addNew() : cache.getLast();
      firstPointIndexCache = index;
      for (int i = 0; i < in.vertexCount; i++) {
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

        tess.setPolyVertex(vertIdx, x0, y0, 0, rgba, clamp);
        vertIdx++;
        for (int k = 0; k < nvert - 1; k++) {
          tess.setPolyVertex(vertIdx,
                             x0 + 0.5f * QUAD_POINT_SIGNS[k][0] * strokeWeight,
                             y0 + 0.5f * QUAD_POINT_SIGNS[k][1] * strokeWeight,
                             0, rgba, clamp);
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

    boolean clamp2D() {
      return is2D && tess.renderMode == IMMEDIATE &&
             zero(pg.modelview.m01) && zero(pg.modelview.m10);
    }

    boolean clampSquarePoints2D() {
      return clamp2D();
    }

    // -----------------------------------------------------------------
    //
    // Line tessellation

    void tessellateLines() {
      int nInVert = in.vertexCount;
      if (stroke && 2 <= nInVert) {
        strokeVertices = in.vertices;
        strokeColors = in.strokeColors;
        strokeWeights = in.strokeWeights;
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

      tess.lineVertexCheck(nvert);
      tess.lineIndexCheck(nind);
      int index = in.renderMode == RETAINED ? tess.lineIndexCache.addNew() :
                                              tess.lineIndexCache.getLast();
      firstLineIndexCache = index;
      for (int ln = 0; ln < lineCount; ln++) {
        int i0 = 2 * ln + 0;
        int i1 = 2 * ln + 1;
        index = addLineSegment3D(i0, i1, index, null, false);
      }
      lastLineIndexCache = index;
    }

    void tessellateLines2D(int lineCount) {
      int nvert = lineCount * 4;
      int nind = lineCount * 2 * 3;

      if (noCapsJoins(nvert)) {
        tess.polyVertexCheck(nvert);
        tess.polyIndexCheck(nind);
        int index = in.renderMode == RETAINED ? tess.polyIndexCache.addNew() :
                                                tess.polyIndexCache.getLast();
        firstLineIndexCache = index;
        if (firstPolyIndexCache == -1) firstPolyIndexCache = index; // If the geometry has no fill, needs the first poly index.
        boolean clamp = clampLines2D(lineCount);
        for (int ln = 0; ln < lineCount; ln++) {
          int i0 = 2 * ln + 0;
          int i1 = 2 * ln + 1;
          index = addLineSegment2D(i0, i1, index, false, clamp);
        }
        lastLineIndexCache = lastPolyIndexCache = index;
      } else { // full stroking algorithm
        LinePath path = new LinePath(LinePath.WIND_NON_ZERO);
        for (int ln = 0; ln < lineCount; ln++) {
          int i0 = 2 * ln + 0;
          int i1 = 2 * ln + 1;
          path.moveTo(in.vertices[3 * i0 + 0], in.vertices[3 * i0 + 1],
                      in.strokeColors[i0]);
          path.lineTo(in.vertices[3 * i1 + 0], in.vertices[3 * i1 + 1],
                      in.strokeColors[i1]);
        }
        tessellateLinePath(path);
      }
    }

    boolean clampLines2D(int lineCount) {
      boolean res = clamp2D();
      if (res) {
        for (int ln = 0; ln < lineCount; ln++) {
          int i0 = 2 * ln + 0;
          int i1 = 2 * ln + 1;
          res = segmentIsAxisAligned(i0, i1);
          if (!res) break;
        }
      }
      return res;
    }

    void tessellateLineStrip() {
      int nInVert = in.vertexCount;
      if (stroke && 2 <= nInVert) {
        strokeVertices = in.vertices;
        strokeColors = in.strokeColors;
        strokeWeights = in.strokeWeights;
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
      int nBevelTr = noCapsJoins() ? 0 : (lineCount - 1);
      int nvert = lineCount * 4 + nBevelTr;
      int nind = lineCount * 2 * 3 + nBevelTr * 2 * 3;

      tess.lineVertexCheck(nvert);
      tess.lineIndexCheck(nind);
      int index = in.renderMode == RETAINED ? tess.lineIndexCache.addNew() :
                                              tess.lineIndexCache.getLast();
      firstLineIndexCache = index;
      int i0 = 0;
      short[] lastInd = {-1, -1};
      for (int ln = 0; ln < lineCount; ln++) {
        int i1 = ln + 1;
        if (0 < nBevelTr) {
          index = addLineSegment3D(i0, i1, index, lastInd, false);
        } else {
          index = addLineSegment3D(i0, i1, index, null, false);
        }
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
        int index = in.renderMode == RETAINED ? tess.polyIndexCache.addNew() :
                                                tess.polyIndexCache.getLast();
        firstLineIndexCache = index;
        if (firstPolyIndexCache == -1) firstPolyIndexCache = index; // If the geometry has no fill, needs the first poly index.
        int i0 = 0;
        boolean clamp = clampLineStrip2D(lineCount);
        for (int ln = 0; ln < lineCount; ln++) {
          int i1 = ln + 1;
          index = addLineSegment2D(i0, i1, index, false, clamp);
          i0 = i1;
        }
        lastLineIndexCache = lastPolyIndexCache = index;
      } else {  // full stroking algorithm
        LinePath path = new LinePath(LinePath.WIND_NON_ZERO);
        path.moveTo(in.vertices[0], in.vertices[1], in.strokeColors[0]);
        for (int ln = 0; ln < lineCount; ln++) {
          int i1 = ln + 1;
          path.lineTo(in.vertices[3 * i1 + 0], in.vertices[3 * i1 + 1],
                      in.strokeColors[i1]);
        }
        tessellateLinePath(path);
      }
    }

    boolean clampLineStrip2D(int lineCount) {
      boolean res = clamp2D();
      if (res) {
        for (int ln = 0; ln < lineCount; ln++) {
          res = segmentIsAxisAligned(0, ln + 1);
          if (!res) break;
        }
      }
      return res;
    }

    void tessellateLineLoop() {
      int nInVert = in.vertexCount;
      if (stroke && 2 <= nInVert) {
        strokeVertices = in.vertices;
        strokeColors = in.strokeColors;
        strokeWeights = in.strokeWeights;
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
      int nBevelTr = noCapsJoins() ? 0 : lineCount;
      int nvert = lineCount * 4 + nBevelTr;
      int nind = lineCount * 2 * 3 + nBevelTr * 2 * 3;

      tess.lineVertexCheck(nvert);
      tess.lineIndexCheck(nind);
      int index = in.renderMode == RETAINED ? tess.lineIndexCache.addNew() :
                                              tess.lineIndexCache.getLast();
      firstLineIndexCache = index;
      int i0 = 0;
      short[] lastInd = {-1, -1};
      short firstInd = -1;
      for (int ln = 0; ln < lineCount - 1; ln++) {
        int i1 = ln + 1;
        if (0 < nBevelTr) {
          index = addLineSegment3D(i0, i1, index, lastInd, false);
          if (ln == 0) firstInd = (short)(lastInd[0] - 2);
        } else {
          index = addLineSegment3D(i0, i1, index, null, false);
        }
        i0 = i1;
      }
      index = addLineSegment3D(0, in.vertexCount - 1, index, lastInd, false);
      if (0 < nBevelTr) {
        index = addBevel3D(0, index, lastInd, firstInd, false);
      }
      lastLineIndexCache = index;
    }

    void tessellateLineLoop2D(int lineCount) {
      int nvert = lineCount * 4;
      int nind = lineCount * 2 * 3;

      if (noCapsJoins(nvert)) {
        tess.polyVertexCheck(nvert);
        tess.polyIndexCheck(nind);
        int index = in.renderMode == RETAINED ? tess.polyIndexCache.addNew() :
                                                tess.polyIndexCache.getLast();
        firstLineIndexCache = index;
        if (firstPolyIndexCache == -1) firstPolyIndexCache = index; // If the geometry has no fill, needs the first poly index.
        int i0 = 0;
        boolean clamp = clampLineLoop2D(lineCount);
        for (int ln = 0; ln < lineCount - 1; ln++) {
          int i1 = ln + 1;
          index = addLineSegment2D(i0, i1, index, false, clamp);
          i0 = i1;
        }
        index = addLineSegment2D(0, in.vertexCount - 1, index, false, clamp);
        lastLineIndexCache = lastPolyIndexCache = index;
      } else { // full stroking algorithm
        LinePath path = new LinePath(LinePath.WIND_NON_ZERO);
        path.moveTo(in.vertices[0], in.vertices[1], in.strokeColors[0]);
        for (int ln = 0; ln < lineCount - 1; ln++) {
          int i1 = ln + 1;
          path.lineTo(in.vertices[3 * i1 + 0], in.vertices[3 * i1 + 1],
                      in.strokeColors[i1]);
        }
        path.closePath();
        tessellateLinePath(path);
      }
    }

    boolean clampLineLoop2D(int lineCount) {
      boolean res = clamp2D();
      if (res) {
        for (int ln = 0; ln < lineCount; ln++) {
          res = segmentIsAxisAligned(0, ln + 1);
          if (!res) break;
        }
      }
      return res;
    }

    void tessellateEdges() {
      if (stroke) {
        if (in.edgeCount == 0) return;
        strokeVertices = in.vertices;
        strokeColors = in.strokeColors;
        strokeWeights = in.strokeWeights;
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
      boolean bevel = !noCapsJoins();
      int nInVert = in.getNumEdgeVertices(bevel);
      int nInInd = in.getNumEdgeIndices(bevel);

      tess.lineVertexCheck(nInVert);
      tess.lineIndexCheck(nInInd);
      int index = in.renderMode == RETAINED ? tess.lineIndexCache.addNew() :
                                              tess.lineIndexCache.getLast();
      firstLineIndexCache = index;
      short[] lastInd = {-1, -1};
      short firstInd = -1;
      for (int i = 0; i <= in.edgeCount - 1; i++) {
        int[] edge = in.edges[i];
        int i0 = edge[0];
        int i1 = edge[1];
        if (bevel) {
          if (edge[2] == EDGE_CLOSE) {
            index = addBevel3D(edge[1], index, lastInd, firstInd, false);
            lastInd[0] = lastInd[1] = -1; // No join with next line segment.
          } else {
            index = addLineSegment3D(i0, i1, index, lastInd, false);
            if (edge[2] == EDGE_START) firstInd = (short)(lastInd[0] - 2);
            if (edge[2] == EDGE_STOP || edge[2] == EDGE_SINGLE) {
              lastInd[0] = lastInd[1] = -1; // No join with next line segment.
            }
          }
        } else if (edge[2] != EDGE_CLOSE) {
          index = addLineSegment3D(i0, i1, index, null, false);
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
        int index = in.renderMode == RETAINED ? tess.polyIndexCache.addNew() :
                                                tess.polyIndexCache.getLast();
        firstLineIndexCache = index;
        if (firstPolyIndexCache == -1) firstPolyIndexCache = index; // If the geometry has no fill, needs the first poly index.
        boolean clamp = clampEdges2D();
        for (int i = 0; i <= in.edgeCount - 1; i++) {
          int[] edge = in.edges[i];
          if (edge[2] == EDGE_CLOSE) continue; // ignoring edge closures when not doing caps or joins.
          int i0 = edge[0];
          int i1 = edge[1];
          index = addLineSegment2D(i0, i1, index, false, clamp);
        }
        lastLineIndexCache = lastPolyIndexCache = index;
      } else { // full stroking algorithm
        LinePath path = new LinePath(LinePath.WIND_NON_ZERO);
        for (int i = 0; i <= in.edgeCount - 1; i++) {
          int[] edge = in.edges[i];
          int i0 = edge[0];
          int i1 = edge[1];
          switch (edge[2]) {
          case EDGE_MIDDLE:
            path.lineTo(strokeVertices[3 * i1 + 0], strokeVertices[3 * i1 + 1],
                        strokeColors[i1]);
            break;
          case EDGE_START:
            path.moveTo(strokeVertices[3 * i0 + 0], strokeVertices[3 * i0 + 1],
                        strokeColors[i0]);
            path.lineTo(strokeVertices[3 * i1 + 0], strokeVertices[3 * i1 + 1],
                        strokeColors[i1]);
            break;
          case EDGE_STOP:
            path.lineTo(strokeVertices[3 * i1 + 0], strokeVertices[3 * i1 + 1],
                        strokeColors[i1]);
            path.moveTo(strokeVertices[3 * i1 + 0], strokeVertices[3 * i1 + 1],
                        strokeColors[i1]);
            break;
          case EDGE_SINGLE:
            path.moveTo(strokeVertices[3 * i0 + 0], strokeVertices[3 * i0 + 1],
                        strokeColors[i0]);
            path.lineTo(strokeVertices[3 * i1 + 0], strokeVertices[3 * i1 + 1],
                        strokeColors[i1]);
            path.moveTo(strokeVertices[3 * i1 + 0], strokeVertices[3 * i1 + 1],
                        strokeColors[i1]);
            break;
          case EDGE_CLOSE:
            path.closePath();
            break;
          }
        }
        tessellateLinePath(path);
      }
    }

    boolean clampEdges2D() {
      boolean res = clamp2D();
      if (res) {
        for (int i = 0; i <= in.edgeCount - 1; i++) {
          int[] edge = in.edges[i];
          if (edge[2] == EDGE_CLOSE) continue;
          int i0 = edge[0];
          int i1 = edge[1];
          res = segmentIsAxisAligned(strokeVertices, i0, i1);
          if (!res) break;
        }
      }
      return res;
    }

    // Adding the data that defines a quad starting at vertex i0 and
    // ending at i1.
    int addLineSegment3D(int i0, int i1, int index, short[] lastInd,
                         boolean constStroke) {
      IndexCache cache = tess.lineIndexCache;
      int count = cache.vertexCount[index];
      boolean addBevel = lastInd != null && -1 < lastInd[0] && -1 < lastInd[1];
      boolean newCache = false;
      if (PGL.MAX_VERTEX_INDEX1 <= count + 4 + (addBevel ? 1 : 0)) {
        // We need to start a new index block for this line.
        index = cache.addNew();
        count = 0;
        newCache = true;
      }
      int iidx = cache.indexOffset[index] + cache.indexCount[index];
      int vidx = cache.vertexOffset[index] + cache.vertexCount[index];
      int color, color0;
      float weight;

      color0 = color = constStroke ? strokeColor : strokeColors[i0];
      weight = constStroke ? strokeWeight : strokeWeights[i0];

      tess.setLineVertex(vidx++, strokeVertices, i0, i1, color, +weight/2);
      tess.lineIndices[iidx++] = (short) (count + 0);

      tess.setLineVertex(vidx++, strokeVertices, i0, i1, color, -weight/2);
      tess.lineIndices[iidx++] = (short) (count + 1);

      color = constStroke ? strokeColor : strokeColors[i1];
      weight = constStroke ? strokeWeight : strokeWeights[i1];

      tess.setLineVertex(vidx++, strokeVertices, i1, i0, color, -weight/2);
      tess.lineIndices[iidx++] = (short) (count + 2);

      // Starting a new triangle re-using prev vertices.
      tess.lineIndices[iidx++] = (short) (count + 2);
      tess.lineIndices[iidx++] = (short) (count + 1);

      tess.setLineVertex(vidx++, strokeVertices, i1, i0, color, +weight/2);
      tess.lineIndices[iidx++] = (short) (count + 3);

      cache.incCounts(index, 6, 4);

      if (lastInd != null) {
        if (-1 < lastInd[0] && -1 < lastInd[1]) {
          // Adding bevel triangles
          tess.setLineVertex(vidx, strokeVertices, i0, color0);

          if (newCache) {
            PGraphics.showWarning(TOO_LONG_STROKE_PATH_ERROR);

            // TODO: Fix this situation, the vertices from the previous cache
            // block should be copied in the newly created one.
            tess.lineIndices[iidx++] = (short) (count + 4);
            tess.lineIndices[iidx++] = (short) (count + 0);
            tess.lineIndices[iidx++] = (short) (count + 0);

            tess.lineIndices[iidx++] = (short) (count + 4);
            tess.lineIndices[iidx++] = (short) (count + 1);
            tess.lineIndices[iidx  ] = (short) (count + 1);
          } else {
            tess.lineIndices[iidx++] = (short) (count + 4);
            tess.lineIndices[iidx++] = lastInd[0];
            tess.lineIndices[iidx++] = (short) (count + 0);

            tess.lineIndices[iidx++] = (short) (count + 4);
            tess.lineIndices[iidx++] = lastInd[1];
            tess.lineIndices[iidx  ] = (short) (count + 1);
          }

          cache.incCounts(index, 6, 1);
        }

        // Vertices for next bevel
        lastInd[0] = (short) (count + 2);
        lastInd[1] = (short) (count + 3);
      }
      return index;
    }

    int addBevel3D(int i0, int index, short[] lastInd, short firstInd,
                   boolean constStroke) {
      IndexCache cache = tess.lineIndexCache;
      int count = cache.vertexCount[index];
      boolean addBevel = lastInd != null && -1 < lastInd[0] && -1 < lastInd[1];
      boolean newCache = false;
      if (PGL.MAX_VERTEX_INDEX1 <= count + (addBevel ? 1 : 0)) {
        // We need to start a new index block for this line.
        index = cache.addNew();
        count = 0;
        newCache = true;
      }
      int iidx = cache.indexOffset[index] + cache.indexCount[index];
      int vidx = cache.vertexOffset[index] + cache.vertexCount[index];
      int color0 = constStroke ? strokeColor : strokeColors[i0];

      if (lastInd != null) {
        if (-1 < lastInd[0] && -1 < lastInd[1]) {
          tess.setLineVertex(vidx, strokeVertices, i0, color0);

          if (newCache) {
            PGraphics.showWarning(TOO_LONG_STROKE_PATH_ERROR);

            // TODO: Fix this situation, the vertices from the previous cache
            // block should be copied in the newly created one.
//            tess.lineIndices[iidx++] = (short) (count + 4);
//            tess.lineIndices[iidx++] = (short) (count + 0);
//            tess.lineIndices[iidx++] = (short) (count + 0);
//
//            tess.lineIndices[iidx++] = (short) (count + 4);
//            tess.lineIndices[iidx++] = (short) (count + 1);
//            tess.lineIndices[iidx  ] = (short) (count + 1);
          } else {
            tess.lineIndices[iidx++] = (short) (count + 0);
            tess.lineIndices[iidx++] = lastInd[0];
            tess.lineIndices[iidx++] = (short) (firstInd + 0);

            tess.lineIndices[iidx++] = (short) (count + 0);
            tess.lineIndices[iidx++] = lastInd[1];
            tess.lineIndices[iidx  ] = (short) (firstInd + 1);
          }

          cache.incCounts(index, 6, 1);
        }
      }

      return index;
    }

    // Adding the data that defines a quad starting at vertex i0 and
    // ending at i1, in the case of pure 2D renderers (line geometry
    // is added to the poly arrays).
    int addLineSegment2D(int i0, int i1, int index,
                         boolean constStroke, boolean clamp) {
      IndexCache cache = tess.polyIndexCache;
      int count = cache.vertexCount[index];
      if (PGL.MAX_VERTEX_INDEX1 <= count + 4) {
        // We need to start a new index block for this line.
        index = cache.addNew();
        count = 0;
      }
      int iidx = cache.indexOffset[index] + cache.indexCount[index];
      int vidx = cache.vertexOffset[index] + cache.vertexCount[index];

      int color = constStroke ? strokeColor : strokeColors[i0];
      float weight = constStroke ? strokeWeight : strokeWeights[i0];
      if (subPixelStroke(weight)) clamp = false;

      float x0 = strokeVertices[3 * i0 + 0];
      float y0 = strokeVertices[3 * i0 + 1];

      float x1 = strokeVertices[3 * i1 + 0];
      float y1 = strokeVertices[3 * i1 + 1];

      // Calculating direction and normal of the line.
      float dirx = x1 - x0;
      float diry = y1 - y0;
      float llen = PApplet.sqrt(dirx * dirx + diry * diry);
      float normx = 0, normy = 0;
      float dirdx = 0, dirdy = 0;
      if (nonZero(llen)) {
        normx = -diry / llen;
        normy = +dirx / llen;

        // Displacement along the direction of the line to force rounding to next
        // integer and so making sure that no pixels are missing, some relevant
        // links:
        // http://stackoverflow.com/questions/10040961/opengl-pixel-perfect-2d-drawing
        // http://msdn.microsoft.com/en-us/library/dd374282(VS.85)
        dirdx = (dirx / llen) * PApplet.min(0.75f, weight/2);
        dirdy = (diry / llen) * PApplet.min(0.75f, weight/2);
      }

      float normdx = normx * weight/2;
      float normdy = normy * weight/2;

      tess.setPolyVertex(vidx++, x0 + normdx - dirdx, y0 + normdy - dirdy,
                         0, color, clamp);
      tess.polyIndices[iidx++] = (short) (count + 0);

      tess.setPolyVertex(vidx++, x0 - normdx - dirdx, y0 - normdy - dirdy,
                         0, color, clamp);
      tess.polyIndices[iidx++] = (short) (count + 1);

      if (clamp) {
        // Check for degeneracy due to coordinate clamping
        float xac = tess.polyVertices[4 * (vidx - 2) + 0];
        float yac = tess.polyVertices[4 * (vidx - 2) + 1];
        float xbc = tess.polyVertices[4 * (vidx - 1) + 0];
        float ybc = tess.polyVertices[4 * (vidx - 1) + 1];
        if (same(xac, xbc) && same(yac, ybc)) {
          unclampLine2D(vidx - 2, x0 + normdx - dirdx, y0 + normdy - dirdy);
          unclampLine2D(vidx - 1, x0 - normdx - dirdx, y0 - normdy - dirdy);
        }
      }

      if (!constStroke) {
        color =  strokeColors[i1];
        weight = strokeWeights[i1];
        normdx = normx * weight/2;
        normdy = normy * weight/2;
        if (subPixelStroke(weight)) clamp = false;
      }

      tess.setPolyVertex(vidx++, x1 - normdx + dirdx, y1 - normdy + dirdy,
                         0, color, clamp);
      tess.polyIndices[iidx++] = (short) (count + 2);

      // Starting a new triangle re-using prev vertices.
      tess.polyIndices[iidx++] = (short) (count + 2);
      tess.polyIndices[iidx++] = (short) (count + 0);

      tess.setPolyVertex(vidx++, x1 + normdx + dirdx, y1 + normdy + dirdy,
                         0, color, clamp);
      tess.polyIndices[iidx++] = (short) (count + 3);

      if (clamp) {
        // Check for degeneracy due to coordinate clamping
        float xac = tess.polyVertices[4 * (vidx - 2) + 0];
        float yac = tess.polyVertices[4 * (vidx - 2) + 1];
        float xbc = tess.polyVertices[4 * (vidx - 1) + 0];
        float ybc = tess.polyVertices[4 * (vidx - 1) + 1];
        if (same(xac, xbc) && same(yac, ybc)) {
          unclampLine2D(vidx - 2, x1 - normdx + dirdx, y1 - normdy + dirdy);
          unclampLine2D(vidx - 1, x1 + normdx + dirdx, y1 + normdy + dirdy);
        }
      }

      cache.incCounts(index, 6, 4);
      return index;
    }

    void unclampLine2D(int tessIdx, float x, float y) {
      PMatrix3D mm = pg.modelview;
      int index = 4 * tessIdx;
      tess.polyVertices[index++] = x*mm.m00 + y*mm.m01 + mm.m03;
      tess.polyVertices[index++] = x*mm.m10 + y*mm.m11 + mm.m13;
    }

    boolean noCapsJoins(int nInVert) {
      if (!accurate2DStrokes) {
        return true;
      } else if (PGL.MAX_CAPS_JOINS_LENGTH <= nInVert) {
        // The line path is too long, so it could make the GLU tess
        // to run out of memory, so full caps and joins are disabled.
        return true;
      } else {
        return noCapsJoins();
      }
    }

    boolean subPixelStroke(float weight) {
      float sw = transformScale() * weight;
      return PApplet.abs(sw - (int)sw) > 0;
    }

    boolean noCapsJoins() {
      // The stroke weight is scaled so it correspons to the current
      // "zoom level" being applied on the geometry due to scaling:
      return tess.renderMode == IMMEDIATE &&
             transformScale() * strokeWeight < PGL.MIN_CAPS_JOINS_WEIGHT;
    }

    float transformScale() {
      if (-1 < transformScale) return transformScale;

      // Volumetric scaling factor that is associated to the current
      // transformation matrix, which is given by the absolute value of its
      // determinant:
      float factor = 1;

      if (transform != null) {
        if (transform instanceof PMatrix2D) {
          PMatrix2D tr = (PMatrix2D)transform;
          float areaScaleFactor = Math.abs(tr.m00 * tr.m11 - tr.m01 * tr.m10);
          factor = (float) Math.sqrt(areaScaleFactor);
        } else if (transform instanceof PMatrix3D) {
          PMatrix3D tr = (PMatrix3D)transform;
          float volumeScaleFactor =
            Math.abs(tr.m00 * (tr.m11 * tr.m22 - tr.m12 * tr.m21) +
                     tr.m01 * (tr.m12 * tr.m20 - tr.m10 * tr.m22) +
                     tr.m02 * (tr.m10 * tr.m21 - tr.m11 * tr.m20));
          factor = (float) Math.pow(volumeScaleFactor, 1.0f / 3.0f);
        }
      }

      return transformScale = factor;
    }

    boolean segmentIsAxisAligned(int i0, int i1) {
      return zero(in.vertices[3 * i0 + 0] - in.vertices[3 * i1 + 0]) ||
             zero(in.vertices[3 * i0 + 1] - in.vertices[3 * i1 + 1]);
    }

    boolean segmentIsAxisAligned(float[] vertices, int i0, int i1) {
      return zero(vertices[3 * i0 + 0] - vertices[3 * i1 + 0]) ||
             zero(vertices[3 * i0 + 1] - vertices[3 * i1 + 1]);
    }

    // -----------------------------------------------------------------
    //
    // Polygon primitives tessellation

    void tessellateTriangles() {
      beginTex();
      int nTri = in.vertexCount / 3;
      if (fill && 1 <= nTri) {
        int nInInd = 3 * nTri;
        setRawSize(nInInd);
        int idx = 0;
        boolean clamp = clampTriangles();
        for (int i = 0; i < 3 * nTri; i++) {
          rawIndices[idx++] = i;
        }
        splitRawIndices(clamp);
      }
      endTex();
      tessellateEdges();
    }

    boolean clampTriangles() {
      boolean res = clamp2D();
      if (res) {
        int nTri = in.vertexCount / 3;
        for (int i = 0; i < nTri; i++) {
          int i0 = 3 * i + 0;
          int i1 = 3 * i + 1;
          int i2 = 3 * i + 2;
          int count = 0;
          if (segmentIsAxisAligned(i0, i1)) count++;
          if (segmentIsAxisAligned(i0, i2)) count++;
          if (segmentIsAxisAligned(i1, i2)) count++;
          res = 1 < count;
          if (!res) break;
        }
      }
      return res;
    }

    void tessellateTriangles(int[] indices) {
      beginTex();
      int nInVert = in.vertexCount;
      if (fill && 3 <= nInVert) {
        int nInInd = indices.length;
        setRawSize(nInInd);
        PApplet.arrayCopy(indices, rawIndices, nInInd);
        boolean clamp = clampTriangles(indices);
        splitRawIndices(clamp);
      }
      endTex();
      tessellateEdges();
    }

    boolean clampTriangles(int[] indices) {
      boolean res = clamp2D();
      if (res) {
        int nTri = indices.length;
        for (int i = 0; i < nTri; i++) {
          int i0 = indices[3 * i + 0];
          int i1 = indices[3 * i + 1];
          int i2 = indices[3 * i + 2];
          int count = 0;
          if (segmentIsAxisAligned(i0, i1)) count++;
          if (segmentIsAxisAligned(i0, i2)) count++;
          if (segmentIsAxisAligned(i1, i2)) count++;
          res = 1 < count;
          if (!res) break;
        }
      }
      return res;
    }

    void tessellateTriangleFan() {
      beginTex();
      int nInVert = in.vertexCount;
      if (fill && 3 <= nInVert) {
        int nInInd = 3 * (nInVert - 2);
        setRawSize(nInInd);
        int idx = 0;
        boolean clamp = clampTriangleFan();
        for (int i = 1; i < in.vertexCount - 1; i++) {
          rawIndices[idx++] = 0;
          rawIndices[idx++] = i;
          rawIndices[idx++] = i + 1;
        }
        splitRawIndices(clamp);
      }
      endTex();
      tessellateEdges();
    }

    boolean clampTriangleFan() {
      boolean res = clamp2D();
      if (res) {
        for (int i = 1; i < in.vertexCount - 1; i++) {
          int i0 = 0;
          int i1 = i;
          int i2 = i + 1;
          int count = 0;
          if (segmentIsAxisAligned(i0, i1)) count++;
          if (segmentIsAxisAligned(i0, i2)) count++;
          if (segmentIsAxisAligned(i1, i2)) count++;
          res = 1 < count;
          if (!res) break;
        }
      }
      return res;
    }

    void tessellateTriangleStrip() {
      beginTex();
      int nInVert = in.vertexCount;
      if (fill && 3 <= nInVert) {
        int nInInd = 3 * (nInVert - 2);
        setRawSize(nInInd);
        int idx = 0;
        boolean clamp = clampTriangleStrip();
        for (int i = 1; i < in.vertexCount - 1; i++) {
          rawIndices[idx++] = i;
          if (i % 2 == 0) {
            rawIndices[idx++] = i - 1;
            rawIndices[idx++] = i + 1;
          } else {
            rawIndices[idx++] = i + 1;
            rawIndices[idx++] = i - 1;
          }
        }
        splitRawIndices(clamp);
      }
      endTex();
      tessellateEdges();
    }

    boolean clampTriangleStrip() {
      boolean res = clamp2D();
      if (res) {
        for (int i = 1; i < in.vertexCount - 1; i++) {
          int i0 = i;
          int i1, i2;
          if (i % 2 == 0) {
            i1 = i - 1;
            i2 = i + 1;
          } else {
            i1 = i + 1;
            i2 = i - 1;
          }
          int count = 0;
          if (segmentIsAxisAligned(i0, i1)) count++;
          if (segmentIsAxisAligned(i0, i2)) count++;
          if (segmentIsAxisAligned(i1, i2)) count++;
          res = 1 < count;
          if (!res) break;
        }
      }
      return res;
    }

    void tessellateQuads() {
      beginTex();
      int quadCount = in.vertexCount / 4;
      if (fill && 1 <= quadCount) {
        int nInInd = 6 * quadCount;
        setRawSize(nInInd);
        int idx = 0;
        boolean clamp = clampQuads(quadCount);
        for (int qd = 0; qd < quadCount; qd++) {
          int i0 = 4 * qd + 0;
          int i1 = 4 * qd + 1;
          int i2 = 4 * qd + 2;
          int i3 = 4 * qd + 3;

          rawIndices[idx++] = i0;
          rawIndices[idx++] = i1;
          rawIndices[idx++] = i2;

          rawIndices[idx++] = i2;
          rawIndices[idx++] = i3;
          rawIndices[idx++] = i0;
        }
        splitRawIndices(clamp);
      }
      endTex();
      tessellateEdges();
    }

    boolean clampQuads(int quadCount) {
      boolean res = clamp2D();
      if (res) {
        for (int qd = 0; qd < quadCount; qd++) {
          int i0 = 4 * qd + 0;
          int i1 = 4 * qd + 1;
          int i2 = 4 * qd + 2;
          int i3 = 4 * qd + 3;
          res = segmentIsAxisAligned(i0, i1) &&
                segmentIsAxisAligned(i1, i2) &&
                segmentIsAxisAligned(i2, i3);
          if (!res) break;
        }
      }
      return res;
    }

    void tessellateQuadStrip() {
      beginTex();
      int quadCount = in.vertexCount / 2 - 1;
      if (fill && 1 <= quadCount) {
        int nInInd = 6 * quadCount;
        setRawSize(nInInd);
        int idx = 0;
        boolean clamp = clampQuadStrip(quadCount);
        for (int qd = 1; qd < quadCount + 1; qd++) {
          int i0 = 2 * (qd - 1);
          int i1 = 2 * (qd - 1) + 1;
          int i2 = 2 * qd + 1;
          int i3 = 2 * qd;

          rawIndices[idx++] = i0;
          rawIndices[idx++] = i1;
          rawIndices[idx++] = i3;

          rawIndices[idx++] = i1;
          rawIndices[idx++] = i2;
          rawIndices[idx++] = i3;
        }
        splitRawIndices(clamp);
      }
      endTex();
      tessellateEdges();
    }

    boolean clampQuadStrip(int quadCount) {
      boolean res = clamp2D();
      if (res) {
        for (int qd = 1; qd < quadCount + 1; qd++) {
          int i0 = 2 * (qd - 1);
          int i1 = 2 * (qd - 1) + 1;
          int i2 = 2 * qd + 1;
          int i3 = 2 * qd;
          res = segmentIsAxisAligned(i0, i1) &&
                segmentIsAxisAligned(i1, i2) &&
                segmentIsAxisAligned(i2, i3);
          if (!res) break;
        }
      }
      return res;
    }

    // Uses the raw indices to split the geometry into contiguous
    // index groups when the vertex indices become too large. The basic
    // idea of this algorithm is to scan through the array of raw indices
    // in groups of three vertices at the time (since we are always dealing
    // with triangles) and create a new offset in the index cache once the
    // index values go above the MAX_VERTEX_INDEX constant. The tricky part
    // is that triangles in the new group might refer to vertices in a
    // previous group. Since the index groups are by definition disjoint,
    // these vertices need to be duplicated at the end of the corresponding
    // region in the vertex array.
    //
    // Also to keep in mind, the ordering of the indices affects performance
    // take a look at some of this references:
    // http://gameangst.com/?p=9
    // http://home.comcast.net/~tom_forsyth/papers/fast_vert_cache_opt.html
    // http://www.ludicon.com/castano/blog/2009/02/optimal-grid-rendering/
    void splitRawIndices(boolean clamp) {
      tess.polyIndexCheck(rawSize);
      int offset = tess.firstPolyIndex;

      // Current index and vertex ranges
      int inInd0 = 0, inInd1 = 0;
      int inMaxVert0 = 0, inMaxVert1 = 0;

      int inMaxVertRef = inMaxVert0; // Reference vertex where last break split occurred
      int inMaxVertRel = -1;         // Position of vertices from last range relative to
                                     // split position.
      dupCount = 0;

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
        int ii0 = i0 - inMaxVertRef;
        int ii1 = i1 - inMaxVertRef;
        int ii2 = i2 - inMaxVertRef;

        // Vertex indices relative to the current group.
        int count = cache.vertexCount[index];
        int ri0, ri1, ri2;
        if (ii0 < 0) {
          addDupIndex(ii0);
          ri0 = ii0;
        } else ri0 = count + ii0;
        if (ii1 < 0) {
          addDupIndex(ii1);
          ri1 = ii1;
        } else ri1 = count + ii1;
        if (ii2 < 0) {
          addDupIndex(ii2);
          ri2 = ii2;
        } else ri2 = count + ii2;

        tess.polyIndices[offset + 3 * tr + 0] = (short) ri0;
        tess.polyIndices[offset + 3 * tr + 1] = (short) ri1;
        tess.polyIndices[offset + 3 * tr + 2] = (short) ri2;

        inInd1 = 3 * tr + 2;
        inMaxVert1 = PApplet.max(inMaxVert1, PApplet.max(i0, i1, i2));
        inMaxVert0 = PApplet.min(inMaxVert0, PApplet.min(i0, i1, i2));

        inMaxVertRel = PApplet.max(inMaxVertRel, PApplet.max(ri0, ri1, ri2));

        if ((PGL.MAX_VERTEX_INDEX1 - 3 <= inMaxVertRel + dupCount &&
             inMaxVertRel + dupCount < PGL.MAX_VERTEX_INDEX1) ||
            (tr == trCount - 1)) {
          // The vertex indices of the current group are about to
          // surpass the MAX_VERTEX_INDEX limit, or we are at the last triangle
          // so we need to wrap-up things anyways.

          int nondupCount = 0;
          if (0 < dupCount) {
            // Adjusting the negative indices so they correspond to vertices
            // added at the end of the block.
            for (int i = inInd0; i <= inInd1; i++) {
              int ri = tess.polyIndices[offset + i];
              if (ri < 0) {
                tess.polyIndices[offset + i] =
                  (short) (inMaxVertRel + 1 + dupIndexPos(ri));
              }
            }

            if (inMaxVertRef <= inMaxVert1) {
              // Copy non-duplicated vertices from current region first
              tess.addPolyVertices(in, inMaxVertRef, inMaxVert1, clamp);
              nondupCount = inMaxVert1 - inMaxVertRef + 1;
            }

            // Copy duplicated vertices from previous regions last
            for (int i = 0; i < dupCount; i++) {
              tess.addPolyVertex(in, dupIndices[i] + inMaxVertRef, clamp);
            }
          } else {
            // Copy non-duplicated vertices from current region first
            tess.addPolyVertices(in, inMaxVert0, inMaxVert1, clamp);
            nondupCount = inMaxVert1 - inMaxVert0 + 1;
          }

          // Increment counts:
          cache.incCounts(index, inInd1 - inInd0 + 1, nondupCount + dupCount);
          lastPolyIndexCache = index;

          // Prepare all variables to start next cache:
          index = -1;
          inMaxVertRel = -1;
          inMaxVertRef = inMaxVert1 + 1;
          inMaxVert0 = inMaxVertRef;
          inInd0 = inInd1 + 1;
          if (dupIndices != null) Arrays.fill(dupIndices, 0, dupCount, 0);
          dupCount = 0;
        }
      }
    }

    void addDupIndex(int idx) {
      if (dupIndices == null) {
        dupIndices = new int[16];
      }
      if (dupIndices.length == dupCount) {
        int n = dupCount << 1;

        int temp[] = new int[n];
        PApplet.arrayCopy(dupIndices, 0, temp, 0, dupCount);
        dupIndices = temp;
      }

      if (idx < dupIndices[0]) {
        // Add at the beginning
        for (int i = dupCount; i > 0; i--) dupIndices[i] = dupIndices[i - 1];
        dupIndices[0] = idx;
        dupCount++;
      } else if (dupIndices[dupCount - 1] < idx) {
        // Add at the end
        dupIndices[dupCount] = idx;
        dupCount++;
      } else {
        for (int i = 0; i < dupCount - 1; i++) {
          if (dupIndices[i] == idx) break;
          if (dupIndices[i] < idx && idx < dupIndices[i + 1]) {
            // Insert between i and i + 1:
            for (int j = dupCount; j > i + 1; j--) {
              dupIndices[j] = dupIndices[j - 1];
            }
            dupIndices[i + 1] = idx;
            dupCount++;
            break;
          }
        }
      }
    }

    int dupIndexPos(int idx) {
      for (int i = 0; i < dupCount; i++) {
        if (dupIndices[i] == idx) return i;
      }
      return 0;
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
          texCache.addTexture(newTexImage, firstTexIndex, firstTexCache,
                              lastIndex, lastCache);
        } else {
          texCache.setLastIndex(lastIndex, lastCache);
        }
      }
    }

    // -----------------------------------------------------------------
    //
    // Polygon tessellation, includes edge calculation and tessellation.

    void tessellatePolygon(boolean solid, boolean closed, boolean calcNormals) {
      beginTex();

      int nInVert = in.vertexCount;

      if (3 <= nInVert) {
        firstPolyIndexCache = -1;

        boolean clamp = clampPolygon();
        callback.init(in.renderMode == RETAINED, false, calcNormals, clamp);

        if (fill) {
          gluTess.beginPolygon();
          if (solid) {
            // Using NONZERO winding rule for solid polygons.
            gluTess.setWindingRule(PGL.TESS_WINDING_NONZERO);
          } else {
            // Using ODD winding rule to generate polygon with holes.
            gluTess.setWindingRule(PGL.TESS_WINDING_ODD);
          }
          gluTess.beginContour();
        }

        if (stroke) {
          beginPolygonStroke();
          beginStrokePath();
        }

        int i = 0;
        int c = 0;
        while (i < in.vertexCount) {
          int code = VERTEX;
          boolean brk = false;
          if (in.codes != null && c < in.codeCount) {
            code = in.codes[c++];
            if (code == BREAK && c < in.codeCount) {
              brk = true;
              code = in.codes[c++];
            }
          }

          if (brk) {
            if (stroke) {
              endStrokePath(closed);
              beginStrokePath();
            }
            if (fill) {
              gluTess.endContour();
              gluTess.beginContour();
            }
          }

          if (code == BEZIER_VERTEX) {
            addBezierVertex(i);
            i += 3;
          } else if (code == QUADRATIC_VERTEX) {
            addQuadraticVertex(i);
            i += 2;
          } else if (code == CURVE_VERTEX) {
            addCurveVertex(i);
            i++;
          } else {
            addVertex(i);
            i++;
          }
        }
        if (stroke) {
          endStrokePath(closed);
          endPolygonStroke();
        }
        if (fill) {
          gluTess.endContour();
          gluTess.endPolygon();
        }
      }
      endTex();

      if (stroke) tessellateStrokePath();
    }

    void addBezierVertex(int i) {
      pg.curveVertexCount = 0;
      pg.bezierInitCheck();
      pg.bezierVertexCheck(POLYGON, i);

      PMatrix3D draw = pg.bezierDrawMatrix;

      int i1 = i - 1;
      float x1 = in.vertices[3*i1 + 0];
      float y1 = in.vertices[3*i1 + 1];
      float z1 = in.vertices[3*i1 + 2];

      int strokeColor = 0;
      float strokeWeight = 0;
      if (stroke) {
        strokeColor = in.strokeColors[i];
        strokeWeight = in.strokeWeights[i];
      }

      int fcol = 0, fa = 0, fr = 0, fg = 0, fb = 0;
      int acol = 0, aa = 0, ar = 0, ag = 0, ab = 0;
      int scol = 0, sa = 0, sr = 0, sg = 0, sb = 0;
      int ecol = 0, ea = 0, er = 0, eg = 0, eb = 0;
      float nx = 0, ny = 0, nz = 0, u = 0, v = 0, sh = 0;
      if (fill) {
        fcol = in.colors[i];
        fa = (fcol >> 24) & 0xFF;
        fr = (fcol >> 16) & 0xFF;
        fg = (fcol >>  8) & 0xFF;
        fb = (fcol >>  0) & 0xFF;

        acol = in.ambient[i];
        aa = (acol >> 24) & 0xFF;
        ar = (acol >> 16) & 0xFF;
        ag = (acol >>  8) & 0xFF;
        ab = (acol >>  0) & 0xFF;

        scol = in.specular[i];
        sa = (scol >> 24) & 0xFF;
        sr = (scol >> 16) & 0xFF;
        sg = (scol >>  8) & 0xFF;
        sb = (scol >>  0) & 0xFF;

        ecol = in.emissive[i];
        ea = (ecol >> 24) & 0xFF;
        er = (ecol >> 16) & 0xFF;
        eg = (ecol >>  8) & 0xFF;
        eb = (ecol >>  0) & 0xFF;

        nx = in.normals[3*i + 0];
        ny = in.normals[3*i + 1];
        nz = in.normals[3*i + 2];
        u = in.texcoords[2*i + 0];
        v = in.texcoords[2*i + 1];
        sh = in.shininess[i];
      }

      float x2 = in.vertices[3*i + 0];
      float y2 = in.vertices[3*i + 1];
      float z2 = in.vertices[3*i + 2];
      float x3 = in.vertices[3*(i+1) + 0];
      float y3 = in.vertices[3*(i+1) + 1];
      float z3 = in.vertices[3*(i+1) + 2];
      float x4 = in.vertices[3*(i+2) + 0];
      float y4 = in.vertices[3*(i+2) + 1];
      float z4 = in.vertices[3*(i+2) + 2];

      float xplot1 = draw.m10*x1 + draw.m11*x2 + draw.m12*x3 + draw.m13*x4;
      float xplot2 = draw.m20*x1 + draw.m21*x2 + draw.m22*x3 + draw.m23*x4;
      float xplot3 = draw.m30*x1 + draw.m31*x2 + draw.m32*x3 + draw.m33*x4;

      float yplot1 = draw.m10*y1 + draw.m11*y2 + draw.m12*y3 + draw.m13*y4;
      float yplot2 = draw.m20*y1 + draw.m21*y2 + draw.m22*y3 + draw.m23*y4;
      float yplot3 = draw.m30*y1 + draw.m31*y2 + draw.m32*y3 + draw.m33*y4;

      float zplot1 = draw.m10*z1 + draw.m11*z2 + draw.m12*z3 + draw.m13*z4;
      float zplot2 = draw.m20*z1 + draw.m21*z2 + draw.m22*z3 + draw.m23*z4;
      float zplot3 = draw.m30*z1 + draw.m31*z2 + draw.m32*z3 + draw.m33*z4;

      for (int j = 0; j < pg.bezierDetail; j++) {
        x1 += xplot1; xplot1 += xplot2; xplot2 += xplot3;
        y1 += yplot1; yplot1 += yplot2; yplot2 += yplot3;
        z1 += zplot1; zplot1 += zplot2; zplot2 += zplot3;
        if (fill) {
          double[] vertex = new double[] {
            x1, y1, z1,
            fa, fr, fg, fb,
            nx, ny, nz,
            u, v,
            aa, ar, ag, ab, sa, sr, sg, sb, ea, er, eg, eb, sh};
          gluTess.addVertex(vertex);
        }
        if (stroke) addStrokeVertex(x1, y1, z1, strokeColor, strokeWeight);
      }
    }

    void addQuadraticVertex(int i) {
      pg.curveVertexCount = 0;
      pg.bezierInitCheck();
      pg.bezierVertexCheck(pg.shape, i);

      PMatrix3D draw = pg.bezierDrawMatrix;

      int i1 = i - 1;
      float x1 = in.vertices[3*i1 + 0];
      float y1 = in.vertices[3*i1 + 1];
      float z1 = in.vertices[3*i1 + 2];

      int strokeColor = 0;
      float strokeWeight = 0;
      if (stroke) {
        strokeColor = in.strokeColors[i];
        strokeWeight = in.strokeWeights[i];
      }

      int fcol = 0, fa = 0, fr = 0, fg = 0, fb = 0;
      int acol = 0, aa = 0, ar = 0, ag = 0, ab = 0;
      int scol = 0, sa = 0, sr = 0, sg = 0, sb = 0;
      int ecol = 0, ea = 0, er = 0, eg = 0, eb = 0;
      float nx = 0, ny = 0, nz = 0, u = 0, v = 0, sh = 0;
      if (fill) {
        fcol = in.colors[i];
        fa = (fcol >> 24) & 0xFF;
        fr = (fcol >> 16) & 0xFF;
        fg = (fcol >>  8) & 0xFF;
        fb = (fcol >>  0) & 0xFF;

        acol = in.ambient[i];
        aa = (acol >> 24) & 0xFF;
        ar = (acol >> 16) & 0xFF;
        ag = (acol >>  8) & 0xFF;
        ab = (acol >>  0) & 0xFF;

        scol = in.specular[i];
        sa = (scol >> 24) & 0xFF;
        sr = (scol >> 16) & 0xFF;
        sg = (scol >>  8) & 0xFF;
        sb = (scol >>  0) & 0xFF;

        ecol = in.emissive[i];
        ea = (ecol >> 24) & 0xFF;
        er = (ecol >> 16) & 0xFF;
        eg = (ecol >>  8) & 0xFF;
        eb = (ecol >>  0) & 0xFF;

        nx = in.normals[3*i + 0];
        ny = in.normals[3*i + 1];
        nz = in.normals[3*i + 2];
        u = in.texcoords[2*i + 0];
        v = in.texcoords[2*i + 1];
        sh = in.shininess[i];
      }

      float cx = in.vertices[3*i + 0];
      float cy = in.vertices[3*i + 1];
      float cz = in.vertices[3*i + 2];
      float x = in.vertices[3*(i+1) + 0];
      float y = in.vertices[3*(i+1) + 1];
      float z = in.vertices[3*(i+1) + 2];

      float x2 = x1 + ((cx-x1)*2/3.0f);
      float y2 = y1 + ((cy-y1)*2/3.0f);
      float z2 = z1 + ((cz-z1)*2/3.0f);
      float x3 = x + ((cx-x)*2/3.0f);
      float y3 = y + ((cy-y)*2/3.0f);
      float z3 = z + ((cz-z)*2/3.0f);
      float x4 = x;
      float y4 = y;
      float z4 = z;

      float xplot1 = draw.m10*x1 + draw.m11*x2 + draw.m12*x3 + draw.m13*x4;
      float xplot2 = draw.m20*x1 + draw.m21*x2 + draw.m22*x3 + draw.m23*x4;
      float xplot3 = draw.m30*x1 + draw.m31*x2 + draw.m32*x3 + draw.m33*x4;

      float yplot1 = draw.m10*y1 + draw.m11*y2 + draw.m12*y3 + draw.m13*y4;
      float yplot2 = draw.m20*y1 + draw.m21*y2 + draw.m22*y3 + draw.m23*y4;
      float yplot3 = draw.m30*y1 + draw.m31*y2 + draw.m32*y3 + draw.m33*y4;

      float zplot1 = draw.m10*z1 + draw.m11*z2 + draw.m12*z3 + draw.m13*z4;
      float zplot2 = draw.m20*z1 + draw.m21*z2 + draw.m22*z3 + draw.m23*z4;
      float zplot3 = draw.m30*z1 + draw.m31*z2 + draw.m32*z3 + draw.m33*z4;

      for (int j = 0; j < pg.bezierDetail; j++) {
        x1 += xplot1; xplot1 += xplot2; xplot2 += xplot3;
        y1 += yplot1; yplot1 += yplot2; yplot2 += yplot3;
        z1 += zplot1; zplot1 += zplot2; zplot2 += zplot3;
        if (fill) {
          double[] vertex = new double[] {
            x1, y1, z1,
            fa, fr, fg, fb,
            nx, ny, nz,
            u, v,
            aa, ar, ag, ab, sa, sr, sg, sb, ea, er, eg, eb, sh};
          gluTess.addVertex(vertex);
        }
        if (stroke) addStrokeVertex(x1, y1, z1, strokeColor, strokeWeight);
      }
    }

    void addCurveVertex(int i) {
      pg.curveVertexCheck(POLYGON);

      float[] vertex = pg.curveVertices[pg.curveVertexCount];
      vertex[X] = in.vertices[3*i + 0];
      vertex[Y] = in.vertices[3*i + 1];
      vertex[Z] = in.vertices[3*i + 2];
      pg.curveVertexCount++;

      // draw a segment if there are enough points
      if (pg.curveVertexCount > 3) {
        float[] v1 = pg.curveVertices[pg.curveVertexCount - 4];
        float[] v2 = pg.curveVertices[pg.curveVertexCount - 3];
        float[] v3 = pg.curveVertices[pg.curveVertexCount - 2];
        float[] v4 = pg.curveVertices[pg.curveVertexCount - 1];
        addCurveVertexSegment(i, v1[X], v1[Y], v1[Z],
                                 v2[X], v2[Y], v2[Z],
                                 v3[X], v3[Y], v3[Z],
                                 v4[X], v4[Y], v4[Z]);
      }
    }

    void addCurveVertexSegment(int i, float x1, float y1, float z1,
                                      float x2, float y2, float z2,
                                      float x3, float y3, float z3,
                                      float x4, float y4, float z4) {
      int strokeColor = 0;
      float strokeWeight = 0;
      if (stroke) {
        strokeColor = in.strokeColors[i];
        strokeWeight = in.strokeWeights[i];
      }

      int fcol = 0, fa = 0, fr = 0, fg = 0, fb = 0;
      int acol = 0, aa = 0, ar = 0, ag = 0, ab = 0;
      int scol = 0, sa = 0, sr = 0, sg = 0, sb = 0;
      int ecol = 0, ea = 0, er = 0, eg = 0, eb = 0;
      float nx = 0, ny = 0, nz = 0, u = 0, v = 0, sh = 0;
      if (fill) {
        fcol = in.colors[i];
        fa = (fcol >> 24) & 0xFF;
        fr = (fcol >> 16) & 0xFF;
        fg = (fcol >>  8) & 0xFF;
        fb = (fcol >>  0) & 0xFF;

        acol = in.ambient[i];
        aa = (acol >> 24) & 0xFF;
        ar = (acol >> 16) & 0xFF;
        ag = (acol >>  8) & 0xFF;
        ab = (acol >>  0) & 0xFF;

        scol = in.specular[i];
        sa = (scol >> 24) & 0xFF;
        sr = (scol >> 16) & 0xFF;
        sg = (scol >>  8) & 0xFF;
        sb = (scol >>  0) & 0xFF;

        ecol = in.emissive[i];
        ea = (ecol >> 24) & 0xFF;
        er = (ecol >> 16) & 0xFF;
        eg = (ecol >>  8) & 0xFF;
        eb = (ecol >>  0) & 0xFF;

        nx = in.normals[3*i + 0];
        ny = in.normals[3*i + 1];
        nz = in.normals[3*i + 2];
        u = in.texcoords[2*i + 0];
        v = in.texcoords[2*i + 1];
        sh = in.shininess[i];
      }

      float x = x2;
      float y = y2;
      float z = z2;

      PMatrix3D draw = pg.curveDrawMatrix;

      float xplot1 = draw.m10*x1 + draw.m11*x2 + draw.m12*x3 + draw.m13*x4;
      float xplot2 = draw.m20*x1 + draw.m21*x2 + draw.m22*x3 + draw.m23*x4;
      float xplot3 = draw.m30*x1 + draw.m31*x2 + draw.m32*x3 + draw.m33*x4;

      float yplot1 = draw.m10*y1 + draw.m11*y2 + draw.m12*y3 + draw.m13*y4;
      float yplot2 = draw.m20*y1 + draw.m21*y2 + draw.m22*y3 + draw.m23*y4;
      float yplot3 = draw.m30*y1 + draw.m31*y2 + draw.m32*y3 + draw.m33*y4;

      float zplot1 = draw.m10*z1 + draw.m11*z2 + draw.m12*z3 + draw.m13*z4;
      float zplot2 = draw.m20*z1 + draw.m21*z2 + draw.m22*z3 + draw.m23*z4;
      float zplot3 = draw.m30*z1 + draw.m31*z2 + draw.m32*z3 + draw.m33*z4;

      if (fill) {
        double[] vertex0 = new double[] {
          x, y, z,
          fa, fr, fg, fb,
          nx, ny, nz,
          u, v,
          aa, ar, ag, ab, sa, sr, sg, sb, ea, er, eg, eb, sh};
        gluTess.addVertex(vertex0);
      }
      if (stroke) addStrokeVertex(x, y, z, strokeColor, strokeWeight);

      for (int j = 0; j < pg.curveDetail; j++) {
        x += xplot1; xplot1 += xplot2; xplot2 += xplot3;
        y += yplot1; yplot1 += yplot2; yplot2 += yplot3;
        z += zplot1; zplot1 += zplot2; zplot2 += zplot3;
        if (fill) {
          double[] vertex1 = new double[] {
            x, y, z,
            fa, fr, fg, fb,
            nx, ny, nz,
            u, v,
            aa, ar, ag, ab, sa, sr, sg, sb, ea, er, eg, eb, sh};
          gluTess.addVertex(vertex1);
        }
        if (stroke) addStrokeVertex(x, y, z, strokeColor, strokeWeight);
      }
    }

    void addVertex(int i) {
      pg.curveVertexCount = 0;

      float x = in.vertices[3*i + 0];
      float y = in.vertices[3*i + 1];
      float z = in.vertices[3*i + 2];

      int strokeColor = 0;
      float strokeWeight = 0;
      if (stroke) {
        strokeColor = in.strokeColors[i];
        strokeWeight = in.strokeWeights[i];
      }

      if (fill) {
        // Separating colors into individual rgba components for interpolation.
        int fcol = in.colors[i];
        int fa = (fcol >> 24) & 0xFF;
        int fr = (fcol >> 16) & 0xFF;
        int fg = (fcol >>  8) & 0xFF;
        int fb = (fcol >>  0) & 0xFF;

        int acol = in.ambient[i];
        int aa = (acol >> 24) & 0xFF;
        int ar = (acol >> 16) & 0xFF;
        int ag = (acol >>  8) & 0xFF;
        int ab = (acol >>  0) & 0xFF;

        int scol = in.specular[i];
        int sa = (scol >> 24) & 0xFF;
        int sr = (scol >> 16) & 0xFF;
        int sg = (scol >>  8) & 0xFF;
        int sb = (scol >>  0) & 0xFF;

        int ecol = in.emissive[i];
        int ea = (ecol >> 24) & 0xFF;
        int er = (ecol >> 16) & 0xFF;
        int eg = (ecol >>  8) & 0xFF;
        int eb = (ecol >>  0) & 0xFF;

        float nx = in.normals[3*i + 0];
        float ny = in.normals[3*i + 1];
        float nz = in.normals[3*i + 2];
        float u = in.texcoords[2*i + 0];
        float v = in.texcoords[2*i + 1];
        float sh = in.shininess[i];

        double[] vertex = new double[] {
          x, y, z,
          fa, fr, fg, fb,
          nx, ny, nz,
          u, v,
          aa, ar, ag, ab, sa, sr, sg, sb, ea, er, eg, eb, sh};
        gluTess.addVertex(vertex);
      }
      if (stroke) addStrokeVertex(x, y, z, strokeColor, strokeWeight);
    }

    void beginPolygonStroke() {
      pathVertexCount = 0;
      if (pathVertices == null) {
        pathVertices = new float[3 * PGL.DEFAULT_IN_VERTICES];
        pathColors = new int[PGL.DEFAULT_IN_VERTICES];
        pathWeights = new float[PGL.DEFAULT_IN_VERTICES];
      }
    }

    void endPolygonStroke() {
      // Nothing to do here.
    }

    void beginStrokePath() {
      beginPath = pathVertexCount;
    }

    void endStrokePath(boolean closed) {
      int idx = pathVertexCount;
      if (beginPath + 1 < idx) {
        boolean begin = beginPath == idx - 2;
        boolean end = begin || !closed;
        in.addEdge(idx - 2, idx - 1, begin, end);
        if (!end) {
          in.addEdge(idx - 1, beginPath, false, false);
          in.closeEdge(idx - 1, beginPath);
        }
      }
    }

    void addStrokeVertex(float x, float y, float z, int c, float w) {
      int idx = pathVertexCount;
      if (beginPath + 1 < idx) {
        in.addEdge(idx - 2, idx - 1, beginPath == idx - 2, false);
      }

      if (pathVertexCount == pathVertices.length / 3) {
        int newSize = pathVertexCount << 1;

        float vtemp[] = new float[3 * newSize];
        PApplet.arrayCopy(pathVertices, 0, vtemp, 0, 3 * pathVertexCount);
        pathVertices = vtemp;

        int ctemp[] = new int[newSize];
        PApplet.arrayCopy(pathColors, 0, ctemp, 0, pathVertexCount);
        pathColors = ctemp;

        float wtemp[] = new float[newSize];
        PApplet.arrayCopy(pathWeights, 0, wtemp, 0, pathVertexCount);
        pathWeights = wtemp;
      }

      pathVertices[3 * idx + 0] = x;
      pathVertices[3 * idx + 1] = y;
      pathVertices[3 * idx + 2] = z;
      pathColors[idx] = c;
      pathWeights[idx] = w;

      pathVertexCount++;
    }

    void tessellateStrokePath() {
      if (in.edgeCount == 0) return;
      strokeVertices = pathVertices;
      strokeColors = pathColors;
      strokeWeights = pathWeights;
      if (is3D) {
        tessellateEdges3D();
      } else if (is2D) {
        beginNoTex();
        tessellateEdges2D();
        endNoTex();
      }
    }

    boolean clampPolygon() {
      return false;
    }

    // Tessellates the path given as parameter. This will work only in 2D.
    // Based on the opengl stroke hack described here:
    // http://wiki.processing.org/w/Stroke_attributes_in_OpenGL
    public void tessellateLinePath(LinePath path) {
      boolean clamp = clampLinePath();

      callback.init(in.renderMode == RETAINED, true, false, clamp);

      int cap = strokeCap == ROUND ? LinePath.CAP_ROUND :
                strokeCap == PROJECT ? LinePath.CAP_SQUARE :
                LinePath.CAP_BUTT;
      int join = strokeJoin == ROUND ? LinePath.JOIN_ROUND :
                 strokeJoin == BEVEL ? LinePath.JOIN_BEVEL :
                 LinePath.JOIN_MITER;

      // Make the outline of the stroke from the path
      LinePath strokedPath = LinePath.createStrokedPath(path, strokeWeight,
                                                        cap, join);

      gluTess.beginPolygon();

      double[] vertex;
      float[] coords = new float[6];

      LinePath.PathIterator iter = strokedPath.getPathIterator();
      int rule = iter.getWindingRule();
      switch(rule) {
      case LinePath.WIND_EVEN_ODD:
        gluTess.setWindingRule(PGL.TESS_WINDING_ODD);
        break;
      case LinePath.WIND_NON_ZERO:
        gluTess.setWindingRule(PGL.TESS_WINDING_NONZERO);
        break;
      }

      while (!iter.isDone()) {
        switch (iter.currentSegment(coords)) {

        case LinePath.SEG_MOVETO:
          gluTess.beginContour();

          // $FALL-THROUGH$
        case LinePath.SEG_LINETO:
          // Vertex data includes coordinates, colors, normals, texture
          // coordinates, and material properties.
          vertex = new double[] { coords[0], coords[1], 0,
                                  coords[2], coords[3], coords[4], coords[5],
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

    boolean clampLinePath() {
      return clamp2D() &&
             strokeCap == PROJECT && strokeJoin == BEVEL &&
             !subPixelStroke(strokeWeight);
    }

    /////////////////////////////////////////

    // Interesting notes about using the GLU tessellator to render thick
    // polylines:
    // http://stackoverflow.com/questions/687173/how-do-i-render-thick-2d-lines-as-polygons
    //
    // "...Since I disliked the tesselator API I lifted the tesselation code
    //  from the free SGI OpenGL reference implementation, rewrote the entire
    //  front-end and added memory pools to get the number of allocations down.
    //  It took two days to do this, but it was well worth it (like factor five
    //  performance improvement)..."
    //
    // This C implementation of GLU could be useful:
    // http://code.google.com/p/glues/
    // to eventually come up with an optimized GLU tessellator in native code.
    protected class TessellatorCallback implements PGL.TessellatorCallback {
      boolean calcNormals;
      boolean strokeTess;
      boolean clampXY;
      IndexCache cache;
      int cacheIndex;
      int vertFirst;
      int vertCount;
      int primitive;

      public void init(boolean addCache, boolean strokeTess, boolean calcNorm,
                       boolean clampXY) {
        this.strokeTess = strokeTess;
        this.calcNormals = calcNorm;
        this.clampXY = clampXY;

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

        if (type == PGL.TRIANGLE_FAN) primitive = TRIANGLE_FAN;
        else if (type == PGL.TRIANGLE_STRIP) primitive = TRIANGLE_STRIP;
        else if (type == PGL.TRIANGLES) primitive = TRIANGLES;
      }

      public void end() {
        if (PGL.MAX_VERTEX_INDEX1 <= vertFirst + vertCount) {
          // We need a new index block for the new batch of
          // vertices resulting from this primitive. tessVert can
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
        tess.polyIndices[tess.polyIndexCount - 1] =
          (short) (vertFirst + tessIdx);
      }

      protected void calcTriNormal(int tessIdx0, int tessIdx1, int tessIdx2) {
        tess.calcPolyNormal(vertFirst + tessIdx0, vertFirst + tessIdx1,
                            vertFirst + tessIdx2);
      }

      public void vertex(Object data) {
        if (data instanceof double[]) {
          double[] d = (double[]) data;
          int l = d.length;
          if (l < 25) {
            throw new RuntimeException("TessCallback vertex() data is not " +
                                       "of length 25");
          }

          if (vertCount < PGL.MAX_VERTEX_INDEX1) {
            // Combining individual rgba components back into int color values
            int fcolor =
             ((int)d[ 3]<<24) | ((int)d[ 4]<<16) | ((int)d[ 5]<<8) | (int)d[ 6];
            int acolor =
             ((int)d[12]<<24) | ((int)d[13]<<16) | ((int)d[14]<<8) | (int)d[15];
            int scolor =
             ((int)d[16]<<24) | ((int)d[17]<<16) | ((int)d[18]<<8) | (int)d[19];
            int ecolor =
             ((int)d[20]<<24) | ((int)d[21]<<16) | ((int)d[22]<<8) | (int)d[23];

            tess.addPolyVertex((float) d[ 0],  (float) d[ 1], (float) d[ 2],
                               fcolor,
                               (float) d[ 7],  (float) d[ 8], (float) d[ 9],
                               (float) d[10], (float) d[11],
                               acolor, scolor, ecolor,
                               (float) d[24], clampXY);

            vertCount++;
          } else {
            throw new RuntimeException("The tessellator is generating too " +
                                       "many vertices, reduce complexity of " +
                                       "shape.");
          }

        } else {
          throw new RuntimeException("TessCallback vertex() data not " +
                                     "understood");
        }
      }

      public void error(int errnum) {
        String estring = pgl.tessError(errnum);
        PGraphics.showWarning(TESSELLATION_ERROR, estring);
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
