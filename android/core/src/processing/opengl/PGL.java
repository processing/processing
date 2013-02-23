/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2011-12 Ben Fry and Casey Reas

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

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;

import processing.core.PApplet;
import processing.opengl.tess.PGLU;
import processing.opengl.tess.PGLUtessellator;
import processing.opengl.tess.PGLUtessellatorCallbackAdapter;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.opengles.*;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView.EGLConfigChooser;
import android.opengl.GLSurfaceView.Renderer;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;

/**
 * Processing-OpenGL abstraction layer.
 *
 */
public class PGL {

  ///////////////////////////////////////////////////////////

  // Parameters

  public static boolean FORCE_SCREEN_FBO             = false;
  // The use of indirect buffers creates problems with glBufferSubData because
  // the buffer position is ignored:
  // http://stackoverflow.com/questions/3380489/glbuffersubdata-with-an-offset-into-buffer-without-causing-garbage
  // http://code.google.com/p/android/issues/detail?id=12245
  // This doesn't happen with direct buffers.
  public static final boolean USE_DIRECT_BUFFERS     = true;
  public static final int MIN_DIRECT_BUFFER_SIZE     = 1;
  public static final boolean SAVE_SURFACE_TO_PIXELS = false;

  /** Enables/disables mipmap use. **/
  protected static final boolean MIPMAPS_ENABLED     = false;

  /** Initial sizes for arrays of input and tessellated data. */
  protected static final int DEFAULT_IN_VERTICES   = 16;
  protected static final int DEFAULT_IN_EDGES      = 32;
  protected static final int DEFAULT_IN_TEXTURES   = 16;
  protected static final int DEFAULT_TESS_VERTICES = 16;
  protected static final int DEFAULT_TESS_INDICES  = 32;

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
  protected static final int MAX_FONT_TEX_SIZE = 512;

  /** Minimum stroke weight needed to apply the full path stroking
   * algorithm that properly generates caps and joing.
   */
  protected static final float MIN_CAPS_JOINS_WEIGHT = 2.f;

  /** Maximum length of linear paths to be stroked with the
   * full algorithm that generates accurate caps and joins.
   */
  protected static final int MAX_CAPS_JOINS_LENGTH = 1000;

  /** Minimum array size to use arrayCopy method(). **/
  protected static final int MIN_ARRAYCOPY_SIZE = 2;

  protected static final int SIZEOF_SHORT = Short.SIZE / 8;
  protected static final int SIZEOF_INT = Integer.SIZE / 8;
  protected static final int SIZEOF_FLOAT = Float.SIZE / 8;
  protected static final int SIZEOF_BYTE = Byte.SIZE / 8;
  protected static final int SIZEOF_INDEX = SIZEOF_SHORT;
  protected static final int INDEX_TYPE = GLES20.GL_UNSIGNED_SHORT;

  /** Error string from framebuffer errors **/
  protected static final String FRAMEBUFFER_ERROR_MESSAGE =
    "Framebuffer error (%1$s), rendering will probably not work as expected";

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

  // The values for constants not defined in the GLES20 interface can be found
  // in this file:
  // http://code.metager.de/source/xref/android/4.0.3/development/tools/glesv2debugger/src/com/android/glesv2debugger/GLEnum.java

  public static final int FALSE = GLES20.GL_FALSE;
  public static final int TRUE  = GLES20.GL_TRUE;

  public static final int LESS   = GLES20.GL_LESS;
  public static final int LEQUAL = GLES20.GL_LEQUAL;

  public static final int CCW    = GLES20.GL_CCW;
  public static final int CW     = GLES20.GL_CW;

  public static final int CULL_FACE      = GLES20.GL_CULL_FACE;
  public static final int FRONT          = GLES20.GL_FRONT;
  public static final int BACK           = GLES20.GL_BACK;
  public static final int FRONT_AND_BACK = GLES20.GL_FRONT_AND_BACK;

  public static final int VIEWPORT = GLES20.GL_VIEWPORT;

  public static final int SCISSOR_TEST    = GLES20.GL_SCISSOR_TEST;
  public static final int DEPTH_TEST      = GLES20.GL_DEPTH_TEST;
  public static final int DEPTH_WRITEMASK = GLES20.GL_DEPTH_WRITEMASK;

  public static final int COLOR_BUFFER_BIT   = GLES20.GL_COLOR_BUFFER_BIT;
  public static final int DEPTH_BUFFER_BIT   = GLES20.GL_DEPTH_BUFFER_BIT;
  public static final int STENCIL_BUFFER_BIT = GLES20.GL_STENCIL_BUFFER_BIT;

  public static final int FUNC_ADD              = GLES20.GL_FUNC_ADD;
  public static final int FUNC_MIN              = 0x8007;
  public static final int FUNC_MAX              = 0x8008;
  public static final int FUNC_REVERSE_SUBTRACT =
    GLES20.GL_FUNC_REVERSE_SUBTRACT;

  public static final int TEXTURE_2D = GLES20.GL_TEXTURE_2D;

  public static final int TEXTURE_BINDING_2D = GLES20.GL_TEXTURE_BINDING_2D;

  public static final int RGB            = GLES20.GL_RGB;
  public static final int RGBA           = GLES20.GL_RGBA;
  public static final int ALPHA          = GLES20.GL_ALPHA;
  public static final int UNSIGNED_INT   = GLES20.GL_UNSIGNED_INT;
  public static final int UNSIGNED_BYTE  = GLES20.GL_UNSIGNED_BYTE;
  public static final int UNSIGNED_SHORT = GLES20.GL_UNSIGNED_SHORT;
  public static final int FLOAT          = GLES20.GL_FLOAT;

  public static final int NEAREST               = GLES20.GL_NEAREST;
  public static final int LINEAR                = GLES20.GL_LINEAR;
  public static final int LINEAR_MIPMAP_NEAREST =
    GLES20.GL_LINEAR_MIPMAP_NEAREST;
  public static final int LINEAR_MIPMAP_LINEAR  =
    GLES20.GL_LINEAR_MIPMAP_LINEAR;

  public static final int TEXTURE_MAX_ANISOTROPY     = 0x84FE;
  public static final int MAX_TEXTURE_MAX_ANISOTROPY = 0x84FF;

