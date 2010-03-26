package processing.app.tools;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import processing.app.Base;
import processing.app.Editor;
import processing.util.exec.ProcessHelper;
import processing.util.exec.ProcessResult;

public class Uncrustify implements Tool {

  private Editor editor;

  public String getMenuTitle() {
    return "Uncrustify (experimental)";
  }

  public void init(Editor editor) {
    this.editor = editor;
  }

  public void run() {
    try {
      final File lib = Base.getContentFile("lib");
      final File uncrustify = new File(lib, "uncrustify");
      final File config = new File(lib, "uncrustify-pde.cfg");
      final File tmp = File.createTempFile("uncrustify", ".pde");
      try {
        final FileWriter out = new FileWriter(tmp);
        try {
          out.write(editor.getText());
        } finally {
          out.close();
        }
        final ProcessResult result = new ProcessHelper(uncrustify
            .getAbsolutePath(), "-c", config.getAbsolutePath(), "-l", "JAVA",
                                                       "-f", tmp
                                                           .getAbsolutePath())
            .execute();
        if (!result.succeeded()) {
          Base.showMessage("Could not Uncrustify", result.getStderr());
          return;
        }
        editor.setText(result.getStdout());
      } catch (InterruptedException e) {
        Base.showWarning("Could not Uncrustify", "Unexpected exception", e);
      } finally {
        if (!tmp.delete()) {
          tmp.deleteOnExit();
        }
      }
    } catch (IOException e) {
      Base.showWarning("Could not Uncrustify", "Unexpected exception", e);
    }
  }

}
