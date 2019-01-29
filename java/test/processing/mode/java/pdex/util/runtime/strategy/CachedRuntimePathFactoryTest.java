package processing.mode.java.pdex.util.runtime.strategy;

import org.junit.Before;
import org.junit.Test;
import processing.app.Sketch;
import processing.mode.java.JavaMode;
import processing.mode.java.pdex.ImportStatement;
import processing.mode.java.pdex.util.runtime.RuntimePathUtilTest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;


public class CachedRuntimePathFactoryTest {

  private CachedRuntimePathFactory cachedRuntimePathFactory;
  private JavaMode testMode;
  private List<ImportStatement> testImports;
  private Sketch testSketch;

  @Before
  public void setUp() throws Exception {
    cachedRuntimePathFactory = new CachedRuntimePathFactory(new RuntimePathFactoryStrategy() {

      private int calls = 0;

      @Override
      public List<String> buildClasspath(JavaMode mode, List<ImportStatement> imports,
            Sketch sketch) {

        String retVal = String.format("Test%d", calls);
        calls++;

        List<String> retList = new ArrayList<>();
        retList.add(retVal);
        return retList;
      }
    });

    testMode = RuntimePathFactoryTestUtil.createTestJavaMode();
    testImports = RuntimePathFactoryTestUtil.createTestImports();
    testSketch = RuntimePathFactoryTestUtil.createTestSketch();
  }

  @Test
  public void buildClasspath() {
    List<String> classpath = cachedRuntimePathFactory.buildClasspath(
        testMode,
        testImports,
        testSketch
    );

    assertEquals(1, classpath.size());
    assertEquals("Test0", classpath.get(0));
  }

  @Test
  public void invalidateCache() {
    cachedRuntimePathFactory.buildClasspath(
        testMode,
        testImports,
        testSketch
    );

    cachedRuntimePathFactory.invalidateCache();

    List<String> classpath = cachedRuntimePathFactory.buildClasspath(
        testMode,
        testImports,
        testSketch
    );

    assertEquals(1, classpath.size());
    assertEquals("Test1", classpath.get(0));
  }

}