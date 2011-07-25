/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2004-11 Ben Fry and Casey Reas
  Copyright (c) 2001-04 Massachusetts Institute of Technology

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

package processing.app;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.*;

import javax.swing.*;
import javax.swing.tree.*;

import processing.core.*;
import processing.mode.android.AndroidMode;
import processing.mode.javascript.JavaScriptMode;
import processing.mode.java.*;


/**
 * The base class for the main processing application.
 * Primary role of this class is for platform identification and
 * general interaction with the system (launching URLs, loading
 * files and images, etc) that comes from that.
 */
public class Base {
  static public final int REVISION = 200;
  /** This might be replaced by main() if there's a lib/version.txt file. */
  static public String VERSION_NAME = "0200";
  /** Set true if this a proper release rather than a numbered revision. */
  static public boolean RELEASE = false;
  /** True if heavy debugging error/log messages are enabled */
  static public boolean DEBUG = false;
//  static public boolean DEBUG = true;
  static public boolean ENABLE_LIBRARY_MANAGER = false;

  static HashMap<Integer, String> platformNames =
    new HashMap<Integer, String>();
  static {
    platformNames.put(PConstants.WINDOWS, "windows");
    platformNames.put(PConstants.MACOSX, "macosx");
    platformNames.put(PConstants.LINUX, "linux");
  }

  static HashMap<String, Integer> platformIndices = new HashMap<String, Integer>();
  static {
    platformIndices.put("windows", PConstants.WINDOWS);
    platformIndices.put("macosx", PConstants.MACOSX);
    platformIndices.put("linux", PConstants.LINUX);
  }
  static Platform platform;

  static private boolean commandLine;

  // A single instance of the preferences window
  Preferences preferencesFrame;

  // A single instance of the library manager window
  ContributionManager libraryManagerFrame;

  // set to true after the first time the menu is built.
  // so that the errors while building don't show up again.
  boolean builtOnce;

  // Location for untitled items
  static File untitledFolder;

  java.util.List<Editor> editors =
    Collections.synchronizedList(new ArrayList<Editor>());
  protected Editor activeEditor;
  // a lone file menu to be used when all sketch windows are closed
  static public JMenu defaultFileMenu;

  private Mode defaultMode;
//  private Mode androidMode;
  private Mode[] modeList;
//  private JMenu modeMenu;

  private JMenu sketchbookMenu;

  protected File sketchbookFolder;
  protected File toolsFolder;


  static public void main(final String[] args) {
    EventQueue.invokeLater(new Runnable() {
        public void run() {
          createAndShowGUI(args);
        }
    });
  }


