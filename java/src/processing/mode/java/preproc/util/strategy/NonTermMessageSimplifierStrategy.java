package processing.mode.java.preproc.util.strategy;

import processing.mode.java.preproc.util.IssueMessageSimplification;

import java.util.Optional;

/**
 * Clean up errors for a non-terminated statement like a statement without a semicolon.
 */
public class NonTermMessageSimplifierStrategy implements SyntaxIssueMessageSimplifierStrategy {

  @Override
  public Optional<IssueMessageSimplification> simplify(String message) {
    if (message.contains(";")) {
      return Optional.empty();
    }

    String newMessage = String.format(
        "Syntax error. Hint: Are you missing semicolon, identifier, or brace near '%s'?",
        MessageSimplifierUtil.getOffendingArea(message)
    );

    return Optional.of(
        new IssueMessageSimplification(newMessage)
    );
  }

}
