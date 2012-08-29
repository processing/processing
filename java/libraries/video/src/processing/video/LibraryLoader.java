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

import java.util.HashMap;
import java.util.Map;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Platform;
 
/**
 * This class loads the gstreamer native libraries.
 * By Andres Colubri
 * Based on code by Tal Shalif
 * 
 */
public class LibraryLoader {

  public interface DummyLibrary extends Library {
  }

  private static LibraryLoader instance;
   
  static final Object[][] WINDOWS_DEPENDENCIES = {           
      // glib libraries
      { "gio-2.0", new String[] {}, true },
      { "glib-2.0", new String[] {}, true },
      { "gmodule-2.0", new String[] {}, true },
      { "gobject-2.0", new String[] {}, true },
      { "gthread-2.0", new String[] {}, true },

      // Core gstreamer libraries  
      { "gstapp-0.10", new String[] {}, true },
      { "gstaudio-0.10", new String[] {}, true },
      { "gstbase-0.10", new String[] {}, true },
      { "gstbasevideo-0.10", new String[] {}, true },
      { "gstcdda-0.10", new String[] {}, true },
      { "gstcontroller-0.10", new String[] {}, true },
      { "gstdataprotocol-0.10", new String[] {}, true },
      { "gstfft-0.10", new String[] {}, true },
      { "gstinterfaces-0.10", new String[] {}, true },
      { "gstnet-0.10", new String[] {}, true },
      { "gstnetbuffer-0.10", new String[] {}, true },
      { "gstpbutils-0.10", new String[] {}, true },
      { "gstphotography-0.10", new String[] {}, true },
      { "gstreamer-0.10", new String[] {}, true },
      { "gstriff-0.10", new String[] {}, true },
      { "gstrtp-0.10", new String[] {}, true },
      { "gstrtsp-0.10", new String[] {}, true },
      { "gstsdp-0.10", new String[] {}, true },
      { "gstsignalprocessor-0.10", new String[] {}, true },
      { "gsttag-0.10", new String[] {}, true },
      { "gstvideo-0.10", new String[] {}, true },
      
      // External libraries
      { "libiconv-2", new String[] {}, false },
      { "libintl-8", new String[] {}, false },
      { "libjpeg-8", new String[] {}, false },
      { "libogg-0", new String[] {}, false },
      { "liborc-0.4-0", new String[] {}, false },
      { "liborc-test-0.4-0", new String[] {}, false },
      { "libpng14-14", new String[] {}, false },
      { "libtheora-0", new String[] {}, false },
      { "libtheoradec-1", new String[] {}, false },
      { "libtheoraenc-1", new String[] {}, false },
      { "libvorbis-0", new String[] {}, false },
      { "libvorbisenc-2", new String[] {}, false },
      { "libvorbisfile-3", new String[] {}, false },
      { "libxml2-2", new String[] {}, false },
      { "zlib1", new String[] {}, false } };
  
  static final Object[][] MACOSX_DEPENDENCIES = {
      { "gstbase-0.10", new String[] { "gstreamer-0.10" }, true },
      { "gstinterfaces-0.10", new String[] { "gstreamer-0.10" }, true },
      { "gstcontroller-0.10", new String[] { "gstreamer-0.10" }, true },
      { "gstaudio-0.10", new String[] { "gstbase-0.10" }, true },
      { "gstvideo-0.10", new String[] { "gstbase-0.10" }, true } };

  static final Object[][] DEFAULT_DEPENDENCIES = {
      { "gstreamer-0.10", new String[] {}, true },
      { "gstbase-0.10", new String[] { "gstreamer-0.10" }, true },
      { "gstinterfaces-0.10", new String[] { "gstreamer-0.10" }, true },
      { "gstcontroller-0.10", new String[] { "gstreamer-0.10" }, true },
      { "gstaudio-0.10", new String[] { "gstbase-0.10" }, true },
      { "gstvideo-0.10", new String[] { "gstbase-0.10" }, true }, };

  
  static final Object[][] dependencies = 
    Platform.isWindows() ? WINDOWS_DEPENDENCIES : 
      Platform.isMac() ? MACOSX_DEPENDENCIES : DEFAULT_DEPENDENCIES;

  
  private static final Map<String, Object> loadedMap = 
    new HashMap<String, Object>();

  
  private static final int RECURSIVE_LOAD_MAX_DEPTH = 5;
  
  
  private LibraryLoader() {
  }

  
  private void preLoadLibs() {
    for (Object[] a : dependencies) {
      load(a[0].toString(), DummyLibrary.class, true, 0, (Boolean) a[2]);
    }
  }

  
  private String[] findDeps(String name) {

    for (Object[] a : dependencies) {
      if (name.equals(a[0])) {

        return (String[]) a[1];
      }
    }

    return new String[] {}; // library dependancy load chain unspecified -
                            // probably client call
  }

  
  public Object load(String name, Class<?> clazz, boolean reqLib) {
    return load(name, clazz, true, 0, reqLib);
  }

  
  private Object load(String name, Class<?> clazz, boolean forceReload,
      int depth, boolean reqLib) {

    assert depth < RECURSIVE_LOAD_MAX_DEPTH : String.format(
        "recursive max load depth %s has been exceeded", depth);

    Object library = loadedMap.get(name);

    if (null == library || forceReload) {

      // Logger.getAnonymousLogger().info(String.format("%" + ((depth + 1) * 2)
      // + "sloading %s", "->", name));

      try {
        String[] deps = findDeps(name);

        for (String lib : deps) {
          load(lib, DummyLibrary.class, false, depth + 1, reqLib);
        }

        library = loadLibrary(name, clazz, reqLib);

        if (library != null) {
          loadedMap.put(name, library);
        }
      } catch (Exception e) {
        if (reqLib)
          throw new RuntimeException(String.format(
            "can not load required library %s", name, e));
        else
          System.out.println(String.format("can not load library %s", name, e));
      }
    }

    return library;
  }

  
  private static Object loadLibrary(String name, Class<?> clazz, 
    boolean reqLib) {

    // Logger.getAnonymousLogger().info(String.format("loading %s", name));

    String[] nameFormats;
    nameFormats = Platform.isWindows() ? new String[] { "lib%s", "lib%s-0",
        "%s" } : new String[] { "%s-0", "%s" };

    UnsatisfiedLinkError linkError = null;

    for (String fmt : nameFormats) {
      try {
        String s = String.format(fmt, name);
        //System.out.println("Trying to load library file " + s);
        Object obj = Native.loadLibrary(s, clazz);
        //System.out.println("Loaded library " + s + " succesfully!");
        return obj;
      } catch (UnsatisfiedLinkError ex) {
        linkError = ex;
      }
    }

    if (reqLib)
      throw new UnsatisfiedLinkError(
        String.format(
          "can't load library %s (%1$s|lib%1$s|lib%1$s-0) with " + 
          "-Djna.library.path=%s. Last error:%s",
          name, System.getProperty("jna.library.path"), linkError));
    else {
      System.out.println(String.format(
        "can't load library %s (%1$s|lib%1$s|lib%1$s-0) with " + 
        "-Djna.library.path=%s. Last error:%s",
        name, System.getProperty("jna.library.path"), linkError));
      return null;
    }
  }

  
  public static synchronized LibraryLoader getInstance() {
    if (null == instance) {
      instance = new LibraryLoader();
      instance.preLoadLibs();
    }
    return instance;
  }
}
