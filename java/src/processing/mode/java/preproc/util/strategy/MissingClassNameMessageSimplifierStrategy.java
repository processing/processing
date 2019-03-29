package processing.mode.java.preproc.util.strategy;

public class MissingClassNameMessageSimplifierStrategy extends RegexTemplateMessageSimplifierStrategy {

  @Override
  public String getRegexPattern() {
    return ".*class\\s*[a-zA-Z0-9_]*\\s+(extends|implements|<.*>)?\\s*[a-zA-Z0-9_]*\\s*\\{.*";
  }

  @Override
  public String getMessageTemplate() {
    return "Did you forget to give a class name near '%s'?";
  }

}
