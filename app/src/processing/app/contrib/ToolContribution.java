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
    return outgoing;
  }


  public void init(Editor editor) {
    tool.init(editor);
  }


  public void run() {
    tool.run();
  }


  public String getMenuTitle() {
    return tool.getMenuTitle();
  }


  public ContributionType getType() {
    return ContributionType.TOOL;
  }
}