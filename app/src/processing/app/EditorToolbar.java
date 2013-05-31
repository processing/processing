/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2004-13 Ben Fry and Casey Reas
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
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import javax.swing.*;
import javax.swing.event.*;


/**
 * run/stop/etc buttons for the ide
 */
public abstract class EditorToolbar extends JComponent implements MouseInputListener, KeyListener {

  /** Width of each toolbar button. */
  static final int BUTTON_WIDTH = 27;
  /** Height of each toolbar button. */
  static final int BUTTON_HEIGHT = 32;
  /** The amount of space between groups of buttons on the toolbar. */
  static final int BUTTON_GAP = 5;
  /** Size (both width and height) of the buttons in the source image. */
  static final int BUTTON_IMAGE_SIZE = 33;

  static final int INACTIVE = 0;
  static final int ROLLOVER = 1;
  static final int ACTIVE   = 2;

  protected Base base;
  protected Editor editor;
  protected Mode mode;

  Image offscreen;
  int width, height;

  Color bgcolor;

  protected Button rollover;

  Font statusFont;
  Color statusColor;
  
  boolean shiftPressed;

  // what the mode indicator looks like
  Color modeButtonColor;
  Font modeTextFont;
  Color modeTextColor;
  String modeTitle;
  int modeX1, modeY1;
  int modeX2, modeY2;
  JMenu modeMenu;
  
  protected ArrayList<Button> buttons;

  static final int ARROW_WIDTH = 14;
  static final int ARROW_HEIGHT = 14;
  static Image modeArrow;

  
  public EditorToolbar(Editor editor, Base base) {  //, JMenu menu) {
    this.editor = editor;
    this.base = base;
//    this.menu = menu;

    buttons = new ArrayList<Button>();
    rollover = null;

    mode = editor.getMode();
    bgcolor = mode.getColor("buttons.bgcolor");
    statusFont = mode.getFont("buttons.status.font");
    statusColor = mode.getColor("buttons.status.color");
//    modeTitle = mode.getTitle().toUpperCase();
    modeTitle = mode.getTitle();
    modeTextFont = mode.getFont("header.text.font");
    modeButtonColor = mode.getColor("mode.button.color");

    if (modeArrow == null) {
      String suffix = Toolkit.highResDisplay() ? "-2x.png" : ".png";
      modeArrow = Toolkit.getLibImage("mode-arrow" + suffix);
    }

    addMouseListener(this);
    addMouseMotionListener(this);
  }


  /** Load images and add toolbar buttons */
  abstract public void init();


  /**
   * Load button images and slice them up. Only call this from paintComponent,  
   * or when the comp is displayable, otherwise createImage() might fail.
   * (Using BufferedImage instead of createImage() nowadays, so that may 
   * no longer be relevant.) 
   */
  public Image[][] loadImages() {
    int res = Toolkit.highResDisplay() ? 2 : 1;
    
    String suffix = null; 
    Image allButtons = null;
    // Some modes may not have a 2x version. If a mode doesn't have a 1x 
    // version, this will cause an error... they should always have 1x.
    if (res == 2) {
      suffix = "-2x.png";
      allButtons = mode.loadImage("theme/buttons" + suffix);
      if (allButtons == null) {
        res = 1;  // take him down a notch
      }
    }
    if (res == 1) {
      suffix = ".png";
      allButtons = mode.loadImage("theme/buttons" + suffix);
      if (allButtons == null) {
        // use the old (pre-2.0b9) file name
        suffix = ".gif";
        allButtons = mode.loadImage("theme/buttons" + suffix);
      }
    }

    int count = allButtons.getWidth(this) / BUTTON_WIDTH*res;
    Image[][] buttonImages = new Image[count][3];
    
    for (int i = 0; i < count; i++) {
      for (int state = 0; state < 3; state++) {
        Image image = new BufferedImage(BUTTON_WIDTH*res, BUTTON_HEIGHT*res, BufferedImage.TYPE_INT_ARGB);
        Graphics g = image.getGraphics();
        g.drawImage(allButtons, 
                    -(i*BUTTON_IMAGE_SIZE*res) - 3, 
                    (state-2)*BUTTON_IMAGE_SIZE*res, null);
        g.dispose();
        buttonImages[i][state] = image;
      }
    }
    
    return buttonImages;
  }
  
  
//  abstract static public String getTitle(int index, boolean shift);


