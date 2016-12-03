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

package processing.app;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;


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
public class Console {
//  PrintStream sketchOut;
//  PrintStream sketchErr;

  // Single static instance shared because there's only one real System.out.
  // Within the input handlers, the currentConsole variable will be used to
  // echo things to the correct location.

  /** The original System.out */
  static PrintStream systemOut;
  /** The original System.err */
  static PrintStream systemErr;

  /** Our replacement System.out */
  static PrintStream consoleOut;
  /** Our replacement System.err */
  static PrintStream consoleErr;

  /** All stdout also written to a file */
  static OutputStream stdoutFile;
  /** All stderr also written to a file */
  static OutputStream stderrFile;

  /** stdout listener for the currently active Editor */
  static OutputStream editorOut;
  /** stderr listener for the currently active Editor */
  static OutputStream editorErr;


  static public void startup() {
    systemOut = System.out;
    systemErr = System.err;

    // placing everything inside a try block because this can be a dangerous
    // time for the lights to blink out and crash for and obscure reason.
    try {
      SimpleDateFormat formatter = new SimpleDateFormat("yyMMdd_HHmmss");
      // Moving away from a random string in 0256 (and adding hms) because
      // the random digits looked like times anyway, causing confusion.
      //String randy = String.format("%04d", (int) (1000 * Math.random()));
      //final String stamp = formatter.format(new Date()) + "_" + randy;
      final String stamp = formatter.format(new Date());

      File consoleDir = Base.getSettingsFile("console");
      if (consoleDir.exists()) {
        // clear old debug files
        File[] stdFiles = consoleDir.listFiles(new FileFilter() {
          final String todayPrefix = stamp.substring(0, 4);

          public boolean accept(File file) {
            if (!file.isDirectory()) {
              String name = file.getName();
              if (name.endsWith(".err") || name.endsWith(".out")) {
                // don't delete any of today's debug messages
                return !name.startsWith(todayPrefix);
              }
            }
            return false;
          }
        });
        // Remove any files that aren't from today
        for (File file : stdFiles) {
          file.delete();
        }
      } else {
        consoleDir.mkdirs();
      }

      File outFile = new File(consoleDir, stamp + ".out");
      stdoutFile = new FileOutputStream(outFile);
      File errFile = new File(consoleDir, stamp + ".err");
      stderrFile = new FileOutputStream(errFile);

//      consoleOut = new PrintStream(new EditorConsoleStream(false, null));
//      consoleErr = new PrintStream(new EditorConsoleStream(true, null));
      consoleOut = new PrintStream(new ConsoleStream(false));
      consoleErr = new PrintStream(new ConsoleStream(true));

      System.setOut(consoleOut);
      System.setErr(consoleErr);

    } catch (Exception e) {
      stdoutFile = null;
      stderrFile = null;

      consoleOut = null;
      consoleErr = null;

      System.setOut(systemOut);
      System.setErr(systemErr);

      e.printStackTrace();
    }
  }


  static public void setEditor(OutputStream out, OutputStream err) {
    editorOut = out;
    editorErr = err;
  }


//  public Console() {
//    sketchOut = new PrintStream(new EditorConsoleStream(false, this));
//    sketchErr = new PrintStream(new EditorConsoleStream(true, this));
//  }


//  public PrintStream getOut() {
//    return sketchOut;
//  }


//  public PrintStream getErr() {
//    return sketchErr;
//  }


  /**
   * Close the streams so that the temporary files can be deleted.
   * <p/>
   * File.deleteOnExit() cannot be used because the stdout and stderr
   * files are inside a folder, and have to be deleted before the
   * folder itself is deleted, which can't be guaranteed when using
   * the deleteOnExit() method.
   */
  static public void shutdown() {
    // replace original streams to remove references to console's streams
    System.setOut(systemOut);
    System.setErr(systemErr);

    cleanup(consoleOut);
    cleanup(consoleErr);

    // also have to close the original FileOutputStream
    // otherwise it won't be shut down completely
    cleanup(stdoutFile);
    cleanup(stderrFile);
  }


  static private void cleanup(OutputStream output) {
    try {
      if (output != null) {
        output.flush();
        output.close();
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  static class ConsoleStream extends OutputStream {
    boolean err; // whether stderr or stdout
    byte single[] = new byte[1];

    public ConsoleStream(boolean err) {
      this.err = err;
    }

    public void close() { }

    public void flush() { }

    public void write(byte b[]) {  // appears never to be used
      write(b, 0, b.length);
    }

    public void write(byte b[], int offset, int length) {
      // First write to the original stdout/stderr
      if (err) {
        systemErr.write(b, offset, length);
      } else {
        systemOut.write(b, offset, length);
      }

      // Write to the files that are storing this information
      writeFile(b, offset, length);

      // Write to the console of the current Editor, if any
      try {
        if (err) {
          if (editorErr != null) {
            editorErr.write(b, offset, length);
          }
        } else {
          if (editorOut != null) {
            editorOut.write(b, offset, length);
          }
        }
      } catch (IOException e) {
        // Avoid this function being called in a recursive, infinite loop
        e.printStackTrace(systemErr);
      }
    }

    public void writeFile(byte b[], int offset, int length) {
      final OutputStream echo = err ? stderrFile : stdoutFile;
      if (echo != null) {
        try {
          echo.write(b, offset, length);
          echo.flush();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }

    public void write(int b) {
      single[0] = (byte) b;
      write(single, 0, 1);
    }
  }
}