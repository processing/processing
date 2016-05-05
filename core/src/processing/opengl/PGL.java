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

import java.io.IOException;
import java.net.URL;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;
import java.util.regex.Pattern;

import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PGraphics;


/**
 * Processing-OpenGL abstraction layer. Needs to be implemented by subclasses
 * using specific OpenGL-Java bindings.
 *
 * It includes a full GLES 2.0 interface.
 *
 */
public abstract class PGL {
  // ........................................................

  // Basic fields

  /** The PGraphics and PApplet objects using this interface */
  protected PGraphicsOpenGL graphics;
  protected PApplet sketch;

  /** OpenGL thread */
  protected Thread glThread;

  /** ID of the GL context associated to the surface **/
  protected int glContext;

  /** true if this is the GL interface for a primary surface PGraphics */
  public boolean primaryPGL;

  // ........................................................

  // Parameters

  public static int REQUESTED_DEPTH_BITS   = 24;
  public static int REQUESTED_STENCIL_BITS = 8;
  public static int REQUESTED_ALPHA_BITS   = 8;

  /** Switches between the use of regular and direct buffers. */
  protected static boolean USE_DIRECT_BUFFERS = true;
  protected static int MIN_DIRECT_BUFFER_SIZE = 1;

  /** Enables/disables mipmap use. */
  protected static boolean MIPMAPS_ENABLED = true;

  /** Initial sizes for arrays of input and tessellated data. */
  protected static int DEFAULT_IN_VERTICES   = 64;
  protected static int DEFAULT_IN_EDGES      = 128;
  protected static int DEFAULT_IN_TEXTURES   = 64;
  protected static int DEFAULT_TESS_VERTICES = 64;
  protected static int DEFAULT_TESS_INDICES  = 128;

  /** Maximum lights by default is 8, the minimum defined by OpenGL. */
  protected static int MAX_LIGHTS = 8;

  /** Maximum index value of a tessellated vertex. GLES restricts the vertex
   * indices to be of type unsigned short. Since Java only supports signed
   * shorts as primitive type we have 2^15 = 32768 as the maximum number of
   * vertices that can be referred to within a single VBO.
   */
  protected static int MAX_VERTEX_INDEX  = 32767;
  protected static int MAX_VERTEX_INDEX1 = MAX_VERTEX_INDEX + 1;

  /** Count of tessellated fill, line or point vertices that will
   * trigger a flush in the immediate mode. It doesn't necessarily
   * be equal to MAX_VERTEX_INDEX1, since the number of vertices can
   * be effectively much large since the renderer uses offsets to
   * refer to vertices beyond the MAX_VERTEX_INDEX limit.
   */
  protected static int FLUSH_VERTEX_COUNT = MAX_VERTEX_INDEX1;

  /** Minimum/maximum dimensions of a texture used to hold font data. */
  protected static int MIN_FONT_TEX_SIZE = 256;
  protected static int MAX_FONT_TEX_SIZE = 1024;

  /** Minimum stroke weight needed to apply the full path stroking
   * algorithm that properly generates caps and joins.
   */
  protected static float MIN_CAPS_JOINS_WEIGHT = 2f;

  /** Maximum length of linear paths to be stroked with the
   * full algorithm that generates accurate caps and joins.
   */
  protected static int MAX_CAPS_JOINS_LENGTH = 5000;

  /** Minimum array size to use arrayCopy method(). */
  protected static int MIN_ARRAYCOPY_SIZE = 2;

  /** Factor used to displace the stroke vertices towards the camera in
   * order to make sure the lines are always on top of the fill geometry */
  protected static float STROKE_DISPLACEMENT = 0.999f;

  // ........................................................

  // Variables to handle single-buffered situations (i.e.: Android)

  protected IntBuffer firstFrame;
  protected static boolean SINGLE_BUFFERED = false;

  // ........................................................

  // FBO layer

  protected boolean fboLayerEnabled = false;
  protected boolean fboLayerCreated = false;
  protected boolean fboLayerEnabledReq = false;
  protected boolean fboLayerDisableReq = false;
  protected boolean fbolayerResetReq = false;
  public int reqNumSamples;
  protected int numSamples;

  protected IntBuffer glColorFbo;
  protected IntBuffer glColorTex;
  protected IntBuffer glDepthStencil;
  protected IntBuffer glDepth;
  protected IntBuffer glStencil;

  protected IntBuffer glMultiFbo;
  protected IntBuffer glMultiColor;
  protected IntBuffer glMultiDepthStencil;
  protected IntBuffer glMultiDepth;
  protected IntBuffer glMultiStencil;

  protected int fboWidth, fboHeight;
  protected int backTex, frontTex;

  /** Flags used to handle the creation of a separate front texture */
  protected boolean usingFrontTex = false;
  protected boolean needSepFrontTex = false;

  // ........................................................

  // Texture rendering

  protected boolean loadedTex2DShader = false;
  protected int tex2DShaderProgram;
  protected int tex2DVertShader;
  protected int tex2DFragShader;
  protected int tex2DShaderContext;
  protected int tex2DVertLoc;
  protected int tex2DTCoordLoc;
  protected int tex2DSamplerLoc;
  protected int tex2DGeoVBO;

  protected boolean loadedTexRectShader = false;
  protected int texRectShaderProgram;
  protected int texRectVertShader;
  protected int texRectFragShader;
  protected int texRectShaderContext;
  protected int texRectVertLoc;
  protected int texRectTCoordLoc;
  protected int texRectSamplerLoc;
  protected int texRectGeoVBO;

  protected float[] texCoords = {
    //  X,     Y,    U,    V
    -1.0f, -1.0f, 0.0f, 0.0f,
    +1.0f, -1.0f, 1.0f, 0.0f,
    -1.0f, +1.0f, 0.0f, 1.0f,
    +1.0f, +1.0f, 1.0f, 1.0f
  };
  protected FloatBuffer texData;

  protected static final String SHADER_PREPROCESSOR_DIRECTIVE =
    "#ifdef GL_ES\n" +
    "precision mediump float;\n" +
    "precision mediump int;\n" +
    "#endif\n";

  protected static String[] texVertShaderSource = {
    "attribute vec2 position;",
    "attribute vec2 texCoord;",
    "varying vec2 vertTexCoord;",
    "void main() {",
    "  gl_Position = vec4(position, 0, 1);",
    "  vertTexCoord = texCoord;",
    "}"
  };

  protected static String[] tex2DFragShaderSource = {
    SHADER_PREPROCESSOR_DIRECTIVE,
    "uniform sampler2D texMap;",
    "varying vec2 vertTexCoord;",
    "void main() {",
   "  gl_FragColor = texture2D(texMap, vertTexCoord.st);",
    "}"
  };

  protected static String[] texRectFragShaderSource = {
    SHADER_PREPROCESSOR_DIRECTIVE,
    "uniform sampler2DRect texMap;",
    "varying vec2 vertTexCoord;",
    "void main() {",
    "  gl_FragColor = texture2DRect(texMap, vertTexCoord.st);",
    "}"
  };

  /** Which texturing targets are enabled */
  protected boolean[] texturingTargets = { false, false };

  /** Used to keep track of which textures are bound to each target */
  protected int maxTexUnits;
  protected int activeTexUnit = 0;
  protected int[][] boundTextures;

  // ........................................................

  // Framerate handling

  protected float targetFps = 60;
  protected float currentFps = 60;
  protected boolean setFps = false;

  // ........................................................

  // Utility buffers

  protected ByteBuffer byteBuffer;
  protected IntBuffer intBuffer;
  protected IntBuffer viewBuffer;

  protected IntBuffer colorBuffer;
  protected FloatBuffer depthBuffer;
  protected ByteBuffer stencilBuffer;

  //........................................................

  // Rendering information

  /** Used to register amount of geometry rendered in each frame. */
  protected int geomCount = 0;
  protected int pgeomCount;

  /** Used to register calls to background. */
  protected boolean clearColor = false;
  protected boolean pclearColor;

  // ........................................................

  // Error messages

  public static final String WIKI =
    " Read http://wiki.processing.org/w/OpenGL_Issues for help.";

  public static final String FRAMEBUFFER_ERROR =
    "Framebuffer error (%1$s), rendering will probably not work as expected" + WIKI;

  public static final String MISSING_FBO_ERROR =
    "Framebuffer objects are not supported by this hardware (or driver)" + WIKI;

  public static final String MISSING_GLSL_ERROR =
    "GLSL shaders are not supported by this hardware (or driver)" + WIKI;

  public static final String MISSING_GLFUNC_ERROR =
    "GL function %1$s is not available on this hardware (or driver)" + WIKI;

  public static final String UNSUPPORTED_GLPROF_ERROR =
    "Unsupported OpenGL profile.";

  public static final String TEXUNIT_ERROR =
    "Number of texture units not supported by this hardware (or driver)" + WIKI;

  public static final String NONPRIMARY_ERROR =
    "The renderer is trying to call a PGL function that can only be called on a primary PGL. " +
    "This is most likely due to a bug in the renderer's code, please report it with an " +
    "issue on Processing's github page https://github.com/processing/processing/issues?state=open " +
    "if using any of the built-in OpenGL renderers. If you are using a contributed " +
    "library, contact the library's developers.";

  protected static final String DEPTH_READING_NOT_ENABLED_ERROR =
    "Reading depth and stencil values from this multisampled buffer is not enabled. " +
    "You can enable it by calling hint(ENABLE_DEPTH_READING) once. " +
    "If your sketch becomes too slow, disable multisampling with noSmooth() instead.";

  // ........................................................

  // Constants

  /** Size of different types in bytes */
  protected static int SIZEOF_SHORT = Short.SIZE / 8;
  protected static int SIZEOF_INT   = Integer.SIZE / 8;
  protected static int SIZEOF_FLOAT = Float.SIZE / 8;
  protected static int SIZEOF_BYTE  = Byte.SIZE / 8;
  protected static int SIZEOF_INDEX = SIZEOF_SHORT;
  protected static int INDEX_TYPE   = 0x1403; // GL_UNSIGNED_SHORT

  /** Machine Epsilon for float precision. */
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

  // ........................................................

  // Present mode

  // ........................................................

  // Present mode

  protected boolean presentMode = false;
  protected boolean showStopButton = true;
  public float presentX;
  public float presentY;
  protected IntBuffer closeButtonTex;
  protected int stopButtonColor;
  protected int stopButtonWidth = 28;
  protected int stopButtonHeight = 12;
  protected int stopButtonX = 21; // The position of the close button is relative to the
  protected int closeButtonY = 21; // lower left corner
  protected static int[] closeButtonPix = {
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, -1, -1, -1, 0, 0, 0, -1, -1, -1, -1, -1, 0, 0, 0, -1,
    -1, -1, 0, 0, 0, -1, -1, -1, -1, 0, 0, 0, -1, 0, 0, 0, -1, 0, 0, 0, 0, -1, 0,
    0, 0, 0, -1, -1, 0, -1, -1, 0, 0, -1, -1, 0, -1, -1, 0, 0, -1, 0, 0, 0, 0, 0,
    0, 0, 0, -1, 0, 0, 0, 0, -1, 0, 0, 0, -1, 0, 0, -1, 0, 0, 0, -1, 0, 0, 0, -1,
    -1, -1, 0, 0, 0, 0, 0, -1, 0, 0, 0, 0, -1, 0, 0, 0, -1, 0, 0, -1, 0, 0, 0, -1,
    0, 0, 0, 0, 0, 0, -1, 0, 0, 0, 0, -1, 0, 0, 0, 0, -1, 0, 0, 0, -1, 0, 0, -1,
    0, 0, 0, -1, 0, 0, -1, 0, 0, 0, -1, 0, 0, 0, 0, -1, 0, 0, 0, 0, -1, -1, 0, -1,
    -1, 0, 0, -1, -1, 0, -1, -1, 0, 0, 0, -1, -1, -1, 0, 0, 0, 0, 0, -1, -1, -1,
    0, 0, 0, -1, -1, -1, 0, 0, 0, -1, -1, -1, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, 0, 0, 0, 0, 0};


