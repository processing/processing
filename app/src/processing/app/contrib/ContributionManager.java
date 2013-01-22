package processing.app.contrib;

import java.io.*;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.*;

import javax.swing.JOptionPane;

import processing.app.Base;
import processing.app.Editor;
import processing.app.Library;
import processing.app.Preferences;
import processing.app.contrib.Contribution.Type;
import processing.app.contrib.ContributionListing.AdvertisedContribution;


interface ErrorWidget {
  void setErrorMessage(String msg);
}


public class ContributionManager {
  static public final String DELETION_FLAG = "flagged_for_deletion";
  static public final ContributionListing contribListing;

  static {
    contribListing = ContributionListing.getInstance();
  }


  /**
   * Non-blocking call to remove a contribution in a new thread.
   */
  static public void removeContribution(final Editor editor,
                                        final InstalledContribution contribution,
                                        final ProgressMonitor pm,
                                        final ErrorWidget statusBar) {
    if (contribution != null) {
      final ProgressMonitor progressMonitor = (pm != null) ? pm : new NullProgressMonitor();

      new Thread(new Runnable() {

        public void run() {
          progressMonitor.startTask("Removing", ProgressMonitor.UNKNOWN);

          boolean doBackup = Preferences.getBoolean("contribution.backup.on_remove");
          if (ContributionManager.requiresRestart(contribution)) {

            if (!doBackup || (doBackup && backupContribution(editor, contribution, false, statusBar))) {
              if (ContributionManager.setDeletionFlag(contribution)) {
                contribListing.replaceContribution(contribution, contribution);
              }
            }
          } else {
            boolean success = false;
            if (doBackup) {
              success = backupContribution(editor, contribution, true, statusBar);
            } else {
              Base.removeDir(contribution.getFolder());
              success = !contribution.getFolder().exists();
            }

            if (success) {
              Contribution advertisedVersion =
                contribListing.getAdvertisedContribution(contribution);

              if (advertisedVersion == null) {
                contribListing.removeContribution(contribution);
              } else {
                contribListing.replaceContribution(contribution,
                                                   advertisedVersion);
              }
            } else {
              // There was a failure backing up the folder
              if (doBackup) {

              } else {
                statusBar.setErrorMessage("Could not delete the contribution's files");
              }
            }
          }
          refreshInstalled(editor);
          progressMonitor.finished();
        }
      }).start();
    }
  }


  /**
   * Non-blocking call to download and install a contribution in a new thread.
   *
   * @param url
   *          Direct link to the contribution.
   * @param toBeReplaced
   *          The Contribution that will be replaced by this library being
   *          installed (e.g. an advertised version of a contribution, or the
   *          old version of a contribution that is being updated). Must not be
   *          null.
   */
  static public void downloadAndInstall(final Editor editor,
                                        final URL url,
                                        final AdvertisedContribution ad,
                                        final JProgressMonitor downloadProgressMonitor,
                                        final JProgressMonitor installProgressMonitor,
                                        final ErrorWidget statusBar) {

    new Thread(new Runnable() {
      public void run() {
        final File libArchive = createTemporaryFile(url, statusBar);
        FileDownloader.downloadFile(url, libArchive, downloadProgressMonitor);
        if (!downloadProgressMonitor.isCanceled() && !downloadProgressMonitor.isError()) {
          installProgressMonitor.startTask("Installing", ProgressMonitor.UNKNOWN);
          InstalledContribution contribution = null;
          contribution = install(editor, libArchive, ad, false, statusBar);

          if (contribution != null) {
            contribListing.replaceContribution(ad, contribution);
            refreshInstalled(editor);
          }
          installProgressMonitor.finished();
        }
        libArchive.delete();
      }
    }).start();
  }


  static public void refreshInstalled(Editor editor) {
    editor.getMode().rebuildImportMenu();
    editor.rebuildToolMenu();
  }


