/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2012-14 The Processing Foundation
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
 * 
 * Flexible, automated cross-platform layout implemented via GroupLayout.
 * 
 * The order of the displayed elements in the actual Preferences window
 * is respected and mirrored throughout the PreferencesFrame class itself,
 * more specifically in the order of the instances, the initialization
 * of the respective sections and the vertical & horizontal layout.
 * 
 */
public class PreferencesFrame {
  JFrame dialog;

  static final Integer[] FONT_SIZES = { 10, 12, 14, 18, 24, 36, 48 };

  JLabel sketchbookLocationLabel;
  JTextField sketchbookLocationField;
  JButton sketchbookLocationButton;

  JLabel languageLabel;
  JComboBox languageSelectionBox;
  JLabel languageRestartLabel;

  JLabel fontLabel;
  String[] monoFontFamilies;
  JComboBox fontSelectionBox;
  
  JLabel editorFontSizeLabel;
  JComboBox fontSizeField;
  JLabel consoleFontSizeLabel;
  JComboBox consoleSizeField;
  
  JLabel backgroundColorLabel;
  JLabel numberSignLabel;
  JTextField presentColorHex;
  JTextField presentColor;
  ColorChooser selector;

  JCheckBox editorAntialiasBox;
  JCheckBox inputMethodBox;
  JCheckBox errorCheckerBox;
  JCheckBox warningsCheckerBox;
  JCheckBox codeCompletionBox;
  JCheckBox codeCompletionTriggerBox;
  JCheckBox importSuggestionsBox;
  
  JCheckBox memoryOverrideBox;
  JTextField memoryField;
  JLabel memoryMBLabel;

  JCheckBox deletePreviousBox;
  JCheckBox whinyBox;
  JCheckBox checkUpdatesBox;

  JLabel displayLabel;
  JComboBox displaySelectionBox;
  int displayCount;

  JCheckBox autoAssociateBox;
  
  JLabel morePreferencesLabel;
  JLabel sketchbookPathLabel;
  JLabel morePreferencesHintLabel;

  JButton okButton;
  JButton cancelButton;
  
  /** Base object so that updates can be applied to the list of editors. */
  Base base;


