// Using integration with GLGraphics for fast video playback.
// All the decoding stages, until the color conversion from YUV
// to RGB are handled by gstreamer, and the video frames are
// directly transfered over to the OpenGL texture encapsulated
// by the GLTexture object.
// You need the GLGraphics library (0.99+) to use this functionality:
// http://glgraphics.sourceforge.net/

import processing.opengl.*;
import codeanticode.glgraphics.*;
import codeanticode.gsvideo.*;

GSCapture cam;
GLTexture tex;

void setup() {
  size(640, 480, GLConstants.GLGRAPHICS);
  
  cam = new GSCapture(this, 640, 480);
  
  // Use texture tex as the destination for the camera pixels.
  tex = new GLTexture(this);
  cam.setPixelDest(tex);     
  cam.play();
  
  /*
  // You can get the resolutions supported by the
  // capture device using the resolutions() method.
  // It must be called after creating the capture 
  // object. 
  int[][] res = cam.resolutions();
  for (int i = 0; i < res.length; i++) {
    println(res[i][0] + "x" + res[i][1]);
  } 
  */
  
  /*
  // You can also get the framerates supported by the
  // capture device:
  String[] fps = cam.framerates();
  for (int i = 0; i < fps.length; i++) {
    println(fps[i]);
  } 
  */  
}

void captureEvent(GSCapture cam) {
  cam.read();
}

void draw() {
  // If there is a new frame available from the camera, the 
  // putPixelsIntoTexture() function will copy it to the
  // video card and will return true.
  if (tex.putPixelsIntoTexture()) {
    image(tex, 0, 0, width, height);
  }
}
