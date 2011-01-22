/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
 Part of the Processing project - http://processing.org

 Copyright (c) 2010 Ben Fry and Casey Reas

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

package processing.mode.android;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

import processing.app.*;
import processing.core.PApplet;
import processing.xml.XMLElement;


public class Manifest {
  static final String MANIFEST_XML = "AndroidManifest.xml";

  static final String WORLD_OF_HURT_COMING =  
    "Errors occurred while reading or writing " + MANIFEST_XML + ",\n" + 
    "which means lots of things are likely to stop working properly.\n" +
    "To prevent losing any data, it's recommended that you use “Save As”\n" + 
    "to save a separate copy of your sketch, and the restart Processing."; 
  static final String MULTIPLE_ACTIVITIES = 
    "Processing only supports a single Activity in the AndroidManifest.xml\n" +
    "file. Only the first activity entry will be updated, and you better \n" + 
    "hope that's the right one, smartypants.";

//  private Editor editor;
  private Sketch sketch;
  
  // entries we care about from the manifest file
//  private String packageName;
  
  /** the manifest data read from the file */
  private XMLElement xml;


//  public Manifest(Editor editor) {
//    this.editor = editor;
//    this.sketch = editor.getSketch();
//    load();
//  }
  public Manifest(Sketch sketch) {
    this.sketch = sketch;
    load();
  }
  
  
  private String defaultPackageName() {
//    Sketch sketch = editor.getSketch();
    return AndroidBuild.basePackage + "." + sketch.getName().toLowerCase();
  }
  

  // called by other classes who want an actual package name
  // internally, we'll figure this out ourselves whether it's filled or not
  public String getPackageName() {
    String pkg = xml.getString("package");
    return pkg.length() == 0 ? defaultPackageName() : pkg;
  }
  
  
  public void setPackageName(String packageName) {
//    this.packageName = packageName;
    // this is the package attribute in the root <manifest> object
    xml.setString("package", packageName);
    save();
  }
  
  
//writer.println("  <uses-permission android:name=\"android.permission.INTERNET\" />");
//writer.println("  <uses-permission android:name=\"android.permission.WRITE_EXTERNAL_STORAGE\" />");
  static final String PERMISSION_PREFIX = "android.permission.";
  
  public String[] getPermissions() {
    XMLElement[] elements = xml.getChildren("uses-permission");
    int count = elements.length;
    String[] names = new String[count];
    for (int i = 0; i < count; i++) {
      names[i] = elements[i].getString("android:name").substring(PERMISSION_PREFIX.length());
    }
    return names;
  }
  
  
  public void setPermissions(String[] names) {
    // just remove all the old ones
    for (XMLElement kid : xml.getChildren("uses-permission")) {
      xml.removeChild(kid);
    }
    // ...and add the new kids back
    for (String name : names) {
      XMLElement newbie = new XMLElement("uses-permission");
      newbie.setString("android:name", PERMISSION_PREFIX + name);
      xml.addChild(newbie);
    }
    save();
  }


  public void setClassName(String className) {
    XMLElement[] kids = xml.getChildren("application/activity");
    if (kids.length != 1) {
      Base.showWarning("Don't touch that", MULTIPLE_ACTIVITIES, null);
    }
    XMLElement activity = kids[0];
    String currentName = activity.getString("android:name");
    // only update if there are changes
    if (currentName == null || !currentName.equals(className)) {
      activity.setString("android:name", "." + className);
      save();
    }
  }


