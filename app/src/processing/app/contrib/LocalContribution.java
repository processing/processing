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
import java.net.URL;
import java.net.URLClassLoader;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.*;

import javax.swing.JOptionPane;

import processing.app.*;


/** 
 * A contribution that has been downloaded to the disk, and may or may not
 * be installed.
 */
public abstract class LocalContribution extends Contribution {
  static public final String DELETION_FLAG = "flagged_for_deletion";
  
  protected String id;          // 1
  protected int latestVersion;  // 103
  protected File folder;
  protected HashMap<String, String> properties;
  protected ClassLoader loader;


  public LocalContribution(File folder) {
    this.folder = folder;

    // required for contributed modes, but not for built-in core modes
    File propertiesFile = new File(folder, getTypeName() + ".properties");
    if (propertiesFile.exists()) {
      properties = Base.readSettings(propertiesFile);

      name = properties.get("name");
      id = properties.get("id");
      category = ContributionListing.getCategory(properties.get("category"));
      if (name == null) {
        name = folder.getName();
      }
      authorList = properties.get("authorList");
      url = properties.get("url");
      sentence = properties.get("sentence");
      paragraph = properties.get("paragraph");

      try {
        version = Integer.parseInt(properties.get("version"));
      } catch (NumberFormatException e) {
        e.printStackTrace();
      }
      prettyVersion = properties.get("prettyVersion");
      
    } else {
      Base.log("No properties file at " + propertiesFile.getAbsolutePath());
      // We'll need this to be set at a minimum.
      name = folder.getName();
    }
  }


  public String initLoader(String className) throws Exception {
    File modeDirectory = new File(folder, getTypeName());
    if (modeDirectory.exists()) {
      Base.log("checking mode folder regarding " + className);
      // If no class name specified, search the main <modename>.jar for the
      // full name package and mode name.
      if (className == null) {
        String shortName = folder.getName();
        File mainJar = new File(modeDirectory, shortName + ".jar");
        if (mainJar.exists()) {
          className = findClassInZipFile(shortName, mainJar);
        } else {
          throw new IgnorableException(mainJar.getAbsolutePath() + " does not exist.");
        }

        if (className == null) {
          throw new IgnorableException("Could not find " + shortName +
                                       " class inside " + mainJar.getAbsolutePath());
        }
      }

      // Add .jar and .zip files from the "mode" folder into the classpath
      File[] archives = Base.listJarFiles(modeDirectory);
      if (archives != null && archives.length > 0) {
        URL[] urlList = new URL[archives.length];
        for (int j = 0; j < urlList.length; j++) {
          Base.log("Found archive " + archives[j] + " for " + getName());
          urlList[j] = archives[j].toURI().toURL();
        }
//        loader = new URLClassLoader(urlList, Thread.currentThread().getContextClassLoader());
        loader = new URLClassLoader(urlList);
        Base.log("loading above JARs with loader " + loader);
//        System.out.println("listing classes for loader " + loader);
//        listClasses(loader);
      }
    }

    // If no archives were found, just use the regular ClassLoader
    if (loader == null) {
      loader = Thread.currentThread().getContextClassLoader();
    }
    return className;
  }


