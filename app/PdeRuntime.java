// -*- Mode: JDE; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 2 -*-

import java.awt.*; // for window 
import java.awt.event.*; // also for window
import java.io.*;

// Runs the compiled java applet.
//
public class PdeRuntime implements PdeMessageConsumer {

  Process process;
  //KjcApplet applet;
  BApplet applet;
  PdeException exception;
  Window window;
  PdeEditor editor;
  PrintStream leechErr;
  String className;

  boolean newMessage;
  int messageLineCount;

  public PdeRuntime(PdeEditor editor, String className) {
    this.editor = editor;
    this.className = className;
  }

  public void start(Point windowLocation, PrintStream leechErr)
    throws PdeException {

    this.leechErr = leechErr;

    Frame frame = PdeBase.frame;
    Point parentLoc = frame.getLocation();
    Insets parentInsets = frame.getInsets();
	
    int x1 = parentLoc.x - 20;
    int y1 = parentLoc.y;

    try {
      if (PdeBase.getBoolean("play.external", false)) {
        String cmd = PdeBase.get("play.external.command");
        // "cmd /c " +   not helpful?
        process = Runtime.getRuntime().exec(cmd + " " + className + 
                                            " " + className +
                                            " " + x1 + " " + y1);
        new PdeMessageSiphon(process.getInputStream(), 
                             process.getErrorStream(),
                             this);

      } else {
        Class c = Class.forName(className);

        // to get rid of KjcEngine [fry]
        //applet = (KjcApplet) c.newInstance();
        applet = (BApplet) c.newInstance();

        // replaces setRuntime with BApplet having leechErr [fry]
        //applet.setRuntime(this);
        applet.leechErr = leechErr;

        // has to be before init
        applet.serialProperties(PdeBase.properties);
        applet.init();
        if (applet.exception != null) {
          if (applet.exception instanceof javax.comm.PortInUseException) {
            throw new PdeException("Another program is already " +
                                   "using the serial port.");
          } else {
            throw new PdeException(applet.exception.getMessage());
          }
        }
        applet.start();

        if (editor.presenting) {
          window = new Window(new Frame());
          window.addKeyListener(new KeyAdapter() {
              public void keyPressed(KeyEvent e) {
                //System.out.println("window got " + e);
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                  //editor.doClose();
                  //new DelayedClose(editor);
                  stop();
                  editor.doClose();
                }
              }
            });
          /*
            window.addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent e) {
            System.out.println(e);
            window.toFront();
            }});
          */

        } else {
          window = new Frame(editor.sketchName); // gonna use ugly windows instead
          ((Frame)window).setResizable(false);
          if (PdeBase.icon != null) ((Frame)window).setIconImage(PdeBase.icon);
          window.pack(); // to get a peer, size set later, need for insets

          window.addWindowListener(new WindowAdapter() {
              public void windowClosing(WindowEvent e) {
                stop();
                editor.doClose();
                //new DelayedClose(editor);
                //editor.doClose();
                //editor.doStop();
              }
            });
        }
        if (!(window instanceof Frame)) y1 += parentInsets.top;
        window.add(applet);

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

        //window.setLayout(new BorderLayout());
        window.setLayout(null);
        if (editor.presenting) {
          window.setBounds((screen.width - applet.width) / 2,
                           (screen.height - applet.height) / 2,
                           applet.width, applet.height);
          applet.setBounds(0, 0, applet.width, applet.height);

        } else {
          Insets insets = window.getInsets();
          //System.out.println(insets);
          int mw = PdeBase.getInteger("run.window.width.minimum", 120);
          int mh = PdeBase.getInteger("run.window.height.minimum", 120);
          int ww = Math.max(applet.width, mw) + insets.left + insets.right;
          int wh = Math.max(applet.height, mh) + insets.top + insets.bottom;
          if (x1 - ww > 10) {  // if it fits to the left of the window
            window.setBounds(x1 - ww, y1, ww, wh);

          } else { // if it fits inside the editor window
            x1 = parentLoc.x + PdeEditor.GRID_SIZE * 2;
            y1 = parentLoc.y + PdeEditor.GRID_SIZE * 2;

            if ((x1 + ww > screen.width - PdeEditor.GRID_SIZE) ||
                (y1 + wh > screen.height - PdeEditor.GRID_SIZE)) {
              // otherwise center on screen
              x1 = (screen.width - ww) / 2;
              y1 = (screen.height - wh) / 2;
            }
            window.setBounds(x1, y1, ww, wh);
          }

          Color windowBgColor = 
            PdeBase.getColor("run.window.bgcolor", SystemColor.control); 
          //new Color(102, 102, 102));
          window.setBackground(windowBgColor);
          //window.setBackground(SystemColor.windowBorder);
          //window.setBackground(SystemColor.control);

          applet.setBounds((ww - applet.width)/2, 
                           insets.top + ((wh-insets.top-insets.bottom) -
                                         applet.height)/2, ww, wh);
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
      //newMessage = true;
      leechErr.println(BApplet.LEECH_WAKEUP);
      e.printStackTrace(this.leechErr);
      //if (exception != null) throw exception;
    }
  }

  public void stop() {
    //System.out.println();
    //System.out.println("* stopping");

    // in case stop is called during compilation
    if (applet != null) applet.stop();
    //if (window != null) window.hide();

    // above avoids NullPointerExceptions 
    // but still threading is too complex, and so
    // some boogers are being left behind

    applet = null;
    //window = null;

    if (process != null) {  // running externally
      //System.out.println("killing external process");
      //process.destroy();

      //System.out.println("cutting umbilical cord");
      try {
        FileOutputStream fos = new FileOutputStream("die");
        fos.close();
      } catch (Exception e) {
        e.printStackTrace();
      }
      //try {
      //umbilical.close();
      //umbilical = null;
      //} catch (IOException e) {
      //e.printStackTrace();w
      //}
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
  }

  public void message(String s) {
    if (s.indexOf(BApplet.LEECH_WAKEUP) == 0) {
      newMessage = true;
      return;  // this line ignored
    }

    //} else {
    if (newMessage) {
      //System.out.println("making msg of " + s);
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
      }
      //System.out.println("got it " + s);
    }
  }
}
