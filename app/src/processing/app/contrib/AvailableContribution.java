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
import java.util.List;

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
    
    //category = ContributionListing.getCategory(params.get("category"));
    categories = parseCategories(params.get("category"));
    name = params.get("name");
    authorList = params.get("authorList");
    url = params.get("url");
    sentence = params.get("sentence");
    paragraph = params.get("paragraph");
    String versionStr = params.get("version");
    if (versionStr != null) {
      version = PApplet.parseInt(versionStr, 0);
    }
    prettyVersion = params.get("prettyVersion");
    String lastUpdatedStr = params.get("lastUpdated");
    if (lastUpdatedStr != null)
      try {
        lastUpdated =  Long.parseLong(lastUpdatedStr);
      } catch (NumberFormatException e) {
        lastUpdated = 0;
      }
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
//    File sketchbookContribFolder = type.getSketchbookFolder();
    File tempFolder = null; 
    
    try {
      tempFolder = type.createTempFolder();
    } catch (IOException e) {
      status.setErrorMessage("Could not create a temporary folder to install.");
      return null;
    }
    Base.unzip(contribArchive, tempFolder);
//    System.out.println("temp folder is " + tempFolder);
//    Base.openFolder(tempFolder);

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
      status.setErrorMessage(getName() + " needs to be repackaged according to the " + type.getTitle() + " guidelines.");
      //status.setErrorMessage("This " + type + " needs to be repackaged according to the guidelines.");
      return null;
    }

//    if (contribFolder == null) {
    // Find the first legitimate looking folder in what we just unzipped
    contribFolder = type.findCandidate(tempFolder);
