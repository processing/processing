/* -*- mode: jde; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  PdePreferences - controls user preferences and environment settings
  Part of the Processing project - http://Proce55ing.net

  Except where noted, code is written by Ben Fry and
  Copyright (c) 2001-03 Massachusetts Institute of Technology

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
import javax.swing.filechooser.*;
import javax.swing.text.*;
import javax.swing.undo.*;


/*
  need to bring all the prefs into here
  PdeEditor with its sketch.properties
  and PdeBase with pde.properties

  on first run:
  processing.properties is created in user.home
  it contains the contents of 
  pde.properties + pde_platform.properties
  and then begins writing additional sketch.properties stuff

  this class no longer uses the Properties class, since 
  properties files are iso8859-1, which is highly likely to 
  be a problem when trying to save sketch folders and locations
 */
public class PdePreferences extends JComponent {

  // prompt text stuff

  static final String PROMPT_YES     = "Yes";
  static final String PROMPT_NO      = "No";
  static final String PROMPT_CANCEL  = "Cancel";
  static final String PROMPT_OK      = "OK";
  static final String PROMPT_BROWSE  = "Browse";

  // mac needs it to be 70, windows needs 66, linux needs 76

  static /*final*/ int BUTTON_WIDTH  = 76;
  static /*final*/ int BUTTON_HEIGHT = 24;

  // value for the size bars, buttons, etc

  //static final int INSET_SIZE    = 5;
  static final int GRID_SIZE     = 33;

  // gui variables

  static final int GUI_BIG     = 13;
  static final int GUI_BETWEEN = 10;
  static final int GUI_SMALL   = 6;

  // gui elements

  JFrame frame;
  int wide, high;

  JTextField sketchbookLocationField;
  JCheckBox sketchPromptBox;
  JCheckBox exportLibraryBox;
  JCheckBox externalEditorBox;

  // the calling editor, so updates can be applied
  PdeEditor editor;


  // data model

  static Hashtable table = new Hashtable();;

  File preferencesFile;
  //boolean firstTime;  // first time this feller has been run


