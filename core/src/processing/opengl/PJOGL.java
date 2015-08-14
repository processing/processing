/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2011-12 Ben Fry and Casey Reas

  This library is free software; you can redistribute it and/or
  modify it under the terms of the GNU Lesser General Public
  License as published by the Free Software Foundation; either
  version 2.1 of the License, or (at your option) any later version.

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

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.PathIterator;
import java.io.IOException;
import java.net.URL;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL2ES1;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GL2ES3;
import com.jogamp.opengl.GL2GL3;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilitiesImmutable;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLDrawable;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.glu.GLUtessellator;
import com.jogamp.opengl.glu.GLUtessellatorCallbackAdapter;

import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PGraphics;
import processing.opengl.PGL;
import processing.opengl.PGraphicsOpenGL;


public class PJOGL extends PGL {
  // OpenGL profile to use (2, 3 or 4)
  public static int profile = 2;

  // The two windowing toolkits available to use in JOGL:
  public static final int AWT  = 0; // http://jogamp.org/wiki/index.php/Using_JOGL_in_AWT_SWT_and_Swing
  public static final int NEWT = 1; // http://jogamp.org/jogl/doc/NEWT-Overview.html

  // ........................................................

  // Public members to access the underlying GL objects and context

  /** Basic GL functionality, common to all profiles */
  public GL gl;

  /** GLU interface **/
  public GLU glu;

  /** The rendering context (holds rendering state info) */
  public GLContext context;

  // ........................................................

  // Additional parameters

  /** Time that the Processing's animation thread will wait for JOGL's rendering
   * thread to be done with a single frame.
   */
  protected static int DRAW_TIMEOUT_MILLIS = 500;

  // ........................................................

  // OS-specific configuration

  /*
  protected static int WINDOW_TOOLKIT;
  protected static int EVENTS_TOOLKIT;
  protected static boolean USE_JOGL_FBOLAYER;
  static {
    if (PApplet.platform == PConstants.WINDOWS) {
      // Using AWT on Windows because NEWT displays a black background while
      // initializing, and the cursor functions don't work. GLWindow has some
      // functions for basic cursor handling (hide/show):
      // GLWindow.setPointerVisible(false);
      // but apparently nothing to set the cursor icon:
      // https://jogamp.org/bugzilla/show_bug.cgi?id=409
      WINDOW_TOOLKIT = AWT;
      EVENTS_TOOLKIT = AWT;
      USE_FBOLAYER_BY_DEFAULT = false;
      USE_JOGL_FBOLAYER = false;
    } else if (PApplet.platform == PConstants.MACOSX) {
      // Note: The JOGL FBO layer (in 2.0.2) seems incompatible with NEWT.
      WINDOW_TOOLKIT = AWT;
      EVENTS_TOOLKIT = AWT;
      USE_FBOLAYER_BY_DEFAULT = true;
      USE_JOGL_FBOLAYER = true;
    } else if (PApplet.platform == PConstants.LINUX) {
      WINDOW_TOOLKIT = AWT;
      EVENTS_TOOLKIT = AWT;
      USE_FBOLAYER_BY_DEFAULT = false;
      USE_JOGL_FBOLAYER = false;
    } else if (PApplet.platform == PConstants.OTHER) {
      WINDOW_TOOLKIT = NEWT; // NEWT works on the Raspberry pi?
      EVENTS_TOOLKIT = NEWT;
      USE_FBOLAYER_BY_DEFAULT = false;
      USE_JOGL_FBOLAYER = false;
    }
  }
*/

//  protected static boolean USE_FBOLAYER_BY_DEFAULT = false;
//  protected static boolean USE_JOGL_FBOLAYER = false;

  // ........................................................

  // Protected JOGL-specific objects needed to access the GL profiles

  /** The capabilities of the OpenGL rendering surface */
  protected GLCapabilitiesImmutable capabilities;

  /** The rendering surface */
  protected GLDrawable drawable;

  /** GLES2 functionality (shaders, etc) */
  protected GL2ES2 gl2;

  /** GL3 interface */
  protected GL2GL3 gl3;

  /** GL2 desktop functionality (blit framebuffer, map buffer range,
   * multisampled renderbuffers) */
  protected GL2 gl2x;

  /** The AWT-OpenGL canvas */
//  protected GLCanvas canvasAWT;

  /** The NEWT window */
//  protected GLWindow windowNEWT;

  /** The NEWT-OpenGL canvas */
//  protected NewtCanvasAWT canvasNEWT;

  /** The listener that fires the frame rendering in Processing */
//  protected PGLListener listener;

  /** This countdown latch is used to maintain the synchronization between
   * Processing's drawing thread and JOGL's rendering thread */
//  protected CountDownLatch drawLatch = new CountDownLatch(0);

  /** Flag used to do request final display() call to make sure that the
   * buffers are properly swapped.
   */
//  protected boolean prevCanDraw = false;

  /** Stores exceptions that ocurred during drawing */
  protected Exception drawException;

  // ........................................................

  // JOGL's FBO-layer

  /** Back (== draw, current frame) buffer */
//  protected FBObject backFBO;
  /** Sink buffer, used in the multisampled case */
//  protected FBObject sinkFBO;
  /** Front (== read, previous frame) buffer */
//  protected FBObject frontFBO;
//  protected FBObject.TextureAttachment backTexAttach;
//  protected FBObject.TextureAttachment frontTexAttach;

//  protected boolean changedFrontTex = false;
//  protected boolean changedBackTex = false;

  // ........................................................

  // Utility arrays to copy projection/modelview matrices to GL

  protected float[] projMatrix;
  protected float[] mvMatrix;

  // ........................................................

  // Static initialization for some parameters that need to be different for
  // JOGL

  static {
    MIN_DIRECT_BUFFER_SIZE = 2;
    INDEX_TYPE             = GL.GL_UNSIGNED_SHORT;
  }


  ///////////////////////////////////////////////////////////////

  // Initialization, finalization


  public PJOGL(PGraphicsOpenGL pg) {
    super(pg);
    glu = new GLU();
  }


  /*
  @Override
  public Canvas getCanvas() {
    return canvas;
  }
*/


  protected void setFps(float fps) {
    if (!setFps || targetFps != fps) {
      if (60 < fps) {
        // Disables v-sync
        gl.setSwapInterval(0);
      } else if (30 < fps) {
        gl.setSwapInterval(1);
      } else {
        gl.setSwapInterval(2);
      }
      targetFps = currentFps = fps;
      setFps = true;
    }
  }


  /*
  @Override
  protected void initSurface(int antialias) {

    if (profile == null) {
      if (PROFILE == 2) {
        try {
          profile = GLProfile.getGL2ES1();
        } catch (GLException ex) {
          profile = GLProfile.getMaxFixedFunc(true);
        }
      } else if (PROFILE == 3) {
        try {
          profile = GLProfile.getGL2GL3();
        } catch (GLException ex) {
          profile = GLProfile.getMaxProgrammable(true);
        }
        if (!profile.isGL3()) {
          PGraphics.showWarning("Requested profile GL3 but is not available, got: " + profile);
        }
      } else if (PROFILE == 4) {
        try {
          profile = GLProfile.getGL4ES3();
        } catch (GLException ex) {
          profile = GLProfile.getMaxProgrammable(true);
        }
        if (!profile.isGL4()) {
          PGraphics.showWarning("Requested profile GL4 but is not available, got: " + profile);
        }
      } else throw new RuntimeException(UNSUPPORTED_GLPROF_ERROR);

      if (2 < PROFILE) {
        texVertShaderSource = convertVertexSource(texVertShaderSource, 120, 150);
        tex2DFragShaderSource = convertFragmentSource(tex2DFragShaderSource, 120, 150);
        texRectFragShaderSource = convertFragmentSource(texRectFragShaderSource, 120, 150);
      }
    }

    if (canvasAWT != null || canvasNEWT != null) {
      // Restarting...
      if (canvasAWT != null) {
        canvasAWT.removeGLEventListener(listener);
        pg.parent.removeListeners(canvasAWT);
        pg.parent.remove(canvasAWT);
      } else if (canvasNEWT != null) {
        windowNEWT.removeGLEventListener(listener);
        pg.parent.remove(canvasNEWT);
      }
      sinkFBO = backFBO = frontFBO = null;
    }

    // Setting up the desired capabilities;
    GLCapabilities caps = new GLCapabilities(profile);
    caps.setAlphaBits(REQUESTED_ALPHA_BITS);
    caps.setDepthBits(REQUESTED_DEPTH_BITS);
    caps.setStencilBits(REQUESTED_STENCIL_BITS);

    caps.setBackgroundOpaque(true);
    caps.setOnscreen(true);
    if (USE_FBOLAYER_BY_DEFAULT) {
      if (USE_JOGL_FBOLAYER) {
        caps.setPBuffer(false);
        caps.setFBO(true);
        if (1 < antialias) {
          caps.setSampleBuffers(true);
          caps.setNumSamples(antialias);
        } else {
          caps.setSampleBuffers(false);
        }
        fboLayerRequested = false;
      } else {
        caps.setPBuffer(false);
        caps.setFBO(false);
        caps.setSampleBuffers(false);
        fboLayerRequested = 1 < antialias;
      }
    } else {
      if (1 < antialias) {
        caps.setSampleBuffers(true);
        caps.setNumSamples(antialias);
      } else {
        caps.setSampleBuffers(false);
      }
      fboLayerRequested = false;
    }
    caps.setDepthBits(REQUESTED_DEPTH_BITS);
    caps.setStencilBits(REQUESTED_STENCIL_BITS);
    caps.setAlphaBits(REQUESTED_ALPHA_BITS);
    reqNumSamples = qualityToSamples(antialias);

    if (WINDOW_TOOLKIT == AWT) {
      canvasAWT = new GLCanvas(caps);

      if (RETINA) {
        canvasAWT.setSurfaceScale(new int[] { ScalableSurface.AUTOMAX_PIXELSCALE,
                                              ScalableSurface.AUTOMAX_PIXELSCALE });
        retf = 2;
      } else {
        canvasAWT.setSurfaceScale(new int[] { ScalableSurface.IDENTITY_PIXELSCALE,
                                              ScalableSurface.IDENTITY_PIXELSCALE });
      }

      canvasAWT.setBounds(0, 0, pg.width, pg.height);
      canvasAWT.setBackground(new Color(pg.backgroundColor, true));
      canvasAWT.setFocusable(true);

      pg.parent.setLayout(new BorderLayout());
      pg.parent.add(canvasAWT, BorderLayout.CENTER);
      canvasAWT.requestFocusInWindow();



      canvas = canvasAWT;
      canvasNEWT = null;
    } else if (WINDOW_TOOLKIT == NEWT) {
      windowNEWT = GLWindow.create(caps);
      canvasNEWT = new NewtCanvasAWT(windowNEWT);
      canvasNEWT.setBounds(0, 0, pg.width, pg.height);
      canvasNEWT.setBackground(new Color(pg.backgroundColor, true));
      canvasNEWT.setFocusable(true);

      pg.parent.setLayout(new BorderLayout());
      pg.parent.add(canvasNEWT, BorderLayout.CENTER);
      canvasNEWT.requestFocusInWindow();

      int[] reqSurfacePixelScale = new int[] { ScalableSurface.AUTOMAX_PIXELSCALE, ScalableSurface.AUTOMAX_PIXELSCALE };
      windowNEWT.setSurfaceScale(reqSurfacePixelScale);

      canvas = canvasNEWT;
      canvasAWT = null;
    }

    pg.parent.defaultSize = false;
    registerListeners();


    fboLayerCreated = false;
    fboLayerInUse = false;
    firstFrame = true;
    setFps = false;
  }
    */

/*
  @Override
  protected void reinitSurface() {
    sinkFBO = backFBO = frontFBO = null;
    fboLayerCreated = false;
    fboLayerInUse = false;
    firstFrame = true;
    pg.parent.defaultSize = false;
  }
*/

//  @Override
//  protected void registerListeners() {
//    if (WINDOW_TOOLKIT == AWT) {
//      pg.parent.addListeners(canvasAWT);
//
//      listener = new PGLListener();
//      canvasAWT.addGLEventListener(listener);
//    } else if (WINDOW_TOOLKIT == NEWT) {
//      if (EVENTS_TOOLKIT == NEWT) {
//        NEWTMouseListener mouseListener = new NEWTMouseListener();
//        windowNEWT.addMouseListener(mouseListener);
//        NEWTKeyListener keyListener = new NEWTKeyListener();
//        windowNEWT.addKeyListener(keyListener);
//        NEWTWindowListener winListener = new NEWTWindowListener();
//        windowNEWT.addWindowListener(winListener);
//      } else if (EVENTS_TOOLKIT == AWT) {
//        pg.parent.addListeners(canvasNEWT);
//      }
//
//      listener = new PGLListener();
//      windowNEWT.addGLEventListener(listener);
//    }
//
//    if (canvas != null) {
//      canvas.setFocusTraversalKeysEnabled(false);
//    }
//  }


//  @Override
//  protected void deleteSurface() {
//    super.deleteSurface();
//
//    if (canvasAWT != null) {
//      canvasAWT.removeGLEventListener(listener);
//      pg.parent.removeListeners(canvasAWT);
//    } else if (canvasNEWT != null) {
//      windowNEWT.removeGLEventListener(listener);
//    }
//  }

/*
  @Override
  protected int getReadFramebuffer() {
    if (fboLayerInUse) {
      return glColorFbo.get(0);
    } else if (capabilities.isFBO()) {
      return context.getDefaultReadFramebuffer();
    } else {
      return 0;
    }
  }


  @Override
  protected int getDrawFramebuffer() {
    if (fboLayerInUse) {
      if (1 < numSamples) {
        return glMultiFbo.get(0);
      } else {
        return glColorFbo.get(0);
      }
    } else if (capabilities.isFBO()) {
      return context.getDefaultDrawFramebuffer();
    } else {
      return 0;
    }
  }


  @Override
  protected int getDefaultDrawBuffer() {
    if (fboLayerInUse) {
      return COLOR_ATTACHMENT0;
    } else if (capabilities.isFBO()) {
      return GL.GL_COLOR_ATTACHMENT0;
    } else if (capabilities.getDoubleBuffered()) {
      return GL.GL_BACK;
    } else {
      return GL.GL_FRONT;
    }
  }


  @Override
  protected int getDefaultReadBuffer() {
    if (fboLayerInUse) {
      return COLOR_ATTACHMENT0;
    } else if (capabilities.isFBO()) {
      return GL.GL_COLOR_ATTACHMENT0;
    } else if (capabilities.getDoubleBuffered()) {
      return GL.GL_BACK;
    } else {
      return GL.GL_FRONT;
    }
  }


  @Override
  protected boolean isFBOBacked() {
    return super.isFBOBacked() || capabilities.isFBO();
  }
*/

