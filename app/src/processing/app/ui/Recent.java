/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2012-19 The Processing Foundation
  Copyright (c) 2011-12 Ben Fry and Casey Reas

  This program is free software; you can redistribute it and/or modify
  it under the terms of the GNU General Public License version 2
  as published by the Free Software Foundation.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software Foundation,
  Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package processing.app.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

import processing.app.Base;
import processing.app.Language;
import processing.app.Library;
import processing.app.Messages;
import processing.app.Mode;
import processing.app.Preferences;
import processing.core.PApplet;


// TODO this isn't pretty... probably better to do an internal instance fancy thing

// dealing with renaming
//   before sketch save/rename, remove it from the recent list
//   after sketch save/rename add it to the list
//   (this is the more straightforward model, otherwise has lots of weird edge cases)

public class Recent {
  static final String FILENAME = "recent.txt";
  static final String VERSION = "2";

  static Base base;
  static File file;
  static List<Record> records;
  /** actual menu used in the primary menu bar */
  static JMenu mainMenu;
  /** copy of the menu to use in the toolbar */
  static JMenu toolbarMenu;


  static public void init(Base b) {
    base = b;
    file = Base.getSettingsFile(FILENAME);
    mainMenu = new JMenu(Language.text("menu.file.recent"));
    toolbarMenu = new JMenu(Language.text("menu.file.open"));

    try {
      load();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }


  static protected void load() throws IOException {
    records = new ArrayList<>();
    if (file.exists()) {
      BufferedReader reader = PApplet.createReader(file);
      String version = reader.readLine();
      if (version != null && version.equals(VERSION)) {
        String line = null;
        while ((line = reader.readLine()) != null) {
//          String[] pieces = PApplet.split(line, '\t');
//          Record record = new Record(pieces[0], new EditorState(pieces[1]));
//          Record record = new Record(pieces[0]); //, new EditorState(pieces[1]));
//          records.add(record);
          if (new File(line).exists()) {  // don't add ghost entries
            records.add(new Record(line));
          } else {
            Messages.log("ghost file: " + line);
          }
        }
      }
      reader.close();
    }
    updateMenu(mainMenu);
    updateMenu(toolbarMenu);
  }


  static protected void save() {
    file.setWritable(true, false);
    PrintWriter writer = PApplet.createWriter(file);
    writer.println(VERSION);
    for (Record record : records) {
//      System.out.println(record.getPath() + "\t" + record.getState());
//      writer.println(record.path + "\t" + record.getState());
      writer.println(record.path); // + "\t" + record.getState());
    }
    writer.flush();
    writer.close();
    updateMenu(mainMenu);
    updateMenu(toolbarMenu);
  }


  static public JMenu getMenu() {
    return mainMenu;
  }


  static public JMenu getToolbarMenu() {
    return toolbarMenu;
  }


  static private void updateMenu(JMenu menu) {
    menu.removeAll();
    String sketchbookPath = Base.getSketchbookFolder().getAbsolutePath();
    for (Record rec : records) {
      updateMenuRecord(menu, rec, sketchbookPath);
    }
  }


  static private void updateMenuRecord(JMenu menu, final Record rec,
                                       String sketchbookPath) {
    try {
      String recPath = new File(rec.getPath()).getParent();
      String purtyPath = null;

      if (recPath.startsWith(sketchbookPath)) {
        purtyPath = "sketchbook \u2192 " +
          recPath.substring(sketchbookPath.length() + 1);
      } else {
        List<Mode> modes = base.getModeList();
        for (Mode mode : modes) {
          File examplesFolder = mode.getExamplesFolder();
          String examplesPath = examplesFolder.getAbsolutePath();
          if (recPath.startsWith(examplesPath)) {
            String modePrefix = mode.getTitle() + " ";
            if (mode.getTitle().equals("Standard")) {
              modePrefix = "";  // "Standard examples" is dorky
            }
            purtyPath = modePrefix + "examples \u2192 " +
              recPath.substring(examplesPath.length() + 1);
            break;
          }

          if (mode.coreLibraries != null) {
            for (Library lib : mode.coreLibraries) {
              examplesFolder = lib.getExamplesFolder();
              examplesPath = examplesFolder.getAbsolutePath();
              if (recPath.startsWith(examplesPath)) {
                purtyPath = lib.getName() + " examples \u2192 " +
                  recPath.substring(examplesPath.length() + 1);
                break;
              }
            }
          }

          if (mode.contribLibraries != null) {
            for (Library lib : mode.contribLibraries) {
              examplesFolder = lib.getExamplesFolder();
              examplesPath = examplesFolder.getAbsolutePath();
              if (recPath.startsWith(examplesPath)) {
                purtyPath = lib.getName() + " examples \u2192 " +
                  recPath.substring(examplesPath.length() + 1);
                break;
              }
            }
          }
        }
      }
      if (purtyPath == null) {
        String homePath = System.getProperty("user.home");
        if (recPath.startsWith(homePath)) {
          // Not localized, but this is gravy. It'll work on OS X & EN Windows
          String desktopPath = homePath + File.separator + "Desktop";
          if (recPath.startsWith(desktopPath)) {
            purtyPath = "Desktop \u2192 " + recPath.substring(desktopPath.length() + 1);
          } else {
            //purtyPath = "\u2302 \u2192 " + recPath.substring(homePath.length() + 1);
            //purtyPath = "Home \u2192 " + recPath.substring(homePath.length() + 1);
            String userName = new File(homePath).getName();
            //purtyPath = "\u2302 " + userName + " \u2192 " + recPath.substring(homePath.length() + 1);
            purtyPath = userName + " \u2192 " + recPath.substring(homePath.length() + 1);
          }
        } else {
          purtyPath = recPath;
        }
      }

//      JMenuItem item = new JMenuItem(rec.getName() + " | " + purtyPath);
      JMenuItem item = new JMenuItem(purtyPath);
      item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          // Base will call handle() (below) which will cause this entry to
          // be removed from the list and re-added to the end. If already
          // opened, Base will bring the window forward, and also call handle()
          // so that it's re-queued to the newest slot in the Recent menu.
          base.handleOpen(rec.path);
//          if (rec.sketch == null) {
//            // this will later call 'add' to put it back on the stack
//            base.handleOpen(rec.path); //, rec.state);
////            int index = findRecord(rec);
////            if (index != -1) {
////              records.remove(index);  // remove from the list
////              save();  // write the recent file with the latest
////            }
//          } else {
////              System.out.println("sketch not null in handleOpen: " + record.getPath());
//          }
        }
      });
      //menu.add(item);
      menu.insert(item, 0);

    } catch (Exception e) {
      // Strange things can happen... report them for the geeky and move on:
      // https://github.com/processing/processing/issues/2463
      e.printStackTrace();
    }
  }


  synchronized static public void remove(Editor editor) {
    int index = findRecord(editor.getSketch().getMainFilePath());
    if (index != -1) {
      records.remove(index);
    }
  }


