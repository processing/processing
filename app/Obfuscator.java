package processing.app;

import java.io.*;

public class Obfuscator implements MessageConsumer {
  
  public Obfuscator() {    
  }
  
  public boolean obfuscate(File source, File output) {
    String wtkPath = Preferences.get("wtk.path");
    String wtkBinPath = wtkPath + File.separator + "bin" + File.separator;
    String wtkLibPath = wtkPath + File.separator + "lib" + File.separator;
    
    //// cldc version
    String cldc = Preferences.get("wtk.cldc");
    if (cldc == null) {
        //// default 1.0
        cldc = "10";
    }
    String midp = Preferences.get("wtk.midp");
    if (midp == null) { 
        //// default 1.0
        midp = "10";
    }
    
    StringBuffer command = new StringBuffer();
    command.append("java -jar lib");
    command.append(File.separator);
    command.append("proguard.jar -libraryjars ");
    command.append(wtkLibPath);
    command.append("cldcapi");
    command.append(cldc);
    command.append(".jar");
    command.append(File.pathSeparator);
    command.append(wtkLibPath);
    command.append("midpapi");
    command.append(midp);
    command.append(".jar");
    command.append(" -injars \"");
    command.append(source.getPath());
    command.append("\" -outjar \"");
    command.append(output.getPath());
    command.append("\" @lib");
    command.append(File.separator);
    command.append("proguard.pro");
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
