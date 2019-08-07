/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2017-19 The Processing Foundation

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

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextArea;

import processing.app.Base;
import processing.app.Language;
import processing.app.Platform;
import processing.app.Preferences;
import processing.core.PApplet;


/**
 * The Welcome class creates a welcome window upon startup
 *
 * It provides links to changes Processing 3
 *
 * If the user is migrating from Processing 2, it provides a
 * prompt asking whether to use the same sketchbook folder
 * as before, or use a new one.
 */
public class Welcome2 extends JFrame {
  Base base;
  boolean newSketchbook;

  /**
   * @param Base the current Processing Base
   * @param oldSketchbook true if the user has a Processing 2 sketchbook
   * @throws IOException if resources cannot be found
   */
  public Welcome2(Base base, boolean oldSketchbook) throws IOException {
    this.base = base;

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

    Font headerFont = Toolkit.getSansFont(20, Font.BOLD);
    Font bodyFont = Toolkit.getSansFont(12, Font.PLAIN);

    //Set welcome window title
    setTitle(welcomeText);

    // release frame resources on close
    setDefaultCloseOperation(DISPOSE_ON_CLOSE);

    //Main content panel
    JPanel panel = new JPanel(new GridBagLayout());
    Toolkit.setBorder(panel, 20, 20, 20, 20);
    GridBagConstraints c = new GridBagConstraints();
    c.anchor = GridBagConstraints.LINE_START;
    c.fill = GridBagConstraints.HORIZONTAL;

    //int width = sketchbook ? 500 : 400;
//    int width = Toolkit.zoom(400);
//    int height = Toolkit.zoom(oldSketchbook ? 400 : 250);
    int width = 400;
    int height = oldSketchbook ? 400 : 250;

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
    Toolkit.setBorder(compatible, 10, 0, 10, 0);
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

    // if the user needs to choose a new sketchbook
    if (oldSketchbook) {
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
      Toolkit.setBorder(chooseSketchbook, 10, 0, 10, 0);
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
            //dict.set("sketchbook", "create_new");
            newSketchbook = true;
          }
        }
      });
      // set default
      //dict.set("sketchbook", "create_new");
      newSketchbook = true;
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
            //dict.set("sketchbook", "use_existing");
            newSketchbook = false;
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
    // handles the Help menu invocation, and also the pref not existing
    showEachTime.setSelected("true".equals(Preferences.get("welcome.show")));
    showEachTime.setFont(bodyFont);
    showEachTime.addItemListener(new ItemListener() {
      @Override
      public void itemStateChanged(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED) {
          Preferences.setBoolean("welcome.show", true);
        } else if (e.getStateChange() == ItemEvent.DESELECTED) {
          Preferences.setBoolean("welcome.show", false);
        }
      }
    });
    // set default
//    dict.set("show_each_time", "on");
    c.gridx = 0;
    c.gridy = 6;
    panel.add(showEachTime, c);

    // get started (submit) button
    JButton getStarted = new JButton("Get Started");
    getStarted.setFont(bodyFont);
    getStarted.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        handleClose();
      }
    });
    c.fill = GridBagConstraints.NONE;
    c.gridx = 0;
    c.gridy = 7;
    c.anchor = GridBagConstraints.LAST_LINE_END;
    panel.add(getStarted, c);

    add(panel);
    pack();

    Toolkit.registerWindowCloseKeys(getRootPane(), new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        handleClose();
      }
    });

    // center window on the screen
    setLocationRelativeTo(null);

    setVisible(true);
  }


  /**
   * Callback for the folder selector, used when user chooses
   * a new sketchbook for Processing 3
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
    Preferences.save();  // save the "show this" setting

    if (newSketchbook) {
      File folder = new File(Preferences.getSketchbookPath()).getParentFile();
      PApplet.selectFolder(Language.text("preferences.sketchbook_location.popup"),
                           "sketchbookCallback", folder, this, this);
    }
    dispose();
  }
}
