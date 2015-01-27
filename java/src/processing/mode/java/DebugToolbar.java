/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
Part of the Processing project - http://processing.org
Copyright (c) 2012-15 The Processing Foundation

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License version 2
as published by the Free Software Foundation.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software Foundation, Inc.
51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
*/

package processing.mode.java;

import java.awt.event.ActionEvent;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;

import processing.app.Base;
import processing.app.EditorButton;
import processing.app.Language;
import processing.app.Mode;


/** Toolbar for the top of the DebugTray */
public class DebugToolbar extends JPanel {  //Component implements MouseInputListener, KeyListener {
  static public final int GAP = 13;

  // preserve original button id's, but re-define so they are accessible 
  // (they are used by DebugEditor, so they want to be public)

//  static final int BUTTON_GAP = 13;
//  static final int BUTTON_DIM = 46;
  
//  static protected final int RUN    = 100; // change this, to be able to get it's name via getTitle()
//  static protected final int DEBUG = JavaToolbar.RUN;

//  static protected final int CONTINUE = 101;
//  static protected final int STEP = 102;
//  static protected final int BREAKPOINT = 103;
//  static protected final int TOGGLE_VAR_INSPECTOR = 104;
//  static protected final int STOP   = JavaToolbar.STOP;

//  static protected final int NEW    = JavaToolbar.NEW;
//  static protected final int OPEN   = JavaToolbar.OPEN;
//  static protected final int SAVE   = JavaToolbar.SAVE;
//  static protected final int EXPORT = JavaToolbar.EXPORT;

//  enum DebugButton {
//    CONTINUE, STEP, BREAKPOINT;
//    
//    String getTitle(boolean shift) {
//      if (this == CONTINUE) {
//        return Language.text("toolbar.debug.continue");
//      } else if (this == BREAKPOINT) {
//        return Language.text("toolbar.debug.toggle_breakpoints");
//      } else if (this == STEP) {
//        if (shift) {
//          return Language.text("toolbar.debug.step_into");
//        } else {
//          return Language.text("toolbar.debug.step");
//        }
//      }
//    }
//    
//    String getFilename() {
//      if (this == CONTINUE) {
//        return "debug-continue";
//      } else if (this == STEP) {
//        return "debug-step";
//      } else if (this == BREAKPOINT) {
//        return "debug-breakpoint";
//      }
//      throw new IllegalStateException("How did you get here?");
//    }
//  }
  
//  // the sequence of button ids. (this maps button position = index to button ids)
//  static protected final int[] buttonSequence = {
//    CONTINUE, STEP, BREAKPOINT
////    DEBUG, CONTINUE, STEP, STOP, TOGGLE_BREAKPOINT, TOGGLE_VAR_INSPECTOR 
////    NEW, OPEN, SAVE, EXPORT
//  }; 

  
//    /** Width of each toolbar button. */
//  static final int BUTTON_WIDTH = 27;
//  /** Height of each toolbar button. */
////  static final int BUTTON_HEIGHT = 32;
//  /** The amount of space between groups of buttons on the toolbar. */
//  static final int BUTTON_GAP = 5;
//  /** Size (both width and height) of the buttons in the source image. */
//  static final int BUTTON_IMAGE_SIZE = 33;

    // DISABLED, ENABLED, SELECTED, ROLLOVER, PRESSED
  // if no selected available, defaults to pressed (or vice versa?)

//  static final int INACTIVE = 0;
//  static final int ROLLOVER = 1;
//  static final int ACTIVE   = 2;
  
//  protected Base base;
//  protected Editor editor;
//  protected Mode mode;
//
//  Image offscreen;
//  int width, height;
//
//  Color bgColor;
//
//  protected Button rollover;
//
//  Font statusFont;
//  int statusAscent;
//  Color statusColor;
//  
//  boolean shiftPressed;

//  // what the mode indicator looks like
//  Color modeButtonColor;
//  Font modeTextFont;
//  int modeTextAscent;
//  Color modeTextColor;
//  String modeTitle;
//  int modeX1, modeY1;
//  int modeX2, modeY2;
//  JMenu modeMenu;
  
//  protected List<Button> buttons;

//  static final int ARROW_WIDTH = 7;
//  static final int ARROW_HEIGHT = 6;
//  static Image modeArrow;
  
