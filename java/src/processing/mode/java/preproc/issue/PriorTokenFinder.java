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

import java.util.Optional;
import java.util.regex.Pattern;


/**
 * Simple automaton that reads backwards from a position in source to find the prior token.
 *
 * <p>
 *   When helping generate messages for the user, it is often useful to be able to locate the
 *   position of the first token immediately before another location in source. For example,
 *   consider error reporting when a semicolon is missing. The error is generated on the token after
 *   the line on which the semicolon was omitted so, while the error technically emerges on the next
 *   line, it is better for the user for it to appear earlier. Specifically, it is most sensible for
 *   it to appear on the "prior token" because this is where it was forgotten.
 * </p>
 *
 * <p>
 *   To that end, this finite state automaton can read backwards from a position in source to locate
 *   the first "non-skip token" preceding that location. Here a "skip" token means one that is
 *   ignored by the preprocessor and does not impact output code (this includes comments and
 *   whitespace). This automaton will read character by character from source until it knows it has
 *   seen a non-skip token, returning the location of that non-skip token.
 * </p>
 *
 * <p>
 *   A formalized FSA is useful here in order to traverse code which can have a complex grammar.
 *   As there are a number of ways in the Java / Processing grammar one can encounter skip tokens,
 *   this formalized implementation describes the state machine directly in order to provide
 *   hopefully more readability / transparency compared to a regex without requiring the use of
 *   something heavier like ANTLR.
 * </p>
 */
public class PriorTokenFinder {

  // Simple regex matching all "whitespace" characters recognized by the ANTLR grammar.
  private static final String WS_PATTERN = "[ \\t\\r\\n\\u000C]";

  // Possible states for this FSA
  private enum AutomatonState {

    // Automaton is not certain if it is parsing a skip or non-skip character
    UNKNOWN,

    // Automaton has found a possible token but it is not sure if inside a comment
    POSSIBLE_TOKEN,

    // Automaton has found a token but also a forward slash so, if the next character is also a "/",
    // it is inside a single line comment.
    TOKEN_OR_MAYBE_SL_COMMENT,

    // Automaton has found a forward slash so, depending on the next character, it may be inside a
    // single line comment, multi-line comment, or it may have found a standalone token.
    TOKEN_OR_MAYBE_COMMENT,

    // Automaton has found a token and hit its terminal state.
    TOKEN,

    // Automaton is current traversing a multi-line comment.
    MULTI_LINE_COMMENT,

    // Automaton is maybe leaving a multi line comment because it found an "*". If it picks up a "/"
    // next, the automaton knows it is no longer within a multi-line comment.
    MAYBE_LEAVE_MULTI_LINE_COMMENT
  }

  private boolean done;
  private Optional<Integer> tokenPosition;
  private AutomatonState state;
  private int charPosition;
  private Pattern whitespacePattern;

  /**
   * Create a new automaton in unknown state and a character position of zero.
   */
  public PriorTokenFinder() {
    whitespacePattern = Pattern.compile(WS_PATTERN);
    reset();
  }

  /**
   * Determine if this automaton has found a token.
   *
   * @return True if this automaton has found a token and, thus, is in terminal state (so will
   *    ignore all future input). False if this autoamton has not yet found a token since creation
   *    or last call to reset.
   */
  public boolean isDone() {
    return done;
  }

  /**
   * Get the position of the token found.
   *
   * @return Optional containing the number of characters processed prior to finding the token or
   *    empty if no token found. Note that this is different the number of total characters
   *    processed as some extra characters have to be read prior to the token itself to ensure it is
   *    not part of a comment or something similar.
   */
  public Optional<Integer> getTokenPositionMaybe() {
    return tokenPosition;
  }

  /**
   * Reset this automaton to UNKNOWN state with a character count of zero.
   */
  public void reset() {
    done = false;
    tokenPosition = Optional.empty();
    state = AutomatonState.UNKNOWN;
    charPosition = 0;
  }

  /**
   * Process a character.
   *
   * <p>
   *   Process the next character in an effort to find the "prior token". Note that this is
   *   expecting the processing sketch source code to be fed one character at a time
   *   <i>backwards</i> from the starting position in code. This is because it is looking for the
   *   first non-skip token immediately <i>preceding</i> a position in source.
   * </p>
   *
   * @param input The next character to process.
   */
  public void step(char input) {
    switch(state) {
      case UNKNOWN: stepUnknown(input); break;
      case POSSIBLE_TOKEN: stepPossibleToken(input); break;
      case TOKEN_OR_MAYBE_SL_COMMENT: stepTokenOrMaybeSingleLineComment(input); break;
      case TOKEN_OR_MAYBE_COMMENT: stepTokenOrMaybeComment(input); break;
      case MULTI_LINE_COMMENT: stepMultiLineComment(input); break;
      case MAYBE_LEAVE_MULTI_LINE_COMMENT: stepMaybeLeaveMultiLineComment(input); break;
      case TOKEN: /* Already have token. Nothing to be done. */ break;
    }

    charPosition++;
  }

