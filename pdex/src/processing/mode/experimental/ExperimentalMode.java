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
import processing.app.Mode;
import processing.app.Preferences;
import processing.mode.java.JavaMode;


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
  
  volatile public static boolean errorCheckEnabled = true, warningsEnabled = true,
      codeCompletionsEnabled = true, debugOutputEnabled = false, errorLogsEnabled = false;
  public static int autoSaveInterval = 3; //in minutes

  public static final String prefErrorCheck = "pdex.errorCheckEnabled",
      prefWarnings = "pdex.warningsEnabled",
      prefCodeCompletionEnabled = "pdex.ccEnabled",
      prefDebugOP = "pdex.dbgOutput", prefErrorLogs = "pdex.writeErrorLogs", prefAutoSaveInterval = "pdex.autoSaveInterval";
  
  public void loadPreferences(){
    log("Load PDEX prefs");
    ensurePrefsExist();
    errorCheckEnabled = Preferences.getBoolean(prefErrorCheck);
    warningsEnabled = Preferences.getBoolean(prefWarnings);
    codeCompletionsEnabled = Preferences.getBoolean(prefCodeCompletionEnabled);
    DEBUG = Preferences.getBoolean(prefDebugOP);
    errorLogsEnabled = Preferences.getBoolean(prefErrorLogs);
    autoSaveInterval = Preferences.getInteger(prefAutoSaveInterval);
  }
  
  public void savePreferences(){
    log("Saving PDEX prefs");
    Preferences.setBoolean(prefErrorCheck, errorCheckEnabled);
    Preferences.setBoolean(prefWarnings, warningsEnabled);
    Preferences.setBoolean(prefCodeCompletionEnabled, codeCompletionsEnabled);
    Preferences.setBoolean(prefDebugOP, DEBUG);
    Preferences.setBoolean(prefErrorLogs,errorLogsEnabled);
    Preferences.setInteger(prefAutoSaveInterval,autoSaveInterval);
  }
  
  public void ensurePrefsExist(){
    if(Preferences.get(prefErrorCheck) == null) 
      Preferences.setBoolean(prefErrorCheck,errorCheckEnabled);
    if(Preferences.get(prefWarnings) == null) 
      Preferences.setBoolean(prefWarnings,warningsEnabled);
    if(Preferences.get(prefCodeCompletionEnabled) == null) 
      Preferences.setBoolean(prefCodeCompletionEnabled,codeCompletionsEnabled);
    if(Preferences.get(prefDebugOP) == null) 
      Preferences.setBoolean(prefDebugOP,DEBUG);
    if(Preferences.get(prefErrorLogs) == null) 
      Preferences.setBoolean(prefErrorLogs,errorLogsEnabled);
    if(Preferences.get(prefAutoSaveInterval) == null) 
      Preferences.setInteger(prefAutoSaveInterval,autoSaveInterval);
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
}
