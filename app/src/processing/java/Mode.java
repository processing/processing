/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2010 Ben Fry and Casey Reas

  This program is free software; you can redistribute it and/or modify
  it under the terms of the GNU General Public License version 2
  as published by the Free Software Foundation.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software Foundation,
  Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package processing.java;

import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.JOptionPane;

import processing.app.*;
import processing.app.syntax.PdeKeywords;
import processing.app.syntax.SyntaxStyle;
import processing.app.syntax.TokenMarker;
import processing.core.PApplet;
import processing.java.runner.Runner;


public class Mode {
  // these are static because they're used by Sketch
  static private File examplesFolder;
  static private File librariesFolder;
  static private File toolsFolder;

  ArrayList<LibraryFolder> coreLibraries;
  ArrayList<LibraryFolder> contribLibraries;

  // maps imported packages to their library folder
//  static HashMap<String, File> importToLibraryTable;
  static HashMap<String, LibraryFolder> importToLibraryTable;
  
  protected PdeKeywords keywords;
  protected Settings theme;

  private Runner runtime;

  // classpath for all known libraries for p5
  // (both those in the p5/libs folder and those with lib subfolders
  // found in the sketchbook)
//  static public String librariesClassPath;


  public Mode(File folder) {
    try {
      keywords = new PdeKeywords(new File(folder, "keywords.txt"));
    } catch (IOException e) {
      Base.showError("Problem loading keywords",
                     "Could not load keywords.txt, please re-install Processing.", e);
    }
    
    try {
      theme = new Settings(new File(folder, "theme.txt"));
    } catch (IOException e) {
      Base.showError("Problem loading theme.txt", 
                     "Could not load theme.txt, please re-install Processing", e);
    }
  }
  
  
//  public Editor createEditor(Base ibase, String path, int[] location) {
//  }
  

  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

  
  /**
   * Implements Sketch &rarr; Run.
   * @param present Set true to run in full screen (present mode).
   */
  public void handleRun(boolean present) {
    internalCloseRunner();
    toolbar.activate(EditorToolbar.RUN);
    statusEmpty();

    // do this to advance/clear the terminal window / dos prompt / etc
    for (int i = 0; i < 10; i++) System.out.println();

    // clear the console on each run, unless the user doesn't want to
    if (Preferences.getBoolean("console.auto_clear")) {
      console.clear();
    }

    // Cannot use invokeLater() here, otherwise it gets
    // placed on the event thread and causes a hang--bad idea all around.
    new Thread(present ? presentHandler : runHandler).start();
  }


  class DefaultRunHandler implements Runnable {
    public void run() {
      try {
        Build build = new Build(sketch);
        build.prepareRun();
        String appletClassName = sketch.build();
        if (appletClassName != null) {
          runtime = new Runner(Editor.this, sketch);
          runtime.launch(appletClassName, false);
        }
      } catch (Exception e) {
        statusError(e);
      }
    }
  }


  class DefaultPresentHandler implements Runnable {
    public void run() {
      try {
        sketch.prepareRun();
        String appletClassName = sketch.build();
        if (appletClassName != null) {
          runtime = new Runner(Editor.this, sketch);
          runtime.launch(appletClassName, true);
        }
      } catch (Exception e) {
        statusError(e);
      }
    }
  }


  class DefaultStopHandler implements Runnable {
    public void run() {
      try {
        if (runtime != null) {
          runtime.close();  // kills the window
          runtime = null; // will this help?
        }
      } catch (Exception e) {
        statusError(e);
      }
    }
  }

  
  /**
   * Implements Sketch &rarr; Stop, or pressing Stop on the toolbar.
   */
  public void handleStop() {  // called by menu or buttons
    toolbar.activate(EditorToolbar.STOP);

    internalCloseRunner();

    toolbar.deactivate(EditorToolbar.RUN);
    toolbar.deactivate(EditorToolbar.STOP);

    // focus the PDE again after quitting presentation mode [toxi 030903]
    toFront();
  }


  /**
   * Deactivate the Run button. This is called by Runner to notify that the
   * sketch has stopped running, usually in response to an error (or maybe
   * the sketch completing and exiting?) Tools should not call this function.
   * To initiate a "stop" action, call handleStop() instead.
   */
  public void deactivateRun() {
    toolbar.deactivate(EditorToolbar.RUN);
  }


