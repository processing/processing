/**
 * Modified version of Stephen Ostermiller's syntax highlighting
 * demo, taken from http://ostermiller.org/syntax/editor.html
 * 
 * original message:
 *
 * A simple text editor that demonstrates the integration of the 
 * com.Ostermiller.Syntax Syntax Highlighting package with a text editor.
 * Copyright (C) 2001 Stephen Ostermiller <syntax@ostermiller.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * See COPYING.TXT for details.
 */

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.*;
import javax.swing.event.*;

import java.io.*;
import java.util.*;


public class PdeEditorTextPane extends JPanel {
  protected JTextPane textPane;

  // styled document that is the model for the textPane
  protected HighLightedDocument document;

  // a reader to be used to feed the doc to the lexer
  protected DocumentReader documentReader;

  // lexer determines coloring
  protected Lexer syntaxLexer;

  // thread that handles the actual coloring.
  protected Colorer colorer;

  // A lock for modifying the document, or for actions
  // that depend on the document not being modified.
  private Object doclock = new Object();


  public PdeEditorTextPane() {
    // initial set up that sets the title
    //super("Programmer's Editor Demonstration");

    // Create the document model.
    document = new HighLightedDocument();

    // Create the text pane and configure it.
    textPane = new JTextPane(document);
    textPane.setCaretPosition(0);
    //textPane.setMargin(new Insets(5,5,5,5));
    JScrollPane scrollPane = new JScrollPane(textPane);

    // specify the initial size and location for the window.
    //scrollPane.setPreferredSize(new Dimension(620, 460));
    //setLocation(50, 50);

    // Add the components to the frame.
    //JPanel contentPane = new JPanel(new BorderLayout());
    //contentPane.add(scrollPane, BorderLayout.CENTER);
    //setContentPane(contentPane);
    add(scrollPane, BorderLayout.CENTER);

    // Set up the menu bar.
    //    JMenu styleMenu = createStyleMenu();
    //    JMenuBar mb = new JMenuBar();
    //    mb.add(styleMenu);
    //    setJMenuBar(mb);

    // Make the window so that it can close the application
    //    addWindowListener(new WindowAdapter() {
    //	public void windowClosing(WindowEvent e) {
    //	  System.exit(0);
    //	}
    //	public void windowActivated(WindowEvent e) {
    //	  // focus magic
    //	  textPane.requestFocus();
    //	}
    //      });

    // Start the thread that does the coloring
    colorer = new Colorer();
    colorer.start();

    // Set up the hash table that contains the styles.
    //initStyles();

    SimpleAttributeSet dstyle = new SimpleAttributeSet();
    StyleConstants.setFontFamily(dstyle, "Monospaced");
    StyleConstants.setFontSize(dstyle, 12);
    StyleConstants.setBackground(dstyle, Color.white);
    StyleConstants.setForeground(dstyle, Color.black);
    StyleConstants.setBold(dstyle, false);
    StyleConstants.setItalic(dstyle, false);
    //styles.put("body", style);

    styles.put("body",         PdeBase.getStyle("body"), dstyle);
    styles.put("tag",          PdeBase.getStyle("tag"), dstyle);
    styles.put("endtag",       PdeBase.getStyle("tag"), dstyle); // same
    styles.put("reference",    PdeBase.getStyle("reference"), dstyle);
    styles.put("name",         PdeBase.getStyle("name"), dstyle);
    styles.put("value",        PdeBase.getStyle("value"), dstyle);
    styles.put("text",         PdeBase.getStyle("text"), dstyle);
    styles.put("reservedWord", PdeBase.getStyle("reserved_word"), dstyle);
    styles.put("identifier",   PdeBase.getStyle("identifier"), dstyle);
    styles.put("literal",      PdeBase.getStyle("literal"), dstyle);
    styles.put("separator",    PdeBase.getStyle("separator"), dstyle);
    styles.put("operator",     PdeBase.getStyle("operator"), dstyle);
    styles.put("comment",      PdeBase.getStyle("comment"), dstyle);
    styles.put("preprocessor", PdeBase.getStyle("preprocessor"), dstyle);
    styles.put("whitespace",   PdeBase.getStyle("whitespace"), dstyle);
    styles.put("error",        PdeBase.getStyle("error"), dstyle);
    styles.put("unknown",      PdeBase.getStyle("unknown"), dstyle);

    // create the new document.
    documentReader = new DocumentReader(document);

    // Put the initial text into the text pane and
    // set it's initial coloring style.
    //    initDocument();
    String initString = 
      ("/**\n" +
       " * Simple common test program.\n" +
       " */\n" +
       "public class HelloWorld {\n" +
       "    public static void main(String[] args) {\n" +
       "        // Display the greeting.\n" +
       "        System.out.println(\"Hello World!\");\n" +
       "    }\n" +
       "}\n");

    syntaxLexer = new JavaLexer(documentReader);

    try {
      document.insertString(document.getLength(), 
			    initString, 
			    getStyle("text"));
    } catch (BadLocationException ble) {
      System.err.println("Couldn't insert initial text.");
    }

    // put it all together and show it.
    //pack();
    //setVisible(true);
  }