  @Override
  protected int getDepthBits() {
    return capabilities.getDepthBits();
  }


  @Override
  protected int getStencilBits() {
    return capabilities.getStencilBits();
  }
/*

  @Override
  protected Texture wrapBackTexture(Texture texture) {
    if (texture == null || changedBackTex) {
      if (USE_JOGL_FBOLAYER) {
        texture = new Texture(pg);
        texture.init(pg.width, pg.height,
                     backTexAttach.getName(), TEXTURE_2D, RGBA,
                     backTexAttach.getWidth(), backTexAttach.getHeight(),
                     backTexAttach.minFilter, backTexAttach.magFilter,
                     backTexAttach.wrapS, backTexAttach.wrapT);
        texture.invertedY(true);
        texture.colorBuffer(true);
        pg.setCache(pg, texture);
      } else {
        texture = super.wrapBackTexture(null);
      }
    } else {
      if (USE_JOGL_FBOLAYER) {
        texture.glName = backTexAttach.getName();
      } else {
        texture = super.wrapBackTexture(texture);
      }
    }
    return texture;
  }


  @Override
  protected Texture wrapFrontTexture(Texture texture) {
    if (texture == null || changedFrontTex) {
      if (USE_JOGL_FBOLAYER) {
        texture = new Texture(pg);
        texture.init(pg.width, pg.height,
                     backTexAttach.getName(), TEXTURE_2D, RGBA,
                     frontTexAttach.getWidth(), frontTexAttach.getHeight(),
                     frontTexAttach.minFilter, frontTexAttach.magFilter,
                     frontTexAttach.wrapS, frontTexAttach.wrapT);
        texture.invertedY(true);
        texture.colorBuffer(true);
      } else {
        texture = super.wrapFrontTexture(null);
      }
    } else {
      if (USE_JOGL_FBOLAYER) {
        texture.glName = frontTexAttach.getName();
      } else {
        texture = super.wrapFrontTexture(texture);
      }
    }
    return texture;
  }


  @Override
  protected void bindFrontTexture() {
    if (USE_JOGL_FBOLAYER) {
      usingFrontTex = true;
      if (!texturingIsEnabled(TEXTURE_2D)) {
        enableTexturing(TEXTURE_2D);
      }
      bindTexture(TEXTURE_2D, frontTexAttach.getName());
    } else super.bindFrontTexture();
  }


  @Override
  protected void unbindFrontTexture() {
    if (USE_JOGL_FBOLAYER) {
      if (textureIsBound(TEXTURE_2D, frontTexAttach.getName())) {
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
    } else super.unbindFrontTexture();
  }


  @Override
  protected void syncBackTexture() {
    if (USE_JOGL_FBOLAYER) {
      if (usingFrontTex) needSepFrontTex = true;
      if (1 < numSamples && backFBO != null) {
        backFBO.syncSamplingSink(gl);
        backFBO.bind(gl);
      }
    } else super.syncBackTexture();
  }


  @Override
  protected void beginDraw(boolean clear0) {
    if (!setFps) setFps(targetFps);
    if (USE_JOGL_FBOLAYER) return;
    super.beginDraw(clear0);
  }


  @Override
  protected void endDraw(boolean clear0) {
    if (isFBOBacked()) {
      if (USE_JOGL_FBOLAYER) {
        if (!clear0 && isFBOBacked() && !isMultisampled() &&
            frontFBO != null && backFBO != null) {
          // Draw the back texture into the front texture, which will be used as
          // back texture in the next frame. Otherwise flickering will occur if
          // the sketch uses "incremental drawing" (background() not called).
          frontFBO.bind(gl);
          gl.glDisable(GL.GL_BLEND);
          drawTexture(TEXTURE_2D, backTexAttach.getName(),
                      backTexAttach.getWidth(), backTexAttach.getHeight(),
                      pg.width, pg.height,
                      0, 0, pg.width, pg.height, 0, 0, pg.width, pg.height);
          backFBO.bind(gl);
        }
      } else {
        super.endDraw(clear0);
      }
    }
  }
*/

  @Override
  protected void getGL(PGL pgl) {
    PJOGL pjogl = (PJOGL)pgl;

    this.drawable = pjogl.drawable;
    this.context = pjogl.context;
    this.glContext = pjogl.glContext;
    setThread(pjogl.glThread);

    this.gl = pjogl.gl;
    this.gl2 = pjogl.gl2;
    this.gl2x = pjogl.gl2x;
    this.gl3 = pjogl.gl3;
  }


  protected void getGL(GLAutoDrawable glDrawable) {
    context = glDrawable.getContext();
    glContext = context.hashCode();
    setThread(Thread.currentThread());

    gl = context.getGL();
    gl2 = gl.getGL2ES2();
    try {
      gl2x = gl.getGL2();
    } catch (com.jogamp.opengl.GLException e) {
      gl2x = null;
    }
    try {
      gl3 = gl.getGL2GL3();
    } catch (com.jogamp.opengl.GLException e) {
      gl3 = null;
    }
  }



  /*
  @Override
  protected boolean canDraw() {
    return true;
//    return pg.initialized;
//      && pg.parent.isDisplayable();
  }


  @Override
  protected void requestFocus() { }


  @Override
  protected void requestDraw() {

    drawException = null;
    boolean canDraw = pg.parent.canDraw();
    if (pg.initialized && (canDraw || prevCanDraw)) {
      drawLatch = new CountDownLatch(1);
      if (WINDOW_TOOLKIT == AWT) {
        canvasAWT.display();
      } else if (WINDOW_TOOLKIT == NEWT) {
        windowNEWT.display();
      }
      try {
        drawLatch.await(DRAW_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }

      if (canDraw) prevCanDraw = true;
      else prevCanDraw = false;
    }

    // Throw wherever exception happened during drawing outside the GL thread
    // to it is properly picked up by the PDE.
    if (drawException != null) {
      if (drawException instanceof RuntimeException) {
        throw (RuntimeException)drawException;
      } else {
        throw new RuntimeException(drawException);
      }
    }
  }


  @Override
  protected void swapBuffers() {
    if (WINDOW_TOOLKIT == AWT) {
      canvasAWT.swapBuffers();
    } else if (WINDOW_TOOLKIT == NEWT) {
      windowNEWT.swapBuffers();
    }
  }
 */


  @Override
  protected void beginGL() {
    if (gl2x != null) {
      if (projMatrix == null) {
        projMatrix = new float[16];
      }
      gl2x.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
      projMatrix[ 0] = pg.projection.m00;
      projMatrix[ 1] = pg.projection.m10;
      projMatrix[ 2] = pg.projection.m20;
      projMatrix[ 3] = pg.projection.m30;
      projMatrix[ 4] = pg.projection.m01;
      projMatrix[ 5] = pg.projection.m11;
      projMatrix[ 6] = pg.projection.m21;
      projMatrix[ 7] = pg.projection.m31;
      projMatrix[ 8] = pg.projection.m02;
      projMatrix[ 9] = pg.projection.m12;
      projMatrix[10] = pg.projection.m22;
      projMatrix[11] = pg.projection.m32;
      projMatrix[12] = pg.projection.m03;
      projMatrix[13] = pg.projection.m13;
      projMatrix[14] = pg.projection.m23;
      projMatrix[15] = pg.projection.m33;
      gl2x.glLoadMatrixf(projMatrix, 0);

      if (mvMatrix == null) {
        mvMatrix = new float[16];
      }
      gl2x.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
      mvMatrix[ 0] = pg.modelview.m00;
      mvMatrix[ 1] = pg.modelview.m10;
      mvMatrix[ 2] = pg.modelview.m20;
      mvMatrix[ 3] = pg.modelview.m30;
      mvMatrix[ 4] = pg.modelview.m01;
      mvMatrix[ 5] = pg.modelview.m11;
      mvMatrix[ 6] = pg.modelview.m21;
      mvMatrix[ 7] = pg.modelview.m31;
      mvMatrix[ 8] = pg.modelview.m02;
      mvMatrix[ 9] = pg.modelview.m12;
      mvMatrix[10] = pg.modelview.m22;
      mvMatrix[11] = pg.modelview.m32;
      mvMatrix[12] = pg.modelview.m03;
      mvMatrix[13] = pg.modelview.m13;
      mvMatrix[14] = pg.modelview.m23;
      mvMatrix[15] = pg.modelview.m33;
      gl2x.glLoadMatrixf(mvMatrix, 0);
    }
  }