  /**
   * Process the next character while in the UNKNOWN state.
   *
   * <p>
   *   While not certain if looking at a skip or non-skip token, read the next character. If
   *   whitespace, can ignore. If a forward slash, could indicate either a comment or a possible
   *   token (move to TOKEN_OR_MAYBE_COMMENT). If anything else, may have found token but need to
   *   ensure this line isn't part of a comment (move to POSSIBLE_TOKEN).
   * </p>
   *
   * @param input The next character to process.
   */
  private void stepUnknown(char input) {
    if (isWhitespace(input)) {
      return;
    }

    tokenPosition = Optional.of(charPosition);

    if (input == '/') {
      state = AutomatonState.TOKEN_OR_MAYBE_COMMENT;
    } else {
      state = AutomatonState.POSSIBLE_TOKEN;
    }
  }

  /**
   * Process the next character while in the POSSIBLE_TOKEN state.
   *
   * <p>
   *   After having found a character that could indicate a token, need to ensure that the token
   *   wasn't actually part of a single line comment ("//") so look for forward slashes (if found
   *   move to TOKEN_OR_MAYBE_SL_COMMENT). If encountered a newline, the earlier found token was
   *   not part of a comment so enter TOKEN state.
   * </p>
   *
   * @param input The next character to process.
   */
  private void stepPossibleToken(char input) {
    if (input == '\n') {
      enterNonSkipTokenState();
    } else if (input == '/') {
      state = AutomatonState.TOKEN_OR_MAYBE_SL_COMMENT;
    }

    // Else stay put
  }

  /**
   * Process the next character while in the TOKEN_OR_MAYBE_SL_COMMENT state.
   *
   * <p>
   *   After having found a forward slash after encountering something else which may be a non-skip
   *   token, one needs to check that it is preceded by another forward slash to have detected a
   *   single line comment (return to UNKNOWN state). If found a new line, that forward slash was
   *   actually a non-skip token itself so enter TOKEN state. Finally, if anything else, it is still
   *   possible that we are traversing a single line comment so return to POSSIBLE_TOKEN state.
   * </p>
   *
   * @param input The next character to process.
   */
  private void stepTokenOrMaybeSingleLineComment(char input) {
    if (input == '\n') {
      enterNonSkipTokenState();
    } else if (input == '/') {
      returnToUnknownState();
    } else {
      state = AutomatonState.POSSIBLE_TOKEN;
    }
  }

  /**
   * Process the next character while in the TOKEN_OR_MAYBE_COMMENT state.
   *
   * <p>
   *   After having found a forward slash without encountering something else that may be a non-skip
   *   token: that forward slash is a non-skip token if preceded by a newline, could be a single
   *   line comment if preceded by a forward slash, could be a multi-line comment if preceded
   *   by an asterisk, or could by a non-skip token otherwise.
   * </p>
   *
   * @param input The next character to process.
   */
  private void stepTokenOrMaybeComment(char input) {
    if (input == '\n') {
      enterNonSkipTokenState();
    } else if (input == '/') {
      returnToUnknownState();
    } else if (input == '*') {
      enterMultilineComment();
    } else {
      state = AutomatonState.POSSIBLE_TOKEN;
    }
  }

  /**
   * Process the next character while in the MULTI_LINE_COMMENT state.
   *
   * <p>
   *   Process the next character while traversing a multi-line comment. If an asterisk, we may be
   *   encountering the end of the multiline comment (move to MAYBE_LEAVE_MULTI_LINE_COMMENT).
   *   Otherwise, can ignore character.
   * </p>
   *
   * @param input The next character to process.
   */
  private void stepMultiLineComment(char input) {
    if (input == '*') {
      state = AutomatonState.MAYBE_LEAVE_MULTI_LINE_COMMENT;
    }

    // else stay put
  }

  /**
   * Process the next character while in the MAYBE_LEAVE_MULTI_LINE_COMMENT state.
   *
   * <p>
   *   If already found an asterisk while inside a multi-line comment, one may be leaving the multi-
   *   line comment depending on the next character. If forward slash, at end of comment (return to
   *   UNKNOWN state). If another asterisk, could still end comment depending on next character
   *   (stay in current state). Finally, if anything else, we are still in the body of the multi-
   *   line comment and not about to leave (return to MULTI_LINE_COMMENT state).
   * </p>
   *
   * @param input
   */
  private void stepMaybeLeaveMultiLineComment(char input) {
    if (input == '/') {
      state = AutomatonState.UNKNOWN;
    } else if (input != '*') {
      state = AutomatonState.MULTI_LINE_COMMENT;
    }

    // If * stay put
  }

  /**
   * Convenience function to set up internal FSA state when entering a multi-line comment.
   */
  private void enterMultilineComment() {
    tokenPosition = Optional.of(charPosition);
    state = AutomatonState.MULTI_LINE_COMMENT;
  }

  /**
   * Convenience function to set up internal FSA state when having found a non-skip token.
   */
  private void enterNonSkipTokenState() {
    done = true;
    state = AutomatonState.TOKEN;
  }

  /**
   * Convenience function to set up internal FSA state when entering UNKNOWN state.
   */
  private void returnToUnknownState() {
    tokenPosition = Optional.empty();
    state = AutomatonState.UNKNOWN;
  }

  /**
   * Convenience function which determines if a character is whitespace.
   *
   * @param input The character to test.
   * @return True if whitespace. False otherwise.
   */
  private boolean isWhitespace(char input) {
    return whitespacePattern.matcher("" + input).find();
  }

}