  @Override
  public void paintComponent(Graphics screen) {
    if (buttons.size() == 0) {
      init();
    }

    Dimension size = getSize();
    if ((offscreen == null) ||
        (size.width != width) || (size.height != height)) {
      if (Toolkit.highResDisplay()) {
        offscreen = createImage(size.width*2, size.height*2);
      } else {
        offscreen = createImage(size.width, size.height);
      }

      width = size.width;
      height = size.height;

      int offsetX = 3;
      for (Button b : buttons) {
        b.left = offsetX;
        if (b.gap) {
          b.left += BUTTON_GAP;
        }
        b.right = b.left + BUTTON_WIDTH; 
        offsetX = b.right;
      }
//      for (int i = 0; i < buttons.size(); i++) {
//        x1[i] = offsetX;
//        if (i == 2) x1[i] += BUTTON_GAP;
//        x2[i] = x1[i] + BUTTON_WIDTH;
//        offsetX = x2[i];
//      }
    }
    Graphics g = offscreen.getGraphics();    
    Graphics2D g2 = (Graphics2D) g;
    
    if (Toolkit.highResDisplay()) {
      // scale everything 2x, will be scaled down when drawn to the screen
      g2.scale(2, 2);
    } else {
      // don't anti-alias text in retina mode
      g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                          RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }

    g.setColor(bgcolor); //getBackground());
    g.fillRect(0, 0, width, height);
//    if (backgroundImage != null) {
//      g.drawImage(backgroundImage, 0, 0, BACKGROUND_WIDTH, BACKGROUND_HEIGHT, null);
//    }
    mode.drawBackground(g, 0, this.getWidth()); // also pass the width in order to tile the background image

//    for (int i = 0; i < buttonCount; i++) {
//      g.drawImage(stateImage[i], x1[i], y1, null);
//    }
    for (Button b : buttons) {
      g.drawImage(b.stateImage, b.left, 0, BUTTON_WIDTH, BUTTON_HEIGHT, null);
    }

    g.setColor(statusColor);
    g.setFont(statusFont);

    // If I ever find the guy who wrote the Java2D API, I will hurt him.
//    Graphics2D g2 = (Graphics2D) g;
//    FontRenderContext frc = g2.getFontRenderContext();
//    float statusW = (float) statusFont.getStringBounds(status, frc).getWidth();
//    float statusX = (getSize().width - statusW) / 2;
//    g2.drawString(status, statusX, statusY);

//    if (currentRollover != -1) {
    if (rollover != null) {
      int statusY = (BUTTON_HEIGHT + g.getFontMetrics().getAscent()) / 2;
      //String status = shiftPressed ? titleShift[currentRollover] : title[currentRollover];
      String status = shiftPressed ? rollover.titleShift : rollover.title;
      g.drawString(status, buttons.size() * BUTTON_WIDTH + 3 * BUTTON_GAP, statusY);
    }

    g.setFont(modeTextFont);
    FontMetrics metrics = g.getFontMetrics();
    int modeTextHeight = metrics.getAscent();
    int modeTextWidth = metrics.stringWidth(modeTitle);
    final int modeGapWidth = 8;
    final int modeBoxHeight = 20;
    modeX2 = getWidth();
    modeX1 = modeX2 - (modeGapWidth + modeTextWidth + modeGapWidth + ARROW_WIDTH + modeGapWidth);
    modeY1 = 9; //(getHeight() - modeBoxHeight) / 2;
    modeY2 = modeY1 + modeBoxHeight; //modeY1 + modeH + modeGapV*2;
    g.setColor(modeButtonColor);
    //g.drawRect(modeX1, modeY1, modeX2 - modeX1, modeY2 - modeY1);
    g.drawString(modeTitle, 
                 modeX1 + modeGapWidth, 
                 modeY1 + modeTextHeight + (modeBoxHeight - modeTextHeight) / 2 - 3); // minimal offset to the bottom to center the text visually, not mathematically
    g.drawImage(modeArrow, 
                modeX2 - ARROW_WIDTH - modeGapWidth, 
                modeY1 + (modeBoxHeight - ARROW_HEIGHT) / 2, 
                ARROW_WIDTH, ARROW_HEIGHT, null);

    screen.drawImage(offscreen, 0, 0, size.width, size.height, null);

    // dim things out when not enabled (not currently in use) 
//    if (!isEnabled()) {
//      screen.setColor(new Color(0, 0, 0, 100));
//      screen.fillRect(0, 0, getWidth(), getHeight());
//    }
  }

  
  protected void checkRollover(int x, int y) {
    Button over = findSelection(x, y);
    if (over != null) {
      //        if (state[sel] != ACTIVE) {
      if (over.state != ACTIVE) {
        //          setState(sel, ROLLOVER, true);
        over.setState(ROLLOVER, true);
        //          currentRollover = sel;
        rollover = over;
      }
    }
  }


