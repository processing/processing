/* -*- mode: jde; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  PdeException - an exception with a line number attached
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


public class PdeException extends Exception {
  int line = -1;
  int column = -1;

  public PdeException() { }

  public PdeException(String message) {
    super(massage(message));
    //System.out.println("message for this error is " + message);
  }

  public PdeException(String message, int line) {
    super(massage(message));
    this.line = line;
  }

  // 0060 currently only used by the new preprocessor
  public PdeException(String message, int line, int column) {
    super(massage(message));
    this.line = line;
    this.column = column;
  }

  // make static so that super() can call it
  static public final String massage(String msg) {
    if (msg.indexOf("java.lang.") == 0) {
      //int dot = msg.lastIndexOf('.');
      msg = msg.substring("java.lang.".length());
    }
    return msg;
    //return (dot == -1) ? msg : msg.substring(dot+1);
  }
}