  ///////////////////////////////////////////////////////////////

  // Initialization, finalization


  public PGL() { }


  public PGL(PGraphicsOpenGL pg) {
    this.graphics = pg;
    if (glColorTex == null) {
      glColorFbo = allocateIntBuffer(1);
      glColorTex = allocateIntBuffer(2);
      glDepthStencil = allocateIntBuffer(1);
      glDepth = allocateIntBuffer(1);
      glStencil = allocateIntBuffer(1);

      glMultiFbo = allocateIntBuffer(1);
      glMultiColor = allocateIntBuffer(1);
      glMultiDepthStencil = allocateIntBuffer(1);
      glMultiDepth = allocateIntBuffer(1);
      glMultiStencil = allocateIntBuffer(1);
    }

    byteBuffer = allocateByteBuffer(1);
    intBuffer = allocateIntBuffer(1);
    viewBuffer = allocateIntBuffer(4);
  }


  public void dispose() {
    destroyFBOLayer();
  }


  public void setPrimary(boolean primary) {
    primaryPGL = primary;
  }


  static public int smoothToSamples(int smooth) {
    if (smooth == 0) {
      // smooth(0) is noSmooth(), which is 1x sampling
      return 1;
    } else if (smooth == 1) {
      // smooth(1) means "default smoothing", which is 2x for OpenGL
      return 2;
    } else {
      // smooth(N) can be used for 4x, 8x, etc
      return smooth;
    }
  }


  abstract public Object getNative();


  abstract protected void setFrameRate(float fps);


  abstract protected void initSurface(int antialias);


  abstract protected void reinitSurface();


  abstract protected void registerListeners();


  protected int getReadFramebuffer()  {
    return fboLayerEnabled ? glColorFbo.get(0) : 0;
  }


  protected int getDrawFramebuffer()  {
    if (fboLayerEnabled) return 1 < numSamples ? glMultiFbo.get(0) :
                                                 glColorFbo.get(0);
    else return 0;
  }


  protected int getDefaultDrawBuffer()  {
    return fboLayerEnabled ? COLOR_ATTACHMENT0 : BACK;
  }


  protected int getDefaultReadBuffer()  {
    return fboLayerEnabled ? COLOR_ATTACHMENT0 : FRONT;
  }


  protected boolean isFBOBacked() {;
    return fboLayerEnabled;
  }


  @Deprecated
  public void requestFBOLayer() {
    enableFBOLayer();
  }


  public void enableFBOLayer() {
    fboLayerEnabledReq = true;
  }


  public void disableFBOLayer() {
    fboLayerDisableReq = true;
  }


  public void resetFBOLayer() {
    fbolayerResetReq = true;
  }


  protected boolean isMultisampled() {
    return 1 < numSamples;
  }


  abstract protected int getDepthBits();


  abstract protected int getStencilBits();


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


  protected Texture wrapBackTexture(Texture texture) {
    if (texture == null) {
      texture = new Texture(graphics);
      texture.init(graphics.width, graphics.height,
                   glColorTex.get(backTex), TEXTURE_2D, RGBA,
                   fboWidth, fboHeight, NEAREST, NEAREST,
                   CLAMP_TO_EDGE, CLAMP_TO_EDGE);
      texture.invertedY(true);
      texture.colorBuffer(true);
      graphics.setCache(graphics, texture);
    } else {
      texture.glName = glColorTex.get(backTex);
    }
    return texture;
  }


  protected Texture wrapFrontTexture(Texture texture)  {
    if (texture == null) {
      texture = new Texture(graphics);
      texture.init(graphics.width, graphics.height,
                   glColorTex.get(frontTex), TEXTURE_2D, RGBA,
                   fboWidth, fboHeight, NEAREST, NEAREST,
                   CLAMP_TO_EDGE, CLAMP_TO_EDGE);
      texture.invertedY(true);
      texture.colorBuffer(true);
    } else {
      texture.glName = glColorTex.get(frontTex);
    }
    return texture;
  }


  protected void bindFrontTexture() {
    usingFrontTex = true;
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
    if (usingFrontTex) needSepFrontTex = true;
    if (1 < numSamples) {
      bindFramebufferImpl(READ_FRAMEBUFFER, glMultiFbo.get(0));
      bindFramebufferImpl(DRAW_FRAMEBUFFER, glColorFbo.get(0));
      int mask = COLOR_BUFFER_BIT;
      if (graphics.getHint(PConstants.ENABLE_BUFFER_READING)) {
        mask |= DEPTH_BUFFER_BIT | STENCIL_BUFFER_BIT;
      }
      blitFramebuffer(0, 0, fboWidth, fboHeight,
                      0, 0, fboWidth, fboHeight,
                      mask, NEAREST);
    }
  }


  abstract protected float getPixelScale();

  ///////////////////////////////////////////////////////////

  // Present mode


  public void initPresentMode(float x, float y, int stopColor) {
    presentMode = true;
    showStopButton = stopColor != 0;
    stopButtonColor = stopColor;
    presentX = x;
    presentY = y;
    enableFBOLayer();
  }


  public boolean presentMode() {
    return presentMode;
  }


  public float presentX() {
    return presentX;
  }


  public float presentY() {
    return presentY;
  }


  public boolean insideStopButton(float x, float y) {
    if (!showStopButton) return false;
    return stopButtonX < x && x < stopButtonX + stopButtonWidth &&
           -(closeButtonY + stopButtonHeight) < y && y < -closeButtonY;
  }


  ///////////////////////////////////////////////////////////

  // Frame rendering


  protected void clearBackground(float r, float g, float b, float a, boolean depth) {
    if (depth) {
      clearDepth(1);
      clear(PGL.DEPTH_BUFFER_BIT);
    }
    clearColor(r, g, b, a);
    clear(PGL.COLOR_BUFFER_BIT);
    if (0 < sketch.frameCount) {
      clearColor = true;
    }
  }


  protected void beginRender() {
    if (sketch == null) {
      sketch = graphics.parent;
    }

    pgeomCount = geomCount;
    geomCount = 0;

    pclearColor = clearColor;
    clearColor = false;

    if (SINGLE_BUFFERED && sketch.frameCount == 1) {
      restoreFirstFrame();
    }

    if (fboLayerEnabledReq) {
      fboLayerEnabled = true;
      fboLayerEnabledReq = false;
    }

    if (fboLayerEnabled) {
      if (fbolayerResetReq) {
        destroyFBOLayer();
        fbolayerResetReq = false;
      }
      if (!fboLayerCreated) {
        createFBOLayer();
      }

      // Draw to the back texture
      bindFramebufferImpl(FRAMEBUFFER, glColorFbo.get(0));
      framebufferTexture2D(FRAMEBUFFER, COLOR_ATTACHMENT0,
                           TEXTURE_2D, glColorTex.get(backTex), 0);

      if (1 < numSamples) {
        bindFramebufferImpl(FRAMEBUFFER, glMultiFbo.get(0));
      }

      if (sketch.frameCount == 0) {
        // No need to draw back color buffer because we are in the first frame.
        int argb = graphics.backgroundColor;
        float ba = ((argb >> 24) & 0xff) / 255.0f;
        float br = ((argb >> 16) & 0xff) / 255.0f;
        float bg = ((argb >> 8) & 0xff) / 255.0f;
        float bb = ((argb) & 0xff) / 255.0f;
        clearColor(br, bg, bb, ba);
        clear(COLOR_BUFFER_BIT);
      } else if (!pclearColor || !sketch.isLooping()) {
        // Render previous back texture (now is the front) as background,
        // because no background() is being used ("incremental drawing")
        int x = 0;
        int y = 0;
        if (presentMode) {
          x = (int)presentX;
          y = (int)presentY;
        }
        float scale = getPixelScale();
        drawTexture(TEXTURE_2D, glColorTex.get(frontTex), fboWidth, fboHeight,
                    x, y, graphics.width, graphics.height,
                    0, 0, (int)(scale * graphics.width), (int)(scale * graphics.height),
                    0, 0, graphics.width, graphics.height);
      }
    }
  }


  protected void endRender(int windowColor) {
    if (fboLayerEnabled) {
      syncBackTexture();

      // Draw the contents of the back texture to the screen framebuffer.
      bindFramebufferImpl(FRAMEBUFFER, 0);

      if (presentMode) {
        float wa = ((windowColor >> 24) & 0xff)  / 255.0f;
        float wr = ((windowColor >> 16) & 0xff) / 255.0f;
        float wg = ((windowColor >> 8) & 0xff) / 255.0f;
        float wb = (windowColor & 0xff) / 255.0f;
        clearDepth(1);
        clearColor(wr, wg, wb, wa);
        clear(COLOR_BUFFER_BIT | DEPTH_BUFFER_BIT);

        if (showStopButton) {
          if (closeButtonTex == null) {
            closeButtonTex = allocateIntBuffer(1);
            genTextures(1, closeButtonTex);
            bindTexture(TEXTURE_2D, closeButtonTex.get(0));
            texParameteri(TEXTURE_2D, TEXTURE_MIN_FILTER, NEAREST);
            texParameteri(TEXTURE_2D, TEXTURE_MAG_FILTER, NEAREST);
            texParameteri(TEXTURE_2D, TEXTURE_WRAP_S, CLAMP_TO_EDGE);
            texParameteri(TEXTURE_2D, TEXTURE_WRAP_T, CLAMP_TO_EDGE);
            texImage2D(TEXTURE_2D, 0, RGBA, stopButtonWidth, stopButtonHeight, 0, RGBA, UNSIGNED_BYTE, null);

            int[] color = new int[closeButtonPix.length];
            PApplet.arrayCopy(closeButtonPix, color);


            // Multiply the texture by the button color
            float ba = ((stopButtonColor >> 24) & 0xFF) / 255f;
            float br = ((stopButtonColor >> 16) & 0xFF) / 255f;
            float bg = ((stopButtonColor >>  8) & 0xFF) / 255f;
            float bb = ((stopButtonColor >>  0) & 0xFF) / 255f;
            for (int i = 0; i < color.length; i++) {
              int c = closeButtonPix[i];
              int a = (int)(ba * ((c >> 24) & 0xFF));
              int r = (int)(br * ((c >> 16) & 0xFF));
              int g = (int)(bg * ((c >>  8) & 0xFF));
              int b = (int)(bb * ((c >>  0) & 0xFF));
              color[i] = javaToNativeARGB((a << 24) | (r << 16) | (g << 8) | b);
            }
            IntBuffer buf = allocateIntBuffer(color);
            copyToTexture(TEXTURE_2D, RGBA, closeButtonTex.get(0), 0, 0, stopButtonWidth, stopButtonHeight, buf);
            bindTexture(TEXTURE_2D, 0);
          }
          drawTexture(TEXTURE_2D, closeButtonTex.get(0), stopButtonWidth, stopButtonHeight,
                      0, 0, stopButtonX + stopButtonWidth, closeButtonY + stopButtonHeight,
                      0, stopButtonHeight, stopButtonWidth, 0,
                      stopButtonX, closeButtonY, stopButtonX + stopButtonWidth, closeButtonY + stopButtonHeight);
          }
      } else {
        clearDepth(1);
        clearColor(0, 0, 0, 0);
        clear(COLOR_BUFFER_BIT | DEPTH_BUFFER_BIT);
      }

      // Render current back texture to screen, without blending.
      disable(BLEND);
      int x = 0;
      int y = 0;
      if (presentMode) {
        x = (int)presentX;
        y = (int)presentY;
      }
      float scale = getPixelScale();
      drawTexture(TEXTURE_2D, glColorTex.get(backTex),
                  fboWidth, fboHeight,
                  x, y, graphics.width, graphics.height,
                  0, 0, (int)(scale * graphics.width), (int)(scale * graphics.height),
                  0, 0, graphics.width, graphics.height);

      // Swapping front and back textures.
      int temp = frontTex;
      frontTex = backTex;
      backTex = temp;

      if (fboLayerDisableReq) {
        fboLayerEnabled = false;
        fboLayerDisableReq = false;
      }
    } else {
      if (SINGLE_BUFFERED && sketch.frameCount == 0) {
        saveFirstFrame();
      }

      if (!clearColor && 0 < sketch.frameCount || !sketch.isLooping()) {
        enableFBOLayer();
        if (SINGLE_BUFFERED) {
          createFBOLayer();
        }
      }
    }
  }


