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

import javax.swing.JOptionPane;

import processing.app.Base;
import processing.app.Editor;
import processing.core.PApplet;


public class SerialFixer implements Tool {
  Editor editor;


  public String getMenuTitle() {
    return "Fix the Serial Library";
  }


  public void init(Editor editor) {
    this.editor = editor;
  }


  public void run() {
    final String primary =
      "Attempt to fix common serial port problems?";
    final String secondary =
      "Click “OK” to perform additional installation steps to enable " +
      "the Serial library. An administrator password will be required.";

      int result =
        JOptionPane.showConfirmDialog(editor,
                                      "<html> " +
                                      "<head> <style type=\"text/css\">"+
                                      "b { font: 13pt \"Lucida Grande\" }"+
                                      "p { font: 11pt \"Lucida Grande\"; margin-top: 8px }"+
                                      "</style> </head>" +
                                      "<b>" + primary + "</b>" +
                                      "<p>" + secondary + "</p>",
                                      "Commander",
                                      JOptionPane.OK_CANCEL_OPTION,
                                      JOptionPane.QUESTION_MESSAGE);

      if (result == JOptionPane.OK_OPTION) {
        String shellScript = "mkdir -p /var/lock && chmod 777 /var/lock";
        String appleScript =
          "do shell script \"" + shellScript + "\" with administrator privileges";
        PApplet.exec(new String[] { "osascript", "-e", appleScript });
      }
      editor.statusNotice("Finished.");
  }


  static public boolean isNeeded() {
    if (Base.isMacOS()) {
      File lockFolder = new File("/var/lock");
      if (!lockFolder.exists() ||
          !lockFolder.canRead() ||
          !lockFolder.canWrite() ||
          !lockFolder.canExecute()) {
        return true;
      }
    }
    return false;
  }
}