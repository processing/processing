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
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.media.nativewindow.GraphicsConfigurationFactory;
import javax.media.nativewindow.NativeWindow;
import javax.media.nativewindow.NativeWindowFactory;
import javax.media.nativewindow.awt.AWTGraphicsConfiguration;
import javax.media.nativewindow.awt.AWTGraphicsDevice;
import javax.media.nativewindow.awt.AWTGraphicsScreen;
import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GL2ES1;
import javax.media.opengl.GL2ES2;
import javax.media.opengl.GL2GL3;
import javax.media.opengl.GL3;
import javax.media.opengl.GL4;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLDrawable;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.GLException;
import javax.media.opengl.GLProfile;
import javax.media.opengl.glu.GLU;
import javax.media.opengl.glu.GLUtessellator;
import javax.media.opengl.glu.GLUtessellatorCallbackAdapter;

import processing.core.PApplet;
import processing.core.PConstants;

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

  public static final int LESS              = GL.GL_LESS;
  public static final int LESS_OR_EQUAL     = GL.GL_LEQUAL;
  public static final int COUNTER_CLOCKWISE = GL.GL_CCW;
  public static final int CLOCKWISE         = GL.GL_CW;  
  public static final int FRONT             = GL.GL_FRONT;
  public static final int BACK              = GL.GL_BACK;
  
  public static final int BLEND_EQ_ADD              = GL.GL_FUNC_ADD;
  public static final int BLEND_EQ_MIN              = GL2.GL_MIN;
  public static final int BLEND_EQ_MAX              = GL2.GL_MAX;
  public static final int BLEND_EQ_REVERSE_SUBTRACT = GL.GL_FUNC_REVERSE_SUBTRACT;
  
  public static final int REPLACE  = GL2.GL_REPLACE;
  public static final int MODULATE = GL2.GL_MODULATE;
  
  public static final int FLAT   = GL2.GL_FLAT;
  public static final int SMOOTH = GL2.GL_SMOOTH;
  
  public static final int TEXTURE_2D = GL.GL_TEXTURE_2D;
  public static final int RGB        = GL.GL_RGB;
  public static final int RGBA       = GL.GL_RGBA;
  public static final int ALPHA      = GL.GL_ALPHA;
  
  public static final int NEAREST              = GL.GL_NEAREST;
  public static final int LINEAR               = GL.GL_LINEAR;
  public static final int LINEAR_MIPMAP_LINEAR = GL.GL_LINEAR_MIPMAP_LINEAR;
  
  public static final int CLAMP_TO_EDGE = GL.GL_CLAMP_TO_EDGE;
  public static final int REPEAT        = GL.GL_REPEAT;
  
  public static final int RGBA8 = GL.GL_RGBA8;  
  public static final int DEPTH_24BIT_STENCIL_8BIT = GL.GL_DEPTH24_STENCIL8;
  
  public static final int DEPTH_16BIT = GL.GL_DEPTH_COMPONENT16;
  public static final int DEPTH_24BIT = GL.GL_DEPTH_COMPONENT24;
  public static final int DEPTH_32BIT = GL.GL_DEPTH_COMPONENT32;    
  
  public static final int STENCIL_1BIT = GL.GL_STENCIL_INDEX1; 
  public static final int STENCIL_4BIT = GL.GL_STENCIL_INDEX4;
  public static final int STENCIL_8BIT = GL.GL_STENCIL_INDEX8;   
  
  public static final int FRAMEBUFFER_COMPLETE                      = GL.GL_FRAMEBUFFER_COMPLETE;    
  public static final int FRAMEBUFFER_INCOMPLETE_ATTACHMENT         = GL.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT;
  public static final int FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT = GL.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT;
  public static final int FRAMEBUFFER_INCOMPLETE_DIMENSIONS         = GL.GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS;      
  public static final int FRAMEBUFFER_INCOMPLETE_FORMATS            = GL.GL_FRAMEBUFFER_INCOMPLETE_FORMATS;
  public static final int FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER        = -1;
  public static final int FRAMEBUFFER_INCOMPLETE_READ_BUFFER        = -1;    
  public static final int FRAMEBUFFER_UNSUPPORTED                   = GL.GL_FRAMEBUFFER_UNSUPPORTED;
    
  public static final int STATIC_DRAW  = GL.GL_STATIC_DRAW;
  public static final int DYNAMIC_DRAW = GL.GL_DYNAMIC_DRAW;
  public static final int STREAM_DRAW  = GL2.GL_STREAM_DRAW;
  
  public static final int TRIANGLE_FAN   = GL.GL_TRIANGLE_FAN;
  public static final int TRIANGLE_STRIP = GL.GL_TRIANGLE_STRIP;
  public static final int TRIANGLES      = GL.GL_TRIANGLES;  
  
  public static final int TESS_WINDING_NONZERO = GLU.GLU_TESS_WINDING_NONZERO;
  public static final int TESS_WINDING_ODD = GLU.GLU_TESS_WINDING_ODD;
  
  // Rendering pipeline modes
  public static final int FIXED    = 0;
  public static final int PROG_GL2 = 1;
  public static final int PROG_GL3 = 2;
  public static final int PROG_GL4 = 3;  
  
  /** Pipeline mode: FIXED, PROG_GL2, PROG_GL3 or PROG_GL4 */
  public int pipeline;
  
  /** Basic GL functionality, common to all profiles */
  public GL gl;
  
  public GL2 gl2;
  
  /** Advanced GL functionality (usually, things available as extensions in JOGL1).
   * This profile is the intersection between GL 2.0 and GL 3.0 */
  public GL2GL3 gl2x;
  
  /** Fixed GL pipeline, with the functionality common to the GL2 desktop and GLES 1.1 profiles */
  public GL2ES1 gl2f;
  
  /** Basic programmable GL pipeline: intersection of desktop GL3, GL2 and embedded ES2 profile */
  public GL2ES2 gl2p;
  
  /** GL3 programmable pipeline, not backwards compatible with GL2 fixed */
  public GL3 gl3p;
  
  /** GL4 programmable pipeline, not backwards compatible with GL2 fixed */
  public GL4 gl4p;
  
  public GLU glu;
  
  /** Selected GL profile */
  public GLProfile profile;
  
  /** The capabilities of the OpenGL rendering surface */
  public GLCapabilities capabilities;  
  
  /** The rendering surface */
  public GLDrawable drawable;   
  
  /** The rendering context (holds rendering state info) */
  public GLContext context;  
  
  public PGraphicsOpenGL pg;
  
  public boolean initialized;
  
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
  static public void startup(boolean beforeUI) {
    try {
      GLProfile.initSingleton(beforeUI);
    } catch (Exception e) {
      e.printStackTrace();
    }      
  }
  
  static public void shutdown() {
    GLProfile.shutdown();    
  }
  
  public void updatePrimary() {
    gl = context.getGL();       
    
    if (pipeline == PROG_GL4) {
      gl4p = gl.getGL4();
      gl3p = gl4p;
      gl2p = gl4p;        
      gl2f = null;
    } else if (pipeline == PROG_GL3) {     
      gl4p = null;
      gl3p = gl.getGL3();
      gl2p = gl3p;
      gl2f = null;        
    } else if (pipeline == PROG_GL2) { 
      gl4p = null;
      gl3p = null;
      gl2p = gl.getGL2ES2();
      gl2f = null;        
    } else if (pipeline == FIXED) {
      gl4p = null;
      gl3p = null;
      gl2p = null;
      gl2f = gl.getGL2ES1();
      gl2 = gl.getGL2();
    }
    
    try {
      gl2x = gl.getGL2GL3();
    } catch (GLException e) {}    
  }
  
  public void updateOffscreen(PGL primary) {
    gl   = primary.gl;       
    gl4p = primary.gl4p;
    gl3p = primary.gl3p;
    gl2p = primary.gl2p;        
    gl2f = primary.gl2f;
  }
  
  
  public void initPrimarySurface(int antialias) {
    if (pg.parent.online) {
      // RCP Application (Applet's, Webstart, Netbeans, ..) using JOGL may not 
      // be able to initialize JOGL before the first UI action, so initSingleton()
      // is called with its argument set to false.
      GLProfile.initSingleton(false);
    } else {
      if (PApplet.platform == PConstants.LINUX) {
        // Special case for Linux, since the multithreading issues described for
        // example here:
        // http://forum.jogamp.org/QtJambi-JOGL-Ubuntu-Lucid-td909554.html
        // have not been solved yet (at least for stable release b32 of JOGL2).
        GLProfile.initSingleton(false);
      } else { 
        GLProfile.initSingleton(true);
      }
    }
    
    profile = null;      
    
    profile = GLProfile.getDefault();
    
    //profile = GLProfile.get(GLProfile.GL2ES1);    
    //profile = GLProfile.get(GLProfile.GL4bc);
    //profile = GLProfile.getMaxProgrammable();    
    pipeline = PGL.FIXED; 

    /*
    // Profile auto-selection disabled for the time being.
    // TODO: Implement programmable pipeline :-)
    try {
      profile = GLProfile.get(GLProfile.GL4);
      pipeline = PROG_GL4;
    } catch (GLException e) {}   
    
    if (profile == null) {
      try {
        profile = GLProfile.get(GLProfile.GL3);
        pipeline = PROG_GL3;
      } catch (GLException e) {}           
    }
    
    if (profile == null) {
      try {
        profile = GLProfile.get(GLProfile.GL2ES2);
        pipeline = PROG_GL2;
      } catch (GLException e) {}           
    }

    if (profile == null) {
      try {
        profile = GLProfile.get(GLProfile.GL2ES1);
        pipeline = FIXED;
      } catch (GLException e) {}
    }
    */      
          
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

    // Getting the native window:
    // http://www.java-gaming.org/index.php/topic,21559.0.html
    AWTGraphicsScreen screen = (AWTGraphicsScreen)AWTGraphicsScreen.createDefault();
    AWTGraphicsConfiguration config = (AWTGraphicsConfiguration)GraphicsConfigurationFactory
        .getFactory(AWTGraphicsDevice.class).chooseGraphicsConfiguration(capabilities, capabilities, null, screen);
    NativeWindow win = NativeWindowFactory.getNativeWindow(pg.parent, config);    
    
    // With the native window we get the drawable and context:
    GLDrawableFactory factory = GLDrawableFactory.getFactory(profile);
    drawable = factory.createGLDrawable(win);
    context = drawable.createContext(null);
    
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
  protected void detainContext() {
    try {
      while (context.makeCurrent() == GLContext.CONTEXT_NOT_CURRENT) {
        Thread.sleep(10);
      }
    } catch (InterruptedException e) {
      e.printStackTrace();
    }  
  }
  
  /**
   * Release the context, otherwise the AWT lock on X11 will not be released
   */
  public void releaseContext() {
    context.release();
  }  
  
  public void destroyContext() {
    context.destroy();
    context = null;    
  }
  
  public boolean contextIsCurrent(Context other) {
    return other.same(context);
  }
  
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
  
  public void beginOnscreenDraw() {

  }
  
  public void endOnscreenDraw() {
    if (drawable != null) {
      drawable.swapBuffers();        
    }    
  }
  
  public void beginOffscreenDraw() {

  }
  
  public void endOffscreenDraw() {
    
  }
  
  public boolean canDraw() {
    return pg.parent.isDisplayable();    
  }
  
  public void requestDraw() {
        
  }
  
  ///////////////////////////////////////////////////////////////////////////////////
  
  // Caps query
  
  public String getVendorString() {
    return gl.glGetString(GL.GL_VENDOR);  
  }
  
  public String getRendererString() {
    return gl.glGetString(GL.GL_RENDERER);  
  }
  
  public String getVersionString() {
    return gl.glGetString(GL.GL_VERSION);  
  }
  
  public String getExtensionsString() {
    return gl.glGetString(GL.GL_EXTENSIONS); 
  }
  
  public boolean isNpotTexSupported() {
    // Better way to check for extensions and related functions (taken from jMonkeyEngine):
    // renderbufferStorageMultisample = gl.isExtensionAvailable("GL_EXT_framebuffer_multisample") && 
    //                                  gl.isFunctionAvailable("glRenderbufferStorageMultisample");    
    // For more details on GL properties initialization in jMonkey using JOGL2, take a look at:
    // http://code.google.com/p/jmonkeyengine/source/browse/branches/jme3/src/jogl2/com/jme3/renderer/jogl/JoglRenderer.java
    
    String ext = gl.glGetString(GL.GL_EXTENSIONS);
    if (-1 < ext.indexOf("texture_non_power_of_two")) {
      return true;
    }
    return false;    
  }
  
  public boolean hasMipmapGeneration() {
    String ext = gl.glGetString(GL.GL_EXTENSIONS);
    if (-1 < ext.indexOf("generate_mipmap")) {
      return true;
    }    
    return false;
  }

  public boolean isMatrixGetSupported() {
    String ext = gl.glGetString(GL.GL_EXTENSIONS);
    if (-1 < ext.indexOf("matrix_get")) {
      return true;
    }
    return false;
  }
  
  public boolean isTexenvCrossbarSupported() {
    String ext = gl.glGetString(GL.GL_EXTENSIONS);
    if (-1 < ext.indexOf("texture_env_crossbar")) {
      return true;
    }    
    return false;
  }

  public boolean isVboSupported() {
    String ext = gl.glGetString(GL.GL_EXTENSIONS);
    if (-1 < ext.indexOf("vertex_buffer_object")) {
      return true;
    }    
    return false;
  }

  public boolean isFboSupported() {
    String ext = gl.glGetString(GL.GL_EXTENSIONS);
    if (-1 < ext.indexOf("framebuffer_object")) {
      return true;
    }    
    return false;
  }

  public boolean isFboMultisampleSupported() {
    String ext = gl.glGetString(GL.GL_EXTENSIONS);
    if (-1 < ext.indexOf("framebuffer_multisample")) {
      return true;
    }      
    return false;
  }
  
  public boolean isBlendEqSupported() {
    return true;
  }  
  
  public int getMaxTexureSize() {
    int temp[] = new int[1];    
    gl.glGetIntegerv(GL.GL_MAX_TEXTURE_SIZE, temp, 0);
    return temp[0];    
  }
  
  public int getMaxAliasedLineWidth() {
    int temp[] = new int[2];
    gl.glGetIntegerv(GL.GL_ALIASED_LINE_WIDTH_RANGE, temp, 0);
    return temp[1];
  }
  
  public int getMaxAliasedPointSize() {
    int temp[] = new int[2];
    gl.glGetIntegerv(GL.GL_ALIASED_POINT_SIZE_RANGE, temp, 0);
    return temp[1];    
  }
  
  public int getMaxTextureUnits() {
    // The maximum number of texture units only makes sense in the
    // fixed pipeline.
    int temp[] = new int[1];
    gl.glGetIntegerv(GL2.GL_MAX_TEXTURE_UNITS, temp, 0);
    return temp[0];    
  }  
  
  public void getNumSamples(int[] num) {
    gl.glGetIntegerv(GL.GL_SAMPLES, num, 0);    
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
  
  // Errors
  
  public int getError() {
    return gl.glGetError();
  }
  
  public String getErrorString(int err) {
    return glu.gluErrorString(err);
  }

  /////////////////////////////////////////////////////////////////////////////////
  
  // Rendering options
  
  public void enableDepthTest() {
    gl.glEnable(GL.GL_DEPTH_TEST);
  }

  public void disableDepthTest() {
    gl.glDisable(GL.GL_DEPTH_TEST);
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
    gl2f.glShadeModel(model);
  }
  
  public void setFrontFace(int mode) {
    gl.glFrontFace(mode);
  }
  
  public void enableMultisample() {
    gl2f.glEnable(GL2.GL_MULTISAMPLE);  
  }
  
  public void disableMultisample() {
    gl2f.glDisable(GL2.GL_MULTISAMPLE);  
  }

  public void enablePointSmooth() {
    gl2f.glEnable(GL2.GL_POINT_SMOOTH);  
  }
  
  public void disablePointSmooth() {
    gl2f.glDisable(GL2.GL_POINT_SMOOTH);  
  }

  public void enableLineSmooth() {
    gl2f.glEnable(GL2.GL_LINE_SMOOTH);  
  }
  
  public void disableLineSmooth() {
    gl2f.glDisable(GL2.GL_LINE_SMOOTH);  
  }  
  
  public void enablePolygonSmooth() {
    gl2f.glEnable(GL2.GL_POLYGON_SMOOTH);  
  }
  
  public void disablePolygonSmooth() {
    gl2f.glDisable(GL2.GL_POLYGON_SMOOTH);
  }    
  
  public void enableColorMaterial() {
    gl2f.glEnable(GL2.GL_COLOR_MATERIAL);    
  }

  public void disableColorMaterial() {
    gl2f.glDisable(GL2.GL_COLOR_MATERIAL);    
  }  
  
  public void enableNormalization() {
    gl2f.glEnable(GL2.GL_NORMALIZE);  
  }

  public void disableNormalization() {
    gl2f.glDisable(GL2.GL_NORMALIZE);  
  }  
  
  public void enableRescaleNormals() {
    gl2f.glEnable(GL2.GL_RESCALE_NORMAL);
  }

  public void disableRescaleNormals() {
    gl2f.glDisable(GL2.GL_RESCALE_NORMAL);
  }  
  
  
  /////////////////////////////////////////////////////////////////////////////////
  
  // Vertex arrays    
  
  public void genVertexArray(int[] id) {
    gl2x.glGenVertexArrays(1, id, 0);  
  }
  
  public void delVertexArray(int[] id) {
    gl2x.glDeleteVertexArrays(1, id, 0);
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
    gl2f.glEnable(target);
  }
  
  public void setActiveTexUnit(int tu) {
    gl2f.glActiveTexture(GL.GL_TEXTURE0 + tu);
  }
  
  public void bindTexture(int target, int id) {
    gl2f.glBindTexture(target, id);
  }

  public void unbindTexture(int target) {
    gl2f.glBindTexture(target, 0);
  }  
  
  public void disableTexturing(int target) {
    gl2f.glDisable(target);
  }    
  
  public void initTex(int target, int format, int w, int h) {
    gl.glTexImage2D(target, 0, format, w, h, 0, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, null);
  }

  public void copyTexImage(Buffer image, int target, int format, int level, int w, int h) {
    gl.glTexImage2D(target, level, format, w, h, 0, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, image);
  }  
  
  public void copyTexSubImage(Buffer image, int target, int level, int x, int y, int w, int h) {
    gl.glTexSubImage2D(target, level, x, y, w, h, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, image);
  }

  public void copyTexSubImage(int[] pixels, int target, int level, int x, int y, int w, int h) {
    gl.glTexSubImage2D(target, level, x, y, w, h, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, IntBuffer.wrap(pixels));
  }  
  
  public void setTexEnvironmentMode(int mode) {
    gl2f.glTexEnvi(GL2.GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_MODE, mode);   
  }
  
  public void enableTexMipmapGen(int target) {
    gl.glTexParameteri(target, GL2.GL_GENERATE_MIPMAP, GL.GL_TRUE);
  }

  public void disableTexMipmapGen(int target) {
    gl.glTexParameteri(target, GL2.GL_GENERATE_MIPMAP, GL.GL_FALSE);
  }  
  
  public void setTexMinFilter(int target, int filter) {
    gl.glTexParameteri(target, GL.GL_TEXTURE_MIN_FILTER, filter); 
  }
  
  public void setTexMagFilter(int target, int filter) {
    gl.glTexParameteri(target, GL.GL_TEXTURE_MAG_FILTER, filter);
  }
  
  public void setTexWrapS(int target, int wrap) {
    gl.glTexParameteri(target, GL.GL_TEXTURE_WRAP_S, wrap);
  }
  
  public void setTexWrapT(int target, int wrap) {
    gl.glTexParameteri(target, GL.GL_TEXTURE_WRAP_T, wrap); 
  }  
  
  /////////////////////////////////////////////////////////////////////////////////
  
  // Vertex Buffers

  public void genBuffer(int[] id) {
    gl.glGenBuffers(1, id, 0);  
  }
  
  public void delBuffer(int[] id) {
    gl.glDeleteBuffers(1, id, 0);  
  }

  public void bindVertexBuffer(int id) {
    gl2f.glBindBuffer(GL.GL_ARRAY_BUFFER, id);
  }
  
  public void initVertexBuffer(int size, int mode) {
    gl2f.glBufferData(GL.GL_ARRAY_BUFFER, size * SIZEOF_FLOAT, null, mode);  
  }
  
  public void copyVertexBufferData(float[] data, int size, int mode) {
    gl2f.glBufferData(GL.GL_ARRAY_BUFFER, size * SIZEOF_FLOAT, FloatBuffer.wrap(data, 0, size), mode);     
  }
  
  public void copyVertexBufferData(float[] data, int offset, int size, int mode) {
    gl2f.glBufferData(GL.GL_ARRAY_BUFFER, size * SIZEOF_FLOAT, FloatBuffer.wrap(data, offset, size), mode);     
  }  
  
  public void copyVertexBufferSubData(float[] data, int offset, int size, int mode) {
    gl2f.glBufferSubData(GL.GL_ARRAY_BUFFER, offset * SIZEOF_FLOAT, size * SIZEOF_FLOAT, FloatBuffer.wrap(data, 0, size));    
  }
  
  public void setVertexFormat(int size, int stride, long offset) {
    gl2f.glVertexPointer(size, GL.GL_FLOAT, stride, offset);
  }
  
  public void setColorFormat(int size, int stride, long offset) {
    gl2f.glColorPointer(size, GL.GL_FLOAT, stride, offset);
  }
  
  public void setNormalFormat(int size, int stride, long offset) {
    gl2f.glNormalPointer(GL.GL_FLOAT, stride, offset);
  }
  
  public void setTexCoordFormat(int size, int stride, long offset) {
    gl2f.glTexCoordPointer(size, GL.GL_FLOAT, stride, offset);
  }
  
  public void unbindVertexBuffer() {
    gl2f.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
  }
  
  public void bindIndexBuffer(int id) {
    gl2f.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, id);
  }
  
  public void initIndexBuffer(int size, int mode) {
    gl2f.glBufferData(GL.GL_ELEMENT_ARRAY_BUFFER, size * SIZEOF_INT, null, mode);  
  }
  
  public void copyIndexBufferData(int[] data, int size, int mode) {
    gl2f.glBufferData(GL.GL_ELEMENT_ARRAY_BUFFER, size * SIZEOF_INT, IntBuffer.wrap(data, 0, size), mode);     
  }
  
  public void copyIndexBufferData(int[] data, int offset, int size, int mode) {
    gl2f.glBufferData(GL.GL_ELEMENT_ARRAY_BUFFER, size * SIZEOF_INT, IntBuffer.wrap(data, offset, size), mode);     
  }

  public void copyIndexBufferSubData(int[] data, int offset, int size, int mode) {
    gl2f.glBufferSubData(GL.GL_ELEMENT_ARRAY_BUFFER, offset * SIZEOF_INT, size * SIZEOF_INT, IntBuffer.wrap(data, 0, size));
  }  
  
  public void renderIndexBuffer(int size) {
    gl2f.glDrawElements(GL.GL_TRIANGLES, size, GL.GL_UNSIGNED_INT, 0);
  }

  public void renderIndexBuffer(int offset, int size) {
    gl2f.glDrawElements(GL.GL_TRIANGLES, size, GL.GL_UNSIGNED_INT, offset * SIZEOF_INT);    
  }
    
  public void unbindIndexBuffer() {
    gl2f.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, 0);
  }
  
  public void enableVertexArrays() {
    gl2f.glEnableClientState(GL2.GL_VERTEX_ARRAY);    
  }

  public void enableColorArrays() {
    gl2f.glEnableClientState(GL2.GL_COLOR_ARRAY);    
  }

  public void enableNormalArrays() {
    gl2f.glEnableClientState(GL2.GL_NORMAL_ARRAY);  
  }

  public void enableTexCoordArrays() {
    gl2f.glEnableClientState(GL2.GL_TEXTURE_COORD_ARRAY);  
  }  
  
  public void disableVertexArrays() {
    gl2f.glDisableClientState(GL2.GL_VERTEX_ARRAY);    
  }

  public void disableColorArrays() {
    gl2f.glDisableClientState(GL2.GL_COLOR_ARRAY);    
  }

  public void disableNormalArrays() {
    gl2f.glDisableClientState(GL2.GL_NORMAL_ARRAY);  
  }
  
  public void disableTexCoordArrays() {
    gl2f.glDisableClientState(GL2.GL_TEXTURE_COORD_ARRAY);  
  }    
  
  public void enableAttribsArray(int loc) {
    gl2x.glEnableVertexAttribArray(loc);
  }
  
  public void setAttribsFormat(int loc, int size, int stride, long offset) {
    gl2x.glVertexAttribPointer(loc, size, GL.GL_FLOAT, false, stride, offset);
  }

  public void disableAttribsArray(int loc) {
    gl2x.glDisableVertexAttribArray(loc);
  }  
  
  public ByteBuffer mapVertexBuffer() {  
    return gl2f.glMapBuffer(GL.GL_ARRAY_BUFFER, GL2.GL_READ_WRITE);
  }
  
  public ByteBuffer mapVertexBufferRange(int offset, int length) {
    return gl2x.glMapBufferRange(GL.GL_ARRAY_BUFFER, offset, length, GL2.GL_READ_WRITE);    
  }
  
  public void unmapVertexBuffer() {
    gl2f.glUnmapBuffer(GL.GL_ARRAY_BUFFER);
  }

  public ByteBuffer mapIndexBuffer() {  
    return gl2f.glMapBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, GL2.GL_READ_WRITE);
  }
  
  public ByteBuffer mapIndexBufferRange(int offset, int length) {
    return gl2x.glMapBufferRange(GL.GL_ELEMENT_ARRAY_BUFFER, offset, length, GL2.GL_READ_WRITE);    
  }
  
  public void unmapIndexBuffer() {
    gl2f.glUnmapBuffer(GL.GL_ELEMENT_ARRAY_BUFFER);
  }  
  
  /////////////////////////////////////////////////////////////////////////////////
  
  // Framebuffers, renderbuffers  
  
  public void genFramebuffer(int[] id) {
    gl.glGenFramebuffers(1, id, 0);    
  }
  
  public void delFramebuffer(int[] id) {
    gl.glDeleteFramebuffers(1, id, 0);    
  }
  
  public void genRenderbuffer(int[] id) {
    gl.glGenRenderbuffers(1, id, 0);    
  }
  
  public void delRenderbuffer(int[] id) {
    gl.glGenRenderbuffers(1, id, 0);    
  }
  
  public void bindFramebuffer(int id) {
    gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, id);
  }
  
  public void bindReadFramebuffer(int id) {
    gl.glBindFramebuffer(GL2.GL_READ_FRAMEBUFFER, id);  
  }

  public void bindWriteFramebuffer(int id) {
    gl.glBindFramebuffer(GL2.GL_DRAW_FRAMEBUFFER, id);  
  }  
  
  public void copyFramebuffer(int srcW, int srcH, int destW, int destH) {
    gl2x.glBlitFramebuffer(0, 0, srcW, srcH, 0, 0, destW, destH, GL.GL_COLOR_BUFFER_BIT, GL.GL_NEAREST);    
  }
  
  public void cleanFramebufferTexture(int fb) {
    gl.glFramebufferTexture2D(GL.GL_FRAMEBUFFER, GL.GL_COLOR_ATTACHMENT0 + fb, GL.GL_TEXTURE_2D, 0, 0);  
  }
  
  public void setFramebufferTexture(int fb, int target, int id) {
    gl.glFramebufferTexture2D(GL.GL_FRAMEBUFFER, GL.GL_COLOR_ATTACHMENT0 + fb, target, id, 0);
  }

  public void bindRenderbuffer(int id) {
    gl.glBindRenderbuffer(GL.GL_RENDERBUFFER, id);
  }
    
  public void setRenderbufferNumSamples(int samples, int format, int w, int h) {
    gl2x.glRenderbufferStorageMultisample(GL.GL_RENDERBUFFER, samples, format, w, h);
  }
  
  public void setRenderbufferStorage(int format, int w, int h) {
    gl.glRenderbufferStorage(GL.GL_RENDERBUFFER, format, w, h);
  }
  
  public void setRenderbufferColorAttachment(int id) {
    gl.glFramebufferRenderbuffer(GL.GL_FRAMEBUFFER, GL.GL_COLOR_ATTACHMENT0, GL.GL_RENDERBUFFER, id);
  }  
  
  public void setRenderbufferDepthAttachment(int id) {
    gl.glFramebufferRenderbuffer(GL.GL_FRAMEBUFFER, GL.GL_DEPTH_ATTACHMENT, GL.GL_RENDERBUFFER, id);  
  }
  
  public void setRenderbufferStencilAttachment(int id) {
    gl.glFramebufferRenderbuffer(GL.GL_FRAMEBUFFER, GL.GL_STENCIL_ATTACHMENT, GL.GL_RENDERBUFFER, id);  
  }
  
  public int getFramebufferStatus() {
    return gl.glCheckFramebufferStatus(GL.GL_FRAMEBUFFER);
  }  

  /////////////////////////////////////////////////////////////////////////////////
  
  // Shaders  
  
  public void genProgram(int[] id) {
    id[0] = gl2x.glCreateProgram();    
  }
  
  public void delProgram(int[] id) {
    gl2x.glDeleteProgram(id[0]);  
  }
  
  public void genVertexShader(int[] id) {
    id[0] = gl2x.glCreateShader(GL2.GL_VERTEX_SHADER);    
  }
  
  public void delVertexShader(int[] id) {
    gl2x.glDeleteShader(id[0]);    
  }
  
  public void genFragmentShader(int[] id) {
    id[0] = gl2x.glCreateShader(GL2.GL_FRAGMENT_SHADER);    
  }
  
  public void delFragmentShader(int[] id) {
    gl2x.glDeleteShader(id[0]);    
  }  

  public void linkProgram(int prog) {
    gl2.glLinkProgram(prog);  
  }
  
  public void validateProgram(int prog) {
    gl2.glValidateProgram(prog);
  }
  
  public void startProgram(int prog) {
    gl2.glUseProgramObjectARB(prog);  
  }
  
  public void stopProgram() {
    gl2.glUseProgramObjectARB(0);  
  }  
  
  public int getAttribLocation(int prog, String name) {
    return gl2.glGetAttribLocation(prog, name);  
  }
  
  public int getUniformLocation(int prog, String name) {
    return gl2.glGetUniformLocation(prog, name);  
  }  
  
  public void setIntUniform(int loc, int value) {
    gl2.glUniform1i(loc, value);  
  }
  
  public void setFloatUniform(int loc, float value) {
    gl2.glUniform1f(loc, value);  
  }    
  
  public void setFloatUniform(int loc, float value0, float value1) {
    gl2.glUniform2f(loc, value0, value1);  
  }
  
  public void setFloatUniform(int loc, float value0, float value1, float value2) {
    gl2.glUniform3f(loc, value0, value1, value2);  
  }
  
  public void setFloatUniform(int loc, float value0, float value1, float value2, float value3) {
    gl2.glUniform4f(loc, value0, value1, value2, value3);  
  }
  
  public void setMatUniform(int loc, float m00, float m01,
                                     float m10, float m11) {
    float[] mat = new float[4];
    mat[0] = m00; mat[4] = m01;
    mat[1] = m10; mat[5] = m11;
    gl2.glUniformMatrix2fv(loc, 1, false, mat, 0);
  }
  
  public void setMatUniform(int loc, float m00, float m01, float m02,
                                     float m10, float m11, float m12,
                                     float m20, float m21, float m22) {
    float[] mat = new float[9];
    mat[0] = m00; mat[4] = m01; mat[ 8] = m02;
    mat[1] = m10; mat[5] = m11; mat[ 9] = m12;
    mat[2] = m20; mat[6] = m21; mat[10] = m22;    
    gl2.glUniformMatrix3fv(loc, 1, false, mat, 0);    
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
    gl2.glUniformMatrix4fv(loc, 1, false, mat, 0);       
  }
  
  public void setFloatAttrib(int loc, float value) {
    gl2.glVertexAttrib1f(loc, value);  
  }
  
  public void setFloatAttrib(int loc, float value0, float value1) {
    gl2.glVertexAttrib2f(loc, value0, value1);  
  }  
  
  public void setFloatAttrib(int loc, float value0, float value1, float value2) {
    gl2.glVertexAttrib3f(loc, value0, value1, value2);  
  }    

  public void setFloatAttrib(int loc, float value0, float value1, float value2, float value3) {
    gl2.glVertexAttrib4f(loc, value0, value1, value2, value3);  
  }
  
  public void setShaderSource(int id, String source) {
    gl2.glShaderSource(id, 1, new String[] { source }, (int[]) null, 0);    
  }
  
  public void compileShader(int id) {
    gl2.glCompileShader(id);    
  }
  
  public void attachShader(int prog, int shader) {
    gl2.glAttachObjectARB(prog, shader);  
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
  
  // Viewport  
    
  public void getViweport(int[] viewport) {
    gl.glGetIntegerv(GL.GL_VIEWPORT, viewport, 0);    
  }
  
  public void setViewport(int[] viewport) {
    gl.glViewport(viewport[0], viewport[1], viewport[2], viewport[3]);
  }

  /////////////////////////////////////////////////////////////////////////////////
  
  // Clipping (scissor test)
  
  public void enableClipping() {
    gl.glEnable(GL.GL_SCISSOR_TEST);
  }

  public void disableClipping() {
    gl.glDisable(GL.GL_SCISSOR_TEST);
  }  
  
  public void setClipRect(int x, int y, int w, int h) {
    gl.glScissor(x, y, w, h);
  }
  
  
  /////////////////////////////////////////////////////////////////////////////////
  
  // Matrices, transformations
  
  public void setProjectionMode() {
    gl2f.glMatrixMode(GL2.GL_PROJECTION);
  }
  
  public void setModelviewMode() {
    gl2f.glMatrixMode(GL2.GL_MODELVIEW);
  }
    
  public void pushMatrix() {
    gl2f.glPushMatrix();  
  }

  public void popMatrix() {
    gl2f.glPopMatrix();  
  }  
  
  public void loadIdentity() {
    gl2f.glLoadIdentity();    
  }
  
  public void multMatrix(float[] mat) {
    gl2f.glMultMatrixf(mat, 0);
  }

  public void loadMatrix(float[] mat) {
    gl2f.glLoadMatrixf(mat, 0);
  }    
  
  public void translate(float tx, float ty, float tz) {
    gl2f.glTranslatef(tx, ty, tz);  
  }
  
  public void rotate(float angle, float vx, float vy, float vz) {
    gl2f.glRotatef(PApplet.degrees(angle), vx, vy, vz);    
  }
  
  public void scale(float sx, float sy, float sz) {
    gl2f.glScalef(sx, sy, sz);
  }  
  
  public void setOrthographicProjection(float left, float right, float bottom, float top, float near, float far) {
    gl2f.glOrthof(left, right, bottom, top, near, far);
  }  
  
  /////////////////////////////////////////////////////////////////////////////////
  
  // Materials
  
  public void setMaterialAmbient(float[] color) {
    gl2f.glMaterialfv(GL.GL_FRONT_AND_BACK, GL2.GL_AMBIENT, color, 0);
  }
  
  public void setMaterialSpecular(float[] color) {
    gl2f.glMaterialfv(GL.GL_FRONT_AND_BACK, GL2.GL_SPECULAR, color, 0);
  }

  public void setMaterialEmission(float[] color) {
    gl2f.glMaterialfv(GL2.GL_FRONT_AND_BACK, GL2.GL_EMISSION, color, 0);
  }  
  
  public void setMaterialShininess(float shine) {
    gl2f.glMaterialf(GL.GL_FRONT_AND_BACK, GL2.GL_SHININESS, shine);
  }
  
  public void setColor(float r, float g, float b, float a) {
    gl2f.glColor4f(r, g, b, a);
  }  
  
  /////////////////////////////////////////////////////////////////////////////////
  
  // Lights
  
  public void enableLighting() {
    gl2f.glEnable(GL2.GL_LIGHTING);
  }
  
  public void disableLighting() {
    gl2f.glDisable(GL2.GL_LIGHTING);
  }  

  public void setTwoSidedLightModel() {
    gl2f.glLightModelf(GL2.GL_LIGHT_MODEL_TWO_SIDE, 0);
  }
  
  public void setDefaultAmbientLight(float[] color) {
    gl2f.glLightModelfv(GL2.GL_LIGHT_MODEL_AMBIENT, color, 0);
  }  
  
  public void enableLight(int light) {
    gl2f.glEnable(GL2.GL_LIGHT0 + light);
  }

  public void disableLight(int light) {
    gl2f.glDisable(GL2.GL_LIGHT0 + light);
  }  

  public void setLightPosition(int light, float[] pos) {
    gl2f.glLightfv(GL2.GL_LIGHT0 + light, GL2.GL_POSITION, pos, 0);
  }
  
  public void setAmbientLight(int light, float[] color) {
    gl2f.glLightfv(GL2.GL_LIGHT0 + light, GL2.GL_AMBIENT, color, 0);
  }
    
  public void setDiffuseLight(int light, float[] color) {
    gl2f.glLightfv(GL2.GL_LIGHT0 + light, GL2.GL_DIFFUSE, color, 0);
  }

  public void setSpecularLight(int light, float[] color) {
    gl2f.glLightfv(GL2.GL_LIGHT0 + light, GL2.GL_SPECULAR, color, 0);
  }
  
  public void setLightDirection(int light, float[] dir) {
    // The w component of lightNormal[num] is zero, so the light is considered as
    // a directional source because the position effectively becomes a direction
    // in homogeneous coordinates:
    // http://glprogramming.com/red/appendixf.html 
    dir[3] = 0;
    gl2f.glLightfv(GL2.GL_LIGHT0 + light, GL2.GL_POSITION, dir, 0);    
  }
    
  public void setSpotLightCutoff(int light, float cutoff) {
    gl2f.glLightf(GL2.GL_LIGHT0 + light, GL2.GL_SPOT_CUTOFF, cutoff);  
  }
  
  public void setSpotLightExponent(int light, float exp) {
    gl2f.glLightf(GL2.GL_LIGHT0 + light, GL2.GL_SPOT_EXPONENT, exp);      
  }
  
  public void setSpotLightDirection(int light, float[] dir) {
    gl2f.glLightfv(GL2.GL_LIGHT0 + light, GL2.GL_POSITION, dir, 0);
  }
  
  public void setLightConstantAttenuation(int light, float attn) {
    gl2f.glLightf(GL2.GL_LIGHT0 + light, GL2.GL_CONSTANT_ATTENUATION, attn);  
  }
  
  public void setLightLinearAttenuation(int light, float attn) {
    gl2f.glLightf(GL2.GL_LIGHT0 + light, GL2.GL_LINEAR_ATTENUATION, attn);  
  }
  
  public void setLightQuadraticAttenuation(int light, float attn) {
    gl2f.glLightf(GL2.GL_LIGHT0 + light, GL2.GL_QUADRATIC_ATTENUATION, attn);  
  }

  public void setNormal(float nx, float ny, float nz) {
    gl2f.glNormal3f(nx, ny, nz);    
  }  
  
  /////////////////////////////////////////////////////////////////////////////////
  
  // Blending  
  
  public void enableBlend() {
    gl.glEnable(GL.GL_BLEND);
  }
  
  public void setBlendEquation(int eq) {
    gl.glBlendEquation(eq);
  }
  
  public void setReplaceBlend() {
    gl.glBlendFunc(GL.GL_ONE, GL.GL_ZERO);
  }
  
  public void setDefaultBlend() {
    gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
  }
  
  public void setAdditiveBlend() {
    gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE);
  }
  
  public void setSubstractiveBlend() {
    gl.glBlendFunc(GL.GL_ONE_MINUS_DST_COLOR, GL.GL_ZERO);
  }
  
  public void setLightestBlend() {
    gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_DST_ALPHA);
  }
  
  public void setDarkestBlend() {
    gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_DST_ALPHA);
  }
  
  public void setDifferenceBlend() {
    gl.glBlendFunc(GL.GL_ONE, GL.GL_ONE);
  }
  
  public void setExclussionBlend() {
    gl.glBlendFunc(GL.GL_ONE_MINUS_DST_COLOR, GL.GL_ONE_MINUS_SRC_COLOR);
  }
  
  public void setMultiplyBlend() {
    gl.glBlendFunc(GL.GL_DST_COLOR, GL.GL_SRC_COLOR);
  }
  
  public void setScreenBlend() {
    gl.glBlendFunc(GL.GL_ONE_MINUS_DST_COLOR, GL.GL_ONE);
  }  
  
  /////////////////////////////////////////////////////////////////////////////////
  
  // Pixels  
  
  public void setReadBuffer(int buf) {
    gl2x.glReadBuffer(buf);
  }
  
  public void readPixels(Buffer buffer, int x, int y, int w, int h) {
    gl.glReadPixels(x, y, w, h, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, buffer);
  } 
  
  public void setDrawBuffer(int buf) {
    gl2x.glDrawBuffer(GL.GL_COLOR_ATTACHMENT0 + buf);
  }
  
  public void setClearColor(float r, float g, float b, float a) {
    gl.glClearColor(r, g, b, a);    
  }
  
  public void clearDepthBuffer() {
    gl.glClear(GL.GL_DEPTH_BUFFER_BIT);
  }
    
  public void clearStencilBuffer() {
    gl.glClear(GL.GL_STENCIL_BUFFER_BIT);
  }

  public void clearDepthAndStencilBuffers() {
    gl.glClear(GL.GL_DEPTH_BUFFER_BIT | GL.GL_STENCIL_BUFFER_BIT);
  }
  
  public void clearColorBuffer() {
    gl.glClear(GL.GL_COLOR_BUFFER_BIT);
  } 

  public void clearAllBuffers() {
    gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT | GL.GL_STENCIL_BUFFER_BIT); 
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
}
