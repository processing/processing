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

package processing.app.linux;

import javax.swing.UIManager;


/**
 * Used by Base for platform-specific tweaking, for instance finding the
 * sketchbook location using the Windows registry, or OS X event handling.
 */
public class Platform {

  // TODO Need to be smarter here since KDE people ain't gonna like that GTK.
  //      It may even throw a weird exception at 'em for their trouble.
  public void setLookAndFeel() throws Exception {
    // Linux is by default even uglier than metal (Motif?).
    // Actually, i'm using native menus, so they're even uglier
    // and Motif-looking (Lesstif?). Ick. Need to fix this.
    //String lfname = UIManager.getCrossPlatformLookAndFeelClassName();
    //UIManager.setLookAndFeel(lfname);

    // For 0120, trying out the gtk+ look and feel as the default.
    // This is available in Java 1.4.2 and later, and it can't possibly
    // be any worse than Metal. (Ocean might also work, but that's for
    // Java 1.5, and we aren't going there yet)
    UIManager.setLookAndFeel("com.sun.java.swing.plaf.gtk.GTKLookAndFeel");
  }
}