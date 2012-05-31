/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2011 Andres Colubri
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

package processing.opengl;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
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
  /** Size of a short (in bytes). */
  static final int SIZEOF_SHORT = Short.SIZE / 8; 
  
  /** Size of an int (in bytes). */
  static final int SIZEOF_INT = Integer.SIZE / 8;
   
  /** Size of a float (in bytes). */
  static final int SIZEOF_FLOAT = Float.SIZE / 8;
  
  /** Size of a byte (in bytes). */
  static final int SIZEOF_BYTE = Byte.SIZE / 8;  
  
  /** Size of a vertex index. */
  static final int SIZEOF_INDEX = SIZEOF_SHORT; 
  
  /** Type of a vertex index. */
  static final int INDEX_TYPE = GLES20.GL_UNSIGNED_SHORT;

  /** Initial sizes for arrays of input and tessellated data. */
  public static final int DEFAULT_IN_VERTICES = 16;
  public static final int DEFAULT_IN_EDGES = 32;
  public static final int DEFAULT_IN_TEXTURES = 16;
  public static final int DEFAULT_TESS_VERTICES = 16;
  public static final int DEFAULT_TESS_INDICES = 32;  
  
  /** Initial sizes for vertex cache used in PShape3D. */
  public static final int DEFAULT_VERTEX_CACHE_SIZE = 256;
  
  /** Maximum lights by default is 8, the minimum defined by OpenGL. */   
  public static final int MAX_LIGHTS = 8;
  
  /** Maximum index value of a tessellated vertex. GLES restricts the vertex 
   * indices to be of type unsigned short. Since Java only supports signed
   * shorts as primitive type we have 2^15 = 32768 as the maximum number of  
   * vertices that can be referred to within a single VBO. */
  public static final int MAX_VERTEX_INDEX = 32767;
  public static final int MAX_VERTEX_INDEX1 = MAX_VERTEX_INDEX + 1;
  
  /** Count of tessellated fill, line or point vertices that will 
   * trigger a flush in the immediate mode. It doesn't necessarily 
   * be equal to MAX_VERTEX_INDEX1, since the number of vertices can 
   * be effectively much large since the renderer uses offsets to
   * refer to vertices beyond the MAX_VERTEX_INDEX limit. 
   */
  public static final int FLUSH_VERTEX_COUNT = MAX_VERTEX_INDEX1; 

  /** Maximum dimension of a texture used to hold font data. **/
  public static final int MAX_FONT_TEX_SIZE = 256;
  
  /** Minimum stroke weight needed to apply the full path stroking
   * algorithm that properly generates caps and joing. 
   */
  public static final float MIN_CAPS_JOINS_WEIGHT = 2.f; 
  
  /** Maximum length of linear paths to be stroked with the 
   * full algorithm that generates accurate caps and joins. 
   */  
  public static final int MAX_CAPS_JOINS_LENGTH = 1000; 
  
  /** Minimum array size to use arrayCopy method(). **/
  static protected final int MIN_ARRAYCOPY_SIZE = 2;    
  
  /** Enables/disables mipmap use. **/
  static protected final boolean MIPMAPS_ENABLED = false;    
  
  /** Machine Epsilon for float precision. **/
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
  
  protected static final String SHADER_PREPROCESSOR_DIRECTIVE = "#ifdef GL_ES\n" +
                                                                "precision mediump float;\n" +
                                                                "precision mediump int;\n" +
                                                                "#endif\n";  
  
  ///////////////////////////////////////////////////////////////////////////////////
  
  // OpenGL constants
  
  // The values for constants not defined in the GLES20 interface can be found in this file:
  // http://androidxref.com/source/raw/development/tools/glesv2debugger/src/com/android/glesv2debugger/GLEnum.java
  
  public static final int GL_FALSE = GLES20.GL_FALSE;
  public static final int GL_TRUE  = GLES20.GL_TRUE;  
  
  public static final int GL_LESS   = GLES20.GL_LESS;
  public static final int GL_LEQUAL = GLES20.GL_LEQUAL;
  
  public static final int GL_CCW    = GLES20.GL_CCW;
  public static final int GL_CW     = GLES20.GL_CW;
  
  public static final int GL_CULL_FACE      = GLES20.GL_CULL_FACE;
  public static final int GL_FRONT          = GLES20.GL_FRONT;
  public static final int GL_BACK           = GLES20.GL_BACK;
  public static final int GL_FRONT_AND_BACK = GLES20.GL_FRONT_AND_BACK;
  
  public static final int GL_VIEWPORT = GLES20.GL_VIEWPORT;
  
  public static final int GL_SCISSOR_TEST    = GLES20.GL_SCISSOR_TEST;  
  public static final int GL_DEPTH_TEST      = GLES20.GL_DEPTH_TEST;
  public static final int GL_DEPTH_WRITEMASK = GLES20.GL_DEPTH_WRITEMASK;
  
  public static final int GL_COLOR_BUFFER_BIT   = GLES20.GL_COLOR_BUFFER_BIT; 
  public static final int GL_DEPTH_BUFFER_BIT   = GLES20.GL_DEPTH_BUFFER_BIT; 
  public static final int GL_STENCIL_BUFFER_BIT = GLES20.GL_STENCIL_BUFFER_BIT;
  
  public static final int GL_FUNC_ADD              = GLES20.GL_FUNC_ADD;
  public static final int GL_FUNC_MIN              = 0x8007;
  public static final int GL_FUNC_MAX              = 0x8008;
  public static final int GL_FUNC_REVERSE_SUBTRACT = GLES20.GL_FUNC_REVERSE_SUBTRACT;
  
  public static final int GL_TEXTURE_2D     = GLES20.GL_TEXTURE_2D;
  public static final int GL_RGB            = GLES20.GL_RGB;
  public static final int GL_RGBA           = GLES20.GL_RGBA;
  public static final int GL_ALPHA          = GLES20.GL_ALPHA;
  public static final int GL_UNSIGNED_INT   = GLES20.GL_UNSIGNED_INT;
  public static final int GL_UNSIGNED_BYTE  = GLES20.GL_UNSIGNED_BYTE;
  public static final int GL_UNSIGNED_SHORT = GLES20.GL_UNSIGNED_SHORT;
  public static final int GL_FLOAT          = GLES20.GL_FLOAT;
  
  public static final int GL_NEAREST               = GLES20.GL_NEAREST;
  public static final int GL_LINEAR                = GLES20.GL_LINEAR;
  public static final int GL_LINEAR_MIPMAP_NEAREST = GLES20.GL_LINEAR_MIPMAP_NEAREST;
  public static final int GL_LINEAR_MIPMAP_LINEAR  = GLES20.GL_LINEAR_MIPMAP_LINEAR; 
  
  public static final int GL_CLAMP_TO_EDGE = GLES20.GL_CLAMP_TO_EDGE;
  public static final int GL_REPEAT        = GLES20.GL_REPEAT;
  
  public static final int GL_RGBA8            = -1;  
  public static final int GL_DEPTH24_STENCIL8 = 0x88F0;
  
  public static final int GL_DEPTH_COMPONENT   = GLES20.GL_DEPTH_COMPONENT;
  public static final int GL_DEPTH_COMPONENT16 = GLES20.GL_DEPTH_COMPONENT16;
  public static final int GL_DEPTH_COMPONENT24 = 0x81A6;
  public static final int GL_DEPTH_COMPONENT32 = 0x81A7;    
  
  public static final int GL_STENCIL_INDEX  = GLES20.GL_STENCIL_INDEX;
  public static final int GL_STENCIL_INDEX1 = 0x8D46;
  public static final int GL_STENCIL_INDEX4 = 0x8D47; 
  public static final int GL_STENCIL_INDEX8 = GLES20.GL_STENCIL_INDEX8;   
  
  public static final int GL_ARRAY_BUFFER         = GLES20.GL_ARRAY_BUFFER;
  public static final int GL_ELEMENT_ARRAY_BUFFER = GLES20.GL_ELEMENT_ARRAY_BUFFER;
    
  public static final int GL_SAMPLES = GLES20.GL_SAMPLES;  
  
  public static final int GL_FRAMEBUFFER_COMPLETE                      = GLES20.GL_FRAMEBUFFER_COMPLETE;    
  public static final int GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT         = GLES20.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT;
  public static final int GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT = GLES20.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT;
  public static final int GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS         = GLES20.GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS;      
  public static final int GL_FRAMEBUFFER_INCOMPLETE_FORMATS            = 0x8CDA;  
  public static final int GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER        = -1;
  public static final int GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER        = -1;  
  public static final int GL_FRAMEBUFFER_UNSUPPORTED                   = GLES20.GL_FRAMEBUFFER_UNSUPPORTED;
    
  public static final int GL_STATIC_DRAW  = GLES20.GL_STATIC_DRAW;
  public static final int GL_DYNAMIC_DRAW = GLES20.GL_DYNAMIC_DRAW;
  public static final int GL_STREAM_DRAW  = GLES20.GL_STREAM_DRAW;
  
  public static final int GL_READ_ONLY  = -1;
  public static final int GL_WRITE_ONLY = -1;  
  public static final int GL_READ_WRITE = -1;
    
  public static final int GL_TRIANGLE_FAN   = GLES20.GL_TRIANGLE_FAN;
  public static final int GL_TRIANGLE_STRIP = GLES20.GL_TRIANGLE_STRIP;
  public static final int GL_TRIANGLES      = GLES20.GL_TRIANGLES;  
  
  public static final int GL_VENDOR                   = GLES20.GL_VENDOR;
  public static final int GL_RENDERER                 = GLES20.GL_RENDERER;
  public static final int GL_VERSION                  = GLES20.GL_VERSION;
  public static final int GL_EXTENSIONS               = GLES20.GL_EXTENSIONS;
  public static final int GL_SHADING_LANGUAGE_VERSION = GLES20.GL_SHADING_LANGUAGE_VERSION;
    
  public static final int GL_MAX_TEXTURE_SIZE         = GLES20.GL_MAX_TEXTURE_SIZE;
  public static final int GL_MAX_SAMPLES              = -1;
  public static final int GL_ALIASED_LINE_WIDTH_RANGE = GLES20.GL_ALIASED_LINE_WIDTH_RANGE;
  public static final int GL_ALIASED_POINT_SIZE_RANGE = GLES20.GL_ALIASED_POINT_SIZE_RANGE;  
  public static final int GL_DEPTH_BITS               = GLES20.GL_DEPTH_BITS;
  public static final int GL_STENCIL_BITS             = GLES20.GL_STENCIL_BITS;
  
  public static final int GLU_TESS_WINDING_NONZERO = PGLU.GLU_TESS_WINDING_NONZERO;
  public static final int GLU_TESS_WINDING_ODD     = PGLU.GLU_TESS_WINDING_ODD;  
    
  public static final int GL_TEXTURE0           = GLES20.GL_TEXTURE0;
  public static final int GL_TEXTURE1           = GLES20.GL_TEXTURE1;
  public static final int GL_TEXTURE2           = GLES20.GL_TEXTURE2;
  public static final int GL_TEXTURE3           = GLES20.GL_TEXTURE3;
  public static final int GL_TEXTURE_MIN_FILTER = GLES20.GL_TEXTURE_MIN_FILTER;
  public static final int GL_TEXTURE_MAG_FILTER = GLES20.GL_TEXTURE_MAG_FILTER;
  public static final int GL_TEXTURE_WRAP_S     = GLES20.GL_TEXTURE_WRAP_S;
  public static final int GL_TEXTURE_WRAP_T     = GLES20.GL_TEXTURE_WRAP_T;  
  
  public static final int GL_BLEND               = GLES20.GL_BLEND;
  public static final int GL_ONE                 = GLES20.GL_ONE; 
  public static final int GL_ZERO                = GLES20.GL_ZERO;
  public static final int GL_SRC_ALPHA           = GLES20.GL_SRC_ALPHA; 
  public static final int GL_DST_ALPHA           = GLES20.GL_DST_ALPHA;
  public static final int GL_ONE_MINUS_SRC_ALPHA = GLES20.GL_ONE_MINUS_SRC_ALPHA;
  public static final int GL_ONE_MINUS_DST_COLOR = GLES20.GL_ONE_MINUS_DST_COLOR;
  public static final int GL_ONE_MINUS_SRC_COLOR = GLES20.GL_ONE_MINUS_SRC_COLOR;
  public static final int GL_DST_COLOR           = GLES20.GL_DST_COLOR;
  public static final int GL_SRC_COLOR           = GLES20.GL_SRC_COLOR;
  
  public static final int GL_FRAMEBUFFER        = GLES20.GL_FRAMEBUFFER;
  public static final int GL_COLOR_ATTACHMENT0  = GLES20.GL_COLOR_ATTACHMENT0;
  public static final int GL_COLOR_ATTACHMENT1  = -1;
  public static final int GL_COLOR_ATTACHMENT2  = -1;
  public static final int GL_COLOR_ATTACHMENT3  = -1;  
  public static final int GL_RENDERBUFFER       = GLES20.GL_RENDERBUFFER;
  public static final int GL_DEPTH_ATTACHMENT   = GLES20.GL_DEPTH_ATTACHMENT;
  public static final int GL_STENCIL_ATTACHMENT = GLES20.GL_STENCIL_ATTACHMENT;  
  public static final int GL_READ_FRAMEBUFFER   = -1;
  public static final int GL_DRAW_FRAMEBUFFER   = -1;     
  
  public static final int GL_VERTEX_SHADER        = GLES20.GL_VERTEX_SHADER;
  public static final int GL_FRAGMENT_SHADER      = GLES20.GL_FRAGMENT_SHADER;  
  public static final int GL_INFO_LOG_LENGTH      = GLES20.GL_INFO_LOG_LENGTH;
  public static final int GL_SHADER_SOURCE_LENGTH = GLES20.GL_SHADER_SOURCE_LENGTH;
  public static final int GL_COMPILE_STATUS       = GLES20.GL_COMPILE_STATUS;
  public static final int GL_LINK_STATUS          = GLES20.GL_LINK_STATUS;
  public static final int GL_VALIDATE_STATUS      = GLES20.GL_VALIDATE_STATUS;
  
  public static final int GL_MULTISAMPLE    = -1;  
  public static final int GL_POINT_SMOOTH   = -1;      
  public static final int GL_LINE_SMOOTH    = -1;    
  public static final int GL_POLYGON_SMOOTH = -1;  
  
  // Some EGL constants needed to initialize an GLES2 context.
  public static final int EGL_CONTEXT_CLIENT_VERSION = 0x3098;
  public static final int EGL_OPENGL_ES2_BIT         = 0x0004;
  
  /** Basic GLES 1.0 interface */
  public GL10 gl;
  
  /** GLU interface **/
  public PGLU glu; 
  
  /** The PGraphics object using this interface */
  public PGraphicsOpenGL pg;
  
  /** Whether OpenGL has been initialized or not */
  public boolean initialized;

  /** The renderer object driving the rendering loop,
   * analogous to the GLEventListener in JOGL */
  protected AndroidRenderer renderer;

  ///////////////////////////////////////////////////////////////////////////////////
  
  // FBO for incremental drawing  
  
  protected boolean firstOnscreenFrame = true;
  protected int[] textures = { 0, 0 };
  protected int[] fbo = { 0 };
  protected int[] depth = { 0 };
  protected int[] stencil = { 0 };
  protected int texWidth, texHeight;
  protected int backTex, frontTex;
    
  ///////////////////////////////////////////////////////////////////////////////////
  
  // Texture rendering
  
  protected boolean loadedTexShader = false;
  protected int texShaderProgram;
  protected int texVertShader;
  protected int texFragShader;
  
  protected int texVertLoc;
  protected int texTCoordLoc;
  
  protected float[] texCoords = {
    //  X,     Y,    U,    V
    -1.0f, -1.0f, 0.0f, 0.0f,
    +1.0f, -1.0f, 1.0f, 0.0f,    
    -1.0f, +1.0f, 0.0f, 1.0f,
    +1.0f, +1.0f, 1.0f, 1.0f
  }; 
  protected FloatBuffer texData;
  
  protected String texVertShaderSource = "attribute vec2 inVertex;" +
                                         "attribute vec2 inTexcoord;" +
                                         "varying vec2 vertTexcoord;" +
                                         "void main() {" +
                                         "  gl_Position = vec4(inVertex, 0, 1);" +
                                         "  vertTexcoord = inTexcoord;" +    
                                         "}";
  
  protected String texFragShaderSource = SHADER_PREPROCESSOR_DIRECTIVE +
                                         "uniform sampler2D textureSampler;" +
                                         "varying vec2 vertTexcoord;" +
                                         "void main() {" +
                                         "  gl_FragColor = texture2D(textureSampler, vertTexcoord.st);" +                                   
                                         "}";  
  
  ///////////////////////////////////////////////////////////////////////////////////
  
  // Rectangle rendering
  
  protected boolean loadedRectShader = false;
  protected int rectShaderProgram;
  protected int rectVertShader;
  protected int rectFragShader;
  
  protected int rectVertLoc;
  protected int rectColorLoc;
      
  protected float[] rectCoords = {
    //  X,     Y
    -1.0f, -1.0f,
    +1.0f, -1.0f,    
    -1.0f, +1.0f,
    +1.0f, +1.0f,
  }; 
  protected FloatBuffer rectData;  
  
  protected String rectVertShaderSource = "attribute vec2 inVertex;" +
                                          "void main() {" +
                                          "  gl_Position = vec4(inVertex, 0, 1);" +
                                          "}";

  protected String rectFragShaderSource = SHADER_PREPROCESSOR_DIRECTIVE +
                                          "uniform vec4 rectColor;" +
                                          "void main() {" +
                                          "  gl_FragColor = rectColor;" +                                   
                                          "}";
  
  ///////////////////////////////////////////////////////////////////////////////////
  
  // 1-pixel color, depth, stencil buffers
  
  protected IntBuffer colorBuffer;
  protected FloatBuffer depthBuffer;
  protected ByteBuffer stencilBuffer;  
  
  ///////////////////////////////////////////////////////////////////////////////////
  
  // Intialization, finalization
  
  
  public PGL(PGraphicsOpenGL pg) {
    this.pg = pg;
    renderer = new AndroidRenderer();
    glu = new PGLU();
    initialized = false;
  }

  
  public void setFramerate(float framerate) {    
  }
  
  
  public void initPrimarySurface(int antialias) {
    // We do the initialization in updatePrimary() because
    // at the moment initPrimarySurface() gets called we 
    // cannot rely on the GL surface actually being
    // available.
  }
  
  
  public void initOffscreenSurface(PGL primary) {
    initialized = true;
  }  
  
  
  public void updatePrimary() {    
    if (!initialized) {
      String ext = GLES20.glGetString(GLES20.GL_EXTENSIONS); 
      if (-1 < ext.indexOf("texture_non_power_of_two")) {
        texWidth = pg.width;
        texHeight = pg.height;
      } else {
        texWidth = PGL.nextPowerOfTwo(pg.width);
        texHeight = PGL.nextPowerOfTwo(pg.height);
      }
      
      // We create the GL resources we need to draw incrementally, ie: not clearing
      // the screen in each frame. Because the way Android handles double buffering
      // we need to handle our own custom buffering using FBOs.
      GLES20.glGenTextures(2, textures, 0);        
      for (int i = 0; i < 2; i++) {
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[i]);    
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);    
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, texWidth, texHeight, 0, PGL.GL_RGBA, PGL.GL_UNSIGNED_BYTE, null);
        initTexture(GLES20.GL_TEXTURE_2D, PGL.GL_RGBA, texWidth, texHeight);
      }
      GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
      
      GLES20.glGenFramebuffers(1, fbo, 0);      
      GLES20.glGenRenderbuffers(1, depth, 0);       
      
      GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fbo[0]);
            
      GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, depth[0]);
      GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER, GLES20.GL_DEPTH_COMPONENT16, texWidth, texHeight);
      GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_DEPTH_ATTACHMENT, GLES20.GL_RENDERBUFFER, depth[0]);
      
      int[] temp = new int[1];
      GLES20.glGetIntegerv(GLES20.GL_STENCIL_BITS, temp, 0);    
      int stencilBits = temp[0];       
      if (stencilBits == 8) {
        GLES20.glGenRenderbuffers(1, stencil, 0);
        GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, stencil[0]);
        GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER, GLES20.GL_STENCIL_INDEX8, texWidth, texHeight);
        GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_STENCIL_ATTACHMENT, GLES20.GL_RENDERBUFFER, stencil[0]);
      }
      
      GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
      
      backTex = 1; 
      frontTex = 0;
      
      // The screen framebuffer is the FBO just created. We need
      // to update the screenFramebuffer object so when the
      // framebuffer is popped back to the screen, the correct
      // id is set.
      PGraphicsOpenGL.screenFramebuffer.glFboID = fbo[0];
      
      initialized = true;
    }    
  }

  
  public void updateOffscreen(PGL primary) {
    gl = primary.gl;       
  }  
  
  
  public boolean primaryIsDoubleBuffered() {
    return true;
  }
  
  
  ///////////////////////////////////////////////////////////////////////////////////
  
  // Frame rendering  

  
  public void beginOnscreenDraw(boolean clear) {
    if (clear) {
      // Simplest scenario: clear mode means we clear both the color and depth buffers.
      // No need for saving front color buffer, etc.
      GLES20.glClearColor(0, 0, 0, 0);
      GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
      PGraphicsOpenGL.screenFramebuffer.glFboID = 0;
    } else {      
      GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fbo[0]);      
      GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, textures[frontTex], 0);
      validateFramebuffer();      
      
      // We need to save the color buffer after finishing with the rendering of this frame,
      // to use is as the background for the next frame ("incremental drawing"). 
      GLES20.glClearColor(0, 0, 0, 0);
      GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_STENCIL_BUFFER_BIT);
      if (firstOnscreenFrame) {
        // No need to draw back color buffer because we are in the first frame.
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
      } else {
        // Render previous draw texture as background.      
        drawTexture(GLES20.GL_TEXTURE_2D, textures[backTex], texWidth, texHeight, 0, 0, pg.width, pg.height, 0, 0, pg.width, pg.height);
      }
      PGraphicsOpenGL.screenFramebuffer.glFboID = fbo[0];
    }
    
    if (firstOnscreenFrame) {
      firstOnscreenFrame = false;
    }
  }
  
  
  public void endOnscreenDraw(boolean clear0) {
    if (!clear0) {
      // We are in the primary surface, and no clear mode, this means that the current
      // contents of the front buffer needs to be used in the next frame as the background.
      GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0); 
      
      GLES20.glClearColor(0, 0, 0, 0);
      GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);      
      
      // Render current front texture to screen.
      drawTexture(GLES20.GL_TEXTURE_2D, textures[frontTex], texWidth, texHeight, 0, 0, pg.width, pg.height, 0, 0, pg.width, pg.height);
      
      // Swapping front and back textures.
      int temp = frontTex;
      frontTex = backTex;
      backTex = temp;
    }
  }
  
  
  public void beginOffscreenDraw(boolean clear) {
  }
  
  
  public void endOffscreenDraw(boolean clear0) {
  }  
  
  
  public boolean canDraw() {
    return true;    
  }  
  
  
  public void requestDraw() {
    pg.parent.andresNeedsBetterAPI();
  }  
  
  
  ///////////////////////////////////////////////////////////////////////////////////
  
  // Caps query

  
  public String glGetString(int name) {
    return GLES20.glGetString(name);
  }
 
  
  public void glGetIntegerv(int name, int[] values, int offset) {
    if (-1 < name) {
      GLES20.glGetIntegerv(name, values, offset);
    } else {
      Arrays.fill(values, 0);
    }
  }
  
  
  public void glGetBooleanv(int name, boolean[] values, int offset) {
    if (-1 < name) {
      GLES20.glGetBooleanv(name, values, offset);
    } else {
      Arrays.fill(values, false);
    }    
  }
    
  
  ///////////////////////////////////////////////////////////////////////////////////
  
  // Enable/disable caps
  
  
  public void glEnable(int cap) {
    if (-1 < cap) {
      GLES20.glEnable(cap);
    }
  }  
  
  
  public void glDisable(int cap) {
    if (-1 < cap) {
      GLES20.glDisable(cap);
    }
  }  
  
  
  ///////////////////////////////////////////////////////////////////////////////////
  
  // Render control 
  
  
  public void glFlush() {
    GLES20.glFlush();
  }  
  
  
  public void glFinish() {
    GLES20.glFinish();
  }
  
  
  /////////////////////////////////////////////////////////////////////////////////
  
  // Error handling  
  
  
  public int glGetError() {
    return GLES20.glGetError();
  }
  
  
  public String glErrorString(int err) {
    return GLU.gluErrorString(err);
  }

  
  public String gluErrorString(int err) {
    return PGLU.gluErrorString(err);
  }
  
  
  /////////////////////////////////////////////////////////////////////////////////
  
  // Rendering options

  
  public void glFrontFace(int mode) {
    GLES20.glFrontFace(mode);
  }
  
  
  public void glCullFace(int mode) {
    GLES20.glCullFace(mode);  
  }
  
  
  public void glDepthMask(boolean flag) {
    GLES20.glDepthMask(flag);   
  }
  
  
  public void glDepthFunc(int func) {
    GLES20.glDepthFunc(func);
  }  

  
  /////////////////////////////////////////////////////////////////////////////////
  
  // Textures     
  
  
  public void glGenTextures(int n, int[] ids, int offset) {
    GLES20.glGenTextures(n, ids, offset);
  }

  
  public void glDeleteTextures(int n, int[] ids, int offset) {
    GLES20.glDeleteTextures(n, ids, offset);
  }  
  
  
  public void glActiveTexture(int unit) {
    GLES20.glActiveTexture(unit);
  }
  
  
  public void glBindTexture(int target, int id) {
    GLES20.glBindTexture(target, id);
  }
  
  
  public void glTexImage2D(int target, int level, int internalFormat, int width, int height, int border, int format, int type, Buffer data) {
    GLES20.glTexImage2D(target, level, internalFormat, width, height, border, format, type, data);
  }
  
  
  public void glTexSubImage2D(int target, int level, int xOffset, int yOffset, int width, int height, int format, int type, Buffer data) {
    GLES20.glTexSubImage2D(target, level, xOffset, yOffset, width, height, format, type, data);
  }

  
  public void glTexParameterf(int target, int param, int value) {
    GLES20.glTexParameterf(target, param, value); 
  }

  
  public void glGenerateMipmap(int target) {
    GLES20.glGenerateMipmap(target);
  }  
  
  
  /////////////////////////////////////////////////////////////////////////////////
  
  // Vertex Buffers

  
  public void glGenBuffers(int n, int[] ids, int offset) {
    GLES20.glGenBuffers(n, ids, offset);  
  }
  
  
  public void glDeleteBuffers(int n, int[] ids, int offset) {
    GLES20.glDeleteBuffers(n, ids, offset);  
  }
  
  
  public void glBindBuffer(int target, int id) {
    GLES20.glBindBuffer(target, id);
  }
  
  
  public void glBufferData(int target, int size, Buffer data, int usage) {
    GLES20.glBufferData(target, size, data, usage);
  }

  
  public void glBufferSubData(int target, int offset, int size, Buffer data) {
    GLES20.glBufferSubData(target, offset, size, data);
  }
  
  
  public void glDrawArrays(int mode, int first, int count) {
    GLES20.glDrawArrays(mode, first, count);
  }
  
  
  public void glDrawElements(int mode, int count, int type, int offset) {
    GLES20.glDrawElements(mode, count, type, offset);
  }

  
  public void glEnableVertexAttribArray(int loc) {
    GLES20.glEnableVertexAttribArray(loc);
  }
  
  
  public void glDisableVertexAttribArray(int loc) {
    GLES20.glDisableVertexAttribArray(loc);
  }  
  
  
  public void glVertexAttribPointer(int loc, int size, int type, boolean normalized, int stride, int offset) {
    GLES20.glVertexAttribPointer(loc, size, type, normalized, stride, offset);    
  }
  
  
  public void glVertexAttribPointer(int loc, int size, int type, boolean normalized, int stride, Buffer data) {
    GLES20.glVertexAttribPointer(loc, size, type, normalized, stride, data);
  }
  
  
  public ByteBuffer glMapBuffer(int target, int access) {  
    //return gl2f.glMapBuffer(GL.GL_ARRAY_BUFFER, GL2.GL_READ_WRITE);
    return null;
  }
  
  
  public ByteBuffer glMapBufferRange(int target, int offset, int length, int access) {
    //return gl2x.glMapBufferRange(GL.GL_ARRAY_BUFFER, offset, length, GL2.GL_READ_WRITE);
    return null;
  }
  
  
  public void glUnmapBuffer(int target) {
    //gl2f.glUnmapBuffer(GL.GL_ARRAY_BUFFER);
  }
  
  
  /////////////////////////////////////////////////////////////////////////////////
  
  // Framebuffers, renderbuffers  
  
  
  public void glGenFramebuffers(int n, int[] ids, int offset) {
    GLES20.glGenFramebuffers(n, ids, offset);    
  }
  
  
  public void glDeleteFramebuffers(int n, int[] ids, int offset) {
    GLES20.glDeleteFramebuffers(n, ids, offset);    
  }
  
  
  public void glGenRenderbuffers(int n, int[] ids, int offset) {
    GLES20.glGenRenderbuffers(n, ids, offset);    
  }
  
  
  public void glDeleteRenderbuffers(int n, int[] ids, int offset) {
    GLES20.glDeleteRenderbuffers(n, ids, offset);    
  }
  
  
  public void glBindFramebuffer(int target, int id) {
    GLES20.glBindFramebuffer(target, id);
  }
  
  
  public void glBlitFramebuffer(int srcX0, int srcY0, int srcX1, int srcY1, int dstX0, int dstY0, int dstX1, int dstY1, int mask, int filter) {
//    GLES20.glBlitFramebuffer(0, 0, srcW, srcH, 0, 0, destW, destH, GLES20.GL_COLOR_BUFFER_BIT, GLES20.GL_NEAREST);    
  }
  
  
  public void glFramebufferTexture2D(int target, int attachment, int texTarget, int texId, int level) {   
    GLES20.glFramebufferTexture2D(target, attachment, texTarget, texId, level);
  }

  
  public void glBindRenderbuffer(int target, int id) {
    GLES20.glBindRenderbuffer(target, id);
  }
  
  
  public void glRenderbufferStorageMultisample(int target, int samples, int format, int width, int height) {
//    GLES20.glRenderbufferStorageMultisample(GLES20.GL_RENDERBUFFER, samples, format, w, h);
  }
  
  
  public void glRenderbufferStorage(int target, int format, int width, int height) {
    GLES20.glRenderbufferStorage(target, format, width, height);
  }
  
  
  public void glFramebufferRenderbuffer(int target, int attachment, int rendbufTarget, int rendbufId) {
    GLES20.glFramebufferRenderbuffer(target, attachment, rendbufTarget, rendbufId);
  }  

  
  public int glCheckFramebufferStatus(int target) {
    return GLES20.glCheckFramebufferStatus(target);
  }  

  
  /////////////////////////////////////////////////////////////////////////////////
  
  // Shaders  
  
  
  public int glCreateProgram() {
    return GLES20.glCreateProgram();    
  }
  
  
  public void glDeleteProgram(int id) {
    GLES20.glDeleteProgram(id);  
  }
  
  
  public int glCreateShader(int type) {
    return GLES20.glCreateShader(type);    
  }
  
  
  public void glDeleteShader(int id) {
    GLES20.glDeleteShader(id);    
  }
  
  
  public void glLinkProgram(int prog) {
    GLES20.glLinkProgram(prog);  
  }
  
  
  public void glValidateProgram(int prog) {
    GLES20.glValidateProgram(prog);
  }
  
  
  public void glUseProgram(int prog) {
    GLES20.glUseProgram(prog);  
  }
  
  
  public int glGetAttribLocation(int prog, String name) {
    return GLES20.glGetAttribLocation(prog, name);
  }
  
  
  public int glGetUniformLocation(int prog, String name) {
    return GLES20.glGetUniformLocation(prog, name);
  }  
  
  
  public void glUniform1i(int loc, int value) {
    GLES20.glUniform1i(loc, value);  
  }
  
  
  public void glUniform1f(int loc, float value) {
    GLES20.glUniform1f(loc, value);  
  }    
  
  
  public void glUniform2f(int loc, float value0, float value1) {
    GLES20.glUniform2f(loc, value0, value1);  
  }
  
  
  public void glUniform3f(int loc, float value0, float value1, float value2) {
    GLES20.glUniform3f(loc, value0, value1, value2);  
  }
  
  
  public void glUniform4f(int loc, float value0, float value1, float value2, float value3) {
    GLES20.glUniform4f(loc, value0, value1, value2, value3);  
  }
  
  
  public void glUniform1fv(int loc, int count, float[] v, int offset) {
    GLES20.glUniform1fv(loc, count, v, offset);
  }    

  
  public void glUniform2fv(int loc, int count, float[] v, int offset) {
    GLES20.glUniform2fv(loc, count, v, offset);
  }    

  
  public void glUniform3fv(int loc, int count, float[] v, int offset) {
    GLES20.glUniform3fv(loc, count, v, offset);
  }

  
  public void glUniform4fv(int loc, int count, float[] v, int offset) {
    GLES20.glUniform4fv(loc, count, v, offset);
  }  
  
  
  public void glUniformMatrix2fv(int loc, int count, boolean transpose, float[] mat, int offset) {
    GLES20.glUniformMatrix2fv(loc, count, transpose, mat, offset);
  }
  
  
  public void glUniformMatrix3fv(int loc, int count, boolean transpose, float[] mat, int offset) {
    GLES20.glUniformMatrix3fv(loc, count, transpose, mat, offset);
  }
  
  
  public void glUniformMatrix4fv(int loc, int count, boolean transpose, float[] mat, int offset) {
    GLES20.glUniformMatrix4fv(loc, count, transpose, mat, offset);      
  }
  
  
  public void glVertexAttrib1f(int loc, float value) {
    GLES20.glVertexAttrib1f(loc, value);  
  }
  
  
  public void glVertexAttrib2f(int loc, float value0, float value1) {
    GLES20.glVertexAttrib2f(loc, value0, value1);  
  }  
  
  
  public void glVertexAttrib3f(int loc, float value0, float value1, float value2) {
    GLES20.glVertexAttrib3f(loc, value0, value1, value2);  
  }    

  
  public void glVertexAttrib4f(int loc, float value0, float value1, float value2, float value3) {
    GLES20.glVertexAttrib4f(loc, value0, value1, value2, value3);  
  }
  
  
  public void glShaderSource(int id, String source) {
    GLES20.glShaderSource(id, source);    
  }
  
  
  public void glCompileShader(int id) {
    GLES20.glCompileShader(id);    
  }
  
  
  public void glAttachShader(int prog, int shader) {
    GLES20.glAttachShader(prog, shader);  
  }
  
  
  public void glGetShaderiv(int shader, int pname, int[] params, int offset) {
    GLES20.glGetShaderiv(shader, pname, params, offset);  
  }
  
  
  public String glGetShaderInfoLog(int shader) {
    return GLES20.glGetShaderInfoLog(shader);
  }
  
  
  public void glGetProgramiv(int prog, int pname, int[] params, int offset) {
    GLES20.glGetProgramiv(prog, pname, params, offset);  
  }
  
  
  public String glGetProgramInfoLog(int prog) {
    return GLES20.glGetProgramInfoLog(prog);
  }  
  
  
  /////////////////////////////////////////////////////////////////////////////////
  
  // Viewport
  
      
  public void glViewport(int x, int y, int width, int height) {
    GLES20.glViewport(x, y, width, height);
  }

  
  /////////////////////////////////////////////////////////////////////////////////
  
  // Clipping (scissor test)
  
  
  public void glScissor(int x, int y, int w, int h) {
    GLES20.glScissor(x, y, w, h);
  }    
  
  
  /////////////////////////////////////////////////////////////////////////////////
  
  // Blending  
    
    
  public void glBlendEquation(int eq) {
    GLES20.glBlendEquation(eq);
  }
  

  public void glBlendFunc(int srcFactor, int dstFactor) {
    GLES20.glBlendFunc(srcFactor, dstFactor);
  }

  
  /////////////////////////////////////////////////////////////////////////////////
  
  // Pixels  
  
  
  public void glReadBuffer(int buf) {
//    GLES20.glReadBuffer(buf);
  }
  
  
  public void glReadPixels(int x, int y, int width, int height, int format, int type, Buffer buffer) {
    GLES20.glReadPixels(x, y, width, height, format, type, buffer);
  } 
  
  
  public void glDrawBuffer(int buf) {
//    GLES20.glDrawBuffer(GLES20.GL_COLOR_ATTACHMENT0 + buf);
  }
  
  
  public void glClearColor(float r, float g, float b, float a) {
    GLES20.glClearColor(r, g, b, a);    
  }
  
  
  public void glClear(int mask) {
    GLES20.glClear(mask);
  }  
  
  
  /////////////////////////////////////////////////////////////////////////////////
  
  // Context interface  
  
  
  public Context createEmptyContext() {
    return new Context();
  }
    
  
  public Context getCurrentContext() {
    return new Context();
  }
  
  
  public class Context {

    Context() {    
    }
    
    boolean current() {
      return true;
    }    
    
    boolean equal() {
      return true;
    }
    
    int code() {
      return 0;
    }
  }  
  
  
  /////////////////////////////////////////////////////////////////////////////////
  
  // Tessellator interface
  
  
  public Tessellator createTessellator(TessellatorCallback callback) {
    return new Tessellator(callback);
  }
  
  
  public class Tessellator {
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
      public void begin(int type) {
        callback.begin(type);
      }
      
      public void end() {
        callback.end();
      }
      
      public void vertex(Object data) {
        callback.vertex(data);
      }
      
      public void combine(double[] coords, Object[] data,
                          float[] weight, Object[] outData) {
        callback.combine(coords, data, weight, outData);
      }
      
      public void error(int errnum) {
        callback.error(errnum);
      }
    }
  }

  
  public interface TessellatorCallback  {
    public void begin(int type);
    public void end();
    public void vertex(Object data);
    public void combine(double[] coords, Object[] data,
                        float[] weight, Object[] outData);
    public void error(int errnum);    
  }
  
  
  ///////////////////////////////////////////////////////////////////////////////////
  
  // Utility functions  
  
  
  public boolean contextIsCurrent(Context other) {
    return other == null || other.current();
  }
  
  
  public void enableTexturing(int target) {
  }

  
  public void disableTexturing(int target) {
  }  
  
  
  public void initTexture(int target, int format, int width, int height) {
    // Doing in patches of 16x16 pixels to avoid creating a (potentially)
    // very large transient array which in certain situations (memory-
    // constrained android devices) might lead to an out-of-memory error.
    int[] texels = new int[16 * 16];
    for (int y = 0; y < height; y += 16) {
      int h = PApplet.min(16, height - y);
      for (int x = 0; x < width; x += 16) {
        int w = PApplet.min(16, width - x);
        glTexSubImage2D(target, 0, x, y, w, h, format, GL_UNSIGNED_BYTE, IntBuffer.wrap(texels));
      }
    }
  }  

  
  public void copyToTexture(int target, int format, int id, int x, int y, int w, int h, IntBuffer buffer) {           
    enableTexturing(target);
    glBindTexture(target, id);    
    glTexSubImage2D(target, 0, x, y, w, h, format, GL_UNSIGNED_BYTE, buffer);
    glBindTexture(target, 0);
    disableTexturing(target);
  }   
   
  
  public void drawTexture(int target, int id, int width, int height,
                          int X0, int Y0, int X1, int Y1) {
    drawTexture(target, id, width, height, X0, Y0, X1, Y1, X0, Y0, X1, Y1);
  }  
  
  
  public void drawTexture(int target, int id, int width, int height,
                                              int texX0, int texY0, int texX1, int texY1, 
                                              int scrX0, int scrY0, int scrX1, int scrY1) {
    if (!loadedTexShader) {
      texVertShader = createShader(GL_VERTEX_SHADER, texVertShaderSource);
      texFragShader = createShader(GL_FRAGMENT_SHADER, texFragShaderSource);
      if (0 < texVertShader && 0 < texFragShader) {
        texShaderProgram = createProgram(texVertShader, texFragShader);
      }
      if (0 < texShaderProgram) {
        texVertLoc = glGetAttribLocation(texShaderProgram, "inVertex");
        texTCoordLoc = glGetAttribLocation(texShaderProgram, "inTexcoord");
      }
      texData = ByteBuffer.allocateDirect(texCoords.length * SIZEOF_FLOAT).order(ByteOrder.nativeOrder()).asFloatBuffer();
      loadedTexShader = true;
    }
        
    if (0 < texShaderProgram) {
      // When drawing the texture we don't write to the
      // depth mask, so the texture remains in the background
      // and can be occluded by anything drawn later, even if
      // if it is behind it.
      boolean[] val = new boolean[1];
      glGetBooleanv(GL_DEPTH_WRITEMASK, val, 0);
      boolean writeMask = val[0];
      glDepthMask(false);
      
      glUseProgram(texShaderProgram);
      
      glEnableVertexAttribArray(texVertLoc);
      glEnableVertexAttribArray(texTCoordLoc);

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
      
      enableTexturing(target);
      glActiveTexture(GL_TEXTURE0);
      glBindTexture(target, id);
      
      texData.position(0);
      glVertexAttribPointer(texVertLoc, 2, GL_FLOAT, false, 4 * SIZEOF_FLOAT, texData);
      texData.position(2);
      glVertexAttribPointer(texTCoordLoc, 2, GL_FLOAT, false, 4 * SIZEOF_FLOAT, texData);
      
      glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
      
      glBindTexture(target, 0);
      disableTexturing(target);

      glDisableVertexAttribArray(texVertLoc);
      glDisableVertexAttribArray(texTCoordLoc);

      glUseProgram(0);

      glDepthMask(writeMask);
    }
  }
  
  
  public void drawRectangle(float r, float g, float b, float a, 
                            int scrX0, int scrY0, int scrX1, int scrY1) {
    if (!loadedRectShader) {
      rectVertShader = createShader(GL_VERTEX_SHADER, rectVertShaderSource);
      rectFragShader = createShader(GL_FRAGMENT_SHADER, rectFragShaderSource);
      if (0 < rectVertShader && 0 < rectFragShader) {
        rectShaderProgram = createProgram(rectVertShader, rectFragShader);
      }
      if (0 < rectShaderProgram) {
        rectVertLoc = glGetAttribLocation(rectShaderProgram, "inVertex");
        rectColorLoc = glGetUniformLocation(rectShaderProgram, "rectColor");
      }
      rectData = ByteBuffer.allocateDirect(rectCoords.length * SIZEOF_FLOAT).order(ByteOrder.nativeOrder()).asFloatBuffer();
      loadedRectShader = true;
    }

    if (0 < rectShaderProgram) {
      // When drawing the rectangle we don't write to the
      // depth mask, so the rectangle remains in the background
      // and can be occluded by anything drawn later, even if
      // if it is behind it.
      boolean[] val = new boolean[1];
      glGetBooleanv(GL_DEPTH_WRITEMASK, val, 0);
      boolean writeMask = val[0];
      glDepthMask(false);

      glUseProgram(rectShaderProgram);

      glEnableVertexAttribArray(rectVertLoc);
      glUniform4f(rectColorLoc, r, g, b, a);

      // Vertex coordinates of the rectangle are specified
      // in normalized screen space (-1, 1):

      // Corner 1
      rectCoords[0] = 2 * (float)scrX0 / pg.width - 1;
      rectCoords[1] = 2 * (float)scrY0 / pg.height - 1;

      // Corner 2
      rectCoords[2] = 2 * (float)scrX1 / pg.width - 1;
      rectCoords[3] = 2 * (float)scrY0 / pg.height - 1;

      // Corner 3
      rectCoords[4] = 2 * (float)scrX0 / pg.width - 1;
      rectCoords[5] = 2 * (float)scrY1 / pg.height - 1;

      // Corner 4
      rectCoords[6] = 2 * (float)scrX1 / pg.width - 1;
      rectCoords[7] = 2 * (float)scrY1 / pg.height - 1;

      rectData.rewind();
      rectData.put(rectCoords);

      rectData.position(0);
      glVertexAttribPointer(rectVertLoc, 2, GL_FLOAT, false, 2 * SIZEOF_FLOAT, rectData);

      glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

      glDisableVertexAttribArray(rectVertLoc);

      glUseProgram(0);

      glDepthMask(writeMask);
    }
  }
  
  
  public int getColorValue(int scrX, int scrY) {
    if (colorBuffer == null) {
      colorBuffer = IntBuffer.allocate(1);
    }
    colorBuffer.rewind();
    glReadPixels(scrX, pg.height - scrY - 1, 1, 1, GL_RGBA, GL_UNSIGNED_BYTE, colorBuffer);    
    return colorBuffer.get();
  }
  
  
  public float getDepthValue(int scrX, int scrY) {
    // http://stackoverflow.com/questions/2596682/opengl-es-2-0-read-depth-buffer
    return 0;
  }
  
  
  public byte getStencilValue(int scrX, int scrY) {
    return 0;
  }
  
  
  // bit shifting this might be more efficient
  static public int nextPowerOfTwo(int val) {
    int ret = 1;
    while (ret < val) {
      ret <<= 1;
    }
    return ret;
  }   
  
  
  static public int javaToNativeARGB(int color) {
    if (BIG_ENDIAN) {
      return ((color >> 24) & 0xff) | 
             ((color << 8) & 0xffffff00);
    } else {
      return (color & 0xff000000) | 
             ((color << 16) & 0xff0000) | 
             (color & 0xff00) | 
             ((color >> 16) & 0xff);
    }
  }
  
  
  static public int nativeToJavaARGB(int color) {
    if (BIG_ENDIAN) {
      return (color & 0xff000000) |
             ((color >> 8) & 0x00ffffff);
    } else {
      return (color & 0xff000000) |
             ((color << 16) & 0xff0000) |
             (color & 0xff00) |
             ((color >> 16) & 0xff);
    }
  }  
  
  
  /**
   * Convert native OpenGL format into palatable ARGB format. This function
   * leaves alone (ignores) the alpha component. Also flips the image
   * vertically, since images are upside-down in GL.
   */ 
  static public void nativeToJavaRGB(int[] pixels, int width, int height) {
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
    
    // When height is an odd number, the middle line needs to be
    // endian swapped, but not y-swapped.
    // http://dev.processing.org/bugs/show_bug.cgi?id=944
    if ((height % 2) == 1) {
      index = (height / 2) * width;
      if (BIG_ENDIAN) {
        for (int x = 0; x < width; x++) {
          pixels[index] = 0xff000000 | ((pixels[index] >> 8) & 0x00ffffff);
          index++;
        }
      } else {
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
   * Convert native OpenGL format into palatable ARGB format. This function
   * leaves alone (ignores) the alpha component. Also flips the image
   * vertically, since images are upside-down in GL.
   */  
  static public void nativeToJavaARGB(int[] pixels, int width, int height) {
    int index = 0;
    int yindex = (height - 1) * width;
    for (int y = 0; y < height / 2; y++) {
      if (BIG_ENDIAN) {
        for (int x = 0; x < width; x++) {
          int temp = pixels[index];
          // ignores alpha component, just sets it opaque
          pixels[index] = (pixels[yindex] & 0xff000000) | 
                          ((pixels[yindex] >> 8) & 0x00ffffff);
          pixels[yindex] = (temp & 0xff000000) | 
                           ((temp >> 8) & 0x00ffffff);
          index++;
          yindex++;
        }
      } else { // LITTLE_ENDIAN, convert ABGR to ARGB
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
    
    if ((height % 2) == 1) {
      index = (height / 2) * width;
      if (BIG_ENDIAN) {
        for (int x = 0; x < width; x++) {
          pixels[index] = (pixels[index] & 0xff000000) | 
                          ((pixels[index] >> 8) & 0x00ffffff);
          index++;
        }
      } else {
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
   * Convert ARGB (Java/Processing) data to native OpenGL format. This function
   * leaves alone (ignores) the alpha component. Also flips the image
   * vertically, since images are upside-down in GL.
   */  
  static public void javaToNativeRGB(int[] pixels, int width, int height) {
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
    
    if ((height % 2) == 1) {
      index = (height / 2) * width;
      if (BIG_ENDIAN) {
        for (int x = 0; x < width; x++) {
          pixels[index] = ((pixels[index] << 8) & 0xffffff00) | 0xff;
          index++;
        }
      } else {
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
   * Convert Java ARGB to native OpenGL format. Also flips the image vertically,
   * since images are upside-down in GL.
   */ 
  static public void javaToNativeARGB(int[] pixels, int width, int height) {
    int index = 0;
    int yindex = (height - 1) * width;
    for (int y = 0; y < height / 2; y++) {
      if (BIG_ENDIAN) {
        // and convert ARGB back to opengl RGBA components (big endian)
        for (int x = 0; x < width; x++) {
          int temp = pixels[index];
          pixels[index] = ((pixels[yindex] >> 24) & 0xff) | 
                          ((pixels[yindex] << 8) & 0xffffff00);
          pixels[yindex] = ((temp >> 24) & 0xff) | 
                           ((temp << 8) & 0xffffff00);
          index++;
          yindex++;
        }

      } else {
        // convert ARGB back to native little endian ABGR
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
    
    if ((height % 2) == 1) {
      index = (height / 2) * width;
      if (BIG_ENDIAN) {
        for (int x = 0; x < width; x++) {
          pixels[index] = ((pixels[index] >> 24) & 0xff) | 
                          ((pixels[index] << 8) & 0xffffff00);
          index++;
        }
      } else {
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
  
  
  public int createShader(int shaderType, String source) {
    int shader = glCreateShader(shaderType);
    if (shader != 0) {
      glShaderSource(shader, source);
      glCompileShader(shader);
      int[] compiled = new int[1];
      glGetShaderiv(shader, GL_COMPILE_STATUS, compiled, 0);
      if (compiled[0] == GL_FALSE) {
        System.err.println("Could not compile shader " + shaderType + ":");
        System.err.println(glGetShaderInfoLog(shader));
        glDeleteShader(shader);
        shader = 0;
      }
    }
    return shader;
  } 
  
  
  public int createProgram(int vertexShader, int fragmentShader) {
    int program = glCreateProgram();
    if (program != 0) {
      glAttachShader(program, vertexShader);
      glAttachShader(program, fragmentShader);
      glLinkProgram(program);
      int[] linked = new int[1];
      glGetProgramiv(program, GL_LINK_STATUS, linked, 0);
      if (linked[0] == GL_FALSE) {
        System.err.println("Could not link program: ");
        System.err.println(glGetProgramInfoLog(program));
        glDeleteProgram(program);
        program = 0;
      }            
    }
    return program;
  }
  
  
  public boolean validateFramebuffer() {
    int status = glCheckFramebufferStatus(GL_FRAMEBUFFER);
    if (status == GL_FRAMEBUFFER_COMPLETE) {
      return true;
    } else if (status == GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT) {
      throw new RuntimeException("PFramebuffer: GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT (" + Integer.toHexString(status) + ")");
    } else if (status == GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT) {
      throw new RuntimeException("PFramebuffer: GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT (" + Integer.toHexString(status) + ")");
    } else if (status == GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS) {
      throw new RuntimeException("PFramebuffer: GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS (" + Integer.toHexString(status) + ")");      
    } else if (status == GL_FRAMEBUFFER_INCOMPLETE_FORMATS) {
      throw new RuntimeException("PFramebuffer: GL_FRAMEBUFFER_INCOMPLETE_FORMATS (" + Integer.toHexString(status) + ")");
    } else if (status == GL_FRAMEBUFFER_UNSUPPORTED) {
      throw new RuntimeException("PFramebuffer: GL_FRAMEBUFFER_UNSUPPORTED" + Integer.toHexString(status));      
    } else {
      throw new RuntimeException("PFramebuffer: unknown framebuffer error (" + Integer.toHexString(status) + ")");
    }
  }  
  
  
  /////////////////////////////////////////////////////////////////////////////////
  
  // Android specific stuff   
  
  
  public AndroidRenderer getRenderer() {
    return renderer;
  }
  
  
  public AndroidContextFactory getContextFactory() {
    return new AndroidContextFactory();
  }  
  
  
  public AndroidConfigChooser getConfigChooser(int r, int g, int b, int a, int d, int s) {
    return new AndroidConfigChooser(r, g, b, a, d, s);
  }    
  
  
  public class AndroidRenderer implements Renderer {
    public AndroidRenderer() {
    }

    public void onDrawFrame(GL10 igl) {
      gl = igl;
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
    }    
  }
  
  
  public class AndroidContextFactory implements GLSurfaceView.EGLContextFactory {
    public EGLContext createContext(EGL10 egl, EGLDisplay display,
        EGLConfig eglConfig) {
      int[] attrib_list = { EGL_CONTEXT_CLIENT_VERSION, 2,
                            EGL10.EGL_NONE };
      EGLContext context = egl.eglCreateContext(display, eglConfig, EGL10.EGL_NO_CONTEXT, attrib_list);
      return context;
    }

    public void destroyContext(EGL10 egl, EGLDisplay display, EGLContext context) {
      egl.eglDestroyContext(display, context);
    }
  }    

  
  public class AndroidConfigChooser implements EGLConfigChooser {
    // Desired size (in bits) for the rgba color, depth and stencil buffers.
    public int redTarget;
    public int greenTarget;
    public int blueTarget;
    public int alphaTarget;
    public int depthTarget;
    public int stencilTarget;
    
    // Actual rgba color, depth and stencil sizes (in bits) supported by the device.
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
                                        EGL10.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
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

    public EGLConfig chooseBestConfig(EGL10 egl, EGLDisplay display, EGLConfig[] configs) {
      EGLConfig bestConfig = null;
      float bestScore = 1000;
      
      for (EGLConfig config : configs) {
        int gl = findConfigAttrib(egl, display, config, EGL10.EGL_RENDERABLE_TYPE, 0);
        if (gl == EGL_OPENGL_ES2_BIT) {        
          int d = findConfigAttrib(egl, display, config, EGL10.EGL_DEPTH_SIZE, 0);
          int s = findConfigAttrib(egl, display, config, EGL10.EGL_STENCIL_SIZE, 0);

          int r = findConfigAttrib(egl, display, config, EGL10.EGL_RED_SIZE, 0);
          int g = findConfigAttrib(egl, display, config, EGL10.EGL_GREEN_SIZE, 0);
          int b = findConfigAttrib(egl, display, config, EGL10.EGL_BLUE_SIZE, 0);
          int a = findConfigAttrib(egl, display, config, EGL10.EGL_ALPHA_SIZE, 0);
          
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

    protected String printConfig(EGL10 egl, EGLDisplay display, EGLConfig config) {
      int r = findConfigAttrib(egl, display, config, EGL10.EGL_RED_SIZE, 0);
      int g = findConfigAttrib(egl, display, config, EGL10.EGL_GREEN_SIZE, 0);
      int b = findConfigAttrib(egl, display, config, EGL10.EGL_BLUE_SIZE, 0);
      int a = findConfigAttrib(egl, display, config, EGL10.EGL_ALPHA_SIZE, 0);
      int d = findConfigAttrib(egl, display, config, EGL10.EGL_DEPTH_SIZE, 0);
      int s = findConfigAttrib(egl, display, config, EGL10.EGL_STENCIL_SIZE, 0);
      int type = findConfigAttrib(egl, display, config, EGL10.EGL_RENDERABLE_TYPE, 0);
      int nat = findConfigAttrib(egl, display, config, EGL10.EGL_NATIVE_RENDERABLE, 0);
      int bufSize = findConfigAttrib(egl, display, config, EGL10.EGL_BUFFER_SIZE, 0);
      int bufSurf = findConfigAttrib(egl, display, config, EGL10.EGL_RENDER_BUFFER, 0);

      return String.format("EGLConfig rgba=%d%d%d%d depth=%d stencil=%d", r,g,b,a,d,s) 
        + " type=" + type 
        + " native=" + nat 
        + " buffer size=" + bufSize 
        + " buffer surface=" + bufSurf + 
        String.format(" caveat=0x%04x", findConfigAttrib(egl, display, config, EGL10.EGL_CONFIG_CAVEAT, 0));
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
