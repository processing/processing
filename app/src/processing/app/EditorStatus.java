/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2004-10 Ben Fry and Casey Reas
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

import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;


/**
 * Panel just below the editing area that contains status messages.
 */
public class EditorStatus extends BasicSplitPaneDivider {  //JPanel {
  static final int HIGH = 28;

  Color[] bgcolor;
  Color[] fgcolor;

  static public final int NOTICE = 0;
  static public final int ERR    = 1;
  static public final int EDIT   = 2;

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

//  JButton cancelButton;
//  JButton okButton;
//  JTextField editField;

  int response;

  boolean indeterminate;
  Thread thread;


  //public EditorStatus(Editor editor) {
  public EditorStatus(BasicSplitPaneUI ui, Editor editor) {
    super(ui);
    this.editor = editor;
    empty();
    updateMode();
  }


  public void updateMode() {
    Mode mode = editor.getMode();
    bgcolor = new Color[] {
      mode.getColor("status.notice.bgcolor"),
      mode.getColor("status.error.bgcolor"),
      mode.getColor("status.edit.bgcolor")
    };

    fgcolor = new Color[] {
      mode.getColor("status.notice.fgcolor"),
      mode.getColor("status.error.fgcolor"),
      mode.getColor("status.edit.fgcolor")
    };

    font = mode.getFont("status.font");
    metrics = null;
  }


  public void empty() {
    mode = NOTICE;
    message = NO_MESSAGE;
    repaint();
  }