  public static final int CLAMP_TO_EDGE = GLES20.GL_CLAMP_TO_EDGE;
  public static final int REPEAT        = GLES20.GL_REPEAT;

  public static final int RGBA8            = -1;
  public static final int DEPTH24_STENCIL8 = 0x88F0;

  public static final int DEPTH_COMPONENT   = GLES20.GL_DEPTH_COMPONENT;
  public static final int DEPTH_COMPONENT16 = GLES20.GL_DEPTH_COMPONENT16;
  public static final int DEPTH_COMPONENT24 = 0x81A6;
  public static final int DEPTH_COMPONENT32 = 0x81A7;

  public static final int STENCIL_INDEX  = GLES20.GL_STENCIL_INDEX;
  public static final int STENCIL_INDEX1 = 0x8D46;
  public static final int STENCIL_INDEX4 = 0x8D47;
  public static final int STENCIL_INDEX8 = GLES20.GL_STENCIL_INDEX8;

  public static final int ARRAY_BUFFER         = GLES20.GL_ARRAY_BUFFER;
  public static final int ELEMENT_ARRAY_BUFFER = GLES20.GL_ELEMENT_ARRAY_BUFFER;

  public static final int SAMPLES = GLES20.GL_SAMPLES;

  public static final int FRAMEBUFFER_COMPLETE                      =
    GLES20.GL_FRAMEBUFFER_COMPLETE;
  public static final int FRAMEBUFFER_INCOMPLETE_ATTACHMENT         =
    GLES20.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT;
  public static final int FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT =
    GLES20.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT;
  public static final int FRAMEBUFFER_INCOMPLETE_DIMENSIONS         =
    GLES20.GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS;
  public static final int FRAMEBUFFER_INCOMPLETE_FORMATS            =
    0x8CDA;
  public static final int FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER        = -1;
  public static final int FRAMEBUFFER_INCOMPLETE_READ_BUFFER        = -1;
  public static final int FRAMEBUFFER_UNSUPPORTED                   =
    GLES20.GL_FRAMEBUFFER_UNSUPPORTED;

  public static final int STATIC_DRAW  = GLES20.GL_STATIC_DRAW;
  public static final int DYNAMIC_DRAW = GLES20.GL_DYNAMIC_DRAW;
  public static final int STREAM_DRAW  = GLES20.GL_STREAM_DRAW;

  public static final int READ_ONLY  = -1;
  public static final int WRITE_ONLY = -1;
  public static final int READ_WRITE = -1;

  public static final int TRIANGLE_FAN   = GLES20.GL_TRIANGLE_FAN;
  public static final int TRIANGLE_STRIP = GLES20.GL_TRIANGLE_STRIP;
  public static final int TRIANGLES      = GLES20.GL_TRIANGLES;

  public static final int VENDOR                   = GLES20.GL_VENDOR;
  public static final int RENDERER                 = GLES20.GL_RENDERER;
  public static final int VERSION                  = GLES20.GL_VERSION;
  public static final int EXTENSIONS               = GLES20.GL_EXTENSIONS;
  public static final int SHADING_LANGUAGE_VERSION =
    GLES20.GL_SHADING_LANGUAGE_VERSION;

  public static final int MAX_TEXTURE_SIZE         = GLES20.GL_MAX_TEXTURE_SIZE;
  public static final int MAX_SAMPLES              = -1;
  public static final int ALIASED_LINE_WIDTH_RANGE =
    GLES20.GL_ALIASED_LINE_WIDTH_RANGE;
  public static final int ALIASED_POINT_SIZE_RANGE =
    GLES20.GL_ALIASED_POINT_SIZE_RANGE;
  public static final int DEPTH_BITS               = GLES20.GL_DEPTH_BITS;
  public static final int STENCIL_BITS             = GLES20.GL_STENCIL_BITS;

  public static final int TESS_WINDING_NONZERO = PGLU.GLU_TESS_WINDING_NONZERO;
  public static final int TESS_WINDING_ODD     = PGLU.GLU_TESS_WINDING_ODD;

  public static final int TEXTURE0           = GLES20.GL_TEXTURE0;
  public static final int TEXTURE1           = GLES20.GL_TEXTURE1;
  public static final int TEXTURE2           = GLES20.GL_TEXTURE2;
  public static final int TEXTURE3           = GLES20.GL_TEXTURE3;
  public static final int TEXTURE_MIN_FILTER = GLES20.GL_TEXTURE_MIN_FILTER;
  public static final int TEXTURE_MAG_FILTER = GLES20.GL_TEXTURE_MAG_FILTER;
  public static final int TEXTURE_WRAP_S     = GLES20.GL_TEXTURE_WRAP_S;
  public static final int TEXTURE_WRAP_T     = GLES20.GL_TEXTURE_WRAP_T;

  public static final int BLEND               = GLES20.GL_BLEND;
  public static final int ONE                 = GLES20.GL_ONE;
  public static final int ZERO                = GLES20.GL_ZERO;
  public static final int SRC_ALPHA           = GLES20.GL_SRC_ALPHA;
  public static final int DST_ALPHA           = GLES20.GL_DST_ALPHA;
  public static final int ONE_MINUS_SRC_ALPHA = GLES20.GL_ONE_MINUS_SRC_ALPHA;
  public static final int ONE_MINUS_DST_COLOR = GLES20.GL_ONE_MINUS_DST_COLOR;
  public static final int ONE_MINUS_SRC_COLOR = GLES20.GL_ONE_MINUS_SRC_COLOR;
  public static final int DST_COLOR           = GLES20.GL_DST_COLOR;
  public static final int SRC_COLOR           = GLES20.GL_SRC_COLOR;

  public static final int FRAMEBUFFER        = GLES20.GL_FRAMEBUFFER;
  public static final int COLOR_ATTACHMENT0  = GLES20.GL_COLOR_ATTACHMENT0;
  public static final int COLOR_ATTACHMENT1  = -1;
  public static final int COLOR_ATTACHMENT2  = -1;
  public static final int COLOR_ATTACHMENT3  = -1;
  public static final int RENDERBUFFER       = GLES20.GL_RENDERBUFFER;
  public static final int DEPTH_ATTACHMENT   = GLES20.GL_DEPTH_ATTACHMENT;
  public static final int STENCIL_ATTACHMENT = GLES20.GL_STENCIL_ATTACHMENT;
  public static final int READ_FRAMEBUFFER   = -1;
  public static final int DRAW_FRAMEBUFFER   = -1;

