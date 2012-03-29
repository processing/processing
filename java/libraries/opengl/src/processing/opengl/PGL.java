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

import java.awt.BorderLayout;
import java.awt.Canvas;
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
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLCanvas;
import javax.media.opengl.glu.GLU;
import javax.media.opengl.glu.GLUtessellator;
import javax.media.opengl.glu.GLUtessellatorCallbackAdapter;

import com.jogamp.newt.awt.NewtCanvasAWT;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.util.AnimatorBase;

/** 
 * Processing-OpenGL abstraction layer.
 * 
 */
public class PGL {
  // The two windowing toolkits available to use in JOGL:
  public static int AWT  = 0; // http://jogamp.org/wiki/index.php/Using_JOGL_in_AWT_SWT_and_Swing
  public static int NEWT = 1; // http://jogamp.org/jogl/doc/NEWT-Overview.html  
  
  public static int toolkit = AWT;
  
  /** Size of a short (in bytes). */
  static final int SIZEOF_SHORT = Short.SIZE / 8; 
  
  /** Size of an int (in bytes). */
  static final int SIZEOF_INT = Integer.SIZE / 8;
   
  /** Size of a float (in bytes). */
  static final int SIZEOF_FLOAT = Float.SIZE / 8;
  
  /** Size of a vertex index. */
  static final int SIZEOF_INDEX = SIZEOF_INT; 
  
  /** Type of a vertex index. */
  static final int INDEX_TYPE = GL.GL_UNSIGNED_INT;

  /** Initial sizes for arrays of input and tessellated data. */
  public static final int DEFAULT_IN_VERTICES = 64;
  public static final int DEFAULT_IN_EDGES = 128;
  public static final int DEFAULT_IN_TEXTURES = 64;
  public static final int DEFAULT_TESS_VERTICES = 64;
  public static final int DEFAULT_TESS_INDICES = 128;  
  
  /** Initial sizes for vertex cache used in PShape3D. */
  public static final int DEFAULT_VERTEX_CACHE_SIZE = 512;
  
  /** Maximum lights by default is 8, the minimum defined by OpenGL. */   
  public static final int MAX_LIGHTS = 8;
  
  /** Maximum number of tessellated vertices, using 2^19 for Mac/PC. */
  public static final int MAX_TESS_VERTICES = 524288;
  
  /** Maximum number of indices. 2 times the max number of 
   * vertices to have good room for vertex reuse. */
  public static final int MAX_TESS_INDICES  = 2 * MAX_TESS_VERTICES;  

  /** Maximum dimension of a texture used to hold font data. **/
  public static final int MAX_FONT_TEX_SIZE = 256;
  
  /** Minimum array size to use arrayCopy method(). **/
  static protected final int MIN_ARRAYCOPY_SIZE = 2;    
  
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
  
  public static final int GL_FALSE = GL.GL_FALSE;
  public static final int GL_TRUE  = GL.GL_TRUE;  
  
  public static final int GL_LESS   = GL.GL_LESS;
  public static final int GL_LEQUAL = GL.GL_LEQUAL;
  public static final int GL_CCW    = GL.GL_CCW;
  public static final int GL_CW     = GL.GL_CW;  
  public static final int GL_FRONT  = GL.GL_FRONT;
  public static final int GL_BACK   = GL.GL_BACK;
  
  public static final int GL_VIEWPORT = GL.GL_VIEWPORT;
  
  public static final int GL_SCISSOR_TEST    = GL.GL_SCISSOR_TEST;  
  public static final int GL_DEPTH_TEST      = GL.GL_DEPTH_TEST;
  public static final int GL_DEPTH_WRITEMASK = GL.GL_DEPTH_WRITEMASK;  
  
  public static final int GL_COLOR_BUFFER_BIT   = GL.GL_COLOR_BUFFER_BIT; 
  public static final int GL_DEPTH_BUFFER_BIT   = GL.GL_DEPTH_BUFFER_BIT; 
  public static final int GL_STENCIL_BUFFER_BIT = GL.GL_STENCIL_BUFFER_BIT;
  
  public static final int GL_FUNC_ADD              = GL.GL_FUNC_ADD;
  public static final int GL_FUNC_MIN              = GL2.GL_MIN;
  public static final int GL_FUNC_MAX              = GL2.GL_MAX;
  public static final int GL_FUNC_REVERSE_SUBTRACT = GL.GL_FUNC_REVERSE_SUBTRACT;
  
