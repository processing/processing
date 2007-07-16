/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2004-06 Ben Fry and Casey Reas
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

package processing.app;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;


/**
 * Panel just below the editing area that contains status messages.
 */
public class EditorStatus extends JPanel /*implements ActionListener*/ {
  static Color bgcolor[];
  static Color fgcolor[];

  static final int NOTICE = 0;
  static final int ERR    = 1;
  static final int PROMPT = 2;
  static final int EDIT   = 3;

  static final int YES    = 1;
  static final int NO     = 2;
  static final int CANCEL = 3;
  static final int OK     = 4;

  static final String NO_MESSAGE = "";

  Editor editor;

  int mode;
  String message;

  Font font;
  FontMetrics metrics;
  int ascent;

  Image offscreen;
  int sizeW, sizeH;
  int imageW, imageH;

  JButton yesButton;
  JButton noButton;
  JButton cancelButton;
  JButton okButton;
  JTextField editField;

  //Thread promptThread;
  int response;


  public EditorStatus(Editor editor) {
    this.editor = editor;
    empty();

    if (bgcolor == null) {
      bgcolor = new Color[4];
      bgcolor[0] = Preferences.getColor("status.notice.bgcolor");
      bgcolor[1] = Preferences.getColor("status.error.bgcolor");
      bgcolor[2] = Preferences.getColor("status.prompt.bgcolor");
      bgcolor[3] = Preferences.getColor("status.prompt.bgcolor");

      fgcolor = new Color[4];
      fgcolor[0] = Preferences.getColor("status.notice.fgcolor");
      fgcolor[1] = Preferences.getColor("status.error.fgcolor");
      fgcolor[2] = Preferences.getColor("status.prompt.fgcolor");
      fgcolor[3] = Preferences.getColor("status.prompt.fgcolor");
    }
  }


  public void empty() {
    mode = NOTICE;
    message = NO_MESSAGE;
    //update();
    repaint();
  }


  public void notice(String message) {
    mode = NOTICE;
    this.message = message;
    //update();
    repaint();
  }

  public void unnotice(String unmessage) {
    if (message.equals(unmessage)) empty();
  }


  public void error(String message) {
    mode = ERR;
    this.message = message;
    repaint();
  }


  /*
  public void prompt(String message) {
    mode = PROMPT;
    this.message = message;

    response = 0;
    yesButton.setVisible(true);
    noButton.setVisible(true);
    cancelButton.setVisible(true);
    yesButton.requestFocus();

    repaint();
  }


  // prompt has been handled, re-hide the buttons
  public void unprompt() {
    yesButton.setVisible(false);
    noButton.setVisible(false);
    cancelButton.setVisible(false);
    empty();
  }
  */


  public void edit(String message, String dflt) {
    mode = EDIT;
    this.message = message;

    response = 0;
    okButton.setVisible(true);
    cancelButton.setVisible(true);
    editField.setVisible(true);
    editField.setText(dflt);
    editField.selectAll();
    editField.requestFocus();

    repaint();
  }

  public void unedit() {
    okButton.setVisible(false);
    cancelButton.setVisible(false);
    editField.setVisible(false);
    empty();
  }


  /*
  public void update() {
    Graphics g = this.getGraphics();
    try {
      setBackground(bgcolor[mode]);
    } catch (NullPointerException e) { } // if not ready yet
    if (g != null) paint(g);
  }

  public void update(Graphics g) {
    paint(g);
  }
  */


  public void paintComponent(Graphics screen) {
    //if (screen == null) return;
    if (yesButton == null) setup();

    //System.out.println("status.paintComponent");

    Dimension size = getSize();
    if ((size.width != sizeW) || (size.height != sizeH)) {
      // component has been resized

      if ((size.width > imageW) || (size.height > imageH)) {
        // nix the image and recreate, it's too small
        offscreen = null;

      } else {
        // who cares, just resize
        sizeW = size.width;
        sizeH = size.height;
        setButtonBounds();
      }
    }

    if (offscreen == null) {
      sizeW = size.width;
      sizeH = size.height;
      setButtonBounds();
      imageW = sizeW;
      imageH = sizeH;
      offscreen = createImage(imageW, imageH);
    }

    Graphics g = offscreen.getGraphics();
    if (font == null) {
      font = Preferences.getFont("status.font");
      //new Font("SansSerif", Font.PLAIN, 12));
      g.setFont(font);
      metrics = g.getFontMetrics();
      ascent = metrics.getAscent();
    }

    //setBackground(bgcolor[mode]);  // does nothing

    g.setColor(bgcolor[mode]);
    g.fillRect(0, 0, imageW, imageH);

    g.setColor(fgcolor[mode]);
    g.setFont(font); // needs to be set each time on osx
    g.drawString(message, Preferences.GUI_SMALL, (sizeH + ascent) / 2);

    screen.drawImage(offscreen, 0, 0, null);
  }


