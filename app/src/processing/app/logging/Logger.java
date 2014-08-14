/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2004-13 Ben Fry and Casey Reas
  Copyright (c) 2001-04 Massachusetts Institute of Technology

  This program is free software; you can redistribute it and/or
  modify it under the terms of the GNU General Public License
  version 2, as published by the Free Software Foundation.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software Foundation,
  Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/
package processing.app.logging;

import processing.app.Base;

public class Logger {
  static public void log(Object from, String message) {
    if (Base.DEBUG) {
      System.out.println(from.getClass().getName() + ": " + message);
    }
  }


  static public void log(String message) {
    if (Base.DEBUG) {
      System.out.println(message);
    }
  }


  static public void logf(String message, Object... args) {
    if (Base.DEBUG) {
      System.out.println(String.format(message, args));
    }
  }


  static public void log(String message, Throwable e) {
    if (Base.DEBUG) {
      System.out.println(message);
      e.printStackTrace();
    }
  }
}
