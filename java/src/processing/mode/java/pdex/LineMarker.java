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

package processing.mode.java.pdex;


/**
 * Line markers displayed on the Error Column.
 */
public class LineMarker {
  /**
   * y co-ordinate of the marker
   */
  private int y;
  /**
   * Type of marker: Error or Warning?
   */
  private int type = -1;
  /**
   * Error Type constant
   */
  public static final int ERROR = 1;
  /**
   * Warning Type constant
   */
  public static final int WARNING = 2;
  /**
   * Problem that the error marker represents
   * @see Problem
   */
  private Problem problem;


  public LineMarker(Problem problem, int y, int type) {
    this.problem = problem;
    this.y = y;
    this.type = type;
  }


  /**
   * y co-ordinate of the marker
   */
  public int getY() {
    return y;
  }


  /**
   * Type of marker: ErrorMarker.Error or ErrorMarker.Warning?
   */
  public int getType() {
    return type;
  }


  /**
   * Problem that the error marker represents
   * @see Problem
   */
  public Problem getProblem() {
    return problem;
  }
}