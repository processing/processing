// Nehe by Andres Colubri
// Example of direct OpenGL use inside Processing in Android mode.
// Ported from NeHe tutorial 8:
// http://nehe.gamedev.net/data/lessons/lesson.asp?lesson=08

import javax.microedition.khronos.opengles.*;
import android.opengl.*;
import android.graphics.*;
import java.nio.*;

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

void setup() {
  size(480, 800, P3D);
  orientation(PORTRAIT);  

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

  PGraphicsAndroid3D a3d = (PGraphicsAndroid3D)g;
  GL10 gl = a3d.beginGL();    

  texturesBuffer = IntBuffer.allocate(3);
  a3d.gl.glGenTextures(3, texturesBuffer);

  InputStream stream = createInput("glass.bmp");
  Bitmap bitmap = BitmapFactory.decodeStream(stream);
  try {
    stream.close();
    stream = null;
  } 
  catch (IOException e) {
  }

  gl.glEnable(GL10.GL_TEXTURE_2D);
  // setup texture 0 with nearest filtering
  gl.glBindTexture(GL10.GL_TEXTURE_2D, texturesBuffer.get(0));
  gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_NEAREST);
  gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST);
  gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
  gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);
  GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bitmap, 0);

  // setup texture 1 with linear filtering for both minification and magnification, 
  // this is usually called bilinear sampling
  gl.glBindTexture(GL10.GL_TEXTURE_2D, texturesBuffer.get(1));
  gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
  gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
  gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
  gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);
  GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bitmap, 0);

  // setup texture 2 with linear filtering for magnification and linear-linear mipmapping
  // (trilinear sampling)
  gl.glBindTexture(GL10.GL_TEXTURE_2D, texturesBuffer.get(2));
  gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
  gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR_MIPMAP_NEAREST);
  gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
  gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);
  GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bitmap, 0);
  gl.glDisable(GL10.GL_TEXTURE_2D);

  a3d.endGL();
}

public void draw() {
  background(0);

  PGraphicsAndroid3D a3d = (PGraphicsAndroid3D)g;
  GL10 gl = a3d.beginGL();

  gl.glShadeModel(GL10.GL_SMOOTH);
  gl.glClearColor(0, 0, 0, 0);

  if (depthTest) {
    gl.glClearDepthf(1.0f);
    gl.glEnable(GL10.GL_DEPTH_TEST);
    gl.glDepthFunc(GL10.GL_LEQUAL);
  } 
  else {
    gl.glDisable(GL10.GL_DEPTH_TEST);
  }
  gl.glDepthMask(depthMask);

  gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_NICEST);

  // lighting
  gl.glEnable(GL10.GL_LIGHT0);
  gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_AMBIENT, lightAmbBfr);
  gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_DIFFUSE, lightDifBfr);
  gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_POSITION, lightPosBfr);

  // blending
  gl.glEnable(GL10.GL_COLOR_MATERIAL);
  gl.glColor4f(1.0f, 1.0f, 1.0f, transparency);
  if (selBlend == 0) {
    gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE);
  } 
  else if (selBlend == 1) {
    gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
  }

  gl.glViewport(0, 0, width, height);
  // setup projection matrix
  gl.glMatrixMode(GL10.GL_PROJECTION);
  gl.glLoadIdentity();
  GLU.gluPerspective(gl, 45.0f, (float)width / (float)height, 1.0f, 100.0f);    

  gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
  gl.glMatrixMode(GL10.GL_MODELVIEW);
  gl.glLoadIdentity(); 

  // update lighting
  if (lighting) {
    gl.glEnable(GL10.GL_LIGHTING);
  } 
  else {
    gl.glDisable(GL10.GL_LIGHTING);
  }

  // update blending
  if (blending) {
    gl.glEnable(GL10.GL_BLEND);
    gl.glDisable(GL10.GL_CULL_FACE);
  } 
  else {
    gl.glDisable(GL10.GL_BLEND);
    gl.glEnable(GL10.GL_CULL_FACE);
  }

  gl.glTranslatef(0, 0, -6);
  gl.glRotatef(xRot, 1, 0, 0);
  gl.glRotatef(yRot, 0, 1, 0);

  if (texturing) {
    gl.glEnable(GL10.GL_TEXTURE_2D);
    gl.glBindTexture(GL10.GL_TEXTURE_2D, texturesBuffer.get(selFilter));
  }
  gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
  gl.glEnableClientState(GL10.GL_NORMAL_ARRAY);
  if (texturing) gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
  for (int i = 0; i < 6; i++) // draw each face
  {
    gl.glVertexPointer(3, GL10.GL_FLOAT, 0, cubeVertexBfr[i]);
    if (texturing) gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, cubeTextureBfr[i]);
    gl.glNormalPointer(GL10.GL_FLOAT, 0, cubeNormalBfr[i]);
    gl.glDrawArrays(GL10.GL_TRIANGLE_FAN, 0, 4);
  }
  gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
  gl.glDisableClientState(GL10.GL_NORMAL_ARRAY);
  if (texturing) {
    gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
    gl.glDisable(GL10.GL_TEXTURE_2D);
  }

  // update rotations
  xRot += xSpeed;
  yRot += ySpeed;

  a3d.endGL();
}

