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

import java.io.PrintWriter;
import processing.app.Sketch;
import processing.app.debug.Runner;
import processing.app.debug.RunnerListener;

public class AndroidRunner extends Runner {

  public AndroidRunner(final RunnerListener listener, final Sketch sketch) {
    super(listener, sketch);
  }

  public boolean launch(final String port) {
    generateTrace(null);
    return true;
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

  /**
   * Generate the trace.
   * Enable events, start thread to display events,
   * start threads to forward remote error and output streams,
   * resume the remote VM, wait for the final event, and shutdown.
   */
  @Override
  protected void generateTrace(final PrintWriter writer) {

    final Thread logcatter = new Thread(new Runnable() {
      public void run() {
        try {
          new SketchLogCatter().start();
          System.err.println("logcatter exited normally");
        } catch (final InterruptedException e) {
          System.err.println("logcat interrupted");
        } catch (final Exception e) {
          throw new RuntimeException(e);
        }
      }
    }, "logcatter");
    logcatter.start();

    // At this point, disable the run button.
    // This happens when the sketch is exited by hitting ESC,
    // or the user manually closes the sketch window.
    // TODO this should be handled better, should it not?
    if (editor != null) {
      editor.internalRunnerClosed();
    }

    if (writer != null) {
      writer.close();
    }
  }

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
