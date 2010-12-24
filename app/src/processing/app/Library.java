package processing.app;

import java.io.*;
import java.util.*;

import processing.core.*;


public class Library {
  static final String[] platformNames = PConstants.platformNames;
  
  protected File folder;
  protected File libraryFolder;   // name/library
  protected File examplesFolder;  // name/examples
  protected File referenceFile;   // name/reference/index.html

  protected String name;          // "pdf" or "PDF Export"
  protected String author;        // Ben Fry
  protected String authorURL;     // http://processing.org
  protected String sentence;      // Write graphics to PDF files.
  protected String paragraph;     // <paragraph length description for site>
  protected int version;          // 102
  protected String prettyVersion; // "1.0.2"

  /** Packages provided by this library. */
  String[] packageList;
  
  /** Per-platform exports for this library. */
  HashMap<String,String[]> exportList;
  
  /** Applet exports (cross-platform by definition). */
  String[] appletExportList;
  
  /** True if there are separate 32/64 bit for the specified platform index. */
  boolean[] multipleArch = new boolean[platformNames.length];

  /**
   * For runtime, the native library path for this platform. e.g. on Windows 64, 
   * this might be the windows64 subfolder with the library.
   */
  String nativeLibraryPath;
  
  /** How many bits this machine is */
  static int nativeBits;
  static {
    nativeBits = 32;  // perhaps start with 32
    String bits = System.getProperty("sun.arch.data.model");
    if (bits != null) {
      if (bits.equals("64")) {
        nativeBits = 64;
      }
    } else {
      // if some other strange vm, maybe try this instead
      if (System.getProperty("java.vm.name").contains("64")) {
        nativeBits = 64;
      }
    }
  }  

  /** Filter to pull out just files and no directories, and to skip export.txt */
  FilenameFilter standardFilter = new FilenameFilter() {
    public boolean accept(File dir, String name) {
      // skip .DS_Store files, .svn folders, etc
      if (name.charAt(0) == '.') return false;
      if (name.equals("CVS")) return false;
      if (name.equals("export.txt")) return false;
      File file = new File(dir, name); 
      return (!file.isDirectory());
    }
  };

  FilenameFilter jarFilter = new FilenameFilter() {
    public boolean accept(File dir, String name) {
      if (name.charAt(0) == '.') return false;  // skip ._blah.jar crap on OS X
      if (new File(dir, name).isDirectory()) return false;
      String lc = name.toLowerCase();
      return lc.endsWith(".jar") || lc.endsWith(".zip"); 
    }
  };


