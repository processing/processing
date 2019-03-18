/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2012-19 The Processing Foundation
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

package processing.app.ui;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.GroupLayout.Group;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import processing.app.Language;
import processing.app.Platform;
import processing.app.Sketch;


/**
 * Find & Replace window for the Processing editor.
 */
public class FindReplace extends JFrame {
  Editor editor;

  static final int BORDER = Platform.isMacOS() ? 20 : 13;

  JTextField findField;
  JTextField replaceField;
  static String findString;
  static String replaceString;

  JButton replaceButton;
  JButton replaceAllButton;
  JButton replaceAndFindButton;
  JButton previousButton;
  JButton findButton;

  JCheckBox ignoreCaseBox;
  static boolean ignoreCase = true;

  JCheckBox allTabsBox;
  static boolean allTabs = false;

  JCheckBox wrapAroundBox;
  static boolean wrapAround = true;


  public FindReplace(Editor editor) {
    super(Language.text("find"));
    this.editor = editor;

    Container pain = getContentPane();

    JLabel findLabel = new JLabel(Language.text("find.find"));
    JLabel replaceLabel = new JLabel(Language.text("find.replace_with"));
    findField = new JTextField();
    replaceField = new JTextField();

    if (findString != null) findField.setText(findString);
    if (replaceString != null) replaceField.setText(replaceString);

    ignoreCaseBox = new JCheckBox(Language.text("find.ignore_case"));
    ignoreCaseBox.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          ignoreCase = ignoreCaseBox.isSelected();
        }
      });
    ignoreCaseBox.setSelected(ignoreCase);

    allTabsBox = new JCheckBox(Language.text("find.all_tabs"));
    allTabsBox.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          allTabs = allTabsBox.isSelected();
        }
      });
    allTabsBox.setSelected(allTabs);
    allTabsBox.setEnabled(true);

    wrapAroundBox = new JCheckBox(Language.text("find.wrap_around"));
    wrapAroundBox.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          wrapAround = wrapAroundBox.isSelected();
        }
      });
    wrapAroundBox.setSelected(wrapAround);

    GroupLayout layout = new GroupLayout(pain);
    pain.setLayout(layout);
    layout.setAutoCreateGaps(true);
    layout.setAutoCreateContainerGaps(true);

    // To hold the buttons in the specified order depending on the OS
    Group buttonsHorizontalGroup = layout.createSequentialGroup();

    replaceAllButton = new JButton(Language.text("find.btn.replace_all"));
    replaceButton = new JButton(Language.text("find.btn.replace"));
    replaceAndFindButton = new JButton(Language.text("find.btn.replace_and_find"));
    previousButton = new JButton(Language.text("find.btn.previous"));
    findButton = new JButton(Language.text("find.btn.find"));

    // ordering is different on mac versus pc
    if (Platform.isMacOS()) {
      buttonsHorizontalGroup.addComponent(replaceAllButton)
        .addComponent(replaceButton)
        .addComponent(replaceAndFindButton)
        .addComponent(previousButton)
        .addComponent(findButton);

    } else {
      buttonsHorizontalGroup.addComponent(findButton)
        .addComponent(previousButton)
        .addComponent(replaceAndFindButton)
        .addComponent(replaceButton)
        .addComponent(replaceAllButton);
    }
    setFound(false);

    Group buttonsVerticalGroup = layout.createParallelGroup(); // Creates group for arranging buttons vertically
    buttonsVerticalGroup.addComponent(findButton)
    .addComponent(previousButton)
    .addComponent(replaceAndFindButton)
    .addComponent(replaceButton)
    .addComponent(replaceAllButton);

    layout.setHorizontalGroup(layout
        .createSequentialGroup()
        .addGap(BORDER)
        .addGroup(layout.createParallelGroup()
            .addGroup(GroupLayout.Alignment.TRAILING, // TRAILING makes everything right alinged
                layout.createSequentialGroup()
                    .addGap(replaceLabel.getPreferredSize().width
                        - findLabel.getPreferredSize().width)
                    .addComponent(findLabel)
                    .addComponent(findField))
            .addGroup(GroupLayout.Alignment.TRAILING,
                layout.createSequentialGroup()
                    .addComponent(replaceLabel)
                    .addGroup(layout.createParallelGroup()
                        .addComponent(replaceField)
                        .addGroup(GroupLayout.Alignment.LEADING,
                                  layout.createSequentialGroup()
                                      .addComponent(ignoreCaseBox)
                                      .addComponent(allTabsBox)
                                      .addComponent(wrapAroundBox)
                                      .addGap(0))))
            .addGroup(GroupLayout.Alignment.CENTER,buttonsHorizontalGroup))
        .addGap(BORDER)
    );

