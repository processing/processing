import java.awt.Point;
import java.io.*;

/**
 *
 * @author  Francis Li
 */
public class PdeEmulator extends PdeRuntime {
  
  /** Creates a new instance of PdeEmulator */
  public PdeEmulator(PdeSketch sketch, PdeEditor editor) {
    super(sketch, editor);
  }
  
  public void start(Point windowLocation) throws PdeException {
    try{
      String wtkBinPath = PdePreferences.get("wtk.path") + File.separator + "bin";
      
      StringBuffer command = new StringBuffer();
      command.append(wtkBinPath);
      command.append(File.separator);
      command.append("emulator.exe -Xdescriptor:\"");
      command.append(sketch.folder.getPath());
      command.append(File.separator);
      command.append("midlet");
      command.append(File.separator);
      command.append(sketch.name);
      command.append(".jad\"");
      
      process = Runtime.getRuntime().exec(command.toString(), null, new File(wtkBinPath));
      processInput = new SystemOutSiphon(process.getInputStream());
      processError = new PdeMessageSiphon(process.getErrorStream(), this);
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
