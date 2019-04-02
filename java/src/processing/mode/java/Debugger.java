/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2012-19 The Processing Foundation

  This program is free software; you can redistribute it and/or modify
  it under the terms of the GNU General Public License version 2
  as published by the Free Software Foundation.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software Foundation, Inc.
  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
*/

package processing.mode.java;

import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.JTree; // needed for javadocs
import javax.swing.tree.DefaultMutableTreeNode;

import processing.app.Messages;
import processing.app.RunnerListenerEdtAdapter;
import processing.app.Sketch;
import processing.app.SketchCode;
import processing.mode.java.debug.*;
import processing.mode.java.runner.Runner;


public class Debugger {

  /// editor window, acting as main view
  protected JavaEditor editor;

  /// the runtime, contains debuggee VM
  protected Runner runtime;

  /// debuggee vm has started, VMStartEvent received, main class loaded
  protected boolean started = false;

  /// currently paused at breakpoint or step
  protected boolean paused = false;

  /// thread the last breakpoint or step occured in
  protected ThreadReference currentThread;

  /// name of the main class that's currently being debugged
  protected String mainClassName;

  /// the debuggee's main class
  protected ReferenceType mainClass;

  /// holds all loaded classes in the debuggee VM
  protected Set<ReferenceType> classes = new LinkedHashSet<>();

  /// listeners for class load events
  protected List<ClassLoadListener> classLoadListeners = new ArrayList<>();

  /// path to the src folder of the current build
  protected String srcPath;

  /// list of current breakpoints
  protected List<LineBreakpoint> breakpoints = new ArrayList<>();

  /// the step request we are currently in, or null if not in a step
  protected StepRequest requestedStep;

  /// maps line number changes at runtime (orig -> changed)
  protected Map<LineID, LineID> runtimeLineChanges = new HashMap<>();

  /// tab filenames which already have been tracked for runtime changes
  protected Set<String> runtimeTabsTracked = new HashSet<>();

  /// VM event listener
  protected VMEventListener vmEventListener = this::vmEvent;


  public Debugger(JavaEditor editor) {
    this.editor = editor;
  }


  public VirtualMachine vm() {
    if (runtime != null) {
      return runtime.vm();
    }
    return null;
  }


  public JavaEditor getEditor() {
    return editor;
  }


  /**
   * Retrieve the main class of the debuggee VM.
   * @return the main classes {@link ReferenceType}
   *         or null if the debugger is not started.
   */
  public ReferenceType getMainClass() {
    if (isStarted()) {
      return mainClass;
    }
    return null;
  }


  /**
   * Get the main and nested {@link ReferenceType}s for the sketch.
   * @return a list of main and nested {@link ReferenceType}s,
   * empty list if nothing found (e.g. not yet loaded)
   */
  public Set<ReferenceType> getClasses() {
    return classes;
  }


  /**
   * Add a class load listener. Will be notified when a class is loaded in the
   * debuggee VM.
   * @param listener the {@link ClassLoadListener}
   */
  public void addClassLoadListener(ClassLoadListener listener) {
    classLoadListeners.add(listener);
  }


  /**
   * Remove a class load listener. Cease to be notified when classes are
   * loaded in the debuggee VM.
   * @param listener {@link ClassLoadListener}
   */
  public void removeClassLoadListener(ClassLoadListener listener) {
    classLoadListeners.remove(listener);
  }


  /**
   * Start a debugging session. Builds the sketch and launches a VM to run it.
   * VM starts suspended. Should produce a VMStartEvent.
   */
  public synchronized void startDebug() {
    //stopDebug(); // stop any running sessions
    if (isStarted()) {
      return; // do nothing
    }

    // we are busy now
    editor.statusBusy();

    // clear console
    editor.clearConsole();

    // clear variable inspector (also resets expanded states)
    editor.variableInspector().reset();

    // load edits into sketch obj, etc...
    editor.prepareRun();

    // after prepareRun, since this removes highlights
    editor.activateDebug();

    try {
      Sketch sketch = editor.getSketch();
      JavaBuild build = new JavaBuild(sketch);

      log("building sketch: " + sketch.getName());
      //LineMapping.addLineNumbers(sketch); // annotate
//      mainClassName = build.build(false);
      mainClassName = build.build(true);
      //LineMapping.removeLineNumbers(sketch); // annotate
      log("class: " + mainClassName);

      // folder with assembled/preprocessed src
      srcPath = build.getSrcFolder().getPath();
      log("build src: " + srcPath);
      // folder with compiled code (.class files)
      log("build bin: " + build.getBinFolder().getPath());

      if (mainClassName != null) {
        // generate the source line mapping
        //lineMap = LineMapping.generateMapping(srcPath + File.separator + mainClassName + ".java");

        log("launching debuggee runtime");
        runtime = new Runner(build, new RunnerListenerEdtAdapter(editor));
        VirtualMachine vm = runtime.debug(null); // non-blocking
        if (vm == null) {
          loge("error 37: launch failed", null);
        }

        // start receiving vm events
        VMEventReader eventThread = new VMEventReader(vm.eventQueue(), vmEventListener);
        eventThread.start();

        startTrackingLineChanges();
        editor.statusBusy();
      }
    } catch (Exception e) {
      editor.statusError(e);
    }
  }


