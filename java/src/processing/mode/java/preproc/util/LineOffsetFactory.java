package processing.mode.java.preproc.util;

import processing.mode.java.preproc.util.strategy.MessageSimplifierUtil;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Optionally used utility that can help clean up on which line an error is reported.
 */
public class LineOffsetFactory {

  public static LineOffset getLineWithOffset(IssueMessageSimplification simplification,
        int originalLine, int originalOffset, String source) {

    int finalLine = SyntaxUtil.getCount(source, "\n");

    boolean shouldAttributeToPrior = simplification.getAttributeToPriorToken();
    shouldAttributeToPrior = shouldAttributeToPrior && originalLine != finalLine;

    if (!shouldAttributeToPrior) {
      return new LineOffset(originalLine, originalOffset);
    }

    String priorCode = getContentsUpToLine(source, originalLine);

    PriorTokenFinder finder = new PriorTokenFinder();
    int charPos = priorCode.length();
    while (!finder.isDone() && charPos > 0) {
      charPos--;
      finder.step(priorCode.charAt(charPos));
    }

    Optional<Integer> foundStartOfMatchMaybe = finder.getTokenPositionMaybe();
    int startOfMatch;
    int linesOffset;

    if (foundStartOfMatchMaybe.isPresent()) {
      startOfMatch = priorCode.length() - foundStartOfMatchMaybe.get();
      String contentsOfMatch = priorCode.substring(startOfMatch);
      System.err.println(contentsOfMatch);
      linesOffset = SyntaxUtil.getCount(contentsOfMatch, "\n");
    } else {
      startOfMatch = priorCode.length();
      linesOffset = 0;
    }

    String contentsPriorToMatch = priorCode.substring(0, startOfMatch);
    int newLine = originalLine - linesOffset;
    int col = contentsPriorToMatch.length() - contentsPriorToMatch.lastIndexOf('\n') - 1;

    return new LineOffset(newLine, col);
  }

  private static String getContentsUpToLine(String source, int originalLine) {
    int line = 0;
    int stringCursor = 0;
    int strLength = source.length();

    while (line < originalLine-1 && stringCursor < strLength) {
      if (source.charAt(stringCursor) == '\n') {
        line++;
      }

      stringCursor++;
    }

    return source.substring(0, stringCursor);
  }

}
