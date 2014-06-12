package processing.mode.experimental;
import static processing.mode.experimental.ExperimentalMode.log;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

public class CompletionCandidate implements Comparable<CompletionCandidate>{

  private String elementName; //

  private String label; // the toString value

  private String completionString;
  
  private Object wrappedObject;

  private int type;

  public static final int PREDEF_CLASS = 0, PREDEF_FIELD = 1,
      PREDEF_METHOD = 2, LOCAL_CLASS = 3, LOCAL_METHOD = 4, LOCAL_FIELD = 5,
      LOCAL_VAR = 6;

  public CompletionCandidate(Method method) {
    method.getDeclaringClass().getName();
    elementName = method.getName();
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
    if(method.getReturnType() != null)
      label.append(" : " + method.getReturnType().getSimpleName());
    label.append(" - " + method.getDeclaringClass().getSimpleName());
    cstr.append(")");
    this.label = label.toString();
    this.completionString = cstr.toString();
    type = PREDEF_METHOD;
    wrappedObject = method;
  }
  
  public Object getWrappedObject() {
    return wrappedObject;
  }

  public CompletionCandidate(SingleVariableDeclaration svd) {
    completionString = svd.getName().toString();
    elementName = svd.getName().toString();
    if(svd.getParent() instanceof FieldDeclaration)
      type = LOCAL_FIELD;
    else
      type = LOCAL_VAR;
    label = svd.getName() + " : " + svd.getType();    
  }
  
  public CompletionCandidate(VariableDeclarationFragment  vdf) {
    completionString = vdf.getName().toString();
    elementName = vdf.getName().toString();
    if(vdf.getParent() instanceof FieldDeclaration)
      type = LOCAL_FIELD;
    else
      type = LOCAL_VAR;
    label = vdf.getName() + " : " + ASTGenerator.extracTypeInfo2(vdf);
  }
  
  public CompletionCandidate(MethodDeclaration method) {
    // log("ComCan " + method.getName());
    elementName = method.getName().toString();
    type = LOCAL_METHOD;
    List<ASTNode> params = (List<ASTNode>) method
        .getStructuralProperty(MethodDeclaration.PARAMETERS_PROPERTY);
    StringBuffer label = new StringBuffer(elementName + "(");
    StringBuffer cstr = new StringBuffer(method.getName() + "(");
    for (int i = 0; i < params.size(); i++) {
      label.append(params.get(i).toString());
      if (i < params.size() - 1) {
        label.append(",");
        cstr.append(",");
      }
    }
    if (params.size() == 1) {
      cstr.append(' ');
    }
    label.append(")");
    if (method.getReturnType2() != null)
      label.append(" : " + method.getReturnType2());
    cstr.append(")");
    this.label = label.toString();
    this.completionString = cstr.toString();
  }

  public CompletionCandidate(TypeDeclaration td){
    type = LOCAL_CLASS;
    elementName = td.getName().toString();
    label = elementName;
    completionString = elementName;
  }

  public CompletionCandidate(Field f) {
    f.getDeclaringClass().getName();
    elementName = f.getName();
    type = PREDEF_FIELD;
    label = f.getName() + " : " + f.getType().getSimpleName()
        + " - " + f.getDeclaringClass().getSimpleName();
    completionString = elementName;
    wrappedObject = f;
  }

  public CompletionCandidate(String name, String labelStr, String completionStr, int type) {    
    elementName = name;
    label = labelStr;
    completionString = completionStr;
    this.type = type;    
  }

  public CompletionCandidate(String name, int type) {
    elementName = name;
    label = name;
    completionString = name;
    this.type = type;
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
