package processing.mode.java.preproc.util.strategy;

import processing.mode.java.preproc.util.IssueMessageSimplification;

import java.util.Optional;


/**
 * Strategy to handle missing token messages.
 */
public class KnownMissingMessageSimplifierStrategy implements SyntaxIssueMessageSimplifierStrategy {

  @Override
  public Optional<IssueMessageSimplification> simplify(String message) {
    if (message.toLowerCase().contains("missing")) {
      String newContents = message.replaceAll("' at '.*", "' near here");

      String newMessage = String.format(
          "Syntax error. Hint: Are you %s?",
          newContents
      );

      return Optional.of(
          new IssueMessageSimplification(newMessage, true)
      );
    } else {
      return Optional.empty();
    }
  }

}
