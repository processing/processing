package processing.app;

import java.io.*;
import java.util.*;

import processing.app.contrib.*;
import processing.core.*;
import processing.data.StringDict;
import processing.data.StringList;


public class Library extends LocalContribution {
  static final String[] platformNames = PConstants.platformNames;

  //protected File folder;          // /path/to/shortname
  protected File libraryFolder;   // shortname/library
  protected File examplesFolder;  // shortname/examples
  protected File referenceFile;   // shortname/reference/index.html

  /**
   * Subfolder for grouping libraries in a menu. Basic subfolder support
   * is provided so that some organization can be done in the import menu.
   * (This is the replacement for the "library compilation" type.)
   */
  protected String group;

  /** Packages provided by this library. */
  StringList packageList;

  /** Per-platform exports for this library. */
  HashMap<String,String[]> exportList;

  /** Applet exports (cross-platform by definition). */
  String[] appletExportList;

  /** Android exports (single platform for now, may not exist). */
  String[] androidExportList;

  /** True if there are separate 32/64 bit for the specified platform index. */
  boolean[] multipleArch = new boolean[platformNames.length];

  /**
   * For runtime, the native library path for this platform. e.g. on Windows 64,
   * this might be the windows64 subfolder with the library.
   */
  String nativeLibraryPath;

  static public final String propertiesFileName = "library.properties";

  /**
   * Filter to pull out just files and none of the platform-specific
   * directories, and to skip export.txt. As of 2.0a2, other directories are
   * included, because we need things like the 'plugins' subfolder w/ video.
   */
  static FilenameFilter standardFilter = new FilenameFilter() {
    public boolean accept(File dir, String name) {
      // skip .DS_Store files, .svn folders, etc
      if (name.charAt(0) == '.') return false;
      if (name.equals("CVS")) return false;
      if (name.equals("export.txt")) return false;
      File file = new File(dir, name);
//      return (!file.isDirectory());
      if (file.isDirectory()) {
        if (name.equals("macosx")) return false;
        if (name.equals("macosx32")) return false;
        if (name.equals("macosx64")) return false;
        if (name.equals("windows")) return false;
        if (name.equals("windows32")) return false;
        if (name.equals("windows64")) return false;
        if (name.equals("linux")) return false;
        if (name.equals("linux32")) return false;
        if (name.equals("linux64")) return false;
        if (name.equals("linux-armv6hf")) return false;
        if (name.equals("android")) return false;
      }
      return true;
    }
  };

  static FilenameFilter jarFilter = new FilenameFilter() {
    public boolean accept(File dir, String name) {
      if (name.charAt(0) == '.') return false;  // skip ._blah.jar crap on OS X
      if (new File(dir, name).isDirectory()) return false;
      String lc = name.toLowerCase();
      return lc.endsWith(".jar") || lc.endsWith(".zip");
    }
  };


