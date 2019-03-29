package processing.mode.java.pdex.util;

import processing.app.Problem;
import processing.app.ui.Editor;
import processing.data.IntList;
import processing.mode.java.preproc.PdePreprocessIssue;

import java.util.List;
import java.util.OptionalInt;
import java.util.stream.IntStream;


public class ProblemFactory {

  public static Problem build(PdePreprocessIssue pdePreprocessIssue, List<Integer> tabStarts,
      Editor editor) {

    int line = pdePreprocessIssue.getLine();

    OptionalInt tabMaybe = IntStream.range(0, tabStarts.size())
        .filter((index) -> line > tabStarts.get(index))
        .max();

    int tab = tabMaybe.orElse(0);

    int col = pdePreprocessIssue.getCharPositionInLine();

    String message = pdePreprocessIssue.getMsg();

    int localLine = line - tabStarts.get(tab);
    int lineStart = editor.getLineStartOffset(localLine);

    return new SyntaxProblem(
        tab,
        localLine,
        message,
        lineStart,
        lineStart + col
    );
  }

  public static Problem build(PdePreprocessIssue pdePreprocessIssue, List<Integer> tabStarts) {
    int line = pdePreprocessIssue.getLine();

    OptionalInt tabMaybe = IntStream.range(0, tabStarts.size())
        .filter((index) -> line > tabStarts.get(index))
        .max();

    int tab = tabMaybe.orElse(0);

    int col = pdePreprocessIssue.getCharPositionInLine();

    String message = pdePreprocessIssue.getMsg();

    int localLine = line - tabStarts.get(tab);

    return new SyntaxProblem(
        tab,
        localLine,
        message,
        localLine,
        localLine + col
    );
  }

}
