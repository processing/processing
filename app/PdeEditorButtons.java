/* -*- mode: jde; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  PdeEditorButtons - run/stop/etc buttons for the ide
  Part of the Processing project - http://processing.org

  Except where noted, code is written by Ben Fry and
  Copyright (c) 2001-03 Massachusetts Institute of Technology

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
import java.awt.font.*;
import java.awt.geom.*;
import javax.swing.*;
import javax.swing.event.*;


public class PdeEditorButtons extends JComponent implements MouseInputListener {

  static final String title[] = {
    "", "run", "stop", "new", "open", "save", "export"
  };

  static final int BUTTON_COUNT  = title.length;
  static final int BUTTON_WIDTH  = PdePreferences.GRID_SIZE;
  static final int BUTTON_HEIGHT = PdePreferences.GRID_SIZE;

  static final int NOTHING  = 0;
  static final int RUN      = 1;
  static final int STOP     = 2;

  static final int NEW      = 3;
  static final int OPEN     = 4;
  static final int SAVE     = 5;
  static final int EXPORT   = 6;

  static final int INACTIVE = 0;
  static final int ROLLOVER = 1;
  static final int ACTIVE   = 2;

  PdeEditor editor;
  //Label status;

  Image offscreen;
  int width, height;

  Color bgcolor;

  Image buttons;
  Image inactive[];
  Image rollover[];
  Image active[];
  int currentRollover;
  int currentSelection;

  JPopupMenu popup;

  int buttonCount;
  int state[] = new int[BUTTON_COUNT];
  Image stateImage[];
  int which[]; // mapping indices to implementation

  int x1, x2;
  int y1[], y2[];

  String status;
  Font statusFont;
  Color statusColor;
  int statusY;


  public PdeEditorButtons(PdeEditor editor) {
    this.editor = editor;
    buttons = PdeBase.getImage("buttons.gif", this);

    buttonCount = 0;
    which = new int[BUTTON_COUNT];

    which[buttonCount++] = NOTHING;
    which[buttonCount++] = RUN;
    which[buttonCount++] = STOP;
    which[buttonCount++] = NEW;
    which[buttonCount++] = OPEN;
    which[buttonCount++] = SAVE;
    which[buttonCount++] = EXPORT;

    currentRollover = -1;

    bgcolor = PdePreferences.getColor("buttons.bgcolor");

    status = "";

    //setLayout(null);
    //status = new JLabel();
    statusFont = PdePreferences.getFont("buttons.status.font");
    statusColor = PdePreferences.getColor("buttons.status.color");
    //add(status);

    //status.setBounds(-5, BUTTON_COUNT * BUTTON_HEIGHT, 
    //               BUTTON_WIDTH + 15, BUTTON_HEIGHT);
    //status.setAlignment(Label.CENTER);
    statusY = (BUTTON_COUNT + 1) * BUTTON_HEIGHT;

    addMouseListener(this);
    addMouseMotionListener(this);
  }


  /*
  public void update() {
    paint(this.getGraphics());
  }

  public void update(Graphics g) {
    paint(g);
  }
  */

  //public void paintComponent(Graphics g) {
  //super.paintComponent(g);
  //}


  public void paintComponent(Graphics screen) {
    if (inactive == null) {
      inactive = new Image[BUTTON_COUNT];
      rollover = new Image[BUTTON_COUNT];
      active   = new Image[BUTTON_COUNT];

      //state = new int[BUTTON_COUNT];

      for (int i = 0; i < BUTTON_COUNT; i++) {
        inactive[i] = createImage(BUTTON_WIDTH, BUTTON_HEIGHT);
        Graphics g = inactive[i].getGraphics();
        g.drawImage(buttons, -(i*BUTTON_WIDTH), -2*BUTTON_HEIGHT, null);

        rollover[i] = createImage(BUTTON_WIDTH, BUTTON_HEIGHT);
        g = rollover[i].getGraphics();
        g.drawImage(buttons, -(i*BUTTON_WIDTH), -1*BUTTON_HEIGHT, null);

        active[i] = createImage(BUTTON_WIDTH, BUTTON_HEIGHT);
        g = active[i].getGraphics();
        g.drawImage(buttons, -(i*BUTTON_WIDTH), -0*BUTTON_HEIGHT, null);
      }

      state = new int[buttonCount];
      stateImage = new Image[buttonCount];
      for (int i = 0; i < buttonCount; i++) {
        setState(i, INACTIVE, false);
      }
    }
    Dimension size = size();
    if ((offscreen == null) || 
        (size.width != width) || (size.height != height)) {
      offscreen = createImage(size.width, size.height);
      width = size.width;
      height = size.height;

      x1 = 0; 
      x2 = BUTTON_WIDTH;

      y1 = new int[buttonCount];
      y2 = new int[buttonCount];

      int offsetY = 0;
      for (int i = 0; i < buttonCount; i++) {
        y1[i] = offsetY;
        y2[i] = offsetY + BUTTON_HEIGHT;
        offsetY = y2[i];
      }
    }
    Graphics g = offscreen.getGraphics();
    g.setColor(bgcolor); //getBackground());
    g.fillRect(0, 0, width, height);

    for (int i = 0; i < buttonCount; i++) {
      //g.drawImage(stateImage[i], x1[i], y1, null);
      g.drawImage(stateImage[i], x1, y1[i], null);
    }

    g.setColor(statusColor);
    g.setFont(statusFont);

    // if i ever find the guy who wrote the java2d api, 
    // i will hurt him. or just laugh in his face. or pity him.
    Graphics2D g2 = (Graphics2D) g;
    FontRenderContext frc = g2.getFontRenderContext();
    float statusW = (float) statusFont.getStringBounds(status, frc).getWidth();
    float statusX = (getSize().width - statusW) / 2;

    //int statusWidth = g.getFontMetrics().stringWidth(status);
    //int statusX = (getSize().width - statusWidth) / 2;

    g2.drawString(status, statusX, statusY);
    screen.drawImage(offscreen, 0, 0, null);
  }


  public void mouseMoved(MouseEvent e) {
    // mouse events before paint();
    if (state == null) return; 

    if (state[OPEN] != INACTIVE) {
      // avoid flicker, since there will probably be an update event
      setState(OPEN, INACTIVE, false);
    }
    //System.out.println(e);
    //mouseMove(e);
    handleMouse(e.getX(), e.getY());
  }


  public void mouseDragged(MouseEvent e) { }


  public void handleMouse(int x, int y) {
    if (currentRollover != -1) {
      if ((y > y1[currentRollover]) && (x > x1) &&
          (y < y2[currentRollover]) && (x < x2)) {
        return;

      } else {
        setState(currentRollover, INACTIVE, true);
        messageClear(title[currentRollover]);
        currentRollover = -1;
      }
    }
    int sel = findSelection(x, y);
    if (sel == -1) return;
    
    if (state[sel] != ACTIVE) {
      setState(sel, ROLLOVER, true);
      currentRollover = sel;
    }
  }


  private int findSelection(int x, int y) {
    // if app loads slowly and cursor is near the buttons 
    // when it comes up, the app may not have time to load
    if ((y1 == null) || (y2 == null)) return -1;

    for (int i = 0; i < buttonCount; i++) {
      if ((x > x1) && (y > y1[i]) &&
          (x < x2) && (y < y2[i])) {
        //if ((x > x1[i]) && (y > y1) &&
        //(x < x2[i]) && (y < y2)) {
        return i;
      }
    }
    return -1;
  }


  private void setState(int slot, int newState, boolean updateAfter) {
    //if (inactive == null) return;
    state[slot] = newState;
    switch (newState) {
    case INACTIVE:
      stateImage[slot] = inactive[which[slot]]; 
      break;
    case ACTIVE: 
      stateImage[slot] = active[which[slot]]; 
      break;
    case ROLLOVER: 
      stateImage[slot] = rollover[which[slot]]; 
      message(title[which[slot]]);
      break;
    }
    if (updateAfter) repaint(); // changed for swing from update();
  }


  public void mouseEntered(MouseEvent e) {
    //mouseMove(e);
    handleMouse(e.getX(), e.getY());
  }


  public void mouseExited(MouseEvent e) {
    if (state[OPEN] != INACTIVE) {
      setState(OPEN, INACTIVE, true);
    }

    // kludge
    //for (int i = 0; i < BUTTON_COUNT; i++) {
    //messageClear(title[i]);
    //}
    status = "";
    //mouseMove(e);
    handleMouse(e.getX(), e.getY());
  }

  int wasDown = -1;


  public void mousePressed(MouseEvent e) {
    int x = e.getX();
    int y = e.getY();
    
    int sel = findSelection(x, y);
    ///if (sel == -1) return false;
    if (sel == -1) return;
    currentRollover = -1;
    currentSelection = sel;
    setState(sel, ACTIVE, true);

    if (currentSelection == OPEN) {
      if (popup == null) {
        //popup = new JPopupMenu();
        popup = editor.sketchbook.getPopupMenu();
        add(popup);
      }
      //editor.sketchbook.rebuildPopup(popup);
      popup.show(this, x, y);
    }
  }


  public void mouseClicked(MouseEvent e) { }


  public void mouseReleased(MouseEvent e) {
    switch (currentSelection) {
      case RUN:    editor.handleRun(e.isShiftDown()); break;
      case STOP:   setState(RUN, INACTIVE, true); editor.handleStop(); break;
      case OPEN:   setState(OPEN, INACTIVE, true); break;
      case NEW:    editor.handleNew(); break;
      case SAVE:   editor.handleSave2(); break;
      case EXPORT: editor.handleExport(); break;
    }
    currentSelection = -1;
  }


  public void clear() { // (int button) {
    if (inactive == null) return;

    // skip the run button, do the others
    for (int i = 1; i < buttonCount; i++) {
      setState(i, INACTIVE, false);
    }
    repaint(); // changed for swing from update();
  }


  public void run() {
    if (inactive == null) return;
    clear();
    setState(RUN, ACTIVE, true);
  }


  public void running(boolean yesno) {
    setState(RUN, yesno ? ACTIVE : INACTIVE, true);
  }


  public void clearRun() {
    if (inactive == null) return;
    setState(RUN, INACTIVE, true);
  }


  public void message(String msg) {
    //status.setText(msg + "  ");  // don't mind the hack
    status = msg;
  }

  public void messageClear(String msg) {
    //if (status.getText().equals(msg + "  ")) status.setText(PdeEditor.EMPTY);
    if (status.equals(msg)) status = "";
  }


  public Dimension getPreferredSize() {
    return new Dimension(BUTTON_WIDTH, (BUTTON_COUNT + 1)*BUTTON_HEIGHT);
  }
}
