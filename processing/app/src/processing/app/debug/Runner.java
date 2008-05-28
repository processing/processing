/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2004-07 Ben Fry and Casey Reas
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

package processing.app.debug;

import processing.app.*;
import processing.core.*;

import java.awt.Point;
import java.io.*;
import java.util.*;

import com.sun.jdi.*;
import com.sun.jdi.connect.*;


/**
 * Runs a compiled sketch. As of release 0136, all sketches are run externally
 * to the environment so that a debugging interface can be used. This opens up
 * future options for a decent debugger, but in the meantime fixes several
 * problems with output and error streams, messages getting lost on Mac OS X,
 * the run/stop buttons not working, libraries not shutting down, exceptions
 * not coming through, exceptions being printed twice, having to force quit
 * if you make a bad while() loop, and so on.
 */
public class Runner implements MessageConsumer {

  private boolean presenting;

  // Running remote VM
  //private final VirtualMachine vm;
  private VirtualMachine vm;

  // Thread transferring remote error stream to our error stream
  private Thread errThread = null;

  // Thread transferring remote output stream to our output stream
  private Thread outThread = null;

  // Mode for tracing the Trace program (default= 0 off)
  private int debugTraceMode = 0;

  //  Do we want to watch assignments to fields
  private boolean watchFields = false;

  // Class patterns for which we don't want events
  private String[] excludes = {
      "java.*", "javax.*", "sun.*", "com.sun.*",
      "apple.*",
      "processing.*"
  };

  //PApplet applet;
  RunnerException exception;
  //Window window;
  PrintStream leechErr;

  Editor editor;
  // TODO remove this, and use only getters and setters in the sketch class
  Sketch sketch;

  boolean newMessage;
  int messageLineCount;
  boolean foundMessageSource;

  //Process process;
  SystemOutSiphon processInput;
  OutputStream processOutput;
  MessageSiphon processError;


  public Runner(Editor editor, boolean presenting) throws RunnerException {
    this.editor = editor;
    this.sketch = editor.sketch;
    this.presenting = presenting;
    //EditorConsole.systemOut.println("clear");
    //System.out.println("clear");
  }


  public void launch() {
    // TODO this code is a total mess as of release 0136. 
    // This will be cleaned up significantly over the next couple months.
    
    // all params have to be stored as separate items,
    // so a growable array needs to be used. i.e. -Xms128m -Xmx1024m
    // will throw an error if it's shoved into a single array element
    //Vector params = new Vector();

    // get around Apple's Java 1.5 bugs
    //params.add("/System/Library/Frameworks/JavaVM.framework/Versions/1.4.2/Commands/java");
    //params.add("java");
    //System.out.println("0");

    String[] vmParamList = getVirtualMachineParams();
    String[] appletParamList = getSketchParams();

//    String[] vmParamList = (String[]) PApplet.subset(command, 1, command.length-7);
//    String[] appletParamList = (String[]) PApplet.subset(command, command.length-6);
    //new Trace(new String[] { vmparamString, sketch.mainClassName });
    //new Trace(vmParamList, appletParamList);
    vm = launch(vmParamList, appletParamList);

//    PrintWriter writer = new PrintWriter(System.out);
//    PrintWriter writer = null;
//    try {
//      writer = new PrintWriter(new FileWriter("/Users/fry/Desktop/runner.txt"));
//    } catch (IOException e) {
//      e.printStackTrace();
//    }
    //generateTrace(writer);
    generateTrace(null);

//    String[] guiParams = PApplet.concat(vmParamList, appletParamList);
//    for (int i = 0; i < guiParams.length; i++) {
//      if (guiParams[i].equals("-cp")) {
//        guiParams[i] = "-classpath";
//      }
//    }
//    processing.app.debug.gui.GUI.main(guiParams);

//    process = Runtime.getRuntime().exec(command);
//    processInput = new SystemOutSiphon(process.getInputStream());
//    processError = new MessageSiphon(process.getErrorStream(), this);
//    processOutput = process.getOutputStream();
  }


