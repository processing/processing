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
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

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
import com.jogamp.opengl.util.AnimatorBase;

/**
 * Processing-OpenGL abstraction layer.
 *
 * Warnings are suppressed for static access because presumably on Android,
 * the GL2 vs GL distinctions are necessary, whereas on desktop they are not.
 */
@SuppressWarnings("static-access")
public class PGL {
  // The two windowing toolkits available to use in JOGL:
  protected static final int AWT  = 0; // http://jogamp.org/wiki/index.php/Using_JOGL_in_AWT_SWT_and_Swing
  protected static final int NEWT = 1; // http://jogamp.org/jogl/doc/NEWT-Overview.html

  /** Size of a short (in bytes). */
  protected static final int SIZEOF_SHORT = Short.SIZE / 8;

  /** Size of an int (in bytes). */
  protected static final int SIZEOF_INT = Integer.SIZE / 8;

  /** Size of a float (in bytes). */
  protected static final int SIZEOF_FLOAT = Float.SIZE / 8;

  /** Size of a byte (in bytes). */
  protected static final int SIZEOF_BYTE = Byte.SIZE / 8;

  /** Size of a vertex index. */
  protected static final int SIZEOF_INDEX = SIZEOF_SHORT;

  /** Type of a vertex index. */
  protected static final int INDEX_TYPE = GL.GL_UNSIGNED_SHORT;

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

  /** Enables/disables mipmap use. **/
  protected static final boolean MIPMAPS_ENABLED = true;

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
  public GL gl;

  /** GLU interface **/
  public GLU glu;

  /** The rendering context (holds rendering state info) */
  public GLContext context;

  /** The AWT canvas where OpenGL rendering takes place */
  public Canvas canvas;

  /** GLES2 functionality (shaders, etc) */
  protected GL2ES2 gl2;

  /** GL2 desktop functionality (blit framebuffer, map buffer range,
   * multisampled renerbuffers) */
  protected GL2 gl2x;

  /** The PGraphics object using this interface */
  protected PGraphicsOpenGL pg;

  /** Whether OpenGL has been initialized or not */
  protected boolean initialized;

  /** Windowing toolkit */
  protected static int toolkit = NEWT;

  protected static boolean enable_screen_FBO_macosx  = true;
  protected static boolean enable_screen_FBO_windows = false;
  protected static boolean enable_screen_FBO_linux   = false;
  protected static boolean enable_screen_FBO_other   = false;

  /** Selected GL profile */
  protected GLProfile profile;

  /** The capabilities of the OpenGL rendering surface */
  protected GLCapabilitiesImmutable capabilities;

  /** The rendering surface */
  protected GLDrawable drawable;

  /** The AWT-OpenGL canvas */
  protected GLCanvas canvasAWT;

  /** The NEWT-OpenGL canvas */
  protected NewtCanvasAWT canvasNEWT;

  /** The NEWT window */
  protected GLWindow window;

  /** The listener that fires the frame rendering in Processing */
  protected PGLListener listener;

  /** Animator to drive the rendering thread in NEWT */
  protected NEWTAnimator animator;

  /** Desired target framerate */
  protected float targetFramerate = 60;
  protected boolean setFramerate = false;

  /** Which texturing targets are enabled */
  protected static boolean[] texturingTargets = { false, false };

  /** Which textures are bound to each target */
  protected static int[] boundTextures = { 0, 0 };

  ///////////////////////////////////////////////////////////

  // FBO for anti-aliased rendering

  protected int drawTexName;
  protected int drawTexWidth, drawTexHeight;
  protected FBObject drawFBO;

  /*
  protected static final boolean ENABLE_OSX_SCREEN_FBO  = false;
  protected static final int MIN_OSX_VER_FOR_SCREEN_FBO = 6;
  protected static final int MIN_SAMPLES_FOR_SCREEN_FBO = 1;
  protected boolean needScreenFBO = false;
  protected int fboWidth, fboHeight;
  protected int numSamples;
  protected boolean multisample;
  protected boolean packedDepthStencil;
  protected int backTex, frontTex;
  protected int[] glColorTex = { 0, 0 };
  protected int[] glColorFbo = { 0 };
  protected int[] glMultiFbo = { 0 };
  protected int[] glColorRenderBuffer = { 0 };
  protected int[] glPackedDepthStencil = { 0 };
  protected int[] glDepthBuffer = { 0 };
  protected int[] glStencilBuffer = { 0 };
  protected int contextHashCode;
*/

  ///////////////////////////////////////////////////////////

