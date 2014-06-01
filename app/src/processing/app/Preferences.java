/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2004-12 Ben Fry and Casey Reas
  Copyright (c) 2001-04 Massachusetts Institute of Technology

  This program is free software; you can redistribute it and/or modify
  it under the terms of the GNU General Public License version 2
  as published by the Free Software Foundation.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software Foundation,
  Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package processing.app;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.text.*;

import processing.core.*;


/**
 * Storage class for user preferences and environment settings.
 * <P>
 * This class no longer uses the Properties class, since
 * properties files are iso8859-1, which is highly likely to
 * be a problem when trying to save sketch folders and locations.
 * <p>
 * The GUI portion in here is really ugly, as it uses exact layout. This was
 * done in frustration one evening (and pre-Swing), but that's long since past,
 * and it should all be moved to a proper swing layout like BoxLayout.
 * <p>
 * This is very poorly put together, that the preferences panel and the actual
 * preferences i/o is part of the same code. But there hasn't yet been a
 * compelling reason to bother with the separation aside from concern about
 * being lectured by strangers who feel that it doesn't look like what they
 * learned in CS class.
 * <p>
 * We don't use the Java Preferences API because it would entail writing to 
 * the registry (on Windows), or an obscure file location (on Mac OS X) and 
 * make it far more difficult (impossible) to remove the preferences.txt to 
 * reset them (when they become corrupt), or to find the the file to make 
 * edits for numerous obscure preferences that are not part of the preferences
 * window. If we added a generic editor (e.g. about:config in Mozilla) for 
 * such things, we could start using the Java Preferences API. But wow, that
 * sounds like a lot of work. Not unlike writing this paragraph. 
 */
public class Preferences {

  static final Integer[] FONT_SIZES = { 10, 12, 14, 18, 24, 36, 48 }; 
  // what to call the feller

  static final String PREFS_FILE = "preferences.txt"; //$NON-NLS-1$


  // prompt text stuff

  static final String PROMPT_YES     = "Yes";
  static final String PROMPT_NO      = "No";
  static final String PROMPT_CANCEL  = "Cancel";
  static final String PROMPT_OK      = "OK";
  static final String PROMPT_BROWSE  = "Browse";

  /**
   * Standardized width for buttons. Mac OS X 10.3 wants 70 as its default,
   * Windows XP needs 66, and my Ubuntu machine needs 80+, so 80 seems proper.
   */
  static public int BUTTON_WIDTH  = 80;

  /**
   * Standardized button height. Mac OS X 10.3 (Java 1.4) wants 29,
   * presumably because it now includes the blue border, where it didn't
   * in Java 1.3. Windows XP only wants 23 (not sure what default Linux
   * would be). Because of the disparity, on Mac OS X, it will be set
   * inside a static block.
   */
  static public int BUTTON_HEIGHT = 24;
  
  /** height of the EditorHeader, EditorToolbar, and EditorStatus */
  static final int GRID_SIZE = 32;
  //static final int GRID_SIZE = 33;

  // indents and spacing standards. these probably need to be modified
  // per platform as well, since macosx is so huge, windows is smaller,
  // and linux is all over the map

  static final int GUI_BIG     = 13;
  static final int GUI_BETWEEN = 10;
  static final int GUI_SMALL   = 6;

  // gui elements

  JFrame dialog;
  int wide, high;

  JTextField sketchbookLocationField;
  JTextField presentColor;
  JTextField presentColorHex;
  JCheckBox editorAntialiasBox;
  JCheckBox deletePreviousBox;
  JCheckBox whinyBox;
  JCheckBox memoryOverrideBox;
  JTextField memoryField;
  JCheckBox checkUpdatesBox;
  //JTextField fontSizeField;
  JComboBox fontSizeField;
  JComboBox consoleSizeField;
  JCheckBox inputMethodBox;
  JCheckBox autoAssociateBox;
  
  //JRadioButton bitsThirtyTwoButton;
  //JRadioButton bitsSixtyFourButton;
  
  JComboBox displaySelectionBox;
  int displayCount;
  
  //Font[] monoFontList;
  String[] monoFontFamilies;
  JComboBox fontSelectionBox;

  /** Base object so that updates can be applied to the list of editors. */
  Base base;


  // data model

  static HashMap<String,String> defaults;
  static HashMap<String,String> table = new HashMap<String,String>();
  static File preferencesFile;


//  static public void init(String commandLinePrefs) {
  static public void init() {
    // start by loading the defaults, in case something
    // important was deleted from the user prefs
    try {
      // Name changed for 2.1b2 to avoid problems with users modifying or 
      // replacing the file after doing a search for "preferences.txt".
      load(Base.getLibStream("defaults.txt")); //$NON-NLS-1$
    } catch (Exception e) {
      Base.showError(null, "Could not read default settings.\n" +
                           "You'll need to reinstall Processing.", e);
    }

    // check for platform-specific properties in the defaults
    String platformExt = "." + PConstants.platformNames[PApplet.platform]; //$NON-NLS-1$
    int platformExtLength = platformExt.length();
//    Enumeration e = table.keys();
//    while (e.hasMoreElements()) {
//      String key = (String) e.nextElement();
//      if (key.endsWith(platformExt)) {
//        // this is a key specific to a particular platform
//        String actualKey = key.substring(0, key.length() - platformExtLength);
//        String value = get(key);
//        table.put(actualKey, value);
//      }
//    }

    // Get a list of keys that are specific to this platform
    ArrayList<String> platformKeys = new ArrayList<String>();
    for (String key : table.keySet()) {
      if (key.endsWith(platformExt)) {
        platformKeys.add(key);
      }
    }
    
    // Use those platform-specific keys to override
    for (String key : platformKeys) {
      // this is a key specific to a particular platform
      String actualKey = key.substring(0, key.length() - platformExtLength);
      String value = get(key);
      set(actualKey, value);
    }

    // clone the hash table
    defaults = (HashMap<String, String>) table.clone();

    // other things that have to be set explicitly for the defaults
    setColor("run.window.bgcolor", SystemColor.control); //$NON-NLS-1$

    // Load a prefs file if specified on the command line
//    if (commandLinePrefs != null) {
//      try {
//        load(new FileInputStream(commandLinePrefs));
//
//      } catch (Exception poe) {
//        Base.showError("Error",
//                       "Could not read preferences from " +
//                       commandLinePrefs, poe);
//      }
//    } else if (!Base.isCommandLine()) {
    // next load user preferences file
    preferencesFile = Base.getSettingsFile(PREFS_FILE);
    if (!preferencesFile.exists()) {
      // create a new preferences file if none exists
      // saves the defaults out to the file
      save();

    } else {
      // load the previous preferences file

      try {
        load(new FileInputStream(preferencesFile));

      } catch (Exception ex) {
        Base.showError("Error reading preferences",
                       "Error reading the preferences file. " +
                       "Please delete (or move)\n" +
                       preferencesFile.getAbsolutePath() +
                       " and restart Processing.", ex);
      }
//      }
    }

    PApplet.useNativeSelect = 
      Preferences.getBoolean("chooser.files.native"); //$NON-NLS-1$
    
    // Set http proxy for folks that require it. 
    // http://docs.oracle.com/javase/6/docs/technotes/guides/net/proxies.html
    String proxyHost = get("proxy.host");
    String proxyPort = get("proxy.port");
    if (proxyHost != null && proxyHost.trim().length() != 0 &&
        proxyPort != null && proxyPort.trim().length() != 0) {
      System.setProperty("http.proxyHost", proxyHost);
      System.setProperty("http.proxyPort", proxyPort);
    }
  }