  public static final int VERTEX_SHADER        = GLES20.GL_VERTEX_SHADER;
  public static final int FRAGMENT_SHADER      = GLES20.GL_FRAGMENT_SHADER;
  public static final int INFO_LOG_LENGTH      = GLES20.GL_INFO_LOG_LENGTH;
  public static final int SHADER_SOURCE_LENGTH = GLES20.GL_SHADER_SOURCE_LENGTH;
  public static final int COMPILE_STATUS       = GLES20.GL_COMPILE_STATUS;
  public static final int LINK_STATUS          = GLES20.GL_LINK_STATUS;
  public static final int VALIDATE_STATUS      = GLES20.GL_VALIDATE_STATUS;

  public static final int MULTISAMPLE    = -1;
  public static final int POINT_SMOOTH   = -1;
  public static final int LINE_SMOOTH    = -1;
  public static final int POLYGON_SMOOTH = -1;

  // Some EGL constants needed to initialize an GLES2 context.
  protected static final int EGL_CONTEXT_CLIENT_VERSION = 0x3098;
  protected static final int EGL_OPENGL_ES2_BIT         = 0x0004;

  /** Basic GLES 1.0 interface */
  public static GL10 gl;

  /** GLU interface **/
  public static PGLU glu;

  /** The current opengl context */
  public static EGLContext context;

  /** The current surface view */
  public static GLSurfaceView glview;

  /** The PGraphics object using this interface */
  protected PGraphicsOpenGL pg;

  /** The renderer object driving the rendering loop,
   * analogous to the GLEventListener in JOGL */
  protected static AndroidRenderer renderer;

  /** OpenGL thread */
  protected static Thread glThread;

  /** Which texturing targets are enabled */
  protected static boolean[] texturingTargets = { false };

  /** Which textures are bound to each target */
  protected static int[] boundTextures = { 0 };

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

  ///////////////////////////////////////////////////////////

  // Texture rendering

  protected static boolean loadedTexShader = false;
  protected static int texShaderProgram;
  protected static int texVertShader;
  protected static int texFragShader;
  protected static EGLContext texShaderContext;
  protected static int texVertLoc;
  protected static int texTCoordLoc;

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

  protected static String texFragShaderSource =
    SHADER_PREPROCESSOR_DIRECTIVE +
    "uniform sampler2D textureSampler;" +
    "varying vec2 vertTexcoord;" +
    "void main() {" +
    "  gl_FragColor = texture2D(textureSampler, vertTexcoord.st);" +
    "}";

  ///////////////////////////////////////////////////////////

  // Utilities

  protected ByteBuffer byteBuffer;
  protected IntBuffer intBuffer;

  protected IntBuffer colorBuffer;
  protected FloatBuffer depthBuffer;
  protected ByteBuffer stencilBuffer;

  ///////////////////////////////////////////////////////////

  // Intialization, finalization


  public PGL(PGraphicsOpenGL pg) {
    this.pg = pg;
    if (glu == null) {
      glu = new PGLU();
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
    }

    byteBuffer = allocateByteBuffer(1);
    intBuffer = allocateIntBuffer(1);
  }


  protected void setFrameRate(float framerate) {
  }


  protected void initSurface(int antialias) {
    glview = (GLSurfaceView)pg.parent.getSurfaceView();
    reqNumSamples = qualityToSamples(antialias);
    fboLayerCreated = false;
    fboLayerInUse = false;
    firstFrame = true;
  }


