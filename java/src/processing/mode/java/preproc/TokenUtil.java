package processing.mode.java.preproc;

import java.lang.reflect.Field;
import antlr.collections.AST;
import processing.mode.java.preproc.PdeTokenTypes;

/**
 * 
 * @author Jonathan Feinberg &lt;jdf@pobox.com&gt;
 *
 */
public class TokenUtil {
  private static final String[] tokenNames= new String[200];
  static {
    for (int i = 0; i < tokenNames.length; i++) {
      tokenNames[i] = "ERROR:" + i;
    }
    for (final Field f : PdeTokenTypes.class.getDeclaredFields()) {
      try {
        tokenNames[f.getInt(null)] = f.getName();
      } catch (Exception unexpected) {
        throw new RuntimeException(unexpected);
      }
    }
  }

  public static String nameOf(final AST node) {
    return tokenNames[node.getType()];
  }
}
