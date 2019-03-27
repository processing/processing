package processing.mode.java.preproc;

public class PdePreprocessIssue {

  private final int line;
  private final int charPositionInLine;
  private final String msg;

  public PdePreprocessIssue(int newLine, int newCharPositionInLine, String newMsg) {
    line = newLine;
    charPositionInLine = newCharPositionInLine;
    msg = newMsg;
  }

  public int getLine() {
    return line;
  }

  public int getCharPositionInLine() {
    return charPositionInLine;
  }

  public String getMsg() {
    return msg;
  }

}
