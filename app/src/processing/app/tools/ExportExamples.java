package processing.app.tools;

import java.io.*;
//import java.util.HashMap;

import processing.app.*;
import processing.mode.java.JavaBuild;

public class ExportExamples implements Tool {
  static final String DELETE_TARGET = "export.delete_target_folder";
  static final String SEPARATE_JAR = "export.applet.separate_jar_files";

//  HashMap<String,Throwable> errors;
//  Editor orig;

  Base base;
  File[] folders;
  
  
  public void init(Editor editor) {
//    orig = editor;
    base = editor.getBase();
    Mode mode = editor.getMode();
    folders = mode.getExampleCategoryFolders();
  }


  public void run() {
    new Thread(new Runnable() { public void run() {
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
    } }).start();

//    if (errors.size() > 0) {
//      orig.statusError((errors.size() == 1 ? "One sketch" : (errors.size() + " sketches")) + " had errors.");
//    } else {
//      orig.statusNotice("Finished exporting examples.");
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
    JavaBuild build = new JavaBuild(sketch);    
    File appletFolder = new File(sketch.getFolder(), "applet");
    return build.exportApplet(appletFolder);
  }

  
  public String getMenuTitle() {
    return "Export Examples";
  }
}