package processing.mode.experimental;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.TreeMap;

import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.internal.compiler.problem.DefaultProblem;


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
   * 
   * @param problem
   * @return
   */
  public static String getSimplifiedErrorMessage(Problem problem) {
    if (problem == null)
      return null;
    IProblem iprob = problem.getIProblem();
    String args[] = iprob.getArguments();
//    log("Simplifying message: " + problem.getMessage() + " ID: "
//        + getIDName(iprob.getID()));
//    log("Arg count: " + args.length);
//    for (int i = 0; i < args.length; i++) {
//      log("Arg " + args[i]);
//    }

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
          if(args[0].equals("AssignmentOperator Expression")){
            result = "Consider adding a \"=\"";
          }
          else {
            result = "Consider adding a \"" + args[0] + "\"";
          }
        }
      }
      break;
    case IProblem.ParsingErrorInvalidToken:
      if (args.length > 0) {
        if (args[1].equals("VariableDeclaratorId")) {
          if(args[0].equals("int")) {
            result = "\"color\" and \"int\" are reserved words & can't be used as variable names";
          }
          else {
            result = "\"" + args[0]
                + "\" is a reserved word, it can't be used as a variable name";
          }
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
        result = "The method \"" + args[args.length - 2] + "("
            + getSimpleName(args[args.length - 1]) + ")\" doesn't exist";
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
          result = "The method \"" + args[1]
              + "\" expects parameters like this: " + args[1] + "("
              + getSimpleName(args[2]) + ")";
        }
      }
      break;
    case IProblem.UndefinedField:
      if (args.length > 0) {
        result = "The global variable \"" + args[0] + "\" doesn't exist";
      }
      break;
    case IProblem.UndefinedType:
      if (args.length > 0) {
        result = "The class \"" + args[0] + "\" doesn't exist";
      }
      break;
    case IProblem.UnresolvedVariable:
      if (args.length > 0) {
        result = "The variable \"" + args[0] + "\" doesn't exist";
      }
      break;
    case IProblem.UndefinedName:
      if (args.length > 0) {
        result = "The name \"" + args[0] + "\" can't be recognized";
      }
      break;
    case IProblem.TypeMismatch:
      if (args.length > 1) {
        result = "Type mismatch, \"" + getSimpleName(args[0])
            + "\" doesn't match with \"" + getSimpleName(args[1]) + "\"";
      }
      break;
    }
    
//    log("Simplified Error Msg: " + result);
    if (result == null)
      result = problem.getMessage();
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
      result = "Missing a semi-colon \";\"";
      break;
    case '[':
      result = "Missing opening square bracket \"[\"";
      break;
    case ']':
      result = "Missing closing square bracket \"]\"";
      break;
    case '(':
      result = "Missing opening parentheses \"(\"";
      break;
    case ')':
      result = "Missing closing parentheses \")\"";
      break;
    case '{':
      result = "Missing opening curly bracket \"{\"";
      break;
    case '}':
      result = "Missing closing curly bracket \"}\"";
      break;
    default:
      result = "Consider adding a \"" + c + "\"";
    }

    return result;
  }
  

}
