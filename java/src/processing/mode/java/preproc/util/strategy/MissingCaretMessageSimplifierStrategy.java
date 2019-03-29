package processing.mode.java.preproc.util.strategy;

public class MissingCaretMessageSimplifierStrategy
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
