/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2012-16 The Processing Foundation
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
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;
import javax.swing.border.MatteBorder;
import javax.swing.text.*;

import processing.app.Console;
import processing.app.Mode;
import processing.app.Preferences;


/**
 * Message console that sits below the editing area.
 * <p />
 * Be careful when debugging this class, because if it's throwing exceptions,
 * don't take over System.err, and debug while watching just System.out
 * or just call println() or whatever directly to systemOut or systemErr.
 * <p />
 * Also note that encodings will not work properly when run from Eclipse. This
 * means that if you use non-ASCII characters in a println() or some such,
 * the characters won't print properly in the Processing and/or Eclipse console.
 * It seems that Eclipse's console-grabbing and that of Processing don't
 * get along with one another. Use 'ant run' to work on encoding-related issues.
 */
public class EditorConsole extends JScrollPane {
  Editor editor;

  Timer flushTimer;

  JTextPane consoleTextPane;
  BufferedStyledDocument consoleDoc;

  MutableAttributeSet stdStyle;
  MutableAttributeSet errStyle;

  int maxLineCount;

  PrintStream sketchOut;
  PrintStream sketchErr;

  /*
  // Single static instance shared because there's only one real System.out.
  // Within the input handlers, the currentConsole variable will be used to
  // echo things to the correct location.

  static PrintStream systemOut;
  static PrintStream systemErr;

  static PrintStream consoleOut;
  static PrintStream consoleErr;

  static OutputStream stdoutFile;
  static OutputStream stderrFile;
  */

  static EditorConsole current;

  /*
  // For 0185, moved the first init to this static { } block, so that we never
  // have a situation that causes systemOut/Err to not be set properly.
  static {
    systemOut = System.out;
    systemErr = System.err;

    // placing everything inside a try block because this can be a dangerous
    // time for the lights to blink out and crash for and obscure reason.
    try {
      // Create output files that will have a randomized name. Has to
      // be randomized otherwise another instance of Processing (or one of its
      // sister IDEs) might collide with the file causing permissions problems.
      // The files and folders are not deleted on exit because they may be
      // needed for debugging or bug reporting.
      SimpleDateFormat formatter = new SimpleDateFormat("yyMMdd");
      String randy = PApplet.nf((int) (1000 * Math.random()), 4);
      String stamp = formatter.format(new Date()) + "_" + randy;

      File consoleDir = Base.getSettingsFile("console");
      consoleDir.mkdirs();
      File outFile = new File(consoleDir, stamp + ".out");
      stdoutFile = new FileOutputStream(outFile);
      File errFile = new File(consoleDir, stamp + ".err");
      stderrFile = new FileOutputStream(errFile);

      consoleOut = new PrintStream(new EditorConsoleStream(false, null));
      consoleErr = new PrintStream(new EditorConsoleStream(true, null));

      System.setOut(consoleOut);
      System.setErr(consoleErr);

//    } catch (Exception e) {
//      stdoutFile = null;
//      stderrFile = null;
//
//      e.printStackTrace();
//      Base.showWarning("Console Error",
//                       "A problem occurred while trying to open the\n" +
//                       "files used to store the console output.", e);
    } catch (Exception e) {
      stdoutFile = null;
      stderrFile = null;

      consoleOut = null;
      consoleErr = null;

      System.setOut(systemOut);
      System.setErr(systemErr);

      e.printStackTrace(systemErr);
    }
  }
  */


  public EditorConsole(Editor editor) {
    this.editor = editor;

    maxLineCount = Preferences.getInteger("console.length");

    consoleDoc = new BufferedStyledDocument(10000, maxLineCount);
    consoleTextPane = new JTextPane(consoleDoc);
    consoleTextPane.setEditable(false);

    updateMode();

    // add the jtextpane to this scrollpane
    this.setViewportView(consoleTextPane);

//    sketchOut = new PrintStream(new EditorConsoleStream(false, this));
//    sketchErr = new PrintStream(new EditorConsoleStream(true, this));
    sketchOut = new PrintStream(new EditorConsoleStream(false));
    sketchErr = new PrintStream(new EditorConsoleStream(true));

    startTimer();
  }


