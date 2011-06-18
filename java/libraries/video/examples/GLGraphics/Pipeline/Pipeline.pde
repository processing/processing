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

GSPipeline pipeline;
GLTexture tex;

void setup() {
  size(320, 240, GLConstants.GLGRAPHICS);
  
  pipeline = new GSPipeline(this, "videotestsrc");
  
  // Use texture tex as the destination for the pipeline pixels. 
  tex = new GLTexture(this);
  pipeline.setPixelDest(tex); 
  pipeline.play();
}

void pipelineEvent(GSPipeline pipeline) {
  pipeline.read();
}

void draw() {
  // If there is a new frame available from the pipeline, the 
  // putPixelsIntoTexture() function will copy it to the
  // video card and will return true.  
  if (tex.putPixelsIntoTexture()) {
    image(tex, 0, 0, width, height);
  }
}