  /**
   * End debugging session. Stops and disconnects VM. Should produce
   * VMDisconnectEvent.
   */
  public synchronized void stopDebug() {
    editor.variableInspector().lock();
    if (runtime != null) {
      Messages.log("closing runtime");

      for (LineBreakpoint bp : breakpoints) {
        bp.detach();
      }

      runtime.close();
      runtime = null;
      //build = null;
      classes.clear();
      // need to clear highlight here because, VMDisconnectedEvent seems to be unreliable. TODO: likely synchronization problem
      editor.clearCurrentLine();
    }
    stopTrackingLineChanges();
    started = false;

    editor.deactivateDebug();
    editor.deactivateContinue();
    editor.deactivateStep();

    editor.statusEmpty();
  }


  /** Resume paused debugging session. Resumes VM. */
  public synchronized void continueDebug() {
    editor.activateContinue();
    editor.variableInspector().lock();
    //editor.clearSelection();
    //clearHighlight();
    editor.clearCurrentLine();
    if (!isStarted()) {
      startDebug();
    } else if (isPaused()) {
      runtime.vm().resume();
      paused = false;
      editor.statusBusy();
    }
  }


  /**
   * Step through source code lines.
   * @param stepDepth the step depth ({@link StepRequest#STEP_OVER},
   * {@link StepRequest#STEP_INTO} or {@link StepRequest#STEP_OUT})
   */
  protected void step(int stepDepth) {
    if (!isStarted()) {
      startDebug();
    } else if (isPaused()) {
      editor.variableInspector().lock();
      editor.activateStep();

      // use global to mark that there is a step request pending
      requestedStep = runtime.vm().eventRequestManager().createStepRequest(currentThread, StepRequest.STEP_LINE, stepDepth);
      requestedStep.addCountFilter(1); // valid for one step only
      requestedStep.enable();
      paused = false;
      runtime.vm().resume();
      editor.statusBusy();
    }
  }


  /** Step over current statement. */
  public synchronized void stepOver() {
    step(StepRequest.STEP_OVER);
  }


  /** Step into current statement. */
  public synchronized void stepInto() {
    step(StepRequest.STEP_INTO);
  }


  /** Step out of current statement. */
  public synchronized void stepOut() {
    step(StepRequest.STEP_OUT);
  }


//  /** Print the current stack trace. */
//  public synchronized void printStackTrace() {
//    if (isStarted()) {
//      printStackTrace(currentThread);
//    }
//  }
//
//
//  /**
//   * Print local variables. Outputs type, name and value of each variable.
//   */
//  public synchronized void printLocals() {
//    if (isStarted()) {
//      printLocalVariables(currentThread);
//    }
//  }
//
//
//  /**
//   * Print fields of current {@code this}-object.
//   * Outputs type, name and value of each field.
//   */
//  public synchronized void printThis() {
//    if (isStarted()) {
//      printThis(currentThread);
//    }
//  }
//
//
//  /**
//   * Print a source code snippet of the current location.
//   */
//  public synchronized void printSource() {
//    if (isStarted()) {
//      printSourceLocation(currentThread);
//    }
//  }


  /**
   * Set a breakpoint on the current line.
   */
  synchronized void setBreakpoint() {
    setBreakpoint(editor.getCurrentLineID());
  }


  /**
   * Set a breakpoint on a line in the current tab.
   * @param lineIdx the line index (0-based) of the current tab to set the
   * breakpoint on
   */
  synchronized void setBreakpoint(int lineIdx) {
    setBreakpoint(editor.getLineIDInCurrentTab(lineIdx));
  }


  synchronized void setBreakpoint(LineID line) {
    // do nothing if we are kinda busy
    if (isStarted() && !isPaused()) {
      return;
    }
    // do nothing if there already is a breakpoint on this line
    if (hasBreakpoint(line)) {
      return;
    }
    breakpoints.add(new LineBreakpoint(line, this));
    log("set breakpoint on line " + line);
  }


  /**
   * Remove a breakpoint from the current line (if set).
   */
  synchronized void removeBreakpoint() {
    removeBreakpoint(editor.getCurrentLineID().lineIdx());
  }


  /**
   * Remove a breakpoint from a line in the current tab.
   *
   * @param lineIdx the line index (0-based) in the current tab to remove the
   * breakpoint from
   */
  void removeBreakpoint(int lineIdx) {
    // do nothing if we are kinda busy
    if (isBusy()) {
      return;
    }

    LineBreakpoint bp = breakpointOnLine(editor.getLineIDInCurrentTab(lineIdx));
    if (bp != null) {
      bp.remove();
      breakpoints.remove(bp);
      log("removed breakpoint " + bp);
    }
  }


  /** Remove all breakpoints. */
  synchronized void clearBreakpoints() {
    // TODO: handle busy-ness correctly
    if (isBusy()) {
      log("busy");
      return;
    }

    for (LineBreakpoint bp : breakpoints) {
      bp.remove();
    }
    breakpoints.clear();
  }


