/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2009 Ben Fry and Casey Reas

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


public class Debug {
  
  static final String ADB_DEVICES_ERROR = 
    "Received unfamiliar output from “avd devices”.\n" +
    "The device list may have errors.";    

  static protected String[] listDevices() throws IOException {
    String[] cmd = { "adb", "devices" };
    Process p = Runtime.getRuntime().exec(cmd);
    StringRedirectThread error = new StringRedirectThread(p.getErrorStream());
    StringRedirectThread output = new StringRedirectThread(p.getInputStream());

    try {
      int result = p.waitFor();
      if (result == 0) {
        String[] lines = output.getLines();
        // First line starts "List of devices", last line is blank.

        // when an emulator is started with a debug port, then it shows up 
        // in the list of devices.

//        List of devices attached 
//        HT91MLC00031  device
//        emulator-5554 offline

//        List of devices attached 
//        HT91MLC00031  device
//        emulator-5554 device
        
        if (lines == null) {
          // result was 0, so we're ok, but this is odd.
          System.out.println("No devices found.");
          return new String[] { };
        }
        
        if (!lines[0].startsWith("List of devices") ||
            lines[lines.length-1].trim().length() != 0) {
          Base.showWarning("Android Error", ADB_DEVICES_ERROR, null);
        }
        String[] devices = new String[lines.length - 2];
        int deviceIndex = 0;
        for (int i = 1; i < lines.length - 1; i++) {
          int tab = lines[i].indexOf('\t');
          if (tab != -1) {
            devices[deviceIndex++] = lines[i].substring(0, tab);
          } else if (lines[i].trim().length() != 0) {
            System.out.println("Unknown “adb devices” response: " + lines[i]);
          }
        }
        devices = PApplet.subset(devices, 0, deviceIndex);
        return devices;

      } else {
        for (String s : error.getLines()) {
          System.err.println(s);
        }
      }
    } catch (InterruptedException ie) { 
      // ignored, just other thread fun 
    }
    return null;
  }
}