  protected String[] getVirtualMachineParams() {
    ArrayList params = new ArrayList();

    //params.add("-Xint"); // interpreted mode
    //params.add("-Xprof");  // profiler
    //params.add("-Xaprof");  // allocation profiler
    //params.add("-Xrunhprof:cpu=samples");  // old-style profiler

    // TODO change this to use run.args = true, run.args.0, run.args.1, etc.
    // so that spaces can be included in the arg names
    String options = Preferences.get("run.options");
    if (options.length() > 0) {
      String pieces[] = PApplet.split(options, ' ');
      for (int i = 0; i < pieces.length; i++) {
        String p = pieces[i].trim();
        if (p.length() > 0) {
          params.add(p);
        }
      }
    }

    if (Preferences.getBoolean("run.options.memory")) {
      params.add("-Xms" + Preferences.get("run.options.memory.initial") + "m");
      params.add("-Xmx" + Preferences.get("run.options.memory.maximum") + "m");
    }

    // sketch.libraryPath might be ""
    // librariesClassPath will always have sep char prepended
    params.add("-Djava.library.path=" +
               sketch.getLibraryPath() +
               File.pathSeparator +
               System.getProperty("java.library.path"));

    params.add("-cp");
    params.add(sketch.getClassPath());
//    params.add(sketch.getClassPath() +
//        File.pathSeparator +
//        Base.librariesClassPath);

    //PApplet.println(PApplet.split(sketch.classPath, ':'));

    String outgoing[] = new String[params.size()];
    params.toArray(outgoing);
    PApplet.println(outgoing);
    return outgoing;
    //return (String[]) params.toArray();

//  System.out.println("sketch class path");
//  PApplet.println(PApplet.split(sketch.classPath, ';'));
//  System.out.println();
//  System.out.println("libraries class path");
//  PApplet.println(PApplet.split(Base.librariesClassPath, ';'));
//  System.out.println();
  }


  protected String[] getSketchParams() {
    ArrayList params = new ArrayList();

    params.add("processing.core.PApplet");

    // If there was a saved location (this guy has been run more than once)
    // then the location will be set to the last position of the sketch window.
    // This will be passed to the PApplet runner using something like
    // --location=30,20
    // Otherwise, the editor location will be passed, and the applet will
    // figure out where to place itself based on the editor location.
    // --editor-location=150,20
    Point windowLocation = editor.getSketchLocation();
    if (windowLocation != null) {
      params.add(PApplet.ARGS_LOCATION + "=" +
                 windowLocation.x + "," + windowLocation.y);
    } else {
      Point editorLocation = editor.getLocation();
      params.add(PApplet.ARGS_EDITOR_LOCATION + "=" +
                 editorLocation.x + "," + editorLocation.y);
    }

    params.add(PApplet.ARGS_EXTERNAL);
    params.add(PApplet.ARGS_DISPLAY + "=" +
               Preferences.get("run.display"));
    params.add(PApplet.ARGS_SKETCH_FOLDER + "=" +
               sketch.folder.getAbsolutePath());

    if (presenting) {
      params.add(PApplet.ARGS_PRESENT);
      params.add(PApplet.ARGS_STOP_COLOR + "=" +
          Preferences.get("run.present.stop.color"));
      params.add(PApplet.ARGS_BGCOLOR + "=" +
          Preferences.get("run.present.bgcolor"));
    }

    params.add(sketch.getMainClassName());

    //String command[] = (String[]) params.toArray();
    String outgoing[] = new String[params.size()];
    params.toArray(outgoing);
    return outgoing;
  }