  // setup dialog for the prefs
  public Preferences(Base base) {
    this.base = base;
    //dialog = new JDialog(editor, "Preferences", true);
    dialog = new JFrame("Preferences");
    dialog.setResizable(false);

//    GroupLayout layout = new GroupLayout(getContentPane());
//    dialog.getContentPane().setLayout(layout);
//    layout.setAutoCreateGaps(true);
//    layout.setAutoCreateContainerGaps(true);
    
    Container pain = dialog.getContentPane();
    pain.setLayout(null);

    int top = GUI_BIG;
    int left = GUI_BIG;
    int right = 0;

    JLabel label;
    JButton button; //, button2;
    //JComboBox combo;
    Dimension d, d2; //, d3;
    int h, vmax;


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
          File dflt = new File(sketchbookLocationField.getText());
          PApplet.selectFolder("Select new sketchbook location",
                               "sketchbookCallback", dflt,
                               Preferences.this, dialog);
//          File file =
//            Base.selectFolder("Select new sketchbook location", dflt, dialog);
//          if (file != null) {
//            sketchbookLocationField.setText(file.getAbsolutePath());
//          }
        }
      });
    pain.add(button);
    d2 = button.getPreferredSize();

    // take max height of all components to vertically align em
    vmax = Math.max(d.height, d2.height);
    sketchbookLocationField.setBounds(left, top + (vmax-d.height)/2,
                                      d.width, d.height);
    h = left + d.width + GUI_SMALL;
    button.setBounds(h, top + (vmax-d2.height)/2,
                     d2.width, d2.height);

    right = Math.max(right, h + d2.width + GUI_BIG);
    top += vmax + GUI_BETWEEN;


    // Editor and console font [ Source Code Pro ]

    // Nevermind on this for now.. Java doesn't seem to have a method for 
    // enumerating only the fixed-width (monospaced) fonts. To do this 
    // properly, we'd need to list the fonts, and compare the metrics of 
    // i and M for each. When they're identical (and not degenerate), 
    // we'd call that font fixed width. That's all a very expensive set of 
    // operations, so it should also probably be cached between runs and 
    // updated in the background.

    Container fontBox = Box.createHorizontalBox();
    JLabel fontLabel = new JLabel("Editor and Console font ");
    final String fontTip = "<html>" +
      "Select the font used in the Editor and the Console.<br/>" +
      "Only monospaced (fixed-width) fonts may be used, <br/>" +
      "though the list may be imperfect.";
    fontLabel.setToolTipText(fontTip);
    fontBox.add(fontLabel);
    // get a wide name in there before getPreferredSize() is called
    fontSelectionBox = new JComboBox(new Object[] { Toolkit.getMonoFontName() });
    fontSelectionBox.setToolTipText(fontTip);
//    fontSelectionBox.addItem(Toolkit.getMonoFont(size, style));
    //updateDisplayList();  
    fontSelectionBox.setEnabled(false);  // don't enable until fonts are loaded
    fontBox.add(fontSelectionBox);
//    fontBox.add(Box.createHorizontalGlue());
    pain.add(fontBox);
    d = fontBox.getPreferredSize();
    fontBox.setBounds(left, top, d.width + 150, d.height);
//    fontBox.setBounds(left, top, dialog.getWidth() - left*2, d.height);
    top += d.height + GUI_BETWEEN;
    
    
    // Editor font size [ 12 ]  Console font size [ 10 ]

    Container box = Box.createHorizontalBox();
    
    label = new JLabel("Editor font size: ");
    box.add(label);
    //fontSizeField = new JTextField(4);
    fontSizeField = new JComboBox<Integer>(FONT_SIZES);
    fontSizeField.setEditable(true);
    box.add(fontSizeField);
//    label = new JLabel("  (requires restart of Processing)");
//    box.add(label);
    box.add(Box.createHorizontalStrut(GUI_BETWEEN));

    label = new JLabel("Console font size: ");
    box.add(label);
    consoleSizeField = new JComboBox<Integer>(FONT_SIZES);
    consoleSizeField.setEditable(true);
    box.add(consoleSizeField);
    
    pain.add(box);
    d = box.getPreferredSize();
    box.setBounds(left, top, d.width, d.height);
//    Font editorFont = Preferences.getFont("editor.font");
    //fontSizeField.setText(String.valueOf(editorFont.getSize()));
//    fontSizeField.setSelectedItem(editorFont.getSize());
    fontSizeField.setSelectedItem(Preferences.getFont("editor.font.size"));
    top += d.height + GUI_BETWEEN;

    Container colorBox = Box.createHorizontalBox();

    label = new JLabel("Background color when Presenting: ");
    colorBox.add(label);

    final String colorTip = "<html>"
        + "Select the background color used when using Present.<br/>"
        + "Present is used to present a sketch in full-screen, <br/>"
        + "accessible from the Sketch menu.";
    label.setToolTipText(colorTip);

    presentColor = new JTextField("      ");
    presentColor.setOpaque(true);
    presentColor.setEnabled(false);
    presentColor.setBorder(BorderFactory.createLineBorder(Color.BLACK));
    presentColor.setBackground(Preferences.getColor("run.present.bgcolor"));
    presentColor.setForeground(Preferences.getColor("run.present.bgcolor"));

    presentColorHex = new JTextField(6);
    presentColorHex
        .setText(Preferences.get("run.present.bgcolor").substring(1));
    presentColorHex.getDocument().addDocumentListener(new DocumentListener() {

      @Override
      public void removeUpdate(DocumentEvent e) {
        final String colorValue = presentColorHex.getText().toUpperCase();
        if (colorValue.length() == 7 && (colorValue.startsWith("#")))
          EventQueue.invokeLater(new Runnable() {
            public void run() {
              presentColorHex.setText(colorValue.substring(1));
            }
          });
        if (colorValue.length() == 6
            && colorValue.matches("[0123456789ABCDEF]*")) {
          presentColor.setBackground(new Color(Integer.parseInt(
              colorValue.substring(0, 2), 16), Integer.parseInt(
              colorValue.substring(2, 4), 16), Integer.parseInt(
              colorValue.substring(4, 6), 16)));
          if (!colorValue.equals(presentColorHex.getText()))
            EventQueue.invokeLater(new Runnable() {
              public void run() {
                presentColorHex.setText(colorValue);
              }
            });
        }
      }

      @Override
      public void insertUpdate(DocumentEvent e) {
        final String colorValue = presentColorHex.getText().toUpperCase();
        if (colorValue.length() == 7 && (colorValue.startsWith("#")))
          EventQueue.invokeLater(new Runnable() {
            public void run() {
              presentColorHex.setText(colorValue.substring(1));
            }
          });
        if (colorValue.length() == 6
            && colorValue.matches("[0123456789ABCDEF]*")) {
          presentColor.setBackground(new Color(Integer.parseInt(
              colorValue.substring(0, 2), 16), Integer.parseInt(
              colorValue.substring(2, 4), 16), Integer.parseInt(
              colorValue.substring(4, 6), 16)));
          if (!colorValue.equals(presentColorHex.getText()))
            EventQueue.invokeLater(new Runnable() {
              public void run() {
                presentColorHex.setText(colorValue);
              }
            });
        }
      }

      @Override
      public void changedUpdate(DocumentEvent e) {
      }
    });

    final CustomColorChooser c = new CustomColorChooser(
        Preferences.getColor("run.present.bgcolor"), presentColorHex,
        presentColor);

    presentColor.addMouseListener(new MouseListener() {

      @Override
      public void mouseReleased(MouseEvent e) {
      }

      @Override
      public void mousePressed(MouseEvent e) {
      }

      @Override
      public void mouseExited(MouseEvent e) {
      }

      @Override
      public void mouseEntered(MouseEvent e) {
      }

      @Override
      public void mouseClicked(MouseEvent e) {
        c.show();
      }
    });

    presentColorHex.addInputMethodListener(new InputMethodListener() {

      @Override
      public void inputMethodTextChanged(InputMethodEvent event) {
      }

      @Override
      public void caretPositionChanged(InputMethodEvent event) {
      }
    });

    colorBox.add(presentColor);

    colorBox.add(Box.createHorizontalStrut(GUI_SMALL));

    label = new JLabel("#");
    colorBox.add(label);

    colorBox.add(presentColorHex);

    pain.add(colorBox);
    d = colorBox.getPreferredSize();
    colorBox.setBounds(left, top, d.width, d.height);

    top += d.height + GUI_BETWEEN;

    // [ ] Use smooth text in editor window

    editorAntialiasBox = new JCheckBox("Use smooth text in editor window");
