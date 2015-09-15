package processing.app.ui;

import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;

import processing.app.Messages;
import processing.app.Preferences;
import processing.app.Sketch;
import processing.app.SketchCode;


public class ChangeDetector implements WindowFocusListener {
  private final Sketch sketch;
  private final Editor editor;

  // Windows and others seem to have a few hundred ms difference in reported
  // times, so we're arbitrarily setting a gap in time here.
  // Mac OS X has an (exactly) one second difference. Not sure if it's a Java
  // bug or something else about how OS X is writing files.
  static private final int MODIFICATION_WINDOW_MILLIS =
    Preferences.getInteger("editor.watcher.window");

  // Debugging this feature is particularly difficult, adding an option for it
  static private final boolean DEBUG =
    Preferences.getBoolean("editor.watcher.debug");

  // Store the known number of files to avoid re-asking about the same change
//  private int lastKnownCount = -1;


  public ChangeDetector(Editor editor) {
    this.sketch = editor.sketch;
    this.editor = editor;
  }


  @Override
  public void windowGainedFocus(WindowEvent e) {
    // When the window is activated, fire off a Thread to check for changes
    if (Preferences.getBoolean("editor.watcher")) {
      new Thread(new Runnable() {
        @Override
        public void run() {
          if (sketch != null) {
            // make sure the sketch folder exists at all.
            // if it does not, it will be re-saved, and no changes will be detected
            sketch.ensureExistence();

//            if (lastKnownCount == -1) {
//              lastKnownCount = sketch.getCodeCount();
//            }

            boolean alreadyPrompted = checkFileCount();
            if (!alreadyPrompted) {
              checkFileTimes();
            }
          }
        }
      }).start();
    }
  }


  @Override
  public void windowLostFocus(WindowEvent e) {
    // Shouldn't need to do anything here, and not storing anything here b/c we
    // don't want to assume a loss of focus is required before change detection
  }


  private boolean checkFileCount() {
    // check file count first
    File sketchFolder = sketch.getFolder();
    File[] sketchFiles = sketchFolder.listFiles(new FilenameFilter() {
      @Override
      public boolean accept(File dir, String filename) {
        for (String ext : editor.getMode().getExtensions()) {
          if (filename.toLowerCase().endsWith(ext.toLowerCase())) {
            return true;
          }
        }
        return false;
      }
    });
    int fileCount = sketchFiles.length;

    // Was considering keeping track of the last "known" number of files
    // (instead of using sketch.getCodeCount() here) in case the user
    // didn't want to reload after the number of files had changed.
    // However, that's a bad situation anyway and there aren't good
    // ways to recover or work around it, so just prompt the user again.
    if (fileCount == sketch.getCodeCount()) {
      return false;
    }

    if (DEBUG) {
      System.out.println(sketch.getName() + " file count now " + fileCount +
                         " instead of " + sketch.getCodeCount());
    }

    if (reloadPrompt()) {
      if (sketch.getMainFile().exists()) {
        reloadSketch();
      } else {
        // If the main file was deleted, and that's why we're here,
        // then we need to re-save the sketch instead.
        try {
          // Mark everything as modified so that it saves properly
          for (SketchCode code : sketch.getCode()) {
            code.setModified(true);
          }
          sketch.save();
        } catch (Exception e) {
          //if that didn't work, tell them it's un-recoverable
          showErrorEDT("Reload Failed",
                       "The main file for this sketch was deleted\n" +
                       "and could not be rewritten.", e);
        }
      }

      /*
      if (fileCount < 1) {
        // if they chose to reload and there aren't any files left
        try {
          // make a blank file for the main PDE
          sketch.getMainFile().createNewFile();
        } catch (Exception e1) {
          //if that didn't work, tell them it's un-recoverable
          showErrorEDT("Reload failed", "The sketch contains no code files.", e1);
          //don't try to reload again after the double fail
          //this editor is probably trashed by this point, but a save-as might be possible
//          skip = true;
          return true;
        }
        // it's okay to do this without confirmation, because they already
        // confirmed to deleting the unsaved changes above
        sketch.reload();
        showWarningEDT("Modified Reload",
                       "You cannot delete the last code file in a sketch.\n" +
                       "A new blank sketch file has been generated for you.");
      }
      */
    } else {  // !reload (user said no or closed the window)
      // Because the number of files changed, they may be working with a file
      // that doesn't exist any more. So find the files that are missing,
      // and mark them as modified so that the next "Save" will write them.
      for (SketchCode code : sketch.getCode()) {
        if (!code.getFile().exists()) {
          setCodeModified(code);
        }
      }
      rebuildHeaderEDT();
    }
    // Yes, we've brought this up with the user (so don't bother them further)
    return true;
  }