  /**
   * Run the Syntax Highlighting as a separate thread.
   * Things that need to be colored are messaged to the
   * thread and put in a list.
   */
  private class Colorer extends Thread {
    /**
     * Keep a list of places in the file that it is safe to restart the
     * highlighting.  This happens whenever the lexer reports that it has
     * returned to its initial state.  Since this list needs to be sorted
     * and we need to be able to retrieve ranges from it, it is stored in a
     * balanced tree.
     */
    private TreeSet iniPositions = new TreeSet(new DocPositionComparator());

    /**
     * As we go through and remove invalid positions we will also be finding
     * new valid positions. 
     * Since the position list cannot be deleted from and written to at the same
     * time, we will keep a list of the new positions and simply add it to the
     * list of positions once all the old positions have been removed.
     */
    private HashSet newPositions = new HashSet();

    /**
     * A simple wrapper representing something that needs to be colored.
     * Placed into an object so that it can be stored in a Vector.
     */
    private class RecolorEvent {
      public int position;
      public int adjustment;
      public RecolorEvent(int position, int adjustment){
	this.position = position;
	this.adjustment = adjustment;
      }
    }

    /**
     * Vector that stores the communication between the two threads.
     */
    private volatile Vector v = new Vector();

    /**
     * The amount of change that has occurred before the place in the
     * document that we are currently highlighting (lastPosition).
     */
    private volatile int change = 0;

    /**
     * The last position colored
     */
    private volatile int lastPosition = -1;
        
    private volatile boolean asleep = false;

    /**
     * When accessing the vector, we need to create a critical section.
     * we will synchronize on this object to ensure that we don't get
     * unsafe thread behavior.
     */
    private Object lock = new Object();

    /**
     * Tell the Syntax Highlighting thread to take another look at this
     * section of the document.  It will process this as a FIFO.
     * This method should be done inside a doclock.
     */
    public void color(int position, int adjustment){
      // figure out if this adjustment effects the current run.
      // if it does, then adjust the place in the document
      // that gets highlighted.
      if (position < lastPosition){
	if (lastPosition < position - adjustment){
	  change -= lastPosition - position;
	} else {
	  change += adjustment;
	}
      }
      synchronized(lock){
	v.add(new RecolorEvent(position, adjustment));
	if (asleep){
	  this.interrupt();
	}
      }
    }

