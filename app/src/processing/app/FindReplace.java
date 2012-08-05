/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2004-12 Ben Fry and Casey Reas
  Copyright (c) 2001-04 Massachusetts Institute of Technology

  This program is free software; you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation, version 2.

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
import javax.swing.*;


/**
 * Find & Replace window for the Processing editor.
 */
public class FindReplace extends JFrame implements ActionListener {

  static final int EDGE = Base.isMacOS() ? 20 : 13;
  static final int SMALL = 6;
  // 12 is correct for Mac, other numbers may be required for other platforms
  static final int BUTTON_GAP = 12;

  Editor editor;

  JTextField findField;
  JTextField replaceField;
  static String findString;
  static String replaceString;

  JButton replaceButton;
  JButton replaceAllButton;
  JButton replaceFindButton;
  JButton previousButton;
  JButton findButton;

  JCheckBox ignoreCaseBox;
  static boolean ignoreCase = true;

  JCheckBox allTabsBox;
  static boolean allTabs = false;

  JCheckBox wrapAroundBox;
  static boolean wrapAround = true;


  public FindReplace(Editor editor) {
    super("Find");
    setResizable(false);
    this.editor = editor;

    Container pain = getContentPane();
    pain.setLayout(null);

    JLabel findLabel = new JLabel("Find:");
    JLabel replaceLabel = new JLabel("Replace with:");
    Dimension labelDimension = replaceLabel.getPreferredSize();

    pain.add(findLabel);
    pain.add(replaceLabel);

    pain.add(findField = new JTextField());
    pain.add(replaceField = new JTextField());
    int fieldHeight = findField.getPreferredSize().height;

    if (findString != null) findField.setText(findString);
    if (replaceString != null) replaceField.setText(replaceString);

    ignoreCaseBox = new JCheckBox("Ignore Case");
    ignoreCaseBox.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          ignoreCase = ignoreCaseBox.isSelected();
        }
      });
    ignoreCaseBox.setSelected(ignoreCase);
    pain.add(ignoreCaseBox);

    allTabsBox = new JCheckBox("All Tabs");
    allTabsBox.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          allTabs = allTabsBox.isSelected();
        }
      });
    allTabsBox.setSelected(allTabs);
    allTabsBox.setEnabled(false);
    pain.add(allTabsBox);

    wrapAroundBox = new JCheckBox("Wrap Around");
    wrapAroundBox.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          wrapAround = wrapAroundBox.isSelected();
        }
      });
    wrapAroundBox.setSelected(wrapAround);
    pain.add(wrapAroundBox);

    JPanel buttons = new JPanel();
    buttons.setLayout(new FlowLayout(FlowLayout.CENTER,BUTTON_GAP, 0));

    // ordering is different on mac versus pc
    if (Base.isMacOS()) {
      buttons.add(replaceAllButton = new JButton("Replace All"));
      buttons.add(replaceButton = new JButton("Replace"));
      buttons.add(replaceFindButton = new JButton("Replace & Find"));
      buttons.add(previousButton = new JButton("Previous"));
      buttons.add(findButton = new JButton("Find"));

    } else {
      buttons.add(findButton = new JButton("Find"));
      buttons.add(previousButton = new JButton("Previous")); // is this the right position for non-Mac?
      buttons.add(replaceFindButton = new JButton("Replace & Find"));
      buttons.add(replaceButton = new JButton("Replace"));
      buttons.add(replaceAllButton = new JButton("Replace All"));
    }
    pain.add(buttons);

    // to fix ugliness.. normally macosx java 1.3 puts an
    // ugly white border around this object, so turn it off.
    if (Base.isMacOS()) {
      buttons.setBorder(null);
    }

    /*
    findField.addFocusListener(new FocusListener() {
        public void focusGained(FocusEvent e) {
          System.out.println("Focus gained " + e.getOppositeComponent());
        }

        public void focusLost(FocusEvent e) {
          System.out.println("Focus lost "); // + e.getOppositeComponent());
          if (e.getOppositeComponent() == null) {
            requestFocusInWindow();
          }
        }
      });
    */

    Dimension buttonsDimension = buttons.getPreferredSize();
    int visibleButtonWidth = buttonsDimension.width - 2 * BUTTON_GAP;
    int fieldWidth = visibleButtonWidth - (labelDimension.width + SMALL);

   // +1 since it's better to tend downwards
    int yoff = (1 + fieldHeight - labelDimension.height) / 2;

    int ypos = EDGE;

    int labelWidth = findLabel.getPreferredSize().width;
    findLabel.setBounds(EDGE + (labelDimension.width-labelWidth), ypos + yoff, //  + yoff was added to the wrong field
                        labelWidth, labelDimension.height);
    findField.setBounds(EDGE + labelDimension.width + SMALL, ypos,
                        fieldWidth, fieldHeight);

    ypos += fieldHeight + SMALL;

    labelWidth = replaceLabel.getPreferredSize().width;
    replaceLabel.setBounds(EDGE + (labelDimension.width-labelWidth), ypos + yoff,
                           labelWidth, labelDimension.height);
    replaceField.setBounds(EDGE + labelDimension.width + SMALL, ypos,
                           fieldWidth, fieldHeight);

    ypos += fieldHeight + SMALL;

    final int third = (fieldWidth - SMALL*2) / 3;
    ignoreCaseBox.setBounds(EDGE + labelDimension.width + SMALL,
                            ypos,
                            third, fieldHeight);

    allTabsBox.setBounds(EDGE + labelDimension.width + SMALL + third + SMALL,
                         ypos,
                         third, fieldHeight);

    //wrapAroundBox.setBounds(EDGE + labelDimension.width + SMALL + (fieldWidth-SMALL)/2 + SMALL,
    wrapAroundBox.setBounds(EDGE + labelDimension.width + SMALL + third*2 + SMALL*2,
                            ypos,
                            third, fieldHeight);

    ypos += fieldHeight + SMALL;

    buttons.setBounds(EDGE-BUTTON_GAP, ypos,
                      buttonsDimension.width, buttonsDimension.height);

    ypos += buttonsDimension.height + EDGE;

    int wide = visibleButtonWidth + EDGE*2;
    int high = ypos;

    pack();
    Insets insets = getInsets();
    setSize(wide + insets.left + insets.right,high + insets.top + insets.bottom);

    setLocationRelativeTo(null); // center

    replaceButton.addActionListener(this);
    replaceAllButton.addActionListener(this);
    replaceFindButton.addActionListener(this);
    findButton.addActionListener(this);
    previousButton.addActionListener(this);

    // you mustn't replace what you haven't found, my son
    // semantics of replace are "replace the current selection with the replace field"
    // so whether we have found before or not is irrelevent
    // replaceButton.setEnabled(false);
    // replaceFindButton.setEnabled(false);

    // make the find button the blinky default
    getRootPane().setDefaultButton(findButton);

    setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    addWindowListener(new WindowAdapter() {
        public void windowClosing(WindowEvent e) {
          handleClose();
        }
      });
    Base.registerWindowCloseKeys(getRootPane(), new ActionListener() {
        public void actionPerformed(ActionEvent actionEvent) {
          //hide();
          handleClose();
        }
      });
    Base.setIcon(this);

    // hack to to get first field to focus properly on osx
    addWindowListener(new WindowAdapter() {
        public void windowActivated(WindowEvent e) {
          //System.out.println("activating");
          /*boolean ok =*/ findField.requestFocusInWindow();
          //System.out.println("got " + ok);
          findField.selectAll();
        }
      });
  }


  public void handleClose() {
    //System.out.println("handling close now");
    findString = findField.getText();
    replaceString = replaceField.getText();

    // this object should eventually become dereferenced
    setVisible(false);
  }


  /*
  public void show() {
    findField.requestFocusInWindow();
    super.show();
    //findField.selectAll();
    //findField.requestFocus();
  }
  */


  public void actionPerformed(ActionEvent e) {
    Object source = e.getSource();

    if (source == findButton) {
      findNext();

    } else if (source == previousButton) {
      findPrevious();

    } else if (source == replaceFindButton) {
      replaceAndFindNext();

    } else if (source == replaceButton) {
      replace();

    } else if (source == replaceAllButton) {
      replaceAll();
    }
  }


  // look for the next instance of the find string to be found
  // once found, select it (and go to that line)
  private boolean find(boolean wrap, boolean backwards) {
    String searchTerm = findField.getText();
    //System.out.println("finding for " + search + " " + findString);
    // this will catch "find next" being called when no search yet
    if (searchTerm.length() == 0) return false;

    String text = editor.getText();

    // Started work on find/replace across tabs. These two variables store
    // the original tab and selection position so that it knew when to stop
    // rotating through.
//    Sketch sketch = editor.getSketch();
//    int tabIndex = sketch.getCurrentCodeIndex();
//    int selIndex = backwards ?
//      editor.getSelectionStart() : editor.getSelectionStop();

    if (ignoreCase) {
      searchTerm = searchTerm.toLowerCase();
      text = text.toLowerCase();
    }

    int nextIndex;
    if (!backwards) {
      //int selectionStart = editor.textarea.getSelectionStart();
      int selectionEnd = editor.getSelectionStop();

      nextIndex = text.indexOf(searchTerm, selectionEnd);
      if (nextIndex == -1 && wrap) {
        // if wrapping, a second chance is ok, start from beginning
        nextIndex = text.indexOf(searchTerm, 0);
      }
    } else {
      //int selectionStart = editor.textarea.getSelectionStart();
      int selectionStart = editor.getSelectionStart()-1;

      if (selectionStart >= 0) {
        nextIndex = text.lastIndexOf(searchTerm, selectionStart);
      } else {
        nextIndex = -1;
      }
      if (wrap && nextIndex == -1) {
        // if wrapping, a second chance is ok, start from the end
        nextIndex = text.lastIndexOf(searchTerm);
      }
    }

    if (nextIndex != -1) {
      editor.setSelection(nextIndex, nextIndex + searchTerm.length());
    } else {
      //Toolkit.getDefaultToolkit().beep();
    }
    return nextIndex != -1;
  }


  /**
   * Replace the current selection with whatever's in the
   * replacement text field.
   */
  public void replace() {
    editor.setSelectedText(replaceField.getText());
    editor.getSketch().setModified(true);  // TODO is this necessary?
  }


  /**
   * Replace the current selection with whatever's in the
   * replacement text field, and then find the next match
   */
  public void replaceAndFindNext() {
    replace();
    findNext();
  }


  /**
   * Replace everything that matches by doing find and replace
   * alternately until nothing more found.
   */
  public void replaceAll() {
    // move to the beginning
    editor.setSelection(0, 0);

    boolean foundAtLeastOne = false;
    while (true) {
      if (find(false, false)) {
        foundAtLeastOne = true;
        replace();
     } else {
        break;
      }
    }
    if (!foundAtLeastOne) {
      Toolkit.getDefaultToolkit().beep();
    }
  }


  public void setFindText(String t) {
    findField.setText(t);
    findString = t;
  }


  public void findNext() {
    if (!find(wrapAround, false)) {
      Toolkit.getDefaultToolkit().beep();
    }
  }


  public void findPrevious() {
    if (!find(wrapAround, true)) {
      Toolkit.getDefaultToolkit().beep();
    }
  }
}
