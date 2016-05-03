package processing.mode.java.pdex;

import com.google.classpath.ClassPath;

import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;

import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import processing.app.Sketch;
import processing.mode.java.pdex.TextTransform.OffsetMapper;

public class PreprocessedSketch {

  public final Sketch sketch;

  public final CompilationUnit compilationUnit;

  public final String[] classPathArray;
  public final ClassPath classPath;
  public final URLClassLoader classLoader;

  public final ClassPath searchClassPath;

  public final int[] tabStartOffsets;

  public final String pdeCode;
  public final String javaCode;

  public final OffsetMapper offsetMapper;

  public final boolean hasSyntaxErrors;
  public final boolean hasCompilationErrors;

  public final List<IProblem> problems;

  public final List<ImportStatement> programImports;
  public final List<ImportStatement> coreAndDefaultImports;
  public final List<ImportStatement> codeFolderImports;



  /// JAVA -> SKETCH -----------------------------------------------------------


  public static class SketchInterval {
    private SketchInterval(int tabIndex,
                           int startTabOffset, int stopTabOffset,
                           int startPdeOffset, int stopPdeOffset) {
      this.tabIndex = tabIndex;
      this.startTabOffset = startTabOffset;
      this.stopTabOffset = stopTabOffset;
      this.startPdeOffset = startPdeOffset;
      this.stopPdeOffset = stopPdeOffset;
    }

    final int tabIndex;
    final int startTabOffset;
    final int stopTabOffset;

    final int startPdeOffset;
    final int stopPdeOffset;
  }


  public SketchInterval mapJavaToSketch(ASTNode node) {
    return mapJavaToSketch(node.getStartPosition(),
                           node.getStartPosition() + node.getLength());
  }


  public SketchInterval mapJavaToSketch(int startJavaOffset, int stopJavaOffset) {
    boolean zeroLength = stopJavaOffset == startJavaOffset;
    int startPdeOffset = javaOffsetToPdeOffset(startJavaOffset);
    int stopPdeOffset = zeroLength ?
        javaOffsetToPdeOffset(stopJavaOffset) :
        javaOffsetToPdeOffset(stopJavaOffset-1)+1;
    int tabIndex = pdeOffsetToTabIndex(startPdeOffset);

    return new SketchInterval(tabIndex,
                              pdeOffsetToTabOffset(tabIndex, startPdeOffset),
                              pdeOffsetToTabOffset(tabIndex, stopPdeOffset),
                              startPdeOffset, stopPdeOffset);
  }


  private int javaOffsetToPdeOffset(int javaOffset) {
    return offsetMapper.getInputOffset(javaOffset);
  }


  private int pdeOffsetToTabIndex(int pdeOffset) {
    int tab = Arrays.binarySearch(tabStartOffsets, pdeOffset);
    if (tab < 0) {
      tab = -(tab + 1) - 1;
    }
    return tab;
  }


  private int pdeOffsetToTabOffset(int tabIndex, int pdeOffset) {
    int tabStartOffset = tabStartOffsets[tabIndex];
    return pdeOffset - tabStartOffset;
  }



  /// SKETCH -> JAVA -----------------------------------------------------------


  public int tabOffsetToJavaOffset(int tabIndex, int tabOffset) {
    int tabStartOffset = tabStartOffsets[tabIndex];
    int pdeOffset = tabStartOffset + tabOffset;
    return offsetMapper.getOutputOffset(pdeOffset);
  }



  /// LINE NUMBERS -------------------------------------------------------------


  public int tabOffsetToJavaLine(int tabIndex, int tabOffset) {
    int javaOffset = tabOffsetToJavaOffset(tabIndex, tabOffset);
    return offsetToLine(javaCode, javaOffset);
  }


  public int tabOffsetToTabLine(int tabIndex, int tabOffset) {
    int tabStartOffset = tabStartOffsets[tabIndex];
    return offsetToLine(pdeCode, tabStartOffset, tabStartOffset + tabOffset);
  }


  // TODO: optimize
  private static int offsetToLine(String text, int offset) {
    return offsetToLine(text, 0, offset);
  }


  // TODO: optimize
  private static int offsetToLine(String text, int start, int offset) {
    int line = 0;
    while (offset >= start) {
      offset = text.lastIndexOf('\n', offset-1);
      line++;
    }
    return line - 1;
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

    public ClassPath searchClassPath;

    public int[] tabStartOffsets = new int[0];

    public String pdeCode;
    public String javaCode;

    public OffsetMapper offsetMapper;

    public boolean hasSyntaxErrors;
    public boolean hasCompilationErrors;

    public List<IProblem> problems = new ArrayList<>();

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

    searchClassPath = b.searchClassPath;

    tabStartOffsets = b.tabStartOffsets;

    pdeCode = b.pdeCode;
    javaCode = b.javaCode;

    offsetMapper = b.offsetMapper != null ? b.offsetMapper : OffsetMapper.EMPTY_MAPPER;

    hasSyntaxErrors = b.hasSyntaxErrors;
    hasCompilationErrors = b.hasCompilationErrors;

    problems = Collections.unmodifiableList(b.problems);

    programImports = Collections.unmodifiableList(b.programImports);
    coreAndDefaultImports = Collections.unmodifiableList(b.coreAndDefaultImports);
    codeFolderImports = Collections.unmodifiableList(b.codeFolderImports);
  }
}
