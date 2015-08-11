/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2012-15 The Processing Foundation
  Copyright (c) 2004-12 Ben Fry and Casey Reas
  Copyright (c) 2001-04 Massachusetts Institute of Technology

  This program is free software; you can redistribute it and/or
  modify it under the terms of the GNU General Public License
  version 2, as published by the Free Software Foundation.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software Foundation,
  Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package processing.app;

import java.awt.EventQueue;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.tree.DefaultMutableTreeNode;

import processing.app.contrib.*;
import processing.app.ui.*;
import processing.core.*;
import processing.data.StringList;


/**
 * The base class for the main processing application.
 * Primary role of this class is for platform identification and
 * general interaction with the system (launching URLs, loading
 * files and images, etc) that comes from that.
 */
public class Base {
  // Added accessors for 0218 because the UpdateCheck class was not properly
  // updating the values, due to javac inlining the static final values.
  static private final int REVISION = 242;
  /** This might be replaced by main() if there's a lib/version.txt file. */
  static private String VERSION_NAME = "0242"; //$NON-NLS-1$
  /** Set true if this a proper release rather than a numbered revision. */

  /** True if heavy debugging error/log messages are enabled */
  static public boolean DEBUG = false;
//  static public boolean DEBUG = true;

  static HashMap<Integer, String> platformNames =
    new HashMap<Integer, String>();
  static {
    platformNames.put(PConstants.WINDOWS, "windows"); //$NON-NLS-1$
    platformNames.put(PConstants.MACOSX, "macosx"); //$NON-NLS-1$
    platformNames.put(PConstants.LINUX, "linux"); //$NON-NLS-1$
  }

  static HashMap<String, Integer> platformIndices = new HashMap<String, Integer>();
  static {
    platformIndices.put("windows", PConstants.WINDOWS); //$NON-NLS-1$
    platformIndices.put("macosx", PConstants.MACOSX); //$NON-NLS-1$
    platformIndices.put("linux", PConstants.LINUX); //$NON-NLS-1$
  }
  static Platform platform;

  /** How many bits this machine is */
  static int nativeBits;
  static {
    nativeBits = 32;  // perhaps start with 32
    String bits = System.getProperty("sun.arch.data.model"); //$NON-NLS-1$
    if (bits != null) {
      if (bits.equals("64")) { //$NON-NLS-1$
        nativeBits = 64;
      }
    } else {
      // if some other strange vm, maybe try this instead
      if (System.getProperty("java.vm.name").contains("64")) { //$NON-NLS-1$ //$NON-NLS-2$
        nativeBits = 64;
      }
    }
  }

  static String nativeArch = System.getProperty("os.arch");

  static private boolean commandLine;

  // A single instance of the preferences window
  PreferencesFrame preferencesFrame;

  // A single instance of the library manager window
  ContributionManagerDialog contributionManagerFrame;

  // Location for untitled items
  static File untitledFolder;

  /** List of currently active editors. */
  protected List<Editor> editors =
    Collections.synchronizedList(new ArrayList<Editor>());
  protected Editor activeEditor;
  /** A lone file menu to be used when all sketch windows are closed. */
  static public JMenu defaultFileMenu;

  /**
   * Starts with the last mode used with the environment,
   * or the default mode if not used.
   */
  private Mode nextMode;

  /** The built-in modes. coreModes[0] will be considered the 'default'. */
  private Mode[] coreModes;

  protected ArrayList<ModeContribution> modeContribs;
  protected ArrayList<ExamplesContribution> exampleContribs;

  private JMenu sketchbookMenu;

  private Recent recent;

  // Used by handleOpen(), this saves the chooser to remember the directory.
  // Doesn't appear to be necessary with the AWT native file dialog.
  // https://github.com/processing/processing/pull/2366
  private JFileChooser openChooser;

  static protected File sketchbookFolder;
//  protected File toolsFolder;


  static public void main(final String[] args) {
    EventQueue.invokeLater(new Runnable() {
        public void run() {
          try {
            createAndShowGUI(args);
          } catch (Throwable t) {
            showBadnessTrace("It was not meant to be",
                             "A serious problem happened during startup. Please report:\n" +
                             "http://github.com/processing/processing/issues/new", t, true);
          }
        }
    });
  }


