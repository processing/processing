package processing.mode.experimental;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.MethodDeclaration;

public class CompletionCandidate {

  private String definingClass;
  private String elementName; //
  private String label; // the toString value
  private String completionString;
  private int type;
  public static final int METHOD = 0, FIELD = 1, LOCAL_VAR = 3; 
  
  public CompletionCandidate(String name, String className, String label, int TYPE){
    definingClass = className;
    elementName = name;
    if(label.length() > 0)
      this.label = label;
    else
      this.label = name;
    this.type = TYPE;
    if(type == METHOD){
      this.label += "()";
    }
    
  }
  
  public CompletionCandidate(Method method){
    definingClass = method.getDeclaringClass().getName();
    elementName = method.getName();
    type = METHOD;
    StringBuffer label = new StringBuffer(method.getName() + "(");
    for (int i = 0; i < method.getParameterTypes().length; i++) {
      label.append(method.getParameterTypes()[i].getSimpleName());
      if (i < method.getParameterTypes().length - 1)
        label.append(",");
    }

    label.append(")");
    this.label = label.toString();
  }
  
  public CompletionCandidate(MethodDeclaration method){
    definingClass = "";
    elementName = method.getName().toString();
    type = METHOD;
    List<ASTNode> params = (List<ASTNode>) method
        .getStructuralProperty(MethodDeclaration.PARAMETERS_PROPERTY);
    StringBuffer label = new StringBuffer(elementName + "(");
    for (int i = 0; i < params.size(); i++) {
      label.append(params.get(i).toString());
      if (i < params.size() - 1)
        label.append(",");
    }
    label.append(")");
    this.label = label.toString();
  }
  
  
  public CompletionCandidate(Field f){
    definingClass = f.getDeclaringClass().getName();
    elementName = f.getName();
    type = FIELD;
    label = f.getName();
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
  
  public String getCompletionString(){
    return completionString;
  }
 
  public String toString(){
    return label;
  }
  
  public int getType(){
    return type;
  }

}
