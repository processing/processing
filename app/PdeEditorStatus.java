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
import javax.swing.*;

import sun.awt.AppContext;  // from java.awt.Dialog, for blocking


public class PdeEditorStatus extends JPanel implements ActionListener {
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

  static final String NO_MESSAGE = "";

  PdeEditor editor;

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


  public PdeEditorStatus(PdeEditor editor) {
    this.editor = editor;
    empty();

    if (bgcolor == null) {
      bgcolor = new Color[4];
      bgcolor[0] = PdePreferences.getColor("status.notice.bgcolor");
      bgcolor[1] = PdePreferences.getColor("status.error.bgcolor");
      bgcolor[2] = PdePreferences.getColor("status.prompt.bgcolor");
      bgcolor[3] = PdePreferences.getColor("status.prompt.bgcolor");

      fgcolor = new Color[4];
      fgcolor[0] = PdePreferences.getColor("status.notice.fgcolor");
      fgcolor[1] = PdePreferences.getColor("status.error.fgcolor");
      fgcolor[2] = PdePreferences.getColor("status.prompt.fgcolor");
      fgcolor[3] = PdePreferences.getColor("status.prompt.fgcolor");
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
    mode = ERROR;
    this.message = message;
    //update();
    repaint();
  }



  public void prompt(String message) {
    //System.out.println("prompting...");

    mode = PROMPT;
    this.message = message;

    response = 0;
    yesButton.setVisible(true);
    noButton.setVisible(true);
    cancelButton.setVisible(true);
    yesButton.requestFocus();

    //update();
    repaint();

    /*
    Point upperLeft = new Point(getLocation());
    //Point lowerRight = new Point(upperLeft.x + getBounds().width,
    //                           upperLeft.y + getBounds().height);
    SwingUtilities.convertPointToScreen(upperLeft, this);

    //Dialog dialog = new JDialog(editor.base, "none", true);
    Dialog dialog = new Dialog(editor.base, "none", true);
    //System.out.println(dialog.isDisplayable());
    //System.out.println(dialog.isDisplayable());
    dialog.setBounds(upperLeft.x, upperLeft.y, 
                     getBounds().width, getBounds().height);

    //System.out.println(dialog.isDisplayable());
    //dialog.setModal(true);
    //dialog.undecorated = true;
    dialog.setUndecorated(true);

    System.out.println("showing");

    dialog.show();
    System.out.println(dialog.isDisplayable());
    */

    /*
    //System.out.println(pt);
    System.out.println(Thread.currentThread());

    promptThread = new Thread(this);
    promptThread.start();
    */
  }


  /*
  public void run() {
    //while (Thread.currentThread() == promptThread) {
    synchronized (promptThread) {
    while (promptThread != null) {
      if (response != 0) {
        System.out.println("stopping prompt thread");
        //promptThread.stop();
        promptThread = null;
        System.out.println("exiting prompt loop");
        unprompt();
        break;

      } else {
        try {
          System.out.println("inside prompt thread " + 
          System.currentTimeMillis());
          Thread.sleep(10);
        } catch (InterruptedException e) { }
      }
    }
    System.out.println("exiting prompt thread");
    }
  }
  */


  /**
   * Makes the Dialog visible. If the dialog and/or its owner
   * are not yet displayable, both are made displayable.  The 
   * dialog will be validated prior to being made visible.  
   * If the dialog is already visible, this will bring the dialog 
   * to the front.
   * <p>
   * If the dialog is modal and is not already visible, this call will
   * not return until the dialog is hidden by calling <code>hide</code> or
   * <code>dispose</code>. It is permissible to show modal dialogs from
   * the event dispatching thread because the toolkit will ensure that
   * another event pump runs while the one which invoked this method
   * is blocked. 
   * @see Component#hide
   * @see Component#isDisplayable
   * @see Component#validate
   * @see java.awt.Dialog#isModal
   */


  /*
  // Stores the app context on which event dispatch thread the dialog
  // is being shown. Initialized in show(), used in hideAndDisposeHandler()
  private AppContext showAppContext;
  
  private boolean keepBlocking = false;


  public void show() {
    //if (!isModal()) {
    //conditionalShow();
    //} else {

    // Set this variable before calling conditionalShow(). That
    // way, if the Dialog is hidden right after being shown, we
    // won't mistakenly block this thread.
    keepBlocking = true;

    // Store the app context on which this dialog is being shown.
    // Event dispatch thread of this app context will be sleeping until
    // we wake it by any event from hideAndDisposeHandler().
    showAppContext = AppContext.getAppContext();

    //if (conditionalShow()) {
      // We have two mechanisms for blocking: 1. If we're on the
      // EventDispatchThread, start a new event pump. 2. If we're
      // on any other thread, call wait() on the treelock.

      // keep the KeyEvents from being dispatched
      // until the focus has been transfered
      long time = Toolkit.getEventQueue().getMostRecentEventTime();
      Component predictedFocusOwner = getMostRecentFocusOwner();
      KeyboardFocusManager.getCurrentKeyboardFocusManager().
        enqueueKeyEvents(time, predictedFocusOwner); 

      if (Toolkit.getEventQueue().isDispatchThread()) {
        EventDispatchThread dispatchThread =
          (EventDispatchThread)Thread.currentThread();
        dispatchThread.pumpEventsForHierarchy(new Conditional() {
            public boolean evaluate() {
              return keepBlocking && windowClosingException == null;
            }
          }, this);
      } else {
        synchronized (getTreeLock()) {
          while (keepBlocking && windowClosingException == null) {
            try {
              getTreeLock().wait();
            } catch (InterruptedException e) {
              break;
            }
          }
        }
      }
      KeyboardFocusManager.getCurrentKeyboardFocusManager().
        dequeueKeyEvents(time, predictedFocusOwner);
      if (windowClosingException != null) {
        windowClosingException.fillInStackTrace();
        throw windowClosingException;
      }
      //}
  }
  //}

  void interruptBlocking() {
    hideAndDisposeHandler(); // this is what impl did

    //if (modal) {
    //disposeImpl();
    //} else if (windowClosingException != null) {
    //      windowClosingException.fillInStackTrace();
    //      windowClosingException.printStackTrace();
    //      windowClosingException = null;
    //  }
  }

  final static class WakingRunnable implements Runnable {
    public void run() {
    }
  }

  private void hideAndDisposeHandler() {
    if (keepBlocking) {
      synchronized (getTreeLock()) {
        keepBlocking = false;
        
        if (showAppContext != null) {
          // Wake up event dispatch thread on which the dialog was 
          // initially shown
          SunToolkit.postEvent(showAppContext, 
                               new PeerEvent(this, 
                                             new WakingRunnable(), 
                                             PeerEvent.PRIORITY_EVENT));
        }
        EventQueue.invokeLater(new WakingRunnable());
        getTreeLock().notifyAll();
      }
    }
  }   
  */


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

    repaint(); 
    //update();
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
      font = PdePreferences.getFont("status.font");
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
    g.drawString(message, PdePreferences.GUI_SMALL, (sizeH + ascent) / 2);

    screen.drawImage(offscreen, 0, 0, null);
  }


