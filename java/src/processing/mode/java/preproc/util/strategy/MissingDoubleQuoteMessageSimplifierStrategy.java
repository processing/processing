package processing.mode.java.preproc.util.strategy;

import java.util.Optional;

public class MissingDoubleQuoteMessageSimplifierStrategy
    extends EvenCountTemplateMessageSimplifierStrategy {

  @Override
  public String getToken() {
    return "\"";
  }

  @Override
  public Optional<String> getFilter() {
    return Optional.of("\\\"");
  }

}
