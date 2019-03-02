/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2012-19 The Processing Foundation
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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;

import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.tree.DefaultMutableTreeNode;

import processing.app.contrib.*;
import processing.app.tools.Tool;
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
  static private final int REVISION = 270;
  /** This might be replaced by main() if there's a lib/version.txt file. */
  static private String VERSION_NAME = "0270"; //$NON-NLS-1$
  /** Set true if this a proper release rather than a numbered revision. */

  /**
   * True if heavy debugging error/log messages are enabled. Set to true
   * if an empty file named 'debug' is found in the settings folder.
   * See implementation in createAndShowGUI().
   */
  static public boolean DEBUG;

  static private boolean commandLine;

  // A single instance of the preferences window
  PreferencesFrame preferencesFrame;

  // A single instance of the library manager window
//  ContributionManagerDialog contributionManagerFrame;

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

  protected List<ModeContribution> modeContribs;
  protected List<ExamplesContribution> exampleContribs;

  private JMenu sketchbookMenu;

//  private Recent recent;

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
            // Windows Defender has been insisting on destroying each new
            // release by removing core.jar and other files. Yay!
            // https://github.com/processing/processing/issues/5537
            if (Platform.isWindows()) {
              String mess = t.getMessage();
              String missing = null;
              if (mess.contains("Could not initialize class com.sun.jna.Native")) {
                missing = "jnidispatch.dll";
              } else if (mess.contains("NoClassDefFoundError: processing/core/PApplet")) {
                missing = "core.jar";
              }
              if (missing != null) {
                Messages.showError("Necessary files are missing",
                                   "A file required by Processing (" + missing + ") is missing.\n\n" +
                                   "Make sure that you're not trying to run Processing from inside\n" +
                                   "the .zip file you downloaded, and check that Windows Defender\n" +
                                   "hasn't removed files from the Processing folder.\n\n" +
                                   "(It sometimes flags parts of Processing as a trojan or virus.\n" +
                                   "It is neither, but Microsoft has ignored our pleas for help.)", t);
              }
            }
            Messages.showTrace("Unknown Problem",
                               "A serious error happened during startup. Please report:\n" +
                               "http://github.com/processing/processing/issues/new", t, true);
          }
        }
    });
  }


  static private void createAndShowGUI(String[] args) {
    try {
      File versionFile = Platform.getContentFile("lib/version.txt");
      if (versionFile.exists()) {
        String version = PApplet.loadStrings(versionFile)[0];
        if (!version.equals(VERSION_NAME)) {
          VERSION_NAME = version;
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    Platform.init();
    // call after Platform.init() because we need the settings folder
    Console.startup();

    // Set the debug flag based on a file being present in the settings folder
    File debugFile = getSettingsFile("debug.txt");
    /*
    if (debugFile.isDirectory()) {
      // if it's a directory, it's a leftover from older releases, clear it
      Util.removeDir(debugFile);
    } else*/
    if (debugFile.exists()) {
      DEBUG = true;
    }

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
        Platform.setLookAndFeel();
      } catch (Exception e) {
        Messages.loge("Could not set the Look & Feel", e); //$NON-NLS-1$
      }

      boolean sketchbookPrompt = false;
      if (Preferences.getBoolean("welcome.show")) {
        // only ask once about split sketchbooks
        if (!Preferences.getBoolean("welcome.seen")) {
          // Check if there's a 2.0 sketchbook present
          String oldPath = Preferences.getOldSketchbookPath();
          if (oldPath != null) {
            String newPath = Preferences.getSketchbookPath();
            // If newPath is null, this is the first run of any 3.x version
            if (newPath == null) {
              sketchbookPrompt = true;

            } else if (oldPath.equals(newPath)) {
              // If both exist and are identical, then the user has used
              // pre-releases of 3.x and needs to be warned about the
              // larger changes in this release.
              sketchbookPrompt = true;
            }
          }
        }
      }

      // Get the sketchbook path, and make sure it's set properly
      locateSketchbookFolder();

      // Create a location for untitled sketches
      try {
        untitledFolder = Util.createTempFolder("untitled", "sketches", null);
        untitledFolder.deleteOnExit();
      } catch (IOException e) {
        Messages.showError("Trouble without a name",
                           "Could not create a place to store untitled sketches.\n" +
                           "That's gonna prevent us from continuing.", e);
      }

      Messages.log("About to create Base..."); //$NON-NLS-1$
      try {
        final Base base = new Base(args);
        Messages.log("Base() constructor succeeded");

        // Prevent more than one copy of the PDE from running.
        SingleInstance.startServer(base);

        // Needs to be shown after the first editor window opens, so that it
        // shows up on top, and doesn't prevent an editor window from opening.
        if (Preferences.getBoolean("welcome.show")) {
          try {
            new Welcome(base, sketchbookPrompt);
          } catch (IOException e) {
            Messages.showTrace("Unwelcoming",
                               "Please report this error to\n" +
                               "https://github.com/processing/processing/issues", e, false);
          }
        }

        checkDriverBug();

      } catch (Throwable t) {
        // Catch-all to pick up badness during startup.
        if (t.getCause() != null) {
          // Usually this is the more important piece of information. We'll
          // show this one so that it's not truncated in the error window.
          t = t.getCause();
        }
        Messages.showTrace("We're off on the wrong foot",
                           "An error occurred during startup.", t, true);
      }
      Messages.log("Done creating Base..."); //$NON-NLS-1$
    }
  }


  // Remove this code in a couple months [fry 170211]
  // https://github.com/processing/processing/issues/4853
  // Or maybe not, if NVIDIA keeps doing this [fry 170423]
  // https://github.com/processing/processing/issues/4997
  static private void checkDriverBug() {
    if (System.getProperty("os.name").contains("Windows 10")) {
      new Thread(new Runnable() {
        public void run() {
          try {
            Process p = Runtime.getRuntime().exec("powershell Get-WmiObject Win32_PnPSignedDriver| select devicename, driverversion | where {$_.devicename -like \\\"*nvidia*\\\"}");
            BufferedReader reader = PApplet.createReader(p.getInputStream());
            String line = null;
            while ((line = reader.readLine()) != null) {
              if (line.contains("3.7849")) {
                EventQueue.invokeLater(new Runnable() {
                  public void run() {
                    Messages.showWarning("NVIDIA screwed up",
                                         "Due to an NVIDIA bug, you need to update your graphics drivers,\n" +
                                         "otherwise you won't be able to run any sketches. Update here:\n" +
                                         "http://nvidia.custhelp.com/app/answers/detail/a_id/4378\n" +
                                         "or read background about the issue at this link:\n" +
                                         "https://github.com/processing/processing/issues/4853");
                  }
                });
              } else if (line.contains("3.8165")) {
                EventQueue.invokeLater(new Runnable() {
                  public void run() {
                    Messages.showWarning("NVIDIA screwed up again",
                                         "Due to an NVIDIA bug, you need to update your graphics drivers,\n" +
                                         "otherwise you won't be able to run any sketches. Update here:\n" +
                                         "http://nvidia.custhelp.com/app/answers/detail/a_id/4453/\n" +
                                         "or read background about the issue at this link:\n" +
                                         "https://github.com/processing/processing/issues/4997");
                  }
                });
              }
            }
          } catch (Exception e) {
            Messages.loge("Problem checking NVIDIA driver", e);
          }
        }
      }).start();
    }
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  /**
   * @return the current revision number, safe to be used for update checks
   */
  static public int getRevision() {
    return REVISION;
  }


  /**
   * @return something like 2.2.1 or 3.0b4 (or 0213 if it's not a release)
   */
  static public String getVersionName() {
    return VERSION_NAME;
  }



  public static void setCommandLine() {
    commandLine = true;
  }


  static public boolean isCommandLine() {
    return commandLine;
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  public Base(String[] args) throws Exception {
    ContributionManager.init(this);

    buildCoreModes();
    rebuildContribModes();
    rebuildContribExamples();

    // Needs to happen after the sketchbook folder has been located.
    // Also relies on the modes to be loaded so it knows what can be
    // marked as an example.
//    recent = new Recent(this);
    Recent.init(this);

    String lastModeIdentifier = Preferences.get("mode.last"); //$NON-NLS-1$
    if (lastModeIdentifier == null) {
      nextMode = getDefaultMode();
      Messages.log("Nothing set for last.sketch.mode, using default."); //$NON-NLS-1$
    } else {
      for (Mode m : getModeList()) {
        if (m.getIdentifier().equals(lastModeIdentifier)) {
          Messages.logf("Setting next mode to %s.", lastModeIdentifier); //$NON-NLS-1$
          nextMode = m;
        }
      }
      if (nextMode == null) {
        nextMode = getDefaultMode();
        Messages.logf("Could not find mode %s, using default.", lastModeIdentifier); //$NON-NLS-1$
      }
    }

    //contributionManagerFrame = new ContributionManagerDialog();

    // Make sure ThinkDifferent has library examples too
    nextMode.rebuildLibraryList();

    // Put this after loading the examples, so that building the default file
    // menu works on Mac OS X (since it needs examplesFolder to be set).
    Platform.initBase(this);

//    toolsFolder = getContentFile("tools");

//    // Check if there were previously opened sketches to be restored
//    boolean opened = restoreSketches();
    boolean opened = false;

    // Check if any files were passed in on the command line
    for (int i = 0; i < args.length; i++) {
      Messages.logf("Parsing command line... args[%d] = '%s'", i, args[i]);

      String path = args[i];
      // Fix a problem with systems that use a non-ASCII languages. Paths are
      // being passed in with 8.3 syntax, which makes the sketch loader code
      // unhappy, since the sketch folder naming doesn't match up correctly.
      // http://dev.processing.org/bugs/show_bug.cgi?id=1089
      if (Platform.isWindows()) {
        try {
          File file = new File(args[i]);
          path = file.getCanonicalPath();
          Messages.logf("Changing %s to canonical %s", i, args[i], path);
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
      Messages.log("Calling handleNew() to open a new window");
      handleNew();
    } else {
      Messages.log("No handleNew(), something passed on the command line");
    }

    // check for updates
    new UpdateCheck(this);

    ContributionListing cl = ContributionListing.getInstance();
    cl.downloadAvailableList(this, new ContribProgressMonitor() { });
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  void buildCoreModes() {
    Mode javaMode =
      ModeContribution.load(this, Platform.getContentFile("modes/java"),
                            getDefaultModeIdentifier()).getMode();

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
      modeContribs = new ArrayList<>();
    }
    File modesFolder = getSketchbookModesFolder();
    List<ModeContribution> contribModes = getModeContribs();

    Map<File, ModeContribution> known = new HashMap<>();
    for (ModeContribution contrib : contribModes) {
      known.put(contrib.getFolder(), contrib);
    }
    File[] potential = ContributionType.MODE.listCandidates(modesFolder);
    // If modesFolder does not exist or is inaccessible (folks might like to
    // mess with folders then report it as a bug) 'potential' will be null.
    if (potential != null) {
      for (File folder : potential) {
        if (!known.containsKey(folder)) {
          try {
            contribModes.add(new ModeContribution(this, folder, null));
          } catch (NoSuchMethodError nsme) {
            System.err.println(folder.getName() + " is not compatible with this version of Processing");
            if (DEBUG) nsme.printStackTrace();
          } catch (NoClassDefFoundError ncdfe) {
            System.err.println(folder.getName() + " is not compatible with this version of Processing");
            if (DEBUG) ncdfe.printStackTrace();
          } catch (InvocationTargetException ite) {
            System.err.println(folder.getName() + " could not be loaded and may not compatible with this version of Processing");
            if (DEBUG) ite.printStackTrace();
          } catch (IgnorableException ig) {
            Messages.log(ig.getMessage());
            if (DEBUG) ig.printStackTrace();
          } catch (Throwable e) {
            System.err.println("Could not load Mode from " + folder);
            e.printStackTrace();
          }
        } else {
          known.remove(folder);  // remove this item as already been seen
        }
      }
    }

    // This allows you to build and test your Mode code from Eclipse.
    // -Dusemode=com.foo.FrobMode:/path/to/FrobMode
    final String useMode = System.getProperty("usemode");
    if (useMode != null) {
      final String[] modeInfo = useMode.split(":", 2);
      final String modeClass = modeInfo[0];
      final String modeResourcePath = modeInfo[1];
      System.out.println("Attempting to load " + modeClass + " with resources at " + modeResourcePath);
      ModeContribution mc = ModeContribution.load(this, new File(modeResourcePath), modeClass);
      contribModes.add(mc);
      File key = getFileForContrib(mc, known);
      if (key != null) {
        known.remove(key);
      }
    }
    if (known.size() != 0) {
      for (ModeContribution mc : known.values()) {
        System.out.println("Extraneous Mode entry: " + mc.getName());
      }
    }
  }


  static private File getFileForContrib(ModeContribution contrib,
                                 Map<File, ModeContribution> known) {
    for (Entry<File, ModeContribution> entry : known.entrySet()) {
      if (entry.getValue() == contrib) {
        return entry.getKey();
      }
    }
    return null;
  }


  /**
   * Instantiates and adds new contributed modes to the contribModes list.
   * Checks for duplicates so the same mode isn't instantiates twice. Does not
   * remove modes because modes can't be removed once they are instantiated.
   */
  void rebuildContribExamples() {
    if (exampleContribs == null) {
      exampleContribs = new ArrayList<>();
    }
    ExamplesContribution.loadMissing(this);
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  /**
   * Tools require an 'Editor' object when they're instantiated, but the
   * activeEditor will be null when the first Editor that opens is creating
   * its Tools menu. This will temporarily set the activeEditor to the one
   * that's opening so that we don't go all NPE on startup. If there's already
   * an active editor, then this does nothing.
   */
  public void checkFirstEditor(Editor editor) {
    if (activeEditor == null) {
      activeEditor = editor;
    }
  }


  /** Returns the front most, active editor window. */
  public Editor getActiveEditor() {
    return activeEditor;
  }


  /** Get the list of currently active editor windows. */
  public List<Editor> getEditors() {
    return editors;
  }


  /**
   * Called when a window is activated. Because of variations in native
   * windowing systems, no guarantees about changes to the focused and active
   * Windows can be made. Never assume that this Window is the focused or
   * active Window until this Window actually receives a WINDOW_GAINED_FOCUS
   * or WINDOW_ACTIVATED event.
   */
  public void handleActivated(Editor whichEditor) {
    activeEditor = whichEditor;

    // set the current window to be the console that's getting output
    EditorConsole.setEditor(activeEditor);

    // make this the next mode to be loaded
    nextMode = whichEditor.getMode();
    Preferences.set("mode.last", nextMode.getIdentifier()); //$NON-NLS-1$
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  public void refreshContribs(ContributionType ct) {
    if (ct == ContributionType.LIBRARY) {
      for (Mode m : getModeList()) {
        m.rebuildImportMenu();
      }

    } else if (ct == ContributionType.MODE) {
      rebuildContribModes();
      for (Editor editor : editors) {
        editor.rebuildModePopup();
      }

    } else if (ct == ContributionType.TOOL) {
      rebuildToolList();
      for (Editor editor : editors) {
        populateToolsMenu(editor.getToolMenu());
      }

    } else if (ct == ContributionType.EXAMPLES) {
      rebuildContribExamples();
      for (Mode m : getModeList()) {
        m.rebuildExamplesFrame();
      }
    }
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  private int updatesAvailable = 0;


  public void setUpdatesAvailable(int n) {
    updatesAvailable = n;
    synchronized (editors) {
      for (Editor e : editors) {
        e.setUpdatesAvailable(n);
      }
    }
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  List<Tool> internalTools;
  List<ToolContribution> coreTools;
  List<ToolContribution> contribTools;


  public List<ToolContribution> getCoreTools() {
    return coreTools;
  }


  public List<ToolContribution> getToolContribs() {
    return contribTools;
  }


  public void removeToolContrib(ToolContribution tc) {
    contribTools.remove(tc);
  }


  public void rebuildToolList() {
    // Only do this once because the list of internal tools will never change
    if (internalTools == null) {
      internalTools = new ArrayList<>();

      initInternalTool("processing.app.tools.CreateFont");
      initInternalTool("processing.app.tools.ColorSelector");
      initInternalTool("processing.app.tools.Archiver");

      if (Platform.isMacOS()) {
        initInternalTool("processing.app.tools.InstallCommander");
      }
    }

    // No need to reload these either
    if (coreTools == null) {
      coreTools = ToolContribution.loadAll(Base.getToolsFolder());
      for (Tool tool : coreTools) {
        tool.init(this);
      }
    }

    // Rebuilt when new tools installed, etc
    contribTools = ToolContribution.loadAll(Base.getSketchbookToolsFolder());
    for (Tool tool : contribTools) {
      try {
        tool.init(this);

        // With the exceptions, we can't call statusError because the window
        // isn't completely set up yet. Also not gonna pop up a warning because
        // people may still be running different versions of Processing.

      } catch (VerifyError ve) {
        System.err.println("\"" + tool.getMenuTitle() + "\" is not " +
                           "compatible with this version of Processing");

      } catch (NoSuchMethodError nsme) {
        System.err.println("\"" + tool.getMenuTitle() + "\" is not " +
                           "compatible with this version of Processing");
        System.err.println("The " + nsme.getMessage() + " method no longer exists.");
        Messages.loge("Incompatible Tool found during tool.init()", nsme);

      } catch (NoClassDefFoundError ncdfe) {
        System.err.println("\"" + tool.getMenuTitle() + "\" is not " +
                           "compatible with this version of Processing");
        System.err.println("The " + ncdfe.getMessage() + " class is no longer available.");
        Messages.loge("Incompatible Tool found during tool.init()", ncdfe);

      } catch (AbstractMethodError ame) {
        System.err.println("\"" + tool.getMenuTitle() + "\" is not " +
                           "compatible with this version of Processing");
//        ame.printStackTrace();

      } catch (Error err) {
        System.err.println("An error occurred inside \"" + tool.getMenuTitle() + "\"");
        err.printStackTrace();

      } catch (Exception ex) {
        System.err.println("An exception occurred inside \"" + tool.getMenuTitle() + "\"");
        ex.printStackTrace();
      }
    }
  }


  protected void initInternalTool(String className) {
    try {
      Class<?> toolClass = Class.forName(className);
      final Tool tool = (Tool)
        toolClass.getDeclaredConstructor().newInstance();

      tool.init(this);
      internalTools.add(tool);

    } catch (Exception e) {
      e.printStackTrace();
    }
  }


  /*
    Iterator<Editor> editorIter = base.getEditors().iterator();
    while (editorIter.hasNext()) {
      Editor editor = editorIter.next();
      List<ToolContribution> contribTools = editor.getToolContribs();
      for (ToolContribution toolContrib : contribTools) {
        if (toolContrib.getName().equals(this.name)) {
          try {
            ((URLClassLoader) toolContrib.loader).close();
            editor.removeToolContrib(toolContrib);
            break;
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
      }
   */


  public void clearToolMenus() {
    for (Editor ed : editors) {
      ed.clearToolMenu();
    }
  }


  public void populateToolsMenu(JMenu toolsMenu) {
    // If this is the first run, need to build out the lists
    if (internalTools == null) {
      rebuildToolList();
    }
//    coreTools = ToolContribution.loadAll(Base.getToolsFolder());
//    contribTools = ToolContribution.loadAll(Base.getSketchbookToolsFolder());

//    Collections.sort(coreTools);
//    Collections.sort(contribTools);
//    Collections.sort(coreTools, new Comparator<ToolContribution>() {
//      @Override
//      public int compare(ToolContribution o1, ToolContribution o2) {
//        return o1.getMenuTitle().compareTo(o2.getMenuTitle());
//      }
//    });
    toolsMenu.removeAll();
    for (Tool tool : internalTools) {
      toolsMenu.add(createToolItem(tool));
    }
    toolsMenu.addSeparator();

    if (coreTools.size() > 0) {
      for (Tool tool : coreTools) {
        toolsMenu.add(createToolItem(tool));
      }
      toolsMenu.addSeparator();
    }

    if (contribTools.size() > 0) {
      for (Tool tool : contribTools) {
        toolsMenu.add(createToolItem(tool));
      }
      toolsMenu.addSeparator();
    }

    JMenuItem item = new JMenuItem(Language.text("menu.tools.add_tool"));
    item.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        ContributionManager.openTools();
      }
    });
    toolsMenu.add(item);
  }


  /*
  static public void addTools(JMenu menu, List<Tool> tools) {
    Map<String, JMenuItem> toolItems = new HashMap<String, JMenuItem>();

    for (final Tool tool : tools) {
      // If init() fails, the item won't be added to the menu
      addToolItem(tool, toolItems);
    }

    List<String> toolList = new ArrayList<String>(toolItems.keySet());
    if (toolList.size() > 0) {
      if (menu.getItemCount() != 0) {
        menu.addSeparator();
      }
      Collections.sort(toolList);
      for (String title : toolList) {
        menu.add(toolItems.get(title));
      }
    }
  }
  */


  JMenuItem createToolItem(final Tool tool) { //, Map<String, JMenuItem> toolItems) {
    String title = tool.getMenuTitle();
    final JMenuItem item = new JMenuItem(title);
    item.addActionListener(new ActionListener() {

      public void actionPerformed(ActionEvent e) {
        try {
          tool.run();

        } catch (NoSuchMethodError nsme) {
          activeEditor.statusError("\"" + tool.getMenuTitle() + "\" is not" +
                                   "compatible with this version of Processing");
          //nsme.printStackTrace();
          Messages.loge("Incompatible tool found during tool.run()", nsme);
          item.setEnabled(false);

        } catch (Exception ex) {
          activeEditor.statusError("An error occurred inside \"" + tool.getMenuTitle() + "\"");
          ex.printStackTrace();
          item.setEnabled(false);
        }
      }
    });
    //toolItems.put(title, item);
    return item;
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  public List<ModeContribution> getModeContribs() {
    return modeContribs;
  }


  public List<Mode> getModeList() {
    List<Mode> allModes = new ArrayList<>();
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
    List<Contribution> contributions = new ArrayList<>();

    List<ModeContribution> modeContribs = getModeContribs();
    contributions.addAll(modeContribs);

    for (ModeContribution modeContrib : modeContribs) {
      Mode mode = modeContrib.getMode();
      contributions.addAll(new ArrayList<>(mode.contribLibraries));
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
                                        c.getVersion(), c.getBenignVersion()));
      entries.append(entry);
    }
    String joined =
      "id=" + UpdateCheck.getUpdateID() + "&" + entries.join("&");
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


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  /**
   * Create or modify a sketch.proprties file to specify the given Mode.
   */
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


  String getDefaultModeIdentifier() {
    return "processing.mode.java.JavaMode";
  }


  public Mode getDefaultMode() {
    return coreModes[0];
  }


  /** Used by ThinkDifferent so that it can have a Sketchbook menu. */
  public Mode getNextMode() {
    return nextMode;
  }


  /**
   * The call has already checked to make sure this sketch is not modified,
   * now change the mode.
   * @return true if mode is changed.
   */
  public boolean changeMode(Mode mode) {
    Mode oldMode = activeEditor.getMode();
    if (oldMode != mode) {
      Sketch sketch = activeEditor.getSketch();
      nextMode = mode;

      if (sketch.isUntitled()) {
        // The current sketch is empty, just close and start fresh.
        // (Otherwise the editor would lose its 'untitled' status.)
        handleClose(activeEditor, true);
        handleNew();

      } else {
        // If the current editor contains file extensions that the new mode can handle, then
        // write a sketch.properties file with the new mode specified, and reopen.
        boolean newModeCanHandleCurrentSource = true;
        for (final SketchCode code : sketch.getCode()) {
          if (!mode.validExtension(code.getExtension())) {
            newModeCanHandleCurrentSource = false;
            break;
          }
        }
        if (!newModeCanHandleCurrentSource) {
          return false;
        } else {
          final File props = new File(sketch.getCodeFolder(), "sketch.properties");
          saveModeSettings(props, nextMode);
          handleClose(activeEditor, true);
          Editor editor = handleOpen(sketch.getMainFilePath());
          if (editor == null) {
            // the Mode change failed (probably code that's out of date)
            // re-open the sketch using the mode we were in before
            saveModeSettings(props, oldMode);
            handleOpen(sketch.getMainFilePath());
            return false;
          }
        }
      }
    }
    return true;
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
    final List<Mode> possibleModes = new ArrayList<>();
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
        final String msg =
          "I don't know how to open a sketch with the \"" + extension + "\"\n" +
          "file extension. You'll have to install a different\n" +
          "Mode for that.";
        Messages.showWarning("Modeless Dialog", msg);
      } else {
        Messages.showWarning("Modeless Dialog",
                             "Install " + preferredMode.title + " Mode " +
                             "to open this sketch.");
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
            Messages.showWarning("Time for a Break",
                                 "You've reached the limit for auto naming of new sketches\n" +
                                 "for the day. How about going for a walk instead?", null);
            breakTime = true;
          } else {
            Messages.showWarning("Sunshine",
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

      // Add any template files from the Mode itself
      File newbieFile = nextMode.addTemplateFiles(newbieDir, newbieName);

      /*
      // Make an empty pde file
      File newbieFile =
        new File(newbieDir, newbieName + "." + nextMode.getDefaultExtension()); //$NON-NLS-1$
      if (!newbieFile.createNewFile()) {
        throw new IOException(newbieFile + " already exists.");
      }
      */

      // Create sketch properties file if it's not the default mode.
      if (!nextMode.equals(getDefaultMode())) {
        saveModeSettings(new File(newbieDir, "sketch.properties"), nextMode);
      }

      String path = newbieFile.getAbsolutePath();
      /*Editor editor =*/ handleOpen(path, true);

    } catch (IOException e) {
      Messages.showWarning("That's new to me",
                           "A strange and unexplainable error occurred\n" +
                           "while trying to create a new sketch.", e);
    }
  }


  /**
   * Prompt for a sketch to open, and open it in a new window.
   */
  public void handleOpenPrompt() {
    final StringList extensions = new StringList();
    for (Mode mode : getModeList()) {
      extensions.append(mode.getDefaultExtension());
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


  protected Editor handleOpen(String path, boolean untitled,
                              EditorState state) {
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
            Recent.append(editor);
            return editor;
          }
        }
      }

      if (!Sketch.isSanitaryName(file.getName())) {
        Messages.showWarning("You're tricky, but not tricky enough",
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

      try {
        Editor editor = nextMode.createEditor(this, path, state);

        editor.setUpdatesAvailable(updatesAvailable);

        // opened successfully, let's go to work
        editor.getSketch().setUntitled(untitled);
        editors.add(editor);
        Recent.append(editor);

        // now that we're ready, show the window
        // (don't do earlier, cuz we might move it based on a window being closed)
        editor.setVisible(true);

        return editor;

      } catch (EditorException ee) {
        if (ee.getMessage() != null) {  // null if the user canceled
          Messages.showWarning("Error opening sketch", ee.getMessage(), ee);
        }
      } catch (NoSuchMethodError nsme) {
        Messages.showWarning("Mode out of date",
                             nextMode.getTitle() + " is not compatible with this version of Processing.\n" +
                             "Try updating the Mode or contact its author for a new version.", nsme);
      } catch (Throwable t) {
        if (nextMode.equals(getDefaultMode())) {
          Messages.showTrace("Serious Problem",
                             "An unexpected, unknown, and unrecoverable error occurred\n" +
                             "while opening a new editor window. Please report this.", t, true);
        } else {
          Messages.showTrace("Mode Problems",
                             "A nasty error occurred while trying to use " + nextMode.getTitle() + ".\n" +
                             "It may not be compatible with this version of Processing.\n" +
                             "Try updating the Mode or contact its author for a new version.", t, false);
        }
      }
      if (editors.isEmpty()) {
        Mode defaultMode = getDefaultMode();
        if (nextMode == defaultMode) {
          // unreachable? hopefully?
          Messages.showError("Editor Problems",
                             "An error occurred while trying to change modes.\n" +
                             "We'll have to quit for now because it's an\n" +
                             "unfortunate bit of indigestion with the default Mode.",
                             null);
        } else {
          // Don't leave the user hanging or the PDE locked up
          // https://github.com/processing/processing/issues/4467
          if (untitled) {
            nextMode = defaultMode;
            handleNew();
            return null;  // ignored by any caller

          } else {
            // This null response will be kicked back to changeMode(),
            // signaling it to re-open the sketch in the default Mode.
            return null;
          }
        }
      }

      /*
        if (editors.isEmpty()) {
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
      */

    } catch (Throwable t) {
      Messages.showTrace("Terrible News",
                         "A serious error occurred while " +
                         "trying to create a new editor window.", t,
                         nextMode == getDefaultMode());  // quit if default
      nextMode = getDefaultMode();
    }
    return null;
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


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
      if (Platform.isMacOS()) {
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

//      // This will store the sketch count as zero
//      editors.remove(editor);
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
        defaultFileMenu.insert(Recent.getMenu(), 2);
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

      // Finished with this guy
      Console.shutdown();

      if (!Platform.isMacOS()) {
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


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  /**
   * Asynchronous version of menu rebuild to be used on save and rename
   * to prevent the interface from locking up until the menus are done.
   */
  protected void rebuildSketchbookMenusAsync() {
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
    for (Mode mode : getModeList()) {
      mode.rebuildImportMenu();  // calls rebuildLibraryList
      mode.rebuildToolbarMenu();
      mode.rebuildExamplesFrame();
      mode.rebuildSketchbookFrame();
    }
  }


  protected void rebuildSketchbookMenu() {
    sketchbookMenu.removeAll();
    populateSketchbookMenu(sketchbookMenu);
  }


  public void populateSketchbookMenu(JMenu menu) {
    boolean found = false;
    try {
      found = addSketches(menu, sketchbookFolder, false);
    } catch (IOException e) {
      Messages.showWarning("Sketchbook Menu Error",
                           "An error occurred while trying to list the sketchbook.", e);
    }
    if (!found) {
      JMenuItem empty = new JMenuItem(Language.text("menu.file.sketchbook.empty"));
      empty.setEnabled(false);
      menu.add(empty);
    }
  }


  /*
  public JMenu getRecentMenu() {
    return recent.getMenu();
  }


  public JMenu getToolbarRecentMenu() {
    return recent.getToolbarMenu();
  }


  public void handleRecent(Editor editor) {
    recent.handle(editor);
  }


  public void handleRecentRename(Editor editor, String oldPath) {
    recent.handleRename(editor, oldPath);
  }


  // Called before a sketch is renamed so that its old name is
  // no longer in the menu.
  public void removeRecent(Editor editor) {
    recent.remove(editor);
  }
  */


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

    if (folder.getName().equals("sdk")) {
      // This could be Android's SDK folder. Let's double check:
      File suspectSDKPath = new File(folder.getParent(), folder.getName());
      File expectedSDKPath = new File(sketchbookFolder, "android" + File.separator + "sdk");
      if (expectedSDKPath.getAbsolutePath().equals(suspectSDKPath.getAbsolutePath())) {
        return false;  // Most likely the SDK folder, skip it
      }
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
            Messages.showWarning("Sketch Disappeared",
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


  public boolean addSketches(DefaultMutableTreeNode node, File folder,
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


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  /**
   * Show the Preferences window.
   */
  public void handlePrefs() {
    if (preferencesFrame == null) {
      preferencesFrame = new PreferencesFrame(this);
    }
    preferencesFrame.showFrame();
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  /**
   * Return a File from inside the Processing 'lib' folder.
   */
  static public File getLibFile(String filename) throws IOException {
    return new File(Platform.getContentFile("lib"), filename);
  }


  /**
   * Return an InputStream for a file inside the Processing lib folder.
   */
  static public InputStream getLibStream(String filename) throws IOException {
    return new FileInputStream(getLibFile(filename));
  }


  /**
   * Get the directory that can store settings. (Library on OS X, App Data or
   * something similar on Windows, a dot folder on Linux.) Removed this as a
   * preference for 3.0a3 because we need this to be stable.
   */
  static public File getSettingsFolder() {
    File settingsFolder = null;

    try {
      settingsFolder = Platform.getSettingsFolder();

      // create the folder if it doesn't exist already
      if (!settingsFolder.exists()) {
        if (!settingsFolder.mkdirs()) {
          Messages.showError("Settings issues",
                             "Processing cannot run because it could not\n" +
                             "create a folder to store your settings.\n" +
                             settingsFolder.getAbsolutePath(), null);
        }
      }
    } catch (Exception e) {
      Messages.showTrace("An rare and unknowable thing happened",
                         "Could not get the settings folder. Please report:\n" +
                         "http://github.com/processing/processing/issues/new",
                         e, true);
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


  static public File getToolsFolder() {
    return Platform.getContentFile("tools");
  }


  static public void locateSketchbookFolder() {
    // If a value is at least set, first check to see if the folder exists.
    // If it doesn't, warn the user that the sketchbook folder is being reset.
    String sketchbookPath = Preferences.getSketchbookPath();
    if (sketchbookPath != null) {
      sketchbookFolder = new File(sketchbookPath);
      if (!sketchbookFolder.exists()) {
        Messages.showWarning("Sketchbook folder disappeared",
                             "The sketchbook folder no longer exists.\n" +
                             "Processing will switch to the default sketchbook\n" +
                             "location, and create a new sketchbook folder if\n" +
                             "necessary. Processing will then stop talking\n" +
                             "about itself in the third person.", null);
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
    getSketchbookTemplatesFolder().mkdirs();
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


  static public File getSketchbookTemplatesFolder() {
    return new File(sketchbookFolder, "templates");
  }


  static protected File getDefaultSketchbookFolder() {
    File sketchbookFolder = null;
    try {
      sketchbookFolder = Platform.getDefaultSketchbookFolder();
    } catch (Exception e) { }

    if (sketchbookFolder == null) {
      Messages.showError("No sketchbook",
                         "Problem while trying to get the sketchbook", null);
    }

    // create the folder if it doesn't exist already
    boolean result = true;
    if (!sketchbookFolder.exists()) {
      result = sketchbookFolder.mkdirs();
    }

    if (!result) {
      Messages.showError("You forgot your sketchbook",
                         "Processing cannot run because it could not\n" +
                         "create a folder to store your sketchbook.", null);
    }

    return sketchbookFolder;
  }
}
