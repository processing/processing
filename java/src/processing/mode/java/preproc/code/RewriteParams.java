package processing.mode.java.preproc.code;

import org.antlr.v4.runtime.TokenStreamRewriter;
import processing.mode.java.preproc.PdePreprocessor;

import java.util.List;
import java.util.Optional;


/**
 * Set of parameters required for re-writing as part of sketch preprocessing.
 */
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

  /**
   * Create a new set of parameters.
   *
   * @param newVersion The version of the preprocessor.
   * @param newSketchName The name of the sketch.
   * @param newIsTested Flag indicating if this is being run as part of automated testing.
   * @param newRewriter The rewriter into which edits should be made.
   * @param newMode The mode (like STATIC) in which processing is being run.
   * @param newFoundMain Flag indicating if a user-provided main method was found in preprocessing.
   * @param newLineOffset The line offset of the preprocessor prior to rewrite.
   * @param newCoreImports The set of imports to include that are required for processing.
   * @param newDefaultImports The set of imports included for user convenience.
   * @param newCodeFolderImports The imports required to include other code in the code folder.
   * @param newFoundImports The imports included by the user.
   * @param newSketchWidth The width of the sketch or code used to generate it. If not included,
   *    call to size will not be made.
   * @param newSketchHeight The height of the sketch or code used to generate it. If not included,
   *    call to size will not be made.
   * @param newSketchRenderer The renderer like P2D.
   * @param newIsSizeValidInGlobal Flag indicating if a call to size is valid when that call to size
   *    is made from sketch global context.
   */
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

  /**
   * Get the version of the preprocessor.
   *
   * @return The version of the preprocessor.
   */
  public String getVersion() {
    return version;
  }

  /**
   * The user provided or automated name of the sketch.
   *
   * @return The name of the sketch.
   */
  public String getSketchName() {
    return sketchName;
  }

  /**
   * Determine if this code is being exercised in automated test.
   *
   * @return Flag indicating if this is being run as part of automated testing.
   */
  public boolean getIsTested() {
    return isTested;
  }

  /**
   * Get the rewriter to be used in rewriting.
   *
   * @return The rewriter into which edits should be made.
   */
  public TokenStreamRewriter getRewriter() {
    return rewriter;
  }

  /**
   * Get the mode in which processing is being run.
   *
   * @return The mode (like STATIC) in which processing is being run.
   */
  public PdePreprocessor.Mode getMode() {
    return mode;
  }

  /**
   * Determine if the user provided their own main method.
   *
   * @return Flag indicating if a user-provided main method was found in preprocessing.
   */
  public boolean getFoundMain() {
    return foundMain;
  }

  /**
   * Determine the line offset of the preprocessor prior to rewrite.
   *
   * @return The line offset of the preprocessor prior to rewrite.
   */
  public int getLineOffset() {
    return lineOffset;
  }

  /**
   * Get imports required for processing.
   *
   * @return The set of imports to include that are required for processing.
   */
  public List<String> getCoreImports() {
    return coreImports;
  }

  /**
   * Get the imports added for user convenience.
   *
   * @return The set of imports included for user convenience.
   */
  public List<String> getDefaultImports() {
    return defaultImports;
  }

  /**
   * The imports required to access other code in the code folder.
   *
   * @return The imports required to include other code in the code folder.
   */
  public List<String> getCodeFolderImports() {
    return codeFolderImports;
  }

  /**
   * Get the users included by the user.
   *
   * @return The imports included by the user.
   */
  public List<String> getFoundImports() {
    return foundImports;
  }

  /**
   * Get the code used to determine sketch width if given.
   *
   * @return The width of the sketch or code used to generate it. If not included, call to size will
   *    not be made. Not included means it is an empty optional.
   */
  public Optional<String> getSketchWidth() {
    return sketchWidth;
  }

  /**
   * Get the code used to determine sketch height if given.
   *
   * @return The height of the sketch or code used to generate it. If not included, call to size
   *    will not be made. Not included means it is an empty optional.
   */
  public Optional<String> getSketchHeight() {
    return sketchHeight;
  }

  /**
   * Get the user provided renderer or an empty optional if user has not provided renderer.
   *
   * @return The renderer like P2D if given.
   */
  public Optional<String> getSketchRenderer() {
    return sketchRenderer;
  }

  /**
   * Determine if a call to size has been made in sketch global context.
   *
   * @return Flag indicating if a call to size is valid when that call to size is made from sketch
   *    global context.
   */
  public boolean getIsSizeValidInGlobal() {
    return isSizeValidInGlobal;
  }

}
