package processing.mode.java.preproc.util.strategy;


/**
 * Strategy to check for an opening parentheses without a close parantheses.
 */
public class MissingParenMessageSimplifierStrategy
    extends TokenPairTemplateMessageSimplifierStrategy {

  @Override
  public String getToken1() {
    return "(";
  }

  @Override
  public String getToken2() {
    return ")";
  }

}
