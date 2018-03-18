/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2017 The Processing Foundation

  This program is free software; you can redistribute it and/or
  modify it under the terms of the GNU General Public License
  version 2, as published by the Free Software Foundation.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software Foundation,
  Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package processing.app.ui;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.border.EmptyBorder;
import javax.imageio.ImageIO;
import javax.swing.UIManager;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.image.BufferedImage;
import java.awt.Dimension;
import java.awt.Color;
import java.awt.Font;
import java.awt.Cursor;
import java.awt.FontFormatException;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.io.File;
import java.io.IOException;

import processing.app.Base;
import processing.app.Platform;
import processing.app.Language;
import processing.app.Preferences;
import processing.core.PApplet;
import processing.data.StringDict;

/**
 * The Welcome class creates a welcome window upon startup
 *
 * It provides links to changes Processing 3
 *
 * If the user is migrating from Processing 2, it provides a
 * prompt asking whether to use the same sketchbook folder
 * as before, or use a new one.
 *
 */

public class Welcome2 extends JFrame {
  Base base;
  StringDict dict;  // used for submitting form values

  /**
   * Constructor for the Welcome window
   *
   * @param Base the current Processing Base
   * @param sketchbook true if the user is migrating from Processing 2
   * @throws IOException if resources cannot be found
   */
  public Welcome2(Base base, boolean sketchbook) throws IOException {
    this.base = base;
    dict = new StringDict();

    // strings used in the GUI
    // should be moved to external files to make tranlsation easier
    final String welcomeText = "Welcome to Processing 3";
    final String whatsNewText = "Read about what's new in 3.0 \u2192";
    final String compatibleText = "Note that some sketches from Processing 2 " +
      "may not be compatible.";
    final String whatHasChangedText = "What has changed?";
    final String newSketchbookText = "Since older sketches may " +
      "not be compatible, we recommend creating a new sketchbook folder, " +
      "so Processing 2 and 3 can happily coexist. This is a one-time " +
      "process.";
    final String readMoreText = "Read more about it";
    final String createNewSketchbookText = "Create a new sketchbook " +
      "folder for use with Processing 3 sketches (recommended!)";
    final String useOldSketchbookText = "Use the existing sketchbook " +
      "for both old and new sketches (may cause conflicts with installed " +
      "libraries)";
    final String showEachTimeText = "Show this welcome message each time";

    // color used for boxes with special information
    final Color insetColor = new Color(224, 253, 251);
    // color used in hyperlinks
    final Color linkColor = new Color(44, 123, 181);

    final String whatsNewUrl = "https://github.com/processing/processing/wiki/Changes-in-3.0";

    Font headerFont;
    Font bodyFont;
//    Font processingSemibold;
//    Font processingSansPro;

    // load fonts
//    try {
//        processingSemibold = Font.createFont(Font.TRUETYPE_FONT,
//          Base.getLibFile("/fonts/ProcessingSansPro-Semibold.ttf"));
//        processingSansPro = Font.createFont(Font.TRUETYPE_FONT,
//          Base.getLibFile("/fonts/ProcessingSansPro-Regular.ttf"));
//    } catch (FontFormatException e) {
//        processingSemibold = UIManager.getDefaults().getFont("Label.font");
//        processingSansPro = UIManager.getDefaults().getFont("Label.font");
//    }
//
//    headerFont = processingSemibold.deriveFont(20f);
//    bodyFont = processingSansPro.deriveFont(12f);

    headerFont = Toolkit.getSansFont(20, Font.BOLD);
    bodyFont = Toolkit.getSansFont(12, Font.PLAIN);

    //Set welcome window title
    setTitle(welcomeText);

    // release frame resources on close
    setDefaultCloseOperation(DISPOSE_ON_CLOSE);

    //Main content panel
    JPanel panel = new JPanel(new GridBagLayout());
    GridBagConstraints c = new GridBagConstraints();
    c.anchor = GridBagConstraints.LINE_START;
    c.fill = GridBagConstraints.HORIZONTAL;

    //int width = sketchbook ? 500 : 400;
    int width = Toolkit.zoom(400);
    int height = Toolkit.zoom(sketchbook ? 400 : 250);

    panel.setPreferredSize(Toolkit.zoom(width, height));
    panel.setBackground(Color.white);

    // Processing logo
    JLabel logo = new JLabel(Toolkit.getLibIcon("/icons/pde-64.png"));
    c.gridx = 0;
    c.gridy = 0;
    panel.add(logo, c);

    // welcome header
    JLabel header = new JLabel(welcomeText);
    header.setFont(headerFont);
    c.gridx = 1;
    c.gridy = 0;
    panel.add(header, c);

    // read what's new link
    JLabel readNew = new JLabel(whatsNewText);
    readNew.setForeground(linkColor);
    readNew.setFont(bodyFont);
    readNew.setCursor(new Cursor(Cursor.HAND_CURSOR));
    readNew.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        Platform.openURL(whatsNewUrl);
      }
    });
    c.gridwidth = 2;
    c.gridx = 0;
    c.gridy = 1;
    panel.add(readNew, c);

    // compatible notice inset
    JPanel compatible = new JPanel(new GridBagLayout());
    GridBagConstraints compc = new GridBagConstraints();
    compatible.setBackground(insetColor);
    compatible.setBorder(new EmptyBorder(10, 0, 10, 0));
    compc.anchor = GridBagConstraints.FIRST_LINE_START;
    compc.fill = GridBagConstraints.HORIZONTAL;

    // compatible notice text
    JLabel compatibleNotice = new JLabel(compatibleText);
    compatibleNotice.setFont(bodyFont);
    compc.gridx = 0;
    compc.gridy = 0;
    compatible.add(compatibleNotice, compc);

    // link to what has changed
    JLabel changed = new JLabel(whatHasChangedText);
    changed.setFont(bodyFont);
    changed.setForeground(linkColor);
    changed.setCursor(new Cursor(Cursor.HAND_CURSOR));
    changed.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        Platform.openURL(whatsNewUrl);
      }
    });
    compc.gridx = 0;
    compc.gridy = 1;
    compatible.add(changed, compc);

    // add compatible notice inset into main panel
    c.gridx = 0;
    c.gridy = 2;
    panel.add(compatible, c);

    //if the user needs to choose a new sketchbook
    if(sketchbook) {
      // create new sketchbook prompt
      JTextArea newSketchbookPrompt = new JTextArea(newSketchbookText);
      newSketchbookPrompt.setFont(bodyFont);
      newSketchbookPrompt.setEditable(false);
      newSketchbookPrompt.setLineWrap(true);
      newSketchbookPrompt.setWrapStyleWord(true);
      c.gridx = 0;
      c.gridy = 3;
      panel.add(newSketchbookPrompt, c);

      // read more link
      JLabel readMore = new JLabel(readMoreText);
      readMore.setFont(bodyFont);
      c.gridx = 0;
      c.gridy = 4;
      panel.add(readMore, c);

      // inset for choose sketchbook
      JPanel chooseSketchbook = new JPanel(new GridBagLayout());
      compatible.setBorder(new EmptyBorder(10, 0, 10, 0));
      GridBagConstraints choosec = new GridBagConstraints();
      choosec.fill = GridBagConstraints.HORIZONTAL;
      choosec.anchor = GridBagConstraints.LINE_START;
      chooseSketchbook.setBackground(insetColor);

      // sketchbookGroup contains radio buttons for selection
      ButtonGroup sketchbookGroup = new ButtonGroup();

      // create a new sketchbook for Processing 3 option
      JRadioButton createNew = new JRadioButton(createNewSketchbookText);
      sketchbookGroup.add(createNew);
      createNew.setSelected(true);
      createNew.setFont(bodyFont);
      createNew.addItemListener(new ItemListener() {
        @Override
        public void itemStateChanged(ItemEvent e) {
          if (e.getStateChange() == ItemEvent.SELECTED) {
            dict.set("sketchbook", "create_new");
          }
        }
      });
      // set default
      dict.set("sketchbook", "create_new");
      choosec.gridx = 0;
      choosec.gridy = 0;
      chooseSketchbook.add(createNew, choosec);

      // share sketchbook with Processing 2 option
      JRadioButton useOld = new JRadioButton("<html>" + useOldSketchbookText);
      sketchbookGroup.add(useOld);
      useOld.setFont(bodyFont);
      useOld.addItemListener(new ItemListener() {
        @Override
        public void itemStateChanged(ItemEvent e) {
          if (e.getStateChange() == ItemEvent.SELECTED) {
            dict.set("sketchbook", "use_existing");
          }
        }
      });
      choosec.gridx = 0;
      choosec.gridy = 1;
      chooseSketchbook.add(useOld, choosec);

      // add choose sketchbook inset into main panel
      c.gridx = 0;
      c.gridy = 5;
      panel.add(chooseSketchbook, c);
    }

    // show welcome each time checkbox
    // fixes https://github.com/processing/processing/issues/3912
    JCheckBox showEachTime = new JCheckBox("<html>" + showEachTimeText);
    showEachTime.setSelected(true);
    showEachTime.setFont(bodyFont);
    showEachTime.addItemListener(new ItemListener() {
      @Override
      public void itemStateChanged(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED) {
          dict.set("show_each_time", "on");
        }
        else if (e.getStateChange() == ItemEvent.DESELECTED) {
          dict.set("show_each_time", "off");
        }
      }
    });
    // set default
    dict.set("show_each_time", "on");
    c.gridx = 0;
    c.gridy = 6;
    panel.add(showEachTime, c);

    // get started (submit) button
    JButton getStarted = new JButton("Get Started");
    getStarted.setFont(bodyFont);
    getStarted.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        handleSubmit(dict);
      }
    });
    c.fill = GridBagConstraints.NONE;
    c.gridx = 0;
    c.gridy = 7;
    c.anchor = GridBagConstraints.LAST_LINE_END;
    panel.add(getStarted, c);

    add(panel);
    pack();

    // adds submit function to closing the window
    // fixes https://github.com/processing/processing/issues/3911
    addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        if (!sketchbook) {
          handleSubmit(dict);
        }
      }
    });

    // center window on the screen
    setLocationRelativeTo(null);

    setVisible(true);
  }


  /**
   * Handles the form submission, called when 'Get Started' button is clicked
   * or when the window is closed
   *
   * @param dict a StringDict containing form options
   */
  public void handleSubmit(StringDict dict) {
    // sketchbook = "create_new" or "use_existing"
    // show_each_time = "on" or <not param>
    // dict.print();

    String sketchbookAction = dict.get("sketchbook", null);
    if ("create_new".equals(sketchbookAction)) {
      // open file dialog
      // on affirmative selection, update sketchbook folder
      // String path = Preferences.getSketchbookPath() + "3";
      //      File folder = new File(path);
      //      folder.mkdirs();
      File folder = new File(Preferences.getSketchbookPath()).getParentFile();
      PApplet.selectFolder(Language.text("preferences.sketchbook_location.popup"),
                           "sketchbookCallback", folder,
                           this, this);
    }

    // If un-checked, the key won't be in the dict, so null will be passed
    boolean keepShowing = "on".equals(dict.get("show_each_time", null));
    Preferences.setBoolean("welcome.show", keepShowing);
    Preferences.save();

    handleClose();
  }


  /**
   * Callback for the folder selector, used when user chooses a new sketchbook
   * for Processing 3
   *
   * @param folder the path to the new sketcbook
   */
  public void sketchbookCallback(File folder) {
    if (folder != null) {
      if (base != null) {
        base.setSketchbookFolder(folder);
      } else {
        System.out.println("user selected " + folder);
      }
    }
  }


  /**
   * Closes the window
   */
  public void handleClose() {
    dispose();
  }
}
