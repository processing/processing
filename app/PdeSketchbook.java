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

  //JMenu sketchbookMenu;
  JMenu menu;
  File sketchbookFolder;
  String sketchbookPath;


  public PdeSketchbook(PdeEditor editor) {
    this.editor = editor;
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


  public JMenu rebuildMenu() {
    if (menu == null) {
      menu = new JMenu("Open");
    } else {
      menu.removeAll();
    }

    try {
      //MenuItem newSketchItem = new MenuItem("New Sketch");
      //newSketchItem.addActionListener(this);
      //menu.add(newSkechItem);
      //menu.addSeparator();

      sketchbookFolder = 
        new File(PdePreferences.get("sketchbook.path", "sketchbook"));
      sketchbookPath = sketchbookFolder.getAbsolutePath();
      if (!sketchbookFolder.exists()) {
        System.err.println("sketchbook folder doesn't exist, " + 
                           "making a new one");
        sketchbookFolder.mkdirs();
      }

      // files for the current user (for now, most likely 'default')

      // header knows what the current user is
      String userPath = sketchbookPath + 
        File.separator + editor.userName;

      File userFolder = new File(userPath);
      if (!userFolder.exists()) {
        System.err.println("sketchbook folder for '" + editor.userName + 
                           "' doesn't exist, creating a new one");
        userFolder.mkdirs();
      }

      /*
      SketchbookMenuListener userMenuListener = 
        new SketchbookMenuListener(userPath);

      String entries[] = new File(userPath).list();
      boolean added = false;
      for (int j = 0; j < entries.length; j++) {
        if (entries[j].equals(".") || 
            entries[j].equals("..") ||
            entries[j].equals("CVS")) continue;
        //entries[j].equals(".cvsignore")) continue;
        added = true;
        if (new File(userPath, entries[j] + File.separator + 
                     entries[j] + ".pde").exists()) {
          MenuItem item = new MenuItem(entries[j]);
          item.addActionListener(userMenuListener);
          menu.add(item);
        }
        //submenu.add(entries[j]);
      }
      if (!added) {
        MenuItem item = new MenuItem("No sketches");
        item.setEnabled(false);
        menu.add(item);
      }
      menu.addSeparator();
      */
      if (addSketches(menu, userFolder, false)) {
        menu.addSeparator();
      }
      if (!addSketches(menu, sketchbookFolder, true)) {
        MenuItem item = new MenuItem("No sketches");
        item.setEnabled(false);
        menu.add(item);
      }

      /*
      // doesn't seem that refresh is worthy of its own menu item
      // people can stop and restart p5 if they want to muck with it
      menu.addSeparator();
      MenuItem item = new MenuItem("Refresh");
      item.addActionListener(this);
      menu.add(item);
      */

    } catch (IOException e) {
      e.printStackTrace();
    }
    return menu;
  }


  protected boolean addSketches(Menu menu, File folder, 
                                /*boolean allowUser,*/ boolean root) 
    throws IOException {
    // skip .DS_Store files, etc
    if (!folder.isDirectory()) return false;

    String list[] = folder.list();
    SketchbookMenuListener listener = 
      new SketchbookMenuListener(folder.getAbsolutePath());

    boolean ifound = false;

    for (int i = 0; i < list.length; i++) {
      if (list[i].equals(editor.userName) && root) continue;

      if (list[i].equals(".") ||
          list[i].equals("..") ||
          list[i].equals("CVS")) continue;

      File subfolder = new File(folder, list[i]);
      if (new File(subfolder, list[i] + ".pde").exists()) {
        MenuItem item = new MenuItem(list[i]);
        item.addActionListener(listener);
        menu.add(item);
        ifound = true;

      } else {  // might contain other dirs, get recursive
        Menu submenu = new Menu(list[i]);
        // needs to be separate var 
        // otherwise would set ifound to false
        boolean found = addSketches(submenu, subfolder, false);
        if (found) {
          menu.add(submenu);
          ifound = true;
        }
      }
    }
    return ifound;
  } 
}