  protected VirtualMachine launch(String[] vmParams, String[] classParams) {
    //vm = launchTarget(sb.toString());
    LaunchingConnector connector = findLaunchingConnector();
    //Map arguments = connectorArguments(connector, mainArgs);

    Map arguments = connector.defaultArguments();
    //System.out.println(arguments);
    Connector.Argument mainArg =
      (Connector.Argument)arguments.get("main");
    if (mainArg == null) {
      throw new Error("Bad launching connector");
    }
    String mainArgs = "";
    //mainArgs = addArgument(mainArgs, className);
    if (classParams != null) {
      for (int i = 0; i < classParams.length; i++) {
        mainArgs = addArgument(mainArgs, classParams[i], ' ');
      }
    }
    mainArg.setValue(mainArgs);

    //System.out.println("main args are: ");
    //System.out.println(mainArgs);

    /*
    if (watchFields) {
      // We need a VM that supports watchpoints
      Connector.Argument optionArg =
        (Connector.Argument)arguments.get("options");
      if (optionArg == null) {
        throw new Error("Bad launching connector");
      }
      optionArg.setValue("-classic");
    }
    */
    String optionArgs = "";
    for (int i = 0; i < vmParams.length; i++) {
      optionArgs = addArgument(optionArgs, vmParams[i], ' ');
    }

    Connector.Argument optionArg =
      (Connector.Argument)arguments.get("options");
    optionArg.setValue(optionArgs);

    //arguments.put("address", "localhost");

//    Connector.Argument addressArg =
//      (Connector.Argument)arguments.get("address");
//    addressArg.setValue("localhost");

//    System.out.println("option args are: ");
//    System.out.println(arguments.get("options"));

    //System.out.println("args are " + arguments);
    try {
      return connector.launch(arguments);
    } catch (IOException exc) {
      throw new Error("Unable to launch target VM: " + exc);
    } catch (IllegalConnectorArgumentsException exc) {
      throw new Error("Internal error: " + exc);
    } catch (VMStartException exc) {
      throw new Error("Target VM failed to initialize: " +
      exc.getMessage());
    }
  }


  private static boolean hasWhitespace(String string) {
    int length = string.length();
    for (int i = 0; i < length; i++) {
      if (Character.isWhitespace(string.charAt(i))) {
        return true;
      }
    }
    return false;
  }


  private static String addArgument(String string, String argument, char sep) {
    if (hasWhitespace(argument) || argument.indexOf(',') != -1) {
      // Quotes were stripped out for this argument, add 'em back.
      StringBuffer buffer = new StringBuffer(string);
      buffer.append('"');
      for (int i = 0; i < argument.length(); i++) {
        char c = argument.charAt(i);
        if (c == '"') {
          buffer.append('\\');
        }
        buffer.append(c);
      }
      buffer.append('\"');
      buffer.append(sep);
      return buffer.toString();
    } else {
      return string + argument + String.valueOf(sep);
    }
  }


  /**
   * Generate the trace.
   * Enable events, start thread to display events,
   * start threads to forward remote error and output streams,
   * resume the remote VM, wait for the final event, and shutdown.
   */
  void generateTrace(PrintWriter writer) {
    vm.setDebugTraceMode(debugTraceMode);

    EventThread eventThread = null;
    if (writer != null) {
      eventThread = new EventThread(vm, excludes, writer);
      eventThread.setEventRequests(watchFields);
      eventThread.start();
    }

    //redirectOutput();
    
    Process process = vm.process();

//  processInput = new SystemOutSiphon(process.getInputStream());
//  processError = new MessageSiphon(process.getErrorStream(), this);

    // Copy target's output and error to our output and error.
//    errThread = new StreamRedirectThread("error reader",
//        process.getErrorStream(),
//        System.err);
    MessageSiphon ms = new MessageSiphon(process.getErrorStream(), this);
    errThread = ms.thread;

    outThread = new StreamRedirectThread("output reader",
        process.getInputStream(),
        System.out);

    errThread.start();
    outThread.start();
    
    vm.resume();

    // Shutdown begins when event thread terminates
    try {
      if (eventThread != null) eventThread.join();
      // Bug #775 tracked to this next line in the code. 
      // http://dev.processing.org/bugs/show_bug.cgi?id=775
      errThread.join(); // Make sure output is forwarded
      outThread.join(); // before we exit
      
      // at this point, disable the run button
      // TODO this should be handled better, should it not?
      editor.handleStopped();
      
    } catch (InterruptedException exc) {
      // we don't interrupt
    }
    if (writer != null) writer.close();
  }


