import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.text.*;
import javax.swing.event.*;


public class PdeEditorListener extends KeyAdapter /*implements FocusListener*/ {
  static final String spaces = "                                                                                        ";
  //String newline = System.getProperty("line.separator");

  PdeEditor editor;

  boolean expandTabs;
  int tabSize;
  String tabString;

  boolean autoIndent;

  //  boolean balanceParens;
  //  boolean balancing = false;

  //boolean fakeArrowKeys;

  //TextArea tc;
  //JTextPane tc;
  int selectionStart, selectionEnd;
  int position;


  public PdeEditorListener(PdeEditor editor) {
    this.editor = editor;

    //System.out.println("initing PdeEditorListener " + editor);

    expandTabs = PdeBase.getBoolean("editor.expand_tabs", true);
    tabSize = PdeBase.getInteger("editor.tab_size", 2);
    tabString = spaces.substring(0, tabSize);
    autoIndent = PdeBase.getBoolean("editor.auto_indent", true);
    //    balanceParens = PdeBase.getBoolean("editor.balance_parens", false);
    //fakeArrowKeys = PdeBase.getBoolean("editor.fake_arrow_keys", 
    //				       PdeBase.platform == PdeBase.MACOSX);
  }


  public void keyPressed(KeyEvent event) {
    // don't do things if the textarea isn't editable
    if (editor.externalEditor) return;

    //System.out.println("blah blah");
    //System.out.println("source of event is " + event.getSource());

    // only works with TextArea, because it needs 'insert'
    //TextComponent tc = (TextComponent) event.getSource();
    //tc = (TextArea) event.getSource();

    JTextPane tc = (JTextPane) event.getSource();
    // getSource() returns PdeEditorTextPane, which is a JTextPane
    //tc = editor.textarea;

    //deselect();  // this is for paren balancing
    char c = event.getKeyChar();
    int code = event.getKeyCode();
    //System.out.println(event);

    if (!editor.sketchModified) {
      if ((code == KeyEvent.VK_BACK_SPACE) || (code == KeyEvent.VK_TAB) || 
	  (code == KeyEvent.VK_ENTER) || ((c >= 32) && (c < 128))) {
	//editor.sketchModified = true;
	editor.setSketchModified(true);
      }
    }

/*
    if (fakeArrowKeys && (c == 65535) && 
	(code == KeyEvent.VK_LEFT || code == KeyEvent.VK_RIGHT || 
	 code == KeyEvent.VK_UP || code == KeyEvent.VK_DOWN)) {
      position = tc.getCaretPosition();
      switch (code) {

      case KeyEvent.VK_LEFT:
	tc.setCaretPosition(Math.max(0, position - 1));
	break;

      case KeyEvent.VK_RIGHT:
	tc.setCaretPosition(position + 1);
	break;

      case KeyEvent.VK_UP:
	char contents[] = tc.getText().toCharArray();

	// figure out how far left to the newline
	int howfar = 0;

	// an improved version might use this..
	// it worked for DOWN, but i'd mostly fixed UP already
	//if (contents[position] == 10) position--;

	int p = position;
	//System.out.println("position = " + p);
	while ((p > 0) && (contents[p] != 10)) {
	  p--; howfar++;
	}
	// step over the newline
	p--;
	if (p <= 0) {
	  tc.setCaretPosition(0);
	  return;
	}

	//if (contents[position] == 10) {
	if (contents[p] == 10) {
	  //System.out.println("double newlines at current position");
	  //}
	  p--;
	} else {
	  howfar--;
	}
	//System.out.println("dist from left = " + howfar);

	if (p == 0) return; // nothing above
	//System.out.println("now backing up from char " + contents[p]);

	// determine length of previous line
	int howlong = 0;
	while ((p >= 0) && (contents[p] != 10)) {
	  p--; howlong++;
	}
	//System.out.println("moving to line of length " + howlong);

	tc.setCaretPosition(p + Math.min(howfar, howlong) + 1);
	//System.out.println();
	break;

      case KeyEvent.VK_DOWN:
	contents = tc.getText().toCharArray();
	// figure out how far left to the newline
	howfar = 0;
	p = position;
	if (contents[p] == 10) {
	  p--;
	  position--;
	}
	while ((p > 0) && (contents[p] != 10)) {
	  p--; howfar++;
	}
	//System.out.println("howfar = " + howfar);
	// step forward and find the next newline
	p = position;

	// if at end of line, this is an eol, step over it
	//if (contents[p] == 10) p++;

	int last = contents.length - 1;
	//System.out.println("howfar = " + howfar);
	//int howlong = 0;
	while ((p < last) && (contents[p] != 10)) 
	  p++;
	if (p == last) return; // nothing below
	while ((p < last) && (contents[p] == 10))
	  p++;
	int newline = p;

	// see if enough room on this line
	while ((p < last) && (p < newline + howfar-1) &&
	       (contents[p] != 10)) p++;
	tc.setCaretPosition(p);
	break;
      }
    } 
*/

    //System.err.println((int)c);
    switch ((int) c) {
/*
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
*/

    case 9:  // expand tabs
      if (expandTabs) {
	//System.out.println("start = " + tc.getSelectionStart());
	//System.out.println("end = " + tc.getSelectionEnd());
	//System.out.println("pos = " + tc.getCaretPosition());
	//	tc.replaceRange(tabString, tc.getSelectionStart(),
	//			tc.getSelectionEnd());
	tc.replaceSelection(tabString);
	event.consume();
      }
      break;

    case 10:  // auto-indent
    case 13:
      if (autoIndent) {
	//System.err.println("auto indenting");

	//System.out.println("caret, sel = " + tc.getCaretPosition() + ", " + 
	//	   tc.getSelectionStart());

	char contents[] = tc.getText().toCharArray();
	//	for (int j = 0; j < Math.min(contents.length, 200); j++) {
	//	  System.out.println(j + " " + ((char)contents[j]) + " " + 
	//			   ((int)contents[j]) + "     ");
	//	}
	//	System.out.println();

	// this is the position of the caret, if the textarea
	// only used a single kind of line ending
	int origIndex = tc.getCaretPosition() - 1;

	//for (int i = 0; i < contents.length-1; i++) {
	//if ((contents[i] == 13) && (contents[i+1] == 10)) offset++;
	//}

	// walk through the array to the current caret position,
	// and count how many weirdo windows line endings there are,
	// which would be throwing off the caret position number
	int offset = 0;
	int realIndex = origIndex;
	for (int i = 0; i < realIndex-1; i++) {
	  if ((contents[i] == 13) && (contents[i+1] == 10)) {
	    offset++;
	    realIndex++;
	  }
	}

	// back up until \r \r\n or \n.. @#($* cross platform

	//System.out.println(origIndex + " offset = " + offset);
	origIndex += offset; // ARGH!#(* WINDOWS#@($*

	// if hitting enter on a line that is followed by spaces 
	// or blank lines, it seems that the caret position will be
	// just after the newline
	//if (PdeBase.platform == PdeBase.WINDOWS) {
	  //if (contents[origIndex] == 13) origIndex--;
	//}

	int index = origIndex;
	int spaceCount = 0;
	boolean finished = false;
	while ((index != -1) && (!finished)) {
	  if ((contents[index] == 10) ||
	      (contents[index] == 13)) {
	    finished = true;
	    index++; // maybe ?
	    //	  } else {
	    //	    spaceCount = (contents[index] == ' ') ?
	    //	      (spaceCount + 1) : 0;
	  } else {
	    //System.out.println("'" + (char)contents[index] + "'");
	    index--;  // new
	  }
	}
	//System.out.println("index is " + index);
	while ((index < contents.length) && (index >= 0) &&
	       (contents[index++] == ' ')) {
	  spaceCount++;
	}
	//System.out.println("spaceCount is " + spaceCount);

	// !@#$@#$ MS VM doesn't move the caret position to the
	// end of an insertion after it happens, even though sun does
	//String insertion = newline + spaces.substring(0, spaceCount);

	// seems that \r is being inserted anyway
	// so no need to insert the platform's line separator
	String insertion = "\n" + spaces.substring(0, spaceCount);
	//	int oldCarrot = tc.getSelectionStart();
	//	tc.replaceRange(insertion, oldCarrot, tc.getSelectionEnd());
	//	System.out.println("replacing with '" + insertion + "'");
	tc.replaceSelection(insertion);
	// microsoft vm version:
	//tc.setCaretPosition(oldCarrot + insertion.length() - 1);
	// sun vm version:
	//	tc.setCaretPosition(oldCarrot + insertion.length());
	event.consume();
      }
      break;

      //case 1: tc.selectAll(); break;  // control a for select all
    }
  }

  /*
    // for balancing parens
  protected void deselect() {
    //if (!balancing || (tc == null)) return;	
    // bounce back, otherwise will write over stuff
    if ((selectionStart == tc.getSelectionStart()) &&
	(selectionEnd == tc.getSelectionEnd()))
      tc.setCaretPosition(position);
    //balancing = false;
  }


  public void focusGained(FocusEvent event) { }


  public void focusLost(FocusEvent event) {
    deselect();
  }
  */
}
