#ifdef EDITOR


import java.awt.*;
import java.awt.event.*;


public class PdeKeyListener extends KeyAdapter {
  PdeEditor editor;

  public PdeKeyListener(PdeEditor editor) {
    this.editor = editor;
  }

  public void keyPressed(KeyEvent event) {
    switch ((int) event.getKeyChar()) {
    case  2: editor.doBeautify(); break;  // control b for beautify
    case 15: editor.doOpen(); break;  // control o for open
    case 16: editor.doPrint(); break;  // control p for print
    case 18: editor.doPlay(); break;  // control r for run
    case 19: editor.doSave(); break;  // control s for save
      //case 20: editor.doSnapshot(); break;  // control t for snapshot
      // escape only works from the runpanel, because that's
      // who's getting all the key events while running
      //case 27: editor.terminate(); break;  // escape to stop	
    }
  }
}


#endif