  @Override
  protected boolean hasFBOs() {
    if (context.hasBasicFBOSupport()) return true;
    else return super.hasFBOs();
  }


  @Override
  protected boolean hasShaders() {
    if (context.hasGLSL()) return true;
    else return super.hasShaders();
  }


  ///////////////////////////////////////////////////////////

  // JOGL event listeners

/*
  protected void getBuffers(GLWindow glWindow) {
    if (false) {
//    if (capabilities.isFBO()) {
//    if (USE_JOGL_FBOLAYER && capabilities.isFBO()) {
      // The onscreen drawing surface is backed by an FBO layer.
      GLFBODrawable fboDrawable = null;
      fboDrawable = (GLFBODrawable)glWindow.getDelegatedDrawable();

      if (fboDrawable != null) {
        backFBO = fboDrawable.getFBObject(GL.GL_BACK);
        if (1 < numSamples) {
          if (needSepFrontTex) {
            // When using multisampled FBO, the back buffer is the MSAA
            // surface so it cannot be read from. The sink buffer contains
            // the readable 2D texture.
            // In this case, we create an auxiliary "front" buffer that it is
            // swapped with the sink buffer at the beginning of each frame.
            // In this way, we always have a readable copy of the previous
            // frame in the front texture, while the back is synchronized
            // with the contents of the MSAA back buffer when requested.
            if (frontFBO == null) {
              // init
              frontFBO = new FBObject();
              frontFBO.reset(gl, pg.width, pg.height, numSamples);
              frontFBO.attachTexture2D(gl, 0, true);
              sinkFBO = backFBO.getSamplingSinkFBO();
              changedFrontTex = changedBackTex = true;
            } else {
              // swap
              FBObject temp = sinkFBO;
              sinkFBO = frontFBO;
              frontFBO = temp;
              backFBO.setSamplingSink(sinkFBO);
              changedFrontTex = changedBackTex = false;
            }
            backTexAttach  = (FBObject.TextureAttachment) sinkFBO.
                             getColorbuffer(0);
            frontTexAttach = (FBObject.TextureAttachment)frontFBO.
                             getColorbuffer(0);
          } else {
            changedFrontTex = changedBackTex = sinkFBO == null;

            // Default setting (to save resources): the front and back
            // textures are the same.
            sinkFBO = backFBO.getSamplingSinkFBO();
            backTexAttach = (FBObject.TextureAttachment) sinkFBO.
                            getColorbuffer(0);
            frontTexAttach = backTexAttach;
          }
        } else {
          // w/out multisampling, rendering is done on the back buffer.
          frontFBO = fboDrawable.getFBObject(GL.GL_FRONT);
          backTexAttach  = (FBObject.TextureAttachment) backFBO.getColorbuffer(0);
          frontTexAttach = (FBObject.TextureAttachment) frontFBO.getColorbuffer(0);
        }
      }

    }
  }
*/

  protected void init(GLAutoDrawable glDrawable) {
    firstFrame = true;
    capabilities = glDrawable.getChosenGLCapabilities();
    if (!hasFBOs()) {
      throw new RuntimeException(MISSING_FBO_ERROR);
    }
    if (!hasShaders()) {
      throw new RuntimeException(MISSING_GLSL_ERROR);
    }
//    if (USE_JOGL_FBOLAYER && capabilities.isFBO()) {
//      int maxs = maxSamples();
//      numSamples = PApplet.min(capabilities.getNumSamples(), maxs);
//    }
  }

  /*
  protected class PGLListener implements GLEventListener {
    public PGLListener() {}

    @Override
    public void display(GLAutoDrawable glDrawable) {

      getGL(glDrawable);

      if (USE_JOGL_FBOLAYER && capabilities.isFBO()) {
        // The onscreen drawing surface is backed by an FBO layer.
        GLFBODrawable fboDrawable = null;

        if (WINDOW_TOOLKIT == AWT) {
          GLCanvas glCanvas = (GLCanvas)glDrawable;
          fboDrawable = (GLFBODrawable)glCanvas.getDelegatedDrawable();
        } else {
          GLWindow glWindow = (GLWindow)glDrawable;
          fboDrawable = (GLFBODrawable)glWindow.getDelegatedDrawable();
        }

        if (fboDrawable != null) {
          backFBO = fboDrawable.getFBObject(GL.GL_BACK);
          if (1 < numSamples) {
            if (needSepFrontTex) {
              // When using multisampled FBO, the back buffer is the MSAA
              // surface so it cannot be read from. The sink buffer contains
              // the readable 2D texture.
              // In this case, we create an auxiliary "front" buffer that it is
              // swapped with the sink buffer at the beginning of each frame.
              // In this way, we always have a readable copy of the previous
              // frame in the front texture, while the back is synchronized
              // with the contents of the MSAA back buffer when requested.
              if (frontFBO == null) {
                // init
                frontFBO = new FBObject();
                frontFBO.reset(gl, pg.width, pg.height, numSamples);
                frontFBO.attachTexture2D(gl, 0, true);
                sinkFBO = backFBO.getSamplingSinkFBO();
                changedFrontTex = changedBackTex = true;
              } else {
                // swap
                FBObject temp = sinkFBO;
                sinkFBO = frontFBO;
                frontFBO = temp;
                backFBO.setSamplingSink(sinkFBO);
                changedFrontTex = changedBackTex = false;
              }
              backTexAttach  = (FBObject.TextureAttachment) sinkFBO.
                               getColorbuffer(0);
              frontTexAttach = (FBObject.TextureAttachment)frontFBO.
                               getColorbuffer(0);
            } else {
              changedFrontTex = changedBackTex = sinkFBO == null;

              // Default setting (to save resources): the front and back
              // textures are the same.
              sinkFBO = backFBO.getSamplingSinkFBO();
              backTexAttach = (FBObject.TextureAttachment) sinkFBO.
                              getColorbuffer(0);
              frontTexAttach = backTexAttach;
            }
          } else {
            // w/out multisampling, rendering is done on the back buffer.
            frontFBO = fboDrawable.getFBObject(GL.GL_FRONT);
            backTexAttach  = (FBObject.TextureAttachment) backFBO.getColorbuffer(0);
            frontTexAttach = (FBObject.TextureAttachment) frontFBO.getColorbuffer(0);
          }
        }
      }

      try {
        pg.parent.handleDraw();
      } catch (Exception ex) {
        drawException = ex;
      }
      drawLatch.countDown();
    }

    @Override
    public void dispose(GLAutoDrawable adrawable) {
    }

    @Override
    public void init(GLAutoDrawable glDrawable) {
      getGL(glDrawable);

      capabilities = glDrawable.getChosenGLCapabilities();
      if (!hasFBOs()) {
        throw new RuntimeException(MISSING_FBO_ERROR);
      }
      if (!hasShaders()) {
        throw new RuntimeException(MISSING_GLSL_ERROR);
      }
      if (USE_JOGL_FBOLAYER && capabilities.isFBO()) {
        int maxs = maxSamples();
        numSamples = PApplet.min(capabilities.getNumSamples(), maxs);
      }
    }

    @Override
    public void reshape(GLAutoDrawable glDrawable, int x, int y, int w, int h) {
      //getGL(glDrawable);
    }

//    private void getGL(GLAutoDrawable glDrawable) {
//      drawable = glDrawable;
//      context = glDrawable.getContext();
//      glContext = context.hashCode();
//      glThread = Thread.currentThread();
//
//      gl = context.getGL();
//      gl2 = gl.getGL2ES2();
//      try {
//        gl2x = gl.getGL2();
//      } catch (javax.media.opengl.GLException e) {
//        gl2x = null;
//      }
//      try {
//        gl3 = gl.getGL2GL3();
//      } catch (javax.media.opengl.GLException e) {
//        gl3 = null;
//      }
//    }
  }
  */

