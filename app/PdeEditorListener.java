#ifdef EDITOR

import java.awt.*;
import java.awt.event.*;


public class PdeEditorListener extends KeyAdapter implements FocusListener {
  static final String spaces = "                                                                              ";
  String tabString;
  String newline = System.getProperty("line.separator");

  boolean expandTabs;
  int tabSize;
  boolean autoIndent;

  boolean balanceParens;
  boolean balancing = false;
  TextArea tc;
  int selectionStart, selectionEnd;
  int position;


  public PdeEditorListener() {
    expandTabs = PdeApplet.getBoolean("editor.expandTabs", false);
    tabSize = PdeApplet.getInteger("editor.tabSize", 2);
    tabString = spaces.substring(0, tabSize);
    autoIndent = PdeApplet.getBoolean("editor.autoIndent", false);
    balanceParens = PdeApplet.getBoolean("editor.balanceParens", false);
  }


  public void keyPressed(KeyEvent event) {
    // only works with TextArea, because it needs 'insert'
    //TextComponent tc = (TextComponent) event.getSource();
    tc = (TextArea) event.getSource();
    deselect();
    char c = event.getKeyChar();
	
    //System.err.println((int)c);
    switch ((int) c) {
    case ')':
      if (balanceParens) {
	position = tc.getCaretPosition() + 1;
	char contents[] = tc.getText().toCharArray();
	int counter = 1; // char not in the textfield yet
	//int index = contents.length-1;
	int index = tc.getCaretPosition() - 1;
	boolean error = false;
	if (index == -1) {  // special case for first char
	  counter = 0;
	  error = true;
	}
	while (counter != 0) {
	  if (contents[index] == ')') counter++;
	  if (contents[index] == '(') counter--;
	  index--;
	  if ((index == -1) && (counter != 0)) {
	    error = true;
	    break;
	  }
	}
	if (error) {
	  //System.err.println("mismatched paren");
	  Toolkit.getDefaultToolkit().beep();
	  tc.select(0, 0);
	  tc.setCaretPosition(position);
	}
	tc.insert(")", position-1);
	event.consume();
	if (!error) {
	  selectionStart = index+1;
	  selectionEnd = index+2;
	  tc.select(selectionStart, selectionEnd);
	  balancing = true;
	}
      }
      break;

    case 9:  // expand tabs
      if (expandTabs) {
	//System.out.println("start = " + tc.getSelectionStart());
	//System.out.println("end = " + tc.getSelectionEnd());
	//System.out.println("pos = " + tc.getCaretPosition());
	tc.replaceRange(tabString, tc.getSelectionStart(),
			tc.getSelectionEnd());
	event.consume();
      }
      break;

    case 10:  // auto-indent
      if (autoIndent) {
	//System.err.println("auto indenting");
	char contents[] = tc.getText().toCharArray();
	// back up until \r \r\n or \n.. @#($* cross platform
	//index = contents.length-1;
	int index = tc.getCaretPosition() - 1;
	int spaceCount = 0;
	boolean finished = false;
	while ((index != -1) && (!finished)) {
	  if ((contents[index] == '\r') ||
	      (contents[index] == '\n')) {
	    finished = true;
	  } else {
	    spaceCount = (contents[index] == ' ') ?
	      (spaceCount + 1) : 0;
	  }
	  index--;
	}

	// !@#$@#$ MS VM doesn't move the caret position to the
	// end of an insertion after it happens, even though sun does
	String insertion = newline + spaces.substring(0, spaceCount);
	int oldCarrot = tc.getSelectionStart();
	tc.replaceRange(insertion, oldCarrot, tc.getSelectionEnd());
	// microsoft vm version:
	//tc.setCaretPosition(oldCarrot + insertion.length() - 1);
	// sun vm version:
	tc.setCaretPosition(oldCarrot + insertion.length());
	event.consume();
      }
      break;

    case 1: tc.selectAll(); break;  // control a for select all
    }
  }


  protected void deselect() {
    if (!balancing || (tc == null)) return;	
    // bounce back, otherwise will write over stuff
    if ((selectionStart == tc.getSelectionStart()) &&
	(selectionEnd == tc.getSelectionEnd()))
      tc.setCaretPosition(position);
    balancing = false;
  }


  public void focusGained(FocusEvent event) { }


  public void focusLost(FocusEvent event) {
    deselect();
  }
}


#endif
