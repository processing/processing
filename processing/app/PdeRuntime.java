/* -*- mode: jde; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  PdeRuntime - runs compiled java applet
  Part of the Processing project - http://processing.org

  Except where noted, code is written by Ben Fry and is
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

import processing.core.*;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.lang.reflect.*;


public class PdeRuntime implements PdeMessageConsumer {

  PApplet applet;
  PdeException exception;
  Window window;
  PrintStream leechErr;
  //String className;

  PdeEditor editor;
  PdeSketch sketch;

  boolean newMessage;
  int messageLineCount;
  boolean foundMessageSource;

  Process process;
  OutputStream processOutput;

  //boolean externalRuntime;
  //String libraryPath;
  //String classPath;


  public PdeRuntime(PdeSketch sketch, PdeEditor editor) {
    this.sketch = sketch;
    this.editor = editor;
  }


  public void start(Point windowLocation) throws PdeException {

    this.leechErr = new PrintStream(new PdeMessageStream(this));

    Point parentLoc = editor.getLocation();
    Insets parentInsets = editor.getInsets();

    int x1 = parentLoc.x - 20;
    int y1 = parentLoc.y;

    try {
      if (sketch.externalRuntime) {
        // if there was a saved location (this guy has been run more than
        // once) then windowLocation will be set to the last position of
        // the sketch window. this will be passed to the PApplet runner
        // using something like --external=e30,20 where the e stands for
        // exact. otherwise --external=x,y for just the regular positioning.
        /*
        String location = (windowLocation != null) ? 
          (PApplet.EXTERNAL_EXACT_LOCATION + 
           windowLocation.x + "," + windowLocation.y) : 
          (x1 + "," + y1);      
        */
        String location = 
          (windowLocation != null) ? 
          (PApplet.EXT_EXACT_LOCATION + 
           windowLocation.x + "," + windowLocation.y) :
          (PApplet.EXT_LOCATION + x1 + "," + y1);

        String command[] = new String[] { 
          "java",
          "-Djava.library.path=" + sketch.libraryPath,  // might be ""
          "-cp",
          sketch.classPath,
          "processing.core.PApplet",
          location,
          PApplet.EXT_SKETCH_FOLDER + sketch.folder.getAbsolutePath(),
          sketch.mainClassName
        };

        //PApplet.println(PApplet.join(command, " "));
        process = Runtime.getRuntime().exec(command);
        new SystemOutSiphon(process.getInputStream());
        new PdeMessageSiphon(process.getErrorStream(), this);
        processOutput = process.getOutputStream();

      } else {  // !externalRuntime
        //Class c = Class.forName(className);
        Class c = Class.forName(sketch.mainClassName);
        applet = (PApplet) c.newInstance();

        // replaces setRuntime with PApplet having leechErr [fry]
        applet.leechErr = leechErr;
        applet.folder = sketch.folder.getAbsolutePath();

        // has to be before init
        //applet.serialProperties(PdePreferences.properties);
        applet.init();
        if (applet.exception != null) {
          /*
            TODO: fix me
          if (applet.exception instanceof PortInUseException) {
            throw new PdeException("Another program is already " +
                                   "using the serial port.");
          } else {
          */
          throw new PdeException(applet.exception.getMessage());
        }
        applet.start();

        /*
          // appears to be no longer necessary now that draw()
          // is gonna be run inside of setup()

        // check to see if it's a draw mode applet
        boolean drawMode = false;
        try {
          Method meth[] = c.getDeclaredMethods();
          for (int i = 0; i < meth.length; i++) {
            //System.out.println(meth[i].getName());
            if (meth[i].getName().equals("draw")) drawMode = true;
          }
        } catch (SecurityException e) { 
          e.printStackTrace();
        }
        // if it's a draw mode app, don't even show on-screen
        // until it's finished rendering, otherwise the width/height
        // may not have been properly set.
        if (drawMode) {
          //System.out.println("draw mode");
          while ((applet.frame != 1) && (!applet.finished)) {
            try {
              Thread.sleep(100);
            } catch (InterruptedException e) { }
          }
        }
        */

        if (editor.presenting) {
          //window = new Window(new Frame());
          // toxi_030903: attach applet window to editor's presentation window
          window = new Window(editor.presentationWindow);
          // toxi_030903: moved keyListener to PdeEditor's presentationWindow

        } else {
          //window = new Frame(sketch.name); // use ugly windows
          window = new Frame(sketch.name); // use ugly windows
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

          // toxi_030903: only attach keyListener if not in presentation mode
          // else events are coming directly from editor.presentationWindow
          applet.addKeyListener(new KeyAdapter() {
              public void keyPressed(KeyEvent e) {
                //System.out.println("applet got " + e);
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                  stop();
                  editor.doClose();
                  //new DelayedClose(editor);
                  //editor.doClose();
                }
                //System.out.println("consuming1");
                //e.consume();
                //System.out.println("consuming2");
              }
            });
          y1 += parentInsets.top;
        }
        // toxi_030903: moved this in the above else branch
        // if (!(window instanceof Frame)) y1 += parentInsets.top;

        window.add(applet);

        // @#$((* java 1.3
        // removed because didn't seem to be needed anymore
        // also, was causing offset troubles
        /*
          window.addMouseListener(applet);
          window.addMouseMotionListener(applet);
          window.addKeyListener(applet);
        */

        Dimension screen = 
          Toolkit.getDefaultToolkit().getScreenSize();

        //System.out.println(SystemColor.windowBorder.toString());

        /*
        int appletWidth = applet.width;
        int appletHeight = applet.height;
        if ((appletWidth == 0) || (appletHeight == 0)) {
          appletWidth = PApplet.DEFAULT_WIDTH;
          appletWidth = PApplet.DEFAULT_HEIGHT;
        }
        */

        window.setLayout(null);
        if (editor.presenting) {
          window.setBounds((screen.width - applet.width) / 2,
                           (screen.height - applet.height) / 2,
                           applet.width, applet.height);
          applet.setBounds(0, 0, applet.width, applet.height);

        } else {
          Insets insets = window.getInsets();
          //System.out.println(insets);

          int minW = PdePreferences.getInteger("run.window.width.minimum");
          int minH = PdePreferences.getInteger("run.window.height.minimum");
          int windowW = 
            Math.max(applet.width, minW) + insets.left + insets.right;
          int windowH = 
            Math.max(applet.height, minH) + insets.top + insets.bottom;

          if (x1 - windowW > 10) {  // if it fits to the left of the window
            window.setBounds(x1 - windowW, y1, windowW, windowH);
            //windowX = x1 - ww;
            //windowY = y1;

          } else { // if it fits inside the editor window
            x1 = parentLoc.x + PdePreferences.GRID_SIZE * 2;  // 66
            y1 = parentLoc.y + PdePreferences.GRID_SIZE * 2;  // 66

            if ((x1 + windowW > screen.width - PdePreferences.GRID_SIZE) ||
                (y1 + windowH > screen.height - PdePreferences.GRID_SIZE)) {
              // otherwise center on screen
              x1 = (screen.width - windowW) / 2;
              y1 = (screen.height - windowH) / 2;
            }
            window.setBounds(x1, y1, windowW, windowH); //ww, wh);
          }

          Color windowBgColor = PdePreferences.getColor("run.window.bgcolor");
          window.setBackground(windowBgColor);

          applet.setBounds((windowW - applet.width)/2, 
                           insets.top + ((windowH - 
                                          insets.top - insets.bottom) -
                                         applet.height)/2, 
                           windowW, windowH);
        }

        applet.setVisible(true);  // no effect
        if (windowLocation != null) {
          window.setLocation(windowLocation);
        }
        window.show();
        applet.requestFocus();  // necessary for key events
      }

    } catch (Exception e) {
      // this will pass through to the first part of message
      // this handles errors that happen inside setup()

      // mod by fry for removal of KjcEngine
      applet.finished = true;
      leechErr.println(PApplet.LEECH_WAKEUP);
      e.printStackTrace(this.leechErr);
    }
  }

  public void stop() {
    //System.out.println();
    //System.out.println("PdeRuntime.stop()");

    // check for null in case stop is called during compilation
    if (applet != null) {
      applet.stop();
      //if (window != null) window.hide();

      // above avoids NullPointerExceptions 
      // but still threading is too complex, and so
      // some boogers are being left behind

      applet = null;
      //window = null;

    } else if (process != null) {  // running externally
      //System.out.println("killing external process");

      try {
        //System.out.println("writing to stop process");
        processOutput.write('s');
        processOutput.flush();

      } catch (IOException e) {
        //System.err.println("error stopping external applet");
        //e.printStackTrace();
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
    //System.out.println("M" + s.length() + ":" + s);
    // this is PApplet sending a message (via System.out.println)
    // that signals that the applet has been quit.
    if (s.indexOf(PApplet.EXTERNAL_QUIT) == 0) {
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
      //System.out.println("wanna move to " + left + " " + top);
      return;
    }

    //System.err.println("message " + s.length() + ":" + s);
    // is this only for debugging, or?
    if (s.length() > 2) System.err.println(s);

    // this is PApplet sending a message saying "i'm about to spew 
    // a stack trace because an error occurred during PApplet.run()"
    if (s.indexOf(PApplet.LEECH_WAKEUP) == 0) {
      // newMessage being set to 'true' means that the next time 
      // message() is called, expect the first line of the actual
      // error message & stack trace to be sent from the applet.
      newMessage = true;
      return;  // this line ignored
    }

    // if s.length <=2, ignore it because that probably means 
    // that it's just the platform line-terminators.
    if (newMessage && s.length() > 2) {
      exception = new PdeException(s);  // type of java ex
      //System.out.println("setting ex type to " + s);
      newMessage = false;
      foundMessageSource = false;
      messageLineCount = 0;

    } else {
      messageLineCount++;

      // TODO this is insufficient. need to cycle through the 
      // different classes that are currently loaded and see if
      // there is an error in one of them.
      //String className = sketch.mainClassName;

      //\s+at\s([\w\d\._]+)\.([\<\w\d_]+)\(([\w\d_].java\:(\d+)

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
              //System.out.println("code/line is " + codeIndex + " " + lineIndex);
              exception = new PdeException(exception.getMessage(),
                                           codeIndex, lineIndex - 1, -1);
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
        exception = new PdeException(//"inside \"" + functionStr + "()\": " +
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
        exception = new PdeException(exception.getMessage());
        editor.error(exception);

      } else {
        //System.err.print(s);
      } 
      //System.out.println("got it " + s);
    }
  }


  class SystemOutSiphon implements Runnable {
    InputStream input;
    Thread thread;


    public SystemOutSiphon(InputStream input) {
      this.input = input;

      thread = new Thread(this);
      thread.setPriority(Thread.MIN_PRIORITY);
      thread.start();
    }

    public void run() {
      byte boofer[] = new byte[1024];

      // read, block until something good comes through
      while (Thread.currentThread() == thread) {
        try {
          int count = input.read(boofer, 0, boofer.length);
          if (count == -1) thread = null;
          //System.out.print("bc" + count + " " + new String(boofer, 0, count));
          //PApplet.println(boofer);

        } catch (IOException e) {
          thread = null;
        }
        //System.out.println("SystemOutSiphon: out");
        //thread = null;
      }        
    }


      /*
      //while (Thread.currentThread() == thread) {
      //try {
      //while (true) {
          //int count = input.available();
            //int offset = 0; 
          int count = input.read(boofer, 0, boofer.length);
          if (count == -1) {
            System.out.println("SystemOutSiphon: out");
            thread = null;
          }
          //if (count != -1) {
          if (count > 0) {
            System.out.print(new String(boofer, 0, count));
          }
          //}
            //}

        } catch (Exception e) { 
          System.err.println("SystemOutSiphon error " + e);
          e.printStackTrace();
        }
        //Thread.yield();
        try {
          Thread.sleep(100);
        } catch (InterruptedException e) { }
      }
    }
      */

  }
}
