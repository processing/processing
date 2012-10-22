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
//import java.io.FileFilter;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import processing.app.Base;
import processing.app.Mode;


public class ModeContribution extends InstalledContribution {
//  static final String propertiesFileName = "mode.properties";

  /** Class name with package declaration. */
//  private String className;
  private Mode mode;
//  Base base;


//  static public Mode getCoreMode(Base base, String className, File folder) {
//    try {
//      Class<?> c = Thread.currentThread().getContextClassLoader().loadClass(className);
////      Class c = Class.forName(classname);
//      Constructor cc = c.getConstructor(Base.class, File.class);
//      return (Mode) cc.newInstance(base, folder);
//    } catch (Exception e) {
//      e.printStackTrace();
//    }
//    return null;
//  }


//  static public ModeContribution getContributedMode(Base base, File folder) {
//    ModeContribution mode = new ModeContribution(base, folder);
//    return mode.isValid() ? mode : null;
//  }

//  static public ModeContribution load(Base base, File folder) {
  static public ModeContribution load(Base base, File folder) {
    return load(base, folder, null);
  }


//  static public ModeContribution load(Base base, File folder, String searchName) {
  static public ModeContribution load(Base base, File folder, String searchName) {
    try {
      return new ModeContribution(base, folder, searchName);
    } catch (IgnorableException ig) {
      Base.log(ig.getMessage());
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }


  /**
   *
   * @param base the base object that this will be tied to
   * @param folder location inside the sketchbook modes folder or contrib
   * @param className name of class and full package, or null to use default
   * @throws Exception
   */
  private ModeContribution(Base base, File folder,
                           String className) throws Exception {
    super(folder);

    className = initLoader(className);
    if (className != null) {
      Class<?> modeClass = loader.loadClass(className);
      Constructor con = modeClass.getConstructor(Base.class, File.class);
      mode = (Mode) con.newInstance(base, folder);
      mode.setClassLoader(loader);
      mode.setupGUI();
    }

//    // Class name already found above, go ahead and instantiate
//    if (className != null) {
////      try {
//      System.out.println("instantiating " + className + " using loader " + loader);
////      Class<?> modeClass = Class.forName(className, true, loader);
//      modeClass = loader.loadClass(className);
////        return true;
////      } catch (Exception e) {
////        e.printStackTrace();
////      }
////      return false;
////    }
//
//    } else {  // className == null, might be a built-in fella
//      System.out.println("class name null while looking for " + searchName);
////      try {
//        // Probably a contributed mode, check to see if it's already available
//      if (loader.loadClass(searchName) != null) {
//        System.out.println("  found " + searchName + " after all");
//        className = searchName;
//      }
////      } catch (ClassNotFoundException e) {
////        e.printStackTrace();
////      }
//    }
//
//    if (modeClass != null) {
//      Constructor con = modeClass.getConstructor(Base.class, File.class);
//      mode = (Mode) con.newInstance(base, folder);
//      mode.setupGUI();
//    }
  }


//  private boolean isValid() {
//    return className != null;
//  }


//  /**
//   * Creates an instance of the Mode object. Warning: this makes it impossible
//   * (on Windows) to move the files in the mode's classpath without restarting
//   * the PDE.
//   */
//  public boolean instantiateModeClass(Base base) {
//    new Exception().printStackTrace(System.out);
//    try {
//      System.out.println("instantiating " + className + " using loader " + loader);
////      Class<?> modeClass = Class.forName(className, true, loader);
//      Class<?> modeClass = loader != null ?
//        loader.loadClass(className) :
//        Thread.currentThread().getContextClassLoader().loadClass(className);
//      Constructor contr = modeClass.getConstructor(Base.class, File.class);
//      mode = (Mode) contr.newInstance(base, folder);
//      mode.setupGUI();
//      return true;
//    } catch (Exception e) {
//      e.printStackTrace();
//    }
//
//    return false;
//  }


  public Mode getMode() {
    return mode;
  }


  public Type getType() {
    return Type.MODE;
  }


  public boolean equals(Object o) {
    if (o == null || !(o instanceof ModeContribution)) {
      return false;
    }
    ModeContribution other = (ModeContribution) o;
//    return loader.equals(other.loader) && className.equals(other.className);
    return loader.equals(other.loader) && mode.equals(other.getMode());
  }


  static public void loadMissing(Base base) {
    File modesFolder = Base.getSketchbookModesFolder();
    ArrayList<ModeContribution> contribModes = base.getModeContribs();

    HashMap<File, ModeContribution> existing = new HashMap<File, ModeContribution>();
    for (ModeContribution contrib : contribModes) {
      existing.put(contrib.getFolder(), contrib);
    }
    File[] potential = listCandidates(modesFolder, "mode");
    for (File folder : potential) {
      if (!existing.containsKey(folder)) {
          try {
            contribModes.add(new ModeContribution(base, folder, null));
          } catch (IgnorableException ig) {
            Base.log(ig.getMessage());
          } catch (Exception e) {
            e.printStackTrace();
          }
      }
    }
  }


//  static public ArrayList<ModeContribution> loadAll(Base base, File folder) {
//    ArrayList<ModeContribution> modes = new ArrayList<ModeContribution>();
//    ArrayList<File> modeFolders = discover(folder);
//
//    for (File potentialModeFolder : modeFolders) {
//      //ModeContribution contrib = getContributedMode(base, potentialModeFolder);
//      ModeContribution contrib = load(base, potentialModeFolder);
//      if (contrib != null) {
//        modes.add(contrib);
//      }
//    }
//    return modes;
//  }


//  static protected ArrayList<File> discover(File folder) {
//    ArrayList<File> modeFolders = new ArrayList<File>();
////    discover(folder, modeFolders);
////    return modeFolders;
////  }
////
////
////  static protected void discover(File folder, ArrayList<File> modeFolders) {
//    File[] folders = listCandidates(folder, "mode");
////    File[] folders = folder.listFiles(new FileFilter() {
////      public boolean accept(File potentialModeFolder) {
////        return (potentialModeFolder.isDirectory() &&
////                new File(potentialModeFolder, "mode").exists());
////      }
////    });
//
//    if (folders != null && folders.length > 0) {
//      for (File potentialModeFolder : folders) {
//        modeFolders.add(potentialModeFolder);
//      }
//    }
//    return modeFolders;
//  }


  static protected List<File> discover(File folder) {
    File[] folders = listCandidates(folder, "mode");
    if (folders == null) {
      return new ArrayList<File>();
    } else {
      return Arrays.asList(folders);
    }
  }
}