//      new JCheckBox("Use smooth text in editor window " +
//                    "(requires restart of Processing)");
    pain.add(editorAntialiasBox);
    d = editorAntialiasBox.getPreferredSize();
    // adding +10 because ubuntu + jre 1.5 truncating items
    editorAntialiasBox.setBounds(left, top, d.width + 10, d.height);
    right = Math.max(right, left + d.width);
    top += d.height + GUI_BETWEEN;


    // [ ] Enable complex text input (for Japanese et al, requires restart)

    inputMethodBox =
      new JCheckBox("Enable complex text input " +
                    "(i.e. Japanese, requires restart of Processing)");
    pain.add(inputMethodBox);
    d = inputMethodBox.getPreferredSize();
    inputMethodBox.setBounds(left, top, d.width + 10, d.height);
    right = Math.max(right, left + d.width);
    top += d.height + GUI_BETWEEN;


    // [ ] Increase maximum available memory to [______] MB

    Container memoryBox = Box.createHorizontalBox();
    memoryOverrideBox = new JCheckBox("Increase maximum available memory to ");
    memoryBox.add(memoryOverrideBox);
    memoryField = new JTextField(4);
    memoryBox.add(memoryField);
    memoryBox.add(new JLabel(" MB"));
    pain.add(memoryBox);
    d = memoryBox.getPreferredSize();
    memoryBox.setBounds(left, top, d.width, d.height);
    top += d.height + GUI_BETWEEN;


//    // [ ] Use multiple .jar files when exporting applets
//
//    exportSeparateBox =
//      new JCheckBox("Use multiple .jar files when exporting applets " +
//                    "(ignored when using libraries)");
//    pain.add(exportSeparateBox);
//    d = exportSeparateBox.getPreferredSize();
//    // adding +10 because ubuntu + jre 1.5 truncating items
//    exportSeparateBox.setBounds(left, top, d.width + 10, d.height);
//    right = Math.max(right, left + d.width);
//    top += d.height + GUI_BETWEEN;


    // [ ] Delete previous application folder on export

    deletePreviousBox =
      new JCheckBox("Delete previous application folder on export");
    pain.add(deletePreviousBox);
    d = deletePreviousBox.getPreferredSize();
    deletePreviousBox.setBounds(left, top, d.width + 10, d.height);
    right = Math.max(right, left + d.width);
    top += d.height + GUI_BETWEEN;
    
    