  public void mouseMoved(MouseEvent e) {
    if (!isEnabled()) return;

    // ignore mouse events before the first paintComponent() call
    if (offscreen == null) return;

    // TODO this isn't quite right, since it's gonna kill rollovers too
//    if (state[OPEN] != INACTIVE) {
//      // avoid flicker, since there should be another update event soon
//      setState(OPEN, INACTIVE, false);
//    }

    int x = e.getX();
    int y = e.getY();

    if (rollover != null) {
      //if (y > TOP && y < BOTTOM && x > rollover.left && x < rollover.right) {
      if (y > 0 && y < getHeight() && x > rollover.left && x < rollover.right) {
        // nothing has changed
        return;

      } else {
        if (rollover.state == ROLLOVER) {
          rollover.setState(INACTIVE, true);
        }
        rollover = null;
      }
    }
    checkRollover(x, y);
  }


  public void mouseDragged(MouseEvent e) { }


//  public void handleMouse(MouseEvent e) {
//    int x = e.getX();
//    int y = e.getY();
//
////    if (currentRollover != -1) {
//    if (rollover != null) {
////      if ((x > x1[currentRollover]) && (y > y1) &&
////          (x < x2[currentRollover]) && (y < y2)) {
//      if (y > y1 && y < y2 && x > rollover.left && x < rollover.right) {
//        // nothing has changed
//        return;
//
//      } else {
////        setState(currentRollover, INACTIVE, true);
//        rollover.setState(INACTIVE, true);
////        currentRollover = -1;
//        rollover = null;
//      }
//    }
////    int sel = findSelection(x, y);
//    Button over = findSelection(x, y);
//    if (over != null) {
////      if (state[sel] != ACTIVE) {
//      if (over.state != ACTIVE) {
////        setState(sel, ROLLOVER, true);
//        over.setState(ROLLOVER, true);
////        currentRollover = sel;
//        rollover = over;
//      }
//    }
//  }


//  private int findSelection(int x, int y) {
//    // if app loads slowly and cursor is near the buttons
//    // when it comes up, the app may not have time to load
//    if ((x1 == null) || (x2 == null)) return -1;
//
//    for (int i = 0; i < buttonCount; i++) {
//      if ((y > y1) && (x > x1[i]) &&
//          (y < y2) && (x < x2[i])) {
//        //System.out.println("sel is " + i);
//        return i;
//      }
//    }
//    return -1;
//  }

  
  private Button findSelection(int x, int y) {
    // if app loads slowly and cursor is near the buttons
    // when it comes up, the app may not have time to load
    if (offscreen != null && y > 0 && y < getHeight()) {
      for (Button b : buttons) {
        if (x > b.left && x < b.right) {
          return b;
        }
      }
    }
    return null;
  }


//  private void setState(int slot, int newState, boolean updateAfter) {
//    if (buttonImages != null) {
//      state[slot] = newState;
//      stateImage[slot] = buttonImages[which[slot]][newState];
//      if (updateAfter) {
//        repaint();
//      }
//    }
//  }


  public void mouseEntered(MouseEvent e) {
//    handleMouse(e);
  }


