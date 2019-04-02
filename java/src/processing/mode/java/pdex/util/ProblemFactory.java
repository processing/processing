package processing.mode.java.pdex.util;

import processing.app.Problem;
import processing.app.ui.Editor;
import processing.mode.java.preproc.issue.PdePreprocessIssue;

import java.util.List;


/**
 * Factory which helps create {Problem}s during preprocessing.
 */
public class ProblemFactory {

  /**
   * Create a new {Problem}.
   *
   * @param pdePreprocessIssue The preprocess issue found.
   * @param tabStarts The list of line numbers on which each tab starts.
   * @param editor The editor in which errors will appear.
   * @return Newly created problem.
   */
  public static Problem build(PdePreprocessIssue pdePreprocessIssue, List<Integer> tabStarts,
      Editor editor) {

    int line = pdePreprocessIssue.getLine();

    TabLine tabLine = TabLineFactory.getTab(tabStarts, line);

    int tab = tabLine.getTab();
    int localLine = tabLine.getLineInTab(); // Problems emitted in 0 index
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

  /**
   * Create a new {Problem}.
   *
   * @param pdePreprocessIssue The preprocess issue found.
   * @param tabStarts The list of line numbers on which each tab starts.
   * @return Newly created problem.
   */
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
