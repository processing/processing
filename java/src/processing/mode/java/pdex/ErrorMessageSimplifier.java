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
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.TreeMap;

import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.internal.compiler.problem.DefaultProblem;

import processing.app.Language;


public class ErrorMessageSimplifier {
  /**
   * Mapping between ProblemID constant and the constant name. Holds about 650
   * of them. Also, this is just temporary, will be used to find the common
   * error types, cos you know, identifying String names is easier than
   * identifying 8 digit int constants!
   * TODO: this is temporary
   */
  private static TreeMap<Integer, String> constantsMap;


  public ErrorMessageSimplifier() {

    new Thread() {
      public void run() {
        prepareConstantsList();
      }
    }.start();
  }


  private static void prepareConstantsList() {
    constantsMap = new TreeMap<Integer, String>();
    Class<DefaultProblem> probClass = DefaultProblem.class;
    Field f[] = probClass.getFields();
    for (Field field : f) {
      if (Modifier.isStatic(field.getModifiers()))
        try {
          //System.out.println(field.getName() + " :" + field.get(null));
          Object val = field.get(null);
          if (val instanceof Integer) {
            constantsMap.put((Integer) (val), field.getName());
          }
        } catch (Exception e) {
          e.printStackTrace();
          break;
        }
    }
    //System.out.println("Total items: " + constantsMap.size());
  }


  public static String getIDName(int id) {
    if (constantsMap == null){
      prepareConstantsList();
    }
    return constantsMap.get(id);
  }


  /**
   * Tones down the jargon in the ecj reported errors.
   */
  public static String getSimplifiedErrorMessage(Problem problem) {
    if (problem == null) return null;

    IProblem iprob = problem.getIProblem();
    String args[] = iprob.getArguments();
//    Base.log("Simplifying message: " + problem.getMessage() + " ID: "
//        + getIDName(iprob.getID()));
//    Base.log("Arg count: " + args.length);
//    for (int i = 0; i < args.length; i++) {
//      Base.log("Arg " + args[i]);
//    }

    String result = null;

    switch (iprob.getID()) {

    case IProblem.ParsingError:
      if (args.length > 0) {
        result = Language.text("editor.status.error_on") + qs(args[0]);
      }
      break;

    case IProblem.ParsingErrorDeleteToken:
      if (args.length > 0) {
        result = Language.text("editor.status.error_on") + qs(args[0]);
      }
      break;

    case IProblem.ParsingErrorInsertToComplete:
      if (args.length > 0) {
        if (args[0].length() == 1) {
          result = getErrorMessageForBracket(args[0].charAt(0));

        } else {
          if (args[0].equals("AssignmentOperator Expression")) {
            result = Language.text("editor.status.missing.add") + qs("=");

          } else if (args[0].equalsIgnoreCase(") Statement")) {
            result = getErrorMessageForBracket(args[0].charAt(0));

          } else {
            result = Language.text("editor.status.error_on") + qs(args[0]);
          }
        }
      }
      break;

    case IProblem.ParsingErrorInvalidToken:
      if (args.length > 0) {
        if (args[1].equals("VariableDeclaratorId")) {
          if (args[0].equals("int")) {
            result = Language.text ("editor.status.reserved_words");
          } else {
            result = Language.text("editor.status.error_on") + qs(args[0]);
          }
        } else {
          result = Language.text("editor.status.error_on") + qs(args[0]);
        }
      }
      break;

    case IProblem.ParsingErrorInsertTokenAfter:
      if (args.length > 0) {
        if (args[1].length() == 1) {
          result = getErrorMessageForBracket(args[1].charAt(0));
        }
        else {
          if(args[1].equalsIgnoreCase("Statement")){ // See #3104
            result = Language.text("editor.status.error_on") + qs(args[0]);
          }
          else {
            result = Language.text("editor.status.error_on") +
              " \"" + args[0] + Language.text("editor.status.missing.add") + args[1] + "\"";
          }
        }
      }
      break;

    case IProblem.UndefinedMethod:
      if (args.length > 2) {
        result = Language.text("editor.status.undefined_method");
        String methodDef = "\"" + args[args.length - 2] + "("
          + getSimpleName (args[args.length - 1]) + ")\"";
        result = result.replace("methoddef", methodDef);
      }
      break;

    case IProblem.ParameterMismatch:
      if (args.length > 3) {
        // 2nd arg is method name, 3rd arg is correct param list
        if (args[2].trim().length() == 0) {
          // the case where no params are needed.
          result = Language.text("editor.status.empty_param");
          String methodDef = "\"" + args[1] + "()\"";
          result = result.replace("methoddef", methodDef);

        } else {
          result = Language.text("editor.status.wrong_param");

          String method = q(args[1]);
          String methodDef = " \"" + args[1] + "(" + getSimpleName(args[2]) + ")\"";
          result = result.replace("method", method);
          result += methodDef;
        }
      }
      break;

    case IProblem.UndefinedField:
      if (args.length > 0) {
        result = Language.text("editor.status.undef_global_var");
        result = result.replace("varname", q(args[0]));
      }
      break;

    case IProblem.UndefinedType:
      if (args.length > 0) {
        result = Language.text("editor.status.undef_class");
        result = result.replace("classname", q(args[0]));
      }
      break;

    case IProblem.UnresolvedVariable:
      if (args.length > 0) {
        result = Language.text("editor.status.undef_var");
        result = result.replace("varname", q(args[0]));
      }
      break;

    case IProblem.UndefinedName:
      if (args.length > 0) {
        result = Language.text("editor.status.undef_name");
        result = result.replace("namefield", q(args[0]));
      }
      break;

    case IProblem.TypeMismatch:
      if (args.length > 1) {
        result = Language.text("editor.status.type_mismatch");
        result = result.replace("typeA", q(args[0]));
        result = result.replace("typeB", q(args[1]));
      }
      break;

    case IProblem.LocalVariableIsNeverUsed:
      if (args.length > 0) {
        result = Language.text("editor.status.unused_variable");
        result = result.replace("varname", q(args[0]));
      }
      break;

    case IProblem.UninitializedLocalVariable:
      if (args.length > 0) {
        result = Language.text("editor.status.uninitialized_variable");
        result = result.replace("varname", q(args[0]));
      }
      break;

    case IProblem.AssignmentHasNoEffect:
      if (args.length > 0) {
        result = Language.text("editor.status.no_effect_assignment");
        result = result.replace("varname", q(args[0]));
      }
      break;
    }

    //log("Simplified Error Msg: " + result);
    return (result == null) ? problem.getMessage() : result;
  }


