/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2011 Andres Colubri

  This library is free software; you can redistribute it and/or
  modify it under the terms of the GNU Lesser General Public
  License as published by the Free Software Foundation; either
  version 2.1 of the License, or (at your option) any later version.

  This library is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  Lesser General Public License for more details.

  You should have received a copy of the GNU Lesser General
  Public License along with this library; if not, write to the
  Free Software Foundation, Inc., 59 Temple Place, Suite 330,
  Boston, MA  02111-1307  USA
*/

package processing.video;

import org.gstreamer.*;
import processing.core.PApplet;
import processing.core.PConstants;
import java.io.File;
import java.util.List;

/**
 * This class contains some basic functions used by the rest of the classes in
 * this library.
 */
public class Video implements PConstants {
  protected static long INSTANCES_COUNT = 0;

  protected static String gstreamerBinPath = "";
  protected static String gstreamerPluginsPath = "";

  protected static boolean defaultGLibContext = false;
  
  // Priority is given to global install of GStreamer if this is set to true. 
  public static boolean globalGStreamer = false;
 
  // Direct buffer pass enabled by default.
  public static boolean passDirectBuffer = true; 
    
  public static String globalGStreamerPath;
  public static String globalPluginsFolder = "gstreamer-0.10";
  // Default locations of the global install of gstreamer for each platform:
  static {
    if (PApplet.platform == MACOSX) {
      globalGStreamerPath = "/System/Library/Frameworks/GStreamer.framework/Versions/Current/lib";
    } else if (PApplet.platform == WINDOWS) {
      globalGStreamerPath = "";
    } else if (PApplet.platform == LINUX) {
      globalGStreamerPath = "/usr/lib";
    } else {}
  }
  
  protected static int bitsJVM;
  static {
    bitsJVM = PApplet.parseInt(System.getProperty("sun.arch.data.model"));
  }  
  
  // Default location of the local install of gstreamer. Suggested by Charles Bourasseau. 
  // When it is left as empty string, GSVideo will attempt to use the path from GSLibraryPath.get(),
  // otherwise it will use it as the path to the folder where the libgstreamer.dylib and other 
  // files are located.  
  public static String localGStreamerPath = "";
  public static String localPluginsFolder = "plugins";
  
  // Some constants to identify AUDIO, VIDEO and RAW streams.
  static public final int AUDIO = 0;
  static public final int VIDEO = 1;
  static public final int RAW = 2;
  
  static public void init() {
    if (INSTANCES_COUNT == 0) {
      initImpl();
    }
    INSTANCES_COUNT++;
  }
  
  static public void restart() {
    removePlugins();
    Gst.deinit();
    initImpl();
  }
    
  static protected void initImpl() {
    if (PApplet.platform == LINUX) {
      // Linux only supports global gstreamer for now.
      globalGStreamer = true;         
      setLinuxPath();
    } else if (PApplet.platform == WINDOWS) {
      setWindowsPath();
    } else if (PApplet.platform == MACOSX) {
      setMacOSXPath();
    }

    if (!gstreamerBinPath.equals("")) {
      System.setProperty("jna.library.path", gstreamerBinPath);
    }

    if ((PApplet.platform == LINUX) && !globalGStreamer) {
      System.err.println("Loading local version of GStreamer not supported in Linux at this time.");
    }

    if ((PApplet.platform == WINDOWS) && !globalGStreamer) {
      LibraryLoader loader = LibraryLoader.getInstance();
      if (loader == null) {
        System.err.println("Cannot load local version of GStreamer libraries.");
      }
    }

    if ((PApplet.platform == MACOSX) && !globalGStreamer) {
      // Nothing to do here, since the dylib mechanism in OSX doesn't require the
      // library loader.      
    }    
    
    String[] args = { "" };
    Gst.setUseDefaultContext(defaultGLibContext);
    Gst.init("Processing core video", args);

    addPlugins();
  }

  static protected void addPlugins() {
    if (!gstreamerPluginsPath.equals("")) {
      Registry reg = Registry.getDefault();
      boolean res;
      res = reg.scanPath(gstreamerPluginsPath);
      if (!res) {
        System.err.println("Cannot load GStreamer plugins from " + gstreamerPluginsPath);
      }
    }       
  }
  
