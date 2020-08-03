/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2010-11 Ben Fry and Casey Reas
  Copyright (c) 2012-15 The Processing Foundation

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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.SwingUtilities;

import processing.app.*;
import processing.app.ui.Editor;
import processing.app.ui.EditorException;
import processing.app.ui.EditorState;
import processing.mode.java.runner.Runner;
import processing.mode.java.tweak.SketchParser;


public class JavaMode extends Mode {

  public Editor createEditor(Base base, String path,
                             EditorState state) throws EditorException {
    return new JavaEditor(base, path, state, this);
  }


  public JavaMode(Base base, File folder) {
    super(base, folder);

//    initLogger();
    loadPreferences();
  }


  public String getTitle() {
    return "Java";
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  public File[] getExampleCategoryFolders() {
    return new File[] {
      new File(examplesFolder, "Basics"),
      new File(examplesFolder, "Topics"),
      new File(examplesFolder, "Demos"),
      new File(examplesFolder, "Books")
    };
  }


  public String getDefaultExtension() {
    return "pde";
  }


  public String[] getExtensions() {
    return new String[] { "pde", "java" };
  }


  public String[] getIgnorable() {
    return new String[] {
      "applet",
      "application.macosx",
      "application.windows",
      "application.linux"
    };
  }


  public Library getCoreLibrary() {
    if (coreLibrary == null) {
      File coreFolder = Platform.getContentFile("core");
      coreLibrary = new Library(coreFolder);
//      try {
//        coreLibrary = getLibrary("processing.core");
//        System.out.println("core found at " + coreLibrary.getLibraryPath());
//      } catch (SketchException e) {
//        Base.log("Serious problem while locating processing.core", e);
//      }
    }
    return coreLibrary;
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .



  /** Handles the standard Java "Run" or "Present" */
  public Runner handleLaunch(Sketch sketch, RunnerListener listener,
                             final boolean present) throws SketchException {
    JavaBuild build = new JavaBuild(sketch);
//    String appletClassName = build.build(false);
    String appletClassName = build.build(true);
    if (appletClassName != null) {
      final Runner runtime = new Runner(build, listener);
      new Thread(new Runnable() {
        public void run() {
          // these block until finished
          if (present) {
            runtime.present(null);
          } else {
            runtime.launch(null);
          }
        }
      }).start();
      return runtime;
    }
    return null;
  }


  /** Start a sketch in tweak mode */
  public Runner handleTweak(Sketch sketch,
                            RunnerListener listener, JavaEditor editor) throws SketchException {

    // first try to build the unmodified code
    JavaBuild build = new JavaBuild(sketch);
//    String appletClassName = build.build(false);
    String appletClassName = build.build(true);
    if (appletClassName == null) {
      // unmodified build failed, so fail
      return null;
    }

    // if compilation passed, modify the code and build again
    // save the original sketch code of the user
    editor.initBaseCode();
    // check for "// tweak" comment in the sketch
    boolean requiresTweak = SketchParser.containsTweakComment(editor.baseCode);
    // parse the saved sketch to get all (or only with "//tweak" comment) numbers
    final SketchParser parser = new SketchParser(editor.baseCode, requiresTweak);

    // add our code to the sketch
    final boolean launchInteractive = editor.automateSketch(sketch, parser);

    build = new JavaBuild(sketch);
    appletClassName = build.build(false);

    if (appletClassName != null) {
      final Runner runtime = new Runner(build, listener);
      new Thread(new Runnable() {
        public void run() {
          // these block until finished
//          if (present) {
//            runtime.present(null);
//          } else {
          runtime.launch(null);
//          }
          // next lines are executed when the sketch quits
          if (launchInteractive) {
            // fix swing deadlock issue: https://github.com/processing/processing/issues/3928
            SwingUtilities.invokeLater(new Runnable() {
              public void run() {
                editor.initEditorCode(parser.allHandles, false);
                editor.stopTweakMode(parser.allHandles);
              }
            });
          }
        }
      }).start();

      if (launchInteractive) {
        // fix swing deadlock issue: https://github.com/processing/processing/issues/3928
        SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            // replace editor code with baseCode
            editor.initEditorCode(parser.allHandles, false);
            editor.updateInterface(parser.allHandles, parser.colorBoxes);
            editor.startTweakMode();
          }
        });
      }
      return runtime;
    }
    return null;
  }


  /*
  // TODO Why is this necessary? Why isn't Sketch.isModified() used?
  static private boolean isSketchModified(Sketch sketch) {
    for (SketchCode sc : sketch.getCode()) {
      if (sc.isModified()) {
        return true;
      }
    }
    return false;
  }
  */


//  public void handleStop() {
//    if (runtime != null) {
//      runtime.close();  // kills the window
//      runtime = null; // will this help?
//    }
//  }


