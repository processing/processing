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

import javax.swing.JProgressBar;


// I suspect this code can mostly be replaced with built-in Swing functions.
// This code seems like it's adapted from old example code found on the web.
// https://github.com/processing/processing/issues/3176

abstract class ContribProgressBar extends ContribProgressMonitor {
  JProgressBar progressBar;

  public ContribProgressBar(JProgressBar progressBar) {
    this.progressBar = progressBar;
  }

  public void startTask(String name, int maxValue) {
    finished = false;
    progressBar.setString(name);
    progressBar.setIndeterminate(maxValue == UNKNOWN);
    progressBar.setMaximum(maxValue);
  }

  public void setProgress(int value) {
    super.setProgress(value);
    progressBar.setValue(value);
  }

  @Override
  public void finished() {
    super.finished();

    // TODO: this one almost always touches the UI, should be invoked on the EDT;
    // TODO: then one can remove synchronization on visibleContributions in ListPanel
    finishedAction();
  }

  public abstract void finishedAction();
}
