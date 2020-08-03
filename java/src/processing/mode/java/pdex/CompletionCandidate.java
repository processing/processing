/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
Part of the Processing project - http://processing.org
Copyright (c) 2012-18 The Processing Foundation

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


// TODO when building the label in some variants in this file,
// getReturnType2() is used instead of getReturnType().
// need to check whether that's identical in how it performs,
// and if so, use makeLabel() and makeCompletion() more [fry 180326]
// https://help.eclipse.org/neon/index.jsp?topic=%2Forg.eclipse.jdt.doc.isv%2Freference%2Fapi%2Forg%2Feclipse%2Fjdt%2Fcore%2Fdom%2FMethodDeclaration.html

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
    label = makeLabel(method);
    completion = makeCompletion(method);
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

    { // label
      StringBuilder labelBuilder = new StringBuilder(elementName);
      labelBuilder.append('(');
      for (int i = 0; i < params.size(); i++) {
        labelBuilder.append(params.get(i).toString());
        if (i < params.size() - 1) {
          labelBuilder.append(',');
        }
      }
      labelBuilder.append(')');
      if (method.getReturnType2() != null) {
        labelBuilder.append(" : ");
        labelBuilder.append(method.getReturnType2());
      }
      label = labelBuilder.toString();
    }

    { // completion
      StringBuilder compBuilder = new StringBuilder(elementName);
      compBuilder.append('(');

      for (int i = 0; i < params.size(); i++) {
        if (i < params.size() - 1) {
          compBuilder.append(',');
        }
      }
      if (params.size() == 1) {
        compBuilder.append(' ');
      }
      compBuilder.append(')');
      completion = compBuilder.toString();
    }

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
    label = "<html>" +
      f.getName() + " : " +
      f.getType().getSimpleName() + " - " +
      "<font color=#777777>" +
      f.getDeclaringClass().getSimpleName() +
      "</font></html>";
    completion = elementName;
    wrappedObject = f;
  }


  CompletionCandidate(String elementName, String label,
                      String completion, int type) {
    this(elementName, label, completion, type, null);
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


  public int getType() {
    return type;
  }


  public String getLabel() {
    return label;
  }


  // TODO this is gross [fry 180326]
  /*
  private String getNoHtmlLabel(){
    if (!label.contains("<html>")) {
      return label;

    } else {
      StringBuilder ans = new StringBuilder(label);
      while (ans.indexOf("<") > -1) {
        int a = ans.indexOf("<"), b = ans.indexOf(">");
        if (a > b) break;
        ans.replace(a, b+1, "");
      }
      return ans.toString();
    }
  }
  */


  boolean startsWith(String newWord) {
//    System.out.println("checking " + newWord);
//    return getNoHtmlLabel().toLowerCase().startsWith(newWord);
    // this seems to be elementName in all cases [fry 180326]
    return elementName.startsWith(newWord);
  }


  CompletionCandidate withLabelAndCompString(String withLabel,
                                             String withCompletion) {
    return new CompletionCandidate(elementName,
                                   withLabel, withCompletion,
                                   type, wrappedObject);
  }


  CompletionCandidate withRegeneratedCompString() {
    if (wrappedObject instanceof MethodDeclaration) {
      MethodDeclaration method = (MethodDeclaration)wrappedObject;

      @SuppressWarnings("unchecked")
      List<ASTNode> params = (List<ASTNode>)
        method.getStructuralProperty(MethodDeclaration.PARAMETERS_PROPERTY);

      // build the html label
      StringBuilder labelBuilder = new StringBuilder(elementName);
      labelBuilder.append('(');
      for (int i = 0; i < params.size(); i++) {
        labelBuilder.append(params.get(i));
        if (i < params.size() - 1) {
          labelBuilder.append(',');
        }
      }
      labelBuilder.append(')');
      if (method.getReturnType2() != null) {
        labelBuilder.append(" : ");
        labelBuilder.append(method.getReturnType2());
      }

      // build the completion str
      StringBuilder compBuilder = new StringBuilder();
      compBuilder.append(method.getName());
      compBuilder.append('(');
      for (int i = 0; i < params.size(); i++) {
        if (i < params.size() - 1) {
          compBuilder.append(',');
        }
      }
      if (params.size() == 1) {
        compBuilder.append(' ');
      }
      compBuilder.append(')');

      return withLabelAndCompString(labelBuilder.toString(), compBuilder.toString());

    } else if (wrappedObject instanceof Method) {
      Method method = (Method) wrappedObject;
      Class<?>[] types = method.getParameterTypes();

      // build html label
      StringBuilder labelBuilder = new StringBuilder();
      labelBuilder.append("<html>");
      labelBuilder.append(method.getName());
      labelBuilder.append('(');

      for (int i = 0; i < types.length; i++) {
        labelBuilder.append(types[i].getSimpleName());
        if (i < types.length - 1) {
          labelBuilder.append(',');
        }
      }
      labelBuilder.append(')');
      if (method.getReturnType() != null) {
        labelBuilder.append(" : " + method.getReturnType().getSimpleName());
      }

      labelBuilder.append(" - <font color=#777777>");
      labelBuilder.append(method.getDeclaringClass().getSimpleName());
      labelBuilder.append("</font>");
      labelBuilder.append("</html>");

      // make completion string
      StringBuilder compBuilder = new StringBuilder(method.getName());
      compBuilder.append('(');
      for (int i = 0; i < types.length; i++) {
        if (i < types.length - 1) {
          compBuilder.append(',');
        }
      }
      if (types.length == 1) {
        compBuilder.append(' ');
      }
      compBuilder.append(')');

      return withLabelAndCompString(labelBuilder.toString(), compBuilder.toString());
    }

    // fall-through silently does nothing? [fry 180326]
    return this;
  }


  static private String makeLabel(Method method) {
    Class<?>[] types = method.getParameterTypes();

    StringBuilder labelBuilder = new StringBuilder();
    labelBuilder.append("<html>");
    labelBuilder.append(method.getName());
    labelBuilder.append('(');

    for (int i = 0; i < types.length; i++) {
      labelBuilder.append(types[i].getSimpleName());
      if (i < types.length - 1) {
        labelBuilder.append(',');
      }
    }
    labelBuilder.append(")");
    if (method.getReturnType() != null) {
      labelBuilder.append(" : ");
      labelBuilder.append(method.getReturnType().getSimpleName());
    }
    labelBuilder.append(" - <font color=#777777>");
    labelBuilder.append(method.getDeclaringClass().getSimpleName());
    labelBuilder.append("</font>");
    labelBuilder.append("</html>");

    return labelBuilder.toString();
  }


  static private String makeCompletion(Method method) {
    Class<?>[] types = method.getParameterTypes();

    StringBuilder compBuilder = new StringBuilder();
    compBuilder.append(method.getName());
    compBuilder.append('(');

    for (int i = 0; i < types.length; i++) {
      if (i < types.length - 1) {
        compBuilder.append(',');  // wtf? [fry 180326]
      }
    }
    if (types.length == 1) {
      compBuilder.append(' ');
    }
    compBuilder.append(')');
    return compBuilder.toString();
  }


  @Override
  public int compareTo(CompletionCandidate cc) {
    if (type != cc.getType()) {
      return cc.getType() - type;
    }
    return elementName.compareTo(cc.getElementName());
  }


  public String toString() {
    return label;
  }
}
