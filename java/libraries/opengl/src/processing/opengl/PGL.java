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

import java.awt.Insets;
import java.nio.Buffer;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Timer;
import java.util.TimerTask;

import javax.media.nativewindow.GraphicsConfigurationFactory;
import javax.media.nativewindow.NativeWindow;
import javax.media.nativewindow.NativeWindowFactory;
import javax.media.nativewindow.awt.AWTGraphicsConfiguration;
import javax.media.nativewindow.awt.AWTGraphicsDevice;
import javax.media.nativewindow.awt.AWTGraphicsScreen;
import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
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
  
  /** Maximum number of tessellated vertices, using 2^20 for Mac/PC. */
  public static final int MAX_TESS_VERTICES = 524288;
  
  /** Maximum number of indices. 2 times the max number of 
   * vertices to have good room for vertex reuse. */
  public static final int MAX_TESS_INDICES  = 2 * MAX_TESS_VERTICES;  

  /** Maximum dimension of a texture used to hold font data. **/
  public static final int MAX_FONT_TEX_SIZE = 256;
  
  ///////////////////////////////////////////////////////////////////////////////////
  
  // OpenGL constants
  
  public static final int GL_LESS   = GL.GL_LESS;
  public static final int GL_LEQUAL = GL.GL_LEQUAL;
  public static final int GL_CCW    = GL.GL_CCW;
  public static final int GL_CW     = GL.GL_CW;  
  public static final int GL_FRONT  = GL.GL_FRONT;
  public static final int GL_BACK   = GL.GL_BACK;
  
  public static final int GL_VIEWPORT = GL.GL_VIEWPORT;
  
  public static final int GL_SCISSOR_TEST = GL.GL_SCISSOR_TEST;  
  public static final int GL_DEPTH_TEST   = GL.GL_DEPTH_TEST;
  
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
  
  public static final int GL_RGBA8 = -1;  
  public static final int GL_DEPTH24_STENCIL8 = -1;
  
  public static final int GL_DEPTH_COMPONENT16 = GL.GL_DEPTH_COMPONENT16;
  public static final int GL_DEPTH_COMPONENT24 = GL.GL_DEPTH_COMPONENT24;
  public static final int GL_DEPTH_COMPONENT32 = GL.GL_DEPTH_COMPONENT32;    
  
  public static final int GL_STENCIL_INDEX1 = GL.GL_STENCIL_INDEX1;
  public static final int GL_STENCIL_INDEX4 = GL.GL_STENCIL_INDEX4; 
  public static final int GL_STENCIL_INDEX8 = GL.GL_STENCIL_INDEX8;   
  
  public static final int GL_ARRAY_BUFFER         = GL.GL_ARRAY_BUFFER;
  public static final int GL_ELEMENT_ARRAY_BUFFER = GL.GL_ELEMENT_ARRAY_BUFFER;
    
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
  public static final int GL_ALIASED_LINE_WIDTH_RANGE = GL.GL_ALIASED_LINE_WIDTH_RANGE;
  public static final int GL_ALIASED_POINT_SIZE_RANGE = GL.GL_ALIASED_POINT_SIZE_RANGE;
  public static final int GL_SAMPLES                  = GL.GL_SAMPLES;

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
  
  public static final int GL_FRAMEBUFFER       = GL.GL_FRAMEBUFFER;
  public static final int GL_COLOR_ATTACHMENT0 = GL.GL_COLOR_ATTACHMENT0;
  public static final int GL_RENDERBUFFER       = GL.GL_RENDERBUFFER;
  public static final int GL_DEPTH_ATTACHMENT   = GL.GL_DEPTH_ATTACHMENT;
  public static final int GL_STENCIL_ATTACHMENT = GL.GL_STENCIL_ATTACHMENT;  
  public static final int GL_READ_FRAMEBUFFER  = GL2.GL_READ_FRAMEBUFFER;
  public static final int GL_DRAW_FRAMEBUFFER  = GL2.GL_DRAW_FRAMEBUFFER ;   
  public static final int GL_COLOR_ATTACHMENT1 = GL2.GL_COLOR_ATTACHMENT1;
  public static final int GL_COLOR_ATTACHMENT2 = GL2.GL_COLOR_ATTACHMENT2;
  public static final int GL_COLOR_ATTACHMENT3 = GL2.GL_COLOR_ATTACHMENT3;  
  
  public static final int GL_VERTEX_SHADER   = GL2.GL_VERTEX_SHADER;
  public static final int GL_FRAGMENT_SHADER = GL2.GL_FRAGMENT_SHADER;
  
  public static final int GL_MULTISAMPLE    = GL.GL_MULTISAMPLE;  
  public static final int GL_POINT_SMOOTH   = GL2.GL_POINT_SMOOTH;      
  public static final int GL_LINE_SMOOTH    = GL.GL_LINE_SMOOTH;    
  public static final int GL_POLYGON_SMOOTH = GL2.GL_POLYGON_SMOOTH;  
  
  /** Basic GL functionality, common to all profiles */
  public GL gl;
  
  /** GL2 functionality (shaders, etc) */
  public GL2 gl2;
    
  /** GLU interface **/
  public GLU glu;
  
  /** Selected GL profile */
  public GLProfile profile;
  
  /** The capabilities of the OpenGL rendering surface */
  public GLCapabilities capabilities;  
  
  /** The rendering surface */
  public GLDrawable drawable;   
  
  /** The rendering context (holds rendering state info) */
  public GLContext context;
  
  public GLCanvas awtCanvas;
  public NewtCanvasAWT newtCanvas;
  public GLWindow window;
  protected PGLAnimator animator;
  
  // Some testing parameters
  boolean useNewtCanvas = true;
  boolean addCanvasToFrame = true;
  boolean printThreadInfo = false;
  
  public PGraphicsOpenGL pg;
  
  public boolean initialized;
  
  ///////////////////////////////////////////////////////////////////////////////////
  
  // Intialization, finalization  
  
  public PGL(PGraphicsOpenGL pg) {
    this.pg = pg;
    glu = new GLU();
    initialized = false;
  }
  
  /**
   * This static method can be called by applications that use
   * Processing+P3D inside their own GUI, so they can initialize
   * JOGL2 before anything else.
   * According to the JOGL2 documentation, applications shall call 
   * GLProfile.initSingleton() ASAP, before any other UI invocation.
   * In case applications are able to initialize JOGL before any other 
   * UI action, hey shall invoke this method with beforeUI=true and 
   * benefit from fast native multithreading support on all platforms 
   * if possible. 
   *
   */  