  protected abstract void getGL(PGL pgl);


  protected abstract boolean canDraw();


  protected abstract void requestFocus();


  protected abstract void requestDraw();


  protected abstract void swapBuffers();


  public boolean threadIsCurrent()  {
    return Thread.currentThread() == glThread;
  }


  public void setThread(Thread thread) {
    glThread = thread;
  }


  protected void beginGL() { }


  protected void endGL() { }


  private void createFBOLayer() {
    float scale = getPixelScale();

    if (hasNpotTexSupport()) {
      fboWidth = (int)(scale * graphics.width);
      fboHeight = (int)(scale * graphics.height);
    } else {
      fboWidth = nextPowerOfTwo((int)(scale * graphics.width));
      fboHeight = nextPowerOfTwo((int)(scale * graphics.height));
    }

    if (hasFboMultisampleSupport()) {
      int maxs = maxSamples();
      numSamples = PApplet.min(reqNumSamples, maxs);
    } else {
      numSamples = 1;
    }
    boolean multisample = 1 < numSamples;

    boolean packed = hasPackedDepthStencilSupport();
    int depthBits = PApplet.min(REQUESTED_DEPTH_BITS, getDepthBits());
    int stencilBits = PApplet.min(REQUESTED_STENCIL_BITS, getStencilBits());

    genTextures(2, glColorTex);
    for (int i = 0; i < 2; i++) {
      bindTexture(TEXTURE_2D, glColorTex.get(i));
      texParameteri(TEXTURE_2D, TEXTURE_MIN_FILTER, NEAREST);
      texParameteri(TEXTURE_2D, TEXTURE_MAG_FILTER, NEAREST);
      texParameteri(TEXTURE_2D, TEXTURE_WRAP_S, CLAMP_TO_EDGE);
      texParameteri(TEXTURE_2D, TEXTURE_WRAP_T, CLAMP_TO_EDGE);
      texImage2D(TEXTURE_2D, 0, RGBA, fboWidth, fboHeight, 0,
                 RGBA, UNSIGNED_BYTE, null);
      initTexture(TEXTURE_2D, RGBA, fboWidth, fboHeight, graphics.backgroundColor);
    }
    bindTexture(TEXTURE_2D, 0);

    backTex = 0;
    frontTex = 1;

    genFramebuffers(1, glColorFbo);
    bindFramebufferImpl(FRAMEBUFFER, glColorFbo.get(0));
    framebufferTexture2D(FRAMEBUFFER, COLOR_ATTACHMENT0, TEXTURE_2D,
                         glColorTex.get(backTex), 0);

    if (!multisample || graphics.getHint(PConstants.ENABLE_BUFFER_READING)) {
      // If not multisampled, this is the only depth and stencil buffer.
      // If multisampled and depth reading enabled, these are going to
      // hold downsampled depth and stencil buffers.
      createDepthAndStencilBuffer(false, depthBits, stencilBits, packed);
    }

    if (multisample) {
      // Creating multisampled FBO
      genFramebuffers(1, glMultiFbo);
      bindFramebufferImpl(FRAMEBUFFER, glMultiFbo.get(0));

      // color render buffer...
      genRenderbuffers(1, glMultiColor);
      bindRenderbuffer(RENDERBUFFER, glMultiColor.get(0));
      renderbufferStorageMultisample(RENDERBUFFER, numSamples,
                                     RGBA8, fboWidth, fboHeight);
      framebufferRenderbuffer(FRAMEBUFFER, COLOR_ATTACHMENT0,
                              RENDERBUFFER, glMultiColor.get(0));

      // Creating multisampled depth and stencil buffers
      createDepthAndStencilBuffer(true, depthBits, stencilBits, packed);
    }

    validateFramebuffer();

    // Clear all buffers.
    clearDepth(1);
    clearStencil(0);
    int argb = graphics.backgroundColor;
    float ba = ((argb >> 24) & 0xff) / 255.0f;
    float br = ((argb >> 16) & 0xff) / 255.0f;
    float bg = ((argb >> 8) & 0xff) / 255.0f;
    float bb = ((argb) & 0xff) / 255.0f;
    clearColor(br, bg, bb, ba);
    clear(DEPTH_BUFFER_BIT | STENCIL_BUFFER_BIT | COLOR_BUFFER_BIT);

    bindFramebufferImpl(FRAMEBUFFER, 0);
    initFBOLayer();

    fboLayerCreated = true;
  }

  protected abstract void initFBOLayer();


  protected void saveFirstFrame() {
    firstFrame = allocateDirectIntBuffer(graphics.width * graphics.height);
    if (hasReadBuffer()) readBuffer(BACK);
    readPixelsImpl(0, 0, graphics.width, graphics.height, RGBA, UNSIGNED_BYTE, firstFrame);
  }


  protected void restoreFirstFrame() {
    if (firstFrame == null) return;

    IntBuffer tex = allocateIntBuffer(1);
    genTextures(1, tex);

    int w, h;
    float scale = getPixelScale();
    if (hasNpotTexSupport()) {
      w = (int)(scale * graphics.width);
      h = (int)(scale * graphics.height);
    } else {
      w = nextPowerOfTwo((int)(scale * graphics.width));
      h = nextPowerOfTwo((int)(scale * graphics.height));
    }

    bindTexture(TEXTURE_2D, tex.get(0));
    texParameteri(TEXTURE_2D, TEXTURE_MIN_FILTER, NEAREST);
    texParameteri(TEXTURE_2D, TEXTURE_MAG_FILTER, NEAREST);
    texParameteri(TEXTURE_2D, TEXTURE_WRAP_S, CLAMP_TO_EDGE);
    texParameteri(TEXTURE_2D, TEXTURE_WRAP_T, CLAMP_TO_EDGE);
    texImage2D(TEXTURE_2D, 0, RGBA, w, h, 0, RGBA, UNSIGNED_BYTE, null);
    texSubImage2D(TEXTURE_2D, 0, 0, 0, graphics.width, graphics.height, RGBA, UNSIGNED_BYTE, firstFrame);

    drawTexture(TEXTURE_2D, tex.get(0), w, h,
                0, 0, graphics.width, graphics.height,
                0, 0, (int)(scale * graphics.width), (int)(scale * graphics.height),
                0, 0, graphics.width, graphics.height);

    deleteTextures(1, tex);
    firstFrame.clear();
    firstFrame = null;
  }

  protected void destroyFBOLayer() {
    if (threadIsCurrent() && fboLayerCreated) {
      deleteFramebuffers(1, glColorFbo);
      deleteTextures(2, glColorTex);
      deleteRenderbuffers(1, glDepthStencil);
      deleteRenderbuffers(1, glDepth);
      deleteRenderbuffers(1, glStencil);

      deleteFramebuffers(1, glMultiFbo);
      deleteRenderbuffers(1, glMultiColor);
      deleteRenderbuffers(1, glMultiDepthStencil);
      deleteRenderbuffers(1, glMultiDepth);
      deleteRenderbuffers(1, glMultiStencil);
    }
    fboLayerCreated = false;
  }


