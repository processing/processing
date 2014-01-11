package processing.mode.experimental;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import processing.app.Base;
import processing.app.Sketch;

public class AutoSaveUtil {
  
  private DebugEditor editor;
  
  private Timer timer;
  
  private int saveTime;
  
  /**
   * 
   * @param dedit
   * @param timeOut - in minutes
   */
  public AutoSaveUtil(DebugEditor dedit, int timeOut){
    editor = dedit;
    if (timeOut < 5) {
      saveTime = -1;
      throw new IllegalArgumentException("");      
    }
    else{
      saveTime = timeOut * 60 * 1000;
    }
  }
  
  public void init(){
    if(saveTime < 1000) return;
    saveTime = 3000;
    timer = new Timer();
    timer.schedule(new SaveTask(), saveTime, saveTime);
  }
  
  public void shutDown(){
    timer.cancel();
  }
  
  private boolean saveSketch() throws IOException{
    
    Sketch sc = editor.getSketch();
    File autosaveDir = new File(sc.getFolder().getAbsolutePath() + File.separator + ".autosave");
    boolean deleteOldSave = false;
    String oldSave = null;
    if(!autosaveDir.exists()){
      autosaveDir = new File(sc.getFolder().getAbsolutePath(), ".autosave");
      autosaveDir.mkdir();
    }
    else
    {
      // delete the previous backup after saving current one.
      String prevSaves[] = Base.listFiles(autosaveDir, false);
      if(prevSaves.length > 0){
        deleteOldSave = true;
        oldSave = prevSaves[0];
      }
    }
    String newParentDir = autosaveDir + File.separator + System.currentTimeMillis();
    String newName = sc.getName();

    
    // check on the sanity of the name
    String sanitaryName = Sketch.checkName(newName);
    File newFolder = new File(newParentDir, sanitaryName);
    if (!sanitaryName.equals(newName) && newFolder.exists()) {
      Base.showMessage("Cannot Save",
                       "A sketch with the cleaned name\n" +
                       "“" + sanitaryName + "” already exists.");
      return false;
    }
    newName = sanitaryName;

//    String newPath = newFolder.getAbsolutePath();
//    String oldPath = folder.getAbsolutePath();

//    if (newPath.equals(oldPath)) {
//      return false;  // Can't save a sketch over itself
//    }

    // make sure there doesn't exist a tab with that name already
    // but ignore this situation for the first tab, since it's probably being
    // resaved (with the same name) to another location/folder.
    for (int i = 1; i < sc.getCodeCount(); i++) {
      if (newName.equalsIgnoreCase(sc.getCode()[i].getPrettyName())) {
        Base.showMessage("Nope",
                         "You can't save the sketch as \"" + newName + "\"\n" +
                         "because the sketch already has a tab with that name.");
        return false;
      }
    }

    

    // if the new folder already exists, then first remove its contents before
    // copying everything over (user will have already been warned).
    if (newFolder.exists()) {
      Base.removeDir(newFolder);
    }
    // in fact, you can't do this on Windows because the file dialog
    // will instead put you inside the folder, but it happens on OS X a lot.

    // now make a fresh copy of the folder
    newFolder.mkdirs();

    // grab the contents of the current tab before saving
    // first get the contents of the editor text area
    if (sc.getCurrentCode().isModified()) {
      sc.getCurrentCode().setProgram(editor.getText());
    }

    File[] copyItems = sc.getFolder().listFiles(new FileFilter() {
      public boolean accept(File file) {
        String name = file.getName();
        // just in case the OS likes to return these as if they're legit
        if (name.equals(".") || name.equals("..")) {
          return false;
        }
        // list of files/folders to be ignored during "save as"
        for (String ignorable : editor.getMode().getIgnorable()) {
          if (name.equals(ignorable)) {
            return false;
          }
        }
        // ignore the extensions for code, since that'll be copied below
        for (String ext : editor.getMode().getExtensions()) {
          if (name.endsWith(ext)) {
            return false;
          }
        }
        // don't do screen captures, since there might be thousands. kind of
        // a hack, but seems harmless. hm, where have i heard that before...
        if (name.startsWith("screen-")) {
          return false;
        }
        return true;
      }
    });
    // now copy over the items that make sense
    for (File copyable : copyItems) {
      if (copyable.isDirectory()) {
        Base.copyDir(copyable, new File(newFolder, copyable.getName()));
      } else {
        Base.copyFile(copyable, new File(newFolder, copyable.getName()));
      }
    }

    // save the other tabs to their new location
    for (int i = 1; i < sc.getCodeCount(); i++) {
      File newFile = new File(newFolder, sc.getCode()[i].getFileName());
      sc.getCode()[i].saveAs(newFile);
    }

    // While the old path to the main .pde is still set, remove the entry from
    // the Recent menu so that it's not sticking around after the rename.
    // If untitled, it won't be in the menu, so there's no point.
//    if (!isUntitled()) {
//      editor.removeRecent();
//    }

    // save the main tab with its new name
    File newFile = new File(newFolder, newName + ".pde");
    sc.getCode()[0].saveAs(newFile);

//    updateInternal(newName, newFolder);
//
//    // Make sure that it's not an untitled sketch
//    setUntitled(false);
//
//    // Add this sketch back using the new name
//    editor.addRecent();

    // let Editor know that the save was successful
    
    if(deleteOldSave)
      Base.removeDir(new File(oldSave));
    
    return true;
  }
  
  private class SaveTask extends TimerTask{

    @Override
    public void run() {
      try {
        saveSketch();
        ExperimentalMode.log("Saved " + editor.getSketch().getMainFilePath());
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      
      //editor
      
      
    }
    
  }

  public static void main(String[] args) {

  }

}
