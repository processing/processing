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

package processing.app.platform;

import java.awt.Desktop;
import java.io.File;
import java.net.URI;

import javax.swing.UIManager;

import processing.app.Base;
import processing.app.Preferences;


/**
 * Used by Base for platform-specific tweaking, for instance finding the
 * sketchbook location using the Windows registry, or OS X event handling.
 *
 * The methods in this implementation are used by default, and can be
 * overridden by a subclass.
 *
 * These methods throw vanilla-flavored Exceptions, so that error handling
 * occurs inside Platform (which will show warnings in some cases).
 *
 * There is currently no mechanism for adding new platforms, as the setup is
 * not automated. We could use getProperty("os.arch") perhaps, but that's
 * debatable (could be upper/lowercase, have spaces, etc.. basically we don't
 * know if name is proper Java package syntax.)
 */
public class DefaultPlatform {
  Base base;


  public void initBase(Base base) {
    this.base = base;
  }


  /**
   * Set the default L & F. While I enjoy the bounty of the sixteen possible
   * exception types that this UIManager method might throw, I feel that in
   * just this one particular case, I'm being spoiled by those engineers
   * at Sun, those Masters of the Abstractionverse. So instead, I'll pretend
   * that I'm not offered eleven dozen ways to report to the user exactly what
   * went wrong, and I'll bundle them all into a single catch-all "Exception".
   * Because in the end, all I really care about is whether things worked or
   * not. And even then, I don't care.
   * @throws Exception Just like I said.
   */
  public void setLookAndFeel() throws Exception {
    String laf = Preferences.get("editor.laf");
    if (laf == null || laf.length() == 0) {  // normal situation
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    } else {
      UIManager.setLookAndFeel(laf);
    }
  }


  /**
   * Handle any platform-specific languages saving. This is necessary on OS X
   * because of how bundles are handled, but perhaps your platform would like
   * to Think Different too?
   * @param languageCode 2-digit lowercase ISO language code
   */
  public void saveLanguage(String languageCode) { }


  /**
   * This function should throw an exception or return a value.
   * Do not return null.
   */
  public File getSettingsFolder() throws Exception {
    // otherwise make a .processing directory int the user's home dir
    File home = new File(System.getProperty("user.home"));
    return new File(home, ".processing");
  }


  /**
   * @return if not overridden, a folder named "sketchbook" in user.home.
   * @throws Exception so that subclasses can throw a fit
   */
  public File getDefaultSketchbookFolder() throws Exception {
    return new File(System.getProperty("user.home"), "sketchbook");
  }


  public void openURL(String url) throws Exception {
    Desktop.getDesktop().browse(new URI(url));
  }


  public boolean openFolderAvailable() {
    return Desktop.isDesktopSupported();
  }


  public void openFolder(File file) throws Exception {
    Desktop.getDesktop().open(file);
  }
}
