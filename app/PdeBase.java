import java.awt.*;
import java.applet.Applet;
import java.io.*;
import java.net.*;
import java.util.*;
import java.awt.event.*;


public class PdeBase implements ActionListener {
  static Properties properties;
  static Frame frame;
  static String encoding;

  boolean errorState;
  PdeEditor editor;

  WindowAdapter windowListener;

  File sketchbookFolder;
  String sketchbookPath;

  static final String WINDOW_TITLE = "Proce55ing";


  static public void main(String args[]) {
    PdeBase app = new PdeBase();
  }

  public PdeBase() {
    frame = new Frame(WINDOW_TITLE);

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
      properties.load(getClass().getResource("pde.properties").openStream());

    } catch (Exception e) {
      System.err.println("Error reading pde.properties");
      e.printStackTrace();
      System.exit(1);
    }
    int width = getInteger("window.width", 600);
    int height = getInteger("window.height", 350);

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

    editor = new PdeEditor(/*this,*/ /*program*/);
    frame.setLayout(new BorderLayout());
    frame.add("Center", editor);

    MenuBar menubar = new MenuBar();
    Menu menu;
    MenuItem item;

    menu = new Menu("File");
    menu.add(new MenuItem("New"));
    Menu openMenu = new Menu("Open");
    rebuildSketchbookMenu(openMenu);
    menu.add(openMenu);
    menu.add(new MenuItem("Save"));
    menu.add(new MenuItem("Duplicate"));
    menu.add(new MenuItem("Export"));
    menu.addSeparator();
    menu.add(new MenuItem("Proce55ing.net"));
    menu.add(new MenuItem("Reference"));
    menu.addSeparator();
    menu.add(new MenuItem("Quit"));
    menu.addActionListener(this);
    menubar.add(menu);

    // completely un-functional edit menu
    menu = new Menu("Edit");
    menu.add(new MenuItem("Undo"));
    menu.addSeparator();    
    menu.add(new MenuItem("Cut"));
    menu.add(new MenuItem("Copy"));
    menu.add(new MenuItem("Paste"));
    menu.addSeparator();
    menu.add(new MenuItem("Select all"));
    menu.setEnabled(false);
    menubar.add(menu);

    menu = new Menu("Sketch");
    menu.add(new MenuItem("Play"));
    menu.add(new MenuItem("Present"));
    menu.add(new MenuItem("Stop"));
    menu.addActionListener(this);
    menubar.add(menu);

    frame.setMenuBar(menubar);


    /*
    Menu fileMenu = new Menu("File");
    MenuItem mi;
    goodies.add(new MenuItem("Save QuickTime movie..."));
    goodies.add(new MenuItem("Quit"));
    goodies.addActionListener(this);
    menubar.add(goodies);
    frame.setMenuBar(menubar);
    */


    Insets insets = frame.getInsets();
    Toolkit tk = Toolkit.getDefaultToolkit();
    Dimension screen = tk.getScreenSize();
    int frameX = getInteger("window.x", (screen.width - width) / 2);
    int frameY = getInteger("window.y", (screen.height - height) / 2);

    frame.setBounds(frameX, frameY, 
		    width + insets.left + insets.right, 
		    height + insets.top + insets.bottom);
    //frame.reshape(50, 50, width + insets.left + insets.right, 
    //	  height + insets.top + insets.bottom);

    // i don't like this being here, but..
    //((PdeEditor)environment).graphics.frame = frame;
    //((PdeEditor)environment).frame = frame;
    editor.frame = frame;

    frame.pack();
    frame.show();  // added back in for pde
  }


  // listener for sketchbk items uses getParent() to figure out
  // the directories above it

  class SketchbookMenuListener implements ActionListener {
    //PdeEditor editor;
    String path;

    public SketchbookMenuListener(/*PdeEditor editor,*/ String path) {
      //this.editor = editor;
      this.path = path;
    }

    public void actionPerformed(ActionEvent e) {
      //if (e.getActionCommand().equals(NEW_SKETCH_ITEM)) {
      //editor.handleNew();

      //} else {
      //editor.sketchbookOpen(path + File.separator + e.getActionCommand());
      //}
      String name = e.getActionCommand();
      editor.skOpen(path, name); // + File.separator + name + 
      //File.separator + name + ".pde");
    }
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
      for (int j = 0; j < entries.length; j++) {
	if ((entries[j].equals(".")) || 
	    (entries[j].equals(".."))) continue;
	MenuItem item = new MenuItem(entries[j]);
	item.addActionListener(userMenuListener);
	menu.add(item);
	//submenu.add(entries[j]);
      }
      menu.addSeparator();


      // other available subdirectories

      String toplevel[] = sketchbookFolder.list();
      for (int i = 0; i < toplevel.length; i++) {
	if ((toplevel[i].equals(editor.userName)) ||
	    (toplevel[i].equals(".")) ||
	    (toplevel[i].equals(".."))) continue;

	Menu subMenu = new Menu(toplevel[i]);
	File subFolder = new File(sketchbookFolder, toplevel[i]);
	String subPath = subFolder.getCanonicalPath();
	SketchbookMenuListener subMenuListener = 
	  new SketchbookMenuListener(subPath);

	entries = subFolder.list();
	for (int j = 0; j < entries.length; j++) {
	  if ((entries[j].equals(".")) || 
	      (entries[j].equals(".."))) continue;
	  //subMenu.add(entries[j]);
	  MenuItem item = new MenuItem(entries[j]);
	  item.addActionListener(subMenuListener);
	  subMenu.add(item);
	}

	menu.add(subMenu);
      }
      //menu.addSeparator();

    } catch (IOException e) {
      e.printStackTrace();
    }
  }


  public void actionPerformed(ActionEvent event) {
    String command = event.getActionCommand();
    //System.out.println(command);

    if (command.equals("New")) {
      editor.skNew();

    } else if (command.equals("Save")) {
      editor.doSave();

    } else if (command.equals("Duplicate")) {
      editor.skDuplicate();

    } else if (command.equals("Export")) {
      editor.skExport();

    } else if (command.equals("Proce55ing.net")) {
      // same thing here with runtime

    } else if (command.equals("Reference")) {
      /*
      try {
	// need to get the stream from here
	Runtime.getRuntime().exec("cmd /c d:\\fry\\processing\\app\\application\\reference\\Transform.html");
      } catch (IOException e) {
	e.printStackTrace();
      }
      */

    } else if (command.equals("Quit")) {
      editor.doQuit();


    } else if (command.equals("Play")) {
      editor.doPlay();

    } else if (command.equals("Present")) {
      //editor.doPresent();

    } else if (command.equals("Stop")) {    
      editor.doStop();

    }
    //if (command.equals("Save QuickTime movie...")) {
    //  ((PdeEditor)environment).doRecord();
    //} else if (command.equals("Quit")) {
    //  System.exit(0);
    //}
  }


  // does this do anything useful?
  public void destroy() {
    if (editor != null) {
      editor.terminate();
    }
  }

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
  }

  static public int getInteger(String attribute, int defaultValue) {
    String value = get(attribute, null);
    return (value == null) ? defaultValue : 
      Integer.parseInt(value);
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

  static public boolean isMacintosh() {
    return System.getProperty("os.name").toLowerCase().indexOf("mac") != -1;
  }


  // used by PdeEditorButtons, but probably more later
  static public Image getImage(String name, Component who) {
    Image image = null;
    //if (isApplet()) {
    //image = applet.getImage(applet.getCodeBase(), name);
    //} else {
    Toolkit tk = Toolkit.getDefaultToolkit();
    image = tk.getImage(who.getClass().getResource(name));
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

