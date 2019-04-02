package processing.mode.java.preproc.code;

import org.antlr.v4.runtime.TokenStreamRewriter;
import processing.mode.java.preproc.PdePreprocessor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;


/**
 * Builder to help generate a {RewriteParams}.
 */
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

  /**
   * Create a new params build.
   *
   * @param newVersion The version to include in generated RewriteParams.
   */
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

  /**
   * Specify the name of the sketch.
   *
   * @param newSketchName The name of the sketch.
   */
  public void setSketchName(String newSketchName) {
    sketchName = Optional.ofNullable(newSketchName);
  }

  /**
   * Specify if this is being run as part of automated testing.
   *
   * @param newIsTested Flag indicating if this is being run as part of automated testing.
   */
  public void setIsTested(boolean newIsTested) {
    isTested = Optional.of(newIsTested);
  }

  /**
   * Specify rewriter into which edits should be made.
   *
   * @param newRewriter The rewriter into which edits should be made.
   */
  public void setRewriter(TokenStreamRewriter newRewriter) {
    rewriter = Optional.ofNullable(newRewriter);
  }

  /**
   * Specify mode (like STATIC) in which processing is being run.
   *
   * @param newMode The mode (like STATIC) in which processing is being run.
   */
  public void setMode(PdePreprocessor.Mode newMode) {
    mode = Optional.ofNullable(newMode);
  }

  /**
   * Specify if a user-provided main method was found in preprocessing.
   *
   * @param newFoundMain Flag indicating if a user-provided main method was found in preprocessing.
   */
  public void setFoundMain(boolean newFoundMain) {
    foundMain = Optional.of(newFoundMain);
  }

  /**
   * Specify line offset of the preprocessor prior to rewrite.
   *
   * @param newLineOffset The line offset of the preprocessor prior to rewrite.
   */
  public void setLineOffset(int newLineOffset) {
    lineOffset = Optional.of(newLineOffset);
  }

  /**
   * Specify width of the sketch.
   *
   * @param newSketchWidth The width of the sketch or code used to generate it. If not included,
   *    call to size will not be made.
   */
  public void setSketchWidth(String newSketchWidth) {
    sketchWidth = Optional.ofNullable(newSketchWidth);
  }

  /**
   * Specify height of the sketch.
   *
   * @param newSketchHeight The height of the sketch or code used to generate it. If not included,
   *    call to size will not be made.
   */
  public void setSketchHeight(String newSketchHeight) {
    sketchHeight = Optional.ofNullable(newSketchHeight);
  }

  /**
   * Specify renderer like P2D.
   *
   * @param newSketchRenderer The renderer like P2D.
   */
  public void setSketchRenderer(String newSketchRenderer) {
    sketchRenderer = Optional.ofNullable(newSketchRenderer);
  }

  /**
   * Specify if the user made a valid call to size in sketch global context.
   *
   * @param newIsSizeValidInGlobal Flag indicating if a call to size is valid when that call to size
   *    is made from sketch global context.
   */
  public void setIsSizeValidInGlobal(boolean newIsSizeValidInGlobal) {
    isSizeValidInGlobal = Optional.of(newIsSizeValidInGlobal);
  }

  /**
   * Add imports required for processing to function.
   *
   * @param newImports The set of imports to include that are required for processing.
   */
  public void addCoreImports(Collection<String> newImports) {
    coreImports.addAll(newImports);
  }

  /**
   * Add imports that are included ahead of time for the user.
   *
   * @param newImports The set of imports included for user convenience.
   */
  public void addDefaultImports(Collection<String> newImports) {
    defaultImports.addAll(newImports);
  }

  /**
   * Add imports required for the sketch to reach code in its own code folder.
   *
   * @param newImports The imports required to include other code in the code folder.
   */
  public void addCodeFolderImports(Collection<String> newImports) {
    codeFolderImports.addAll(newImports);
  }

  /**
   * Add imports included manually by the user.
   *
   * @param newImports The imports included by the user.
   */
  public void addFoundImports(Collection<String> newImports) {
    foundImports.addAll(newImports);
  }

  /**
   * Build a new set of rewrite parameters.
   *
   * @return Parameters required to execute {RewriterCodeGenerator};
   */
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
        sketchWidth,
        sketchHeight,
        sketchRenderer,
        isSizeValidInGlobal.get()
    );
  }
}
