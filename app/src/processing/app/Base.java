/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2004-07 Ben Fry and Casey Reas
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

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.lang.reflect.*;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.swing.*;

import com.ice.jni.registry.*;

import processing.app.debug.Compiler;
import processing.core.*;


/**
 * The base class for the main processing application.
 * Primary role of this class is for platform identification and
 * general interaction with the system (launching URLs, loading
 * files and images, etc) that comes from that.
 */
public class Base {
  static final int VERSION = 136;
  static final String VERSION_NAME = "0136 Beta";

  // set to true after the first time it's built.
  // so that the errors while building don't show up again.
  boolean builtOnce;

  // these are static because they're used by Sketch
  static File examplesFolder;
  static String examplesPath;  // canonical path (for comparison)

  static File librariesFolder;
  static String librariesPath;

  // maps imported packages to their library folder
  static Hashtable importToLibraryTable = new Hashtable();

  // classpath for all known libraries for p5
  // (both those in the p5/libs folder and those with lib subfolders
  // found in the sketchbook)
  static public String librariesClassPath;

  // Location for untitled items
  static File untitledFolder;

  // p5 icon for the window
  static Image icon;

  int editorCount;
  Editor[] editors;
  Editor activeEditor;

  int nextEditorX;
  int nextEditorY;


  static public void main(String args[]) {

    // make sure that this is running on java 1.4
    if (PApplet.javaVersion < 1.4f) {
      //System.err.println("no way man");
      Base.showError("Need to install Java 1.4",
                     "This version of Processing requires    \n" +
                     "Java 1.4 or later to run properly.\n" +
                     "Please visit java.com to upgrade.", null);
    }

    // Set the look and feel before opening the window
    try {
      if (Base.isMacOS()) {
        // Use the Quaqua L & F on OS X to make JFileChooser less awful
        UIManager.setLookAndFeel("ch.randelshofer.quaqua.QuaquaLookAndFeel");
        // undo quaqua trying to fix the margins, since we've already
        // hacked that in, bit by bit, over the years
        UIManager.put("Component.visualMargin", new Insets(1, 1, 1, 1));

      } else if (Base.isLinux()) {
        // Linux is by default even uglier than metal (Motif?).
        // Actually, i'm using native menus, so they're even uglier
        // and Motif-looking (Lesstif?). Ick. Need to fix this.
        //String lfname = UIManager.getCrossPlatformLookAndFeelClassName();
        //UIManager.setLookAndFeel(lfname);

        // For 0120, trying out the gtk+ look and feel as the default.
        // This is available in Java 1.4.2 and later, and it can't possibly
        // be any worse than Metal. (Ocean might also work, but that's for
        // Java 1.5, and we aren't going there yet)
        UIManager.setLookAndFeel("com.sun.java.swing.plaf.gtk.GTKLookAndFeel");

      } else {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
      }
    } catch (Exception e) {
      System.err.println("Non-fatal error while setting the Look & Feel.");
      System.err.println("The error message follows, however Processing should run fine.");
      e.printStackTrace();
    }

    // Use native popups so they don't look so crappy on osx
    JPopupMenu.setDefaultLightWeightPopupEnabled(false);

    // Create a location for untitled sketches
    untitledFolder = createTempFolder("untitled");
    untitledFolder.deleteOnExit();

    // run static initialization that grabs all the prefs
    try {
      Preferences.init();
    } catch (Exception e) {
      e.printStackTrace();
    }

    /*Base base =*/ new Base(args);
  }


  public Base(String[] args) {
    // #@$*(@#$ apple.. always gotta think different
    if (PApplet.platform == PConstants.MACOSX) {
      registerMacOS();
    }

    // Get paths for the libraries and examples in the Processing folder
    examplesFolder = new File(System.getProperty("user.dir"), "examples");
    examplesPath = examplesFolder.getAbsolutePath();
    librariesFolder = new File(System.getProperty("user.dir"), "libraries");
    librariesPath = librariesFolder.getAbsolutePath();

    // Get the sketchbook path, and make sure it's set properly
    String sketchbookPath = Preferences.get("sketchbook.path");

    // If a value is at least set, first check to see if the folder exists.
    // If it doesn't, warn the user that the sketchbook folder is being reset.
    if (sketchbookPath != null) {
      File skechbookFolder = new File(sketchbookPath);
      if (!skechbookFolder.exists()) {
        Base.showWarning("Sketchbook folder disappeared",
                         "The sketchbook folder no longer exists,\n" +
                         "so a new sketchbook will be created in the\n" +
                         "default location.", null);
        sketchbookPath = null;
      }
    }

    // If not path is set, get the default sketchbook folder for this platform
    if (sketchbookPath == null) {
      File defaultFolder = Base.getDefaultSketchbookFolder();
      Preferences.set("sketchbook.path", defaultFolder.getAbsolutePath());
      if (!defaultFolder.exists()) {
        defaultFolder.mkdirs();
      }
    }

    // Check if there were previously opened sketches to be restored
    boolean opened = restoreSketches();

    // Check if any files were passed in on the command line
    for (int i = 0; i < args.length; i++) {
      if (handleOpen(args[i]) != null) {
        opened = true;
      }
    }

    // Create a new empty window (will be replaced with any files to be opened)
    if (!opened) {
      handleNew();
    }

    // check for updates
    if (Preferences.getBoolean("update.check")) {
      new UpdateCheck(this);
    }
  }


  protected void registerMacOS() {
    try {
      String name = "processing.app.BaseMacOS";
      Class osxAdapter = ClassLoader.getSystemClassLoader().loadClass(name);

      Class[] defArgs = { Base.class };
      Method registerMethod = osxAdapter.getDeclaredMethod("register", defArgs);
      if (registerMethod != null) {
        Object[] args = { this };
        registerMethod.invoke(osxAdapter, args);
      }
    } catch (NoClassDefFoundError e) {
      // This will be thrown first if the OSXAdapter is loaded on a system without the EAWT
      // because OSXAdapter extends ApplicationAdapter in its def
      System.err.println("This version of Mac OS X does not support the Apple EAWT." +
                         "Application Menu handling has been disabled (" + e + ")");

    } catch (ClassNotFoundException e) {
      // This shouldn't be reached; if there's a problem with the OSXAdapter
      // we should get the above NoClassDefFoundError first.
      System.err.println("This version of Mac OS X does not support the Apple EAWT. " +
                         "Application Menu handling has been disabled (" + e + ")");
    } catch (Exception e) {
      System.err.println("Exception while loading BaseOSX:");
      e.printStackTrace();
    }
  }


  /**
   * Post-constructor setup for the editor area. Loads the last
   * sketch that was used (if any), and restores other Editor settings.
   * The complement to "storePreferences", this is called when the
   * application is first launched.
   */
  public boolean restoreSketches() {
    // figure out window placement

    Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
    boolean windowPositionValid = true;

    if (Preferences.get("last.screen.height") != null) {
      // if screen size has changed, the window coordinates no longer
      // make sense, so don't use them unless they're identical
      int screenW = Preferences.getInteger("last.screen.width");
      int screenH = Preferences.getInteger("last.screen.height");

      if ((screen.width != screenW) || (screen.height != screenH)) {
        windowPositionValid = false;
      }
      /*
      int windowX = Preferences.getInteger("last.window.x");
      int windowY = Preferences.getInteger("last.window.y");
      if ((windowX < 0) || (windowY < 0) ||
          (windowX > screenW) || (windowY > screenH)) {
        windowPositionValid = false;
      }
      */
    } else {
      windowPositionValid = false;
    }

    // Iterate through all sketches that were open last time p5 was running.
    // If !windowPositionValid, then ignore the coordinates found for each.

    // Save the sketch path and window placement for each open sketch
    int count = Preferences.getInteger("last.sketch.count");
    int opened = 0;
    for (int i = 0; i < count; i++) {
      String path = Preferences.get("last.sketch" + i + ".path");
      int[] location;
      if (windowPositionValid) {
        String locationStr = Preferences.get("last.sketch" + i + ".location");
        location = PApplet.parseInt(PApplet.split(locationStr, ','));
      } else {
        location = nextEditorLocation();
      }
      // If file did not exist, null will be returned for the Editor
      if (handleOpen(path, location) != null) {
        opened++;
      }
    }
    return (opened > 0);
  }


