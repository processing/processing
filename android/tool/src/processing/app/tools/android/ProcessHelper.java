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
package processing.app.tools.android;

import java.io.IOException;
import java.io.StringWriter;

/**
 * Class to handle calling Runtime.exec() and stuffing output and error streams
 * into Strings that can be dealt with more easily.
 *
 * @author Jonathan Feinberg <jdf@pobox.com>
 */
class ProcessHelper {
  private final String[] cmd;

  public ProcessHelper(final String... cmd) {
    this.cmd = cmd;
  }

  public String getCommand() {
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
    return execute(false);
  }

  /**
   * Blocking execution.
   * @param tee Send process's stdout/stderr to stdout/stderr in addition to capturing them
   * @return exit value of process
   * @throws InterruptedException
   * @throws IOException
   */
  public ProcessResult execute(final boolean tee) throws InterruptedException,
      IOException {
    return execute(tee, tee);
  }

  /**
   * Blocking execution.
   * @param teeOut Send process's stdout to stdout in addition to capturing it
   * @param teeErr Send process's stderr to stderr in addition to capturing it
   * @return exit value of process
   * @throws InterruptedException
   * @throws IOException
   */
  public ProcessResult execute(final boolean teeOut, final boolean teeErr)
      throws InterruptedException, IOException {
    final StringWriter outWriter = new StringWriter();
    final StringWriter errWriter = new StringWriter();
    final long startTime = System.currentTimeMillis();

    final String prettyCommand = getCommand();
    //    System.err.println("ProcessHelper: begin " + prettyCommand);
    final Process process = Runtime.getRuntime().exec(cmd);

    final StreamPump outpump = new StreamPump(process.getInputStream())
        .addTarget(outWriter);
    if (teeOut) {
      outpump.addTarget(System.out);
    }
    outpump.start();

    final StreamPump errpump = new StreamPump(process.getErrorStream())
        .addTarget(errWriter);
    if (teeErr) {
      errpump.addTarget(System.err);
    }
    errpump.start();
    try {
      final int result = process.waitFor();
      final long time = System.currentTimeMillis() - startTime;
      //      System.err.println("ProcessHelper: " + time + "ms: " + prettyCommand);
      return new ProcessResult(prettyCommand, result, outWriter.toString(),
                               errWriter.toString(), time);
    } catch (final InterruptedException e) {
      System.err.println("Interrupted: " + prettyCommand);
      throw e;
    }
  }
}