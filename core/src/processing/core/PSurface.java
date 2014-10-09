package processing.core;

import java.awt.Color;
import java.awt.Frame;


public interface PSurface {
  /**
   * Minimum dimensions for the window holding an applet. This varies between
   * platforms, Mac OS X 10.3 (confirmed with 10.7 and Java 6) can do any
   * height but requires at least 128 pixels width. Windows XP has another
   * set of limitations. And for all I know, Linux probably allows window
   * sizes to be negative numbers.
   */
  static public final int MIN_WINDOW_WIDTH = 128;
  static public final int MIN_WINDOW_HEIGHT = 128;

  // Background default needs to be different from the default value in
  // PGraphics.backgroundColor, otherwise size(100, 100) bg spills over.
  // https://github.com/processing/processing/issues/2297
  static final Color WINDOW_BGCOLOR = new Color(0xDD, 0xDD, 0xDD);

  public Frame initFrame(PApplet sketch, Color backgroundColor,
                         int deviceIndex, boolean fullScreen, boolean spanDisplays);

  public void placeWindow(boolean external, int[] location, int[] editorLocation);

  //public void placeFullScreen(boolean hideStop);
  public void placePresent(Color stopColor);

  // Sketch is running from the PDE, set up messaging back to the PDE
  public void setupExternalMessages();

  // start the animation thread
  public void startThread();

  // sets displayWidth/Height inside PApplet
  //public void checkDisplaySize();

  public void setSize(int width, int height);

  public void setFrameRate(float fps);

  // called on the first frame so that the now-visible drawing surface can
  // receive key and mouse events
  public void requestFocus();

  // finish rendering to the screen
  public void blit();

  //

  public void setCursor(int kind);

  public void setCursor(PImage image, int hotspotX, int hotspotY);

  public void showCursor();

  public void hideCursor();

  //

}