/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2004-14 Ben Fry and Casey Reas
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

import processing.app.ColorChooser;
import processing.app.Language;
import processing.core.*;

import static processing.app.Preferences.*;


/**
 * Preferences frame class.
 * <p>
 * The GUI here is really ugly, as it uses exact layout. This was
 * done in frustration one evening (and pre-Swing), but that's long since past,
 * and it should all be moved to a proper swing layout like BoxLayout.
 * <p>
 * This is very poorly put together, that the preferences panel and the actual
 * preferences i/o is part of the same code. But there hasn't yet been a
 * compelling reason to bother with the separation aside from concern about
 * being lectured by strangers who feel that it doesn't look like what they
 * learned in CS class.
 */
public class PreferencesFrame {

  static final Integer[] FONT_SIZES = { 10, 12, 14, 18, 24, 36, 48 };

  // prompt text stuff

  static final String PROMPT_YES     = Language.text("prompt.yes");
  static final String PROMPT_NO      = Language.text("prompt.no");
  static final String PROMPT_CANCEL  = Language.text("prompt.cancel");
  static final String PROMPT_OK      = Language.text("prompt.ok");
  static final String PROMPT_BROWSE  = Language.text("prompt.browse");

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
  JComboBox fontSizeField;
  JComboBox consoleSizeField;
  JCheckBox inputMethodBox;
  JCheckBox autoAssociateBox;

  ColorChooser selector;

  JCheckBox errorCheckerBox;
  JCheckBox warningsCheckerBox;
  JCheckBox codeCompletionBox;
  JCheckBox importSuggestionsBox;
  JCheckBox codeCompletionTriggerBox;

  JComboBox displaySelectionBox;
  JComboBox languageSelectionBox;

  int displayCount;

  String[] monoFontFamilies;
  JComboBox fontSelectionBox;

  /** Base object so that updates can be applied to the list of editors. */
  Base base;


