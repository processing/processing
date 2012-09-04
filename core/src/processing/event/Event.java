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


public class Event {
  protected Object nativeObject;

  protected long millis;
  protected int action;

  static public final int SHIFT_MASK = 1 << 6;
  static public final int CTRL_MASK  = 1 << 7;
  static public final int META_MASK  = 1 << 8;
  static public final int ALT_MASK   = 1 << 9;
  protected int modifiers;


  public Event(Object nativeObject, long millis, int action, int modifiers) {
    this.nativeObject = nativeObject;
    this.millis = millis;
    this.action = action;
    this.modifiers = modifiers;
  }


  public Object getNative() {
    return nativeObject;
  }


//  public void setNative(Object nativeObject) {
//    this.nativeObject = nativeObject;
//  }


  public long getMillis() {
    return millis;
  }


//  public void setMillis(long millis) {
//    this.millis = millis;
//  }


  public int getAction() {
    return action;
  }


//  public void setAction(int action) {
//    this.action = action;
//  }


  public int getModifiers() {
    return modifiers;
  }


//  public void setModifiers(int modifiers) {
//    this.modifiers = modifiers;
//  }


  public boolean isShiftDown() {
    return (modifiers & SHIFT_MASK) != 0;
  }


  public boolean isControlDown() {
    return (modifiers & CTRL_MASK) != 0;
  }


  public boolean isMetaDown() {
    return (modifiers & META_MASK) != 0;
  }


  public boolean isAltDown() {
    return (modifiers & ALT_MASK) != 0;
  }
}