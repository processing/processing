/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2013-15 The Processing Foundation
  Copyright (c) 2011-12 Ben Fry and Casey Reas

  This program is free software; you can redistribute it and/or modify
  it under the terms of the GNU General Public License version 2
  as published by the Free Software Foundation.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License along
  with this program; if not, write to the Free Software Foundation, Inc.
  59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/
package processing.app.contrib;


// I suspect this code can mostly be replaced with built-in Swing functions.
// This code seems like it's adapted from old example code found on the web.
// https://github.com/processing/processing/issues/3176

public abstract class ContribProgressMonitor {
  static final int UNKNOWN = -1;
  boolean canceled = false;
  boolean error = false;
  boolean finished = false;
  Exception exception;
  int max;
  int progress = 0;

  public void startTask(String name, int maxValue) {
  }

  public void setProgress(int value) {
    progress = value;
  }

  public int getProgress() {
    return progress;
  }

  public boolean isCanceled() {
    return canceled;
  }

  public void cancel() {
    canceled = true;
  }

  public boolean isError() {
    return error;
  }

  public Exception getException() {
    return exception;
  }

  public void error(Exception e) {
    error = true;
    exception = e;
  }

  public boolean isFinished() {
    return finished;
  }

  public void finished() {
    finished = true;
  }
}

