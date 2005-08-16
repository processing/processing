/* -*- mode: jde; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  PGraphicsGL - opengl version of the graphics engine
  Part of the Processing project - http://processing.org

  Copyright (c) 2004-05 Ben Fry and Casey Reas

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
import java.lang.reflect.*;
import net.java.games.jogl.*;


/**
 * Implementation of the PGraphics API that employs OpenGL rendering via JOGL.
 * <p/>
 * JOGL requires Java 1.4 or higher, so there are no restrictions on this
 * code to be compatible with Java 1.1 or Java 1.3.
 * <p/>
 * This code relies on PGraphics3 for all lighting and transformations.
 * Meaning that translate(), rotate(), and any lighting will be done in
 * PGraphics3, and OpenGL is only used to blit lines and triangles as fast
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
 */
public class PGraphicsGL extends PGraphics3 {
  public GL gl;
  public GLU glu;
  public GLCanvas canvas;

  public Illustrator ai;

  protected float[] projectionFloats;

  GLUtesselator tobj;
  TessCallback tessCallback;

  /**
   * true if the host system is big endian (PowerPC, MIPS, SPARC),
   * false if little endian (x86 Intel).
   */
  static public boolean BIG_ENDIAN =
    System.getProperty("sun.cpu.endian").equals("big");


  /**
   * Create a new PGraphicsGL at the specified size.
   * <P/>
   * Unlike unlike other PGraphics objects, the PApplet object passed in
   * cannot be null for this renderer because OpenGL uses a special canvas
   * object that must be added to a component (the host PApplet, in this case)
   * that is visible on screen in order to work properly.
   * @param applet the host applet
   */
  public PGraphicsGL(int width, int height, PApplet applet) {
    //System.out.println("creating PGraphicsGL");

    if (applet == null) {
      throw new RuntimeException("The applet passed to PGraphicsGL " +
                                 "cannot be null");
    }

    //System.out.println("creating PGraphicsGL 2");

    GLCapabilities capabilities = new GLCapabilities();
    canvas = GLDrawableFactory.getFactory().createGLCanvas(capabilities);

    //System.out.println("creating PGraphicsGL 3");

    final PApplet parent = applet;
    canvas.addGLEventListener(new GLEventListener() {

        public void display(GLDrawable drawable) {
          parent.display();  // this means it's time to go
        }

        public void init(GLDrawable drawable) { }

        public void displayChanged(GLDrawable drawable,
                                   boolean modeChanged,
                                   boolean deviceChanged) { }

        public void reshape(GLDrawable drawable,
                            int x, int y, int width, int height) { }
      });

    //System.out.println("creating PGraphicsGL 4");

    applet.setLayout(null);
    applet.add(canvas);
    canvas.setBounds(0, 0, width, height);

    //System.out.println("creating PGraphicsGL 5");
    //System.out.println("adding canvas listeners");
    canvas.addMouseListener(applet);
    canvas.addMouseMotionListener(applet);
    canvas.addKeyListener(applet);
    canvas.addFocusListener(applet);

    //System.out.println("creating PGraphicsGL 6");

    // need to get proper opengl context since will be needed below
    gl = canvas.getGL();
    glu = canvas.getGLU();

    //System.out.println("creating PGraphicsGL 7");

    // this sets width/height and calls allocate() in PGraphics
    resize(width, height);
    //defaults();  // call this just before setup instead

    tobj = glu.gluNewTess();
    // unfortunately glu.gluDeleteTess(tobj); is never called
    //glu.gluTessProperty(tobj, GLU.GLU_TESS_WINDING_RULE,
    //                  GLU.GLU_TESS_WINDING_NONZERO);
    //glu.gluTessProperty(tobj, GLU.GLU_TESS_WINDING_RULE,
    //                  GLU.GLU_TESS_WINDING_POSITIVE);
    //GLU.GLU_TESS_WINDING_ODD);
    //glu.gluTessProperty(tobj, GLU.GLU_TESS_BOUNDARY_ONLY,
    //                  GL.GL_TRUE);

    tessCallback = new TessCallback(); //gl, glu);
    glu.gluTessCallback(tobj, GLU.GLU_TESS_BEGIN, tessCallback);
    glu.gluTessCallback(tobj, GLU.GLU_TESS_END, tessCallback);
    glu.gluTessCallback(tobj, GLU.GLU_TESS_VERTEX, tessCallback);
    glu.gluTessCallback(tobj, GLU.GLU_TESS_COMBINE, tessCallback);
    glu.gluTessCallback(tobj, GLU.GLU_TESS_ERROR, tessCallback);

    //System.out.println("done creating gl");
  }


  protected boolean displayed = false;

