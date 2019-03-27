package test.processing.mode.java;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import processing.app.Base;
import processing.app.Platform;
import processing.app.exec.ProcessHelper;
import processing.app.exec.ProcessResult;

/**
 * Utility class for compiling single compilationUnits.
 *
 * @author Jonathan Feinberg &lt;jdf@pobox.com&gt;
 *
 */
class UTCompiler {
  private final String classpath;

  UTCompiler(File... classpath) throws IOException {
    final StringBuilder sb = new StringBuilder();
    for (final File f : classpath) {
      if (sb.length() > 0)
        sb.append(File.pathSeparatorChar);
      sb.append(f.getAbsolutePath());
    }
    this.classpath = sb.toString();

    final String javaHomeProp = System.getProperty("java.home");
    if (javaHomeProp == null) {
      throw new RuntimeException(
                                 "I don't know how to deal with a null java.home proprty, to be quite frank.");
    }
    final File javaHome = new File(javaHomeProp).getCanonicalFile();
    Platform.setenv("JAVA_HOME", javaHome.getCanonicalPath());

    final String path = new File(javaHome, "bin").getCanonicalPath()
        + File.pathSeparator + Platform.getenv("PATH");

    Platform.setenv("PATH", path);
  }

  ProcessResult compile(final String name, final String program)
      throws IOException {
    final File tmpdir = File.createTempFile("utcompiler", ".tmp");
    if (!tmpdir.delete())
      throw new IOException("Cannot delete " + tmpdir);
    if (!tmpdir.mkdir())
      throw new IOException("Cannot create " + tmpdir);
    final File javaFile = new File(tmpdir, name + ".java");
    final FileWriter java = new FileWriter(javaFile);
    try {
      java.write(program);
    } finally {
      java.close();
    }
    try {
      return new ProcessHelper("javac",
                               "-sourcepath", tmpdir.getAbsolutePath(),
                               "-cp", classpath,
                               "-nowarn",
                               "-d", tmpdir.getAbsolutePath(),
                               javaFile.getAbsolutePath()).execute();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    } finally {
      for (final File f: tmpdir.listFiles())
        if (!f.getName().startsWith("."))if (!f.delete())
          throw new IOException("Can't delete " + f);
      if (!tmpdir.delete())
        throw new IOException("Can't delete " + tmpdir);
    }
  }
}
