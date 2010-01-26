package processing.android.core;

public class GLModelParameters implements GLConstants, PConstants {
  GLModelParameters() {
    updateMode = STATIC;    
    drawMode= POINTS;
  }

   GLModelParameters(GLModelParameters src) {
    updateMode = src.updateMode;    
    drawMode= src.drawMode;
  }

  public int updateMode;  
  public int drawMode;
}
