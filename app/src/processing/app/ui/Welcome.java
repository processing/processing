/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2015 The Processing Foundation

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

package processing.app.ui;

import java.awt.EventQueue;
import java.io.File;
import java.io.IOException;

import processing.app.Base;
import processing.app.Language;
import processing.app.Preferences;
import processing.core.PApplet;
import processing.data.StringDict;


public class Welcome extends WebFrame {
  Base base;


  public Welcome(Base base, boolean sketchbook) throws IOException {
    super(getIndexFile(sketchbook), 400);
    this.base = base;
    //addStyle("#new_sketchbook { background-color: rgb(0, 255, 0); }");
    setVisible(true);
  }


  public void handleSubmit(StringDict dict) {
    // sketchbook = "create_new" or "use_existing"
    // show_each_time = "on" or <not param>
    //dict.print();

    String sketchbookAction = dict.get("sketchbook", null);
    if ("create_new".equals(sketchbookAction)) {
      // open file dialog
      // on affirmative selection, update sketchbook folder
//      String path = Preferences.getSketchbookPath() + "3";
//      File folder = new File(path);
//      folder.mkdirs();
      File folder = new File(Preferences.getSketchbookPath()).getParentFile();
      PApplet.selectFolder(Language.text("preferences.sketchbook_location.popup"),
                           "sketchbookCallback", folder,
                           this, this);
    }

    // If un-checked, the key won't be in the dict, so null will be passed
    boolean keepShowing = "on".equals(dict.get("show_each_time", null));
    Preferences.setBoolean("welcome.show", keepShowing);
    Preferences.save();

    handleClose();
  }


  /** Callback for the folder selector. */
  public void sketchbookCallback(File folder) {
    if (folder != null) {
      if (base != null) {
        base.setSketchbookFolder(folder);
      } else {
        System.out.println("user selected " + folder);
      }
    }
  }


  public void handleClose() {
    dispose();
  }


  static private File getIndexFile(boolean sketchbook) {
    String filename =
      "welcome/" + (sketchbook ? "sketchbook.html" : "generic.html");

    // version when running from command line for editing
    File htmlFile = new File("../build/shared/lib/" + filename);
    if (htmlFile.exists()) {
      return htmlFile;
    }
    // processing/build/macosx/work/Processing.app/Contents/Java
    // version for Scott to use for OS X debugging
    htmlFile = Base.getContentFile("../../../../../shared/lib/" + filename);
    if (htmlFile.exists()) {
      return htmlFile;
    }

    try {
      return Base.getLibFile(filename);
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }


  static public void main(String[] args) {
    Base.initPlatform();

    EventQueue.invokeLater(new Runnable() {
      public void run() {
        try {
          new Welcome(null, true) {
            public void handleClose() {
              System.exit(0);
            }
          };
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    });
  }
}
