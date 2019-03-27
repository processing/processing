/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */
package processing.mode.java.preproc;

import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import processing.app.Preferences;
import processing.app.SketchException;


public class PdePreprocessor {

  public static enum Mode {
    STATIC, ACTIVE, JAVA
  }

  private String sketchName;
  private int tabSize;

  private boolean hasMain;

  private final boolean isTested;

  public PdePreprocessor(final String sketchName) {
    this(sketchName, Preferences.getInteger("editor.tabs.size"), false);
  }

  public PdePreprocessor(final String sketchName, final int tabSize) {
    this(sketchName, tabSize, false);
  }

  public PdePreprocessor(final String sketchName, final int tabSize, boolean isTested) {
    this.sketchName = sketchName;
    this.tabSize = tabSize;
    this.isTested = isTested;
  }

  public PreprocessorResult write(final Writer out, String program) throws SketchException {
    return write(out, program, null);
  }

  public PreprocessorResult write(Writer outWriter, String inProgram,
                                  Iterable<String> codeFolderPackages)
                                    throws SketchException {

    ArrayList<String> codeFolderImports = new ArrayList<>();
    if (codeFolderPackages != null) {
      for (String item : codeFolderPackages) {
        codeFolderImports.add(item + ".*");
      }
    }

    if (Preferences.getBoolean("preproc.substitute_unicode")) {
      inProgram = substituteUnicode(inProgram);
    }

    while (inProgram.endsWith("\n")) {
      inProgram = inProgram.substring(0, inProgram.length() - 1);
    }

    CommonTokenStream tokens;
    {
      ANTLRInputStream antlrInStream = new ANTLRInputStream(inProgram);
      ProcessingLexer lexer = new ProcessingLexer(antlrInStream);
      tokens = new CommonTokenStream(lexer);
    }

    PdeParseTreeListener listener = createListener(tokens, sketchName);
    listener.setTested(isTested);
    listener.setIndent(tabSize);
    listener.setCoreImports(getCoreImports());
    listener.setDefaultImports(getDefaultImports());
    listener.setCodeFolderImports(codeFolderImports);

    ParseTree tree;
    {
      ProcessingParser parser = new ProcessingParser(tokens);
      parser.removeErrorListeners();
      parser.addErrorListener(PdeIgnoreErrorListener.getInstance());
      parser.setBuildParseTree(true);
      tree = parser.processingSketch();
    }

    ParseTreeWalker treeWalker = new ParseTreeWalker();
    treeWalker.walk(listener, tree);

    SketchException sketchException = listener.getSketchException();
    if (sketchException != null) throw sketchException;

    String outputProgram = listener.getOutputProgram();
    PrintWriter outPrintWriter = new PrintWriter(outWriter);
    outPrintWriter.print(outputProgram);

    hasMain = listener.foundMain();

    return listener.getResult();
  }

  protected PdeParseTreeListener createListener(CommonTokenStream tokens, String sketchName) {
    return new PdeParseTreeListener(tokens, sketchName);
  }

  public boolean hasMain() {
    return hasMain;
  }

  private static String substituteUnicode(String program) {
    // check for non-ascii chars (these will be/must be in unicode format)
    char p[] = program.toCharArray();
    int unicodeCount = 0;
    for (int i = 0; i < p.length; i++) {
      if (p[i] > 127)
        unicodeCount++;
    }
    if (unicodeCount == 0)
      return program;
    // if non-ascii chars are in there, convert to unicode escapes
    // add unicodeCount * 5.. replacing each unicode char
    // with six digit uXXXX sequence (xxxx is in hex)
    // (except for nbsp chars which will be a replaced with a space)
    int index = 0;
    char p2[] = new char[p.length + unicodeCount * 5];
    for (int i = 0; i < p.length; i++) {
      if (p[i] < 128) {
        p2[index++] = p[i];
      } else if (p[i] == 160) { // unicode for non-breaking space
        p2[index++] = ' ';
      } else {
        int c = p[i];
        p2[index++] = '\\';
        p2[index++] = 'u';
        char str[] = Integer.toHexString(c).toCharArray();
        // add leading zeros, so that the length is 4
        //for (int i = 0; i < 4 - str.length; i++) p2[index++] = '0';
        for (int m = 0; m < 4 - str.length; m++)
          p2[index++] = '0';
        System.arraycopy(str, 0, p2, index, str.length);
        index += str.length;
      }
    }
    return new String(p2, 0, index);
  }

  public String[] getCoreImports() {
    return new String[] {
      "processing.core.*",
      "processing.data.*",
      "processing.event.*",
      "processing.opengl.*"
    };
  }

  public String[] getDefaultImports() {
    // These may change in-between (if the prefs panel adds this option)
    //String prefsLine = Preferences.get("preproc.imports");
    //return PApplet.splitTokens(prefsLine, ", ");
    return new String[] {
      "java.util.HashMap",
      "java.util.ArrayList",
      "java.io.File",
      "java.io.BufferedReader",
      "java.io.PrintWriter",
      "java.io.InputStream",
      "java.io.OutputStream",
      "java.io.IOException"
    };
  }
}
