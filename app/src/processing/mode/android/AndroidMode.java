/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
 Part of the Processing project - http://processing.org

 Copyright (c) 2011 Ben Fry and Casey Reas

 This program is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License version 2
 as published by the Free Software Foundation.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software Foundation,
 Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package processing.mode.android;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import processing.app.*;
import processing.mode.java.JavaMode;


public class AndroidMode extends JavaMode {
  static private File coreZipLocation;

  
  public AndroidMode(Base base, File folder) {
    super(base, folder);
  }

  
  @Override
  public Editor createEditor(Base base, String path, int[] location) {
    return new AndroidEditor(base, path, location, this);
  }

  
  @Override
  public String getTitle() {
    return "Android";
  }
  
  
  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

  
  protected File getCoreZipLocation() {
    if (coreZipLocation == null) {
      // for debugging only, check to see if this is an svn checkout
      File debugFile = new File("../../../android/core.zip");
      if (!debugFile.exists() && Base.isMacOS()) {
        // current path might be inside Processing.app, so need to go much higher
        debugFile = new File("../../../../../../../android/core.zip");
      }
      if (debugFile.exists()) {
        System.out.println("Using version of core.zip from local SVN checkout.");
//        return debugFile;
        coreZipLocation = debugFile;
      }

      // otherwise do the usual
      //    return new File(base.getSketchbookFolder(), ANDROID_CORE_FILENAME);
      coreZipLocation = getContentFile("android-core.zip");
    }
    return coreZipLocation;
  }
  
  
  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .
  
  
  static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyMMdd.HHmm");
  
  
  static public String getDateStamp() {
    return dateFormat.format(new Date());
  }

  
  static public String getDateStamp(long stamp) {
    return dateFormat.format(new Date(stamp));
  }

  
  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

}