  public PdePreferences() {

    // start by loading the defaults, in case something
    // important was deleted from the user prefs

    try {
      load(PdeBase.getStream("pde.properties"));

    } catch (Exception e) {
      PdeBase.showError(null, "Could not read default settings.\n" + 
                        "You'll need to reinstall Processing.", e);
    }


    // check for platform-specific properties in the defaults

    String platformExtension = "." + PdeBase.platforms[PdeBase.platform];
    int extensionLength = platformExtension.length();

    Enumeration e = table.keys(); //properties.propertyNames();
    while (e.hasMoreElements()) {
      String key = (String) e.nextElement();
      if (key.endsWith(platformExtension)) {
        // this is a key specific to a particular platform
        String actualKey = key.substring(0, key.length() - extensionLength);
        String value = get(key);
        table.put(actualKey, value);
      }
    }


    // other things that have to be set explicitly for the defaults

    setColor("run.window.bgcolor", SystemColor.control);


    // next load user preferences file

    File home = new File(System.getProperty("user.home"));
    preferencesFile = new File(home, ".processing");

    if (!preferencesFile.exists()) {
      // create a new preferences file if none exists
      // saves the defaults out to the file
      save();  

    } else {
      // load the previous preferences file

      try {
        load(new FileInputStream(preferencesFile));

      } catch (Exception ex) {
        PdeBase.showError("Error reading preferences", 
                          "Error reading the preferences file. " +
                          "Please delete (or move)\n" +
                          preferencesFile.getAbsolutePath() +
                          " and restart Processing.", ex);
      }
    }


    // setup frame for the prefs

    frame = new JFrame("Preferences");
    //frame = new JDialog("Preferences");
    frame.setResizable(false);

    Container pain = this;
    //Container pain = frame.getContentPane();
    pain.setLayout(null);

    int top = GUI_BIG;
    int left = GUI_BIG;
    int right = 0;

    JLabel label;
    JButton button, button2;
    JComboBox combo;
    Dimension d, d2, d3;
    int h, v, vmax;


    // [ ] Prompt for name and folder when creating new sketch

    /*
    sketchPromptBox = 
      new JCheckBox("Prompt for name when opening or creating a sketch");
    pain.add(sketchPromptBox);
    d = sketchPromptBox.getPreferredSize();
    sketchPromptBox.setBounds(left, top, d.width, d.height);
    right = Math.max(right, left + d.width);
    top += d.height + GUI_BETWEEN;
    */


    // Sketchbook location: 
    // [...............................]  [ Browse ]

    label = new JLabel("Sketchbook location:");
    pain.add(label);
    d = label.getPreferredSize();
    label.setBounds(left, top, d.width, d.height);
    top += d.height; // + GUI_SMALL;

    sketchbookLocationField = new JTextField(40);
    pain.add(sketchbookLocationField);
    d = sketchbookLocationField.getPreferredSize();

    button = new JButton(PROMPT_BROWSE);
    button.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          JFileChooser fc = new JFileChooser();
          fc.setSelectedFile(new File(sketchbookLocationField.getText()));
          fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

          int returned = fc.showOpenDialog(new JDialog());
          if (returned == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            sketchbookLocationField.setText(file.getAbsolutePath());
          }
        }
      });
    pain.add(button);
    d2 = button.getPreferredSize();

    // take max height of all components to vertically align em
    vmax = Math.max(d.height, d2.height);
    //label.setBounds(left, top + (vmax-d.height)/2, 
    //              d.width, d.height);

    //h = left + d.width + GUI_BETWEEN;
    sketchbookLocationField.setBounds(left, top + (vmax-d.height)/2, 
                                      d.width, d.height);
    h = left + d.width + GUI_SMALL; //GUI_BETWEEN;
    button.setBounds(h, top + (vmax-d2.height)/2, 
                     d2.width, d2.height);

    right = Math.max(right, h + d2.width + GUI_BIG);
    top += vmax + GUI_BETWEEN;


    // [ ] Use external editor

    exportLibraryBox = new JCheckBox("Enable export to \"Library\"");
    pain.add(exportLibraryBox);
    d = exportLibraryBox.getPreferredSize();
    exportLibraryBox.setBounds(left, top, d.width, d.height);
    right = Math.max(right, left + d.width);
    top += d.height + GUI_BETWEEN;


    // [ ] Use external editor

    externalEditorBox = new JCheckBox("Use external editor");
    pain.add(externalEditorBox);
    d = externalEditorBox.getPreferredSize();
    externalEditorBox.setBounds(left, top, d.width, d.height);
    right = Math.max(right, left + d.width);
    top += d.height + GUI_BETWEEN;


    // More preferences are in the ...

    /*
    String blather = 
      "More preferences can be edited directly\n" + 
      "in the file " + preferencesFile.getAbsolutePath();
      //"More preferences are in the 'lib' folder inside text files\n" +
      //"named pde.properties and pde_" + 
      //PdeBase.platforms[PdeBase.platform] + ".properties";

    JTextArea textarea = new JTextArea(blather);
    textarea.setEditable(false);
    textarea.setBorder(new EmptyBorder(0, 0, 0, 0));
    textarea.setBackground(null);
    textarea.setFont(new Font("Dialog", Font.PLAIN, 12));
    pain.add(textarea);
    */
    label = new JLabel("More preferences can be edited directly");
    pain.add(label);
    d = label.getPreferredSize();
    label.setBounds(left, top, d.width, d.height);
    top += d.height; // + GUI_SMALL;

    label = new JLabel("in the file " + preferencesFile.getAbsolutePath());
    pain.add(label);
    d = label.getPreferredSize();
    label.setBounds(left, top, d.width, d.height);
    //textarea.setBounds(left, top, d.width, d.height);
    //top += d.height; // + GUI_BETWEEN;


    // [  OK  ] [ Cancel ]  maybe these should be next to the message?

    right = Math.max(right, left + d.width + GUI_BETWEEN + 
                     BUTTON_WIDTH + GUI_SMALL + BUTTON_WIDTH);

    button = new JButton(PROMPT_OK);
    button.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          applyFrame();
          disposeFrame();
        }
      });
    pain.add(button);
    d2 = button.getPreferredSize();
    BUTTON_HEIGHT = d2.height;

    // smoosh up to the line before
    top -= BUTTON_HEIGHT;

    h = right - (BUTTON_WIDTH + GUI_SMALL + BUTTON_WIDTH);
    button.setBounds(h, top, BUTTON_WIDTH, BUTTON_HEIGHT);
    h += BUTTON_WIDTH + GUI_SMALL;

    button = new JButton(PROMPT_CANCEL);
    button.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          disposeFrame();
        }
      });
    pain.add(button);
    button.setBounds(h, top, BUTTON_WIDTH, BUTTON_HEIGHT);

    top += BUTTON_HEIGHT + GUI_BETWEEN;


    // finish up

    wide = right + GUI_BIG;
    high = top + GUI_BIG;
    setSize(wide, high);

    Container content = frame.getContentPane();
    content.setLayout(new BorderLayout());
    content.add(this, BorderLayout.CENTER);

    frame.pack();

    Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
    frame.setLocation((screen.width - wide) / 2,
                      (screen.height - high) / 2);

    //

    /*
    frame.addWindowListener(new WindowAdapter() {
        public void windowClosing(WindowEvent e) {
          frame.hide();
        }
      });
    */
  }


  public Dimension getPreferredSize() {
    return new Dimension(wide, high);
  }


  // .................................................................


  public void disposeFrame() {
    frame.hide();
    editor.applyPreferences();
    editor.show();
    //frame.dispose();
  }


  // change settings based on what was chosen in the prefs

  public void applyFrame() {
    //editor.setExternalEditor(getBoolean("editor.external"));
    // put each of the settings into the table

    //setBoolean("sketchbook.prompt", sketchPromptBox.isSelected());
    set("sketchbook.path", sketchbookLocationField.getText());
    setBoolean("export.library", exportLibraryBox.isSelected());
    setBoolean("editor.external", externalEditorBox.isSelected());
  }


  public void showFrame(PdeEditor editor) {
    // hide the editor window so it can't be messed with
    this.editor = editor;
    editor.hide();

    // set all settings entry boxes to their actual status
    //sketchPromptBox.setSelected(getBoolean("sketchbook.prompt"));
    sketchbookLocationField.setText(get("sketchbook.path"));
    exportLibraryBox.setSelected(getBoolean("export.library"));
    externalEditorBox.setSelected(getBoolean("editor.external"));

    frame.show();
  }


  // .................................................................


  public void load(InputStream input) throws IOException {
    BufferedReader reader = 
      new BufferedReader(new InputStreamReader(input));

    //table = new Hashtable();
    String line = null;
    while ((line = reader.readLine()) != null) {
      if ((line.length() == 0) ||
          (line.charAt(0) == '#')) continue;

      // this won't properly handle = signs being in the text
      int equals = line.indexOf('=');
      if (equals != -1) {
        String key = line.substring(0, equals).trim();
        String value = line.substring(equals + 1).trim();
        table.put(key, value);
      }
    }
    reader.close();
  }


  // .................................................................


  public void save() {
    try {
      FileOutputStream output = new FileOutputStream(preferencesFile);
      PrintWriter writer = new PrintWriter(new OutputStreamWriter(output));

      Enumeration e = table.keys(); //properties.propertyNames();
      while (e.hasMoreElements()) {
        String key = (String) e.nextElement();
        writer.println(key + "=" + ((String) table.get(key)));
      }

      writer.flush();
      writer.close();

      /*
      FileOutputStream output = null;

      if ((PdeBase.platform == PdeBase.MACOSX) ||
          (PdeBase.platform == PdeBase.MACOS9)) {
        output = new FileOutputStream("lib/pde.properties");

      } else { // win95/98/ME doesn't set cwd properly
        URL url = getClass().getResource("buttons.gif");
        String urlstr = url.getFile();
        urlstr = urlstr.substring(0, urlstr.lastIndexOf("/") + 1) +
          ".properties";
#ifdef JDK13
        // the ifdef is weird, but it's set for everything but 
        // macos9, and this will never get hit 
        output = new FileOutputStream(URLDecoder.decode(urlstr));
#else
        System.err.println("bad error while writing sketch.properties");
        System.err.println("you should never see this message");
#endif
      }
      */

      /*
      //base.storePreferences();

      Properties skprops = new Properties();

      //Rectangle window = PdeBase.frame.getBounds();
      Rectangle window = editor.getBounds();
      Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();

      skprops.put("last.window.x", String.valueOf(window.x));
      skprops.put("last.window.y", String.valueOf(window.y));
      skprops.put("last.window.w", String.valueOf(window.width));
      skprops.put("last.window.h", String.valueOf(window.height));

      skprops.put("last.screen.w", String.valueOf(screen.width));
      skprops.put("last.screen.h", String.valueOf(screen.height));

      skprops.put("last.sketch.name", sketchName);
      skprops.put("last.sketch.directory", sketchDir.getAbsolutePath());
      //skprops.put("user.name", userName);

      skprops.put("last.divider.location", 
                  String.valueOf(splitPane.getDividerLocation()));

      //

      skprops.put("editor.external", externalEditor ? "true" : "false");

      //skprops.put("serial.port", PdePreferences.get("serial.port", "unspecified"));

      // save() is deprecated, and didn't properly
      // throw exceptions when it wasn't working
      skprops.store(output, "Settings for processing. " + 
                    "See lib/pde.properties for defaults.");

      // need to close the stream.. didn't do this before
      skprops.close();
      */

    } catch (IOException ex) {
      PdeBase.showWarning(null, "Error while saving the settings file", ex);
      //e.printStackTrace();
    }
  }


  // .................................................................


  // all the information from pde.properties

  //static public String get(String attribute) {
  //return get(attribute, null);
  //}

  static public String get(String attribute /*, String defaultValue */) {
    return (String) table.get(attribute);
    /*
    //String value = (properties != null) ?
    //properties.getProperty(attribute) : applet.getParameter(attribute);
    String value = properties.getProperty(attribute);

    return (value == null) ? 
      defaultValue : value;
    */
  }


  static public void set(String attribute, String value) {
    //preferences.put(attribute, value);
    table.put(attribute, value);
  }


  static public boolean getBoolean(String attribute) {
    String value = get(attribute); //, null);
    return (new Boolean(value)).booleanValue();

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


  static public void setBoolean(String attribute, boolean value) {
    set(attribute, value ? "true" : "false");
  }


  static public int getInteger(String attribute /*, int defaultValue*/) {
    return Integer.parseInt(get(attribute));

    /*
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
    */
  }


  static public void setInteger(String key, int value) {
    set(key, String.valueOf(value));
  }


  static public Color getColor(String name /*, Color otherwise*/) {
    Color parsed = null;
    String s = get(name); //, null);
    //System.out.println(name + " = " + s);
    if ((s != null) && (s.indexOf("#") == 0)) {
      try {
        int v = Integer.parseInt(s.substring(1), 16);
        parsed = new Color(v);
      } catch (Exception e) {
      }
    }
    //if (parsed == null) return otherwise;
    return parsed;
  }


  static public void setColor(String attr, Color what) {
    String r = Integer.toHexString(what.getRed());
    String g = Integer.toHexString(what.getGreen());
    String b = Integer.toHexString(what.getBlue());
    set(attr, "#" + r.substring(r.length() - 2) + 
        g.substring(g.length() - 2) + b.substring(b.length() - 2));
  }


  static public Font getFont(String which /*, Font otherwise*/) {
    //System.out.println("getting font '" + which + "'");
    String str = get(which);
    //if (str == null) return otherwise;  // ENABLE LATER
    StringTokenizer st = new StringTokenizer(str, ",");
    String fontname = st.nextToken();
    String fontstyle = st.nextToken();
    return new Font(fontname, 
                    ((fontstyle.indexOf("bold") != -1) ? Font.BOLD : 0) | 
                    ((fontstyle.indexOf("italic") != -1) ? Font.ITALIC : 0),
                    Integer.parseInt(st.nextToken()));
  }


  static public SyntaxStyle getStyle(String what /*, String dflt*/) {
    String str = get("editor." + what + ".style"); //, dflt);

    StringTokenizer st = new StringTokenizer(str, ",");

    String s = st.nextToken();
    if (s.indexOf("#") == 0) s = s.substring(1);
    Color color = new Color(Integer.parseInt(s, 16));

    s = st.nextToken();
    boolean bold = (s.indexOf("bold") != -1);
    boolean italic = (s.indexOf("italic") != -1);
    //System.out.println(what + " = " + str + " " + bold + " " + italic);

    return new SyntaxStyle(color, italic, bold);
  }
}


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

  // open the last-used sketch, etc

  //public void init() {

    //String what = path + File.separator + name + ".pde";

    ///String serialPort = skprops.getProperty("serial.port");
    //if (serialPort != null) {
    //  properties.put("serial.port", serialPort);
    //}

    //boolean ee = new Boolean(skprops.getProperty("editor.external", "false")).booleanValue();
    //editor.setExternalEditor(ee);

    ///} catch (Exception e) { 
      // this exception doesn't matter, it's just the normal course of things
      // the app reaches here when no sketch.properties file exists
      //e.printStackTrace();

      // indicator that this is the first time this feller has used p5
    //firstTime = true;

      // even if folder for 'default' user doesn't exist, or
      // sketchbook itself is missing, mkdirs() will make it happy
      //userName = "default";

      // doesn't exist, not available, make my own
      //skNew();
      //}
  //}
