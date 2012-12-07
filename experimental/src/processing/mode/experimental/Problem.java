/*
  Part of the XQMode project - https://github.com/Manindra29/XQMode

  Under Google Summer of Code 2012 - 
  http://www.google-melange.com/gsoc/homepage/google/gsoc2012

  Copyright (C) 2012 Manindra Moharana

  This program is free software; you can redistribute it and/or modify
  it under the terms of the GNU General Public License version 2
  as published by the Free Software Foundation.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software Foundation,
  Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package processing.mode.experimental;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.core.compiler.IProblem;

/**
 * Wrapper class for IProblem.
 * 
 * Stores the tabIndex and line number according to its tab, including the
 * original IProblem object
 * 
 * @author Manindra Moharana &lt;me@mkmoharana.com&gt;
 * 
 */
public class Problem {
  /**
   * The IProblem which is being wrapped
   */
  private IProblem iProblem;
  /**
   * The tab number to which the error belongs to
   */
  public int tabIndex; 
  /**
   * Line number(pde code) of the error
   */
  public int lineNumber;

  /**
   * Error Message. Processed form of IProblem.getMessage()
   */
  public String message;

  /**
   * The type of error - WARNING or ERROR.
   */
  public int type;

  public static final int ERROR = 1, WARNING = 2;

  /**
   * 
   * @param iProblem - The IProblem which is being wrapped
   * @param tabIndex - The tab number to which the error belongs to
   * @param lineNumber - Line number(pde code) of the error
   */
  public Problem(IProblem iProblem, int tabIndex, int lineNumber) {
    this.iProblem = iProblem;
    if(iProblem.isError()) {
      type = ERROR;
    }
    else if(iProblem.isWarning()) {
      type = WARNING;
    }
    this.tabIndex = tabIndex;
    this.lineNumber = lineNumber;
    this.message = process(iProblem);
  }

  public String toString() {
    return new String("TAB " + tabIndex + ",LN " + lineNumber + ",PROB: "
      + message);
  }

  public boolean isError(){
    return type == ERROR;
  }

  public boolean isWarning(){
    return type == WARNING;
  }

  public String getMessage(){
    return message;
  }

  public IProblem getIProblem(){
    return iProblem;
  }

  public void setType(int ProblemType){
    if(ProblemType == ERROR)
      type = ERROR;
    else if(ProblemType == WARNING)
      type = WARNING;
    else throw new IllegalArgumentException("Illegal Problem type passed to Problem.setType(int)");
  }

  private static Pattern pattern;
  private static Matcher matcher;

  private static final String tokenRegExp = "\\b token\\b";

  public static String process(IProblem problem) {
    return process(problem.getMessage());
  }

  /**
   * Processes error messages and attempts to make them a bit more english like. 
   * Currently performs:
   * <li>Remove all instances of token. "Syntax error on token 'blah', delete this token"
   * becomes "Syntax error on 'blah', delete this"
   * @param message - The message to be processed
   * @return String - The processed message
   */
  public static String process(String message) {
    // Remove all instances of token
    // "Syntax error on token 'blah', delete this token"

    pattern = Pattern.compile(tokenRegExp);
    matcher = pattern.matcher(message);
    message = matcher.replaceAll("");

    return message;
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
