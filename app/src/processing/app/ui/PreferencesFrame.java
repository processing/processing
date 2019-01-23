/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2012-19 The Processing Foundation
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

package processing.app.ui;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;

import processing.app.Base;
import processing.app.Language;
import processing.app.Messages;
import processing.app.Platform;
import processing.app.Preferences;
import processing.app.ui.ColorChooser;
import processing.core.*;


/**
 * Creates the window for modifying preferences.
 */
public class PreferencesFrame {
  JFrame frame;
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

  JComboBox<String> zoomSelectionBox;
  JCheckBox zoomAutoBox;

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

    frame = new JFrame(Language.text("preferences"));
    Container pain = frame.getContentPane();
    layout = new GroupLayout(pain);
    layout.setAutoCreateGaps(true);
    layout.setAutoCreateContainerGaps(true);

    pain.setLayout(layout);

    JLabel sketchbookLocationLabel;
    JLabel languageRestartLabel;
    JLabel zoomRestartLabel;
    JButton browseButton; //, button2;


    // Sketchbook location:
    // [...............................]  [ Browse ]

    sketchbookLocationLabel = new JLabel(Language.text("preferences.sketchbook_location")+":");

    sketchbookLocationField = new JTextField(40);

    browseButton = new JButton(Language.getPrompt("browse"));
    browseButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          File dflt = new File(sketchbookLocationField.getText());
          PApplet.selectFolder(Language.text("preferences.sketchbook_location.popup"),
                               "sketchbookCallback", dflt,
                               PreferencesFrame.this, frame);
        }
      });


    // Language: [ English ] (requires restart of Processing)

    JLabel languageLabel = new JLabel(Language.text("preferences.language")+": ");
    languageSelectionBox = new JComboBox<>();

    Map<String, String> languages = Language.getLanguages();
    String[] languageSelection = new String[languages.size()];
    languageSelection[0] = languages.get(Language.getLanguage());
    int i = 1;
    for (Map.Entry<String, String> lang : languages.entrySet()) {
      if(!lang.getKey().equals(Language.getLanguage())){
        languageSelection[i++] = lang.getValue();
      }
    }
    languageSelectionBox.setModel(new DefaultComboBoxModel<>(languageSelection));
    languageRestartLabel = new JLabel(" (" + Language.text("preferences.requires_restart") + ")");


    // Editor and console font [ Source Code Pro ]

    JLabel fontLabel = new JLabel(Language.text("preferences.editor_and_console_font")+": ");
    final String fontTip = "<html>" + Language.text("preferences.editor_and_console_font.tip");
    fontLabel.setToolTipText(fontTip);
    // get a wide name in there before getPreferredSize() is called
    fontSelectionBox = new JComboBox<>(new String[] { Toolkit.getMonoFontName() });
    fontSelectionBox.setToolTipText(fontTip);
    fontSelectionBox.setEnabled(false);  // don't enable until fonts are loaded


    // Editor font size [ 12 ]  Console font size [ 10 ]

    JLabel fontSizelabel = new JLabel(Language.text("preferences.editor_font_size")+": ");
    fontSizeField = new JComboBox<>(FONT_SIZES);

    JLabel consoleFontSizeLabel = new JLabel(Language.text("preferences.console_font_size")+": ");
    consoleFontSizeField = new JComboBox<>(FONT_SIZES);
    fontSizeField.setSelectedItem(Preferences.getFont("editor.font.size"));


    // Interface scale: [ 100% ] (requires restart of Processing)

    JLabel zoomLabel = new JLabel(Language.text("preferences.zoom") + ": ");

    zoomAutoBox = new JCheckBox(Language.text("preferences.zoom.auto"));
    zoomAutoBox.addChangeListener(new ChangeListener() {
      @Override
      public void stateChanged(ChangeEvent e) {
        zoomSelectionBox.setEnabled(!zoomAutoBox.isSelected());
      }
    });

    zoomSelectionBox = new JComboBox<>();
    zoomSelectionBox.setModel(new DefaultComboBoxModel<>(Toolkit.zoomOptions.array()));
    zoomRestartLabel = new JLabel(" (" + Language.text("preferences.requires_restart") + ")");

    //

    JLabel backgroundColorLabel = new JLabel(Language.text("preferences.background_color")+": ");

    final String colorTip = "<html>" + Language.text("preferences.background_color.tip");
    backgroundColorLabel.setToolTipText(colorTip);

    presentColor = new JTextField("      ");
    presentColor.setOpaque(true);
    presentColor.setEnabled(true);
    presentColor.setEditable(false);
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

    selector = new ColorChooser(frame, false,
                                Preferences.getColor("run.present.bgcolor"),
                                Language.text("prompt.ok"),
                                new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        String colorValue = selector.getHexColor();
        colorValue = colorValue.substring(1);  // remove the #
        presentColorHex.setText(colorValue);
        presentColor.setBackground(new Color(PApplet.unhex(colorValue)));
        selector.hide();
      }
    });

    presentColor.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseExited(MouseEvent e) {
        frame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
      }

      @Override
      public void mouseEntered(MouseEvent e) {
        frame.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
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
    errorCheckerBox.addItemListener(e -> {
      warningsCheckerBox.setEnabled(errorCheckerBox.isSelected());
    });


    // [ ] Show Warnings - PDE X

    warningsCheckerBox =
      new JCheckBox(Language.text("preferences.show_warnings"));


    // [ ] Enable Code Completion - PDE X

    codeCompletionBox =
      new JCheckBox(Language.text("preferences.code_completion") +
                    " Ctrl-" + Language.text("preferences.cmd_space"));


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

    JLabel displayLabel = new JLabel(Language.text("preferences.run_sketches_on_display") + ": ");
    final String tip = "<html>" + Language.text("preferences.run_sketches_on_display.tip");
    displayLabel.setToolTipText(tip);
    displaySelectionBox = new JComboBox<>();
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
          Platform.openFolder(Base.getSettingsFolder());
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

    okButton = new JButton(Language.getPrompt("ok"));
    okButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          applyFrame();
          disposeFrame();
        }
      });

    JButton cancelButton = new JButton(Language.getPrompt("cancel"));
    cancelButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          disposeFrame();
        }
      });

    final int buttonWidth = Toolkit.getButtonWidth();
    layout.setHorizontalGroup(layout.createSequentialGroup() // sequential group for border + mainContent + border
      .addGap(Toolkit.BORDER)
      .addGroup(layout.createParallelGroup() // parallel group for rest of the components
          .addComponent(sketchbookLocationLabel)
          .addGroup(layout.createSequentialGroup()
                      .addComponent(sketchbookLocationField)
                      .addComponent(browseButton))
          .addGroup(layout.createSequentialGroup()
                      .addComponent(languageLabel)
                      .addComponent(languageSelectionBox,GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE) // This makes the component non-resizable in the X direction
                      .addComponent(languageRestartLabel))
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
                      .addComponent(zoomLabel)
                      .addComponent(zoomAutoBox)
                      .addComponent(zoomSelectionBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                      .addComponent(zoomRestartLabel))
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
          .addComponent(warningsCheckerBox)
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
                      .addComponent(okButton, buttonWidth, GroupLayout.DEFAULT_SIZE, buttonWidth) // Ok and Cancel buttton are now of size BUTTON_WIDTH
                      .addComponent(cancelButton, buttonWidth, GroupLayout.DEFAULT_SIZE, buttonWidth)
          ))
      .addGap(Toolkit.BORDER)
    );

    layout.setVerticalGroup(layout.createSequentialGroup() // sequential group for border + mainContent + border
      .addGap(Toolkit.BORDER)
      .addComponent(sketchbookLocationLabel)
      .addGroup(layout.createParallelGroup()
                  .addComponent(sketchbookLocationField)
                  .addComponent(browseButton))
      .addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                  .addComponent(languageLabel)
                  .addComponent(languageSelectionBox)
                  .addComponent(languageRestartLabel))
      .addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER).
                  addComponent(fontLabel)
                  .addComponent(fontSelectionBox))
      .addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                  .addComponent(fontSizelabel)
                  .addComponent(fontSizeField)
                  .addComponent(consoleFontSizeLabel)
                  .addComponent(consoleFontSizeField))
      .addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                  .addComponent(zoomLabel)
                  .addComponent(zoomAutoBox)
                  .addComponent(zoomSelectionBox)
                  .addComponent(zoomRestartLabel))
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
      .addGap(Toolkit.BORDER)
      );

    if (Platform.isWindows()){
      autoAssociateBox.setVisible(true);
    }
    // closing the window is same as hitting cancel button

    frame.addWindowListener(new WindowAdapter() {
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

    Toolkit.registerWindowCloseKeys(frame.getRootPane(), disposer);
    Toolkit.setIcon(frame);
    frame.setResizable(false);
    frame.pack();
    frame.setLocationRelativeTo(null);

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


  /** Callback for the folder selector. */
  public void sketchbookCallback(File file) {
    if (file != null) {  // null if cancel or closed
      sketchbookLocationField.setText(file.getAbsolutePath());
    }
  }


  /** Close the window after an OK or Cancel. */
  protected void disposeFrame() {
    frame.dispose();
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

    // The preference will have already been reset when the window was created
    if (displaySelectionBox.isEnabled()) {
      int oldDisplayNum = Preferences.getInteger("run.display");
      int displayNum = -1;
      for (int d = 0; d < displaySelectionBox.getItemCount(); d++) {
        if (displaySelectionBox.getSelectedIndex() == d) {
          displayNum = d + 1;
        }
      }
      if ((displayNum != -1) && (displayNum != oldDisplayNum)) {
        Preferences.setInteger("run.display", displayNum); //$NON-NLS-1$
        // Reset the location of the sketch, the window has changed
        for (Editor editor : base.getEditors()) {
          editor.setSketchLocation(null);
        }
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
      Messages.log("Ignoring invalid font size " + fontSizeField); //$NON-NLS-1$
      fontSizeField.setSelectedItem(Preferences.getInteger("editor.font.size"));
    }

    Preferences.setBoolean("editor.zoom.auto", zoomAutoBox.isSelected());
    Preferences.set("editor.zoom",
                    String.valueOf(zoomSelectionBox.getSelectedItem()));

    try {
      Object selection = consoleFontSizeField.getSelectedItem();
      if (selection instanceof String) {
        // Replace with Integer version
        selection = Integer.parseInt((String) selection);
      }
      Preferences.set("console.font.size", String.valueOf(selection));

    } catch (NumberFormatException e) {
      Messages.log("Ignoring invalid font size " + consoleFontSizeField); //$NON-NLS-1$
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
//    Preferences.setBoolean("pdex.completion.trigger", codeCompletionTriggerBox.isSelected());
    Preferences.setBoolean("pdex.suggest.imports", importSuggestionsBox.isSelected());

    for (Editor editor : base.getEditors()) {
      editor.applyPreferences();
    }
  }


  public void showFrame() {
    editorAntialiasBox.setSelected(Preferences.getBoolean("editor.smooth")); //$NON-NLS-1$
    inputMethodBox.setSelected(Preferences.getBoolean("editor.input_method_support")); //$NON-NLS-1$
    errorCheckerBox.setSelected(Preferences.getBoolean("pdex.errorCheckEnabled"));
    warningsCheckerBox.setSelected(Preferences.getBoolean("pdex.warningsEnabled"));
    warningsCheckerBox.setEnabled(errorCheckerBox.isSelected());
    codeCompletionBox.setSelected(Preferences.getBoolean("pdex.completion"));
    //codeCompletionTriggerBox.setSelected(Preferences.getBoolean("pdex.completion.trigger"));
    //codeCompletionTriggerBox.setEnabled(codeCompletionBox.isSelected());
    importSuggestionsBox.setSelected(Preferences.getBoolean("pdex.suggest.imports"));
    deletePreviousBox.setSelected(Preferences.getBoolean("export.delete_target_folder")); //$NON-NLS-1$

    sketchbookLocationField.setText(Preferences.getSketchbookPath());
    checkUpdatesBox.setSelected(Preferences.getBoolean("update.check")); //$NON-NLS-1$

    int defaultDisplayNum = updateDisplayList();
    int displayNum = Preferences.getInteger("run.display"); //$NON-NLS-1$
    //if (displayNum > 0 && displayNum <= displayCount) {
    if (displayNum < 1 || displayNum > displayCount) {
      displayNum = defaultDisplayNum;
      Preferences.setInteger("run.display", displayNum);
    }
    displaySelectionBox.setSelectedIndex(displayNum-1);

    // This takes a while to load, so run it from a separate thread
    //EventQueue.invokeLater(new Runnable() {
    new Thread(new Runnable() {
      public void run() {
        initFontList();
      }
    }).start();

    fontSizeField.setSelectedItem(Preferences.getInteger("editor.font.size"));
    consoleFontSizeField.setSelectedItem(Preferences.getInteger("console.font.size"));

    boolean zoomAuto = Preferences.getBoolean("editor.zoom.auto");
    if (zoomAuto) {
      zoomAutoBox.setSelected(zoomAuto);
      zoomSelectionBox.setEnabled(!zoomAuto);
    }
    String zoomSel = Preferences.get("editor.zoom");
    int zoomIndex = Toolkit.zoomOptions.index(zoomSel);
    if (zoomIndex != -1) {
      zoomSelectionBox.setSelectedIndex(zoomIndex);
    } else {
      zoomSelectionBox.setSelectedIndex(0);
    }

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
    frame.getRootPane().setDefaultButton(okButton);

    // The pack is called again here second time to fix layout bugs
    // the bugs are not due to groupLayout but due to HTML rendering of components
    // more info can be found here -> https://netbeans.org/bugzilla/show_bug.cgi?id=79967
    frame.pack();

    frame.setVisible(true);
  }


  /**
   * I have some ideas on how we could make Swing even more obtuse for the
   * most basic usage scenarios. Is there someone on the team I can contact?
   * Are you an Oracle staffer reading this? This could be your meal ticket.
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
          fontSelectionBox.setModel(new DefaultComboBoxModel<>(monoFontFamilies));
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


  /**
   * @return the number (1..whatever, not 0-indexed) of the default display
   */
  int updateDisplayList() {
    GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
    GraphicsDevice defaultDevice = ge.getDefaultScreenDevice();
    GraphicsDevice[] devices = ge.getScreenDevices();

    int defaultNum = -1;
    displayCount = devices.length;
    String[] items = new String[displayCount];
    for (int i = 0; i < displayCount; i++) {
      DisplayMode mode = devices[i].getDisplayMode();
      String title = String.format("%d (%d \u2715 %d)",  // or \u00d7?
                                   i + 1, mode.getWidth(), mode.getHeight());
      if (devices[i] == defaultDevice) {
        title += " default";
        defaultNum = i + 1;
      }
      items[i] = title;
    }
    displaySelectionBox.setModel(new DefaultComboBoxModel<>(items));

    // Disable it if you can't actually change the default display
    displaySelectionBox.setEnabled(displayCount != 1);

    // Send back the number (1-indexed) of the default display
    return defaultNum;
  }
}
