/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org
  Copyright (c) 2015 The Processing Foundation

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

package processing.app;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;


abstract public class EditorButton extends JComponent 
implements MouseListener, MouseMotionListener, ActionListener {
  static public final int DIM = 35;

  /** Button's description. */ 
  protected String title;
  /** Description of alternate behavior when shift is down. */ 
  protected String titleShift;

  protected boolean pressed;
  protected boolean selected;
  protected boolean rollover;
  protected JLabel rolloverLabel;
  protected boolean shift;
  
  protected Image enabledImage;
  protected Image disabledImage;
  protected Image selectedImage;
  protected Image rolloverImage;
  protected Image pressedImage;
  
  protected Image gradient;
  
  protected Mode mode;
  
  
  public EditorButton(Mode mode, String name, String title) {
    this(mode, name, title, title);
  }
  

  public EditorButton(Mode mode, String name, String title, String titleShift) {
    this.mode = mode;
    this.title = title;
    this.titleShift = titleShift;
   
    final int res = Toolkit.highResDisplay() ? 2 : 1;
    disabledImage = mode.loadImage(name + "-disabled-" + res + "x.png");
    enabledImage = mode.loadImage(name + "-enabled-" + res + "x.png");
    selectedImage = mode.loadImage(name + "-selected-" + res + "x.png");
    pressedImage = mode.loadImage(name + "-pressed-" + res + "x.png");
    rolloverImage = mode.loadImage(name + "-rollover-" + res + "x.png");
    
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
    if (gradient != null) {
      g.drawImage(gradient, 0, 0, DIM, DIM, this);
    }
    g.drawImage(image, 0, 0, DIM, DIM, this);
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
  
  
  public void setReverse() {
    gradient = mode.getGradient("reversed", DIM, DIM);
  }
  
  
//  public void setGradient(Image gradient) {
//    this.gradient = gradient;
//  }
  
  
  public void setRolloverLabel(JLabel label) {
    rolloverLabel = label;
  }


  @Override
  public void mouseClicked(MouseEvent e) {
    if (isEnabled()) {
      shift = e.isShiftDown();
      actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, 
                                      null, e.getModifiers()));
    }
  }
  
  
  public boolean isShiftDown() {
    return shift;
  }


  @Override
  public void mousePressed(MouseEvent e) {
    setPressed(true);
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


  @Override
  public void mouseEntered(MouseEvent e) {
    rollover = true;
    if (rolloverLabel != null) {
      rolloverLabel.setText(e.isShiftDown() ? titleShift : title);
    }
    repaint();
  }


  @Override
  public void mouseExited(MouseEvent e) {
    rollover = false;
    if (rolloverLabel != null) {
      rolloverLabel.setText("");
    }
    repaint();
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