  EditorButton continueButton;
  EditorButton stepButton;
  EditorButton breakpointButton;

  
  public DebugToolbar(final JavaEditor editor, Base base) {
//    this.editor = editor;
//    this.base = base;
//
//    buttons = new ArrayList<Button>();
//    rollover = null;
//
//    mode = editor.getMode();
    
    final Mode mode = editor.getMode();
    Box box = Box.createHorizontalBox();

    continueButton = 
      new EditorButton(mode, "debug-continue", 
                       Language.text("toolbar.debug.continue")) {
      @Override
      public void actionPerformed(ActionEvent e) {
        Logger.getLogger(DebugToolbar.class.getName()).log(Level.INFO, "Invoked 'Continue' toolbar button");
        editor.debugger.continueDebug();
      }
    };
    box.add(continueButton);
    box.add(Box.createHorizontalStrut(GAP));
    
    stepButton = 
      new EditorButton(mode, "debug-step",
                       Language.text("toolbar.debug.step"),
                       Language.text("toolbar.debug.step_into")) {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (isShiftDown()) {
          Logger.getLogger(DebugToolbar.class.getName()).log(Level.INFO, "Invoked 'Step Into' toolbar button");
          editor.debugger.stepInto();
        } else {
          Logger.getLogger(DebugToolbar.class.getName()).log(Level.INFO, "Invoked 'Step' toolbar button");
          editor.debugger.stepOver();
        }
      }
    };
    box.add(stepButton);
    box.add(Box.createHorizontalStrut(GAP));

    breakpointButton = 
      new EditorButton(mode, "debug-breakpoint",
                       Language.text("toolbar.debug.toggle_breakpoints")) {
      @Override
      public void actionPerformed(ActionEvent e) {
        Logger.getLogger(DebugToolbar.class.getName()).log(Level.INFO, "Invoked 'Toggle Breakpoint' toolbar button");
        editor.debugger.toggleBreakpoint();
      }
    };
    add(breakpointButton);
    box.add(Box.createHorizontalStrut(GAP));

//    int x = GAP;
//    int y = GAP;
//    final int DIM = EditorButton.DIM;
//    continueButton.setBounds(x, y, DIM, DIM);
//    x += DIM + GAP;
//    stepButton.setBounds(x, y, DIM, DIM);
//    x += DIM + GAP;
//    breakpointButton.setBounds(x, y, DIM, DIM);
//    x += DIM + GAP;
    
    JLabel label = new JLabel();
    box.add(label);
    continueButton.setRolloverLabel(label);
    stepButton.setRolloverLabel(label);
    breakpointButton.setRolloverLabel(label);
    
    add(box);

    /*
    bgColor = mode.getColor("buttons.bgcolor");
    statusFont = mode.getFont("buttons.status.font");
    statusColor = mode.getColor("buttons.status.color");
//    modeTitle = mode.getTitle().toUpperCase();
    modeTitle = mode.getTitle();
    modeTextFont = mode.getFont("mode.button.font");
    modeButtonColor = mode.getColor("mode.button.color");    
    */
  }

  
  protected void activateContinue() {
  }


  protected void deactivateContinue() {
  }


  protected void activateStep() {
  }


  protected void deactivateStep() {
  }
  

//  /**
//   * Load button images and slice them up. Only call this from paintComponent,  
//   * or when the comp is displayable, otherwise createImage() might fail.
//   * (Using BufferedImage instead of createImage() nowadays, so that may 
//   * no longer be relevant.) 
//   */
//  public Image[][] loadImages() {
//    int res = Toolkit.highResDisplay() ? 2 : 1;
//    
//    String suffix = null; 
//    Image allButtons = null;
//    // Some modes may not have a 2x version. If a mode doesn't have a 1x 
//    // version, this will cause an error... they should always have 1x.
//    if (res == 2) {
//      suffix = "-2x.png";
//      allButtons = mode.loadImage("theme/buttons" + suffix);
//      if (allButtons == null) {
//        res = 1;  // take him down a notch
//      }
//    }
//    if (res == 1) {
//      suffix = ".png";
//      allButtons = mode.loadImage("theme/buttons" + suffix);
//      if (allButtons == null) {
//        // use the old (pre-2.0b9) file name
//        suffix = ".gif";
//        allButtons = mode.loadImage("theme/buttons" + suffix);
//      }
//    }
//
//    int count = allButtons.getWidth(this) / BUTTON_DIM*res;
//    Image[][] buttonImages = new Image[count][3];
//    
//    for (int i = 0; i < count; i++) {
//      for (int state = 0; state < 3; state++) {
//        Image image = new BufferedImage(BUTTON_DIM*res, Preferences.GRID_SIZE*res, BufferedImage.TYPE_INT_ARGB);
//        Graphics g = image.getGraphics();
//        g.drawImage(allButtons, 
//                    -(i*BUTTON_IMAGE_SIZE*res) - 3, 
//                    (state-2)*BUTTON_IMAGE_SIZE*res, null);
//        g.dispose();
//        buttonImages[i][state] = image;
//      }
//    }
//    
//    return buttonImages;
//  }
  
  
//  abstract static public String getTitle(int index, boolean shift);


