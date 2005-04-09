/* -*- mode: jde; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  PdeEditorConsole - message console that sits below the program area
  Part of the Processing project - http://Proce55ing.net

  Except where noted, code is written by Ben Fry and
  Copyright (c) 2001-03 Massachusetts Institute of Technology

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

package processing.app;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;
import javax.swing.text.*;


// debugging this class is tricky.. if it's throwing
// exceptions, don't take over System.err, and debug
// while watching just System.out
// or just write directly to systemOut or systemErr

public class PdeEditorConsole extends JScrollPane {
  PdeEditor editor;

  JTextPane consoleTextPane;
  StyledDocument consoleDoc;

  MutableAttributeSet stdStyle;
  MutableAttributeSet errStyle;

  boolean cerror;

  //int maxCharCount;
  int maxLineCount;

  static PrintStream systemOut;
  static PrintStream systemErr;

  static PrintStream consoleOut;
  static PrintStream consoleErr;

  static OutputStream stdoutFile;
  static OutputStream stderrFile;


  public PdeEditorConsole(PdeEditor editor) {
    this.editor = editor;

    maxLineCount = PdePreferences.getInteger("console.length");

    consoleTextPane = new JTextPane();
    consoleTextPane.setEditable(false);
    consoleDoc = consoleTextPane.getStyledDocument();

    // necessary?
    MutableAttributeSet standard = new SimpleAttributeSet();
    StyleConstants.setAlignment(standard, StyleConstants.ALIGN_LEFT);
    consoleDoc.setParagraphAttributes(0, 0, standard, true);

    // build styles for different types of console output
    Color bgColor    = PdePreferences.getColor("console.color");
    Color fgColorOut = PdePreferences.getColor("console.output.color");
    Color fgColorErr = PdePreferences.getColor("console.error.color");
    Font font        = PdePreferences.getFont("console.font");

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

    consoleTextPane.setBackground(bgColor);

    // add the jtextpane to this scrollpane
    this.setViewportView(consoleTextPane);

    // calculate height of a line of text in pixels
    // and size window accordingly
    FontMetrics metrics = this.getFontMetrics(font);
    int height = metrics.getAscent() + metrics.getDescent();
    int lines = PdePreferences.getInteger("console.lines"); //, 4);
    int sizeFudge = 6; //10; // unclear why this is necessary, but it is
    setPreferredSize(new Dimension(1024, (height * lines) + sizeFudge));
    setMinimumSize(new Dimension(1024, (height * 4) + sizeFudge));

    if (systemOut == null) {
      systemOut = System.out;
      systemErr = System.err;

      try {
        String outFileName = PdePreferences.get("console.output.file");
        if (outFileName != null) {
          stdoutFile = new FileOutputStream(outFileName);
        }

        String errFileName = PdePreferences.get("console.error.file");
        if (errFileName != null) {
          stderrFile = new FileOutputStream(outFileName);
        }
      } catch (IOException e) {
        PdeBase.showWarning("Console Error",
                            "A problem occurred while trying to open the\n" +
                            "files used to store the console output.", e);
      }

      consoleOut =
        new PrintStream(new PdeEditorConsoleStream(this, false, stdoutFile));
      consoleErr =
        new PrintStream(new PdeEditorConsoleStream(this, true, stderrFile));

      if (PdePreferences.getBoolean("console")) {
        try {
          System.setOut(consoleOut);
          System.setErr(consoleErr);
        } catch (Exception e) {
          e.printStackTrace(systemOut);
        }
      }
    }

    // to fix ugliness.. normally macosx java 1.3 puts an
    // ugly white border around this object, so turn it off.
    if (PdeBase.isMacOS()) {
      setBorder(null);
    }
  }


  public void write(byte b[], int offset, int length, boolean err) {
    if (err != cerror) {
      // advance the line because switching between err/out streams
      // potentially, could check whether we're already on a new line
      message("", cerror, true);
    }

    // we could do some cross platform CR/LF mangling here before outputting

    // add text to output document
    message(new String(b, offset, length), err, false);
    // set last error state
    cerror = err;
  }


  public void message(String what, boolean err, boolean advance) {
    // under osx, suppress the spew about the serial port
    // to avoid an error every time someone loads their app
    // (the error is dealt with in PdeBase with a message dialog)
    /*
    // no longer an issue. using a newer rev of rxtx
    if (PdeBase.platform == PdeBase.MACOSX) {
      if (what.equals("Error loading SolarisSerial: java.lang.UnsatisfiedLinkError: no SolarisSerialParallel in java.library.path")) return;
      if (what.equals("Caught java.lang.UnsatisfiedLinkError: readRegistrySerial while loading driver com.sun.comm.SolarisDriver")) return;
    }
    */

    if (err) {
      systemErr.print(what);
    } else {
      systemOut.print(what);
    }

    if (advance) {
      appendText("\n", err);
      if (err) {
        systemErr.println();
      } else {
        systemOut.println();
      }
    }

    // to console display
    appendText(what, err);
    // moved down here since something is punting
  }


  /**
   * append a piece of text to the console.
   *
   * Swing components are NOT thread-safe, and since the PdeMessageSiphon
   * instantiates new threads, and in those callbacks, they often print
   * output to stdout and stderr, which are wrapped by PdeEditorConsoleStream
   * and eventually leads to PdeEditorConsole.appendText(), which directly
   * updates the Swing text components, causing deadlock.
   *
   * A quick hack from Francis Li (who found this to be a problem)
   * wraps the contents of appendText() into a Runnable and uses
   * SwingUtilities.invokeLater() to ensure that the updates only
   * occur on the main event dispatching thread, and that appears
   * to have solved the problem.
   */
  synchronized private void appendText(String txt, boolean e) {
    final String text = txt;
    final boolean err = e;
    SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          try {
            // check how many lines have been used so far
            // if too many, shave off a few lines from the beginning
            Element element = consoleDoc.getDefaultRootElement();
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
              consoleDoc.remove(0, endOffset);
            }

            // add the text to the end of the console,
            consoleDoc.insertString(consoleDoc.getLength(), text,
                                    err ? errStyle : stdStyle);

            // always move to the end of the text as it's added
            consoleTextPane.setCaretPosition(consoleDoc.getLength());

          } catch (BadLocationException e) {
            // ignore the error otherwise this will cause an infinite loop
            // maybe not a good idea in the long run?
          }
        }
      });
  }


  public void clear() {
    try {
      consoleDoc.remove(0, consoleDoc.getLength());
    } catch (BadLocationException e) {
      // ignore the error otherwise this will cause an infinite loop
      // maybe not a good idea in the long run?
    }
  }
}


class PdeEditorConsoleStream extends OutputStream {
  PdeEditorConsole parent;
  boolean err; // whether stderr or stdout
  byte single[] = new byte[1];
  OutputStream echo;

  public PdeEditorConsoleStream(PdeEditorConsole parent,
                                boolean err, OutputStream echo) {
    this.parent = parent;
    this.err = err;
    this.echo = echo;
  }

  public void close() { }

  public void flush() { }

  public void write(byte b[]) {  // appears never to be used
    parent.write(b, 0, b.length, err);
    if (echo != null) {
      try {
        echo.write(b); //, 0, b.length);
        echo.flush();
      } catch (IOException e) {
        e.printStackTrace();
        echo = null;
      }
    }
  }

  public void write(byte b[], int offset, int length) {
    parent.write(b, offset, length, err);
    if (echo != null) {
      try {
        echo.write(b, offset, length);
        echo.flush();
      } catch (IOException e) {
        e.printStackTrace();
        echo = null;
      }
    }
  }

  public void write(int b) {
    single[0] = (byte)b;
    parent.write(single, 0, 1, err);
    if (echo != null) {
      try {
        echo.write(b);
        echo.flush();
      } catch (IOException e) {
        e.printStackTrace();
        echo = null;
      }
    }
  }
}
