package processing.app;

import java.io.*;
import java.util.*;

public class LibraryCompilation extends Contribution {
  
  File folder;
  
  ArrayList<Library> libraries;
  
  /** Properties for this library compilation. */
  LibraryCompilationInfo info;
  
  private LibraryCompilation(File folder) throws IOException {
    
    this.folder = folder;
    
    File propertiesFile = new File(folder, "properties.txt");
    
    info = new LibraryCompilationInfo();
    info.compilation = this;
    
    HashMap<String,String> propertiesTable = Base.readSettings(propertiesFile);
    readProperties(propertiesTable, info);
    if (info.name == null) {
      info.name = folder.getName();
    }
    
    libraries = new ArrayList<Library>();
    Library.list(folder, libraries, info.name);
    
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
    
    this.libraries = libraries;
    
    if (libraries == null || libraries.isEmpty()) {
      throw new IllegalArgumentException("No libraries given");
    }
    
    folder = libraries.get(0).folder.getParentFile();
    String group = libraries.get(0).group;
    for (Library lib : libraries) {
      if (!group.equals(lib.group)) {
        throw new IllegalArgumentException("Libraries are not all in the same group");
      }
      
      if (!folder.equals(lib.folder.getParentFile())) {
        throw new IllegalArgumentException("Libraries do not all have the same parent folder");
      }
    }
    
    File propertiesFile = new File(folder, "properties.txt");
    
    
    info = new LibraryCompilationInfo();
    info.compilation = this;
    
    HashMap<String,String> propertiesTable = Base.readSettings(propertiesFile);
    readProperties(propertiesTable, info);
    if (info.name == null) {
      info.name = group;
    }
    
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
  
  ContributionInfo getInfo() {
    return info;
  }
  
  File getFolder() {
    return folder;
  }

  public static class LibraryCompilationInfo extends ContributionInfo {
    
    protected LibraryCompilation compilation;
    
    protected List<String> libraryNames;

    public ContributionType getType() {
      return ContributionType.LIBRARY_COMPILATION;
    }
    
    public boolean isInstalled() {
      // TODO: Check that the right number of libraries are installed
      return compilation != null;
    }

    public Contribution getContribution() {
      return compilation;
    }

  }
  
}
