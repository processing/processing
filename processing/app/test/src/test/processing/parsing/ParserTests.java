package test.processing.parsing;

import static org.junit.Assert.fail;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import org.junit.BeforeClass;
import org.junit.Test;
import processing.app.Preferences;
import processing.app.debug.RunnerException;
import processing.app.preproc.PdePreprocessor;
import antlr.ANTLRException;

public class ParserTests {

  private static final String RESOURCES = "test/resources/";

  private static File res(final String resourceName) {
    return new File(RESOURCES, resourceName);
  }

  @BeforeClass
  static public void initPrefs() throws Exception {
    Preferences.load(new FileInputStream(res("preferences.txt")));
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
    final String program = read(res(resource));
    final StringWriter out = new StringWriter();
    new PdePreprocessor(resource, 4).write(out, program);
    return out.toString();
  }

  static void expectGood(final String resource) {
    try {
      preprocess(resource);
    } catch (Exception e) {
      if (!e.equals(e.getCause()))
        fail(e.getCause().getMessage());
      else
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

  @Test
  public void bug1511() {
    expectGood("bug1511.pde");
  }
}