//  static public void startup(boolean beforeUI) {
//    try {
//      GLProfile.initSingleton(beforeUI);
//    } catch (Exception e) {
//      e.printStackTrace();
//    }      
//  }
  
  static public void shutdown() {
   // GLProfile.shutdown();    
  }
  
//  public void updatePrimary() {
//    gl = context.getGL();
//    gl2 = gl.getGL2();
//  }
  
  public void updateOffscreen(PGL primary) {
    gl  = primary.gl;
    gl2 = primary.gl2;        
  }
  
  
  public void initPrimarySurface(int antialias) {
    if (printThreadInfo) System.out.println("Current thread at PGL.initPrimarySurface(): " + Thread.currentThread());
    
    // For the time being we use the fixed function profile
    // because GL3 is not supported in MacOSX Snow Leopard.
    profile = GLProfile.getMaxFixedFunc();
    //profile = GLProfile.getMaxProgrammable();
    
    if (profile == null) {
      pg.parent.die("Cannot get a valid OpenGL profile");
    }

    capabilities = new GLCapabilities(profile);
    if (1 < antialias) {
      capabilities.setSampleBuffers(true);
      capabilities.setNumSamples(antialias);
    } else {
      capabilities.setSampleBuffers(false);
    }
        
    AWTGraphicsScreen screen = (AWTGraphicsScreen)AWTGraphicsScreen.createDefault();
    AWTGraphicsConfiguration config = (AWTGraphicsConfiguration)GraphicsConfigurationFactory
        .getFactory(AWTGraphicsDevice.class).chooseGraphicsConfiguration(capabilities, capabilities, null, screen);
    NativeWindow natWin = NativeWindowFactory.getNativeWindow(pg.parent.frame, config);    
    
    if (useNewtCanvas) {    
      // NEWT canvas
      
      window = GLWindow.create(natWin, capabilities);    
      newtCanvas = new NewtCanvasAWT(window);
      newtCanvas.setBounds(0, 0, pg.parent.width, pg.parent.height);
      Insets insets = pg.parent.frame.getInsets();
      newtCanvas.setLocation(insets.left, insets.top);    
      
      if (addCanvasToFrame) {
        pg.parent.frame.add(newtCanvas);
        pg.parent.frame.validate();
      } else {      
        // Framerate doesn't work, invalid drawable error (same for AWT).
        pg.parent.add(newtCanvas);
        pg.parent.validate();
      }
      
      window.addGLEventListener(new PGLListener());
      animator = new PGLAnimator(window);
      animator.setThreadName("Processing-OpenGL");
      animator.start();
      
      newtCanvas.addMouseListener(pg.parent);
      newtCanvas.addMouseMotionListener(pg.parent);
      newtCanvas.addKeyListener(pg.parent);
      newtCanvas.addFocusListener(pg.parent);
    } else {
      // AWT canvas
      
      awtCanvas = new GLCanvas(capabilities);
      awtCanvas.setBounds(0, 0, pg.parent.width, pg.parent.height);
      Insets insets = pg.parent.frame.getInsets();
      awtCanvas.setLocation(insets.left, insets.top);
      
      if (addCanvasToFrame) {
        pg.parent.frame.add(awtCanvas);
        pg.parent.frame.validate();
      } else{
        // Framerate doesn't work, invalid drawable error (same for NEWT).
        pg.parent.add(awtCanvas);
        pg.parent.validate();        
      }
          
      awtCanvas.addGLEventListener(new PGLListener());    
      animator = new PGLAnimator(awtCanvas);    
      animator.setThreadName("Processing-OpenGL");
      animator.start();
      
      awtCanvas.addMouseListener(pg.parent);
      awtCanvas.addMouseMotionListener(pg.parent);
      awtCanvas.addKeyListener(pg.parent);
      awtCanvas.addFocusListener(pg.parent);    
    }
    
    initialized = true;
  }

  public void initOffscreenSurface(PGL primary) {
    context = primary.context;
    capabilities = primary.capabilities;
    drawable = null;
    initialized = true;
  }  
    
  public void updateOffscreenSurface(PGL primary) {
    context = primary.context;
    capabilities = primary.capabilities;
    drawable = null;    
  }  
  
  /**
   * Make the OpenGL rendering context current for this thread.
   */
