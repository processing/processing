package processing.app;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.tree.*;

import processing.app.syntax.*;
//import processing.app.tools.Tool;

public abstract class Mode {
  protected Base base;
  
  protected File folder;

  protected HashMap<String, String> keywordToReference;
  
  protected PdeKeywords tokenMarker;
  protected Settings theme;
//  protected Formatter formatter;
//  protected Tool formatter;
  
  // maps imported packages to their library folder
  protected HashMap<String, Library> importToLibraryTable;

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
   * Create a new editor associated with this mode. 
   */
  abstract public Editor createEditor(Base base, String path, int[] location);
  

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
//    System.out.println("rebuildLibraryList()");

    // reset the table mapping imports to libraries
    importToLibraryTable = new HashMap<String, Library>();

    coreLibraries = Library.list(librariesFolder);
    contribLibraries = Library.list(base.getSketchbookLibrariesFolder());
    
    for (Library lib : coreLibraries) {
      lib.addPackageList(importToLibraryTable);
    }
    for (Library lib : contribLibraries) {
      lib.addPackageList(importToLibraryTable);
    }
  }
  
  
  public Library getLibrary(String name) {
    return importToLibraryTable.get(name);
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


  protected void rebuildToolbarMenu() {  //JMenu menu) {
    JMenuItem item;
    if (toolbarMenu == null) {
      toolbarMenu = new JMenu();
    } else {
      toolbarMenu.removeAll();
    }

    //System.out.println("rebuilding toolbar menu");
    // Add the single "Open" item
    item = Base.newJMenuItem("Open...", 'O');
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          base.handleOpenPrompt();
        }
      });
    toolbarMenu.add(item);
    
//    JMenu examplesMenu = new JMenu("Examples");
//    rebuildExamplesMenu(examplesMenu, true);
    item = new JMenuItem("Examples...");
    item.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        showExamplesFrame();
      }
    });
    toolbarMenu.add(item);
//    toolbarMenu.add(examplesMenu);
    
    toolbarMenu.addSeparator();

    // Add a list of all sketches and subfolders
    try {
      base.addSketches(toolbarMenu, base.getSketchbookFolder(), true);
//      boolean sketches = base.addSketches(toolbarMenu, base.getSketchbookFolder(), true);
//      if (sketches) {
//        toolbarMenu.addSeparator();
//      }
    } catch (IOException e) {
      e.printStackTrace();
    }

    //System.out.println("rebuilding examples menu");
    // Add each of the subfolders of examples directly to the menu
//    try {
//      base.addSketches(toolbarMenu, examplesFolder, true);
//    } catch (IOException e) {
//      e.printStackTrace();
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
      final JTree tree = buildExamplesTree();

      tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
      tree.setShowsRootHandles(true);
      tree.setToggleClickCount(2);
      // expand the root
      tree.expandRow(0);
      // now hide the root
      tree.setRootVisible(false);
      // now expand the other folks
      for (int row = tree.getRowCount()-1; row >= 0; --row) {
        tree.expandRow(row);
      }

      /*
      tree.addTreeSelectionListener(new TreeSelectionListener() {
        public void valueChanged(TreeSelectionEvent e) {
          DefaultMutableTreeNode node = (DefaultMutableTreeNode)
          tree.getLastSelectedPathComponent();

          if (node != null) {
            Object nodeInfo = node.getUserObject();
            if (node.isLeaf()) {
              System.out.println(node + " user obj: " + nodeInfo);
              //            BookInfo book = (BookInfo)nodeInfo;
              //            displayURL(book.bookURL);
            }
          }
        }
      });
      */
      
      /*
       *  MouseListener ml = new MouseAdapter() {
       *     public void <b>mousePressed</b>(MouseEvent e) {
       *         int selRow = tree.getRowForLocation(e.getX(), e.getY());
       *         TreePath selPath = tree.getPathForLocation(e.getX(), e.getY());
       *         if(selRow != -1) {
       *             if(e.getClickCount() == 1) {
       *                 mySingleClick(selRow, selPath);
       *             }
       *             else if(e.getClickCount() == 2) {
       *                 myDoubleClick(selRow, selPath);
       *             }
       *         }
       *     }
       * };
       * tree.addMouseListener(ml);
       */
      tree.addMouseListener(new MouseAdapter() {
        public void mouseClicked(MouseEvent e) {
          if (e.getClickCount() == 2) {
            DefaultMutableTreeNode node = 
              (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
            if (node != null && node.isLeaf()) {
              SketchReference sketch = (SketchReference) node.getUserObject();
              base.handleOpen(sketch.getPath());
            } else {
              int selRow = tree.getRowForLocation(e.getX(), e.getY());
              TreePath selPath = tree.getPathForLocation(e.getX(), e.getY());
              if (selRow != -1) {
                if (tree.isExpanded(selRow)) {
                  tree.collapsePath(selPath);
                } else {
                  tree.expandPath(selPath);
                }
              }
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
      
      tree.setBorder(new EmptyBorder(5, 5, 5, 5));
      JScrollPane treePane = new JScrollPane(tree);
      treePane.setPreferredSize(new Dimension(250, 450));
      treePane.setBorder(new EmptyBorder(0, 0, 0, 0));
      examplesFrame.getContentPane().add(treePane);
      examplesFrame.pack();
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


//  public void handleActivated(Editor editor) {
//    //// re-add the sub-menus that are shared by all windows
//    fileMenu.insert(Base.sketchbookMenu, 2);
//    fileMenu.insert(mode.examplesMenu, 3);
//    sketchMenu.insert(mode.importMenu, 4);
//  }


//  public void handleDeactivated(Editor editor) {
//    fileMenu.remove(Base.sketchbookMenu);
//    fileMenu.remove(examplesMenu);
//    sketchMenu.remove(importMenu);
//  }

  
//  abstract public void internalCloseRunner(Editor editor);  

  
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
    return theme.getStyle(attribute);
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