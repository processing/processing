package processing.app.tools.android;

//import java.awt.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

//import javax.swing.*;

import processing.app.*;
import processing.app.debug.RunnerException;
import processing.app.preproc.PdePreprocessor;
import processing.app.tools.Tool;
import processing.core.PApplet;


public class Build implements Tool {
  Editor editor;

  SimpleDateFormat dateFormat = new SimpleDateFormat("yyMMdd.HHmm");
  
  String defaultPackage = "processing.android.test";
  String sdkLocation = "/opt/android";
  

  public String getMenuTitle() {
    return "Android";
  }

  
  public void init(Editor parent) {
    this.editor = parent;
    
    /*
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        JMenuBar mb = null;
        while (mb == null) {
          mb = editor.getJMenuBar();
          if (mb != null) {
            int menuCount = mb.getMenuCount();
            for (int i = 0; i < menuCount; i++) {
              JMenu menu = mb.getMenu(i);
              String menuName = menu.getName();
              System.out.println(menu.getName());
              if (menuName == null) continue;
              //if (menu.getName().equals("Tools")) {
//              if (menuName.equals("Tools")) {
                int itemCount = menu.getItemCount();
                for (int j = 0; j < itemCount; j++) {
                  JMenuItem item = menu.getItem(j);
                  System.out.println(item.getName());
                  if (item.getName().equals("Android")) {
                    System.out.println("done");
                    int modifiers = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
                    item.setAccelerator(KeyStroke.getKeyStroke('D', modifiers));
                    //item.setShortcut(new MenuShortcut('D'));
                    return;
                  }
                }
//              }
            }
          } 
          try {
            Thread.sleep(100);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }
      }
    });
    */
  }

  
  public void run() {
    Sketch sketch = editor.getSketch();
    
    // Create the 'android' build folder, and move any existing version out. 
    File androidFolder = new File(sketch.getFolder(), "android");
    if (androidFolder.exists()) {
      Date mod = new Date(androidFolder.lastModified());
      File dest = new File(sketch.getFolder(), "android." + dateFormat.format(mod));
      boolean result = androidFolder.renameTo(dest);
      if (!result) {
        Base.showWarning("Failed to rename", 
                         "Could not rename the old \"android\" build folder.\n" + 
                         "Please delete, close, or rename the folder\n" + 
                         androidFolder.getAbsolutePath() + "\n" +  
                         "and try again." , null);
        Base.openFolder(sketch.getFolder());
        return;
      }
    } else {
      boolean result = androidFolder.mkdirs();
      if (!result) {
        Base.showWarning("Folders, folders, folders", 
                         "Could not create the necessary folders to build.\n" +
                         "Perhaps you have some permissions to sort out?", null);
        return;
      }
    }

    // Create the 'src' folder with the preprocessed code.
    File srcFolder = new File(androidFolder, "src");
    File javaFolder = new File(srcFolder, defaultPackage.replace('.', '/'));
    javaFolder.mkdirs();
    //File srcFile = new File(actualSrc, className + ".java");
    String buildPath = javaFolder.getAbsolutePath();
    
    String prefsLine = Preferences.get("preproc.imports");
//    System.out.println("imports are " + prefsLine);
    Preferences.set("preproc.imports", "");
    
    try {
      // need to change to a better set of imports here

      String className = sketch.preprocess(buildPath, new Preproc());
      if (className != null) {
        writeAndroidManifest(new File(androidFolder, "AndroidManifest.xml"), className);      
        writeBuildProps(new File(androidFolder, "build.properties"));
        File buildFile = new File(androidFolder, "build.xml");
        writeBuildXML(buildFile, sketch.getName());
        writeDefaultProps(new File(androidFolder, "default.properties"));
        writeLocalProps(new File(androidFolder, "local.properties"));
        writeRes(new File(androidFolder, "res"), className);
        writeLibs(new File(androidFolder, "libs"));
        Base.openFolder(androidFolder);

        // looking for BUILD SUCCESSFUL or BUILD FAILED
//        Process p = Runtime.getRuntime().exec(new String[] { 
//          "ant", "-f", buildFile.getAbsolutePath()
//        });
//        BufferedReader stdout = PApplet.createReader(p.getInputStream());
//        String line = null; 
      }
//    } catch (IOException ioe) {
//      ioe.printStackTrace();
    } catch (RunnerException e) {
      e.printStackTrace();
    }
    
    // set this back, even if there's an error
    Preferences.set("preproc.imports", prefsLine);    
  }
  
  
  class Preproc extends PdePreprocessor {
    public int writeImports(PrintStream out) {
      out.println("package " + defaultPackage + ";");
      out.println();
      return super.writeImports(out);
    }
  }