//  protected void detainContext() {
//    try {
//      while (context.makeCurrent() == GLContext.CONTEXT_NOT_CURRENT) {
//        Thread.sleep(10);
//      }
//    } catch (InterruptedException e) {
//      e.printStackTrace();
//    }  
//  }
//  
//  /**
//   * Release the context, otherwise the AWT lock on X11 will not be released
//   */
//  public void releaseContext() {
//    context.release();
//  }  
//  
//  public void destroyContext() {
//    context.destroy();
//    context = null;    
//  }  
  
  ///////////////////////////////////////////////////////////////////////////////////
  
  // Frame rendering    
  
  /*
  public boolean initOnscreenDraw() {
    if (drawable != null) {
      // Call setRealized() after addNotify() has been called
      drawable.setRealized(pg.parent.isDisplayable());
      if (pg.parent.isDisplayable()) {
        drawable.setRealized(true);
        return true;
      } else {
        return false;  // Should have called canDraw() anyway
      }
    }
    return false;    
  }
  */
  
  public void beginOnscreenDraw(boolean clear, int frame) {
  }
  
  public void endOnscreenDraw(boolean clear0) {
    if (drawable != null) {
      drawable.swapBuffers();        
    }    
  }
  
  public void beginOffscreenDraw(boolean clear, int frame) {
  }
  
  public void endOffscreenDraw(boolean clear0) {    
  }
  
  public boolean canDraw() {
    return pg.parent.isDisplayable();    
  }
  
  public void requestDraw() {    
//    if (canvas != null) {
//      canvas.display();
//    }
//    if (window != null) {
//      window.display();
//    }

    if (animator != null) {
      animator.requestRender();
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
  
  
  public ByteBuffer glMapBuffer(int target, int access) {  
    return gl2.glMapBuffer(target, access);
  }
  
  public ByteBuffer glMapBufferRange(int target, int offset, int length, int access) {
    return gl2.glMapBufferRange(target, offset, length, access);
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
    gl2.glBlitFramebuffer(srcX0, srcY0, srcX1, srcY1, dstX0, dstY0, dstX1, dstY1, mask, filter);    
  }
  
  public void glFramebufferTexture2D(int target, int attachment, int texTarget, int texId, int level) {   
    gl.glFramebufferTexture2D(target, attachment, texTarget, texId, level);
  }

  public void glBindRenderbuffer(int target, int id) {
    gl.glBindRenderbuffer(target, id);
  }
    
  public void glRenderbufferStorageMultisample(int target, int samples, int format, int width, int height) {
    gl2.glRenderbufferStorageMultisample(target, samples, format, width, height);
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
  
  public void setReadBuffer(int buf) {
    gl2.glReadBuffer(buf);
  }
  
  public void glReadPixels(int x, int y, int width, int height, int format, int type, Buffer buffer) {
    gl.glReadPixels(x, y, width, height, format, type, buffer);
  } 
  
  public void setDrawBuffer(int buf) {
    gl2.glDrawBuffer(buf);
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
    return other.same(context);
  }
  
  static public int makeIndex(int intIdx) {
    return intIdx;
  }  
  
  public void enableTexturing(int target) {
    gl.glEnable(target);
  }

  public void disableTexturing(int target) {
    gl.glDisable(target);
  }   
  
  public void initTexture(int target, int width, int height, int format, int type) {
    int[] texels = new int[width * height];
    gl.glTexSubImage2D(target, 0, 0, 0, width, height, format, type, IntBuffer.wrap(texels));
  }
  
  public String getShaderLog(int id) {
    IntBuffer val = IntBuffer.allocate(1);
    gl2.glGetObjectParameterivARB(id, GL2.GL_OBJECT_INFO_LOG_LENGTH_ARB, val);
    
    int length = val.get();

    if (length <= 1) {
      return ""; 
    }

    // Some error occurred...
    ByteBuffer infoLog = ByteBuffer.allocate(length);
    val.flip();
    
    gl2.glGetInfoLogARB(id, length, val, infoLog);
        
    byte[] infoBytes = new byte[length];
    infoLog.get(infoBytes);
    return new String(infoBytes);
  }

  /////////////////////////////////////////////////////////////////////////////////
  
  // Java specific stuff     
  
  class PGLListener implements GLEventListener {      
    @Override
    public void display(GLAutoDrawable drawable) {
      if (printThreadInfo) System.out.println("Current thread at PGLListener.display(): " + Thread.currentThread());
      context = drawable.getContext();
      gl = context.getGL();
      gl2 = gl.getGL2();      
      pg.parent.handleDraw();      
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
    }

    @Override
    public void init(GLAutoDrawable drawable) {
      context = drawable.getContext();
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int w, int h) {
      context = drawable.getContext();
    }    
  }
  
  /** An Animator subclass which renders one frame at the time
   *  upon calls to the requestRender() method. */
  public class PGLAnimator extends AnimatorBase {    
      private Timer timer = null;
      private TimerTask task = null;
      private String threadName = null;
      private volatile boolean shouldRun;

      protected String getBaseName(String prefix) {
          return "Custom" + prefix + "Animator" ;
      }

      /** Creates an CustomAnimator with an initial drawable to 
       * animate. */
      public PGLAnimator(GLAutoDrawable drawable) {
          if (drawable != null) {
              add(drawable);
          }
      }
      
      public void setThreadName(String name) {
        threadName = name;
      }

      public synchronized void requestRender() {
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
                    if (threadName != null) Thread.currentThread().setName(threadName);
                    firstRun = false;
                  }
                  if(PGLAnimator.this.shouldRun) {
                     PGLAnimator.this.animThread = Thread.currentThread();
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