//  public boolean handleExportApplet(Sketch sketch) throws SketchException, IOException {
//    JavaBuild build = new JavaBuild(sketch);
//    return build.exportApplet();
//  }


  public boolean handleExportApplication(Sketch sketch) throws SketchException, IOException {
    JavaBuild build = new JavaBuild(sketch);
    return build.exportApplication();
  }


  /**
   * Any modes that extend JavaMode can override this method to add additional
   * JARs to be included in the classpath for code completion and error checking
   * @return searchPath: file-paths separated by File.pathSeparatorChar
   */
  public String getSearchPath() {
    return getCoreLibrary().getJarPath();
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

  // Merged from ExperimentalMode

  /*
  void initLogger() {
    final boolean VERBOSE_LOGGING = true;
    final int LOG_SIZE = 512 * 1024; // max log file size (in bytes)

    Logger globalLogger = Logger.getLogger("");
    //Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME); // doesn't work on os x
    if (VERBOSE_LOGGING) {
      globalLogger.setLevel(Level.INFO);
    } else {
      globalLogger.setLevel(Level.WARNING);
    }

    // enable logging to file
    try {
      // settings is writable for built-in modes, mode folder is not writable
      File logFolder = Base.getSettingsFile("debug");
      if (!logFolder.exists()) {
        logFolder.mkdir();
      }
      File logFile = new File(logFolder, "DebugMode.%g.log");
      Handler handler = new FileHandler(logFile.getAbsolutePath(), LOG_SIZE, 10, false);
      globalLogger.addHandler(handler);

    } catch (IOException ex) {
      Logger.getLogger(JavaMode.class.getName()).log(Level.SEVERE, null, ex);
    } catch (SecurityException ex) {
      Logger.getLogger(JavaMode.class.getName()).log(Level.SEVERE, null, ex);
    }
  }
  */


  //ImageIcon classIcon, fieldIcon, methodIcon, localVarIcon;

//  protected void loadIcons() {
//    String iconPath = getContentFile("data").getAbsolutePath() + File.separator + "icons";
//    classIcon = new ImageIcon(iconPath + File.separator + "class_obj.png");
//    methodIcon = new ImageIcon(iconPath + File.separator + "methpub_obj.png");
//    fieldIcon = new ImageIcon(iconPath + File.separator + "field_protected_obj.png");
//    localVarIcon = new ImageIcon(iconPath + File.separator + "field_default_obj.png");
//  }


  static public volatile boolean errorCheckEnabled = true;
  static public volatile boolean warningsEnabled = true;
  static public volatile boolean codeCompletionsEnabled = true;
  static public volatile boolean debugOutputEnabled = false;
  static public volatile boolean errorLogsEnabled = false;
  static public volatile boolean autoSaveEnabled = true;
  static public volatile boolean autoSavePromptEnabled = true;
  static public volatile boolean defaultAutoSaveEnabled = true;
  static public volatile boolean ccTriggerEnabled = false;
  static public volatile boolean importSuggestEnabled = true;
  static public volatile boolean inspectModeHotkeyEnabled = true;
  static public int autoSaveInterval = 3; //in minutes


  /**
   * After how many typed characters, code completion is triggered
   */
  volatile public static int codeCompletionTriggerLength = 1;

  static public final String prefErrorCheck = "pdex.errorCheckEnabled";
  static public final String prefWarnings = "pdex.warningsEnabled";
  static public final String prefDebugOP = "pdex.dbgOutput";
  static public final String prefErrorLogs = "pdex.writeErrorLogs";
  static public final String prefAutoSaveInterval = "pdex.autoSaveInterval";
  static public final String prefAutoSave = "pdex.autoSave.autoSaveEnabled";
  static public final String prefAutoSavePrompt = "pdex.autoSave.promptDisplay";
  static public final String prefDefaultAutoSave = "pdex.autoSave.autoSaveByDefault";
  static public final String suggestionsFileName = "suggestions.txt";

  static public final String COMPLETION_PREF = "pdex.completion";
  static public final String COMPLETION_TRIGGER_PREF = "pdex.completion.trigger";
  static public final String SUGGEST_IMPORTS_PREF = "pdex.suggest.imports";
  static public final String INSPECT_MODE_HOTKEY_PREF = "pdex.inspectMode.hotkey";

//  static volatile public boolean enableTweak = false;

  /**
   * Stores the white list/black list of allowed/blacklisted imports. These are defined in
   * suggestions.txt in java mode folder.
   */
  static public final Map<String, Set<String>> suggestionsMap = new HashMap<>();

  public void loadPreferences() {
    Messages.log("Load PDEX prefs");
    ensurePrefsExist();
    errorCheckEnabled = Preferences.getBoolean(prefErrorCheck);
    warningsEnabled = Preferences.getBoolean(prefWarnings);
    codeCompletionsEnabled = Preferences.getBoolean(COMPLETION_PREF);
//    DEBUG = Preferences.getBoolean(prefDebugOP);
    errorLogsEnabled = Preferences.getBoolean(prefErrorLogs);
    autoSaveInterval = Preferences.getInteger(prefAutoSaveInterval);
//    untitledAutoSaveEnabled = Preferences.getBoolean(prefUntitledAutoSave);
    autoSaveEnabled = Preferences.getBoolean(prefAutoSave);
    autoSavePromptEnabled = Preferences.getBoolean(prefAutoSavePrompt);
    defaultAutoSaveEnabled = Preferences.getBoolean(prefDefaultAutoSave);
    ccTriggerEnabled = Preferences.getBoolean(COMPLETION_TRIGGER_PREF);
    importSuggestEnabled = Preferences.getBoolean(SUGGEST_IMPORTS_PREF);
    inspectModeHotkeyEnabled = Preferences.getBoolean(INSPECT_MODE_HOTKEY_PREF);
    loadSuggestionsMap();
  }


  public void savePreferences() {
    Messages.log("Saving PDEX prefs");
    Preferences.setBoolean(prefErrorCheck, errorCheckEnabled);
    Preferences.setBoolean(prefWarnings, warningsEnabled);
    Preferences.setBoolean(COMPLETION_PREF, codeCompletionsEnabled);
//    Preferences.setBoolean(prefDebugOP, DEBUG);
    Preferences.setBoolean(prefErrorLogs, errorLogsEnabled);
    Preferences.setInteger(prefAutoSaveInterval, autoSaveInterval);
//    Preferences.setBoolean(prefUntitledAutoSave,untitledAutoSaveEnabled);
    Preferences.setBoolean(prefAutoSave, autoSaveEnabled);
    Preferences.setBoolean(prefAutoSavePrompt, autoSavePromptEnabled);
    Preferences.setBoolean(prefDefaultAutoSave, defaultAutoSaveEnabled);
    Preferences.setBoolean(COMPLETION_TRIGGER_PREF, ccTriggerEnabled);
    Preferences.setBoolean(SUGGEST_IMPORTS_PREF, importSuggestEnabled);
    Preferences.setBoolean(INSPECT_MODE_HOTKEY_PREF, inspectModeHotkeyEnabled);
  }

  public void loadSuggestionsMap() {
    File suggestionsListFile = new File(getFolder() + File.separator
        + suggestionsFileName);
    if (!suggestionsListFile.exists()) {
      Messages.loge("Suggestions file not found! "
          + suggestionsListFile.getAbsolutePath());
      return;
    }

    try {
      BufferedReader br = new BufferedReader(
                                             new FileReader(suggestionsListFile));
      while (true) {
        String line = br.readLine();
        if (line == null) {
          break;
        }
        line = line.trim();
        if (line.startsWith("#")) {
          continue;
        } else {
          if (line.contains("=")) {
            String key = line.split("=")[0];
            String val = line.split("=")[1];
            if (suggestionsMap.containsKey(key)) {
              suggestionsMap.get(key).add(val);
            } else {
              HashSet<String> set = new HashSet<>();
              set.add(val);
              suggestionsMap.put(key, set);
            }
          }
        }
      }
      br.close();
    } catch (IOException e) {
      Messages.loge("IOException while reading suggestions file:"
          + suggestionsListFile.getAbsolutePath());
    }
  }


  public void ensurePrefsExist() {
    //TODO: Need to do a better job of managing prefs. Think lists.
    if (Preferences.get(prefErrorCheck) == null)
      Preferences.setBoolean(prefErrorCheck, errorCheckEnabled);
    if (Preferences.get(prefWarnings) == null)
      Preferences.setBoolean(prefWarnings, warningsEnabled);
    if (Preferences.get(COMPLETION_PREF) == null)
      Preferences.setBoolean(COMPLETION_PREF, codeCompletionsEnabled);
    if (Preferences.get(prefDebugOP) == null)
//      Preferences.setBoolean(prefDebugOP, DEBUG);
    if (Preferences.get(prefErrorLogs) == null)
      Preferences.setBoolean(prefErrorLogs, errorLogsEnabled);
    if (Preferences.get(prefAutoSaveInterval) == null)
      Preferences.setInteger(prefAutoSaveInterval, autoSaveInterval);
//    if(Preferences.get(prefUntitledAutoSave) == null)
//      Preferences.setBoolean(prefUntitledAutoSave,untitledAutoSaveEnabled);
    if (Preferences.get(prefAutoSave) == null)
      Preferences.setBoolean(prefAutoSave, autoSaveEnabled);
    if (Preferences.get(prefAutoSavePrompt) == null)
      Preferences.setBoolean(prefAutoSavePrompt, autoSavePromptEnabled);
    if (Preferences.get(prefDefaultAutoSave) == null)
      Preferences.setBoolean(prefDefaultAutoSave, defaultAutoSaveEnabled);
    if (Preferences.get(COMPLETION_TRIGGER_PREF) == null)
      Preferences.setBoolean(COMPLETION_TRIGGER_PREF, ccTriggerEnabled);
    if (Preferences.get(SUGGEST_IMPORTS_PREF) == null)
      Preferences.setBoolean(SUGGEST_IMPORTS_PREF, importSuggestEnabled);
    if (Preferences.get(INSPECT_MODE_HOTKEY_PREF) == null)
      Preferences.setBoolean(INSPECT_MODE_HOTKEY_PREF, inspectModeHotkeyEnabled);
  }


  static public void main(String[] args) {
    processing.app.Base.main(args);
  }
}
