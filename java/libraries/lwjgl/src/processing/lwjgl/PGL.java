/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2011-12 Ben Fry and Casey Reas

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

package processing.lwjgl;

import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Color;
import java.nio.Buffer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;

import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.EXTFramebufferObject;
import org.lwjgl.opengl.EXTTextureFilterAnisotropic;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;
import org.lwjgl.util.glu.GLU;
import org.lwjgl.util.glu.GLUtessellator;
import org.lwjgl.util.glu.GLUtessellatorCallbackAdapter;
import org.lwjgl.opengl.PixelFormat;

import processing.core.PApplet;
import processing.core.PConstants;
import processing.event.Event;
import processing.event.KeyEvent;
import processing.event.MouseEvent;
import processing.opengl.PGraphicsOpenGL;
import processing.opengl.Texture;

/**
 * Processing-OpenGL abstraction layer.
 *
 * Warnings are suppressed for static access because presumably on Android,
 * the GL2 vs GL distinctions are necessary, whereas on desktop they are not.
 *
 * This version of PGL uses LWJGL, see some issues with it:
 * http://lwjgl.org/forum/index.php/topic,4711.0.html
 * http://www.java-gaming.org/topics/cannot-add-mouselistener-to-java-awt-canvas-with-lwjgl-on-windows/24650/view.html
 *
 */
@SuppressWarnings("static-access")
public class PGL extends processing.opengl.PGL {

  ///////////////////////////////////////////////////////////

  // Parameters

  public static boolean FORCE_SCREEN_FBO             = false;
  public static final boolean USE_DIRECT_BUFFERS     = true;
  public static final int MIN_DIRECT_BUFFER_SIZE     = 16;
  public static final boolean SAVE_SURFACE_TO_PIXELS = true;

  /** Enables/disables mipmap use. **/
  protected static final boolean MIPMAPS_ENABLED = true;

  /** Initial sizes for arrays of input and tessellated data. */
  protected static final int DEFAULT_IN_VERTICES   = 64;
  protected static final int DEFAULT_IN_EDGES      = 128;
  protected static final int DEFAULT_IN_TEXTURES   = 64;
  protected static final int DEFAULT_TESS_VERTICES = 64;
  protected static final int DEFAULT_TESS_INDICES  = 128;

  /** Maximum lights by default is 8, the minimum defined by OpenGL. */
  protected static final int MAX_LIGHTS = 8;

  /** Maximum index value of a tessellated vertex. GLES restricts the vertex
   * indices to be of type unsigned short. Since Java only supports signed
   * shorts as primitive type we have 2^15 = 32768 as the maximum number of
   * vertices that can be referred to within a single VBO. */
  protected static final int MAX_VERTEX_INDEX  = 32767;
  protected static final int MAX_VERTEX_INDEX1 = MAX_VERTEX_INDEX + 1;

  /** Count of tessellated fill, line or point vertices that will
   * trigger a flush in the immediate mode. It doesn't necessarily
   * be equal to MAX_VERTEX_INDEX1, since the number of vertices can
   * be effectively much large since the renderer uses offsets to
   * refer to vertices beyond the MAX_VERTEX_INDEX limit.
   */
  protected static final int FLUSH_VERTEX_COUNT = MAX_VERTEX_INDEX1;

  /** Maximum dimension of a texture used to hold font data. **/
  protected static final int MAX_FONT_TEX_SIZE = 1024;

  /** Minimum stroke weight needed to apply the full path stroking
   * algorithm that properly generates caps and joins.
   */
  protected static final float MIN_CAPS_JOINS_WEIGHT = 2f;

  /** Maximum length of linear paths to be stroked with the
   * full algorithm that generates accurate caps and joins.
   */
  protected static final int MAX_CAPS_JOINS_LENGTH = 5000;

  /** Minimum array size to use arrayCopy method(). **/
  protected static final int MIN_ARRAYCOPY_SIZE = 2;

  protected static int request_depth_bits = 24;
  protected static int request_stencil_bits = 8;
  protected static int request_alpha_bits = 8;

  protected static final int SIZEOF_SHORT = Short.SIZE / 8;
  protected static final int SIZEOF_INT = Integer.SIZE / 8;
  protected static final int SIZEOF_FLOAT = Float.SIZE / 8;
  protected static final int SIZEOF_BYTE = Byte.SIZE / 8;
  protected static final int SIZEOF_INDEX = SIZEOF_SHORT;
  protected static final int INDEX_TYPE = GL11.GL_UNSIGNED_SHORT;

  /** Machine Epsilon for float precision. **/
  protected static float FLOAT_EPS = Float.MIN_VALUE;
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
  protected static boolean BIG_ENDIAN =
    ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN;

  protected static final String SHADER_PREPROCESSOR_DIRECTIVE =
    "#ifdef GL_ES\n" +
    "precision mediump float;\n" +
    "precision mediump int;\n" +
    "#endif\n";

  ///////////////////////////////////////////////////////////

  // OpenGL constants

  public static final int FALSE = GL11.GL_FALSE;
  public static final int TRUE  = GL11.GL_TRUE;

  public static final int LESS      = GL11.GL_LESS;
  public static final int LEQUAL    = GL11.GL_LEQUAL;

  public static final int CCW       = GL11.GL_CCW;
  public static final int CW        = GL11.GL_CW;

  public static final int CULL_FACE      = GL11.GL_CULL_FACE;
  public static final int FRONT          = GL11.GL_FRONT;
  public static final int BACK           = GL11.GL_BACK;
  public static final int FRONT_AND_BACK = GL11.GL_FRONT_AND_BACK;

  public static final int VIEWPORT = GL11.GL_VIEWPORT;

  public static final int SCISSOR_TEST    = GL11.GL_SCISSOR_TEST;
  public static final int DEPTH_TEST      = GL11.GL_DEPTH_TEST;
  public static final int DEPTH_WRITEMASK = GL11.GL_DEPTH_WRITEMASK;

  public static final int COLOR_BUFFER_BIT   = GL11.GL_COLOR_BUFFER_BIT;
  public static final int DEPTH_BUFFER_BIT   = GL11.GL_DEPTH_BUFFER_BIT;
  public static final int STENCIL_BUFFER_BIT = GL11.GL_STENCIL_BUFFER_BIT;

  public static final int FUNC_ADD              = GL14.GL_FUNC_ADD;
  public static final int FUNC_MIN              = GL14.GL_MIN;
  public static final int FUNC_MAX              = GL14.GL_MAX;
  public static final int FUNC_REVERSE_SUBTRACT = GL14.GL_FUNC_REVERSE_SUBTRACT;

  public static final int TEXTURE_2D        = GL11.GL_TEXTURE_2D;
  public static final int TEXTURE_RECTANGLE = GL31.GL_TEXTURE_RECTANGLE;

  public static final int TEXTURE_BINDING_2D        = GL11.GL_TEXTURE_BINDING_2D;
  public static final int TEXTURE_BINDING_RECTANGLE =
    GL31.GL_TEXTURE_BINDING_RECTANGLE;

  public static final int RGB            = GL11.GL_RGB;
  public static final int RGBA           = GL11.GL_RGBA;
  public static final int ALPHA          = GL11.GL_ALPHA;
  public static final int UNSIGNED_INT   = GL11.GL_UNSIGNED_INT;
  public static final int UNSIGNED_BYTE  = GL11.GL_UNSIGNED_BYTE;
  public static final int UNSIGNED_SHORT = GL11.GL_UNSIGNED_SHORT;
  public static final int FLOAT          = GL11.GL_FLOAT;

  public static final int NEAREST               = GL11.GL_NEAREST;
  public static final int LINEAR                = GL11.GL_LINEAR;
  public static final int LINEAR_MIPMAP_NEAREST = GL11.GL_LINEAR_MIPMAP_NEAREST;
  public static final int LINEAR_MIPMAP_LINEAR  = GL11.GL_LINEAR_MIPMAP_LINEAR;

  public static final int TEXTURE_MAX_ANISOTROPY =
    EXTTextureFilterAnisotropic.GL_TEXTURE_MAX_ANISOTROPY_EXT;
  public static final int MAX_TEXTURE_MAX_ANISOTROPY =
    EXTTextureFilterAnisotropic.GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT;

  public static final int CLAMP_TO_EDGE = GL12.GL_CLAMP_TO_EDGE;
  public static final int REPEAT        = GL11.GL_REPEAT;

  public static final int RGBA8            = GL11.GL_RGBA8;
  public static final int DEPTH24_STENCIL8 = GL30.GL_DEPTH24_STENCIL8;

  public static final int DEPTH_COMPONENT   = GL11.GL_DEPTH_COMPONENT;
  public static final int DEPTH_COMPONENT16 = GL14.GL_DEPTH_COMPONENT16;
  public static final int DEPTH_COMPONENT24 = GL14.GL_DEPTH_COMPONENT24;
  public static final int DEPTH_COMPONENT32 = GL14.GL_DEPTH_COMPONENT32;

  public static final int STENCIL_INDEX  = GL11.GL_STENCIL_INDEX;
  public static final int STENCIL_INDEX1 = GL30.GL_STENCIL_INDEX1;
  public static final int STENCIL_INDEX4 = GL30.GL_STENCIL_INDEX4;
  public static final int STENCIL_INDEX8 = GL30.GL_STENCIL_INDEX8;

  public static final int ARRAY_BUFFER         = GL15.GL_ARRAY_BUFFER;
  public static final int ELEMENT_ARRAY_BUFFER = GL15.GL_ELEMENT_ARRAY_BUFFER;

  public static final int SAMPLES = GL13.GL_SAMPLES;

  public static final int FRAMEBUFFER_COMPLETE                      =
    GL30.GL_FRAMEBUFFER_COMPLETE;
  public static final int FRAMEBUFFER_INCOMPLETE_ATTACHMENT         =
    GL30.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT;
  public static final int FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT =
    GL30.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT;
  public static final int FRAMEBUFFER_INCOMPLETE_DIMENSIONS         =
    EXTFramebufferObject.GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS_EXT;
  public static final int FRAMEBUFFER_INCOMPLETE_FORMATS            =
    EXTFramebufferObject.GL_FRAMEBUFFER_INCOMPLETE_FORMATS_EXT;
  public static final int FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER        =
    GL30.GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER;
  public static final int FRAMEBUFFER_INCOMPLETE_READ_BUFFER        =
    GL30.GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER;
  public static final int FRAMEBUFFER_UNSUPPORTED                   =
    GL30.GL_FRAMEBUFFER_UNSUPPORTED;

  public static final int STATIC_DRAW  = GL15.GL_STATIC_DRAW;
  public static final int DYNAMIC_DRAW = GL15.GL_DYNAMIC_DRAW;
  public static final int STREAM_DRAW  = GL15.GL_STREAM_DRAW;

  public static final int READ_ONLY  = GL15.GL_READ_ONLY;
  public static final int WRITE_ONLY = GL15.GL_WRITE_ONLY;
  public static final int READ_WRITE = GL15.GL_READ_WRITE;

  public static final int TRIANGLE_FAN   = GL11.GL_TRIANGLE_FAN;
  public static final int TRIANGLE_STRIP = GL11.GL_TRIANGLE_STRIP;
  public static final int TRIANGLES      = GL11.GL_TRIANGLES;

  public static final int VENDOR                   = GL11.GL_VENDOR;
  public static final int RENDERER                 = GL11.GL_RENDERER;
  public static final int VERSION                  = GL11.GL_VERSION;
  public static final int EXTENSIONS               = GL11.GL_EXTENSIONS;
  public static final int SHADING_LANGUAGE_VERSION =
    GL20.GL_SHADING_LANGUAGE_VERSION;

  public static final int MAX_TEXTURE_SIZE         = GL11.GL_MAX_TEXTURE_SIZE;
  public static final int MAX_SAMPLES              = GL30.GL_MAX_SAMPLES;
  public static final int ALIASED_LINE_WIDTH_RANGE =
    GL12.GL_ALIASED_LINE_WIDTH_RANGE;
  public static final int ALIASED_POINT_SIZE_RANGE =
    GL12.GL_ALIASED_POINT_SIZE_RANGE;
  public static final int DEPTH_BITS = GL11.GL_DEPTH_BITS;
  public static final int STENCIL_BITS = GL11.GL_STENCIL_BITS;

  public static final int TESS_WINDING_NONZERO = GLU.GLU_TESS_WINDING_NONZERO;
  public static final int TESS_WINDING_ODD     = GLU.GLU_TESS_WINDING_ODD;

  public static final int TEXTURE0           = GL13.GL_TEXTURE0;
  public static final int TEXTURE1           = GL13.GL_TEXTURE1;
  public static final int TEXTURE2           = GL13.GL_TEXTURE2;
  public static final int TEXTURE3           = GL13.GL_TEXTURE3;
  public static final int TEXTURE_MIN_FILTER = GL11.GL_TEXTURE_MIN_FILTER;
  public static final int TEXTURE_MAG_FILTER = GL11.GL_TEXTURE_MAG_FILTER;
  public static final int TEXTURE_WRAP_S     = GL11.GL_TEXTURE_WRAP_S;
  public static final int TEXTURE_WRAP_T     = GL11.GL_TEXTURE_WRAP_T;

