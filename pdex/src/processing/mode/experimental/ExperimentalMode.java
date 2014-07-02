/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2012 The Processing Foundation

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

package processing.mode.experimental;

import galsasson.mode.tweak.SketchParser;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.ImageIcon;

import processing.app.Base;
import processing.app.Editor;
import processing.app.EditorState;
import processing.app.Library;
import processing.app.Mode;
import processing.app.Preferences;
import processing.app.RunnerListener;
import processing.app.Sketch;
import processing.app.SketchCode;
import processing.app.SketchException;
import processing.mode.java.JavaBuild;
import processing.mode.java.JavaMode;
import processing.mode.java.runner.Runner;


/**
 * Experimental Mode for Processing, combines Debug Mode and XQMode and 
 * starts us working toward our next generation editor/debugger setup.
 */
public class ExperimentalMode extends JavaMode {
  public static final boolean VERBOSE_LOGGING = true;
  //public static final boolean VERBOSE_LOGGING = false;  
  public static final int LOG_SIZE = 512 * 1024; // max log file size (in bytes)
  public static boolean DEBUG = !true;
  
  public ExperimentalMode(Base base, File folder) {
    super(base, folder);

    // use libraries folder from javamode. will make sketches using core libraries work, as well as import libraries and examples menus
    for (Mode m : base.getModeList()) {
      if (m.getClass() == JavaMode.class) {
        JavaMode jMode = (JavaMode) m;
        librariesFolder = jMode.getLibrariesFolder();
        rebuildLibraryList();
        break;
      }
    }

    // Fetch examples and reference from java mode
    // thx to Manindra (https://github.com/martinleopold/DebugMode/issues/4)
    examplesFolder = Base.getContentFile("modes/java/examples");
    // https://github.com/martinleopold/DebugMode/issues/6
    referenceFolder = Base.getContentFile("modes/java/reference");

    // set logging level
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
      Logger.getLogger(ExperimentalMode.class.getName()).log(Level.SEVERE, null, ex);
    } catch (SecurityException ex) {
      Logger.getLogger(ExperimentalMode.class.getName()).log(Level.SEVERE, null, ex);
    }

