package processing.mode.java.pdex.util.runtime.strategy;

import processing.app.Library;
import processing.app.Sketch;
import processing.mode.java.JavaMode;
import processing.mode.java.pdex.ImportStatement;
import processing.mode.java.pdex.util.runtime.RuntimePathUtil;

import java.util.ArrayList;
import java.util.List;


public class ModeSketchRuntimePathFactory implements RuntimePathFactoryStrategy {

  @Override
  public List<String> buildClasspath(JavaMode mode, List<ImportStatement> imports, Sketch sketch) {
    Library coreLibrary = mode.getCoreLibrary();
    String coreClassPath = coreLibrary != null ?
        coreLibrary.getClassPath() : mode.getSearchPath();
    if (coreClassPath != null) {
      return RuntimePathUtil.sanitizeClassPath(coreClassPath);
    } else {
      return new ArrayList<>();
    }
  }

}