  protected void setup() {
    if (yesButton == null) {
      yesButton    = new JButton(Preferences.PROMPT_YES);
      noButton     = new JButton(Preferences.PROMPT_NO);
      cancelButton = new JButton(Preferences.PROMPT_CANCEL);
      okButton     = new JButton(Preferences.PROMPT_OK);

      cancelButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          if (mode == EDIT) {
            unedit();
            editor.buttons.clear();
          }
        }
      });
      
      okButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          // answering to rename/new code question
          if (mode == EDIT) {  // this if() isn't (shouldn't be?) necessary
            String answer = editField.getText();
            editor.sketch.nameCode(answer);
            unedit();
          }
        }
      });

      // !@#(* aqua ui #($*(( that turtle-neck wearing #(** (#$@)(
      // os9 seems to work if bg of component is set, but x still a bastard
      if (Base.isMacOS()) {
        yesButton.setBackground(bgcolor[PROMPT]);
        noButton.setBackground(bgcolor[PROMPT]);
        cancelButton.setBackground(bgcolor[PROMPT]);
        okButton.setBackground(bgcolor[PROMPT]);
      }
      setLayout(null);

      /*
      yesButton.addActionListener(this);
      noButton.addActionListener(this);
      cancelButton.addActionListener(this);
      okButton.addActionListener(this);
      */

      add(yesButton);
      add(noButton);
      add(cancelButton);
      add(okButton);

      yesButton.setVisible(false);
      noButton.setVisible(false);
      cancelButton.setVisible(false);
      okButton.setVisible(false);

      editField = new JTextField();
      // disabling, was not in use
      //editField.addActionListener(this);

      //if (Base.platform != Base.MACOSX) {
      editField.addKeyListener(new KeyAdapter() {
          // no-op implemented because of a jikes bug
          //protected void noop() { }

          //public void keyPressed(KeyEvent event) {
          //System.out.println("pressed " + event + "  " + KeyEvent.VK_SPACE);
          //}

          // use keyTyped to catch when the feller is actually
          // added to the text field. with keyTyped, as opposed to
          // keyPressed, the keyCode will be zero, even if it's
          // enter or backspace or whatever, so the keychar should
          // be used instead. grr.
          public void keyTyped(KeyEvent event) {
            //System.out.println("got event " + event + "  " +
            // KeyEvent.VK_SPACE);
            int c = event.getKeyChar();

            if (c == KeyEvent.VK_ENTER) {  // accept the input
              String answer = editField.getText();
              editor.sketch.nameCode(answer);
              unedit();
              event.consume();

              // easier to test the affirmative case than the negative
            } else if ((c == KeyEvent.VK_BACK_SPACE) ||
                       (c == KeyEvent.VK_DELETE) ||
                       (c == KeyEvent.VK_RIGHT) ||
                       (c == KeyEvent.VK_LEFT) ||
                       (c == KeyEvent.VK_UP) ||
                       (c == KeyEvent.VK_DOWN) ||
                       (c == KeyEvent.VK_HOME) ||
                       (c == KeyEvent.VK_END) ||
                       (c == KeyEvent.VK_SHIFT)) {
              //System.out.println("nothing to see here");
              //noop();

            } else if (c == KeyEvent.VK_ESCAPE) {
              unedit();
              editor.buttons.clear();
              event.consume();

            } else if (c == KeyEvent.VK_SPACE) {
              //System.out.println("got a space");
              // if a space, insert an underscore
              //editField.insert("_", editField.getCaretPosition());
              /* tried to play nice and see where it got me
                 editField.dispatchEvent(new KeyEvent(editField,
                 KeyEvent.KEY_PRESSED,
                 System.currentTimeMillis(),
                 0, 45, '_'));
              */
              //System.out.println("start/end = " +
              //                 editField.getSelectionStart() + " " +
              //                 editField.getSelectionEnd());
              String t = editField.getText();
              //int p = editField.getCaretPosition();
              //editField.setText(t.substring(0, p) + "_" + t.substring(p));
              //editField.setCaretPosition(p+1);
              int start = editField.getSelectionStart();
              int end = editField.getSelectionEnd();
              editField.setText(t.substring(0, start) + "_" +
                                t.substring(end));
              editField.setCaretPosition(start+1);
              //System.out.println("consuming event");
              event.consume();

            } else if ((c == '_') || (c == '.') ||  // allow .pde and .java
                       ((c >= 'A') && (c <= 'Z')) ||
                       ((c >= 'a') && (c <= 'z'))) {
              // everything fine, catches upper and lower
              //noop();

            } else if ((c >= '0') && (c <= '9')) {
              // getCaretPosition == 0 means that it's the first char
              // and the field is empty.
              // getSelectionStart means that it *will be* the first
              // char, because the selection is about to be replaced
              // with whatever is typed.
              if ((editField.getCaretPosition() == 0) ||
                  (editField.getSelectionStart() == 0)) {
                // number not allowed as first digit
                //System.out.println("bad number bad");
                event.consume();
              }
            } else {
              event.consume();
              //System.out.println("code is " + code + "  char = " + c);
            }
            //System.out.println("code is " + code + "  char = " + c);
          }
        });
      add(editField);
      editField.setVisible(false);
    }
  }


  protected void setButtonBounds() {
    int top = (sizeH - Preferences.BUTTON_HEIGHT) / 2;
    int eachButton = Preferences.GUI_SMALL + Preferences.BUTTON_WIDTH;

    int cancelLeft = sizeW      - eachButton;
    int noLeft     = cancelLeft - eachButton;
    int yesLeft    = noLeft     - eachButton;

    yesButton.setLocation(yesLeft, top);
    noButton.setLocation(noLeft, top);
    cancelButton.setLocation(cancelLeft, top);
    okButton.setLocation(noLeft, top);

    yesButton.setSize(Preferences.BUTTON_WIDTH, Preferences.BUTTON_HEIGHT);
    noButton.setSize(Preferences.BUTTON_WIDTH, Preferences.BUTTON_HEIGHT);
    cancelButton.setSize(Preferences.BUTTON_WIDTH, Preferences.BUTTON_HEIGHT);
    okButton.setSize(Preferences.BUTTON_WIDTH, Preferences.BUTTON_HEIGHT);

    // edit field height is awkward, and very different between mac and pc,
    // so use at least the preferred height for now.
    int editWidth = 2*Preferences.BUTTON_WIDTH;
    int editHeight = editField.getPreferredSize().height;
    int editTop = (1 + sizeH - editHeight) / 2;  // add 1 for ceil
    editField.setBounds(yesLeft - Preferences.BUTTON_WIDTH, editTop,
                        editWidth, editHeight);
  }


  public Dimension getPreferredSize() {
    return getMinimumSize();
  }

  public Dimension getMinimumSize() {
    return new Dimension(300, Preferences.GRID_SIZE);
  }

  public Dimension getMaximumSize() {
    return new Dimension(3000, Preferences.GRID_SIZE);
  }


  public void actionPerformed(ActionEvent e) {
    if (e.getSource() == cancelButton) {
      if (mode == EDIT) unedit();
      editor.buttons.clear();

    } else if (e.getSource() == okButton) {
      // answering to rename/new code question
      if (mode == EDIT) {  // this if() isn't (shouldn't be?) necessary
        String answer = editField.getText();
        editor.sketch.nameCode(answer);
        unedit();
      }
    }
    
    /*
    if (e.getSource() == noButton) {
      // shut everything down, clear status, and return
      unprompt();
      // don't need to save changes
      editor.checkModified2();

    } else if (e.getSource() == yesButton) {
      // answer was in response to "save changes?"
      unprompt();
      editor.handleSave(true);
      editor.checkModified2();

    } else if (e.getSource() == cancelButton) {
      // don't do anything, don't continue with checkModified2
      if (mode == PROMPT) unprompt();
      else if (mode == EDIT) unedit();
      editor.buttons.clear();

    } else if (e.getSource() == okButton) {
      // answering to "save as..." question
      String answer = editField.getText();
      //editor.handleSaveAs2(answer);
      editor.sketch.nameCode(answer);
      unedit();
    }
    */
  }
}
