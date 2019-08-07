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
import java.awt.Desktop;
import java.awt.Toolkit;

import processing.app.Base;
import processing.app.Messages;
import processing.app.Preferences;
import processing.app.platform.DefaultPlatform;
import processing.core.PApplet;


public class LinuxPlatform extends DefaultPlatform {
  String homeDir;


  public void initBase(Base base) {
    super.initBase(base);

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


  // The default Look & Feel is set in preferences.txt
  // As of 3.0a6, defaults.txt is set to Nimbus for Linux.


  // Java sets user.home to be /root for execution with sudo.
  // This method attempts to use the user's real home directory instead.
  public String getHomeDir() {
    if (homeDir == null) {
      // get home directory of SUDO_USER if set, else use user.home
      homeDir = System.getProperty("user.home");
      String sudoUser = System.getenv("SUDO_USER");
      if (sudoUser != null && sudoUser.length() != 0) {
        try {
          homeDir = getHomeDir(sudoUser);
        } catch (Exception e) { }
      }
    }
    return homeDir;
  }


  static public String getHomeDir(String user) throws Exception {
    Process p = PApplet.exec("/bin/sh", "-c", "echo ~" + user);
    return PApplet.createReader(p.getInputStream()).readLine();
  }


  @Override
  public File getSettingsFolder() throws Exception {
    return new File(getHomeDir(), ".processing");
  }


  @Override
  public File getDefaultSketchbookFolder() throws Exception {
    return new File(getHomeDir(), "sketchbook");
  }


  @Override
  public void openURL(String url) throws Exception {
    if (Desktop.isDesktopSupported()) {
      super.openURL(url);

    } else if (openFolderAvailable()) {
      String launcher = Preferences.get("launcher");  // guaranteed non-null
      Runtime.getRuntime().exec(new String[] { launcher, url });

    } else {
      System.err.println("No launcher set, cannot open " + url);
    }
  }


  @Override
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


  @Override
  public void openFolder(File file) throws Exception {
    if (Desktop.isDesktopSupported()) {
      super.openFolder(file);

    } else if (openFolderAvailable()) {
      String launcher = Preferences.get("launcher");
      String[] params = new String[] { launcher, file.getAbsolutePath() };
      Runtime.getRuntime().exec(params);

    } else {
      System.err.println("No launcher set, cannot open " +
                         file.getAbsolutePath());
    }
  }
}
