package test.processing.mode.java;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static test.processing.mode.java.ProcessingTestUtil.COMPILER;
import static test.processing.mode.java.ProcessingTestUtil.preprocess;
import static test.processing.mode.java.ProcessingTestUtil.res;

import java.io.File;
import java.io.FileWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.BeforeClass;
import org.junit.Test;

import processing.app.SketchException;
import processing.app.exec.ProcessResult;
import org.antlr.v4.runtime.RecognitionException;

public class ParserTests {

  @BeforeClass
  public static void init() {
    ProcessingTestUtil.init();
  }

  static void expectRecognitionException(final String id,
                                         final String expectedMessage,
                                         final int expectedLine) {
    fail("recognition exception testing not yet implemented");
    
// TODO: fix error testing
//    try {
//      preprocess(id, res(id + ".pde"));
//      fail("Expected to fail with \"" + expectedMessage + "\" on line "
//          + expectedLine);
//    } catch (RecognitionException e) {
//      assertEquals(expectedMessage, e.getMessage());
//      assertEquals(expectedLine, e.getLine());
//    } catch (Exception e) {
//      if (!e.equals(e.getCause()) && e.getCause() != null)
//        fail(e.getCause().toString());
//      else
//        fail(e.toString());
//    }
  }

  static void expectRunnerException(final String id,
                                    final String expectedMessage,
                                    final int expectedLine) {
    fail("runner exception testing not yet implemented");
    
// TODO: fix error testing
//    try {
//      preprocess(id, res(id + ".pde"));
//      fail("Expected to fail with \"" + expectedMessage + "\" on line "
//          + expectedLine);
//    } catch (SketchException e) {
//      assertEquals(expectedMessage, e.getMessage());
//      assertEquals(expectedLine, e.getCodeLine());
//    } catch (Exception e) {
//      if (!e.equals(e.getCause()) && e.getCause() != null)
//        fail(e.getCause().toString());
//      else
//        fail(e.toString());
//    }
  }

  static void expectCompilerException(final String id,
                                      final String expectedMessage,
                                      final int expectedLine) {
    try {
      final String program = ProcessingTestUtil
          .preprocess(id, res(id + ".pde"));
      final ProcessResult compilerResult = COMPILER.compile(id, program);
      if (compilerResult.succeeded()) {
        fail("Expected to fail with \"" + expectedMessage + "\" on line "
            + expectedLine);
      }
      final String e = compilerResult.getStderr().split("\n")[0];
      final Matcher m = Pattern.compile(":(\\d+):\\s+(.+)$").matcher(e);
      m.find();
      assertEquals(expectedMessage, m.group(2));
      assertEquals(String.valueOf(expectedLine), m.group(1));
    } catch (Exception e) {
      if (!e.equals(e.getCause()) && e.getCause() != null)
        fail(e.getCause().toString());
      else
        fail(e.toString());
    }
  }

  static void expectGood(final String id) {
    try {
      final String program = ProcessingTestUtil
          .preprocess(id, res(id + ".pde"));
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
    expectRecognitionException("bug6", "expecting EOF, found '/'", 1);
  }

  @Test
  public void bug16() {
    expectRunnerException("bug16", "Unclosed /* comment */", 2);
  }

  @Test
  public void bug136() {
    expectGood("bug136");
  }

  @Test
  public void bug196() {
    expectRecognitionException("bug196",
      "Web colors must be exactly 6 hex digits. This looks like 5.", 4);
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
    expectRecognitionException("bug507", "expecting EOF, found 'else'", 5);
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
    expectRunnerException("bug763", "Unterminated string constant", 6);
  }

  @Test
  public void bug820() {
    expectCompilerException("bug820", "error: variable x1 is already defined in method setup()", 19);
  }

  @Test
  public void bug1064() {
    expectGood("bug1064");
  }

  @Test
  public void bug1145() {
    expectCompilerException("bug1145", "error: '.' expected", 6);
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
  public void bug1519() {
    expectRecognitionException("bug1519", "Maybe too many > characters?", 7);
  }

  @Test
  public void bug1525() {
    expectGood("bug1525");
  }

  @Test
  public void bug1532() {
    expectRecognitionException("bug1532", "unexpected token: break", 50);
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
    expectGood("bug400g");
  }
  
  @Test
  public void bug427g() {
    expectGood("bug427g");
  }

  @Test
  public void annotations() {
    expectGood("annotations");
  }
}
