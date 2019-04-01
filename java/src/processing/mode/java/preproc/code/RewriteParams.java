package processing.mode.java.preproc.code;

import org.antlr.v4.runtime.TokenStreamRewriter;
import processing.mode.java.preproc.PdePreprocessor;

import java.util.List;
import java.util.Optional;


public class RewriteParams {

  private final String version;
  private final String sketchName;
  private final boolean isTested;
  private final TokenStreamRewriter rewriter;
  private final PdePreprocessor.Mode mode;
  private final boolean foundMain;
  private final int lineOffset;
  private final List<String> coreImports;
  private final List<String> defaultImports;
  private final List<String> codeFolderImports;
  private final List<String> foundImports;
  private final Optional<String> sketchWidth;
  private final Optional<String> sketchHeight;
  private final Optional<String> sketchRenderer;
  private final boolean isSizeValidInGlobal;

  public RewriteParams(String newVersion, String newSketchName, boolean newIsTested,
                       TokenStreamRewriter newRewriter, PdePreprocessor.Mode newMode,
                       boolean newFoundMain, int newLineOffset, List<String> newCoreImports,
                       List<String> newDefaultImports, List<String> newCodeFolderImports,
                       List<String> newFoundImports, Optional<String> newSketchWidth,
                       Optional<String> newSketchHeight, Optional<String> newSketchRenderer,
                       boolean newIsSizeValidInGlobal) {

    version = newVersion;
    sketchName = newSketchName;
    isTested = newIsTested;
    rewriter = newRewriter;
    mode = newMode;
    foundMain = newFoundMain;
    lineOffset = newLineOffset;
    coreImports = newCoreImports;
    defaultImports = newDefaultImports;
    codeFolderImports = newCodeFolderImports;
    foundImports = newFoundImports;
    sketchWidth = newSketchWidth;
    sketchHeight = newSketchHeight;
    sketchRenderer = newSketchRenderer;
    isSizeValidInGlobal = newIsSizeValidInGlobal;
  }

  public String getVersion() {
    return version;
  }

  public String getSketchName() {
    return sketchName;
  }

  public boolean getIsTested() {
    return isTested;
  }

  public TokenStreamRewriter getRewriter() {
    return rewriter;
  }

  public PdePreprocessor.Mode getMode() {
    return mode;
  }

  public boolean getFoundMain() {
    return foundMain;
  }

  public int getLineOffset() {
    return lineOffset;
  }

  public List<String> getCoreImports() {
    return coreImports;
  }

  public List<String> getDefaultImports() {
    return defaultImports;
  }

  public List<String> getCodeFolderImports() {
    return codeFolderImports;
  }

  public List<String> getFoundImports() {
    return foundImports;
  }

  public Optional<String> getSketchWidth() {
    return sketchWidth;
  }

  public Optional<String> getSketchHeight() {
    return sketchHeight;
  }

  public Optional<String> getSketchRenderer() {
    return sketchRenderer;
  }

  public boolean getIsSizeValidInGlobal() {
    return isSizeValidInGlobal;
  }

}