  /**
   * Converts java.lang.String into String, etc
   */
  static private String getSimpleName(String inp) {
    if (inp.indexOf('.') < 0) {
      return inp;
    }
    String res = "";
    ArrayList<String> names = new ArrayList<String>();
    if (inp.indexOf(',') >= 0) {
      String arr[] = inp.split(",");
      for (int i = 0; i < arr.length; i++) {
        names.add(arr[i]);
      }
    } else {
      names.add(inp);
    }
    for (String n : names) {
      int x = n.lastIndexOf('.');
      if (x >= 0) {
        n = n.substring(x + 1, n.length());
      }
      res = res + ", " + n;
    }
    return res.substring(2, res.length());
  }


  static private String getErrorMessageForBracket(char c) {
    switch (c) {
      case ';': return Language.text("editor.status.missing.semi_colon") + qs(";");
      case '[': return Language.text("editor.status.missing.open_sq_bracket") + qs("[");
      case ']': return Language.text("editor.status.missing.closing_sq_bracket") + qs("]");
      case '(': return Language.text("editor.status.missing.open_paren") + qs("(");
      case ')': return Language.text("editor.status.missing.close_paren") + qs(")");
      case '{': return Language.text("editor.status.missing.open_curly_bracket") + qs("{");
      case '}': return Language.text("editor.status.missing.closing_curly_bracket") + qs("}");
    }
    return Language.text("editor.status.missing.default") + qs(c);
  }


  static private final String q(Object quotable) {
    return "\"" + quotable + "\"";
  }


  static private final String qs(Object quotable) {
    return " " + q(quotable);
  }
}