  void redirectOutput() {

  }


  /**
   * Find a com.sun.jdi.CommandLineLaunch connector
   */
  LaunchingConnector findLaunchingConnector() {
    List connectors = Bootstrap.virtualMachineManager().allConnectors();

    // code to list available connectors
//    Iterator iter2 = connectors.iterator();
//    while (iter2.hasNext()) {
//      Connector connector = (Connector)iter2.next();
//      System.out.println("connector name is " + connector.name());
//    }

    Iterator iter = connectors.iterator();
    while (iter.hasNext()) {
      Connector connector = (Connector)iter.next();
      if (connector.name().equals("com.sun.jdi.CommandLineLaunch")) {
      //if (connector.name().equals("com.sun.jdi.RawCommandLineLaunch")) {
        return (LaunchingConnector)connector;
      }
    }
    throw new Error("No launching connector");
  }


  /*
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
    Vector params = new Vector();

    params.add("java");

    String options = Preferences.get("run.options");
    if (options.length() > 0) {
      String pieces[] = PApplet.split(options, ' ');
      for (int i = 0; i < pieces.length; i++) {
        String p = pieces[i].trim();
        if (p.length() > 0) {
          params.add(p);
        }
      }
    }

    if (Preferences.getBoolean("run.options.memory")) {
      params.add("-Xms" + Preferences.get("run.options.memory.initial") + "m");
      params.add("-Xmx" + Preferences.get("run.options.memory.maximum") + "m");
    }

    params.add("-Djava.library.path=" +
               sketch.libraryPath +
               File.pathSeparator +
               System.getProperty("java.library.path"));

    // still leaves menubar et al
    //params.add("-Dapple.awt.fakefullscreen=true");

    params.add("-cp");
    params.add(sketch.classPath +
               File.pathSeparator +
               Base.librariesClassPath);

    params.add("processing.core.PApplet");

    params.add(PApplet.ARGS_EXTERNAL);
    params.add(PApplet.ARGS_PRESENT);
    params.add(PApplet.ARGS_STOP_COLOR + "=" +
               Preferences.get("run.present.stop.color"));
    params.add(PApplet.ARGS_BGCOLOR + "=" +
               Preferences.get("run.present.bgcolor"));
    params.add(PApplet.ARGS_DISPLAY + "=" +
               Preferences.get("run.display"));
    params.add(PApplet.ARGS_SKETCH_FOLDER + "=" +
               sketch.folder.getAbsolutePath());
    params.add(sketch.mainClassName);

    String command[] = new String[params.size()];
    params.copyInto(command);
    //PApplet.println(command);

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

    // this as prefix made the code folder bug go away, but killed stdio
    //"cmd", "/c", "start",

    // all params have to be stored as separate items,
    // so a growable array needs to be used. i.e. -Xms128m -Xmx1024m
    // will throw an error if it's shoved into a single array element
    Vector params = new Vector();

    // get around Apple's Java 1.5 bugs
    //params.add("/System/Library/Frameworks/JavaVM.framework/Versions/1.4.2/Commands/java");
    params.add("java");

    //params.add("-Xint"); // interpreted mode
    //params.add("-Xprof");  // profiler
    //params.add("-Xaprof");  // allocation profiler
    //params.add("-Xrunhprof:cpu=samples");  // old-style profiler

    String options = Preferences.get("run.options");
    if (options.length() > 0) {
      String pieces[] = PApplet.split(options, ' ');
      for (int i = 0; i < pieces.length; i++) {
        String p = pieces[i].trim();
        if (p.length() > 0) {
          params.add(p);
        }
      }
    }

    if (Preferences.getBoolean("run.options.memory")) {
      params.add("-Xms" + Preferences.get("run.options.memory.initial") + "m");
      params.add("-Xmx" + Preferences.get("run.options.memory.maximum") + "m");
    }

    // sketch.libraryPath might be ""
    // librariesClassPath will always have sep char prepended
    params.add("-Djava.library.path=" +
               sketch.libraryPath +
               File.pathSeparator +
               System.getProperty("java.library.path"));

    params.add("-cp");
    params.add(sketch.classPath +
               File.pathSeparator +
               Base.librariesClassPath);

//    System.out.println("sketch class path");
//    PApplet.println(PApplet.split(sketch.classPath, ';'));
//    System.out.println();
//    System.out.println("libraries class path");
//    PApplet.println(PApplet.split(Base.librariesClassPath, ';'));
//    System.out.println();

    params.add("processing.core.PApplet");

    params.add(location);
    params.add(PApplet.ARGS_EXTERNAL);
    params.add(PApplet.ARGS_DISPLAY + "=" +
               Preferences.get("run.display"));
    params.add(PApplet.ARGS_SKETCH_FOLDER + "=" +
               sketch.folder.getAbsolutePath());
    params.add(sketch.mainClassName);

    //String command[] = (String[]) params.toArray();
    String command[] = new String[params.size()];
    params.copyInto(command);
    //PApplet.println(command);

    String[] vmParamList = (String[]) PApplet.subset(command, 1, command.length-7);
    String[] appletParamList = (String[]) PApplet.subset(command, command.length-6);
    //new Trace(new String[] { vmparamString, sketch.mainClassName });
    new Trace(vmParamList, appletParamList);

//    String[] guiParams = PApplet.concat(vmParamList, appletParamList);
//    for (int i = 0; i < guiParams.length; i++) {
//      if (guiParams[i].equals("-cp")) {
//        guiParams[i] = "-classpath";
//      }
//    }
//    processing.app.debug.gui.GUI.main(guiParams);

    process = Runtime.getRuntime().exec(command);
    processInput = new SystemOutSiphon(process.getInputStream());
    processError = new MessageSiphon(process.getErrorStream(), this);
    processOutput = process.getOutputStream();
  }
*/