  /**
   * Used after unpacking a contrib download do determine the file contents.
   */
  static List<File> discover(Contribution.Type type, File tempDir) {
    switch (type) {
    case LIBRARY:
      return Library.discover(tempDir);
    case LIBRARY_COMPILATION:
      // XXX Implement
      return null;
    case TOOL:
      return ToolContribution.discover(tempDir);
    case MODE:
      return ModeContribution.discover(tempDir);
    }
    return null;
  }


//  static String getPropertiesFileName(Type type) {
//    return type.toString() + ".properties";
////    switch (type) {
////    case LIBRARY:
////      return Library.propertiesFileName;
////    case LIBRARY_COMPILATION:
////      return LibraryCompilation.propertiesFileName;
////    case TOOL:
////      return ToolContribution.propertiesFileName;
////    case MODE:
////      return ModeContribution.propertiesFileName;
////    }
////    return null;
//  }


  static File getSketchbookContribFolder(Base base, Type type) {
    switch (type) {
    case LIBRARY:
    case LIBRARY_COMPILATION:
      return Base.getSketchbookLibrariesFolder();
    case TOOL:
      return Base.getSketchbookToolsFolder();
    case MODE:
      return Base.getSketchbookModesFolder();
    }
    return null;
  }


  static InstalledContribution create(Base base, File folder, Type type) {
    switch (type) {
    case LIBRARY:
      return new Library(folder);
    case LIBRARY_COMPILATION:
      return LibraryCompilation.create(folder);
    case TOOL:
      return ToolContribution.load(folder);
    case MODE:
      return ModeContribution.load(base, folder);
    }
    return null;
  }