  /**
   * Clear breakpoints in a specific tab.
   * @param tabFilename the tab's file name
   */
  synchronized void clearBreakpoints(String tabFilename) {
    // TODO: handle busy-ness correctly
    if (isBusy()) {
      log("busy");
      return;
    }

    Iterator<LineBreakpoint> i = breakpoints.iterator();
    while (i.hasNext()) {
      LineBreakpoint bp = i.next();
      if (bp.lineID().fileName().equals(tabFilename)) {
        bp.remove();
        i.remove();
      }
    }
  }


  /**
   * Get the breakpoint on a certain line, if set.
   * @param line the line to get the breakpoint from
   * @return the breakpoint, or null if no breakpoint is set on the specified
   * line.
   */
  LineBreakpoint breakpointOnLine(LineID line) {
    for (LineBreakpoint bp : breakpoints) {
      if (bp.isOnLine(line)) {
        return bp;
      }
    }
    return null;
  }


  /**
   * Toggle a breakpoint on a line in the current tab.
   * @param lineIdx the line index (0-based) in the current tab
   */
  synchronized void toggleBreakpoint(int lineIdx) {
    LineID line = editor.getLineIDInCurrentTab(lineIdx);
    int index = line.lineIdx();
    if (hasBreakpoint(line)) {
      removeBreakpoint(index);
    } else {
      // Make sure the line contains actual code before setting the break
      // https://github.com/processing/processing/issues/3765
      if (editor.getLineText(index).trim().length() != 0) {
        setBreakpoint(index);
      }
    }
  }


  /**
   * Check if there's a breakpoint on a particular line.
   * @param line the line id
   * @return true if a breakpoint is set on the given line, otherwise false
   */
  protected boolean hasBreakpoint(LineID line) {
    LineBreakpoint bp = breakpointOnLine(line);
    return bp != null;
  }


//  /** Print a list of currently set breakpoints. */
//  public synchronized void listBreakpoints() {
//    if (breakpoints.isEmpty()) {
//      System.out.addEmptyLine("no breakpoints");
//    } else {
//      System.out.addEmptyLine("line breakpoints:");
//      for (LineBreakpoint bp : breakpoints) {
//        System.out.addEmptyLine(bp);
//      }
//    }
//  }


  /**
   * Retrieve a list of breakpoint in a particular tab.
   * @param tabFilename the tab's file name
   * @return the list of breakpoints in the given tab
   */
  synchronized List<LineBreakpoint> getBreakpoints(String tabFilename) {
    List<LineBreakpoint> list = new ArrayList<>();
    for (LineBreakpoint bp : breakpoints) {
      if (bp.lineID().fileName().equals(tabFilename)) {
        list.add(bp);
      }
    }
    return list;
  }


  /**
   * Callback for VM events. Will be called from another thread.
   * ({@link VMEventReader})
   * @param es Incoming set of events from VM
   */
  public synchronized void vmEvent(EventSet es) {
    VirtualMachine vm = vm();
    if (vm != null && vm != es.virtualMachine()) {
      // This is no longer VM we are interested in,
      // we already cleaned up and run different VM now.
      return;
    }
    for (Event e : es) {
      log("*** VM Event: " + e);
      if (e instanceof VMStartEvent) {
        vmStartEvent();

      } else if (e instanceof ClassPrepareEvent) {
        vmClassPrepareEvent((ClassPrepareEvent) e);

      } else if (e instanceof BreakpointEvent) {
        vmBreakPointEvent((BreakpointEvent) e);

      } else if (e instanceof StepEvent) {
        vmStepEvent((StepEvent) e);

      } else if (e instanceof VMDisconnectEvent) {
        stopDebug();

      } else if (e instanceof VMDeathEvent) {
        started = false;
        editor.statusEmpty();
      }
    }
  }

  private void createClassPrepareRequest(String name) {
    ClassPrepareRequest classPrepareRequest = runtime.vm().eventRequestManager().createClassPrepareRequest();
    classPrepareRequest.addClassFilter(name);
    classPrepareRequest.enable();
  }


  private void vmStartEvent() {
    // break on main class load
    log("requesting event on main class load: " + mainClassName);
    createClassPrepareRequest(mainClassName);
    createClassPrepareRequest(mainClassName + "$*");
    // break on loading custom classes
    for (SketchCode tab : editor.getSketch().getCode()) {
      if (tab.isExtension("java")) {
        log("requesting event on class load: " + tab.getPrettyName());
        String name = tab.getPrettyName();
        createClassPrepareRequest(name);
        createClassPrepareRequest(name + "$*");
      }
    }
    runtime.vm().resume();
  }


  private void vmClassPrepareEvent(ClassPrepareEvent ce) {
    ReferenceType rt = ce.referenceType();
    currentThread = ce.thread();
    paused = true; // for now we're paused

    if (rt.name().equals(mainClassName)) {
      //printType(rt);
      mainClass = rt;
      classes.add(rt);
      log("main class load: " + rt.name());
      started = true; // now that main class is loaded, we're started
    } else {
      classes.add(rt); // save loaded classes
      log("class load: {0}" + rt.name());
    }

    // notify listeners
    for (ClassLoadListener listener : classLoadListeners) {
      if (listener != null) {
        listener.classLoaded(rt);
      }
    }
    paused = false; // resuming now
    runtime.vm().resume();
  }


