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

import java.io.File;
import java.io.IOException;

import javax.swing.JOptionPane;
//import java.util.ArrayList;

import processing.app.*;
import processing.app.tools.Tool;
import processing.core.PApplet;


// http://dl.google.com/android/repository/repository.xml
// http://dl.google.com/android/android-sdk_r3-mac.zip
// http://dl.google.com/android/repository/tools_r03-macosx.zip

public class Android implements Tool {
  static String sdkPath;
  static String toolsPath;

  private Editor editor;
  Build build;
  
  String emulator;
  Process emulatorProcess;

  static final String ANDROID_SDK_PRIMARY = 
    "Is the Android SDK installed?";
  static final String ANDROID_SDK_SECONDARY = 
    "The Android SDK does not appear to be installed, <br>" +
    "because the ANDROID_SDK variable is not set. <br>" +
    "If it is installed, click “Yes” to select the <br>" +
    "location of the SDK, or “No” to visit the SDK<br>" + 
    "download site at http://developer.android.com/sdk.";
  static final String SELECT_ANDROID_SDK_FOLDER = 
    "Choose the location of the Android SDK";
  static final String NOT_ANDROID_SDK = 
    "The selected folder does not appear to contain an Android SDK.";
  static final String ANDROID_SDK_URL = 
    "http://developer.android.com/sdk/";
  
  
  public String getMenuTitle() {
    return "Android Mode";
  }


  public void init(Editor parent) {
    this.editor = parent;
  }


  public void run() {
    editor.statusNotice("Loading Android tools.");
    
    boolean success = checkPath();
    if (!success) {
      editor.statusNotice("Android mode canceled.");
      return;
    }
    success = Device.checkDefaults();
    if (!success) {
      editor.statusError("Could not load Android tools.");
      return;
    }
    editor.setHandlers(new RunHandler(), new PresentHandler(), 
                       new StopHandler(),
                       new ExportHandler(), new ExportAppHandler());
    build = new Build(editor);
    editor.statusNotice("Done loading Android tools.");
  }


  protected boolean checkPath() {
    Platform platform = Base.getPlatform();
    
    // The environment variable is king. The preferences.txt entry is a page.
    String envPath = platform.getenv("ANDROID_SDK");
    if (envPath != null) {
      sdkPath = envPath;
      // Just set the pref, in case it the ANDROID_SDK variable gets 
      // knocked out later. For instance, by that pesky Eclipse,  
      // which nukes all env variables when launching from the IDE.
      Preferences.set("android.sdk.path", envPath);

    } else {
      sdkPath = Preferences.get("android.sdk.path");

      if (sdkPath == null) {
        int result = Base.showYesNoQuestion(editor, "Android SDK", 
                                            ANDROID_SDK_PRIMARY,
                                            ANDROID_SDK_SECONDARY);
        if (result == JOptionPane.YES_OPTION) {
          File folder = 
            Base.selectFolder(SELECT_ANDROID_SDK_FOLDER, null, editor);
          if (folder != null) {
            boolean basicCheck = new File(folder, "tools/android").exists();
            if (basicCheck) {
              sdkPath = folder.getAbsolutePath();
              Preferences.set("android.sdk.path", sdkPath);              
            } else {
              // tools/android not found in the selected folder
              JOptionPane.showMessageDialog(editor, NOT_ANDROID_SDK);
              return false;
            }
          }
        } else if (result == JOptionPane.NO_OPTION) {
          // user admitted they don't have the SDK installed, and need help.
          Base.openURL(ANDROID_SDK_URL);
        }
      }
    }
    if (sdkPath == null) {  // still not interested?
      return false;
    }
    if (envPath == null) {
      platform.setenv("ANDROID_SDK", sdkPath);
    }
    //platform.setenv("ANDROID_SDK", "/opt/android");
    //sdkPath = platform.getenv("ANDROID_SDK");
    //System.out.println("sdk path is " + sdkPath);
    //  sdkPath = "/opt/android";

    // Make sure that the tools are in the PATH
    toolsPath = sdkPath + File.separator + "tools";
    String path = platform.getenv("PATH");
    platform.setenv("PATH", path + File.pathSeparator + toolsPath);
    //System.out.println("path after set is " + Base.getenv("PATH"));
    return true;
  }


