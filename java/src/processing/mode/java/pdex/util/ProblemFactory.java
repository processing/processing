package processing.mode.java.pdex.util;

import processing.app.Problem;
import processing.app.ui.Editor;
import processing.data.IntList;
import processing.mode.java.preproc.PdePreprocessIssue;

import java.util.OptionalInt;
import java.util.stream.IntStream;


public class ProblemFactory {

  public static Problem build(PdePreprocessIssue pdePreprocessIssue, IntList tabStarts, Editor editor) {
    int line = pdePreprocessIssue.getLine();

    OptionalInt tabMaybe = IntStream.range(0, tabStarts.size())
        .filter((index) -> line > tabStarts.get(index))
        .min();

    int tab = tabMaybe.orElse(0);

    int col = pdePreprocessIssue.getCharPositionInLine();

    String message = pdePreprocessIssue.getMsg();

    int localLine = line - tabStarts.get(tab) - 1;
    int lineStart = editor.getLineStartOffset(localLine);

    return new SyntaxProblem(
        tab,
        localLine,
        message,
        lineStart,
        lineStart + col
    );
  }

}
