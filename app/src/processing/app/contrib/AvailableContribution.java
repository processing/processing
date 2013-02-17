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
import java.util.HashMap;

import processing.app.Base;
import processing.app.Editor;
import processing.core.PApplet;


/**
 * A class to hold information about a Contribution that can be downloaded. 
 */
class AvailableContribution extends Contribution {
  protected final ContributionType type;   // Library, tool, etc.
  protected final String link;             // Direct link to download the file

  
  public AvailableContribution(ContributionType type, HashMap<String, String> params) {
    this.type = type;
    this.link = params.get("download");
    
    category = ContributionListing.getCategory(params.get("category"));
    name = params.get("name");
    authorList = params.get("authorList");
    url = params.get("url");
    sentence = params.get("sentence");
    paragraph = params.get("paragraph");
    version = PApplet.parseInt(params.get("version"), 0);
    prettyVersion = params.get("prettyVersion");
  }
  
  
  /**
   * @param contribArchive
   *          a zip file containing the library to install
   * @param confirmReplace
   *          true to open a dialog asking the user to confirm removing/moving
   *          the library when a library by the same name already exists
   * @return
   */
  public LocalContribution install(Editor editor, File contribArchive,
                                   boolean confirmReplace, StatusPanel status) {
    // Unzip the file into the modes, tools, or libraries folder inside the 
    // sketchbook. Unzipping to /tmp is problematic because it may be on 
    // another file system, so move/rename operations will break.
    File sketchbookContribFolder = type.getSketchbookFolder();
    File tempFolder = null; 
    
    try {
      tempFolder = 
        Base.createTempFolder(type.toString(), "tmp", sketchbookContribFolder);
    } catch (IOException e) {
      status.setErrorMessage("Could not create a temporary folder to install.");
      return null;
    }
    Base.unzip(contribArchive, tempFolder);
//    System.out.println("temp folder is " + tempFolder);
    Base.openFolder(tempFolder);

    // Now go looking for a legit contrib inside what's been unpacked.
    File contribFolder = null;
    
    // Sometimes contrib authors place all their folders in the base directory 
    // of the .zip file instead of in single folder as the guidelines suggest. 
    if (type.isCandidate(tempFolder)) {
      /*
      // Can't just rename the temp folder, because a contrib with this name
      // may already exist. Instead, create a new temp folder, and rename the 
      // old one to be the correct folder.
      File enclosingFolder = null;  
      try {
        enclosingFolder = Base.createTempFolder(type.toString(), "tmp", sketchbookContribFolder);
      } catch (IOException e) {
        status.setErrorMessage("Could not create a secondary folder to install.");
        return null;
      }
      contribFolder = new File(enclosingFolder, getName());
      tempFolder.renameTo(contribFolder);
      tempFolder = enclosingFolder;
      */
      status.setErrorMessage("This " + type + " needs to be repackaged according to the guidelines.");
      return null;

    }

    if (contribFolder == null) {
      // Find the first legitimate looking folder in what we just unzipped
      contribFolder = type.findCandidate(tempFolder);
    }
    
    LocalContribution installedContrib = null;

    if (contribFolder == null) {
      status.setErrorMessage("Could not find a " + type + " in the downloaded file.");
      
    } else {
      File propFile = new File(contribFolder, type + ".properties");
      if (writePropertiesFile(propFile)) {        
        // 1. contribFolder now has a legit contribution, load it to get info. 
        LocalContribution newContrib =
          type.load(editor.getBase(), contribFolder);
        
        // 2. Check to make sure nothing has the same name already, 
        // backup old if needed, then move things into place and reload.
        installedContrib = 
          newContrib.moveAndLoad(editor, confirmReplace, status);
        
      } else {
        status.setErrorMessage("Error overwriting .properties file.");
      }
    }

    // Remove any remaining boogers
    if (tempFolder.exists()) {
      Base.removeDir(tempFolder);
    }
    return installedContrib;
  }
  
  
  public boolean isInstalled() {
    return false;
  }

  
  public ContributionType getType() {
    return type;
  }


  /**
   * We overwrite the properties file with the curated version from the 
   * Processing site. This ensures that things have been cleaned up (for 
   * instance, that the "sentence" is really a sentence) and that bad data 
   * from the contrib's .properties file doesn't break the manager. 
   * @param propFile
   * @return
   */
  public boolean writePropertiesFile(File propFile) {
    try {
      if (propFile.delete() && propFile.createNewFile() && propFile.setWritable(true)) {
        PrintWriter writer = PApplet.createWriter(propFile);

        writer.println("name=" + getName());
        writer.println("category=" + getCategory());
        writer.println("authorList=" + getAuthorList());
        writer.println("url=" + getUrl());
        writer.println("sentence=" + getSentence());
        writer.println("paragraph=" + getParagraph());
        writer.println("version=" + getVersion());
        writer.println("prettyVersion=" + getPrettyVersion());

        writer.flush();
        writer.close();
      }
      return true;

    } catch (FileNotFoundException e) {
      e.printStackTrace();

    } catch (IOException e) {
      e.printStackTrace();
    }
    return false;
  }
}