package test.processing.parsing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static test.processing.parsing.ProcessingTestUtil.res;
import java.io.File;
import java.io.FileWriter;
import org.junit.BeforeClass;
import org.junit.Test;

public class AutoFormatTests {

  @BeforeClass
  public static void init() {
    ProcessingTestUtil.init();
  }

  static void expectGood(final String id) {
    try {
      final String program = ProcessingTestUtil.format(res(id + ".pde"));
      final File expectedFile = res(id + ".expected");
      if (expectedFile.exists()) {
        final String expected = ProcessingTestUtil.read(expectedFile);
        assertEquals(expected, program);
      } else {
        System.err.println("WARN: " + id
            + " does not have an expected output file. Generating.");
        final FileWriter sug = new FileWriter(res(id + ".expected"));
        sug.write(ProcessingTestUtil.normalize(program));
        sug.close();
      }
    } catch (Exception e) {
      if (!e.equals(e.getCause()) && e.getCause() != null)
        fail(e.getCause().toString());
      else
        e.printStackTrace(System.err);
      fail(e.toString());
    }
  }

  @Test
  public void bug109() {
    expectGood("bug109");
  }
}
