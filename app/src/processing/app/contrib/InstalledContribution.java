/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2004-12 Ben Fry and Casey Reas
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

import java.io.*;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.zip.*;

import processing.app.*;


public abstract class InstalledContribution implements Contribution {

  protected String name;          // "pdf" or "PDF Export"
  protected String id;            // 1
  protected String category;      // "Sound"
  protected String authorList;    // Ben Fry
  protected String url;           // http://processing.org
  protected String sentence;      // Write graphics to PDF files.
  protected String paragraph;     // <paragraph length description for site>
  protected int version;          // 102
  protected int latestVersion;    // 103
  protected String prettyVersion; // "1.0.2"

  protected File folder;

  protected HashMap<String, String> properties;

  protected ClassLoader loader;


  public InstalledContribution(File folder) {
    this.folder = folder;

    // required for contributed modes, but not for built-in core modes
    File propertiesFile = new File(folder, getTypeName() + ".properties");
    if (propertiesFile.exists()) {
      properties = Base.readSettings(propertiesFile);

      name = properties.get("name");
      id = properties.get("id");
      category = ContributionListing.getCategory(properties.get("category"));
      if (name == null) {
        name = folder.getName();
      }
      authorList = properties.get("authorList");
      url = properties.get("url");
      sentence = properties.get("sentence");
      paragraph = properties.get("paragraph");

      try {
        version = Integer.parseInt(properties.get("version"));
      } catch (NumberFormatException e) {
        e.printStackTrace();
      }
      prettyVersion = properties.get("prettyVersion");
      
    } else {
      Base.log("No properties file at " + propertiesFile.getAbsolutePath());
      // We'll need this to be set at a minimum.
      name = folder.getName();
    }
  }


  public String initLoader(String className) throws Exception {
    File modeDirectory = new File(folder, getTypeName());
    if (modeDirectory.exists()) {
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

      // Add .jar and .zip files from the "mode" folder into the classpath
      File[] archives = Base.listJarFiles(modeDirectory);
      if (archives != null && archives.length > 0) {
        URL[] urlList = new URL[archives.length];
        for (int j = 0; j < urlList.length; j++) {
          Base.log("found lib: " + archives[j] + " for " + getName());
          urlList[j] = archives[j].toURI().toURL();
        }
//        loader = new URLClassLoader(urlList, Thread.currentThread().getContextClassLoader());
        loader = new URLClassLoader(urlList);
        Base.log("loading above JARs with loader " + loader);
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


  // doesn't work with URLClassLoader, but works with the system CL
  static void listClasses(ClassLoader loader) {
//    loader = Thread.currentThread().getContextClassLoader();
    try {
      Field f = ClassLoader.class.getDeclaredField("classes");
      f.setAccessible(true);
      Vector<Class> classes =  (Vector<Class>) f.get(loader);
      for (Class c : classes) {
        System.out.println(c.getName());
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }


  /**
   * Return a list of directories that have the necessary subfolder for this
   * contribution type. For instance, a list of folders that have a 'mode'
   * subfolder if this is a ModeContribution.
   */
  static protected File[] listCandidates(File folder, final String typeName) {
    return folder.listFiles(new FileFilter() {
      public boolean accept(File potential) {
        return (potential.isDirectory() &&
                new File(potential, typeName).exists());
      }
    });
  }


  public File getFolder() {
    return folder;
  }


  public boolean isInstalled() {
    return folder != null;
  }


  public String getCategory() {
    return category;
  }


  public String getName() {
    return name;
  }


  public String getId() {
    return id;
  }


  public String getAuthorList() {
    return authorList;
  }


  public String getUrl() {
    return url;
  }


  public String getSentence() {
    return sentence;
  }


  public String getParagraph() {
    return paragraph;
  }


  public int getVersion() {
    return version;
  }


  public int getLatestVersion() {
    return latestVersion;
  }


  public String getPrettyVersion() {
    return prettyVersion;
  }


  public String getTypeName() {
    return getType().toString();
  }


  /*
  static protected String findClassInZipFileList(String base, File[] fileList) {
    for (File file : fileList) {
      String found = findClassInZipFile(base, file);
      if (found != null) {
        return found;
      }
    }
    return null;
  }
  */


  /**
   *
   * @param base name of the class, with or without the package
   * @param file
   * @return name of class (with full package name) or null if not found
   */
  static protected String findClassInZipFile(String base, File file) {
    // Class file to search for
    String classFileName = "/" + base + ".class";

    try {
      ZipFile zipFile = new ZipFile(file);
      Enumeration<?> entries = zipFile.entries();
      while (entries.hasMoreElements()) {
        ZipEntry entry = (ZipEntry) entries.nextElement();

        if (!entry.isDirectory()) {
          String name = entry.getName();
//          System.out.println("entry: " + name);

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


  class IgnorableException extends Exception {
    public IgnorableException(String msg) {
      super(msg);
    }
  }
}
