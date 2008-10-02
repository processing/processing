/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2004-08 Ben Fry and Casey Reas

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

import processing.core.*;

import java.awt.*;
import java.awt.font.*;
import java.awt.geom.*;
import java.nio.*;

import javax.media.opengl.*;
import javax.media.opengl.glu.*;
import com.sun.opengl.util.*;


/**
 * Implementation of the PGraphics API that employs OpenGL rendering via JOGL.
 * <p/>
 * JOGL requires Java 1.4 or higher, so there are no restrictions on this
 * code to be compatible with Java 1.1 or Java 1.3.
 * <p/>
 * This code relies on PGraphics3D for all lighting and transformations.
 * Meaning that translate(), rotate(), and any lighting will be done in
 * PGraphics3D, and OpenGL is only used to blit lines and triangles as fast
 * as it possibly can.
 * <p/>
 * For this reason, OpenGL may not be accelerated as far as it could be,
 * but I don't have the time to maintain two separate versions of the
 * renderer. My development time must always be focused on implementation
 * and covering features first, and optimizing later.
 * <p/>
 * Further, the difference may be negligible, as the primary slowdown
 * in Java is moving pixels (i.e. a large frame buffer is nearly impossible
 * because Java just can't do a MemoryImageSource at screen resolution)
 * and the overhead from JNI tends to be significant. In the latter case,
 * we may even save time in some cases where a large number of calls to
 * OpenGL would otherwise be used, but that's probably a stretch.
 * <p/>
 * The code is also very messy, while features are being added and
 * removed rapidly as we head towards 1.0. Things got particularly ugly
 * as we approached beta while both Simon and I were working on it.
 * Relax, we'll get it fixed up later.
 * <p/>
 * When exporting applets, the JOGL Applet Launcher is used. More information
 * about the launcher can be found at its <A HREF="http://download.java.net/media/jogl/builds/nightly/javadoc_public/com/sun/opengl/util/JOGLAppletLauncher.html">documentation page</A>.
 */
public class PGraphicsOpenGL extends PGraphics3D {
  protected GLDrawable drawable;   // the rendering 'surface'
  protected GLContext context;     // the rendering context (holds rendering state info)

  public GL gl;
  public GLU glu;
  //public GLCanvas canvas;

  //protected FloatBuffer projectionFloatBuffer;
  protected float[] projectionFloats;

  protected GLUtessellator tobj;
  protected TessCallback tessCallback;

  /// Buffer to hold light values before they're sent to OpenGL
  //protected FloatBuffer lightBuffer;
  protected float[] lightArray = new float[] { 1, 1, 1 };

  /// Used to hold color values to be sent to OpenGL
  protected FloatBuffer colorBuffer;

  /// Used to store empty values to be passed when a light has no ambient value
  protected FloatBuffer zeroBuffer;

  /// IntBuffer to go with the pixels[] array
  protected IntBuffer pixelBuffer;

  /**
   * Set to true if the host system is big endian (PowerPC, MIPS, SPARC),
   * false if little endian (x86 Intel for Mac or PC).
   */
  static public boolean BIG_ENDIAN =
    ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN;


  public PGraphicsOpenGL() {
    glu = new GLU();

    tobj = glu.gluNewTess();

    // unfortunately glu.gluDeleteTess(tobj); is never called
    //glu.gluTessProperty(tobj, GLU.GLU_TESS_WINDING_RULE,
    //                  GLU.GLU_TESS_WINDING_NONZERO);
    //glu.gluTessProperty(tobj, GLU.GLU_TESS_WINDING_RULE,
    //                  GLU.GLU_TESS_WINDING_POSITIVE);
    //GLU.GLU_TESS_WINDING_ODD);
    //glu.gluTessProperty(tobj, GLU.GLU_TESS_BOUNDARY_ONLY,
    //                  GL.GL_TRUE);

    tessCallback = new TessCallback();
    glu.gluTessCallback(tobj, GLU.GLU_TESS_BEGIN, tessCallback);
    glu.gluTessCallback(tobj, GLU.GLU_TESS_END, tessCallback);
    glu.gluTessCallback(tobj, GLU.GLU_TESS_VERTEX, tessCallback);
    glu.gluTessCallback(tobj, GLU.GLU_TESS_COMBINE, tessCallback);
    glu.gluTessCallback(tobj, GLU.GLU_TESS_ERROR, tessCallback);

//    lightBuffer = BufferUtil.newFloatBuffer(4);
//    lightBuffer.put(3, 1.0f);
//    lightBuffer.rewind();
  }


  /**
   * Called by resize(), this handles creating the actual GLCanvas the
   * first time around, or simply resizing it on subsequent calls.
   * There is no pixel array to allocate for an OpenGL canvas
   * because OpenGL's pixel buffer is all handled internally.
   */
  protected void allocate() {
    if (context == null) {
//      System.out.println("PGraphicsOpenGL.allocate() for " + width + " " + height);
//      new Exception().printStackTrace(System.out);
      // If OpenGL 2X or 4X smoothing is enabled, setup caps object for them
      GLCapabilities capabilities = new GLCapabilities();
      if (hints[ENABLE_OPENGL_2X_SMOOTH]) {
        capabilities.setSampleBuffers(true);
        capabilities.setNumSamples(2);
      } else if (hints[ENABLE_OPENGL_4X_SMOOTH]) {
        capabilities.setSampleBuffers(true);
        capabilities.setNumSamples(4);
      }

      // get a rendering surface and a context for this canvas
      GLDrawableFactory factory = GLDrawableFactory.getFactory();

      /*
      if (PApplet.platform == PConstants.LINUX) {
        GraphicsConfiguration pconfig = parent.getGraphicsConfiguration();
        System.out.println("parent config is " + pconfig);

        //      GraphicsDevice device = config.getDevice();
        //AbstractGraphicsDevice agd = new AbstractGraphicsDevice(device);
        //AbstractGraphicsConfiguration agc = factory.chooseGraphicsConfiguration(capabilities, null, null);

        AWTGraphicsConfiguration agc = (AWTGraphicsConfiguration)
        factory.chooseGraphicsConfiguration(capabilities, null, null);
        GraphicsConfiguration config = agc.getGraphicsConfiguration();
        System.out.println("agc config is " + config);
      }
      */

      drawable = factory.getGLDrawable(parent, capabilities, null);
      context = drawable.createContext(null);

      // need to get proper opengl context since will be needed below
      gl = context.getGL();
      // Flag defaults to be reset on the next trip into beginDraw().
      settingsInited = false;

    } else {
      // changing for 0100, need to resize rather than re-allocate
      //System.out.println("PGraphicsOpenGL.allocate() again for " + width + " " + height);
      reapplySettings();
    }
  }


  public void hint(int which) {
    // make note of whether these are set, if they are,
    // then will prevent the new renderer exception from being thrown.
    boolean opengl2X = hints[ENABLE_OPENGL_2X_SMOOTH];
    boolean opengl4X = hints[ENABLE_OPENGL_4X_SMOOTH];
    super.hint(which);

    if (which == DISABLE_DEPTH_TEST) {
      gl.glDisable(GL.GL_DEPTH_TEST);

    } else if (which == ENABLE_DEPTH_TEST) {
      gl.glEnable(GL.GL_DEPTH_TEST);

    } else if (which == ENABLE_OPENGL_2X_SMOOTH) {
      if (!opengl2X) {
        releaseContext();
        context.destroy();
        context = null;
        allocate();
        throw new PApplet.RendererChangeException();
      }
    } else if (which == ENABLE_OPENGL_4X_SMOOTH) {
      if (!opengl4X) {
        releaseContext();
        context.destroy();
        context = null;
        allocate();
        throw new PApplet.RendererChangeException();
      }
    }
  }


  /**
   * Get the current context, for use by libraries that need to talk to it.
   */
  public GLContext getContext() {
    return context;
  }


  /**
   * Make the OpenGL rendering context current for this thread.
   */
  protected void detainContext() {
    try {
      while (context.makeCurrent() == GLContext.CONTEXT_NOT_CURRENT) {
//        System.out.println("Context not yet current...");
//        new Exception().printStackTrace(System.out);
//        Thread.sleep(1000);
        Thread.sleep(10);
      }
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }


  /**
   * Release the context, otherwise the AWT lock on X11 will not be released
   */
  protected void releaseContext() {
    context.release();
  }


  /**
   * OpenGL cannot draw until a proper native peer is available, so this
   * returns the value of PApplet.isDisplayable() (inherited from Component).
   */
  public boolean canDraw() {
    return parent.isDisplayable();
  }


  public void beginDraw() {
    //if (!parent.isDisplayable()) return;

    // When using an offscreen buffer, the drawable instance will be null.
    // The offscreen buffer uses the drawing context of the main PApplet.
    if (drawable != null) {
      // Call setRealized() after addNotify() has been called
      drawable.setRealized(parent.isDisplayable());
      //System.out.println("OpenGL beginDraw() setting realized " + parent.isDisplayable());
      if (parent.isDisplayable()) {
        //System.out.println("  we'll realize it alright");
        drawable.setRealized(true);
      } else {
        //System.out.println("  not yet ready to be realized");
        return;  // Should have called canDraw() anyway
      }
      detainContext();
    }

    // On the first frame that's guaranteed to be on screen,
    // and the component valid and all that, ask for focus.
//    if ((parent != null) && parent.frameCount == 1) {
//      canvas.requestFocus();
//    }

    super.beginDraw();

    report("top beginDraw()");

    gl.glDisable(GL.GL_LIGHTING);
    for (int i = 0; i < MAX_LIGHTS; i++) {
      gl.glDisable(GL.GL_LIGHT0 + i);
    }

    gl.glMatrixMode(GL.GL_PROJECTION);
    if (projectionFloats == null) {
      projectionFloats = new float[] {
        projection.m00, projection.m10, projection.m20, projection.m30,
        projection.m01, projection.m11, projection.m21, projection.m31,
        projection.m02, projection.m12, projection.m22, projection.m32,
        projection.m03, projection.m13, projection.m23, projection.m33
      };
    } else {
      projectionFloats[0] = projection.m00;
      projectionFloats[1] = projection.m10;
      projectionFloats[2] = projection.m20;
      projectionFloats[3] = projection.m30;

      projectionFloats[4] = projection.m01;
      projectionFloats[5] = projection.m11;
      projectionFloats[6] = projection.m21;
      projectionFloats[7] = projection.m31;

      projectionFloats[8] = projection.m02;
      projectionFloats[9] = projection.m12;
      projectionFloats[10] = projection.m22;
      projectionFloats[11] = projection.m32;

      projectionFloats[12] = projection.m03;
      projectionFloats[13] = projection.m13;
      projectionFloats[14] = projection.m23;
      projectionFloats[15] = projection.m33;
    }
    //projection.print();
    gl.glLoadMatrixf(projectionFloats, 0);

    gl.glMatrixMode(GL.GL_MODELVIEW);
    gl.glLoadIdentity();
    // Flip Y-axis to make y count from 0 downwards
    gl.glScalef(1, -1, 1);

    // these are necessary for alpha (i.e. fonts) to work
    gl.glEnable(GL.GL_BLEND);
    gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);

    // this is necessary for 3D drawing
    if (hints[DISABLE_DEPTH_TEST]) {
      gl.glDisable(GL.GL_DEPTH_TEST);
    } else {
      gl.glEnable(GL.GL_DEPTH_TEST);
    }
    // use <= since that's what processing.core does
    gl.glDepthFunc(GL.GL_LEQUAL);

    // because y is flipped
    gl.glFrontFace(GL.GL_CW);

    // coloured stuff
    gl.glEnable(GL.GL_COLOR_MATERIAL);
    gl.glColorMaterial(GL.GL_FRONT_AND_BACK, GL.GL_AMBIENT_AND_DIFFUSE);
    gl.glColorMaterial(GL.GL_FRONT_AND_BACK, GL.GL_SPECULAR);

    // these tend to make life easier
    // (but sometimes at the expense of a little speed)
    // Not using them right now because we're doing our own lighting.
    //gl.glEnable(GL.GL_NORMALIZE);
    //gl.glEnable(GL.GL_AUTO_NORMAL); // I think this is OpenGL 1.2 only
    //gl.glEnable(GL.GL_RESCALE_NORMAL);
    //gl.GlLightModeli(GL.GL_LIGHT_MODEL_COLOR_CONTROL, GL_SEPARATE_SPECULAR_COLOR);

    report("bot beginDraw()");
    // are there other things to do here?
    //System.out.println("beginDraw() stop error " + PApplet.hex(gl.glGetError()));
  }


