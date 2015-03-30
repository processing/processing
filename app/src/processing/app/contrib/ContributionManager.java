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
import java.net.*;
import java.util.*;

import javax.swing.SwingWorker;

import processing.app.Base;
import processing.app.Editor;
import processing.app.Language;


public class ContributionManager {
  static public final ContributionListing contribListing;

  static {
    contribListing = ContributionListing.getInstance();
  }


  /**
   * Blocks until the file is downloaded or an error occurs. Returns true if the
   * file was successfully downloaded, false otherwise.
   *
   * @param source
   *          the URL of the file to download
   * @param dest
   *          the file on the local system where the file will be written. This
   *          must be a file (not a directory), and must already exist.
   * @param progress
   *          null if progress is irrelevant, such as when downloading for an
   *          install during startup, when the ProgressMonitor is useless since
   *          UI isn't setup yet.
   * @throws FileNotFoundException
   *           if an error occurred downloading the file
   */
  static boolean download(URL source, File dest, ProgressMonitor progress) {
    boolean success = false;
    try {
//      System.out.println("downloading file " + source);
//      URLConnection conn = source.openConnection();
      HttpURLConnection conn = (HttpURLConnection) source.openConnection();
      HttpURLConnection.setFollowRedirects(true);
      conn.setConnectTimeout(15 * 1000);
      conn.setReadTimeout(60 * 1000);
      conn.setRequestMethod("GET");
      conn.connect();

      if (progress != null) {
        // TODO this is often -1, may need to set progress to indeterminate
        int fileSize = conn.getContentLength();
        progress.max = fileSize;
//      System.out.println("file size is " + fileSize);
        progress.startTask(Language.text("contrib.progress.downloading"), fileSize);
      }

      InputStream in = conn.getInputStream();
      FileOutputStream out = new FileOutputStream(dest);

      byte[] b = new byte[8192];
      int amount;
      if (progress != null) {
        int total = 0;
        while (!progress.isCanceled() && (amount = in.read(b)) != -1) {
          out.write(b, 0, amount);
          total += amount;
          progress.setProgress(total);
        }
      } else {
        while ((amount = in.read(b)) != -1) {
          out.write(b, 0, amount);
        }
      }
      out.flush();
      out.close();
      success = true;

    } catch (SocketTimeoutException ste) {
      if (progress != null) {
        progress.error(ste);
        progress.cancel();
      }
    } catch (IOException ioe) {
      if (progress != null) {
        progress.error(ioe);
        progress.cancel();
      }
      // Hiding stack trace. An error has been shown where needed.
//      ioe.printStackTrace();
    }
    if (progress != null)
      progress.finished();
    return success;
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
  static void downloadAndInstall(final Editor editor,
                                 final URL url,
                                 final AvailableContribution ad,
                                 final JProgressMonitor downloadProgress,
                                 final JProgressMonitor installProgress,
                                 final StatusPanel status) {

    new Thread(new Runnable() {
      public void run() {
        String filename = url.getFile();
        filename = filename.substring(filename.lastIndexOf('/') + 1);
        try {
          File contribZip = File.createTempFile("download", filename);
          contribZip.setWritable(true);  // necessary?

          try {
            download(url, contribZip, downloadProgress);

            if (!downloadProgress.isCanceled() && !downloadProgress.isError()) {
              installProgress.startTask(Language.text("contrib.progress.installing"), ProgressMonitor.UNKNOWN);
              LocalContribution contribution =
                ad.install(editor.getBase(), contribZip, false, status);

              if (contribution != null) {
                contribListing.replaceContribution(ad, contribution);
                if (contribution.getType() == ContributionType.MODE) {
                  ArrayList<ModeContribution> contribModes = editor.getBase().getModeContribs();
                  if (!contribModes.contains(contribution))
                    contribModes.add((ModeContribution) contribution);
                }
                refreshInstalled(editor);
              }
              installProgress.finished();
            }
            else {
              if (downloadProgress.exception instanceof SocketTimeoutException) {
                status.setErrorMessage(Language
                  .interpolate("contrib.errors.contrib_download.timeout",
                               ad.getName()));
              } else {
                status.setErrorMessage(Language
                  .interpolate("contrib.errors.download_and_install",
                               ad.getName()));
              }
            }
            contribZip.delete();

          } catch (Exception e) {
            // Hiding stack trace. The error message ought to suffice.
//            e.printStackTrace();
            status
              .setErrorMessage(Language
                .interpolate("contrib.errors.download_and_install",
                             ad.getName()));
            downloadProgress.cancel();
            installProgress.cancel();
          }
        } catch (IOException e) {
          status.setErrorMessage(Language.text("contrib.errors.temporary_directory"));
          downloadProgress.cancel();
          installProgress.cancel();
        }
      }
    }, "Contribution Installer").start();
  }



  /**
   * Non-blocking call to download and install a contribution in a new thread.
   *
   * @param url
   *          Direct link to the contribution.
   * @param ad
   *          The AvailableContribution to be downloaded and installed.
   */
  static void downloadAndInstallOnStartup(final Base base, final URL url,
                                          final AvailableContribution ad) {

    new Thread(new Runnable() {
      public void run() {
        String filename = url.getFile();
        filename = filename.substring(filename.lastIndexOf('/') + 1);
        try {
          File contribZip = File.createTempFile("download", filename);
          contribZip.setWritable(true); // necessary?

          try {
            download(url, contribZip, null);

            LocalContribution contribution = ad.install(base, contribZip,
                                                        false, null);

            if (contribution != null) {
              contribListing.replaceContribution(ad, contribution);
              if (contribution.getType() == ContributionType.MODE) {
                ArrayList<ModeContribution> contribModes = base
                  .getModeContribs();
                if (contribModes != null && !contribModes.contains(contribution)) {
                  contribModes.add((ModeContribution) contribution);
                }
              }
              if (base.getActiveEditor() != null)
                refreshInstalled(base.getActiveEditor());
            }

            contribZip.delete();

            handleUpdateFailedMarkers(ad, filename.substring(0, filename.lastIndexOf('.')));

          } catch (Exception e) {
//            Chuck the stack trace. The user might have no idea why it is appearing, or what (s)he did wrong...
//            e.printStackTrace();
            System.out.println("Error during download and install of "
              + ad.getName());
          }
        } catch (IOException e) {
          System.err
            .println("Could not write to temporary directory during download and install of "
              + ad.getName());
        }
      }
    }, "Contribution Installer").start();
  }


/**
 * After install, this function checks whether everything went properly or not.
 * If not, it adds a marker file so that the next time Processing is started, installPreviouslyFailed()
 * can install the contribution.
 * @param ac
 * The contribution just installed.
 * @param filename
 * The name of the folder in which the contribution is supposed to be stored.
 */
  static private void handleUpdateFailedMarkers(final AvailableContribution ac, String filename) {

    File contribLocn = ac.getType().getSketchbookFolder();

    for (File contribDir : contribLocn.listFiles())
      if (contribDir.isDirectory()) {
        File[] contents = contribDir.listFiles(new FilenameFilter() {

          @Override
          public boolean accept(File dir, String file) {
            return file.equals(ac.getType() + ".properties");
          }
        });
        if (contents.length > 0 && Base.readSettings(contents[0]).get("name").equals(ac.getName())) {
          return;
        }
      }

    try {
      new File(contribLocn, ac.getName()).createNewFile();
    } catch (IOException e) {
//      Again, forget about the stack trace. The user ain't done wrong
//      e.printStackTrace();
      System.err.println("The unupdated contribution marker seems to not like "
        + ac.getName() + ". You may have to install it manually to update...");
    }

  }


  static public void refreshInstalled(Editor e) {

    Iterator<Editor> iter = e.getBase().getEditors().iterator();
    while (iter.hasNext()) {
      Editor ed = iter.next();
      ed.getMode().rebuildImportMenu();
      ed.getMode().resetExamples();
      ed.rebuildToolMenu();
      ed.rebuildModeMenu();
    }
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


  /**
   * Called by Base to clean up entries previously marked for deletion
   * and remove any "requires restart" flags.
   * Also updates all entries previously marked for update.
   */
  static public void cleanup(final Base base) throws Exception {

    deleteTemp(Base.getSketchbookModesFolder());
    deleteTemp(Base.getSketchbookToolsFolder());

    deleteFlagged(Base.getSketchbookLibrariesFolder());
    deleteFlagged(Base.getSketchbookModesFolder());
    deleteFlagged(Base.getSketchbookToolsFolder());

    installPreviouslyFailed(base, Base.getSketchbookModesFolder());
    updateFlagged(base, Base.getSketchbookModesFolder());

    updateFlagged(base, Base.getSketchbookToolsFolder());

    SwingWorker s = new SwingWorker<Void, Void>() {

      @Override
      protected Void doInBackground() throws Exception {
        try {
          Thread.sleep(1 * 1000);
          installPreviouslyFailed(base, Base.getSketchbookToolsFolder());
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
        return null;
      }
    };
    s.execute();



    clearRestartFlags(Base.getSketchbookModesFolder());
    clearRestartFlags(Base.getSketchbookToolsFolder());
  }


  /**
   * Deletes the icky tmp folders that were left over from installs and updates
   * in the previous run of Processing. Needed to be called only on the tools
   * and modes sketchbook folders.
   *
   * @param root
   */
  static private void deleteTemp(File root) {

    LinkedList<File> deleteList = new LinkedList<File>();

    for (File f : root.listFiles())
      if (f.getName().matches(root.getName().substring(0, 4) + "\\d*" + "tmp"))
        deleteList.add(f);

    Iterator<File> folderIter = deleteList.iterator();

    while (folderIter.hasNext()) {
      Base.removeDir(folderIter.next());
    }
  }


  /**
   * Deletes all the modes/tools/libs that are flagged for removal.
   *
   * @param root
   * @throws Exception
   */
  static private void deleteFlagged(File root) throws Exception {
    File[] markedForDeletion = root.listFiles(new FileFilter() {
      public boolean accept(File folder) {
        return (folder.isDirectory() && LocalContribution
          .isDeletionFlagged(folder));
      }
    });
    for (File folder : markedForDeletion) {
      Base.removeDir(folder);
    }
  }


  /**
   * Installs all the modes/tools whose installation failed during an
   * auto-update the previous time Processing was started up.
   *
   * @param base
   * @param root
   * @throws Exception
   */
  static private void installPreviouslyFailed(Base base, File root) throws Exception {
    File[] installList = root.listFiles(new FileFilter() {
      public boolean accept(File folder) {
        return (folder.isFile());
      }
    });

    for (File file : installList) {
      Iterator<AvailableContribution> iter = contribListing.advertisedContributions.iterator();
      while (iter.hasNext()) {
        AvailableContribution availableContrib = iter.next();
        if (file.getName().equals(availableContrib.getName())) {
          file.delete();
          installOnStartUp(base, availableContrib);
          contribListing
            .replaceContribution(availableContrib, availableContrib);
        }
      }
    }
  }


  /**
   * Updates all the flagged modes/tools.
   *
   * @param base
   * @param root
   * @throws Exception
   */
  static private void updateFlagged(Base base, File root) throws Exception {
    File[] markedForUpdate = root.listFiles(new FileFilter() {
      public boolean accept(File folder) {
        return (folder.isDirectory() && LocalContribution
          .isUpdateFlagged(folder));
      }
    });

    ArrayList<String> updateContribsNames = new ArrayList<String>();
    LinkedList<AvailableContribution> updateContribsList = new LinkedList<AvailableContribution>();

    String type = root.getName().substring(root.getName().lastIndexOf('/') + 1);
    String propFileName = null;

    if (type.equalsIgnoreCase("tools"))
      propFileName = "tool.properties";
    else if (type.equalsIgnoreCase("modes"))
      propFileName = "mode.properties";
    else if (type.equalsIgnoreCase("libraries")) //putting this here, just in case
      propFileName = "libraries.properties";

    for (File folder : markedForUpdate) {
      Map<String, String> properties = 
        Base.readSettings(new File(folder, propFileName));
      updateContribsNames.add(properties.get("name"));
      Base.removeDir(folder);
    }

    Iterator<AvailableContribution> iter = contribListing.advertisedContributions.iterator();
    while (iter.hasNext()) {
      AvailableContribution availableContribs = iter.next();
      if (updateContribsNames.contains(availableContribs.getName())) {
        updateContribsList.add(availableContribs);
      }
    }

    Iterator<AvailableContribution> iter2 = updateContribsList.iterator();
    while (iter2.hasNext()) {
      AvailableContribution contribToUpdate = iter2.next();
      installOnStartUp(base, contribToUpdate);
      contribListing.replaceContribution(contribToUpdate, contribToUpdate);
    }
  }


  static private void installOnStartUp(final Base base, final AvailableContribution availableContrib) {
    if (availableContrib.link == null) {
      Base.showWarning(Language.interpolate("contrib.errors.update_on_restart_failed", availableContrib.getName()),
                       Language.text("contrib.unsupported_operating_system"));
      return;
    }
    try {
      URL downloadUrl = new URL(availableContrib.link);

      ContributionManager.downloadAndInstallOnStartup(base, downloadUrl, availableContrib);

    } catch (MalformedURLException e) {
      Base.showWarning(Language.interpolate("contrib.errors.update_on_restart_failed", availableContrib.getName()),
                       Language.text("contrib.errors.malformed_url"), e);
    }
  }


  static private void clearRestartFlags(File root) throws Exception {
    File[] folderList = root.listFiles(new FileFilter() {
      public boolean accept(File folder) {
        return folder.isDirectory();
      }
    });
    for (File folder : folderList) {
      LocalContribution.clearRestartFlags(folder);
    }
  }
}
