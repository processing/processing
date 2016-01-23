/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2013-15 The Processing Foundation
  Copyright (c) 2010-13 Ben Fry and Casey Reas

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
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.*;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.tree.*;

import processing.app.contrib.ContributionManager;
import processing.app.syntax.*;
import processing.app.ui.Editor;
import processing.app.ui.EditorException;
import processing.app.ui.EditorState;
import processing.app.ui.ExamplesFrame;
import processing.app.ui.Recent;
import processing.app.ui.SketchbookFrame;
import processing.app.ui.Toolkit;
import processing.core.PApplet;
import processing.core.PConstants;


public abstract class Mode {
  protected Base base;

  protected File folder;

  protected TokenMarker tokenMarker;
  protected Map<String, String> keywordToReference =
    new HashMap<String, String>();

  protected Settings theme;
//  protected Formatter formatter;
//  protected Tool formatter;

  // maps imported packages to their library folder
  protected Map<String, List<Library>> importToLibraryTable;

  // these menus are shared so that they needn't be rebuilt for all windows
  // each time a sketch is created, renamed, or moved.
  protected JMenu examplesMenu;  // this is for the menubar, not the toolbar
  protected JMenu importMenu;

  protected ExamplesFrame examplesFrame;
  protected SketchbookFrame sketchbookFrame;

  // popup menu used for the toolbar
  protected JMenu toolbarMenu;

  protected File examplesFolder;
  protected File librariesFolder;
  protected File referenceFolder;

//  protected File examplesContribFolder;

  public List<Library> coreLibraries;
  public List<Library> contribLibraries;

  /** Library folder for core. (Used for OpenGL in particular.) */
  protected Library coreLibrary;

  /**
   * ClassLoader used to retrieve classes for this mode. Useful if you want
   * to grab any additional classes that subclass what's in the mode folder.
   */
  protected ClassLoader classLoader;

  static final int BACKGROUND_WIDTH = 1025;
  static final int BACKGROUND_HEIGHT = 65;
  protected Image backgroundImage;

//  public Mode(Base base, File folder) {
//    this(base, folder, base.getSketchbookLibrariesFolder());
//  }


  public Mode(Base base, File folder) {
    this.base = base;
    this.folder = folder;
    tokenMarker = createTokenMarker();

    // Get paths for the libraries and examples in the mode folder
    examplesFolder = new File(folder, "examples");
    librariesFolder = new File(folder, "libraries");
    referenceFolder = new File(folder, "reference");

//    rebuildToolbarMenu();
    rebuildLibraryList();
//    rebuildExamplesMenu();

    try {
      for (File file : getKeywordFiles()) {
        loadKeywords(file);
      }
    } catch (IOException e) {
      Messages.showWarning("Problem loading keywords",
                           "Could not load keywords file for " + getTitle() + " mode.", e);
    }
  }


  /**
   * To add additional keywords, or to grab them from another mode, override
   * this function. If your mode has no keywords, return a zero length array.
   */
  public File[] getKeywordFiles() {
    return new File[] { new File(folder, "keywords.txt") };
  }


  protected void loadKeywords(File keywordFile) throws IOException {
    // overridden for Python, where # is an actual keyword
    loadKeywords(keywordFile, "#");
  }


  protected void loadKeywords(File keywordFile,
                              String commentPrefix) throws IOException {
    BufferedReader reader = PApplet.createReader(keywordFile);
    String line = null;
    while ((line = reader.readLine()) != null) {
      if (!line.trim().startsWith(commentPrefix)) {
        // Was difficult to make sure that mode authors were properly doing
        // tab-separated values. By definition, there can't be additional
        // spaces inside a keyword (or filename), so just splitting on tokens.
        String[] pieces = PApplet.splitTokens(line);
        if (pieces.length >= 2) {
          String keyword = pieces[0];
          String coloring = pieces[1];

          if (coloring.length() > 0) {
            tokenMarker.addColoring(keyword, coloring);
          }
          if (pieces.length == 3) {
            String htmlFilename = pieces[2];
            if (htmlFilename.length() > 0) {
              // if the file is for the version with parens,
              // add a paren to the keyword
              if (htmlFilename.endsWith("_")) {
                keyword += "_";
              }
              keywordToReference.put(keyword, htmlFilename);
            }
          }
        }
      }
    }
    reader.close();
  }