  protected void deleteSurface() {
    if (threadIsCurrent() && glColorTex != null) {
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
    firstFrame = true;
  }


  protected void update() {
    if (!fboLayerCreated) {
      String ext = getString(EXTENSIONS);
      if (-1 < ext.indexOf("texture_non_power_of_two")) {
        fboWidth = pg.width;
        fboHeight = pg.height;
      } else {
        fboWidth = nextPowerOfTwo(pg.width);
        fboHeight = nextPowerOfTwo(pg.height);
      }

      getIntegerv(MAX_SAMPLES, intBuffer);
      if (-1 < ext.indexOf("_framebuffer_multisample") &&
          1 < intBuffer.get(0)) {
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
      return glColorFbo.get(0);
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
    return false;
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
    Texture tex = new Texture(pg.parent);
    tex.init(pg.width, pg.height,
             glColorTex.get(backTex), TEXTURE_2D, RGBA,
             fboWidth, fboHeight, NEAREST, NEAREST,
             CLAMP_TO_EDGE, CLAMP_TO_EDGE);
    tex.invertedY(true);
    tex.colorBufferOf(pg);
    pg.setCache(pg, tex);
    return tex;
  }


  protected Texture wrapFrontTexture() {
    Texture tex = new Texture(pg.parent);
    tex.init(pg.width, pg.height,
             glColorTex.get(frontTex), TEXTURE_2D, RGBA,
             fboWidth, fboHeight, NEAREST, NEAREST,
             CLAMP_TO_EDGE, CLAMP_TO_EDGE);
    tex.invertedY(true);
    tex.colorBufferOf(pg);
    return tex;
  }


  int getBackTextureName() {
    return glColorTex.get(backTex);
  }


  int getFrontTextureName() {
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
      // at some point the use of the FBO layer, but subsequently didn't
      // request it again, then the rendering won't use the FBO layer if not
      // needed, since it is slower than simple on-screen rendering.
      FORCE_SCREEN_FBO = false;
    }
  }


  protected void endDraw(boolean clear) {
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
    return true;
  }


  protected void requestDraw() {
    if (pg.initialized && pg.parent.canDraw()) {
      glview.requestRender();
    }
  }


  protected boolean threadIsCurrent() {
    return Thread.currentThread() == glThread;
  }


  protected boolean fboLayerInUse(boolean clear0) {
    boolean cond = !clear0 || FORCE_SCREEN_FBO || 1 < numSamples;
    return cond && glColorFbo.get(0) != 0;
  }


  protected void beginGL() {
  }


  protected void endGL() {
  }


  ///////////////////////////////////////////////////////////

  // Caps query


  public String getString(int name) {
    return GLES20.glGetString(name);
  }


  public void getIntegerv(int name, IntBuffer values) {
    if (-1 < name) {
      GLES20.glGetIntegerv(name, values);
    } else {
      fillIntBuffer(values, 0, values.capacity(), 0);
    }
  }


  public void getFloatv(int name, FloatBuffer values) {
    if (-1 < name) {
      GLES20.glGetFloatv(name, values);
    } else {
      fillFloatBuffer(values, 0, values.capacity(), 0);
    }
  }


  public void getBooleanv(int name, IntBuffer values) {
    if (-1 < name) {
      GLES20.glGetBooleanv(name, values);
    } else {
      fillIntBuffer(values, 0, values.capacity(), 0);
    }
  }


  ///////////////////////////////////////////////////////////

  // Enable/disable caps


  public void enable(int cap) {
    if (-1 < cap) {
      GLES20.glEnable(cap);
    }
  }


  public void disable(int cap) {
    if (-1 < cap) {
      GLES20.glDisable(cap);
    }
  }


  ///////////////////////////////////////////////////////////

  // Render control


  public void flush() {
    GLES20.glFlush();
  }


  public void finish() {
    GLES20.glFinish();
  }


  ///////////////////////////////////////////////////////////

  // Error handling


  public int getError() {
    return GLES20.glGetError();
  }


  public String errorString(int err) {
    return GLU.gluErrorString(err);
  }


  ///////////////////////////////////////////////////////////

  // Rendering options


  public void frontFace(int mode) {
    GLES20.glFrontFace(mode);
  }


  public void cullFace(int mode) {
    GLES20.glCullFace(mode);
  }


  public void depthMask(boolean flag) {
    GLES20.glDepthMask(flag);
  }


  public void depthFunc(int func) {
    GLES20.glDepthFunc(func);
  }


  ///////////////////////////////////////////////////////////

  // Textures


  public void genTextures(int n, IntBuffer ids) {
    GLES20.glGenTextures(n, ids);
  }


  public void deleteTextures(int n, IntBuffer ids) {
    GLES20.glDeleteTextures(n, ids);
  }


  public void activeTexture(int unit) {
    GLES20.glActiveTexture(unit);
  }


  public void bindTexture(int target, int id) {
    GLES20.glBindTexture(target, id);
    if (target == TEXTURE_2D) {
      boundTextures[0] = id;
    }
  }


  public void texImage2D(int target, int level, int internalFormat,
                         int width, int height, int border, int format,
                         int type, Buffer data) {
    GLES20.glTexImage2D(target, level, internalFormat,
                        width, height, border, format, type, data);
  }


  public void texSubImage2D(int target, int level, int xOffset, int yOffset,
                            int width, int height, int format,
                            int type, Buffer data) {
    GLES20.glTexSubImage2D(target, level, xOffset, yOffset,
                           width, height, format, type, data);
  }


  public void texParameteri(int target, int param, int value) {
    GLES20.glTexParameteri(target, param, value);
  }


  public void texParameterf(int target, int param, float value) {
    GLES20.glTexParameterf(target, param, value);
  }


  public void getTexParameteriv(int target, int param, IntBuffer values) {
    GLES20.glGetTexParameteriv(target, param, values);
  }


  public void generateMipmap(int target) {
    GLES20.glGenerateMipmap(target);
  }


  ///////////////////////////////////////////////////////////

  // Vertex Buffers


  public void genBuffers(int n, IntBuffer ids) {
    GLES20.glGenBuffers(n, ids);
  }


  public void deleteBuffers(int n, IntBuffer ids) {
    GLES20.glDeleteBuffers(n, ids);
  }


  public void bindBuffer(int target, int id) {
    GLES20.glBindBuffer(target, id);
  }


  public void bufferData(int target, int size, Buffer data, int usage) {
    GLES20.glBufferData(target, size, data, usage);
  }


  public void bufferSubData(int target, int offset, int size, Buffer data) {
    GLES20.glBufferSubData(target, offset, size, data);
  }


  public void drawArrays(int mode, int first, int count) {
    GLES20.glDrawArrays(mode, first, count);
  }


  public void drawElements(int mode, int count, int type, int offset) {
    GLES20.glDrawElements(mode, count, type, offset);
  }


  public void enableVertexAttribArray(int loc) {
    GLES20.glEnableVertexAttribArray(loc);
  }


  public void disableVertexAttribArray(int loc) {
    GLES20.glDisableVertexAttribArray(loc);
  }


  public void vertexAttribPointer(int loc, int size, int type,
                                  boolean normalized, int stride, int offset) {
    GLES20.glVertexAttribPointer(loc, size, type, normalized, stride, offset);
  }


  public void vertexAttribPointer(int loc, int size, int type,
                                  boolean normalized, int stride, Buffer data) {
    GLES20.glVertexAttribPointer(loc, size, type, normalized, stride, data);
  }


  public ByteBuffer mapBuffer(int target, int access) {
    //return gl2f.glMapBuffer(GL.GL_ARRAY_BUFFER, GL2.GL_READ_WRITE);
    return null;
  }


  public ByteBuffer mapBufferRange(int target, int offset, int length,
                                   int access) {
    //return gl2x.glMapBufferRange(GL.GL_ARRAY_BUFFER, offset, length, GL2.GL_READ_WRITE);
    return null;
  }


  public void unmapBuffer(int target) {
    //gl2f.glUnmapBuffer(GL.GL_ARRAY_BUFFER);
  }


  ///////////////////////////////////////////////////////////

  // Framebuffers, renderbuffers


  public void genFramebuffers(int n, IntBuffer ids) {
    GLES20.glGenFramebuffers(n, ids);
  }


  public void deleteFramebuffers(int n, IntBuffer ids) {
    GLES20.glDeleteFramebuffers(n, ids);
  }


  public void bindFramebuffer(int target, int id) {
    GLES20.glBindFramebuffer(target, id);
  }


  public void genRenderbuffers(int n, IntBuffer ids) {
    GLES20.glGenRenderbuffers(n, ids);
  }


  public void deleteRenderbuffers(int n, IntBuffer ids) {
    GLES20.glDeleteRenderbuffers(n, ids);
  }


  public void blitFramebuffer(int srcX0, int srcY0, int srcX1, int srcY1,
                              int dstX0, int dstY0, int dstX1, int dstY1,
                              int mask, int filter) {
//    GLES20.glBlitFramebuffer(0, 0, srcW, srcH, 0, 0, destW, destH, GLES20.GL_COLOR_BUFFER_BIT, GLES20.GL_NEAREST);
  }


  public void framebufferTexture2D(int target, int attachment, int texTarget,
                                   int texId, int level) {
    GLES20.glFramebufferTexture2D(target, attachment, texTarget, texId, level);
  }


  public void bindRenderbuffer(int target, int id) {
    GLES20.glBindRenderbuffer(target, id);
  }


  public void renderbufferStorageMultisample(int target, int samples,
                                             int format, int width, int height){
//    GLES20.glRenderbufferStorageMultisample(GLES20.GL_RENDERBUFFER, samples, format, w, h);
  }


  public void renderbufferStorage(int target, int format,
                                  int width, int height) {
    GLES20.glRenderbufferStorage(target, format, width, height);
  }


  public void framebufferRenderbuffer(int target, int attachment,
                                      int rendbufTarget, int rendbufId) {
    GLES20.glFramebufferRenderbuffer(target, attachment, rendbufTarget,
                                     rendbufId);
  }


  public int checkFramebufferStatus(int target) {
    return GLES20.glCheckFramebufferStatus(target);
  }


  ///////////////////////////////////////////////////////////

  // Shaders


  public int createProgram() {
    return GLES20.glCreateProgram();
  }


  public void deleteProgram(int id) {
    GLES20.glDeleteProgram(id);
  }


  public int createShader(int type) {
    return GLES20.glCreateShader(type);
  }


  public void deleteShader(int id) {
    GLES20.glDeleteShader(id);
  }


  public void linkProgram(int prog) {
    GLES20.glLinkProgram(prog);
  }


  public void validateProgram(int prog) {
    GLES20.glValidateProgram(prog);
  }


  public void useProgram(int prog) {
    GLES20.glUseProgram(prog);
  }


  public int getAttribLocation(int prog, String name) {
    return GLES20.glGetAttribLocation(prog, name);
  }


  public int getUniformLocation(int prog, String name) {
    return GLES20.glGetUniformLocation(prog, name);
  }


  public void uniform1i(int loc, int value) {
    GLES20.glUniform1i(loc, value);
  }


  public void uniform2i(int loc, int value0, int value1) {
    GLES20.glUniform2i(loc, value0, value1);
  }


  public void uniform3i(int loc, int value0, int value1, int value2) {
    GLES20.glUniform3i(loc, value0, value1, value2);
  }


  public void uniform4i(int loc, int value0, int value1, int value2,
                                 int value3) {
    GLES20.glUniform4i(loc, value0, value1, value2, value3);
  }


  public void uniform1f(int loc, float value) {
    GLES20.glUniform1f(loc, value);
  }


  public void uniform2f(int loc, float value0, float value1) {
    GLES20.glUniform2f(loc, value0, value1);
  }


  public void uniform3f(int loc, float value0, float value1, float value2) {
    GLES20.glUniform3f(loc, value0, value1, value2);
  }


  public void uniform4f(int loc, float value0, float value1, float value2,
                                 float value3) {
    GLES20.glUniform4f(loc, value0, value1, value2, value3);
  }


  public void uniform1iv(int loc, int count, IntBuffer v) {
    GLES20.glUniform1iv(loc, count, v);
  }


  public void uniform2iv(int loc, int count, IntBuffer v) {
    GLES20.glUniform2iv(loc, count, v);
  }


  public void uniform3iv(int loc, int count, IntBuffer v) {
    GLES20.glUniform3iv(loc, count, v);
  }


  public void uniform4iv(int loc, int count, IntBuffer v) {
    GLES20.glUniform4iv(loc, count, v);
  }


  public void uniform1fv(int loc, int count, FloatBuffer v) {
    GLES20.glUniform1fv(loc, count, v);
  }


  public void uniform2fv(int loc, int count, FloatBuffer v) {
    GLES20.glUniform2fv(loc, count, v);
  }


  public void uniform3fv(int loc, int count, FloatBuffer v) {
    GLES20.glUniform3fv(loc, count, v);
  }


  public void uniform4fv(int loc, int count, FloatBuffer v) {
    GLES20.glUniform4fv(loc, count, v);
  }


  public void uniformMatrix2fv(int loc, int count, boolean transpose,
                               FloatBuffer mat) {
    GLES20.glUniformMatrix2fv(loc, count, transpose, mat);
  }


  public void uniformMatrix3fv(int loc, int count, boolean transpose,
                               FloatBuffer mat) {
    GLES20.glUniformMatrix3fv(loc, count, transpose, mat);
  }


  public void uniformMatrix4fv(int loc, int count, boolean transpose,
                               FloatBuffer mat) {
    GLES20.glUniformMatrix4fv(loc, count, transpose, mat);
  }


  public void vertexAttrib1f(int loc, float value) {
    GLES20.glVertexAttrib1f(loc, value);
  }


  public void vertexAttrib2f(int loc, float value0, float value1) {
    GLES20.glVertexAttrib2f(loc, value0, value1);
  }


  public void vertexAttrib3f(int loc, float value0, float value1, float value2){
    GLES20.glVertexAttrib3f(loc, value0, value1, value2);
  }


  public void vertexAttrib4f(int loc, float value0, float value1, float value2,
                                      float value3) {
    GLES20.glVertexAttrib4f(loc, value0, value1, value2, value3);
  }


  public void vertexAttrib1fv(int loc, FloatBuffer v) {
    GLES20.glVertexAttrib1fv(loc, v);
  }


  public void vertexAttrib2fv(int loc, FloatBuffer v) {
    GLES20.glVertexAttrib2fv(loc, v);
  }


  public void vertexAttrib3fv(int loc, FloatBuffer v) {
    GLES20.glVertexAttrib3fv(loc, v);
  }


  public void vertexAttri4fv(int loc, FloatBuffer v) {
    GLES20.glVertexAttrib4fv(loc, v);
  }


  public void shaderSource(int id, String source) {
    GLES20.glShaderSource(id, source);
  }


  public void compileShader(int id) {
    GLES20.glCompileShader(id);
  }


  public void attachShader(int prog, int shader) {
    GLES20.glAttachShader(prog, shader);
  }


  public void getShaderiv(int shader, int pname, IntBuffer params) {
    GLES20.glGetShaderiv(shader, pname, params);
  }


  public String getShaderInfoLog(int shader) {
    return GLES20.glGetShaderInfoLog(shader);
  }


  public void getProgramiv(int prog, int pname, IntBuffer params) {
    GLES20.glGetProgramiv(prog, pname, params);
  }


  public String getProgramInfoLog(int prog) {
    return GLES20.glGetProgramInfoLog(prog);
  }


  ///////////////////////////////////////////////////////////

  // Viewport


  public void viewport(int x, int y, int width, int height) {
    GLES20.glViewport(x, y, width, height);
  }


  ///////////////////////////////////////////////////////////

  // Clipping (scissor test)


  public void scissor(int x, int y, int w, int h) {
    GLES20.glScissor(x, y, w, h);
  }


  ///////////////////////////////////////////////////////////

  // Blending


  public void blendEquation(int eq) {
    GLES20.glBlendEquation(eq);
  }


  public void blendFunc(int srcFactor, int dstFactor) {
    GLES20.glBlendFunc(srcFactor, dstFactor);
  }


  ///////////////////////////////////////////////////////////

  // Pixels


  public void readBuffer(int buf) {
//    GLES20.glReadBuffer(buf);
  }


  public void readPixels(int x, int y, int width, int height, int format,
                         int type, Buffer buffer) {
    GLES20.glReadPixels(x, y, width, height, format, type, buffer);
  }


  public void drawBuffer(int buf) {
//    GLES20.glDrawBuffer(GLES20.GL_COLOR_ATTACHMENT0 + buf);
  }


  public void clearDepth(float d) {
    GLES20.glClearDepthf(d);
  }


  public void clearStencil(int s) {
    GLES20.glClearStencil(s);
  }


  public void colorMask(boolean wr, boolean wg, boolean wb, boolean wa) {
    GLES20.glColorMask(wr, wg, wb, wa);
  }


  public void clearColor(float r, float g, float b, float a) {
    GLES20.glClearColor(r, g, b, a);
  }


  public void clear(int mask) {
    GLES20.glClear(mask);
  }


  ///////////////////////////////////////////////////////////

  // Context interface


  protected int createEmptyContext() {
    return -1;
  }


  protected int getCurrentContext() {
    return context.hashCode();
  }


  ///////////////////////////////////////////////////////////

  // Tessellator interface


  protected Tessellator createTessellator(TessellatorCallback callback) {
    return new Tessellator(callback);
  }


  protected class Tessellator {
    protected PGLUtessellator tess;
    protected TessellatorCallback callback;
    protected GLUCallback gluCallback;

    public Tessellator(TessellatorCallback callback) {
      this.callback = callback;
      tess = PGLU.gluNewTess();
      gluCallback = new GLUCallback();

      PGLU.gluTessCallback(tess, PGLU.GLU_TESS_BEGIN, gluCallback);
      PGLU.gluTessCallback(tess, PGLU.GLU_TESS_END, gluCallback);
      PGLU.gluTessCallback(tess, PGLU.GLU_TESS_VERTEX, gluCallback);
      PGLU.gluTessCallback(tess, PGLU.GLU_TESS_COMBINE, gluCallback);
      PGLU.gluTessCallback(tess, PGLU.GLU_TESS_ERROR, gluCallback);
    }

    public void beginPolygon() {
      PGLU.gluTessBeginPolygon(tess, null);
    }

    public void endPolygon() {
      PGLU.gluTessEndPolygon(tess);
    }

    public void setWindingRule(int rule) {
      PGLU.gluTessProperty(tess, PGLU.GLU_TESS_WINDING_RULE, rule);
    }

    public void beginContour() {
      PGLU.gluTessBeginContour(tess);
    }

    public void endContour() {
      PGLU.gluTessEndContour(tess);
    }

    public void addVertex(double[] v) {
      PGLU.gluTessVertex(tess, v, 0, v);
    }

    protected class GLUCallback extends PGLUtessellatorCallbackAdapter {
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
    return PGLU.gluErrorString(err);
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
    return other == -1 || other == context.hashCode();
  }


  protected void enableTexturing(int target) {
    if (target == TEXTURE_2D) {
      texturingTargets[0] = true;
    }
  }


  protected void disableTexturing(int target) {
    if (target == TEXTURE_2D) {
      texturingTargets[0] = false;
    }
  }


  protected boolean texturingIsEnabled(int target) {
    if (target == TEXTURE_2D) {
      return texturingTargets[0];
    } else {
      return false;
    }
  }


  protected boolean textureIsBound(int target, int id) {
    if (target == TEXTURE_2D) {
      return boundTextures[0] == id;
    } else {
      return false;
    }
  }


  protected void initTexture(int target, int format, int width, int height) {
    initTexture(target, format, width, height, 0);
  }


  protected void initTexture(int target, int format, int width, int height,
                             int initColor) {
    // Doing in patches of 16x16 pixels to avoid creating a (potentially)
    // very large transient array which in certain situations (memory-
    // constrained android devices) might lead to an out-of-memory error.
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
    if (!loadedTexShader ||
        texShaderContext.hashCode() != context.hashCode()) {
      texVertShader = createShader(VERTEX_SHADER, texVertShaderSource);
      texFragShader = createShader(FRAGMENT_SHADER, texFragShaderSource);
      if (0 < texVertShader && 0 < texFragShader) {
        texShaderProgram = createProgram(texVertShader, texFragShader);
      }
      if (0 < texShaderProgram) {
        texVertLoc = getAttribLocation(texShaderProgram, "inVertex");
        texTCoordLoc = getAttribLocation(texShaderProgram, "inTexcoord");
      }
      loadedTexShader = true;
      texShaderContext = context;
    }

    if (texData == null) {
      texData = allocateDirectFloatBuffer(texCoords.length);
    }

    if (0 < texShaderProgram) {
      // The texture overwrites anything drawn earlier.
      boolean depthTest = getDepthTest();
      disable(DEPTH_TEST);

      // When drawing the texture we don't write to the
      // depth mask, so the texture remains in the background
      // and can be occluded by anything drawn later, even if
      // if it is behind it.
      boolean depthMask = getDepthWriteMask();
      depthMask(false);

      useProgram(texShaderProgram);

      enableVertexAttribArray(texVertLoc);
      enableVertexAttribArray(texTCoordLoc);

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
      bindTexture(target, id);

      bindBuffer(ARRAY_BUFFER, 0); // Making sure that no VBO is bound at this point.

      texData.position(0);
      vertexAttribPointer(texVertLoc, 2, FLOAT, false, 4 * SIZEOF_FLOAT,
                          texData);
      texData.position(2);
      vertexAttribPointer(texTCoordLoc, 2, FLOAT, false, 4 * SIZEOF_FLOAT,
                          texData);

      drawArrays(TRIANGLE_STRIP, 0, 4);

      bindTexture(target, 0);
      if (enabledTex) {
        disableTexturing(TEXTURE_2D);
      }

      disableVertexAttribArray(texVertLoc);
      disableVertexAttribArray(texTCoordLoc);

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
    // http://stackoverflow.com/questions/2596682/opengl-es-2-0-read-depth-buffer
    return 0;
  }


  protected byte getStencilValue(int scrX, int scrY) {
    return 0;
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
      System.err.println(String.format(FRAMEBUFFER_ERROR_MESSAGE,
                                       "incomplete attachment"));
    } else if (status == FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT) {
      System.err.println(String.format(FRAMEBUFFER_ERROR_MESSAGE,
                                       "incomplete missing attachment"));
    } else if (status == FRAMEBUFFER_INCOMPLETE_DIMENSIONS) {
      System.err.println(String.format(FRAMEBUFFER_ERROR_MESSAGE,
                                       "incomplete dimensions"));
    } else if (status == FRAMEBUFFER_INCOMPLETE_FORMATS) {
      System.err.println(String.format(FRAMEBUFFER_ERROR_MESSAGE,
                                       "incomplete formats"));
    } else if (status == FRAMEBUFFER_UNSUPPORTED) {
      System.err.println(String.format(FRAMEBUFFER_ERROR_MESSAGE,
                                       "framebuffer unsupported"));
    } else {
      System.err.println(String.format(FRAMEBUFFER_ERROR_MESSAGE,
                                       "unknown error"));
    }
    return false;
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


  protected static void updateByteBuffer(ByteBuffer buf, byte[] arr,
                                         int offset, int size) {
    if (USE_DIRECT_BUFFERS || (buf.hasArray() && buf.array() != arr)) {
      buf.position(offset);
      buf.put(arr, offset, size);
      buf.rewind();
    }
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


  protected static void updateShortBuffer(ShortBuffer buf, short[] arr,
                                          int offset, int size) {
    if (USE_DIRECT_BUFFERS || (buf.hasArray() && buf.array() != arr)) {
      buf.position(offset);
      buf.put(arr, offset, size);
      buf.rewind();
    }
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


  protected static void updateIntBuffer(IntBuffer buf, int[] arr,
                                        int offset, int size) {
     if (USE_DIRECT_BUFFERS || (buf.hasArray() && buf.array() != arr)) {
       buf.position(offset);
       buf.put(arr, offset, size);
       buf.rewind();
     }
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


  protected static void updateFloatBuffer(FloatBuffer buf, float[] arr,
                                        int offset, int size) {
     if (USE_DIRECT_BUFFERS || (buf.hasArray() && buf.array() != arr)) {
       buf.position(offset);
       buf.put(arr, offset, size);
       buf.rewind();
     }
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

  // Android specific stuff


  public AndroidRenderer getRenderer() {
    renderer = new AndroidRenderer();
    return renderer;
  }


  public AndroidContextFactory getContextFactory() {
    return new AndroidContextFactory();
  }


  public AndroidConfigChooser getConfigChooser(int r, int g, int b, int a,
                                               int d, int s) {
    return new AndroidConfigChooser(r, g, b, a, d, s);
  }


  protected class AndroidRenderer implements Renderer {
    public AndroidRenderer() {
    }

    public void onDrawFrame(GL10 igl) {
      gl = igl;
      glThread = Thread.currentThread();
      pg.parent.handleDraw();
    }

    public void onSurfaceChanged(GL10 igl, int iwidth, int iheight) {
      gl = igl;

      // Here is where we should initialize native libs...
      // lib.init(iwidth, iheight);

      pg.setSize(iwidth, iheight);
    }

    public void onSurfaceCreated(GL10 igl, EGLConfig config) {
      gl = igl;
      context = ((EGL10)EGLContext.getEGL()).eglGetCurrentContext();
    }
  }


  protected class AndroidContextFactory implements
    GLSurfaceView.EGLContextFactory {
    public EGLContext createContext(EGL10 egl, EGLDisplay display,
        EGLConfig eglConfig) {
      int[] attrib_list = { EGL_CONTEXT_CLIENT_VERSION, 2,
                            EGL10.EGL_NONE };
      EGLContext context = egl.eglCreateContext(display, eglConfig,
                                                EGL10.EGL_NO_CONTEXT,
                                                attrib_list);
      return context;
    }

    public void destroyContext(EGL10 egl, EGLDisplay display,
                               EGLContext context) {
      egl.eglDestroyContext(display, context);
    }
  }


  protected class AndroidConfigChooser implements EGLConfigChooser {
    // Desired size (in bits) for the rgba color, depth and stencil buffers.
    public int redTarget;
    public int greenTarget;
    public int blueTarget;
    public int alphaTarget;
    public int depthTarget;
    public int stencilTarget;

    // Actual rgba color, depth and stencil sizes (in bits) supported by the
    // device.
    public int redBits;
    public int greenBits;
    public int blueBits;
    public int alphaBits;
    public int depthBits;
    public int stencilBits;
    public int[] tempValue = new int[1];

    // The attributes we want in the frame buffer configuration for Processing.
    // For more details on other attributes, see:
    // http://www.khronos.org/opengles/documentation/opengles1_0/html/eglChooseConfig.html
    protected int[] configAttribsGL = { EGL10.EGL_RED_SIZE, 4,
                                        EGL10.EGL_GREEN_SIZE, 4,
                                        EGL10.EGL_BLUE_SIZE, 4,
                                        EGL10.EGL_RENDERABLE_TYPE,
                                        EGL_OPENGL_ES2_BIT,
                                        EGL10.EGL_NONE };

    public AndroidConfigChooser(int r, int g, int b, int a, int d, int s) {
      redTarget = r;
      greenTarget = g;
      blueTarget = b;
      alphaTarget = a;
      depthTarget = d;
      stencilTarget = s;
    }

    public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display) {

      // Get the number of minimally matching EGL configurations
      int[] num_config = new int[1];
      egl.eglChooseConfig(display, configAttribsGL, null, 0, num_config);

      int numConfigs = num_config[0];

      if (numConfigs <= 0) {
        throw new IllegalArgumentException("No EGL configs match configSpec");
      }

      // Allocate then read the array of minimally matching EGL configs
      EGLConfig[] configs = new EGLConfig[numConfigs];
      egl.eglChooseConfig(display, configAttribsGL, configs, numConfigs,
          num_config);

      if (PApplet.DEBUG) {
        for (EGLConfig config : configs) {
          String configStr = "P3D - selected EGL config : "
            + printConfig(egl, display, config);
          System.out.println(configStr);
        }
      }

      // Now return the configuration that best matches the target one.
      return chooseBestConfig(egl, display, configs);
    }

    public EGLConfig chooseBestConfig(EGL10 egl, EGLDisplay display,
                                      EGLConfig[] configs) {
      EGLConfig bestConfig = null;
      float bestScore = 1000;

      for (EGLConfig config : configs) {
        int gl = findConfigAttrib(egl, display, config,
                                  EGL10.EGL_RENDERABLE_TYPE, 0);
        if (gl == EGL_OPENGL_ES2_BIT) {
          int d = findConfigAttrib(egl, display, config,
                                   EGL10.EGL_DEPTH_SIZE, 0);
          int s = findConfigAttrib(egl, display, config,
                                   EGL10.EGL_STENCIL_SIZE, 0);

          int r = findConfigAttrib(egl, display, config,
                                   EGL10.EGL_RED_SIZE, 0);
          int g = findConfigAttrib(egl, display, config,
                                   EGL10.EGL_GREEN_SIZE, 0);
          int b = findConfigAttrib(egl, display, config,
                                   EGL10.EGL_BLUE_SIZE, 0);
          int a = findConfigAttrib(egl, display, config,
                                   EGL10.EGL_ALPHA_SIZE, 0);

          float score = 0.20f * PApplet.abs(r - redTarget) +
                        0.20f * PApplet.abs(g - greenTarget) +
                        0.20f * PApplet.abs(b - blueTarget) +
                        0.15f * PApplet.abs(a - blueTarget) +
                        0.15f * PApplet.abs(d - depthTarget) +
                        0.10f * PApplet.abs(s - stencilTarget);

          if (score < bestScore) {
            // We look for the config closest to the target config.
            // Closeness is measured by the score function defined above:
            // we give more weight to the RGB components, followed by the
            // alpha, depth and finally stencil bits.
            bestConfig = config;
            bestScore = score;

            redBits = r;
            greenBits = g;
            blueBits = b;
            alphaBits = a;
            depthBits = d;
            stencilBits = s;
          }
        }
      }

      if (PApplet.DEBUG) {
        String configStr = "P3D - selected EGL config : "
          + printConfig(egl, display, bestConfig);
        System.out.println(configStr);
      }
      return bestConfig;
    }

    protected String printConfig(EGL10 egl, EGLDisplay display,
                                 EGLConfig config) {
      int r = findConfigAttrib(egl, display, config,
                               EGL10.EGL_RED_SIZE, 0);
      int g = findConfigAttrib(egl, display, config,
                               EGL10.EGL_GREEN_SIZE, 0);
      int b = findConfigAttrib(egl, display, config,
                               EGL10.EGL_BLUE_SIZE, 0);
      int a = findConfigAttrib(egl, display, config,
                               EGL10.EGL_ALPHA_SIZE, 0);
      int d = findConfigAttrib(egl, display, config,
                               EGL10.EGL_DEPTH_SIZE, 0);
      int s = findConfigAttrib(egl, display, config,
                               EGL10.EGL_STENCIL_SIZE, 0);
      int type = findConfigAttrib(egl, display, config,
                                  EGL10.EGL_RENDERABLE_TYPE, 0);
      int nat = findConfigAttrib(egl, display, config,
                                 EGL10.EGL_NATIVE_RENDERABLE, 0);
      int bufSize = findConfigAttrib(egl, display, config,
                                     EGL10.EGL_BUFFER_SIZE, 0);
      int bufSurf = findConfigAttrib(egl, display, config,
                                     EGL10.EGL_RENDER_BUFFER, 0);

      return String.format("EGLConfig rgba=%d%d%d%d depth=%d stencil=%d",
                           r,g,b,a,d,s)
        + " type=" + type
        + " native=" + nat
        + " buffer size=" + bufSize
        + " buffer surface=" + bufSurf +
        String.format(" caveat=0x%04x",
                      findConfigAttrib(egl, display, config,
                                       EGL10.EGL_CONFIG_CAVEAT, 0));
    }

    protected int findConfigAttrib(EGL10 egl, EGLDisplay display,
      EGLConfig config, int attribute, int defaultValue) {
      if (egl.eglGetConfigAttrib(display, config, attribute, tempValue)) {
        return tempValue[0];
      }
      return defaultValue;
    }
  }
}