//  @Override
//  public void paintComponent(Graphics screen) {
//    if (buttons.size() == 0) {
//      init();
//    }
//
//    Dimension size = getSize();
//    if ((offscreen == null) ||
//        (size.width != width) || (size.height != height)) {
//      if (Toolkit.highResDisplay()) {
//        offscreen = createImage(size.width*2, size.height*2);
//      } else {
//        offscreen = createImage(size.width, size.height);
//      }
//
//      width = size.width;
//      height = size.height;
//
//      int offsetX = 3;
//      for (Button b : buttons) {
//        b.left = offsetX;
//        if (b.gap) {
//          b.left += BUTTON_GAP;
//        }
//        b.right = b.left + BUTTON_WIDTH; 
//        offsetX = b.right;
//      }
////      for (int i = 0; i < buttons.size(); i++) {
////        x1[i] = offsetX;
////        if (i == 2) x1[i] += BUTTON_GAP;
////        x2[i] = x1[i] + BUTTON_WIDTH;
////        offsetX = x2[i];
////      }
//    }
//    Graphics g = offscreen.getGraphics();
//    /*Graphics2D g2 =*/ Toolkit.prepareGraphics(g);
//
//    g.setColor(hiding ? hideColor : bgColor);
//    g.fillRect(0, 0, width, height);
////    if (backgroundImage != null) {
////      g.drawImage(backgroundImage, 0, 0, BACKGROUND_WIDTH, BACKGROUND_HEIGHT, null);
////    }
//    if (!hiding) {
//      mode.drawBackground(g, 0);
//    }
//
////    for (int i = 0; i < buttonCount; i++) {
////      g.drawImage(stateImage[i], x1[i], y1, null);
////    }
//    for (Button b : buttons) {
//      g.drawImage(b.stateImage, b.left, 0, BUTTON_WIDTH, Preferences.GRID_SIZE, null);
//    }
//
//    g.setColor(statusColor);
//    g.setFont(statusFont);
//    if (statusAscent == 0) {
//      statusAscent = (int) Toolkit.getAscent(g);
//    }
//
//    // If I ever find the guy who wrote the Java2D API, I will hurt him.
////    Graphics2D g2 = (Graphics2D) g;
////    FontRenderContext frc = g2.getFontRenderContext();
////    float statusW = (float) statusFont.getStringBounds(status, frc).getWidth();
////    float statusX = (getSize().width - statusW) / 2;
////    g2.drawString(status, statusX, statusY);
//
////    if (currentRollover != -1) {
//    if (rollover != null) {
//      //int statusY = (BUTTON_HEIGHT + g.getFontMetrics().getAscent()) / 2;
//      int statusY = (Preferences.GRID_SIZE + statusAscent) / 2;
//      //String status = shiftPressed ? titleShift[currentRollover] : title[currentRollover];
//      String status = shiftPressed ? rollover.titleShift : rollover.title;
//      g.drawString(status, buttons.size() * BUTTON_WIDTH + 3 * BUTTON_GAP, statusY);
//    }
//
//    g.setFont(modeTextFont);
//    FontMetrics metrics = g.getFontMetrics();
//    if (modeTextAscent == 0) {
//      modeTextAscent = (int) Toolkit.getAscent(g); //metrics.getAscent();
//    }
//    int modeTextWidth = metrics.stringWidth(modeTitle);
//    final int modeGapWidth = 8;
//    final int modeBoxHeight = 20;
//    modeX2 = getWidth() - 16;
//    modeX1 = modeX2 - (modeGapWidth + modeTextWidth + modeGapWidth + ARROW_WIDTH + modeGapWidth);
////    modeY1 = 8; //(getHeight() - modeBoxHeight) / 2;
//    modeY1 = (getHeight() - modeBoxHeight) / 2;
//    modeY2 = modeY1 + modeBoxHeight; //modeY1 + modeH + modeGapV*2;
//    g.setColor(modeButtonColor);
//    g.drawRect(modeX1, modeY1, modeX2 - modeX1, modeY2 - modeY1 - 1);
//    
//    g.drawString(modeTitle, 
//                 modeX1 + modeGapWidth, 
//                 modeY1 + (modeBoxHeight + modeTextAscent) / 2);
//                 //modeY1 + modeTextAscent + (modeBoxHeight - modeTextAscent) / 2);
//    g.drawImage(modeArrow, 
//                modeX2 - ARROW_WIDTH - modeGapWidth, 
//                modeY1 + (modeBoxHeight - ARROW_HEIGHT) / 2, 
//                ARROW_WIDTH, ARROW_HEIGHT, null);
//
////    g.drawLine(modeX1, modeY2, modeX2, modeY2);
////    g.drawLine(0, size.height, size.width, size.height);
////    g.fillRect(modeX1 - modeGapWidth*2,  modeY1, modeGapWidth, modeBoxHeight);
//    
//    screen.drawImage(offscreen, 0, 0, size.width, size.height, null);
//
//    // dim things out when not enabled (not currently in use) 
////    if (!isEnabled()) {
////      screen.setColor(new Color(0, 0, 0, 100));
////      screen.fillRect(0, 0, getWidth(), getHeight());
////    }
//  }

  
//  protected void checkRollover(int x, int y) {
//    Button over = findSelection(x, y);
//    if (over != null) {
//      //        if (state[sel] != ACTIVE) {
//      if (over.state != ACTIVE) {
//        //          setState(sel, ROLLOVER, true);
//        over.setState(ROLLOVER, true);
//        //          currentRollover = sel;
//        rollover = over;
//      }
//    }
//  }


