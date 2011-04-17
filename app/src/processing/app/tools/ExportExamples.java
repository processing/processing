package processing.app.tools;

import java.io.*;
//import java.util.HashMap;

import processing.app.*;
import processing.mode.java.JavaBuild;

public class ExportExamples implements Tool {
  static final String DELETE_TARGET = "export.delete_target_folder";
  static final String SEPARATE_JAR = "export.applet.separate_jar_files";

//  HashMap<String,Throwable> errors;
  Editor orig;

  // copy the files to processing.org/content/examples with their hierarchy intact
  // that won't be checked into svn, even though they were before

//  File webroot;
//  File templates;
//  File xml; 
  
  Base base;
  File[] folders;
  File outputFolder;
  String examplesPath;
  
  
  public void init(Editor editor) {
    orig = editor;
    base = editor.getBase();
    Mode mode = editor.getMode();
    folders = mode.getExampleCategoryFolders();
    examplesPath = mode.getExamplesFolder().getAbsolutePath();

    // Not perfect, but will work for Casey and I
    File desktop = new File(System.getProperty("user.home"), "Desktop");
    outputFolder = new File(desktop, "examples"); 
//    webroot = new File("/Users/fry/coconut/processing.web");
//    templates = new File(webroot, "templates");    
  }


  public void run() {
    new Thread(new Runnable() { public void run() {
      if (outputFolder.exists()) {
        Base.showWarning("Try Again", "Please remove the examples folder from the desktop,\n" + 
                         "because that's where I wanna put things.", null);
        return;
      }
//    errors = new HashMap<String, Throwable>();
    boolean delete = Preferences.getBoolean(DELETE_TARGET);
    Preferences.setBoolean(DELETE_TARGET, false);
    boolean separate = Preferences.getBoolean(SEPARATE_JAR);
    Preferences.setBoolean(SEPARATE_JAR, true);
    
    for (File folder : folders) {
      if (!folder.getName().equals("Books")) {
        handleFolder(folder);
      }
    }

    Preferences.setBoolean(DELETE_TARGET, delete);
    Preferences.setBoolean(SEPARATE_JAR, separate);
    orig.statusNotice("Finished exporting examples.");
    } }).start();

//    if (errors.size() > 0) {
//      orig.statusError((errors.size() == 1 ? "One sketch" : (errors.size() + " sketches")) + " had errors.");
//    } else {
//    }
//    for (String path : errors.keySet()) {
//      System.err.println("Error: " 
//    }
  }
  
  
  public void handleFolder(File folder) {
    File pdeFile = new File(folder, folder.getName() + ".pde");
    if (pdeFile.exists()) {
      String pdePath = pdeFile.getAbsolutePath();
      Editor editor = base.handleOpen(pdePath);
      if (editor != null) {
        try {
//          System.out.println(pdePath);
          if (handle(editor)) {
            base.handleClose(editor, false);
            try {
              Thread.sleep(20);
            } catch (InterruptedException e) { }
          }
        } catch (Exception e) {
          e.printStackTrace();
          //          errors.put(pdePath, e);
          //          System.err.println("Error handling " + pdePath);
          //          e.printStackTrace();
        }
      }
    } else {  // recurse into the folder
      //System.out.println("  into " + folder.getAbsolutePath());
      File[] sub = folder.listFiles();
      for (File f : sub) {
        if (f.isDirectory()) {
          handleFolder(f);
        }
      }
    }
  }
  
  
  public boolean handle(Editor editor) throws SketchException, IOException {
    Sketch sketch = editor.getSketch();
    File sketchFolder = sketch.getFolder();
    String sketchPath = sketchFolder.getAbsolutePath();
    String uniquePath = sketchPath.substring(examplesPath.length());
    File sketchTarget = new File(outputFolder, uniquePath);
    
    // copy the PDE files so that they can be pulled in by the generator script
//    File[] files = sketchFolder.listFiles();
//    for (File file : files) {
//      if (file.getName().endsWith(".pde")) {
//        Base.copyFile(file, new File(sketchTarget, file.getName()));
//      }
//    }
    // no need to do this because the source files will be in 'applet' anyway
    
    // build the applet into this folder
    File appletFolder = new File(sketchTarget, "applet");
    JavaBuild build = new JavaBuild(sketch);
    boolean result = build.exportApplet(appletFolder);
    
    // Just one copy of core.jar into the root
    File coreTarget = new File(outputFolder, "core.jar");
    File sketchCore = new File(appletFolder, "core.jar");
    if (!coreTarget.exists()) {
      Base.copyFile(sketchCore, coreTarget);
    }
    sketchCore.delete();
    
    new File(appletFolder, "index.html").delete();
    new File(appletFolder, "loading.gif").delete();
    new File(appletFolder, sketch.getName() + ".java").delete();

    return result;
  }

  
  public String getMenuTitle() {
    return "Export Examples";
  }
}