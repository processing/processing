package test.processing.parsing;

import static org.junit.Assert.fail;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import org.junit.BeforeClass;
import org.junit.Test;
import processing.app.Base;
import processing.app.Preferences;
import processing.app.debug.RunnerException;
import processing.app.preproc.PdePreprocessor;
import antlr.ANTLRException;

public class ParserTests {

  @BeforeClass
  static public void initPrefs() {
    System.err.println("Initializing prefs");
    Base.initPlatform();
    Preferences.init(null);
  }

  static String read(final String path) {
    try {
      final FileInputStream fin = new FileInputStream(path);
      final InputStreamReader in = new InputStreamReader(fin, "UTF-8");
      try {
        final StringBuilder sb = new StringBuilder();
        final char[] buf = new char[1 << 12];
        int len;
        while ((len = in.read(buf)) != -1)
          sb.append(buf, 0, len);
        return sb.toString();
      } finally {
        in.close();
      }
    } catch (Exception e) {
      throw new RuntimeException("Unexpected", e);
    }
  }

  static String preprocess(final String resource) throws RunnerException,
      ANTLRException {
    final String program = read("../../../app/test/resources/" + resource);
    final StringWriter out = new StringWriter();
    new PdePreprocessor(resource, 4).write(out, program);
    return out.toString();
  }

  static void expectGood(final String resource) {
    try {
      preprocess(resource);
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  @Test
  public void bug5a() {
    expectGood("bug5.a.pde");
  }

  @Test
  public void bug5b() {
    expectGood("bug5.b.pde");
  }
}
