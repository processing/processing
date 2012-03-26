/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

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

package processing.app;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.ArrayList;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

import processing.core.PApplet;


// dealing with renaming
//   before sketch save/rename, remove it from the recent list
//   after sketch save/rename add it to the list
//   need to do this whether or not 

public class Recent {
  
  static final String FILENAME = "recent.txt";
  static final String VERSION = "1";
  
  Base base;
  
  File file;
  int count;
  ArrayList<Record> records;


  public Recent(Base base) {
    this.base = base;
    count = Preferences.getInteger("recent.count");
    file = Base.getSettingsFile(FILENAME);
    try {
      load();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
  
  
  protected void load() throws IOException {
    records = new ArrayList<Record>();
    if (file.exists()) {
      BufferedReader reader = PApplet.createReader(file);
      String version = reader.readLine();
      if (version != null && version.equals(VERSION)) {
        String line = null;
        while ((line = reader.readLine()) != null) {
          String[] pieces = PApplet.split(line, '\t');
          Record record = new Record(pieces[0], new EditorState(pieces[1])); 
          records.add(record);
        }
      }
    }
  }


  protected void save() {
    PrintWriter writer = PApplet.createWriter(file);
    writer.println(VERSION);
    for (Record record : records) {
//      System.out.println(record.getPath() + "\t" + record.getState());
      writer.println(record.getPath() + "\t" + record.getState());
//      if (record.sketch == null) {
//        writer.println(record.path + "\t" + record.state);
//      } else {
//        writer.println(record.)
//      }
    }
    writer.flush();
    writer.close();
//    System.out.println();
  }
  
  
  public JMenu createMenu() {
    JMenu menu = new JMenu("Recent Sketches");
    updateMenu(menu);
    return menu;
  }
  
  
  public void updateMenu(JMenu menu) {
    menu.removeAll();
    for (final Record rec : records) {
      JMenuItem item = new JMenuItem(rec.getName());
      item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          handleOpen(rec);
        }
      });
      menu.add(item);
    }
  }
  
  
  /** Opened from the recent sketches menu. */
  synchronized void handleOpen(Record record) {
    if (record.sketch == null) {
//      Editor editor = base.handleOpen(record.path, record.state);
      base.handleOpen(record.path, record.state);
      int index = findRecord(record);
      if (index != -1) {
        records.remove(index);  // remove from the list
//      if (editor != null) {
//        record.sketch = editor.getSketch();
//        records.add(record);  // move to the end of the line
//      }
        save();  // write the recent file with the latest
      }
    }
    // otherwise the other handleOpen() will be called once it opens 
  }
  
  
  /** Called by Base when a new sketch is opened. */
  synchronized void handleOpen(Editor editor, JMenu menu) {
    Record newRec = new Record(editor, editor.getSketch());
    // If this is already in the menu, remove it
    int index = findRecord(newRec);
    if (index != -1) {
      records.remove(index);
    }
    if (records.size() == count) {
      records.remove(0);  // remove the first entry
    }
    records.add(newRec);
    updateMenu(menu);
    save();
  }
  
  
  int findRecord(Record rec) {
    int index = records.indexOf(rec);
    if (index != -1) {
      return index;
    }
    index = 0;
    for (Record r : records) {
//      System.out.println("checking " + r + "\n  against " + rec);
      if (r.equals(rec)) {
        return index;
      }
      index++;
    }
    return -1;
  }
  
  
  class Record {
    String path;  // if not loaded, this is non-null
    EditorState state;  // if not loaded, this is non-null

    /**
     * If currently loaded, this is non-null, and takes precedence over the 
     * path and state information, which will instead be stored actively by
     * the actual sketch object.
     */
    Sketch sketch;
    Editor editor;

    Record(String path, EditorState state) {
      this.path = path;
      this.state = state;
    }
    
    Record(Editor editor, Sketch sketch) {
      this.editor = editor;
      this.sketch = sketch;
    }

    String getName() {
      if (sketch != null) {
        return sketch.getName();
      }
      // Get the filename of the .pde (or .js or .py...)
      String name = path.substring(path.lastIndexOf(File.separatorChar) + 1);
      // Return the name with the extension removed
      return name.substring(0, name.indexOf('.'));
    }

    String getPath() {
      if (sketch != null) {
        return sketch.getMainFilePath();
      } else {
        return path;
      }
    }

    EditorState getState() {
      //return sketch != null ? sketch.getEditor() state;
      return sketch == null ? state : editor.getEditorState(); 
    }
    
    public boolean equals(Object o) {
      Record r = (Record) o;
      return getPath().equals(r.getPath());
    }
  }


//  class Record {
//    // path to the main .pde file for the sketch 
//    String path;
//    // placement of the window
////    int windowX, windowY, windowW, windowH;
//    Rectangle editorBounds;
//    // width/height of the screen on which this window was placed
////    int displayW, displayH;
//    String deviceName;
//    Rectangle deviceBounds;
//
//    
//    Record(BufferedReader reader) throws IOException {
//      String line = reader.readLine();
//      String[] pieces = PApplet.split(line, '\t');
//      path = pieces[0];
//      
////      windowX = Integer.parseInt(pieces[1]);
////      windowY = Integer.parseInt(pieces[2]);
////      windowW = Integer.parseInt(pieces[3]);
////      windowH = Integer.parseInt(pieces[4]);
//      
////      displayW = Integer.parseInt(pieces[5]);
////      displayH = Integer.parseInt(pieces[6]);
//    }
//
//
//    void update(Editor editor) {
//      path = editor.getSketch().getMainFilePath();
//      editorBounds = editor.getBounds();
//      GraphicsConfiguration config = editor.getGraphicsConfiguration();
//      GraphicsDevice device = config.getDevice();
//      deviceBounds = config.getBounds();
//      deviceName = device.getIDstring();
//    }
//    
//    
//    void write(PrintWriter writer) {
//      writer.print(path);
//      writer.print('\t');
//      writeRect(writer, editorBounds);
//      writer.print('\t');
//      writer.print(deviceName);
//      writer.print('\t');
//      writeRect(writer, deviceBounds);
//    }
//
//
//    void writeRect(PrintWriter writer, Rectangle rect) {
//      writer.print(rect.x);
//      writer.print('\t');
//      writer.print(rect.y);
//      writer.print('\t');
//      writer.print(rect.width);
//      writer.print('\t');
//      writer.print(rect.height);
//    }
//  }
  
//  Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
//  Preferences.setInteger("last.screen.width", screen.width);
//  Preferences.setInteger("last.screen.height", screen.height);
//
//  String untitledPath = untitledFolder.getAbsolutePath();
//
//  // Save the sketch path and window placement for each open sketch
//  int index = 0;
//  for (Editor editor : editors) {
//    String path = editor.getSketch().getMainFilePath();
//    // In case of a crash, save untitled sketches if they contain changes.
//    // (Added this for release 0158, may not be a good idea.)
//    if (path.startsWith(untitledPath) &&
//        !editor.getSketch().isModified()) {
//      continue;
//    }
//    Preferences.set("last.sketch" + index + ".path", path);
//
//    int[] location = editor.getPlacement();
//    String locationStr = PApplet.join(PApplet.str(location), ",");
//    Preferences.set("last.sketch" + index + ".location", locationStr);
//    index++;
//  }
//  Preferences.setInteger("last.sketch.count", index);
//  Preferences.set("last.sketch.mode", defaultMode.getClass().getName());
}