  public void endDraw() {
    //System.out.println("endDraw() error " + PApplet.hex(gl.glGetError()));

    report("top endDraw()");

    if (hints[ENABLE_DEPTH_SORT]) {
      flush();
    }

    if (drawable != null) {
      drawable.swapBuffers();
    }

    //insideDraw = false;
    report("bot endDraw()");

    if (drawable != null) {
      releaseContext();
    }
  }


  private float ctm[];

  // this would also need to set up the lighting.. ?
  public GL beginGL() {
    //beginDraw();  // frame will have already started
    gl.glPushMatrix();

    // load p5 modelview into the opengl modelview
    if (ctm == null) ctm = new float[16];

    ctm[0] = modelview.m00;
    ctm[1] = modelview.m10;
    ctm[2] = modelview.m20;
    ctm[3] = modelview.m30;

    ctm[4] = modelview.m01;
    ctm[5] = modelview.m11;
    ctm[6] = modelview.m21;
    ctm[7] = modelview.m31;

    ctm[8] = modelview.m02;
    ctm[9] = modelview.m12;
    ctm[10] = modelview.m22;
    ctm[11] = modelview.m32;

    ctm[12] = modelview.m03;
    ctm[13] = modelview.m13;
    ctm[14] = modelview.m23;
    ctm[15] = modelview.m33;

    // apply this modelview and get to work
    gl.glMultMatrixf(ctm, 0);

    return gl;
  }


  public void endGL() {
    // remove the p5 modelview from opengl
    gl.glPopMatrix();
  }


  protected void endShapeLighting(boolean lights) {
    super.endShapeLighting(lights);
    
    // For now do our own lighting--sum the specular and diffuse light colors
    if (lights) {
      for (int i = shapeFirst; i < shapeLast; i++) {
        float v[] = vertices[i];
        v[R] = clamp(v[R] + v[SPR]);
        v[G] = clamp(v[G] + v[SPG]);
        v[B] = clamp(v[B] + v[SPB]);
      }
    }
  }


  protected void renderTriangles() {
    report("render_triangles in");
    //System.out.println("rendering " + triangleCount + " triangles");

    for (int i = 0; i < triangleCount; i ++) {
      //System.out.println("rendering triangle " + i);

      float a[] = vertices[triangles[i][VERTEX1]];
      float b[] = vertices[triangles[i][VERTEX2]];
      float c[] = vertices[triangles[i][VERTEX3]];
      //System.out.println(triangles[i][VERTEX1] + " " +
      //                 triangles[i][VERTEX2] + " " +
      //                 triangles[i][VERTEX3] + " " + vertexCount);

      // This is only true when not textured.
      // We really should pass specular straight through to triangle rendering.
      float ar = clamp(triangleColors[i][0][TRI_DIFFUSE_R] + triangleColors[i][0][TRI_SPECULAR_R]);
      float ag = clamp(triangleColors[i][0][TRI_DIFFUSE_G] + triangleColors[i][0][TRI_SPECULAR_G]);
      float ab = clamp(triangleColors[i][0][TRI_DIFFUSE_B] + triangleColors[i][0][TRI_SPECULAR_B]);
      float br = clamp(triangleColors[i][1][TRI_DIFFUSE_R] + triangleColors[i][1][TRI_SPECULAR_R]);
      float bg = clamp(triangleColors[i][1][TRI_DIFFUSE_G] + triangleColors[i][1][TRI_SPECULAR_G]);
      float bb = clamp(triangleColors[i][1][TRI_DIFFUSE_B] + triangleColors[i][1][TRI_SPECULAR_B]);
      float cr = clamp(triangleColors[i][2][TRI_DIFFUSE_R] + triangleColors[i][2][TRI_SPECULAR_R]);
      float cg = clamp(triangleColors[i][2][TRI_DIFFUSE_G] + triangleColors[i][2][TRI_SPECULAR_G]);
      float cb = clamp(triangleColors[i][2][TRI_DIFFUSE_B] + triangleColors[i][2][TRI_SPECULAR_B]);

      if (raw != null) {
        raw.colorMode(RGB, 1);
        raw.noStroke();
        raw.beginShape(TRIANGLES);
      }

      int textureIndex = triangles[i][TEXTURE_INDEX];
      if (textureIndex != -1) {
        report("before enable");
        gl.glEnable(GL.GL_TEXTURE_2D);
        report("after enable");

        PImage texture = textures[textureIndex];
        bindTexture(texture);

        report("before bind");
        //System.out.println(gl.glIsTexture(image.tindex));

        //GL_PERSPECTIVE_CORRECTION_HINT to GL_NICEST
        // and running the example again. To do this, use glHint().

        // these don't seem to do much
        //gl.glPixelStorei(GL.GL_UNPACK_ALIGNMENT, 1);
        //gl.glPixelStorei(GL.GL_UNPACK_SWAP_BYTES, 1);

        //if (image.format == ALPHA) {
        //System.out.println("binding with replace");
        //  gl.glTexEnvf(GL.GL_TEXTURE_ENV,
        //             GL.GL_TEXTURE_ENV_MODE, GL.GL_REPLACE);
        //} else {
        //gl.glTexEnvf(GL.GL_TEXTURE_ENV,
        //             GL.GL_TEXTURE_ENV_MODE, GL.GL_MODULATE);
        //}

        report("after bind");

        /*
        } else {
          cache(texture);
          cash = (ImageCache) texture.cache;

          report("non-binding 0");
          // may be needed for windows
          report("non-binding 1");

          gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, 4,
                          cash.twidth, cash.theight,
                          0, GL.GL_BGRA_EXT, GL.GL_UNSIGNED_BYTE,
                          cash.tpixels);

          report("non-binding 2 " + cash.twidth + " " +
                 cash.theight + " " + cash.tpixels);
          gl.glTexParameterf(GL.GL_TEXTURE_2D,
                             GL.GL_TEXTURE_WRAP_S, GL.GL_CLAMP);
          gl.glTexParameterf(GL.GL_TEXTURE_2D,
                             GL.GL_TEXTURE_WRAP_T, GL.GL_CLAMP);
          gl.glTexParameterf(GL.GL_TEXTURE_2D,
                             GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
          gl.glTexParameterf(GL.GL_TEXTURE_2D,
                             GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);

          report("non-binding 3");
          gl.glTexEnvf(GL.GL_TEXTURE_ENV,
                       GL.GL_TEXTURE_ENV_MODE, GL.GL_MODULATE);
        }
        */

        ImageCache cash = (ImageCache) texture.cache;
        float uscale = (float) texture.width / (float) cash.twidth;
        float vscale = (float) texture.height / (float) cash.theight;

        //int tindex = ((ImageCache) texture.cache).tindex;

        gl.glBegin(GL.GL_TRIANGLES);

        gl.glColor4f(ar, ag, ab, a[A]);
        gl.glTexCoord2f(a[U] * uscale, a[V] * vscale);
        gl.glNormal3f(a[NX], a[NY], a[NZ]);
        //gl.glEdgeFlag(a[EDGE] == 1);
        gl.glVertex3f(a[VX], a[VY], a[VZ]);

        gl.glColor4f(br, bg, bb, b[A]);
        gl.glTexCoord2f(b[U] * uscale, b[V] * vscale);
        gl.glNormal3f(b[NX], b[NY], b[NZ]);
        //gl.glEdgeFlag(a[EDGE] == 1);
        gl.glVertex3f(b[VX], b[VY], b[VZ]);

        gl.glColor4f(cr, cg, cb, c[A]);
        gl.glTexCoord2f(c[U] * uscale, c[V] * vscale);
        gl.glNormal3f(c[NX], c[NY], c[NZ]);
        //gl.glEdgeFlag(a[EDGE] == 1);
        gl.glVertex3f(c[VX], c[VY], c[VZ]);

        gl.glEnd();
        report("non-binding 6");

        gl.glDisable(GL.GL_TEXTURE_2D);

        if (raw != null) {
          if (raw instanceof PGraphics3D) {
            if ((a[VW] != 0) && (b[VW] != 0) && (c[VW] != 0)) {
              raw.texture(texture);
              raw.fill(ar, ag, ab, a[A]);
              raw.vertex(a[VX] / a[VW], a[VY] / a[VW], a[VZ] / a[VW],
                         a[U] * uscale, a[V] * vscale);
              raw.fill(br, bg, bb, b[A]);
              raw.vertex(b[VX] / b[VW], b[VY] / b[VW], b[VZ] / b[VW],
                         b[U] * uscale, b[V] * vscale);
              raw.fill(cr, cg, cb, c[A]);
              raw.vertex(c[VX] / c[VW], c[VY] / c[VW], c[VZ] / c[VW],
                         c[U] * uscale, c[V] * vscale);
            } else {
              if (reasonablePoint(a[TX], a[TY], a[TZ]) &&
                  reasonablePoint(b[TX], b[TY], b[TZ]) &&
                  reasonablePoint(c[TX], c[TY], c[TZ])) {
                raw.fill(ar, ag, ab, a[A]);
                raw.vertex(a[TX], a[TY], a[U] * uscale, a[V] * vscale);
                raw.fill(br, bg, bb, b[A]);
                raw.vertex(b[TX], b[TY], b[U] * uscale, b[V] * vscale);
                raw.fill(cr, cg, cb, c[A]);
                raw.vertex(c[TX], c[TY], c[U] * uscale, c[V] * vscale);
              }
            }
          }
        }

      } else {  // image has no texture
        gl.glBegin(GL.GL_TRIANGLES);

        //System.out.println("tri " + a[VX] + " " + a[VY] + " " + a[VZ]);
        //System.out.println("    " + b[VX] + " " + b[VY] + " " + b[VZ]);
        //System.out.println("    " + c[VX] + " " + c[VY] + " " + c[VZ]);

        gl.glColor4f(ar, ag, ab, a[A]);
        gl.glNormal3f(a[NX], a[NY], a[NZ]);
        //gl.glEdgeFlag(a[EDGE] == 1);
        gl.glVertex3f(a[VX], a[VY], a[VZ]);

        gl.glColor4f(br, bg, bb, b[A]);
        gl.glNormal3f(b[NX], b[NY], b[NZ]);
        //gl.glEdgeFlag(a[EDGE] == 1);
        gl.glVertex3f(b[VX], b[VY], b[VZ]);

        gl.glColor4f(cr, cg, cb, c[A]);
        gl.glNormal3f(c[NX], c[NY], c[NZ]);
        //gl.glEdgeFlag(a[EDGE] == 1);
        gl.glVertex3f(c[VX], c[VY], c[VZ]);

        if (raw != null) {
          if (raw instanceof PGraphics3D) {
            if ((a[VW] != 0) && (b[VW] != 0) && (c[VW] != 0)) {
              raw.fill(ar, ag, ab, a[A]);
              raw.vertex(a[VX] / a[VW], a[VY] / a[VW], a[VZ] / a[VW]);
              raw.fill(br, bg, bb, b[A]);
              raw.vertex(b[VX] / b[VW], b[VY] / b[VW], b[VZ] / b[VW]);
              raw.fill(cr, cg, cb, c[A]);
              raw.vertex(c[VX] / c[VW], c[VY] / c[VW], c[VZ] / c[VW]);
            }
          } else {
            if (reasonablePoint(a[TX], a[TY], a[TZ]) &&
                reasonablePoint(b[TX], b[TY], b[TZ]) &&
                reasonablePoint(c[TX], c[TY], c[TZ])) {
              raw.fill(ar, ag, ab, a[A]);
              raw.vertex(a[TX], a[TY]);
              raw.fill(br, bg, bb, b[A]);
              raw.vertex(b[TX], b[TY]);
              raw.fill(cr, cg, cb, c[A]);
              raw.vertex(c[TX], c[TY]);
            }
          }
        }
        gl.glEnd();
      }
    }
    if (raw != null) {
      raw.endShape();
    }
    triangleCount = 0;
    report("render_triangles out");
  }


