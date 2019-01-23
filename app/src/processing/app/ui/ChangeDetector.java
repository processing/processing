/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2012-19 The Processing Foundation

  This program is free software; you can redistribute it and/or modify
  it under the terms of the GNU General Public License version 2
  as published by the Free Software Foundation.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software Foundation, Inc.
  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
*/

package processing.app.ui;

import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import processing.app.Language;
import processing.app.Messages;
import processing.app.Preferences;
import processing.app.Sketch;
import processing.app.SketchCode;


public class ChangeDetector implements WindowFocusListener {
  private final Sketch sketch;
  private final Editor editor;

  private List<SketchCode> ignoredRemovals = new ArrayList<>();
  private List<SketchCode> ignoredModifications = new ArrayList<>();

  // Windows and others seem to have a few hundred ms difference in reported
  // times, so we're arbitrarily setting a gap in time here.
  // Mac OS X has an (exactly) one second difference. Not sure if it's a Java
  // bug or something else about how OS X is writing files.
  static private final int MODIFICATION_WINDOW_MILLIS =
    Preferences.getInteger("editor.watcher.window");

  // Debugging this feature is particularly difficult, adding an option for it
  static private final boolean DEBUG =
    Preferences.getBoolean("editor.watcher.debug");


  public ChangeDetector(Editor editor) {
    this.sketch = editor.sketch;
    this.editor = editor;
  }


  @Override
  public void windowGainedFocus(WindowEvent e) {
    if (Preferences.getBoolean("editor.watcher")) {
      if (sketch != null) {
        // make sure the sketch folder exists at all.
        // if it does not, it will be re-saved, and no changes will be detected
        sketch.ensureExistence();

        checkFiles();
      }
    }
  }


  @Override
  public void windowLostFocus(WindowEvent e) {
    // Shouldn't need to do anything here, and not storing anything here b/c we
    // don't want to assume a loss of focus is required before change detection
  }


