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
  //File sketchbookFolder;
  //String sketchbookPath;  // canonical path

  File examplesFolder;
  String examplesPath;  // canonical path (for comparison)


  public PdeSketchbook(PdeEditor editor) {
    this.editor = editor;

    examplesFolder = new File(System.getProperty("user.dir"), "examples");
    examplesPath = examplesFolder.getCanonicalPath();

    String sketchbookPath = PdePreferences.get("sketchbook.path");

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
      File sketchbookFolder = new File(home, folderName);
      PdePreferences.set("sketchbook.path", 
                         sketchbookFolder.getAbsolutePath());

      if (!sketchbookFolder.exists()) {  // in case it exists already
        sketchbookFolder.mkdirs();
      }

      try {
        sketchbookPath = sketchbookFolder.getCanonicalPath();
      } catch (IOException e) {
        sketchbookPath = sketchbookFolder.getPath();
        e.printStackTrace();
      }

    } else {
      sketchbookFolder = new File(sketchbookPath);
    }

    menu = new JMenu("Open");
  }


  /**
   * Handle creating a sketch folder, return its base .pde file 
   * or null if the operation was cancelled.
   */
  public String handleNew() throws IOException {
    File newbieDir = null;
    String newbieName = null;

    // no sketch has been started, don't prompt for the name if it's 
    // starting up, just make the farker. otherwise if the person hits 
    // 'cancel' i'd have to add a thing to make p5 quit, which is silly. 
    // instead give them an empty sketch, and they can look at examples. 
    // i hate it when imovie makes you start with that goofy dialog box. 
    // unless, ermm, they user tested it and people preferred that as 
    // a way to get started. shite. now i hate myself. 
    // 
    if (PdePreferences.getBoolean("sketchbook.prompt") && !startup) {
      // prompt for the filename and location for the new sketch

      FileDialog fd = new FileDialog(new Frame(), 
                                     "Create new sketch named", 
                                     FileDialog.SAVE);
      fd.setDirectory(PdePreferences.get("sketchbook.path"));
      fd.show();

      String newbieParentDir = fd.getDirectory();
      newbieName = fd.getFile();
      if (newbieName == null) return null;

      newbieDir = new File(newbieParentDir, newbieName);

    } else {
      // use a generic name like sketch_031008a, the date plus a char
      String newbieParentDir = PdePreferences.get("sketchbook.path");

      int index = 0;
      SimpleDateFormat formatter = new SimpleDateFormat("yyMMdd");
      String purty = formatter.format(new Date());
      do {
        newbieName = "sketch_" + purty + ((char) ('a' + index));
        newbieDir = new File(newbieParentDir, newbieName);
        index++;
      } while (newbieDir.exists());
    }

    // make the directory for the new sketch
    newbieDir.mkdirs();

    // make an empty pde file
    File newbieFile = new File(newbieDir, newbieName + ".pde");
    new FileOutputStream(newbieFile);  // create the file

#ifdef MACOS
    // thank you apple, for changing this @#$)(*
    //com.apple.eio.setFileTypeAndCreator(String filename, int, int);

    // TODO this wouldn't be needed if i could figure out how to 
    // associate document icons via a dot-extension/mime-type scenario
    // help me steve jobs. you're my only hope

    // jdk13 on osx, or jdk11
    // though apparently still available for 1.4
    if ((PdeBase.platform == PdeBase.MACOS9) ||
        (PdeBase.platform == PdeBase.MACOSX)) {
      MRJFileUtils.setFileTypeAndCreator(newbieFile,
                                         MRJOSType.kTypeTEXT,
                                         new MRJOSType("Pde1"));
    }
#endif

    // make a note of a newly added sketch in the sketchbook menu
    rebuildMenu();

    // now open it up
    //handleOpen(newbieName, newbieFile, newbieDir);
    //return newSketch;
    return newbieFile.getCanonicalPath();
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

    try {
      //MenuItem newSketchItem = new MenuItem("New Sketch");
      //newSketchItem.addActionListener(this);
      //menu.add(newSkechItem);
      //menu.addSeparator();

      addSketches(menu, sketchbookFolder);

      menu.addSeparator();
      addSketches(menu, examplesFolder);

      // TODO add examples folder here too

      /*
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
      */

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

      /*
      if (addSketches(menu, userFolder, false)) {
        menu.addSeparator();
      }
      if (!addSketches(menu, sketchbookFolder, true)) {
        JMenuItem item = new JMenuItem("No sketches");
        item.setEnabled(false);
        menu.add(item);
      }
      */

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


  protected boolean addSketches(JMenu menu, File folder) throws IOException {
    // skip .DS_Store files, etc
    if (!folder.isDirectory()) return false;

    String list[] = folder.list();
    SketchbookMenuListener listener = 
      new SketchbookMenuListener(folder.getAbsolutePath());

    boolean ifound = false;

    for (int i = 0; i < list.length; i++) {
      //if (list[i].equals(editor.userName) && root) continue;

      if (list[i].equals(".") ||
          list[i].equals("..") ||
          list[i].equals("CVS")) continue;

      File subfolder = new File(folder, list[i]);
      if (new File(subfolder, list[i] + ".pde").exists()) {
        JMenuItem item = new JMenuItem(list[i]);
        item.addActionListener(listener);
        menu.add(item);
        ifound = true;

      } else {  // might contain other dirs, get recursive
        JMenu submenu = new JMenu(list[i]);
        // needs to be separate var 
        // otherwise would set ifound to false
        boolean found = addSketches(submenu, subfolder); //, false);
        if (found) {
          menu.add(submenu);
          ifound = true;
        }
      }
    }
    return ifound;
  }


  /**
   * clear out projects that are empty
   */
  public void clean() {
    if (!PdePreferences.getBoolean("sketchbook.auto_clean")) return;

    //String userPath = base.sketchbookPath + File.separator + userName;
    //File userFolder = new File(userPath);
    File sketchbookFolder = new File(PdePreferences.get("sketchbook.path"));

    //System.out.println("auto cleaning");
    if (sketchbookFolder.exists()) {
      //String entries[] = new File(userPath).list();
      String entries[] = sketchbookFolder.list();
      if (entries != null) {
        for (int j = 0; j < entries.length; j++) {
          //System.out.println(entries[j] + " " + entries.length);

          if ((entries[j].equals(".")) || 
              (entries[j].equals(".."))) continue;

          //File prey = new File(userPath, entries[j]);
          File prey = new File(sketchbookFolder, entries[j]);
          File pde = new File(prey, entries[j] + ".pde");

          // make sure this is actually a sketch folder with a .pde,
          // not a .DS_Store file or another random user folder

          if (pde.exists()) {
            if (PdeBase.calcFolderSize(prey) == 0) {
              //System.out.println("i want to remove " + prey);
              PdeBase.removeDir(prey);
              //} else {
              //System.out.println("not removign because size is " + 
              //                 calcFolderSize(prey));
            }
          }

          //File prey = new File(preyDir, entries[j] + ".pde");
          //if (prey.exists()) {
          //if (prey.length() == 0) {
          // this is a candidate for deletion, but make sure
          // that the user hasn't added anything else to the folder
          
          //System.out.println("remove: " + prey);
          //  removeDir(preyDir);
          //}
          //} else {
          //System.out.println(prey + " doesn't exist.. weird");
          //}
        }
      }
    }
  }
}
