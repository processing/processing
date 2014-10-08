package processing.core;

import java.awt.Color;
import java.awt.Frame;


public interface PSurface {
  // Background default needs to be different from the default value in
  // PGraphics.backgroundColor, otherwise size(100, 100) bg spills over.
  // https://github.com/processing/processing/issues/2297
  static final Color WINDOW_BGCOLOR = new Color(0xDD, 0xDD, 0xDD);

  public Frame initFrame(int width, int height, Color backgroundColor,
                         int deviceIndex, boolean fullScreen, boolean spanDisplays);

  public void placeSketch();

  // sets displayWidth/Height inside PApplet
  public void checkDisplaySize();

  public void setFrameRate(float fps);

  // called on the first frame so that the now-visible drawing surface can
  // receive key and mouse events
  public void requestFocus();

  // finish rendering to the screen
  public void blit();


}