  private void checkFiles() {
    List<String> filenames = new ArrayList<>();
    List<String> extensions = new ArrayList<>();
    sketch.getSketchCodeFiles(filenames, extensions);

    SketchCode[] codes = sketch.getCode();

    // Separate codes with and without files
    Map<Boolean, List<SketchCode>> existsMap = Arrays.stream(codes)
        .collect(Collectors.groupingBy(code -> filenames.contains(code.getFileName())));


    // ADDED FILES

    List<String> codeFilenames = Arrays.stream(codes)
        .map(SketchCode::getFileName)
        .collect(Collectors.toList());

    // Get filenames which are in filesystem but don't have code
    List<String> addedFilenames = filenames.stream()
        .filter(f -> !codeFilenames.contains(f))
        .collect(Collectors.toList());

    // Take action if there are any added files which were not previously ignored
    boolean added = !addedFilenames.isEmpty();


    // REMOVED FILES

    // Get codes which don't have file
    List<SketchCode> removedCodes = Optional.ofNullable(existsMap.get(Boolean.FALSE))
        .orElse(Collections.emptyList());
    List<SketchCode> removedCodesFinal = removedCodes.stream()
      .filter(code -> !ignoredRemovals.contains(code))
      .collect(Collectors.toList());

    // Show prompt if there are any removed codes which were not previously ignored
    boolean removed = !removedCodesFinal.isEmpty();


    /// MODIFIED FILES

    // Get codes which have file with different modification time
    List<SketchCode> modifiedCodes = existsMap.containsKey(Boolean.TRUE) ?
      existsMap.get(Boolean.TRUE) : Collections.emptyList();
    List<SketchCode> modifiedCodesFinal = new ArrayList<>();
    for (SketchCode code : modifiedCodes) {
      if (ignoredModifications.contains(code)) continue;
      long fileLastModified = code.getFile().lastModified();
      long codeLastModified = code.getLastModified();
      long diff = fileLastModified - codeLastModified;
      if (fileLastModified == 0L || diff > MODIFICATION_WINDOW_MILLIS) {
        modifiedCodesFinal.add(code);
      }
    }

    // Show prompt if any open codes were modified
    boolean modified = !modifiedCodesFinal.isEmpty();

    // Clean ignore lists
    ignoredModifications.retainAll(modifiedCodes);
    ignoredRemovals.retainAll(removedCodes);


    boolean changes = added || removed || modified;
    // Do both PDE and disk change for any one file?
    List<SketchCode> mergeConflicts = modifiedCodesFinal.stream()
      .filter(SketchCode::isModified)
      .collect(Collectors.toList());
    boolean ask = !mergeConflicts.isEmpty() || removed;

    if (DEBUG) {
      System.out.println("ask: "             + ask + "\n" +
                         "merge conflicts: " + mergeConflicts + ",\n" +
                         "added filenames: " + addedFilenames + ",\n" +
                         "removed codes: " + removedCodes + ",\n" +
                         "ignored removed: " + ignoredRemovals + ",\n" +
                         "modified codes: " + modifiedCodesFinal + "\n");
    }


    // No prompt yet.
    if (changes) {
      for (int i = 0; i < filenames.size(); i++) {
        for (String addedTab : addedFilenames) {
          if (filenames.get(i).equals(addedTab)) {
            sketch.loadNewTab(filenames.get(i), extensions.get(i), true);
          }
        }
      }
      for (SketchCode modifiedCode : modifiedCodesFinal) {
        if (!mergeConflicts.contains(modifiedCode)) {
          sketch.loadNewTab(modifiedCode.getFileName(),
              modifiedCode.getExtension(), false);
        }
      }

      // Destructive actions, so prompt.
      if (ask) {
        sketch.updateSketchCodes();

        showReloadPrompt(mergeConflicts, removedCodesFinal,
          scReload -> {
            try {
              File file = scReload.getFile();
              File autosave = File.createTempFile(scReload.getPrettyName(),
                ".autosave", file.getParentFile());
              scReload.copyTo(autosave);
            } catch (IOException e) {
              Messages.showWarning("Could not autosave modified tab",
                  "Your changes to " + scReload.getPrettyName() +
                  " have not been saved, so we won't load the new version.", e);
              scReload.setModified(true); // So we'll have another go at saving
                                          // it later,
              ignoredModifications.add(scReload); // but not create a loop.
              return;
            }
            sketch.loadNewTab(scReload.getFileName(), scReload.getExtension(), false);
          },
          scKeep -> {
            scKeep.setLastModified();
            scKeep.setModified(true);
          },
          scDelete -> sketch.removeCode(scDelete),
          scResave -> {
            try {
              scResave.save();
            } catch (IOException e) {
              if (sketch.getCode(0).equals(scResave)) {
                // Not a fatal error; the sketch has to stay open if
                // they're going to save the code that's in it.
                Messages.showWarning(
                    scResave.getFileName() + " deleted and not re-saved",
                    "Your main tab was deleted, and Processing couldn't " +
                    "resave it.\nYour sketch won't work without the " +
                    "main tab.", e);
              } else {
                Messages.showWarning("Could not re-save deleted tab",
                                     "Your copy of " + scResave.getPrettyName() +
                                     " will stay in the editor.", e);
              }
              ignoredRemovals.add(scResave);
              scResave.setModified(true); // So we'll have another go at
                                          // saving it later.
            }
          }
        );
      }
      editor.rebuildHeader();
      sketch.setCurrentCode(sketch.getCurrentCodeIndex());
      editor.repaintHeader();

      editor.sketchChanged();
    }

  }


  /**
   * Prompt the user what to do about each tab. Passes the tab to the user's
   * choice of Consumer. Won't let you delete the main tab.
   */
  private void showReloadPrompt(
      List<SketchCode> mergeConflict, List<SketchCode> removed,
      Consumer<SketchCode> modifiedReload, Consumer<SketchCode> modifiedKeep,
      Consumer<SketchCode> delete, Consumer<SketchCode> deletedResave) {
    for (SketchCode sc : mergeConflict) {
      if (1 == Messages.showCustomQuestion(editor,
          Language.text("change_detect.reload.title"),
          Language.interpolate("change_detect.reload.question", sc.getFileName()),
          Language.text("change_detect.reload.comment"),
          0,
          Language.text("change_detect.button.keep"),
          Language.text("change_detect.button.load_new"))) {
        modifiedReload.accept(sc);
      } else {
        modifiedKeep.accept(sc);
      }
    }

    for (SketchCode sc : removed) {
      if (!sketch.getCode(0).equals(sc) &&
          1 == Messages.showCustomQuestion(editor,
          Language.text("change_detect.delete.title"),
          Language.interpolate("change_detect.delete.question", sc.getFileName()),
          Language.text("change_detect.delete.comment"),
          0,
          Language.text("change_detect.button.resave"),
          Language.text("change_detect.button.discard"))) {
        delete.accept(sc);
      } else {
        deletedResave.accept(sc);
      }
    }
  }
}
