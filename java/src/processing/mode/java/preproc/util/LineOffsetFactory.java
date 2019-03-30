package processing.mode.java.preproc.util;

import processing.mode.java.preproc.util.strategy.MessageSimplifierUtil;

import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Optionally used utility that can help clean up on which line an error is reported.
 */
public class LineOffsetFactory {

  // Derived from ANTLR grammar
  private static final String SKIP_PATTERN_STR = "([ \\t\\r\\n\\u000C]+|\\/\\*(.|\\n)*?\\*\\/|\\/\\*\\*\\/|\\/\\/[^\\r\\n]*)+$";

  private static final AtomicReference<LineOffsetFactory> instance = new AtomicReference<>();

  private final Pattern skipPattern;

  public static LineOffsetFactory get() {
    instance.compareAndSet(null, new LineOffsetFactory());
    return instance.get();
  }

  private LineOffsetFactory() {
    skipPattern = Pattern.compile(SKIP_PATTERN_STR);
  }

  public LineOffset getLineWithOffset(IssueMessageSimplification simplification, int originalLine,
        int originalOffset, String source) {

    int finalLine = SyntaxUtil.getCount(source, "\n");

    boolean shouldAttributeToPrior = simplification.getAttributeToPriorToken();
    shouldAttributeToPrior = shouldAttributeToPrior && originalLine != finalLine;

    if (!shouldAttributeToPrior) {
      return new LineOffset(originalLine, originalOffset);
    }

    String priorCode = getContentsUpToLine(source, originalLine);

    Matcher matcher = skipPattern.matcher(priorCode);

    int startOfMatch;
    int linesOffset;

    boolean found;
    try {
      found = matcher.find();
    } catch (Exception | Error e) {
      String innerMessage = e.getMessage().substring(0, 100);
      System.err.println("Error parsing whitespace: " + innerMessage + ". However, showing error.");
      found = false;
    }

    if (found) {
      startOfMatch = matcher.start();
      String contentsOfMatch = priorCode.substring(startOfMatch);
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

  private String getContentsUpToLine(String source, int originalLine) {
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
