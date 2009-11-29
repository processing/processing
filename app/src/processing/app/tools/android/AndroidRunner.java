/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2004-09 Ben Fry and Casey Reas
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

package processing.app.tools.android;

import processing.app.*;
import processing.app.debug.*;

import java.io.*;
import java.util.*;

import com.sun.jdi.*;
import com.sun.jdi.connect.*;
import com.sun.jdi.event.ExceptionEvent;


public class AndroidRunner extends Runner {

//  private boolean presenting;

  // Object that listens for error messages or exceptions.
//  private RunnerListener listener;

  // Running remote VM
//  private VirtualMachine vm;

  // Thread transferring remote error stream to our error stream
//  private Thread errThread = null;

  // Thread transferring remote output stream to our output stream
//  private Thread outThread = null;

  // Mode for tracing the Trace program (default= 0 off)
//  private int debugTraceMode = 0;

  //  Do we want to watch assignments to fields
//  private boolean watchFields = false;

  // Class patterns for which we don't want events
//  private String[] excludes = {
//      "java.*", "javax.*", "sun.*", "com.sun.*",
//      "apple.*",
//      "processing.*"
//  };

//  private RunnerException exception;

//  private Editor editor;
//  private Sketch sketch;
//  private String appletClassName;


  public AndroidRunner(RunnerListener listener) {
    super(listener);
    if (editor != null) {
      sketch = editor.getSketch();
    }
  }


  public boolean launch(String port) {
    vm = launchVirtualMachine(port);
    System.out.println("vm launched");
    if (vm != null) {
      System.out.println("starting trace");
      generateTrace(null);
      System.out.println("done starting trace");
      return true;
    }
    System.out.println("no trace for you");
    return false;
  }
  
  
  /*
  public void launch(Sketch sketch, String appletClassName, 
                     boolean presenting) {
    this.sketch = sketch;
    this.appletClassName = appletClassName;
    this.presenting = presenting;
    
    // TODO entire class is a total mess as of release 0136.
    // This will be cleaned up significantly over the next couple months.

    // all params have to be stored as separate items,
    // so a growable array needs to be used. i.e. -Xms128m -Xmx1024m
    // will throw an error if it's shoved into a single array element
    //Vector params = new Vector();

    // get around Apple's Java 1.5 bugs
    //params.add("/System/Library/Frameworks/JavaVM.framework/Versions/1.4.2/Commands/java");
    //params.add("java");
    //System.out.println("0");

    String[] machineParamList = getMachineParams();
    String[] sketchParamList = getSketchParams();

    vm = launchVirtualMachine(machineParamList, sketchParamList);
    if (vm != null) {
      generateTrace(null);
//      try {
//        generateTrace(new PrintWriter("/Users/fry/Desktop/output.txt"));
//      } catch (Exception e) {
//        e.printStackTrace();
//      }
    }
  }


  protected String[] getMachineParams() {
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

//    params.add("-Djava.ext.dirs=nuffing");

    if (Preferences.getBoolean("run.options.memory")) {
      params.add("-Xms" + Preferences.get("run.options.memory.initial") + "m");
      params.add("-Xmx" + Preferences.get("run.options.memory.maximum") + "m");
    }

    if (Base.isMacOS()) {
      params.add("-Xdock:name=" + appletClassName);
//      params.add("-Dcom.apple.mrj.application.apple.menu.about.name=" +
//                 sketch.getMainClassName());
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

    //PApplet.println(outgoing);
//    PApplet.println(PApplet.split(outgoing[0], ":"));
//    PApplet.println();
//    PApplet.println("class path");
//    PApplet.println(PApplet.split(outgoing[2], ":"));

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
    ArrayList<String> params = new ArrayList<String>();

    params.add("processing.core.PApplet");

    // If there was a saved location (this guy has been run more than once)
    // then the location will be set to the last position of the sketch window.
    // This will be passed to the PApplet runner using something like
    // --location=30,20
    // Otherwise, the editor location will be passed, and the applet will
    // figure out where to place itself based on the editor location.
    // --editor-location=150,20
    if (editor != null) {  // if running processing-cmd, don't do placement
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
    }

    params.add(PApplet.ARGS_DISPLAY + "=" +
               Preferences.get("run.display"));
    params.add(PApplet.ARGS_SKETCH_FOLDER + "=" +
               sketch.getFolder().getAbsolutePath());

    if (presenting) {
      params.add(PApplet.ARGS_PRESENT);
      if (Preferences.getBoolean("run.present.exclusive")) {
        params.add(PApplet.ARGS_EXCLUSIVE);
      }
      params.add(PApplet.ARGS_STOP_COLOR + "=" +
          Preferences.get("run.present.stop.color"));
      params.add(PApplet.ARGS_BGCOLOR + "=" +
          Preferences.get("run.present.bgcolor"));
    }

    params.add(appletClassName);

//    String outgoing[] = new String[params.size()];
//    params.toArray(outgoing);
//    return outgoing;
    return (String[]) params.toArray(new String[0]);
  }
  */


