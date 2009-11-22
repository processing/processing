package processing.android.core;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

import android.opengl.GLU;
import android.view.SurfaceHolder;

//import javax.microedition.khronos.egl.EGL;
import javax.microedition.khronos.opengles.*;

import processing.android.opengl.EglHelper;


// drawPixels is missing...calls to glDrawPixels are commented out
//   setRasterPos() is also commented out
// remove the BufferUtil class at the end (verify the endian order, rewind, etc)

// other changes:
// mipmaps are disabled


public class PGraphicsAndroid3D extends PGraphics3D {
  SurfaceHolder holder;
  
  public GL10 gl;
  public GLU glu;

  protected float[] projectionFloats;

  /// Buffer to hold light values before they're sent to OpenGL
  //protected FloatBuffer lightBuffer;
  protected float[] lightArray = new float[] { 1, 1, 1 };

  static int maxTextureSize;

  int[] textureDeleteQueue = new int[10];
  int textureDeleteQueueCount = 0;

  /// Used to hold color values to be sent to OpenGL
  //protected FloatBuffer colorBuffer;
  protected float[] colorFloats;

  /// Used to store empty values to be passed when a light has no ambient value
  //protected FloatBuffer zeroBuffer;
  protected float[] zeroFloats = new float[] { 0, 0, 0 };

  /// IntBuffer to go with the pixels[] array
  protected IntBuffer pixelBuffer;
  
  /**
   * Set to true if the host system is big endian (PowerPC, MIPS, SPARC),
   * false if little endian (x86 Intel for Mac or PC).
   */
  static public boolean BIG_ENDIAN =
    ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN;

  
  private EglHelper mEglHelper;

  
  public PGraphicsAndroid3D() {
    glu = new GLU();  // or maybe not until used?
  }
  
  
  //public void setParent(PApplet parent)
  
  
  //public void setPrimary(boolean primary)
  
  
  //public void setPath(String path)
  
  
  public void setSize(int iwidth, int iheight) {
//    public void sizeChanged(GL10 gl, int width, int height) {
    gl.glViewport(0, 0, width, height);

    // Set our projection matrix. This doesn't have to be done
    // each time we draw, but usually a new projection needs to
    // be set when the viewport is resized.
    float ratio = (float) width / height;
    gl.glMatrixMode(GL10.GL_PROJECTION);
    gl.glLoadIdentity();
    gl.glFrustumf(-ratio, ratio, -1, 1, 1, 10);
  }


  public void setSurfaceHolder(SurfaceHolder holder) {
    this.holder = holder;
  }
  
  
  protected void allocate() {
    mEglHelper.start();  // excessive, but it'll check if it's started or not
    gl = (GL10) mEglHelper.createSurface(holder);

//    public void surfaceCreated(GL10 gl) {
    
    // By default, OpenGL enables features that improve quality
    // but reduce performance. One might want to tweak that
    // especially on software renderer.
    gl.glDisable(GL10.GL_DITHER);

    // Some one-time OpenGL initialization can be made here
    // probably based on features of this particular context
    gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT,
              GL10.GL_FASTEST);

//    if (mTranslucentBackground) {
//      gl.glClearColor(0,0,0,0);
//    } else {
    gl.glClearColor(1,1,1,1);
//    }
    gl.glEnable(GL10.GL_CULL_FACE);
    gl.glShadeModel(GL10.GL_SMOOTH);
    gl.glEnable(GL10.GL_DEPTH_TEST);
  }

  
  public void dispose() {
    mEglHelper.finish();
  }
  
  

  //////////////////////////////////////////////////////////////

  // FRAME


  /**
   * OpenGL cannot draw until a proper native peer is available, so this
   * returns the value of PApplet.isDisplayable() (inherited from Component).
   */
  public boolean canDraw() {
    return true;
    //return parent.isDisplayable();
  }


  public void beginDraw() {
    // originally created at the start of guardedRun(), instead create on 
    // first use inside beginDraw(). can't do this in constructor cuz opengl
    // won't be ready yet (and semaphore not locked, etc).
    if (mEglHelper == null) {
      mEglHelper = new EglHelper();
    }
    
//    // When using an offscreen buffer, the drawable instance will be null.
//    // The offscreen buffer uses the drawing context of the main PApplet.
//    if (drawable != null) {
//      // Call setRealized() after addNotify() has been called
//      drawable.setRealized(parent.isDisplayable());
//      //System.out.println("OpenGL beginDraw() setting realized " + parent.isDisplayable());
//      if (parent.isDisplayable()) {
//        //System.out.println("  we'll realize it alright");
//        drawable.setRealized(true);
//      } else {
//        //System.out.println("  not yet ready to be realized");
//        return;  // Should have called canDraw() anyway
//      }
//      detainContext();
//    }
    

    // On the first frame that's guaranteed to be on screen,
    // and the component valid and all that, ask for focus.
//    if ((parent != null) && parent.frameCount == 1) {
//      canvas.requestFocus();
//    }

    super.beginDraw();

    report("top beginDraw()");

    /* draw method from gl renderer app
    // Usually, the first thing one might want to do is to clear
    // the screen. The most efficient way of doing this is to use glClear().
    gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);

    gl.glMatrixMode(GL10.GL_MODELVIEW);
    gl.glLoadIdentity();
    gl.glTranslatef(0, 0, -3.0f);
    gl.glRotatef(mAngle,        0, 1, 0);
    gl.glRotatef(mAngle*0.25f,  1, 0, 0);

    gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
    gl.glEnableClientState(GL10.GL_COLOR_ARRAY);

    mCube.draw(gl);

    gl.glRotatef(mAngle*2.0f, 0, 1, 1);
    gl.glTranslatef(0.5f, 0.5f, 0.5f);

    mCube.draw(gl);

    mAngle += 1.2f;
     */
    
    gl.glDisable(GL10.GL_LIGHTING);
    for (int i = 0; i < MAX_LIGHTS; i++) {
      gl.glDisable(GL10.GL_LIGHT0 + i);
    }

    gl.glMatrixMode(GL10.GL_PROJECTION);
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

    gl.glMatrixMode(GL10.GL_MODELVIEW);
    gl.glLoadIdentity();
    // Flip Y-axis to make y count from 0 downwards
    gl.glScalef(1, -1, 1);

    // these are necessary for alpha (i.e. fonts) to work
    gl.glEnable(GL10.GL_BLEND);
    gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);

    // this is necessary for 3D drawing
    if (hints[DISABLE_DEPTH_TEST]) {
      gl.glDisable(GL10.GL_DEPTH_TEST);
    } else {
      gl.glEnable(GL10.GL_DEPTH_TEST);
    }
    // use <= since that's what processing.core does
    gl.glDepthFunc(GL10.GL_LEQUAL);

    // because y is flipped
    gl.glFrontFace(GL10.GL_CW);

    // coloured stuff
    gl.glEnable(GL10.GL_COLOR_MATERIAL);
    // TODO maybe not available in OpenGL ES?
