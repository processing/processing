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
  
  private File autosaveDir, pastSave;
  
  private boolean isSaving;
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
      ExperimentalMode.log("AutoSaver Interval(mins): " + timeOut);
    }
    autosaveDir = new File(editor.getSketch().getFolder().getAbsolutePath() + File.separator + "_autosave");
  }
  
  public boolean checkForPastSave(){
    if(autosaveDir.exists()){
      String prevSaves[] = Base.listFiles(autosaveDir, false);
      if(prevSaves.length > 0){
       File t = new File(Base.listFiles(new File(prevSaves[0]), false)[0]);
       pastSave = new File(t.getAbsolutePath() + File.separator + t.getName() + ".pde");
       if(pastSave.exists())
       return true;
      }
    }
    return false;
  }
  
  public void reloadAutosaveDir(){
    while(isSaving);
    autosaveDir = new File(editor.getSketch().getFolder().getAbsolutePath() + File.separator + "_autosave");
  }
  
  public File getPastSave(){
    return pastSave;
  }
  
  public void init(){
    if(saveTime < 10000) saveTime = 10 * 1000;
    //saveTime = 10 * 1000; //TODO: remove
    timer = new Timer();
    timer.schedule(new SaveTask(), saveTime, saveTime);
    isSaving = false;
    ExperimentalMode.log("AutoSaver started");
  }
  
  public void stop(){
    while(isSaving); // save operation mustn't be interrupted
    if(timer != null) timer.cancel();
    Base.removeDir(autosaveDir);
  }
  
  private boolean saveSketch() throws IOException{
    if(!editor.getSketch().isModified()) return false;
    isSaving = true;
    Sketch sc = editor.getSketch();
    
    boolean deleteOldSave = false;
    String oldSave = null;
    if(!autosaveDir.exists()){
      autosaveDir = new File(sc.getFolder().getAbsolutePath(), "_autosave");
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
      isSaving = false;
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
        isSaving = false;
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
    
    if(deleteOldSave){
      Base.removeDir(new File(oldSave));
    }
    isSaving = false;
    return true;
  }
  
  private class SaveTask extends TimerTask{

    @Override
    public void run() {
      try {
        if(saveSketch())
          ExperimentalMode.log("Backup Saved " + editor.getSketch().getMainFilePath());
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
