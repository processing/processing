package processing.mode.java.preproc.util;

public class LineOffset {

  private final int line;
  private final int charPosition;

  public LineOffset(int newLine, int newCharPosition) {
    line = newLine;
    charPosition = newCharPosition;
  }

  public int getLine() {
    return line;
  }

  public int getCharPosition() {
    return charPosition;
  }

}
