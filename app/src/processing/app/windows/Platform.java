/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2008 Ben Fry and Casey Reas

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

package processing.app.windows;

import java.io.File;

import processing.app.windows.Registry.REGISTRY_ROOT_KEY;


public class Platform {

  // looking for Documents and Settings/blah/Application Data/Processing
  public File getSettingsFolder() throws Exception {
    // HKEY_CURRENT_USER\Software\Microsoft
    //   \Windows\CurrentVersion\Explorer\Shell Folders
    // Value Name: AppData
    // Value Type: REG_SZ
    // Value Data: path

    String keyPath =
      "Software\\Microsoft\\Windows\\CurrentVersion" +
      "\\Explorer\\Shell Folders";
    String appDataPath = 
      Registry.getStringValue(REGISTRY_ROOT_KEY.CURRENT_USER, keyPath, "AppData");

    File dataFolder = new File(appDataPath, "Processing");
    return dataFolder;
  }


  // looking for Documents and Settings/blah/My Documents/Processing
  // (though using a reg key since it's different on other platforms)
  public File getDefaultSketchbookFolder() throws Exception {

    // http://support.microsoft.com/?kbid=221837&sd=RMVP
    // http://support.microsoft.com/kb/242557/en-us
    
    // The path to the My Documents folder is stored in the following 
    // registry key, where path is the complete path to your storage location
    
    // HKEY_CURRENT_USER\Software\Microsoft\Windows\CurrentVersion\Explorer\Shell Folders
    // Value Name: Personal
    // Value Type: REG_SZ
    // Value Data: path

    // in some instances, this may be overridden by a policy, in which case check:
    // HKEY_CURRENT_USER\Software\Microsoft\Windows\CurrentVersion\Explorer\User Shell Folders

    String keyPath =
      "Software\\Microsoft\\Windows\\CurrentVersion" +
      "\\Explorer\\Shell Folders";
    String personalPath = 
      Registry.getStringValue(REGISTRY_ROOT_KEY.CURRENT_USER, keyPath, "Personal");

    return new File(personalPath, "Processing");
  }
}