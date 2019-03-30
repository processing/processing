package processing.mode.java.preproc.util.strategy;


/**
 * Strategy to check that every open curly has a corresponding close curly.
 */
public class MissingCurlyMessageSimplifierStrategy
    extends TokenPairTemplateMessageSimplifierStrategy {

  @Override
  public String getToken1() {
    return "{";
  }

  @Override
  public String getToken2() {
    return "}";
  }

}
