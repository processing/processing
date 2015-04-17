/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2013 The Processing Foundation
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

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.plaf.basic.BasicTreeUI;
import javax.swing.tree.*;

import processing.app.syntax.*;
import processing.core.PApplet;
import processing.core.PConstants;


public abstract class Mode {
  protected Base base;

  protected File folder;

  protected TokenMarker tokenMarker;
  protected HashMap<String, String> keywordToReference =
    new HashMap<String, String>();

  protected Settings theme;
//  protected Formatter formatter;
//  protected Tool formatter;

  // maps imported packages to their library folder
//  protected HashMap<String, Library> importToLibraryTable;
  protected HashMap<String, ArrayList<Library>> importToLibraryTable;

  // these menus are shared so that they needn't be rebuilt for all windows
  // each time a sketch is created, renamed, or moved.
  protected JMenu examplesMenu;  // this is for the menubar, not the toolbar
  protected JMenu importMenu;

//  protected JTree examplesTree;
  protected JFrame examplesFrame;

  // popup menu used for the toolbar
  protected JMenu toolbarMenu;

  protected File examplesFolder;
  protected File librariesFolder;
  protected File referenceFolder;

  protected File examplesContribFolder;

  public ArrayList<Library> coreLibraries;
  public ArrayList<Library> contribLibraries;

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

    // Get path to the contributed examples compatible with this mode
    examplesContribFolder = Base.getSketchbookExamplesFolder();

//    rebuildToolbarMenu();
    rebuildLibraryList();
//    rebuildExamplesMenu();

    try {
      for (File file : getKeywordFiles()) {
        loadKeywords(file);
      }
    } catch (IOException e) {
      Base.showWarning("Problem loading keywords",
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
      theme = new Settings(Base.getContentFile("lib/theme.txt"));

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
      Base.showError("Problem loading theme.txt",
                     "Could not load theme.txt, please re-install Processing", e);
    }
  }


  /*
  protected void loadBackground() {
    String suffix = Toolkit.highResDisplay() ? "-2x.png" : ".png";
    backgroundImage = loadImage("theme/mode" + suffix);
    if (backgroundImage == null) {
      // If the image wasn't available, try the other resolution.
      // i.e. we don't (currently) have low-res versions of mode.png,
      // so this will grab the 2x version and scale it when drawn.
      suffix = !Toolkit.highResDisplay() ? "-2x.png" : ".png";
      backgroundImage = loadImage("theme/mode" + suffix);
    }
  }


  public void drawBackground(Graphics g, int offset) {
    if (backgroundImage != null) {
      if (!Toolkit.highResDisplay()) {
        // Image might be downsampled from a 2x version. If so, we need nice
        // anti-aliasing for the very geometric images we're using.
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                            RenderingHints.VALUE_INTERPOLATION_BICUBIC);
      }
      g.drawImage(backgroundImage, 0, -offset,
                  BACKGROUND_WIDTH, BACKGROUND_HEIGHT, null);
    }
  }
  */


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
  abstract public Editor createEditor(Base base, String path, EditorState state);
  //abstract public Editor createEditor(Base base, String path, int[] location);


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
    // reset the table mapping imports to libraries
    importToLibraryTable = new HashMap<String, ArrayList<Library>>();

    coreLibraries = Library.list(librariesFolder);
    for (Library lib : coreLibraries) {
      lib.addPackageList(importToLibraryTable);
    }

