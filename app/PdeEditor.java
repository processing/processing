/* -*- mode: jde; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  PdeEditor - main editor panel for the processing ide
  Part of the Processing project - http://Proce55ing.net

  Copyright (c) 2001-03 
  Ben Fry, Massachusetts Institute of Technology and 
  Casey Reas, Interaction Design Institute Ivrea

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

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;
import java.util.zip.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;

import com.oroinc.text.regex.*;

#ifdef MACOS
import com.apple.mrj.*;
#endif


public class PdeEditor extends JPanel {
  // otherwise, if the window is resized with the message label
  // set to blank, it's preferredSize() will be fukered
  static final String EMPTY = "                                                                                                                                                             ";

  static final String HISTORY_SEPARATOR = 
    "#################################################";

  static final int SK_NEW  = 1;
  static final int SK_OPEN = 2;
  static final int DO_OPEN = 3;
  static final int DO_QUIT = 4;
  int checking;
  String openingPath; 
  String openingName;

  static final int RUN      = 5; // for history
  static final int SAVE     = 6;
  static final int AUTOSAVE = 7;
  static final int BEAUTIFY = 8;

  PdeEditorButtons buttons;
  PdeEditorHeader header;
  PdeEditorStatus status;
  PdeEditorConsole console;

  JSplitPane splitPane;
  JPanel consolePanel;

  JEditTextArea textarea;

  boolean externalEditor;

  // currently opened program
  String userName;   // user currently logged in
  String sketchName; // name of the file (w/o pde if a sketch)
  File sketchFile;   // the .pde file itself
  File sketchDir;    // if a sketchbook project, the parent dir
  boolean sketchModified;

  File historyFile;
  //OutputStream historyStream;
  //PrintWriter historyWriter;
  String historyLast;

  //String lastDirectory;
  //String lastFile;

  //PdeRunner runner;
  Point appletLocation; //= new Point(0, 0);
  Point presentLocation; // = new Point(0, 0);

  Window presentationWindow;

  RunButtonWatcher watcher;

  PdeRuntime pdeRuntime;

  static final int GRID_SIZE  = 33;
  static final int INSET_SIZE = 5;

  boolean running;
  boolean presenting;
  boolean renaming;

  PdeBase base;

  //PrintStream leechErr;
  PdeMessageStream messageStream;

  // location for lib/build, contents for which will be emptied
  String tempBuildPath;

  static final String TEMP_CLASS = "Temporary";


  public PdeEditor(PdeBase base) {
    this.base = base;

    setLayout(new BorderLayout());

    Panel leftPanel = new Panel();
    leftPanel.setLayout(new BorderLayout());

    // set bgcolor of buttons here, b/c also used for empty component
    buttons = new PdeEditorButtons(this);
    Color buttonBgColor = 
      PdeBase.getColor("editor.buttons.bgcolor", new Color(0x99, 0x99, 0x99));
    buttons.setBackground(buttonBgColor);
    leftPanel.add("North", buttons);
    Label dummy = new Label();
    dummy.setBackground(buttonBgColor);
    leftPanel.add("Center", dummy);

    add("West", leftPanel);

    JPanel rightPanel = new JPanel();
    rightPanel.setLayout(new BorderLayout());

    header = new PdeEditorHeader(this);
    rightPanel.add(header, BorderLayout.NORTH);

    textarea = new JEditTextArea();
    textarea.setTokenMarker(new PdeTokenMarker());
 
    // assemble console panel, consisting of status area and the console itself
    consolePanel = new JPanel();
    //System.out.println(consolePanel.getInsets());
    consolePanel.setLayout(new BorderLayout());

    status = new PdeEditorStatus(this);
    consolePanel.add(status, BorderLayout.NORTH);
    console = new PdeEditorConsole(this);
    consolePanel.add(console, BorderLayout.CENTER);

    splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                               textarea, consolePanel);

    splitPane.setOneTouchExpandable(true);
    // repaint child panes while resizing
    splitPane.setContinuousLayout(true);
    // if window increases in size, give all of increase to textarea (top pane)
    splitPane.setResizeWeight(1D);

    // to fix ugliness.. normally macosx java 1.3 puts an 
    // ugly white border around this object, so turn it off.
    if (PdeBase.platform == PdeBase.MACOSX) {
      splitPane.setBorder(null);
    }

    // the default size on windows is too small and kinda ugly
    int dividerSize = PdeBase.getInteger("editor.divider.size", 0);
    if (dividerSize != 0) {
      splitPane.setDividerSize(dividerSize);
    }

    rightPanel.add(splitPane, BorderLayout.CENTER);

    add("Center", rightPanel);

    // hopefully these are no longer needed w/ swing
    // (that was wishful thinking, they still are, until we switch to jedit)
    PdeEditorListener listener = new PdeEditorListener(this, textarea);
    textarea.pdeEditorListener = listener;

    Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
    if ((PdeBase.platform == PdeBase.MACOSX) ||
        (PdeBase.platform == PdeBase.MACOS9)) {
      presentationWindow = new Frame();

      // mrj is still (with version 2.2.x) a piece of shit, 
      // and doesn't return valid insets for frames
      //presentationWindow.pack(); // make a peer so insets are valid
      //Insets insets = presentationWindow.getInsets();
      // the extra +20 is because the resize boxes intrude
      Insets insets = new Insets(21, 5, 5 + 20, 5);

      presentationWindow.setBounds(-insets.left, -insets.top, 
                                   screen.width + insets.left + insets.right, 
                                   screen.height + insets.top + insets.bottom);
    } else {
      presentationWindow = new Frame();
      ((Frame)presentationWindow).setUndecorated(true);
      presentationWindow.setBounds(0, 0, screen.width, screen.height);
    }

    Label label = new Label("stop");
    label.addMouseListener(new MouseAdapter() {
        public void mousePressed(MouseEvent e) {
          //System.out.println("got stop");
          //doStop();
          setVisible(true);
          doClose();

          //#ifdef JDK13
          // move editor to front in case it was hidden
          //PdeBase.frame.setState(Frame.NORMAL);
          //base.setState(Frame.NORMAL);
          //#endif
        }});

    Dimension labelSize = new Dimension(60, 20);
    presentationWindow.setLayout(null);
    presentationWindow.add(label);
    label.setBounds(5, screen.height - 5 - labelSize.height, 
                    labelSize.width, labelSize.height);

    Color presentationBgColor = 
      PdeBase.getColor("run.present.bgcolor", new Color(102, 102, 102));
    presentationWindow.setBackground(presentationBgColor);

    textarea.addFocusListener(new FocusAdapter() {
        public void focusGained(FocusEvent e) {
          if (presenting == true) {
            try {
              presentationWindow.toFront();
              pdeRuntime.applet.requestFocus();
            } catch (Exception ex) { }
          }
        }
      });

    this.addFocusListener(new FocusAdapter() {
        public void focusGained(FocusEvent e) {
          if (presenting == true) {
            try {
              presentationWindow.toFront();
              pdeRuntime.applet.requestFocus();
            } catch (Exception ex) { }
          }
        }
      });

    // moved from the PdeRuntime window to the main presentation window
    // [toxi 030903]
    presentationWindow.addKeyListener(new KeyAdapter() {
        public void keyPressed(KeyEvent e) {
          //System.out.println("window got " + e);
          if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            pdeRuntime.stop();
            doClose();
          } else {
            // pass on the event to the applet [toxi 030903]
            pdeRuntime.applet.keyPressed(e);
          }
        }
      });
  }


  public void init() {
    // load the last program that was in use

    Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
    int windowX = -1, windowY = 0, windowW = 0, windowH = 0;

    Properties skprops = new Properties();
    try {
      if (PdeBase.platform == PdeBase.MACOSX) {
        //String pkg = "Proce55ing.app/Contents/Resources/Java/";
        //skprops.load(new FileInputStream(pkg + "sketch.properties"));
        skprops.load(new FileInputStream("lib/sketch.properties"));

      } else if (PdeBase.platform == PdeBase.MACOS9) {
        skprops.load(new FileInputStream("lib/sketch.properties"));

      } else {
        skprops.load(getClass().getResource("sketch.properties").openStream());
      }

      windowX = Integer.parseInt(skprops.getProperty("window.x", "-1"));
      windowY = Integer.parseInt(skprops.getProperty("window.y", "-1"));
      windowW = Integer.parseInt(skprops.getProperty("window.w", "-1"));
      windowH = Integer.parseInt(skprops.getProperty("window.h", "-1"));

      // if screen size has changed, the window coordinates no longer
      // make sense, so don't use them unless they're identical
      int screenW = Integer.parseInt(skprops.getProperty("screen.w", "-1"));
      int screenH = Integer.parseInt(skprops.getProperty("screen.h", "-1"));

      if ((screen.width != screenW) || (screen.height != screenH)) {
        // probably not valid for this machine, so invalidate sizing
        windowX = -1;
      }

      String name = skprops.getProperty("sketch.name");
      String path = skprops.getProperty("sketch.directory");
      String user = skprops.getProperty("user.name");

      String what = path + File.separator + name + ".pde";

      if (windowX != -1) {
        String dividerLocation = 
          skprops.getProperty("editor.divider.location");
        if (dividerLocation != null) {
          splitPane.setDividerLocation(Integer.parseInt(dividerLocation));
        }
      }

      if (new File(what).exists()) {
        userName = user;
        skOpen(path, name);

      } else {
        userName = "default";
        skNew();
      }

      String serialPort = skprops.getProperty("serial.port");
      if (serialPort != null) {
        PdeBase.properties.put("serial.port", serialPort);
      }

      boolean ee = new Boolean(skprops.getProperty("editor.external", "false")).booleanValue();
      setExternalEditor(ee);

    } catch (Exception e) { 
      // this exception doesn't matter, it's just the normal course of things
      // the app reaches here when no sketch.properties file exists
      //e.printStackTrace();

      // indicator that this is the first time this feller has used p5
      PdeBase.firstTime = true;

      // even if folder for 'default' user doesn't exist, or
      // sketchbook itself is missing, mkdirs() will make it happy
      userName = "default";

      // doesn't exist, not available, make my own
      skNew();
    }

    if (windowX == -1) {
      //System.out.println("using defaults for window size");
      windowW = PdeBase.getInteger("window.width", 500);
      windowH = PdeBase.getInteger("window.height", 500);
      windowX = (screen.width - windowW) / 2;
      windowY = (screen.height - windowH) / 2;
    }
    //PdeBase.frame.setBounds(windowX, windowY, windowW, windowH);
    base.setBounds(windowX, windowY, windowW, windowH);
    //rebuildSketchbookMenu(PdeBase.sketchbookMenu);
  }


  protected void changeText(String what, boolean emptyUndo) {
    textarea.setText(what);

    // TODO need to wipe out the undo state here
    if (emptyUndo) PdeBase.undo.discardAllEdits();

    textarea.select(0, 0);    // move to the beginning of the document
    textarea.requestFocus();  // get the caret blinking
  }


  // mode is RUN, SAVE or AUTO
  public void makeHistory(String program, int mode) {
    if (!base.recordingHistory) return;
    //if (historyLast.equals(program) && !externalEditor) return;
    if ((historyLast != null) &&
        (historyLast.equals(program))) return;

    String modeStr = null;
    switch (mode) {
    case RUN: modeStr = "run"; break;
    case SAVE: modeStr = "save"; break;
    case AUTOSAVE: modeStr = "autosave"; break;
    case BEAUTIFY: modeStr = "beautify"; break;
    }

    try {
      ByteArrayOutputStream old = null;
      if (historyFile.exists()) {
        InputStream oldStream = new GZIPInputStream(new BufferedInputStream(new FileInputStream(historyFile)));
        old = new ByteArrayOutputStream();

        int c = oldStream.read();
        while (c != -1) {
          old.write(c);
          c = oldStream.read();
        }
        //return out.toByteArray();
        oldStream.close();
      }

      OutputStream historyStream = 
        new GZIPOutputStream(new FileOutputStream(historyFile));

      if (old != null) {
        historyStream.write(old.toByteArray());
      }
      PrintWriter historyWriter = 
        new PrintWriter(new OutputStreamWriter(historyStream));

      historyWriter.println();
      historyWriter.println(HISTORY_SEPARATOR);

      Calendar now = Calendar.getInstance();
      // 2002 06 18  11 43 29
      // when listing, study for descrepancies.. if all are
      // 2002, then don't list the year and soforth.
      // for the other end, if all minutes are unique, 
      // then don't show seconds
      int year = now.get(Calendar.YEAR);
      int month = now.get(Calendar.MONTH) + 1;
      int day = now.get(Calendar.DAY_OF_MONTH);
      int hour = now.get(Calendar.HOUR_OF_DAY);
      int minute = now.get(Calendar.MINUTE);
      int second = now.get(Calendar.SECOND);
      String parseDate = year + " " + month + " " + day + " " +
        hour + " " + minute + " " + second;

      String readableDate = now.getTime().toString();

      // increment this so sketchbook won't be mangled 
      // each time this format has to change
      String historyVersion = "1";
      //Date date = new Date();
      //String datestamp = date.toString();

      historyWriter.println(historyVersion + " " + modeStr + " - " + 
                            parseDate + " - " + readableDate);
      historyWriter.println();
      historyWriter.println(program);
      historyWriter.flush();  // ??
      historyLast = program;

      //JMenuItem menuItem = new JMenuItem(modeStr + " - " + readableDate);
      MenuItem menuItem = new MenuItem(modeStr + " - " + readableDate);
      menuItem.addActionListener(base.historyMenuListener);
      base.historyMenu.insert(menuItem, 0);

      historyWriter.flush();
      historyWriter.close();

    } catch (IOException e) {
      e.printStackTrace();
    }
  }


  public void retrieveHistory(String selection) {
    //System.out.println("sel '" + selection + "'");
    String readableDate = 
      selection.substring(selection.indexOf("-") + 2);

    // make history for the current guy
    makeHistory(textarea.getText(), AUTOSAVE);
    // mark editor text as having been edited

    try {
      BufferedReader reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(historyFile))));
      String line = null;

      int historyCount = 0;
      String historyList[] = new String[100];

      try {
        boolean found = false;
        while ((line = reader.readLine()) != null) {
          //System.out.println("->" + line);
          if (line.equals(PdeEditor.HISTORY_SEPARATOR)) {
            line = reader.readLine();
            if (line.indexOf(readableDate) != -1) {  // this is the one
              found = true;
              break;
            }
          }
        }
        if (found) {
          // read lines until the next separator
          line = reader.readLine(); // ignored
          //String sep = System.getProperty("line.separator");
          StringBuffer buffer = new StringBuffer();
          while ((line = reader.readLine()) != null) {
            if (line.equals(PdeEditor.HISTORY_SEPARATOR)) break;
            //textarea.append(line + sep);
            //buffer.append(line + sep);  // JTextPane wants only \n going in
            buffer.append(line + "\n");
            //System.out.println("'" + line + "'");
          }
          //textarea.editorSetText(buffer.toString());
          changeText(buffer.toString(), true);
          historyLast = textarea.getText();
          setSketchModified(false);

        } else {
          System.err.println("couldn't find history entry for " + 
                             "'" + readableDate + "'");
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }


  // in an advanced program, the returned classname could be different,
  // which is why the className is set based on the return value.
  // @param exporting if set, then code is cleaner, 
  //                  but line numbers won't line up properly.
  //                  also modifies which imports (1.1 only) are included.
  // @return null if compilation failed, className if not
  //
  protected String build(String program, String className,
                         String buildPath, boolean exporting) 
    throws PdeException, Exception {

    // true if this should extend BApplet instead of BAppletGL
    boolean extendsNormal = base.normalItem.getState();

      PdePreprocessor preprocessor = null;
      if (PdeBase.getBoolean("preprocessor.antlr", true)) {
        preprocessor = new PdePreprocessor(program, buildPath);
        try {
          className = 
            preprocessor.writeJava(className, extendsNormal, false);

        } catch (antlr.RecognitionException ae) {
          // this even returns a column
          throw new PdeException(ae.getMessage(), 
                                 ae.getLine() - 1, ae.getColumn());

        } catch (PdeException pe) {
          throw pe;
        } catch (Exception ex) {
          System.err.println("Uncaught exception type:" + ex.getClass());
          ex.printStackTrace();
          throw new PdeException(ex.toString());
        }
      } else {  // use the old oro processor (yech)
        preprocessor = new PdePreprocessorOro(program, buildPath);
        className = 
          preprocessor.writeJava(className, extendsNormal, false);
      }

      // compile the program
      //
      PdeCompiler compiler = 
        new PdeCompiler(buildPath, className, this);
      // macos9 now officially broken.. see PdeCompilerJavac
      //PdeCompiler compiler = 
      //  ((PdeBase.platform == PdeBase.MACOS9) ? 
      //   new PdeCompilerJavac(buildPath, className, this) :
      //   new PdeCompiler(buildPath, className, this));

      // run the compiler, and funnel errors to the leechErr
      // which is a wrapped around 
      // (this will catch and parse errors during compilation
      // the messageStream will call message() for 'compiler')
      messageStream = new PdeMessageStream(compiler);
      //PrintStream leechErr = new PrintStream(messageStream);
      //boolean result = compiler.compileJava(leechErr);
      //return compiler.compileJava(leechErr);
      boolean success = 
        compiler.compileJava(new PrintStream(messageStream));

      return success ? className : null;
  }


  /**
   * Preprocess, Compile, and Run the current code.
   *
   * There are three main parts to this process:
   *
   *   (0. if not java, then use another 'engine'.. i.e. python)
   *
   *    1. do the p5 language preprocessing
   *       this creates a working .java file in a specific location
   *       better yet, just takes a chunk of java code and returns a 
   *       new/better string editor can take care of saving this to a 
   *       file location
   *
   *    2. compile the code from that location
   *       catching errors along the way
   *       currently done with kjc, but would be nice to use jikes
   *       placing it in a ready classpath, or .. ?
   *
   *    3. run the code 
   *       needs to communicate location for window 
   *       and maybe setup presentation space as well
   *       currently done internally
   *       would be nice to use external (at least on non-os9)
   *
   *    X. afterwards, some of these steps need a cleanup function
   */
  public void doRun(boolean present) {
    //System.out.println(System.getProperty("java.class.path"));

    //doStop();
    doClose();
    running = true;
    //System.out.println("RUNNING");
    buttons.run();

    if (externalEditor) {
      // history gets screwed by the open..
      String historySaved = historyLast;
      handleOpen(sketchName, sketchFile, sketchDir);
      historyLast = historySaved;
    }

    presenting = present;

    try {
      if (presenting) {
        presentationWindow.show();
        presentationWindow.toFront();
        //doRun(true);
      }

      String program = textarea.getText();
      makeHistory(program, RUN);

      //if (PdeBase.platform == PdeBase.MACOSX) {
      //String pkg = "Proce55ing.app/Contents/Resources/Java/";
      //buildPath = pkg + "build";
      //}
      //buildPath = "lib" + File.separator + "build";
      tempBuildPath = "lib" + File.separator + "build";

      File buildDir = new File(tempBuildPath);
      if (!buildDir.exists()) {
        buildDir.mkdirs();
      }

      String dataPath = sketchFile.getParent() + File.separator + "data";

      if (dataPath != null) {
        File dataDir = new File(dataPath);
        if (dataDir.exists()) {
          PdeEditor.copyDir(dataDir, buildDir);
        }
      }
      int numero1 = (int) (Math.random() * 10000);
      int numero2 = (int) (Math.random() * 10000);
      String className = TEMP_CLASS + "_" + numero1 + "_" + numero2;

      
      // handle building the code
      className = build(program, className, tempBuildPath, false);


      // if the compilation worked, run the applet
      if (className != null) {

        // create a runtime object
        pdeRuntime = new PdeRuntime(this, className);

        // use the runtime object to consume the errors now
        //messageStream.setMessageConsumer(pdeRuntime);
        // no need to bother recycling the old guy
        PdeMessageStream messageStream = new PdeMessageStream(pdeRuntime);

        // start the applet
        pdeRuntime.start(presenting ? presentLocation : appletLocation,
                         new PrintStream(messageStream));
                         //leechErr);

        // spawn a thread to update PDE GUI state
        watcher = new RunButtonWatcher();

      } else {
        // [dmose] throw an exception here?
        // [fry] iirc the exception will have already been thrown
        cleanTempFiles(); //tempBuildPath);
      }
    } catch (PdeException e) { 
      // if we made it to the runtime stage, unwind that thread
      if (pdeRuntime != null) pdeRuntime.stop();
      cleanTempFiles(); //tempBuildPath);

      // printing the stack trace may be overkill since it happens
      // even on a simple parse error
      //e.printStackTrace();

      error(e);

    } catch (Exception e) {  // something more general happened
      e.printStackTrace();

      // if we made it to the runtime stage, unwind that thread
      if (pdeRuntime != null) pdeRuntime.stop();

      cleanTempFiles(); //tempBuildPath);
    }        

    //engine = null;
    //System.out.println("out of doRun()");
    // required so that key events go to the panel and <key> works
    //graphics.requestFocus();  // removed for pde    
  }


  class RunButtonWatcher implements Runnable {
    Thread thread;

    public RunButtonWatcher() {
      thread = new Thread(this);
      thread.start();
    }

    public void run() {
      while (Thread.currentThread() == thread) {
        if ((pdeRuntime != null) && (pdeRuntime.applet != null)) {
          //System.out.println(pdeRuntime.applet.finished);
          buttons.running(!pdeRuntime.applet.finished);
          //} else {
          //System.out.println("still pooping");
        }
        try {
          Thread.sleep(250);
        } catch (InterruptedException e) { }
      }
    }

    public void stop() {
      thread.stop();
    }
  }


  public void doStop() {
    if (pdeRuntime != null) pdeRuntime.stop();
    if (watcher != null) watcher.stop();
    //System.out.println("stop2");
    message(EMPTY);
    //System.out.println("stop3");
    buttons.clear();
    //System.out.println("stop4");
    running = false;
    //System.out.println("NOT RUNNING");
    //System.out.println("stop5");
  }


  // this is the former 'kill' function
  // may just roll this in with the other code
  // -> keep this around for closing the external window
  public void doClose() {
    //System.out.println("doclose1");
    if (presenting) {
      presentationWindow.hide();
      //if ((presentationWindow == null) || 
      //(!presentationWindow.isVisible())) {

    } else {
      try {
        appletLocation = pdeRuntime.window.getLocation();
      } catch (NullPointerException e) { }
    }
    //System.out.println("doclose2");

    if (running) {
      //System.out.println("was running, will call doStop()");
      doStop();
    }

    //System.out.println("doclose3");
    try {
      if (pdeRuntime != null) {
        pdeRuntime.close();  // kills the window
        pdeRuntime = null; // will this help?
      }
    } catch (Exception e) { }
    //System.out.println("doclose4");
    //buttons.clear();  // done by doStop

    //if (buildPath != null) {
    cleanTempFiles(); //buildPath);
    //}

    // toxi_030903: focus the PDE again after quitting presentation mode
    base.toFront();
  }


  public void setSketchModified(boolean what) {
    header.sketchModified = what;
    header.update();
    sketchModified = what;
  }


  // check to see if there have been changes
  // if so, prompt user whether or not to save first
  // if the user cancels, return false to abort parent operation
  protected void checkModified(int checking) {
    checkModified(checking, null, null);
  }

  protected void checkModified(int checking, String path, String name) {
    this.checking = checking;
    openingPath = path;
    openingName = name;

    if (sketchModified) {
      String prompt = "Save changes to " + sketchName + "?  ";

      if (checking == DO_QUIT) {

        int result = 0;

        if (PdeBase.platform == PdeBase.MACOSX) {
          // macosx java kills the app even though cancel might get hit
          // so the cancel button is (temporarily) left off
          // this may be treated differently in macosx java 1.4, 
          // but 1.4 isn't currently stable enough to use.
          Object[] options = { "Yes", "No" };
          result = JOptionPane.showOptionDialog(this,
                                                prompt,
                                                "Quit",
                                                JOptionPane.YES_NO_OPTION,
                                                JOptionPane.QUESTION_MESSAGE,
                                                null,
                                                options, 
                                                options[0]);  // default to save

        } else {
          Object[] options = { "Yes", "No", "Cancel" };
          result = JOptionPane.showOptionDialog(this,
                                                prompt,
                                                "Quit",
                                                JOptionPane.YES_NO_CANCEL_OPTION,
                                                JOptionPane.QUESTION_MESSAGE,
                                                null,
                                                options, 
                                                options[2]);
        }

        if (result == JOptionPane.YES_OPTION) {
          //System.out.println("yes");
          //System.out.println("saving");
          doSave();
          //System.out.println("done saving");
          checkModified2();

        } else if (result == JOptionPane.NO_OPTION) {
          //System.out.println("no");
          checkModified2();  // though this may just quit

        } else if (result == JOptionPane.CANCEL_OPTION) {
          //System.out.println("cancel");
          // does nothing
        }

      } else {
        status.prompt(prompt);
      }

    } else {
      checkModified2();
    }
    //System.out.println("exiting checkmodified");
  }

  public void checkModified2() {
    //System.out.println("checkmodified2");
    switch (checking) {
    case SK_NEW: skNew2(); break;
    case SK_OPEN: skOpen2(openingPath, openingName); break;
    case DO_OPEN: doOpen2(); break;
    case DO_QUIT: doQuit2(); break;
    }
    checking = 0;
  }


  // local vars prevent sketchName from being set
  public void skNew() {
    doStop();
    checkModified(SK_NEW);
  }

  protected void skNew2() {
    try {
      // does all the plumbing to create a new project
      // then calls handleOpen to load it up

      File sketchbookDir = new File("sketchbook", userName); //header.user);
      File sketchDir = null;
      String sketchName = null;

      int index = 0;
      //Calendar now = Calendar.getInstance();
      SimpleDateFormat formatter = new SimpleDateFormat("yyMMdd");
      String purty = formatter.format(new Date());
      do {
        sketchName = "sketch_" + purty + ((char) ('a' + index));
        //int index = (int) (Math.random() * 1000);
        //sketchName = "sketch_" + pad3(index);
        sketchDir = new File(sketchbookDir, sketchName);
        index++;
      } while (sketchDir.exists());

      // mkdir for new project name
      sketchDir.mkdirs();
      new File(sketchDir, "data").mkdirs();
      //new File(sketchDir, "build").mkdirs();

      // make empty pde file
      File sketchFile = new File(sketchDir, sketchName + ".pde");
      new FileOutputStream(sketchFile);

#ifdef MACOS
      // thank you apple, for changing this
      //com.apple.eio.setFileTypeAndCreator(String filename, int, int);

      // jdk13 on osx, or jdk11
      // though apparently still available for 1.4
      if ((PdeBase.platform == PdeBase.MACOS9) ||
          (PdeBase.platform == PdeBase.MACOSX)) {
        MRJFileUtils.setFileTypeAndCreator(sketchFile,
                                           MRJOSType.kTypeTEXT,
                                           new MRJOSType("Pde1"));
      }
#endif

      // make 'data' 'applet' dirs inside that
      // actually, don't, that way can avoid too much extra mess

      // rebuild the menu here
      base.rebuildSketchbookMenu();

      // now open it up
      //skOpen(sketchFile, sketchDir);
      handleOpen(sketchName, sketchFile, sketchDir);

    } catch (IOException e) {
      // NEED TO DO SOME ERROR REPORTING HERE ***
      e.printStackTrace();
    }
  }


  public void skOpen(String path, String name) {
    doStop();
    checkModified(SK_OPEN, path, name);
  }

  protected void skOpen2(String path, String name) {
    File osketchFile = new File(path, name + ".pde");
    File osketchDir = new File(path);
    handleOpen(name, osketchFile, osketchDir);
  }


  public void doOpen() {
    checkModified(DO_OPEN);
  }

  protected void doOpen2() {
    FileDialog fd = new FileDialog(new Frame(), 
                                   "Open a PDE program...", 
                                   FileDialog.LOAD);
    if (sketchFile != null) {
      fd.setDirectory(sketchFile.getPath());
    }
    fd.show();

    String directory = fd.getDirectory();
    String filename = fd.getFile();
    if (filename == null) {
      buttons.clear();
      return; // user cancelled
    }

    handleOpen(filename, new File(directory, filename), null);
  }


  protected void handleOpen(String isketchName, 
                            File isketchFile, File isketchDir) {
    if (!isketchFile.exists()) {
      status.error("no file named " + isketchName);
      return;
    }

    try {
      String program = null;

      if (isketchFile.length() != 0) {
        FileInputStream input = new FileInputStream(isketchFile);
        BufferedReader reader = new BufferedReader(new InputStreamReader(input));
        StringBuffer buffer = new StringBuffer();
        String line = null;
        while ((line = reader.readLine()) != null) {
          buffer.append(line);
          buffer.append('\n');
        }
        program = buffer.toString();
        changeText(program, true);

      } else {
        changeText("", true);
      }

      sketchName = isketchName;
      sketchFile = isketchFile;
      sketchDir = isketchDir;
      setSketchModified(false);

      historyFile = new File(sketchFile.getParent(), "history.gz");
      base.rebuildHistoryMenu(historyFile.getPath());
      historyLast = program;

      header.reset();

      presentLocation = null;
      appletLocation = null;

    } catch (FileNotFoundException e1) {
      e1.printStackTrace();
        
    } catch (IOException e2) {
      e2.printStackTrace();
    }
    buttons.clear();
  }


  public void doSave() {
    // true if lastfile not set, otherwise false, meaning no prompt
    //handleSave(lastFile == null);
    // actually, this will always be false...
    handleSave(sketchName == null);
  }

  public void doSaveAs() {
    handleSave(true);
  }

  protected void handleSave(boolean promptUser) {
    message("Saving file...");
    String s = textarea.getText();

    String directory = sketchFile.getParent(); //lastDirectory;
    String filename = sketchFile.getName(); //lastFile;

    if (promptUser) {
      FileDialog fd = new FileDialog(new Frame(), 
                                     "Save PDE program as...", 
                                     FileDialog.SAVE);
      fd.setDirectory(directory);
      fd.setFile(filename);
      fd.show();

      directory = fd.getDirectory();
      filename = fd.getFile();
      if (filename == null) {
        message(EMPTY);
        buttons.clear();
        return; // user cancelled
      }
    }
    makeHistory(s, SAVE);
    File file = new File(directory, filename);

    try {
      //System.out.println("handleSave: results of getText");
      //System.out.print(s);

      BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(s.getBytes())));

      PrintWriter writer = 
        new PrintWriter(new BufferedWriter(new FileWriter(file)));

      String line = null;
      while ((line = reader.readLine()) != null) {
        //System.out.println("w '" + line + "'");
        writer.println(line);
      }
      writer.flush();
      writer.close();

      sketchFile = file;
      setSketchModified(false);
      message("Done saving " + filename + ".");

    } catch (IOException e) {
      e.printStackTrace();
      //message("Did not write file.");
      message("Could not write " + filename + ".");
    }
    buttons.clear();
  }


  public void skSaveAs(boolean rename) {
    doStop();

    this.renaming = rename;
    if (rename) {
      status.edit("Rename sketch to...", sketchName);
    } else {
      status.edit("Save sketch as...", sketchName);
    }
  }

  public void skSaveAs2(String newSketchName) {
    if (newSketchName.equals(sketchName)) {
      // nothing changes
      return;
    }
    //doSave(); // save changes before renaming.. risky but oh well
    String textareaContents = textarea.getText();
    int textareaPosition = textarea.getCaretPosition();

    File newSketchDir = new File(sketchDir.getParent() +
                                 File.separator + newSketchName);
    File newSketchFile = new File(newSketchDir, newSketchName + ".pde");

    // make new dir
    newSketchDir.mkdirs();
    // copy the sketch file itself with new name
    copyFile(sketchFile, newSketchFile);

    // copy everything from the old dir to the new one
    copyDir(sketchDir, newSketchDir);

    // remove the old sketch file from the new dir
    new File(newSketchDir, sketchName + ".pde").delete();

    // remove the old dir (!)
    if (renaming) {
      // in case java is holding on to any files we want to delete
      System.gc();
      removeDir(sketchDir);
    }
    // (important!) has to be done before opening, 
    // otherwise the new dir is set to sketchDir.. 

    // remove .jar, .class, and .java files from the applet dir
    File appletDir = new File(newSketchDir, "applet");
    File oldjar = new File(appletDir, sketchName + ".jar");
    if (oldjar.exists()) oldjar.delete();
    File oldjava = new File(appletDir, sketchName + ".java");
    if (oldjava.exists()) oldjava.delete();
    File oldclass = new File(appletDir, sketchName + ".class");
    if (oldclass.exists()) oldclass.delete();

    base.rebuildSketchbookMenu();

    // open the new guy
    handleOpen(newSketchName, newSketchFile, newSketchDir);

    // update with the new junk and save that as the new code
    //textarea.editorSetText(textareaContents);
    //textarea.setText(textareaContents);
    changeText(textareaContents, true);
    textarea.setCaretPosition(textareaPosition);
    doSave();
  }


  public void skExport() {
    doStop();
    message("Exporting for the web...");
    File appletDir = new File(sketchDir, "applet");
    handleExport(appletDir, sketchName, new File(sketchDir, "data"));
  }

  public void doExport() {
    message("Exporting for the web...");
    String s = textarea.getText();
    FileDialog fd = new FileDialog(new Frame(), 
                                   "Create applet project named...", 
                                   FileDialog.SAVE);

    String directory = sketchFile.getPath(); //lastDirectory;
    String project = sketchFile.getName(); //lastFile;

    fd.setDirectory(directory);
    fd.setFile(project);
    fd.show();

    directory = fd.getDirectory();
    project = fd.getFile();
    if (project == null) {   // user cancelled
      message(EMPTY);
      buttons.clear();
      return;

    } else if (project.indexOf(' ') != -1) {  // space in filename
      message("Project name cannot have spaces.");
      buttons.clear();
      return;
    }
    handleExport(new File(directory), project, null);
  }

  protected void handleExport(File appletDir, String exportSketchName, 
                              File dataDir) {
    try {
      String program = textarea.getText();

      // create the project directory
      // pass null for datapath because the files shouldn't be 
      // copied to the build dir.. that's only for the temp stuff

      //File projectDir = new File(appletDir, projectName);
      //projectDir.mkdirs();
      appletDir.mkdirs();

      ///

      exportSketchName = 
        build(program, exportSketchName, appletDir.getPath(), true);

      if (exportSketchName == null) {
        buttons.clear();
        return;
      }

      /*
      // preprocess the program
      //
      PdePreprocessor preprocessor = null;
      if (PdeBase.getBoolean("preprocessor.antlr", true)) {
        preprocessor = new PdePreprocessor(program, 
                                           appletDir.getPath());
        try { 
          exportSketchName = 
            preprocessor.writeJava(exportSketchName, 
                                   base.normalItem.getState(),
                                   true);
        } catch (Exception ex) {
          // if there was an issue (including unrecoverable parse errors)
          // try falling back to the old preprocessor
          preprocessor = 
            new PdePreprocessorOro(program, appletDir.getPath());
          exportSketchName = 
            preprocessor.writeJava(exportSketchName, 
                                   base.normalItem.getState(),
                                   true);
        }
      } else {  // use old preproc
        preprocessor = 
          new PdePreprocessorOro(program, appletDir.getPath());

        exportSketchName = 
          preprocessor.writeJava(exportSketchName, 
                                 base.normalItem.getState(), true);
      }

      PdeCompiler compiler = 
        new PdeCompiler(appletDir.getPath(), exportSketchName, this);

      // this will catch and parse errors during compilation
      messageStream = new PdeMessageStream(compiler);
      PrintStream leechErr = new PrintStream(messageStream);

      if (!compiler.compileJava(leechErr)) {
        //throw new Exception("error while compiling, couldn't export");
        // message() will already have error message in this case
        return;
      }
      */


      ///


      int wide = BApplet.DEFAULT_WIDTH;
      int high = BApplet.DEFAULT_HEIGHT;

      try {
        PatternMatcher matcher = new Perl5Matcher();
        PatternCompiler compiler = new Perl5Compiler();

        // don't just use this version, since it only grabs the numbers
        //String sizing = "[\\s\\;]size\\s*\\(\\s*(\\d+)\\s*,\\s*(\\d+)\\s*\\);";

        // this matches against any uses of the size() function, whether they
        // contain numbers of variables or whatever. this way, no warning is 
        // shown if size() isn't actually used in the applet, which is the case 
        // especially for beginners that are cutting/pasting from the reference.
        String sizing = "[\\s\\;]size\\s*\\(\\s*(\\S+)\\s*,\\s*(\\S+)\\s*\\);";

        Pattern pattern = compiler.compile(sizing);

        PatternMatcherInput input = new PatternMatcherInput(program);
        if (matcher.contains(input, pattern)) {
          MatchResult result = matcher.getMatch();
          try {
            wide = Integer.parseInt(result.group(1).toString());
            high = Integer.parseInt(result.group(2).toString());

            System.out.println("width " + wide + " high " + high);

          } catch (NumberFormatException e) {

            // found a reference to size, but it didn't seem to contain numbers
            final String message = 
              "The size of this applet could not automatically be\n" +
              "determined from your code. You'll have to edit the\n" + 
              "HTML file to set the size of the applet.";

            JOptionPane.showMessageDialog(this, message, 
                                          "Could not find applet size",
                                          JOptionPane.WARNING_MESSAGE);
          }
        }
          //} else {
          // no size() defined, make it default
          //sizeInfo = "size(" + BApplet.DEFAULT_WIDTH + ", " + 
          //BApplet.DEFAULT_HEIGHT + "); ";
          //}

      } catch (MalformedPatternException e){
        e.printStackTrace();
        //System.err.println("Bad pattern.");
        //System.err.println(e.getMessage());
      }

        /*
      int index = program.indexOf("size(");  // space in size ( problem!
      if (index != -1) {
        try {
          String str = program.substring(index + 5);
          int comma = str.indexOf(',');
          int paren = str.indexOf(')');
          wide = Integer.parseInt(str.substring(0, comma).trim());
          high = Integer.parseInt(str.substring(comma+1, paren).trim());
        } catch (Exception e) { 
          e.printStackTrace();
        }
      }
        */

      File htmlOutputFile = new File(appletDir, "index.html");
      FileOutputStream fos = new FileOutputStream(htmlOutputFile);
      PrintStream ps = new PrintStream(fos);

      ps.println("<html>");
      ps.println("<head>");
      ps.println("<title>" + exportSketchName + " : Built with Processing</title>");
      ps.println("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=iso-8859-1\">");
      ps.println("<BODY BGCOLOR=\"#666666\" text=\"#FFFFFF\" link=\"#CCCC00\" vlink=\"#CCCC00\" alink=\"#999900\">");
      ps.println("<center>");
      ps.println("  <table width=\"400\" border=\"0\" cellspacing=\"0\" cellpadding=\"10\">");
      ps.println("    <tr>");
      ps.println("      <td>&nbsp;</td>");
      ps.println("    </tr>");
      ps.println("    <tr>");
      ps.println("      <td><applet code=\"" + exportSketchName + "\" archive=\"" + exportSketchName + ".jar\" width=" + wide + " height=" + high + ">");
      ps.println("        </applet></td>");
      ps.println("    </tr>");
      ps.println("    <tr>");
      ps.println("      <td>&nbsp;</td>");
      ps.println("    </tr>");
      ps.println("    <tr>");
      ps.println("      <td><a href=\"" + exportSketchName + ".pde\"><font face=\"Arial, Helvetica, sans-serif\" size=\"2\">Source code</font></a></td>");
      ps.println("    </tr>");
      ps.println("    <tr>");
      ps.println("      <td><font size=\"2\" face=\"Arial, Helvetica, sans-serif\">Built with <a href=\"http://Proce55ing.net\">Processing</a></font></td>");
      ps.println("    </tr>");
      ps.println("  </table>");
      ps.println("</center>");
      ps.println("</body>");
      ps.println("</html>");

      ps.flush();
      ps.close();

#ifdef MACOS
      // this chunk left disabled, because safari doesn't actually
      // set the type/creator of html files it makes

      // however, for macos9, this should be re-enabled
      // (but it's not here since macos9 isn't supported for beta)

      /*
      // thank you apple, for changing this
      //com.apple.eio.setFileTypeAndCreator(String filename, int, int);

      // jdk13 on osx, or jdk11
      // though apparently still available for 1.4
      if ((PdeBase.platform == PdeBase.MACOS9) ||
          (PdeBase.platform == PdeBase.MACOSX)) {
        MRJFileUtils.setFileTypeAndCreator(sketchFile, 
                                           MRJOSType.kTypeTEXT,
                                           new MRJOSType("MSIE"));
      }
      */
#endif

      // create new .jar file
      FileOutputStream zipOutputFile = 
        new FileOutputStream(new File(appletDir, exportSketchName + ".jar"));
        //new FileOutputStream(new File(projectDir, projectName + ".jar"));
      ZipOutputStream zos = new ZipOutputStream(zipOutputFile);
      ZipEntry entry;

      // add standard .class files to the jar
      // these are the bagel classes found in export
      // they are a jdk11-only version of bagel
      String exportDir = ("lib" + File.separator + 
                          "export" + File.separator);
      String bagelClasses[] = new File(exportDir).list();

      for (int i = 0; i < bagelClasses.length; i++) {
        if (!bagelClasses[i].endsWith(".class")) continue;
        entry = new ZipEntry(bagelClasses[i]);
        zos.putNextEntry(entry);
        zos.write(grabFile(new File(exportDir + bagelClasses[i])));
        zos.closeEntry();
      }

      // files to include from data directory
      if ((dataDir != null) && (dataDir.exists())) {
        String datafiles[] = dataDir.list();
        for (int i = 0; i < datafiles.length; i++) {
          if (datafiles[i].equals(".") || datafiles[i].equals("..")) {
            continue;
          }
          entry = new ZipEntry(datafiles[i]);
          zos.putNextEntry(entry);
          zos.write(grabFile(new File(dataDir, datafiles[i])));
          zos.closeEntry();
        }
      }

      // add the project's .class to the jar
      // actually, these should grab everything from the build directory
      // since there may be some inner classes
      // (add any .class files from the applet dir, then delete them)
      String classfiles[] = appletDir.list();
      for (int i = 0; i < classfiles.length; i++) {
        if (classfiles[i].endsWith(".class")) {
          entry = new ZipEntry(classfiles[i]);
          zos.putNextEntry(entry);
          zos.write(grabFile(new File(appletDir, classfiles[i])));
          zos.closeEntry();
        }
      }
      for (int i = 0; i < classfiles.length; i++) {
        if (classfiles[i].endsWith(".class")) {
          new File(appletDir, classfiles[i]).delete();  // not yet
        }
      }

      // close up the jar file
      zos.flush();
      zos.close();

      // make a copy of the .pde file to post on the web
      FileOutputStream sketchOutput = 
        new FileOutputStream(new File(appletDir, exportSketchName + ".pde"));
      PrintWriter sketchWriter = 
        new PrintWriter(new OutputStreamWriter(sketchOutput));
      sketchWriter.print(program);
      sketchWriter.flush();
      sketchWriter.close();

      message("Done exporting.");

    } catch (Exception e) {
      message("Error during export.");
      e.printStackTrace();
    }
    buttons.clear();
  }


  public void doPrint() {
    /*
    Frame frame = new Frame(); // bullocks
    int screenWidth = getToolkit().getScreenSize().width;
    frame.reshape(screenWidth + 20, 100, screenWidth + 100, 200);
    frame.show();

    Properties props = new Properties();
    PrintJob pj = getToolkit().getPrintJob(frame, "PDE", props);
    if (pj != null) {
      Graphics g = pj.getGraphics();
      // awful way to do printing, but sometimes brute force is
      // just the way. java printing across multiple platforms is
      // outrageously inconsistent.
      int offsetX = 100;
      int offsetY = 100;
      int index = 0;
      for (int y = 0; y < graphics.height; y++) {
        for (int x = 0; x < graphics.width; x++) {
          g.setColor(new Color(graphics.pixels[index++]));
          g.drawLine(offsetX + x, offsetY + y,
                     offsetX + x, offsetY + y);
        }
      }
      g.dispose();
      g = null;
      pj.end();
    }
    frame.dispose();
    buttons.clear();
    */
  }


  public void doQuit() {
    doStop();
    //if (!checkModified()) return;
    checkModified(DO_QUIT);
    //System.out.println("exiting doquit");
  }

  protected int calcFolderSize(File folder) {
    int size = 0;

    //System.out.println("calcFolderSize " + folder);
    String files[] = folder.list();
    for (int i = 0; i < files.length; i++) {
      if (files[i].equals(".") || (files[i].equals("..")) ||
          files[i].equals(".DS_Store")) continue;
      File fella = new File(folder, files[i]);
      if (fella.isDirectory()) {
        size += calcFolderSize(fella);
      } else {
        size += (int) fella.length();
      }
    }
    return size;
  }

  protected void doQuit2() {
    //System.out.println("doquit2");
    //doStop();

    // clear out projects that are empty
    if (PdeBase.getBoolean("sketchbook.auto_clean", true)) {
      String userPath = base.sketchbookPath + File.separator + userName;
      File userFolder = new File(userPath);

      //System.out.println("auto cleaning");
      if (userFolder.exists()) {  // huh?
        String entries[] = new File(userPath).list();
        if (entries != null) {
          for (int j = 0; j < entries.length; j++) {
            //System.out.println(entries[j] + " " + entries.length);

            if ((entries[j].equals(".")) || 
                (entries[j].equals(".."))) continue;

            File prey = new File(userPath, entries[j]);
            File pde = new File(prey, entries[j] + ".pde");

            // make sure this is actually a sketch folder with a .pde,
            // not a .DS_Store file or another random user folder

            if (pde.exists()) {
              if (calcFolderSize(prey) == 0) {
                //System.out.println("i want to remove " + prey);
                removeDir(prey);
                //} else {
                //System.out.println("not removign because size is " + 
                //                 calcFolderSize(prey));
              }
            }

            //File prey = new File(preyDir, entries[j] + ".pde");
            //if (prey.exists()) {
            //if (prey.length() == 0) {
                // this is a candidate for deletion, but make sure
                // that the user hasn't added anything else to the folder

                //System.out.println("remove: " + prey);
            //  removeDir(preyDir);
            //}
            //} else {
              //System.out.println(prey + " doesn't exist.. weird");
            //}
          }
        }
      }
    }

    // write sketch.properties
    try {
      FileOutputStream output = null;

      if (PdeBase.platform == PdeBase.MACOSX) {
        //String pkg = "Proce55ing.app/Contents/Resources/Java/";
        //output = new FileOutputStream(pkg + "sketch.properties");
        output = new FileOutputStream("lib/sketch.properties");

      } else if (PdeBase.platform == PdeBase.MACOS9) {
        output = new FileOutputStream("lib/sketch.properties");

      } else { // win95/98/ME doesn't set cwd properly
        URL url = getClass().getResource("buttons.gif");
        String urlstr = url.getFile();
        urlstr = urlstr.substring(0, urlstr.lastIndexOf("/") + 1) +
          "sketch.properties";
#ifdef JDK13
        // the ifdef is weird, but it's set for everything but 
        // macos9, and this will never get hit 
        output = new FileOutputStream(URLDecoder.decode(urlstr));
#else
        System.err.println("bad error while writing sketch.properties");
        System.err.println("you should never see this message");
#endif
      }

      Properties skprops = new Properties();

      //Rectangle window = PdeBase.frame.getBounds();
      Rectangle window = base.getBounds();
      Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();

      skprops.put("window.x", String.valueOf(window.x));
      skprops.put("window.y", String.valueOf(window.y));
      skprops.put("window.w", String.valueOf(window.width));
      skprops.put("window.h", String.valueOf(window.height));

      skprops.put("screen.w", String.valueOf(screen.width));
      skprops.put("screen.h", String.valueOf(screen.height));

      skprops.put("sketch.name", sketchName);
      skprops.put("sketch.directory", sketchDir.getCanonicalPath());
      skprops.put("user.name", userName);

      skprops.put("editor.external", externalEditor ? "true" : "false");
      skprops.put("editor.divider.location", 
                  String.valueOf(splitPane.getDividerLocation()));

      skprops.put("serial.port", PdeBase.get("serial.port", "unspecified"));

      skprops.save(output, "auto-generated by pde, please don't touch");

    } catch (IOException e) {
      System.err.println("doQuit: error saving properties");
      e.printStackTrace();
    }

    //System.out.println("exiting here");
    System.exit(0);
  }


  PdeEditorFind find;

  public void find() {
    if (find == null) { 
      find = new PdeEditorFind(this);
    } else {
      find.show();
    }
  }

  public void findNext() {
    if (find != null) find.find();
  }


  public void doBeautify() {
    String prog = textarea.getText();
    makeHistory(prog, BEAUTIFY);

    char program[] = prog.toCharArray();
    StringBuffer buffer = new StringBuffer();
    boolean gotBlankLine = false;
    int index = 0;
    int level = 0;

    while (index != program.length) {
      int begin = index;
      while ((program[index] != '\n') &&
             (program[index] != '\r')) {
        index++;
        if (program.length == index)
          break;
      }
      int end = index;
      if (index != program.length) {
        if ((index+1 != program.length) &&
            // treat \r\n from windows as one line
            (program[index] == '\r') && 
            (program[index+1] == '\n')) {
          index += 2;
        } else {
          index++;
        }                
      } // otherwise don't increment

      String line = new String(program, begin, end-begin);
      line = line.trim();
            
      if (line.length() == 0) {
        if (!gotBlankLine) {
          // let first blank line through
          buffer.append('\n');
          gotBlankLine = true;
        }
      } else {
        //System.out.println(level);
        int idx = -1;
        String myline = line.substring(0);
        while (myline.lastIndexOf('}') != idx) {
          idx = myline.indexOf('}');
          myline = myline.substring(idx+1);
          level--;
        }
        //for (int i = 0; i < level*2; i++) {
        for (int i = 0; i < level; i++) {
          buffer.append(' ');
        }
        buffer.append(line);
        buffer.append('\n');
        //if (line.charAt(0) == '{') {
        //level++;
        //}
        idx = -1;
        myline = line.substring(0);
        while (myline.lastIndexOf('{') != idx) {
          idx = myline.indexOf('{');
          myline = myline.substring(idx+1);
          level++;
        }
        gotBlankLine = false;
      }
    }
    changeText(buffer.toString(), false);
    setSketchModified(true);
    buttons.clear();
  }


  public void setExternalEditor(boolean external) {
    this.externalEditor = external;
    //System.out.println("setting ee to " + externalEditor);

    textarea.setEditable(!external);
    base.externalEditorItem.setState(external);
    base.saveMenuItem.setEnabled(!external);
    base.saveAsMenuItem.setEnabled(!external);
    base.beautifyMenuItem.setEnabled(!external);

    // disable line highlight and turn off the caret when disabling
    TextAreaPainter painter = textarea.getPainter();
    if (external) {
      painter.setBackground(PdeBase.getColor("editor.program.bgcolor.external", new Color(204, 204, 204)));
      painter.lineHighlight = false;
      textarea.setCaretVisible(false);

    } else {
      painter.setBackground(PdeBase.getColor("editor.program.bgcolor", Color.white));
      painter.lineHighlight = PdeBase.getBoolean("editor.program.linehighlight.enabled", true);
      textarea.setCaretVisible(true);
    }
  }


  // TODO iron out bugs with this code under
  //      different platforms, especially macintosh
  public void highlightLine(int lnum) {
    if (lnum < 0) {
      textarea.select(0, 0);
      return;
    }
    //System.out.println(lnum);
    String s = textarea.getText();
    int len = s.length();
    int st = -1;
    int ii = 0;
    int end = -1;
    int lc = 0;
    if (lnum == 0) st = 0;
    for (int i = 0; i < len; i++) {
      ii++;
      //if ((s.charAt(i) == '\n') || (s.charAt(i) == '\r')) {
      boolean newline = false;
      if (s.charAt(i) == '\r') {
        if ((i != len-1) && (s.charAt(i+1) == '\n')) {
          i++; //ii--;
        }
        lc++;
        newline = true;
      } else if (s.charAt(i) == '\n') {
        lc++;
        newline = true;
      }
      if (newline) {
        if (lc == lnum)
          //st = i+1;
          st = ii;
        else if (lc == lnum+1) {
          //end = i;
          end = ii;
          break;
        }
      }
    }
    if (end == -1) end = len;

    // sometimes KJC claims that the line it found an error in is
    // the last line in the file + 1.  Just highlight the last line
    // in this case. [dmose]
    if (st == -1) st = len;

    textarea.select(st, end);
  }


  public void error(PdeException e) {   // part of PdeEnvironment
    if (e.line >= 0) highlightLine(e.line); 

    status.error(e.getMessage());
    buttons.clearRun();
  }


  public void finished() {  // part of PdeEnvironment
    running = false;
    buttons.clearRun();
    message("Done.");
  }


  public void message(String msg) {  // part of PdeEnvironment
    status.notice(msg);
  }
  
  
  public void messageClear(String msg) {
    status.unnotice(msg);
  }


  ///////////////////////////////////////////////////////////////////

  // utility functions


  static protected byte[] grabFile(File file) throws IOException {
    int size = (int) file.length();
    FileInputStream input = new FileInputStream(file);
    byte buffer[] = new byte[size];
    int offset = 0;
    int bytesRead;
    while ((bytesRead = input.read(buffer, offset, size-offset)) != -1) {
      offset += bytesRead;
      if (bytesRead == 0) break;
    }
    return buffer;
  }


  static protected void copyFile(File afile, File bfile) {
    try {
      FileInputStream from = new FileInputStream(afile);
      FileOutputStream to = new FileOutputStream(bfile);
      byte[] buffer = new byte[4096];
      int bytesRead;
      while ((bytesRead = from.read(buffer)) != -1) {
        to.write(buffer, 0, bytesRead);
      }
      to.flush();
      from.close(); // ??
      to.close(); // ??

#ifdef JDK13
      bfile.setLastModified(afile.lastModified());  // jdk13 required
#endif
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  static protected void copyDir(File sourceDir, File targetDir) {
    String files[] = sourceDir.list();
    for (int i = 0; i < files.length; i++) {
      if (files[i].equals(".") || files[i].equals("..")) continue;
      File source = new File(sourceDir, files[i]);
      File target = new File(targetDir, files[i]);
      if (source.isDirectory()) {
        target.mkdirs();
        copyDir(source, target);
#ifdef JDK13
        target.setLastModified(source.lastModified());
#endif
      } else {
        copyFile(source, target);
      }
    }
  }

  // cleanup temp files
  //
  //static protected void cleanTempFiles(String buildPath) {
  //static protected void cleanTempFiles() {
  protected void cleanTempFiles() {
    if (tempBuildPath == null) return;

    // if the java runtime is holding onto any files in the build dir, we
    // won't be able to delete them, so we need to force a gc here
    //
    System.gc();

    //File dirObject = new File(buildPath);
    File dirObject = new File(tempBuildPath);

    // note that we can't remove the builddir itself, otherwise
    // the next time we start up, internal runs using PdeRuntime won't
    // work because the build dir won't exist at startup, so the classloader
    // will ignore the fact that that dir is in the CLASSPATH in run.sh
    //
    if (dirObject.exists()) {
      removeDescendants(dirObject);
    }
  }

  // remove all files in a directory
  //
  static protected void removeDescendants(File dir) {
    String files[] = dir.list();
    for (int i = 0; i < files.length; i++) {
      if (files[i].equals(".") || files[i].equals("..")) continue;
      File dead = new File(dir, files[i]);
      if (!dead.isDirectory()) {
        if (!PdeBase.getBoolean("editor.save_build_files", false)) {
          if (!dead.delete()) {
            // temporarily disabled
            //System.err.println("couldn't delete " + dead);
          }
        }
      } else {
        removeDir(dead);
        //dead.delete();
      }
    }
  }

  // remove all files in a directory and the dir itself
  //
  static protected void removeDir(File dir) {
    //System.out.println("removing " + dir);
    removeDescendants(dir);
    dir.delete();
  }
}