  static private void createAndShowGUI(String[] args) {
    try {
      File versionFile = getContentFile("lib/version.txt"); //$NON-NLS-1$
      if (versionFile.exists()) {
        String version = PApplet.loadStrings(versionFile)[0];
        if (!version.equals(VERSION_NAME)) {
          VERSION_NAME = version;
//          RELEASE = true;
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    initPlatform();

    // Use native popups so they don't look so crappy on OS X
    JPopupMenu.setDefaultLightWeightPopupEnabled(false);

    // Don't put anything above this line that might make GUI,
    // because the platform has to be inited properly first.

    // Make sure a full JDK is installed
    //initRequirements();

    // Load the languages
    Language.init();

    // run static initialization that grabs all the prefs
    Preferences.init();

    if (!SingleInstance.alreadyRunning(args)) {
      // Set the look and feel before opening the window
      try {
        platform.setLookAndFeel();
      } catch (Exception e) {
        loge("Could not set the Look & Feel", e); //$NON-NLS-1$
      }

      boolean sketchbookPrompt = false;
      if (Preferences.getBoolean("welcome.show")) {
        if (!Preferences.getBoolean("welcome.seen")) {
          // Check if there's a 2.0 sketchbook present
          String oldPath = Preferences.getOldSketchbookPath();
          if (oldPath != null) {
            String newPath = Preferences.getSketchbookPath();
            // If newPath is null, this is the first run of any 3.x version
            if (newPath == null) {
              sketchbookPrompt = true;

            } else if (oldPath.equals(newPath)) {
              // If both exist and are identical, then the user has been using
              // alpha releases of 3.x and needs to be warned about the larger
              // changes in this release.
              sketchbookPrompt = true;
            }
          }
        }
      }

      // Get the sketchbook path, and make sure it's set properly
      locateSketchbookFolder();


      // Create a location for untitled sketches
      try {
        untitledFolder = Base.createTempFolder("untitled", "sketches", null);
        untitledFolder.deleteOnExit();
      } catch (IOException e) {
        Base.showError("Trouble without a name",
                       "Could not create a place to store untitled sketches.\n" +
                       "That's gonna prevent us from continuing.", e);
      }

      log("about to create base..."); //$NON-NLS-1$
      try {
        final Base base = new Base(args);
        // Prevent more than one copy of the PDE from running.
        SingleInstance.startServer(base);

        // Needs to be shown after the first editor window opens, so that it
        // shows up on top, and doesn't prevent an editor window from opening.
        if (Preferences.getBoolean("welcome.show")) {
          final boolean prompt = sketchbookPrompt;
          EventQueue.invokeLater(new Runnable() {
            public void run() {
              try {
                new Welcome(base, prompt);
              } catch (IOException e) {
                Base.showBadnessTrace("Unwelcoming",
                                      "Please report this error to\n" +
                                      "https://github.com/processing/processing/issues", e, false);
              }
            }
          });
        }

      } catch (Throwable t) {
        // Catch-all to pick up badness during startup.
        if (t.getCause() != null) {
          // Usually this is the more important piece of information. We'll
          // show this one so that it's not truncated in the error window.
          t = t.getCause();
        }
        showBadnessTrace("We're off on the wrong foot",
                         "An error occurred during startup.", t, true);
      }
      log("done creating base..."); //$NON-NLS-1$
    }
  }


  public static void setCommandLine() {
    commandLine = true;
  }


  static public boolean isCommandLine() {
    return commandLine;
  }


  static public void initPlatform() {
    try {
      Class<?> platformClass = Class.forName("processing.app.Platform"); //$NON-NLS-1$
      if (Base.isMacOS()) {
        platformClass = Class.forName("processing.app.platform.MacPlatform"); //$NON-NLS-1$
      } else if (Base.isWindows()) {
        platformClass = Class.forName("processing.app.platform.WindowsPlatform"); //$NON-NLS-1$
      } else if (Base.isLinux()) {
        platformClass = Class.forName("processing.app.platform.LinuxPlatform"); //$NON-NLS-1$
      }
      platform = (Platform) platformClass.newInstance();
    } catch (Exception e) {
      Base.showError("Problem Setting the Platform",
                     "An unknown error occurred while trying to load\n" +
                     "platform-specific code for your machine.", e);
    }
  }


  /*
  public static void initRequirements() {
    try {
      Class.forName("com.sun.jdi.VirtualMachine"); //$NON-NLS-1$
    } catch (ClassNotFoundException cnfe) {
      //String cp = System.getProperty("java.class.path").replace(File.pathSeparatorChar, '\n');
//      String cp = System.getProperty("sun.boot.class.path").replace(File.pathSeparatorChar, '\n');

      Base.openURL("http://wiki.processing.org/w/Supported_Platforms");
//      Base.showError("Please install JDK 1.6 or later",
//                     "Processing requires a full JDK (not just a JRE)\n" +
//                     "to run. Please install JDK 1.6 or later.\n" +
//                     "More information can be found on the Wiki." +
//                     "\n\nJAVA_HOME is currently\n" +
//                     System.getProperty("java.home") + "\n" +
//                     "And the CLASSPATH contains\n" + cp, cnfe);
      Base.showError("Missing required files",
                     "Processing requires a JRE with tools.jar (or a\n" +
                     "full JDK) installed in (or linked to) a folder\n" +
                     "named “java” next to the Processing application.\n" +
                     "More information can be found on the Wiki.", cnfe);
    }
  }
  */


  // TODO should this be public to suggest override for Arduino and others?
  private String getDefaultModeIdentifier() {
    //return "processing.mode.java.pdex.ExperimentalMode";
    return "processing.mode.java.JavaMode";
  }


  // TODO same as above... make public?
  private void buildCoreModes() {
    Mode javaMode =
      ModeContribution.load(this, getContentFile("modes/java"), //$NON-NLS-1$
                            getDefaultModeIdentifier()).getMode(); //$NON-NLS-1$

    // PDE X calls getModeList() while it's loading, so coreModes must be set
    coreModes = new Mode[] { javaMode };

    /*
    Mode pdexMode =
      ModeContribution.load(this, getContentFile("modes/ExperimentalMode"), //$NON-NLS-1$
                            "processing.mode.experimental.ExperimentalMode").getMode(); //$NON-NLS-1$

    // Safe to remove the old Java mode here?
    //coreModes = new Mode[] { pdexMode };
    coreModes = new Mode[] { pdexMode, javaMode };
    */
  }


  /**
   * Instantiates and adds new contributed modes to the contribModes list.
   * Checks for duplicates so the same mode isn't instantiates twice. Does not
   * remove modes because modes can't be removed once they are instantiated.
   */
  void rebuildContribModes() {
    if (modeContribs == null) {
      modeContribs = new ArrayList<ModeContribution>();
    }
    ModeContribution.loadMissing(this);

//    ArrayList<ModeContribution> newContribs =
//      ModeContribution.loadAll(getSketchbookModesFolder());
//    for (ModeContribution contrib : newContribs) {
//      if (!contribModes.contains(contrib)) {
//        if (contrib.instantiateModeClass(this)) {
//          contribModes.add(contrib);
//        }
//      }
//    }
  }


  /**
   * Instantiates and adds new contributed modes to the contribModes list.
   * Checks for duplicates so the same mode isn't instantiates twice. Does not
   * remove modes because modes can't be removed once they are instantiated.
   */
  void rebuildContribExamples() {
    if (exampleContribs == null) {
      exampleContribs = new ArrayList<ExamplesContribution>();
    }
    ExamplesContribution.loadMissing(this);
  }


  public Base(String[] args) throws Exception {
    ContributionManager.cleanup(this);
    buildCoreModes();
    rebuildContribModes();
    rebuildContribExamples();

    // Needs to happen after the sketchbook folder has been located.
    // Also relies on the modes to be loaded so it knows what can be
    // marked as an example.
    recent = new Recent(this);

    String lastModeIdentifier = Preferences.get("mode.last"); //$NON-NLS-1$
    if (lastModeIdentifier == null) {
      nextMode = getDefaultMode();
      log("Nothing set for last.sketch.mode, using default."); //$NON-NLS-1$
    } else {
      for (Mode m : getModeList()) {
        if (m.getIdentifier().equals(lastModeIdentifier)) {
          logf("Setting next mode to %s.", lastModeIdentifier); //$NON-NLS-1$
          nextMode = m;
        }
      }
      if (nextMode == null) {
        nextMode = getDefaultMode();
        logf("Could not find mode %s, using default.", lastModeIdentifier); //$NON-NLS-1$
      }
    }

    contributionManagerFrame = new ContributionManagerDialog();

    // Make sure ThinkDifferent has library examples too
    nextMode.rebuildLibraryList();

    // Put this after loading the examples, so that building the default file
    // menu works on Mac OS X (since it needs examplesFolder to be set).
    platform.init(this);

//    toolsFolder = getContentFile("tools");

//    // Check if there were previously opened sketches to be restored
//    boolean opened = restoreSketches();
    boolean opened = false;

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
    new UpdateCheck(this);
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  /** Returns the front most, active editor window. */
  public Editor getActiveEditor() {
    return activeEditor;
  }


  /** Get the list of currently active editor windows. */
  public List<Editor> getEditors() {
    return editors;
  }


  /**
   * The call has already checked to make sure this sketch is not modified,
   * now change the mode.
   */
  public void changeMode(Mode mode) {
    if (activeEditor.getMode() != mode) {
      Sketch sketch = activeEditor.getSketch();
      nextMode = mode;

      if (sketch.isUntitled()) {
        // If no changes have been made, just close and start fresh.
        // (Otherwise the editor would lose its 'untitled' status.)
        handleClose(activeEditor, true);
        handleNew();

      } else {
        // If the current editor contains file extensions that the new mode can handle, then
        // write a sketch.properties file with the new mode specified, and reopen.
        boolean newModeCanHandleCurrentSource = true;
        for (final SketchCode code: sketch.getCode()) {
          if (!mode.validExtension(code.getExtension())) {
            newModeCanHandleCurrentSource = false;
            break;
          }
        }
        if (newModeCanHandleCurrentSource) {
          final File props = new File(sketch.getCodeFolder(), "sketch.properties");
          saveModeSettings(props, nextMode);
          handleClose(activeEditor, true);
          handleOpen(sketch.getMainFilePath());
        }
      }
    }
  }


  public List<ModeContribution> getModeContribs() {
    return modeContribs;
  }


  public List<Mode> getModeList() {
    ArrayList<Mode> allModes = new ArrayList<Mode>();
    allModes.addAll(Arrays.asList(coreModes));
    if (modeContribs != null) {
      for (ModeContribution contrib : modeContribs) {
        allModes.add(contrib.getMode());
      }
    }
    return allModes;
  }


  public List<ExamplesContribution> getExampleContribs() {
    return exampleContribs;
  }


  private List<Contribution> getInstalledContribs() {
    List<Contribution> contributions = new ArrayList<Contribution>();

    List<ModeContribution> modeContribs = getModeContribs();
    contributions.addAll(modeContribs);

    for (ModeContribution modeContrib : modeContribs) {
      Mode mode = modeContrib.getMode();
      contributions.addAll(new ArrayList<Library>(mode.contribLibraries));
    }

    // TODO this duplicates code in Editor, but it's not editor-specific
//    List<ToolContribution> toolContribs =
//      ToolContribution.loadAll(Base.getSketchbookToolsFolder());
//    contributions.addAll(toolContribs);
    contributions.addAll(ToolContribution.loadAll(getSketchbookToolsFolder()));

    contributions.addAll(getExampleContribs());
    return contributions;
  }


  public byte[] getInstalledContribsInfo() {
    List<Contribution> contribs = getInstalledContribs();
    StringList entries = new StringList();
    for (Contribution c : contribs) {
      String entry = c.getTypeName() + "=" +
        PApplet.urlEncode(String.format("name=%s\nurl=%s\nrevision=%d\nversion=%s",
                                        c.getName(), c.getUrl(),
                                        c.getVersion(), c.getPrettyVersion()));
      entries.append(entry);
    }
    String joined =
      "id=" + Preferences.get("update.id") + "&" + entries.join("&");
//    StringBuilder sb = new StringBuilder();
//    try {
//      // Truly ridiculous attempt to shove everything into a GET request.
//      // More likely to be seen as part of a grand plot.
//      ByteArrayOutputStream baos = new ByteArrayOutputStream();
//      GZIPOutputStream output = new GZIPOutputStream(baos);
//      PApplet.saveStream(output, new ByteArrayInputStream(joined.getBytes()));
//      output.close();
//      byte[] b = baos.toByteArray();
//      for (int i = 0; i < b.length; i++) {
//        sb.append(PApplet.hex(b[i], 2));
//      }
//    } catch (IOException e) {
//      e.printStackTrace();
//    }
//    return sb.toString();
    return joined.getBytes();
  }


  // Because of variations in native windowing systems, no guarantees about
  // changes to the focused and active Windows can be made. Developers must
  // never assume that this Window is the focused or active Window until this
  // Window receives a WINDOW_GAINED_FOCUS or WINDOW_ACTIVATED event.
  public void handleActivated(Editor whichEditor) {
    activeEditor = whichEditor;

    // set the current window to be the console that's getting output
    EditorConsole.setEditor(activeEditor);

    // make this the next mode to be loaded
    nextMode = whichEditor.getMode();
    Preferences.set("mode.last", nextMode.getIdentifier()); //$NON-NLS-1$
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  boolean breakTime = false;
  String[] months = {
    "jan", "feb", "mar", "apr", "may", "jun",
    "jul", "aug", "sep", "oct", "nov", "dec"
  };


  /**
   * Create a new untitled document in a new sketch window.
   */
  public void handleNew() {
    try {
      File newbieDir = null;
      String newbieName = null;

      // In 0126, untitled sketches will begin in the temp folder,
      // and then moved to a new location because Save will default to Save As.
//      File sketchbookDir = getSketchbookFolder();
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
          return;
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
      File newbieFile =
        new File(newbieDir, newbieName + "." + nextMode.getDefaultExtension()); //$NON-NLS-1$
      if (!newbieFile.createNewFile()) {
        throw new IOException(newbieFile + " already exists.");
      }

      // Create sketch properties file if it's not the default mode.
      if (!nextMode.equals(getDefaultMode())) {
        saveModeSettings(new File(newbieDir, "sketch.properties"), nextMode);
      }

      String path = newbieFile.getAbsolutePath();
      /*Editor editor =*/ handleOpen(path, true);

    } catch (IOException e) {
      Base.showWarning("That's new to me",
                       "A strange and unexplainable error occurred\n" +
                       "while trying to create a new sketch.", e);
    }
  }

  // Create or modify a sketch.proprties file to specify the given Mode.
  private void saveModeSettings(final File sketchProps, final Mode mode) {
    try {
      final Settings settings = new Settings(sketchProps);
      settings.set("mode", mode.getTitle());
      settings.set("mode.id", mode.getIdentifier());
      settings.save();
    } catch (IOException e) {
      System.err.println("While creating " + sketchProps + ": " + e.getMessage());
    }
  }


  public Mode getDefaultMode() {
    return coreModes[0];
  }


  /** Used by ThinkDifferent so that it can have a Sketchbook menu. */
  public Mode getNextMode() {
    return nextMode;
  }


  /**
   * Prompt for a sketch to open, and open it in a new window.
   */
  public void handleOpenPrompt() {
    final ArrayList<String> extensions = new ArrayList<String>();
    for (Mode mode : getModeList()) {
      extensions.add(mode.getDefaultExtension());
    }


    final String prompt = Language.text("open");

    // don't use native dialogs on Linux (or anyone else w/ override)
    if (Preferences.getBoolean("chooser.files.native")) {  //$NON-NLS-1$
      // use the front-most window frame for placing file dialog
      FileDialog openDialog =
        new FileDialog(activeEditor, prompt, FileDialog.LOAD);

      // Only show .pde files as eligible bachelors
      openDialog.setFilenameFilter(new FilenameFilter() {
        public boolean accept(File dir, String name) {
          // confirmed to be working properly [fry 110128]
          for (String ext : extensions) {
            if (name.toLowerCase().endsWith("." + ext)) { //$NON-NLS-1$
              return true;
            }
          }
          return false;
        }
      });

      openDialog.setVisible(true);

      String directory = openDialog.getDirectory();
      String filename = openDialog.getFile();
      if (filename != null) {
        File inputFile = new File(directory, filename);
        handleOpen(inputFile.getAbsolutePath());
      }

    } else {
      if (openChooser == null) {
        openChooser = new JFileChooser();
      }
      openChooser.setDialogTitle(prompt);

      openChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
        public boolean accept(File file) {
          // JFileChooser requires you to explicitly say yes to directories
          // as well (unlike the AWT chooser). Useful, but... different.
          // http://code.google.com/p/processing/issues/detail?id=1151
          if (file.isDirectory()) {
            return true;
          }
          for (String ext : extensions) {
            if (file.getName().toLowerCase().endsWith("." + ext)) { //$NON-NLS-1$
              return true;
            }
          }
          return false;
        }

        public String getDescription() {
          return "Processing Sketch";
        }
      });
      if (openChooser.showOpenDialog(activeEditor) == JFileChooser.APPROVE_OPTION) {
        handleOpen(openChooser.getSelectedFile().getAbsolutePath());
      }
    }
  }


  /**
   * Open a sketch from the path specified. Do not use for untitled sketches.
   */
  public Editor handleOpen(String path) {
    return handleOpen(path, false);
  }


  /**
   * Open a sketch in a new window.
   * @param path Path to the pde file for the sketch in question
   * @return the Editor object, so that properties (like 'untitled')
   *         can be set by the caller
   */
  public Editor handleOpen(String path, boolean untitled) {
    return handleOpen(path, untitled, new EditorState(editors));
  }


//  protected Editor handleOpen(String path, int[] location) {
//  protected Editor handleOpen(String path, Rectangle bounds, int divider) {
  protected Editor handleOpen(String path, boolean untitled, EditorState state) {
    try {
      // System.err.println("entering handleOpen " + path);

      final File file = new File(path);
      if (!file.exists()) {
        return null;
      }

      // Cycle through open windows to make sure that it's not already open.
      for (Editor editor : editors) {
        // User may have double-clicked any PDE in the sketch folder,
        // so we have to check each open tab (not just the main one).
        // https://github.com/processing/processing/issues/2506
        for (SketchCode tab : editor.getSketch().getCode()) {
          if (tab.getFile().equals(file)) {
            editor.toFront();
            // move back to the top of the recent list
            handleRecent(editor);
            return editor;
          }
        }
      }

      if (!Sketch.isSanitaryName(file.getName())) {
        Base.showWarning("You're tricky, but not tricky enough",
                         file.getName() + " is not a valid name for a sketch.\n" +
                         "Better to stick to ASCII, no spaces, and make sure\n" +
                         "it doesn't start with a number.", null);
        return null;
      }

      if (!nextMode.canEdit(file)) {
        final Mode mode = selectMode(file);
        if (mode == null) {
          return null;
        }
        nextMode = mode;
      }

//    Editor.State state = new Editor.State(editors);
      Editor editor = null;
      try {
        editor = nextMode.createEditor(this, path, state);

      } catch (NoSuchMethodError nsme) {
        Base.showWarning("Mode out of date",
                         nextMode.getTitle() + " is not compatible with this version of Processing.\n" +
                         "Try updating the Mode or contact its author for a new version.", nsme);
      } catch (Throwable t) {
        showBadnessTrace("Mode Problems",
                         "A nasty error occurred while trying to use " + nextMode.getTitle() + ".\n" +
                         "It may not be compatible with this version of Processing.\n" +
                         "Try updating the Mode or contact its author for a new version.", t, false);
      }
      if (editor == null) {
        // if the bad mode is the default mode, don't go into an infinite loop
        // trying to recreate a window with the default mode.
        Mode defaultMode = getDefaultMode();
        if (nextMode == defaultMode) {
          Base.showError("Editor Problems",
                         "An error occurred while trying to change modes.\n" +
                         "We'll have to quit for now because it's an\n" +
                         "unfortunate bit of indigestion with the default Mode.",
                         null);
        } else {
          editor = defaultMode.createEditor(this, path, state);
        }
      }

      // Make sure that the sketch actually loaded
      Sketch sketch = editor.getSketch();
      if (sketch == null) {
        return null;  // Just walk away quietly
      }

      sketch.setUntitled(untitled);
      editors.add(editor);
      handleRecent(editor);

      // now that we're ready, show the window
      // (don't do earlier, cuz we might move it based on a window being closed)
      editor.setVisible(true);

      return editor;

//    } catch (NoSuchMethodError nsme) {
//      Base.showWarning(title, message);

    } catch (Throwable t) {
      showBadnessTrace("Terrible News",
                       "A serious error occurred while " +
                       "trying to create a new editor window.", t,
                       nextMode == getDefaultMode());  // quit if default
      nextMode = getDefaultMode();
      return null;
    }
  }


  private static class ModeInfo {
    public final String title;
    public final String id;

    public ModeInfo(String id, String title) {
      this.id = id;
      this.title = title;
    }
  }


  private static ModeInfo modeInfoFor(final File sketch) {
    final File sketchFolder = sketch.getParentFile();
    final File sketchProps = new File(sketchFolder, "sketch.properties");
    if (!sketchProps.exists()) {
      return null;
    }
    try {
      final Settings settings = new Settings(sketchProps);
      final String title = settings.get("mode");
      final String id = settings.get("mode.id");
      if (title == null || id == null) {
        return null;
      }
      return new ModeInfo(id, title);
    } catch (IOException e) {
      System.err.println("While trying to read " + sketchProps + ": "
        + e.getMessage());
    }
    return null;
  }


  private Mode promptForMode(final File sketch, final ModeInfo preferredMode) {
    final String extension =
      sketch.getName().substring(sketch.getName().lastIndexOf('.') + 1);
    final List<Mode> possibleModes = new ArrayList<Mode>();
    for (final Mode mode : getModeList()) {
      if (mode.canEdit(sketch)) {
        possibleModes.add(mode);
      }
    }
    if (possibleModes.size() == 1 &&
        possibleModes.get(0).getIdentifier().equals(getDefaultModeIdentifier())) {
      // If default mode can open it, then do so without prompting.
      return possibleModes.get(0);
    }
    if (possibleModes.size() == 0) {
      if (preferredMode == null) {
        Base.showWarning("Modeless Dialog",
                         "I don't know how to open a sketch with the \""
                         + extension
                         + "\"\nfile extension. You'll have to install a different"
                         + "\nProcessing mode for that.");
      } else {
        Base.showWarning("Modeless Dialog", "You'll have to install "
          + preferredMode.title + " Mode " + "\nin order to open that sketch.");
      }
      return null;
    }
    final Mode[] modes = possibleModes.toArray(new Mode[possibleModes.size()]);
    final String message = preferredMode == null ?
      (nextMode.getTitle() + " Mode can't open ." + extension + " files, " +
       "but you have one or more modes\ninstalled that can. " +
       "Would you like to try one?") :
      ("That's a " + preferredMode.title + " Mode sketch, " +
       "but you don't have " + preferredMode.title + " installed.\n" +
       "Would you like to try a different mode for opening a " +
       "." + extension + " sketch?");
    return (Mode) JOptionPane.showInputDialog(null, message, "Choose Wisely",
                                              JOptionPane.QUESTION_MESSAGE,
                                              null, modes, modes[0]);
  }


  private Mode selectMode(final File sketch) {
    final ModeInfo modeInfo = modeInfoFor(sketch);
    final Mode specifiedMode = modeInfo == null ? null : findMode(modeInfo.id);
    if (specifiedMode != null) {
      return specifiedMode;
    }
    return promptForMode(sketch, modeInfo);
  }


  protected Mode findMode(String id) {
    for (Mode mode : getModeList()) {
      if (mode.getIdentifier().equals(id)) {
        return mode;
      }
    }
    return null;
  }


  /**
   * Close a sketch as specified by its editor window.
   * @param editor Editor object of the sketch to be closed.
   * @param modeSwitch Whether this close is being done in the context of a
   *      mode switch.
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

//    System.out.println("editors size is " + editors.size());
    if (editors.size() == 1) {
      // For 0158, when closing the last window /and/ it was already an
      // untitled sketch, just give up and let the user quit.
//      if (Preferences.getBoolean("sketchbook.closing_last_window_quits") ||
//          (editor.untitled && !editor.getSketch().isModified())) {
      if (Base.isMacOS()) {
        // If the central menubar isn't supported on this OS X JVM,
        // we have to do the old behavior. Yuck!
        if (defaultFileMenu == null) {
          Object[] options = { Language.text("prompt.ok"), Language.text("prompt.cancel") };
          String prompt =
            "<html> " +
            "<head> <style type=\"text/css\">"+
            "b { font: 13pt \"Lucida Grande\" }"+
            "p { font: 11pt \"Lucida Grande\"; margin-top: 8px; width: 300px }"+
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

      Preferences.unset("server.port"); //$NON-NLS-1$
      Preferences.unset("server.key"); //$NON-NLS-1$

      // This will store the sketch count as zero
      editors.remove(editor);
//      System.out.println("editors size now " + editors.size());
//      storeSketches();

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
      } else {  // on OS X, update the default file menu
        editor.setVisible(false);
        editor.dispose();
        defaultFileMenu.insert(getRecentMenu(), 2);
        activeEditor = null;
        editors.remove(editor);
      }

    } else {
      // More than one editor window open,
      // proceed with closing the current window.
      editor.setVisible(false);
      editor.dispose();
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
//    storeSketches();

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
//    int index = 0;
    for (Editor editor : editors) {
//      if (editor.checkModified()) {
//        // Update to the new/final sketch path for this fella
//        storeSketchPath(editor, index);
//        index++;
//
//      } else {
//        return false;
//      }
      if (!editor.checkModified()) {
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


  public void thinkDifferentExamples() {
    nextMode.showExamplesFrame();
  }


  /**
   * Synchronous version of rebuild, used when the sketchbook folder has
   * changed, so that the libraries are properly re-scanned before those menus
   * (and the examples window) are rebuilt.
   */
  protected void rebuildSketchbookMenus() {
    // rebuildSketchbookMenu(); // no need to rebuild sketchbook post 3.0
    for (Mode mode : getModeList()) {
      //mode.rebuildLibraryList();
      mode.rebuildImportMenu();  // calls rebuildLibraryList
      mode.rebuildToolbarMenu();
      mode.rebuildExamplesFrame();
      mode.rebuildSketchbookFrame();
    }
  }


  protected void rebuildSketchbookMenu() {
//      System.err.println("sketchbook: " + sketchbookFolder);
    sketchbookMenu.removeAll();
    populateSketchbookMenu(sketchbookMenu);
//    boolean found = false;
//    try {
//      found = addSketches(sketchbookMenu, sketchbookFolder, false);
//    } catch (IOException e) {
//      Base.showWarning("Sketchbook Menu Error",
//                       "An error occurred while trying to list the sketchbook.", e);
//    }
//    if (!found) {
//      JMenuItem empty = new JMenuItem("(empty)");
//      empty.setEnabled(false);
//      sketchbookMenu.add(empty);
//    }
  }


  public void populateSketchbookMenu(JMenu menu) {
    boolean found = false;
    try {
      found = addSketches(menu, sketchbookFolder, false);
    } catch (IOException e) {
      Base.showWarning("Sketchbook Menu Error",
                       "An error occurred while trying to list the sketchbook.", e);
    }
    if (!found) {
      JMenuItem empty = new JMenuItem(Language.text("menu.file.sketchbook.empty"));
      empty.setEnabled(false);
      menu.add(empty);
    }
  }


  public JMenu getRecentMenu() {
    return recent.getMenu();
  }


  public JMenu getToolbarRecentMenu() {
    return recent.getToolbarMenu();
  }


  public void handleRecent(Editor editor) {
    recent.handle(editor);
  }


  public void handleRecentRename(Editor editor,String oldPath){
    recent.handleRename(editor,oldPath);
  }


  /**
   * Called before a sketch is renamed so that its old name is
   * no longer in the menu.
   */
  public void removeRecent(Editor editor) {
    recent.remove(editor);
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
//            if (replace) {
//              handleOpenReplace(path);
//            } else {
            handleOpen(path);
//            }
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
          if (anything && !name.equals("old")) { //Don't add old contributions
            menu.add(submenu);
            found = true;
          }
        }
      }
    }
    return found;
  }