    /**
     * The colorer runs forever and may sleep for long
     * periods of time.  It should be interrupted every
     * time there is something for it to do.
     */
    public void run() {
      int position = -1;
      int adjustment = 0;
      // if we just finish, we can't go to sleep until we
      // ensure there is nothing else for us to do.
      // use try again to keep track of this.
      boolean tryAgain = false;
      for (;;){  // forever
	synchronized(lock){
	  if (v.size() > 0){
	    RecolorEvent re = (RecolorEvent)(v.elementAt(0));
	    v.removeElementAt(0);
	    position = re.position;
	    adjustment = re.adjustment;
	  } else {
	    tryAgain = false;
	    position = -1;
	    adjustment = 0;
	  }
	}
	if (position != -1){
	  SortedSet workingSet;
	  Iterator workingIt;
	  DocPosition startRequest = new DocPosition(position);
	  DocPosition endRequest = new DocPosition(position + ((adjustment>=0)?adjustment:-adjustment));
	  DocPosition  dp;
	  DocPosition dpStart = null;
	  DocPosition dpEnd = null;

	  // find the starting position.  We must start at least one
	  // token before the current position
	  try {
	    // all the good positions before
	    workingSet = iniPositions.headSet(startRequest);
	    // the last of the stuff before
	    dpStart = ((DocPosition)workingSet.last());
	  } catch (NoSuchElementException x){
	    // if there were no good positions before the requested start,
	    // we can always start at the very beginning.
	    dpStart = new DocPosition(0);
	  }

	  // if stuff was removed, take any removed positions off the list.
	  if (adjustment < 0){
	    workingSet = iniPositions.subSet(startRequest, endRequest);
	    workingIt = workingSet.iterator();
	    while (workingIt.hasNext()){
	      workingIt.next();
	      workingIt.remove();
	    }
	  }

	  // adjust the positions of everything after the insertion/removal.
	  workingSet = iniPositions.tailSet(startRequest);
	  workingIt = workingSet.iterator();
	  while (workingIt.hasNext()){
	    ((DocPosition)workingIt.next()).adjustPosition(adjustment);
	  }

	  // now go through and highlight as much as needed
	  workingSet = iniPositions.tailSet(dpStart);
	  workingIt = workingSet.iterator();
	  dp = null;
	  if (workingIt.hasNext()){
	    dp = (DocPosition)workingIt.next();
	  }
	  try {
	    Token t;
	    boolean done = false;
	    dpEnd = dpStart;
	    synchronized (doclock){
	      // we are playing some games with the lexer for efficiency.
	      // we could just create a new lexer each time here, but instead,
	      // we will just reset it so that it thinks it is starting at the
	      // beginning of the document but reporting a funny start position.
	      // Reseting the lexer causes the close() method on the reader
	      // to be called but because the close() method has no effect on the
	      // DocumentReader, we can do this.
	      syntaxLexer.reset(documentReader, 0, dpStart.getPosition(), 0);
	      // After the lexer has been set up, scroll the reader so that it
	      // is in the correct spot as well.
	      documentReader.seek(dpStart.getPosition());
	      // we will highlight tokens until we reach a good stopping place.
	      // the first obvious stopping place is the end of the document.
	      // the lexer will return null at the end of the document and wee
	      // need to stop there.
	      t = syntaxLexer.getNextToken();
	    }
	    newPositions.add(dpStart);
	    while (!done && t != null){
	      // this is the actual command that colors the stuff.
	      // Color stuff with the description of the style matched
	      // to the hash table that has been set up ahead of time.
	      synchronized (doclock){
		if (t.getCharEnd() <= document.getLength()){
		  document.setCharacterAttributes(
						  t.getCharBegin() + change,
						  t.getCharEnd()-t.getCharBegin(),
						  getStyle(t.getDescription()),
						  true
						  );                                                                  
		  // record the position of the last bit of text that we colored
		  dpEnd = new DocPosition(t.getCharEnd());
		}
		lastPosition = (t.getCharEnd() + change);
	      }
	      // The other more complicated reason for doing no more highlighting
	      // is that all the colors are the same from here on out anyway.
	      // We can detect this by seeing if the place that the lexer returned
	      // to the initial state last time we highlighted is the same as the
	      // place that returned to the initial state this time.
	      // As long as that place is after the last changed text, everything
	      // from there on is fine already.
	      if (t.getState() == Token.INITIAL_STATE){
                                //System.out.println(t);
                                // look at all the positions from last time that are less than or
                                // equal to the current position
		while (dp != null && dp.getPosition() <= t.getCharEnd()){
		  if (dp.getPosition() == t.getCharEnd() && dp.getPosition() >= endRequest.getPosition()){
		    // we have found a state that is the same
		    done = true;
		    dp = null;
		  } else if (workingIt.hasNext()){
		    // didn't find it, try again.
		    dp = (DocPosition)workingIt.next();
		  } else {
		    // didn't find it, and there is no more info from last
		    // time.  This means that we will just continue
		    // until the end of the document.
		    dp = null;
		  }
		}
                                // so that we can do this check next time, record all the
                                // initial states from this time.
		newPositions.add(dpEnd);
	      }
	      synchronized (doclock){
		t = syntaxLexer.getNextToken();
	      }
	    }

	    // remove all the old initial positions from the place where
	    // we started doing the highlighting right up through the last
	    // bit of text we touched.
	    workingIt = iniPositions.subSet(dpStart, dpEnd).iterator();
	    while (workingIt.hasNext()){
	      workingIt.next();
	      workingIt.remove();
	    }
                        
	    // Remove all the positions that are after the end of the file.:
	    workingIt = iniPositions.tailSet(new DocPosition(document.getLength())).iterator();
	    while (workingIt.hasNext()){
	      workingIt.next();
	      workingIt.remove();
	    }

	    // and put the new initial positions that we have found on the list.
	    iniPositions.addAll(newPositions);
	    newPositions.clear();
                        
	    /*workingIt = iniPositions.iterator();
	      while (workingIt.hasNext()){
	      System.out.println(workingIt.next());
	      }
                        
	      System.out.println("Started: " + dpStart.getPosition() + " Ended: " + dpEnd.getPosition());*/
	  } catch (IOException x){
	  }
	  synchronized (doclock){
	    lastPosition = -1;
	    change = 0;
	  }
	  // since we did something, we should check that there is
	  // nothing else to do before going back to sleep.
	  tryAgain = true;
	}                
	asleep = true;
	if (!tryAgain){
	  try {
	    sleep (0xffffff);
	  } catch (InterruptedException x){
	  }
                    
	}
	asleep = false;
      }
    }
  }

