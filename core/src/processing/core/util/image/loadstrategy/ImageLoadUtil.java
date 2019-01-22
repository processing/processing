package processing.core.util.image.loadstrategy;

public class ImageLoadUtil {

  private static String UNKNOWN_FILE_EXTENSION = "unknown";

  public static String parseExtension(String path) {
    String lower = path.toLowerCase();
    String extension;

    int dot = path.lastIndexOf('.');
    if (dot == -1) {
      extension = UNKNOWN_FILE_EXTENSION;  // no extension found

    } else {
      extension = lower.substring(dot + 1);

      // check for, and strip any parameters on the url, i.e.
      // filename.jpg?blah=blah&something=that
      int question = extension.indexOf('?');
      if (question != -1) {
        extension = extension.substring(0, question);
      }
    }

    return extension;
  }

  public static String cleanExtension(String extension) {
    return extension == null ? UNKNOWN_FILE_EXTENSION : extension.toLowerCase();
  }

  public static boolean checkExtensionRequiresAlpha(String extension) {
    return extension.equals("gif") ||
        extension.equals("png") ||
        extension.equals("unknown");
  }

}
