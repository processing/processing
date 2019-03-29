package processing.mode.java.preproc.util.strategy;

import java.util.Optional;

public abstract class TokenPairTemplateMessageSimplifierStrategy
    implements SyntaxIssueMessageSimplifierStrategy {

  @Override
  public Optional<String> simplify(String message) {
    String messageContent = MessageSimplifierUtil.getOffendingArea(message);

    int count1 = MessageSimplifierUtil.getCount(messageContent, getToken1());
    int count2 = MessageSimplifierUtil.getCount(messageContent, getToken2());

    if (count1 == count2) {
      return Optional.empty();
    }

    String newMessage = String.format(
        "Syntax error. Hint: Maybe unequal '%s' and '%s'. Forget one or have unterminated strings / comment?",
        getToken1().replace("\\", ""),
        getToken2().replace("\\", "")
    );

    return Optional.of(newMessage);
  }

  public abstract String getToken1();
  public abstract String getToken2();

}
