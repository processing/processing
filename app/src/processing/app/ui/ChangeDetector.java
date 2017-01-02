package processing.app.ui;

import java.awt.EventQueue;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.JDialog;
import javax.swing.JOptionPane;

import processing.app.Language;
import processing.app.Messages;
import processing.app.Preferences;
import processing.app.Sketch;
import processing.app.SketchCode;


public class ChangeDetector implements WindowFocusListener {
  private final Sketch sketch;
  private final Editor editor;

  private List<String> ignoredAdditions = new ArrayList<>();
  private List<SketchCode> ignoredRemovals = new ArrayList<>();

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
        // This resolves #4805. The ensureExistence() method does not need to be called if the sketch gains focus
        // from the dialog box that informs the user about a missing sketch- if the missing sketch was saved
        // successfully, all is well; if not, the user has been warned and asked to save it manually. Similarly,
        // if processing could not re-save the sketch, don't call ensureExistence(), but allow the user to save
        // manually.
        if (!(e.getOppositeWindow() instanceof JDialog &&
          (((JDialog)e.getOppositeWindow()).getTitle().equals(Language.text("ensure_exist.messages.missing_sketch")) ||
            ((JDialog)e.getOppositeWindow()).getTitle().equals(Language.text("ensure_exist.messages.unrecoverable"))))) {
          // make sure the sketch folder exists at all.
          // if it does not, it will be re-saved, and no changes will be detected
          sketch.ensureExistence(); // <- touches UI, stay on EDT
        }

        // TODO: Not sure if we even need to run this async. Usually takes
        //   just a few ms and we probably want to prevent any changes from
        //   users until the external changes are sorted out. [jv 2016-12-05]

        // Run task in common pool, starting threads directly is so Java 6
        ForkJoinPool.commonPool().execute(this::checkFiles);
      }
    }
  }


  @Override
  public void windowLostFocus(WindowEvent e) {
    // Shouldn't need to do anything here, and not storing anything here b/c we
    // don't want to assume a loss of focus is required before change detection
  }


  // Synchronize, we are running async and touching fields
  private synchronized void checkFiles() {

    List<String> filenames = new ArrayList<>();
    sketch.getSketchCodeFiles(filenames, null);

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

    // Show prompt if there are any added files which were not previously ignored
    boolean added = addedFilenames.stream()
        .anyMatch(f -> !ignoredAdditions.contains(f));


    // REMOVED FILES

    // Get codes which don't have file
    List<SketchCode> removedCodes = Optional.ofNullable(existsMap.get(Boolean.FALSE))
        .orElse(Collections.emptyList());

    // Show prompt if there are any removed codes which were not previously ignored
    boolean removed = removedCodes.stream()
        .anyMatch(code -> !ignoredRemovals.contains(code));


    /// MODIFIED FILES

    // Get codes which have file with different modification time
    List<SketchCode> modifiedCodes = Optional.ofNullable(existsMap.get(Boolean.TRUE))
        .orElse(Collections.emptyList())
        .stream()
        .filter(code -> {
          long fileLastModified = code.getFile().lastModified();
          long codeLastModified = code.getLastModified();
          long diff = fileLastModified - codeLastModified;
          return fileLastModified == 0L || diff > MODIFICATION_WINDOW_MILLIS;
        })
        .collect(Collectors.toList());

    // Show prompt if any open codes were modified
    boolean modified = !modifiedCodes.isEmpty();


    boolean ask = added || removed || modified;

    if (DEBUG) {
      System.out.println("ask: " + ask + "\n" +
                             "added filenames: " + addedFilenames + ",\n" +
                             "ignored added: " + ignoredAdditions + ",\n" +
                             "removed codes: " + removedCodes + ",\n" +
                             "ignored removed: " + ignoredRemovals + ",\n" +
                             "modified codes: " + modifiedCodes);
    }


    // This has to happen in one go and also touches UI everywhere. It has to
    // run on EDT, otherwise windowGainedFocus callback runs again right after
    // dismissing the prompt and we get another prompt before we even finished.
    try {
      // Wait for EDT to finish its business
      // We need to stay in synchronized scope because of ignore lists
      EventQueue.invokeAndWait(() -> {
        // Show prompt if something interesting happened
        if (ask && showReloadPrompt()) {
          // She said yes!!!
          if (sketch.getMainFile().exists()) {
            sketch.reload();
            editor.rebuildHeader();
          } else {
            // If the main file was deleted, and that's why we're here,
            // then we need to re-save the sketch instead.
            // Mark everything as modified so that it saves properly
            for (SketchCode code : codes) {
              code.setModified(true);
            }
            try {
              sketch.save();
            } catch (Exception e) {
              //if that didn't work, tell them it's un-recoverable
              Messages.showError("Reload Failed", "The main file for this sketch was deleted\n" +
                  "and could not be rewritten.", e);
            }
          }

          // Sketch was reloaded, clear ignore lists
          ignoredAdditions.clear();
          ignoredRemovals.clear();

          return;
        }

        // Update ignore lists to get rid of old stuff
        ignoredAdditions = addedFilenames;
        ignoredRemovals = removedCodes;

        // If something changed, set modified flags and modification times
        if (!removedCodes.isEmpty() || !modifiedCodes.isEmpty()) {
          Stream.concat(removedCodes.stream(), modifiedCodes.stream())
              .forEach(code -> {
                code.setModified(true);
                code.setLastModified();
              });

          // Not sure if this is needed
          editor.rebuildHeader();
        }
      });
    } catch (InterruptedException ignore) {
    } catch (InvocationTargetException e) {
      Messages.loge("exception in ChangeDetector", e);
    }

  }


  /**
   * Prompt the user whether to reload the sketch. If the user says yes,
   * perform the actual reload.
   * @return true if user said yes, false if they hit No or closed the window
   */
  private boolean showReloadPrompt() {
    int response = Messages
        .showYesNoQuestion(editor, "File Modified",
                           "Your sketch has been modified externally.<br>" +
                               "Would you like to reload the sketch?",
                           "If you reload the sketch, any unsaved changes will be lost.");
    return response == JOptionPane.YES_OPTION;
  }
}
