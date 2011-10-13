package processing.app;

import java.io.*;
import java.util.*;

import processing.app.contribution.*;

public class LibraryCompilation extends InstalledContribution {

  ArrayList<Library> libraries;
  
  static String propertiesFileName = "compilation.properties";
  
  private LibraryCompilation(File folder) throws IOException {
    
    super(folder, LibraryCompilation.propertiesFileName);
    
    libraries = new ArrayList<Library>();
    ArrayList<File> librariesFolders = new ArrayList<File>();
    Library.discover(folder, librariesFolders);
    
    for (File baseFolder : librariesFolders) {
      libraries.add(new Library(baseFolder, name));
    }
  }
  
  public static ArrayList<LibraryCompilation> list(ArrayList<Library> libraries) {
    HashMap<String, File> folderByGroup = new HashMap<String, File>();
    
    // Find each file that is in a group, and record what directory it is in.
    // This makes the assumption that all libraries that are grouped are
    // contained in the same folder.
    for (Library lib : libraries) {
      String group = lib.getGroup();
      if (group != null) {
        folderByGroup.put(group, lib.getFolder().getParentFile());
      }
    }
    
    ArrayList<LibraryCompilation> compilations = new ArrayList<LibraryCompilation>();
    for (File folder : folderByGroup.values()) {
      try {
        compilations.add(new LibraryCompilation(folder));
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    
    return compilations;
  }
  
  public static LibraryCompilation create(File folder) {
    try {
      LibraryCompilation compilation = new LibraryCompilation(folder);
      if (compilation.libraries.isEmpty()) {
        return null;
      }
      return compilation;
    } catch (IOException e) {
    }
    return null;
  }
  
  public Type getType() {
    return Type.LIBRARY_COMPILATION;
  }
  
}
