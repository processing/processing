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

package processing.opengl;

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

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GL2ES2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLCapabilitiesImmutable;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLDrawable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLException;
import javax.media.opengl.GLFBODrawable;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLCanvas;
import javax.media.opengl.glu.GLU;
import javax.media.opengl.glu.GLUtessellator;
import javax.media.opengl.glu.GLUtessellatorCallbackAdapter;

import processing.core.PApplet;
import processing.core.PConstants;
import processing.event.KeyEvent;
import processing.event.MouseEvent;

import com.jogamp.newt.awt.NewtCanvasAWT;
import com.jogamp.newt.event.InputEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.FBObject;

/**
 * Processing-OpenGL abstraction layer.
 *
 * Warnings are suppressed for static access because presumably on Android,
 * the GL2 vs GL distinctions are necessary, whereas on desktop they are not.
 */
@SuppressWarnings("static-access")
public class PGL {

  ///////////////////////////////////////////////////////////

  // Parameters

  public static final boolean USE_JOGL_FBOLAYER      = false;
  public static boolean FORCE_SCREEN_FBO             = false;
  public static final boolean USE_DIRECT_BUFFERS     = true;
  public static final int MIN_DIRECT_BUFFER_SIZE     = 1;
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

  /** JOGL's windowing toolkit */
  // The two windowing toolkits available to use in JOGL:
  protected static final int AWT  = 0; // http://jogamp.org/wiki/index.php/Using_JOGL_in_AWT_SWT_and_Swing
  protected static final int NEWT = 1; // http://jogamp.org/jogl/doc/NEWT-Overview.html

  protected static int toolkit;
  protected static int events;
  static {
    if (PApplet.platform == PConstants.WINDOWS) {
      // Using AWT on Windows because NEWT displays a black background while
      // initializing, and the cursor functions don't work. GLWindow has some
      // functions for basic cursor handling (hide/show):
      // GLWindow.setPointerVisible(false);
      // but apparently nothing to set the cursor icon:
      // https://jogamp.org/bugzilla/show_bug.cgi?id=409
      toolkit = AWT;
      events = AWT;
    } else if (PApplet.platform == PConstants.MACOSX) {
      // NEWT solves the issues with Java 7 and OS X 10.7+: calls to frame
      // hanging the sketch, as well as cursor, etc.
      toolkit = NEWT;
      events = AWT;
    } else if (PApplet.platform == PConstants.LINUX) {
      toolkit = NEWT; // AWT extremely broken on Linux?
      events = NEWT;
    } else if (PApplet.platform == PConstants.OTHER) {
      toolkit = NEWT; // NEWT should work on the Raspberry pi
      events = NEWT;
    }
  }

  protected static int request_depth_bits = 24;
  protected static int request_stencil_bits = 8;
  protected static int request_alpha_bits = 8;

  protected static final int SIZEOF_SHORT = Short.SIZE / 8;
  protected static final int SIZEOF_INT = Integer.SIZE / 8;
  protected static final int SIZEOF_FLOAT = Float.SIZE / 8;
  protected static final int SIZEOF_BYTE = Byte.SIZE / 8;
  protected static final int SIZEOF_INDEX = SIZEOF_SHORT;
  protected static final int INDEX_TYPE = GL.GL_UNSIGNED_SHORT;

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

  public static final int FALSE = GL.GL_FALSE;
  public static final int TRUE  = GL.GL_TRUE;

  public static final int LESS      = GL.GL_LESS;
  public static final int LEQUAL    = GL.GL_LEQUAL;

  public static final int CCW       = GL.GL_CCW;
  public static final int CW        = GL.GL_CW;

  public static final int CULL_FACE      = GL.GL_CULL_FACE;
  public static final int FRONT          = GL.GL_FRONT;
  public static final int BACK           = GL.GL_BACK;
  public static final int FRONT_AND_BACK = GL.GL_FRONT_AND_BACK;

  public static final int VIEWPORT = GL.GL_VIEWPORT;

  public static final int SCISSOR_TEST    = GL.GL_SCISSOR_TEST;
  public static final int DEPTH_TEST      = GL.GL_DEPTH_TEST;
  public static final int DEPTH_WRITEMASK = GL.GL_DEPTH_WRITEMASK;

  public static final int COLOR_BUFFER_BIT   = GL.GL_COLOR_BUFFER_BIT;
  public static final int DEPTH_BUFFER_BIT   = GL.GL_DEPTH_BUFFER_BIT;
  public static final int STENCIL_BUFFER_BIT = GL.GL_STENCIL_BUFFER_BIT;

  public static final int FUNC_ADD              = GL.GL_FUNC_ADD;
  public static final int FUNC_MIN              = GL2.GL_MIN;
  public static final int FUNC_MAX              = GL2.GL_MAX;
  public static final int FUNC_REVERSE_SUBTRACT = GL.GL_FUNC_REVERSE_SUBTRACT;

  public static final int TEXTURE_2D        = GL.GL_TEXTURE_2D;
  public static final int TEXTURE_RECTANGLE = GL2.GL_TEXTURE_RECTANGLE;

  public static final int TEXTURE_BINDING_2D        = GL.GL_TEXTURE_BINDING_2D;
  public static final int TEXTURE_BINDING_RECTANGLE =
    GL2.GL_TEXTURE_BINDING_RECTANGLE;

  public static final int RGB            = GL.GL_RGB;
  public static final int RGBA           = GL.GL_RGBA;
  public static final int ALPHA          = GL.GL_ALPHA;
  public static final int UNSIGNED_INT   = GL.GL_UNSIGNED_INT;
  public static final int UNSIGNED_BYTE  = GL.GL_UNSIGNED_BYTE;
  public static final int UNSIGNED_SHORT = GL.GL_UNSIGNED_SHORT;
  public static final int FLOAT          = GL.GL_FLOAT;

  public static final int NEAREST               = GL.GL_NEAREST;
  public static final int LINEAR                = GL.GL_LINEAR;
  public static final int LINEAR_MIPMAP_NEAREST = GL.GL_LINEAR_MIPMAP_NEAREST;
  public static final int LINEAR_MIPMAP_LINEAR  = GL.GL_LINEAR_MIPMAP_LINEAR;

  public static final int TEXTURE_MAX_ANISOTROPY =
    GL.GL_TEXTURE_MAX_ANISOTROPY_EXT;
  public static final int MAX_TEXTURE_MAX_ANISOTROPY =
    GL.GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT;

  public static final int CLAMP_TO_EDGE = GL.GL_CLAMP_TO_EDGE;
  public static final int REPEAT        = GL.GL_REPEAT;

  public static final int RGBA8            = GL.GL_RGBA8;
  public static final int DEPTH24_STENCIL8 = GL.GL_DEPTH24_STENCIL8;

  public static final int DEPTH_COMPONENT   = GL2.GL_DEPTH_COMPONENT;
  public static final int DEPTH_COMPONENT16 = GL.GL_DEPTH_COMPONENT16;
  public static final int DEPTH_COMPONENT24 = GL.GL_DEPTH_COMPONENT24;
  public static final int DEPTH_COMPONENT32 = GL.GL_DEPTH_COMPONENT32;