//    // [ ] Use external editor
//
//    externalEditorBox = new JCheckBox("Use external editor");
//    pain.add(externalEditorBox);
//    d = externalEditorBox.getPreferredSize();
//    externalEditorBox.setBounds(left, top, d.width + 10, d.height);
//    right = Math.max(right, left + d.width);
//    top += d.height + GUI_BETWEEN;

    
    // [ ] Use external editor

    whinyBox = new JCheckBox("Hide tab/toolbar background image (requires restart)");
    pain.add(whinyBox);
    d = whinyBox.getPreferredSize();
    whinyBox.setBounds(left, top, d.width + 10, d.height);
    right = Math.max(right, left + d.width);
    top += d.height + GUI_BETWEEN;


    // [ ] Check for updates on startup

    checkUpdatesBox = new JCheckBox("Check for updates on startup");
    pain.add(checkUpdatesBox);
    d = checkUpdatesBox.getPreferredSize();
    checkUpdatesBox.setBounds(left, top, d.width + 10, d.height);
    right = Math.max(right, left + d.width);
    top += d.height + GUI_BETWEEN;


    // Run sketches on display [  1 ]

    Container displayBox = Box.createHorizontalBox();
    JLabel displayLabel = new JLabel("Run sketches on display ");
    final String tip = "<html>" +
      "Sets the display where sketches are initially placed.<br>" +
      "As usual, if the sketch window is moved, it will re-open<br>" +
      "at the same location, however when running in present<br>" +
      "(full screen) mode, this display will always be used.";
    displayLabel.setToolTipText(tip);
    displayBox.add(displayLabel);
    displaySelectionBox = new JComboBox();
    updateDisplayList();  // needs to happen here for getPreferredSize()
    displayBox.add(displaySelectionBox);
    pain.add(displayBox);
    d = displayBox.getPreferredSize();
    displayBox.setBounds(left, top, d.width, d.height);
    top += d.height + GUI_BETWEEN;


    // [ ] Automatically associate .pde files with Processing

    if (Base.isWindows()) {
      autoAssociateBox =
        new JCheckBox("Automatically associate .pde files with Processing");
      pain.add(autoAssociateBox);
      d = autoAssociateBox.getPreferredSize();
      autoAssociateBox.setBounds(left, top, d.width + 10, d.height);
      right = Math.max(right, left + d.width);
      top += d.height + GUI_BETWEEN;
    }


    // Launch programs as [ ] 32-bit [ ] 64-bit (Mac OS X only)

    /*
    if (Base.isMacOS()) {
      box = Box.createHorizontalBox();
      label = new JLabel("Launch programs in  ");
      box.add(label);
      bitsThirtyTwoButton = new JRadioButton("32-bit mode  ");
      box.add(bitsThirtyTwoButton);
      bitsSixtyFourButton = new JRadioButton("64-bit mode");
      box.add(bitsSixtyFourButton);

      ButtonGroup bg = new ButtonGroup();
      bg.add(bitsThirtyTwoButton);
      bg.add(bitsSixtyFourButton);

      pain.add(box);
      d = box.getPreferredSize();
      box.setBounds(left, top, d.width, d.height);
      top += d.height + GUI_BETWEEN;
    }
    */


    // More preferences are in the ...

    label = new JLabel("More preferences can be edited directly in the file");
    pain.add(label);
    d = label.getPreferredSize();
    label.setForeground(Color.gray);
    label.setBounds(left, top, d.width, d.height);
    right = Math.max(right, left + d.width);
    top += d.height; // + GUI_SMALL;

    label = new JLabel(preferencesFile.getAbsolutePath());
    final JLabel clickable = label;
    label.addMouseListener(new MouseAdapter() {
        public void mousePressed(MouseEvent e) {
          Base.openFolder(Base.getSettingsFolder());
        }

        public void mouseEntered(MouseEvent e) {
          clickable.setForeground(new Color(0, 0, 140));
        }

        public void mouseExited(MouseEvent e) {
          clickable.setForeground(Color.BLACK);
        }
      });
    pain.add(label);
    d = label.getPreferredSize();
    label.setBounds(left, top, d.width, d.height);
    right = Math.max(right, left + d.width);
    top += d.height;

    label = new JLabel("(edit only when Processing is not running)");
    pain.add(label);
    d = label.getPreferredSize();
    label.setForeground(Color.gray);
    label.setBounds(left, top, d.width, d.height);
    right = Math.max(right, left + d.width);
    top += d.height; // + GUI_SMALL;


    // [  OK  ] [ Cancel ]  maybe these should be next to the message?

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
    high = top + GUI_SMALL;


    // closing the window is same as hitting cancel button

    dialog.addWindowListener(new WindowAdapter() {
        public void windowClosing(WindowEvent e) {
          disposeFrame();
        }
      });

    ActionListener disposer = new ActionListener() {
        public void actionPerformed(ActionEvent actionEvent) {
          disposeFrame();
        }
      };
    Toolkit.registerWindowCloseKeys(dialog.getRootPane(), disposer);
    Toolkit.setIcon(dialog);

    Dimension screen = Toolkit.getScreenSize();
    dialog.setLocation((screen.width - wide) / 2,
                      (screen.height - high) / 2);

    dialog.pack(); // get insets
    Insets insets = dialog.getInsets();
    dialog.setSize(wide + insets.left + insets.right,
                  high + insets.top + insets.bottom);


    // handle window closing commands for ctrl/cmd-W or hitting ESC.

    pain.addKeyListener(new KeyAdapter() {
        public void keyPressed(KeyEvent e) {
          //System.out.println(e);
          KeyStroke wc = Toolkit.WINDOW_CLOSE_KEYSTROKE;
          if ((e.getKeyCode() == KeyEvent.VK_ESCAPE) ||
              (KeyStroke.getKeyStrokeForEvent(e).equals(wc))) {
            disposeFrame();
          }
        }
      });
  }

  private class CustomColorChooser {

    int hue, saturation, brightness; // range 360, 100, 100
    int red, green, blue; // range 256, 256, 256

    ColorRange range;
    ColorSlider slider;

    JTextField hueField, saturationField, brightnessField;
    JTextField redField, greenField, blueField;

    JTextField hexField;

    JPanel colorPanel;
    DocumentListener colorListener;

    JDialog window;

    JTextField externalHex, externalColor;

    public CustomColorChooser(Color initialColor, final JTextField externalHex,
        final JTextField externalColor) {
      Frame owner = null;
      this.externalHex = externalHex;
      this.externalColor = externalColor;
      window = new JDialog(owner, "Choose the background color for Present",
          true);
      window.getContentPane().setLayout(new BorderLayout());
      Box box = Box.createHorizontalBox();
      box.setBorder(new EmptyBorder(12, 12, 12, 12));

      range = new ColorRange();
      range.init();
      Box rangeBox = new Box(BoxLayout.Y_AXIS);
      rangeBox.setAlignmentY(0);
      rangeBox.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
      rangeBox.add(range);
      box.add(rangeBox);
      box.add(Box.createHorizontalStrut(10));

      slider = new ColorSlider();
      slider.init();
      Box sliderBox = new Box(BoxLayout.Y_AXIS);
      sliderBox.setAlignmentY(0);
      sliderBox.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
      sliderBox.add(slider);
      box.add(sliderBox);
      box.add(Box.createHorizontalStrut(10));

      box.add(createColorFields(PROMPT_OK, new ActionListener() {

        @Override
        public void actionPerformed(ActionEvent e) {
          externalHex.setText(getHexColor().substring(1));
          externalColor.setBackground(getColor());
          hide();
        }
      }, PROMPT_CANCEL, new ActionListener() {

        @Override
        public void actionPerformed(ActionEvent e) {
          hide();
        }
      }));

      box.add(Box.createHorizontalStrut(10));

      window.getContentPane().add(box, BorderLayout.CENTER);

      window.pack();
      window.setResizable(false);

      window.setLocationRelativeTo(null);

      window.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
      window.addWindowListener(new WindowAdapter() {
        public void windowClosing(WindowEvent e) {
          hide();
        }
      });
      Toolkit.registerWindowCloseKeys(window.getRootPane(),
          new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
              hide();
            }
          });

      Toolkit.setIcon(window);

      colorListener = new ColorListener();
      hueField.getDocument().addDocumentListener(colorListener);
      saturationField.getDocument().addDocumentListener(colorListener);
      brightnessField.getDocument().addDocumentListener(colorListener);
      redField.getDocument().addDocumentListener(colorListener);
      greenField.getDocument().addDocumentListener(colorListener);
      blueField.getDocument().addDocumentListener(colorListener);
      hexField.getDocument().addDocumentListener(colorListener);

      setColor(initialColor);
    }

    public void show() {
      window.setVisible(true);
      window.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
    }

    public void hide() {
      window.setVisible(false);
      setColor(new Color(Integer.parseInt(
          externalHex.getText().substring(0, 2), 16), Integer.parseInt(
          externalHex.getText().substring(2, 4), 16), Integer.parseInt(
          externalHex.getText().substring(4, 6), 16)));
    }

    public Color getColor() {
      return new Color(red, green, blue);
    }

    public void setColor(Color color) {
      updateRGB(color.getRGB());
    }

    public String getHexColor() {
      return "#" + PApplet.hex(red, 2) + PApplet.hex(green, 2)
          + PApplet.hex(blue, 2);
    }

    public class ColorListener implements DocumentListener {

      public void changedUpdate(DocumentEvent e) {
      }

      public void removeUpdate(DocumentEvent e) {
      }

      boolean updating;

      public void insertUpdate(DocumentEvent e) {
        if (updating)
          return; // don't update forever recursively
        updating = true;

        Document doc = e.getDocument();
        if (doc == hueField.getDocument()) {
          hue = bounded(hue, hueField, 359);
          updateRGB();
          updateHex();

        } else if (doc == saturationField.getDocument()) {
          saturation = bounded(saturation, saturationField, 99);
          updateRGB();
          updateHex();

        } else if (doc == brightnessField.getDocument()) {
          brightness = bounded(brightness, brightnessField, 99);
          updateRGB();
          updateHex();

        } else if (doc == redField.getDocument()) {
          red = bounded(red, redField, 255);
          updateHSB();
          updateHex();

        } else if (doc == greenField.getDocument()) {
          green = bounded(green, greenField, 255);
          updateHSB();
          updateHex();

        } else if (doc == blueField.getDocument()) {
          blue = bounded(blue, blueField, 255);
          updateHSB();
          updateHex();

        } else if (doc == hexField.getDocument()) {
          String str = hexField.getText();
          if (str.startsWith("#")) {
            str = str.substring(1);
          }
          while (str.length() < 6) {
            str += "0";
          }
          if (str.length() > 6) {
            str = str.substring(0, 6);
          }
          updateRGB(Integer.parseInt(str, 16));
          updateHSB();
        }
        range.redraw();
        slider.redraw();
        colorPanel.repaint();
        updating = false;
      }
    }

    /**
     * Set the RGB values based on the current HSB values.
     */
    protected void updateRGB() {
      updateRGB(Color.HSBtoRGB(hue / 359f, saturation / 99f, brightness / 99f));
    }

    /**
     * Set the RGB values based on a calculated ARGB int. Used by both
     * updateRGB() to set the color from the HSB values, and by updateHex(), to
     * unpack the hex colors and assign them.
     */
    protected void updateRGB(int rgb) {
      red = (rgb >> 16) & 0xff;
      green = (rgb >> 8) & 0xff;
      blue = rgb & 0xff;
      redField.setText(String.valueOf(red));
      greenField.setText(String.valueOf(green));
      blueField.setText(String.valueOf(blue));
    }

    /**
     * Set the HSB values based on the current RGB values.
     */
    protected void updateHSB() {
      float hsb[] = new float[3];
      Color.RGBtoHSB(red, green, blue, hsb);
      hue = (int) (hsb[0] * 359.0f);
      saturation = (int) (hsb[1] * 99.0f);
      brightness = (int) (hsb[2] * 99.0f);

      hueField.setText(String.valueOf(hue));
      saturationField.setText(String.valueOf(saturation));
      brightnessField.setText(String.valueOf(brightness));
    }

    protected void updateHex() {
      hexField.setText(getHexColor());
    }

    /**
     * Get the bounded value for a specific range. If the value is outside the
     * max, you can't edit right away, so just act as if it's already been
     * bounded and return the bounded value, then fire an event to set it to the
     * value that was just returned.
     */
    protected int bounded(int current, final JTextField field, final int max) {
      String text = field.getText();
      if (text.length() == 0) {
        return 0;
      }
      try {
        int value = Integer.parseInt(text);
        if (value > max) {
          SwingUtilities.invokeLater(new Runnable() {
            public void run() {
              field.setText(String.valueOf(max));
            }
          });
          return max;
        }
        return value;
      } catch (NumberFormatException e) {
        return current; // should not be reachable
      }
    }

    protected Container createColorFields(String buttonName,
        ActionListener buttonListener, String buttonName2,
        ActionListener buttonListener2) {
      Box box = Box.createVerticalBox();
      box.setAlignmentY(0);

      final int GAP = Base.isWindows() ? 5 : 0;
      final int BETWEEN = Base.isWindows() ? 8 : 6; // 10;

      Box row;

      row = Box.createHorizontalBox();
      if (Base.isMacOS()) {
        row.add(Box.createHorizontalStrut(17));
      } else {
        row.add(createFixedLabel(""));
      }
      // Can't just set the bg color of the panel because it also tints the
      // bevel
      // (on OS X), which looks odd. So instead we override paintComponent().
      colorPanel = new JPanel() {
        public void paintComponent(Graphics g) {
          g.setColor(new Color(red, green, blue));
          Dimension size = getSize();
          g.fillRect(0, 0, size.width, size.height);
        }
      };
      colorPanel
          .setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
      Dimension dim = new Dimension(70, 25);
      colorPanel.setMinimumSize(dim);
      colorPanel.setMaximumSize(dim);
      colorPanel.setPreferredSize(dim);
      row.add(colorPanel);
      row.add(Box.createHorizontalGlue());
      box.add(row);
      box.add(Box.createVerticalStrut(BETWEEN));

      row = Box.createHorizontalBox();
      row.add(createFixedLabel("H"));
      row.add(hueField = new NumberField(4, false));
      row.add(new JLabel(" \u00B0")); // degree symbol
      row.add(Box.createHorizontalGlue());
      box.add(row);
      box.add(Box.createVerticalStrut(GAP));

      row = Box.createHorizontalBox();
      row.add(createFixedLabel("S"));
      row.add(saturationField = new NumberField(4, false));
      row.add(new JLabel(" %"));
      row.add(Box.createHorizontalGlue());
      box.add(row);
      box.add(Box.createVerticalStrut(GAP));

      row = Box.createHorizontalBox();
      row.add(createFixedLabel("B"));
      row.add(brightnessField = new NumberField(4, false));
      row.add(new JLabel(" %"));
      row.add(Box.createHorizontalGlue());
      box.add(row);
      box.add(Box.createVerticalStrut(BETWEEN));

      row = Box.createHorizontalBox();
      row.add(createFixedLabel("R"));
      row.add(redField = new NumberField(4, false));
      row.add(Box.createHorizontalGlue());
      box.add(row);
      box.add(Box.createVerticalStrut(GAP));

      row = Box.createHorizontalBox();
      row.add(createFixedLabel("G"));
      row.add(greenField = new NumberField(4, false));
      row.add(Box.createHorizontalGlue());
      box.add(row);
      box.add(Box.createVerticalStrut(GAP));

      row = Box.createHorizontalBox();
      row.add(createFixedLabel("B"));
      row.add(blueField = new NumberField(4, false));
      row.add(Box.createHorizontalGlue());
      box.add(row);
      box.add(Box.createVerticalStrut(BETWEEN));

      row = Box.createHorizontalBox();
      row.add(createFixedLabel(""));
      // Windows needs extra space, OS X and Linux do not
      // Mac OS X needs 6 because #CCCCCC is quite wide
      final int hexCount = Base.isWindows() ? 7 : 6;
      row.add(hexField = new NumberField(hexCount, true));
      row.add(Box.createHorizontalGlue());
      box.add(row);
      box.add(Box.createVerticalStrut(GAP));

      row = Box.createHorizontalBox();
      if (Base.isMacOS()) {
        row.add(Box.createHorizontalStrut(11));
      } else {
        row.add(createFixedLabel(""));
      }
      JButton button = new JButton(buttonName);
      button.addActionListener(buttonListener);

      row.add(button);
      row.add(Box.createHorizontalGlue());
      box.add(row);

      row = Box.createHorizontalBox();
      if (Base.isMacOS()) {
        row.add(Box.createHorizontalStrut(11));
      } else {
        row.add(createFixedLabel(""));
      }
      JButton button2 = new JButton(buttonName2);
      button2.addActionListener(buttonListener2);

      row.add(button2);
      row.add(Box.createHorizontalGlue());
      box.add(row);

      box.add(Box.createVerticalGlue());
      return box;
    }

    int labelH;

    /**
     * return a label of a fixed width
     */
    protected JLabel createFixedLabel(String title) {
      JLabel label = new JLabel(title);
      if (labelH == 0) {
        labelH = label.getPreferredSize().height;
      }
      Dimension dim = new Dimension(15, labelH);
      label.setPreferredSize(dim);
      label.setMinimumSize(dim);
      label.setMaximumSize(dim);
      return label;
    }

    public class ColorRange extends PApplet {

      static final int WIDE = 256;
      static final int HIGH = 256;

      int lastX, lastY;

      public void setup() {
        size(WIDE, HIGH);
        noLoop();

        colorMode(HSB, 360, 256, 256);
        noFill();
        rectMode(CENTER);

        loadPixels();
      }

      public void draw() {

        if ((width != WIDE) || (height < HIGH)) {
          return;
        }

        int index = 0;
        for (int j = 0; j < 256; j++) {
          for (int i = 0; i < 256; i++) {
            pixels[index++] = color(hue, i, 255 - j);
          }
        }

        updatePixels();
        stroke((brightness > 50) ? 0 : 255);
        rect(lastX, lastY, 9, 9);
      }

      public void mousePressed() {
        updateMouse();
      }

      public void mouseDragged() {
        updateMouse();
      }

      public void updateMouse() {
        if ((mouseX >= 0) && (mouseX < 256) && (mouseY >= 0) && (mouseY < 256)) {
          int nsaturation = (int) (100 * (mouseX / 255.0f));
          int nbrightness = 100 - ((int) (100 * (mouseY / 255.0f)));
          saturationField.setText(String.valueOf(nsaturation));
          brightnessField.setText(String.valueOf(nbrightness));

          lastX = mouseX;
          lastY = mouseY;
        }
      }

      public Dimension getPreferredSize() {
        return new Dimension(WIDE, HIGH);
      }

      public Dimension getMinimumSize() {
        return new Dimension(WIDE, HIGH);
      }

      public Dimension getMaximumSize() {
        return new Dimension(WIDE, HIGH);
      }

      public void keyPressed() {
        if (key == ESC) {
          CustomColorChooser.this.hide();
          // don't quit out of processing
          // http://dev.processing.org/bugs/show_bug.cgi?id=1006
          key = 0;
        }
      }
    }

    public class ColorSlider extends PApplet {

      static final int WIDE = 20;
      static final int HIGH = 256;

      public void setup() {
        size(WIDE, HIGH); // , P3D);
        colorMode(HSB, 255, 100, 100);
        noLoop();
        loadPixels();
      }

      public void draw() {
        if ((width != WIDE) || (height < HIGH)) {
          return;
        }

        int index = 0;
        int sel = 255 - (int) (255 * (hue / 359f));
        for (int j = 0; j < 256; j++) {
          int c = color(255 - j, 100, 100);
          if (j == sel)
            c = 0xFF000000;
          for (int i = 0; i < WIDE; i++) {
            g.pixels[index++] = c;
          }
        }
        updatePixels();
      }

      public void mousePressed() {
        updateMouse();
      }

      public void mouseDragged() {
        updateMouse();
      }

      public void updateMouse() {
        if ((mouseX >= 0) && (mouseX < 256) && (mouseY >= 0) && (mouseY < 256)) {
          int nhue = 359 - (int) (359 * (mouseY / 255.0f));
          hueField.setText(String.valueOf(nhue));
        }
      }

      public Dimension getPreferredSize() {
        return new Dimension(WIDE, HIGH);
      }

      public Dimension getMinimumSize() {
        return new Dimension(WIDE, HIGH);
      }

      public Dimension getMaximumSize() {
        return new Dimension(WIDE, HIGH);
      }

      public void keyPressed() {
        if (key == ESC) {
          CustomColorChooser.this.hide();
          // don't quit out of processing
          // http://dev.processing.org/bugs/show_bug.cgi?id=1006
          key = 0;
        }
      }
    }

    /**
     * Extension of JTextField that only allows numbers
     */
    class NumberField extends JTextField {

      public boolean allowHex;

      public NumberField(int cols, boolean allowHex) {
        super(cols);
        this.allowHex = allowHex;
      }

      protected Document createDefaultModel() {
        return new NumberDocument(this);
      }

      public Dimension getPreferredSize() {
        if (!allowHex) {
          return new Dimension(45, super.getPreferredSize().height);
        }
        return super.getPreferredSize();
      }

      public Dimension getMinimumSize() {
        return getPreferredSize();
      }

      public Dimension getMaximumSize() {
        return getPreferredSize();
      }
    }

    /**
     * Document model to go with JTextField that only allows numbers.
     */
    class NumberDocument extends PlainDocument {

      NumberField parentField;

      public NumberDocument(NumberField parentField) {
        this.parentField = parentField;
      }

      public void insertString(int offs, String str, AttributeSet a)
          throws BadLocationException {

        if (str == null)
          return;

        char chars[] = str.toCharArray();
        int charCount = 0;
        // remove any non-digit chars
        for (int i = 0; i < chars.length; i++) {
          boolean ok = Character.isDigit(chars[i]);
          if (parentField.allowHex) {
            if ((chars[i] >= 'A') && (chars[i] <= 'F'))
              ok = true;
            if ((chars[i] >= 'a') && (chars[i] <= 'f'))
              ok = true;
            if ((offs == 0) && (i == 0) && (chars[i] == '#'))
              ok = true;
          }
          if (ok) {
            if (charCount != i) { // shift if necessary
              chars[charCount] = chars[i];
            }
            charCount++;
          }
        }
        super.insertString(offs, new String(chars, 0, charCount), a);
      }
    }

  }

  public void sketchbookCallback(File file) {
    if (file != null) {
      sketchbookLocationField.setText(file.getAbsolutePath());
    }
  }


  public Dimension getPreferredSize() {
    return new Dimension(wide, high);
  }


  // .................................................................


  /**
   * Close the window after an OK or Cancel.
   */
  protected void disposeFrame() {
    dialog.dispose();
  }


  /**
   * Change internal settings based on what was chosen in the prefs,
   * then send a message to the editor saying that it's time to do the same.
   */
  protected void applyFrame() {
    setBoolean("editor.smooth", //$NON-NLS-1$
               editorAntialiasBox.isSelected());

    setBoolean("export.delete_target_folder", //$NON-NLS-1$
               deletePreviousBox.isSelected());

    boolean wine = whinyBox.isSelected();
    setBoolean("header.hide.image", wine); //$NON-NLS-1$
    setBoolean("buttons.hide.image", wine); //$NON-NLS-1$
    // Could iterate through editors here and repaint them all, but probably 
    // requires a doLayout() call, and that may have different effects on
    // each platform, and nobody wants to debug/support that.

    // if the sketchbook path has changed, rebuild the menus
    String oldPath = get("sketchbook.path"); //$NON-NLS-1$
    String newPath = sketchbookLocationField.getText();
    if (!newPath.equals(oldPath)) {
      base.setSketchbookFolder(new File(newPath));
    }

//    setBoolean("editor.external", externalEditorBox.isSelected());
    setBoolean("update.check", checkUpdatesBox.isSelected()); //$NON-NLS-1$

    int oldDisplayIndex = getInteger("run.display"); //$NON-NLS-1$
    int displayIndex = 0;
    for (int d = 0; d < displaySelectionBox.getItemCount(); d++) {
      if (displaySelectionBox.getSelectedIndex() == d) {
        displayIndex = d;
      }
    }
    if (oldDisplayIndex != displayIndex) {
      setInteger("run.display", displayIndex); //$NON-NLS-1$
      for (Editor editor : base.getEditors()) {
        editor.setSketchLocation(null);
      }
    }

    setBoolean("run.options.memory", memoryOverrideBox.isSelected()); //$NON-NLS-1$
    int memoryMin = Preferences.getInteger("run.options.memory.initial"); //$NON-NLS-1$
    int memoryMax = Preferences.getInteger("run.options.memory.maximum"); //$NON-NLS-1$
    try {
      memoryMax = Integer.parseInt(memoryField.getText().trim());
      // make sure memory setting isn't too small
      if (memoryMax < memoryMin) memoryMax = memoryMin;
      setInteger("run.options.memory.maximum", memoryMax); //$NON-NLS-1$
    } catch (NumberFormatException e) {
      System.err.println("Ignoring bad memory setting");
    }

    /*
      // was gonna use this to check memory settings,
      // but it quickly gets much too messy
    if (getBoolean("run.options.memory")) {
      Process process = Runtime.getRuntime().exec(new String[] {
          "java", "-Xms" + memoryMin + "m", "-Xmx" + memoryMax + "m"
        });
      processInput = new SystemOutSiphon(process.getInputStream());
      processError = new MessageSiphon(process.getErrorStream(), this);
    }
    */

    /*
    // If a change has been made between 32- and 64-bit, the libraries need
    // to be reloaded so that their native paths are set correctly.
    if (Base.isMacOS()) {
      String oldBits = get("run.options.bits"); //$NON-NLS-1$
      String newBits = bitsThirtyTwoButton.isSelected() ? "32" : "64"; //$NON-NLS-1$ //$NON-NLS-2$
      if (!oldBits.equals(newBits)) {
        set("run.options.bits", newBits); //$NON-NLS-1$
        for (Mode m : base.getModeList()) {
          m.rebuildLibraryList();
        }
      }
    }
    */

    // Don't change anything if the user closes the window before fonts load
    if (fontSelectionBox.isEnabled()) {
      String fontFamily = (String) fontSelectionBox.getSelectedItem();
      set("editor.font.family", fontFamily);
    }

    /*
    String newSizeText = fontSizeField.getText();
    try {
      int newSize = Integer.parseInt(newSizeText.trim());
      //String pieces[] = PApplet.split(get("editor.font"), ','); //$NON-NLS-1$
      //pieces[2] = String.valueOf(newSize);
      //set("editor.font", PApplet.join(pieces, ',')); //$NON-NLS-1$
      set("editor.font.size", String.valueOf(newSize));

    } catch (Exception e) {
      Base.log("Ignoring invalid font size " + newSizeText); //$NON-NLS-1$
    }
    */
    try {
      Object selection = fontSizeField.getSelectedItem();
      if (selection instanceof String) {
        // Replace with Integer version
        selection = Integer.parseInt((String) selection);
      }
      set("editor.font.size", String.valueOf(selection));

    } catch (NumberFormatException e) {
      Base.log("Ignoring invalid font size " + fontSizeField); //$NON-NLS-1$
      fontSizeField.setSelectedItem(getInteger("editor.font.size"));
    }
    
    try {
      Object selection = consoleSizeField.getSelectedItem();
      if (selection instanceof String) {
        // Replace with Integer version
        selection = Integer.parseInt((String) selection);
      }
      set("console.font.size", String.valueOf(selection));

    } catch (NumberFormatException e) {
      Base.log("Ignoring invalid font size " + consoleSizeField); //$NON-NLS-1$
      consoleSizeField.setSelectedItem(getInteger("console.font.size"));
    }
    
    setColor("run.present.bgcolor", presentColor.getBackground());
    
    setBoolean("editor.input_method_support", inputMethodBox.isSelected()); //$NON-NLS-1$

    if (autoAssociateBox != null) {
      setBoolean("platform.auto_file_type_associations", //$NON-NLS-1$
                 autoAssociateBox.isSelected());
    }

    for (Editor editor : base.getEditors()) {
      editor.applyPreferences();
    }
  }


  protected void showFrame() {
    editorAntialiasBox.setSelected(getBoolean("editor.smooth")); //$NON-NLS-1$
    inputMethodBox.setSelected(getBoolean("editor.input_method_support")); //$NON-NLS-1$

    // set all settings entry boxes to their actual status
//    exportSeparateBox.
//      setSelected(getBoolean("export.applet.separate_jar_files"));
    deletePreviousBox.
      setSelected(getBoolean("export.delete_target_folder")); //$NON-NLS-1$

    //closingLastQuitsBox.
    //  setSelected(getBoolean("sketchbook.closing_last_window_quits"));
    //sketchPromptBox.
    //  setSelected(getBoolean("sketchbook.prompt"));
    //sketchCleanBox.
    //  setSelected(getBoolean("sketchbook.auto_clean"));

    sketchbookLocationField.
      setText(get("sketchbook.path")); //$NON-NLS-1$
//    externalEditorBox.
//      setSelected(getBoolean("editor.external"));
    checkUpdatesBox.
      setSelected(getBoolean("update.check")); //$NON-NLS-1$

    whinyBox.setSelected(getBoolean("header.hide.image") || //$NON-NLS-1$
                         getBoolean("buttons.hide.image")); //$NON-NLS-1$

    updateDisplayList();
    int displayNum = getInteger("run.display"); //$NON-NLS-1$
//    System.out.println("display is " + displayNum + ", d count is " + displayCount);
    if (displayNum >= 0 && displayNum < displayCount) {
//      System.out.println("setting num to " + displayNum);
      displaySelectionBox.setSelectedIndex(displayNum);
    }
    
    // This takes a while to load, so run it from a separate thread
    new Thread(new Runnable() {
      public void run() {
        initFontList();
      }
    }).start();
    
    fontSizeField.setSelectedItem(getInteger("editor.font.size"));
    consoleSizeField.setSelectedItem(getInteger("console.font.size"));

    presentColor.setBackground(Preferences.getColor("run.present.bgcolor"));
    presentColorHex
        .setText(Preferences.get("run.present.bgcolor").substring(1));

    /*
    if (Base.isMacOS()) {
      String bits = Preferences.get("run.options.bits"); //$NON-NLS-1$
      if (bits.equals("32")) { //$NON-NLS-1$
        bitsThirtyTwoButton.setSelected(true);
      } else if (bits.equals("64")) { //$NON-NLS-1$
        bitsSixtyFourButton.setSelected(true);
      }
      // in case we go back and support OS X 10.5...
      if (System.getProperty("os.version").startsWith("10.5")) { //$NON-NLS-1$ //$NON-NLS-2$
        bitsSixtyFourButton.setSelected(true);
        bitsThirtyTwoButton.setEnabled(false);
      }
    }
    */

    if (autoAssociateBox != null) {
      autoAssociateBox.
        setSelected(getBoolean("platform.auto_file_type_associations")); //$NON-NLS-1$
    }

    dialog.setVisible(true);
  }


  /** 
   * I have some ideas on how we could make Swing even more obtuse for the
   * most basic usage scenarios. Is there someone on the team I can contact?
   * Oracle, are you listening?
   */
  class FontNamer extends JLabel implements ListCellRenderer<Font> {
    public Component getListCellRendererComponent(JList<? extends Font> list,
                                                  Font value, int index,
                                                  boolean isSelected,
                                                  boolean cellHasFocus) {
      //if (Base.isMacOS()) {
      setText(value.getFamily() + " / " + value.getName() + " (" + value.getPSName() + ")");
      return this;
    }
  }
  

  void initFontList() {
    /*
    if (monoFontList == null) {
      monoFontList = Toolkit.getMonoFontList().toArray(new Font[0]);
      fontSelectionBox.setModel(new DefaultComboBoxModel(monoFontList));
      fontSelectionBox.setRenderer(new FontNamer());
      
      // Preferred size just makes it extend to the container
      //fontSelectionBox.setSize(fontSelectionBox.getPreferredSize());
      // Minimum size is better, but cuts things off (on OS X), so we add 20
      //Dimension minSize = fontSelectionBox.getMinimumSize();
      //Dimension minSize = fontSelectionBox.getPreferredSize();
      //fontSelectionBox.setSize(minSize.width + 20, minSize.height);
      fontSelectionBox.setEnabled(true);
    }
    */
    if (monoFontFamilies == null) {
      monoFontFamilies = Toolkit.getMonoFontFamilies();
      fontSelectionBox.setModel(new DefaultComboBoxModel(monoFontFamilies));
      String family = get("editor.font.family");
//      System.out.println("family is " + family);
//      System.out.println("font sel items = " + fontSelectionBox.getItemCount());
//      for (int i = 0; i < fontSelectionBox.getItemCount(); i++) {
//        String item = (String) fontSelectionBox.getItemAt(i);
//        if (fontSelectionBox.getItemAt(i) == family) {
//          System.out.println("found at index " + i);
//        } else if (item.equals(family)) {
//          System.out.println("equals at index " + i);
//        } else {
//          System.out.println("nothing doing: " + item);
//        }
//      }
      // Set a reasonable default, in case selecting the family fails 
      fontSelectionBox.setSelectedItem("Monospaced");
      fontSelectionBox.setSelectedItem(family);
      fontSelectionBox.setEnabled(true);
    }
  }
  
  
  void updateDisplayList() {
    GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
    displayCount = ge.getScreenDevices().length;
//    displaySelectionBox.removeAll();
    String[] items = new String[displayCount];
    for (int i = 0; i < displayCount; i++) {
      items[i] = String.valueOf(i + 1);
//      displaySelectionBox.add(String.valueOf(i + 1));
    }
//    PApplet.println(items);
    displaySelectionBox.setModel(new DefaultComboBoxModel(items));
//    displaySelectionBox = new JComboBox(items);
  }


  // Workaround for Apple bullsh*t caused by their not releasing a 32-bit
  // version of Java for Mac OS X 10.5.
