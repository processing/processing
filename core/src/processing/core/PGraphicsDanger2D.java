package processing.core;


public class PGraphicsDanger2D extends PGraphicsJava2D {


  @Override
  public PSurface createSurface() {
    return surface = new PSurfaceDanger(this);
  }


  @Override
  public void beginDraw() {
    //g2 = checkImage();  // already set g2

    // Calling getGraphics() seems to nuke the smoothing settings
    smooth(quality);

    checkSettings();
    resetMatrix(); // reset model matrix
    vertexCount = 0;
  }
}