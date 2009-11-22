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
//import java.util.ArrayList;

import processing.app.*;
import processing.app.tools.Tool;
import processing.core.PApplet;


// http://dl.google.com/android/android-sdk_r3-mac.zip
// http://dl.google.com/android/repository/repository.xml
// http://dl.google.com/android/repository/tools_r03-macosx.zip

public class Android implements Tool {
  static String sdkPath;
  static String toolsPath;

  private Editor editor;
  Build build;
  
  String emulator;
  Process emulatorProcess;

  
  public String getMenuTitle() {
    return "Android";
  }


  public void init(Editor parent) {
    this.editor = parent;
  }


  public void run() {
    editor.statusNotice("Loading Android tools.");
    
    checkPath();
    Device.checkDefaults();

    editor.setHandlers(new RunHandler(this), new PresentHandler(this), 
                       new ExportHandler(this), new ExportAppHandler(this));
    build = new Build(editor);
    editor.statusNotice("Done loading Android tools.");
  }


  static protected void checkPath() {
    Platform platform = Base.getPlatform();
    //  System.out.println("PATH is " + Base.getenv("PATH"));
    //  System.out.println("PATH from System is " + System.getenv("PATH"));
    if (platform.getenv("ANDROID_SDK") == null) {
      platform.setenv("ANDROID_SDK", "/opt/android");
    }
    sdkPath = platform.getenv("ANDROID_SDK");
//    System.out.println("sdk path is " + sdkPath);
    //  sdkPath = "/opt/android";
    String toolsPath = sdkPath + File.separator + "tools";
    platform.setenv("PATH", platform.getenv("PATH") + File.pathSeparator + toolsPath);
//    System.out.println("path after set is " + Base.getenv("PATH")); 
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
    String portString = Preferences.get("android.emulator.port");
    if (portString == null) {
      portString = "5566";
      Preferences.set("android.emulator.port", portString);
    }
    //int port = Integer.parseInt(portString);
    String name = "emulator-" + portString;
    
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
    
    // launch emulator because it's not running yet
    try {
      editor.statusNotice("Starting new Android emulator.");
      emulatorProcess = Runtime.getRuntime().exec(new String[] { 
          "emulator", 
          "-avd", getDefaultDevice(),
          "-port", portString,
          "-logcat", "'*:i'",
          "-no-boot-anim"
      });
      System.out.println(PApplet.join(new String[] { 
          "emulator", 
          "-avd", getDefaultDevice(),
          "-port", portString,
          "-logcat", "'*:i'",
          "-no-boot-anim"
      }, " "));
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

    //  // if no emulator is running, start an emulator
    //  String name = findEmulator();

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
          "shell", "am", "start", 
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
}