  // setup dialog for the prefs
  public PreferencesFrame(Base base) {
    this.base = base;
    dialog = new JFrame(Language.text("preferences"));
    dialog.setResizable(false);

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

    label = new JLabel(Language.text("preferences.sketchbook_location")+":");
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
          PApplet.selectFolder(Language.text("preferences.sketchbook_location.popup"),
                               "sketchbookCallback", dflt,
                               PreferencesFrame.this, dialog);
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


    // Language: [ English ] (requires restart of Processing)

    Container languageBox = Box.createHorizontalBox();
    JLabel languageLabel = new JLabel(Language.text("preferences.language")+": ");
    languageBox.add(languageLabel);
    languageSelectionBox = new JComboBox();

    Map<String, String> languages = Language.getLanguages();
    String[] languageSelection = new String[languages.size()];
    languageSelection[0] = languages.get(Language.getLanguage());
    int i = 1;
    for (Map.Entry<String, String> lang : languages.entrySet()) {
      if(!lang.getKey().equals(Language.getLanguage())){
        languageSelection[i++] = lang.getValue();
      }
    }
    languageSelectionBox.setModel(new DefaultComboBoxModel(languageSelection));
    languageBox.add(languageSelectionBox);
    label = new JLabel(" ("+Language.text("preferences.requires_restart")+")");
    languageBox.add(label);
    pain.add(languageBox);
    d = languageBox.getPreferredSize();
    languageBox.setBounds(left, top, d.width, d.height);
    top += d.height + GUI_BETWEEN;


    // Editor and console font [ Source Code Pro ]

    // Nevermind on this for now.. Java doesn't seem to have a method for
    // enumerating only the fixed-width (monospaced) fonts. To do this
    // properly, we'd need to list the fonts, and compare the metrics of
    // i and M for each. When they're identical (and not degenerate),
    // we'd call that font fixed width. That's all a very expensive set of
    // operations, so it should also probably be cached between runs and
    // updated in the background.

    Container fontBox = Box.createHorizontalBox();
    JLabel fontLabel = new JLabel(Language.text("preferences.editor_and_console_font")+": ");
    final String fontTip = "<html>" +
      "Select the font used in the Editor and the Console.<br/>" +
      "Only monospaced (fixed-width) fonts may be used, <br/>" +
      "though the list may be imperfect.";
    fontLabel.setToolTipText(fontTip);
    fontBox.add(fontLabel);
    // get a wide name in there before getPreferredSize() is called
    fontSelectionBox = new JComboBox(new Object[] { Toolkit.getMonoFontName() });
    fontSelectionBox.setToolTipText(fontTip);
    fontSelectionBox.setEnabled(false);  // don't enable until fonts are loaded
    fontBox.add(fontSelectionBox);
    pain.add(fontBox);
    d = fontBox.getPreferredSize();
    fontBox.setBounds(left, top, d.width + 150, d.height);
    top += d.height + GUI_BETWEEN;


    // Editor font size [ 12 ]  Console font size [ 10 ]

    Container box = Box.createHorizontalBox();
    label = new JLabel(Language.text("preferences.editor_font_size")+": ");
    box.add(label);
    fontSizeField = new JComboBox<Integer>(FONT_SIZES);
//    fontSizeField = new JComboBox<Integer>(FONT_SIZES);
    fontSizeField.setEditable(true);
    box.add(fontSizeField);
    box.add(Box.createHorizontalStrut(GUI_BETWEEN));

    label = new JLabel(Language.text("preferences.console_font_size")+": ");

    box.add(label);
//    consoleSizeField = new JComboBox<Integer>(FONT_SIZES);
    consoleSizeField = new JComboBox<Integer>(FONT_SIZES);
    consoleSizeField.setEditable(true);
    box.add(consoleSizeField);

    pain.add(box);
    d = box.getPreferredSize();
    box.setBounds(left, top, d.width, d.height);
    fontSizeField.setSelectedItem(Preferences.getFont("editor.font.size"));
    top += d.height + GUI_BETWEEN;


    Container colorBox = Box.createHorizontalBox();

    label = new JLabel(Language.text("preferences.background_color")+": ");
    colorBox.add(label);

    final String colorTip = "<html>"
        + "Select the background color used when using Present.<br/>"
        + "Present is used to present a sketch in full-screen, <br/>"
        + "accessible from the Sketch menu.";
    label.setToolTipText(colorTip);

    presentColor = new JTextField("      ");
    presentColor.setOpaque(true);
    presentColor.setEnabled(false);
    presentColor.setBorder(new CompoundBorder(BorderFactory.createMatteBorder(
        1, 1, 0, 0, new Color(195, 195, 195)), BorderFactory.createMatteBorder(
        0, 0, 1, 1, new Color(54, 54, 54))));
    presentColor.setBackground(Preferences.getColor("run.present.bgcolor"));

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

      @Override public void changedUpdate(DocumentEvent e) {}
    });

    selector = new ColorChooser(dialog, false,
        Preferences.getColor("run.present.bgcolor"), "OK",
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            String colorValue = selector.getHexColor();
            presentColorHex.setText(colorValue.substring(1));
            presentColor.setBackground(new Color(Integer.parseInt(
                colorValue.substring(1, 3), 16), Integer.parseInt(
                colorValue.substring(3, 5), 16), Integer.parseInt(
                colorValue.substring(5, 7), 16)));
            selector.hide();
          }
        });

    presentColor.addMouseListener(new MouseListener() {
      @Override public void mouseReleased(MouseEvent e) {}
      @Override public void mousePressed(MouseEvent e) {}

      @Override
      public void mouseExited(MouseEvent e) {
        dialog.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
      }

      @Override
      public void mouseEntered(MouseEvent e) {
        dialog.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
      }

      @Override
      public void mouseClicked(MouseEvent e) {
        selector.show();
      }
    });

    label = new JLabel("#");
    colorBox.add(label);
    colorBox.add(presentColorHex);
    colorBox.add(Box.createHorizontalStrut(GUI_SMALL + 2 / 3 * GUI_SMALL));
    colorBox.add(presentColor);

    pain.add(colorBox);
    d = colorBox.getPreferredSize();
    colorBox.setBounds(left, top, d.width, d.height);

    top += d.height + GUI_BETWEEN;


    // [ ] Use smooth text in editor window

    editorAntialiasBox = new JCheckBox(Language.text("preferences.use_smooth_text"));
    pain.add(editorAntialiasBox);
    d = editorAntialiasBox.getPreferredSize();
    // adding +10 because ubuntu + jre 1.5 truncating items
    editorAntialiasBox.setBounds(left, top, d.width + 10, d.height);
    right = Math.max(right, left + d.width);
    top += d.height + GUI_BETWEEN;


    // [ ] Enable complex text input (for Japanese et al, requires restart)

    inputMethodBox =
      new JCheckBox(Language.text("preferences.enable_complex_text_input")+
                    " ("+Language.text("preferences.enable_complex_text_input_example")+
                    ", "+Language.text("preferences.requires_restart")+")");
    pain.add(inputMethodBox);
    d = inputMethodBox.getPreferredSize();
    inputMethodBox.setBounds(left, top, d.width + 10, d.height);
    right = Math.max(right, left + d.width);
    top += d.height + GUI_BETWEEN;


    // [ ] Continuously check for errors - PDE X

    errorCheckerBox =
      new JCheckBox(Language.text("preferences.continuously_check"));

    pain.add(errorCheckerBox);
    d = errorCheckerBox.getPreferredSize();
    errorCheckerBox.setBounds(left, top, d.width + 10, d.height);
    //right = Math.max(right, left + d.width);
    //top += d.height + GUI_BETWEEN;
    int warningLeft = left + d.width;


    // [ ] Show Warnings - PDE X

    warningsCheckerBox =
      new JCheckBox(Language.text("preferences.show_warnings"));
    pain.add(warningsCheckerBox);
    d = warningsCheckerBox.getPreferredSize();
    warningsCheckerBox.setBounds(warningLeft, top, d.width + 10, d.height);
    right = Math.max(right, warningLeft + d.width);
    top += d.height + GUI_BETWEEN;


    // [ ] Enable Code Completion - PDE X

    codeCompletionBox =
      new JCheckBox(Language.text("preferences.code_completion"));
    pain.add(codeCompletionBox);
    d = codeCompletionBox.getPreferredSize();
    codeCompletionBox.setBounds(left, top, d.width + 10, d.height);
    int toggleLeft = left + d.width;


    // [ ] Toggle Code Completion Trigger - PDE X

    final String modifier = Base.isMacOS() ? "\u2318" : "Ctrl";
    codeCompletionTriggerBox =
      new JCheckBox(Language.text("preferences.trigger_with")+" " + modifier + "-"+Language.text("preferences.cmd_space"));
    pain.add(codeCompletionTriggerBox);
    d = codeCompletionTriggerBox.getPreferredSize();
    codeCompletionTriggerBox.setBounds(toggleLeft, top, d.width + 10, d.height);
    right = Math.max(right, toggleLeft + d.width);
    top += d.height + GUI_BETWEEN;


    // [ ] Increase maximum available memory to [______] MB

    Container memoryBox = Box.createHorizontalBox();
    memoryOverrideBox = new JCheckBox(Language.text("preferences.increase_max_memory")+": ");
    memoryBox.add(memoryOverrideBox);
    memoryField = new JTextField(4);
    memoryBox.add(memoryField);
    memoryBox.add(new JLabel(" MB"));
    pain.add(memoryBox);
    d = memoryBox.getPreferredSize();
    memoryBox.setBounds(left, top, d.width, d.height);
    top += d.height + GUI_BETWEEN;


    // [ ] Delete previous application folder on export

    deletePreviousBox =
    new JCheckBox(Language.text("preferences.delete_previous_folder_on_export"));
    pain.add(deletePreviousBox);
    d = deletePreviousBox.getPreferredSize();
    deletePreviousBox.setBounds(left, top, d.width + 10, d.height);
    right = Math.max(right, left + d.width);
    top += d.height + GUI_BETWEEN;


    // [ ] Hide tab/toolbar background image

    whinyBox = new JCheckBox(Language.text("preferences.hide_toolbar_background_image")+
                             " ("+Language.text("preferences.requires_restart")+")");
    pain.add(whinyBox);
    d = whinyBox.getPreferredSize();
    whinyBox.setBounds(left, top, d.width + 10, d.height);
    right = Math.max(right, left + d.width);
    top += d.height + GUI_BETWEEN;


    // [ ] Check for updates on startup

    checkUpdatesBox = new JCheckBox(Language.text("preferences.check_for_updates_on_startup"));
    pain.add(checkUpdatesBox);
    d = checkUpdatesBox.getPreferredSize();
    checkUpdatesBox.setBounds(left, top, d.width + 10, d.height);
    right = Math.max(right, left + d.width);
    top += d.height + GUI_BETWEEN;


    // Run sketches on display [  1 ]

    Container displayBox = Box.createHorizontalBox();
    JLabel displayLabel = new JLabel(Language.text("preferences.run_sketches_on_display")+": ");
    final String tip = "<html>" + Language.text("preferences.run_sketches_on_display.tip");
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
        new JCheckBox(Language.text("preferences.automatically_associate_pde_files"));
      pain.add(autoAssociateBox);
      d = autoAssociateBox.getPreferredSize();
      autoAssociateBox.setBounds(left, top, d.width + 10, d.height);
      right = Math.max(right, left + d.width);
      top += d.height + GUI_BETWEEN;
    }


    // More preferences are in the ...

    label = new JLabel(Language.text("preferences.file")+":");
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

    label = new JLabel("("+Language.text("preferences.file.hint")+")");
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
    String oldPath = getSketchbookPath();
    String newPath = sketchbookLocationField.getText();
    if (!newPath.equals(oldPath)) {
      base.setSketchbookFolder(new File(newPath));
    }

    setBoolean("update.check", checkUpdatesBox.isSelected()); //$NON-NLS-1$

    // Save Language
    Map<String, String> languages = Language.getLanguages();
    String language = "";
    for (Map.Entry<String, String> lang : languages.entrySet()) {
      if (lang.getValue().equals(String.valueOf(languageSelectionBox.getSelectedItem()))) {
        language = lang.getKey().trim().toLowerCase();
        break;
      }
    }
    if (!language.equals(Language.getLanguage()) && !language.equals("")) {
      Language.saveLanguage(language);
    }

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

    // Don't change anything if the user closes the window before fonts load
    if (fontSelectionBox.isEnabled()) {
      String fontFamily = (String) fontSelectionBox.getSelectedItem();
      set("editor.font.family", fontFamily);
    }

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

    setBoolean("pdex.errorCheckEnabled", errorCheckerBox.isSelected());
    setBoolean("pdex.warningsEnabled", warningsCheckerBox.isSelected());
    setBoolean("pdex.ccEnabled", codeCompletionBox.isSelected());
    setBoolean("pdex.ccTriggerEnabled", codeCompletionTriggerBox.isSelected());
    for (Editor editor : base.getEditors()) {
      editor.applyPreferences();
    }

  }


  protected void showFrame() {
    editorAntialiasBox.setSelected(getBoolean("editor.smooth")); //$NON-NLS-1$
    inputMethodBox.setSelected(getBoolean("editor.input_method_support")); //$NON-NLS-1$
    errorCheckerBox.setSelected(getBoolean("pdex.errorCheckEnabled"));
    warningsCheckerBox.setSelected(getBoolean("pdex.warningsEnabled"));
    codeCompletionBox.setSelected(getBoolean("pdex.ccEnabled"));
    codeCompletionTriggerBox.setSelected(getBoolean("pdex.ccTriggerEnabled"));
    deletePreviousBox.
      setSelected(getBoolean("export.delete_target_folder")); //$NON-NLS-1$

    sketchbookLocationField.setText(getSketchbookPath());
    checkUpdatesBox.setSelected(getBoolean("update.check")); //$NON-NLS-1$

    whinyBox.setSelected(getBoolean("header.hide.image") || //$NON-NLS-1$
                         getBoolean("buttons.hide.image")); //$NON-NLS-1$

    updateDisplayList();
    int displayNum = getInteger("run.display"); //$NON-NLS-1$
    if (displayNum >= 0 && displayNum < displayCount) {
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
    presentColorHex.setText(Preferences.get("run.present.bgcolor").substring(1));

    memoryOverrideBox.
      setSelected(getBoolean("run.options.memory")); //$NON-NLS-1$
    memoryField.
      setText(get("run.options.memory.maximum")); //$NON-NLS-1$

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
      setText(value.getFamily() + " / " + value.getName() + " (" + value.getPSName() + ")");
      return this;
    }
  }


  void initFontList() {
    if (monoFontFamilies == null) {
      monoFontFamilies = Toolkit.getMonoFontFamilies();
      fontSelectionBox.setModel(new DefaultComboBoxModel(monoFontFamilies));
      String family = get("editor.font.family");

      // Set a reasonable default, in case selecting the family fails
      fontSelectionBox.setSelectedItem("Monospaced");
      fontSelectionBox.setSelectedItem(family);
      fontSelectionBox.setEnabled(true);
    }
  }


  void updateDisplayList() {
    GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
    displayCount = ge.getScreenDevices().length;
    String[] items = new String[displayCount];
    for (int i = 0; i < displayCount; i++) {
      items[i] = String.valueOf(i + 1);
    }
    displaySelectionBox.setModel(new DefaultComboBoxModel(items));
  }
}