  public Editor getEditor() {
    return editor;
  }
  
  
  public Sketch getSketch() {
    return editor.getSketch();
  }


  public Build getBuilder() {
    return build;
  }
  
  
  /**
   * Launch an emulator if not already running.
   * @return The name of the emulator.
   */
  public String findEmulator() {
    if (emulatorProcess != null) {
      // see if it's done yet
      try {
        //int result = emulatorProcess.exitValue();
        //emulatorProcess.destroy();
        emulatorProcess.exitValue();
        emulatorProcess = null;

      } catch (IllegalThreadStateException itse) {
        // not done yet, let's continue
      }
    }
    
    String portString = Preferences.get("android.emulator.port");
    if (portString == null) {
      portString = "5566";
      Preferences.set("android.emulator.port", portString);
    }
    //int port = Integer.parseInt(portString);
    String name = "emulator-" + portString;
    
    if (emulatorProcess != null) {  // this must be it, if we're running
      System.out.println("Found a perfectly good emulator. Trying that.");
      return name;
    }
    
    try {
      String[] devices = Debug.listDevices();
      for (String s : devices) {
        if (s.equals(name)) {
          return name;
        }
      }
    } catch (IOException e) {
      //e.printStackTrace();
      editor.statusError(e);
      return null;
    }
    
    //# starts and uses port 5554 for communication (but not logs)
    //emulator -avd gee1 -port 5554
    //# only informative messages and up (emulator -help-logcat for more info)
    //emulator -avd gee1 -logcat '*:i'
    //# faster boot
    //emulator -avd gee1 -logcat '*:i' -no-boot-anim 
    //# only get System.out and System.err
    //emulator -avd gee1 -logcat 'System.*:i' -no-boot-anim 
    //# though lots of messages aren't through System.*, so that's not great
    //# need to instead use the adb interface

    // adb -s emulator-5566 jdwp
    // prints a list of connections that can be made to the device
    // the final port will be the entry of the most recently started application
    // while launching, will say 'error: device offline'
    // when not running, will say 'error: device not found'

    // adb -s emulator-5566 -d forward tcp:29882 jdwp:736
    // jdb -connect com.sun.jdi.SocketAttach:hostname=localhost,port=29882
    
    // launch emulator because it's not running yet
    try {
      editor.statusNotice("Starting new Android emulator.");
      String[] cmd = new String[] { 
          "emulator", 
          "-avd", getDefaultDevice(),
          "-port", portString,
          //"-logcat", "'*:i'",
          "-no-boot-anim"
      };
      emulatorProcess = Runtime.getRuntime().exec(cmd);
      System.out.println(PApplet.join(cmd, " "));
      // make sure that the streams are drained properly
      new StreamRedirectThread("android-emulator-out", 
                               emulatorProcess.getInputStream(), System.out).start();
      new StreamRedirectThread("android-emulator-err", 
                               emulatorProcess.getErrorStream(), System.err).start();
      return name;
      
    } catch (IOException e) {
      //e.printStackTrace();
      editor.statusError(e);
      return null;
    }
  }


  public String getDefaultDevice() {
    return Device.avdDonut.name;
  }


  /** Find connected devices */
  public String findDevice() {
    String[] devices;
    try {
      devices = Debug.listDevices();
    } catch (IOException e) {
      editor.statusError(e);
      //e.printStackTrace();
      return null;
    }
//    ArrayList<String> available = new ArrayList<String>();
//    for (String s : devices) {
//    }
    // just go with the first non-emulator device
    for (String name : devices) {
      if (!name.startsWith("emulator-")) {
        return name;
      }
    }
    return null;
  }
  
  
//adb -s emulator-5556 install helloWorld.apk

//: adb -s HT91MLC00031 install bin/Brightness-debug.apk 
//532 KB/s (190588 bytes in 0.349s)
//  pkg: /data/local/tmp/Brightness-debug.apk
//Failure [INSTALL_FAILED_ALREADY_EXISTS]

//: adb -s HT91MLC00031 install -r bin/Brightness-debug.apk 
//1151 KB/s (190588 bytes in 0.161s)
//  pkg: /data/local/tmp/Brightness-debug.apk
//Success

//safe to just always include the -r (reinstall) flag

  // if user asks for 480x320, 320x480, 854x480 etc, then launch like that
  // though would need to query the emulator to see if it can do that
  