  // http://java.sun.com/j2se/1.5.0/docs/guide/jpda/conninv.html
  protected VirtualMachine launchVirtualMachine(String localPort) {
    // hostname, port, and timeout (ms) are the only items needed here
    AttachingConnector connector = 
      (AttachingConnector) findConnector("com.sun.jdi.SocketAttach");
    //PApplet.println(connector);  // gets the defaults

    Map arguments = connector.defaultArguments();

//    Connector.Argument portArg =
//      (Connector.Argument)arguments.get("port");
//    portArg.setValue(port);
    ((Connector.Argument) arguments.get("port")).setValue(localPort);
    
    ((Connector.Argument) arguments.get("hostname")).setValue("127.0.0.1");
//    ((Connector.Argument) arguments.get("hostname")).setValue("localhost");
//    ((Connector.Argument) arguments.get("timeout")).setValue("5000");

    try {
//      PApplet.println(connector);
//      PApplet.println(arguments);

//      PApplet.println("attaching now...");
      //return connector.attach(arguments);
      VirtualMachine machine = connector.attach(arguments);
//      PApplet.println("attached");
      return machine;
      
    } catch (IOException ioe) {
      //throw new Error("Unable to launch target VM: " + exc);
      ioe.printStackTrace();
      editor.statusError(ioe);

    } catch (IllegalConnectorArgumentsException icae) {
      //throw new Error("Internal error: " + exc);
      editor.statusError(icae);
    }
    return null;
  }


  /**
   * Generate the trace.
   * Enable events, start thread to display events,
   * start threads to forward remote error and output streams,
   * resume the remote VM, wait for the final event, and shutdown.
   */
  protected void generateTrace(PrintWriter writer) {
    vm.setDebugTraceMode(debugTraceMode);

    EventThread eventThread = null;
    //if (writer != null) {
    eventThread = new EventThread(this, vm, excludes, writer);
    eventThread.setEventRequests(watchFields);
    eventThread.start();
    //}

    //redirectOutput();

    Process process = vm.process();

    if (process != null) {
      MessageSiphon ms = new MessageSiphon(process.getErrorStream(), this);
      errThread = ms.getThread();

      outThread = new StreamRedirectThread("output reader",
                                           process.getInputStream(),
                                           System.out);

      errThread.start();
      outThread.start();
    } else {
      System.out.println("process is null, so no streams...");
    }

    vm.resume();
    System.out.println("done with resume");

    // Shutdown begins when event thread terminates
    try {
      if (eventThread != null) eventThread.join();
//      errThread.join(); // Make sure output is forwarded
//      outThread.join(); // before we exit

      // At this point, disable the run button.
      // This happens when the sketch is exited by hitting ESC,
      // or the user manually closes the sketch window.
      // TODO this should be handled better, should it not?
      if (editor != null) {
        editor.internalRunnerClosed();
      }

    } catch (InterruptedException exc) {
      // we don't interrupt
    }
    //System.out.println("and leaving");
    if (writer != null) writer.close();
  }


  /**
   * Find a com.sun.jdi.CommandLineLaunch connector
   */
  /*
  LaunchingConnector findLaunchingConnector(String connectorName) {
    //VirtualMachineManager mgr = Bootstrap.virtualMachineManager();

    // Get the default connector.
    // Not useful here since they all need different args.
//      System.out.println(Bootstrap.virtualMachineManager().defaultConnector());
//      return Bootstrap.virtualMachineManager().defaultConnector();

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
      if (connector.name().equals(connectorName)) {
        return (LaunchingConnector)connector;
      }
    }
    throw new Error("No launching connector");
  }
  */


