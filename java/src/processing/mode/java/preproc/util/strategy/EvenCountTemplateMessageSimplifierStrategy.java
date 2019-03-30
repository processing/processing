package processing.mode.java.preproc.util.strategy;

import processing.mode.java.preproc.util.IssueMessageSimplification;
import processing.mode.java.preproc.util.SyntaxUtil;

import java.util.Optional;


/**
 * Strategy to check to make sure that the number of occurrences of a token are even.
 *
 * <p>
 *   Strategy to ensure that there are an even number of tokens like even number of double quotes
 *   for example.
 * </p>
 */
public abstract class EvenCountTemplateMessageSimplifierStrategy
    implements SyntaxIssueMessageSimplifierStrategy {

  @Override
  public Optional<IssueMessageSimplification> simplify(String message) {
    String messageContent = MessageSimplifierUtil.getOffendingArea(message);

    if (getFilter().isPresent()) {
      messageContent = messageContent.replace(getFilter().get(), "");
    }

    int count = SyntaxUtil.getCount(messageContent, getToken());

    if (count % 2 == 0) {
      return Optional.empty();
    } else {
      String newMessage = String.format(
          "Syntax error. Hint: There are an uneven number of '%s'. Did you forget one?",
          getToken()
      );
      return Optional.of(
          new IssueMessageSimplification(newMessage)
      );
    }
  }

  /**
   * Get the token that should be counted.
   *
   * @return The token whose occurrences should be even.
   */
  public abstract String getToken();

  /**
   * Get the text that should be removed before counting.
   *
   * @return An optional string whose occurrences will be removed prior to counting.
   */
  public Optional<String> getFilter() {
    return Optional.empty();
  }

}
