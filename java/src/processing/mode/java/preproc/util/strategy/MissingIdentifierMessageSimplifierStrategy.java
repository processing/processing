package processing.mode.java.preproc.util.strategy;

import java.util.Optional;

public class MissingIdentifierMessageSimplifierStrategy
    implements SyntaxIssueMessageSimplifierStrategy {

  @Override
  public Optional<String> simplify(String message) {
    if (message.toLowerCase().contains("missing identifier at")) {
      String newMessage = String.format(
          "Syntax error. Hint: Did you forget an identifier near %s ?",
          message.replace("missing Identifier at", "")
      );
      return Optional.of(newMessage);
    } else {
      return Optional.empty();
    }
  }

}
