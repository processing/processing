package processing.mode.java.preproc.util.strategy;

public class MissingParamTypeMessageSimplifierStrategy
    extends RegexTemplateMessageSimplifierStrategy {

  @Override
  public String getRegexPattern() {
    return ".*\\(([a-zA-Z0-9_]+\\s+[a-zA-Z0-9_]+\\s*\\,\\s*)?([a-zA-Z0-9_]+\\s*)\\).*";
  }

  @Override
  public String getMessageTemplate() {
    return "Missing parameter type near '%s' or no parameter name?";
  }

}
