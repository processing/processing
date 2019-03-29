package processing.mode.java.preproc.util.strategy;

public class BadIdentifierMessageSimplifierStrategy extends RegexTemplateMessageSimplifierStrategy{

  @Override
  public String getRegexPattern() {
    return "([.\\s]*[0-9]+[a-zA-Z_]+[0-9a-zA-Z_]*|\\s+\\d+[a-zA-Z_]+)";
  }

  @Override
  public String getMessageTemplate() {
    return "Do you have an identifier starting with digits?";
  }

}
