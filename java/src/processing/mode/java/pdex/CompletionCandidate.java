/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
Part of the Processing project - http://processing.org
Copyright (c) 2012-15 The Processing Foundation

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License version 2
as published by the Free Software Foundation.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software Foundation, Inc.
51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
*/

package processing.mode.java.pdex;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;


public class CompletionCandidate implements Comparable<CompletionCandidate> {
  private final String elementName;
  private final String label; // the toString value
  private final String completionString;
  private final Object wrappedObject;
  private final int type;

  static final int PREDEF_CLASS = 0;
  static final int PREDEF_FIELD = 1;
  static final int PREDEF_METHOD = 2;
  static final int LOCAL_CLASS = 3;
  static final int LOCAL_METHOD = 4;
  static final int LOCAL_FIELD = 5;
  static final int LOCAL_VAR = 6;


  public CompletionCandidate(Method method) {
    method.getDeclaringClass().getName();
    elementName = method.getName();
    StringBuilder label = new StringBuilder("<html>"+method.getName() + "(");
    StringBuilder cstr = new StringBuilder(method.getName() + "(");
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
    label.append(" - <font color=#777777>" + method.getDeclaringClass().getSimpleName() + "</font></html>");
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
    wrappedObject = svd;
  }

  public CompletionCandidate(VariableDeclarationFragment  vdf) {
    completionString = vdf.getName().toString();
    elementName = vdf.getName().toString();
    if(vdf.getParent() instanceof FieldDeclaration)
      type = LOCAL_FIELD;
    else
      type = LOCAL_VAR;
    label = vdf.getName() + " : " + CompletionGenerator.extracTypeInfo2(vdf);
    wrappedObject = vdf;
  }

  public CompletionCandidate(MethodDeclaration method) {
    // log("ComCan " + method.getName());
    elementName = method.getName().toString();
    type = LOCAL_METHOD;

    @SuppressWarnings("unchecked")
    List<ASTNode> params = (List<ASTNode>)
      method.getStructuralProperty(MethodDeclaration.PARAMETERS_PROPERTY);

    StringBuilder label = new StringBuilder(elementName + "(");
    StringBuilder cstr = new StringBuilder(method.getName() + "(");
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
    wrappedObject = method;
  }

  public CompletionCandidate(TypeDeclaration td){
    type = LOCAL_CLASS;
    elementName = td.getName().toString();
    label = elementName;
    completionString = elementName;
    wrappedObject = td;
  }

  public CompletionCandidate(Field f) {
    f.getDeclaringClass().getName();
    elementName = f.getName();
    type = PREDEF_FIELD;
//    "<html>"
//    + matchedClass + " : " + "<font color=#777777>"
//    + matchedClass2.substring(0, d) + "</font>", matchedClass
//    + "</html>"
    label = "<html>" + f.getName() + " : " + f.getType().getSimpleName() +
        " - <font color=#777777>" + f.getDeclaringClass().getSimpleName() +
        "</font></html>";
    completionString = elementName;
    wrappedObject = f;
  }

  public CompletionCandidate(String name, String labelStr, String completionStr, int type) {
    elementName = name;
    label = labelStr;
    completionString = completionStr;
    this.type = type;
    wrappedObject = null;
  }

  public CompletionCandidate(String name, int type) {
    elementName = name;
    label = name;
    completionString = name;
    this.type = type;
    wrappedObject = null;
  }

  private CompletionCandidate(String elementName, String label,
                              String completionString, int type,
                              Object wrappedObject) {
    this.elementName = elementName;
    this.label = label;
    this.completionString = completionString;
    this.type = type;
    this.wrappedObject = wrappedObject;
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

  public String getLabel() {
    return label;
  }

  public String getNoHtmlLabel(){
    if(!label.contains("<html>")) {
      return label;
    }
    else {
      StringBuilder ans = new StringBuilder(label);
      while(ans.indexOf("<") > -1) {
        int a = ans.indexOf("<"), b = ans.indexOf(">");
        if(a > b) break;
        ans.replace(a, b+1, "");
//        System.out.println(ans.replace(a, b+1, ""));
//        System.out.println(ans + "--");
      }
      return ans.toString();
    }
  }

  public CompletionCandidate withLabelAndCompString(String label,
                                                    String completionString) {
    return new CompletionCandidate(this.elementName, label, completionString,
                                   this.type, this.wrappedObject);
  }

  @Override
  public int compareTo(CompletionCandidate cc) {
    if(type != cc.getType()){
      return cc.getType() - type;
    }
    return (elementName.compareTo(cc.getElementName()));
  }

  public CompletionCandidate withRegeneratedCompString() {
    if (wrappedObject instanceof MethodDeclaration) {
      MethodDeclaration method = (MethodDeclaration)wrappedObject;

      @SuppressWarnings("unchecked")
      List<ASTNode> params = (List<ASTNode>)
          method.getStructuralProperty(MethodDeclaration.PARAMETERS_PROPERTY);

      StringBuilder label = new StringBuilder(elementName + "(");
      StringBuilder cstr = new StringBuilder(method.getName() + "(");
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
      return this.withLabelAndCompString(label.toString(), cstr.toString());
    }
   else if (wrappedObject instanceof Method) {
     Method method = (Method)wrappedObject;
     StringBuilder label = new StringBuilder("<html>" + method.getName() + "(");
     StringBuilder cstr = new StringBuilder(method.getName() + "(");
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
     label.append(" - <font color=#777777>" + method.getDeclaringClass().getSimpleName() + "</font></html>");
     cstr.append(")");
     return this.withLabelAndCompString(label.toString(), cstr.toString());
     /*
      * StringBuilder label = new StringBuilder("<html>"+method.getName() + "(");
    StringBuilder cstr = new StringBuilder(method.getName() + "(");
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
    label.append(" - <font color=#777777>" + method.getDeclaringClass().getSimpleName() + "</font></html>");
      * */
   }
    return this;
  }

}
