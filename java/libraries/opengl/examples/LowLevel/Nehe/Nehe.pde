// Nehe by Andres Colubri
// Example of direct OpenGL use inside Processing with the 
// OPENGL2 renderer.
// Ported from NeHe tutorial 8:
// http://nehe.gamedev.net/data/lessons/lesson.asp?lesson=08

import javax.media.opengl.*;
import java.nio.*;
import javax.media.opengl.glu.gl2.GLUgl2;

boolean lighting = true;
boolean blending = true;
boolean depthTest = true;
boolean depthMask = false;
boolean texturing = true;

int selBlend = 1;
int selFilter = 0;
float transparency = 0.5f;

// The color depth of the sketch can be set with this
// method. The 6 numbers separated by colons correspond 
// to the red, green, blue, alpha, depth and stencil bits.
// If this method is not defined, then Processing will let
// OpenGL to automatically choose the color depth.
String sketchColordepth() {
  return "8:8:8:8:16:0";
}

// Whether the sketch surface supports translucenty or not.
boolean sketchTranslucency() {
  return true;
} 

FloatBuffer[] cubeVertexBfr;
FloatBuffer[] cubeNormalBfr;
FloatBuffer[] cubeTextureBfr;

FloatBuffer lightAmbBfr;
FloatBuffer lightDifBfr;
FloatBuffer lightPosBfr;

private IntBuffer texturesBuffer;

private float xRot;
private float yRot;
float xSpeed = 0.2f;
float ySpeed = 0.2f;  

GLUgl2 glu;

void setup() {
  size(400, 400, P3D);

  glu = new GLUgl2();

  int SIZEOF_FLOAT = Float.SIZE / 8;

  cubeVertexBfr = new FloatBuffer[6];
  cubeNormalBfr = new FloatBuffer[6];
  cubeTextureBfr = new FloatBuffer[6];
  for (int i = 0; i < 6; i++) {
    ByteBuffer vbb = ByteBuffer.allocateDirect(4 * 3 * SIZEOF_FLOAT);
    vbb.order(ByteOrder.nativeOrder());
    cubeVertexBfr[i] = vbb.asFloatBuffer(); 
    cubeVertexBfr[i].put(cubeVertexCoords[i]);
    cubeVertexBfr[i].flip();

    ByteBuffer nbb = ByteBuffer.allocateDirect(4 * 3 * SIZEOF_FLOAT);
    nbb.order(ByteOrder.nativeOrder());
    cubeNormalBfr[i] = nbb.asFloatBuffer(); 
    cubeNormalBfr[i].put(cubeNormalCoords[i]);
    cubeNormalBfr[i].flip();

    ByteBuffer tbb = ByteBuffer.allocateDirect(4 * 2 * SIZEOF_FLOAT);
    tbb.order(ByteOrder.nativeOrder());      
    cubeTextureBfr[i] = tbb.asFloatBuffer();
    cubeTextureBfr[i].put(cubeTextureCoords[i]);
    cubeTextureBfr[i].flip();
  }

  lightAmbBfr = FloatBuffer.wrap(lightAmb);
  lightDifBfr = FloatBuffer.wrap(lightDif);
  lightPosBfr = FloatBuffer.wrap(lightPos);    

  PGraphicsOpenGL pgl = (PGraphicsOpenGL)g;
  GL gl = pgl.beginGL();

  Texture teximage = null;
  try {
    teximage = readTexture("glass.bmp");
  } catch (IOException e) {
    e.printStackTrace();
    throw new RuntimeException(e);
  }

  texturesBuffer = IntBuffer.allocate(3);
  gl.glGenTextures(3, texturesBuffer);

  gl.glEnable(GL.GL_TEXTURE_2D);
  // setup texture 0 with nearest filtering
  gl.glBindTexture(GL.GL_TEXTURE_2D, texturesBuffer.get(0));
  gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_NEAREST);
  gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST);
  gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, GL.GL_CLAMP_TO_EDGE);
  gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, GL.GL_CLAMP_TO_EDGE);
  makeRGBTexture(gl, glu, teximage, GL.GL_TEXTURE_2D, false);

  // setup texture 1 with linear filtering for both minification and magnification, 
  // this is usually called bilinear sampling
  gl.glBindTexture(GL.GL_TEXTURE_2D, texturesBuffer.get(1));
  gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
  gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);
  gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, GL.GL_CLAMP_TO_EDGE);
  gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, GL.GL_CLAMP_TO_EDGE);
  makeRGBTexture(gl, glu, teximage, GL.GL_TEXTURE_2D, false);

  // setup texture 2 with linear filtering for magnification and linear-linear mipmapping
  // (trilinear sampling)
  gl.glBindTexture(GL.GL_TEXTURE_2D, texturesBuffer.get(2));
  gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
  gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR_MIPMAP_NEAREST);
  gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, GL.GL_CLAMP_TO_EDGE);
  gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, GL.GL_CLAMP_TO_EDGE);
  makeRGBTexture(gl, glu, teximage, GL.GL_TEXTURE_2D, true);
  gl.glDisable(GL.GL_TEXTURE_2D);

  pgl.endGL();
}