  /**
   * Color or recolor the entire document
   */
  public void colorAll(){
    color(0, document.getLength());
  }

  /**
   * Color a section of the document.
   * The actual coloring will start somewhere before
   * the requested position and continue as long
   * as needed.
   *
   * @param position the starting point for the coloring.
   * @param adjustment amount of text inserted or removed
   *    at the starting point.
   */
  public void color(int position, int adjustment){
    colorer.color(position, adjustment);
  }

  /**
   * Create the style menu.
   *
   * @return the style menu.
   */
  /*
  private JMenu createStyleMenu() {
    ActionListener actionListener = new ActionListener(){
	public void actionPerformed(ActionEvent e){
	  if (e.getActionCommand().equals("Java")){
	    syntaxLexer = new JavaLexer(documentReader);
	    colorAll();
	  } else if (e.getActionCommand().equals("C/C++")){
	    syntaxLexer = new CLexer(documentReader);
	    colorAll();
	  } else if (e.getActionCommand().equals("LaTeX")){
	    syntaxLexer = new LatexLexer(documentReader);
	    colorAll();
	  } else if (e.getActionCommand().equals("SQL")){
	    syntaxLexer = new SQLLexer(documentReader);
	    colorAll();
	  } else if (e.getActionCommand().equals("Java Properties")){
	    syntaxLexer = new PropertiesLexer(documentReader);
	    colorAll();
	  } else if (e.getActionCommand().equals("HTML (Simple)")){
	    syntaxLexer = new HTMLLexer(documentReader);
	    colorAll();
	  } else if (e.getActionCommand().equals("HTML (Complex)")){
	    syntaxLexer = new HTMLLexer1(documentReader);
	    colorAll();
	  } else if (e.getActionCommand().equals("Gray Out")){
	    SimpleAttributeSet style = new SimpleAttributeSet();
	    StyleConstants.setFontFamily(style, "Monospaced");
	    StyleConstants.setFontSize(style, 12);
	    StyleConstants.setBackground(style, Color.gray);
	    StyleConstants.setForeground(style, Color.black);
	    StyleConstants.setBold(style, false);
	    StyleConstants.setItalic(style, false);
	    document.setCharacterAttributes(0, document.getLength(), style, true);
	  }                
	}
      };

    JMenu menu = new JMenu("Style");
    ButtonGroup group = new ButtonGroup();
    JRadioButtonMenuItem item;
    item = new JRadioButtonMenuItem("Java", true);
    group.add(item);
    item.addActionListener(actionListener);
    menu.add(item);
    item = new JRadioButtonMenuItem("C/C++", false);
    group.add(item);
    item.addActionListener(actionListener);
    menu.add(item);
    item = new JRadioButtonMenuItem("HTML (Simple)", false);
    group.add(item);
    item.addActionListener(actionListener);
    menu.add(item);
    item = new JRadioButtonMenuItem("HTML (Complex)", false);
    group.add(item);
    item.addActionListener(actionListener);
    menu.add(item);
    item = new JRadioButtonMenuItem("LaTeX", false);
    group.add(item);
    item.addActionListener(actionListener);
    menu.add(item);
    item = new JRadioButtonMenuItem("SQL", false);
    group.add(item);
    item.addActionListener(actionListener);
    menu.add(item);
    item = new JRadioButtonMenuItem("Java Properties", false);
    group.add(item);
    item.addActionListener(actionListener);
    menu.add(item);
        
    JMenuItem i = new JMenuItem("Gray Out");
    i.addActionListener(actionListener);
    menu.add(i);

    return menu;
  }
  */

