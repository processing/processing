import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.zip.*;
import javax.comm.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.undo.*;


public class PdeBase extends Frame implements ActionListener {
  static Properties properties;
  static Frame frame;  // now 'this'
  static String encoding;
  static Image icon;

  protected UndoAction undoAction;
  protected RedoAction redoAction;
  protected UndoManager undo = new UndoManager();

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
  MenuItem saveMenuItem;
  MenuItem saveAsMenuItem;
  MenuItem beautifyMenuItem;
  CheckboxMenuItem externalEditorItem;

  static final String WINDOW_TITLE = "Proce55ing";

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
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    } catch (Exception e) { 
      e.printStackTrace();
    }

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
    //#ifdef JDK14
    //    try {
      //#endif
    /*
    frame = new Frame(WINDOW_TITLE) {
	public Dimension getMinimumSize() {
	  return new Dimension(300, 300);
	}
      };
    */
    frame = this;  // clean this up later
    //#ifdef JDK14

    //#endif

    try {
      icon = Toolkit.getDefaultToolkit().getImage("lib/icon.gif");
      frame.setIconImage(icon);
    } catch (Exception e) { } // fail silently

    windowListener = new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
	//System.exit(0);
	editor.doQuit();
      }
    };
    frame.addWindowListener(windowListener);

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
    //    int width = getInteger("window.width", 600);
    //    int height = getInteger("window.height", 350);

    /*
    encoding = get("encoding");
    boolean beautify = false; 
    boolean convertSemicolons = false;
    String program = get("program"); 
    if (program != null) { 
    //program = getFile(program);
    } else {
      program = get("inline_program");
      convertSemicolons = true;
    } 
    if (program != null) {
      // don't beautify if it's java code
      if (program.indexOf("extends PdePlayer") == -1) {
	// don't convert ; to \n if scheme  
	if (program.charAt(0) != ';') {  
	  if (convertSemicolons) {
	    program = program.replace(';', '\n'); 
	  }
	  // not scheme, but don't beautify if it's python 
	  if (program.charAt(0) != '#') 
	    beautify = true; 
	}  
      }
    } 
    */

    editor = new PdeEditor(this);
    frame.setLayout(new BorderLayout());
    frame.add("Center", editor);

    MenuBar menubar = new MenuBar();
    Menu menu;
    MenuItem item;

    menu = new Menu("File");
    menu.add(new MenuItem("New", new MenuShortcut('N')));
    sketchbookMenu = new Menu("Open");
    //rebuildSketchbookMenu(openMenu);
    menu.add(sketchbookMenu);
    saveMenuItem = new MenuItem("Save", new MenuShortcut('S'));
    saveAsMenuItem = new MenuItem("Save as...", new MenuShortcut('S', true));
    menu.add(saveMenuItem);
    menu.add(saveAsMenuItem);
    menu.add(new MenuItem("Rename..."));
    //menu.add(new MenuItem("Save", new MenuShortcut('S')));
    //menu.add(new MenuItem("Save as...", new MenuShortcut('S', true)));
    //menu.add(new MenuItem("Rename", new MenuShortcut('S', true)));
    //menu.add(new MenuItem("Duplicate", new MenuShortcut('D')));
    menu.add(new MenuItem("Export to Web", new MenuShortcut('E')));
    item = new MenuItem("Export Application", new MenuShortcut('E', true));
    item.setEnabled(false);
    menu.add(item);

    menu.addSeparator();
    menu.add(new MenuItem("Proce55ing.net", new MenuShortcut('5')));
    menu.add(new MenuItem("Reference", new MenuShortcut('F')));
    menu.addSeparator();
    menu.add(new MenuItem("Quit", new MenuShortcut('Q')));
    menu.addActionListener(this);
    menubar.add(menu);

    createActionTable(editor.textarea);
    menu = new Menu("Edit");
    //undoAction = new UndoAction();
    //menu.add(undoAction);
    item = new MenuItem("Undo", new MenuShortcut('Z'));
    item.addActionListener(undoAction = new UndoAction());
    menu.add(item);
    item = new MenuItem("Redo", new MenuShortcut('Y'));
    item.addActionListener(redoAction = new RedoAction());
    menu.add(item);
    menu.addSeparator();

    item = new MenuItem("Cut", new MenuShortcut('X'));
    //Action act = getActionByName(DefaultEditorKit.cutAction);
    //System.out.println("act is " + act);
    item.addActionListener(getActionByName(DefaultEditorKit.cutAction));
    menu.add(item);
    item = new MenuItem("Copy", new MenuShortcut('C'));
    item.addActionListener(getActionByName(DefaultEditorKit.copyAction));
    menu.add(item);
    item = new MenuItem("Paste", new MenuShortcut('V'));
    item.addActionListener(getActionByName(DefaultEditorKit.pasteAction));
    menu.add(item);
    menu.addSeparator();
    item = new MenuItem("Select All", new MenuShortcut('A'));
    item.addActionListener(getActionByName(DefaultEditorKit.selectAllAction));
    menu.add(item);
    menubar.add(menu);

    // i hear a cs prof or a first year student screaming somewhere
    Document document = editor.textarea.document;
    document.addUndoableEditListener(new MyUndoableEditListener());

    menu = new Menu("Sketch");
    menu.add(new MenuItem("Run", new MenuShortcut('R')));
    menu.add(new MenuItem("Present", new MenuShortcut('P')));
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

    frame.setMenuBar(menubar);

    Insets insets = frame.getInsets();
    Toolkit tk = Toolkit.getDefaultToolkit();
    Dimension screen = tk.getScreenSize();

    // THESE CAN BE REMOVED TO SOME EXTENT
    /*
    int frameX = getInteger("window.x", (screen.width - width) / 2);
    int frameY = getInteger("window.y", (screen.height - height) / 2);

    frame.setBounds(frameX, frameY, 
		    width + insets.left + insets.right, 
		    height + insets.top + insets.bottom);
    */

    //frame.reshape(50, 50, width + insets.left + insets.right, 
    //	  height + insets.top + insets.bottom);

    // i don't like this being here, but..
    //((PdeEditor)environment).graphics.frame = frame;
    //((PdeEditor)environment).frame = frame
    frame.pack();  // maybe this should be before the setBounds call

    //System.out.println(frame.getMinimumSize() + " " + frame.getSize());

    editor.frame = frame;  // no longer really used
    editor.init();
    rebuildSketchbookMenu(sketchbookMenu);
    buildSerialMenu();
    frame.show();  // added back in for pde
  }


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
	System.out.println("Unable to undo: " + ex);
	ex.printStackTrace();
      }
      updateUndoState();
      redoAction.updateRedoState();
    }

    protected void updateUndoState() {
      if (undo.canUndo()) {
	this.setEnabled(true);
	putValue(Action.NAME, undo.getUndoPresentationName());
      } else {
	this.setEnabled(false);
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
	System.out.println("Unable to redo: " + ex);
	ex.printStackTrace();
      }
      updateRedoState();
      undoAction.updateUndoState();
    }

    protected void updateRedoState() {
      if (undo.canRedo()) {
	this.setEnabled(true);
	putValue(Action.NAME, undo.getRedoPresentationName());
      } else {
	this.setEnabled(false);
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

      sketchbookFolder = new File("sketchbook");
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

      SketchbookMenuListener userMenuListener = 
	new SketchbookMenuListener(userPath);

      String entries[] = new File(userPath).list();
      boolean added = false;
      for (int j = 0; j < entries.length; j++) {
	if (entries[j].equals(".") || 
	    entries[j].equals("..") ||
	    entries[j].equals("CVS") ||
	    entries[j].equals(".cvsignore")) continue;
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

      // other available subdirectories
      /*
      String toplevel[] = sketchbookFolder.list();
      added = false;
      for (int i = 0; i < toplevel.length; i++) {
	if (toplevel[i].equals(editor.userName) ||
	    toplevel[i].equals(".") ||
	    toplevel[i].equals("..") ||
	    toplevel[i].equals("CVS") ||
	    toplevel[i].equals(".cvsignore")) continue;

	added = true;
	Menu subMenu = new Menu(toplevel[i]);
	File subFolder = new File(sketchbookFolder, toplevel[i]);
	String subPath = subFolder.getCanonicalPath();
	SketchbookMenuListener subMenuListener = 
	  new SketchbookMenuListener(subPath);

	entries = subFolder.list();
	if (entries != null) {
	  for (int j = 0; j < entries.length; j++) {
	    if (entries[j].equals(".") || 
		entries[j].equals("..") ||
		entries[j].equals("CVS") ||
		entries[j].equals(".cvsignore")) continue;
	    //subMenu.add(entries[j]);
	    if (new File(subFolder, entries[j] + File.separator + 
			 entries[j] + ".pde").exists()) {
	      MenuItem item = new MenuItem(entries[j]);
	      item.addActionListener(subMenuListener);
	      subMenu.add(item);
	    }
	  }
	}

	menu.add(subMenu);
      }
      if (added) menu.addSeparator();
      */
      addSketches(menu, sketchbookFolder, true);

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


  protected void addSketches(Menu menu, File folder, boolean root) 
    throws IOException {
    String list[] = folder.list();
    SketchbookMenuListener listener = 
      new SketchbookMenuListener(folder.getCanonicalPath());

    for (int i = 0; i < list.length; i++) {
      if (list[i].equals(editor.userName) && root) continue;
	  
      if (list[i].equals(".") ||
	  list[i].equals("..") ||
	  list[i].equals("CVS") ||
	  list[i].equals(".cvsignore")) continue;

      File subfolder = new File(folder, list[i]);
      if (new File(subfolder, list[i] + ".pde").exists()) {
	MenuItem item = new MenuItem(list[i]);
	item.addActionListener(listener);
	menu.add(item);

      } else {  // might contain other dirs, get recursive
	Menu submenu = new Menu(list[i]);
	menu.add(submenu);
	addSketches(submenu, subfolder, false);
      }
    }
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
      //e.printStackTrace();
      problem = true;

    } catch (Exception e) {
      System.out.println("exception building serial menu");
      e.printStackTrace();
    }

    // only warn them if this is the first time
    if (problem && firstTime) {
      JOptionPane.showMessageDialog(frame,
				    "Serial port support not installed.\n" +
				    "Check the readme for instructions if you " +
				    "need to use the serial port.   ",
				    "Serial Port Warning",
				    JOptionPane.WARNING_MESSAGE);
    }
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

      /*
    } else if (command.equals("Rename")) {
      editor.skDuplicateRename(true);

    } else if (command.equals("Duplicate")) {
      editor.skDuplicateRename(false);
      */

    } else if (command.equals("Export to Web")) {
      editor.skExport();

    } else if (command.equals("Proce55ing.net")) {
      if (platform == WINDOWS) {
	try {
	  Runtime.getRuntime().exec("c:\\progra~1\\intern~1\\iexplore http://Proce55ing.net");
	  //Runtime.getRuntime().exec("start http://Proce55ing.net");
	} catch (IOException e) {
	  e.printStackTrace();
	}

      } else if ((platform == MACOS9) || (platform == MACOSX)) {
#ifdef MACOS
	try {
	  com.apple.mrj.MRJFileUtils.openURL("http://Proce55ing.net");
	} catch (IOException e) {
	  e.printStackTrace();
	}
#endif

      } else if (platform == LINUX) {
	try {
	  // wild ass guess
	  Runtime.getRuntime().exec("mozilla http://Proce55ing.net");
	} catch (IOException e) {
	  e.printStackTrace();
	}

      } else {
	System.err.println("unspecified platform");
      }

    } else if (command.equals("Reference")) {
      if (platform == WINDOWS) {
	try {
	  Runtime.getRuntime().exec("cmd /c reference\\index.html");
	} catch (IOException e) {
	  e.printStackTrace();
	}

      } else if ((platform == MACOSX) || (platform == MACOS9)) {
#ifdef MACOS
	try {
	  com.apple.mrj.MRJFileUtils.openURL("reference/index.html");
	} catch (IOException e) {
	  e.printStackTrace();
	}
#endif

      } else if (platform == LINUX) {
	try {
	  // another wild ass guess
	  Runtime.getRuntime().exec("mozilla reference/index.html");
	} catch (IOException e) {
	  e.printStackTrace();
	}

      } else {
	System.err.println("unspecified platform");
      }

    } else if (command.equals("Quit")) {
      editor.doQuit();
      //editor.initiate(Editor.QUIT);

    } else if (command.equals("Run")) {
      editor.doRun(false);

    } else if (command.equals("Present")) {
      editor.doRun(true);
      //editor.doPresent();

    } else if (command.equals("Stop")) {    
      if (editor.presenting) {
	editor.doClose();
      } else {
	editor.doStop();
      }

    } else if (command.equals("Refresh")) {    
      //System.err.println("got refresh");
      rebuildSketchbookMenu(sketchbookMenu);      

    } else if (command.equals("Beautify")) {
      editor.doBeautify();

      //} else if (command.equals("Use External Editor")) {
      //boolean external = externalEditorItem.getState();
      //external = !external;
      //editor.setExternalEditor(external);

      // disable save, save as menus
      
    }
    //if (command.equals("Save QuickTime movie...")) {
    //  ((PdeEditor)environment).doRecord();
    //} else if (command.equals("Quit")) {
    //  System.exit(0);
    //}
  }


  // does this do anything useful?
  /*
  public void destroy() {
    if (editor != null) {
      editor.terminate();
    }
  }
  */

  /*
  public void paint(Graphics g) {
    if (errorState) {
      g.setColor(Color.red);
      Dimension d = size();
      g.fillRect(0, 0, d.width, d.height);
    }
  }
  */

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
    return new Font(st.nextToken(), 
		    st.nextToken().equals("bold") ? Font.BOLD : Font.PLAIN,
		    Integer.parseInt(st.nextToken()));
  }

  static public SimpleAttributeSet getStyle(String what, 
					    SimpleAttributeSet otherwise) {
    String str = get("editor.program." + what + ".style");
    if (str == null) return otherwise;  // ENABLE LATER
    StringTokenizer st = new StringTokenizer(str, ",");

    SimpleAttributeSet style = new SimpleAttributeSet();

    StyleConstants.setFontFamily(style, st.nextToken());

    String s = st.nextToken();
    StyleConstants.setBold(style, s.indexOf("bold") != -1);
    StyleConstants.setItalic(style, s.indexOf("italic") != -1);

    StyleConstants.setFontSize(style, Integer.parseInt(st.nextToken()));

    s = st.nextToken();
    if (s.indexOf("#") == 0) s = s.substring(1);
    StyleConstants.setForeground(style, new Color(Integer.parseInt(s, 16)));

    s = st.nextToken();
    if (s.indexOf("#") == 0) s = s.substring(1);
    StyleConstants.setBackground(style, new Color(Integer.parseInt(s, 16)));

    return style;
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
      url = frame.getClass().getResource(filename);
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

  /*
  static public boolean hasFullPrivileges() {
    //if (applet == null) return true;  // application
    //return false;
    return !isApplet();
  }
  */
}

