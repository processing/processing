/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */
/*
 Part of the Processing project - http://processing.org

 Copyright (c) 2009-10 Ben Fry and Casey Reas

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
package processing.app.exec;

import java.io.IOException;
import java.io.StringWriter;

/**
 * Class to handle calling Runtime.exec() and stuffing output and error streams
 * into Strings that can be dealt with more easily.
 *
 * @author Jonathan Feinberg <jdf@pobox.com>
 */
public class ProcessHelper {
  private final String[] cmd;

  public ProcessHelper(final String... cmd) {
    this.cmd = cmd;
  }

  @Override
  public String toString() {
    final StringBuffer buffer = new StringBuffer();
    for (int i = 0; i < cmd.length; i++) {
      if (i != 0) {
        buffer.append(" ");
      }
      buffer.append(cmd[i]);
    }
    return buffer.toString();
  }

  /**
   * Blocking execution.
   * @return exit value of process
   * @throws InterruptedException
   * @throws IOException
   */
  public ProcessResult execute() throws InterruptedException, IOException {
    final StringWriter outWriter = new StringWriter();
    final StringWriter errWriter = new StringWriter();
    final long startTime = System.currentTimeMillis();

    final String prettyCommand = toString();
    //    System.err.println("ProcessHelper: >>>>> " + Thread.currentThread().getId()
    //        + " " + prettyCommand);
    final Process process = Runtime.getRuntime().exec(cmd);
    ProcessRegistry.watch(process);
    try {
      new StreamPump(process.getInputStream()).addTarget(outWriter).start();
      new StreamPump(process.getErrorStream()).addTarget(errWriter).start();
      try {
        final int result = process.waitFor();
        final long time = System.currentTimeMillis() - startTime;
        //        System.err.println("ProcessHelper: <<<<< "
        //            + Thread.currentThread().getId() + " " + cmd[0] + " (" + time
        //            + "ms)");
        return new ProcessResult(prettyCommand, result, outWriter.toString(),
                                 errWriter.toString(), time);
      } catch (final InterruptedException e) {
        System.err.println("Interrupted: " + prettyCommand);
        throw e;
      }
    } finally {
      process.destroy();
      ProcessRegistry.unwatch(process);
    }
  }
}