  /*
  protected void nativeMouseEvent(com.jogamp.newt.event.MouseEvent nativeEvent,
                                  int peAction) {
    int modifiers = nativeEvent.getModifiers();
    int peModifiers = modifiers &
                      (InputEvent.SHIFT_MASK |
                       InputEvent.CTRL_MASK |
                       InputEvent.META_MASK |
                       InputEvent.ALT_MASK);

    int peButton = 0;
    if ((modifiers & InputEvent.BUTTON1_MASK) != 0) {
      peButton = PConstants.LEFT;
    } else if ((modifiers & InputEvent.BUTTON2_MASK) != 0) {
      peButton = PConstants.CENTER;
    } else if ((modifiers & InputEvent.BUTTON3_MASK) != 0) {
      peButton = PConstants.RIGHT;
    }

    if (PApplet.platform == PConstants.MACOSX) {
      //if (nativeEvent.isPopupTrigger()) {
      if ((modifiers & InputEvent.CTRL_MASK) != 0) {
        peButton = PConstants.RIGHT;
      }
    }

    int peCount = 0;
    if (peAction == MouseEvent.WHEEL) {
      peCount = nativeEvent.isShiftDown() ? (int)nativeEvent.getRotation()[0] :
                                            (int)nativeEvent.getRotation()[1];
    } else {
      peCount = nativeEvent.getClickCount();
    }

    MouseEvent me = new MouseEvent(nativeEvent, nativeEvent.getWhen(),
                                   peAction, peModifiers,
                                   nativeEvent.getX(), nativeEvent.getY(),
                                   peButton,
                                   peCount);

    pg.parent.postEvent(me);
  }

  protected void nativeKeyEvent(com.jogamp.newt.event.KeyEvent nativeEvent,
                                int peAction) {
    int peModifiers = nativeEvent.getModifiers() &
                      (InputEvent.SHIFT_MASK |
                       InputEvent.CTRL_MASK |
                       InputEvent.META_MASK |
                       InputEvent.ALT_MASK);

    char keyChar;
    if (nativeEvent.getKeyChar() == 0) {
      keyChar = PConstants.CODED;
    } else {
      keyChar = nativeEvent.getKeyChar();
    }

    KeyEvent ke = new KeyEvent(nativeEvent, nativeEvent.getWhen(),
                               peAction, peModifiers,
                               keyChar,
                               nativeEvent.getKeyCode());

    pg.parent.postEvent(ke);
  }

  protected class NEWTWindowListener implements com.jogamp.newt.event.WindowListener {
    public NEWTWindowListener() {
      super();
    }
    @Override
    public void windowGainedFocus(com.jogamp.newt.event.WindowEvent arg0) {
      pg.parent.focusGained(null);
    }

    @Override
    public void windowLostFocus(com.jogamp.newt.event.WindowEvent arg0) {
      pg.parent.focusLost(null);
    }

    @Override
    public void windowDestroyNotify(com.jogamp.newt.event.WindowEvent arg0) {
    }

    @Override
    public void windowDestroyed(com.jogamp.newt.event.WindowEvent arg0) {
    }

    @Override
    public void windowMoved(com.jogamp.newt.event.WindowEvent arg0) {
    }

    @Override
    public void windowRepaint(com.jogamp.newt.event.WindowUpdateEvent arg0) {
    }

    @Override
    public void windowResized(com.jogamp.newt.event.WindowEvent arg0) { }
  }

  // NEWT mouse listener
  protected class NEWTMouseListener extends com.jogamp.newt.event.MouseAdapter {
    public NEWTMouseListener() {
      super();
    }
    @Override
    public void mousePressed(com.jogamp.newt.event.MouseEvent e) {
      nativeMouseEvent(e, MouseEvent.PRESS);
    }
    @Override
    public void mouseReleased(com.jogamp.newt.event.MouseEvent e) {
      nativeMouseEvent(e, MouseEvent.RELEASE);
    }
    @Override
    public void mouseClicked(com.jogamp.newt.event.MouseEvent e) {
      nativeMouseEvent(e, MouseEvent.CLICK);
    }
    @Override
    public void mouseDragged(com.jogamp.newt.event.MouseEvent e) {
      nativeMouseEvent(e, MouseEvent.DRAG);
    }
    @Override
    public void mouseMoved(com.jogamp.newt.event.MouseEvent e) {
      nativeMouseEvent(e, MouseEvent.MOVE);
    }
    @Override
    public void mouseWheelMoved(com.jogamp.newt.event.MouseEvent e) {
      nativeMouseEvent(e, MouseEvent.WHEEL);
    }
    @Override
    public void mouseEntered(com.jogamp.newt.event.MouseEvent e) {
      nativeMouseEvent(e, MouseEvent.ENTER);
    }
    @Override
    public void mouseExited(com.jogamp.newt.event.MouseEvent e) {
      nativeMouseEvent(e, MouseEvent.EXIT);
    }
  }

  // NEWT key listener
  protected class NEWTKeyListener extends com.jogamp.newt.event.KeyAdapter {
    public NEWTKeyListener() {
      super();
    }
    @Override
    public void keyPressed(com.jogamp.newt.event.KeyEvent e) {
      nativeKeyEvent(e, KeyEvent.PRESS);
    }
    @Override
    public void keyReleased(com.jogamp.newt.event.KeyEvent e) {
      nativeKeyEvent(e, KeyEvent.RELEASE);
    }
    public void keyTyped(com.jogamp.newt.event.KeyEvent e)  {
      nativeKeyEvent(e, KeyEvent.TYPE);
    }
  }
*/

  ///////////////////////////////////////////////////////////

  // Utility functions


  @Override
  protected void enableTexturing(int target) {
    if (profile == 2) enable(target);
    if (target == TEXTURE_2D) {
      texturingTargets[0] = true;
    } else if (target == TEXTURE_RECTANGLE) {
      texturingTargets[1] = true;
    }
  }


  @Override
  protected void disableTexturing(int target) {
    if (profile == 2) disable(target);
    if (target == TEXTURE_2D) {
      texturingTargets[0] = false;
    } else if (target == TEXTURE_RECTANGLE) {
      texturingTargets[1] = false;
    }
  }


  /**
   * Convenience method to get a legit FontMetrics object. Where possible,
   * override this any renderer subclass so that you're not using what's
   * returned by getDefaultToolkit() to get your metrics.
   */
  @SuppressWarnings("deprecation")
  private FontMetrics getFontMetrics(Font font) {  // ignore
    return Toolkit.getDefaultToolkit().getFontMetrics(font);
  }


  /**
   * Convenience method to jump through some Java2D hoops and get an FRC.
   */
  private FontRenderContext getFontRenderContext(Font font) {  // ignore
    return getFontMetrics(font).getFontRenderContext();
  }


  @Override
  protected int getFontAscent(Object font) {
    return getFontMetrics((Font) font).getAscent();
  }


  @Override
  protected int getFontDescent(Object font) {
    return getFontMetrics((Font) font).getDescent();
  }


  @Override
  protected int getTextWidth(Object font, char[] buffer, int start, int stop) {
    // maybe should use one of the newer/fancier functions for this?
    int length = stop - start;
    FontMetrics metrics = getFontMetrics((Font) font);
    return metrics.charsWidth(buffer, start, length);
  }


  @Override
  protected Object getDerivedFont(Object font, float size) {
    return ((Font) font).deriveFont(size);
  }


  @Override
  protected String[] loadVertexShader(String filename, int version) {
    if (PApplet.platform == PConstants.MACOSX) {
      String[] fragSrc0 = pg.parent.loadStrings(filename);
      return preprocessFragmentSource(fragSrc0, 130);
    } else {
      return pg.parent.loadStrings(filename);
    }
  }


  @Override
  protected String[] loadFragmentShader(String filename, int version) {
    if (PApplet.platform == PConstants.MACOSX) {
      String[] vertSrc0 = pg.parent.loadStrings(filename);
      return preprocessVertexSource(vertSrc0, 130);
    } else {
      return pg.parent.loadStrings(filename);
    }
  }


  @Override
  protected String[] loadFragmentShader(URL url, int version) {
    try {
      if (PApplet.platform == PConstants.MACOSX) {
        String[] fragSrc0 = PApplet.loadStrings(url.openStream());
        return preprocessFragmentSource(fragSrc0, 130);
      } else {
        return PApplet.loadStrings(url.openStream());
      }
    } catch (IOException e) {
      PGraphics.showException("Cannot load fragment shader " + url.getFile());
    }
    return null;
  }


  @Override
  protected String[] loadVertexShader(URL url, int version) {
    try {
      if (PApplet.platform == PConstants.MACOSX) {
        String[] vertSrc0 = PApplet.loadStrings(url.openStream());
        return preprocessVertexSource(vertSrc0, 130);
      } else {
        return PApplet.loadStrings(url.openStream());
      }
    } catch (IOException e) {
      PGraphics.showException("Cannot load vertex shader " + url.getFile());
    }
    return null;
  }


  ///////////////////////////////////////////////////////////

  // Tessellator


  @Override
  protected Tessellator createTessellator(TessellatorCallback callback) {
    return new Tessellator(callback);
  }


  protected static class Tessellator implements PGL.Tessellator {
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

    @Override
    public void beginPolygon() {
      GLU.gluTessBeginPolygon(tess, null);
    }

    @Override
    public void endPolygon() {
      GLU.gluTessEndPolygon(tess);
    }

    @Override
    public void setWindingRule(int rule) {
      GLU.gluTessProperty(tess, GLU.GLU_TESS_WINDING_RULE, rule);
    }

    @Override
    public void beginContour() {
      GLU.gluTessBeginContour(tess);
    }

    @Override
    public void endContour() {
      GLU.gluTessEndContour(tess);
    }

    @Override
    public void addVertex(double[] v) {
      GLU.gluTessVertex(tess, v, 0, v);
    }

    protected class GLUCallback extends GLUtessellatorCallbackAdapter {
      @Override
      public void begin(int type) {
        callback.begin(type);
      }

      @Override
      public void end() {
        callback.end();
      }

      @Override
      public void vertex(Object data) {
        callback.vertex(data);
      }

      @Override
      public void combine(double[] coords, Object[] data,
                          float[] weight, Object[] outData) {
        callback.combine(coords, data, weight, outData);
      }

      @Override
      public void error(int errnum) {
        callback.error(errnum);
      }
    }
  }


  @Override
  protected String tessError(int err) {
    return glu.gluErrorString(err);
  }


  ///////////////////////////////////////////////////////////

  // Font outline


  static {
    SHAPE_TEXT_SUPPORTED = true;
    SEG_MOVETO  = PathIterator.SEG_MOVETO;
    SEG_LINETO  = PathIterator.SEG_LINETO;
    SEG_QUADTO  = PathIterator.SEG_QUADTO;
    SEG_CUBICTO = PathIterator.SEG_CUBICTO;
    SEG_CLOSE   = PathIterator.SEG_CLOSE;
  }


  @Override
  protected FontOutline createFontOutline(char ch, Object font) {
    return new FontOutline(ch, (Font) font);
  }


  protected class FontOutline implements PGL.FontOutline {
    PathIterator iter;

    public FontOutline(char ch, Font font) {
      char textArray[] = new char[] { ch };
      FontRenderContext frc = getFontRenderContext(font);
      GlyphVector gv = font.createGlyphVector(frc, textArray);
      Shape shp = gv.getOutline();
      iter = shp.getPathIterator(null);
    }

    public boolean isDone() {
      return iter.isDone();
    }

    public int currentSegment(float coords[]) {
      return iter.currentSegment(coords);
    }

    public void next() {
      iter.next();
    }
  }


  ///////////////////////////////////////////////////////////

  // Constants

