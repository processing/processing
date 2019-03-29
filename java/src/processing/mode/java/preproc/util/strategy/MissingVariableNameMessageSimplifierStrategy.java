package processing.mode.java.preproc.util.strategy;

public class MissingVariableNameMessageSimplifierStrategy
    extends RegexTemplateMessageSimplifierStrategy {

  @Override
  public String getRegexPattern() {
    return "[a-zA-Z_]+[0-9a-zA-Z_]*\\s*(=.*|;).*";
  }

  @Override
  public String getMessageTemplate() {
    return "Did you forget to give a variable its name or forget to call a method?";
  }
}
