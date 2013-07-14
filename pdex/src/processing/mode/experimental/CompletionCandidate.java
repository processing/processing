package processing.mode.experimental;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

public class CompletionCandidate implements Comparable<CompletionCandidate>{

  private String definingClass;

  private String elementName; //

  private String label; // the toString value

  private String completionString;

  private int type;

  public static final int METHOD = 0, FIELD = 1, LOCAL_VAR = 3;

  public CompletionCandidate(String name, String className, String label,
                             int TYPE) {
    definingClass = className;
    elementName = name;
    if (label.length() > 0)
      this.label = label;
    else
      this.label = name;
    this.type = TYPE;
    if (type == METHOD) {
      this.label += "()";
    }
    completionString = this.label;
  }

  public CompletionCandidate(Method method) {
    definingClass = method.getDeclaringClass().getName();
    elementName = method.getName();
    type = METHOD;
    StringBuffer label = new StringBuffer(method.getName() + "(");
    StringBuffer cstr = new StringBuffer(method.getName() + "(");
    for (int i = 0; i < method.getParameterTypes().length; i++) {
      label.append(method.getParameterTypes()[i].getSimpleName());
      if (i < method.getParameterTypes().length - 1) {
        label.append(",");
        cstr.append(",");
      }
    }
    if(method.getParameterTypes().length == 1) {
      cstr.append(' ');
    }
    label.append(")");
    label.append(" : "+method.getReturnType().getSimpleName());
    cstr.append(")");
    this.label = label.toString();
    this.completionString = cstr.toString();
  }
  
  public CompletionCandidate(SingleVariableDeclaration svd) {
    completionString = svd.getName().toString();
    elementName = svd.getName().toString();
    type = LOCAL_VAR;
    label = svd.getName() + " : " + svd.getType();
  }
  
  public CompletionCandidate(VariableDeclarationFragment  vdf) {
    completionString = vdf.getName().toString();
    elementName = vdf.getName().toString();
    type = LOCAL_VAR;
    label = vdf.getName() + " : " + ASTGenerator.extracTypeInfo2(vdf);
  }
  
  public CompletionCandidate(MethodDeclaration method) {
    System.out.println("ComCan " + method.getName());
    definingClass = "";
    elementName = method.getName().toString();
    type = METHOD;
    List<ASTNode> params = (List<ASTNode>) method
        .getStructuralProperty(MethodDeclaration.PARAMETERS_PROPERTY);
    StringBuffer label = new StringBuffer(elementName + "(");
    StringBuffer cstr = new StringBuffer(method.getName() + "(");
    for (int i = 0; i < params.size(); i++) {
      label.append(params.get(i).toString());
      if (i < params.size() - 1){
        label.append(",");
        cstr.append(",");
      }
    }
    if(params.size() == 1) {
      cstr.append(' ');
    }
    label.append(")");
    label.append(" : "+method.getReturnType2());
    cstr.append(")");
    this.label = label.toString();
    this.completionString = cstr.toString();
  }

  public CompletionCandidate(Field f) {
    definingClass = f.getDeclaringClass().getName();
    elementName = f.getName();
    type = FIELD;
    label = f.getName() + " : " + f.getType().getSimpleName();
    completionString = elementName;
  }

  public CompletionCandidate(String name, String className) {
    definingClass = className;
    elementName = name;
    label = name;
    completionString = name;
  }

  public CompletionCandidate(String name) {
    definingClass = "";
    elementName = name;
    label = name;
    completionString = name;
  }

  public String getDefiningClass() {
    return definingClass;
  }

  public String getElementName() {
    return elementName;
  }

  public String getCompletionString() {
    return completionString;
  }

  public String toString() {
    return label;
  }

  public int getType() {
    return type;
  }

  public int compareTo(CompletionCandidate cc) {
    if(type != cc.getType()){
      return cc.getType() - type;
    }

    return (elementName.compareTo(cc.getElementName()));
  }

}
