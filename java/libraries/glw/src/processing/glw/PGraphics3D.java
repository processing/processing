package processing.glw;

import processing.opengl.PGL;
import processing.opengl.PGraphicsOpenGL;

public class PGraphics3D extends processing.opengl.PGraphics3D {
//  protected boolean windowed = false;
  
  protected PGL createPGL(PGraphicsOpenGL pg) {
    return new PNEWT(pg);
  }

//  public void requestDraw() {
//    if (primarySurface || windowed) {
//      if (initialized) {
//        ((PNEWT)pgl).update(sized);
//      } else {
//        initPrimary();
//      }
//    }
//  }    
}
