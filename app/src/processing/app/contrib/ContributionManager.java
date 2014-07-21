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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ArrayList;

import javax.swing.JProgressBar;

import processing.app.Base;
import processing.app.Editor;
import processing.app.Language;


public class ContributionManager {
  static public final ContributionListing contribListing;

  static {
    contribListing = ContributionListing.getInstance();
  }


  /**
   * Blocks until the file is downloaded or an error occurs. 
   * Returns true if the file was successfully downloaded, false otherwise.
   * 
   * @param source
   *          the URL of the file to download
   * @param dest
   *          the file on the local system where the file will be written. This
   *          must be a file (not a directory), and must already exist.
   * @param progress
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
  
      // TODO this is often -1, may need to set progress to indeterminate
      int fileSize = conn.getContentLength();
//      System.out.println("file size is " + fileSize);
      progress.startTask(Language.text("contributions.progress.downloading"), fileSize);
  
      InputStream in = conn.getInputStream();
      FileOutputStream out = new FileOutputStream(dest);
  
      byte[] b = new byte[8192];
      int amount;
      int total = 0;
      while (!progress.isCanceled() && (amount = in.read(b)) != -1) {
        out.write(b, 0, amount);
        total += amount;  
        progress.setProgress(total);
      }
      out.flush();
      out.close();
      success = true;
      
    } catch (SocketTimeoutException ste) {
      progress.error(ste);
      
    } catch (IOException ioe) {
      progress.error(ioe);
      ioe.printStackTrace();
    }
    progress.finished();
    return success;
  }


  /**
   * Blocks until the file is downloaded or an error occurs. 
   * Returns true if the file was successfully downloaded, false otherwise.
   * Used at startup for automatically downloading and installing.
   * 
   * @param source
   *          the URL of the file to download
   * @param dest
   *          the file on the local system where the file will be written. This
   *          must be a file (not a directory), and must already exist.
   */
  static boolean download(URL source, File dest) {
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
  
      InputStream in = conn.getInputStream();
      FileOutputStream out = new FileOutputStream(dest);
  
      byte[] b = new byte[8192];
      int amount;
      while ((amount = in.read(b)) != -1) {
        out.write(b, 0, amount);  
      }
      out.flush();
      out.close();
      success = true;
      
    } catch (SocketTimeoutException ste) {
      // When there's no internet... 
      // TODO: Will have to find a way to download later
    } catch (IOException ioe) {
      ioe.printStackTrace();
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
              installProgress.startTask("Installing...", ProgressMonitor.UNKNOWN);
              LocalContribution contribution = 
                ad.install(editor, contribZip, false, status);

              if (contribution != null) {
                contribListing.replaceContribution(ad, contribution);
                if (contribution.getType() == ContributionType.MODE) {
                  ArrayList<ModeContribution> contribModes = editor.getBase().getModeContribs();
                  contribModes.add((ModeContribution)contribution);
                }
                refreshInstalled(editor);
              }
              installProgress.finished();
            }
            contribZip.delete();

          } catch (Exception e) {
            e.printStackTrace();
            status.setErrorMessage("Error during download and install.");
          }
        } catch (IOException e) {
          status.setErrorMessage("Could not write to temporary directory.");
        }
      }
    }).start();
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
            download(url, contribZip);

            StatusPanel status = new StatusPanel();
            LocalContribution contribution = ad.installOnStartup(base,
                                                                 contribZip,
                                                                 status);

            if (contribution != null) {
              contribListing.replaceContribution(ad, contribution);
              if (contribution.getType() == ContributionType.MODE) {
                ArrayList<ModeContribution> contribModes = base
                  .getModeContribs();
                if (contribModes != null)
                  contribModes.add((ModeContribution) contribution);
              }
              if (base.getActiveEditor() != null)
                refreshInstalled(base.getActiveEditor());
            }

            System.out.println(status.getText());
//            if (contribution != null) {
//                contribListing.replaceContribution(ad, contribution);
//              }
            contribZip.delete();

          } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error during download and install of "
              + ad.getName());
          }
        } catch (IOException e) {
          System.err
            .println("Could not write to temporary directory during download and install of "
              + ad.getName());
        }
      }
    }).start();
  }


  static public void refreshInstalled(Editor e) {
    List<Editor> editor = e.getBase().getEditors();
    for (Editor ed : editor) {
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
  static public void cleanup(Base base) throws Exception {
    deleteTemp(Base.getSketchbookModesFolder());
    deleteTemp(Base.getSketchbookToolsFolder());
    
    deleteFlagged(Base.getSketchbookLibrariesFolder());
    deleteFlagged(Base.getSketchbookModesFolder());
    deleteFlagged(Base.getSketchbookToolsFolder());
    
    updateFlagged(base, Base.getSketchbookLibrariesFolder());
    updateFlagged(base, Base.getSketchbookModesFolder());
    updateFlagged(base, Base.getSketchbookToolsFolder());
    
    clearRestartFlags(Base.getSketchbookModesFolder());
    clearRestartFlags(Base.getSketchbookToolsFolder());
  }


  static private void deleteTemp(File root) {
    
    LinkedList<File> deleteList = new LinkedList<File>();
    
    for (File f : root.listFiles())
      if (f.getName().matches(root.getName().substring(0, 4) + "\\d*" + "tmp"))
        deleteList.add(f);
    
    Iterator<File> folderIter = deleteList.iterator();
    
    while(folderIter.hasNext()) {
      Base.removeDir(folderIter.next());
    }
  }

  
  static private void deleteFlagged(File root) throws Exception {
    File[] markedForDeletion = root.listFiles(new FileFilter() {
      public boolean accept(File folder) {
        return (folder.isDirectory() && 
                LocalContribution.isDeletionFlagged(folder));
      }
    });
    for (File folder : markedForDeletion) {
      Base.removeDir(folder);
    }
  }
  
  
  static private void updateFlagged(Base base, File root) throws Exception {
    File[] markedForUpdate = root.listFiles(new FileFilter() {
      public boolean accept(File folder) {
        return (folder.isDirectory() && 
                LocalContribution.isUpdateFlagged(folder));
      }
    });
    ArrayList<String> updateContribsNames = new ArrayList<String>();
    LinkedList<AvailableContribution> updateContribsList = new LinkedList<AvailableContribution>();
    for (File folder : markedForUpdate) {
      updateContribsNames.add(folder.getName());
      Base.removeDir(folder);
    }
    for (AvailableContribution availableContribs : contribListing.advertisedContributions) {
      if(updateContribsNames.contains(availableContribs.getName())) {
        updateContribsList.add(availableContribs);
      }
    }
    for (AvailableContribution contribToUpdate : updateContribsList) {
      installOnStartUp(base, contribToUpdate);
      contribListing.replaceContribution(contribToUpdate, contribToUpdate);
    }
  }
  
  
  static private void installOnStartUp(final Base base, final AvailableContribution availableContrib) {
    if (availableContrib.link == null) {
      Base.showWarning("Update on Restart of " + availableContrib.getName() + "failed",
                       "Your operating system "
                         + "doesn't appear to be supported. You should visit the "
                         + availableContrib.getType() + "'s library for more info.");
      return;
    }
    try {
      URL downloadUrl = new URL(availableContrib.link);
      
      ContributionManager.downloadAndInstallOnStartup(base, downloadUrl, availableContrib);
      
    } catch (MalformedURLException e) {
      Base.showWarning("Update on Restart of " + availableContrib.getName() + "failed",
                       ContributionListPanel.MALFORMED_URL_MESSAGE, e);
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
