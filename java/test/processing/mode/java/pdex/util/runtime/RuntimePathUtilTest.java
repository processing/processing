package processing.mode.java.pdex.util.runtime;

import org.junit.Test;

import java.io.File;
import java.util.List;
import java.util.StringJoiner;

import static org.junit.Assert.*;


public class RuntimePathUtilTest {

  @Test
  public void sanitizeClassPath() {
    StringJoiner testStrJoiner = new StringJoiner(File.pathSeparator);
    testStrJoiner.add("test1");
    testStrJoiner.add("");
    testStrJoiner.add("test2");

    List<String> classPath = RuntimePathUtil.sanitizeClassPath(testStrJoiner.toString());
    assertEquals(2, classPath.size());
    assertEquals("test1", classPath.get(0));
    assertEquals("test2", classPath.get(1));
  }

  @Test
  public void sanitizeClassPathNoDuplicate() {
    StringJoiner testStrJoiner = new StringJoiner(File.pathSeparator);
    testStrJoiner.add("test1");
    testStrJoiner.add("");
    testStrJoiner.add("test2");
    testStrJoiner.add("test2");

    List<String> classPath = RuntimePathUtil.sanitizeClassPath(testStrJoiner.toString());
    assertEquals(2, classPath.size());
    assertEquals("test1", classPath.get(0));
    assertEquals("test2", classPath.get(1));
  }

}