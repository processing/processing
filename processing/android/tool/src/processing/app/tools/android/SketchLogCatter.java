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
import java.io.PrintWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 
 * @author Jonathan Feinberg <jdf@us.ibm.com>
 *
 */
class SketchLogCatter implements LineProcessor {
  private final PrintWriter stdout, stderr;
  private final Pattern interestingLine;

  public SketchLogCatter() {
    stdout = new PrintWriter(System.out, true);
    stderr = new PrintWriter(System.err, true);
    interestingLine = Pattern
        .compile("^([IW])/System\\.(?:err|out)\\(\\s*(\\d+)\\):\\s*(.+)$");
  }

  public void start() throws InterruptedException, IOException {
    final Process process = Runtime.getRuntime().exec(
      new String[] { "adb", "logcat" });
    new StreamPump(process.getInputStream()).addTarget(this).start();
    new StreamPump(process.getErrorStream()).addTarget(System.err).start();
    process.waitFor();
  }

  @Override
  public void processLine(final String line) {
    final Matcher m = interestingLine.matcher(line);
    if (m.matches()) {
      final PrintWriter w = m.group(1).equals("W") ? stderr : stdout;
      w.println(m.group(3));
    }
  }
}