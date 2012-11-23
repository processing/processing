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


public class KeyEvent extends Event {
  static public final int PRESS = 1;
  static public final int RELEASE = 2;
  static public final int TYPE = 3;

  char key;
  int keyCode;


  public KeyEvent(Object nativeObject,
                  long millis, int action, int modifiers,
                  char key, int keyCode) {
    super(nativeObject, millis, action, modifiers);
    this.flavor = KEY;
    this.key = key;
    this.keyCode = keyCode;
  }


  public char getKey() {
    return key;
  }


  public int getKeyCode() {
    return keyCode;
  }
}