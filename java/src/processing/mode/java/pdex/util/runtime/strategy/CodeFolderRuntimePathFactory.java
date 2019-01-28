package processing.mode.java.pdex.util.runtime.strategy;

import processing.app.Sketch;
import processing.app.Util;
import processing.mode.java.JavaMode;
import processing.mode.java.pdex.ImportStatement;
import processing.mode.java.pdex.util.runtime.RuntimePathUtil;

import java.io.File;
import java.util.List;


public class CodeFolderRuntimePathFactory implements RuntimePathFactoryStrategy {
  @Override
  public List<String> buildClasspath(JavaMode mode, List<ImportStatement> imports, Sketch sketch) {
    StringBuilder classPath = new StringBuilder();

    // Code folder
    if (sketch.hasCodeFolder()) {
      File codeFolder = sketch.getCodeFolder();
      String codeFolderClassPath = Util.contentsToClassPath(codeFolder);
      classPath.append(codeFolderClassPath);
    }

    return RuntimePathUtil.sanitizeClassPath(classPath.toString());
  }
}
