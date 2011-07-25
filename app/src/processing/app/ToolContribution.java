package processing.app;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.zip.*;

import processing.app.tools.Tool;

public class ToolContribution extends Contribution implements Tool {
  
  Tool tool;
  
  ToolInfo info;
  
  File folder;
  
  static public ToolContribution getTool(File folder) {
    try {
      ToolContribution tool = new ToolContribution(folder);
      if (tool.tool != null)
        return tool;
    } catch (Exception e) {
    }
    return null;
  }
  
  private ToolContribution(File folder) throws Exception {
    this.folder = folder;
    
    // XXX: This is repeated in LibraryCompilcation.java
    File propertiesFile = new File(folder, "properties.txt");
    
    info = new ToolInfo();
    info.tool = this;
    
    HashMap<String,String> propertiesTable = Base.readSettings(propertiesFile);
    readProperties(propertiesTable, info);
    if (info.name == null) {
      info.name = folder.getName();
    }
    
    File toolDirectory = new File(folder, "tool");
    // add dir to classpath for .classes
    //urlList.add(toolDirectory.toURL());

    // add .jar files to classpath
    File[] archives = toolDirectory.listFiles(new FilenameFilter() {
      public boolean accept(File dir, String name) {
        return (name.toLowerCase().endsWith(".jar") ||
                name.toLowerCase().endsWith(".zip"));
      }
    });

    URL[] urlList = new URL[archives.length];
    for (int j = 0; j < urlList.length; j++) {
      urlList[j] = archives[j].toURI().toURL();
    }
    URLClassLoader loader = new URLClassLoader(urlList);

    String className = null;
    for (int j = 0; j < archives.length; j++) {
      className = findClassInZipFile(folder.getName(), archives[j]);
      if (className != null) break;
    }

    /*
    // Alternatively, could use manifest files with special attributes:
    // http://java.sun.com/j2se/1.3/docs/guide/jar/jar.html
    // Example code for loading from a manifest file:
    // http://forums.sun.com/thread.jspa?messageID=3791501
    File infoFile = new File(toolDirectory, "tool.txt");
    if (!infoFile.exists()) continue;

    String[] info = PApplet.loadStrings(infoFile);
    //Main-Class: org.poo.shoe.AwesomerTool
    //String className = folders[i].getName();
    String className = null;
    for (int k = 0; k < info.length; k++) {
      if (info[k].startsWith(";")) continue;

      String[] pieces = PApplet.splitTokens(info[k], ": ");
      if (pieces.length == 2) {
        if (pieces[0].equals("Main-Class")) {
          className = pieces[1];
        }
      }
    }
    */
    // If no class name found, just move on.
    if (className == null) return;

    Class<?> toolClass = Class.forName(className, true, loader);
    tool = (Tool) toolClass.newInstance();
  }
  
  protected String findClassInZipFile(String base, File file) {
    // Class file to search for
    String classFileName = "/" + base + ".class";

    try {
      ZipFile zipFile = new ZipFile(file);
      Enumeration<?> entries = zipFile.entries();
      while (entries.hasMoreElements()) {
        ZipEntry entry = (ZipEntry) entries.nextElement();

        if (!entry.isDirectory()) {
          String name = entry.getName();
          //System.out.println("entry: " + name);

          if (name.endsWith(classFileName)) {
            //int slash = name.lastIndexOf('/');
            //String packageName = (slash == -1) ? "" : name.substring(0, slash);
            // Remove .class and convert slashes to periods.
            zipFile.close();
            return name.substring(0, name.length() - 6).replace('/', '.');
          }
        }
      }
      zipFile.close();
    } catch (IOException e) {
      //System.err.println("Ignoring " + filename + " (" + e.getMessage() + ")");
      e.printStackTrace();
    }
    return null;
  }
  
  static protected ArrayList<ToolContribution> list(File folder) {
    ArrayList<ToolContribution> tools = new ArrayList<ToolContribution>();
    list(folder, tools);
    return tools;
  }
  
  static protected void list(File folder, ArrayList<ToolContribution> tools) {
    
    File[] folders = folder.listFiles(new FileFilter() {
      public boolean accept(File folder) {
        if (folder.isDirectory()) {
          //System.out.println("checking " + folder);
          File subfolder = new File(folder, "tool");
          return subfolder.exists();
        }
        return false;
      }
    });

    if (folders == null || folders.length == 0) {
      return;
    }
    
    for (int i = 0; i < folders.length; i++) {
      try {
        final ToolContribution tool = getTool(folders[i]);
        if (tool != null) {
          tools.add(tool);
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }
  
  ContributionInfo getInfo() {
    return info;
  }

  File getFolder() {
    return folder;
  }
  
  public void init(Editor editor) {
    tool.init(editor);
  }

  public void run() {
    tool.run();
  }

  public String getMenuTitle() {
    return tool.getMenuTitle();
  }
  
  public static class ToolInfo extends ContributionInfo {
    
    ToolContribution tool;

    public ContributionType getType() {
      return ContributionType.TOOL;
    }

    public boolean isInstalled() {
      return tool != null;
    }

    public Contribution getContribution() {
      return tool;
    }

  }

}
