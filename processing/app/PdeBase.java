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

#ifndef RXTX
import javax.comm.*;
#else
import gnu.io.*;
#endif

#ifdef MACOS
import com.apple.mrj.*;
#endif


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
  static Properties properties;
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

  WindowAdapter windowListener;

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

  Menu serialMenu;
  MenuItem undoItem, redoItem;
  MenuItem saveMenuItem;
  MenuItem saveAsMenuItem;
  MenuItem beautifyMenuItem;
  CheckboxMenuItem externalEditorItem;

  //Menu renderMenu;
  CheckboxMenuItem normalItem, openglItem;
  //MenuItem illustratorItem;

  

  static final String WINDOW_TITLE = "Processing";

  // the platforms
  static final int WINDOWS = 1;
  static final int MACOS9  = 2;
  static final int MACOSX  = 3;
  static final int LINUX   = 4;
  static int platform;

  static final String platforms[] = {
    "", "windows", "macos9", "macosx", "linux"
  };


  static public void main(String args[]) {
    //System.getProperties().list(System.out);
    //System.out.println(System.getProperty("java.class.path"));

    // should be static though the mac is acting sketchy
    if (System.getProperty("mrj.version") != null) {  // running on a mac
      //System.out.println(UIManager.getSystemLookAndFeelClassName());
      //System.out.println(System.getProperty("mrj.version"));
      //System.out.println(System.getProperty("os.name"));
      platform = (System.getProperty("os.name").equals("Mac OS X")) ?
        MACOSX : MACOS9;
        
    } else {
      //System.out.println("unknown OS");
      //System.out.println(System.getProperty("os.name"));
      String osname = System.getProperty("os.name");
      //System.out.println("osname is " + osname);
      if (osname.indexOf("Windows") != -1) {
        platform = WINDOWS;

      } else if (osname.equals("Linux")) {  // true for the ibm vm
        platform = LINUX;

      } else {
        platform = WINDOWS;  // probably safest
        System.out.println("unhandled osname: " + osname);
      }
    }

    try {
      //if (platform == LINUX) {
        // linux is by default (motif?) even uglier than metal
        // actually, i'm using native menus, so they're ugly and
        // motif-looking. ick. need to fix this.
        //System.out.println("setting to metal");
      //UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
      //} else {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        //}
    } catch (Exception e) { 
      e.printStackTrace();
    }

    //try {
    PdeBase app = new PdeBase();
    // people attempting to use p5 in headless mode are
    // already setting themselves up for disappointment
    //} catch (HeadlessException e) {
    //e.printStackTrace();
    //System.exit(1);
    //}
  }


  // hack for #@#)$(* macosx
  public Dimension getMinimumSize() {
    return new Dimension(500, 500);
  }


  public PdeBase() {
    super(WINDOW_TITLE);

    try {
      icon = Toolkit.getDefaultToolkit().getImage("lib/icon.gif");
      this.setIconImage(icon);
    } catch (Exception e) { } // fail silently, no big whup

    windowListener = new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
        handleQuit();
      }
    };
    //frame.addWindowListener(windowListener);
    this.addWindowListener(windowListener);

    properties = new Properties();
    try {
      //properties.load(new FileInputStream("lib/pde.properties"));
      //#URL where = getClass().getResource("PdeBase.class");
      //System.err.println(where);
      //System.getProperties().list(System.err);
      //System.err.println("userdir = " + System.getProperty("user.dir"));

      if (PdeBase.platform == PdeBase.MACOSX) {
        //String pkg = "Proce55ing.app/Contents/Resources/Java/";
        //properties.load(new FileInputStream(pkg + "pde.properties"));
        //properties.load(new FileInputStream(pkg + "pde.properties_macosx"));
        properties.load(new FileInputStream("lib/pde.properties"));
        properties.load(new FileInputStream("lib/pde_macosx.properties"));

      } else if (PdeBase.platform == PdeBase.MACOS9) {
        properties.load(new FileInputStream("lib/pde.properties"));
        properties.load(new FileInputStream("lib/pde_macos9.properties"));

      } else {  
        // under win95, current dir not set properly
        // so using a relative url like "lib/" won't work
        properties.load(getClass().getResource("pde.properties").openStream());
        String platformProps = "pde_" + platforms[platform] + ".properties";
        properties.load(getClass().getResource(platformProps).openStream());
      }
      //properties.list(System.out);

    } catch (Exception e) {
      System.err.println("Error reading pde.properties");
      e.printStackTrace();
      //System.exit(1);
    }


    // 0058 check to see if quicktime for java is installed on windows
    // 0058 since it's temporarily required for 0058
    // 0059 still required for 0059, since BApplet uses it when
    // 0059 compiled with video enabled. the fix for this ain't easy.

    if (platform == WINDOWS) {
      //println(System.getenv("QTJAVA"));
      //Process p = Runtime.getRuntime().exec("c:\\windows\\system32\\cmd.exe /C set");
      /*
      try {
        Process p = Runtime.getRuntime().exec("cmd /C echo %QTJAVA%");
        InputStream is = p.getInputStream();
        StringBuffer sb = new StringBuffer();
        int c;
        while ((c = is.read()) != '\r') {
          if (c == '\"') continue;
          //println(c);
          sb.append((char)c);
        }
        is.close();
        println(">>" + sb.toString() + "<<");
      } catch (IOException e) {
        e.printStackTrace();
      }
      */

      // location for 95/98/ME/XP
      //File qt1 = new File("C:\\WINDOWS\\system32\\QTJava.zip");
      // location for win2k
      //File qt2 = new File("C:\\WINNT\\system32\\QTJava.zip");

      //if (!qt1.exists() && !qt2.exists()) {
      //System.out.println("jcp = " + System.getProperty("java.class.path"));

      try {
        Class c = Class.forName("quicktime.std.StdQTConstants");
        //System.out.println("class is " + c);

      } catch (ClassNotFoundException e) {
        e.printStackTrace();

        final String message = 
          "QuickTime for Java could not be found.\n" +
          "Please download QuickTime from Apple at:\n" + 
          "http://www.apple.com/quicktime/download\n" + 
          "and use the 'Custom' install to make sure\n" +
          "that QuickTime for Java is included.\n" +
          "If it's already installed, try reinstalling.";

        JOptionPane.showMessageDialog(this, message, 
                                      "Could not find QuickTime for Java",
                                      JOptionPane.WARNING_MESSAGE);
        System.exit(1);  // can't run without quicktime
      }
    }


    // read in the keywords for the reference

    final String KEYWORDS = "pde_keywords.properties";
    keywords = new Properties();
    try {
      // this probably won't work on macos9
      // seems to work on macosx java 1.4, though i don't think
      // it worked on java 1.3 for macosx (see implementation above)
      keywords.load(getClass().getResource(KEYWORDS).openStream());
      
    } catch (Exception e) {
      System.err.println("Error loading keywords, " + 
                         "reference lookup will be unavailable");
      System.err.println(e.toString());
      e.printStackTrace();
    }


    // build the editor object

    editor = new PdeEditor(this);
