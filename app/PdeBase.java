/* -*- mode: jde; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  PdeBase - base class for the main processing application
  Part of the Processing project - http://Proce55ing.net

  Copyright (c) 2001-03 
  Ben Fry, Massachusetts Institute of Technology and 
  Casey Reas, Interaction Design Institute Ivrea

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

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.zip.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.undo.*;

#ifdef MACOS
import com.apple.mrj.*;
#endif


/**
 * Primary role of this class is for platform identification and
 * general interaction with the system (launching URLs, loading 
 * files and images, etc) that comes from that.
 */
#ifndef SWINGSUCKS
public class PdeBase extends JFrame
#else
public class PdeBase extends Frame
#endif
  implements ActionListener
#ifdef MACOS
             , MRJAboutHandler
             , MRJQuitHandler
             , MRJPrefsHandler
#endif
{
  static final String VERSION = "0068 Alpha";

  //static Properties properties;
  static Properties keywords; // keyword -> reference html lookup

  //static Frame frame;  // now 'this'
  static String encoding;
  static Image icon;

  protected UndoAction undoAction;
  protected RedoAction redoAction;
  static public UndoManager undo = new UndoManager(); // editor needs this guy

  // indicator that this is the first time this feller has used p5
  static boolean firstTime;

  boolean errorState;
  PdeEditor editor;

  //WindowAdapter windowListener;

  Menu sketchbookMenu;
  File sketchbookFolder;
  String sketchbookPath;

  boolean recordingHistory;
  Menu historyMenu;
  ActionListener historyMenuListener = 
    new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          editor.retrieveHistory(e.getActionCommand());
        }
      };

  //Menu serialMenu;
  MenuItem undoItem, redoItem;
  MenuItem saveMenuItem;
  MenuItem saveAsMenuItem;
  MenuItem beautifyMenuItem;
  //CheckboxMenuItem externalEditorItem;

  //Menu renderMenu;
  //CheckboxMenuItem normalItem, openglItem;
  //MenuItem illustratorItem;


  static final String WINDOW_TITLE = "Processing";

  // the platforms
  static final int WINDOWS = 1;
  static final int MACOS9  = 2;
  static final int MACOSX  = 3;
  static final int LINUX   = 4;
  static final int IRIX    = 5;
  static int platform;

  static final String platforms[] = {
    "", "windows", "macos9", "macosx", "linux", "irix"
  };


  static public void main(String args[]) {
    //System.getProperties().list(System.out);
    PdeBase app = new PdeBase();
  }


  // hack for #@#)$(* macosx
  public Dimension getMinimumSize() {
    return new Dimension(500, 500);
  }


  public PdeBase() {
    super(WINDOW_TITLE);


    // figure out which operating system

    if (System.getProperty("mrj.version") != null) {  // running on a mac
      platform = (System.getProperty("os.name").equals("Mac OS X")) ?
        MACOSX : MACOS9;

    } else {
      String osname = System.getProperty("os.name");

      if (osname.indexOf("Windows") != -1) {
        platform = WINDOWS;

      } else if (osname.equals("Linux")) {  // true for the ibm vm
        platform = LINUX;

      } else if (osname.equals("Irix")) {
        platform = IRIX;

      } else {
        platform = WINDOWS;  // probably safest
        System.out.println("unhandled osname: \"" + osname + "\"");
      }
    }


    // set the look and feel before opening the window

    try {
      if (platform == LINUX) {
        // linux is by default (motif?) even uglier than metal
        // actually, i'm using native menus, so they're ugly and
        // motif-looking. ick. need to fix this.
        UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
      } else {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
      }
    } catch (Exception e) { 
      e.printStackTrace();
    }


    // set the window icon

    try {
      icon = Toolkit.getDefaultToolkit().getImage("lib/icon.gif");
      this.setIconImage(icon);
    } catch (Exception e) { } // fail silently, no big whup


    // add listener to handle window close box hit event

    addWindowListener(new WindowAdapter() {
        public void windowClosing(WindowEvent e) {
          handleQuit();
        }
      });
    //frame.addWindowListener(windowListener);
    //this.addWindowListener(windowListener);


    // load in preferences (last sketch used, window placement, etc)

    prefs = new PdePreferences();


    // read in the keywords for the reference

    final String KEYWORDS = "pde_keywords.properties";
    keywords = new Properties();
    try {
      if ((PdeBase.platform == PdeBase.MACOSX) || 
          (PdeBase.platform == PdeBase.MACOS9)) {
        // macos doesn't seem to think that files in the lib folder
        // are part of the resources, unlike windows or linux.
        // actually, this is only the case when running as a .app, 
        // since it works fine from run.sh, but not Processing.app
        keywords.load(new FileInputStream("lib/" + KEYWORDS));

      } else {  // other, more reasonable operating systems
        keywords.load(getClass().getResource(KEYWORDS).openStream());
      }

    } catch (Exception e) {
      String message = 
        "An error occurred while loading the keywords,\n" + 
        "\"Find in reference\" will not be available.";
      JOptionPane.showMessageDialog(this, message, 
                                    "Problem loading keywords",
                                    JOptionPane.WARNING_MESSAGE);

      System.err.println(e.toString());
      e.printStackTrace();
    }


    // build the editor object

    editor = new PdeEditor(this);
    getContentPane().setLayout(new BorderLayout());
    getContentPane().add("Center", editor);


    // setup menu bar

    MenuBar menubar = new MenuBar();
    Menu menu;
    MenuItem item;


    // file menu

    menu = new Menu("File");
    menu.add(new MenuItem("New", new MenuShortcut('N')));
    sketchbookMenu = new Menu("Open");
    menu.add(sketchbookMenu);
    saveMenuItem = new MenuItem("Save", new MenuShortcut('S'));
    saveAsMenuItem = new MenuItem("Save as...", new MenuShortcut('S', true));
    menu.add(saveMenuItem);
    menu.add(saveAsMenuItem);
    menu.add(new MenuItem("Rename..."));
    menu.addSeparator();

    menu.add(new MenuItem("Export to Web", new MenuShortcut('E')));
    item = new MenuItem("Export Application", new MenuShortcut('E', true));
    item.setEnabled(false);
    menu.add(item);

    if (platform != MACOSX) {
      menu.add(new MenuItem("Preferences"));
      menu.addSeparator();
      menu.add(new MenuItem("Quit", new MenuShortcut('Q')));

    } else {
#ifdef MACOS
      // #@$*(@#$ apple.. always gotta think different
      MRJApplicationUtils.registerAboutHandler(this);
      MRJApplicationUtils.registerPrefsHandler(this);
      MRJApplicationUtils.registerQuitHandler(this);
#endif
    }
    menu.addActionListener(this);
    menubar.add(menu);


    // edit menu

    menu = new Menu("Edit");
    
    undoItem = new MenuItem("Undo", new MenuShortcut('Z'));
    undoItem.addActionListener(undoAction = new UndoAction());
    menu.add(undoItem);

    redoItem = new MenuItem("Redo", new MenuShortcut('Y'));
    redoItem.addActionListener(redoAction = new RedoAction());
    menu.add(redoItem);

    menu.addSeparator();

    // "cut" and "copy" should really only be enabled if some text 
    // is currently selected
    item = new MenuItem("Cut", new MenuShortcut('X'));
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          editor.textarea.cut();
        }
      });
    menu.add(item);

    item = new MenuItem("Copy", new MenuShortcut('C'));
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          editor.textarea.copy();
        }
      });
    menu.add(item);

    item = new MenuItem("Paste", new MenuShortcut('V'));
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          editor.textarea.paste();
        }
      });
    menu.add(item);

    item = new MenuItem("Select All", new MenuShortcut('A'));
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          editor.textarea.selectAll();
        }
      });
    menu.add(item);

    beautifyMenuItem = new MenuItem("Beautify", new MenuShortcut('B'));
    beautifyMenuItem.addActionListener(this);
    menu.add(beautifyMenuItem);

    menu.addSeparator();

    item = new MenuItem("Find...", new MenuShortcut('F'));
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          editor.find();
        }
      });
    menu.add(item);

    item = new MenuItem("Find Next", new MenuShortcut('G'));
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          editor.findNext();
        }
      });
    menu.add(item);

    item = new MenuItem("Find in Reference", new MenuShortcut('F', true));
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) { 
          if (editor.textarea.isSelectionActive()) {
            String text = editor.textarea.getSelectedText();
            if (text.length() == 0) {
              editor.message("First select a word to find in the reference.");

            } else {
              String referenceFile = (String) keywords.get(text);
              if (referenceFile == null) {
                editor.message("No reference available for \"" + text + "\"");
              } else {
                showReference(referenceFile);
              }
            }
          }
        }
      });
    menu.add(item);

    menubar.add(menu);

    Document document = editor.textarea.getDocument();
    document.addUndoableEditListener(new MyUndoableEditListener());


    // sketch menu

    menu = new Menu("Sketch");
    menu.add(new MenuItem("Run", new MenuShortcut('R')));
    menu.add(new MenuItem("Present", new MenuShortcut('R', true)));
    menu.add(new MenuItem("Stop", new MenuShortcut('T')));
    menu.addSeparator();

    menu.add(new MenuItem("Add file..."));
    menu.add(new MenuItem("Create font..."));

    if ((platform == WINDOWS) || (platform == MACOSX)) {
      // no way to do an 'open in file browser' on other platforms
      // since there isn't any sort of standard
      menu.add(new MenuItem("Show sketch folder"));
    }

    recordingHistory = PdePreferences.getBoolean("history.recording", true);
    if (recordingHistory) {
      historyMenu = new Menu("History");
      menu.add(historyMenu);
    }

    //menu.addSeparator();

    //menu.addSeparator();
    //serialMenu = new Menu("Serial Port");
    //menu.add(serialMenu);

    /*
    Menu rendererMenu = new Menu("Renderer");
#ifdef OPENGL
    // opengl support has started, but remains as yet unfinished
    menu.add(rendererMenu);
#endif

    normalItem = new CheckboxMenuItem("Normal");
    rendererMenu.add(normalItem);
    normalItem.setState(true);
    normalItem.addItemListener(new ItemListener() {
        public void itemStateChanged(ItemEvent e) {
          openglItem.setState(false);
          normalItem.setState(true);
        }
      });

    openglItem = new CheckboxMenuItem("OpenGL");
    rendererMenu.add(openglItem);
    openglItem.addItemListener(new ItemListener() {
        public void itemStateChanged(ItemEvent e) {
          openglItem.setState(true);
          normalItem.setState(false);
        }
      });
    */

    /*
    externalEditorItem = new CheckboxMenuItem("Use External Editor");
    externalEditorItem.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        //System.out.println(e);
        if (e.getStateChange() == ItemEvent.SELECTED) {
          editor.setExternalEditor(true);
        } else {
          editor.setExternalEditor(false);
        }
      }
    });
    menu.add(externalEditorItem);
    */

    menu.addActionListener(this);
    menubar.add(menu);  // add the sketch menu


    // help menu

    menu = new Menu("Help");
    menu.add(new MenuItem("Help"));
    menu.add(new MenuItem("Reference"));
    menu.add(new MenuItem("Proce55ing.net", new MenuShortcut('5')));

    // macosx already has its own about menu
    if (platform != MACOSX) {
      menu.addSeparator();
      menu.add(new MenuItem("About Processing"));
    }
    menu.addActionListener(this);
    menubar.setHelpMenu(menu);


    // set all menus

    this.setMenuBar(menubar);


    // handle layout

    //Insets insets = frame.getInsets();
    //Toolkit tk = Toolkit.getDefaultToolkit();
    //Dimension screen = tk.getScreenSize();

    this.pack();  // maybe this should be before the setBounds call


    // figure out window placement

    Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
    boolean windowPositionInvalid = false;

    if (PdePreferences.get("last.screen.height") != null) {
      // if screen size has changed, the window coordinates no longer
      // make sense, so don't use them unless they're identical
      int screenW = getInteger("last.screen.width");
      int screenH = getInteger("last.screen.height");

      if ((screen.width != screenW) || (screen.height != screenH)) {
        windowPositionInvalid = true;
      }
    } else {
      windowPositionInvalid = true;
    }

    if (windowPositionInvalid) {
      int windowH = PdePreferences.getInteger("default.window.height");
      int windowW = PdePreferences.getInteger("default.window.width");
      setBounds((screen.width - windowW) / 2, 
                (screen.height - windowH) / 2,
                windowW, windowH);
      // this will be invalid as well, so grab the new value
      PdePreferences.setInteger("last.divider.location", 
                                splitPane.getDividerLocation());
    } else {
      setBounds(PdePreferences.getInteger("last.window.x"), 
                PdePreferences.getInteger("last.window.y"), 
                PdePreferences.getInteger("last.window.width"), 
                PdePreferences.getInteger("last.window.height"));
    }

    // now that everything is set up, open last-used sketch, etc.
    editor.init();

    rebuildSketchbookMenu(sketchbookMenu);
    //buildSerialMenu();
    this.show();  // added back in for pde
  }


  //This one listens for edits that can be undone.
  protected class MyUndoableEditListener implements UndoableEditListener {
    public void undoableEditHappened(UndoableEditEvent e) {
      //Remember the edit and update the menus.
      undo.addEdit(e.getEdit());
      undoAction.updateUndoState();
      redoAction.updateRedoState();
      //System.out.println("setting sketch to modified");
      //if (!editor.sketchModified) editor.setSketchModified(true);
    }
  }


  class UndoAction extends AbstractAction {
    public UndoAction() {
      super("Undo");
      this.setEnabled(false);
    }

    public void actionPerformed(ActionEvent e) {
      try {
        undo.undo();
      } catch (CannotUndoException ex) {
        //System.out.println("Unable to undo: " + ex);
        //ex.printStackTrace();
      }
      updateUndoState();
      redoAction.updateRedoState();
    }

    protected void updateUndoState() {
      if (undo.canUndo()) {
        this.setEnabled(true);
        undoItem.setEnabled(true);
        putValue(Action.NAME, undo.getUndoPresentationName());
      } else {
        this.setEnabled(false);
        undoItem.setEnabled(false);
        putValue(Action.NAME, "Undo");
      }
    }      
  }    


  class RedoAction extends AbstractAction {
    public RedoAction() {
      super("Redo");
      this.setEnabled(false);
    }

    public void actionPerformed(ActionEvent e) {
      try {
        undo.redo();
      } catch (CannotRedoException ex) {
        //System.out.println("Unable to redo: " + ex);
        //ex.printStackTrace();
      }
      updateRedoState();
      undoAction.updateUndoState();
    }

    protected void updateRedoState() {
      if (undo.canRedo()) {
        this.setEnabled(true);
        redoItem.setEnabled(true);
        putValue(Action.NAME, undo.getRedoPresentationName());
      } else {
        this.setEnabled(false);
        redoItem.setEnabled(false);
        putValue(Action.NAME, "Redo");
      }
    }
  }    


  // listener for sketchbk items uses getParent() to figure out
  // the directories above it

  class SketchbookMenuListener implements ActionListener {
    String path;

    public SketchbookMenuListener(String path) {
      this.path = path;
    }

    public void actionPerformed(ActionEvent e) {
      String name = e.getActionCommand();
      editor.skOpen(path + File.separator + name, name);
    }
  }

  public void rebuildSketchbookMenu() {
    rebuildSketchbookMenu(sketchbookMenu);
  }

  public void rebuildSketchbookMenu(Menu menu) {
    menu.removeAll();

    try {
      //MenuItem newSketchItem = new MenuItem("New Sketch");
      //newSketchItem.addActionListener(this);
      //menu.add(newSkechItem);
      //menu.addSeparator();

      sketchbookFolder = 
        new File(PdePreferences.get("sketchbook.path", "sketchbook"));
      sketchbookPath = sketchbookFolder.getCanonicalPath();
      if (!sketchbookFolder.exists()) {
        System.err.println("sketchbook folder doesn't exist, " + 
                           "making a new one");
        sketchbookFolder.mkdirs();
      }

      // files for the current user (for now, most likely 'default')

      // header knows what the current user is
      String userPath = sketchbookPath + 
        File.separator + editor.userName;

      File userFolder = new File(userPath);
      if (!userFolder.exists()) {
        System.err.println("sketchbook folder for '" + editor.userName + 
                           "' doesn't exist, creating a new one");
        userFolder.mkdirs();
      }

      /*
      SketchbookMenuListener userMenuListener = 
        new SketchbookMenuListener(userPath);

      String entries[] = new File(userPath).list();
      boolean added = false;
      for (int j = 0; j < entries.length; j++) {
        if (entries[j].equals(".") || 
            entries[j].equals("..") ||
            entries[j].equals("CVS")) continue;
        //entries[j].equals(".cvsignore")) continue;
        added = true;
        if (new File(userPath, entries[j] + File.separator + 
                     entries[j] + ".pde").exists()) {
          MenuItem item = new MenuItem(entries[j]);
          item.addActionListener(userMenuListener);
          menu.add(item);
        }
        //submenu.add(entries[j]);
      }
      if (!added) {
        MenuItem item = new MenuItem("No sketches");
        item.setEnabled(false);
        menu.add(item);
      }
      menu.addSeparator();
      */
      if (addSketches(menu, userFolder, false)) {
        menu.addSeparator();
      }
      if (!addSketches(menu, sketchbookFolder, true)) {
        MenuItem item = new MenuItem("No sketches");
        item.setEnabled(false);
        menu.add(item);
      }

      /*
      // doesn't seem that refresh is worthy of its own menu item
      // people can stop and restart p5 if they want to muck with it
      menu.addSeparator();
      MenuItem item = new MenuItem("Refresh");
      item.addActionListener(this);
      menu.add(item);
      */

    } catch (IOException e) {
      e.printStackTrace();
    }
  }


  protected boolean addSketches(Menu menu, File folder, 
                                /*boolean allowUser,*/ boolean root) 
    throws IOException {
    // skip .DS_Store files, etc
    if (!folder.isDirectory()) return false;

    String list[] = folder.list();
    SketchbookMenuListener listener = 
      new SketchbookMenuListener(folder.getCanonicalPath());

    boolean ifound = false;

    for (int i = 0; i < list.length; i++) {
      if (list[i].equals(editor.userName) && root) continue;

      if (list[i].equals(".") ||
          list[i].equals("..") ||
          list[i].equals("CVS")) continue;

      File subfolder = new File(folder, list[i]);
      if (new File(subfolder, list[i] + ".pde").exists()) {
        MenuItem item = new MenuItem(list[i]);
        item.addActionListener(listener);
        menu.add(item);
        ifound = true;

      } else {  // might contain other dirs, get recursive
        Menu submenu = new Menu(list[i]);
        // needs to be separate var 
        // otherwise would set ifound to false
        boolean found = addSketches(submenu, subfolder, false);
        if (found) {
          menu.add(submenu);
          ifound = true;
        }
      }
    }
    return ifound;
  }


  /*
  class HistoryMenuListener implements ActionListener {
    public void actionPerformed(ActionEvent e) {
      editor.selectHistory(e.getActionCommand);
    }
  }
  */

  public void rebuildHistoryMenu(String path) {
    rebuildHistoryMenu(historyMenu, path);
  }

  public void rebuildHistoryMenu(Menu menu, String path) {
    if (!recordingHistory) return;

    menu.removeAll();

    File hfile = new File(path);
    if (!hfile.exists()) return;  // no history yet

    MenuItem item = new MenuItem("Clear History");
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          if (!editor.historyFile.delete()) {
            System.err.println("couldn't erase history");
          }
          rebuildHistoryMenu(historyMenu, editor.historyFile.getPath());
        }
      });
    menu.add(item);
    menu.addSeparator();

    try {
      BufferedReader reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(path))));
      String line = null;

      int historyCount = 0;
      String historyList[] = new String[100];

      try {
        while ((line = reader.readLine()) != null) {
        //while (line = reader.readLine()) {
        //while (true) { line = reader.readLine();
          //if (line == null) continue;
          //System.out.println("line: " + line);
          if (line.equals(PdeEditor.HISTORY_SEPARATOR)) {
            // next line is the good stuff
            line = reader.readLine();
            int version = 
              Integer.parseInt(line.substring(0, line.indexOf(' ')));
            if (version == 1) {
              String whysub = line.substring(2);  // after "1 "
              String why = whysub.substring(0, whysub.indexOf(" -"));
              //System.out.println("'" + why + "'");

              String readable = line.substring(line.lastIndexOf("-") + 2);
              if (historyList.length == historyCount) {
                String temp[] = new String[historyCount*2];
                System.arraycopy(historyList, 0, temp, 0, historyCount);
                historyList = temp;
              }
              historyList[historyCount++] = why + " - " + readable;

            } // otherwise don't know what to do
          }
        }
        //System.out.println(line);
      } catch (IOException e) {
        e.printStackTrace();
      }

      // add the items to the menu in reverse order
      /*
      ActionListener historyMenuListener = 
        new ActionListener() {
            public void actionPerformed(ActionEvent e) {
              editor.retrieveHistory(e.getActionCommand());
            }
          };
      */

      for (int i = historyCount-1; i >= 0; --i) {
        MenuItem mi = new MenuItem(historyList[i]);
        mi.addActionListener(historyMenuListener);
        menu.add(mi);
      }

      reader.close();

    } catch (IOException e) {
      e.printStackTrace();
    }
  }


  // interfaces for MRJ Handlers, but naming is fine 
  // so used internally for everything else

  public void handleAbout() {
    //System.out.println("the about box will now be shown");
    final Image image = getImage("about.jpg", this);
    int w = image.getWidth(this);
    int h = image.getHeight(this);
    final Window window = new Window(this) {
        public void paint(Graphics g) {
          g.drawImage(image, 0, 0, null);

          /*
            // does nothing..
          Graphics2D g2 = (Graphics2D) g;
          g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
                              RenderingHints.VALUE_ANTIALIAS_OFF);
          */

          g.setFont(new Font("SansSerif", Font.PLAIN, 11));
          g.setColor(Color.white);
          g.drawString(VERSION, 50, 30);
        }
      };
    window.addMouseListener(new MouseAdapter() {
        public void mousePressed(MouseEvent e) {
          window.dispose();
        }
      });
    Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
    window.setBounds((screen.width-w)/2, (screen.height-h)/2, w, h);
    window.show();
  }

  public void handlePrefs() {
    new PdePreferences();
    /*
    JOptionPane.showMessageDialog(this, //frame,
                                  "Preferences are in the 'lib' folder\n" +
                                  "inside text files named pde.properties\n" +
                                  "and pde_" + platforms[platform] + 
                                  ".properties",
                                  "Preferences",
                                  JOptionPane.INFORMATION_MESSAGE);
    */
    //System.out.println("now showing preferences");
  }

  public void handleQuit() {
    editor.doQuit();
  }


  public void actionPerformed(ActionEvent event) {
    String command = event.getActionCommand();
    //System.out.println(command);

    if (command.equals("New")) {
      editor.skNew();
      //editor.initiate(Editor.NEW);

    } else if (command.equals("Save")) {
      editor.doSave();

    } else if (command.equals("Save as...")) {
      editor.skSaveAs(false);

    } else if (command.equals("Rename...")) {
      editor.skSaveAs(true);

    } else if (command.equals("Export to Web")) {
      editor.skExport();

    } else if (command.equals("Preferences")) {
      handlePrefs();

    } else if (command.equals("Quit")) {
      handleQuit();

    } else if (command.equals("Run")) {
      editor.doRun(false);

    } else if (command.equals("Present")) {
      editor.doRun(true);

    } else if (command.equals("Stop")) {    
      if (editor.presenting) {
        editor.doClose();
      } else {
        editor.doStop();
      }
    } else if (command.equals("Beautify")) {
      editor.doBeautify();

    } else if (command.equals("Add file...")) {
      editor.addFile();

    } else if (command.equals("Create font...")) {
      new PdeFontBuilder(new File(editor.sketchDir, "data"));

    } else if (command.equals("Show sketch folder")) {
      openFolder(editor.sketchDir);

    } else if (command.equals("Help")) {
      openURL(System.getProperty("user.dir") + 
              File.separator + "reference" + 
              File.separator + "environment" +
              File.separator + "index.html");

    } else if (command.equals("Proce55ing.net")) {
      openURL("http://Proce55ing.net/");

    } else if (command.equals("Reference")) {
      openURL(System.getProperty("user.dir") + File.separator + 
              "reference" + File.separator + "index.html");

    } else if (command.equals("About Processing")) {
      handleAbout();
    }
  }


  static public void showReference(String referenceFile) {
    String currentDir = System.getProperty("user.dir");
    openURL(currentDir + File.separator + 
            "reference" + File.separator + 
            referenceFile + ".html");
  }


  /**
   * Implements the cross-platform headache of opening URLs
   */
  static public void openURL(String url) { 
    //System.out.println("opening url " + url);
    try {
      if (platform == WINDOWS) {
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

#ifdef MACOS
      } else if (platform == MACOSX) {
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
        //System.out.println("trying to open " + url);
        com.apple.mrj.MRJFileUtils.openURL(url);

      } else if (platform == MACOS9) {
        com.apple.mrj.MRJFileUtils.openURL(url);
#endif

      } else if (platform == LINUX) {
        //String currentDir = System.getProperty("user.dir");
        //Runtime.getRuntime().exec("mozilla "+ currentDir + 
        //                          "/reference/index.html");
        // another wild ass guess

        // probably need to replace spaces or use quotes here
        Runtime.getRuntime().exec("mozilla " + url);

      } else {
        System.err.println("unspecified platform");
      }

    } catch (IOException e) {
      e.printStackTrace();
    }
  }


  /** 
   * Implements the other cross-platform headache of opening
   * a folder in the machine's native file browser.
   */
  static public void openFolder(File file) {
    try {
      String folder = file.getCanonicalPath();

      if (platform == WINDOWS) {
        // doesn't work
        //Runtime.getRuntime().exec("cmd /c \"" + folder + "\"");

        // works fine on winxp, prolly win2k as well
        Runtime.getRuntime().exec("explorer \"" + folder + "\"");

        // not tested
        //Runtime.getRuntime().exec("start explorer \"" + folder + "\"");

#ifdef MACOS
      } else if (platform == MACOSX) {
        openURL(folder);  // handles char replacement, etc

#endif
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  //

  // could also do showMessage with JOptionPane.INFORMATION_MESSAGE

  //

  static public void showWarning(String title, String message, 
                                 Exception e) {
    if (title == null) title = "Warning";
    JOptionPane.showMessageDialog(this, message, title,
                                  JOptionPane.WARNING_MESSAGE);

    //System.err.println(e.toString());
    if (e != null) e.printStackTrace();
  }

  //

  static public void showError(String title, String message, 
                               Exception e) {
    if (title == null) title = "Error";
    JOptionPane.showMessageDialog(this, message, title,
                                  JOptionPane.ERROR_MESSAGE);

    if (e != null) e.printStackTrace();
  }

  //

  // used by PdeEditorButtons, but probably more later
  static public Image getImage(String name, Component who) {
    Image image = null;
    //if (isApplet()) {
    //image = applet.getImage(applet.getCodeBase(), name);
    //} else {
    Toolkit tk = Toolkit.getDefaultToolkit();

    if (PdeBase.platform == PdeBase.MACOSX) {
      //String pkg = "Proce55ing.app/Contents/Resources/Java/";
      //image = tk.getImage(pkg + name);
      image = tk.getImage("lib/" + name);
    } else if (PdeBase.platform == PdeBase.MACOS9) {
      image = tk.getImage("lib/" + name);
    } else {
      image = tk.getImage(who.getClass().getResource(name));
    }

    //image =  tk.getImage("lib/" + name);
    //URL url = PdeApplet.class.getResource(name);
    //image = tk.getImage(url);
    //}
    //MediaTracker tracker = new MediaTracker(applet);
    MediaTracker tracker = new MediaTracker(who); //frame);
    tracker.addImage(image, 0);
    try {
      tracker.waitForAll();
    } catch (InterruptedException e) { }      
    return image;
  }
}