  public static final int BLEND               = GL11.GL_BLEND;
  public static final int ONE                 = GL11.GL_ONE;
  public static final int ZERO                = GL11.GL_ZERO;
  public static final int SRC_ALPHA           = GL11.GL_SRC_ALPHA;
  public static final int DST_ALPHA           = GL11.GL_DST_ALPHA;
  public static final int ONE_MINUS_SRC_ALPHA = GL11.GL_ONE_MINUS_SRC_ALPHA;
  public static final int ONE_MINUS_DST_COLOR = GL11.GL_ONE_MINUS_DST_COLOR;
  public static final int ONE_MINUS_SRC_COLOR = GL11.GL_ONE_MINUS_SRC_COLOR;
  public static final int DST_COLOR           = GL11.GL_DST_COLOR;
  public static final int SRC_COLOR           = GL11.GL_SRC_COLOR;

  public static final int FRAMEBUFFER        = GL30.GL_FRAMEBUFFER;
  public static final int COLOR_ATTACHMENT0  = GL30.GL_COLOR_ATTACHMENT0;
  public static final int COLOR_ATTACHMENT1  = GL30.GL_COLOR_ATTACHMENT1;
  public static final int COLOR_ATTACHMENT2  = GL30.GL_COLOR_ATTACHMENT2;
  public static final int COLOR_ATTACHMENT3  = GL30.GL_COLOR_ATTACHMENT3;
  public static final int RENDERBUFFER       = GL30.GL_RENDERBUFFER;
  public static final int DEPTH_ATTACHMENT   = GL30.GL_DEPTH_ATTACHMENT;
  public static final int STENCIL_ATTACHMENT = GL30.GL_STENCIL_ATTACHMENT;
  public static final int READ_FRAMEBUFFER   = GL30.GL_READ_FRAMEBUFFER;
  public static final int DRAW_FRAMEBUFFER   = GL30.GL_DRAW_FRAMEBUFFER;

  public static final int VERTEX_SHADER        = GL20.GL_VERTEX_SHADER;
  public static final int FRAGMENT_SHADER      = GL20.GL_FRAGMENT_SHADER;
  public static final int INFO_LOG_LENGTH      = GL20.GL_INFO_LOG_LENGTH;
  public static final int SHADER_SOURCE_LENGTH = GL20.GL_SHADER_SOURCE_LENGTH;
  public static final int COMPILE_STATUS       = GL20.GL_COMPILE_STATUS;
  public static final int LINK_STATUS          = GL20.GL_LINK_STATUS;
  public static final int VALIDATE_STATUS      = GL20.GL_VALIDATE_STATUS;

  public static final int MULTISAMPLE    = GL13.GL_MULTISAMPLE;
  public static final int POINT_SMOOTH   = GL11.GL_POINT_SMOOTH;
  public static final int LINE_SMOOTH    = GL11.GL_LINE_SMOOTH;
  public static final int POLYGON_SMOOTH = GL11.GL_POLYGON_SMOOTH;

  /** GLU interface **/
  public static GLU glu;

  /** The canvas where OpenGL rendering takes place */
  public static Canvas canvas;

  /** OpenGL thread */
  protected static Thread glThread;

  /** Just holds a unique ID */
  protected static int context;

  /** The PGraphics object using this interface */
  protected PGraphicsOpenGL pg;

  /** Poller threads to get the keyboard/mouse events from LWJGL */
  protected static KeyPoller keyPoller;
  protected static MousePoller mousePoller;

  /** Which texturing targets are enabled */
  protected static boolean[] texturingTargets = { false, false };

  /** Which textures are bound to each target */
  protected static int[] boundTextures = { 0, 0 };

  ///////////////////////////////////////////////////////////

  // FBO layer

  protected static boolean fboLayerByDefault = FORCE_SCREEN_FBO;
  protected static boolean fboLayerCreated = false;
  protected static boolean fboLayerInUse = false;
  protected static boolean firstFrame = true;
  protected static int reqNumSamples;
  protected static int numSamples;
  protected static IntBuffer glColorFbo;
  protected static IntBuffer glMultiFbo;
  protected static IntBuffer glColorBuf;
  protected static IntBuffer glColorTex;
  protected static IntBuffer glDepthStencil;
  protected static IntBuffer glDepth;
  protected static IntBuffer glStencil;
  protected static int fboWidth, fboHeight;
  protected static int backTex, frontTex;

  protected static boolean needToClearBuffers;

  ///////////////////////////////////////////////////////////

  // Texture rendering

  protected static boolean loadedTex2DShader = false;
  protected static int tex2DShaderProgram;
  protected static int tex2DVertShader;
  protected static int tex2DFragShader;
  protected static int tex2DShaderContext;
  protected static int tex2DVertLoc;
  protected static int tex2DTCoordLoc;

  protected static boolean loadedTexRectShader = false;
  protected static int texRectShaderProgram;
  protected static int texRectVertShader;
  protected static int texRectFragShader;
  protected static int texRectShaderContext;
  protected static int texRectVertLoc;
  protected static int texRectTCoordLoc;

  protected static float[] texCoords = {
    //  X,     Y,    U,    V
    -1.0f, -1.0f, 0.0f, 0.0f,
    +1.0f, -1.0f, 1.0f, 0.0f,
    -1.0f, +1.0f, 0.0f, 1.0f,
    +1.0f, +1.0f, 1.0f, 1.0f
  };
  protected static FloatBuffer texData;

  protected static String texVertShaderSource =
    "attribute vec2 inVertex;" +
    "attribute vec2 inTexcoord;" +
    "varying vec2 vertTexcoord;" +
    "void main() {" +
    "  gl_Position = vec4(inVertex, 0, 1);" +
    "  vertTexcoord = inTexcoord;" +
    "}";

  protected static String tex2DFragShaderSource =
    SHADER_PREPROCESSOR_DIRECTIVE +
    "uniform sampler2D textureSampler;" +
    "varying vec2 vertTexcoord;" +
    "void main() {" +
    "  gl_FragColor = texture2D(textureSampler, vertTexcoord.st);" +
    "}";

  protected static String texRectFragShaderSource =
    SHADER_PREPROCESSOR_DIRECTIVE +
    "uniform sampler2DRect textureSampler;" +
    "varying vec2 vertTexcoord;" +
    "void main() {" +
    "  gl_FragColor = texture2DRect(textureSampler, vertTexcoord.st);" +
    "}";

  ///////////////////////////////////////////////////////////

  // Utilities

  protected ByteBuffer byteBuffer;
  protected IntBuffer intBuffer;

  protected IntBuffer colorBuffer;
  protected FloatBuffer depthBuffer;
  protected ByteBuffer stencilBuffer;


  ///////////////////////////////////////////////////////////

  // Initialization, finalization
  
  public PGL(PGraphicsOpenGL pg) {
    this.pg = pg;
    if (glu == null) {
      glu = new GLU();
    }
    if (glColorTex == null) {
      glColorTex = allocateIntBuffer(2);
      glColorFbo = allocateIntBuffer(1);
      glMultiFbo = allocateIntBuffer(1);
      glColorBuf = allocateIntBuffer(1);
      glDepthStencil = allocateIntBuffer(1);
      glDepth = allocateIntBuffer(1);
      glStencil = allocateIntBuffer(1);

      fboLayerCreated = false;
      fboLayerInUse = false;
      firstFrame = false;
      needToClearBuffers = false;
    }

    byteBuffer = allocateByteBuffer(1);
    intBuffer = allocateIntBuffer(1);
  }


  protected void setFrameRate(float framerate) {
  }


  protected void initSurface(int antialias) {
    if (canvas != null) {
      keyPoller.requestStop();
      mousePoller.requestStop();

      try {
        Display.setParent(null);
      } catch (LWJGLException e) {
        e.printStackTrace();
      }
      Display.destroy();
      
      pg.parent.remove(canvas);
    }

    canvas = new Canvas();
    canvas.setFocusable(true);
    canvas.requestFocus();
    canvas.setBackground(new Color(pg.backgroundColor, true));   
    canvas.setBounds(0, 0, pg.parent.width, pg.parent.height);
    
    pg.parent.setLayout(new BorderLayout());
    pg.parent.add(canvas, BorderLayout.CENTER);     
    
    try {      
      DisplayMode[] modes = Display.getAvailableDisplayModes();
      int bpp = 0; 
      for (int i = 0; i < modes.length; i++) {
        bpp = PApplet.max(modes[i].getBitsPerPixel(), bpp);
      }
      PixelFormat format = new PixelFormat(bpp, request_alpha_bits,
                                                request_depth_bits,
                                                request_stencil_bits, 1);
      Display.setDisplayMode(new DisplayMode(pg.parent.width, pg.parent.height));
      int argb = pg.backgroundColor;
      float r = ((argb >> 16) & 0xff) / 255.0f;
      float g = ((argb >> 8) & 0xff) / 255.0f;
      float b = ((argb) & 0xff) / 255.0f; 
      Display.setInitialBackground(r, g, b); 
      Display.setParent(canvas);      
      Display.setVSyncEnabled(true);
      Display.create(format);

      // Might be useful later to specify the context attributes.
      // http://lwjgl.org/javadoc/org/lwjgl/opengl/ContextAttribs.html
//      ContextAttribs contextAtrributes = new ContextAttribs(4, 0);
//      contextAtrributes.withForwardCompatible(true);
//      contextAtrributes.withProfileCore(true);
//      Display.create(pixelFormat, contextAtrributes);      
    } catch (LWJGLException e) {
      e.printStackTrace();
    }
    
    context = Display.getDrawable().hashCode();

    keyPoller = new KeyPoller(pg.parent);
    keyPoller.start();

    mousePoller = new MousePoller(pg.parent);
    mousePoller.start();

    reqNumSamples = qualityToSamples(antialias);
    fboLayerCreated = false;
    fboLayerInUse = false;
    firstFrame = true;
    needToClearBuffers = true;
  }


  protected void deleteSurface() {
    if (glColorTex != null) {
      deleteTextures(2, glColorTex);
      deleteFramebuffers(1, glColorFbo);
      deleteFramebuffers(1, glMultiFbo);
      deleteRenderbuffers(1, glColorBuf);
      deleteRenderbuffers(1, glDepthStencil);
      deleteRenderbuffers(1, glDepth);
      deleteRenderbuffers(1, glStencil);
    }
    fboLayerCreated = false;
    fboLayerInUse = false;
    firstFrame = false;
    needToClearBuffers = false;
  }


  protected void update() {
    if (!fboLayerCreated) {
      if (!hasFBOs()) {
        throw new RuntimeException("Framebuffer objects are not supported by this hardware (or driver)");
      }
      if (!hasShaders()) {
        throw new RuntimeException("GLSL shaders are not supported by this hardware (or driver)");
      }      
      
      String ext = getString(EXTENSIONS);
      if (-1 < ext.indexOf("texture_non_power_of_two")) {
        fboWidth = pg.width;
        fboHeight = pg.height;
      } else {
        fboWidth = nextPowerOfTwo(pg.width);
        fboHeight = nextPowerOfTwo(pg.height);
      }

      if (-1 < ext.indexOf("_framebuffer_multisample")) {
        numSamples = reqNumSamples;
      } else {
        numSamples = 1;
      }
      boolean multisample = 1 < numSamples;

      boolean packed = ext.indexOf("packed_depth_stencil") != -1;
      int depthBits = getDepthBits();
      int stencilBits = getStencilBits();

      genTextures(2, glColorTex);
      for (int i = 0; i < 2; i++) {
        bindTexture(TEXTURE_2D, glColorTex.get(i));
        texParameteri(TEXTURE_2D, TEXTURE_MIN_FILTER, NEAREST);
        texParameteri(TEXTURE_2D, TEXTURE_MAG_FILTER, NEAREST);
        texParameteri(TEXTURE_2D, TEXTURE_WRAP_S, CLAMP_TO_EDGE);
        texParameteri(TEXTURE_2D, TEXTURE_WRAP_T, CLAMP_TO_EDGE);
        texImage2D(TEXTURE_2D, 0, RGBA, fboWidth, fboHeight, 0,
                   RGBA, UNSIGNED_BYTE, null);
        initTexture(TEXTURE_2D, RGBA, fboWidth, fboHeight, pg.backgroundColor);
      }
      bindTexture(TEXTURE_2D, 0);

      backTex = 0;
      frontTex = 1;

      genFramebuffers(1, glColorFbo);
      bindFramebuffer(FRAMEBUFFER, glColorFbo.get(0));
      framebufferTexture2D(FRAMEBUFFER, COLOR_ATTACHMENT0, TEXTURE_2D,
                           glColorTex.get(backTex), 0);

      if (multisample) {
        // Creating multisampled FBO
        genFramebuffers(1, glMultiFbo);
        bindFramebuffer(FRAMEBUFFER, glMultiFbo.get(0));

        // color render buffer...
        genRenderbuffers(1, glColorBuf);
        bindRenderbuffer(RENDERBUFFER, glColorBuf.get(0));
        renderbufferStorageMultisample(RENDERBUFFER, numSamples,
                                       RGBA8, fboWidth, fboHeight);
        framebufferRenderbuffer(FRAMEBUFFER, COLOR_ATTACHMENT0,
                                RENDERBUFFER, glColorBuf.get(0));
      }

      // Creating depth and stencil buffers
      if (packed && depthBits == 24 && stencilBits == 8) {
        // packed depth+stencil buffer
        genRenderbuffers(1, glDepthStencil);
        bindRenderbuffer(RENDERBUFFER, glDepthStencil.get(0));
        if (multisample) {
          renderbufferStorageMultisample(RENDERBUFFER, numSamples,
                                         DEPTH24_STENCIL8, fboWidth, fboHeight);
        } else {
          renderbufferStorage(RENDERBUFFER, DEPTH24_STENCIL8,
                              fboWidth, fboHeight);
        }
        framebufferRenderbuffer(FRAMEBUFFER, DEPTH_ATTACHMENT, RENDERBUFFER,
                                glDepthStencil.get(0));
        framebufferRenderbuffer(FRAMEBUFFER, STENCIL_ATTACHMENT, RENDERBUFFER,
                                glDepthStencil.get(0));
      } else {
        // separate depth and stencil buffers
        if (0 < depthBits) {
          int depthComponent = DEPTH_COMPONENT16;
          if (depthBits == 32) {
            depthComponent = DEPTH_COMPONENT32;
          } else if (depthBits == 24) {
            depthComponent = DEPTH_COMPONENT24;
          } else if (depthBits == 16) {
            depthComponent = DEPTH_COMPONENT16;
          }

          genRenderbuffers(1, glDepth);
          bindRenderbuffer(RENDERBUFFER, glDepth.get(0));
          if (multisample) {
            renderbufferStorageMultisample(RENDERBUFFER, numSamples,
                                           depthComponent, fboWidth, fboHeight);
          } else {
            renderbufferStorage(RENDERBUFFER, depthComponent,
                                fboWidth, fboHeight);
          }
          framebufferRenderbuffer(FRAMEBUFFER, DEPTH_ATTACHMENT,
                                  RENDERBUFFER, glDepth.get(0));
        }

        if (0 < stencilBits) {
          int stencilIndex = STENCIL_INDEX1;
          if (stencilBits == 8) {
            stencilIndex = STENCIL_INDEX8;
          } else if (stencilBits == 4) {
            stencilIndex = STENCIL_INDEX4;
          } else if (stencilBits == 1) {
            stencilIndex = STENCIL_INDEX1;
          }

          genRenderbuffers(1, glStencil);
          bindRenderbuffer(RENDERBUFFER, glStencil.get(0));
          if (multisample) {
            renderbufferStorageMultisample(RENDERBUFFER, numSamples,
                                           stencilIndex, fboWidth, fboHeight);
          } else {
            renderbufferStorage(RENDERBUFFER, stencilIndex,
                                fboWidth, fboHeight);
          }
          framebufferRenderbuffer(FRAMEBUFFER, STENCIL_ATTACHMENT,
                                  RENDERBUFFER, glStencil.get(0));
        }
      }

      validateFramebuffer();

      // Clear all buffers.
      clearDepth(1);
      clearStencil(0);      
      int argb = pg.backgroundColor;
      float a = ((argb >> 24) & 0xff) / 255.0f;
      float r = ((argb >> 16) & 0xff) / 255.0f;
      float g = ((argb >> 8) & 0xff) / 255.0f;
      float b = ((argb) & 0xff) / 255.0f;
      clearColor(r, g, b, a);
      clear(DEPTH_BUFFER_BIT | STENCIL_BUFFER_BIT | COLOR_BUFFER_BIT);

      bindFramebuffer(FRAMEBUFFER, 0);

      fboLayerCreated = true;
    }
  }


