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

import processing.app.Base;
import processing.app.Editor;


public class ContributionManager {
  static public final ContributionListing contribListing;

  static {
    contribListing = ContributionListing.getInstance();
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
            ContributionListing.download(url, contribZip, downloadProgress);
            
            if (!downloadProgress.isCanceled() && !downloadProgress.isError()) {
              installProgress.startTask("Installing...", ProgressMonitor.UNKNOWN);
              LocalContribution contribution = 
                ad.install(editor, contribZip, false, status);

              if (contribution != null) {
                contribListing.replaceContribution(ad, contribution);
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


  static public void refreshInstalled(Editor editor) {
    editor.getMode().rebuildImportMenu();
    editor.rebuildToolMenu();
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
  
  
  /** Called by Base to clean up entries previously marked for deletion. */
  static public void deleteFlagged() {
    deleteFlagged(Base.getSketchbookLibrariesFolder());
    deleteFlagged(Base.getSketchbookModesFolder());
    deleteFlagged(Base.getSketchbookToolsFolder());
  }

  
  static private void deleteFlagged(File root) {
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
}
