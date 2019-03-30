package processing.mode.java.preproc.util.strategy;

import processing.mode.java.preproc.util.IssueMessageSimplification;
import processing.mode.java.preproc.util.SyntaxUtil;

import java.util.Optional;


/**
 * Template class for checking that two tokens appear in pairs.
 *
 * <p>
 * Template class for message simplification strategies that check for an equal number of
 * occurrences for two characters like "(" and ")".
 * </p>
 */
public abstract class TokenPairTemplateMessageSimplifierStrategy
    implements SyntaxIssueMessageSimplifierStrategy {

  @Override
  public Optional<IssueMessageSimplification> simplify(String message) {
    String messageContent = MessageSimplifierUtil.getOffendingArea(message);

    int count1 = SyntaxUtil.getCount(messageContent, getToken1());
    int count2 = SyntaxUtil.getCount(messageContent, getToken2());

    if (count1 == count2) {
      return Optional.empty();
    }

    String newMessage = String.format(
        "Syntax error. Hint: Maybe unequal '%s' and '%s'. Forget one or have unterminated strings / comment?",
        getToken1().replace("\\", ""),
        getToken2().replace("\\", "")
    );

    return Optional.of(
        new IssueMessageSimplification(newMessage)
    );
  }

  /**
   * Get the first token in the pair.
   *
   * @return The first token whose occurrences should be counted.
   */
  public abstract String getToken1();


  /**
   * Get the second token in the pair.
   *
   * @return The second token whose occurrences should be counted.
   */
  public abstract String getToken2();

}
