package processing.mode.java.preproc.util;

public class IssueMessageSimplification {

  private final String message;
  private final int lineOffset;

  public IssueMessageSimplification(String newMessage) {
    message = newMessage;
    lineOffset = 0;
  }

  public IssueMessageSimplification(String newMessage, int newLineOffset) {
    message = newMessage;
    lineOffset = newLineOffset;
  }

  public String getMessage() {
    return message;
  }

  public int getLineOffset() {
    return lineOffset;
  }

}
