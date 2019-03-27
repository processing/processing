package test.processing.mode.java;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import processing.app.Preferences;
import processing.app.SketchException;
import processing.mode.java.preproc.PdePreprocessor;
import processing.mode.java.AutoFormat;

import test.processing.mode.java.UTCompiler;


public class ProcessingTestUtil {
  static void init() {
    // noop; just causes class to be loaded
  }
  
  private static final String RESOURCES = "test/resources/";
  static final UTCompiler COMPILER;

  static {
    try {
      COMPILER = new UTCompiler(new File("bin"), new File("../core/bin"));
      Preferences.load(new FileInputStream(res("preferences.txt")));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    //System.err.println("ProcessingTestUtil initialized.");
  }

  static String normalize(final Object s) {
    return String.valueOf(s).replace("\r", "");
  }

  static String preprocess(final String name, final File resource)
      throws SketchException {
    final String program = read(resource);
    final StringWriter out = new StringWriter();
    new PdePreprocessor(name, 4, true).write(out, program);
    return normalize(out);
  }
  
  static String format(final File resource)
  {
    return format(read(resource));
  }

  static String format(final String programText) {
    return normalize(new AutoFormat().format(programText));
  }

  static File res(final String resourceName) {
    return new File(RESOURCES, resourceName);
  }

  static String read(final File f) {
    try {
      final FileInputStream fin = new FileInputStream(f);
      final InputStreamReader in = new InputStreamReader(fin, "UTF-8");
      try {
        final StringBuilder sb = new StringBuilder();
        final char[] buf = new char[1 << 12];
        int len;
        while ((len = in.read(buf)) != -1)
          sb.append(buf, 0, len);
        return normalize(sb);
      } finally {
        in.close();
      }
    } catch (Exception e) {
      throw new RuntimeException("Unexpected", e);
    }
  }

}
