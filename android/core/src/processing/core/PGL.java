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
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.microedition.khronos.opengles.*;
import android.opengl.GLU;

import processing.core.PApplet;

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
  
  public PGL() {    
    glu = new GLU();
  }
  
  void update(GL10 gl10) {
    gl = gl10;       
    
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
    return glu.gluErrorString(err);
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
  
  public void copyTexSubImage(Buffer image, int target, int x, int y, int w, int h) {
    gl.glTexSubImage2D(target, 0, x, y, w, h, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, image);
  }

  public void copyTexSubPixels(int[] pixels, int target, int x, int y, int w, int h) {
    gl.glTexSubImage2D(target, 0, x, y, w, h, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, IntBuffer.wrap(pixels));
  }  
  
  public void copyTexImage(Buffer image, int target, int format, int w, int h) {
    gl.glTexImage2D(target, 0, format, w, h, 0, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, image);
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
  
  public void setVertexFormat(int size, int stride, int offset) {
    gl11.glVertexPointer(size, GL11.GL_FLOAT, stride, offset);
  }
  
  public void setColorFormat(int size, int stride, int offset) {
    gl11.glColorPointer(size, GL11.GL_FLOAT, stride, offset);
  }
  
  public void setNormalFormat(int size, int stride, int offset) {
    gl11.glNormalPointer(GL11.GL_FLOAT, stride, offset);
  }
  
  public void setTexCoordFormat(int size, int stride, int offset) {
    gl11.glTexCoordPointer(size, GL11.GL_FLOAT, stride, offset);
  }
  
  public void unbindVertexBuffer() {
    gl11.glBindBuffer(GL11.GL_ARRAY_BUFFER, 0);
  }
  
  public void bindIndexBuffer(int id) {
    gl11.glBindBuffer(GL11.GL_ELEMENT_ARRAY_BUFFER, id);
  }
  
  public void initIndexBuffer(int size, int mode) {
    gl11.glBufferData(GL11.GL_ELEMENT_ARRAY_BUFFER, size * SIZEOF_INT, null, mode);  
  }
  
  public void copyIndexBufferData(int[] data, int size, int mode) {
    gl11.glBufferData(GL11.GL_ELEMENT_ARRAY_BUFFER, size * SIZEOF_INT, IntBuffer.wrap(data, 0, size), mode);     
  }
  
  public void copyIndexBufferData(int[] data, int offset, int size, int mode) {
    gl11.glBufferData(GL11.GL_ELEMENT_ARRAY_BUFFER, size * SIZEOF_INT, IntBuffer.wrap(data, offset, size), mode);     
  }

  public void copyIndexBufferSubData(int[] data, int offset, int size, int mode) {
    gl11.glBufferSubData(GL11.GL_ELEMENT_ARRAY_BUFFER, offset * SIZEOF_INT, size * SIZEOF_INT, IntBuffer.wrap(data, 0, size));
  }  
  
  public void renderIndexBuffer(int size) {
    gl11.glDrawElements(GL10.GL_TRIANGLES, size, GL10.GL_UNSIGNED_SHORT, 0);
  }

  public void renderIndexBuffer(int offset, int size) {
    gl11.glDrawElements(GL10.GL_TRIANGLES, size, GL10.GL_UNSIGNED_SHORT, offset * SIZEOF_INT);    
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
    gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
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
}