  public boolean addSketches(DefaultMutableTreeNode node,
                             File folder,
                             boolean examples) throws IOException {
    // skip .DS_Store files, etc (this shouldn't actually be necessary)
    if (!folder.isDirectory()) {
      return false;
    }

    final String folderName = folder.getName();

    // Don't look inside the 'libraries' folders in the sketchbook
    if (folderName.equals("libraries")) {
      return false;
    }

    // When building the sketchbook, don't show the contributed 'examples'
    // like it's a subfolder. But when loading examples, allow the folder
    // to be named 'examples'.
    if (!examples && folderName.equals("examples")) {
      return false;
    }

//    // Conversely, when looking for examples, ignore the other folders
//    // (to avoid going through hoops with the tree node setup).
//    if (examples && !folderName.equals("examples")) {
//      return false;
//    }
//    // Doesn't quite work because the parent will be 'examples', and we want
//    // to walk inside that, but the folder itself will have a different name

    String[] fileList = folder.list();
    // If a bad folder or unreadable or whatever, this will come back null
    if (fileList == null) {
      return false;
    }

    // Alphabetize the list, since it's not always alpha order
    Arrays.sort(fileList, String.CASE_INSENSITIVE_ORDER);

    boolean found = false;
    for (String name : fileList) {
      if (name.charAt(0) == '.') {  // Skip hidden files
        continue;
      }

      File subfolder = new File(folder, name);
      if (subfolder.isDirectory()) {
        File entry = checkSketchFolder(subfolder, name);
        if (entry != null) {
          DefaultMutableTreeNode item =
            new DefaultMutableTreeNode(new SketchReference(name, entry));

          node.add(item);
          found = true;

        } else {
          // not a sketch folder, but maybe a subfolder containing sketches
          DefaultMutableTreeNode subnode = new DefaultMutableTreeNode(name);
          // needs to be separate var otherwise would set ifound to false
          boolean anything = addSketches(subnode, subfolder, examples);
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
    for (Mode mode : getModeList()) {
      File entry = new File(subfolder, item + "." + mode.getDefaultExtension()); //$NON-NLS-1$
      // if a .pde file of the same prefix as the folder exists..
      if (entry.exists()) {
        return entry;
      }
    }
    return null;
  }


  // .................................................................


//  /**
//   * Show the About box.
//   */
//  static public void handleAbout() {
//    new About(activeEditor);
//  }


  /**
   * Show the Preferences window.
   */
  public void handlePrefs() {
    if (preferencesFrame == null) {
      preferencesFrame = new PreferencesFrame(this);
    }
    preferencesFrame.showFrame();
  }


  /**
   * Show the library installer window.
   */
  public void handleOpenLibraryManager() {
    contributionManagerFrame.showFrame(activeEditor,ContributionType.LIBRARY);
  }


  /**
   * Show the tool installer window.
   */
  public void handleOpenToolManager() {
    contributionManagerFrame.showFrame(activeEditor,ContributionType.TOOL);
  }


  /**
   * Show the mode installer window.
   */
  public void handleOpenModeManager() {
    contributionManagerFrame.showFrame(activeEditor,ContributionType.MODE);
  }


  /**
   * Show the examples installer window.
   */
  public void handleOpenExampleManager() {
    contributionManagerFrame.showFrame(activeEditor,ContributionType.EXAMPLES);
  }


  public void handleShowUpdates() {
    contributionManagerFrame.showFrame(activeEditor,null);
  }


  // ...................................................................


  static public int getRevision() {
    return REVISION;
  }


  /**
   * Return the version name, something like 1.5 or 2.0b8 or 0213 if it's not
   * a release version.
   */
  static public String getVersionName() {
    return VERSION_NAME;
  }


  //...................................................................


  static public Platform getPlatform() {
    return platform;
  }


  static public String getPlatformName() {
    return PConstants.platformNames[PApplet.platform];
  }


  // Because the Oracle JDK is 64-bit only, we lose this ability, feature,
  // edge case, headache.
//  /**
//   * Return whether sketches will run as 32- or 64-bits. On Linux and Windows,
//   * this is the bit depth of the machine, while on OS X it's determined by the
//   * setting from preferences, since both 32- and 64-bit are supported.
//   */
//  static public int getNativeBits() {
//    if (Base.isMacOS()) {
//      return Preferences.getInteger("run.options.bits"); //$NON-NLS-1$
//    }
//    return nativeBits;
//  }

  /**
   * Return whether sketches will run as 32- or 64-bits based
   * on the JVM that's in use.
   */
  static public int getNativeBits() {
    return nativeBits;
  }

  /**
   * Return the value of the os.arch propery
   */
  static public String getNativeArch() {
    return nativeArch;
  }

  /*
   * Return a string that identifies the variant of a platform
   * e.g. "32" or "64" on Intel
   */
  static public String getVariant() {
    return getVariant(PApplet.platform, getNativeArch(), getNativeBits());
  }

  static public String getVariant(int platform, String arch, int bits) {
    if (platform == PConstants.LINUX && bits == 32 && "arm".equals(Base.getNativeArch())) {
      // assume armv6hf for now
      return "armv6hf";
    } else {
      // 32 or 64
      return Integer.toString(bits);
    }
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
    return System.getProperty("os.name").indexOf("Mac") != -1; //$NON-NLS-1$ //$NON-NLS-2$
  }


  /*
  static private Boolean usableOracleJava;

  // Make sure this is Oracle Java 7u40 or later. This is temporary.
  static public boolean isUsableOracleJava() {
    if (usableOracleJava == null) {
      usableOracleJava = false;

      if (Base.isMacOS() &&
          System.getProperty("java.vendor").contains("Oracle")) {
        String version = System.getProperty("java.version");  // 1.7.0_40
        String[] m = PApplet.match(version, "1.(\\d).*_(\\d+)");

        if (m != null &&
          PApplet.parseInt(m[1]) >= 7 &&
          PApplet.parseInt(m[2]) >= 40) {
          usableOracleJava = true;
        }
      }
    }
    return usableOracleJava;
  }
  */


  /**
   * returns true if running on windows.
   */
  static public boolean isWindows() {
    //return PApplet.platform == PConstants.WINDOWS;
    return System.getProperty("os.name").indexOf("Windows") != -1; //$NON-NLS-1$ //$NON-NLS-2$
  }


  /**
   * true if running on linux.
   */
  static public boolean isLinux() {
    //return PApplet.platform == PConstants.LINUX;
    return System.getProperty("os.name").indexOf("Linux") != -1; //$NON-NLS-1$ //$NON-NLS-2$
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  /**
   * Get the directory that can store settings. (Library on OS X, App Data or
   * something similar on Windows, a dot folder on Linux.) Removed this as a
   * preference for 3.0a3 because we need this to be stable.
   */
  static public File getSettingsFolder() {
    File settingsFolder = null;

//    String preferencesPath = Preferences.get("settings.path"); //$NON-NLS-1$
//    if (preferencesPath != null) {
//      settingsFolder = new File(preferencesPath);
//
//    } else {
    try {
      settingsFolder = platform.getSettingsFolder();
    } catch (Exception e) {
      showError("Problem getting the settings folder",
                "Error getting the Processing the settings folder.", e);
    }
//    }

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
   * the settings folder. Used to get preferences and recent sketch files.
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
   *
   * Unlike createTempFile(), there is no minimum size for prefix. If
   * prefix is less than 3 characters, the remaining characters will be
   * filled with underscores
   */
  static public File createTempFolder(String prefix, String suffix, File directory) throws IOException {
    int fillChars = 3 - prefix.length();
    for (int i = 0; i < fillChars; i++) {
      prefix += '_';
    }
    File folder = File.createTempFile(prefix, suffix, directory);
    // Now delete that file and create a folder in its place
    folder.delete();
    folder.mkdirs();
    // And send the folder back to your friends
    return folder;
  }


  static public File getToolsFolder() {
    return getContentFile("tools");
  }


  static public void locateSketchbookFolder() {
    // If a value is at least set, first check to see if the folder exists.
    // If it doesn't, warn the user that the sketchbook folder is being reset.
    String sketchbookPath = Preferences.getSketchbookPath();
    if (sketchbookPath != null) {
      sketchbookFolder = new File(sketchbookPath);
      if (!sketchbookFolder.exists()) {
        Base.showWarning("Sketchbook folder disappeared",
                         "The sketchbook folder no longer exists.\n" +
                         "Processing will switch to the default sketchbook\n" +
                         "location, and create a new sketchbook folder if\n" +
                         "necessary. Processing will then stop talking\n" +
                         "about himself in the third person.", null);
        sketchbookFolder = null;
      }
    }

    // If no path is set, get the default sketchbook folder for this platform
    if (sketchbookFolder == null) {
      sketchbookFolder = getDefaultSketchbookFolder();
      Preferences.setSketchbookPath(sketchbookFolder.getAbsolutePath());
      if (!sketchbookFolder.exists()) {
        sketchbookFolder.mkdirs();
      }
    }
    makeSketchbookSubfolders();
  }


  public void setSketchbookFolder(File folder) {
    sketchbookFolder = folder;
    Preferences.setSketchbookPath(folder.getAbsolutePath());
    rebuildSketchbookMenus();
    makeSketchbookSubfolders();
  }


  /**
   * Create the libraries, modes, tools, examples folders in the sketchbook.
   */
  static protected void makeSketchbookSubfolders() {
    getSketchbookLibrariesFolder().mkdirs();
    getSketchbookToolsFolder().mkdirs();
    getSketchbookModesFolder().mkdirs();
    getSketchbookExamplesFolder().mkdirs();
  }


  static public File getSketchbookFolder() {
    return sketchbookFolder;
  }


  static public File getSketchbookLibrariesFolder() {
    return new File(sketchbookFolder, "libraries");
  }


  static public File getSketchbookToolsFolder() {
    return new File(sketchbookFolder, "tools");
  }


  static public File getSketchbookModesFolder() {
    return new File(sketchbookFolder, "modes");
  }


  static public File getSketchbookExamplesFolder() {
    return new File(sketchbookFolder, "examples");
  }


  static protected File getDefaultSketchbookFolder() {
    File sketchbookFolder = null;
    try {
      sketchbookFolder = platform.getDefaultSketchbookFolder();
    } catch (Exception e) { }

    if (sketchbookFolder == null) {
      showError("No sketchbook",
                "Problem while trying to get the sketchbook", null);
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


  // .................................................................


  /**
   * Implements the cross-platform headache of opening URLs.
   *
   * For 2.0a8 and later, this requires the parameter to be an actual URL,
   * meaning that you can't send it a file:// path without a prefix. It also
   * just calls into Platform, which now uses java.awt.Desktop (where
   * possible, meaning not on Linux) now that we're requiring Java 6.
   * As it happens the URL must also be properly URL-encoded.
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
  static public boolean openFolderAvailable() {
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
   * Non-fatal error message.
   */
  static public void showWarning(String title, String message) {
    showWarning(title, message, null);
  }

  /**
   * Non-fatal error message with optional stack trace side dish.
   */
  static public void showWarning(String title, String message, Throwable e) {
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
                                       Throwable e) {
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
                          "p { font: 11pt \"Lucida Grande\"; margin-top: 8px; width: 300px }"+
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


  /**
   * Testing a new warning window that includes the stack trace.
   */
  static private void showBadnessTrace(String title, String message,
                                       Throwable t, boolean fatal) {
    if (title == null) title = fatal ? "Error" : "Warning";

    if (commandLine) {
      System.err.println(title + ": " + message);
      if (t != null) {
        t.printStackTrace();
      }

    } else {
      StringWriter sw = new StringWriter();
      t.printStackTrace(new PrintWriter(sw));
      // Necessary to replace \n with <br/> (even if pre) otherwise Java
      // treats it as a closed tag and reverts to plain formatting.
      message = ("<html>" + message +
                 "<br/><font size=2><br/>" +
                 sw + "</html>").replaceAll("\n", "<br/>");

      JOptionPane.showMessageDialog(new Frame(), message, title,
                                    fatal ?
                                    JOptionPane.ERROR_MESSAGE :
                                    JOptionPane.WARNING_MESSAGE);

      if (fatal) {
        System.exit(1);
      }
    }
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
                        "p { font: 11pt \"Lucida Grande\"; margin-top: 8px; width: 300px }"+
                        "</style> </head>" +
                        "<b>" + Language.text("save.title") + "</b>" +
                        "<p>" + Language.text("save.hint") + "</p>",
                        JOptionPane.QUESTION_MESSAGE);

      String[] options = new String[] {
          Language.text("save.btn.save"), Language.text("prompt.cancel"), Language.text("save.btn.dont_save")
      };
      pane.setOptions(options);

      // highlight the safest option ala apple hig
      pane.setInitialValue(options[0]);

      // on macosx, setting the destructive property places this option
      // away from the others at the lefthand side
      pane.putClientProperty("Quaqua.OptionPane.destructiveOption",
                             Integer.valueOf(2));

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
                        "p { font: 11pt \"Lucida Grande\"; margin-top: 8px; width: 300px }"+
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


  static protected File processingRoot;

  /**
   * Get reference to a file adjacent to the executable on Windows and Linux,
   * or inside Contents/Resources/Java on Mac OS X. This will return the local
   * JRE location, *whether or not it is the active JRE*.
   */
  static public File getContentFile(String name) {
    if (processingRoot == null) {
      // Get the path to the .jar file that contains Base.class
      String path = Base.class.getProtectionDomain().getCodeSource().getLocation().getPath();
      // Path may have URL encoding, so remove it
      String decodedPath = PApplet.urlDecode(path);

      if (decodedPath.contains("/app/bin")) {  // This means we're in Eclipse
        if (Base.isMacOS()) {
          processingRoot =
            new File(path, "../../build/macosx/work/Processing.app/Contents/Java");
        } else if (Base.isWindows()) {
          processingRoot =  new File(path, "../../build/windows/work");
        } else if (Base.isLinux()) {
          processingRoot =  new File(path, "../../build/linux/work");
        }
      } else {
        // The .jar file will be in the lib folder
        File jarFolder = new File(decodedPath).getParentFile();
        if (jarFolder.getName().equals("lib")) {
          // The main Processing installation directory.
          // This works for Windows, Linux, and Apple's Java 6 on OS X.
          processingRoot = jarFolder.getParentFile();
        } else if (Base.isMacOS()) {
          // This works for Java 8 on OS X. We don't have things inside a 'lib'
          // folder on OS X. Adding it caused more problems than it was worth.
          processingRoot = jarFolder;
        }
        if (processingRoot == null || !processingRoot.exists()) {
          // Try working directory instead (user.dir, different from user.home)
          System.err.println("Could not find lib folder via " +
            jarFolder.getAbsolutePath() +
            ", switching to user.dir");
          processingRoot = new File(System.getProperty("user.dir"));
        }
      }
    }
    return new File(processingRoot, name);
  }


  static public File getJavaHome() {
    if (isMacOS()) {
      //return "Contents/PlugIns/jdk1.7.0_40.jdk/Contents/Home/jre/bin/java";
      File[] plugins = getContentFile("../PlugIns").listFiles(new FilenameFilter() {
        public boolean accept(File dir, String name) {
          return dir.isDirectory() &&
            name.endsWith(".jdk") && !name.startsWith(".");
        }
      });
      return new File(plugins[0], "Contents/Home/jre");
    }
    // On all other platforms, it's the 'java' folder adjacent to Processing
    return getContentFile("java");
  }


  /** Get the path to the embedded Java executable. */
  static public String getJavaPath() {
    String javaPath = "bin/java" + (isWindows() ? ".exe" : "");
    File javaFile = new File(getJavaHome(), javaPath);
    try {
      return javaFile.getCanonicalPath();
    } catch (IOException e) {
      return javaFile.getAbsolutePath();
    }
  }


  /**
   * Return a File from inside the Processing 'lib' folder.
   */
  static public File getLibFile(String filename) throws IOException {
    return new File(getContentFile("lib"), filename);
  }


  /**
   * Return an InputStream for a file inside the Processing lib folder.
   */
  static public InputStream getLibStream(String filename) throws IOException {
    return new FileInputStream(getLibFile(filename));
  }


  // Note: getLibImage() has moved to Toolkit


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  static public void log(Object from, String message) {
    if (DEBUG) {
      System.out.println(from.getClass().getName() + ": " + message);
    }
  }


  static public void log(String message) {
    if (DEBUG) {
      System.out.println(message);
    }
  }


  static public void logf(String message, Object... args) {
    if (DEBUG) {
      System.out.println(String.format(message, args));
    }
  }


  static public void loge(String message, Throwable e) {
    if (DEBUG) {
      System.err.println(message);
      e.printStackTrace();
    }
  }


  static public void loge(String message) {
    if (DEBUG) {
      System.out.println(message);
    }
  }
}
