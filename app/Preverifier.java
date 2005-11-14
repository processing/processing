package processing.app;

import java.io.*;

public class Preverifier implements MessageConsumer {
  
  public Preverifier() {    
  }
  
  public boolean preverify(File source, File output) {
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
    if (Base.isMacOS()) {
        command.append(wtkPath);        
        command.append("/osx/preverify/preverify -classpath ");
        command.append(wtkPath);
        command.append("/cldc.jar:");
        command.append(wtkPath);
        command.append("/midp.jar:");
        command.append("/System/Library/Frameworks/JavaVM.framework/Versions/CurrentJDK/Classes/classes.jar:lib");
    } else {
        command.append(wtkBinPath);
        command.append("preverify.exe -target CLDC");
        command.append(cldc.charAt(0));
        command.append(".");
        command.append(cldc.charAt(1));
        command.append(" -classpath ");
        command.append(wtkLibPath);
        command.append("cldcapi");
        command.append(cldc);
        command.append(".jar");
        command.append(File.pathSeparator);
        command.append(wtkLibPath);
        command.append("midpapi");
        command.append(midp);
        command.append(".jar");
        command.append(File.pathSeparator);
        command.append("lib");
    }
    command.append(File.separator);
    command.append("mobile.jar");
    command.append(File.pathSeparator);
    command.append(Sketchbook.librariesClassPath);
    if (Base.isWindows()) {
        command.append(" -d \"");
        command.append(output.getPath());
        command.append("\" \"");
        command.append(source.getPath());
        command.append("\"");
    } else {
        command.append(" -d ");
        command.append(output.getPath());
        command.append(" ");
        command.append(source.getPath());
    }

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