  public void setClassLoader(ClassLoader loader) {
    this.classLoader = loader;
  }


  public ClassLoader getClassLoader() {
    return classLoader;
  }


  /**
   * Setup additional elements that are only required when running with a GUI,
   * rather than from the command-line. Note that this will not be called when
   * the Mode is used from the command line (because Base will be null).
   */
  public void setupGUI() {
    try {
      // First load the default theme data for the whole PDE.
      theme = new Settings(Platform.getContentFile("lib/theme.txt"));

      // The mode-specific theme.txt file should only contain additions,
      // and in extremely rare cases, it might override entries from the
      // main theme. Do not override for style changes unless they are
      // objectively necessary for your Mode.
      File modeTheme = new File(folder, "theme/theme.txt");
      if (modeTheme.exists()) {
        // Override the built-in settings with what the theme provides
        theme.load(modeTheme);
      }

      // other things that have to be set explicitly for the defaults
      theme.setColor("run.window.bgcolor", SystemColor.control);

//      loadBackground();

    } catch (IOException e) {
      Messages.showError("Problem loading theme.txt",
                         "Could not load theme.txt, please re-install Processing", e);
    }
  }


  public File getContentFile(String path) {
    return new File(folder, path);
  }


  public InputStream getContentStream(String path) throws FileNotFoundException {
    return new FileInputStream(getContentFile(path));
  }


  /**
   * Return the pretty/printable/menu name for this mode. This is separate from
   * the single word name of the folder that contains this mode. It could even
   * have spaces, though that might result in sheer madness or total mayhem.
   */
  abstract public String getTitle();


  /**
   * Get an identifier that can be used to resurrect this mode and connect it
   * to a sketch. Using this instead of getTitle() because there might be name
   * clashes with the titles, but there should not be once the actual package,
   * et al. is included.
   * @return full name (package + class name) for this mode.
   */
  public String getIdentifier() {
    return getClass().getCanonicalName();
  }


  /**
   * Create a new editor associated with this mode.
   */
  abstract public Editor createEditor(Base base, String path,
                                      EditorState state) throws EditorException;


  /**
   * Get the folder where this mode is stored.
   * @since 3.0a3
   */
  public File getFolder() {
    return folder;
  }


  public File getExamplesFolder() {
    return examplesFolder;
  }


  public File getLibrariesFolder() {
    return librariesFolder;
  }


  public File getReferenceFolder() {
    return referenceFolder;
  }


  public void rebuildLibraryList() {
    //new Exception("Rebuilding library list").printStackTrace(System.out);
    // reset the table mapping imports to libraries
    importToLibraryTable = new HashMap<String, List<Library>>();

    Library core = getCoreLibrary();
    if (core != null) {
      core.addPackageList(importToLibraryTable);
    }

    coreLibraries = Library.list(librariesFolder);
    File contribLibrariesFolder = Base.getSketchbookLibrariesFolder();
    contribLibraries = Library.list(contribLibrariesFolder);

    // Check to see if video and sound are installed and move them
    // from the contributed list to the core list.
    List<Library> foundationLibraries = new ArrayList<>();
    for (Library lib : contribLibraries) {
      if (lib.isFoundation()) {
        foundationLibraries.add(lib);
      }
    }
    coreLibraries.addAll(foundationLibraries);
    contribLibraries.removeAll(foundationLibraries);

    /*
    File sketchbookLibs = Base.getSketchbookLibrariesFolder();
    File videoFolder = new File(sketchbookLibs, "video");
    if (videoFolder.exists()) {
      coreLibraries.add(new Library(videoFolder));
    }
    File soundFolder = new File(sketchbookLibs, "sound");
    if (soundFolder.exists()) {
      coreLibraries.add(new Library(soundFolder));
    }
    */

    for (Library lib : coreLibraries) {
      lib.addPackageList(importToLibraryTable);
    }

    for (Library lib : contribLibraries) {
      lib.addPackageList(importToLibraryTable);
    }
  }


  public Library getCoreLibrary() {
    return null;
  }


