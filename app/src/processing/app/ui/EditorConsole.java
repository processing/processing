/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2012-19 The Processing Foundation
  Copyright (c) 2004-12 Ben Fry and Casey Reas
  Copyright (c) 2001-04 Massachusetts Institute of Technology

  This program is free software; you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation; either version 2 of the License, or
  (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software Foundation,
  Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package processing.app.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.concurrent.LinkedBlockingQueue;

import javax.swing.*;
import javax.swing.border.MatteBorder;
import javax.swing.text.*;

import processing.app.Console;
import processing.app.Mode;
import processing.app.Preferences;


/**
 * Message console that sits below the editing area.
 */
public class EditorConsole extends JScrollPane {
  Editor editor;

  Timer flushTimer;

  JTextPane consoleTextPane;
  BufferedStyledDocument consoleDoc;

  MutableAttributeSet stdStyle;
  MutableAttributeSet errStyle;

  int maxLineCount;
  int maxCharCount;

  PrintStream sketchOut;
  PrintStream sketchErr;

  static EditorConsole current;


  public EditorConsole(Editor editor) {
    this.editor = editor;

    maxLineCount = Preferences.getInteger("console.scrollback.lines");
    maxCharCount = Preferences.getInteger("console.scrollback.chars");

    consoleDoc = new BufferedStyledDocument(10000, maxLineCount, maxCharCount);
    consoleTextPane = new JTextPane(consoleDoc);
    consoleTextPane.setEditable(false);

    updateMode();

    setViewportView(consoleTextPane);

    sketchOut = new PrintStream(new EditorConsoleStream(false));
    sketchErr = new PrintStream(new EditorConsoleStream(true));

    startTimer();
  }


  protected void flush() {
    // only if new text has been added
    if (consoleDoc.hasAppendage()) {
      // insert the text that's been added in the meantime
      consoleDoc.insertAll();
      // always move to the end of the text as it's added
      consoleTextPane.setCaretPosition(consoleDoc.getLength());
    }
  }


  /**
   * Start the timer that handles flushing the console text. Has to be started
   * and stopped/cleared because the Timer thread will keep a reference to its
   * Editor around even after the Editor has been closed, leaking memory.
   */
  protected void startTimer() {
    if (flushTimer == null) {
      // periodically post buffered messages to the console
      // should the interval come from the preferences file?
      flushTimer = new Timer(250, new ActionListener() {
        public void actionPerformed(ActionEvent evt) {
          flush();
        }
      });
      flushTimer.start();
    }
  }


  protected void stopTimer() {
    if (flushTimer != null) {
      flush();  // clear anything that's there
      flushTimer.stop();
      flushTimer = null;
    }
  }


  public PrintStream getOut() {
    return sketchOut;
  }


  public PrintStream getErr() {
    return sketchErr;
  }


  /**
   * Update the font family and sizes based on the Preferences window.
   */
  protected void updateAppearance() {
    String fontFamily = Preferences.get("editor.font.family");
    int fontSize = Toolkit.zoom(Preferences.getInteger("console.font.size"));
    StyleConstants.setFontFamily(stdStyle, fontFamily);
    StyleConstants.setFontSize(stdStyle, fontSize);
    StyleConstants.setFontFamily(errStyle, fontFamily);
    StyleConstants.setFontSize(errStyle, fontSize);
    clear();  // otherwise we'll have mixed fonts
  }


  /**
   * Change coloring, fonts, etc in response to a mode change.
   */
  protected void updateMode() {
    Mode mode = editor.getMode();

    // necessary?
    MutableAttributeSet standard = new SimpleAttributeSet();
    StyleConstants.setAlignment(standard, StyleConstants.ALIGN_LEFT);
    consoleDoc.setParagraphAttributes(0, 0, standard, true);

    Font font = Preferences.getFont("console.font");

    // build styles for different types of console output
    Color bgColor = mode.getColor("console.color");
    Color fgColorOut = mode.getColor("console.output.color");
    Color fgColorErr = mode.getColor("console.error.color");

    // Make things line up with the Editor above. If this is ever removed,
    // setBorder(null) should be called instead. The defaults are nasty.
    setBorder(new MatteBorder(0, Editor.LEFT_GUTTER, 0, 0, bgColor));

    stdStyle = new SimpleAttributeSet();
    StyleConstants.setForeground(stdStyle, fgColorOut);
    StyleConstants.setBackground(stdStyle, bgColor);
    StyleConstants.setFontSize(stdStyle, font.getSize());
    StyleConstants.setFontFamily(stdStyle, font.getFamily());
    StyleConstants.setBold(stdStyle, font.isBold());
    StyleConstants.setItalic(stdStyle, font.isItalic());

    errStyle = new SimpleAttributeSet();
    StyleConstants.setForeground(errStyle, fgColorErr);
    StyleConstants.setBackground(errStyle, bgColor);
    StyleConstants.setFontSize(errStyle, font.getSize());
    StyleConstants.setFontFamily(errStyle, font.getFamily());
    StyleConstants.setBold(errStyle, font.isBold());
    StyleConstants.setItalic(errStyle, font.isItalic());

    if (UIManager.getLookAndFeel().getID().equals("Nimbus")) {
      getViewport().setBackground(bgColor);
      consoleTextPane.setOpaque(false);
      consoleTextPane.setBackground(new Color(0, 0, 0, 0));
    } else {
      consoleTextPane.setBackground(bgColor);
    }

    // calculate height of a line of text in pixels
    // and size window accordingly
    FontMetrics metrics = this.getFontMetrics(font);
    int height = metrics.getAscent() + metrics.getDescent();
    int lines = Preferences.getInteger("console.lines"); //, 4);
    int sizeFudge = 6; //10; // unclear why this is necessary, but it is
    setPreferredSize(new Dimension(1024, (height * lines) + sizeFudge));
    setMinimumSize(new Dimension(1024, (height * 4) + sizeFudge));
  }


  static public void setEditor(Editor editor) {
    if (current != null) {
      current.stopTimer();  // allow to be garbage collected
    }
    editor.console.setCurrent();
  }


  void setCurrent() {
    current = this;  //editor.console;
    startTimer();
    Console.setEditor(sketchOut, sketchErr);
  }


  public void message(String what, boolean err) {
    if (err && (what.contains("invalid context 0x0") || (what.contains("invalid drawable")))) {
      // Respectfully declining... This is a quirk of more recent releases of
      // Java on Mac OS X, but is widely reported as the source of any other
      // bug or problem that a user runs into. It may well be a Processing
      // bug, but until we know, we're suppressing the messages.
    } else if (err && what.contains("is calling TIS/TSM in non-main thread environment")) {
      // Error message caused by JOGL since macOS 10.13.4, cannot fix at the moment so silencing it:
      // https://github.com/processing/processing/issues/5462
      // Some discussion on the Apple's developer forums seems to suggest that is not serious:
      // https://forums.developer.apple.com/thread/105244
    } else if (err && what.contains("NSWindow drag regions should only be invalidated on the Main Thread")) {
      // Keep hiding warnings triggered by JOGL on recent macOS versions (this is from 10.14 onwards I think).
    } else if (err && what.contains("Make pbuffer:")) {
      // Remove initalization warning from LWJGL.
    } else if (err && what.contains("XInitThreads() called for concurrent")) {
      // "Info: XInitThreads() called for concurrent Thread support" message on Linux
    } else if (!err && what.contains("Listening for transport dt_socket at address")) {
      // Message from the JVM about the socket launch for debug
      // Listening for transport dt_socket at address: 8727
    } else {
      // Append a piece of text to the console. Swing components are NOT
      // thread-safe, and since the MessageSiphon instantiates new threads,
      // and in those callbacks, they often print output to stdout and stderr,
      // which are wrapped by EditorConsoleStream and eventually leads to
      // EditorConsole.appendText(), which directly updates the Swing text
      // components, causing deadlock. Updates are buffered to the console and
      // displayed at regular intervals on Swing's event-dispatching thread.
      // (patch by David Mellis)
      consoleDoc.appendString(what, err ? errStyle : stdStyle);
    }
  }


  public void clear() {
    try {
      consoleDoc.remove(0, consoleDoc.getLength());
    } catch (BadLocationException e) {
      // ignore the error otherwise this will cause an infinite loop
      // maybe not a good idea in the long run?
    }
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  class EditorConsoleStream extends OutputStream {
    boolean err;

    public EditorConsoleStream(boolean err) {
      this.err = err;
    }

    public void write(byte b[], int offset, int length) {
      message(new String(b, offset, length), err);
    }

    // doesn't appear to be called (but must be implemented)
    public void write(int b) {
      write(new byte[] { (byte) b }, 0, 1);
    }
  }
}


// . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


/**
 * Buffer updates to the console and output them in batches. For info, see:
 * http://java.sun.com/products/jfc/tsc/articles/text/element_buffer and
 * http://javatechniques.com/public/java/docs/gui/jtextpane-speed-part2.html
 * appendString() is called from multiple threads, and insertAll from the
 * swing event thread, so they need to be synchronized
 */
class BufferedStyledDocument extends DefaultStyledDocument {
  //List<ElementSpec> elements = new ArrayList<>();
  LinkedBlockingQueue<ElementSpec> elements;
//  AtomicInteger queuedLineCount = new AtomicInteger();
  int maxLineLength, maxLineCount, maxCharCount;
  int currentLineLength = 0;
  boolean needLineBreak = false;
//  boolean hasAppendage = false;
  final Object insertLock = new Object();

  public BufferedStyledDocument(int maxLineLength, int maxLineCount,
                                int maxCharCount) {
    this.maxLineLength = maxLineLength;
    this.maxLineCount = maxLineCount;
    this.maxCharCount = maxCharCount;
    elements = new LinkedBlockingQueue<>();
  }

  // monitor this so that it's only updated when needed (otherwise console
  // updates every 250 ms when an app isn't even running.. see bug 180)
  public boolean hasAppendage() {
    return elements.size() > 0;
  }

  /** buffer a string for insertion at the end of the DefaultStyledDocument */
  public void appendString(String str, AttributeSet a) {
//    hasAppendage = true;

    // process each line of the string
    while (str.length() > 0) {
      // newlines within an element have (almost) no effect, so we need to
      // replace them with proper paragraph breaks (start and end tags)
      if (needLineBreak || currentLineLength > maxLineLength) {
        elements.add(new ElementSpec(a, ElementSpec.EndTagType));
        elements.add(new ElementSpec(a, ElementSpec.StartTagType));
//        queuedLineCount.incrementAndGet();
        currentLineLength = 0;
      }

      if (str.indexOf('\n') == -1) {
        elements.add(new ElementSpec(a, ElementSpec.ContentType,
          str.toCharArray(), 0, str.length()));
        currentLineLength += str.length();
        needLineBreak = false;
        str = str.substring(str.length()); // eat the string
      } else {
        elements.add(new ElementSpec(a, ElementSpec.ContentType,
          str.toCharArray(), 0, str.indexOf('\n') + 1));
        needLineBreak = true;
        str = str.substring(str.indexOf('\n') + 1); // eat the line
      }
      /*
      while (queuedLineCount.get() > maxLineCount) {
        Console.systemOut("too many: " + queuedLineCount);
        ElementSpec elem = elements.remove();
        if (elem.getType() == ElementSpec.EndTagType) {
          queuedLineCount.decrementAndGet();
        }
      }
      */
    }
    if (elements.size() > 1000) {
      insertAll();
    }
  }

  /** insert the buffered strings */
  public void insertAll() {
    /*
    // each line is ~3 elements
    int tooMany = elements.size() - maxLineCount*3;
    if (tooMany > 0) {
      try {
        remove(0, getLength()); // clear the document first
      } catch (BadLocationException ble) {
        ble.printStackTrace();
      }
      Console.systemOut("skipping " + elements.size());
      for (int i = 0; i < tooMany; i++) {
        elements.remove();
      }
    }
    */
    ElementSpec[] elementArray = elements.toArray(new ElementSpec[0]);

    try {
      /*
      // check how many lines have been used so far
      // if too many, shave off a few lines from the beginning
      Element element = super.getDefaultRootElement();
      int lineCount = element.getElementCount();
      int overage = lineCount - maxLineCount;
      if (overage > 0) {
        // if 1200 lines, and 1000 lines is max,
        // find the position of the end of the 200th line
        //systemOut.println("overage is " + overage);
        Element lineElement = element.getElement(overage);
        if (lineElement == null) return;  // do nuthin

        int endOffset = lineElement.getEndOffset();
        // remove to the end of the 200th line
        super.remove(0, endOffset);
      }
      */
      synchronized (insertLock) {
        checkLength();
        insert(getLength(), elementArray);
        checkLength();
      }

    } catch (BadLocationException e) {
      // ignore the error otherwise this will cause an infinite loop
      // maybe not a good idea in the long run?
    }
    elements.clear();
//    hasAppendage = false;
  }

  private void checkLength() throws BadLocationException {
    // set a limit on the number of characters in the console
    int docLength = getLength();
    if (docLength > maxCharCount) {
      remove(0, docLength - maxCharCount);
    }
    // check how many lines have been used so far
    // if too many, shave off a few lines from the beginning
    Element element = super.getDefaultRootElement();
    int lineCount = element.getElementCount();
    int overage = lineCount - maxLineCount;
    if (overage > 0) {
      // if 1200 lines, and 1000 lines is max,
      // find the position of the end of the 200th line
      //systemOut.println("overage is " + overage);
      Element lineElement = element.getElement(overage);
      if (lineElement != null) {
        int endOffset = lineElement.getEndOffset();
        // remove to the end of the 200th line
        super.remove(0, endOffset);
      }
    }
  }
}
