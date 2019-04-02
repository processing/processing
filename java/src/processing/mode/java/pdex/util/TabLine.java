package processing.mode.java.pdex.util;


/**
 * Identifier of a line within a tab.
 */
public class TabLine {

  private final int tab;
  private final int globalLine;
  private final int lineInTab;

  /**
   * Create a new tab line identifier.
   *
   * @param newTab The zero indexed tab number in which the line of code appears.
   * @param newGlobalLine The line of that code within the concatenated "global" java file version
   *    of the sketch.
   * @param newLineIntTab The line of the code within the tab.
   */
  public TabLine(int newTab, int newGlobalLine, int newLineIntTab) {
    tab = newTab;
    globalLine = newGlobalLine;
    lineInTab = newLineIntTab;
  }

  /**
   * The tab number within the sketch in which the line of code appears.
   *
   * @return The tab number on which the code appears.
   */
  public int getTab() {
    return tab;
  }

  /**
   * Get the location of the source as a line within the "global" concatenated java file.
   *
   * @return Line within the concatenated java file version of this sketch.
   */
  public int getGlobalLine() {
    return globalLine;
  }

  /**
   * Get the location of the source within the tab.
   *
   * @return The "local" line for the source or, in other words, the line number within the tab
   *    housing the code.
   */
  public int getLineInTab() {
    return lineInTab;
  }

}