  // TODO bad clipping, replace me
  protected boolean reasonablePoint(float x, float y, float z) {
    return ((z < 1) && (x > -width) && (x < width*2) && (y > -height) && (y < height*2));
  }

  // TODO need to remove this
  public float uscale(PImage texture) {
    ImageCache cash = (ImageCache) texture.cache;
    return (float) texture.width / (float) cash.twidth;
  }

  // TODO need to remove this
  public float vscale(PImage texture) {
    ImageCache cash = (ImageCache) texture.cache;
    return (float) texture.height / (float) cash.theight;
  }

  // TODO need to remove this
  public void bindTexture(PImage texture) {
    ImageCache cash = (ImageCache) texture.cache;  // as in johnny
    if (cash == null) {
      cash = new ImageCache();
      texture.cache = cash;
      texture.setModified(true);
    }

    if (texture.isModified()) {
      //System.out.println("texture modified");
      // TODO make this more efficient and just update a sub-part
      // based on mx1 et al, also use gl function to update
      // only a sub-portion of the image.
      cash.rebind(texture);
      // clear the modified flag
      texture.setModified(false);

    } else {
      gl.glBindTexture(GL.GL_TEXTURE_2D, cash.tindex);
    }
  }


  public void renderLines() {
    report("render_lines in");

    int i = 0;
    for (int j = 0; j < pathCount; j++) {
      float sw = vertices[lines[i][VERTEX1]][SW];
      //report("render_lines 1");
      // stroke weight zero will cause a gl error
      //if (lines[i][STROKE_WEIGHT] == 0) continue;
      if (sw == 0) continue;
      // glLineWidth has to occur outside glBegin/glEnd
      //gl.glLineWidth(lines[i][STROKE_WEIGHT]);
      gl.glLineWidth(sw);
      //report("render_lines 2 " + lines[i][STROKE_WEIGHT]);
      gl.glBegin(GL.GL_LINE_STRIP);

      if (raw != null) {
        raw.strokeWeight(sw);
      }

      // always draw a first point
      float a[] = vertices[lines[i][VERTEX1]];
      gl.glColor4f(a[SR], a[SG], a[SB], a[SA]);
      //gl.glEdgeFlag(a[EDGE] == 1);
      gl.glVertex3f(a[VX], a[VY], a[VZ]);
      //System.out.println("First point: " + a[VX] +", "+ a[VY] +", "+ a[VZ]);

      if (raw != null) {
        if (a[SA] > EPSILON) {  // don't draw if transparent
          raw.colorMode(RGB, 1);
          raw.noFill();  // 0116
          raw.beginShape(); // 0116  LINE_STRIP);
          if (raw instanceof PGraphics3D) {
            if (a[VW] != 0) {
              raw.stroke(a[SR], a[SG], a[SB], a[SA]);
              raw.vertex(a[VX] / a[VW], a[VY] / a[VW], a[VZ] / a[VW]);
            }
          } else {
            raw.stroke(a[SR], a[SG], a[SB], a[SA]);
            raw.vertex(a[TX], a[TY]);
          }
        }
      }

      // on this and subsequent lines, only draw the second point
      //System.out.println(pathLength[j]);
      for (int k = 0; k < pathLength[j]; k++) {
        float b[] = vertices[lines[i][VERTEX2]];
        gl.glColor4f(b[SR], b[SG], b[SB], b[SA]);
        //gl.glEdgeFlag(a[EDGE] == 1);
        gl.glVertex3f(b[VX], b[VY], b[VZ]);
        if (raw != null) {
          if (raw instanceof PGraphics3D) {
            if ((a[VW] != 0) && (b[VW] != 0)) {
              raw.stroke(b[SR], b[SG], b[SB], b[SA]);
              raw.vertex(b[VX] / b[VW], b[VY] / b[VW], b[VZ] / b[VW]);
            }
          } else {
            //System.out.println("writing 2d vertex");
            raw.stroke(b[SR], b[SG], b[SB], b[SA]);
            raw.vertex(b[TX], b[TY]);
          }
        }
        i++;
      }
      if (raw != null) {
        raw.endShape();
      }
      gl.glEnd();
    }
    report("render_lines out");
  }


  /**
   * Handled entirely by OpenGL, so use this to override the superclass.
   */
  //protected void light_and_transform() {
  //}


  //GL will do the clipping for us
  protected void add_line(int a, int b) {
    add_line_no_clip(a, b);
  }


  // GL will do the clipping for us
  protected void add_triangle(int a, int b, int c) {
    add_triangle_no_clip(a, b, c);
  }


  //////////////////////////////////////////////////////////////


  static int maxTextureSize;

  int[] deleteQueue = new int[10];
  int deleteQueueCount = 0;

  protected class ImageCache {
    int tindex = -1;  // not yet ready
    int tpixels[];
    IntBuffer tbuffer;
    public int twidth, theight;

    int[] tp;


    /**
     * Delete any texture memory that had been allocated.
     * Added for 0125 to deal with memory problems reported in Bug #150.
     */
    protected void finalize() {
      if (deleteQueue.length == deleteQueueCount) {
        deleteQueue = (int[]) PApplet.expand(deleteQueue);
      }
      if (tindex != -1) {
        deleteQueue[deleteQueueCount++] = tindex;
      }
    }