  protected int getReadFramebuffer() {
    if (fboLayerInUse) {
      return glColorFbo.get(0);
    } else {
      return 0;
    }
  }


  protected int getDrawFramebuffer() {
    if (fboLayerInUse) {
      if (1 < numSamples) {
        return glMultiFbo.get(0);
      } else {
        return glColorFbo.get(0);
      }
    } else {
      return 0;
    }
  }


  protected int getDefaultDrawBuffer() {
    if (fboLayerInUse) {
      return COLOR_ATTACHMENT0;
    } else {
      return BACK;
    }
  }


  protected int getDefaultReadBuffer() {
    if (fboLayerInUse) {
      return COLOR_ATTACHMENT0;
    } else {
      return FRONT;
    }
  }


  protected boolean isFBOBacked() {
    return fboLayerInUse;
  }


  protected void needFBOLayer() {
    FORCE_SCREEN_FBO = true;
  }


  protected boolean isMultisampled() {
    return 1 < numSamples;
  }


  protected int getDepthBits() {
    intBuffer.rewind();
    getIntegerv(DEPTH_BITS, intBuffer);
    return intBuffer.get(0);
  }


  protected int getStencilBits() {
    intBuffer.rewind();
    getIntegerv(STENCIL_BITS, intBuffer);
    return intBuffer.get(0);
  }


  protected boolean getDepthTest() {
    intBuffer.rewind();
    getBooleanv(DEPTH_TEST, intBuffer);
    return intBuffer.get(0) == 0 ? false : true;
  }


  protected boolean getDepthWriteMask() {
    intBuffer.rewind();
    getBooleanv(DEPTH_WRITEMASK, intBuffer);
    return intBuffer.get(0) == 0 ? false : true;
  }


  protected Texture wrapBackTexture() {
    Texture tex = new Texture();
    tex.init(pg.width, pg.height,
             glColorTex.get(backTex), TEXTURE_2D, RGBA,
             fboWidth, fboHeight, NEAREST, NEAREST,
             CLAMP_TO_EDGE, CLAMP_TO_EDGE);
    tex.invertedY(true);
    tex.colorBuffer(true);
    pg.setCache(pg, tex);
    return tex;
  }


  protected Texture wrapFrontTexture() {
    Texture tex = new Texture();
    tex.init(pg.width, pg.height,
             glColorTex.get(frontTex), TEXTURE_2D, RGBA,
             fboWidth, fboHeight, NEAREST, NEAREST,
             CLAMP_TO_EDGE, CLAMP_TO_EDGE);
    tex.invertedY(true);
    tex.colorBuffer(true);
    return tex;
  }


  protected int getBackTextureName() {
    return glColorTex.get(backTex);
  }


  protected int getFrontTextureName() {
    return glColorTex.get(frontTex);
  }


  protected void bindFrontTexture() {
    if (!texturingIsEnabled(TEXTURE_2D)) {
      enableTexturing(TEXTURE_2D);
    }
    bindTexture(TEXTURE_2D, glColorTex.get(frontTex));
  }


  protected void unbindFrontTexture() {
    if (textureIsBound(TEXTURE_2D, glColorTex.get(frontTex))) {
      // We don't want to unbind another texture
      // that might be bound instead of this one.
      if (!texturingIsEnabled(TEXTURE_2D)) {
        enableTexturing(TEXTURE_2D);
        bindTexture(TEXTURE_2D, 0);
        disableTexturing(TEXTURE_2D);
      } else {
        bindTexture(TEXTURE_2D, 0);
      }
    }
  }


  protected void syncBackTexture() {
    if (1 < numSamples) {
      bindFramebuffer(READ_FRAMEBUFFER, glMultiFbo.get(0));
      bindFramebuffer(DRAW_FRAMEBUFFER, glColorFbo.get(0));
      blitFramebuffer(0, 0, fboWidth, fboHeight,
                      0, 0, fboWidth, fboHeight,
                      COLOR_BUFFER_BIT, NEAREST);
    }
  }


  protected int qualityToSamples(int quality) {
    if (quality <= 1) {
      return 1;
    } else {
      // Number of samples is always an even number:
      int n = 2 * (quality / 2);
      return n;
    }
  }


  ///////////////////////////////////////////////////////////

  // Frame rendering


  protected void beginDraw(boolean clear0) {
    if (fboLayerInUse(clear0)) {
      bindFramebuffer(FRAMEBUFFER, glColorFbo.get(0));
      framebufferTexture2D(FRAMEBUFFER, COLOR_ATTACHMENT0,
                           TEXTURE_2D, glColorTex.get(backTex), 0);

      if (1 < numSamples) {
        bindFramebuffer(FRAMEBUFFER, glMultiFbo.get(0));
      }

      if (firstFrame) {
        // No need to draw back color buffer because we are in the first frame.
        int argb = pg.backgroundColor;
        float a = ((argb >> 24) & 0xff) / 255.0f;
        float r = ((argb >> 16) & 0xff) / 255.0f;
        float g = ((argb >> 8) & 0xff) / 255.0f;
        float b = ((argb) & 0xff) / 255.0f;
        clearColor(r, g, b, a);
        clear(COLOR_BUFFER_BIT);
      } else if (!clear0) {
        // Render previous back texture (now is the front) as background,
        // because no background() is being used ("incremental drawing")
        drawTexture(TEXTURE_2D, glColorTex.get(frontTex),
                    fboWidth, fboHeight, 0, 0, pg.width, pg.height,
                                         0, 0, pg.width, pg.height);
      }

      fboLayerInUse = true;
    } else {
      fboLayerInUse = false;
    }

    if (firstFrame) {
      firstFrame = false;
    }

    if (!fboLayerByDefault) {
      // The result of this assignment is the following: if the user requested
      // at some point the use of the FBO layer, but subsequently didn't do
      // request it again, then the rendering won't use the FBO layer if not
      // needed, since it is slower than simple onscreen rendering.
      FORCE_SCREEN_FBO = false;
    }
  }


  protected void endDraw(boolean clear0) {
    if (fboLayerInUse) {
      syncBackTexture();

      // Draw the contents of the back texture to the screen framebuffer.
      bindFramebuffer(FRAMEBUFFER, 0);

      clearDepth(1);
      clearColor(0, 0, 0, 0);
      clear(COLOR_BUFFER_BIT | DEPTH_BUFFER_BIT);

      // Render current back texture to screen, without blending.
      disable(BLEND);
      drawTexture(TEXTURE_2D, glColorTex.get(backTex),
                  fboWidth, fboHeight, 0, 0, pg.width, pg.height,
                                       0, 0, pg.width, pg.height);

      // Swapping front and back textures.
      int temp = frontTex;
      frontTex = backTex;
      backTex = temp;
    }
    flush();
  }


  protected boolean canDraw() {
    return pg.initialized && pg.parent.isDisplayable();
  }


  protected void requestDraw() {
    if (pg.initialized) {
      glThread = Thread.currentThread();
      pg.parent.handleDraw();
      Display.update();
    }
  }


  protected boolean threadIsCurrent() {
    return Thread.currentThread() == glThread;
  }


  protected boolean fboLayerInUse(boolean clear0) {
    boolean cond = !clear0 || FORCE_SCREEN_FBO || 1 < numSamples;
    return cond && glColorFbo.get(0) != 0;
  }


  //////////////////////////////////////////////////////////////////////////////

  // Caps query


  public String getString(int name) {
    return GL11.glGetString(name);
  }


  public void getIntegerv(int name, IntBuffer values) {
    if (-1 < name) {
      GL11.glGetInteger(name, values);
    } else {
      fillIntBuffer(values, 0, values.capacity() - 1, 0);
    }
  }


  public void getFloatv(int name, FloatBuffer values) {
    if (-1 < name) {
      GL11.glGetFloat(name, values);
    } else {
      fillFloatBuffer(values, 0, values.capacity() - 1, 0);
    }
  }


  public void getBooleanv(int name, IntBuffer values) {
    if (-1 < name) {
      if (byteBuffer.capacity() < values.capacity()) {
        byteBuffer = allocateDirectByteBuffer(values.capacity());
      }
      GL11.glGetBoolean(name, byteBuffer);
      for (int i = 0; i < values.capacity(); i++) {
        values.put(i, byteBuffer.get(i));
      }
    } else {
      fillIntBuffer(values, 0, values.capacity() - 1, 0);
    }
  }


  ///////////////////////////////////////////////////////////

  // Enable/disable caps


  public void enable(int cap) {
    if (-1 < cap) {
      GL11.glEnable(cap);
    }
  }


  public void disable(int cap) {
    if (-1 < cap) {
      GL11.glDisable(cap);
    }
  }


  ///////////////////////////////////////////////////////////

  // Render control


  public void flush() {
    GL11.glFlush();
  }


  public void finish() {
    GL11.glFinish();
  }


  ///////////////////////////////////////////////////////////

  // Error handling


  public int getError() {
    return GL11.glGetError();
  }


  public String errorString(int err) {
    return glu.gluErrorString(err);
  }


  ///////////////////////////////////////////////////////////

  // Rendering options


  public void frontFace(int mode) {
    GL11.glFrontFace(mode);
  }


  public void cullFace(int mode) {
    GL11.glCullFace(mode);
  }


  public void depthMask(boolean flag) {
    GL11.glDepthMask(flag);
  }


  public void depthFunc(int func) {
    GL11.glDepthFunc(func);
  }


  ///////////////////////////////////////////////////////////

  // Textures


  public void genTextures(int n, IntBuffer ids) {
    GL11.glGenTextures(ids);
  }


  public void deleteTextures(int n, IntBuffer ids) {
    GL11.glDeleteTextures(ids);
  }


  public void activeTexture(int unit) {
    GL13.glActiveTexture(unit);
  }


  public void bindTexture(int target, int id) {
    GL11.glBindTexture(target, id);
    if (target == TEXTURE_2D) {
      boundTextures[0] = id;
    } else if (target == TEXTURE_RECTANGLE) {
      boundTextures[1] = id;
    }
  }


  public void texImage2D(int target, int level, int internalFormat,
                         int width, int height, int border, int format,
                         int type, Buffer data) {
    GL11.glTexImage2D(target, level, internalFormat,
                      width, height, border, format, type, (IntBuffer)data);
  }


  public void texSubImage2D(int target, int level, int xOffset, int yOffset,
                            int width, int height, int format,
                            int type, Buffer data) {
    GL11.glTexSubImage2D(target, level, xOffset, yOffset,
                       width, height, format, type, (IntBuffer)data);
  }


  public void texParameteri(int target, int param, int value) {
    GL11.glTexParameteri(target, param, value);
  }


  public void texParameterf(int target, int param, float value) {
    GL11.glTexParameterf(target, param, value);
  }


  public void getTexParameteriv(int target, int param, IntBuffer values) {
    GL11.glGetTexParameter(target, param, values);
  }


