package processing.mode.java.preproc.util.strategy;


/**
 * Strategy to check for a method declaration without a name or return type.
 */
public class MissingMethodNameMessageSimplifierStrategy
    extends RegexTemplateMessageSimplifierStrategy{

  @Override
  public String getRegexPattern() {
    return "[a-zA-Z0-9_]+\\s*\\(.*\\)\\s*\\{";
  }

  @Override
  public String getHintTemplate() {
    return "Did you forget to give your method a name or return type near '%s'?";
  }

}
