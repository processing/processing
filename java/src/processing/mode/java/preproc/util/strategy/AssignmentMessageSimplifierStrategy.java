package processing.mode.java.preproc.util.strategy;

public class AssignmentMessageSimplifierStrategy extends RegexTemplateMessageSimplifierStrategy {

  @Override
  public String getRegexPattern() {
    return "\\s*[0-9a-zA-Z\\_]+\\s*=\\s*.*";
  }

  @Override
  public String getMessageTemplate() {
    return "Possible issue in assignment: '%s'. Forgotten value, var name, semicolon, or loose '='?";
  }

}
