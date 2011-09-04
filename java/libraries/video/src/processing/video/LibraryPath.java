/**
 * Part of the GSVideo library: http://gsvideo.sourceforge.net/
 * Copyright (c) 2008-11 Andres Colubri 
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation, version 2.1.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General
 * Public License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA  02111-1307  USA
 */

package processing.video;

import java.net.URL;

import com.sun.jna.Platform;

class LibraryPath {
  // This method returns the folder inside which the gstreamer library folder
  // should be located.
  String get() {
    URL url = this.getClass().getResource("LibraryPath.class");
    if (url != null) {
      // Convert URL to string, taking care of spaces represented by the "%20"
      // string.
      String path = url.toString().replace("%20", " ");
      int n0 = path.indexOf('/');

      int n1 = -1;
      if (Platform.isWindows()) {
        n1 = path.indexOf("/lib/video.jar"); // location of video.jar in
                                               // exported apps.
        if (n1 == -1)
          n1 = path.indexOf("/video.jar"); // location of video.jar in
                                             // library folder.

        // In Windows, path string starts with "jar file/C:/..."
        // so the substring up to the first / is removed.
        n0++;
      } else if (Platform.isMac()) {
        // In Mac, getting the index of video.jar is enough in the case of sketches running from the PDE
        // as well as exported applications.
        n1 = path.indexOf("video.jar");        
      } else if (Platform.isLinux()) {
        // TODO: what's up?
      }

      if ((-1 < n0) && (-1 < n1)) {
        return path.substring(n0, n1);
      } else {
        return "";
      }
    }
    return "";
  }
}