  /**
   * Store list of sketches that are currently open.
   * Called when the application is quitting and documents are still open.
   */
  public void storeSketches() {
    // Save the width and height of the screen
    Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
    Preferences.setInteger("last.screen.width", screen.width);
    Preferences.setInteger("last.screen.height", screen.height);

    String untitledPath = untitledFolder.getAbsolutePath();

    // Save the sketch path and window placement for each open sketch
    Preferences.setInteger("last.sketch.count", editorCount);
    //System.out.println("saving sketch count " + editorCount);
    for (int i = 0; i < editorCount; i++) {
      String path = editors[i].sketch.getMainFilePath();
      if (path.startsWith(untitledPath)) {
        path = "";  // this will prevent it from opening
      }
      Preferences.set("last.sketch" + i + ".path", path);

      int[] location = editors[i].getPlacement();
      String locationStr = PApplet.join(PApplet.str(location), ",");
      Preferences.set("last.sketch" + i + ".location", locationStr);
    }
  }


  // If a sketch is untitled on quit, may need to store the new name
  // rather than the location from the temp folder.
  protected void storeSketchPath(Editor editor, int index) {
    String path = editor.sketch.getMainFilePath();
    String untitledPath = untitledFolder.getAbsolutePath();
    if (path.startsWith(untitledPath)) {
      path = "";
    }
    Preferences.set("last.sketch" + index + ".path", path);
  }


  /*
  public void storeSketch(Editor editor) {
    int index = -1;
    for (int i = 0; i < editorCount; i++) {
      if (editors[i] == editor) {
        index = i;
        break;
      }
    }
    if (index == -1) {
      System.err.println("Problem storing sketch " + editor.sketch.name);
    } else {
      String path = editor.sketch.getMainFilePath();
      Preferences.set("last.sketch" + index + ".path", path);
    }
  }
  */


  // .................................................................


  static public File getSketchbookFolder() {
    return new File(Preferences.get("sketchbook.path"));
  }


  // Because of variations in native windowing systems, no guarantees about
  // changes to the focused and active Windows can be made. Developers must
  // never assume that this Window is the focused or active Window until this
  // Window receives a WINDOW_GAINED_FOCUS or WINDOW_ACTIVATED event.
  public void handleActivated(Editor whichEditor) {
    activeEditor = whichEditor;

    // set the current window to be the console that's getting output
    EditorConsole.setEditor(activeEditor);
  }


  protected int[] nextEditorLocation() {
    int[] location;

    if (activeEditor == null) {
      // If no current active editor, use default placement
      location = new int[5];

      // Get default window width and height
      location[2] = Preferences.getInteger("default.window.width");
      location[3] = Preferences.getInteger("default.window.height");

      Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
      location[0] = (screen.width - location[2]) / 2;
      location[1] = (screen.height - location[3]) / 2;

    } else {
      // With a currently active editor, open the new window
      // using the same dimensions, but offset slightly.
      location = activeEditor.getPlacement();
      location[0] += 50;
      location[1] += 50;
    }
    return location;
  }


  // .................................................................


  /*
  public void handleNew(boolean shiftDown) {
    // buttons.activate(EditorButtons.NEW);

    boolean prompt = Preferences.getBoolean("sketchbook.prompt");
    if (shiftDown) prompt = !prompt; // reverse behavior if shift is down

    // no sketch has been started, don't prompt for the name if it's
    // starting up, just make the farker. otherwise if the person hits
    // 'cancel' i'd have to add a thing to make p5 quit, which is silly.
    // instead give them an empty sketch, and they can look at examples.
    // i hate it when imovie makes you start with that goofy dialog box.
    // unless, ermm, they user tested it and people preferred that as
    // a way to get started. shite. now i hate myself.
    //if (disablePrompt) prompt = false;

    try {
      if (prompt) {
        handleNewPrompt(activeEditor);
      } else {
        handleNewUntitled();
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }


  public void handleNewPrompt(JFrame parent) throws IOException {
    File newbieDir = null;
    String newbieName = null;

    // prompt for the filename and location for the new sketch
    FileDialog fd = new FileDialog(parent,
                                   "Create sketch folder named:",
                                   FileDialog.SAVE);
    //fd.setDirectory(getSketchbookPath());
    fd.setVisible(true);

    String newbieParentDir = fd.getDirectory();
    newbieName = fd.getFile();
    if (newbieName != null) {
      newbieName = Sketch.sanitizeName(newbieName);
      newbieDir = new File(newbieParentDir, newbieName);

      // Make the directory for the new sketch
      newbieDir.mkdirs();

      // Make an empty pde file
      File newbieFile = new File(newbieDir, newbieName + ".pde");
      new FileOutputStream(newbieFile);  // create the file

      handleOpen(newbieFile.getAbsolutePath());
    }
  }
  */


  /**
   * Handle creating a sketch folder, return its base .pde file
   * or null if the operation was canceled.
   * @param shift whether shift is pressed, which will invert prompt setting
   * @param noPrompt disable prompt, no matter the setting
   */
  protected String createNewUntitled() throws IOException {
    File newbieDir = null;
    String newbieName = null;

    // In 0126, untitled sketches will begin in the temp folder,
    // and then moved to a new location because Save will default to Save As.
    File sketchbookDir = getSketchbookFolder();
    File newbieParentDir = untitledFolder;

    // Use a generic name like sketch_031008a, the date plus a char
    int index = 0;
    SimpleDateFormat formatter = new SimpleDateFormat("yyMMdd");
    String purty = formatter.format(new Date());
    do {
      newbieName = "sketch_" + purty + ((char) ('a' + index));
      newbieDir = new File(newbieParentDir, newbieName);
      index++;
      // Make sure it's not in the temp folder *and* it's not in the sketchbook
    } while (newbieDir.exists() || new File(sketchbookDir, newbieName).exists());

    // Make the directory for the new sketch
    newbieDir.mkdirs();

    // Make an empty pde file
    File newbieFile = new File(newbieDir, newbieName + ".pde");
    new FileOutputStream(newbieFile);  // create the file
    return newbieFile.getAbsolutePath();
  }


  public void handleNew() {
    try {
      String path = createNewUntitled();
      Editor editor = handleOpen(path);
      editor.untitled = true;

    } catch (IOException e) {
      if (activeEditor != null) {
        activeEditor.error(e);
      }
    }
  }


  public void handleNewReplace() {
    if (!activeEditor.checkModified(false)) {
      return;  // sketch was modified, and user canceled
    }
    // Close the running window, avoid window boogers with multiple sketches
    activeEditor.closeRunner();
    // Actually replace things
    handleNewReplaceImpl();
  }


  protected void handleNewReplaceImpl() {
    try {
      String path = createNewUntitled();
      activeEditor.handleOpenInternal(path);
      activeEditor.untitled = true;

    } catch (IOException e) {
      if (activeEditor != null) {
        activeEditor.error(e);
      }
    }
  }


//  protected Editor handleNewInternal(File newbieDir, String newbieName)
//  throws FileNotFoundException {
//    // Make the directory for the new sketch
//    newbieDir.mkdirs();
//
//    // Make an empty pde file
//    File newbieFile = new File(newbieDir, newbieName + ".pde");
//    new FileOutputStream(newbieFile);  // create the file
//
//    // TODO For 0126, need to check if this is the only way that the doc is
//    // getting associated, and if so, have we removed the connection between
//    // .pde files and Processing.app
//
//    //  Disabling this starting in 0125... There's no need for it,
//    // and it's likely to cause more trouble than necessary by
//    // leaving around little ._ boogers.
//
//    // this wouldn't be needed if i could figure out how to
//    // associate document icons via a dot-extension/mime-type scenario
//    // help me steve jobs, you're my only hope.
//
//    // jdk13 on osx, or jdk11
//    // though apparently still available for 1.4
//    /*
//    if (Base.isMacOS()) {
//      MRJFileUtils.setFileTypeAndCreator(newbieFile,
//                                         MRJOSType.kTypeTEXT,
//                                         new MRJOSType("Pde1"));
//      // thank you apple, for changing this @#$)(*
//      //com.apple.eio.setFileTypeAndCreator(String filename, int, int)
//    }
//    */
//    return handleOpen(newbieFile.getAbsolutePath());
//  }


