package processing.mode.java.pdex.util.runtime.strategy;

import processing.app.Sketch;
import processing.mode.java.JavaMode;
import processing.mode.java.pdex.ImportStatement;
import processing.mode.java.pdex.util.runtime.RuntimePathUtil;

import java.util.ArrayList;
import java.util.List;


public class ModeSearchRuntimePathFactory implements RuntimePathFactoryStrategy {

  @Override
  public List<String> buildClasspath(JavaMode mode, List<ImportStatement> imports, Sketch sketch) {
    String searchClassPath = mode.getSearchPath();

    if (searchClassPath != null) {
      return RuntimePathUtil.sanitizeClassPath(searchClassPath);
    } else {
      return new ArrayList<>();
    }
  }

}