//    gl.glColorMaterial(GL10.GL_FRONT_AND_BACK, GL10.GL_AMBIENT_AND_DIFFUSE);
//    gl.glColorMaterial(GL10.GL_FRONT_AND_BACK, GL10.GL_SPECULAR);

    // these tend to make life easier
    // (but sometimes at the expense of a little speed)
    // Not using them right now because we're doing our own lighting.
    //gl.glEnable(GL10.GL_NORMALIZE);
    //gl.glEnable(GL10.GL_AUTO_NORMAL); // I think this is OpenGL 1.2 only
    //gl.glEnable(GL10.GL_RESCALE_NORMAL);
    //gl.GlLightModeli(GL10.GL_LIGHT_MODEL_COLOR_CONTROL, GL_SEPARATE_SPECULAR_COLOR);

    report("bot beginDraw()");
  }


  public void endDraw() {
    report("top endDraw()");

    if (hints[ENABLE_DEPTH_SORT]) {
      flush();
    }

//    if (drawable != null) {
//      drawable.swapBuffers();
//    }
    boolean success = mEglHelper.swap();
    if (!success) {
      System.err.println("Could not swap buffers.");
    }

    report("bot endDraw()");

//    if (drawable != null) {
//      releaseContext();
//    }
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
  
  
  ////////////////////////////////////////////////////////////

  // SETTINGS

  // checkSettings, defaultSettings, reapplySettings in PGraphics



  ////////////////////////////////////////////////////////////

  // HINTS


  public void hint(int which) {
    super.hint(which);

    if (which == DISABLE_DEPTH_TEST) {
      gl.glDisable(GL10.GL_DEPTH_TEST);
      gl.glClear(GL10.GL_DEPTH_BUFFER_BIT);

    } else if (which == ENABLE_DEPTH_TEST) {
      gl.glEnable(GL10.GL_DEPTH_TEST);

    } else if (which == DISABLE_OPENGL_2X_SMOOTH) {
      // TODO throw an error?

    } else if (which == ENABLE_OPENGL_2X_SMOOTH) {
      // TODO throw an error?

    } else if (which == ENABLE_OPENGL_4X_SMOOTH) {
      // TODO throw an error?      
    }
  }



  //////////////////////////////////////////////////////////////

  // VERTEX SHAPES

  // All picked up from either PGraphics or PGraphics3D


  //public void beginShape()
  //public void beginShape(int kind)
  //public void edge(boolean e)
  //public void normal(float nx, float ny, float nz)
  //public void textureMode(int mode)
  //public void texture(PImage image)
  
  
  private IntBuffer vertexBuffer;
  private IntBuffer colorBuffer;
  private IntBuffer textureBuffer;
  private IntBuffer normalBuffer;
  
  static public int toFixed32(float x) {
    return (int) (x * 65536.0f);
  }
  
  static public int toFixed16(float x) {
    return (int) (x * 4096.0f);
  }
  
  protected void vertexCheck() {
    super.vertexCheck();

    int vertexAlloc = vertices.length;
    if (vertexBuffer == null || vertexBuffer.capacity() != vertexAlloc) {
      ByteBuffer vbb = ByteBuffer.allocateDirect(vertexAlloc * 3);
      vbb.order(ByteOrder.nativeOrder());
      vertexBuffer = vbb.asIntBuffer();
//      vertexBuffer.put(vertices);
//      vertexBuffer.position(0);

      ByteBuffer cbb = ByteBuffer.allocateDirect(vertexAlloc * 4);
      cbb.order(ByteOrder.nativeOrder());
      colorBuffer = cbb.asIntBuffer();
//      mColorBuffer.put(colors);
//      mColorBuffer.position(0);

      ByteBuffer tbb = ByteBuffer.allocateDirect(vertexAlloc * 2);
      tbb.order(ByteOrder.nativeOrder());
      textureBuffer = tbb.asIntBuffer();
      
      ByteBuffer nbb = ByteBuffer.allocateDirect(vertexAlloc * 3);
      nbb.order(ByteOrder.nativeOrder());
      normalBuffer = nbb.asIntBuffer();
    }
  }

  
  //public void vertex(float x, float y)
  //public void vertex(float x, float y, float z)
  //public void vertex(float x, float y, float u, float v)
  //public void vertex(float x, float y, float z, float u, float v)
  //protected void vertexTexture(float u, float v);
  //public void breakShape()
  //public void endShape()
  //public void endShape(int mode)


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



  //////////////////////////////////////////////////////////////

  // BEZIER CURVE VERTICES

  // All picked up from either PGraphics or PGraphics3D, however
  // a faster version that made use of OpenGL's evaluator methods
  // would be a nice improvement.


  //protected void bezierVertexCheck();
  //public void bezierVertex(float x2, float y2,
  //                         float x3, float y3,
  //                         float x4, float y4)
  //public void bezierVertex(float x2, float y2, float z2,
  //                         float x3, float y3, float z3,
  //                         float x4, float y4, float z4)



  //////////////////////////////////////////////////////////////

  // CATMULL-ROM CURVE VERTICES

  // Like bezier, these could be implemented using an OpenGL evaluator.


  //protected void curveVertexCheck();
  //public void curveVertex(float x, float y)
  //public void curveVertex(float x, float y, float z)
  //protected void curveVertexSegment(float x1, float y1,
  //                                  float x2, float y2,
  //                                  float x3, float y3,
  //                                  float x4, float y4)
  //protected void curveVertexSegment(float x1, float y1, float z1,
  //                                  float x2, float y2, float z2,
  //                                  float x3, float y3, float z3,
  //                                  float x4, float y4, float z4)



  //////////////////////////////////////////////////////////////

  // POINTS (override from P3D)


  // Buffers to be passed to gl*Pointer() functions
  // must be direct, i.e., they must be placed on the
  // native heap where the garbage collector cannot
  // move them.
  //
  // Buffers with multi-byte datatypes (e.g., short, int, float)
  // must have their byte order set to native order

//  ByteBuffer vbb = ByteBuffer.allocateDirect(vertices.length*4);
//  vbb.order(ByteOrder.nativeOrder());
//  mVertexBuffer = vbb.asIntBuffer();
//  mVertexBuffer.put(vertices);
//  mVertexBuffer.position(0);
//
//  ByteBuffer cbb = ByteBuffer.allocateDirect(colors.length*4);
//  cbb.order(ByteOrder.nativeOrder());
//  mColorBuffer = cbb.asIntBuffer();
//  mColorBuffer.put(colors);
//  mColorBuffer.position(0);
//
//  mIndexBuffer = ByteBuffer.allocateDirect(indices.length);
//  mIndexBuffer.put(indices);
//  mIndexBuffer.position(0);
  
//  gl.glFrontFace(gl.GL_CW);
//  gl.glVertexPointer(3, gl.GL_FIXED, 0, mVertexBuffer);
//  gl.glColorPointer(4, gl.GL_FIXED, 0, mColorBuffer);
//  gl.glDrawElements(gl.GL_TRIANGLES, 36, gl.GL_UNSIGNED_BYTE, mIndexBuffer);

  
  
  protected void renderPoints(int start, int stop) {
    gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
    gl.glEnableClientState(GL10.GL_COLOR_ARRAY);

    vertexBuffer.rewind();
    colorBuffer.rewind();
    
    float sw = vertices[lines[start][VERTEX1]][SW];
    if (sw > 0) {
      gl.glPointSize(sw);  // can only be set outside glBegin/glEnd
//      gl.glBegin(GL10.GL_POINTS);
      for (int i = start; i < stop; i++) {
//        gl.glColor4f(a[SR], a[SG], a[SB], a[SA]);
        float[] a = vertices[points[i][VERTEX1]];
        colorBuffer.put(toFixed16(a[SR]));
        colorBuffer.put(toFixed16(a[SG]));
        colorBuffer.put(toFixed16(a[SB]));
        colorBuffer.put(toFixed16(a[SA]));
//        gl.glVertex3f(a[VX], a[VY], a[VZ]);
        vertexBuffer.put(toFixed32(a[VX]));
        vertexBuffer.put(toFixed32(a[VY]));
        vertexBuffer.put(toFixed32(a[VZ]));
      }
//      gl.glEnd();
      gl.glVertexPointer(3, GL10.GL_FIXED, 0, vertexBuffer);
      gl.glColorPointer(4, GL10.GL_FIXED, 0, colorBuffer);
      gl.glDrawArrays(GL10.GL_POINTS, start, stop - start);
    }

    gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
    gl.glDisableClientState(GL10.GL_COLOR_ARRAY);
  }


  //protected void rawPoints(int start, int stop)  // PGraphics3D



  //////////////////////////////////////////////////////////////

  // LINES (override from P3D)


  //protected final void addLineBreak()  // PGraphics3D


  /**
   * Add this line, but disable clipping because GL will handle it.
   */
  protected void addLine(int a, int b) {
    addLineWithoutClip(a, b);
  }


  //protected final void addLineWithClip(int a, int b)


  //protected final void addLineWithoutClip(int a, int b)


  /**
   * In the current implementation, start and stop are ignored (in OpenGL).
   * This will obviously have to be revisited if/when proper depth sorting
   * is implemented.
   */
  protected void renderLines(int start, int stop) {
    report("render_lines in");

    gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
    gl.glEnableClientState(GL10.GL_COLOR_ARRAY);

    //int i = 0;
    for (int j = 0; j < pathCount; j++) {
      int i = pathOffset[j];
      float sw = vertices[lines[i][VERTEX1]][SW];
      //report("render_lines 1");
      // stroke weight zero will cause a gl error
      if (sw > 0) {
        // glLineWidth has to occur outside glBegin/glEnd
        gl.glLineWidth(sw);
//        gl.glBegin(GL10.GL_LINE_STRIP);
        vertexBuffer.rewind();
        colorBuffer.rewind();

        // always draw a first point
        float a[] = vertices[lines[i][VERTEX1]];
//        gl.glColor4f(a[SR], a[SG], a[SB], a[SA]);
        colorBuffer.put(toFixed16(a[SR]));
        colorBuffer.put(toFixed16(a[SG]));
        colorBuffer.put(toFixed16(a[SB]));
        colorBuffer.put(toFixed16(a[SA]));
//        gl.glVertex3f(a[VX], a[VY], a[VZ]);
        vertexBuffer.put(toFixed32(a[VX]));
        vertexBuffer.put(toFixed32(a[VY]));
        vertexBuffer.put(toFixed32(a[VZ]));

        // on this and subsequent lines, only draw the second point
        //System.out.println(pathLength[j]);
        for (int k = 0; k < pathLength[j]; k++) {
          float b[] = vertices[lines[i][VERTEX2]];
//          gl.glColor4f(b[SR], b[SG], b[SB], b[SA]);
          colorBuffer.put(toFixed16(b[SR]));
          colorBuffer.put(toFixed16(b[SG]));
          colorBuffer.put(toFixed16(b[SB]));
          colorBuffer.put(toFixed16(b[SA]));
          
          //gl.glEdgeFlag(a[EDGE] == 1);
          
//          gl.glVertex3f(b[VX], b[VY], b[VZ]);
          vertexBuffer.put(toFixed32(b[VX]));
          vertexBuffer.put(toFixed32(b[VY]));
          vertexBuffer.put(toFixed32(b[VZ]));
          i++;
        }
//        gl.glEnd();
        gl.glVertexPointer(3, GL10.GL_FIXED, 0, vertexBuffer);
        gl.glColorPointer(4, GL10.GL_FIXED, 0, colorBuffer);
        gl.glDrawArrays(GL10.GL_LINE_STRIP, 0, pathLength[j] + 1);        
      }
    }
    report("render_lines out");
  }


  //protected void rawLines(int start, int stop)



  //////////////////////////////////////////////////////////////

  // TRIANGLES


  /**
   * Add the triangle, but disable clipping because GL will handle it.
   */
  protected void addTriangle(int a, int b, int c) {
    addTriangleWithoutClip(a, b, c);
  }


  protected void renderTriangles(int start, int stop) {
    report("render_triangles in");

    gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
    gl.glEnableClientState(GL10.GL_COLOR_ARRAY);
    gl.glEnableClientState(GL10.GL_NORMAL_ARRAY);

    float uscale = 0;
    float vscale = 0;
    
    for (int i = start; i < stop; i++) {
      float a[] = vertices[triangles[i][VERTEX1]];
      float b[] = vertices[triangles[i][VERTEX2]];
      float c[] = vertices[triangles[i][VERTEX3]];

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

      int textureIndex = triangles[i][TEXTURE_INDEX];
      if (textureIndex != -1) {
        report("before enable");
        gl.glEnable(GL10.GL_TEXTURE_2D);
        report("after enable");

        report("before bind");
        PImage texture = textures[textureIndex];
        bindTexture(texture);
        report("after bind");

        ImageCache cash = (ImageCache) texture.getCache(this);
        uscale = (float) texture.width / (float) cash.twidth;
        vscale = (float) texture.height / (float) cash.theight;

        gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
      }

//      gl.glBegin(GL10.GL_TRIANGLES);
      vertexBuffer.rewind();
      colorBuffer.rewind();
      normalBuffer.rewind();

      if (textureIndex != -1) {
        textureBuffer.rewind();
      }
        
      // vertex A
        
        //System.out.println(a[U] + " " + a[V] + " " + uscale + " " + vscale);
        //System.out.println(ar + " " + ag + " " + ab + " " + a[A]);
        //ar = ag = ab = 1;
//        gl.glColor4f(ar, ag, ab, a[A]);
      colorBuffer.put(toFixed16(ar));
      colorBuffer.put(toFixed16(ag));
      colorBuffer.put(toFixed16(ab));
      colorBuffer.put(toFixed16(a[A]));
        
//        gl.glTexCoord2f(a[U] * uscale, a[V] * vscale);
      if (textureIndex != -1) {
        textureBuffer.put(toFixed32(a[U] * uscale));
        textureBuffer.put(toFixed32(a[V] * vscale));
      }
        
//        gl.glNormal3f(a[NX], a[NY], a[NZ]);
      normalBuffer.put(toFixed32(a[NX]));
      normalBuffer.put(toFixed32(a[NY]));
      normalBuffer.put(toFixed32(a[NZ]));
        
//        gl.glEdgeFlag(a[EDGE] == 1);
        
//        gl.glVertex3f(a[VX], a[VY], a[VZ]);
      vertexBuffer.put(toFixed32(a[VX]));
      vertexBuffer.put(toFixed32(a[VY]));
      vertexBuffer.put(toFixed32(a[VZ]));

      // vertex B
        
//        gl.glColor4f(br, bg, bb, b[A]);
      colorBuffer.put(toFixed16(br));
      colorBuffer.put(toFixed16(bg));
      colorBuffer.put(toFixed16(bb));
      colorBuffer.put(toFixed16(b[A]));

//        gl.glTexCoord2f(b[U] * uscale, b[V] * vscale);
      if (textureIndex != -1) {
        textureBuffer.put(toFixed32(b[U] * uscale));
        textureBuffer.put(toFixed32(b[V] * vscale));
      }

//        gl.glNormal3f(b[NX], b[NY], b[NZ]);
      normalBuffer.put(toFixed32(b[NX]));
      normalBuffer.put(toFixed32(b[NY]));
      normalBuffer.put(toFixed32(b[NZ]));

//        gl.glEdgeFlag(a[EDGE] == 1);
        
//        gl.glVertex3f(b[VX], b[VY], b[VZ]);
      vertexBuffer.put(toFixed32(b[VX]));
      vertexBuffer.put(toFixed32(b[VY]));
      vertexBuffer.put(toFixed32(b[VZ]));

      // vertex C
        
//      gl.glColor4f(cr, cg, cb, c[A]);
      colorBuffer.put(toFixed16(cr));
      colorBuffer.put(toFixed16(cg));
      colorBuffer.put(toFixed16(cb));
      colorBuffer.put(toFixed16(c[A]));

//      gl.glTexCoord2f(c[U] * uscale, c[V] * vscale);
      if (textureIndex != -1) {
        textureBuffer.put(toFixed32(c[U] * uscale));
        textureBuffer.put(toFixed32(c[V] * vscale));
      }
      
//      gl.glNormal3f(c[NX], c[NY], c[NZ]);
      normalBuffer.put(toFixed32(c[NX]));
      normalBuffer.put(toFixed32(c[NY]));
      normalBuffer.put(toFixed32(c[NZ]));
      
//      gl.glEdgeFlag(a[EDGE] == 1);
      
//      gl.glVertex3f(c[VX], c[VY], c[VZ]);
      vertexBuffer.put(toFixed32(c[VX]));
      vertexBuffer.put(toFixed32(c[VY]));
      vertexBuffer.put(toFixed32(c[VZ]));

//      gl.glEnd();
      gl.glVertexPointer(3, GL10.GL_FIXED, 0, vertexBuffer);
      gl.glColorPointer(4, GL10.GL_FIXED, 0, colorBuffer);
      gl.glNormalPointer(GL10.GL_FIXED, 3, normalBuffer);
      gl.glDrawArrays(GL10.GL_TRIANGLES, 0, 3);        

      report("non-binding 6");
      if (textureIndex != -1) {
        gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
        gl.glDisable(GL10.GL_TEXTURE_2D);
      }
    }
    gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
    gl.glDisableClientState(GL10.GL_COLOR_ARRAY);
    gl.glDisableClientState(GL10.GL_NORMAL_ARRAY);

    triangleCount = 0;
    report("render_triangles out");
  }


  //protected void rawTriangles(int start, int stop)  // PGraphics3D


  protected void bindTexture(PImage texture) {
    ImageCache cash = (ImageCache) texture.getCache(this);  // as in johnny
    if (cash == null) {
      cash = new ImageCache();
      texture.setCache(this, cash);
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
      gl.glBindTexture(GL10.GL_TEXTURE_2D, cash.tindex);
    }
  }


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
      if (textureDeleteQueue.length == textureDeleteQueueCount) {
        textureDeleteQueue = (int[]) PApplet.expand(textureDeleteQueue);
      }
      if (tindex != -1) {
        textureDeleteQueue[textureDeleteQueueCount++] = tindex;
      }
    }


    /**
     * Generate a texture ID and do the necessary bitshifting for the image.
     */
    public void rebind(PImage source) {
      if (textureDeleteQueueCount != 0) {
        //gl.glDeleteTextures(1, new int[] { tindex }, 0);
        gl.glDeleteTextures(textureDeleteQueueCount, textureDeleteQueue, 0);
        textureDeleteQueueCount = 0;
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
        gl.glGetIntegerv(GL10.GL_MAX_TEXTURE_SIZE, maxSize, 0);
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

      gl.glBindTexture(GL10.GL_TEXTURE_2D, tindex);

      gl.glPixelStorei(GL10.GL_UNPACK_ALIGNMENT, 1);
      //gl.glPixelStorei(GL10.GL_UNPACK_SWAP_BYTES, 0);

      gl.glTexImage2D(GL10.GL_TEXTURE_2D, 0, 4, twidth, theight,
                      //0, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, cash.tpixels);
                      0, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, tbuffer);

      gl.glTexParameterf(GL10.GL_TEXTURE_2D,
                         //GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_NEAREST);
                         GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
      gl.glTexParameterf(GL10.GL_TEXTURE_2D,
                         //GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST);
                         GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);

      //

//      /*int err =*/ glu.gluBuild2DMipmaps(GL10.GL_TEXTURE_2D, 4,
//                                          twidth, theight,
//                                          GL10.GL_RGBA,
//                                          GL10.GL_UNSIGNED_BYTE, tbuffer);
//      //System.out.println("mipmap: " + err);

      // The MAG_FILTER should only be GL_LINEAR or GL_NEAREST.
      // Some cards are OK with LINEAR_MIPMAP_LINEAR, but not the
      // Radeon 9700, which is in all the PB G4s.. Not sure if this
      // is an OpenGL version thing, tho it makes sense MIN_FILTER
      // is the only one that uses mipmapping.
      gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER,
                         GL10.GL_LINEAR);
      gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER,
                         GL10.GL_LINEAR_MIPMAP_LINEAR);

//      gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP);
//      gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP);
      gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
      gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);

      //

      gl.glTexEnvf(GL10.GL_TEXTURE_ENV, GL10.GL_TEXTURE_ENV_MODE, GL10.GL_MODULATE);
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

  // RENDERING


  //public void flush()


  //protected void render()


  //protected void sort()



  //////////////////////////////////////////////////////////////

  // POINT, LINE, TRIANGLE, QUAD

  // Because vertex(x, y) is mapped to vertex(x, y, 0), none of these commands
  // need to be overridden from their default implementation in PGraphics.


  //public void point(float x, float y)


  //public void point(float x, float y, float z)


  //public void line(float x1, float y1, float x2, float y2)


  //public void line(float x1, float y1, float z1,
  //                 float x2, float y2, float z2)


  //public void triangle(float x1, float y1, float x2, float y2,
  //                     float x3, float y3)


  //public void quad(float x1, float y1, float x2, float y2,
  //                 float x3, float y3, float x4, float y4)



  //////////////////////////////////////////////////////////////

  // RECT


  //public void rectMode(int mode)


  //public void rect(float a, float b, float c, float d)


  //protected void rectImpl(float x1, float y1, float x2, float y2)



  //////////////////////////////////////////////////////////////

  // ELLIPSE


  //public void ellipseMode(int mode)


  //public void ellipse(float a, float b, float c, float d)

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
      gl.glNewList(ellipseList, GL10.GL_COMPILE);
      gl.glBegin(GL10.GL_LINE_LOOP);
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


  //public void arc(float a, float b, float c, float d,
  //                float start, float stop)


  //protected void arcImpl(float x, float y, float w, float h,
  //                       float start, float stop)



  //////////////////////////////////////////////////////////////

  // BOX

  // TODO P3D overrides box to turn on triangle culling, but that's a waste
  // for OpenGL10. Also could just use the cube method from GL or GLUT.


  //public void box(float size)


  //public void box(float w, float h, float d)  // P3D



  //////////////////////////////////////////////////////////////

  // SPHERE

  // TODO P3D overrides sphere to turn on triangle culling, but that's a waste
  // for OpenGL10. Also could just use the cube method from GL or GLUT.


  //public void sphereDetail(int res)


  //public void sphereDetail(int ures, int vres)


  //public void sphere(float r)



  //////////////////////////////////////////////////////////////

  // BEZIER


  //public float bezierPoint(float a, float b, float c, float d, float t)


  //public float bezierTangent(float a, float b, float c, float d, float t)


  //public void bezierDetail(int detail)


  //public void bezier(float x1, float y1,
  //                   float x2, float y2,
  //                   float x3, float y3,
  //                   float x4, float y4)


  //public void bezier(float x1, float y1, float z1,
  //                   float x2, float y2, float z2,
  //                   float x3, float y3, float z3,
  //                   float x4, float y4, float z4)



  //////////////////////////////////////////////////////////////

  // CATMULL-ROM CURVES


  //public float curvePoint(float a, float b, float c, float d, float t)


  //public float curveTangent(float a, float b, float c, float d, float t)


  //public void curveDetail(int detail)


  //public void curveTightness(float tightness)


  //public void curve(float x1, float y1,
  //                  float x2, float y2,
  //                  float x3, float y3,
  //                  float x4, float y4)


  //public void curve(float x1, float y1, float z1,
  //                  float x2, float y2, float z2,
  //                  float x3, float y3, float z3,
  //                  float x4, float y4, float z4)



  //////////////////////////////////////////////////////////////

  // SMOOTH


  public void smooth() {
    smooth = true;
    if (hints[DISABLE_OPENGL_2X_SMOOTH]) {
      //gl.glEnable(GL10.GL_MULTISAMPLE);
      gl.glEnable(GL10.GL_POINT_SMOOTH);
      gl.glEnable(GL10.GL_LINE_SMOOTH);
//      gl.glEnable(GL10.GL_POLYGON_SMOOTH);  // OpenGL ES
    }
  }


  public void noSmooth() {
    smooth = false;
    if (hints[DISABLE_OPENGL_2X_SMOOTH]) {
      //gl.glDisable(GL10.GL_MULTISAMPLE);
      gl.glDisable(GL10.GL_POINT_SMOOTH);
      gl.glDisable(GL10.GL_LINE_SMOOTH);
//      gl.glDisable(GL10.GL_POLYGON_SMOOTH);  // OpenGL ES
    }
  }



  //////////////////////////////////////////////////////////////

  // IMAGES


  //public void imageMode(int mode)


  //public void image(PImage image, float x, float y)


  //public void image(PImage image, float x, float y, float c, float d)


  //public void image(PImage image,
  //                  float a, float b, float c, float d,
  //                  int u1, int v1, int u2, int v2)


  //protected void imageImpl(PImage image,
  //                         float x1, float y1, float x2, float y2,
  //                         int u1, int v1, int u2, int v2)



  //////////////////////////////////////////////////////////////

  // SHAPE


  //public void shapeMode(int mode)


  //public void shape(PShape shape)


  //public void shape(PShape shape, float x, float y)


  //public void shape(PShape shape, float x, float y, float c, float d)



  //////////////////////////////////////////////////////////////

  // TEXT SETTINGS


  //public void textAlign(int align)


  //public void textAlign(int alignX, int alignY)


  //public float textAscent()


  //public float textDescent()


  //public void textFont(PFont which)


  //public void textFont(PFont which, float size)


  //public void textLeading(float leading)


  //public void textMode(int mode)


  //protected boolean textModeCheck(int mode)


  //public void textSize(float size)


  //public float textWidth(char c)


  //public float textWidth(String str)


  //protected float textWidthImpl(char buffer[], int start, int stop)



  //////////////////////////////////////////////////////////////

  // TEXT

  // None of the variations of text() are overridden from PGraphics.



  //////////////////////////////////////////////////////////////

  // TEXT IMPL


  //protected void textLineAlignImpl(char buffer[], int start, int stop,
  //                                 float x, float y)


  //protected void textLineImpl(char buffer[], int start, int stop,
  //                            float x, float y)


  //protected void textCharImpl(char ch, float x, float y)


