/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2012 The Processing Foundation

  This library is free software; you can redistribute it and/or
  modify it under the terms of the GNU Lesser General Public
  License as published by the Free Software Foundation, version 2.1.

  This library is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  Lesser General Public License for more details.

  You should have received a copy of the GNU Lesser General
  Public License along with this library; if not, write to the
  Free Software Foundation, Inc., 59 Temple Place, Suite 330,
  Boston, MA  02111-1307  USA
*/

package processing.event;

//import processing.core.PConstants;


public class MouseEvent extends Event {
  static public final int PRESS = 1;
  static public final int RELEASE = 2;
  static public final int CLICK = 3;
  static public final int DRAG = 4;
  static public final int MOVE = 5;
  static public final int ENTER = 6;
  static public final int EXIT = 7;
  static public final int WHEEL = 8;

  protected int x, y;
  protected int button;
//  protected int clickCount;
  protected float amount;


//  public MouseEvent(int x, int y) {
//    this(null,
//         System.currentTimeMillis(), PRESSED, 0,
//         x, y, PConstants.LEFT, 1);
//  }


  public MouseEvent(Object nativeObject,
                    long millis, int action, int modifiers,
                    int x, int y, int button, float amount) {  //int clickCount) {
    super(nativeObject, millis, action, modifiers);
    this.flavor = MOUSE;
    this.x = x;
    this.y = y;
    this.button = button;
    //this.clickCount = clickCount;
    this.amount = amount;
  }


  public int getX() {
    return x;
  }


  public int getY() {
    return y;
  }


  /** Which button was pressed, either LEFT, CENTER, or RIGHT. */
  public int getButton() {
    return button;
  }


//  public void setButton(int button) {
//    this.button = button;
//  }


  public int getClickCount() {
    return (int) amount; //clickCount;
  }


  /**
   * Number of clicks for mouse button events, or the number of steps (positive
   * or negative depending on direction) for a mouse wheel event.
   */
  public float getAmount() {
    return amount;
  }

  public static String eventName(final int eventCode) {
    switch (eventCode) {
    case PRESS:
      return "PRESS";
    case RELEASE:
      return "RELEASE";
    case CLICK:
      return "CLICK";
    case DRAG:
      return "DRAG";
    case MOVE:
      return "MOVE";
    case ENTER:
      return "ENTER";
    case EXIT:
      return "EXIT";
    case WHEEL:
      return "WHEEL";
    default:
      return "Unknown event code " + String.valueOf(eventCode);
    }
  }

  @Override
  public String toString() {
    return String.format("MouseEvent<%s at %d,%d b(%d) amount(%f)>",
                         eventName(getAction()), x, y, button, amount);
  }

//  public void setClickCount(int clickCount) {
//    this.clickCount = clickCount;
//  }
}
