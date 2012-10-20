/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2012 The Processing Foundation

  This program is free software; you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation, version 2.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software Foundation,
  Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package processing.app.tools;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import processing.app.Base;
import processing.app.Editor;
import processing.core.PApplet;


public class InstallCommander implements Tool {
  Editor editor;


  public String getMenuTitle() {
    return "Install Command Line Tool";
  }


  public void init(Editor editor) {
    this.editor = editor;
  }


  public void run() {
    try {
      File file = File.createTempFile("processing", "commander");
      PrintWriter writer = PApplet.createWriter(file);
      writer.println("#!/bin/sh");
      writer.println("echo 'bag of awesome'");
      writer.flush();
      writer.close();
      file.setExecutable(true);

      System.out.println(System.getProperty("javaroot"));
      if (true) return;

      String sourcePath = file.getAbsolutePath();
      String targetPath = "/usr/bin/processing-java";
      String shellScript = "/bin/mv " + sourcePath + " " + targetPath;
      String appleScript =
        "do shell script \"" + shellScript + "\" with administrator privileges";
      PApplet.exec(new String[] { "osascript", "-e", appleScript });
    } catch (IOException e) {
      Base.showWarning("Error while installing",
                       "An error occurred and the tools were not installed.", e);
    }
  }
}