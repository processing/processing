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
   * The IProblem which is being wrapped
   */
  private IProblem iProblem;
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

  /**
   *
   * @param iProblem - The IProblem which is being wrapped
   * @param tabIndex - The tab number to which the error belongs to
   * @param lineNumber - Line number(pde code) of the error
   */
  public JavaProblem(IProblem iProblem, int tabIndex, int lineNumber) {
    this.iProblem = iProblem;
    if(iProblem.isError()) {
      type = ERROR;
    }
    else if(iProblem.isWarning()) {
      type = WARNING;
    }
    this.tabIndex = tabIndex;
    this.lineNumber = lineNumber;
    this.message = ErrorMessageSimplifier.getSimplifiedErrorMessage(iProblem);
  }

  public void setPDEOffsets(int startOffset, int stopOffset){
    this.startOffset = startOffset;
    this.stopOffset = stopOffset;
  }

  public int getStartOffset() {
    return startOffset;
  }

  public int getStopOffset() {
    return stopOffset;
  }

  public String toString() {
    return new String("TAB " + tabIndex + ",LN " + lineNumber + "LN START OFF: "
        + startOffset + ",LN STOP OFF: " + stopOffset + ",PROB: "
        + message);
  }

  public boolean isError() {
    return type == ERROR;
  }

  public boolean isWarning() {
    return type == WARNING;
  }

  public String getMessage() {
    return message;
  }

  public IProblem getIProblem() {
    return iProblem;
  }

  public int getTabIndex() {
    return tabIndex;
  }

  public int getLineNumber() {
    return lineNumber;
  }

  /**
   * Remember to subtract a -1 to line number because in compile check code an
   * extra package statement is added, so all line numbers are increased by 1
   *
   * @return
   */
  public int getSourceLineNumber() {
    return iProblem.getSourceLineNumber();
  }

  public void setType(int ProblemType){
    if(ProblemType == ERROR)
      type = ERROR;
    else if(ProblemType == WARNING)
      type = WARNING;
    else throw new IllegalArgumentException("Illegal Problem type passed to Problem.setType(int)");
  }

  public String[] getImportSuggestions() {
    return importSuggestions;
  }

  public void setImportSuggestions(String[] a) {
    importSuggestions = a;
  }

  // Split camel case words into separate words.
  // "VaraibleDeclaration" becomes "Variable Declaration"
  // But sadly "PApplet" become "P Applet" and so on.
  public static String splitCamelCaseWord(String word) {
    String newWord = "";
    for (int i = 1; i < word.length(); i++) {
      if (Character.isUpperCase(word.charAt(i))) {
        // System.out.println(word.substring(0, i) + " "
        // + word.substring(i));
        newWord += word.substring(0, i) + " ";
        word = word.substring(i);
        i = 1;
      }
    }
    newWord += word;
    // System.out.println(newWord);
    return newWord.trim();
  }

}