  /**
   * A hash table containing the text styles.
   * Simple attribute sets are hashed by name (String)
   */
  private Hashtable styles = new Hashtable();

  /**
   * retrieve the style for the given type of text.
   *
   * @param styleName the label for the type of text ("tag" for example) 
   *      or null if the styleName is not known.
   * @return the style
   */
  private SimpleAttributeSet getStyle(String styleName){
    return ((SimpleAttributeSet)styles.get(styleName));
  }

  /**
   * Create the styles and place them in the hash table.
   */
  /*
  private void initStyles(){
    SimpleAttributeSet style;

    style = new SimpleAttributeSet();
    StyleConstants.setFontFamily(style, "Monospaced");
    StyleConstants.setFontSize(style, 12);
    StyleConstants.setBackground(style, Color.white);
    StyleConstants.setForeground(style, Color.black);
    StyleConstants.setBold(style, false);
    StyleConstants.setItalic(style, false);
    styles.put("body", style);

    style = new SimpleAttributeSet();
    StyleConstants.setFontFamily(style, "Monospaced");
    StyleConstants.setFontSize(style, 12);
    StyleConstants.setBackground(style, Color.white);
    StyleConstants.setForeground(style, Color.blue);
    StyleConstants.setBold(style, true);
    StyleConstants.setItalic(style, false);
    styles.put("tag", style);

    style = new SimpleAttributeSet();
    StyleConstants.setFontFamily(style, "Monospaced");
    StyleConstants.setFontSize(style, 12);
    StyleConstants.setBackground(style, Color.white);
    StyleConstants.setForeground(style, Color.blue);
    StyleConstants.setBold(style, false);
    StyleConstants.setItalic(style, false);
    styles.put("endtag", style);

    style = new SimpleAttributeSet();
    StyleConstants.setFontFamily(style, "Monospaced");
    StyleConstants.setFontSize(style, 12);
    StyleConstants.setBackground(style, Color.white);
    StyleConstants.setForeground(style, Color.black);
    StyleConstants.setBold(style, false);
    StyleConstants.setItalic(style, false);
    styles.put("reference", style);

    style = new SimpleAttributeSet();
    StyleConstants.setFontFamily(style, "Monospaced");
    StyleConstants.setFontSize(style, 12);
    StyleConstants.setBackground(style, Color.white);
    StyleConstants.setForeground(style, new Color(0xB03060));
    StyleConstants.setBold(style, true);
    StyleConstants.setItalic(style, false);
    styles.put("name", style);

    style = new SimpleAttributeSet();
    StyleConstants.setFontFamily(style, "Monospaced");
    StyleConstants.setFontSize(style, 12);
    StyleConstants.setBackground(style, Color.white);
    StyleConstants.setForeground(style, new Color(0xB03060));
    StyleConstants.setBold(style, false);
    StyleConstants.setItalic(style, true);
    styles.put("value", style);

    style = new SimpleAttributeSet();
    StyleConstants.setFontFamily(style, "Monospaced");
    StyleConstants.setFontSize(style, 12);
    StyleConstants.setBackground(style, Color.white);
    StyleConstants.setForeground(style, Color.black);
    StyleConstants.setBold(style, true);
    StyleConstants.setItalic(style, false);
    styles.put("text", style);

    style = new SimpleAttributeSet();
    StyleConstants.setFontFamily(style, "Monospaced");
    StyleConstants.setFontSize(style, 12);
    StyleConstants.setBackground(style, Color.white);
    StyleConstants.setForeground(style, Color.blue);
    StyleConstants.setBold(style, false);
    StyleConstants.setItalic(style, false);
    styles.put("reservedWord", style);

    style = new SimpleAttributeSet();
    StyleConstants.setFontFamily(style, "Monospaced");
    StyleConstants.setFontSize(style, 12);
    StyleConstants.setBackground(style, Color.white);
    StyleConstants.setForeground(style, Color.black);
    StyleConstants.setBold(style, false);
    StyleConstants.setItalic(style, false);
    styles.put("identifier", style);

    style = new SimpleAttributeSet();
    StyleConstants.setFontFamily(style, "Monospaced");
    StyleConstants.setFontSize(style, 12);
    StyleConstants.setBackground(style, Color.white);
    StyleConstants.setForeground(style, new Color(0xB03060));
    StyleConstants.setBold(style, false);
    StyleConstants.setItalic(style, false);
    styles.put("literal", style);

    style = new SimpleAttributeSet();
    StyleConstants.setFontFamily(style, "Monospaced");
    StyleConstants.setFontSize(style, 12);
    StyleConstants.setBackground(style, Color.white);
    StyleConstants.setForeground(style, new Color(0x000080));
    StyleConstants.setBold(style, false);
    StyleConstants.setItalic(style, false);
    styles.put("separator", style);

    style = new SimpleAttributeSet();
    StyleConstants.setFontFamily(style, "Monospaced");
    StyleConstants.setFontSize(style, 12);
    StyleConstants.setBackground(style, Color.white);
    StyleConstants.setForeground(style, Color.black);
    StyleConstants.setBold(style, true);
    StyleConstants.setItalic(style, false);
    styles.put("operator", style);

    style = new SimpleAttributeSet();
    StyleConstants.setFontFamily(style, "Monospaced");
    StyleConstants.setFontSize(style, 12);
    StyleConstants.setBackground(style, Color.white);
    StyleConstants.setForeground(style, Color.green.darker());
    StyleConstants.setBold(style, false);
    StyleConstants.setItalic(style, false);
    styles.put("comment", style);

    style = new SimpleAttributeSet();
    StyleConstants.setFontFamily(style, "Monospaced");
    StyleConstants.setFontSize(style, 12);
    StyleConstants.setBackground(style, Color.white);
    StyleConstants.setForeground(style, new Color(0xA020F0).darker());
    StyleConstants.setBold(style, false);
    StyleConstants.setItalic(style, false);
    styles.put("preprocessor", style);

    style = new SimpleAttributeSet();
    StyleConstants.setFontFamily(style, "Monospaced");
    StyleConstants.setFontSize(style, 12);
    StyleConstants.setBackground(style, Color.white);
    StyleConstants.setForeground(style, Color.black);
    StyleConstants.setBold(style, false);
    StyleConstants.setItalic(style, false);
    styles.put("whitespace", style);

    style = new SimpleAttributeSet();
    StyleConstants.setFontFamily(style, "Monospaced");
    StyleConstants.setFontSize(style, 12);
    StyleConstants.setBackground(style, Color.white);
    StyleConstants.setForeground(style, Color.red);
    StyleConstants.setBold(style, false);
    StyleConstants.setItalic(style, false);
    styles.put("error", style);

    style = new SimpleAttributeSet();
    StyleConstants.setFontFamily(style, "Monospaced");
    StyleConstants.setFontSize(style, 12);
    StyleConstants.setBackground(style, Color.white);
    StyleConstants.setForeground(style, Color.orange);
    StyleConstants.setBold(style, false);
    StyleConstants.setItalic(style, false);
    styles.put("unknown", style);
  }
*/

