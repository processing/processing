package processing.mode.java.pdex.util.runtime.strategy;

import processing.app.Library;
import processing.app.Sketch;
import processing.mode.java.JavaMode;
import processing.mode.java.pdex.ImportStatement;
import processing.mode.java.pdex.util.runtime.RuntimePathUtil;

import java.io.File;
import java.util.List;


public class CoreLibraryRuntimePathFactory implements RuntimePathFactoryStrategy {

  @Override
  public List<String> buildClasspath(JavaMode mode, List<ImportStatement> imports, Sketch sketch) {
    StringBuilder classPath = new StringBuilder();

    for (Library lib : mode.coreLibraries) {
      classPath.append(File.pathSeparator).append(lib.getClassPath());
    }

    return RuntimePathUtil.sanitizeClassPath(classPath.toString());
  }

}
