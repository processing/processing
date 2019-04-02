package processing.mode.java.pdex.util;

import processing.app.Problem;


/**
 * Problem identifying a syntax error found in preprocessing.
 */
public class SyntaxProblem implements Problem {

  private final int tabIndex;
  private final int lineNumber;
  private final String message;
  private final int startOffset;
  private final int stopOffset;

  /**
   * Create a new syntax problem.
   *
   * @param newTabIndex The tab number containing the source with the syntax issue.
   * @param newLineNumber The line number within the tab at which the offending code can be found.
   * @param newMessage Human readable message describing the issue.
   * @param newStartOffset The character index at which the issue starts. This is relative to start
   *    of tab / file not relative to start of line.
   * @param newStopOffset The character index at which the issue ends. This is relative to start
   *    *    of tab / file not relative to start of line.
   */
  public SyntaxProblem(int newTabIndex, int newLineNumber, String newMessage, int newStartOffset,
                       int newStopOffset) {

    tabIndex = newTabIndex;
    lineNumber = newLineNumber;
    message = newMessage;
    startOffset = newStartOffset;
    stopOffset = newStopOffset;
  }

  @Override
  public boolean isError() {
    return true;
  }

  @Override
  public boolean isWarning() {
    return false;
  }

  @Override
  public int getTabIndex() {
    return tabIndex;
  }

  @Override
  public int getLineNumber() {
    return lineNumber;
  }

  @Override
  public String getMessage() {
    return message;
  }

  @Override
  public int getStartOffset() {
    return startOffset;
  }

  @Override
  public int getStopOffset() {
    return stopOffset;
  }

}