  protected void setup() {
    if (yesButton == null) {
      yesButton    = new JButton(PdePreferences.PROMPT_YES);
      noButton     = new JButton(PdePreferences.PROMPT_NO);
      cancelButton = new JButton(PdePreferences.PROMPT_CANCEL);
      okButton     = new JButton(PdePreferences.PROMPT_OK);

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

      editField = new JTextField();
      editField.addActionListener(this);

      //if (PdeBase.platform != PdeBase.MACOSX) {
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
              editor.skSaveAs2(editField.getText());
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

            } else if ((c == '_') ||
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
    int top = (sizeH - PdePreferences.BUTTON_HEIGHT) / 2;
    int eachButton = PdePreferences.GUI_SMALL + PdePreferences.BUTTON_WIDTH;

    int cancelLeft = sizeW      - eachButton;
    int noLeft     = cancelLeft - eachButton;
    int yesLeft    = noLeft     - eachButton;

    yesButton.setLocation(yesLeft, top);
    noButton.setLocation(noLeft, top);
    cancelButton.setLocation(cancelLeft, top);
    editField.setLocation(yesLeft - PdePreferences.BUTTON_WIDTH, top);
    okButton.setLocation(noLeft, top);

    yesButton.setSize(   PdePreferences.BUTTON_WIDTH, PdePreferences.BUTTON_HEIGHT);
    noButton.setSize(    PdePreferences.BUTTON_WIDTH, PdePreferences.BUTTON_HEIGHT);
    cancelButton.setSize(PdePreferences.BUTTON_WIDTH, PdePreferences.BUTTON_HEIGHT);
    okButton.setSize(    PdePreferences.BUTTON_WIDTH, PdePreferences.BUTTON_HEIGHT);
    editField.setSize( 2*PdePreferences.BUTTON_WIDTH, PdePreferences.BUTTON_HEIGHT);
  }


  public Dimension getPreferredSize() {
    return getMinimumSize();
  }

  public Dimension getMinimumSize() {
    return new Dimension(300, PdePreferences.GRID_SIZE);
  }

  public Dimension getMaximumSize() {
    return new Dimension(3000, PdePreferences.GRID_SIZE);    
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
      editor.skSaveAs2(answer);
      unedit();

    } else if (e.getSource() == editField) {
      //System.out.println("editfield: " + e);
    }
  }
}
