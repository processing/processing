/* -*- mode: jde; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  PdeEditorFind - find/replace window for processing
  Part of the Processing project - http://processing.org

  Except where noted, code is written by Ben Fry and is
  Copyright (c) 2001-04 Massachusetts Institute of Technology

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
import javax.swing.*;


public class PdeEditorFind extends JFrame implements ActionListener {
  static final int BIG = 13;
  static final int SMALL = 6;

  PdeEditor editor;

  JTextField findField;
  JTextField replaceField;

  JButton replaceButton; 
  JButton replaceAllButton; 
  JButton findButton;

  JCheckBox ignoreCaseBox;
  boolean ignoreCase;

  boolean found;


  public PdeEditorFind(PdeEditor editor) {
    super("Find");
    setResizable(false);
    this.editor = editor;

    Container pain = getContentPane();
    pain.setLayout(null);

    JLabel findLabel = new JLabel("Find:");
    Dimension d0 = findLabel.getPreferredSize();
    JLabel replaceLabel = new JLabel("Replace with:");
    Dimension d1 = replaceLabel.getPreferredSize();

    pain.add(findLabel);
    pain.add(replaceLabel);

    pain.add(findField = new JTextField(20));
    pain.add(replaceField = new JTextField(20));
    Dimension d2 = findField.getPreferredSize();

    // +1 since it's better to tend downwards
    int yoff = (1 + d2.height - d1.height) / 2;

    findLabel.setBounds(BIG + (d1.width-d0.width) + yoff, BIG, 
			d1.width, d1.height);
    replaceLabel.setBounds(BIG, BIG + d2.height + SMALL + yoff,
			   d1.width, d1.height);

    ignoreCase = true;
    ignoreCaseBox = new JCheckBox("Ignore Case");
    ignoreCaseBox.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) { 
          ignoreCase = ignoreCaseBox.isSelected();
        }
      });
    ignoreCaseBox.setSelected(ignoreCase);
    pain.add(ignoreCaseBox);

    //

    JPanel buttons = new JPanel();
    buttons.setLayout(new FlowLayout());

    // ordering is different on mac versus pc
    if ((PdeBase.platform == PdeBase.MACOSX) || 
	(PdeBase.platform == PdeBase.MACOS9)) {
      buttons.add(replaceButton = new JButton("Replace"));
      buttons.add(replaceAllButton = new JButton("Replace All"));
      buttons.add(findButton = new JButton("Find"));

    } else {
      buttons.add(findButton = new JButton("Find"));
      buttons.add(replaceButton = new JButton("Replace"));
      buttons.add(replaceAllButton = new JButton("Replace All"));
    }
    pain.add(buttons);

    // 0069 TEMPORARILY DISABLED!
    //replaceAllButton.setEnabled(false);

    // to fix ugliness.. normally macosx java 1.3 puts an 
    // ugly white border around this object, so turn it off.
    if (PdeBase.platform == PdeBase.MACOSX) {
      buttons.setBorder(null);
    }

    Dimension d3 = buttons.getPreferredSize();
    //buttons.setBounds(BIG, BIG + d2.height*2 + SMALL + BIG, 
    buttons.setBounds(BIG, BIG + d2.height*3 + SMALL*2 + BIG, 
		      d3.width, d3.height);

    //

    findField.setBounds(BIG + d1.width + SMALL, BIG, 
			d3.width - (d1.width + SMALL), d2.height);
    replaceField.setBounds(BIG + d1.width + SMALL, BIG + d2.height + SMALL, 
			   d3.width - (d1.width + SMALL), d2.height);

    ignoreCaseBox.setBounds(BIG + d1.width + SMALL, 
                            BIG + d2.height*2 + SMALL*2,
                            d3.width, d2.height);

    //

    replaceButton.addActionListener(this);
    replaceAllButton.addActionListener(this);
    findButton.addActionListener(this);

    // you mustn't replace what you haven't found, my son
    replaceButton.setEnabled(false);

    // so that typing will go straight to this field
    findField.requestFocus();

    // make the find button the blinky default
    getRootPane().setDefaultButton(findButton);

    Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();

    int wide = d3.width + BIG*2;
    Rectangle butt = buttons.getBounds();  // how big is your butt?
    int high = butt.y + butt.height + BIG*2;

    setBounds((screen.width - wide) / 2,
	      (screen.height - high) / 2, wide, high);
    //show();
  }


  public void actionPerformed(ActionEvent e) {
    Object source = e.getSource();

    if (source == findButton) {
      find(true);

    } else if (source == replaceButton) {
      replace();

    } else if (source == replaceAllButton) {
      replaceAll();
    }
  }


  // look for the next instance of the find string
  // to be found later than the current caret selection

  // once found, select it (and go to that line)

  public void find(boolean wrap) {
    // in case search len is zero, 
    // otherwise replace all will go into an infinite loop
    found = false;

    String search = findField.getText();
    // this will catch "find next" being called when no search yet
    if (search.length() == 0) return;  

    String text = editor.textarea.getText();

    if (ignoreCase) {
    search = search.toLowerCase();
      text = text.toLowerCase();
    }

    //int selectionStart = editor.textarea.getSelectionStart();
    int selectionEnd = editor.textarea.getSelectionEnd();

    int nextIndex = text.indexOf(search, selectionEnd);
    if (nextIndex == -1) {
      if (wrap) {
        // if wrapping, a second chance is ok, start from beginning
        nextIndex = text.indexOf(search, 0);
      }

      if (nextIndex == -1) {
	found = false;
	replaceButton.setEnabled(false);
	//Toolkit.getDefaultToolkit().beep();
	return;
      }
    }
    found = true;
    replaceButton.setEnabled(true);
    editor.textarea.select(nextIndex, nextIndex + search.length());
  }


  // replace the current selection with whatever's in the 
  // replacement text field

  public void replace() {
    if (!found) return;  // don't replace if nothing found

    // check to see if the document has wrapped around
    // otherwise this will cause an infinite loop
    String sel = editor.textarea.getSelectedText();
    if (sel.equals(replaceField.getText())) {
      found = false; 
      replaceButton.setEnabled(false);
      return;
    }

    editor.textarea.setSelectedText(replaceField.getText());
    //editor.setSketchModified(true);
    //editor.sketch.setCurrentModified(true);
    editor.sketch.setModified();

    // don't allow a double replace
    replaceButton.setEnabled(false);
  }


  // keep doing find and replace alternately until nothing more found

  public void replaceAll() {
    // move to the beginning
    editor.textarea.select(0, 0);

    do {
      find(false);
      replace();
    } while (found);
  }
}
