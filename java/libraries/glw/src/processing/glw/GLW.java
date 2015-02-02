package processing.glw;

import processing.core.PGraphics;

import com.jogamp.newt.opengl.GLWindow;

import java.util.HashMap;

public class GLW {
  static public final String RENDERER = "processing.glw.PGraphicsGLW";
  static public final String OPENGL   = "processing.glw.PGraphicsGLW";
  
  static public final String P2D      = "processing.glw.PGraphics2D";
  static public final String P3D      = "processing.glw.PGraphics3D";
  
  static protected HashMap<PGraphics, GLWindow> windows = 
      new HashMap<PGraphics, GLWindow>();
  
  public GLW() {    
  }
  
  static public void createWindow(PGraphics pg) {
    if (pg instanceof PGraphics2D || pg instanceof PGraphics3D) {
      windows.put(pg, null);
    } else {
      throw new RuntimeException("Only GLW.P2D or GLW.P3D surfaces can be attached to a window");
    }
  }
  
  static public GLWindow getWindow(PGraphics pg) {
    return windows.get(pg);
  }
  
  static public boolean isFocused(PGraphics pg) {
    GLWindow win = windows.get(pg);
    return win != null && win.hasFocus(); 
  }
  
  static public PGraphics getFocusedGraphics() {
    for (PGraphics pg: windows.keySet()) {
      if (isFocused(pg)) return pg;
    }
    return null;
  }
  
  static public GLWindow getFocusedWindow() {
    for (PGraphics pg: windows.keySet()) {
      if (isFocused(pg)) return windows.get(pg);
    }
    return null;
  } 
}
