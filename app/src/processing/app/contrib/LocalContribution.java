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

import java.awt.EventQueue;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.*;

import javax.swing.JOptionPane;

import processing.app.*;
import processing.app.ui.Editor;
import processing.core.PApplet;
import processing.data.StringDict;
import processing.data.StringList;


/**
 * A contribution that has been downloaded to the disk, and may or may not
 * be installed.
 */
public abstract class LocalContribution extends Contribution {
  static public final String DELETION_FLAG = "marked_for_deletion";
  static public final String UPDATE_FLAGGED = "marked_for_update";
  static public final String RESTART_FLAG = "requires_restart";

  protected String id;          // 1 (unique id for this library)
  protected int latestVersion;  // 103
  protected File folder;
  protected StringDict properties;
  protected ClassLoader loader;


  public LocalContribution(File folder) {
    this.folder = folder;

    // required for contributed modes, but not for built-in core modes
    File propertiesFile = new File(folder, getTypeName() + ".properties");
    if (propertiesFile.exists()) {
      properties = Util.readSettings(propertiesFile);

      name = properties.get("name");
      id = properties.get("id");
      categories = parseCategories(properties);
      imports = parseImports(properties);
      if (name == null) {
        name = folder.getName();
      }
      // changing to 'authors' in 3.0a11
      authors = properties.get(AUTHORS_PROPERTY);
      if (authors == null) {
        authors = properties.get("authorList");
      }
      url = properties.get("url");
      sentence = properties.get("sentence");
      paragraph = properties.get("paragraph");

      try {
        version = Integer.parseInt(properties.get("version"));
      } catch (NumberFormatException e) {
        System.err.println("The version number for the “" + name + "” library is not set properly.");
        System.err.println("Please contact the library author to fix it according to the guidelines.");
      }

      prettyVersion = properties.get("prettyVersion");

      try {
        lastUpdated = Long.parseLong(properties.get("lastUpdated"));
      } catch (NumberFormatException e) {
        lastUpdated = 0;

      // Better comment these out till all contribs have a lastUpdated
//        System.err.println("The last updated timestamp for the “" + name + "” library is not set properly.");
//        System.err.println("Please contact the library author to fix it according to the guidelines.");
      }

      String minRev = properties.get("minRevision");
      if (minRev != null) {
        minRevision = PApplet.parseInt(minRev, 0);
      }

      String maxRev = properties.get("maxRevision");
      if (maxRev != null) {
        maxRevision = PApplet.parseInt(maxRev, 0);
      }

    } else {
      Messages.log("No properties file at " + propertiesFile.getAbsolutePath());
      // We'll need this to be set at a minimum.
      name = folder.getName();
      categories = unknownCategoryList();
    }

    if (categories.hasValue(SPECIAL_CATEGORY)) {
      validateSpecial();
    }
  }


  private void validateSpecial() {
    for (AvailableContribution available : ContributionListing.getInstance().advertisedContributions) {
      if (available.getName().equals(name)) {
        if (!available.isSpecial()) {
          categories.removeValue(SPECIAL_CATEGORY);
        }
      }
      break;
    }
  }


