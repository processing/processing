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

package processing.app.contrib;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

public class FileDownloader {

  /**
   * Blocks until the file is downloaded or an error occurs. Returns true if the
   * file was successfully downloaded, false otherwise
   * 
   * @param source
   *          the URL of the file to donwload
   * @param dest
   *          the file on the local system where the file will be written. This
   *          must be a file (not a directory), and must already exist.
   * @param progressMonitor
   * @throws FileNotFoundException
   *           if an error occurred downloading the file
   */
  static public void downloadFile(URL source, File dest,
                                     ProgressMonitor progressMonitor) {

    try {
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
    } catch (IOException ioe) {
      progressMonitor.error(ioe);
    }
    
    progressMonitor.finished();
  }
  
}