    /**
     * Generate a texture ID and do the necessary bitshifting for the image.
     */
    public void rebind(PImage source) {
      if (deleteQueueCount != 0) {
        //gl.glDeleteTextures(1, new int[] { tindex }, 0);
        gl.glDeleteTextures(deleteQueueCount, deleteQueue, 0);
        deleteQueueCount = 0;
      }

      //System.out.println("rebinding texture for " + source);
      if (tindex != -1) {
        // free up the old memory
        gl.glDeleteTextures(1, new int[] { tindex }, 0);
      }
      // generate a new texture number to bind to
      int[] tmp = new int[1];
      gl.glGenTextures(1, tmp, 0);
      tindex = tmp[0];
      //System.out.println("got index " + tindex);

      // bit shifting this might be more efficient
      int width2 = nextPowerOfTwo(source.width);
      //(int) Math.pow(2, Math.ceil(Math.log(source.width) / Math.log(2)));
      int height2 = nextPowerOfTwo(source.height);
      //(int) Math.pow(2, Math.ceil(Math.log(source.height) / Math.log(2)));

      // use glGetIntegerv with the argument GL_MAX_TEXTURE_SIZE
      // to figure out min/max texture sizes
      if (maxTextureSize == 0) {
        int maxSize[] = new int[1];
        gl.glGetIntegerv(GL.GL_MAX_TEXTURE_SIZE, maxSize, 0);
        maxTextureSize = maxSize[0];
        //System.out.println("max texture size is " + maxTextureSize);
      }
      if ((width2 > maxTextureSize) || (height2 > maxTextureSize)) {
        throw new RuntimeException("Image width and height cannot be" +
                                   " larger than " + maxTextureSize +
                                   " with your graphics card.");
      }

      if ((width2 > twidth) || (height2 > theight)) {
        // either twidth/theight are zero, or size has changed
        tpixels = null;
      }
      if (tpixels == null) {
        twidth = width2;
        theight = height2;
        tpixels = new int[twidth * theight];
        tbuffer = BufferUtil.newIntBuffer(twidth * theight);
      }

      // copy image data into the texture
      int p = 0;
      int t = 0;

      if (BIG_ENDIAN) {
        switch (source.format) {
        case ALPHA:
          for (int y = 0; y < source.height; y++) {
            for (int x = 0; x < source.width; x++) {
              tpixels[t++] = 0xFFFFFF00 | source.pixels[p++];
            }
            t += twidth - source.width;
          }
          break;

        case RGB:
          for (int y = 0; y < source.height; y++) {
            for (int x = 0; x < source.width; x++) {
              int pixel = source.pixels[p++];
              tpixels[t++] = (pixel << 8) | 0xff;
            }
            t += twidth - source.width;
          }
          break;

        case ARGB:
          for (int y = 0; y < source.height; y++) {
            for (int x = 0; x < source.width; x++) {
              int pixel = source.pixels[p++];
              tpixels[t++] = (pixel << 8) | ((pixel >> 24) & 0xff);
            }
            t += twidth - source.width;
          }
          break;
        }

      } else {  // LITTLE_ENDIAN
        // ARGB native, and RGBA opengl means ABGR on windows
        // for the most part just need to swap two components here
        // the sun.cpu.endian here might be "false", oddly enough..
        // (that's why just using an "else", rather than check for "little")

        switch (source.format) {
        case ALPHA:
          for (int y = 0; y < source.height; y++) {
            for (int x = 0; x < source.width; x++) {
              tpixels[t++] = (source.pixels[p++] << 24) | 0x00FFFFFF;
            }
            t += twidth - source.width;
          }
          break;

        case RGB:
          for (int y = 0; y < source.height; y++) {
            for (int x = 0; x < source.width; x++) {
              int pixel = source.pixels[p++];
              // needs to be ABGR, stored in memory xRGB
              // so R and B must be swapped, and the x just made FF
              tpixels[t++] =
                0xff000000 |  // force opacity for good measure
                ((pixel & 0xFF) << 16) |
                ((pixel & 0xFF0000) >> 16) |
                (pixel & 0x0000FF00);
            }
            t += twidth - source.width;
          }
          break;

        case ARGB:
          for (int y = 0; y < source.height; y++) {
            for (int x = 0; x < source.width; x++) {
              int pixel = source.pixels[p++];
              // needs to be ABGR stored in memory ARGB
              // so R and B must be swapped, A and G just brought back in
              tpixels[t++] =
                ((pixel & 0xFF) << 16) |
                ((pixel & 0xFF0000) >> 16) |
                (pixel & 0xFF00FF00);
            }
            t += twidth - source.width;
          }
          break;
        }
      }
      tbuffer.put(tpixels);
      tbuffer.rewind();

      //

      gl.glBindTexture(GL.GL_TEXTURE_2D, tindex);

      gl.glPixelStorei(GL.GL_UNPACK_ALIGNMENT, 1);
      //gl.glPixelStorei(GL.GL_UNPACK_SWAP_BYTES, 0);

      gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, 4, twidth, theight,
                      //0, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, cash.tpixels);
                      0, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, tbuffer);

      gl.glTexParameterf(GL.GL_TEXTURE_2D,
                         //GL.GL_TEXTURE_MAG_FILTER, GL.GL_NEAREST);
                         GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
      gl.glTexParameterf(GL.GL_TEXTURE_2D,
                         //GL.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST);
                         GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);

      //

      /*int err =*/ glu.gluBuild2DMipmaps(GL.GL_TEXTURE_2D, 4,
                                          twidth, theight,
                                          GL.GL_RGBA,
                                          GL.GL_UNSIGNED_BYTE, tbuffer);
      //System.out.println("mipmap: " + err);

      // The MAG_FILTER should only be GL_LINEAR or GL_NEAREST.
      // Some cards are OK with LINEAR_MIPMAP_LINEAR, but not the
      // Radeon 9700, which is in all the PB G4s.. Not sure if this
      // is an OpenGL version thing, tho it makes sense MIN_FILTER
      // is the only one that uses mipmapping.
      gl.glTexParameterf(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER,
                         GL.GL_LINEAR);
      gl.glTexParameterf(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER,
                         GL.GL_LINEAR_MIPMAP_LINEAR);

      gl.glTexParameterf(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, GL.GL_CLAMP);
      gl.glTexParameterf(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, GL.GL_CLAMP);

      //

      gl.glTexEnvf(GL.GL_TEXTURE_ENV, GL.GL_TEXTURE_ENV_MODE, GL.GL_MODULATE);
    }


    private int nextPowerOfTwo(int val) {
      int ret = 1;
      while (ret < val) {
        ret <<= 1;
      }
      return ret;
    }
  }


  //////////////////////////////////////////////////////////////


  public float textAscent() {
    Font font = textFont.getFont();    
    if ((textMode != SHAPE) || (font == null)) {
      return super.textAscent();
    }
    FontMetrics metrics = parent.getFontMetrics(font);
    return metrics.getAscent();
  }


  public float textDescent() {
    Font font = textFont.getFont();    
    if ((textMode != SHAPE) || (font == null)) {
      return super.textDescent();
    }
    FontMetrics metrics = parent.getFontMetrics(font);
    return metrics.getDescent();
  }


  public void textFont(PFont which) {
    super.textFont(which);

    if (textMode == SHAPE) {
      if (textFont.findFont() == null) {
        showError("Cannot use " + which.name + " as with textMode(SHAPE) " +
                  "because its native equivalent cannot be found.");
      }
    }
  }


  /**
   * Same as parent, but override for native version of the font.
   * <p/>
   * Also gets called by textFont, so the metrics
   * will get recorded properly.
   */
  public void textSize(float size) {
    // can't cancel on textMode(SHAPE) because textMode() must happen
    // after textFont() and textFont() calls textSize()
    //if ((textMode != SHAPE) || (textFontNative == null)) {

    // call this anyway to set the base variables for cases
    // where textMode(SHAPE) will not be used
    super.textSize(size);

    /*
    // derive the font just in case the user is gonna call
    // textMode(SHAPE) afterwards
    if (textFontNative != null) {
      textFontNative = textFontNative.deriveFont(size);
      Graphics2D graphics = (Graphics2D) parent.getGraphics();
      graphics.setFont(textFontNative);

      // get the metrics info
      textFontNativeMetrics = graphics.getFontMetrics(textFontNative);
    }
    */
  }


  protected float textWidthImpl(char buffer[], int start, int stop) {
    Font font = textFont.getFont();
    if ((textMode != SHAPE) || (font == null)) {
      return super.textWidthImpl(buffer, start, stop);
    }

    /*
    // maybe should use one of the newer/fancier functions for this?
    int length = stop - start;
    return textFontNativeMetrics.charsWidth(buffer, start, length);
    */
    Graphics2D graphics = (Graphics2D) parent.getGraphics();
    // otherwise smaller sizes will be totally crapped up
    // seems to need to be before the getFRC, but after the canvas.getGraphics
    // (placing this inside textSize(), even though it was called, wasn't working)
    graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
                              RenderingHints.VALUE_FRACTIONALMETRICS_ON);

    FontRenderContext frc = graphics.getFontRenderContext();
    GlyphVector gv;