  /*
  public String handleOpenPrompt(JFrame editor) {
    // The file chooser in Swing is ass ugly, so we use the
    // native (AWT peered) dialogs where possible
    FileDialog fd = new FileDialog(editor,
                                   "Open a Processing sketch...",
                                   FileDialog.LOAD);

    // gimme some money
    fd.setVisible(true);

    // what in the hell yu want, boy?
    String directory = fd.getDirectory();
    String filename = fd.getFile();

    // user cancelled selection
    if (filename == null) return null;

    // this may come in handy sometime
    //handleOpenDirectory = directory;

    File selection = new File(directory, filename);
    return selection.getAbsolutePath();
  }
  */


  public void handleOpenReplace(String path) {
    if (!activeEditor.checkModified(false)) {
      return;  // sketch was modified, and user canceled
    }
    // Close the running window, avoid window boogers with multiple sketches
    activeEditor.closeRunner();

    boolean loaded = activeEditor.handleOpenInternal(path);
    if (!loaded) {
      // replace the document without checking if that's ok
      handleNewReplaceImpl();
    }
  }


  public void handleOpenPrompt() {
    // get the frontmost window frame for placing file dialog
    FileDialog fd = new FileDialog(activeEditor,
                                   "Open a Processing sketch...",
                                   FileDialog.LOAD);
    // This was annoying people, so disabled it in 0125.
    //fd.setDirectory(Preferences.get("sketchbook.path"));
    //fd.setDirectory(getSketchbookPath());

    // Only show .pde files as eligible bachelors
    fd.setFilenameFilter(new FilenameFilter() {
        public boolean accept(File dir, String name) {
          // TODO this doesn't seem to ever be used. AWESOME.
          //System.out.println("check filter on " + dir + " " + name);
          return name.toLowerCase().endsWith(".pde");
        }
      });

    fd.setVisible(true);

    String directory = fd.getDirectory();
    String filename = fd.getFile();

    // User canceled selection
    if (filename == null) return;

    File inputFile = new File(directory, filename);
    handleOpen(inputFile.getAbsolutePath());
  }


  /*
  public void handleOpen(File file) {
    handleOpen(file.getAbsolutePath());
  }
  */


  /**
   * @param path Path to the .pde file for the sketch in question
   * @return the Editor object, so that properties (like 'untitled')
   *         can be set by the caller
   */
  public Editor handleOpen(String path) {
    return handleOpen(path, nextEditorLocation());
  }


  public Editor handleOpen(String path, int[] location) {
    File file = new File(path);
    if (!file.exists()) return null;

    // Cycle through open windows to make sure that it's not already open.
    for (int i = 0; i < editorCount; i++) {
      if (editors[i].sketch.path.equals(path)) {
        editors[i].toFront();
        return editors[i];
      }
    }

    // If the active editor window is an untitled, and un-modified document,
    // just replace it with the file that's being opened.
//    if (activeEditor != null) {
//      Sketch activeSketch = activeEditor.sketch;
//      if (activeSketch.isUntitled() && !activeSketch.isModified()) {
//        // if it's an untitled, unmodified document, it can be replaced.
//        // except in cases where a second blank window is being opened.
//        if (!path.startsWith(untitledFolder.getAbsolutePath())) {
//          activeEditor.handleOpenUnchecked(path, 0, 0, 0, 0);
//          return activeEditor;
//        }
//      }
//    }

    Editor editor = new Editor(this, path, location);

    // Make sure that the sketch actually loaded
    if (editor.sketch == null) {
      return null;  // Just walk away quietly
    }

    if (editors == null) {
      editors = new Editor[5];
    }
    if (editorCount == editors.length) {
      editors = (Editor[]) PApplet.expand(editors);
    }
    editors[editorCount++] = editor;

//    if (markedForClose != null) {
//      Point p = markedForClose.getLocation();
//      handleClose(markedForClose, false);
//      // open the new window in
//      editor.setLocation(p);
//    }

    // now that we're ready, show the window
    // (don't do earlier, cuz we might move it based on a window being closed)
    editor.setVisible(true);

    return editor;
  }


  public boolean handleClose(Editor editor, boolean quitting) {
    // Check if modified
    if (!editor.checkModified(quitting)) {  //false)) {  // was false in 0126
      return false;
    }

    // If quitting, this is all that needs to be done
    if (quitting) {
      return true;
    }

    // Close the running window, avoid window boogers with multiple sketches
    editor.closeRunner();

    if (editorCount == 1) {
      if (Preferences.getBoolean("sketchbook.closing_last_window_quits")) {
        // This will store the sketch count as zero
        editorCount = 0;
        storeSketches();

        // Save out the current prefs state
        Preferences.save();

        // Clean out empty sketches
        //Base.cleanSketchbook();

        // Since this wasn't an actual Quit event,
        // System.exit() needs to be called for Mac OS X.
        //if (PApplet.platform == PConstants.MACOSX) {
        System.exit(0);

      } else {
        try {
          // open an untitled document in the last remaining window
          String path = createNewUntitled();
          activeEditor.handleOpenInternal(path);
          return true;  // or false?

        } catch (IOException e) {
          e.printStackTrace();
          return false;
        }
      }
    } else {
      // More than one editor window open,
      // proceed with closing the current window.
      editor.setVisible(false);
      editor.dispose();

      for (int i = 0; i < editorCount; i++) {
        if (editor == editors[i]) {
          for (int j = i; j < editorCount-1; j++) {
            editors[j] = editors[j+1];
          }
          editorCount--;
          // Set to null so that garbage collection occurs
          editors[editorCount] = null;
        }
      }
    }
    return true;

    /*
    // If not canceled, check if this was the last open window.
    // If it was the last, could either do a new untitled window,
    // or could just quit the application.
    if (!quitting && (editorCount == 0)) {
      // This will store the sketch count as zero
      storeSketches();

      // Save out the current prefs state
      Preferences.save();

      // Clean out empty sketches
      Base.cleanSketchbook();
      // can't do handleQuit(), would do weird recursive thing
      //handleQuit();

      // Since this wasn't an actual Quit event,
      // System.exit() needs to be called for Mac OS X.
      //if (PApplet.platform == PConstants.MACOSX) {
      System.exit(0);
      //}
    }
    return true;
    */
  }


  public boolean handleQuit() {
    // If quit is canceled, this will be replaced anyway
    // by a later handleQuit() that is not canceled.
    storeSketches();

    boolean canceled = false;
    for (int i = 0; i < editorCount; i++) {
      Editor editor = editors[i];
      if (!handleClose(editor, true)) {
        canceled = true;
        break;
      } else {
        // Update to the new/final sketch path for this fella
        storeSketchPath(editor, i);
      }
    }
    // make sure running sketches close before quitting
    for (int i = 0; i < editorCount; i++) {
      editors[i].closeRunner();
    }
    if (!canceled) {
      // Clean out empty sketches
      //Base.cleanSketchbook();

      // Save out the current prefs state
      Preferences.save();
      //console.handleQuit();

      if (PApplet.platform != PConstants.MACOSX) {
        // If this was fired from the menu or an AppleEvent (the Finder),
        // then Mac OS X will send the terminate signal itself.
        System.exit(0);
      }
    }
    return !canceled;
  }


  // .................................................................


