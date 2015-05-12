/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2012-15 The Processing Foundation
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

import processing.app.ColorChooser;
import processing.app.Language;
import processing.core.*;


/**
 * Window for modifying preferences.
 */
public class PreferencesFrame {
  JFrame dialog;
  GroupLayout layout;

  static final Integer[] FONT_SIZES = { 10, 12, 14, 18, 24, 36, 48 };

  JTextField sketchbookLocationField;
  JTextField presentColor;
  JTextField presentColorHex;
  JCheckBox editorAntialiasBox;
  JCheckBox deletePreviousBox;
  JCheckBox memoryOverrideBox;
  JTextField memoryField;
  JCheckBox checkUpdatesBox;
  JComboBox<Integer> fontSizeField;
  JComboBox<Integer> consoleFontSizeField;
  JCheckBox inputMethodBox;
  JCheckBox autoAssociateBox;

  ColorChooser selector;

  JCheckBox errorCheckerBox;
  JCheckBox warningsCheckerBox;
  JCheckBox codeCompletionBox;
  JCheckBox importSuggestionsBox;
  //JCheckBox codeCompletionTriggerBox;

  JComboBox<String> displaySelectionBox;
  JComboBox<String> languageSelectionBox;

  int displayCount;

  String[] monoFontFamilies;
  JComboBox<String> fontSelectionBox;

  JButton okButton;

  /** Base object so that updates can be applied to the list of editors. */
  Base base;


