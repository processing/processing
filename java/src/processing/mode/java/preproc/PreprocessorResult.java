package processing.mode.java.preproc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import processing.app.SketchException;

/**
 * 
 * @author Jonathan Feinberg &lt;jdf@pobox.com&gt;
 *
 */
public class PreprocessorResult {
  public final int headerOffset;
  public final String className;
  public final List<String> extraImports;
  public final PdePreprocessor.Mode programType;

  public PreprocessorResult(PdePreprocessor.Mode programType,
                          int headerOffset, String className,
                          final List<String> extraImports) throws SketchException {
    if (className == null) {
      throw new SketchException("Could not find main class");
    }
    this.headerOffset = headerOffset;
    this.className = className;
    this.extraImports = Collections.unmodifiableList(new ArrayList<String>(extraImports));
    this.programType = programType;
  }

}