  /*
  // doesn't work with URLClassLoader, but works with the system CL
  static void listClasses(ClassLoader loader) {
//    loader = Thread.currentThread().getContextClassLoader();
    try {
      Field f = ClassLoader.class.getDeclaredField("classes");
      f.setAccessible(true);
      Vector<Class> classes =  (Vector<Class>) f.get(loader);
      for (Class c : classes) {
        System.out.println(c.getName());
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  */


//  static protected boolean isCandidate(File potential, final ContributionType type) {
//    return (potential.isDirectory() &&
//      new File(potential, type.getFolderName()).exists());
//  }
//  
//  
//  /**
//   * Return a list of directories that have the necessary subfolder for this
//   * contribution type. For instance, a list of folders that have a 'mode'
//   * subfolder if this is a ModeContribution.
//   */
//  static protected File[] listCandidates(File folder, final ContributionType type) {
//    return folder.listFiles(new FileFilter() {
//      public boolean accept(File potential) {
//        return isCandidate(potential, type);
//      }
//    });
//  }
//
//
//  /**
//   * Return the first directory that has the necessary subfolder for this
//   * contribution type. For instance, the first folder that has a 'mode'
//   * subfolder if this is a ModeContribution.
//   */
//  static protected File findCandidate(File folder, final ContributionType type) {
//    File[] folders = listCandidates(folder, type);
//    
//    if (folders.length == 0) {
//      return null;
//    
//    } else if (folders.length > 1) {
//      Base.log("More than one " + type.toString() + " found inside " + folder.getAbsolutePath());
//    }
//    return folders[0];
//  }
  
  
  LocalContribution moveAndLoad(Editor editor, 
                                    boolean confirmReplace, 
                                    StatusPanel status) {
    ArrayList<LocalContribution> oldContribs = 
      getType().listContributions(editor);
    
    String contribFolderName = getFolder().getName();

    File contribTypeFolder = getType().getSketchbookFolder();
    File contribFolder = new File(contribTypeFolder, contribFolderName);

    for (LocalContribution oldContrib : oldContribs) {
      if ((oldContrib.getFolder().exists() && oldContrib.getFolder().equals(contribFolder)) ||
          (oldContrib.getId() != null && oldContrib.getId().equals(getId()))) {

        if (oldContrib.requiresRestart()) {
          // XXX: We can't replace stuff, soooooo.... do something different
          if (!oldContrib.backup(editor, false, status)) {
            return null;
          }
        } else {
          int result = 0;
          boolean doBackup = Preferences.getBoolean("contribution.backup.on_install");
          if (confirmReplace) {
            if (doBackup) {
              result = Base.showYesNoQuestion(editor, "Replace",
                     "Replace pre-existing \"" + oldContrib.getName() + "\" library?",
                     "A pre-existing copy of the \"" + oldContrib.getName() + "\" library<br>"+
                     "has been found in your sketchbook. Clicking “Yes”<br>"+
                     "will move the existing library to a backup folder<br>" +
                     " in <i>libraries/old</i> before replacing it.");
              if (result != JOptionPane.YES_OPTION || !oldContrib.backup(editor, true, status)) {
                return null;
              }
            } else {
              result = Base.showYesNoQuestion(editor, "Replace",
                     "Replace pre-existing \"" + oldContrib.getName() + "\" library?",
                     "A pre-existing copy of the \"" + oldContrib.getName() + "\" library<br>"+
                     "has been found in your sketchbook. Clicking “Yes”<br>"+
                     "will permanently delete this library and all of its contents<br>"+
                     "before replacing it.");
              if (result != JOptionPane.YES_OPTION || !oldContrib.getFolder().delete()) {
                return null;
              }
            }
          } else {
            if ((doBackup && !oldContrib.backup(editor, true, status)) ||
                (!doBackup && !oldContrib.getFolder().delete())) {
              return null;
            }
          }
        }
      }
    }

    // At this point it should be safe to replace this fella
    if (contribFolder.exists()) {
      Base.removeDir(contribFolder);
    }

    if (!getFolder().renameTo(contribFolder)) {
      status.setErrorMessage("Could not move " + getTypeName() + 
                                " \"" + getName() + "\" to the sketchbook.");
      return null;
    }
    return getType().load(editor.getBase(), contribFolder);
  }


  /**
   * Moves the given contribution to a backup folder.
   * @param deleteOriginal
   *          true if the file should be moved to the directory, false if it
   *          should instead be copied, leaving the original in place
   */
  boolean backup(Editor editor, boolean deleteOriginal, StatusPanel status) {

    boolean success = false;
    File backupFolder = getType().createBackupFolder(status);
    
    if (backupFolder != null) {
      String libFolderName = getFolder().getName();
      String prefix = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
      final String backupName = prefix + "_" + libFolderName;
      File backupSubFolder = ContributionManager.getUniqueName(backupFolder, backupName);

      if (deleteOriginal) {
        success = getFolder().renameTo(backupSubFolder);
      } else {
        try {
          Base.copyDir(getFolder(), backupSubFolder);
          success = true;
        } catch (IOException e) { }
      }
      if (!success) {
        status.setErrorMessage("Could not move contribution to backup folder.");
      }
    }
    return success;
  }