  static ArrayList<InstalledContribution> getContributions(Type type, Editor editor) {
    ArrayList<InstalledContribution> contribs = new ArrayList<InstalledContribution>();
    switch (type) {
    case LIBRARY:
      contribs.addAll(editor.getMode().contribLibraries);
      break;
    case LIBRARY_COMPILATION:
      contribs.addAll(LibraryCompilation.list(editor.getMode().contribLibraries));
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


//  static void initialize(InstalledContribution contribution, Base base) throws Exception {
//    if (contribution instanceof ToolContribution) {
//      ((ToolContribution) contribution).initializeToolClass();
//    } else if (contribution instanceof ModeContribution) {
//      ((ModeContribution) contribution).instantiateModeClass(base);
//    }
//  }


  /**
   * @param libFile
   *          a zip file containing the library to install
   * @param ad
   *          the advertised version of this library, if it was downloaded
   *          through the Contribution Manager. This is used to check the type
   *          of library being installed, and to replace the .properties file in
   *          the zip
   * @param confirmReplace
   *          true to open a dialog asking the user to confirm removing/moving
   *          the library when a library by the same name already exists
   * @return
   */
  static public InstalledContribution install(Editor editor, File libFile,
                                              AdvertisedContribution ad,
                                              boolean confirmReplace,
                                              ErrorWidget statusBar) {

    File tempDir = ContributionManager.unzipFileToTemp(libFile, statusBar);
    List<File> libFolders = ContributionManager.discover(ad.getType(), tempDir);
    InstalledContribution outgoing = null;

    if (libFolders.isEmpty()) {
      // Sometimes library authors place all their folders in the base
      // directory of a zip file instead of in single folder as the
      // guidelines suggest. If this is the case, we might be able to find the
      // library by stepping up a directory and searching for libraries again.
      libFolders = ContributionManager.discover(ad.getType(), tempDir.getParentFile());
    }

    if (libFolders != null && libFolders.size() == 1) {
      File libFolder = libFolders.get(0);
//      File propFile = new File(libFolder, getPropertiesFileName(ad.getType()));
      File propFile = new File(libFolder, ad.getTypeName() + ".properties");

      if (ad.writePropertiesFile(propFile)) {
        InstalledContribution newContrib =
          ContributionManager.create(editor.getBase(), libFolder, ad.getType());

        outgoing = ContributionManager.installContribution(editor, newContrib,
                                                           confirmReplace,
                                                           statusBar);
      } else {
        statusBar.setErrorMessage("Error overwriting .properties file.");
      }
    } else {
      // Diagnose the problem and notify the user
      if (libFolders == null) {
        statusBar.setErrorMessage("An internal error occured while searching "
            + "for contributions in the downloaded file.");
      } else if (libFolders.isEmpty()) {
        statusBar.setErrorMessage("Maybe it's just me, but it looks like " +
                                  "there are no contributions in the file " +
                                  "for \"" + ad.getName() + ".\"");
      } else {
        statusBar.setErrorMessage("There were multiple libraries in the file, " +
                                  "so we're ignoring it.");
      }
    }

    Base.removeDir(tempDir);
    return outgoing;
  }


  /**
   * @param confirmReplace
   *          if true and the library is already installed, opens a prompt to
   *          ask the user if it's okay to replace the library. If false, the
   *          library is always replaced with the new copy.
   */
  static public InstalledContribution installContribution(Editor editor, InstalledContribution newContrib,
                                                          boolean confirmReplace, ErrorWidget statusBar) {

    ArrayList<InstalledContribution> oldContribs = getContributions(newContrib.getType(), editor);
    String libFolderName = newContrib.getFolder().getName();

    File libraryDestination =
      ContributionManager.getSketchbookContribFolder(editor.getBase(), newContrib.getType());
    File newContribDest = new File(libraryDestination, libFolderName);

    for (InstalledContribution oldContrib : oldContribs) {

      if ((oldContrib.getFolder().exists() && oldContrib.getFolder().equals(newContribDest)) ||
          (oldContrib.getId() != null && oldContrib.getId().equals(newContrib.getId()))) {

        if (ContributionManager.requiresRestart(oldContrib)) {
          // XXX: We can't replace stuff, soooooo.... do something different
          if (!backupContribution(editor, oldContrib, false, statusBar)) {
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
              if (result != JOptionPane.YES_OPTION || !backupContribution(editor, oldContrib, true, statusBar)) {
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
            if ((doBackup && !backupContribution(editor, oldContrib, true, statusBar)) ||
                (!doBackup && !oldContrib.getFolder().delete())) {
              return null;
            }
          }
        }
      }
    }

    if (newContribDest.exists()) {
      Base.removeDir(newContribDest);
    }

    // Move newLib to the sketchbook library folder
    if (newContrib.getFolder().renameTo(newContribDest)) {
      Base base = editor.getBase();
      /* InstalledContribution contrib = */
      ContributionManager.create(base, newContribDest, newContrib.getType());
//      try {
//        initialize(contrib, base);
//        return contrib;
//      } catch (Exception e) {
//        e.printStackTrace();
//      }

//      try {
//        FileUtils.copyDirectory(newLib.folder, libFolder);
//        FileUtils.deleteQuietly(newLib.folder);
//        newLib.folder = libFolder;
//      } catch (IOException e) {
    } else {
      String errorMsg = null;
      switch (newContrib.getType()) {
      case LIBRARY:
        errorMsg = "Could not move library \"" + newContrib.getName() + "\" to sketchbook.";
        break;
      case LIBRARY_COMPILATION:
        break;
      case TOOL:
        errorMsg = "Could not move tool \"" + newContrib.getName() + "\" to sketchbook.";
        break;
      case MODE:
        break;
      }
      statusBar.setErrorMessage(errorMsg);
    }
    return null;
  }


  /*
  static public boolean writePropertiesFile(File propFile, AdvertisedContribution ad) {
    try {
      if (propFile.delete() && propFile.createNewFile() && propFile.setWritable(true)) {
        //BufferedWriter bw = new BufferedWriter(new FileWriter(propFile));
        PrintWriter writer = PApplet.createWriter(propFile);

        writer.println("name=" + ad.getName());
        writer.println("category=" + ad.getCategory());
        writer.println("authorList=" + ad.getAuthorList());
        writer.println("url=" + ad.getUrl());
        writer.println("sentence=" + ad.getSentence());
        writer.println("paragraph=" + ad.getParagraph());
        writer.println("version=" + ad.getVersion());
        writer.println("prettyVersion=" + ad.getPrettyVersion());

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
  */


  /**
   * Moves the given contribution to a backup folder.
   * @param doDeleteOriginal
   *          true if the file should be moved to the directory, false if it
   *          should instead be copied, leaving the original in place
   */
  static public boolean backupContribution(Editor editor,
                                           InstalledContribution contribution,
                                           boolean doDeleteOriginal,
                                           ErrorWidget statusBar) {

    File backupFolder = null;

    switch (contribution.getType()) {
    case LIBRARY:
    case LIBRARY_COMPILATION:
      backupFolder = createLibraryBackupFolder(editor, statusBar);
      break;
    case MODE:
      break;
    case TOOL:
      backupFolder = createToolBackupFolder(editor, statusBar);
      break;
    }

    if (backupFolder == null) return false;

    String libFolderName = contribution.getFolder().getName();

    String prefix = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
    final String backupName = prefix + "_" + libFolderName;
    File backupSubFolder = ContributionManager.getUniqueName(backupFolder, backupName);

//    try {
//      FileUtils.moveDirectory(lib.folder, backupFolderForLib);
//      return true;

    boolean success = false;
    if (doDeleteOriginal) {
      success = contribution.getFolder().renameTo(backupSubFolder);
    } else {
      try {
        Base.copyDir(contribution.getFolder(), backupSubFolder);
        success = true;
      } catch (IOException e) {
      }
    }
//    } catch (IOException e) {
    if (!success) {
      statusBar.setErrorMessage("Could not move contribution to backup folder.");
    }
    return success;
  }


  static public File createLibraryBackupFolder(Editor editor, ErrorWidget logger) {
    File libraryBackupFolder = new File(Base.getSketchbookLibrariesFolder(), "old");
    return createBackupFolder(libraryBackupFolder, logger,
                              "Could not create backup folder for library.");
  }


  static public File createToolBackupFolder(Editor editor, ErrorWidget logger) {
    File libraryBackupFolder = new File(Base.getSketchbookToolsFolder(), "old");
    return createBackupFolder(libraryBackupFolder, logger,
                              "Could not create backup folder for tool.");
  }


  static private File createBackupFolder(File backupFolder,
                                  ErrorWidget logger,
                                  String errorMessage) {
    if (!backupFolder.exists() || !backupFolder.isDirectory()) {
      if (!backupFolder.mkdirs()) {
        logger.setErrorMessage(errorMessage);
        return null;
      }
    }
    return backupFolder;
  }


  /**
   * Returns a file in the parent folder that does not exist yet. If
   * parent/fileName already exists, this will look for parent/fileName(2)
   * then parent/fileName(3) and so forth.
   *
   * @return a file that does not exist yet
   */
  public static File getUniqueName(File parentFolder, String fileName) {
    File backupFolderForLib;
    int i = 1;
    do {
      String folderName = fileName;
      if (i >= 2) {
        folderName += "(" + i + ")";
      }
      i++;

      backupFolderForLib = new File(parentFolder, folderName);
    } while (backupFolderForLib.exists());

    return backupFolderForLib;
  }


  static protected File createTemporaryFile(URL url, ErrorWidget statusBar) {
    try {
//      //File tmpFolder = Base.createTempFolder("library", "download", Base.getSketchbookLibrariesFolder());
//      String[] segments = url.getFile().split("/");
//      File libFile = new File(tmpFolder, segments[segments.length - 1]);
      String filename = url.getFile();
      filename = filename.substring(filename.lastIndexOf('/') + 1);
      File libFile = File.createTempFile("download", filename, Base.getSketchbookLibrariesFolder());
      libFile.setWritable(true);
      return libFile;

    } catch (IOException e) {
      statusBar.setErrorMessage("Could not create a temp folder for download.");
    }
    return null;
  }


  /**
   * Creates a temporary folder and unzips a file to a subdirectory of the temp
   * folder. The subdirectory is the only file of the tempo folder.
   *
   * e.g. if the contents of foo.zip are /hello and /world, then the resulting
   * files will be
   *     /tmp/foo9432423uncompressed/foo/hello
   *     /tmp/foo9432423uncompress/foo/world
   * ...and "/tmp/id9432423uncompress/foo/" will be returned.
   *
   * @return the folder where the zips contents have been unzipped to (the
   *         subdirectory of the temp folder).
   */
  static public File unzipFileToTemp(File libFile, ErrorWidget statusBar) {
    String fileName = ContributionManager.getFileName(libFile);
    File tmpFolder = null;

    try {
      tmpFolder = Base.createTempFolder(fileName, "uncompressed", Base.getSketchbookLibrariesFolder());
//      tmpFolder = new File(tmpFolder, fileName);  // don't make another subdirectory
//      tmpFolder.mkdirs();
    } catch (IOException e) {
      statusBar.setErrorMessage("Could not create temp folder to uncompress zip file.");
    }

    ContributionManager.unzip(libFile, tmpFolder);
    return tmpFolder;
  }


  /**
   * Returns the name of a file without its path or extension.
   *
   * For example,
   *   "/path/to/helpfullib.zip" returns "helpfullib"
   *   "helpfullib-0.1.1.plb" returns "helpfullib-0.1.1"
   */
  static public String getFileName(File libFile) {
    String path = libFile.getPath();
    int lastSeparator = path.lastIndexOf(File.separatorChar);

    String fileName;
    if (lastSeparator != -1) {
      fileName = path.substring(lastSeparator + 1);
    } else {
      fileName = path;
    }

    int lastDot = fileName.lastIndexOf('.');
    if (lastDot != -1) {
      return fileName.substring(0, lastDot);
    }

    return fileName;
  }


  public static void unzip(File zipFile, File dest) {
    try {
      FileInputStream fis = new FileInputStream(zipFile);
      CheckedInputStream checksum = new CheckedInputStream(fis, new Adler32());
      ZipInputStream zis = new ZipInputStream(new BufferedInputStream(checksum));
      ZipEntry next = null;
      while ((next = zis.getNextEntry()) != null) {
        File currentFile = new File(dest, next.getName());
        if (next.isDirectory()) {
          currentFile.mkdirs();
        } else {
          currentFile.createNewFile();
          ContributionManager.unzipEntry(zis, currentFile);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }


  private static void unzipEntry(ZipInputStream zin, File f) throws IOException {
    FileOutputStream out = new FileOutputStream(f);
    byte[] b = new byte[512];
    int len = 0;
    while ((len = zin.read(b)) != -1) {
      out.write(b, 0, len);
    }
    out.flush();
    out.close();
  }


  /** Returns true if the type of contribution requires the PDE to restart
   * when being removed. */
  static public boolean requiresRestart(Contribution contrib) {
    return contrib.getType() == Type.TOOL || contrib.getType() == Type.MODE;
  }


  static public boolean setDeletionFlag(InstalledContribution contrib) {
    // Only returns false if the file already exists, so we can
    // ignore the return value.
    try {
      new File(contrib.getFolder(), DELETION_FLAG).createNewFile();
      return true;
    } catch (IOException e) {
      return false;
    }
  }


  static public boolean unsetDeletionFlag(InstalledContribution contrib) {
    return new File(contrib.getFolder(), DELETION_FLAG).delete();
  }


  static public boolean isDeletionFlagSet(Contribution contrib) {
    if (contrib instanceof InstalledContribution) {
      InstalledContribution installed = (InstalledContribution) contrib;
      return isDeletionFlagSet(installed.getFolder());
    }
    return false;
  }


  static public boolean isDeletionFlagSet(File folder) {
    return new File(folder, DELETION_FLAG).exists();
  }


  static public void checkDeletions(File root) {
    File[] markedForDeletion = root.listFiles(new FileFilter() {
      public boolean accept(File folder) {
        return (folder.isDirectory() && isDeletionFlagSet(folder));
      }
    });
    for (File folder : markedForDeletion) {
      Base.removeDir(folder);
    }
  }
}
