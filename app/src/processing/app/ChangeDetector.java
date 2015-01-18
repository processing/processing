package processing.app;

import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

public class ChangeDetector implements WindowFocusListener {
  private Sketch sketch;

  private Editor editor;

  private boolean enabled = true;

  public ChangeDetector(Sketch sketch, Editor editor) {
    this.sketch = sketch;
    this.editor = editor;
  }

  @Override
  public void windowGainedFocus(WindowEvent e) {
    //remove the detector from main if it is disabled during runtime (due to an error?)
    if (!enabled || !Preferences.getBoolean("editor.watcher")) {
      editor.removeWindowFocusListener(this);
      return;
    }
    checkFileChange();
  }

  private void checkFileChange() {
    //check that the content of each of the files in sketch matches what is in memory
    if (sketch == null) {
      return;
    }

    //check file count first
    File sketchFolder = sketch.getFolder();
    if (sketchFolder.isDirectory()) {
      int fileCount = sketchFolder.list(new FilenameFilter() {
        //return true if the file is a code file for this mode
        @Override
        public boolean accept(File dir, String name) {
          for (String s : editor.getMode().getExtensions()) {
            if (name.endsWith(s)) {
              return true;
            }
          }
          return false;
        }
      }).length;
      if (fileCount != sketch.getCodeCount()) {
        if (reloadSketch()) {
          return;
        }
      }
    }

    SketchCode[] codes = sketch.getCode();
    for (SketchCode sc : codes) {
      String inMemory = sc.getProgram();
      String onDisk = null;
      File sketchFile = sc.getFile();
      if (!sketchFile.exists()) {
        //if a file in the sketch was not found, then it must have been deleted externally
        //so reload the sketch
        if (reloadSketch()) {
          break;
        }
      }
      try {
        onDisk = Base.loadFile(sketchFile);
      } catch (IOException e1) {
        Base
          .showWarningTiered("File Change Detection Failed",
                             "Checking for changed files for this sketch has failed.",
                             "The file change detector will be disabled.", e1);
        enabled = false;
        return;
      }
      if (onDisk == null) {
        //failed
      } else if (!inMemory.equals(onDisk)) {
        if (reloadSketch()) {
          return;
        }
      }
    }
  }

  //returns true if the files in the sketch have been reloaded
  private boolean reloadSketch() {
    int response = Base
      .showYesNoQuestion(editor, "File Modified",
                         "Your sketch has been modified externally",
                         "Would you like to reload the sketch?");
    if (response == 0) {
      File mainFile = sketch.getMainFile();
      //reload the sketch
      try {
        sketch.reload();
        editor.header.rebuild();
      } catch (Exception f) {
        if (sketch.getCodeCount() < 1) {
          Base.showWarning("Canceling Reload",
                           "You cannot delete the last code file in a sketch.");
          //if they deleted the last file, re-save the SketchCode
          try {
            //make a blank file
            mainFile.createNewFile();
          } catch (IOException e1) {
            //if that didn't work, tell them it's un-recoverable
            Base.showError("Reload failed",
                           "The sketch contains no code files", e1);
            //don't try to reload again after the double fail
            //this editor is probably trashed by this point, but a save-as might be possible
            return false;
          }
        }
      }
    }
    return true;
  }

  @Override
  public void windowLostFocus(WindowEvent e) {
    //shouldn't need to do anything here
  }

}