  public void generateMipmap(int target) {
    GL30.glGenerateMipmap(target);
  }


  ///////////////////////////////////////////////////////////

  // Vertex Buffers


  public void genBuffers(int n, IntBuffer ids) {
    GL15.glGenBuffers(ids);
  }


  public void deleteBuffers(int n, IntBuffer ids) {
    GL15.glDeleteBuffers(ids);
  }


  public void bindBuffer(int target, int id) {
    GL15.glBindBuffer(target, id);
  }


  public void bufferData(int target, int size, Buffer data, int usage) {
    if (data == null) {
      FloatBuffer empty = BufferUtils.createFloatBuffer(size);
      GL15.glBufferData(target, empty, usage);
    } else {
      if (data instanceof ByteBuffer) {
        GL15.glBufferData(target, (ByteBuffer)data, usage);
      } else if (data instanceof ShortBuffer) {
        GL15.glBufferData(target, (ShortBuffer)data, usage);
      } else if (data instanceof IntBuffer) {
        GL15.glBufferData(target, (IntBuffer)data, usage);
      } else if (data instanceof FloatBuffer) {
        GL15.glBufferData(target, (FloatBuffer)data, usage);
      }
    }
  }


  public void bufferSubData(int target, int offset, int size, Buffer data) {
    if (data instanceof ByteBuffer) {
      GL15.glBufferSubData(target, offset, (ByteBuffer)data);
    } else if (data instanceof ShortBuffer) {
      GL15.glBufferSubData(target, offset, (ShortBuffer)data);
    } else if (data instanceof IntBuffer) {
      GL15.glBufferSubData(target, offset, (IntBuffer)data);
    } else if (data instanceof FloatBuffer) {
      GL15.glBufferSubData(target, offset, (FloatBuffer)data);
    }
  }


  public void drawArrays(int mode, int first, int count) {
    GL11.glDrawArrays(mode, first, count);
  }


  public void drawElements(int mode, int count, int type, int offset) {
    GL11.glDrawElements(mode, count, type, offset);
  }


  public void enableVertexAttribArray(int loc) {
    GL20.glEnableVertexAttribArray(loc);
  }


  public void disableVertexAttribArray(int loc) {
    GL20.glDisableVertexAttribArray(loc);
  }


  public void vertexAttribPointer(int loc, int size, int type,
                                  boolean normalized, int stride, int offset) {
    GL20.glVertexAttribPointer(loc, size, type, normalized, stride, offset);
  }


  public void vertexAttribPointer(int loc, int size, int type,
                                  boolean normalized, int stride, Buffer data) {
    if (type == UNSIGNED_INT) {
      GL20.glVertexAttribPointer(loc, size, true, normalized, stride, (IntBuffer)data);
    } else if (type == UNSIGNED_BYTE) {
      GL20.glVertexAttribPointer(loc, size, true, normalized, stride, (ByteBuffer)data);
    } else if (type == UNSIGNED_SHORT) {
      GL20.glVertexAttribPointer(loc, size, true, normalized, stride, (ShortBuffer)data);
    } else if (type == FLOAT) {
      GL20.glVertexAttribPointer(loc, size, normalized, stride, (FloatBuffer)data);
    }
  }


  public ByteBuffer mapBuffer(int target, int access) {
    return GL15.glMapBuffer(target, access, null);
  }


  public ByteBuffer mapBufferRange(int target, int offset, int length,
                                   int access) {
    return GL30.glMapBufferRange(target, offset, length, access, null);
  }


  public void unmapBuffer(int target) {
    GL15.glUnmapBuffer(target);
  }


  ///////////////////////////////////////////////////////////

  // Framebuffers, renderbuffers


  public void genFramebuffers(int n, IntBuffer ids) {
    GL30.glGenFramebuffers(ids);
  }


  public void deleteFramebuffers(int n, IntBuffer ids) {
    GL30.glDeleteFramebuffers(ids);
  }


  public void genRenderbuffers(int n, IntBuffer ids) {
    GL30.glGenRenderbuffers(ids);
  }


  public void deleteRenderbuffers(int n, IntBuffer ids) {
    GL30.glDeleteRenderbuffers(ids);
  }


  public void bindFramebuffer(int target, int id) {
    GL30.glBindFramebuffer(target, id);
  }


  public void blitFramebuffer(int srcX0, int srcY0, int srcX1, int srcY1,
                              int dstX0, int dstY0, int dstX1, int dstY1,
                              int mask, int filter) {
    GL30.glBlitFramebuffer(srcX0, srcY0, srcX1, srcY1,
                           dstX0, dstY0, dstX1, dstY1, mask, filter);
  }


  public void framebufferTexture2D(int target, int attachment, int texTarget,
                                   int texId, int level) {
    GL30.glFramebufferTexture2D(target, attachment, texTarget, texId, level);
  }


  public void bindRenderbuffer(int target, int id) {
    GL30.glBindRenderbuffer(target, id);
  }


  public void renderbufferStorageMultisample(int target, int samples,
                                             int format, int width, int height){
    GL30.glRenderbufferStorageMultisample(target, samples, format,
                                          width, height);
  }


  public void renderbufferStorage(int target, int format,
                                  int width, int height) {
    GL30.glRenderbufferStorage(target, format, width, height);
  }


  public void framebufferRenderbuffer(int target, int attachment,
                                      int rendbufTarget, int rendbufId) {
    GL30.glFramebufferRenderbuffer(target, attachment, rendbufTarget, rendbufId);
  }


  public int checkFramebufferStatus(int target) {
    return GL30.glCheckFramebufferStatus(target);
  }


  ///////////////////////////////////////////////////////////

  // Shaders


  public int createProgram() {
    return GL20.glCreateProgram();
  }


  public void deleteProgram(int id) {
    GL20.glDeleteProgram(id);
  }


  public int createShader(int type) {
    return GL20.glCreateShader(type);
  }


  public void deleteShader(int id) {
    GL20.glDeleteShader(id);
  }


  public void linkProgram(int prog) {
    GL20.glLinkProgram(prog);
  }


  public void validateProgram(int prog) {
    GL20.glValidateProgram(prog);
  }


  public void useProgram(int prog) {
    GL20.glUseProgram(prog);
  }


  public int getAttribLocation(int prog, String name) {
    return GL20.glGetAttribLocation(prog, name);
  }


  public int getUniformLocation(int prog, String name) {
    return GL20.glGetUniformLocation(prog, name);
  }


  public void uniform1i(int loc, int value) {
    GL20.glUniform1i(loc, value);
  }


  public void uniform2i(int loc, int value0, int value1) {
    GL20.glUniform2i(loc, value0, value1);
  }


  public void uniform3i(int loc, int value0, int value1, int value2) {
    GL20.glUniform3i(loc, value0, value1, value2);
  }


  public void uniform4i(int loc, int value0, int value1, int value2,
                                 int value3) {
    GL20.glUniform4i(loc, value0, value1, value2, value3);
  }


  public void uniform1f(int loc, float value) {
    GL20.glUniform1f(loc, value);
  }


  public void uniform2f(int loc, float value0, float value1) {
    GL20.glUniform2f(loc, value0, value1);
  }


  public void uniform3f(int loc, float value0, float value1, float value2) {
    GL20.glUniform3f(loc, value0, value1, value2);
  }


  public void uniform4f(int loc, float value0, float value1, float value2,
                                 float value3) {
    GL20.glUniform4f(loc, value0, value1, value2, value3);
  }


  public void uniform1iv(int loc, int count, IntBuffer v) {
    v.limit(count);
    GL20.glUniform1(loc, v);
    v.clear();
  }


  public void uniform2iv(int loc, int count, IntBuffer v) {
    v.limit(2 * count);
    GL20.glUniform2(loc, v);
    v.clear();
  }


  public void uniform3iv(int loc, int count, IntBuffer v) {
    v.limit(3 * count);
    GL20.glUniform3(loc, v);
    v.clear();
  }


  public void uniform4iv(int loc, int count, IntBuffer v) {
    v.limit(4 * count);
    GL20.glUniform4(loc, v);
    v.clear();
  }


  public void uniform1fv(int loc, int count, FloatBuffer v) {
    v.limit(count);
    GL20.glUniform1(loc, v);
    v.clear();
  }


  public void uniform2fv(int loc, int count, FloatBuffer v) {
    v.limit(2 * count);
    GL20.glUniform2(loc, v);
    v.clear();
  }


  public void uniform3fv(int loc, int count, FloatBuffer v) {
    v.limit(3 * count);
    GL20.glUniform3(loc, v);
    v.clear();
  }


  public void uniform4fv(int loc, int count, FloatBuffer v) {
    v.limit(4 * count);
    GL20.glUniform4(loc, v);
    v.clear();
  }


  public void uniformMatrix2fv(int loc, int count, boolean transpose,
                               FloatBuffer mat) {
    mat.limit(4);
    GL20.glUniformMatrix2(loc, transpose, mat);
    mat.clear();
  }


  public void uniformMatrix3fv(int loc, int count, boolean transpose,
                               FloatBuffer mat) {
    mat.limit(9);
    GL20.glUniformMatrix3(loc, transpose, mat);
    mat.clear();
  }


  public void uniformMatrix4fv(int loc, int count, boolean transpose,
                               FloatBuffer mat) {
    mat.limit(16);
    GL20.glUniformMatrix4(loc, transpose, mat);
    mat.clear();
  }


  public void vertexAttrib1f(int loc, float value) {
    GL20.glVertexAttrib1f(loc, value);
  }


  public void vertexAttrib2f(int loc, float value0, float value1) {
    GL20.glVertexAttrib2f(loc, value0, value1);
  }


  public void vertexAttrib3f(int loc, float value0, float value1, float value2){
    GL20.glVertexAttrib3f(loc, value0, value1, value2);
  }


  public void vertexAttrib4f(int loc, float value0, float value1, float value2,
                                      float value3) {
    GL20.glVertexAttrib4f(loc, value0, value1, value2, value3);
  }


  public void shaderSource(int id, String source) {
    GL20.glShaderSource(id, source);
  }


  public void compileShader(int id) {
    GL20.glCompileShader(id);
  }


  public void attachShader(int prog, int shader) {
    GL20.glAttachShader(prog, shader);
  }


  public void getShaderiv(int shader, int pname, IntBuffer params) {
    GL20.glGetShader(shader, pname, params);
  }


  public String getShaderInfoLog(int shader) {
    int len = GL20.glGetShaderi(shader, GL20.GL_INFO_LOG_LENGTH);
    return GL20.glGetShaderInfoLog(shader, len);
  }


  public void getProgramiv(int prog, int pname, IntBuffer params) {
    GL20.glGetProgram(prog, pname, params);
  }


  public String getProgramInfoLog(int prog) {
    int len = GL20.glGetProgrami(prog, GL20.GL_INFO_LOG_LENGTH);
    return GL20.glGetProgramInfoLog(prog, len);
  }


  ///////////////////////////////////////////////////////////

  // Viewport


  public void viewport(int x, int y, int width, int height) {
    GL11.glViewport(x, y, width, height);
  }


  ///////////////////////////////////////////////////////////

  // Clipping (scissor test)


  public void scissor(int x, int y, int w, int h) {
    GL11.glScissor(x, y, w, h);
  }


  ///////////////////////////////////////////////////////////

  // Blending


  public void blendEquation(int eq) {
    GL14.glBlendEquation(eq);
  }


  public void blendFunc(int srcFactor, int dstFactor) {
    GL11.glBlendFunc(srcFactor, dstFactor);
  }


  ///////////////////////////////////////////////////////////

  // Pixels


  public void readBuffer(int buf) {
    GL11.glReadBuffer(buf);
  }


  public void readPixels(int x, int y, int width, int height, int format,
                         int type, Buffer buffer) {

    GL11.glReadPixels(x, y, width, height, format, type, (IntBuffer)buffer);
  }


  public void drawBuffer(int buf) {
    GL11.glDrawBuffer(buf);
  }


  public void clearDepth(float d) {
    GL11.glClearDepth(d);
  }


  public void clearStencil(int s) {
    GL11.glClearStencil(s);
  }


  public void colorMask(boolean wr, boolean wg, boolean wb, boolean wa) {
    GL11.glColorMask(wr, wg, wb, wa);
  }


  public void clearColor(float r, float g, float b, float a) {
    GL11.glClearColor(r, g, b, a);
  }


  public void clear(int mask) {
    GL11.glClear(mask);
  }


  ///////////////////////////////////////////////////////////

  // Context interface


  protected int createEmptyContext() {
    return -1;
  }


  protected int getCurrentContext() {
    return context;
  }


  ///////////////////////////////////////////////////////////

  // Tessellator interface


  protected Tessellator createTessellator(TessellatorCallback callback) {
    return new Tessellator(callback);
  }


  protected class Tessellator {
    protected GLUtessellator tess;
    protected TessellatorCallback callback;
    protected GLUCallback gluCallback;

    public Tessellator(TessellatorCallback callback) {
      this.callback = callback;
      tess = GLU.gluNewTess();
      gluCallback = new GLUCallback();

      tess.gluTessCallback(GLU.GLU_TESS_BEGIN, gluCallback);
      tess.gluTessCallback(GLU.GLU_TESS_END, gluCallback);
      tess.gluTessCallback(GLU.GLU_TESS_VERTEX, gluCallback);
      tess.gluTessCallback(GLU.GLU_TESS_COMBINE, gluCallback);
      tess.gluTessCallback(GLU.GLU_TESS_ERROR, gluCallback);
    }

    public void beginPolygon() {
      tess.gluTessBeginPolygon(null);
    }

    public void endPolygon() {
      tess.gluTessEndPolygon();
    }

