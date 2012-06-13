/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2011-12 Ben Fry and Casey Reas

  This library is free software; you can redistribute it and/or
  modify it under the terms of the GNU Lesser General Public
  License as published by the Free Software Foundation; either
  version 2.1 of the License, or (at your option) any later version.

  This library is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  Lesser General Public License for more details.

  You should have received a copy of the GNU Lesser General
  Public License along with this library; if not, write to the
  Free Software Foundation, Inc., 59 Temple Place, Suite 330,
  Boston, MA  02111-1307  USA
*/

package processing.video;

import java.net.URL;

import com.sun.jna.Platform;

class LibraryPath {
  // This method returns the folder inside which the gstreamer library folder
  // is located.
  String get() {
    URL url = this.getClass().getResource("LibraryPath.class");
    if (url != null) {
      // Convert URL to string, taking care of spaces represented by the "%20"
      // string.
      String path = url.toString().replace("%20", " ");
      int n0 = path.indexOf('/');

      int n1 = -1;
        
      if (Platform.isLinux()) {
        return "";
      } else {
        n1 = path.indexOf("video.jar");
        if (Platform.isWindows()) {
          // In Windows, path string starts with "jar file/C:/..."
          // so the substring up to the first / is removed.
          n0++;
        }
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