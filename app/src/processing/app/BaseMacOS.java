package processing.app;

import java.io.FileNotFoundException;

import com.apple.eawt.*;
import com.apple.eio.FileManager;


/**
 * Deal with issues connected to thinking different.
 * Based on OSXAdapter.java from Apple DTS.
 */
public class BaseMacOS implements ApplicationListener {

  // pseudo-singleton model; no point in making multiple instances
  // of the EAWT application or our adapter
  private static BaseMacOS adapter;
  // http://developer.apple.com/documentation/Java/Reference/1.4.2/appledoc/api/com/apple/eawt/Application.html
  private static Application application;

  // reference to the app where the existing quit, about, prefs code is
  private Base base;

  
  private BaseMacOS(Base base) {
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
      adapter = new BaseMacOS(base);
    }
    application.addApplicationListener(adapter);
    application.setEnabledAboutMenu(true);
    application.setEnabledPreferencesMenu(true);
  }  
  
  
  static final int kDocumentsFolderType =
    ('d' << 24) | ('o' << 16) | ('c' << 8) | 's';
  //static final int kPreferencesFolderType =
  //  ('p' << 24) | ('r' << 16) | ('e' << 8) | 'f';
  static final int kDomainLibraryFolderType =
    ('d' << 24) | ('l' << 16) | ('i' << 8) | 'b';
  static final short kUserDomain = -32763;

  
  // http://developer.apple.com/documentation/Java/Reference/1.4.2/appledoc/api/com/apple/eio/FileManager.html
  
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