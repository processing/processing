/* -*- mode: jde; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  PdeSketchbook - handles sketchbook mechanics for the sketch menu
  Part of the Processing project - http://Proce55ing.net

  Except where noted, code is written by Ben Fry 
  Copyright (c) 2001-03 Massachusetts Institute of Technology

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

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.zip.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.undo.*;


public class PdeSketchbook {
  PdeEditor editor;

  JMenu menu;
  File sketchbookFolder;
  String sketchbookPath;  // canonical path

  File examplesFolder;
  String examplesPath;  // canonical path (for comparison)


  public PdeSketchbook(PdeEditor editor) {
    this.editor = editor;

    sketchbookPath = PdePreferences.get("sketchbook.path");

    if (sketchbookPath == null) {
      // by default, set default sketchbook path to the user's 
      // home folder with 'sketchbook' as a subdirectory of that
      File home = new File(System.getProperty("user.home"));

      if (PdeBase.platform == PdeBase.MACOSX) {
        // on macosx put the sketchbook in the "Documents" folder
        home = new File(home, "Documents");

      } else if (PdeBase.platform == PdeBase.WINDOWS) {
        // on windows put the sketchbook in the "My Documents" folder
        home = new File(home, "My Documents");
      }

      String folderName = PdePreferences.get("sketchbook.name.default");
      //System.out.println("home = " + home);
      //System.out.println("fname = " + folderName);
      sketchbookFolder = new File(home, folderName);
      PdePreferences.set("sketchbook.path", 
                         sketchbookFolder.getAbsolutePath());

      if (!sketchbookFolder.exists()) {  // in case it exists already
        sketchbookFolder.mkdirs();
      }

      sketchbookPath = sketchbookFolder.getCanonicalPath();

    } else {
      sketchbookFolder = new File(sketchbookPath);
    }

    menu = new JMenu("Open");
  }


  // listener for sketchbk items uses getParent() to figure out
  // the directories above it

  class SketchbookMenuListener implements ActionListener {
    String path;

    public SketchbookMenuListener(String path) {
      this.path = path;
    }

    public void actionPerformed(ActionEvent e) {
      String name = e.getActionCommand();
      editor.skOpen(path + File.separator + name, name);
    }
  }


  public JPopupMenu getPopup() {
    return menu.getPopupMenu();
  }

  //public void rebuildPopup(JPopupMenu popup) {
  //rebuildMenu();
  //popup.
  //}


  public JMenu rebuildMenu() {
    menu.removeAll();
        File prey = new File(sketchbookFolder, entries[j]);
        File pde = new File(prey, entries[j] + ".pde");

        // make sure this is actually a sketch folder with a .pde,
        // not a .DS_Store file or another random user folder
        if (pde.exists()) {
          if (PdeBase.calcFolderSize(prey) == 0) {
            //System.out.println("i want to remove " + prey);
            PdeBase.removeDir(prey);
          }
        }
      }
    }
  }
}
