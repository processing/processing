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

package processing.mode.java;

import java.awt.Color;
import java.awt.Font;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.*;

import processing.app.*;
import processing.app.syntax.PdeKeywords;
import processing.app.syntax.SyntaxStyle;
import processing.app.syntax.TokenMarker;
import processing.core.PApplet;
import processing.mode.java.runner.Runner;


public class JavaMode extends Mode {
  private Runner runtime;

  // classpath for all known libraries for p5
  // (both those in the p5/libs folder and those with lib subfolders
  // found in the sketchbook)
//  static public String librariesClassPath;


  public JavaMode(Base base, File folder) {
    super(base, folder);

    try {
      loadKeywords();
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
    
    /*
    item = newJMenuItem("Export", 'E');
    if (editor != null) {
      item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          editor.handleExport();
        }
      });
    } else {
      item.setEnabled(false);
    }
    fileMenu.add(item);

    item = newJMenuItemShift("Export Application", 'E');
    if (editor != null) {
      item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          editor.handleExportApplication();
        }
      });
    } else {
      item.setEnabled(false);
    }
    fileMenu.add(item);
     */
  }


  public JMenu buildFileMenu(Editor editor) {
    JMenuItem exportApplet = Base.newJMenuItem("Export Applet", 'E');
    exportApplet.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        handleExport();
      }
    });
      
    JMenuItem exportApplication = Base.newJMenuItemShift("Export Application", 'E');
    exportApplication.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        handleExportApplication();
      }
    });
    return buildFileMenu(editor, new JMenuItem[] { exportApplet, exportApplication });
  }


  protected void loadKeywords() throws IOException {
    File file = new File(folder, "keywords.txt");
    BufferedReader reader = PApplet.createReader(file);

    tokenMarker = new PdeKeywords();
    keywordToReference = new HashMap<String, String>();

    String line = null;
    while ((line = reader.readLine()) != null) {
      String[] pieces = PApplet.trim(PApplet.split(line, '\t'));
      if (pieces.length >= 2) {
        String keyword = pieces[0];
        String coloring = pieces[1];

        if (coloring.length() > 0) {
          tokenMarker.addColoring(keyword, coloring);
        }
        if (pieces.length == 3) {
          String htmlFilename = pieces[2];
          if (htmlFilename.length() > 0) {
            keywordToReference.put(keyword, htmlFilename);
          }
        }
      }
    }
  }

  
  public String getTitle() {
    return "Standard";
  }


  public EditorToolbar createToolbar(Editor editor) {
    return new Toolbar(editor);
  }

  
  public Formatter createFormatter() {
    return new AutoFormat();
  }
  
  
  public String getCommentPrefix() {
    return "//";
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
    toolbar.activate(Toolbar.RUN);
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


  /**
   * Implements Sketch &rarr; Stop, or pressing Stop on the toolbar.
   */
  public void handleStop(Editor editor) {  // called by menu or buttons
    Toolbar toolbar = editor.getToolbar();
    toolbar.activate(Toolbar.STOP);

    internalCloseRunner(editor);

    toolbar.deactivate(Toolbar.RUN);
    toolbar.deactivate(Toolbar.STOP);

    // focus the PDE again after quitting presentation mode [toxi 030903]
    editor.toFront();
  }
  
  
//  class DefaultStopHandler implements Runnable {
//    public void run() {
//      try {
//        if (runtime != null) {
//          runtime.close();  // kills the window
//          runtime = null; // will this help?
//        }
//      } catch (Exception e) {
//        statusError(e);
//      }
//    }
//  }

  
  public void handleStopImpl(Editor editor) {
    try {
      if (runtime != null) {
        runtime.close();  // kills the window
        runtime = null; // will this help?
      }
    } catch (Exception e) {
      editor.statusError(e);
    }
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
  public void internalCloseRunner(Editor editor) {
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
}