  public static final int STENCIL_INDEX  = GL2.GL_STENCIL_INDEX;
  public static final int STENCIL_INDEX1 = GL.GL_STENCIL_INDEX1;
  public static final int STENCIL_INDEX4 = GL.GL_STENCIL_INDEX4;
  public static final int STENCIL_INDEX8 = GL.GL_STENCIL_INDEX8;

  public static final int ARRAY_BUFFER         = GL.GL_ARRAY_BUFFER;
  public static final int ELEMENT_ARRAY_BUFFER = GL.GL_ELEMENT_ARRAY_BUFFER;

  public static final int SAMPLES = GL.GL_SAMPLES;

  public static final int FRAMEBUFFER_COMPLETE                      =
    GL.GL_FRAMEBUFFER_COMPLETE;
  public static final int FRAMEBUFFER_INCOMPLETE_ATTACHMENT         =
    GL.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT;
  public static final int FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT =
    GL.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT;
  public static final int FRAMEBUFFER_INCOMPLETE_DIMENSIONS         =
    GL.GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS;
  public static final int FRAMEBUFFER_INCOMPLETE_FORMATS            =
    GL.GL_FRAMEBUFFER_INCOMPLETE_FORMATS;
  public static final int FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER        =
    GL2.GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER;
  public static final int FRAMEBUFFER_INCOMPLETE_READ_BUFFER        =
    GL2.GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER;
  public static final int FRAMEBUFFER_UNSUPPORTED                   =
    GL.GL_FRAMEBUFFER_UNSUPPORTED;

  public static final int STATIC_DRAW  = GL.GL_STATIC_DRAW;
  public static final int DYNAMIC_DRAW = GL.GL_DYNAMIC_DRAW;
  public static final int STREAM_DRAW  = GL2.GL_STREAM_DRAW;

  public static final int READ_ONLY  = GL2.GL_READ_ONLY;
  public static final int WRITE_ONLY = GL2.GL_WRITE_ONLY;
  public static final int READ_WRITE = GL2.GL_READ_WRITE;

  public static final int TRIANGLE_FAN   = GL.GL_TRIANGLE_FAN;
  public static final int TRIANGLE_STRIP = GL.GL_TRIANGLE_STRIP;
  public static final int TRIANGLES      = GL.GL_TRIANGLES;

  public static final int VENDOR                   = GL.GL_VENDOR;
  public static final int RENDERER                 = GL.GL_RENDERER;
  public static final int VERSION                  = GL.GL_VERSION;
  public static final int EXTENSIONS               = GL.GL_EXTENSIONS;
  public static final int SHADING_LANGUAGE_VERSION =
    GL2ES2.GL_SHADING_LANGUAGE_VERSION;

  public static final int MAX_TEXTURE_SIZE         = GL.GL_MAX_TEXTURE_SIZE;
  public static final int MAX_SAMPLES              = GL2.GL_MAX_SAMPLES;
  public static final int ALIASED_LINE_WIDTH_RANGE =
    GL.GL_ALIASED_LINE_WIDTH_RANGE;
  public static final int ALIASED_POINT_SIZE_RANGE =
    GL.GL_ALIASED_POINT_SIZE_RANGE;
  public static final int DEPTH_BITS = GL.GL_DEPTH_BITS;
  public static final int STENCIL_BITS = GL.GL_STENCIL_BITS;

  public static final int TESS_WINDING_NONZERO = GLU.GLU_TESS_WINDING_NONZERO;
  public static final int TESS_WINDING_ODD     = GLU.GLU_TESS_WINDING_ODD;

  public static final int TEXTURE0           = GL.GL_TEXTURE0;
  public static final int TEXTURE1           = GL.GL_TEXTURE1;
  public static final int TEXTURE2           = GL.GL_TEXTURE2;
  public static final int TEXTURE3           = GL.GL_TEXTURE3;
  public static final int TEXTURE_MIN_FILTER = GL.GL_TEXTURE_MIN_FILTER;
  public static final int TEXTURE_MAG_FILTER = GL.GL_TEXTURE_MAG_FILTER;
  public static final int TEXTURE_WRAP_S     = GL.GL_TEXTURE_WRAP_S;
  public static final int TEXTURE_WRAP_T     = GL.GL_TEXTURE_WRAP_T;

  public static final int BLEND               = GL.GL_BLEND;
  public static final int ONE                 = GL.GL_ONE;
  public static final int ZERO                = GL.GL_ZERO;
  public static final int SRC_ALPHA           = GL.GL_SRC_ALPHA;
  public static final int DST_ALPHA           = GL.GL_DST_ALPHA;
  public static final int ONE_MINUS_SRC_ALPHA = GL.GL_ONE_MINUS_SRC_ALPHA;
  public static final int ONE_MINUS_DST_COLOR = GL.GL_ONE_MINUS_DST_COLOR;
  public static final int ONE_MINUS_SRC_COLOR = GL.GL_ONE_MINUS_SRC_COLOR;
  public static final int DST_COLOR           = GL.GL_DST_COLOR;
  public static final int SRC_COLOR           = GL.GL_SRC_COLOR;

  public static final int FRAMEBUFFER        = GL.GL_FRAMEBUFFER;
  public static final int COLOR_ATTACHMENT0  = GL.GL_COLOR_ATTACHMENT0;
  public static final int COLOR_ATTACHMENT1  = GL2.GL_COLOR_ATTACHMENT1;
  public static final int COLOR_ATTACHMENT2  = GL2.GL_COLOR_ATTACHMENT2;
  public static final int COLOR_ATTACHMENT3  = GL2.GL_COLOR_ATTACHMENT3;
  public static final int RENDERBUFFER       = GL.GL_RENDERBUFFER;
  public static final int DEPTH_ATTACHMENT   = GL.GL_DEPTH_ATTACHMENT;
  public static final int STENCIL_ATTACHMENT = GL.GL_STENCIL_ATTACHMENT;
  public static final int READ_FRAMEBUFFER   = GL2.GL_READ_FRAMEBUFFER;
  public static final int DRAW_FRAMEBUFFER   = GL2.GL_DRAW_FRAMEBUFFER;

  public static final int VERTEX_SHADER        = GL2.GL_VERTEX_SHADER;
  public static final int FRAGMENT_SHADER      = GL2.GL_FRAGMENT_SHADER;
  public static final int INFO_LOG_LENGTH      = GL2.GL_INFO_LOG_LENGTH;
  public static final int SHADER_SOURCE_LENGTH = GL2.GL_SHADER_SOURCE_LENGTH;
  public static final int COMPILE_STATUS       = GL2.GL_COMPILE_STATUS;
  public static final int LINK_STATUS          = GL2.GL_LINK_STATUS;
  public static final int VALIDATE_STATUS      = GL2.GL_VALIDATE_STATUS;