  public String initLoader(String className) throws Exception {
    File modeDirectory = new File(folder, getTypeName());
    if (modeDirectory.exists()) {
      Messages.log("checking mode folder regarding " + className);
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
      File[] archives = Util.listJarFiles(modeDirectory);
      if (archives != null && archives.length > 0) {
        URL[] urlList = new URL[archives.length];
        for (int j = 0; j < urlList.length; j++) {
          Messages.log("Found archive " + archives[j] + " for " + getName());
          urlList[j] = archives[j].toURI().toURL();
        }
//        loader = new URLClassLoader(urlList, Thread.currentThread().getContextClassLoader());
        loader = new URLClassLoader(urlList);
        Messages.log("loading above JARs with loader " + loader);
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


  LocalContribution copyAndLoad(Base base,
                                boolean confirmReplace,
                                StatusPanel status) {
// NOTE: null status => function is called on startup when Editor objects, et al. aren't ready

    String contribFolderName = getFolder().getName();

    File contribTypeFolder = getType().getSketchbookFolder();
    File contribFolder = new File(contribTypeFolder, contribFolderName);

    if (status != null) { // when status != null, install is not occurring on startup

      Editor editor = base.getActiveEditor();

      ArrayList<LocalContribution> oldContribs =
        getType().listContributions(editor);

      // In case an update marker exists, and the user wants to install, delete the update marker
      if (contribFolder.exists() && !contribFolder.isDirectory()) {
        contribFolder.delete();
        contribFolder = new File(contribTypeFolder, contribFolderName);
      }

      for (LocalContribution oldContrib : oldContribs) {
        if ((oldContrib.getFolder().exists() && oldContrib.getFolder().equals(contribFolder)) ||
            (oldContrib.getId() != null && oldContrib.getId().equals(getId()))) {

          if (oldContrib.getType().requiresRestart()) {
            // XXX: We can't replace stuff, soooooo.... do something different
            if (!oldContrib.backup(false, status)) {
              return null;
            }
          } else {
            int result = 0;
            boolean doBackup = Preferences.getBoolean("contribution.backup.on_install");
            if (confirmReplace) {
              if (doBackup) {
                result = Messages.showYesNoQuestion(editor, "Replace",
                       "Replace pre-existing \"" + oldContrib.getName() + "\" library?",
                       "A pre-existing copy of the \"" + oldContrib.getName() + "\" library<br>"+
                       "has been found in your sketchbook. Clicking “Yes”<br>"+
                       "will move the existing library to a backup folder<br>" +
                       "in <i>libraries/old</i> before replacing it.");
                if (result != JOptionPane.YES_OPTION || !oldContrib.backup(true, status)) {
                  return null;
                }
              } else {
                result = Messages.showYesNoQuestion(editor, "Replace",
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
              if ((doBackup && !oldContrib.backup(true, status)) ||
                  (!doBackup && !oldContrib.getFolder().delete())) {
                return null;
              }
            }
          }
        }
      }

      // At this point it should be safe to replace this fella
      if (contribFolder.exists()) {
        Util.removeDir(contribFolder);
      }

    }
    else {
      // This if should ideally never happen, since this function
      // is to be called only when restarting on update
      if (contribFolder.exists() && contribFolder.isDirectory()) {
        Util.removeDir(contribFolder);
      }
      else if (contribFolder.exists()) {
        contribFolder.delete();
        contribFolder = new File(contribTypeFolder, contribFolderName);
      }
    }

    File oldFolder = getFolder();

    try {
      Util.copyDir(oldFolder, contribFolder);
    } catch (IOException e) {
      status.setErrorMessage("Could not copy " + getTypeName() +
                             " \"" + getName() + "\" to the sketchbook.");
      e.printStackTrace();
      return null;
    }


    /*
    if (!getFolder().renameTo(contribFolder)) {
      status.setErrorMessage("Could not move " + getTypeName() +
                                " \"" + getName() + "\" to the sketchbook.");
      return null;
    }
    */

    return getType().load(base, contribFolder);
  }


  /**
   * Moves the given contribution to a backup folder.
   * @param deleteOriginal
   *          true if the file should be moved to the directory, false if it
   *          should instead be copied, leaving the original in place
   */
  boolean backup(boolean deleteOriginal, StatusPanel status) {
    File backupFolder = getType().createBackupFolder(status);

    boolean success = false;
    if (backupFolder != null) {
      String libFolderName = getFolder().getName();
      String prefix = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
      final String backupName = prefix + " " + libFolderName;
      File backupSubFolder =
        ContributionManager.getUniqueName(backupFolder, backupName);

      if (deleteOriginal) {
        success = getFolder().renameTo(backupSubFolder);
      } else {
        try {
          Util.copyDir(getFolder(), backupSubFolder);
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
  void removeContribution(final Base base,
                          final ContribProgressMonitor pm,
                          final StatusPanel status) {
    // TODO: replace with SwingWorker [jv]
    new Thread(new Runnable() {
      public void run() {
        remove(base,
               pm,
               status,
               ContributionListing.getInstance());
      }
    }, "Contribution Uninstaller").start();
  }


  void remove(final Base base,
              final ContribProgressMonitor pm,
              final StatusPanel status,
              final ContributionListing contribListing) {
    pm.startTask("Removing", ContribProgressMonitor.UNKNOWN);

    boolean doBackup = Preferences.getBoolean("contribution.backup.on_remove");
//    if (getType().requiresRestart()) {
//      if (!doBackup || (doBackup && backup(editor, false, status))) {
//        if (setDeletionFlag(true)) {
//          contribListing.replaceContribution(this, this);
//        }
//      }
//    } else {
    boolean success = false;
    if (getType() == ContributionType.MODE) {
      boolean isModeActive = false;
      ModeContribution m = (ModeContribution) this;
      Iterator<Editor> iter = base.getEditors().iterator();
      while (iter.hasNext()) {
        Editor e = iter.next();
        if (e.getMode().equals(m.getMode())) {
          isModeActive = true;
          break;
        }
      }
      if (!isModeActive) {
        m.clearClassLoader(base);
      } else {
        pm.cancel();
        Messages.showMessage("Mode Manager",
                             "Please save your Sketch and change the Mode of all Editor\n" +
                             "windows that have " + name + " as the active Mode.");
        return;
      }
    }

    if (getType() == ContributionType.TOOL) {
      /*
      ToolContribution t = (ToolContribution) this;
      Iterator<Editor> iter = editor.getBase().getEditors().iterator();
      while (iter.hasNext()) {
        Editor ed = iter.next();
        ed.clearToolMenu();
      }
      t.clearClassLoader(editor.getBase());
      */
      // menu will be rebuilt below with the refreshContribs() call
      base.clearToolMenus();
      ((ToolContribution) this).clearClassLoader();
    }

    if (doBackup) {
      success = backup(true, status);
    } else {
      Util.removeDir(getFolder());
      success = !getFolder().exists();
    }

    if (success) {
      // this was just rebuilding the tool menu in one editor, which happens
      // yet again down below with the call to refreshInstalled() [fry 150828]
//      if (getType() == ContributionType.TOOL) {
//        editor.removeTool();
//      }

      try {
        // TODO: run this in SwingWorker done() [jv]
        EventQueue.invokeAndWait(new Runnable() {
          @Override
          public void run() {
            Contribution advertisedVersion =
                contribListing.getAvailableContribution(LocalContribution.this);

            if (advertisedVersion == null) {
              contribListing.removeContribution(LocalContribution.this);
            } else {
              contribListing.replaceContribution(LocalContribution.this, advertisedVersion);
            }
            base.setUpdatesAvailable(contribListing.countUpdates(base));
          }
        });
      } catch (InterruptedException e) {
        e.printStackTrace();
      } catch (InvocationTargetException e) {
        Throwable cause = e.getCause();
        if (cause instanceof RuntimeException) {
          throw (RuntimeException) cause;
        } else {
          cause.printStackTrace();
        }
      }

    } else {
      // There was a failure backing up the folder
      if (!doBackup || (doBackup && backup(false, status))) {
        if (setDeletionFlag(true)) {
          try {
            // TODO: run this in SwingWorker done() [jv]
            EventQueue.invokeAndWait(new Runnable() {
              @Override
              public void run() {
                contribListing.replaceContribution(LocalContribution.this,
                                                   LocalContribution.this);
                base.setUpdatesAvailable(contribListing.countUpdates(base));
              }
            });
          } catch (InterruptedException e) {
            e.printStackTrace();
          } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
              throw (RuntimeException) cause;
            } else {
              cause.printStackTrace();
            }
          }
        }
      } else {
        status.setErrorMessage("Could not delete the contribution's files");
      }
    }
    base.refreshContribs(this.getType());
    if (success) {
      pm.finished();
    } else {
      pm.cancel();
    }
  }


  public File getFolder() {
    return folder;
  }


  public boolean isInstalled() {
    return folder != null;
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
   * Returns the imports (package-names) for a library, as specified in its library.properties
   * (e.g., imports=libname.*,libname.support.*)
   *
   * @return String[] packageNames (without wildcards) or null if none are specified
   */
  public StringList getImports() {
    //return imports != null ? imports.toArray(new String[0]) : null;
    return imports;
  }


  // this duplicates code found in Contribution (though that version doesn't check for .* at the end)
//  /**
//   * @return the list of Java imports to be added to the sketch when the library is imported
//   * or null if none are specified
//   */
//  static StringList parseImports(String importsStr) {
//    StringList outgoing = new StringList();
//
//    if (importsStr != null) {
//      String[] listing = PApplet.trim(PApplet.split(importsStr, ','));
//      for (String imp : listing) {
//
//        // In case the wildcard is specified, strip it, as it gets added later)
//        if (imp.endsWith(".*")) {
//
//          imp = imp.substring(0, imp.length() - 2);
//        }
//
//        outgoing.add(imp);
//      }
//    }
////    return (outgoing.size() > 0) ? outgoing : null;
//    return outgoing;
//  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  boolean setDeletionFlag(boolean flag) {
    return setFlag(DELETION_FLAG, flag);
  }


  boolean isDeletionFlagged() {
    return isDeletionFlagged(getFolder());
  }


  static boolean isDeletionFlagged(File folder) {
    return isFlagged(folder, DELETION_FLAG);
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  boolean setUpdateFlag(boolean flag) {
    return setFlag(UPDATE_FLAGGED, flag);
  }


  boolean isUpdateFlagged() {
    return isUpdateFlagged(getFolder());
  }


  static boolean isUpdateFlagged(File folder) {
    return isFlagged(folder, UPDATE_FLAGGED);
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  boolean setRestartFlag() {
    //System.out.println("setting restart flag for " + folder);
    return setFlag(RESTART_FLAG, true);
  }


  @Override
  boolean isRestartFlagged() {
    //System.out.println("checking for restart inside LocalContribution for " + getName());
    return isFlagged(getFolder(), RESTART_FLAG);
  }


  static void clearRestartFlags(File folder) {
    File restartFlag = new File(folder, RESTART_FLAG);
    if (restartFlag.exists()) {
      restartFlag.delete();
    }
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  private boolean setFlag(String flagFilename, boolean flag) {
    if (flag) {
      // Only returns false if the file already exists, so we can
      // ignore the return value.
      try {
        new File(getFolder(), flagFilename).createNewFile();
        return true;
      } catch (IOException e) {
        return false;
      }
    } else {
      return new File(getFolder(), flagFilename).delete();
    }
  }


  static private boolean isFlagged(File folder, String flagFilename) {
    return new File(folder, flagFilename).exists();
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


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
}
