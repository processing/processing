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

import javax.swing.JOptionPane;

import processing.app.Base;
import processing.app.Editor;
import processing.app.Language;
import processing.app.Messages;
import processing.core.PApplet;


public class InstallCommander implements Tool {
  Editor editor;


  public String getMenuTitle() {
    return Language.text("menu.tools.install_processing_java");
  }


  public void init(Editor editor) {
    this.editor = editor;
  }


  public void run() {
    try {
      final String primary =
        "Install processing-java for all users?";
      final String secondary =
        "This will install the processing-java program, which is capable " +
        "of building and running Java Mode sketches from the command line. " +
        "Click “Yes” to install it for all users (an administrator password " +
        "is required), or “No” to place the program in your home directory. " +
        "If you rename or move Processing.app, " +
        "you'll need to reinstall the tool.";

      int result =
        JOptionPane.showConfirmDialog(editor,
                                      "<html> " +
                                      "<head> <style type=\"text/css\">"+
                                      "b { font: 13pt \"Lucida Grande\" }"+
                                      "p { font: 11pt \"Lucida Grande\"; margin-top: 8px; width: 300px }"+
                                      "</style> </head>" +
                                      "<b>" + primary + "</b>" +
                                      "<p>" + secondary + "</p>",
                                      "Commander",
                                      JOptionPane.YES_NO_CANCEL_OPTION,
                                      JOptionPane.QUESTION_MESSAGE);

      if (result == JOptionPane.CANCEL_OPTION) {
        return;
      }

      File file = File.createTempFile("processing", "commander");
      PrintWriter writer = PApplet.createWriter(file);
      writer.println("#!/bin/sh");

      String[] jarList = new String[] {
        "pde.jar",
        "antlr.jar",
        "jna.jar",
        "ant.jar",
        "ant-launcher.jar",

        // extra libraries for new JDI setup
        "org-netbeans-swing-outline.jar",
        "com.ibm.icu_4.4.2.v20110823.jar",
        "jdi.jar",
        "jdimodel.jar",
        "org.eclipse.osgi_3.8.1.v20120830-144521.jar",

        "core/library/core.jar"        
      };
      String classPath = PApplet.join(jarList, ":");

      //String javaRoot = System.getProperty("javaroot");
      String javaRoot = Base.getContentFile(".").getCanonicalPath();
      writer.println("cd \"" + javaRoot + "\" && " +
                     Base.getJavaPath() + 
                     " -Djna.nosys=true" +
      		           " -cp \"" + classPath + "\"" +
      		           " processing.mode.java.Commander \"$@\"");
      writer.flush();
      writer.close();
      file.setExecutable(true);
      String sourcePath = file.getAbsolutePath();

      if (result == JOptionPane.YES_OPTION) {
        String targetPath = "/usr/bin/processing-java";
        String shellScript = "/bin/mv " + sourcePath + " " + targetPath;
        String appleScript =
          "do shell script \"" + shellScript + "\" with administrator privileges";
        PApplet.exec(new String[] { "osascript", "-e", appleScript });

      } else if (result == JOptionPane.NO_OPTION) {
        File targetFile = new File(System.getProperty("user.home"), "processing-java");
        String targetPath = targetFile.getAbsolutePath();
        if (targetFile.exists()) {
          Messages.showWarning("File Already Exists",
                           "The processing-java program already exists at:\n" +
                           targetPath + "\n" +
                           "Please remove it and try again.", null);
        } else {
          PApplet.exec(new String[] { "mv", sourcePath, targetPath });
        }
      }
      editor.statusNotice("Finished.");

    } catch (IOException e) {
      Messages.showWarning("Error while installing",
                       "An error occurred and the tools were not installed.", e);
    }
  }
}
