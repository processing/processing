package processing.app;

import java.awt.Point;
import java.io.*;

/**
 *
 * @author  Francis Li
 */
public class Emulator extends Runner {
  
  /** Creates a new instance of Emulator */
  public Emulator(Sketch sketch, Editor editor) {
    super(sketch, editor);
  }
  
  public void start(Point windowLocation) throws RunnerException {
    try{
      String wtkPath = Preferences.get("wtk.path");
      String wtkBinPath = wtkPath + File.separator + "bin";
      
      StringBuffer command = new StringBuffer();
      if (Base.isMacOS()) {
        wtkBinPath = wtkPath;
        command.append("java -jar ");
        command.append(wtkPath);
        command.append("/player.jar ");
      } else {
        command.append(wtkBinPath);
        command.append(File.separator);
        command.append("emulator.exe -Xdescriptor:");
        if (Base.isWindows()) {
            command.append("\"");
        };          
      }
      command.append(sketch.folder.getPath());
      command.append(File.separator);
      command.append("midlet");
      command.append(File.separator);
      command.append(sketch.name);
      command.append(".jad");
      if (Base.isWindows()) {
          command.append("\"");
      }
      
      process = Runtime.getRuntime().exec(command.toString(), null, new File(wtkBinPath));
      processInput = new SystemOutSiphon(process.getInputStream());
      processError = new MessageSiphon(process.getErrorStream(), this);
      processOutput = process.getOutputStream();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  
  public void stop() {    
  }
  
  public void close() {
  }  
  
  public void message(String s) {
    System.err.println(s);
  }  
}
