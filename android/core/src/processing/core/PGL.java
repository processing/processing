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
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.opengles.*;
import android.opengl.GLSurfaceView.EGLConfigChooser;
import android.opengl.GLSurfaceView.Renderer;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;

/** 
 * How the P3D renderer handles the different OpenGL profiles? Basically,
 * P3D has two pipeline modes: fixed or programmable. In the fixed mode,
 * only the gl and gl2f objects are available. The gl2f object contains the 
 * intersection between OpenGL 2.x desktop and OpenGL 1.1 embedded, and in this
 * way it ensures the functionality parity between the P3D render (PC/MAC)
 * and A3D (Android) in the fixed pipeline mode.
 * In the programmable mode, there further options: GL2, GL3 and GL4. 
 * GL2 corresponds to the basic programmable profile that results from the common 
 * functionality between OpenGL 3.0 desktop and OpenGL 2.0 embedded. As said just 
 * before, since P3D and A3D aim at feature parity as much as possible, this is
 * the only programmable-pipeline GL object that the P3D renderer uses.
 * The gl3 and gl4 objects will be available when the pipeline mode is PROG_GL3 or
 * PROG_GL4, respectively. Although P3D doens't make any use of these objects,
 * they are part of the API nonetheless for users (or libraries) requiring advanced 
 * functionality introduced with OpenGL 3 or OpenGL 4.
 * By default, P3D tries to auto-select the pipeline mode by with the following 
 * priority order: PROG_GL4, PROG_GL3, PROG_GL2, FIXED. In all the programmable modes, 
 * the gl2p object is always available. This auto-selection can be optionally
 * overridden when creating the renderer object, so that a specific mode is set. 
 * Note that the programmable mode uses the non-backward compatible GL objects
 * (GL3, GL4, and not GL3bc, GL4bc) so no fixed mode calls are possible under this mode. 
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
  public static final int DEFAULT_VERTEX_CACHE_SIZE = 128;
  
  /** Maximum lights by default is 8, the minimum defined by OpenGL. */   
  public static final int MAX_LIGHTS = 8;
  
  /** Maximum number of tessellated vertices. GLES restricts the vertex indices
   * to be of type short, so 2^16 = 65536 is the maximum possible number of 
   * vertices that can be referred to within a single VBO. */
  public static final int MAX_TESS_VERTICES = 65536;
  
  /** Maximum number of indices. 2 times the max number of 
   * vertices to have good room for vertex reuse. */
  public static final int MAX_TESS_INDICES  = 2 * MAX_TESS_VERTICES;  

  public static final int LESS              = GL10.GL_LESS;
  public static final int LESS_OR_EQUAL     = GL10.GL_LEQUAL;
  public static final int COUNTER_CLOCKWISE = GL10.GL_CCW;
  public static final int CLOCKWISE         = GL10.GL_CW;  
  public static final int FRONT             = GL10.GL_FRONT;
  public static final int BACK              = GL10.GL_BACK;
  
  public static final int BLEND_EQ_ADD              = GL11ExtensionPack.GL_FUNC_ADD;
  public static final int BLEND_EQ_MIN              = 0x8007;
  public static final int BLEND_EQ_MAX              = 0x8008;
  public static final int BLEND_EQ_REVERSE_SUBTRACT = GL11ExtensionPack.GL_FUNC_REVERSE_SUBTRACT;
  
  public static final int REPLACE  = GL10.GL_REPLACE;
  public static final int MODULATE = GL10.GL_MODULATE;
  
  public static final int FLAT   = GL10.GL_FLAT;
  public static final int SMOOTH = GL10.GL_SMOOTH;
  
  public static final int TEXTURE_2D = GL10.GL_TEXTURE_2D;
  public static final int RGB        = GL10.GL_RGB;
  public static final int RGBA       = GL10.GL_RGBA;
  public static final int ALPHA      = GL10.GL_ALPHA;
  
  public static final int NEAREST              = GL10.GL_NEAREST;
  public static final int LINEAR               = GL10.GL_LINEAR;
  public static final int LINEAR_MIPMAP_LINEAR = GL10.GL_LINEAR_MIPMAP_LINEAR;
  
  public static final int CLAMP_TO_EDGE = GL10.GL_CLAMP_TO_EDGE;
  public static final int REPEAT        = GL10.GL_REPEAT;
  
  public static final int RGBA8 = -1;  
  public static final int DEPTH_24BIT_STENCIL_8BIT = -1;
  
  public static final int DEPTH_16BIT = GL11ExtensionPack.GL_DEPTH_COMPONENT16;
  public static final int DEPTH_24BIT = GL11ExtensionPack.GL_DEPTH_COMPONENT24;
  public static final int DEPTH_32BIT = GL11ExtensionPack.GL_DEPTH_COMPONENT32;    
  
  public static final int STENCIL_1BIT = GL11ExtensionPack.GL_STENCIL_INDEX1_OES; 
  public static final int STENCIL_4BIT = GL11ExtensionPack.GL_STENCIL_INDEX4_OES; 
  public static final int STENCIL_8BIT = GL11ExtensionPack.GL_STENCIL_INDEX8_OES;   
  
  public static final int FRAMEBUFFER_COMPLETE                      = GL11ExtensionPack.GL_FRAMEBUFFER_COMPLETE_OES;    
  public static final int FRAMEBUFFER_INCOMPLETE_ATTACHMENT         = GL11ExtensionPack.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT_OES;
  public static final int FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT = GL11ExtensionPack.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT_OES;
  public static final int FRAMEBUFFER_INCOMPLETE_DIMENSIONS         = GL11ExtensionPack.GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS_OES;      
  public static final int FRAMEBUFFER_INCOMPLETE_FORMATS            = GL11ExtensionPack.GL_FRAMEBUFFER_INCOMPLETE_FORMATS_OES;  
  public static final int FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER        = GL11ExtensionPack.GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER_OES;
  public static final int FRAMEBUFFER_INCOMPLETE_READ_BUFFER        = GL11ExtensionPack.GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER_OES;  
  public static final int FRAMEBUFFER_UNSUPPORTED                   = GL11ExtensionPack.GL_FRAMEBUFFER_UNSUPPORTED_OES;
    
  public static final int STATIC_DRAW  = GL11.GL_STATIC_DRAW;
  public static final int DYNAMIC_DRAW = GL11.GL_DYNAMIC_DRAW;
  public static final int STREAM_DRAW  = -1;
    
  public static final int TRIANGLE_FAN   = GL11.GL_TRIANGLE_FAN;
  public static final int TRIANGLE_STRIP = GL10.GL_TRIANGLE_STRIP;
  public static final int TRIANGLES      = GL10.GL_TRIANGLES;  
  
  public static final int TESS_WINDING_NONZERO = -1;
  public static final int TESS_WINDING_ODD     = -1;  
  
  // Rendering pipeline modes
  public static final int FIXED    = 0;
  public static final int PROG_GL2 = 1;
  public static final int PROG_GL3 = 2;
  public static final int PROG_GL4 = 3;  
  
  /** Pipeline mode: FIXED, PROG_GL2, PROG_GL3 or PROG_GL4 */
  public int pipeline;
  
  public GL10 gl;
  public GL11 gl11;
  public GL11Ext gl11x;
  public GL11ExtensionPack gl11xp;
  public GLU glu; 

  public AndroidRenderer renderer;
  
  public PGraphicsAndroid3D pg;
  
  public boolean initialized;
    
  ///////////////////////////////////////////////////////////////////////////////////
  
  // Intialization, finalization
  
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
    gl11 = primary.gl11;
    gl11x = primary.gl11x;
    gl11xp = primary.gl11xp;
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
    gl.glClearColor(0, 0, 0, 0);
    gl.glClear(GL10.GL_COLOR_BUFFER_BIT);
    
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
  
  public String getVendorString() {
    return gl.glGetString(GL10.GL_VENDOR);  
  }
  
  public String getRendererString() {
    return gl.glGetString(GL10.GL_RENDERER);  
  }
  
  public String getVersionString() {
    return gl.glGetString(GL10.GL_VERSION);  
  }
  
  public String getExtensionsString() {
    return gl.glGetString(GL10.GL_EXTENSIONS); 
  }
  
  public boolean isNpotTexSupported() {
    // Better way to check for extensions and related functions (taken from jMonkeyEngine):
    // renderbufferStorageMultisample = gl.isExtensionAvailable("GL_EXT_framebuffer_multisample") && 
    //                                  gl.isFunctionAvailable("glRenderbufferStorageMultisample");    
    // For more details on GL properties initialization in jMonkey using JOGL2, take a look at:
    // http://code.google.com/p/jmonkeyengine/source/browse/branches/jme3/src/jogl2/com/jme3/renderer/jogl/JoglRenderer.java
    
    String ext = gl.glGetString(GL10.GL_EXTENSIONS);
    if (-1 < ext.indexOf("texture_non_power_of_two")) {
      return true;
    }
    return false;    
  }
  
  public boolean hasMipmapGeneration() {
    String ext = gl.glGetString(GL10.GL_EXTENSIONS);
    if (-1 < ext.indexOf("generate_mipmap")) {
      return true;
    }    
    return false;
  }

  public boolean isMatrixGetSupported() {
    String ext = gl.glGetString(GL10.GL_EXTENSIONS);
    if (-1 < ext.indexOf("matrix_get")) {
      return true;
    }
    return false;
  }
  
  public boolean isTexenvCrossbarSupported() {
    String ext = gl.glGetString(GL10.GL_EXTENSIONS);
    if (-1 < ext.indexOf("texture_env_crossbar")) {
      return true;
    }    
    return false;
  }

  public boolean isVboSupported() {
    String ver = gl.glGetString(GL10.GL_VERSION);
    String ext = gl.glGetString(GL10.GL_EXTENSIONS);
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
    String ext = gl.glGetString(GL10.GL_EXTENSIONS);
    if (-1 < ext.indexOf("framebuffer_object") && gl11xp != null) {
      try {
        gl11xp.glCheckFramebufferStatusOES(GL11ExtensionPack.GL_FRAMEBUFFER_OES);
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
    if (gl11xp != null) { 
      try {
        gl11xp.glBlendEquation(GL11ExtensionPack.GL_FUNC_ADD);
        return true;
      } catch (UnsupportedOperationException e) {
        // This takes care of Android 2.1 and older where the glBlendEquation is present in the API,
        // but any call to it will result in an error.
        return false;
      }
    } else {
      return false;
    }
  }  
  
  public int getMaxTexureSize() {
    int temp[] = new int[1];    
    gl.glGetIntegerv(GL10.GL_MAX_TEXTURE_SIZE, temp, 0);
    return temp[0];    
  }
  
  public int getMaxAliasedLineWidth() {
    int temp[] = new int[2];
    gl.glGetIntegerv(GL10.GL_ALIASED_LINE_WIDTH_RANGE, temp, 0);
    return temp[1];
  }
  
  public int getMaxAliasedPointSize() {
    int temp[] = new int[2];
    gl.glGetIntegerv(GL10.GL_ALIASED_POINT_SIZE_RANGE, temp, 0);
    return temp[1];    
  }
  
  public int getMaxTextureUnits() {
    // The maximum number of texture units only makes sense in the
    // fixed pipeline.
    int temp[] = new int[1];
    gl.glGetIntegerv(GL10.GL_MAX_TEXTURE_UNITS, temp, 0);
    return temp[0];    
  }  

  public void getNumSamples(int[] num) {
    num[0] = 1;    
  }  
  
  ///////////////////////////////////////////////////////////////////////////////////
  
  // Render control 
  
  public void flush() {
    gl.glFlush();
  }  
  
  public void finish() {
    gl.glFinish();
  }
  
  /////////////////////////////////////////////////////////////////////////////////
  
  // Error  
  
  public int getError() {
    return gl.glGetError();
  }
  
  public String getErrorString(int err) {
    return GLU.gluErrorString(err);
  }

  /////////////////////////////////////////////////////////////////////////////////
  
  // Rendering options
  
  public void enableDepthTest() {
    gl.glEnable(GL10.GL_DEPTH_TEST);
  }

  public void disableDepthTest() {
    gl.glDisable(GL10.GL_DEPTH_TEST);
  }  
  
  public void enableDepthMask() {
    gl.glDepthMask(true);    
  }
  
  public void disableDepthMask() {
    gl.glDepthMask(false);    
  }  
  
  public void setDepthFunc(int func) {
    gl.glDepthFunc(func);  
  }  
  
  public void setShadeModel(int model) {
    gl.glShadeModel(model);
  }
  
  public void setFrontFace(int mode) {
    gl.glFrontFace(mode);
  }
  
  public void enableMultisample() {
    gl.glEnable(GL10.GL_MULTISAMPLE);  
  }
  
  public void disableMultisample() {
    gl.glDisable(GL10.GL_MULTISAMPLE);  
  }

  public void enablePointSmooth() {
    gl.glEnable(GL10.GL_POINT_SMOOTH);  
  }
  
  public void disablePointSmooth() {
    gl.glDisable(GL10.GL_POINT_SMOOTH);  
  }

  public void enableLineSmooth() {
    gl.glEnable(GL10.GL_LINE_SMOOTH);  
  }
  
  public void disableLineSmooth() {
    gl.glDisable(GL10.GL_LINE_SMOOTH);  
  }  
  
  public void enablePolygonSmooth() {
    //gl.glEnable(GL10.GL_POLYGON_SMOOTH);  
  }
  
  public void disablePolygonSmooth() {
    //gl.glDisable(GL10.GL_POLYGON_SMOOTH);
  }    
  
  public void enableColorMaterial() {
    gl.glEnable(GL10.GL_COLOR_MATERIAL);    
  }

  public void disableColorMaterial() {
    gl.glDisable(GL10.GL_COLOR_MATERIAL);    
  }  
  
  public void enableNormalization() {
    gl.glEnable(GL10.GL_NORMALIZE);  
  }

  public void disableNormalization() {
    gl.glDisable(GL10.GL_NORMALIZE);  
  }  
  
  public void enableRescaleNormals() {
    gl.glEnable(GL10.GL_RESCALE_NORMAL);
  }

  public void disableRescaleNormals() {
    gl.glDisable(GL10.GL_RESCALE_NORMAL);
  }  
  
  
  /////////////////////////////////////////////////////////////////////////////////
  
  // Vertex arrays    
  
  public void genVertexArray(int[] id) {
    //gl.glGenVertexArrays(1, id, 0);  
  }
  
  public void delVertexArray(int[] id) {
    //gl.glDeleteVertexArrays(1, id, 0);
  }
  
  /////////////////////////////////////////////////////////////////////////////////
  
  // Textures     
  
  public void genTexture(int[] id) {
    gl.glGenTextures(1, id, 0);
  }

  public void delTexture(int[] id) {
    gl.glDeleteTextures(1, id, 0);
  }  
  
  public void enableTexturing(int target) {
    gl.glEnable(target);
  }
  
  public void setActiveTexUnit(int tu) {
    gl.glActiveTexture(GL10.GL_TEXTURE0 + tu);
  }
  
  public void bindTexture(int target, int id) {
    gl.glBindTexture(target, id);
  }

  public void unbindTexture(int target) {
    gl.glBindTexture(target, 0);
  }  
  
  public void disableTexturing(int target) {
    gl.glDisable(target);
  }    
  
  public void initTex(int target, int format, int w, int h) {
    gl.glTexImage2D(target, 0, format, w, h, 0, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, null);
  }

  public void copyTexImage(Buffer image, int target, int format, int level, int w, int h) {
    gl.glTexImage2D(target, level, format, w, h, 0, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, image);
  } 
  
  public void copyTexSubImage(Buffer image, int target, int level, int x, int y, int w, int h) {
    gl.glTexSubImage2D(target, 0, x, y, w, h, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, image);
  }

  public void copyTexSubImage(int[] pixels, int target, int level, int x, int y, int w, int h) {
    gl.glTexSubImage2D(target, level, x, y, w, h, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, IntBuffer.wrap(pixels));
  }  
  
  public void setTexEnvironmentMode(int mode) {
    //gl.glTexEnvi(GL10.GL_TEXTURE_ENV, GL10.GL_TEXTURE_ENV_MODE, mode);   
  }
  
  public void enableTexMipmapGen(int target) {
    gl.glTexParameterf(target, GL11.GL_GENERATE_MIPMAP, GL10.GL_TRUE);
  }

  public void disableTexMipmapGen(int target) {
    gl.glTexParameterf(target, GL11.GL_GENERATE_MIPMAP, GL10.GL_FALSE);
  }  
  
  public void setTexMinFilter(int target, int filter) {
    gl.glTexParameterf(target, GL10.GL_TEXTURE_MIN_FILTER, filter); 
  }
  
  public void setTexMagFilter(int target, int filter) {
    gl.glTexParameterf(target, GL10.GL_TEXTURE_MAG_FILTER, filter);
  }
  
  public void setTexWrapS(int target, int wrap) {
    gl.glTexParameterf(target, GL10.GL_TEXTURE_WRAP_S, wrap);
  }
  
  public void setTexWrapT(int target, int wrap) {
    gl.glTexParameterf(target, GL10.GL_TEXTURE_WRAP_T, wrap); 
  }  
  
  /////////////////////////////////////////////////////////////////////////////////
  
  // Vertex Buffers

  public void genBuffer(int[] id) {
    gl11.glGenBuffers(1, id, 0);  
  }
  
  public void delBuffer(int[] id) {
    gl11.glDeleteBuffers(1, id, 0);  
  }

  public void bindVertexBuffer(int id) {
    gl11.glBindBuffer(GL11.GL_ARRAY_BUFFER, id);
  }
  
  public void initVertexBuffer(int size, int mode) {
    gl11.glBufferData(GL11.GL_ARRAY_BUFFER, size * SIZEOF_FLOAT, null, mode);  
  }
  
  public void copyVertexBufferData(float[] data, int size, int mode) {
    gl11.glBufferData(GL11.GL_ARRAY_BUFFER, size * SIZEOF_FLOAT, FloatBuffer.wrap(data, 0, size), mode);     
  }
  
  public void copyVertexBufferData(float[] data, int offset, int size, int mode) {
    gl11.glBufferData(GL11.GL_ARRAY_BUFFER, size * SIZEOF_FLOAT, FloatBuffer.wrap(data, offset, size), mode);     
  }  
  
  public void copyVertexBufferSubData(float[] data, int offset, int size, int mode) {
    gl11.glBufferSubData(GL11.GL_ARRAY_BUFFER, offset * SIZEOF_FLOAT, size * SIZEOF_FLOAT, FloatBuffer.wrap(data, 0, size));    
  }
  
  public void setVertexFormat(int size, int offset) {
    gl11.glVertexPointer(size, GL11.GL_FLOAT, 0, size * offset * SIZEOF_FLOAT);
  }
  
  public void setColorFormat(int size, int offset) {
    gl11.glColorPointer(size, GL11.GL_FLOAT, 0, size * offset* SIZEOF_FLOAT);
  }
  
  public void setNormalFormat(int size, int offset) {
    gl11.glNormalPointer(GL11.GL_FLOAT, 0, size * offset* SIZEOF_FLOAT);
  }
  
  public void setTexCoordFormat(int size, int offset) {
    gl11.glTexCoordPointer(size, GL11.GL_FLOAT, 0, size * offset* SIZEOF_FLOAT);
  }
  
  public void unbindVertexBuffer() {
    gl11.glBindBuffer(GL11.GL_ARRAY_BUFFER, 0);
  }
  
  public void bindIndexBuffer(int id) {
    gl11.glBindBuffer(GL11.GL_ELEMENT_ARRAY_BUFFER, id);
  }
  
  public void initIndexBuffer(int size, int mode) {
    gl11.glBufferData(GL11.GL_ELEMENT_ARRAY_BUFFER, size * SIZEOF_SHORT, null, mode);  
  }
  
  public void copyIndexBufferData(short[] data, int size, int mode) {
    gl11.glBufferData(GL11.GL_ELEMENT_ARRAY_BUFFER, size * SIZEOF_SHORT, ShortBuffer.wrap(data, 0, size), mode);     
  }
  
  public void copyIndexBufferData(short[] data, int offset, int size, int mode) {
    gl11.glBufferData(GL11.GL_ELEMENT_ARRAY_BUFFER, size * SIZEOF_SHORT, ShortBuffer.wrap(data, offset, size), mode);     
  }

  public void copyIndexBufferSubData(short[] data, int offset, int size, int mode) {
    gl11.glBufferSubData(GL11.GL_ELEMENT_ARRAY_BUFFER, offset * SIZEOF_SHORT, size * SIZEOF_SHORT, ShortBuffer.wrap(data, 0, size));
  }  
  
  public void renderIndexBuffer(int size) {
    gl11.glDrawElements(GL10.GL_TRIANGLES, size, GL10.GL_UNSIGNED_SHORT, 0);
  }

  public void renderIndexBuffer(int offset, int size) {
    gl11.glDrawElements(GL10.GL_TRIANGLES, size, GL10.GL_UNSIGNED_SHORT, offset * SIZEOF_SHORT);    
  }
    
  public void unbindIndexBuffer() {
    gl11.glBindBuffer(GL11.GL_ELEMENT_ARRAY_BUFFER, 0);
  }
  
  public void enableVertexArrays() {
    gl11.glEnableClientState(GL11.GL_VERTEX_ARRAY);    
  }

  public void enableColorArrays() {
    gl11.glEnableClientState(GL11.GL_COLOR_ARRAY);    
  }

  public void enableNormalArrays() {
    gl11.glEnableClientState(GL11.GL_NORMAL_ARRAY);  
  }

  public void enableTexCoordArrays() {
    gl11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);  
  }  
  
  public void disableVertexArrays() {
    gl11.glDisableClientState(GL11.GL_VERTEX_ARRAY);    
  }

  public void disableColorArrays() {
    gl11.glDisableClientState(GL11.GL_COLOR_ARRAY);    
  }

  public void disableNormalArrays() {
    gl11.glDisableClientState(GL11.GL_NORMAL_ARRAY);  
  }
  
  public void disableTexCoordArrays() {
    gl11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);  
  }    
  
  public void enableAttribsArray(int loc) {
    //gl.glEnableVertexAttribArray(loc);
  }
  
  public void setAttribsFormat(int loc, int size, int stride, long offset) {
    //gl.glVertexAttribPointer(loc, size, GL.GL_FLOAT, false, stride, offset);
  }

  public void disableAttribsArray(int loc) {
    //gl2x.glDisableVertexAttribArray(loc);
  }  
  
  public ByteBuffer mapVertexBuffer() {  
    //return gl2f.glMapBuffer(GL.GL_ARRAY_BUFFER, GL2.GL_READ_WRITE);
    return null;
  }
  
  public ByteBuffer mapVertexBufferRange(int offset, int length) {
    //return gl2x.glMapBufferRange(GL.GL_ARRAY_BUFFER, offset, length, GL2.GL_READ_WRITE);
    return null;
  }
  
  public void unmapVertexBuffer() {
    //gl2f.glUnmapBuffer(GL.GL_ARRAY_BUFFER);
  }

  public ByteBuffer mapIndexBuffer() {  
    //return gl2f.glMapBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, GL2.GL_READ_WRITE);
    return null;
  }
  
  public ByteBuffer mapIndexBufferRange(int offset, int length) {
    //return gl2x.glMapBufferRange(GL.GL_ELEMENT_ARRAY_BUFFER, offset, length, GL2.GL_READ_WRITE);
    return null;
  }
  
  public void unmapIndexBuffer() {
    //gl2f.glUnmapBuffer(GL.GL_ELEMENT_ARRAY_BUFFER);
  }    
  
  /////////////////////////////////////////////////////////////////////////////////
  
  // Framebuffers, renderbuffers  
  
  public void genFramebuffer(int[] id) {
    gl11xp.glGenFramebuffersOES(1, id, 0);    
  }
  
  public void delFramebuffer(int[] id) {
    gl11xp.glDeleteFramebuffersOES(1, id, 0);    
  }
  
  public void genRenderbuffer(int[] id) {
    gl11xp.glGenRenderbuffersOES(1, id, 0);    
  }
  
  public void delRenderbuffer(int[] id) {
    gl11xp.glGenRenderbuffersOES(1, id, 0);    
  }
  
  public void bindFramebuffer(int id) {
    gl11xp.glBindFramebufferOES(GL11ExtensionPack.GL_FRAMEBUFFER_OES, id);
  }
  
  public void bindReadFramebuffer(int id) {
    //gl11xp.glBindFramebufferOES(GL11ExtensionPack.GL_READ_FRAMEBUFFER, id);  
  }

  public void bindWriteFramebuffer(int id) {
    //gl.glBindFramebuffer(GL2.GL_DRAW_FRAMEBUFFER, id);  
  }  
  
  public void copyFramebuffer(int srcW, int srcH, int destW, int destH) {
    //gl2x.glBlitFramebuffer(0, 0, srcW, srcH, 0, 0, destW, destH, GL.GL_COLOR_BUFFER_BIT, GL.GL_NEAREST);    
  }
  
  public void cleanFramebufferTexture(int fb) {
    gl11xp.glFramebufferTexture2DOES(GL11ExtensionPack.GL_FRAMEBUFFER_OES, 
                                     GL11ExtensionPack.GL_COLOR_ATTACHMENT0_OES + fb, 
                                     GL10.GL_TEXTURE_2D, 0, 0);  
  }
  
  public void setFramebufferTexture(int fb, int target, int id) {
    gl11xp.glFramebufferTexture2DOES(GL11ExtensionPack.GL_RENDERBUFFER_OES, 
                                     GL11ExtensionPack.GL_COLOR_ATTACHMENT0_OES + fb, target, id, 0);
  }

  public void bindRenderbuffer(int id) {
    gl11xp.glBindRenderbufferOES(GL11ExtensionPack.GL_RENDERBUFFER_OES, id);
  }
    
  public void setRenderbufferNumSamples(int samples, int format, int w, int h) {
    //gl2x.glRenderbufferStorageMultisample(GL.GL_RENDERBUFFER, samples, format, w, h);
  }
  
  public void setRenderbufferStorage(int format, int w, int h) {
    gl11xp.glRenderbufferStorageOES(GL11ExtensionPack.GL_RENDERBUFFER_OES, format, w, h);
  }
  
  public void setRenderbufferColorAttachment(int id) {
    //gl11xp.glFramebufferRenderbufferOES(GL.GL_FRAMEBUFFER, GL.GL_COLOR_ATTACHMENT0, GL.GL_RENDERBUFFER, id);
  }  
  
  public void setRenderbufferDepthAttachment(int id) {
    gl11xp.glFramebufferRenderbufferOES(GL11ExtensionPack.GL_FRAMEBUFFER_OES,            
                                        GL11ExtensionPack.GL_DEPTH_ATTACHMENT_OES,
                                        GL11ExtensionPack.GL_RENDERBUFFER_OES, id);  
  }
  
  public void setRenderbufferStencilAttachment(int id) {
    gl11xp.glFramebufferRenderbufferOES(GL11ExtensionPack.GL_FRAMEBUFFER_OES,
                                        GL11ExtensionPack.GL_STENCIL_ATTACHMENT_OES,
                                        GL11ExtensionPack.GL_RENDERBUFFER_OES, id);  
  }
  
  public int getFramebufferStatus() {
    return gl11xp.glCheckFramebufferStatusOES(GL11ExtensionPack.GL_FRAMEBUFFER_OES);
  }  

  /////////////////////////////////////////////////////////////////////////////////
  
  // Shaders  
  
  public void genProgram(int[] id) {
    //id[0] = gl2x.glCreateProgram();    
  }
  
  public void delProgram(int[] id) {
    //gl2x.glDeleteProgram(id[0]);  
  }
  
  public void genVertexShader(int[] id) {
    //id[0] = gl2x.glCreateShader(GL2.GL_VERTEX_SHADER);    
  }
  
  public void delVertexShader(int[] id) {
    //gl2x.glDeleteShader(id[0]);    
  }
  
  public void genFragmentShader(int[] id) {
    //id[0] = gl2x.glCreateShader(GL2.GL_FRAGMENT_SHADER);    
  }
  
  public void delFragmentShader(int[] id) {
    //gl2x.glDeleteShader(id[0]);    
  }  

  public void linkProgram(int prog) {
    //gl2.glLinkProgram(prog);  
  }
  
  public void validateProgram(int prog) {
    //gl2.glValidateProgram(prog);
  }
  
  public void startProgram(int prog) {
    //gl2.glUseProgramObjectARB(prog);  
  }
  
  public void stopProgram() {
    //gl2.glUseProgramObjectARB(0);  
  }  
  
  public int getAttribLocation(int prog, String name) {
    //return gl2.glGetAttribLocation(prog, name);
    return -1;
  }
  
  public int getUniformLocation(int prog, String name) {
    //return gl2.glGetUniformLocation(prog, name);
    return -1;
  }  
  
  public void setIntUniform(int loc, int value) {
    //gl2.glUniform1i(loc, value);  
  }
  
  public void setFloatUniform(int loc, float value) {
    //gl2.glUniform1f(loc, value);  
  }    
  
  public void setFloatUniform(int loc, float value0, float value1) {
    //gl2.glUniform2f(loc, value0, value1);  
  }
  
  public void setFloatUniform(int loc, float value0, float value1, float value2) {
    //gl2.glUniform3f(loc, value0, value1, value2);  
  }
  
  public void setFloatUniform(int loc, float value0, float value1, float value2, float value3) {
    //gl2.glUniform4f(loc, value0, value1, value2, value3);  
  }
  
  public void setMatUniform(int loc, float m00, float m01,
                                     float m10, float m11) {
    float[] mat = new float[4];
    mat[0] = m00; mat[4] = m01;
    mat[1] = m10; mat[5] = m11;
    //gl2.glUniformMatrix2fv(loc, 1, false, mat, 0);
  }
  
  public void setMatUniform(int loc, float m00, float m01, float m02,
                                     float m10, float m11, float m12,
                                     float m20, float m21, float m22) {
    float[] mat = new float[9];
    mat[0] = m00; mat[4] = m01; mat[ 8] = m02;
    mat[1] = m10; mat[5] = m11; mat[ 9] = m12;
    mat[2] = m20; mat[6] = m21; mat[10] = m22;    
    //gl2.glUniformMatrix3fv(loc, 1, false, mat, 0);    
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
    //gl2.glUniformMatrix4fv(loc, 1, false, mat, 0);       
  }
  
  public void setFloatAttrib(int loc, float value) {
    //gl2.glVertexAttrib1f(loc, value);  
  }
  
  public void setFloatAttrib(int loc, float value0, float value1) {
    //gl2.glVertexAttrib2f(loc, value0, value1);  
  }  
  
  public void setFloatAttrib(int loc, float value0, float value1, float value2) {
    //gl2.glVertexAttrib3f(loc, value0, value1, value2);  
  }    

  public void setFloatAttrib(int loc, float value0, float value1, float value2, float value3) {
    //gl2.glVertexAttrib4f(loc, value0, value1, value2, value3);  
  }
  
  public void setShaderSource(int id, String source) {
    //gl2.glShaderSource(id, 1, new String[] { source }, (int[]) null, 0);    
  }
  
  public void compileShader(int id) {
    //gl2.glCompileShader(id);    
  }
  
  public void attachShader(int prog, int shader) {
    //gl2.glAttachObjectARB(prog, shader);  
  }
  
  public String getShaderLog(int id) {
    /*
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
    */
    return "";
  }
  
  /////////////////////////////////////////////////////////////////////////////////
  
  // Viewport  
    
  public void getViweport(int[] viewport) {
    gl.glGetIntegerv(GL11.GL_VIEWPORT, viewport, 0);    
  }
  
  public void setViewport(int[] viewport) {
    gl.glViewport(viewport[0], viewport[1], viewport[2], viewport[3]);
  }

  /////////////////////////////////////////////////////////////////////////////////
  
  // Clipping (scissor test)
  
  public void enableClipping() {
    gl.glEnable(GL11.GL_SCISSOR_TEST);
  }

  public void disableClipping() {
    gl.glDisable(GL11.GL_SCISSOR_TEST);
  }  
  
  public void setClipRect(int x, int y, int w, int h) {
    gl.glScissor(x, y, w, h);
  }
  
  
  /////////////////////////////////////////////////////////////////////////////////
  
  // Matrices, transformations
  
  public void setProjectionMode() {
    gl.glMatrixMode(GL10.GL_PROJECTION);
  }
  
  public void setModelviewMode() {
    gl.glMatrixMode(GL10.GL_MODELVIEW);
  }
    
  public void pushMatrix() {
    gl.glPushMatrix();  
  }

  public void popMatrix() {
    gl.glPopMatrix();  
  }  
  
  public void loadIdentity() {
    gl.glLoadIdentity();    
  }
  
  public void multMatrix(float[] mat) {
    gl.glMultMatrixf(mat, 0);
  }

  public void loadMatrix(float[] mat) {
    gl.glLoadMatrixf(mat, 0);
  }    
  
  public void translate(float tx, float ty, float tz) {
    gl.glTranslatef(tx, ty, tz);  
  }
  
  public void rotate(float angle, float vx, float vy, float vz) {
    gl.glRotatef(PApplet.degrees(angle), vx, vy, vz);    
  }
  
  public void scale(float sx, float sy, float sz) {
    gl.glScalef(sx, sy, sz);
  }  
  
  public void setOrthographicProjection(float left, float right, float bottom, float top, float near, float far) {
    gl.glOrthof(left, right, bottom, top, near, far);
  }  
  
  /////////////////////////////////////////////////////////////////////////////////
  
  // Materials
  
  public void setMaterialAmbient(float[] color) {
    gl.glMaterialfv(GL10.GL_FRONT_AND_BACK, GL10.GL_AMBIENT, color, 0);
  }
  
  public void setMaterialSpecular(float[] color) {
    gl.glMaterialfv(GL10.GL_FRONT_AND_BACK, GL10.GL_SPECULAR, color, 0);
  }

  public void setMaterialEmission(float[] color) {
    gl.glMaterialfv(GL10.GL_FRONT_AND_BACK, GL10.GL_EMISSION, color, 0);
  }  
  
  public void setMaterialShininess(float shine) {
    gl.glMaterialf(GL10.GL_FRONT_AND_BACK, GL10.GL_SHININESS, shine);
  }
  
  public void setColor(float r, float g, float b, float a) {
    gl.glColor4f(r, g, b, a);
  }  
  
  /////////////////////////////////////////////////////////////////////////////////
  
  // Lights
  
  public void enableLighting() {
    gl.glEnable(GL10.GL_LIGHTING);
  }
  
  public void disableLighting() {
    gl.glDisable(GL10.GL_LIGHTING);
  }  

  public void setTwoSidedLightModel() {
    gl.glLightModelx(GL11.GL_LIGHT_MODEL_TWO_SIDE, 0);
  }
  
  public void setDefaultAmbientLight(float[] color) {
    gl.glLightModelfv(GL10.GL_LIGHT_MODEL_AMBIENT, color, 0);
  }  
  
  public void enableLight(int light) {
    gl.glEnable(GL10.GL_LIGHT0 + light);
  }

  public void disableLight(int light) {
    gl.glDisable(GL10.GL_LIGHT0 + light);
  }  

  public void setLightPosition(int light, float[] pos) {
    gl.glLightfv(GL10.GL_LIGHT0 + light, GL10.GL_POSITION, pos, 0);
  }
  
  public void setAmbientLight(int light, float[] color) {
    gl.glLightfv(GL10.GL_LIGHT0 + light, GL10.GL_AMBIENT, color, 0);
  }
    
  public void setDiffuseLight(int light, float[] color) {
    gl.glLightfv(GL10.GL_LIGHT0 + light, GL10.GL_DIFFUSE, color, 0);
  }

  public void setSpecularLight(int light, float[] color) {
    gl.glLightfv(GL10.GL_LIGHT0 + light, GL10.GL_SPECULAR, color, 0);
  }
  
  public void setLightDirection(int light, float[] dir) {
    // The w component of lightNormal[num] is zero, so the light is considered as
    // a directional source because the position effectively becomes a direction
    // in homogeneous coordinates:
    // http://glprogramming.com/red/appendixf.html 
    dir[3] = 0;
    gl.glLightfv(GL10.GL_LIGHT0 + light, GL10.GL_POSITION, dir, 0);    
  }
    
  public void setSpotLightCutoff(int light, float cutoff) {
    gl.glLightf(GL10.GL_LIGHT0 + light, GL10.GL_SPOT_CUTOFF, cutoff);  
  }
  
  public void setSpotLightExponent(int light, float exp) {
    gl.glLightf(GL10.GL_LIGHT0 + light, GL10.GL_SPOT_EXPONENT, exp);      
  }
  
  public void setSpotLightDirection(int light, float[] dir) {
    gl.glLightfv(GL10.GL_LIGHT0 + light, GL10.GL_POSITION, dir, 0);
  }
  
  public void setLightConstantAttenuation(int light, float attn) {
    gl.glLightf(GL10.GL_LIGHT0 + light, GL10.GL_CONSTANT_ATTENUATION, attn);  
  }
  
  public void setLightLinearAttenuation(int light, float attn) {
    gl.glLightf(GL10.GL_LIGHT0 + light, GL10.GL_LINEAR_ATTENUATION, attn);  
  }
  
  public void setLightQuadraticAttenuation(int light, float attn) {
    gl.glLightf(GL10.GL_LIGHT0 + light, GL10.GL_QUADRATIC_ATTENUATION, attn);  
  }

  public void setNormal(float nx, float ny, float nz) {
    gl.glNormal3f(nx, ny, nz);    
  }  
  
  /////////////////////////////////////////////////////////////////////////////////
  
  // Blending  
  
  public void enableBlend() {
    gl.glEnable(GL10.GL_BLEND);
  }
  
  public void setBlendEquation(int eq) {
    gl11xp.glBlendEquation(eq);
  }
  
  public void setReplaceBlend() {
    gl.glBlendFunc(GL10.GL_ONE, GL10.GL_ZERO);
  }
  
  public void setDefaultBlend() {
    gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
  }
  
  public void setAdditiveBlend() {
    gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE);
  }
  
  public void setSubstractiveBlend() {
    gl.glBlendFunc(GL10.GL_ONE_MINUS_DST_COLOR, GL10.GL_ZERO);
  }
  
  public void setLightestBlend() {
    gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_DST_ALPHA);
  }
  
  public void setDarkestBlend() {
    gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_DST_ALPHA);
  }
  
  public void setDifferenceBlend() {
    gl.glBlendFunc(GL10.GL_ONE, GL10.GL_ONE);
  }
  
  public void setExclussionBlend() {
    gl.glBlendFunc(GL10.GL_ONE_MINUS_DST_COLOR, GL10.GL_ONE_MINUS_SRC_COLOR);
  }
  
  public void setMultiplyBlend() {
    gl.glBlendFunc(GL10.GL_DST_COLOR, GL10.GL_SRC_COLOR);
  }
  
  public void setScreenBlend() {
    gl.glBlendFunc(GL10.GL_ONE_MINUS_DST_COLOR, GL10.GL_ONE);
  }  
  
  /////////////////////////////////////////////////////////////////////////////////
  
  // Pixels  
  
  public void setReadBuffer(int buf) {
    //gl2x.glReadBuffer(buf);
  }
  
  public void readPixels(Buffer buffer, int x, int y, int w, int h) {
    gl.glReadPixels(x, y, w, h, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, buffer);
  } 
  
  public void setDrawBuffer(int buf) {
    //gl2x.glDrawBuffer(GL.GL_COLOR_ATTACHMENT0 + buf);
  }
  
  public void setClearColor(float r, float g, float b, float a) {
    gl.glClearColor(r, g, b, a);    
  }
  
  public void clearDepthBuffer() {
    gl.glClear(GL10.GL_DEPTH_BUFFER_BIT);
  }
    
  public void clearStencilBuffer() {
    gl.glClear(GL10.GL_STENCIL_BUFFER_BIT);
  }

  public void clearDepthAndStencilBuffers() {
    gl.glClear(GL10.GL_DEPTH_BUFFER_BIT | GL10.GL_STENCIL_BUFFER_BIT);
  }
  
  public void clearColorBuffer() {
    gl.glClear(GL10.GL_COLOR_BUFFER_BIT);
  } 

  public void clearAllBuffers() {
    gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT | GL10.GL_STENCIL_BUFFER_BIT); 
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
  
  public AndroidConfigChooser getConfigChooser(int r, int g, int b, int a, int d, int s) {
    AndroidConfigChooser configChooser = new AndroidConfigChooser(r, g, b, a, d, s);
    return configChooser;
  }    
  
  public class AndroidRenderer implements Renderer {
    public AndroidRenderer() {
    }

    public void onDrawFrame(GL10 igl) {
      gl = igl;

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
      
      pg.parent.handleDraw();
    }

    public void onSurfaceChanged(GL10 igl, int iwidth, int iheight) {
      gl = igl;
      
      // Here is where we should initialize native libs...
      // PGL2JNILib.init(iwidth, iheight);

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
      
      pg.setSize(iwidth, iheight);
    }

    public void onSurfaceCreated(GL10 igl, EGLConfig config) {
      gl = igl;
      
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

    /*
     * This EGL config specification is used to specify 2.0 rendering. We use a
     * minimum size of 4 bits for red/green/blue, but will perform actual
     * matching in chooseConfig() below.
     */
    private int EGL_OPENGL_ES_BIT = 0x01; // EGL 1.x attribute value for
                                                 // GL_RENDERABLE_TYPE.
//    private int EGL_OPENGL_ES2_BIT = 0x04; // EGL 2.x attribute value for
                                                  // GL_RENDERABLE_TYPE.
    private int[] configAttribsGL = { EGL10.EGL_RED_SIZE, 4,
        EGL10.EGL_GREEN_SIZE, 4, EGL10.EGL_BLUE_SIZE, 4,
        EGL10.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES_BIT,
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
          String configStr = "A3D - selected EGL config : "
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
      
      if (PApplet.DEBUG) {
        String configStr = "A3D - selected EGL config : "
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
