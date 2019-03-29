package processing.mode.java.preproc.util.strategy;

public class MessageSimplifierUtil {

  public static String getOffendingArea(String area) {
    if (!area.contains("viable alternative")) {
      return area;
    }

    String content = area.replace("no viable alternative at input \'", "")
        .replace("\n", "");

    if (content.endsWith("'")) {
      return content.substring(0, content.length() - 1);
    } else {
      return content;
    }
  }

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