  public PreferencesFrame(Base base) {
    this.base = base;
    dialog = new JFrame(Language.text("preferences"));
    dialog.setResizable(false);

    // "Find a place inside where there's joy, and the joy will burn out the pain."
    Container bliss = dialog.getContentPane();
    GroupLayout layout = new GroupLayout(bliss);
    layout.setAutoCreateGaps(true);
    layout.setAutoCreateContainerGaps(true);
    bliss.setLayout(layout);
    
    // Sketchbook location:
    // [...............................]  [ Browse ]

    sketchbookLocationLabel = new JLabel(Language.text("preferences.sketchbook_location")+":");
    sketchbookLocationField = new JTextField(40);
    sketchbookLocationButton = new JButton(Preferences.PROMPT_BROWSE);

    sketchbookLocationButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        File dflt = new File(sketchbookLocationField.getText());
        PApplet.selectFolder(Language.text("preferences.sketchbook_location.popup"),
                             "sketchbookCallback", dflt,
                             PreferencesFrame.this, dialog);
      }
    });

    // Language: [ English ] (requires restart of Processing)

    languageLabel = new JLabel(Language.text("preferences.language")+": ");
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
    languageRestartLabel = new JLabel(" ("+Language.text("preferences.requires_restart")+")");


    // Editor and console font [ Source Code Pro ]

    // Nevermind on this for now.. Java doesn't seem to have a method for 
    // enumerating only the fixed-width (monospaced) fonts. To do this 
    // properly, we'd need to list the fonts, and compare the metrics of 
    // i and M for each. When they're identical (and not degenerate), 
    // we'd call that font fixed width. That's all a very expensive set of 
    // operations, so it should also probably be cached between runs and 
    // updated in the background.

    fontLabel = new JLabel(Language.text("preferences.editor_and_console_font")+": ");
    final String fontTip = "<html>" + Language.text("preferences.editor_and_console_font.tip");
    fontLabel.setToolTipText(fontTip);
    fontSelectionBox = new JComboBox(new Object[] { Toolkit.getMonoFontName() });
    fontSelectionBox.setToolTipText(fontTip);
    fontSelectionBox.setEnabled(false);  // don't enable until fonts are loaded

    // Editor font size [ 12 ]  Console font size [ 10 ]

    editorFontSizeLabel = new JLabel(Language.text("preferences.editor_font_size")+": ");
    fontSizeField = new JComboBox<Integer>(FONT_SIZES);
    fontSizeField.setEditable(true);
    fontSizeField.setSelectedItem(Preferences.getFont("editor.font.size"));
    
    consoleFontSizeLabel = new JLabel(Language.text("preferences.console_font_size")+": ");
    consoleSizeField = new JComboBox<Integer>(FONT_SIZES);
    consoleSizeField.setEditable(true);
    consoleSizeField.setSelectedItem(Preferences.getFont("console.font.size"));

    // Background color when Presenting: # [ 666666 ] [ colorSelector ]

    backgroundColorLabel = new JLabel(Language.text("preferences.background_color")+": ");
    final String colorTip = "<html>" + Language.text("preferences.background_color.tip");
    backgroundColorLabel.setToolTipText(colorTip);

    numberSignLabel = new JLabel("#");
   
    presentColor = new JTextField("      ");
    presentColor.setOpaque(true);
    presentColor.setEnabled(false);
    presentColor.setBorder(new CompoundBorder(BorderFactory.createMatteBorder(
        1, 1, 0, 0, new Color(195, 195, 195)), BorderFactory.createMatteBorder(
        0, 0, 1, 1, new Color(54, 54, 54))));
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
        Preferences.getColor("run.present.bgcolor"), Language.text("prompt.ok"),
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

    // [ ] Use smooth text in editor window
    
    editorAntialiasBox = new JCheckBox(Language.text("preferences.use_smooth_text"));

    // [ ] Enable complex text input (for Japanese et al, requires restart)

    inputMethodBox = new JCheckBox(Language.text("preferences.enable_complex_text_input") +
                    " ("+Language.text("preferences.enable_complex_text_input_example") +
                    ", "+Language.text("preferences.requires_restart")+")");
    
    // [ ] Continuously check for errors - PDE X

    errorCheckerBox = new JCheckBox(Language.text("preferences.continuously_check"));
    
    // [ ] Show Warnings - PDE X

    warningsCheckerBox = new JCheckBox(Language.text("preferences.show_warnings"));
    
    // [ ] Enable Code Completion - PDE X

    codeCompletionBox = new JCheckBox(Language.text("preferences.code_completion"));
    codeCompletionBox.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        // Disable code completion trigger option if CC is disabled
        codeCompletionTriggerBox.setEnabled(codeCompletionBox.isSelected());
      }
    });
    
    // [ ] Toggle Code Completion Trigger - PDE X

    codeCompletionTriggerBox = new JCheckBox(Language.text("preferences.trigger_with")+" Ctrl-"+Language.text("preferences.cmd_space"));

    // [ ] Show import suggestions - PDE X

    importSuggestionsBox = new JCheckBox(Language.text("preferences.suggest_imports"));
    
    // [ ] Increase maximum available memory to [______] MB

    memoryOverrideBox = new JCheckBox(Language.text("preferences.increase_max_memory")+": ");
    memoryField = new JTextField(4);
    memoryOverrideBox.addChangeListener(new ChangeListener() {
      @Override
      public void stateChanged(ChangeEvent e) {
        memoryField.setEnabled(memoryOverrideBox.isSelected());
      }
    });
    memoryMBLabel = new JLabel(" MB");

    // [ ] Delete previous application folder on export

    deletePreviousBox = new JCheckBox(Language.text("preferences.delete_previous_folder_on_export"));
    
    // [ ] Hide tab/toolbar background image

    whinyBox = new JCheckBox(Language.text("preferences.hide_toolbar_background_image") +
                             " ("+Language.text("preferences.requires_restart")+")");

    // [ ] Check for updates on startup

    checkUpdatesBox = new JCheckBox(Language.text("preferences.check_for_updates_on_startup"));

    // Run sketches on display [  1 ]

    displayLabel = new JLabel(Language.text("preferences.run_sketches_on_display")+": ");
    final String tip = "<html>" + Language.text("preferences.run_sketches_on_display.tip");
    displayLabel.setToolTipText(tip);
    displaySelectionBox = new JComboBox();

    // [ ] Automatically associate .pde files with Processing

    autoAssociateBox = new JCheckBox(Language.text("preferences.automatically_associate_pde_files"));
    if (!Base.isWindows()) {
      autoAssociateBox.setVisible(false);
    }

    // More preferences are in the ...

    morePreferencesLabel = new JLabel(Language.text("preferences.file")+":");
    morePreferencesLabel.setForeground(Color.gray);

    sketchbookPathLabel = new JLabel(Preferences.getSketchbookPath());
    final JLabel clickable = sketchbookPathLabel;
    sketchbookPathLabel.addMouseListener(new MouseAdapter() {
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
    
    morePreferencesHintLabel = new JLabel("("+Language.text("preferences.file.hint")+")");
    morePreferencesHintLabel.setForeground(Color.gray);

    // [  OK  ] [ Cancel ]  maybe these should be next to the message?

    okButton = new JButton(Preferences.PROMPT_OK);
    okButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          applyFrame();
          disposeFrame();
        }
      });

    cancelButton = new JButton(Preferences.PROMPT_CANCEL);
    cancelButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          disposeFrame();
        }
      });
    
    // Empty border applied to all JCheckBoxes for correct alignment
    Border emptyBorder = BorderFactory.createEmptyBorder(0, 0, 0, 0);
    editorAntialiasBox.setBorder(emptyBorder);
    inputMethodBox.setBorder(emptyBorder);
    errorCheckerBox.setBorder(emptyBorder);
    warningsCheckerBox.setBorder(emptyBorder);
    codeCompletionBox.setBorder(emptyBorder);
    codeCompletionTriggerBox.setBorder(emptyBorder);
    importSuggestionsBox.setBorder(emptyBorder);
    memoryOverrideBox.setBorder(emptyBorder);
    deletePreviousBox.setBorder(emptyBorder);
    whinyBox.setBorder(emptyBorder);
    checkUpdatesBox.setBorder(emptyBorder);
    autoAssociateBox.setBorder(emptyBorder);
    
    /**
     * GroupLayout requires a vertical and a horizontal layout
     * See: http://docs.oracle.com/javase/tutorial/uiswing/layout/groupExample.html
     * 
     * Features of the chosen layout scheme:
     * - Fully automated (no manual placement)
     * - Flexible (resizes automatically)
     * - Cross-platform (should work on everything)
     * - Less code
     * - Less error-prone
     * - Easy to understand
     * 
     * USAGE:
     * The horizontal layout in the code solution below should mirror the vertical layout.
     * This is easy to understand and it will result in correct placement of all elements.
     */ 
    
    // Vertical layout
    layout.setVerticalGroup(layout.createSequentialGroup()
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
            .addComponent(sketchbookLocationLabel))
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
            .addComponent(sketchbookLocationField)
            .addComponent(sketchbookLocationButton))
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
            .addComponent(languageLabel)
            .addComponent(languageSelectionBox)
            .addComponent(languageRestartLabel))
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
            .addComponent(fontLabel)
            .addComponent(fontSelectionBox))
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
            .addComponent(backgroundColorLabel)
            .addComponent(numberSignLabel)
            .addComponent(presentColorHex)
            .addComponent(presentColor))
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
            .addComponent(editorAntialiasBox))
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
            .addComponent(inputMethodBox))
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
            .addComponent(errorCheckerBox)
            .addComponent(warningsCheckerBox))
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
            .addComponent(codeCompletionBox)
            .addComponent(codeCompletionTriggerBox))
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
            .addComponent(importSuggestionsBox))
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
            .addComponent(memoryOverrideBox)
            .addComponent(memoryField)
            .addComponent(memoryMBLabel))
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
            .addComponent(deletePreviousBox))
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
            .addComponent(whinyBox))
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
            .addComponent(checkUpdatesBox))
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
            .addComponent(displayLabel)
            .addComponent(displaySelectionBox))
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
            .addComponent(autoAssociateBox))
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
            .addComponent(morePreferencesLabel))
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
            .addComponent(sketchbookPathLabel))
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
            .addComponent(morePreferencesHintLabel))
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
            .addComponent(okButton)
            .addComponent(cancelButton))
    );

    // Horizontal layout
    layout.setHorizontalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addComponent(sketchbookLocationLabel)
            .addGroup(layout.createSequentialGroup()
                .addComponent(sketchbookLocationField)
                .addComponent(sketchbookLocationButton))
            .addGroup(layout.createSequentialGroup()
                .addComponent(languageLabel)
                .addComponent(languageSelectionBox)
                .addComponent(languageRestartLabel))
            .addGroup(layout.createSequentialGroup()
                .addComponent(fontLabel)
                .addComponent(fontSelectionBox))
            .addGroup(layout.createSequentialGroup()
                .addComponent(backgroundColorLabel)
                .addComponent(numberSignLabel)
                .addComponent(presentColorHex)
                .addComponent(presentColor))
            .addGroup(layout.createSequentialGroup()
                .addComponent(editorAntialiasBox))
            .addGroup(layout.createSequentialGroup()
                .addComponent(inputMethodBox))
            .addGroup(layout.createSequentialGroup()
                .addComponent(errorCheckerBox)
                .addComponent(warningsCheckerBox))
            .addGroup(layout.createSequentialGroup()
                .addComponent(codeCompletionBox)
                .addComponent(codeCompletionTriggerBox))
            .addGroup(layout.createSequentialGroup()
                .addComponent(importSuggestionsBox))
            .addGroup(layout.createSequentialGroup()
                .addComponent(memoryOverrideBox)
                .addComponent(memoryField)
                .addComponent(memoryMBLabel))
            .addGroup(layout.createSequentialGroup()
                .addComponent(deletePreviousBox))
            .addGroup(layout.createSequentialGroup()
                .addComponent(whinyBox))
            .addGroup(layout.createSequentialGroup()
                .addComponent(checkUpdatesBox))
            .addGroup(layout.createSequentialGroup()
                .addComponent(displayLabel)
                .addComponent(displaySelectionBox))
            .addGroup(layout.createSequentialGroup()
                .addComponent(autoAssociateBox))
            .addGroup(layout.createSequentialGroup()
                .addComponent(morePreferencesLabel))
            .addGroup(layout.createSequentialGroup()
                .addComponent(sketchbookPathLabel))
            .addGroup(layout.createSequentialGroup()
                .addComponent(morePreferencesHintLabel))
            .addGroup(GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(okButton)
                .addComponent(cancelButton))
    );

    // finishing touches...

    // dimensions and placement of Preferences window on the screen

    dialog.pack();
    Dimension screen = Toolkit.getScreenSize();
    dialog.setLocation((screen.width - dialog.getWidth()) / 2, (screen.height - dialog.getHeight()) / 2);

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

    // handle window closing commands for ctrl/cmd-W or hitting ESC.

    bliss.addKeyListener(new KeyAdapter() {
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


  // .................................................................


  /**
   * Close the window after an OK or Cancel.
   */
  protected void disposeFrame() {
    dialog.dispose();
  }


  /**
   * Change internal settings based on what was chosen in the preferences,
   * then send a message to the editor saying that it's time to do the same.
   */
  protected void applyFrame() {
    Preferences.setBoolean("editor.smooth", //$NON-NLS-1$
                           editorAntialiasBox.isSelected());

    Preferences.setBoolean("export.delete_target_folder", //$NON-NLS-1$
                           deletePreviousBox.isSelected());

    boolean wine = whinyBox.isSelected();
    Preferences.setBoolean("header.hide.image", wine); //$NON-NLS-1$
    Preferences.setBoolean("buttons.hide.image", wine); //$NON-NLS-1$
    // Could iterate through editors here and repaint them all, but probably 
    // requires a doLayout() call, and that may have different effects on
    // each platform, and nobody wants to debug/support that.

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
      Object selection = consoleSizeField.getSelectedItem();
      if (selection instanceof String) {
        // Replace with Integer version
        selection = Integer.parseInt((String) selection);
      }
      Preferences.set("console.font.size", String.valueOf(selection));

    } catch (NumberFormatException e) {
      Base.log("Ignoring invalid font size " + consoleSizeField); //$NON-NLS-1$
      consoleSizeField.setSelectedItem(Preferences.getInteger("console.font.size"));
    }
    
    Preferences.setColor("run.present.bgcolor", presentColor.getBackground());
    
    Preferences.setBoolean("editor.input_method_support", inputMethodBox.isSelected()); //$NON-NLS-1$

    if (autoAssociateBox != null) {
      Preferences.setBoolean("platform.auto_file_type_associations", //$NON-NLS-1$
                             autoAssociateBox.isSelected());
    }
    
    Preferences.setBoolean("pdex.errorCheckEnabled", errorCheckerBox.isSelected());
    Preferences.setBoolean("pdex.warningsEnabled", warningsCheckerBox.isSelected());
    Preferences.setBoolean("pdex.ccEnabled", codeCompletionBox.isSelected());
    Preferences.setBoolean("pdex.ccTriggerEnabled", codeCompletionTriggerBox.isSelected());
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
    codeCompletionBox.setSelected(Preferences.getBoolean("pdex.ccEnabled"));
    codeCompletionTriggerBox.setSelected(Preferences.getBoolean("pdex.ccTriggerEnabled"));
    codeCompletionTriggerBox.setEnabled(codeCompletionBox.isSelected());
    importSuggestionsBox.setSelected(Preferences.getBoolean("pdex.importSuggestEnabled"));
    deletePreviousBox.
      setSelected(Preferences.getBoolean("export.delete_target_folder")); //$NON-NLS-1$

    sketchbookLocationField.setText(Preferences.getSketchbookPath());
    checkUpdatesBox.setSelected(Preferences.getBoolean("update.check")); //$NON-NLS-1$

    whinyBox.setSelected(Preferences.getBoolean("header.hide.image") || //$NON-NLS-1$
                         Preferences.getBoolean("buttons.hide.image")); //$NON-NLS-1$

    updateDisplayList();
    int displayNum = Preferences.getInteger("run.display"); //$NON-NLS-1$
    if (displayNum >= 0 && displayNum < displayCount) {
      displaySelectionBox.setSelectedIndex(displayNum);
    }
    
    // This takes a while to load, so run it from a separate thread
    new Thread(new Runnable() {
      public void run() {
        initFontList();
      }
    }).start();
    
    fontSizeField.setSelectedItem(Preferences.getInteger("editor.font.size"));
    consoleSizeField.setSelectedItem(Preferences.getInteger("console.font.size"));

    presentColor.setBackground(Preferences.getColor("run.present.bgcolor"));
    presentColorHex.setText(Preferences.get("run.present.bgcolor").substring(1));
    
    memoryOverrideBox.
      setSelected(Preferences.getBoolean("run.options.memory")); //$NON-NLS-1$
    memoryField.
      setText(Preferences.get("run.options.memory.maximum")); //$NON-NLS-1$
    memoryField.setEnabled(memoryOverrideBox.isSelected());

    if (autoAssociateBox != null) {
      autoAssociateBox.
        setSelected(Preferences.getBoolean("platform.auto_file_type_associations")); //$NON-NLS-1$
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
    if (monoFontFamilies == null) {
      monoFontFamilies = Toolkit.getMonoFontFamilies();
      fontSelectionBox.setModel(new DefaultComboBoxModel(monoFontFamilies));
      String family = Preferences.get("editor.font.family");

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
}