//  public class TessCallback extends GLUtessellatorCallbackAdapter {
//    public void begin(int type) {
//      switch (type) {
//      case GL10.GL_TRIANGLE_FAN: beginShape(TRIANGLE_FAN); break;
//      case GL10.GL_TRIANGLE_STRIP: beginShape(TRIANGLE_STRIP); break;
//      case GL10.GL_TRIANGLES: beginShape(TRIANGLES); break;
//      }
//    }
//
//    public void end() {
//      //gl.glEnd();
//      endShape();
//    }
//
//    public void edge(boolean e) {
//      PGraphicsOpenGL.this.edge(e);
//    }
//
//    public void vertex(Object data) {
//      if (data instanceof double[]) {
//        double[] d = (double[]) data;
//        if (d.length != 3) {
//          throw new RuntimeException("TessCallback vertex() data " +
//                                     "isn't length 3");
//        }
//        //System.out.println("tess callback vertex " +
//        //                 d[0] + " " + d[1] + " " + d[2]);
//        //vertexRedirect((float) d[0], (float) d[1], (float) d[2]);
//        PGraphicsOpenGL.this.vertex((float) d[0], (float) d[1], (float) d[2]);
//        /*
//        if (d.length == 6) {
//          double[] d2 = {d[0], d[1], d[2]};
//          gl.glVertex3dv(d2);
//          d2 = new double[]{d[3], d[4], d[5]};
//          gl.glColor3dv(d2);
//        } else if (d.length == 3) {
//          gl.glVertex3dv(d);
//        }
//        */
//      } else {
//        throw new RuntimeException("TessCallback vertex() data not understood");
//      }
//    }
//
//    public void error(int errnum) {
//      String estring = glu.gluErrorString(errnum);
//      PGraphics.showWarning("Tessellation Error: " + estring);
//    }
//
//    /**
//     * Implementation of the GLU_TESS_COMBINE callback.
//     * @param coords is the 3-vector of the new vertex
//     * @param data is the vertex data to be combined, up to four elements.
//     * This is useful when mixing colors together or any other
//     * user data that was passed in to gluTessVertex.
//     * @param weight is an array of weights, one for each element of "data"
//     * that should be linearly combined for new values.
//     * @param outData is the set of new values of "data" after being
//     * put back together based on the weights. it's passed back as a
//     * single element Object[] array because that's the closest
//     * that Java gets to a pointer.
//     */
//    public void combine(double[] coords, Object[] data,
//                        float[] weight, Object[] outData) {
//      //System.out.println("coords.length = " + coords.length);
//      //System.out.println("data.length = " + data.length);
//      //System.out.println("weight.length = " + weight.length);
//      //for (int i = 0; i < data.length; i++) {
//      //System.out.println(i + " " + data[i].getClass().getName() + " " + weight[i]);
//      //}
//
//      double[] vertex = new double[coords.length];
//      vertex[0] = coords[0];
//      vertex[1] = coords[1];
//      vertex[2] = coords[2];
//      //System.out.println("combine " +
//      //                 vertex[0] + " " + vertex[1] + " " + vertex[2]);
//
//      // this is just 3, so nothing interesting to bother combining
//      //System.out.println("data length " + ((double[]) data[0]).length);
//
//      // not gonna bother doing any combining,
//      // since no user data is being passed in.
//      /*
//      for (int i = 3; i < 6; i++) {
//        vertex[i] =
//          weight[0] * ((double[]) data[0])[i] +
//          weight[1] * ((double[]) data[1])[i] +
//          weight[2] * ((double[]) data[2])[i] +
//          weight[3] * ((double[]) data[3])[i];
//      }
//      */
//      outData[0] = vertex;
//    }
//  }



  //////////////////////////////////////////////////////////////

  // MATRIX MATH

  //public void pushMatrix()
  //public void popMatrix()

  //public void translate(float tx, float ty)
  //public void translate(float tx, float ty, float tz)
  //public void rotate(float angle)
  //public void rotateX(float angle)
  //public void rotateY(float angle)
  //public void rotateZ(float angle)
  //public void rotate(float angle, float vx, float vy, float vz)
  //public void scale(float s)
  //public void scale(float sx, float sy)
  //public void scale(float x, float y, float z)

  //public void resetMatrix()
  //public void applyMatrix(PMatrix2D source)
  //public void applyMatrix(float n00, float n01, float n02,
  //                        float n10, float n11, float n12)
  //public void applyMatrix(PMatrix3D source)
  //public void applyMatrix(float n00, float n01, float n02, float n03,
  //                        float n10, float n11, float n12, float n13,
  //                        float n20, float n21, float n22, float n23,
  //                        float n30, float n31, float n32, float n33)

  //public getMatrix(PMatrix2D target)
  //public getMatrix(PMatrix3D target)
  //public void setMatrix(PMatrix2D source)
  //public void setMatrix(PMatrix3D source)
  //public void printMatrix()

  //public void beginCamera()
  //public void endCamera()
  //public void camera()
  //public void camera(float eyeX, float eyeY, float eyeZ,
  //                   float centerX, float centerY, float centerZ,
  //                   float upX, float upY, float upZ)
  //public void printCamera()

  //public void ortho()
  //public void ortho(float left, float right,
  //                  float bottom, float top,
  //                  float near, float far)
  //public void perspective()
  //public void perspective(float fov, float aspect, float near, float far)
  //public void frustum(float left, float right,
  //                    float bottom, float top,
  //                    float near, float far)
  //public void printProjection()

  //public float screenX(float x, float y)
  //public float screenY(float x, float y)
  //public float screenX(float x, float y, float z)
  //public float screenY(float x, float y, float z)
  //public float screenZ(float x, float y, float z)
  //public float modelX(float x, float y, float z)
  //public float modelY(float x, float y, float z)
  //public float modelZ(float x, float y, float z)



  //////////////////////////////////////////////////////////////

  // STYLES


  //public void pushStyle()
  //public void popStyle()
  //public void style(PStyle)
  //public PStyle getStyle()
  //public void getStyle(PStyle)



  //////////////////////////////////////////////////////////////

  // COLOR MODE


  //public void colorMode(int mode)
  //public void colorMode(int mode, float max)
  //public void colorMode(int mode, float mx, float my, float mz);
  //public void colorMode(int mode, float mx, float my, float mz, float ma);



  //////////////////////////////////////////////////////////////

  // COLOR CALC


  //protected void colorCalc(int rgb)
  //protected void colorCalc(int rgb, float alpha)
  //protected void colorCalc(float gray)
  //protected void colorCalc(float gray, float alpha)
  //protected void colorCalc(float x, float y, float z)
  //protected void colorCalc(float x, float y, float z, float a)
  //protected void colorCalcARGB(int argb, float alpha)



  //////////////////////////////////////////////////////////////

  // STROKE CAP/JOIN/WEIGHT


  public void strokeWeight(float weight) {
    this.strokeWeight = weight;
  }


  public void strokeJoin(int join) {
    if (join != DEFAULT_STROKE_JOIN) {
      showMethodWarning("strokeJoin");
    }
  }


  public void strokeCap(int cap) {
    if (cap != DEFAULT_STROKE_CAP) {
      showMethodWarning("strokeCap");
    }
  }



  //////////////////////////////////////////////////////////////

  // STROKE, TINT, FILL


  //public void noStroke()
  //public void stroke(int rgb)
  //public void stroke(int rgb, float alpha)
  //public void stroke(float gray)
  //public void stroke(float gray, float alpha)
  //public void stroke(float x, float y, float z)
  //public void stroke(float x, float y, float z, float a)
  //protected void strokeFromCalc()

  //public void noTint()
  //public void tint(int rgb)
  //public void tint(int rgb, float alpha)
  //public void tint(float gray)
  //public void tint(float gray, float alpha)
  //public void tint(float x, float y, float z)
  //public void tint(float x, float y, float z, float a)
  //protected void tintFromCalc()

  //public void noFill()
  //public void fill(int rgb)
  //public void fill(int rgb, float alpha)
  //public void fill(float gray)
  //public void fill(float gray, float alpha)
  //public void fill(float x, float y, float z)
  //public void fill(float x, float y, float z, float a)


  protected void fillFromCalc() {
    super.fillFromCalc();
    calcColorBuffer();
    gl.glMaterialfv(GL10.GL_FRONT_AND_BACK, GL10.GL_AMBIENT_AND_DIFFUSE,
                    colorFloats, 0);
  }



  //////////////////////////////////////////////////////////////

  // MATERIAL PROPERTIES


