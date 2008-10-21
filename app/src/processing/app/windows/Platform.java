/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2008 Ben Fry and Casey Reas

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

package processing.app.windows;

import java.io.File;
import java.io.UnsupportedEncodingException;

import processing.app.Base;
import processing.app.Preferences;
import processing.app.windows.Registry.REGISTRY_ROOT_KEY;
import processing.core.PApplet;


// http://developer.apple.com/documentation/QuickTime/Conceptual/QT7Win_Update_Guide/Chapter03/chapter_3_section_1.html
// HKEY_LOCAL_MACHINE\SOFTWARE\Apple Computer, Inc.\QuickTime\QTSysDir

// HKEY_LOCAL_MACHINE\SOFTWARE\JavaSoft\Java Development Kit\CurrentVersion -> 1.6 (String)
// HKEY_LOCAL_MACHINE\SOFTWARE\JavaSoft\Java Development Kit\CurrentVersion\1.6\JavaHome -> c:\jdk-1.6.0_05

public class Platform extends processing.app.Platform {

  static final String openCommand =
    System.getProperty("user.dir").replace('/', '\\') +
    "\\processing.exe \"%1\"";
  static final String DOC = "Processing.Document";

  public void init(Base base) {
    super.init(base);

    checkAssociations();
    checkQuickTime();
    checkPath();
  }


