import java.awt.*;
import java.applet.Applet;
import java.io.*;
import java.net.*;
import java.util.*;
import java.awt.event.*;


/*


[ File ]
New
Open ->
Save
Duplicate
Export
-
Proce55ing.net
Reference
-
Quit


[ Open-> (Sketchbook) ]
sketch-001
sketch-002
sketch-003
-- 
Course ->
Examples -> 
Users ->
Proce55ing.net ->
--
Refresh List
Compile All


[ Edit ]
Undo
-
Cut
Copy 
Paste
-
Select all


[ Sketch ]
Play
Present
Stop


new sketch just makes a new sketch, doesn't ask for name
tries to do numbered version based on sketches already present

last section is configurable
choose a name for the entries, and a url for the base of the code
  file urls will be local, don't include file:/// to user
  http urls are base of directory where the code is found

*/ 


public class PdeBase /*extends Component*/ implements ActionListener {
  //static PdeApplet applet;
  static Properties properties;
  boolean errorState;

  String encoding;
  PdeEditor editor;

  // made static so that toFront() can be called by 
  // full screen code in editor
  static Frame frame;
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
	System.exit(0);
      }
    };
    frame.addWindowListener(windowListener);

#ifdef RECORDER
    MenuBar menubar = new MenuBar();
    Menu goodies = new Menu("Processing");
    goodies.add(new MenuItem("Save QuickTime movie..."));
    goodies.add(new MenuItem("Quit"));
    goodies.addActionListener(this);
    menubar.add(goodies);
    frame.setMenuBar(menubar);
#endif

    properties = new Properties();
    try {
      properties.load(new FileInputStream("lib/pde.properties"));

    } catch (Exception e) {
      System.err.println("Error reading pde.properties");
      e.printStackTrace();
      System.exit(1);
    }
    int width = getInteger("window.width", 600);
    int height = getInteger("window.height", 350);
    // ms jdk requires that BorderLayout is set explicitly
    //frame.add("Center", this);

    //applet = this;
    //System.getProperties().list(System.out);
    //System.out.println("home = " + System.getProperty("user.home"));
    //System.out.println("prefix = " + System.getProperty("sys.prefix"));

    encoding = get("encoding");

    //String mode = get("mode", "editor");
    //System.err.println("mode is " + mode);
    //if (mode.equals("editor")) {

    //System.err.println("editor not yet complete");
    //System.err.println("editor dammit");
    //System.exit(0);
    boolean beautify = false; 
    boolean convertSemicolons = false;
    String program = get("program"); 
    if (program != null) { 
      program = readFile(program);
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
    //add(hostess = new PdeEditor(this, program));
    //PdeEditor editor = new PdeEditor(this, program);
    editor = new PdeEditor(/*this,*/ program);
    //if (beautify) editor.doBeautify(); 

    frame.setLayout(new BorderLayout());
    //setLayout(new BorderLayout());
    //add("Center", editor);
    frame.add("Center", editor);

    MenuBar menubar = new MenuBar();

    Menu menu;
    MenuItem item;

    fileOpenItem = new MenuItem("Open");

    menu = new Menu("File");
    menu.add(new MenuItem("New"));
    menu.add(new MenuItem("Open"));
    menu.add(new MenuItem("New"));
    menu.add(new MenuItem("New"));
    menu.add(new MenuItem("New"));
    menu.add(new MenuItem("New"));


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
      editor.sketchbookOpen(path + File.separator + e.getActionCommand());
      //}
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


      // files for the current user (for now, most likely 'default')

      // header knows what the current user is
      String userPath = sketchbookPath + File.separator + header.user;

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
      for (int i = 0; i < toplevel; i++) {
	if ((toplevel[i].equals(header.user)) ||
	    (toplevel[i].equals(".")) ||
	    (toplevel[i].equals(".."))) continue;

	Menu subMenu = new Menu(toplevel[i]);
	File subFolder = new File(sketchbookFolder, toplevel[i]);
	String subPath = subfolder.getCanonicalPath();
	SketchbookMenuListener subMenuListener = 
	  new SketchbookMenuListener(subPath);

	String entries[] = subfolder.list();
	for (int j = 0; j < entries.length; j++) {
	  if ((entries[j].equals(".")) || 
	      (entries[j].equals(".."))) continue;
	  submenu.add(entries[j]);
	}

	menu.add(submenu);
      }
      menu.addSeparator();


    Menu skmenu = new Menu("Sketchbook");
  }

  public void actionPerformed(ActionEvent event) {
    String command = event.getActionCommand();
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

  // this could be pruned further
  public String readFile(String filename) {
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
      url = getClass().getResource(filename);
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


  // used by PdeEditorButtons
  static public Image readImage(String name) {
    Image image = null;
    //if (isApplet()) {
    //image = applet.getImage(applet.getCodeBase(), name);
    //} else {
    Toolkit tk = Toolkit.getDefaultToolkit();
    image =  tk.getImage("lib/" + name);
    //URL url = PdeApplet.class.getResource(name);
    //image = tk.getImage(url);
    //}
    //MediaTracker tracker = new MediaTracker(applet);
    MediaTracker tracker = new MediaTracker(frame);
    tracker.addImage(image, 0);
    try {
      tracker.waitForAll();
    } catch (InterruptedException e) { }      
    return image;
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

  /*
  static public boolean hasFullPrivileges() {
    //if (applet == null) return true;  // application
    //return false;
    return !isApplet();
  }
  */
}

