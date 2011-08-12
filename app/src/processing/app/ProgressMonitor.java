/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
 Part of the Processing project - http://processing.org

 Copyright (c) 2004-11 Ben Fry and Casey Reas
 Copyright (c) 2001-04 Massachusetts Institute of Technology

 This program is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License version 2
 as published by the Free Software Foundation.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software Foundation,
 Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package processing.app;

/**
 * The ProgressMonitor interface should be implemented by objects that observe
 * progress of a task or activity.
 */
public interface ProgressMonitor {

  int UNKNOWN = -1;

  /**
   * Starts a new task with the given name.
   * 
   * @param maxValue
   *          the amount of progress that must be made before a task is
   *          finished. This may be set to UNKNOWN.
   */
  public void startTask(String name, int maxValue);

  /**
   * Updates the amount of progress for the current task.
   */
  public void setProgress(int value);

  /**
   * Returns the progress made toward the current task, as previously set by a
   * call to setProgress().
   */
  public int getProgress();

  /**
   * @return <code>true</code> if a cancellation has been requested, false
   *         otherwise
   */
  public boolean isCanceled();

  /**
   * Requests for the task to be cancelled by setting isCanceled() to true.
   */
  public void cancel();
  
  /**
   * @return <code>true</code> if an error occured while completing the task
   */
  public boolean isError();
  
  /**
   * @return an exception that caused the error, may be null.
   */
  public Exception getException();
  
  /**
   * Indicates that an error occurred while performing the task. Exception may
   * be null.
   */
  public void error(Exception e);
  
  /**
   * Returns true if this task is complete
   */
  public boolean isFinished();
  
  /**
   * This is called when the current task is finished. This should always be
   * called when a task is finished, whether or not an error occurred or the
   * task was cancelled.
   */
  public void finished();
  
}

abstract class AbstractProgressMonitor implements ProgressMonitor {
  boolean isCanceled = false;

  boolean isError = false;
  
  boolean isFinished = false;
  
  Exception exception;
  
  int progress = 0;
  
  public void setProgress(int value) {
    progress = value;
  }
  
  public int getProgress() {
    return progress;
  }

  public boolean isCanceled() {
    return isCanceled;
  }

  public void cancel() {
    isCanceled = true;
  }
  
  public boolean isError() {
    return isError;
  }

  public Exception getException() {
    return exception;
  }

  public void error(Exception e) {
    isError = true;
    exception = e;
  }
  
  public boolean isFinished() {
    return isFinished;
  }
  
  public void finished() {
    isFinished = true;
  }

}

class NullProgressMonitor extends AbstractProgressMonitor {

  public void startTask(String name, int maxValue) {
  }
  
}