  /**
   * Just like a DefaultStyledDocument but intercepts inserts and
   * removes to color them.
   */
  private class HighLightedDocument extends DefaultStyledDocument {
    public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
      synchronized (doclock){
	super.insertString(offs, str, a);
	color(offs, str.length());
	documentReader.update(offs, str.length());
      }
    }

    public void remove(int offs, int len) throws BadLocationException {
      synchronized (doclock){
	super.remove(offs, len);
	color(offs, -len);
	documentReader.update(offs, -len);
      }
    }
  }
}

/**
 * A wrapper for a position in a document appropriate for storing
 * in a collection.
 */
class DocPosition {

  /**
   * The actual position
   */
  private int position;

  /**
   * Get the position represented by this DocPosition
   *
   * @return the position
   */
  int getPosition(){
    return position;
  }

  /**
   * Construct a DocPosition from the given offset into the document.
   *
   * @param position The position this DocObject will represent
   */
  public DocPosition(int position){
    this.position = position;
  }

  /**
   * Adjust this position.
   * This is useful in cases that an amount of text is inserted
   * or removed before this position.
   *
   * @param adjustment amount (either positive or negative) to adjust this position.
   * @return the DocPosition, adjusted properly.
   */
  public DocPosition adjustPosition(int adjustment){
    position += adjustment;
    return this;
  }

