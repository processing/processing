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

  /**
   * Generate the trace.
   * Enable events, start thread to display events,
   * start threads to forward remote error and output streams,
   * resume the remote VM, wait for the final event, and shutdown.
   */
  @Override
  protected void generateTrace(final PrintWriter writer) {

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

}
