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

import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.io.*;
import java.util.List;

import processing.app.Preferences;
import processing.core.PApplet;

//import processing.core.PApplet;


// scenarios:
// 1) new untitled sketch (needs device, needs bounds)
// 2) restoring sketch from recent menu
//    - device cannot be found
//    - device is found but it's a different size
//    - device is found and size is correct
// 3) re-opening sketch in a new mode


public class EditorState {
  // path to the main .pde file for the sketch
//    String path;
  // placement of the window
//    int windowX, windowY, windowW, windowH;
  Rectangle editorBounds;
  int dividerLocation;
  // width/height of the screen on which this window was placed
//    int displayW, displayH;
//    String deviceName;  // not really useful b/c it's more about bounds anyway
  Rectangle deviceBounds;
  boolean isMaximized;

  // how far to offset a new window from the previous window
  static final int WINDOW_OFFSET = 28;


  /**
   * Create a fresh editor state object from the default screen device and
   * set its placement relative to the last opened window.
   * @param editors List of active editor objects
   */
  public EditorState(List<Editor> editors) {
    defaultConfig();
    defaultLocation(editors);
  }


//    EditorState(BufferedReader reader) throws IOException {
//      String line = reader.readLine();
//  EditorState(String[] pieces) throws IOException {
  EditorState(String info) throws IOException {
//      String line = reader.readLine();
//      String[] pieces = PApplet.split(line, '\t');
      String[] pieces = PApplet.split(info, ',');
//      path = pieces[0];

    editorBounds = new Rectangle(Integer.parseInt(pieces[0]),
                                 Integer.parseInt(pieces[1]),
                                 Integer.parseInt(pieces[2]),
                                 Integer.parseInt(pieces[3]));

    dividerLocation = Integer.parseInt(pieces[4]);

    deviceBounds = new Rectangle(Integer.parseInt(pieces[5]),
                                 Integer.parseInt(pieces[6]),
                                 Integer.parseInt(pieces[7]),
                                 Integer.parseInt(pieces[8]));

//      windowX = Integer.parseInt(pieces[1]);
//      windowY = Integer.parseInt(pieces[2]);
//      windowW = Integer.parseInt(pieces[3]);
//      windowH = Integer.parseInt(pieces[4]);

//      displayW = Integer.parseInt(pieces[5]);
//      displayH = Integer.parseInt(pieces[6]);
  }


  public String toString() {
    return (editorBounds.x + "," +
            editorBounds.y + "," +
            editorBounds.width + "," +
            editorBounds.height + "," +
            dividerLocation + "," +
            deviceBounds.x + "," +
            deviceBounds.y + "," +
            deviceBounds.width + "," +
            deviceBounds.height);
  }


  /**
   * Returns a GraphicsConfiguration so that a new Editor Frame can be
   * constructed. First tries to match the bounds for this state information
   * to an existing config (nominally, a display) and if that doesn't work,
   * then returns the default configuration/default display.
   */
  GraphicsConfiguration checkConfig() {
    if (deviceBounds != null) {
      GraphicsEnvironment graphicsEnvironment =
        GraphicsEnvironment.getLocalGraphicsEnvironment();
      GraphicsDevice[] screenDevices = graphicsEnvironment.getScreenDevices();
      for (GraphicsDevice device : screenDevices) {
        GraphicsConfiguration[] configurations = device.getConfigurations();
        for (GraphicsConfiguration config : configurations) {
//          if (config.getDevice().getIDstring().equals(deviceName)) {
          if (config.getBounds().equals(deviceBounds)) {
            return config;
          }
        }
      }
    }
    // otherwise go to the default config
    return defaultConfig();
  }


  GraphicsConfiguration defaultConfig() {
    GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
    GraphicsDevice device = ge.getDefaultScreenDevice();
    GraphicsConfiguration config = device.getDefaultConfiguration();
//      deviceName = device.getIDstring();
    deviceBounds = config.getBounds();
    return config;
  }


  /**
   * Figure out the next location by sizing up the last editor in the list.
   * If no editors are opened, it'll just open on the main screen.
   * @param editors List of editors currently opened
   */
  void defaultLocation(List<Editor> editors) {
    int defaultWidth =
      Toolkit.zoom(Preferences.getInteger("editor.window.width.default"));
    int defaultHeight =
      Toolkit.zoom(Preferences.getInteger("editor.window.height.default"));

    defaultWidth = Math.min(defaultWidth, deviceBounds.width);
    defaultHeight = Math.min(defaultHeight, deviceBounds.height);
    //System.out.println("default w/h = " + defaultWidth + "/" + defaultHeight);

    if (editors.size() == 0) {
      // If no current active editor, use default placement.
      // Center the window on ths screen, taking into account that the
      // upper-left corner of the device may have a non (0, 0) origin.
      int editorX =
        deviceBounds.x + (deviceBounds.width - defaultWidth) / 2;
      int editorY =
        deviceBounds.y + (deviceBounds.height - defaultHeight) / 2;
      editorBounds =
        new Rectangle(editorX, editorY, defaultWidth, defaultHeight);
      dividerLocation = 0;

    } else {
      // With a currently active editor, open the new window using the same
      // dimensions and divider location, but offset slightly.
      synchronized (editors) {
        Editor lastOpened = editors.get(editors.size() - 1);
        isMaximized = (lastOpened.getExtendedState() == Frame.MAXIMIZED_BOTH);
        editorBounds = lastOpened.getBounds();
        editorBounds.x += WINDOW_OFFSET;
        editorBounds.y += WINDOW_OFFSET;
        dividerLocation = lastOpened.getDividerLocation();

        if (!deviceBounds.contains(editorBounds)) {
          // Warp the next window to a randomish location on screen.
          editorBounds.x = deviceBounds.x +
            (int) (Math.random() * (deviceBounds.width - defaultWidth));
          editorBounds.y = deviceBounds.y +
            (int) (Math.random() * (deviceBounds.height - defaultHeight));
        }
        if (isMaximized) {
          editorBounds.width = defaultWidth;
          editorBounds.height = defaultHeight;
        }
      }
    }
  }


  void update(Editor editor) {
//    path = editor.getSketch().getMainFilePath();
    editorBounds = editor.getBounds();
    dividerLocation = editor.getDividerLocation();
    GraphicsConfiguration config = editor.getGraphicsConfiguration();
//      GraphicsDevice device = config.getDevice();
    deviceBounds = config.getBounds();
//      deviceName = device.getIDstring();
  }


  void apply(Editor editor) {
    editor.setBounds(editorBounds);

    if (dividerLocation == 0) {
      dividerLocation = 2 * editor.getSize().height / 3;
    }
    editor.setDividerLocation(dividerLocation);

    if (isMaximized) {
      editor.setExtendedState(Frame.MAXIMIZED_BOTH);
    }
  }


//  void write(PrintWriter writer) {
////      writer.print(path);
//    writer.print('\t');
//    writeRect(writer, editorBounds);
////      writer.print('\t');
////      writer.print(deviceName);
//    writer.print('\t');
//    writeRect(writer, deviceBounds);
//  }
//
//
//  void writeRect(PrintWriter writer, Rectangle rect) {
//    writer.print(rect.x);
//    writer.print('\t');
//    writer.print(rect.y);
//    writer.print('\t');
//    writer.print(rect.width);
//    writer.print('\t');
//    writer.print(rect.height);
//  }
}
