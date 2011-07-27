package processing.app;

import java.io.*;
import java.util.*;

import processing.app.contribution.*;

public class LibraryCompilation extends InstalledContribution {

  List<String> libraryNames;
  
  ArrayList<Library> libraries;
  
  private LibraryCompilation(File folder) throws IOException {
    
    super(folder);
    
    libraryNames = toList(properties.get("libraryNames"));
    
    libraries = new ArrayList<Library>();
    Library.list(folder, libraries, name);
  }
  
  /**
   * 
   * @param libraries
   * @throws IOException
   * @throws IllegalArgumentException
   *           if libraries is empty, libraries are not all in the same folder
   *           or group.
   */
  private LibraryCompilation(ArrayList<Library> libraries)
      throws IllegalArgumentException {
    
    super(null);
    
    this.libraries = libraries;
    
    if (libraries == null || libraries.isEmpty()) {
      throw new IllegalArgumentException("No libraries given");
    }
    
    folder = libraries.get(0).getFolder().getParentFile();
    String group = libraries.get(0).group;
    for (Library lib : libraries) {
      if (!group.equals(lib.group)) {
        throw new IllegalArgumentException("Libraries are not all in the same group");
      }
      
      if (!folder.equals(lib.getFolder().getParentFile())) {
        throw new IllegalArgumentException("Libraries do not all have the same parent folder");
      }
    }
    
    // XXX; Uhg, I wish we could just call super here. Should this constructor even exist?
  }
  
  public static ArrayList<LibraryCompilation> list(ArrayList<Library> libraries) {
    HashMap<String, ArrayList<Library>> libsByGroup = new HashMap<String, ArrayList<Library>>();

    for (Library lib : libraries) {
      String group = lib.getGroup();
      if (group != null) {
        if (!libsByGroup.containsKey(group)) {
          ArrayList<Library> libs = new ArrayList<Library>();
          libs.add(lib);
          libsByGroup.put(group, libs);
        } else {
          libsByGroup.get(group).add(lib);
        }
      }
    }
    
    ArrayList<LibraryCompilation> compilations = new ArrayList<LibraryCompilation>();
    for (ArrayList<Library> libList : libsByGroup.values()) {
      try {
        compilations.add(new LibraryCompilation(libList));
      } catch (IllegalArgumentException e) {
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
