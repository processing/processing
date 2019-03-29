package processing.mode.java;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static processing.mode.java.ProcessingTestUtil.*;

import java.io.File;
import java.io.FileWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.BeforeClass;
import org.junit.Test;

import processing.app.SketchException;
import processing.app.exec.ProcessResult;
import org.antlr.v4.runtime.RecognitionException;
import processing.mode.java.preproc.PdePreprocessIssueException;


public class ParserTests {

  @BeforeClass
  public static void init() {
    ProcessingTestUtil.init();
  }

  static void expectRecognitionException(final String id,
                                         final int expectedLine) {

    try {
      preprocess(id, res(id + ".pde"));
      fail("Expected to fail with on line " + expectedLine);
    } catch (PdePreprocessIssueException e) {
      assertNotNull(e.getIssue().getMsg());
      assertEquals(expectedLine, e.getIssue().getLine());
    } catch (Exception e) {
      if (!e.equals(e.getCause()) && e.getCause() != null)
        fail(e.getCause().toString());
      else
        fail(e.toString());
    }
  }

  static void expectRunnerException(final String id,
                                    final int expectedLine) {

    try {
      preprocess(id, res(id + ".pde"));
      fail("Expected to fail with on line " + expectedLine);
    } catch (SketchException e) {
      assertEquals(expectedLine, e.getCodeLine());
    } catch (PdePreprocessIssueException e) {
      assertNotNull(e.getIssue().getMsg());
      assertEquals(expectedLine, e.getIssue().getLine());
    } catch (Exception e) {
      if (!e.equals(e.getCause()) && e.getCause() != null)
        fail(e.getCause().toString());
      else
        fail(e.toString());
    }
  }

  static void expectCompilerException(final String id,
                                      final int expectedLine) {
    try {
      final String program = preprocess(id, res(id + ".pde"));
      final ProcessResult compilerResult = COMPILER.compile(id, program);
      if (compilerResult.succeeded()) {
        fail("Expected to fail with on line "  + expectedLine);
      }
      final String e = compilerResult.getStderr().split("\n")[0];
      final Matcher m = Pattern.compile(":(\\d+):\\s+(.+)$").matcher(e);
      m.find();
      assertEquals(String.valueOf(expectedLine), m.group(1));
    } catch (Exception e) {
      if (!e.equals(e.getCause()) && e.getCause() != null)
        fail(e.getCause().toString());
      else
        fail(e.toString());
    }
  }

  static void expectGood(final String id) {
    expectGood(id, false);
  }

  static void expectGood(final String id, boolean ignoreWhitespace) {
    try {
      final String program = preprocess(id, res(id + ".pde"));
      final ProcessResult compilerResult = COMPILER.compile(id, program);
      if (!compilerResult.succeeded()) {
        System.err.println(program);
        System.err.println("----------------------------");
        System.err.println(compilerResult.getStderr());
        fail("Compilation failed with status " + compilerResult.getResult());
      }

      final File expectedFile = res(id + ".expected");
      if (expectedFile.exists()) {
        final String expected = ProcessingTestUtil.read(expectedFile);
        if (ignoreWhitespace) {
          String expectedStrip = expected.replace("\t", "")
              .replace(" ", "")
              .replace("\n", "")
              .replace("\r", "");

          String actualStrip = program.replace("\t", "")
              .replace(" ", "")
              .replace("\n", "")
              .replace("\r", "");

          assertEquals(expectedStrip, actualStrip);
        } else {
          assertEquals(expected, program);
        }
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
        fail(e.toString());
    }
  }

  @Test
  public void bug4() {
    expectGood("bug4");
  }

  @Test
  public void bug5a() {
    expectGood("bug5a");
  }

  @Test
  public void bug5b() {
    expectGood("bug5b");
  }

  @Test
  public void bug6() {
    expectRecognitionException("bug6", 1);
  }

  @Test
  public void bug16() {
    expectRunnerException("bug16", 3);
  }

  @Test
  public void bug136() {
    expectGood("bug136", true);
  }

  @Test
  public void bug196() {
    expectRecognitionException("bug196", 5);
  }

  @Test
  public void bug281() {
    expectGood("bug281");
  }

  @Test
  public void bug481() {
    expectGood("bug481");
  }

  @Test
  public void bug507() {
    expectRecognitionException("bug507", 5);
  }

  @Test
  public void bug598() {
    expectGood("bug598");
  }

  @Test
  public void bug631() {
    expectGood("bug631");
  }

  @Test
  public void bug763() {
    expectRunnerException("bug763", 8);
  }

  @Test
  public void bug820() {
    expectCompilerException("bug820", 19);
  }

  @Test
  public void bug1064() {
    expectGood("bug1064");
  }

  @Test
  public void bug1145() {
    expectCompilerException("bug1145", 6);
  }

  @Test
  public void bug1362() {
    expectGood("bug1362");
  }

  @Test
  public void bug1390() {
    expectGood("bug1390");
  }

  @Test
  public void bug1442() {
    expectGood("bug1442");
  }

  @Test
  public void bug1511() {
    expectGood("bug1511");
  }

  @Test
  public void bug1512() {
    expectGood("bug1512");
  }

  @Test
  public void bug1514a() {
    expectGood("bug1514a");
  }

  @Test
  public void bug1514b() {
    expectGood("bug1514b");
  }

  @Test
  public void bug1515() {
    expectGood("bug1515");
  }

  @Test
  public void bug1516() {
    expectGood("bug1516");
  }

  @Test
  public void bug1517() {
    expectGood("bug1517");
  }

  @Test
  public void bug1518a() {
    expectGood("bug1518a");
  }

  @Test
  public void bug1518b() {
    expectGood("bug1518b");
  }

  @Test
  public void bug1525() {
    expectGood("bug1525");
  }

  @Test
  public void bug1532() {
    expectRecognitionException("bug1532", 46);
  }

  @Test
  public void bug1534() {
    expectGood("bug1534");
  }

  @Test
  public void bug1936() {
    expectGood("bug1936");
  }

  @Test
  public void bug315g() {
    expectGood("bug315g");
  }

  @Test
  public void bug400g() {
    expectGood("bug400g", true);
  }

  @Test
  public void bug427g() {
    expectGood("bug427g");
  }

  @Test
  public void color() {
    expectGood("color", true);
  }

  @Test
  public void annotations() {
    expectGood("annotations", true);
  }

  @Test
  public void generics() {
    expectGood("generics", true);
  }

  @Test
  public void lambda() {
    expectGood("lambdaexample", true);
  }

}