  public static final int GL_TEXTURE_2D     = GL.GL_TEXTURE_2D;
  public static final int GL_RGB            = GL.GL_RGB;
  public static final int GL_RGBA           = GL.GL_RGBA;
  public static final int GL_ALPHA          = GL.GL_ALPHA;
  public static final int GL_UNSIGNED_INT   = GL.GL_UNSIGNED_INT;
  public static final int GL_UNSIGNED_BYTE  = GL.GL_UNSIGNED_BYTE;
  public static final int GL_UNSIGNED_SHORT = GL.GL_UNSIGNED_SHORT;
  public static final int GL_FLOAT          = GL.GL_FLOAT;
  
  public static final int GL_NEAREST              = GL.GL_NEAREST;
  public static final int GL_LINEAR               = GL.GL_LINEAR;
  public static final int GL_LINEAR_MIPMAP_LINEAR = GL.GL_LINEAR_MIPMAP_LINEAR;
  
  public static final int GL_CLAMP_TO_EDGE = GL.GL_CLAMP_TO_EDGE;
  public static final int GL_REPEAT        = GL.GL_REPEAT;
  
  public static final int GL_RGBA8            = GL.GL_RGBA8;  
  public static final int GL_DEPTH24_STENCIL8 = GL.GL_DEPTH24_STENCIL8;
  
  public static final int GL_DEPTH_COMPONENT   = GL2.GL_DEPTH_COMPONENT;
  public static final int GL_DEPTH_COMPONENT16 = GL.GL_DEPTH_COMPONENT16;
  public static final int GL_DEPTH_COMPONENT24 = GL.GL_DEPTH_COMPONENT24;
  public static final int GL_DEPTH_COMPONENT32 = GL.GL_DEPTH_COMPONENT32;    
  
  public static final int GL_STENCIL_INDEX  = GL2.GL_STENCIL_INDEX;
  public static final int GL_STENCIL_INDEX1 = GL.GL_STENCIL_INDEX1;
  public static final int GL_STENCIL_INDEX4 = GL.GL_STENCIL_INDEX4; 
  public static final int GL_STENCIL_INDEX8 = GL.GL_STENCIL_INDEX8;   
  
  public static final int GL_ARRAY_BUFFER         = GL.GL_ARRAY_BUFFER;
  public static final int GL_ELEMENT_ARRAY_BUFFER = GL.GL_ELEMENT_ARRAY_BUFFER;
  
  public static final int GL_SAMPLES = GL.GL_SAMPLES;
    
  public static final int GL_FRAMEBUFFER_COMPLETE                      = GL.GL_FRAMEBUFFER_COMPLETE;    
  public static final int GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT         = GL.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT;
  public static final int GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT = GL.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT;
  public static final int GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS         = GL.GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS;      
  public static final int GL_FRAMEBUFFER_INCOMPLETE_FORMATS            = GL.GL_FRAMEBUFFER_INCOMPLETE_FORMATS;  
  public static final int GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER        = GL2.GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER;
  public static final int GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER        = GL2.GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER;  
  public static final int GL_FRAMEBUFFER_UNSUPPORTED                   = GL.GL_FRAMEBUFFER_UNSUPPORTED;
    
  public static final int GL_STATIC_DRAW  = GL.GL_STATIC_DRAW;
  public static final int GL_DYNAMIC_DRAW = GL.GL_DYNAMIC_DRAW;
  public static final int GL_STREAM_DRAW  = GL2.GL_STREAM_DRAW;
  
  public static final int GL_READ_ONLY  = GL2.GL_READ_ONLY;
  public static final int GL_WRITE_ONLY = GL2.GL_WRITE_ONLY;  
  public static final int GL_READ_WRITE = GL2.GL_READ_WRITE;
    
  public static final int GL_TRIANGLE_FAN   = GL.GL_TRIANGLE_FAN;
  public static final int GL_TRIANGLE_STRIP = GL.GL_TRIANGLE_STRIP;
  public static final int GL_TRIANGLES      = GL.GL_TRIANGLES;  
  
  public static final int GL_VENDOR     = GL.GL_VENDOR;
  public static final int GL_RENDERER   = GL.GL_RENDERER;
  public static final int GL_VERSION    = GL.GL_VERSION;
  public static final int GL_EXTENSIONS = GL.GL_EXTENSIONS;
    
