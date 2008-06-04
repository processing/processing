/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2007 Ben Fry and Casey Reas

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

package processing.app.macosx;

import java.io.FileNotFoundException;

import processing.app.Base;

import com.apple.eawt.*;
import com.apple.eio.FileManager;


/**
 * Deal with issues connected to thinking different.
 * Based on OSXAdapter.java from Apple DTS.
 * 
 * As of 0140, this code need not be built on platforms other than OS X, 
 * because it is hit by only two accessor methods in processing.app.Base that
 * are now called via reflection.  
 */
public class ThinkDifferent implements ApplicationListener {

  // pseudo-singleton model; no point in making multiple instances
  // of the EAWT application or our adapter
  private static ThinkDifferent adapter;
  // http://developer.apple.com/documentation/Java/Reference/1.4.2/appledoc/api/com/apple/eawt/Application.html
  private static Application application;

  // reference to the app where the existing quit, about, prefs code is
  private Base base;

  
  private ThinkDifferent(Base base) {
    this.base = base;
  }
  
  
  // implemented handler methods.  These are basically hooks into existing 
  // functionality from the main app, as if it came over from another platform.
  public void handleAbout(ApplicationEvent ae) {
    if (base != null) {
      ae.setHandled(true);
      base.handleAbout();
    } else {
      throw new IllegalStateException("handleAbout: Base instance detached from listener");
    }
  }
  
  
  public void handlePreferences(ApplicationEvent ae) {
    if (base != null) {
      base.handlePrefs();
      ae.setHandled(true);
    } else {
      throw new IllegalStateException("handlePreferences: Base instance detached from listener");
    }
  }
  
  
  public void handleOpenApplication(ApplicationEvent ae) {
  }


  public void handleOpenFile(ApplicationEvent ae) {
    String filename = ae.getFilename();
    base.handleOpen(filename);
    ae.setHandled(true);
  }


  public void handlePrintFile(ApplicationEvent ae) {
    // TODO implement os x print handler here (open app, call handlePrint, quit)
  }


  public void handleQuit(ApplicationEvent ae) {
    if (base != null) {
      /*  
      / You MUST setHandled(false) if you want to delay or cancel the quit.
      / This is important for cross-platform development -- have a universal quit
      / routine that chooses whether or not to quit, so the functionality is identical
      / on all platforms.  This example simply cancels the AppleEvent-based quit and
      / defers to that universal method.
      */
      boolean result = base.handleQuit();
      ae.setHandled(result);
    } else {
      throw new IllegalStateException("handleQuit: Base instance detached from listener");
    }
  }
  
  
  public void handleReOpenApplication(ApplicationEvent arg0) {
  }

  
  // The main entry-point for this functionality.  This is the only method
  // that needs to be called at runtime, and it can easily be done using
  // reflection (see Base.java) 
  public static void register(Base base) {
    if (application == null) {
      application = new com.apple.eawt.Application();
    }
    if (adapter == null) {
      adapter = new ThinkDifferent(base);
    }
    application.addApplicationListener(adapter);
    application.setEnabledAboutMenu(true);
    application.setEnabledPreferencesMenu(true);
  }  
  

  // Some of these are supposedly constants in com.apple.eio.FileManager, 
  // however they don't seem to link properly from Eclipse.
  
  static final int kDocumentsFolderType =
    ('d' << 24) | ('o' << 16) | ('c' << 8) | 's';
  //static final int kPreferencesFolderType =
  //  ('p' << 24) | ('r' << 16) | ('e' << 8) | 'f';
  static final int kDomainLibraryFolderType =
    ('d' << 24) | ('l' << 16) | ('i' << 8) | 'b';
  static final short kUserDomain = -32763;

  
  // apple java extensions documentation
  // http://developer.apple.com/documentation/Java/Reference/1.5.0
  //   /appledoc/api/com/apple/eio/FileManager.html
  
  // carbon folder constants
  // http://developer.apple.com/documentation/Carbon/Reference
  //   /Folder_Manager/folder_manager_ref/constant_6.html#/
  //   /apple_ref/doc/uid/TP30000238/C006889

  // additional information found int the local file:
  // /System/Library/Frameworks/CoreServices.framework
  //   /Versions/Current/Frameworks/CarbonCore.framework/Headers/


  static public String getLibraryFolder() {
    try {
      return FileManager.findFolder(kUserDomain, kDomainLibraryFolderType);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
    return null;
  }
  
  
  static public String getDocumentsFolder() {
    try {
      return FileManager.findFolder(kUserDomain, kDocumentsFolderType);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
    return null;
  }
}