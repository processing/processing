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

GSMovie mov;
GLTexture tex;

int fcount, lastm;
float frate;
int fint = 3;

void setup() {
  size(1280, 800, GLConstants.GLGRAPHICS);
  frameRate(90);
  
  mov = new GSMovie(this, "movie.avi");
  
  // Use texture tex as the destination for the movie pixels.
  tex = new GLTexture(this);
  mov.setPixelDest(tex);
  
  // This is the size of the buffer where frames are stored
  // when they are not rendered quickly enough.
  tex.setPixelBufferSize(10);
  // New frames put into the texture when the buffer is full
  // are deleted forever, so this could lead dropeed frames:
  tex.delPixelsWhenBufferFull(false);
  // Otherwise, they are kept by gstreamer and will be sent
  // again later. This avoids loosing any frames, but increases 
  // the memory used by the application.
  
  mov.loop();
  
  background(0);
  noStroke();
}

void draw() {
  // Using the available() method and reading the new frame inside draw()
  // instead of movieEvent() is the most effective way to keep the 
  // audio and video synchronization.
  if (mov.available()) {
    mov.read();
    // putPixelsIntoTexture() copies the frame pixels to the OpenGL texture
    // encapsulated by 
    if (tex.putPixelsIntoTexture()) {
      
      // Calculating height to keep aspect ratio.      
      float h = width * tex.height / tex.width;
      float b = 0.5 * (height - h);

      image(tex, 0, b, width, h);
      
      String info = "Resolution: " + mov.width + "x" + mov.height +
                    " , framerate: " + nfc(frate, 2) + 
                    " , number of buffered frames: " + tex.getPixelBufferUse();
        
      fill(0);
      rect(0, 0, textWidth(info), b);
      fill(255);
      text(info, 0, 15);

      fcount += 1;
      int m = millis();
      if (m - lastm > 1000 * fint) {
        frate = float(fcount) / fint;
        fcount = 0;
        lastm = m; 
      }      
    }
  }
}