    public void setWindingRule(int rule) {
      tess.gluTessProperty(GLU.GLU_TESS_WINDING_RULE, rule);
    }

    public void beginContour() {
      tess.gluTessBeginContour();
    }

    public void endContour() {
      tess.gluTessEndContour();
    }

    public void addVertex(double[] v) {
      tess.gluTessVertex(v, 0, v);
    }

    protected class GLUCallback extends GLUtessellatorCallbackAdapter {
      @Override
      public void begin(int type) {
        callback.begin(type);
      }

      @Override
      public void end() {
        callback.end();
      }

      @Override
      public void vertex(Object data) {
        callback.vertex(data);
      }

      @Override
      public void combine(double[] coords, Object[] data,
                          float[] weight, Object[] outData) {
        callback.combine(coords, data, weight, outData);
      }

      @Override
      public void error(int errnum) {
        callback.error(errnum);
      }
    }
  }


  protected String tessError(int err) {
    return glu.gluErrorString(err);
  }

  protected interface TessellatorCallback  {
    public void begin(int type);
    public void end();
    public void vertex(Object data);
    public void combine(double[] coords, Object[] data,
                        float[] weight, Object[] outData);
    public void error(int errnum);
  }


  ///////////////////////////////////////////////////////////

  // Utility functions


  protected boolean contextIsCurrent(int other) {
    return other == -1 || other == context;
  }


  protected void enableTexturing(int target) {
    enable(target);
    if (target == TEXTURE_2D) {
      texturingTargets[0] = true;
    } else if (target == TEXTURE_RECTANGLE) {
      texturingTargets[1] = true;
    }
  }


  protected void disableTexturing(int target) {
    disable(target);
    if (target == TEXTURE_2D) {
      texturingTargets[0] = false;
    } else if (target == TEXTURE_RECTANGLE) {
      texturingTargets[1] = false;
    }
  }


  protected boolean texturingIsEnabled(int target) {
    if (target == TEXTURE_2D) {
      return texturingTargets[0];
    } else if (target == TEXTURE_RECTANGLE) {
      return texturingTargets[1];
    } else {
      return false;
    }
  }


  protected boolean textureIsBound(int target, int id) {
    if (target == TEXTURE_2D) {
      return boundTextures[0] == id;
    } else if (target == TEXTURE_RECTANGLE) {
      return boundTextures[1] == id;
    } else {
      return false;
    }
  }


  protected void initTexture(int target, int format, int width, int height) {
    initTexture(target, format, width, height, 0);
  }


  protected void initTexture(int target, int format, int width, int height,
                             int initColor) {
    int[] glcolor = new int[16 * 16];
    Arrays.fill(glcolor, javaToNativeARGB(initColor));
    IntBuffer texels = PGL.allocateDirectIntBuffer(16 * 16);
    texels.put(glcolor);
    texels.rewind();
    for (int y = 0; y < height; y += 16) {
      int h = PApplet.min(16, height - y);
      for (int x = 0; x < width; x += 16) {
        int w = PApplet.min(16, width - x);
        texSubImage2D(target, 0, x, y, w, h, format, UNSIGNED_BYTE, texels);
      }
    }
  }


  protected void copyToTexture(int target, int format, int id, int x, int y,
                               int w, int h, IntBuffer buffer) {
    activeTexture(TEXTURE0);
    boolean enabledTex = false;
    if (!texturingIsEnabled(target)) {
      enableTexturing(target);
      enabledTex = true;
    }
    bindTexture(target, id);
    texSubImage2D(target, 0, x, y, w, h, format, UNSIGNED_BYTE, buffer);
    bindTexture(target, 0);
    if (enabledTex) {
      disableTexturing(target);
    }
  }


  protected void drawTexture(int target, int id, int width, int height,
                             int X0, int Y0, int X1, int Y1) {
    drawTexture(target, id, width, height, X0, Y0, X1, Y1, X0, Y0, X1, Y1);
  }


  protected void drawTexture(int target, int id, int width, int height,
                             int texX0, int texY0, int texX1, int texY1,
                             int scrX0, int scrY0, int scrX1, int scrY1) {
    if (target == TEXTURE_2D) {
      drawTexture2D(id, width, height,
                    texX0, texY0, texX1, texY1,
                    scrX0, scrY0, scrX1, scrY1);
    } else if (target == TEXTURE_RECTANGLE) {
      drawTextureRect(id, width, height,
                      texX0, texY0, texX1, texY1,
                      scrX0, scrY0, scrX1, scrY1);
    }
  }


  protected void drawTexture2D(int id, int width, int height,
                               int texX0, int texY0, int texX1, int texY1,
                               int scrX0, int scrY0, int scrX1, int scrY1) {
    if (!loadedTex2DShader || tex2DShaderContext != context) {
      tex2DVertShader = createShader(VERTEX_SHADER, texVertShaderSource);
      tex2DFragShader = createShader(FRAGMENT_SHADER, tex2DFragShaderSource);
      if (0 < tex2DVertShader && 0 < tex2DFragShader) {
        tex2DShaderProgram = createProgram(tex2DVertShader, tex2DFragShader);
      }
      if (0 < tex2DShaderProgram) {
        tex2DVertLoc = getAttribLocation(tex2DShaderProgram, "inVertex");
        tex2DTCoordLoc = getAttribLocation(tex2DShaderProgram, "inTexcoord");
      }
      loadedTex2DShader = true;
      tex2DShaderContext = context;
    }

    if (texData == null) {
      texData = allocateDirectFloatBuffer(texCoords.length);
    }

    if (0 < tex2DShaderProgram) {
      // The texture overwrites anything drawn earlier.
      boolean depthTest = getDepthTest();
      disable(DEPTH_TEST);

      // When drawing the texture we don't write to the
      // depth mask, so the texture remains in the background
      // and can be occluded by anything drawn later, even if
      // if it is behind it.
      boolean depthMask = getDepthWriteMask();
      depthMask(false);

      useProgram(tex2DShaderProgram);

      enableVertexAttribArray(tex2DVertLoc);
      enableVertexAttribArray(tex2DTCoordLoc);

      // Vertex coordinates of the textured quad are specified
      // in normalized screen space (-1, 1):
      // Corner 1
      texCoords[ 0] = 2 * (float)scrX0 / pg.width - 1;
      texCoords[ 1] = 2 * (float)scrY0 / pg.height - 1;
      texCoords[ 2] = (float)texX0 / width;
      texCoords[ 3] = (float)texY0 / height;
      // Corner 2
      texCoords[ 4] = 2 * (float)scrX1 / pg.width - 1;
      texCoords[ 5] = 2 * (float)scrY0 / pg.height - 1;
      texCoords[ 6] = (float)texX1 / width;
      texCoords[ 7] = (float)texY0 / height;
      // Corner 3
      texCoords[ 8] = 2 * (float)scrX0 / pg.width - 1;
      texCoords[ 9] = 2 * (float)scrY1 / pg.height - 1;
      texCoords[10] = (float)texX0 / width;
      texCoords[11] = (float)texY1 / height;
      // Corner 4
      texCoords[12] = 2 * (float)scrX1 / pg.width - 1;
      texCoords[13] = 2 * (float)scrY1 / pg.height - 1;
      texCoords[14] = (float)texX1 / width;
      texCoords[15] = (float)texY1 / height;

      texData.rewind();
      texData.put(texCoords);

      activeTexture(TEXTURE0);
      boolean enabledTex = false;
      if (!texturingIsEnabled(TEXTURE_2D)) {
        enableTexturing(TEXTURE_2D);
        enabledTex = true;
      }
      bindTexture(TEXTURE_2D, id);

      bindBuffer(ARRAY_BUFFER, 0); // Making sure that no VBO is bound at this point.

      texData.position(0);
      vertexAttribPointer(tex2DVertLoc, 2, FLOAT, false, 4 * SIZEOF_FLOAT,
                          texData);
      texData.position(2);
      vertexAttribPointer(tex2DTCoordLoc, 2, FLOAT, false, 4 * SIZEOF_FLOAT,
                          texData);

      drawArrays(TRIANGLE_STRIP, 0, 4);

      bindTexture(TEXTURE_2D, 0);
      if (enabledTex) {
        disableTexturing(TEXTURE_2D);
      }

      disableVertexAttribArray(tex2DVertLoc);
      disableVertexAttribArray(tex2DTCoordLoc);

      useProgram(0);

      if (depthTest) {
        enable(DEPTH_TEST);
      } else {
        disable(DEPTH_TEST);
      }
      depthMask(depthMask);
    }
  }


  protected void drawTextureRect(int id, int width, int height,
                                 int texX0, int texY0, int texX1, int texY1,
                                 int scrX0, int scrY0, int scrX1, int scrY1) {
    if (!loadedTexRectShader || texRectShaderContext != context) {
      texRectVertShader = createShader(VERTEX_SHADER, texVertShaderSource);
      texRectFragShader = createShader(FRAGMENT_SHADER, texRectFragShaderSource);
      if (0 < texRectVertShader && 0 < texRectFragShader) {
        texRectShaderProgram = createProgram(texRectVertShader,
                                             texRectFragShader);
      }
      if (0 < texRectShaderProgram) {
        texRectVertLoc = getAttribLocation(texRectShaderProgram, "inVertex");
        texRectTCoordLoc = getAttribLocation(texRectShaderProgram, "inTexcoord");
      }
      loadedTexRectShader = true;
      texRectShaderContext = context;
    }

    if (texData == null) {
      texData = allocateDirectFloatBuffer(texCoords.length);
    }

    if (0 < texRectShaderProgram) {
      // The texture overwrites anything drawn earlier.
      boolean depthTest = getDepthTest();
      disable(DEPTH_TEST);

      // When drawing the texture we don't write to the
      // depth mask, so the texture remains in the background
      // and can be occluded by anything drawn later, even if
      // if it is behind it.
      boolean depthMask = getDepthWriteMask();
      depthMask(false);

      useProgram(texRectShaderProgram);

      enableVertexAttribArray(texRectVertLoc);
      enableVertexAttribArray(texRectTCoordLoc);

      // Vertex coordinates of the textured quad are specified
      // in normalized screen space (-1, 1):
      // Corner 1
      texCoords[ 0] = 2 * (float)scrX0 / pg.width - 1;
      texCoords[ 1] = 2 * (float)scrY0 / pg.height - 1;
      texCoords[ 2] = texX0;
      texCoords[ 3] = texY0;
      // Corner 2
      texCoords[ 4] = 2 * (float)scrX1 / pg.width - 1;
      texCoords[ 5] = 2 * (float)scrY0 / pg.height - 1;
      texCoords[ 6] = texX1;
      texCoords[ 7] = texY0;
      // Corner 3
      texCoords[ 8] = 2 * (float)scrX0 / pg.width - 1;
      texCoords[ 9] = 2 * (float)scrY1 / pg.height - 1;
      texCoords[10] = texX0;
      texCoords[11] = texY1;
      // Corner 4
      texCoords[12] = 2 * (float)scrX1 / pg.width - 1;
      texCoords[13] = 2 * (float)scrY1 / pg.height - 1;
      texCoords[14] = texX1;
      texCoords[15] = texY1;

      texData.rewind();
      texData.put(texCoords);

      activeTexture(TEXTURE0);
      boolean enabledTex = false;
      if (!texturingIsEnabled(TEXTURE_RECTANGLE)) {
        enableTexturing(TEXTURE_RECTANGLE);
        enabledTex = true;
      }
      bindTexture(TEXTURE_RECTANGLE, id);

      bindBuffer(ARRAY_BUFFER, 0); // Making sure that no VBO is bound at this point.

      texData.position(0);
      vertexAttribPointer(texRectVertLoc, 2, FLOAT, false, 4 * SIZEOF_FLOAT,
                          texData);
      texData.position(2);
      vertexAttribPointer(texRectTCoordLoc, 2, FLOAT, false, 4 * SIZEOF_FLOAT,
                          texData);

      drawArrays(TRIANGLE_STRIP, 0, 4);

      bindTexture(TEXTURE_RECTANGLE, 0);
      if (enabledTex) {
        disableTexturing(TEXTURE_RECTANGLE);
      }

      disableVertexAttribArray(texRectVertLoc);
      disableVertexAttribArray(texRectTCoordLoc);

      useProgram(0);

      if (depthTest) {
        enable(DEPTH_TEST);
      } else {
        disable(DEPTH_TEST);
      }
      depthMask(depthMask);
    }
  }


  protected int getColorValue(int scrX, int scrY) {
    if (colorBuffer == null) {
      colorBuffer = IntBuffer.allocate(1);
    }
    colorBuffer.rewind();
    readPixels(scrX, pg.height - scrY - 1, 1, 1, RGBA, UNSIGNED_BYTE,
               colorBuffer);
    return colorBuffer.get();
  }


  protected float getDepthValue(int scrX, int scrY) {
    if (depthBuffer == null) {
      depthBuffer = FloatBuffer.allocate(1);
    }
    depthBuffer.rewind();
    readPixels(scrX, pg.height - scrY - 1, 1, 1, DEPTH_COMPONENT, FLOAT,
               depthBuffer);
    return depthBuffer.get(0);
  }


  protected byte getStencilValue(int scrX, int scrY) {
    if (stencilBuffer == null) {
      stencilBuffer = ByteBuffer.allocate(1);
    }
    readPixels(scrX, pg.height - scrY - 1, 1, 1, STENCIL_INDEX,
               UNSIGNED_BYTE, stencilBuffer);
    return stencilBuffer.get(0);
  }


