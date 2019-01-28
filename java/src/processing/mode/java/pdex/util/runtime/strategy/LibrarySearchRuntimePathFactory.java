package processing.mode.java.pdex.util.runtime.strategy;

import processing.app.Library;
import processing.app.Sketch;
import processing.mode.java.JavaMode;
import processing.mode.java.pdex.ImportStatement;
import processing.mode.java.pdex.util.runtime.RuntimePathUtil;

import java.io.File;
import java.util.List;
import java.util.StringJoiner;


public class LibrarySearchRuntimePathFactory implements RuntimePathFactoryStrategy {

  @Override
  public List<String> buildClasspath(JavaMode mode, List<ImportStatement> imports, Sketch sketch) {
    StringJoiner classPathBuilder = new StringJoiner(File.pathSeparator);

    for (Library lib : mode.contribLibraries) {
      classPathBuilder.add(lib.getClassPath());
    }

    return RuntimePathUtil.sanitizeClassPath(classPathBuilder.toString());
  }

}
