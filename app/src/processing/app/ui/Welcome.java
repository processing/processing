/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2015-19 The Processing Foundation

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

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.IOException;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;

import processing.app.Base;
import processing.app.Language;
import processing.app.Platform;
import processing.app.Preferences;
import processing.core.PApplet;


public class Welcome {
  Base base;
  WebFrame view;


  public Welcome(Base base, boolean sketchbook) throws IOException {
    this.base = base;

    // TODO this should live inside theme or somewhere modifiable
    Font dialogFont = Toolkit.getSansFont(14, Font.PLAIN);

    JComponent panel = Box.createHorizontalBox();
    panel.setBackground(new Color(245, 245, 245));
    Toolkit.setBorder(panel, 15, 20, 15, 20);

    //panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
    //panel.add(Box.createHorizontalStrut(20));
    JCheckBox checkbox = new JCheckBox("Show this message on startup");
    checkbox.setFont(dialogFont);
    // handles the Help menu invocation, and also the pref not existing
    checkbox.setSelected("true".equals(Preferences.get("welcome.show")));
    checkbox.addItemListener(new ItemListener() {
      @Override
      public void itemStateChanged(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED) {
          Preferences.setBoolean("welcome.show", true);
        } else if (e.getStateChange() == ItemEvent.DESELECTED) {
          Preferences.setBoolean("welcome.show", false);
        }
      }
    });
    panel.add(checkbox);

    panel.add(Box.createHorizontalGlue());

    JButton button = new JButton("Get Started");
    button.setFont(Toolkit.getSansFont(14, Font.PLAIN));
    button.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        view.handleClose();
      }
    });
    panel.add(button);
    //panel.add(Box.createHorizontalGlue());

    view = new WebFrame(getIndexFile(sketchbook), 425, panel) {
      /*
      @Override
      public void handleSubmit(StringDict dict) {
        String sketchbookAction = dict.get("sketchbook", null);
        if ("create_new".equals(sketchbookAction)) {
          File folder = new File(Preferences.getSketchbookPath()).getParentFile();
          PApplet.selectFolder(Language.text("preferences.sketchbook_location.popup"),
                               "sketchbookCallback", folder,
                               this, this);
        }

//        // If un-checked, the key won't be in the dict, so null will be passed
//        boolean keepShowing = "on".equals(dict.get("show_each_time", null));
//        Preferences.setBoolean("welcome.show", keepShowing);
//        Preferences.save();
        handleClose();
      }
      */

      @Override
      public void handleLink(String link) {
        // The link will already have the full URL prefix
        if (link.endsWith("#sketchbook")) {
          File folder = new File(Preferences.getSketchbookPath()).getParentFile();
          PApplet.selectFolder(Language.text("preferences.sketchbook_location.popup"),
                               "sketchbookCallback", folder,
                               this, this);
        } else {
          super.handleLink(link);
        }
      }

      @Override
      public void handleClose() {
        Preferences.setBoolean("welcome.seen", true);
        Preferences.save();
        super.handleClose();
      }
    };
    view.setVisible(true);
  }


  /** Callback for the folder selector. */
  public void sketchbookCallback(File folder) {
    if (folder != null) {
      if (base != null) {
        base.setSketchbookFolder(folder);
//      } else {
//        System.out.println("user selected " + folder);
      }
    }
  }


//  @Override
//  public void handleClose() {
//    dispose();
//  }


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
    htmlFile = Platform.getContentFile("../../../../../shared/lib/" + filename);
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
    Platform.init();

    EventQueue.invokeLater(new Runnable() {
      public void run() {
        try {
          new Welcome(null, true);
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    });
  }
}
