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
  private final String completion;
  private final Object wrappedObject;
  private final int type;

  static final int PREDEF_CLASS = 0;
  static final int PREDEF_FIELD = 1;
  static final int PREDEF_METHOD = 2;
  static final int LOCAL_CLASS = 3;
  static final int LOCAL_METHOD = 4;
  static final int LOCAL_FIELD = 5;
  static final int LOCAL_VAR = 6;


  CompletionCandidate(Method method) {
    // return value ignored? [fry 180326]
    method.getDeclaringClass().getName();
    elementName = method.getName();

    StringBuilder labelBuilder = new StringBuilder("<html>"+method.getName() + "(");
    StringBuilder compBuilder = new StringBuilder(method.getName() + "(");
    Class<?>[] types = method.getParameterTypes();
    for (int i = 0; i < types.length; i++) {
      labelBuilder.append(types[i].getSimpleName());
      if (i < types.length - 1) {
        labelBuilder.append(',');
        compBuilder.append(',');
      }
    }
    if (types.length == 1) {
      compBuilder.append(' ');
    }
    labelBuilder.append(")");

    if (method.getReturnType() != null) {
      labelBuilder.append(" : " + method.getReturnType().getSimpleName());
    }
    labelBuilder.append(" - <font color=#777777>" + method.getDeclaringClass().getSimpleName() + "</font></html>");

    compBuilder.append(")");
    label = labelBuilder.toString();
    completion = compBuilder.toString();

    type = PREDEF_METHOD;
    wrappedObject = method;
  }


  CompletionCandidate(SingleVariableDeclaration svd) {
    completion = svd.getName().toString();
    elementName = svd.getName().toString();
    type = (svd.getParent() instanceof FieldDeclaration) ?
      LOCAL_FIELD : LOCAL_VAR;
    label = svd.getName() + " : " + svd.getType();
    wrappedObject = svd;
  }


  CompletionCandidate(VariableDeclarationFragment  vdf) {
    completion = vdf.getName().toString();
    elementName = vdf.getName().toString();
    type = (vdf.getParent() instanceof FieldDeclaration) ?
      LOCAL_FIELD : LOCAL_VAR;
    label = vdf.getName() + " : " + CompletionGenerator.extracTypeInfo2(vdf);
    wrappedObject = vdf;
  }


  CompletionCandidate(MethodDeclaration method) {
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
    this.completion = cstr.toString();
    wrappedObject = method;
  }


  CompletionCandidate(TypeDeclaration td) {
    type = LOCAL_CLASS;
    elementName = td.getName().toString();
    label = elementName;
    completion = elementName;
    wrappedObject = td;
  }


  CompletionCandidate(Field f) {
    f.getDeclaringClass().getName();
    elementName = f.getName();
    type = PREDEF_FIELD;
    label = "<html>" + f.getName() + " : " + f.getType().getSimpleName() +
        " - <font color=#777777>" + f.getDeclaringClass().getSimpleName() +
        "</font></html>";
    completion = elementName;
    wrappedObject = f;
  }


  public CompletionCandidate(String name, String label, String completion, int type) {
    this(name, label, completion, type, null);
  }


  public CompletionCandidate(String name, int type) {
    this(name, name, name, type, null);
  }


  private CompletionCandidate(String elementName, String label,
                              String completion, int type,
                              Object wrappedObject) {
    this.elementName = elementName;
    this.label = label;
    this.completion = completion;
    this.type = type;
    this.wrappedObject = wrappedObject;
  }


  Object getWrappedObject() {
    return wrappedObject;
  }


  public String getElementName() {
    return elementName;
  }


  public String getCompletionString() {
    return completion;
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
    if (!label.contains("<html>")) {
      return label;

    } else {
      // TODO this is gross [fry 180326]
      StringBuilder ans = new StringBuilder(label);
      while (ans.indexOf("<") > -1) {
        int a = ans.indexOf("<"), b = ans.indexOf(">");
        if (a > b) break;
        ans.replace(a, b+1, "");
      }
      return ans.toString();
    }
  }


  public CompletionCandidate withLabelAndCompString(String label,
                                                    String completion) {
    return new CompletionCandidate(this.elementName, label, completion,
                                   this.type, this.wrappedObject);
  }


  @Override
  public int compareTo(CompletionCandidate cc) {
    if (type != cc.getType()) {
      return cc.getType() - type;
    }
    return elementName.compareTo(cc.getElementName());
  }


  public CompletionCandidate withRegeneratedCompString() {
    if (wrappedObject instanceof MethodDeclaration) {
      MethodDeclaration method = (MethodDeclaration)wrappedObject;

      @SuppressWarnings("unchecked")
      List<ASTNode> params = (List<ASTNode>)
        method.getStructuralProperty(MethodDeclaration.PARAMETERS_PROPERTY);

      StringBuilder labelBuilder = new StringBuilder(elementName + "(");
      StringBuilder compBuilder = new StringBuilder(method.getName() + "(");
      for (int i = 0; i < params.size(); i++) {
        labelBuilder.append(params.get(i));
        if (i < params.size() - 1) {
          labelBuilder.append(',');
          compBuilder.append(',');
        }
      }
      if (params.size() == 1) {
        compBuilder.append(' ');
      }
      labelBuilder.append(')');
      if (method.getReturnType2() != null) {
        labelBuilder.append(" : ");
        labelBuilder.append(method.getReturnType2());
      }
      compBuilder.append(')');
      return withLabelAndCompString(labelBuilder.toString(), compBuilder.toString());

    } else if (wrappedObject instanceof Method) {
      Method method = (Method) wrappedObject;
      StringBuilder labelBuilder = new StringBuilder("<html>" + method.getName() + "(");
      StringBuilder compBuilder = new StringBuilder(method.getName() + "(");
      for (int i = 0; i < method.getParameterTypes().length; i++) {
        labelBuilder.append(method.getParameterTypes()[i].getSimpleName());
        if (i < method.getParameterTypes().length - 1) {
          labelBuilder.append(',');
          compBuilder.append(',');
        }
      }
      if (method.getParameterTypes().length == 1) {
        compBuilder.append(' ');
      }
      compBuilder.append(')');

      labelBuilder.append(')');
      if (method.getReturnType() != null) {
        labelBuilder.append(" : " + method.getReturnType().getSimpleName());
      }
      labelBuilder.append(" - <font color=#777777>");
      labelBuilder.append(method.getDeclaringClass().getSimpleName());
      labelBuilder.append("</font></html>");

      return withLabelAndCompString(labelBuilder.toString(), compBuilder.toString());
    }

    // fall-through silently does nothing? [fry 180326]
    return this;
  }
}