//    }
    LocalContribution installedContrib = null;

    if (contribFolder == null) {
      status.setErrorMessage("Could not find a " + type + " in the downloaded file.");
      
    } else {
      File propFile = new File(contribFolder, type + ".properties");
      if (writePropertiesFile(propFile)) {        
        // 1. contribFolder now has a legit contribution, load it to get info. 
        LocalContribution newContrib =
          type.load(editor.getBase(), contribFolder);
        
        // 1.1. get info we need to delete the newContrib folder later
        File newContribFolder = newContrib.getFolder();
        
        // 2. Check to make sure nothing has the same name already, 
        // backup old if needed, then move things into place and reload.
        installedContrib = 
          newContrib.copyAndLoad(editor, confirmReplace, status);
        
        // Restart no longer needed. Yay!
//        if (newContrib != null && type.requiresRestart()) {
//          installedContrib.setRestartFlag();
//          //status.setMessage("Restart Processing to finish the installation.");
//        }
        
        // 3.1 Unlock all the jars if it is a mode or tool
        if (newContrib.getType() == ContributionType.MODE) {
          ((ModeContribution)newContrib).clearClassLoader(editor.getBase());
          System.out.println("ismode");
        }
        else if (newContrib.getType() == ContributionType.TOOL) {
          ((ToolContribution)newContrib).clearClassLoader(editor.getBase());
          System.out.println("istool");
        }
        
        // 3.2 Delete the newContrib, do a garbage collection, hope and pray
        // that Java will unlock the temp folder on Windows now
        newContrib = null;
        System.gc();
        
        
        if (Base.isWindows()) {
          // we'll even give it a second to finish up ... because file ops are
          // just that flaky on Windows.
          try {
            Thread.sleep(1000);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }

        // 4. Okay, now actually delete that temp folder
        Base.removeDir(newContribFolder);
        
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
  
  
  /**
   * @param contribArchive
   *          a zip file containing the library to install
   * @return
   */
  public LocalContribution installOnStartup(Base base, File contribArchive, StatusPanel status) {
    // Unzip the file into the modes, tools, or libraries folder inside the 
    // sketchbook. Unzipping to /tmp is problematic because it may be on 
    // another file system, so move/rename operations will break.

    File tempFolder = null; 
    
    try {
      tempFolder = type.createTempFolder();
    } catch (IOException e) {
      status.setErrorMessage("Could not create a temporary folder to install.");
      return null;
    }
    Base.unzip(contribArchive, tempFolder);

    // Now go looking for a legit contrib inside what's been unpacked.
    File contribFolder = null;
    
    // Sometimes contrib authors place all their folders in the base directory 
    // of the .zip file instead of in single folder as the guidelines suggest. 
    if (type.isCandidate(tempFolder)) {
      status.setErrorMessage(getName() + " needs to be repackaged according to the " + type.getTitle() + " guidelines.");
      return null;
    }

    contribFolder = type.findCandidate(tempFolder);
    LocalContribution installedContrib = null;

    if (contribFolder == null) {
      status.setErrorMessage("Could not find a " + type + " in the downloaded file.");
      
    } else {
      File propFile = new File(contribFolder, type + ".properties");
      if (writePropertiesFile(propFile)) {        
        // 1. contribFolder now has a legit contribution, load it to get info. 
        LocalContribution newContrib =
          type.load(base, contribFolder);
        
        // 1.1. get info we need to delete the newContrib folder later
        File newContribFolder = newContrib.getFolder();
        
        // 2. Check to make sure nothing has the same name already, 
        // backup old if needed, then move things into place and reload.
        installedContrib = 
          newContrib.copyAndLoadOnStartup(base, status);
        
        // 3. Delete the newContrib, do a garbage collection, hope and pray
        // that Java will unlock the temp folder on Windows now
        newContrib = null;
        System.gc();
        
        
        if (Base.isWindows()) {
          // we'll even give it 2 seconds to finish up ... because file ops are
          // just that flaky on Windows.
          try {
            Thread.sleep(2000);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }

        // 4. Okay, now actually delete that temp folder
        Base.removeDir(newContribFolder);
        
      } else {
        status.setErrorMessage("Error overwriting .properties file.");
      }
    }

    // Remove any remaining ickies
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
   * We overwrite those fields that aren't proper in the properties file with
   * the curated version from the Processing site. This ensures that things have
   * been cleaned up (for instance, that the "sentence" is really a sentence)
   * and that bad data from the contrib's .properties file doesn't break the
   * manager. However, it also ensures that valid fields in the properties file
   * aren't overwritten, since the properties file may be more recent than the
   * contributions.txt file.
   * 
   * @param propFile
   * @return
   */
  public boolean writePropertiesFile(File propFile) {
    try {

      HashMap<String, String> properties = Base.readSettings(propFile);

      String name = properties.get("name");
      if (name == null || name.isEmpty())
        name = getName();

      String category;
      List<String> categoryList = parseCategories(properties.get("category"));
      if (categoryList.size() == 1 && categoryList.get(0).equals("Unknown"))
        category = getCategoryStr();
      else {
        StringBuilder sb = new StringBuilder();
        for (String cat : categories) {
          sb.append(cat);
          sb.append(',');
        }
        sb.deleteCharAt(sb.length() - 1);
        category = sb.toString();
      }

      String authorList = properties.get("authorList");
      if (authorList == null || authorList.isEmpty())
        authorList = getAuthorList();

      String url = properties.get("url");
      if (url == null || url.isEmpty())
        url = getUrl();

      String sentence = properties.get("sentence");
      if (sentence == null || sentence.isEmpty())
        sentence = getSentence();

      String paragraph = properties.get("paragraph");
      if (paragraph == null || paragraph.isEmpty())
        paragraph = getParagraph();

      int version;
      try {
        version = Integer.parseInt(properties.get("version"));
      } catch (NumberFormatException e) {
        version = getVersion();
        System.err.println("The version number for the “" + name
          + "” contribution is not set properly.");
        System.err
          .println("Please contact the author to fix it according to the guidelines.");
      }

      String prettyVersion = properties.get("prettyVersion");
      if (prettyVersion == null || prettyVersion.isEmpty())
        prettyVersion = getPrettyVersion();

      long lastUpdated;
      try {
        lastUpdated = Long.parseLong(properties.get("lastUpdated"));
      }
      catch (NumberFormatException nfe) {
        lastUpdated = getLastUpdated();
      // Better comment these out till all contribs have a lastUpdated 
//        System.err.println("The last updated date for the “" + name
//                           + "” contribution is not set properly.");
//        System.err
//          .println("Please contact the author to fix it according to the guidelines.");
      }

      if (propFile.delete() && propFile.createNewFile() && propFile.setWritable(true)) {
        PrintWriter writer = PApplet.createWriter(propFile);

        writer.println("name=" + name);
        writer.println("category=" + category);
        writer.println("authorList=" + authorList);
        writer.println("url=" + url);
        writer.println("sentence=" + sentence);
        writer.println("paragraph=" + paragraph);
        writer.println("version=" + version);
        writer.println("prettyVersion=" + prettyVersion);
        writer.println("lastUpdated=" + lastUpdated);

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