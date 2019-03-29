package processing.mode.java.preproc.util.strategy;

import java.util.Optional;

public class SemicolonMessageSimplifierStrategy implements SyntaxIssueMessageSimplifierStrategy {

  @Override
  public Optional<String> simplify(String message) {
    if (message.contains(";")) {
      return Optional.empty();
    }

    String newMessage = String.format(
        "Syntax error. Hint: Are you missing a semicolon near '%s'?",
        MessageSimplifierUtil.getOffendingArea(message)
    );

    return Optional.of(newMessage);
  }

}
