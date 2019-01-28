package processing.mode.java.pdex.util.runtime;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class RuntimePathUtil {

  public static List<String> sanitizeClassPath(String classPathString) {
    // Make sure class path does not contain empty string (home dir)
    return Arrays.stream(classPathString.split(File.pathSeparator))
        .filter(p -> p != null && !p.trim().isEmpty())
        .distinct()
        .collect(Collectors.toList());
  }

}
