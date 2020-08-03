/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Copyright (c) The Processing Foundation 2015
  Hardware I/O library developed by Gottfried Haider as part of GSoC 2015

  This library is free software; you can redistribute it and/or
  modify it under the terms of the GNU Lesser General Public
  License as published by the Free Software Foundation; either
  version 2.1 of the License, or (at your option) any later version.

  This library is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  Lesser General Public License for more details.

  You should have received a copy of the GNU Lesser General
  Public License along with this library; if not, write to the
  Free Software Foundation, Inc., 59 Temple Place, Suite 330,
  Boston, MA  02111-1307  USA
*/

package processing.io;

import processing.io.NativeInterface;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;


/**
 *  @webref
 */
public class PWM {

  int channel;
  String chip;


  /**
   *  Opens a PWM channel
   *  @param channel PWM channel
   *  @see list
   *  @webref
   */
  public PWM(String channel) {
    NativeInterface.loadLibrary();

    int pos = channel.indexOf("/pwm");
    if (pos == -1) {
      throw new IllegalArgumentException("Unsupported channel");
    }
    chip = channel.substring(0, pos);
    this.channel = Integer.parseInt(channel.substring(pos+4));

    if (NativeInterface.isSimulated()) {
      return;
    }

    // export channel through sysfs
    String fn = "/sys/class/pwm/" + chip + "/export";
    int ret = NativeInterface.writeFile(fn, Integer.toString(this.channel));
    if (ret < 0) {
      if (ret == -2) {    // ENOENT
        System.err.println("Make sure your kernel is compiled with PWM_SYSFS enabled and you have the necessary PWM driver for your platform");
      }
      // XXX: check
      if (ret == -22) {   // EINVAL
        System.err.println("PWM channel " + channel + " does not seem to be available on your platform");
      }
      // XXX: check
      if (ret != -16) {   // EBUSY, returned when the pin is already exported
        throw new RuntimeException(fn + ": " + NativeInterface.getError(ret));
      }
    }

    // delay to give udev a chance to change the file permissions behind our back
    // there should really be a cleaner way for this
    try {
      Thread.sleep(500);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }


  /**
   *  Disables the PWM output
   *  @webref
   */
  public void clear() {
    if (NativeInterface.isSimulated()) {
      return;
    }

    String fn = String.format("/sys/class/pwm/%s/pwm%d/enable", chip, channel);
    int ret = NativeInterface.writeFile(fn, "0");
    if (ret < 0) {
      throw new RuntimeException(NativeInterface.getError(ret));
    }
  }


  /**
   *  Gives ownership of a channel back to the operating system
   *  @webref
   */
  public void close() {
    if (NativeInterface.isSimulated()) {
      return;
    }

    // XXX: implicit clear()?
    // XXX: also check GPIO

    String fn = "/sys/class/pwm/" + chip + "/unexport";
    int ret = NativeInterface.writeFile(fn, Integer.toString(channel));
    if (ret < 0) {
      if (ret == -2) {    // ENOENT
        System.err.println("Make sure your kernel is compiled with PWM_SYSFS enabled and you have the necessary PWM driver for your platform");
      }
      // XXX: check
      // EINVAL is also returned when trying to unexport pins that weren't exported to begin with
      throw new RuntimeException(NativeInterface.getError(ret));
    }
  }


  /**
   *  Lists all available PWM channels
   *  @return String array
   *  @webref
   */
  public static String[] list() {
    if (NativeInterface.isSimulated()) {
      return new String[]{ "pwmchip0/pwm0", "pwmchip0/pwm1" };
    }

    ArrayList<String> devs = new ArrayList<String>();
    File dir = new File("/sys/class/pwm");
    File[] chips = dir.listFiles();
    if (chips != null) {
      for (File chip : chips) {
        // get the number of supported channels
        try {
          Path path = Paths.get("/sys/class/pwm/" + chip.getName() + "/npwm");
          String tmp = new String(Files.readAllBytes(path));
          int npwm = Integer.parseInt(tmp.trim());
          for (int i=0; i < npwm; i++) {
            devs.add(chip.getName() + "/pwm" + i);
          }
        } catch (Exception e) {
        }
      }
    }
    // listFiles() does not guarantee ordering
    String[] tmp = devs.toArray(new String[devs.size()]);
    Arrays.sort(tmp);
    return tmp;
  }


  /**
   *  Enables the PWM output
   *  @param period cycle period in Hz
   *  @param duty duty cycle, 0.0 (always off) to 1.0 (always on)
   *  @webref
   */
  public void set(int period, float duty) {
    if (NativeInterface.isSimulated()) {
      return;
    }

    // set period
    String fn = fn = String.format("/sys/class/pwm/%s/pwm%d/period", chip, channel);
    // convert to nanoseconds
    int ret = NativeInterface.writeFile(fn, String.format("%d", (int)(1000000000 / period)));
    if (ret < 0) {
      throw new RuntimeException(fn + ": " + NativeInterface.getError(ret));
    }

    // set duty cycle
    fn = fn = String.format("/sys/class/pwm/%s/pwm%d/duty_cycle", chip, channel);
    if (duty < 0.0 || 1.0 < duty) {
      System.err.println("Duty cycle must be between 0.0 and 1.0.");
      throw new IllegalArgumentException("Illegal argument");
    }
    // convert to nanoseconds
    ret = NativeInterface.writeFile(fn, String.format("%d", (int)((1000000000 * duty) / period)));
    if (ret < 0) {
      throw new RuntimeException(fn + ": " + NativeInterface.getError(ret));
    }

    // enable output
    fn = String.format("/sys/class/pwm/%s/pwm%d/enable", chip, channel);
    ret = NativeInterface.writeFile(fn, "1");
    if (ret < 0) {
      throw new RuntimeException(fn + ": " + NativeInterface.getError(ret));
    }
  }


  /**
   *  Enables the PWM output with a preset period of 1 kHz
   *  @webref
   */
  public void set(float duty) {
    set(1000, duty);
  }
}