  public static final int MULTISAMPLE    = GL.GL_MULTISAMPLE;
  public static final int POINT_SMOOTH   = GL2.GL_POINT_SMOOTH;
  public static final int LINE_SMOOTH    = GL.GL_LINE_SMOOTH;
  public static final int POLYGON_SMOOTH = GL2.GL_POLYGON_SMOOTH;

  /** Basic GL functionality, common to all profiles */
  public static GL gl;

  /** GLU interface **/
  public static GLU glu;

  /** The rendering context (holds rendering state info) */
  public static GLContext context;

  /** The canvas where OpenGL rendering takes place */
  public static Canvas canvas;

  /** Selected GL profile */
  public static GLProfile profile;

  /** OpenGL thread */
  protected static Thread glThread;

  /** The PGraphics object using this interface */
  protected PGraphicsOpenGL pg;

  /** The capabilities of the OpenGL rendering surface */
  protected static GLCapabilitiesImmutable capabilities;

  /** The rendering surface */
  protected static GLDrawable drawable;

  /** GLES2 functionality (shaders, etc) */
  protected static GL2ES2 gl2;

  /** GL2 desktop functionality (blit framebuffer, map buffer range,
   * multisampled renerbuffers) */
  protected static GL2 gl2x;

  /** The AWT-OpenGL canvas */
  protected static GLCanvas canvasAWT;

  /** The NEWT-OpenGL canvas */
  protected static NewtCanvasAWT canvasNEWT;

  /** The NEWT window */
  protected static GLWindow window;

  /** The listener that fires the frame rendering in Processing */
  protected static PGLListener listener;

  /** Desired target framerate */
  protected float targetFramerate = 60;
  protected boolean setFramerate = false;

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

  /** Back (== draw, current frame) buffer */
  protected static FBObject backFBO;
  /** Sink buffer, used in the multisampled case */
  protected static FBObject sinkFBO;
  /** Front (== read, previous frame) buffer */
  protected static FBObject frontFBO;
  protected static FBObject.TextureAttachment backTexAttach;
  protected static FBObject.TextureAttachment frontTexAttach;

  ///////////////////////////////////////////////////////////

  // Texture rendering

  protected static boolean loadedTex2DShader = false;
  protected static int tex2DShaderProgram;
  protected static int tex2DVertShader;
  protected static int tex2DFragShader;
  protected static GLContext tex2DShaderContext;
  protected static int tex2DVertLoc;
  protected static int tex2DTCoordLoc;

  protected static boolean loadedTexRectShader = false;
  protected static int texRectShaderProgram;
  protected static int texRectVertShader;
  protected static int texRectFragShader;
  protected static GLContext texRectShaderContext;
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

  protected float[] projMatrix;
  protected float[] mvMatrix;


  ///////////////////////////////////////////////////////////

  // Initialization, finalization


