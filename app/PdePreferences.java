/* -*- mode: jde; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  PdePreferences - controls user preferences and environment settings
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
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.undo.*;

/*
#ifndef RXTX
import javax.comm.*;
#else
import gnu.io.*;
#endif
*/


/*
  need to bring all the prefs into here
  PdeEditor with its sketch.properties
  and PdeBase with pde.properties

  on first run:
  processing.properties is created in user.home
  it contains the contents of 
  pde.properties + pde_platform.properties
  and then begins writing additional sketch.properties stuff
 */
public class PdePreferences extends JComponent {

  // gui variables

  static final int BIG = 13;
  static final int BETWEEN = 13;
  static final int SMALL = 6;

  JFrame frame;
  int wide, high;

  JCheckBox newSketchPromptBox;
  JTextField sketchbookLocationField;
  JCheckbox externalEditorBox;


  // data model

  static Properties properties;  // needs to be accessible everywhere
  static Hashtable defaults;

  File preferencesFile;
  boolean firstTime;  // first time this feller has been run


  public PdePreferences() {

    /*
    switch (PdeBase.platform) {
    case PdeBase.WINDOWS:
      // the larger divider on windows is ugly with the little arrows
      // this makes it large enough to see (mouse changes) and use, 
      // but keeps it from being annoyingly obtrusive
      defaults.put("editor.divider.size", "2");
      break;

    case PdeBase.MACOSX:
      // the usual 12 point from other platforms is too big on osx
      // monospaced on java 1.3 was monaco, but on 1.4 it has changed
      // to courier, which actually matches other platforms better.
      // (and removes the 12 point being too large issue)
      // and monaco is nicer on macosx, so use that explicitly
      defaults.put("editor.program.font", "Monaco,plain,10");
      defaults.put("editor.console.font", "Monaco,plain,10");
      break;
    }
    */


    // getting started

    properties = new Properties();


    // load user preferences file

    String home = System.getProperty("user.home");
    preferencesFile = new File(home, ".processing");


    if (!preferencesFile.exists()) {
      // create a new preferences file if none exists

      String platformFilename = 
        "pde_" + PdeBase.platforms[PdeBase.platform] + ".properties";

      try {
        if ((PdeBase.platform == PdeBase.MACOSX) ||
            (PdeBase.platform == PdeBase.MACOS9)) {
          properties.load(new FileInputStream("lib/pde.properties"));
          properties.load(new FileInputStream("lib/" + platformFilename));

        } else {  
          // under win95, current dir not set properly
          // so using a relative url like "lib/" won't work
          properties.load(getClass().getResource("pde.properties").openStream());
          properties.load(getClass().getResource(platformFilename).openStream());
        }

      } catch (Exception e) {
        System.err.println("Error reading pde.properties");
        e.printStackTrace();
      }
    }


    // setup frame for the prefs

    frame = new JFrame("Preferences");
    frame.setResizable(false);

    Container pain = this;
    //Container pain = frame.getContentPane();
    pain.setLayout(null);

    int top = BIG;
    int left = BIG;
    int right = 0;

    JLabel label;
    JButton button;
    JComboBox combo;
    Dimension d, d2, d3;
    int h, v, vmax;


    // [ ] Prompt for name and folder when creating new sketch

    newSketchPromptBox = 
      new JCheckBox("Prompt for name when creating a new sketch");
    pain.add(newSketchPromptBox);
    d = newSketchPromptBox.getPreferredSize();
    newSketchPromptBox.setBounds(left, top, d.width, d.height);
    right = Math.max(right, left + d.width);
    top += d.height + BETWEEN;


    // Sketchbook location: [...............................]  [ Browse ]

    label = new JLabel("Sketchbook location:");
    pain.add(label);
    d = label.getPreferredSize();

    sketchbookLocationField = new JTextField(18);
    pain.add(sketchbookLocationField);
    d2 = sketchbookLocationField.getPreferredSize();

    button = new JButton("Browse");
    pain.add(button);
    d3 = button.getPreferredSize();

    // take max height of all components to vertically align em
    vmax = Math.max(Math.max(d.height, d2.height), d3.height);
    label.setBounds(left, top + (vmax-d.height)/2, 
                    d.width, d.height);
    h = left + d.width + BETWEEN;
    sketchbookLocation.setBounds(h, top + (vmax-d2.height)/2, 
                                 d2.width, d2.height);
    h += d2.width + BETWEEN;
    button.setBounds(h, top + (vmax-d3.height)/2, 
                     d3.width, d3.height);

    right = Math.max(right, h + d3.width + BIG);
    top += vmax + BETWEEN;


    // Default serial port:  [ COM1 + ]

    /*
    label = new JLabel("Default serial port:");
    pain.add(label);
    d = label.getPreferredSize();

    Vector list = buildPortList();
    combo = new JComboBox(list);
    pain.add(combo);
    d2 = combo.getPreferredSize();

    if (list.size() == 0) {
      label.setEnabled(false);
      combo.setEnabled(false);

    } else {
      String defaultName = PdePreferences.get("serial.port", "unspecified");
      combo.setSelectedItem(defaultName);
    }

    vmax = Math.max(d.height, d2.height);
    label.setBounds(left, top + (vmax-d.height)/2, 
                    d.width, d.height);
    h = left + d.width + BETWEEN;
    combo.setBounds(h, top + (vmax-d2.height)/2, 
                    d2.width, d2.height);    
    right = Math.max(right, h + d2.width + BIG);
    top += vmax + BETWEEN;
    */


    // [ ] Use external editor

    externalEditorBox = new JCheckBox("Use external editor");
    pain.add(externalEditorBox);
    d = newSketchPromptBox.getPreferredSize();
    newSketchPromptBox.setBounds(left, top, d.width, d.height);
    right = Math.max(right, left + d.width);
    top += d.height + BETWEEN;


    // 

    String blather = 
      "More preferences are in the 'lib' folder inside text files\n" +
      "named pde.properties and pde_" + 
      PdeBase.platforms[PdeBase.platform] + ".properties";

    JTextArea textarea = new JTextArea(blather);
    textarea.setBorder(new EmptyBorder(SMALL, SMALL, SMALL, SMALL));
    textarea.setFont(new Font("Dialog", Font.PLAIN, 12));
    pain.add(textarea);

    //pain.add(label);
    d = textarea.getPreferredSize();
    textarea.setBounds(left, top, d.width, d.height);
    top += d.height + BETWEEN;

    //

    wide = right + BIG;
    high = top + BIG;
    setSize(wide, high);

    Container content = frame.getContentPane();
    content.setLayout(new BorderLayout());
    content.add(this, BorderLayout.CENTER);

    frame.pack();

    Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
    frame.setLocation((screen.width - wide) / 2,
                      (screen.height - high) / 2);

    //

    frame.addWindowListener(new WindowAdapter() {
        public void windowClosing(WindowEvent e) {
          frame.hide();
        }
      });
  }