  // main applet thread requests an update,
  // but PGraphics has to respond back by calling PApplet.display()
  public void requestDisplay(PApplet parent) {
    //System.out.println("requesting display");

    if (!displayed) {
      // these two method calls (and explanations) were taken from
      // the FpsAnimator implementation from the jogl hoo-ha

      // Try to get OpenGL context optimization since we know we
      // will be rendering this one drawable continually from
      // this thread; make the context current once instead of
      // making it current and freeing it each frame.
      canvas.setRenderingThread(Thread.currentThread());
      //System.out.println(Thread.currentThread());

      // Since setRenderingThread is currently advisory (because
      // of the poor JAWT implementation in the Motif AWT, which
      // performs excessive locking) we also prevent repaint(),
      // which is called from the AWT thread, from having an
      // effect for better multithreading behavior. This call is
      // not strictly necessary, but if end users write their
      // own animation loops which update multiple drawables per
      // tick then it may be necessary to enforce the order of
      // updates.
      canvas.setNoAutoRedrawMode(true);

      // maybe this will help?
      //canvas.requestFocus();

      // done with this business
      displayed = true;
    }
    // request a display from the gl canvas. when it happens,
    // we'll hear about it from the GLEventListener, which will
    // in turn call PApplet.display()... hold your breath...
    try {
      canvas.display();

    } catch (GLException e) {
      Throwable t = e.getCause();
      // if InvocationTargetException, need to unpack one level first
      if (t instanceof InvocationTargetException) {
        Throwable target = ((InvocationTargetException) t).getTargetException();
        throw new RuntimeException(target);
      } else {
        throw new RuntimeException(t);
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }


  /*
  // this was additional stuff used when the animator
  // thread was being shut down.. how to handle this..
  // (doesn't seem to be needed?)

        drawable.setNoAutoRedrawMode(false);
        try {
          // The surface is already unlocked and rendering
          // thread is already null if an exception occurred
          // during display(), so don't disable the rendering
          // thread again.
          if (noException) {
            drawable.setRenderingThread(null);
          }
        } finally {
          synchronized (PAppletThreadGL.this) {
            thread = null;
            PAppletThreadGL.this.notify();
          }
        }
  */


  /**
   * Called by resize(), but nothing to allocate for an OpenGL canvas
   * because OpenGL's pixel buffer is all handled internally.
   */
  protected void allocate() {
    // nothing to do here
  }


  // public void defaults() { }

  /*
  private void syncMatrices()
  {
    gl.glMatrixMode(GL.GL_PROJECTION);
    gl.glLoadMatrixf(new float[] {
      projection.m00, projection.m10, projection.m20, projection.m30,
      projection.m01, projection.m11, projection.m21, projection.m31,
      projection.m02, projection.m12, projection.m22, projection.m32,
      projection.m03, projection.m13, projection.m23, projection.m33
    });

    gl.glMatrixMode(GL.GL_MODELVIEW);
    gl.glLoadIdentity();
    gl.glScalef(1, -1, 1);
  }
  */


  /*
  public void clearLights() {
    super.clearLights();
    for (int i = 0; i < MAX_LIGHTS; i++) {
      lightDisable(i);
    }
  }
  */


  public void beginFrame() {
    super.beginFrame();

    report("top beginFrame()");

    gl.glDisable(GL.GL_LIGHTING);
    for (int i = 0; i < MAX_LIGHTS; i++) {
      gl.glDisable(GL.GL_LIGHT0 + i);
    }

    //syncMatrices();
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
    gl.glLoadMatrixf(projectionFloats);

    gl.glMatrixMode(GL.GL_MODELVIEW);
    gl.glLoadIdentity();
    gl.glScalef(1, -1, 1);

    // these are necessary for alpha (i.e. fonts) to work
    gl.glEnable(GL.GL_BLEND);
    gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);

    // this is necessary for 3D drawing
    gl.glEnable(GL.GL_DEPTH_TEST);
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

    report("bot beginFrame()");
    // are there other things to do here?
    //System.out.println("beginFrame() stop error " + PApplet.hex(gl.glGetError()));
  }


  public void endFrame() {
    //System.out.println("endFrame() error " + PApplet.hex(gl.glGetError()));

    report("top endFrame()");

    if (hints[DEPTH_SORT]) {
      if (triangleCount > 0) {
        depth_sort_triangles();
        render_triangles();
      }
      if (lineCount > 0) {
        depth_sort_lines();
        render_lines();
      }
    }
    //gl.glPopMatrix();

    report("bot endFrame()");
  }


  // For now we do our own lighting (so sum the specular
  // and diffuse light colors...)
  protected void handle_lighting() {
    super.handle_lighting();
    for (int i = vertex_start; i < vertex_end; i++) {
      float v[] = vertices[i];
      v[R] = min(ONE, v[R] + v[SPR]);
      v[G] = min(ONE, v[G] + v[SPG]);
      v[B] = min(ONE, v[B] + v[SPB]);
    }
  }


  protected void render_triangles() {
    report("into triangles");
    //System.out.println("rendering " + triangleCount + " triangles");

    for (int i = 0; i < triangleCount; i ++) {
      //System.out.println("rendering triangle " + i);

      float a[] = vertices[triangles[i][VERTEX1]];
      float b[] = vertices[triangles[i][VERTEX2]];
      float c[] = vertices[triangles[i][VERTEX3]];

      // This is only true when not textured. We really should pass SPECULAR
      // straight through to triangle rendering.
      float ar = min(1, triangleColors[i][0][TRI_DIFFUSE_R] +
                     triangleColors[i][0][TRI_SPECULAR_R]);
      float ag = min(1, triangleColors[i][0][TRI_DIFFUSE_G] +
                     triangleColors[i][0][TRI_SPECULAR_G]);
      float ab = min(1, triangleColors[i][0][TRI_DIFFUSE_B] +
                     triangleColors[i][0][TRI_SPECULAR_B]);
      float br = min(1, triangleColors[i][1][TRI_DIFFUSE_R] +
                     triangleColors[i][1][TRI_SPECULAR_R]);
      float bg = min(1, triangleColors[i][1][TRI_DIFFUSE_G] +
                     triangleColors[i][1][TRI_SPECULAR_G]);
      float bb = min(1, triangleColors[i][1][TRI_DIFFUSE_B] +
                     triangleColors[i][1][TRI_SPECULAR_B]);
      float cr = min(1, triangleColors[i][2][TRI_DIFFUSE_R] +
                     triangleColors[i][2][TRI_SPECULAR_R]);
      float cg = min(1, triangleColors[i][2][TRI_DIFFUSE_G] +
                     triangleColors[i][2][TRI_SPECULAR_G]);
      float cb = min(1, triangleColors[i][2][TRI_DIFFUSE_B] +
                     triangleColors[i][2][TRI_SPECULAR_B]);

      int textureIndex = triangles[i][TEXTURE_INDEX];
      if (textureIndex != -1) {
        //System.out.println("texture drawing");

        PImage texture = textures[textureIndex];
        report("before enable");
        //gl.glEnable(GL.GL_TEXTURE_2D);
        report("after enable");

        ImageCache cash = (ImageCache) texture.cache;  // as in johnny
        if (cash == null) {
          // make sure this thing is cached and a power of 2
          //if (texture.cache == null) {
          cache(texture);
          cash = (ImageCache) texture.cache;
          // mark for update (nope, already updated)
          //texture.updatePixels(0, 0, texture.width, texture.height);
          //texture.modified = true;

        } else if (texture.modified) {
          // TODO make this more efficient and just update a sub-part
          // based on mx1 et al, also use gl function to update
          // only a sub-portion of the image.
          //cash.update(texture.pixels, texture.width, texture.height);
          cash.update(texture);

          gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, 4,
                          cash.twidth, cash.theight,
                          0, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE,
                          //0, GL.GL_BGRA_EXT, GL.GL_UNSIGNED_BYTE,
                          cash.tpixels);

          report("re-binding " + cash.twidth + " " +
                 cash.theight + " " + cash.tpixels);
          gl.glTexParameterf(GL.GL_TEXTURE_2D,
                             GL.GL_TEXTURE_WRAP_S, GL.GL_CLAMP);
          gl.glTexParameterf(GL.GL_TEXTURE_2D,
                             GL.GL_TEXTURE_WRAP_T, GL.GL_CLAMP);
          gl.glTexParameterf(GL.GL_TEXTURE_2D,
                             GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
          gl.glTexParameterf(GL.GL_TEXTURE_2D,
                             GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);

          report("re-binding 3");
          gl.glTexEnvf(GL.GL_TEXTURE_ENV,
                       GL.GL_TEXTURE_ENV_MODE, GL.GL_MODULATE);

          // actually bind this feller
          texture.modified = false;
        }

        report("before bind");
        //System.out.println(gl.glIsTexture(image.tindex));

        //GL_PERSPECTIVE_CORRECTION_HINT to GL_NICEST
        // and running the example again. To do this, use glHint().

        // these don't seem to do much
        //gl.glPixelStorei(GL.GL_UNPACK_ALIGNMENT, 1);
        //gl.glPixelStorei(GL.GL_UNPACK_SWAP_BYTES, 1);

        int tindex = ((ImageCache) texture.cache).tindex;
        gl.glBindTexture(GL.GL_TEXTURE_2D, tindex);

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

        float uscale = (float) texture.width / (float) cash.twidth;
        float vscale = (float) texture.height / (float) cash.theight;

        gl.glEnable(GL.GL_TEXTURE_2D);

        gl.glBegin(GL.GL_TRIANGLES);

        gl.glColor4f(ar, ag, ab, a[A]);
        gl.glTexCoord2f(a[U] * uscale, a[V] * vscale);
        gl.glNormal3f(a[NX], a[NY], a[NZ]);
        gl.glVertex3f(a[VX], a[VY], a[VZ]);

        gl.glColor4f(br, bg, bb, b[A]);
        gl.glTexCoord2f(b[U] * uscale, b[V] * vscale);
        gl.glNormal3f(b[NX], b[NY], b[NZ]);
        gl.glVertex3f(b[VX], b[VY], b[VZ]);

        gl.glColor4f(cr, cg, cb, c[A]);
        gl.glTexCoord2f(c[U] * uscale, c[V] * vscale);
        gl.glNormal3f(c[NX], c[NY], c[NZ]);
        gl.glVertex3f(c[VX], c[VY], c[VZ]);

        gl.glEnd();
        report("non-binding 6");

        gl.glDisable(GL.GL_TEXTURE_2D);

      } else {
        gl.glBegin(GL.GL_TRIANGLES);

        //System.out.println("tri " + a[VX] + " " + a[VY] + " " + a[VZ]);
        //System.out.println("    " + b[VX] + " " + b[VY] + " " + b[VZ]);
        //System.out.println("    " + c[VX] + " " + c[VY] + " " + c[VZ]);

        gl.glColor4f(ar, ag, ab, a[A]);
        gl.glNormal3f(a[NX], a[NY], a[NZ]);
        gl.glVertex3f(a[VX], a[VY], a[VZ]);

        gl.glColor4f(br, bg, bb, b[A]);
        gl.glNormal3f(b[NX], b[NY], b[NZ]);
        gl.glVertex3f(b[VX], b[VY], b[VZ]);

        gl.glColor4f(cr, cg, cb, c[A]);
        gl.glNormal3f(c[NX], c[NY], c[NZ]);
        gl.glVertex3f(c[VX], c[VY], c[VZ]);

        if (ai != null) {
          ai.fillColorRGBA((ar + br + cr) / 3.0f,
                           (ag + bg + cg) / 3.0f,
                           (ab + bb + cb) / 3.0f,
                           (a[A] + b[A] + c[A]) / 3.0f);
          ai.moveto(a[VX], a[VY]);
          ai.lineto(b[VX], b[VY]);
          ai.lineto(c[VX], c[VY]);
          ai.lineto(a[VX], a[VY]);
          /*
          ai.fillColorRGB((ar + br + cr) / 3.0f,
                          (ag + bg + cg) / 3.0f,
                          (ab + bb + cb) / 3.0f);
          ai.moveto(screenX(a[VX], a[VY], a[VZ]),
                    screenY(a[VX], a[VY], a[VZ]));
          ai.lineto(screenX(b[VX], b[VY], b[VZ]),
                    screenY(b[VX], b[VY], b[VZ]));
          ai.lineto(screenX(c[VX], c[VY], c[VZ]),
                    screenY(c[VX], c[VY], c[VZ]));
          ai.lineto(screenX(a[VX], a[VY], a[VZ]),  // closing things off
                    screenY(a[VX], a[VY], a[VZ]));
          */
          ai.fillPath();
        }

        gl.glEnd();
      }
    }
    report("out of triangles");
  }


  public void render_lines() {
    report("render_lines in");

    int i = 0;
    for (int j = 0; j < pathCount; j++) {
      //report("render_lines 1");
      // stroke weight zero will cause a gl error
      if (lines[i][STROKE_WEIGHT] == 0) continue;
      // glLineWidth has to occur outside glBegin/glEnd
      gl.glLineWidth(lines[i][STROKE_WEIGHT]);
      //report("render_lines 2 " + lines[i][STROKE_WEIGHT]);
      gl.glBegin(GL.GL_LINE_STRIP);

      if (ai != null) {
        ai.strokeWeight(lines[i][STROKE_WEIGHT]);
      }

      // always draw a first point
      float a[] = vertices[lines[i][VERTEX1]];
      gl.glColor4f(a[SR], a[SG], a[SB], a[SA]);
      gl.glVertex3f(a[VX], a[VY], a[VZ]);
      //System.out.println("First point: " + a[VX] +", "+ a[VY] +", "+ a[VZ]);

      if (ai != null) {
        ai.strokeColorRGBA(a[SR], a[SG], a[SB], a[SA]);
        //ai.strokeColorRGB(a[SR], a[SG], a[SB]); //, a[SA]);
        ai.moveto(a[VX], a[VY]);
        //ai.moveto(screenX(a[VX], a[VY], a[VZ]),
        //        screenY(a[VX], a[VY], a[VZ]));
      }

      // on this and subsequent lines, only draw the second point
      //System.out.println(pathLength[j]);
      for (int k = 0; k < pathLength[j]; k++) {
        float b[] = vertices[lines[i][VERTEX2]];
        gl.glColor4f(b[SR], b[SG], b[SB], b[SA]);
        gl.glVertex3f(b[VX], b[VY], b[VZ]);

        if (ai != null) {
          ai.lineto(b[VX], b[VY]);
          //ai.lineto(screenX(b[VX], b[VY], b[VZ]),
          //        screenY(b[VX], b[VY], b[VZ]));
        }

        //System.out.println("  point: " + b[VX] +", "+ b[VY] +", "+ b[VZ]);
        //System.out.println();
        i++;
        //report("render_lines path out " + pathLength[j]);
      }
      if (ai != null) {
        ai.endPath();
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


  //////////////////////////////////////////////////////////////


  /**
   * Cache an image using a specified glTexName
   * (name is just an integer index).
   * <p/>
   * If a cacheIndex is already assigned in the image,
   * this request will be ignored.
   */
  protected void cache(PImage image) {
    if (image.cache != null) return;

    int names[] = new int[1];
    gl.glGenTextures(1, names);
    int index = names[0];

    //if (image.tindex != -1) return;

    // use glGetIntegerv with the argument GL_MAX_TEXTURE_SIZE
    // to figure out min/max texture sizes

    //report("into cache");
    //image.modified();
    image.cache = new ImageCache();
    ImageCache cash = (ImageCache) image.cache;
    //cash.update(image.pixels, image.width, image.height);
    cash.update(image);

    // first-time binding of entire texture

    gl.glEnable(GL.GL_TEXTURE_2D);
    gl.glPixelStorei(GL.GL_UNPACK_ALIGNMENT, 1);
    //gl.glPixelStorei(GL.GL_UNPACK_SWAP_BYTES, 0);
    gl.glBindTexture(GL.GL_TEXTURE_2D, index);

    gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, 4, cash.twidth, cash.theight,
                    0, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, cash.tpixels);

    gl.glTexParameterf(GL.GL_TEXTURE_2D,
                       //GL.GL_TEXTURE_MAG_FILTER, GL.GL_NEAREST);
                       GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
    gl.glTexParameterf(GL.GL_TEXTURE_2D,
                       //GL.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST);
                       GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);

    /*
    int err = glu.gluBuild2DMipmaps(GL.GL_TEXTURE_2D, 4,
                                    image.width, image.height,
                                    GL.GL_RGBA,
                                    //GL.GL_ALPHA,
                                    GL.GL_UNSIGNED_BYTE, image.pixels);
    //System.out.println("mipmap: " + err);

    gl.glTexParameterf(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER,
                       GL.GL_NEAREST_MIPMAP_NEAREST);
    gl.glTexParameterf(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER,
                       GL.GL_NEAREST_MIPMAP_LINEAR);
    */

    //gl.glTexParameterf(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, GL.GL_CLAMP);
    //gl.glTexParameterf(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, GL.GL_CLAMP);

    gl.glTexEnvf(GL.GL_TEXTURE_ENV, GL.GL_TEXTURE_ENV_MODE, GL.GL_MODULATE);

    //gl.glDisable(GL.GL_TEXTURE_2D);

    cash.tindex = index;
  }


  class ImageCache {
    int tindex;
    int tpixels[];
    int twidth, theight;

    //public void update(int pixels[], int width, int height) {
    public void update(PImage source) {
      tindex = -1;

      // bit shifting this might be more efficient
      int width2 =
        (int) Math.pow(2, Math.ceil(Math.log(source.width) / Math.log(2)));
      int height2 =
        (int) Math.pow(2, Math.ceil(Math.log(source.height) / Math.log(2)));

      if ((width2 > twidth) || (height2 > theight)) {
        // either twidth/theight are zero, or size has changed
        tpixels = null;
      }
      if (tpixels == null) {
        twidth = width2;
        theight = height2;
        tpixels = new int[twidth * theight];
      }

      // copy image data into the texture
      int p = 0;
      int t = 0;

      //if (System.getProperty("sun.cpu.endian").equals("big")) {
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
          //System.out.println("swapping RGB");
          for (int y = 0; y < source.height; y++) {
            for (int x = 0; x < source.width; x++) {
              int pixel = source.pixels[p++];
              tpixels[t++] = (pixel << 8) | 0xff;
              //tpixels[t++] = pixel;  // nice effect, actually
            }
            t += twidth - source.width;
          }
          break;

        case ARGB:
          //System.out.println("gonna swap ARGB");
          for (int y = 0; y < source.height; y++) {
            for (int x = 0; x < source.width; x++) {
              int pixel = source.pixels[p++];
              tpixels[t++] = (pixel << 8) | ((pixel >> 24) & 0xff);
            }
            t += twidth - source.width;
          }
          break;
        }

      } else {
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
    }
  }


  //////////////////////////////////////////////////////////////


  public float textAscent() {
    if ((textMode != SHAPE) || (textFontNative == null)) {
      return super.textAscent();
    }
    return textFontNativeMetrics.getAscent();
  }


  public float textDescent() {
    if ((textMode != SHAPE) || (textFontNative == null)) {
      return super.textDescent();
    }
    return textFontNativeMetrics.getDescent();
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
    if (textFontNative == null) {
      super.textSize(size);
      return;
    }
    //System.out.println("PGraphicsGL.textSize()");

    textFontNative = textFontNative.deriveFont(size);
    Graphics2D graphics = (Graphics2D) canvas.getGraphics();
    //g2.setFont(textFontNative);
    graphics.setFont(textFontNative);

    // get the metrics info
    //textFontNativeMetrics = g2.getFontMetrics(textFontNative);
    textFontNativeMetrics = graphics.getFontMetrics(textFontNative);
  }


  protected float textWidthImpl(char buffer[], int start, int stop) {
    if ((textMode != SHAPE) || (textFontNative == null)) {
      return super.textWidthImpl(buffer, start, stop);
    }
    // maybe should use one of the newer/fancier functions for this?
    int length = stop - start;
    return textFontNativeMetrics.charsWidth(buffer, start, length);
  }


  /**
   * Override to handle rendering characters with textMode(SHAPE).
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
  protected void textCharImpl(char ch, float x, float y) {
    if (textMode != SHAPE) {
      super.textCharImpl(ch, x, y);
      return;
    }

    // save the current stroke because it needs to be disabled
    // while the text is being drawn
    boolean strokeSaved = stroke;
    stroke = false;

    // six element array received from the Java2D path iterator
    float textPoints[] = new float[6];

    // array passed to createGylphVector
    char textArray[] = new char[] { ch };

    Graphics2D graphics = (Graphics2D) canvas.getGraphics();
    FontRenderContext frc = graphics.getFontRenderContext();
    GlyphVector gv = textFontNative.createGlyphVector(frc, textArray);
    Shape shp = gv.getOutline();
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

    while (!iter.isDone()) {
      int type = iter.currentSegment(textPoints);
      switch (type) {
      case PathIterator.SEG_MOVETO:   // 1 point (2 vars) in textPoints
      case PathIterator.SEG_LINETO:   // 1 point
        if (type == PathIterator.SEG_MOVETO) {
          //System.out.println("moveto\t" +
          //                   textPoints[0] + "\t" + textPoints[1]);
          glu.gluTessBeginContour(tobj);
        } else {
          //System.out.println("lineto\t" +
          //                   textPoints[0] + "\t" + textPoints[1]);
        }
        vertex = new double[] {
          x + textPoints[0], y + textPoints[1], 0
        };
        glu.gluTessVertex(tobj, vertex, vertex);
        lastX = textPoints[0];
        lastY = textPoints[1];
        break;

      case PathIterator.SEG_QUADTO:   // 2 points
        //System.out.println("quadto\t" +
        //                 textPoints[0] + "\t" + textPoints[1] + "\t" +
        //                 textPoints[2] + "\t" + textPoints[3]);

        for (int i = 1; i < bezierDetail; i++) {
          float t = (float)i / (float)bezierDetail;
          vertex = new double[] {
            x + bezierPoint(lastX, textPoints[0],
                            textPoints[2], textPoints[2], t),
            y + bezierPoint(lastY, textPoints[1],
                            textPoints[3], textPoints[3], t), 0
          };
          glu.gluTessVertex(tobj, vertex, vertex);
        }

        /*
        vertex = new double[] {
          x + textPoints[2], y + textPoints[3], 0
        };
        glu.gluTessVertex(tobj, vertex, vertex);
        */

        lastX = textPoints[2];
        lastY = textPoints[3];
        break;

      case PathIterator.SEG_CUBICTO:  // 3 points
        //System.out.println("cubicto\t" +
        //                 textPoints[0] + "\t" + textPoints[1] + "\t" +
        //                 textPoints[2] + "\t" + textPoints[3] + "\t" +
        //                 textPoints[4] + "\t" + textPoints[5]);

        for (int i = 1; i < bezierDetail; i++) {
          float t = (float)i / (float)bezierDetail;
          vertex = new double[] {
            x + bezierPoint(lastX, textPoints[0],
                            textPoints[2], textPoints[4], t),
            y + bezierPoint(lastY, textPoints[1],
                            textPoints[3], textPoints[5], t), 0
          };
          glu.gluTessVertex(tobj, vertex, vertex);
        }
        /*
        vertex = new double[] {
          x + textPoints[4], y + textPoints[5], 0
        };
        glu.gluTessVertex(tobj, vertex, vertex);
        */

        lastX = textPoints[4];
        lastY = textPoints[5];
        break;

      case PathIterator.SEG_CLOSE:
        //System.out.println("close");
        //System.out.println();
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
  public void vertexRedirect(float x, float y, float z) {
    vertex(x, y, z);
  }


  //public static class TessCallback extends GLUtesselatorCallbackAdapter {
  public class TessCallback extends GLUtesselatorCallbackAdapter {
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
      case GL.GL_LINE_LOOP: beginShape(LINE_LOOP); break;
      }
      //System.out.println("shape type is " + shape);
    }

    public void end() {
      //gl.glEnd();
      endShape();
    }

    public void vertex(Object data) {
      if (data instanceof double[]) {
        double[] d = (double[]) data;
        if (d.length != 3) {
          throw new RuntimeException("TessCallback vertex() data " +
                                     "isn't length 3");
        }
        //System.out.println("tess callback vertex " +
        //                 d[0] + " " + d[1] + " " + d[2]);
        vertexRedirect((float) d[0], (float) d[1], (float) d[2]);
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
     * @data is the vertex data to be combined, up to four elements.
     * This is useful when mixing colors together or any other
     * user data that was passed in to gluTessVertex.
     * @weight is an array of weights, one for each element of "data"
     * that should be linearly combined for new values.
     * @outData is the set of new values of "data" after being
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


  //public void cameraMode(int mode) {
  //super.cameraMode(mode);
  //syncMatrices();
  //}

/*
  public void endCamera() {
    //System.out.println("PGraphicsGL.endCamera() 1");
    super.endCamera();

    System.out.println("begin endCamera");
    //System.out.println("PGraphicsGL.endCamera() " + width + " " + height);
    //System.exit(0);

    //System.out.println("into camera error " + PApplet.hex(gl.glGetError()));

    gl.glMatrixMode(GL.GL_PROJECTION);
    //gl.glLoadIdentity();
    //System.out.println("camera should be");
    //printCamera();

    // opengl matrices are rotated from processing's
    gl.glLoadMatrixf(new float[] { projection.m00, projection.m10, projection.m20, projection.m30,
        projection.m01, projection.m11, projection.m21, projection.m31,
        projection.m02, projection.m12, projection.m22, projection.m32,
        projection.m03, projection.m13, projection.m23, projection.m33 } );
    //gl.glScalef(1, -1, 1);

    //System.out.println("trying " + height);
    // this needs to be done since the model matrix will be
    // goofy since it's mid-frame and the translate/scale will be wrong
    gl.glMatrixMode(GL.GL_MODELVIEW);
    gl.glLoadIdentity();
    // gl coordinates are reversed
    gl.glTranslatef(0, height, 0);
    gl.glScalef(1, -1, 1);

    report("out of endCamera");
  }
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
    gl.glLightfv(GL.GL_LIGHT0 + num,
                 GL.GL_AMBIENT, new float[] { lightsDiffuseR[num],
                                              lightsDiffuseG[num],
                                              lightsDiffuseB[num] });
  }


  protected void glLightNoAmbient(int num) {
    gl.glLightfv(GL.GL_LIGHT0 + num,
                 GL.GL_AMBIENT, new float[] { 0, 0, 0 });
  }


  protected void glLightDiffuse(int num) {
    gl.glLightfv(GL.GL_LIGHT0 + num,
                 GL.GL_DIFFUSE, new float[] { lightsDiffuseR[num],
                                              lightsDiffuseG[num],
                                              lightsDiffuseB[num] });
  }


  protected void glLightDirection(int num) {
    if (lights[num] == DIRECTIONAL) {
      gl.glLightfv(GL.GL_LIGHT0 + num,
                   GL.GL_POSITION, new float[] { lightsNX[num],
                                                 lightsNY[num],
                                                 lightsNZ[num], 1 });
    } else {  // spotlight
      gl.glLightfv(GL.GL_LIGHT0 + num,
                   GL.GL_SPOT_DIRECTION,
                   new float[] { lightsNX[num],
                                 lightsNY[num],
                                 lightsNZ[num] });
    }
  }


  protected void glLightEnable(int num) {
    gl.glEnable(GL.GL_LIGHT0 + num);
  }


  protected void glLightFalloff(int num) {
    gl.glLightf(GL.GL_LIGHT0 + num,
                GL.GL_CONSTANT_ATTENUATION, lightsFalloffConstant[num]);
    gl.glLightf(GL.GL_LIGHT0 + num,
                GL.GL_LINEAR_ATTENUATION, lightsFalloffLinear[num]);
    gl.glLightf(GL.GL_LIGHT0 + num,
                GL.GL_QUADRATIC_ATTENUATION, lightsFalloffQuadratic[num]);
  }


  protected void glLightPosition(int num) {
    gl.glLightfv(GL.GL_LIGHT0 + num,
                 GL.GL_POSITION,
                 new float[] { lightsX[num], lightsY[num], lightsZ[num] });
  }


  protected void glLightSpecular(int num) {
    gl.glLightfv(GL.GL_LIGHT0 + num,
                 GL.GL_SPECULAR, new float[] { lightsSpecularR[num],
                                               lightsSpecularG[num],
                                               lightsSpecularB[num] });
  }


  public void glLightSpotAngle(int num) {
    gl.glLightf(GL.GL_LIGHT0 + num,
                GL.GL_SPOT_CUTOFF, lightsSpotAngle[num]);
  }


  public void glLightSpotConcentration(int num) {
    gl.glLightf(GL.GL_LIGHT0 + num,
                GL.GL_SPOT_EXPONENT, lightsSpotConcentration[num]);
  }


  //////////////////////////////////////////////////////////////


  protected void fillFromCalc() {
    super.fillFromCalc();
    gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_AMBIENT_AND_DIFFUSE,
                    new float[] { calcR, calcG, calcB, calcA });
  }


  //////////////////////////////////////////////////////////////


  public void ambient(int rgb) {
    super.ambient(rgb);
    gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_AMBIENT,
                    new float[] { calcR, calcG, calcB, calcA });
  }


  public void ambient(float gray) {
    super.ambient(gray);
    gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_AMBIENT,
                    new float[] { calcR, calcG, calcB, calcA });
  }


  public void ambient(float x, float y, float z) {
    super.ambient(x, y, z);
    gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_AMBIENT,
                    new float[] { calcR, calcG, calcB, calcA });
  }


  //////////////////////////////////////////////////////////////


  public void specular(int rgb) {
    super.specular(rgb);
    gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_SPECULAR,
                    new float[] { calcR, calcG, calcB, calcA });
  }

  public void specular(float gray) {
    super.specular(gray);
    gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_SPECULAR,
                    new float[] { calcR, calcG, calcB, calcA });
  }


  public void specular(float gray, float alpha) {
    super.specular(gray, alpha);
    gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_SPECULAR,
                    new float[] { calcR, calcG, calcB, calcA });
  }


  public void specular(float x, float y, float z) {
    super.specular(x, y, z);
    gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_SPECULAR,
                    new float[] { calcR, calcG, calcB, calcA });
  }


  public void specular(float x, float y, float z, float a) {
    super.specular(x, y, z, a);
    gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_SPECULAR,
                    new float[] { calcR, calcG, calcB, calcA });
  }


  //////////////////////////////////////////////////////////////


  public void emissive(int rgb) {
    super.emissive(rgb);
    gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_EMISSION,
                    new float[] { calcR, calcG, calcB, calcA });
  }


  public void emissive(float gray) {
    super.emissive(gray);
    gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_EMISSION,
                    new float[] { calcR, calcG, calcB, calcA });
  }


  public void emissive(float x, float y, float z) {
    super.emissive(x, y, z);
    gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_EMISSION,
                    new float[] { calcR, calcG, calcB, calcA });
  }


  //////////////////////////////////////////////////////////////


  public void shininess(float shine) {
    super.shininess(shine);
    gl.glMaterialf(GL.GL_FRONT_AND_BACK, GL.GL_SHININESS, shine);
  }


  //////////////////////////////////////////////////////////////


  public void background(PImage image) {
    clear();
    set(0, 0, image);
  }


  public void clear() {
    float backgroundR = (float) ((backgroundColor >> 16) & 0xff) / 255.0f;
    float backgroundG = (float) ((backgroundColor >> 8) & 0xff) / 255.0f;
    float backgroundB = (float) (backgroundColor & 0xff) / 255.0f;

    gl.glClearColor(backgroundR, backgroundG, backgroundB, 1);
    gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
  }


  //////////////////////////////////////////////////////////////


  public void smooth() {
    gl.glEnable(GL.GL_POINT_SMOOTH);
    gl.glEnable(GL.GL_LINE_SMOOTH);
    gl.glEnable(GL.GL_POLYGON_SMOOTH);
  }


  public void noSmooth() {
    gl.glDisable(GL.GL_POINT_SMOOTH);
    gl.glDisable(GL.GL_LINE_SMOOTH);
    gl.glDisable(GL.GL_POLYGON_SMOOTH);
  }


  //////////////////////////////////////////////////////////////


  public void loadPixels() {
    //throw new RuntimeException("loadPixels() not yet implemented for OpenGL");
    if ((pixels == null) || (pixels.length != width*height)) {
      pixels = new int[width * height];
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
                    GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, pixels);

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

          // identical to updatePixels because only two
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

          // identical to updatePixels because only two
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

          // identical to updatePixels because only two
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

    // re-pack ARGB data into RGBA for opengl (big endian)
    /*
    for (int i = 0; i < pixels.length; i++) {
      pixels[i] = ((pixels[i] >> 24) & 0xff) |
        ((pixels[i] << 8) & 0xffffff00);
    }
    */

    //System.out.println("running glDrawPixels");
    //gl.glRasterPos2i(width/2, height/2);
    //gl.glRasterPos2i(width/2, 1); //height/3);
    //gl.glRasterPos2i(1, height - 1); //1, 1);

    // for some reason, glRasterPos(0, height) won't draw anything.
    // my guess is that it's getting "clipped", so adding an epsilon
    // makes it work. also, height-1 would be the logical start,
    // but apparently that's not how opengl coordinates work
    gl.glRasterPos2f(0.0001f, height - 0.0001f);

    gl.glDrawPixels(width, height, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, pixels);
  }


  public void updatePixels(int x, int y, int c, int d) {
    //throw new RuntimeException("updatePixels() not available with OpenGL");
    // TODO make this actually work for a smaller region
    //      problem is, it gets pretty messy with the y reflection, etc
    updatePixels();
  }


  //////////////////////////////////////////////////////////////


  int getset[] = new int[1];

  public int get(int x, int y) {
    gl.glReadPixels(x, y, 1, 1, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, getset);

    if (BIG_ENDIAN) {
      return 0xff000000 | ((getset[0] >> 8)  & 0x00ffffff);

    } else {
      return 0xff000000 |
            ((getset[0] << 16) & 0xff0000) |
            (getset[0] & 0xff00) |
            ((getset[0] >> 16) & 0xff);
    }
  }


  public PImage get(int x, int y, int w, int h) {
    if (imageMode == CORNERS) {  // if CORNER, do nothing
      w = (w - x);
      h = (h - x);
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

    gl.glReadPixels(x, y, w, h, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE,
                    newbie.pixels);

    nativeToJavaARGB(newbie);
    return newbie;
  }


  public PImage get() {
    return get(0, 0, width, height);
  }


  public void set(int x, int y, int argb) {
    if (BIG_ENDIAN) {
      // convert ARGB to RGBA
      getset[0] = (argb << 8) | 0xff;

    } else {
      // convert ARGB to ABGR
      getset[0] =
        (argb & 0xff00ff00) |
        ((argb << 16) & 0xff0000) |
        ((argb >> 16) & 0xff);
    }

    gl.glRasterPos2f(x + EPSILON, y + EPSILON);
    gl.glDrawPixels(1, 1, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, getset);
  }


  /**
   * Set an image directly to the screen.
   * <P>
   * TODO not optimized properly, creates a temporary buffer the
   * size of the image. Needs to instead use image cache, but that
   * requires two types of image cache. One for power of 2 textures
   * and another for glReadPixels/glDrawPixels data that's flipped
   * vertically. Both have their components all swapped to native.
   */
  public void set(int x, int y, PImage source) {
    /*
    ImageCache cash = (ImageCache) source.cache;
    if (cash == null) {
      // this will flip the bits and make it a power of 2
      cache(source);
      cash = (ImageCache) source.cache;
    }
    // now draw to the screen but set the scanline length
    */
    int backup[] = new int[source.pixels.length];
    System.arraycopy(source.pixels, 0, backup, 0, source.pixels.length);
    javaToNativeARGB(source);

    gl.glRasterPos2f(x + EPSILON, (height - y) - EPSILON);
    gl.glDrawPixels(source.width, source.height,
                    GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, source.pixels);
    //nativeToJavaARGB(source);
    source.pixels = backup;
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
   * Currently calls a loadPixels() on the whole canvas, then does the copy,
   * then it calls updatePixels().
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
   * Currently calls a loadPixels() on the whole canvas,
   * then does the copy, then it calls updatePixels().
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
    set(dx, dy, PImage.blend(get(sx, sy), get(dx, dy), mode));
  }


  public void blend(PImage src,
                    int sx, int sy, int dx, int dy, int mode) {
    set(dx, dy, PImage.blend(src.get(sx, sy), get(dx, dy), mode));
  }


  /**
   * TODO - extremely slow and not optimized.
   * Currently calls a loadPixels() on the whole canvas,
   * then does the blend, then it calls updatePixels().
   */
  public void blend(int sx1, int sy1, int sx2, int sy2,
                    int dx1, int dy1, int dx2, int dy2, int mode) {
    loadPixels();
    super.blend(sx1, sy1, sx2, sy2, dx1, dy1, dx2, dy2, mode);
    updatePixels();
  }


  /**
   * TODO - extremely slow and not optimized.
   * Currently calls a loadPixels() on the whole canvas,
   * then does the blend, then it calls updatePixels().
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


  private final float min(float a, float b) {
    return (a < b) ? a : b;
  }


  //////////////////////////////////////////////////////////////


  /**
   * Report on anything from glError().
   * Don't use this inside glBegin/glEnd otherwise it'll
   * throw an GL_INVALID_OPERATION error.
   */
  public void report(String where) {
    //if (true) return;

    int err = gl.glGetError();
    if (err == 0) {
      return;
      //System.out.println("no error");
    } else {
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
  }

//GL will do the clipping for us
  protected void add_line(int a, int b) {
    add_line_no_clip(a, b);
  }

  // GL will do the clipping for us
  protected void add_triangle(int a, int b, int c) {
    add_triangle_no_clip(a, b, c);
  }
}
