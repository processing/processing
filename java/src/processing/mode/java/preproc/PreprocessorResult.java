package processing.mode.java.preproc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import processing.app.SketchException;
import processing.mode.java.pdex.TextTransform;
import processing.mode.java.preproc.PdePreprocessor;


/**
 * Result of sketch preprocessing.
 */
public class PreprocessorResult {

  private final int headerOffset;
  private final String className;
  private final List<String> extraImports;
  private final PdePreprocessor.Mode programType;
  private final List<TextTransform.Edit> edits;

  public PreprocessorResult(PdePreprocessor.Mode programType,
                            int headerOffset,
                            String className,
                            List<String> extraImports,
                            List<TextTransform.Edit> edits) {

    if (className == null) {
      throw new RuntimeException("Could not find main class");
    }

    this.headerOffset = headerOffset;
    this.className = className;
    this.extraImports = Collections.unmodifiableList(new ArrayList<String>(extraImports));
    this.programType = programType;
    this.edits = edits;
  }

  public int getHeaderOffset() {
    return headerOffset;
  }

  public String getClassName() {
    return className;
  }

  public List<String> getExtraImports() {
    return extraImports;
  }

  public PdePreprocessor.Mode getProgramType() {
    return programType;
  }

  public List<TextTransform.Edit> getEdits() {
    return edits;
  }

}