//  public void ambient(int rgb) {
//    super.ambient(rgb);
//    calcColorBuffer();
//    gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_AMBIENT, colorBuffer, 0);
//  }


//  public void ambient(float gray) {
//    super.ambient(gray);
//    calcColorBuffer();
//    gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_AMBIENT, colorBuffer, 0);
//  }


//  public void ambient(float x, float y, float z) {
//    super.ambient(x, y, z);
//    calcColorBuffer();
//    gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_AMBIENT, colorBuffer, 0);
//  }


  protected void ambientFromCalc() {
    super.ambientFromCalc();
    calcColorBuffer();
    gl.glMaterialfv(GL10.GL_FRONT_AND_BACK, GL10.GL_AMBIENT, colorFloats, 0);
  }


//  public void specular(int rgb) {
//    super.specular(rgb);
//    calcColorBuffer();
//    gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_SPECULAR, colorBuffer, 0);
//  }


//  public void specular(float gray) {
//    super.specular(gray);
//    calcColorBuffer();
//    gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_SPECULAR, colorBuffer, 0);
//  }


//  public void specular(float x, float y, float z) {
//    super.specular(x, y, z);
//    calcColorBuffer();
//    gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_SPECULAR, colorBuffer, 0);
//  }


  protected void specularFromCalc() {
    super.specularFromCalc();
    calcColorBuffer();
    gl.glMaterialfv(GL10.GL_FRONT_AND_BACK, GL10.GL_SPECULAR, colorFloats, 0);
  }


  public void shininess(float shine) {
    super.shininess(shine);
    gl.glMaterialf(GL10.GL_FRONT_AND_BACK, GL10.GL_SHININESS, shine);
  }


