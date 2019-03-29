package processing.mode.java.preproc.util.strategy;

import java.util.Optional;

public abstract class EvenCountTemplateMessageSimplifierStrategy
    implements SyntaxIssueMessageSimplifierStrategy {

  @Override
  public Optional<String> simplify(String message) {
    String messageContent = MessageSimplifierUtil.getOffendingArea(message);

    if (getFilter().isPresent()) {
      messageContent = messageContent.replace(getFilter().get(), "");
    }

    int count = MessageSimplifierUtil.getCount(messageContent, getToken());

    if (count % 2 == 0) {
      return Optional.empty();
    } else {
      String newMessage = String.format(
          "Syntax error. Hint: There are an uneven number of '%s'. Did you forget one?",
          getToken()
      );
      return Optional.of(newMessage);
    }
  }

  public abstract String getToken();

  public Optional<String> getFilter() {
    return Optional.empty();
  }

}
