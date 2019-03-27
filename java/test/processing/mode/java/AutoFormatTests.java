package processing.mode.java;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static processing.mode.java.ProcessingTestUtil.res;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import org.junit.BeforeClass;
import org.junit.Test;

public class AutoFormatTests {

  @BeforeClass
  public static void init() {
    ProcessingTestUtil.init();
  }

  static void expectGood(final String id) {
    try {
      final String formattedProgram = ProcessingTestUtil.format(res(id + ".pde"));
      final File goldenFile = res(id + ".expected");
      checkGolden(formattedProgram, goldenFile);
      // check that the formatted text doesn't change
      checkGolden(ProcessingTestUtil.format(formattedProgram), goldenFile);
    } catch (Exception e) {
      if (!e.equals(e.getCause()) && e.getCause() != null)
        fail(e.getCause().toString());
      else
        e.printStackTrace(System.err);
      fail(e.toString());
    }
  }

  private static void checkGolden(final String expectedText,
                                  final File goldenFile) throws IOException {
    if (goldenFile.exists()) {
      final String expected = ProcessingTestUtil.read(goldenFile);
      assertEquals(expected, expectedText);
    } else {
      System.err.println("WARN: golden file " + goldenFile
          + " does not exist. Generating.");
      final FileWriter sug = new FileWriter(goldenFile);
      sug.write(ProcessingTestUtil.normalize(expectedText));
      sug.close();
    }
  }

  @Test
  public void bug109() {
    expectGood("bug109");
  }

  @Test
  public void bug405() {
    expectGood("bug405");
  }
  
  @Test
  public void bug420() {
    expectGood("bug420");
  }
}
