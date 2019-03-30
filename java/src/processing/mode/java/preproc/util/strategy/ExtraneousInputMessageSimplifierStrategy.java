package processing.mode.java.preproc.util.strategy;

import processing.mode.java.preproc.util.IssueMessageSimplification;

import java.util.Optional;


/**
 * Strategy to handle extraneous input messages.
 */
public class ExtraneousInputMessageSimplifierStrategy
    implements SyntaxIssueMessageSimplifierStrategy {

  @Override
  public Optional<IssueMessageSimplification> simplify(String message) {
    if (message.toLowerCase().contains("extraneous")) {
      String newMessage = String.format("Syntax error. Hint: %s.", message);
      return Optional.of(
          new IssueMessageSimplification(newMessage)
      );
    } else {
      return Optional.empty();
    }
  }

}
