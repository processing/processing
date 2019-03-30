package processing.mode.java.pdex.util;

import processing.app.Problem;
import processing.app.ui.Editor;
import processing.mode.java.preproc.PdePreprocessIssue;

import java.util.List;


public class ProblemFactory {

  public static Problem build(PdePreprocessIssue pdePreprocessIssue, List<Integer> tabStarts,
      Editor editor) {

    int line = pdePreprocessIssue.getLine();

    TabLine tabLine = TabLineFactory.getTab(tabStarts, line);

    int tab = tabLine.getTab();
    int localLine = tabLine.getLineInTab();
    int col = pdePreprocessIssue.getCharPositionInLine();

    String message = pdePreprocessIssue.getMsg();

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

    TabLine tabLine = TabLineFactory.getTab(tabStarts, line);

    int tab = tabLine.getTab();
    int localLine = tabLine.getLineInTab();
    int col = pdePreprocessIssue.getCharPositionInLine();

    String message = pdePreprocessIssue.getMsg();

    return new SyntaxProblem(
        tab,
        localLine,
        message,
        localLine,
        localLine + col
    );
  }

}