  public void installAndRun(String target, String device) {
    boolean success;
  
    Build build = getBuilder();
    success = build.createProject();
    if (!success) return;

    // now run the ant debug or release version
    success = build.antBuild("debug");
    //System.out.println("ant build complete " + success);
    if (!success) return;

    success = installSketch(device);
    if (!success) return;
    
    success = startSketch(device);
  }
  
  
  boolean installSketch(String device) {
    // install the new package into the emulator
    //System.out.println("installing onto emulator/device");
    boolean emu = device.startsWith("emulator");
    editor.statusNotice("Sending sketch to the " + 
                        (emu ? "emulator" : "phone") + ".");
    try {
      Device.sendMenuButton(device);  // wake up
      Device.sendHomeButton(device);  // kill any running app
      
      String[] cmd = new String[] { 
          "adb",
          "-s", device,
          //"wait-for-device",
          "install", "-r",  // safe to always use -r switch
          build.getPathForAPK("debug")
      };
      Process p = Runtime.getRuntime().exec(cmd);
      
      System.out.println();
      System.out.print("Install command: ");
      System.out.println(PApplet.join(cmd, " "));

      StringRedirectThread error = new StringRedirectThread(p.getErrorStream());
      StringRedirectThread output = new StringRedirectThread(p.getInputStream());

      int result = p.waitFor();
      if (result != 0) {
        for (String err : error.getLines()) {
          System.err.println(err);
        }
        //editor.statusNotice("“adb install” returned " + result + ".");
        //System.out.println("Could not install the sketch.");
        editor.statusError("Could not install the sketch.");
        System.out.println("“adb install” returned " + result + ".");
        return false;
        
      } else {
        String errorMsg = null;
        for (String out : output.getLines()) {
          if (out.startsWith("Failure")) {
//            String[] stuff = PApplet.match(out, "\\[(.*)\\]");
//            if (stuff != null) {
//              errorMsg = stuff[1];
//            } else {
            errorMsg = out.substring(8);
//            }
            System.err.println(out);
          } else {
            System.out.println(out);
          }
        }
        if (errorMsg == null) {
          editor.statusNotice("Done installing.");
          return true;

        } else {
          editor.statusError("Error while installing " + errorMsg);
        }
      }
    } catch (IOException e) {
      editor.statusError(e);

    } catch (InterruptedException e) { }
    
    return false;
  }    


  // better version that actually runs through JDI:
  // http://asantoso.wordpress.com/2009/09/26/using-jdb-with-adb-to-debugging-of-android-app-on-a-real-device/
  boolean startSketch(String device) {
    try {
      Device.sendMenuButton(device);  // wake up
      Device.sendHomeButton(device);  // kill any running app
      
      //"am start -a android.intent.action.MAIN -n com.android.browser/.BrowserActivity"
      Process p = Runtime.getRuntime().exec(new String[] { 
          "adb",
          "-s", device,
          //"-d",  // this is for a single USB device 
          "shell", "am", "start",  // kick things off
          "-e", "debug", "true",
          "-a", "android.intent.action.MAIN", "-n",
          build.getPackageName() + "/." + build.getClassName()          
      });
      int result = p.waitFor();
      if (result != 0) {
        editor.statusError("Could not start the sketch.");
        System.out.println("“adb shell” for “am start” returned " + result + ".");
        
      } else {
        boolean emu = device.startsWith("emulator");
        editor.statusNotice("Sketch started on the " + 
                            (emu ? "emulator" : "phone") + ".");
        
        return true;
      }
    } catch (IOException e) {
      editor.statusError(e);

    } catch (InterruptedException e) { }
    
    return false;
  }
  
  
  class RunHandler implements Runnable {
    public void run() {
      installAndRun("debug", findEmulator());
    } 
  }
  
  
  class PresentHandler implements Runnable {
    public void run() {
      String device = findDevice();
      if (device == null) {
        editor.statusError("No device found.");
      } else {
        installAndRun("debug", device);
      }
    }
  }

  
  class StopHandler implements Runnable {
    public void run() {
    }
  }
  
  
  class ExportHandler implements Runnable {  
    public void run() {      
    }
  }


  class ExportAppHandler implements Runnable {  
    public void run() {      
    }
  }
}