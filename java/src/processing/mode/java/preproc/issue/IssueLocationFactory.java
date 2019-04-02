/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
Part of the Processing project - http://processing.org

Copyright (c) 2012-19 The Processing Foundation

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License version 2
as published by the Free Software Foundation.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software Foundation,
Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package processing.mode.java.preproc.issue;

import processing.mode.java.preproc.code.SyntaxUtil;

import java.util.Optional;


/**
 * Utility that can help clean up where in source an issue should be reported.
 *
 * <p>
 *    For some errors, the location of the "mistake" does not appear close to where the actual error
 *    is generated. For example, consider omitting a semicolon. Though the "mistake" is arguably on
 *    the line on which a semicolon is forgotten, the grammatical error appears in the first
 *    non-skip token after the omitted character. This means that the issue shown to the user may
 *    be far away from the line they would want to edit. This utility helps determine if an issue
 *    requires a new location and, if so, where the location should be.
 * </p>
 */
public class IssueLocationFactory {

  /**
   * Determine where an issue should be reported.
   *
   * @param simplification The issue simplification generated from {PreprocessIssueMessageSimplifier}.
   * @param originalLine The original line (1 indexed) on which the issue was reported.
   * @param originalOffset The original number of characters from the start of the line where the
   *    the issue was reported.
   * @param source The full concatenated source of the sketch being built.
   * @param lineCount The total
   * @return The new location where the issue should be reported. This may be identical to the
   *    original location if the issue was not moved.
   */
  public static IssueLocation getLineWithOffset(IssueMessageSimplification simplification,
        int originalLine, int originalOffset, String source) {

    // Determine if the issue should be relocated
    boolean shouldAttributeToPrior = simplification.getAttributeToPriorToken();
    shouldAttributeToPrior = shouldAttributeToPrior && originalLine != 0;

    if (!shouldAttributeToPrior) {
      return new IssueLocation(originalLine, originalOffset);
    }

    // Find the code prior the issue
    String priorCode = getContentsUpToLine(source, originalLine);

    // Find the token immediately prior to the issue
    PriorTokenFinder finder = new PriorTokenFinder();
    int charPos = priorCode.length();
    while (!finder.isDone() && charPos > 0) {
      charPos--;
      finder.step(priorCode.charAt(charPos));
    }

    // Find the location offset depending on if the prior token could be found
    Optional<Integer> foundStartOfMatchMaybe = finder.getTokenPositionMaybe();
    int startOfMatch;
    int linesOffset;

    if (foundStartOfMatchMaybe.isPresent()) {
      startOfMatch = priorCode.length() - foundStartOfMatchMaybe.get();
      String contentsOfMatch = priorCode.substring(startOfMatch);
      linesOffset = SyntaxUtil.getCount(contentsOfMatch, "\n");
    } else {
      startOfMatch = priorCode.length();
      linesOffset = 0;
    }

    // Apply the location offset and highlight to the end of the line
    String contentsPriorToMatch = priorCode.substring(0, startOfMatch);
    int newLine = originalLine - linesOffset;
    int lengthIncludingLine = contentsPriorToMatch.length();
    int lengthExcludingLine = contentsPriorToMatch.lastIndexOf('\n');
    int lineLength = lengthIncludingLine - lengthExcludingLine;
    int col = lineLength - 1; // highlight from start of line to end

    // Build the new issue location
    return new IssueLocation(newLine, col);
  }

  /**
   * Get all of the contents of source leading up to a line.
   *
   * @param source The full concatenated sketch source.
   * @param endLineExclusive The line up to which code should be returned. Note that this is an
   *    "exclusive" boundary. Code from this line itself will not be included.
   * @return All of the sketch code leading up to but not including the line given.
   */
  private static String getContentsUpToLine(String source, int endLineExclusive) {
    int line = 0;
    int stringCursor = 0;
    int strLength = source.length();

    while (line < endLineExclusive-1 && stringCursor < strLength) {
      if (source.charAt(stringCursor) == '\n') {
        line++;
      }

      stringCursor++;
    }

    return source.substring(0, stringCursor);
  }

}