//    layout.linkSize(SwingConstants.HORIZONTAL, findButton, replaceButton, replaceAllButton, replaceAndFindButton, previousButton);

    layout.setVerticalGroup(layout.createSequentialGroup()
      .addGap(BORDER)
      .addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
          .addComponent(findLabel)
          .addComponent(findField))
      .addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
          .addComponent(replaceLabel)
          .addComponent(replaceField))
      .addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
          .addComponent(ignoreCaseBox)
          .addComponent(allTabsBox)
          .addComponent(wrapAroundBox))
          .addGroup(buttonsVerticalGroup)
      .addGap(BORDER)
      );

    setLocationRelativeTo(null); // center
    Dimension size = layout.preferredLayoutSize(pain);
    setSize(size.width, size.height);
    Dimension screen = Toolkit.getScreenSize();
    setLocation((screen.width - size.width) / 2,
                      (screen.height - size.height) / 2);

    replaceButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        replace();
      }
    });

    replaceAllButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        replaceAll();
      }
    });

    replaceAndFindButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        replaceAndFindNext();
      }
    });

    findButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        findNext();
      }
    });

    previousButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        findPrevious();
      }
    });

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
    Toolkit.registerWindowCloseKeys(getRootPane(), new ActionListener() {
      public void actionPerformed(ActionEvent actionEvent) {
        handleClose();
      }
    });
    Toolkit.setIcon(this);

    // hack to to get first field to focus properly on osx
    addWindowListener(new WindowAdapter() {
      public void windowActivated(WindowEvent e) {
        //System.out.println("activating");
        /*boolean ok =*/ findField.requestFocusInWindow();
        //System.out.println("got " + ok);
        findField.selectAll();
      }
    });
    pack();
    setResizable(true);
    setLocationRelativeTo(null);
  }


  public void handleClose() {
    //System.out.println("handling close now");
    findString = findField.getText();
    replaceString = replaceField.getText();

    // this object should eventually become dereferenced
    setVisible(false);
  }


  // look for the next instance of the find string to be found
  // once found, select it (and go to that line)
  private boolean find(boolean wrap, boolean backwards) {
    String searchTerm = findField.getText();

    // this will catch "find next" being called when no search yet
    if (searchTerm.length() != 0) {
      String text = editor.getText();

      // Started work on find/replace across tabs. These two variables store
      // the original tab and selection position so that it knew when to stop
      // rotating through.
      Sketch sketch = editor.getSketch();
      int tabIndex = sketch.getCurrentCodeIndex();
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
        if (nextIndex == -1 && wrap && !allTabs) {
          // if wrapping, a second chance is ok, start from beginning
          nextIndex = text.indexOf(searchTerm, 0);

        } else if (nextIndex == -1 && allTabs) {
          // For searching in all tabs, wrapping always happens.

          int tempIndex = tabIndex;
          // Look for searchterm in all tabs.
          while (tabIndex <= sketch.getCodeCount() - 1) {
            if (tabIndex == sketch.getCodeCount() - 1) {
              // System.out.println("wrapping.");
              tabIndex = -1;
            } else if (tabIndex == sketch.getCodeCount() - 1) {
              break;
            }

            try {
              Document doc = sketch.getCode(tabIndex + 1).getDocument();
              if(doc != null) {
                text = doc.getText(0, doc.getLength()); // this thing has the latest changes
              }
              else {
                text = sketch.getCode(tabIndex + 1).getProgram(); // not this thing.
              }
            } catch (BadLocationException e) {
              e.printStackTrace();
            }
            tabIndex++;
            if (ignoreCase) {
              text = text.toLowerCase();
            }
            nextIndex = text.indexOf(searchTerm, 0);

            if (nextIndex != -1  || tabIndex == tempIndex) {
              break;
            }
          }

          // searchterm wasn't found in any of the tabs.
          // No tab switching should happen, restore tabIndex
          if (nextIndex == -1) {
            tabIndex = tempIndex;
          }
        }
      } else {
        //int selectionStart = editor.textarea.getSelectionStart();
        int selectionStart = editor.getSelectionStart()-1;

        if (selectionStart >= 0) {
          nextIndex = text.lastIndexOf(searchTerm, selectionStart);
        } else {
          nextIndex = -1;
        }
        if (wrap &&  !allTabs && nextIndex == -1) {
          // if wrapping, a second chance is ok, start from the end
          nextIndex = text.lastIndexOf(searchTerm);

        } else if (nextIndex == -1 && allTabs) {
          int tempIndex = tabIndex;
          // Look for search term in previous tabs.
          while (tabIndex >= 0) {
            if (tabIndex == 0) {
              //System.out.println("wrapping.");
              tabIndex = sketch.getCodeCount();
            } else if (tabIndex == 0) {
              break;
            }
            try {
              Document doc = sketch.getCode(tabIndex - 1).getDocument();
              if(doc != null) {
                text = doc.getText(0, doc.getLength()); // this thing has the latest changes
              }
              else {
                text = sketch.getCode(tabIndex - 1).getProgram(); // not this thing.
              }
            } catch (BadLocationException e) {
              e.printStackTrace();
            }
            tabIndex--;
            if (ignoreCase) {
              text = text.toLowerCase();
            }
            nextIndex = text.lastIndexOf(searchTerm);

            if (nextIndex != -1  || tabIndex == tempIndex) {
              break;
            }
          }

          // search term wasn't found in any of the tabs.
          // No tab switching should happen, restore tabIndex
          if (nextIndex == -1) {
            tabIndex = tempIndex;
          }
        }
      }

      if (nextIndex != -1) {
        if (allTabs) {
          sketch.setCurrentCode(tabIndex);
        }
        editor.setSelection(nextIndex, nextIndex + searchTerm.length());
      } else {
        //Toolkit.getDefaultToolkit().beep();
      }
      if (nextIndex != -1) {
        setFound(true);
        return true;
      }
    }
    setFound(false);
    return false;
  }


  protected void setFound(boolean found) {
    replaceButton.setEnabled(found);
    replaceAndFindButton.setEnabled(found);
  }