  /**
   * Non-blocking call to remove a contribution in a new thread.
   */
  void removeContribution(final Editor editor,
                          final ProgressMonitor pm,
                          final StatusPanel status) {
    new Thread(new Runnable() {
      public void run() {
        remove(editor,
               pm,
               status, 
               ContributionListing.getInstance()); 
      }
    }).start();
  }
  
  
  void remove(final Editor editor,
              final ProgressMonitor pm,
              final StatusPanel status, 
              final ContributionListing contribListing) {
    pm.startTask("Removing", ProgressMonitor.UNKNOWN);

    boolean doBackup = Preferences.getBoolean("contribution.backup.on_remove");
    if (requiresRestart()) {
      if (!doBackup || (doBackup && backup(editor, false, status))) {
        if (setDeletionFlag()) {
          contribListing.replaceContribution(this, this);
        }
      }
    } else {
      boolean success = false;
      if (doBackup) {
        success = backup(editor, true, status);
      } else {
        Base.removeDir(getFolder());
        success = !getFolder().exists();
      }

      if (success) {
        Contribution advertisedVersion =
          contribListing.getAvailableContribution(this);

        if (advertisedVersion == null) {
          contribListing.removeContribution(this);
        } else {
          contribListing.replaceContribution(this, advertisedVersion);
        }
      } else {
        // There was a failure backing up the folder
        if (!doBackup) {
          status.setErrorMessage("Could not delete the contribution's files");
        }
      }
    }
    ContributionManager.refreshInstalled(editor);
    pm.finished();
  }

  
  public File getFolder() {
    return folder;
  }


  public boolean isInstalled() {
    return folder != null;
  }
  
  
  boolean setDeletionFlag() {
    // Only returns false if the file already exists, so we can
    // ignore the return value.
    try {
      new File(getFolder(), DELETION_FLAG).createNewFile();
      return true;
    } catch (IOException e) {
      return false;
    }
  }


  boolean unsetDeletionFlag() {
    return new File(getFolder(), DELETION_FLAG).delete();
  }


  boolean isDeletionFlagged() {
    return isDeletionFlagged(getFolder());
  }


  static boolean isDeletionFlagged(File folder) {
    return new File(folder, DELETION_FLAG).exists();
  }


//  public String getCategory() {
//    return category;
//  }
//
//
//  public String getName() {
//    return name;
//  }


  public String getId() {
    return id;
  }


//  public String getAuthorList() {
//    return authorList;
//  }
//
//
//  public String getUrl() {
//    return url;
//  }
//
//
//  public String getSentence() {
//    return sentence;
//  }
//
//
//  public String getParagraph() {
//    return paragraph;
//  }
//
//
//  public int getVersion() {
//    return version;
//  }


  public int getLatestVersion() {
    return latestVersion;
  }


//  public String getPrettyVersion() {
//    return prettyVersion;
//  }
//
//
//  public String getTypeName() {
//    return getType().toString();
//  }


  /*
  static protected String findClassInZipFileList(String base, File[] fileList) {
    for (File file : fileList) {
      String found = findClassInZipFile(base, file);
      if (found != null) {
        return found;
      }
    }
    return null;
  }
  */


  /**
   *
   * @param base name of the class, with or without the package
   * @param file
   * @return name of class (with full package name) or null if not found
   */
  static protected String findClassInZipFile(String base, File file) {
    // Class file to search for
    String classFileName = "/" + base + ".class";

    try {
      ZipFile zipFile = new ZipFile(file);
      Enumeration<?> entries = zipFile.entries();
      while (entries.hasMoreElements()) {
        ZipEntry entry = (ZipEntry) entries.nextElement();

        if (!entry.isDirectory()) {
          String name = entry.getName();
//          System.out.println("entry: " + name);

          if (name.endsWith(classFileName)) {
            //int slash = name.lastIndexOf('/');
            //String packageName = (slash == -1) ? "" : name.substring(0, slash);
            // Remove .class and convert slashes to periods.
            zipFile.close();
            return name.substring(0, name.length() - 6).replace('/', '.');
          }
        }
      }
      zipFile.close();
    } catch (IOException e) {
      //System.err.println("Ignoring " + filename + " (" + e.getMessage() + ")");
      e.printStackTrace();
    }
    return null;
  }


  class IgnorableException extends Exception {
    public IgnorableException(String msg) {
      super(msg);
    }
  }
}
