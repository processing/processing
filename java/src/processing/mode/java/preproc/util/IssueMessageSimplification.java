package processing.mode.java.preproc.util;

public class IssueMessageSimplification {

  private final String message;
  private final boolean attributeToPriorToken;

  public IssueMessageSimplification(String newMessage) {
    message = newMessage;
    attributeToPriorToken = false;
  }

  public IssueMessageSimplification(String newMessage, boolean newAttributeToPriorToken) {
    message = newMessage;
    attributeToPriorToken = newAttributeToPriorToken;
  }

  public String getMessage() {
    return message;
  }

  public boolean getAttributeToPriorToken() {
    return attributeToPriorToken;
  }

}