  public Library(File folder) {
    this.folder = folder;
    libraryFolder = new File(folder, "library");
    examplesFolder = new File(folder, "examples");
    referenceFile = new File(folder, "reference/index.html");

    File exportSettings = new File(libraryFolder, "export.txt");
    HashMap<String,String> exportTable = Base.readSettings(exportSettings);

    name = exportTable.get("name");
    if (name == null) {
      name = folder.getName();
    }

    exportList = new HashMap<String, String[]>();

    // get the list of files just in the library root
    String[] baseList = libraryFolder.list(standardFilter);
    System.out.println("Loading " + name + "...");
//    PApplet.println(baseList);

    String appletExportStr = exportTable.get("applet");
    if (appletExportStr != null) {
      appletExportList = PApplet.splitTokens(appletExportStr, ", ");
    } else {
      appletExportList = baseList;
    }

    // for the host platform, need to figure out what's available
    File nativeLibraryFolder = libraryFolder;
    String hostPlatform = Base.getPlatformName(); 
    // see if there's a 'windows', 'macosx', or 'linux' folder
    File hostLibrary = new File(libraryFolder, hostPlatform);
    if (hostLibrary.exists()) {
      nativeLibraryFolder = hostLibrary;
    }
    // check for bit-specific version, e.g. on windows, check if there  
    // is a window32 or windows64 folder (on windows)
    hostLibrary = new File(libraryFolder, hostPlatform + nativeBits);
    if (hostLibrary.exists()) {
      nativeLibraryFolder = hostLibrary;
    }
    // save that folder for later use
    nativeLibraryPath = nativeLibraryFolder.getAbsolutePath();

    // for each individual platform that this library supports, figure out what's around
    for (int i = 1; i < platformNames.length; i++) {
      String platformName = platformNames[i];
      String platformName32 = platformName + "32";
      String platformName64 = platformName + "64";

      String platformAll = exportTable.get("application." + platformName);
      String[] platformList = platformAll == null ? null : PApplet.splitTokens(platformAll, ", ");

      String platform32 = exportTable.get("application." + platformName + "32");
      String[] platformList32 = platform32 == null ? null : PApplet.splitTokens(platform32, ", ");

      String platform64 = exportTable.get("application." + platformName + "64");
      String[] platformList64 = platform64 == null ? null : PApplet.splitTokens(platform64, ", ");

      if (platformAll == null) {
        File folderAll = new File(libraryFolder, platformName);
        if (folderAll.exists()) {
          platformList = PApplet.concat(baseList, folderAll.list(standardFilter));
        }
      }
      if (platform32 == null) {
        File folder32 = new File(libraryFolder, platformName32);
        if (folder32.exists()) {
          platformList32 = PApplet.concat(baseList, folder32.list(standardFilter));
        }
      }
      if (platform64 == null) {
        File folder64 = new File(libraryFolder, platformName64);
        if (folder64.exists()) {
          platformList64 = PApplet.concat(baseList, folder64.list(standardFilter));
        }
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
    packageList = Base.packageListFromClassPath(getClassPath());
  }
  

  /** 
   * Add the packages provided by this library to the master list that maps
   * imports to specific libraries.
   * @param importToLibraryTable mapping from package names to Library objects 
   */
  public void addPackageList(HashMap<String,Library> importToLibraryTable) {
//    PApplet.println(packages);
    for (String pkg : packageList) {
      //    pw.println(pkg + "\t" + libraryFolder.getAbsolutePath());
      Library library = importToLibraryTable.get(pkg);
      if (library != null) {
        System.err.println("The library found in " + getPath());
        System.err.println("conflicts with " + library.getPath());
        System.err.println("which already defines the package " + pkg);
        System.err.println();
      } else {
//        PApplet.println("adding pkg " + pkg + " for " + name);
        importToLibraryTable.put(pkg, this);
      }
    }
  }
  
  
  public String getName() {
    return name;
  }
  
  
  public boolean hasExamples() {
    return examplesFolder.exists();
  }


  public File getExamplesFolder() {
    return examplesFolder;
  }
  
  
  public String getPath() {
    return folder.getAbsolutePath();
  }

  
  public String getLibraryPath() {
    return libraryFolder.getAbsolutePath();
  }


  public String getJarPath() {
    return new File(folder, "library/" + name + ".jar").getAbsolutePath(); 
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
    String platformName = PApplet.platformNames[platform];
    if (bits == 32) {
      String[] pieces = exportList.get(platformName + "32");
      if (pieces != null) return pieces;
    } else if (bits == 64) {
      String[] pieces = exportList.get(platformName + "64");
      if (pieces != null) return pieces;
    }
    return exportList.get(platformName);
  }


//  public boolean hasMultiplePlatforms() {
//    return false;
//  }


  public boolean hasMultipleArch(int platform) {
    return multipleArch[platform];
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


  static protected ArrayList<Library> list(File folder) throws IOException {
    ArrayList<Library> libraries = new ArrayList<Library>();
    list(folder, libraries);
    return libraries;
  }


  static protected void list(File folder, ArrayList<Library> libraries) throws IOException {
    if (folder.isDirectory()) {
      String[] list = folder.list(new FilenameFilter() {
        public boolean accept(File dir, String name) {
          // skip .DS_Store files, .svn folders, etc
          if (name.charAt(0) == '.') return false;
          if (name.equals("CVS")) return false;
          return (new File(dir, name).isDirectory());
        }
      });
      // if a bad folder or something like that, this might come back null
      if (list != null) {
        // alphabetize list, since it's not always alpha order
        // replaced hella slow bubble sort with this feller for 0093
        Arrays.sort(list, String.CASE_INSENSITIVE_ORDER);

        for (String potentialName : list) {
          File baseFolder = new File(folder, potentialName);
          File libraryFolder = new File(baseFolder, "library");
          File libraryJar = new File(libraryFolder, potentialName + ".jar");
          // If a .jar file of the same prefix as the folder exists
          // inside the 'library' subfolder of the sketch
          if (libraryJar.exists()) {
            String sanityCheck = Sketch.sanitizeName(potentialName);
            if (sanityCheck.equals(potentialName)) {
              libraries.add(new Library(baseFolder));              

            } else {
              String mess =
                "The library \"" + potentialName + "\" cannot be used.\n" +
                "Library names must contain only basic letters and numbers.\n" +
                "(ASCII only and no spaces, and it cannot start with a number)";
              Base.showMessage("Ignoring bad library name", mess);
              continue;
            }
          }
        }
      }
    }
  }
}