  public static final int GL_MAX_TEXTURE_SIZE         = GL.GL_MAX_TEXTURE_SIZE;
  public static final int GL_MAX_SAMPLES              = GL2.GL_MAX_SAMPLES;  
  public static final int GL_ALIASED_LINE_WIDTH_RANGE = GL.GL_ALIASED_LINE_WIDTH_RANGE;
  public static final int GL_ALIASED_POINT_SIZE_RANGE = GL.GL_ALIASED_POINT_SIZE_RANGE;
  public static final int GL_DEPTH_BITS               = GL.GL_DEPTH_BITS;
  public static final int GL_STENCIL_BITS             = GL.GL_STENCIL_BITS;  
  
  public static final int GLU_TESS_WINDING_NONZERO = GLU.GLU_TESS_WINDING_NONZERO;
  public static final int GLU_TESS_WINDING_ODD     = GLU.GLU_TESS_WINDING_ODD;  
    
  public static final int GL_TEXTURE0           = GL.GL_TEXTURE0;
  public static final int GL_TEXTURE1           = GL.GL_TEXTURE1;
  public static final int GL_TEXTURE2           = GL.GL_TEXTURE2;
  public static final int GL_TEXTURE3           = GL.GL_TEXTURE3;
  public static final int GL_TEXTURE_MIN_FILTER = GL.GL_TEXTURE_MIN_FILTER;
  public static final int GL_TEXTURE_MAG_FILTER = GL.GL_TEXTURE_MAG_FILTER;
  public static final int GL_TEXTURE_WRAP_S     = GL.GL_TEXTURE_WRAP_S;
  public static final int GL_TEXTURE_WRAP_T     = GL.GL_TEXTURE_WRAP_T;  
  
  public static final int GL_BLEND               = GL.GL_BLEND;
  public static final int GL_ONE                 = GL.GL_ONE; 
  public static final int GL_ZERO                = GL.GL_ZERO;
  public static final int GL_SRC_ALPHA           = GL.GL_SRC_ALPHA; 
  public static final int GL_DST_ALPHA           = GL.GL_DST_ALPHA;
  public static final int GL_ONE_MINUS_SRC_ALPHA = GL.GL_ONE_MINUS_SRC_ALPHA;
  public static final int GL_ONE_MINUS_DST_COLOR = GL.GL_ONE_MINUS_DST_COLOR;
  public static final int GL_ONE_MINUS_SRC_COLOR = GL.GL_ONE_MINUS_SRC_COLOR;
  public static final int GL_DST_COLOR           = GL.GL_DST_COLOR;
  public static final int GL_SRC_COLOR           = GL.GL_SRC_COLOR;
  
  public static final int GL_FRAMEBUFFER        = GL.GL_FRAMEBUFFER;
  public static final int GL_COLOR_ATTACHMENT0  = GL.GL_COLOR_ATTACHMENT0;
  public static final int GL_COLOR_ATTACHMENT1  = GL2.GL_COLOR_ATTACHMENT1;
  public static final int GL_COLOR_ATTACHMENT2  = GL2.GL_COLOR_ATTACHMENT2;
  public static final int GL_COLOR_ATTACHMENT3  = GL2.GL_COLOR_ATTACHMENT3;  
  public static final int GL_RENDERBUFFER       = GL.GL_RENDERBUFFER;
  public static final int GL_DEPTH_ATTACHMENT   = GL.GL_DEPTH_ATTACHMENT;
  public static final int GL_STENCIL_ATTACHMENT = GL.GL_STENCIL_ATTACHMENT;  
  public static final int GL_READ_FRAMEBUFFER   = GL2.GL_READ_FRAMEBUFFER;
  public static final int GL_DRAW_FRAMEBUFFER   = GL2.GL_DRAW_FRAMEBUFFER;   
  
  public static final int GL_VERTEX_SHADER        = GL2.GL_VERTEX_SHADER;
  public static final int GL_FRAGMENT_SHADER      = GL2.GL_FRAGMENT_SHADER;
  public static final int GL_INFO_LOG_LENGTH      = GL2.GL_INFO_LOG_LENGTH;
  public static final int GL_SHADER_SOURCE_LENGTH = GL2.GL_SHADER_SOURCE_LENGTH;
  public static final int GL_COMPILE_STATUS       = GL2.GL_COMPILE_STATUS;
  public static final int GL_LINK_STATUS          = GL2.GL_LINK_STATUS;
  public static final int GL_VALIDATE_STATUS      = GL2.GL_VALIDATE_STATUS;  
  
