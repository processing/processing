/* -*- mode: jde; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Runner - runs a compiled java applet
  Part of the Processing project - http://processing.org

  Copyright (c) 2004-05 Ben Fry and Casey Reas
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

import processing.core.*;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.lang.reflect.*;

import com.oroinc.text.regex.*;


public class Runner implements MessageConsumer {

  PApplet applet;
  RunnerException exception;
  Window window;
  PrintStream leechErr;
  //String className;

  Editor editor;
  Sketch sketch;

  boolean newMessage;
  int messageLineCount;
  boolean foundMessageSource;

  Process process;
  SystemOutSiphon processInput;
  OutputStream processOutput;
  MessageSiphon processError;


  public Runner(Sketch sketch, Editor editor) {
    this.sketch = sketch;
    this.editor = editor;
  }


  public void start(Point windowLocation) throws RunnerException {
    //System.out.println(" externalRuntime is " +  sketch.externalRuntime);
    this.leechErr = new PrintStream(new MessageStream(this));

    try {
      if (editor.presenting) {
        startPresenting();

      } else if (sketch.externalRuntime) {
        startExternalRuntime(windowLocation);

      } else {
        startInternal(windowLocation);
      }

    } catch (Exception e) {
      // this will pass through to the first part of message
      // this handles errors that happen inside setup()
      e.printStackTrace();

      // make sure applet is in use
      if (applet != null) applet.finished = true;

      leechErr.println(PApplet.LEECH_WAKEUP);
      e.printStackTrace(this.leechErr);
    }
  }


  public void startPresenting() throws Exception {
    String command[] = new String[] {
      "java",
      "-Djava.library.path=" +
      sketch.libraryPath +
      File.pathSeparator + System.getProperty("java.library.path"),
      "-cp",
      sketch.classPath + Sketchbook.librariesClassPath,
      "processing.core.PApplet",
      PApplet.ARGS_EXTERNAL,
      PApplet.ARGS_PRESENT,
      PApplet.ARGS_PRESENT_BGCOLOR + "=" +
      Preferences.get("run.present.bgcolor"),
      PApplet.ARGS_DISPLAY + "=" +
      Preferences.get("run.display"),
      PApplet.ARGS_SKETCH_FOLDER + "=" +
      sketch.folder.getAbsolutePath(),
      sketch.mainClassName
    };

    process = Runtime.getRuntime().exec(command);
    processInput = new SystemOutSiphon(process.getInputStream());
    processError = new MessageSiphon(process.getErrorStream(), this);
    processOutput = process.getOutputStream();
  }


  public void startExternalRuntime(Point windowLocation) throws Exception {
    // if there was a saved location (this guy has been run more than
    // once) then windowLocation will be set to the last position of
    // the sketch window. this will be passed to the PApplet runner
    // using something like --external=e30,20 where the e stands for
    // exact. otherwise --external=x,y for just the regular positioning.
    Point editorLocation = editor.getLocation();
    String location =
      (windowLocation != null) ?
      (PApplet.ARGS_LOCATION + "=" +
       windowLocation.x + "," + windowLocation.y) :
      (PApplet.ARGS_EDITOR_LOCATION + "=" +
       editorLocation.x + "," + editorLocation.y);

    //System.out.println("library path is " + sketch.libraryPath);
    String command[] = new String[] {
      // this made the code folder bug go away, but killed stdio
      //"cmd", "/c", "start",
      "java",
      "-Djava.library.path=" +
      // sketch.libraryPath might be ""
      // librariesClassPath will always have sep char prepended
      sketch.libraryPath +
      File.pathSeparator + System.getProperty("java.library.path"),
      "-cp",
      sketch.classPath + Sketchbook.librariesClassPath,
      "processing.core.PApplet",
      location,
      PApplet.ARGS_EXTERNAL,
      PApplet.ARGS_DISPLAY + "=" + Preferences.get("run.display"),
      PApplet.ARGS_SKETCH_FOLDER + "=" + sketch.folder.getAbsolutePath(),
      sketch.mainClassName
    };
    //PApplet.printarr(command);
    //PApplet.println(PApplet.join(command, " "));

    process = Runtime.getRuntime().exec(command);
    processInput = new SystemOutSiphon(process.getInputStream());
    processError = new MessageSiphon(process.getErrorStream(), this);
    processOutput = process.getOutputStream();
  }


  public void startInternal(Point windowLocation) throws Exception {
    Point editorLocation = editor.getLocation();
    //Insets editorInsets = editor.getInsets();

    int windowX = editorLocation.x;
    int windowY = editorLocation.y + editor.getInsets().top;

    RunnerClassLoader loader = new RunnerClassLoader();
    Class c = loader.loadClass(sketch.mainClassName);
    applet = (PApplet) c.newInstance();

    applet.leechErr = leechErr;
    applet.folder = sketch.folder.getAbsolutePath();

    applet.init();
    //applet.start();

    while ((applet.width == 0) && !applet.finished) {
      try {
        if (applet.exception != null) {
          throw new RunnerException(applet.exception.getMessage());
        }
        Thread.sleep(5);
      } catch (InterruptedException e) { }
    }

    window = new Frame(sketch.name); // use ugly window
    ((Frame)window).setResizable(false);
    if (editor.icon != null) {
      ((Frame)window).setIconImage(editor.icon);
    }
    window.pack(); // to get a peer, size set later, need for insets

    window.addWindowListener(new WindowAdapter() {
        public void windowClosing(WindowEvent e) {
          stop();
          editor.doClose();
        }
      });

    applet.addKeyListener(new KeyAdapter() {
        public void keyPressed(KeyEvent e) {
          if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            stop();
            editor.doClose();
          }
        }
      });

    window.add(applet);

    Dimension screen =
      Toolkit.getDefaultToolkit().getScreenSize();

    window.setLayout(null);
    Insets insets = window.getInsets();

    int minW = Preferences.getInteger("run.window.width.minimum");
    int minH = Preferences.getInteger("run.window.height.minimum");
    int windowW =
      Math.max(applet.width, minW) + insets.left + insets.right;
    int windowH =
      Math.max(applet.height, minH) + insets.top + insets.bottom;

    if (windowX - windowW > 10) {  // if it fits to the left of the window
      window.setBounds(windowX - windowW, windowY, windowW, windowH);

    } else { // if it fits inside the editor window
      windowX = editorLocation.x + Preferences.GRID_SIZE * 2;  // 66
      windowY = editorLocation.y + Preferences.GRID_SIZE * 2;  // 66

      if ((windowX + windowW > screen.width - Preferences.GRID_SIZE) ||
          (windowY + windowH > screen.height - Preferences.GRID_SIZE)) {
        // otherwise center on screen
        windowX = (screen.width - windowW) / 2;
        windowY = (screen.height - windowH) / 2;
      }
      window.setBounds(windowX, windowY, windowW, windowH); //ww, wh);
    }

    Color windowBgColor = Preferences.getColor("run.window.bgcolor");
    window.setBackground(windowBgColor);

    int usableH = windowH - insets.top - insets.bottom;
    applet.setBounds((windowW - applet.width)/2,
                     insets.top + (usableH - applet.height) / 2,
                     windowW, windowH);

    applet.setVisible(true);  // no effect
    if (windowLocation != null) {
      window.setLocation(windowLocation);
    }
    window.show();
    applet.requestFocus();  // necessary for key events
  }


  public void stop() {
    // check for null in case stop is called during compilation
    if (applet != null) {
      applet.stop();

      // above avoids NullPointerExceptions
      // but still threading is too complex, and so
      // some boogers are being left behind
      applet = null;

    } else if (process != null) {  // running externally
      try {
        processOutput.write(PApplet.EXTERNAL_STOP);
        processOutput.flush();

      } catch (IOException e) {
        close();
      }
    }
  }


  public void close() {
    //if (window != null) window.hide();
    if (window != null) {
      //System.err.println("disposing window");
      window.dispose();
      window = null;
    }

    if (process != null) {
      try {
        process.destroy();
      } catch (Exception e) {
        //System.err.println("(ignored) error while destroying");
        //e.printStackTrace();
      }
      process = null;
    }
  }


  public void message(String s) {
    if (s.trim().length() == 0) return;
    //System.out.println("M" + s.length() + ":" + s);

    // this is PApplet sending a message (via System.out.println)
    // that signals that the applet has been quit.
    if (s.indexOf(PApplet.EXTERNAL_QUIT) == 0) {
      //System.out.println("external: quit");
      editor.doClose();
      return;
    }

    // this is the PApplet sending us a message that the applet
    // is being moved to a new window location
    if (s.indexOf(PApplet.EXTERNAL_MOVE) == 0) {
      String nums = s.substring(s.indexOf(' ') + 1);
      int space = nums.indexOf(' ');
      int left = Integer.parseInt(nums.substring(0, space));
      int top = Integer.parseInt(nums.substring(space + 1));
      editor.appletLocation = new Point(left, top);
      //System.out.println("external: move to " + left + " " + top);
      return;
    }

    // this is PApplet sending a message saying "i'm about to spew
    // a stack trace because an error occurred during PApplet.run()"
    if (s.indexOf(PApplet.LEECH_WAKEUP) == 0) {
      // newMessage being set to 'true' means that the next time
      // message() is called, expect the first line of the actual
      // error message & stack trace to be sent from the applet.
      newMessage = true;
      return;  // this line ignored
    }

    // these are used for debugging, in case there are concerns
    // that some errors aren't coming through properly
    /*
    if (s.length() > 2) {
      System.err.println(newMessage);
      System.err.println("message " + s.length() + ":" + s);
    }
    */
    // always shove out the mesage, since it might not fall under
    // the same setup as we're expecting
    System.err.print(s);
    //System.err.println("[" + s.length() + "] " + s);
    System.err.flush();

    // if s.length <=2, ignore it because that probably means
    // that it's just the platform line-terminators.
    //if (s.length() < 2) return;

    // annoying, because it seems as though the terminators
    // aren't being sent properly
    //System.err.println(s);

    //if (newMessage && s.length() > 2) {
    if (newMessage) {
      exception = new RunnerException(s);  // type of java ex
      exception.hideStackTrace = true;
      //System.out.println("setting ex type to " + s);
      newMessage = false;
      foundMessageSource = false;
      messageLineCount = 0;

    } else {
      messageLineCount++;

      /*
java.lang.NullPointerException
        at javatest.<init>(javatest.java:5)
        at Temporary_2425_1153.draw(Temporary_2425_1153.java:11)
        at PApplet.nextFrame(PApplet.java:481)
        at PApplet.run(PApplet.java:428)
        at java.lang.Thread.run(Unknown Source)
      */

      if (!foundMessageSource) {
        //    "     at javatest.<init>(javatest.java:5)"
        // -> "javatest.<init>(javatest.java:5)"
        int afterAt = s.indexOf("at") + 3;
        //if (afterAt == -1) {
        if (afterAt == 2) {  // means indexOf was -1
          System.err.println(s);
          return;
        }
        s = s.substring(afterAt + 1);

        //    "javatest.<init>(javatest.java:5)"
        // -> "javatest.<init>" and "(javatest.java:5)"
        int startParen = s.indexOf('(');
        // at javatest.<init>(javatest.java:5)
        String pkgClassFxn = null;
        //String fileLine = null;
        int codeIndex = -1;
        int lineIndex = -1;

        if (startParen == -1) {
          pkgClassFxn = s;

        } else {
          pkgClassFxn = s.substring(0, startParen);
          // "(javatest.java:5)"
          String fileAndLine = s.substring(startParen + 1);
          fileAndLine = fileAndLine.substring(0, fileAndLine.length() - 1);
          //if (!fileAndLine.equals("Unknown Source")) {
          // "javatest.java:5"
          int colonIndex = fileAndLine.indexOf(':');
          if (colonIndex != -1) {
            String filename = fileAndLine.substring(0, colonIndex);
            // "javatest.java" and "5"
            //System.out.println("filename = " + filename);
            //System.out.println("pre0 = " + sketch.code[0].preprocName);
            for (int i = 0; i < sketch.codeCount; i++) {
              if (sketch.code[i].preprocName.equals(filename)) {
                codeIndex = i;
                break;
              }
            }
            if (codeIndex != -1) {
              // lineIndex is 1-indexed, but editor wants zero-indexed
              lineIndex = Integer.parseInt(fileAndLine.substring(colonIndex + 1));
              exception = new RunnerException(exception.getMessage(),
                                           codeIndex, lineIndex - 1, -1);
              exception.hideStackTrace = true;
              foundMessageSource = true;
            }
          }
        }
        editor.error(exception);

      /*
      int index = s.indexOf(className + ".java");
      if (index != -1) {
        int len = (className + ".java").length();
        String lineNumberStr = s.substring(index + len + 1);
        index = lineNumberStr.indexOf(')');
        lineNumberStr = lineNumberStr.substring(0, index);
        try {
          exception.line = Integer.parseInt(lineNumberStr) - 1; //2;
        } catch (NumberFormatException e) { }
          //e.printStackTrace();  // a recursive error waiting to happen?
        // if nfe occurs, who cares, still send the error on up
        editor.error(exception);
      */

        /*
          // WARNING THESE ARE DISABLED!!
      } else if ((index = s.indexOf(className + ".class")) != -1) {
        // code to check for:
        // at Temporary_484_3845.loop(Compiled Code)
        // would also probably get:
        // at Temporary_484_3845.loop
        // which (i believe) is used by the mac and/or jview
        String functionStr = s.substring(index +
                                         (className + ".class").length() + 1);
        index = functionStr.indexOf('(');
        if (index != -1) {
          functionStr = functionStr.substring(0, index);
        }
        exception = new RunnerException(//"inside \"" + functionStr + "()\": " +
                                     exception.getMessage() +
                                     " inside " + functionStr + "() " +
                                     "[add Compiler.disable() to setup()]");
        editor.error(exception);
        // this will fall through in tihs example:
        // at Temporary_4636_9696.pootie(Compiled Code)
        // at Temporary_4636_9696.loop(Temporary_4636_9696.java:24)
        // because pootie() (re)sets the exception title
        // and throws it, but then the line number gets set
        // because of the line that comes after
        */

      } else if (messageLineCount > 5) {
        // this means the class name may not be mentioned
        // in the stack trace.. this is just a general purpose
        // error, but needs to make it through anyway.
        // so if five lines have gone past, might as well signal
        messageLineCount = -100;
        exception = new RunnerException(exception.getMessage());
        exception.hideStackTrace = true;
        editor.error(exception);

      } else {
        //System.err.print(s);
      }
      //System.out.println("got it " + s);
    }
  }


  //////////////////////////////////////////////////////////////


  /**
   * Siphons from an InputStream of System.out (from a Process)
   * and sends it to the real System.out.
   */
  class SystemOutSiphon implements Runnable {
    InputStream input;
    Thread thread;

    public SystemOutSiphon(InputStream input) {
      this.input = input;

      thread = new Thread(this);
      // unless this is set to min, it seems to hork the app
      // since it's in charge of stuffing the editor console with strings
      // maybe it's time to get rid of/fix that friggin console
      // ...disabled for 0075, with 0074's fix for code folder hanging
      // this only seems to make the console unresponsive
      //thread.setPriority(Thread.MIN_PRIORITY);
      thread.start();
    }

    public void run() {
      byte boofer[] = new byte[256];

      while (Thread.currentThread() == thread) {
        try {
          // can't use a buffered reader here because incremental
          // print statements are interesting too.. causes some
          // disparity with how System.err gets spewed, oh well.
          int count = input.read(boofer, 0, boofer.length);
          if (count == -1) {
            thread = null;

          } else {
            System.out.print(new String(boofer, 0, count));
            //System.out.flush();
          }

        } catch (IOException e) {
          // this is prolly because the app was quit & the stream broken
          //e.printStackTrace(System.out);
          //e.printStackTrace();
          thread = null;

        } catch (Exception e) {
          //System.out.println("SystemOutSiphon: i just died in your arms tonight");
          // on mac os x, this will spew a "Bad File Descriptor" ex
          // each time an external app is shut down.
          //e.printStackTrace();
          thread = null;
          //System.out.println("");
        }
        //System.out.println("SystemOutSiphon: out");
        //thread = null;
      }
    }
  }
}