  static protected void removePlugins() {
    Registry reg = Registry.getDefault();
    List<Plugin> list = reg.getPluginList();
    for (Plugin plg : list) {
      reg.removePlugin(plg);
    }    
  }
  
  static protected void setLinuxPath() {
    if (globalGStreamer && lookForGlobalGStreamer()) {
      gstreamerBinPath = "";
      gstreamerPluginsPath = "";
    } else {
      globalGStreamer = false;
      if (localGStreamerPath.equals("")) {
        LibraryPath libPath = new LibraryPath();
        String path = libPath.get();
        gstreamerBinPath = buildGStreamerBinPath(path, "/linux" + bitsJVM);
        gstreamerPluginsPath = gstreamerBinPath + "/" + localPluginsFolder;        
      } else {
        gstreamerBinPath = localGStreamerPath;
        gstreamerPluginsPath = localGStreamerPath + "/" + localPluginsFolder;
      }       
    }
  }

  static protected void setWindowsPath() {
    if (globalGStreamer && lookForGlobalGStreamer()) {
      gstreamerBinPath = "";
      gstreamerPluginsPath = "";
    } else {
      globalGStreamer = false;
      if (localGStreamerPath.equals("")) {
        LibraryPath libPath = new LibraryPath();
        String path = libPath.get();
        gstreamerBinPath = buildGStreamerBinPath(path, "\\windows" + bitsJVM);
        gstreamerPluginsPath = gstreamerBinPath + "\\" + localPluginsFolder;
      } else {
        gstreamerBinPath = localGStreamerPath;
        gstreamerPluginsPath = localGStreamerPath + "\\" + localPluginsFolder;
      }    
    }
  }

  static protected void setMacOSXPath() {
    if (globalGStreamer && lookForGlobalGStreamer()) {
      gstreamerBinPath = globalGStreamerPath;
      gstreamerPluginsPath = globalGStreamerPath + "/" + globalPluginsFolder;
    } else {
      globalGStreamer = false;  
      if (localGStreamerPath.equals("")) {
        LibraryPath libPath = new LibraryPath();
        String path = libPath.get();        
        gstreamerBinPath = buildGStreamerBinPath(path, "/macosx" + bitsJVM);
        gstreamerPluginsPath = gstreamerBinPath + "/" + localPluginsFolder;
      } else {
        gstreamerBinPath = localGStreamerPath;
        gstreamerPluginsPath = localGStreamerPath + "/" + localPluginsFolder;
      }
    }
  }

  static protected boolean lookForGlobalGStreamer() {    
    LibraryPath libPath = new LibraryPath();
    String locPath = libPath.get();
    locPath = locPath.replace("/", System.getProperty("file.separator"));
    
    String[] searchPaths = null;
    if (!globalGStreamerPath.equals("")) {
      searchPaths = new String[] {globalGStreamerPath};
    }
    
    if (searchPaths == null) {
      String lpaths = System.getProperty("java.library.path");
      String pathsep = System.getProperty("path.separator");    
      searchPaths = lpaths.split(pathsep);
    }
    
    for (int i = 0; i < searchPaths.length; i++) {      
      String path = searchPaths[i];
      if ((locPath.equals("") || path.indexOf(locPath) == -1) && libgstreamerPresent(path, "libgstreamer")) {
        globalGStreamerPath = path;
        return true;
      }      
    }
    return false;
  }
  
  static protected boolean libgstreamerPresent(String dir, String file) {
    File libPath = new File(dir);
    String[] files = libPath.list();
    if (files != null) {
      for (int i = 0; i < files.length; i++) {
        if (-1 < files[i].indexOf(file)) {
          return true;
        }
      }
    }
    return false;
  }
  
  static protected String buildGStreamerBinPath(String base, String os) {        
    File path = new File(base + os);
    if (path.exists()) {
      return base + os;	
    } else {
    	
      return base;	
    }
  }
  
  static protected float nanoSecToSecFrac(float nanosec) {
    for (int i = 0; i < 3; i++)
      nanosec /= 1E3;
    return nanosec;
  }

  static protected long secToNanoLong(float sec) {
    Float f = new Float(sec * 1E9);
    return f.longValue();
  }
}
