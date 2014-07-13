package processing.mode.experimental;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.TreeMap;

import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.internal.compiler.problem.DefaultProblem;
import static processing.mode.experimental.ExperimentalMode.log;
import static processing.mode.experimental.ExperimentalMode.logE;

public class ErrorMessageSimplifier {

//  private ErrorCheckerService errorCheckerService;

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
    Class probClass = DefaultProblem.class;
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
  
  public static String getSimplifiedErrorMessage(Problem problem) {
    if (problem == null)
      return null;
    IProblem iprob = problem.getIProblem();
    String args[] = iprob.getArguments();
    log("Simplifying message: " + problem.getMessage() + " ID: "
        + getIDName(iprob.getID()));
    log("Arg count: " + args.length);
    for (int i = 0; i < args.length; i++) {
      log("Arg " + args[i]);
    }

    String result = null;
    switch (iprob.getID()) {
    case IProblem.ParsingError:
      if (args.length > 0) {
        if (problem.getMessage().endsWith("expected")) {
          result = "Probably a \"" + args[args.length - 1]
              + "\" should go here";
        }
        else {
        result = "Problem with code syntax: Consider removing \"" + args[0]
            + "\"";
        }        
      }
      break;
    case IProblem.ParsingErrorInsertToComplete:
      if (args.length > 0) {
        if (args[0].length() == 1) {
          result = getErrorMessageForBracket(args[0].charAt(0));
        }
        else {
          result = "Consider adding a \"" + args[0] + "\"";
        }
      }
      break;
    case IProblem.ParsingErrorInsertTokenAfter:
      if (args.length > 0) {
        if (args[1].length() == 1) {
          result = getErrorMessageForBracket(args[1].charAt(0));
        }
        else {
          result = "Consider adding a \"" + args[1] + "\"";
        }
      }
      break;
    case IProblem.UndefinedMethod:
      if (args.length > 2) {
        result = "I don't know the function \"" + args[args.length - 2] + "\"";
      }
      break;
    case IProblem.ParameterMismatch:
      if (args.length > 3) {
        // 2nd arg is method name, 3rd arg is correct param list
        if (args[2].trim().length() == 0) {
          // the case where no params are needed.
          result = "The method \"" + args[1]
              + "\" doesn't expect any parameters";
        } else {
          result = "The method \"" + args[1] + "\" expects parameters ("
              + getSimpleName(args[2]) + ")";
        }
      }
      break;
    case IProblem.UndefinedField:
      if (args.length > 0) {
        result = "I don't know the global variable \"" + args[0] + "\"";
      }
      break;
    case IProblem.UndefinedType:
      if (args.length > 0) {
        result = "I don't know the class \"" + args[0] + "\"";
      }
      break;
    case IProblem.UnresolvedVariable:
      if (args.length > 0) {
        result = "I can't recognize the variable \"" + args[0] + "\"";
      }
      break;
    case IProblem.UndefinedName:
      if (args.length > 0) {
        result = "I don't recognize the name \"" + args[0] + "\"";
      }
      break;
    case IProblem.TypeMismatch:
      if (args.length > 1) {
        result = "You can't assign a \"" + getSimpleName(args[0])
            + "\" type to a \"" + getSimpleName(args[1]) + "\" type";
      }
      break;
    }
    
    log("Simplified Error Msg: " + result);
    if (result == null)
      return problem.getMessage();
    return result;
  }
  
  /**
   * Converts java.lang.String into String, etc
   * 
   * @param inp
   * @return
   */
  private static String getSimpleName(String inp) {
    if (inp.indexOf('.') < 0)
      return inp;
    String res = "";
    ArrayList<String> names = new ArrayList<String>();
    if (inp.indexOf(',') >= 0) {
      String arr[] = inp.split(",");
      for (int i = 0; i < arr.length; i++) {
        names.add(arr[i]);
      }
    } else
      names.add(inp);
    for (String n : names) {
      int x = n.lastIndexOf('.');
      if (x >= 0) {
        n = n.substring(x + 1, n.length());
      }
      res = res + ", " + n;
    }
    return res.substring(2, res.length());
  }
  
  private static String getErrorMessageForBracket(char c){
    String result = null;
    switch (c) {
    case ';':
      result = "You're missing a semi-colon \";\"";
      break;
    case '[':
      result = "I sense a missing opening square bracket \"[\"";
      break;
    case ']':
      result = "Looks like you forgot to close your square bracket \"]\"";
      break;
    case '(':
      result = "I sense a missing opening parentheses \"(\"";
      break;
    case ')':
      result = "Looks like you forgot to close your parentheses \")\"";
      break;
    case '{':
      result = "I sense a missing opening curly brace \"{\"";
      break;
    case '}':
      result = "Looks like you forgot to close your curly brace \"}\"";
      break;
    default:
      result = "Consider adding a \"" + c + "\"";
    }

    return result;
  }
  

}