  /**
   * Make sure that .pde files are associated with processing.exe.
   */
  protected void checkAssociations() {
    try {
      String knownCommand =
        Registry.getStringValue(REGISTRY_ROOT_KEY.CLASSES_ROOT,
                                DOC + "\\shell\\open\\command", "");
      if (knownCommand == null) {
        if (Preferences.getBoolean("platform.auto_file_type_associations")) {
          setAssociations();
        }

      } else if (!knownCommand.equals(openCommand)) {
        // If the value is set differently, just change the registry setting.
        if (Preferences.getBoolean("platform.auto_file_type_associations")) {
          setAssociations();
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }


  /**
   * Associate .pde files with this version of Processing.
   */
  protected void setAssociations() throws UnsupportedEncodingException {
    if (Registry.createKey(REGISTRY_ROOT_KEY.CLASSES_ROOT,
                           "", ".pde") &&
        Registry.setStringValue(REGISTRY_ROOT_KEY.CLASSES_ROOT,
                                ".pde", "", DOC) &&

        Registry.createKey(REGISTRY_ROOT_KEY.CLASSES_ROOT, "", DOC) &&
        Registry.setStringValue(REGISTRY_ROOT_KEY.CLASSES_ROOT, DOC, "",
                                "Processing Source Code") &&

        Registry.createKey(REGISTRY_ROOT_KEY.CLASSES_ROOT,
                           DOC, "shell") &&
        Registry.createKey(REGISTRY_ROOT_KEY.CLASSES_ROOT,
                           DOC + "\\shell", "open") &&
        Registry.createKey(REGISTRY_ROOT_KEY.CLASSES_ROOT,
                           DOC + "\\shell\\open", "command") &&
        Registry.setStringValue(REGISTRY_ROOT_KEY.CLASSES_ROOT,
                                DOC + "\\shell\\open\\command", "",
                                openCommand)) {
      // everything ok
      // hooray!

    } else {
      Preferences.setBoolean("platform.auto_file_type_associations", false);
    }
  }


  /**
   * Find QuickTime for Java installation.
   */
  protected void checkQuickTime() {
    try {
      String qtsystemPath =
        Registry.getStringValue(REGISTRY_ROOT_KEY.LOCAL_MACHINE,
                                "Software\\Apple Computer, Inc.\\QuickTime",
                                "QTSysDir");
      // Could show a warning message here if QT not installed, but that
      // would annoy people who don't want anything to do with QuickTime.
      if (qtsystemPath != null) {
        File qtjavaZip = new File(qtsystemPath, "QTJava.zip");
        if (qtjavaZip.exists()) {
          String qtjavaZipPath = qtjavaZip.getAbsolutePath();
          String cp = System.getProperty("java.class.path");
          System.setProperty("java.class.path",
                             cp + File.pathSeparator + qtjavaZipPath);
        }
      }
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }
  }
  
  
  /**
   * Remove extra quotes, slashes, and garbage from the Windows PATH.
   */
  protected void checkPath() {
    String path = System.getProperty("java.library.path");
    String[] pieces = PApplet.split(path, File.pathSeparatorChar);
    String[] legit = new String[pieces.length];
    int legitCount = 0;
    for (String item : pieces) {
      if (item.startsWith("\"")) {
        item = item.substring(1);
      }
      if (item.endsWith("\"")) {
        item = item.substring(0, item.length() - 1);
      }
      if (item.endsWith(File.separator)) {
        item = item.substring(0, item.length() - File.separator.length());
      }
      File directory = new File(item);
      if (!directory.exists()) {
        continue;
      }
      if (item.trim().length() == 0) {
        continue;
      }
      legit[legitCount++] = item;
    }
    legit = PApplet.subset(legit, 0, legitCount);
    String newPath = PApplet.join(legit, File.pathSeparator);
    if (!newPath.equals(path)) {
      System.setProperty("java.library.path", newPath);
    }
  }


  // looking for Documents and Settings/blah/Application Data/Processing
  public File getSettingsFolder() throws Exception {
    // HKEY_CURRENT_USER\Software\Microsoft
    //   \Windows\CurrentVersion\Explorer\Shell Folders
    // Value Name: AppData
    // Value Type: REG_SZ
    // Value Data: path

    String keyPath =
      "Software\\Microsoft\\Windows\\CurrentVersion" +
      "\\Explorer\\Shell Folders";
    String appDataPath =
      Registry.getStringValue(REGISTRY_ROOT_KEY.CURRENT_USER, keyPath, "AppData");

    File dataFolder = new File(appDataPath, "Processing");
    return dataFolder;
  }


  // looking for Documents and Settings/blah/My Documents/Processing
  // (though using a reg key since it's different on other platforms)
  public File getDefaultSketchbookFolder() throws Exception {

    // http://support.microsoft.com/?kbid=221837&sd=RMVP
    // http://support.microsoft.com/kb/242557/en-us

    // The path to the My Documents folder is stored in the following
    // registry key, where path is the complete path to your storage location

    // HKEY_CURRENT_USER\Software\Microsoft\Windows\CurrentVersion\Explorer\Shell Folders
    // Value Name: Personal
    // Value Type: REG_SZ
    // Value Data: path

    // in some instances, this may be overridden by a policy, in which case check:
    // HKEY_CURRENT_USER\Software\Microsoft\Windows\CurrentVersion\Explorer\User Shell Folders

    String keyPath =
      "Software\\Microsoft\\Windows\\CurrentVersion" +
      "\\Explorer\\Shell Folders";
    String personalPath =
      Registry.getStringValue(REGISTRY_ROOT_KEY.CURRENT_USER, keyPath, "Personal");

    return new File(personalPath, "Processing");
  }


  public void openURL(String url) throws Exception {
    // this is not guaranteed to work, because who knows if the
    // path will always be c:\progra~1 et al. also if the user has
    // a different browser set as their default (which would
    // include me) it'd be annoying to be dropped into ie.
    //Runtime.getRuntime().exec("c:\\progra~1\\intern~1\\iexplore "
    // + currentDir

    // the following uses a shell execute to launch the .html file
    // note that under cygwin, the .html files have to be chmodded +x
    // after they're unpacked from the zip file. i don't know why,
    // and don't understand what this does in terms of windows
    // permissions. without the chmod, the command prompt says
    // "Access is denied" in both cygwin and the "dos" prompt.
    //Runtime.getRuntime().exec("cmd /c " + currentDir + "\\reference\\" +
    //                    referenceFile + ".html");
    if (url.startsWith("http://")) {
      // open dos prompt, give it 'start' command, which will
      // open the url properly. start by itself won't work since
      // it appears to need cmd
      Runtime.getRuntime().exec("cmd /c start " + url);
    } else {
      // just launching the .html file via the shell works
      // but make sure to chmod +x the .html files first
      // also place quotes around it in case there's a space
      // in the user.dir part of the url
      Runtime.getRuntime().exec("cmd /c \"" + url + "\"");
    }
  }


  public boolean openFolderAvailable() {
    return true;
  }


  public void openFolder(File file) throws Exception {
    String folder = file.getAbsolutePath();

    // doesn't work
    //Runtime.getRuntime().exec("cmd /c \"" + folder + "\"");

    // works fine on winxp, prolly win2k as well
    Runtime.getRuntime().exec("explorer \"" + folder + "\"");

    // not tested
    //Runtime.getRuntime().exec("start explorer \"" + folder + "\"");
  }
}
