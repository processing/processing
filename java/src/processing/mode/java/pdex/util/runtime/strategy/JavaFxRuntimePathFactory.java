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


public class JavaFxRuntimePathFactory implements RuntimePathFactoryStrategy {

  @Override
  public List<String> buildClasspath(JavaMode mode, List<ImportStatement> imports, Sketch sketch) {
    return Arrays.stream(RuntimeConst.JAVA_FX_JARS)
        .map(this::buildEntry)
        .collect(Collectors.toList());
  }

  private String buildEntry(String jarName) {
    StringJoiner joiner = new StringJoiner(File.separator);
    joiner.add(System.getProperty("java.home"));
    joiner.add("lib");
    joiner.add(jarName);

    return joiner.toString();
  }

}
