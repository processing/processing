package processing.mode.java.preproc.util.strategy;

import processing.mode.java.preproc.util.IssueMessageSimplification;

import java.util.Optional;


/**
 * Default strategy to use if other message simplification strategies have failed.
 */
public class DefaultMessageSimplifier implements SyntaxIssueMessageSimplifierStrategy {

  @Override
  public Optional<IssueMessageSimplification> simplify(String message) {
    if (message.contains("viable alternative")) {
      String newMessage = String.format(
          "Syntax error on '%s'. Did you misspell something or forget to call a method?",
          MessageSimplifierUtil.getOffendingArea(message)
      );
      return Optional.of(
          new IssueMessageSimplification(newMessage)
      );
    } else {
      return Optional.of(
          new IssueMessageSimplification(message)
      );
    }
  }

}
