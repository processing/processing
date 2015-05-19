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

import com.apple.eawt.AppEvent.QuitEvent;
import com.apple.eawt.Application;
import com.apple.eawt.QuitHandler;
import com.apple.eawt.QuitResponse;


/**
 * Deal with issues related to thinking differently. This handles the basic
 * Mac OS X menu commands (and apple events) for open, about, prefs, etc.
 *
 * This is a stripped-down version of what's in processing.app.platform to fix
 * <a href="https://github.com/processing/processing/issues/3301">3301</a>.
 */
public class ThinkDifferent {

  // pseudo-singleton model; no point in making multiple instances
  // of the EAWT application or our adapter
  private static ThinkDifferent adapter;
  // http://developer.apple.com/documentation/Java/Reference/1.4.2/appledoc/api/com/apple/eawt/Application.html
  private static Application application;

  // reference to the app where the existing quit, about, prefs code is
  //private Base base;


  static public void init(final PApplet sketch) {
    if (application == null) {
      application = Application.getApplication();
    }
    if (adapter == null) {
      adapter = new ThinkDifferent();  //base);
    }

    // Keeping these around in case we decide we want to add generic handlers
    // for these other features. Not sure how this affects JavaFX.
    /*
    application.setAboutHandler(new AboutHandler() {
      public void handleAbout(AboutEvent ae) {
        new About(null);
      }
    });

    application.setPreferencesHandler(new PreferencesHandler() {
      public void handlePreferences(PreferencesEvent arg0) {
        base.handlePrefs();
      }
    });

    application.setOpenFileHandler(new OpenFilesHandler() {
      public void openFiles(OpenFilesEvent event) {
        for (File file : event.getFiles()) {
          base.handleOpen(file.getAbsolutePath());
        }
      }
    });

    application.setPrintFileHandler(new PrintFilesHandler() {
      public void printFiles(PrintFilesEvent event) {
        // TODO not yet implemented
      }
    });
    */

    application.setQuitHandler(new QuitHandler() {
      public void handleQuitRequestWith(QuitEvent event, QuitResponse response) {
        sketch.exit();
        /*
        if (base.handleQuit()) {
          response.performQuit();
        } else {
          response.cancelQuit();
        }
        */
      }
    });

    /*
    // Set the menubar to be used when nothing else is open.
    JMenuBar defaultMenuBar = new JMenuBar();
    JMenu fileMenu = buildFileMenu(base);
    defaultMenuBar.add(fileMenu);
    // This is kind of a gross way to do this, but the alternatives? Hrm.
    Base.defaultFileMenu = fileMenu;

//    if (PApplet.javaVersion <= 1.6f) {  // doesn't work on Oracle's Java
    try {
      application.setDefaultMenuBar(defaultMenuBar);

    } catch (Exception e) {
      e.printStackTrace();  // oh well, never mind
    }
    */
  }
}
