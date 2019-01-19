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

import java.awt.Image;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import com.apple.eawt.Application;


/**
 * Deal with issues related to thinking differently.
 *
 * We have to register a quit handler to safely shut down the sketch,
 * otherwise OS X will just kill the sketch when a user hits Cmd-Q.
 * In addition, we have a method to set the dock icon image so we look more
 * like a native application.
 *
 * This is a stripped-down version of what's in processing.app.platform to fix
 * <a href="https://github.com/processing/processing/issues/3301">3301</a>.
 */
public class ThinkDifferent {

  // http://developer.apple.com/documentation/Java/Reference/1.4.2/appledoc/api/com/apple/eawt/Application.html
  private static Application application;

  // True if user has tried to quit once. Prevents us from canceling the quit
  // call if the sketch is held up for some reason, like an exception that's
  // managed to put the sketch in a bad state.
  static boolean attemptedQuit;


  static public void init(final PApplet sketch) {
    if (application == null) {
      application = Application.getApplication();
    }

    setHandler(application, "setQuitHandler", (proxy, method, args) -> {
      sketch.exit();
      if (PApplet.uncaughtThrowable == null &&  // no known crash
          !attemptedQuit) {  // haven't tried yet
        args[1].getClass().getMethod("cancelQuit").invoke(args[1]);  // tell OS X we'll handle this
        attemptedQuit = true;
      } else {
        args[1].getClass().getMethod("performQuit").invoke(args[1]);  // just force it this time
      }
      return null;
    });
  }

  /**
   * Sets a handler on an instance of {@link Application}, taking into account JDK version
   * differences.
   *
   * @param app an instance of {@link Application}
   * @param name the "set handler" method name
   * @param handler the handler
   */
  private static void setHandler(Application app, String name, InvocationHandler handler) {
    // Determine which version of com.apple.eawt.Application to use and pass it a handler of the
    // appropriate type
    Method[] methods = app.getClass().getMethods();
    for (Method m : methods) {
      if (!name.equals(m.getName())) {
        continue;
      }
      if (m.getParameterCount() != 1) {
        continue;
      }
      Class paramType = m.getParameterTypes()[0];
      try {
        // Allow a null handler
        Object proxy = null;
        if (handler != null) {
          proxy = Proxy.newProxyInstance(
              paramType.getClassLoader(), new Class<?>[] { paramType }, handler);
        }
        m.invoke(app, proxy);
      } catch (IllegalArgumentException ex) {
        // TODO: Print error?: method doesn't take an interface, etc.
      } catch (IllegalAccessException ex) {
        // TODO: Print error?: Other method invocation problem
      } catch (InvocationTargetException ex) {
        ex.getCause().printStackTrace();
        // TODO: Print ex.getCause() a different way?
      }
      break;
    }
  }

  static public void cleanup() {
    if (application == null) {
      application = Application.getApplication();
    }
    setHandler(application, "setQuitHandler", null);
  }

  // Called via reflection from PSurfaceAWT and others
  static public void setIconImage(Image image) {
    // When already set, is a sun.awt.image.MultiResolutionCachedImage on OS X
//    Image current = application.getDockIconImage();
//    System.out.println("current dock icon image is " + current);
//    System.out.println("changing to " + image);

    application.setDockIconImage(image);
  }


  // Instead, just use Application.getApplication() inside your app
//  static public Application getApplication() {
//    return application;
//  }
}
