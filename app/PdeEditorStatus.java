/* -*- mode: jde; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  PdeEditorStatus - panel containing status messages
  Part of the Processing project - http://Proce55ing.net

  Copyright (c) 2001-03 
  Ben Fry, Massachusetts Institute of Technology and 
  Casey Reas, Interaction Design Institute Ivrea

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


public class PdeEditorStatus extends Panel 
  implements ActionListener /*, Runnable*/ {
  static Color bgcolor[];
  static Color fgcolor[];

  static final int NOTICE = 0;
  static final int ERROR  = 1;
  static final int PROMPT = 2;
  static final int EDIT   = 3;

  static final int YES    = 1;
  static final int NO     = 2;
  static final int CANCEL = 3;
  static final int OK     = 4;

  static final String PROMPT_YES     = "Yes";
  static final String PROMPT_NO      = "No";
  static final String PROMPT_CANCEL  = "Cancel";
  static final String PROMPT_OK      = "Ok";
  static final String NO_MESSAGE     = "";

  static final int BUTTON_WIDTH  = 66;
  static final int BUTTON_HEIGHT = 24; //20;

  PdeEditor editor;

  int mode;
  String message;

  Font font;
  FontMetrics metrics;
  int ascent;

  Image offscreen;
  int sizeW, sizeH;
  int imageW, imageH;

  Button yesButton;
  Button noButton;
  Button cancelButton;
  Button okButton;
  TextField editField;
  //boolean editRename;
  //Thread promptThread;
  int response;


  public PdeEditorStatus(PdeEditor editor) {
    this.editor = editor;
    empty();

    if (bgcolor == null) {
      bgcolor = new Color[4];
      bgcolor[0] = PdeBase.getColor("editor.status.notice.bgcolor",
                                    new Color(102, 102, 102));
      bgcolor[1] = PdeBase.getColor("editor.status.error.bgcolor",
                                    new Color(102, 26, 0));
      bgcolor[2] = PdeBase.getColor("editor.status.prompt.bgcolor",
                                    new Color(204, 153, 0));
      bgcolor[3] = PdeBase.getColor("editor.status.prompt.bgcolor",
                                    new Color(204, 153, 0));
      fgcolor = new Color[4];
      fgcolor[0] = PdeBase.getColor("editor.status.notice.fgcolor",
                                    new Color(255, 255, 255));
      fgcolor[1] = PdeBase.getColor("editor.status.error.fgcolor",
                                    new Color(255, 255, 255));
      fgcolor[2] = PdeBase.getColor("editor.status.prompt.fgcolor",
                                    new Color(0, 0, 0));
      fgcolor[3] = PdeBase.getColor("editor.status.prompt.fgcolor",
                                    new Color(0, 0, 0));
    }
  }


  public void empty() {
    mode = NOTICE;
    message = NO_MESSAGE;
    update();
  }


  public void notice(String message) {
    mode = NOTICE;
    this.message = message;
    update();
  }

  public void unnotice(String unmessage) {
    if (message.equals(unmessage)) empty();
  }


  public void error(String message) {
    mode = ERROR;
    this.message = message;
    update();
  }


  /*
  public void run() {
    while (Thread.currentThread() == promptThread) {
      if (response != 0) {
        //System.out.println("stopping prompt thread");
        //promptThread.stop();
        //System.out.println("exiting prompt loop");
        unprompt();
        break;

      } else {
        try {
          //System.out.println("inside prompt thread " + 
          //System.currentTimeMillis());
          Thread.sleep(100);
        } catch (InterruptedException e) { }
      }
    }
  }
  */

  public void prompt(String message) {
    mode = PROMPT;
    this.message = message;

    response = 0;
    yesButton.setVisible(true);
    noButton.setVisible(true);
    cancelButton.setVisible(true);
    yesButton.requestFocus();

    update();

    //promptThread = new Thread(this);
    //promptThread.start();
  }

  // prompt has been handled, re-hide the buttons
  public void unprompt() {
    yesButton.setVisible(false);
    noButton.setVisible(false);
    cancelButton.setVisible(false);
    //promptThread = null;
    empty();
  }


  public void edit(String message, String dflt /*, boolean rename*/) {
    mode = EDIT;
    this.message = message;
    //this.editRename = rename;

    response = 0;
    okButton.setVisible(true);
    cancelButton.setVisible(true);
    editField.setVisible(true);
    editField.setText(dflt);
    editField.selectAll();
    editField.requestFocus();

    update();
  }

  public void unedit() {
    okButton.setVisible(false);
    cancelButton.setVisible(false);
    editField.setVisible(false);
    empty();
  }

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

  public void paint(Graphics screen) {
    //if (screen == null) return;
    if (yesButton == null) {
      yesButton = new Button(PROMPT_YES);
      noButton = new Button(PROMPT_NO);
      cancelButton = new Button(PROMPT_CANCEL);
      okButton = new Button(PROMPT_OK);

      // !@#(* aqua ui #($*(( that turtle-neck wearing #(** (#$@)( 
      // os9 seems to work if bg of component is set, but x still a bastard
      if (PdeBase.platform == PdeBase.MACOSX) {
        yesButton.setBackground(bgcolor[PROMPT]);
        noButton.setBackground(bgcolor[PROMPT]);
        cancelButton.setBackground(bgcolor[PROMPT]);
        okButton.setBackground(bgcolor[PROMPT]);
      }
      setLayout(null);

      yesButton.addActionListener(this);
      noButton.addActionListener(this);
      cancelButton.addActionListener(this);
      okButton.addActionListener(this);

      add(yesButton);
      add(noButton);
      add(cancelButton);
      add(okButton);

      yesButton.setVisible(false);
      noButton.setVisible(false);
      cancelButton.setVisible(false);
      okButton.setVisible(false);

      editField = new TextField();
      editField.addActionListener(this);

      if (PdeBase.platform != PdeBase.MACOSX) {
        editField.addKeyListener(new KeyAdapter() {
            protected void noop() { }

            public void keyPressed(KeyEvent event) {
              //System.out.println("got event " + event + "  " + KeyEvent.VK_SPACE);
              int c = event.getKeyChar();
              int code = event.getKeyCode();
            
              if (code == KeyEvent.VK_ENTER) {
                // accept the input
                //editor.skDuplicateRename2(editField.getText(), editRename);
                editor.skSaveAs2(editField.getText());
                unedit();
                event.consume();

              } else if ((code == KeyEvent.VK_BACK_SPACE) ||
                         (code == KeyEvent.VK_DELETE) || 
                         (code == KeyEvent.VK_RIGHT) || 
                         (code == KeyEvent.VK_LEFT) || 
                         (code == KeyEvent.VK_UP) || 
                         (code == KeyEvent.VK_DOWN) || 
                         (code == KeyEvent.VK_HOME) || 
                         (code == KeyEvent.VK_END) || 
                         (code == KeyEvent.VK_SHIFT)) {
                //System.out.println("nothing to see here");
                noop();

              } else if (code == KeyEvent.VK_ESCAPE) {
                unedit();
                editor.buttons.clear();
                event.consume();

                //} else if (c == ' ') {
              } else if (code == KeyEvent.VK_SPACE) {
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

              } else if (c == '_') {
                noop();
                // everything fine

              } else if (((code >= 'A') && (code <= 'Z')) &&
                         (((c >= 'A') && (c <= 'Z')) ||
                          ((c >= 'a') && (c <= 'z')))) {
                // everything fine, catches upper and lower
                noop();

              } else if ((c >= '0') && (c <= '9')) {
                if (editField.getCaretPosition() == 0) {
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
      }
      add(editField);
      editField.setVisible(false);
    }

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
      font = PdeBase.getFont("editor.status.font",
                             new Font("SansSerif", Font.PLAIN, 12));
      g.setFont(font);
      metrics = g.getFontMetrics();
      ascent = metrics.getAscent();
    }

    g.setColor(bgcolor[mode]);
    g.fillRect(0, 0, imageW, imageH);

    g.setColor(fgcolor[mode]);
    g.setFont(font); // needs to be set each time on osx
    g.drawString(message, PdeEditor.INSET_SIZE, (sizeH + ascent) / 2);

    screen.drawImage(offscreen, 0, 0, null);
  }


  protected void setButtonBounds() {
    int top = (sizeH - BUTTON_HEIGHT) / 2;

    int cancelLeft = sizeW - PdeEditor.INSET_SIZE - BUTTON_WIDTH;
    int noLeft = cancelLeft - PdeEditor.INSET_SIZE - BUTTON_WIDTH;
    int yesLeft = noLeft - PdeEditor.INSET_SIZE - BUTTON_WIDTH;

    yesButton.setBounds(yesLeft, top, BUTTON_WIDTH, BUTTON_HEIGHT);
    noButton.setBounds(noLeft, top, BUTTON_WIDTH, BUTTON_HEIGHT);
    cancelButton.setBounds(cancelLeft, top, BUTTON_WIDTH, BUTTON_HEIGHT);

    editField.setBounds(yesLeft-BUTTON_WIDTH, top, 
                        BUTTON_WIDTH*2, BUTTON_HEIGHT);
    okButton.setBounds(noLeft, top, BUTTON_WIDTH, BUTTON_HEIGHT);
  }


  public Dimension getPreferredSize() {
    return getMinimumSize();
  }

  public Dimension getMinimumSize() {
    return new Dimension(300, PdeEditor.GRID_SIZE);
  }

  public Dimension getMaximumSize() {
    return new Dimension(3000, PdeEditor.GRID_SIZE);    
  }


  public void actionPerformed(ActionEvent e) {
    if (e.getSource() == noButton) {
      //System.out.println("clicked no");
      //response = 2;
      // shut everything down, clear status, and return
      unprompt();
      editor.checkModified2();

    } else if (e.getSource() == yesButton) {
      //System.out.println("clicked yes");
      //response = 1;
      // shutdown/clear status, and call checkModified2
      unprompt();
      editor.doSave();  // assuming that something is set? hmm
      //System.out.println("calling checkmodified2");
      editor.checkModified2();

    } else if (e.getSource() == cancelButton) { 
      if (mode == PROMPT) unprompt();
      if (mode == EDIT) unedit();
      editor.buttons.clear();

    } else if (e.getSource() == okButton) {
      String answer = editField.getText();

      if (PdeBase.platform == PdeBase.MACOSX) {
        char unscrubbed[] = editField.getText().toCharArray();
        for (int i = 0; i < unscrubbed.length; i++) {
          if (!(((unscrubbed[i] >= '0') && (unscrubbed[i] <= '9')) ||
                ((unscrubbed[i] >= 'A') && (unscrubbed[i] <= 'Z')) ||
                ((unscrubbed[i] >= 'a') && (unscrubbed[i] <= 'z')))) {
            unscrubbed[i] = '_';
          }
        }
        answer = new String(unscrubbed);
      }
      editor.skSaveAs2(answer);
      //editor.skDuplicateRename2(editField.getText(), editRename);
      unedit();

    } else if (e.getSource() == editField) {
      //System.out.println("editfield: " + e);
    }
  }
}


  /*
  Color noticeBgColor = new Color(102, 102, 102);
  Color noticeFgColor = new Color(255, 255, 255);

  Color errorBgColor = new Color(102, 26, 0);
  Color errorFgColor = new Color(255, 255, 255);

  Color promptBgColor = new Color(204, 153, 0);
  Color promptFgColor = new COlor(0, 0, 0);
  */