  /**
   * Begin running this sketch internally to the editor.
   * This code needs to be refactored with the semi-identical code
   * in PApplet.main() as outlined in
   * <A HREF="http://dev.processing.org/bugs/show_bug.cgi?id=245">Bug 245</A>
   */
  /*
  public void startInternal(Point windowLocation) throws Exception {
    Point editorLocation = editor.getLocation();
    //Insets editorInsets = editor.getInsets();

    int windowX = editorLocation.x;
    int windowY = editorLocation.y + editor.getInsets().top;

    RunnerClassLoader loader = new RunnerClassLoader();
    Class c = loader.loadClass(sketch.mainClassName);
    applet = (PApplet) c.newInstance();

    window = new Frame(sketch.name); // use ugly window
    ((Frame)window).setResizable(false);
    Base.setIcon((Frame) window);
    //if (Editor.icon != null) {
    //  ((Frame)window).setIconImage(Editor.icon);
    //}
    window.pack(); // to get a peer, size set later, need for insets

    applet.leechErr = leechErr;
    //applet.folder = sketch.folder.getAbsolutePath();
    applet.sketchPath = sketch.folder.getAbsolutePath();
    applet.frame = (Frame) window;

    applet.init();
    //applet.start();

    //while ((applet.width == 0) && !applet.finished) {
    while ((applet.defaultSize) && !applet.finished) {
      try {
        if (applet.exception != null) {
          throw new RunnerException(applet.exception.getMessage());
        }
        Thread.sleep(5);
      } catch (InterruptedException e) { }
    }

    window.addWindowListener(new WindowAdapter() {
        public void windowClosing(WindowEvent e) {
          stop();
          editor.closeRunner();
        }
      });

    int modifiers = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
    final KeyStroke closeWindowKeyStroke = KeyStroke.getKeyStroke('W', modifiers);

    applet.addKeyListener(new KeyAdapter() {
        public void keyPressed(KeyEvent e) {
          if ((e.getKeyCode() == KeyEvent.VK_ESCAPE) ||
              KeyStroke.getKeyStrokeForEvent(e).equals(closeWindowKeyStroke)) {
            stop();
            editor.closeRunner();
          }
        }
      });

    window.add(applet);

    Dimension screen =
      Toolkit.getDefaultToolkit().getScreenSize();

    window.setLayout(null);
    Insets insets = window.getInsets();

    int windowW = Math.max(applet.width, PApplet.MIN_WINDOW_WIDTH) +
      insets.left + insets.right;
    int windowH = Math.max(applet.height, PApplet.MIN_WINDOW_HEIGHT) +
      insets.top + insets.bottom;

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
                     applet.width, applet.height);

    // handle frame resizing events
    applet.setupFrameResizeListener();

    applet.setVisible(true);  // no effect
    if (windowLocation != null) {
      window.setLocation(windowLocation);
    }
    if (applet.displayable()) {
      window.setVisible(true);
    }
    applet.requestFocus();  // necessary for key events
  }
  */


