#ifndef KVM


import java.awt.*;
import java.applet.Applet;
import java.io.*;
import java.net.*;
import java.util.*;


public class PdeApplet extends Applet
{
  static PdeApplet applet;
  static Properties properties;
  boolean errorState;

#ifndef PLAYER
  String encoding;
  //PdeEnvironment environment;
  PdeEditor editor;
#endif

  public void init() {
    applet = this;
    //System.getProperties().list(System.out);
    //System.out.println("home = " + System.getProperty("user.home"));
    //System.out.println("prefix = " + System.getProperty("sys.prefix"));

#ifdef PLAYER
    // because it's the player version, cut out all the 
    // other crap, so that this file is as small as possible

    //} else if (mode.equals("player")) {
    // could also do a class.forname for jdk11
    //PdePlayerProgram dpp = new PdePlayerProgram(this);
    try {
      String program = get("program");
      PdePlayer player = 
	((PdePlayer) Class.forName(program).newInstance());
      add(player);
      //editor = player;
      player.init(this);
      player.start();
    } catch (Exception e) {
      e.printStackTrace();
      errorState = true;
    }
#else
    encoding = get("encoding");

#ifdef DBN
    new DbnPreprocessor(this);
#endif

    String mode = get("mode", "editor");
    //System.err.println("mode is " + mode);
    if (mode.equals("editor")) {
#ifdef EDITOR
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
      editor = new PdeEditor(this, program);
      //if (beautify) editor.doBeautify(); 

      setLayout(new BorderLayout());
      add("Center", editor);
      //editor = editor;

      //convert();
#endif

      /*
    } else if (mode.equals("grid")) {
      // read 1 or more programs to be laid out in grid mode
      // first count how many programs
      int counter = 0;
      while (true) {
	if (get("program" + counter) == null)
	  break;
	counter++;
      }
      // next load the programs
      // what to do if zero programs in griddify?
      String filenames[] = new String[counter];
      String programs[] = new String[counter];
      for (int i = 0; i < counter; i++) {
	String filename = get("program" + i);
	programs[i] = readFile(filename);
      }
      PdeGrid grid = new PdeGrid(this, programs);
      setLayout(new BorderLayout());
      add("Center", grid);
      environment = grid;
      */

    } else if (mode.equals("none")) {
      // don't do anything, handled by subclass
    }
#endif PLAYER
  }


#ifndef PLAYER
  public void destroy() {
    if (editor != null) {
      editor.terminate();
    }
  }
#endif

  /*
#ifdef EDITOR
  // this is used by PdeFancy, but could be useful in other
  // contexts as well, i would imagine
  public void setProgram(String p) {
    if (environment instanceof PdeEditor) {
      ((PdeEditor)environment).setProgram(p);
    }
  }
#endif
  */

  public void paint(Graphics g) {
    if (errorState) {
      g.setColor(Color.red);
      Dimension d = size();
      g.fillRect(0, 0, d.width, d.height);
      //} else {
      //super(g);
    }
  }



#ifndef PLAYER
  /* loading order:
   * 0. if application, a file on the disk
   * 1. a file relative to the .html file containing the applet
   * 2. a url 
   * 3. a file relative to the .class files
   */
  public String readFile(String filename) {
    if (filename.length() == 0) {
      return null;
    }
    URL url;
    InputStream stream = null;
    String openMe;
    byte temp[] = new byte[65536];  // 64k, 16k was too small

    try {
      // this is two cases, one is bound to throw (or work)
      if (isApplet()) {
	// Try to open it relative to the document base
	url = new URL(getDocumentBase(), filename);
	stream = url.openStream();
      } else {
	// if running as an application, get file from disk
	stream = new FileInputStream(filename);
      }

    } catch (Exception e1) { try {
      if (isApplet()) {
	// now try to open it relative to the code base
	url = new URL(getCodeBase(), filename);
	stream = url.openStream();
      } else {
	url = getClass().getResource(filename);
	stream = url.openStream();
      } 

    } catch (Exception e2) { try {
      // Try to open the param string as a URL
      url = new URL(filename);
      stream = url.openStream();
	
    } catch (Exception e3) {
      //e1.printStackTrace(); 
      //e2.printStackTrace();
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

#ifdef EDITOR
  static public Image readImage(String name) {
    Image image = null;
    if (isApplet()) {
      image = applet.getImage(applet.getCodeBase(), name);
    } else {
      Toolkit tk = Toolkit.getDefaultToolkit();
      image =  tk.getImage("lib/" + name);
      //URL url = PdeApplet.class.getResource(name);
      //image = tk.getImage(url);
    }
    MediaTracker tracker = new MediaTracker(applet);
    tracker.addImage(image, 0);
    try {
      tracker.waitForAll();
    } catch (InterruptedException e) { }      
    return image;
  }
#endif  // EDITOR

#endif  // !PLAYER

  // all the information from PdeProperties

  static public String get(String attribute) {
    return get(attribute, null);
  }

  static public String get(String attribute, String defaultValue) {
    String value = (properties != null) ?
      properties.getProperty(attribute) : applet.getParameter(attribute);

    return (value == null) ? 
      defaultValue : value;
  }

#ifndef PLAYER
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

  static public boolean isMacintosh() {
    return System.getProperty("os.name").toLowerCase().indexOf("mac") != -1;
  }

  static public boolean hasFullPrivileges() {
    //if (applet == null) return true;  // application
    //return false;
    return !isApplet();
  }

  static public Font getFont(String which) {
    if (which.equals("editor")) {
      // 'Monospaced' and 'courier' also caused problems.. ;-/
      //return new Font("monospaced", Font.PLAIN, 12);
      return new Font("Monospaced", Font.PLAIN, 12);
    }
    return null;
  }
#endif  // PLAYER

  public String getNetServer() {
    String host = get("net_server", null);
    if (host != null) return host;

    if (isApplet()) {
      return getCodeBase().getHost();
    }
    return "dbn.media.mit.edu";
  }

  static public boolean isApplet() {
    return (properties == null);
  }
}


#else  // if it is the KVM


public class PdeApplet {
  public PdeApplet() { 
  }

  String get(String something) {
    return get(something, null);
  }

  String get(String something, String otherwise) {
    return null;
  }

  String readFile(String name) {
    // grab something out of the database
    return null;
  }
}


#endif


/* temporary, a little something for the kids */
/*
  static public void debugString(String s) {
  byte output[] = s.getBytes();
  for (int i = 0; i < output.length; i++) {
  if (output[i] >= 32) {
  System.out.print((char)output[i]);
  } else {
  System.out.print("\\" + (int)output[i]);
  if (output[i] == '\n') System.out.println();
  }
  }
  System.out.println();
  }
*/
