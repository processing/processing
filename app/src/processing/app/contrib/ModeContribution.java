/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2013-15 The Processing Foundation
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
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import processing.app.Base;
import processing.app.Messages;
import processing.app.Mode;
import processing.app.Util;


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
      Messages.log(ig.getMessage());

    } catch (Throwable err) {
      System.out.println("err is " + err);
      // Throwable to catch Exceptions or UnsupportedClassVersionError et al
      if (searchName == null) {
        //err.printStackTrace(System.out);
        // for 3.0b1, pass this through to the Contribution Manager so that
        // we can provide better error messages
        throw new RuntimeException(err);

      } else {
        // For the built-in modes, don't print the exception, just log it
        // for debugging. This should be impossible for most users to reach,
        // but it helps us load experimental mode when it's available.
        Messages.loge("ModeContribution.load() failed for " + searchName, err);
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
  public ModeContribution(Base base, File folder,
                           String className) throws Exception {
    super(folder);
    className = initLoader(base, className);
    if (className != null) {
      Class<?> modeClass = loader.loadClass(className);
      Messages.log("Got mode class " + modeClass);
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


  public String initLoader(Base base, String className) throws Exception {
    File modeDirectory = new File(folder, getTypeName());
    if (modeDirectory.exists()) {
      Messages.log("checking mode folder regarding class name " + className);
      // If no class name specified, search the main <modename>.jar for the
      // full name package and mode name.
      if (className == null) {
        String shortName = folder.getName();
        File mainJar = new File(modeDirectory, shortName + ".jar");
        if (mainJar.exists()) {
          className = findClassInZipFile(shortName, mainJar);
        } else {
          throw new IgnorableException(mainJar.getAbsolutePath() + " does not exist.");
        }

        if (className == null) {
          throw new IgnorableException("Could not find " + shortName +
                                       " class inside " + mainJar.getAbsolutePath());
        }
      }

      ArrayList<URL> extraUrls = new ArrayList<>();
      if (imports != null && imports.size() > 0) {
        // if the mode has any dependencies (defined as imports in mode.properties),
        // add the dependencies to the classloader

        HashMap<String, Mode> installedModes = new HashMap<>();
        for(Mode m: base.getModeList()){
          // Base.log("Mode contrib: " + m.getClass().getName() + " : "+ m.getFolder());
          installedModes.put(m.getClass().getName(), m);
        }

        for (String modeImport: imports) {
          if (installedModes.containsKey(modeImport)) {
            Messages.log("Found mode dependency " + modeImport);
            File modeFolder = installedModes.get(modeImport).getFolder();
            File[] archives = Util.listJarFiles(new File(modeFolder, "mode"));
            if (archives != null && archives.length > 0) {
              for (int i = 0; i < archives.length; i++) {
                // Base.log("Adding jar dependency: " + archives[i].getAbsolutePath());
                extraUrls.add(archives[i].toURI().toURL());
              }
            }
          } else {
            throw new IgnorableException("Can't load " + className +
                                         " because the import " + modeImport +
                                         " could not be found. ");
          }
        }
      }

      // Add .jar and .zip files from the "mode" folder into the classpath
      File[] archives = Util.listJarFiles(modeDirectory);
      if (archives != null && archives.length > 0) {
        int arrLen = archives.length + extraUrls.size();
        URL[] urlList = new URL[arrLen];

        int j = 0;
        for (; j < extraUrls.size(); j++) {
          //Base.log("Found archive " + archives[j] + " for " + getName());
          urlList[j] = extraUrls.get(j);
        }

        for (int k = 0; k < archives.length; k++,j++) {
          Messages.log("Found archive " + archives[k] + " for " + getName());
          urlList[j] = archives[k].toURI().toURL();
        }

        loader = new URLClassLoader(urlList);
        Messages.log("loading above JARs with loader " + loader);
//        System.out.println("listing classes for loader " + loader);
//        listClasses(loader);
      }
    }

    // If no archives were found, just use the regular ClassLoader
    if (loader == null) {
      loader = Thread.currentThread().getContextClassLoader();
    }
    return className;
  }
}