  public void stop() {
    //System.out.println("external stop not implemented");
    close();

    //EventQueue eq = vm.eventQueue();
    //EventRequestManager rm = vm.eventRequestManager();

    // TODO call sketch.stop() (or dispose()) here

    // need to get the instance of the main class

    // then need to call getMethod() on that (or its superclass PApplet?)

//    List list = vm.allClasses();
//    System.out.println("all classes");
//    System.out.println(list);

    //vm.getClass();

    /*
    try {
      System.out.println("gonna suspend");
      vm.suspend();  // TODO need to call PApplet.stop()
      System.out.println("done suspending");

    } catch (com.sun.jdi.VMDisconnectedException vmde) {
      // TODO do nothing.. is this ok?
      System.out.println("harmless disconnect " + vmde.getMessage());

    } catch (Exception e) {
      e.printStackTrace();
    }
    */

    /*
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
    */
  }


  public void close() {
    // TODO make sure stop() has already been called to exit the sketch

    // TODO actually kill off the vm here
    if (vm != null) {
      try {
        vm.exit(0);

      } catch (com.sun.jdi.VMDisconnectedException vmde) {
        // if the vm has disconnected on its own, ignore message
        //System.out.println("harmless disconnect " + vmde.getMessage());
        // TODO shouldn't need to do this, need to do more cleanup
      }
      vm = null;
    }

    //if (window != null) window.hide();
//    if (window != null) {
//      //System.err.println("disposing window");
//      window.dispose();
//      window = null;
//    }

    /*
    if (process != null) {
      try {
        process.destroy();
      } catch (Exception e) {
        //System.err.println("(ignored) error while destroying");
        //e.printStackTrace();
      }
      process = null;
    }
    */
  }


