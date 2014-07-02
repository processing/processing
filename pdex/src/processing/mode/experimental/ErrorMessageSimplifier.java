package processing.mode.experimental;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
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
        result = "Problem with code syntax: Consider removing \"" + args[0]
            + "\"";
        break;
      }
    case IProblem.ParsingErrorInsertToComplete:
      if (args.length > 0) {

        switch (args[0].charAt(0)) {
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
          result = "I sense a missing opening square bracket \"(\"";
          break;
        case ')':
          result = "Looks like you forgot to close your parentheses \")\"";
          break;
        case '{':
          result = "I sense a missing opening curly brace \";\"";
          break;
        case '}':
          result = "Looks like you forgot to close your curly brace \";\"";
          break;
        default:
          result = "Consider adding a \"" + args[0] + "\"";
        }
        break;
      }
    case IProblem.ParsingErrorInsertTokenAfter:
      if (args.length > 0) {
        switch (args[1].charAt(0)) {
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
          result = "I sense a missing opening square bracket \"(\"";
          break;
        case ')':
          result = "Looks like you forgot to close your parentheses \")\"";
          break;
        case '{':
          result = "I sense a missing opening curly brace \";\"";
          break;
        case '}':
          result = "Looks like you forgot to close your curly brace \";\"";
          break;
        default:
          result = "Consider adding a \"" + args[1] + "\"";
        }
        break;
      }
    case IProblem.UndefinedMethod:
      if (args.length > 2) {
        result = "I don't know the function \"" + args[args.length-2] + "\"";
      }
      break;
    }
    log("Simplified Error Msg: " + result);
    if (result == null)
      return problem.getMessage();
    return result;
  }

}
