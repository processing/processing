package processing.glw;

import processing.opengl.PGL;
import processing.opengl.PGraphicsOpenGL;

public class PGraphics2D extends processing.opengl.PGraphics2D {
//  protected boolean windowed = false;
  protected PGL createPGL(PGraphicsOpenGL pg) {
    return new PNEWT(pg);
  }  
  
//  public void requestDraw() {
//    System.out.println("requesting draw");
//    if (primarySurface || windowed) {
//      if (initialized) {
//        ((PNEWT)pgl).update(sized);
//      } else {
//        initPrimary();
//      }
//    }
//  }   
}