//  public void emissive(int rgb) {
//    super.emissive(rgb);
//    calcColorBuffer();
//    gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_EMISSION, colorBuffer, 0);
//  }


//  public void emissive(float gray) {
//    super.emissive(gray);
//    calcColorBuffer();
//    gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_EMISSION, colorBuffer, 0);
//  }


//  public void emissive(float x, float y, float z) {
//    super.emissive(x, y, z);
//    calcColorBuffer();
//    gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_EMISSION, colorBuffer, 0);
//  }


  protected void emissiveFromCalc() {
    super.emissiveFromCalc();
    calcColorBuffer();
    gl.glMaterialfv(GL10.GL_FRONT_AND_BACK, GL10.GL_EMISSION, colorFloats, 0);
  }



  //////////////////////////////////////////////////////////////

  // LIGHTING

  // We're not actually turning on GL lights right now
  // because our home-grown ones work better for now.


//  public void lights() {
//    super.lights();
//    gl.glEnable(GL.GL_LIGHTING);
//  }


//  public void noLights() {
//    super.noLights();
//    gl.glDisable(GL.GL_LIGHTING);
//  }


  public void ambientLight(float r, float g, float b) {
    super.ambientLight(r, g, b);
    glLightEnable(lightCount - 1);
    glLightAmbient(lightCount - 1);
    glLightPosition(lightCount - 1);
    glLightFalloff(lightCount - 1);
  }

  public void ambientLight(float r, float g, float b,
                           float x, float y, float z) {
    super.ambientLight(r, g, b, x, y, z);
    glLightEnable(lightCount - 1);
    glLightAmbient(lightCount - 1);
    glLightPosition(lightCount - 1);
    glLightFalloff(lightCount - 1);
  }


  public void directionalLight(float r, float g, float b,
                               float nx, float ny, float nz) {
    super.directionalLight(r, g, b, nx, ny, nz);
    glLightEnable(lightCount - 1);
    glLightNoAmbient(lightCount - 1);
    glLightDirection(lightCount - 1);
    glLightDiffuse(lightCount - 1);
    glLightSpecular(lightCount - 1);
    glLightFalloff(lightCount - 1);
  }


  public void pointLight(float r, float g, float b,
                         float x, float y, float z) {
    super.pointLight(r, g, b, x, y, z);
    glLightEnable(lightCount - 1);
    glLightNoAmbient(lightCount - 1);
    glLightPosition(lightCount - 1);
    glLightDiffuse(lightCount - 1);
    glLightSpecular(lightCount - 1);
    glLightFalloff(lightCount - 1);
  }


  public void spotLight(float r, float g, float b,
                        float x, float y, float z,
                        float nx, float ny, float nz,
                        float angle, float concentration) {
    super.spotLight(r, g, b, x, y, z, nx, ny, nz, angle, concentration);
    glLightNoAmbient(lightCount - 1);
    glLightPosition(lightCount - 1);
    glLightDirection(lightCount - 1);
    glLightDiffuse(lightCount - 1);
    glLightSpecular(lightCount - 1);
    glLightFalloff(lightCount - 1);
    glLightSpotAngle(lightCount - 1);
    glLightSpotConcentration(lightCount - 1);
  }


  public void lightFalloff(float constant, float linear, float quadratic) {
    super.lightFalloff(constant, linear, quadratic);
    glLightFalloff(lightCount);
  }


  public void lightSpecular(float x, float y, float z) {
    super.lightSpecular(x, y, z);
    glLightSpecular(lightCount);
  }


  protected void lightPosition(int num, float x, float y, float z) {
    super.lightPosition(num, x, y, z);
    glLightPosition(num);
  }


  protected void lightDirection(int num, float x, float y, float z) {
    super.lightDirection(num, x, y, z);
    glLightDirection(num);
  }


  private void glLightAmbient(int num) {
    gl.glLightfv(GL10.GL_LIGHT0 + num,
                 GL10.GL_AMBIENT, lightDiffuse[num], 0);
  }


  private void glLightNoAmbient(int num) {
    gl.glLightfv(GL10.GL_LIGHT0 + num,
                 GL10.GL_AMBIENT, zeroFloats, 0);
  }


  private void glLightDiffuse(int num) {
    gl.glLightfv(GL10.GL_LIGHT0 + num,
                 GL10.GL_DIFFUSE, lightDiffuse[num], 0);
  }


  private void glLightDirection(int num) {
    if (lightType[num] == DIRECTIONAL) {
      // TODO this expects a fourth arg that will be set to 1
      //      this is why lightBuffer is length 4,
      //      and the [3] element set to 1 in the constructor.
      //      however this may be a source of problems since
      //      it seems a bit "hack"
      gl.glLightfv(GL10.GL_LIGHT0 + num, GL10.GL_POSITION,
                   lightNormal[num].array(), 0);
    } else {  // spotlight
      // this one only needs the 3 arg version
      gl.glLightfv(GL10.GL_LIGHT0 + num, GL10.GL_SPOT_DIRECTION,
                   lightNormal[num].array(), 0);
    }
  }


  private void glLightEnable(int num) {
    gl.glEnable(GL10.GL_LIGHT0 + num);
  }


  private void glLightFalloff(int num) {
    gl.glLightf(GL10.GL_LIGHT0 + num,
                GL10.GL_CONSTANT_ATTENUATION, lightFalloffConstant[num]);
    gl.glLightf(GL10.GL_LIGHT0 + num,
                GL10.GL_LINEAR_ATTENUATION, lightFalloffLinear[num]);
    gl.glLightf(GL10.GL_LIGHT0 + num,
                GL10.GL_QUADRATIC_ATTENUATION, lightFalloffQuadratic[num]);
  }


  private void glLightPosition(int num) {
    gl.glLightfv(GL10.GL_LIGHT0 + num, 
                 GL10.GL_POSITION, lightPosition[num].array(), 0);
  }


  private void glLightSpecular(int num) {
    gl.glLightfv(GL10.GL_LIGHT0 + num, GL10.GL_SPECULAR, lightSpecular[num], 0);
  }


  private void glLightSpotAngle(int num) {
    gl.glLightf(GL10.GL_LIGHT0 + num,
                GL10.GL_SPOT_CUTOFF, lightSpotAngle[num]);
  }


  private void glLightSpotConcentration(int num) {
    gl.glLightf(GL10.GL_LIGHT0 + num,
                GL10.GL_SPOT_EXPONENT, lightSpotConcentration[num]);
  }



  //////////////////////////////////////////////////////////////

  // BACKGROUND


  protected void backgroundImpl(PImage image) {
    gl.glClearColor(backgroundR, backgroundG, backgroundB, 1);
    gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
    set(0, 0, image);
  }


  protected void backgroundImpl() {
    gl.glClearColor(backgroundR, backgroundG, backgroundB, 1);
    gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
  }



  //////////////////////////////////////////////////////////////

  // COLOR MODE

  // colorMode() is inherited from PGraphics.



  //////////////////////////////////////////////////////////////

  // COLOR CALC

  // This is the OpenGL complement to the colorCalc() methods.


  /**
   * Load the calculated color into a pre-allocated array so that
   * it can be quickly passed over to OpenGL.
   */
  private final void calcColorBuffer() {
    if (colorFloats == null) {
//      colorBuffer = BufferUtil.newFloatBuffer(4);
      colorFloats = new float[4];
    }
    colorFloats[0] = calcR;
    colorFloats[1] = calcG;
    colorFloats[2] = calcB;
    colorFloats[3] = calcA;
//    colorBuffer.put(0, calcR);
//    colorBuffer.put(1, calcG);
//    colorBuffer.put(2, calcB);
//    colorBuffer.put(3, calcA);
//    colorBuffer.rewind();
  }



  //////////////////////////////////////////////////////////////

  // COLOR METHODS

  //public final int color(int gray)
  //public final int color(int gray, int alpha)
  //public final int color(int rgb, float alpha)
  //public final int color(int x, int y, int z)

  //public final float alpha(int what)
  //public final float red(int what)
  //public final float green(int what)
  //public final float blue(int what)
  //public final float hue(int what)
  //public final float saturation(int what)
  //public final float brightness(int what)

  //public int lerpColor(int c1, int c2, float amt)
  //static public int lerpColor(int c1, int c2, float amt, int mode)



  //////////////////////////////////////////////////////////////

  // BEGINRAW/ENDRAW

  // beginRaw, endRaw() both inherited.



  //////////////////////////////////////////////////////////////

  // WARNINGS and EXCEPTIONS

  // showWarning() and showException() available from PGraphics.


  /**
   * Report on anything from glError().
   * Don't use this inside glBegin/glEnd otherwise it'll
   * throw an GL_INVALID_OPERATION error.
   */
  public void report(String where) {
    if (!hints[DISABLE_OPENGL_ERROR_REPORT]) {
      int err = gl.glGetError();
      if (err != 0) {
        String errString = GLU.gluErrorString(err);
        String msg = "OpenGL error " + err + " at " + where + ": " + errString;
        PGraphics.showWarning(msg);
      }
    }
  }



  //////////////////////////////////////////////////////////////

  // RENDERER SUPPORT QUERIES


  //public boolean displayable()


  //public boolean dimensional()  // from P3D



  //////////////////////////////////////////////////////////////

  // PIMAGE METHODS

  // getImage
  // setCache, getCache, removeCache
  // isModified, setModified



  //////////////////////////////////////////////////////////////

  // LOAD/UPDATE PIXELS


  public void loadPixels() {
    if ((pixels == null) || (pixels.length != width*height)) {
      pixels = new int[width * height];
      pixelBuffer = BufferUtil.newIntBuffer(pixels.length);
//      pixelBuffer = IntBuffer.allocate(pixels.length);
    }

    gl.glReadPixels(0, 0, width, height,
                    GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, pixelBuffer);
    pixelBuffer.get(pixels);
    pixelBuffer.rewind();

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

    // When height is an odd number, the middle line needs to be
    // endian swapped, but not y-swapped.
    // http://dev.processing.org/bugs/show_bug.cgi?id=944
    if ((height % 2) == 1) {
      index = (height / 2) * width;
      if (BIG_ENDIAN) {
        for (int x = 0; x < width; x++) {
          // ignores alpha component, just sets it opaque
          pixels[index] = 0xff000000 | ((pixels[index] >> 8)  & 0x00ffffff);
        }
      } else {
        for (int x = 0; x < width; x++) {
          pixels[index] = 0xff000000 |
            ((pixels[index] << 16) & 0xff0000) |
             (pixels[index] & 0xff00) |
            ((pixels[index] >> 16) & 0xff);
        }
      }
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
    // TODO fix me for android
//    gl.glDrawPixels(width, height,
//                    GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, pixelBuffer);
  }



  //////////////////////////////////////////////////////////////

  // RESIZE


  public void resize(int wide, int high) {
    PGraphics.showMethodWarning("resize");
  }



  //////////////////////////////////////////////////////////////

  // GET/SET


//  IntBuffer getsetBuffer = IntBuffer.allocate(1);
  IntBuffer getsetBuffer = BufferUtil.newIntBuffer(1);
//  int getset[] = new int[1];

  public int get(int x, int y) {
    gl.glReadPixels(x, y, 1, 1, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, getsetBuffer);
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


  //public PImage get(int x, int y, int w, int h)


  protected PImage getImpl(int x, int y, int w, int h) {
    PImage newbie = new PImage(w, h); //new int[w*h], w, h, ARGB);

    //IntBuffer newbieBuffer = BufferUtil.newIntBuffer(w*h);
    IntBuffer newbieBuffer = IntBuffer.allocate(w * h);
    gl.glReadPixels(x, y, w, h, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, newbieBuffer);
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
    // TODO whither drawPixels? 
//    gl.glDrawPixels(1, 1, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, getsetBuffer);
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
    int[] backup = new int[source.pixels.length];
    System.arraycopy(source.pixels, 0, backup, 0, source.pixels.length);
    javaToNativeARGB(source);

    // TODO is this possible without intbuffer?
    IntBuffer setBuffer = BufferUtil.newIntBuffer(source.pixels.length);
    setBuffer.put(source.pixels);
    setBuffer.rewind();

    setRasterPos(x, (height-y) - source.height); //+source.height);
//    gl.glDrawPixels(source.width, source.height,
//                    GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, setBuffer);
    source.pixels = backup;
  }


  // TODO remove the implementation above and use setImpl instead,
  // since it'll be more efficient
  // http://dev.processing.org/bugs/show_bug.cgi?id=943
  //protected void setImpl(int dx, int dy, int sx, int sy, int sw, int sh,
  //                       PImage src)


  /**
   * Definitive method for setting raster pos, including offscreen locations.
   * The raster position is tricky because it's affected by the modelview and
   * projection matrices. Further, offscreen coords won't properly set the
   * raster position. This code gets around both issues.
   * http://www.mesa3d.org/brianp/sig97/gotchas.htm
   * @param y the Y-coordinate, which is flipped upside down in OpenGL
   */
  protected void setRasterPos(float x, float y) {
//    float z = 0;
//    float w = 1;
//
//    float fx, fy;
//
//    // Push current matrix mode and viewport attributes
//    gl.glPushAttrib(GL.GL_TRANSFORM_BIT | GL.GL_VIEWPORT_BIT);
//
//    // Setup projection parameters
//    gl.glMatrixMode(GL.GL_PROJECTION);
//    gl.glPushMatrix();
//    gl.glLoadIdentity();
//    gl.glMatrixMode(GL.GL_MODELVIEW);
//    gl.glPushMatrix();
//    gl.glLoadIdentity();
//
//    gl.glDepthRange(z, z);
//    gl.glViewport((int) x - 1, (int) y - 1, 2, 2);
//
//    // set the raster (window) position
//    fx = x - (int) x;
//    fy = y - (int) y;
//    gl.glRasterPos4f(fx, fy, 0, w);
//
//    // restore matrices, viewport and matrix mode
//    gl.glPopMatrix();
//    gl.glMatrixMode(GL.GL_PROJECTION);
//    gl.glPopMatrix();
//
//    gl.glPopAttrib();
  }



  //////////////////////////////////////////////////////////////

  // MASK


  public void mask(int alpha[]) {
    PGraphics.showMethodWarning("mask");
  }


  public void mask(PImage alpha) {
    PGraphics.showMethodWarning("mask");
  }



  //////////////////////////////////////////////////////////////

  // FILTER


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
   * Extremely slow and not optimized, should use GL methods instead.
   * Currently calls a beginPixels() on the whole canvas, then does the copy,
   * then it calls endPixels().
   */
  //public void copy(int sx1, int sy1, int sx2, int sy2,
  //                 int dx1, int dy1, int dx2, int dy2)


  /**
   * TODO - extremely slow and not optimized.
   * Currently calls a beginPixels() on the whole canvas,
   * then does the copy, then it calls endPixels().
   */
  //public void copy(PImage src,
  //                 int sx1, int sy1, int sx2, int sy2,
  //                 int dx1, int dy1, int dx2, int dy2)



  //////////////////////////////////////////////////////////////

  // BLEND


  //static public int blendColor(int c1, int c2, int mode)


//  public void blend(PImage src,
//                    int sx, int sy, int dx, int dy, int mode) {
//    set(dx, dy, PImage.blendColor(src.get(sx, sy), get(dx, dy), mode));
//  }


  /**
   * Extremely slow and not optimized, should use GL methods instead.
   * Currently calls a beginPixels() on the whole canvas, then does the copy,
   * then it calls endPixels(). Please help fix:
   * <A HREF="http://dev.processing.org/bugs/show_bug.cgi?id=941">Bug 941</A>,
   * <A HREF="http://dev.processing.org/bugs/show_bug.cgi?id=942">Bug 942</A>.
   */
//  public void blend(int sx1, int sy1, int sx2, int sy2,
//                    int dx1, int dy1, int dx2, int dy2, int mode) {
//    loadPixels();
//    super.blend(sx1, sy1, sx2, sy2, dx1, dy1, dx2, dy2, mode);
//    updatePixels();
//  }


//  public void blend(PImage src,
//                    int sx1, int sy1, int sx2, int sy2,
//                    int dx1, int dy1, int dx2, int dy2, int mode) {
//    loadPixels();
//    super.blend(src, sx1, sy1, sx2, sy2, dx1, dy1, dx2, dy2, mode);
//    updatePixels();
//  }



  //////////////////////////////////////////////////////////////

  // SAVE


  //public void save(String filename)  // PImage calls loadPixels()



  //////////////////////////////////////////////////////////////

  // INTERNAL MATH


  protected final float clamp(float a) {
    return (a < 1) ? a : 1;
  }  
}


class BufferUtil {
  static IntBuffer newIntBuffer(int big) {
    IntBuffer buffer = IntBuffer.allocate(big);
    buffer.rewind();
    return buffer;
  }
}
