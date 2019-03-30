package processing.mode.java.preproc.util.strategy;

import processing.mode.java.preproc.util.IssueMessageSimplification;

import java.util.Optional;

public class MismatchedInputMessageSimplifierStrategy implements SyntaxIssueMessageSimplifierStrategy {
  @Override
  public Optional<IssueMessageSimplification> simplify(String message) {
    if (message.toLowerCase().contains("mismatched input")) {
      return Optional.of(
          new IssueMessageSimplification("Syntax error. Hint: Did you forget an operator or semicolon here?")
      );
    } else {
      return Optional.empty();
    }
  }
}