  static private void createAndShowGUI(String[] args) {
    try {
      File versionFile = getContentFile("lib/version.txt");
      if (versionFile.exists()) {
        String version = PApplet.loadStrings(versionFile)[0];
        if (!version.equals(VERSION_NAME)) {
          VERSION_NAME = version;
          RELEASE = true;
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    initPlatform();

    // Use native popups so they don't look so crappy on osx
    JPopupMenu.setDefaultLightWeightPopupEnabled(false);

    // Don't put anything above this line that might make GUI,
    // because the platform has to be inited properly first.

    // Make sure a full JDK is installed
    initRequirements();

    // run static initialization that grabs all the prefs
    Preferences.init(null);

    // setup the theme coloring fun
//    Theme.init();

    // Set the look and feel before opening the window
    try {
      platform.setLookAndFeel();
    } catch (Exception e) {
      String mess = e.getMessage();
      if (mess.indexOf("ch.randelshofer.quaqua.QuaquaLookAndFeel") == -1) {
        System.err.println("Non-fatal error while setting the Look & Feel.");
        System.err.println("The error message follows, however Processing should run fine.");
        System.err.println(mess);
      }
    }

    // Create a location for untitled sketches
    try {
      untitledFolder = Base.createTempFolder("untitled", "sketches");
      untitledFolder.deleteOnExit();
    } catch (IOException e) {
      //e.printStackTrace();
      Base.showError("Trouble without a name",
                     "Could not create a place to store untitled sketches.\n" +
                     "That's gonna prevent us from continuing.", e);
    }

//    System.out.println("about to create base...");
    new Base(args);
//    System.out.println("done creating base...");
  }


  public static void setCommandLine() {
    commandLine = true;
  }


  static protected boolean isCommandLine() {
    return commandLine;
  }


  static public void initPlatform() {
    try {
      Class<?> platformClass = Class.forName("processing.app.Platform");
      if (Base.isMacOS()) {
        platformClass = Class.forName("processing.app.macosx.Platform");
      } else if (Base.isWindows()) {
        platformClass = Class.forName("processing.app.windows.Platform");
      } else if (Base.isLinux()) {
        platformClass = Class.forName("processing.app.linux.Platform");
      }
      platform = (Platform) platformClass.newInstance();
    } catch (Exception e) {
      Base.showError("Problem Setting the Platform",
                     "An unknown error occurred while trying to load\n" +
                     "platform-specific code for your machine.", e);
    }
  }


  public static void initRequirements() {
    try {
      Class.forName("com.sun.jdi.VirtualMachine");
    } catch (ClassNotFoundException cnfe) {
      //Base.showPlatforms();
      //static public void showPlatforms() {
      Base.openURL("http://wiki.processing.org/w/Supported_Platforms");
      Base.showError("Please install JDK 1.5 or later",
                     "Processing requires a full JDK (not just a JRE)\n" +
                     "to run. Please install JDK 1.5 or later.\n" +
                     "More information can be found in the reference.", cnfe);
    }
  }


  public Base(String[] args) {
    // TODO this will be dynamically loading modes in no time
    defaultMode = new JavaMode(this, getContentFile("modes/java"));
    Mode androidMode = new AndroidMode(this, getContentFile("modes/android"));
        Mode javaScriptMode = new JavaScriptMode(this, getContentFile("modes/javascript"));
    modeList = new Mode[] { defaultMode, androidMode, javaScriptMode };
//    modeList = new Mode[] { defaultMode, androidMode };
    
    libraryManagerFrame = new ContributionManager();

    // Get the sketchbook path, and make sure it's set properly
    determineSketchbookFolder();

    // Make sure ThinkDifferent has library examples too
    defaultMode.rebuildLibraryList();

    // Put this after loading the examples, so that building the default file
    // menu works on Mac OS X (since it needs examplesFolder to be set).
    platform.init(this);

    toolsFolder = getContentFile("tools");

//    // Get the sketchbook path, and make sure it's set properly
//    determineSketchbookFolder();

    // Check if there were previously opened sketches to be restored
    boolean opened = restoreSketches();

    // Check if any files were passed in on the command line
    for (int i = 0; i < args.length; i++) {
      String path = args[i];
      // Fix a problem with systems that use a non-ASCII languages. Paths are
      // being passed in with 8.3 syntax, which makes the sketch loader code
      // unhappy, since the sketch folder naming doesn't match up correctly.
      // http://dev.processing.org/bugs/show_bug.cgi?id=1089
      if (isWindows()) {
        try {
          File file = new File(args[i]);
          path = file.getCanonicalPath();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
      if (handleOpen(path) != null) {
        opened = true;
      }
    }

    // Create a new empty window (will be replaced with any files to be opened)
    if (!opened) {
//      System.out.println("opening a new window");
      handleNew();
//    } else {
//      System.out.println("something else was opened");
    }

    // check for updates
    if (Preferences.getBoolean("update.check")) {
      new UpdateCheck(this);
    }
  }


  /**
   * Single location for the default extension, rather than hardwiring .pde
   * all over the place. While it may seem like fun to send the Arduino guys
   * on a treasure hunt, it gets old after a while.
   */
//  static protected String getExtension() {
//    return ".pde";
//  }


//  public Mode getDefaultMode() {
//    return defaultMode;
//  }


  /**
   * Post-constructor setup for the editor area. Loads the last
   * sketch that was used (if any), and restores other Editor settings.
   * The complement to "storePreferences", this is called when the
   * application is first launched.
   */
  protected boolean restoreSketches() {
    String lastMode = Preferences.get("last.sketch.mode");
    if (DEBUG) {
      System.out.println("setting mode to " + lastMode);
    }
    if (lastMode != null) {
//      try {
//        Class<?> modeClass = Class.forName(lastMode);
//        defaultMode = (Mode) modeClass.newInstance();
//      } catch (ClassNotFoundException e) {
//        e.printStackTrace();
//      } catch (InstantiationException e) {
//        e.printStackTrace();
//      } catch (IllegalAccessException e) {
//        e.printStackTrace();
//      }
      for (Mode m : modeList) {
        if (m.getClass().getName().equals(lastMode)) {
          defaultMode = m;
        }
      }
    }

    if (DEBUG) {
      System.out.println("default mode set to " + defaultMode.getClass().getName());
    }

    if (!Preferences.getBoolean("last.sketch.restore")) {
      return false;
    }

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
  protected void storeSketches() {
    // Save the width and height of the screen
    Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
    Preferences.setInteger("last.screen.width", screen.width);
    Preferences.setInteger("last.screen.height", screen.height);

    String untitledPath = untitledFolder.getAbsolutePath();

    // Save the sketch path and window placement for each open sketch
    int index = 0;
    for (Editor editor : editors) {
      String path = editor.getSketch().getMainFilePath();
      // In case of a crash, save untitled sketches if they contain changes.
      // (Added this for release 0158, may not be a good idea.)
      if (path.startsWith(untitledPath) &&
          !editor.getSketch().isModified()) {
        continue;
      }
      Preferences.set("last.sketch" + index + ".path", path);

      int[] location = editor.getPlacement();
      String locationStr = PApplet.join(PApplet.str(location), ",");
      Preferences.set("last.sketch" + index + ".location", locationStr);
      index++;
    }
    Preferences.setInteger("last.sketch.count", index);
    Preferences.set("last.sketch.mode", defaultMode.getClass().getName());
  }


  // If a sketch is untitled on quit, may need to store the new name
  // rather than the location from the temp folder.
  protected void storeSketchPath(Editor editor, int index) {
    String path = editor.getSketch().getMainFilePath();
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


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  /** Command on Mac OS X, Ctrl on Windows and Linux */
  static final int SHORTCUT_KEY_MASK =
    Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
  /** Command-W on Mac OS X, Ctrl-W on Windows and Linux */
  static final KeyStroke WINDOW_CLOSE_KEYSTROKE =
    KeyStroke.getKeyStroke('W', SHORTCUT_KEY_MASK);
  /** Command-Option on Mac OS X, Ctrl-Alt on Windows and Linux */
  static final int SHORTCUT_ALT_KEY_MASK = ActionEvent.ALT_MASK |
    Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();


  /**
   * A software engineer, somewhere, needs to have his abstraction
   * taken away. In some countries they jail or beat people for crafting
   * the sort of API that would require a five line helper function
   * just to set the shortcut key for a menu item.
   */
  static public JMenuItem newJMenuItem(String title, int what) {
    JMenuItem menuItem = new JMenuItem(title);
    int modifiers = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
    menuItem.setAccelerator(KeyStroke.getKeyStroke(what, modifiers));
    return menuItem;
  }


  /**
   * Like newJMenuItem() but adds shift as a modifier for the shortcut.
   */
  static public JMenuItem newJMenuItemShift(String title, int what) {
    JMenuItem menuItem = new JMenuItem(title);
    int modifiers = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
    modifiers |= ActionEvent.SHIFT_MASK;
    menuItem.setAccelerator(KeyStroke.getKeyStroke(what, modifiers));
    return menuItem;
  }


  /**
   * Same as newJMenuItem(), but adds the ALT (on Linux and Windows)
   * or OPTION (on Mac OS X) key as a modifier.
   */
  static public JMenuItem newJMenuItemAlt(String title, int what) {
    JMenuItem menuItem = new JMenuItem(title);
    //int modifiers = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
    //menuItem.setAccelerator(KeyStroke.getKeyStroke(what, modifiers));
    menuItem.setAccelerator(KeyStroke.getKeyStroke(what, SHORTCUT_ALT_KEY_MASK));
    return menuItem;
  }


  static public JCheckBoxMenuItem newJCheckBoxMenuItem(String title, int what) {
    JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem(title);
    int modifiers = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
    menuItem.setAccelerator(KeyStroke.getKeyStroke(what, modifiers));
    return menuItem;
  }


  static public void addDisabledItem(JMenu menu, String title) {
    JMenuItem item = new JMenuItem(title);
    item.setEnabled(false);
    menu.add(item);
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  /** Returns the frontmost, active editor window. */
  public Editor getActiveEditor() {
    return activeEditor;
  }


  protected void changeMode(Mode mode) {
    if (activeEditor.getMode() != mode) {
      Sketch sketch = activeEditor.getSketch();
      if (sketch.isModified()) {
        Base.showWarning("Save",
                         "Please save the sketch before changing the mode.",
                         null);
      } else {
        boolean untitled = activeEditor.untitled;
        String mainPath = sketch.getMainFilePath();

        // save a mode file into this sketch folder
        File sketchProps = new File(sketch.getFolder(), "sketch.properties");
        try {
          Settings props = new Settings(sketchProps);
          props.set("mode", mode.getTitle());
          props.save();
        } catch (IOException e) {
          e.printStackTrace();
        }
//        PrintWriter writer = PApplet.createWriter(sketchProps);
//        writer.println("mode=" + mode.getTitle());
//        writer.flush();
//        writer.close();

        // close this sketch
        int[] where = activeEditor.getPlacement();
        handleClose(activeEditor, true);

        // re-open the sketch
        Editor editor = handleOpen(mainPath, where);
        if (editor != null) {
          editor.untitled = untitled;
        }
      }
    }
  }


  public Mode[] getModeList() {
    return modeList;
  }


  // Because of variations in native windowing systems, no guarantees about
  // changes to the focused and active Windows can be made. Developers must
  // never assume that this Window is the focused or active Window until this
  // Window receives a WINDOW_GAINED_FOCUS or WINDOW_ACTIVATED event.
  protected void handleActivated(Editor whichEditor) {
    activeEditor = whichEditor;

    // set the current window to be the console that's getting output
    EditorConsole.setEditor(activeEditor);

    // make this the next mode to be loaded
    defaultMode = whichEditor.getMode();
  }


  protected int[] nextEditorLocation() {
    Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
    int defaultWidth = Preferences.getInteger("editor.window.width.default");
    int defaultHeight = Preferences.getInteger("editor.window.height.default");

//    if (activeEditor == null) {
    if (editors.size() == 0) {
//      System.out.println("active editor is " + activeEditor);
      // If no current active editor, use default placement
      return new int[] {
          (screen.width - defaultWidth) / 2,
          (screen.height - defaultHeight) / 2,
          defaultWidth, defaultHeight, 0
      };

    } else {
      // With a currently active editor, open the new window
      // using the same dimensions, but offset slightly.
      synchronized (editors) {
        final int OVER = 50;
        // In release 0160, don't
        //location = activeEditor.getPlacement();
        Editor lastOpened = editors.get(editors.size() - 1);
        int[] location = lastOpened.getPlacement();
        // Just in case the bounds for that window are bad
        location[0] += OVER;
        location[1] += OVER;

        if (location[0] == OVER ||
            location[2] == OVER ||
            location[0] + location[2] > screen.width ||
            location[1] + location[3] > screen.height) {
          // Warp the next window to a randomish location on screen.
          return new int[] {
              (int) (Math.random() * (screen.width - defaultWidth)),
              (int) (Math.random() * (screen.height - defaultHeight)),
              defaultWidth, defaultHeight, 0
          };
        }

        return location;
      }
    }
  }


  /**
   * Return the same mode as the active editor, or the default mode, which
   * begins as Java/Standard, but is updated with the last mode used.
   */
  public Mode nextEditorMode() {
    return (activeEditor == null) ? defaultMode : activeEditor.getMode();
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  boolean breakTime = false;
  String[] months = {
    "jan", "feb", "mar", "apr", "may", "jun",
    "jul", "aug", "sep", "oct", "nov", "dec"
  };

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
//    File sketchbookDir = getSketchbookFolder();
    File newbieParentDir = untitledFolder;

    String prefix = Preferences.get("editor.untitled.prefix");

    // Use a generic name like sketch_031008a, the date plus a char
    int index = 0;
    String format = Preferences.get("editor.untitled.suffix");
    String suffix = null;
    if (format == null) {
      Calendar cal = Calendar.getInstance();
      int day = cal.get(Calendar.DAY_OF_MONTH);  // 1..31
      int month = cal.get(Calendar.MONTH);  // 0..11
      suffix = months[month] + PApplet.nf(day, 2);
    } else {
      //SimpleDateFormat formatter = new SimpleDateFormat("yyMMdd");
      //SimpleDateFormat formatter = new SimpleDateFormat("MMMdd");
      //String purty = formatter.format(new Date()).toLowerCase();
      SimpleDateFormat formatter = new SimpleDateFormat(format);
      suffix = formatter.format(new Date());
    }
    do {
      if (index == 26) {
        // In 0159, avoid running past z by sending people outdoors.
        if (!breakTime) {
          Base.showWarning("Time for a Break",
                           "You've reached the limit for auto naming of new sketches\n" +
                           "for the day. How about going for a walk instead?", null);
          breakTime = true;
        } else {
          Base.showWarning("Sunshine",
                           "No really, time for some fresh air for you.", null);
        }
        return null;
      }
      newbieName = prefix + suffix + ((char) ('a' + index));
      // Also sanitize the name since it might do strange things on
      // non-English systems that don't use this sort of date format.
      // http://code.google.com/p/processing/issues/detail?id=283
      newbieName = Sketch.sanitizeName(newbieName);
      newbieDir = new File(newbieParentDir, newbieName);
      index++;
      // Make sure it's not in the temp folder *and* it's not in the sketchbook
    } while (newbieDir.exists() || new File(sketchbookFolder, newbieName).exists());

    // Make the directory for the new sketch
    newbieDir.mkdirs();

    // Make an empty pde file
    //File newbieFile = new File(newbieDir, newbieName + getExtension());
    File newbieFile = new File(newbieDir, newbieName + "." + defaultMode.getDefaultExtension());
    new FileOutputStream(newbieFile);  // create the file
    return newbieFile.getAbsolutePath();
  }


  /**
   * Create a new untitled document in a new sketch window.
   */
  public void handleNew() {
    try {
      String path = createNewUntitled();
      if (path != null) {
        Editor editor = handleOpen(path);
        editor.untitled = true;
      } else {
        // happens when user gets to the end of 26 new sketches for the day
        //System.err.println("untitled went null...");
      }

    } catch (IOException e) {
      if (activeEditor != null) {
        activeEditor.statusError(e);
      } else {
        e.printStackTrace();
      }
    }
  }


  /**
   * Replace the sketch in the current window with a new untitled document.
   */
  public void handleNewReplace() {
    if (!activeEditor.checkModified()) {
      return;  // sketch was modified, and user canceled
    }
    // Close the running window, avoid window boogers with multiple sketches
    activeEditor.internalCloseRunner();

    // Actually replace things
    handleNewReplaceImpl();
  }


  protected void handleNewReplaceImpl() {
    try {
      String path = createNewUntitled();
      if (path != null) {
        activeEditor.handleOpenInternal(path);
        activeEditor.untitled = true;
      }
//      return true;

    } catch (IOException e) {
      activeEditor.statusError(e);
//      return false;
    }
  }


  /**
   * Open a sketch, replacing the sketch in the current window.
   * @param path Location of the primary pde file for the sketch.
   */
  public void handleOpenReplace(String path) {
    if (!activeEditor.checkModified()) {
      return;  // sketch was modified, and user canceled
    }
    // Close the running window, avoid window boogers with multiple sketches
    activeEditor.internalCloseRunner();

    boolean loaded = activeEditor.handleOpenInternal(path);
    if (!loaded) {
      // replace the document without checking if that's ok
      handleNewReplaceImpl();
    }
  }


  /**
   * Prompt for a sketch to open, and open it in a new window.
   */
  public void handleOpenPrompt() {
    // get the frontmost window frame for placing file dialog
    FileDialog fd = new FileDialog(activeEditor,
                                   "Open a Processing sketch...",
                                   FileDialog.LOAD);
    // This was annoying people, so disabled it in 0125.
    //fd.setDirectory(Preferences.get("sketchbook.path"));
    //fd.setDirectory(getSketchbookPath());

    final ArrayList<String> extensions = new ArrayList<String>();
    for (Mode mode : modeList) {
      extensions.add(mode.getDefaultExtension());
    }

    // Only show .pde files as eligible bachelors
    fd.setFilenameFilter(new FilenameFilter() {
        public boolean accept(File dir, String name) {
          // confirmed to be working properly [fry 110128]
          for (String ext : extensions) {
            if (name.toLowerCase().endsWith("." + ext)) {
              return true;
            }
          }
          return false;
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


  /**
   * Open a sketch in a new window.
   * @param path Path to the pde file for the sketch in question
   * @return the Editor object, so that properties (like 'untitled')
   *         can be set by the caller
   */
  public Editor handleOpen(String path) {
    return handleOpen(path, nextEditorLocation());
  }


  protected Editor handleOpen(String path, int[] location) {
//    System.err.println("entering handleOpen " + path);

    File file = new File(path);
    if (!file.exists()) return null;

    if (!Sketch.isSanitaryName(file.getName())) {
      Base.showWarning("You're tricky, but not tricky enough",
                       file.getName() + " is not a valid name for a sketch.\n" +
                       "Better to stick to ASCII, no spaces, and make sure\n" +
                       "it doesn't start with a number.", null);
      return null;
    }

//    System.err.println("  editors: " + editors);
    // Cycle through open windows to make sure that it's not already open.
    for (Editor editor : editors) {
      if (editor.getSketch().getMainFilePath().equals(path)) {
        editor.toFront();
//        System.err.println("  handleOpen: already opened");
        return editor;
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

    Mode nextMode = nextEditorMode();
    try {
      File sketchFolder = new File(path).getParentFile();
      File sketchProps = new File(sketchFolder, "sketch.properties");
      if (sketchProps.exists()) {
        Settings props = new Settings(sketchProps);
        String modeTitle = props.get("mode");
        if (modeTitle != null) {
          nextMode = findMode(modeTitle);
          if (nextMode == null) {
            final String msg =
              "This sketch was last used in “" + modeTitle + "” mode,\n" +
              "which does not appear to be installed. The sketch will\n" +
              "be opened in “" + defaultMode.getTitle() + "” mode instead.";
            Base.showWarning("Depeche Mode", msg, null);
            nextMode = defaultMode;
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    Editor editor = nextMode.createEditor(this, path, location);
    if (editor == null) {
      // if it's the last editor window
//      if (editors.size() == 0 && defaultFileMenu == null) {
      // if it's not mode[0] already, then don't go into an infinite loop
      // trying to recreate a window with the default mode.
      if (nextMode == modeList[0]) {
        Base.showError("Editor Problems",
                       "An error occurred while trying to change modes.\n" +
                       "We'll have to quit for now because it's an\n" +
                       "unfortunate bit of indigestion.",
                       null);
      } else {
        editor = modeList[0].createEditor(this, path, location);
      }
    }

    // Make sure that the sketch actually loaded
    if (editor.getSketch() == null) {
//      System.err.println("sketch was null, getting out of handleOpen");
      return null;  // Just walk away quietly
    }

    editors.add(editor);

    // now that we're ready, show the window
    // (don't do earlier, cuz we might move it based on a window being closed)
    editor.setVisible(true);

    return editor;
  }


  protected Mode findMode(String title) {
    for (Mode mode : modeList) {
      if (mode.getTitle().equals(title)) {
        return mode;
      }
    }
    return null;
  }


  /**
   * Close a sketch as specified by its editor window.
   * @param editor Editor object of the sketch to be closed.
   * @return true if succeeded in closing, false if canceled.
   */
  public boolean handleClose(Editor editor, boolean modeSwitch) {
    // Check if modified
//    boolean immediate = editors.size() == 1;
    if (!editor.checkModified()) {
      return false;
    }

    // Close the running window, avoid window boogers with multiple sketches
    editor.internalCloseRunner();

    if (editors.size() == 1) {
      // For 0158, when closing the last window /and/ it was already an
      // untitled sketch, just give up and let the user quit.
//      if (Preferences.getBoolean("sketchbook.closing_last_window_quits") ||
//          (editor.untitled && !editor.getSketch().isModified())) {
      if (Base.isMacOS()) {
        // If the central menubar isn't supported on this OS X JVM,
        // we have to do the old behavior. Yuck!
        if (defaultFileMenu == null) {
          Object[] options = { "OK", "Cancel" };
          String prompt =
            "<html> " +
            "<head> <style type=\"text/css\">"+
            "b { font: 13pt \"Lucida Grande\" }"+
            "p { font: 11pt \"Lucida Grande\"; margin-top: 8px }"+
            "</style> </head>" +
            "<b>Are you sure you want to Quit?</b>" +
            "<p>Closing the last open sketch will quit Processing.";

          int result = JOptionPane.showOptionDialog(editor,
            prompt,
            "Quit",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null,
            options,
            options[0]);
          if (result == JOptionPane.NO_OPTION ||
              result == JOptionPane.CLOSED_OPTION) {
            return false;
          }
        }
      }

      // This will store the sketch count as zero
      editors.remove(editor);
      storeSketches();

      // Save out the current prefs state
      Preferences.save();

      if (defaultFileMenu == null) {
        if (modeSwitch) {
          // need to close this editor, ever so temporarily
          editor.setVisible(false);
          editor.dispose();
          activeEditor = null;
          editors.remove(editor);
        } else {
          // Since this wasn't an actual Quit event, call System.exit()
          System.exit(0);
        }
      } else {
        editor.setVisible(false);
        editor.dispose();
        defaultFileMenu.insert(sketchbookMenu, 2);
//        defaultFileMenu.insert(defaultMode.getExamplesMenu(), 3);
        activeEditor = null;
        editors.remove(editor);
      }

    } else {
      // More than one editor window open,
      // proceed with closing the current window.
      editor.setVisible(false);
      editor.dispose();
//      for (int i = 0; i < editorCount; i++) {
//        if (editor == editors[i]) {
//          for (int j = i; j < editorCount-1; j++) {
//            editors[j] = editors[j+1];
//          }
//          editorCount--;
//          // Set to null so that garbage collection occurs
//          editors[editorCount] = null;
//        }
//      }
      editors.remove(editor);
    }
    return true;
  }


  /**
   * Handler for File &rarr; Quit.
   * @return false if canceled, true otherwise.
   */
  public boolean handleQuit() {
    // If quit is canceled, this will be replaced anyway
    // by a later handleQuit() that is not canceled.
    storeSketches();

    if (handleQuitEach()) {
      // make sure running sketches close before quitting
      for (Editor editor : editors) {
        editor.internalCloseRunner();
      }
      // Save out the current prefs state
      Preferences.save();

      if (!Base.isMacOS()) {
        // If this was fired from the menu or an AppleEvent (the Finder),
        // then Mac OS X will send the terminate signal itself.
        System.exit(0);
      }
      return true;
    }
    return false;
  }


  /**
   * Attempt to close each open sketch in preparation for quitting.
   * @return false if canceled along the way
   */
  protected boolean handleQuitEach() {
    int index = 0;
    for (Editor editor : editors) {
      if (editor.checkModified()) {
        // Update to the new/final sketch path for this fella
        storeSketchPath(editor, index);
        index++;

      } else {
        return false;
      }
    }
    return true;
  }


  // .................................................................


  /**
   * Asynchronous version of menu rebuild to be used on save and rename
   * to prevent the interface from locking up until the menus are done.
   */
  protected void rebuildSketchbookMenusAsync() {
    //System.out.println("async enter");
    //new Exception().printStackTrace();
    EventQueue.invokeLater(new Runnable() {
      public void run() {
        rebuildSketchbookMenus();
      }
    });
  }


  /**
   * Synchronous version of rebuild, used when the sketchbook folder has
   * changed, so that the libraries are properly re-scanned before those menus
   * (and the examples window) are rebuilt.
   */
  protected void rebuildSketchbookMenus() {
    rebuildSketchbookMenu();
    for (Mode mode : modeList) {
      //mode.rebuildLibraryList();
      mode.rebuildImportMenu();  // calls rebuildLibraryList
      mode.rebuildToolbarMenu();
      mode.resetExamples();
    }
  }


  protected void rebuildSketchbookMenu() {
    try {
//      System.err.println("sketchbook: " + sketchbookFolder);
      sketchbookMenu.removeAll();
      addSketches(sketchbookMenu, sketchbookFolder, false);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }


  public JMenu getSketchbookMenu() {
    if (sketchbookMenu == null) {
      sketchbookMenu = new JMenu("Sketchbook");
      rebuildSketchbookMenu();
    }
    return sketchbookMenu;
  }


  /**
   * Scan a folder recursively, and add any sketches found to the menu
   * specified. Set the openReplaces parameter to true when opening the sketch
   * should replace the sketch in the current window, or false when the
   * sketch should open in a new window.
   */
  protected boolean addSketches(JMenu menu, File folder,
                                final boolean replaceExisting) throws IOException {
    // skip .DS_Store files, etc (this shouldn't actually be necessary)
    if (!folder.isDirectory()) {
      return false;
    }

    if (folder.getName().equals("libraries")) {
      return false;  // let's not go there
    }

    String[] list = folder.list();
    // If a bad folder or unreadable or whatever, this will come back null
    if (list == null) {
      return false;
    }

    // Alphabetize the list, since it's not always alpha order
    Arrays.sort(list, String.CASE_INSENSITIVE_ORDER);

    ActionListener listener = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          String path = e.getActionCommand();
          if (new File(path).exists()) {
            boolean replace = replaceExisting;
            if ((e.getModifiers() & ActionEvent.SHIFT_MASK) != 0) {
              replace = !replace;
            }
            if (replace) {
              handleOpenReplace(path);
            } else {
              handleOpen(path);
            }
          } else {
            showWarning("Sketch Disappeared",
                        "The selected sketch no longer exists.\n" +
                        "You may need to restart Processing to update\n" +
                        "the sketchbook menu.", null);
          }
        }
      };
    // offers no speed improvement
    //menu.addActionListener(listener);

    boolean found = false;

//    for (int i = 0; i < list.length; i++) {
//      if ((list[i].charAt(0) == '.') ||
//          list[i].equals("CVS")) continue;
    for (String name : list) {
      if (name.charAt(0) == '.') {
        continue;
      }

      File subfolder = new File(folder, name);
      if (subfolder.isDirectory()) {
        File entry = checkSketchFolder(subfolder, name);
        if (entry != null) {

//      File entry = new File(subfolder, list[i] + getExtension());
//      // if a .pde file of the same prefix as the folder exists..
//      if (entry.exists()) {
//        //String sanityCheck = sanitizedName(list[i]);
//        //if (!sanityCheck.equals(list[i])) {
//        if (!Sketch.isSanitaryName(list[i])) {
//          if (!builtOnce) {
//            String complaining =
//              "The sketch \"" + list[i] + "\" cannot be used.\n" +
//              "Sketch names must contain only basic letters and numbers\n" +
//              "(ASCII-only with no spaces, " +
//              "and it cannot start with a number).\n" +
//              "To get rid of this message, remove the sketch from\n" +
//              entry.getAbsolutePath();
//            Base.showMessage("Ignoring sketch with bad name", complaining);
//          }
//          continue;
//        }

          JMenuItem item = new JMenuItem(name);
          item.addActionListener(listener);
          item.setActionCommand(entry.getAbsolutePath());
          menu.add(item);
          found = true;

        } else {
          // not a sketch folder, but maybe a subfolder containing sketches
          JMenu submenu = new JMenu(name);
          // needs to be separate var otherwise would set ifound to false
          boolean anything = addSketches(submenu, subfolder, replaceExisting);
          if (anything) {
            menu.add(submenu);
            found = true;
          }
        }
      }
    }
    return found;  // actually ignored, but..
  }


  protected boolean addSketches(DefaultMutableTreeNode node, File folder) throws IOException {
    // skip .DS_Store files, etc (this shouldn't actually be necessary)
    if (!folder.isDirectory()) {
      return false;
    }

    if (folder.getName().equals("libraries")) {
      return false;  // let's not go there
    }

    String[] list = folder.list();
    // If a bad folder or unreadable or whatever, this will come back null
    if (list == null) {
      return false;
    }

    // Alphabetize the list, since it's not always alpha order
    Arrays.sort(list, String.CASE_INSENSITIVE_ORDER);

//    ActionListener listener = new ActionListener() {
//        public void actionPerformed(ActionEvent e) {
//          String path = e.getActionCommand();
//          if (new File(path).exists()) {
//            handleOpen(path);
//          } else {
//            showWarning("Sketch Disappeared",
//                        "The selected sketch no longer exists.\n" +
//                        "You may need to restart Processing to update\n" +
//                        "the sketchbook menu.", null);
//          }
//        }
//    };
    // offers no speed improvement
    //menu.addActionListener(listener);

    boolean found = false;

    for (String name : list) {
      if (name.charAt(0) == '.') {
        continue;
      }

//      JTree tree = null;
//      TreePath[] a = tree.getSelectionPaths();
//      for (TreePath path : a) {
//        Object[] o = path.getPath();
//      }

      File subfolder = new File(folder, name);
      if (subfolder.isDirectory()) {
        File entry = checkSketchFolder(subfolder, name);
        if (entry != null) {
//          DefaultMutableTreeNode item = new DefaultMutableTreeNode(name);
          DefaultMutableTreeNode item =
            new DefaultMutableTreeNode(new SketchReference(name, entry));
//          item.addActionListener(listener);
//          item.setActionCommand(entry.getAbsolutePath());
//          menu.add(item);
          node.add(item);
          found = true;

        } else {
          // not a sketch folder, but maybe a subfolder containing sketches
//          JMenu submenu = new JMenu(name);
          DefaultMutableTreeNode subnode = new DefaultMutableTreeNode(name);
          // needs to be separate var otherwise would set ifound to false
          boolean anything = addSketches(subnode, subfolder);
          if (anything) {
            node.add(subnode);
            found = true;
          }
        }
      }
    }
    return found;
  }


  /**
   * Check through the various modes and see if this is a legit sketch.
   * Because the default mode will be the first in the list, this will always
   * prefer that one over the others.
   */
  File checkSketchFolder(File subfolder, String item) {
    for (Mode mode : modeList) {
      File entry = new File(subfolder, item + "." + mode.getDefaultExtension());
      // if a .pde file of the same prefix as the folder exists..
      if (entry.exists()) {
        return entry;
      }
      // for the new releases, don't bother lecturing.. just ignore the sketch
      /*
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
      */
    }
    return null;
  }


  // .................................................................


  /**
   * Show the About box.
   */
  public void handleAbout() {
    final Image image = Base.getLibImage("about.jpg", activeEditor);
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
   * Show the Preferences window.
   */
  public void handlePrefs() {
    if (preferencesFrame == null) preferencesFrame = new Preferences();
    preferencesFrame.showFrame(activeEditor);
  }

  /**
   * Show the library installer window.
   */
  public void handleAddOrRemoveLibrary() {
    libraryManagerFrame.showFrame(activeEditor);
  }
  
  public void handleShowUpdates() {
    libraryManagerFrame.showFrame(activeEditor);
    libraryManagerFrame.setFilterText("has:updates");
  }
  
  /**
   * Installed the libraries in the given file after a confirmation from the
   * user. Returns the number of libraries installed.
   */
  public boolean handleConfirmAndInstallLibrary(File libFile) {
    return libraryManagerFrame.confirmAndInstallLibrary(activeEditor, libFile) != null;
  }

  // ...................................................................


  /**
   * Get list of platform constants.
   */
//  static public int[] getPlatforms() {
//    return platforms;
//  }


//  static public int getPlatform() {
//    String osname = System.getProperty("os.name");
//
//    if (osname.indexOf("Mac") != -1) {
//      return PConstants.MACOSX;
//
//    } else if (osname.indexOf("Windows") != -1) {
//      return PConstants.WINDOWS;
//
//    } else if (osname.equals("Linux")) {  // true for the ibm vm
//      return PConstants.LINUX;
//
//    } else {
//      return PConstants.OTHER;
//    }
//  }


  static public Platform getPlatform() {
    return platform;
  }


  static public String getPlatformName() {
    return PConstants.platformNames[PApplet.platform];
  }

  /*
  static public String getPlatformName() {
    String osname = System.getProperty("os.name");

    if (osname.indexOf("Mac") != -1) {
      return "macosx";

    } else if (osname.indexOf("Windows") != -1) {
      return "windows";

    } else if (osname.equals("Linux")) {  // true for the ibm vm
      return "linux";

    } else {
      return "other";
    }
  }
  */


  /**
   * Map a platform constant to its name.
   * @param which PConstants.WINDOWS, PConstants.MACOSX, PConstants.LINUX
   * @return one of "windows", "macosx", or "linux"
   */
  static public String getPlatformName(int which) {
    return platformNames.get(which);
  }


  static public int getPlatformIndex(String what) {
    Integer entry = platformIndices.get(what);
    return (entry == null) ? -1 : entry.intValue();
  }


  // These were changed to no longer rely on PApplet and PConstants because
  // of conflicts that could happen with older versions of core.jar, where
  // the MACOSX constant would instead read as the LINUX constant.


  /**
   * returns true if Processing is running on a Mac OS X machine.
   */
  static public boolean isMacOS() {
    //return PApplet.platform == PConstants.MACOSX;
    return System.getProperty("os.name").indexOf("Mac") != -1;
  }


  /**
   * returns true if running on windows.
   */
  static public boolean isWindows() {
    //return PApplet.platform == PConstants.WINDOWS;
    return System.getProperty("os.name").indexOf("Windows") != -1;
  }


  /**
   * true if running on linux.
   */
  static public boolean isLinux() {
    //return PApplet.platform == PConstants.LINUX;
    return System.getProperty("os.name").indexOf("Linux") != -1;
  }


  // .................................................................


  static public File getSettingsFolder() {
    File settingsFolder = null;

    String preferencesPath = Preferences.get("settings.path");
    if (preferencesPath != null) {
      settingsFolder = new File(preferencesPath);

    } else {
      try {
        settingsFolder = platform.getSettingsFolder();
      } catch (Exception e) {
        showError("Problem getting data folder",
                  "Error getting the Processing data folder.", e);
      }
    }

    // create the folder if it doesn't exist already
    if (!settingsFolder.exists()) {
      if (!settingsFolder.mkdirs()) {
        showError("Settings issues",
                  "Processing cannot run because it could not\n" +
                  "create a folder to store your settings.", null);
      }
    }
    return settingsFolder;
  }


  /**
   * Convenience method to get a File object for the specified filename inside
   * the settings folder.
   * For now, only used by Preferences to get the preferences.txt file.
   * @param filename A file inside the settings folder.
   * @return filename wrapped as a File object inside the settings folder
   */
  static public File getSettingsFile(String filename) {
    return new File(getSettingsFolder(), filename);
  }


  /*
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
  */


  /**
   * Create a temporary folder by using the createTempFile() mechanism,
   * deleting the file it creates, and making a folder using the location
   * that was provided.
   */
  static public File createTempFolder(String prefix, String suffix) throws IOException {
    File folder = File.createTempFile(prefix, suffix);
    // Now delete that file and create a folder in its place
    folder.delete();
    folder.mkdirs();
    // And send the folder back to your friends
    return folder;
  }


//  static public String getExamplesPath() {
//    return examplesFolder.getAbsolutePath();
//  }

//  public File getExamplesFolder() {
//    return examplesFolder;
//  }


//  static public String getLibrariesPath() {
//    return librariesFolder.getAbsolutePath();
//  }


//  public File getLibrariesFolder() {
//    return librariesFolder;
//  }


//  static public File getToolsFolder() {
  public File getToolsFolder() {
    return toolsFolder;
  }


//  static public String getToolsPath() {
//    return toolsFolder.getAbsolutePath();
//  }


  protected void determineSketchbookFolder() {
    // If a value is at least set, first check to see if the folder exists.
    // If it doesn't, warn the user that the sketchbook folder is being reset.
    String sketchbookPath = Preferences.get("sketchbook.path");
    if (sketchbookPath != null) {
      sketchbookFolder = new File(sketchbookPath);
      if (!sketchbookFolder.exists()) {
        Base.showWarning("Sketchbook folder disappeared",
                         "The sketchbook folder no longer exists.\n" +
                         "Processing will switch to the default sketchbook\n" +
                         "location, and create a new sketchbook folder if\n" +
                         "necessary. Procesing will then stop talking about\n" +
                         "himself in the third person.", null);
        sketchbookFolder = null;
      }
    }

    // If no path is set, get the default sketchbook folder for this platform
    if (sketchbookFolder == null) {
      sketchbookFolder = getDefaultSketchbookFolder();
      Preferences.set("sketchbook.path", sketchbookFolder.getAbsolutePath());
      if (!sketchbookFolder.exists()) {
        sketchbookFolder.mkdirs();
      }
    }
//    System.err.println("sketchbook: " + sketchbookFolder);
  }


  public void setSketchbookFolder(File folder) {
    sketchbookFolder = folder;
    Preferences.set("sketchbook.path", folder.getAbsolutePath());
    rebuildSketchbookMenus();
  }


  public File getSketchbookFolder() {
//    return new File(Preferences.get("sketchbook.path"));
    return sketchbookFolder;
  }


  public File getSketchbookLibrariesFolder() {
//    return new File(getSketchbookFolder(), "libraries");
    return new File(sketchbookFolder, "libraries");
  }


  public File getSketchbookToolsFolder() {
    return new File(sketchbookFolder, "tools");
  }


  static protected File getDefaultSketchbookFolder() {
    File sketchbookFolder = null;
    try {
      sketchbookFolder = platform.getDefaultSketchbookFolder();
    } catch (Exception e) { }

    if (sketchbookFolder == null) {
      sketchbookFolder = promptSketchbookLocation();
    }

    // create the folder if it doesn't exist already
    boolean result = true;
    if (!sketchbookFolder.exists()) {
      result = sketchbookFolder.mkdirs();
    }

    if (!result) {
      showError("You forgot your sketchbook",
                "Processing cannot run because it could not\n" +
                "create a folder to store your sketchbook.", null);
    }

    return sketchbookFolder;
  }


  /**
   * Check for a new sketchbook location.
   */
  static protected File promptSketchbookLocation() {
    // Most often this will happen on Linux, so default to their home dir.
    File folder = new File(System.getProperty("user.home"), "sketchbook");
    String prompt = "Select (or create new) folder for sketches...";
    folder = Base.selectFolder(prompt, folder, null);
    if (folder == null) {
      System.exit(0);
    }
    // Create the folder if it doesn't exist already
    if (!folder.exists()) {
      folder.mkdirs();
      return folder;
    }
    return folder;
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
    try {
      platform.openURL(url);

    } catch (Exception e) {
      showWarning("Problem Opening URL",
                  "Could not open the URL\n" + url, e);
    }
  }


  /**
   * Used to determine whether to disable the "Show Sketch Folder" option.
   * @return true If a means of opening a folder is known to be available.
   */
  static protected boolean openFolderAvailable() {
    return platform.openFolderAvailable();
  }


  /**
   * Implements the other cross-platform headache of opening
   * a folder in the machine's native file browser.
   */
  static public void openFolder(File file) {
    try {
      platform.openFolder(file);

    } catch (Exception e) {
      showWarning("Problem Opening Folder",
                  "Could not open the folder\n" + file.getAbsolutePath(), e);
    }
  }


  // .................................................................


  /**
   * Prompt for a fodler and return it as a File object (or null).
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


  // .................................................................


  /**
   * Give this Frame a Processing icon.
   */
  static public void setIcon(Frame frame) {
    Image image = Toolkit.getDefaultToolkit().createImage(PApplet.ICON_IMAGE);
    frame.setIconImage(image);
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
  static public void registerWindowCloseKeys(JRootPane root,
                                             ActionListener disposer) {
    KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
    root.registerKeyboardAction(disposer, stroke,
                                JComponent.WHEN_IN_FOCUSED_WINDOW);

    int modifiers = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
    stroke = KeyStroke.getKeyStroke('W', modifiers);
    root.registerKeyboardAction(disposer, stroke,
                                JComponent.WHEN_IN_FOCUSED_WINDOW);
  }


  // .................................................................


  /**
   * "No cookie for you" type messages. Nothing fatal or all that
   * much of a bummer, but something to notify the user about.
   */
  static public void showMessage(String title, String message) {
    if (title == null) title = "Message";

    if (commandLine) {
      System.out.println(title + ": " + message);

    } else {
      JOptionPane.showMessageDialog(new Frame(), message, title,
                                    JOptionPane.INFORMATION_MESSAGE);
    }
  }


  /**
   * Non-fatal error message with optional stack trace side dish.
   */
  static public void showWarning(String title, String message, Exception e) {
    if (title == null) title = "Warning";

    if (commandLine) {
      System.out.println(title + ": " + message);

    } else {
      JOptionPane.showMessageDialog(new Frame(), message, title,
                                    JOptionPane.WARNING_MESSAGE);
    }
    if (e != null) e.printStackTrace();
  }


  /**
   * Non-fatal error message with optional stack trace side dish.
   */
  static public void showWarningTiered(String title,
                                       String primary, String secondary,
                                       Exception e) {
    if (title == null) title = "Warning";

    final String message = primary + "\n" + secondary;
    if (commandLine) {
      System.out.println(title + ": " + message);

    } else {
//      JOptionPane.showMessageDialog(new Frame(), message,
//                                    title, JOptionPane.WARNING_MESSAGE);
      if (!Base.isMacOS()) {
        JOptionPane.showMessageDialog(new JFrame(),
                                      "<html><body>" +
                                      "<b>" + primary + "</b>" +
                                      "<br>" + secondary, title,
                                      JOptionPane.WARNING_MESSAGE);
      } else {
        // Pane formatting adapted from the Quaqua guide
        // http://www.randelshofer.ch/quaqua/guide/joptionpane.html
        JOptionPane pane =
          new JOptionPane("<html> " +
                          "<head> <style type=\"text/css\">"+
                          "b { font: 13pt \"Lucida Grande\" }"+
                          "p { font: 11pt \"Lucida Grande\"; margin-top: 8px }"+
                          "</style> </head>" +
                          "<b>" + primary + "</b>" +
                          "<p>" + secondary + "</p>",
                          JOptionPane.WARNING_MESSAGE);

//        String[] options = new String[] {
//            "Yes", "No"
//        };
//        pane.setOptions(options);

        // highlight the safest option ala apple hig
//        pane.setInitialValue(options[0]);

        JDialog dialog = pane.createDialog(new JFrame(), null);
        dialog.setVisible(true);

//        Object result = pane.getValue();
//        if (result == options[0]) {
//          return JOptionPane.YES_OPTION;
//        } else if (result == options[1]) {
//          return JOptionPane.NO_OPTION;
//        } else {
//          return JOptionPane.CLOSED_OPTION;
//        }
      }
    }
    if (e != null) e.printStackTrace();
  }


  /**
   * Show an error message that's actually fatal to the program.
   * This is an error that can't be recovered. Use showWarning()
   * for errors that allow P5 to continue running.
   */
  static public void showError(String title, String message, Throwable e) {
    if (title == null) title = "Error";

    if (commandLine) {
      System.err.println(title + ": " + message);

    } else {
      JOptionPane.showMessageDialog(new Frame(), message, title,
                                    JOptionPane.ERROR_MESSAGE);
    }
    if (e != null) e.printStackTrace();
    System.exit(1);
  }


  // ...................................................................



  // incomplete
  static public int showYesNoCancelQuestion(Editor editor, String title,
                                            String primary, String secondary) {
    if (!Base.isMacOS()) {
      int result =
        JOptionPane.showConfirmDialog(null, primary + "\n" + secondary, title,
                                      JOptionPane.YES_NO_CANCEL_OPTION,
                                      JOptionPane.QUESTION_MESSAGE);
      return result;
//    if (result == JOptionPane.YES_OPTION) {
//
//    } else if (result == JOptionPane.NO_OPTION) {
//      return true;  // ok to continue
//
//    } else if (result == JOptionPane.CANCEL_OPTION) {
//      return false;
//
//    } else {
//      throw new IllegalStateException();
//    }

    } else {
      // Pane formatting adapted from the Quaqua guide
      // http://www.randelshofer.ch/quaqua/guide/joptionpane.html
      JOptionPane pane =
        new JOptionPane("<html> " +
                        "<head> <style type=\"text/css\">"+
                        "b { font: 13pt \"Lucida Grande\" }"+
                        "p { font: 11pt \"Lucida Grande\"; margin-top: 8px }"+
                        "</style> </head>" +
                        "<b>Do you want to save changes to this sketch<BR>" +
                        " before closing?</b>" +
                        "<p>If you don't save, your changes will be lost.",
                        JOptionPane.QUESTION_MESSAGE);

      String[] options = new String[] {
          "Save", "Cancel", "Don't Save"
      };
      pane.setOptions(options);

      // highlight the safest option ala apple hig
      pane.setInitialValue(options[0]);

      // on macosx, setting the destructive property places this option
      // away from the others at the lefthand side
      pane.putClientProperty("Quaqua.OptionPane.destructiveOption",
                             new Integer(2));

      JDialog dialog = pane.createDialog(editor, null);
      dialog.setVisible(true);

      Object result = pane.getValue();
      if (result == options[0]) {
        return JOptionPane.YES_OPTION;
      } else if (result == options[1]) {
        return JOptionPane.CANCEL_OPTION;
      } else if (result == options[2]) {
        return JOptionPane.NO_OPTION;
      } else {
        return JOptionPane.CLOSED_OPTION;
      }
    }
  }


//if (result == JOptionPane.YES_OPTION) {
  //
//      } else if (result == JOptionPane.NO_OPTION) {
//        return true;  // ok to continue
  //
//      } else if (result == JOptionPane.CANCEL_OPTION) {
//        return false;
  //
//      } else {
//        throw new IllegalStateException();
//      }

  static public int showYesNoQuestion(Frame editor, String title,
                                      String primary, String secondary) {
    if (!Base.isMacOS()) {
      return JOptionPane.showConfirmDialog(editor,
                                           "<html><body>" +
                                           "<b>" + primary + "</b>" +
                                           "<br>" + secondary, title,
                                           JOptionPane.YES_NO_OPTION,
                                           JOptionPane.QUESTION_MESSAGE);
    } else {
      // Pane formatting adapted from the Quaqua guide
      // http://www.randelshofer.ch/quaqua/guide/joptionpane.html
      JOptionPane pane =
        new JOptionPane("<html> " +
                        "<head> <style type=\"text/css\">"+
                        "b { font: 13pt \"Lucida Grande\" }"+
                        "p { font: 11pt \"Lucida Grande\"; margin-top: 8px }"+
                        "</style> </head>" +
                        "<b>" + primary + "</b>" +
                        "<p>" + secondary + "</p>",
                        JOptionPane.QUESTION_MESSAGE);

      String[] options = new String[] {
          "Yes", "No"
      };
      pane.setOptions(options);

      // highlight the safest option ala apple hig
      pane.setInitialValue(options[0]);

      JDialog dialog = pane.createDialog(editor, null);
      dialog.setVisible(true);

      Object result = pane.getValue();
      if (result == options[0]) {
        return JOptionPane.YES_OPTION;
      } else if (result == options[1]) {
        return JOptionPane.NO_OPTION;
      } else {
        return JOptionPane.CLOSED_OPTION;
      }
    }
  }


  /**
   * Retrieve a path to something in the Processing folder. Eventually this
   * may refer to the Contents subfolder of Processing.app, if we bundle things
   * up as a single .app file with no additional folders.
   */
//  static public String getContentsPath(String filename) {
//    String basePath = System.getProperty("user.dir");
//    /*
//      // do this later, when moving to .app package
//    if (PApplet.platform == PConstants.MACOSX) {
//      basePath = System.getProperty("processing.contents");
//    }
//    */
//    return basePath + File.separator + filename;
//  }


  /**
   * Get a path for something in the Processing lib folder.
   */
  /*
  static public String getLibContentsPath(String filename) {
    String libPath = getContentsPath("lib/" + filename);
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
  */

  static public File getContentFile(String name) {
    String path = System.getProperty("user.dir");

    // Get a path to somewhere inside the .app folder
    if (Base.isMacOS()) {
//      <key>javaroot</key>
//      <string>$JAVAROOT</string>
      String javaroot = System.getProperty("javaroot");
      if (javaroot != null) {
        path = javaroot;
      }
    }
    File working = new File(path);
    return new File(working, name);
  }


  /**
   * Get an image associated with the current color theme.
   * @deprecated
   */
  static public Image getThemeImage(String name, Component who) {
    return getLibImage("theme/" + name, who);
  }


  /**
   * Return an Image object from inside the Processing lib folder.
   * @deprecated
   */
  static public Image getLibImage(String name, Component who) {
    Image image = null;
    Toolkit tk = Toolkit.getDefaultToolkit();

    File imageLocation = new File(getContentFile("lib"), name);
    image = tk.getImage(imageLocation.getAbsolutePath());
    MediaTracker tracker = new MediaTracker(who);
    tracker.addImage(image, 0);
    try {
      tracker.waitForAll();
    } catch (InterruptedException e) { }
    return image;
  }


  /**
   * Return an InputStream for a file inside the Processing lib folder.
   */
  static public InputStream getLibStream(String filename) throws IOException {
    return new FileInputStream(new File(getContentFile("lib"), filename));
  }


  // ...................................................................


  /**
   * Get the number of lines in a file by counting the number of newline
   * characters inside a String (and adding 1).
   */
  static public int countLines(String what) {
    int count = 1;
    for (char c : what.toCharArray()) {
      if (c == '\n') count++;
    }
    return count;
  }


  /**
   * Same as PApplet.loadBytes(), however never does gzip decoding.
   */
  static public byte[] loadBytesRaw(File file) throws IOException {
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



  /**
   * Read from a file with a bunch of attribute/value pairs
   * that are separated by = and ignore comments with #.
   */
  static public HashMap<String,String> readSettings(File inputFile) {
    HashMap<String,String> outgoing = new HashMap<String,String>();
    if (inputFile.exists()) {
      String lines[] = PApplet.loadStrings(inputFile);
      for (int i = 0; i < lines.length; i++) {
        int hash = lines[i].indexOf('#');
        String line = (hash == -1) ?
          lines[i].trim() : lines[i].substring(0, hash).trim();
          if (line.length() == 0) continue;

        int equals = line.indexOf('=');
        if (equals == -1) {
          System.err.println("ignoring illegal line in " + inputFile);
          System.err.println("  " + line);
          continue;
        }
        String attr = line.substring(0, equals).trim();
        String valu = line.substring(equals + 1).trim();
        outgoing.put(attr, valu);
      }
    }
    return outgoing;
  }


  static public void copyFile(File sourceFile,
                              File targetFile) throws IOException {
    InputStream from =
      new BufferedInputStream(new FileInputStream(sourceFile));
    OutputStream to =
      new BufferedOutputStream(new FileOutputStream(targetFile));
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

    targetFile.setLastModified(sourceFile.lastModified());
  }


  /**
   * Grab the contents of a file as a string.
   */
  static public String loadFile(File file) throws IOException {
    String[] contents = PApplet.loadStrings(file);
    if (contents == null) return null;
    return PApplet.join(contents, "\n");
  }


  /**
   * Spew the contents of a String object out to a file.
   */
  static public void saveFile(String str, File file) throws IOException {
    File temp = File.createTempFile(file.getName(), null, file.getParentFile());
    try{
      // fix from cjwant to prevent symlinks from being destroyed.
      File canon = file.getCanonicalFile();
      file = canon;
    } catch (IOException e) {
      throw new IOException("Could not resolve canonical representation of " +
                            file.getAbsolutePath());
    }
    PApplet.saveStrings(temp, new String[] { str });
    if (file.exists()) {
      boolean result = file.delete();
      if (!result) {
        throw new IOException("Could not remove old version of " +
                              file.getAbsolutePath());
      }
    }
    boolean result = temp.renameTo(file);
    if (!result) {
      throw new IOException("Could not replace " +
                            file.getAbsolutePath());
    }
  }


  /**
   * Copy a folder from one place to another. This ignores all dot files and
   * folders found in the source directory, to avoid copying silly .DS_Store
   * files and potentially troublesome .svn folders.
   */
  static public void copyDir(File sourceDir,
                             File targetDir) throws IOException {
    if (sourceDir.equals(targetDir)) {
      final String urDum = "source and target directories are identical";
      throw new IllegalArgumentException(urDum);
    }
    targetDir.mkdirs();
    String files[] = sourceDir.list();
    for (int i = 0; i < files.length; i++) {
      // Ignore dot files (.DS_Store), dot folders (.svn) while copying
      if (files[i].charAt(0) == '.') continue;
      //if (files[i].equals(".") || files[i].equals("..")) continue;
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
   * Recursively creates a list of all files within the specified folder,
   * and returns a list of their relative paths.
   * Ignores any files/folders prefixed with a dot.
   */
//  static public String[] listFiles(String path, boolean relative) {
//    return listFiles(new File(path), relative);
//  }


  static public String[] listFiles(File folder, boolean relative) {
    String path = folder.getAbsolutePath();
    Vector<String> vector = new Vector<String>();
    listFiles(relative ? (path + File.separator) : "", path, null, vector);
    String outgoing[] = new String[vector.size()];
    vector.copyInto(outgoing);
    return outgoing;
  }


  static public String[] listFiles(File folder, boolean relative, String extension) {
    String path = folder.getAbsolutePath();
    Vector<String> vector = new Vector<String>();
    if (extension != null) {
      if (!extension.startsWith(".")) {
        extension = "." + extension;
      }
    }
    listFiles(relative ? (path + File.separator) : "", path, extension, vector);
    String outgoing[] = new String[vector.size()];
    vector.copyInto(outgoing);
    return outgoing;
  }


  static protected void listFiles(String basePath,
                                  String path, String extension,
                                  Vector<String> vector) {
    File folder = new File(path);
    String[] list = folder.list();
    if (list != null) {
      for (String item : list) {
        if (item.charAt(0) == '.') continue;
        if (extension == null || item.toLowerCase().endsWith(extension)) {
          File file = new File(path, item);
          String newPath = file.getAbsolutePath();
          if (newPath.startsWith(basePath)) {
            newPath = newPath.substring(basePath.length());
          }
          vector.add(newPath);
          if (file.isDirectory()) {
            listFiles(basePath, newPath, extension, vector);
          }
        }
      }
    }
  }


  /////////////////////////////////////////////////////////////////////////////


  /**
   * Given a folder, return a list of absolute paths to all jar or zip files
   * inside that folder, separated by pathSeparatorChar.
   *
   * This will prepend a colon (or whatever the path separator is)
   * so that it can be directly appended to another path string.
   *
   * As of 0136, this will no longer add the root folder as well.
   *
   * This function doesn't bother checking to see if there are any .class
   * files in the folder or within a subfolder.
   */
  static public String contentsToClassPath(File folder) {
    if (folder == null) return "";

    StringBuffer abuffer = new StringBuffer();
    String sep = System.getProperty("path.separator");

    try {
      String path = folder.getCanonicalPath();

//    disabled as of 0136
      // add the folder itself in case any unzipped files
//      abuffer.append(sep);
//      abuffer.append(path);
//
      // When getting the name of this folder, make sure it has a slash
      // after it, so that the names of sub-items can be added.
      if (!path.endsWith(File.separator)) {
        path += File.separator;
      }

      String list[] = folder.list();
      for (int i = 0; i < list.length; i++) {
        // Skip . and ._ files. Prior to 0125p3, .jar files that had
        // OS X AppleDouble files associated would cause trouble.
        if (list[i].startsWith(".")) continue;

        if (list[i].toLowerCase().endsWith(".jar") ||
            list[i].toLowerCase().endsWith(".zip")) {
          abuffer.append(sep);
          abuffer.append(path);
          abuffer.append(list[i]);
        }
      }
    } catch (IOException e) {
      e.printStackTrace();  // this would be odd
    }
    //System.out.println("included path is " + abuffer.toString());
    //packageListFromClassPath(abuffer.toString());  // WHY?
    return abuffer.toString();
  }


  /**
   * A classpath, separated by the path separator, will contain
   * a series of .jar/.zip files or directories containing .class
   * files, or containing subdirectories that have .class files.
   *
   * @param path the input classpath
   * @return array of possible package names
   */
  static public String[] packageListFromClassPath(String path) {
    Hashtable table = new Hashtable();
    String pieces[] =
      PApplet.split(path, File.pathSeparatorChar);

    for (int i = 0; i < pieces.length; i++) {
      //System.out.println("checking piece '" + pieces[i] + "'");
      if (pieces[i].length() == 0) continue;

      if (pieces[i].toLowerCase().endsWith(".jar") ||
          pieces[i].toLowerCase().endsWith(".zip")) {
        //System.out.println("checking " + pieces[i]);
        packageListFromZip(pieces[i], table);

      } else {  // it's another type of file or directory
        File dir = new File(pieces[i]);
        if (dir.exists() && dir.isDirectory()) {
          packageListFromFolder(dir, null, table);
          //importCount = magicImportsRecursive(dir, null,
          //                                  table);
                                              //imports, importCount);
        }
      }
    }
    int tableCount = table.size();
    String output[] = new String[tableCount];
    int index = 0;
    Enumeration e = table.keys();
    while (e.hasMoreElements()) {
      output[index++] = ((String) e.nextElement()).replace('/', '.');
    }
    //System.arraycopy(imports, 0, output, 0, importCount);
    //PApplet.printarr(output);
    return output;
  }


  static private void packageListFromZip(String filename, Hashtable table) {
    try {
      ZipFile file = new ZipFile(filename);
      Enumeration entries = file.entries();
      while (entries.hasMoreElements()) {
        ZipEntry entry = (ZipEntry) entries.nextElement();

        if (!entry.isDirectory()) {
          String name = entry.getName();

          if (name.endsWith(".class")) {
            int slash = name.lastIndexOf('/');
            if (slash == -1) continue;

            String pname = name.substring(0, slash);
            if (table.get(pname) == null) {
              table.put(pname, new Object());
            }
          }
        }
      }
      file.close();
    } catch (IOException e) {
      System.err.println("Ignoring " + filename + " (" + e.getMessage() + ")");
      //e.printStackTrace();
    }
  }


  /**
   * Make list of package names by traversing a directory hierarchy.
   * Each time a class is found in a folder, add its containing set
   * of folders to the package list. If another folder is found,
   * walk down into that folder and continue.
   */
  static private void packageListFromFolder(File dir, String sofar,
                                            Hashtable table) {
                                          //String imports[],
                                          //int importCount) {
    //System.err.println("checking dir '" + dir + "'");
    boolean foundClass = false;
    String files[] = dir.list();

    for (int i = 0; i < files.length; i++) {
      if (files[i].equals(".") || files[i].equals("..")) continue;

      File sub = new File(dir, files[i]);
      if (sub.isDirectory()) {
        String nowfar =
          (sofar == null) ? files[i] : (sofar + "." + files[i]);
        packageListFromFolder(sub, nowfar, table);
        //System.out.println(nowfar);
        //imports[importCount++] = nowfar;
        //importCount = magicImportsRecursive(sub, nowfar,
        //                                  imports, importCount);
      } else if (!foundClass) {  // if no classes found in this folder yet
        if (files[i].endsWith(".class")) {
          //System.out.println("unique class: " + files[i] + " for " + sofar);
          table.put(sofar, new Object());
          foundClass = true;
        }
      }
    }
  }
}
