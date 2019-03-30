package processing.mode.java.preproc.util.strategy;

import java.util.Optional;


/**
 * Strategy to check for an open single quote without a corresponding close single quote.
 */
public class MissingSingleQuoteMessageSimplifierStrategy
    extends EvenCountTemplateMessageSimplifierStrategy {

  @Override
  public String getToken() {
    return "\'";
  }

  @Override
  public Optional<String> getFilter() {
    return Optional.of("\\'");
  }

}
