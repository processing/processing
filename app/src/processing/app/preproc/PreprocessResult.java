package processing.app.preproc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import processing.app.debug.RunnerException;

/**
 * 
 * @author Jonathan Feinberg &lt;jdf@pobox.com&gt;
 *
 */
public class PreprocessResult {
  public final int headerOffset;
  public final String className;
  public final List<String> extraImports;

  public PreprocessResult(int headerOffset, String className,
                          final List<String> extraImports)
      throws RunnerException {
    if (className == null)
      throw new RunnerException("Could not find main class");
    this.headerOffset = headerOffset;
    this.className = className;
    this.extraImports = Collections
        .unmodifiableList(new ArrayList<String>(extraImports));
  }

}
