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

import org.eclipse.jdt.core.compiler.IProblem;

import processing.app.Problem;


/**
 * Wrapper class for IProblem that stores the tabIndex and line number
 * according to its tab, including the original IProblem object
 */
public class JavaProblem implements Problem {
  /**
   * The tab number to which the error belongs to
   */
  private int tabIndex;
  /**
   * Line number(pde code) of the error
   */
  private int lineNumber;

  private int startOffset;

  private int stopOffset;

  /**
   * Error Message. Processed form of IProblem.getMessage()
   */
  private String message;

  /**
   * The type of error - WARNING or ERROR.
   */
  private int type;

  /**
   * If the error is a 'cannot find type' contains the list of suggested imports
   */
  private String[] importSuggestions;

  public static final int ERROR = 1, WARNING = 2;

  public JavaProblem(String message, int type, int tabIndex, int lineNumber) {
    this.message = message;
    this.type = type;
    this.tabIndex = tabIndex;
    this.lineNumber = lineNumber;
  }

  /**
   *
   * @param iProblem - The IProblem which is being wrapped
   * @param tabIndex - The tab number to which the error belongs to
   * @param lineNumber - Line number(pde code) of the error
   * @param badCode - The code iProblem refers to.
   */
  public static JavaProblem fromIProblem(IProblem iProblem,
      int tabIndex, int lineNumber, String badCode) {
    int type = 0;
    if(iProblem.isError()) {
      type = ERROR;
    } else if (iProblem.isWarning()) {
      type = WARNING;
    }
    String message = CompileErrorMessageSimplifier.getSimplifiedErrorMessage(iProblem, badCode);
    return new JavaProblem(message, type, tabIndex, lineNumber);
  }

  public void setPDEOffsets(int startOffset, int stopOffset){
    this.startOffset = startOffset;
    this.stopOffset = stopOffset;
  }

  @Override
  public int getStartOffset() {
    return startOffset;
  }

  @Override
  public int getStopOffset() {
    return stopOffset;
  }

  @Override
  public boolean isError() {
    return type == ERROR;
  }

  @Override
  public boolean isWarning() {
    return type == WARNING;
  }

  @Override
  public String getMessage() {
    return message;
  }

  @Override
  public int getTabIndex() {
    return tabIndex;
  }

  @Override
  public int getLineNumber() {
    return lineNumber;
  }

  public String[] getImportSuggestions() {
    return importSuggestions;
  }

  public void setImportSuggestions(String[] a) {
    importSuggestions = a;
  }

  @Override
  public String toString() {
    return "TAB " + tabIndex + ",LN " + lineNumber + "LN START OFF: "
        + startOffset + ",LN STOP OFF: " + stopOffset + ",PROB: "
        + message;
  }

}
