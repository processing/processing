package processing.mode.java.preproc.util.strategy;

/**
 * Strategy to describe issue in an identifier name like an identifier starting with a digit.
 */
public class BadIdentifierMessageSimplifierStrategy extends RegexTemplateMessageSimplifierStrategy{

  @Override
  public String getRegexPattern() {
    return "([.\\s]*[0-9]+[a-zA-Z_<>]+[0-9a-zA-Z_<>]*|\\s+\\d+[a-zA-Z_<>]+|[0-9a-zA-Z_<>]+\\s+[0-9]+)";
  }

  @Override
  public String getHintTemplate() {
    return "Do you have an identifier starting with digits?";
  }

}
