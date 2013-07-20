package processing.mode.experimental;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.TreeMap;

import org.eclipse.jdt.internal.compiler.problem.DefaultProblem;

public class ErrorMessageSimplifier {

//  private ErrorCheckerService errorCheckerService;

  /**
   * Mapping between ProblemID constant and the constant name. Holds about 650
   * of them. Also, this is just temporary, will be used to find the common
   * error types, cos you know, identifying String names is easier than
   * identifying 8 digit int constants!
   * TODO: this is temporary
   */
  private TreeMap<Integer, String> constantsMap;

  public ErrorMessageSimplifier() {

    new Thread() {
      public void run() {
        prepareConstantsList();
      }
    }.start();
  }

  private void prepareConstantsList() {
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
          System.out.println("Here");
          e.printStackTrace();
          break;
        }
    }
    System.out.println("Total items: " + constantsMap.size());
  }

  public String getIDName(int id) {
    if (constantsMap == null)
      return null;
    return constantsMap.get(id);
  }

}
