package processing.mode.java.preproc.util.strategy;


/**
 * Strategy to describe an issue in an assignment.
 */
public class AssignmentMessageSimplifierStrategy extends RegexTemplateMessageSimplifierStrategy {

  @Override
  public String getRegexPattern() {
    return "\\s*[0-9a-zA-Z\\_<>]+\\s*=\\s*.*";
  }

  @Override
  public String getHintTemplate() {
    return "Possible issue in assignment: '%s'. Forgotten value, var name, semicolon, or loose '='?";
  }

}
