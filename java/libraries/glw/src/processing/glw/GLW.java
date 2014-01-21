package processing.glw;

import java.util.HashMap;

import com.jogamp.newt.opengl.GLWindow;

import processing.core.PGraphics;
import processing.opengl.PGraphicsOpenGL;

public class GLW {
  static public final String DUMMY  = "processing.glw.PGraphicsGLW";
  static public final String OPENGL = "processing.glw.PGraphicsGLW";
  static public final String P2D    = "processing.glw.PGraphics2D";
  static public final String P3D    = "processing.glw.PGraphics3D";
  
  static protected HashMap<PGraphics, GLWindow> windows = new HashMap<PGraphics, GLWindow>();
//  static protected HashMap<GLWindow, PGraphics> canvases;
  
  public GLW() {
    
  }
  
  static public void createWindow(PGraphics pg) {
    if (pg instanceof PGraphicsGLW || pg instanceof PGraphics2D || pg instanceof PGraphics3D) {
      //PGraphicsOpenGL pgopengl = (PGraphicsOpenGL)pg;
      //PNEWT pgl = (PNEWT)pgopengl.pgl;
      //GLWindow win = pgl.createWindow(pg.width, pg.height, /*PNEWT.getWindow().getContext(), */pgopengl);
      

//      windows.put(pg, win);
      windows.put(pg, null);
      //canvases.put(win, pg);
      //win.setTitle("NEWT window " + windows.size());
      
//      if (pg instanceof PGraphicsGLW) {
//        PGraphicsGLW pgw = (PGraphicsGLW)pg;
//        pgw.windowed = true;        
//      } else if (pg instanceof PGraphics2D) {
//        PGraphics2D pgw = (PGraphics2D)pg;
//        pgw.windowed = true;        
//      } else if (pg instanceof PGraphics3D) {
//        PGraphics3D pgw = (PGraphics3D)pg;
//        pgw.windowed = true;      
//      }
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
