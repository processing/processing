package processing.mode.java.preproc.util.strategy;

import processing.mode.java.preproc.util.IssueMessageSimplification;

import java.util.Optional;
import java.util.regex.Pattern;


/**
 * Strategy that cleans up errors based on a regex matching the error message.
 */
public abstract class RegexTemplateMessageSimplifierStrategy
    implements SyntaxIssueMessageSimplifierStrategy {

  private Pattern pattern;

  /**
   * Create a new instance of this strategy.
   */
  public RegexTemplateMessageSimplifierStrategy() {
    pattern = Pattern.compile(getRegexPattern());
  }

  @Override
  public Optional<IssueMessageSimplification> simplify(String message) {
    if (pattern.matcher(message).find()) {
      String hint = String.format(
          getHintTemplate(),
          MessageSimplifierUtil.getOffendingArea(message)
      );

      String newMessage = "Syntax error. Hint: " + hint;

      return Optional.of(
          new IssueMessageSimplification(newMessage)
      );
    } else {
      return Optional.empty();
    }
  }

  /**
   * Get the regex that should be matched against the error message for this strategy to apply.
   *
   * @return The regex that should be matched in order to activate this strategy.
   */
  public abstract String getRegexPattern();

  /**
   * Get the hint template for this strategy.
   *
   * <p>
   * Get a template string with a "%s" where the "offending snippet of code" can be inserted where
   * the resulting rendered template can be used as an error hint for the user. For example,
   * "Invalid identifier near %s" may be rendered to the user like "Syntax error. Hint: Invalid
   * identifier near ,1a);" for example.
   * </p>
   *
   * @return The rendered hint template.
   */
  public abstract String getHintTemplate();

}
