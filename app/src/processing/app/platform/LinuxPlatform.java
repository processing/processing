/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2012-2013 The Processing Foundation
  Copyright (c) 2008-2012 Ben Fry and Casey Reas

  This program is free software; you can redistribute it and/or
  modify it under the terms of the GNU General Public License
  version 2, as published by the Free Software Foundation.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software Foundation,
  Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package processing.app.platform;

import java.io.File;
import java.awt.Toolkit;

import processing.app.Base;
import processing.app.Messages;
import processing.app.Preferences;
import processing.app.platform.DefaultPlatform;


public class LinuxPlatform extends DefaultPlatform {

  public void initBase(Base base) {
    super.initBase(base);

    String javaVendor = System.getProperty("java.vendor");
    String javaVM = System.getProperty("java.vm.name");
    if (javaVendor == null ||
        (!javaVendor.contains("Sun") && !javaVendor.contains("Oracle")) ||
        javaVM == null || !javaVM.contains("Java")) {
      Messages.showWarning("Not fond of this Java VM",
                           "Processing requires Java 8 from Oracle.\n" +
                           "Other versions such as OpenJDK, IcedTea,\n" +
                           "and GCJ are strongly discouraged. Among other things, you're\n" +
                           "likely to run into problems with sketch window size and\n" +
                           "placement. For more background, please read the wiki:\n" +
                           "https://github.com/processing/processing/wiki/Supported-Platforms#linux", null);
    }

    // Set x11 WM_CLASS property which is used as the application
    // name by Gnome3 and other window managers.
    // https://github.com/processing/processing/issues/2534
    try {
      Toolkit xToolkit = Toolkit.getDefaultToolkit();
      java.lang.reflect.Field awtAppClassNameField =
        xToolkit.getClass().getDeclaredField("awtAppClassName");
      awtAppClassNameField.setAccessible(true);
      awtAppClassNameField.set(xToolkit, "Processing");

    } catch(Exception e) {
      // In case the implementation details change
      e.printStackTrace();
    }
  }


  // Unlike many other UNIX programs, the JVM does not evaluate
  // the HOME environment variable to construct user.home and
  // friends, but solely relies on the UID of the current user
  // for this purpose.
  // For this reason user.home will always be "/root", even when
  // a different user attempts to launch Processing with "sudo".
  // (see also https://askubuntu.com/a/659882)
  // Attempt to work around this in the least invasive manner,
  // so that "sudo -E processing" or "sudo -E processing-java"
  // will pick up the invoking user's sketchbook folder instead.
  public File getDefaultSketchbookFolder() throws Exception {
    String sysEnvHome = System.getenv("HOME");
    if (sysEnvHome != null && 0 < sysEnvHome.length()) {
      return new File(sysEnvHome, "sketchbook");
    } else {
      return super.getDefaultSketchbookFolder();
    }
  }


  public void openURL(String url) throws Exception {
    if (openFolderAvailable()) {
      String launcher = Preferences.get("launcher");
      if (launcher != null) {
        Runtime.getRuntime().exec(new String[] { launcher, url });
      }
    }
  }


  public boolean openFolderAvailable() {
    if (Preferences.get("launcher") != null) {
      return true;
    }

    // Attempt to use xdg-open
    try {
      Process p = Runtime.getRuntime().exec(new String[] { "xdg-open" });
      p.waitFor();
      Preferences.set("launcher", "xdg-open");
      return true;
    } catch (Exception e) { }

    // Attempt to use gnome-open
    try {
      Process p = Runtime.getRuntime().exec(new String[] { "gnome-open" });
      p.waitFor();
      // Not installed will throw an IOException (JDK 1.4.2, Ubuntu 7.04)
      Preferences.set("launcher", "gnome-open");
      return true;
    } catch (Exception e) { }

    // Attempt with kde-open
    try {
      Process p = Runtime.getRuntime().exec(new String[] { "kde-open" });
      p.waitFor();
      Preferences.set("launcher", "kde-open");
      return true;
    } catch (Exception e) { }

    return false;
  }


  public void openFolder(File file) throws Exception {
    if (openFolderAvailable()) {
      String lunch = Preferences.get("launcher");
      try {
        String[] params = new String[] { lunch, file.getAbsolutePath() };
        //processing.core.PApplet.println(params);
        /*Process p =*/ Runtime.getRuntime().exec(params);
        /*int result =*/ //p.waitFor();
      } catch (Exception e) {
        e.printStackTrace();
      }
    } else {
      System.out.println("No launcher set, cannot open " +
                         file.getAbsolutePath());
    }
  }
}
