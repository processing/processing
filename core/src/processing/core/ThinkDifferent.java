/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2012-2014 The Processing Foundation
  Copyright (c) 2007-2012 Ben Fry and Casey Reas

  This program is free software; you can redistribute it and/or
  modify it under the terms of the GNU General Public License
  version 2, as published by the Free Software Foundation.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software Foundation,
  Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package processing.core;

import java.awt.*;


/**
 * Deal with issues related to Mac OS window behavior.
 *
 * We have to register a quit handler to safely shut down the sketch,
 * otherwise OS X will just kill the sketch when a user hits Cmd-Q.
 * In addition, we have a method to set the dock icon image so we look more
 * like a native desktop.
 *
 * This is a stripped-down version of what's in processing.app.platform to fix
 * <a href="https://github.com/processing/processing/issues/3301">3301</a>.
 */
public class ThinkDifferent {

  private static Desktop desktop;
  private static Taskbar taskbar;

  // True if user has tried to quit once. Prevents us from canceling the quit
  // call if the sketch is held up for some reason, like an exception that's
  // managed to put the sketch in a bad state.
  static boolean attemptedQuit;

  /**
   * Initialize the sketch with the quit handler.
   *
   * Initialize the sketch with the quit handler such that, if there is no known
   * crash, the application will not exit on its own if this is the first quit
   * attempt.
   *
   * @param sketch The sketch whose quit handler callback should be set.
   */
  static public void init(final PApplet sketch) {
    getDesktop().setQuitHandler((event, quitResponse) -> {
      sketch.exit();

      boolean noKnownCrash = PApplet.uncaughtThrowable == null;

      if (noKnownCrash && !attemptedQuit) {  // haven't tried yet
        quitResponse.cancelQuit();  // tell OS X we'll handle this
        attemptedQuit = true;
      } else {
        quitResponse.performQuit();  // just force it this time
      }
    });
  }

  /**
   * Remove the quit handler.
   */
  static public void cleanup() {
    getDesktop().setQuitHandler(null);
  }

  /**
   * Called via reflection from PSurfaceAWT and others, set the dock icon image.
   *
   * @param image The image to provide for Processing icon.
   */
  static public void setIconImage(Image image) {
    getTaskbar().setIconImage(image);
  }

  /**
   * Get the taskbar where OS visual settings can be provided.
   *
   * @return Cached taskbar singleton instance.
   */
  static private Taskbar getTaskbar() {
    if (taskbar == null) {
      taskbar = Taskbar.getTaskbar();
    }

    return taskbar;
  }

  /**
   * Get the desktop where OS behavior can be provided.
   *
   * @return Cached desktop singleton instance.
   */
  static private Desktop getDesktop() {
    if (desktop == null) {
      desktop = Desktop.getDesktop();
    }

    return desktop;
  }


  // Instead, just use Application.getApplication() inside your app
//  static public Application getApplication() {
//    return desktop;
//  }
}
