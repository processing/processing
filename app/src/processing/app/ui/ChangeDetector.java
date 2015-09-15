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
          checkFileChange();
        }
      }).start();
    }
  }


  @Override
  public void windowLostFocus(WindowEvent e) {
    // Shouldn't need to do anything here, and not storing anything here b/c we
    // don't want to assume a loss of focus is required before change detection
  }


  private void checkFileChange() {
    //check that the content of each of the files in sketch matches what is in memory
    if (sketch == null) {
      return;
    }

    // make sure the sketch folder exists at all.
    // if it does not, it will be re-saved, and no changes will be detected
    sketch.ensureExistence();

//    if (lastKnownCount == -1) {
//      lastKnownCount = sketch.getCodeCount();
//    }

    // check file count first
    File sketchFolder = sketch.getFolder();
    File[] sketchFiles = sketchFolder.listFiles(new FilenameFilter() {
      @Override
      public boolean accept(File dir, String name) {
        for (String s : editor.getMode().getExtensions()) {
          if (name.toLowerCase().endsWith(s.toLowerCase())) {
            return true;
          }
        }
        return false;
      }
    });
    int fileCount = sketchFiles.length;

    // Prompt the user about this one even if they canceled before. It's a bad
    // situation and there aren't good ways to recover or work around it.
    if (fileCount != sketch.getCodeCount()) {
    //if (fileCount != lastKnownCount) {  //sketch.getCodeCount()) {
      // if they chose to reload and there aren't any files left
      if (reloadCode(null) && fileCount < 1) {
        try {
          //make a blank file
          sketch.getMainFile().createNewFile();
        } catch (Exception e1) {
          //if that didn't work, tell them it's un-recoverable
          showErrorEDT("Reload failed", "The sketch contains no code files.", e1);
          //don't try to reload again after the double fail
          //this editor is probably trashed by this point, but a save-as might be possible
//          skip = true;
          return;
        }
        //it's okay to do this without confirmation, because they already confirmed to deleting the unsaved changes above
        sketch.reload();
        showWarningEDT("Modified Reload",
                       "You cannot delete the last code file in a sketch.\n" +
                       "A new blank sketch file has been generated for you.");

      }
      return;
    }

//    SketchCode[] codes = sketch.getCode();
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
    if (reloadList.size() > 0) {
      reloadCode(reloadList);
    }
  }


  private void setSketchCodeModified(SketchCode sc) {
    sc.setModified(true);
    sketch.setModified(true);
  }


  /**
   * @param changed The file that was known to be modified
   * @return true if the files in the sketch have been reloaded
   */
  private boolean reloadCode(List<SketchCode> changed) {
    int response = blockingYesNoPrompt(editor,
                                       "File Modified",
                                       "Your sketch has been modified externally.<br>" +
                                       "Would you like to reload the sketch?",
                                       "If you reload the sketch, any unsaved changes will be lost.");
    if (response == JOptionPane.YES_OPTION) {
      sketch.reload();
      rebuildHeaderEDT();
      return true;

    } else {  // "No" (or cancel by hitting ESC)
      if (changed != null) {
        for (SketchCode code : changed) {
          // Set the file as modified in the Editor so the contents will
          // save to disk when the user saves from inside Processing.
          setSketchCodeModified(code);
          // Since this was canceled, update the "last modified" time so we
          // don't ask the user about it again.
          code.setLastModified();
        }
      } else {  // the file count changed
        // Because the number of files changed, they may be working with a file
        // that doesn't exist any more. So find the files that are missing,
        // and mark them as modified so that the next "Save" will write them.
        for (SketchCode code : sketch.getCode()) {
          if (!code.getFile().exists()) {
            setSketchCodeModified(code);
          }
        }
        // If files were simply added, then nothing needs done
      }
      rebuildHeaderEDT();
//      skip = true;
      return false;
    }
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


  private void showWarningEDT(final String title, final String message) {
    EventQueue.invokeLater(new Runnable() {
      @Override
      public void run() {
        Messages.showWarning(title, message);
      }
    });
  }


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