    // disable initial chattiness for now
//    // output version from manifest file
//    Package p = ExperimentalMode.class.getPackage();
//    String titleAndVersion = p.getImplementationTitle() + " (v" + p.getImplementationVersion() + ")";
//    //log(titleAndVersion);
//    Logger.getLogger(ExperimentalMode.class.getName()).log(Level.INFO, titleAndVersion);
    loadPreferences();
    loadIcons();
  }


  @Override
  public String getTitle() {
    return "PDE X";
  }
  
  
  public File[] getKeywordFiles() {
    return new File[] { 
      Base.getContentFile("modes/java/keywords.txt") 
    };
  }
  
  public File getContentFile(String path) {
    // workaround for #45
    if (path.startsWith("application" + File.separator)) {
      return new File(Base.getContentFile("modes" + File.separator + "java")
          .getAbsolutePath() + File.separator + path);
    }
    return new File(folder, path);
  }
  
  volatile public static boolean errorCheckEnabled = true,
      warningsEnabled = true, codeCompletionsEnabled = true,
      debugOutputEnabled = false, errorLogsEnabled = false,
      autoSaveEnabled = true, autoSavePromptEnabled = true,
      defaultAutoSaveEnabled = true, // ,untitledAutoSaveEnabled;
      ccTriggerEnabled = false;
  public static int autoSaveInterval = 3; //in minutes
  
  /**
   * After how many typed characters, code completion is triggered
   */
  volatile public static int codeCompletionTriggerLength = 2;

  public static final String prefErrorCheck = "pdex.errorCheckEnabled",
      prefWarnings = "pdex.warningsEnabled",
      prefCodeCompletionEnabled = "pdex.ccEnabled",
      prefDebugOP = "pdex.dbgOutput",
      prefErrorLogs = "pdex.writeErrorLogs",
      prefAutoSaveInterval = "pdex.autoSaveInterval",
      prefAutoSave = "pdex.autoSave.autoSaveEnabled", // prefUntitledAutoSave = "pdex.autoSave.untitledAutoSaveEnabled", 
      prefAutoSavePrompt = "pdex.autoSave.promptDisplay",
      prefDefaultAutoSave = "pdex.autoSave.autoSaveByDefault",
      prefCCTriggerEnabled = "pdex.ccTriggerEnabled";

  // TweakMode code (Preferences)
  volatile public static boolean enableTweak = false;
  public static final String prefEnableTweak = "pdex.enableTweak";

  public void loadPreferences() {
    log("Load PDEX prefs");
    ensurePrefsExist();
    errorCheckEnabled = Preferences.getBoolean(prefErrorCheck);
    warningsEnabled = Preferences.getBoolean(prefWarnings);
    codeCompletionsEnabled = Preferences.getBoolean(prefCodeCompletionEnabled);
    DEBUG = Preferences.getBoolean(prefDebugOP);
    errorLogsEnabled = Preferences.getBoolean(prefErrorLogs);
    autoSaveInterval = Preferences.getInteger(prefAutoSaveInterval);
//    untitledAutoSaveEnabled = Preferences.getBoolean(prefUntitledAutoSave);
    autoSaveEnabled = Preferences.getBoolean(prefAutoSave);
    autoSavePromptEnabled = Preferences.getBoolean(prefAutoSavePrompt);
    defaultAutoSaveEnabled = Preferences.getBoolean(prefDefaultAutoSave);
    ccTriggerEnabled = Preferences.getBoolean(prefCCTriggerEnabled);

    // TweakMode code
    enableTweak = Preferences.getBoolean(prefEnableTweak);
  }

  public void savePreferences() {
    log("Saving PDEX prefs");
    Preferences.setBoolean(prefErrorCheck, errorCheckEnabled);
    Preferences.setBoolean(prefWarnings, warningsEnabled);
    Preferences.setBoolean(prefCodeCompletionEnabled, codeCompletionsEnabled);
    Preferences.setBoolean(prefDebugOP, DEBUG);
    Preferences.setBoolean(prefErrorLogs, errorLogsEnabled);
    Preferences.setInteger(prefAutoSaveInterval, autoSaveInterval);
//    Preferences.setBoolean(prefUntitledAutoSave,untitledAutoSaveEnabled);
    Preferences.setBoolean(prefAutoSave, autoSaveEnabled);
    Preferences.setBoolean(prefAutoSavePrompt, autoSavePromptEnabled);
    Preferences.setBoolean(prefDefaultAutoSave, defaultAutoSaveEnabled);
    Preferences.setBoolean(prefCCTriggerEnabled, ccTriggerEnabled);

    // TweakMode code
    Preferences.setBoolean(prefEnableTweak, enableTweak);
  }

  public void ensurePrefsExist() {
    //TODO: Need to do a better job of managing prefs. Think lists.
    if (Preferences.get(prefErrorCheck) == null)
      Preferences.setBoolean(prefErrorCheck, errorCheckEnabled);
    if (Preferences.get(prefWarnings) == null)
      Preferences.setBoolean(prefWarnings, warningsEnabled);
    if (Preferences.get(prefCodeCompletionEnabled) == null)
      Preferences.setBoolean(prefCodeCompletionEnabled, codeCompletionsEnabled);
    if (Preferences.get(prefDebugOP) == null)
      Preferences.setBoolean(prefDebugOP, DEBUG);
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
    if (Preferences.get(prefCCTriggerEnabled) == null)
      Preferences.setBoolean(prefCCTriggerEnabled, ccTriggerEnabled);

    // TweakMode code
    if (Preferences.get(prefEnableTweak) == null) {
    	Preferences.setBoolean(prefEnableTweak, enableTweak);
    }
  }


  /**
   * Create a new editor associated with this mode.
   */
  @Override
  public Editor createEditor(Base base, String path, EditorState state) {
    return new DebugEditor(base, path, state, this);
  }


  /**
   * Load a String value from theme.txt
   *
   * @param attribute the attribute key to load
   * @param defaultValue the default value
   * @return the attributes value, or the default value if the attribute
   * couldn't be loaded
   */
  public String loadThemeString(String attribute, String defaultValue) {
    String newString = theme.get(attribute);
    if (newString != null) {
      return newString;
    }
    Logger.getLogger(getClass().getName()).log(Level.WARNING, "Error loading String: {0}", attribute);
    return defaultValue;
  }
    

  /**
   * Load a Color value from theme.txt
   *
   * @param attribute the attribute key to load
   * @param defaultValue the default value
   * @return the attributes value, or the default value if the attribute
   * couldn't be loaded
   */
  public Color getThemeColor(String attribute, Color defaultValue) {
    Color newColor = theme.getColor(attribute);
    if (newColor != null) {
      return newColor;
    }
    log("error loading color: " + attribute);
    Logger.getLogger(ExperimentalMode.class.getName()).log(Level.WARNING, "Error loading Color: {0}", attribute);
    return defaultValue;
  }
  
  protected ImageIcon classIcon, fieldIcon, methodIcon, localVarIcon;
  protected void loadIcons(){
    String iconPath = getContentFile("data")
        .getAbsolutePath()
        + File.separator + "icons";
    classIcon = new ImageIcon(iconPath + File.separator + "class_obj.png");
    methodIcon = new ImageIcon(iconPath + File.separator
        + "methpub_obj.png");
    fieldIcon = new ImageIcon(iconPath + File.separator
        + "field_protected_obj.png"); 
    localVarIcon = new ImageIcon(iconPath + File.separator
                              + "field_default_obj.png");
    log("Icons loaded");
  }

    
  public ClassLoader getJavaModeClassLoader() {
    for (Mode m : base.getModeList()) {
      if (m.getClass() == JavaMode.class) {
        JavaMode jMode = (JavaMode) m;
        return jMode.getClassLoader();
      }
    }
    // badness
    return null;
  }
  
  /**
   * System.out.println()
   */
  public static final void log(Object message){
    if(ExperimentalMode.DEBUG)
      System.out.println(message);
  }
  
  /**
   * System.err.println()
   */
  public static final void logE(Object message){
    if(ExperimentalMode.DEBUG)
      System.err.println(message);
  }
  
  /**
   * System.out.print
   */
  public static final void log2(Object message){
    if(ExperimentalMode.DEBUG)
      System.out.print(message);
  }
  
  public String[] getIgnorable() {
    return new String[] {
      "applet",
      "application.macosx",
      "application.windows",
      "application.linux",
      "_autosave"
    };
  }

  // TweakMode code
	@Override
	public Runner handleRun(Sketch sketch, RunnerListener listener) throws SketchException
	{
		if (enableTweak) {
			return handleTweakPresentOrRun(sketch, listener, false);
		}
		else {
			/* Do the usual (JavaMode style) */
		    JavaBuild build = new JavaBuild(sketch);
		    String appletClassName = build.build(false);
		    if (appletClassName != null) {
		      final Runner runtime = new Runner(build, listener);
		      new Thread(new Runnable() {
		        public void run() {
		          runtime.launch(false);  // this blocks until finished
		        }
		      }).start();
		      return runtime;
		    }
		    return null;
		}
	}

	@Override
	public Runner handlePresent(Sketch sketch, RunnerListener listener) throws SketchException
	{
		if (enableTweak) {
			return handleTweakPresentOrRun(sketch, listener, true);
		}
		else {
			/* Do the usual (JavaMode style) */
		    JavaBuild build = new JavaBuild(sketch);
		    String appletClassName = build.build(false);
		    if (appletClassName != null) {
		      final Runner runtime = new Runner(build, listener);
		      new Thread(new Runnable() {
		        public void run() {
		          runtime.launch(true);
		        }
		      }).start();
		      return runtime;
		    }
		    return null;
		}
	}

	public Runner handleTweakPresentOrRun(Sketch sketch, RunnerListener listener, boolean present) throws SketchException
	{
		final DebugEditor editor = (DebugEditor)listener;
		final boolean toPresent = present;

		if (!verifyOscP5()) {
			editor.deactivateRun();
			return null;
		}

		boolean launchInteractive = false;

		if (isSketchModified(sketch)) {
			editor.deactivateRun();
			Base.showMessage("Save", "Please save the sketch before running in Tweak Mode.");
			return null;
		}

		/* first try to build the unmodified code */
		JavaBuild build = new JavaBuild(sketch);
		String appletClassName = build.build(false);
		if (appletClassName == null) {
			// unmodified build failed, so fail
			return null;
		}

		/* if compilation passed, modify the code and build again */
		editor.initBaseCode();
		// check for "// tweak" comment in the sketch
		boolean requiresTweak = SketchParser.containsTweakComment(editor.baseCode);
		// parse the saved sketch to get all (or only with "//tweak" comment) numbers
		final SketchParser parser = new SketchParser(editor.baseCode, requiresTweak);

		// add our code to the sketch
		launchInteractive = editor.automateSketch(sketch, parser.allHandles);

		build = new JavaBuild(sketch);
		appletClassName = build.build(false);

		if (appletClassName != null) {
			final Runner runtime = new Runner(build, listener);
			new Thread(new Runnable() {
				public void run() {
					runtime.launch(toPresent);  // this blocks until finished

					// executed when the sketch quits
					editor.initEditorCode(parser.allHandles, false);
					editor.stopInteractiveMode(parser.allHandles);
				}

			}).start();

			if (launchInteractive) {

				// replace editor code with baseCode
				editor.initEditorCode(parser.allHandles, false);
				editor.updateInterface(parser.allHandles, parser.colorBoxes);
				editor.startInteractiveMode();
			}

			return runtime;
		}

		return null;
	}

	private boolean verifyOscP5()
	{
		for (Library l : contribLibraries) {
			if (l.getName().equals("oscP5")) {
				return true;
			}
		}

		// could not find oscP5 library
		Base.showWarning("Tweak Mode", "Tweak Mode needs the 'oscP5' library.\n"
				+ "Please install this library by clicking \"Sketch --> Import Library --> Add Library ...\" and choose 'ocsP5'", null);

		return false;
	}

	private boolean isSketchModified(Sketch sketch)
	{
		for (SketchCode sc : sketch.getCode()) {
			if (sc.isModified()) {
				return true;
			}
		}
		return false;
	}

}