  static public Library load(File folder) {
    try {
      return new Library(folder);
//    } catch (IgnorableException ig) {
//      Base.log(ig.getMessage());
    } catch (Error err) {
      // Handles UnsupportedClassVersionError and others
      err.printStackTrace();
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    return null;
  }


  public Library(File folder) {
    this(folder, null);
  }


  private Library(File folder, String groupName) {
    super(folder);
    this.group = groupName;

    libraryFolder = new File(folder, "library");
    examplesFolder = new File(folder, "examples");
    referenceFile = new File(folder, "reference/index.html");

    File exportSettings = new File(libraryFolder, "export.txt");
    StringDict exportTable = exportSettings.exists() ?
      Util.readSettings(exportSettings) : new StringDict();

    exportList = new HashMap<String, String[]>();

    // get the list of files just in the library root
    String[] baseList = libraryFolder.list(standardFilter);
//    System.out.println("Loading " + name + "...");
//    PApplet.println(baseList);

    String appletExportStr = exportTable.get("applet");
    if (appletExportStr != null) {
      appletExportList = PApplet.splitTokens(appletExportStr, ", ");
    } else {
      appletExportList = baseList;
    }

    String androidExportStr = exportTable.get("android");
    if (androidExportStr != null) {
      androidExportList = PApplet.splitTokens(androidExportStr, ", ");
    } else {
      androidExportList = baseList;
    }

    // for the host platform, need to figure out what's available
    File nativeLibraryFolder = libraryFolder;
    String hostPlatform = Base.getPlatformName();
//    System.out.println("1 native lib folder now " + nativeLibraryFolder);
    // see if there's a 'windows', 'macosx', or 'linux' folder
    File hostLibrary = new File(libraryFolder, hostPlatform);
    if (hostLibrary.exists()) {
      nativeLibraryFolder = hostLibrary;
    }
//    System.out.println("2 native lib folder now " + nativeLibraryFolder);
    // check for bit-specific version, e.g. on windows, check if there
    // is a window32 or windows64 folder (on windows)
    hostLibrary = new File(libraryFolder, hostPlatform + Base.getNativeBits());
    if (hostLibrary.exists()) {
      nativeLibraryFolder = hostLibrary;
    }
//    System.out.println("3 native lib folder now " + nativeLibraryFolder);
    // save that folder for later use
    nativeLibraryPath = nativeLibraryFolder.getAbsolutePath();

    // for each individual platform that this library supports, figure out what's around
    for (int i = 1; i < platformNames.length; i++) {
      String platformName = platformNames[i];
      String platformName32 = platformName + "32";
      String platformName64 = platformName + "64";

      // First check for things like 'application.macosx=' or 'application.windows32' in the export.txt file.
      // These will override anything in the platform-specific subfolders.
      String platformAll = exportTable.get("application." + platformName);
      String[] platformList = platformAll == null ? null : PApplet.splitTokens(platformAll, ", ");
      String platform32 = exportTable.get("application." + platformName + "32");
      String[] platformList32 = platform32 == null ? null : PApplet.splitTokens(platform32, ", ");
      String platform64 = exportTable.get("application." + platformName + "64");
      String[] platformList64 = platform64 == null ? null : PApplet.splitTokens(platform64, ", ");

      // If nothing specified in the export.txt entries, look for the platform-specific folders.
      if (platformAll == null) {
        platformList = listPlatformEntries(libraryFolder, platformName, baseList);
      }
      if (platform32 == null) {
        platformList32 = listPlatformEntries(libraryFolder, platformName32, baseList);
      }
      if (platform64 == null) {
        platformList64 = listPlatformEntries(libraryFolder, platformName64, baseList);
      }

      if (platformList32 != null || platformList64 != null) {
        multipleArch[i] = true;
      }

      // if there aren't any relevant imports specified or in their own folders,
      // then use the baseList (root of the library folder) as the default.
      if (platformList == null && platformList32 == null && platformList64 == null) {
        exportList.put(platformName, baseList);

      } else {
        // once we've figured out which side our bread is buttered on, save it.
        // (also concatenate the list of files in the root folder as well
        if (platformList != null) {
          exportList.put(platformName, platformList);
        }
        if (platformList32 != null) {
          exportList.put(platformName32, platformList32);
        }
        if (platformList64 != null) {
          exportList.put(platformName64, platformList64);
        }
      }
    }
//    for (String p : exportList.keySet()) {
//      System.out.println(p + " -> ");
//      PApplet.println(exportList.get(p));
//    }

    // get the path for all .jar files in this code folder
    packageList = Util.packageListFromClassPath(getClassPath());
  }


  /**
   * List who's inside a windows64, macosx, linux32, etc folder.
   */
  static String[] listPlatformEntries(File libraryFolder, String folderName, String[] baseList) {
    File folder = new File(libraryFolder, folderName);
    if (folder.exists()) {
      String[] entries = folder.list(standardFilter);
      if (entries != null) {
        String[] outgoing = new String[entries.length + baseList.length];
        for (int i = 0; i < entries.length; i++) {
          outgoing[i] = folderName + "/" + entries[i];
        }
        // Copy the base libraries in there as well
        System.arraycopy(baseList, 0, outgoing, entries.length, baseList.length);
        return outgoing;
      }
    }
    return null;
  }


  static protected HashMap<String, Object> packageWarningMap = new HashMap<String, Object>();

  /**
   * Add the packages provided by this library to the master list that maps
   * imports to specific libraries.
   * @param importToLibraryTable mapping from package names to Library objects
   */
//  public void addPackageList(HashMap<String,Library> importToLibraryTable) {
  public void addPackageList(HashMap<String,ArrayList<Library>> importToLibraryTable) {
//    PApplet.println(packages);
    for (String pkg : packageList) {
//          pw.println(pkg + "\t" + libraryFolder.getAbsolutePath());
//      PApplet.println(pkg + "\t" + getName());
//      Library library = importToLibraryTable.get(pkg);
      ArrayList<Library> libraries = importToLibraryTable.get(pkg);
      if (libraries == null) {
        libraries = new ArrayList<Library>();
        importToLibraryTable.put(pkg, libraries);
      } else {
        if (Base.DEBUG) {
          System.err.println("The library found in");
          System.err.println(getPath());
          System.err.println("conflicts with");
          for (Library library : libraries) {
            System.err.println(library.getPath());
          }
          System.err.println("which already define(s) the package " + pkg);
          System.err.println("If you have a line in your sketch that reads");
          System.err.println("import " + pkg + ".*;");
          System.err.println("Then you'll need to first remove one of those libraries.");
          System.err.println();
        }
      }
      libraries.add(this);
    }
  }


  public boolean hasExamples() {
    return examplesFolder.exists();
  }


  public File getExamplesFolder() {
    return examplesFolder;
  }


  public String getGroup() {
    return group;
  }


  public String getPath() {
    return folder.getAbsolutePath();
  }


  public String getLibraryPath() {
    return libraryFolder.getAbsolutePath();
  }


  public String getJarPath() {
    //return new File(folder, "library/" + name + ".jar").getAbsolutePath();
    return new File(libraryFolder, folder.getName() + ".jar").getAbsolutePath();
  }


  // this prepends a colon so that it can be appended to other paths safely
  public String getClassPath() {
    StringBuilder cp = new StringBuilder();

//    PApplet.println(libraryFolder.getAbsolutePath());
//    PApplet.println(libraryFolder.list());
    String[] jarHeads = libraryFolder.list(jarFilter);
    for (String jar : jarHeads) {
      cp.append(File.pathSeparatorChar);
      cp.append(new File(libraryFolder, jar).getAbsolutePath());
    }
    jarHeads = new File(nativeLibraryPath).list(jarFilter);
    for (String jar : jarHeads) {
      cp.append(File.pathSeparatorChar);
      cp.append(new File(nativeLibraryPath, jar).getAbsolutePath());
    }
    //cp.setLength(cp.length() - 1);  // remove the last separator
    return cp.toString();
  }


  public String getNativePath() {
//    PApplet.println("native lib folder " + nativeLibraryPath);
    return nativeLibraryPath;
  }


//  public String[] getAppletExports() {
//    return appletExportList;
//  }


  protected File[] wrapFiles(String[] list) {
    File[] outgoing = new File[list.length];
    for (int i = 0; i < list.length; i++) {
      outgoing[i] = new File(libraryFolder, list[i]);
    }
    return outgoing;
  }


  /**
   * Applet exports don't go by platform, since by their nature applets are
   * meant to be cross-platform. Technically, you could have a situation where
   * you want to export applet code for different platforms, but it's too
   * obscure a case that we're not interested in supporting it.
   */
  public File[] getAppletExports() {
    return wrapFiles(appletExportList);
  }


  public File[] getApplicationExports(int platform, int bits) {
    String[] list = getApplicationExportList(platform, bits);
    return wrapFiles(list);
  }


  /**
   * Returns the necessary exports for the specified platform.
   * If no 32 or 64-bit version of the exports exists, it returns the version
   * that doesn't specify bit depth.
   */
  public String[] getApplicationExportList(int platform, int bits) {
    String platformName = PConstants.platformNames[platform];
    if (bits == 32) {
      String[] pieces = exportList.get(platformName + "32");
      if (pieces != null) return pieces;
    } else if (bits == 64) {
      String[] pieces = exportList.get(platformName + "64");
      if (pieces != null) return pieces;
    }
    return exportList.get(platformName);
  }


  public File[] getAndroidExports() {
    return wrapFiles(androidExportList);
  }


//  public boolean hasMultiplePlatforms() {
//    return false;
//  }


  public boolean hasMultipleArch(int platform) {
    return multipleArch[platform];
  }


  public boolean supportsArch(int platform, int bits) {
    // If this is a universal library, or has no natives, then we're good.
    if (multipleArch[platform] == false) {
      return true;
    }
    return getApplicationExportList(platform, bits) != null;
  }


//  static boolean hasMultipleArch(String platformName, ArrayList<LibraryFolder> libraries) {
//    int platform = Base.getPlatformIndex(platformName);
  static public boolean hasMultipleArch(int platform, ArrayList<Library> libraries) {
    for (Library library : libraries) {
      if (library.hasMultipleArch(platform)) {
        return true;
      }
    }
    return false;
  }


  // for sorting
//  public int compareTo(Object o) {
//    return prettyName.compareTo(((LibraryFolder) o).prettyName);
//  }}


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  static protected FilenameFilter junkFolderFilter = new FilenameFilter() {
    public boolean accept(File dir, String name) {
      // skip .DS_Store files, .svn folders, etc
      if (name.charAt(0) == '.') return false;
      if (name.equals("CVS")) return false;
      return (new File(dir, name).isDirectory());
    }
  };


  static public List<File> discover(File folder) {
    List<File> libraries = new ArrayList<File>();
//    discover(folder, libraries);
//    return libraries;
//  }
//
//
//  static void discover(File folder, List<File> libraries) {
    String[] folderNames = folder.list(junkFolderFilter);

    // if a bad folder or something like that, this might come back null
    if (folderNames != null) {
      // alphabetize list, since it's not always alpha order
      // replaced hella slow bubble sort with this feller for 0093
      Arrays.sort(folderNames, String.CASE_INSENSITIVE_ORDER);

      for (String potentialName : folderNames) {
        File baseFolder = new File(folder, potentialName);
        File libraryFolder = new File(baseFolder, "library");
        File libraryJar = new File(libraryFolder, potentialName + ".jar");
        // If a .jar file of the same prefix as the folder exists
        // inside the 'library' subfolder of the sketch
        if (libraryJar.exists()) {
          String sanityCheck = Sketch.sanitizeName(potentialName);
          if (sanityCheck.equals(potentialName)) {
            libraries.add(baseFolder);

          } else {
            String mess = "The library \""
                + potentialName
                + "\" cannot be used.\n"
                + "Library names must contain only basic letters and numbers.\n"
                + "(ASCII only and no spaces, and it cannot start with a number)";
            Base.showMessage("Ignoring bad library name", mess);
            continue;
          }
        }
      }
    }
    return libraries;
  }


  static public List<Library> list(File folder) {
    List<Library> libraries = new ArrayList<Library>();
//    list(folder, libraries);
//    return libraries;
//  }
//
//
//  static void list(File folder, List<Library> libraries) {
    List<File> librariesFolders = new ArrayList<File>();
    librariesFolders.addAll(discover(folder));

    for (File baseFolder : librariesFolders) {
      libraries.add(new Library(baseFolder));
    }

    // Support libraries inside of one level of subfolders? I believe this was
    // the compromise for supporting library groups, but probably a bad idea
    // because it's not compatible with the Manager.
    String[] folderNames = folder.list(junkFolderFilter);
    if (folderNames != null) {
      for (String subfolderName : folderNames) {
        File subfolder = new File(folder, subfolderName);

        if (!librariesFolders.contains(subfolder)) {
//          ArrayList<File> discoveredLibFolders = new ArrayList<File>();
//          discover(subfolder, discoveredLibFolders);
          List<File> discoveredLibFolders = discover(subfolder);

          for (File discoveredFolder : discoveredLibFolders) {
            libraries.add(new Library(discoveredFolder, subfolderName));
          }
        }
      }
    }
    return libraries;
  }


  public ContributionType getType() {
    return ContributionType.LIBRARY;
  }


  /**
   * Returns the object stored in the referenceFile field, which contains an
   * instance of the file object representing the index file of the reference
   *
   * @return referenceFile
   */
  public File getReferenceIndexFile() {
    return referenceFile;
  }


  /**
   * Tests whether the reference's index file indicated by referenceFile exists.
   *
   * @return true if and only if the file denoted by referenceFile exists; false
   *         otherwise.
   */
  public boolean hasReference() {
    return referenceFile.exists();
  }
}
