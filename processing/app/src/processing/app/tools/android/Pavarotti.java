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

import java.io.File;
import java.io.IOException;
import java.util.Map;

import processing.core.PApplet;


/**
 * Class to handle calling Runtime.exec() and stuffing input and error streams
 * into String arrays that can be dealt with more easily.
 */
public class Pavarotti {
  Process process;
  String[] cmd;

  StringRedirectThread error;
  StringRedirectThread output;


  public Pavarotti(String[] cmd) throws IOException {
    this.cmd = cmd;

    ProcessBuilder pb = new ProcessBuilder(cmd);

    // Make sure the ANDROID_SDK variable is set
    Map<String,String> env = pb.environment();
    env.put("ANDROID_SDK", Android.sdkPath);
    // Also make sure that the tools are included in the PATH
    String path = env.get("PATH");
    String toolsPath = Android.sdkPath + File.separator + "tools";
    env.put("PATH", path + File.pathSeparator + toolsPath);
    System.out.println("path should be " + env.get("PATH"));

    process = pb.start();
    //process = Runtime.getRuntime().exec(cmd);
    error = new StringRedirectThread(process.getErrorStream());
    output = new StringRedirectThread(process.getInputStream());
  }


  public void printCommand() {
    System.out.println(PApplet.join(cmd, " "));
  }


  public void printArgs() {
    PApplet.println(cmd);
  }


  int waitFor() throws InterruptedException {
    int result = process.waitFor();
    error.finish();
    output.finish();
    return result;
  }


  public String[] getErrorLines() {
    return error.getLines();
  }


  public String[] getOutputLines() {
    return output.getLines();
  }


  public void printErrorLines() {
    for (String err : getErrorLines()) {
      //if (err.length() > 0) System.err.println("err: " + err);
      if (err.length() > 0) System.err.println(err);
//      System.err.println(err);
    }
  }


  public void printOutputLines() {
    for (String out : getOutputLines()) {
      //if (out.length() > 0) System.out.println("out: " + out);
      if (out.length() > 0) System.out.println(out);
//      System.out.println(out);
    }
  }


  public void printLines() {
    printOutputLines();
    printErrorLines();
  }
}