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

GSMovie movie;
GLTexture tex;

void setup() {
  size(640, 480, GLConstants.GLGRAPHICS);
  background(0);
    
  movie = new GSMovie(this, "station.mov");
  
  // Use texture tex as the destination for the movie pixels.
  tex = new GLTexture(this);
  movie.setPixelDest(tex);  
  movie.loop();
}

void movieEvent(GSMovie movie) {
  movie.read();
}

void draw() {
  // If there is a new frame available from the movie, the 
  // putPixelsIntoTexture() function will copy it to the
  // video card and will return true.
  if (tex.putPixelsIntoTexture()) {
    tint(255, 20);
    image(tex, mouseX-movie.width/2, mouseY-movie.height/2);
  }
}
