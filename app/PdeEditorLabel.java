import java.awt.*;


public class PdeEditorLabel extends Label {
  int rows;

  public PdeEditorLabel(int rows) {
    super(""); //pde editor label");
    setRows(rows);
  }

  public void setRows(int rows) {
    this.rows = rows;
  }

  public Dimension getPreferredSize() {
    return new Dimension(200, rows * PdeEditorButtons.BUTTON_HEIGHT);
  }
}
