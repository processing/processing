/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2011 Ben Fry and Casey Reas

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

import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.io.*;

import processing.core.PApplet;


public class Recent {
  static final String FILENAME = "recent.txt";
  static final String VERSION = "1";
  File file;

  public Recent() throws IOException {
    file = Base.getSettingsFile(FILENAME);
    if (!file.exists()) {
      BufferedReader reader = PApplet.createReader(file);
      String version = reader.readLine();
      if (version != null && version.equals(VERSION)) {
        
      }
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