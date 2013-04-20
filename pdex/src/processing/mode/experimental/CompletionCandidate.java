package processing.mode.experimental;

public class CompletionCandidate {

  private String definingClass;
  private String elementName;
  private String label;
  
  public CompletionCandidate(String name, String className, String label){
    definingClass = className;
    elementName = name;
    this.label = label;
  }
  
  public CompletionCandidate(String name, String className){
    definingClass = className;
    elementName = name;
    label = name;
  }
  
  public CompletionCandidate(String name){
    definingClass = "";
    elementName = name;
    label = name;
  }

  public String getDefiningClass() {
    return definingClass;
  }

  public String getElementName() {
    return elementName;
  }
 
  public String toString(){
    return label;
  }

}
