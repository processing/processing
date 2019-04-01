package processing.mode.java.preproc.util;

import java.util.Optional;
import java.util.regex.Pattern;

public class PriorTokenFinder {

  private static final String WS_PATTERN = "[ \\t\\r\\n\\u000C]";

  private enum AutomatonState {
    UNKNOWN,
    POSSIBLE_TOKEN,
    TOKEN_OR_MAYBE_SL_COMMENT,
    TOKEN_OR_MAYBE_COMMENT,
    TOKEN,
    MULTI_LINE_COMMENT,
    MAYBE_LEAVE_MULTI_LINE_COMMENT
  }

  private boolean done;
  private Optional<Integer> tokenPosition;
  private AutomatonState state;
  private int charPosition;
  private Pattern whitespacePattern;

  public PriorTokenFinder() {
    whitespacePattern = Pattern.compile(WS_PATTERN);
    reset();
  }

  public boolean isDone() {
    return done;
  }

  public Optional<Integer> getTokenPositionMaybe() {
    return tokenPosition;
  }

  public void reset() {
    done = false;
    tokenPosition = Optional.empty();
    state = AutomatonState.UNKNOWN;
    charPosition = 0;
  }

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

  private void stepPossibleToken(char input) {
    if (input == '\n') {
      enterTokenState();
    } else if (input == '/') {
      state = AutomatonState.TOKEN_OR_MAYBE_SL_COMMENT;
    }

    // Else stay put
  }

  private void stepTokenOrMaybeSingleLineComment(char input) {
    if (input == '\n') {
      enterTokenState();
    } else if (input == '/') {
      returnToUnknownState();
    } else {
      state = AutomatonState.POSSIBLE_TOKEN;
    }
  }

  private void stepTokenOrMaybeComment(char input) {
    if (input == '\n') {
      enterTokenState();
    } else if (input == '/') {
      returnToUnknownState();
    } else if (input == '*') {
      enterMultilineComment();
    } else {
      state = AutomatonState.POSSIBLE_TOKEN;
    }
  }

  private void stepMultiLineComment(char input) {
    if (input == '*') {
      state = AutomatonState.MAYBE_LEAVE_MULTI_LINE_COMMENT;
    }

    // else stay put
  }

  private void stepMaybeLeaveMultiLineComment(char input) {
    if (input == '/') {
      state = AutomatonState.UNKNOWN;
    } else if (input != '*') {
      state = AutomatonState.MULTI_LINE_COMMENT;
    }

    // If * stay put
  }

  private void enterMultilineComment() {
    tokenPosition = Optional.of(charPosition);
    state = AutomatonState.MULTI_LINE_COMMENT;
  }

  private void enterTokenState() {
    done = true;
    state = AutomatonState.TOKEN;
  }

  private void returnToUnknownState() {
    tokenPosition = Optional.empty();
    state = AutomatonState.UNKNOWN;
  }

  private boolean isWhitespace(char input) {
    return whitespacePattern.matcher("" + input).find();
  }

}
