package processing.mode.java.pdex.util;

public class TabLine {

  private final int tab;
  private final int globalLine;
  private final int lineInTab;

  public TabLine(int newTab, int newGlobalLine, int newLineIntTab) {
    tab = newTab;
    globalLine = newGlobalLine;
    lineInTab = newLineIntTab;
  }

  public int getTab() {
    return tab;
  }

  public int getGlobalLine() {
    return globalLine;
  }

  public int getLineInTab() {
    return lineInTab;
  }

}
