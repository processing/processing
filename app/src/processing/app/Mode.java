package processing.app;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.tree.*;

import processing.app.syntax.*;
import processing.core.PApplet;


public abstract class Mode {
  protected Base base;

  protected File folder;

  protected PdeKeywords tokenMarker = new PdeKeywords();
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

  public ArrayList<Library> coreLibraries;
  public ArrayList<Library> contribLibraries;

  /** Library folder for core. (Used for OpenGL in particular.) */
  protected Library coreLibrary;
  
  /** 
   * ClassLoader used to retrieve classes for this mode. Useful if you want
   * to grab any additional classes that subclass what's in the mode folder. 
   */
  protected ClassLoader classLoader;


//  public Mode(Base base, File folder) {
//    this(base, folder, base.getSketchbookLibrariesFolder());
//  }


  public Mode(Base base, File folder) {
    this.base = base;
    this.folder = folder;

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
    BufferedReader reader = PApplet.createReader(keywordFile);
    String line = null;
    while ((line = reader.readLine()) != null) {
      String[] pieces = PApplet.trim(PApplet.split(line, '\t'));
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
      theme = new Settings(new File(folder, "theme/theme.txt"));

      // other things that have to be set explicitly for the defaults
      theme.setColor("run.window.bgcolor", SystemColor.control);

    } catch (IOException e) {
      Base.showError("Problem loading theme.txt",
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
  abstract public Editor createEditor(Base base, String path, EditorState state);
  //abstract public Editor createEditor(Base base, String path, int[] location);


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


  public JMenu getImportMenu() {
    if (importMenu == null) {
      rebuildImportMenu();
    }
    return importMenu;
  }


  public void rebuildImportMenu() {  //JMenu importMenu) {
    if (importMenu == null) {
      importMenu = new JMenu("Import Library...");
    } else {
      //System.out.println("rebuilding import menu");
      importMenu.removeAll();
    }

    JMenuItem addLib = new JMenuItem("Add Library...");
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
      JMenuItem item = new JMenuItem(getTitle() + " mode has no core libraries");
      item.setEnabled(false);
      importMenu.add(item);
    } else {
      for (Library library : coreLibraries) {
        JMenuItem item = new JMenuItem(library.getName());
        item.addActionListener(listener);
        item.setActionCommand(library.getJarPath());
        importMenu.add(item);
      }
    }

    if (contribLibraries.size() != 0) {
      importMenu.addSeparator();
      JMenuItem contrib = new JMenuItem("Contributed");
      contrib.setEnabled(false);
      importMenu.add(contrib);

      HashMap<String, JMenu> subfolders = new HashMap<String, JMenu>();

      for (Library library : contribLibraries) {
        JMenuItem item = new JMenuItem(library.getName());
        item.addActionListener(listener);
        item.setActionCommand(library.getJarPath());

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


  public JTree buildExamplesTree() {
    DefaultMutableTreeNode node = new DefaultMutableTreeNode("Examples");

    JTree examplesTree = new JTree(node);
//    rebuildExamplesTree(node);
//  }

    //DefaultTreeCellRenderer renderer = tree.
//    TreeCellRenderer tcr = examplesTree.getCellRenderer();

    //
//
//  public void rebuildExamplesTree(DefaultMutableTreeNode node) {
    try {
      // break down the examples folder for examples
//      File[] subfolders = examplesFolder.listFiles(new FilenameFilter() {
//        public boolean accept(File dir, String name) {
//          return dir.isDirectory() && name.charAt(0) != '.';
//        }
//      });
      File[] subfolders = getExampleCategoryFolders();

//      DefaultMutableTreeNode examplesParent = new DefaultMutableTreeNode("Examples");
      for (File sub : subfolders) {
        DefaultMutableTreeNode subNode = new DefaultMutableTreeNode(sub.getName());
        if (base.addSketches(subNode, sub)) {
//          examplesParent.add(subNode);
          node.add(subNode);
        }
      }
//      node.add(examplesParent);
//      examplesTree.expandPath(new TreePath(examplesParent));

      // get library examples
      boolean any = false;
      DefaultMutableTreeNode libParent = new DefaultMutableTreeNode("Libraries");
      for (Library lib : coreLibraries) {
        if (lib.hasExamples()) {
          DefaultMutableTreeNode libNode = new DefaultMutableTreeNode(lib.getName());
          any |= base.addSketches(libNode, lib.getExamplesFolder());
          libParent.add(libNode);
        }
      }
      if (any) {
        node.add(libParent);
      }

      // get contrib library examples
      any = false;
      for (Library lib : contribLibraries) {
        if (lib.hasExamples()) {
          any = true;
        }
      }
      if (any) {
//        menu.addSeparator();
        DefaultMutableTreeNode contribParent = new DefaultMutableTreeNode("Contributed Libraries");
//        Base.addDisabledItem(menu, "Contributed");
        for (Library lib : contribLibraries) {
          if (lib.hasExamples()) {
//            JMenu libMenu = new JMenu(lib.getName());
            DefaultMutableTreeNode libNode = new DefaultMutableTreeNode(lib.getName());
//            base.addSketches(libMenu, lib.getExamplesFolder(), replace);
            base.addSketches(libNode, lib.getExamplesFolder());
//            menu.add(libMenu);
            contribParent.add(libNode);
          }
        }
        node.add(contribParent);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return examplesTree;
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


  public void showExamplesFrame() {
    if (examplesFrame == null) {
      examplesFrame = new JFrame(getTitle() + " Examples");
      Toolkit.registerWindowCloseKeys(examplesFrame.getRootPane(), new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          examplesFrame.setVisible(false);
        }
      });
      
      final JTree tree = buildExamplesTree();

      tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
      tree.setShowsRootHandles(true);
      tree.setToggleClickCount(2);
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

      tree.setBorder(new EmptyBorder(5, 5, 5, 5));
      tree.setToggleClickCount(1);
      JScrollPane treePane = new JScrollPane(tree);
      treePane.setPreferredSize(new Dimension(250, 450));
      treePane.setBorder(new EmptyBorder(0, 0, 0, 0));
      examplesFrame.getContentPane().add(treePane);
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


/**
   * Get an image object from the theme folder.
   */
  public Image loadImage(String filename) {
    File file = new File(folder, filename);
    return new ImageIcon(file.getAbsolutePath()).getImage();
  }


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


//  abstract public Formatter createFormatter();


//  public Formatter getFormatter() {
//    return formatter;
//  }


//  public Tool getFormatter() {
//    return formatter;
//  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  //public String get(String attribute) {
  //  return theme.get(attribute);
  //}


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
//    System.out.println("getFont(" + attribute + ") -> " + theme.getFont(attribute));
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
    boolean italic = (s.indexOf("italic") != -1);

    return new SyntaxStyle(color, italic, bold);
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
   * Returns a String[] array of proper extensions.
   */
  abstract public String[] getExtensions();


  /**
   * Get array of file/directory names that needn't be copied during "Save As".
   */
  abstract public String[] getIgnorable();


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


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
}