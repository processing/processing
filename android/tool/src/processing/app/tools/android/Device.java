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
import processing.app.Base;
import processing.core.PApplet;

class Device {

  private static final ProcessHelper LIST_DEVICES_CMD = new ProcessHelper(
                                                                          "adb",
                                                                          "devices");

  private static final String ADB_DEVICES_ERROR = "Received unfamiliar output from “adb devices”.\n"
      + "The device list may have errors.";

  static final int DEFAULT_WIDTH = 320;

  static final int DEFAULT_HEIGHT = 480;

  static void sendMenuButton(final String device) throws IOException {
    // adb -s emulator-5566 shell getevent

    // back on G1
    // /dev/input/event3: 0001 008b 00000001
    // /dev/input/event3: 0001 008b 00000000

    // back on the emulator
    // /dev/input/event0: 0001 00e5 00000001
    // /dev/input/event0: 0001 00e5 00000000

    if (device.startsWith("emulator-")) {
      sendKey(device, 0x00e5);
    } else {
      sendKey(device, 0x008b);
    }
  }

  static void sendHomeButton(final String device) throws IOException {
    sendKey(device, 0x0066); // 102

    // home on the G1
    // /dev/input/event3: 0001 0066 00000001
    // /dev/input/event3: 0001 0066 00000000

    // home on the emulator
    // /dev/input/event0: 0001 0066 00000001
    // /dev/input/event0: 0001 0066 00000000
  }

  private static void sendKey(final String device, final int key)
      throws IOException {
    final String inputDevice = device.startsWith("emulator") ? "/dev/input/event0"
        : "/dev/input/event3";
    final String[] cmd = new String[] {
      "adb", "-s", device, "shell", "sendevent", inputDevice, "1",
      String.valueOf(key), "1" // start with
    // key down
    };
    try {
      int result;
      int attempts = 0;
      do {
        result = Runtime.getRuntime().exec(cmd).waitFor();
        attempts++;
      } while (result != 0 && attempts < 5);

      attempts = 0;
      cmd[cmd.length - 1] = "0"; // send key up
      do {
        result = Runtime.getRuntime().exec(cmd).waitFor();
        attempts++;
      } while (result != 0 && attempts < 5);

    } catch (final InterruptedException ie) {
    }
  }

  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

  static protected String[] list() throws IOException {
    try {
      final ProcessResult result = LIST_DEVICES_CMD.execute();
      if (!result.succeeded()) {
        System.err.println(result);
        return null;
      }
      final String[] lines = result.getStdout().split("\r?\n");
      // First line starts "List of devices"

      // when an emulator is started with a debug port, then it shows up
      // in the list of devices.

      // List of devices attached
      // HT91MLC00031 device
      // emulator-5554 offline

      // List of devices attached
      // HT91MLC00031 device
      // emulator-5554 device

      // for (String s : lines) {
      // System.out.println("Device.list(): '" + s + ".");
      // }

      if (lines.length == 0) {
        return new String[] {};
      }

      int startIndex = 0;

      if (lines[startIndex]
          .startsWith("* daemon not running. starting it now *")) {
        // just pretend he didn't say that.
        startIndex++;
      }

      if (lines[startIndex].startsWith("* daemon started successfully *")) {
        // ignore that too
        startIndex++;
      }

      // might read "List of devices attached"
      if (!lines[startIndex].startsWith("List of devices")) {
        Base.showWarning("Android Error", ADB_DEVICES_ERROR, null);
      }
      startIndex++;
      String[] devices = new String[lines.length - startIndex];
      int deviceIndex = 0;
      for (int i = startIndex; i < lines.length; i++) {
        final String line = lines[i];
        final int tab = line.indexOf('\t');
        if (tab != -1) {
          devices[deviceIndex++] = line.substring(0, tab);
        } else if (line.trim().length() != 0) {
          System.out.println("Unknown “adb devices” response: " + line);
        }
      }
      devices = PApplet.subset(devices, 0, deviceIndex);
      return devices;
    } catch (final InterruptedException ie) {
      // ignored, just other thread fun
    }
    return null;
  }
}