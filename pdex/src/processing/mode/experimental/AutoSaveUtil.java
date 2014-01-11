package processing.mode.experimental;

import java.util.Timer;
import java.util.TimerTask;

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
    saveTime = 1000;
    timer = new Timer();
    timer.schedule(new SaveTask(), saveTime, saveTime);
  }
  
  private class SaveTask extends TimerTask{

    @Override
    public void run() {
      ExperimentalMode.log("Saved " + editor.getSketch().getMainFilePath());
    }
    
  }

  public static void main(String[] args) {

  }

}
