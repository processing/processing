/* -*- mode: jde; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  PdeRuntime - runs compiled java applet
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

import java.awt.*; // for window 
import java.awt.event.*; // also for window
import java.io.*;
import java.lang.reflect.*;

#ifndef RXTX
import javax.comm.*;
#else
import gnu.io.*;
#endif


public class PdeRuntime implements PdeMessageConsumer {

  BApplet applet;
  PdeException exception;
  Window window;
  PdeEditor editor;
  PrintStream leechErr;
  String className;

  boolean newMessage;
  int messageLineCount;

  Process process;
  OutputStream processOutput;
  boolean externalRuntime;
  //String codeFolderPath;
  //String externalPaths;
  String libraryPath;
  String classPath;


  public PdeRuntime(PdeEditor editor, String className,
                    boolean externalRuntime, 
                    String classPath, String libraryPath) {
                    //String codeFolderPath, String externalPaths) {
    this.editor = editor;
    this.className = className;

    this.externalRuntime = externalRuntime;
    this.classPath = classPath;
    this.libraryPath = libraryPath;
    //this.codeFolderPath = codeFolderPath;
    //this.externalPaths = externalPaths;
  }


  public void start(Point windowLocation, PrintStream leechErr)
    throws PdeException {

    this.leechErr = leechErr;

    Point parentLoc = editor.getLocation();
    Insets parentInsets = editor.getInsets();

    int x1 = parentLoc.x - 20;
    int y1 = parentLoc.y;

    try {
      if (externalRuntime) {
        String command[] = new String[] { 
          "java",
          "-Djava.library.path=" + libraryPath,
          "-cp",
          classPath,
          "BApplet",
          BApplet.EXTERNAL_FLAG + ((windowLocation != null) ? 
                                   ("e" + 
                                    windowLocation.x + "," + 
                                    windowLocation.y) : 
                                   (x1 + "," + y1)),
          className
        };

        process = Runtime.getRuntime().exec(command);
        //new PdeMessageSiphon(process.getInputStream(), this);
        new SystemOutSiphon(process.getInputStream());
        new PdeMessageSiphon(process.getErrorStream(), this);
        processOutput = process.getOutputStream();

      } else {
        Class c = Class.forName(className);
        applet = (BApplet) c.newInstance();

        // replaces setRuntime with BApplet having leechErr [fry]
        //applet.setRuntime(this);
        applet.leechErr = leechErr;

        // has to be before init
        //applet.serialProperties(PdePreferences.properties);
        applet.init();
        if (applet.exception != null) {
          if (applet.exception instanceof PortInUseException) {
            throw new PdeException("Another program is already " +
                                   "using the serial port.");
          } else {
            throw new PdeException(applet.exception.getMessage());
          }
        }
        applet.start();

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
              //System.out.println("waiting to complete drawing");
              Thread.sleep(100);
            } catch (InterruptedException e) { }
          }
        }

        if (editor.presenting) {
          //window = new Window(new Frame());
          // toxi_030903: attach applet window to editor's presentation window
          window = new Window(editor.presentationWindow);
          // toxi_030903: moved keyListener to PdeEditor's presentationWindow

        } else {
          window = new Frame(editor.sketchName); // use ugly windows
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
          appletWidth = BApplet.DEFAULT_WIDTH;
          appletWidth = BApplet.DEFAULT_HEIGHT;
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
          int windowW = Math.max(applet.width, minW) + insets.left + insets.right;
          int windowH = Math.max(applet.height, minH) + insets.top + insets.bottom;

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

          Color windowBgColor = 
            PdePreferences.getColor("run.window.bgcolor");
          //new Color(102, 102, 102));
          window.setBackground(windowBgColor);
          //window.setBackground(SystemColor.windowBorder);
          //window.setBackground(SystemColor.control);

          applet.setBounds((windowW - applet.width)/2, 
                           insets.top + ((windowH - insets.top - insets.bottom) -
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
      //System.out.println("KJC RUNNING");

      //need to parse this code to give a decent error message
      //internal error
      //java.lang.NullPointerException
      //  at ProcessingApplet.colorMode(ProcessingApplet.java:652)
      //  at Temporary_203_1176.setup(Temporary_203_1176.java:3)

    } catch (Exception e) {
      // this will pass through to the first part of message
      // this handles errors that happen inside setup()

      // mod by fry for removal of KjcEngine
      applet.finished = true;
      leechErr.println(BApplet.LEECH_WAKEUP);
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

      /*
      try {
        FileOutputStream fos = new FileOutputStream("die");
        fos.close();
      } catch (Exception e) {
        e.printStackTrace();
      }
      */
    }

    //System.gc();
    //System.out.println("* stopped");

    //System.out.println("thread count: " + Thread.activeCount());
    //System.out.println();
    //System.out.println();
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
    if (s.indexOf(BApplet.EXTERNAL_QUIT) == 0) {
      //close();
      //System.out.println("got proper quit message");
      editor.doClose();
      return;
    }

    if (s.indexOf(BApplet.EXTERNAL_MOVE) == 0) {
      String nums = s.substring(s.indexOf(' ') + 1);
      int space = nums.indexOf(' ');
      int left = Integer.parseInt(nums.substring(0, space));
      int top = Integer.parseInt(nums.substring(space + 1));
      editor.appletLocation = new Point(left, top);
      //System.out.println("wanna move to " + left + " " + top);
      return;
    }

    //System.err.println("message " + s.length() + ":" + s);
    if (s.length() > 2) System.err.println(s);

    if (s.indexOf(BApplet.LEECH_WAKEUP) == 0) {
      //System.err.println("got wakeup");
      newMessage = true;
      return;  // this line ignored
    }

    // if s.length <=2, that probably means that it's just the platform
    // line-terminators.  ignore it.
    if (newMessage && s.length() > 2) {
      exception = new PdeException(s);  // type of java ex
      //System.out.println("setting ex type to " + s);
      newMessage = false;
      messageLineCount = 0;

    } else {
      messageLineCount++;

      int index = s.indexOf(className + ".java");
      //System.out.println("> " + index + " " + s);
      if (index != -1) {
        int len = (className + ".java").length();
        String lineNumberStr = s.substring(index + len + 1);
        index = lineNumberStr.indexOf(')');
        lineNumberStr = lineNumberStr.substring(0, index);
        //System.err.println("error line is: " + lineNumberStr);
        try {
          exception.line = Integer.parseInt(lineNumberStr) - 1; //2;
          //System.out.println("exception in RUNNING");
          editor.error(exception);
        } catch (NumberFormatException e) {  
          e.printStackTrace();
        }
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

      } else if (messageLineCount > 5) {
        // this means the class name may not be mentioned 
        // in the stack trace.. this is just a general purpose
        // error, but needs to make it through anyway.
        // so if five lines have gone past, might as well signal
        //System.out.println("signalling");
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
      thread.start();
    }

    public void run() {
      byte boofer[] = new byte[1024];

      try {
        while (true) {
          //int count = input.available();
          //int offset = 0; 
          int count = input.read(boofer, 0, boofer.length);
          if (count == -1) break;
          System.out.print(new String(boofer, 0, count));
        }

        /*
        int c;
        while ((c = input.read()) != -1) {
          System.out.print((char) c);
        }
        */
      } catch (Exception e) { 
        System.err.println("SystemOutSiphon error " + e);
        e.printStackTrace();
      }
    }
  }
}
