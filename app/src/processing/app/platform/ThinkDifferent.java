/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2012-2013 The Processing Foundation
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

import javax.swing.*;

import processing.app.About;
import processing.app.Base;
import processing.app.Toolkit;
import processing.app.Language;

import com.apple.eawt.*;


/**
 * Deal with issues related to thinking differently. This handles the basic
 * Mac OS X menu commands (and apple events) for open, about, prefs, etc.
 *
 * Based on OSXAdapter.java from Apple DTS.
 *
 * As of 0140, this code need not be built on platforms other than OS X,
 * because of the new platform structure which isolates through reflection.
 *
 * This suppresses deprecation warnings because to use the new code, all users
 * would be forced to use Java Update 3 on OS X 10.6, and Java Update 8 on
 * OS X 10.5, which doesn't seem likely at the moment.
 */
@SuppressWarnings("deprecation")
public class ThinkDifferent implements ApplicationListener {

  // pseudo-singleton model; no point in making multiple instances
  // of the EAWT application or our adapter
  private static ThinkDifferent adapter;
  // http://developer.apple.com/documentation/Java/Reference/1.4.2/appledoc/api/com/apple/eawt/Application.html
  private static Application application;

  // reference to the app where the existing quit, about, prefs code is
  private Base base;


  static protected void init(Base base) {
    if (application == null) {
      //application = new com.apple.eawt.Application();
      application = Application.getApplication();
    }
    if (adapter == null) {
      adapter = new ThinkDifferent(base);
    }
    application.addApplicationListener(adapter);
    application.setEnabledAboutMenu(true);
    application.setEnabledPreferencesMenu(true);

    // Set the menubar to be used when nothing else is open. http://j.mp/dkZmka
    // Only available since Java for Mac OS X 10.6 Update 1, but removed
    // dynamic loading code because that should be installed in 10.6.8, and
    // we may be dropped 10.6 really soon anyway
    
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
  

  public ThinkDifferent(Base base) {
    this.base = base;
  }


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


  // implemented handler methods.  These are basically hooks into existing
  // functionality from the main app, as if it came over from another platform.
  public void handleAbout(ApplicationEvent ae) {
    if (base != null) {
      ae.setHandled(true);
      new About(null);
    } else {
      throw new IllegalStateException("handleAbout: Base instance detached from listener");
    }
  }


  public void handlePreferences(ApplicationEvent ae) {
    if (base != null) {
      base.handlePrefs();
      ae.setHandled(true);
    } else {
      throw new IllegalStateException("handlePreferences: Base instance detached from listener");
    }
  }


  public void handleOpenApplication(ApplicationEvent ae) {
  }


  public void handleOpenFile(ApplicationEvent ae) {
//    System.out.println("got open file event " + ae.getFilename());
    String filename = ae.getFilename();
    base.handleOpen(filename);
    ae.setHandled(true);
  }


  public void handlePrintFile(ApplicationEvent ae) {
    // TODO implement os x print handler here (open app, call handlePrint, quit)
  }


  public void handleQuit(ApplicationEvent ae) {
    if (base != null) {
      /*
      / You MUST setHandled(false) if you want to delay or cancel the quit.
      / This is important for cross-platform development -- have a universal quit
      / routine that chooses whether or not to quit, so the functionality is identical
      / on all platforms.  This example simply cancels the AppleEvent-based quit and
      / defers to that universal method.
      */
      boolean result = base.handleQuit();
      ae.setHandled(result);
    } else {
      throw new IllegalStateException("handleQuit: Base instance detached from listener");
    }
  }


  public void handleReOpenApplication(ApplicationEvent arg0) {
  }
}
