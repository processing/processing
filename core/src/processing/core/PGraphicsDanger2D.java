package processing.core;


public class PGraphicsDanger2D extends PGraphicsJava2D {
  // doesn't exist/not necessary because Java2D will do this automatically
  //static final boolean HIDPI = true;
  //static final boolean HIDPI = false;

  public PGraphicsDanger2D() {
//    if (HIDPI) {
//      pixelFactor = 2;
//    }
  }

  @Override
  public PSurface createSurface() {
    return surface = new PSurfaceDanger(this);
  }


  @Override
  public void beginDraw() {
    //g2 = checkImage();  // already set g2

//    if (HIDPI) {
//      g2.scale(2, 2);
//    }

    // Calling getGraphics() seems to nuke the smoothing settings
    smooth(quality);

    checkSettings();
    resetMatrix(); // reset model matrix
    vertexCount = 0;
  }
}