//  synchronized void handleRename(String oldPath, String newPath) {
//    int index = findRecord(oldPath);
//    if (index != -1) {
//      Record rec = records.get(index);
//      rec.path = newPath;
//      save();
//    } else {
//      Base.log(this, "Could not find " + oldPath +
//               " in list of recent sketches to replace with " + newPath);
//    }
//  }


//  /** Opened from the recent sketches menu. */
//  synchronized void handleOpen(Record record) {
//    if (record.sketch == null) {
////      Editor editor = base.handleOpen(record.path, record.state);
//      base.handleOpen(record.path, record.state);
//      int index = findRecord(record);
//      if (index != -1) {
//        records.remove(index);  // remove from the list
////      if (editor != null) {
////        record.sketch = editor.getSketch();
////        records.add(record);  // move to the end of the line
////      }
//        save();  // write the recent file with the latest
//      }
//    } else {
////      System.out.println("sketch not null in handleOpen: " + record.getPath());
//    }
//    // otherwise the other handleOpen() will be called once it opens
//  }


  /**
   * Called by Base when a new sketch is opened, to add the sketch to the last
   * entry on the Recent queue. If the sketch is already in the list, it is
   * first removed so it doesn't show up multiple times.
   */
  synchronized static public void append(Editor editor) {
    if (!editor.getSketch().isUntitled()) {
      // If this sketch is already in the menu, remove it
      remove(editor);

      if (records.size() == Preferences.getInteger("recent.count")) {
        records.remove(0);  // remove the first entry
      }

//      new Exception("adding to recent: " + editor.getSketch().getMainFilePath()).printStackTrace(System.out);
//    Record newRec = new Record(editor, editor.getSketch());
//    records.add(newRec);
      records.add(new Record(editor));
//    updateMenu();
      save();
//    } else {
//      new Exception("NOT adding to recent: " + editor.getSketch().getMainFilePath()).printStackTrace(System.out);
    }
  }


  synchronized static public void rename(Editor editor, String oldPath) {
    if (records.size() == Preferences.getInteger("recent.count")) {
      records.remove(0);  // remove the first entry
    }
    int index = findRecord(oldPath);
    //check if record exists
    if (index != -1) {
      records.remove(index);
    }
    records.add(new Record(editor));
    save();
  }


  static int findRecord(String path) {
    for (int i = 0; i < records.size(); i++) {
      if (path.equals(records.get(i).path)) {
        return i;
      }
    }
    return -1;
  }


//  int findRecord(Record rec) {
//    int index = records.indexOf(rec);
//    if (index != -1) {
//      return index;
//    }
//    return findRecord(rec.path);
////    index = 0;
////    for (Record r : records) {
////      System.out.println("checking " + r + "\n  against " + rec);
////      if (r.equals(rec)) {
////        return index;
////      }
////      index++;
////    }
////    return -1;
//  }


  static class Record {
    String path;  // if not loaded, this is non-null
//    EditorState state;  // if not loaded, this is non-null

//    Sketch sketch;

//    Record(String path, EditorState state) {
//      this.path = path;
//      this.state = state;
//    }

    Record(String path) {
      this.path = path;
    }

//    Record(Editor editor, Sketch sketch) {
//      this.editor = editor;
//      this.sketch = sketch;
//    }

    Record(Editor editor) {
      this(editor.getSketch().getMainFilePath());
    }

    String getName() {
      // Get the filename of the .pde (or .js or .py...)
      String name = path.substring(path.lastIndexOf(File.separatorChar) + 1);
      // Return the name with the extension removed
      return name.substring(0, name.indexOf('.'));
    }

    String getPath() {
      return path;
    }

//    String getPath() {
//      if (sketch != null) {
//        return sketch.getMainFilePath();
//      } else {
//        return path;
//      }
//    }

//    EditorState getState() {
////      return sketch == null ? state : editor.getEditorState();
//      if (editor != null) {  // update state if editor is open
//        state = editor.getEditorState();
//      }
//      return state;
//    }

//    public boolean equals(Object o) {
//      Record r = (Record) o;
//      return getPath().equals(r.getPath());
//    }
  }
}
