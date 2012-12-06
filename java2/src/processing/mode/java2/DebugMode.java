/*
 * Copyright (C) 2012 Martin Leopold <m@martinleopold.com>
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package processing.mode.java2;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import processing.app.Base;
import processing.app.EditorState;
import processing.app.Mode;
import processing.mode.java.JavaMode;

/**
 * Debug Mode for Processing. Built on top of JavaMode.
 *
 * @author Martin Leopold <m@martinleopold.com>
 */
public class DebugMode extends JavaMode {
  public static final boolean VERBOSE_LOGGING = true;
  //public static final boolean VERBOSE_LOGGING = false;  
  public static final int LOG_SIZE = 512 * 1024; // max log file size (in bytes)

  
  public DebugMode(Base base, File folder) {
    super(base, folder);

        // use libraries folder from javamode. will make sketches using core libraries work, as well as import libraries and examples menus
//        for (Mode m : base.getModeList()) {
//            if (m.getClass() == JavaMode.class) {
//                JavaMode jMode = (JavaMode) m;
//                librariesFolder = jMode.getLibrariesFolder();
//                rebuildLibraryList();
//                break;
//            }
//        }

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
      Logger.getLogger(DebugMode.class.getName()).log(Level.SEVERE, null, ex);
    } catch (SecurityException ex) {
      Logger.getLogger(DebugMode.class.getName()).log(Level.SEVERE, null, ex);
    }

    // output version from manifest file
    Package p = DebugMode.class.getPackage();
    String titleAndVersion = p.getImplementationTitle() + " (v" + p.getImplementationVersion() + ")";
    //System.out.println(titleAndVersion);
    Logger.getLogger(DebugMode.class.getName()).log(Level.INFO, titleAndVersion);
  }


  @Override
  public String getTitle() {
    return "Experimental";
  }


  /**
   * Create a new editor associated with this mode.
   */
  @Override
  public processing.app.Editor createEditor(Base base, String path, EditorState state) {
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
    Logger.getLogger(DebugMode.class.getName()).log(Level.WARNING, "Error loading String: {0}", attribute);
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
    System.out.println("error loading color: " + attribute);
    Logger.getLogger(DebugMode.class.getName()).log(Level.WARNING, "Error loading Color: {0}", attribute);
    return defaultValue;
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
}
