package processing.glw;

import processing.opengl.PGL;
import processing.opengl.PGraphicsOpenGL;

public class PGraphics2D extends processing.opengl.PGraphics2D {
  protected PGL createPGL(PGraphicsOpenGL pg) {
    return new PNEWT(pg);
  }   
}
