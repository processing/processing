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

package processing.core;

import java.nio.Buffer;
import java.nio.ByteBuffer;

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
  public static final int DEFAULT_VERTEX_CACHE_SIZE = 512;
  
  /** Maximum lights by default is 8, the minimum defined by OpenGL. */   
  public static final int MAX_LIGHTS = 8;
  
  /** Maximum number of tessellated vertices. GLES restricts the vertex indices
   * to be of type short, so 2^16 = 65536 is the maximum possible number of 
   * vertices that can be referred to within a single VBO. */
  public static final int MAX_TESS_VERTICES = 65536;
  
  /** Maximum number of indices. 2 times the max number of 
   * vertices to have good room for vertex reuse. */
  public static final int MAX_TESS_INDICES  = 2 * MAX_TESS_VERTICES;  

  /** Maximum dimension of a texture used to hold font data. **/
  public static final int MAX_FONT_TEX_SIZE = 256;
  
  ///////////////////////////////////////////////////////////////////////////////////
  
  // OpenGL constants
  
  public static final int GL_LESS   = GLES20.GL_LESS;
  public static final int GL_LEQUAL = GLES20.GL_LEQUAL;
  public static final int GL_CCW    = GLES20.GL_CCW;
  public static final int GL_CW     = GLES20.GL_CW;  
  public static final int GL_FRONT  = GLES20.GL_FRONT;
  public static final int GL_BACK   = GLES20.GL_BACK;
  
  public static final int GL_VIEWPORT = GLES20.GL_VIEWPORT;
  
  public static final int GL_SCISSOR_TEST = GLES20.GL_SCISSOR_TEST;  
  public static final int GL_DEPTH_TEST   = GLES20.GL_DEPTH_TEST;
  
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
  
  public static final int GL_NEAREST              = GLES20.GL_NEAREST;
  public static final int GL_LINEAR               = GLES20.GL_LINEAR;
  public static final int GL_LINEAR_MIPMAP_LINEAR = GLES20.GL_LINEAR_MIPMAP_LINEAR;
  
  public static final int GL_CLAMP_TO_EDGE = GLES20.GL_CLAMP_TO_EDGE;
  public static final int GL_REPEAT        = GLES20.GL_REPEAT;
  
  public static final int GL_RGBA8 = -1;  
  public static final int GL_DEPTH24_STENCIL8 = -1;
  
  public static final int GL_DEPTH_COMPONENT16 = GLES20.GL_DEPTH_COMPONENT16;
  public static final int GL_DEPTH_COMPONENT24 = -1;
  public static final int GL_DEPTH_COMPONENT32 = -1;    
  
  public static final int GL_STENCIL_INDEX1 = -1;
  public static final int GL_STENCIL_INDEX4 = -1; 
  public static final int GL_STENCIL_INDEX8 = GLES20.GL_STENCIL_INDEX8;   
  
  public static final int GL_ARRAY_BUFFER         = GLES20.GL_ARRAY_BUFFER;
  public static final int GL_ELEMENT_ARRAY_BUFFER = GLES20.GL_ELEMENT_ARRAY_BUFFER;
    
  public static final int GL_FRAMEBUFFER_COMPLETE                      = GLES20.GL_FRAMEBUFFER_COMPLETE;    
  public static final int GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT         = GLES20.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT;
  public static final int GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT = GLES20.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT;
  public static final int GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS         = GLES20.GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS;      
  public static final int GL_FRAMEBUFFER_INCOMPLETE_FORMATS            = -1;  
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
  
  public static final int GL_VENDOR     = GLES20.GL_VENDOR;
  public static final int GL_RENDERER   = GLES20.GL_RENDERER;
  public static final int GL_VERSION    = GLES20.GL_VERSION;
  public static final int GL_EXTENSIONS = GLES20.GL_EXTENSIONS;
    
  public static final int GL_MAX_TEXTURE_SIZE         = GLES20.GL_MAX_TEXTURE_SIZE;
  public static final int GL_ALIASED_LINE_WIDTH_RANGE = GLES20.GL_ALIASED_LINE_WIDTH_RANGE;
  public static final int GL_ALIASED_POINT_SIZE_RANGE = GLES20.GL_ALIASED_POINT_SIZE_RANGE;
  public static final int GL_SAMPLES                  = GLES20.GL_SAMPLES;

  public static final int GLU_TESS_WINDING_NONZERO = -1;
  public static final int GLU_TESS_WINDING_ODD     = -1;  
    
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
  
  public static final int GL_FRAMEBUFFER       = GLES20.GL_FRAMEBUFFER;
  public static final int GL_COLOR_ATTACHMENT0 = GLES20.GL_COLOR_ATTACHMENT0;
  public static final int GL_RENDERBUFFER       = GLES20.GL_RENDERBUFFER;
  public static final int GL_DEPTH_ATTACHMENT   = GLES20.GL_DEPTH_ATTACHMENT;
  public static final int GL_STENCIL_ATTACHMENT = GLES20.GL_STENCIL_ATTACHMENT;  
  public static final int GL_READ_FRAMEBUFFER  = -1;
  public static final int GL_DRAW_FRAMEBUFFER  = -1;   
  public static final int GL_COLOR_ATTACHMENT1 = -1;
  public static final int GL_COLOR_ATTACHMENT2 = -1;
  public static final int GL_COLOR_ATTACHMENT3 = -1;  
  
  public static final int GL_VERTEX_SHADER   = GLES20.GL_VERTEX_SHADER;
  public static final int GL_FRAGMENT_SHADER = GLES20.GL_FRAGMENT_SHADER;
  
  public static final int GL_MULTISAMPLE    = -1;  
  public static final int GL_POINT_SMOOTH   = -1;      
  public static final int GL_LINE_SMOOTH    = -1;    
  public static final int GL_POLYGON_SMOOTH = -1;  
  
  // Some EGL constants needed to initialize an GLES2 context.
  public static final int EGL_CONTEXT_CLIENT_VERSION = 0x3098;
  public static final int EGL_OPENGL_ES2_BIT         = 0x0004;
  
  /** Pipeline mode: FIXED, PROG_GL2, PROG_GL3 or PROG_GL4 */
  public int pipeline;
  
  public GL10 gl;
  public GLU glu; 

  public AndroidRenderer renderer;
  
  public PGraphicsAndroid3D pg;
  
  public boolean initialized;
    
  ///////////////////////////////////////////////////////////////////////////////////
  
  // Intialization, finalization
  
  // TODO: implement double buffering support in onscreen rendering, offscreen rendering.  
  
  public PGL(PGraphicsAndroid3D pg) {
    this.pg = pg;
    renderer = new AndroidRenderer();
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
  }
  
  static public void shutdown() {
  }  
  
  public void updatePrimary() {
    
  }

  public void updateOffscreen(PGL primary) {
    gl = primary.gl;       
//    gl11 = primary.gl11;
//    gl11x = primary.gl11x;
//    gl11xp = primary.gl11xp;
  }  
  
  
  public void initPrimarySurface(int antialias) {
    /*
    
    offscreenTexCrop = new int[4];
    offscreenTexCrop[0] = 0;
    offscreenTexCrop[1] = 0;
    offscreenTexCrop[2] = width;
    offscreenTexCrop[3] = height;      

    offscreenImages = new PImage[2];
    offscreenParams = new PTexture.Parameters[2];
    
    // Nearest filtering is used for the primary surface, otherwise some 
    // artifacts appear (diagonal line when blending, for instance). This
    // might deserve further examination.
    offscreenParams[0] = new PTexture.Parameters(ARGB, POINT);
    offscreenParams[1] = new PTexture.Parameters(ARGB, POINT);
    offscreenImages[0] = parent.createImage(width, height, ARGB, offscreenParams[0]);
    offscreenImages[1] = parent.createImage(width, height, ARGB, offscreenParams[1]);          
    
    offscreenTextures = new PTexture[2];
    offscreenTextures[0] = addTexture(offscreenImages[0]);
    offscreenTextures[1] = addTexture(offscreenImages[1]);
    
    // Drawing textures are marked as flipped along Y to ensure they are properly
    // rendered by Processing, which has inverted Y axis with respect to
    // OpenGL.
    offscreenTextures[0].setFlippedY(true);
    offscreenTextures[1].setFlippedY(true);

    offscreenIndex = 0;

    offscreenFramebuffer = new PFramebuffer(parent, offscreenTextures[0].glWidth, offscreenTextures[0].glHeight,
                                            1, 1, offscreenDepthBits, offscreenStencilBits, false);
    
    // The image texture points to the current offscreen texture.
    texture = offscreenTextures[offscreenIndex]; 
    this.setCache(a3d, offscreenTextures[offscreenIndex]);
    this.setParams(a3d, offscreenParams[offscreenIndex]);       
    
    */
    
    initialized = true;
  }
  
  public void initOffscreenSurface(PGL primary) {
    
    /*
    offscreenTexCrop = new int[4];
    offscreenTexCrop[0] = 0;
    offscreenTexCrop[1] = 0;
    offscreenTexCrop[2] = width;
    offscreenTexCrop[3] = height;      

    offscreenImages = new PImage[2];
    offscreenParams = new PTexture.Parameters[2];
    // Linear filtering is needed to keep decent image quality when rendering 
    // texture at a size different from its original resolution. This is expected
    // to happen for offscreen rendering.
    offscreenParams[0] = new PTexture.Parameters(ARGB, BILINEAR);
    offscreenParams[1] = new PTexture.Parameters(ARGB, BILINEAR);      
    offscreenImages[0] = parent.createImage(width, height, ARGB, offscreenParams[0]);
    offscreenImages[1] = parent.createImage(width, height, ARGB, offscreenParams[1]);                
    
    offscreenTextures = new PTexture[2];
    offscreenTextures[0] = addTexture(offscreenImages[0]);
    offscreenTextures[1] = addTexture(offscreenImages[1]);
    
    // Drawing textures are marked as flipped along Y to ensure they are properly
    // rendered by Processing, which has inverted Y axis with respect to
    // OpenGL.
    offscreenTextures[0].setFlippedY(true);
    offscreenTextures[1].setFlippedY(true);

    offscreenIndex = 0;

    offscreenFramebuffer = new PFramebuffer(parent, offscreenTextures[0].glWidth, offscreenTextures[0].glHeight,
                                            1, 1, offscreenDepthBits, offscreenStencilBits, false);
    
    // The image texture points to the current offscreen texture.
    texture = offscreenTextures[offscreenIndex]; 
    this.setCache(a3d, offscreenTextures[offscreenIndex]);
    this.setParams(a3d, offscreenParams[offscreenIndex]);         
    */
    
    
    initialized = true;
  }
  
  public void updateOffscreenSurface(PGL primary) {    
  }
  
  protected void detainContext() {    
  }
  
  public void releaseContext() {    
  }
  
  public void destroyContext() {   
  }
 
  ///////////////////////////////////////////////////////////////////////////////////
  
  // Utilities  
  
  public boolean contextIsCurrent(Context other) {
    return other.same(/*context*/);
  }  
  
  static public short makeIndex(int intIdx) {
    // When the index value is greater than 32767 subtracting 65536
    // will make it as a short to wrap around to the negative range, which    
    // is all we need to later pass these numbers to opengl (which will 
    // interpret them as unsigned shorts). See discussion here:
    // http://stackoverflow.com/questions/4331021/java-opengl-gldrawelements-with-32767-vertices
    return 32767 < intIdx ? (short)(intIdx - 65536) : (short)intIdx;
  }
  
  ///////////////////////////////////////////////////////////////////////////////////
  
  // Frame rendering  
  
  public boolean initOnscreenDraw() {
    return true;
  }
  
  public void beginOnscreenDraw() {
    GLES20.glClearColor(0, 0, 0, 0);
    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
    
    /*
    if (clearColorBuffer) {
      // Simplest scenario: clear mode means we clear both the color and depth buffers.
      // No need for saving front color buffer, etc.
      gl.glClearColor(0, 0, 0, 0);
      gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);        
    } else {
      // We need to save the color buffer after finishing with the rendering of this frame,
      // to use is as the background for the next frame (I call this "incremental rendering"). 
     
      if (fboSupported) {
        if (offscreenFramebuffer != null) {
          // Setting the framebuffer corresponding to this surface.
          pushFramebuffer();
          setFramebuffer(offscreenFramebuffer);
          // Setting the current front color buffer.
          offscreenFramebuffer.setColorBuffer(offscreenTextures[offscreenIndex]);
          
          // Drawing contents of back color buffer as background.
          gl.glClearColor(0, 0, 0, 0);
          if (parent.frameCount == 0) {
            // No need to draw back color buffer because we are in the first frame ever.
            gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);  
          } else {
            gl.glClear(GL10.GL_DEPTH_BUFFER_BIT);
            // Render previous draw texture as background.      
            drawOffscreenTexture((offscreenIndex + 1) % 2);        
          }
        }
      } else {
        if (texture != null) { 
          gl.glClearColor(0, 0, 0, 0);
          gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
          if (0 < parent.frameCount) {
            drawTexture();
          }
        }
      }
    }    
     */
  }
  
  public void endOnscreenDraw() {
    /*
      if (!clearColorBuffer0) {
        // We are in the primary surface, and no clear mode, this means that the current
        // contents of the front buffer needs to be used in the next frame as the background
        // for incremental rendering. Depending on whether or not FBOs are supported,
        // one of the two following paths is selected.
        if (fboSupported) {
          if (offscreenFramebuffer != null) {
            // Restoring screen buffer.
            popFramebuffer();
            
            // Only the primary surface in clear mode will write the contents of the
            // offscreen framebuffer to the screen.
            gl.glClearColor(0, 0, 0, 0);
            gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
      
            // Render current draw texture to screen.
            drawOffscreenTexture(offscreenIndex);
        
            swapOffscreenIndex();
          }
        } else {
          if (texture != null) {
            copyFrameToTexture();
          }
        }
      } 
     */
  }
  
  
  public void beginOffscreenDraw() {
    /*
    // Drawing contents of back color buffer as background.
    gl.glClearColor(0, 0, 0, 0);
    if (clearColorBuffer || parent.frameCount == 0) {
      // No need to draw back color buffer.
      gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);  
    } else {
      gl.glClear(GL10.GL_DEPTH_BUFFER_BIT);
      // Render previous draw texture as background.      
      drawOffscreenTexture((offscreenIndex + 1) % 2);        
    } 
    */ 
  }
  
  public void endOffscreenDraw() {
    //swapOffscreenIndex(); 
  }  
  
  public boolean canDraw() {
    return true;    
  }  
  
  public void requestDraw() {
    if (pg.parent.looping) { // This "if" is needed to avoid flickering when looping is disabled.
      ((GLSurfaceView) pg.parent.surfaceView).requestRender();
    }
  }  
  

  ///////////////////////////////////////////////////////////////////////////////////
  
  // Caps query

  public String glGetString(int name) {
    return GLES20.glGetString(name);
  }
 
  public void glGetIntegerv(int name, int[] values, int offset) {
    GLES20.glGetIntegerv(name, values, offset);
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
  
  public int getError() {
    return GLES20.glGetError();
  }
  
  public String getErrorString(int err) {
    return GLU.gluErrorString(err);
  }
  
  /////////////////////////////////////////////////////////////////////////////////
  
  // Rendering options

  public void setFrontFace(int mode) {
    GLES20.glFrontFace(mode);
  }
    
  public void glDepthMask(boolean flag) {
    GLES20.glDepthMask(flag);   
  }
    
  public void setDepthFunc(int func) {
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
  
  
  public ByteBuffer glMapBuffer(int target, int acccess) {  
    //return gl2f.glMapBuffer(GL.GL_ARRAY_BUFFER, GL2.GL_READ_WRITE);
    return null;
  }
  
  public ByteBuffer glMapBufferRange(int target, int offset, int length, int acccess) {
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
    GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, id);
  }
    
  public void glRenderbufferStorageMultisample(int target, int samples, int format, int width, int height) {
//    GLES20.glRenderbufferStorageMultisample(GLES20.GL_RENDERBUFFER, samples, format, w, h);
  }
  
  public void glRenderbufferStorage(int target, int format, int width, int height) {
//    GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER, format, w, h);
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
  
  public String getShaderLog(int id) {
    int[] compiled = new int[1];
    GLES20.glGetShaderiv(id, GLES20.GL_COMPILE_STATUS, compiled, 0);
    if (compiled[0] == 0) {
      return GLES20.glGetShaderInfoLog(id);
    } else {
      return "";
    }
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
  
  public void setReadBuffer(int buf) {
//    GLES20.glReadBuffer(buf);
  }
  
  public void glReadPixels(int x, int y, int width, int height, int format, int type, Buffer buffer) {
    GLES20.glReadPixels(x, y, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buffer);
  } 
  
  public void setDrawBuffer(int buf) {
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
  
  public Context getContext() {
    return new Context(/*context*/);
  }
  
  public class Context {
    //protected GLContext context;
    
    Context(/*GLContext context*/) {
      //this.context = context;
    }
    
    boolean same(/*GLContext context*/) {
      //return this.context.hashCode() == context.hashCode();
      return true;
    }
  }   
  
  /////////////////////////////////////////////////////////////////////////////////
  
  // Tessellator interface
    
  // TODO: Implement. Options:
  // 1) Port Java GLU tessellator implementation from JOGL. 
  // 2) Compile GLUES (http://code.google.com/p/glues/) in native code and access through JNI.
  
  public Tessellator createTessellator(TessellatorCallback callback) {
    return new Tessellator(callback);
  }
  
  public class Tessellator {
    //protected GLUtessellator tess;
    protected TessellatorCallback callback;
    //protected GLUCallback gluCallback;
    
    public Tessellator(TessellatorCallback callback) {
      this.callback = callback;
      //tess = GLU.gluNewTess();
      //gluCallback = new GLUCallback();
      
      //GLU.gluTessCallback(tess, GLU.GLU_TESS_BEGIN, gluCallback);
      //GLU.gluTessCallback(tess, GLU.GLU_TESS_END, gluCallback);
      //GLU.gluTessCallback(tess, GLU.GLU_TESS_VERTEX, gluCallback);
      //GLU.gluTessCallback(tess, GLU.GLU_TESS_COMBINE, gluCallback);
      //GLU.gluTessCallback(tess, GLU.GLU_TESS_ERROR, gluCallback);      
    }
    
    public void beginPolygon() {
      //GLU.gluTessBeginPolygon(tess, null);      
    }
    
    public void endPolygon() {
      //GLU.gluTessEndPolygon(tess);
    }
    
    public void setWindingRule(int rule) {
      //GLU.gluTessProperty(tess, GLU.GLU_TESS_WINDING_RULE, rule);  
    }
    
    public void beginContour() {
      //GLU.gluTessBeginContour(tess);  
    }
    
    public void endContour() {
      //GLU.gluTessEndContour(tess);
    }
    
    public void addVertex(double[] v) {
      //GLU.gluTessVertex(tess, v, 0, v);  
    }
    
    /*
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
    */
  }

  public interface TessellatorCallback  {
    public void begin(int type);
    public void end();
    public void vertex(Object data);
    public void combine(double[] coords, Object[] data,
                        float[] weight, Object[] outData);
    public void error(int errnum);    
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
      
      /*
      try {
        gl11 = (GL11) gl;
      } catch (ClassCastException cce) {
        gl11 = null;
      }

      try {
        gl11x = (GL11Ext) gl;
      } catch (ClassCastException cce) {
        gl11x = null;
      }

      try {
        gl11xp = (GL11ExtensionPack) gl;
      } catch (ClassCastException cce) {
        gl11xp = null;
      }
      */
      pg.parent.handleDraw();
    }

    public void onSurfaceChanged(GL10 igl, int iwidth, int iheight) {      
      gl = igl;
      
      // Here is where we should initialize native libs...
      // PGL2JNILib.init(iwidth, iheight);

      /*
      try {
        gl11 = (GL11) gl;
      } catch (ClassCastException cce) {
        gl11 = null;
      }

      try {
        gl11x = (GL11Ext) gl;
      } catch (ClassCastException cce) {
        gl11x = null;
      }

      try {
        gl11xp = (GL11ExtensionPack) gl;
      } catch (ClassCastException cce) {
        gl11xp = null;
      }
      */
      pg.setSize(iwidth, iheight);
    }

    public void onSurfaceCreated(GL10 igl, EGLConfig config) {      
      gl = igl;
      
      /*
      try {
        gl11 = (GL11) gl;
      } catch (ClassCastException cce) {
        gl11 = null;
      }

      try {
        gl11x = (GL11Ext) gl;
      } catch (ClassCastException cce) {
        gl11x = null;
      }

      try {
        gl11xp = (GL11ExtensionPack) gl;
      } catch (ClassCastException cce) {
        gl11xp = null;
      }
      */
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
            
            pg.offscreenDepthBits = d;
            pg.offscreenStencilBits = s;
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