  public void showFrame() {
    frame.show();
  }


  // change settings based on what was chosen in the prefs

  public void apply() {
    //if (external editor checked) {
      editor.setExternalEditor(true);
      //}
  }


  // open the last-used sketch, etc

  public void init() {
    // load the last program that was in use

    Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
    int windowX = -1, windowY = 0, windowW = 0, windowH = 0;

    Properties skprops = new Properties();
    try {
      if (PdeBase.platform == PdeBase.MACOSX) {
        //String pkg = "Proce55ing.app/Contents/Resources/Java/";
        //skprops.load(new FileInputStream(pkg + "sketch.properties"));
        skprops.load(new FileInputStream("lib/sketch.properties"));

      } else if (PdeBase.platform == PdeBase.MACOS9) {
        skprops.load(new FileInputStream("lib/sketch.properties"));

      } else {
        skprops.load(getClass().getResource("sketch.properties").openStream());
      }

      windowX = Integer.parseInt(skprops.getProperty("window.x", "-1"));
      windowY = Integer.parseInt(skprops.getProperty("window.y", "-1"));
      windowW = Integer.parseInt(skprops.getProperty("window.w", "-1"));
      windowH = Integer.parseInt(skprops.getProperty("window.h", "-1"));

      // if screen size has changed, the window coordinates no longer
      // make sense, so don't use them unless they're identical
      int screenW = Integer.parseInt(skprops.getProperty("screen.w", "-1"));
      int screenH = Integer.parseInt(skprops.getProperty("screen.h", "-1"));

      if ((screen.width != screenW) || (screen.height != screenH)) {
        // probably not valid for this machine, so invalidate sizing
        windowX = -1;
      }

      String name = skprops.getProperty("sketch.name");
      String path = skprops.getProperty("sketch.directory");
      String user = skprops.getProperty("user.name");

      String what = path + File.separator + name + ".pde";

      if (windowX != -1) {
        String dividerLocation = 
          skprops.getProperty("editor.divider.location");
        if (dividerLocation != null) {
          splitPane.setDividerLocation(Integer.parseInt(dividerLocation));
        }
      }

      if (new File(what).exists()) {
        userName = user;
        skOpen(path, name);

      } else {
        userName = "default";
        skNew();
      }

      String serialPort = skprops.getProperty("serial.port");
      if (serialPort != null) {
        properties.put("serial.port", serialPort);
      }

      boolean ee = new Boolean(skprops.getProperty("editor.external", "false")).booleanValue();
      setExternalEditor(ee);

    } catch (Exception e) { 
      // this exception doesn't matter, it's just the normal course of things
      // the app reaches here when no sketch.properties file exists
      //e.printStackTrace();

      // indicator that this is the first time this feller has used p5
      firstTime = true;

      // even if folder for 'default' user doesn't exist, or
      // sketchbook itself is missing, mkdirs() will make it happy
      userName = "default";

      // doesn't exist, not available, make my own
      skNew();
    }

    if (windowX == -1) {
      //System.out.println("using defaults for window size");
      windowW = PdePreferences.getInteger("window.width", 500);
      windowH = PdePreferences.getInteger("window.height", 500);
      windowX = (screen.width - windowW) / 2;
      windowY = (screen.height - windowH) / 2;
    }
    //PdeBase.frame.setBounds(windowX, windowY, windowW, windowH);
    base.setBounds(windowX, windowY, windowW, windowH);
    //rebuildSketchbookMenu(PdeBase.sketchbookMenu);
  }

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


