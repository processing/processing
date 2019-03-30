package processing.mode.java.preproc.util.strategy;

/**
 * Convenience functions useful for generating simplified messages.
 */
public class MessageSimplifierUtil {

  /**
   * Get the snippet of "offending code" from an error message if given.
   *
   * @param area The area from which to extract the offending code.
   * @return The offending code described in the error message or the original message if the subset
   *    describing the offending code could not be found.
   */
  public static String getOffendingArea(String area) {
    return getOffendingArea(area, true);
  }

  /**
   * Get the snippet of "offending code" from an error message if given.
   *
   * @param area The area from which to extract the offending code.
   * @param removeNewline Flag indicating if newlines should be removed or not.
   * @return The offending code described in the error message or the original message if the subset
   *    describing the offending code could not be found.
   */
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