  /**
   * Two DocPositions are equal iff they have the same internal position.
   *
   * @return if this DocPosition represents the same position as another.
   */
  public boolean equals(Object obj){
    if (obj instanceof DocPosition){
      DocPosition d = (DocPosition)(obj);
      if (this.position == d.position){
	return true;
      } else {
	return false;
      }
    } else {
      return false;
    }
  }

  /**
   * A string representation useful for debugging.
   *
   * @return A string representing the position.
   */
  public String toString(){
    return "" + position;
  }
}

/**
 * A comparator appropriate for use with Collections of
 * DocPositions.
 */
class DocPositionComparator implements Comparator{
  /**
   * Does this Comparator equal another?
   * Since all DocPositionComparators are the same, they
   * are all equal.
   *
   * @return true for DocPositionComparators, false otherwise.
   */
  public boolean equals(Object obj){
    if (obj instanceof DocPositionComparator){
      return true;
    } else {
      return false;
    }
  }

  /**
   * Compare two DocPositions
   *
   * @param o1 first DocPosition
   * @param o2 second DocPosition
   * @return negative if first < second, 0 if equal, positive if first > second
   */
  public int compare(Object o1, Object o2){
    if (o1 instanceof DocPosition && o2 instanceof DocPosition){
      DocPosition d1 = (DocPosition)(o1);
      DocPosition d2 = (DocPosition)(o2);
      return (d1.getPosition() - d2.getPosition());
    } else if (o1 instanceof DocPosition){
      return -1;
    } else if (o2 instanceof DocPosition){
      return 1;
    } else if (o1.hashCode() < o2.hashCode()){
      return -1;
    } else if (o2.hashCode() > o1.hashCode()){
      return 1;
    } else {
      return 0;
    }
  }
}

