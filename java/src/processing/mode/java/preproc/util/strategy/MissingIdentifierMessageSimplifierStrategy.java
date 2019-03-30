package processing.mode.java.preproc.util.strategy;

import processing.mode.java.preproc.util.IssueMessageSimplification;

import java.util.Optional;


/**
 * Strategy to check for an error indicating that an identifier was expected but not given.
 */
public class MissingIdentifierMessageSimplifierStrategy
    implements SyntaxIssueMessageSimplifierStrategy {

  @Override
  public Optional<IssueMessageSimplification> simplify(String message) {
    if (message.toLowerCase().contains("missing identifier at")) {
      String newMessage = String.format(
          "Syntax error. Hint: Did you forget an identifier near %s ?",
          message.replace("missing Identifier at", "")
      );
      return Optional.of(
          new IssueMessageSimplification(newMessage, -1)
      );
    } else {
      return Optional.empty();
    }
  }

}