    File contribLibrariesFolder = Base.getSketchbookLibrariesFolder();
    if (contribLibrariesFolder != null) {
      contribLibraries = Library.list(contribLibrariesFolder);
      for (Library lib : contribLibraries) {
        lib.addPackageList(importToLibraryTable);
      }
    }
  }


  public Library getCoreLibrary() {
    return null;
  }


  public Library getLibrary(String pkgName) throws SketchException {
    ArrayList<Library> libraries = importToLibraryTable.get(pkgName);
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
      Base.showWarningTiered("Duplicate Library Problem", primary, secondary, null);
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
      toolbarMenu.insert(base.getToolbarRecentMenu(), 1);
    }
  }


  public void removeToolbarRecentMenu() {
    toolbarMenu.remove(base.getToolbarRecentMenu());
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
        base.handleOpenExampleManager();
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
    } else {
      //System.out.println("rebuilding import menu");
      importMenu.removeAll();
    }

    JMenuItem addLib = new JMenuItem(Language.text("menu.library.add_library"));
    addLib.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        base.handleOpenLibraryManager();
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


  /*
  public JMenu getExamplesMenu() {
    if (examplesMenu == null) {
      rebuildExamplesMenu();
    }
    return examplesMenu;
  }


  public void rebuildExamplesMenu() {
    if (examplesMenu == null) {
      examplesMenu = new JMenu("Examples");
    }
    rebuildExamplesMenu(examplesMenu, false);
  }


  public void rebuildExamplesMenu(JMenu menu, boolean replace) {
    try {
      // break down the examples folder for examples
      File[] subfolders = getExampleCategoryFolders();

      for (File sub : subfolders) {
        Base.addDisabledItem(menu, sub.getName());
//        JMenuItem categoryItem = new JMenuItem(sub.getName());
//        categoryItem.setEnabled(false);
//        menu.add(categoryItem);
        base.addSketches(menu, sub, replace);
        menu.addSeparator();
      }

//      if (coreLibraries == null) {
//        rebuildLibraryList();
//      }

      // get library examples
      Base.addDisabledItem(menu, "Libraries");
      for (Library lib : coreLibraries) {
        if (lib.hasExamples()) {
          JMenu libMenu = new JMenu(lib.getName());
          base.addSketches(libMenu, lib.getExamplesFolder(), replace);
          menu.add(libMenu);
        }
      }

      // get contrib library examples
      boolean any = false;
      for (Library lib : contribLibraries) {
        if (lib.hasExamples()) {
          any = true;
        }
      }
      if (any) {
        menu.addSeparator();
        Base.addDisabledItem(menu, "Contributed");
        for (Library lib : contribLibraries) {
          if (lib.hasExamples()) {
            JMenu libMenu = new JMenu(lib.getName());
            base.addSketches(libMenu, lib.getExamplesFolder(), replace);
            menu.add(libMenu);
          }
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
  */


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


  public DefaultMutableTreeNode buildExamplesTree() {
    DefaultMutableTreeNode root = new DefaultMutableTreeNode("Examples");

    try {

      File[] examples = getExampleCategoryFolders();

      for (File subFolder : examples) {
        DefaultMutableTreeNode subNode = new DefaultMutableTreeNode(subFolder.getName());
        if (base.addSketches(subNode, subFolder)) {
          root.add(subNode);
        }
      }

      DefaultMutableTreeNode foundationLibraries =
        new DefaultMutableTreeNode(Language.text("examples.core_libraries"));

      // Get examples for core libraries
      for (Library lib : coreLibraries) {
        if (lib.hasExamples()) {
          DefaultMutableTreeNode libNode = new DefaultMutableTreeNode(lib.getName());
          if (base.addSketches(libNode, lib.getExamplesFolder()))
            foundationLibraries.add(libNode);
        }
      }
      if(foundationLibraries.getChildCount() > 0) {
        root.add(foundationLibraries);
      }

      // Get examples for third party libraries
      DefaultMutableTreeNode contributed = new
        DefaultMutableTreeNode(Language.text("examples.libraries"));
      for (Library lib : contribLibraries) {
        if (lib.hasExamples()) {
            DefaultMutableTreeNode libNode = new DefaultMutableTreeNode(lib.getName());
            base.addSketches(libNode, lib.getExamplesFolder());
          contributed.add(libNode);
        }
      }
      if(contributed.getChildCount() > 0){
        root.add(contributed);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

    return root;
  }

  public void resetExamples() {
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


  /**
   * Function to give a JTree a pretty alternating gray-white colouring for
   * its rows.
   *
   * @param tree
   */
  private void colourizeTreeRows(JTree tree) {
    // Code in this function adapted from:
    // http://mateuszstankiewicz.eu/?p=263
    tree.setCellRenderer(new DefaultTreeCellRenderer() {

      @Override
      public Component getTreeCellRendererComponent(JTree tree, Object value,
                                                    boolean sel,
                                                    boolean expanded,
                                                    boolean leaf, int row,
                                                    boolean hasFocus) {
        JComponent c = (JComponent) super
          .getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row,
                                        hasFocus);

        if (!tree.isRowSelected(row)) {
          if (row % 2 == 0) {

            // Need to set this, else the gray from the odd
            // rows colours this gray as well.
            c.setBackground(new Color(255, 255, 255));

            setBackgroundSelectionColor(new Color(0, 0, 255));
            setTextSelectionColor(Color.WHITE);
            setBorderSelectionColor(new Color(0, 0, 255));
          } else {

            // Set background for entire component (including the image).
            // Using transparency messes things up, probably since the
            // transparent colour is not good friends with the images background colour.
            c.setBackground(new Color(240, 240, 240));

            // Can't use setBackgroundSelectionColor() directly, since then, the
            // image's background isn't affected.
            // The setUI() doesn't fix the image's background because the
            // transparency likely interferes with its normal background,
            // making its background lighter than the rest.
//            setBackgroundNonSelectionColor(new Color(190, 190, 190));

            setBackgroundSelectionColor(new Color(0, 0, 255));
            setTextSelectionColor(Color.WHITE);
            setBorderSelectionColor(new Color(0, 0, 255));
          }
        } else {// Transparent blue if selected
          c.setBackground(new Color(127, 127, 255));
        }

        c.setOpaque(true);
        return c;
      }

    });

    tree.setUI(new BasicTreeUI() {

      @Override
      protected void paintRow(Graphics g, Rectangle clipBounds, Insets insets,
                              Rectangle bounds, TreePath path, int row,
                              boolean isExpanded, boolean hasBeenExpanded,
                              boolean isLeaf) {
        Graphics g2 = g.create();

        if (!tree.isRowSelected(row)) {
          if (row % 2 == 0) {
            // Need to set this, else the gray from the odd rows
            // affects the even rows too.
            g2.setColor(new Color(255, 255, 255, 128));
          } else {
            // Transparent light-gray
            g2.setColor(new Color(226, 226, 226, 128));
          }
        } else
          // Transparent blue if selected
          g2.setColor(new Color(0, 0, 255, 128));

        g2.fillRect(0, bounds.y, tree.getWidth(), bounds.height);

        g2.dispose();

        super.paintRow(g, clipBounds, insets, bounds, path, row, isExpanded,
                       hasBeenExpanded, isLeaf);
      }
    });
  }


  public void showExamplesFrame() {
    if (examplesFrame == null) {
      examplesFrame = new JFrame(getTitle() + " " + Language.text("examples"));
      Toolkit.setIcon(examplesFrame);
      Toolkit.registerWindowCloseKeys(examplesFrame.getRootPane(), new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          examplesFrame.setVisible(false);
        }
      });

      JPanel examplesPanel = new JPanel();
      examplesPanel.setLayout(new BorderLayout());
      examplesPanel.setBackground(Color.WHITE);

      final JPanel openExamplesManagerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
      JButton addExamplesButton = new JButton(Language.text("examples.add_examples"));
      openExamplesManagerPanel.add(addExamplesButton);
      openExamplesManagerPanel.setOpaque(false);
      Border lineBorder = BorderFactory.createMatteBorder(0, 0, 1, 0, Color.BLACK);
      Border paddingBorder = BorderFactory.createEmptyBorder(3, 5, 1, 4);
      openExamplesManagerPanel.setBorder(BorderFactory.createCompoundBorder(lineBorder, paddingBorder));
      openExamplesManagerPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
      openExamplesManagerPanel.setCursor(new Cursor(Cursor.HAND_CURSOR));
      addExamplesButton.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          base.handleOpenExampleManager();
        }
      });

      final JTree tree = new JTree(buildExamplesTree());

      colourizeTreeRows(tree);

      tree.setOpaque(true);
      tree.setAlignmentX(Component.LEFT_ALIGNMENT);

      tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
      tree.setShowsRootHandles(true);
      // expand the root
      tree.expandRow(0);
      // now hide the root
      tree.setRootVisible(false);

      // After 2.0a7, no longer expanding each of the categories at Casey's
      // request. He felt that the window was too complicated too quickly.