  static {
    FALSE = GL.GL_FALSE;
    TRUE  = GL.GL_TRUE;

    INT            = GL2ES2.GL_INT;
    BYTE           = GL.GL_BYTE;
    SHORT          = GL.GL_SHORT;
    FLOAT          = GL.GL_FLOAT;
    BOOL           = GL2ES2.GL_BOOL;
    UNSIGNED_INT   = GL.GL_UNSIGNED_INT;
    UNSIGNED_BYTE  = GL.GL_UNSIGNED_BYTE;
    UNSIGNED_SHORT = GL.GL_UNSIGNED_SHORT;

    RGB             = GL.GL_RGB;
    RGBA            = GL.GL_RGBA;
    ALPHA           = GL.GL_ALPHA;
    LUMINANCE       = GL.GL_LUMINANCE;
    LUMINANCE_ALPHA = GL.GL_LUMINANCE_ALPHA;

    UNSIGNED_SHORT_5_6_5   = GL.GL_UNSIGNED_SHORT_5_6_5;
    UNSIGNED_SHORT_4_4_4_4 = GL.GL_UNSIGNED_SHORT_4_4_4_4;
    UNSIGNED_SHORT_5_5_5_1 = GL.GL_UNSIGNED_SHORT_5_5_5_1;

    RGBA4   = GL.GL_RGBA4;
    RGB5_A1 = GL.GL_RGB5_A1;
    RGB565  = GL.GL_RGB565;
    RGB8    = GL.GL_RGB8;
    RGBA8   = GL.GL_RGBA8;
    ALPHA8  = GL.GL_ALPHA8;

    READ_ONLY  = GL2ES3.GL_READ_ONLY;
    WRITE_ONLY = GL.GL_WRITE_ONLY;
    READ_WRITE = GL2ES3.GL_READ_WRITE;

    TESS_WINDING_NONZERO = GLU.GLU_TESS_WINDING_NONZERO;
    TESS_WINDING_ODD     = GLU.GLU_TESS_WINDING_ODD;

    GENERATE_MIPMAP_HINT = GL.GL_GENERATE_MIPMAP_HINT;
    FASTEST              = GL.GL_FASTEST;
    NICEST               = GL.GL_NICEST;
    DONT_CARE            = GL.GL_DONT_CARE;

    VENDOR                   = GL.GL_VENDOR;
    RENDERER                 = GL.GL_RENDERER;
    VERSION                  = GL.GL_VERSION;
    EXTENSIONS               = GL.GL_EXTENSIONS;
    SHADING_LANGUAGE_VERSION = GL2ES2.GL_SHADING_LANGUAGE_VERSION;

    MAX_SAMPLES = GL.GL_MAX_SAMPLES;
    SAMPLES     = GL.GL_SAMPLES;

    ALIASED_LINE_WIDTH_RANGE = GL.GL_ALIASED_LINE_WIDTH_RANGE;
    ALIASED_POINT_SIZE_RANGE = GL.GL_ALIASED_POINT_SIZE_RANGE;

    DEPTH_BITS   = GL.GL_DEPTH_BITS;
    STENCIL_BITS = GL.GL_STENCIL_BITS;

    CCW = GL.GL_CCW;
    CW  = GL.GL_CW;

    VIEWPORT = GL.GL_VIEWPORT;

    ARRAY_BUFFER         = GL.GL_ARRAY_BUFFER;
    ELEMENT_ARRAY_BUFFER = GL.GL_ELEMENT_ARRAY_BUFFER;

    MAX_VERTEX_ATTRIBS  = GL2ES2.GL_MAX_VERTEX_ATTRIBS;

    STATIC_DRAW  = GL.GL_STATIC_DRAW;
    DYNAMIC_DRAW = GL.GL_DYNAMIC_DRAW;
    STREAM_DRAW  = GL2ES2.GL_STREAM_DRAW;

    BUFFER_SIZE  = GL.GL_BUFFER_SIZE;
    BUFFER_USAGE = GL.GL_BUFFER_USAGE;

    POINTS         = GL.GL_POINTS;
    LINE_STRIP     = GL.GL_LINE_STRIP;
    LINE_LOOP      = GL.GL_LINE_LOOP;
    LINES          = GL.GL_LINES;
    TRIANGLE_FAN   = GL.GL_TRIANGLE_FAN;
    TRIANGLE_STRIP = GL.GL_TRIANGLE_STRIP;
    TRIANGLES      = GL.GL_TRIANGLES;

    CULL_FACE      = GL.GL_CULL_FACE;
    FRONT          = GL.GL_FRONT;
    BACK           = GL.GL_BACK;
    FRONT_AND_BACK = GL.GL_FRONT_AND_BACK;

    POLYGON_OFFSET_FILL = GL.GL_POLYGON_OFFSET_FILL;

    UNPACK_ALIGNMENT = GL.GL_UNPACK_ALIGNMENT;
    PACK_ALIGNMENT   = GL.GL_PACK_ALIGNMENT;

    TEXTURE_2D        = GL.GL_TEXTURE_2D;
    TEXTURE_RECTANGLE = GL2GL3.GL_TEXTURE_RECTANGLE;

    TEXTURE_BINDING_2D        = GL.GL_TEXTURE_BINDING_2D;
    TEXTURE_BINDING_RECTANGLE = GL2GL3.GL_TEXTURE_BINDING_RECTANGLE;

    MAX_TEXTURE_SIZE           = GL.GL_MAX_TEXTURE_SIZE;
    TEXTURE_MAX_ANISOTROPY     = GL.GL_TEXTURE_MAX_ANISOTROPY_EXT;
    MAX_TEXTURE_MAX_ANISOTROPY = GL.GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT;

    MAX_VERTEX_TEXTURE_IMAGE_UNITS   = GL2ES2.GL_MAX_VERTEX_TEXTURE_IMAGE_UNITS;
    MAX_TEXTURE_IMAGE_UNITS          = GL2ES2.GL_MAX_TEXTURE_IMAGE_UNITS;
    MAX_COMBINED_TEXTURE_IMAGE_UNITS = GL2ES2.GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS;

    NUM_COMPRESSED_TEXTURE_FORMATS = GL.GL_NUM_COMPRESSED_TEXTURE_FORMATS;
    COMPRESSED_TEXTURE_FORMATS     = GL.GL_COMPRESSED_TEXTURE_FORMATS;

    NEAREST               = GL.GL_NEAREST;
    LINEAR                = GL.GL_LINEAR;
    LINEAR_MIPMAP_NEAREST = GL.GL_LINEAR_MIPMAP_NEAREST;
    LINEAR_MIPMAP_LINEAR  = GL.GL_LINEAR_MIPMAP_LINEAR;

    CLAMP_TO_EDGE = GL.GL_CLAMP_TO_EDGE;
    REPEAT        = GL.GL_REPEAT;

    TEXTURE0           = GL.GL_TEXTURE0;
    TEXTURE1           = GL.GL_TEXTURE1;
    TEXTURE2           = GL.GL_TEXTURE2;
    TEXTURE3           = GL.GL_TEXTURE3;
    TEXTURE_MIN_FILTER = GL.GL_TEXTURE_MIN_FILTER;
    TEXTURE_MAG_FILTER = GL.GL_TEXTURE_MAG_FILTER;
    TEXTURE_WRAP_S     = GL.GL_TEXTURE_WRAP_S;
    TEXTURE_WRAP_T     = GL.GL_TEXTURE_WRAP_T;
    TEXTURE_WRAP_R     = GL2ES2.GL_TEXTURE_WRAP_R;

    TEXTURE_CUBE_MAP = GL.GL_TEXTURE_CUBE_MAP;
    TEXTURE_CUBE_MAP_POSITIVE_X = GL.GL_TEXTURE_CUBE_MAP_POSITIVE_X;
    TEXTURE_CUBE_MAP_POSITIVE_Y = GL.GL_TEXTURE_CUBE_MAP_POSITIVE_Y;
    TEXTURE_CUBE_MAP_POSITIVE_Z = GL.GL_TEXTURE_CUBE_MAP_POSITIVE_Z;
    TEXTURE_CUBE_MAP_NEGATIVE_X = GL.GL_TEXTURE_CUBE_MAP_NEGATIVE_X;
    TEXTURE_CUBE_MAP_NEGATIVE_Y = GL.GL_TEXTURE_CUBE_MAP_NEGATIVE_Y;
    TEXTURE_CUBE_MAP_NEGATIVE_Z = GL.GL_TEXTURE_CUBE_MAP_NEGATIVE_Z;

    VERTEX_SHADER        = GL2ES2.GL_VERTEX_SHADER;
    FRAGMENT_SHADER      = GL2ES2.GL_FRAGMENT_SHADER;
    INFO_LOG_LENGTH      = GL2ES2.GL_INFO_LOG_LENGTH;
    SHADER_SOURCE_LENGTH = GL2ES2.GL_SHADER_SOURCE_LENGTH;
    COMPILE_STATUS       = GL2ES2.GL_COMPILE_STATUS;
    LINK_STATUS          = GL2ES2.GL_LINK_STATUS;
    VALIDATE_STATUS      = GL2ES2.GL_VALIDATE_STATUS;
    SHADER_TYPE          = GL2ES2.GL_SHADER_TYPE;
    DELETE_STATUS        = GL2ES2.GL_DELETE_STATUS;

    FLOAT_VEC2   = GL2ES2.GL_FLOAT_VEC2;
    FLOAT_VEC3   = GL2ES2.GL_FLOAT_VEC3;
    FLOAT_VEC4   = GL2ES2.GL_FLOAT_VEC4;
    FLOAT_MAT2   = GL2ES2.GL_FLOAT_MAT2;
    FLOAT_MAT3   = GL2ES2.GL_FLOAT_MAT3;
    FLOAT_MAT4   = GL2ES2.GL_FLOAT_MAT4;
    INT_VEC2     = GL2ES2.GL_INT_VEC2;
    INT_VEC3     = GL2ES2.GL_INT_VEC3;
    INT_VEC4     = GL2ES2.GL_INT_VEC4;
    BOOL_VEC2    = GL2ES2.GL_BOOL_VEC2;
    BOOL_VEC3    = GL2ES2.GL_BOOL_VEC3;
    BOOL_VEC4    = GL2ES2.GL_BOOL_VEC4;
    SAMPLER_2D   = GL2ES2.GL_SAMPLER_2D;
    SAMPLER_CUBE = GL2ES2.GL_SAMPLER_CUBE;

    LOW_FLOAT    = GL2ES2.GL_LOW_FLOAT;
    MEDIUM_FLOAT = GL2ES2.GL_MEDIUM_FLOAT;
    HIGH_FLOAT   = GL2ES2.GL_HIGH_FLOAT;
    LOW_INT      = GL2ES2.GL_LOW_INT;
    MEDIUM_INT   = GL2ES2.GL_MEDIUM_INT;
    HIGH_INT     = GL2ES2.GL_HIGH_INT;

    CURRENT_VERTEX_ATTRIB = GL2ES2.GL_CURRENT_VERTEX_ATTRIB;

    VERTEX_ATTRIB_ARRAY_BUFFER_BINDING = GL2ES2.GL_VERTEX_ATTRIB_ARRAY_BUFFER_BINDING;
    VERTEX_ATTRIB_ARRAY_ENABLED        = GL2ES2.GL_VERTEX_ATTRIB_ARRAY_ENABLED;
    VERTEX_ATTRIB_ARRAY_SIZE           = GL2ES2.GL_VERTEX_ATTRIB_ARRAY_SIZE;
    VERTEX_ATTRIB_ARRAY_STRIDE         = GL2ES2.GL_VERTEX_ATTRIB_ARRAY_STRIDE;
    VERTEX_ATTRIB_ARRAY_TYPE           = GL2ES2.GL_VERTEX_ATTRIB_ARRAY_TYPE;
    VERTEX_ATTRIB_ARRAY_NORMALIZED     = GL2ES2.GL_VERTEX_ATTRIB_ARRAY_NORMALIZED;
    VERTEX_ATTRIB_ARRAY_POINTER        = GL2ES2.GL_VERTEX_ATTRIB_ARRAY_POINTER;

    BLEND               = GL.GL_BLEND;
    ONE                 = GL.GL_ONE;
    ZERO                = GL.GL_ZERO;
    SRC_ALPHA           = GL.GL_SRC_ALPHA;
    DST_ALPHA           = GL.GL_DST_ALPHA;
    ONE_MINUS_SRC_ALPHA = GL.GL_ONE_MINUS_SRC_ALPHA;
    ONE_MINUS_DST_COLOR = GL.GL_ONE_MINUS_DST_COLOR;
    ONE_MINUS_SRC_COLOR = GL.GL_ONE_MINUS_SRC_COLOR;
    DST_COLOR           = GL.GL_DST_COLOR;
    SRC_COLOR           = GL.GL_SRC_COLOR;

    SAMPLE_ALPHA_TO_COVERAGE = GL.GL_SAMPLE_ALPHA_TO_COVERAGE;
    SAMPLE_COVERAGE          = GL.GL_SAMPLE_COVERAGE;

    KEEP      = GL.GL_KEEP;
    REPLACE   = GL.GL_REPLACE;
    INCR      = GL.GL_INCR;
    DECR      = GL.GL_DECR;
    INVERT    = GL.GL_INVERT;
    INCR_WRAP = GL.GL_INCR_WRAP;
    DECR_WRAP = GL.GL_DECR_WRAP;
    NEVER     = GL.GL_NEVER;
    ALWAYS    = GL.GL_ALWAYS;

    EQUAL    = GL.GL_EQUAL;
    LESS     = GL.GL_LESS;
    LEQUAL   = GL.GL_LEQUAL;
    GREATER  = GL.GL_GREATER;
    GEQUAL   = GL.GL_GEQUAL;
    NOTEQUAL = GL.GL_NOTEQUAL;

    FUNC_ADD              = GL.GL_FUNC_ADD;
    FUNC_MIN              = GL2ES3.GL_MIN;
    FUNC_MAX              = GL2ES3.GL_MAX;
    FUNC_REVERSE_SUBTRACT = GL.GL_FUNC_REVERSE_SUBTRACT;
    FUNC_SUBTRACT         = GL.GL_FUNC_SUBTRACT;

    DITHER = GL.GL_DITHER;

    CONSTANT_COLOR           = GL2ES2.GL_CONSTANT_COLOR;
    CONSTANT_ALPHA           = GL2ES2.GL_CONSTANT_ALPHA;
    ONE_MINUS_CONSTANT_COLOR = GL2ES2.GL_ONE_MINUS_CONSTANT_COLOR;
    ONE_MINUS_CONSTANT_ALPHA = GL2ES2.GL_ONE_MINUS_CONSTANT_ALPHA;
    SRC_ALPHA_SATURATE       = GL.GL_SRC_ALPHA_SATURATE;

    SCISSOR_TEST    = GL.GL_SCISSOR_TEST;
    STENCIL_TEST    = GL.GL_STENCIL_TEST;
    DEPTH_TEST      = GL.GL_DEPTH_TEST;
    DEPTH_WRITEMASK = GL.GL_DEPTH_WRITEMASK;
    ALPHA_TEST      = GL2ES1.GL_ALPHA_TEST;

    COLOR_BUFFER_BIT   = GL.GL_COLOR_BUFFER_BIT;
    DEPTH_BUFFER_BIT   = GL.GL_DEPTH_BUFFER_BIT;
    STENCIL_BUFFER_BIT = GL.GL_STENCIL_BUFFER_BIT;

    FRAMEBUFFER        = GL.GL_FRAMEBUFFER;
    COLOR_ATTACHMENT0  = GL.GL_COLOR_ATTACHMENT0;
    COLOR_ATTACHMENT1  = GL2ES2.GL_COLOR_ATTACHMENT1;
    COLOR_ATTACHMENT2  = GL2ES2.GL_COLOR_ATTACHMENT2;
    COLOR_ATTACHMENT3  = GL2ES2.GL_COLOR_ATTACHMENT3;
    RENDERBUFFER       = GL.GL_RENDERBUFFER;
    DEPTH_ATTACHMENT   = GL.GL_DEPTH_ATTACHMENT;
    STENCIL_ATTACHMENT = GL.GL_STENCIL_ATTACHMENT;
    READ_FRAMEBUFFER   = GL.GL_READ_FRAMEBUFFER;
    DRAW_FRAMEBUFFER   = GL.GL_DRAW_FRAMEBUFFER;

    RGBA8            = GL.GL_RGBA8;
    DEPTH24_STENCIL8 = GL.GL_DEPTH24_STENCIL8;

    DEPTH_COMPONENT   = GL2ES2.GL_DEPTH_COMPONENT;
    DEPTH_COMPONENT16 = GL.GL_DEPTH_COMPONENT16;
    DEPTH_COMPONENT24 = GL.GL_DEPTH_COMPONENT24;
    DEPTH_COMPONENT32 = GL.GL_DEPTH_COMPONENT32;

    STENCIL_INDEX  = GL2ES2.GL_STENCIL_INDEX;
    STENCIL_INDEX1 = GL.GL_STENCIL_INDEX1;
    STENCIL_INDEX4 = GL.GL_STENCIL_INDEX4;
    STENCIL_INDEX8 = GL.GL_STENCIL_INDEX8;

    DEPTH_STENCIL = GL.GL_DEPTH_STENCIL;

    FRAMEBUFFER_COMPLETE                      = GL.GL_FRAMEBUFFER_COMPLETE;
    FRAMEBUFFER_INCOMPLETE_ATTACHMENT         = GL.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT;
    FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT = GL.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT;
    FRAMEBUFFER_INCOMPLETE_DIMENSIONS         = GL.GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS;
    FRAMEBUFFER_INCOMPLETE_FORMATS            = GL.GL_FRAMEBUFFER_INCOMPLETE_FORMATS;
    FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER        = GL2GL3.GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER;
    FRAMEBUFFER_INCOMPLETE_READ_BUFFER        = GL2GL3.GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER;
    FRAMEBUFFER_UNSUPPORTED                   = GL.GL_FRAMEBUFFER_UNSUPPORTED;

    FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE           = GL.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE;
    FRAMEBUFFER_ATTACHMENT_OBJECT_NAME           = GL.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME;
    FRAMEBUFFER_ATTACHMENT_TEXTURE_LEVEL         = GL.GL_FRAMEBUFFER_ATTACHMENT_TEXTURE_LEVEL;
    FRAMEBUFFER_ATTACHMENT_TEXTURE_CUBE_MAP_FACE = GL.GL_FRAMEBUFFER_ATTACHMENT_TEXTURE_CUBE_MAP_FACE;

    RENDERBUFFER_WIDTH           = GL.GL_RENDERBUFFER_WIDTH;
    RENDERBUFFER_HEIGHT          = GL.GL_RENDERBUFFER_HEIGHT;
    RENDERBUFFER_RED_SIZE        = GL.GL_RENDERBUFFER_RED_SIZE;
    RENDERBUFFER_GREEN_SIZE      = GL.GL_RENDERBUFFER_GREEN_SIZE;
    RENDERBUFFER_BLUE_SIZE       = GL.GL_RENDERBUFFER_BLUE_SIZE;
    RENDERBUFFER_ALPHA_SIZE      = GL.GL_RENDERBUFFER_ALPHA_SIZE;
    RENDERBUFFER_DEPTH_SIZE      = GL.GL_RENDERBUFFER_DEPTH_SIZE;
    RENDERBUFFER_STENCIL_SIZE    = GL.GL_RENDERBUFFER_STENCIL_SIZE;
    RENDERBUFFER_INTERNAL_FORMAT = GL.GL_RENDERBUFFER_INTERNAL_FORMAT;

    MULTISAMPLE    = GL.GL_MULTISAMPLE;
    POINT_SMOOTH   = GL2ES1.GL_POINT_SMOOTH;
    LINE_SMOOTH    = GL.GL_LINE_SMOOTH;
    POLYGON_SMOOTH = GL2GL3.GL_POLYGON_SMOOTH;
  }

