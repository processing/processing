package processing.app;

import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

public class ChangeDetector implements WindowFocusListener {
  private Sketch sketch;

  private Editor editor;

//  private boolean enabled = true;
  private boolean enabled = false;  // broken on OS X

  private boolean skip = false;

  public ChangeDetector(Editor editor) {
    this.sketch = editor.sketch;
    this.editor = editor;
  }

  @Override
  public void windowGainedFocus(WindowEvent e) {
    //remove the detector from main if it is disabled during runtime (due to an error?)
    if (!enabled || !Preferences.getBoolean("editor.watcher")) {
      editor.removeWindowFocusListener(this);
      return;
    }
    //if they selected no, skip the next focus event
    if (skip) {
      skip = false;
      return;
    }
    checkFileChangeAsync();
  }

  private void checkFileChangeAsync() {
    Thread th = new Thread(new Runnable() {
      @Override
      public void run() {
        checkFileChange();
      }
    });
    th.start();
  }

  private void checkFileChange() {
    //check that the content of each of the files in sketch matches what is in memory
    if (sketch == null) {
      return;
    }

    //check file count first
    File sketchFolder = sketch.getFolder();
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
      reloadSketch(null);
      if (fileCount < 1) {
        try {
          //make a blank file
          sketch.getMainFile().createNewFile();
        } catch (Exception e1) {
          //if that didn't work, tell them it's un-recoverable
          Base.showError("Reload failed", "The sketch contains no code files.",
                         e1);
          //don't try to reload again after the double fail
          //this editor is probably trashed by this point, but a save-as might be possible
          skip = true;
          return;
        }
        //it's okay to do this without confirmation, because they already confirmed to deleting the unsaved changes above
        sketch.reload();
        editor.header.rebuild();
        Base
          .showWarning("Modified Reload",
                       "You cannot delete the last code file in a sketch.\n"
                         + "A new blank sketch file has been generated for you.");

      }
      return;
    }

    SketchCode[] codes = sketch.getCode();
    for (SketchCode sc : codes) {
      String inMemory = sc.getProgram();
      String onDisk = null;
      File sketchFile = sc.getFile();
      if (!sketchFile.exists()) {
        //if a file in the sketch was not found, then it must have been deleted externally
        //so reload the sketch
        reloadSketch(sc);
        return;
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
        reloadSketch(sc);
        return;
      }
    }
  }

  private void setSketchCodeModified(SketchCode sc) {
    sc.setModified(true);
    sketch.setModified(true);
  }

  //returns true if the files in the sketch have been reloaded
  private void reloadSketch(SketchCode changed) {
    int response = Base
      .showYesNoQuestion(editor,
                         "File Modified",
                         "Your sketch has been modified externally.<br>Would you like to reload the sketch?",
                         "If you reload the sketch, any unsaved changes will be lost!");
    if (response == 0) {
      //reload the sketch

      sketch.reload();
      editor.header.rebuild();

    } else {
      //they said no, make it possible for them to stop the errors by saving
      if (changed != null) {
        //set it to be modified so that it will actually save to disk when the user saves from inside processing
        setSketchCodeModified(changed);
      } else {
        //the number of files changed, so they may be working with a file that doesn't exist any more
        //find the files that are missing, and mark them as modified
        for (SketchCode sc : sketch.getCode()) {
          if (!sc.getFile().exists()) {
            setSketchCodeModified(sc);
          }
        }
        //if files were simply added, then nothing needs done
      }
      editor.header.rebuild();
      skip = true;
      return;
    }
    return;
  }

  @Override
  public void windowLostFocus(WindowEvent e) {
    //shouldn't need to do anything here
  }

}
