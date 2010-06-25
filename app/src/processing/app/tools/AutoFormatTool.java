package processing.app.tools;

import processing.app.Editor;
import processing.app.format.AutoFormat;

public class AutoFormatTool implements Tool {
  private Editor editor;

  public void init(final Editor editor) {
    this.editor = editor;
  }

  public String getMenuTitle() {
    return "Auto Format";
  }

  public void run() {
    final String source = editor.getText();

    try {
      final AutoFormat formatter = new AutoFormat();
      final String formattedText = formatter.format(source);
      // save current (rough) selection point
      int selectionEnd = editor.getSelectionStop();

      // make sure the caret would be past the end of the text
      if (formattedText.length() < selectionEnd - 1) {
        selectionEnd = formattedText.length() - 1;
      }

      if (formattedText.equals(source)) {
        editor.statusNotice("No changes necessary for Auto Format.");
      } else {
        // replace with new bootiful text
        // selectionEnd hopefully at least in the neighborhood
        editor.setText(formattedText);
        editor.setSelection(selectionEnd, selectionEnd);
        editor.getSketch().setModified(true);
        // mark as finished
        editor.statusNotice("Auto Format finished.");
      }

    } catch (final Exception e) {
      editor.statusError(e);
    }
  }

}
