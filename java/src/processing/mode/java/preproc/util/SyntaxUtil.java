package processing.mode.java.preproc.util;

public class SyntaxUtil {

  public static int getCount(String body, String search) {
    if (search.length() == 1) {
      return getCountChar(body, search.charAt(0));
    } else {
      return getCountString(body, search);
    }
  }

  private static int getCountString(String body, String search) {
    int count = 0;

    for(int i = 0; i < body.length(); i++)
    {
      count += body.substring(i).startsWith(search) ? 1 : 0;
    }

    return count;
  }

  private static int getCountChar(String body, char search) {
    int count = 0;

    for(int i = 0; i < body.length(); i++)
    {
      count += body.charAt(i) == search ? 1 : 0;
    }

    return count;
  }

}