  public static final int GL_MULTISAMPLE    = GL.GL_MULTISAMPLE;  
  public static final int GL_POINT_SMOOTH   = GL2.GL_POINT_SMOOTH;      
  public static final int GL_LINE_SMOOTH    = GL.GL_LINE_SMOOTH;    
  public static final int GL_POLYGON_SMOOTH = GL2.GL_POLYGON_SMOOTH;  
  
  /** Basic GL functionality, common to all profiles */
  public GL gl;
  
  /** GLES2 functionality (shaders, etc) */
  public GL2ES2 gl2;
  
  /** GL2 desktop functionality (blit framebuffer, map buffer range, multisampled renerbuffers) */
  public GL2 gl2x;
  
  /** GLU interface **/
  public GLU glu;
  
  /** The PGraphics object using this interface */
  public PGraphicsOpenGL pg;  
  
  /** Whether OpenGL has been initialized or not */ 
  public boolean initialized;
  
  /** Selected GL profile */
  public GLProfile profile;
  
  /** The capabilities of the OpenGL rendering surface */
  public GLCapabilitiesImmutable capabilities;  
  
  /** The rendering surface */
  public GLDrawable drawable;   
  
  /** The rendering context (holds rendering state info) */
  public GLContext context;  
  
  /** The AWT canvas where OpenGL rendering takes place */
  public Canvas canvas;
  
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
    glu = new GLU();
    initialized = false;
  }

  
  public void setFramerate(float framerate) {    
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
  

  public void initPrimarySurface(int antialias) {
    if (profile == null) {
      profile = GLProfile.getDefault();      
    } else {
      // Restarting...
      if (toolkit == AWT) {
        canvasAWT.removeGLEventListener(listener);    
        pg.parent.removeListeners(canvasAWT);
        pg.parent.remove(canvasAWT);
      } else if (toolkit == NEWT) {
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
    caps.setDepthBits(24);
    caps.setStencilBits(8);
    caps.setAlphaBits(8);
    
    if (toolkit == AWT) {      
      canvasAWT = new GLCanvas(caps);
      canvasAWT.setBounds(0, 0, pg.width, pg.height);
      
      pg.parent.setLayout(new BorderLayout());
      pg.parent.add(canvasAWT, BorderLayout.CENTER);
      pg.parent.removeListeners();
      pg.parent.addListeners(canvasAWT);
      
      listener = new PGLListener();
      canvasAWT.addGLEventListener(listener);
      
      capabilities = canvasAWT.getChosenGLCapabilities();
      canvas = canvasAWT;
    } else if (toolkit == NEWT) {    
      window = GLWindow.create(capabilities);    
      canvasNEWT = new NewtCanvasAWT(window);
      
      pg.parent.setLayout(new BorderLayout());
      pg.parent.add(canvasNEWT, BorderLayout.CENTER);
      pg.parent.removeListeners();
      pg.parent.addListeners(canvasNEWT);
      
      listener = new PGLListener();
      window.addGLEventListener(listener);
      animator = new NEWTAnimator(window);
      animator.start();
      
      capabilities = window.getChosenGLCapabilities();
      canvas = canvasNEWT;      
    }
    
    initialized = true;
  }
  
  
  public void initOffscreenSurface(PGL primary) {
    context = primary.context;
    capabilities = primary.capabilities;
    drawable = null;
    initialized = true;
  }    
  
  
  public void updatePrimary() {
    if (!setFramerate) {
      setFramerate(targetFramerate);
    }
  }

  
  public void updateOffscreen(PGL primary) {
    gl  = primary.gl;
    gl2 = primary.gl2;
    gl2x = primary.gl2x;
  }

  
  ///////////////////////////////////////////////////////////////////////////////////
  
  // Frame rendering    
  
  
  public void beginOnscreenDraw(boolean clear) {
  }
  
  
  public void endOnscreenDraw(boolean clear0) {
  }
  
  
  public void beginOffscreenDraw(boolean clear) {
  }
  
  
  public void endOffscreenDraw(boolean clear0) {    
  }
  
  
  public boolean canDraw() {
    return initialized && pg.parent.isDisplayable();    
  }
  
  
  public void requestDraw() {
    if (initialized) {
      if (toolkit == AWT) {
        canvasAWT.display();           
      } else if (toolkit == NEWT) {
        animator.requestDisplay();       
      }
    }
  }
  
  
  ///////////////////////////////////////////////////////////////////////////////////
  
  // Caps query

  
  public String glGetString(int name) {
    return gl.glGetString(name);
  }
 
  
  public void glGetIntegerv(int name, int[] values, int offset) {
    gl.glGetIntegerv(name, values, offset);
  }
  
  
  public void glGetBooleanv(int name, boolean[] values, int offset) {
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
  
  
  ///////////////////////////////////////////////////////////////////////////////////
  
  // Enable/disable caps
  
  
  public void glEnable(int cap) {
    if (-1 < cap) {
      gl.glEnable(cap);
    }
  }  
  
  
  public void glDisable(int cap) {
    if (-1 < cap) {
      gl.glDisable(cap);
    }
  }  
  
  
  ///////////////////////////////////////////////////////////////////////////////////
  
  // Render control 
  
  
  public void glFlush() {
    gl.glFlush();
  }  
  
  
  public void glFinish() {
    gl.glFinish();
  }
  
  
  /////////////////////////////////////////////////////////////////////////////////
  
  // Error handling  
  
  
  public int glGetError() {
    return gl.glGetError();
  }

  
  public String glErrorString(int err) {
    return glu.gluErrorString(err);
  }  
  
  
  public String gluErrorString(int err) {
    return glu.gluErrorString(err);
  }
  
  
  /////////////////////////////////////////////////////////////////////////////////
  
  // Rendering options

  
  public void glFrontFace(int mode) {
    gl.glFrontFace(mode);
  }
  
  
  public void glDepthMask(boolean flag) {
    gl.glDepthMask(flag);   
  }
  
  
  public void glDepthFunc(int func) {
    gl.glDepthFunc(func);
  }  

  
  /////////////////////////////////////////////////////////////////////////////////
  
  // Textures     
  
  
  public void glGenTextures(int n, int[] ids, int offset) {
    gl.glGenTextures(n, ids, offset);
  }

  
  public void glDeleteTextures(int n, int[] ids, int offset) {
    gl.glDeleteTextures(n, ids, offset);
  }  
  
  
  public void glActiveTexture(int unit) {
    gl.glActiveTexture(unit);
  }
  
  
  public void glBindTexture(int target, int id) {
    gl.glBindTexture(target, id);
  }
  
  
  public void glTexImage2D(int target, int level, int internalFormat, int width, int height, int border, int format, int type, Buffer data) {
    gl.glTexImage2D(target, level, internalFormat, width, height, border, format, type, data);
  }
  
  
  public void glTexSubImage2D(int target, int level, int xOffset, int yOffset, int width, int height, int format, int type, Buffer data) {
    gl.glTexSubImage2D(target, level, xOffset, yOffset, width, height, format, type, data);
  }

  
  public void glTexParameterf(int target, int param, int value) {
    gl.glTexParameterf(target, param, value); 
  }

  
  public void glGenerateMipmap(int target) {
    gl.glGenerateMipmap(target);
  }  
  
  
  /////////////////////////////////////////////////////////////////////////////////
  
  // Vertex Buffers

  
  public void glGenBuffers(int n, int[] ids, int offset) {
    gl.glGenBuffers(n, ids, offset);  
  }
  
  
  public void glDeleteBuffers(int n, int[] ids, int offset) {
    gl.glDeleteBuffers(n, ids, offset);  
  }
  
  
  public void glBindBuffer(int target, int id) {
    gl.glBindBuffer(target, id);
  }
  
  
  public void glBufferData(int target, int size, Buffer data, int usage) {
    gl.glBufferData(target, size, data, usage);
  }

  
  public void glBufferSubData(int target, int offset, int size, Buffer data) {
    gl.glBufferSubData(target, offset, size, data);
  }
  
  
  public void glDrawArrays(int mode, int first, int count) {
    gl.glDrawArrays(mode, first, count);
  }
  
  
  public void glDrawElements(int mode, int count, int type, int offset) {
    gl.glDrawElements(mode, count, type, offset);
  }

  
  public void glEnableVertexAttribArray(int loc) {
    gl2.glEnableVertexAttribArray(loc);
  }
  
  
  public void glDisableVertexAttribArray(int loc) {
    gl2.glDisableVertexAttribArray(loc);
  }  
  
  
  public void glVertexAttribPointer(int loc, int size, int type, boolean normalized, int stride, int offset) {
    gl2.glVertexAttribPointer(loc, size, type, normalized, stride, offset);
  }
  
  
  public void glVertexAttribPointer(int loc, int size, int type, boolean normalized, int stride, Buffer data) {
    gl2.glVertexAttribPointer(loc, size, type, normalized, stride, data);
  }
  
  
  public ByteBuffer glMapBuffer(int target, int access) {  
    return gl2.glMapBuffer(target, access);
  }
  
  
  public ByteBuffer glMapBufferRange(int target, int offset, int length, int access) {
    if (gl2x != null) {
      return gl2x.glMapBufferRange(target, offset, length, access);
    } else {
      return null;
    }
  }
  
  
  public void glUnmapBuffer(int target) {
    gl2.glUnmapBuffer(target);
  }
  
  
  /////////////////////////////////////////////////////////////////////////////////
  
  // Framebuffers, renderbuffers  
  
  
  public void glGenFramebuffers(int n, int[] ids, int offset) {
    gl.glGenFramebuffers(n, ids, offset);    
  }
  
  
  public void glDeleteFramebuffers(int n, int[] ids, int offset) {
    gl.glDeleteFramebuffers(n, ids, offset);    
  }
  
  
  public void glGenRenderbuffers(int n, int[] ids, int offset) {
    gl.glGenRenderbuffers(n, ids, offset);    
  }
  
  
  public void glDeleteRenderbuffers(int n, int[] ids, int offset) {
    gl.glDeleteRenderbuffers(n, ids, offset);    
  }
  
  
  public void glBindFramebuffer(int target, int id) {
    gl.glBindFramebuffer(target, id);
  }
  
  
  public void glBlitFramebuffer(int srcX0, int srcY0, int srcX1, int srcY1, int dstX0, int dstY0, int dstX1, int dstY1, int mask, int filter) {
    if (gl2x != null) {
      gl2x.glBlitFramebuffer(srcX0, srcY0, srcX1, srcY1, dstX0, dstY0, dstX1, dstY1, mask, filter);
    }
  }
  
  
  public void glFramebufferTexture2D(int target, int attachment, int texTarget, int texId, int level) {   
    gl.glFramebufferTexture2D(target, attachment, texTarget, texId, level);
  }

  
  public void glBindRenderbuffer(int target, int id) {
    gl.glBindRenderbuffer(target, id);
  }
  
  
  public void glRenderbufferStorageMultisample(int target, int samples, int format, int width, int height) {
    if (gl2x != null) {
      gl2x.glRenderbufferStorageMultisample(target, samples, format, width, height);
    }
  }
  
  
  public void glRenderbufferStorage(int target, int format, int width, int height) {
    gl.glRenderbufferStorage(target, format, width, height);
  }
  
  
  public void glFramebufferRenderbuffer(int target, int attachment, int rendbufTarget, int rendbufId) {
    gl.glFramebufferRenderbuffer(target, attachment, rendbufTarget, rendbufId);
  }  

  
  public int glCheckFramebufferStatus(int target) {
    return gl.glCheckFramebufferStatus(target);
  }  

  
  /////////////////////////////////////////////////////////////////////////////////
  
  // Shaders  
  
  
  public int glCreateProgram() {
    return gl2.glCreateProgram();    
  }
  
  
  public void glDeleteProgram(int id) {
    gl2.glDeleteProgram(id);  
  }
  
  
  public int glCreateShader(int type) {
    return gl2.glCreateShader(type);    
  }
  
  
  public void glDeleteShader(int id) {
    gl2.glDeleteShader(id);    
  }
  
  
  public void glLinkProgram(int prog) {
    gl2.glLinkProgram(prog);  
  }
  
  
  public void glValidateProgram(int prog) {
    gl2.glValidateProgram(prog);
  }
  
  
  public void glUseProgram(int prog) {
    gl2.glUseProgram(prog);  
  }
  
  
  public int glGetAttribLocation(int prog, String name) {
    return gl2.glGetAttribLocation(prog, name);
  }
  
  
  public int glGetUniformLocation(int prog, String name) {
    return gl2.glGetUniformLocation(prog, name);
  }  
  
  
  public void glUniform1i(int loc, int value) {
    gl2.glUniform1i(loc, value);  
  }
  
  
  public void glUniform1f(int loc, float value) {
    gl2.glUniform1f(loc, value);  
  }    
  
  
  public void glUniform2f(int loc, float value0, float value1) {
    gl2.glUniform2f(loc, value0, value1);  
  }
  
  
  public void glUniform3f(int loc, float value0, float value1, float value2) {
    gl2.glUniform3f(loc, value0, value1, value2);  
  }
  
  
  public void glUniform4f(int loc, float value0, float value1, float value2, float value3) {
    gl2.glUniform4f(loc, value0, value1, value2, value3);  
  }
  
  
  public void glUniform1fv(int loc, int count, float[] v, int offset) {
    gl2.glUniform1fv(loc, count, v, offset);
  }    

  
  public void glUniform2fv(int loc, int count, float[] v, int offset) {
    gl2.glUniform2fv(loc, count, v, offset);
  }    

  
  public void glUniform3fv(int loc, int count, float[] v, int offset) {
    gl2.glUniform3fv(loc, count, v, offset);
  }

  
  public void glUniform4fv(int loc, int count, float[] v, int offset) {
    gl2.glUniform4fv(loc, count, v, offset);
  }  
  
  
  public void glUniformMatrix2fv(int loc, int count, boolean transpose, float[] mat, int offset) {
    gl2.glUniformMatrix2fv(loc, count, transpose, mat, offset);
  }
  
  
  public void glUniformMatrix3fv(int loc, int count, boolean transpose, float[] mat, int offset) {
    gl2.glUniformMatrix3fv(loc, count, transpose, mat, offset);
  }
  
  
  public void glUniformMatrix4fv(int loc, int count, boolean transpose, float[] mat, int offset) {
    gl2.glUniformMatrix4fv(loc, count, transpose, mat, offset);      
  }
  
  
  public void glVertexAttrib1f(int loc, float value) {
    gl2.glVertexAttrib1f(loc, value);  
  }
  
  
  public void glVertexAttrib2f(int loc, float value0, float value1) {
    gl2.glVertexAttrib2f(loc, value0, value1);  
  }  
  
  
  public void glVertexAttrib3f(int loc, float value0, float value1, float value2) {
    gl2.glVertexAttrib3f(loc, value0, value1, value2);  
  }    

  
  public void glVertexAttrib4f(int loc, float value0, float value1, float value2, float value3) {
    gl2.glVertexAttrib4f(loc, value0, value1, value2, value3);  
  }
  
  
  public void glShaderSource(int id, String source) {
    gl2.glShaderSource(id, 1, new String[] { source }, (int[]) null, 0);  
  }
  
  
  public void glCompileShader(int id) {
    gl2.glCompileShader(id);    
  }
  
  
  public void glAttachShader(int prog, int shader) {
    gl2.glAttachShader(prog, shader);  
  }
  
  
  public void glGetShaderiv(int shader, int pname, int[] params, int offset) {
    gl2.glGetShaderiv(shader, pname, params, offset);  
  }
  
  
  public String glGetShaderInfoLog(int shader) {
    int[] val = { 0 };
    gl2.glGetShaderiv(shader, GL2.GL_INFO_LOG_LENGTH, val, 0);
    int length = val[0];
    
    byte[] log = new byte[length];
    gl2.glGetShaderInfoLog(shader, length, val, 0, log, 0);
    return new String(log);
  }
  
  
  public void glGetProgramiv(int prog, int pname, int[] params, int offset) {
    gl2.glGetProgramiv(prog, pname, params, offset);  
  }
  
  
  public String glGetProgramInfoLog(int prog) {
    int[] val = { 0 };
    gl2.glGetShaderiv(prog, GL2.GL_INFO_LOG_LENGTH, val, 0);
    int length = val[0];
    
    byte[] log = new byte[length];
    gl2.glGetProgramInfoLog(prog, length, val, 0, log, 0);
    return new String(log);
  }    

  
  /////////////////////////////////////////////////////////////////////////////////
  
  // Viewport
  
      
  public void glViewport(int x, int y, int width, int height) {
    gl.glViewport(x, y, width, height);
  }

  
  /////////////////////////////////////////////////////////////////////////////////
  
  // Clipping (scissor test)
  
  
  public void glScissor(int x, int y, int w, int h) {
    gl.glScissor(x, y, w, h);
  }    
  
  
  /////////////////////////////////////////////////////////////////////////////////
  
  // Blending  
    
    
  public void glBlendEquation(int eq) {
    gl.glBlendEquation(eq);
  }
  

  public void glBlendFunc(int srcFactor, int dstFactor) {
    gl.glBlendFunc(srcFactor, dstFactor);
  }

  
  /////////////////////////////////////////////////////////////////////////////////
  
  // Pixels  
  
  
  public void glReadBuffer(int buf) {
    if (gl2x != null) {
      gl2x.glReadBuffer(buf);
    }
  }
  
  
  public void glReadPixels(int x, int y, int width, int height, int format, int type, Buffer buffer) {
    gl.glReadPixels(x, y, width, height, format, type, buffer);
  } 

  
  public void glDrawBuffer(int buf) {
    if (gl2x != null) {
      gl2x.glDrawBuffer(buf);
    }
  }
  
  
  public void glClearColor(float r, float g, float b, float a) {
    gl.glClearColor(r, g, b, a);    
  }
  
  
  public void glClear(int mask) {
    gl.glClear(mask);
  }  
  
  
  /////////////////////////////////////////////////////////////////////////////////
  
  // Context interface  
  
  
  public Context getContext() {
    return new Context(context);
  }
  
  public class Context {
    protected GLContext context;
    
    Context(GLContext context) {
      this.context = context;
    }
    
    boolean same(GLContext context) {
      return this.context.hashCode() == context.hashCode();  
    }
  }  
  
  
  /////////////////////////////////////////////////////////////////////////////////
  
  // Tessellator interface
  
  
  public Tessellator createTessellator(TessellatorCallback callback) {
    return new Tessellator(callback);
  }
  
  
  public class Tessellator {
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
    return other != null && other.same(context);
  }
  
  
  static public int makeIndex(int intIdx) {
    return intIdx;
  }  
  
  
  public void enableTexturing(int target) {
    glEnable(target);
  }

  
  public void disableTexturing(int target) {
    glDisable(target);
  }   
  
  
  public void initTexture(int target, int format, int width, int height) {
    int[] texels = new int[width * height];
    glTexSubImage2D(target, 0, 0, 0, width, height, format, GL_UNSIGNED_BYTE, IntBuffer.wrap(texels));
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
    if (depthBuffer == null) {
      depthBuffer = FloatBuffer.allocate(1);      
    }
    depthBuffer.rewind();
    glReadPixels(scrX, pg.height - scrY - 1, 1, 1, GL_DEPTH_COMPONENT, GL_FLOAT, depthBuffer);
    return depthBuffer.get(0);
  }
  
  
  public byte getStencilValue(int scrX, int scrY) {
    if (stencilBuffer == null) {
      stencilBuffer = ByteBuffer.allocate(1);      
    }    
    glReadPixels(scrX, pg.height - scrY - 1, 1, 1, GL_STENCIL_INDEX, GL.GL_UNSIGNED_BYTE, stencilBuffer);
    return stencilBuffer.get(0);
  }
  
  
  // bit shifting this might be more efficient
  static public int nextPowerOfTwo(int val) {
    int ret = 1;
    while (ret < val) {
      ret <<= 1;
    }
    return ret;
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
  
  
  ///////////////////////////////////////////////////////////////////////////////////
  
  // Java specific stuff  
  
  
  protected class PGLListener implements GLEventListener {
    @Override
    public void display(GLAutoDrawable adrawable) {
      drawable = adrawable;
      context = adrawable.getContext();
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
    }

    @Override
    public void reshape(GLAutoDrawable adrawable, int x, int y, int w, int h) {
      drawable = adrawable;
      context = adrawable.getContext();
    }        
  }
  
  
  /** Animator subclass to drive render loop when using NEWT. 
   **/
  protected static class NEWTAnimator extends AnimatorBase {
    private static int count = 0;
    private Timer timer = null;
    private TimerTask task = null;
    private volatile boolean shouldRun;

    protected String getBaseName(String prefix) {
      return prefix + "PGLAnimator" ;
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
        public void run() {
          if (firstRun) {
            Thread.currentThread().setName("NEWT-RenderQueue-" + count);
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
