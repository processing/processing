package processing.mode.java.pdex.util.runtime.strategy;

import processing.app.Sketch;
import processing.mode.java.JavaMode;
import processing.mode.java.pdex.ImportStatement;

import java.util.List;


public interface RuntimePathFactoryStrategy {

  List<String> buildClasspath(JavaMode mode, List<ImportStatement> programImports, Sketch sketch);

}
