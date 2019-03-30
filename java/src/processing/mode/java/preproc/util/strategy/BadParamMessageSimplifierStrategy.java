package processing.mode.java.preproc.util.strategy;

/**
 * Strategy to check for an error in specifying a parameter value.
 */
public class BadParamMessageSimplifierStrategy
    extends RegexTemplateMessageSimplifierStrategy {

  @Override
  public String getRegexPattern() {
    return "([a-zA-Z0-9_]+\\s*,|[a-zA-Z0-9_]\\)|\\([^\\)]+)";
  }

  @Override
  public String getHintTemplate() {
    return "Issue with parameter near '%s'?";
  }

}
