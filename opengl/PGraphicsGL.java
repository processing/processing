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
import net.java.games.jogl.*;


public class PGraphicsGL extends PGraphics3 {
  public GL gl;
  public GLU glu;
  public GLCanvas canvas;

  /**
   * true if the host system is big endian (mac, irix, sun),
   * false if little endian (intel).
   */
  static public boolean BIG_ENDIAN =
    System.getProperty("sun.cpu.endian").equals("big");


  /**
   * Create a new PGraphicsGL at the specified size.
   * @param applet the host applet, cannot be null
   */
  public PGraphicsGL(int width, int height, PApplet applet) {
    //System.out.println("creating PGraphicsGL");

    if (applet == null) {
      throw new RuntimeException("The applet passed to PGraphicsGL " +
                                 "cannot be null.");
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

      // done with this business
      displayed = true;
    }
    // request a display from the gl canvas. when it happens,
    // we'll hear about it from the GLEventListener, which will
    // in turn call PApplet.display()... hold your breath...
    try {
      canvas.display();
    } catch (Exception e) {
      //} catch (InterruptedException e) {
      e.printStackTrace();
    }
  }


  /*
  // this was additional stuff used when the animator
  // thread was being shut down.. how to handle this..

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
   * Called by resize(), but nothing to allocate for an OpenGL canvas.
   */
  protected void allocate() {
    // nothing to do here just yet
    // normally this allocates the pixel buffer, etc.
  }


  // public void defaults() { }
  // this sets up the positions of the two base lights
  // not sure if this needs to be enabled in opengl


  public void beginFrame() {
    super.beginFrame();
    //resetMatrix();
    //normal(0, 0, 1);
    //System.out.println("beginFrame() start error " + PApplet.hex(gl.glGetError()));

    report("top beginFrame()");

    gl.glMatrixMode(GL.GL_MODELVIEW);
    gl.glLoadIdentity();
    // gl coordinates are reversed
    gl.glTranslatef(0, height, 0);
    gl.glScalef(1, -1, 1);
    //gl.glPushMatrix();

    // these are necessary for alpha (i.e. fonts) to work
    gl.glEnable(GL.GL_BLEND);
    gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);

    // this is necessary for 3D drawing
    gl.glEnable(GL.GL_DEPTH_TEST);
    // use <= since that's what processing.core does
    gl.glDepthFunc(GL.GL_LEQUAL);

    // I never really got the hang of lighting in OpenGL ...
    // I've done something like [the following] which at least
    // demonstrates that it works... --tom carden

    // because y is flipped
    gl.glFrontFace(GL.GL_CW);

    // coloured stuff
    gl.glEnable(GL.GL_COLOR_MATERIAL);
    gl.glColorMaterial(GL.GL_FRONT_AND_BACK, GL.GL_AMBIENT_AND_DIFFUSE);
    gl.glColorMaterial(GL.GL_FRONT_AND_BACK, GL.GL_SPECULAR);

    // these tend to make life easier
    // (but sometimes at the expense of a little speed)
    gl.glEnable(GL.GL_NORMALIZE);
    gl.glEnable(GL.GL_AUTO_NORMAL); // I think this is OpenGL 1.2 only
    gl.glEnable(GL.GL_RESCALE_NORMAL);

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


