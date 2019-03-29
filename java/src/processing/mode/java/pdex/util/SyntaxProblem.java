package processing.mode.java.pdex.util;

import processing.app.Problem;

public class SyntaxProblem implements Problem {

  private final int tabIndex;
  private final int lineNumber;
  private final String message;
  private final int startOffset;
  private final int stopOffset;

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
