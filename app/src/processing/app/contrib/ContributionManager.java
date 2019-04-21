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

import java.awt.EventQueue;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.*;
import java.util.*;

import javax.swing.SwingWorker;

import processing.app.Base;
import processing.app.Language;
import processing.app.Messages;
import processing.app.Util;
import processing.app.ui.Editor;
import processing.core.PApplet;
import processing.data.StringDict;


public class ContributionManager {
  static ContributionListing listing;


  /**
   * Blocks until the file is downloaded or an error occurs.
   *
   * @param source the URL of the file to download
   * @param post Binary blob of POST data if a payload should be sent.
   *             Must already be URL-encoded and will be Gzipped for upload.
   * @param dest The file on the local system where the file will be written.
   *             This must be a file (not a directory), and must already exist.
   * @param progress null if progress is irrelevant, such as when downloading
   *                 for an install during startup, when the ProgressMonitor
   *                 is useless since UI isn't setup yet.
   *
   * @return true if the file was successfully downloaded, false otherwise.
   */
  static boolean download(URL source, byte[] post,
                          File dest, ContribProgressMonitor progress) {
    boolean success = false;
    try {
      HttpURLConnection conn = (HttpURLConnection) source.openConnection();
      // Will not handle a protocol change (see below)
      HttpURLConnection.setFollowRedirects(true);
      conn.setConnectTimeout(15 * 1000);
      conn.setReadTimeout(60 * 1000);

      if (post == null) {
        conn.setRequestMethod("GET");
        conn.connect();

      } else {
        post = Util.gzipEncode(post);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setRequestProperty("Content-Encoding", "gzip");
        conn.setRequestProperty("Content-Length", String.valueOf(post.length));
        conn.setUseCaches(false);
        conn.setDoInput(true);
        conn.setDoOutput(true);
        conn.getOutputStream().write(post);
      }

      if (progress != null) {
        // TODO this is often -1, may need to set progress to indeterminate
        int fileSize = conn.getContentLength();
        progress.max = fileSize;
//      System.out.println("file size is " + fileSize);
        progress.startTask(Language.text("contrib.progress.downloading"), fileSize);
      }

      int response = conn.getResponseCode();
      // Default won't follow HTTP -> HTTPS redirects for security reasons
      // http://stackoverflow.com/a/1884427
      if (response >= 300 && response < 400) {
        // Handle SSL redirects from HTTP sources
        // https://github.com/processing/processing/issues/5554
        String newLocation = conn.getHeaderField("Location");
        return download(new URL(newLocation), post, dest, progress);

      } else {
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
      }
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
    }
    if (progress != null) {
      progress.finished();
    }
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
  static void downloadAndInstall(final Base base,
                                 final URL url,
                                 final AvailableContribution ad,
                                 final ContribProgressBar downloadProgress,
                                 final ContribProgressBar installProgress,
                                 final StatusPanel status) {
    // TODO: replace with SwingWorker [jv]
    new Thread(new Runnable() {
      public void run() {
        String filename = url.getFile();
        filename = filename.substring(filename.lastIndexOf('/') + 1);
        try {
          File contribZip = File.createTempFile("download", filename);
          contribZip.setWritable(true);  // necessary?

          try {
            download(url, null, contribZip, downloadProgress);

            if (!downloadProgress.isCanceled() && !downloadProgress.isError()) {
              installProgress.startTask(Language.text("contrib.progress.installing"), ContribProgressMonitor.UNKNOWN);
              final LocalContribution contribution =
                ad.install(base, contribZip, false, status);

              if (contribution != null) {
                try {
                  // TODO: run this in SwingWorker done() [jv]
                  EventQueue.invokeAndWait(new Runnable() {
                    @Override
                    public void run() {
                      listing.replaceContribution(ad, contribution);
                      /*
                      if (contribution.getType() == ContributionType.MODE) {
                        List<ModeContribution> contribModes = editor.getBase().getModeContribs();
                        if (!contribModes.contains(contribution)) {
                          contribModes.add((ModeContribution) contribution);
                        }
                      }
                      */
                      base.refreshContribs(contribution.getType());
                      base.setUpdatesAvailable(listing.countUpdates(base));
                    }
                  });
                } catch (InterruptedException e) {
                  e.printStackTrace();
                } catch (InvocationTargetException e) {
                  throw (Exception) e.getCause();
                }
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

          //} catch (NoClassDefFoundError ncdfe) {
          } catch (Exception e) {
            String msg = null;
            if (e instanceof RuntimeException) {
              Throwable cause = ((RuntimeException) e).getCause();
              if (cause instanceof NoClassDefFoundError ||
                  cause instanceof NoSuchMethodError) {
                msg = "This item is not compatible with this version of Processing";
              } else if (cause instanceof UnsupportedClassVersionError) {
                msg = "This item needs to be recompiled for Java " +
                  PApplet.javaPlatform;
              }
            }

            if (msg == null) {
              msg = Language.interpolate("contrib.errors.download_and_install", ad.getName());
            }
            status.setErrorMessage(msg);
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
   * Used when information about the progress of the download and install
   * procedure is not of importance, such as if a contribution has to be
   * installed at startup time.
   *
   * @param url Direct link to the contribution.
   * @param ad The AvailableContribution to be downloaded and installed.
   */
  static void downloadAndInstallOnStartup(final Base base, final URL url,
                                          final AvailableContribution ad) {
    // TODO: replace with SwingWorker [jv]
    new Thread(new Runnable() {
      public void run() {
        String filename = url.getFile();
        filename = filename.substring(filename.lastIndexOf('/') + 1);
        try {
          File contribZip = File.createTempFile("download", filename);
          contribZip.setWritable(true); // necessary?

          try {
            download(url, null, contribZip, null);

            final LocalContribution contribution = ad.install(base, contribZip,
                                                        false, null);

            if (contribution != null) {
              try {
                // TODO: run this in SwingWorker done() [jv]
                EventQueue.invokeAndWait(new Runnable() {
                  @Override
                  public void run() {
                    listing.replaceContribution(ad, contribution);
                    base.refreshContribs(contribution.getType());
                    base.setUpdatesAvailable(listing.countUpdates(base));
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

            contribZip.delete();
            handleUpdateFailedMarkers(ad, filename.substring(0, filename.lastIndexOf('.')));

          } catch (Exception e) {
            String arg = "contrib.startup.errors.download_install";
            System.err.println(Language.interpolate(arg, ad.getName()));
          }
        } catch (IOException e) {
          String arg = "contrib.startup.errors.temp_dir";
          System.err.println(Language.interpolate(arg, ad.getName()));
        }
      }
    }, "Contribution Installer").start();
  }


  /**
   * After install, this function checks whether everything went properly.
   * If not, it adds a marker file so that the next time Processing is started,
   * installPreviouslyFailed() can install the contribution.
   * @param c the contribution just installed
   * @param filename name of the folder for the contribution
   */
  static private void handleUpdateFailedMarkers(final AvailableContribution c,
                                                String filename) {
    File typeFolder = c.getType().getSketchbookFolder();

    for (File contribDir : typeFolder.listFiles()) {
      if (contribDir.isDirectory()) {
        /*
        File[] contents = contribDir.listFiles(new FilenameFilter() {

          @Override
          public boolean accept(File dir, String file) {
            return file.equals(c.getType() + ".properties");
          }
        });
        if (contents.length > 0 && Util.readSettings(contents[0]).get("name").equals(c.getName())) {
          return;
        }
        */
        File propsFile = new File(contribDir, c.getType() + ".properties");
        if (propsFile.exists()) {
          StringDict props = Util.readSettings(propsFile);
          if (c.getName().equals(props.get("name"))) {
            return;
          }
        }
      }
    }

    try {
      new File(typeFolder, c.getName()).createNewFile();
    } catch (IOException e) {
      String arg = "contrib.startup.errors.new_marker";
      System.err.println(Language.interpolate(arg, c.getName()));
    }
  }


  /**
   * Blocking call to download and install a set of libraries. Used when a list
   * of libraries have to be installed while forcing the user to not modify
   * anything and providing feedback via the console status area, such as when
   * the user tries to run a sketch that imports uninstaled libraries.
   *
   * @param list The list of AvailableContributions to be downloaded and installed.
   */
  static public void downloadAndInstallOnImport(final Base base,
                                                final List<AvailableContribution> list) {
    // To avoid the user from modifying stuff, since this function is only
    // called during pre-processing
    Editor editor = base.getActiveEditor();
    editor.getTextArea().setEditable(false);
//    base.getActiveEditor().getConsole().clear();

    List<String> installedLibList = new ArrayList<>();

    // boolean variable to check if previous lib was installed successfully,
    // to give the user an idea about progress being made.
    boolean isPrevDone = false;

    for (final AvailableContribution contrib : list) {
      if (contrib.getType() != ContributionType.LIBRARY) {
        continue;
      }
      try {
        URL url = new URL(contrib.link);
        String filename = url.getFile();
        filename = filename.substring(filename.lastIndexOf('/') + 1);
        try {

          File contribZip = File.createTempFile("download", filename);
          contribZip.setWritable(true);

          try {
            // Use the console to let the user know what's happening
            // The slightly complex if-else is required to let the user know when
            // one install is completed and the next download has begun without
            // interfering with other status messages that may arise in the meanwhile
            String statusMsg = editor.getStatusMessage();
            if (isPrevDone) {
              String status = statusMsg + " "
                + Language.interpolate("contrib.import.progress.download", contrib.name);
              editor.statusNotice(status);
            } else {
              String arg = "contrib.import.progress.download";
              String status = Language.interpolate(arg, contrib.name);
              editor.statusNotice(status);
            }

            isPrevDone = false;

            download(url, null, contribZip, null);

            String arg = "contrib.import.progress.install";
            editor.statusNotice(Language.interpolate(arg,contrib.name));
            final LocalContribution contribution =
              contrib.install(base, contribZip, false, null);

            if (contribution != null) {
              try {
                EventQueue.invokeAndWait(new Runnable() {
                  @Override
                  public void run() {
                    listing.replaceContribution(contrib, contribution);
                    base.refreshContribs(contribution.getType());
                    base.setUpdatesAvailable(listing.countUpdates(base));
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

            contribZip.delete();

            installedLibList.add(contrib.name);
            isPrevDone = true;

            arg = "contrib.import.progress.done";
            editor.statusNotice(Language.interpolate(arg,contrib.name));

          } catch (Exception e) {
            String arg = "contrib.startup.errors.download_install";
            System.err.println(Language.interpolate(arg, contrib.getName()));
          }
        } catch (IOException e) {
          String arg = "contrib.startup.errors.temp_dir";
          System.err.println(Language.interpolate(arg,contrib.getName()));
        }
      } catch (MalformedURLException e1) {
        System.err.println(Language.interpolate("contrib.import.errors.link",
                                                contrib.getName()));
      }
    }
    editor.getTextArea().setEditable(true);
    editor.statusEmpty();
    System.out.println(Language.text("contrib.import.progress.final_list"));
    for (String l : installedLibList) {
      System.out.println("  * " + l);
    }
  }


  /*
  static void refreshInstalled(Editor e) {
    for (Editor ed : e.getBase().getEditors()) {
      ed.getMode().rebuildImportMenu();
      ed.getMode().rebuildExamplesFrame();
      ed.rebuildToolMenu();
      ed.rebuildModeMenu();
    }
  }
  */


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
  static private void cleanup(final Base base) throws Exception {
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
          // TODO: pls explain the sleep and why this runs on a worker thread,
          //   but a couple of lines above on EDT [jv]
          Thread.sleep(1000);
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
    String pattern = root.getName().substring(0, 4) + "\\d*" + "tmp";
    File[] possible = root.listFiles();
    if (possible != null) {
      for (File f : possible) {
        if (f.getName().matches(pattern)) {
          Util.removeDir(f);
        }
      }
    }
  }


  /**
   * Deletes all the modes/tools/libs that are flagged for removal.
   */
  static private void deleteFlagged(File root) throws Exception {
    File[] markedForDeletion = root.listFiles(new FileFilter() {
      public boolean accept(File folder) {
        return (folder.isDirectory() &&
                LocalContribution.isDeletionFlagged(folder));
      }
    });
    if (markedForDeletion != null) {
      for (File folder : markedForDeletion) {
        Util.removeDir(folder);
      }
    }
  }


  /**
   * Installs all the modes/tools whose installation failed during an
   * auto-update the previous time Processing was started up.
   */
  static private void installPreviouslyFailed(Base base, File root) throws Exception {
    File[] installList = root.listFiles(new FileFilter() {
      public boolean accept(File folder) {
        return folder.isFile();
      }
    });
    
    for (File file : installList) {
      for (AvailableContribution contrib : listing.advertisedContributions) {
        if (file.getName().equals(contrib.getName())) {
          file.delete();
          installOnStartUp(base, contrib);
          EventQueue.invokeAndWait(() -> {
            listing.replaceContribution(contrib, contrib);
          });
        }
      }
    }
  }


  /**
   * Updates all the flagged modes/tools.
   */
  static private void updateFlagged(Base base, File root) throws Exception {
    File[] markedForUpdate = root.listFiles(new FileFilter() {
      public boolean accept(File folder) {
        return (folder.isDirectory() &&
                LocalContribution.isUpdateFlagged(folder));
      }
    });

    List<String> updateContribsNames = new ArrayList<>();
    List<AvailableContribution> updateContribsList = new LinkedList<>();

    // TODO This is bad code... This root.getName() stuff to get the folder
    // type, plus "libraries.properties" (not the correct file name),
    // and I have no idea what "putting this here, in just in case" means.
    // Not sure the function here so I'm not fixing it at the moment,
    // but this whole function could use some cleaning. [fry 180105]

    String type = root.getName().substring(root.getName().lastIndexOf('/') + 1);
    String propFileName = null;

    if (type.equalsIgnoreCase("tools"))
      propFileName = "tool.properties";
    else if (type.equalsIgnoreCase("modes"))
      propFileName = "mode.properties";
    else if (type.equalsIgnoreCase("libraries")) //putting this here, just in case
      propFileName = "libraries.properties";

    for (File folder : markedForUpdate) {
      StringDict props = Util.readSettings(new File(folder, propFileName));
      updateContribsNames.add(props.get("name"));
      Util.removeDir(folder);
    }

    Iterator<AvailableContribution> iter = listing.advertisedContributions.iterator();
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
      listing.replaceContribution(contribToUpdate, contribToUpdate);
    }
  }


  static private void installOnStartUp(final Base base, final AvailableContribution availableContrib) {
    if (availableContrib.link == null) {
      Messages.showWarning(Language.interpolate("contrib.errors.update_on_restart_failed", availableContrib.getName()),
                           Language.text("contrib.unsupported_operating_system"));
    } else {
      try {
        URL downloadUrl = new URL(availableContrib.link);
        ContributionManager.downloadAndInstallOnStartup(base, downloadUrl, availableContrib);

      } catch (MalformedURLException e) {
        Messages.showWarning(Language.interpolate("contrib.errors.update_on_restart_failed", availableContrib.getName()),
                             Language.text("contrib.errors.malformed_url"), e);
      }
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


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  static ManagerFrame managerDialog;


  static public void init(Base base) throws Exception {
    listing = ContributionListing.getInstance(); // Moved here to make sure it runs on EDT [jv 170121]
    managerDialog = new ManagerFrame(base);
    cleanup(base);
  }


  /**
   * Show the Library installer window.
   */
  static public void openLibraries() {
    managerDialog.showFrame(ContributionType.LIBRARY);
  }


  /**
   * Show the Mode installer window.
   */
  static public void openModes() {
    managerDialog.showFrame(ContributionType.MODE);
  }


  /**
   * Show the Tool installer window.
   */
  static public void openTools() {
    managerDialog.showFrame(ContributionType.TOOL);
  }


  /**
   * Show the Examples installer window.
   */
  static public void openExamples() {
    managerDialog.showFrame(ContributionType.EXAMPLES);
  }


  /**
   * Open the updates panel.
   */
  static public void openUpdates() {
    managerDialog.showFrame(null);
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  static int getTypeIndex(ContributionType contributionType) {
    int index;
    if (contributionType == ContributionType.LIBRARY) {
      index = 0;
    } else if (contributionType == ContributionType.MODE) {
      index = 1;
    } else if (contributionType == ContributionType.TOOL) {
      index = 2;
    } else if (contributionType == ContributionType.EXAMPLES) {
      index = 3;
    } else {
      index = 4;
    }
    return index;
  }
}
