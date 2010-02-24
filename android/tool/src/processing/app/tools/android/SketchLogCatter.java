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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 
 * @author Jonathan Feinberg <jdf@us.ibm.com>
 *
 */
class SketchLogCatter {

  public void start() throws InterruptedException, IOException {
    final Process process = Runtime.getRuntime().exec(
      new String[] { "adb", "logcat" });
    final CountDownLatch latch = new CountDownLatch(2);
    new LogcatThread(process, latch).start();
    new ProcessHelper.StringRedirectThread(process.getErrorStream(), latch,
                                           new OutputStreamWriter(System.err))
        .start();
    latch.await();
    process.waitFor();
  }

  private static class LogcatThread extends Thread {
    private final BufferedReader in;
    private final CountDownLatch latch;
    private final PrintWriter stdout, stderr;
    private final Pattern interestingLine;

    public LogcatThread(final Process process, final CountDownLatch latch) {
      this.in = new BufferedReader(new InputStreamReader(process
          .getInputStream()));
      stdout = new PrintWriter(System.out, true);
      stderr = new PrintWriter(System.err, true);
      this.latch = latch;
      interestingLine = Pattern
          .compile("^([IW])/System\\.(?:err|out)\\(\\s*(\\d+)\\):\\s*(.+)$");
    }

    @Override
    public void run() {
      try {
        String line;
        while ((line = in.readLine()) != null) {
          final Matcher m = interestingLine.matcher(line);
          if (!m.matches()) {
            continue;
          }
          final PrintWriter w = m.group(1).equals("W") ? stderr : stdout;
          w.println("<" + m.group(3) + ">");
        }
      } catch (final IOException e) {
        e.printStackTrace(System.err);
      } finally {
        latch.countDown();
      }
    }
  }
}