  // Texture rendering

  protected boolean loadedTex2DShader = false;
  protected int tex2DShaderProgram;
  protected int tex2DVertShader;
  protected int tex2DFragShader;
  protected GLContext tex2DShaderContext;
  protected int tex2DVertLoc;
  protected int tex2DTCoordLoc;

  protected boolean loadedTexRectShader = false;
  protected int texRectShaderProgram;
  protected int texRectVertShader;
  protected int texRectFragShader;
  protected GLContext texRectShaderContext;
  protected int texRectVertLoc;
  protected int texRectTCoordLoc;

  protected float[] texCoords = {
    //  X,     Y,    U,    V
    -1.0f, -1.0f, 0.0f, 0.0f,
    +1.0f, -1.0f, 1.0f, 0.0f,
    -1.0f, +1.0f, 0.0f, 1.0f,
    +1.0f, +1.0f, 1.0f, 1.0f
  };
  protected FloatBuffer texData;

  protected String texVertShaderSource =
    "attribute vec2 inVertex;" +
    "attribute vec2 inTexcoord;" +
    "varying vec2 vertTexcoord;" +
    "void main() {" +
    "  gl_Position = vec4(inVertex, 0, 1);" +
    "  vertTexcoord = inTexcoord;" +
    "}";

  protected String tex2DFragShaderSource =
    SHADER_PREPROCESSOR_DIRECTIVE +
    "uniform sampler2D textureSampler;" +
    "varying vec2 vertTexcoord;" +
    "void main() {" +
    "  gl_FragColor = texture2D(textureSampler, vertTexcoord.st);" +
    "}";

  protected String texRectFragShaderSource =
    SHADER_PREPROCESSOR_DIRECTIVE +
    "uniform sampler2DRect textureSampler;" +
    "varying vec2 vertTexcoord;" +
    "void main() {" +
    "  gl_FragColor = texture2DRect(textureSampler, vertTexcoord.st);" +
    "}";

  ///////////////////////////////////////////////////////////

  // 1-pixel color, depth, stencil buffers

  protected IntBuffer colorBuffer;
  protected FloatBuffer depthBuffer;
  protected ByteBuffer stencilBuffer;


  ///////////////////////////////////////////////////////////

  // Initialization, finalization