//  public void mouseMoved(MouseEvent e) {
//    if (!isEnabled()) return;
//
//    // ignore mouse events before the first paintComponent() call
//    if (offscreen == null) return;
//
//    // TODO this isn't quite right, since it's gonna kill rollovers too
////    if (state[OPEN] != INACTIVE) {
////      // avoid flicker, since there should be another update event soon
////      setState(OPEN, INACTIVE, false);
////    }
//
//    int x = e.getX();
//    int y = e.getY();
//
//    if (rollover != null) {
//      //if (y > TOP && y < BOTTOM && x > rollover.left && x < rollover.right) {
//      if (y > 0 && y < getHeight() && x > rollover.left && x < rollover.right) {
//        // nothing has changed
//        return;
//
//      } else {
//        if (rollover.state == ROLLOVER) {
//          rollover.setState(INACTIVE, true);
//        }
//        rollover = null;
//      }
//    }
//    checkRollover(x, y);
//  }


//  public void mouseDragged(MouseEvent e) { }


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

  
//  private Button findSelection(int x, int y) {
//    // if app loads slowly and cursor is near the buttons
//    // when it comes up, the app may not have time to load
//    if (offscreen != null && y > 0 && y < getHeight()) {
//      for (Button b : buttons) {
//        if (x > b.left && x < b.right) {
//          return b;
//        }
//      }
//    }
//    return null;
//  }


//  private void setState(int slot, int newState, boolean updateAfter) {
//    if (buttonImages != null) {
//      state[slot] = newState;
//      stateImage[slot] = buttonImages[which[slot]][newState];
//      if (updateAfter) {
//        repaint();
//      }
//    }
//  }


