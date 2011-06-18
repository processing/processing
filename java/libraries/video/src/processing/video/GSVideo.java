/**
 * Part of the GSVideo library: http://gsvideo.sourceforge.net/
 * Copyright (c) 2008-11 Andres Colubri 
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation, version 2.1.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General
 * Public License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA  02111-1307  USA
 */

package codeanticode.gsvideo;

import org.gstreamer.*;

// TODO: update to latest gstreamer on windows, jmcvideo on mac, seeking in gspipeline, check sf tracker

import processing.core.PApplet;
import processing.core.PConstants;

import java.io.File;
import java.util.List;

/**
 * This class contains some basic functions used by the rest of the classes in
 * this library.
 */
public class GSVideo implements PConstants {
  protected static String VERSION_STRING = "0.9";
  protected static long INSTANCES_COUNT = 0;

  protected static String gstreamerBinPath = "";
  protected static String gstreamerPluginsPath = "";

  protected static boolean defaultGLibContext = false;
  
  // Priority is given to global install of GStreamer if this is set to true. 
  public static boolean globalGStreamer = true;
 
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
      //globalGStreamerPath = "C://Program Files (x86)//OSSBuild//GStreamer//v0.10.7//lib";
    } else if (PApplet.platform == LINUX) {
      globalGStreamerPath = "/usr/lib";
    } else {}
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
  
  public static void init() {
    if (INSTANCES_COUNT == 0) {
      PApplet.println("GSVideo version: " + VERSION_STRING);
      initImpl();
    }
    INSTANCES_COUNT++;
  }
  
  public static void restart() {
    removePlugins();
    Gst.deinit();
    initImpl();
  }
    
  protected static void initImpl() {
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
      GSLibraryLoader loader = GSLibraryLoader.getInstance();
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
    Gst.init("GSVideo", args);

    addPlugins();
  }

  protected static void addPlugins() {
    if (!gstreamerPluginsPath.equals("")) {
      Registry reg = Registry.getDefault();
      boolean res;
      res = reg.scanPath(gstreamerPluginsPath);
      if (!res) {
        System.err.println("Cannot load GStreamer plugins from " + gstreamerPluginsPath);
      }
    }       
  }
  
  protected static void removePlugins() {
    Registry reg = Registry.getDefault();
    List<Plugin> list = reg.getPluginList();
    for (int i = 0; i < list.size(); i++) {
      Plugin plg = (Plugin)list.get(i);

      reg.removePlugin(plg);
    }    
  }
  
  protected static void setLinuxPath() {
    if (globalGStreamer && lookForGlobalGStreamer()) {
      gstreamerBinPath = "";
      gstreamerPluginsPath = "";
    } else {
      globalGStreamer = false;
      if (localGStreamerPath.equals("")) {
        GSLibraryPath libPath = new GSLibraryPath();
        String path = libPath.get();
        gstreamerBinPath = path + "/gstreamer/linux";
        gstreamerPluginsPath = path + "/gstreamer/linux/" + localPluginsFolder;
      } else {
        gstreamerBinPath = localGStreamerPath;
        gstreamerPluginsPath = localGStreamerPath + "/" + localPluginsFolder;
      }       
    }
  }

  protected static void setWindowsPath() {
    if (globalGStreamer && lookForGlobalGStreamer()) {
      gstreamerBinPath = "";
      gstreamerPluginsPath = "";
    } else {
      globalGStreamer = false;
      if (localGStreamerPath.equals("")) {
        GSLibraryPath libPath = new GSLibraryPath();
        String path = libPath.get();
        gstreamerBinPath = path + "\\gstreamer\\win";
        gstreamerPluginsPath = path + "\\gstreamer\\win\\" + localPluginsFolder;
      } else {
        gstreamerBinPath = localGStreamerPath;
        gstreamerPluginsPath = localGStreamerPath + "\\" + localPluginsFolder;
      }    
    }
  }

  protected static void setMacOSXPath() {
    if (globalGStreamer && lookForGlobalGStreamer()) {
      gstreamerBinPath = globalGStreamerPath;
      gstreamerPluginsPath = globalGStreamerPath + "/" + globalPluginsFolder;
    } else {
      globalGStreamer = false;  
      if (localGStreamerPath.equals("")) {
        GSLibraryPath libPath = new GSLibraryPath();
        String path = libPath.get();
        gstreamerBinPath = path + "/gstreamer/macosx";
        gstreamerPluginsPath = path + "/gstreamer/macosx/" + localPluginsFolder;
      } else {
        gstreamerBinPath = localGStreamerPath;
        gstreamerPluginsPath = localGStreamerPath + "/" + localPluginsFolder;
      }
    }
  }

  protected static boolean lookForGlobalGStreamer() {    
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
      if (libgstreamerPresent(path, "libgstreamer")) {
        globalGStreamerPath = path;
        return true;
      }      
    }
    return false;
  }
  
  protected static boolean libgstreamerPresent(String dir, String file) {
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
