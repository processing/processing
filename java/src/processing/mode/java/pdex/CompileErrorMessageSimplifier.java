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
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.internal.compiler.problem.DefaultProblem;

import processing.app.Language;
import processing.app.Messages;
import processing.core.PApplet;
import processing.data.StringList;


public class CompileErrorMessageSimplifier {
  /**
   * Mapping between ProblemID constant and the constant name. Holds about 650
   * of them. Also, this is just temporary, will be used to find the common
   * error types, cos you know, identifying String names is easier than
   * identifying 8 digit int constants!
   * TODO: this is temporary
   */
  private static TreeMap<Integer, String> constantsMap;

  private static final boolean DEBUG = false;

  private static final Pattern tokenRegExp = Pattern.compile("\\b token\\b");

  private static void prepareConstantsList() {
    constantsMap = new TreeMap<>();
    Class<DefaultProblem> probClass = DefaultProblem.class;
    Field f[] = probClass.getFields();
    for (Field field : f) {
      if (Modifier.isStatic(field.getModifiers()))
        try {
          if (DEBUG) {
            Messages.log(field.getName() + " :" + field.get(null));
          }
          Object val = field.get(null);
          if (val instanceof Integer) {
            constantsMap.put((Integer) (val), field.getName());
          }
        } catch (Exception e) {
          e.printStackTrace();
          break;
        }
    }
    if (DEBUG) {
      Messages.log("Total items: " + constantsMap.size());
    }
  }


  public static String getIDName(int id) {
    if (constantsMap == null) {
      prepareConstantsList();
    }
    return constantsMap.get(id);
  }


  /**
   * Tones down the jargon in the ecj reported errors.
   */
  public static String getSimplifiedErrorMessage(IProblem iprob, String badCode) {
    if (iprob == null) return null;

    String args[] = iprob.getArguments();

    if (DEBUG) {
      Messages.log("Simplifying message: " + iprob.getMessage() +
                       " ID: " + getIDName(iprob.getID()));
      Messages.log("Arg count: " + args.length);
      for (String arg : args) {
        Messages.log("Arg " + arg);
      }
      Messages.log("Bad code: " + badCode);
    }

    String result = null;

    switch (iprob.getID()) {

      case IProblem.ParsingError:
        if (args.length > 0) {
          result = Language.interpolate("editor.status.error_on", args[0]);
        }
        break;

      case IProblem.ParsingErrorDeleteToken:
        if (args.length > 0) {
          if (args[0].equalsIgnoreCase("Invalid Character")) {
            result = getErrorMessageForCurlyQuote(badCode);
          }
        }
        break;

      case IProblem.ParsingErrorDeleteTokens:
        result = getErrorMessageForCurlyQuote(badCode);
        if (result == null) {
          result = Language.interpolate("editor.status.error_on", args[0]);
        }
        break;

      case IProblem.ParsingErrorInsertToComplete:
        if (args.length > 0) {
          if (args[0].length() == 1) {
            result = getErrorMessageForBracket(args[0].charAt(0));

          } else {
            if (args[0].equals("AssignmentOperator Expression")) {
              result = Language.interpolate("editor.status.missing.add", "=");

            } else if (args[0].equalsIgnoreCase(") Statement")) {
              result = getErrorMessageForBracket(args[0].charAt(0));

            } else {
              result = Language.interpolate("editor.status.error_on", args[0]);
            }
          }
        }
        break;

      case IProblem.ParsingErrorInvalidToken:
        if (args.length > 0) {
          if (args[0].equals("int")) {
            if (args[1].equals("VariableDeclaratorId")) {
              result = Language.text("editor.status.reserved_words");
            } else {
              result = Language.interpolate("editor.status.error_on", args[0]);
            }
          } else if (args[0].equalsIgnoreCase("Invalid Character")) {
            result = getErrorMessageForCurlyQuote(badCode);
          }
          if (result == null) {
            result = Language.interpolate("editor.status.error_on", args[0]);
          }
        }
        break;

      case IProblem.ParsingErrorInsertTokenAfter:
        if (args.length > 0) {
          if (args[1].length() == 1) {
            result = getErrorMessageForBracket(args[1].charAt(0));
          } else {
            // https://github.com/processing/processing/issues/3104
            if (args[1].equalsIgnoreCase("Statement")) {
              result = Language.interpolate("editor.status.error_on", args[0]);
            } else {
              result =
                  Language.interpolate("editor.status.error_on", args[0]) + " " +
                      Language.interpolate("editor.status.missing.add", args[1]);
            }
          }
        }
        break;

      case IProblem.ParsingErrorReplaceTokens:
        result = getErrorMessageForCurlyQuote(badCode);

      case IProblem.UndefinedConstructor:
        if (args.length == 2) {
          String constructorName = args[0];
          // For messages such as "contructor sketch_name.ClassXYZ() is undefined", change
          // constructor name to "ClassXYZ()". See #3434
          if (constructorName.contains(".")) {
            // arg[0] contains sketch name twice: sketch_150705a.sketch_150705a.Thing
            constructorName = constructorName.substring(constructorName.indexOf('.') + 1);
            constructorName = constructorName.substring(constructorName.indexOf('.') + 1);
          }
          String constructorArgs = removePackagePrefixes(args[args.length - 1]);
          result = Language.interpolate("editor.status.undefined_constructor", constructorName, constructorArgs);
        }
        break;

      case IProblem.UndefinedMethod:
        if (args.length > 2) {
          String methodName = args[args.length - 2];
          String methodArgs = removePackagePrefixes(args[args.length - 1]);
          result = Language.interpolate("editor.status.undefined_method", methodName, methodArgs);
        }
        break;

      case IProblem.ParameterMismatch:
        if (args.length > 3) {
          // 2nd arg is method name, 3rd arg is correct param list
          if (args[2].trim().length() == 0) {
            // the case where no params are needed.
            result = Language.interpolate("editor.status.empty_param", args[1]);

          } else {
            result = Language.interpolate("editor.status.wrong_param",
                                          args[1], args[1], removePackagePrefixes(args[2]));
//          String method = q(args[1]);
//          String methodDef = " \"" + args[1] + "(" + getSimpleName(args[2]) + ")\"";
//          result = result.replace("method", method);
//          result += methodDef;
          }
        }
        break;

      case IProblem.UndefinedField:
        if (args.length > 0) {
          result = Language.interpolate("editor.status.undef_global_var", args[0]);
        }
        break;

      case IProblem.UndefinedType:
        if (args.length > 0) {
          result = Language.interpolate("editor.status.undef_class", args[0]);
        }
        break;

      case IProblem.UnresolvedVariable:
        if (args.length > 0) {
          result = Language.interpolate("editor.status.undef_var", args[0]);
        }
        break;

      case IProblem.UndefinedName:
        if (args.length > 0) {
          result = Language.interpolate("editor.status.undef_name", args[0]);
        }
        break;

      case IProblem.UnterminatedString:
        if (badCode.contains("“") || badCode.contains("”")) {
          result = Language.interpolate("editor.status.unterm_string_curly",
              badCode.replaceAll("[^“”]", ""));
        }
        break;

      case IProblem.TypeMismatch:
        if (args.length > 1) {
          result = Language.interpolate("editor.status.type_mismatch", args[0], args[1]);
//        result = result.replace("typeA", q(args[0]));
//        result = result.replace("typeB", q(args[1]));
        }
        break;

      case IProblem.LocalVariableIsNeverUsed:
        if (args.length > 0) {
          result = Language.interpolate("editor.status.unused_variable", args[0]);
        }
        break;

      case IProblem.UninitializedLocalVariable:
        if (args.length > 0) {
          result = Language.interpolate("editor.status.uninitialized_variable", args[0]);
        }
        break;

      case IProblem.AssignmentHasNoEffect:
        if (args.length > 0) {
          result = Language.interpolate("editor.status.no_effect_assignment", args[0]);
        }
        break;

      case IProblem.HidingEnclosingType:
        if (args.length > 0) {
          result = Language.interpolate("editor.status.hiding_enclosing_type", args[0]);
        }
        break;
    }

    if (result == null) {
      String message = iprob.getMessage();
      if (message != null) {
        // Remove all instances of token
        // "Syntax error on token 'blah', delete this token"
        Matcher matcher = tokenRegExp.matcher(message);
        message = matcher.replaceAll("");
        result = message;
      }
    }

    if (DEBUG) {
      Messages.log("Simplified Error Msg: " + result);
    }

    return result;
  }


