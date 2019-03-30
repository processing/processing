package processing.mode.java.preproc.util;

/**
 * Convenience functions useful for working on syntax checking for source.
 */
public class SyntaxUtil {

  /**
   * Determine how many times a string appears in another.
   *
   * @param body The string in which occurrences should be counted.
   * @param search The string to look for.
   * @return The number of times search appears in body.
   */
  public static int getCount(String body, String search) {
    if (search.length() == 1) {
      return getCountChar(body, search.charAt(0));
    } else {
      return getCountString(body, search);
    }
  }

  /**
   * Determine how many times a string appears in another.
   *
   * @param body The string in which occurrences should be counted.
   * @param search The string to look for.
   * @return The number of times search appears in body.
   */
  private static int getCountString(String body, String search) {
    int count = 0;

    for(int i = 0; i < body.length(); i++)
    {
      count += body.substring(i).startsWith(search) ? 1 : 0;
    }

    return count;
  }

  /**
   * Determine how many times a character appears in another.
   *
   * @param body The string in which occurrences should be counted.
   * @param search The character to look for.
   * @return The number of times search appears in body.
   */
  private static int getCountChar(String body, char search) {
    int count = 0;

    for(int i = 0; i < body.length(); i++)
    {
      count += body.charAt(i) == search ? 1 : 0;
    }

    return count;
  }

}