  // bit shifting this might be more efficient
  protected static int nextPowerOfTwo(int val) {
    int ret = 1;
    while (ret < val) {
      ret <<= 1;
    }
    return ret;
  }


  /**
   * Converts input native OpenGL value (RGBA on big endian, ABGR on little
   * endian) to Java ARGB.
   */
  protected static int nativeToJavaARGB(int color) {
    if (BIG_ENDIAN) { // RGBA to ARGB
      return (color & 0xff000000) |
             ((color >> 8) & 0x00ffffff);
    } else { // ABGR to ARGB
      return (color & 0xff000000) |
             ((color << 16) & 0xff0000) |
             (color & 0xff00) |
             ((color >> 16) & 0xff);
    }
  }


  /**
   * Converts input array of native OpenGL values (RGBA on big endian, ABGR on
   * little endian) representing an image of width x height resolution to Java
   * ARGB. It also rearranges the elements in the array so that the image is
   * flipped vertically.
   */
  protected static void nativeToJavaARGB(int[] pixels, int width, int height) {
    int index = 0;
    int yindex = (height - 1) * width;
    for (int y = 0; y < height / 2; y++) {
      if (BIG_ENDIAN) { // RGBA to ARGB
        for (int x = 0; x < width; x++) {
          int temp = pixels[index];
          pixels[index] = (pixels[yindex] & 0xff000000) |
                          ((pixels[yindex] >> 8) & 0x00ffffff);
          pixels[yindex] = (temp & 0xff000000) |
                           ((temp >> 8) & 0x00ffffff);
          index++;
          yindex++;
        }
      } else { // ABGR to ARGB
        for (int x = 0; x < width; x++) {
          int temp = pixels[index];
          pixels[index] = (pixels[yindex] & 0xff000000) |
                          ((pixels[yindex] << 16) & 0xff0000) |
                          (pixels[yindex] & 0xff00) |
                          ((pixels[yindex] >> 16) & 0xff);
          pixels[yindex] = (temp & 0xff000000) |
                           ((temp << 16) & 0xff0000) |
                           (temp & 0xff00) |
                           ((temp >> 16) & 0xff);
          index++;
          yindex++;
        }
      }
      yindex -= width * 2;
    }

    // Flips image
    if ((height % 2) == 1) {
      index = (height / 2) * width;
      if (BIG_ENDIAN) { // RGBA to ARGB
        for (int x = 0; x < width; x++) {
          pixels[index] = (pixels[index] & 0xff000000) |
                          ((pixels[index] >> 8) & 0x00ffffff);
          index++;
        }
      } else { // ABGR to ARGB
        for (int x = 0; x < width; x++) {
          pixels[index] = (pixels[index] & 0xff000000) |
                          ((pixels[index] << 16) & 0xff0000) |
                          (pixels[index] & 0xff00) |
                          ((pixels[index] >> 16) & 0xff);
          index++;
        }
      }
    }
  }


  /**
   * Converts input native OpenGL value (RGBA on big endian, ABGR on little
   * endian) to Java RGB, so that the alpha component of the result is set
   * to opaque (255).
   */
  protected static int nativeToJavaRGB(int color) {
    if (BIG_ENDIAN) { // RGBA to ARGB
      return ((color << 8) & 0xffffff00) | 0xff;
    } else { // ABGR to ARGB
       return 0xff000000 | ((color << 16) & 0xff0000) |
                           (color & 0xff00) |
                           ((color >> 16) & 0xff);
    }
  }


  /**
   * Converts input array of native OpenGL values (RGBA on big endian, ABGR on
   * little endian) representing an image of width x height resolution to Java
   * RGB, so that the alpha component of all pixels is set to opaque (255). It
   * also rearranges the elements in the array so that the image is flipped
   * vertically.
   */
  protected static void nativeToJavaRGB(int[] pixels, int width, int height) {
    int index = 0;
    int yindex = (height - 1) * width;
    for (int y = 0; y < height / 2; y++) {
      if (BIG_ENDIAN) { // RGBA to ARGB
        for (int x = 0; x < width; x++) {
          int temp = pixels[index];
          pixels[index] = 0xff000000 | ((pixels[yindex] >> 8) & 0x00ffffff);
          pixels[yindex] = 0xff000000 | ((temp >> 8) & 0x00ffffff);
          index++;
          yindex++;
        }
      } else { // ABGR to ARGB
        for (int x = 0; x < width; x++) {
          int temp = pixels[index];
          pixels[index] = 0xff000000 | ((pixels[yindex] << 16) & 0xff0000) |
                                       (pixels[yindex] & 0xff00) |
                                       ((pixels[yindex] >> 16) & 0xff);
          pixels[yindex] = 0xff000000 | ((temp << 16) & 0xff0000) |
                                        (temp & 0xff00) |
                                        ((temp >> 16) & 0xff);
          index++;
          yindex++;
        }
      }
      yindex -= width * 2;
    }

    // Flips image
    if ((height % 2) == 1) {
      index = (height / 2) * width;
      if (BIG_ENDIAN) { // RGBA to ARGB
        for (int x = 0; x < width; x++) {
          pixels[index] = 0xff000000 | ((pixels[index] >> 8) & 0x00ffffff);
          index++;
        }
      } else { // ABGR to ARGB
        for (int x = 0; x < width; x++) {
          pixels[index] = 0xff000000 | ((pixels[index] << 16) & 0xff0000) |
                                       (pixels[index] & 0xff00) |
                                       ((pixels[index] >> 16) & 0xff);
          index++;
        }
      }
    }
  }


  /**
   * Converts input Java ARGB value to native OpenGL format (RGBA on big endian,
   * BGRA on little endian).
   */
  protected static int javaToNativeARGB(int color) {
    if (BIG_ENDIAN) { // ARGB to RGBA
      return ((color >> 24) & 0xff) |
             ((color << 8) & 0xffffff00);
    } else { // ARGB to ABGR
      return (color & 0xff000000) |
             ((color << 16) & 0xff0000) |
             (color & 0xff00) |
             ((color >> 16) & 0xff);
    }
  }


  /**
   * Converts input array of Java ARGB values representing an image of width x
   * height resolution to native OpenGL format (RGBA on big endian, BGRA on
   * little endian). It also rearranges the elements in the array so that the
   * image is flipped vertically.
   */
  protected static void javaToNativeARGB(int[] pixels, int width, int height) {
    int index = 0;
    int yindex = (height - 1) * width;
    for (int y = 0; y < height / 2; y++) {
      if (BIG_ENDIAN) { // ARGB to RGBA
        for (int x = 0; x < width; x++) {
          int temp = pixels[index];
          pixels[index] = ((pixels[yindex] >> 24) & 0xff) |
                          ((pixels[yindex] << 8) & 0xffffff00);
          pixels[yindex] = ((temp >> 24) & 0xff) |
                           ((temp << 8) & 0xffffff00);
          index++;
          yindex++;
        }

      } else { // ARGB to ABGR
        for (int x = 0; x < width; x++) {
          int temp = pixels[index];
          pixels[index] = (pixels[yindex] & 0xff000000) |
                          ((pixels[yindex] << 16) & 0xff0000) |
                          (pixels[yindex] & 0xff00) |
                          ((pixels[yindex] >> 16) & 0xff);
          pixels[yindex] = (pixels[yindex] & 0xff000000) |
                           ((temp << 16) & 0xff0000) |
                           (temp & 0xff00) |
                           ((temp >> 16) & 0xff);
          index++;
          yindex++;
        }
      }
      yindex -= width * 2;
    }

    // Flips image
    if ((height % 2) == 1) {
      index = (height / 2) * width;
      if (BIG_ENDIAN) { // ARGB to RGBA
        for (int x = 0; x < width; x++) {
          pixels[index] = ((pixels[index] >> 24) & 0xff) |
                          ((pixels[index] << 8) & 0xffffff00);
          index++;
        }
      } else { // ARGB to ABGR
        for (int x = 0; x < width; x++) {
          pixels[index] = (pixels[index] & 0xff000000) |
                          ((pixels[index] << 16) & 0xff0000) |
                          (pixels[index] & 0xff00) |
                          ((pixels[index] >> 16) & 0xff);
          index++;
        }
      }
    }
  }


  /**
   * Converts input Java ARGB value to native OpenGL format (RGBA on big endian,
   * BGRA on little endian), setting alpha component to opaque (255).
   */
  protected static int javaToNativeRGB(int color) {
    if (BIG_ENDIAN) { // ARGB to RGBA
        return ((color << 8) & 0xffffff00) | 0xff;
    } else { // ARGB to ABGR
        return 0xff000000 | ((color << 16) & 0xff0000) |
                            (color & 0xff00) |
                            ((color >> 16) & 0xff);
    }
  }


  /**
   * Converts input array of Java ARGB values representing an image of width x
   * height resolution to native OpenGL format (RGBA on big endian, BGRA on
   * little endian), while setting alpha component of all pixels to opaque
   * (255). It also rearranges the elements in the array so that the image is
   * flipped vertically.
   */
  protected static void javaToNativeRGB(int[] pixels, int width, int height) {
    int index = 0;
    int yindex = (height - 1) * width;
    for (int y = 0; y < height / 2; y++) {
      if (BIG_ENDIAN) { // ARGB to RGBA
        for (int x = 0; x < width; x++) {
          int temp = pixels[index];
          pixels[index] = ((pixels[yindex] << 8) & 0xffffff00) | 0xff;
          pixels[yindex] = ((temp << 8) & 0xffffff00) | 0xff;
          index++;
          yindex++;
        }

      } else {
        for (int x = 0; x < width; x++) { // ARGB to ABGR
          int temp = pixels[index];
          pixels[index] = 0xff000000 | ((pixels[yindex] << 16) & 0xff0000) |
                                       (pixels[yindex] & 0xff00) |
                                       ((pixels[yindex] >> 16) & 0xff);
          pixels[yindex] = 0xff000000 | ((temp << 16) & 0xff0000) |
                                        (temp & 0xff00) |
                                        ((temp >> 16) & 0xff);
          index++;
          yindex++;
        }
      }
      yindex -= width * 2;
    }

    // Flips image
    if ((height % 2) == 1) { // ARGB to RGBA
      index = (height / 2) * width;
      if (BIG_ENDIAN) {
        for (int x = 0; x < width; x++) {
          pixels[index] = ((pixels[index] << 8) & 0xffffff00) | 0xff;
          index++;
        }
      } else { // ARGB to ABGR
        for (int x = 0; x < width; x++) {
          pixels[index] = 0xff000000 | ((pixels[index] << 16) & 0xff0000) |
                                       (pixels[index] & 0xff00) |
                                       ((pixels[index] >> 16) & 0xff);
          index++;
        }
      }
    }
  }


  protected int createShader(int shaderType, String source) {
    int shader = createShader(shaderType);
    if (shader != 0) {
      shaderSource(shader, source);
      compileShader(shader);
      if (!compiled(shader)) {
        System.err.println("Could not compile shader " + shaderType + ":");
        System.err.println(getShaderInfoLog(shader));
        deleteShader(shader);
        shader = 0;
      }
    }
    return shader;
  }


  protected int createProgram(int vertexShader, int fragmentShader) {
    int program = createProgram();
    if (program != 0) {
      attachShader(program, vertexShader);
      attachShader(program, fragmentShader);
      linkProgram(program);
      if (!linked(program)) {
        System.err.println("Could not link program: ");
        System.err.println(getProgramInfoLog(program));
        deleteProgram(program);
        program = 0;
      }
    }
    return program;
  }


  protected boolean compiled(int shader) {
    intBuffer.rewind();
    getShaderiv(shader, COMPILE_STATUS, intBuffer);
    return intBuffer.get(0) == 0 ? false : true;
  }


  protected boolean linked(int program) {
    intBuffer.rewind();
    getProgramiv(program, LINK_STATUS, intBuffer);
    return intBuffer.get(0) == 0 ? false : true;
  }


  protected boolean validateFramebuffer() {
    int status = checkFramebufferStatus(FRAMEBUFFER);
    if (status == FRAMEBUFFER_COMPLETE) {
      return true;
    } else if (status == FRAMEBUFFER_INCOMPLETE_ATTACHMENT) {
      throw new RuntimeException(
        "GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT (" +
        Integer.toHexString(status) + ")");
    } else if (status == FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT) {
      throw new RuntimeException(
        "GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT (" +
        Integer.toHexString(status) + ")");
    } else if (status == FRAMEBUFFER_INCOMPLETE_DIMENSIONS) {
      throw new RuntimeException("GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS (" +
                                 Integer.toHexString(status) + ")");
    } else if (status == FRAMEBUFFER_INCOMPLETE_FORMATS) {
      throw new RuntimeException("GL_FRAMEBUFFER_INCOMPLETE_FORMATS (" +
                                 Integer.toHexString(status) + ")");
    } else if (status == FRAMEBUFFER_UNSUPPORTED) {
      throw new RuntimeException("GL_FRAMEBUFFER_UNSUPPORTED" +
                                 Integer.toHexString(status));
    } else {
      throw new RuntimeException("unknown framebuffer error (" +
                                 Integer.toHexString(status) + ")");
    }
  }