  private void vmBreakPointEvent(BreakpointEvent be) {
    currentThread = be.thread(); // save this thread
    updateVariableInspector(currentThread); // this is already on the EDT
    final LineID newCurrentLine = locationToLineID(be.location());
    javax.swing.SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        editor.setCurrentLine(newCurrentLine);
        editor.deactivateStep();
        editor.deactivateContinue();
      }
    });

    // hit a breakpoint during a step, need to cancel the step.
    if (requestedStep != null) {
      runtime.vm().eventRequestManager().deleteEventRequest(requestedStep);
      requestedStep = null;
    }

    // fix canvas update issue
    // TODO: is this a good solution?
    resumeOtherThreads(currentThread);

    paused = true;
    editor.statusHalted();
  }


  private void vmStepEvent(StepEvent se) {
    currentThread = se.thread();

    //printSourceLocation(currentThread);
    updateVariableInspector(currentThread); // this is already on the EDT
    final LineID newCurrentLine = locationToLineID(se.location());
    javax.swing.SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        editor.setCurrentLine(newCurrentLine);
        editor.deactivateStep();
        editor.deactivateContinue();
      }
    });

    // delete the steprequest that triggered this step so new ones can be placed (only one per thread)
    EventRequestManager mgr = runtime.vm().eventRequestManager();
    mgr.deleteEventRequest(se.request());
    requestedStep = null; // mark that there is no step request pending
    paused = true;
    editor.statusHalted();

    // disallow stepping into invisible lines
    if (!locationIsVisible(se.location())) {
      // TODO: this leads to stepping, should it run on the EDT?
      javax.swing.SwingUtilities.invokeLater(new Runnable() {
        @Override
        public void run() {
          stepOutIntoViewOrContinue();
        }
      });
    }
  }


  /**
   * Check whether a location corresponds to a code line in the editor.
   * @param l the location
   * @return true if the location corresponds to a line in the editor
   */
  protected boolean locationIsVisible(Location l) {
    return locationToLineID(l) != null;
  }


  /**
   * Step out if this results in a visible location, otherwise continue.
   */
  protected void stepOutIntoViewOrContinue() {
    try {
      List<StackFrame> frames = currentThread.frames();
      if (frames.size() > 1) {
        if (locationIsVisible(frames.get(1).location())) {
          //System.out.addEmptyLine("stepping out to: " + locationToString(frames.get(1).location()));
          stepOut();
          return;
        }
      }
      continueDebug();

    } catch (IncompatibleThreadStateException ex) {
      logitse(ex);
    }
  }


  /**
   * Check whether a debugging session is running. i.e. the debugger is
   * connected to a debuggee VM, VMStartEvent has been received and main
   * class is loaded.
   * @return true if the debugger is started.
   */
  public synchronized boolean isStarted() {
    return started && runtime != null && runtime.vm() != null;
  }


  /**
   * Check whether the debugger is paused. i.e. it is currently suspended
   * at a breakpoint or step.
   *
   * @return true if the debugger is paused, false otherwise or if not started
   * ({@link #isStarted()})
   */
  public synchronized boolean isPaused() {
    return isStarted() && paused && currentThread != null && currentThread.isSuspended();
  }


  /**
   * Check whether the debugger is currently busy. i.e. running (not
   * suspended).
   * @return true if the debugger is currently running and not suspended.
   */
  public synchronized boolean isBusy() {
    return isStarted() && !isPaused();
  }


  /**
   * Print call stack trace of a thread. Only works on suspended threads.
   * @param t suspended thread to addCode stack trace of
   */
  protected void printStackTrace(ThreadReference t) {
    if (!t.isSuspended()) {
      return;
    }
    try {
      System.out.println("stack trace for thread " + t.name() + ":");
      int i = 0;
      for (StackFrame f : t.frames()) {
        //                Location l = f.location();
        System.out.println(i++ + ": " + f.toString());
      }
    } catch (IncompatibleThreadStateException ex) {
      logitse(ex);
    }
  }


  /**
   * Resume all other threads except the one given as parameter. Useful
   * e.g. to just keep the thread suspended a breakpoint occurred in.
   * @param t the thread not to resume
   */
  protected void resumeOtherThreads(ThreadReference t) {
    if (!isStarted()) {
      return;
    }
    for (ThreadReference other : vm().allThreads()) {
      if (!other.equals(t) && other.isSuspended()) {
        other.resume();
      }
    }
  }


  /**
   * Print info about all current threads. Includes name, status,
   * isSuspended, isAtBreakpoint.
   */
  public synchronized void printThreads() {
    if (!isPaused()) {
      return;
    }
    System.out.println("threads:");
    for (ThreadReference t : vm().allThreads()) {
      printThread(t);
    }
  }


  /**
   * Print info about a thread. Includes name, status, isSuspended,
   * isAtBreakpoint.
   * @param t the thread to addCode info about
   */
  protected void printThread(ThreadReference t) {
    System.out.println(t.name());
    System.out.println("   is suspended: " + t.isSuspended());
    System.out.println("   is at breakpoint: " + t.isAtBreakpoint());
    System.out.println("   status: " + threadStatusToString(t.status()));
  }


  /**
   * Convert a status code returned by {@link ThreadReference#status() }
   * to a human readable form.
   * @param status {@link ThreadReference#THREAD_STATUS_MONITOR},
   * {@link ThreadReference#THREAD_STATUS_NOT_STARTED},
   * {@link ThreadReference#THREAD_STATUS_RUNNING},
   * {@link ThreadReference#THREAD_STATUS_SLEEPING},
   * {@link ThreadReference#THREAD_STATUS_UNKNOWN},
   * {@link ThreadReference#THREAD_STATUS_WAIT} or
   * {@link ThreadReference#THREAD_STATUS_ZOMBIE}
   * @return String containing readable status code.
   */
  protected String threadStatusToString(int status) {
    switch (status) {
    case ThreadReference.THREAD_STATUS_MONITOR:
      return "THREAD_STATUS_MONITOR";
    case ThreadReference.THREAD_STATUS_NOT_STARTED:
      return "THREAD_STATUS_NOT_STARTED";
    case ThreadReference.THREAD_STATUS_RUNNING:
      return "THREAD_STATUS_RUNNING";
    case ThreadReference.THREAD_STATUS_SLEEPING:
      return "THREAD_STATUS_SLEEPING";
    case ThreadReference.THREAD_STATUS_UNKNOWN:
      return "THREAD_STATUS_UNKNOWN";
    case ThreadReference.THREAD_STATUS_WAIT:
      return "THREAD_STATUS_WAIT";
    case ThreadReference.THREAD_STATUS_ZOMBIE:
      return "THREAD_STATUS_ZOMBIE";
    default:
      return "";
    }
  }


  /**
   * Print local variables on a suspended thread. Takes the topmost stack
   * frame and lists all local variables and their values.
   *
   * @param t suspended thread
   */
  protected void printLocalVariables(ThreadReference t) {
    if (!t.isSuspended()) {
      return;
    }
    try {
      if (t.frameCount() == 0) {
        System.out.println("call stack empty");
      } else {
        StackFrame sf = t.frame(0);
        List<LocalVariable> locals = sf.visibleVariables();
        if (locals.isEmpty()) {
          System.out.println("no local variables");
          return;
        }
        for (LocalVariable lv : locals) {
          System.out.println(lv.typeName() + " " + lv.name() + " = " + sf.getValue(lv));
        }
      }
    } catch (IncompatibleThreadStateException ex) {
      logitse(ex);
    } catch (AbsentInformationException ex) {
      log("local variable information not available");
    }
  }


  /**
   * Update variable inspector window. Displays local variables and this
   * fields.
   * @param t suspended thread to retrieve locals and this
   */
  protected void updateVariableInspector(ThreadReference t) {
    if (!t.isSuspended()) {
      return;
    }
    try {
      if (t.frameCount() == 0) {
        // TODO: needs to be handled in a better way:
        log("call stack empty");
      } else {
        final VariableInspector vi = editor.variableInspector();
        // first get data
        final List<DefaultMutableTreeNode> stackTrace = getStackTrace(t);
        final List<VariableNode> locals = getLocals(t, 0);
        final String currentLocation = currentLocation(t);
        final List<VariableNode> thisFields = getThisFields(t, 0, true);
        final List<VariableNode> declaredThisFields = getThisFields(t, 0, false);
        final String thisName = thisName(t);
        // now update asynchronously
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
          @Override
          public void run() {
            //System.out.addEmptyLine("updating vi. from EDT: " + javax.swing.SwingUtilities.isEventDispatchThread());
            vi.updateCallStack(stackTrace, "Call Stack");
            vi.updateLocals(locals, "Locals at " + currentLocation);
            vi.updateThisFields(thisFields, "Class " + thisName);
            vi.updateDeclaredThisFields(declaredThisFields, "Class " + thisName);
            vi.unlock(); // need to do this before rebuilding, otherwise we get these ... dots in the labels
            vi.rebuild();
          }
        });
      }
    } catch (IncompatibleThreadStateException ex) {
      logitse(ex);
    }
  }


  /**
   * Get the class name of the current this object in a suspended thread.
   * @param t a suspended thread
   * @return the class name of this
   */
  protected String thisName(ThreadReference t) {
    try {
      if (!t.isSuspended() || t.frameCount() == 0) {
        return "";
      }
      ObjectReference ref = t.frame(0).thisObject();
      return ref == null ? "" : ref.referenceType().name();

    } catch (IncompatibleThreadStateException ex) {
      logitse(ex);
      return "";
    }
  }


  /**
   * Get a description of the current location in a suspended thread.
   * Format: class.method:translated_line_number
   * @param t a suspended thread
   * @return descriptive string for the given location
   */
  protected String currentLocation(ThreadReference t) {
    try {
      if (!t.isSuspended() || t.frameCount() == 0) {
        return "";
      }
      return locationToString(t.frame(0).location());

    } catch (IncompatibleThreadStateException ex) {
      logitse(ex);
      return "";
    }
  }


  /**
   * Get a string describing a location.
   * Format: class.method:translated_line_number
   * @param loc a location
   * @return descriptive string for the given location
   */
  protected String locationToString(Location loc) {
    LineID line = locationToLineID(loc);
    int lineNumber = (line != null) ? (line.lineIdx() + 1) : loc.lineNumber();
    return loc.declaringType().name() + "." + loc.method().name() + ":" + lineNumber;
  }


  /**
   * Compile a list of current locals usable for insertion into a
   * {@link JTree}. Recursively resolves object references.
   * @param t the suspended thread to get locals for
   * @param depth how deep to resolve nested object references. 0 will not
   * resolve nested objects.
   * @return the list of current locals
   */
  protected List<VariableNode> getLocals(ThreadReference t, int depth) {
    //System.out.addEmptyLine("getting locals");
    List<VariableNode> vars = new ArrayList<>();
    try {
      if (t.frameCount() > 0) {
        StackFrame sf = t.frame(0);
        for (LocalVariable lv : sf.visibleVariables()) {
          //System.out.addEmptyLine("local var: " + lv.name());
          Value val = sf.getValue(lv);
          VariableNode var = new LocalVariableNode(lv.name(), lv.typeName(), val, lv, sf);
          if (depth > 0) {
            var.addChildren(getFields(val, depth - 1, true));
          }
          vars.add(var);
        }
      }
    } catch (IncompatibleThreadStateException ex) {
      logitse(ex);
    } catch (AbsentInformationException ex) {
      loge("local variable information not available", ex);
    }
    return vars;
  }


  /**
   * Compile a list of fields in the current this object usable for insertion
   * into a {@link JTree}. Recursively resolves object references.
   * @param t the suspended thread to get locals for
   * @param depth how deep to resolve nested object references. 0 will not
   * resolve nested objects.
   * @return the list of fields in the current this object
   */
  protected List<VariableNode> getThisFields(ThreadReference t, int depth,
                                             boolean includeInherited) {
    try {
      if (t.frameCount() > 0) {
        StackFrame sf = t.frame(0);
        ObjectReference thisObj = sf.thisObject();
        return getFields(thisObj, depth, includeInherited);
      }
    } catch (IncompatibleThreadStateException ex) {
      logitse(ex);
    }
    return new ArrayList<>();
  }


  /**
   * Recursively get the fields of a {@link Value} for insertion into a
   * {@link JTree}.
   * @param value must be an instance of {@link ObjectReference}
   * @param depth the current depth
   * @param maxDepth the depth to stop at (inclusive)
   * @return list of child fields of the given value
   */
  protected List<VariableNode> getFields(Value value, int depth, int maxDepth,
                                         boolean includeInherited) {
    // remember: Value <- ObjectReference, ArrayReference
    List<VariableNode> vars = new ArrayList<>();
    if (depth <= maxDepth) {
      if (value instanceof ArrayReference) {
        return getArrayFields((ArrayReference) value);
      } else if (value instanceof ObjectReference) {
        ObjectReference obj = (ObjectReference) value;
        // get the fields of this object
        List<Field> fields = includeInherited ? obj.referenceType().visibleFields() : obj.referenceType().fields();
        for (Field field : fields) {
          Value val = obj.getValue(field); // get the value, may be null
          VariableNode var = new FieldNode(field.name(), field.typeName(), val, field, obj);
          // recursively add children
          if (val != null) {
            var.addChildren(getFields(val, depth + 1, maxDepth, includeInherited));
          }
          vars.add(var);
        }
      }
    }
    return vars;
  }


  /**
   * Recursively get the fields of a {@link Value} for insertion into a
   * {@link JTree}.
   *
   * @param value must be an instance of {@link ObjectReference}
   * @param maxDepth max recursion depth. 0 will give only direct children
   * @return list of child fields of the given value
   */
  public List<VariableNode> getFields(Value value, int maxDepth, boolean includeInherited) {
    return getFields(value, 0, maxDepth, includeInherited);
  }


  /**
   * Get the fields of an array for insertion into a {@link JTree}.
   * @param array the array reference
   * @return list of array fields
   */
  protected List<VariableNode> getArrayFields(ArrayReference array) {
    List<VariableNode> fields = new ArrayList<>();
    if (array != null) {
      String arrayType = array.type().name();
      if (arrayType.endsWith("[]")) {
        arrayType = arrayType.substring(0, arrayType.length() - 2);
      }
      int i = 0;
      for (Value val : array.getValues()) {
        VariableNode var = new ArrayFieldNode("[" + i + "]", arrayType, val, array, i);
        fields.add(var);
        i++;
      }
    }
    return fields;
  }


  /**
   * Get the current call stack trace usable for insertion into a
   * {@link JTree}.
   * @param t the suspended thread to retrieve the call stack from
   * @return call stack as list of {@link DefaultMutableTreeNode}s
   */
  protected List<DefaultMutableTreeNode> getStackTrace(ThreadReference t) {
    List<DefaultMutableTreeNode> stack = new ArrayList<>();
    try {
      for (StackFrame f : t.frames()) {
        stack.add(new DefaultMutableTreeNode(locationToString(f.location())));
      }
    } catch (IncompatibleThreadStateException ex) {
      logitse(ex);
    }
    return stack;
  }


  /**
   * Print visible fields of current "this" object on a suspended thread.
   * Prints type, name and value.
   * @param t suspended thread
   */
  protected void printThis(ThreadReference t) {
    if (!t.isSuspended()) {
      return;
    }
    try {
      if (t.frameCount() == 0) {
        // TODO: needs to be handled in a better way
        System.out.println("call stack empty");
      } else {
        StackFrame sf = t.frame(0);
        ObjectReference thisObject = sf.thisObject();
        if (thisObject != null) {
          ReferenceType type = thisObject.referenceType();
          System.out.println("fields in this (" + type.name() + "):");
          for (Field f : type.visibleFields()) {
            System.out.println(f.typeName() + " " + f.name() + " = " + thisObject.getValue(f));
          }
        } else {
          System.out.println("can't get this (in native or static method)");
        }
      }
    } catch (IncompatibleThreadStateException ex) {
      logitse(ex);
    }
  }


  /**
   * Print source code snippet of current location in a suspended thread.
   * @param t suspended thread
   */
  protected void printSourceLocation(ThreadReference t) {
    try {
      if (t.frameCount() == 0) {
        // TODO: needs to be handled in a better way
        System.out.println("call stack empty");
      } else {
        Location l = t.frame(0).location(); // current stack frame location
        printSourceLocation(l);
      }
    } catch (IncompatibleThreadStateException ex) {
      logitse(ex);
    }
  }


  /**
   * Print source code snippet.
   * @param l {@link Location} object to addCode source code for
   */
  protected void printSourceLocation(Location l) {
    try {
      //System.out.addEmptyLine(l.sourceName() + ":" + l.lineNumber());
      System.out.println("in method " + l.method() + ":");
      System.out.println(getSourceLine(l.sourcePath(), l.lineNumber(), 2));

    } catch (AbsentInformationException ex) {
      log("absent information", ex);
    }
  }


  /**
   * Read a line from the given file in the builds src folder.
   * 1-based i.e. first line has line no. 1
   * @param filePath
   * @param lineNo
   * @return the requested source line
   */
  protected String getSourceLine(String filePath, int lineNo, int radius) {
    if (lineNo == -1) {
      loge("invalid line number: " + lineNo, null);
      return "";
    }
    //System.out.addEmptyLine("getting line: " + lineNo);
    File f = new File(srcPath + File.separator + filePath);
    String output = "";
    try {
      BufferedReader r = new BufferedReader(new FileReader(f));
      int i = 1;
      //String line = "";
      while (i <= lineNo + radius) {
        String line = r.readLine(); // line no. i
        if (line == null) {
          break; // end of file
        }
        if (i >= lineNo - radius) {
          if (i > lineNo - radius) {
            output += "\n"; // add newlines before all lines but the first
          }
          output += f.getName() + ":" + i + (i == lineNo ? " =>  " : "     ") + line;
        }
        i++;
      }
      r.close();
      return output;

    } catch (FileNotFoundException ex) {
      //System.err.addEmptyLine(ex);
      return f.getName() + ":" + lineNo;

    } catch (IOException ex) {
      loge("other io exception", ex);
      return "";
    }
  }


  /**
   * Print info about a ReferenceType. Prints class name, source file name,
   * lists methods.
   * @param rt the reference type to addCode out
   */
  protected void printType(ReferenceType rt) {
    System.out.println("ref.type: " + rt);
    System.out.println("name: " + rt.name());
    try {
      System.out.println("sourceName: " + rt.sourceName());
    } catch (AbsentInformationException ex) {
      System.out.println("sourceName: unknown");
    }
    System.out.println("methods:");
    for (Method m : rt.methods()) {
      System.out.println(m.toString());
    }
  }


  /**
   * Translate a java source location to a sketch line id.
   * @param l the location to translate
   * @return the corresponding line id, or null if not found
   */
  protected LineID locationToLineID(Location l) {
    try {
      //return lineMap.get(LineID.create(l.sourceName(), l.lineNumber() - 1));
      return javaToSketchLine(new LineID(l.sourceName(), l.lineNumber() - 1));

    } catch (AbsentInformationException ex) {
      loge("absent information", ex);
      return null;
    }
  }


  /**
   * Translate a line (index) from java space to sketch space.
   * @param javaLine the java line id
   * @return the corresponding sketch line id or null if failed to translate
   */
  public LineID javaToSketchLine(LineID javaLine) {
    Sketch sketch = editor.getSketch();

    // it may belong to a pure java file created in the sketch
    // try to find an exact filename match and check the extension
    SketchCode tab = editor.getTab(javaLine.fileName());
    if (tab != null && tab.isExtension("java")) {
      // can translate 1:1
      return originalToRuntimeLine(javaLine);
    }

    // check if it is the preprocessed/assembled file for this sketch
    // java file name needs to match the sketches filename
    if (!javaLine.fileName().equals(sketch.getName() + ".java")) {
      return null;
    }

    // find the tab (.pde file) this line belongs to
    // get the last tab that has an offset not greater than the java line number
    for (int i = sketch.getCodeCount() - 1; i >= 0; i--) {
      tab = sketch.getCode(i);
      // ignore .java files
      // the tab's offset must not be greater than the java line number
      if (tab.isExtension("pde") && tab.getPreprocOffset() <= javaLine.lineIdx()) {
        final int index = javaLine.lineIdx() - tab.getPreprocOffset();
        return originalToRuntimeLine(new LineID(tab.getFileName(), index));
      }
    }
    return null;
  }


  /**
   * Get the runtime-changed line id for an original sketch line. Used to
   * translate line numbers from the VM (which runs on the original line
   * numbers) to their current (possibly changed) counterparts.
   * @param line the original line id (at compile time)
   * @return the changed version or the line given as parameter if not found
   */
  protected LineID originalToRuntimeLine(LineID line) {
    LineID transformed = runtimeLineChanges.get(line);
    if (transformed == null) {
      return line;
    }
    return transformed;
  }


  /**
   * Get the original line id for a sketch line that was changed at runtime.
   * Used to translate line numbers from the UI at runtime (which can differ
   * from the ones the VM runs on) to their original counterparts.
   * @param line the (possibly) changed runtime line
   * @return the original line or the line given as parameter if not found
   */
  protected LineID runtimeToOriginalLine(LineID line) {
    for (Entry<LineID, LineID> entry : runtimeLineChanges.entrySet()) {
      if (entry.getValue().equals(line)) {
        return entry.getKey();
      }
    }
    return line;
  }


  /**
   * Translate a line (index) from sketch space to java space.
   * @param sketchLine the sketch line id
   * @return the corresponding java line id or null if failed to translate
   */
  public LineID sketchToJavaLine(LineID sketchLine) {
    // transform back to orig (before changes at runtime)
    sketchLine = runtimeToOriginalLine(sketchLine);

    // check if there is a tab for this line
    SketchCode tab = editor.getTab(sketchLine.fileName());
    if (tab == null) {
      return null;
    }

    // check if the tab is a pure java file anyway
    if (tab.isExtension("java")) {
      // 1:1 translation
      return sketchLine;
    }

    // the java file has a name sketchname.java
    // just add the tab's offset to get the java name
    LineID javaLine =
      new LineID(editor.getSketch().getName() + ".java",
                 sketchLine.lineIdx() + tab.getPreprocOffset());
    return javaLine;
  }


  /** Start tracking all line changes (due to edits) in the current tab. */
  protected void startTrackingLineChanges() {
    // TODO: maybe move this to the editor?
    SketchCode tab = editor.getSketch().getCurrentCode();
    if (runtimeTabsTracked.contains(tab.getFileName())) {
      return;
    }

    for (int i = 0; i < tab.getLineCount(); i++) {
      LineID old = new LineID(tab.getFileName(), i);
      LineID tracked = new LineID(tab.getFileName(), i);
      tracked.startTracking(editor.currentDocument());
      runtimeLineChanges.put(old, tracked);
    }
    runtimeTabsTracked.add(tab.getFileName());
    //System.out.addEmptyLine("tracking tab: " + tab.getFileName());
  }


  /** Stop tracking line changes in all tabs. */
  protected void stopTrackingLineChanges() {
    //System.out.addEmptyLine("stop tracking line changes");
    for (LineID tracked : runtimeLineChanges.values()) {
      tracked.stopTracking();
    }
    runtimeLineChanges.clear();
    runtimeTabsTracked.clear();
  }


  private void log(String msg, Object... args) {
    if (args != null && args.length != 0) {
      Messages.logf(getClass().getName() + " " + msg, args);
    } else {
      Messages.log(getClass().getName() + " " + msg);
    }
  }


  private void loge(String msg, Throwable t) {
    if (t != null) {
      Messages.loge(getClass().getName() + " " + msg, t);
    } else {
      Messages.loge(getClass().getName() + " " + msg);
    }
  }


  static private void logitse(Throwable t) {
    Messages.loge("incompatible thread state?", t);
  }


  /**
   * Interface for VM callbacks.
   */
  protected interface VMEventListener {
    /**
     * Receive an event from the VM. Events are sent in batches. See
     * documentation of EventSet for more information.
     * @param es Set of events
     */
    void vmEvent(EventSet es);
  }


  /**
   * Reader Thread for VM Events. Constantly monitors a VMs EventQueue for new
   * events and forwards them to an VMEventListener.
   *
   * @author Martin Leopold <m@martinleopold.com>
   */
  protected static class VMEventReader extends Thread {
    EventQueue eventQueue;
    VMEventListener listener;

    /**
     * Construct a VMEventReader. Needs to be kicked off with start() once
     * constructed.
     *
     * @param eventQueue The queue to read events from. Can be obtained from a
     * VirtualMachine via eventQueue().
     * @param listener the listener to forward events to.
     */
    public VMEventReader(EventQueue eventQueue, VMEventListener listener) {
      super("VM Event Thread");
      this.eventQueue = eventQueue;
      this.listener = listener;
    }

    @Override
    public void run() {
      try {
        while (true) {
          EventSet eventSet = eventQueue.remove();
          listener.vmEvent(eventSet);
          // for (Event e : eventSet) { System.out.addEmptyLine("VM Event: " + e.toString()); }
        }
      } catch (VMDisconnectedException e) {
        Messages.log("VMEventReader quit on VM disconnect");
      } catch (Exception e) {
        Messages.loge("VMEventReader quit", e);
      }
    }
  }
}
