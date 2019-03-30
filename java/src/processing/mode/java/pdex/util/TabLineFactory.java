package processing.mode.java.pdex.util;

import java.util.List;
import java.util.OptionalInt;
import java.util.stream.IntStream;


public class TabLineFactory {

  public static TabLine getTab(List<Integer> tabStarts, int line) {
    OptionalInt tabMaybe = IntStream.range(0, tabStarts.size())
        .filter((index) -> line > tabStarts.get(index))
        .max();

    int tab = tabMaybe.orElse(0);

    int localLine = line - tabStarts.get(tab);

    return new TabLine(tab, line, localLine);
  }

}