  private void createDepthAndStencilBuffer(boolean multisample, int depthBits,
                                           int stencilBits, boolean packed) {
    // Creating depth and stencil buffers
    if (packed && depthBits == 24 && stencilBits == 8) {
      // packed depth+stencil buffer
      IntBuffer depthStencilBuf =
          multisample ? glMultiDepthStencil : glDepthStencil;
      genRenderbuffers(1, depthStencilBuf);
      bindRenderbuffer(RENDERBUFFER, depthStencilBuf.get(0));
      if (multisample) {
        renderbufferStorageMultisample(RENDERBUFFER, numSamples,
                                       DEPTH24_STENCIL8, fboWidth, fboHeight);
      } else {
        renderbufferStorage(RENDERBUFFER, DEPTH24_STENCIL8,
                            fboWidth, fboHeight);
      }
      framebufferRenderbuffer(FRAMEBUFFER, DEPTH_ATTACHMENT, RENDERBUFFER,
                              depthStencilBuf.get(0));
      framebufferRenderbuffer(FRAMEBUFFER, STENCIL_ATTACHMENT, RENDERBUFFER,
                              depthStencilBuf.get(0));
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

        IntBuffer depthBuf = multisample ? glMultiDepth : glDepth;
        genRenderbuffers(1, depthBuf);
        bindRenderbuffer(RENDERBUFFER, depthBuf.get(0));
        if (multisample) {
          renderbufferStorageMultisample(RENDERBUFFER, numSamples,
                                         depthComponent, fboWidth, fboHeight);
        } else {
          renderbufferStorage(RENDERBUFFER, depthComponent,
                              fboWidth, fboHeight);
        }
        framebufferRenderbuffer(FRAMEBUFFER, DEPTH_ATTACHMENT,
                                RENDERBUFFER, depthBuf.get(0));
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

        IntBuffer stencilBuf = multisample ? glMultiStencil : glStencil;
        genRenderbuffers(1, stencilBuf);
        bindRenderbuffer(RENDERBUFFER, stencilBuf.get(0));
        if (multisample) {
          renderbufferStorageMultisample(RENDERBUFFER, numSamples,
                                         stencilIndex, fboWidth, fboHeight);
        } else {
          renderbufferStorage(RENDERBUFFER, stencilIndex,
                              fboWidth, fboHeight);
        }
        framebufferRenderbuffer(FRAMEBUFFER, STENCIL_ATTACHMENT,
                                RENDERBUFFER, stencilBuf.get(0));
      }
    }
  }


  ///////////////////////////////////////////////////////////

  // Context interface


  protected int createEmptyContext() {
    return -1;
  }


  protected int getCurrentContext() {
    return glContext;
  }


  ///////////////////////////////////////////////////////////

  // Utility functions


  protected boolean contextIsCurrent(int other) {
    return other == -1 || other == glContext;
  }


  protected void enableTexturing(int target) {
    if (target == TEXTURE_2D) {
      texturingTargets[0] = true;
    } else if (target == TEXTURE_RECTANGLE) {
      texturingTargets[1] = true;
    }
  }


  protected void disableTexturing(int target) {
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
    if (boundTextures == null) return false;

    if (target == TEXTURE_2D) {
      return boundTextures[activeTexUnit][0] == id;
    } else if (target == TEXTURE_RECTANGLE) {
      return boundTextures[activeTexUnit][1] == id;
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
    IntBuffer texels = allocateDirectIntBuffer(16 * 16);
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
                               int w, int h, int[] buffer) {
    copyToTexture(target, format, id, x, y, w, h, IntBuffer.wrap(buffer));

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


  /**
   * Not an approved function, this will change or be removed in the future.
   */
  public void drawTexture(int target, int id, int width, int height,
                          int X0, int Y0, int X1, int Y1) {
    // If a texture is drawing on a viewport of the same size as its resolution,
    // the pixel factor is 1:1, so we override the surface's pixel factor.
    drawTexture(target, id, width, height,
                0, 0, width, height, 1,
                X0, Y0, X1, Y1,
                X0, Y0, X1, Y1);
  }


  /**
   * Not an approved function, this will change or be removed in the future.
   */
  public void drawTexture(int target, int id,int texW, int texH,
                          int viewX, int viewY, int viewW, int viewH,
                          int texX0, int texY0, int texX1, int texY1,
                          int scrX0, int scrY0, int scrX1, int scrY1) {
    int viewF = (int)getPixelScale();
    drawTexture(target, id, texW, texH,
                viewX, viewY, viewW, viewH, viewF,
                texX0, texY0, texX1, texY1,
                scrX0, scrY0, scrX1, scrY1);
  }


  public void drawTexture(int target, int id,int texW, int texH,
                          int viewX, int viewY, int viewW, int viewH, int viewF,
                          int texX0, int texY0, int texX1, int texY1,
                          int scrX0, int scrY0, int scrX1, int scrY1) {
    if (target == TEXTURE_2D) {
      drawTexture2D(id, texW, texH,
                    viewX, viewY, viewW, viewH, viewF,
                    texX0, texY0, texX1, texY1,
                    scrX0, scrY0, scrX1, scrY1);
    } else if (target == TEXTURE_RECTANGLE) {
      drawTextureRect(id, texW, texH,
                      viewX, viewY, viewW, viewH, viewF,
                      texX0, texY0, texX1, texY1,
                      scrX0, scrY0, scrX1, scrY1);
    }
  }


  protected PGL initTex2DShader() {
    PGL ppgl = primaryPGL ? this : graphics.getPrimaryPGL();

    if (!ppgl.loadedTex2DShader || ppgl.tex2DShaderContext != ppgl.glContext) {
      String[] preprocVertSrc = preprocessVertexSource(texVertShaderSource, getGLSLVersion());
      String vertSource = PApplet.join(preprocVertSrc, "\n");
      String[] preprocFragSrc = preprocessFragmentSource(tex2DFragShaderSource, getGLSLVersion());
      String fragSource = PApplet.join(preprocFragSrc, "\n");
      ppgl.tex2DVertShader = createShader(VERTEX_SHADER, vertSource);
      ppgl.tex2DFragShader = createShader(FRAGMENT_SHADER, fragSource);
      if (0 < ppgl.tex2DVertShader && 0 < ppgl.tex2DFragShader) {
        ppgl.tex2DShaderProgram = createProgram(ppgl.tex2DVertShader, ppgl.tex2DFragShader);
      }
      if (0 < ppgl.tex2DShaderProgram) {
        ppgl.tex2DVertLoc = getAttribLocation(ppgl.tex2DShaderProgram, "position");
        ppgl.tex2DTCoordLoc = getAttribLocation(ppgl.tex2DShaderProgram, "texCoord");
        ppgl.tex2DSamplerLoc = getUniformLocation(ppgl.tex2DShaderProgram, "texMap");
      }
      ppgl.loadedTex2DShader = true;
      ppgl.tex2DShaderContext = ppgl.glContext;

      genBuffers(1, intBuffer);
      ppgl.tex2DGeoVBO = intBuffer.get(0);
      bindBuffer(ARRAY_BUFFER, ppgl.tex2DGeoVBO);
      bufferData(ARRAY_BUFFER, 16 * SIZEOF_FLOAT, null, STATIC_DRAW);
    }

    if (texData == null) {
      texData = allocateDirectFloatBuffer(texCoords.length);
    }

    return ppgl;
  }


  protected void drawTexture2D(int id, int texW, int texH,
                               int viewX, int viewY, int viewW, int viewH, int viewF,
                               int texX0, int texY0, int texX1, int texY1,
                               int scrX0, int scrY0, int scrX1, int scrY1) {
    PGL ppgl = initTex2DShader();

    if (0 < ppgl.tex2DShaderProgram) {
      // The texture overwrites anything drawn earlier.
      boolean depthTest = getDepthTest();
      disable(DEPTH_TEST);

      // When drawing the texture we don't write to the
      // depth mask, so the texture remains in the background
      // and can be occluded by anything drawn later, even if
      // if it is behind it.
      boolean depthMask = getDepthWriteMask();
      depthMask(false);

      // Making sure that the viewport matches the provided screen dimensions
      viewBuffer.rewind();
      getIntegerv(VIEWPORT, viewBuffer);
      viewportImpl(viewF * viewX, viewF * viewY, viewF * viewW, viewF * viewH);

      useProgram(ppgl.tex2DShaderProgram);

      enableVertexAttribArray(ppgl.tex2DVertLoc);
      enableVertexAttribArray(ppgl.tex2DTCoordLoc);

      // Vertex coordinates of the textured quad are specified
      // in normalized screen space (-1, 1):
      // Corner 1
      texCoords[ 0] = 2 * (float)scrX0 / viewW - 1;
      texCoords[ 1] = 2 * (float)scrY0 / viewH - 1;
      texCoords[ 2] = (float)texX0 / texW;
      texCoords[ 3] = (float)texY0 / texH;
      // Corner 2
      texCoords[ 4] = 2 * (float)scrX1 / viewW - 1;
      texCoords[ 5] = 2 * (float)scrY0 / viewH - 1;
      texCoords[ 6] = (float)texX1 / texW;
      texCoords[ 7] = (float)texY0 / texH;
      // Corner 3
      texCoords[ 8] = 2 * (float)scrX0 / viewW - 1;
      texCoords[ 9] = 2 * (float)scrY1 / viewH - 1;
      texCoords[10] = (float)texX0 / texW;
      texCoords[11] = (float)texY1 / texH;
      // Corner 4
      texCoords[12] = 2 * (float)scrX1 / viewW - 1;
      texCoords[13] = 2 * (float)scrY1 / viewH - 1;
      texCoords[14] = (float)texX1 / texW;
      texCoords[15] = (float)texY1 / texH;

      texData.rewind();
      texData.put(texCoords);

      activeTexture(TEXTURE0);
      boolean enabledTex = false;
      if (!texturingIsEnabled(TEXTURE_2D)) {
        enableTexturing(TEXTURE_2D);
        enabledTex = true;
      }
      bindTexture(TEXTURE_2D, id);
      uniform1i(ppgl.tex2DSamplerLoc, 0);

      texData.position(0);
      bindBuffer(ARRAY_BUFFER, ppgl.tex2DGeoVBO);
      bufferData(ARRAY_BUFFER, 16 * SIZEOF_FLOAT, texData, STATIC_DRAW);

      vertexAttribPointer(ppgl.tex2DVertLoc, 2, FLOAT, false, 4 * SIZEOF_FLOAT, 0);
      vertexAttribPointer(ppgl.tex2DTCoordLoc, 2, FLOAT, false, 4 * SIZEOF_FLOAT, 2 * SIZEOF_FLOAT);

      drawArrays(TRIANGLE_STRIP, 0, 4);

      bindBuffer(ARRAY_BUFFER, 0); // Making sure that no VBO is bound at this point.

      bindTexture(TEXTURE_2D, 0);
      if (enabledTex) {
        disableTexturing(TEXTURE_2D);
      }

      disableVertexAttribArray(ppgl.tex2DVertLoc);
      disableVertexAttribArray(ppgl.tex2DTCoordLoc);

      useProgram(0);

      if (depthTest) {
        enable(DEPTH_TEST);
      } else {
        disable(DEPTH_TEST);
      }
      depthMask(depthMask);

      viewportImpl(viewBuffer.get(0), viewBuffer.get(1),
                   viewBuffer.get(2), viewBuffer.get(3));
    }
  }


  protected PGL initTexRectShader() {
    PGL ppgl = primaryPGL ? this : graphics.getPrimaryPGL();

    if (!ppgl.loadedTexRectShader || ppgl.texRectShaderContext != ppgl.glContext) {
      String[] preprocVertSrc = preprocessVertexSource(texVertShaderSource, getGLSLVersion());
      String vertSource = PApplet.join(preprocVertSrc, "\n");
      String[] preprocFragSrc = preprocessFragmentSource(texRectFragShaderSource, getGLSLVersion());
      String fragSource = PApplet.join(preprocFragSrc, "\n");
      ppgl.texRectVertShader = createShader(VERTEX_SHADER, vertSource);
      ppgl.texRectFragShader = createShader(FRAGMENT_SHADER, fragSource);
      if (0 < ppgl.texRectVertShader && 0 < ppgl.texRectFragShader) {
        ppgl.texRectShaderProgram = createProgram(ppgl.texRectVertShader,
                                                  ppgl.texRectFragShader);
      }
      if (0 < ppgl.texRectShaderProgram) {
        ppgl.texRectVertLoc = getAttribLocation(ppgl.texRectShaderProgram, "position");
        ppgl.texRectTCoordLoc = getAttribLocation(ppgl.texRectShaderProgram, "texCoord");
        ppgl.texRectSamplerLoc = getUniformLocation(ppgl.texRectShaderProgram, "texMap");
      }
      ppgl.loadedTexRectShader = true;
      ppgl.texRectShaderContext = ppgl.glContext;

      genBuffers(1, intBuffer);
      ppgl.texRectGeoVBO = intBuffer.get(0);
      bindBuffer(ARRAY_BUFFER, ppgl.texRectGeoVBO);
      bufferData(ARRAY_BUFFER, 16 * SIZEOF_FLOAT, null, STATIC_DRAW);
    }

    return ppgl;
  }


  protected void drawTextureRect(int id, int texW, int texH,
                                 int viewX, int viewY, int viewW, int viewH, int viewF,
                                 int texX0, int texY0, int texX1, int texY1,
                                 int scrX0, int scrY0, int scrX1, int scrY1) {
    PGL ppgl = initTexRectShader();

    if (texData == null) {
      texData = allocateDirectFloatBuffer(texCoords.length);
    }

    if (0 < ppgl.texRectShaderProgram) {
      // The texture overwrites anything drawn earlier.
      boolean depthTest = getDepthTest();
      disable(DEPTH_TEST);

      // When drawing the texture we don't write to the
      // depth mask, so the texture remains in the background
      // and can be occluded by anything drawn later, even if
      // if it is behind it.
      boolean depthMask = getDepthWriteMask();
      depthMask(false);

      // Making sure that the viewport matches the provided screen dimensions
      viewBuffer.rewind();
      getIntegerv(VIEWPORT, viewBuffer);
      viewportImpl(viewF * viewX, viewF * viewY, viewF * viewW, viewF * viewH);

      useProgram(ppgl.texRectShaderProgram);

      enableVertexAttribArray(ppgl.texRectVertLoc);
      enableVertexAttribArray(ppgl.texRectTCoordLoc);

      // Vertex coordinates of the textured quad are specified
      // in normalized screen space (-1, 1):
      // Corner 1
      texCoords[ 0] = 2 * (float)scrX0 / viewW - 1;
      texCoords[ 1] = 2 * (float)scrY0 / viewH - 1;
      texCoords[ 2] = texX0;
      texCoords[ 3] = texY0;
      // Corner 2
      texCoords[ 4] = 2 * (float)scrX1 / viewW - 1;
      texCoords[ 5] = 2 * (float)scrY0 / viewH - 1;
      texCoords[ 6] = texX1;
      texCoords[ 7] = texY0;
      // Corner 3
      texCoords[ 8] = 2 * (float)scrX0 / viewW - 1;
      texCoords[ 9] = 2 * (float)scrY1 / viewH - 1;
      texCoords[10] = texX0;
      texCoords[11] = texY1;
      // Corner 4
      texCoords[12] = 2 * (float)scrX1 / viewW - 1;
      texCoords[13] = 2 * (float)scrY1 / viewH - 1;
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
      uniform1i(ppgl.texRectSamplerLoc, 0);

      texData.position(0);
      bindBuffer(ARRAY_BUFFER, ppgl.texRectGeoVBO);
      bufferData(ARRAY_BUFFER, 16 * SIZEOF_FLOAT, texData, STATIC_DRAW);

      vertexAttribPointer(ppgl.texRectVertLoc, 2, FLOAT, false, 4 * SIZEOF_FLOAT, 0);
      vertexAttribPointer(ppgl.texRectTCoordLoc, 2, FLOAT, false, 4 * SIZEOF_FLOAT, 2 * SIZEOF_FLOAT);

      drawArrays(TRIANGLE_STRIP, 0, 4);

      bindBuffer(ARRAY_BUFFER, 0); // Making sure that no VBO is bound at this point.

      bindTexture(TEXTURE_RECTANGLE, 0);
      if (enabledTex) {
        disableTexturing(TEXTURE_RECTANGLE);
      }

      disableVertexAttribArray(ppgl.texRectVertLoc);
      disableVertexAttribArray(ppgl.texRectTCoordLoc);

      useProgram(0);

      if (depthTest) {
        enable(DEPTH_TEST);
      } else {
        disable(DEPTH_TEST);
      }
      depthMask(depthMask);

      viewportImpl(viewBuffer.get(0), viewBuffer.get(1),
                   viewBuffer.get(2), viewBuffer.get(3));
    }
  }


  protected int getColorValue(int scrX, int scrY) {
    if (colorBuffer == null) {
      colorBuffer = IntBuffer.allocate(1);
    }
    colorBuffer.rewind();
    readPixels(scrX, graphics.height - scrY - 1, 1, 1, RGBA, UNSIGNED_BYTE,
               colorBuffer);
    return colorBuffer.get();
  }


  protected float getDepthValue(int scrX, int scrY) {
    if (depthBuffer == null) {
      depthBuffer = FloatBuffer.allocate(1);
    }
    depthBuffer.rewind();
    readPixels(scrX, graphics.height - scrY - 1, 1, 1, DEPTH_COMPONENT, FLOAT,
               depthBuffer);
    return depthBuffer.get(0);
  }


  protected byte getStencilValue(int scrX, int scrY) {
    if (stencilBuffer == null) {
      stencilBuffer = ByteBuffer.allocate(1);
    }
    stencilBuffer.rewind();
    readPixels(scrX, graphics.height - scrY - 1, 1, 1, STENCIL_INDEX,
               UNSIGNED_BYTE, stencilBuffer);
    return stencilBuffer.get(0);
  }


  protected static boolean isPowerOfTwo(int val) {
    return (val & (val - 1)) == 0;
  }


  // bit shifting this might be more efficient
  protected static int nextPowerOfTwo(int val) {
    int ret = 1;
    while (ret < val) ret <<= 1;
    return ret;
  }


  /**
   * Converts input native OpenGL value (RGBA on big endian, ABGR on little
   * endian) to Java ARGB.
   */
  protected static int nativeToJavaARGB(int color) {
    if (BIG_ENDIAN) { // RGBA to ARGB
      return (color >>> 8) | (color << 24);
    } else { // ABGR to ARGB
      int rb = color & 0x00FF00FF;
      return (color & 0xFF00FF00) | (rb << 16) | (rb >> 16);
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
      for (int x = 0; x < width; x++) {
        int pixy = pixels[yindex];
        int pixi = pixels[index];
        if (BIG_ENDIAN) { // RGBA to ARGB
          pixels[index] = (pixy >>> 8) | (pixy << 24);
          pixels[yindex] = (pixi >>> 8) | (pixi << 24);
        } else { // ABGR to ARGB
          int rbi = pixi & 0x00FF00FF;
          int rby = pixy & 0x00FF00FF;
          pixels[index] = (pixy & 0xFF00FF00) | (rby << 16) | (rby >> 16);
          pixels[yindex] = (pixi & 0xFF00FF00) | (rbi << 16) | (rbi >> 16);
        }
        index++;
        yindex++;
      }
      yindex -= width * 2;
    }

    if ((height % 2) == 1) { // Converts center row
      index = (height / 2) * width;
      for (int x = 0; x < width; x++) {
        int pixi = pixels[index];
        if (BIG_ENDIAN) { // RGBA to ARGB
          pixels[index] = (pixi >>> 8) | (pixi << 24);
        } else { // ABGR to ARGB
          int rbi = pixi & 0x00FF00FF;
          pixels[index] = (pixi & 0xFF00FF00) | (rbi << 16) | (rbi >> 16);
        }
        index++;
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
      return (color >>> 8) | 0xFF000000;
    } else { // ABGR to ARGB
      int rb = color & 0x00FF00FF;
      return 0xFF000000 | (rb << 16) |
             (color & 0x0000FF00) | (rb >> 16);
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
      for (int x = 0; x < width; x++) {
        int pixy = pixels[yindex];
        int pixi = pixels[index];
        if (BIG_ENDIAN) { // RGBA to ARGB
          pixels[index] = (pixy >>> 8) | 0xFF000000;
          pixels[yindex] = (pixi >>> 8) | 0xFF000000;
        } else { // ABGR to ARGB
          int rbi = pixi & 0x00FF00FF;
          int rby = pixy & 0x00FF00FF;
          pixels[index] = 0xFF000000 | (rby << 16) |
                          (pixy & 0x0000FF00) | (rby >> 16);
          pixels[yindex] = 0xFF000000 | (rbi << 16) |
                           (pixi & 0x0000FF00) | (rbi >> 16);
        }
        index++;
        yindex++;
      }
      yindex -= width * 2;
    }

    if ((height % 2) == 1) { // Converts center row
      index = (height / 2) * width;
      for (int x = 0; x < width; x++) {
        int pixi = pixels[index];
        if (BIG_ENDIAN) { // RGBA to ARGB
          pixels[index] = (pixi >>> 8) | 0xFF000000;
        } else { // ABGR to ARGB
          int rbi = pixi & 0x00FF00FF;
          pixels[index] = 0xFF000000 | (rbi << 16) |
                          (pixi & 0x000FF00) | (rbi >> 16);
        }
        index++;
      }
    }
  }


  /**
   * Converts input Java ARGB value to native OpenGL format (RGBA on big endian,
   * BGRA on little endian).
   */
  protected static int javaToNativeARGB(int color) {
    if (BIG_ENDIAN) { // ARGB to RGBA
      return (color >>> 24) | (color << 8);
    } else { // ARGB to ABGR
      int rb = color & 0x00FF00FF;
      return (color & 0xFF00FF00) | (rb << 16) | (rb >> 16);
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
      for (int x = 0; x < width; x++) {
        int pixy = pixels[yindex];
        int pixi = pixels[index];
        if (BIG_ENDIAN) { // ARGB to RGBA
          pixels[index] = (pixy >>> 24) | (pixy << 8);
          pixels[yindex] = (pixi >>> 24) | (pixi << 8);
        } else { // ARGB to ABGR
          int rbi = pixi & 0x00FF00FF;
          int rby = pixy & 0x00FF00FF;
          pixels[index] = (pixy & 0xFF00FF00) | (rby << 16) | (rby >> 16);
          pixels[yindex] = (pixi & 0xFF00FF00) | (rbi << 16) | (rbi >> 16);
        }
        index++;
        yindex++;
      }
      yindex -= width * 2;
    }

    if ((height % 2) == 1) { // Converts center row
      index = (height / 2) * width;
      for (int x = 0; x < width; x++) {
        int pixi = pixels[index];
        if (BIG_ENDIAN) { // ARGB to RGBA
          pixels[index] = (pixi >>> 24) | (pixi << 8);
        } else { // ARGB to ABGR
          int rbi = pixi & 0x00FF00FF;
          pixels[index] = (pixi & 0xFF00FF00) | (rbi << 16) | (rbi >> 16);
        }
        index++;
      }
    }
  }


  /**
   * Converts input Java ARGB value to native OpenGL format (RGBA on big endian,
   * BGRA on little endian), setting alpha component to opaque (255).
   */
  protected static int javaToNativeRGB(int color) {
    if (BIG_ENDIAN) { // ARGB to RGB
      return 0xFF | (color << 8);
    } else { // ARGB to BGR
      int rb = color & 0x00FF00FF;
      return 0xFF000000 | (rb << 16) | (color & 0x0000FF00) | (rb >> 16);
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
      for (int x = 0; x < width; x++) {
        int pixy = pixels[yindex];
        int pixi = pixels[index];
        if (BIG_ENDIAN) { // ARGB to RGB
          pixels[index] = 0xFF | (pixy << 8);
          pixels[yindex] = 0xFF | (pixi << 8);
        } else { // ARGB to BGR
          int rbi = pixi & 0x00FF00FF;
          int rby = pixy & 0x00FF00FF;
          pixels[index] = 0xFF000000 | (rby << 16) |
                          (pixy & 0x0000FF00) | (rby >> 16);
          pixels[yindex] = 0xFF000000 | (rbi << 16) |
                           (pixi & 0x0000FF00) | (rbi >> 16);
        }
        index++;
        yindex++;
      }
      yindex -= width * 2;
    }

    if ((height % 2) == 1) { // Converts center row
      index = (height / 2) * width;
      for (int x = 0; x < width; x++) {
        int pixi = pixels[index];
        if (BIG_ENDIAN) { // ARGB to RGB
          pixels[index] = 0xFF | (pixi << 8);
        } else { // ARGB to BGR
          int rbi = pixi & 0x00FF00FF;
          pixels[index] = 0xFF000000 | (rbi << 16) |
                          (pixi & 0x0000FF00) | (rbi >> 16);
        }
        index++;
      }
    }
  }


  protected static int qualityToSamples(int quality) {
    if (quality <= 1) {
      return 1;
    } else {
      // Number of samples is always an even number:
      int n = 2 * (quality / 2);
      return n;
    }
  }


  abstract protected int getGLSLVersion();


  protected String[] loadVertexShader(String filename) {
    return sketch.loadStrings(filename);
  }


  protected String[] loadFragmentShader(String filename) {
    return sketch.loadStrings(filename);
  }


  protected String[] loadFragmentShader(URL url) {
    try {
      return PApplet.loadStrings(url.openStream());
    } catch (IOException e) {
      PGraphics.showException("Cannot load fragment shader " + url.getFile());
    }
    return null;
  }


  protected String[] loadVertexShader(URL url) {
    try {
      return PApplet.loadStrings(url.openStream());
    } catch (IOException e) {
      PGraphics.showException("Cannot load vertex shader " + url.getFile());
    }
    return null;
  }


  protected String[] loadVertexShader(String filename, int version) {
    return loadVertexShader(filename);
  }


  protected String[] loadFragmentShader(String filename, int version) {
    return loadFragmentShader(filename);
  }


  protected String[] loadFragmentShader(URL url, int version) {
    return loadFragmentShader(url);
  }


  protected String[] loadVertexShader(URL url, int version) {
    return loadVertexShader(url);
  }


  protected static String[] preprocessFragmentSource(String[] fragSrc0,
                                                     int version) {
    if (containsVersionDirective(fragSrc0)) {
      // The user knows what she or he is doing
      return fragSrc0;
    }

    String[] fragSrc;

    if (version < 130) {
      Pattern[] search = { };
      String[] replace = { };
      int offset = 1;

      fragSrc = preprocessShaderSource(fragSrc0, search, replace, offset);
      fragSrc[0] = "#version " + version;
    } else {
      // We need to replace 'texture' uniform by 'texMap' uniform and
      // 'textureXXX()' functions by 'texture()' functions. Order of these
      // replacements is important to prevent collisions between these two.
      Pattern[] search = new Pattern[] {
          Pattern.compile(String.format(GLSL_ID_REGEX, "varying|attribute")),
          Pattern.compile(String.format(GLSL_ID_REGEX, "texture")),
          Pattern.compile(String.format(GLSL_FN_REGEX, "textureRect|texture2D|texture3D|textureCube")),
          Pattern.compile(String.format(GLSL_ID_REGEX, "gl_FragColor"))
      };
      String[] replace = new String[] {
          "in", "texMap", "texture", "_fragColor"
      };
      int offset = 2;

      fragSrc = preprocessShaderSource(fragSrc0, search, replace, offset);
      fragSrc[0] = "#version " + version;
      fragSrc[1] = "out vec4 _fragColor;";
    }

    return fragSrc;
  }

  protected static String[] preprocessVertexSource(String[] vertSrc0,
                                                   int version) {
    if (containsVersionDirective(vertSrc0)) {
      // The user knows what she or he is doing
      return vertSrc0;
    }

    String[] vertSrc;

    if (version < 130) {
      Pattern[] search = { };
      String[] replace = { };
      int offset = 1;

      vertSrc = preprocessShaderSource(vertSrc0, search, replace, offset);
      vertSrc[0] = "#version " + version;
    } else {
      // We need to replace 'texture' uniform by 'texMap' uniform and
      // 'textureXXX()' functions by 'texture()' functions. Order of these
      // replacements is important to prevent collisions between these two.
      Pattern[] search = new Pattern[] {
          Pattern.compile(String.format(GLSL_ID_REGEX, "varying")),
          Pattern.compile(String.format(GLSL_ID_REGEX, "attribute")),
          Pattern.compile(String.format(GLSL_ID_REGEX, "texture")),
          Pattern.compile(String.format(GLSL_FN_REGEX, "textureRect|texture2D|texture3D|textureCube"))
      };
      String[] replace = new String[] {
          "out", "in", "texMap", "texture",
      };
      int offset = 1;

      vertSrc = preprocessShaderSource(vertSrc0, search, replace, offset);
      vertSrc[0] = "#version " + version;
    }

    return vertSrc;
  }


  protected static final String GLSL_ID_REGEX = "(?<![0-9A-Z_a-z])(%s)(?![0-9A-Z_a-z]|\\s*\\()";
  protected static final String GLSL_FN_REGEX = "(?<![0-9A-Z_a-z])(%s)(?=\\s*\\()";


  protected static String[] preprocessShaderSource(String[] src0,
                                                   Pattern[] search,
                                                   String[] replace,
                                                   int offset) {
    String[] src = new String[src0.length+offset];
    for (int i = 0; i < src0.length; i++) {
      String line = src0[i];
      if (line.contains("#version")) {
        line = "";
      }
      for (int j = 0; j < search.length; j++) {
        line = search[j].matcher(line).replaceAll(replace[j]);
      }
      src[i+offset] = line;
    }
    return src;
  }

  protected static boolean containsVersionDirective(String[] shSrc) {
    for (int i = 0; i < shSrc.length; i++) {
      String line = shSrc[i];
      if (line.contains("#version")) {
        return true;
      }
    }
    return false;
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
      System.err.println(String.format(FRAMEBUFFER_ERROR,
                                       "incomplete attachment"));
    } else if (status == FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT) {
      System.err.println(String.format(FRAMEBUFFER_ERROR,
                                       "incomplete missing attachment"));
    } else if (status == FRAMEBUFFER_INCOMPLETE_DIMENSIONS) {
      System.err.println(String.format(FRAMEBUFFER_ERROR,
                                       "incomplete dimensions"));
    } else if (status == FRAMEBUFFER_INCOMPLETE_FORMATS) {
      System.err.println(String.format(FRAMEBUFFER_ERROR,
                                       "incomplete formats"));
    } else if (status == FRAMEBUFFER_UNSUPPORTED) {
      System.err.println(String.format(FRAMEBUFFER_ERROR,
                                       "framebuffer unsupported"));
    } else {
      System.err.println(String.format(FRAMEBUFFER_ERROR,
                                       "unknown error"));
    }
    return false;
  }

  protected boolean isES() {
    return getString(VERSION).trim().toLowerCase().contains("opengl es");
  }

  protected int[] getGLVersion() {
    String version = getString(VERSION).trim().toLowerCase();

    String ES = "opengl es";
    int esPosition = version.indexOf(ES);
    if (esPosition >= 0) {
      version = version.substring(esPosition + ES.length()).trim();
    }

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
    // FBOs might still be available through extensions.
    int major = getGLVersion()[0];
    if (major < 2) {
      String ext = getString(EXTENSIONS);
      return ext.indexOf("_framebuffer_object") != -1 &&
             ext.indexOf("_vertex_shader")      != -1 &&
             ext.indexOf("_shader_objects")     != -1 &&
             ext.indexOf("_shading_language")   != -1;
    } else {
      return true;
    }
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
    } else {
      return true;
    }
  }


  protected boolean hasNpotTexSupport() {
    int major = getGLVersion()[0];
    if (major < 3) {
      String ext = getString(EXTENSIONS);
      if (isES()) {
        return -1 < ext.indexOf("_texture_npot");
      } else {
        return -1 < ext.indexOf("_texture_non_power_of_two");
      }
    } else {
      return true;
    }
  }


  protected boolean hasAutoMipmapGenSupport() {
    int major = getGLVersion()[0];
    if (major < 3) {
      String ext = getString(EXTENSIONS);
      return -1 < ext.indexOf("_generate_mipmap");
    } else {
      return true;
    }
  }


  protected boolean hasFboMultisampleSupport() {
    int major = getGLVersion()[0];
    if (major < 3) {
      String ext = getString(EXTENSIONS);
      return -1 < ext.indexOf("_framebuffer_multisample");
    } else {
      return true;
    }
  }


  protected boolean hasPackedDepthStencilSupport() {
    int major = getGLVersion()[0];
    if (major < 3) {
      String ext = getString(EXTENSIONS);
      return -1 < ext.indexOf("_packed_depth_stencil");
    } else {
      return true;
    }
  }


  protected boolean hasAnisoSamplingSupport() {
    int major = getGLVersion()[0];
    if (major < 3) {
      String ext = getString(EXTENSIONS);
      return -1 < ext.indexOf("_texture_filter_anisotropic");
    } else {
      return true;
    }
  }


  protected boolean hasSynchronization() {
    int[] version = getGLVersion();
    if (isES()) {
      return version[0] >= 3;
    }
    return (version[0] > 3) || (version[0] == 3 && version[1] >= 2);
  }


  protected boolean hasPBOs() {
    int[] version = getGLVersion();
    if (isES()) {
      return version[0] >= 3;
    }
    return (version[0] > 2) || (version[0] == 2 && version[1] >= 1);
  }


  protected boolean hasReadBuffer() {
    int[] version = getGLVersion();
    if (isES()) {
      return version[0] >= 3;
    }
    return version[0] >= 2;
  }


  protected boolean hasDrawBuffer() {
    int[] version = getGLVersion();
    if (isES()) {
      return version[0] >= 3;
    }
    return version[0] >= 2;
  }


  protected int maxSamples() {
    intBuffer.rewind();
    getIntegerv(MAX_SAMPLES, intBuffer);
    return intBuffer.get(0);
  }


  protected int getMaxTexUnits() {
    intBuffer.rewind();
    getIntegerv(MAX_TEXTURE_IMAGE_UNITS, intBuffer);
    return intBuffer.get(0);
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
      ByteBuffer buf = allocateDirectByteBuffer(arr.length);
      buf.put(arr);
      buf.position(0);
      return buf;
    } else {
      return ByteBuffer.wrap(arr);
    }
  }


  protected static ByteBuffer updateByteBuffer(ByteBuffer buf, byte[] arr,
                                               boolean wrap) {
    if (USE_DIRECT_BUFFERS) {
      if (buf == null || buf.capacity() < arr.length) {
        buf = allocateDirectByteBuffer(arr.length);
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
      ShortBuffer buf = allocateDirectShortBuffer(arr.length);
      buf.put(arr);
      buf.position(0);
      return buf;
    } else {
      return ShortBuffer.wrap(arr);
    }
  }


  protected static ShortBuffer updateShortBuffer(ShortBuffer buf, short[] arr,
                                                 boolean wrap) {
    if (USE_DIRECT_BUFFERS) {
      if (buf == null || buf.capacity() < arr.length) {
        buf = allocateDirectShortBuffer(arr.length);
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
      IntBuffer buf = allocateDirectIntBuffer(arr.length);
      buf.put(arr);
      buf.position(0);
      return buf;
    } else {
      return IntBuffer.wrap(arr);
    }
  }


  protected static IntBuffer updateIntBuffer(IntBuffer buf, int[] arr,
                                             boolean wrap) {
    if (USE_DIRECT_BUFFERS) {
      if (buf == null || buf.capacity() < arr.length) {
        buf = allocateDirectIntBuffer(arr.length);
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
      FloatBuffer buf = allocateDirectFloatBuffer(arr.length);
      buf.put(arr);
      buf.position(0);
      return buf;
    } else {
      return FloatBuffer.wrap(arr);
    }
  }


  protected static FloatBuffer updateFloatBuffer(FloatBuffer buf, float[] arr,
                                                 boolean wrap) {
    if (USE_DIRECT_BUFFERS) {
      if (buf == null || buf.capacity() < arr.length) {
        buf = allocateDirectFloatBuffer(arr.length);
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


  // TODO: the next three functions shouldn't be here...
  // Uses 'Object' so that the API can be used w/ Android Typeface objects

  abstract protected int getFontAscent(Object font);


  abstract protected int getFontDescent(Object font);


  abstract protected int getTextWidth(Object font, char[] buffer, int start, int stop);


  abstract protected Object getDerivedFont(Object font, float size);


  ///////////////////////////////////////////////////////////

  // Tessellator interface


  protected abstract Tessellator createTessellator(TessellatorCallback callback);


  protected interface Tessellator {
    public void beginPolygon();
    public void endPolygon();
    public void setWindingRule(int rule);
    public void beginContour();
    public void endContour();
    public void addVertex(double[] v);
  }


  protected interface TessellatorCallback  {
    public void begin(int type);
    public void end();
    public void vertex(Object data);
    public void combine(double[] coords, Object[] data,
                        float[] weight, Object[] outData);
    public void error(int errnum);
  }


  protected String tessError(int err) {
    return "";
  }


  ///////////////////////////////////////////////////////////

  // FontOutline interface


  protected static boolean SHAPE_TEXT_SUPPORTED;
  protected static int SEG_MOVETO;
  protected static int SEG_LINETO;
  protected static int SEG_QUADTO;
  protected static int SEG_CUBICTO;
  protected static int SEG_CLOSE;


  protected abstract FontOutline createFontOutline(char ch, Object font);


  protected interface FontOutline {
    public boolean isDone();
    public int currentSegment(float coords[]);
    public void next();
  }


  //////////////////////////////////////////////////////////////////////////////
  //
  // OpenGL ES 2.0 API, with a few additional functions for multisampling and
  // and buffer mapping from OpenGL 2.1+.
  //
  // The functions are organized following the groups in the GLES 2.0 reference
  // card:
  // http://www.khronos.org/opengles/sdk/docs/reference_cards/OpenGL-ES-2_0-Reference-card.pdf
  //
  // The entire GLES 2.0 specification is available below:
  // http://www.khronos.org/opengles/2_X/
  //
  // Implementations of the PGL functions for specific OpenGL bindings (JOGL,
  // LWJGL) should simply call the corresponding GL function in the bindings.
  // readPixels(), activeTexture() and bindTexture() are special cases, please
  // read their comments.
  // Also, keep in mind the note about the PGL constants below.
  //
  //////////////////////////////////////////////////////////////////////////////

  ///////////////////////////////////////////////////////////

  // Constants
  // Very important note: set the GL constants in your PGL subclass by using an
  // static initialization block as follows:
  // static {
  //    FALSE = SUPER_DUPER_JAVA_OPENGL_BINDINGS.GL_FALSE;
  //    TRUE  = SUPER_DUPER_JAVA_OPENGL_BINDINGS.GL_TRUE;
  //    ...
  // }
  // and not by re-declaring the constants, because doing so will lead to
  // errors when the constants are accessed through PGL because they are not
  // overridden but hidden by the new declarations, and hence they keep their
  // initial values (all zeroes) when accessed through the superclass.

  public static int FALSE;
  public static int TRUE;

  public static int INT;
  public static int BYTE;
  public static int SHORT;
  public static int FLOAT;
  public static int BOOL;
  public static int UNSIGNED_INT;
  public static int UNSIGNED_BYTE;
  public static int UNSIGNED_SHORT;

  public static int RGB;
  public static int RGBA;
  public static int ALPHA;
  public static int LUMINANCE;
  public static int LUMINANCE_ALPHA;

  public static int UNSIGNED_SHORT_5_6_5;
  public static int UNSIGNED_SHORT_4_4_4_4;
  public static int UNSIGNED_SHORT_5_5_5_1;

  public static int RGBA4;
  public static int RGB5_A1;
  public static int RGB565;
  public static int RGB8;
  public static int RGBA8;
  public static int ALPHA8;

  public static int READ_ONLY;
  public static int WRITE_ONLY;
  public static int READ_WRITE;

  public static int TESS_WINDING_NONZERO;
  public static int TESS_WINDING_ODD;

  public static int GENERATE_MIPMAP_HINT;
  public static int FASTEST;
  public static int NICEST;
  public static int DONT_CARE;

  public static int VENDOR;
  public static int RENDERER;
  public static int VERSION;
  public static int EXTENSIONS;
  public static int SHADING_LANGUAGE_VERSION;

  public static int MAX_SAMPLES;
  public static int SAMPLES;

  public static int ALIASED_LINE_WIDTH_RANGE;
  public static int ALIASED_POINT_SIZE_RANGE;

  public static int DEPTH_BITS;
  public static int STENCIL_BITS;

  public static int CCW;
  public static int CW;

  public static int VIEWPORT;

  public static int ARRAY_BUFFER;
  public static int ELEMENT_ARRAY_BUFFER;
  public static int PIXEL_PACK_BUFFER;

  public static int MAX_VERTEX_ATTRIBS;

  public static int STATIC_DRAW;
  public static int DYNAMIC_DRAW;
  public static int STREAM_DRAW;
  public static int STREAM_READ;

  public static int BUFFER_SIZE;
  public static int BUFFER_USAGE;

  public static int POINTS;
  public static int LINE_STRIP;
  public static int LINE_LOOP;
  public static int LINES;
  public static int TRIANGLE_FAN;
  public static int TRIANGLE_STRIP;
  public static int TRIANGLES;

  public static int CULL_FACE;
  public static int FRONT;
  public static int BACK;
  public static int FRONT_AND_BACK;

  public static int POLYGON_OFFSET_FILL;

  public static int UNPACK_ALIGNMENT;
  public static int PACK_ALIGNMENT;

  public static int TEXTURE_2D;
  public static int TEXTURE_RECTANGLE;

  public static int TEXTURE_BINDING_2D;
  public static int TEXTURE_BINDING_RECTANGLE;

  public static int MAX_TEXTURE_SIZE;
  public static int TEXTURE_MAX_ANISOTROPY;
  public static int MAX_TEXTURE_MAX_ANISOTROPY;

  public static int MAX_VERTEX_TEXTURE_IMAGE_UNITS;
  public static int MAX_TEXTURE_IMAGE_UNITS;
  public static int MAX_COMBINED_TEXTURE_IMAGE_UNITS;

  public static int NUM_COMPRESSED_TEXTURE_FORMATS;
  public static int COMPRESSED_TEXTURE_FORMATS;

  public static int NEAREST;
  public static int LINEAR;
  public static int LINEAR_MIPMAP_NEAREST;
  public static int LINEAR_MIPMAP_LINEAR;

  public static int CLAMP_TO_EDGE;
  public static int REPEAT;

  public static int TEXTURE0;
  public static int TEXTURE1;
  public static int TEXTURE2;
  public static int TEXTURE3;
  public static int TEXTURE_MIN_FILTER;
  public static int TEXTURE_MAG_FILTER;
  public static int TEXTURE_WRAP_S;
  public static int TEXTURE_WRAP_T;
  public static int TEXTURE_WRAP_R;

  public static int TEXTURE_CUBE_MAP;
  public static int TEXTURE_CUBE_MAP_POSITIVE_X;
  public static int TEXTURE_CUBE_MAP_POSITIVE_Y;
  public static int TEXTURE_CUBE_MAP_POSITIVE_Z;
  public static int TEXTURE_CUBE_MAP_NEGATIVE_X;
  public static int TEXTURE_CUBE_MAP_NEGATIVE_Y;
  public static int TEXTURE_CUBE_MAP_NEGATIVE_Z;

  public static int VERTEX_SHADER;
  public static int FRAGMENT_SHADER;
  public static int INFO_LOG_LENGTH;
  public static int SHADER_SOURCE_LENGTH;
  public static int COMPILE_STATUS;
  public static int LINK_STATUS;
  public static int VALIDATE_STATUS;
  public static int SHADER_TYPE;
  public static int DELETE_STATUS;

  public static int FLOAT_VEC2;
  public static int FLOAT_VEC3;
  public static int FLOAT_VEC4;
  public static int FLOAT_MAT2;
  public static int FLOAT_MAT3;
  public static int FLOAT_MAT4;
  public static int INT_VEC2;
  public static int INT_VEC3;
  public static int INT_VEC4;
  public static int BOOL_VEC2;
  public static int BOOL_VEC3;
  public static int BOOL_VEC4;
  public static int SAMPLER_2D;
  public static int SAMPLER_CUBE;

  public static int LOW_FLOAT;
  public static int MEDIUM_FLOAT;
  public static int HIGH_FLOAT;
  public static int LOW_INT;
  public static int MEDIUM_INT;
  public static int HIGH_INT;

  public static int CURRENT_VERTEX_ATTRIB;

  public static int VERTEX_ATTRIB_ARRAY_BUFFER_BINDING;
  public static int VERTEX_ATTRIB_ARRAY_ENABLED;
  public static int VERTEX_ATTRIB_ARRAY_SIZE;
  public static int VERTEX_ATTRIB_ARRAY_STRIDE;
  public static int VERTEX_ATTRIB_ARRAY_TYPE;
  public static int VERTEX_ATTRIB_ARRAY_NORMALIZED;
  public static int VERTEX_ATTRIB_ARRAY_POINTER;

  public static int BLEND;
  public static int ONE;
  public static int ZERO;
  public static int SRC_ALPHA;
  public static int DST_ALPHA;
  public static int ONE_MINUS_SRC_ALPHA;
  public static int ONE_MINUS_DST_COLOR;
  public static int ONE_MINUS_SRC_COLOR;
  public static int DST_COLOR;
  public static int SRC_COLOR;

  public static int SAMPLE_ALPHA_TO_COVERAGE;
  public static int SAMPLE_COVERAGE;

  public static int KEEP;
  public static int REPLACE;
  public static int INCR;
  public static int DECR;
  public static int INVERT;
  public static int INCR_WRAP;
  public static int DECR_WRAP;
  public static int NEVER;
  public static int ALWAYS;

  public static int EQUAL;
  public static int LESS;
  public static int LEQUAL;
  public static int GREATER;
  public static int GEQUAL;
  public static int NOTEQUAL;

  public static int FUNC_ADD;
  public static int FUNC_MIN;
  public static int FUNC_MAX;
  public static int FUNC_REVERSE_SUBTRACT;
  public static int FUNC_SUBTRACT;

  public static int DITHER;

  public static int CONSTANT_COLOR;
  public static int CONSTANT_ALPHA;
  public static int ONE_MINUS_CONSTANT_COLOR;
  public static int ONE_MINUS_CONSTANT_ALPHA;
  public static int SRC_ALPHA_SATURATE;

  public static int SCISSOR_TEST;
  public static int STENCIL_TEST;
  public static int DEPTH_TEST;
  public static int DEPTH_WRITEMASK;

  public static int COLOR_BUFFER_BIT;
  public static int DEPTH_BUFFER_BIT;
  public static int STENCIL_BUFFER_BIT;

  public static int FRAMEBUFFER;
  public static int COLOR_ATTACHMENT0;
  public static int COLOR_ATTACHMENT1;
  public static int COLOR_ATTACHMENT2;
  public static int COLOR_ATTACHMENT3;
  public static int RENDERBUFFER;
  public static int DEPTH_ATTACHMENT;
  public static int STENCIL_ATTACHMENT;
  public static int READ_FRAMEBUFFER;
  public static int DRAW_FRAMEBUFFER;

  public static int DEPTH24_STENCIL8;

  public static int DEPTH_COMPONENT;
  public static int DEPTH_COMPONENT16;
  public static int DEPTH_COMPONENT24;
  public static int DEPTH_COMPONENT32;

  public static int STENCIL_INDEX;
  public static int STENCIL_INDEX1;
  public static int STENCIL_INDEX4;
  public static int STENCIL_INDEX8;

  public static int DEPTH_STENCIL;

  public static int FRAMEBUFFER_COMPLETE;
  public static int FRAMEBUFFER_INCOMPLETE_ATTACHMENT;
  public static int FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT;
  public static int FRAMEBUFFER_INCOMPLETE_DIMENSIONS;
  public static int FRAMEBUFFER_INCOMPLETE_FORMATS;
  public static int FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER;
  public static int FRAMEBUFFER_INCOMPLETE_READ_BUFFER;
  public static int FRAMEBUFFER_UNSUPPORTED;

  public static int FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE;
  public static int FRAMEBUFFER_ATTACHMENT_OBJECT_NAME;
  public static int FRAMEBUFFER_ATTACHMENT_TEXTURE_LEVEL;
  public static int FRAMEBUFFER_ATTACHMENT_TEXTURE_CUBE_MAP_FACE;

  public static int RENDERBUFFER_WIDTH;
  public static int RENDERBUFFER_HEIGHT;
  public static int RENDERBUFFER_RED_SIZE;
  public static int RENDERBUFFER_GREEN_SIZE;
  public static int RENDERBUFFER_BLUE_SIZE;
  public static int RENDERBUFFER_ALPHA_SIZE;
  public static int RENDERBUFFER_DEPTH_SIZE;
  public static int RENDERBUFFER_STENCIL_SIZE;
  public static int RENDERBUFFER_INTERNAL_FORMAT;

  public static int MULTISAMPLE;
  public static int LINE_SMOOTH;
  public static int POLYGON_SMOOTH;

  public static int SYNC_GPU_COMMANDS_COMPLETE;
  public static int ALREADY_SIGNALED;
  public static int CONDITION_SATISFIED;

  ///////////////////////////////////////////////////////////

  // Special Functions

  public abstract void flush();
  public abstract void finish();
  public abstract void hint(int target, int hint);

  ///////////////////////////////////////////////////////////

  // State and State Requests

  public abstract void enable(int value);
  public abstract void disable(int value);
  public abstract void getBooleanv(int value, IntBuffer data);
  public abstract void getIntegerv(int value, IntBuffer data);
  public abstract void getFloatv(int value, FloatBuffer data);
  public abstract boolean isEnabled(int value);
  public abstract String getString(int name);

  ///////////////////////////////////////////////////////////

  // Error Handling

  public abstract int getError();
  public abstract String errorString(int err);

  //////////////////////////////////////////////////////////////////////////////

  // Buffer Objects

  public abstract void genBuffers(int n, IntBuffer buffers);
  public abstract void deleteBuffers(int n, IntBuffer buffers);
  public abstract void bindBuffer(int target, int buffer);
  public abstract void bufferData(int target, int size, Buffer data, int usage);
  public abstract void bufferSubData(int target, int offset, int size, Buffer data);
  public abstract void isBuffer(int buffer);
  public abstract void getBufferParameteriv(int target, int value, IntBuffer data);
  public abstract ByteBuffer mapBuffer(int target, int access);
  public abstract ByteBuffer mapBufferRange(int target, int offset, int length, int access);
  public abstract void unmapBuffer(int target);

  //////////////////////////////////////////////////////////////////////////////

  // Synchronization

  public abstract long fenceSync(int condition, int flags);
  public abstract void deleteSync(long sync);
  public abstract int clientWaitSync(long sync, int flags, long timeout);

  //////////////////////////////////////////////////////////////////////////////

  // Viewport and Clipping

  public abstract void depthRangef(float n, float f);
  public abstract void viewport(int x, int y, int w, int h);
  protected abstract void viewportImpl(int x, int y, int w, int h);

  //////////////////////////////////////////////////////////////////////////////

  // Reading Pixels
  // This is a special case: because the renderer might be using an FBO even on
  // the main surface, some extra handling might be needed before and after
  // reading the pixels. To make this transparent to the user, the actual call
  // to glReadPixels() should be done in readPixelsImpl().

  public void readPixels(int x, int y, int width, int height, int format, int type, Buffer buffer){
    boolean multisampled = isMultisampled() || graphics.offscreenMultisample;
    boolean depthReadingEnabled = graphics.getHint(PConstants.ENABLE_BUFFER_READING);
    boolean depthRequested = format == STENCIL_INDEX || format == DEPTH_COMPONENT || format == DEPTH_STENCIL;

    if (multisampled && depthRequested && !depthReadingEnabled) {
      PGraphics.showWarning(DEPTH_READING_NOT_ENABLED_ERROR);
      return;
    }

    graphics.beginReadPixels();
    readPixelsImpl(x, y, width, height, format, type, buffer);
    graphics.endReadPixels();
  }

  public void readPixels(int x, int y, int width, int height, int format, int type, long offset){
    boolean multisampled = isMultisampled() || graphics.offscreenMultisample;
    boolean depthReadingEnabled = graphics.getHint(PConstants.ENABLE_BUFFER_READING);
    boolean depthRequested = format == STENCIL_INDEX || format == DEPTH_COMPONENT || format == DEPTH_STENCIL;

    if (multisampled && depthRequested && !depthReadingEnabled) {
      PGraphics.showWarning(DEPTH_READING_NOT_ENABLED_ERROR);
      return;
    }

    graphics.beginReadPixels();
    readPixelsImpl(x, y, width, height, format, type, offset);
    graphics.endReadPixels();
  }

  protected abstract void readPixelsImpl(int x, int y, int width, int height, int format, int type, Buffer buffer);
  protected abstract void readPixelsImpl(int x, int y, int width, int height, int format, int type, long offset);

  //////////////////////////////////////////////////////////////////////////////

  // Vertices

  public abstract void vertexAttrib1f(int index, float value);
  public abstract void vertexAttrib2f(int index, float value0, float value1);
  public abstract void vertexAttrib3f(int index, float value0, float value1, float value2);
  public abstract void vertexAttrib4f(int index, float value0, float value1, float value2, float value3);
  public abstract void vertexAttrib1fv(int index, FloatBuffer values);
  public abstract void vertexAttrib2fv(int index, FloatBuffer values);
  public abstract void vertexAttrib3fv(int index, FloatBuffer values);
  public abstract void vertexAttrib4fv(int index, FloatBuffer values);
  public abstract void vertexAttribPointer(int index, int size, int type, boolean normalized, int stride, int offset);
  public abstract void enableVertexAttribArray(int index);
  public abstract void disableVertexAttribArray(int index);

  public void drawArrays(int mode, int first, int count) {
    geomCount += count;
    drawArraysImpl(mode, first, count);
  }

  public abstract void drawArraysImpl(int mode, int first, int count);

  public void drawElements(int mode, int count, int type, int offset) {
    geomCount += count;
    drawElementsImpl(mode, count, type, offset);
  }

  public abstract void drawElementsImpl(int mode, int count, int type, int offset);

  //////////////////////////////////////////////////////////////////////////////

  // Rasterization

  public abstract void lineWidth(float width);
  public abstract void frontFace(int dir);
  public abstract void cullFace(int mode);
  public abstract void polygonOffset(float factor, float units);

  //////////////////////////////////////////////////////////////////////////////

  // Pixel Rectangles

  public abstract void pixelStorei(int pname, int param);

  ///////////////////////////////////////////////////////////

  // Texturing

  public abstract void texImage2D(int target, int level, int internalFormat, int width, int height, int border, int format, int type, Buffer data);
  public abstract void copyTexImage2D(int target, int level, int internalFormat, int x, int y, int width, int height, int border);
  public abstract void texSubImage2D(int target, int level, int xOffset, int yOffset, int width, int height, int format, int type, Buffer data);
  public abstract void copyTexSubImage2D(int target, int level, int xOffset, int yOffset, int x, int y, int width, int height);
  public abstract void compressedTexImage2D(int target, int level, int internalFormat, int width, int height, int border, int imageSize, Buffer data);
  public abstract void compressedTexSubImage2D(int target, int level, int xOffset, int yOffset, int width, int height, int format, int imageSize, Buffer data);
  public abstract void texParameteri(int target, int pname, int param);
  public abstract void texParameterf(int target, int pname, float param);
  public abstract void texParameteriv(int target, int pname, IntBuffer params);
  public abstract void texParameterfv(int target, int pname, FloatBuffer params);
  public abstract void generateMipmap(int target);
  public abstract void genTextures(int n, IntBuffer textures);
  public abstract void deleteTextures(int n, IntBuffer textures);
  public abstract void getTexParameteriv(int target, int pname, IntBuffer params);
  public abstract void getTexParameterfv(int target, int pname, FloatBuffer params);
  public abstract boolean isTexture(int texture);

  // activeTexture() and bindTexture() have some extra logic to keep track of
  // the bound textures, so the actual GL call should go in activeTextureImpl()
  // and bindTextureImpl().
  public void activeTexture(int texture) {
    activeTexUnit = texture - TEXTURE0;
    activeTextureImpl(texture);
  }

  protected abstract void activeTextureImpl(int texture);

  public void bindTexture(int target, int texture) {
    bindTextureImpl(target, texture);

    if (boundTextures == null) {
      maxTexUnits = getMaxTexUnits();
      boundTextures = new int[maxTexUnits][2];
    }

    if (maxTexUnits <= activeTexUnit) {
      throw new RuntimeException(TEXUNIT_ERROR);
    }

    if (target == TEXTURE_2D) {
      boundTextures[activeTexUnit][0] = texture;
    } else if (target == TEXTURE_RECTANGLE) {
      boundTextures[activeTexUnit][1] = texture;
    }
  }
  protected abstract void bindTextureImpl(int target, int texture);

  ///////////////////////////////////////////////////////////

  // Shaders and Programs

  public abstract int createShader(int type);
  public abstract void shaderSource(int shader, String source);
  public abstract void compileShader(int shader);
  public abstract void releaseShaderCompiler();
  public abstract void deleteShader(int shader);
  public abstract void shaderBinary(int count, IntBuffer shaders, int binaryFormat, Buffer binary, int length);
  public abstract int createProgram();
  public abstract void attachShader(int program, int shader);
  public abstract void detachShader(int program, int shader);
  public abstract void linkProgram(int program);
  public abstract void useProgram(int program);
  public abstract void deleteProgram(int program);
  public abstract String getActiveAttrib(int program, int index, IntBuffer size, IntBuffer type);
  public abstract int getAttribLocation(int program, String name);
  public abstract void bindAttribLocation(int program, int index, String name);
  public abstract int getUniformLocation(int program, String name);
  public abstract String getActiveUniform(int program, int index, IntBuffer size, IntBuffer type);
  public abstract void uniform1i(int location, int value);
  public abstract void uniform2i(int location, int value0, int value1);
  public abstract void uniform3i(int location, int value0, int value1, int value2);
  public abstract void uniform4i(int location, int value0, int value1, int value2, int value3);
  public abstract void uniform1f(int location, float value);
  public abstract void uniform2f(int location, float value0, float value1);
  public abstract void uniform3f(int location, float value0, float value1, float value2);
  public abstract void uniform4f(int location, float value0, float value1, float value2, float value3);
  public abstract void uniform1iv(int location, int count, IntBuffer v);
  public abstract void uniform2iv(int location, int count, IntBuffer v);
  public abstract void uniform3iv(int location, int count, IntBuffer v);
  public abstract void uniform4iv(int location, int count, IntBuffer v);
  public abstract void uniform1fv(int location, int count, FloatBuffer v);
  public abstract void uniform2fv(int location, int count, FloatBuffer v);
  public abstract void uniform3fv(int location, int count, FloatBuffer v);
  public abstract void uniform4fv(int location, int count, FloatBuffer v);
  public abstract void uniformMatrix2fv(int location, int count, boolean transpose, FloatBuffer mat);
  public abstract void uniformMatrix3fv(int location, int count, boolean transpose, FloatBuffer mat);
  public abstract void uniformMatrix4fv(int location, int count, boolean transpose, FloatBuffer mat);
  public abstract void validateProgram(int program);
  public abstract boolean isShader(int shader);
  public abstract void getShaderiv(int shader, int pname, IntBuffer params);
  public abstract void getAttachedShaders(int program, int maxCount, IntBuffer count, IntBuffer shaders);
  public abstract String getShaderInfoLog(int shader);
  public abstract String getShaderSource(int shader);
  public abstract void getShaderPrecisionFormat(int shaderType, int precisionType, IntBuffer range, IntBuffer precision);
  public abstract void getVertexAttribfv(int index, int pname, FloatBuffer params);
  public abstract void getVertexAttribiv(int index, int pname, IntBuffer params);
  public abstract void getVertexAttribPointerv(int index, int pname, ByteBuffer data);
  public abstract void getUniformfv(int program, int location, FloatBuffer params);
  public abstract void getUniformiv(int program, int location, IntBuffer params);
  public abstract boolean isProgram(int program);
  public abstract void getProgramiv(int program, int pname, IntBuffer params);
  public abstract String getProgramInfoLog(int program);

  ///////////////////////////////////////////////////////////

  // Per-Fragment Operations

  public abstract void scissor(int x, int y, int w, int h);
  public abstract void sampleCoverage(float value, boolean invert);
  public abstract void stencilFunc(int func, int ref, int mask);
  public abstract void stencilFuncSeparate(int face, int func, int ref, int mask);
  public abstract void stencilOp(int sfail, int dpfail, int dppass);
  public abstract void stencilOpSeparate(int face, int sfail, int dpfail, int dppass);
  public abstract void depthFunc(int func);
  public abstract void blendEquation(int mode);
  public abstract void blendEquationSeparate(int modeRGB, int modeAlpha);
  public abstract void blendFunc(int src, int dst);
  public abstract void blendFuncSeparate(int srcRGB, int dstRGB, int srcAlpha, int dstAlpha);
  public abstract void blendColor(float red, float green, float blue, float alpha);

  ///////////////////////////////////////////////////////////

  // Whole Framebuffer Operations

  public abstract void colorMask(boolean r, boolean g, boolean b, boolean a);
  public abstract void depthMask(boolean mask);
  public abstract void stencilMask(int mask);
  public abstract void stencilMaskSeparate(int face, int mask);
  public abstract void clearColor(float r, float g, float b, float a);
  public abstract void clearDepth(float d);
  public abstract void clearStencil(int s);
  public abstract void clear(int buf);

  ///////////////////////////////////////////////////////////

  // Framebuffers Objects

  public void bindFramebuffer(int target, int framebuffer) {
    graphics.beginBindFramebuffer(target, framebuffer);
    bindFramebufferImpl(target, framebuffer);
    graphics.endBindFramebuffer(target, framebuffer);
  }
  protected abstract void bindFramebufferImpl(int target, int framebuffer);

  public abstract void deleteFramebuffers(int n, IntBuffer framebuffers);
  public abstract void genFramebuffers(int n, IntBuffer framebuffers);
  public abstract void bindRenderbuffer(int target, int renderbuffer);
  public abstract void deleteRenderbuffers(int n, IntBuffer renderbuffers);
  public abstract void genRenderbuffers(int n, IntBuffer renderbuffers);
  public abstract void renderbufferStorage(int target, int internalFormat, int width, int height);
  public abstract void framebufferRenderbuffer(int target, int attachment, int rendbuferfTarget, int renderbuffer);
  public abstract void framebufferTexture2D(int target, int attachment, int texTarget, int texture, int level);
  public abstract int checkFramebufferStatus(int target);
  public abstract boolean isFramebuffer(int framebuffer);
  public abstract void getFramebufferAttachmentParameteriv(int target, int attachment, int pname, IntBuffer params);
  public abstract boolean isRenderbuffer(int renderbuffer);
  public abstract void getRenderbufferParameteriv(int target, int pname, IntBuffer params);
  public abstract void blitFramebuffer(int srcX0, int srcY0, int srcX1, int srcY1, int dstX0, int dstY0, int dstX1, int dstY1, int mask, int filter);
  public abstract void renderbufferStorageMultisample(int target, int samples, int format, int width, int height);
  public abstract void readBuffer(int buf);
  public abstract void drawBuffer(int buf);
}
