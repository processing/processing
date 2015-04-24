package processing.app;

import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.InvocationTargetException;

import javax.swing.JOptionPane;


public class ChangeDetector implements WindowFocusListener {
  private Sketch sketch;
  private Editor editor;

  private boolean enabled = true;
  private boolean skip = false;


  public ChangeDetector(Editor editor) {
    this.sketch = editor.sketch;
    this.editor = editor;
  }


  private void checkFileChange() {
    //check that the content of each of the files in sketch matches what is in memory
    if (sketch == null) {
      return;
    }

    // make sure the sketch folder exists at all.
    // if it does not, it will be re-saved, and no changes will be detected
    sketch.ensureExistence();

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

    if (fileCount != sketch.getCodeCount()) {
      // if they chose to reload and there aren't any files left
      if (reloadSketch(null) && fileCount < 1) {
        try {
          //make a blank file
          sketch.getMainFile().createNewFile();
        } catch (Exception e1) {
          //if that didn't work, tell them it's un-recoverable
          showErrorEDT("Reload failed", "The sketch contains no code files.", e1);
          //don't try to reload again after the double fail
          //this editor is probably trashed by this point, but a save-as might be possible
          skip = true;
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

    SketchCode[] codes = sketch.getCode();
    for (SketchCode sc : codes) {
      File sketchFile = sc.getFile();
      if (sketchFile.exists()) {
        long diff = sketchFile.lastModified() - sc.lastModified();
        if (diff != 0) {
          if (Base.isMacOS() && diff == 1000L) {
            // Mac OS X has a one second difference. Not sure if it's a Java bug
            // or something else about how OS X is writing files.
            continue;
          }
          System.out.println(sketchFile.getName() + " " + diff);
          reloadSketch(sc);
          return;
        }
      } else {
        // If a file in the sketch was not found, then it must have been
        // deleted externally, so reload the sketch.
        reloadSketch(sc);
        return;
      }
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
  private boolean reloadSketch(SketchCode changed) {
    int response = blockingYesNoPrompt(editor,
                                       "File Modified",
                                       "Your sketch has been modified externally.<br>" +
                                       "Would you like to reload the sketch?",
                                       "If you reload the sketch, any unsaved changes will be lost.");
    if (response == JOptionPane.YES_OPTION) {
      sketch.reload();
      rebuildHeaderEDT();
      return true;
    }

    // they said no (or canceled), make it possible to stop the msgs by saving
    if (changed != null) {
      //set it to be modified so that it will actually save to disk when the user saves from inside processing
      setSketchCodeModified(changed);

    } else {
      // Because the number of files changed, they may be working with a file
      // that doesn't exist any more. So find the files that are missing,
      // and mark them as modified so that the next "Save" will write them.
      for (SketchCode sc : sketch.getCode()) {
        if (!sc.getFile().exists()) {
          setSketchCodeModified(sc);
        }
      }
      // If files were simply added, then nothing needs done
    }
    rebuildHeaderEDT();
    skip = true;
    return false;
  }


  @Override
  public void windowLostFocus(WindowEvent e) {
    //shouldn't need to do anything here
  }


  @Override
  public void windowGainedFocus(WindowEvent e) {
    if (enabled) {
      //remove the detector from main if it is disabled during runtime (due to an error?)
      //if (!enabled || !Preferences.getBoolean("editor.watcher")) {
      //editor.removeWindowFocusListener(this);
      //} else if (skip) {

      // if they selected no, skip the next focus event
      if (skip) {
        skip = false;

      } else {
        new Thread(new Runnable() {
          @Override
          public void run() {
            checkFileChange();
          }
        }).start();
      }
    }
  }


  private void showErrorEDT(final String title, final String message,
                              final Exception e) {
    EventQueue.invokeLater(new Runnable() {
      @Override
      public void run() {
        Base.showError(title, message, e);
      }
    });
  }


  private void showWarningEDT(final String title, final String message) {
    EventQueue.invokeLater(new Runnable() {
      @Override
      public void run() {
        Base.showWarning(title, message);
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
          result[0] = Base.showYesNoQuestion(editor, title, message1, message2);
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