  protected int[] getGLVersion() {
    String version = getString(VERSION).trim();
    int[] res = {0, 0, 0};
    String[] parts = version.split(" ");
    for (int i = 0; i < parts.length; i++) {
      if (0 < parts[i].indexOf(".")) {
        String nums[] = parts[i].split("\\.");
        try {
          res[0] = Integer.parseInt(nums[0]);
        } catch (NumberFormatException e) { }
        if (1 < nums.length) {
          try {
            res[1] = Integer.parseInt(nums[1]);
          } catch (NumberFormatException e) { }
        }
        if (2 < nums.length) {
          try {
            res[2] = Integer.parseInt(nums[2]);
          } catch (NumberFormatException e) { }
        }
        break;
      }
    }
    return res;
  }

  
  protected boolean hasFBOs() {
    int major = getGLVersion()[0];
    if (major < 2) {
      String ext = getString(EXTENSIONS);
      return ext.indexOf("_framebuffer_object")  != -1;
    } 
    return true; // Assuming FBOs are always available for OpenGL >= 2.0
  }
  
  
  protected boolean hasShaders() {
    // GLSL might still be available through extensions. For instance,
    // GLContext.hasGLSL() gives false for older intel integrated chipsets on
    // OSX, where OpenGL is 1.4 but shaders are available.
    int major = getGLVersion()[0];
    if (major < 2) {
      String ext = getString(EXTENSIONS);
      return ext.indexOf("_fragment_shader")  != -1 &&
             ext.indexOf("_vertex_shader")    != -1 &&
             ext.indexOf("_shader_objects")   != -1 &&
             ext.indexOf("_shading_language") != -1;
    } 
    return true; // Assuming shaders are always available for OpenGL >= 2.0
  }  

  
  protected static ByteBuffer allocateDirectByteBuffer(int size) {
    int bytes = PApplet.max(MIN_DIRECT_BUFFER_SIZE, size) * SIZEOF_BYTE;
    return ByteBuffer.allocateDirect(bytes).order(ByteOrder.nativeOrder());
  }


  protected static ByteBuffer allocateByteBuffer(int size) {
    if (USE_DIRECT_BUFFERS) {
      return allocateDirectByteBuffer(size);
    } else {
      return ByteBuffer.allocate(size);
    }
  }


  protected static ByteBuffer allocateByteBuffer(byte[] arr) {
    if (USE_DIRECT_BUFFERS) {
      return PGL.allocateDirectByteBuffer(arr.length);
    } else {
      return ByteBuffer.wrap(arr);
    }
  }


  protected static ByteBuffer updateByteBuffer(ByteBuffer buf, byte[] arr,
                                               boolean wrap) {
    if (USE_DIRECT_BUFFERS) {
      if (buf == null || buf.capacity() < arr.length) {
        buf = PGL.allocateDirectByteBuffer(arr.length);
      }
      buf.position(0);
      buf.put(arr);
      buf.rewind();
    } else {
      if (wrap) {
        buf = ByteBuffer.wrap(arr);
      } else {
        if (buf == null || buf.capacity() < arr.length) {
          buf = ByteBuffer.allocate(arr.length);
        }
        buf.position(0);
        buf.put(arr);
        buf.rewind();
      }
    }
    return buf;
  }


  protected static void getByteArray(ByteBuffer buf, byte[] arr) {
    if (!buf.hasArray() || buf.array() != arr) {
      buf.position(0);
      buf.get(arr);
      buf.rewind();
    }
  }


  protected static void putByteArray(ByteBuffer buf, byte[] arr) {
    if (!buf.hasArray() || buf.array() != arr) {
      buf.position(0);
      buf.put(arr);
      buf.rewind();
    }
  }


  protected static void fillByteBuffer(ByteBuffer buf, int i0, int i1,
                                       byte val) {
    int n = i1 - i0;
    byte[] temp = new byte[n];
    Arrays.fill(temp, 0, n, val);
    buf.position(i0);
    buf.put(temp, 0, n);
    buf.rewind();
  }


  protected static ShortBuffer allocateDirectShortBuffer(int size) {
    int bytes = PApplet.max(MIN_DIRECT_BUFFER_SIZE, size) * SIZEOF_SHORT;
    return ByteBuffer.allocateDirect(bytes).order(ByteOrder.nativeOrder()).
           asShortBuffer();
  }


  protected static ShortBuffer allocateShortBuffer(int size) {
    if (USE_DIRECT_BUFFERS) {
      return allocateDirectShortBuffer(size);
    } else {
      return ShortBuffer.allocate(size);
    }
  }


  protected static ShortBuffer allocateShortBuffer(short[] arr) {
    if (USE_DIRECT_BUFFERS) {
      return PGL.allocateDirectShortBuffer(arr.length);
    } else {
      return ShortBuffer.wrap(arr);
    }
  }


  protected static ShortBuffer updateShortBuffer(ShortBuffer buf, short[] arr,
                                                 boolean wrap) {
    if (USE_DIRECT_BUFFERS) {
      if (buf == null || buf.capacity() < arr.length) {
        buf = PGL.allocateDirectShortBuffer(arr.length);
      }
      buf.position(0);
      buf.put(arr);
      buf.rewind();
    } else {
      if (wrap) {
        buf = ShortBuffer.wrap(arr);
      } else {
        if (buf == null || buf.capacity() < arr.length) {
          buf = ShortBuffer.allocate(arr.length);
        }
        buf.position(0);
        buf.put(arr);
        buf.rewind();
      }
    }
    return buf;
  }


  protected static void getShortArray(ShortBuffer buf, short[] arr) {
    if (!buf.hasArray() || buf.array() != arr) {
      buf.position(0);
      buf.get(arr);
      buf.rewind();
    }
  }


  protected static void putShortArray(ShortBuffer buf, short[] arr) {
    if (!buf.hasArray() || buf.array() != arr) {
      buf.position(0);
      buf.put(arr);
      buf.rewind();
    }
  }


  protected static void fillShortBuffer(ShortBuffer buf, int i0, int i1,
                                        short val) {
    int n = i1 - i0;
    short[] temp = new short[n];
    Arrays.fill(temp, 0, n, val);
    buf.position(i0);
    buf.put(temp, 0, n);
    buf.rewind();
  }


  protected static IntBuffer allocateDirectIntBuffer(int size) {
    int bytes = PApplet.max(MIN_DIRECT_BUFFER_SIZE, size) * SIZEOF_INT;
    return ByteBuffer.allocateDirect(bytes).order(ByteOrder.nativeOrder()).
           asIntBuffer();
  }


  protected static IntBuffer allocateIntBuffer(int size) {
    if (USE_DIRECT_BUFFERS) {
      return allocateDirectIntBuffer(size);
    } else {
      return IntBuffer.allocate(size);
    }
  }


  protected static IntBuffer allocateIntBuffer(int[] arr) {
    if (USE_DIRECT_BUFFERS) {
      return PGL.allocateDirectIntBuffer(arr.length);
    } else {
      return IntBuffer.wrap(arr);
    }
  }


  protected static IntBuffer updateIntBuffer(IntBuffer buf, int[] arr,
                                             boolean wrap) {
    if (USE_DIRECT_BUFFERS) {
      if (buf == null || buf.capacity() < arr.length) {
        buf = PGL.allocateDirectIntBuffer(arr.length);
      }
      buf.position(0);
      buf.put(arr);
      buf.rewind();
    } else {
      if (wrap) {
        buf = IntBuffer.wrap(arr);
      } else {
        if (buf == null || buf.capacity() < arr.length) {
          buf = IntBuffer.allocate(arr.length);
        }
        buf.position(0);
        buf.put(arr);
        buf.rewind();
      }
    }
    return buf;
  }


  protected static void getIntArray(IntBuffer buf, int[] arr) {
    if (!buf.hasArray() || buf.array() != arr) {
      buf.position(0);
      buf.get(arr);
      buf.rewind();
    }
  }


  protected static void putIntArray(IntBuffer buf, int[] arr) {
    if (!buf.hasArray() || buf.array() != arr) {
      buf.position(0);
      buf.put(arr);
      buf.rewind();
    }
  }


  protected static void fillIntBuffer(IntBuffer buf, int i0, int i1, int val) {
    int n = i1 - i0;
    int[] temp = new int[n];
    Arrays.fill(temp, 0, n, val);
    buf.position(i0);
    buf.put(temp, 0, n);
    buf.rewind();
  }


  protected static FloatBuffer allocateDirectFloatBuffer(int size) {
    int bytes = PApplet.max(MIN_DIRECT_BUFFER_SIZE, size) * SIZEOF_FLOAT;
    return ByteBuffer.allocateDirect(bytes).order(ByteOrder.nativeOrder()).
           asFloatBuffer();
  }


  protected static FloatBuffer allocateFloatBuffer(int size) {
    if (USE_DIRECT_BUFFERS) {
      return allocateDirectFloatBuffer(size);
    } else {
      return FloatBuffer.allocate(size);
    }
  }


  protected static FloatBuffer allocateFloatBuffer(float[] arr) {
    if (USE_DIRECT_BUFFERS) {
      return PGL.allocateDirectFloatBuffer(arr.length);
    } else {
      return FloatBuffer.wrap(arr);
    }
  }


  protected static FloatBuffer updateFloatBuffer(FloatBuffer buf, float[] arr,
                                                 boolean wrap) {
    if (USE_DIRECT_BUFFERS) {
      if (buf == null || buf.capacity() < arr.length) {
        buf = PGL.allocateDirectFloatBuffer(arr.length);
      }
      buf.position(0);
      buf.put(arr);
      buf.rewind();
    } else {
      if (wrap) {
        buf = FloatBuffer.wrap(arr);
      } else {
        if (buf == null || buf.capacity() < arr.length) {
          buf = FloatBuffer.allocate(arr.length);
        }
        buf.position(0);
        buf.put(arr);
        buf.rewind();
      }
    }
    return buf;
  }


  protected static void getFloatArray(FloatBuffer buf, float[] arr) {
    if (!buf.hasArray() || buf.array() != arr) {
      buf.position(0);
      buf.get(arr);
      buf.rewind();
    }
  }


  protected static void putFloatArray(FloatBuffer buf, float[] arr) {
    if (!buf.hasArray() || buf.array() != arr) {
      buf.position(0);
      buf.put(arr);
      buf.rewind();
    }
  }


  protected static void fillFloatBuffer(FloatBuffer buf, int i0, int i1,
                                        float val) {
    int n = i1 - i0;
    float[] temp = new float[n];
    Arrays.fill(temp, 0, n, val);
    buf.position(i0);
    buf.put(temp, 0, n);
    buf.rewind();
  }


  ///////////////////////////////////////////////////////////

  // Java specific stuff


  protected class KeyPoller extends Thread {
    protected PApplet parent;
    protected boolean stopRequested;
    protected boolean[] pressedKeys;
    protected char[] charCheys;

    KeyPoller(PApplet parent) {
      this.parent = parent;
      stopRequested = false;
    }

    @Override
    public void run() {
      pressedKeys = new boolean[256];
      charCheys = new char[256];
      Keyboard.enableRepeatEvents(true);
      while (true) {
        if (stopRequested) break;

        Keyboard.poll();
        while (Keyboard.next()) {
          if (stopRequested) break;

          long millis = Keyboard.getEventNanoseconds() / 1000000L;
          char keyChar = Keyboard.getEventCharacter();
          int keyCode = Keyboard.getEventKey();

          if (keyCode >= pressedKeys.length) continue;

          int modifiers = 0;
          if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) ||
              Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) {
            modifiers |= Event.SHIFT;
          }
          if (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) ||
              Keyboard.isKeyDown(Keyboard.KEY_RCONTROL)) {
            modifiers |= Event.CTRL;
          }
          if (Keyboard.isKeyDown(Keyboard.KEY_LMETA) ||
              Keyboard.isKeyDown(Keyboard.KEY_RMETA)) {
            modifiers |= Event.META;
          }
          if (Keyboard.isKeyDown(Keyboard.KEY_LMENU) ||
              Keyboard.isKeyDown(Keyboard.KEY_RMENU)) {
            // LWJGL maps the menu key and the alt key to the same value.
            modifiers |= Event.ALT;
          }

          int keyPCode = LWJGLtoAWTCode(keyCode);
          if (keyChar == 0) {
            keyChar = PConstants.CODED;
          }

          int action = 0;
          if (Keyboard.getEventKeyState()) {
            action = KeyEvent.PRESS;
            KeyEvent ke = new KeyEvent(null, millis,
                                       action, modifiers,
                                       keyChar, keyPCode);
            parent.postEvent(ke);
            pressedKeys[keyCode] = true;
            charCheys[keyCode] = keyChar;
            keyPCode = 0;
            action = KeyEvent.TYPE;
          } else if (pressedKeys[keyCode]) {
            keyChar = charCheys[keyCode];
            pressedKeys[keyCode] = false;
            action = KeyEvent.RELEASE;
          }

          KeyEvent ke = new KeyEvent(null, millis,
                                     action, modifiers,
                                     keyChar, keyPCode);
          parent.postEvent(ke);
        }
        try {
          Thread.sleep(10);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }

    public void requestStop() {
      stopRequested = true;
    }
  }


  protected class MousePoller extends Thread {
    protected PApplet parent;
    protected boolean stopRequested;
    protected boolean pressed;
    protected boolean inside;
    protected long startedClickTime;
    protected int startedClickButton;

    MousePoller(PApplet parent) {
      this.parent = parent;
      stopRequested = false;
    }

    @Override
    public void run() {
      while (true) {
        if (stopRequested) break;

        Mouse.poll();
        while (Mouse.next()) {
          if (stopRequested) break;

          long millis = Mouse.getEventNanoseconds() / 1000000L;

          int modifiers = 0;
          if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) ||
              Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) {
            modifiers |= Event.SHIFT;
          }
          if (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) ||
              Keyboard.isKeyDown(Keyboard.KEY_RCONTROL)) {
            modifiers |= Event.CTRL;
          }
          if (Keyboard.isKeyDown(Keyboard.KEY_LMETA) ||
              Keyboard.isKeyDown(Keyboard.KEY_RMETA)) {
            modifiers |= Event.META;
          }
          if (Keyboard.isKeyDown(Keyboard.KEY_LMENU) ||
              Keyboard.isKeyDown(Keyboard.KEY_RMENU)) {
            // LWJGL maps the menu key and the alt key to the same value.
            modifiers |= Event.ALT;
          }

          int x = Mouse.getX();
          int y = parent.height - Mouse.getY();
          int button = 0;
          if (Mouse.isButtonDown(0)) {
            button = PConstants.LEFT;
          } else if (Mouse.isButtonDown(1)) {
            button = PConstants.RIGHT;
          } else if (Mouse.isButtonDown(2)) {
            button = PConstants.CENTER;
          }

          int action = 0;
          if (button != 0) {
            if (pressed) {
              action = MouseEvent.DRAG;
            } else {
              action = MouseEvent.PRESS;
              pressed = true;
            }
          } else if (pressed) {
            action = MouseEvent.RELEASE;
            pressed = false;
          } else {
            action = MouseEvent.MOVE;
          }

          if (inside) {
            if (!Mouse.isInsideWindow()) {
              inside = false;
              action = MouseEvent.EXIT;
            }
          } else {
            if (Mouse.isInsideWindow()) {
              inside = true;
              action = MouseEvent.ENTER;
            }
          }

          int count = 0;
          if (Mouse.getEventButtonState()) {
            startedClickTime = millis;
            startedClickButton = button;
          } else {
            if (action == MouseEvent.RELEASE) {
              boolean clickDetected = millis - startedClickTime < 500;
              if (clickDetected) {
                // post a RELEASE event, in addition to the CLICK event.
                MouseEvent me = new MouseEvent(null, millis, action, modifiers,
                                               x, y, button, count);
                parent.postEvent(me);
                action = MouseEvent.CLICK;
                count = 1;
              }
            }
          }

          MouseEvent me = new MouseEvent(null, millis, action, modifiers,
                                         x, y, button, count);
          parent.postEvent(me);
        }
        try {
          Thread.sleep(10);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }

    public void requestStop() {
      stopRequested = true;
    }
  }

  // To complete later...
  // http://docs.oracle.com/javase/6/docs/api/java/awt/event/KeyEvent.html
  // http://processing.org/reference/keyCode.html
  protected int LWJGLtoAWTCode(int code) {
    switch (code) {
    case Keyboard.KEY_0:
      return java.awt.event.KeyEvent.VK_0;
    case Keyboard.KEY_1:
      return java.awt.event.KeyEvent.VK_1;
    case Keyboard.KEY_2:
      return java.awt.event.KeyEvent.VK_2;
    case Keyboard.KEY_3:
      return java.awt.event.KeyEvent.VK_3;
    case Keyboard.KEY_4:
      return java.awt.event.KeyEvent.VK_4;
    case Keyboard.KEY_5:
      return java.awt.event.KeyEvent.VK_5;
    case Keyboard.KEY_6:
      return java.awt.event.KeyEvent.VK_6;
    case Keyboard.KEY_7:
      return java.awt.event.KeyEvent.VK_7;
    case Keyboard.KEY_8:
      return java.awt.event.KeyEvent.VK_8;
    case Keyboard.KEY_9:
      return java.awt.event.KeyEvent.VK_9;
    case Keyboard.KEY_A:
      return java.awt.event.KeyEvent.VK_A;
    case Keyboard.KEY_B:
      return java.awt.event.KeyEvent.VK_B;
    case Keyboard.KEY_C:
      return java.awt.event.KeyEvent.VK_C;
    case Keyboard.KEY_D:
      return java.awt.event.KeyEvent.VK_D;
    case Keyboard.KEY_E:
      return java.awt.event.KeyEvent.VK_E;
    case Keyboard.KEY_F:
      return java.awt.event.KeyEvent.VK_F;
    case Keyboard.KEY_G:
      return java.awt.event.KeyEvent.VK_G;
    case Keyboard.KEY_H:
      return java.awt.event.KeyEvent.VK_H;
    case Keyboard.KEY_I:
      return java.awt.event.KeyEvent.VK_I;
    case Keyboard.KEY_J:
      return java.awt.event.KeyEvent.VK_J;
    case Keyboard.KEY_K:
      return java.awt.event.KeyEvent.VK_K;
    case Keyboard.KEY_L:
      return java.awt.event.KeyEvent.VK_L;
    case Keyboard.KEY_M:
      return java.awt.event.KeyEvent.VK_M;
    case Keyboard.KEY_N:
      return java.awt.event.KeyEvent.VK_N;
    case Keyboard.KEY_O:
      return java.awt.event.KeyEvent.VK_O;
    case Keyboard.KEY_P:
      return java.awt.event.KeyEvent.VK_P;
    case Keyboard.KEY_Q:
      return java.awt.event.KeyEvent.VK_Q;
    case Keyboard.KEY_R:
      return java.awt.event.KeyEvent.VK_R;
    case Keyboard.KEY_S:
      return java.awt.event.KeyEvent.VK_S;
    case Keyboard.KEY_T:
      return java.awt.event.KeyEvent.VK_T;
    case Keyboard.KEY_U:
      return java.awt.event.KeyEvent.VK_U;
    case Keyboard.KEY_V:
      return java.awt.event.KeyEvent.VK_V;
    case Keyboard.KEY_W:
      return java.awt.event.KeyEvent.VK_W;
    case Keyboard.KEY_X:
      return java.awt.event.KeyEvent.VK_X;
    case Keyboard.KEY_Y:
      return java.awt.event.KeyEvent.VK_Y;
    case Keyboard.KEY_Z:
      return java.awt.event.KeyEvent.VK_Z;
    case Keyboard.KEY_ADD:
      return java.awt.event.KeyEvent.VK_ADD;
    case Keyboard.KEY_APOSTROPHE:
      return java.awt.event.KeyEvent.VK_ASTERISK;
    case Keyboard.KEY_AT:
      return java.awt.event.KeyEvent.VK_AT;
    case Keyboard.KEY_BACK:
      return java.awt.event.KeyEvent.VK_BACK_SPACE;
    case Keyboard.KEY_BACKSLASH:
      return java.awt.event.KeyEvent.VK_BACK_SLASH;
    case Keyboard.KEY_CAPITAL:
      return java.awt.event.KeyEvent.VK_CAPS_LOCK;
    case Keyboard.KEY_CIRCUMFLEX:
      return java.awt.event.KeyEvent.VK_CIRCUMFLEX;
    case Keyboard.KEY_COLON:
      return java.awt.event.KeyEvent.VK_COLON;
    case Keyboard.KEY_COMMA:
      return java.awt.event.KeyEvent.VK_COMMA;
    case Keyboard.KEY_CONVERT:
      return java.awt.event.KeyEvent.VK_CONVERT;
    case Keyboard.KEY_DECIMAL:
      return java.awt.event.KeyEvent.VK_DECIMAL;
    case Keyboard.KEY_DELETE:
      return java.awt.event.KeyEvent.VK_DELETE;
    case Keyboard.KEY_DIVIDE:
      return java.awt.event.KeyEvent.VK_DIVIDE;
    case Keyboard.KEY_DOWN:
      return java.awt.event.KeyEvent.VK_DOWN;
    case Keyboard.KEY_END:
      return java.awt.event.KeyEvent.VK_END;
    case Keyboard.KEY_EQUALS:
      return java.awt.event.KeyEvent.VK_EQUALS;
    case Keyboard.KEY_ESCAPE:
      return java.awt.event.KeyEvent.VK_ESCAPE;
    case Keyboard.KEY_F1:
      return java.awt.event.KeyEvent.VK_F1;
    case Keyboard.KEY_F10:
      return java.awt.event.KeyEvent.VK_F10;
    case Keyboard.KEY_F11:
      return java.awt.event.KeyEvent.VK_F11;
    case Keyboard.KEY_F12:
      return java.awt.event.KeyEvent.VK_F12;
    case Keyboard.KEY_F13:
      return java.awt.event.KeyEvent.VK_F13;
    case Keyboard.KEY_F14:
      return java.awt.event.KeyEvent.VK_F14;
    case Keyboard.KEY_F15:
      return java.awt.event.KeyEvent.VK_F15;
    case Keyboard.KEY_F2:
      return java.awt.event.KeyEvent.VK_F2;
    case Keyboard.KEY_F3:
      return java.awt.event.KeyEvent.VK_F3;
    case Keyboard.KEY_F4:
      return java.awt.event.KeyEvent.VK_F4;
    case Keyboard.KEY_F5:
      return java.awt.event.KeyEvent.VK_F5;
    case Keyboard.KEY_F6:
      return java.awt.event.KeyEvent.VK_F6;
    case Keyboard.KEY_F7:
      return java.awt.event.KeyEvent.VK_F7;
    case Keyboard.KEY_F8:
      return java.awt.event.KeyEvent.VK_F8;
    case Keyboard.KEY_F9:
      return java.awt.event.KeyEvent.VK_F9;
//    case Keyboard.KEY_GRAVE:
    case Keyboard.KEY_HOME:
      return java.awt.event.KeyEvent.VK_HOME;
    case Keyboard.KEY_INSERT:
      return java.awt.event.KeyEvent.VK_INSERT;
    case Keyboard.KEY_LBRACKET:
      return java.awt.event.KeyEvent.VK_BRACELEFT;
    case Keyboard.KEY_LCONTROL:
      return java.awt.event.KeyEvent.VK_CONTROL;
    case Keyboard.KEY_LEFT:
      return java.awt.event.KeyEvent.VK_LEFT;
    case Keyboard.KEY_LMENU:
      return java.awt.event.KeyEvent.VK_ALT;
    case Keyboard.KEY_LMETA:
      return java.awt.event.KeyEvent.VK_META;
    case Keyboard.KEY_LSHIFT:
      return java.awt.event.KeyEvent.VK_SHIFT;
    case Keyboard.KEY_MINUS:
      return java.awt.event.KeyEvent.VK_MINUS;
    case Keyboard.KEY_MULTIPLY:
      return java.awt.event.KeyEvent.VK_MULTIPLY;
//    case Keyboard.KEY_NEXT:
    case Keyboard.KEY_NUMLOCK:
      return java.awt.event.KeyEvent.VK_NUM_LOCK;
    case Keyboard.KEY_NUMPAD0:
      return java.awt.event.KeyEvent.VK_NUMPAD0;
    case Keyboard.KEY_NUMPAD1:
      return java.awt.event.KeyEvent.VK_NUMPAD1;
    case Keyboard.KEY_NUMPAD2:
      return java.awt.event.KeyEvent.VK_NUMPAD2;
    case Keyboard.KEY_NUMPAD3:
      return java.awt.event.KeyEvent.VK_NUMPAD3;
    case Keyboard.KEY_NUMPAD4:
      return java.awt.event.KeyEvent.VK_NUMPAD4;
    case Keyboard.KEY_NUMPAD5:
      return java.awt.event.KeyEvent.VK_NUMPAD5;
    case Keyboard.KEY_NUMPAD6:
      return java.awt.event.KeyEvent.VK_NUMPAD6;
    case Keyboard.KEY_NUMPAD7:
      return java.awt.event.KeyEvent.VK_NUMPAD7;
    case Keyboard.KEY_NUMPAD8:
      return java.awt.event.KeyEvent.VK_NUMPAD8;
    case Keyboard.KEY_NUMPAD9:
      return java.awt.event.KeyEvent.VK_NUMPAD9;
//    case Keyboard.KEY_NUMPADCOMMA:
//    case Keyboard.KEY_NUMPADENTER:
//    case Keyboard.KEY_NUMPADEQUALS:
    case Keyboard.KEY_PAUSE:
      return java.awt.event.KeyEvent.VK_PAUSE;
    case Keyboard.KEY_PERIOD:
      return java.awt.event.KeyEvent.VK_PERIOD;
//    case Keyboard.KEY_POWER:
//    case Keyboard.KEY_PRIOR:
    case Keyboard.KEY_RBRACKET:
      return java.awt.event.KeyEvent.VK_BRACERIGHT;
    case Keyboard.KEY_RCONTROL:
      return java.awt.event.KeyEvent.VK_CONTROL;
    case Keyboard.KEY_RETURN:
      return java.awt.event.KeyEvent.VK_ENTER;
    case Keyboard.KEY_RIGHT:
      return java.awt.event.KeyEvent.VK_RIGHT;
//    case Keyboard.KEY_RMENU:
    case Keyboard.KEY_RMETA:
      return java.awt.event.KeyEvent.VK_META;
    case Keyboard.KEY_RSHIFT:
      return java.awt.event.KeyEvent.VK_SHIFT;
//    case Keyboard.KEY_SCROLL:
    case Keyboard.KEY_SEMICOLON:
      return java.awt.event.KeyEvent.VK_SEMICOLON;
    case Keyboard.KEY_SLASH:
      return java.awt.event.KeyEvent.VK_SLASH;
//    case Keyboard.KEY_SLEEP:
    case Keyboard.KEY_SPACE:
      return java.awt.event.KeyEvent.VK_SPACE;
    case Keyboard.KEY_STOP:
      return java.awt.event.KeyEvent.VK_STOP;
    case Keyboard.KEY_SUBTRACT:
      return java.awt.event.KeyEvent.VK_SUBTRACT;
    case Keyboard.KEY_TAB:
      return java.awt.event.KeyEvent.VK_TAB;
//    case Keyboard.KEY_UNDERLINE:
    case Keyboard.KEY_UP:
      return java.awt.event.KeyEvent.VK_UP;
    default:
      return 0;
    }
  }
}