//  static public String checkBits() {
//    String bits = Preferences.get("run.options.bits");
//    if (bits == null) {
//      if (System.getProperty("os.version").startsWith("10.5")) {
//        bits = "64";
//      } else {
//        bits = "32";
//      }
//      Preferences.set("run.options.bits", bits);
//    }
//    return bits;
//  }


  // .................................................................


  static public void load(InputStream input) throws IOException {
    String[] lines = PApplet.loadStrings(input);  // Reads as UTF-8
    for (String line : lines) {
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
  }


  // .................................................................


  static protected void save() {
//    try {
    // on startup, don't worry about it
    // this is trying to update the prefs for who is open
    // before Preferences.init() has been called.
    if (preferencesFile == null) return;

    // Fix for 0163 to properly use Unicode when writing preferences.txt
    PrintWriter writer = PApplet.createWriter(preferencesFile);

//    Enumeration e = table.keys(); //properties.propertyNames();
//    while (e.hasMoreElements()) {
//      String key = (String) e.nextElement();
//      writer.println(key + "=" + ((String) table.get(key)));
//    }
    String[] keyList = table.keySet().toArray(new String[table.size()]);
    keyList = PApplet.sort(keyList);
    for (String key : keyList) {
      writer.println(key + "=" + table.get(key)); //$NON-NLS-1$
    }

    writer.flush();
    writer.close();

//    } catch (Exception ex) {
//      Base.showWarning(null, "Error while saving the settings file", ex);
//    }
  }


  // .................................................................


  // all the information from preferences.txt

  //static public String get(String attribute) {
  //return get(attribute, null);
  //}

  static public String get(String attribute /*, String defaultValue */) {
    return table.get(attribute);
    /*
    //String value = (properties != null) ?
    //properties.getProperty(attribute) : applet.getParameter(attribute);
    String value = properties.getProperty(attribute);

    return (value == null) ?
      defaultValue : value;
    */
  }


  static public String getDefault(String attribute) {
    return defaults.get(attribute);
  }


  static public void set(String attribute, String value) {
    table.put(attribute, value);
  }


  static public void unset(String attribute) {
    table.remove(attribute);
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
    set(attribute, value ? "true" : "false"); //$NON-NLS-1$ //$NON-NLS-2$
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


  static public Color getColor(String name) {
    Color parsed = Color.GRAY;  // set a default
    String s = get(name);
    if ((s != null) && (s.indexOf("#") == 0)) { //$NON-NLS-1$
      try {
        parsed = new Color(Integer.parseInt(s.substring(1), 16));
      } catch (Exception e) { }
    }
    return parsed;
  }


  static public void setColor(String attr, Color what) {
    set(attr, "#" + PApplet.hex(what.getRGB() & 0xffffff, 6)); //$NON-NLS-1$
  }


  // Identical version found in Settings.java
  static public Font getFont(String attr) {
    try {
      boolean replace = false;
      String value = get(attr);
      if (value == null) {
        // use the default font instead
        value = getDefault(attr);
        replace = true;
      }

      String[] pieces = PApplet.split(value, ',');

      if (pieces.length != 3) {
        value = getDefault(attr);
        pieces = PApplet.split(value, ',');
        replace = true;
      }

      String name = pieces[0];
      int style = Font.PLAIN;  // equals zero
      if (pieces[1].indexOf("bold") != -1) { //$NON-NLS-1$
        style |= Font.BOLD;
      }
      if (pieces[1].indexOf("italic") != -1) { //$NON-NLS-1$
        style |= Font.ITALIC;
      }
      int size = PApplet.parseInt(pieces[2], 12);
      
      // replace bad font with the default from lib/preferences.txt
      if (replace) {
        set(attr, value);
      }

      if (!name.startsWith("processing.")) {
        return new Font(name, style, size);

      } else {
        if (pieces[0].equals("processing.sans")) {
          return Toolkit.getSansFont(size, style);
        } else if (pieces[0].equals("processing.mono")) {
          return Toolkit.getMonoFont(size, style);
        }
      }

    } catch (Exception e) {
      // Adding try/catch block because this may be where 
      // a lot of startup crashes are happening. 
      Base.log("Error with font " + get(attr) + " for attribute " + attr); 
    }
    return new Font("Dialog", Font.PLAIN, 12);
  }


  /*
  static public SyntaxStyle getStyle(String what) {
    String str = get("editor." + what + ".style"); //, dflt); //$NON-NLS-1$ //$NON-NLS-2$

    StringTokenizer st = new StringTokenizer(str, ","); //$NON-NLS-1$

    String s = st.nextToken();
    if (s.indexOf("#") == 0) s = s.substring(1); //$NON-NLS-1$
    Color color = Color.DARK_GRAY;
    try {
      color = new Color(Integer.parseInt(s, 16));
    } catch (Exception e) { }

    s = st.nextToken();
    boolean bold = (s.indexOf("bold") != -1); //$NON-NLS-1$
//    boolean italic = (s.indexOf("italic") != -1); //$NON-NLS-1$
    //System.out.println(what + " = " + str + " " + bold + " " + italic);

//    return new SyntaxStyle(color, italic, bold);
    return new SyntaxStyle(color, bold);
  }
  */
}