//      for (int row = tree.getRowCount()-1; row >= 0; --row) {
//        tree.expandRow(row);
//      }

      tree.addMouseListener(new MouseAdapter() {
        public void mouseClicked(MouseEvent e) {
          if (e.getClickCount() == 2) {
            DefaultMutableTreeNode node =
              (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();

            int selRow = tree.getRowForLocation(e.getX(), e.getY());
            //TreePath selPath = tree.getPathForLocation(e.getX(), e.getY());
            //if (node != null && node.isLeaf() && node.getPath().equals(selPath)) {
            if (node != null && node.isLeaf() && selRow != -1) {
              SketchReference sketch = (SketchReference) node.getUserObject();
              base.handleOpen(sketch.getPath());
            }
          }
        }
      });
      tree.addKeyListener(new KeyAdapter() {
        public void keyPressed(KeyEvent e) {
          if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {  // doesn't fire keyTyped()
            examplesFrame.setVisible(false);
          }
        }
        public void keyTyped(KeyEvent e) {
          if (e.getKeyChar() == KeyEvent.VK_ENTER) {
            DefaultMutableTreeNode node =
              (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
            if (node != null && node.isLeaf()) {
              SketchReference sketch = (SketchReference) node.getUserObject();
              base.handleOpen(sketch.getPath());
            }
          }
        }
      });

      tree.addTreeExpansionListener(new TreeExpansionListener() {
        @Override
        public void treeExpanded(TreeExpansionEvent event) {
          updateExpanded(tree);
        }

        @Override
        public void treeCollapsed(TreeExpansionEvent event) {
          updateExpanded(tree);
        }
      });

      tree.setBorder(new EmptyBorder(0, 5, 5, 5));
      if (Base.isMacOS()) {
        tree.setToggleClickCount(2);
      } else {
        tree.setToggleClickCount(1);
      }

      JScrollPane treePane = new JScrollPane(tree);
      treePane.setPreferredSize(new Dimension(250, 300));
      treePane.setBorder(new EmptyBorder(2, 0, 0, 0));
      treePane.setOpaque(true);
      treePane.setBackground(Color.WHITE);
      treePane.setAlignmentX(Component.LEFT_ALIGNMENT);

      examplesPanel.add(openExamplesManagerPanel,BorderLayout.PAGE_START);
      examplesPanel.add(treePane, BorderLayout.CENTER);
      examplesFrame.getContentPane().add(examplesPanel);
      examplesFrame.pack();

      restoreExpanded(tree);
    }

    // Space for the editor plus a li'l gap
    int roughWidth = examplesFrame.getWidth() + 20;
    Point p = null;
    // If no window open, or the editor is at the edge of the screen
    if (base.activeEditor == null ||
        (p = base.activeEditor.getLocation()).x < roughWidth) {
      // Center the window on the screen
      examplesFrame.setLocationRelativeTo(null);
    } else {
      // Open the window relative to the editor
      examplesFrame.setLocation(p.x - roughWidth, p.y);
    }
    examplesFrame.setVisible(true);
  }


  protected void updateExpanded(JTree tree) {
    Enumeration en = tree.getExpandedDescendants(new TreePath(tree.getModel().getRoot()));
    //en.nextElement();  // skip the root "Examples" node

    StringBuilder s = new StringBuilder();
    while (en.hasMoreElements()) {
      //System.out.println(en.nextElement());
      TreePath tp = (TreePath) en.nextElement();
      Object[] path = tp.getPath();
      for (Object o : path) {
        DefaultMutableTreeNode p = (DefaultMutableTreeNode) o;
        String name = (String) p.getUserObject();
        //System.out.print(p.getUserObject().getClass().getName() + ":" + p.getUserObject() + " -> ");
        //System.out.print(name + " -> ");
        s.append(name);
        s.append(File.separatorChar);
      }
      //System.out.println();
      s.setCharAt(s.length() - 1, File.pathSeparatorChar);
    }
    s.setLength(s.length() - 1);  // nix that last separator
    String pref = "examples." + getClass().getName() + ".visible";
    Preferences.set(pref, s.toString());
    Preferences.save();
//    System.out.println(s);
//    System.out.println();
  }


  protected void restoreExpanded(JTree tree) {
    String pref = "examples." + getClass().getName() + ".visible";
    String value = Preferences.get(pref);
    if (value != null) {
      String[] paths = PApplet.split(value, File.pathSeparator);
      for (String path : paths) {
//        System.out.println("trying to expand " + path);
        String[] items = PApplet.split(path, File.separator);
        DefaultMutableTreeNode[] nodes = new DefaultMutableTreeNode[items.length];
        expandTree(tree, null, items, nodes, 0);
      }
    }
  }


  void expandTree(JTree tree, Object object, String[] items, DefaultMutableTreeNode[] nodes, int index) {
//    if (object == null) {
//      object = model.getRoot();
//    }
    TreeModel model = tree.getModel();

    if (index == 0) {
      nodes[0] = (DefaultMutableTreeNode) model.getRoot();
      expandTree(tree, nodes[0], items, nodes, 1);

    } else if (index < items.length) {
//    String item = items[0];
//    TreeModel model = object.getModel();
//    System.out.println(object.getClass().getName());
      DefaultMutableTreeNode node = (DefaultMutableTreeNode) object;
      int count = model.getChildCount(node);
//    System.out.println("child count is " + count);
      for (int i = 0; i < count; i++) {
        DefaultMutableTreeNode child = (DefaultMutableTreeNode) model.getChild(node, i);
        if (items[index].equals(child.getUserObject())) {
          nodes[index] = child;
          expandTree(tree, child, items, nodes, index+1);
        }
      }
    } else {  // last one
//      PApplet.println(nodes);
      tree.expandPath(new TreePath(nodes));
    }
  }