  public PreferencesFrame(Base base) {
    this.base = base;
    //dialog = new JDialog(editor, "Preferences", true);
    dialog = new JFrame(Language.text("preferences"));
    Container pain = dialog.getContentPane();
    layout = new GroupLayout(pain);
    layout.setAutoCreateGaps(true);
    layout.setAutoCreateContainerGaps(true);

    pain.setLayout(layout);

//    final int GUI_BETWEEN = Preferences.GUI_BETWEEN;
//    final int GUI_BIG = Preferences.GUI_BIG;
//    final int GUI_SMALL = Preferences.GUI_SMALL;
    final int BUTTON_WIDTH = Preferences.BUTTON_WIDTH;
    final int BORDER = Base.isMacOS() ? 20 : 13;

    JLabel sketchbookLocationLabel, restartProcessingLabel;
    JButton browseButton; //, button2;


    // Sketchbook location:
    // [...............................]  [ Browse ]

    sketchbookLocationLabel = new JLabel(Language.text("preferences.sketchbook_location")+":");

    sketchbookLocationField = new JTextField(40);

    browseButton = new JButton(Preferences.PROMPT_BROWSE);
    browseButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          File dflt = new File(sketchbookLocationField.getText());
          PApplet.selectFolder(Language.text("preferences.sketchbook_location.popup"),
                               "sketchbookCallback", dflt,
                               PreferencesFrame.this, dialog);
        }
      });

    // Language: [ English ] (requires restart of Processing)



    JLabel languageLabel = new JLabel(Language.text("preferences.language")+": ");
    languageSelectionBox = new JComboBox<String>();

    Map<String, String> languages = Language.getLanguages();
    String[] languageSelection = new String[languages.size()];
    languageSelection[0] = languages.get(Language.getLanguage());
    int i = 1;
    for (Map.Entry<String, String> lang : languages.entrySet()) {
      if(!lang.getKey().equals(Language.getLanguage())){
        languageSelection[i++] = lang.getValue();
      }
    }
    languageSelectionBox.setModel(new DefaultComboBoxModel<String>(languageSelection));
    restartProcessingLabel = new JLabel(" ("+Language.text("preferences.requires_restart")+")");

    // Editor and console font [ Source Code Pro ]

    // Nevermind on this for now.. Java doesn't seem to have a method for
    // enumerating only the fixed-width (monospaced) fonts. To do this
    // properly, we'd need to list the fonts, and compare the metrics of
    // i and M for each. When they're identical (and not degenerate),
    // we'd call that font fixed width. That's all a very expensive set of
    // operations, so it should also probably be cached between runs and
    // updated in the background.

    JLabel fontLabel = new JLabel(Language.text("preferences.editor_and_console_font")+": ");
    final String fontTip = "<html>" + Language.text("preferences.editor_and_console_font.tip");
    fontLabel.setToolTipText(fontTip);
    // get a wide name in there before getPreferredSize() is called
    fontSelectionBox = new JComboBox<String>(new String[] { Toolkit.getMonoFontName() });
    fontSelectionBox.setToolTipText(fontTip);
    //updateDisplayList();
    fontSelectionBox.setEnabled(false);  // don't enable until fonts are loaded

    // Editor font size [ 12 ]  Console font size [ 10 ]

    JLabel fontSizelabel = new JLabel(Language.text("preferences.editor_font_size")+": ");
    fontSizeField = new JComboBox<Integer>(FONT_SIZES);

    JLabel consoleFontSizeLabel = new JLabel(Language.text("preferences.console_font_size")+": ");
    consoleFontSizeField = new JComboBox<Integer>(FONT_SIZES);
    fontSizeField.setSelectedItem(Preferences.getFont("editor.font.size"));

    JLabel backgroundColorLabel = new JLabel(Language.text("preferences.background_color")+": ");

    final String colorTip = "<html>" + Language.text("preferences.background_color.tip");
    backgroundColorLabel.setToolTipText(colorTip);

    presentColor = new JTextField("      ");
    presentColor.setOpaque(true);
    presentColor.setEnabled(false);
    Border cb = new CompoundBorder(BorderFactory.createMatteBorder(1, 1, 0, 0, new Color(195, 195, 195)),
                                   BorderFactory.createMatteBorder(0, 0, 1, 1, new Color(54, 54, 54)));
    presentColor.setBorder(cb);
    presentColor.setBackground(Preferences.getColor("run.present.bgcolor"));

    presentColorHex = new JTextField(6);
    presentColorHex.setText(Preferences.get("run.present.bgcolor").substring(1));
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
        if (colorValue.length() == 6 &&
            colorValue.matches("[0123456789ABCDEF]*")) {
          presentColor.setBackground(new Color(PApplet.unhex(colorValue)));
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
          presentColor.setBackground(new Color(PApplet.unhex(colorValue)));
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
        Preferences.getColor("run.present.bgcolor"), Language.text("prompt.ok"),
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            String colorValue = selector.getHexColor();
            presentColorHex.setText(colorValue.substring(1));
            presentColor.setBackground(new Color(PApplet.unhex(colorValue)));
            selector.hide();
          }
        });

    presentColor.addMouseListener(new MouseAdapter() {

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

    JLabel hashLabel = new JLabel("#");

    // [ ] Use smooth text in editor window

    editorAntialiasBox = new JCheckBox(Language.text("preferences.use_smooth_text"));

    // [ ] Enable complex text input (for Japanese et al, requires restart)

    inputMethodBox =
      new JCheckBox(Language.text("preferences.enable_complex_text_input")+
                    " ("+Language.text("preferences.enable_complex_text_input_example")+
                    ", "+Language.text("preferences.requires_restart")+")");

    // [ ] Continuously check for errors - PDE X

    errorCheckerBox =
      new JCheckBox(Language.text("preferences.continuously_check"));

    // [ ] Show Warnings - PDE X

    warningsCheckerBox =
      new JCheckBox(Language.text("preferences.show_warnings"));

    // [ ] Enable Code Completion - PDE X

    codeCompletionBox =
      new JCheckBox(Language.text("preferences.code_completion") +
                    " Ctrl-" + Language.text("preferences.cmd_space"));
////    codeCompletionBox.addActionListener(new ActionListener() {
////
////      @Override
////      public void actionPerformed(ActionEvent e) {
////        // Disble code completion trigger option if completion is disabled
////        codeCompletionTriggerBox.setEnabled(codeCompletionBox.isSelected());
////      }
////    });

////    int toggleLeft = left + d.width;

    //// [ ] Toggle Code Completion Trigger - PDE X. No longer needed (Manindra)

////    codeCompletionTriggerBox =
////      new JCheckBox(Language.text("preferences.trigger_with")+" Ctrl-"+Language.text("preferences.cmd_space"));
////    pain.add(codeCompletionTriggerBox);
////    d = codeCompletionTriggerBox.getPreferredSize();
////    codeCompletionTriggerBox.setBounds(toggleLeft, top, d.width + 10, d.height);
////    right = Math.max(right, toggleLeft + d.width);

    // [ ] Show import suggestions - PDE X

    importSuggestionsBox =
      new JCheckBox(Language.text("preferences.suggest_imports"));

    // [ ] Increase maximum available memory to [______] MB

    memoryOverrideBox = new JCheckBox(Language.text("preferences.increase_max_memory")+": ");
    memoryField = new JTextField(4);
    memoryOverrideBox.addChangeListener(new ChangeListener() {
      @Override
      public void stateChanged(ChangeEvent e) {
        memoryField.setEnabled(memoryOverrideBox.isSelected());
      }
    });
    JLabel mbLabel = new JLabel("MB");

    // [ ] Delete previous application folder on export

    deletePreviousBox =
      new JCheckBox(Language.text("preferences.delete_previous_folder_on_export"));

    // [ ] Check for updates on startup

    checkUpdatesBox =
      new JCheckBox(Language.text("preferences.check_for_updates_on_startup"));

    // Run sketches on display [  1 ]

    JLabel displayLabel = new JLabel(Language.text("preferences.run_sketches_on_display")+": ");
    final String tip = "<html>" + Language.text("preferences.run_sketches_on_display.tip");
    displayLabel.setToolTipText(tip);
    displaySelectionBox = new JComboBox<String>();
    updateDisplayList();  // needs to happen here for getPreferredSize()

    // [ ] Automatically associate .pde files with Processing

      autoAssociateBox =
        new JCheckBox(Language.text("preferences.automatically_associate_pde_files"));
      autoAssociateBox.setVisible(false);

    // More preferences are in the ...

    JLabel morePreferenceLabel = new JLabel(Language.text("preferences.file") + ":");
    morePreferenceLabel.setForeground(Color.gray);

    JLabel preferencePathLabel = new JLabel(Preferences.getPreferencesPath());
    final JLabel clickable = preferencePathLabel;
    preferencePathLabel.addMouseListener(new MouseAdapter() {
        public void mousePressed(MouseEvent e) {
          Base.openFolder(Base.getSettingsFolder());
        }

        // Light this up in blue like a hyperlink
        public void mouseEntered(MouseEvent e) {
          clickable.setForeground(new Color(0, 0, 140));
        }

        // Set the text back to black when the mouse is outside
        public void mouseExited(MouseEvent e) {
          clickable.setForeground(Color.BLACK);
        }
      });

    JLabel preferenceHintLabel = new JLabel("(" + Language.text("preferences.file.hint") + ")");
    preferenceHintLabel.setForeground(Color.gray);

    // [  OK  ] [ Cancel ]

    okButton = new JButton(Preferences.PROMPT_OK);
    okButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          applyFrame();
          disposeFrame();
        }
      });

    JButton cancelButton = new JButton(Preferences.PROMPT_CANCEL);
    cancelButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          disposeFrame();
        }
      });

    layout.setHorizontalGroup(layout.createSequentialGroup() // sequential group for border + mainContent + border
      .addGap(BORDER)
      .addGroup(layout.createParallelGroup() // parallel group for rest of the components
          .addComponent(sketchbookLocationLabel)
          .addGroup(layout.createSequentialGroup()
                      .addComponent(sketchbookLocationField)
                      .addComponent(browseButton))
          .addGroup(layout.createSequentialGroup()
                      .addComponent(languageLabel)
                      .addComponent(languageSelectionBox,GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE) // This makes the component non-resizable in the X direction
                      .addComponent(restartProcessingLabel))
          .addGroup(layout.createSequentialGroup()
                      .addComponent(fontLabel)
                      .addComponent(fontSelectionBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
          .addGroup(GroupLayout.Alignment.LEADING,
                       layout.createSequentialGroup()
                      .addComponent(fontSizelabel)
                      .addComponent(fontSizeField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                      .addComponent(consoleFontSizeLabel)
                      .addComponent(consoleFontSizeField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
          .addGroup(layout.createSequentialGroup()
                      .addComponent(backgroundColorLabel)
                      .addComponent(hashLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                      .addGap(0)
                      .addComponent(presentColorHex, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                      .addComponent(presentColor, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
          .addComponent(editorAntialiasBox)
          .addComponent(inputMethodBox)
          .addGroup(layout.createSequentialGroup()
                      .addComponent(errorCheckerBox)
                      .addComponent(warningsCheckerBox))
          .addComponent(codeCompletionBox)
          .addComponent(importSuggestionsBox)
          .addGroup(layout.createSequentialGroup()
                        .addComponent(memoryOverrideBox)
                        .addComponent(memoryField,
                                      GroupLayout.PREFERRED_SIZE,
                                      GroupLayout.DEFAULT_SIZE,
                                      GroupLayout.PREFERRED_SIZE)
                        .addComponent(mbLabel))
          .addComponent(deletePreviousBox)
          .addComponent(checkUpdatesBox)
          .addGroup(layout.createSequentialGroup()
                      .addComponent(displayLabel)
                      .addComponent(displaySelectionBox,
                                    GroupLayout.PREFERRED_SIZE,
                                    GroupLayout.DEFAULT_SIZE,
                                    GroupLayout.PREFERRED_SIZE)
          )
          .addComponent(autoAssociateBox)
          .addComponent(morePreferenceLabel)
          .addComponent(preferencePathLabel)
          .addComponent(preferenceHintLabel)
          .addGroup(GroupLayout.Alignment.TRAILING,layout.createSequentialGroup() // Trailing so that the buttons are to the right
                      .addComponent(okButton, BUTTON_WIDTH, GroupLayout.DEFAULT_SIZE, BUTTON_WIDTH) // Ok and Cancel buttton are now of size BUTTON_WIDTH
                      .addComponent(cancelButton, BUTTON_WIDTH, GroupLayout.DEFAULT_SIZE, BUTTON_WIDTH)
          ))
      .addGap(BORDER)
    );

    layout.setVerticalGroup(layout.createSequentialGroup() // sequential group for border + mainContent + border
      .addGap(BORDER)
      .addComponent(sketchbookLocationLabel)
      .addGroup(layout.createParallelGroup()
                  .addComponent(sketchbookLocationField)
                  .addComponent(browseButton))
      .addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                  .addComponent(languageLabel)
                  .addComponent(languageSelectionBox)
                  .addComponent(restartProcessingLabel))
      .addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER).
                  addComponent(fontLabel)
                  .addComponent(fontSelectionBox))
      .addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                  .addComponent(fontSizelabel)
                  .addComponent(fontSizeField)
                  .addComponent(consoleFontSizeLabel)
                  .addComponent(consoleFontSizeField))
      .addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                  .addComponent(backgroundColorLabel)
                  .addComponent(hashLabel)
                  .addComponent(presentColorHex)
                  .addComponent(presentColor))
      .addComponent(editorAntialiasBox)
      .addComponent(inputMethodBox)
      .addGroup(layout.createParallelGroup()
                  .addComponent(errorCheckerBox)
                  .addComponent(warningsCheckerBox))
      .addComponent(codeCompletionBox)
      .addComponent(importSuggestionsBox)
      .addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                .addComponent(memoryOverrideBox)
                .addComponent(memoryField)
                .addComponent(mbLabel))
      .addComponent(deletePreviousBox)
      .addComponent(checkUpdatesBox)
      .addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                  .addComponent(displayLabel)
                  .addComponent(displaySelectionBox))
      .addComponent(autoAssociateBox)
      .addComponent(morePreferenceLabel)
      .addGap(0)
      .addComponent(preferencePathLabel)
      .addGap(0)
      .addComponent(preferenceHintLabel)
      .addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                  .addComponent(okButton)
                  .addComponent(cancelButton))
      .addGap(BORDER)
      );

    if (Base.isWindows()){
      autoAssociateBox.setVisible(true);
    }
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
    // finish up

    Toolkit.registerWindowCloseKeys(dialog.getRootPane(), disposer);
    Toolkit.setIcon(dialog);
    dialog.setResizable(false);
    dialog.pack();
    dialog.setLocationRelativeTo(null);

    // Workaround for OS X, which breaks the layout when these are set earlier
    // https://github.com/processing/processing/issues/3212
    fontSizeField.setEditable(true);
    consoleFontSizeField.setEditable(true);

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


//  public Dimension getPreferredSize() {
//    return new Dimension(wide, high);
//  }


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
    Preferences.setBoolean("editor.smooth", //$NON-NLS-1$
                           editorAntialiasBox.isSelected());

    Preferences.setBoolean("export.delete_target_folder", //$NON-NLS-1$
                           deletePreviousBox.isSelected());

    // if the sketchbook path has changed, rebuild the menus
    String oldPath = Preferences.getSketchbookPath();
    String newPath = sketchbookLocationField.getText();
    if (!newPath.equals(oldPath)) {
      base.setSketchbookFolder(new File(newPath));
    }

//    setBoolean("editor.external", externalEditorBox.isSelected());
    Preferences.setBoolean("update.check", checkUpdatesBox.isSelected()); //$NON-NLS-1$

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

    int oldDisplayIndex = Preferences.getInteger("run.display"); //$NON-NLS-1$
    int displayIndex = 0;
    for (int d = 0; d < displaySelectionBox.getItemCount(); d++) {
      if (displaySelectionBox.getSelectedIndex() == d) {
        displayIndex = d;
      }
    }
    if (oldDisplayIndex != displayIndex) {
      Preferences.setInteger("run.display", displayIndex); //$NON-NLS-1$
      for (Editor editor : base.getEditors()) {
        editor.setSketchLocation(null);
      }
    }

    Preferences.setBoolean("run.options.memory", memoryOverrideBox.isSelected()); //$NON-NLS-1$
    int memoryMin = Preferences.getInteger("run.options.memory.initial"); //$NON-NLS-1$
    int memoryMax = Preferences.getInteger("run.options.memory.maximum"); //$NON-NLS-1$
    try {
      memoryMax = Integer.parseInt(memoryField.getText().trim());
      // make sure memory setting isn't too small
      if (memoryMax < memoryMin) memoryMax = memoryMin;
      Preferences.setInteger("run.options.memory.maximum", memoryMax); //$NON-NLS-1$
    } catch (NumberFormatException e) {
      System.err.println("Ignoring bad memory setting");
    }

    // Don't change anything if the user closes the window before fonts load
    if (fontSelectionBox.isEnabled()) {
      String fontFamily = (String) fontSelectionBox.getSelectedItem();
      Preferences.set("editor.font.family", fontFamily);
    }

    try {
      Object selection = fontSizeField.getSelectedItem();
      if (selection instanceof String) {
        // Replace with Integer version
        selection = Integer.parseInt((String) selection);
      }
      Preferences.set("editor.font.size", String.valueOf(selection));

    } catch (NumberFormatException e) {
      Base.log("Ignoring invalid font size " + fontSizeField); //$NON-NLS-1$
      fontSizeField.setSelectedItem(Preferences.getInteger("editor.font.size"));
    }

    try {
      Object selection = consoleFontSizeField.getSelectedItem();
      if (selection instanceof String) {
        // Replace with Integer version
        selection = Integer.parseInt((String) selection);
      }
      Preferences.set("console.font.size", String.valueOf(selection));

    } catch (NumberFormatException e) {
      Base.log("Ignoring invalid font size " + consoleFontSizeField); //$NON-NLS-1$
      consoleFontSizeField.setSelectedItem(Preferences.getInteger("console.font.size"));
    }

    Preferences.setColor("run.present.bgcolor", presentColor.getBackground());

    Preferences.setBoolean("editor.input_method_support", inputMethodBox.isSelected()); //$NON-NLS-1$

    if (autoAssociateBox != null) {
      Preferences.setBoolean("platform.auto_file_type_associations", //$NON-NLS-1$
                             autoAssociateBox.isSelected());
    }

    Preferences.setBoolean("pdex.errorCheckEnabled", errorCheckerBox.isSelected());
    Preferences.setBoolean("pdex.warningsEnabled", warningsCheckerBox.isSelected());
    Preferences.setBoolean("pdex.completion", codeCompletionBox.isSelected());
    //Preferences.setBoolean("pdex.completion.trigger", codeCompletionTriggerBox.isSelected());
    Preferences.setBoolean("pdex.importSuggestEnabled", importSuggestionsBox.isSelected());

    for (Editor editor : base.getEditors()) {
      editor.applyPreferences();
    }
  }


  protected void showFrame() {
    editorAntialiasBox.setSelected(Preferences.getBoolean("editor.smooth")); //$NON-NLS-1$
    inputMethodBox.setSelected(Preferences.getBoolean("editor.input_method_support")); //$NON-NLS-1$
    errorCheckerBox.setSelected(Preferences.getBoolean("pdex.errorCheckEnabled"));
    warningsCheckerBox.setSelected(Preferences.getBoolean("pdex.warningsEnabled"));
    codeCompletionBox.setSelected(Preferences.getBoolean("pdex.completion"));
    //codeCompletionTriggerBox.setSelected(Preferences.getBoolean("pdex.completion.trigger"));
    //codeCompletionTriggerBox.setEnabled(codeCompletionBox.isSelected());
    importSuggestionsBox.setSelected(Preferences.getBoolean("pdex.importSuggestEnabled"));
    deletePreviousBox.setSelected(Preferences.getBoolean("export.delete_target_folder")); //$NON-NLS-1$

    sketchbookLocationField.setText(Preferences.getSketchbookPath());
    checkUpdatesBox.setSelected(Preferences.getBoolean("update.check")); //$NON-NLS-1$

    updateDisplayList();
    int displayNum = Preferences.getInteger("run.display"); //$NON-NLS-1$
    if (displayNum >= 0 && displayNum < displayCount) {
      displaySelectionBox.setSelectedIndex(displayNum);
    }

    // This takes a while to load, so run it from a separate thread
    //EventQueue.invokeLater(new Runnable() {
    new Thread(new Runnable() {
      public void run() {
        initFontList();
      }
    }).start();

    fontSizeField.setSelectedItem(Preferences.getInteger("editor.font.size"));
    consoleFontSizeField.setSelectedItem(Preferences.getInteger("console.font.size"));

    presentColor.setBackground(Preferences.getColor("run.present.bgcolor"));
    presentColorHex.setText(Preferences.get("run.present.bgcolor").substring(1));

    memoryOverrideBox.
      setSelected(Preferences.getBoolean("run.options.memory")); //$NON-NLS-1$
    memoryField.
      setText(Preferences.get("run.options.memory.maximum")); //$NON-NLS-1$
    memoryField.setEnabled(memoryOverrideBox.isSelected());

    if (autoAssociateBox != null) {
      autoAssociateBox.setSelected(Preferences.getBoolean("platform.auto_file_type_associations")); //$NON-NLS-1$
    }
    // The OK Button has to be set as the default button every time the
    // PrefWindow is to be displayed
    dialog.getRootPane().setDefaultButton(okButton);

    // The pack is called again here second time to fix layout bugs
    // the bugs are not due to groupLayout but due to HTML rendering of components
    // more info can be found here -> https://netbeans.org/bugzilla/show_bug.cgi?id=79967
    dialog.pack();

    dialog.setVisible(true);
  }


  /**
   * I have some ideas on how we could make Swing even more obtuse for the
   * most basic usage scenarios. Is there someone on the team I can contact?
   * Oracle staffer, are you reading this? This could be your meal ticket.
   */
  static class FontNamer extends JLabel implements ListCellRenderer<Font> {
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

      EventQueue.invokeLater(new Runnable() {
        @Override
        public void run() {
          fontSelectionBox.setModel(new DefaultComboBoxModel<String>(monoFontFamilies));
          String family = Preferences.get("editor.font.family");

          // Set a reasonable default, in case selecting the family fails
          fontSelectionBox.setSelectedItem("Monospaced");
          // Now try to select the family (will fail silently, see prev line)
          fontSelectionBox.setSelectedItem(family);
          fontSelectionBox.setEnabled(true);
        }
      });
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
    displaySelectionBox.setModel(new DefaultComboBoxModel<String>(items));
//    displaySelectionBox = new JComboBox(items);
  }
}