  // made synchronized for rev 87
  // attempted to remove synchronized for 0136 to fix bug #775 (no luck tho)
  // http://dev.processing.org/bugs/show_bug.cgi?id=775
  synchronized public void message(String s) {
    //System.out.println("M" + s.length() + ":" + s.trim()); // + "MMM" + s.length());

    // this eats the CRLFs on the lines.. oops.. do it later
    //if (s.trim().length() == 0) return;

    // this is PApplet sending a message (via System.out.println)
    // that signals that the applet has been quit.
    if (s.indexOf(PApplet.EXTERNAL_STOP) == 0) {
      //System.out.println("external: quit");
      editor.closeRunner();
      return;
    }

    // this is the PApplet sending us a message that the applet
    // is being moved to a new window location
    if (s.indexOf(PApplet.EXTERNAL_MOVE) == 0) {
      String nums = s.substring(s.indexOf(' ') + 1).trim();
      int space = nums.indexOf(' ');
      int left = Integer.parseInt(nums.substring(0, space));
      int top = Integer.parseInt(nums.substring(space + 1));
      editor.setSketchLocation(new Point(left, top));
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

    // exit here because otherwise the exception name
    // may be titled with a blank string
    if (s.trim().length() == 0) return;

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
        int atIndex = s.indexOf("at ");
        if (atIndex == -1) {
          //System.err.println(s);  // stop double-printing exceptions
          return;
        }
        s = s.substring(atIndex + 3);

        // added for 0124 to improve error handling
        // not highlighting lines if it's in the p5 code
        if (s.startsWith("processing.")) return;
        // no highlight if it's java.lang.whatever
        if (s.startsWith("java.")) return;

        //    "javatest.<init>(javatest.java:5)"
        // -> "javatest.<init>" and "(javatest.java:5)"
        int startParen = s.indexOf('(');
        // at javatest.<init>(javatest.java:5)
        //String pkgClassFxn = null;
        //String fileLine = null;
        int codeIndex = -1;
        int lineNumber = -1;

        if (startParen == -1) {
          //pkgClassFxn = s;

        } else {
          //pkgClassFxn = s.substring(0, startParen);

          // "(javatest.java:5)"
          String fileAndLine = s.substring(startParen + 1);
          int stopParen = fileAndLine.indexOf(')');
          //fileAndLine = fileAndLine.substring(0, fileAndLine.length() - 1);
          fileAndLine = fileAndLine.substring(0, stopParen);
          //System.out.println("file 'n line " + fileAndLine);

          //if (!fileAndLine.equals("Unknown Source")) {
          // "javatest.java:5"
          int colonIndex = fileAndLine.indexOf(':');
          if (colonIndex != -1) {
            String filename = fileAndLine.substring(0, colonIndex);
            // "javatest.java" and "5"
            //System.out.println("filename = " + filename);
            //System.out.println("pre0 = " + sketch.code[0].preprocName);
            //for (int i = 0; i < sketch.codeCount; i++) {
            //System.out.println(i + " " + sketch.code[i].lineOffset + " " +
            //                   sketch.code[i].preprocName);
            //}
            lineNumber =
              Integer.parseInt(fileAndLine.substring(colonIndex + 1)) - 1;

            for (int i = 0; i < sketch.getCodeCount(); i++) {
              SketchCode code = sketch.getCode(i);
              //System.out.println(code.preprocName + " " + lineNumber + " " +
              //                 code.preprocOffset);
              if (((code.preprocName == null) &&
                   (lineNumber >= code.preprocOffset)) ||
                  ((code.preprocName != null) &&
                   code.preprocName.equals(filename))) {
                codeIndex = i;
                //System.out.println("got codeindex " + codeIndex);
                //break;
                //} else if (
              }
            }

            if (codeIndex != -1) {
              //System.out.println("got line num " + lineNumber);
              // in case this was a tab that got embedded into the main .java
              lineNumber -= sketch.getCode(codeIndex).preprocOffset;

              // this may have a paren on the end, if so need to strip
              // down to just the digits
              /*
              int lastNumberIndex = colonIndex + 1;
              while ((lastNumberIndex < fileAndLine.length()) &&
                     Character.isDigit(fileAndLine.charAt(lastNumberIndex))) {
                lastNumberIndex++;
              }
              */

              // lineNumber is 1-indexed, but editor wants zero-indexed
              // getMessage() will be what's shown in the editor
              exception =
                new RunnerException(exception.getMessage(),
                                    codeIndex, lineNumber, -1);
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

      } else if (messageLineCount > 10) {  // 5 -> 10 for 0088
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