    /*
    if (start == 0 && stop == buffer.length) {
        gv = textFontNative.createGlyphVector(frc, buffer);
    } else {
        char[] fellas = PApplet.subset(buffer, start, length);
        gv = textFontNative.createGlyphVector(frc, fellas);
    }
    */
    gv = font.createGlyphVector(frc, buffer);
    float sum = 0;
    for (int i = start; i < stop; i++) {
        GlyphMetrics gm = gv.getGlyphMetrics(i);
        sum += gm.getAdvance();
    }
    return sum;
  }


  /**
   * Override to handle rendering characters with textMode(SHAPE).
   */
  protected void textCharImpl(char ch, float x, float y) {
    if (textMode == SHAPE) {
      if (textFont.getFont() == null) {
        PGraphics.showError("textMode(SHAPE) is disabled because the font " +
                            "\"" + textFont.name + "\" is not available.");
      } else {
        textCharImplShape(ch, x, y);
      }
    } else {
      super.textCharImpl(ch, x, y);
    }
  }


  /**
   * This uses the tesselation functions from GLU to handle triangulation
   * to convert the character into a series of shapes.
   * <p/>
   * <EM>No attempt has been made to optimize this code</EM>
   * <p/>
   * TODO: Should instead override textPlacedImpl() because createGlyphVector
   * takes a char array. Or better yet, cache the font on a per-char basis,
   * so that it's not being re-tessellated each time, could make it into
   * a display list which would be nice and speedy.
   * <p/>
   * Also a problem where some fonts seem to be a bit slight, as if the
   * control points aren't being mapped quite correctly. Probably doing
   * something dumb that the control points don't map to P5's control
   * points. Perhaps it's returning b-spline data from the TrueType font?
   * Though it seems like that would make a lot of garbage rather than
   * just a little flattening.
   * <p/>
   * There also seems to be a bug that is causing a line (but not a filled
   * triangle) back to the origin on some letters (i.e. a capital L when
   * tested with Akzidenz Grotesk Light). But this won't be visible
   * with the stroke shut off, so tabling that bug for now.
   */
  protected void textCharImplShape(char ch, float x, float y) {
    // save the current stroke because it needs to be disabled
    // while the text is being drawn
    boolean strokeSaved = stroke;
    stroke = false;

    // six element array received from the Java2D path iterator
    float textPoints[] = new float[6];

    // array passed to createGylphVector
    char textArray[] = new char[] { ch };

    Graphics2D graphics = (Graphics2D) parent.getGraphics();
    FontRenderContext frc = graphics.getFontRenderContext();
    Font font = textFont.getFont();
    GlyphVector gv = font.createGlyphVector(frc, textArray);
    Shape shp = gv.getOutline();
    //PathIterator iter = shp.getPathIterator(null, 0.05);
    PathIterator iter = shp.getPathIterator(null);

    glu.gluTessBeginPolygon(tobj, null);
    // second param to gluTessVertex is for a user defined object that contains
    // additional info about this point, but that's not needed for anything

    float lastX = 0;
    float lastY = 0;

    // unfortunately the tesselator won't work properly unless a
    // new array of doubles is allocated for each point. that bites ass,
    // but also just reaffirms that in order to make things fast,
    // display lists will be the way to go.
    double vertex[];

    final boolean DEBUG_OPCODES = false; //true;

    while (!iter.isDone()) {
      int type = iter.currentSegment(textPoints);
      switch (type) {
      case PathIterator.SEG_MOVETO:   // 1 point (2 vars) in textPoints
      case PathIterator.SEG_LINETO:   // 1 point
        if (type == PathIterator.SEG_MOVETO) {
          if (DEBUG_OPCODES) {
            System.out.println("moveto\t" +
                               textPoints[0] + "\t" + textPoints[1]);
          }
          glu.gluTessBeginContour(tobj);
        } else {
          if (DEBUG_OPCODES) {
            System.out.println("lineto\t" +
                               textPoints[0] + "\t" + textPoints[1]);
           }
        }
        vertex = new double[] {
          x + textPoints[0], y + textPoints[1], 0
        };
        glu.gluTessVertex(tobj, vertex, 0, vertex);
        lastX = textPoints[0];
        lastY = textPoints[1];
        break;

      case PathIterator.SEG_QUADTO:   // 2 points
        if (DEBUG_OPCODES) {
          System.out.println("quadto\t" +
                             textPoints[0] + "\t" + textPoints[1] + "\t" +
                             textPoints[2] + "\t" + textPoints[3]);
        }

        for (int i = 1; i < bezierDetail; i++) {
          float t = (float)i / (float)bezierDetail;
          vertex = new double[] {
            x + bezierPoint(lastX, textPoints[0],
                            textPoints[2], textPoints[2], t),
            y + bezierPoint(lastY, textPoints[1],
                            textPoints[3], textPoints[3], t), 0
          };
          glu.gluTessVertex(tobj, vertex, 0, vertex);
        }

        lastX = textPoints[2];
        lastY = textPoints[3];
        break;

      case PathIterator.SEG_CUBICTO:  // 3 points
        if (DEBUG_OPCODES) {
          System.out.println("cubicto\t" +
                             textPoints[0] + "\t" + textPoints[1] + "\t" +
                             textPoints[2] + "\t" + textPoints[3] + "\t" +
                             textPoints[4] + "\t" + textPoints[5]);
        }

        for (int i = 1; i < bezierDetail; i++) {
          float t = (float)i / (float)bezierDetail;
          vertex = new double[] {
            x + bezierPoint(lastX, textPoints[0],
                            textPoints[2], textPoints[4], t),
            y + bezierPoint(lastY, textPoints[1],
                            textPoints[3], textPoints[5], t), 0
          };
          glu.gluTessVertex(tobj, vertex, 0, vertex);
        }

        lastX = textPoints[4];
        lastY = textPoints[5];
        break;

      case PathIterator.SEG_CLOSE:
        if (DEBUG_OPCODES) {
          System.out.println("close");
          System.out.println();
        }
        glu.gluTessEndContour(tobj);
        break;
      }
      iter.next();
    }
    glu.gluTessEndPolygon(tobj);

    // re-enable stroke if it was in use before
    stroke = strokeSaved;
  }


  public void textMode(int mode) {
    if (mode == SHAPE) {
      textMode = SHAPE;

    } else {
      // if not SHAPE mode, then pass off to the PGraphics.textMode()
      // which is built for error handling (but objects to SHAPE).
      super.textMode(mode);
    }
  }


  /**
   * There must be a better way to do this, but I'm having a brain fart
   * with all the inner class crap. Fix it later once this stuff is debugged.
   * <p/>
   * The method "void vertex(float $1, float $2, float $3);" contained in
   * the enclosing type "processing.core.PGraphics3" is a perfect match for
   * this method call. However, it is not visible in this nested class because
   * a method with the same name in an intervening class is hiding it.
   */
  /*
  public void vertexRedirect(float x, float y, float z) {
    vertex(x, y, z);
  }
  */


  //public static class TessCallback extends GLUtesselatorCallbackAdapter {
  public class TessCallback extends GLUtessellatorCallbackAdapter {
    //GL gl;
    //GLU glu;

    // grabs the gl and glu variables because it's a static class
    //public TessCallback(GL gl, GLU glu) {
    //this.gl = gl;
    //this.glu = glu;
    //}

    // *** need to shut off the stroke here

    public void begin(int type) {
      // one of GL_TRIANGLE_FAN, GL_TRIANGLE_STRIP,
      // GL_TRIANGLES, or GL_LINE_LOOP
      //gl.glBegin(type);
      switch (type) {
      case GL.GL_TRIANGLE_FAN: beginShape(TRIANGLE_FAN); break;
      case GL.GL_TRIANGLE_STRIP: beginShape(TRIANGLE_STRIP); break;
      case GL.GL_TRIANGLES: beginShape(TRIANGLES); break;
      //case GL.GL_LINE_LOOP: beginShape(LINE_LOOP); break;
      }
      //System.out.println("shape type is " + shape);
    }

    public void end() {
      //gl.glEnd();
      endShape();
    }

//    public void edge(boolean e) {
//      PGraphicsOpenGL.this.edge(e);
//    }

    public void vertex(Object data) {
      if (data instanceof double[]) {
        double[] d = (double[]) data;
        if (d.length != 3) {
          throw new RuntimeException("TessCallback vertex() data " +
                                     "isn't length 3");
        }
        //System.out.println("tess callback vertex " +
        //                 d[0] + " " + d[1] + " " + d[2]);
        //vertexRedirect((float) d[0], (float) d[1], (float) d[2]);
        PGraphicsOpenGL.this.vertex((float) d[0], (float) d[1], (float) d[2]);
        /*
        if (d.length == 6) {
          double[] d2 = {d[0], d[1], d[2]};
          gl.glVertex3dv(d2);
          d2 = new double[]{d[3], d[4], d[5]};
          gl.glColor3dv(d2);
        } else if (d.length == 3) {
          gl.glVertex3dv(d);
        }
        */
      } else {
        throw new RuntimeException("TessCallback vertex() data not understood");
      }
    }

    public void error(int errnum) {
      String estring = glu.gluErrorString(errnum);
      //System.out.println("Tessellation Error: " + estring);
      //throw new RuntimeException();
      throw new RuntimeException("Tessellation Error: " + estring);
    }

    /**
     * Implementation of the GLU_TESS_COMBINE callback.
     * @param coords is the 3-vector of the new vertex
     * @param data is the vertex data to be combined, up to four elements.
     * This is useful when mixing colors together or any other
     * user data that was passed in to gluTessVertex.
     * @param weight is an array of weights, one for each element of "data"
     * that should be linearly combined for new values.
     * @param outData is the set of new values of "data" after being
     * put back together based on the weights. it's passed back as a
     * single element Object[] array because that's the closest
     * that Java gets to a pointer.
     */
    public void combine(double[] coords, Object[] data,
                        float[] weight, Object[] outData) {
      //System.out.println("coords.length = " + coords.length);
      //System.out.println("data.length = " + data.length);
      //System.out.println("weight.length = " + weight.length);
      //for (int i = 0; i < data.length; i++) {
      //System.out.println(i + " " + data[i].getClass().getName() + " " + weight[i]);
      //}

      double[] vertex = new double[coords.length];
      vertex[0] = coords[0];
      vertex[1] = coords[1];
      vertex[2] = coords[2];
      //System.out.println("combine " +
      //                 vertex[0] + " " + vertex[1] + " " + vertex[2]);

      // this is just 3, so nothing interesting to bother combining
      //System.out.println("data length " + ((double[]) data[0]).length);

      // not gonna bother doing any combining,
      // since no user data is being passed in.
      /*
      for (int i = 3; i < 6; i++) {
        vertex[i] =
          weight[0] * ((double[]) data[0])[i] +
          weight[1] * ((double[]) data[1])[i] +
          weight[2] * ((double[]) data[2])[i] +
          weight[3] * ((double[]) data[3])[i];
      }
      */
      outData[0] = vertex;
    }
  }


  //////////////////////////////////////////////////////////////


  /*
  boolean ellipseInited;
  int ellipseFillList;
  int ellipseStrokeList;

  protected void ellipseImpl(float x1, float y1, float w, float h) {
    float hradius = w / 2f;
    float vradius = h / 2f;

    float centerX = x1 + hradius;
    float centerY = y1 + vradius;

    // adapt accuracy to radii used w/ a minimum of 4 segments [toxi]
    // now uses current scale factors to determine "real" transformed radius

    //int cAccuracy = (int)(4+Math.sqrt(hradius*abs(m00)+vradius*abs(m11))*2);
    //int cAccuracy = (int)(4+Math.sqrt(hradius+vradius)*2);

    // notched this up to *3 instead of *2 because things were
    // looking a little rough, i.e. the calculate->arctangent example [fry]

    // also removed the m00 and m11 because those were causing weirdness
    // need an actual measure of magnitude in there [fry]

    int accuracy = (int)(4+Math.sqrt(hradius+vradius)*3);
    //System.out.println("accuracy is " + accuracy);
    //accuracy = 5;

    // [toxi031031] adapted to use new lookup tables
    float inc = (float)SINCOS_LENGTH / accuracy;

    float val = 0;

    if (fill) {
      boolean savedStroke = stroke;
      stroke = false;

      beginShape(TRIANGLE_FAN);
      normal(0, 0, 1);
      vertex(centerX, centerY);
      for (int i = 0; i < accuracy; i++) {
        vertex(centerX + cosLUT[(int) val] * hradius,
               centerY + sinLUT[(int) val] * vradius);
        val += inc;
      }
      // back to the beginning
      vertex(centerX + cosLUT[0] * hradius,
             centerY + sinLUT[0] * vradius);
      endShape();

      stroke = savedStroke;
    }

    if (stroke) {
      boolean savedFill = fill;
      fill = false;

      val = 0;
      beginShape(); //LINE_LOOP);
      for (int i = 0; i < accuracy; i++) {
        vertex(centerX + cosLUT[(int) val] * hradius,
               centerY + sinLUT[(int) val] * vradius);
        val += inc;
      }
      endShape(CLOSE);

      fill = savedFill;
    }
  }
  */

  /*
    pgl.beginGL();
    //PGraphics gr = PApplet.this.g;
    //GL gl = ((PGraphicsOpenGL).gr).beginGL();
    if (!ellipseInited) {
      ellipseList = gl.glGenLists(1);
      gl.glNewList(ellipseList, GL.GL_COMPILE);
      gl.glBegin(GL.GL_LINE_LOOP);
      int seg = 15;
      float segf = 15;
      for (int i = 0; i < seg; i++) {
        float theta = TWO_PI * (float)i / segf;
        gl.glVertex2f(cos(theta), sin(theta));
      }
      gl.glEnd();
      gl.glEndList();
      ellipseInited = true;
    }

    for (int i=1; i<numSegments-1; i++) {
    gl.glPushMatrix();
    gl.glTranslatef(x[i], y[i], z);
    float r = w[i]/2f;
    gl.glScalef(r, r, r);
    gl.glColor4f(1, 1, 1, 150.0/255.0);
    gl.glCallList(ellipseList);
    gl.glScalef(0.5, 0.5, 0.5);
    gl.glColor4f(1, 1, 1, 50.0/255.0);
    gl.glCallList(ellipseList);
    gl.glPopMatrix();
    }
    pgl.endGL();
  */


  //////////////////////////////////////////////////////////////


  // We're not actually turning on GL lights right now
  // because our home-grown ones work better for now.
  public void lights() {
    super.lights();
    //gl.glEnable(GL.GL_LIGHTING);
  }

  //public void noLights() {
  //super.noLights();
  //gl.glDisable(GL.GL_LIGHTING);
  //}


  public void ambientLight(float r, float g, float b) {
    super.ambientLight(r, g, b);
    glLightEnable(lightCount - 1);
    glLightAmbient(lightCount - 1);
    glLightPosition(lightCount - 1);
    glLightFalloff(lightCount - 1);
    //return num;
  }

  public void ambientLight(float r, float g, float b,
                           float x, float y, float z) {
    super.ambientLight(r, g, b, x, y, z);
    glLightEnable(lightCount - 1);
    glLightAmbient(lightCount - 1);
    glLightPosition(lightCount - 1);
    glLightFalloff(lightCount - 1);
    //return num;
  }


  public void directionalLight(float r, float g, float b,
                               float nx, float ny, float nz) {
    super.directionalLight(r, g, b, nx, ny, nz);
    //int num = super.internalCreateDirectionalLight(lr, lg, lb, nx, ny, nz);
    glLightEnable(lightCount - 1);
    glLightNoAmbient(lightCount - 1);
    glLightDirection(lightCount - 1);
    glLightDiffuse(lightCount - 1);
    glLightSpecular(lightCount - 1);
    glLightFalloff(lightCount - 1);
    //return num;
  }


  public void pointLight(float r, float g, float b,
                         float x, float y, float z) {
    super.pointLight(r, g, b, x, y, z);
    //int num = super.internalCreatePointLight(lr, lg, lb, x, y, z);
    glLightEnable(lightCount - 1);
    glLightNoAmbient(lightCount - 1);
    glLightPosition(lightCount - 1);
    glLightDiffuse(lightCount - 1);
    glLightSpecular(lightCount - 1);
    glLightFalloff(lightCount - 1);
    //return num;
  }


  public void spotLight(float r, float g, float b,
                        float x, float y, float z,
                        float nx, float ny, float nz,
                        float angle, float concentration) {
    super.spotLight(r, g, b, x, y, z, nx, ny, nz, angle, concentration);
    //int num = super.internalCreateSpotLight(lr, lg, lb, x, y, z, nx, ny, nz, angle);
    glLightNoAmbient(lightCount - 1);
    glLightPosition(lightCount - 1);
    glLightDirection(lightCount - 1);
    glLightDiffuse(lightCount - 1);
    glLightSpecular(lightCount - 1);
    glLightFalloff(lightCount - 1);
    glLightSpotAngle(lightCount - 1);
    glLightSpotConcentration(lightCount - 1);
    //return num;
  }


  //public void lightDisable(int num) {
  //super.lightDisable(num);
  //gl.glDisable(GL.GL_LIGHT0 + num);
  //}


  public void lightFalloff(float constant, float linear, float quadratic) {
    super.lightFalloff(constant, linear, quadratic);
    glLightFalloff(lightCount);
  }


  public void lightSpecular(float x, float y, float z) {
    super.lightSpecular(x, y, z);
    glLightSpecular(lightCount);
  }


  //////////////////////////////////////////////////////////////

  // internal helper functions to update position and direction
  // (eventually remove the 'num' param here)


  protected void lightPosition(int num, float x, float y, float z) {
    super.lightPosition(num, x, y, z);
    glLightPosition(num);
  }


  protected void lightDirection(int num, float x, float y, float z) {
    super.lightDirection(num, x, y, z);
    glLightDirection(num);
  }


  //////////////////////////////////////////////////////////////

  // internal functions to update gl state for lighting variables
  // (eventually remove the 'num' param here)


  protected void glLightAmbient(int num) {
//    lightBuffer.put(lightDiffuse[num]);
//    lightBuffer.rewind();
//    gl.glLightfv(GL.GL_LIGHT0 + num,
//                 GL.GL_AMBIENT, lightBuffer);
    gl.glLightfv(GL.GL_LIGHT0 + num,
                 GL.GL_AMBIENT, lightDiffuse[num], 0);
  }


  protected void glLightNoAmbient(int num) {
    if (zeroBuffer == null) {
      // hopefully buffers are filled with zeroes..
      zeroBuffer = BufferUtil.newFloatBuffer(3);
    }
    gl.glLightfv(GL.GL_LIGHT0 + num,
                 GL.GL_AMBIENT, zeroBuffer);
  }


  protected void glLightDiffuse(int num) {
//    lightBuffer.put(lightDiffuse[num]);
//    lightBuffer.rewind();
//    gl.glLightfv(GL.GL_LIGHT0 + num,
//                 GL.GL_DIFFUSE, lightBuffer);
    gl.glLightfv(GL.GL_LIGHT0 + num,
                 GL.GL_DIFFUSE, lightDiffuse[num], 0);
  }


  protected void glLightDirection(int num) {
//    lightBuffer.put(lightNormal[num]);
//    lightBuffer.rewind();

    if (lightType[num] == DIRECTIONAL) {
      // TODO this expects a fourth arg that will be set to 1
      //      this is why lightBuffer is length 4,
      //      and the [3] element set to 1 in the constructor.
      //      however this may be a source of problems since
      //      it seems a bit "hack"
      gl.glLightfv(GL.GL_LIGHT0 + num, GL.GL_POSITION, 
                   lightNormal[num].array(), 0);
    } else {  // spotlight
      // this one only needs the 3 arg version
      gl.glLightfv(GL.GL_LIGHT0 + num, GL.GL_SPOT_DIRECTION, 
                   lightNormal[num].array(), 0);
    }
  }


  protected void glLightEnable(int num) {
    gl.glEnable(GL.GL_LIGHT0 + num);
  }


  protected void glLightFalloff(int num) {
    gl.glLightf(GL.GL_LIGHT0 + num,
                GL.GL_CONSTANT_ATTENUATION, lightFalloffConstant[num]);
    gl.glLightf(GL.GL_LIGHT0 + num,
                GL.GL_LINEAR_ATTENUATION, lightFalloffLinear[num]);
    gl.glLightf(GL.GL_LIGHT0 + num,
                GL.GL_QUADRATIC_ATTENUATION, lightFalloffQuadratic[num]);
  }


  protected void glLightPosition(int num) {
//    lightBuffer.put(lightPosition[num]);
//    lightBuffer.rewind();
//    gl.glLightfv(GL.GL_LIGHT0 + num, GL.GL_POSITION, lightBuffer);
    gl.glLightfv(GL.GL_LIGHT0 + num, GL.GL_POSITION, lightPosition[num].array(), 0);
  }


  protected void glLightSpecular(int num) {
//    lightBuffer.put(lightSpecular[num]);
//    lightBuffer.rewind();
//    gl.glLightfv(GL.GL_LIGHT0 + num, GL.GL_SPECULAR, lightBuffer);
    gl.glLightfv(GL.GL_LIGHT0 + num, GL.GL_SPECULAR, lightSpecular[num], 0);
  }


  public void glLightSpotAngle(int num) {
    gl.glLightf(GL.GL_LIGHT0 + num,
                GL.GL_SPOT_CUTOFF, lightSpotAngle[num]);
  }


  public void glLightSpotConcentration(int num) {
    gl.glLightf(GL.GL_LIGHT0 + num,
                GL.GL_SPOT_EXPONENT, lightSpotConcentration[num]);
  }


  //////////////////////////////////////////////////////////////


  public void strokeJoin(int join) {
    String msg = "strokeJoin() not available with OPENGL";
    throw new RuntimeException(msg);
  }


  public void strokeCap(int cap) {
    String msg = "strokeCap() not available with OPENGL";
    throw new RuntimeException(msg);
  }


  //////////////////////////////////////////////////////////////


  /**
   * Load the calculated color into a pre-allocated array so that
   * it can be quickly passed over to OpenGL. (fix from Willis Morse)
   */
  private final void calcColorBuffer() {
    if (colorBuffer == null) {
      colorBuffer = BufferUtil.newFloatBuffer(4);
    }
    colorBuffer.put(0, calcR);
    colorBuffer.put(1, calcG);
    colorBuffer.put(2, calcB);
    colorBuffer.put(3, calcA);
    colorBuffer.rewind();
  }


  //////////////////////////////////////////////////////////////


  protected void fillFromCalc() {
    super.fillFromCalc();
    calcColorBuffer();
//    String nom = Thread.currentThread().getName();
//    System.out.println("fill in " + nom);
//    if (!nom.equals("Animation Thread")) {
//      new Exception().printStackTrace();
//    }
    gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_AMBIENT_AND_DIFFUSE,
                    colorBuffer);
