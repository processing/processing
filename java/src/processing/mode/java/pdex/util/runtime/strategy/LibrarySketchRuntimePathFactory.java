package processing.mode.java.pdex.util.runtime.strategy;

import processing.app.Library;
import processing.app.Sketch;
import processing.app.SketchException;
import processing.mode.java.JavaMode;
import processing.mode.java.pdex.ImportStatement;
import processing.mode.java.pdex.util.runtime.RuntimePathUtil;

import java.io.File;
import java.util.List;
import java.util.StringJoiner;


public class LibrarySketchRuntimePathFactory implements RuntimePathFactoryStrategy {

  @Override
  public List<String> buildClasspath(JavaMode mode, List<ImportStatement> imports, Sketch sketch) {
    StringJoiner classPathBuilder = new StringJoiner(File.pathSeparator);

    imports.stream()
        .map(ImportStatement::getPackageName)
        .filter(pckg -> !ignorableImport(pckg))
        .map(pckg -> {
          try {
            return mode.getLibrary(pckg);
          } catch (SketchException e) {
            return null;
          }
        })
        .filter(lib -> lib != null)
        .map(Library::getClassPath)
        .forEach(cp -> classPathBuilder.add(cp));

    return RuntimePathUtil.sanitizeClassPath(classPathBuilder.toString());
  }

  /**
   * Ignore processing packages, java.*.*. etc.
   */
  private boolean ignorableImport(String packageName) {
    return (packageName.startsWith("java.") ||
        packageName.startsWith("javax."));
  }

}
