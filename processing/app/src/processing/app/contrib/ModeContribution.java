/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2004-11 Ben Fry and Casey Reas
  Copyright (c) 2001-04 Massachusetts Institute of Technology

  This program is free software; you can redistribute it and/or modify
  it under the terms of the GNU General Public License version 2
  as published by the Free Software Foundation.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software Foundation,
  Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package processing.app.contrib;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;

import processing.app.Base;
import processing.app.Mode;

public class ModeContribution extends InstalledContribution {

  private URLClassLoader loader;
  
  private String className;

  Base base;
  
  Mode mode;
  
  static String propertiesFileName = "mode.properties";
  
  static public Mode getCoreMode(Base base, String classname, File folder) {
    try {
      Class c = Class.forName(classname);
      Constructor cc;
      cc = c.getConstructor(Base.class, File.class);
      return (Mode) cc.newInstance(base, folder);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }
  
  static public ModeContribution getContributedMode(Base base, File folder) {
    ModeContribution mode = new ModeContribution(base, folder);
    if (mode.isValid())
      return mode;
    
    return null;
  }
  
  private ModeContribution(Base base, File folder) {
    super(folder, ModeContribution.propertiesFileName);
    
    this.base = base;
    
    File modeDirectory = new File(folder, "mode");
    
    // add .jar and .zip files to classpath
    File[] archives = modeDirectory.listFiles(new FilenameFilter() {
      public boolean accept(File dir, String name) {
        return (name.toLowerCase().endsWith(".jar") ||
                name.toLowerCase().endsWith(".zip"));
      }
    });
    
    if (archives == null || archives.length == 0)
      return;
    
    try {
      URL[] urlList = new URL[archives.length];
      for (int j = 0; j < urlList.length; j++) {
          urlList[j] = archives[j].toURI().toURL();
      }
      loader = new URLClassLoader(urlList);
  
      for (int j = 0; j < archives.length; j++) {
        className = ToolContribution.findClassInZipFile(folder.getName(),
                                                        archives[j]);
      }
    } catch (MalformedURLException e) {
      // Maybe log this
    }
  }
  
  private boolean isValid() {
    return className != null;
  }
  
  /**
   * Creates and instance of the Mode object. Warning: this makes it impossible
   * (on Windows) to move the files in the mode's classpath without restarting
   * the PDE.
   */
  public boolean instantiateModeClass() {
    try {
      Class<?> modeClass = Class.forName(className, true, loader);
      Constructor contr = modeClass.getConstructor(Base.class, File.class);
      mode = (Mode) contr.newInstance(base, folder);
      return true;
    } catch (Exception e) {
      e.printStackTrace();
    }
    
    return false;
  }
  
  public Mode getMode() {
    return mode;
  }

  public Type getType() {
    return Type.MODE;
  }
  
  public boolean equals(Object o) {
    if (o == null || o instanceof ModeContribution)
      return false;

    ModeContribution other = (ModeContribution) o;
    return loader.equals(other.loader) && className.equals(other.className);
  }
  
  static public ArrayList<ModeContribution> list(Base base, File folder) {
    ArrayList<ModeContribution> modes = new ArrayList<ModeContribution>();
    ArrayList<File> modeFolders = discover(folder);
    
    for (File potentialModeFolder : modeFolders) {
      ModeContribution contrib = getContributedMode(base, potentialModeFolder);
      if (contrib != null) {
        modes.add(contrib);
      }
    }
    return modes;
  }
  
  static protected ArrayList<File> discover(File folder) {
    ArrayList<File> modeFolders = new ArrayList<File>();
    discover(folder, modeFolders);
    return modeFolders;
  }
  
  static protected void discover(File folder, ArrayList<File> modeFolders) {
  
    File[] folders = folder.listFiles(new FileFilter() {
      public boolean accept(File potentialModeFolder) {
        if (!potentialModeFolder.isDirectory()) return false;
        return new File(potentialModeFolder, "mode").exists();
      }
    });

    if (folders == null || folders.length == 0) {
      return;
    }
    
    for (File potentialModeFolder : folders) {
      modeFolders.add(potentialModeFolder);
    }
  }

}