  /*
android create project -t 3 -n test_name -p test_path -a test_activity -k test.pkg
file:///opt/android/docs/guide/developing/other-ide.html
# compile code for a project
ant debug
# this pulls in tons of other .jar files for android ant 
# local.properties (in the android folder) has the sdk location

# starts and uses port 5554 for communication (but not logs)
emulator -avd gee1 -port 5554

# only informative messages and up (emulator -help-logcat for more info)
emulator -avd gee1 -logcat '*:i'
# faster boot
emulator -avd gee1 -logcat '*:i' -no-boot-anim 
# only get System.out and System.err
emulator -avd gee1 -logcat 'System.*:i' -no-boot-anim 
# though lots of messages aren't through System.*, so that's not great

# need to instead use the adb interface
   */


  void writeAndroidManifest(File file, String className) {
    PrintWriter writer = PApplet.createWriter(file);
    writer.println("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
    writer.println("<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\" ");
    writer.println("          package=\"" + defaultPackage + "\" ");
    writer.println("          android:versionCode=\"1\" ");
    writer.println("          android:versionName=\"1.0\">");
    writer.println("  <application android:label=\"@string/app_name\">");
    writer.println("    <activity android:name=\"." + className + "\"");
    writer.println("              android:label=\"@string/app_name\">");
    writer.println("      <intent-filter>");
    writer.println("        <action android:name=\"android.intent.action.MAIN\" />");
    writer.println("        <category android:name=\"android.intent.category.LAUNCHER\" />");
    writer.println("      </intent-filter>");
    writer.println("    </activity>");
    writer.println("  </application>");
    writer.println("</manifest>");
    writer.flush();
    writer.close();
  }
  
  
  void writeBuildProps(File file) {
    PrintWriter writer = PApplet.createWriter(file);
    writer.println("application-package=" + defaultPackage);
    writer.flush();
    writer.close();
  }
  
  
  void writeBuildXML(File file, String projectName) {
    PrintWriter writer = PApplet.createWriter(file);
    writer.println("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
    
    writer.println("<project name=\"" + projectName + "\" default=\"help\">");
    writer.println("  <property file=\"local.properties\"/>");
    writer.println("  <property file=\"build.properties\"/>");
    writer.println("  <property file=\"default.properties\"/>");

    writer.println("  <path id=\"android.antlibs\">");
    writer.println("    <pathelement path=\"${sdk-location}/tools/lib/anttasks.jar\" />");
    writer.println("    <pathelement path=\"${sdk-location}/tools/lib/sdklib.jar\" />");
    writer.println("    <pathelement path=\"${sdk-location}/tools/lib/androidprefs.jar\" />");
    writer.println("    <pathelement path=\"${sdk-location}/tools/lib/apkbuilder.jar\" />");
    writer.println("    <pathelement path=\"${sdk-location}/tools/lib/jarutils.jar\" />");
    writer.println("  </path>");

    writer.println("  <taskdef name=\"setup\"");
    writer.println("           classname=\"com.android.ant.SetupTask\"");
    writer.println("           classpathref=\"android.antlibs\" />");

    writer.println("  <setup />");
    
    writer.println("</project>");
    writer.flush();
    writer.close();
  }
  
  
  void writeDefaultProps(File file) {
    PrintWriter writer = PApplet.createWriter(file);
    writer.println("target=Google Inc.:Google APIs:3");
    writer.flush();
    writer.close();
  }
  
  
  void writeLocalProps(File file) {
    PrintWriter writer = PApplet.createWriter(file);
    writer.println("sdk-location=" + sdkLocation);
    writer.flush();
    writer.close();
  }
  
  
  void writeRes(File resFolder, String className) {
    File layoutFolder = new File(resFolder, "layout");
    layoutFolder.mkdirs();
    File layoutFile = new File(layoutFolder, "main.xml");
    writeResLayoutMain(layoutFile);
    File valuesFolder = new File(resFolder, "values");
    valuesFolder.mkdirs();
    File stringsFile = new File(valuesFolder, "strings.xml");
    writeResValuesStrings(stringsFile, className);
  }
  
  
  void writeResLayoutMain(File file) {
    PrintWriter writer = PApplet.createWriter(file);
    writer.println("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
    writer.println("<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"");
    // this was in the bageldroid version
    //android:id="@+id/fullscreen_layout"
    writer.println("              android:orientation=\"vertical\"");
    writer.println("              android:layout_width=\"fill_parent\"");
    writer.println("              android:layout_height=\"fill_parent\">");
//    <TextView  
//        android:layout_width="fill_parent" 
//        android:layout_height="wrap_content" 
//        android:text="Hello World, test_activity"
//        />
    writer.println("</LinearLayout>");
    writer.flush();
    writer.close();
  }
  
  
  void writeResValuesStrings(File file, String className) {
    PrintWriter writer = PApplet.createWriter(file);
    writer.println("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
    writer.println("<resources>");
    writer.println("  <string name=\"app_name\">" + className + "</string>");
    writer.println("</resources>");
    writer.flush();
    writer.close();
  }
  
  
  void writeLibs(File libsFolder) {
    libsFolder.mkdirs();
    InputStream input = getClass().getResourceAsStream("processing-core.zip");
    PApplet.saveStream(new File(libsFolder, "processing-core.jar"), input);
  }
}