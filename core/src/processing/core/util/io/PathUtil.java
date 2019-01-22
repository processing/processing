package processing.core.util.io;

import java.io.File;

public class PathUtil {
  private static final String UNKNOWN_FILE_EXTENSION = "unknown";

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

  static public void createPath(String path) {
    createPath(new File(path));
  }

  public static void createPath(File file) {
    try {
      String parent = file.getParent();
      if (parent != null) {
        File unit = new File(parent);
        if (!unit.exists()) unit.mkdirs();
      }
    } catch (SecurityException se) {
      System.err.println("You don't have permissions to create " +
          file.getAbsolutePath());
    }
  }
}