/**
 * A reader interface for an abstract document.  Since
 * the syntax highlighting packages only accept Stings and
 * Readers, this must be used.
 * Since the close() method does nothing and a seek() method
 * has been added, this allows us to get some performance
 * improvements through reuse.  It can be used even after the
 * lexer explicitly closes it by seeking to the place that
 * we want to read next, and reseting the lexer.
 */
class DocumentReader extends Reader {

  /**
   * Modifying the document while the reader is working is like
   * pulling the rug out from under the reader.  Alerting the
   * reader with this method (in a nice thread safe way, this
   * should not be called at the same time as a read) allows
   * the reader to compensate.
   */
  public void update(int position, int adjustment){
    if (position < this.position){
      if (this.position < position - adjustment){
	this.position = position;
      } else {
	this.position += adjustment;
      }
    }
  }

  /**
   * Current position in the document. Incremented
   * whenever a character is read.
   */
  private long position = 0;

  /**
   * Saved position used in the mark and reset methods.
   */
  private long mark = -1;

  /**
   * The document that we are working with.
   */
  private AbstractDocument document;

  /**
   * Construct a reader on the given document.
   *
   * @param document the document to be read.
   */
  public DocumentReader(AbstractDocument document){
    this.document = document;
  }

  /**
   * Has no effect.  This reader can be used even after
   * it has been closed.
   */
  public void close() {
  }

  /**
   * Save a position for reset.
   *
   * @param readAheadLimit ignored.
   */
  public void mark(int readAheadLimit){
    mark = position;
  }

  /**
   * This reader support mark and reset.
   *
   * @return true
   */
  public boolean markSupported(){
    return true;
  }

  /**
   * Read a single character.
   *
   * @return the character or -1 if the end of the document has been reached.
   */
  public int read(){
    if (position < document.getLength()){
      try {
	char c = document.getText((int)position, 1).charAt(0);
	position++;
	return c;
      } catch (BadLocationException x){
	return -1;
      }
    } else {
      return -1;
    }
  }

  /**
   * Read and fill the buffer.
   * This method will always fill the buffer unless the end of the document is reached.
   *
   * @param cbuf the buffer to fill.
   * @return the number of characters read or -1 if no more characters are available in the document.
   */
  public int read(char[] cbuf){
    return read(cbuf, 0, cbuf.length);
  }

  /**
   * Read and fill the buffer.
   * This method will always fill the buffer unless the end of the document is reached.
   *
   * @param cbuf the buffer to fill.
   * @param off offset into the buffer to begin the fill.
   * @param len maximum number of characters to put in the buffer.
   * @return the number of characters read or -1 if no more characters are available in the document.
   */
  public int read(char[] cbuf, int off, int len){
    if (position < document.getLength()){
      int length = len;
      if (position + length >= document.getLength()){
	length = document.getLength() - (int)position;
      }
      if (off + length >= cbuf.length){
	length = cbuf.length - off;
      }
      try {
	String s = document.getText((int)position, length);
	position += length;
	for (int i=0; i<length; i++){
	  cbuf[off+i] = s.charAt(i);
	}
	return length;
      } catch (BadLocationException x){
	return -1;
      }
    } else {
      return -1;
    }
  }

  /**
   * @return true
   */
  public boolean ready() {
    return true;
  }

  /**
   * Reset this reader to the last mark, or the beginning of the document if a mark has not been set.
   */
  public void reset(){
    if (mark == -1){
      position = 0;
    } else {
      position = mark;
    }
    mark = -1;
  }

  /**
   * Skip characters of input.
   * This method will always skip the maximum number of characters unless
   * the end of the file is reached.
   *
   * @param n number of characters to skip.
   * @return the actual number of characters skipped.
   */
  public long skip(long n){
    if (position + n <= document.getLength()){
      position += n;
      return n;
    } else {
      long oldPos = position;
      position = document.getLength();
      return (document.getLength() - oldPos);
    }
  }

  /**
   * Seek to the given position in the document.
   *
   * @param n the offset to which to seek.
   */
  public void seek(long n){
    if (n <= document.getLength()){
      position = n;
    } else {
      position = document.getLength();
    }
  }
}

