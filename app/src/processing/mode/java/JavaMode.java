/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2010-11 Ben Fry and Casey Reas

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

import java.io.File;
import java.io.IOException;

import processing.app.*;
import processing.mode.java.runner.Runner;


public class JavaMode extends Mode {
  // classpath for all known libraries for p5
  // (both those in the p5/libs folder and those with lib subfolders
  // found in the sketchbook)
//  static public String librariesClassPath;


  public Editor createEditor(Base base, String path, EditorState state) {
    return new JavaEditor(base, path, state, this);
  }


  public JavaMode(Base base, File folder) {
    super(base, folder);
  }


  public String getTitle() {
    return "Java";
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


  public File[] getExampleCategoryFolders() {
    return new File[] {
      new File(examplesFolder, "Basics"),
      new File(examplesFolder, "Topics"),
      new File(examplesFolder, "Demos"),
      new File(examplesFolder, "Books")
    };
  }


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


  public Library getCoreLibrary() {
    if (coreLibrary == null) {
      File coreFolder = Base.getContentFile("core");
      coreLibrary = new Library(coreFolder, null);
    }
    return coreLibrary;
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  public Runner handleRun(Sketch sketch, RunnerListener listener) throws SketchException {
    JavaBuild build = new JavaBuild(sketch);
    String appletClassName = build.build(false);
    if (appletClassName != null) {
      final Runner runtime = new Runner(build, listener);
      new Thread(new Runnable() {
        public void run() {
          runtime.launch(false);  // this blocks until finished
        }
      }).start();
      return runtime;
    }
    return null;
  }


  public Runner handlePresent(Sketch sketch, RunnerListener listener) throws SketchException {
    JavaBuild build = new JavaBuild(sketch);
    String appletClassName = build.build(false);
    if (appletClassName != null) {
      final Runner runtime = new Runner(build, listener);
      new Thread(new Runnable() {
        public void run() {
          runtime.launch(true);
        }
      }).start();
      return runtime;
    }
    return null;
  }


//  public void handleStop() {
//    if (runtime != null) {
//      runtime.close();  // kills the window
//      runtime = null; // will this help?
//    }
//  }


//  public boolean handleExportApplet(Sketch sketch) throws SketchException, IOException {
//    JavaBuild build = new JavaBuild(sketch);
//    return build.exportApplet();
//  }


  public boolean handleExportApplication(Sketch sketch) throws SketchException, IOException {
    JavaBuild build = new JavaBuild(sketch);
    return build.exportApplication();
  }
}