package processing.mode.java.preproc.util.strategy;

public class MessageSimplifierUtil {

  public static String getOffendingArea(String area) {
    return getOffendingArea(area, true);
  }

  public static String getOffendingArea(String area, boolean removeNewline) {
    if (!area.contains("viable alternative")) {
      return area;
    }

    String content = area.replace("no viable alternative at input \'", "");

    if (removeNewline) {
        content = content
            .replace("\n", "")
            .replace("\\n", "");
    }

    if (content.endsWith("'")) {
      return content.substring(0, content.length() - 1);
    } else {
      return content;
    }
  }

}