//    System.out.println("fill out");
  }


  //////////////////////////////////////////////////////////////


  public void ambient(int rgb) {
    super.ambient(rgb);
    calcColorBuffer();
    gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_AMBIENT, colorBuffer);
  }


  public void ambient(float gray) {
    super.ambient(gray);
    calcColorBuffer();
    gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_AMBIENT, colorBuffer);
  }


  public void ambient(float x, float y, float z) {
    super.ambient(x, y, z);
    calcColorBuffer();
    gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_AMBIENT, colorBuffer);
  }


  //////////////////////////////////////////////////////////////


  public void specular(int rgb) {
    super.specular(rgb);
    calcColorBuffer();
    gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_SPECULAR, colorBuffer);
  }

  public void specular(float gray) {
    super.specular(gray);
    calcColorBuffer();
    gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_SPECULAR, colorBuffer);
  }


//  public void specular(float gray, float alpha) {
//    super.specular(gray, alpha);
//    calcColorBuffer();
//    gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_SPECULAR, colorBuffer);
//  }


  public void specular(float x, float y, float z) {
    super.specular(x, y, z);
    calcColorBuffer();
    gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_SPECULAR, colorBuffer);
  }


//  public void specular(float x, float y, float z, float a) {
//    super.specular(x, y, z, a);
//    calcColorBuffer();
//    gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_SPECULAR, colorBuffer);
//  }


  //////////////////////////////////////////////////////////////


  public void emissive(int rgb) {
    super.emissive(rgb);
    calcColorBuffer();
    gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_EMISSION, colorBuffer);
  }


  public void emissive(float gray) {
    super.emissive(gray);
    calcColorBuffer();
    gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_EMISSION, colorBuffer);
  }


  public void emissive(float x, float y, float z) {
    super.emissive(x, y, z);
    calcColorBuffer();
    gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_EMISSION, colorBuffer);
  }


  //////////////////////////////////////////////////////////////


  public void shininess(float shine) {
    super.shininess(shine);
    gl.glMaterialf(GL.GL_FRONT_AND_BACK, GL.GL_SHININESS, shine);
  }


  //////////////////////////////////////////////////////////////


  public void background(PImage bgimage) {
    backgroundImpl();
    set(0, 0, bgimage);
  }


  public void backgroundImpl() {
    gl.glClearColor(backgroundR, backgroundG, backgroundB, 1);
    gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

    clearRaw();
  }


  //////////////////////////////////////////////////////////////


  public void smooth() {
    gl.glEnable(GL.GL_MULTISAMPLE);
    gl.glEnable(GL.GL_POINT_SMOOTH);
    gl.glEnable(GL.GL_LINE_SMOOTH);
    gl.glEnable(GL.GL_POLYGON_SMOOTH);
    smooth = true;
  }


  public void noSmooth() {
    gl.glDisable(GL.GL_MULTISAMPLE);
    gl.glDisable(GL.GL_POINT_SMOOTH);
    gl.glDisable(GL.GL_LINE_SMOOTH);
    gl.glDisable(GL.GL_POLYGON_SMOOTH);
    smooth = false;
  }


  //////////////////////////////////////////////////////////////


  public void loadPixels() {
    if ((pixels == null) || (pixels.length != width*height)) {
      pixels = new int[width * height];
      pixelBuffer = BufferUtil.newIntBuffer(pixels.length);
    }

    /*
    for (int y = 0; y < height; y++) {
      // or SKIP_PIXELS with y*width
      //gl.glPixelStorei(GL.GL_PACK_SKIP_ROWS, (height-1) - y);
      gl.glReadPixels(0, y, width, y + 1,
                      GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, pixels);
    }
    gl.glPixelStorei(GL.GL_PACK_SKIP_ROWS, 0);
    */

    gl.glReadPixels(0, 0, width, height,
                    GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, pixelBuffer);
    pixelBuffer.get(pixels);
    pixelBuffer.rewind();

    //for (int i = 0; i < 5; i++) {
    //System.out.println(PApplet.hex(pixels[i]));
    //}

    /*
    int temp[] = new int[width];
    // 3 rows, skips the middle

    for (int y = 0; y < height/2; y++) {
      int yy = (height - 1) - y;
      System.arraycopy(pixels, y*width, temp, 0, width);
      System.arraycopy(pixels, yy*width, pixels, y*width, width);
      System.arraycopy(temp, 0, pixels, yy*width, width);
    }
    */

    /*
    // now need to swap the RGBA components to ARGB (big endian)
    for (int i = 0; i < pixels.length; i++) {
      //pixels[i] = ((pixels[i] & 0xff) << 24) |
      pixels[i] = ((pixels[i] << 24) & 0xff) |  // safer?
        ((pixels[i] >> 8) & 0xffffff);
    }
    */

    // flip vertically (opengl stores images upside down),
    // and swap RGBA components to ARGB (big endian)
    int index = 0;
    int yindex = (height - 1) * width;
    for (int y = 0; y < height/2; y++) {
      if (BIG_ENDIAN) {
        for (int x = 0; x < width; x++) {
          int temp = pixels[index];
          // ignores alpha component, just sets it opaque
          pixels[index] = 0xff000000 | ((pixels[yindex] >> 8)  & 0x00ffffff);
          pixels[yindex] = 0xff000000 | ((temp >> 8)  & 0x00ffffff);

          index++;
          yindex++;
        }
      } else {  // LITTLE_ENDIAN, convert ABGR to ARGB
        for (int x = 0; x < width; x++) {
          int temp = pixels[index];

          // identical to endPixels because only two
          // components are being swapped
          pixels[index] = 0xff000000 |
            ((pixels[yindex] << 16) & 0xff0000) |
            (pixels[yindex] & 0xff00) |
            ((pixels[yindex] >> 16) & 0xff);

          pixels[yindex] = 0xff000000 |
            ((temp << 16) & 0xff0000) |
            (temp & 0xff00) |
            ((temp >> 16) & 0xff);

          index++;
          yindex++;
        }
      }
      yindex -= width*2;
    }
  }


  /**
   * Convert native OpenGL format into palatable ARGB format.
   * This function leaves alone (ignores) the alpha component.
   * Also flips the image vertically, since images are upside-down in GL.
   */
  static void nativeToJavaRGB(PImage image) {
    int index = 0;
    int yindex = (image.height - 1) * image.width;
    for (int y = 0; y < image.height/2; y++) {
      if (BIG_ENDIAN) {
        for (int x = 0; x < image.width; x++) {
          int temp = image.pixels[index];
          // ignores alpha component, just sets it opaque
          image.pixels[index] =
            0xff000000 | ((image.pixels[yindex] >> 8)  & 0x00ffffff);
          image.pixels[yindex] =
            0xff000000 | ((temp >> 8)  & 0x00ffffff);
          index++;
          yindex++;
        }
      } else {  // LITTLE_ENDIAN, convert ABGR to ARGB
        for (int x = 0; x < image.width; x++) {
          int temp = image.pixels[index];

          // identical to endPixels because only two
          // components are being swapped
          image.pixels[index] = 0xff000000 |
            ((image.pixels[yindex] << 16) & 0xff0000) |
            (image.pixels[yindex] & 0xff00) |
            ((image.pixels[yindex] >> 16) & 0xff);

          image.pixels[yindex] = 0xff000000 |
            ((temp << 16) & 0xff0000) |
            (temp & 0xff00) |
            ((temp >> 16) & 0xff);

          index++;
          yindex++;
        }
      }
      yindex -= image.width*2;
    }
  }


  /**
   * Convert native OpenGL format into palatable ARGB format.
   * This function leaves alone (ignores) the alpha component.
   * Also flips the image vertically, since images are upside-down in GL.
   */
  static void nativeToJavaARGB(PImage image) {
    int index = 0;
    int yindex = (image.height - 1) * image.width;
    for (int y = 0; y < image.height/2; y++) {
      if (BIG_ENDIAN) {
        for (int x = 0; x < image.width; x++) {
          int temp = image.pixels[index];
          // ignores alpha component, just sets it opaque
          image.pixels[index] =
            (image.pixels[yindex] & 0xff000000) |
            ((image.pixels[yindex] >> 8)  & 0x00ffffff);
          image.pixels[yindex] =
            (temp & 0xff000000) |
            ((temp >> 8)  & 0x00ffffff);
          index++;
          yindex++;
        }
      } else {  // LITTLE_ENDIAN, convert ABGR to ARGB
        for (int x = 0; x < image.width; x++) {
          int temp = image.pixels[index];

          // identical to endPixels because only two
          // components are being swapped
          image.pixels[index] =
            (image.pixels[yindex] & 0xff000000) |
            ((image.pixels[yindex] << 16) & 0xff0000) |
            (image.pixels[yindex] & 0xff00) |
            ((image.pixels[yindex] >> 16) & 0xff);

          image.pixels[yindex] =
            (temp & 0xff000000) |
            ((temp << 16) & 0xff0000) |
            (temp & 0xff00) |
            ((temp >> 16) & 0xff);

          index++;
          yindex++;
        }
      }
      yindex -= image.width*2;
    }
  }


  /**
   * Convert ARGB (Java/Processing) data to native OpenGL format.
   * This function leaves alone (ignores) the alpha component.
   * Also flips the image vertically, since images are upside-down in GL.
   */
  static void javaToNativeRGB(PImage image) {
    int width = image.width;
    int height = image.height;
    int pixels[] = image.pixels;

    int index = 0;
    int yindex = (height - 1) * width;
    for (int y = 0; y < height/2; y++) {
      if (BIG_ENDIAN) {
        // and convert ARGB back to opengl RGBA components (big endian)
        for (int x = 0; x < image.width; x++) {
          int temp = pixels[index];
          /*
            pixels[index] =
            ((pixels[yindex] >> 24) & 0xff) |
            ((pixels[yindex] << 8) & 0xffffff00);
          pixels[yindex] =
            ((temp >> 24) & 0xff) |
            ((temp << 8) & 0xffffff00);
          */
          pixels[index] = ((pixels[yindex] << 8) & 0xffffff00) | 0xff;
          pixels[yindex] = ((temp << 8) & 0xffffff00) | 0xff;

          index++;
          yindex++;
        }

      } else {
        // convert ARGB back to native little endian ABGR
        for (int x = 0; x < width; x++) {
          int temp = pixels[index];

          pixels[index] = 0xff000000 |
            ((pixels[yindex] << 16) & 0xff0000) |
            (pixels[yindex] & 0xff00) |
            ((pixels[yindex] >> 16) & 0xff);

          pixels[yindex] = 0xff000000 |
            ((temp << 16) & 0xff0000) |
            (temp & 0xff00) |
            ((temp >> 16) & 0xff);

          index++;
          yindex++;
        }
      }
      yindex -= width*2;
    }
  }


  /**
   * Convert Java ARGB to native OpenGL format.
   * Also flips the image vertically, since images are upside-down in GL.
   */
  static void javaToNativeARGB(PImage image) {
    int width = image.width;
    int height = image.height;
    int pixels[] = image.pixels;

    int index = 0;
    int yindex = (height - 1) * width;
    for (int y = 0; y < height/2; y++) {
      if (BIG_ENDIAN) {
        // and convert ARGB back to opengl RGBA components (big endian)
        for (int x = 0; x < image.width; x++) {
          int temp = pixels[index];
          pixels[index] =
            ((pixels[yindex] >> 24) & 0xff) |
            ((pixels[yindex] << 8) & 0xffffff00);
          pixels[yindex] =
            ((temp >> 24) & 0xff) |
            ((temp << 8) & 0xffffff00);

          index++;
          yindex++;
        }

      } else {
        // convert ARGB back to native little endian ABGR
        for (int x = 0; x < width; x++) {
          int temp = pixels[index];

          pixels[index] =
            (pixels[yindex] & 0xff000000) |
            ((pixels[yindex] << 16) & 0xff0000) |
            (pixels[yindex] & 0xff00) |
            ((pixels[yindex] >> 16) & 0xff);

          pixels[yindex] =
            (pixels[yindex] & 0xff000000) |
            ((temp << 16) & 0xff0000) |
            (temp & 0xff00) |
            ((temp >> 16) & 0xff);

          index++;
          yindex++;
        }
      }
      yindex -= width*2;
    }
  }


  public void updatePixels() {
    // flip vertically (opengl stores images upside down),

    int index = 0;
    int yindex = (height - 1) * width;
    for (int y = 0; y < height/2; y++) {
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

          pixels[index] = 0xff000000 |
            ((pixels[yindex] << 16) & 0xff0000) |
            (pixels[yindex] & 0xff00) |
            ((pixels[yindex] >> 16) & 0xff);

          pixels[yindex] = 0xff000000 |
            ((temp << 16) & 0xff0000) |
            (temp & 0xff00) |
            ((temp >> 16) & 0xff);

          index++;
          yindex++;
        }
      }
      yindex -= width*2;
    }

    // re-pack ARGB data into RGBA for opengl (big endian)
    //for (int i = 0; i < pixels.length; i++) {
      //pixels[i] = ((pixels[i] >> 24) & 0xff) |
        //((pixels[i] << 8) & 0xffffff00);
    //}

    setRasterPos(0, 0);  // lower-left corner

    pixelBuffer.put(pixels);
    pixelBuffer.rewind();
    gl.glDrawPixels(width, height,
                    GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, pixelBuffer);
  }



  //////////////////////////////////////////////////////////////


  IntBuffer getsetBuffer = BufferUtil.newIntBuffer(1);
  //int getset[] = new int[1];

  public int get(int x, int y) {
    gl.glReadPixels(x, y, 1, 1, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, getsetBuffer);
    int getset = getsetBuffer.get(0);

    if (BIG_ENDIAN) {
      return 0xff000000 | ((getset >> 8)  & 0x00ffffff);

    } else {
      return 0xff000000 |
            ((getset << 16) & 0xff0000) |
            (getset & 0xff00) |
            ((getset >> 16) & 0xff);
    }
  }


  public PImage get(int x, int y, int w, int h) {
    if (imageMode == CORNERS) {  // if CORNER, do nothing
      w = (w - x);
      h = (h - x);
    } else if (imageMode == CENTER) {
      x -= w/2;
      y -= h/2;
    }

    if (x < 0) {
      w += x; // clip off the left edge
      x = 0;
    }
    if (y < 0) {
      h += y; // clip off some of the height
      y = 0;
    }

    if (x + w > width) w = width - x;
    if (y + h > height) h = height - y;

    PImage newbie = new PImage(w, h); //new int[w*h], w, h, ARGB);

    IntBuffer newbieBuffer = BufferUtil.newIntBuffer(w*h);
    gl.glReadPixels(x, y, w, h, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, newbieBuffer);
    newbieBuffer.get(newbie.pixels);

    nativeToJavaARGB(newbie);
    return newbie;
  }


  public PImage get() {
    return get(0, 0, width, height);
  }


  public void set(int x, int y, int argb) {
    int getset = 0;

    if (BIG_ENDIAN) {
      // convert ARGB to RGBA
      getset = (argb << 8) | 0xff;

    } else {
      // convert ARGB to ABGR
      getset =
        (argb & 0xff00ff00) |
        ((argb << 16) & 0xff0000) |
        ((argb >> 16) & 0xff);
    }
    getsetBuffer.put(0, getset);
    getsetBuffer.rewind();
    //gl.glRasterPos2f(x + EPSILON, y + EPSILON);
    setRasterPos(x, (height-y) - 1);
    gl.glDrawPixels(1, 1, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, getsetBuffer);
  }


  /**
   * Set an image directly to the screen.
   * <P>
   * TODO not optimized properly, creates multiple temporary buffers
   * the size of the image. Needs to instead use image cache, but that
   * requires two types of image cache. One for power of 2 textures
   * and another for glReadPixels/glDrawPixels data that's flipped
   * vertically. Both have their components all swapped to native.
   */
  public void set(int x, int y, PImage source) {
    if (imageMode == CENTER) {
      x -= source.width / 2;
      y -= source.height / 2;
    }
    int backup[] = new int[source.pixels.length];
    System.arraycopy(source.pixels, 0, backup, 0, source.pixels.length);
    javaToNativeARGB(source);

    // TODO is this possible without intbuffer?
    IntBuffer setBuffer = BufferUtil.newIntBuffer(source.pixels.length);
    setBuffer.put(source.pixels);
    setBuffer.rewind();

    setRasterPos(x, (height-y) - source.height); //+source.height);
    gl.glDrawPixels(source.width, source.height,
                    GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, setBuffer);
    source.pixels = backup;
  }


  /**
   * Definitive method for setting raster pos, including offscreen locations.
   * The raster position is tricky because it's affected by the modelview and
   * projection matrices. Further, offscreen coords won't properly set the
   * raster position. This code gets around both issues.
   * http://www.mesa3d.org/brianp/sig97/gotchas.htm
   * @param y the Y-coordinate, which is flipped upside down in OpenGL
   */
  protected void setRasterPos(float x, float y) {
    float z = 0;
    float w = 1;

    float fx, fy;

    // Push current matrix mode and viewport attributes
    gl.glPushAttrib(GL.GL_TRANSFORM_BIT | GL.GL_VIEWPORT_BIT);

    // Setup projection parameters
    gl.glMatrixMode(GL.GL_PROJECTION);
    gl.glPushMatrix();
    gl.glLoadIdentity();
    gl.glMatrixMode(GL.GL_MODELVIEW);
    gl.glPushMatrix();
    gl.glLoadIdentity();

    gl.glDepthRange(z, z);
    gl.glViewport((int) x - 1, (int) y - 1, 2, 2);

    // set the raster (window) position
    fx = x - (int) x;
    fy = y - (int) y;
    gl.glRasterPos4f(fx, fy, 0, w);

    // restore matrices, viewport and matrix mode
    gl.glPopMatrix();
    gl.glMatrixMode(GL.GL_PROJECTION);
    gl.glPopMatrix();

    gl.glPopAttrib();
  }


  //////////////////////////////////////////////////////////////


  /**
   * This is really inefficient and not a good idea in OpenGL.
   * Use get() and set() with a smaller image area, or call the
   * filter on an image instead, and then draw that.
   */
  public void filter(int kind) {
    PImage temp = get();
    temp.filter(kind);
    set(0, 0, temp);
  }


  /**
   * This is really inefficient and not a good idea in OpenGL.
   * Use get() and set() with a smaller image area, or call the
   * filter on an image instead, and then draw that.
   */
  public void filter(int kind, float param) {
    PImage temp = get();
    temp.filter(kind, param);
    set(0, 0, temp);
  }


  //////////////////////////////////////////////////////////////


  /**
   * Extremely slow and not optimized, should use glCopyPixels instead.
   * Currently calls a beginPixels() on the whole canvas, then does the copy,
   * then it calls endPixels().
   */
  public void copy(int sx1, int sy1, int sx2, int sy2,
                   int dx1, int dy1, int dx2, int dy2) {
    //throw new RuntimeException("copy() not available with OpenGL");
    loadPixels();
    super.copy(sx1, sy1, sx2, sy2, dx1, dy1, dx2, dy2);
    updatePixels();
  }


  /**
   * TODO - extremely slow and not optimized.
   * Currently calls a beginPixels() on the whole canvas,
   * then does the copy, then it calls endPixels().
   */
  public void copy(PImage src,
                   int sx1, int sy1, int sx2, int sy2,
                   int dx1, int dy1, int dx2, int dy2) {
    loadPixels();
    super.copy(src, sx1, sy1, sx2, sy2, dx1, dy1, dx2, dy2);
    updatePixels();
  }


  //////////////////////////////////////////////////////////////


  public void blend(int sx, int sy, int dx, int dy, int mode) {
    set(dx, dy, PImage.blendColor(get(sx, sy), get(dx, dy), mode));
  }


  public void blend(PImage src,
                    int sx, int sy, int dx, int dy, int mode) {
    set(dx, dy, PImage.blendColor(src.get(sx, sy), get(dx, dy), mode));
  }


  /**
   * TODO - extremely slow and not optimized.
   * Currently calls a beginPixels() on the whole canvas,
   * then does the blend, then it calls endPixels().
   */
  public void blend(int sx1, int sy1, int sx2, int sy2,
                    int dx1, int dy1, int dx2, int dy2, int mode) {
    loadPixels();
    super.blend(sx1, sy1, sx2, sy2, dx1, dy1, dx2, dy2, mode);
    updatePixels();
  }


  /**
   * TODO - extremely slow and not optimized.
   * Currently calls a beginPixels() on the whole canvas,
   * then does the blend, then it calls endPixels().
   */
  public void blend(PImage src,
                    int sx1, int sy1, int sx2, int sy2,
                    int dx1, int dy1, int dx2, int dy2, int mode) {
    loadPixels();
    super.blend(src, sx1, sy1, sx2, sy2, dx1, dy1, dx2, dy2, mode);
    updatePixels();
  }


  //////////////////////////////////////////////////////////////


  public void save(String filename) {
    loadPixels();
    super.save(filename);
  }


  //////////////////////////////////////////////////////////////


  /**
   * Report on anything from glError().
   * Don't use this inside glBegin/glEnd otherwise it'll
   * throw an GL_INVALID_OPERATION error.
   */
  public void report(String where) {
    if (!hints[DISABLE_OPENGL_ERROR_REPORT]) {
      int err = gl.glGetError();
      if (err != 0) {
        String errString = glu.gluErrorString(err);
        System.err.println("OpenGL error " + err +
                           " at " + where +
                           ": " + errString);
      /*
        System.out.print("GL_ERROR at " + where + ": ");
        System.out.print(PApplet.hex(err, 4) + "  ");
        switch (err) {
        case 0x0500: System.out.print("GL_INVALID_ENUM"); break;
        case 0x0501: System.out.print("GL_INVALID_VALUE"); break;
        case 0x0502: System.out.print("GL_INVALID_OPERATION"); break;
        case 0x0503: System.out.print("GL_STACK_OVERFLOW"); break;
        case 0x0504: System.out.print("GL_STACK_UNDERFLOW"); break;
        case 0x0505: System.out.print("GL_OUT_OF_MEMORY"); break;
        default: System.out.print("UNKNOWN");
        }
        System.out.println();
      }
      */
      }
    }
  }


  //////////////////////////////////////////////////////////////


  //protected final float min(float a, float b) {
  //  return (a < b) ? a : b;
  //}
  private final float clamp(float a) {
    return (a < 1) ? a : 1;
  }
}
