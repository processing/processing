/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2011-12 Ben Fry and Casey Reas

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
import java.nio.ByteOrder;
import java.util.List;

/**
 * This class contains some basic functions used by the rest of the classes in
 * this library.
 */
public class Video implements PConstants {
  // Path that the video library will use to load the GStreamer base libraries 
  // and plugins from. They can be passed from the application using the 
  // gstreamer.library.path and gstreamer.plugin.path system variables (see
  // comments in initImpl() below).
  protected static String gstreamerLibPath = "";
  protected static String gstreamerPluginPath = "";
 
  // Direct buffer pass enabled by default. With this mode enabled, no new 
  // buffers are created and disposed by the GC in each frame (thanks to Octavi 
  // Estape for suggesting this improvement) which should help performance in 
  // most situations.
  protected static boolean passDirectBuffer = true; 

  // OpenGL texture used as buffer sink by default, when the renderer is 
  // GL-based. This can improve performance significantly, since the video 
  // frames are automatically copied into the texture without passing through 
  // the pixels arrays, as well as having the color conversion into RGBA handled 
  // natively by GStreamer.
  protected static boolean useGLBufferSink = true;    

  protected static boolean defaultGLibContext = false;
  
  protected static long INSTANCES_COUNT = 0;
  
  protected static int bitsJVM;
  static {
    bitsJVM = PApplet.parseInt(System.getProperty("sun.arch.data.model"));
  }  
  
  
  static protected void init() {
    if (INSTANCES_COUNT == 0) {
      initImpl();
    }
    INSTANCES_COUNT++;
  }
  
  
  static protected void restart() {
    removePlugins();
    Gst.deinit();
    initImpl();
  }
   
  
  static protected void initImpl() {
    // The location of the GStreamer base libraries can be passed from the 
    // application to the vide library via a system variable. In Eclipse, add to 
    // "VM Arguments" in  "Run Configurations" the following line:
    // -Dgstreamer.library.path=path    
    String libPath = System.getProperty("gstreamer.library.path");
    if (libPath != null) {
      gstreamerLibPath = libPath;
      
      // If the GStreamer installation referred by gstreamer.library.path is not
      // a system installation, then the path containing the plugins needs to be
      // specified separately, otherwise the plugins will be automatically 
      // loaded from the default location. The system property for the plugin
      // path is "gstreamer.plugin.path"
      String pluginPath = System.getProperty("gstreamer.plugin.path");
      if (pluginPath != null) {
        gstreamerPluginPath = pluginPath;
      }
    } else {
      // Paths are build automatically from the curren location of the video
      // library.
      if (PApplet.platform == LINUX) {    
        buildLinuxPaths();
      } else if (PApplet.platform == WINDOWS) {
        buildWindowsPaths();
      } else if (PApplet.platform == MACOSX) {
        buildMacOSXPaths();
      }      
    }    

    if (!gstreamerLibPath.equals("")) {
      System.setProperty("jna.library.path", gstreamerLibPath);
    }

    if (PApplet.platform == WINDOWS) {
      LibraryLoader loader = LibraryLoader.getInstance();
      if (loader == null) {
        System.err.println("Cannot load local version of GStreamer libraries.");
      }
    }
    
    String[] args = { "" };
    Gst.setUseDefaultContext(defaultGLibContext);
    Gst.init("Processing core video", args);

    addPlugins();
  }

  
  static protected void addPlugins() {
    if (!gstreamerPluginPath.equals("")) {
      Registry reg = Registry.getDefault();
      boolean res;
      res = reg.scanPath(gstreamerPluginPath);
      if (!res) {
        System.err.println("Cannot load GStreamer plugins from " + 
                           gstreamerPluginPath);
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
  
  
  static protected void buildLinuxPaths() {
    gstreamerLibPath = "";
    gstreamerPluginPath = "";
  }

  
  static protected void buildWindowsPaths() {
    LibraryPath libPath = new LibraryPath();
    String path = libPath.get();
    gstreamerLibPath = buildGStreamerLibPath(path, "\\windows" + bitsJVM);
    gstreamerPluginPath = gstreamerLibPath + "\\plugins";
  }

  
  static protected void buildMacOSXPaths() {
    LibraryPath libPath = new LibraryPath();
    String path = libPath.get();        
    gstreamerLibPath = buildGStreamerLibPath(path, "/macosx" + bitsJVM);
    gstreamerPluginPath = gstreamerLibPath + "/plugins";
  }

  
  static protected String buildGStreamerLibPath(String base, String os) {        
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
  
  
  /**
   * Reorders an OpenGL pixel array (RGBA) into ARGB. The array must be
   * of size width * height.
   * @param pixels int[]
   */
  static protected void convertToARGB(int[] pixels, int width, int height) {
    int t = 0;
    int p = 0;
    if (ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN) {
      // RGBA to ARGB conversion: shifting RGB 8 bits to the right,
      // and placing A 24 bits to the left.
      for (int y = 0; y < height; y++) {
        for (int x = 0; x < width; x++) {
          int pixel = pixels[p++];
          pixels[t++] = (pixel >>> 8) | ((pixel << 24) & 0xFF000000);
        }
      }
    } else {
      // We have to convert ABGR into ARGB, so R and B must be swapped,
      // A and G just brought back in.
      for (int y = 0; y < height; y++) {
        for (int x = 0; x < width; x++) {
          int pixel = pixels[p++];
          pixels[t++] = ((pixel & 0xFF) << 16) | ((pixel & 0xFF0000) >> 16) |
                        (pixel & 0xFF00FF00);
        }
      }
    }
  }  
}
