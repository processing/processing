package processing.android.core;

public class GLModelParameters implements GLConstants, PConstants {
  GLModelParameters() {
    updateMode = STATIC;    
    drawMode= POINTS;
    pointSize = 1;
    lineWidth = 1;
  }

   GLModelParameters(GLModelParameters src) {
    updateMode = src.updateMode;    
    drawMode= src.drawMode;
    pointSize = src.pointSize;
    lineWidth = src.lineWidth;
  }

  public int updateMode;  
  public int drawMode;
  public int pointSize;
  public int lineWidth;
}
