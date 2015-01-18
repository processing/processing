package processing.lwjgl;

import processing.core.PSurface;
import processing.opengl.PGL;
import processing.opengl.PGraphicsOpenGL;

public class PGraphics2D extends processing.opengl.PGraphics2D {
  protected PGL createPGL(PGraphicsOpenGL pg) {
    return new PLWJGL(pg);
  }  
  @Override
  public PSurface createSurface() {  // ignore
    return new PSurfaceLWJGL(this);
  }  
}