  public Dimension getPreferredSize() {
    return new Dimension(wide, high);
  }


  public void save() {
    try {
      FileOutputStream output = null;

      if (PdeBase.platform == PdeBase.MACOSX) {
        //String pkg = "Proce55ing.app/Contents/Resources/Java/";
        //output = new FileOutputStream(pkg + "sketch.properties");
        output = new FileOutputStream("lib/sketch.properties");

      } else if (PdeBase.platform == PdeBase.MACOS9) {
        output = new FileOutputStream("lib/sketch.properties");

      } else { // win95/98/ME doesn't set cwd properly
        URL url = getClass().getResource("buttons.gif");
        String urlstr = url.getFile();
        urlstr = urlstr.substring(0, urlstr.lastIndexOf("/") + 1) +
          "sketch.properties";
#ifdef JDK13
        // the ifdef is weird, but it's set for everything but 
        // macos9, and this will never get hit 
        output = new FileOutputStream(URLDecoder.decode(urlstr));
#else
        System.err.println("bad error while writing sketch.properties");
        System.err.println("you should never see this message");
#endif
      }

      Properties skprops = new Properties();

      //Rectangle window = PdeBase.frame.getBounds();
      Rectangle window = base.getBounds();
      Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();

      skprops.put("window.x", String.valueOf(window.x));
      skprops.put("window.y", String.valueOf(window.y));
      skprops.put("window.w", String.valueOf(window.width));
      skprops.put("window.h", String.valueOf(window.height));

      skprops.put("screen.w", String.valueOf(screen.width));
      skprops.put("screen.h", String.valueOf(screen.height));

      skprops.put("sketch.name", sketchName);
      skprops.put("sketch.directory", sketchDir.getCanonicalPath());
      skprops.put("user.name", userName);

      skprops.put("editor.external", externalEditor ? "true" : "false");
      skprops.put("editor.divider.location", 
                  String.valueOf(splitPane.getDividerLocation()));

      //skprops.put("serial.port", PdePreferences.get("serial.port", "unspecified"));

      skprops.save(output, "auto-generated by pde, please don't touch");

    } catch (IOException e) {
      System.err.println("doQuit: error saving properties");
      e.printStackTrace();
    }
}

  // all the information from pde.properties

  //static public String get(String attribute) {
  //return get(attribute, null);
  //}

  static public String get(String attribute /*, String defaultValue */) {
    //String value = (properties != null) ?
    //properties.getProperty(attribute) : applet.getParameter(attribute);
    String value = properties.getProperty(attribute);

    return (value == null) ? 
      defaultValue : value;
  }

  static public boolean getBoolean(String attribute /*, boolean defaultValue*/) {
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

  static public int getInteger(String attribute /*, int defaultValue*/) {
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


  static public Color getColor(String name /*, Color otherwise*/) {
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


  static public Font getFont(String which /*, Font otherwise*/) {
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


  static public SyntaxStyle getStyle(String what /*, String dflt*/) {
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


  /*
  class SerialMenuListener implements ItemListener {
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
  }
  */


  /*
  protected Vector buildPortList() {
    // get list of names for serial ports
    // have the default port checked (if present)
    Vector list = new Vector();

    //SerialMenuListener listener = new SerialMenuListener();
    boolean problem = false;

    // if this is failing, it may be because
    // lib/javax.comm.properties is missing.
    // java is weird about how it searches for java.comm.properties
    // so it tends to be very fragile. i.e. quotes in the CLASSPATH
    // environment variable will hose things.
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
          //CheckboxMenuItem mi = 
          //new CheckboxMenuItem(name, name.equals(defaultName));

          //mi.addItemListener(listener);
          //serialMenu.add(mi);
          list.addElement(name);
        }
      }
    } catch (UnsatisfiedLinkError e) {
      e.printStackTrace();
      problem = true;

    } catch (Exception e) {
      System.out.println("exception building serial menu");
      e.printStackTrace();
    }

    //if (serialMenu.getItemCount() == 0) {
      //System.out.println("dimming serial menu");
    //serialMenu.setEnabled(false);
    //}

    // only warn them if this is the first time
    if (problem && PdeBase.firstTime) {
      JOptionPane.showMessageDialog(this, //frame,
                                    "Serial port support not installed.\n" +
                                    "Check the readme for instructions\n" +
                                    "if you need to use the serial port.    ",
                                    "Serial Port Warning",
                                    JOptionPane.WARNING_MESSAGE);
    }
    return list;
  }
  */
}
