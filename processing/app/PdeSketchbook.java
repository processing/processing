/* -*- mode: jde; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  PdeSketchbook - handles sketchbook mechanics for the sketch menu
  Part of the Processing project - http://processing.org

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
import java.text.*;
import java.util.*;
import java.util.zip.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.undo.*;

#ifdef MACOS
import com.apple.mrj.*;
#endif


public class PdeSketchbook {
  PdeEditor editor;

  JMenu menu;
  //File sketchbookFolder;
  //String sketchbookPath;  // canonical path

  // last file/directory used for file opening
  String handleOpenDirectory;

  File examplesFolder;
  String examplesPath;  // canonical path (for comparison)


  public PdeSketchbook(PdeEditor editor) {
    this.editor = editor;

    // this shouldn't change throughout.. it may as well be static 
    // but only one instance of sketchbook will be built so who cares
    examplesFolder = new File(System.getProperty("user.dir"), "examples");
    examplesPath = examplesFolder.getAbsolutePath();

    //String sketchbookPath = PdePreferences.get("sketchbook.path");
    //if (sketchbookPath == null) {
    if (PdePreferences.get("sketchbook.path") == null) {
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

      if (!sketchbookFolder.exists()) sketchbookFolder.mkdirs();
      //sketchbookPath = sketchbookFolder.getAbsolutePath();
    //} else {
      //sketchbookFolder = new File(sketchbookPath);
    }
    menu = new JMenu("Open");
  }


  /**
   * Handle creating a sketch folder, return its base .pde file 
   * or null if the operation was cancelled.
   */
  public String handleNew(boolean startup) throws IOException {
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
    // thank you apple, for changing this @#$)(*
    //com.apple.eio.setFileTypeAndCreator(String filename, int, int)
#endif

    // make a note of a newly added sketch in the sketchbook menu
    rebuildMenu();

    // now open it up
    //handleOpen(newbieName, newbieFile, newbieDir);
    //return newSketch;
    return newbieFile.getAbsolutePath();
  }


  public String handleOpen() {
    FileDialog fd = new FileDialog(new Frame(), 
                                   "Open a Processing sketch...", 
                                   FileDialog.LOAD);
    if (handleOpenDirectory == null) {
      handleOpenDirectory = PdePreferences.get("sketchbook.path");
    }
    fd.setDirectory(handleOpenDirectory);

    // only show .pde files as eligible bachelors
    fd.setFilenameFilter(new FilenameFilter() {
        public boolean accept(File dir, String name) {
          return name.endsWith(".pde");
        }
      });

    // gimme some money
    fd.show();

    // what in the hell yu want, boy?
    String directory = fd.getDirectory();
    String filename = fd.getFile();

    // user cancelled selection
    if (filename == null) return null;

    // this may come in handy sometime
    handleOpenDirectory = directory;

    File selection = new File(directory, filename);
    return selection.getAbsolutePath();
  }


  // listener for sketchbk items uses getParent() to figure out
  // the directories above it

  /*
  class SketchbookMenuListener implements ActionListener {
    String path;

    public SketchbookMenuListener(String path) {
      this.path = path;
    }

    public void actionPerformed(ActionEvent e) {
      //String name = e.getActionCommand();
      //editor.skOpen(path + File.separator + name, name);
      editor.handleOpen(
    }
  }
  */

  public JPopupMenu getPopupMenu() {
    return menu.getPopupMenu();
  }

  //public void rebuildPopup(JPopupMenu popup) {
  //rebuildMenu();
  //popup.
  //}


  public JMenu rebuildMenu() {
    menu.removeAll();

    try {
      addSketches(menu, sketchbookFolder);
      menu.addSeparator();
      addSketches(menu, examplesFolder);

    } catch (IOException e) {
      PdeBase.showWarning("Problem while building sketchbook menu",
                          "There was a problem with building the\n" +
                          "sketchbook menu. Things might get a little\n" +
                          "kooky around here.\n", e);
      //e.printStackTrace();
    }
    return menu;
  }


  protected boolean addSketches(File folder) throws IOException {
    // skip .DS_Store files, etc
    if (!folder.isDirectory()) return false;

    String list[] = folder.list();
    //SketchbookMenuListener listener = 
    //new SketchbookMenuListener(folder.getAbsolutePath());

    ActionListener listener = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          editor.handleOpen(e.getActionCommand());
        }
      };

    boolean ifound = false;

    for (int i = 0; i < list.length; i++) {
      if ((list[i].charAt(0) == '.') ||
          list[i].equals("CVS")) continue;

      File subfolder = new File(folder, list[i]);
      File entry = new File(subfolder, list[i] + ".pde");
      if (entry.exists()) {
        JMenuItem item = new JMenuItem(list[i]);
        item.addActionListener(listener);
        item.setActionCommand(entry.getAbsolutePath());
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
    return ifound;  // actually ignored, but..
  }


  /**
   * Clear out projects that are empty.
   */
  public void clean() {
    System.err.println("TODO sketchbook.clean() is disabled");
    if (true) return;  // fool the compiler

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

          if (entries[j].charAt(0) == '.') continue;

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
        }
      }
    }
  }
}
