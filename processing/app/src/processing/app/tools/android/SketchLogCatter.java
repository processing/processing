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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class SketchLogCatter {

  public void start() throws InterruptedException, IOException {
    System.err.println("Beginning sketch logcatter");
    final Process process = Runtime.getRuntime().exec(
      new String[] { "adb", "logcat" });
    // The latch is decremented by the LogcatThread when it exhausts its stream
    final CountDownLatch latch = new CountDownLatch(2);
    new LogcatThread(process, Stream.out, latch).start();
    new LogcatThread(process, Stream.err, latch).start();
    latch.await();
    process.waitFor();
  }

  private enum Stream {
    out {
      @Override
      public InputStream extract(final Process p) {
        return p.getInputStream();
      }

      @Override
      Pattern getPattern() {
        return Pattern.compile("^I/System\\.out\\(\\s*(\\d+)\\):\\s*(.+)$");
      }

      @Override
      PrintStream getStream() {
        return System.out;
      }
    },
    err {
      @Override
      public InputStream extract(final Process p) {
        return p.getErrorStream();
      }

      @Override
      Pattern getPattern() {
        return Pattern.compile("^W/System\\.err\\(\\s*(\\d+)\\):\\s*(.+)$");
      }

      @Override
      PrintStream getStream() {
        return System.err;
      }
    };

    abstract InputStream extract(final Process p);

    abstract PrintStream getStream();

    abstract Pattern getPattern();

  }

  private static class LogcatThread extends Thread {
    private final BufferedReader in;
    private final Pattern pattern;
    private final CountDownLatch latch;
    private final PrintWriter out;

    public LogcatThread(final Process process, final Stream stream,
                        final CountDownLatch latch) {
      this.in = new BufferedReader(new InputStreamReader(stream
          .extract(process)));
      this.pattern = stream.getPattern();
      this.out = new PrintWriter(stream.getStream(), true);
      this.latch = latch;
    }

    @Override
    public void run() {
      try {
        String line;
        while ((line = in.readLine()) != null) {
          final Matcher m = pattern.matcher(line);
          if (!m.matches()) {
            continue;
          }
          out.println(m.group(2));
        }
      } catch (final IOException e) {
        e.printStackTrace(System.err);
      } finally {
        latch.countDown();
      }
    }
  }
}