  ///////////////////////////////////////////////////////////

  // Special Functions

  @Override
  public void flush() {
    gl.glFlush();
  }

  @Override
  public void finish() {
    gl.glFinish();
  }

  @Override
  public void hint(int target, int hint) {
    gl.glHint(target, hint);
  }

  ///////////////////////////////////////////////////////////

  // State and State Requests

  @Override
  public void enable(int value) {
    if (-1 < value) {
      gl.glEnable(value);
    }
  }

  @Override
  public void disable(int value) {
    if (-1 < value) {
      gl.glDisable(value);
    }
  }

  @Override
  public void getBooleanv(int value, IntBuffer data) {
    if (-1 < value) {
      if (byteBuffer.capacity() < data.capacity()) {
        byteBuffer = allocateDirectByteBuffer(data.capacity());
      }
      gl.glGetBooleanv(value, byteBuffer);
      for (int i = 0; i < data.capacity(); i++) {
        data.put(i, byteBuffer.get(i));
      }
    } else {
      fillIntBuffer(data, 0, data.capacity() - 1, 0);
    }
  }

  @Override
  public void getIntegerv(int value, IntBuffer data) {
    if (-1 < value) {
      gl.glGetIntegerv(value, data);
    } else {
      fillIntBuffer(data, 0, data.capacity() - 1, 0);
    }
  }

  @Override
  public void getFloatv(int value, FloatBuffer data) {
    if (-1 < value) {
      gl.glGetFloatv(value, data);
    } else {
      fillFloatBuffer(data, 0, data.capacity() - 1, 0);
    }
  }

  @Override
  public boolean isEnabled(int value) {
    return gl.glIsEnabled(value);
  }

  @Override
  public String getString(int name) {
    return gl.glGetString(name);
  }

  ///////////////////////////////////////////////////////////

  // Error Handling

  @Override
  public int getError() {
    return gl.glGetError();
  }

  @Override
  public String errorString(int err) {
    return glu.gluErrorString(err);
  }

  //////////////////////////////////////////////////////////////////////////////

  // Buffer Objects

  @Override
  public void genBuffers(int n, IntBuffer buffers) {
    gl.glGenBuffers(n, buffers);
  }

  @Override
  public void deleteBuffers(int n, IntBuffer buffers) {
    gl.glDeleteBuffers(n, buffers);
  }

  @Override
  public void bindBuffer(int target, int buffer) {
    gl.glBindBuffer(target, buffer);
  }

  @Override
  public void bufferData(int target, int size, Buffer data, int usage) {
    gl.glBufferData(target, size, data, usage);
  }

  @Override
  public void bufferSubData(int target, int offset, int size, Buffer data) {
    gl.glBufferSubData(target, offset, size, data);
  }

  @Override
  public void isBuffer(int buffer) {
    gl.glIsBuffer(buffer);
  }

  @Override
  public void getBufferParameteriv(int target, int value, IntBuffer data) {
    gl.glGetBufferParameteriv(target, value, data);
  }

  @Override
  public ByteBuffer mapBuffer(int target, int access) {
    return gl2.glMapBuffer(target, access);
  }

  @Override
  public ByteBuffer mapBufferRange(int target, int offset, int length, int access) {
    if (gl2x != null) {
      return gl2x.glMapBufferRange(target, offset, length, access);
    } else if (gl3 != null) {
      return gl3.glMapBufferRange(target, offset, length, access);
    } else {
      throw new RuntimeException(String.format(MISSING_GLFUNC_ERROR, "glMapBufferRange()"));
    }
  }

  @Override
  public void unmapBuffer(int target) {
    gl2.glUnmapBuffer(target);
  }

  //////////////////////////////////////////////////////////////////////////////

  // Viewport and Clipping

  @Override
  public void depthRangef(float n, float f) {
    gl.glDepthRangef(n, f);
  }

  @Override
  public void viewport(int x, int y, int w, int h) {
    float scale = pg.getPixelScale();
    viewportImpl((int)scale * x, (int)(scale * y), (int)(scale * w), (int)(scale * h));
  }

