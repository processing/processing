package processing.mode.java.pdex.util;

import java.util.List;
import java.util.OptionalInt;
import java.util.stream.IntStream;


/**
 * Utility which determines the tab and local line number on which a global line number appears.
 *
 * <p>
 *   Processing concatenates tabs into single file for compilation as Java where a source line
 *   from a tab is a "local" line and the same line in the concatenated file is "global". This
 *   utility determines the local line and tab number given a global line number.
 * </p>
 */
public class TabLineFactory {

  /**
   * Get the local tab and line number for a global line.
   *
   * @param tabStarts The lines on which each tab starts.
   * @param line The global line to locate as a local line.
   * @return The local tab number and local line number.
   */
  public static TabLine getTab(List<Integer> tabStarts, int line) {
    OptionalInt tabMaybe = IntStream.range(0, tabStarts.size())
        .filter((index) -> line >= tabStarts.get(index))
        .max();

    int tab = tabMaybe.orElse(0);

    int localLine = line - tabStarts.get(tab);

    return new TabLine(tab, line, localLine);
  }

}