  public void notice(String message) {
    mode = NOTICE;
    this.message = message;
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


//  public void edit(String message, String dflt) {
//    mode = EDIT;
//    this.message = message;
//
//    response = 0;
//    okButton.setVisible(true);
//    cancelButton.setVisible(true);
//    editField.setVisible(true);
//    editField.setText(dflt);
//    editField.selectAll();
//    editField.requestFocusInWindow();
//
//    repaint();
//  }


//  public void unedit() {
//    okButton.setVisible(false);
//    cancelButton.setVisible(false);
//    editField.setVisible(false);
//    editor.textarea.requestFocusInWindow();
//    empty();
//  }


  public void startIndeterminate() {
    indeterminate = true;
    thread = new Thread() {
      public void run() {
        while (Thread.currentThread() == thread) {
          repaint();
          try {
            Thread.sleep(1000 / 10);
          } catch (InterruptedException e) { }
        }
      }
    };
    thread.setName("Editor Status");
    thread.start();
  }


  public void stopIndeterminate() {
    indeterminate = false;
    thread = null;
    repaint();
  }


  //public void paintComponent(Graphics screen) {
  public void paint(Graphics screen) {
//    if (okButton == null) setup();

    Dimension size = getSize();
    if ((size.width != sizeW) || (size.height != sizeH)) {
      // component has been resized
      offscreen = null;
    }

    if (offscreen == null) {
      sizeW = size.width;
      sizeH = size.height;
//      setButtonBounds();
      if (Toolkit.highResDisplay()) {
        offscreen = createImage(sizeW*2, sizeH*2);
      } else {
        offscreen = createImage(sizeW, sizeH);
      }
    }

    Graphics g = offscreen.getGraphics();
    /*Graphics2D g2 =*/ Toolkit.prepareGraphics(g);

    g.setFont(font);
    if (metrics == null) {
      metrics = g.getFontMetrics();
      ascent = metrics.getAscent();
    }

    g.setColor(bgcolor[mode]);
    g.fillRect(0, 0, sizeW, sizeH);

    g.setColor(fgcolor[mode]);
    // https://github.com/processing/processing/issues/3265
    if (message != null) {
      g.setFont(font); // needs to be set each time on osx
      g.drawString(message, Preferences.GUI_SMALL, (sizeH + ascent) / 2);
    }

    if (indeterminate) {
      //int x = cancelButton.getX();
      //int w = cancelButton.getWidth();
      int w = Preferences.BUTTON_WIDTH;
      int x = getWidth() - Preferences.GUI_SMALL - w;
      int y = getHeight() / 3;
      int h = getHeight() / 3;
      g.setColor(new Color(0x80000000, true));
      g.drawRect(x, y, w, h);
      for (int i = 0; i < 10; i++) {
        int r = (int) (x + Math.random() * w);
        g.drawLine(r, y, r, y+h);
      }
    }

    screen.drawImage(offscreen, 0, 0, sizeW, sizeH, null);
  }


  /*
  protected void setup() {
    if (okButton == null) {
      cancelButton = new JButton(Preferences.PROMPT_CANCEL);
      okButton = new JButton(Preferences.PROMPT_OK);

      cancelButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          if (mode == EDIT) {
            unedit();
            //editor.toolbar.clear();
          }
        }
      });

      okButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          // answering to rename/new code question
          if (mode == EDIT) {  // this if() isn't (shouldn't be?) necessary
            String answer = editField.getText();
            editor.getSketch().nameCode(answer);
            unedit();
          }
        }
      });

      // !@#(* aqua ui #($*(( that turtle-neck wearing #(** (#$@)(
      // os9 seems to work if bg of component is set, but x still a bastard
      if (Base.isMacOS()) {
        //yesButton.setBackground(bgcolor[EDIT]);
        //noButton.setBackground(bgcolor[EDIT]);
        cancelButton.setBackground(bgcolor[EDIT]);
        okButton.setBackground(bgcolor[EDIT]);
      }
      setLayout(null);

      add(cancelButton);
      add(okButton);

      cancelButton.setVisible(false);
      okButton.setVisible(false);

      editField = new JTextField();
      // disabling, was not in use
      //editField.addActionListener(this);

      //if (Base.platform != Base.MACOSX) {
      editField.addKeyListener(new KeyAdapter() {

          // Grab ESC with keyPressed, because it's not making it to keyTyped
          public void keyPressed(KeyEvent event) {
            if (event.getKeyChar() == KeyEvent.VK_ESCAPE) {
              unedit();
              //editor.toolbar.clear();
              event.consume();
            }
          }

          // use keyTyped to catch when the feller is actually
          // added to the text field. with keyTyped, as opposed to
          // keyPressed, the keyCode will be zero, even if it's
          // enter or backspace or whatever, so the keychar should
          // be used instead. grr.
          public void keyTyped(KeyEvent event) {
            //System.out.println("got event " + event);
            int c = event.getKeyChar();

            if (c == KeyEvent.VK_ENTER) {  // accept the input
              String answer = editField.getText();
              editor.getSketch().nameCode(answer);
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
              // these events are ignored

//            } else if (c == KeyEvent.VK_ESCAPE) {
//              unedit();
//              editor.toolbar.clear();
//              event.consume();

            } else if (c == KeyEvent.VK_SPACE) {
              String t = editField.getText();
              int start = editField.getSelectionStart();
              int end = editField.getSelectionEnd();
              editField.setText(t.substring(0, start) + "_" +
                                t.substring(end));
              editField.setCaretPosition(start+1);
              event.consume();

            } else if ((c == '_') || (c == '.') ||  // allow .pde and .java
                       ((c >= 'A') && (c <= 'Z')) ||
                       ((c >= 'a') && (c <= 'z'))) {
              // these are ok, allow them through

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


  private void setButtonBounds() {
    int top = (sizeH - BUTTON_HEIGHT) / 2;
    int eachButton = Preferences.GUI_SMALL + Preferences.BUTTON_WIDTH;

    int cancelLeft = sizeW      - eachButton;
    int noLeft     = cancelLeft - eachButton;
    int yesLeft    = noLeft     - eachButton;

    //yesButton.setLocation(yesLeft, top);
    //noButton.setLocation(noLeft, top);
    cancelButton.setLocation(cancelLeft, top);
    okButton.setLocation(noLeft, top);

    //yesButton.setSize(Preferences.BUTTON_WIDTH, Preferences.BUTTON_HEIGHT);
    //noButton.setSize(Preferences.BUTTON_WIDTH, Preferences.BUTTON_HEIGHT);
    cancelButton.setSize(Preferences.BUTTON_WIDTH, BUTTON_HEIGHT);
    okButton.setSize(Preferences.BUTTON_WIDTH, BUTTON_HEIGHT);

    // edit field height is awkward, and very different between mac and pc,
    // so use at least the preferred height for now.
    int editWidth = 2*Preferences.BUTTON_WIDTH;
    int editHeight = editField.getPreferredSize().height;
    int editTop = (1 + sizeH - editHeight) / 2;  // add 1 for ceil
    editField.setBounds(yesLeft - Preferences.BUTTON_WIDTH, editTop,
                        editWidth, editHeight);
  }
  */


  public Dimension getPreferredSize() {
    return getMinimumSize();
  }


  public Dimension getMinimumSize() {
    return new Dimension(300, HIGH);
  }


  public Dimension getMaximumSize() {
    return new Dimension(super.getMaximumSize().width, HIGH);
  }


  /*
  public void actionPerformed(ActionEvent e) {
    if (e.getSource() == cancelButton) {
      if (mode == EDIT) unedit();
      //editor.toolbar.clear();

    } else if (e.getSource() == okButton) {
      // answering to rename/new code question
      if (mode == EDIT) {  // this if() isn't (shouldn't be?) necessary
        String answer = editField.getText();
        editor.getSketch().nameCode(answer);
        unedit();
      }
    }
  }
  */
}
