/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2012-15 The Processing Foundation
  Copyright (c) 2004-12 Ben Fry and Casey Reas
  Copyright (c) 2001-04 Massachusetts Institute of Technology

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

import processing.core.*;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.nio.*;
import java.util.*;


/**
 * OpenGL renderer.
 */
public class PGraphicsOpenGL extends PGraphics {
  /** Interface between Processing and OpenGL */
  public PGL pgl;

  /** The renderer currently in use. */
  public PGraphicsOpenGL currentPG;

  /** Font cache for texture objects. */
  protected WeakHashMap<PFont, FontTexture> fontMap;

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

  protected VertexBuffer bufPolyVertex;
  protected VertexBuffer bufPolyColor;
  protected VertexBuffer bufPolyNormal;
  protected VertexBuffer bufPolyTexcoord;
  protected VertexBuffer bufPolyAmbient;
  protected VertexBuffer bufPolySpecular;
  protected VertexBuffer bufPolyEmissive;
  protected VertexBuffer bufPolyShininess;
  protected VertexBuffer bufPolyIndex;
  protected boolean polyBuffersCreated = false;
  protected int polyBuffersContext;

  protected VertexBuffer bufLineVertex;
  protected VertexBuffer bufLineColor;
  protected VertexBuffer bufLineAttrib;
  protected VertexBuffer bufLineIndex;
  protected boolean lineBuffersCreated = false;
  protected int lineBuffersContext;

  protected VertexBuffer bufPointVertex;
  protected VertexBuffer bufPointColor;
  protected VertexBuffer bufPointAttrib;
  protected VertexBuffer bufPointIndex;
  protected boolean pointBuffersCreated = false;
  protected int pointBuffersContext;

  // Generic vertex attributes (only for polys)
  protected AttributeMap polyAttribs;

  static protected final int INIT_VERTEX_BUFFER_SIZE  = 256;
  static protected final int INIT_INDEX_BUFFER_SIZE   = 512;

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
  static public boolean readBufferSupported;
  static public boolean drawBufferSupported;

  /** Some hardware limits */
  static public int maxTextureSize;
  static public int maxSamples;
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

  // Shaders

  static protected URL defColorShaderVertURL =
    PGraphicsOpenGL.class.getResource("/processing/opengl/shaders/ColorVert.glsl");
  static protected URL defTextureShaderVertURL =
    PGraphicsOpenGL.class.getResource("/processing/opengl/shaders/TexVert.glsl");
  static protected URL defLightShaderVertURL =
    PGraphicsOpenGL.class.getResource("/processing/opengl/shaders/LightVert.glsl");
  static protected URL defTexlightShaderVertURL =
    PGraphicsOpenGL.class.getResource("/processing/opengl/shaders/TexLightVert.glsl");
  static protected URL defColorShaderFragURL =
    PGraphicsOpenGL.class.getResource("/processing/opengl/shaders/ColorFrag.glsl");
  static protected URL defTextureShaderFragURL =
    PGraphicsOpenGL.class.getResource("/processing/opengl/shaders/TexFrag.glsl");
  static protected URL defLightShaderFragURL =
    PGraphicsOpenGL.class.getResource("/processing/opengl/shaders/LightFrag.glsl");
  static protected URL defTexlightShaderFragURL =
    PGraphicsOpenGL.class.getResource("/processing/opengl/shaders/TexLightFrag.glsl");

  static protected URL defLineShaderVertURL =
    PGraphicsOpenGL.class.getResource("/processing/opengl/shaders/LineVert.glsl");
  static protected URL defLineShaderFragURL =
    PGraphicsOpenGL.class.getResource("/processing/opengl/shaders/LineFrag.glsl");
  static protected URL defPointShaderVertURL =
    PGraphicsOpenGL.class.getResource("/processing/opengl/shaders/PointVert.glsl");
  static protected URL defPointShaderFragURL =
    PGraphicsOpenGL.class.getResource("/processing/opengl/shaders/PointFrag.glsl");
  static protected URL maskShaderFragURL =
    PGraphicsOpenGL.class.getResource("/processing/opengl/shaders/MaskFrag.glsl");

  protected PShader defColorShader;
  protected PShader defTextureShader;
  protected PShader defLightShader;
  protected PShader defTexlightShader;
  protected PShader defLineShader;
  protected PShader defPointShader;
  protected PShader maskShader;

  protected PShader polyShader;
  protected PShader lineShader;
  protected PShader pointShader;

  // ........................................................

  // Tessellator, geometry

  protected InGeometry inGeo;
  protected TessGeometry tessGeo;
  protected TexCache texCache;
  protected Tessellator tessellator;

  // ........................................................

  // Depth sorter

  protected DepthSorter sorter;
  protected boolean isDepthSortingEnabled;

  // ........................................................

  // Async pixel reader

  protected AsyncPixelReader asyncPixelReader;
  protected boolean asyncPixelReaderInitialized;

  // Keeps track of ongoing transfers so they can be finished.
  // Set is copied to the List when we need to iterate it
  // so that readers can remove themselves from the Set during
  // iteration if they don't have any ongoing transfers.
  protected static final Set<PGraphicsOpenGL.AsyncPixelReader>
      ongoingPixelTransfers = new HashSet<>();
  protected static final List<PGraphicsOpenGL.AsyncPixelReader>
      ongoingPixelTransfersIterable = new ArrayList<>();

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

  /** Distance between camera eye and center. */
  protected float eyeDist;

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

  protected int fbStackDepth;
  protected FrameBuffer[] fbStack;
  protected FrameBuffer drawFramebuffer;
  protected FrameBuffer readFramebuffer;
  protected FrameBuffer currentFramebuffer;

  // .......................................................

  // Offscreen rendering:

  protected FrameBuffer offscreenFramebuffer;
  protected FrameBuffer multisampleFramebuffer;
  protected boolean offscreenMultisample;

  protected boolean pixOpChangedFB;

  // ........................................................

  // Screen surface:

  /** Texture containing the current frame */
  protected Texture texture = null;

  /** Texture containing the previous frame */
  protected Texture ptexture = null;

  /** IntBuffer wrapping the pixels array. */
  protected IntBuffer pixelBuffer;

  /** Array to store pixels in OpenGL format. */
  protected int[] nativePixels;

  /** IntBuffer wrapping the native pixels array. */
  protected IntBuffer nativePixelBuffer;

  /** texture used to apply a filter on the screen image. */
  protected Texture filterTexture = null;

  /** PImage that wraps filterTexture. */
  protected PImage filterImage;

  // ........................................................

  // Utility variables:

  /** True if we are inside a beginDraw()/endDraw() block. */
  protected boolean drawing = false;

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

  protected boolean openContour = false;
  protected boolean breakShape = false;
  protected boolean defaultEdges = false;

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

  // ........................................................

  // Error strings:

  static final String OPENGL_THREAD_ERROR =
    "Cannot run the OpenGL renderer outside the main thread, change your code" +
    "\nso the drawing calls are all inside the main thread, " +
    "\nor use the default renderer instead.";
  static final String BLEND_DRIVER_ERROR =
    "blendMode(%1$s) is not supported by this hardware (or driver)";
  static final String BLEND_RENDERER_ERROR =
    "blendMode(%1$s) is not supported by this renderer";
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
  static final String MISSING_UV_TEXCOORDS_ERROR =
    "No uv texture coordinates supplied with vertex() call";
  static final String INVALID_FILTER_SHADER_ERROR =
    "Your shader cannot be used as a filter because is of type POINT or LINES";
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
  static final String TESSELLATION_ERROR =
    "Tessellation Error: %1$s";
  static final String GL_THREAD_NOT_CURRENT =
    "You are trying to draw outside OpenGL's animation thread.\n" +
    "Place all drawing commands in the draw() function, or inside\n" +
    "your own functions as long as they are called from draw(),\n" +
    "but not in event handling functions such as keyPressed()\n" +
    "or mousePressed().";

  //////////////////////////////////////////////////////////////

  // INIT/ALLOCATE/FINISH


  public PGraphicsOpenGL() {
    pgl = createPGL(this);

    if (intBuffer == null) {
      intBuffer = PGL.allocateIntBuffer(2);
      floatBuffer = PGL.allocateFloatBuffer(2);
    }

    viewport = PGL.allocateIntBuffer(4);

    polyAttribs = newAttributeMap();
    inGeo = newInGeometry(this, polyAttribs, IMMEDIATE);
    tessGeo = newTessGeometry(this, polyAttribs, IMMEDIATE);
    texCache = newTexCache(this);

    projection = new PMatrix3D();
    camera = new PMatrix3D();
    cameraInv = new PMatrix3D();
    modelview = new PMatrix3D();
    modelviewInv = new PMatrix3D();
    projmodelview = new PMatrix3D();

    lightType = new int[PGL.MAX_LIGHTS];
    lightPosition = new float[4 * PGL.MAX_LIGHTS];
    lightNormal = new float[3 * PGL.MAX_LIGHTS];
    lightAmbient = new float[3 * PGL.MAX_LIGHTS];
    lightDiffuse = new float[3 * PGL.MAX_LIGHTS];
    lightSpecular = new float[3 * PGL.MAX_LIGHTS];
    lightFalloffCoefficients = new float[3 * PGL.MAX_LIGHTS];
    lightSpotParameters = new float[2 * PGL.MAX_LIGHTS];
    currentLightSpecular = new float[3];

    initialized = false;
  }


  @Override
  public void setParent(PApplet parent) {
    super.setParent(parent);
    if (pgl != null) {
      pgl.sketch = parent;
    }
  }


  @Override
  public void setPrimary(boolean primary) {
    super.setPrimary(primary);
    pgl.setPrimary(primary);
    format = ARGB;
    if (primary) {
      fbStack = new FrameBuffer[FB_STACK_DEPTH];
      fontMap = new WeakHashMap<PFont, FontTexture>();
      tessellator = new Tessellator();
    } else {
      tessellator = getPrimaryPG().tessellator;
    }
  }


  //public void setPath(String path)  // PGraphics


  //public void setAntiAlias(int samples)  // PGraphics


  @Override
  public void setSize(int iwidth, int iheight) {
    width = iwidth;
    height = iheight;
    updatePixelSize();

    // init perspective projection based on new dimensions
    cameraFOV = 60 * DEG_TO_RAD; // at least for now
    cameraX = width / 2.0f;
    cameraY = height / 2.0f;
    cameraZ = cameraY / ((float) Math.tan(cameraFOV / 2.0f));
    cameraNear = cameraZ / 10.0f;
    cameraFar = cameraZ * 10.0f;
    cameraAspect = (float) width / (float) height;

    sized = true;
  }


  @Override
  public void dispose() { // PGraphics
    if (asyncPixelReader != null) {
      asyncPixelReader.dispose();
      asyncPixelReader = null;
    }

    if (!primaryGraphics) {
      deleteSurfaceTextures();
      FrameBuffer ofb = offscreenFramebuffer;
      FrameBuffer mfb = multisampleFramebuffer;
      if (ofb != null) {
        ofb.dispose();
      }
      if (mfb != null) {
        mfb.dispose();
      }
    }

    pgl.dispose();

    super.dispose();
  }


  protected void setFlushMode(int mode) {
    flushMode = mode;
  }


  protected void updatePixelSize() {
    float f = pgl.getPixelScale();
    pixelWidth = (int)(width * f);
    pixelHeight = (int)(height * f);
  }


  //////////////////////////////////////////////////////////////

  // PLATFORM-SPECIFIC CODE (Java, Android, etc.). Needs to be manually edited.


  // Factory method
  protected PGL createPGL(PGraphicsOpenGL pg) {
    return new PJOGL(pg);
//    return new PGLES(pg);
  }


/*
  @Override
  // Android only
  public void setFrameRate(float frameRate) {
    pgl.setFrameRate(frameRate);
  }


  @Override
  // Android only
  public boolean canDraw() {
    return pgl.canDraw();
  }


  @Override
  // Android only
  public void requestDraw() {
    if (primaryGraphics) {
      if (initialized) {
        if (sized) pgl.reinitSurface();
        if (parent.canDraw()) pgl.requestDraw();
      } else {
        initPrimary();
      }
    }
  }
*/


  @Override
  // Java only
  public PSurface createSurface() {  // ignore
    return surface = new PSurfaceJOGL(this);
  }


  public boolean saveImpl(String filename) {
//    return super.save(filename); // ASYNC save frame using PBOs not yet available on Android

    if (getHint(DISABLE_ASYNC_SAVEFRAME)) {
      // Act as an opaque surface for the purposes of saving.
      if (primaryGraphics) {
        int prevFormat = format;
        format = RGB;
        boolean result = super.save(filename);
        format = prevFormat;
        return result;
      }

      return super.save(filename);
    }

    if (asyncImageSaver == null) {
      asyncImageSaver = new AsyncImageSaver();
    }

    if (!asyncPixelReaderInitialized) {
      // First call! Get this guy initialized
      if (pgl.hasPBOs() && pgl.hasSynchronization()) {
        asyncPixelReader = new AsyncPixelReader();
      }
      asyncPixelReaderInitialized = true;
    }

    if (asyncPixelReader != null && !loaded) {
      boolean needEndDraw = false;
      if (!drawing) {
        beginDraw();
        needEndDraw = true;
      }
      flush();
      updatePixelSize();

      // get the whole async package
      asyncPixelReader.readAndSaveAsync(filename);

      if (needEndDraw) endDraw();
    } else {
      // async transfer is not supported or
      // pixels are already in memory, just do async save
      if (!loaded) loadPixels();
      int format = primaryGraphics ? RGB : ARGB;
      PImage target = asyncImageSaver.getAvailableTarget(pixelWidth, pixelHeight,
                                                         format);
      if (target == null) return false;
      int count = PApplet.min(pixels.length, target.pixels.length);
      System.arraycopy(pixels, 0, target.pixels, 0, count);
      asyncImageSaver.saveTargetAsync(this, target, filename);
    }

    return true;
  }


  //////////////////////////////////////////////////////////////

  // IMAGE METADATA FOR THIS RENDERER


  @Override
  public void setCache(PImage image, Object storage) {
    if (image instanceof PGraphicsOpenGL) {
      // Prevent strong reference to the key from the value by wrapping
      // the Texture into WeakReference (proposed solution by WeakHashMap docs)
      getPrimaryPG().cacheMap.put(image, new WeakReference<>(storage));
      return;
    }
    getPrimaryPG().cacheMap.put(image, storage);
  }


  @Override
  @SuppressWarnings("rawtypes")
  public Object getCache(PImage image) {
    Object storage = getPrimaryPG().cacheMap.get(image);
    if (storage != null && storage.getClass() == WeakReference.class) {
      // Unwrap the value, use getClass() for fast check
      return ((WeakReference) storage).get();
    }
    return storage;
  }


  @Override
  public void removeCache(PImage image) {
    getPrimaryPG().cacheMap.remove(image);
  }


  //////////////////////////////////////////////////////////////


  protected void setFontTexture(PFont font, FontTexture fontTexture) {
    getPrimaryPG().fontMap.put(font, fontTexture);
  }


  protected FontTexture getFontTexture(PFont font) {
    return getPrimaryPG().fontMap.get(font);
  }


  protected void removeFontTexture(PFont font) {
    getPrimaryPG().fontMap.remove(font);
  }


  //////////////////////////////////////////////////////////////

  // RESOURCE HANDLING
  // Using the technique alternative to finalization described in:
  // http://www.oracle.com/technetwork/articles/java/finalization-137655.html

  static final private int MAX_DRAIN_GLRES_ITERATIONS = 10;

  protected static class GLResourceTexture extends WeakReference<Texture> {
    int glName;

    private PGL pgl;
    private int context;

    static private ReferenceQueue<Texture> refQueue = new ReferenceQueue<Texture>();
    static private List<GLResourceTexture> refList = new ArrayList<GLResourceTexture>();

    static void drainRefQueueBounded() {
      ReferenceQueue<Texture> refQueue = GLResourceTexture.referenceQueue();
      int iterations = 0;
      while (iterations < MAX_DRAIN_GLRES_ITERATIONS) {
        GLResourceTexture res = (GLResourceTexture)refQueue.poll();
        if (res == null) {
          break;
        }
        res.dispose();
        ++iterations;
      }
    }

    static ReferenceQueue<Texture> referenceQueue() {
      return refQueue;
    }

    public GLResourceTexture(Texture tex) {
      super(tex, refQueue);

      drainRefQueueBounded();

      pgl = tex.pg.getPrimaryPGL();
      pgl.genTextures(1, intBuffer);
      tex.glName = intBuffer.get(0);

      this.glName = tex.glName;
      this.context = tex.context;

      refList.add(this);
    }

    private void disposeNative() {
      if (pgl != null) {
        if (glName != 0) {
          intBuffer.put(0, glName);
          pgl.deleteTextures(1, intBuffer);
          glName = 0;
        }
        pgl = null;
      }
    }

    void dispose() {
      refList.remove(this);
      disposeNative();
    }

    @Override
    public boolean equals(Object obj) {
      GLResourceTexture other = (GLResourceTexture)obj;
      return other.glName == glName &&
             other.context == context;
    }

    @Override
    public int hashCode() {
      int result = 17;
      result = 31 * result + glName;
      result = 31 * result + context;
      return result;
    }
  }


  protected static class GLResourceVertexBuffer extends WeakReference<VertexBuffer> {
    int glId;

    private PGL pgl;
    private int context;

    static private ReferenceQueue<VertexBuffer> refQueue = new ReferenceQueue<VertexBuffer>();
    static private List<GLResourceVertexBuffer> refList = new ArrayList<GLResourceVertexBuffer>();

    static void drainRefQueueBounded() {
      ReferenceQueue<VertexBuffer> refQueue = GLResourceVertexBuffer.referenceQueue();
      int iterations = 0;
      while (iterations < MAX_DRAIN_GLRES_ITERATIONS) {
        GLResourceVertexBuffer res = (GLResourceVertexBuffer)refQueue.poll();
        if (res == null) {
          break;
        }
        res.dispose();
        ++iterations;
      }
    }

    static ReferenceQueue<VertexBuffer> referenceQueue() {
      return refQueue;
    }

    public GLResourceVertexBuffer(VertexBuffer vbo) {
      super(vbo, refQueue);

      drainRefQueueBounded();

      pgl = vbo.pgl.graphics.getPrimaryPGL();
      pgl.genBuffers(1, intBuffer);
      vbo.glId = intBuffer.get(0);

      this.glId = vbo.glId;
      this.context = vbo.context;

      refList.add(this);
    }

    private void disposeNative() {
      if (pgl != null) {
        if (glId != 0) {
          intBuffer.put(0, glId);
          pgl.deleteBuffers(1, intBuffer);
          glId = 0;
        }
        pgl = null;
      }
    }

    void dispose() {
      refList.remove(this);
      disposeNative();
    }

    @Override
    public boolean equals(Object obj) {
      GLResourceVertexBuffer other = (GLResourceVertexBuffer)obj;
      return other.glId == glId &&
             other.context == context;
    }

    @Override
    public int hashCode() {
      int result = 17;
      result = 31 * result + glId;
      result = 31 * result + context;
      return result;
    }
  }


  protected static class GLResourceShader extends WeakReference<PShader> {
    int glProgram;
    int glVertex;
    int glFragment;

    private PGL pgl;
    private int context;

    static private ReferenceQueue<PShader> refQueue = new ReferenceQueue<PShader>();
    static private List<GLResourceShader> refList = new ArrayList<GLResourceShader>();

    static void drainRefQueueBounded() {
      ReferenceQueue<PShader> refQueue = GLResourceShader.referenceQueue();
      int iterations = 0;
      while (iterations < MAX_DRAIN_GLRES_ITERATIONS) {
        GLResourceShader res = (GLResourceShader)refQueue.poll();
        if (res == null) {
          break;
        }
        res.dispose();
        ++iterations;
      }
    }

    static ReferenceQueue<PShader> referenceQueue() {
      return refQueue;
    }

    public GLResourceShader(PShader sh) {
      super(sh, refQueue);

      drainRefQueueBounded();

      this.pgl = sh.pgl.graphics.getPrimaryPGL();
      sh.glProgram = pgl.createProgram();
      sh.glVertex = pgl.createShader(PGL.VERTEX_SHADER);
      sh.glFragment = pgl.createShader(PGL.FRAGMENT_SHADER);

      this.glProgram = sh.glProgram;
      this.glVertex = sh.glVertex;
      this.glFragment = sh.glFragment;

      this.context = sh.context;

      refList.add(this);
    }

    private void disposeNative() {
      if (pgl != null) {
        if (glFragment != 0) {
          pgl.deleteShader(glFragment);
          glFragment = 0;
        }
        if (glVertex != 0) {
          pgl.deleteShader(glVertex);
          glVertex = 0;
        }
        if (glProgram != 0) {
          pgl.deleteProgram(glProgram);
          glProgram = 0;
        }
        pgl = null;
      }
    }

    void dispose() {
      refList.remove(this);
      disposeNative();
    }

    @Override
    public boolean equals(Object obj) {
      GLResourceShader other = (GLResourceShader)obj;
      return other.glProgram == glProgram &&
             other.glVertex == glVertex &&
             other.glFragment == glFragment &&
             other.context == context;
    }

    @Override
    public int hashCode() {
      int result = 17;
      result = 31 * result + glProgram;
      result = 31 * result + glVertex;
      result = 31 * result + glFragment;
      result = 31 * result + context;
      return result;
    }
  }


  protected static class GLResourceFrameBuffer extends WeakReference<FrameBuffer> {
    int glFbo;
    int glDepth;
    int glStencil;
    int glDepthStencil;
    int glMultisample;

    private PGL pgl;
    private int context;

    static private ReferenceQueue<FrameBuffer> refQueue = new ReferenceQueue<FrameBuffer>();
    static private List<GLResourceFrameBuffer> refList = new ArrayList<GLResourceFrameBuffer>();

    static void drainRefQueueBounded() {
      ReferenceQueue<FrameBuffer> refQueue = GLResourceFrameBuffer.referenceQueue();
      int iterations = 0;
      while (iterations < MAX_DRAIN_GLRES_ITERATIONS) {
        GLResourceFrameBuffer res = (GLResourceFrameBuffer)refQueue.poll();
        if (res == null) {
          break;
        }
        res.dispose();
        ++iterations;
      }
    }

    static ReferenceQueue<FrameBuffer> referenceQueue() {
      return refQueue;
    }

