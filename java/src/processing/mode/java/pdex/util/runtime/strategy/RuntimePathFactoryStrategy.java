package processing.mode.java.pdex.util.runtime.strategy;

import processing.app.Sketch;
import processing.mode.java.JavaMode;
import processing.mode.java.pdex.ImportStatement;

import java.util.List;


/**
 * Strategy which generates part of the classpath and/or module path.
 *
 * <p>
 * Strategy for factories each of which generate part of the classpath and/or module path required
 * by a sketch through user supplied requirements, mode (as in JavaMode) requirements, or transitive
 * requirements imposed by third party libraries.
 * </p>
 */
public interface RuntimePathFactoryStrategy {

  /**
   * Create classpath and/or module path entries.
   *
   * @param mode The mode engaged by the user like JavaMode.
   * @param programImports The program imports imposed by the user within their sketch.
   * @param sketch The sketch provided by the user.
   * @return List of classpath and/or module path entries.
   */
  List<String> buildClasspath(JavaMode mode, List<ImportStatement> programImports, Sketch sketch);

}