  public void deactivateExport() {
    toolbar.deactivate(EditorToolbar.EXPORT);
  }


  /**
   * Handle internal shutdown of the runner.
   */
  public void internalCloseRunner() {
    if (stopHandler != null)
    try {
      stopHandler.run();
    } catch (Exception e) { }

//    sketch.cleanup();
  }
  
  
  /**
   * Called by Sketch &rarr; Export.
   * Handles calling the export() function on sketch, and
   * queues all the gui status stuff that comes along with it.
   * <p/>
   * Made synchronized to (hopefully) avoid problems of people
   * hitting export twice, quickly, and horking things up.
   */
  synchronized public void handleExport() {
    if (!handleExportCheckModified()) return;
    toolbar.activate(EditorToolbar.EXPORT);

    new Thread(exportHandler).start();
  }


  class DefaultExportHandler implements Runnable {
    public void run() {
      try {
        boolean success = sketch.exportApplet();
        if (success) {
          File appletFolder = new File(sketch.getFolder(), "applet");
          Base.openFolder(appletFolder);
          statusNotice("Done exporting.");
        } else {
          // error message will already be visible
        }
      } catch (Exception e) {
        statusError(e);
      }
      //toolbar.clear();
      toolbar.deactivate(EditorToolbar.EXPORT);
    }
  }


  /**
   * Handler for Sketch &rarr; Export Application
   */
  synchronized public void handleExportApplication() {
    if (!handleExportCheckModified()) return;
    toolbar.activate(EditorToolbar.EXPORT);

    // previous was using SwingUtilities.invokeLater()
    new Thread(exportAppHandler).start();
  }


  class DefaultExportAppHandler implements Runnable {
    public void run() {
      statusNotice("Exporting application...");
      try {
        if (sketch.exportApplicationPrompt()) {
          Base.openFolder(sketch.getFolder());
          statusNotice("Done exporting.");
        } else {
          // error message will already be visible
          // or there was no error, in which case it was canceled.
        }
      } catch (Exception e) {
        statusNotice("Error during export.");
        e.printStackTrace();
      }
      //toolbar.clear();
      toolbar.deactivate(EditorToolbar.EXPORT);
    }
  }


  /**
   * Checks to see if the sketch has been modified, and if so,
   * asks the user to save the sketch or cancel the export.
   * This prevents issues where an incomplete version of the sketch
   * would be exported, and is a fix for
   * <A HREF="http://dev.processing.org/bugs/show_bug.cgi?id=157">Bug 157</A>
   */
  protected boolean handleExportCheckModified() {
    if (!sketch.isModified()) return true;

    Object[] options = { "OK", "Cancel" };
    int result = JOptionPane.showOptionDialog(this,
                                              "Save changes before export?",
                                              "Save",
                                              JOptionPane.OK_CANCEL_OPTION,
                                              JOptionPane.QUESTION_MESSAGE,
                                              null,
                                              options,
                                              options[0]);

    if (result == JOptionPane.OK_OPTION) {
      handleSave(true);

    } else {
      // why it's not CANCEL_OPTION is beyond me (at least on the mac)
      // but f-- it.. let's get this shite done..
      //} else if (result == JOptionPane.CANCEL_OPTION) {
      statusNotice("Export canceled, changes must first be saved.");
      //toolbar.clear();
      return false;
    }
    return true;
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

  
  public Settings getTheme() {
    return theme;
  }
  
  
  public String getReference(String keyword) {
    return keywords.getReference(keyword);
  }
  
  
  public TokenMarker getTokenMarker() {
    return new PdeKeywords();
  }


  /*
  public String get(String attribute) {
    return theme.get(attribute);
  }

  
  public boolean getBoolean(String attribute) {
    return theme.getBoolean(attribute);
  }
  
  
  public int getInteger(String attribute) {
    return theme.getInteger(attribute);
  }


  public Color getColor(String attribute) {
    return theme.getColor(attribute);
  }


  public Font getFont(String attribute) {
    return theme.getFont(attribute);
  }


  public SyntaxStyle getStyle(String attribute) {
    return theme.getStyle(attribute);
  }
  */
}