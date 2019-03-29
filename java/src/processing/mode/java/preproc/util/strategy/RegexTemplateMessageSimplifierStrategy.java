package processing.mode.java.preproc.util.strategy;

import java.util.Optional;
import java.util.regex.Pattern;

public abstract class RegexTemplateMessageSimplifierStrategy
    implements SyntaxIssueMessageSimplifierStrategy {

  private Pattern pattern;

  public RegexTemplateMessageSimplifierStrategy() {
    pattern = Pattern.compile(getRegexPattern());
  }

  @Override
  public Optional<String> simplify(String message) {
    if (pattern.matcher(message).find()) {
      String hint = String.format(
          getMessageTemplate(),
          MessageSimplifierUtil.getOffendingArea(message)
      );

      String newMessage = "Syntax error. Hint: " + hint;

      return Optional.of(newMessage);
    } else {
      return Optional.empty();
    }
  }

  public abstract String getRegexPattern();

  public abstract String getMessageTemplate();

}