  /**
   * Asynchronous version of menu rebuild to be used on save and rename
   * to prevent the interface from locking up until the menus are done.
   */
  public void rebuildSketchbookMenu() {
    //System.out.println("async enter");
    //new Exception().printStackTrace();
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        //System.out.println("starting rebuild");
        rebuildSketchbookMenu(Editor.sketchbookMenu);
        rebuildToolbarMenu(Editor.toolbarMenu);
        //System.out.println("done with rebuild");
      }
    });
    //System.out.println("async exit");
  }


  /**
   * Rebuild the menu full of sketches based on the
   * contents of the sketchbook.
   *
   * Creates a separate JMenu object for the popup,
   * because it seems that after calling "getPopupMenu"
   * the menu will disappear from its original location.
   */
  /*
  public void rebuildMenus() {
    //EditorConsole.systemOut.println("rebuilding menus");
    try {
      // rebuild file/open and the toolbar popup menus
      buildMenu(openMenu);
      builtOnce = true;  // disable error messages while loading
      buildMenu(toolbarMenu);

      // rebuild the "import library" menu
      librariesClassPath = "";
      importMenu.removeAll();
      if (addLibraries(importMenu, new File(getSketchbookPath()))) {
        importMenu.addSeparator();
      }
      // removed for rev 0125 because not used
      //if (addLibraries(importMenu, examplesFolder)) {
      //  importMenu.addSeparator();
      //}
      addLibraries(importMenu, librariesFolder);
      //System.out.println("libraries cp is now " + librariesClassPath);

    } catch (IOException e) {
      Base.showWarning("Problem while building sketchbook menu",
                       "There was a problem with building the\n" +
                       "sketchbook menu. Things might get a little\n" +
                       "kooky around here.", e);
    }
    //EditorConsole.systemOut.println("done rebuilding menus");
  }
  */


  /*
  public JPopupMenu createPopup() {
    JMenu menu = new JMenu();
    rebuildPopup(menu);
    return menu.getPopupMenu();
  }
  */


  public void rebuildToolbarMenu(JMenu menu) {
    JMenuItem item;
    menu.removeAll();

    //System.out.println("rebuilding toolbar menu");
    // Add the single "Open" item
    item = Editor.newJMenuItem("Open...", 'O', false);
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          handleOpenPrompt();
        }
      });
    menu.add(item);
    menu.addSeparator();

    // Add a list of all sketches and subfolders
    try {
      boolean sketches = addSketches(menu, getSketchbookFolder(), true);
      if (sketches) menu.addSeparator();
    } catch (IOException e) {
      e.printStackTrace();
    }

    //System.out.println("rebuilding examples menu");
    // Add each of the subfolders of examples directly to the menu
    try {
      addSketches(menu, examplesFolder, true);
//      String[] subfolders = examplesFolder.list();
//      for (int i = 0; i < subfolders.length; i++) {
//        if (!subfolders[i].startsWith(".")) {
//          File dir = new File(examplesFolder, subfolders[i]);
//          if (dir.isDirectory()) {
//            addSketches(menu, dir);
//          }
//        }
//      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }


  public void rebuildSketchbookMenu(JMenu menu) {
    //System.out.println("rebuilding sketchbook menu");
    //new Exception().printStackTrace();
    try {
      menu.removeAll();
      addSketches(menu, getSketchbookFolder(), false);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }


  public void rebuildImportMenu(JMenu importMenu) {
    //System.out.println("rebuilding import menu");
    importMenu.removeAll();

    // Add from the "libraries" subfolder in the Processing directory
    try {
      boolean found = addLibraries(importMenu, getSketchbookFolder());
      if (found) importMenu.addSeparator();
    } catch (IOException e) {
      e.printStackTrace();
    }
    // Add libraries found in the sketchbook folder
    try {
      addLibraries(importMenu, librariesFolder);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }


  public void rebuildExamplesMenu(JMenu menu) {
    //System.out.println("rebuilding examples menu");

    try {
      menu.removeAll();
      addSketches(menu, examplesFolder, false);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }


  /**
   * Scan a folder recursively, and add any sketches found to the menu
   * specified. Set the openReplaces parameter to true when opening the sketch
   * should replace the sketch in the current window, or false when the
   * sketch should open in a new window.
   */
  protected boolean addSketches(JMenu menu, File folder,
                                final boolean openReplaces) throws IOException {
    // skip .DS_Store files, etc (this shouldn't actually be necessary)
    if (!folder.isDirectory()) return false;

    String[] list = folder.list();
    // If a bad folder or unreadable or whatever, this will come back null
    if (list == null) return false;

    // Alphabetize list, since it's not always alpha order
    Arrays.sort(list, String.CASE_INSENSITIVE_ORDER);
    //processing.core.PApplet.println("adding sketches " + folder.getAbsolutePath());
    //PApplet.println(list);

    ActionListener listener = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
//          Component source = (Component) e.getSource();
//          Component parent = source.getParent();
//          if (parent.isValid()) {
//            // parent component (the menu) will be valid when it's a popup
          if (openReplaces) {
            handleOpenReplace(e.getActionCommand());
          } else {
            handleOpen(e.getActionCommand());
          }
        }
      };

    boolean ifound = false;

    for (int i = 0; i < list.length; i++) {
      if ((list[i].charAt(0) == '.') ||
          list[i].equals("CVS")) continue;

      File subfolder = new File(folder, list[i]);
      if (!subfolder.isDirectory()) continue;

      File entry = new File(subfolder, list[i] + ".pde");
      // if a .pde file of the same prefix as the folder exists..
      if (entry.exists()) {
        //String sanityCheck = sanitizedName(list[i]);
        //if (!sanityCheck.equals(list[i])) {
        if (!Sketch.isSanitaryName(list[i])) {
          if (!builtOnce) {
            String complaining =
              "The sketch \"" + list[i] + "\" cannot be used.\n" +
              "Sketch names must contain only basic letters and numbers\n" +
              "(ASCII-only with no spaces, " +
              "and it cannot start with a number).\n" +
              "To get rid of this message, remove the sketch from\n" +
              entry.getAbsolutePath();
            Base.showMessage("Ignoring sketch with bad name", complaining);
          }
          continue;
        }

        JMenuItem item = new JMenuItem(list[i]);
        item.addActionListener(listener);
        item.setActionCommand(entry.getAbsolutePath());
        menu.add(item);
        ifound = true;

      } else {
        // not a sketch folder, but maybe a subfolder containing sketches
        JMenu submenu = new JMenu(list[i]);
        // needs to be separate var
        // otherwise would set ifound to false
        boolean found = addSketches(submenu, subfolder, openReplaces); //, false);
        if (found) {
          menu.add(submenu);
          ifound = true;
        }
      }
    }
    return ifound;  // actually ignored, but..
  }


  protected boolean addLibraries(JMenu menu, File folder) throws IOException {
    // skip .DS_Store files, etc
    if (!folder.isDirectory()) return false;

    String list[] = folder.list();
    // if a bad folder or something like that, this might come back null
    if (list == null) return false;

    // alphabetize list, since it's not always alpha order
    // replaced hella slow bubble sort with this feller for 0093
    Arrays.sort(list, String.CASE_INSENSITIVE_ORDER);

    ActionListener listener = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          // TODO ohmigod that's nassssteee!
          activeEditor.sketch.importLibrary(e.getActionCommand());
        }
      };

    boolean ifound = false;

    for (int i = 0; i < list.length; i++) {
      if ((list[i].charAt(0) == '.') ||
          list[i].equals("CVS")) continue;

      File subfolder = new File(folder, list[i]);
      if (!subfolder.isDirectory()) continue;

      File exported = new File(subfolder, "library");
      File entry = new File(exported, list[i] + ".jar");
      // If a .jar file of the same prefix as the folder exists
      // inside the 'library' subfolder of the sketch
      if (entry.exists()) {
        String sanityCheck = Sketch.sanitizedName(list[i]);
        if (!sanityCheck.equals(list[i])) {
          String mess =
            "The library \"" + list[i] + "\" cannot be used.\n" +
            "Library names must contain only basic letters and numbers.\n" +
            "(ascii only and no spaces, and it cannot start with a number)";
          Base.showMessage("Ignoring bad library name", mess);
          continue;
        }

        // get the path for all .jar files in this code folder
        String libraryClassPath =
          Compiler.contentsToClassPath(exported);
        // grab all jars and classes from this folder,
        // and append them to the library classpath
        librariesClassPath +=
          File.pathSeparatorChar + libraryClassPath;
        // need to associate each import with a library folder
        String packages[] =
          Compiler.packageListFromClassPath(libraryClassPath);
        for (int k = 0; k < packages.length; k++) {
          //System.out.println(packages[k] + " -> " + exported);
          //String already = (String) importToLibraryTable.get(packages[k]);
          importToLibraryTable.put(packages[k], exported);
        }

        JMenuItem item = new JMenuItem(list[i]);
        item.addActionListener(listener);
        item.setActionCommand(entry.getAbsolutePath());
        menu.add(item);
        ifound = true;

      } else {  // not a library, but is still a folder, so recurse
        JMenu submenu = new JMenu(list[i]);
        // needs to be separate var, otherwise would set ifound to false
        boolean found = addLibraries(submenu, subfolder);
        if (found) {
          menu.add(submenu);
          ifound = true;
        }
      }
    }
    return ifound;
  }


  // .................................................................


  /**
   * Show the About box.
   */
  public void handleAbout() {
    final Image image = Base.getImage("about.jpg", activeEditor);
    final Window window = new Window(activeEditor) {
        public void paint(Graphics g) {
          g.drawImage(image, 0, 0, null);

          Graphics2D g2 = (Graphics2D) g;
          g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                              RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);

          g.setFont(new Font("SansSerif", Font.PLAIN, 11));
          g.setColor(Color.white);
          g.drawString(Base.VERSION_NAME, 50, 30);
        }
      };
    window.addMouseListener(new MouseAdapter() {
        public void mousePressed(MouseEvent e) {
          window.dispose();
        }
      });
    int w = image.getWidth(activeEditor);
    int h = image.getHeight(activeEditor);
    Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
    window.setBounds((screen.width-w)/2, (screen.height-h)/2, w, h);
    window.setVisible(true);
  }


  /**
   * Show the preferences window.
   */
  public void handlePrefs() {
    Preferences preferences = new Preferences();
    preferences.showFrame(activeEditor);
  }


  // ...................................................................


  /**
   * returns true if Processing is running on a Mac OS machine,
   * specifically a Mac OS X machine because it doesn't un on OS 9 anymore.
   */
  static public boolean isMacOS() {
    return PApplet.platform == PConstants.MACOSX;
  }


  /**
   * returns true if running on windows.
   */
  static public boolean isWindows() {
    return PApplet.platform == PConstants.WINDOWS;
  }


  /**
   * true if running on linux.
   */
  static public boolean isLinux() {
    return PApplet.platform == PConstants.LINUX;
  }


  // .................................................................


  static public File getSettingsFolder() {
    File dataFolder = null;

    String pref = Preferences.get("settings.path");
    if (pref != null) {
      dataFolder = new File(pref);

    } else if (PApplet.platform == PConstants.MACOSX) {
      // carbon folder constants
      // http://developer.apple.com/documentation/Carbon/Reference
      //   /Folder_Manager/folder_manager_ref/constant_6.html#/
      //   /apple_ref/doc/uid/TP30000238/C006889

      // additional information found int the local file:
      // /System/Library/Frameworks/CoreServices.framework
      //   /Versions/Current/Frameworks/CarbonCore.framework/Headers/

      // this is the 1.4 version.. but using 1.3 since i have the stubs
      // import com.apple.eio.*
      //println(FileManager.findFolder(kUserDomain,
      //        kDomainLibraryFolderType));

      // not clear if i can write to this folder tho..
      try {
        /*
        if (false) {
          // this is because the mrjtoolkit stubs don't have the
          // thows exception around them
          new FileInputStream("ignored");
        }
        */


        /*
        MRJOSType domainLibrary = new MRJOSType("dlib");
        Method findFolderMethod =
          MRJFileUtils.class.getMethod("findFolder",
                                       new Class[] { Short.TYPE,
                                                     MRJOSType.class });
        File libraryFolder = (File)
          findFolderMethod.invoke(null, new Object[] { new Short(kUserDomain),
                                                       domainLibrary });

                                                       */
        // TODO load this dynamically
        File libraryFolder = new File(BaseMacOS.getLibraryFolder());
        dataFolder = new File(libraryFolder, "Processing");

      } catch (Exception e) {
        // this could be FileNotFound or NoSuchMethod
        //} catch (FileNotFoundException e) {
        //e.printStackTrace();
        //System.exit(1);
        showError("Problem getting data folder",
                  "Error getting the Processing data folder.", e);
      }

    } else if (Base.isWindows()) {
      // looking for Documents and Settings/blah/Application Data/Processing

      // this is just based on the other documentation, and eyeballing
      // that part of the registry.. not confirmed by any msft/msdn docs.
      // HKEY_CURRENT_USER\Software\Microsoft
      //   \Windows\CurrentVersion\Explorer\Shell Folders
      // Value Name: AppData
      // Value Type: REG_SZ
      // Value Data: path

      try {
        //RegistryKey topKey = Registry.getTopLevelKey("HKCU");
        RegistryKey topKey = Registry.HKEY_CURRENT_USER;

        String localKeyPath =
          "Software\\Microsoft\\Windows\\CurrentVersion" +
          "\\Explorer\\Shell Folders";
        RegistryKey localKey = topKey.openSubKey(localKeyPath);
        String appDataPath = cleanKey(localKey.getStringValue("AppData"));
        //System.out.println("app data path is " + appDataPath);
        //System.exit(0);
        //topKey.closeKey();  // necessary?
        //localKey.closeKey();

        dataFolder = new File(appDataPath, "Processing");

      } catch (Exception e) {
        showError("Problem getting data folder",
                  "Error getting the Processing data folder.", e);
      }
      //return null;

    } else {
      // otherwise make a .processing directory int the user's home dir
      File home = new File(System.getProperty("user.home"));
      dataFolder = new File(home, ".processing");
    }

    // create the folder if it doesn't exist already
    boolean result = true;
    if (!dataFolder.exists()) {
      result = dataFolder.mkdirs();
    }

    if (!result) {
      // try the fallback location
      System.out.println("Using fallback path for settings.");
      String fallback = Preferences.get("settings.path.fallback");
      dataFolder = new File(fallback);
      if (!dataFolder.exists()) {
        result = dataFolder.mkdirs();
      }
    }

    if (!result) {
      showError("Settings issues",
                "Processing cannot run because it could not\n" +
                "create a folder to store your settings.", null);
    }

    return dataFolder;
  }


  /**
   * For now, only used by Preferences to get the preferences.txt file.
   * @param filename
   * @return
   */
  static public File getSettingsFile(String filename) {
    return new File(getSettingsFolder(), filename);
  }


  static File buildFolder;

  static public File getBuildFolder() {
    if (buildFolder == null) {
      String buildPath = Preferences.get("build.path");
      if (buildPath != null) {
        buildFolder = new File(buildPath);

      } else {
        //File folder = new File(getTempFolder(), "build");
        //if (!folder.exists()) folder.mkdirs();
        buildFolder = createTempFolder("build");
        buildFolder.deleteOnExit();
      }
    }
    return buildFolder;
  }


  /**
   * Get the path to the platform's temporary folder, by creating
   * a temporary temporary file and getting its parent folder.
   * <br/>
   * Modified for revision 0094 to actually make the folder randomized
   * to avoid conflicts in multi-user environments. (Bug 177)
   */
  static public File createTempFolder(String name) {
    try {
      File folder = File.createTempFile(name, null);
      //String tempPath = ignored.getParent();
      //return new File(tempPath);
      folder.delete();
      folder.mkdirs();
      return folder;

    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }


  /*
  static public void addBuildFolderToClassPath() {
    String path = getBuildFolder().getAbsolutePath();
    String jcp = System.getProperty("java.class.path");
    if (jcp.indexOf(path) == -1) {
      System.setProperty("java.class.path", path + File.pathSeparator + jcp);
      //return new File(getProcessingDataFolder(), "build");
      System.out.println("jcp is now " +
                         System.getProperty("java.class.path"));
    }
  }
  */


  static public File getDefaultSketchbookFolder() {
    File sketchbookFolder = null;

    if (Base.isMacOS()) {
      // looking for /Users/blah/Documents/Processing

      // carbon folder constants
      // http://developer.apple.com/documentation/Carbon/Reference/Folder_Manager/folder_manager_ref/constant_6.html#//apple_ref/doc/uid/TP30000238/C006889

      // additional information found int the local file:
      // /System/Library/Frameworks/CoreServices.framework/Versions/Current/Frameworks/CarbonCore.framework/Headers/

      // this is the 1.4 version.. but using 1.3 since i have the stubs
      // import com.apple.eio.*
      //println(FileManager.findFolder(kUserDomain,
      //        kDomainLibraryFolderType));

      // not clear if i can write to this folder tho..
      try {
        /*
        MRJOSType domainDocuments = new MRJOSType("docs");
        //File libraryFolder = MRJFileUtils.findFolder(domainDocuments);

        // for 77, try switching this to the user domain, just to be sure
        Method findFolderMethod =
          MRJFileUtils.class.getMethod("findFolder",
                                       new Class[] { Short.TYPE,
                                                     MRJOSType.class });
        File documentsFolder = (File)
          findFolderMethod.invoke(null, new Object[] { new Short(kUserDomain),
                                                       domainDocuments });
        */
        File documentsFolder = new File(BaseMacOS.getDocumentsFolder());
        sketchbookFolder = new File(documentsFolder, "Processing");

        /*
          // more specific version for debugging
      } catch (InvocationTargetException ite) {
        Throwable target =
          ((InvocationTargetException) ite).getTargetException();
        showError("sketch folder problem",
                  "Could not locate default sketch folder location.", target);
        */

      } catch (Exception e) {
        //showError("Could not find folder",
        //          "Could not locate the Documents folder.", e);
        sketchbookFolder = promptSketchbookLocation();
      }

    } else if (isWindows()) {
      // looking for Documents and Settings/blah/My Documents/Processing
      // (though using a reg key since it's different on other platforms)

      // http://support.microsoft.com/?kbid=221837&sd=RMVP
      // The path to the My Documents folder is stored in the
      // following registry key, where path is the complete path
      // to your storage location:
      // HKEY_CURRENT_USER\Software\Microsoft\Windows\CurrentVersion\Explorer\Shell Folders
      // Value Name: Personal
      // Value Type: REG_SZ
      // Value Data: path

      try {
        RegistryKey topKey = Registry.HKEY_CURRENT_USER;

        String localKeyPath =
          "Software\\Microsoft\\Windows\\CurrentVersion" +
          "\\Explorer\\Shell Folders";
        RegistryKey localKey = topKey.openSubKey(localKeyPath);
        String personalPath = cleanKey(localKey.getStringValue("Personal"));
        //topKey.closeKey();  // necessary?
        //localKey.closeKey();
        sketchbookFolder = new File(personalPath, "Processing");

      } catch (Exception e) {
        //showError("Problem getting folder",
        //          "Could not locate the Documents folder.", e);
        sketchbookFolder = promptSketchbookLocation();
      }

    } else {
      sketchbookFolder = promptSketchbookLocation();

      /*
      // on linux (or elsewhere?) prompt the user for the location
      JFileChooser fc = new JFileChooser();
      fc.setDialogTitle("Select the folder where " +
                        "Processing sketches should be stored...");
      //fc.setSelectedFile(new File(sketchbookLocationField.getText()));
      fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

      int returned = fc.showOpenDialog(new JDialog());
      if (returned == JFileChooser.APPROVE_OPTION) {
        //File file = fc.getSelectedFile();
        //sketchbookLocationField.setText(file.getAbsolutePath());
        sketchbookFolder = fc.getSelectedFile();

      } else {
        System.exit(0);
      }
      */
    }

    // create the folder if it doesn't exist already
    boolean result = true;
    if (!sketchbookFolder.exists()) {
      result = sketchbookFolder.mkdirs();
    }

    if (!result) {
      // try the fallback location
      System.out.println("Using fallback path for sketchbook.");
      String fallback = Preferences.get("sketchbook.path.fallback");
      sketchbookFolder = new File(fallback);
      if (!sketchbookFolder.exists()) {
        result = sketchbookFolder.mkdirs();
      }
    }

    if (!result) {
      showError("error",
                "Processing cannot run because it could not\n" +
                "create a folder to store your sketchbook.", null);
    }

    return sketchbookFolder;
  }


  /**
   * Check for a new sketchbook location.
   */
  static protected File promptSketchbookLocation() {
    File folder = null;

    folder = new File(System.getProperty("user.home"), "sketchbook");
    if (!folder.exists()) {
      folder.mkdirs();
      return folder;
    }

    folder = Base.selectFolder("Select (or create new) folder for sketches...",
                               null, null);
    if (folder == null) {
      System.exit(0);
    }
    return folder;
  }


  /**
   * Clear out projects that are empty.
   */
//  static public void cleanSketchbook() {
//    if (!Preferences.getBoolean("sketchbook.auto_clean")) return;
//
//    File sketchbookFolder = getSketchbookFolder();
//    if (!sketchbookFolder.exists()) return;
//
//    //String entries[] = new File(userPath).list();
//    String entries[] = sketchbookFolder.list();
//    if (entries != null) {
//      for (int j = 0; j < entries.length; j++) {
//        //System.out.println(entries[j] + " " + entries.length);
//        if (entries[j].charAt(0) == '.') continue;
//
//        //File prey = new File(userPath, entries[j]);
//        File prey = new File(sketchbookFolder, entries[j]);
//        File pde = new File(prey, entries[j] + ".pde");
//
//        // make sure this is actually a sketch folder with a .pde,
//        // not a .DS_Store file or another random user folder
//
//        if (pde.exists() && (Base.calcFolderSize(prey) == 0)) {
//          //System.out.println("i want to remove " + prey);
//
//          //if (Preferences.getBoolean("sketchbook.auto_clean")) {
//          Base.removeDir(prey);
//
//            /*
//          } else {  // otherwise prompt the user
//            String prompt =
//              "Remove empty sketch titled \"" + entries[j] + "\"?";
//
//            Object[] options = { "Yes", "No" };
//            int result =
//              JOptionPane.showOptionDialog(editor,
//                                           prompt,
//                                           "Housekeeping",
//                                           JOptionPane.YES_NO_OPTION,
//                                           JOptionPane.QUESTION_MESSAGE,
//                                           null,
//                                           options,
//                                           options[0]);
//            if (result == JOptionPane.YES_OPTION) {
//              Base.removeDir(prey);
//            }
//            */
//          //}
//        }
//      }
//    }
//  }


  // .................................................................


  /**
   * Implementation for choosing directories that handles both the
   * Mac OS X hack to allow the native AWT file dialog, or uses
   * the JFileChooser on other platforms. Mac AWT trick obtained from
   * <A HREF="http://lists.apple.com/archives/java-dev/2003/Jul/msg00243.html">this post</A>
   * on the OS X Java dev archive which explains the cryptic note in
   * Apple's Java 1.4 release docs about the special System property.
   */
  static public File selectFolder(String prompt, File folder, Frame frame) {
    if (Base.isMacOS()) {
      if (frame == null) frame = new Frame(); //.pack();
      FileDialog fd = new FileDialog(frame, prompt, FileDialog.LOAD);
      if (folder != null) {
        fd.setDirectory(folder.getParent());
        //fd.setFile(folder.getName());
      }
      System.setProperty("apple.awt.fileDialogForDirectories", "true");
      fd.setVisible(true);
      System.setProperty("apple.awt.fileDialogForDirectories", "false");
      if (fd.getFile() == null) {
        return null;
      }
      return new File(fd.getDirectory(), fd.getFile());

    } else {
      JFileChooser fc = new JFileChooser();
      fc.setDialogTitle(prompt);
      if (folder != null) {
        fc.setSelectedFile(folder);
      }
      fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

      int returned = fc.showOpenDialog(new JDialog());
      if (returned == JFileChooser.APPROVE_OPTION) {
        return fc.getSelectedFile();
      }
    }
    return null;
  }


  static public String cleanKey(String what) {
    // jnireg seems to be reading the chars as bytes
    // so maybe be as simple as & 0xff and then running through decoder

    char c[] = what.toCharArray();

    // if chars are in the tooHigh range, it's prolly because
    // a byte from the jni registry was turned into a char
    // and there was a sign extension.
    // e.g. 0xFC (252, umlaut u) became 0xFFFC (65532).
    // but on a japanese system, maybe this is two-byte and ok?
    int tooHigh = 65536 - 128;
    for (int i = 0; i < c.length; i++) {
      if (c[i] >= tooHigh) c[i] &= 0xff;

      /*
      if ((c[i] >= 32) && (c[i] < 128)) {
        System.out.print(c[i]);
      } else {
        System.out.print("[" + PApplet.hex(c[i]) + "]");
      }
      */
    }
    //System.out.println();
    return new String(c);
  }


  // .................................................................


  static public void setIcon(Frame frame) {
    // set the window icon
    if (icon == null) {
      try {
        icon = Base.getImage("icon.gif", frame);
      } catch (Exception e) { } // fail silently, no big whup
    }
    if (icon != null) {
      frame.setIconImage(icon);
    }
  }


  // someone needs to be slapped
  //static KeyStroke closeWindowKeyStroke;

  /**
   * Return true if the key event was a Ctrl-W or an ESC,
   * both indicators to close the window.
   * Use as part of a keyPressed() event handler for frames.
   */
  /*
  static public boolean isCloseWindowEvent(KeyEvent e) {
    if (closeWindowKeyStroke == null) {
      int modifiers = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
      closeWindowKeyStroke = KeyStroke.getKeyStroke('W', modifiers);
    }
    return ((e.getKeyCode() == KeyEvent.VK_ESCAPE) ||
            KeyStroke.getKeyStrokeForEvent(e).equals(closeWindowKeyStroke));
  }
  */


  /**
   * Registers key events for a Ctrl-W and ESC with an ActionListener
   * that will take care of disposing the window.
   */
  static public void registerWindowCloseKeys(JRootPane root, //Window window,
                                             ActionListener disposer) {
    /*
    JRootPane root = null;
    if (window instanceof JFrame) {
      root = ((JFrame)window).getRootPane();
    } else if (window instanceof JDialog) {
      root = ((JDialog)window).getRootPane();
    }
    */

    KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
    root.registerKeyboardAction(disposer, stroke,
                                JComponent.WHEN_IN_FOCUSED_WINDOW);

    int modifiers = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
    stroke = KeyStroke.getKeyStroke('W', modifiers);
    root.registerKeyboardAction(disposer, stroke,
                                JComponent.WHEN_IN_FOCUSED_WINDOW);
  }


  // .................................................................


  static public void showReference(String referenceFile) {
    openURL(Base.getContents("reference" + File.separator + referenceFile));
  }


  static public void showReference() {
    showReference("index.html");
  }


  static public void showEnvironment() {
    showReference("environment" + File.separator + "index.html");
  }


  static public void showTroubleshooting() {
    showReference("troubleshooting" + File.separator + "index.html");
  }


  /**
   * Opens the local copy of the FAQ that's included
   * with the Processing download.
   */
  static public void showFAQ() {
    showReference("faq.html");
  }


  // .................................................................


  /**
   * Implements the cross-platform headache of opening URLs
   * TODO This code should be replaced by PApplet.link(),
   * however that's not a static method (because it requires
   * an AppletContext when used as an applet), so it's mildly
   * trickier than just removing this method.
   */
  static public void openURL(String url) {
    //System.out.println("opening url " + url);
    try {
      if (Base.isWindows()) {
        // this is not guaranteed to work, because who knows if the
        // path will always be c:\progra~1 et al. also if the user has
        // a different browser set as their default (which would
        // include me) it'd be annoying to be dropped into ie.
        //Runtime.getRuntime().exec("c:\\progra~1\\intern~1\\iexplore "
        // + currentDir

        // the following uses a shell execute to launch the .html file
        // note that under cygwin, the .html files have to be chmodded +x
        // after they're unpacked from the zip file. i don't know why,
        // and don't understand what this does in terms of windows
        // permissions. without the chmod, the command prompt says
        // "Access is denied" in both cygwin and the "dos" prompt.
        //Runtime.getRuntime().exec("cmd /c " + currentDir + "\\reference\\" +
        //                    referenceFile + ".html");
        if (url.startsWith("http://")) {
          // open dos prompt, give it 'start' command, which will
          // open the url properly. start by itself won't work since
          // it appears to need cmd
          Runtime.getRuntime().exec("cmd /c start " + url);
        } else {
          // just launching the .html file via the shell works
          // but make sure to chmod +x the .html files first
          // also place quotes around it in case there's a space
          // in the user.dir part of the url
          Runtime.getRuntime().exec("cmd /c \"" + url + "\"");
        }

      } else if (Base.isMacOS()) {
        //com.apple.eio.FileManager.openURL(url);

        if (!url.startsWith("http://")) {
          // prepend file:// on this guy since it's a file
          url = "file://" + url;

          // replace spaces with %20 for the file url
          // otherwise the mac doesn't like to open it
          // can't just use URLEncoder, since that makes slashes into
          // %2F characters, which is no good. some might say "useless"
          if (url.indexOf(' ') != -1) {
            StringBuffer sb = new StringBuffer();
            char c[] = url.toCharArray();
            for (int i = 0; i < c.length; i++) {
              if (c[i] == ' ') {
                sb.append("%20");
              } else {
                sb.append(c[i]);
              }
            }
            url = sb.toString();
          }
        }
        com.apple.eio.FileManager.openURL(url);

      } else if (Base.isLinux()) {
        // how's mozilla sound to ya, laddie?
        //Runtime.getRuntime().exec(new String[] { "mozilla", url });
        //String browser = Preferences.get("browser");
        //Runtime.getRuntime().exec(new String[] { browser, url });
        String launcher = Preferences.get("launcher.linux");
        if (launcher != null) {
          Runtime.getRuntime().exec(new String[] { launcher, url });
        }
      } else {
        String launcher = Preferences.get("launcher");
        if (launcher != null) {
          Runtime.getRuntime().exec(new String[] { launcher, url });
        } else {
          System.err.println("Unspecified platform, no launcher available.");
        }
      }

    } catch (IOException e) {
      Base.showWarning("Could not open URL",
                       "An error occurred while trying to open\n" + url, e);
    }
  }


  static boolean openFolderAvailable() {
    if (Base.isWindows() || Base.isMacOS()) return true;

    if (Base.isLinux()) {
      // Assume that this is set to something valid
      if (Preferences.get("launcher.linux") != null) {
        return true;
      }

      // Attempt to use gnome-open
      try {
        Process p = Runtime.getRuntime().exec(new String[] { "gnome-open" });
        /*int result =*/ p.waitFor();
        // Not installed will throw an IOException (JDK 1.4.2, Ubuntu 7.04)
        Preferences.set("launcher.linux", "gnome-open");
        return true;
      } catch (Exception e) { }

      // Attempt with kde-open
      try {
        Process p = Runtime.getRuntime().exec(new String[] { "kde-open" });
        /*int result =*/ p.waitFor();
        Preferences.set("launcher.linux", "kde-open");
        return true;
      } catch (Exception e) { }
    }
    return false;
  }


  /**
   * Implements the other cross-platform headache of opening
   * a folder in the machine's native file browser.
   */
  static public void openFolder(File file) {
    try {
      String folder = file.getAbsolutePath();

      if (Base.isWindows()) {
        // doesn't work
        //Runtime.getRuntime().exec("cmd /c \"" + folder + "\"");

        // works fine on winxp, prolly win2k as well
        Runtime.getRuntime().exec("explorer \"" + folder + "\"");

        // not tested
        //Runtime.getRuntime().exec("start explorer \"" + folder + "\"");

      } else if (Base.isMacOS()) {
        openURL(folder);  // handles char replacement, etc

      } else if (Base.isLinux()) {
        String launcher = Preferences.get("launcher.linux");
        if (launcher != null) {
          Runtime.getRuntime().exec(new String[] { launcher, folder });
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }


  /**
   * "No cookie for you" type messages. Nothing fatal or all that
   * much of a bummer, but something to notify the user about.
   */
  static public void showMessage(String title, String message) {
    if (title == null) title = "Message";
    JOptionPane.showMessageDialog(new Frame(), message, title,
                                  JOptionPane.INFORMATION_MESSAGE);
  }


  /**
   * Non-fatal error message with optional stack trace side dish.
   */
  static public void showWarning(String title, String message,
                                 Exception e) {
    if (title == null) title = "Warning";
    JOptionPane.showMessageDialog(new Frame(), message, title,
                                  JOptionPane.WARNING_MESSAGE);

    //System.err.println(e.toString());
    if (e != null) e.printStackTrace();
  }


  /**
   * Show an error message that's actually fatal to the program.
   * This is an error that can't be recovered. Use showWarning()
   * for errors that allow P5 to continue running.
   */
  static public void showError(String title, String message,
                               Throwable e) {
    if (title == null) title = "Error";
    JOptionPane.showMessageDialog(new Frame(), message, title,
                                  JOptionPane.ERROR_MESSAGE);

    if (e != null) e.printStackTrace();
    System.exit(1);
  }


  // ...................................................................

  
  // "contents" refers to the Mac OS X style way of handling Processing
  // applications. 


  static public String getContents(String what) {
    String basePath = System.getProperty("user.dir");
    /*
      // do this later, when moving to .app package
    if (PApplet.platform == PConstants.MACOSX) {
      basePath = System.getProperty("processing.contents");
    }
    */
    return basePath + File.separator + what;
  }


  static public String getLibContents(String what) {
    String libPath = getContents("lib/" + what);
    File libDir = new File(libPath);
    if (libDir.exists()) {
      return libPath;
    }
//    was looking into making this run from Eclipse, but still too much mess
//    libPath = getContents("build/shared/lib/" + what);
//    libDir = new File(libPath);
//    if (libDir.exists()) {
//      return libPath;
//    }
    return null;
  }


  static public Image getImage(String name, Component who) {
    Image image = null;
    Toolkit tk = Toolkit.getDefaultToolkit();

    image = tk.getImage(getLibContents(name));
    MediaTracker tracker = new MediaTracker(who);
    tracker.addImage(image, 0);
    try {
      tracker.waitForAll();
    } catch (InterruptedException e) { }
    return image;
  }


  static public InputStream getStream(String filename) throws IOException {
    return new FileInputStream(getLibContents(filename));
  }


  // ...................................................................


  static public byte[] grabFile(File file) throws IOException {
    int size = (int) file.length();
    FileInputStream input = new FileInputStream(file);
    byte buffer[] = new byte[size];
    int offset = 0;
    int bytesRead;
    while ((bytesRead = input.read(buffer, offset, size-offset)) != -1) {
      offset += bytesRead;
      if (bytesRead == 0) break;
    }
    input.close();  // weren't properly being closed
    input = null;
    return buffer;
  }


  static public void copyFile(File afile, File bfile) throws IOException {
    InputStream from = new BufferedInputStream(new FileInputStream(afile));
    OutputStream to = new BufferedOutputStream(new FileOutputStream(bfile));
    byte[] buffer = new byte[16 * 1024];
    int bytesRead;
    while ((bytesRead = from.read(buffer)) != -1) {
      to.write(buffer, 0, bytesRead);
    }
    to.flush();
    from.close(); // ??
    from = null;
    to.close(); // ??
    to = null;

    bfile.setLastModified(afile.lastModified());  // jdk13+ required
  //} catch (IOException e) {
  //  e.printStackTrace();
  //}
  }


  /**
   * Grab the contents of a file as a string.
   */
  static public String loadFile(File file) throws IOException {
    // empty code file.. no worries, might be getting filled up later
    if (file.length() == 0) return "";

    InputStreamReader isr = new InputStreamReader(new FileInputStream(file));
    BufferedReader reader = new BufferedReader(isr);

    StringBuffer buffer = new StringBuffer();
    String line = null;
    while ((line = reader.readLine()) != null) {
      buffer.append(line);
      buffer.append('\n');
    }
    reader.close();
    return buffer.toString();
  }


  /**
   * Spew the contents of a String object out to a file.
   */
  static public void saveFile(String str,
                              File file) throws IOException {

    ByteArrayInputStream bis = new ByteArrayInputStream(str.getBytes());
    InputStreamReader isr = new InputStreamReader(bis);
    BufferedReader reader = new BufferedReader(isr);

    FileWriter fw = new FileWriter(file);
    PrintWriter writer = new PrintWriter(new BufferedWriter(fw));

    String line = null;
    while ((line = reader.readLine()) != null) {
      writer.println(line);
    }
    writer.flush();
    writer.close();
  }


  static public void copyDir(File sourceDir,
                             File targetDir) throws IOException {
    targetDir.mkdirs();
    String files[] = sourceDir.list();
    for (int i = 0; i < files.length; i++) {
      if (files[i].equals(".") || files[i].equals("..")) continue;
      File source = new File(sourceDir, files[i]);
      File target = new File(targetDir, files[i]);
      if (source.isDirectory()) {
        //target.mkdirs();
        copyDir(source, target);
        target.setLastModified(source.lastModified());
      } else {
        copyFile(source, target);
      }
    }
  }


  /**
   * Remove all files in a directory and the directory itself.
   */
  static public void removeDir(File dir) {
    if (dir.exists()) {
      removeDescendants(dir);
      if (!dir.delete()) {
        System.err.println("Could not delete " + dir);
      }
    }
  }


  /**
   * Recursively remove all files within a directory,
   * used with removeDir(), or when the contents of a dir
   * should be removed, but not the directory itself.
   * (i.e. when cleaning temp files from lib/build)
   */
  static public void removeDescendants(File dir) {
    if (!dir.exists()) return;

    String files[] = dir.list();
    for (int i = 0; i < files.length; i++) {
      if (files[i].equals(".") || files[i].equals("..")) continue;
      File dead = new File(dir, files[i]);
      if (!dead.isDirectory()) {
        if (!Preferences.getBoolean("compiler.save_build_files")) {
          if (!dead.delete()) {
            // temporarily disabled
            System.err.println("Could not delete " + dead);
          }
        }
      } else {
        removeDir(dead);
        //dead.delete();
      }
    }
  }


  /**
   * Calculate the size of the contents of a folder.
   * Used to determine whether sketches are empty or not.
   * Note that the function calls itself recursively.
   */
  static public int calcFolderSize(File folder) {
    int size = 0;

    String files[] = folder.list();
    // null if folder doesn't exist, happens when deleting sketch
    if (files == null) return -1;

    for (int i = 0; i < files.length; i++) {
      if (files[i].equals(".") || (files[i].equals("..")) ||
          files[i].equals(".DS_Store")) continue;
      File fella = new File(folder, files[i]);
      if (fella.isDirectory()) {
        size += calcFolderSize(fella);
      } else {
        size += (int) fella.length();
      }
    }
    return size;
  }


  /**
   * Gets a list of all files within the specified folder,
   * and returns a list of their relative paths.
   * Ignores any files/folders prefixed with a dot.
   */
  static public String[] listFiles(String path, boolean relative) {
    return listFiles(new File(path), relative);
  }


  static public String[] listFiles(File folder, boolean relative) {
    String path = folder.getAbsolutePath();
    Vector vector = new Vector();
    listFiles(relative ? (path + File.separator) : "", path, vector);
    String outgoing[] = new String[vector.size()];
    vector.copyInto(outgoing);
    return outgoing;
  }


  static protected void listFiles(String basePath,
                                  String path, Vector vector) {
    File folder = new File(path);
    String list[] = folder.list();
    if (list == null) return;

    for (int i = 0; i < list.length; i++) {
      if (list[i].charAt(0) == '.') continue;

      File file = new File(path, list[i]);
      String newPath = file.getAbsolutePath();
      if (newPath.startsWith(basePath)) {
        newPath = newPath.substring(basePath.length());
      }
      vector.add(newPath);
      if (file.isDirectory()) {
        listFiles(basePath, newPath, vector);
      }
    }
  }
}
