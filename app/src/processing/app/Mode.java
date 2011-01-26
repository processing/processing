package processing.app;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

import javax.swing.*;

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
  protected JMenu examplesMenu;
  protected JMenu importMenu;
  
  // popup menu used for the toolbar
  protected JMenu toolbarMenu;
  
  protected File examplesFolder;
  protected File librariesFolder;

  protected ArrayList<Library> coreLibraries;
  protected ArrayList<Library> contribLibraries;  

  
  public Mode(Base base, File folder) {
    this.base = base;
    this.folder = folder;
    
    // Get paths for the libraries and examples in the mode folder
    examplesFolder = new File(folder, "examples");
    librariesFolder = new File(folder, "libraries");
  }
  
  
  public File getContentFile(String path) {
    return new File(folder, path);
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


  public void rebuildLibraryList() {
    System.out.println("rebuildLibraryList()");

    // reset the table mapping imports to libraries
    importToLibraryTable = new HashMap<String, Library>();

    try {
      coreLibraries = Library.list(librariesFolder);
      contribLibraries = Library.list(base.getSketchbookLibrariesFolder());
      
      for (Library lib : coreLibraries) {
        lib.addPackageList(importToLibraryTable);
      }
      for (Library lib : contribLibraries) {
        lib.addPackageList(importToLibraryTable);
      }
      
    } catch (IOException e) {
      Base.showWarning("Unhappiness", 
                       "An error occurred while loading libraries.\n" +
                       "Not all the books will be in place.", e);
    }
  }
  
  
  public Library getLibrary(String name) {
    return importToLibraryTable.get(name);
  }

  
  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .
    

//  abstract public EditorToolbar createToolbar(Editor editor);
  
  
  public JMenu getToolbarMenu() {
    if (toolbarMenu == null) {
      toolbarMenu = new JMenu();
      rebuildToolbarMenu();
    }
    return toolbarMenu;
  }
  
  
  protected void rebuildToolbarMenu() {  //JMenu menu) {
    JMenuItem item;
    toolbarMenu.removeAll();

    //System.out.println("rebuilding toolbar menu");
    // Add the single "Open" item
    item = Base.newJMenuItem("Open...", 'O');
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          base.handleOpenPrompt();
        }
      });
    toolbarMenu.add(item);
    toolbarMenu.addSeparator();

    // Add a list of all sketches and subfolders
    try {
      boolean sketches = base.addSketches(toolbarMenu, base.getSketchbookFolder(), true);
      if (sketches) {
        toolbarMenu.addSeparator();
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

    //System.out.println("rebuilding examples menu");
    // Add each of the subfolders of examples directly to the menu
    try {
      base.addSketches(toolbarMenu, examplesFolder, true);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }


  public JMenu getImportMenu() {
    if (importMenu == null) {
      importMenu = new JMenu("Import Library...");
    }
    return importMenu;
  }


  public void rebuildImportMenu() {  //JMenu importMenu) {
    //System.out.println("rebuilding import menu");
    importMenu.removeAll();

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

    for (Library library : coreLibraries) {
      JMenuItem item = new JMenuItem(library.getName());
      item.addActionListener(listener);
      item.setActionCommand(library.getJarPath());
      importMenu.add(item);
    }

    if (contribLibraries.size() != 0) {
      importMenu.addSeparator();
      JMenuItem contrib = new JMenuItem("Contributed");
      contrib.setEnabled(false);

      for (Library library : contribLibraries) {
        JMenuItem item = new JMenuItem(library.getName());
        item.addActionListener(listener);
        item.setActionCommand(library.getJarPath());
        importMenu.add(item);
      }
    }
  }
  
  
  public JMenu getExamplesMenu() {
    if (examplesMenu == null) {
      examplesMenu = new JMenu("Examples");
      rebuildExamplesMenu(examplesMenu);
    }
    return examplesMenu;
  }


  public void rebuildExamplesMenu(JMenu menu) {
    try {
      menu.removeAll();
      //base.addSketches(menu, examplesFolder, false);
      
      // break down the examples folder for examples
      System.out.println("checking examples folder " + examplesFolder);
      File[] subfolders = examplesFolder.listFiles(new FilenameFilter() {
        public boolean accept(File dir, String name) {
          return dir.isDirectory() && name.charAt(0) != '.';
        }
      });
      for (File sub : subfolders) {
        Base.addDisabledItem(menu, sub.getName());
//        JMenuItem categoryItem = new JMenuItem(sub.getName());
//        categoryItem.setEnabled(false);
//        menu.add(categoryItem);
        base.addSketches(menu, sub, false);
      }

      // get library examples
//      JMenuItem coreItem = new JMenuItem("Core Libraries");
//      coreItem.setEnabled(false);
      Base.addDisabledItem(menu, "Libraries");
      for (Library lib : coreLibraries) {
        if (lib.hasExamples()) {
          JMenu libMenu = new JMenu(lib.getName());
          base.addSketches(libMenu, lib.getExamplesFolder(), false);
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
        Base.addDisabledItem(menu, "Contributed");
        for (Library lib : contribLibraries) {
          if (lib.hasExamples()) {
            JMenu libMenu = new JMenu(lib.getName());
            base.addSketches(libMenu, lib.getExamplesFolder(), false);
            menu.add(libMenu);
          }
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
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


  public String getReference(String keyword) {
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


//  public void handleNew() {
//    base.handleNew();    
//  }
//
//
//  public void handleNewReplace() {
//    base.handleNewReplace();
//  }
}