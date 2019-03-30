package processing.mode.java.preproc.util.strategy;

public class MissingMethodParamTypeMessageSimplifierStrategy extends RegexTemplateMessageSimplifierStrategy {
  @Override
  public String getRegexPattern() {
    return "[a-zA-Z0-9_]+\\s+[a-zA-Z0-9_]+\\s*\\(.*\\)\\s*\\{";
  }

  @Override
  public String getHintTemplate() {
    return "Is there an issue with a parameter definition near '%s'?";
  }
}