//  void

//  protected TreePath findPath(FileItem item) {
//    ArrayList<FileItem> items = new ArrayList<FileItem>();
////    FileItem which = item.isDirectory() ? item : (FileItem) item.getParent();
////    FileItem which = item;
//    FileItem which = (FileItem) item.getParent();
//    while (which != null) {
//      items.add(0, which);
//      which = (FileItem) which.getParent();
//    }
//    return new TreePath(items.toArray());
////    FileItem[] array = items.toArray();
////    return new TreePath(array);
//  }


//  public static void loadExpansionState(JTree tree, Enumeration enumeration) {
//    if (enumeration != null) {
//      while (enumeration.hasMoreElements()) {
//        TreePath treePath = (TreePath) enumeration.nextElement();
//        tree.expandPath(treePath);
//      }
//    }
//  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

  public DefaultMutableTreeNode buildSketchbookTree(){
    DefaultMutableTreeNode sbNode = new DefaultMutableTreeNode(Language.text("sketchbook.tree"));
    try {
      base.addSketches(sbNode, Base.getSketchbookFolder());
    } catch (IOException e) {
      e.printStackTrace();
    }
    return sbNode;
  }

  protected JFrame sketchbookFrame;

  public void showSketchbookFrame() {
    if (sketchbookFrame == null) {
      sketchbookFrame = new JFrame(Language.text("sketchbook"));
      Toolkit.setIcon(sketchbookFrame);
      Toolkit.registerWindowCloseKeys(sketchbookFrame.getRootPane(),
                                      new ActionListener() {
                                        public void actionPerformed(ActionEvent e) {
                                          sketchbookFrame.setVisible(false);
                                        }
                                      });

      final JTree tree = new JTree(buildSketchbookTree());
      tree.getSelectionModel()
        .setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
      tree.setShowsRootHandles(true);
      tree.expandRow(0);
      tree.setRootVisible(false);

      tree.addMouseListener(new MouseAdapter() {
        public void mouseClicked(MouseEvent e) {
          if (e.getClickCount() == 2) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree
              .getLastSelectedPathComponent();

            int selRow = tree.getRowForLocation(e.getX(), e.getY());
            //TreePath selPath = tree.getPathForLocation(e.getX(), e.getY());
            //if (node != null && node.isLeaf() && node.getPath().equals(selPath)) {
            if (node != null && node.isLeaf() && selRow != -1) {
              SketchReference sketch = (SketchReference) node.getUserObject();
              base.handleOpen(sketch.getPath());
            }
          }
        }
      });

      tree.addKeyListener(new KeyAdapter() {
        public void keyPressed(KeyEvent e) {
          if (e.getKeyCode() == KeyEvent.VK_ESCAPE) { // doesn't fire keyTyped()
            sketchbookFrame.setVisible(false);
          }
        }

        public void keyTyped(KeyEvent e) {
          if (e.getKeyChar() == KeyEvent.VK_ENTER) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree
              .getLastSelectedPathComponent();
            if (node != null && node.isLeaf()) {
              SketchReference sketch = (SketchReference) node.getUserObject();
              base.handleOpen(sketch.getPath());
            }
          }
        }
      });

      tree.setBorder(new EmptyBorder(5, 5, 5, 5));
      if (Base.isMacOS()) {
        tree.setToggleClickCount(2);
      } else {
        tree.setToggleClickCount(1);
      }
      JScrollPane treePane = new JScrollPane(tree);
      treePane.setPreferredSize(new Dimension(250, 450));
      treePane.setBorder(new EmptyBorder(0, 0, 0, 0));
      sketchbookFrame.getContentPane().add(treePane);
      sketchbookFrame.pack();
    }

    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        // Space for the editor plus a li'l gap
        int roughWidth = sketchbookFrame.getWidth() + 20;
        Point p = null;
        // If no window open, or the editor is at the edge of the screen
        if (base.activeEditor == null
          || (p = base.activeEditor.getLocation()).x < roughWidth) {
          // Center the window on the screen
          sketchbookFrame.setLocationRelativeTo(null);
        } else {
          // Open the window relative to the editor
          sketchbookFrame.setLocation(p.x - roughWidth, p.y);
        }
        sketchbookFrame.setVisible(true);
      }
    });
  }


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


  public Image getGradient(String attribute, int wide, int high) {
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
        Base.removeDir(targetFolder);
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

  @Override
  public String toString() {
    return getTitle();
  }
}