#ifdef SWINGSUCKS
    this.setLayout(new BorderLayout());
    this.add("Center", editor);
#else
    getContentPane().setLayout(new BorderLayout());
    getContentPane().add("Center", editor);
#endif

    MenuBar menubar = new MenuBar();
    Menu menu;
    MenuItem item;

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

    menu.addSeparator();

    item = new MenuItem("Find in Reference");
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) { 
          if (editor.textarea.isSelectionActive()) {
            String text = editor.textarea.getSelectedText();
            String referenceFile = (String) keywords.get(text);
            showReference(referenceFile);
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

    recordingHistory = getBoolean("history.recording", true);
    if (recordingHistory) {
      historyMenu = new Menu("History");
      menu.add(historyMenu);
      item = new MenuItem("Clear History");
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
    }

    beautifyMenuItem = new MenuItem("Beautify", new MenuShortcut('B'));
    //item.setEnabled(false);
    menu.add(beautifyMenuItem);

    //menu.addSeparator();
    serialMenu = new Menu("Serial Port");
    menu.add(serialMenu);

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

    menu.addActionListener(this);
    menubar.add(menu);  // add the sketch menu


    // help menu

    menu = new Menu("Help");
    menu.add(new MenuItem("Help"));
    menu.add(new MenuItem("Reference", new MenuShortcut('F')));
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

    //editor.frame = frame;  // no longer really used
    editor.init();
    rebuildSketchbookMenu(sketchbookMenu);
    buildSerialMenu();
    this.show();  // added back in for pde
  }

  /*
    PdeEditorTextPane

  Hashtable actions;

  //The following two methods allow us to find an
  //action provided by the editor kit by its name.
  private void createActionTable(JTextComponent textComponent) {
    actions = new Hashtable();
    Action[] actionsArray = textComponent.getActions();
    for (int i = 0; i < actionsArray.length; i++) {
      Action a = actionsArray[i];
      actions.put(a.getValue(Action.NAME), a);
    }
  }

  private Action getActionByName(String name) {
    //System.out.println(name);
    //System.out.println(name + " " + actions);
    return (Action)(actions.get(name));
  }
  */

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

      //sketchbookFolder = new File("sketchbook");
      sketchbookFolder = new File(get("sketchbook.path", "sketchbook"));
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
    if (!hfile.exists()) return;

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


  class SerialMenuListener implements ItemListener /*, ActionListener*/ {
    //public SerialMenuListener() { }

    public void itemStateChanged(ItemEvent e) {
      int count = serialMenu.getItemCount();
      for (int i = 0; i < count; i++) {
        ((CheckboxMenuItem)serialMenu.getItem(i)).setState(false);
      }
      CheckboxMenuItem item = (CheckboxMenuItem)e.getSource();
      item.setState(true);
      String name = item.getLabel();
      //System.out.println(item.getLabel());
      PdeBase.properties.put("serial.port", name);
      //System.out.println("set to " + get("serial.port"));
    }
    
    /*
    public void actionPerformed(ActionEvent e) {
      System.out.println(e.getSource());
      String name = e.getActionCommand();
      PdeBase.properties.put("serial.port", name);
      System.out.println("set to " + get("serial.port"));
      //editor.skOpen(path + File.separator + name, name);
      // need to push "serial.port" into PdeBase.properties
    }
    */
  }

  protected void buildSerialMenu() {
    // get list of names for serial ports
    // have the default port checked (if present)

    SerialMenuListener listener = new SerialMenuListener();
    String defaultName = get("serial.port", "unspecified");
    boolean problem = false;

    // if this is failing, it may be because
    // lib/javax.comm.properties is missing
    try {
      //System.out.println("building port list");
      Enumeration portList = CommPortIdentifier.getPortIdentifiers();
      while (portList.hasMoreElements()) {
        CommPortIdentifier portId = 
          (CommPortIdentifier) portList.nextElement();
        //System.out.println(portId);
        
        if (portId.getPortType() == CommPortIdentifier.PORT_SERIAL) {
          //if (portId.getName().equals(port)) {
          String name = portId.getName();
          CheckboxMenuItem mi = 
            new CheckboxMenuItem(name, name.equals(defaultName));
          //mi.addActionListener(listener);
          mi.addItemListener(listener);
          serialMenu.add(mi);
        }
      }
    } catch (UnsatisfiedLinkError e) {
      e.printStackTrace();
      problem = true;

    } catch (Exception e) {
      System.out.println("exception building serial menu");
      e.printStackTrace();
    }

    if (serialMenu.getItemCount() == 0) {
      //System.out.println("dimming serial menu");
      serialMenu.setEnabled(false);
    }

    // macosx fails on its own when trying to load the library
    // so need to explicitly try here.. not sure if this is the 
    // correct lib, but it's at least one that's loaded inside
    // the javacomm solaris stuff, which is the .jar that's included
    // with the osx release (and rxtx takes over)
    /*
    if (platform == MACOSX) {
      try {
        System.loadLibrary("SolarisSerialParallel");
      } catch (UnsatisfiedLinkError e) {
        //e.printStackTrace();
        problem = true;
      }
    }
    */

    // only warn them if this is the first time
    if (problem && firstTime) {
      JOptionPane.showMessageDialog(this, //frame,
                                    "Serial port support not installed.\n" +
                                    "Check the readme for instructions\n" +
                                    "if you need to use the serial port.    ",
                                    "Serial Port Warning",
                                    JOptionPane.WARNING_MESSAGE);
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
    JOptionPane.showMessageDialog(this, //frame,
                                  "Preferences are in the 'lib' folder\n" +
                                  "inside text files named pde.properties\n" +
                                  "and pde_" + platforms[platform] + 
                                  ".properties",
                                  "Preferences",
                                  JOptionPane.INFORMATION_MESSAGE);
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
        if (!url.startsWith("http://")) url = "file://" + url;
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


  // all the information from pde.properties

  static public String get(String attribute) {
    return get(attribute, null);
  }

  static public String get(String attribute, String defaultValue) {
    //String value = (properties != null) ?
    //properties.getProperty(attribute) : applet.getParameter(attribute);
    String value = properties.getProperty(attribute);

    return (value == null) ? 
      defaultValue : value;
  }

  static public boolean getBoolean(String attribute, boolean defaultValue) {
    String value = get(attribute, null);
    return (value == null) ? defaultValue : 
      (new Boolean(value)).booleanValue();

    /*
      supposedly not needed, because anything besides 'true'
      (ignoring case) will just be false.. so if malformed -> false
    if (value == null) return defaultValue;

    try {
      return (new Boolean(value)).booleanValue();
    } catch (NumberFormatException e) {
      System.err.println("expecting an integer: " + attribute + " = " + value);
    }
    return defaultValue;
    */
  }

  static public int getInteger(String attribute, int defaultValue) {
    String value = get(attribute, null);
    if (value == null) return defaultValue;

    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException e) { 
      // ignored will just fall through to returning the default
      System.err.println("expecting an integer: " + attribute + " = " + value);
    }
    return defaultValue;
    //if (value == null) return defaultValue;
    //return (value == null) ? defaultValue : 
    //Integer.parseInt(value);
  }

  static public Color getColor(String name, Color otherwise) {
    Color parsed = null;
    String s = get(name, null);
    if ((s != null) && (s.indexOf("#") == 0)) {
      try {
        int v = Integer.parseInt(s.substring(1), 16);
        parsed = new Color(v);
      } catch (Exception e) {
      }
    }
    if (parsed == null) return otherwise;
    return parsed;
  }


  static public Font getFont(String which, Font otherwise) {
    //System.out.println("getting font '" + which + "'");
    String str = get(which);
    if (str == null) return otherwise;  // ENABLE LATER
    StringTokenizer st = new StringTokenizer(str, ",");
    String fontname = st.nextToken();
    String fontstyle = st.nextToken();
    return new Font(fontname, 
                    ((fontstyle.indexOf("bold") != -1) ? Font.BOLD : 0) | 
                    ((fontstyle.indexOf("italic") != -1) ? Font.ITALIC : 0),
                    Integer.parseInt(st.nextToken()));
  }


  static public SyntaxStyle getStyle(String what, String dflt) {
    String str = get("editor.program." + what + ".style", dflt);

    StringTokenizer st = new StringTokenizer(str, ",");

    String s = st.nextToken();
    if (s.indexOf("#") == 0) s = s.substring(1);
    Color color = new Color(Integer.parseInt(s, 16));

    s = st.nextToken();
    boolean bold = (s.indexOf("bold") != -1);
    boolean italic = (s.indexOf("italic") != -1);
    //System.out.println(str + " " + bold + " " + italic);

    return new SyntaxStyle(color, italic, bold);
  }


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


  // this could be pruned further
  // also a similar version inside PdeEditor 
  // (at least the binary portion)
  /*
  static public String getFile(String filename) {
    if (filename.length() == 0) {
      return null;
    }
    URL url;
    InputStream stream = null;
    String openMe;
    byte temp[] = new byte[65536];  // 64k, 16k was too small

    try {
      // if running as an application, get file from disk
      stream = new FileInputStream(filename);

    } catch (Exception e1) { try {
      url = this.getClass().getResource(filename);
      stream = url.openStream();

    } catch (Exception e2) { try {
      // Try to open the param string as a URL
      url = new URL(filename);
      stream = url.openStream();
        
    } catch (Exception e3) {
      return null;
    } } }

    try {
      int offset = 0;
      while (true) {
        int byteCount = stream.read(temp, offset, 1024);
        if (byteCount <= 0) break;
        offset += byteCount;
      }
      byte program[] = new byte[offset];
      System.arraycopy(temp, 0, program, 0, offset);

      //return languageEncode(program);
      // convert the bytes based on the current encoding
      try {
        if (encoding == null)
          return new String(program);
        return new String(program, encoding);
      } catch (UnsupportedEncodingException e) {
        e.printStackTrace();
        encoding = null;
        return new String(program);
      }

    } catch (Exception e) {
      System.err.println("problem during download");
      e.printStackTrace();
      return null;
    }
  }
  */

  /*
  static public boolean hasFullPrivileges() {
    //if (applet == null) return true;  // application
    //return false;
    return !isApplet();
  }
  */
}

