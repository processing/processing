/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2015-19 The Processing Foundation

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

package processing.app.ui;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import processing.app.Mode;


abstract public class EditorButton extends JComponent
implements MouseListener, MouseMotionListener, ActionListener {
  static public final int DIM = Toolkit.zoom(30);

  /** Button's description. */
  protected String title;
  /** Description of alternate behavior when shift is down. */
  protected String titleShift;
  /** Description of alternate behavior when alt is down. */
  protected String titleAlt;

  protected boolean pressed;
  protected boolean selected;
  protected boolean rollover;
//  protected JLabel rolloverLabel;
  protected boolean shift;

  protected Image enabledImage;
  protected Image disabledImage;
  protected Image selectedImage;
  protected Image rolloverImage;
  protected Image pressedImage;

  protected Image gradient;

  protected EditorToolbar toolbar;
//  protected Mode mode;


  public EditorButton(EditorToolbar parent, String name, String title) {
    this(parent, name, title, title, title);
  }


  public EditorButton(EditorToolbar parent, String name,
                      String title, String titleShift) {
    this(parent, name, title, titleShift, title);
  }


  public EditorButton(EditorToolbar parent, String name,
                      String title, String titleShift, String titleAlt) {
    this.toolbar = parent;
    this.title = title;
    this.titleShift = titleShift;
    this.titleAlt = titleAlt;

    Mode mode = toolbar.mode;

    disabledImage = mode.loadImageX(name + "-disabled");
    enabledImage = mode.loadImageX(name + "-enabled");
    selectedImage = mode.loadImageX(name + "-selected");
    pressedImage = mode.loadImageX(name + "-pressed");
    rolloverImage = mode.loadImageX(name + "-rollover");

    if (disabledImage == null) {
      disabledImage = enabledImage;
    }
    if (selectedImage == null) {
      selectedImage = enabledImage;
    }
    if (pressedImage == null) {
      pressedImage = enabledImage;  // could be selected image
    }
    if (rolloverImage == null) {
      rolloverImage = enabledImage;  // could be pressed image
    }
    addMouseListener(this);
    addMouseMotionListener(this);
  }


  @Override
  public void paintComponent(Graphics g) {
    Image image = enabledImage;
    if (!isEnabled()) {
      image = disabledImage;
    } else if (selected) {
      image = selectedImage;
    } else if (pressed) {
      image = pressedImage;
    } else if (rollover) {
      image = rolloverImage;
    }

    Graphics2D g2 = (Graphics2D) g;
    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BICUBIC);

    int dim = getSize().width;  // width == height
    if (gradient != null) {
      //g.drawImage(gradient, 0, 0, DIM, DIM, this);
      g.drawImage(gradient, 0, 0, dim, dim, this);
    }
    //g.drawImage(image, 0, 0, DIM, DIM, this);
    g.drawImage(image, 0, 0, dim, dim, this);
  }


//    public String toString() {
//      switch (this) {
//      case DISABLED: return "disabled";
//      case ENABLED: return "enabled";
//      case SELECTED: return "selected";
//      case ROLLOVER: return "rollover";
//      case PRESSED: return "pressed";
//
////    for (State bs : State.values()) {
////      Image image = mode.loadImage(bs.getFilename(name));
////      if (image != null) {
////        imageMap.put(bs, image);
////      }
////    }
////
////    enabled = true;
////    //updateState();
////    setState(State.ENABLED);
//  }


//  public void setReverse() {
//    gradient = mode.makeGradient("reversed", DIM, DIM);
//  }


//  public void setGradient(Image gradient) {
//    this.gradient = gradient;
//  }


//  public void setRolloverLabel(JLabel label) {
//    rolloverLabel = label;
//  }


  @Override
  public void mouseClicked(MouseEvent e) {
//    if (isEnabled()) {
//      shift = e.isShiftDown();
//      actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED,
//                                      null, e.getModifiers()));
//    }
  }


  public boolean isShiftDown() {
    return shift;
  }


  @Override
  public void mousePressed(MouseEvent e) {
    setPressed(true);

    // Need to fire here (or on mouse up) because mouseClicked()
    // won't be fired if the user nudges the mouse while clicking.
    // https://github.com/processing/processing/issues/3529
    shift = e.isShiftDown();
    actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED,
                                    null, e.getModifiers()));
  }


  @Override
  public void mouseReleased(MouseEvent e) {
    setPressed(false);
  }


  public void setPressed(boolean pressed) {
    if (isEnabled()) {
      this.pressed = pressed;
      repaint();
    }
  }


  public void setSelected(boolean selected) {
    this.selected = selected;
  }


  /*
  @Override
  public void keyTyped(KeyEvent e) { }


  @Override
  public void keyReleased(KeyEvent e) {
    updateRollover(e);
  }


  @Override
  public void keyPressed(KeyEvent e) {
    System.out.println(e);
    updateRollover(e);
  }
  */


  public String getRolloverText(InputEvent e) {
    if (e.isShiftDown()) {
      return titleShift;
    } else if (e.isAltDown()) {
      return titleAlt;
    }
    return title;
  }


  /*
  public void updateRollover(InputEvent e) {
    if (rolloverLabel != null) {
      if (e.isShiftDown()) {
        rolloverLabel.setText(titleShift);
      } else if (e.isAltDown()) {
        rolloverLabel.setText(titleAlt);
      } else {
        rolloverLabel.setText(title);
      }
    }
  }
  */


  @Override
  public void mouseEntered(MouseEvent e) {
    toolbar.setRollover(this, e);
    /*
    rollover = true;
    updateRollover(e);
    repaint();
    */
  }


  @Override
  public void mouseExited(MouseEvent e) {
    toolbar.setRollover(null, e);
    /*
    rollover = false;
    if (rolloverLabel != null) {
      rolloverLabel.setText("");
    }
    repaint();
    */
  }


  @Override
  public void mouseDragged(MouseEvent e) { }


  @Override
  public void mouseMoved(MouseEvent e) { }


  abstract public void actionPerformed(ActionEvent e);

//  @Override
//  public void actionPerformed(ActionEvent e) {
//    // To be overridden by all subclasses
//  }


  @Override
  public Dimension getPreferredSize() {
    return new Dimension(DIM, DIM);
  }


  @Override
  public Dimension getMinimumSize() {
    return getPreferredSize();
  }


  @Override
  public Dimension getMaximumSize() {
    return getPreferredSize();
  }


//  public Image getImage() {
//    return imageMap.get(state);
//  }
//
//
////  protected void updateState() {
////    state = ButtonState.ENABLED;
////  }
//
//
//  public void setEnabled(boolean enabled) {
//    this.enabled = enabled;
//    if (enabled) {
//      if (state == State.DISABLED) {
//        setState(State.ENABLED);
//      }
//    } else {
//      if (state == State.ENABLED) {
//        setState(State.DISABLED);
//      }
//    }
//  }
//
//
//  public void setState(State state) {
//    this.state = state;
//  }


//  public enum State {
//    DISABLED, ENABLED, SELECTED, ROLLOVER, PRESSED;
//
//    /**
//     * @param name the root name
//     * @return
//     */
//    public String getFilename(String name) {
//      final int res = Toolkit.highResDisplay() ? 2 : 1;
//      return name + "-" + toString() + "-" + res + "x.png";
//    }
//
//    public String toString() {
//      switch (this) {
//      case DISABLED: return "disabled";
//      case ENABLED: return "enabled";
//      case SELECTED: return "selected";
//      case ROLLOVER: return "rollover";
//      case PRESSED: return "pressed";
//      }
//      return null;
//    }
//  }
}