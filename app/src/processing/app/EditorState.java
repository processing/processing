/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2004-11 Ben Fry and Casey Reas
  Copyright (c) 2001-04 Massachusetts Institute of Technology

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

import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;


import processing.core.PApplet;

  public class EditorState {
    // path to the main .pde file for the sketch 
//    String path;
    // placement of the window
//    int windowX, windowY, windowW, windowH;
    Rectangle editorBounds;
    int dividerLocation;
    // width/height of the screen on which this window was placed
//    int displayW, displayH;
    String deviceName;
    Rectangle deviceBounds;

    
    EditorState(List<Editor> editors) {
      defaultConfig();
      defaultLocation(editors);
    }
    
    
    EditorState(BufferedReader reader) throws IOException {
      String line = reader.readLine();
      String[] pieces = PApplet.split(line, '\t');
//      path = pieces[0];
      
//      windowX = Integer.parseInt(pieces[1]);
//      windowY = Integer.parseInt(pieces[2]);
//      windowW = Integer.parseInt(pieces[3]);
//      windowH = Integer.parseInt(pieces[4]);
      
//      displayW = Integer.parseInt(pieces[5]);
//      displayH = Integer.parseInt(pieces[6]);
    }

    // scenarios:
    // 1) new untitled sketch (needs device, needs bounds)
    // 2) restoring sketch from recent menu
    //    - device cannot be found
    //    - device is found but it's a different size
    //    - device is found and size is correct
    // 3) re-opening sketch in a new mode

    GraphicsConfiguration checkConfig() {
      GraphicsEnvironment graphicsEnvironment = 
        GraphicsEnvironment.getLocalGraphicsEnvironment();
      GraphicsDevice[] screenDevices = graphicsEnvironment.getScreenDevices();
      for (GraphicsDevice device : screenDevices) {
        GraphicsConfiguration[] configurations = device.getConfigurations();
        for (GraphicsConfiguration config : configurations) {
          if (config.getDevice().getIDstring().equals(deviceName)) { 
            if (deviceBounds != null && config.getBounds().equals(deviceBounds)) {
              return config;
            } else {
              
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
      deviceName = device.getIDstring();
      deviceBounds = config.getBounds();
      return config;
    }


    void defaultLocation(List<Editor> editors) {
      int defaultWidth = Preferences.getInteger("editor.window.width.default");
      int defaultHeight = Preferences.getInteger("editor.window.height.default");

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
          final int OVER = 50;
          Editor lastOpened = editors.get(editors.size() - 1);
          editorBounds = lastOpened.getBounds();
          editorBounds.x += OVER;
          editorBounds.y += OVER;
          dividerLocation = lastOpened.getDividerLocation();

          if (!deviceBounds.contains(editorBounds)) {
            // Warp the next window to a randomish location on screen.
            editorBounds.x = deviceBounds.x + (int) (Math.random() * (deviceBounds.width - defaultWidth));
            editorBounds.y = deviceBounds.y + (int) (Math.random() * (deviceBounds.height - defaultHeight));
          }
        }
      }
    }


    void update(Editor editor) {
//    path = editor.getSketch().getMainFilePath();
      editorBounds = editor.getBounds();
      dividerLocation = editor.getDividerLocation();
      GraphicsConfiguration config = editor.getGraphicsConfiguration();
      GraphicsDevice device = config.getDevice();
      deviceBounds = config.getBounds();
      deviceName = device.getIDstring();
    }


    void apply(Editor editor) {
      editor.setBounds(editorBounds);
      if (dividerLocation != 0) {
        editor.setDividerLocation(dividerLocation);
      }
    }
    
    
    void write(PrintWriter writer) {
//      writer.print(path);
      writer.print('\t');
      writeRect(writer, editorBounds);
      writer.print('\t');
      writer.print(deviceName);
      writer.print('\t');
      writeRect(writer, deviceBounds);
    }


    void writeRect(PrintWriter writer, Rectangle rect) {
      writer.print(rect.x);
      writer.print('\t');
      writer.print(rect.y);
      writer.print('\t');
      writer.print(rect.width);
      writer.print('\t');
      writer.print(rect.height);
    }
  }