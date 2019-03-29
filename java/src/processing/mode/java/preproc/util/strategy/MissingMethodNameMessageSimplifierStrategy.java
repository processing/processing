package processing.mode.java.preproc.util.strategy;

import java.util.Optional;

public class MissingMethodNameMessageSimplifierStrategy
    extends RegexTemplateMessageSimplifierStrategy{

  @Override
  public String getRegexPattern() {
    return "[a-zA-Z0-9_]+\\s+\\(.*";
  }

  @Override
  public String getMessageTemplate() {
    return "Did you forget to give your method a name near '%s'?";
  }

}
