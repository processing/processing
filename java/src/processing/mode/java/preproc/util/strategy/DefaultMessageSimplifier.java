package processing.mode.java.preproc.util.strategy;

import processing.mode.java.preproc.util.SyntaxIssueMessageSimplifier;

import java.util.Optional;

public class DefaultMessageSimplifier implements SyntaxIssueMessageSimplifierStrategy {

  @Override
  public Optional<String> simplify(String message) {
    if (message.contains("viable alternative")) {
      String newMessage = String.format(
          "Syntax error on '%s'. Did you misspell a variable or method?",
          message
      );
      return Optional.of(newMessage);
    } else {
      return Optional.of(message);
    }
  }

}
