package processing.mode.java.pdex.util.runtime.strategy;

import processing.app.Sketch;
import processing.mode.java.JavaMode;
import processing.mode.java.pdex.ImportStatement;
import processing.mode.java.pdex.util.runtime.RuntimeConst;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;


public class JavaRuntimePathFactory implements RuntimePathFactoryStrategy {

  @Override
  public List<String> buildClasspath(JavaMode mode, List<ImportStatement> imports, Sketch sketch) {
    return Arrays.stream(RuntimeConst.STANDARD_MODULES)
        .map(this::buildForModule)
        .collect(Collectors.toList());
  }

  private String buildForModule(String moduleName) {
    StringJoiner jmodPathJoiner = new StringJoiner(File.separator);
    jmodPathJoiner.add(System.getProperty("java.home"));
    jmodPathJoiner.add("jmods");
    jmodPathJoiner.add(moduleName);
    return jmodPathJoiner.toString();
  }

}