  public void mouseExited(MouseEvent e) {
//    // if the 'open' popup menu is visible, don't register this,
//    // because the popup being set visible will fire a mouseExited() event
//    if ((popup != null) && popup.isVisible()) return;
    // this might be better
    if (e.getComponent() != this) {
      return;
    }

    // TODO another weird one.. come back to this
//    if (state[OPEN] != INACTIVE) {
//      setState(OPEN, INACTIVE, true);
//    }
//    handleMouse(e);
    
    // there is no more rollover, make sure that the rollover text goes away
//    currentRollover = -1;
    if (rollover != null) {
      if (rollover.state == ROLLOVER) {
        rollover.setState(INACTIVE, true);
      }
      rollover = null;
    }
  }

//  int wasDown = -1;


  public void mousePressed(MouseEvent e) {
    // ignore mouse presses so hitting 'run' twice doesn't cause problems
    if (isEnabled()) {
      int x = e.getX();
      int y = e.getY();
      if (x > modeX1 && x < modeX2 && y > modeY1 && y < modeY2) {
        JPopupMenu popup = editor.getModeMenu().getPopupMenu();
        popup.show(this, x, y);
      }
      
      // Need to reset the rollover here. If the window isn't active, 
      // the rollover wouldn't have been updated.
      // http://code.google.com/p/processing/issues/detail?id=561
      checkRollover(x, y);
      if (rollover != null) {
        //handlePressed(rollover);
        handlePressed(e, buttons.indexOf(rollover));
      }
    }
  }
  
  
  public void mouseClicked(MouseEvent e) { }


  public void mouseReleased(MouseEvent e) { }


//  public void handlePressed(Button b) {
//    handlePressed(buttons.indexOf(b));
//  }
  
  
  abstract public void handlePressed(MouseEvent e, int index);
  
  
  /**
   * Set a particular button to be active.
   */
  public void activate(int what) {
//    setState(what, ACTIVE, true);
    buttons.get(what).setState(ACTIVE, true);
  }


  /**
   * Set a particular button to be active.
   */
  public void deactivate(int what) {
//    setState(what, INACTIVE, true);
    buttons.get(what).setState(INACTIVE, true);
  }


  public Dimension getPreferredSize() {
    return getMinimumSize();
  }


  public Dimension getMinimumSize() {
    return new Dimension((buttons.size() + 1)*BUTTON_WIDTH, BUTTON_HEIGHT);
  }


  public Dimension getMaximumSize() {
    return new Dimension(3000, BUTTON_HEIGHT);
  }


  public void keyPressed(KeyEvent e) {
    if (e.getKeyCode() == KeyEvent.VK_SHIFT) {
      shiftPressed = true;
      repaint();
    }
  }


  public void keyReleased(KeyEvent e) {
    if (e.getKeyCode() == KeyEvent.VK_SHIFT) {
      shiftPressed = false;
      repaint();
    }
  }


  public void keyTyped(KeyEvent e) { }

  
  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

  
  public void addButton(String title, String shiftTitle, Image[] images, boolean gap) {
    Button b = new Button(title, shiftTitle, images, gap);
    buttons.add(b);
  }
  

  public class Button {
    /** Button's description. */ 
    String title;
    /** Description of alternate behavior when shift is down. */ 
    String titleShift;
    /** Three state images. */
    Image[] images;
    /** Current state value, one of ACTIVE, INACTIVE, ROLLOVER. */
    int state;
    /** Current state image. */
    Image stateImage;
    /** Left and right coordinates. */
    int left, right;
    /** Whether there's a gap before this button. */
    boolean gap;
    
//    JPopupMenu popup;
//    JMenu menu;


    public Button(String title, String titleShift, Image[] images, boolean gap) {
      this.title = title;
      this.titleShift = titleShift;
      this.images = images;
      this.gap = gap;
      
      state = INACTIVE;
      stateImage = images[INACTIVE];
    }
    
    
//    public void setMenu(JMenu menu) {
//      this.menu = menu;
//    }

    
    public void setState(int newState, boolean updateAfter) {
      state = newState;
      stateImage = images[newState];
      if (updateAfter) {
        repaint();
      }
    }
  }
}
