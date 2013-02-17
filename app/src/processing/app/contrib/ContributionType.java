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

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;

import processing.app.Base;
import processing.app.Editor;
import processing.app.Library;

public enum ContributionType {
//    LIBRARY, LIBRARY_COMPILATION, TOOL, MODE;
  LIBRARY, TOOL, MODE;

    
  public String toString() {
    switch (this) {
    case LIBRARY:
      return "library";
//      case LIBRARY_COMPILATION:
//        return "compilation";
    case TOOL:
      return "tool";
    case MODE:
      return "mode";
    }
    return null;  // should be unreachable
  };
  
  
  /** Return Mode for mode, Tool for tool, etc. */
  public String getTitle() {
    String s = toString();
    return Character.toUpperCase(s.charAt(0)) + s.substring(1);
  }
    
    
  public String getFolderName() {
    switch (this) {
    case LIBRARY:
      return "libraries";
//      case LIBRARY_COMPILATION:
//        return "libraries";
    case TOOL:
      return "tools";
    case MODE:
      return "modes";
    }
    return null;  // should be unreachable
  }
    
    
//    public String getPropertiesName() {
//      return toString() + ".properties";
//    }

    
  static public ContributionType fromName(String s) {
    if (s != null) {
      if ("library".equals(s.toLowerCase())) {
        return LIBRARY;
      }
//        if ("compilation".equals(s.toLowerCase())) {
//          return LIBRARY_COMPILATION;
//        }
      if ("tool".equals(s.toLowerCase())) {
        return TOOL;
      }
      if ("mode".equals(s.toLowerCase())) {
        return MODE;
      }
    }
    return null;
  }


  public File getSketchbookFolder() {
    switch (this) {
    case LIBRARY:
      return Base.getSketchbookLibrariesFolder();
    case TOOL:
      return Base.getSketchbookToolsFolder();
    case MODE:
      return Base.getSketchbookModesFolder();
    }
    return null;
  }


  boolean isCandidate(File potential) {
    return (potential.isDirectory() && new File(potential, toString()).exists());
  }


  /**
   * Return a list of directories that have the necessary subfolder for this
   * contribution type. For instance, a list of folders that have a 'mode'
   * subfolder if this is a ModeContribution.
   */
  File[] listCandidates(File folder) {
    return folder.listFiles(new FileFilter() {
      public boolean accept(File potential) {
        return isCandidate(potential);
      }
    });
  }


  /**
   * Return the first directory that has the necessary subfolder for this
   * contribution type. For instance, the first folder that has a 'mode'
   * subfolder if this is a ModeContribution.
   */
  File findCandidate(File folder) {
    File[] folders = listCandidates(folder);

    if (folders.length == 0) {
      return null;

    } else if (folders.length > 1) {
      Base.log("More than one " + toString() + " found inside " + folder.getAbsolutePath());
    }
    return folders[0];
  }
  
  
  LocalContribution load(Base base, File folder) {
    switch (this) {
    case LIBRARY:
      return new Library(folder);
    case TOOL:
      return ToolContribution.load(folder);
    case MODE:
      return ModeContribution.load(base, folder);
    }
    return null;
  }


  ArrayList<LocalContribution> listContributions(Editor editor) {
    ArrayList<LocalContribution> contribs = new ArrayList<LocalContribution>();
    switch (this) {
    case LIBRARY:
      contribs.addAll(editor.getMode().contribLibraries);
      break;
    case TOOL:
      contribs.addAll(editor.contribTools);
      break;
    case MODE:
      contribs.addAll(editor.getBase().getModeContribs());
      break;
    }
    return contribs;
  }
  
  
  File createBackupFolder(StatusPanel status) {
    File backupFolder = new File(getSketchbookFolder(), "old");
    if (!backupFolder.isDirectory()) {
      status.setErrorMessage("Remove the file named \"old\" from the " + 
                             getFolderName() + " folder in the sketchbook.");
      return null;
    }
    if (!backupFolder.exists() && !backupFolder.mkdirs()) {
      status.setErrorMessage("Could not create a " + toString() + " backup folder.");
      return null;
    }
    return backupFolder;
  }
}