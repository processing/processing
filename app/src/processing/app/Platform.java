/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2012-15 The Processing Foundation
  Copyright (c) 2008-12 Ben Fry and Casey Reas

  This program is free software; you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation; either version 2 of the License, or
  (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software Foundation,
  Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package processing.app;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import com.sun.jna.platform.FileUtils;

import processing.app.platform.DefaultPlatform;
import processing.core.PApplet;
import processing.core.PConstants;


public class Platform {
  static DefaultPlatform inst;

  static Map<Integer, String> platformNames = new HashMap<>();
  static {
    platformNames.put(PConstants.WINDOWS, "windows"); //$NON-NLS-1$
    platformNames.put(PConstants.MACOSX, "macosx"); //$NON-NLS-1$
    platformNames.put(PConstants.LINUX, "linux"); //$NON-NLS-1$
  }

  static Map<String, Integer> platformIndices = new HashMap<>();
  static {
    platformIndices.put("windows", PConstants.WINDOWS); //$NON-NLS-1$
    platformIndices.put("macosx", PConstants.MACOSX); //$NON-NLS-1$
    platformIndices.put("linux", PConstants.LINUX); //$NON-NLS-1$
  }

  /** How many bits this machine is */
  static int nativeBits;
  static {
    nativeBits = 32;  // perhaps start with 32
    String bits = System.getProperty("sun.arch.data.model"); //$NON-NLS-1$
    if (bits != null) {
      if (bits.equals("64")) { //$NON-NLS-1$
        nativeBits = 64;
      }
    } else {
      // if some other strange vm, maybe try this instead
      if (System.getProperty("java.vm.name").contains("64")) { //$NON-NLS-1$ //$NON-NLS-2$
        nativeBits = 64;
      }
    }
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  static public void init() {
    try {
      Class<?> platformClass = Class.forName("processing.app.Platform"); //$NON-NLS-1$
      if (Platform.isMacOS()) {
        platformClass = Class.forName("processing.app.platform.MacPlatform"); //$NON-NLS-1$
      } else if (Platform.isWindows()) {
        platformClass = Class.forName("processing.app.platform.WindowsPlatform"); //$NON-NLS-1$
      } else if (Platform.isLinux()) {
        platformClass = Class.forName("processing.app.platform.LinuxPlatform"); //$NON-NLS-1$
      }
      inst = (DefaultPlatform) platformClass.getDeclaredConstructor().newInstance();
    } catch (Exception e) {
      Messages.showError("Problem Setting the Platform",
                         "An unknown error occurred while trying to load\n" +
                         "platform-specific code for your machine.", e);
    }
  }


  static public void initBase(Base base) throws Exception {
    inst.initBase(base);
  }


  static public void setLookAndFeel() throws Exception {
    inst.setLookAndFeel();
  }


  static public File getSettingsFolder() throws Exception {
    return inst.getSettingsFolder();
  }


  static public File getDefaultSketchbookFolder() throws Exception {
    return inst.getDefaultSketchbookFolder();
  }


  static public void saveLanguage(String languageCode) {
    inst.saveLanguage(languageCode);
  }


//  static public void openURL(String url) throws Exception {
//    inst.openURL(url);
//  }
//
//
//  public boolean openFolderAvailable() {
//    return inst.openFolderAvailable();
//  }
//
//
//  public void openFolder(File file) throws Exception {
//    inst.openFolder(file);
//  }


  /**
   * Implements the cross-platform headache of opening URLs.
   *
   * For 2.0a8 and later, this requires the parameter to be an actual URL,
   * meaning that you can't send it a file:// path without a prefix. It also
   * just calls into Platform, which now uses java.awt.Desktop (where
   * possible, meaning not on Linux) now that we're requiring Java 6.
   * As it happens the URL must also be properly URL-encoded.
   */
  static public void openURL(String url) {
    try {
      inst.openURL(url);

    } catch (Exception e) {
      Messages.showWarning("Problem Opening URL",
                           "Could not open the URL\n" + url, e);
    }
  }


  /**
   * Used to determine whether to disable the "Show Sketch Folder" option.
   * @return true If a means of opening a folder is known to be available.
   */
  static public boolean openFolderAvailable() {
    return inst.openFolderAvailable();
  }


  /**
   * Implements the other cross-platform headache of opening
   * a folder in the machine's native file browser.
   */
  static public void openFolder(File file) {
    try {
      inst.openFolder(file);

    } catch (Exception e) {
      Messages.showWarning("Problem Opening Folder",
                           "Could not open the folder\n" + file.getAbsolutePath(), e);
    }
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  /**
   * Return whether sketches will run as 32- or 64-bits based
   * on the JVM that's in use.
   */
  static public int getNativeBits() {
    return nativeBits;
  }


  /**
   * Return the value of the os.arch property
   */
  static public String getNativeArch() {
    // This will return "arm" for 32-bit ARM, "aarch64" for 64-bit ARM (both on Linux)
    return System.getProperty("os.arch");
  }


  /*
   * Return a string that identifies the variant of a platform
   * e.g. "32" or "64" on Intel
   */
  static public String getVariant() {
    return getVariant(PApplet.platform, getNativeArch(), getNativeBits());
  }


  static public String getVariant(int platform, String arch, int bits) {
    if (platform == PConstants.LINUX &&
        bits == 32 && "arm".equals(Platform.getNativeArch())) {
      return "armv6hf";  // assume armv6hf
    } else if (platform == PConstants.LINUX &&
        bits == 64 && "aarch64".equals(Platform.getNativeArch())) {
      return "arm64";
    }

    return Integer.toString(bits);  // 32 or 64
  }


  static public String getName() {
    return PConstants.platformNames[PApplet.platform];
  }


  /**
   * Map a platform constant to its name.
   * @param which PConstants.WINDOWS, PConstants.MACOSX, PConstants.LINUX
   * @return one of "windows", "macosx", or "linux"
   */
  static public String getName(int which) {
    return platformNames.get(which);
  }


  static public int getIndex(String what) {
    Integer entry = platformIndices.get(what);
    return (entry == null) ? -1 : entry.intValue();
  }


  // These were changed to no longer rely on PApplet and PConstants because
  // of conflicts that could happen with older versions of core.jar, where
  // the MACOSX constant would instead read as the LINUX constant.


  /**
   * returns true if Processing is running on a Mac OS X machine.
   */
  static public boolean isMacOS() {
    //return PApplet.platform == PConstants.MACOSX;
    return System.getProperty("os.name").indexOf("Mac") != -1; //$NON-NLS-1$ //$NON-NLS-2$
  }


  /**
   * returns true if running on windows.
   */
  static public boolean isWindows() {
    //return PApplet.platform == PConstants.WINDOWS;
    return System.getProperty("os.name").indexOf("Windows") != -1; //$NON-NLS-1$ //$NON-NLS-2$
  }


  /**
   * true if running on linux.
   */
  static public boolean isLinux() {
    //return PApplet.platform == PConstants.LINUX;
    return System.getProperty("os.name").indexOf("Linux") != -1; //$NON-NLS-1$ //$NON-NLS-2$
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  static protected File processingRoot;

  /**
   * Get reference to a file adjacent to the executable on Windows and Linux,
   * or inside Contents/Resources/Java on Mac OS X. This will return the local
   * JRE location, *whether or not it is the active JRE*.
   */
  static public File getContentFile(String name) {
    if (processingRoot == null) {
      // Get the path to the .jar file that contains Base.class
      URL pathURL =
          Base.class.getProtectionDomain().getCodeSource().getLocation();
      // Decode URL
      String decodedPath;
      try {
        decodedPath = pathURL.toURI().getSchemeSpecificPart();
      } catch (URISyntaxException e) {
        e.printStackTrace();
        return null;
      }

      if (decodedPath.contains("/app/bin")) {  // This means we're in Eclipse
        final File build = new File(decodedPath, "../../build").getAbsoluteFile();
        if (Platform.isMacOS()) {
          processingRoot = new File(build, "macosx/work/Processing.app/Contents/Java");
        } else if (Platform.isWindows()) {
          processingRoot =  new File(build, "windows/work");
        } else if (Platform.isLinux()) {
          processingRoot =  new File(build, "linux/work");
        }
      } else {
        // The .jar file will be in the lib folder
        File jarFolder = new File(decodedPath).getParentFile();
        if (jarFolder.getName().equals("lib")) {
          // The main Processing installation directory.
          // This works for Windows, Linux, and Apple's Java 6 on OS X.
          processingRoot = jarFolder.getParentFile();
        } else if (Platform.isMacOS()) {
          // This works for Java 8 on OS X. We don't have things inside a 'lib'
          // folder on OS X. Adding it caused more problems than it was worth.
          processingRoot = jarFolder;
        }
        if (processingRoot == null || !processingRoot.exists()) {
          // Try working directory instead (user.dir, different from user.home)
          System.err.println("Could not find lib folder via " +
            jarFolder.getAbsolutePath() +
            ", switching to user.dir");
          processingRoot = new File(""); // resolves to "user.dir"
        }
      }
    }
    return new File(processingRoot, name);
  }


  static public File getJavaHome() {
    if (Platform.isMacOS()) {
      //return "Contents/PlugIns/jdk1.7.0_40.jdk/Contents/Home/jre/bin/java";
      File[] plugins = getContentFile("../PlugIns").listFiles(new FilenameFilter() {
        public boolean accept(File dir, String name) {
          return dir.isDirectory() &&
            name.contains("jdk") && !name.startsWith(".");
        }
      });
      return new File(plugins[0], "Contents/Home");
    }
    // On all other platforms, it's the 'java' folder adjacent to Processing
    return getContentFile("java");
  }


  /** Get the path to the embedded Java executable. */
  static public String getJavaPath() {
    String javaPath = "bin/java" + (Platform.isWindows() ? ".exe" : "");
    File javaFile = new File(getJavaHome(), javaPath);
    try {
      return javaFile.getCanonicalPath();
    } catch (IOException e) {
      return javaFile.getAbsolutePath();
    }
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  /**
   * Attempts to move to the Trash on OS X, or the Recycle Bin on Windows.
   * Also tries to find a suitable Trash location on Linux.
   * If not possible, just deletes the file or folder instead.
   * @param file the folder or file to be removed/deleted
   * @return true if the folder was successfully removed
   * @throws IOException
   */
  static public boolean deleteFile(File file) throws IOException {
    FileUtils fu = FileUtils.getInstance();
    if (fu.hasTrash()) {
      fu.moveToTrash(new File[] { file });
      return true;

    } else if (file.isDirectory()) {
      Util.removeDir(file);
      return true;

    } else {
      return file.delete();
    }
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  static public void setenv(String variable, String value) {
    inst.setenv(variable, value);
  }


  static public String getenv(String variable) {
    return inst.getenv(variable);
  }


  static public int unsetenv(String variable) {
    return inst.unsetenv(variable);
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  static public int getSystemDPI() {
    return inst.getSystemDPI();
  }
}
