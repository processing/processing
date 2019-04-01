package processing.mode.java.preproc.code;

import org.antlr.v4.runtime.TokenStreamRewriter;
import processing.mode.java.preproc.PdePreprocessor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;


public class RewriteParamsBuilder {

  private final String version;

  private Optional<String> sketchName;
  private Optional<Boolean> isTested;
  private Optional<TokenStreamRewriter> rewriter;
  private Optional<PdePreprocessor.Mode> mode;
  private Optional<Boolean> foundMain;
  private Optional<Integer> lineOffset;
  private Optional<String> sketchWidth;
  private Optional<String> sketchHeight;
  private Optional<String> sketchRenderer;
  private Optional<Boolean> isSizeValidInGlobal;

  private ArrayList<String> coreImports;
  private ArrayList<String> defaultImports;
  private ArrayList<String> codeFolderImports;
  private ArrayList<String> foundImports;

  public RewriteParamsBuilder(String newVersion) {
    version = newVersion;

    coreImports = new ArrayList<>();
    defaultImports = new ArrayList<>();
    codeFolderImports = new ArrayList<>();
    foundImports = new ArrayList<>();

    sketchName = Optional.empty();
    isTested = Optional.empty();
    rewriter = Optional.empty();
    mode = Optional.empty();
    foundMain = Optional.empty();
    lineOffset = Optional.empty();
    sketchWidth = Optional.empty();
    sketchHeight = Optional.empty();
    sketchRenderer = Optional.empty();
    isSizeValidInGlobal = Optional.empty();
  }

  public void setSketchName(String newSketchName) {
    sketchName = Optional.of(newSketchName);
  }

  public void setIsTested(boolean newIsTested) {
    isTested = Optional.of(newIsTested);
  }

  public void setRewriter(TokenStreamRewriter newRewriter) {
    rewriter = Optional.of(newRewriter);
  }

  public void setMode(PdePreprocessor.Mode newMode) {
    mode = Optional.of(newMode);
  }

  public void setFoundMain(boolean newFoundMain) {
    foundMain = Optional.of(newFoundMain);
  }

  public void setLineOffset(int newLineOffset) {
    lineOffset = Optional.of(newLineOffset);
  }

  public void setSketchWidth(String newSketchWidth) {
    sketchWidth = Optional.of(newSketchWidth);
  }

  public void setSketchHeight(String newSketchHeight) {
    sketchHeight = Optional.of(newSketchHeight);
  }

  public void setSketchRenderer(String newSketchRenderer) {
    sketchRenderer = Optional.of(newSketchRenderer);
  }

  public void setIsSizeValidInGlobal(boolean newIsSizeValidInGlobal) {
    isSizeValidInGlobal = Optional.of(newIsSizeValidInGlobal);
  }

  public void addCoreImport(String newImport) {
    coreImports.add(newImport);
  }

  public void addDefaultImport(String newImport) {
    defaultImports.add(newImport);
  }

  public void addCodeFolderImport(String newImport) {
    codeFolderImports.add(newImport);
  }

  public void addFoundImport(String newImport) {
    foundImports.add(newImport);
  }

  public void addCoreImports(Collection<String> newImports) {
    coreImports.addAll(newImports);
  }

  public void addDefaultImports(Collection<String> newImports) {
    defaultImports.addAll(newImports);
  }

  public void addCodeFolderImports(Collection<String> newImports) {
    codeFolderImports.addAll(newImports);
  }

  public void addFoundImports(Collection<String> newImports) {
    foundImports.addAll(newImports);
  }

  public RewriteParams build() {
    if (sketchName.isEmpty()) {
      throw new RuntimeException("Expected sketchName to be set");
    }

    if (isTested.isEmpty()) {
      throw new RuntimeException("Expected isTested to be set");
    }

    if (rewriter.isEmpty()) {
      throw new RuntimeException("Expected rewriter to be set");
    }

    if (mode.isEmpty()) {
      throw new RuntimeException("Expected mode to be set");
    }

    if (foundMain.isEmpty()) {
      throw new RuntimeException("Expected foundMain to be set");
    }

    if (lineOffset.isEmpty()) {
      throw new RuntimeException("Expected lineOffset to be set");
    }

    if (sketchWidth.isEmpty()) {
      throw new RuntimeException("Expected sketchWidth to be set");
    }

    if (sketchHeight.isEmpty()) {
      throw new RuntimeException("Expected sketchHeight to be set");
    }

    if (sketchRenderer.isEmpty()) {
      throw new RuntimeException("Expected sketchRenderer to be set");
    }

    if (isSizeValidInGlobal.isEmpty()) {
      throw new RuntimeException("Expected isSizeValidInGlobal to be set");
    }

    return new RewriteParams(
        version,
        sketchName.get(),
        isTested.get(),
        rewriter.get(),
        mode.get(),
        foundMain.get(),
        lineOffset.get(),
        coreImports,
        defaultImports,
        codeFolderImports,
        foundImports,
        sketchWidth.get(),
        sketchHeight.get(),
        sketchRenderer.get(),
        isSizeValidInGlobal.get()
    );
  }
}