  public Library getLibrary(String pkgName) throws SketchException {
    List<Library> libraries = importToLibraryTable.get(pkgName);
    if (libraries == null) {
      return null;

    } else if (libraries.size() > 1) {
      String primary = "More than one library is competing for this sketch.";
      String secondary = "The import " + pkgName + " points to multiple libraries:<br>";
      for (Library library : libraries) {
        String location = library.getPath();
        if (location.startsWith(getLibrariesFolder().getAbsolutePath())) {
          location = "part of Processing";
        }
        secondary += "<b>" + library.getName() + "</b> (" + location + ")<br>";
      }
      secondary += "Extra libraries need to be removed before this sketch can be used.";
      Messages.showWarningTiered("Duplicate Library Problem", primary, secondary, null);
      throw new SketchException("Duplicate libraries found for " + pkgName + ".");

    } else {
      return libraries.get(0);
    }
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


//  abstract public EditorToolbar createToolbar(Editor editor);


  public JMenu getToolbarMenu() {
    if (toolbarMenu == null) {
//      toolbarMenu = new JMenu();
      rebuildToolbarMenu();
    }
    return toolbarMenu;
  }


  public void insertToolbarRecentMenu() {
    if (toolbarMenu == null) {
      rebuildToolbarMenu();
    } else {
      toolbarMenu.insert(Recent.getToolbarMenu(), 1);
    }
  }


  public void removeToolbarRecentMenu() {
    toolbarMenu.remove(Recent.getToolbarMenu());
  }


  protected void rebuildToolbarMenu() {  //JMenu menu) {
    JMenuItem item;
    if (toolbarMenu == null) {
      toolbarMenu = new JMenu();
    } else {
      toolbarMenu.removeAll();
    }

    //System.out.println("rebuilding toolbar menu");
    // Add the single "Open" item
    item = Toolkit.newJMenuItem("Open...", 'O');
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          base.handleOpenPrompt();
        }
      });
    toolbarMenu.add(item);

    insertToolbarRecentMenu();

    item = Toolkit.newJMenuItemShift("Examples...", 'O');
    item.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        showExamplesFrame();
      }
    });
    toolbarMenu.add(item);

    item = new JMenuItem(Language.text("examples.add_examples"));
    item.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        ContributionManager.openExamples();
      }
    });
    toolbarMenu.add(item);

    // Add a list of all sketches and subfolders
    toolbarMenu.addSeparator();
    base.populateSketchbookMenu(toolbarMenu);
//    boolean found = false;
//    try {
//      found = base.addSketches(toolbarMenu, base.getSketchbookFolder(), true);
//    } catch (IOException e) {
//      Base.showWarning("Sketchbook Toolbar Error",
//                       "An error occurred while trying to list the sketchbook.", e);
//    }
//    if (!found) {
//      JMenuItem empty = new JMenuItem("(empty)");
//      empty.setEnabled(false);
//      toolbarMenu.add(empty);
//    }
  }


  protected int importMenuIndex = -1;

  /**
   * Rather than re-building the library menu for every open sketch (very slow
   * and prone to bugs when updating libs, particularly with the contribs mgr),
   * share a single instance across all windows.
   * @since 3.0a6
   * @param sketchMenu the Sketch menu that's currently active
   */
  public void removeImportMenu(JMenu sketchMenu) {
    JMenu importMenu = getImportMenu();
    //importMenuIndex = sketchMenu.getComponentZOrder(importMenu);
    importMenuIndex = Toolkit.getMenuItemIndex(sketchMenu, importMenu);
    sketchMenu.remove(importMenu);
  }


  /**
   * Re-insert the Import Library menu. Added function so that other modes
   * need not have an 'import' menu.
   * @since 3.0a6
   * @param sketchMenu the Sketch menu that's currently active
   */
  public void insertImportMenu(JMenu sketchMenu) {
    // hard-coded as 4 in 3.0a5, change to 5 for 3.0a6, but... yuck
    //sketchMenu.insert(mode.getImportMenu(), 4);
    // This is -1 on when the editor window is first shown, but that's fine
    // because the import menu has just been added in the Editor constructor.
    if (importMenuIndex != -1) {
      sketchMenu.insert(getImportMenu(), importMenuIndex);
    }
  }


  public JMenu getImportMenu() {
    if (importMenu == null) {
      rebuildImportMenu();
    }
    return importMenu;
  }


  public void rebuildImportMenu() {  //JMenu importMenu) {
    if (importMenu == null) {
      importMenu = new JMenu(Language.text("menu.library"));
      Editor.MenuScroller.setScrollerFor(importMenu, Editor.SCROLL_COUNT);
    } else {
      //System.out.println("rebuilding import menu");
      importMenu.removeAll();
    }

    JMenuItem addLib = new JMenuItem(Language.text("menu.library.add_library"));
    addLib.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        ContributionManager.openLibraries();
      }
    });
    importMenu.add(addLib);
    importMenu.addSeparator();

    rebuildLibraryList();

    ActionListener listener = new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        base.activeEditor.handleImportLibrary(e.getActionCommand());
      }
    };

