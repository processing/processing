package processing.lwjgl;

import processing.core.PSurface;
import processing.opengl.PGL;
import processing.opengl.PGraphicsOpenGL;

public class PGraphics3D extends processing.opengl.PGraphics3D {
  protected PGL createPGL(PGraphicsOpenGL pg) {
    return new PLWJGL(pg);
  }
  @Override
  public PSurface createSurface() {  // ignore
    return new PSurfaceLWJGL(this);
  }  
}