//  public void mouseEntered(MouseEvent e) {
////    handleMouse(e);
//  }
//
//
//  public void mouseExited(MouseEvent e) {
////    // if the 'open' popup menu is visible, don't register this,
////    // because the popup being set visible will fire a mouseExited() event
////    if ((popup != null) && popup.isVisible()) return;
//    // this might be better
//    if (e.getComponent() != this) {
//      return;
//    }
//
//    // TODO another weird one.. come back to this
////    if (state[OPEN] != INACTIVE) {
////      setState(OPEN, INACTIVE, true);
////    }
////    handleMouse(e);
//    
//    // there is no more rollover, make sure that the rollover text goes away
////    currentRollover = -1;
//    if (rollover != null) {
//      if (rollover.state == ROLLOVER) {
//        rollover.setState(INACTIVE, true);
//      }
//      rollover = null;
//    }
//  }
//
////  int wasDown = -1;
//
//
//  public void mousePressed(MouseEvent e) {
//    // ignore mouse presses so hitting 'run' twice doesn't cause problems
//    if (isEnabled()) {
//      int x = e.getX();
//      int y = e.getY();
//      if (x > modeX1 && x < modeX2 && y > modeY1 && y < modeY2) {
//        JPopupMenu popup = editor.getModeMenu().getPopupMenu();
//        popup.show(this, x, y);
//      }
//      
//      // Need to reset the rollover here. If the window isn't active, 
//      // the rollover wouldn't have been updated.
//      // http://code.google.com/p/processing/issues/detail?id=561
//      checkRollover(x, y);
//      if (rollover != null) {
//        //handlePressed(rollover);
//        handlePressed(e, buttons.indexOf(rollover));
//      }
//    }
//  }
  
  
//  public void mouseClicked(MouseEvent e) { }
//
//
//  public void mouseReleased(MouseEvent e) { }
//
//
////  public void handlePressed(Button b) {
////    handlePressed(buttons.indexOf(b));
////  }
//    
//  
//  /**
//   * Set a particular button to be active.
//   */
//  public void activate(int what) {
////    setState(what, ACTIVE, true);
//    buttons.get(what).setState(ACTIVE, true);
//  }
//
//
//  /**
//   * Set a particular button to be active.
//   */
//  public void deactivate(int what) {
////    setState(what, INACTIVE, true);
//    buttons.get(what).setState(INACTIVE, true);
//  }


