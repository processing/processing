package processing.android.core;

public class GLModelParameters implements GLConstants, PConstants {
  public GLModelParameters() {
    updateMode = STATIC;    
    drawMode= POINTS;
  }

   public GLModelParameters(GLModelParameters src) {
    updateMode = src.updateMode;    
    drawMode= src.drawMode;
  }

  public int updateMode;  
  public int drawMode;
}
