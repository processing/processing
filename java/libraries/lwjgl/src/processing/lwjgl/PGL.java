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

package processing.lwjgl;

import java.awt.Canvas;
import java.nio.Buffer;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.ARBShaderObjects;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.util.glu.GLU;
import org.lwjgl.util.glu.GLUtessellator;
import org.lwjgl.util.glu.GLUtessellatorCallbackAdapter;
import org.lwjgl.opengl.GLContext;
import org.lwjgl.opengl.PixelFormat;

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
  static final int INDEX_TYPE = GL11.GL_UNSIGNED_INT;

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
  
  public static final int GL_LESS   = GL11.GL_LESS;
  public static final int GL_LEQUAL = GL11.GL_LEQUAL;
  public static final int GL_CCW    = GL11.GL_CCW;
  public static final int GL_CW     = GL11.GL_CW;  
  public static final int GL_FRONT  = GL11.GL_FRONT;
  public static final int GL_BACK   = GL11.GL_BACK;
  
  public static final int GL_VIEWPORT = GL11.GL_VIEWPORT;
  
  public static final int GL_SCISSOR_TEST = GL11.GL_SCISSOR_TEST;  
  public static final int GL_DEPTH_TEST   = GL11.GL_DEPTH_TEST;
  
  public static final int GL_COLOR_BUFFER_BIT   = GL11.GL_COLOR_BUFFER_BIT; 
  public static final int GL_DEPTH_BUFFER_BIT   = GL11.GL_DEPTH_BUFFER_BIT; 
  public static final int GL_STENCIL_BUFFER_BIT = GL11.GL_STENCIL_BUFFER_BIT;
  
  public static final int GL_FUNC_ADD              = 0x8006;
  public static final int GL_FUNC_MIN              = 0x8007;
  public static final int GL_FUNC_MAX              = 0x8008;
  public static final int GL_FUNC_REVERSE_SUBTRACT = 0x800b;
  
  public static final int GL_TEXTURE_2D     = GL11.GL_TEXTURE_2D;
  public static final int GL_RGB            = GL11.GL_RGB;
  public static final int GL_RGBA           = GL11.GL_RGBA;
  public static final int GL_ALPHA          = GL11.GL_ALPHA;
  public static final int GL_UNSIGNED_INT   = GL11.GL_UNSIGNED_INT;
  public static final int GL_UNSIGNED_BYTE  = GL11.GL_UNSIGNED_BYTE;
  public static final int GL_UNSIGNED_SHORT = GL11.GL_UNSIGNED_SHORT;
  public static final int GL_FLOAT          = GL11.GL_FLOAT;
  
  public static final int GL_NEAREST              = GL11.GL_NEAREST;
  public static final int GL_LINEAR               = GL11.GL_LINEAR;
  public static final int GL_LINEAR_MIPMAP_LINEAR = GL11.GL_LINEAR_MIPMAP_LINEAR;
  
  public static final int GL_CLAMP_TO_EDGE = GL12.GL_CLAMP_TO_EDGE;
  public static final int GL_REPEAT        = GL11.GL_REPEAT;
  
  public static final int GL_RGBA8 = -1;  
  public static final int GL_DEPTH24_STENCIL8 = -1;
  
  public static final int GL_DEPTH_COMPONENT16 = GL14.GL_DEPTH_COMPONENT16;
  public static final int GL_DEPTH_COMPONENT24 = GL14.GL_DEPTH_COMPONENT24;
  public static final int GL_DEPTH_COMPONENT32 = GL14.GL_DEPTH_COMPONENT32;    
  
  
  public static final int GL_STENCIL_INDEX1 = GL30.GL_STENCIL_INDEX1;
  public static final int GL_STENCIL_INDEX4 = GL30.GL_STENCIL_INDEX4; 
  public static final int GL_STENCIL_INDEX8 = GL30.GL_STENCIL_INDEX8;   
  
  public static final int GL_ARRAY_BUFFER         = GL15.GL_ARRAY_BUFFER;
  public static final int GL_ELEMENT_ARRAY_BUFFER = GL15.GL_ELEMENT_ARRAY_BUFFER;
    
  public static final int GL_FRAMEBUFFER_COMPLETE                      = GL30.GL_FRAMEBUFFER_COMPLETE;    
  public static final int GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT         = GL30.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT;
  public static final int GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT = GL30.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT;
  public static final int GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS         = 0x8cd9;      
  public static final int GL_FRAMEBUFFER_INCOMPLETE_FORMATS            = 0x8cda;  
  public static final int GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER        = GL30.GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER;
  public static final int GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER        = GL30.GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER;  
  public static final int GL_FRAMEBUFFER_UNSUPPORTED                   = GL30.GL_FRAMEBUFFER_UNSUPPORTED;
    
  public static final int GL_STATIC_DRAW  = GL15.GL_STATIC_DRAW;
  public static final int GL_DYNAMIC_DRAW = GL15.GL_DYNAMIC_DRAW;
  public static final int GL_STREAM_DRAW  = GL15.GL_STREAM_DRAW;
  
  public static final int GL_READ_ONLY  = GL15.GL_READ_ONLY;
  public static final int GL_WRITE_ONLY = GL15.GL_WRITE_ONLY;  
  public static final int GL_READ_WRITE = GL15.GL_READ_WRITE;
    
  public static final int GL_TRIANGLE_FAN   = GL11.GL_TRIANGLE_FAN;
  public static final int GL_TRIANGLE_STRIP = GL11.GL_TRIANGLE_STRIP;
  public static final int GL_TRIANGLES      = GL11.GL_TRIANGLES;  
  
  public static final int GL_VENDOR     = GL11.GL_VENDOR;
  public static final int GL_RENDERER   = GL11.GL_RENDERER;
  public static final int GL_VERSION    = GL11.GL_VERSION;
  public static final int GL_EXTENSIONS = GL11.GL_EXTENSIONS;
    
  public static final int GL_MAX_TEXTURE_SIZE         = GL11.GL_MAX_TEXTURE_SIZE;
  public static final int GL_ALIASED_LINE_WIDTH_RANGE = GL12.GL_ALIASED_LINE_WIDTH_RANGE;
  public static final int GL_ALIASED_POINT_SIZE_RANGE = GL12.GL_ALIASED_POINT_SIZE_RANGE;
  public static final int GL_SAMPLES                  = GL13.GL_SAMPLES;

  public static final int GLU_TESS_WINDING_NONZERO = GLU.GLU_TESS_WINDING_NONZERO;
  public static final int GLU_TESS_WINDING_ODD     = GLU.GLU_TESS_WINDING_ODD;  
    
  public static final int GL_TEXTURE0           = GL13.GL_TEXTURE0;
  public static final int GL_TEXTURE1           = GL13.GL_TEXTURE1;
  public static final int GL_TEXTURE2           = GL13.GL_TEXTURE2;
  public static final int GL_TEXTURE3           = GL13.GL_TEXTURE3;
  public static final int GL_TEXTURE_MIN_FILTER = GL11.GL_TEXTURE_MIN_FILTER;
  public static final int GL_TEXTURE_MAG_FILTER = GL11.GL_TEXTURE_MAG_FILTER;
  public static final int GL_TEXTURE_WRAP_S     = GL11.GL_TEXTURE_WRAP_S;
  public static final int GL_TEXTURE_WRAP_T     = GL11.GL_TEXTURE_WRAP_T;  
  
  public static final int GL_BLEND               = GL11.GL_BLEND;
  public static final int GL_ONE                 = GL11.GL_ONE; 
  public static final int GL_ZERO                = GL11.GL_ZERO;
  public static final int GL_SRC_ALPHA           = GL11.GL_SRC_ALPHA; 
  public static final int GL_DST_ALPHA           = GL11.GL_DST_ALPHA;
  public static final int GL_ONE_MINUS_SRC_ALPHA = GL11.GL_ONE_MINUS_SRC_ALPHA;
  public static final int GL_ONE_MINUS_DST_COLOR = GL11.GL_ONE_MINUS_DST_COLOR;
  public static final int GL_ONE_MINUS_SRC_COLOR = GL11.GL_ONE_MINUS_SRC_COLOR;
  public static final int GL_DST_COLOR           = GL11.GL_DST_COLOR;
  public static final int GL_SRC_COLOR           = GL11.GL_SRC_COLOR;
  
  public static final int GL_FRAMEBUFFER        = GL30.GL_FRAMEBUFFER;
  public static final int GL_COLOR_ATTACHMENT0  = GL30.GL_COLOR_ATTACHMENT0;
  public static final int GL_RENDERBUFFER       = GL30.GL_RENDERBUFFER;
  public static final int GL_DEPTH_ATTACHMENT   = GL30.GL_DEPTH_ATTACHMENT;
  public static final int GL_STENCIL_ATTACHMENT = GL30.GL_STENCIL_ATTACHMENT;  
  public static final int GL_READ_FRAMEBUFFER   = GL30.GL_READ_FRAMEBUFFER;
  public static final int GL_DRAW_FRAMEBUFFER   = GL30.GL_DRAW_FRAMEBUFFER;   
  public static final int GL_COLOR_ATTACHMENT1  = GL30.GL_COLOR_ATTACHMENT1;
  public static final int GL_COLOR_ATTACHMENT2  = GL30.GL_COLOR_ATTACHMENT2;
  public static final int GL_COLOR_ATTACHMENT3  = GL30.GL_COLOR_ATTACHMENT3;  
  
  public static final int GL_VERTEX_SHADER   = GL20.GL_VERTEX_SHADER;
  public static final int GL_FRAGMENT_SHADER = GL20.GL_FRAGMENT_SHADER;
  
  public static final int GL_MULTISAMPLE    = GL13.GL_MULTISAMPLE;  
  public static final int GL_POINT_SMOOTH   = GL11.GL_POINT_SMOOTH;      
  public static final int GL_LINE_SMOOTH    = GL11.GL_LINE_SMOOTH;    
  public static final int GL_POLYGON_SMOOTH = GL11.GL_POLYGON_SMOOTH;  
  
  public Canvas canvas;
  
  public PGraphicsLWJGL pg;
  
  public GLU glu;
  
  public boolean initialized;
  
  ///////////////////////////////////////////////////////////////////////////////////
  
  // Intialization, finalization  
  
  public PGL(PGraphicsLWJGL pg) {
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
  static public void startup(boolean beforeUI) {
//    try {
//      GLProfile.initSingleton(beforeUI);
//    } catch (Exception e) {
//      e.printStackTrace();
//    }      
  }
  
  static public void shutdown() {
//    GLProfile.shutdown();    
  }
  
  public void updatePrimary() {
//    gl = context.getGL();
//    gl2 = gl.getGL2();
  }
  
  public void updateOffscreen(PGL primary) {
//    gl  = primary.gl;
//    gl2 = primary.gl2;        
  }
  
  
  public void initPrimarySurface(int antialias) {
//    if (pg.parent.online) {
//      // RCP Application (Applet's, Webstart, Netbeans, ..) using JOGL may not 
//      // be able to initialize JOGL before the first UI action, so initSingleton()
//      // is called with its argument set to false.
//      GLProfile.initSingleton(false);
//    } else {
//      if (PApplet.platform == PConstants.LINUX) {
//        // Special case for Linux, since the multithreading issues described for
//        // example here:
//        // http://forum.jogamp.org/QtJambi-JOGL-Ubuntu-Lucid-td909554.html
//        // have not been solved yet (at least for stable release b32 of JOGL2).
//        GLProfile.initSingleton(false);
//      } else { 
//        GLProfile.initSingleton(true);
//      }
//    }
    
    canvas = new Canvas();
    pg.parent.add(canvas);
    
    canvas.setBounds(0, 0, pg.parent.width, pg.parent.height);       
    canvas.setFocusable(true);
    canvas.requestFocus();
    canvas.setIgnoreRepaint(true);      
    
    try {
      Display.setParent(canvas);
      PixelFormat format = new PixelFormat(32, 0, 24, 8, antialias);
      Display.create(format);            
      Display.setVSyncEnabled(false);      
    } catch (LWJGLException e) {
      e.printStackTrace();
    }    
    
    initialized = true;
  }
  
  public void initOffscreenSurface(PGL primary) {
//    context = primary.context;
//    capabilities = primary.capabilities;
//    drawable = null;
    initialized = true;
  }  
    
  public void updateOffscreenSurface(PGL primary) {
//    context = primary.context;
//    capabilities = primary.capabilities;
//    drawable = null;    
  }  
  
  /**
   * Make the OpenGL rendering context current for this thread.
   */
  protected void detainContext() {
//    try {
//      while (context.makeCurrent() == GLContext.CONTEXT_NOT_CURRENT) {
//        Thread.sleep(10);
//      }
//    } catch (InterruptedException e) {
//      e.printStackTrace();
//    }  
  }
  
  /**
   * Release the context, otherwise the AWT lock on X11 will not be released
   */
  public void releaseContext() {
//    context.release();
  }  
  
  public void destroyContext() {
//    context.destroy();
//    context = null;    
  }  
  
  ///////////////////////////////////////////////////////////////////////////////////
  
  // Frame rendering    
  
  public boolean initOnscreenDraw() {
//    if (drawable != null) {
//      // Call setRealized() after addNotify() has been called
//      drawable.setRealized(pg.parent.isDisplayable());
//      if (pg.parent.isDisplayable()) {
//        drawable.setRealized(true);
//        return true;
//      } else {
//        return false;  // Should have called canDraw() anyway
//      }
//    }
//    return false; 
    return true;
  }
  
  public void beginOnscreenDraw(boolean clear, int frame) {
  }
  
  public void endOnscreenDraw(boolean clear0) {
    Display.update();
//    if (drawable != null) {
//      drawable.swapBuffers();        
//    }    
  }
  
  public void beginOffscreenDraw(boolean clear, int frame) {
  }
  
  public void endOffscreenDraw(boolean clear0) {    
  }
  
  public boolean canDraw() {
    return pg.parent.isDisplayable();    
  }
  
  public void requestDraw() {        
  }
  
  ///////////////////////////////////////////////////////////////////////////////////
  
  // Caps query

  public String glGetString(int name) {
    return GL11.glGetString(name);
  }
 
  public void glGetIntegerv(int name, IntBuffer values) {
    GL11.glGetInteger(name, values);  
  }
  
  ///////////////////////////////////////////////////////////////////////////////////
  
  // Enable/disable caps
  
  public void glEnable(int cap) {
    if (-1 < cap) {
      GL11.glEnable(cap);
    }
  }  
  
  public void glDisable(int cap) {
    if (-1 < cap) {
      GL11.glDisable(cap);
    }
  }  
  
  ///////////////////////////////////////////////////////////////////////////////////
  
  // Render control 
  
  public void glFlush() {
    GL11.glFlush();
  }  
  
  public void glFinish() {
    GL11.glFinish();
  }
  
  /////////////////////////////////////////////////////////////////////////////////
  
  // Error handling  
  
  public int glGetError() {
    return GL11.glGetError();
  }

  public String glErrorString(int err) {
    return GLU.gluErrorString(err);
  }  
  
  public String gluErrorString(int err) {
    return GLU.gluErrorString(err);
  }
  
  /////////////////////////////////////////////////////////////////////////////////
  
  // Rendering options

  public void glFrontFace(int mode) {
    GL11.glFrontFace(mode);
  }
    
  public void glDepthMask(boolean flag) {
    GL11.glDepthMask(flag);   
  }
    
  public void glDepthFunc(int func) {
    GL11.glDepthFunc(func);
  }  

  
  /////////////////////////////////////////////////////////////////////////////////
  
  // Textures     
  
  public void glGenTextures(int n, IntBuffer ids) {
    ids.limit(n);
    GL11.glGenTextures(ids);      
  }

  public void glDeleteTextures(int n, IntBuffer ids) {
    ids.limit(n);
    GL11.glDeleteTextures(ids);
  }  
  
  public void glActiveTexture(int unit) {
    GL13.glActiveTexture(unit);
  }
  
  public void glBindTexture(int target, int id) {
    GL11.glBindTexture(target, id);
  }
    
  public void glTexImage2D(int target, int level, int internalFormat, int width, int height, int border, int format, int type, IntBuffer data) {
    GL11.glTexImage2D(target, level, internalFormat, width, height, border, format, type, data);
  }
  
  public void glTexSubImage2D(int target, int level, int xOffset, int yOffset, int width, int height, int format, int type, IntBuffer data) {
    GL11.glTexSubImage2D(target, level, xOffset, yOffset, width, height, format, type, data);
  }

  public void glTexParameterf(int target, int param, int value) {
    GL11.glTexParameterf(target, param, value); 
  }

  public void glGenerateMipmap(int target) {
    GL30.glGenerateMipmap(target);
  }  
  
  /////////////////////////////////////////////////////////////////////////////////
  
  // Vertex Buffers

  public void glGenBuffers(int n, IntBuffer ids) {
    ids.limit(n);
    GL15.glGenBuffers(ids);
  }
  
  public void glDeleteBuffers(int n, IntBuffer ids) {
    ids.limit(n);
    GL15.glDeleteBuffers(ids);  
  }
  
  public void glBindBuffer(int target, int id) {
    GL15.glBindBuffer(target, id);
  }

  public void glBufferData(int target, int size, Buffer data, int usage) {
    if (data == null) {      
      FloatBuffer empty = BufferUtils.createFloatBuffer(size);
      GL15.glBufferData(target, empty, usage);  
    } else {  
      if (data instanceof ByteBuffer) {
        GL15.glBufferData(target, (ByteBuffer)data, usage);    
      } else if (data instanceof IntBuffer) {
        GL15.glBufferData(target, (IntBuffer)data, usage);    
      } else if (data instanceof FloatBuffer) {
        GL15.glBufferData(target, (FloatBuffer)data, usage);    
      }
    }
  } 
  
  
  public void glBufferSubData(int target, int offset, int size, Buffer data) {   
    if (data instanceof ByteBuffer) {
      GL15.glBufferSubData(target, offset, (ByteBuffer)data);    
    } else if (data instanceof IntBuffer) {
      GL15.glBufferSubData(target, offset, (IntBuffer)data);    
    } else if (data instanceof FloatBuffer) {
      GL15.glBufferSubData(target, offset, (FloatBuffer)data);    
    }
  }  

  
  public void glDrawElements(int mode, int count, int type, int offset) {
    GL11.glDrawElements(mode, count, type, offset);
  }

  
  public void glEnableVertexAttribArray(int loc) {
    GL20.glEnableVertexAttribArray(loc);
  }
  
  public void glDisableVertexAttribArray(int loc) {
    GL20.glDisableVertexAttribArray(loc);
  }  
  
  public void glVertexAttribPointer(int loc, int size, int type, boolean normalized, int stride, int offset) {
    GL20.glVertexAttribPointer(loc, size, type, normalized, stride, offset);
  }
  
  
  public ByteBuffer glMapBuffer(int target, int access) {  
    return GL15.glMapBuffer(target, access, null);
  }
  
  public ByteBuffer glMapBufferRange(int target, int offset, int length, int access) {
    return GL30.glMapBufferRange(target, offset, length, access, null);
  }
  
  public void glUnmapBuffer(int target) {
    GL15.glUnmapBuffer(target);
  }
  
  /////////////////////////////////////////////////////////////////////////////////
  
  // Framebuffers, renderbuffers  
  
  public void glGenFramebuffers(int n, IntBuffer ids) {
    ids.limit(n);
    GL30.glGenFramebuffers(ids);
  }
  
  public void glDeleteFramebuffers(int n, IntBuffer ids) {
    ids.limit(n);
    GL30.glDeleteFramebuffers(ids);    
  }
  
  public void glGenRenderbuffers(int n, IntBuffer ids) {
    ids.limit(n);
    GL30.glGenRenderbuffers(ids);  
  }
  
  public void glDeleteRenderbuffers(int n, IntBuffer ids) {
    ids.limit(n);
    GL30.glDeleteRenderbuffers(ids);     
  }
  
  public void glBindFramebuffer(int target, int id) {
    GL30.glBindFramebuffer(target, id);
  }
  
  public void glBlitFramebuffer(int srcX0, int srcY0, int srcX1, int srcY1, int dstX0, int dstY0, int dstX1, int dstY1, int mask, int filter) {
    GL30.glBlitFramebuffer(srcX0, srcY0, srcX1, srcY1, dstX0, dstY0, dstX1, dstY1, mask, filter);    
  }
  
  public void glFramebufferTexture2D(int target, int attachment, int texTarget, int texId, int level) {   
    GL30.glFramebufferTexture2D(target, attachment, texTarget, texId, level);
  }

  public void glBindRenderbuffer(int target, int id) {
    GL30.glBindRenderbuffer(target, id);
  }
    
  public void glRenderbufferStorageMultisample(int target, int samples, int format, int width, int height) {
    GL30.glRenderbufferStorageMultisample(target, samples, format, width, height);
  }
  
  public void glRenderbufferStorage(int target, int format, int width, int height) {
    GL30.glRenderbufferStorage(target, format, width, height);
  }
  
  public void glFramebufferRenderbuffer(int target, int attachment, int rendbufTarget, int rendbufId) {
    GL30.glFramebufferRenderbuffer(target, attachment, rendbufTarget, rendbufId);
  }  

  public int glCheckFramebufferStatus(int target) {
    return GL30.glCheckFramebufferStatus(target);
  }  

  /////////////////////////////////////////////////////////////////////////////////
  
  // Shaders  
  
  public int glCreateProgram() {
    return GL20.glCreateProgram();    
  }
  
  public void glDeleteProgram(int id) {
    GL20.glDeleteProgram(id);  
  }
  
  public int glCreateShader(int type) {
    return GL20.glCreateShader(type);    
  }
  
  public void glDeleteShader(int id) {
    GL20.glDeleteShader(id);    
  }
   
  public void glLinkProgram(int prog) {
    GL20.glLinkProgram(prog);  
  }
  
  public void glValidateProgram(int prog) {
    GL20.glValidateProgram(prog);
  }
  
  public void glUseProgram(int prog) {
    GL20.glUseProgram(prog);  
  }
  
  public int glGetAttribLocation(int prog, String name) {
    return GL20.glGetAttribLocation(prog, name);
  }
  
  public int glGetUniformLocation(int prog, String name) {
    return GL20.glGetUniformLocation(prog, name);
  }  
  
  public void glUniform1i(int loc, int value) {
    GL20.glUniform1i(loc, value);  
  }
  
  public void glUniform1f(int loc, float value) {
    GL20.glUniform1f(loc, value);  
  }    
  
  public void glUniform2f(int loc, float value0, float value1) {
    GL20.glUniform2f(loc, value0, value1);  
  }
  
  public void glUniform3f(int loc, float value0, float value1, float value2) {
    GL20.glUniform3f(loc, value0, value1, value2);  
  }
  
  public void glUniform4f(int loc, float value0, float value1, float value2, float value3) {
    GL20.glUniform4f(loc, value0, value1, value2, value3);  
  }
  
  public void glUniform1fv(int loc, int count, FloatBuffer v) {
    GL20.glUniform1(loc, v);
  }    

  public void glUniform2fv(int loc, int count, FloatBuffer v) {
    GL20.glUniform2(loc, v);   
  }    

  public void glUniform3fv(int loc, int count, FloatBuffer v) {
    GL20.glUniform3(loc, v);
  }

  public void glUniform4fv(int loc, int count, FloatBuffer v) {
    GL20.glUniform4(loc, v);
  }  
  
  public void glUniformMatrix2fv(int loc, int count, boolean transpose, FloatBuffer mat) {
    GL20.glUniformMatrix2(loc, transpose, mat);    
  }
  
  public void glUniformMatrix3fv(int loc, int count, boolean transpose, FloatBuffer mat) {
    GL20.glUniformMatrix3(loc, transpose, mat);    
  }
  
  public void glUniformMatrix4fv(int loc, int count, boolean transpose, FloatBuffer mat) {
    GL20.glUniformMatrix4(loc, transpose, mat);    
  }
  
  public void glVertexAttrib1f(int loc, float value) {
    GL20.glVertexAttrib1f(loc, value);  
  }
  
  public void glVertexAttrib2f(int loc, float value0, float value1) {
    GL20.glVertexAttrib2f(loc, value0, value1);  
  }  
  
  public void glVertexAttrib3f(int loc, float value0, float value1, float value2) {
    GL20.glVertexAttrib3f(loc, value0, value1, value2);  
  }    

  public void glVertexAttrib4f(int loc, float value0, float value1, float value2, float value3) {
    GL20.glVertexAttrib4f(loc, value0, value1, value2, value3);  
  }
  
  public void glShaderSource(int id, String source) {
    GL20.glShaderSource(id, source);
  }
  
  public void glCompileShader(int id) {
    GL20.glCompileShader(id);    
  }
  
  public void glAttachShader(int prog, int shader) {
    GL20.glAttachShader(prog, shader);  
  }
  
  /////////////////////////////////////////////////////////////////////////////////
  
  // Viewport
  
      
  public void glViewport(int x, int y, int width, int height) {
    GL11.glViewport(x, y, width, height);
  }

  /////////////////////////////////////////////////////////////////////////////////
  
  // Clipping (scissor test)
  
  
  public void glScissor(int x, int y, int w, int h) {
    GL11.glScissor(x, y, w, h);
  }    
  
  
  /////////////////////////////////////////////////////////////////////////////////
  
  // Blending  
    
    
  public void glBlendEquation(int eq) {
    GL14.glBlendEquation(eq);
  }
  

  public void glBlendFunc(int srcFactor, int dstFactor) {
    GL11.glBlendFunc(srcFactor, dstFactor);
  }

  
  /////////////////////////////////////////////////////////////////////////////////
  
  // Pixels  
  
  public void setReadBuffer(int buf) {
    GL11.glReadBuffer(buf);
  }
  
  public void glReadPixels(int x, int y, int width, int height, int format, int type, IntBuffer buffer) {
    GL11.glReadPixels(x, y, width, height, format, type, buffer);
  } 
  
  public void setDrawBuffer(int buf) {
    GL11.glDrawBuffer(buf);
  }
  
  public void glClearColor(float r, float g, float b, float a) {
    GL11.glClearColor(r, g, b, a);    
  }
  
  public void glClear(int mask) {
    GL11.glClear(mask);
  }  
    
  /////////////////////////////////////////////////////////////////////////////////
  
  // Context interface  
  
  public Context getContext() {
    return new Context(null);
  }
  
  public class Context {
    protected GLContext context;
    
    Context(GLContext context) {
      this.context = context;
    }
    
    boolean same(GLContext context) {
      return true;
      //return this.context.hashCode() == context.hashCode();  
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
  
  public FloatBuffer createFloatBuffer(int size) {
    return BufferUtils.createFloatBuffer(size);
  }
  
  public IntBuffer createIntBuffer(int size) {
    return BufferUtils.createIntBuffer(size);
  }
  
  public boolean contextIsCurrent(Context other) {
    return other.same(null);
  }
  
  static public int makeIndex(int intIdx) {
    return intIdx;
  }  
  
  public void enableTexturing(int target) {
    GL11.glEnable(target);
  }

  public void disableTexturing(int target) {
    GL11.glDisable(target);
  }   
  
  public void initTexture(int target, int width, int height, int format, int type) {
    //int[] texels = new int[width * height];    
    IntBuffer texels = createIntBuffer(width * height);    
    GL11.glTexSubImage2D(target, 0, 0, 0, width, height, format, type, texels);
  }
  
  public String getShaderLog(int id) {
    IntBuffer val = createIntBuffer(1);
    ARBShaderObjects.glGetObjectParameterARB(id, ARBShaderObjects.GL_OBJECT_INFO_LOG_LENGTH_ARB, val);

    int length = val.get();

    if (length <= 1) {
      return ""; 
    }
     
    // Some error occurred...
    ByteBuffer infoLog = ByteBuffer.allocate(length);
    val.flip();
    
    ARBShaderObjects.glGetInfoLogARB(id, val, infoLog);
        
    byte[] infoBytes = new byte[length];
    infoLog.get(infoBytes);
    return new String(infoBytes);
  }    
}
