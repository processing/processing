package processing.mode.java.pdex;

import com.google.classpath.ClassPath;

import org.eclipse.jdt.core.dom.CompilationUnit;

import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import processing.app.Sketch;

public class PreprocessedSketch {

  public final Sketch sketch;

  public final CompilationUnit compilationUnit;

  public final String[] classPathArray;
  public final ClassPath classPath;
  public final URLClassLoader classLoader;

  public final int[] tabStarts;

  public final String pdeCode;
  public final String preprocessedCode;

  public final SourceMapping syntaxMapping;
  public final SourceMapping compilationMapping;

  public final boolean hasSyntaxErrors;
  public final boolean hasCompilationErrors;

  public final List<Problem> problems;

  public final List<ImportStatement> programImports;
  public final List<ImportStatement> coreAndDefaultImports;
  public final List<ImportStatement> codeFolderImports;


  // TODO: optimize
  public static int lineToOffset(String text, int line) {
    int lineOffset = 0;
    for (int i = 0; i < line && lineOffset >= 0; i++) {
      lineOffset = text.indexOf('\n', lineOffset) + 1;
    }
    return lineOffset;
  }


  // TODO: optimize
  public static int offsetToLine(String text, int start, int offset) {
    int line = 0;
    while (offset >= start) {
      offset = text.lastIndexOf('\n', offset-1);
      line++;
    }
    return line - 1;
  }


  // TODO: optimize
  public static int offsetToLine(String text, int offset) {
    return offsetToLine(text, 0, offset);
  }


  // TODO: optimize, build lookup together with tabStarts
  public int tabIndexToTabStartLine(int tabIndex) {
    int pdeLineNumber = 0;
    for (int i = 0; i < tabIndex; i++) {
      pdeLineNumber += sketch.getCode(i).getLineCount();
    }
    return pdeLineNumber;
  }


  public int tabLineToJavaLine(int tabIndex, int tabLine) {
    int tabStartLine = tabIndexToTabStartLine(tabIndex);
    int pdeLine = tabStartLine + tabLine;
    int pdeLineOffset = lineToOffset(pdeCode, pdeLine);
    int javaLineOffset = syntaxMapping.getOutputOffset(pdeLineOffset);
    if (compilationMapping != null) {
      javaLineOffset = compilationMapping.getOutputOffset(javaLineOffset);
    }
    return offsetToLine(preprocessedCode, javaLineOffset);
  }


  public int tabOffsetToJavaOffset(int tabIndex, int tabOffset) {
    int tabStartLine = tabIndexToTabStartLine(tabIndex);
    int tabStartOffset = lineToOffset(pdeCode, tabStartLine);
    int pdeOffset = tabStartOffset + tabOffset;
    int javaOffset = syntaxMapping.getOutputOffset(pdeOffset);
    if (compilationMapping != null) {
      javaOffset = compilationMapping.getOutputOffset(javaOffset);
    }
    return javaOffset;
  }


  public int javaOffsetToPdeOffset(int javaOffset) {
    int pdeOffset = javaOffset;
    if (compilationMapping != null) {
      pdeOffset = compilationMapping.getInputOffset(pdeOffset);
    }
    if (syntaxMapping != null) {
      pdeOffset = syntaxMapping.getInputOffset(pdeOffset);
    }
    return pdeOffset;
  }


  public int pdeOffsetToTabIndex(int pdeOffset) {
    int tab = Arrays.binarySearch(tabStarts, pdeOffset);
    if (tab < 0) {
      tab = -(tab + 1) - 1;
    }
    return tab;
  }


  public int pdeOffsetToTabOffset(int tabIndex, int pdeOffset) {
    int tabStartOffset = tabStarts[tabIndex];
    return pdeOffset - tabStartOffset;
  }


  public int tabOffsetToTabLine(int tabIndex, int tabOffset) {
    int tabStartOffset = tabStarts[tabIndex];
    return offsetToLine(pdeCode, tabStartOffset, tabStartOffset + tabOffset);
  }



  /// BUILDER BUSINESS /////////////////////////////////////////////////////////

  /**
   * There is a lot of fields and having constructor with this many parameters
   * is just not practical. Fill stuff into builder and then simply build it.
   * Builder also guards against calling methods in the middle of building process.
   */

  public static class Builder {
    public Sketch sketch;

    public CompilationUnit compilationUnit;

    public String[] classPathArray;
    public ClassPath classPath;
    public URLClassLoader classLoader;

    public int[] tabStarts = new int[0];

    public String pdeCode;
    public String preprocessedCode;

    public SourceMapping syntaxMapping;
    public SourceMapping compilationMapping;

    public boolean hasSyntaxErrors;
    public boolean hasCompilationErrors;

    public final List<Problem> problems = new ArrayList<>();

    public final List<ImportStatement> programImports = new ArrayList<>();
    public final List<ImportStatement> coreAndDefaultImports = new ArrayList<>();
    public final List<ImportStatement> codeFolderImports = new ArrayList<>();

    public PreprocessedSketch build() {
      return new PreprocessedSketch(this);
    }
  }

  public static PreprocessedSketch empty() {
    return new Builder().build();
  }

  private PreprocessedSketch(Builder b) {
    sketch = b.sketch;

    compilationUnit = b.compilationUnit;

    classPathArray = b.classPathArray;
    classPath = b.classPath;
    classLoader = b.classLoader;

    tabStarts = b.tabStarts;

    pdeCode = b.pdeCode;
    preprocessedCode = b.preprocessedCode;

    syntaxMapping = b.syntaxMapping;
    compilationMapping = b.compilationMapping;

    hasSyntaxErrors = b.hasSyntaxErrors;
    hasCompilationErrors = b.hasCompilationErrors;

    problems = Collections.unmodifiableList(b.problems);

    programImports = Collections.unmodifiableList(b.programImports);
    coreAndDefaultImports = Collections.unmodifiableList(b.coreAndDefaultImports);
    codeFolderImports = Collections.unmodifiableList(b.codeFolderImports);
  }
}
