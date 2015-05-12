/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2013 The Processing Foundation
  Copyright (c) 2011-12 Ben Fry and Casey Reas

  This program is free software; you can redistribute it and/or modify
  it under the terms of the GNU General Public License version 2
  as published by the Free Software Foundation.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License along
  with this program; if not, write to the Free Software Foundation, Inc.
  59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/
package processing.app.contrib;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.URLClassLoader;
import java.util.*;

import processing.app.Base;
import processing.app.Mode;


public class ModeContribution extends LocalContribution {
  private Mode mode;


  static public ModeContribution load(Base base, File folder) {
    return load(base, folder, null);
  }


  static public ModeContribution load(Base base, File folder,
                                      String searchName) {
    try {
      return new ModeContribution(base, folder, searchName);

    } catch (IgnorableException ig) {
      Base.log(ig.getMessage());

    } catch (Throwable err) {
      // Throwable to catch Exceptions or UnsupportedClassVersionError et al
      if (searchName == null) {
        err.printStackTrace();
      } else {
        // For the built-in modes, don't print the exception, just log it
        // for debugging. This should be impossible for most users to reach,
        // but it helps us load experimental mode when it's available.
        Base.loge("ModeContribution.load() failed for " + searchName, err);
      }
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
      Base.log("Got mode class " + modeClass);
      Constructor con = modeClass.getConstructor(Base.class, File.class);
      mode = (Mode) con.newInstance(base, folder);
      mode.setClassLoader(loader);
      if (base != null) {
        mode.setupGUI();
      }
    }
  }


  /**
   * Method to close the ClassLoader so that the archives are no longer "locked"
   * and a mode can be removed without restart.
   */
  public void clearClassLoader(Base base) {
    List<ModeContribution> contribModes = base.getModeContribs();
    int botherToRemove = contribModes.indexOf(this);
    if (botherToRemove != -1) { // The poor thing isn't even loaded, and we're trying to remove it...
      contribModes.remove(botherToRemove);

      try {
        ((URLClassLoader) loader).close();
        // The typecast should be safe, since the only case when loader is not of
        // type URLClassLoader is when no archives were found in the first
        // place...
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }


  static public void loadMissing(Base base) {
    File modesFolder = Base.getSketchbookModesFolder();
    List<ModeContribution> contribModes = base.getModeContribs();

    Map<File, ModeContribution> existing = new HashMap<File, ModeContribution>();
    for (ModeContribution contrib : contribModes) {
      existing.put(contrib.getFolder(), contrib);
    }
    File[] potential = ContributionType.MODE.listCandidates(modesFolder);
    // If modesFolder does not exist or is inaccessible (folks might like to
    // mess with folders then report it as a bug) 'potential' will be null.
    if (potential != null) {
      for (File folder : potential) {
        if (!existing.containsKey(folder)) {
          try {
            contribModes.add(new ModeContribution(base, folder, null));
          } catch (IgnorableException ig) {
            Base.log(ig.getMessage());
          } catch (Throwable e) {
            e.printStackTrace();
          }
        }
      }
    }

    // This allows you to build and test your Mode code from Eclipse.
    // -Dusemode=com.foo.FrobMode:/path/to/FrobMode
    final String useMode = System.getProperty("usemode");
    if (useMode != null) {
      final String[] modeInfo = useMode.split(":", 2);
      final String modeClass = modeInfo[0];
      final String modeResourcePath = modeInfo[1];
      System.out.println("Attempting to load " + modeClass + " with resources at " + modeResourcePath);
      contribModes.add(ModeContribution.load(base, new File(modeResourcePath), modeClass));
    }
  }


  public Mode getMode() {
    return mode;
  }


  public ContributionType getType() {
    return ContributionType.MODE;
  }


  public boolean equals(Object o) {
    if (o == null || !(o instanceof ModeContribution)) {
      return false;
    }
    ModeContribution other = (ModeContribution) o;
    return loader.equals(other.loader) && mode.equals(other.getMode());
  }


//  static protected List<File> discover(File folder) {
//    File[] folders = listCandidates(folder, "mode");
//    if (folders == null) {
//      return new ArrayList<File>();
//    } else {
//      return Arrays.asList(folders);
//    }
//  }
}