  protected void render_triangles() {
  //public void render_triangles() {
    report("into triangles");
    //System.out.println("into triangles error " + PApplet.hex(gl.glGetError()));

    //System.out.println("rendering " + triangleCount + " triangles");

    for (int i = 0; i < triangleCount; i ++) {
      //System.out.println("  rendering triangle " + i);

      float a[] = vertices[triangles[i][VERTEX1]];
      float b[] = vertices[triangles[i][VERTEX2]];
      float c[] = vertices[triangles[i][VERTEX3]];

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

        gl.glColor4f(a[R], a[G], a[B], a[A]);
        gl.glTexCoord2f(a[U] * uscale, a[V] * vscale);
        gl.glVertex3f(a[VX], a[VY], a[VZ]);

        gl.glColor4f(b[R], b[G], b[B], b[A]);
        gl.glTexCoord2f(b[U] * uscale, b[V] * vscale);
        gl.glVertex3f(b[VX], b[VY], b[VZ]);

        gl.glColor4f(c[R], c[G], c[B], c[A]);
        gl.glTexCoord2f(c[U] * uscale, c[V] * vscale);
        gl.glVertex3f(c[VX], c[VY], c[VZ]);

        gl.glEnd();
        report("non-binding 6");

        gl.glDisable(GL.GL_TEXTURE_2D);

      } else {
        gl.glBegin(GL.GL_TRIANGLES);

        gl.glColor4f(a[R], a[G], a[B], a[A]);
        gl.glVertex3f(a[VX], a[VY], a[VZ]);

        gl.glColor4f(b[R], b[G], b[B], b[A]);
        gl.glVertex3f(b[VX], b[VY], b[VZ]);

        gl.glColor4f(c[R], c[G], c[B], c[A]);
        gl.glVertex3f(c[VX], c[VY], c[VZ]);

        gl.glEnd();
      }
    }
    report("out of triangles");
  }


  public void render_lines() {
    //System.out.println("into lines error " + PApplet.hex(gl.glGetError()));
    int i = 0;
    for (int j = 0; j < pathCount; j++) {
      //report("render_lines 1");
      // glLineWidth has to occur outside glBegin/glEnd
      gl.glLineWidth(lines[i][STROKE_WEIGHT]);
      //report("render_lines 2 " + lines[i][STROKE_WEIGHT]);
      gl.glBegin(GL.GL_LINE_STRIP);

      // always draw a first point
      float a[] = vertices[lines[i][VERTEX1]];
      gl.glColor4f(a[SR], a[SG], a[SB], a[SA]);
      gl.glVertex3f(a[VX], a[VY], a[VZ]);

      // on this and subsequent lines, only draw the second point
      for (int k = 0; k < pathLength[j]; k++) {
        float b[] = vertices[lines[i][VERTEX2]];
        gl.glColor4f(b[SR], b[SG], b[SB], b[SA]);
        gl.glVertex3f(b[VX], b[VY], b[VZ]);
        i++;
      }

      gl.glEnd();
      //report("render_lines 3 " + pathLength[j]);
    }
    //System.out.println("outta lines error " + PApplet.hex(gl.glGetError()));
  }


  /**
   * Handled entirely by OpenGL, so use this to override the superclass.
   */
  protected void light_and_transform() {
  }


  //////////////////////////////////////////////////////////////


  /**
   * Cache an image using a specified glTexName
   * (name is just an integer index).
   *
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


  public void textMode(int mode) {
    // TODO get this guy straightened out
    if (mode != OBJECT) {
      throw new RuntimeException("only textMode(OBJECT) is " +
                                 "currently supported for OpenGL");
    }
    super.textMode(mode);
  }


  //////////////////////////////////////////////////////////////


  public void endCamera() {
    //System.out.println("PGraphicsGL.endCamera() 1");
    super.endCamera();

    report("begin endCamera");
    //System.out.println("PGraphicsGL.endCamera() " + width + " " + height);
    //System.exit(0);

    //System.out.println("into camera error " + PApplet.hex(gl.glGetError()));

    gl.glMatrixMode(GL.GL_PROJECTION);
    //gl.glLoadIdentity();
    //System.out.println("camera should be");
    //printCamera();

    /*
    gl.glLoadMatrixf(new float[] { p00, p01, p02, p03,
                                   p10, p11, p12, p13,
                                   p20, p21, p22, p23,
                                   p30, p31, p32, p33 } );
    */

    // opengl matrices are rotated from processing's
    gl.glLoadMatrixf(new float[] { p00, p10, p20, p30,
                                   p01, p11, p21, p31,
                                   p02, p12, p22, p32,
                                   p03, p13, p23, p33 } );
    //gl.glScalef(1, -1, 1);

    //System.out.println("trying " + height);
    // this needs to be done since the model matrix will be
    // goofy since it's mid-frame and the translate/scale will be wrong
    gl.glMatrixMode(GL.GL_MODELVIEW);
    gl.glLoadIdentity();
    // gl coordinates are reversed
    gl.glTranslatef(0, height, 0);
    gl.glScalef(1, -1, 1);

    /*
    float proj[] = new float[16];
    gl.glGetFloatv(GL.GL_PROJECTION_MATRIX, proj);

    //float mod[] = new float[16];
    //gl.glGetFloatv(GL.GL_MODELVIEW_MATRIX, mod);

    for (int i = 0; i < 16; i++) {
      if ((i % 4) == 0) System.out.println();
      System.out.print(PApplet.nfs(proj[i], 3, 4) + " ");
    }
    System.out.println();
    */

    report("out of endCamera");
  }


  //////////////////////////////////////////////////////////////


  public void lights() {
    super.lights();
    gl.glEnable(GL.GL_LIGHTING);
  }

  public void noLights() {
    super.noLights();
    gl.glDisable(GL.GL_LIGHTING);
  }


  public void lightEnable(int num) {
    super.lightEnable(num);
    gl.glEnable(GL.GL_LIGHT0 + num);
  }


  public void lightDisable(int num) {
    super.lightDisable(num);
    gl.glDisable(GL.GL_LIGHT0 + num);
  }


  public void lightPosition(int num, float x, float y, float z) {
    super.lightPosition(num, x, y, z);
    gl.glLightfv(GL.GL_LIGHT0 + num,
                 GL.GL_POSITION, new float[] { x, y, z });
  }


  public void lightAmbient(int num, float x, float y, float z) {
    super.lightAmbient(num, x, y, z);
    gl.glLightfv(GL.GL_LIGHT0 + num,
                 GL.GL_AMBIENT, new float[] { lightAmbientR[num],
                                              lightAmbientG[num],
                                              lightAmbientB[num] });
  }


  public void lightDiffuse(int num, float x, float y, float z) {
    super.lightDiffuse(num, x, y, z);
    gl.glLightfv(GL.GL_LIGHT0 + num,
                 GL.GL_DIFFUSE, new float[] { lightDiffuseR[num],
                                              lightDiffuseG[num],
                                              lightDiffuseB[num] });
  }


  public void lightSpecular(int num, float x, float y, float z) {
    super.lightSpecular(num, x, y, z);
    gl.glLightfv(GL.GL_LIGHT0 + num,
                 GL.GL_SPECULAR, new float[] { lightSpecularR[num],
                                               lightSpecularG[num],
                                               lightSpecularB[num] });
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
    //throw new RuntimeException("get() not yet implemented for OpenGL");
    //return 0; // TODO
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
    //newbie.updatePixels();

    /*
    int index = y*width + x;
    int index2 = 0;
    for (int row = y; row < y+h; row++) {
      System.arraycopy(pixels, index,
                       newbie.pixels, index2, w);
      index+=width;
      index2+=w;
    }
    */
    return newbie;
    //throw new RuntimeException("get() not yet implemented for OpenGL");
    //return null; // TODO
  }


  public PImage get() {
    return get(0, 0, width, height);
  }


  //PImage setter = new PImage(1, 1);

  public void set(int x, int y, int argb) {
    if (BIG_ENDIAN) {
      // convert ARGB to RGBA
      getset[0] = (argb << 8) | 0xff;

    } else {
      // convert ARGB to ABGR
      getset[0] =
        (argb & 0xff00ff00) |
        ((argb << 16) & 0xff0000) |
        //(argb & 0xff00) |
        ((argb >> 16) & 0xff);
    }

    gl.glRasterPos2f(x + EPSILON, y + EPSILON);
    gl.glDrawPixels(1, 1, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, getset);
    //throw new RuntimeException("set() not available with OpenGL");
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
   * This is really inefficient and not a good idea.
   * Use get() and set() with a smaller image area.
   */
  public void filter(int kind) {
    //throw new RuntimeException("filter() not available with OpenGL");
    PImage temp = get();
    temp.filter(kind);
    set(0, 0, temp);
  }


  /**
   * This is really inefficient and not a good idea.
   * Use get() and set() with a smaller image area.
   */
  public void filter(int kind, float param) {
    //throw new RuntimeException("filter() not available with OpenGL");
    PImage temp = get();
    temp.filter(kind, param);
    set(0, 0, temp);
  }


  //////////////////////////////////////////////////////////////


  // TODO implement these with glCopyPixels

  //public void copy(PImage src, int dx, int dy) {
  //throw new RuntimeException("copy() not available with OpenGL");
  //}


  /**
   * TODO - extremely slow and not optimized.
   * Currently calls a loadPixels() on the whole canvas,
   * then does the copy, then it calls updatePixels().
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
    //loadPixels();
    //super.blend(sx, sy, dx, dy, mode);
    //updatePixels();
  }


  public void blend(PImage src,
                    int sx, int sy, int dx, int dy, int mode) {
    set(dx, dy, PImage.blend(src.get(sx, sy), get(dx, dy), mode));
    //loadPixels();
    //super.blend(src, sx, sy, dx, dy, mode);
    //updatePixels();
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
      System.out.print(where + ": ");
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
}
