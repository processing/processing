/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
 Part of the Processing project - http://processing.org

 Copyright (c) 2004-11 Ben Fry and Casey Reas
 Copyright (c) 2001-04 Massachusetts Institute of Technology

 This program is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 2 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software Foundation,
 Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package processing.app;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

public class FileDownloader implements Runnable {
  
  URL url;

  File dest;

  ProgressMonitor progressMonitor;

  Runnable post;

  // The output of the downloaded file, null if the download is still in
  // progress or the download failed.
  File libFile;

  /**
   * 
   * @param url
   *          URL of file to download
   * @param dest
   *          An existing file that is the destination for the download
   * @param progressMonitor
   * @param post
   *          object to run once download is complete, or null if nothing should
   *          be run. The run method will be called even if the download failed.
   */
  public FileDownloader(URL url, File dest, ProgressMonitor progressMonitor) {
    this.url = url;
    this.dest = dest;
    if (progressMonitor == null) {
      this.progressMonitor = new NullProgressMonitor();
    } else {
      this.progressMonitor = progressMonitor;
    }
    post = null;
    libFile = null;
  }
  
  public void setPostOperation(Runnable post) {
    this.post = post;
  }

  public void run() {
    try {
      if (downloadFile(url, dest, progressMonitor)) {
        libFile = dest;
      }
    } catch (IOException e) {
      Base.showWarning("Trouble downloading file",
                       "An error occured while downloading the library:\n"
                           + e.getMessage(), e);
    }

    if (post != null) {
      post.run();
    }
    
    progressMonitor.finished();
  }

  public File getFile() {
    return libFile;
  }

  /**
   * Returns true if the file was successfully downloaded, false otherwise
   * 
   * @param progressMonitor
   * @throws FileNotFoundException
   */
  protected boolean downloadFile(URL source, File dest,
                                 ProgressMonitor progressMonitor)
      throws IOException, FileNotFoundException {
    
    URLConnection urlConn = source.openConnection();
    urlConn.setConnectTimeout(1000);
    urlConn.setReadTimeout(5000);

    // String expectedType1 = "application/x-zip-compressed";
    // String expectedType2 = "application/zip";
    // String type = urlConn.getContentType();
    // if (expectedType1.equals(type) || expectedType2.equals(type)) {
    // }

    int fileSize = urlConn.getContentLength();
    progressMonitor.startTask("Downloading", fileSize);

    InputStream in = urlConn.getInputStream();
    FileOutputStream out = new FileOutputStream(dest);

    byte[] b = new byte[256];
    int bytesDownloaded = 0, len;
    while (!progressMonitor.isCanceled() && (len = in.read(b)) != -1) {
      out.write(b, 0, len);
      bytesDownloaded += len;

      progressMonitor.setProgress(bytesDownloaded);
    }
    out.close();

    if (!progressMonitor.isCanceled()) {
      return true;
    }

    return false;
  }

}

