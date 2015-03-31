/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2014 The Processing Foundation

  This library is free software; you can redistribute it and/or
  modify it under the terms of the GNU Lesser General Public
  License as published by the Free Software Foundation, version 2.1.

  This library is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  Lesser General Public License for more details.

  You should have received a copy of the GNU Lesser General
  Public License along with this library; if not, write to the
  Free Software Foundation, Inc., 59 Temple Place, Suite 330,
  Boston, MA  02111-1307  USA
*/

package processing.core;

import java.awt.Canvas;
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

  // renderer that doesn't draw to the screen
  public void initOffscreen();

  public Canvas initCanvas(PApplet sketch);

  public Frame initFrame(PApplet sketch, Color backgroundColor,
                         int deviceIndex, boolean fullScreen, boolean spanDisplays);

  //

  /** Set the window (and dock, or whatever necessary) title. */
  public void setTitle(String title);

  /** Show or hide the window. */
  public void setVisible(boolean visible);

  /** Set true if we want to resize things (default is not resizable) */
  public void setResizable(boolean resizable);

  //

  public void placeWindow(int[] location);

  public void placeWindow(int[] location, int[] editorLocation);

  //public void placeFullScreen(boolean hideStop);
  public void placePresent(Color stopColor);

  // Sketch is running from the PDE, set up messaging back to the PDE
  public void setupExternalMessages();

  //

  /** Start the animation thread */
  public void startThread();

  /**
   * On the next trip through the animation thread, things should go sleepy-bye.
   * Does not pause the thread immediately because that needs to happen on the
   * animation thread itself, so fires on the next trip through draw().
   */
  public void pauseThread();

  public void resumeThread();

  /**
   * Stop the animation thread (set it null)
   * @return false if already stopped
   */
  public boolean stopThread();

  public boolean isStopped();

  //

  // sets displayWidth/Height inside PApplet
  //public void checkDisplaySize();

  public void setSize(int width, int height);

  public void setSmooth(int level);

  public void setFrameRate(float fps);

  // called on the first frame so that the now-visible drawing surface can
  // receive key and mouse events
  public void requestFocus();

  // finish rendering to the screen (called by PApplet)
  public void blit();

  //

  public void setCursor(int kind);

  public void setCursor(PImage image, int hotspotX, int hotspotY);

  public void showCursor();

  public void hideCursor();

  //

}