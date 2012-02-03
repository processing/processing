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
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

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
  
  public static final int LESS              = GLES20.GL_LESS;
  public static final int LESS_OR_EQUAL     = GLES20.GL_LEQUAL;
  public static final int COUNTER_CLOCKWISE = GLES20.GL_CCW;
  public static final int CLOCKWISE         = GLES20.GL_CW;  
  public static final int FRONT             = GLES20.GL_FRONT;
  public static final int BACK              = GLES20.GL_BACK;
  
  public static final int BLEND_EQ_ADD              = GLES20.GL_FUNC_ADD;
  public static final int BLEND_EQ_MIN              = 0x8007;
  public static final int BLEND_EQ_MAX              = 0x8008;
  public static final int BLEND_EQ_REVERSE_SUBTRACT = GLES20.GL_FUNC_REVERSE_SUBTRACT;
  
  public static final int FLAT   = -1; //GLES20.GL_FLAT;
  public static final int SMOOTH = -1; //GLES20.GL_SMOOTH;
  
  public static final int TEXTURE_2D = GLES20.GL_TEXTURE_2D;
  public static final int RGB        = GLES20.GL_RGB;
  public static final int RGBA       = GLES20.GL_RGBA;
  public static final int ALPHA      = GLES20.GL_ALPHA;
  
  public static final int NEAREST              = GLES20.GL_NEAREST;
  public static final int LINEAR               = GLES20.GL_LINEAR;
  public static final int LINEAR_MIPMAP_LINEAR = GLES20.GL_LINEAR_MIPMAP_LINEAR;
  
  public static final int CLAMP_TO_EDGE = GLES20.GL_CLAMP_TO_EDGE;
  public static final int REPEAT        = GLES20.GL_REPEAT;
  
  public static final int RGBA8 = -1;  
  public static final int DEPTH_24BIT_STENCIL_8BIT = -1;
  
  public static final int DEPTH_16BIT = GLES20.GL_DEPTH_COMPONENT16;
  public static final int DEPTH_24BIT = -1; //GLES20.GL_DEPTH_COMPONENT24;
  public static final int DEPTH_32BIT = -1; //GLES20.GL_DEPTH_COMPONENT32;    
  
  public static final int STENCIL_1BIT = -1; //GLES20.GL_STENCIL_INDEX1; 
  public static final int STENCIL_4BIT = -1; //GLES20.GL_STENCIL_INDEX4; 
  public static final int STENCIL_8BIT = GLES20.GL_STENCIL_INDEX8;   
  
  public static final int FRAMEBUFFER_COMPLETE                      = GLES20.GL_FRAMEBUFFER_COMPLETE;    
  public static final int FRAMEBUFFER_INCOMPLETE_ATTACHMENT         = GLES20.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT;
  public static final int FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT = GLES20.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT;
  public static final int FRAMEBUFFER_INCOMPLETE_DIMENSIONS         = GLES20.GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS;      
  public static final int FRAMEBUFFER_INCOMPLETE_FORMATS            = -1; //GLES20.GL_FRAMEBUFFER_INCOMPLETE_FORMATS;  
  public static final int FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER        = -1; //GLES20.GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER;
  public static final int FRAMEBUFFER_INCOMPLETE_READ_BUFFER        = -1; //GLES20.GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER;  
  public static final int FRAMEBUFFER_UNSUPPORTED                   = GLES20.GL_FRAMEBUFFER_UNSUPPORTED;
    
  public static final int STATIC_DRAW  = GLES20.GL_STATIC_DRAW;
  public static final int DYNAMIC_DRAW = GLES20.GL_DYNAMIC_DRAW;
  public static final int STREAM_DRAW  = -1;
    
  public static final int TRIANGLE_FAN   = GLES20.GL_TRIANGLE_FAN;
  public static final int TRIANGLE_STRIP = GLES20.GL_TRIANGLE_STRIP;
  public static final int TRIANGLES      = GLES20.GL_TRIANGLES;  
  
  public static final int TESS_WINDING_NONZERO = -1;
  public static final int TESS_WINDING_ODD     = -1;  
  
  // Some EGL constants needed to initialize an GLES2 context.
  public static final int EGL_CONTEXT_CLIENT_VERSION = 0x3098;
  public static final int EGL_OPENGL_ES2_BIT = 0x0004;
  
  // Rendering pipeline modes  
  public static final int FIXED    = 0;
  public static final int PROG_GL2 = 1;
  public static final int PROG_GL3 = 2;
  public static final int PROG_GL4 = 3;  
  
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
  
  // TODO: clean-up caps and remove obsolete ones (texture crossbar, etc).
  
  public String getVendorString() {
    return GLES20.glGetString(GLES20.GL_VENDOR);
  }
  
  public String getRendererString() {
    return GLES20.glGetString(GLES20.GL_RENDERER);  
  }
  
  public String getVersionString() {
    return GLES20.glGetString(GLES20.GL_VERSION);  
  }
  
  public String getExtensionsString() {
    return GLES20.glGetString(GLES20.GL_EXTENSIONS); 
  }
  
  public boolean isNpotTexSupported() {
    // Better way to check for extensions and related functions (taken from jMonkeyEngine):
    // renderbufferStorageMultisample = gl.isExtensionAvailable("GL_EXT_framebuffer_multisample") && 
    //                                  gl.isFunctionAvailable("glRenderbufferStorageMultisample");    
    // For more details on GL properties initialization in jMonkey using JOGL2, take a look at:
    // http://code.google.com/p/jmonkeyengine/source/browse/branches/jme3/src/jogl2/com/jme3/renderer/jogl/JoglRenderer.java
    
    String ext = GLES20.glGetString(GLES20.GL_EXTENSIONS);
    if (-1 < ext.indexOf("texture_non_power_of_two")) {
      return true;
    }
    return false;    
  }
  
  public boolean hasMipmapGeneration() {
    String ext = GLES20.glGetString(GLES20.GL_EXTENSIONS);
    if (-1 < ext.indexOf("generate_mipmap")) {
      return true;
    }    
    return false;
  }

  public boolean isMatrixGetSupported() {
    String ext = GLES20.glGetString(GLES20.GL_EXTENSIONS);
    if (-1 < ext.indexOf("matrix_get")) {
      return true;
    }
    return false;
  }
  
  public boolean isTexenvCrossbarSupported() {
    String ext = GLES20.glGetString(GLES20.GL_EXTENSIONS);
    if (-1 < ext.indexOf("texture_env_crossbar")) {
      return true;
    }    
    return false;
  }

  public boolean isVboSupported() {
    String ver = GLES20.glGetString(GLES20.GL_VERSION);
    String ext = GLES20.glGetString(GLES20.GL_EXTENSIONS);
    if (-1 < ext.indexOf("vertex_buffer_object") ||
        -1 < ver.indexOf("1.1") || // Just in case
                                   // vertex_buffer_object
                                   // doesn't appear in the list
                                   // of extensions,
        -1 < ver.indexOf("2.")) { // If the opengl version is
                                  // greater than 1.1, VBOs should
                                  // be supported.
      return true;
    }  
    return false;
  }

  public boolean isFboSupported() {
    String ext = GLES20.glGetString(GLES20.GL_EXTENSIONS);
    if (-1 < ext.indexOf("framebuffer_object")) {
      try {
        GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER);
        return true;
      } catch (UnsupportedOperationException e) {
        // This takes care of Android 2.1 and older where the FBO extension appears to be supported,
        // but any call to the FBO functions would result in an error.
        return false;
      } 
    }    
    return false;
  }

  public boolean isFboMultisampleSupported() { 
    return false;
  }
  
  public boolean isBlendEqSupported() {
    try {
      GLES20.glBlendEquation(GLES20.GL_FUNC_ADD);
      return true;
    } catch (UnsupportedOperationException e) {
      // This takes care of Android 2.1 and older where the glBlendEquation is present in the API,
      // but any call to it will result in an error.
      return false;
    }
  }  
  
  public int getMaxTexureSize() {
    int temp[] = new int[1];    
    GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_SIZE, temp, 0);
    return temp[0];    
  }
  
  public int getMaxAliasedLineWidth() {
    int temp[] = new int[2];
    GLES20.glGetIntegerv(GLES20.GL_ALIASED_LINE_WIDTH_RANGE, temp, 0);
    return temp[1];
  }
  
  public int getMaxAliasedPointSize() {
    int temp[] = new int[2];
    GLES20.glGetIntegerv(GLES20.GL_ALIASED_POINT_SIZE_RANGE, temp, 0);
    return temp[1];    
  }
  
  public int getMaxTextureUnits() {
    // The maximum number of texture units only makes sense in the
    // fixed pipeline.    
    return 2;
  }  

  public void getNumSamples(int[] num) {
    num[0] = 1;    
  }  
  
  ///////////////////////////////////////////////////////////////////////////////////
  
  // Render control 
  
  public void flush() {
    GLES20.glFlush();
  }  
  
  public void finish() {
    GLES20.glFinish();
  }
  
  /////////////////////////////////////////////////////////////////////////////////
  
  // Error  
  
  public int getError() {
    return GLES20.glGetError();
  }
  
  public String getErrorString(int err) {
    return GLU.gluErrorString(err);
  }

  /////////////////////////////////////////////////////////////////////////////////
  
  // Rendering options
  
  // TODO: Check which of the missing options can be emulated in GLES2
  
  public void enableDepthTest() {
    GLES20.glEnable(GLES20.GL_DEPTH_TEST);
  }

  public void disableDepthTest() {
    GLES20.glDisable(GLES20.GL_DEPTH_TEST);
  }  
  
  public void enableDepthMask() {
    GLES20.glDepthMask(true);   
  }
  
  public void disableDepthMask() {
    GLES20.glDepthMask(false);
  }  
  
  public void setDepthFunc(int func) {
    GLES20.glDepthFunc(func); 
  }  
  
  public void setShadeModel(int model) {
//    GLES20.glShadeModel(model);
  }
  
  public void setFrontFace(int mode) {
    GLES20.glFrontFace(mode);
  }
  
  public void enableMultisample() {
//    GLES20.glEnable(GLES20.GL_MULTISAMPLE);  
  }
  
  public void disableMultisample() {
//    GLES20.glDisable(GLES20.GL_MULTISAMPLE);  
  }

  public void enablePointSmooth() {
//    GLES20.glEnable(GLES20.GL_POINT_SMOOTH);  
  }
  
  public void disablePointSmooth() {
//    GLES20.glDisable(GLES20.GL_POINT_SMOOTH);  
  }

  public void enableLineSmooth() {
//    GLES20.glEnable(GLES20.GL_LINE_SMOOTH);  
  }
  
  public void disableLineSmooth() {
//    GLES20.glDisable(GLES20.GL_LINE_SMOOTH);  
  }  
  
  public void enablePolygonSmooth() {
//    GLES20.glEnable(GLES20.GL_POLYGON_SMOOTH);  
  }
  
  public void disablePolygonSmooth() {
//    GLES20.glDisable(GLES20.GL_POLYGON_SMOOTH);
  }    
  
  public void enableColorMaterial() {
//    GLES20.glEnable(GLES20.GL_COLOR_MATERIAL);    
  }

  public void disableColorMaterial() {
//    GLES20.glDisable(GLES20.GL_COLOR_MATERIAL);    
  }  
  
  public void enableNormalization() {
//    GLES20.glEnable(GLES20.GL_NORMALIZE);  
  }

  public void disableNormalization() {
//    GLES20.glDisable(GLES20.GL_NORMALIZE);  
  }  
  
  public void enableRescaleNormals() {
//    GLES20.glEnable(GLES20.GL_RESCALE_NORMAL);
  }

  public void disableRescaleNormals() {
//    GLES20.glDisable(GLES20.GL_RESCALE_NORMAL);
  }  

  
  /////////////////////////////////////////////////////////////////////////////////
  
  // Textures     
  
  public void genTexture(int[] id) {
    GLES20.glGenTextures(1, id, 0);
  }

  public void delTexture(int[] id) {
    GLES20.glDeleteTextures(1, id, 0);
  }  
  
  public void enableTexturing(int target) {
    GLES20.glEnable(target);
  }
  
  public void setActiveTexUnit(int tu) {
    GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + tu);
  }
  
  public void bindTexture(int target, int id) {
    GLES20.glBindTexture(target, id);
  }

  public void unbindTexture(int target) {
    GLES20.glBindTexture(target, 0);
  }  
  
  public void disableTexturing(int target) {
    GLES20.glDisable(target);
  }    
  
  public void initTex(int target, int format, int w, int h) {
    GLES20.glTexImage2D(target, 0, format, w, h, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
  }

  public void copyTexImage(Buffer image, int target, int format, int level, int w, int h) {
    GLES20.glTexImage2D(target, level, format, w, h, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, image);
  } 
  
  public void copyTexSubImage(Buffer image, int target, int level, int x, int y, int w, int h) {
    GLES20.glTexSubImage2D(target, 0, x, y, w, h, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, image);
  }

  public void copyTexSubImage(int[] pixels, int target, int level, int x, int y, int w, int h) {
    GLES20.glTexSubImage2D(target, level, x, y, w, h, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, IntBuffer.wrap(pixels));
  }  
    
  public void setTexMinFilter(int target, int filter) {
    GLES20.glTexParameterf(target, GLES20.GL_TEXTURE_MIN_FILTER, filter); 
  }
  
  public void setTexMagFilter(int target, int filter) {
    GLES20.glTexParameterf(target, GLES20.GL_TEXTURE_MAG_FILTER, filter);
  }
  
  public void setTexWrapS(int target, int wrap) {
    GLES20.glTexParameterf(target, GLES20.GL_TEXTURE_WRAP_S, wrap);
  }
  
  public void setTexWrapT(int target, int wrap) {
    GLES20.glTexParameterf(target, GLES20.GL_TEXTURE_WRAP_T, wrap); 
  }  

  public void generateMipmap(int target) {
    GLES20.glGenerateMipmap(target);
  }  
  
  /////////////////////////////////////////////////////////////////////////////////
  
  // Vertex Buffers

  public void genBuffer(int[] id) {
    GLES20.glGenBuffers(1, id, 0);  
  }
  
  public void delBuffer(int[] id) {
    GLES20.glDeleteBuffers(1, id, 0);  
  }

  public void bindVertexBuffer(int id) {
    GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, id);
  }
  
  public void initVertexBuffer(int size, int mode) {
    GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, size * SIZEOF_FLOAT, null, mode);  
  }
  
  public void copyVertexBufferData(float[] data, int size, int mode) {
    GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, size * SIZEOF_FLOAT, FloatBuffer.wrap(data, 0, size), mode);     
  }
  
  public void copyVertexBufferData(float[] data, int offset, int size, int mode) {
    GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, size * SIZEOF_FLOAT, FloatBuffer.wrap(data, offset, size), mode);     
  }  
  
  public void copyVertexBufferSubData(float[] data, int offset, int size, int mode) {
    GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, offset * SIZEOF_FLOAT, size * SIZEOF_FLOAT, FloatBuffer.wrap(data, 0, size));    
  }  
  
  public void unbindVertexBuffer() {
    GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
  }
  
  public void bindIndexBuffer(int id) {
    GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, id);
  }
  
  public void initIndexBuffer(int size, int mode) {
    GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, size * SIZEOF_SHORT, null, mode);  
  }
  
  public void copyIndexBufferData(short[] data, int size, int mode) {
    GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, size * SIZEOF_SHORT, ShortBuffer.wrap(data, 0, size), mode);     
  }
  
  public void copyIndexBufferData(short[] data, int offset, int size, int mode) {
    GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, size * SIZEOF_SHORT, ShortBuffer.wrap(data, offset, size), mode);     
  }

  public void copyIndexBufferSubData(short[] data, int offset, int size, int mode) {
    GLES20.glBufferSubData(GLES20.GL_ELEMENT_ARRAY_BUFFER, offset * SIZEOF_SHORT, size * SIZEOF_SHORT, ShortBuffer.wrap(data, 0, size));
  }  
  
  public void renderIndexBuffer(int size) {
    GLES20.glDrawElements(GLES20.GL_TRIANGLES, size, GLES20.GL_UNSIGNED_SHORT, 0);
  }

  public void renderIndexBuffer(int offset, int size) { 
    GLES20.glDrawElements(GLES20.GL_TRIANGLES, size, GLES20.GL_UNSIGNED_SHORT, offset * SIZEOF_SHORT);    
  }
    
  public void unbindIndexBuffer() {
    GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);
  }  
  
  public void enableVertexAttribArray(int loc) {
    GLES20.glEnableVertexAttribArray(loc);
  }
  
  public void disableVertexAttribArray(int loc) {
    GLES20.glDisableVertexAttribArray(loc);
  }  
  
  public void setVertexAttribFormat(int loc, int size, int offset) {
    GLES20.glVertexAttribPointer(loc, size, GLES20.GL_FLOAT, false, 0, size * offset * SIZEOF_FLOAT);
  }  
  
  public ByteBuffer mapVertexBuffer() {  
//    return GLES20.glMapBuffer(GLES20.GL_ARRAY_BUFFER, GLES20.GL_READ_WRITE);
    return null;
  }
  
  public ByteBuffer mapVertexBufferRange(int offset, int length) {
//    return GLES20.glMapBufferRange(GLES20.GL_ARRAY_BUFFER, offset, length, GLES20.GL_READ_WRITE);
    return null;
  }
  
  public void unmapVertexBuffer() {
//    GLES20.glUnmapBuffer(GLES20.GL_ARRAY_BUFFER);
  }

  public ByteBuffer mapIndexBuffer() {  
//    return GLES20.glMapBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, GLES20.GL_READ_WRITE);
    return null;
  }
  
  public ByteBuffer mapIndexBufferRange(int offset, int length) {
//    return GLES20.glMapBufferRange(GLES20.GL_ELEMENT_ARRAY_BUFFER, offset, length, GLES20.GL_READ_WRITE);
    return null;
  }
  
  public void unmapIndexBuffer() {
//    GLES20.glUnmapBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER);
  }    
  
  /////////////////////////////////////////////////////////////////////////////////
  
  // Framebuffers, renderbuffers  
  
  public void genFramebuffer(int[] id) {
    GLES20.glGenFramebuffers(1, id, 0);    
  }
  
  public void delFramebuffer(int[] id) {
    GLES20.glDeleteFramebuffers(1, id, 0);    
  }
  
  public void genRenderbuffer(int[] id) {
    GLES20.glGenRenderbuffers(1, id, 0);    
  }
  
  public void delRenderbuffer(int[] id) {
    GLES20.glGenRenderbuffers(1, id, 0);    
  }
  
  public void bindFramebuffer(int id) {
    GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, id);
  }
  
  public void bindReadFramebuffer(int id) {
//    GLES20.glBindFramebuffer(GLES20.GL_READ_FRAMEBUFFER, id);  
  }

  public void bindWriteFramebuffer(int id) {
//    GLES20.glBindFramebuffer(GLES20.GL_DRAW_FRAMEBUFFER, id);  
  }  
  
  public void copyFramebuffer(int srcW, int srcH, int destW, int destH) {
//    GLES20.glBlitFramebuffer(0, 0, srcW, srcH, 0, 0, destW, destH, GLES20.GL_COLOR_BUFFER_BIT, GLES20.GL_NEAREST);    
  }
  
  public void cleanFramebufferTexture(int fb) {
    GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, 
                                  GLES20.GL_COLOR_ATTACHMENT0 + fb, 
                                  GLES20.GL_TEXTURE_2D, 0, 0);  
  }
  
  public void setFramebufferTexture(int fb, int target, int id) {
    GLES20.glFramebufferTexture2D(GLES20.GL_RENDERBUFFER, 
                                  GLES20.GL_COLOR_ATTACHMENT0 + fb, target, id, 0);
  }

  public void bindRenderbuffer(int id) {
    GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, id);
  }
    
  public void setRenderbufferNumSamples(int samples, int format, int w, int h) {
//    GLES20.glRenderbufferStorageMultisample(GLES20.GL_RENDERBUFFER, samples, format, w, h);
  }
  
  public void setRenderbufferStorage(int format, int w, int h) {
//    GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER, format, w, h);
  }
  
  public void setRenderbufferColorAttachment(int id) {
    GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_RENDERBUFFER, id);
  }  
  
  public void setRenderbufferDepthAttachment(int id) {
    GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER,            
                                     GLES20.GL_DEPTH_ATTACHMENT,
                                     GLES20.GL_RENDERBUFFER, id);  
  }
  
  public void setRenderbufferStencilAttachment(int id) {
    GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER,
                                     GLES20.GL_STENCIL_ATTACHMENT,
                                     GLES20.GL_RENDERBUFFER, id);  
  }
  
  public int getFramebufferStatus() {
    return GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER);
  }  

  /////////////////////////////////////////////////////////////////////////////////
  
  // Shaders  
  
  public void genProgram(int[] id) {
    id[0] = GLES20.glCreateProgram();    
  }
  
  public void delProgram(int[] id) {
    GLES20.glDeleteProgram(id[0]);  
  }
  
  public void genVertexShader(int[] id) {
    id[0] = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER);    
  }
  
  public void delVertexShader(int[] id) {
    GLES20.glDeleteShader(id[0]);    
  }
  
  public void genFragmentShader(int[] id) {
    id[0] = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);    
  }
  
  public void delFragmentShader(int[] id) {
    GLES20.glDeleteShader(id[0]);    
  }  

  public void linkProgram(int prog) {
    GLES20.glLinkProgram(prog);  
  }
  
  public void validateProgram(int prog) {
    GLES20.glValidateProgram(prog);
  }
  
  public void startProgram(int prog) {
    GLES20.glUseProgram(prog);  
  }
  
  public void stopProgram() {
    GLES20.glUseProgram(0);  
  }  
  
  public int getAttribLocation(int prog, String name) {
    return GLES20.glGetAttribLocation(prog, name);
  }
  
  public int getUniformLocation(int prog, String name) {
    return GLES20.glGetUniformLocation(prog, name);
  }  
  
  public void setIntUniform(int loc, int value) {
    GLES20.glUniform1i(loc, value);  
  }
  
  public void setFloatUniform(int loc, float value) {
    GLES20.glUniform1f(loc, value);  
  }    
  
  public void setFloatUniform(int loc, float value0, float value1) {
    GLES20.glUniform2f(loc, value0, value1);  
  }
  
  public void setFloatUniform(int loc, float value0, float value1, float value2) {
    GLES20.glUniform3f(loc, value0, value1, value2);  
  }
  
  public void setFloatUniform(int loc, float value0, float value1, float value2, float value3) {
    GLES20.glUniform4f(loc, value0, value1, value2, value3);  
  }

  public void setFloat1ArrayUniform(int loc, float[] v) {
    GLES20.glUniform1fv(loc, v.length, v, 0);
  }    

  public void setFloat2ArrayUniform(int loc, float[] v) {
    GLES20.glUniform2fv(loc, v.length / 2, v, 0);
  }    

  public void setFloat3ArrayUniform(int loc, float[] v) {
    GLES20.glUniform3fv(loc, v.length / 3, v, 0);
  }

  public void setFloat4ArrayUniform(int loc, float[] v) {
    GLES20.glUniform4fv(loc, v.length / 4, v, 0);
  }  
  
  public void setMatUniform(int loc, float m00, float m01,
                                     float m10, float m11) {
    float[] mat = new float[4];
    mat[0] = m00; mat[4] = m01;
    mat[1] = m10; mat[5] = m11;
    GLES20.glUniformMatrix2fv(loc, 1, false, mat, 0);
  }
  
  public void setMatUniform(int loc, float m00, float m01, float m02,
                                     float m10, float m11, float m12,
                                     float m20, float m21, float m22) {
    float[] mat = new float[9];
    mat[0] = m00; mat[4] = m01; mat[ 8] = m02;
    mat[1] = m10; mat[5] = m11; mat[ 9] = m12;
    mat[2] = m20; mat[6] = m21; mat[10] = m22;    
    GLES20.glUniformMatrix3fv(loc, 1, false, mat, 0);    
  }
  
  public void setMatUniform(int loc, float m00, float m01, float m02, float m03,
                                     float m10, float m11, float m12, float m13,
                                     float m20, float m21, float m22, float m23,
                                     float m30, float m31, float m32, float m33) {
    float[] mat = new float[16];      
    mat[0] = m00; mat[4] = m01; mat[ 8] = m02; mat[12] = m03;
    mat[1] = m10; mat[5] = m11; mat[ 9] = m12; mat[13] = m13;
    mat[2] = m20; mat[6] = m21; mat[10] = m22; mat[14] = m23;
    mat[3] = m30; mat[7] = m31; mat[11] = m32; mat[15] = m33;
    GLES20.glUniformMatrix4fv(loc, 1, false, mat, 0);       
  }
  
  public void setFloatAttrib(int loc, float value) {
    GLES20.glVertexAttrib1f(loc, value);  
  }
  
  public void setFloatAttrib(int loc, float value0, float value1) {
    GLES20.glVertexAttrib2f(loc, value0, value1);  
  }  
  
  public void setFloatAttrib(int loc, float value0, float value1, float value2) {
    GLES20.glVertexAttrib3f(loc, value0, value1, value2);  
  }    

  public void setFloatAttrib(int loc, float value0, float value1, float value2, float value3) {
    GLES20.glVertexAttrib4f(loc, value0, value1, value2, value3);  
  }
  
  public void setShaderSource(int id, String source) {
    GLES20.glShaderSource(id, source);    
  }
  
  public void compileShader(int id) {
    GLES20.glCompileShader(id);    
  }
  
  public void attachShader(int prog, int shader) {
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
    
  public void getViweport(int[] viewport) {
    GLES20.glGetIntegerv(GLES20.GL_VIEWPORT, viewport, 0);    
  }
  
  public void setViewport(int[] viewport) {
    GLES20.glViewport(viewport[0], viewport[1], viewport[2], viewport[3]);
  }

  /////////////////////////////////////////////////////////////////////////////////
  
  // Clipping (scissor test)
  
  public void enableClipping() {
    GLES20.glEnable(GLES20.GL_SCISSOR_TEST);
  }

  public void disableClipping() {
    GLES20.glDisable(GLES20.GL_SCISSOR_TEST);
  }  
  
  public void setClipRect(int x, int y, int w, int h) {
    GLES20.glScissor(x, y, w, h);
  }
  
  
  /////////////////////////////////////////////////////////////////////////////////
  
  // Materials
  
  /*
  public void setMaterialAmbient(float[] color) {
//    GLES20.glMaterialfv(GLES20.GL_FRONT_AND_BACK, GLES20.GL_AMBIENT, color, 0);
  }
  
  public void setMaterialSpecular(float[] color) {
//    GLES20.glMaterialfv(GLES20.GL_FRONT_AND_BACK, GLES20.GL_SPECULAR, color, 0);
  }

  public void setMaterialEmission(float[] color) {
//    GLES20.glMaterialfv(GLES20.GL_FRONT_AND_BACK, GLES20.GL_EMISSION, color, 0);
  }  
  
  public void setMaterialShininess(float shine) {
//    GLES20.glMaterialf(GLES20.GL_FRONT_AND_BACK, GLES20.GL_SHININESS, shine);
  }
  
  public void setColor(float r, float g, float b, float a) {
//    GLES20.glColor4f(r, g, b, a);
  } 
  */ 
  
  /////////////////////////////////////////////////////////////////////////////////
  
  // Lights
  
/*  
  public void enableLighting() {
//    GLES20.glEnable(GLES20.GL_LIGHTING);
  }
  
  public void disableLighting() {
//    GLES20.glDisable(GLES20.GL_LIGHTING);
  }  

  public void setTwoSidedLightModel() {
//    GLES20.glLightModelx(GLES20.GL_LIGHT_MODEL_TWO_SIDE, 0);
  }
  
  public void setDefaultAmbientLight(float[] color) {
//    GLES20.glLightModelfv(GLES20.GL_LIGHT_MODEL_AMBIENT, color, 0);
  }  
  
  public void enableLight(int light) {
//    GLES20.glEnable(GLES20.GL_LIGHT0 + light);
  }

  public void disableLight(int light) {
//    GLES20.glDisable(GLES20.GL_LIGHT0 + light);
  }  

  public void setLightPosition(int light, float[] pos) {
//    GLES20.glLightfv(GLES20.GL_LIGHT0 + light, GLES20.GL_POSITION, pos, 0);
  }
  
  public void setAmbientLight(int light, float[] color) {
//    GLES20.glLightfv(GLES20.GL_LIGHT0 + light, GLES20.GL_AMBIENT, color, 0);
  }
    
  public void setDiffuseLight(int light, float[] color) {
//    GLES20.glLightfv(GLES20.GL_LIGHT0 + light, GLES20.GL_DIFFUSE, color, 0);
  }

  public void setSpecularLight(int light, float[] color) {
//    GLES20.glLightfv(GLES20.GL_LIGHT0 + light, GLES20.GL_SPECULAR, color, 0);
  }
  
  public void setLightDirection(int light, float[] dir) {
    // The w component of lightNormal[num] is zero, so the light is considered as
    // a directional source because the position effectively becomes a direction
    // in homogeneous coordinates:
    // http://glprogramming.com/red/appendixf.html 
    dir[3] = 0;
//    GLES20.glLightfv(GLES20.GL_LIGHT0 + light, GLES20.GL_POSITION, dir, 0);    
  }
    
  public void setSpotLightCutoff(int light, float cutoff) {
//    GLES20.glLightf(GLES20.GL_LIGHT0 + light, GLES20.GL_SPOT_CUTOFF, cutoff);  
  }
  
  public void setSpotLightExponent(int light, float exp) {
//    GLES20.glLightf(GLES20.GL_LIGHT0 + light, GLES20.GL_SPOT_EXPONENT, exp);      
  }
  
  public void setSpotLightDirection(int light, float[] dir) {
//    GLES20.glLightfv(GLES20.GL_LIGHT0 + light, GLES20.GL_POSITION, dir, 0);
  }
  
  public void setLightConstantAttenuation(int light, float attn) {
//    GLES20.glLightf(GLES20.GL_LIGHT0 + light, GLES20.GL_CONSTANT_ATTENUATION, attn);  
  }
  
  public void setLightLinearAttenuation(int light, float attn) {
//    GLES20.glLightf(GLES20.GL_LIGHT0 + light, GLES20.GL_LINEAR_ATTENUATION, attn);  
  }
  
  public void setLightQuadraticAttenuation(int light, float attn) {
//    GLES20.glLightf(GLES20.GL_LIGHT0 + light, GLES20.GL_QUADRATIC_ATTENUATION, attn);  
  }

  public void setNormal(float nx, float ny, float nz) {
//    GLES20.glNormal3f(nx, ny, nz);    
  }  
  */
  
  /////////////////////////////////////////////////////////////////////////////////
  
  // Blending  
  
  public void enableBlend() {
    GLES20.glEnable(GLES20.GL_BLEND);
  }
  
  public void setBlendEquation(int eq) {
    GLES20.glBlendEquation(eq);
  }
  
  public void setReplaceBlend() {
    GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ZERO);
  }
  
  public void setDefaultBlend() {
    GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
  }
  
  public void setAdditiveBlend() {
    GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE);
  }
  
  public void setSubstractiveBlend() {
    GLES20.glBlendFunc(GLES20.GL_ONE_MINUS_DST_COLOR, GLES20.GL_ZERO);
  }
  
  public void setLightestBlend() {
    GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_DST_ALPHA);
  }
  
  public void setDarkestBlend() {
    GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_DST_ALPHA);
  }
  
  public void setDifferenceBlend() {
    GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE);
  }
  
  public void setExclussionBlend() {
    GLES20.glBlendFunc(GLES20.GL_ONE_MINUS_DST_COLOR, GLES20.GL_ONE_MINUS_SRC_COLOR);
  }
  
  public void setMultiplyBlend() {
    GLES20.glBlendFunc(GLES20.GL_DST_COLOR, GLES20.GL_SRC_COLOR);
  }
  
  public void setScreenBlend() {
    GLES20.glBlendFunc(GLES20.GL_ONE_MINUS_DST_COLOR, GLES20.GL_ONE);
  }  
  
  /////////////////////////////////////////////////////////////////////////////////
  
  // Pixels  
  
  public void setReadBuffer(int buf) {
//    GLES20.glReadBuffer(buf);
  }
  
  public void readPixels(Buffer buffer, int x, int y, int w, int h) {
    GLES20.glReadPixels(x, y, w, h, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buffer);
  } 
  
  public void setDrawBuffer(int buf) {
//    GLES20.glDrawBuffer(GLES20.GL_COLOR_ATTACHMENT0 + buf);
  }
  
  public void setClearColor(float r, float g, float b, float a) {
    GLES20.glClearColor(r, g, b, a);    
  }
  
  public void clearDepthBuffer() {
    GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT);
  }
    
  public void clearStencilBuffer() {
    GLES20.glClear(GLES20.GL_STENCIL_BUFFER_BIT);
  }

  public void clearDepthAndStencilBuffers() {
    GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_STENCIL_BUFFER_BIT);
  }
  
  public void clearColorBuffer() {
    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
  } 

  public void clearAllBuffers() {
    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_STENCIL_BUFFER_BIT); 
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
