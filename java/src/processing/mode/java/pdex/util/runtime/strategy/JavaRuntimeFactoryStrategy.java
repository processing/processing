package processing.mode.java.pdex.util.runtime.strategy;

import processing.mode.java.pdex.util.runtime.RuntimeConst;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;

public class JavaRuntimeFactoryStrategy implements RuntimeFactoryStrategy {

  @Override
  public List<String> buildClasspath() {
    return Arrays.stream(RuntimeConst.STANDARD_MODULES)
        .map(this::buildJavaRuntimeClassPathEntry)
        .collect(Collectors.toList());
  }

  private String buildJavaRuntimeClassPathEntry(String moduleName) {
    StringJoiner jmodPathJoiner = new StringJoiner(File.separator);
    jmodPathJoiner.add(System.getProperty("java.home"));
    jmodPathJoiner.add("jmods");
    jmodPathJoiner.add(moduleName);
    return jmodPathJoiner.toString();
  }

}
