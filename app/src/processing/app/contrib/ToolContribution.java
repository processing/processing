/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2013-15 The Processing Foundation
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
import java.util.*;

import processing.app.Base;
import processing.app.Messages;
import processing.app.tools.Tool;


public class ToolContribution extends LocalContribution implements Tool, Comparable<ToolContribution> {
  private Tool tool;

  private File referenceFile; // shortname/reference/index.html

  static public ToolContribution load(File folder) {
    try {
      return new ToolContribution(folder);
    } catch (IgnorableException ig) {
      Messages.log(ig.getMessage());

    } catch (VerifyError ve) {  // incompatible
      // avoid the excessive error spew that happens here

    } catch (NoClassDefFoundError ncdfe) {
      if (ncdfe.getMessage().contains("processing/app/Editor")) {
        System.err.println("The Editor class has moved to the processing.app.ui package in Processing 3");
      }

    } catch (Throwable e) {  // unknown error
      e.printStackTrace();
    }
    return null;
  }


  private ToolContribution(File folder) throws Throwable {
    super(folder);

    String className = initLoader(null);
    if (className != null) {
      Class<?> toolClass = loader.loadClass(className);
      tool = (Tool) toolClass.newInstance();
    }

    referenceFile = new File(folder, "reference/index.html");
  }


  /**
   * Method to close the ClassLoader so that the archives are no longer
   * "locked" and a tool can be removed without restart.
   */
  public void clearClassLoader() {
    try {
      ((URLClassLoader) this.loader).close();
    } catch (IOException e1) {
      e1.printStackTrace();
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


  static public List<ToolContribution> loadAll(File toolsFolder) {
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
    Collections.sort(outgoing);
    return outgoing;
  }


  public void init(Base base) {
    tool.init(base);
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


  /**
   * Returns the object stored in the referenceFile field, which contains an
   * instance of the file object representing the index file of the reference
   *
   * @return referenceFile
   */
  public File getReferenceIndexFile() {
    return referenceFile;
  }


  /**
   * Tests whether the reference's index file indicated by referenceFile exists.
   *
   * @return true if and only if the file denoted by referenceFile exists; false
   *         otherwise.
   */
  public boolean hasReference() {
    return referenceFile.exists();
  }


  @Override
  public int compareTo(ToolContribution o) {
    return getMenuTitle().compareTo(o.getMenuTitle());
  }
}