  public PGL() {
  }


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
    }

    byteBuffer = allocateByteBuffer(1);
    intBuffer = allocateIntBuffer(1);
  }


  protected void setFrameRate(float framerate) {
    if (targetFramerate != framerate) {
      if (60 < framerate) {
        // Disables v-sync
        gl.setSwapInterval(0);
      } else if (30 < framerate) {
        gl.setSwapInterval(1);
      } else {
        gl.setSwapInterval(2);
      }
      if ((60 < framerate && targetFramerate <= 60) ||
          (framerate <= 60 && 60 < targetFramerate)) {
        // Enabling/disabling v-sync, we force a
        // surface reinitialization to avoid screen
        // no-paint issue observed on MacOSX.
        pg.initialized = false;
      }
      targetFramerate = framerate;
      setFramerate = true;
    }
  }


  protected void initSurface(int antialias) {
    if (profile == null) {
      profile = GLProfile.getDefault();
    } else {
      // Restarting...
      if (canvasAWT != null) {
        canvasAWT.removeGLEventListener(listener);
        pg.parent.removeListeners(canvasAWT);
        pg.parent.remove(canvasAWT);
      } else if (canvasNEWT != null) {
        window.removeGLEventListener(listener);
        pg.parent.remove(canvasNEWT);
      }
      sinkFBO = backFBO = frontFBO = null;
      setFramerate = false;
    }

    // Setting up the desired GL capabilities;
    GLCapabilities caps = new GLCapabilities(profile);
    caps.setBackgroundOpaque(true);
    caps.setOnscreen(true);
    if (USE_JOGL_FBOLAYER) {
      if (1 < antialias) {
        caps.setSampleBuffers(true);
        caps.setNumSamples(antialias);
      } else {
        caps.setSampleBuffers(false);
      }
    } else {
      caps.setSampleBuffers(false);
      reqNumSamples = qualityToSamples(antialias);
    }
    caps.setDepthBits(request_depth_bits);
    caps.setStencilBits(request_stencil_bits);
    caps.setAlphaBits(request_alpha_bits);
    caps.setDefaultColor(javaToNativeARGB(pg.backgroundColor));

    if (toolkit == AWT) {
      canvasAWT = new GLCanvas(caps);
      canvasAWT.setBounds(0, 0, pg.width, pg.height);
      canvasAWT.setBackground(new Color(pg.backgroundColor, true));
      canvasAWT.setFocusable(true);

      pg.parent.setLayout(new BorderLayout());
      pg.parent.add(canvasAWT, BorderLayout.CENTER);
      pg.parent.validate();
      pg.parent.removeListeners(pg.parent);
      pg.parent.addListeners(canvasAWT);

      canvas = canvasAWT;
      canvasNEWT = null;

      listener = new PGLListener();
      canvasAWT.addGLEventListener(listener);
    } else if (toolkit == NEWT) {
      window = GLWindow.create(caps);
      canvasNEWT = new NewtCanvasAWT(window);
      canvasNEWT.setBounds(0, 0, pg.width, pg.height);
      canvasNEWT.setBackground(new Color(pg.backgroundColor, true));
      canvasNEWT.setFocusable(true);

      pg.parent.setLayout(new BorderLayout());
      pg.parent.add(canvasNEWT, BorderLayout.CENTER);
      pg.parent.validate();

      if (events == NEWT) {
        NEWTMouseListener mouseListener = new NEWTMouseListener();
        window.addMouseListener(mouseListener);
        NEWTKeyListener keyListener = new NEWTKeyListener();
        window.addKeyListener(keyListener);
        NEWTWindowListener winListener = new NEWTWindowListener();
        window.addWindowListener(winListener);
        canvasNEWT.addFocusListener(pg.parent); // So focus detection work.
      } else if (events == AWT) {
        pg.parent.removeListeners(canvasNEWT);
        pg.parent.addListeners(canvasNEWT);
      }

      canvas = canvasNEWT;
      canvasAWT = null;

      listener = new PGLListener();
      window.addGLEventListener(listener);
    }

    fboLayerCreated = false;
    fboLayerInUse = false;
    firstFrame = true;
  }


  protected void deleteSurface() {
    if (threadIsCurrent() && fboLayerCreated) {
      deleteTextures(2, glColorTex);
      deleteFramebuffers(1, glColorFbo);
      deleteFramebuffers(1, glMultiFbo);
      deleteRenderbuffers(1, glColorBuf);
      deleteRenderbuffers(1, glDepthStencil);
      deleteRenderbuffers(1, glDepth);
      deleteRenderbuffers(1, glStencil);
    }

    if (canvasAWT != null) {
      canvasAWT.removeGLEventListener(listener);
      pg.parent.removeListeners(canvasAWT);
    } else if (canvasNEWT != null) {
      window.removeGLEventListener(listener);
    }

    fboLayerCreated = false;
    fboLayerInUse = false;
    firstFrame = false;

    GLProfile.shutdown();
  }


  protected void update() {
    if (!setFramerate) {
      setFrameRate(targetFramerate);
    }
    if (USE_JOGL_FBOLAYER) return;

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
    } else if (capabilities.isFBO()) {
      return context.getDefaultReadFramebuffer();
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
    } else if (capabilities.isFBO()) {
      return context.getDefaultDrawFramebuffer();
    } else {
      return 0;
    }
  }


  protected int getDefaultDrawBuffer() {
    if (fboLayerInUse) {
      return COLOR_ATTACHMENT0;
    } else if (capabilities.isFBO()) {
      return GL.GL_COLOR_ATTACHMENT0;
    } else if (capabilities.getDoubleBuffered()) {
      return GL.GL_BACK;
    } else {
      return GL.GL_FRONT;
    }
  }


  protected int getDefaultReadBuffer() {
    if (fboLayerInUse) {
      return COLOR_ATTACHMENT0;
    } else if (capabilities.isFBO()) {
      return GL.GL_COLOR_ATTACHMENT0;
    } else if (capabilities.getDoubleBuffered()) {
      return GL.GL_BACK;
    } else {
      return GL.GL_FRONT;
    }
  }


  protected boolean isFBOBacked() {
    return fboLayerInUse || capabilities.isFBO();
  }


  protected void needFBOLayer() {
    FORCE_SCREEN_FBO = true;
  }


  protected boolean isMultisampled() {
    return 1 < numSamples || 0 < capabilities.getNumSamples();
  }


  protected int getDepthBits() {
    if (USE_JOGL_FBOLAYER) {
      return capabilities.getDepthBits();
    } else {
      intBuffer.rewind();
      getIntegerv(DEPTH_BITS, intBuffer);
      return intBuffer.get(0);
    }
  }


  protected int getStencilBits() {
    if (USE_JOGL_FBOLAYER) {
      return capabilities.getStencilBits();
    } else {
      intBuffer.rewind();
      getIntegerv(STENCIL_BITS, intBuffer);
      return intBuffer.get(0);
    }
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
    if (USE_JOGL_FBOLAYER) {
      Texture tex = new Texture();
      tex.init(pg.width, pg.height,
               backTexAttach.getName(), GL.GL_TEXTURE_2D, GL.GL_RGBA,
               backTexAttach.getWidth(), backTexAttach.getHeight(),
               backTexAttach.minFilter, backTexAttach.magFilter,
               backTexAttach.wrapS, backTexAttach.wrapT);
      tex.invertedY(true);
      tex.colorBuffer(true);
      pg.setCache(pg, tex);
      return tex;
    } else {
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
  }


  protected Texture wrapFrontTexture() {
    if (USE_JOGL_FBOLAYER) {
      Texture tex = new Texture();
      tex.init(pg.width, pg.height,
               backTexAttach.getName(), GL.GL_TEXTURE_2D, GL.GL_RGBA,
               frontTexAttach.getWidth(), frontTexAttach.getHeight(),
               frontTexAttach.minFilter, frontTexAttach.magFilter,
               frontTexAttach.wrapS, frontTexAttach.wrapT);
      tex.invertedY(true);
      tex.colorBuffer(true);
      return tex;
    } else {
      Texture tex = new Texture();
      tex.init(pg.width, pg.height,
               glColorTex.get(frontTex), TEXTURE_2D, RGBA,
               fboWidth, fboHeight, NEAREST, NEAREST,
               CLAMP_TO_EDGE, CLAMP_TO_EDGE);
      tex.invertedY(true);
      tex.colorBuffer(true);
      return tex;
    }
  }


  protected int getBackTextureName() {
    if (USE_JOGL_FBOLAYER) {
      return backTexAttach.getName();
    } else {
      return glColorTex.get(backTex);
    }
  }


  protected int getFrontTextureName() {
    if (USE_JOGL_FBOLAYER) {
      return frontTexAttach.getName();
    } else {
      return glColorTex.get(frontTex);
    }
  }


  protected void bindFrontTexture() {
    if (USE_JOGL_FBOLAYER) {
      if (!texturingIsEnabled(GL.GL_TEXTURE_2D)) {
        enableTexturing(GL.GL_TEXTURE_2D);
      }
      gl.glBindTexture(GL.GL_TEXTURE_2D, frontTexAttach.getName());
    } else {
      if (!texturingIsEnabled(TEXTURE_2D)) {
        enableTexturing(TEXTURE_2D);
      }
      bindTexture(TEXTURE_2D, glColorTex.get(frontTex));
    }
  }


  protected void unbindFrontTexture() {
    if (USE_JOGL_FBOLAYER) {
      if (textureIsBound(GL.GL_TEXTURE_2D, frontTexAttach.getName())) {
        // We don't want to unbind another texture
        // that might be bound instead of this one.
        if (!texturingIsEnabled(GL.GL_TEXTURE_2D)) {
          enableTexturing(GL.GL_TEXTURE_2D);
          gl.glBindTexture(GL.GL_TEXTURE_2D, 0);
          disableTexturing(GL.GL_TEXTURE_2D);
        } else {
          gl.glBindTexture(GL.GL_TEXTURE_2D, 0);
        }
      }
    } else {
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
  }


  protected void syncBackTexture() {
    if (USE_JOGL_FBOLAYER) {
      if (0 < capabilities.getNumSamples()) {
        backFBO.syncSamplingSink(gl);
        backFBO.bind(gl);
      }
    } else {
      if (1 < numSamples) {
        bindFramebuffer(READ_FRAMEBUFFER, glMultiFbo.get(0));
        bindFramebuffer(DRAW_FRAMEBUFFER, glColorFbo.get(0));
        blitFramebuffer(0, 0, fboWidth, fboHeight,
                        0, 0, fboWidth, fboHeight,
                        COLOR_BUFFER_BIT, NEAREST);
      }
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
    if (USE_JOGL_FBOLAYER) return;

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
    if (USE_JOGL_FBOLAYER) {
      if (!clear0 && isFBOBacked() && !isMultisampled()) {
        // Draw the back texture into the front texture, which will be used as
        // back texture in the next frame. Otherwise flickering will occur if
        // the sketch uses "incremental drawing" (background() not called).
        frontFBO.bind(gl);
        gl.glDisable(GL.GL_BLEND);
        drawTexture(GL.GL_TEXTURE_2D, backTexAttach.getName(),
                    backTexAttach.getWidth(), backTexAttach.getHeight(),
                    0, 0, pg.width, pg.height, 0, 0, pg.width, pg.height);
        backFBO.bind(gl);
      }
    } else {
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
    }
    flush();
  }


  protected boolean canDraw() {
    return pg.initialized && pg.parent.isDisplayable();
  }


  protected void requestDraw() {
    if (pg.initialized && pg.parent.canDraw()) {
      try {
        if (toolkit == AWT) {
          canvasAWT.display();
        } else if (toolkit == NEWT) {
          window.display();
        }
      } catch (GLException e) {
        // Unwrap GLException so that only the causing exception is shown.
        Throwable tr = e.getCause();
        if (tr instanceof RuntimeException) {
          throw (RuntimeException)tr;
        } else {
          throw new RuntimeException(tr);
        }
      }
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
    if (projMatrix == null) {
      projMatrix = new float[16];
    }
    gl2x.glMatrixMode(GL2.GL_PROJECTION);
    projMatrix[ 0] = pg.projection.m00;
    projMatrix[ 1] = pg.projection.m10;
    projMatrix[ 2] = pg.projection.m20;
    projMatrix[ 3] = pg.projection.m30;
    projMatrix[ 4] = pg.projection.m01;
    projMatrix[ 5] = pg.projection.m11;
    projMatrix[ 6] = pg.projection.m21;
    projMatrix[ 7] = pg.projection.m31;
    projMatrix[ 8] = pg.projection.m02;
    projMatrix[ 9] = pg.projection.m12;
    projMatrix[10] = pg.projection.m22;
    projMatrix[11] = pg.projection.m32;
    projMatrix[12] = pg.projection.m03;
    projMatrix[13] = pg.projection.m13;
    projMatrix[14] = pg.projection.m23;
    projMatrix[15] = pg.projection.m33;
    gl2x.glLoadMatrixf(projMatrix, 0);

    if (mvMatrix == null) {
      mvMatrix = new float[16];
    }
    gl2x.glMatrixMode(GL2.GL_MODELVIEW);
    mvMatrix[ 0] = pg.modelview.m00;
    mvMatrix[ 1] = pg.modelview.m10;
    mvMatrix[ 2] = pg.modelview.m20;
    mvMatrix[ 3] = pg.modelview.m30;
    mvMatrix[ 4] = pg.modelview.m01;
    mvMatrix[ 5] = pg.modelview.m11;
    mvMatrix[ 6] = pg.modelview.m21;
    mvMatrix[ 7] = pg.modelview.m31;
    mvMatrix[ 8] = pg.modelview.m02;
    mvMatrix[ 9] = pg.modelview.m12;
    mvMatrix[10] = pg.modelview.m22;
    mvMatrix[11] = pg.modelview.m32;
    mvMatrix[12] = pg.modelview.m03;
    mvMatrix[13] = pg.modelview.m13;
    mvMatrix[14] = pg.modelview.m23;
    mvMatrix[15] = pg.modelview.m33;
    gl2x.glLoadMatrixf(mvMatrix, 0);
  }


  protected void endGL() {
  }


  //////////////////////////////////////////////////////////////////////////////

  // Caps query


  public String getString(int name) {
    return gl.glGetString(name);
  }


  public void getIntegerv(int name, IntBuffer values) {
    if (-1 < name) {
      gl.glGetIntegerv(name, values);
    } else {
      fillIntBuffer(values, 0, values.capacity() - 1, 0);
    }
  }


  public void getFloatv(int name, FloatBuffer values) {
    if (-1 < name) {
      gl.glGetFloatv(name, values);
    } else {
      fillFloatBuffer(values, 0, values.capacity() - 1, 0);
    }
  }


  public void getBooleanv(int name, IntBuffer values) {
    if (-1 < name) {
      if (byteBuffer.capacity() < values.capacity()) {
        byteBuffer = allocateDirectByteBuffer(values.capacity());
      }
      gl.glGetBooleanv(name, byteBuffer);
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
      gl.glEnable(cap);
    }
  }


  public void disable(int cap) {
    if (-1 < cap) {
      gl.glDisable(cap);
    }
  }


  ///////////////////////////////////////////////////////////

  // Render control


  public void flush() {
    gl.glFlush();
  }


  public void finish() {
    gl.glFinish();
  }


  ///////////////////////////////////////////////////////////

  // Error handling


  public int getError() {
    return gl.glGetError();
  }


  public String errorString(int err) {
    return glu.gluErrorString(err);
  }


  ///////////////////////////////////////////////////////////

  // Rendering options


  public void frontFace(int mode) {
    gl.glFrontFace(mode);
  }


  public void cullFace(int mode) {
    gl.glCullFace(mode);
  }


  public void depthMask(boolean flag) {
    gl.glDepthMask(flag);
  }


  public void depthFunc(int func) {
    gl.glDepthFunc(func);
  }


  ///////////////////////////////////////////////////////////

  // Textures


  public void genTextures(int n, IntBuffer ids) {
    gl.glGenTextures(n, ids);
  }


  public void deleteTextures(int n, IntBuffer ids) {
    gl.glDeleteTextures(n, ids);
  }


  public void activeTexture(int unit) {
    gl.glActiveTexture(unit);
  }


  public void bindTexture(int target, int id) {
    gl.glBindTexture(target, id);
    if (target == TEXTURE_2D) {
      boundTextures[0] = id;
    } else if (target == TEXTURE_RECTANGLE) {
      boundTextures[1] = id;
    }
  }


  public void texImage2D(int target, int level, int internalFormat,
                         int width, int height, int border, int format,
                         int type, Buffer data) {
    gl.glTexImage2D(target, level, internalFormat,
                    width, height, border, format, type, data);
  }


  public void texSubImage2D(int target, int level, int xOffset, int yOffset,
                            int width, int height, int format,
                            int type, Buffer data) {
    gl.glTexSubImage2D(target, level, xOffset, yOffset,
                       width, height, format, type, data);
  }


  public void texParameteri(int target, int param, int value) {
    gl.glTexParameteri(target, param, value);
  }


  public void texParameterf(int target, int param, float value) {
    gl.glTexParameterf(target, param, value);
  }


  public void getTexParameteriv(int target, int param, IntBuffer values) {
    gl.glGetTexParameteriv(target, param, values);
  }


  public void generateMipmap(int target) {
    gl.glGenerateMipmap(target);
  }


  ///////////////////////////////////////////////////////////

  // Vertex Buffers


  public void genBuffers(int n, IntBuffer ids) {
    gl.glGenBuffers(n, ids);
  }


  public void deleteBuffers(int n, IntBuffer ids) {
    gl.glDeleteBuffers(n, ids);
  }


  public void bindBuffer(int target, int id) {
    gl.glBindBuffer(target, id);
  }


  public void bufferData(int target, int size, Buffer data, int usage) {
    gl.glBufferData(target, size, data, usage);
  }


  public void bufferSubData(int target, int offset, int size, Buffer data) {
    gl.glBufferSubData(target, offset, size, data);
  }


  public void drawArrays(int mode, int first, int count) {
    gl.glDrawArrays(mode, first, count);
  }


  public void drawElements(int mode, int count, int type, int offset) {
    gl.glDrawElements(mode, count, type, offset);
  }


  public void enableVertexAttribArray(int loc) {
    gl2.glEnableVertexAttribArray(loc);
  }


  public void disableVertexAttribArray(int loc) {
    gl2.glDisableVertexAttribArray(loc);
  }


  public void vertexAttribPointer(int loc, int size, int type,
                                  boolean normalized, int stride, int offset) {
    gl2.glVertexAttribPointer(loc, size, type, normalized, stride, offset);
  }


  public void vertexAttribPointer(int loc, int size, int type,
                                  boolean normalized, int stride, Buffer data) {
    gl2.glVertexAttribPointer(loc, size, type, normalized, stride, data);
  }


  public ByteBuffer mapBuffer(int target, int access) {
    return gl2.glMapBuffer(target, access);
  }


  public ByteBuffer mapBufferRange(int target, int offset, int length,
                                   int access) {
    if (gl2x != null) {
      return gl2x.glMapBufferRange(target, offset, length, access);
    } else {
      return null;
    }
  }


  public void unmapBuffer(int target) {
    gl2.glUnmapBuffer(target);
  }


  ///////////////////////////////////////////////////////////

  // Framebuffers, renderbuffers


  public void genFramebuffers(int n, IntBuffer ids) {
    gl.glGenFramebuffers(n, ids);
  }


  public void deleteFramebuffers(int n, IntBuffer ids) {
    gl.glDeleteFramebuffers(n, ids);
  }


  public void genRenderbuffers(int n, IntBuffer ids) {
    gl.glGenRenderbuffers(n, ids);
  }


  public void deleteRenderbuffers(int n, IntBuffer ids) {
    gl.glDeleteRenderbuffers(n, ids);
  }


  public void bindFramebuffer(int target, int id) {
    gl.glBindFramebuffer(target, id);
  }


  public void blitFramebuffer(int srcX0, int srcY0, int srcX1, int srcY1,
                              int dstX0, int dstY0, int dstX1, int dstY1,
                              int mask, int filter) {
    if (gl2x != null) {
      gl2x.glBlitFramebuffer(srcX0, srcY0, srcX1, srcY1,
                             dstX0, dstY0, dstX1, dstY1, mask, filter);
    }
  }


  public void framebufferTexture2D(int target, int attachment, int texTarget,
                                   int texId, int level) {
    gl.glFramebufferTexture2D(target, attachment, texTarget, texId, level);
  }


  public void bindRenderbuffer(int target, int id) {
    gl.glBindRenderbuffer(target, id);
  }


  public void renderbufferStorageMultisample(int target, int samples,
                                             int format, int width, int height){
    if (gl2x != null) {
      gl2x.glRenderbufferStorageMultisample(target, samples, format,
                                            width, height);
    }
  }


  public void renderbufferStorage(int target, int format,
                                  int width, int height) {
    gl.glRenderbufferStorage(target, format, width, height);
  }


  public void framebufferRenderbuffer(int target, int attachment,
                                      int rendbufTarget, int rendbufId) {
    gl.glFramebufferRenderbuffer(target, attachment, rendbufTarget, rendbufId);
  }


  public int checkFramebufferStatus(int target) {
    return gl.glCheckFramebufferStatus(target);
  }


  ///////////////////////////////////////////////////////////

  // Shaders


  public int createProgram() {
    return gl2.glCreateProgram();
  }


  public void deleteProgram(int id) {
    gl2.glDeleteProgram(id);
  }


  public int createShader(int type) {
    return gl2.glCreateShader(type);
  }


  public void deleteShader(int id) {
    gl2.glDeleteShader(id);
  }


  public void linkProgram(int prog) {
    gl2.glLinkProgram(prog);
  }


  public void validateProgram(int prog) {
    gl2.glValidateProgram(prog);
  }


  public void useProgram(int prog) {
    gl2.glUseProgram(prog);
  }


  public int getAttribLocation(int prog, String name) {
    return gl2.glGetAttribLocation(prog, name);
  }


  public int getUniformLocation(int prog, String name) {
    return gl2.glGetUniformLocation(prog, name);
  }


  public void uniform1i(int loc, int value) {
    gl2.glUniform1i(loc, value);
  }


  public void uniform2i(int loc, int value0, int value1) {
    gl2.glUniform2i(loc, value0, value1);
  }


  public void uniform3i(int loc, int value0, int value1, int value2) {
    gl2.glUniform3i(loc, value0, value1, value2);
  }


  public void uniform4i(int loc, int value0, int value1, int value2,
                                 int value3) {
    gl2.glUniform4i(loc, value0, value1, value2, value3);
  }


  public void uniform1f(int loc, float value) {
    gl2.glUniform1f(loc, value);
  }


  public void uniform2f(int loc, float value0, float value1) {
    gl2.glUniform2f(loc, value0, value1);
  }


  public void uniform3f(int loc, float value0, float value1, float value2) {
    gl2.glUniform3f(loc, value0, value1, value2);
  }


  public void uniform4f(int loc, float value0, float value1, float value2,
                                 float value3) {
    gl2.glUniform4f(loc, value0, value1, value2, value3);
  }


  public void uniform1iv(int loc, int count, IntBuffer v) {
    gl2.glUniform1iv(loc, count, v);
  }


  public void uniform2iv(int loc, int count, IntBuffer v) {
    gl2.glUniform2iv(loc, count, v);
  }


  public void uniform3iv(int loc, int count, IntBuffer v) {
    gl2.glUniform3iv(loc, count, v);
  }


  public void uniform4iv(int loc, int count, IntBuffer v) {
    gl2.glUniform4iv(loc, count, v);
  }


  public void uniform1fv(int loc, int count, FloatBuffer v) {
    gl2.glUniform1fv(loc, count, v);
  }


  public void uniform2fv(int loc, int count, FloatBuffer v) {
    gl2.glUniform2fv(loc, count, v);
  }


  public void uniform3fv(int loc, int count, FloatBuffer v) {
    gl2.glUniform3fv(loc, count, v);
  }


  public void uniform4fv(int loc, int count, FloatBuffer v) {
    gl2.glUniform4fv(loc, count, v);
  }


  public void uniformMatrix2fv(int loc, int count, boolean transpose,
                               FloatBuffer mat) {
    gl2.glUniformMatrix2fv(loc, count, transpose, mat);
  }


  public void uniformMatrix3fv(int loc, int count, boolean transpose,
                               FloatBuffer mat) {
    gl2.glUniformMatrix3fv(loc, count, transpose, mat);
  }


  public void uniformMatrix4fv(int loc, int count, boolean transpose,
                               FloatBuffer mat) {
    gl2.glUniformMatrix4fv(loc, count, transpose, mat);
  }


  public void vertexAttrib1f(int loc, float value) {
    gl2.glVertexAttrib1f(loc, value);
  }


  public void vertexAttrib2f(int loc, float value0, float value1) {
    gl2.glVertexAttrib2f(loc, value0, value1);
  }


  public void vertexAttrib3f(int loc, float value0, float value1, float value2){
    gl2.glVertexAttrib3f(loc, value0, value1, value2);
  }


  public void vertexAttrib4f(int loc, float value0, float value1, float value2,
                                      float value3) {
    gl2.glVertexAttrib4f(loc, value0, value1, value2, value3);
  }


  public void vertexAttrib1fv(int loc, FloatBuffer v) {
    gl2.glVertexAttrib1fv(loc, v);
  }


  public void vertexAttrib2fv(int loc, FloatBuffer v) {
    gl2.glVertexAttrib2fv(loc, v);
  }


  public void vertexAttrib3fv(int loc, FloatBuffer v) {
    gl2.glVertexAttrib3fv(loc, v);
  }


  public void vertexAttri4fv(int loc, FloatBuffer v) {
    gl2.glVertexAttrib4fv(loc, v);
  }


  public void shaderSource(int id, String source) {
    gl2.glShaderSource(id, 1, new String[] { source }, (int[]) null, 0);
  }


  public void compileShader(int id) {
    gl2.glCompileShader(id);
  }


  public void attachShader(int prog, int shader) {
    gl2.glAttachShader(prog, shader);
  }


  public void getShaderiv(int shader, int pname, IntBuffer params) {
    gl2.glGetShaderiv(shader, pname, params);
  }


  public String getShaderInfoLog(int shader) {
    int[] val = { 0 };
    gl2.glGetShaderiv(shader, GL2.GL_INFO_LOG_LENGTH, val, 0);
    int length = val[0];

    byte[] log = new byte[length];
    gl2.glGetShaderInfoLog(shader, length, val, 0, log, 0);
    return new String(log);
  }


  public void getProgramiv(int prog, int pname, IntBuffer params) {
    gl2.glGetProgramiv(prog, pname, params);
  }


  public String getProgramInfoLog(int prog) {
    int[] val = { 0 };
    gl2.glGetShaderiv(prog, GL2.GL_INFO_LOG_LENGTH, val, 0);
    int length = val[0];

    byte[] log = new byte[length];
    gl2.glGetProgramInfoLog(prog, length, val, 0, log, 0);
    return new String(log);
  }


  ///////////////////////////////////////////////////////////

  // Viewport


  public void viewport(int x, int y, int width, int height) {
    gl.glViewport(x, y, width, height);
  }


  ///////////////////////////////////////////////////////////

  // Clipping (scissor test)


  public void scissor(int x, int y, int w, int h) {
    gl.glScissor(x, y, w, h);
  }


  ///////////////////////////////////////////////////////////

  // Blending


  public void blendEquation(int eq) {
    gl.glBlendEquation(eq);
  }


  public void blendFunc(int srcFactor, int dstFactor) {
    gl.glBlendFunc(srcFactor, dstFactor);
  }


  ///////////////////////////////////////////////////////////

  // Pixels


  public void readBuffer(int buf) {
    if (gl2x != null) {
      gl2x.glReadBuffer(buf);
    }
  }


  public void readPixels(int x, int y, int width, int height, int format,
                         int type, Buffer buffer) {


    gl.glReadPixels(x, y, width, height, format, type, buffer);


  }


  public void drawBuffer(int buf) {
    if (gl2x != null) {
      gl2x.glDrawBuffer(buf);
    }
  }


  public void clearDepth(float d) {
    gl.glClearDepthf(d);
  }


  public void clearStencil(int s) {
    gl.glClearStencil(s);
  }


  public void colorMask(boolean wr, boolean wg, boolean wb, boolean wa) {
    gl.glColorMask(wr, wg, wb, wa);
  }


  public void clearColor(float r, float g, float b, float a) {
    gl.glClearColor(r, g, b, a);
  }


  public void clear(int mask) {
    gl.glClear(mask);
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
    protected GLUtessellator tess;
    protected TessellatorCallback callback;
    protected GLUCallback gluCallback;

    public Tessellator(TessellatorCallback callback) {
      this.callback = callback;
      tess = GLU.gluNewTess();
      gluCallback = new GLUCallback();

      GLU.gluTessCallback(tess, GLU.GLU_TESS_BEGIN, gluCallback);
      GLU.gluTessCallback(tess, GLU.GLU_TESS_END, gluCallback);
      GLU.gluTessCallback(tess, GLU.GLU_TESS_VERTEX, gluCallback);
      GLU.gluTessCallback(tess, GLU.GLU_TESS_COMBINE, gluCallback);
      GLU.gluTessCallback(tess, GLU.GLU_TESS_ERROR, gluCallback);
    }

    public void beginPolygon() {
      GLU.gluTessBeginPolygon(tess, null);
    }

    public void endPolygon() {
      GLU.gluTessEndPolygon(tess);
    }

    public void setWindingRule(int rule) {
      GLU.gluTessProperty(tess, GLU.GLU_TESS_WINDING_RULE, rule);
    }

    public void beginContour() {
      GLU.gluTessBeginContour(tess);
    }

    public void endContour() {
      GLU.gluTessEndContour(tess);
    }

    public void addVertex(double[] v) {
      GLU.gluTessVertex(tess, v, 0, v);
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
    return other == -1 || other == context.hashCode();
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
    if (!loadedTex2DShader ||
        tex2DShaderContext.hashCode() != context.hashCode()) {
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
    if (!loadedTexRectShader ||
        texRectShaderContext.hashCode() != context.hashCode()) {
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

  // Java specific stuff

  protected class PGLListener implements GLEventListener {
    public PGLListener() {}

    @Override
    public void display(GLAutoDrawable glDrawable) {
      drawable = glDrawable;
      context = glDrawable.getContext();

      glThread = Thread.currentThread();

      gl = context.getGL();
      gl2 = gl.getGL2ES2();
      try {
        gl2x = gl.getGL2();
      } catch (javax.media.opengl.GLException e) {
        gl2x = null;
      }

      if (USE_JOGL_FBOLAYER && capabilities.isFBO()) {
        // The onscreen drawing surface is backed by an FBO layer.
        GLFBODrawable fboDrawable = null;

        if (toolkit == AWT) {
          GLCanvas glCanvas = (GLCanvas)glDrawable;
          fboDrawable = (GLFBODrawable)glCanvas.getDelegatedDrawable();
        } else {
          GLWindow glWindow = (GLWindow)glDrawable;
          fboDrawable = (GLFBODrawable)glWindow.getDelegatedDrawable();
        }
        if (fboDrawable != null) {
          backFBO = fboDrawable.getFBObject(GL.GL_BACK);
          if (0 < capabilities.getNumSamples()) {
            // When using multisampled FBO, the back buffer is the MSAA
            // surface so it cannot be read from. The sink buffer contains
            // the readable 2D texture.
            // In this case, we create an auxiliar "front" buffer that it is
            // swapped with the sink buffer at the beginning of each frame.
            // In this way, we always have a readable copy of the previous
            // frame in the front texture, while the back is synchronized
            // with the contents of the MSAA back buffer when requested.

            if (frontFBO == null) {
              // init
              frontFBO = new FBObject();
              frontFBO.reset(gl, pg.width, pg.height);
              frontFBO.attachTexture2D(gl, 0, true);
              sinkFBO = backFBO.getSamplingSinkFBO();
            } else {
              // swap
              FBObject temp = sinkFBO;
              sinkFBO = frontFBO;
              frontFBO = temp;
              backFBO.setSamplingSink(sinkFBO);
            }

            backTexAttach  = (FBObject.TextureAttachment) sinkFBO.
                             getColorbuffer(0);
            frontTexAttach = (FBObject.TextureAttachment)frontFBO.
                             getColorbuffer(0);
          } else {
            // w/out multisampling, rendering is done on the back buffer.
            frontFBO = fboDrawable.getFBObject(GL.GL_FRONT);

            backTexAttach  = fboDrawable.getTextureBuffer(GL.GL_BACK);
            frontTexAttach = fboDrawable.getTextureBuffer(GL.GL_FRONT);
          }
        }
      }

      pg.parent.handleDraw();
    }

    @Override
    public void dispose(GLAutoDrawable adrawable) {
    }

    @Override
    public void init(GLAutoDrawable adrawable) {
      drawable = adrawable;
      context = adrawable.getContext();
      capabilities = adrawable.getChosenGLCapabilities();

      if (!context.hasBasicFBOSupport()) {
        throw new RuntimeException("No basic FBO support is available");
      }
      if (!context.hasGLSL()) {
        throw new RuntimeException("No GLSL support is available");
      }
    }

    @Override
    public void reshape(GLAutoDrawable adrawable, int x, int y, int w, int h) {
      drawable = adrawable;
      context = adrawable.getContext();
    }
  }

  protected void nativeMouseEvent(com.jogamp.newt.event.MouseEvent nativeEvent,
                                  int peAction) {
    int modifiers = nativeEvent.getModifiers();
    int peModifiers = modifiers &
                      (InputEvent.SHIFT_MASK |
                       InputEvent.CTRL_MASK |
                       InputEvent.META_MASK |
                       InputEvent.ALT_MASK);

    int peButton = 0;
    if ((modifiers & InputEvent.BUTTON1_MASK) != 0) {
      peButton = PConstants.LEFT;
    } else if ((modifiers & InputEvent.BUTTON2_MASK) != 0) {
      peButton = PConstants.CENTER;
    } else if ((modifiers & InputEvent.BUTTON3_MASK) != 0) {
      peButton = PConstants.RIGHT;
    }

    if (PApplet.platform == PConstants.MACOSX) {
      //if (nativeEvent.isPopupTrigger()) {
      if ((modifiers & InputEvent.CTRL_MASK) != 0) {
        peButton = PConstants.RIGHT;
      }
    }

    int peCount = peAction == MouseEvent.WHEEL ?
      (int) nativeEvent.getWheelRotation() :
      nativeEvent.getClickCount();

    MouseEvent me = new MouseEvent(nativeEvent, nativeEvent.getWhen(),
                                   peAction, peModifiers,
                                   nativeEvent.getX(), nativeEvent.getY(),
                                   peButton,
                                   peCount);

    pg.parent.postEvent(me);
  }

  protected void nativeKeyEvent(com.jogamp.newt.event.KeyEvent nativeEvent,
                                int peAction) {
    int peModifiers = nativeEvent.getModifiers() &
                      (InputEvent.SHIFT_MASK |
                       InputEvent.CTRL_MASK |
                       InputEvent.META_MASK |
                       InputEvent.ALT_MASK);

    char keyChar;
    if ((int)nativeEvent.getKeyChar() == 0) {
      keyChar = PConstants.CODED;
    } else {
      keyChar = nativeEvent.getKeyChar();
    }

    KeyEvent ke = new KeyEvent(nativeEvent, nativeEvent.getWhen(),
                               peAction, peModifiers,
                               keyChar,
                               nativeEvent.getKeyCode());

    pg.parent.postEvent(ke);
  }

  class NEWTWindowListener implements com.jogamp.newt.event.WindowListener {
    @Override
    public void windowGainedFocus(com.jogamp.newt.event.WindowEvent arg0) {
      pg.parent.focusGained(null);
    }

    @Override
    public void windowLostFocus(com.jogamp.newt.event.WindowEvent arg0) {
      pg.parent.focusLost(null);
    }

    @Override
    public void windowDestroyNotify(com.jogamp.newt.event.WindowEvent arg0) {
    }

    @Override
    public void windowDestroyed(com.jogamp.newt.event.WindowEvent arg0) {
    }

    @Override
    public void windowMoved(com.jogamp.newt.event.WindowEvent arg0) {
    }

    @Override
    public void windowRepaint(com.jogamp.newt.event.WindowUpdateEvent arg0) {
    }

    @Override
    public void windowResized(com.jogamp.newt.event.WindowEvent arg0) { }
  }

  // NEWT mouse listener
  class NEWTMouseListener extends com.jogamp.newt.event.MouseAdapter {
    @Override
    public void mousePressed(com.jogamp.newt.event.MouseEvent e) {
      nativeMouseEvent(e, MouseEvent.PRESS);
    }
    @Override
    public void mouseReleased(com.jogamp.newt.event.MouseEvent e) {
      nativeMouseEvent(e, MouseEvent.RELEASE);
    }
    @Override
    public void mouseClicked(com.jogamp.newt.event.MouseEvent e) {
      nativeMouseEvent(e, MouseEvent.CLICK);
    }
    @Override
    public void mouseDragged(com.jogamp.newt.event.MouseEvent e) {
      nativeMouseEvent(e, MouseEvent.DRAG);
    }
    @Override
    public void mouseMoved(com.jogamp.newt.event.MouseEvent e) {
      nativeMouseEvent(e, MouseEvent.MOVE);
    }
    @Override
    public void mouseWheelMoved(com.jogamp.newt.event.MouseEvent e) {
      nativeMouseEvent(e, MouseEvent.WHEEL);
    }
    @Override
    public void mouseEntered(com.jogamp.newt.event.MouseEvent e) {
      nativeMouseEvent(e, MouseEvent.ENTER);
    }
    @Override
    public void mouseExited(com.jogamp.newt.event.MouseEvent e) {
      nativeMouseEvent(e, MouseEvent.EXIT);
    }
  }

  // NEWT key listener
  class NEWTKeyListener extends com.jogamp.newt.event.KeyAdapter {
    @Override
    public void keyPressed(com.jogamp.newt.event.KeyEvent e) {
      nativeKeyEvent(e, KeyEvent.PRESS);
    }
    @Override
    public void keyReleased(com.jogamp.newt.event.KeyEvent e) {
      nativeKeyEvent(e, KeyEvent.RELEASE);
    }
    @Override
    public void keyTyped(com.jogamp.newt.event.KeyEvent e)  {
      nativeKeyEvent(e, KeyEvent.TYPE);
    }
  }
}