  public void exception(ExceptionEvent event) {
//    System.out.println(event);
    ObjectReference or = event.exception();
    ReferenceType rt = or.referenceType();
    String exceptionName = rt.name();
    //Field messageField = Throwable.class.getField("detailMessage");
    Field messageField = rt.fieldByName("detailMessage");
//    System.out.println("field " + messageField);
    Value messageValue = or.getValue(messageField);
//    System.out.println("mess val " + messageValue);

    int last = exceptionName.lastIndexOf('.');
    String message = exceptionName.substring(last + 1);
    if (messageValue != null) {
      String messageStr = messageValue.toString();
      if (messageStr.startsWith("\"")) {
        messageStr = messageStr.substring(1, messageStr.length() - 1);
      }
      message += ": " + messageStr;
    }
//    System.out.println("mess type " + messageValue.type());
    //StringReference messageReference = (StringReference) messageValue.type();
//    System.out.println(or.referenceType().fields());

    reportException(message, event.thread());

    if (editor != null) {
      editor.internalRunnerClosed();
    }
  }


  // This may be called more than one time per error in the VM,
  // presumably because exceptions might be wrapped inside others,
  // and this will fire for both.
  /*
  protected void reportException(String message, ThreadReference thread) {
    try {
      Sketch sketch = editor.getSketch();
      
      // a bit for debugging
//      for (StackFrame frame : thread.frames()) {
//        System.out.println("frame: " + frame);
//      }

      List<StackFrame> frames = thread.frames();
      for (StackFrame frame : frames) {
//        System.out.println("frame: " + frame);
        Location location = frame.location();
        String filename = null;
        filename = location.sourceName();
        int lineNumber = location.lineNumber() - 1;
        RunnerException rex = 
          sketch.placeException(message, filename, lineNumber);
        if (rex != null) {
          listener.statusError(rex);
          return;
        }
      }
      // Give up, nothing found inside the pile of stack frames
      listener.statusError(message);

    } catch (AbsentInformationException e) {
      // Any of the thread.blah() methods can throw an AbsentInformationEx
      // if that bit of data is missing. If so, just write out the error
      // message to the console.
      //e.printStackTrace();  // not useful
      exception = new RunnerException(message);
      exception.hideStackTrace();
      listener.statusError(exception);

    } catch (IncompatibleThreadStateException e) {
      e.printStackTrace();
    }
  }
  */


  /*
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
  }
  */


  // made synchronized for rev 87
  // attempted to remove synchronized for 0136 to fix bug #775 (no luck tho)
  // http://dev.processing.org/bugs/show_bug.cgi?id=775
  /*
  synchronized public void message(String s) {
    //System.out.println("M" + s.length() + ":" + s.trim()); // + "MMM" + s.length());

    // this eats the CRLFs on the lines.. oops.. do it later
    //if (s.trim().length() == 0) return;

    // this is PApplet sending a message (via System.out.println)
    // that signals that the applet has been quit.
    if (s.indexOf(PApplet.EXTERNAL_STOP) == 0) {
      //System.out.println("external: quit");
      if (editor != null) {
        //editor.internalCloseRunner();  // [091124]
        editor.handleStop();
      }
      return;
    }

    // this is the PApplet sending us a message that the applet
    // is being moved to a new window location
    if (s.indexOf(PApplet.EXTERNAL_MOVE) == 0) {
      String nums = s.substring(s.indexOf(' ') + 1).trim();
      int space = nums.indexOf(' ');
      int left = Integer.parseInt(nums.substring(0, space));
      int top = Integer.parseInt(nums.substring(space + 1));
      // this is only fired when connected to an editor
      editor.setSketchLocation(new Point(left, top));
      //System.out.println("external: move to " + left + " " + top);
      return;
    }

    // Removed while doing cleaning for 0145,
    // it seems that this is never actually printed out.
    // this is PApplet sending a message saying "i'm about to spew
    // a stack trace because an error occurred during PApplet.run()"
//    if (s.indexOf(PApplet.LEECH_WAKEUP) == 0) {
//      // newMessage being set to 'true' means that the next time
//      // message() is called, expect the first line of the actual
//      // error message & stack trace to be sent from the applet.
//      newMessage = true;
//      return;  // this line ignored
//    }

    // these are used for debugging, in case there are concerns
    // that some errors aren't coming through properly
//    if (s.length() > 2) {
//      System.err.println(newMessage);
//      System.err.println("message " + s.length() + ":" + s);
//    }
    // always shove out the mesage, since it might not fall under
    // the same setup as we're expecting
    System.err.print(s);
    //System.err.println("[" + s.length() + "] " + s);
    System.err.flush();
  }
  */
}