//  public Dimension getPreferredSize() {
//    return getMinimumSize();
//  }
//
//
//  public Dimension getMinimumSize() {
//    //int wide = 
//    return new Dimension((buttons.size() + 1)*BUTTON_WIDTH, GAP + DIM + GAP);
//  }
//
//
//  public Dimension getMaximumSize() {
//    return new Dimension(3000, Preferences.GRID_SIZE);
//  }
//
//
//  public void keyPressed(KeyEvent e) {
//    if (e.getKeyCode() == KeyEvent.VK_SHIFT) {
//      shiftPressed = true;
//      repaint();
//    }
//  }
//
//
//  public void keyReleased(KeyEvent e) {
//    if (e.getKeyCode() == KeyEvent.VK_SHIFT) {
//      shiftPressed = false;
//      repaint();
//    }
//  }
//
//
//  public void keyTyped(KeyEvent e) { }
//
//  
//  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .
//
//  
//  public void addButton(String title, String shiftTitle, Image[] images, boolean gap) {
//    Button b = new Button(title, shiftTitle, images, gap);
//    buttons.add(b);
//  }
//  
//
//  public class Button {
//    /** Button's description. */ 
//    String title;
//    /** Description of alternate behavior when shift is down. */ 
//    String titleShift;
//    /** Three state images. */
////    Image[] images;
//    Map<ButtonState, Image> images;
//    /** Current state value, one of ACTIVE, INACTIVE, ROLLOVER. */
//    int state;
//    /** Current state image. */
//    Image stateImage;
//    /** Left and right coordinates. */
//    int left, right;
//    /** Whether there's a gap before this button. */
//    boolean gap;
//    
////    JPopupMenu popup;
////    JMenu menu;
//
//
//    public Button(String title, String titleShift, Image[] images, boolean gap) {
//      this.title = title;
//      this.titleShift = titleShift;
//      this.images = images;
//      this.gap = gap;
//      
//      state = INACTIVE;
//      stateImage = images[INACTIVE];
//    }
//    
//    
////    public void setMenu(JMenu menu) {
////      this.menu = menu;
////    }
//
//    
//    public void setState(int newState, boolean updateAfter) {
//      state = newState;
//      stateImage = images[newState];
//      if (updateAfter) {
//        repaint();
//      }
//    }
//  }
//  
//  
//  public Image[][] loadDebugImages() {
//    int res = Toolkit.highResDisplay() ? 2 : 1;
//    
//    String suffix = null; 
//    Image allButtons = null;
//    // Some modes may not have a 2x version. If a mode doesn't have a 1x 
//    // version, this will cause an error... they should always have 1x.
//    if (res == 2) {
//      suffix = "-2x.png";
//      allButtons = mode.loadImage("theme/buttons-debug" + suffix);
//      if (allButtons == null) {
//        res = 1;  // take him down a notch
//      }
//    }
//    if (res == 1) {
//      suffix = ".png";
//      allButtons = mode.loadImage("theme/buttons-debug" + suffix);
//      if (allButtons == null) {
//        // use the old (pre-2.0b9) file name
//        suffix = ".gif";
//        allButtons = mode.loadImage("theme/buttons-debug" + suffix);
//      }
//    }
//    
//    // The following three final fields were not accessible, so just copied the values here
//    // for the time being. TODO: inform Ben, make these fields public
//    /** Width of each toolbar button. */
//    final int BUTTON_WIDTH = 27;
//    /** Size (both width and height) of the buttons in the source image. */
//    final int BUTTON_IMAGE_SIZE = 33;
//    int count = allButtons.getWidth(this) / BUTTON_WIDTH*res;
//    final int GRID_SIZE = 32;
//    
//    Image[][] buttonImages = new Image[count][3];
//    
//    for (int i = 0; i < count; i++) {
//      for (int state = 0; state < 3; state++) {
//        Image image = new BufferedImage(BUTTON_WIDTH*res, GRID_SIZE*res, BufferedImage.TYPE_INT_ARGB);
//        Graphics g = image.getGraphics();
//        g.drawImage(allButtons, 
//                    -(i*BUTTON_IMAGE_SIZE*res) - 3, 
//                    (state-2)*BUTTON_IMAGE_SIZE*res, null);
//        g.dispose();
//        buttonImages[i][state] = image;
//      }
//    }
//    
//    return buttonImages;
//  }
//  
//  
//  /**
//   * Initialize buttons. Loads images and adds the buttons to the toolbar.
//   */
//  @Override
//  public void init() {
//    Image[][] images = loadDebugImages();
//    for (int idx = 0; idx < buttonSequence.length; idx++) {
//      int id = buttonId(idx);
//      //addButton(getTitle(id, false), getTitle(id, true), images[idx], id == NEW || id == TOGGLE_BREAKPOINT);
//      addButton(getTitle(id, false), getTitle(id, true), images[idx], id == TOGGLE_BREAKPOINT);
//    }
//  }
//
//  
//  /**
//   * Get the title for a toolbar button. Displayed in the toolbar when
//   * hovering over a button.
//   * @param id id of the toolbar button
//   * @param shift true if shift is pressed
//   * @return the title
//   */
//  public static String getTitle(int id, boolean shift) {
//    switch (id) {
//    case DebugToolbar.RUN:
//      return JavaToolbar.getTitle(JavaToolbar.RUN, shift);
//    case STOP:
//      return JavaToolbar.getTitle(JavaToolbar.STOP, shift);
////    case NEW:
////      return JavaToolbar.getTitle(JavaToolbar.NEW, shift);
////    case OPEN:
////      return JavaToolbar.getTitle(JavaToolbar.OPEN, shift);
////    case SAVE:
////      return JavaToolbar.getTitle(JavaToolbar.SAVE, shift);
////    case EXPORT:
////      return JavaToolbar.getTitle(JavaToolbar.EXPORT, shift);
//    case DEBUG:
//      if (shift) {
//        return Language.text("toolbar.run");
//      } else {
//        return Language.text("toolbar.debug.debug");
//      }
//    case TOGGLE_VAR_INSPECTOR:
//      return Language.text("toolbar.debug.variable_inspector");
//    }
//    return null;
//  }
//
//  
//  /**
//   * Event handler called when a toolbar button is clicked.
//   * @param e the mouse event
//   * @param idx index (i.e. position) of the toolbar button clicked
//   */
//  @Override
//  public void handlePressed(MouseEvent e, int idx) {
//    boolean shift = e.isShiftDown();
//    JavaEditor deditor = (JavaEditor) editor;
//    int id = buttonId(idx); // convert index/position to button id
//
//    switch (id) {
////            case DebugToolbar.RUN:
////                super.handlePressed(e, JavaToolbar.RUN);
////                break;
//    case STOP:
//      Logger.getLogger(DebugToolbar.class.getName()).log(Level.INFO, "Invoked 'Stop' toolbar button");
//      super.handlePressed(e, JavaToolbar.STOP);
//      break;
////    case NEW:
////      Logger.getLogger(DebugToolbar.class.getName()).log(Level.INFO, "Invoked 'New' toolbar button");
////      super.handlePressed(e, JavaToolbar.NEW);
////      break;
////    case OPEN:
////      Logger.getLogger(DebugToolbar.class.getName()).log(Level.INFO, "Invoked 'Open' toolbar button");
////      super.handlePressed(e, JavaToolbar.OPEN);
////      break;
////    case SAVE:
////      Logger.getLogger(DebugToolbar.class.getName()).log(Level.INFO, "Invoked 'Save' toolbar button");
////      super.handlePressed(e, JavaToolbar.SAVE);
////      break;
////    case EXPORT:
////      Logger.getLogger(DebugToolbar.class.getName()).log(Level.INFO, "Invoked 'Export' toolbar button");
////      super.handlePressed(e, JavaToolbar.EXPORT);
////      break;
//    case DEBUG:
//      deditor.handleStop(); // Close any running sketches
//      deditor.showProblemListView(XQConsoleToggle.CONSOLE);
//      if (shift) {
//        Logger.getLogger(DebugToolbar.class.getName()).log(Level.INFO, "Invoked 'Run' toolbar button");
//        deditor.handleRun();
//      } else {
//        Logger.getLogger(DebugToolbar.class.getName()).log(Level.INFO, "Invoked 'Debug' toolbar button");
//        deditor.debugger.startDebug();
//      }
//      break;
//    case CONTINUE:
//      Logger.getLogger(DebugToolbar.class.getName()).log(Level.INFO, "Invoked 'Continue' toolbar button");
//      deditor.debugger.continueDebug();
//      break;
//    case TOGGLE_BREAKPOINT:
//      Logger.getLogger(DebugToolbar.class.getName()).log(Level.INFO, "Invoked 'Toggle Breakpoint' toolbar button");
//      deditor.debugger.toggleBreakpoint();
//      break;
//    case STEP:
//      if (shift) {
//        Logger.getLogger(DebugToolbar.class.getName()).log(Level.INFO, "Invoked 'Step Into' toolbar button");
//        deditor.debugger.stepInto();
//      } else {
//        Logger.getLogger(DebugToolbar.class.getName()).log(Level.INFO, "Invoked 'Step' toolbar button");
//        deditor.debugger.stepOver();
//      }
//      break;
////            case STEP_INTO:
////                deditor.dbg.stepInto();
////                break;
////            case STEP_OUT:
////                deditor.dbg.stepOut();
////                break;
//    case TOGGLE_VAR_INSPECTOR:
//      Logger.getLogger(DebugToolbar.class.getName()).log(Level.INFO, "Invoked 'Variable Inspector' toolbar button");
//      deditor.toggleVariableInspector();
//      break;
//    }
//  }
//
//  
//  /**
//   * Activate (light up) a button.
//   * @param id the button id
//   */
//  @Override
//  public void activate(int id) {
//    //System.out.println("activate button idx: " + buttonIndex(id));
//    super.activate(buttonIndex(id));
//  }
//
//  
//  /**
//   * Set a button to be inactive.
//   * @param id the button id
//   */
//  @Override
//  public void deactivate(int id) {
//    //System.out.println("deactivate button idx: " + buttonIndex(id));
//    super.deactivate(buttonIndex(id));
//  }
//
//  
//  /**
//   * Get button position (index) from it's id.
//   * @param buttonId the button id
//   * ({@link #RUN}, {@link #DEBUG}, {@link #CONTINUE}), {@link #STEP}, ...)
//   * @return the button index
//   */
//  protected int buttonIndex(int buttonId) {
//    for (int i = 0; i < buttonSequence.length; i++) {
//      if (buttonSequence[i] == buttonId) {
//        return i;
//      }
//    }
//    return -1;
//  }
//
//  
//  /**
//   * Get the button id from its position (index).
//   * @param buttonIdx the button index
//   * @return the button id
//   * ({@link #RUN}, {@link #DEBUG}, {@link #CONTINUE}), {@link #STEP}, ...)
//   */
//  protected int buttonId(int buttonIdx) {
//    return buttonSequence[buttonIdx];
//  }
}
