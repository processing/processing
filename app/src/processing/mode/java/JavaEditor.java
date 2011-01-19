package processing.mode.java;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import processing.app.Base;
import processing.app.Editor;
import processing.app.EditorToolbar;
import processing.app.Mode;
import processing.app.Preferences;

public class JavaEditor extends Editor {
  JavaMode jmode;


  protected JavaEditor(Base base, String path, int[] location, Mode mode) {
    super(base, path, location, mode);
    jmode = (JavaMode) mode;
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

  
  public JMenu buildFileMenu() {
    JMenuItem exportApplet = Base.newJMenuItem("Export Applet", 'E');
    exportApplet.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        handleExportApplet();
      }
    });
      
    JMenuItem exportApplication = Base.newJMenuItemShift("Export Application", 'E');
    exportApplication.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        handleExportApplication();
      }
    });
    return buildFileMenu(new JMenuItem[] { exportApplet, exportApplication });
  }
  
  
  public JMenu buildSketchMenu() {
    JMenuItem runItem = Base.newJMenuItem("Run", 'R');
    runItem.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          handleRun();
        }
      });

    JMenuItem presentItem = Base.newJMenuItemShift("Present", 'R');
    presentItem.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          handlePresent();
        }
      });

    JMenuItem stopItem = new JMenuItem("Stop");
    stopItem.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          handleStop();
        }
      });
    return buildSketchMenu(new JMenuItem[] { runItem, presentItem, stopItem });
  }
  

  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

  
  public String getCommentPrefix() {
    return "//";
  }
  
  
  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  /**
   * Called by Sketch &rarr; Export.
   * Handles calling the export() function on sketch, and
   * queues all the gui status stuff that comes along with it.
   * <p/>
   * Made synchronized to (hopefully) avoid problems of people
   * hitting export twice, quickly, and horking things up.
   */
  public void handleExportApplet() {
    if (handleExportCheckModified()) {
      toolbar.activate(Toolbar.EXPORT);    
      try {
        boolean success = jmode.handleExportApplet();
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
      toolbar.deactivate(Toolbar.EXPORT);
    }
  }
  
  
  public void handleExportApplication() {
    toolbar.activate(Toolbar.EXPORT);
    try {
      jmode.handleExportApplication();
    } catch (Exception e) {
      statusError(e);
    }
    toolbar.deactivate(Toolbar.EXPORT);
  }  
  
  

  /**
   * Handler for Sketch &rarr; Export Application
   */
  synchronized public void handleExportApplication() {
    toolbar.activate(Toolbar.EXPORT);

    if (handleExportCheckModified()) {
      statusNotice("Exporting application...");
      try {
        if (jmode.exportApplicationPrompt()) {
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
    if (sketch.isModified()) {
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
    }
    return true;
  }
  

  protected void prepareRun() {
    internalCloseRunner();
    toolbar.activate(Toolbar.RUN);
    statusEmpty();

    // do this to advance/clear the terminal window / dos prompt / etc
    for (int i = 0; i < 10; i++) System.out.println();

    // clear the console on each run, unless the user doesn't want to
    if (Preferences.getBoolean("console.auto_clear")) {
      console.clear();
    }
  }
  
  
  public void handleRun() {
    prepareRun();
    try {
      jmode.handleRun(this, sketch);
    } catch (Exception e) {
      statusError(e);
    }
  }
  
  
  public void handlePresent() {
    prepareRun();
    try {
      jmode.handlePresent(this, sketch);
    } catch (Exception e) {
      statusError(e);
    }
  }
  
  
  public void handleStop() {
    toolbar.activate(Toolbar.STOP);

    try {
      jmode.handleStop(this);
    } catch (Exception e) {
      statusError(e);
    }

    toolbar.deactivate(Toolbar.RUN);
    toolbar.deactivate(Toolbar.STOP);

    // focus the PDE again after quitting presentation mode [toxi 030903]
    toFront();
  }
  
  
  /**
   * Add import statements to the current tab for all of packages inside
   * the specified jar file.
   */
  public void handleImportLibrary(String jarPath) {
    // make sure the user didn't hide the sketch folder
    sketch.ensureExistence();

    // import statements into the main sketch file (code[0])
    // if the current code is a .java file, insert into current
    //if (current.flavor == PDE) {
    if (mode.isDefaultExtension(sketch.getCurrentCode())) {
      sketch.setCurrentCode(0);
    }
    
    // could also scan the text in the file to see if each import
    // statement is already in there, but if the user has the import
    // commented out, then this will be a problem.
    String[] list = Base.packageListFromClassPath(jarPath);
    StringBuffer buffer = new StringBuffer();
    for (int i = 0; i < list.length; i++) {
      buffer.append("import ");
      buffer.append(list[i]);
      buffer.append(".*;\n");
    }
    buffer.append('\n');
    buffer.append(getText());
    setText(buffer.toString());
    setSelection(0, 0);  // scroll to start
    sketch.setModified(true);
  }
  
  
  /**
   * Deactivate the Run button. This is called by Runner to notify that the
   * sketch has stopped running, usually in response to an error (or maybe
   * the sketch completing and exiting?) Tools should not call this function.
   * To initiate a "stop" action, call handleStop() instead.
   */
  public void deactivateRun() {
    toolbar.deactivate(Toolbar.RUN);
  }


  public void deactivateExport() {
    toolbar.deactivate(Toolbar.EXPORT);
  }
}