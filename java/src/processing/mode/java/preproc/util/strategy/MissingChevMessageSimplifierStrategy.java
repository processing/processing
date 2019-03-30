package processing.mode.java.preproc.util.strategy;

/**
 * Strategy to check for a missing chevron.
 */
public class MissingChevMessageSimplifierStrategy
    extends TokenPairTemplateMessageSimplifierStrategy {

  @Override
  public String getToken1() {
    return "<";
  }

  @Override
  public String getToken2() {
    return ">";
  }

}
