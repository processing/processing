package processing.mode.java.preproc.util.strategy;

/**
 * Strategy to check for a class definition without a name.
 */
public class MissingClassNameMessageSimplifierStrategy extends RegexTemplateMessageSimplifierStrategy {

  @Override
  public String getRegexPattern() {
    return ".*(class|interface)\\s*[a-zA-Z0-9_]*\\s+(extends|implements|<.*>)?\\s*[a-zA-Z0-9_]*\\s*\\{.*";
  }

  @Override
  public String getHintTemplate() {
    return "Forget class or interface name near '%s'?";
  }

}
