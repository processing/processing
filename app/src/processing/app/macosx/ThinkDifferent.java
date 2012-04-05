/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2007-2010 Ben Fry and Casey Reas

  This program is free software; you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation, version 2.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software Foundation,
  Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package processing.app.macosx;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Method;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

import processing.app.Base;

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
    // http://developer.apple.com/mac/library/documentation/Java/Reference/
    //   JavaSE6_AppleExtensionsRef/api/com/apple/eawt/Application.html
    // Only available since Java for Mac OS X 10.6 Update 1, and
    // Java for Mac OS X 10.5 Update 6, so need to load this dynamically
    try {
      // com.apple.eawt.Application.setDefaultMenuBar(JMenuBar)
      Class<?> appClass = Application.class;
      Method method =
        appClass.getMethod("setDefaultMenuBar", new Class[] { JMenuBar.class });
      if (method != null) {
        JMenuBar defaultMenuBar = new JMenuBar();
        JMenu fileMenu = buildFileMenu(base);
        defaultMenuBar.add(fileMenu);
        method.invoke(application, new Object[] { defaultMenuBar });
        // This is kind of a gross way to do this, but the alternatives? Hrm.
        Base.defaultFileMenu = fileMenu;
      }
    } catch (Exception e) {
      e.printStackTrace();  // oh well nevermind
    }
  }


  public ThinkDifferent(Base base) {
    this.base = base;
  }


  /**
   * Gimpy file menu to be used on OS X when no sketches are open.
   */
  static protected JMenu buildFileMenu(final Base base) {
    JMenuItem item;
    JMenu fileMenu = new JMenu("File");

    item = Base.newJMenuItem("New", 'N');
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          base.handleNew();
        }
      });
    fileMenu.add(item);

    item = Base.newJMenuItem("Open...", 'O');
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          base.handleOpenPrompt();
        }
      });
    fileMenu.add(item);

    fileMenu.add(base.getSketchbookMenu());

//    fileMenu.add(base.nextEditorMode().getExamplesMenu());
    item = new JMenuItem("Examples...");
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
      base.handleAbout();
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