public void draw() {
  background(0);

  PGraphicsOpenGL pgl = (PGraphicsOpenGL)g;
  GL gl = pgl.beginGL();

  pgl.gl2f.glShadeModel(GL2.GL_SMOOTH);
  gl.glClearColor(0, 0, 0, 0);

  if (depthTest) {
    gl.glClearDepthf(1.0f);
    gl.glEnable(GL.GL_DEPTH_TEST);
    gl.glDepthFunc(GL.GL_LEQUAL);
  } 
  else {
    gl.glDisable(GL.GL_DEPTH_TEST);
  }
  gl.glDepthMask(depthMask);

  gl.glHint(GL2.GL_PERSPECTIVE_CORRECTION_HINT, GL.GL_NICEST);

  // lighting
  gl.glEnable(GL2.GL_LIGHT0);
  pgl.gl2f.glLightfv(GL2.GL_LIGHT0, GL2.GL_AMBIENT, lightAmbBfr);
  pgl.gl2f.glLightfv(GL2.GL_LIGHT0, GL2.GL_DIFFUSE, lightDifBfr);
  pgl.gl2f.glLightfv(GL2.GL_LIGHT0, GL2.GL_POSITION, lightPosBfr);

  // blending
  gl.glEnable(GL2.GL_COLOR_MATERIAL);
  pgl.gl2f.glColor4f(1.0f, 1.0f, 1.0f, transparency);
  if (selBlend == 0) {
    gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE);
  } 
  else if (selBlend == 1) {
    gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
  }

  gl.glViewport(0, 0, width, height);
  // setup projection matrix
  pgl.gl2f.glMatrixMode(GL2.GL_PROJECTION);
  pgl.gl2f.glLoadIdentity();
  glu.gluPerspective(45.0f, (float)width / (float)height, 1.0f, 100.0f);    

  gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
  pgl.gl2f.glMatrixMode(GL2.GL_MODELVIEW);
  pgl.gl2f.glLoadIdentity(); 

  // update lighting
  if (lighting) {
    gl.glEnable(GL2.GL_LIGHTING);
  } 
  else {
    gl.glDisable(GL2.GL_LIGHTING);
  }

  // update blending
  if (blending) {
    gl.glEnable(GL.GL_BLEND);
    gl.glDisable(GL.GL_CULL_FACE);
  } 
  else {
    gl.glDisable(GL.GL_BLEND);
    gl.glEnable(GL.GL_CULL_FACE);
  }

  pgl.gl2f.glTranslatef(0, 0, -6);
  pgl.gl2f.glRotatef(xRot, 1, 0, 0);
  pgl.gl2f.glRotatef(yRot, 0, 1, 0);

  if (texturing) {
    gl.glEnable(GL.GL_TEXTURE_2D);
    gl.glBindTexture(GL.GL_TEXTURE_2D, texturesBuffer.get(selFilter));
  }
  pgl.gl2f.glEnableClientState(GL2.GL_VERTEX_ARRAY);
  pgl.gl2f.glEnableClientState(GL2.GL_NORMAL_ARRAY);
  if (texturing) pgl.gl2f.glEnableClientState(GL2.GL_TEXTURE_COORD_ARRAY);
  for (int i = 0; i < 6; i++) // draw each face
  {
    pgl.gl2f.glVertexPointer(3, GL.GL_FLOAT, 0, cubeVertexBfr[i]);
    if (texturing) pgl.gl2f.glTexCoordPointer(2, GL.GL_FLOAT, 0, cubeTextureBfr[i]);
    pgl.gl2f.glNormalPointer(GL.GL_FLOAT, 0, cubeNormalBfr[i]);
    pgl.gl2f.glDrawArrays(GL.GL_TRIANGLE_FAN, 0, 4);
  }
  pgl.gl2f.glDisableClientState(GL2.GL_VERTEX_ARRAY);
  pgl.gl2f.glDisableClientState(GL2.GL_NORMAL_ARRAY);
  if (texturing) {
    pgl.gl2f.glDisableClientState(GL2.GL_TEXTURE_COORD_ARRAY);
    gl.glDisable(GL.GL_TEXTURE_2D);
  }

  // update rotations
  xRot += xSpeed;
  yRot += ySpeed;

  pgl.endGL();
}

void makeRGBTexture(GL gl, GLUgl2 glu, Texture img, int target, boolean mipmapped) {
  if (mipmapped) {
    glu.gluBuild2DMipmaps(target, GL.GL_RGB8, img.getWidth(), img.getHeight(), GL.GL_RGB, GL.GL_UNSIGNED_BYTE, img.getPixels());
  } else {
    gl.glTexImage2D(target, 0, GL.GL_RGB, img.getWidth(), img.getHeight(), 0, GL.GL_RGB, GL.GL_UNSIGNED_BYTE, img.getPixels());
  }
}

