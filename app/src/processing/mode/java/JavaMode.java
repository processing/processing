/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2010 Ben Fry and Casey Reas

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

package processing.mode.java;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import processing.app.Base;
import processing.app.Editor;
import processing.app.Mode;
import processing.app.RunnerListener;
import processing.app.Sketch;
import processing.app.SketchException;
import processing.app.syntax.PdeKeywords;
import processing.core.PApplet;
import processing.mode.java.runner.Runner;


public class JavaMode extends Mode {
  private Runner runtime;

  // classpath for all known libraries for p5
  // (both those in the p5/libs folder and those with lib subfolders
  // found in the sketchbook)
//  static public String librariesClassPath;

  
  public Editor createEditor(Base base, String path, int[] location) {
    return new JavaEditor(base, path, location, this);
  }


  public JavaMode(Base base, File folder) {
    super(base, folder);

    try {
      loadKeywords();
    } catch (IOException e) {
      Base.showError("Problem loading keywords",
                     "Could not load keywords.txt, please re-install Processing.", e);
    }
    
    /*
    item = newJMenuItem("Export", 'E');
    if (editor != null) {
      item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          editor.handleExport();
        }
      });
    } else {
      item.setEnabled(false);
    }
    fileMenu.add(item);

    item = newJMenuItemShift("Export Application", 'E');
    if (editor != null) {
      item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          editor.handleExportApplication();
        }
      });
    } else {
      item.setEnabled(false);
    }
    fileMenu.add(item);
     */
  }


  protected void loadKeywords() throws IOException {
    File file = new File(folder, "keywords.txt");
    BufferedReader reader = PApplet.createReader(file);

    tokenMarker = new PdeKeywords();
    keywordToReference = new HashMap<String, String>();

    String line = null;
    while ((line = reader.readLine()) != null) {
      String[] pieces = PApplet.trim(PApplet.split(line, '\t'));
      if (pieces.length >= 2) {
        String keyword = pieces[0];
        String coloring = pieces[1];

        if (coloring.length() > 0) {
          tokenMarker.addColoring(keyword, coloring);
        }
        if (pieces.length == 3) {
          String htmlFilename = pieces[2];
          if (htmlFilename.length() > 0) {
            keywordToReference.put(keyword, htmlFilename);
          }
        }
      }
    }
  }

  
  public String getTitle() {
    return "Standard";
  }


//  public EditorToolbar createToolbar(Editor editor) {
//    return new Toolbar(editor);
//  }

  
//  public Formatter createFormatter() {
//    return new AutoFormat();
//  }
  
  
//  public Editor createEditor(Base ibase, String path, int[] location) {
//  }
  

  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  public String getDefaultExtension() {
    return "pde";
  }
 
  
  public String[] getExtensions() {
    return new String[] { "pde", "java" };
  }

  
  public String[] getIgnorable() {
    return new String[] { 
      "applet",
      "application.macosx",
      "application.windows",
      "application.linux"
    };
  }

  
  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

  
  /**
   * Implements Sketch &rarr; Run.
   * @throws SketchException 
   */
  public void handleRun(Sketch sketch, RunnerListener listener) throws SketchException {
    JavaBuild build = new JavaBuild(sketch);
    String appletClassName = build.build();
    if (appletClassName != null) {
      runtime = new Runner(build, listener);
      runtime.launch(false);
    }
  }


  public void handlePresent(Sketch sketch, RunnerListener listener) throws SketchException {
    JavaBuild build = new JavaBuild(sketch);
    String appletClassName = build.build();
    if (appletClassName != null) {
      runtime = new Runner(build, listener);
      runtime.launch(true);
    }
  }


  public void handleStop() {
    if (runtime != null) {
      runtime.close();  // kills the window
      runtime = null; // will this help?
    }
  }
  
  
  public boolean handleExportApplet(Sketch sketch) throws SketchException, IOException {
    JavaBuild build = new JavaBuild(sketch);
    return build.exportApplet();
  }
  
  
  public boolean handleExportApplication(Sketch sketch) throws SketchException, IOException {
    JavaBuild build = new JavaBuild(sketch);
    return build.exportApplication();
  }
}