  protected void flush() {
    // only if new text has been added
    if (consoleDoc.hasAppendage) {
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
    int fontSize =
      Toolkit.zoom(Preferences.getInteger("console.font.size"));
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


//  /**
//   * Close the streams so that the temporary files can be deleted.
//   * <p/>
//   * File.deleteOnExit() cannot be used because the stdout and stderr
//   * files are inside a folder, and have to be deleted before the
//   * folder itself is deleted, which can't be guaranteed when using
//   * the deleteOnExit() method.
//   */
//  public static void handleQuit() {
//    // replace original streams to remove references to console's streams
//    System.setOut(systemOut);
//    System.setErr(systemErr);
//
//    try {
//      // close the PrintStream
//      if (consoleOut != null) consoleOut.close();
//      if (consoleErr != null) consoleErr.close();
//
//      // also have to close the original FileOutputStream
//      // otherwise it won't be shut down completely
//      if (stdoutFile != null) stdoutFile.close();
//      if (stderrFile != null) stderrFile.close();
//
//    } catch (IOException e) {
//      e.printStackTrace(systemErr);
//    }
//  }


  synchronized public void message(String what, boolean err) {
    // now handled in Console
    /*
    if (err) {
      Console.systemErr.print(what);
    } else {
      Console.systemOut.print(what);
    }
    */

    if (err && (what.contains("invalid context 0x0") || (what.contains("invalid drawable")))) {
      // Respectfully declining... This is a quirk of more recent releases of
      // Java on Mac OS X, but is widely reported as the source of any other
      // bug or problem that a user runs into. It may well be a Processing
      // bug, but until we know, we're suppressing the messages.
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


  /*
  static class EditorConsoleStream extends OutputStream {
    boolean err;
    EditorConsole console;

    public EditorConsoleStream(boolean err, EditorConsole console) {
      this.err = err;
      this.console = console;
    }

    public void write(byte b[], int offset, int length) {
      if (console != null) {
        console.message(new String(b, offset, length), err);
      } else if (currentConsole != null) {
        currentConsole.message(new String(b, offset, length), err);
      } else {
        // If no console is present, still need to write this to the actual
        // System.out or System.err. Otherwise we can't !#$!% debug anything.
        if (err) {
          systemErr.write(b, offset, length);
        } else {
          systemOut.write(b, offset, length);
        }
      }
      writeEcho(b, offset, length);
    }
  }
  */


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
  List<ElementSpec> elements = new ArrayList<ElementSpec>();
  int maxLineLength, maxLineCount;
  int currentLineLength = 0;
  boolean needLineBreak = false;
  boolean hasAppendage = false;

  public BufferedStyledDocument(int maxLineLength, int maxLineCount) {
    this.maxLineLength = maxLineLength;
    this.maxLineCount = maxLineCount;
  }

  /** buffer a string for insertion at the end of the DefaultStyledDocument */
  public synchronized void appendString(String str, AttributeSet a) {
    // do this so that it's only updated when needed (otherwise console
    // updates every 250 ms when an app isn't even running.. see bug 180)
    hasAppendage = true;

    // process each line of the string
    while (str.length() > 0) {
      // newlines within an element have (almost) no effect, so we need to
      // replace them with proper paragraph breaks (start and end tags)
      if (needLineBreak || currentLineLength > maxLineLength) {
        elements.add(new ElementSpec(a, ElementSpec.EndTagType));
        elements.add(new ElementSpec(a, ElementSpec.StartTagType));
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
    }
  }

  /** insert the buffered strings */
  public synchronized void insertAll() {
    ElementSpec[] elementArray = new ElementSpec[elements.size()];
    elements.toArray(elementArray);

    try {
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
      super.insert(super.getLength(), elementArray);

    } catch (BadLocationException e) {
      // ignore the error otherwise this will cause an infinite loop
      // maybe not a good idea in the long run?
    }
    elements.clear();
    hasAppendage = false;
  }
}