/**
 * Replace the current selection with whatever's in the
   * replacement text field.
 * @param isCompoundEdit True if the action is to be marked as a copmound edit
 */
  public void replace(boolean isCompoundEdit) {
    editor.setSelectedText(replaceField.getText(), isCompoundEdit);

    editor.getSketch().setModified(true);  // This necessary- calling replace()
    // doesn't seem to mark a sketch as modified

    setFound(false);
  }


  /**
   * Replace the current selection with whatever's in the
   * replacement text field, marking the action as a compound edit.
   */
  public void replace() {
    replace(true);
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
    int startTab = -1;
    int startIndex = -1;
    int counter = 10000;  // prevent infinite loop

    editor.startCompoundEdit();
    while (--counter > 0) {
      if (find(false, false)) {
        int caret = editor.getSelectionStart();
        int stopIndex = startIndex + replaceField.getText().length();
        if (editor.getSketch().getCurrentCodeIndex() == startTab &&
            (caret >= startIndex && (caret <= stopIndex))) {
          // we've reached where we started, so stop the replace
          Toolkit.beep();
          editor.statusNotice("Reached beginning of search!");
          break;
        }
        if (!foundAtLeastOne) {
          foundAtLeastOne = true;
          startTab = editor.getSketch().getCurrentCodeIndex();
          startIndex = editor.getSelectionStart();
        }
        replace(false);
     } else {
        break;
      }
    }
    editor.stopCompoundEdit();
    if (!foundAtLeastOne) {
      Toolkit.beep();
    }
    setFound(false);
  }


  public void setFindText(String t) {
    findField.setText(t);
    findString = t;
  }


  public void findNext() {
    if (!find(wrapAround, false)) {
      Toolkit.beep();
    }
  }


  public void findPrevious() {
    if (!find(wrapAround, true)) {
      Toolkit.beep();
    }
  }


  /**
   * Returns true if find next/previous will work, for graying-
   * out of menu items.
   */
  public boolean canFindNext() {
    return findField.getText().length() != 0;
  }
}