  @Override
  protected void viewportImpl(int x, int y, int w, int h) {
    gl.glViewport(x, y, w, h);
  }

  //////////////////////////////////////////////////////////////////////////////

  // Reading Pixels

  @Override
  protected void readPixelsImpl(int x, int y, int width, int height, int format, int type, Buffer buffer) {
    gl.glReadPixels(x, y, width, height, format, type, buffer);
  }

  //////////////////////////////////////////////////////////////////////////////

  // Vertices

  @Override
  public void vertexAttrib1f(int index, float value) {
    gl2.glVertexAttrib1f(index, value);
  }

  @Override
  public void vertexAttrib2f(int index, float value0, float value1) {
    gl2.glVertexAttrib2f(index, value0, value1);
  }

  @Override
  public void vertexAttrib3f(int index, float value0, float value1, float value2) {
    gl2.glVertexAttrib3f(index, value0, value1, value2);
  }

  @Override
  public void vertexAttrib4f(int index, float value0, float value1, float value2, float value3) {
    gl2.glVertexAttrib4f(index, value0, value1, value2, value3);
  }

  @Override
  public void vertexAttrib1fv(int index, FloatBuffer values) {
    gl2.glVertexAttrib1fv(index, values);
  }

  @Override
  public void vertexAttrib2fv(int index, FloatBuffer values) {
    gl2.glVertexAttrib2fv(index, values);
  }

  @Override
  public void vertexAttrib3fv(int index, FloatBuffer values) {
    gl2.glVertexAttrib3fv(index, values);
  }

  @Override
  public void vertexAttri4fv(int index, FloatBuffer values) {
    gl2.glVertexAttrib4fv(index, values);
  }

  @Override
  public void vertexAttribPointer(int index, int size, int type, boolean normalized, int stride, int offset) {
    gl2.glVertexAttribPointer(index, size, type, normalized, stride, offset);
  }

  @Override
  public void vertexAttribPointer(int index, int size, int type, boolean normalized, int stride, Buffer data) {
    if (gl2x != null) {
      gl2x.glVertexAttribPointer(index, size, type, normalized, stride, data);
    } else {
      throw new RuntimeException(String.format(MISSING_GLFUNC_ERROR, "glVertexAttribPointer()"));
    }
  }

  @Override
  public void enableVertexAttribArray(int index) {
    gl2.glEnableVertexAttribArray(index);
  }

  @Override
  public void disableVertexAttribArray(int index) {
    gl2.glDisableVertexAttribArray(index);
  }

  @Override
  public void drawArrays(int mode, int first, int count) {
    gl.glDrawArrays(mode, first, count);
  }

  @Override
  public void drawElements(int mode, int count, int type, int offset) {
    gl.glDrawElements(mode, count, type, offset);
  }

  @Override
  public void drawElements(int mode, int count, int type, Buffer indices) {
    if (gl2x != null) {
      gl2x.glDrawElements(mode, count, type, indices);
    } else {
      throw new RuntimeException(String.format(MISSING_GLFUNC_ERROR, "glDrawElements()"));
    }
  }

  //////////////////////////////////////////////////////////////////////////////

  // Rasterization

  @Override
  public void lineWidth(float width) {
    gl.glLineWidth(width);
  }

  @Override
  public void frontFace(int dir) {
    gl.glFrontFace(dir);
  }

  @Override
  public void cullFace(int mode) {
    gl.glCullFace(mode);
  }

  @Override
  public void polygonOffset(float factor, float units) {
    gl.glPolygonOffset(factor, units);
  }

  //////////////////////////////////////////////////////////////////////////////

  // Pixel Rectangles

  @Override
  public void pixelStorei(int pname, int param) {
    gl.glPixelStorei(pname, param);
  }

  ///////////////////////////////////////////////////////////

  // Texturing

  @Override
  public void texImage2D(int target, int level, int internalFormat, int width, int height, int border, int format, int type, Buffer data) {
    gl.glTexImage2D(target, level, internalFormat, width, height, border, format, type, data);
  }

  @Override
  public void copyTexImage2D(int target, int level, int internalFormat, int x, int y, int width, int height, int border) {
    gl.glCopyTexImage2D(target, level, internalFormat, x, y, width, height, border);
  }

  @Override
  public void texSubImage2D(int target, int level, int xOffset, int yOffset, int width, int height, int format, int type, Buffer data) {
    gl.glTexSubImage2D(target, level, xOffset, yOffset, width, height, format, type, data);
  }

  @Override
  public void copyTexSubImage2D(int target, int level, int xOffset, int yOffset, int x, int y, int width, int height) {
    gl.glCopyTexSubImage2D(target, level, x, y, xOffset, yOffset, width, height);
  }

  @Override
  public void compressedTexImage2D(int target, int level, int internalFormat, int width, int height, int border, int imageSize, Buffer data) {
    gl.glCompressedTexImage2D(target, level, internalFormat, width, height, border, imageSize, data);
  }

  @Override
  public void compressedTexSubImage2D(int target, int level, int xOffset, int yOffset, int width, int height, int format, int imageSize, Buffer data) {
    gl.glCompressedTexSubImage2D(target, level, xOffset, yOffset, width, height, format, imageSize, data);
  }

  @Override
  public void texParameteri(int target, int pname, int param) {
    gl.glTexParameteri(target, pname, param);
  }

  @Override
  public void texParameterf(int target, int pname, float param) {
    gl.glTexParameterf(target, pname, param);
  }

  @Override
  public void texParameteriv(int target, int pname, IntBuffer params) {
    gl.glTexParameteriv(target, pname, params);
  }

  @Override
  public void texParameterfv(int target, int pname, FloatBuffer params) {
    gl.glTexParameterfv(target, pname, params);
  }

  @Override
  public void generateMipmap(int target) {
    gl.glGenerateMipmap(target);
  }

  @Override
  public void genTextures(int n, IntBuffer textures) {
    gl.glGenTextures(n, textures);
  }

  @Override
  public void deleteTextures(int n, IntBuffer textures) {
    gl.glDeleteTextures(n, textures);
  }

  @Override
  public void getTexParameteriv(int target, int pname, IntBuffer params) {
    gl.glGetTexParameteriv(target, pname, params);
  }

  @Override
  public void getTexParameterfv(int target, int pname, FloatBuffer params) {
    gl.glGetTexParameterfv(target, pname, params);
  }

  @Override
  public boolean isTexture(int texture) {
    return gl.glIsTexture(texture);
  }

  @Override
  protected void activeTextureImpl(int texture) {
    gl.glActiveTexture(texture);
  }

  @Override
  protected void bindTextureImpl(int target, int texture) {
    gl.glBindTexture(target, texture);
  }

  ///////////////////////////////////////////////////////////

  // Shaders and Programs

  @Override
  public int createShader(int type) {
    return gl2.glCreateShader(type);
  }

  @Override
  public void shaderSource(int shader, String source) {
    gl2.glShaderSource(shader, 1, new String[] { source }, (int[]) null, 0);
  }

  @Override
  public void compileShader(int shader) {
    gl2.glCompileShader(shader);
  }

  @Override
  public void releaseShaderCompiler() {
    gl2.glReleaseShaderCompiler();
  }

  @Override
  public void deleteShader(int shader) {
    gl2.glDeleteShader(shader);
  }

  @Override
  public void shaderBinary(int count, IntBuffer shaders, int binaryFormat, Buffer binary, int length) {
    gl2.glShaderBinary(count, shaders, binaryFormat, binary, length);
  }

  @Override
  public int createProgram() {
    return gl2.glCreateProgram();
  }

  @Override
  public void attachShader(int program, int shader) {
    gl2.glAttachShader(program, shader);
  }

  @Override
  public void detachShader(int program, int shader) {
    gl2.glDetachShader(program, shader);
  }

  @Override
  public void linkProgram(int program) {
    gl2.glLinkProgram(program);
  }

  @Override
  public void useProgram(int program) {
    gl2.glUseProgram(program);
  }

  @Override
  public void deleteProgram(int program) {
    gl2.glDeleteProgram(program);
  }

  @Override
  public String getActiveAttrib(int program, int index, IntBuffer size, IntBuffer type) {
    int[] tmp = {0, 0, 0};
    byte[] namebuf = new byte[1024];
    gl2.glGetActiveAttrib(program, index, 1024, tmp, 0, tmp, 1, tmp, 2, namebuf, 0);
    size.put(tmp[1]);
    type.put(tmp[2]);
    String name = new String(namebuf, 0, tmp[0]);
    return name;
  }

  @Override
  public int getAttribLocation(int program, String name) {
    return gl2.glGetAttribLocation(program, name);
  }

  @Override
  public void bindAttribLocation(int program, int index, String name) {
    gl2.glBindAttribLocation(program, index, name);
  }

  @Override
  public int getUniformLocation(int program, String name) {
    return gl2.glGetUniformLocation(program, name);
  }

  @Override
  public String getActiveUniform(int program, int index, IntBuffer size, IntBuffer type) {
    int[] tmp= {0, 0, 0};
    byte[] namebuf = new byte[1024];
    gl2.glGetActiveUniform(program, index, 1024, tmp, 0, tmp, 1, tmp, 2, namebuf, 0);
    size.put(tmp[1]);
    type.put(tmp[2]);
    String name = new String(namebuf, 0, tmp[0]);
    return name;
  }

  @Override
  public void uniform1i(int location, int value) {
    gl2.glUniform1i(location, value);
  }

  @Override
  public void uniform2i(int location, int value0, int value1) {
    gl2.glUniform2i(location, value0, value1);
  }

  @Override
  public void uniform3i(int location, int value0, int value1, int value2) {
    gl2.glUniform3i(location, value0, value1, value2);
  }

  @Override
  public void uniform4i(int location, int value0, int value1, int value2, int value3) {
    gl2.glUniform4i(location, value0, value1, value2, value3);
  }

  @Override
  public void uniform1f(int location, float value) {
    gl2.glUniform1f(location, value);
  }

  @Override
  public void uniform2f(int location, float value0, float value1) {
    gl2.glUniform2f(location, value0, value1);
  }

  @Override
  public void uniform3f(int location, float value0, float value1, float value2) {
    gl2.glUniform3f(location, value0, value1, value2);
  }

  @Override
  public void uniform4f(int location, float value0, float value1, float value2, float value3) {
    gl2.glUniform4f(location, value0, value1, value2, value3);
  }

  @Override
  public void uniform1iv(int location, int count, IntBuffer v) {
    gl2.glUniform1iv(location, count, v);
  }

  @Override
  public void uniform2iv(int location, int count, IntBuffer v) {
    gl2.glUniform2iv(location, count, v);
  }

  @Override
  public void uniform3iv(int location, int count, IntBuffer v) {
    gl2.glUniform3iv(location, count, v);
  }

  @Override
  public void uniform4iv(int location, int count, IntBuffer v) {
    gl2.glUniform4iv(location, count, v);
  }

  @Override
  public void uniform1fv(int location, int count, FloatBuffer v) {
    gl2.glUniform1fv(location, count, v);
  }

