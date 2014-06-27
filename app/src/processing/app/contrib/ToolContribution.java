/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2013 The Processing Foundation
  Copyright (c) 2011-12 Ben Fry and Casey Reas

  This program is free software; you can redistribute it and/or modify
  it under the terms of the GNU General Public License version 2
  as published by the Free Software Foundation.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License along
  with this program; if not, write to the Free Software Foundation, Inc.
  59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/
package processing.app.contrib;

import java.io.*;
import java.net.URLClassLoader;
//import java.net.*;
import java.util.*;

import processing.app.Base;
//import processing.app.Base;
import processing.app.Editor;
import processing.app.tools.Tool;


public class ToolContribution extends LocalContribution implements Tool {
  private Tool tool;


  static public ToolContribution load(File folder) {
    try {
      return new ToolContribution(folder);
    } catch (IgnorableException ig) {
      Base.log(ig.getMessage());
    } catch (Error err) {
      // Handles UnsupportedClassVersionError and others
      err.printStackTrace();
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    return null;
  }


  private ToolContribution(File folder) throws Exception {
    super(folder);

    String className = initLoader(null);
    if (className != null) {
      Class<?> toolClass = loader.loadClass(className);
      tool = (Tool) toolClass.newInstance();
    }
  }


  /**
   * Method to close the ClassLoader so that the archives are no longer "locked" and
   * a tool can be removed without restart.
   */
  public void clearClassLoader(Base base) {
//    ArrayList<ToolContribution> contribTools = base.getActiveEditor().contribTools;
//    int botherToRemove = contribTools.indexOf(this);
//    if (botherToRemove != -1) { // The poor thing doesn't even exist, and we're trying to remove it...
//      contribTools.remove(botherToRemove);
    ArrayList<ToolContribution> contribTools = base.getActiveEditor().contribTools;
    for (ToolContribution toolContrib : contribTools)
      if (toolContrib.getName().equals(this.name)) {
        try {
          System.out.println("Here  " + name);
          ((URLClassLoader) this.loader).close();
          ((URLClassLoader) toolContrib.loader).close();
          // The typecast should be safe, since the only case when loader is not of
          // type URLClassLoader is when no archives were found in the first
          // place...
        } catch (IOException e) {
          e.printStackTrace();
        }
//        base.getActiveEditor().rebuildToolMenu();
      }
//    }
  }


//  static protected List<File> discover(File folder) {
//    File[] folders = listCandidates(folder, "tool");
//    if (folders == null) {
//      return new ArrayList<File>();
//    } else {
//      return Arrays.asList(folders);
//    }
//  }


  static public ArrayList<ToolContribution> loadAll(File toolsFolder) {
    File[] list = ContributionType.TOOL.listCandidates(toolsFolder);
    ArrayList<ToolContribution> outgoing = new ArrayList<ToolContribution>();
    // If toolsFolder does not exist or is inaccessible (stranger things have
    // happened, and are reported as bugs) list will come back null.
    if (list != null) {
      for (File folder : list) {
        try {
          ToolContribution tc = load(folder);
          if (tc != null) {
            outgoing.add(tc);
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
    return outgoing;
  }


//  Editor editor;  // used to send error messages
  
  public void init(Editor editor) {
//    try {
//      this.editor = editor;
    tool.init(editor);
//    } catch (NoSuchMethodError nsme) {
//      editor.statusError(tool.getMenuTitle() + " is not compatible with this version of Processing");
//      nsme.printStackTrace();
//    }
  }


  public void run() {
//    try {
    tool.run();
//    } catch (NoSuchMethodError nsme) {
//      editor.statusError(tool.getMenuTitle() + " is not compatible with this version of Processing");
//      nsme.printStackTrace();
//    }
  }


  public String getMenuTitle() {
    return tool.getMenuTitle();
  }


  public ContributionType getType() {
    return ContributionType.TOOL;
  }
}