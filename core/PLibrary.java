/* -*- mode: jde; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  PLibrary - interface for classes that plug into bagel
  Part of the Processing project - http://processing.org

  Copyright (c) 2004 Ben Fry and the Processing project.

  This library is free software; you can redistribute it and/or
  modify it under the terms of the GNU Lesser General Public
  License as published by the Free Software Foundation; either
  version 2.1 of the License, or (at your option) any later version.

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

import java.awt.event.*;


/**
 * A series of messages can be registered via PApplet.registerCall().
 * For instance parent.registerCall(PRE) inside of the setup method 
 * will make sure that this library is informed each time beginFrame
 * has just been called yet no drawing has taken place (would you like 
 * to take over the camera, eh?)
 * 
 * An assumption for library writers is that they're fairly
 * technically savvy and familiar with Java. The primary target
 * audience for Procesing is less technical, so libraries are
 * designed to be simple to use but only slightly more complex
 * to write.
 */
public interface PLibrary {

  static final int SIZE       = 6;
  static final int PRE        = 0;
  static final int DRAW       = 1;
  static final int POST       = 2;
  static final int MOUSE      = 3;
  static final int KEY        = 4;
  static final int DISPOSE    = 5;

  static final int CALL_COUNT = 7;

  /**
   * This is called after the constructor on "attach"
   */
  public void setup(PApplet parent);

  /**
   * Called when the applet is resized.
   */
  public void size(int w, int h);

  /**
   * Called just after beginFrame() but before anything is 
   * drawn in the user's draw() method.
   */
  public void pre();

  /** 
   * Called before endFrame() but after all other drawing.
   */
  public void draw(); 

  /**
   * Called betwee endFrame() and the next beginFrame()
   * so that things can be post-processed based on the final, 
   * fully rendered, image.
   */
  public void post();

  /**
   * If registered, this will be called when a mouse event has occurred.
   * Use event.getID() to see whether it's MouseEvent.MOUSE_CLICKED or 
   * something else exciting. 
   */
  public void mouse(MouseEvent event);

  /**
   * A key event has occurred, use event.getID() to see whether it's
   * KeyEvent.KEY_PRESSED or whatever.
   */ 
  public void key(KeyEvent e);

  /**
   * Called when the applet or application is stopped.
   * Override this method to shut down your threads.
   *
   * Named dispose() instead of stop() since stop() is often
   * a useful method name for library functions (i.e. PMovie.stop()
   * which will stop a movie that is playing, but not kill off
   * its thread and associated resources.
   */
  public void dispose();
  //public void stop();
}
