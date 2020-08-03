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

package processing.app.platform;

import java.awt.event.*;
import java.io.File;

import javax.swing.*;

import com.apple.eawt.*;
import com.apple.eawt.AppEvent.*;

import processing.app.*;
import processing.app.ui.About;
import processing.app.ui.Toolkit;


/**
 * Deal with issues related to thinking differently. This handles the basic
 * Mac OS X menu commands (and apple events) for open, about, prefs, etc.
 *
 * As of 0140, this code need not be built on platforms other than OS X,
 * because of the new platform structure which isolates through reflection.
 *
 * Rewritten for 0232 to remove deprecation issues, per the message
 * <a href="http://lists.apple.com/archives/java-dev/2012/Jan/msg00101.html">here</a>.
 * (We're able to do this now because we're dropping older Java versions.)
 */
public class ThinkDifferent {

  // pseudo-singleton model; no point in making multiple instances
  // of the EAWT application or our adapter
  private static ThinkDifferent adapter;
  // http://developer.apple.com/documentation/Java/Reference/1.4.2/appledoc/api/com/apple/eawt/Application.html
  private static Application application;

  // reference to the app where the existing quit, about, prefs code is
  //private Base base;


  static protected void init(final Base base) {
    if (application == null) {
      application = Application.getApplication();
    }
    if (adapter == null) {
      adapter = new ThinkDifferent();  //base);
    }
    
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
    
    application.setQuitHandler(new QuitHandler() {
      public void handleQuitRequestWith(QuitEvent event, QuitResponse response) {
        if (base.handleQuit()) {
          response.performQuit();
        } else {
          response.cancelQuit();
        }
      }
    });

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
//    } else {
//      // The douchebags at Oracle didn't feel that a working f*king menubar 
//      // on OS X was important enough to make it into the 7u40 release. 
//      //http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=8007267
//      // It languished in the JDK 8 source and has been backported for 7u60:
//      //http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=8022667
//      
//      JFrame offscreen = new JFrame();
//      offscreen.setUndecorated(true);
//      offscreen.setJMenuBar(defaultMenuBar);
//      Dimension screen = Toolkit.getScreenSize();
//      offscreen.setLocation(screen.width, screen.height);
//      offscreen.setVisible(true);
//    }
  }
  

//  public ThinkDifferent(Base base) {
//    this.base = base;
//  }


  /**
   * Gimpy file menu to be used on OS X when no sketches are open.
   */
  static protected JMenu buildFileMenu(final Base base) {
    JMenuItem item;
    JMenu fileMenu = new JMenu(Language.text("menu.file"));

    item = Toolkit.newJMenuItem(Language.text("menu.file.new"), 'N');
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          base.handleNew();
        }
      });
    fileMenu.add(item);

    item = Toolkit.newJMenuItem(Language.text("menu.file.open"), 'O');
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          base.handleOpenPrompt();
        }
      });
    fileMenu.add(item);

    item = Toolkit.newJMenuItemShift(Language.text("menu.file.sketchbook"), 'K');
    item.addActionListener(new ActionListener() {      
      @Override
      public void actionPerformed(ActionEvent e) {
        base.getNextMode().showSketchbookFrame();
      }
    });
    fileMenu.add(item);

    item = Toolkit.newJMenuItemShift(Language.text("menu.file.examples"), 'O');
    item.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        base.thinkDifferentExamples();
      }
    });
    fileMenu.add(item);

    return fileMenu;
  }
}