  @Override
  public void uniform2fv(int location, int count, FloatBuffer v) {
    gl2.glUniform2fv(location, count, v);
  }

  @Override
  public void uniform3fv(int location, int count, FloatBuffer v) {
    gl2.glUniform3fv(location, count, v);
  }

  @Override
  public void uniform4fv(int location, int count, FloatBuffer v) {
    gl2.glUniform4fv(location, count, v);
  }

  @Override
  public void uniformMatrix2fv(int location, int count, boolean transpose, FloatBuffer mat) {
    gl2.glUniformMatrix2fv(location, count, transpose, mat);
  }

  @Override
  public void uniformMatrix3fv(int location, int count, boolean transpose, FloatBuffer mat) {
    gl2.glUniformMatrix3fv(location, count, transpose, mat);
  }

  @Override
  public void uniformMatrix4fv(int location, int count, boolean transpose, FloatBuffer mat) {
    gl2.glUniformMatrix4fv(location, count, transpose, mat);
  }

  @Override
  public void validateProgram(int program) {
    gl2.glValidateProgram(program);
  }

  @Override
  public boolean isShader(int shader) {
    return gl2.glIsShader(shader);
  }

  @Override
  public void getShaderiv(int shader, int pname, IntBuffer params) {
    gl2.glGetShaderiv(shader, pname, params);
  }

  @Override
  public void getAttachedShaders(int program, int maxCount, IntBuffer count, IntBuffer shaders) {
    gl2.glGetAttachedShaders(program, maxCount, count, shaders);
  }

  @Override
  public String getShaderInfoLog(int shader) {
    int[] val = { 0 };
    gl2.glGetShaderiv(shader, GL2ES2.GL_INFO_LOG_LENGTH, val, 0);
    int length = val[0];

    byte[] log = new byte[length];
    gl2.glGetShaderInfoLog(shader, length, val, 0, log, 0);
    return new String(log);
  }

  @Override
  public String getShaderSource(int shader) {
    int[] len = {0};
    byte[] buf = new byte[1024];
    gl2.glGetShaderSource(shader, 1024, len, 0, buf, 0);
    return new String(buf, 0, len[0]);
  }

  @Override
  public void getShaderPrecisionFormat(int shaderType, int precisionType, IntBuffer range, IntBuffer precision) {
    gl2.glGetShaderPrecisionFormat(shaderType, precisionType, range, precision);
  }

  @Override
  public void getVertexAttribfv(int index, int pname, FloatBuffer params) {
    gl2.glGetVertexAttribfv(index, pname, params);
  }

  @Override
  public void getVertexAttribiv(int index, int pname, IntBuffer params) {
    gl2.glGetVertexAttribiv(index, pname, params);
  }

  @Override
  public void getVertexAttribPointerv(int index, int pname, ByteBuffer data) {
    throw new RuntimeException(String.format(MISSING_GLFUNC_ERROR, "glGetVertexAttribPointerv()"));
  }

  @Override
  public void getUniformfv(int program, int location, FloatBuffer params) {
    gl2.glGetUniformfv(program, location, params);
  }

  @Override
  public void getUniformiv(int program, int location, IntBuffer params) {
    gl2.glGetUniformiv(program, location, params);
  }

  @Override
  public boolean isProgram(int program) {
    return gl2.glIsProgram(program);
  }

  @Override
  public void getProgramiv(int program, int pname, IntBuffer params) {
    gl2.glGetProgramiv(program, pname, params);
  }

  @Override
  public String getProgramInfoLog(int program) {
    int[] val = { 0 };
    gl2.glGetShaderiv(program, GL2ES2.GL_INFO_LOG_LENGTH, val, 0);
    int length = val[0];

    if (0 < length) {
      byte[] log = new byte[length];
      gl2.glGetProgramInfoLog(program, length, val, 0, log, 0);
      return new String(log);
    } else {
      return "Unknow error";
    }
  }

  ///////////////////////////////////////////////////////////

  // Per-Fragment Operations

  @Override
  public void scissor(int x, int y, int w, int h) {
    float scale = pg.getPixelScale();
    gl.glScissor((int)scale * x, (int)(scale * y), (int)(scale * w), (int)(scale * h));
//    gl.glScissor(x, y, w, h);
  }

  @Override
  public void sampleCoverage(float value, boolean invert) {
    gl2.glSampleCoverage(value, invert);
  }

  @Override
  public void stencilFunc(int func, int ref, int mask) {
    gl2.glStencilFunc(func, ref, mask);
  }

  @Override
  public void stencilFuncSeparate(int face, int func, int ref, int mask) {
    gl2.glStencilFuncSeparate(face, func, ref, mask);
  }

  @Override
  public void stencilOp(int sfail, int dpfail, int dppass) {
    gl2.glStencilOp(sfail, dpfail, dppass);
  }

  @Override
  public void stencilOpSeparate(int face, int sfail, int dpfail, int dppass) {
    gl2.glStencilOpSeparate(face, sfail, dpfail, dppass);
  }

  @Override
  public void depthFunc(int func) {
    gl.glDepthFunc(func);
  }

  @Override
  public void blendEquation(int mode) {
    gl.glBlendEquation(mode);
  }

  @Override
  public void blendEquationSeparate(int modeRGB, int modeAlpha) {
    gl.glBlendEquationSeparate(modeRGB, modeAlpha);
  }

  @Override
  public void blendFunc(int src, int dst) {
    gl.glBlendFunc(src, dst);
  }

  @Override
  public void blendFuncSeparate(int srcRGB, int dstRGB, int srcAlpha, int dstAlpha) {
    gl.glBlendFuncSeparate(srcRGB, dstRGB, srcAlpha, dstAlpha);
  }

  @Override
  public void blendColor(float red, float green, float blue, float alpha) {
    gl2.glBlendColor(red, green, blue, alpha);
  }

  @Override
  public void alphaFunc(int func, float ref) {
    if (gl2x != null) {
      gl2x.glAlphaFunc(func, ref);
    } else {
      throw new RuntimeException(String.format(MISSING_GLFUNC_ERROR, "glAlphaFunc()"));
    }
  }

  ///////////////////////////////////////////////////////////

  // Whole Framebuffer Operations

  @Override
  public void colorMask(boolean r, boolean g, boolean b, boolean a) {
    gl.glColorMask(r, g, b, a);
  }

  @Override
  public void depthMask(boolean mask) {
    gl.glDepthMask(mask);
  }

  @Override
  public void stencilMask(int mask) {
    gl.glStencilMask(mask);
  }

  @Override
  public void stencilMaskSeparate(int face, int mask) {
    gl2.glStencilMaskSeparate(face, mask);
  }

  @Override
  public void clear(int buf) {
    gl.glClear(buf);
  }

  @Override
  public void clearColor(float r, float g, float b, float a) {
    gl.glClearColor(r, g, b, a);
  }

  @Override
  public void clearDepth(float d) {
    gl.glClearDepthf(d);
  }

  @Override
  public void clearStencil(int s) {
    gl.glClearStencil(s);
  }

  ///////////////////////////////////////////////////////////

  // Framebuffers Objects

  @Override
  protected void bindFramebufferImpl(int target, int framebuffer) {
    gl.glBindFramebuffer(target, framebuffer);
  }

  @Override
  public void deleteFramebuffers(int n, IntBuffer framebuffers) {
    gl.glDeleteFramebuffers(n, framebuffers);
  }

  @Override
  public void genFramebuffers(int n, IntBuffer framebuffers) {
    gl.glGenFramebuffers(n, framebuffers);
  }

  @Override
  public void bindRenderbuffer(int target, int renderbuffer) {
    gl.glBindRenderbuffer(target, renderbuffer);
  }

  @Override
  public void deleteRenderbuffers(int n, IntBuffer renderbuffers) {
    gl.glDeleteRenderbuffers(n, renderbuffers);
  }

  @Override
  public void genRenderbuffers(int n, IntBuffer renderbuffers) {
    gl.glGenRenderbuffers(n, renderbuffers);
  }

  @Override
  public void renderbufferStorage(int target, int internalFormat, int width, int height) {
    gl.glRenderbufferStorage(target, internalFormat, width, height);
  }

  @Override
  public void framebufferRenderbuffer(int target, int attachment, int rendbuferfTarget, int renderbuffer) {
    gl.glFramebufferRenderbuffer(target, attachment, rendbuferfTarget, renderbuffer);
  }

  @Override
  public void framebufferTexture2D(int target, int attachment, int texTarget, int texture, int level) {
    gl.glFramebufferTexture2D(target, attachment, texTarget, texture, level);
  }

  @Override
  public int checkFramebufferStatus(int target) {
    return gl.glCheckFramebufferStatus(target);
  }

  @Override
  public boolean isFramebuffer(int framebuffer) {
    return gl2.glIsFramebuffer(framebuffer);
  }

  @Override
  public void getFramebufferAttachmentParameteriv(int target, int attachment, int pname, IntBuffer params) {
    gl2.glGetFramebufferAttachmentParameteriv(target, attachment, pname, params);
  }

  @Override
  public boolean isRenderbuffer(int renderbuffer) {
    return gl2.glIsRenderbuffer(renderbuffer);
  }

  @Override
  public void getRenderbufferParameteriv(int target, int pname, IntBuffer params) {
    gl2.glGetRenderbufferParameteriv(target, pname, params);
  }

  @Override
  public void blitFramebuffer(int srcX0, int srcY0, int srcX1, int srcY1, int dstX0, int dstY0, int dstX1, int dstY1, int mask, int filter) {
    if (gl2x != null) {
      gl2x.glBlitFramebuffer(srcX0, srcY0, srcX1, srcY1, dstX0, dstY0, dstX1, dstY1, mask, filter);
    } else if (gl3 != null) {
      gl3.glBlitFramebuffer(srcX0, srcY0, srcX1, srcY1, dstX0, dstY0, dstX1, dstY1, mask, filter);
    } else {
      throw new RuntimeException(String.format(MISSING_GLFUNC_ERROR, "glBlitFramebuffer()"));
    }
  }

  @Override
  public void renderbufferStorageMultisample(int target, int samples, int format, int width, int height) {
    if (gl2x != null) {
      gl2x.glRenderbufferStorageMultisample(target, samples, format, width, height);
    } else if (gl3 != null) {
      gl3.glRenderbufferStorageMultisample(target, samples, format, width, height);
    } else {
      throw new RuntimeException(String.format(MISSING_GLFUNC_ERROR, "glRenderbufferStorageMultisample()"));
    }
  }

  @Override
  public void readBuffer(int buf) {
    if (gl2x != null) {
      gl2x.glReadBuffer(buf);
    } else if (gl3 != null) {
      gl3.glReadBuffer(buf);
    } else {
      throw new RuntimeException(String.format(MISSING_GLFUNC_ERROR, "glReadBuffer()"));
    }
  }

  @Override
  public void drawBuffer(int buf) {
    if (gl2x != null) {
      gl2x.glDrawBuffer(buf);
    } else if (gl3 != null) {
      gl3.glDrawBuffer(buf);
    } else {
      throw new RuntimeException(String.format(MISSING_GLFUNC_ERROR, "glDrawBuffer()"));
    }
  }
}
