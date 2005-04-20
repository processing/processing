package processing.app;

import java.io.*;

public class Preverifier implements MessageConsumer {
  
  public Preverifier() {    
  }
  
  public boolean preverify(File source, File output) {
    String wtkPath = Preferences.get("wtk.path");
    String wtkBinPath = wtkPath + File.separator + "bin" + File.separator;
    String wtkLibPath = wtkPath + File.separator + "lib" + File.separator;
    
    StringBuffer command = new StringBuffer();
    command.append(wtkBinPath);
    command.append("preverify.exe -target CLDC1.0 -classpath ");
    command.append(wtkLibPath);
    command.append("cldcapi10.jar;");
    command.append(wtkLibPath);
    command.append("midpapi10.jar;lib");
    command.append(File.separator);
    command.append("mobile.jar");
    command.append(" -d \"");
    command.append(output.getPath());
    command.append("\" \"");
    command.append(source.getPath());
    command.append("\"");
    //System.out.println(command.toString());
    try {
      Process p = Runtime.getRuntime().exec(command.toString());
      boolean running = true;
      int result = -1;
      while (running) {
        try {
          result = p.waitFor();
          new MessageSiphon(p.getInputStream(), this);
          new MessageSiphon(p.getErrorStream(), this);
          
          running = false;
        } catch (InterruptedException ie) {
          ie.printStackTrace ();
        }
      }
      //System.out.println("Preverify complete!");
      return (result == 0);
    } catch (Exception e) {
      e.printStackTrace ();
    }
    
    return false;
  }
  
  public void message(String s) {
    System.err.println(s);
  }  
}