    public GLResourceFrameBuffer(FrameBuffer fb) {
      super(fb, refQueue);

      drainRefQueueBounded();

      pgl = fb.pg.getPrimaryPGL();
      if (!fb.screenFb) {
        pgl.genFramebuffers(1, intBuffer);
        fb.glFbo = intBuffer.get(0);

        if (fb.multisample) {
          pgl.genRenderbuffers(1, intBuffer);
          fb.glMultisample = intBuffer.get(0);
        }

        if (fb.packedDepthStencil) {
          pgl.genRenderbuffers(1, intBuffer);
          fb.glDepthStencil = intBuffer.get(0);
        } else {
          if (0 < fb.depthBits) {
            pgl.genRenderbuffers(1, intBuffer);
            fb.glDepth = intBuffer.get(0);
          }
          if (0 < fb.stencilBits) {
            pgl.genRenderbuffers(1, intBuffer);
            fb.glStencil = intBuffer.get(0);
          }
        }

        this.glFbo = fb.glFbo;
        this.glDepth = fb.glDepth;
        this.glStencil = fb.glStencil;
        this.glDepthStencil = fb.glDepthStencil;
        this.glMultisample = fb.glMultisample;
      }

      this.context = fb.context;

      refList.add(this);
    }

    private void disposeNative() {
      if (pgl != null) {
        if (glFbo != 0) {
          intBuffer.put(0, glFbo);
          pgl.deleteFramebuffers(1, intBuffer);
          glFbo = 0;
        }
        if (glDepth != 0) {
          intBuffer.put(0, glDepth);
          pgl.deleteRenderbuffers(1, intBuffer);
          glDepth = 0;
        }
        if (glStencil != 0) {
          intBuffer.put(0, glStencil);
          pgl.deleteRenderbuffers(1, intBuffer);
          glStencil = 0;
        }
        if (glDepthStencil != 0) {
          intBuffer.put(0, glDepthStencil);
          pgl.deleteRenderbuffers(1, intBuffer);
          glDepthStencil = 0;
        }
        if (glMultisample != 0) {
          intBuffer.put(0, glMultisample);
          pgl.deleteRenderbuffers(1, intBuffer);
          glMultisample = 0;
        }
        pgl = null;
      }
    }

    void dispose() {
      refList.remove(this);
      disposeNative();
    }

    @Override
    public boolean equals(Object obj) {
      GLResourceFrameBuffer other = (GLResourceFrameBuffer)obj;
      return other.glFbo == glFbo &&
             other.glDepth == glDepth &&
             other.glStencil == glStencil &&
             other.glDepthStencil == glDepthStencil &&
             other.glMultisample == glMultisample &&
             other.context == context;
    }

    @Override
    public int hashCode() {
      int result = 17;
      result = 31 * result + glFbo;
      result = 31 * result + glDepth;
      result = 31 * result + glStencil;
      result = 31 * result + glDepthStencil;
      result = 31 * result + glMultisample;
      result = 31 * result + context;
      return result;
    }
  }


  //////////////////////////////////////////////////////////////

  // FRAMEBUFFERS


  protected void pushFramebuffer() {
    PGraphicsOpenGL ppg = getPrimaryPG();
    if (ppg.fbStackDepth == FB_STACK_DEPTH) {
      throw new RuntimeException("Too many pushFramebuffer calls");
    }
    ppg.fbStack[ppg.fbStackDepth] = ppg.currentFramebuffer;
    ppg.fbStackDepth++;
  }


  protected void setFramebuffer(FrameBuffer fbo) {
    PGraphicsOpenGL ppg = getPrimaryPG();
    if (ppg.currentFramebuffer != fbo) {
      ppg.currentFramebuffer = fbo;
      if (ppg.currentFramebuffer != null) ppg.currentFramebuffer.bind();
    }
  }


  protected void popFramebuffer() {
    PGraphicsOpenGL ppg = getPrimaryPG();
    if (ppg.fbStackDepth == 0) {
      throw new RuntimeException("popFramebuffer call is unbalanced.");
    }
    ppg.fbStackDepth--;
    FrameBuffer fbo = ppg.fbStack[ppg.fbStackDepth];
    if (ppg.currentFramebuffer != fbo) {
      ppg.currentFramebuffer.finish();
      ppg.currentFramebuffer = fbo;
      if (ppg.currentFramebuffer != null) ppg.currentFramebuffer.bind();
    }
  }


  protected FrameBuffer getCurrentFB() {
    return getPrimaryPG().currentFramebuffer;
  }


  //////////////////////////////////////////////////////////////

  // FRAME RENDERING


  protected void createPolyBuffers() {
    if (!polyBuffersCreated || polyBuffersContextIsOutdated()) {
      polyBuffersContext = pgl.getCurrentContext();

      bufPolyVertex = new VertexBuffer(this, PGL.ARRAY_BUFFER, 3, PGL.SIZEOF_FLOAT);
      bufPolyColor = new VertexBuffer(this, PGL.ARRAY_BUFFER, 1, PGL.SIZEOF_INT);
      bufPolyNormal = new VertexBuffer(this, PGL.ARRAY_BUFFER, 3, PGL.SIZEOF_FLOAT);
      bufPolyTexcoord = new VertexBuffer(this, PGL.ARRAY_BUFFER, 2, PGL.SIZEOF_FLOAT);
      bufPolyAmbient = new VertexBuffer(this, PGL.ARRAY_BUFFER, 1, PGL.SIZEOF_INT);
      bufPolySpecular = new VertexBuffer(this, PGL.ARRAY_BUFFER, 1, PGL.SIZEOF_INT);
      bufPolyEmissive = new VertexBuffer(this, PGL.ARRAY_BUFFER, 1, PGL.SIZEOF_INT);
      bufPolyShininess = new VertexBuffer(this, PGL.ARRAY_BUFFER, 1, PGL.SIZEOF_FLOAT);
      pgl.bindBuffer(PGL.ARRAY_BUFFER, 0);
      bufPolyIndex = new VertexBuffer(this, PGL.ELEMENT_ARRAY_BUFFER, 1, PGL.SIZEOF_INDEX, true);
      pgl.bindBuffer(PGL.ELEMENT_ARRAY_BUFFER, 0);

      polyBuffersCreated = true;
    }

    boolean created = false;
    for (String name: polyAttribs.keySet()) {
      VertexAttribute attrib = polyAttribs.get(name);
      if (!attrib.bufferCreated() || polyBuffersContextIsOutdated()) {
        attrib.createBuffer(pgl);
        created = true;
      }
    }
    if (created) pgl.bindBuffer(PGL.ARRAY_BUFFER, 0);
  }