  /**
   * Converts java.lang.String into String, etc
   */
  static private String removePackagePrefixes(String input) {
    if (!input.contains(".")) {
      return input;
    }
    String[] names = PApplet.split(input, ',');
//    List<String> names = new ArrayList<String>();
//    if (inp.indexOf(',') >= 0) {
//      names.addAll(Arrays.asList(inp.split(",")));
//    } else {
//      names.add(inp);
//    }
    StringList result = new StringList();
    for (String name : names) {
      int dot = name.lastIndexOf('.');
      if (dot >= 0) {
        name = name.substring(dot + 1, name.length());
      }
      result.append(name);
    }
    return result.join(", ");
  }


  static private String getErrorMessageForBracket(char c) {
    switch (c) {
      case ';': return Language.text("editor.status.missing.semicolon");
      case '[': return Language.text("editor.status.missing.left_sq_bracket");
      case ']': return Language.text("editor.status.missing.right_sq_bracket");
      case '(': return Language.text("editor.status.missing.left_paren");
      case ')': return Language.text("editor.status.missing.right_paren");
      case '{': return Language.text("editor.status.missing.left_curly_bracket");
      case '}': return Language.text("editor.status.missing.right_curly_bracket");
    }
    // This seems to be unreachable and wasn't in PDE.properties.
    // I've added it for 3.0a8, but that seems gross. [fry]
    return Language.interpolate("editor.status.missing.default", c);
  }


  /**
   * @param badCode The code which may contain curly quotes
   * @return Friendly error message if there is a curly quote in badCode,
   *         null otherwise.
   */
  static private String getErrorMessageForCurlyQuote(String badCode) {
    if (badCode.contains("‘") || badCode.contains("’") ||
        badCode.contains("“") || badCode.contains("”")) {
      return Language.interpolate("editor.status.bad_curly_quote",
          badCode.replaceAll("[^‘’“”]", ""));
    } else return null;
  }


//  static private final String q(Object quotable) {
//    return "\"" + quotable + "\"";
//  }


//  static private final String qs(Object quotable) {
//    return " " + q(quotable);
//  }
}