  public PGL(PGraphicsOpenGL pg) {
    this.pg = pg;
    glu = new GLU();
    initialized = false;
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
        initialized = false;
      }
      targetFramerate = framerate;
      setFramerate = true;
    }
  }


  protected void initPrimarySurface(int antialias) {
    if (profile == null) {
      profile = GLProfile.getDefault();
    } else {
      // Restarting...
      if (canvasAWT != null) {
        canvasAWT.removeGLEventListener(listener);
        pg.parent.removeListeners(canvasAWT);
        pg.parent.remove(canvasAWT);
      } else if (canvasNEWT != null) {
        animator.stop();
        animator.remove(window);
        window.removeGLEventListener(listener);
        pg.parent.removeListeners(canvasNEWT);
        pg.parent.remove(canvasNEWT);
      }
      setFramerate = false;
    }

    // Setting up the desired GL capabilities;
    GLCapabilities caps = new GLCapabilities(profile);
    if (1 < antialias) {
      caps.setSampleBuffers(true);
      caps.setNumSamples(antialias);
    } else {
      caps.setSampleBuffers(false);
    }

    if (PApplet.platform == PConstants.MACOSX) {
      caps.setFBO(enable_screen_FBO_macosx);
    } else if (PApplet.platform == PConstants.WINDOWS) {
      caps.setFBO(enable_screen_FBO_windows);
    } else if (PApplet.platform == PConstants.LINUX) {
      caps.setFBO(enable_screen_FBO_linux);
    } else {
      caps.setFBO(enable_screen_FBO_other);
    }

    caps.setDepthBits(24);
    // Stencil buffer dissabled for now:
    // http://forum.jogamp.org/Enabling-Stencil-buffer-breaks-rendering-OSX-R11-td4026857.html
    //caps.setStencilBits(8);
    caps.setAlphaBits(8);
    caps.setBackgroundOpaque(true);
    caps.setOnscreen(true);

    if (toolkit == AWT) {
      canvasAWT = new GLCanvas(caps);
      canvasAWT.setBounds(0, 0, pg.width, pg.height);

      pg.parent.setLayout(new BorderLayout());
      pg.parent.add(canvasAWT, BorderLayout.CENTER);
      pg.parent.removeListeners(pg.parent);
      pg.parent.addListeners(canvasAWT);

      listener = new PGLListener();
      canvasAWT.addGLEventListener(listener);
      capabilities = canvasAWT.getChosenGLCapabilities();
      canvas = canvasAWT;
      canvasNEWT = null;
    } else if (toolkit == NEWT) {
      window = GLWindow.create(caps);
      canvasNEWT = new NewtCanvasAWT(window);
      canvasNEWT.setBounds(0, 0, pg.width, pg.height);
      canvasNEWT.setBackground(Color.GRAY);

      pg.parent.setLayout(new BorderLayout());
      pg.parent.add(canvasNEWT, BorderLayout.CENTER);
      //pg.parent.removeListeners(pg.parent);
      //pg.parent.addListeners(canvasNEWT);

      com.jogamp.newt.event.MouseListener mouseListener = new NEWTMouseAdapter();
      window.addMouseListener(mouseListener);
      com.jogamp.newt.event.KeyListener keyListener = new NEWTKeyAdapter();
      window.addKeyListener(keyListener);



      listener = new PGLListener();
      window.addGLEventListener(listener);
      animator = new NEWTAnimator(window);
      animator.start();

      capabilities = window.getChosenGLCapabilities();
      canvas = canvasNEWT;
      canvasAWT = null;

//      System.out.println(capabilities);
    }

    initialized = true;
  }


  protected void initOffscreenSurface(PGL primary) {
    context = primary.context;
    capabilities = primary.capabilities;
    drawable = null;
    initialized = true;
  }


  protected void updatePrimary() {
    if (!setFramerate) {
      setFrameRate(targetFramerate);
    }
  }

  protected void updateOffscreen(PGL primary) {
    gl  = primary.gl;
    gl2 = primary.gl2;
    gl2x = primary.gl2x;
  }


  protected int primaryReadFramebuffer() {
    if (capabilities.isFBO()) {
      return context.getDefaultReadFramebuffer();
    } else {
      return 0;
    }
  }

  protected int primaryDrawFramebuffer() {
    if (capabilities.isFBO()) {
      return context.getDefaultDrawFramebuffer();
    } else {
      return 0;
    }
  }

  protected int primaryDrawBuffer() {
    if (capabilities.isFBO()) {
      return GL.GL_COLOR_ATTACHMENT0;
    } else {
      return GL.GL_BACK;
    }
  }

  protected int primaryReadBuffer() {
    if (capabilities.isFBO()) {
      return GL.GL_COLOR_ATTACHMENT0;
    } else {
      return GL.GL_BACK;
    }
  }

  protected boolean primaryIsFboBacked() {
    return capabilities.isFBO();
  }

  protected int getFboTexTarget() {
    return GL.GL_TEXTURE_2D;
  }

  protected int getFboTexName() {
    return drawTexName;
  }

  protected int getFboWidth() {
    return drawTexWidth;
  }

  protected int getFboHeight() {
    return drawTexHeight;
  }

  /*
  protected void bindPrimaryColorFBO() {
    if (multisample) {
      // Blit the contents of the multisampled FBO into the color FBO,
      // so the later is up to date.
      gl.glBindFramebuffer(GL2.GL_READ_FRAMEBUFFER, glMultiFbo[0]);
      gl.glBindFramebuffer(GL2.GL_DRAW_FRAMEBUFFER, glColorFbo[0]);
      gl2x.glBlitFramebuffer(0, 0, fboWidth, fboHeight,
                             0, 0, fboWidth, fboHeight,
                             GL.GL_COLOR_BUFFER_BIT, GL.GL_NEAREST);
    }

    gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, glColorFbo[0]);
    PGraphicsOpenGL.screenFramebuffer.glFbo = glColorFbo[0];

    // Make the color buffer opaque so it doesn't show
    // the background when drawn on top of another surface.
    gl.glColorMask(false, false, false, true);
    gl.glClearColor(0, 0, 0, 1);
    gl.glClear(GL.GL_COLOR_BUFFER_BIT);
    gl.glColorMask(true, true, true, true);
  }


  protected void bindPrimaryMultiFBO() {
    if (multisample) {
      gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, glMultiFbo[0]);
      PGraphicsOpenGL.screenFramebuffer.glFbo = glMultiFbo[0];
    }
  }


  protected void releaseScreenFBO() {
    gl.glDeleteTextures(1, glColorTex, 0);
    gl.glDeleteFramebuffers(1, glColorFbo, 0);
    if (packedDepthStencil) {
      gl.glDeleteRenderbuffers(1, glPackedDepthStencil, 0);
    } else {
      gl.glDeleteRenderbuffers(1, glDepthBuffer, 0);
      gl.glDeleteRenderbuffers(1, glStencilBuffer, 0);
    }
    if (multisample) {
      gl.glDeleteFramebuffers(1, glMultiFbo, 0);
      gl.glDeleteRenderbuffers(1, glColorRenderBuffer, 0);
    }
  }
*/

  protected int qualityToSamples(int quality) {
    if (quality <= 1) {
      return 1;
    } else {
      // Number of samples is always an even number:
      int n = 2 * (quality / 2);
      return n;
    }
  }

  protected void forceUpdate() {
    if (0 < capabilities.getNumSamples()) {
      drawFBO.syncSamplingSink(gl);
      drawFBO.bind(gl);
    }
  }


  protected void bindBackBufferTex() {
    /*
    if (!texturingIsEnabled(GL.GL_TEXTURE_2D)) {
      enableTexturing(GL.GL_TEXTURE_2D);
    }
    gl.glBindTexture(GL.GL_TEXTURE_2D, glColorTex[backTex]);
    */
  }


  protected void unbindBackBufferTex() {
    /*
    if (textureIsBound(GL.GL_TEXTURE_2D, glColorTex[backTex])) {
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
    */
  }


  ///////////////////////////////////////////////////////////

  // Frame rendering


  protected void beginOnscreenDraw(boolean clear) {
    /*
    if (glColorFbo[0] != 0) {
      gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, glColorFbo[0]);
      gl.glFramebufferTexture2D(GL.GL_FRAMEBUFFER,
                                GL.GL_COLOR_ATTACHMENT0,
                                GL.GL_TEXTURE_2D,
                                glColorTex[frontTex], 0);

      if (multisample) {
        // Render the scene to the mutisampled buffer...
        gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, glMultiFbo[0]);
        gl2x.glDrawBuffer(GL.GL_COLOR_ATTACHMENT0);

        // Now the screen buffer is the multisample FBO.
        PGraphicsOpenGL.screenFramebuffer.glFbo = glMultiFbo[0];
      } else {
        if (gl2x != null) gl2x.glDrawBuffer(GL.GL_COLOR_ATTACHMENT0);

        PGraphicsOpenGL.screenFramebuffer.glFbo = glColorFbo[0];
      }
    }
    */
  }


  protected void endOnscreenDraw(boolean clear0) {
    /*
    if (glColorFbo[0] != 0) {
      if (multisample) {
        // Blit the contents of the multisampled FBO into the color FBO:
        gl.glBindFramebuffer(GL2.GL_READ_FRAMEBUFFER, glMultiFbo[0]);
        gl.glBindFramebuffer(GL2.GL_DRAW_FRAMEBUFFER, glColorFbo[0]);
        gl2x.glBlitFramebuffer(0, 0, fboWidth, fboHeight,
                               0, 0, fboWidth, fboHeight,
                               GL.GL_COLOR_BUFFER_BIT, GL.GL_NEAREST);
      }

      // And finally write the color texture to the screen, without blending.
      gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, 0);

      gl.glClearDepth(1);
      gl.glClearColor(0, 0, 0, 0);
      gl.glClear(GL.GL_DEPTH_BUFFER_BIT | GL.GL_STENCIL_BUFFER_BIT);

      gl.glDisable(GL.GL_BLEND);
      drawTexture(GL.GL_TEXTURE_2D, glColorTex[frontTex], fboWidth, fboHeight,
                  0, 0, pg.width, pg.height, 0, 0, pg.width, pg.height);

      // Leaving the color FBO currently bound as the screen FB.
      gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, glColorFbo[0]);


      // Disabling back-buffer for the time being.
//      // Blitting the front texture into the back texture.
//      gl.glFramebufferTexture2D(GL.GL_FRAMEBUFFER,
//                                GL.GL_COLOR_ATTACHMENT0,
//                                GL.GL_TEXTURE_2D,
//                                glColorTex[backTex], 0);
//      drawTexture(GL.GL_TEXTURE_2D, glColorTex[frontTex], fboWidth, fboHeight,
//                  0, 0, pg.width, pg.height, 0, 0, pg.width, pg.height);
//
//      // Leave the front texture as current
//      gl.glFramebufferTexture2D(GL.GL_FRAMEBUFFER,
//                                GL.GL_COLOR_ATTACHMENT0,
//                                GL.GL_TEXTURE_2D,
//                                glColorTex[frontTex], 0);

      // TODO: check if the screen FBO should be left bound instead
      PGraphicsOpenGL.screenFramebuffer.glFbo = glColorFbo[0];

      // Swapping front and back textures.
//      int temp = frontTex;
//      frontTex = backTex;
//      backTex = temp;
    }
    */
  }


  protected void beginOffscreenDraw(boolean clear) {
  }


  protected void endOffscreenDraw(boolean clear0) {
  }


  protected boolean canDraw() {
    return initialized && pg.parent.isDisplayable();
  }


  protected void requestDraw() {
    if (initialized) {
      try {
        if (toolkit == AWT) {
          canvasAWT.display();
        } else if (toolkit == NEWT) {
          animator.requestDisplay();
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


  //////////////////////////////////////////////////////////////////////////////

  // Caps query


  public String getString(int name) {
    return gl.glGetString(name);
  }


  public void getIntegerv(int name, int[] values, int offset) {
    gl.glGetIntegerv(name, values, offset);
  }


  public void getBooleanv(int name, boolean[] values, int offset) {
    if (-1 < name) {
      byte[] bvalues = new byte[values.length];
      gl.glGetBooleanv(name, bvalues, offset);
      for (int i = 0; i < values.length; i++) {
        values[i] = bvalues[i] != 0;
      }
    } else {
      Arrays.fill(values, false);
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


  public void genTextures(int n, int[] ids, int offset) {
    gl.glGenTextures(n, ids, offset);
  }


  public void deleteTextures(int n, int[] ids, int offset) {
    gl.glDeleteTextures(n, ids, offset);
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


  public void getTexParameteriv(int target, int param, int[] values,
                                int offset) {
    gl.glGetTexParameteriv(target, param, values, offset);
  }


  public void generateMipmap(int target) {
    gl.glGenerateMipmap(target);
  }


  ///////////////////////////////////////////////////////////

  // Vertex Buffers


  public void genBuffers(int n, int[] ids, int offset) {
    gl.glGenBuffers(n, ids, offset);
  }


  public void deleteBuffers(int n, int[] ids, int offset) {
    gl.glDeleteBuffers(n, ids, offset);
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


  public void genFramebuffers(int n, int[] ids, int offset) {
    gl.glGenFramebuffers(n, ids, offset);
  }


  public void deleteFramebuffers(int n, int[] ids, int offset) {
    gl.glDeleteFramebuffers(n, ids, offset);
  }


  public void genRenderbuffers(int n, int[] ids, int offset) {
    gl.glGenRenderbuffers(n, ids, offset);
  }


  public void deleteRenderbuffers(int n, int[] ids, int offset) {
    gl.glDeleteRenderbuffers(n, ids, offset);
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


  public void uniform1iv(int loc, int count, int[] v, int offset) {
    gl2.glUniform1iv(loc, count, v, offset);
  }


  public void uniform2iv(int loc, int count, int[] v, int offset) {
    gl2.glUniform2iv(loc, count, v, offset);
  }


  public void uniform3iv(int loc, int count, int[] v, int offset) {
    gl2.glUniform3iv(loc, count, v, offset);
  }


  public void uniform4iv(int loc, int count, int[] v, int offset) {
    gl2.glUniform4iv(loc, count, v, offset);
  }


  public void uniform1fv(int loc, int count, float[] v, int offset) {
    gl2.glUniform1fv(loc, count, v, offset);
  }


  public void uniform2fv(int loc, int count, float[] v, int offset) {
    gl2.glUniform2fv(loc, count, v, offset);
  }


  public void uniform3fv(int loc, int count, float[] v, int offset) {
    gl2.glUniform3fv(loc, count, v, offset);
  }


  public void uniform4fv(int loc, int count, float[] v, int offset) {
    gl2.glUniform4fv(loc, count, v, offset);
  }


  public void uniformMatrix2fv(int loc, int count, boolean transpose,
                               float[] mat, int offset) {
    gl2.glUniformMatrix2fv(loc, count, transpose, mat, offset);
  }


  public void uniformMatrix3fv(int loc, int count, boolean transpose,
                               float[] mat, int offset) {
    gl2.glUniformMatrix3fv(loc, count, transpose, mat, offset);
  }


  public void uniformMatrix4fv(int loc, int count, boolean transpose,
                               float[] mat, int offset) {
    gl2.glUniformMatrix4fv(loc, count, transpose, mat, offset);
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


  public void vertexAttrib1fv(int loc, float[] v, int offset) {
    gl2.glVertexAttrib1fv(loc, v, offset);
  }


  public void vertexAttrib2fv(int loc, float[] v, int offset) {
    gl2.glVertexAttrib2fv(loc, v, offset);
  }


  public void vertexAttrib3fv(int loc, float[] v, int offset) {
    gl2.glVertexAttrib3fv(loc, v, offset);
  }


  public void vertexAttrib4fv(int loc, float[] v, int offset) {
    gl2.glVertexAttrib4fv(loc, v, offset);
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


  public void getShaderiv(int shader, int pname, int[] params, int offset) {
    gl2.glGetShaderiv(shader, pname, params, offset);
  }


  public String getShaderInfoLog(int shader) {
    int[] val = { 0 };
    gl2.glGetShaderiv(shader, GL2.GL_INFO_LOG_LENGTH, val, 0);
    int length = val[0];

    byte[] log = new byte[length];
    gl2.glGetShaderInfoLog(shader, length, val, 0, log, 0);
    return new String(log);
  }


  public void getProgramiv(int prog, int pname, int[] params, int offset) {
    gl2.glGetProgramiv(prog, pname, params, offset);
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


  protected Context createEmptyContext() {
    return new Context();
  }


  protected Context getCurrentContext() {
    return new Context(context);
  }


  protected class Context {
    protected int id;

    Context() {
      id = -1;
    }

    Context(GLContext context) {
      if (context != null) {
        id = context.hashCode();
      } else {
        id = -1;
      }
    }

    boolean current() {
      return equal(context);
    }

    boolean equal(GLContext context) {
      if (id == -1 || context == null) {
        // A null context means a still non-created resource,
        // so it is considered equal to the argument.
        return true;
      } else {
        return id == context.hashCode();
      }
    }

    int id() {
      return id;
    }
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


  protected boolean contextIsCurrent(Context other) {
    return other == null || other.current();
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
    int[] texels = new int[width * height];
    texSubImage2D(target, 0, 0, 0, width, height, format, UNSIGNED_BYTE,
                  IntBuffer.wrap(texels));
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
      boolean[] depthTest = new boolean[1];
      getBooleanv(DEPTH_TEST, depthTest, 0);
      disable(DEPTH_TEST);

      // When drawing the texture we don't write to the
      // depth mask, so the texture remains in the background
      // and can be occluded by anything drawn later, even if
      // if it is behind it.
      boolean[] depthMask = new boolean[1];
      getBooleanv(DEPTH_WRITEMASK, depthMask, 0);
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

      if (depthTest[0]) {
        enable(DEPTH_TEST);
      } else {
        disable(DEPTH_TEST);
      }
      depthMask(depthMask[0]);
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
      boolean[] depthTest = new boolean[1];
      getBooleanv(DEPTH_TEST, depthTest, 0);
      disable(DEPTH_TEST);

      // When drawing the texture we don't write to the
      // depth mask, so the texture remains in the background
      // and can be occluded by anything drawn later, even if
      // if it is behind it.
      boolean[] depthMask = new boolean[1];
      getBooleanv(DEPTH_WRITEMASK, depthMask, 0);
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

      if (depthTest[0]) {
        enable(DEPTH_TEST);
      } else {
        disable(DEPTH_TEST);
      }
      depthMask(depthMask[0]);
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
               GL.GL_UNSIGNED_BYTE, stencilBuffer);
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
      int[] compiled = new int[1];
      getShaderiv(shader, COMPILE_STATUS, compiled, 0);
      if (compiled[0] == FALSE) {
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
      int[] linked = new int[1];
      getProgramiv(program, LINK_STATUS, linked, 0);
      if (linked[0] == FALSE) {
        System.err.println("Could not link program: ");
        System.err.println(getProgramInfoLog(program));
        deleteProgram(program);
        program = 0;
      }
    }
    return program;
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


  protected static ByteBuffer allocateDirectByteBuffer(int size) {
    return ByteBuffer.allocateDirect(size * SIZEOF_BYTE).
           order(ByteOrder.nativeOrder());
  }


  protected static IntBuffer allocateDirectIntBuffer(int size) {
    return ByteBuffer.allocateDirect(size * SIZEOF_INT).
           order(ByteOrder.nativeOrder()).asIntBuffer();
  }


  protected static FloatBuffer allocateDirectFloatBuffer(int size) {
    return ByteBuffer.allocateDirect(size * SIZEOF_FLOAT).
           order(ByteOrder.nativeOrder()).asFloatBuffer();
  }


  protected int[] getGLVersion() {
    String version = gl.glGetString(GL.GL_VERSION).trim();
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


  ///////////////////////////////////////////////////////////

  // Java specific stuff

  protected class PGLListener implements GLEventListener {
    @Override
    // http://www.opengl.org/wiki/Default_Framebuffer
    public void display(GLAutoDrawable adrawable) {
      drawable = adrawable;
      context = adrawable.getContext();

      if (capabilities.isFBO()) {
        GLFBODrawable fboDrawable = null;
        if (toolkit == AWT) {
          GLCanvas drCanvas = (GLCanvas)adrawable;
          fboDrawable = (GLFBODrawable)drCanvas.getDelegatedDrawable();
//          FBObject fboFront = dr.getFBObject(GL.GL_FRONT);
//          FBObject.Colorbuffer colorBuf = fboFront.getColorbuffer(0);
//          FBObject.TextureAttachment texFront = (FBObject.TextureAttachment) colorBuf;
//          System.out.println("front texture: " + texFront.getName());
        } else {
          GLWindow drWindow = (GLWindow)adrawable;
          fboDrawable = (GLFBODrawable)drWindow.getDelegatedDrawable();
        }
        FBObject.TextureAttachment texAttach = null;
        if (fboDrawable != null) {
          //fboBack = fboDrawable.getFBObject(GL.GL_BACK);
          //fboFront = fboDrawable.getFBObject(GL.GL_FRONT);
          //FBObject.Colorbuffer colorBuf = fboFront.getSamplingSinkFBO().getColorbuffer(0);
          //texAttach = (FBObject.TextureAttachment) colorBuf;
          //texAttach = fboBack.getSamplingSink();

          drawFBO = fboDrawable.getFBObject(GL.GL_BACK);
          if (0 < capabilities.getNumSamples()) {
            // When using multisampled FBO,the back buffer is the MSAA
            // surface so it cannot read from, the one to use is the front.
            texAttach = fboDrawable.getTextureBuffer(GL.GL_FRONT);
          } else {
            // W/out multisampling, rendering is done on the back buffer.
            texAttach = fboDrawable.getTextureBuffer(GL.GL_BACK);
          }
        }
        if (texAttach != null) {
          drawTexName = texAttach.getName();
          drawTexWidth = texAttach.getWidth();
          drawTexHeight = texAttach.getHeight();
        }
      }

      gl = context.getGL();
      gl2 = gl.getGL2ES2();
      try {
        gl2x = gl.getGL2();
      } catch (javax.media.opengl.GLException e) {
        gl2x = null;
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

      gl = context.getGL();
      String extensions = gl.glGetString(GL.GL_EXTENSIONS);
      if (-1 == extensions.indexOf("_framebuffer_object")) {
        throw new RuntimeException("No framebuffer objects available");
      }
      if (-1 == extensions.indexOf("_vertex_buffer_object")) {
        throw new RuntimeException("No vertex buffer objects available");
      }
      if (-1 == extensions.indexOf("_vertex_shader")) {
        throw new RuntimeException("No vertex shaders available");
      }
      if (-1 == extensions.indexOf("_fragment_shader")) {
        throw new RuntimeException("No fragment shaders available");
      }
    }

    @Override
    public void reshape(GLAutoDrawable adrawable, int x, int y, int w, int h) {
      drawable = adrawable;
      context = adrawable.getContext();

      /*
      if (glColorFbo[0] != 0) {
        // The screen FBO hack needs the FBO to be recreated when starting
        // and after resizing.
        glColorFbo[0] = 0;
      }
      */
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

    MouseEvent me = new MouseEvent(nativeEvent, nativeEvent.getWhen(),
                                   peAction, peModifiers,
                                   nativeEvent.getX(), nativeEvent.getY(),
                                   peButton,
                                   nativeEvent.getClickCount());

    pg.parent.postEvent(me);
  }

  protected void nativeKeyEvent(com.jogamp.newt.event.KeyEvent nativeEvent,
                                int peAction) {
    int peModifiers = nativeEvent.getModifiers() &
                      (InputEvent.SHIFT_MASK |
                       InputEvent.CTRL_MASK |
                       InputEvent.META_MASK |
                       InputEvent.ALT_MASK);

    KeyEvent ke = new KeyEvent(nativeEvent, nativeEvent.getWhen(),
                               peAction, peModifiers,
                               nativeEvent.getKeyChar(),
                               nativeEvent.getKeyCode());

    pg.parent.postEvent(ke);
  }

  // NEWT mouse listener
  class NEWTMouseAdapter extends com.jogamp.newt.event.MouseAdapter {
    @Override
    public void mousePressed(com.jogamp.newt.event.MouseEvent e) {
      nativeMouseEvent(e, MouseEvent.PRESSED);
    }
    @Override
    public void mouseReleased(com.jogamp.newt.event.MouseEvent e) {
      nativeMouseEvent(e, MouseEvent.RELEASED);
    }
    @Override
    public void mouseClicked(com.jogamp.newt.event.MouseEvent e) {
      nativeMouseEvent(e, MouseEvent.CLICKED);
    }
    @Override
    public void mouseDragged(com.jogamp.newt.event.MouseEvent e) {
      nativeMouseEvent(e, MouseEvent.DRAGGED);
    }
    @Override
    public void mouseMoved(com.jogamp.newt.event.MouseEvent e) {
      nativeMouseEvent(e, MouseEvent.MOVED);
    }
    @Override
    public void mouseEntered(com.jogamp.newt.event.MouseEvent e) {
      nativeMouseEvent(e, MouseEvent.ENTERED);
    }
    @Override
    public void mouseExited(com.jogamp.newt.event.MouseEvent e) {
      nativeMouseEvent(e, MouseEvent.EXITED);
    }
    @Override
    public void mouseWheelMoved(com.jogamp.newt.event.MouseEvent e) {
      // Not supported in Processing.
    }
  }

  // NEWT key listener
  class NEWTKeyAdapter extends com.jogamp.newt.event.KeyAdapter {
    @Override
    public void keyPressed(com.jogamp.newt.event.KeyEvent e) {
      nativeKeyEvent(e, KeyEvent.PRESSED);
    }
    @Override
    public void keyReleased(com.jogamp.newt.event.KeyEvent e) {
      nativeKeyEvent(e, KeyEvent.RELEASED);
    }
    @Override
    public void keyTyped(com.jogamp.newt.event.KeyEvent e)  {
      nativeKeyEvent(e, KeyEvent.TYPED);
    }
  }

  // Animator to drive render loop when using NEWT.
  protected static class NEWTAnimator extends AnimatorBase {
    private static int count = 0;
    private Timer timer = null;
    private TimerTask task = null;
    private volatile boolean shouldRun;

    @Override
    protected String getBaseName(String prefix) {
      return prefix + "NEWTAnimator";
    }

    /** Creates an CustomAnimator with an initial drawable to
     * animate.
     */
    public NEWTAnimator(GLAutoDrawable drawable) {
      if (drawable != null) {
        add(drawable);
      }
    }

    public synchronized void requestDisplay() {
      shouldRun = true;
    }

    public final boolean isStarted() {
      stateSync.lock();
      try {
        return (timer != null);
      } finally {
        stateSync.unlock();
      }
    }

    public final boolean isAnimating() {
      stateSync.lock();
      try {
        return (timer != null) && (task != null);
      } finally {
        stateSync.unlock();
      }
    }

    private void startTask() {
      if(null != task) {
        return;
      }

      task = new TimerTask() {
        private boolean firstRun = true;
        @Override
        public void run() {
          if (firstRun) {
            Thread.currentThread().setName("PGL-RenderQueue-" + count);
            firstRun = false;
            count++;
          }
          if (NEWTAnimator.this.shouldRun) {
            NEWTAnimator.this.animThread = Thread.currentThread();
            // display impl. uses synchronized block on the animator instance
            display();
            synchronized (this) {
              // done with current frame.
              shouldRun = false;
            }
          }
        }
      };

      fpsCounter.resetFPSCounter();
      shouldRun = false;

      timer.schedule(task, 0, 1);
    }

    public synchronized boolean  start() {
      if (timer != null) {
        return false;
      }
      stateSync.lock();
      try {
        timer = new Timer();
        startTask();
      } finally {
        stateSync.unlock();
      }
      return true;
    }

    /** Stops this CustomAnimator. */
    public synchronized boolean stop() {
      if (timer == null) {
        return false;
      }
      stateSync.lock();
      try {
        shouldRun = false;
        if(null != task) {
          task.cancel();
          task = null;
        }
        if(null != timer) {
          timer.cancel();
          timer = null;
        }
        animThread = null;
        try {
          Thread.sleep(20); // ~ 1/60 hz wait, since we can't ctrl stopped threads
        } catch (InterruptedException e) { }
      } finally {
        stateSync.unlock();
      }
      return true;
    }

    public final boolean isPaused() { return false; }
    public synchronized boolean resume() { return false; }
    public synchronized boolean pause() { return false; }
  }
}