//    try {
//      pw = new PrintWriter(new FileWriter(System.getProperty("user.home") + "/Desktop/libs.csv"));
//    } catch (IOException e1) {
//      e1.printStackTrace();
//    }

    if (coreLibraries.size() == 0) {
      JMenuItem item = new JMenuItem(getTitle() + " " + Language.text("menu.library.no_core_libraries"));
      item.setEnabled(false);
      importMenu.add(item);
    } else {
      for (Library library : coreLibraries) {
        JMenuItem item = new JMenuItem(library.getName());
        item.addActionListener(listener);

        // changed to library-name to facilitate specification of imports from properties file
        item.setActionCommand(library.getName());

        importMenu.add(item);
      }
    }

    if (contribLibraries.size() != 0) {
      importMenu.addSeparator();
      JMenuItem contrib = new JMenuItem(Language.text("menu.library.contributed"));
      contrib.setEnabled(false);
      importMenu.add(contrib);

      HashMap<String, JMenu> subfolders = new HashMap<String, JMenu>();

      for (Library library : contribLibraries) {
        JMenuItem item = new JMenuItem(library.getName());
        item.addActionListener(listener);

        // changed to library-name to facilitate specification if imports from properties file
        item.setActionCommand(library.getName());

        String group = library.getGroup();
        if (group != null) {
          JMenu subMenu = subfolders.get(group);
          if (subMenu == null) {
            subMenu = new JMenu(group);
            importMenu.add(subMenu);
            subfolders.put(group, subMenu);
          }
          subMenu.add(item);
        } else {
          importMenu.add(item);
        }
      }
    }
  }


  /**
   * Override this to control the order of the first set of example folders
   * and how they appear in the examples window.
   */
  public File[] getExampleCategoryFolders() {
    return examplesFolder.listFiles(new FilenameFilter() {
      public boolean accept(File dir, String name) {
        return dir.isDirectory() && name.charAt(0) != '.';
      }
    });
  }


  public void rebuildExamplesFrame() {
    if (examplesFrame != null) {
      boolean visible = examplesFrame.isVisible();
      Rectangle bounds = null;
      if (visible) {
        bounds = examplesFrame.getBounds();
        examplesFrame.setVisible(false);
      }
      examplesFrame = null;
      if (visible) {
        showExamplesFrame();
        examplesFrame.setBounds(bounds);
      }
    }
  }


  public void showExamplesFrame() {
    if (examplesFrame == null) {
      examplesFrame = new ExamplesFrame(base, this);
    }
    examplesFrame.setVisible();
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  public DefaultMutableTreeNode buildSketchbookTree() {
    DefaultMutableTreeNode sbNode =
      new DefaultMutableTreeNode(Language.text("sketchbook.tree"));
    try {
      base.addSketches(sbNode, Base.getSketchbookFolder(), false);
    } catch (IOException e) {
      e.printStackTrace();
    }
    return sbNode;
  }


  /** Sketchbook has changed, update it on next viewing. */
  public void rebuildSketchbookFrame() {
    boolean wasVisible =
      (sketchbookFrame == null) ? false : sketchbookFrame.isVisible();
    sketchbookFrame = null;  // Force a rebuild
    if (wasVisible) {
      showSketchbookFrame();
    }
  }


  public void showSketchbookFrame() {
    if (sketchbookFrame == null) {
      sketchbookFrame = new SketchbookFrame(base, this);
    }
    sketchbookFrame.setVisible();
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  /**
   * Get an ImageIcon object from the Mode folder.
   * Or when prefixed with /lib, load it from the main /lib folder.
   * @since 3.0a6
   */
  public ImageIcon loadIcon(String filename) {
    if (filename.startsWith("/lib/")) {
      return Toolkit.getLibIcon(filename.substring(5));
    }
    File file = new File(folder, filename);
    if (!file.exists()) {
//      EditorConsole.systemErr.println("file does not exist: " + file.getAbsolutePath());
      return null;
    }
//    EditorConsole.systemErr.println("found: " + file.getAbsolutePath());
    return new ImageIcon(file.getAbsolutePath());
  }


  /**
   * Get an image object from the mode folder.
   * Or when prefixed with /lib, load it from the main /lib folder.
   */
  public Image loadImage(String filename) {
    ImageIcon icon = loadIcon(filename);
    if (icon != null) {
      return icon.getImage();
    }
    return null;
  }


  public Image loadImageX(String filename) {
    final int res = Toolkit.highResDisplay() ? 2 : 1;
    return loadImage(filename + "-" + res +  "x.png");
  }


//  public EditorButton loadButton(String name) {
//    return new EditorButton(this, name);
//  }


  //public Settings getTheme() {
  //  return theme;
  //}


  /**
   * Returns the HTML filename (including path prefix if necessary)
   * for this keyword, or null if it doesn't exist.
   */
  public String lookupReference(String keyword) {
    return keywordToReference.get(keyword);
  }


  //public TokenMarker getTokenMarker() throws IOException {
  //  File keywordsFile = new File(folder, "keywords.txt");
  //  return new PdeKeywords(keywordsFile);
  //}
  public TokenMarker getTokenMarker() {
    return tokenMarker;
  }

  protected TokenMarker createTokenMarker() {
    return new PdeKeywords();
  }


//  abstract public Formatter createFormatter();


//  public Formatter getFormatter() {
//    return formatter;
//  }


//  public Tool getFormatter() {
//    return formatter;
//  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

  // Get attributes/values from the theme.txt file. To discourage burying this
  // kind of information in code where it doesn't belong (and is difficult to
  // track down), these don't have a "default" option as a second parameter.


  /** @since 3.0a6 */
  public String getString(String attribute) {
    return theme.get(attribute);
  }


  public boolean getBoolean(String attribute) {
    return theme.getBoolean(attribute);
  }


  public int getInteger(String attribute) {
    return theme.getInteger(attribute);
  }


  public Color getColor(String attribute) {
    return theme.getColor(attribute);
  }


  public Font getFont(String attribute) {
    return theme.getFont(attribute);
  }


  public SyntaxStyle getStyle(String attribute) {
    String str = Preferences.get("editor.token." + attribute + ".style");
    if (str == null) {
      throw new IllegalArgumentException("No style found for " + attribute);
    }

    StringTokenizer st = new StringTokenizer(str, ",");

    String s = st.nextToken();
    if (s.indexOf("#") == 0) s = s.substring(1);
    Color color = new Color(Integer.parseInt(s, 16));

    s = st.nextToken();
    boolean bold = (s.indexOf("bold") != -1);
//    boolean italic = (s.indexOf("italic") != -1);

//    return new SyntaxStyle(color, italic, bold);
    return new SyntaxStyle(color, bold);
  }


  public Image makeGradient(String attribute, int wide, int high) {
    int top = getColor(attribute + ".gradient.top").getRGB();
    int bot = getColor(attribute + ".gradient.bottom").getRGB();

//    float r1 = (top >> 16) & 0xff;
//    float g1 = (top >> 8) & 0xff;
//    float b1 = top & 0xff;
//    float r2 = (bot >> 16) & 0xff;
//    float g2 = (bot >> 8) & 0xff;
//    float b2 = bot & 0xff;

    BufferedImage outgoing =
      new BufferedImage(wide, high, BufferedImage.TYPE_INT_RGB);
    int[] row = new int[wide];
    WritableRaster wr = outgoing.getRaster();
    for (int i = 0; i < high; i++) {
//      Arrays.fill(row, (255 - (i + GRADIENT_TOP)) << 24);
//      int r = (int) PApplet.map(i, 0, high-1, r1, r2);
      int rgb = PApplet.lerpColor(top, bot, i / (float)(high-1), PConstants.RGB);
      Arrays.fill(row, rgb);
//      System.out.println(PApplet.hex(row[0]));
      wr.setDataElements(0, i, wide, 1, row);
    }
//    Graphics g = outgoing.getGraphics();
//    for (int i = 0; i < steps; i++) {
//      g.setColor(new Color(1, 1, 1, 255 - (i + GRADIENT_TOP)));
//      //g.fillRect(0, i, EditorButton.DIM, 10);
//      g.drawLine(0, i, EditorButton.DIM, i);
//    }
    return outgoing;
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  // Breaking out extension types in order to clean up the code, and make it
  // easier for other environments (like Arduino) to incorporate changes.


  /**
   * True if the specified extension should be hidden when shown on a tab.
   * For Processing, this is true for .pde files. (Broken out for subclasses.)
   * You can override this in your Mode subclass to handle it differently.
   */
  public boolean hideExtension(String what) {
    return what.equals(getDefaultExtension());
  }


  /**
   * True if the specified code has the default file extension.
   */
  public boolean isDefaultExtension(SketchCode code) {
    return code.getExtension().equals(getDefaultExtension());
  }


  /**
   * True if the specified extension is the default file extension.
   */
  public boolean isDefaultExtension(String what) {
    return what.equals(getDefaultExtension());
  }


  /**
   * @param f File to be checked against this mode's accepted extensions.
   * @return Whether or not the given file name features an extension supported by this mode.
   */
  public boolean canEdit(final File f) {
    final int dot = f.getName().lastIndexOf('.');
    if (dot < 0) {
      return false;
    }
    return validExtension(f.getName().substring(dot + 1));
  }

  /**
   * Check this extension (no dots, please) against the list of valid
   * extensions.
   */
  public boolean validExtension(String what) {
    String[] ext = getExtensions();
    for (int i = 0; i < ext.length; i++) {
      if (ext[i].equals(what)) return true;
    }
    return false;
  }


  /**
   * Returns the default extension for this editor setup.
   */
  abstract public String getDefaultExtension();


  /**
   * Returns the appropriate file extension to use for auxilliary source files in a sketch.
   * For example, in a Java-mode sketch, auxilliary files should be name "Foo.java"; in
   * Python mode, they should be named "foo.py".
   *
   * <p>Modes that do not override this function will get the default behavior of returning the
   * default extension.
   */
  public String getModuleExtension() {
    return getDefaultExtension();
  }


  /**
   * Returns a String[] array of proper extensions.
   */
  abstract public String[] getExtensions();


  /**
   * Get array of file/directory names that needn't be copied during "Save As".
   */
  abstract public String[] getIgnorable();


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

  /**
   * Checks coreLibraries and contribLibraries for a library with the specified name
   * @param libName the name of the library to find
   * @return the Library or null if not found
   */
  public Library findLibraryByName(String libName) {

    for (Library lib : this.coreLibraries) {
      if (libName.equals(lib.getName()))
        return lib;
    }

    for (Library lib : this.contribLibraries) {
      if (libName.equals(lib.getName()))
        return lib;
    }

    return null;
  }

  /**
   * Create a fresh applet/application folder if the 'delete target folder'
   * pref has been set in the preferences.
   */
  public void prepareExportFolder(File targetFolder) {
    if (targetFolder != null) {
      // Nuke the old applet/application folder because it can cause trouble
      if (Preferences.getBoolean("export.delete_target_folder")) {
//        System.out.println("temporarily skipping deletion of " + targetFolder);
        Util.removeDir(targetFolder);
        //      targetFolder.renameTo(dest);
      }
      // Create a fresh output folder (needed before preproc is run next)
      targetFolder.mkdirs();
    }
  }

//  public void handleNew() {
//    base.handleNew();
//  }
//
//
//  public void handleNewReplace() {
//    base.handleNewReplace();
//  }


  // this is Java-specific, so keeping it in JavaMode
//  public String getSearchPath() {
//    return null;
//  }


  @Override
  public String toString() {
    return getTitle();
  }
}