  protected void updatePolyBuffers(boolean lit, boolean tex,
                                   boolean needNormals, boolean needTexCoords) {
    createPolyBuffers();

    int size = tessGeo.polyVertexCount;
    int sizef = size * PGL.SIZEOF_FLOAT;
    int sizei = size * PGL.SIZEOF_INT;

    tessGeo.updatePolyVerticesBuffer();
    pgl.bindBuffer(PGL.ARRAY_BUFFER, bufPolyVertex.glId);
    pgl.bufferData(PGL.ARRAY_BUFFER, 4 * sizef,
                   tessGeo.polyVerticesBuffer, PGL.STATIC_DRAW);

    tessGeo.updatePolyColorsBuffer();
    pgl.bindBuffer(PGL.ARRAY_BUFFER, bufPolyColor.glId);
    pgl.bufferData(PGL.ARRAY_BUFFER, sizei,
                   tessGeo.polyColorsBuffer, PGL.STATIC_DRAW);

    if (lit) {
      tessGeo.updatePolyAmbientBuffer();
      pgl.bindBuffer(PGL.ARRAY_BUFFER, bufPolyAmbient.glId);
      pgl.bufferData(PGL.ARRAY_BUFFER, sizei,
                     tessGeo.polyAmbientBuffer, PGL.STATIC_DRAW);

      tessGeo.updatePolySpecularBuffer();
      pgl.bindBuffer(PGL.ARRAY_BUFFER, bufPolySpecular.glId);
      pgl.bufferData(PGL.ARRAY_BUFFER, sizei,
                     tessGeo.polySpecularBuffer, PGL.STATIC_DRAW);

      tessGeo.updatePolyEmissiveBuffer();
      pgl.bindBuffer(PGL.ARRAY_BUFFER, bufPolyEmissive.glId);
      pgl.bufferData(PGL.ARRAY_BUFFER, sizei,
                     tessGeo.polyEmissiveBuffer, PGL.STATIC_DRAW);

      tessGeo.updatePolyShininessBuffer();
      pgl.bindBuffer(PGL.ARRAY_BUFFER, bufPolyShininess.glId);
      pgl.bufferData(PGL.ARRAY_BUFFER, sizef,
                     tessGeo.polyShininessBuffer, PGL.STATIC_DRAW);
    }

    if (lit || needNormals) {
      tessGeo.updatePolyNormalsBuffer();
      pgl.bindBuffer(PGL.ARRAY_BUFFER, bufPolyNormal.glId);
      pgl.bufferData(PGL.ARRAY_BUFFER, 3 * sizef,
                     tessGeo.polyNormalsBuffer, PGL.STATIC_DRAW);
    }

    if (tex || needTexCoords) {
      tessGeo.updatePolyTexCoordsBuffer();
      pgl.bindBuffer(PGL.ARRAY_BUFFER, bufPolyTexcoord.glId);
      pgl.bufferData(PGL.ARRAY_BUFFER, 2 * sizef,
                     tessGeo.polyTexCoordsBuffer, PGL.STATIC_DRAW);
    }

    for (String name: polyAttribs.keySet()) {
      VertexAttribute attrib = polyAttribs.get(name);
      tessGeo.updateAttribBuffer(name);
      pgl.bindBuffer(PGL.ARRAY_BUFFER, attrib.buf.glId);
      pgl.bufferData(PGL.ARRAY_BUFFER, attrib.sizeInBytes(size),
                     tessGeo.polyAttribBuffers.get(name), PGL.STATIC_DRAW);
    }

    tessGeo.updatePolyIndicesBuffer();
    pgl.bindBuffer(PGL.ELEMENT_ARRAY_BUFFER, bufPolyIndex.glId);
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


  protected void createLineBuffers() {
    if (!lineBuffersCreated || lineBufferContextIsOutdated()) {
      lineBuffersContext = pgl.getCurrentContext();

      bufLineVertex = new VertexBuffer(this, PGL.ARRAY_BUFFER, 3, PGL.SIZEOF_FLOAT);
      bufLineColor = new VertexBuffer(this, PGL.ARRAY_BUFFER, 1, PGL.SIZEOF_INT);
      bufLineAttrib = new VertexBuffer(this, PGL.ARRAY_BUFFER, 4, PGL.SIZEOF_FLOAT);
      pgl.bindBuffer(PGL.ARRAY_BUFFER, 0);
      bufLineIndex = new VertexBuffer(this, PGL.ELEMENT_ARRAY_BUFFER, 1, PGL.SIZEOF_INDEX, true);
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
    pgl.bindBuffer(PGL.ARRAY_BUFFER, bufLineVertex.glId);
    pgl.bufferData(PGL.ARRAY_BUFFER, 4 * sizef, tessGeo.lineVerticesBuffer,
                   PGL.STATIC_DRAW);

    tessGeo.updateLineColorsBuffer();
    pgl.bindBuffer(PGL.ARRAY_BUFFER, bufLineColor.glId);
    pgl.bufferData(PGL.ARRAY_BUFFER, sizei,
                   tessGeo.lineColorsBuffer, PGL.STATIC_DRAW);

    tessGeo.updateLineDirectionsBuffer();
    pgl.bindBuffer(PGL.ARRAY_BUFFER, bufLineAttrib.glId);
    pgl.bufferData(PGL.ARRAY_BUFFER, 4 * sizef,
                   tessGeo.lineDirectionsBuffer, PGL.STATIC_DRAW);

    tessGeo.updateLineIndicesBuffer();
    pgl.bindBuffer(PGL.ELEMENT_ARRAY_BUFFER, bufLineIndex.glId);
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


  protected void createPointBuffers() {
    if (!pointBuffersCreated || pointBuffersContextIsOutdated()) {
      pointBuffersContext = pgl.getCurrentContext();

      bufPointVertex = new VertexBuffer(this, PGL.ARRAY_BUFFER, 3, PGL.SIZEOF_FLOAT);
      bufPointColor = new VertexBuffer(this, PGL.ARRAY_BUFFER, 1, PGL.SIZEOF_INT);
      bufPointAttrib = new VertexBuffer(this, PGL.ARRAY_BUFFER, 2, PGL.SIZEOF_FLOAT);
      pgl.bindBuffer(PGL.ARRAY_BUFFER, 0);
      bufPointIndex = new VertexBuffer(this, PGL.ELEMENT_ARRAY_BUFFER, 1, PGL.SIZEOF_INDEX, true);
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
    pgl.bindBuffer(PGL.ARRAY_BUFFER, bufPointVertex.glId);
    pgl.bufferData(PGL.ARRAY_BUFFER, 4 * sizef,
                   tessGeo.pointVerticesBuffer, PGL.STATIC_DRAW);

    tessGeo.updatePointColorsBuffer();
    pgl.bindBuffer(PGL.ARRAY_BUFFER, bufPointColor.glId);
    pgl.bufferData(PGL.ARRAY_BUFFER, sizei,
                   tessGeo.pointColorsBuffer, PGL.STATIC_DRAW);

    tessGeo.updatePointOffsetsBuffer();
    pgl.bindBuffer(PGL.ARRAY_BUFFER, bufPointAttrib.glId);
    pgl.bufferData(PGL.ARRAY_BUFFER, 2 * sizef,
                   tessGeo.pointOffsetsBuffer, PGL.STATIC_DRAW);

    tessGeo.updatePointIndicesBuffer();
    pgl.bindBuffer(PGL.ELEMENT_ARRAY_BUFFER, bufPointIndex.glId);
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


  @Override
  public void beginDraw() {
    if (primaryGraphics) {
      if (!initialized) {
        initPrimary();
      }
      setCurrentPG(this);
    } else {
      pgl.getGL(getPrimaryPGL());
      getPrimaryPG().setCurrentPG(this);
    }

    if (!pgl.threadIsCurrent()) {
      PGraphics.showWarning(GL_THREAD_NOT_CURRENT);
      return;
    }

    // This has to go after the surface initialization, otherwise offscreen
    // surfaces will have a null gl object.
    report("top beginDraw()");

    if (!checkGLThread()) {
      return;
    }

    if (drawing) {
      return;
    }

    if (!primaryGraphics && getPrimaryPG().texCache.containsTexture(this)) {
      // This offscreen surface is being used as a texture earlier in draw,
      // so we should update the rendering up to this point since it will be
      // modified.
      getPrimaryPG().flush();
    }

    if (!glParamsRead) {
      getGLParameters();
    }

    setViewport();
    if (primaryGraphics) {
      beginOnscreenDraw();
    } else {
      beginOffscreenDraw();
    }
    checkSettings();

    drawing = true;

    report("bot beginDraw()");
  }


  @Override
  public void endDraw() {
    report("top endDraw()");

    if (!drawing) {
      return;
    }

    // Flushing any remaining geometry.
    flush();

    if (primaryGraphics) {
      endOnscreenDraw();
    } else {
      endOffscreenDraw();
    }

    if (primaryGraphics) {
      setCurrentPG(null);
    } else {
      getPrimaryPG().setCurrentPG();
    }
    drawing = false;

    report("bot endDraw()");
  }


  protected PGraphicsOpenGL getPrimaryPG() {
    if (primaryGraphics) {
      return this;
    } else {
      return (PGraphicsOpenGL)parent.g;
    }
  }

  protected void setCurrentPG(PGraphicsOpenGL pg) {
    currentPG = pg;
  }

  protected void setCurrentPG() {
    currentPG = this;
  }

  protected PGraphicsOpenGL getCurrentPG() {
    return currentPG;
  }

  protected PGL getPrimaryPGL() {
    if (primaryGraphics) {
      return pgl;
    } else {
      return ((PGraphicsOpenGL)parent.g).pgl;
    }
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

    if (smooth < 1) {
      pgl.disable(PGL.MULTISAMPLE);
    } else {
      pgl.enable(PGL.MULTISAMPLE);
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

    FrameBuffer fb = getCurrentFB();
    if (fb != null) {
      fb.bind();
      if (drawBufferSupported) pgl.drawBuffer(fb.getDefaultDrawBuffer());
    }
  }

  protected void beginBindFramebuffer(int target, int framebuffer) {
    // Actually, nothing to do here.
  }

  protected void endBindFramebuffer(int target, int framebuffer) {
    FrameBuffer fb = getCurrentFB();
    if (framebuffer == 0 && fb != null && fb.glFbo != 0) {
      // The user is setting the framebuffer to 0 (screen buffer), but the
      // renderer is drawing into an offscreen buffer.
      fb.bind();
    }
  }

  protected void beginReadPixels() {
    beginPixelsOp(OP_READ);
  }

  protected void endReadPixels() {
    endPixelsOp();
  }

  protected void beginPixelsOp(int op) {
    FrameBuffer pixfb = null;
    if (primaryGraphics) {
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
      FrameBuffer ofb = offscreenFramebuffer;
      FrameBuffer mfb = multisampleFramebuffer;
      if (op == OP_READ) {
        if (offscreenMultisample) {
          // Making sure the offscreen FBO is up-to-date
          int mask = PGL.COLOR_BUFFER_BIT;
          if (hints[ENABLE_BUFFER_READING]) {
            mask |= PGL.DEPTH_BUFFER_BIT | PGL.STENCIL_BUFFER_BIT;
          }
          if (ofb != null && mfb != null) {
            mfb.copy(ofb, mask);
          }
        }
        // We always read the screen pixels from the color FBO.
        pixfb = ofb;
      } else if (op == OP_WRITE) {
        // We can write directly to the color FBO, or to the multisample FBO
        // if multisampling is enabled.
        pixfb = offscreenMultisample ? mfb : ofb;
      }
    }

    // Set the framebuffer where the pixel operation shall be carried out.
    if (pixfb != null && pixfb != getCurrentFB()) {
      pushFramebuffer();
      setFramebuffer(pixfb);
      pixOpChangedFB = true;
    }

    // We read from/write to the draw buffer.
    if (op == OP_READ) {
      if (readBufferSupported) pgl.readBuffer(getCurrentFB().getDefaultDrawBuffer());
    } else if (op == OP_WRITE) {
      if (drawBufferSupported) pgl.drawBuffer(getCurrentFB().getDefaultDrawBuffer());
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
    if (readBufferSupported) pgl.readBuffer(getCurrentFB().getDefaultReadBuffer());
    if (drawBufferSupported) pgl.drawBuffer(getCurrentFB().getDefaultDrawBuffer());

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
    } else if (which == ENABLE_DEPTH_SORT) {
      if (is3D()) {
        flush();
        if (sorter == null) sorter = new DepthSorter(this);
        isDepthSortingEnabled = true;
      } else {
        PGraphics.showWarning("Depth sorting can only be enabled in 3D");
      }
    } else if (which == DISABLE_DEPTH_SORT) {
      if (is3D()) {
        flush();
        isDepthSortingEnabled = false;
      }
    } else if (which == ENABLE_BUFFER_READING) {
      restartPGL();
    } else if (which == DISABLE_BUFFER_READING) {
      restartPGL();
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

  // CREATE SHAPE


  @Override
  protected PShape createShapeFamily(int type) {
    PShape shape = new PShapeOpenGL(this, type);
    if (is3D()) {
      shape.set3D(true);
    }
    return shape;
  }


  @Override
  protected PShape createShapePrimitive(int kind, float... p) {
    PShape shape = new PShapeOpenGL(this, kind, p);
    if (is3D()) {
      shape.set3D(true);
    }
    return shape;
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
    } else {
      // pixels array is not up-to-date anymore
      loaded = false;
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
    } else {
      // pixels array is not up-to-date anymore
      loaded = false;
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
    if (textureImage != null) PGraphics.showWarning(MISSING_UV_TEXCOORDS_ERROR);
  }


  @Override
  public void vertex(float x, float y, float u, float v) {
    vertexImpl(x, y, 0, u, v);
  }


  @Override
  public void vertex(float x, float y, float z) {
    vertexImpl(x, y, z, 0, 0);
    if (textureImage != null) PGraphics.showWarning(MISSING_UV_TEXCOORDS_ERROR);
  }


  @Override
  public void vertex(float x, float y, float z, float u, float v) {
    vertexImpl(x, y, z, u, v);
  }


  @Override
  public void attribPosition(String name, float x, float y, float z) {
    VertexAttribute attrib = attribImpl(name, VertexAttribute.POSITION,
                                        PGL.FLOAT, 3);
    if (attrib != null) attrib.set(x, y, z);
  }


  @Override
  public void attribNormal(String name, float nx, float ny, float nz) {
    VertexAttribute attrib = attribImpl(name, VertexAttribute.NORMAL,
                                        PGL.FLOAT, 3);
    if (attrib != null) attrib.set(nx, ny, nz);
  }


  @Override
  public void attribColor(String name, int color) {
    VertexAttribute attrib = attribImpl(name, VertexAttribute.COLOR, PGL.INT, 1);
    if (attrib != null) attrib.set(new int[] {color});
  }


  @Override
  public void attrib(String name, float... values) {
    VertexAttribute attrib = attribImpl(name, VertexAttribute.OTHER,
                                        PGL.FLOAT, values.length);
    if (attrib != null) attrib.set(values);
  }


  @Override
  public void attrib(String name, int... values) {
    VertexAttribute attrib = attribImpl(name, VertexAttribute.OTHER,
                                        PGL.INT, values.length);
    if (attrib != null) attrib.set(values);
  }


  @Override
  public void attrib(String name, boolean... values) {
    VertexAttribute attrib = attribImpl(name, VertexAttribute.OTHER,
                                        PGL.BOOL, values.length);
    if (attrib != null) attrib.set(values);
  }


  protected VertexAttribute attribImpl(String name, int kind, int type, int size) {
    if (4 < size) {
      PGraphics.showWarning("Vertex attributes cannot have more than 4 values");
      return null;
    }
    VertexAttribute attrib = polyAttribs.get(name);
    if (attrib == null) {
      attrib = new VertexAttribute(this, name, kind, type, size);
      polyAttribs.put(name, attrib);
      inGeo.initAttrib(attrib);
      tessGeo.initAttrib(attrib);
    }
    if (attrib.kind != kind) {
      PGraphics.showWarning("The attribute kind cannot be changed after creation");
      return null;
    }
    if (attrib.type != type) {
      PGraphics.showWarning("The attribute type cannot be changed after creation");
      return null;
    }
    if (attrib.size != size) {
      PGraphics.showWarning("New value for vertex attribute has wrong number of values");
      return null;
    }
    return attrib;
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
    tessellator.setTexCache(texCache, textureImage);
    tessellator.setStroke(stroke);
    tessellator.setStrokeColor(strokeColor);
    tessellator.setStrokeWeight(strokeWeight);
    tessellator.setStrokeCap(strokeCap);
    tessellator.setStrokeJoin(strokeJoin);
    tessellator.setRenderer(this);
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
      tessellator.tessellatePolygon(true, mode == CLOSE,
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
    tessellator.setTexCache(texCache, textureImage);
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

      if (hasPolys && !isDepthSortingEnabled) {
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

      if (hasPolys && isDepthSortingEnabled) {
        // We flush after lines so they are visible
        // under transparent polygons
        flushSortedPolys();
        if (raw != null) {
          rawSortedPolys();
        }
      }

      if (flushMode == FLUSH_WHEN_FULL) {
        modelview = modelview0;
        modelviewInv = modelviewInv0;
        updateProjmodelview();
      }

      loaded = false;
    }

    tessGeo.clear();
    texCache.clear();
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

        shader.setVertexAttribute(bufPolyVertex.glId, 4, PGL.FLOAT, 0,
                                  4 * voffset * PGL.SIZEOF_FLOAT);
        shader.setColorAttribute(bufPolyColor.glId, 4, PGL.UNSIGNED_BYTE, 0,
                                 4 * voffset * PGL.SIZEOF_BYTE);

        if (lights) {
          shader.setNormalAttribute(bufPolyNormal.glId, 3, PGL.FLOAT, 0,
                                    3 * voffset * PGL.SIZEOF_FLOAT);
          shader.setAmbientAttribute(bufPolyAmbient.glId, 4, PGL.UNSIGNED_BYTE, 0,
                                     4 * voffset * PGL.SIZEOF_BYTE);
          shader.setSpecularAttribute(bufPolySpecular.glId, 4, PGL.UNSIGNED_BYTE, 0,
                                      4 * voffset * PGL.SIZEOF_BYTE);
          shader.setEmissiveAttribute(bufPolyEmissive.glId, 4, PGL.UNSIGNED_BYTE, 0,
                                      4 * voffset * PGL.SIZEOF_BYTE);
          shader.setShininessAttribute(bufPolyShininess.glId, 1, PGL.FLOAT, 0,
                                       voffset * PGL.SIZEOF_FLOAT);
        }

        if (lights || needNormals) {
          shader.setNormalAttribute(bufPolyNormal.glId, 3, PGL.FLOAT, 0,
                                    3 * voffset * PGL.SIZEOF_FLOAT);
        }

        if (tex != null || needTexCoords) {
          shader.setTexcoordAttribute(bufPolyTexcoord.glId, 2, PGL.FLOAT, 0,
                                      2 * voffset * PGL.SIZEOF_FLOAT);
          shader.setTexture(tex);
        }

        for (VertexAttribute attrib: polyAttribs.values()) {
          if (!attrib.active(shader)) continue;
          attrib.bind(pgl);
          shader.setAttributeVBO(attrib.glLoc, attrib.buf.glId,
                                 attrib.tessSize, attrib.type,
                                 attrib.isColor(), 0, attrib.sizeInBytes(voffset));
        }

        shader.draw(bufPolyIndex.glId, icount, ioffset);
      }

      for (VertexAttribute attrib: polyAttribs.values()) {
        if (attrib.active(shader)) attrib.unbind(pgl);
      }
      shader.unbind();
    }
    unbindPolyBuffers();
  }

  protected void flushSortedPolys() {
    boolean customShader = polyShader != null;
    boolean needNormals = customShader ? polyShader.accessNormals() : false;
    boolean needTexCoords = customShader ? polyShader.accessTexCoords() : false;

    sorter.sort(tessGeo);

    int triangleCount = tessGeo.polyIndexCount / 3;
    int[] texMap = sorter.texMap;
    int[] voffsetMap = sorter.voffsetMap;

    int[] vertexOffset = tessGeo.polyIndexCache.vertexOffset;

    updatePolyBuffers(lights, texCache.hasTextures, needNormals, needTexCoords);

    int ti = 0;

    while (ti < triangleCount) {

      int startTi = ti;
      int texId = texMap[ti];
      int voffsetId = voffsetMap[ti];

      do {
        ++ti;
      } while (ti < triangleCount &&
          texId == texMap[ti] &&
          voffsetId == voffsetMap[ti]);

      int endTi = ti;

      Texture tex = texCache.getTexture(texId);

      int voffset = vertexOffset[voffsetId];

      int ioffset = 3 * startTi;
      int icount = 3 * (endTi - startTi);

      // If the renderer is 2D, then lights should always be false,
      // so no need to worry about that.
      PShader shader = getPolyShader(lights, tex != null);
      shader.bind();

      shader.setVertexAttribute(bufPolyVertex.glId, 4, PGL.FLOAT, 0,
                                4 * voffset * PGL.SIZEOF_FLOAT);
      shader.setColorAttribute(bufPolyColor.glId, 4, PGL.UNSIGNED_BYTE, 0,
                               4 * voffset * PGL.SIZEOF_BYTE);

      if (lights) {
        shader.setNormalAttribute(bufPolyNormal.glId, 3, PGL.FLOAT, 0,
                                  3 * voffset * PGL.SIZEOF_FLOAT);
        shader.setAmbientAttribute(bufPolyAmbient.glId, 4, PGL.UNSIGNED_BYTE, 0,
                                   4 * voffset * PGL.SIZEOF_BYTE);
        shader.setSpecularAttribute(bufPolySpecular.glId, 4, PGL.UNSIGNED_BYTE, 0,
                                    4 * voffset * PGL.SIZEOF_BYTE);
        shader.setEmissiveAttribute(bufPolyEmissive.glId, 4, PGL.UNSIGNED_BYTE, 0,
                                    4 * voffset * PGL.SIZEOF_BYTE);
        shader.setShininessAttribute(bufPolyShininess.glId, 1, PGL.FLOAT, 0,
                                     voffset * PGL.SIZEOF_FLOAT);
      }

      if (lights || needNormals) {
        shader.setNormalAttribute(bufPolyNormal.glId, 3, PGL.FLOAT, 0,
                                  3 * voffset * PGL.SIZEOF_FLOAT);
      }

      if (tex != null || needTexCoords) {
        shader.setTexcoordAttribute(bufPolyTexcoord.glId, 2, PGL.FLOAT, 0,
                                    2 * voffset * PGL.SIZEOF_FLOAT);
        shader.setTexture(tex);
      }

      for (VertexAttribute attrib: polyAttribs.values()) {
        if (!attrib.active(shader)) continue;
        attrib.bind(pgl);
        shader.setAttributeVBO(attrib.glLoc, attrib.buf.glId,
                               attrib.tessSize, attrib.type,
                               attrib.isColor(), 0, attrib.sizeInBytes(voffset));
      }

      shader.draw(bufPolyIndex.glId, icount, ioffset);

      for (VertexAttribute attrib: polyAttribs.values()) {
        if (attrib.active(shader)) attrib.unbind(pgl);
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


  void rawSortedPolys() {
    raw.colorMode(RGB);
    raw.noStroke();
    raw.beginShape(TRIANGLES);

    float[] vertices = tessGeo.polyVertices;
    int[] color = tessGeo.polyColors;
    float[] uv = tessGeo.polyTexCoords;
    short[] indices = tessGeo.polyIndices;

    sorter.sort(tessGeo);
    int[] triangleIndices = sorter.triangleIndices;
    int[] texMap = sorter.texMap;
    int[] voffsetMap = sorter.voffsetMap;

    int[] vertexOffset = tessGeo.polyIndexCache.vertexOffset;

    for (int i = 0; i < tessGeo.polyIndexCount/3; i++) {
      int ti = triangleIndices[i];
      PImage tex = texCache.getTextureImage(texMap[ti]);
      int voffset = vertexOffset[voffsetMap[ti]];

      int i0 = voffset + indices[3*ti+0];
      int i1 = voffset + indices[3*ti+1];
      int i2 = voffset + indices[3*ti+2];

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

      if (tex != null) {
        raw.texture(tex);
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

      shader.setVertexAttribute(bufLineVertex.glId, 4, PGL.FLOAT, 0,
                                4 * voffset * PGL.SIZEOF_FLOAT);
      shader.setColorAttribute(bufLineColor.glId, 4, PGL.UNSIGNED_BYTE, 0,
                               4 * voffset * PGL.SIZEOF_BYTE);
      shader.setLineAttribute(bufLineAttrib.glId, 4, PGL.FLOAT, 0,
                              4 * voffset * PGL.SIZEOF_FLOAT);

      shader.draw(bufLineIndex.glId, icount, ioffset);
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

      shader.setVertexAttribute(bufPointVertex.glId, 4, PGL.FLOAT, 0,
                                4 * voffset * PGL.SIZEOF_FLOAT);
      shader.setColorAttribute(bufPointColor.glId, 4, PGL.UNSIGNED_BYTE, 0,
                               4 * voffset * PGL.SIZEOF_BYTE);
      shader.setPointAttribute(bufPointAttrib.glId, 2, PGL.FLOAT, 0,
                               2 * voffset * PGL.SIZEOF_FLOAT);

      shader.draw(bufPointIndex.glId, icount, ioffset);
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
    bezierVertexCheck(shape, inGeo.vertexCount);
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
    bezierVertexCheck(shape, inGeo.vertexCount);
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
    curveVertexCheck(shape);
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
    endShape(CLOSE);
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

/*
  @Override
  public void smooth() {
    if (quality < 2) {
      smooth(2);
    } else {
      smooth(quality);
    }
  }


  @Override
  public void smooth(int level) {
    if (smoothDisabled || PGL.MAX_SAMPLES == -1) return;

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

      if (quality <= 1) {
        quality = 0;
        textureSampling = Texture.POINT;
      } else {
        textureSampling = Texture.TRILINEAR;
      }

      // This will trigger a surface restart next time
      // requestDraw() is called.
//      restartPGL();
    }
  }


  @Override
  public void noSmooth() {
    if (smoothDisabled) return;

    smooth = false;
    textureSampling = Texture.POINT;

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
  */


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
        translate(x - shape.getWidth() / 2, y - shape.getHeight() / 2,
                  z - shape.getDepth() / 2);

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
    if (textFont == null) defaultFontOrDeath("textDescent");
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
  protected void handleTextSize(float size) {
    Object font = textFont.getNative();
    if (font != null) {
      Object dfont = pgl.getDerivedFont(font, size);
      textFont.setNative(dfont);
    }
    super.handleTextSize(size);
  }


  /**
   * Implementation of actual drawing for a line of text.
   */
  @Override
  protected void textLineImpl(char buffer[], int start, int stop,
                              float x, float y) {
    if (textMode == MODEL) {
      textTex = getFontTexture(textFont);

      if (textTex == null || textTex.contextIsOutdated()) {
        textTex = new FontTexture(this, textFont, is3D());
        setFontTexture(textFont, textTex);
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
          tinfo = textTex.addToTexture(this, glyph);
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
    beginShape(QUADS);
    texture(textTex.getTexture(info));
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

    boolean open = false;
    beginShape();
    while (!outline.isDone()) {
      int type = outline.currentSegment(textPoints);
      if (!open) {
        beginContour();
        open = true;
      }
      if (type == PGL.SEG_MOVETO || type == PGL.SEG_LINETO) {  // 1 point
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
        open = false;
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


  static protected float matrixScale(PMatrix matrix) {
    // Volumetric scaling factor that is associated to the given
    // transformation matrix, which is given by the absolute value of its
    // determinant:
    float factor = 1;

    if (matrix != null) {
      if (matrix instanceof PMatrix2D) {
        PMatrix2D tr = (PMatrix2D)matrix;
        float areaScaleFactor = Math.abs(tr.m00 * tr.m11 - tr.m01 * tr.m10);
        factor = (float) Math.sqrt(areaScaleFactor);
      } else if (matrix instanceof PMatrix3D) {
        PMatrix3D tr = (PMatrix3D)matrix;
        float volumeScaleFactor =
          Math.abs(tr.m00 * (tr.m11 * tr.m22 - tr.m12 * tr.m21) +
                   tr.m01 * (tr.m12 * tr.m20 - tr.m10 * tr.m22) +
                   tr.m02 * (tr.m10 * tr.m21 - tr.m11 * tr.m20));
        factor = (float) Math.pow(volumeScaleFactor, 1.0f / 3.0f);
      }
    }
    return factor;
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
    eyeDist = PApplet.sqrt(z0 * z0 + z1 * z1 + z2 * z2);
    if (nonZero(eyeDist)) {
      z0 /= eyeDist;
      z1 /= eyeDist;
      z2 /= eyeDist;
    }

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
    float xmag = PApplet.sqrt(x0 * x0 + x1 * x1 + x2 * x2);
    if (nonZero(xmag)) {
      x0 /= xmag;
      x1 /= xmag;
      x2 /= xmag;
    }

    float ymag = PApplet.sqrt(y0 * y0 + y1 * y1 + y2 * y2);
    if (nonZero(ymag)) {
      y0 /= ymag;
      y1 /= ymag;
      y2 /= ymag;
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
    ortho(-width/2f, width/2f, -height/2f, height/2f, 0, eyeDist * 10);
  }


  /**
   * Calls ortho() with the specified size of the viewing volume along
   * the X and Z directions.
   */
  @Override
  public void ortho(float left, float right,
                    float bottom, float top) {
    ortho(left, right, bottom, top, 0, eyeDist * 10);
  }


  /**
   * Sets an orthographic projection.
   *
   */
  @Override
  public void ortho(float left, float right,
                    float bottom, float top,
                    float near, float far) {
    float w = right - left;
    float h = top - bottom;
    float d = far - near;

    // Flushing geometry with a different perspective configuration.
    flush();

    float x = +2.0f / w;
    float y = +2.0f / h;
    float z = -2.0f / d;

    float tx = -(right + left) / w;
    float ty = -(top + bottom) / h;
    float tz = -(far + near)   / d;

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
    flush();
    lights = true;
  }


  protected void disableLighting() {
    flush();
    lights = false;
  }


  protected void lightPosition(int num, float x, float y, float z,
                               boolean dir) {
    lightPosition[4 * num + 0] =
      x*modelview.m00 + y*modelview.m01 + z*modelview.m02 + modelview.m03;
    lightPosition[4 * num + 1] =
      x*modelview.m10 + y*modelview.m11 + z*modelview.m12 + modelview.m13;
    lightPosition[4 * num + 2] =
      x*modelview.m20 + y*modelview.m21 + z*modelview.m22 + modelview.m23;

    // Used to indicate if the light is directional or not.
    lightPosition[4 * num + 3] = dir ? 0 : 1;
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

    float d = PApplet.dist(0, 0, 0, nx, ny, nz);
    if (0 < d) {
      float invn = 1.0f / d;
      lightNormal[3 * num + 0] = invn * nx;
      lightNormal[3 * num + 1] = invn * ny;
      lightNormal[3 * num + 2] = invn * nz;
    } else {
      lightNormal[3 * num + 0] = 0;
      lightNormal[3 * num + 1] = 0;
      lightNormal[3 * num + 2] = 0;
    }
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
    // Setting the background as opaque. If this an offscreen surface, the
    // alpha channel will be set to 1 in endOffscreenDraw(), even if
    // blending operations during draw create translucent areas in the
    // color buffer.
    backgroundA = 1;
    loaded = false;
  }


  @Override
  protected void backgroundImpl() {
    flush();
    pgl.clearBackground(backgroundR, backgroundG, backgroundB, backgroundA,
                        !hints[DISABLE_DEPTH_MASK]);
    loaded = false;
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
  protected void report(String where) {
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

  // LOAD/UPDATE PIXELS


  // Initializes the pixels array, copying the current contents of the
  // color buffer into it.
  @Override
  public void loadPixels() {
    if (primaryGraphics && sized) {
      // Something wrong going on with threading, sized can never be true if
      // all the steps in a resize happen inside the Animation thread.
      return;
    }

    boolean needEndDraw = false;
    if (!drawing) {
      beginDraw();
      needEndDraw = true;
    }

    if (!loaded) {
      // Draws any remaining geometry in case the user is still not
      // setting/getting new pixels.
      flush();
    }

    allocatePixels();

    if (!loaded) {
      readPixels();
    }

    // Pixels are now up-to-date, set the flag.
    loaded = true;


    if (needEndDraw) {
      endDraw();
    }
  }


  protected void allocatePixels() {
    updatePixelSize();
    if ((pixels == null) || (pixels.length != pixelWidth * pixelHeight)) {
      pixels = new int[pixelWidth * pixelHeight];
      pixelBuffer = PGL.allocateIntBuffer(pixels);
      loaded = false;
    }
  }


  protected void readPixels() {
    updatePixelSize();
    beginPixelsOp(OP_READ);
    try {
      // The readPixelsImpl() call in inside a try/catch block because it appears
      // that (only sometimes) JOGL will run beginDraw/endDraw on the EDT
      // thread instead of the Animation thread right after a resize. Because
      // of this the width and height might have a different size than the
      // one of the pixels arrays.
      pgl.readPixelsImpl(0, 0, pixelWidth, pixelHeight, PGL.RGBA, PGL.UNSIGNED_BYTE,
                         pixelBuffer);
    } catch (IndexOutOfBoundsException e) {
      // Silently catch the exception.
    }
    endPixelsOp();
    try {
      // Idem...
      PGL.getIntArray(pixelBuffer, pixels);
      PGL.nativeToJavaARGB(pixels, pixelWidth, pixelHeight);
    } catch (ArrayIndexOutOfBoundsException e) {
    }
  }


  protected void drawPixels(int x, int y, int w, int h) {
    int f = (int)pgl.getPixelScale();
    int len = f * w * f * h;
    if (nativePixels == null || nativePixels.length < len) {
      nativePixels = new int[len];
      nativePixelBuffer = PGL.allocateIntBuffer(nativePixels);
    }

    try {
      if (0 < x || 0 < y || w < width || h < height) {
        // The pixels to be copied to the texture need to be consecutive, and
        // they are not in the pixels array, so putting each row one after
        // another in nativePixels.
        int offset0 = f * (y * width + x);
        int offset1 = 0;

        for (int yc = f * y; yc < f * (y + h); yc++) {
          System.arraycopy(pixels, offset0, nativePixels, offset1, f * w);
          offset0 += f * width;
          offset1 += f * w;
        }
      } else {
        PApplet.arrayCopy(pixels, 0, nativePixels, 0, len);
      }
      PGL.javaToNativeARGB(nativePixels, f * w, f * h);
    } catch (ArrayIndexOutOfBoundsException e) {
    }
    PGL.putIntArray(nativePixelBuffer, nativePixels);
    // Copying pixel buffer to screen texture...
    if (primaryGraphics && !pgl.isFBOBacked()) {
      // First making sure that the screen texture is valid. Only in the case
      // of non-FBO-backed primary surface we might need to create the texture.
      loadTextureImpl(POINT, false);
    }

    boolean needToDrawTex = primaryGraphics && (!pgl.isFBOBacked() ||
                            (pgl.isFBOBacked() && pgl.isMultisampled())) ||
                            offscreenMultisample;
    if (texture == null) return;
    if (needToDrawTex) {
      // The texture to screen needs to be drawn only if we are on the primary
      // surface w/out FBO-layer, or with FBO-layer and multisampling. Or, we
      // are doing multisampled offscreen. Why? Because in the case of
      // non-multisampled FBO, texture is actually the color buffer used by the
      // color FBO, so with the copy operation we should be done updating the
      // (off)screen buffer.
      // First, copy the pixels to the texture. We don't need to invert the
      // pixel copy because the texture will be drawn inverted.
      int tw = PApplet.min(texture.glWidth - f * x, f * w);
      int th = PApplet.min(texture.glHeight - f * y, f * h);
      pgl.copyToTexture(texture.glTarget, texture.glFormat, texture.glName,
                        f * x, f * y, tw, th, nativePixelBuffer);
      beginPixelsOp(OP_WRITE);
      drawTexture(x, y, w, h);
      endPixelsOp();
    } else {
      // We only need to copy the pixels to the back texture where we are
      // currently drawing to. Because the texture is invertex along Y, we
      // need to reflect that in the vertical arguments.
      pgl.copyToTexture(texture.glTarget, texture.glFormat, texture.glName,
                        f * x, f * (height - (y + h)), f * w, f * h, nativePixelBuffer);
    }
  }


  //////////////////////////////////////////////////////////////

  // GET/SET PIXELS


  @Override
  public int get(int x, int y) {
    loadPixels();
    return super.get(x, y);
  }


  @Override
  protected void getImpl(int sourceX, int sourceY,
                         int sourceWidth, int sourceHeight,
                         PImage target, int targetX, int targetY) {
    loadPixels();
    super.getImpl(sourceX, sourceY, sourceWidth, sourceHeight,
                  target, targetX, targetY);
  }


  @Override
  public void set(int x, int y, int argb) {
    loadPixels();
    super.set(x, y, argb);
  }


  @Override
  protected void setImpl(PImage sourceImage,
                         int sourceX, int sourceY,
                         int sourceWidth, int sourceHeight,
                         int targetX, int targetY) {
    updatePixelSize();

    // Copies the pixels
    loadPixels();
    int sourceOffset = sourceY * sourceImage.pixelWidth + sourceX;
    int targetOffset = targetY * pixelWidth + targetX;
    for (int y = sourceY; y < sourceY + sourceHeight; y++) {
      System.arraycopy(sourceImage.pixels, sourceOffset, pixels, targetOffset, sourceWidth);
      sourceOffset += sourceImage.pixelWidth;
      targetOffset += pixelWidth;
    }

    // Draws the texture, copy() is very efficient because it simply renders
    // the texture cache of sourceImage using OpenGL.
    copy(sourceImage,
         sourceX, sourceY, sourceWidth, sourceHeight,
         targetX, targetY, sourceWidth, sourceHeight);
  }


  //////////////////////////////////////////////////////////////

  // SAVE


  @Override
  public boolean save(String filename) {
    return saveImpl(filename);
  }


  @Override
  protected void processImageBeforeAsyncSave(PImage image) {
    if (image.format == AsyncPixelReader.OPENGL_NATIVE) {
      PGL.nativeToJavaARGB(image.pixels, image.width, image.height);
      image.format = ARGB;
    } else if (image.format == AsyncPixelReader.OPENGL_NATIVE_OPAQUE) {
      PGL.nativeToJavaRGB(image.pixels, image.width, image.height);
      image.format = RGB;
    }
  }


  protected static void completeFinishedPixelTransfers() {
    ongoingPixelTransfersIterable.addAll(ongoingPixelTransfers);
    for (PGraphicsOpenGL.AsyncPixelReader pixelReader :
        ongoingPixelTransfersIterable) {
      // if the getter was not called this frame,
      // tell it to check for completed transfers now
      if (!pixelReader.calledThisFrame) {
        pixelReader.completeFinishedTransfers();
      }
      pixelReader.calledThisFrame = false;
    }
    ongoingPixelTransfersIterable.clear();
  }

  protected static void completeAllPixelTransfers() {
    ongoingPixelTransfersIterable.addAll(ongoingPixelTransfers);
    for (PGraphicsOpenGL.AsyncPixelReader pixelReader :
        ongoingPixelTransfersIterable) {
      pixelReader.completeAllTransfers();
    }
    ongoingPixelTransfersIterable.clear();
  }


  protected class AsyncPixelReader {

    // PImage formats used internally to offload
    // color format conversion to save threads
    static final int OPENGL_NATIVE = -1;
    static final int OPENGL_NATIVE_OPAQUE = -2;

    static final int BUFFER_COUNT = 3;

    int[] pbos;
    long[] fences;
    String[] filenames;
    int[] widths;
    int[] heights;

    int head;
    int tail;
    int size;

    boolean supportsAsyncTransfers;

    boolean calledThisFrame;


    /// PGRAPHICS API //////////////////////////////////////////////////////////

    public AsyncPixelReader() {
      supportsAsyncTransfers = pgl.hasPBOs() && pgl.hasSynchronization();
      if (supportsAsyncTransfers) {
        pbos = new int[BUFFER_COUNT];
        fences = new long[BUFFER_COUNT];
        filenames = new String[BUFFER_COUNT];
        widths = new int[BUFFER_COUNT];
        heights = new int[BUFFER_COUNT];

        IntBuffer intBuffer = PGL.allocateIntBuffer(BUFFER_COUNT);
        intBuffer.rewind();
        pgl.genBuffers(BUFFER_COUNT, intBuffer);
        for (int i = 0; i < BUFFER_COUNT; i++) {
          pbos[i] = intBuffer.get(i);
        }
      }
    }


    public void dispose() {
      if (fences != null) {
        while (size > 0) {
          pgl.deleteSync(fences[tail]);
          size--;
          tail = (tail + 1) % BUFFER_COUNT;
        }
        fences = null;
      }
      if (pbos != null) {
        for (int i = 0; i < BUFFER_COUNT; i++) {
          IntBuffer intBuffer = PGL.allocateIntBuffer(pbos);
          pgl.deleteBuffers(BUFFER_COUNT, intBuffer);
        }
        pbos = null;
      }
      filenames = null;
      widths = null;
      heights = null;
      size = 0;
      head = 0;
      tail = 0;
      calledThisFrame = false;
      ongoingPixelTransfers.remove(this);
    }


    public void readAndSaveAsync(final String filename) {
      if (size > 0) {
        boolean shouldRead = (size == BUFFER_COUNT);
        if (!shouldRead) shouldRead = isLastTransferComplete();
        if (shouldRead) endTransfer();
      } else {
        ongoingPixelTransfers.add(this);
      }
      beginTransfer(filename);
      calledThisFrame = true;
    }


    public void completeFinishedTransfers() {
      if (size <= 0 || !asyncImageSaver.hasAvailableTarget()) return;

      boolean needEndDraw = false;
      if (!drawing) {
        beginDraw();
        needEndDraw = true;
      }

      while (asyncImageSaver.hasAvailableTarget() &&
          isLastTransferComplete()) {
        endTransfer();
      }

      // make sure to always unregister if there are no ongoing transfers
      // so that PGraphics can be GC'd if needed
      if (size <= 0) ongoingPixelTransfers.remove(this);

      if (needEndDraw) endDraw();
    }


    protected void completeAllTransfers() {
      if (size <= 0) return;

      boolean needEndDraw = false;
      if (!drawing) {
        beginDraw();
        needEndDraw = true;
      }

      while (size > 0) {
        endTransfer();
      }

      // make sure to always unregister if there are no ongoing transfers
      // so that PGraphics can be GC'd if needed
      ongoingPixelTransfers.remove(this);

      if (needEndDraw) endDraw();
    }


    /// TRANSFERS //////////////////////////////////////////////////////////////

    public boolean isLastTransferComplete() {
      if (size <= 0) return false;
      int status = pgl.clientWaitSync(fences[tail], 0, 0);
      return (status == PGL.ALREADY_SIGNALED) ||
          (status == PGL.CONDITION_SATISFIED);
    }


    public void beginTransfer(String filename) {
      // check the size of the buffer
      if (widths[head] != pixelWidth || heights[head] != pixelHeight) {
        if (widths[head] * heights[head] != pixelWidth * pixelHeight) {
          pgl.bindBuffer(PGL.PIXEL_PACK_BUFFER, pbos[head]);
          pgl.bufferData(PGL.PIXEL_PACK_BUFFER,
                         Integer.SIZE/8 * pixelWidth * pixelHeight,
                         null, PGL.STREAM_READ);
        }
        widths[head] = pixelWidth;
        heights[head] = pixelHeight;
        pgl.bindBuffer(PGL.PIXEL_PACK_BUFFER, 0);
      }

      pgl.bindBuffer(PGL.PIXEL_PACK_BUFFER, pbos[head]);
      pgl.readPixels(0, 0, pixelWidth, pixelHeight, PGL.RGBA, PGL.UNSIGNED_BYTE, 0);
      pgl.bindBuffer(PGL.PIXEL_PACK_BUFFER, 0);

      fences[head] = pgl.fenceSync(PGL.SYNC_GPU_COMMANDS_COMPLETE, 0);
      filenames[head] = filename;

      head = (head + 1) % BUFFER_COUNT;
      size++;
    }


    public void endTransfer() {
      pgl.deleteSync(fences[tail]);
      pgl.bindBuffer(PGL.PIXEL_PACK_BUFFER, pbos[tail]);
      ByteBuffer readBuffer = pgl.mapBuffer(PGL.PIXEL_PACK_BUFFER,
                                            PGL.READ_ONLY);
      if (readBuffer != null) {
        int format = primaryGraphics ? OPENGL_NATIVE_OPAQUE : OPENGL_NATIVE;
        PImage target = asyncImageSaver.getAvailableTarget(widths[tail],
                                                           heights[tail],
                                                           format);
        if (target == null) return;
        readBuffer.rewind();
        readBuffer.asIntBuffer().get(target.pixels);
        pgl.unmapBuffer(PGL.PIXEL_PACK_BUFFER);
        asyncImageSaver.saveTargetAsync(PGraphicsOpenGL.this, target,
                                        filenames[tail]);
      }

      pgl.bindBuffer(PGL.PIXEL_PACK_BUFFER, 0);

      size--;
      tail = (tail + 1) % BUFFER_COUNT;
    }

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

    if (primaryGraphics) {
      updatePixelSize();

      if (pgl.isFBOBacked()) {
        // In the case of MSAA, this is needed so the back buffer is in sync
        // with the rendering.
        pgl.syncBackTexture();
      } else {
        loadTextureImpl(Texture.POINT, false);

        // Here we go the slow route: we first copy the contents of the color
        // buffer into a pixels array (but we keep it in native format) and
        // then copy this array into the texture.
        if (nativePixels == null || nativePixels.length < pixelWidth * pixelHeight) {
          nativePixels = new int[pixelWidth * pixelHeight];
          nativePixelBuffer = PGL.allocateIntBuffer(nativePixels);
        }

        beginPixelsOp(OP_READ);
        try {
          // See comments in readPixels() for the reason for this try/catch.
          pgl.readPixelsImpl(0, 0, pixelWidth, pixelHeight, PGL.RGBA, PGL.UNSIGNED_BYTE,
                             nativePixelBuffer);
        } catch (IndexOutOfBoundsException e) {
        }
        endPixelsOp();

        if (texture != null) {
          texture.setNative(nativePixelBuffer, 0, 0, pixelWidth, pixelHeight);
        }
      }
    } else if (offscreenMultisample) {
       // We need to copy the contents of the multisampled buffer to the color
       // buffer, so the later is up-to-date with the last drawing.
      FrameBuffer ofb = offscreenFramebuffer;
      FrameBuffer mfb = multisampleFramebuffer;
      if (ofb != null && mfb != null) {
        mfb.copyColor(ofb);
      }
    }

    if (needEndDraw) {
      endDraw();
    }
  }


  // Just marks the whole texture as updated
  public void updateTexture() {
    if (texture != null) {
      texture.updateTexels();
    }
  }


  // Marks the specified rectanglular subregion in the texture as
  // updated.
  public void updateTexture(int x, int y, int w, int h) {
    if (texture != null) {
      texture.updateTexels(x, y, w, h);
    }
  }


  // Draws wherever it is in the screen texture right now to the display.
  public void updateDisplay() {
    flush();
    beginPixelsOp(OP_WRITE);
    drawTexture();
    endPixelsOp();
  }


  protected void loadTextureImpl(int sampling, boolean mipmap) {
    updatePixelSize();
    if (pixelWidth == 0 || pixelHeight == 0) return;
    if (texture == null || texture.contextIsOutdated()) {
      Texture.Parameters params = new Texture.Parameters(ARGB,
                                                         sampling, mipmap);
      texture = new Texture(this, pixelWidth, pixelHeight, params);
      texture.invertedY(true);
      texture.colorBuffer(true);
      setCache(this, texture);
    }
  }


  protected void createPTexture() {
    updatePixelSize();
    if (texture != null) {
      ptexture = new Texture(this, pixelWidth, pixelHeight, texture.getParameters());
      ptexture.invertedY(true);
      ptexture.colorBuffer(true);
    }
  }


  protected void swapOffscreenTextures() {
    FrameBuffer ofb = offscreenFramebuffer;
    if (texture != null && ptexture != null && ofb != null) {
      int temp = texture.glName;
      texture.glName = ptexture.glName;
      ptexture.glName = temp;
      ofb.setColorBuffer(texture);
    }
  }


  protected void drawTexture() {
    if (texture != null) {
      // No blend so the texure replaces wherever is on the screen,
      // irrespective of the alpha
      pgl.disable(PGL.BLEND);
      pgl.drawTexture(texture.glTarget, texture.glName,
                      texture.glWidth, texture.glHeight,
                      0, 0, width, height);
      pgl.enable(PGL.BLEND);
    }
  }


  protected void drawTexture(int x, int y, int w, int h) {
    if (texture != null) {
      // Processing Y axis is inverted with respect to OpenGL, so we need to
      // invert the y coordinates of the screen rectangle.
      pgl.disable(PGL.BLEND);
      pgl.drawTexture(texture.glTarget, texture.glName,
                      texture.glWidth, texture.glHeight,
                      0, 0, width, height,
                      x, y, x + w, y + h,
                      x, height - (y + h), x + w, height - y);
      pgl.enable(PGL.BLEND);
    }
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
    updatePixelSize();
    if (alpha.width != pixelWidth || alpha.height != pixelHeight) {
      throw new RuntimeException("The PImage used with mask() must be " +
      "the same size as the applet.");
    }

    PGraphicsOpenGL ppg = getPrimaryPG();
    if (ppg.maskShader == null) {
      ppg.maskShader = new PShader(parent, defTextureShaderVertURL,
                                           maskShaderFragURL);
    }
    ppg.maskShader.set("mask", alpha);
    filter(ppg.maskShader);
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
    if (!shader.isPolyShader()) {
      PGraphics.showWarning(INVALID_FILTER_SHADER_ERROR);
      return;
    }

    boolean needEndDraw = false;
    if (primaryGraphics) {
      pgl.enableFBOLayer();
    } else if (!drawing) {
      beginDraw();
      needEndDraw = true;
    }
    loadTexture();

    if (filterTexture == null || filterTexture.contextIsOutdated()) {
      filterTexture = new Texture(this, texture.width, texture.height,
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
    if (primaryGraphics) pgl.enableFBOLayer();
    loadTexture();
    if (filterTexture == null || filterTexture.contextIsOutdated()) {
      filterTexture = new Texture(this, texture.width, texture.height, texture.getParameters());
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
    boolean invX = tex.invertedX();
    boolean invY = tex.invertedY();
    int scrX0, scrX1;
    int scrY0, scrY1;
    if (invX) {
      scrX0 = dx + dw;
      scrX1 = dx;
    } else {
      scrX0 = dx;
      scrX1 = dx + dw;
    }

    int texX0 = sx;
    int texX1 = sx + sw;
    int texY0, texY1;
    if (invY) {
      scrY0 = height - (dy + dh);
      scrY1 = height - dy;
      texY0 = tex.height - (sy + sh);
      texY1 = tex.height - sy;
    } else {
      // Because drawTexture uses bottom-to-top orientation of Y axis.
      scrY0 = height - dy;
      scrY1 = height - (dy + dh);
      texY0 = sy;
      texY1 = sy + sh;
    }

    pgl.drawTexture(tex.glTarget, tex.glName, tex.glWidth, tex.glHeight,
                    0, 0, width, height,
                    texX0, texY0, texX1, texY1,
                    scrX0, scrY0, scrX1, scrY1);


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
        pgl.blendEquationSeparate(PGL.FUNC_ADD,
                                  PGL.FUNC_ADD);
      }
      pgl.blendFuncSeparate(PGL.SRC_ALPHA, PGL.ONE_MINUS_SRC_ALPHA,
                            PGL.ONE,       PGL.ONE);

    } else if (blendMode == ADD) {
      if (blendEqSupported) {
        pgl.blendEquationSeparate(PGL.FUNC_ADD,
                                  PGL.FUNC_ADD);
      }
      pgl.blendFuncSeparate(PGL.SRC_ALPHA, PGL.ONE,
                            PGL.ONE,       PGL.ONE);

    } else if (blendMode == SUBTRACT) {
      if (blendEqSupported) {
        pgl.blendEquationSeparate(PGL.FUNC_REVERSE_SUBTRACT,
                                  PGL.FUNC_ADD);
        pgl.blendFuncSeparate(PGL.SRC_ALPHA, PGL.ONE,
                              PGL.ONE,       PGL.ONE);
      } else {
        PGraphics.showWarning(BLEND_DRIVER_ERROR, "SUBTRACT");
      }

    } else if (blendMode == LIGHTEST) {
      if (blendEqSupported) {
        pgl.blendEquationSeparate(PGL.FUNC_MAX,
                                  PGL.FUNC_ADD);
        pgl.blendFuncSeparate(PGL.ONE, PGL.ONE,
                              PGL.ONE, PGL.ONE);
      } else {
        PGraphics.showWarning(BLEND_DRIVER_ERROR, "LIGHTEST");
      }

    } else if (blendMode == DARKEST) {
      if (blendEqSupported) {
        pgl.blendEquationSeparate(PGL.FUNC_MIN,
                                  PGL.FUNC_ADD);
        pgl.blendFuncSeparate(PGL.ONE, PGL.ONE,
                              PGL.ONE, PGL.ONE);
      } else {
        PGraphics.showWarning(BLEND_DRIVER_ERROR, "DARKEST");
      }

    } else if (blendMode == EXCLUSION) {
      if (blendEqSupported) {
        pgl.blendEquationSeparate(PGL.FUNC_ADD,
                                  PGL.FUNC_ADD);
      }
      pgl.blendFuncSeparate(PGL.ONE_MINUS_DST_COLOR, PGL.ONE_MINUS_SRC_COLOR,
                            PGL.ONE,                 PGL.ONE);

    } else if (blendMode == MULTIPLY) {
      if (blendEqSupported) {
        pgl.blendEquationSeparate(PGL.FUNC_ADD,
                                  PGL.FUNC_ADD);
      }
      pgl.blendFuncSeparate(PGL.ZERO, PGL.SRC_COLOR,
                            PGL.ONE,  PGL.ONE);

    } else if (blendMode == SCREEN) {
      if (blendEqSupported) {
        pgl.blendEquationSeparate(PGL.FUNC_ADD,
                                  PGL.FUNC_ADD);
      }
      pgl.blendFuncSeparate(PGL.ONE_MINUS_DST_COLOR, PGL.ONE,
                            PGL.ONE,                 PGL.ONE);

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
    return getTexture(true);
  }


  /**
   * Not an approved function either, don't use it.
   */
  public Texture getTexture(boolean load) {
    if (load) loadTexture();
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

    if (img.isModified()) {
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


  /**
   * Not an approved function, test its use in libraries to grab the FB objects
   * for offscreen PGraphics.
   */
  public FrameBuffer getFrameBuffer() {
    return getFrameBuffer(false);
  }


  public FrameBuffer getFrameBuffer(boolean multi) {
    if (multi) {
      return multisampleFramebuffer;
    } else {
      return offscreenFramebuffer;
    }
  }


  protected Object initCache(PImage img) {
    if (!checkGLThread()) {
      return null;
    }

    Texture tex = (Texture)getCache(img);
    if (tex == null || tex.contextIsOutdated()) {
      tex = addTexture(img);
      if (tex != null) {
        img.loadPixels();
        tex.set(img.pixels, img.format);
        img.setModified();
      }
    }
    return tex;
  }


  protected void bindFrontTexture() {
    if (primaryGraphics) {
      pgl.bindFrontTexture();
    } else {
      if (ptexture == null) {
        createPTexture();
      }
      ptexture.bind();
    }
  }


  protected void unbindFrontTexture() {
    if (primaryGraphics) {
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
    Texture tex = new Texture(this, img.pixelWidth, img.pixelHeight, params);
    setCache(img, tex);
    return tex;
  }


  protected void checkTexture(Texture tex) {
    if (!tex.colorBuffer() &&
        (tex.usingMipmaps == hints[DISABLE_TEXTURE_MIPMAPS] ||
         tex.currentSampling() != textureSampling)) {
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
    setCache(img, tex);
    return img;
  }


  protected void updateTexture(PImage img, Texture tex) {
    if (tex != null) {
      if (img.isModified()) {
        int x = img.getModifiedX1();
        int y = img.getModifiedY1();
        int w = img.getModifiedX2() - x;
        int h = img.getModifiedY2() - y;
        tex.set(img.pixels, x, y, w, h, img.format);
      }
    }
    img.setModified(false);
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
    pgl.initSurface(smooth);
    if (texture != null) {
      removeCache(this);
      texture = null;
      ptexture = null;
    }
    initialized = true;
  }


  protected void beginOnscreenDraw() {
    updatePixelSize();

    pgl.beginRender();

    if (drawFramebuffer == null) {
      drawFramebuffer = new FrameBuffer(this, pixelWidth, pixelHeight, true);
    }
    drawFramebuffer.setFBO(pgl.getDrawFramebuffer());
    if (readFramebuffer == null) {
      readFramebuffer = new FrameBuffer(this, pixelWidth, pixelHeight, true);
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
    pgl.endRender(parent.sketchWindowColor());
  }


  protected void initOffscreen() {
    // Getting the context and capabilities from the main renderer.
    loadTextureImpl(textureSampling, false);

    FrameBuffer ofb = offscreenFramebuffer;
    FrameBuffer mfb = multisampleFramebuffer;

    // In case of re-initialization (for example, when the smooth level
    // is changed), we make sure that all the OpenGL resources associated
    // to the surface are released by calling delete().
    if (ofb != null) {
      ofb.dispose();
      ofb = null;
    }
    if (mfb != null) {
      mfb.dispose();
      mfb = null;
    }

    boolean packed = depthBits == 24 && stencilBits == 8 &&
                     packedDepthStencilSupported;
    if (PGraphicsOpenGL.fboMultisampleSupported && 1 < PGL.smoothToSamples(smooth)) {
      mfb = new FrameBuffer(this, texture.glWidth, texture.glHeight, PGL.smoothToSamples(smooth), 0,
                            depthBits, stencilBits, packed, false);
      mfb.clear();
      multisampleFramebuffer = mfb;
      offscreenMultisample = true;

      // The offscreen framebuffer where the multisampled image is finally drawn
      // to. If depth reading is disabled it doesn't need depth and stencil buffers
      // since they are part of the multisampled framebuffer.
      if (hints[ENABLE_BUFFER_READING]) {
        ofb = new FrameBuffer(this, texture.glWidth, texture.glHeight, 1, 1,
                              depthBits, stencilBits, packed, false);
      } else {
        ofb = new FrameBuffer(this, texture.glWidth, texture.glHeight, 1, 1,
                          0, 0, false, false);
      }
    } else {
      smooth = 0;
      ofb = new FrameBuffer(this, texture.glWidth, texture.glHeight, 1, 1,
                            depthBits, stencilBits, packed, false);
      offscreenMultisample = false;
    }
    ofb.setColorBuffer(texture);
    ofb.clear();
    offscreenFramebuffer = ofb;

    initialized = true;
  }


  protected void beginOffscreenDraw() {
    if (!initialized) {
      initOffscreen();
    } else {
      FrameBuffer ofb = offscreenFramebuffer;
      FrameBuffer mfb = multisampleFramebuffer;
      boolean outdated = ofb != null && ofb.contextIsOutdated();
      boolean outdatedMulti = mfb != null && mfb.contextIsOutdated();
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
      FrameBuffer mfb = multisampleFramebuffer;
      if (mfb != null) {
        setFramebuffer(mfb);
      }
    } else {
      FrameBuffer ofb = offscreenFramebuffer;
      if (ofb != null) {
        setFramebuffer(ofb);
      }
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
      FrameBuffer ofb = offscreenFramebuffer;
      FrameBuffer mfb = multisampleFramebuffer;
      if (ofb != null && mfb != null) {
        mfb.copyColor(ofb);
      }
    }

    popFramebuffer();

    if (backgroundA == 1) {
      // Set alpha channel to opaque in order to match behavior of JAVA2D, not
      // on the multisampled FBO because it leads to wrong background color
      // on some Macbooks with AMD graphics.
      pgl.colorMask(false, false, false, true);
      pgl.clearColor(0, 0, 0, backgroundA);
      pgl.clear(PGL.COLOR_BUFFER_BIT);
      pgl.colorMask(true, true, true, true);
    }

    if (texture != null) {
      texture.updateTexels(); // Mark all texels in screen texture as modified.
    }

    getPrimaryPG().restoreGL();
  }


  protected void setViewport() {
    viewport.put(0, 0); viewport.put(1, 0);
    viewport.put(2, width); viewport.put(3, height);
    pgl.viewport(viewport.get(0), viewport.get(1),
                 viewport.get(2), viewport.get(3));
  }


  @Override
  protected void checkSettings() {
    super.checkSettings();
    setGLSettings();
  }


  protected void setGLSettings() {
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

    if (primaryGraphics) {
//      pgl.getIntegerv(PGL.SAMPLES, intBuffer);
//      int temp = intBuffer.get(0);
//      if (smooth != temp && 1 < temp && 1 < smooth) {
        // TODO check why the samples is higher that initialized smooth level.
//        quality = temp;
//      }
    }
    if (smooth < 1) {
      pgl.disable(PGL.MULTISAMPLE);
    } else {
      // work around runtime exceptions in Broadcom's VC IV driver
      if (false == OPENGL_RENDERER.equals("VideoCore IV HW")) {
        pgl.enable(PGL.MULTISAMPLE);
      }
    }
    // work around runtime exceptions in Broadcom's VC IV driver
    if (false == OPENGL_RENDERER.equals("VideoCore IV HW")) {
      pgl.disable(PGL.POLYGON_SMOOTH);
    }

    if (sized) {
//      reapplySettings();

      // To avoid having garbage in the screen after a resize,
      // in the case background is not called in draw().
      if (primaryGraphics) {
        background(backgroundColor);
      } else {
        // offscreen surfaces are transparent by default.
        background(0x00 << 24 | (backgroundColor & 0xFFFFFF));
      }

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

    // Vertices should be specified by user in CW order (left-handed)
    // That is CCW order (right-handed). Vertex shader inverts
    // Y-axis and outputs vertices in CW order (right-handed).
    // Culling occurs after the vertex shader, so FRONT FACE
    // has to be set to CW (right-handed) for OpenGL to correctly
    // recognize FRONT and BACK faces.
    pgl.frontFace(PGL.CW);
    pgl.disable(PGL.CULL_FACE);

    // Processing uses only one texture unit.
    pgl.activeTexture(PGL.TEXTURE0);

    // The current normal vector is set to be parallel to the Z axis.
    normalX = normalY = 0;
    normalZ = 1;

    // Clear depth and stencil buffers.
    pgl.depthMask(true);
    pgl.clearDepth(1);
    pgl.clearStencil(0);
    pgl.clear(PGL.DEPTH_BUFFER_BIT | PGL.STENCIL_BUFFER_BIT);

    if (hints[DISABLE_DEPTH_MASK]) {
      pgl.depthMask(false);
    } else {
      pgl.depthMask(true);
    }

    pixelsOp = OP_NONE;

    modified = false;
    loaded = false;
  }


  protected void getGLParameters() {
    OPENGL_VENDOR     = pgl.getString(PGL.VENDOR);
    OPENGL_RENDERER   = pgl.getString(PGL.RENDERER);
    OPENGL_VERSION    = pgl.getString(PGL.VERSION);
    OPENGL_EXTENSIONS = pgl.getString(PGL.EXTENSIONS);
    GLSL_VERSION      = pgl.getString(PGL.SHADING_LANGUAGE_VERSION);

    npotTexSupported = pgl.hasNpotTexSupport();
    autoMipmapGenSupported = pgl.hasAutoMipmapGenSupport();
    fboMultisampleSupported = pgl.hasFboMultisampleSupport();
    packedDepthStencilSupported = pgl.hasPackedDepthStencilSupport();
    anisoSamplingSupported = pgl.hasAnisoSamplingSupport();
    readBufferSupported = pgl.hasReadBuffer();
    drawBufferSupported = pgl.hasDrawBuffer();

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

    // work around runtime exceptions in Broadcom's VC IV driver
    if (false == OPENGL_RENDERER.equals("VideoCore IV HW")) {
      pgl.getIntegerv(PGL.MAX_SAMPLES, intBuffer);
      maxSamples = intBuffer.get(0);
    }

    if (anisoSamplingSupported) {
      pgl.getFloatv(PGL.MAX_TEXTURE_MAX_ANISOTROPY, floatBuffer);
      maxAnisoAmount = floatBuffer.get(0);
    }

    // overwrite the default shaders with vendor specific versions
    // if needed
    if (OPENGL_RENDERER.equals("VideoCore IV HW") ||    // Broadcom's binary driver for Raspberry Pi
      OPENGL_RENDERER.equals("Gallium 0.4 on VC4")) {   // Mesa driver for same hardware
        defLightShaderVertURL =
          PGraphicsOpenGL.class.getResource("/processing/opengl/shaders/LightVert-vc4.glsl");
        defTexlightShaderVertURL =
          PGraphicsOpenGL.class.getResource("/processing/opengl/shaders/TexLightVert-vc4.glsl");
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

    int type = PShader.getShaderType(parent.loadStrings(fragFilename),
                                     PShader.POLY);
    PShader shader = new PShader(parent);
    shader.setType(type);
    shader.setFragmentShader(fragFilename);
    if (type == PShader.POINT) {
      String[] vertSource = pgl.loadVertexShader(defPointShaderVertURL);
      shader.setVertexShader(vertSource);
    } else if (type == PShader.LINE) {
      String[] vertSource = pgl.loadVertexShader(defLineShaderVertURL);
      shader.setVertexShader(vertSource);
    } else if (type == PShader.TEXLIGHT) {
      String[] vertSource = pgl.loadVertexShader(defTexlightShaderVertURL);
      shader.setVertexShader(vertSource);
    } else if (type == PShader.LIGHT) {
      String[] vertSource = pgl.loadVertexShader(defLightShaderVertURL);
      shader.setVertexShader(vertSource);
    } else if (type == PShader.TEXTURE) {
      String[] vertSource = pgl.loadVertexShader(defTextureShaderVertURL);
      shader.setVertexShader(vertSource);
    } else if (type == PShader.COLOR) {
      String[] vertSource = pgl.loadVertexShader(defColorShaderVertURL);
      shader.setVertexShader(vertSource);
    } else {
      String[] vertSource = pgl.loadVertexShader(defTextureShaderVertURL);
      shader.setVertexShader(vertSource);
    }
    return shader;
  }


  @Override
  public PShader loadShader(String fragFilename, String vertFilename) {
    PShader shader = null;
    if (fragFilename == null || fragFilename.equals("")) {
      PGraphics.showWarning(MISSING_FRAGMENT_SHADER);
    } else if (vertFilename == null || vertFilename.equals("")) {
      PGraphics.showWarning(MISSING_VERTEX_SHADER);
    } else {
      shader = new PShader(parent, vertFilename, fragFilename);
    }
    return shader;
  }


  @Override
  public void shader(PShader shader) {
    flush(); // Flushing geometry drawn with a different shader.

    if (shader != null) shader.init();
    if (shader.isPolyShader()) polyShader = shader;
    else if (shader.isLineShader()) lineShader = shader;
    else if (shader.isPointShader()) pointShader = shader;
    else PGraphics.showWarning(UNKNOWN_SHADER_KIND_ERROR);
  }


  @Override
  public void shader(PShader shader, int kind) {
    flush(); // Flushing geometry drawn with a different shader.

    if (shader != null) shader.init();
    if (kind == TRIANGLES) polyShader = shader;
    else if (kind == LINES) lineShader = shader;
    else if (kind == POINTS) pointShader = shader;
    else PGraphics.showWarning(UNKNOWN_SHADER_KIND_ERROR);
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


  protected PShader getPolyShader(boolean lit, boolean tex) {
    PShader shader;
    PGraphicsOpenGL ppg = getPrimaryPG();
    boolean useDefault = polyShader == null;
    if (polyShader != null) {
      polyShader.setRenderer(this);
      polyShader.loadAttributes();
      polyShader.loadUniforms();
    }
    if (lit) {
      if (tex) {
        if (useDefault || !polyShader.checkPolyType(PShader.TEXLIGHT)) {
          if (ppg.defTexlightShader == null) {
            String[] vertSource = pgl.loadVertexShader(defTexlightShaderVertURL);
            String[] fragSource = pgl.loadFragmentShader(defTexlightShaderFragURL);
            ppg.defTexlightShader = new PShader(parent, vertSource, fragSource);
          }
          shader = ppg.defTexlightShader;
        } else {
          shader = polyShader;
        }
      } else {
        if (useDefault || !polyShader.checkPolyType(PShader.LIGHT)) {
          if (ppg.defLightShader == null) {
            String[] vertSource = pgl.loadVertexShader(defLightShaderVertURL);
            String[] fragSource = pgl.loadFragmentShader(defLightShaderFragURL);
            ppg.defLightShader = new PShader(parent, vertSource, fragSource);
          }
          shader = ppg.defLightShader;
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
          if (ppg.defTextureShader == null) {
            String[] vertSource = pgl.loadVertexShader(defTextureShaderVertURL);
            String[] fragSource = pgl.loadFragmentShader(defTextureShaderFragURL);
            ppg.defTextureShader = new PShader(parent, vertSource, fragSource);
          }
          shader = ppg.defTextureShader;
        } else {
          shader = polyShader;
        }
      } else {
        if (useDefault || !polyShader.checkPolyType(PShader.COLOR)) {
          if (ppg.defColorShader == null) {
            String[] vertSource = pgl.loadVertexShader(defColorShaderVertURL);
            String[] fragSource = pgl.loadFragmentShader(defColorShaderFragURL);
            ppg.defColorShader = new PShader(parent, vertSource, fragSource);
          }
          shader = ppg.defColorShader;
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
    PGraphicsOpenGL ppg = getPrimaryPG();
    if (lineShader == null) {
      if (ppg.defLineShader == null) {
        String[] vertSource = pgl.loadVertexShader(defLineShaderVertURL);
        String[] fragSource = pgl.loadFragmentShader(defLineShaderFragURL);
        ppg.defLineShader = new PShader(parent, vertSource, fragSource);
      }
      shader = ppg.defLineShader;
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
    PGraphicsOpenGL ppg = getPrimaryPG();
    if (pointShader == null) {
      if (ppg.defPointShader == null) {
        String[] vertSource = pgl.loadVertexShader(defPointShaderVertURL);
        String[] fragSource = pgl.loadFragmentShader(defPointShaderFragURL);
        ppg.defPointShader = new PShader(parent, vertSource, fragSource);
      }
      shader = ppg.defPointShader;
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

  // Generic vertex attributes.


  static protected AttributeMap newAttributeMap() {
    return new AttributeMap();
  }


  static protected class AttributeMap extends HashMap<String, VertexAttribute> {
    public ArrayList<String> names = new ArrayList<String>();
    public int numComp = 0; // number of components for a single vertex

    @Override
    public VertexAttribute put(String key, VertexAttribute value) {
      VertexAttribute prev = super.put(key, value);
      names.add(key);
      if (value.kind == VertexAttribute.COLOR) numComp += 4;
      else numComp += value.size;
      return prev;
    }

    public VertexAttribute get(int i) {
      return super.get(names.get(i));
    }
  }


  static protected class VertexAttribute {
    static final int POSITION = 0;
    static final int NORMAL   = 1;
    static final int COLOR    = 2;
    static final int OTHER    = 3;

    PGraphicsOpenGL pg;
    String name;
    int kind; // POSITION, NORMAL, COLOR, OTHER
    int type; // GL_INT, GL_FLOAT, GL_BOOL
    int size; // number of elements (1, 2, 3, or 4)
    int tessSize;
    int elementSize;
    VertexBuffer buf;
    int glLoc;

    float[] fvalues;
    int[] ivalues;
    byte[] bvalues;

    // For use in PShape
    boolean modified;
    int firstModified;
    int lastModified;
    boolean active;

    VertexAttribute(PGraphicsOpenGL pg, String name, int kind, int type, int size) {
      this.pg = pg;
      this.name = name;
      this.kind = kind;
      this.type = type;
      this.size = size;

      if (kind == POSITION) {
        tessSize = 4; // for w
      } else {
        tessSize = size;
      }

      if (type == PGL.FLOAT) {
        elementSize = PGL.SIZEOF_FLOAT;
        fvalues = new float[size];
      } else if (type == PGL.INT) {
        elementSize = PGL.SIZEOF_INT;
        ivalues = new int[size];
      } else if (type == PGL.BOOL) {
        elementSize = PGL.SIZEOF_INT;
        bvalues = new byte[size];
      }

      buf = null;
      glLoc = -1;

      modified = false;
      firstModified = PConstants.MAX_INT;
      lastModified = PConstants.MIN_INT;

      active = true;
    }

    public boolean diff(VertexAttribute attr) {
       return !name.equals(attr.name) ||
              kind != attr.kind ||
              type != attr.type ||
              size != attr.size ||
              tessSize != attr.tessSize ||
              elementSize != attr.elementSize;
    }

    boolean isPosition() {
      return kind == POSITION;
    }

    boolean isNormal() {
      return kind == NORMAL;
    }

    boolean isColor() {
      return kind == COLOR;
    }

    boolean isOther() {
      return kind == OTHER;
    }

    boolean isFloat() {
      return type == PGL.FLOAT;
    }

    boolean isInt() {
      return type == PGL.INT;
    }

    boolean isBool() {
      return type == PGL.BOOL;
    }

    boolean bufferCreated() {
      return buf != null && 0 < buf.glId;
    }

    void createBuffer(PGL pgl) {
      buf = new VertexBuffer(pg, PGL.ARRAY_BUFFER, size, elementSize, false);
    }

    void deleteBuffer(PGL pgl) {
      if (buf.glId != 0) {
        intBuffer.put(0, buf.glId);
        if (pgl.threadIsCurrent()) pgl.deleteBuffers(1, intBuffer);
      }
    }

    void bind(PGL pgl) {
      pgl.enableVertexAttribArray(glLoc);
    }

    void unbind(PGL pgl) {
      pgl.disableVertexAttribArray(glLoc);
    }

    boolean active(PShader shader) {
      if (active) {
        if (glLoc == -1) {
          glLoc = shader.getAttributeLoc(name);
          if (glLoc == -1) active = false;
        }
      }
      return active;
    }

    int sizeInBytes(int length) {
      return length * tessSize * elementSize;
    }

    void set(float x, float y, float z) {
      fvalues[0] = x;
      fvalues[1] = y;
      fvalues[2] = z;
    }

    void set(int c) {
      ivalues[0] = c;
    }

    void set(float[] values) {
      PApplet.arrayCopy(values, 0, fvalues, 0, size);
    }

    void set(int[] values) {
      PApplet.arrayCopy(values, 0, ivalues, 0, size);
    }

    void set(boolean[] values) {
      for (int i = 0; i < values.length; i++) {
        bvalues[i] = (byte)(values[i] ? 1 : 0);
      }
    }

    void add(float[] dstValues, int dstIdx) {
      PApplet.arrayCopy(fvalues, 0, dstValues, dstIdx, size);
    }

    void add(int[] dstValues, int dstIdx) {
      PApplet.arrayCopy(ivalues, 0, dstValues, dstIdx, size);
    }

    void add(byte[] dstValues, int dstIdx) {
      PApplet.arrayCopy(bvalues, 0, dstValues, dstIdx, size);
    }
  }


  //////////////////////////////////////////////////////////////

  // Input (raw) and Tessellated geometry, tessellator.


  static protected InGeometry newInGeometry(PGraphicsOpenGL pg, AttributeMap attr,
                                            int mode) {
    return new InGeometry(pg, attr, mode);
  }


  static protected TessGeometry newTessGeometry(PGraphicsOpenGL pg,
                                                AttributeMap attr, int mode) {
    return new TessGeometry(pg, attr, mode);
  }


  static protected TexCache newTexCache(PGraphicsOpenGL pg) {
    return new TexCache(pg);
  }


  // Holds an array of textures and the range of vertex
  // indices each texture applies to.
  static protected class TexCache {
    PGraphicsOpenGL pg;
    int size;
    PImage[] textures;
    int[] firstIndex;
    int[] lastIndex;
    int[] firstCache;
    int[] lastCache;
    boolean hasTextures;

    TexCache(PGraphicsOpenGL pg) {
      this.pg = pg;
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
        tex = pg.getTexture(img);
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
    int[] counter;

    IndexCache() {
      allocate();
    }

    void allocate() {
      size = 0;
      indexCount = new int[2];
      indexOffset = new int[2];
      vertexCount = new int[2];
      vertexOffset = new int[2];
      counter = null;
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

    void setCounter(int[] counter) {
      this.counter = counter;
    }

    void incCounts(int index, int icount, int vcount) {
      indexCount[index] += icount;
      vertexCount[index] += vcount;
      if (counter != null) {
        counter[0] += icount;
        counter[1] += vcount;
      }
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
    AttributeMap attribs;

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

    // Generic attributes
    HashMap<String, float[]> fattribs;
    HashMap<String, int[]> iattribs;
    HashMap<String, byte[]> battribs;

    // Internally used by the addVertex() methods.
    int fillColor;
    int strokeColor;
    float strokeWeight;
    int ambientColor;
    int specularColor;
    int emissiveColor;
    float shininessFactor;
    float normalX, normalY, normalZ;

    InGeometry(PGraphicsOpenGL pg, AttributeMap attr, int mode) {
      this.pg = pg;
      this.attribs = attr;
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

      fattribs = new HashMap<String, float[]>();
      iattribs = new HashMap<String, int[]>();
      battribs = new HashMap<String, byte[]>();

      clear();
    }

    void initAttrib(VertexAttribute attrib) {
      if (attrib.type == PGL.FLOAT) {
        float[] temp = new float[attrib.size * PGL.DEFAULT_IN_VERTICES];
        fattribs.put(attrib.name, temp);
      } else if (attrib.type == PGL.INT) {
        int[] temp = new int[attrib.size * PGL.DEFAULT_IN_VERTICES];
        iattribs.put(attrib.name, temp);
      } else if (attrib.type == PGL.BOOL) {
        byte[] temp = new byte[attrib.size * PGL.DEFAULT_IN_VERTICES];
        battribs.put(attrib.name, temp);
      }
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
        expandAttribs(newSize);
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
          if (edge[2] == EDGE_MIDDLE || edge[2] == EDGE_START) bevVert += 3;
          if (edge[2] == EDGE_CLOSE) {
            bevVert += 5;
            segVert--;
          }
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
          if (edge[2] == EDGE_CLOSE) {
            bevInd++;
            segInd--;
          }
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

    double[] getAttribVector(int idx) {
      double[] vector = new double[attribs.numComp];
      int vidx = 0;
      for (int i = 0; i < attribs.size(); i++) {
        VertexAttribute attrib = attribs.get(i);
        String name = attrib.name;
        int aidx = attrib.size * idx;
        if (attrib.isColor()) {
          int[] iarray = iattribs.get(name);
          int col = iarray[aidx];
          vector[vidx++] = (col >> 24) & 0xFF;
          vector[vidx++] = (col >> 16) & 0xFF;
          vector[vidx++] = (col >>  8) & 0xFF;
          vector[vidx++] = (col >>  0) & 0xFF;
        } else {
          if (attrib.isFloat()) {
            float[] farray = fattribs.get(name);
            for (int n = 0; n < attrib.size; n++) {
              vector[vidx++] = farray[aidx++];
            }
          } else if (attrib.isInt()) {
            int[] iarray = iattribs.get(name);
            for (int n = 0; n < attrib.size; n++) {
              vector[vidx++] = iarray[aidx++];
            }
          } else if (attrib.isBool()) {
            byte[] barray = battribs.get(name);
            for (int n = 0; n < attrib.size; n++) {
              vector[vidx++] = barray[aidx++];
            }
          }
        }
      }
      return vector;
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

    void expandAttribs(int n) {
      for (String name: attribs.keySet()) {
        VertexAttribute attrib = attribs.get(name);
        if (attrib.type == PGL.FLOAT) {
          expandFloatAttrib(attrib, n);
        } else if (attrib.type == PGL.INT) {
          expandIntAttrib(attrib, n);
        } else if (attrib.type == PGL.BOOL) {
          expandBoolAttrib(attrib, n);
        }
      }
    }

    void expandFloatAttrib(VertexAttribute attrib, int n) {
      float[] values = fattribs.get(attrib.name);
      float temp[] = new float[attrib.size * n];
      PApplet.arrayCopy(values, 0, temp, 0, attrib.size * vertexCount);
      fattribs.put(attrib.name, temp);
    }

    void expandIntAttrib(VertexAttribute attrib, int n) {
      int[] values = iattribs.get(attrib.name);
      int temp[] = new int[attrib.size * n];
      PApplet.arrayCopy(values, 0, temp, 0, attrib.size * vertexCount);
      iattribs.put(attrib.name, temp);
    }

    void expandBoolAttrib(VertexAttribute attrib, int n) {
      byte[] values = battribs.get(attrib.name);
      byte temp[] = new byte[attrib.size * n];
      PApplet.arrayCopy(values, 0, temp, 0, attrib.size * vertexCount);
      battribs.put(attrib.name, temp);
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
        trimAttribs();
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

    void trimAttribs() {
      for (String name: attribs.keySet()) {
        VertexAttribute attrib = attribs.get(name);
        if (attrib.type == PGL.FLOAT) {
          trimFloatAttrib(attrib);
        } else if (attrib.type == PGL.INT) {
          trimIntAttrib(attrib);
        } else if (attrib.type == PGL.BOOL) {
          trimBoolAttrib(attrib);
        }
      }
    }

    void trimFloatAttrib(VertexAttribute attrib) {
      float[] values = fattribs.get(attrib.name);
      float temp[] = new float[attrib.size * vertexCount];
      PApplet.arrayCopy(values, 0, temp, 0, attrib.size * vertexCount);
      fattribs.put(attrib.name, temp);
    }

    void trimIntAttrib(VertexAttribute attrib) {
      int[] values = iattribs.get(attrib.name);
      int temp[] = new int[attrib.size * vertexCount];
      PApplet.arrayCopy(values, 0, temp, 0, attrib.size * vertexCount);
      iattribs.put(attrib.name, temp);
    }

    void trimBoolAttrib(VertexAttribute attrib) {
      byte[] values = battribs.get(attrib.name);
      byte temp[] = new byte[attrib.size * vertexCount];
      PApplet.arrayCopy(values, 0, temp, 0, attrib.size * vertexCount);
      battribs.put(attrib.name, temp);
    }

    // -----------------------------------------------------------------
    //
    // Vertices

    int addVertex(float x, float y, boolean brk) {
      return addVertex(x, y, 0,
                       fillColor,
                       normalX, normalY, normalZ,
                       0, 0,
                       strokeColor, strokeWeight,
                       ambientColor, specularColor, emissiveColor, shininessFactor,
                       VERTEX, brk);
    }

    int addVertex(float x, float y,
                  int code, boolean brk) {
      return addVertex(x, y, 0,
                       fillColor,
                       normalX, normalY, normalZ,
                       0, 0,
                       strokeColor, strokeWeight,
                       ambientColor, specularColor, emissiveColor, shininessFactor,
                       code, brk);
    }

    int addVertex(float x, float y,
                  float u, float v,
                  boolean brk) {
      return addVertex(x, y, 0,
                       fillColor,
                       normalX, normalY, normalZ,
                       u, v,
                       strokeColor, strokeWeight,
                       ambientColor, specularColor, emissiveColor, shininessFactor,
                       VERTEX, brk);
    }

    int addVertex(float x, float y,
                  float u, float v,
                  int code, boolean brk) {
      return addVertex(x, y, 0,
                       fillColor,
                       normalX, normalY, normalZ,
                       u, v,
                       strokeColor, strokeWeight,
                       ambientColor, specularColor, emissiveColor, shininessFactor,
                       code, brk);
    }

    int addVertex(float x, float y, float z, boolean brk) {
      return addVertex(x, y, z,
                       fillColor,
                       normalX, normalY, normalZ,
                       0, 0,
                       strokeColor, strokeWeight,
                       ambientColor, specularColor, emissiveColor, shininessFactor,
                       VERTEX, brk);
    }

    int addVertex(float x, float y, float z, int code, boolean brk) {
      return addVertex(x, y, z,
                       fillColor,
                       normalX, normalY, normalZ,
                       0, 0,
                       strokeColor, strokeWeight,
                       ambientColor, specularColor, emissiveColor, shininessFactor,
                       code, brk);
    }

    int addVertex(float x, float y, float z,
                  float u, float v,
                  boolean brk) {
      return addVertex(x, y, z,
                       fillColor,
                       normalX, normalY, normalZ,
                       u, v,
                       strokeColor, strokeWeight,
                       ambientColor, specularColor, emissiveColor, shininessFactor,
                       VERTEX, brk);
    }

    int addVertex(float x, float y, float z,
                  float u, float v,
                  int code, boolean brk) {
      return addVertex(x, y, z,
                       fillColor,
                       normalX, normalY, normalZ,
                       u, v,
                       strokeColor, strokeWeight,
                       ambientColor, specularColor, emissiveColor, shininessFactor,
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

      for (String name: attribs.keySet()) {
        VertexAttribute attrib = attribs.get(name);
        index = attrib.size * vertexCount;
        if (attrib.type == PGL.FLOAT) {
          float[] values = fattribs.get(name);
          attrib.add(values, index);
        } else if (attrib.type == PGL.INT) {
          int[] values = iattribs.get(name);
          attrib.add(values, index);
        } else if (attrib.type == PGL.BOOL) {
          byte[] values = battribs.get(name);
          attrib.add(values, index);
        }
      }

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
                                float x4, float y4, float z4,
                                boolean brk) {
      addVertex(x2, y2, z2, BEZIER_VERTEX, brk);
      addVertex(x3, y3, z3, -1, false);
      addVertex(x4, y4, z4, -1, false);
    }

    public void addQuadraticVertex(float cx, float cy, float cz,
                                   float x3, float y3, float z3,
                                   boolean brk) {
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

    // Expects vertices in CW (left-handed) order.
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
      // that vertices as given in CCW order (right-handed) so:
      // n = v12 x v10
      // so that the normal extends from the front face.
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
        // Vertices are specified by user as:
        // 1-3 ...
        // |\|\ ...
        // 0-2-4 ...
        if (i % 2 == 1) {
          // The odd triangles (1, 3, 5...) should be CW (left-handed)
          i0 = i - 1;
          i2 = i + 1;
        } else {
          // The even triangles (2, 4, 6...) should be CCW (left-handed)
          i0 = i + 1;
          i2 = i - 1;
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

        // Vertices are specified by user as:
        // 1-3-5 ...
        // |\|\| ...
        // 0-2-4 ...
        // thus (0, 1, 2) and (2, 1, 3) are triangles
        // in CW order (left-handed).
        calcTriangleNormal(i0, i1, i2);
        calcTriangleNormal(i2, i1, i3);
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

      // get length before wrapping indexes so (startLUT <= stopLUT);
      int length = PApplet.constrain(stopLUT - startLUT, 0, SINCOS_LENGTH);

      boolean fullCircle = length == SINCOS_LENGTH;

      if (fullCircle && arcMode == CHORD) {
        // get rid of overlapping vertices,
        // solves problem with closing edge in P3D
        length -= 1;
        stopLUT -= 1;
      }

      { // wrap indexes so they are safe to use in LUT
        startLUT %= SINCOS_LENGTH;
        if (startLUT < 0) startLUT += SINCOS_LENGTH;

        stopLUT %= SINCOS_LENGTH;
        if (stopLUT < 0) stopLUT += SINCOS_LENGTH;
      }

      int idx0;
      if (arcMode == CHORD || arcMode == OPEN) {
        // move center to the middle of flat side
        // to properly display arcs smaller than PI
        float relX = (cosLUT[startLUT] + cosLUT[stopLUT]) * 0.5f * hr;
        float relY = (sinLUT[startLUT] + sinLUT[stopLUT]) * 0.5f * vr;
        idx0 = addVertex(centerX + relX, centerY + relY, VERTEX, true);
      } else {
        idx0 = addVertex(centerX, centerY, VERTEX, true);
      }

      int inc;
      { // initializes inc the same way ellipse does
        float sx1 = pg.screenX(x, y);
        float sy1 = pg.screenY(x, y);
        float sx2 = pg.screenX(x + w, y + h);
        float sy2 = pg.screenY(x + w, y + h);

        int accuracy =
          PApplet.min(MAX_POINT_ACCURACY, PApplet.max(MIN_POINT_ACCURACY,
                      (int) (TWO_PI * PApplet.dist(sx1, sy1, sx2, sy2) /
                      POINT_ACCURACY_FACTOR)));
        inc = PApplet.max(1, SINCOS_LENGTH / accuracy);
      }

      int idx = idx0;
      int pidx;

      int i = -inc;
      int ii;

      // i:  (0 -> length) inclusive
      // ii: (startLUT -> stopLUT) inclusive, going CW (left-handed),
      //     wrapping around end of LUT
      do {
        i += inc;
        i = PApplet.min(i, length); // clamp so last vertex won't go over

        ii = startLUT + i; // ii from 0 to (2 * SINCOS_LENGTH - 1)
        if (ii >= SINCOS_LENGTH) ii -= SINCOS_LENGTH;

        pidx = idx;
        idx = addVertex(centerX + cosLUT[ii] * hr,
                        centerY + sinLUT[ii] * vr,
                        VERTEX, i == 0 && !fill);

        if (stroke) {
          if (arcMode == CHORD || arcMode == PIE) {
            addEdge(pidx, idx, i == 0, false);
          } else if (0 < i) {
            // when drawing full circle, the edge is closed later
            addEdge(pidx, idx, i == PApplet.min(inc, length),
                               i == length && !fullCircle);
          }
        }
      } while (i < length);

      // keeping last vertex as idx and second last vertex as pidx

      if (stroke) {
        if (arcMode == CHORD || arcMode == PIE) {
          addEdge(idx, idx0, false, false);
          closeEdge(idx, idx0);
        } else if (fullCircle) {
          closeEdge(pidx, idx);
        }
      }
    }

    void addBox(float w, float h, float d,
                boolean fill, boolean stroke) {

      // Correct normals if some dimensions are negative so they always
      // extend from front face. We could just take absolute value
      // of dimensions, but that would affect texturing.
      boolean invertNormX = (h > 0) != (d > 0);
      boolean invertNormY = (w > 0) != (d > 0);
      boolean invertNormZ = (w > 0) != (h > 0);

      int normX = invertNormX ? -1 : 1;
      int normY = invertNormY ? -1 : 1;
      int normZ = invertNormZ ? -1 : 1;

      float x1 = -w/2f; float x2 = w/2f;
      float y1 = -h/2f; float y2 = h/2f;
      float z1 = -d/2f; float z2 = d/2f;

      int idx1 = 0, idx2 = 0, idx3 = 0, idx4 = 0;
      if (fill || stroke) {
        // back face
        setNormal(0, 0, -normZ);
        idx1 = addVertex(x1, y1, z1, 0, 0, VERTEX, true);
        idx2 = addVertex(x1, y2, z1, 0, 1, VERTEX, false);
        idx3 = addVertex(x2, y2, z1, 1, 1, VERTEX, false);
        idx4 = addVertex(x2, y1, z1, 1, 0, VERTEX, false);
        if (stroke) {
          addEdge(idx1, idx2, true, false);
          addEdge(idx2, idx3, false, false);
          addEdge(idx3, idx4, false, false);
          addEdge(idx4, idx1, false, false);
          closeEdge(idx4, idx1);
        }

        // front face
        setNormal(0, 0, normZ);
        idx1 = addVertex(x1, y2, z2, 1, 1, VERTEX, false);
        idx2 = addVertex(x1, y1, z2, 1, 0, VERTEX, false);
        idx3 = addVertex(x2, y1, z2, 0, 0, VERTEX, false);
        idx4 = addVertex(x2, y2, z2, 0, 1, VERTEX, false);
        if (stroke) {
          addEdge(idx1, idx2, true, false);
          addEdge(idx2, idx3, false, false);
          addEdge(idx3, idx4, false, false);
          addEdge(idx4, idx1, false, false);
          closeEdge(idx4, idx1);
        }

        // right face
        setNormal(normX, 0, 0);
        idx1 = addVertex(x2, y1, z1, 0, 0, VERTEX, false);
        idx2 = addVertex(x2, y2, z1, 0, 1, VERTEX, false);
        idx3 = addVertex(x2, y2, z2, 1, 1, VERTEX, false);
        idx4 = addVertex(x2, y1, z2, 1, 0, VERTEX, false);
        if (stroke) {
          addEdge(idx1, idx2, true, false);
          addEdge(idx2, idx3, false, false);
          addEdge(idx3, idx4, false, false);
          addEdge(idx4, idx1, false, false);
          closeEdge(idx4, idx1);
        }

        // left face
        setNormal(-normX, 0, 0);
        idx1 = addVertex(x1, y2, z1, 1, 1, VERTEX, false);
        idx2 = addVertex(x1, y1, z1, 1, 0, VERTEX, false);
        idx3 = addVertex(x1, y1, z2, 0, 0, VERTEX, false);
        idx4 = addVertex(x1, y2, z2, 0, 1, VERTEX, false);
        if (stroke) {
          addEdge(idx1, idx2, true, false);
          addEdge(idx2, idx3, false, false);
          addEdge(idx3, idx4, false, false);
          addEdge(idx4, idx1, false, false);
          closeEdge(idx4, idx1);
        }

        // top face
        setNormal(0, -normY, 0);
        idx1 = addVertex(x2, y1, z1, 1, 1, VERTEX, false);
        idx2 = addVertex(x2, y1, z2, 1, 0, VERTEX, false);
        idx3 = addVertex(x1, y1, z2, 0, 0, VERTEX, false);
        idx4 = addVertex(x1, y1, z1, 0, 1, VERTEX, false);
        if (stroke) {
          addEdge(idx1, idx2, true, false);
          addEdge(idx2, idx3, false, false);
          addEdge(idx3, idx4, false, false);
          addEdge(idx4, idx1, false, false);
          closeEdge(idx4, idx1);
        }

        // bottom face
        setNormal(0, normY, 0);
        idx1 = addVertex(x1, y2, z1, 0, 0, VERTEX, false);
        idx2 = addVertex(x1, y2, z2, 0, 1, VERTEX, false);
        idx3 = addVertex(x2, y2, z2, 1, 1, VERTEX, false);
        idx4 = addVertex(x2, y2, z1, 1, 0, VERTEX, false);
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

        indices[indCount + 3 * i + 0] = i1;
        indices[indCount + 3 * i + 1] = i0;
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
    AttributeMap polyAttribs;

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

    // Generic attributes
    HashMap<String, Buffer> polyAttribBuffers = new HashMap<String, Buffer>();

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

    HashMap<String, float[]> fpolyAttribs = new HashMap<String, float[]>();
    HashMap<String, int[]> ipolyAttribs = new HashMap<String, int[]>();
    HashMap<String, byte[]> bpolyAttribs = new HashMap<String, byte[]>();

    TessGeometry(PGraphicsOpenGL pg, AttributeMap attr, int mode) {
      this.pg = pg;
      this.polyAttribs = attr;
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

    void initAttrib(VertexAttribute attrib) {
      if (attrib.type == PGL.FLOAT && !fpolyAttribs.containsKey(attrib.name)) {
        float[] temp = new float[attrib.tessSize * PGL.DEFAULT_TESS_VERTICES];
        fpolyAttribs.put(attrib.name, temp);
        polyAttribBuffers.put(attrib.name, PGL.allocateFloatBuffer(temp));
      } else if (attrib.type == PGL.INT && !ipolyAttribs.containsKey(attrib.name)) {
        int[] temp = new int[attrib.tessSize * PGL.DEFAULT_TESS_VERTICES];
        ipolyAttribs.put(attrib.name, temp);
        polyAttribBuffers.put(attrib.name, PGL.allocateIntBuffer(temp));
      } else if (attrib.type == PGL.BOOL && !bpolyAttribs.containsKey(attrib.name)) {
        byte[] temp = new byte[attrib.tessSize * PGL.DEFAULT_TESS_VERTICES];
        bpolyAttribs.put(attrib.name, temp);
        polyAttribBuffers.put(attrib.name, PGL.allocateByteBuffer(temp));
      }
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
        expandAttributes(newSize);
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
        expandAttributes(newSize);
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

    protected void updateAttribBuffer(String name) {
      updateAttribBuffer(name, 0, polyVertexCount);
    }

    protected void updateAttribBuffer(String name, int offset, int size) {
      VertexAttribute attrib = polyAttribs.get(name);
      if (attrib.type == PGL.FLOAT) {
        FloatBuffer buffer = (FloatBuffer)polyAttribBuffers.get(name);
        float[] array = fpolyAttribs.get(name);
        PGL.updateFloatBuffer(buffer, array,
                              attrib.tessSize * offset, attrib.tessSize * size);
      } else if (attrib.type == PGL.INT) {
        IntBuffer buffer = (IntBuffer)polyAttribBuffers.get(name);
        int[] array = ipolyAttribs.get(name);
        PGL.updateIntBuffer(buffer, array,
                            attrib.tessSize * offset, attrib.tessSize * size);
      } else if (attrib.type == PGL.BOOL) {
        ByteBuffer buffer = (ByteBuffer)polyAttribBuffers.get(name);
        byte[] array = bpolyAttribs.get(name);
        PGL.updateByteBuffer(buffer, array,
                             attrib.tessSize * offset, attrib.tessSize * size);
      }
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

    void expandAttributes(int n) {
      for (String name: polyAttribs.keySet()) {
        VertexAttribute attrib = polyAttribs.get(name);
        if (attrib.type == PGL.FLOAT) {
          expandFloatAttribute(attrib, n);
        } else if (attrib.type == PGL.INT) {
          expandIntAttribute(attrib, n);
        } else if (attrib.type == PGL.BOOL) {
          expandBoolAttribute(attrib, n);
        }
      }
    }

    void expandFloatAttribute(VertexAttribute attrib, int n) {
      float[] array = fpolyAttribs.get(attrib.name);
      float temp[] = new float[attrib.tessSize * n];
      PApplet.arrayCopy(array, 0, temp, 0, attrib.tessSize * polyVertexCount);
      fpolyAttribs.put(attrib.name, temp);
      polyAttribBuffers.put(attrib.name, PGL.allocateFloatBuffer(temp));
    }

    void expandIntAttribute(VertexAttribute attrib, int n) {
      int[] array = ipolyAttribs.get(attrib.name);
      int temp[] = new int[attrib.tessSize * n];
      PApplet.arrayCopy(array, 0, temp, 0, attrib.tessSize * polyVertexCount);
      ipolyAttribs.put(attrib.name, temp);
      polyAttribBuffers.put(attrib.name, PGL.allocateIntBuffer(temp));
    }

    void expandBoolAttribute(VertexAttribute attrib, int n) {
      byte[] array = bpolyAttribs.get(attrib.name);
      byte temp[] = new byte[attrib.tessSize * n];
      PApplet.arrayCopy(array, 0, temp, 0, attrib.tessSize * polyVertexCount);
      bpolyAttribs.put(attrib.name, temp);
      polyAttribBuffers.put(attrib.name, PGL.allocateByteBuffer(temp));
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
        trimPolyAttributes();
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

    void trimPolyAttributes() {
      for (String name: polyAttribs.keySet()) {
        VertexAttribute attrib = polyAttribs.get(name);
        if (attrib.type == PGL.FLOAT) {
          trimFloatAttribute(attrib);
        } else if (attrib.type == PGL.INT) {
          trimIntAttribute(attrib);
        } else if (attrib.type == PGL.BOOL) {
          trimBoolAttribute(attrib);
        }
      }
    }

    void trimFloatAttribute(VertexAttribute attrib) {
      float[] array = fpolyAttribs.get(attrib.name);
      float temp[] = new float[attrib.tessSize * polyVertexCount];
      PApplet.arrayCopy(array, 0, temp, 0, attrib.tessSize * polyVertexCount);
      fpolyAttribs.put(attrib.name, temp);
      polyAttribBuffers.put(attrib.name, PGL.allocateFloatBuffer(temp));
    }

    void trimIntAttribute(VertexAttribute attrib) {
      int[] array = ipolyAttribs.get(attrib.name);
      int temp[] = new int[attrib.tessSize * polyVertexCount];
      PApplet.arrayCopy(array, 0, temp, 0, attrib.tessSize * polyVertexCount);
      ipolyAttribs.put(attrib.name, temp);
      polyAttribBuffers.put(attrib.name, PGL.allocateIntBuffer(temp));
    }

    void trimBoolAttribute(VertexAttribute attrib) {
      byte[] array = bpolyAttribs.get(attrib.name);
      byte temp[] = new byte[attrib.tessSize * polyVertexCount];
      PApplet.arrayCopy(array, 0, temp, 0, attrib.tessSize * polyVertexCount);
      bpolyAttribs.put(attrib.name, temp);
      polyAttribBuffers.put(attrib.name, PGL.allocateByteBuffer(temp));
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

    // Expects vertices in CW (left-handed) order.
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

    void addPolyVertex(double[] d, boolean clampXY) {
      int fcolor =
        (int)d[ 3] << 24 | (int)d[ 4] << 16 | (int)d[ 5] << 8 | (int)d[ 6];
       int acolor =
        (int)d[12] << 24 | (int)d[13] << 16 | (int)d[14] << 8 | (int)d[15];
       int scolor =
        (int)d[16] << 24 | (int)d[17] << 16 | (int)d[18] << 8 | (int)d[19];
       int ecolor =
        (int)d[20] << 24 | (int)d[21] << 16 | (int)d[22] << 8 | (int)d[23];

       addPolyVertex((float)d[ 0], (float)d[ 1], (float)d[ 2],
                     fcolor,
                     (float)d[ 7], (float)d[ 8], (float)d[ 9],
                     (float)d[10], (float)d[11],
                     acolor, scolor, ecolor,
                     (float)d[24],
                     clampXY);

       if (25 < d.length) {
         // Add the values of the custom attributes...
         PMatrix3D mm = pg.modelview;
         PMatrix3D nm = pg.modelviewInv;
         int tessIdx = polyVertexCount - 1;
         int index;
         int pos = 25;
         for (int i = 0; i < polyAttribs.size(); i++) {
           VertexAttribute attrib = polyAttribs.get(i);
           String name = attrib.name;
           index = attrib.tessSize * tessIdx;
           if (attrib.isColor()) {
             // Reconstruct color from ARGB components
             int color =
               (int)d[pos + 0] << 24 | (int)d[pos + 1] << 16 | (int)d[pos + 2] << 8 | (int)d[pos + 3];
             int[] tessValues = ipolyAttribs.get(name);
             tessValues[index] = color;
             pos += 4;
           }  else if (attrib.isPosition()) {
             float[] farray = fpolyAttribs.get(name);
             float x = (float)d[pos++];
             float y = (float)d[pos++];
             float z = (float)d[pos++];
             if (renderMode == IMMEDIATE && pg.flushMode == FLUSH_WHEN_FULL) {
               if (clampXY) {
                 // ceil emulates the behavior of JAVA2D
                 farray[index++] =
                   PApplet.ceil(x*mm.m00 + y*mm.m01 + z*mm.m02 + mm.m03);
                 farray[index++] =
                   PApplet.ceil(x*mm.m10 + y*mm.m11 + z*mm.m12 + mm.m13);
               } else {
                 farray[index++] = x*mm.m00 + y*mm.m01 + z*mm.m02 + mm.m03;
                 farray[index++] = x*mm.m10 + y*mm.m11 + z*mm.m12 + mm.m13;
               }
               farray[index++] = x*mm.m20 + y*mm.m21 + z*mm.m22 + mm.m23;
               farray[index  ] = x*mm.m30 + y*mm.m31 + z*mm.m32 + mm.m33;
             } else {
               farray[index++] = x;
               farray[index++] = y;
               farray[index++] = z;
               farray[index  ] = 1;
             }
           } else if (attrib.isNormal()) {
             float[] farray = fpolyAttribs.get(name);
             float x = (float)d[pos + 0];
             float y = (float)d[pos + 1];
             float z = (float)d[pos + 2];
             if (renderMode == IMMEDIATE && pg.flushMode == FLUSH_WHEN_FULL) {
               farray[index++] = x*nm.m00 + y*nm.m10 + z*nm.m20;
               farray[index++] = x*nm.m01 + y*nm.m11 + z*nm.m21;
               farray[index  ] = x*nm.m02 + y*nm.m12 + z*nm.m22;
             } else {
               farray[index++] = x;
               farray[index++] = y;
               farray[index  ] = z;
             }
             pos += 3;
           } else {
             if (attrib.isFloat()) {
               float[] farray = fpolyAttribs.get(name);
               for (int n = 0; n < attrib.size; n++) {
                 farray[index++] = (float)d[pos++];
               }
             } else if (attrib.isInt()) {
               int[] iarray = ipolyAttribs.get(name);
               for (int n = 0; n < attrib.size; n++) {
                 iarray[index++] = (int)d[pos++];
               }
             } else if (attrib.isBool()) {
               byte[] barray = bpolyAttribs.get(name);
               for (int n = 0; n < attrib.size; n++) {
                 barray[index++] = (byte)d[pos++];
               }
             }
             pos += attrib.size;
           }
         }
       }
    }

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
      int index = 0;
      int nvert = i1 - i0 + 1;

      polyVertexCheck(nvert);

      if (renderMode == IMMEDIATE && pg.flushMode == FLUSH_WHEN_FULL) {
        modelviewCoords(in, i0, index, nvert, clampXY);
      } else {
        if (nvert <= PGL.MIN_ARRAYCOPY_SIZE) {
          copyFewCoords(in, i0, index, nvert);
        } else {
          copyManyCoords(in, i0, index, nvert);
        }
      }

      if (nvert <= PGL.MIN_ARRAYCOPY_SIZE) {
        copyFewAttribs(in, i0, index, nvert);
      } else {
        copyManyAttribs(in, i0, index, nvert);
      }
    }

    // Apply modelview transformation on the vertices
    private void modelviewCoords(InGeometry in, int i0, int index, int nvert, boolean clampXY) {
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

        for (String name: polyAttribs.keySet()) {
          VertexAttribute attrib = polyAttribs.get(name);
          if (attrib.isColor() || attrib.isOther()) continue;

          float[] inValues = in.fattribs.get(name);
          index = 3 * inIdx;
          x = inValues[index++];
          y = inValues[index++];
          z = inValues[index  ];

          float[] tessValues = fpolyAttribs.get(name);
          if (attrib.isPosition()) {
            index = 4 * tessIdx;
            if (clampXY) {
              // ceil emulates the behavior of JAVA2D
              tessValues[index++] =
                PApplet.ceil(x*mm.m00 + y*mm.m01 + z*mm.m02 + mm.m03);
              tessValues[index++] =
                PApplet.ceil(x*mm.m10 + y*mm.m11 + z*mm.m12 + mm.m13);
            } else {
              tessValues[index++] = x*mm.m00 + y*mm.m01 + z*mm.m02 + mm.m03;
              tessValues[index++] = x*mm.m10 + y*mm.m11 + z*mm.m12 + mm.m13;
            }
            tessValues[index++] = x*mm.m20 + y*mm.m21 + z*mm.m22 + mm.m23;
            tessValues[index  ] = x*mm.m30 + y*mm.m31 + z*mm.m32 + mm.m33;
          } else {
            index = 3 * tessIdx;
            tessValues[index++] = x*nm.m00 + y*nm.m10 + z*nm.m20;
            tessValues[index++] = x*nm.m01 + y*nm.m11 + z*nm.m21;
            tessValues[index  ] = x*nm.m02 + y*nm.m12 + z*nm.m22;
          }
        }
      }
    }

    // Just copy vertices one by one.
    private void copyFewCoords(InGeometry in, int i0, int index, int nvert) {
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

        for (String name: polyAttribs.keySet()) {
          VertexAttribute attrib = polyAttribs.get(name);
          if (attrib.isColor() || attrib.isOther()) continue;

          float[] inValues = in.fattribs.get(name);
          index = 3 * inIdx;
          x = inValues[index++];
          y = inValues[index++];
          z = inValues[index  ];

          float[] tessValues = fpolyAttribs.get(name);
          if (attrib.isPosition()) {
            index = 4 * tessIdx;
            tessValues[index++] = x;
            tessValues[index++] = y;
            tessValues[index++] = z;
            tessValues[index  ] = 1;
          } else {
            index = 3 * tessIdx;
            tessValues[index++] = x;
            tessValues[index++] = y;
            tessValues[index  ] = z;
          }
        }
      }
    }

    // Copy many vertices using arrayCopy
    private void copyManyCoords(InGeometry in, int i0, int index, int nvert) {
      for (int i = 0; i < nvert; i++) {
        // Position data needs to be copied in batches of three, because the
        // input vertices don't have a w coordinate.
        int inIdx = i0 + i;
        int tessIdx = firstPolyVertex + i;
        PApplet.arrayCopy(in.vertices, 3 * inIdx,
                          polyVertices, 4 * tessIdx, 3);
        polyVertices[4 * tessIdx + 3] = 1;

        for (String name: polyAttribs.keySet()) {
          VertexAttribute attrib = polyAttribs.get(name);
          if (!attrib.isPosition()) continue;
          float[] inValues = in.fattribs.get(name);
          float[] tessValues = fpolyAttribs.get(name);
          PApplet.arrayCopy(inValues, 3 * inIdx,
                            tessValues, 4 * tessIdx, 3);
          tessValues[4 * tessIdx + 3] = 1;
        }
      }
      PApplet.arrayCopy(in.normals, 3 * i0,
                        polyNormals, 3 * firstPolyVertex, 3 * nvert);
      for (String name: polyAttribs.keySet()) {
        VertexAttribute attrib = polyAttribs.get(name);
        if (!attrib.isNormal()) continue;
        float[] inValues = in.fattribs.get(name);
        float[] tessValues = fpolyAttribs.get(name);
        PApplet.arrayCopy(inValues, 3 * i0,
                          tessValues, 3 * firstPolyVertex, 3 * nvert);
      }
    }

    // Just copy attributes one by one.
    private void copyFewAttribs(InGeometry in, int i0, int index, int nvert) {
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

        for (String name: polyAttribs.keySet()) {
          VertexAttribute attrib = polyAttribs.get(name);
          if (attrib.isPosition() || attrib.isNormal()) continue;
          int index0 = attrib.size * inIdx;
          int index1 = attrib.size * tessIdx;
          if (attrib.isFloat()) {
            float[] inValues = in.fattribs.get(name);
            float[] tessValues = fpolyAttribs.get(name);
            for (int n = 0; n < attrib.size; n++) {
              tessValues[index1++] = inValues[index0++];
            }
          } else if (attrib.isInt()) {
            int[] inValues = in.iattribs.get(name);
            int[] tessValues = ipolyAttribs.get(name);
            for (int n = 0; n < attrib.size; n++) {
              tessValues[index1++] = inValues[index0++];
            }
          } else if (attrib.isBool()) {
            byte[] inValues = in.battribs.get(name);
            byte[] tessValues = bpolyAttribs.get(name);
            for (int n = 0; n < attrib.size; n++) {
              tessValues[index1++] = inValues[index0++];
            }
          }
        }
      }
    }

    // Copy many attributes using arrayCopy()
    private void copyManyAttribs(InGeometry in, int i0, int index, int nvert) {
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

      for (String name: polyAttribs.keySet()) {
        VertexAttribute attrib = polyAttribs.get(name);
        if (attrib.isPosition() || attrib.isNormal()) continue;
        Object inValues = null;
        Object tessValues = null;
        if (attrib.isFloat()) {
          inValues = in.fattribs.get(name);
          tessValues = fpolyAttribs.get(name);
        } else if (attrib.isInt()) {
          inValues = in.iattribs.get(name);
          tessValues = ipolyAttribs.get(name);
        } else if (attrib.isBool()) {
          inValues = in.battribs.get(name);
          tessValues = bpolyAttribs.get(name);
        }
        PApplet.arrayCopy(inValues, attrib.size * i0,
                          tessValues, attrib.tessSize * firstPolyVertex,
                          attrib.size * nvert);
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

          for (String name: polyAttribs.keySet()) {
            VertexAttribute attrib = polyAttribs.get(name);
            if (attrib.isColor() || attrib.isOther()) continue;
            float[] values = fpolyAttribs.get(name);
            if (attrib.isPosition()) {
              index = 4 * i;
              x = values[index++];
              y = values[index  ];
              index = 4 * i;
              values[index++] = x*tr.m00 + y*tr.m01 + tr.m02;
              values[index  ] = x*tr.m10 + y*tr.m11 + tr.m12;
            } else {
              index = 3 * i;
              nx = values[index++];
              ny = values[index  ];
              index = 3 * i;
              values[index++] = nx*tr.m00 + ny*tr.m01;
              values[index  ] = nx*tr.m10 + ny*tr.m11;
            }
          }
        }
      }
    }

    void applyMatrixOnLineGeometry(PMatrix2D tr, int first, int last) {
      if (first < last) {
        int index;

        float scaleFactor = matrixScale(tr);
        for (int i = first; i <= last; i++) {
          index = 4 * i;
          float x = lineVertices[index++];
          float y = lineVertices[index  ];

          index = 4 * i;
          float dx = lineDirections[index++];
          float dy = lineDirections[index  ];

          index = 4 * i;
          lineVertices[index++] = x*tr.m00 + y*tr.m01 + tr.m02;
          lineVertices[index  ] = x*tr.m10 + y*tr.m11 + tr.m12;

          index = 4 * i;
          lineDirections[index++] = dx*tr.m00 + dy*tr.m01;
          lineDirections[index  ] = dx*tr.m10 + dy*tr.m11;
          lineDirections[index + 2] *= scaleFactor;
        }
      }
    }

    void applyMatrixOnPointGeometry(PMatrix2D tr, int first, int last) {
      if (first < last) {
        int index;

        float matrixScale = matrixScale(tr);
        for (int i = first; i <= last; i++) {
          index = 4 * i;
          float x = pointVertices[index++];
          float y = pointVertices[index  ];

          index = 4 * i;
          pointVertices[index++] = x*tr.m00 + y*tr.m01 + tr.m02;
          pointVertices[index  ] = x*tr.m10 + y*tr.m11 + tr.m12;

          index = 2 * i;
          pointOffsets[index++] *= matrixScale;
          pointOffsets[index] *= matrixScale;
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

          for (String name: polyAttribs.keySet()) {
            VertexAttribute attrib = polyAttribs.get(name);
            if (attrib.isColor() || attrib.isOther()) continue;
            float[] values = fpolyAttribs.get(name);
            if (attrib.isPosition()) {
              index = 4 * i;
              x = values[index++];
              y = values[index++];
              z = values[index++];
              w = values[index  ];
              index = 4 * i;
              values[index++] = x*tr.m00 + y*tr.m01 + z*tr.m02 + w*tr.m03;
              values[index++] = x*tr.m10 + y*tr.m11 + z*tr.m12 + w*tr.m13;
              values[index++] = x*tr.m20 + y*tr.m21 + z*tr.m22 + w*tr.m23;
              values[index  ] = x*tr.m30 + y*tr.m31 + z*tr.m32 + w*tr.m33;
            } else {
              index = 3 * i;
              nx = values[index++];
              ny = values[index++];
              nz = values[index  ];
              index = 3 * i;
              values[index++] = nx*tr.m00 + ny*tr.m01 + nz*tr.m02;
              values[index++] = nx*tr.m10 + ny*tr.m11 + nz*tr.m12;
              values[index  ] = nx*tr.m20 + ny*tr.m21 + nz*tr.m22;
            }
          }
        }
      }
    }

    void applyMatrixOnLineGeometry(PMatrix3D tr, int first, int last) {
      if (first < last) {
        int index;

        float scaleFactor = matrixScale(tr);
        for (int i = first; i <= last; i++) {
          index = 4 * i;
          float x = lineVertices[index++];
          float y = lineVertices[index++];
          float z = lineVertices[index++];
          float w = lineVertices[index  ];

          index = 4 * i;
          float dx = lineDirections[index++];
          float dy = lineDirections[index++];
          float dz = lineDirections[index  ];

          index = 4 * i;
          lineVertices[index++] = x*tr.m00 + y*tr.m01 + z*tr.m02 + w*tr.m03;
          lineVertices[index++] = x*tr.m10 + y*tr.m11 + z*tr.m12 + w*tr.m13;
          lineVertices[index++] = x*tr.m20 + y*tr.m21 + z*tr.m22 + w*tr.m23;
          lineVertices[index  ] = x*tr.m30 + y*tr.m31 + z*tr.m32 + w*tr.m33;

          index = 4 * i;
          lineDirections[index++] = dx*tr.m00 + dy*tr.m01 + dz*tr.m02;
          lineDirections[index++] = dx*tr.m10 + dy*tr.m11 + dz*tr.m12;
          lineDirections[index++] = dx*tr.m20 + dy*tr.m21 + dz*tr.m22;
          lineDirections[index] *= scaleFactor;
        }
      }
    }

    void applyMatrixOnPointGeometry(PMatrix3D tr, int first, int last) {
      if (first < last) {
        int index;

        float matrixScale = matrixScale(tr);
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

          index = 2 * i;
          pointOffsets[index++] *= matrixScale;
          pointOffsets[index] *= matrixScale;
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
      rawIndices = new int[512];
      accurate2DStrokes = true;
      transform = null;
      is2D = false;
      is3D = true;
    }

    void initGluTess() {
      if (gluTess == null) {
        callback = new TessellatorCallback(tess.polyAttribs);
        gluTess = pg.pgl.createTessellator(callback);
      }
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

    void setTexCache(TexCache texCache, PImage newTexImage) {
      this.texCache = texCache;
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
            0.5f * cosLUT[(int) val] * transformScale() * strokeWeight;
          tess.pointOffsets[2 * attribIdx + 1] =
            0.5f * sinLUT[(int) val] * transformScale() * strokeWeight;
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
      if (firstPolyIndexCache == -1) firstPolyIndexCache = index; // If the geometry has no fill, needs the first poly index.
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
            0.5f * QUAD_POINT_SIGNS[k][0] * transformScale() * strokeWeight;
          tess.pointOffsets[2 * attribIdx + 1] =
            0.5f * QUAD_POINT_SIGNS[k][1] * transformScale() * strokeWeight;
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
      if (firstPolyIndexCache == -1) firstPolyIndexCache = index; // If the geometry has no fill, needs the first poly index.
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

      int vcount0 = tess.lineVertexCount;
      int icount0 = tess.lineIndexCount;
      tess.lineVertexCheck(nvert);
      tess.lineIndexCheck(nind);
      int index = in.renderMode == RETAINED ? tess.lineIndexCache.addNew() :
                                              tess.lineIndexCache.getLast();
      firstLineIndexCache = index;
      int[] tmp = {0, 0};
      tess.lineIndexCache.setCounter(tmp);
      for (int ln = 0; ln < lineCount; ln++) {
        int i0 = 2 * ln + 0;
        int i1 = 2 * ln + 1;
        index = addLineSegment3D(i0, i1, i0 - 2, i1 - 1, index, null, false);
      }
      // Adjust counts of line vertices and indices to exact values
      tess.lineIndexCache.setCounter(null);
      tess.lineIndexCount = icount0 + tmp[0];
      tess.lineVertexCount = vcount0 + tmp[1];
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
      int nvert = lineCount * 4 + nBevelTr * 3;
      int nind = lineCount * 2 * 3 + nBevelTr * 2 * 3;

      int vcount0 = tess.lineVertexCount;
      int icount0 = tess.lineIndexCount;
      tess.lineVertexCheck(nvert);
      tess.lineIndexCheck(nind);
      int index = in.renderMode == RETAINED ? tess.lineIndexCache.addNew() :
                                              tess.lineIndexCache.getLast();
      firstLineIndexCache = index;
      int i0 = 0;
      short[] lastInd = {-1, -1};
      int[] tmp = {0, 0};
      tess.lineIndexCache.setCounter(tmp);
      for (int ln = 0; ln < lineCount; ln++) {
        int i1 = ln + 1;
        if (0 < nBevelTr) {
          index = addLineSegment3D(i0, i1, i1 - 2, i1 - 1, index, lastInd, false);
        } else {
          index = addLineSegment3D(i0, i1, i1 - 2, i1 - 1, index, null, false);
        }
        i0 = i1;
      }
      // Adjust counts of line vertices and indices to exact values
      tess.lineIndexCache.setCounter(null);
      tess.lineIndexCount = icount0 + tmp[0];
      tess.lineVertexCount = vcount0 + tmp[1];
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
      int nvert = lineCount * 4 + nBevelTr * 3;
      int nind = lineCount * 2 * 3 + nBevelTr * 2 * 3;

      int vcount0 = tess.lineVertexCount;
      int icount0 = tess.lineIndexCount;
      tess.lineVertexCheck(nvert);
      tess.lineIndexCheck(nind);
      int index = in.renderMode == RETAINED ? tess.lineIndexCache.addNew() :
                                              tess.lineIndexCache.getLast();
      firstLineIndexCache = index;
      int i0 = 0;
      int i1 = -1;
      short[] lastInd = {-1, -1};
      int[] tmp = {0, 0};
      tess.lineIndexCache.setCounter(tmp);
      for (int ln = 0; ln < lineCount - 1; ln++) {
        i1 = ln + 1;
        if (0 < nBevelTr) {
          index = addLineSegment3D(i0, i1, i1 - 2, i1 - 1, index, lastInd, false);
        } else {
          index = addLineSegment3D(i0, i1, i1 - 2, i1 - 1, index, null, false);
        }
        i0 = i1;
      }
      index = addLineSegment3D(in.vertexCount - 1, 0, i1 - 2, i1 - 1, index, lastInd, false);
      if (0 < nBevelTr) {
        index = addBevel3D(0, 1, in.vertexCount - 1, 0, index, lastInd, false);
      }
      // Adjust counts of line vertices and indices to exact values
      tess.lineIndexCache.setCounter(null);
      tess.lineIndexCount = icount0 + tmp[0];
      tess.lineVertexCount = vcount0 + tmp[1];
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

      int vcount0 = tess.lineVertexCount;
      int icount0 = tess.lineIndexCount;
      tess.lineVertexCheck(nInVert);
      tess.lineIndexCheck(nInInd);
      int index = in.renderMode == RETAINED ? tess.lineIndexCache.addNew() :
                                              tess.lineIndexCache.getLast();
      firstLineIndexCache = index;
      int fi0 = 0;
      int fi1 = 0;
      short[] lastInd = {-1, -1};
      int pi0 = -1;
      int pi1 = -1;

      int[] tmp = {0, 0};
      tess.lineIndexCache.setCounter(tmp);
      for (int i = 0; i <= in.edgeCount - 1; i++) {
        int[] edge = in.edges[i];
        int i0 = edge[0];
        int i1 = edge[1];
        if (bevel) {
          if (edge[2] == EDGE_CLOSE) {
            index = addBevel3D(fi0, fi1, pi0, pi1, index, lastInd, false);
          } else {
            index = addLineSegment3D(i0, i1, pi0, pi1, index, lastInd, false);
          }
        } else if (edge[2] != EDGE_CLOSE) {
          index = addLineSegment3D(i0, i1, pi0, pi1, index, null, false);
        }
        if (edge[2] == EDGE_START) {
          fi0 = i0;
          fi1 = i1;
        }

        if (edge[2] == EDGE_STOP || edge[2] == EDGE_SINGLE || edge[2] == EDGE_CLOSE) {
          // No join with next line segment.
          lastInd[0] = lastInd[1] = -1;
          pi1 = pi0 = -1;
        } else {
          pi0 = i0;
          pi1 = i1;
        }
      }
      // Adjust counts of line vertices and indices to exact values
      tess.lineIndexCache.setCounter(null);
      tess.lineIndexCount = icount0 + tmp[0];
      tess.lineVertexCount = vcount0 + tmp[1];

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
    int addLineSegment3D(int i0, int i1, int pi0, int pi1, int index, short[] lastInd,
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
      weight *= transformScale();

      tess.setLineVertex(vidx++, strokeVertices, i0, i1, color, +weight/2);
      tess.lineIndices[iidx++] = (short) (count + 0);

      tess.setLineVertex(vidx++, strokeVertices, i0, i1, color, -weight/2);
      tess.lineIndices[iidx++] = (short) (count + 1);

      color = constStroke ? strokeColor : strokeColors[i1];
      weight = constStroke ? strokeWeight : strokeWeights[i1];
      weight *= transformScale();

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
          if (newCache) {
            if (-1 < pi0 && -1 < pi1) {
              // Vertices used in the previous cache need to be copied to the
              // newly created one
              color = constStroke ? strokeColor : strokeColors[pi0];
              weight = constStroke ? strokeWeight : strokeWeights[pi0];
              weight *= transformScale();

              tess.setLineVertex(vidx++, strokeVertices, pi1, color);
              tess.setLineVertex(vidx++, strokeVertices, pi1, pi0, color, -weight/2); // count+2 vert from previous block
              tess.setLineVertex(vidx, strokeVertices, pi1, pi0, color, +weight/2); // count+3 vert from previous block

              tess.lineIndices[iidx++] = (short) (count + 4);
              tess.lineIndices[iidx++] = (short) (count + 5);
              tess.lineIndices[iidx++] = (short) (count + 0);

              tess.lineIndices[iidx++] = (short) (count + 4);
              tess.lineIndices[iidx++] = (short) (count + 6);
              tess.lineIndices[iidx  ] = (short) (count + 1);

              cache.incCounts(index, 6, 3);
            }
          } else {
            tess.setLineVertex(vidx, strokeVertices, i0, color0);

            tess.lineIndices[iidx++] = (short) (count + 4);
            tess.lineIndices[iidx++] = lastInd[0];
            tess.lineIndices[iidx++] = (short) (count + 0);

            tess.lineIndices[iidx++] = (short) (count + 4);
            tess.lineIndices[iidx++] = lastInd[1];
            tess.lineIndices[iidx  ] = (short) (count + 1);

            cache.incCounts(index, 6, 1);
          }
        }

        // The last two vertices of the segment will be used in the next
        // bevel triangle
        lastInd[0] = (short) (count + 2);
        lastInd[1] = (short) (count + 3);
      }
      return index;
    }

    int addBevel3D(int fi0, int fi1, int pi0 ,int pi1, int index, short[] lastInd,
                   boolean constStroke) {
      IndexCache cache = tess.lineIndexCache;
      int count = cache.vertexCount[index];
      boolean newCache = false;
      if (PGL.MAX_VERTEX_INDEX1 <= count + 3) {
        // We need to start a new index block for this line.
        index = cache.addNew();
        count = 0;
        newCache = true;
      }

      int iidx = cache.indexOffset[index] + cache.indexCount[index];
      int vidx = cache.vertexOffset[index] + cache.vertexCount[index];
      int color = constStroke ? strokeColor : strokeColors[fi0];
      float weight = constStroke ? strokeWeight : strokeWeights[fi0];
      weight *= transformScale();

      tess.setLineVertex(vidx++, strokeVertices, fi0, color);
      tess.setLineVertex(vidx++, strokeVertices, fi0, fi1, color, +weight/2);
      tess.setLineVertex(vidx++, strokeVertices, fi0, fi1, color, -weight/2);

      int extra = 0;
      if (newCache && -1 < pi0 && -1 < pi1) {
        // Vertices used in the previous cache need to be copied to the
        // newly created one
        color = constStroke ? strokeColor : strokeColors[pi1];
        weight = constStroke ? strokeWeight : strokeWeights[pi1];
        weight *= transformScale();

        tess.setLineVertex(vidx++, strokeVertices, pi1, pi0, color, -weight/2);
        tess.setLineVertex(vidx  , strokeVertices, pi1, pi0, color, +weight/2);

        lastInd[0] = (short) (count + 3);
        lastInd[1] = (short) (count + 4);
        extra = 2;
      }

      tess.lineIndices[iidx++] = (short) (count + 0);
      tess.lineIndices[iidx++] = lastInd[0];
      tess.lineIndices[iidx++] = (short) (count + 1);

      tess.lineIndices[iidx++] = (short) (count + 0);
      tess.lineIndices[iidx++] = (short) (count + 2);
      tess.lineIndices[iidx  ] = lastInd[1];

      cache.incCounts(index, 6, 3 + extra);

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
      // The stroke weight is scaled so it corresponds to the current
      // "zoom level" being applied on the geometry due to scaling:
      return tess.renderMode == IMMEDIATE &&
             transformScale() * strokeWeight < PGL.MIN_CAPS_JOINS_WEIGHT;
    }

    float transformScale() {
      if (-1 < transformScale) return transformScale;
      return transformScale = matrixScale(transform);
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
      // they should always be available to be rendered individually, even
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
      prevTexImage = newTexImage;
    }

    // -----------------------------------------------------------------
    //
    // Polygon tessellation, includes edge calculation and tessellation.

    void tessellatePolygon(boolean solid, boolean closed, boolean calcNormals) {
      beginTex();

      int nInVert = in.vertexCount;

      if (3 <= nInVert) {
        firstPolyIndexCache = -1;

        initGluTess();
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

      double[] vertexT = fill ? collectVertexAttributes(i) : null;

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
          double[] vertex = Arrays.copyOf(vertexT, vertexT.length);
          vertex[0] = x1;
          vertex[1] = y1;
          vertex[2] = z1;
          gluTess.addVertex(vertex);
        }
        if (stroke) addStrokeVertex(x1, y1, z1, strokeColor, strokeWeight);
      }
    }

    void addQuadraticVertex(int i) {
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

      double[] vertexT = fill ? collectVertexAttributes(i) : null;

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
          double[] vertex = Arrays.copyOf(vertexT, vertexT.length);
          vertex[0] = x1;
          vertex[1] = y1;
          vertex[2] = z1;
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
      if (pg.curveVertexCount == 3) {
        float[] v = pg.curveVertices[pg.curveVertexCount - 2];
        addCurveInitialVertex(i, v[X], v[Y], v[Z]);
      }
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

    void addCurveInitialVertex(int i, float x, float y, float z) {
      if (fill) {
        double[] vertex0 = collectVertexAttributes(i);
        vertex0[0] = x;
        vertex0[1] = y;
        vertex0[2] = z;
        gluTess.addVertex(vertex0);
      }
      if (stroke) {
        addStrokeVertex(x, y, z, in.strokeColors[i], strokeWeight);
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

      double[] vertexT = fill ? collectVertexAttributes(i) : null;

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

      for (int j = 0; j < pg.curveDetail; j++) {
        x += xplot1; xplot1 += xplot2; xplot2 += xplot3;
        y += yplot1; yplot1 += yplot2; yplot2 += yplot3;
        z += zplot1; zplot1 += zplot2; zplot2 += zplot3;
        if (fill) {
          double[] vertex1 = Arrays.copyOf(vertexT, vertexT.length);
          vertex1[0] = x;
          vertex1[1] = y;
          vertex1[2] = z;
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

      if (fill) {
        double[] vertex = collectVertexAttributes(i);
        vertex[0] = x;
        vertex[1] = y;
        vertex[2] = z;
        gluTess.addVertex(vertex);
      }
      if (stroke) {
        addStrokeVertex(x, y, z, in.strokeColors[i], in.strokeWeights[i]);
      }
    }

    double[] collectVertexAttributes(int i) {
      final int COORD_COUNT = 3;
      final int ATTRIB_COUNT = 22;

      double[] avect = in.getAttribVector(i);

      double[] r = new double[COORD_COUNT + ATTRIB_COUNT + avect.length];

      int j = COORD_COUNT;

      int fcol = in.colors[i];
      r[j++] = (fcol >> 24) & 0xFF; // fa
      r[j++] = (fcol >> 16) & 0xFF; // fr
      r[j++] = (fcol >>  8) & 0xFF; // fg
      r[j++] = (fcol >>  0) & 0xFF; // fb

      r[j++] = in.normals[3*i + 0]; // nx
      r[j++] = in.normals[3*i + 1]; // ny
      r[j++] = in.normals[3*i + 2]; // nz

      r[j++] = in.texcoords[2*i + 0]; // u
      r[j++] = in.texcoords[2*i + 1]; // v

      int acol = in.ambient[i];
      r[j++] = (acol >> 24) & 0xFF; // aa
      r[j++] = (acol >> 16) & 0xFF; // ar
      r[j++] = (acol >>  8) & 0xFF; // ag
      r[j++] = (acol >>  0) & 0xFF; // ab

      int scol = in.specular[i];
      r[j++] = (scol >> 24) & 0xFF; // sa
      r[j++] = (scol >> 16) & 0xFF; // sr
      r[j++] = (scol >>  8) & 0xFF; // sg
      r[j++] = (scol >>  0) & 0xFF; // sb

      int ecol = in.emissive[i];
      r[j++] = (ecol >> 24) & 0xFF; // ea
      r[j++] = (ecol >> 16) & 0xFF; // er
      r[j++] = (ecol >>  8) & 0xFF; // eg
      r[j++] = (ecol >>  0) & 0xFF; // eb

      r[j++] = in.shininess[i]; // sh

      System.arraycopy(avect, 0, r, j, avect.length);

      return r;
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
      initGluTess();
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
      AttributeMap attribs;
      boolean calcNormals;
      boolean strokeTess;
      boolean clampXY;
      IndexCache cache;
      int cacheIndex;
      int vertFirst;
      int vertCount;
      int vertOffset;
      int primitive;

      public TessellatorCallback(AttributeMap attribs) {
        this.attribs = attribs;
      }

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
        vertOffset = cache.vertexOffset[cacheIndex];
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
          vertFirst = cache.vertexCount[cacheIndex];
          vertOffset = cache.vertexOffset[cacheIndex];
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
        tess.calcPolyNormal(vertFirst + vertOffset + tessIdx0,
                            vertFirst + vertOffset + tessIdx1,
                            vertFirst + vertOffset + tessIdx2);
      }

      public void vertex(Object data) {
        if (data instanceof double[]) {
          double[] d = (double[]) data;
          int l = d.length;
          if (l < 25) {
            throw new RuntimeException("TessCallback vertex() data is " +
                                       "too small");
          }

          if (vertCount < PGL.MAX_VERTEX_INDEX1) {
            tess.addPolyVertex(d, clampXY);
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
        String estring = pg.pgl.tessError(errnum);
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
        int n = ((double[])data[0]).length;
        double[] vertex = new double[n];
        vertex[0] = coords[0];
        vertex[1] = coords[1];
        vertex[2] = coords[2];

        // Calculating the rest of the vertex parameters (color,
        // normal, texcoords) as the linear combination of the
        // combined vertices.
        for (int i = 3; i < n; i++) {
          vertex[i] = 0;
          for (int j = 0; j < 4; j++) {
            double[] vertData = (double[])data[j];
            if (vertData != null) {
              vertex[i] += weight[j] * vertData[i];
            }
          }
        }

        // Normalizing normal vectors, since the weighted
        // combination of normal vectors is not necessarily
        // normal.
        normalize(vertex, 7);
        if (25 < n) {
          // We have custom attributes, look for normal attributes
          int pos = 25;
          for (int i = 0; i < attribs.size(); i++) {
            VertexAttribute attrib = attribs.get(i);
            if (attrib.isNormal()) {
              normalize(vertex, pos);
              pos += 3;
            } else {
              pos += attrib.size;
            }
          }
        }

        outData[0] = vertex;
      }

      private void normalize(double[] v, int i) {
        double sum = v[i    ] * v[i    ] +
                     v[i + 1] * v[i + 1] +
                     v[i + 2] * v[i + 2];
        double len = Math.sqrt(sum);
        if (0 < len) {
          v[i    ] /= len;
          v[i + 1] /= len;
          v[i + 2] /= len;
        }
      }
    }
  }


  static protected class DepthSorter {

    static final int X = 0;
    static final int Y = 1;
    static final int Z = 2;
    static final int W = 3;

    static final int X0 = 0;
    static final int Y0 = 1;
    static final int Z0 = 2;
    static final int X1 = 3;
    static final int Y1 = 4;
    static final int Z1 = 5;
    static final int X2 = 6;
    static final int Y2 = 7;
    static final int Z2 = 8;

    int[] triangleIndices = new int[0];
    int[] texMap = new int[0];
    int[] voffsetMap = new int[0];

    float[] minXBuffer = new float[0];
    float[] minYBuffer = new float[0];
    float[] minZBuffer = new float[0];
    float[] maxXBuffer = new float[0];
    float[] maxYBuffer = new float[0];
    float[] maxZBuffer = new float[0];

    float[] screenVertices = new float[0];

    float[] triA = new float[9];
    float[] triB = new float[9];

    BitSet marked = new BitSet();
    BitSet swapped = new BitSet();

    PGraphicsOpenGL pg;

    DepthSorter (PGraphicsOpenGL pg) {
      this.pg = pg;
    }

    void checkIndexBuffers(int newTriangleCount) {
      if (triangleIndices.length < newTriangleCount) {
        int newSize = (newTriangleCount / 4 + 1) * 5;
        triangleIndices = new int[newSize];
        texMap          = new int[newSize];
        voffsetMap      = new int[newSize];
        minXBuffer      = new float[newSize];
        minYBuffer      = new float[newSize];
        minZBuffer      = new float[newSize];
        maxXBuffer      = new float[newSize];
        maxYBuffer      = new float[newSize];
        maxZBuffer      = new float[newSize];
      }
    }

    void checkVertexBuffer(int newVertexCount) {
      int coordCount = 3*newVertexCount;
      if (screenVertices.length < coordCount) {
        int newSize = (coordCount / 4 + 1) * 5;
        screenVertices  = new float[newSize];
      }
    }

    // Sorting --------------------------------------------

    void sort(TessGeometry tessGeo) {

      int triangleCount = tessGeo.polyIndexCount / 3;
      checkIndexBuffers(triangleCount);
      int[] triangleIndices = this.triangleIndices;
      int[] texMap = this.texMap;
      int[] voffsetMap = this.voffsetMap;

      { // Initialize triangle indices
        for (int i = 0; i < triangleCount; i++) {
          triangleIndices[i] = i;
        }
      }

      { // Map caches to triangles
        TexCache texCache = pg.texCache;
        IndexCache indexCache = tessGeo.polyIndexCache;
        for (int i = 0; i < texCache.size; i++) {
          int first = texCache.firstCache[i];
          int last = texCache.lastCache[i];
          for (int n = first; n <= last; n++) {
            int ioffset = n == first
                ? texCache.firstIndex[i]
                : indexCache.indexOffset[n];
            int icount = n == last
                ? texCache.lastIndex[i] - ioffset + 1
                : indexCache.indexOffset[n] + indexCache.indexCount[n] - ioffset;

            for (int tr = ioffset / 3; tr < (ioffset + icount) / 3; tr++) {
              texMap[tr] = i;
              voffsetMap[tr] = n;
            }
          }
        }
      }

      { // Map vertices to screen
        int polyVertexCount = tessGeo.polyVertexCount;
        checkVertexBuffer(polyVertexCount);
        float[] screenVertices = this.screenVertices;

        float[] polyVertices = tessGeo.polyVertices;

        PMatrix3D projection = pg.projection;

        for (int i = 0; i < polyVertexCount; i++) {
          float x = polyVertices[4*i+X];
          float y = polyVertices[4*i+Y];
          float z = polyVertices[4*i+Z];
          float w = polyVertices[4*i+W];

          float ox = projection.m00 * x + projection.m01 * y +
              projection.m02 * z + projection.m03 * w;
          float oy = projection.m10 * x + projection.m11 * y +
              projection.m12 * z + projection.m13 * w;
          float oz = projection.m20 * x + projection.m21 * y +
              projection.m22 * z + projection.m23 * w;
          float ow = projection.m30 * x + projection.m31 * y +
              projection.m32 * z + projection.m33 * w;
          if (nonZero(ow)) {
            ox /= ow;
            oy /= ow;
            oz /= ow;
          }
          screenVertices[3*i+X] = ox;
          screenVertices[3*i+Y] = oy;
          screenVertices[3*i+Z] = -oz;
        }
      }
      float[] screenVertices = this.screenVertices;

      int[] vertexOffset = tessGeo.polyIndexCache.vertexOffset;
      short[] polyIndices = tessGeo.polyIndices;

      float[] triA = this.triA;
      float[] triB = this.triB;

      for (int i = 0; i < triangleCount; i++) {
        fetchTriCoords(triA, i, vertexOffset, voffsetMap, screenVertices, polyIndices);
        minXBuffer[i] = PApplet.min(triA[X0], triA[X1], triA[X2]);
        maxXBuffer[i] = PApplet.max(triA[X0], triA[X1], triA[X2]);
        minYBuffer[i] = PApplet.min(triA[Y0], triA[Y1], triA[Y2]);
        maxYBuffer[i] = PApplet.max(triA[Y0], triA[Y1], triA[Y2]);
        minZBuffer[i] = PApplet.min(triA[Z0], triA[Z1], triA[Z2]);
        maxZBuffer[i] = PApplet.max(triA[Z0], triA[Z1], triA[Z2]);
      }

      sortByMinZ(0, triangleCount - 1, triangleIndices, minZBuffer);

      int activeTid = 0;

      BitSet marked = this.marked;
      BitSet swapped = this.swapped;

      marked.clear();

      while (activeTid < triangleCount) {
        int testTid = activeTid + 1;
        boolean draw = false;

        swapped.clear();

        int ati = triangleIndices[activeTid];
        float minXA = minXBuffer[ati];
        float maxXA = maxXBuffer[ati];
        float minYA = minYBuffer[ati];
        float maxYA = maxYBuffer[ati];
        float maxZA = maxZBuffer[ati];

        fetchTriCoords(triA, ati, vertexOffset, voffsetMap, screenVertices, polyIndices);

        while (!draw && testTid < triangleCount) {
          int tti = triangleIndices[testTid];

          // TEST 1 // Z overlap
          if (maxZA <= minZBuffer[tti] && !marked.get(tti)) {
            draw = true; // pass, not overlapping in Z, draw it

            // TEST 2 // XY overlap using square window
          } else if (maxXA <= minXBuffer[tti] || maxYA <= minYBuffer[tti] ||
              minXA >= maxXBuffer[tti] || minYA >= maxYBuffer[tti]) {
            testTid++; // pass, not overlapping in XY

            // TEST 3 // test on which side ACTIVE is relative to TEST
          } else {
            fetchTriCoords(triB, tti, vertexOffset, voffsetMap,
                screenVertices, polyIndices);
            if (side(triB, triA, -1) > 0) {
              testTid++; // pass, ACTIVE is in halfspace behind current TEST

              // TEST 4 // test on which side TEST is relative to ACTIVE
            } else if (side(triA, triB, 1) > 0) {
              testTid++; // pass, current TEST is in halfspace in front of ACTIVE

              // FAIL, wrong depth order, swap
            } else {
              if (!swapped.get(tti)) {
                swapped.set(ati);
                marked.set(tti);
                rotateRight(triangleIndices, activeTid, testTid);

                ati = tti;
                System.arraycopy(triB, 0, triA, 0, 9);
                minXA = minXBuffer[ati];
                maxXA = maxXBuffer[ati];
                minYA = minYBuffer[ati];
                maxYA = maxYBuffer[ati];
                maxZA = maxZBuffer[ati];

                testTid = activeTid + 1;
              } else {
                // oops, we already tested this one, either in one plane or
                // interlocked in loop with others, just ignore it for now :(
                testTid++;
              }
            }
          }
        }
        activeTid++;
      }

      { // Reorder the buffers
        for (int id = 0; id < triangleCount; id++) {
          int mappedId = triangleIndices[id];
          if (id != mappedId) {

            // put the first index aside
            short i0 = polyIndices[3*id+0];
            short i1 = polyIndices[3*id+1];
            short i2 = polyIndices[3*id+2];
            int texId = texMap[id];
            int voffsetId = voffsetMap[id];

            // process the whole permutation cycle
            int currId = id;
            int nextId = mappedId;
            do {
              triangleIndices[currId] = currId;
              polyIndices[3*currId+0] = polyIndices[3*nextId+0];
              polyIndices[3*currId+1] = polyIndices[3*nextId+1];
              polyIndices[3*currId+2] = polyIndices[3*nextId+2];
              texMap[currId] = texMap[nextId];
              voffsetMap[currId] = voffsetMap[nextId];

              currId = nextId;
              nextId = triangleIndices[nextId];
            } while (nextId != id);

            // place the first index at the end
            triangleIndices[currId] = currId;
            polyIndices[3*currId+0] = i0;
            polyIndices[3*currId+1] = i1;
            polyIndices[3*currId+2] = i2;
            texMap[currId] = texId;
            voffsetMap[currId] = voffsetId;
          }
        }
      }

    }

    static void fetchTriCoords(float[] tri, int ti, int[] vertexOffset,
        int[] voffsetMap, float[] screenVertices, short[] polyIndices) {
      int voffset = vertexOffset[voffsetMap[ti]];
      int i0 = 3 * (voffset + polyIndices[3*ti+0]);
      int i1 = 3 * (voffset + polyIndices[3*ti+1]);
      int i2 = 3 * (voffset + polyIndices[3*ti+2]);
      tri[X0] = screenVertices[i0+X];
      tri[Y0] = screenVertices[i0+Y];
      tri[Z0] = screenVertices[i0+Z];
      tri[X1] = screenVertices[i1+X];
      tri[Y1] = screenVertices[i1+Y];
      tri[Z1] = screenVertices[i1+Z];
      tri[X2] = screenVertices[i2+X];
      tri[Y2] = screenVertices[i2+Y];
      tri[Z2] = screenVertices[i2+Z];
    }

    static void sortByMinZ(int leftTid, int rightTid, int[] triangleIndices,
        float[] minZBuffer) {

      // swap pivot to the front
      swap(triangleIndices, leftTid, ((leftTid + rightTid) / 2));

      int k = leftTid;
      float leftMinZ = minZBuffer[triangleIndices[leftTid]];

      // sort by min z
      for (int tid = leftTid+1; tid <= rightTid; tid++) {
        float minZ = minZBuffer[triangleIndices[tid]];
        if (minZ < leftMinZ) {
          swap(triangleIndices, ++k, tid);
        }
      }

      // swap pivot back to the middle
      swap(triangleIndices, leftTid, k);

      if (leftTid  < k - 1) sortByMinZ(leftTid,  k - 1, triangleIndices,
          minZBuffer);
      if (k + 1 < rightTid) sortByMinZ(k + 1, rightTid, triangleIndices,
          minZBuffer);
    }

    // Math -----------------------------------------------

    static int side(float[] tri1, float[] tri2, float tz) {
      float Dx, Dy, Dz, Dw;
      { // Get the equation of the plane
        float
            ABx = tri1[X1] - tri1[X0], ACx = tri1[X2] - tri1[X0],
            ABy = tri1[Y1] - tri1[Y0], ACy = tri1[Y2] - tri1[Y0],
            ABz = tri1[Z1] - tri1[Z0], ACz = tri1[Z2] - tri1[Z0];

        Dx = ABy*ACz - ABz*ACy; Dy = ABz*ACx - ABx*ACz; Dz = ABx*ACy - ABy*ACx;

        // Normalize normal vector
        float rMag = 1.0f/(float) Math.sqrt(Dx * Dx + Dy * Dy + Dz * Dz);
        Dx *= rMag; Dy *= rMag; Dz *= rMag;

        Dw = -dot(Dx, Dy, Dz, tri1[X0], tri1[Y0], tri1[Z0]);
      }

      float distTest = dot(Dx, Dy, Dz,
          tri1[X0], tri1[Y0], tri1[Z0] + 100*tz) + Dw;

      float distA = dot(Dx, Dy, Dz, tri2[X0], tri2[Y0], tri2[Z0]) + Dw;
      float distB = dot(Dx, Dy, Dz, tri2[X1], tri2[Y1], tri2[Z1]) + Dw;
      float distC = dot(Dx, Dy, Dz, tri2[X2], tri2[Y2], tri2[Z2]) + Dw;

      // Ignore relatively close vertices to get stable results
      // when some parts of polygons are close to each other
      float absA = PApplet.abs(distA);
      float absB = PApplet.abs(distB);
      float absC = PApplet.abs(distC);
      float eps = PApplet.max(absA, absB, absC) * 0.1f;

      float sideA = ((absA < eps) ? 0.0f : distA) * distTest;
      float sideB = ((absB < eps) ? 0.0f : distB) * distTest;
      float sideC = ((absC < eps) ? 0.0f : distC) * distTest;

      boolean sameSide    = sideA >= 0 && sideB >= 0 && sideC >= 0;
      boolean notSameSide = sideA <= 0 && sideB <= 0 && sideC <= 0;

      return sameSide ? 1 : notSameSide ? -1 : 0;
    }

    static float dot(float a1, float a2, float a3,
                     float b1, float b2, float b3) {
      return a1 * b1 + a2 * b2 + a3 * b3;
    }


    // Array utils ---------------------------------------

    static void swap(int[] array, int i1, int i2) {
      int temp = array[i1];
      array[i1] = array[i2];
      array[i2] = temp;
    }

    static void rotateRight(int[] array, int i1, int i2) {
      if (i1 == i2) return;
      int temp = array[i2];
      System.arraycopy(array, i1, array, i1 + 1, i2 - i1);
      array[i1] = temp;
    }

  }

}