  private void writeBlankManifest(final File file) {
    final PrintWriter writer = PApplet.createWriter(file);
    writer.println("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
    writer.println("<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\" ");
//    writer.println("          package=\"" + defaultPackageName() + "\" ");
    writer.println("          package=\"\" ");
    
    // Tempting to use 'preferExternal' here, but might annoy some users. 
    // 'auto' at least enables it to be moved back and forth
    // http://developer.android.com/guide/appendix/install-location.html
//    writer.println("          android:installLocation=\"auto\" ");
    // Disabling this for now (0190), requires default.properties to use API 8

    // This is just a number (like the Processing 'revision'). It should 
    // increment with each release. Perhaps P5 should do this automatically
    // with each build or read/write of the manifest file?
    writer.println("          android:versionCode=\"1\" ");
    // This is the version number/name seen by users
    writer.println("          android:versionName=\"1.0\">");

    // for now including this... we're wiring to a particular SDK version anyway...
    writer.println("  <uses-sdk android:minSdkVersion=\"" + AndroidBuild.sdkVersion + "\" />");
//    writer.println("  <uses-sdk android:minSdkVersion=\"\" />");  // insert sdk version
//    writer.println("  <application android:label=\"@string/app_name\"");
    writer.println("  <application android:label=\"\"");  // insert pretty name
    writer.println("               android:icon=\"@drawable/icon\"");
    writer.println("               android:debuggable=\"true\">");

    // turns out label is not required for the activity, so nixing it
//    writer.println("    <activity android:name=\"\"");  // insert class name prefixed w/ dot
////    writer.println("              android:label=\"@string/app_name\">");  // pretty name
//    writer.println("              android:label=\"\">");

    // activity/android:name should be the full name (package + class name) of 
    // the actual activity class. or the package can be replaced by a single 
    // dot as a prefix as an easier shorthand.
    writer.println("    <activity android:name=\"\">");

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


  /**
   * Save a new version of the manifest info to the build location. 
   * Also fill in any missing attributes that aren't yet set properly.
   */
  protected void writeBuild(File file, String className, 
                            boolean debug) throws IOException {
    // write a copy to the build location
    save(file);
    
    // load the copy from the build location and start messing with it
    XMLElement mf = new XMLElement(new FileReader(file));

    // package name, or default
    String p = mf.getString("package").trim();
    if (p.length() == 0) {
      mf.setString("package", defaultPackageName());
    }

    // app name and label, or the class name
    XMLElement app = mf.getChild("application");
    String label = app.getString("android:label");
    if (label.length() == 0) {
      app.setString("android:label", className);
    }
    app.setString("android:debuggable", debug ? "true" : "false");

    XMLElement activity = app.getChild("activity");
    // the '.' prefix is just an alias for the full package name
    // http://developer.android.com/guide/topics/manifest/activity-element.html#name
    activity.setString("android:name", "." + className);  // this has to be right

    PrintWriter writer = PApplet.createWriter(file);
    mf.write(writer);
    writer.close();
  }


  protected void load() {
//    Sketch sketch = editor.getSketch();
//    File manifestFile = new File(sketch.getFolder(), MANIFEST_XML);
//    XMLElement xml = null;
    File manifestFile = getManifestFile();
    if (manifestFile.exists()) {
      try {
        xml = new XMLElement(new FileReader(manifestFile));
      } catch (Exception e) {
        e.printStackTrace();
        System.err.println("Problem reading AndroidManifest.xml, creating a new version");
        
        // remove the old manifest file, rename it with date stamp
        long lastModified = manifestFile.lastModified();
        String stamp = AndroidMode.getDateStamp(lastModified);
        File dest = new File(sketch.getFolder(), MANIFEST_XML + "." + stamp);
        boolean moved = manifestFile.renameTo(dest);
        if (!moved) {
          System.err.println("Could not move/rename " + manifestFile.getAbsolutePath());
          System.err.println("You'll have to move or remove it before continuing.");
          return;
        }
      }
    } 
    if (xml == null) {
      writeBlankManifest(manifestFile);
      try {
        xml = new XMLElement(new FileReader(manifestFile));
      } catch (FileNotFoundException e) {
        System.err.println("Could not read " + manifestFile.getAbsolutePath());
        e.printStackTrace();
      }
    }
    if (xml == null) {
      Base.showWarning("Error handling " + MANIFEST_XML, WORLD_OF_HURT_COMING, null);
    }
//    return xml;
  }
  
  
  protected void save() {
    save(getManifestFile());
  }

  
  /**
   * Save to the sketch folder, so that it can be copied in later.
   */
  protected void save(File file) {
    PrintWriter writer = PApplet.createWriter(file);
    xml.write(writer);
    writer.close();
  }


  private File getManifestFile() {
    return new File(sketch.getFolder(), MANIFEST_XML);
  }
}