  private void checkFileTimes() {
    List<SketchCode> reloadList = new ArrayList<>();
    for (SketchCode code : sketch.getCode()) {
      File sketchFile = code.getFile();
      if (sketchFile.exists()) {
        long diff = sketchFile.lastModified() - code.getLastModified();
        if (diff > MODIFICATION_WINDOW_MILLIS) {
          if (DEBUG) System.out.println(sketchFile.getName() + " " + diff + "ms");
          reloadList.add(code);
        }
      } else {
        // If a file in the sketch was not found, then it must have been
        // deleted externally, so reload the sketch.
        if (DEBUG) System.out.println(sketchFile.getName() + " (file disappeared)");
        reloadList.add(code);
      }
    }

    // If there are any files that need to be reloaded
    if (reloadList.size() > 0) {
      if (reloadPrompt()) {
        reloadSketch();

      } else {
        // User said no, but take bulletproofing actions
        for (SketchCode code : reloadList) {
          // Set the file as modified in the Editor so the contents will
          // save to disk when the user saves from inside Processing.
          setCodeModified(code);
          // Since this was canceled, update the "last modified" time so we
          // don't ask the user about it again.
          code.setLastModified();
        }
        rebuildHeaderEDT();
      }
    }
  }


  private void setCodeModified(SketchCode sc) {
    sc.setModified(true);
    sketch.setModified(true);
  }


  private void reloadSketch() {
    sketch.reload();
    rebuildHeaderEDT();
  }


  /**
   * Prompt the user whether to reload the sketch. If the user says yes,
   * perform the actual reload.
   * @return true if user said yes, false if they hit No or closed the window
   */
  private boolean reloadPrompt() {
    int response = blockingYesNoPrompt(editor,
                                       "File Modified",
                                       "Your sketch has been modified externally.<br>" +
                                       "Would you like to reload the sketch?",
                                       "If you reload the sketch, any unsaved changes will be lost.");
    return response == JOptionPane.YES_OPTION;
  }


  private void showErrorEDT(final String title, final String message,
                              final Exception e) {
    EventQueue.invokeLater(new Runnable() {
      @Override
      public void run() {
        Messages.showError(title, message, e);
      }
    });
  }


  /*
  private void showWarningEDT(final String title, final String message) {
    EventQueue.invokeLater(new Runnable() {
      @Override
      public void run() {
        Messages.showWarning(title, message);
      }
    });
  }
  */


  private int blockingYesNoPrompt(final Frame editor, final String title,
                                  final String message1,
                                  final String message2) {
    final int[] result = { -1 };  // yuck
    try {
      //have to wait for a response on this one
      EventQueue.invokeAndWait(new Runnable() {
        @Override
        public void run() {
          result[0] = Messages.showYesNoQuestion(editor, title, message1, message2);
        }
      });
    } catch (InvocationTargetException e) {
      //occurs if Base.showYesNoQuestion throws an error, so, shouldn't happen
      e.getTargetException().printStackTrace();
    } catch (InterruptedException e) {
      //occurs if the EDT is interrupted, so, shouldn't happen
      e.printStackTrace();
    }
    return result[0];
  }


  private void rebuildHeaderEDT() {
    EventQueue.invokeLater(new Runnable() {
      @Override
      public void run() {
        editor.header.rebuild();
      }
    });
  }
}
